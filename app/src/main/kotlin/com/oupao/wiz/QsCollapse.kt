package com.oupao.wiz

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper

/**
 * 2954：控制中心（磁贴）收起的**动效**。
 *
 * 调查结论（SystemUI 16.001.002，classes3.dex，用 tools/sysui_probe.py + sysui_code.py 查的）：
 *
 *  磁贴点击链（分离式控制中心）：
 *    `com.oplus.systemui.plugins.qs.OplusSeparateQSPluginImpl.clickTile(ComponentName)`
 *      → `OplusQSRootViewComponent.clickTile(ComponentName)`
 *      → `OplusQSTilesComponent.clickTile(ComponentName)`
 *      → `QSTileImpl.click/handleClick(...)`（点击后的动作、长按设置页都在这一层）
 *  点击统计/监听另有 `OplusSeparateClickTileManager.onQStileClick(CharSequence, int)`。
 *  真正发起 Activity 的辅助还是 AOSP 那套
 *  （`ActivityStarterImpl.startActivityDismissingKeyguard(Intent,Z,Z,String)` /
 *   `ActivityStarterInternalImpl.…` / `CustomTile.startActivityAndCollapse(PendingIntent)`），
 *  但**磁贴点击不经过它们**（MEMORY 2893–2932 已实测），所以钩不到。
 *
 *  而「收控制中心」的**带动画实现**在这里：
 *    `OplusSeparateQSPluginImpl.collapseQSPanel(boolean anim, boolean, boolean delayed, Function0)`
 *      → anim==true → `OplusQSRootViewComponent.collapseQSPanel(boolean delayed, String callFun)`
 *          → `collapseQSPanelInner()`  ← ROM 自己的收起动画
 *      → anim==false → `collapseQSPanelWithoutAnimate(boolean, Function0)`
 *  另外 `OplusQSRootViewComponent.isFullyCollapsed()/isClosing/expandedFraction` 可以用来观察状态。
 *
 * system_server 侧只能拿到 `IStatusBarService.collapsePanels()`（**无动画**），
 * 所以 2893–2932 一直是「控制中心直接消失」。现在改成：
 * system_server 发广播 `com.oupao.wiz.QS_COLLAPSE` → 本进程调 ROM 的带动画收起。
 */
object QsCollapse {

    private const val ACTION = "com.oupao.wiz.QS_COLLAPSE"
    private const val PLUGIN = "com.oplus.systemui.plugins.qs.OplusSeparateQSPluginImpl"

    /**
     * 3332：**合并模式**的带动画收起。
     *
     * 合并模式没有 `OplusSeparateQSPluginImpl.qsPanelViewController`（那是分离式的）；
     * `IStatusBarService.collapsePanels()` 与 `CommandQueue.animateCollapsePanels()` 在这台机器上
     * 对合并面板**都不播动画**（实测调用后 260ms 面板仍原样展开，直到小窗出现才被顶掉）。
     * 上滑/返回关闭时真正播动画的是这个类的
     * `collapse(boolean animate, float velocity, String reason)` /
     * `collapse(float velocity, boolean toQsPanel, boolean animate)`。
     */
    private const val PANEL = "com.android.systemui.shade.NotificationPanelViewController"

    @Volatile
    private var panel: Any? = null

    private val main = Handler(Looper.getMainLooper())

    @Volatile
    private var plugin: Any? = null

    @Volatile
    private var registered = false

    /** SystemUI 侧安装：抓插件实例 + 注册广播。返回装上的钩子数。 */
    @JvmStatic
    fun install(cl: ClassLoader?): Int {
        var n = 0
        val cls = runCatching { Rt.cls(cl, PLUGIN) }.getOrNull()
        if (cls != null) {
            for (ctor in cls.declaredConstructors) {
                runCatching { ctor.isAccessible = true }
                if (XBridge.hookMethod(ctor, Grab()) != null) n++
            }
            SLog.i("QsCollapse: 插件构造钩子 " + n + " 个")
        } else {
            SLog.i("QsCollapse: 没找到 " + PLUGIN + "（控制中心收面板将退回无动画）")
        }
        // 合并模式的带动画收起：抓 NotificationPanelViewController 实例
        runCatching {
            val pc = Rt.cls(cl, PANEL)
            if (pc != null) {
                for (ctor in pc.declaredConstructors) {
                    runCatching { ctor.isAccessible = true }
                    if (XBridge.hookMethod(ctor, object : XMH() {
                            override fun afterHookedMethod(param: XMP) {
                                panel = param.thisObject
                                SLog.r("QsCollapse", "QsCollapse: 拿到 NotificationPanelViewController", 2000L)
                            }
                        }) != null
                    ) n++
                }
                SLog.i("QsCollapse: 面板构造钩子已挂")
            } else {
                SLog.i("QsCollapse: 没找到 " + PANEL)
            }
        }.onFailure { SLog.e("QsCollapse: 面板钩子失败", it) }
        registerViaApplication()
        return n
    }

    class Grab : XMH() {
        override fun afterHookedMethod(param: XMP) {
            runCatching {
                plugin = param.thisObject
                SLog.r("QsCollapse", "QsCollapse: 拿到 OplusSeparateQSPluginImpl", 2000L)
            }
        }
    }

    /** 和 NavPill 一样：必须用真正的 SystemUI Application Context 注册，
     *  用 `getSystemContext()` 会被拒（caller package android 不在 systemui 进程里）。 */
    private fun registerViaApplication(attempt: Int = 0) {
        if (registered) return
        runCatching {
            val at = Class.forName("android.app.ActivityThread")
            val app = at.getMethod("currentApplication").invoke(null) as? Context
            if (app == null) {
                if (attempt < 4) main.postDelayed({ registerViaApplication(attempt + 1) }, 1500L)
                return
            }
            registerWith(app)
        }.onFailure { SLog.e("QsCollapse: currentApplication 失败", it) }
    }

    @JvmStatic
    fun registerWith(c: Context?) {
        if (registered || c == null) return
        runCatching {
            c.registerReceiver(Rcv(), IntentFilter(ACTION), Context.RECEIVER_EXPORTED)
            registered = true
            SLog.r("QsCollapse", "QsCollapse: 收面板广播已注册", 3000L)
        }.onFailure { SLog.e("QsCollapse: 注册广播失败", it) }
    }

    class Rcv : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            main.post { collapseNow(context) }
        }
    }

    /** 面板状态（诊断用）：fraction/isClosing/fullyCollapsed，取不到就返回 "?" */
    @JvmStatic
    fun state(): String {
        val p = plugin ?: return "plugin=null"
        return runCatching {
            val comp = Rt.get(p, "qsPanelViewController") ?: return "comp=null"
            val frac = Rt.get(comp, "expandedFraction")
            val closing = Rt.get(comp, "isClosing")
            val full = runCatching {
                comp.javaClass.getMethod("isFullyCollapsed").invoke(comp)
            }.getOrNull()
            "fraction=" + frac + " closing=" + closing + " fullyCollapsed=" + full
        }.getOrDefault("?")
    }

    private fun collapseNow(ctx: Context?) {
        val p = plugin
        if (p == null) {
            // 插件还没建（控制中心从没打开过）→ 退回无动画那条，至少保证面板会收
            fallback(ctx, "插件未就绪")
            return
        }
        // ★ 3329：**合并模式**下这个插件不是当前面板（`qsPanelViewController == null`），
        //   调 collapseQSPanel 虽然不抛异常，但什么都不会收 → 用户看到「小窗先出、面板后收」。
        //   实测日志：`QsCollapse: 已调 ROM 带动画收起 comp=null`。
        //   控制器为空就直接退回无动画收面板，先保证顺序正确。
        val comp = runCatching { Rt.get(p, "qsPanelViewController") }.getOrNull()
        if (comp == null) {
            // 合并模式：走 NotificationPanelViewController 的带动画收起；失败才退回无动画
            if (collapseMergedAnimated()) return
            fallback(ctx, "合并模式(comp=null)")
            return
        }
        val ok = runCatching {
            val m = p.javaClass.declaredMethods.firstOrNull {
                it.name == "collapseQSPanel" && it.parameterCount == 4
            } ?: return@runCatching false
            m.isAccessible = true
            // (anim=true, 无动画版的第 1 参=false, delayed=false, 回调=null)
            // → OplusQSRootViewComponent.collapseQSPanel(delayed=false, "") → collapseQSPanelInner()
            m.invoke(p, true, false, false, null)
            true
        }.getOrDefault(false)
        if (ok) {
            SLog.k("QsCollapse: 已调 ROM 带动画收起 " + state())
        } else {
            fallback(ctx, "collapseQSPanel 调用失败")
        }
    }

    /** 合并模式：`NotificationPanelViewController.collapse(..., animate=true)` —— 上滑/返回走的同一条 */
    private fun collapseMergedAnimated(): Boolean {
        val p = panel ?: return false
        val ok1 = runCatching {
            p.javaClass.getMethod(
                "collapse",
                java.lang.Boolean.TYPE,
                java.lang.Float.TYPE,
                String::class.java
            ).invoke(p, true, 0f, "oupao")
            true
        }.getOrDefault(false)
        if (ok1) {
            SLog.k("QsCollapse: 合并模式 -> NotificationPanelViewController.collapse(带动画)")
            return true
        }
        val ok2 = runCatching {
            p.javaClass.getMethod(
                "collapse",
                java.lang.Float.TYPE,
                java.lang.Boolean.TYPE,
                java.lang.Boolean.TYPE
            ).invoke(p, 0f, false, true)
            true
        }.getOrDefault(false)
        if (ok2) {
            SLog.k("QsCollapse: 合并模式 -> collapse(FZZ)(带动画)")
            return true
        }
        return false
    }

    private fun fallback(ctx: Context?, why: String) {
        val ok = runCatching {
            val c = ctx ?: return@runCatching false
            val sb = c.getSystemService("statusbar") ?: return@runCatching false
            sb.javaClass.getMethod("collapsePanels").invoke(sb)
            true
        }.getOrDefault(false)
        SLog.k("QsCollapse: 退回无动画收起(" + why + ") ok=" + ok)
    }

    /** system_server 侧用：发一条广播让 SystemUI 去带动画收面板。 */
    @JvmStatic
    fun requestFromSystem(): Boolean = runCatching {
        val c = Cfg.sysContext() ?: return@runCatching false
        c.sendBroadcast(Intent(ACTION).setPackage("com.android.systemui"))
        true
    }.getOrDefault(false)
}
