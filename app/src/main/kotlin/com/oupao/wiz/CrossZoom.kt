package com.oupao.wiz

import android.content.Intent
import android.os.Bundle

/**
 * 应用间跳转 → 小窗打开（system_server 侧）。
 *
 * ## 需求
 *
 * 「我在 A 应用里点一下跳到 B 应用，希望 B 是小窗，不是全屏。」
 * 典型场景：微信/QQ 里点链接 → 用浏览器打开。
 *
 * ## 挂点
 *
 * `ActivityTaskManagerService.startActivity*`（所有 App 的跳转都汇聚到这里，
 * 与 `Idle`（闲鱼定向）共用同一条链路，不重复挂钩）。
 *
 * 本机实测 `startActivityAsUser` 签名（ColorOS 16 / Android 16）：
 * ```
 * (IApplicationThread, String callingPackage, String callingFeatureId,
 *  Intent, String resolvedType, IBinder resultTo, String resultWho,
 *  int requestCode, int startFlags, ProfilerInfo, Bundle bOptions,
 *  int userId, boolean)
 *    0        1                    2               3        4
 *    5        6              7             8          9            10
 * ```
 * → `callingPackage` = 1，`bOptions` = 10。下标由 [Idle.install] 按
 * `parameterTypes` 精确算出后传入。
 *
 * ## ★★ 关键一：必须**原地改**调用方那个 Bundle，改完再还原
 *
 * 两种写法实测对比（同机、同一条「QQ → 浏览器」跳转）：
 *
 * | 写法 | 结果 |
 * |---|---|
 * | 原地 `putInt` 到实参里的 Bundle | ✅ 目标 App 变␣小窗（2234） |
 * | `Bundle(src)` 复制一份再放回实参 | ❌ 全屏（2243） |
 *
 * → 系统认的是**调用方那一个 Bundle 实例**，不能替换成副本。
 *
 * 但原地改会**污染调用方**：那个 Bundle 是微信/QQ 自己持有并会复用的，
 * 它后面再用同一个 Bundle 启动自己的页面时就带着小窗参数 →
 * 「点一次链接，本体也变成小窗」（2234 的实测现象）。
 *
 * 所以配对使用：`before` 里 [apply] 原地加键，`after` 里 [restore] 摘掉。
 * 原方法对 options 的消费是同步的，调用返回后还原安全。
 *
 * ## ★ 关键二：不取消原跳转
 *
 * 只补参数，**不设 `returnEarly`** —— 原方法的权限校验、Intent 解析、
 * 动画编排全部保留。最坏情况只是参数被忽略、照旧全屏，绝不会让任何 App 打不开。
 *
 * （另一条路「取消原调用 + 反射 `OplusZoomWindowManager.startZoomWindow`」
 *  实测在本机不产生小窗，已放弃，不要再试。）
 *
 * ## ★★ 判据：全满足才动手
 *
 * 1. 开关 `oupao_light_open` 打开（默认关；3114 起合并了原 notif_fly / cross_zoom / settings_zoom）；
 * 2. 调用方已知且不在 [SKIP_CALLERS]；
 * 3. 不是桌面图标启动（`MAIN` + `LAUNCHER`）——否则所有图标启动都变小窗；
 * 4. 目标包名可判定（显式 component/package，或隐式 Intent 由 PackageManager 解析）；
 * 5. `caller != target` —— App 自己的页面栈必须原样全屏；
 * 6. 不是「设置类页面 / 选择器 / 安全隐私页」，且目标不是系统应用
 *    （唯一例外：用户应用 → http(s) 网页）。
 *
 * 第 6 条是 2460 加的**通用排除**，见下面 [strictOn] 那一段的注释：
 * 之前只靠「设置包名清单 + caller 判断」，漏掉了大量
 * 「应用内点设置」「系统应用跳另一个系统应用」的场景。
 */
object CrossZoom {

    private const val K_WINDOW_MODE = "extra_window_mode"
    private const val K_SPLASH = "android.activity.splashScreenStyle"

    /** ColorOS「缩放小窗」窗口模式值 */
    private const val ZOOM_MODE = 100

    // 2853：实测的常量
    private const val SHADE_COLLAPSE_DELAY_MS = 60L
    private const val OPLUS_LIST_MANAGER = "com.android.server.OplusListManagerImpl"

    /**
     * 2853：主动收起控制中心 / 通知面板。
     *
     * 开源原文：「由通知中心启动的小窗会被已展开的面板遮挡。
     * 全屏启动由 SystemUI 负责收起面板，zoom 启动不触发该流程，需在注入后主动收起。」
     * —— 这就是「点磁贴后控制中心没有收起动画」的原因。
     */
    /**
     * 2870：是不是「磁贴类」启动（开源 reasonFor 的判据）。
     * 磁贴长按会直接跳设置页，action 形如 android.settings.NFC_SETTINGS，
     * 不含 quicksettings 字样；目标包也常是 com.android.settings / com.oplus.wirelesssettings。
     */
    /** 2876：HookStarter 用的总开关（设置页/磁贴小窗）。 */
    @JvmStatic
    fun enabledForStarter(): Boolean = runCatching { Cfg.crossZoom() }.getOrDefault(true)

    @JvmStatic
    fun isTileLaunch(it: android.content.Intent?): Boolean {
        if (it == null) return false
        val act = it.action ?: ""
        val tgt = it.component?.packageName ?: it.`package` ?: ""
        return act.contains("quicksettings", true) || act.contains("QS_TILE", true) ||
                act.startsWith("android.settings") || tgt.contains("settings", true)
    }

    /**
     * 2870：把 zoom 参数写进 `ActivityStarter` 即将使用的 options，**不取消原启动**。
     * 开源 系统实现.inject() 原样：
     *   original = mOptions ?: mRequest.activityOptions
     *   bundle   = ZoomLaunchSpec.bundle(original)
     *   setActivityOptions(bundle, callingPid, callingUid)  或写回 mOptions 字段
     * 这样系统自己会收控制中心（带动画）并走原生过渡。
     */
    @JvmStatic
    fun injectOptions(starter: Any?, it: android.content.Intent?): Boolean {
        if (starter == null || it == null) return false
        return try {
            val request = runCatching { Rt.get(starter, "mRequest") }.getOrNull()
            val options = runCatching { Rt.get(starter, "mOptions") }.getOrNull()
                ?: request?.let { runCatching { Rt.get(it, "activityOptions") }.getOrNull() }
            val b = runCatching {
                options?.javaClass?.getMethod("toBundle")?.invoke(options) as? android.os.Bundle
            }.getOrNull() ?: android.os.Bundle()
            b.putInt("extra_window_mode", ZOOM_MODE)
            b.putInt("android:activity.mZoomLaunchFlags", ZOOM_FLAGS)
            b.putInt("android.activity.windowingMode", ZOOM_MODE)
            b.putInt("androidx.activity.LaunchScenario", LAUNCH_SCENARIO)
            b.putInt("start_zoom_at_other_side", LAUNCH_AT_RIGHT)
            b.putInt("android.activity.splashScreenStyle", 1)

            val callingPid = (request?.let { runCatching { Rt.get(it, "callingPid") }.getOrNull() } as? Int) ?: 0
            val callingUid = (runCatching { Rt.get(starter, "mCallingUid") }.getOrNull() as? Int)
                ?: (request?.let { runCatching { Rt.get(it, "callingUid") }.getOrNull() } as? Int) ?: 0

            var applied = false
            // 1) setActivityOptions(Bundle, int, int)
            runCatching {
                val m = starter.javaClass.declaredMethods.firstOrNull {
                    it.name == "setActivityOptions" && it.parameterCount == 3 &&
                            it.parameterTypes[0] == android.os.Bundle::class.java
                }
                if (m != null) {
                    m.isAccessible = true
                    m.invoke(starter, b, callingPid, callingUid)
                    applied = true
                }
            }
            // 2) 写回 mOptions 字段
            if (!applied) {
                runCatching {
                    val ao = android.app.ActivityOptions::class.java
                        .getMethod("fromBundle", android.os.Bundle::class.java)
                        .invoke(null, b)
                    var c: Class<*>? = starter.javaClass
                    while (c != null) {
                        val f = runCatching { c!!.getDeclaredField("mOptions") }.getOrNull()
                        if (f != null) {
                            f.isAccessible = true
                            f.set(starter, ao)
                            applied = true
                            break
                        }
                        c = c.superclass
                    }
                }
            }
            SLog.r("XZoom", "XZoom: 磁贴补 opts applied=" + applied, 400L)
            applied
        } catch (t: Throwable) {
            SLog.e("XZoom: injectOptions 失败", t)
            false
        }
    }

    @JvmStatic
    fun collapseShade() {
        runCatching {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                var ok = false
                // 1)常规：StatusBarManager（实测在 system_server 里可能拿不到实例）
                runCatching {
                    val sb = runCatching { Cfg.sysContext() }.getOrNull()?.getSystemService("statusbar")
                    if (sb != null) {
                        sb.javaClass.getMethod("collapsePanels").invoke(sb)
                        ok = true
                    }
                }
                // 2)兜底：直接拿 IStatusBarService binder
                if (!ok) {
                    runCatching {
                        val sm = Class.forName("android.os.ServiceManager")
                        val get = sm.getDeclaredMethod("getService", String::class.java)
                        get.isAccessible = true
                        val binder = get.invoke(null, "statusbar")
                        if (binder != null) {
                            val stub = Class.forName("com.android.internal.statusbar.IStatusBarService" + "$" + "Stub")
                            val m = stub.getMethod("asInterface", android.os.IBinder::class.java)
                            val svc = m.invoke(null, binder)
                            if (svc != null) {
                                svc.javaClass.getMethod("collapsePanels").invoke(svc)
                                ok = true
                            }
                        }
                    }
                }
                SLog.i("XZoom: collapsePanels ok=" + ok)
            }, SHADE_COLLAPSE_DELAY_MS)
        }
    }


    /**
     * 2964（用户要求：**只放行 QQ/微信登录跳转**）：
     *  - 申请授权那一跳：target = QQ/微信（caller 是游戏/任意 App）→ 放行（全屏）；
     *  - 回游戏那一跳：caller = QQ/微信，且 intent 既**不是网页**、action 也**不是 VIEW**
     *    （判定成「登录回调」）。VIEW/网页那两类是「微信/QQ 打开内容」的正常跳转，
     *    仍然要小窗（2908 的 QQ→MT 定位、微信「用浏览器打开」都在这两类里）。
     * 其它 App（支付宝等）暂不放行 —— 用户只点名 QQ/微信。
     */
    @JvmStatic
    fun isSdkLoginHop(it: Intent?, caller: String?, target: String?): Boolean {
        if (caller.isNullOrEmpty() || target.isNullOrEmpty()) return false
        // ★ 3256：**我们自己发起的跳转不是登录**
        //   气泡点开时 `Sug.launch` 用 `com.coloros.smartsidebar` 当 caller 调 startZoomWindow；
        //   微信聊天消息的气泡会带 `Main_UI_From_Deeplink=true` 的 ChattingMainUI 意图，
        //   满足下面第 ③ 条（caller 非登录应用 + target 是微信）→ 被误判成 SDK 登录 → 全屏打开。
        //   实测日志：`XZoom: QQ/微信登录跳转 → 全屏 com.coloros.smartsidebar -> com.tencent.mm`
        //             `XZtake … rsn=sdkLogin`，用户点气泡就全屏了。
        if (caller == "com.coloros.smartsidebar") return false
        // 微信「从通知/气泡进来」的直达聊天意图，也不是登录
        if (it != null && it.hasExtra("Main_UI_From_Deeplink")) return false
        // 2971：应用内跳转、设置页都不是登录跳转
        if (caller == target) return false
        if (isSettingsTarget(target)) return false
        // QQ/微信 SDK 分享不是登录（B 站分享到 QQ 走 mqqapi://share/to_fri）
        if (isSdkShare(it, target)) return false
        val launcherStyle = it != null &&
            (Intent.ACTION_MAIN == it.action || it.hasCategory(Intent.CATEGORY_LAUNCHER))
        // ① 正牌授权请求（微信 SDK SendAuth：_wxapi_command_type = 3）→ 一定是登录
        if (isAuthRequest(it)) return true
        // ② ★ 2972（用户：QQ 分享到微信没法小窗）：
        //   QQ ↔ 微信 互跳（分享 / 互相拉起）**不是登录** → 交给正常小窗判据。
        //   实测：QQ 分享到微信走的是
        //   `com.tencent.mobileqq -> com.tencent.mm/.plugin.base.stub.WXEntryActivity`（和登录同一个组件！），
        //   拿组件名根本分不出来。
        if (LOGIN_APPS.contains(caller) && LOGIN_APPS.contains(target)) return false
        // ③ 申请授权：caller 是游戏/普通 App，target 是 QQ/微信
        if (!launcherStyle && LOGIN_APPS.contains(target)) return true
        // ④ 回游戏那一跳：caller 是 QQ/微信，目标不是网页/VIEW
        if (LOGIN_APPS.contains(caller)) {
            if (isWebLaunch(it)) return false
            if (Intent.ACTION_VIEW == it?.action) return false
            return true
        }
        return false
    }

    /**
     * 2972：是不是**真的授权请求**。
     * 微信 SDK 的登录授权是 `SendAuth`，带 `_wxapi_command_type = 3`
     * 或 `_wxapi_sendauth_req_scope`；分享是 SendMessageToWX（command_type = 1）。
     */
    @JvmStatic
    fun isAuthRequest(it: Intent?): Boolean {
        val ex = it?.extras ?: return false
        return runCatching {
            var hit = false
            for (k in ex.keySet()) {
                if (k.contains("sendauth", true)) {
                    hit = true
                    break
                }
                if (k.contains("command_type", true)) {
                    val v = runCatching { ex.get(k) }.getOrNull()
                    if (v is Int && v == 3) {
                        hit = true
                        break
                    }
                }
            }
            hit
        }.getOrDefault(false)
    }

    /**
     * 2974：这一跳是不是**系统的选择器**（含 ColorOS 的「分身选择器」）。
     * 选择器被放进小窗里看着很怪（用户实测：小窗里弹「选择打开的应用」）。
     */
    @JvmStatic
    fun isChooserHop(intent: Intent?, resolvedPkg: String?): Boolean {
        val CH = setOf(
            "com.oplus.multiapp", "com.coloros.multiapp", "com.android.intentresolver",
            "android", "com.android.internal.app"
        )
        if (resolvedPkg != null && CH.contains(resolvedPkg)) return true
        val pkg = intent?.component?.packageName
        if (pkg != null && CH.contains(pkg)) return true
        if (intent == null) return false
        return runCatching {
            "oplus.intent.action.CHOOSER_MULTI_APP" == intent.action ||
                intent.hasExtra("oplus.intent.extra.target_package") ||
                intent.hasExtra("android.intent.extra.INTENT")
        }.getOrDefault(false)
    }

    // ===== 2977：「刚弹过选择器」的短期记忆 =====
    //
    // 背景：有分身的应用（如微信）会先弹「选择打开的应用」。
    // 两跳的 Intent 完全一样、`mCallingPackage` 都是 null、`activityInfo` 也拿不到
    // → 只能用「顺序」判：
    //   第一跳（待弹选择器）时记下目标包；紧接着同一个目标包再来一次
    //   = 选完分身后的真正启动 → 这时才注入小窗。
    @Volatile private var pickPkg: String? = null
    @Volatile private var pickAt = 0L
    private const val PICK_TTL_MS = 5000L

    @JvmStatic
    fun noteChooserPick(pkg: String?) {
        pickPkg = pkg
        pickAt = android.os.SystemClock.uptimeMillis()
    }

    @JvmStatic
    fun chooserArmed(pkg: String?): Boolean {
        if (pkg == null || pickPkg != pkg) return false
        if (android.os.SystemClock.uptimeMillis() - pickAt > PICK_TTL_MS) {
            pickPkg = null
            return false
        }
        return true
    }

    @JvmStatic
    fun clearChooserPick() {
        pickPkg = null
        pickAt = 0L
    }

    // ===== 2983：**白名单式小窗支持** =====
    //
    // 用户：只有「我们真要小窗」的场景才应该被告知「这个页面能进小窗」，
    // 其余一律回到原厂判定（B站/抖音自己调小窗 API 就不会被放行）。
    // 开窗（fire / 注入）前置位 4s，FlexFix 只在这个窗口里强制放行。
    @Volatile private var zoomArmAt = 0L

    @JvmStatic
    fun armZoomSupport() {
        zoomArmAt = android.os.SystemClock.uptimeMillis()
    }

    @JvmStatic
    fun zoomSupportArmed(): Boolean =
        android.os.SystemClock.uptimeMillis() - zoomArmAt <= 4000L

    // ===== 2979：**全局**——任何系统选择器都不接管（用户：桌面分享到 QQ 的选择器也不能小窗） =====
    //
    // 问题：只有 ATMS 层（HookIdle）能看到真正落点 `com.oplus.multiapp`；
    // `HookXzCtl` / `HookStarter` 拿到的是内层 Intent（目标写着 QQ/微信）→ 还会去注入/inject。
    // 做法：HookIdle 看到「选择器启动」时置位 1.5s，后面几层一律放行。
    @Volatile private var chooserSeenAt = 0L

    @JvmStatic
    fun noteChooserLaunch() {
        chooserSeenAt = android.os.SystemClock.uptimeMillis()
    }

    @JvmStatic
    fun chooserJustSeen(): Boolean {
        val d = android.os.SystemClock.uptimeMillis() - chooserSeenAt
        // 2981：1.5s → 6s。实测「桌面分享」是：
        //   launcher → com.android.intentresolver/.ChooserActivityLauncher（透明中转）
        //   → com.oplus.multiapp/.MultiAppResolverActivity，两者相隔 ~2s，1.5s 早就过期了。
        return d >= 0 && d <= 6000L
    }

    /**
     * 2980：看到「选择器发起的启动」（= 用户刚选完）就**清掉**上面那个标记。
     * 否则「选完后的真正启动」距选择器弹出只有 ~1s，
     * 会被 1.5s 的标记误伤 → 又变全屏。
     */
    @JvmStatic
    fun clearChooserLaunch() {
        chooserSeenAt = 0L
    }

    /** 2976：选择器（分身选择器 / 分享选择器）自己的包名 */
    @JvmStatic
    fun isChooserCaller(pkg: String?): Boolean {
        if (pkg.isNullOrEmpty()) return false
        return pkg == "com.oplus.multiapp" || pkg == "com.coloros.multiapp" ||
            pkg == "com.android.intentresolver" || pkg == "android"
    }

    /** 2974：目标应用是不是**有分身**（有分身就会先弹选择器） */
    @JvmStatic
    fun hasClone(pkg: String?): Boolean = if (pkg.isNullOrEmpty()) false else isCloneTarget(pkg)

    /**
     * 2973：是不是**QQ/微信 SDK 的分享**（发内容给微信/微信发给 QQ）。
     *
     * 实测（：微信 SDK 分享走 `WXEntryActivity`，和登录**同一个组件**，
     * 并且带 `FLAG_ACTIVITY_FORWARD_RESULT`（所以 takeReason 会因 `forwardResult` 放行 → 全屏）。
     * 可靠信号是 extras 里的 `_wxapi_sendmessagetowx_req_*`（以及 SEND 动作）。
     */
    @JvmStatic
    fun isQqShare(it: Intent?, target: String?): Boolean {
        if (target != "com.tencent.mobileqq") return false
        val data = it?.dataString ?: return false
        return data.contains("mqqapi://", true) &&
                (data.contains("/share", true) || data.contains("/forward", true))
    }

    @JvmStatic
    fun isSdkShare(it: Intent?, target: String?): Boolean {
        if (target == null || !LOGIN_APPS.contains(target)) return false
        if (it == null) return false
        // QQ SDK 分享：`mqqapi://share/to_fri?...`，action 是 VIEW（不是 SEND）
        if (isQqShare(it, target)) return true
        if (Intent.ACTION_SEND == it.action || "android.intent.action.SEND_MULTIPLE" == it.action) return true
        val ex = it.extras ?: return false
        return runCatching {
            var hit = false
            for (k in ex.keySet()) {
                if (k.contains("sendmessagetowx", true)) {
                    hit = true
                    break
                }
            }
            hit
        }.getOrDefault(false)
    }

    /**
     * 2969：目标是不是**浏览器**。
     * ——「微信 → 用浏览器打开」这类**还要小窗**，
     * 不能因为它带了显式组件就被归到「App-Link 手接」。
     */
    @JvmStatic
    fun isBrowserPkg(pkg: String?): Boolean {
        if (pkg.isNullOrEmpty()) return false
        if (pkg.contains("browser", true)) return true
        return BROWSER_PKGS.contains(pkg)
    }

    private val BROWSER_PKGS = setOf(
        "com.android.chrome", "com.tencent.mtt", "mark.via", "com.UCMobile", "com.quark.browser",
        "org.mozilla.firefox", "com.microsoft.emmx", "com.heytap.browser", "com.oplus.browser",
        "com.android.browser", "com.mi.globalbrowser", "com.sec.android.app.sbrowser"
    )

    /** QQ / 微信（登录 SDK 的宿主） */
    private val LOGIN_APPS = setOf("com.tencent.mobileqq", "com.tencent.mm")

    /**
     * 2853：ColorOS 控制中心里的**应用图标**由桌面代理启动（caller=launcher），
     * 系统在 OplusListManagerImpl 里记了「这个包来自控制中心」。据此放行，
     * 否则它们会被 SKIP_CALLERS 里的 launcher 一律拦成全屏。
     */
    @JvmStatic
    fun fromControlCenter(pkg: String?): Boolean {
        if (pkg.isNullOrEmpty()) return false
        return runCatching {
            val c = Class.forName(OPLUS_LIST_MANAGER)
            val inst = c.getMethod("getInstance").invoke(null)
            c.getMethod("isFromControlCenterPkg", String::class.java).invoke(inst, pkg) as? Boolean
                ?: false
        }.getOrDefault(false)
    }

    // 2851：ColorOS 小窗启动参数的契约（三项缺一不可）
    private const val K_WINDOWING_MODE = "android.activity.windowingMode"
    private const val K_ZOOM_FLAGS = "android:activity.mZoomLaunchFlags"
    private const val K_LAUNCH_SCENARIO = "androidx.activity.LaunchScenario"
    private const val K_ADJUST_IME = "androidx.activity.AdjustInputMethod"
    private const val ZOOM_FLAGS = 5
    private const val LAUNCH_SCENARIO = 1

    /** 2858：展开方向（开源 LAUNCH_AT_RIGHT）——通知横幅/磁贴小窗的下滑展开动画靠它 */
    private const val LAUNCH_AT_RIGHT = 14

    /** `startZoomWindow` 的第 4 参：调用方标识，模块内现有两处都是这个取值 */
    private const val CALL_PKG = "com.coloros.smartsidebar"

    /**
     * 不接管的调用方：桌面 / 系统UI / 系统框架 / shell / 本模块 / 小窗服务。
     * 桌面必须排除（桌面点图标天然跨应用）。
     */
    /** 2856：桌面类调用方（ColorOS 控制中心里的应用图标由桌面代理启动） */
    /** 2970：调用方是不是桌面（ColorOS 桌面 / AOSP 桌面）。 */
    @JvmStatic
    fun isLauncherLike(pkg: String?): Boolean = pkg != null && LAUNCHER_LIKE.contains(pkg)

    private val LAUNCHER_LIKE = setOf("com.android.launcher", "com.android.launcher3")

    /** 3338：调用方是不是「桌面/桌面插件引擎」——插件点击应当全屏打开，不接管 */
    @JvmStatic
    fun isSkipCaller(pkg: String?): Boolean = pkg != null && SKIP_CALLERS.contains(pkg)

    private val SKIP_CALLERS = setOf(
        "com.android.launcher",
        "com.oplus.launcher",
        "com.coloros.launcher",
        // ⚠️ com.android.systemui 不在这里：控制中心长按 / 通知的设置按钮都由它发起，
        //    用户要求这些能小窗 —— 在下面用「只放行去系统设置页」单独判。
        // ⚠️ "android" 也不在这里：QQ「用其他应用打开」先弹框架 ChooserActivity，
        //    用户选完之后那次启动的 callingPackage 就是 `android`
        //    （日志实证：`XZdiag: android -> com.tencent.mobileqq take=false act=VIEW`）→
        //    之前整包跳过，表现成「选完还是全屏」。
        "com.android.shell",
        "com.oupao.wiz",
        "com.coloros.smartsidebar",
        "com.oplus.zoomwindow",
        // 相机里点刚拍的图（相机 → 相册/看图）不该变成小窗，那是全屏看图场景
        "com.oplus.camera",
        "com.oplus.engineercamera",
        // ★ 3338：**桌面插件引擎**。外部用户实测：点桌面时钟/天气插件（intent 带 appWidgetId）
        //   被接管 → 走「原生小窗通道」抛
        //   `SecurityException: Permission Denial: package=android does not belong to uid=…`
        //   → 最后什么都没打开（用户报「桌面部分插件部分位置点击打不开」）。
        //   插件点击本来就该全屏打开，直接不接管。
        "com.heytap.colorfulengine",
        "com.oplus.widget",
        "com.coloros.widget",
        // ★ 3359+：桌面卡片/时钟宿主（C17 实测：点桌面时钟
        //   `XZCtl: com.nowcasting.activity -> com.coloros.alarmclock take=true`）。
        //   这些不是"应用跨应用跳转"，是桌面卡片点击 → 一律全屏。
        "com.nowcasting.activity",
        "com.nowcasting.service"
    )

    /**
     * 递归护栏：`startZoomWindow` 内部会经 `OplusZoomStarter` 再发一次 startActivity，
     * 那次同样会命中我们的钩子。不挡住会反复接管（并把顶层任务一起卷进小窗）。
     * 用 ThreadLocal：钩子可能落在不同线程。
     */
    private val inZoom = ThreadLocal<Boolean>()

    // ===== 不可缩放页面（resizeableActivity=false）的放行窗口 =====
    //
    // 见 HookResizeFix：这类页面的 Task 建成 nonResizable，小窗服务只返回 0、按全屏显示。
    // fire 之前记下目标组件，4s 内把它的 isResizeableMode/resizeMode 按成可缩放。
    private const val RESIZE_GRACE_MS = 4000L
    /** ActivityInfo.RESIZE_MODE_FORCE_RESIZEABLE */
    private const val RESIZE_MODE_FORCE_RESIZABLE = 4

    @Volatile private var rzPkg: String? = null
    @Volatile private var rzCls: String? = null
    @Volatile private var rzAt = 0L

    /** fire 之前调用：记下这次要小窗化的组件（隐式 Intent 没有 component → 清空，不干预） */
    @JvmStatic
    fun noteResizeTarget(pkg: String?, cls: String?) {
        rzPkg = pkg
        rzCls = cls
        rzAt = if (cls.isNullOrEmpty()) 0L else android.os.SystemClock.uptimeMillis()
    }

    /**
     * 2969：诊断「这一层算出来的 hasResult 对不对」。
     * C17 上 `ActivityRecord.resultTo` 可能在某些层已经为空（实测）。
     */
    @JvmStatic
    fun diagRes(layer: String, caller: String?, target: String?, hasResult: Boolean, args: Array<Any?>?, idx: Int) {
        runCatching {
            var binder = false
            if (args != null) {
                for (a in args) if (a is android.os.IBinder) binder = true
            }
            SLog.r(
                "XZres",
                "XZres: layer=" + layer + " " + caller + " -> " + target +
                    " res=" + hasResult + " idx=" + idx + " binderArg=" + binder +
                    " n=" + (args?.size ?: -1),
                400L
            )
        }
    }

    /** 宽限窗内、且组件匹配 → 该把这条 ActivityInfo 按成可缩放 */
    @JvmStatic
    fun resizeOverrideHit(info: Any?): Boolean {
        if (info == null) return false
        val target = rzCls ?: return false
        val pkg = Rt.get(info, "packageName") as? String ?: return false
        val cls = Rt.get(info, "name") as? String ?: return false
        if (pkg != rzPkg || cls != target) return false
        if (!resizeGraceActive()) return false
        runCatching {
            info.javaClass.getField("resizeMode").setInt(info, RESIZE_MODE_FORCE_RESIZABLE)
        }
        return true
    }

    /** 是否处在「刚 fire 小窗」的宽限窗内（供拿不到组件的静态判据用） */
    @JvmStatic
    fun resizeGraceActive(): Boolean {
        if (Cfg.getProp("persist.oupao.zoom_resize", "1") == "0") return false
        if (rzCls.isNullOrEmpty()) return false
        val now = android.os.SystemClock.uptimeMillis()
        if (now - rzAt > RESIZE_GRACE_MS) {
            rzCls = null
            return false
        }
        return true
    }

    /** 当前待放行的 zoom 目标（诊断用） */
    @JvmStatic
    fun resizeTargetDesc(): String = (rzPkg ?: "-") + "/" + (rzCls ?: "-")

    /** 诊断用：pkg/class/resizeMode */
    @JvmStatic
    fun descOf(info: Any?): String = runCatching {
        (Rt.get(info, "packageName") as? String) + "/" + (Rt.get(info, "name") as? String) +
            " resizeMode=" + Rt.get(info, "resizeMode")
    }.getOrDefault("?")

    /**
     * 目标是「有分身的 App」时不接管。
     *
     * 为什么：这类目标被系统（ColorOS 的分身选主应用对话框）先拦一道，
     * 由用户选「主应用 / 分身应用」。我们若在中间把那次启动取消再自己发小窗，
     * 顺序就乱了 —— 用户实测「分享到 QQ/微信时没等选择就直接把内容分享出去了，
     * 卡一会儿才弹主应用/分身选择框，但内容其实已经发出」。
     *
     * 分身列表由 system_server 的 `FlyH.cloneScan()` 写，格式
     * `名称包名#Activity#userId`（多条拼接），这里只取包名。
     * 结果缓存 [CLONES_TTL]，别每次都读文件。
     */
    private const val CLONES_TTL = 30_000L

    /** 分身选择器所在包：只做「主应用 / 分身应用」的选择，绝不是小窗目标 */
    private val MULTIAPP_PKGS = setOf("com.oplus.multiapp", "com.coloros.multiapp")

    /** 系统设置类包名（SystemUI 长按跳转的主要落点之一） */
    private val SETTINGS_PKGS = setOf(
        "com.android.settings", "com.oplus.settings", "com.coloros.settings",
        "com.oplus.wirelesssettings",   // WiFi / 蓝牙
        "com.android.phone",            // 移动数据 / 运营商设置
        "com.oplus.notificationmanager",// 通知的设置按钮
        "com.oplus.screenrecorder",     // 屏幕录制
        "com.coloros.oshare"            // 互传
    )

    /** SystemUI 发起时允许接管的动作族（控制中心磁贴长按 / 通知设置按钮） */
    private val SYSTEMUI_ACTIONS = listOf(
        "android.settings.",                              // 大部分磁贴
        "android.service.quicksettings.",                 // 第三方磁贴偏好页（QS_TILE_PREFERENCES）
        "oplus.intent.action.SCREENRECORDER",             // 屏幕录制
        "com.oplus.notificationmanager."                  // 通知设置
    )

    /** 选择器类目标：必须保持全屏，不能小窗化（否则用户没法选） */
    private val SKIP_TARGETS = setOf(
        "android",                       // ChooserActivity / ResolverActivity（框架）
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.oplus.appdetail",           // 点 APK 时 ColorOS 会路由到「应用详情」
        "com.oplus.pantanal.seedling",   // 部分机型的安装器实现
        "com.google.android.packageinstaller",
        "com.android.intentresolver",    // 新版系统的 Chooser/Resolver 独立包
        "com.oplus.multiapp",            // ColorOS 主应用/分身选择
        "com.coloros.multiapp"
    )

    // ============ 2460：通用排除（治「不该小窗的也被小窗」）============
    //
    // 之前只有两条相关判据：`caller == target`（保护 App 内跳转）和
    // `SETTINGS_PKGS.contains(caller)`（保护设置内部跳转）。它们都只看**调用方**，
    // 于是下面这些「跨包但仍是同一个系统流程」的场景全被误伤：
    //
    //  - 相册 →「隐私图库」输密码页（安全页，必须全屏）  ← 用户 2026-10-30 报
    //  - 应用内点「设置」/ 系统应用里点某个设置项（设置页是全屏语义）
    //  - 应用内选文件 / 选图片（DocumentsUI 被小窗后根本没法选）
    //
    // 不再逐个补包名，改成四条通用判据（任一命中就保持全屏）：
    //  1) 选择器类动作：GET_CONTENT / OPEN_DOCUMENT / PICK …
    //  2) 安全隐私页：类名带 Password / Privacy / Verify …
    //  3) 设置类页面：动作 `android.settings.*`、目标包名/类名含 settings、
    //     或带 `:settings:show_fragment`（SettingsActivity 的 @hide 键）
    //  4) 目标是系统应用 —— 只放行「用户应用 → http(s) 网页」这一种
    //     （微信/QQ 点链接 → 系统浏览器 仍然小窗；系统应用之间互相跳一律全屏）
    //
    // SystemUI 发起的不受这些约束（控制中心磁贴长按 / 通知设置按钮要能小窗），
    // 它自己有一套更严的放行条件（[SYSTEMUI_ACTIONS] / [SETTINGS_PKGS]）。
    //
    // 兜底开关：`setprop persist.oupao.xz_strict 0` → 退回 2459 的判据。

    private fun strictOn(): Boolean =
        runCatching { Cfg.getProp("persist.oupao.xz_strict", "1") != "0" }.getOrDefault(true)

    /** 目标是不是「系统设置页」（含设置类包名 / 类名带 settings）。2937 给 HookIdle 用。 */
    @JvmStatic
    fun isSettingsTarget(target: String?): Boolean {
        if (target.isNullOrEmpty()) return false
        if (SETTINGS_PKGS.contains(target)) return true
        return target.contains("settings", true)
    }

    /** 选择器类动作：选文件/选图/选账号，塞进小窗用户没法选 */
    private val PICKER_ACTIONS = setOf(
        "android.intent.action.GET_CONTENT",
        "android.intent.action.OPEN_DOCUMENT",
        "android.intent.action.OPEN_DOCUMENT_TREE",
        "android.intent.action.CREATE_DOCUMENT",
        "android.intent.action.PICK",
        "android.intent.action.PICK_ACTIVITY",
        "android.intent.action.GET_RESTRICTED_PROFILE",
        "android.intent.action.CHOOSE_ACCOUNT"
    )

    /** 安全/隐私类页面：类名命中就保持全屏（输密码、隐私空间、身份校验） */
    private val SENSITIVE_HINTS = listOf(
        "Password", "Passcode", "Privacy", "PrivateSpace", "SecureSpace",
        "SecuritySpace", "Verify", "Authenticate"
    )

    private fun isSensitiveScreen(it: Intent): Boolean {
        val cn = it.component?.className ?: return false
        return SENSITIVE_HINTS.any { cn.contains(it, ignoreCase = true) }
    }

    /** 设置类页面（不依赖包名清单，避免再漏） */
    private fun isSettingsLike(it: Intent, target: String?): Boolean {
        val a = it.action ?: ""
        if (a.startsWith("android.settings.")) return true
        if (!target.isNullOrEmpty() &&
            (SETTINGS_PKGS.contains(target) || target.contains("settings", true))
        ) return true
        val cn = it.component?.className
        if (!cn.isNullOrEmpty() && cn.contains("settings", true)) return true
        // SettingsActivity 的 @hide extras：跳系统设置里某个二级页
        return runCatching {
            it.hasExtra(":settings:show_fragment") || it.hasExtra(":settings:show_fragment_title")
        }.getOrDefault(false)
    }

    /** 目标是不是系统应用（预装）。缓存，避免热路径反复 IPC。 */
    private val sysAppCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    @JvmStatic
    fun isSystemApp(pkg: String?): Boolean {
        if (pkg.isNullOrEmpty()) return false
        sysAppCache[pkg]?.let { return it }
        val v = runCatching {
            val pm = Cfg.sysContext()?.packageManager ?: return@runCatching false
            val ai = pm.getApplicationInfo(pkg, 0)
            (ai.flags and (android.content.pm.ApplicationInfo.FLAG_SYSTEM or
                android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
        }.getOrDefault(false)
        if (sysAppCache.size > 512) sysAppCache.clear()
        sysAppCache[pkg] = v
        return v
    }

    /**
     * 3140：是不是「安装器 / 安装界面」这一类目标。
     *
     * 为什么不用纯包名白名单：各家 ROM 的安装器包名不统一
     * （`com.android.packageinstaller` / `com.oplus.packageinstaller` /
     *  `com.google.android.packageinstaller` / 机型自带的 `*installer`），
     * 硬编码必漏；所以按「包名或组件类名包含 installer / appdetail / apkinstall」统一判定，
     * 再加上安装/卸载类 action。
     */
    private val INSTALLER_HINTS = arrayOf(
        "packageinstaller", "packagesinstaller", "installer", "appdetail", "apkinstall"
    )

    private fun isInstallerFlow(it: Intent?, target: String?): Boolean = runCatching {
        val a = it?.action ?: ""
        if (a.startsWith("android.intent.action.INSTALL_PACKAGE") ||
            a.startsWith("android.intent.action.UNINSTALL_PACKAGE") ||
            a == "android.intent.action.DELETE"
        ) return@runCatching true
        val t = (target ?: "").lowercase()
        if (t.isNotEmpty() && INSTALLER_HINTS.any { t.contains(it) }) return@runCatching true
        val cn = (it?.component?.className ?: "").lowercase()
        if (cn.isNotEmpty() && (INSTALLER_HINTS.any { cn.contains(it) } || cn.contains("packageinstall"))) {
            return@runCatching true
        }
        false
    }.getOrDefault(false)

    /**
     * 网页类启动：`VIEW http(s)`，或「在浏览器中搜索」（分享面板里的常见项）。
     * 这是「用户应用 → 系统应用」唯一还允许小窗的形态。
     */
    /** 2946：对外暴露的版本（网页 / WEB_SEARCH）。Gemini 那种 App-Link 手接就是 https VIEW + resultTo。 */
    @JvmStatic
    fun isWebLaunch(it: Intent?): Boolean = it != null && runCatching { isWebIntent(it) }.getOrDefault(false)

    /**
     * 跨应用「打开文件」：`content://` / `file://`。
     *
     * ★ 3359+（C16 外部用户 + C17 本机实测）：
     *   QQ 下载 APK →「其他应用打开」→ MT 管理器，这一下带 `FLAG_GRANT_READ_URI_PERMISSION`。
     *   我们如果把它接管成小窗、并**取消原启动**，系统给目标 App 的临时 URI 授权就没了
     *   → MT 能起来但读不到文件，随后自己 finish（看起来就是"点了没反应"）。
     *   这类启动一律**不接管、不注入**，原样交给系统（授权照常生效）。
     */
    @JvmStatic
    fun isFileOpenIntent(it: Intent?): Boolean {
        val s = it?.data?.scheme ?: return false
        return s == "content" || s == "file"
    }

    private fun isWebIntent(it: Intent): Boolean {
        if (it.action == "android.intent.action.WEB_SEARCH") return true
        if (it.action != Intent.ACTION_VIEW) return false
        val s = it.data?.scheme ?: return false
        return s == "http" || s == "https"
    }

    /** 文本操作：选中文字后的翻译/搜索/处理。 */
    @JvmStatic
    fun isTextIntent(it: Intent?): Boolean {
        val a = it?.action ?: return false
        return a == "android.intent.action.PROCESS_TEXT" ||
                a == "android.intent.action.PROCESS_TEXT_READONLY"
    }

    /**
     * 文本操作里只把「搜索 / 浏览器」类目标小窗化。
     * 翻译、词典、AI 这类可能用 setResult 回传文本的目标继续走原来的放行路径。
     */
    @JvmStatic
    fun isTextSearchTarget(it: Intent?, target: String?): Boolean {
        if (!isTextIntent(it)) return false
        if (isBrowserPkg(target)) return true
        val cn = it?.component?.className ?: return false
        return cn.contains("Search", true) || cn.contains("Browser", true)
    }

    /** 分享面板转发：caller 是框架 Chooser / IntentResolver，或 Intent 本身是 SEND。 */
    private fun isShareScenario(it: Intent, caller: String?): Boolean {
        if (it.action == Intent.ACTION_SEND || it.action == Intent.ACTION_SEND_MULTIPLE) return true
        return caller == "android" || caller == "com.android.intentresolver"
    }

    @Volatile private var cloneAt = 0L
    @Volatile private var cloneSet: Set<String> = emptySet()
    private val clonePkgRe = Regex("([A-Za-z][A-Za-z0-9_.]*)#")

    private fun isCloneTarget(pkg: String): Boolean {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - cloneAt > CLONES_TTL) {
            cloneSet = runCatching {
                clonePkgRe.findAll(Cfg.getStrFresh("clones", ""))
                    .map { it.groupValues[1] }
                    .toSet()
            }.getOrDefault(emptySet())
            cloneAt = now
        }
        return cloneSet.contains(pkg)
    }

    /**
     * 判断这次跳转该不该小窗化。**只判断，无副作用**。
     *
     * @param hasResult 是否 `startActivityForResult`（resultTo 非空）。
     *   这里不再按它跳过 —— 见下面「只接管 ACTION_VIEW」的说明。
     */
    @JvmStatic
    fun shouldTake(it: Intent?, caller: String?, target: String?, hasResult: Boolean = false): Boolean =
        takeReason(it, caller, target, hasResult) == null

    /** 诊断用：返回**未接管的原因**（null = 该接管）。与 [shouldTake] 同一套判据。 */
    @JvmStatic
    fun takeReason(it: Intent?, caller: String?, target: String?, hasResult: Boolean = false): String? {
        if (it == null) return "nullIntent"
        // 2975：**灭屏/锁屏期间不接管任何启动**（省功耗）。
        //   例外：「贴边小窗息屏后继续播放」（`Cfg.screenKeep()`）
        //   靠的是挡住系统的暂停请求（Dock/Hooks 那边），不经过这条判据 → 不受影响。
        if (Cfg.sleepingNow()) return "sleeping"
        // ★ 3338：**桌面插件点击**（intent 带 `appWidgetId`）→ 一律不接管，按原厂全屏打开。
        //   外部用户实测：桌面时钟/天气插件被接管后走「原生小窗通道」，抛
        //   `SecurityException: Permission Denial: package=android does not belong to uid=…`
        //   → 点了什么都不打开（用户报「桌面部分插件部分位置点击打不开」）。
        if (it.hasExtra("appWidgetId")) return "appWidget"

        // ★ 2968（用户要求：**在 2961 基线上只修这两个问题**，别的一概不动）
        //   ① 桌面 → Gemini 不是全屏：
        //      `BardEntryPointActivity`（桌面点开的入口，本身全屏）随后自己发
        //      `startActivityForResult(VIEW https://bard.google.com/android?source=bardshell
        //       -> com.google.android.googlequicksearchbox)`。
        //      这是 App-Link 式的「启动手接」，不是用户要小窗打开的网页 → 一律全屏。
        //   ★ 2969：不能只看 `hasResult` —— 实测（C17）`ActivityRecord.resultTo` 在
        //   `startActivityInner` 那一层已经是空的，于是这条规则在那一层不生效 → 又被 fire 成小窗。
        //   补一个不依赖 resultTo 的信号：**显式指定组件的 VIEW http(s)**
        //   就是 App-Link/启动手接（如 Gemini 的 MainAssistantDeeplinkAnimated）→ 全屏；
        //   隐式的（没组件，走浏览器/选择器）保持小窗。
        if (isWebLaunch(it) && (hasResult || (it.component != null && !isBrowserPkg(target)))) {
            SLog.r("XZoom", "XZoom: 网页手接 → 全屏 res=" + hasResult +
                " comp=" + it.component + " " + caller + " -> " + target, 400L)
            return "webResult"
        }
        //   ② 游戏 → QQ/微信 登录跳转：
        //      QQ/微信登录 SDK 是「游戏 → 授权页 → 用 PendingIntent 把结果送回游戏」。
        //      我们一抢（取消原启动）两端都断：授权页等不到 result 永不 finish。
        //      只放行 QQ/微信这条链（目标=QQ/微信，或 QQ/微信回跳且不是网页/VIEW），
        //      **不**做「带 resultTo 一律放行」那种大范围规则（会把设置页小窗也干掉）。
        if (isSdkLoginHop(it, caller, target)) {
            SLog.r("XZoom", "XZoom: QQ/微信登录跳转 → 全屏 " + caller + " -> " + target, 400L)
            return "sdkLogin"
        }

        // 2856：**实测的 SystemUI 快通道**。
        //   开源在 ActivityStarter 上只看两件事：
        //     ① 来自 SystemUI（callingUid == systemui uid）或来自控制中心代理（launcher + isFromControlCenterPkg）；
        //     ② 目标不是 SystemUI 自己。
        //   满足就直接 zoom（磁贴、通知中心、悬浮横幅点击全走这条）。
        //   *之前*它们被我那堆排除项（resultTo、文档动作、strictOn 的系统应用判定、
        //   SKIP_CALLERS…）挡掉了，所以部分磁贴只能全屏。
        run {
            // 2882：**SystemUI / 控制中心来源一律不归这里管**。
            //   通知点击 → NotifFly（SystemUI 浮窗通道，横幅点开的下滑展开动画靠它）；
            //   磁贴 → HookStarter（ActivityStarter.execute）。
            //   这里只管「跨应用跳转」这一个原有功能。
            // 2934（按 2852 的做法）：
            //   2852 的 takeReason 对 SystemUI 来源**不是一律让路**，而是：
            //     settingsZoom 开着 + （action 属于 SYSTEMUI_ACTIONS 或 target 是设置类包）→ 接管。
            //   我们 2882 改成了「SystemUI 一律 deferToZoomHooks」，所以磁贴只能靠 HookStarter
            //   另一套判据，两套判据一旦不一致就出现「部分磁贴不小窗」。
            if ("com.android.systemui" == caller) {
                if (!Cfg.lightQs()) return "sysuiSwitch"
                val act0 = it.action ?: ""
                val byAct = SYSTEMUI_ACTIONS.any { a -> act0.startsWith(a) }
                if (!byAct && !SETTINGS_PKGS.contains(target)) return "sysuiOther"
            } else {
                val fromCenter = caller != null && LAUNCHER_LIKE.contains(caller) && fromControlCenter(target)
                if (fromCenter) return "deferToZoomHooks"
            }
        }
        // ★ 安装/卸载这类系统流程一律不接管：它们要全屏、而且是 nonResizable，
        //   塞进小窗会渲染不出来 —— 用户实测表现为「点 APK 后安装器打不开」。
        run {
            val a0 = it.action ?: return@run
            if (a0.startsWith("android.intent.action.INSTALL_PACKAGE") ||
                a0.startsWith("android.intent.action.UNINSTALL_PACKAGE") ||
                a0 == "android.intent.action.DELETE"
            ) return "installAction"
        }
        // ★ 3140：安装器 / 安装界面一律全屏。
        //   只靠上面的 action + [SKIP_TARGETS] 里那几个包名总会漏 —— 各家 ROM 的安装器包名不同
        //   （com.android.packageinstaller / com.oplus.packageinstaller / com.google.android.packageinstaller /
        //    机型自带的 *installer*），用户实测「点 APK 后安装界面被开成小窗」就是漏的这种。
        //   这里按「目标包名 / 组件类名里带 installer、appdetail、apkinstall」统一挡掉，fail-open 之外的
        //   最坏情况也只是保持全屏（本来就是安装页该有的样子）。
        if (isInstallerFlow(it, target)) {
            SLog.r("XZoom", "XZoom: 安装器 → 全屏 " + caller + " -> " + target, 400L)
            return "installer"
        }

        // 2962 那条「网页手接 + resultTo → 全屏」已经被上面 2963 的「带 resultTo 一律放行」覆盖，
        //   这里不再单独判（保留注释说明来龙去脉：Gemini 桌面入口就是 resultTo + VIEW https）。

        // 2919：**游戏里的 QQ/微信登录（OAuth）一律不接管**。
        //   实测：游戏跳 QQ/微信登录 → 授权回来游戏报「已取消授权」。
        //   原因和 MT定位那条一样：我们把启动取消掉自己开小窗，授权结果回不到游戏。
        //   而游戏模式下系统本身就会把登录页开成小窗，所以放行反而是对的。
        run {
            if ((it.flags and Intent.FLAG_ACTIVITY_FORWARD_RESULT) != 0) return "forwardResult"
            if (runCatching { Sug.isGameFg() }.getOrDefault(false) && !Cfg.lightGame()) return "gameForeground"
        }

        // 2850：**文档/目录选择类动作不接管**。
        //   它们是「选个位置再回去继续」的交互流程，塞进小窗会让 App 等不到结果，
        //   用户实测表现就是「点了没反应」（QQ下载文件 → MT 管理器「定位所在位置」）。
        run {
            val a1 = it.action ?: return@run
            if (a1 == "android.intent.action.OPEN_DOCUMENT" ||
                a1 == "android.intent.action.OPEN_DOCUMENT_TREE" ||
                a1 == "android.intent.action.GET_CONTENT" ||
                a1 == "android.intent.action.CREATE_DOCUMENT" ||
                a1 == "android.intent.action.OPEN_FILE" ||
                a1.startsWith("android.intent.action.QUICK_VIEW")
            ) return "documentAction"
        }

        // ⚠️ 只排除「选择器」本身，其余动作（VIEW / SEND / WEB_SEARCH / PROCESS_TEXT …）都接管。
        // 用户要求：分享弹窗里选「在浏览器中搜索」这类选项也要小窗。
        // 之前收紧成只允许 VIEW 是因为「取消再重发」会把来源 App 带走，
        // 但那条现象与「应用跳转确认」弹窗叠在一起；现在弹窗已关，重新放开测试。
        if (Intent.ACTION_CHOOSER == it.action) { noteChooser(); return "chooser" }
        if (inZoom.get() == true) return "inZoom"
        if (!Cfg.crossZoom()) return "switch"
        if (caller.isNullOrEmpty()) return "noCaller"
        if (SKIP_CALLERS.contains(caller)) {
            // 自测用（默认关）：只有命中跳过集合时才去读属性，正常路径零开销
            //   persist.oupao.xz_allow_shell=1 → 放行 com.android.shell
            //   persist.oupao.xz_allow_self=1  → 放行本模块（XzTest / XzAct）
            val allowShell = Cfg.getProp("persist.oupao.xz_allow_shell", "0") == "1"
            val allowSelf = Cfg.getProp("persist.oupao.xz_allow_self", "0") == "1"
            val allowed = (allowShell && caller == "com.android.shell") ||
                (allowSelf && caller == "com.oupao.wiz")
            // 2853：控制中心里的应用图标由桌面代理启动 → 不能当普通 launcher 跳转挡掉
            if (!allowed && !fromControlCenter(target)) return "skipCaller"
        }
        if (isHomeLaunch(it)) return "home"
        if (target.isNullOrEmpty()) return "noTarget"
        // caller == target 默认不接管（保护应用内跳转）；
        // 但「选择器转发」也是这种形态（见 noteChooser），时间窗内放行。
        if (caller == target && !withinChooserWindow()) return "sameApp"
        // ★ 3359+：**跨应用**的文件打开（content:// / file://）不接管（取消原启动会丢 URI 授权）。
        //   同应用的 content:// 内部跳转（如日历待办卡片 → CreateEventOrTaskActivity）
        //   已经在上面返回 sameApp，不会走到这里、也不会被注入小窗参数。
        if (isFileOpenIntent(it)) return "fileOpen"
        // 设置类应用**内部的页面跳转**（如 设置 → 电池/存储）一律不接管，保持全屏。
        // 实测：从 设置 点「电池」被小窗打开了（电池页在 com.oplus.battery 之类另一个包里，
        // 所以上面的 sameApp 判据挡不住）。
        if (!withinChooserWindow() && SETTINGS_PKGS.contains(caller)) return "settingsCaller"
        // ★ SystemUI 发起的启动：只接管「去系统设置页」这一类
        //   （控制中心长按任意按钮、通知右下的设置按钮，都走 android.settings.* 动作）。
        //   其它（通知点击、媒体输出选择等）保持全屏，避免和「通知点击小窗」功能打架。
        if (caller == "com.android.systemui") {
            if (!Cfg.lightQs()) return "sysuiSwitch"
            val a = it.action ?: ""
            val byAction = SYSTEMUI_ACTIONS.any { a.startsWith(it) }
            if (!byAction && !SETTINGS_PKGS.contains(target)) return "sysuiOther"
        }

        // 系统选择器（分享/打开方式弹窗）不是"真应用"，把它塞进小窗只会添乱
        if (Intent.ACTION_CHOOSER == it.action) return "chooser2"
        // ★ 选择器类目标一律不接管（实测踩过：它们被小窗化后用户根本没法选）
        //   - `android`：框架里的 `com.android.internal.app.ChooserActivity`
        //     —— 微信/QQ 分享时那个「打开方式」列表就是它
        //     （日志实证：`XZtake: com.tencent.mm -> android take=true act=android.intent.action.SEND`）
        //   - `com.android.intentresolver`：新版系统把 Chooser/Resolver 独立成这个包
        //   - `com.oplus.multiapp`：ColorOS 的「主应用 / 分身应用」选择器
        if (SKIP_TARGETS.contains(target)) {
            noteChooser()          // 记下「刚弹过选择器」，供随后的转发启动使用
            return "skipTarget"
        }

        // ===== 2460：通用排除（详见 [strictOn] 上方注释）=====
        // ⚠️ 必须放在选择器处理**之后**：目标若是 `android`（Chooser）这类系统包，
        //    上面的 skipTarget 分支还要靠它打 noteChooser 时间戳。
        // SystemUI 已经在上面的白名单里放行过，这里不再拦它。
        if (strictOn() && caller != "com.android.systemui") {
            // 1) 选文件/选图/选账号：小窗里没法选
            if (it.action?.let { a -> PICKER_ACTIONS.contains(a) } == true) return "picker"
            // 2) 输密码 / 隐私空间 / 身份校验：必须全屏
            if (isSensitiveScreen(it)) return "sensitive"
            // 3) 设置类页面：**设置类应用/系统应用之间**跳设置页全屏。
            //    2936（用户：「微信设置→消息通知设置，跳的系统设置页不是小窗」）：
            //    第三方应用（微信/QQ…）点「通知设置」跳系统设置 → **允许小窗**，
            //    所以只有 caller 自己也是设置/系统应用时才拒。
            val settingsLike = isSettingsLike(it, target)
            if (settingsLike && (SETTINGS_PKGS.contains(caller) || isSystemApp(caller))) return "settingsTarget"
            // 4) 目标是系统应用：只放行「网页类」启动
            //    （微信/QQ/短信 里点链接 → 系统浏览器 仍小窗；其余
            //      「系统应用之间互相跳」一律全屏 —— 这条同时兜住了
            //      「相册 → 隐私图库」这类跨包安全页）
            //    上面已经把「第三方应用 → 设置页」放行了，这里不能再拦回去。
            if (!settingsLike && isSystemApp(target) && !isWebIntent(it) && !isTextIntent(it)) return "sysTarget"
        }
        // ★ 带 URI 授权的内容分享（发文件/图片/视频）不接管。
        //
        // 原因：分享文件时 ATMS 会临时把 `content://` 的读取权限授给目标应用，
        // 这份授权是**跟着那次启动**走的。我们取消原启动再自己发一遍
        // → 授权没带过去 → 用户实测 QQ 提示「文件不存在」、微信「获取资源失败」。
        //
        // 判定：带 GRANT_READ/WRITE_URI_PERMISSION，或 clipData/EXTRA_STREAM 是 content://
        // 开了「带授权通道」时不再跳过：fire() 会用 startZoomWindowWithGrants 送授权
        if (!grantsOn()) {
            // 只看「真的有 content:// 内容」才跳过。
            // 之前还看 FLAG_GRANT_READ_URI_PERMISSION，但很多 App 对纯链接也会带这个 flag
            // （实测 QQ 用其他应用打开 → 浏览器就被它误伤成全屏），去掉。
            if (contentUris(it).isNotEmpty()) return "uriGrant"
        }

        // 目标有分身 → **第一次不接管**，让系统照常弹「主应用 / 分身应用」。
        //
        // 为什么不能连 VIEW 一起接管（实测教训）：
        //   我们对 QQ/微信这类目标先 fire 一次小窗，小窗服务内部又会撞上分身选择器，
        //   于是变成「小窗启动 → 弹选择器 → 选完再小窗启动一次」= **两次启动**，
        //   用户观感就是「慢，和最早微信那种慢一样」。
        //
        // ★ 例外：调用方是分身选择器（`com.oplus.multiapp`）时，说明用户**已经选完了**
        //   （日志实证：`XZdiag: com.oplus.multiapp -> com.tencent.mm take=true`），
        //   这一次正该接管 —— 全流程只启动一次，既快又是小窗。
        val fromMultiApp = caller != null && MULTIAPP_PKGS.contains(caller)
        if (!fromMultiApp && isCloneTarget(target)) return "clone"

        // ===== 轻量打开：按场景细分（默认值保持 3466 行为） =====
        // 文本操作（翻译/搜索）：默认开
        if (isTextIntent(it)) {
            return if (Cfg.lightText()) null else "textOff"
        }
        // 跳到系统设置页：默认开（优先于分享判定）
        if (isSettingsLike(it, target)) {
            return if (Cfg.lightJumpSettings()) null else "settingsOff"
        }
        // 分享面板转发：默认开
        if (isShareScenario(it, caller)) {
            return if (Cfg.lightShare()) null else "shareOff"
        }
        // 其余普通应用间跳转：默认开（保持 3466）
        return if (Cfg.lightJumpThird()) null else "thirdOff"
    }

    /**
     * 真正能出小窗的通道：`OplusZoomWindowManager.startZoomWindow`。
     *
     * ## 为什么不用「往 bOptions 写参数」
     *
     * 那条路在真机上是**看调用方身份**的：
     *  - 用 `am start`（callingPackage=com.android.shell，特权 uid）发起 → 参数被采纳，出小窗 ✅
     *  - 用真实 App（模块自身，普通 uid）发起 → 参数被忽略，照旧全屏 ❌
     * 两次 A/B 都验证过（same target、same url、only caller differs）。
     *
     * 所以对「微信/QQ 里点链接」这种真实场景，写 Bundle 靠不住。
     *
     * ## startZoomWindow 不受调用方影响
     *
     * 它由 **system_server 自己**（本钩子所在进程，uid 1000）经 `OplusActivityManager`
     * 调用 `OplusZoomWindowManagerService`，走的是小窗专用链路，
     * 目标 Activity 直接以缩放窗启动（实测 frame=[182,304][1398,2466]，
     * 全屏是 [0,0][1216,2640]，与模块内 `FlyH.go` 造出的小窗完全一致）。
     *
     * 参数沿用 `FlyH.go` / `Sug.launchTail`：`extra_window_mode=100`
     * + `android.activity.splashScreenStyle=1`，callPkg 用 `com.coloros.smartsidebar`。
     *
     * @return true = 已交给小窗（调用方必须取消原跳转）；false = 失败，请放行原方法
     */
    @JvmStatic
    fun fire(it: Intent, userId: Int = 0, atms: Any? = null, origin: String = "-"): Boolean =
        fire(it, userId, atms?.javaClass?.classLoader, sysCtxOf(atms), origin)

    /** 同上，直接给 ClassLoader（ActivityStarter 层拿不到 ATMS 实例时用） */
    /**
     * 2857：小窗启动的**唯一出口**。
     *
     * 之前把 collapseShade() 挂在 HookXzTake 里，实测大量走的是 HookXzPath（栈里
     * 明显是 HookXzPath.beforeHookedMethod）→ 收面板从没执行过。
     * 开源也是在它自己的 zoom 入口里统一处理，所以放在这里。
     */
    @JvmStatic
    fun fire(
        it: Intent,
        userId: Int,
        cl: ClassLoader?,
        sysCtx: android.content.Context? = null,
        origin: String = "-"
    ): Boolean {
        // 2873：**不再在这里主动收面板**。
        //   开源 preemptWithZoomWindow 直调后就 return 0，靠 SystemUI 自己收（带动画）；
        //   我们在这里多收一次反而会把动画干掉。
        return fireInner(it, userId, cl, sysCtx, origin)
    }

    private fun fireInner(
        it: Intent,
        userId: Int,
        cl: ClassLoader?,
        sysCtx: android.content.Context? = null,
        origin: String = "-"
    ): Boolean {
        // ★ 3359+：文件打开（content:// / file://）**绝不**走小窗直调 —— 会丢 URI 临时授权。
        if (isFileOpenIntent(it)) {
            SLog.r("XZoom", "XZoom: fileOpen 不接管 " + it.data, 800L)
            return false
        }
        armZoomSupport()
        // 小窗 A 里点链接打开 B：先记住 A（以及当前其它小窗）的 taskId。
        // B 真正出现后由 ZoomGate 收掉来源窗；B 没起来时不动 A（fail-open）。
        val oldIds = ZoomGate.snapshotLiveTaskIds()
        fun finishZoom(ok: Boolean): Boolean {
            if (ok) {
                val tp = targetPkg(it) ?: it.component?.packageName ?: it.`package`
                ZoomGate.closeOldAfterCrossZoom(oldIds, tp, userId)
            }
            return ok
        }
        val t0 = android.os.SystemClock.uptimeMillis()
        // ★ 2969：**fire 无条件打日志**。
        //   以前只在 `diagOn()` 打，结果他机日志里看不到到底是谁 fire 的。
        SLog.i("XZoom: fire origin=" + origin + " u=" + userId + " act=" + it.action +
            " comp=" + it.component + " dat=" + it.dataString?.take(70) +
            " flags=0x" + Integer.toHexString(it.flags))
        inZoom.set(true)
        try {
            // ⚠️ 必须带 NEW_TASK：实测去掉后 `startZoomWindow` 建不出小窗 task，
            // 直接退回全屏（2262 实测「变成全屏打开了」）。
            // 虽然它会与微信 Intent 自带的 CLEAR_WHEN_TASK_RESET 叠加，
            // 但这是能出小窗的前提，不能删。
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            val opts = android.app.ActivityOptions.makeBasic()
            opts.setLaunchDisplayId(0)
            val b = opts.toBundle()
            // 2851：按系统的 zoom 参数契约把参数补全。
            //   它的注释写得很明白：**窗口模式 100 + zoomFlags 5 + LaunchScenario 1**
            //   三项必须同时存在，任一缺失 ActivityStarter 就退回窗口模式 0（全屏）。
            //   我们之前只有 extra_window_mode + splash 两项、且 FlyH 用的是
            //   mZoomLaunchFlags=4 —— 这就是「部分磁贴/通知点开只能全屏」的原因。
            b.putInt(K_WINDOW_MODE, ZOOM_MODE)                       // extra_window_mode = 100
            b.putInt(K_WINDOWING_MODE, ZOOM_MODE)                    // android.activity.windowingMode = 100
            b.putInt(K_ZOOM_FLAGS, ZOOM_FLAGS)                       // android:activity.mZoomLaunchFlags = 5
            b.putInt(K_LAUNCH_SCENARIO, LAUNCH_SCENARIO)             // androidx.activity.LaunchScenario = 1
            b.putBoolean(K_ADJUST_IME, true)                         // androidx.activity.AdjustInputMethod
            b.putInt(K_SPLASH, 1)                                  // splashScreenStyle = 1
            // 2858：对齐系统 zoomBundle() 的**展开方向**参数。
            //   没有它时，系统按默认直接弹出小窗；
            //   带上它才会走「从屏幕下方/另一侧滑开」的过渡动画。
            //   LAUNCH_AT_RIGHT = 14（开源 系统实现.LAUNCH_AT_RIGHT）
            b.putInt("start_zoom_at_other_side", LAUNCH_AT_RIGHT)

            // 目标若是 resizeableActivity=false 的页面，先打开「可缩放」宽限窗（见 HookResizeFix）
            noteResizeTarget(it.component?.packageName, it.component?.className)

            // ★★ 不可缩放的页面（resizeableActivity=false）走「原生小窗启动」通道：
            //    system_server 上下文直接 startActivity + ActivityOptions(windowingMode=100,
            //    mZoomLaunchFlags=4)。这就是模块里打开小窗一直在用的写法
            //    （FlyH.java 375-384），也是 ColorOS 自己「启动即小窗」走的那条。
            //    实测：对这些页面 `OplusZoomWindowManagerService.startZoomWindow`
            //    会返回 0 但最终按全屏显示（Task nonResizable），必须换这条通道。
            if (isNonResizableTarget(it)) {
                val ok2 = startViaSysContext(it, sysCtx)
                if (diagOn()) {
                    SLog.i(
                        "XZoom: 原生小窗通道 t=${android.os.SystemClock.uptimeMillis()}" +
                            " ok=$ok2 act=${it.action} comp=${it.component} 耗时=" +
                            (android.os.SystemClock.uptimeMillis() - t0) + "ms"
                    )
                }
                if (ok2) {
                    return finishZoom(true)
                }
            }

            // 直接拿 system_server 里的服务单例并**直接调它的实现**。
            // 方法/实例都缓存，避免每次 Class.forName（热路径）。
            if (ensureSvc(cl)) {
                val m = svcMethod
                val inst = svcInst
                if (m != null && inst != null) {
                    val r = runCatching { m.invoke(inst, it, b, userId, CALL_PKG) }
                        .onFailure { t -> SLog.e("XZoom: 服务直调异常 cause=" + (t.cause ?: t)) }
                        .getOrNull()
                    val ok = (r as? Int)?.let { v -> v >= 0 } ?: false
                    if (true) {   // 2969：不再只在 diag 打
                        // ret 打出来（含负数/异常），用来回答「take=true 但为什么还是全屏」
                        SLog.i("XZoom: 服务直调 origin=$origin t=${android.os.SystemClock.uptimeMillis()}" +
                            " ret=$r u=$userId ok=$ok act=${it.action} 耗时=" +
                            (android.os.SystemClock.uptimeMillis() - t0) + "ms")
                    }
                    if (ok) {
                        return finishZoom(true)
                    }
                }
            }

            // 回退：客户端 OplusZoomWindowManager（跨进程，需 OPLUS_COMPONENT_SAFE，通常被拒）
            val okFallback = runCatching {
                val zc = Class.forName("com.oplus.zoomwindow.OplusZoomWindowManager")
                val inst = Rt.invoke(Rt.methodByName(zc, "getInstance", 0), null, null)
                    ?: return@runCatching false
                val m = Rt.methodByName(zc, "startZoomWindow", 4) ?: return@runCatching false
                val ret = runCatching { m.isAccessible = true; m.invoke(inst, it, b, userId, CALL_PKG) }
                    .onFailure { t -> SLog.e("XZoom: 客户端调用异常 cause=" + (t.cause ?: t)) }
                    .getOrNull()
                (ret as? Int)?.let { v -> v >= 0 } ?: false
            }.getOrDefault(false)
            return finishZoom(okFallback)
        } finally {
            inZoom.set(false)
        }
    }

    /**
     * 给**这次启动自己的** ActivityOptions 写 zoom 键，让系统按小窗启动 ——
     * 与 `NotifFly` 在 SystemUI 里做的完全一样（那套键是实测能出小窗的）：
     * `extra_window_mode=100` / `android:activity.mZoomLaunchFlags=5` /
     * `androidx.activity.LaunchScenario=1` / `android:activity.windowingMode=100`。
     *
     * 与「取消原启动 + 自己发一遍」相比，这条不打断调用方、不重建 task，
     * 控制中心磁贴这类 SystemUI 发起的启动实测能直接进小窗（含不可缩放页面）。
     *
     * @return true = 已写入（调用方**不要**再取消原启动）
     */
    @JvmStatic
    fun bundleZoom(args: Array<Any?>?, optsIdx: Int, it: Intent?): Boolean = runCatching {
        if (Cfg.getProp("persist.oupao.zoom_bundle", "1") == "0") return@runCatching false
        if (args == null || it == null) return@runCatching false
        var idx = optsIdx
        if (idx < 0 || idx >= args.size || args[idx] !is Bundle) {
            idx = args.indexOfFirst { a -> a is Bundle }
        }
        if (idx < 0) return@runCatching false
        val b = (args[idx] as? Bundle) ?: Bundle().also { nb -> args[idx] = nb }
        b.putInt("extra_window_mode", 100)
        b.putInt("android:activity.mZoomLaunchFlags", 5)
        b.putInt("androidx.activity.LaunchScenario", 1)
        b.putInt("android:activity.windowingMode", 100)
        if (diagOn()) {
            SLog.i("XZoom: 已写入 zoom 键（bundle 通道）idx=" + idx + " act=" + it.action +
                " comp=" + it.component)
        }
        true
    }.onFailure { SLog.e("XZoom: 写入 zoom 键异常", it) }.getOrDefault(false)

    /** 递归护栏查询（供 ActivityStarter 层钩子用） */
    @JvmStatic
    fun inZoomNow(): Boolean = inZoom.get() == true

    // ===== 不可缩放页面的第二条通道 =====

    /** `ActivityInfo.RESIZE_MODE_RESIZEABLE` / `..._VIA_SDK_VERSION`，其余都当成「不可缩放」 */
    private const val RESIZE_MODE_RESIZEABLE = 2
    private const val RESIZE_MODE_RESIZEABLE_SDK = 3

    private val rzCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    /** 目标组件是否声明了不可缩放（显式 component 才判；隐式一律当可缩放） */
    @JvmStatic
    fun isNonResizableTarget(it: Intent?): Boolean = runCatching {
        val comp = it?.component ?: return false
        val key = comp.packageName + "/" + comp.className
        rzCache[key]?.let { return@runCatching it }
        val pm = Cfg.sysContext()?.packageManager ?: return@runCatching false
        val ai = pm.getActivityInfo(comp, 0) ?: return@runCatching false
        val rm = (Rt.get(ai, "resizeMode") as? Int) ?: return@runCatching false
        val v = rm != RESIZE_MODE_RESIZEABLE && rm != RESIZE_MODE_RESIZEABLE_SDK
        rzCache[key] = v
        v
    }.getOrDefault(false)

    /**
     * 原生小窗启动：system_server 上下文 + `windowingMode=100` / `mZoomLaunchFlags=4`。
     * 与 [FlyH] 打开小窗用的是同一套参数（FlyH.java 375-384）。
     */
    private fun startViaSysContext(it: Intent, sysCtx: android.content.Context?): Boolean =
        runCatching {
            // ⚠️ 必须用 **system_server 自己的** Context（uid 1000）。
            //   `Cfg.sysContext()` 在别的进程里拿到的是那个进程的上下文 →
            //   实测报 `SecurityException: Permission Denial: package=android does not belong to uid=10249`。
            val ctx = sysCtx ?: systemContext() ?: Cfg.sysContext() ?: return@runCatching false
            val b2 = Bundle()
            b2.putInt("android.activity.windowingMode", 100)
            b2.putInt("android:activity.mZoomLaunchFlags", 4)
            ctx.startActivity(it, b2)
            if (diagOn()) {
                SLog.i("XZoom: 原生小窗通道用的 ctx=" + ctx.packageName + " uid=" +
                    android.os.Process.myUid())
            }
            true
        }.onFailure { SLog.e("XZoom: 原生小窗通道异常", it) }.getOrDefault(false)

    /** ATMS 实例上的 mContext（system_server 自己的 Context） */
    fun sysCtxOf(atms: Any?): android.content.Context? =
        runCatching { Rt.get(atms, "mContext") as? android.content.Context }.getOrNull()

    /** 兜底：ActivityThread.currentActivityThread().getSystemContext()（@hide，反射取） */
    private fun systemContext(): android.content.Context? = runCatching {
        val c = Class.forName("android.app.ActivityThread")
        val at = c.getMethod("currentActivityThread").invoke(null) ?: return@runCatching null
        at.javaClass.getMethod("getSystemContext").invoke(at) as? android.content.Context
    }.getOrNull()

    /**
     * 诊断开关：`setprop persist.oupao.xz_diag 1` 才输出 XZ 系列详细日志与调用栈。
     *
     * 默认关闭 —— `SLog` 是**写文件**的、`Thread.getStackTrace()` 也很贵，
     * 放在每次启动的热路径上会明显拖慢跳转（用户实测「打开很慢」）。
     */
    @JvmStatic
    fun diagOn(): Boolean = runCatching {
        Cfg.getProp("persist.oupao.xz_diag", "0") == "1"
    }.getOrDefault(false)

    // ================= 反射缓存（热路径，绝不能每次 Class.forName） =================

    @Volatile private var svcInst: Any? = null
    @Volatile private var svcMethod: java.lang.reflect.Method? = null
    @Volatile private var svcTried = false
    @Volatile private var svcCl: ClassLoader? = null
    /** 5 参数的 startZoomWindow（最后一个参数是 NeededUriGrants）—— 发内容时用 */
    @Volatile private var svcGrantsMethod: java.lang.reflect.Method? = null

    /**
     * 「带 URI 授权」通道是否启用：`setprop persist.oupao.xz_grants 1`
     *
     * 背景：发文件/图片时 ATMS 会给目标一份 content:// 临时读权限，
     * 而普通的 `startZoomWindow` 不带这份授权 → 目标读不到文件
     * （用户实测 QQ「文件不存在」、微信「获取资源失败」）。
     * 服务里有 `startZoomWindowWithGrants(..., NeededUriGrants)` 重载，
     * 我们自己把授权对象建出来传进去，就能既小窗又能读文件。
     *
     * ⚠️ 默认关闭：这是新通道，先在真机上试；失败会退回全屏（不会丢启动）。
     */
    private fun grantsOn(): Boolean = runCatching {
        Cfg.getProp("persist.oupao.xz_grants", "0") == "1"
    }.getOrDefault(false)

    /** 取（并缓存）`OplusZoomWindowManagerService.sInstance` 与它的 startZoomWindow 方法 */
    private fun ensureSvc(cl: ClassLoader?): Boolean {        svcMethod?.let { return true }
        if (svcTried && svcInst == null && svcMethod == null) {
            // 已经试过且失败；允许在换 classloader 后再试一次
        }
        return runCatching {
            val c = Class.forName("com.android.server.wm.OplusZoomWindowManagerService", false, cl)
            val f = c.getDeclaredField("sInstance")
            f.isAccessible = true
            val inst = f.get(null) ?: return@runCatching false
            val m = c.getMethod(
                "startZoomWindow",
                Intent::class.java, Bundle::class.java, Int::class.java, String::class.java
            )
            m.isAccessible = true
            svcInst = inst
            svcMethod = m
            svcCl = cl
            svcGrantsMethod = c.declaredMethods.firstOrNull {
                it.name == "startZoomWindow" && it.parameterTypes.size == 5 &&
                    it.parameterTypes[4].name.endsWith("NeededUriGrants")
            }
            svcTried = true
            true
        }.onFailure { SLog.e("XZoom: 取 sInstance 失败", it) }.getOrDefault(false)
    }

    /**
     * 收起通知/控制中心面板。
     *
     * 为什么需要：控制中心磁贴长按原本是「面板收起 → 打开设置页」，
     * 收起这一步是 SystemUI 在**它自己发起的那次启动**里做的。
     * 我们把那次启动取消、改成自己发小窗后，没人收面板 →
     * 用户看到「小窗先弹出来，面板过一会儿才收」= 顺序反了。
     *
     * 这里在 fire 之前同步调一次 `IStatusBarService.collapsePanels()`。
     */
    @JvmStatic
    fun collapsePanels(): Boolean = runCatching {
        val smc = Class.forName("android.os.ServiceManager", false, null)
        val binder = smc.getMethod("getService", String::class.java).invoke(null, "statusbar")
            ?: return@runCatching false
        val stub = Class.forName(
            "com.android.internal.statusbar.IStatusBarService\$Stub", false, null
        )
        val svc = stub.getMethod("asInterface", android.os.IBinder::class.java)
            .invoke(null, binder) ?: return@runCatching false
        svc.javaClass.getMethod("collapsePanels").invoke(svc)
        true
    }.onFailure { SLog.e("XZoom: collapsePanels 失败", it) }.getOrDefault(false)

    /**
     * 2954：收控制中心（**带动画**）。
     *
     * `IStatusBarService` 只有无动画的 `collapsePanels()`（这是 2893–2932「控制中心直接消失」
     * 的根因）。带动画的实现在 SystemUI 的分离式控制中心插件里
     * `OplusSeparateQSPluginImpl.collapseQSPanel(anim,…)`，system_server 够不到 →
     * 改成发广播 `com.oupao.wiz.QS_COLLAPSE`，由 SystemUI 侧的 `QsCollapse` 去调。
     *
     * `persist.oupao.qs_anim=0` 可退回老的无动画路径（对比用）。
     */
    @JvmStatic
    fun collapsePanelsAnimated(): Boolean {
        val mode = runCatching { Cfg.getProp("persist.oupao.qs_anim", "1") }.getOrDefault("1")
        if (mode == "0") return collapsePanels()
        if (QsCollapse.requestFromSystem()) {
            SLog.k("XZoom: 控制中心收面板 -> SystemUI(带动画)")
            return true
        }
        return collapsePanels()
    }

    /**
     * 这次启动是不是「控制中心/磁贴」来源。
     * 实测（2893/2934）磁贴启动的调用方包名就是 `com.coloros.smartsidebar`
     * （分离式控制中心挂在这个包名下）；`start_from_systemui_qs` 那条老标记也认。
     */
    @JvmStatic
    fun isTileCaller(caller: String?, intent: android.content.Intent? = null): Boolean {
        if (intent != null && runCatching {
                intent.getBooleanExtra("start_from_systemui_qs", false)
            }.getOrDefault(false)
        ) return true
        return caller == "com.coloros.smartsidebar"
    }

    /** 取出 Intent 里所有 content:// URI（clipData + EXTRA_STREAM） */    private fun contentUris(it: Intent): List<android.net.Uri> {
        val out = ArrayList<android.net.Uri>(2)
        runCatching {
            it.clipData?.let { cd ->
                for (i in 0 until cd.itemCount) {
                    cd.getItemAt(i).uri?.let { u -> if (u.scheme == "content") out.add(u) }
                }
            }
        }
        runCatching {
            when (val st = it.getParcelableExtra<android.os.Parcelable>(Intent.EXTRA_STREAM)) {
                is android.net.Uri -> if (st.scheme == "content") out.add(st)
                is java.util.ArrayList<*> -> st.forEach { x ->
                    if (x is android.net.Uri && x.scheme == "content") out.add(x)
                }
            }
        }
        return out
    }

    /** 目标包在指定用户下的 uid（构造 NeededUriGrants 用） */
    private fun pkgUid(pkg: String, userId: Int): Int = runCatching {
        val ag = Class.forName("android.app.AppGlobals", false, svcCl)
        val ipm = ag.getMethod("getPackageManager").invoke(null) ?: return@runCatching -1
        ipm.javaClass.getMethod(
            "getPackageUid", String::class.java,
            java.lang.Long.TYPE, Integer.TYPE
        ).invoke(ipm, pkg, 0L, userId) as Int
    }.getOrDefault(-1)

    /**
     * 构造 `NeededUriGrants(pkg, uid, flags)` 并逐条 add(uri)。
     * 失败返回 null → 调用方退回普通通道（全屏），绝不丢启动。
     */
    private fun buildGrants(it: Intent, pkg: String?, userId: Int): Any? = runCatching {
        val gm = svcGrantsMethod ?: return@runCatching null
        if (pkg.isNullOrEmpty()) return@runCatching null
        val uris = contentUris(it)
        if (uris.isEmpty()) return@runCatching null
        val gc = gm.parameterTypes[4]
        val ctor = gc.declaredConstructors.firstOrNull { it.parameterTypes.size == 3 }
            ?: return@runCatching null
        val uid = pkgUid(pkg, userId)
        if (uid <= 0) return@runCatching null
        val flags = it.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        val g = ctor.newInstance(pkg, uid, flags)
        val add = gc.methods.firstOrNull { it.name == "add" && it.parameterTypes.size == 1 }
            ?: return@runCatching null
        uris.forEach { u -> add.invoke(g, u) }
        g
    }.getOrNull()

    /**
     * 最近一次「系统选择器」出现的时间。
     *
     * 用途：框架的 ChooserActivity 把用户的选择转发出去时，
     * `launchedFromPackage` 会被写成**目标应用自己**
     * （日志实证：`XZtake: mark.via -> mark.via take=false act=VIEW`，
     *   而前 0.3 秒刚出现 `com.tencent.mobileqq -> com.android.intentresolver CHOOSER`）。
     * 这样会撞上「caller == target 不接管」这条保护应用内跳转的规则 → 全屏。
     *
     * 所以在选择器出现后的 [_CHOOSER_WINDOW] 毫秒内，允许 caller==target 的启动被接管；
     * 其余时间仍严格保护应用内跳转。
     */
    private const val _CHOOSER_WINDOW = 3000L
    @Volatile private var chooserAt = 0L

    /** 记录「刚出现了选择器」，由钩子在看到 CHOOSER / 选择器目标时调用 */
    @JvmStatic
    fun noteChooser() {
        chooserAt = android.os.SystemClock.uptimeMillis()
    }

    /** 距上次选择器出现过了多少毫秒（诊断用，0/负数表示没记录过） */
    @JvmStatic
    fun chooserAge(): Long =
        if (chooserAt == 0L) -1L
        else android.os.SystemClock.uptimeMillis() - chooserAt

    private fun withinChooserWindow(): Boolean =
        chooserAt != 0L &&
            android.os.SystemClock.uptimeMillis() - chooserAt < _CHOOSER_WINDOW

    /** 桌面图标启动：MAIN + LAUNCHER */
    private fun isHomeLaunch(it: Intent): Boolean {
        if (Intent.ACTION_MAIN != it.action) return false
        return it.categories?.contains(Intent.CATEGORY_LAUNCHER) == true
    }

    /** 目标包名：显式 component / setPackage */
    @JvmStatic
    fun targetPkg(it: Intent): String? {
        it.component?.packageName?.let { return it }
        val p = it.`package`
        return if (p.isNullOrEmpty()) null else p
    }

    // ================= 隐式 Intent 的目标解析 =================

    private val cache = java.util.concurrent.ConcurrentHashMap<String, String>()
    private const val CACHE_MAX = 512

    /**
     * 解析隐式 Intent 最终会落到哪个包。
     *
     * ★ 「在 App 里点链接」几乎都是隐式 Intent（只有 `ACTION_VIEW` + data），
     * 只靠 [targetPkg] 会全部返回 null、判据一律放弃 ——
     * 实测表现就是「应用内跳转还是全屏」而日志里连候选都看不到。
     *
     * 失败返回 null（据此放弃，宁可不生效也不要误伤）。
     */
    @JvmStatic
    fun resolveTarget(ctx: android.content.Context?, it: Intent): String? {
        if (ctx == null) return null
        val key = (it.action ?: "") + '|' + (it.dataString ?: "") + '|' + (it.type ?: "")
        cache[key]?.let { return it.ifEmpty { null } }
        val pkg = runCatching {
            ctx.packageManager
                .resolveActivity(it, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
                // 回退：有些 ROM 上带 MATCH_DEFAULT_ONLY 解析不到（比如多浏览器、无默认），
                // 放宽后再试一次 —— 否则会落到最下游那层接管（又慢又会崩）。
                ?: ctx?.packageManager?.resolveActivity(it, 0)?.activityInfo?.packageName
        }.getOrNull()
        if (cache.size > CACHE_MAX) cache.clear()
        cache[key] = pkg ?: ""
        return pkg
    }
}
