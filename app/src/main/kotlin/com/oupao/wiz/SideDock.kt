package com.oupao.wiz

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.view.View

/**
 * 「呼出侧边栏中转站」—— 跑在 **com.coloros.smartsidebar** 进程里。
 *
 * ## 为什么必须在这个进程里做
 *
 * 中转站（TransferDock）不是独立 Activity，它是侧边栏面板里的一个页面：
 * ```
 * UserPanelView.toShowTransferPanelOrDialog(Z)
 *   └─ UserListPanel.enterTransferDockPanel(Z, cb)
 *        └─ UserListPanel.showTransferDock()Z
 * ```
 * 面板视图只活在侧边栏的 UI 进程里，外面（system_server / 我们 App）没有任何
 * Intent 能打开它 —— 实测 `TransferDockReceiveActivity`（唯一导出的中转站组件）
 * 是「接收分享」用的，空 Intent 启动会立刻自己结束。
 * 也没有任何 exported 的 action 能显示面板（`oplus.intent.action.EDGEPANEL_TRANSFER_DOCK`
 * 打开的是**设置页**）。
 *
 * 所以：在本进程里挂住 `UserPanelView` 实例，需要时反射调
 * `toShowTransferPanelOrDialog(true)`。
 *
 * ## 触发通道
 *
 * 广播 `com.oupao.wiz.DOCK`（扇形菜单 / adb 自测都发这个）。
 * 面板实例是在 `onAttachedToWindow` 时捕获的 —— 那时才拿得到侧边栏自己的 Context。
 */
object SideDock {

    const val ACTION = "com.oupao.wiz.DOCK"

    /** 「翻译 / 全局翻译」代劳通道：带 extra `id`（global_translation / translation） */
    const val ACTION_TRANS = "com.oupao.wiz.TRANS"

    /** 系统侧要最近应用数据：发这个给侧边栏进程 */
    const val ACTION_RECENTS_REQ = "com.oupao.wiz.RECENTS_REQ"

    /** 侧边栏进程把最近应用回给系统侧（extra `data`：每行 `label\u0001pkg`） */
    const val ACTION_RECENTS = "com.oupao.wiz.RECENTS"

    /** 3221：导出侧边栏的工具清单（label/alias/type/iconRes）到日志，供补全面板用 */
    const val ACTION_TOOLS_REQ = "com.oupao.wiz.TOOLS_REQ"

    /** 3221：按 alias 调 ROM 自己的 PageRoutUtils.startSysTool(bean) 启动工具 */
    const val ACTION_TOOLRUN = "com.oupao.wiz.TOOLRUN"

    /** 3291：模块 App 拨动「屏蔽侧边栏」开关时发过来，立刻同步拦截/还原（替代 1.5s 常驻轮询） */
    const val ACTION_GATE = "com.oupao.wiz.SIDEBAR_GATE"

    private const val CLS_PANEL = "com.oplus.smartsidebar.panelview.edgepanel.mainpanel.UserPanelView"
    private const val CLS_MAIN = "com.oplus.smartsidebar.panelview.edgepanel.PanelMainView"

    @Volatile private var panel: Any? = null
    @Volatile private var main: Any? = null
    @Volatile private var showPanel: java.lang.reflect.Method? = null
    @Volatile private var show: java.lang.reflect.Method? = null
    @Volatile private var ctx: Context? = null
    @Volatile private var rxDone = false
    @Volatile private var installed = false

    /** 3201：侧边栏注册手势区时抓到的 manager / observer（摘除注册用） */
    @Volatile private var gmmInst: Any? = null
    @Volatile private var gmmObs: Any? = null

    /** 3212：屏蔽期间被动过的悬浮条窗口（关掉开关时要还原），以及还原看门狗 */
    @Volatile private var floatBarView: android.view.View? = null
    @Volatile private var floatBarLp: android.view.WindowManager.LayoutParams? = null
    @Volatile private var floatBarFlags = 0
    @Volatile private var floatBarAlpha = 1f
    @Volatile private var restoreWatcher: Runnable? = null
    /** 3214：当前是否处于"已拦"状态（配合看门狗做双向同步） */
    @Volatile private var applied = false

    /** install 时的 ClassLoader，后置的动态类加载要用 */
    @Volatile private var clRef: ClassLoader? = null

    /** 3212：关掉"屏蔽侧边栏"后，把悬浮条还原 + 重新注册手势区（否则必须去系统设置里开关一遍） */
    private fun restoreSwipe() {
        runCatching {
            val v = floatBarView
            val lp = floatBarLp
            if (v != null && lp != null) {
                lp.flags = floatBarFlags
                lp.alpha = floatBarAlpha
                val wm = v.context.getSystemService(android.content.Context.WINDOW_SERVICE)
                    as? android.view.WindowManager
                wm?.updateViewLayout(v, lp)
                SLog.i("SideDock: 已还原侧边栏悬浮条（可见/可触摸）")
            }
        }.onFailure { SLog.e("SideDock: 还原悬浮条失败", it) }
        // 屏蔽期间我们把 registerRegionInputEvent 拦掉了，这里补一次注册
        runCatching {
            val cl = clRef
            val iu = Class.forName("com.oplus.smartsidebar.utils.InputUtils", false, cl)
            val mm = iu.getDeclaredMethod("m")
            mm.isAccessible = true
            mm.invoke(null)
            SLog.i("SideDock: 已重新注册侧边栏手势区（可滑动呼出）")
        }.onFailure { SLog.e("SideDock: 重新注册手势区失败", it) }
    }

    /** 本地同步一次开关状态（看门狗和 3291 的广播共用） */
    private fun syncGateOnce() {
        val allow = swipeAllowed()
        if (allow && applied) {
            restoreSwipe()
            applied = false
        } else if (!allow && !applied && floatBarView != null) {
            applyBlock()
            applied = true
        }
    }

    private fun stopRestoreWatch() {
        restoreWatcher?.let { Handler(Looper.getMainLooper()).removeCallbacks(it) }
        restoreWatcher = null
    }

    /**
     * 3212/3214：看门狗。
     *
     * 3291：**开关关闭（或还没有悬浮条可拦）时不再空转**。
     * 以前一旦启动就每 1.5s 跑一辈子；现在：
     *  - 屏蔽中（!allow）且确实有东西要拦 → 保持 1.5s 兜底；
     *  - 开关关闭 / 还没捕获到悬浮条 → 停掉，等 ACTION_GATE 广播或 addView hook 再启动。
     */
    private fun startRestoreWatch() {
        if (restoreWatcher != null) return
        val h = Handler(Looper.getMainLooper())
        val r = object : Runnable {
            override fun run() {
                syncGateOnce()
                val allow = swipeAllowed()
                if (!allow && (applied || floatBarView != null)) {
                    h.postDelayed(this, 1500L)
                } else {
                    restoreWatcher = null
                }
            }
        }
        restoreWatcher = r
        h.postDelayed(r, 1500L)
    }

    /** 3214：运行中把开关打开时，立即重新拦（悬浮条隐藏 + 注销手势区） */
    private fun applyBlock() {
        runCatching {
            val v = floatBarView
            val lp = floatBarLp
            if (v != null && lp != null) {
                lp.flags = lp.flags or
                    android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                lp.alpha = 0f
                val wm = v.context.getSystemService(android.content.Context.WINDOW_SERVICE)
                    as? android.view.WindowManager
                wm?.updateViewLayout(v, lp)
            }
            SLog.i("SideDock: 开关打开 → 悬浮条再次隐藏")
        }.onFailure { SLog.e("SideDock: applyBlock 悬浮条失败", it) }
        runCatching {
            Rt.callLoose(gmmInst, "unregisterRegionInputEvent", arrayOf(gmmObs))
            SLog.i("SideDock: 开关打开 → 注销侧边栏手势区")
        }.onFailure { SLog.e("SideDock: applyBlock 注销手势区失败", it) }
    }

    /** 我们自己正在展开面板（放行用；拦滑动时判断） */
    @Volatile private var selfShow = false

    @JvmStatic
    fun install(cl: ClassLoader?) {
        if (installed) return
        installed = true
        clRef = cl
        // 先把广播注册上：翻译通道不依赖面板实例，侧边栏进程一起来就能收
        runCatching {
            val ac = appCtx()
            if (ac != null && ctx == null) ctx = ac
            register()
        }.onFailure { SLog.e("SideDock: 提前注册广播失败", it) }
        runCatching {
            val c = Rt.cls(cl, CLS_PANEL)
            if (c == null) {
                SLog.i("SideDock: 类未找到 $CLS_PANEL")
                return
            }
            // 呼出方法：toShowTransferPanelOrDialog(boolean)
            show = c.declaredMethods.firstOrNull {
                it.name == "toShowTransferPanelOrDialog" &&
                    it.parameterTypes.size == 1 &&
                    it.parameterTypes[0] == java.lang.Boolean.TYPE
            }?.also { it.isAccessible = true }
            if (show == null) SLog.i("SideDock: 方法未找到 toShowTransferPanelOrDialog(Z)")

            // 捕获实例 + Context。
            // ⚠️ 不要挂 onAttachedToWindow：它声明在 View 上，一挂就是**所有 View**都会命中
            //    （实测抓到的是 PanelMainView / FloatBarMainView → 反射调用抛
            //     IllegalArgumentException: Expected receiver of type UserPanelView）。
            //    这里只挂 UserPanelView **自己声明**的无参方法 + 它的构造方法，并做 isInstance 过滤。
            val capture = object : XMH() {
                override fun beforeHookedMethod(param: XMP) = grab(param.thisObject, c)
                override fun afterHookedMethod(param: XMP) = grab(param.thisObject, c)
            }
            var n = 0
            for (m in c.declaredMethods) {
                if (java.lang.reflect.Modifier.isStatic(m.modifiers)) continue
                if (m.parameterTypes.isNotEmpty()) continue
                if (m.name == "onAttachedToWindow") continue
                m.isAccessible = true
                if (XBridge.hookMethod(m, capture) != null) n++
            }
            runCatching {
                c.declaredConstructors.forEach { ctor ->
                    ctor.isAccessible = true
                    if (XBridge.hookMethod(ctor, capture) != null) n++
                }
            }
            SLog.i("SideDock: 已挂捕获点 " + n + " 个 show=" + (show != null))

            // 面板容器 PanelMainView：开机后第一次呼出时，面板窗口还没建出来，
            // 只切页面是没用的（实测：调用了但屏幕无变化）。先让它把面板展开。
            val mc = Rt.cls(cl, CLS_MAIN)
            if (mc == null) {
                SLog.i("SideDock: 类未找到 $CLS_MAIN")
            } else {
                showPanel = mc.declaredMethods.firstOrNull {
                    it.name == "showOrDismissPanelByScale\$default"
                }?.also { it.isAccessible = true }
                val capMain = object : XMH() {
                    override fun afterHookedMethod(param: XMP) {
                        val o = param.thisObject ?: return
                        if (mc.isInstance(o) && main == null) {
                            main = o
                            SLog.i("SideDock: 已捕获 PanelMainView showPanel=" + (showPanel != null))
                        }
                    }
                }
                runCatching {
                    mc.declaredConstructors.forEach { ctor ->
                        ctor.isAccessible = true
                        XBridge.hookMethod(ctor, capMain)
                    }
                }

                // ===== 屏蔽「边缘滑动呼出侧边栏」=====
                // showOrDismissPanelByScale 是**手势/缩放**那条展开路径，滑动呼出就走它；
                // 只拦 show=true，收起(false) 一律放行；我们自己 fire() 时置 selfShow 放行。
                // 恢复原样：setprop persist.oupao.sidebar_swipe 1
                val sw = mc.declaredMethods.firstOrNull {
                    it.name == "showOrDismissPanelByScale" &&
                        it.parameterTypes.size == 5 &&
                        it.parameterTypes[0] == java.lang.Boolean.TYPE
                }
                if (sw == null) {
                    SLog.i("SideDock: 未找到 showOrDismissPanelByScale(5参)")
                } else {
                    sw.isAccessible = true
                    val okHook = XBridge.hookMethod(sw, object : XMH() {
                        override fun beforeHookedMethod(param: XMP) {
                            val showIt = param.args.getOrNull(0) as? Boolean ?: return
                            if (!showIt) return                 // 收起：放行
                            if (selfShow) return               // 我们自己发起的：放行
                            if (swipeAllowed()) return         // 用户要求恢复滑动：放行
                            param.result = false
                            param.returnEarly = true
                            SLog.i("SideDock: 已拦截滑动呼出侧边栏")
                        }
                    }) != null
                    SLog.i("SideDock: 滑动入口屏蔽 hook=" + okHook)
                }

                // 手势真正走的是这条：UI 进程的 IUiProcessHandler.showScalePanelFromRect(...)
                // （:edgepanel 常驻进程经 binder 调过来）。我们自己的 fire() 不经过它，
                // 所以在这里拦最干净 —— 滑动不弹，扇形菜单照常。
                val hc = Rt.cls(
                    cl,
                    "com.oplus.smartsidebar.panelview.edgepanel.data.viewdatahandlers" +
                        ".ViewDataHandlerImpl\$mUiProcessHandler\$2\$1"
                )
                if (hc == null) {
                    SLog.i("SideDock: 未找到 showScalePanelFromRect 的实现类")
                } else {
                    val sm = hc.declaredMethods.firstOrNull { it.name == "showScalePanelFromRect" }
                    if (sm == null) {
                        SLog.i("SideDock: 未找到 showScalePanelFromRect")
                    } else {
                        sm.isAccessible = true
                        val ok2 = XBridge.hookMethod(sm, object : XMH() {
                            override fun beforeHookedMethod(param: XMP) {
                                val toShow = param.args.getOrNull(2) as? Boolean ?: return
                                if (!toShow) return
                                if (swipeAllowed()) return
                                param.returnEarly = true
                                SLog.i("SideDock: 已拦截 showScalePanelFromRect（滑动呼出）")
                            }
                        }) != null
                        SLog.i("SideDock: showScalePanelFromRect 屏蔽 hook=" + ok2)
                    }
                }

                // ===== 3201：把**边缘手势区**本身让给系统（恢复侧滑返回）=====
                //
                // 反编译 sidebars APK（tmp_sysapk/sb_classes.txt）看到：
                //   com.oplus.smartsidebar.utils.InputUtils.m()
                //     → OplusGestureMonitorManager.getInstance()
                //       .registerRegionInputEvent(observer, "sidebar", 0x10000001, [RectF(InputUtils.b)])
                // 也就是说：侧边栏靠**注册一个边缘手势区域**来抢触摸。只拦"弹出面板"
                // （以前的 showOrDismissPanelByScale / showScalePanelFromRect）时，
                // 触摸已经被这个区域消费掉了，系统自己的"边缘侧滑返回"照样收不到 ——
                // 用户反馈"屏蔽了侧边栏，但这个位置也不能侧滑返回"就是这个原因。
                //
                // 开关打开时干脆**不注册/不更新**这个区域，边缘就还给系统了；
                // 万一注册已经发生（运行中才拨开关），下次它想更新/弹出时顺手 unregister。
                runCatching {
                    val gmm = Rt.cls(cl, "android.os.OplusGestureMonitorManager")
                        ?: Class.forName("android.os.OplusGestureMonitorManager", false, cl)
                    if (gmm == null) {
                        SLog.i("SideDock: 未找到 OplusGestureMonitorManager")
                    } else {
                        for (nm in arrayOf("registerRegionInputEvent", "updateRegionInputEvent")) {
                            val gm = gmm.declaredMethods.firstOrNull {
                                it.name == nm && it.parameterTypes.size == 4
                            } ?: continue
                            gm.isAccessible = true
                            val okG = XBridge.hookMethod(gm, object : XMH() {
                                override fun beforeHookedMethod(param: XMP) {
                                    gmmInst = param.thisObject
                                    gmmObs = param.args.getOrNull(0)
                                    // 3206：**把侧边栏的手势区彻底摘掉** —— 这条注册会让系统的边缘
                                    // 返回区把 sidebar 那一段挖掉（dumpsys input 里可见
                                    // `Remote : tag=sidebar validRegion=[1143,266][1217,796]`，
                                    // GestureMode 排除区正是 [1149,135][1216,266] + [1149,796][1216,2640]），
                                    // 于是那条边既没有侧边栏、也没有系统返回。
                                    // 摘掉之后边缘完全回到系统手里：原生的侧滑返回（带动效）。
                                    if (!swipeAllowed()) {
                                        param.result = false
                                        param.returnEarly = true
                                        runCatching {
                                            Rt.callLoose(gmmInst, "unregisterRegionInputEvent",
                                                arrayOf(gmmObs))
                                        }
                                        SLog.i("SideDock: 已摘除侧边栏手势区（边缘还给系统返回） " + nm)
                                    }
                                }
                            }) != null
                            SLog.i("SideDock: " + nm + " hook=" + okG)
                        }
                    }
                }.onFailure { SLog.e("SideDock: 手势区 hook 失败", it) }

                // 3206：不许侧边栏把占用区写回去（system_server 会把它清成 0 0 0 0）
                runCatching {
                    val pm = android.provider.Settings.Secure::class.java.getMethod(
                        "putString",
                        android.content.ContentResolver::class.java,
                        String::class.java,
                        String::class.java
                    )
                    val okS = XBridge.hookMethod(pm, object : XMH() {
                        override fun beforeHookedMethod(param: XMP) {
                            val k = param.args.getOrNull(1) as? String ?: return
                            if (k != "sidebar_scene_region") return
                            if (swipeAllowed()) return
                            param.result = true
                            param.returnEarly = true
                            SLog.i("SideDock: 拒绝侧边栏写回 sidebar_scene_region")
                        }
                    }) != null
                    SLog.i("SideDock: sidebar_scene_region 写入拦截 hook=" + okS)
                }.onFailure { SLog.e("SideDock: region 写入拦截失败", it) }

                // ===== 3209：把侧边栏那条**悬浮条窗口**变成不可触摸/透明 =====
                //
                // 实测：`dumpsys window` 里有一个
                //   Window{.. OplusOSEdgeFloatBar}  package=com.coloros.smartsidebar
                //   frame=[1188,267][1216,795]  ty=2314  fl=NOT_FOCUSABLE NOT_TOUCH_MODAL
                // —— 28px 宽、正好压在右边那条边缘上，且**可触摸**。
                // 这才是"屏蔽了侧边栏，但那条边连返回都不响应"的真凶：
                // 触摸被这个窗口吃掉，系统的边缘返回收不到。
                // 开关打开时把它改成 NOT_TOUCHABLE + 透明（窗口还在，但穿透/看不见）。
                runCatching {
                    val wmi = Class.forName("android.view.WindowManagerImpl", false, cl)
                    var n2 = 0
                    for (nm in arrayOf("addView", "updateViewLayout")) {
                        val wm = wmi.declaredMethods.firstOrNull {
                            it.name == nm && it.parameterTypes.size == 2
                        } ?: continue
                        wm.isAccessible = true
                        val okW = XBridge.hookMethod(wm, object : XMH() {
                            override fun beforeHookedMethod(param: XMP) {
                                // 3217：**这里不能因为"开关是关"就提前 return** ——
                                //   否则"先关后开"时拿不到原始 LP，也没起看门狗，
                                //   开关再打开就没法把悬浮条重新藏起来（贴边失效的老问题）。
                                // 3211：**别把面板本体也当成悬浮条** —— 面板和悬浮条都用 ty=2314，
                                //   之前按 type 一刀切，结果 `ColorOSEdgePanel` 也被改成 alpha=0 +
                                //   NOT_TOUCHABLE → 中转站（侧边栏面板）呼出后完全看不见/点不到。
                                //   现在只认"窄且高"的那个（28x528），并显式排除面板实例。
                                val v = param.args.getOrNull(0)
                                if ((v != null) && (v === panel)) return
                                val lp = param.args.getOrNull(1) as? android.view.WindowManager.LayoutParams
                                    ?: return
                                if (lp.type != 2314) return
                                val narrow = lp.width in 2..48
                                val tall = lp.height >= 200
                                if (!narrow || !tall) return
                                // 3215：**不管开关开还是关，先把原始 LP 记下来**。
                                //   之前只在"屏蔽中"才记，于是"先关后开"时 floatBarView 一直是 null，
                                //   看门狗没法把悬浮条再藏起来 → 那条 28px 又吃掉边缘（用户实测）。
                                if (floatBarView == null) {
                                    floatBarView = v as? android.view.View
                                    floatBarLp = lp
                                    floatBarFlags = lp.flags
                                    floatBarAlpha = lp.alpha
                                    SLog.i("SideDock: 已记录悬浮条 LP alpha=" + floatBarAlpha
                                            + " flags=" + floatBarFlags)
                                    // 3292：悬浮条所在进程（:edgepanel）之前没有 Context，
                                    //   ACTION_GATE 接收器没注册 → 关→开时收不到广播。
                                    //   这里用 View 的 Context 补齐注册。
                                    runCatching {
                                        val vc = (v as? android.view.View)?.context
                                        if (vc != null) {
                                            if (ctx == null) ctx = vc
                                            register()
                                        }
                                    }
                                }
                                if (swipeAllowed()) {
                                    applied = false
                                    startRestoreWatch()
                                    return
                                }
                                lp.flags = lp.flags or
                                    android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                                lp.alpha = 0f
                                SLog.i("SideDock: 悬浮条窗口已改为不可触摸 type=" + lp.type
                                        + " w=" + lp.width + " h=" + lp.height)
                                applied = true
                                startRestoreWatch()
                            }
                        }) != null
                        if (okW) n2++
                    }
                    SLog.i("SideDock: OplusOSEdgeFloatBar 处理 hook=" + n2)
                }.onFailure { SLog.e("SideDock: 悬浮条 hook 失败", it) }

                // 3210：**删掉 3202~3205 那套"观察者→广播→注入返回"**。
                // 边缘已经整个还给系统了（手势区不注册 / 占用区清空 / 悬浮条不可触摸），
                // 系统原生的侧滑返回就够了；再留着这套会出现"偶尔系统返回、偶尔模拟返回"，
                // 甚至一次手势退两层。只保留"面板绝不弹"这一条（下面这个 hook 已在上面
                // 的 showScalePanelFromRect / showOrDismissPanelByScale 里覆盖）。
            }
        }.onFailure { SLog.e("SideDock: install 失败", it) }
    }

    /**
     * 是否允许边缘滑动呼出。
     *
     * 开关：模块界面「FlymeUI」页 → `oupao_sidebar_noswipe`（**默认关 = 不屏蔽**，2799 起）。
     *
     * ## 2830：通道顺序反过来 —— 先读 Settings.Global
     *
     * 以前只能读系统属性 `persist.oupao.sidebar_swipe`，而那个属性**只有 root 能写**
     * （App 用 `su -c setprop`；system_server 侧 `SystemProperties.set` 被 SELinux 拒）。
     * 用户把模块的 root 关掉后：开关能拨、配置也写了，但属性不动
     * → 看着关了，其实还在屏蔽滑动。
     *
     * 现在：App 拨开关 → `push()` 广播 → system_server `Cfg.apply()` 写
     * `Settings.Global["oupao_oupao_sidebar_noswipe"]`（system_server 有写权），
     * 本进程用**自己**的 Context 直接读（读设置不需要权限）。属性只作为 adb/root 覆盖。
     */
    private fun swipeAllowed(): Boolean {
        // 3146：只认权威 prefs（本进程在作用域里，直读同一个存储）；
        // Settings.Global / persist 属性两条老通道已删除（都写不进去过）。
        runCatching { if (Cfg.prefsReady()) return !Cfg.sidebarNoSwipe() }
        // prefs 没接上时退回文件通道（Cfg 内部已按 App 文件 → 系统侧文件取值）
        return !Cfg.sidebarNoSwipe()
    }

    /** 只接受真正的 UserPanelView 实例（其余 View 也会命中同一条 hook） */
    private fun grab(o: Any?, c: Class<*>) {
        if (o == null || panel != null) return
        if (!c.isInstance(o)) return
        panel = o
        SLog.i("SideDock: 已捕获面板实例 " + o.javaClass.name)
        // UserPanelView 不一定是 View（实测拿不到 getContext），优先用本进程的 Application
        val cc = (o as? View)?.context ?: appCtx()
        if (cc != null) {
            ctx = cc
            SLog.i("SideDock: 已拿到侧边栏 Context pkg=" + cc.packageName)
            register()
        } else {
            SLog.i("SideDock: 拿不到 Context，广播未注册")
        }
    }

    /** ActivityThread.currentApplication() —— 本进程的 Application（侧边栏自己的 Context） */
    private fun appCtx(): Context? = runCatching {
        val at = Class.forName("android.app.ActivityThread")
        val cur = at.getMethod("currentActivityThread").invoke(null)
        at.getMethod("currentApplication").invoke(null) as? Context
            ?: (at.getMethod("getApplication").invoke(cur) as? Context)
    }.getOrNull()

    private fun register() {
        val c = ctx ?: return
        if (rxDone) return
        rxDone = true
        runCatching {
            val r = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val act = intent?.action
                    if (act == ACTION_TRANS) {
                        val id = intent.getStringExtra("id") ?: "global_translation"
                        SLog.i("SideDock: 收到翻译请求 id=" + id)
                        trans(id)
                        return
                    }
                    if (act == ACTION_RECENTS_REQ) {
                        SLog.i("SideDock: 收到最近应用请求")
                        dumpRecents(context)
                        return
                    }
                    // 3221：工具清单 / 通用启动
                    if (act == ACTION_TOOLS_REQ) {
                        SLog.i("SideDock: 收到工具清单请求")
                        dumpTools()
                        return
                    }
                    if (act == ACTION_TOOLRUN) {
                        val a = intent.getStringExtra("alias")
                        SLog.i("SideDock: 收到工具启动请求 alias=" + a)
                        runTool(a)
                        return
                    }
                    if (act == ACTION_GATE) {
                        // 3291：开关一拨就同步，不再等 1.5s 轮询
                        SLog.i("SideDock: 收到开关同步")
                        syncGateOnce()
                        if (!swipeAllowed() && (applied || floatBarView != null)) {
                            startRestoreWatch()
                        } else {
                            stopRestoreWatch()
                        }
                        return
                    }
                    SLog.i("SideDock: 收到广播，准备呼出中转站")
                    fire("broadcast")
                }
            }
            val f = IntentFilter(ACTION)
            f.addAction(ACTION_TRANS)
            f.addAction(ACTION_RECENTS_REQ)
            f.addAction(ACTION_TOOLS_REQ)
            f.addAction(ACTION_TOOLRUN)
            f.addAction(ACTION_GATE)
            // Android 13+ 动态注册必须给 flag：2 = RECEIVER_EXPORTED（要让 adb/系统侧能发过来）
            // 2906fix：原来还有一个 `recoverCatching { c.registerReceiver(r, f) }` 的旧重载兜底。
            //   在 system_server 里 IActivityManager 还是 null时两条都会 NPE，
            //   每次开机打两条 `Rx: 3参/5参 失败` 的堆栈——既无用又吵。
            val ok = runCatching {
                val m = Context::class.java.getMethod(
                    "registerReceiver",
                    BroadcastReceiver::class.java, IntentFilter::class.java, Integer.TYPE
                )
                m.invoke(c, r, f, 2) != null
            }.getOrDefault(false)
            if (ok) {
                SLog.i("SideDock: 广播已注册 action=" + ACTION + " / " + ACTION_TRANS)
            } else {
                SLog.r("SideDock", "SideDock: 广播未注册（进程内无 IActivityManager，属正常）", 5000L)
            }
        }.onFailure { SLog.e("SideDock: 注册广播失败", it) }
    }

    /**
     * 把侧边栏的「最近应用」读出来，回给系统侧（MorePanel / IdxBar 用）。
     *
     * 数据源（反编译 `tmp_sysapk\smartsidebar.apk` 确认）：
     * `com.oplus.smartsidebar.panelview.edgepanel.recentapp.RecentAppSource`
     *   ├─ `mCacheRecentList`     ← 界面正在显示的那份（List，元素是最近应用 bean）
     *   └─ `mDefaultRecentPkgList`← 兜底（包名列表）
     *
     * 元素类型各版本不同，所以**不假设字段名**：先试 getPackageName()/getPkgName() 等方法，
     * 再试 packageName/componentName/pkg 等字段，拿到包名即可；
     * 名字用 PackageManager 查（比信任 bean 里的字符串稳）。
     * 回给系统侧的格式：每行 `label\u0001pkg`（cls 由系统侧自己解析桌面入口）。
     */
    @JvmStatic
    fun dumpRecents(ctxIn: Context?) {
        val c: Context = ctxIn ?: ctx ?: appCtx() ?: run {
            SLog.i("SideDock: dumpRecents 拿不到 Context")
            return
        }
        val max = 10
        val sb = StringBuilder()
        var n = 0
        runCatching {
            val cls = Class.forName(
                "com.oplus.smartsidebar.panelview.edgepanel.recentapp.RecentAppSource",
                true, c.classLoader
            )
            // ① 缓存列表
            var list: List<*>? = null
            runCatching {
                val f = cls.getDeclaredField("mCacheRecentList")
                f.isAccessible = true
                list = f.get(null) as? List<*>
            }
            var src = "cache"
            if (list == null || list.isEmpty()) {
                runCatching {
                    val f = cls.getDeclaredField("mDefaultRecentPkgList")
                    f.isAccessible = true
                    list = f.get(null) as? List<*>
                }
                src = "default"
            }
            val seen = HashSet<String>()
            list?.forEach { o ->
                if (n >= max || o == null) return@forEach
                val pkg = findPkg(c, o) ?: return@forEach
                if (pkg == c.packageName || pkg == "android") return@forEach
                if (!seen.add(pkg)) return@forEach
                val label = labelOf(c, pkg) ?: return@forEach
                sb.append(label).append('\u0001').append(pkg).append('\n')
                n++
            }
            val firstCls = list?.firstOrNull { it != null }?.javaClass?.name ?: "-"
            SLog.i("SideDock: 最近应用 src=" + src + " n=" + n + " size=" + (list?.size ?: -1)
                + " cls=" + firstCls)
        }.onFailure { SLog.e("SideDock: 读最近应用失败", it) }

        runCatching {
            val out = Intent(ACTION_RECENTS)
            out.putExtra("data", sb.toString())
            c.sendBroadcast(out)
            SLog.i("SideDock: 已回最近应用 len=" + sb.length)
        }.onFailure { SLog.e("SideDock: 回最近应用失败", it) }
    }

    /**
     * 通用「找包名」：不管 bean 长什么样，把它身上的字符串/ComponentName/Intent
     * 全部掏出来（含嵌套对象，最多两层），**用 PackageManager 验证**哪个是已安装的包。
     *
     * 为什么这么写：2326 版第一次试 `getPackageName()` 一律失败（日志
     * `最近应用 src=cache n=0 size=2` —— 有 2 个元素但抠不出包名），
     * 说明那个 bean 的字段名跟猜的不一样；用「候选 + 验证」就不依赖字段名了。
     */
    private fun findPkg(c: Context, o: Any): String? {
        val cands = ArrayList<String>()
        collectStrings(o, 0, cands)
        for (s in cands) {
            if (s == c.packageName || s == "android") continue
            if (runCatching { c.packageManager.getApplicationInfo(s, 0) }.isSuccess) return s
        }
        return null
    }

    private fun collectStrings(o: Any?, depth: Int, out: MutableList<String>) {
        if (o == null || depth > 2 || out.size > 200) return
        when (o) {
            is String -> { if (o.contains('.')) out.add(o); return }
            is android.content.ComponentName -> { out.add(o.packageName); return }
            is android.content.Intent -> { o.`package`?.let { out.add(it) }; return }
            is CharSequence -> return
        }
        val cl = o.javaClass
        val pkgName = cl.name
        if (pkgName.startsWith("java.") || pkgName.startsWith("kotlin.")) return
        runCatching {
            for (m in cl.methods) {
                if (m.parameterTypes.isNotEmpty()) continue
                val rt = m.returnType
                if (rt != String::class.java && rt != android.content.ComponentName::class.java
                    && rt != android.content.Intent::class.java) continue
                runCatching { collectStrings(m.invoke(o), depth, out) }
            }
        }
        runCatching {
            var k: Class<*>? = cl
            var guard = 0
            while (k != null && k != Any::class.java && guard < 5) {
                guard++
                for (f in k.declaredFields) {
                    if (java.lang.reflect.Modifier.isStatic(f.modifiers)) continue
                    val t = f.type
                    if (t != String::class.java && t != android.content.ComponentName::class.java
                        && t != android.content.Intent::class.java && !t.name.contains("edgepanel")) continue
                    runCatching { f.isAccessible = true; collectStrings(f.get(o), depth + 1, out) }
                }
                k = k.superclass
            }
        }
    }

    private fun labelOf(c: Context, pkg: String): String? = runCatching {
        val pm = c.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    }.getOrNull()

    /**
     * 「翻译 / 全局翻译」—— 必须**在本进程**发出去。
     *
     * ## 为什么不能从 system_server 发
     *
     * 反编译侧边栏确认：`TranslationTool.handle()` 最后是
     * `AppPlatformUtils.m(intent)` → `PageRoutUtils.startActivity(intent)`
     * （就是普通 `startActivity`，没有窗口模式的魔法）；
     * `GlobalTranslationTool.handle()` 是 `startForegroundService`。
     * 也就是说意图/参数我们本来就和它一模一样 —— 唯一不同的是**调用方身份**：
     * 翻译应用靠这个（`getLaunchedFromPackage` / calling uid）决定走浮窗还是全屏，
     * 冷启动时也靠它判断是否放行。所以只能在侧边栏进程里调。
     *
     * ## 做法
     *
     * 优先直接用侧边栏自己的工具类（`…models.tools.GlobalTranslationTool` /
     * `TranslationTool`）：构造 → 反射把 `AbsTool.mValidPkg` 设成正确的翻译应用包
     * （这个字段平时由面板建工具列表时填，我们不构造列表所以得自己填）→ 调 `handle()`。
     * 反射失败再退回「本进程直接发同一套意图」。
     */
    @JvmStatic
    fun trans(id: String) {
        val c: Context = ctx ?: appCtx() ?: run {
            SLog.i("SideDock: trans 拿不到 Context，放弃")
            return
        }
        val clsName = if ("translation" == id)
            "com.oplus.smartsidebar.panelview.edgepanel.data.entrybeans.models.tools.TranslationTool"
        else
            "com.oplus.smartsidebar.panelview.edgepanel.data.entrybeans.models.tools.GlobalTranslationTool"
        val pkg = transPkg(c)
        runCatching {
            val cls = Class.forName(clsName, true, c.classLoader)
            val tool = cls.getConstructor(Context::class.java).newInstance(c)
            if (pkg != null) {
                var k: Class<*>? = cls
                while (k != null) {
                    val f = runCatching { k!!.getDeclaredField("mValidPkg") }.getOrNull()
                    if (f != null) {
                        f.isAccessible = true
                        f.set(tool, pkg)
                        break
                    }
                    k = k.superclass
                }
            }
            val h = cls.getMethod("handle")
            h.isAccessible = true
            h.invoke(tool)
            SLog.i("SideDock: 已用侧边栏工具 " + id + " handle() pkg=" + pkg)
            return
        }.onFailure { SLog.e("SideDock: 调侧边栏工具失败 " + id + "，退回自己发意图", it) }
        fallbackTrans(c, id, pkg)
    }

    /** 翻译应用包名，顺序与侧边栏 `GlobalTranslationTool.checkAvailablePackage()` 一致 */
    private fun transPkg(c: Context): String? {
        for (p in arrayOf("com.oplus.translate", "com.coloros.translate", "com.coloros.ocrscanner")) {
            if (runCatching { c.packageManager.getApplicationInfo(p, 0) }.isSuccess) return p
        }
        return null
    }

    private fun fallbackTrans(c: Context, id: String, pkg: String?) {
        val p = pkg ?: return
        runCatching {
            if ("translation" == id) {
                val i = Intent("coloros.intent.action.TRANSLATION.COMPASS")
                i.setPackage(p)
                i.putExtra("extra_from_package", "com.coloros.smartsidebar")
                i.addFlags(335544320)
                c.startActivity(i)
            } else {
                val i = Intent("oplus.intent.action.GLOBAL_TRANSLATION")
                i.setPackage(p)
                i.putExtra("extra_from_package", "com.coloros.smartsidebar")
                c.startForegroundService(i)
            }
            SLog.i("SideDock: 兜底发意图 " + id + " pkg=" + p)
        }.onFailure { SLog.e("SideDock: 兜底发意图失败 " + id, it) }
    }

    /** 真正呼出中转站面板 */
    @JvmStatic
    fun fire(from: String) {
        val v = panel
        val m = show
        if (v == null || m == null) {
            SLog.i("SideDock: 面板实例/方法缺失 from=" + from + " panel=" + (v != null) + " m=" + (m != null))
            return
        }
        val h = Handler(Looper.getMainLooper())
        // 3218：**先直接调 ROM 自己的 toShowTransferPanelOrDialog(true)** ——
        //   实测系统行为：原生面板的"悬浮卡片"布局是由面板自己算的，
        //   我们额外再调一次 showOrDismissPanelByScale 反而把它铺成"贴边整高"。
        //   原生调用 700ms 内没把面板显示出来（isShown=false），才退回老的"展开+切页"。
        h.post {
            selfShow = true
            runCatching { m.invoke(v, true) }
                .onFailure { SLog.e("SideDock: 原生呼出失败", it) }
            selfShow = false
            SLog.i("SideDock: 已原生呼出中转站 from=" + from)
            // 3219 试过给面板视图补 alpha/translationX 入场动效：实测会让面板**立刻收起**
            //   （ROM 的状态机被这个属性变化干扰），所以不做。原生调用本身是"直接出现"。
        }
        // 3221：**失败自动重试一次原生调用**（侧边栏进程刚起来时第一次常不生效），
        //   两次都失败才退回老路。
        h.postDelayed({
            if (panelShown(v)) {
                SLog.i("SideDock: 原生呼出成功（保持原生布局）")
                return@postDelayed
            }
            SLog.i("SideDock: 首次原生呼出未生效 → 重试一次")
            selfShow = true
            runCatching { m.invoke(v, true) }
                .onFailure { SLog.e("SideDock: 重试原生呼出失败", it) }
            selfShow = false
            h.postDelayed({
                if (panelShown(v)) {
                    SLog.i("SideDock: 重试成功（保持原生布局）")
                    return@postDelayed
                }
                SLog.i("SideDock: 重试仍未生效 → 退回展开+切页")
                legacyFire(v, m, h, from)
            }, 700L)
        }, 700L)
    }

    private fun panelShown(v: Any?): Boolean =
        runCatching { (v as? android.view.View)?.isShown() == true }.getOrDefault(false)

    /** 老的打开方式：先 showOrDismissPanelByScale 展开，再切到中转站页 */
    private fun legacyFire(v: Any, m: java.lang.reflect.Method, h: Handler, from: String) {
        // 3212：展开完成后再切页（以前固定 400ms，太快 → 面板背板还没铺好，
        //   出现"侧边栏第一页和中转站页重叠"）。优先用 showOrDismissPanelByScale
        //   的完成回调；拿不到回调就退回更长的延时。
        var cb: Any? = null
        runCatching {
            val iface = Class.forName("ld.a", false, clRef)
            cb = java.lang.reflect.Proxy.newProxyInstance(
                iface.classLoader, arrayOf(iface),
                java.lang.reflect.InvocationHandler { _, method, _ ->
                    if (method.name == "invoke") {
                        // 3215：展开回调可能早于"背板铺满"，再等 200ms 切页
                        h.postDelayed({ runCatching { m.invoke(v, true) } }, 200L)
                    }
                    null
                }
            )
        }.onFailure { SLog.i("SideDock: 拿不到展开回调接口，改用延时 (" + it + ")") }

        val pm = main
        val sp = showPanel
        if (pm != null && sp != null) {
            h.post {
                selfShow = true
                runCatching {
                    // showOrDismissPanelByScale$default(view, show=true, scale=1.0, anim=true,
                    //                                  delay=0, cb, mask=0, null)
                    // 3213：scale 由 null 改成 1.0f —— null 时面板按"半展开"状态铺，
                    //   背板（面板背景）不会完整绘制，视觉上就是卡片浮在桌面上。
                    sp.invoke(null, pm, true, null, true, 0L, cb, 0, null)
                }.onFailure { SLog.e("SideDock: 展开面板失败", it) }
                selfShow = false
                SLog.i("SideDock: 已请求展开面板 from=" + from + " cb=" + (cb != null))
            }
        }
        if (cb == null) {
            h.postDelayed({
                runCatching { m.invoke(v, true) }
                    .onFailure { SLog.e("SideDock: 调用 toShowTransferPanelOrDialog 失败", it) }
                SLog.i("SideDock: 已调用 toShowTransferPanelOrDialog(true) from=" + from)
            }, 700L)
        }
    }

    /** 诊断：当前捕获状态 */
    @JvmStatic
    fun state(): String =
        "panel=" + (panel != null) + " method=" + (show != null) + " ctx=" + (ctx != null) + " rx=" + rxDone

    // ────────────────────────────────────────────────────────────────
    // 3221：侧边栏工具清单 / 通用启动
    //
    // 实测系统行为（侧边栏条目）：
    //   EntryBeanHelper.getActiveInstance().getAllAppListForTool() → EntryBean 列表
    //   PageRoutUtils.startSysTool(EntryBean)（**静态**）→ 启动该工具
    // 这样面板里就能列全侧边栏的工具，而不是我们手写的那 12 个。
    // ────────────────────────────────────────────────────────────────
    private const val CLS_ENTRY_HELPER =
        "com.oplus.smartsidebar.panelview.edgepanel.data.entrybeans.EntryBeanHelper"
    private const val CLS_ENTRY_BEAN =
        "com.oplus.smartsidebar.panelview.edgepanel.data.entrybeans.models.beans.EntryBean"
    private const val CLS_ROUTE =
        "com.oplus.smartsidebar.panelview.edgepanel.utils.PageRoutUtils"

    /** 取侧边栏当前的工具 bean 列表（拿不到返回空） */
    private fun toolBeans(): List<Any> = runCatching {
        val cl = clRef
        val helper = Class.forName(CLS_ENTRY_HELPER, false, cl)
        val owner = helper.getMethod("getActiveInstance").invoke(null) ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        (helper.getMethod("getAllAppListForTool").invoke(owner) as? List<Any>) ?: emptyList()
    }.getOrDefault(emptyList())

    /** 把工具清单打到日志：`SideDock: TOOL alias=… label=… type=… icon=drawable/xxx` */
    private fun dumpTools() {
        runCatching {
            val cl = clRef
            val bean = Class.forName(CLS_ENTRY_BEAN, false, cl)
            val gAlias = bean.getMethod("getAlias")
            val gLabel = bean.getMethod("getLabel")
            val gType = bean.getMethod("getType")
            val gIcon = bean.getMethod("getIconRes")
            val gCat = bean.getMethod("getCategory")
            val list = toolBeans()
            SLog.i("SideDock: TOOLS 共 " + list.size + " 个")
            for (b in list) {
                val alias = runCatching { gAlias.invoke(b) as? String }.getOrNull()
                val label = runCatching { gLabel.invoke(b) as? String }.getOrNull()
                val type = runCatching { gType.invoke(b)?.toString() }.getOrNull()
                val cat = runCatching { gCat.invoke(b)?.toString() }.getOrNull()
                val iconName = runCatching {
                    val id = gIcon.invoke(b) as? Int ?: return@runCatching "-"
                    ctx?.resources?.getResourceName(id) ?: "-"
                }.getOrDefault("-")
                SLog.i("SideDock: TOOL alias=" + alias + " label=" + label
                        + " type=" + type + " cat=" + cat + " icon=" + iconName)
            }
        }.onFailure { SLog.e("SideDock: dumpTools 失败", it) }
    }

    /** 按 alias 启动侧边栏自己的工具（ROM 的 PageRoutUtils.startSysTool 是静态方法） */
    private fun runTool(alias: String?) {
        if (alias.isNullOrEmpty()) return
        // 3224：**不再先展开面板** —— 用户反馈"补齐的工具都会顺带呼出侧边栏"，
        //   而原来那 12 个工具是不弹面板的。这里直接调 ROM 的启动入口，
        //   需要面板上下文才能工作的工具（最近文件/清理存储）就用它自己的兜底行为。
        runToolNow(alias)
    }

    private fun runToolNow(alias: String) {
        runCatching {
            val cl = clRef
            val beanCls = Class.forName(CLS_ENTRY_BEAN, false, cl)
            val gAlias = beanCls.getMethod("getAlias")
            val b = toolBeans().firstOrNull {
                runCatching { gAlias.invoke(it) == alias }.getOrDefault(false)
            }
            if (b == null) {
                SLog.i("SideDock: TOOLRUN 找不到 alias=" + alias)
                return
            }
            val route = Class.forName(CLS_ROUTE, false, cl)
            val m = route.getMethod("startSysTool", beanCls)
            m.isAccessible = true
            m.invoke(null, b)
            SLog.i("SideDock: TOOLRUN 已启动 alias=" + alias)
        }.onFailure { SLog.e("SideDock: TOOLRUN 失败 alias=" + alias, it) }
    }

}
