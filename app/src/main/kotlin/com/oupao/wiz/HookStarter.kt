package com.oupao.wiz

import android.content.Intent
import android.os.Bundle

/**
 * system_server 侧：**只注入 options，不抢启动**（2890）。
 *
 * 2886–2889 用沿用系统的 `preempt`（直调 `startZoomWindow` + 取消原启动）接管磁贴，
 * 结果是「小窗先出、控制中心后收」—— 顺序反了（用户 2890 反馈）。
 * 老版本（≤2850）和开源真正生效的那条是**注入**：只给 ActivityOptions 补 zoom 参数，
 * 启动仍交给系统，面板收起、转场都由系统自己编排。
 *
 * 所以 2890：
 *  - `preempt` **停用**（保留代码备查，只留注释和开关常量，不再调用）；
 *  - `inject` 放宽一条：**pendingIntent 的磁贴类启动也要注入**（磁贴点击就是走 PendingIntent）。
 */
class HookStarter : XMH() {

    private val preempting = ThreadLocal<Boolean>()

    override fun beforeHookedMethod(param: XMP) {
        val starter = param.thisObject ?: return
        // 2932：控制中心/磁贴 → 「**先收控制中心、再出小窗**」。
        //   注入派（inject）只能改参数、启动仍立即发生 → 小窗先出、面板后收（用户一直抱怨的顺序）。
        //   改成 preempt：拦下本次启动 + 立即发一条 collapsePanels（SystemUI 自己的收起动画）
        //   + 延时后由我们自己 startZoomWindow。
        // 2934：**沿用 2852 的 HookXzCtl**。
        //   2852 在 ActivityStarter 上就是：shouldTake(intent, caller, target, hasResult)
        //   → 真 → CrossZoom.fire(intent, user, cl) → 成功则 result=0 + returnEarly。
        //   没有 reasonFor / inject / preemptTile 那一堆。
        if (runCatching { takeAsZoom2852(param, starter) }.getOrDefault(false)) {
            param.result = 0
            param.returnEarly = true
            return
        }
        // 2931 的 preempt 会把部分磁贴的启动直接吞掉（用户反馈）。
        // 2932：回到「注入派」（=系统实现的靠谱行为，永远不取消原启动），
        //   收控制中心改成「提交开小窗后立即异步发一条 collapsePanels」。
        // if (runCatching { preemptTile(starter) }.getOrDefault(false)) {
        //     param.result = 0
        //     param.returnEarly = true
        //     return
        // }
        if (PREEMPT_ENABLED) {
            val preempted = preemptWithZoomWindow(starter)
            if (preempted) {
                param.result = 0
                param.returnEarly = true
                return
            }
        }
        runCatching { inject(starter) }
    }

    /**
     * 2934：对应 2852 的 `HookXzCtl.beforeHookedMethod`——完整沿用：
     * `CrossZoom.shouldTake(intent, caller, target, hasResult)` 为真时，
     * 直接 `CrossZoom.fire(intent, user, cl)`（= startZoomWindow + 全套 zoom 参数），
     * 成功则由调用方取消原启动。
     */
    private fun takeAsZoom2852(param: XMP, starter: Any?): Boolean {
        if (starter == null) return false
        // 2979：选择器类启动一律不接管
        if (CrossZoom.chooserJustSeen()) return false
        if (preempting.get() == true) return false
        if (CrossZoom.inZoomNow()) return false
        if (!Cfg.crossZoom()) return false

        val request = readField(starter, "mRequest")
        val intent = (readField(starter, "mIntent") as? Intent)
            ?: (request?.let { readField(it, "intent") } as? Intent)
            ?: return false

        val caller = (readField(starter, "mCallingPackage") as? String)
            ?: (request?.let { readField(it, "callingPackage") } as? String)
            ?: scanCaller(param)
            ?: return false

        val target = CrossZoom.targetPkg(intent)
            ?: CrossZoom.resolveTarget(Cfg.sysContext(), intent)
        val hasResult = request?.let { readField(it, "resultTo") } != null
        val take = CrossZoom.shouldTake(intent, caller, target, hasResult)
        if (!take) {
            // ★ 3359+（C17/C16 实测）：content:// / file:// 的文件打开**不能取消原启动**
            //   （取消后 URI 临时授权就没了，MT 能起来但读不到文件 → 自己 finish）。
            //   但用户要的是 C16 那样「以小窗打开」，所以这里**只注入 zoom 参数、放行原启动**：
            //   原启动保留 → 授权在；ActivityOptions 带小窗键 → 系统按小窗起。
            if (hasResult && caller != target && !CrossZoom.isSystemApp(target) &&
                CrossZoom.isFileOpenIntent(intent)
            ) {
                val okInj = runCatching { injectZoomOptions(starter, request) }.getOrDefault(false)
                SLog.r("XZinj", "XZinj: fileOpen → 注入小窗放行 inj=" + okInj + " -> " + target, 400L)
            }
            return false
        }
        if (hasResult) {
            // 2946（用户：桌面打开 Gemini 会被小窗）：
            //   Gemini 的入口 `BardEntryPointActivity` 会发一条
            //   `startActivityForResult(VIEW https://bard.google.com/android?source=bardshell -> com.google.android.googlequicksearchbox)`。
            //   这是 App-Link 式的「启动手接」，不是用户要小窗打开的网页链接 → 一律全屏。
            if (CrossZoom.isWebLaunch(intent)) {
                SLog.r("XZinj", "XZinj: resultTo + 网页手接 → 放行全屏 " + target, 400L)
                return false
            }
            val okInj = runCatching { injectZoomOptions(starter, request) }.getOrDefault(false)
            SLog.r("XZinj", "XZinj: resultTo → 注入放行 inj=" + okInj + " -> " + target, 400L)
            return false
        }
        if (target != null && (target == "com.android.systemui" || target == "com.oupao.wiz")) return false

        val user = (request?.let { intField(it, "userId") } ?: userId()).let { if (it < 0) 0 else it }

        preempting.set(true)
        return try {
            // 2954：磁贴来源 → 先让 ROM 带动画把控制中心收起（IStatusBarService 那条没动画）。
            if (CrossZoom.isTileCaller(caller, intent)) {
                runCatching { CrossZoom.collapsePanelsAnimated() }
            }
            val ok = CrossZoom.fire(intent, user, starter, "Starter")
            SLog.r("XZoom", "XZoom2852: take " + caller + " -> " + target + " user=" + user + " ok=" + ok, 400L)
            ok
        } finally {
            preempting.set(false)
        }
    }

    /** 2939：就地把 zoom 参数写进这次启动的 ActivityOptions（不取消启动）。 */
    private fun injectZoomOptions(starter: Any, request: Any?): Boolean {
        val original = optionsBundle(starter, request)
        if (alreadyZoom(original)) return true
        // 2983：注入小窗前置位。宽限窗内 FlexFix 才会告诉系统「这个页面能进小窗」
        CrossZoom.armZoomSupport()
        val bundle = zoomSpecBundle(original)
        bundle.putInt("android.activity.splashScreenStyle", 1)
        val callingUid = intField(starter, "mCallingUid").takeIf { it > 0 }
            ?: request?.let { intField(it, "callingUid") }
            ?: 0
        val callingPid = request?.let { intField(it, "callingPid") } ?: 0
        var ok = runCatching { callSetActivityOptions(starter, bundle, callingPid, callingUid) }
            .getOrDefault(false)
        if (!ok) {
            runCatching { writeOptions(starter, bundle) }
            ok = true
        }
        runCatching { writeOptions(request, bundle, "activityOptions") }
        return ok
    }

    /** 2852 的 callerOf：在实参里找第一个「含点、不以点开头」的字符串当调用方包名。 */
    private fun scanCaller(param: XMP): String? {
        for (a in param.args) {
            val str = a as? String ?: continue
            if (str.contains('.') && !str.startsWith(".")) return str
        }
        return null
    }

    /** 2932：磁贴/控制中心专用的 preempt——先收面板，延时后再开小窗。 */
    private fun preemptTile(starter: Any?): Boolean {
        if (starter == null) return false
        if (preempting.get() == true) return false
        if (!enabled()) return false

        val request = readField(starter, "mRequest")
        val intent = (readField(starter, "mIntent") as? Intent)
            ?: (request?.let { readField(it, "intent") } as? Intent)
            ?: return false
        val pendingIntent = request?.let { readField(it, "originatingPendingIntent") }
        val callingUid = intField(starter, "mCallingUid").takeIf { it > 0 }
            ?: request?.let { intField(it, "callingUid") }
            ?: 0
        val realCallingUid = intField(starter, "mRealCallingUid")
        val requestRealUid = request?.let { intField(it, "realCallingUid") } ?: -1
        val reason = reasonFor(
            intent, callingUid, realCallingUid, requestRealUid, pendingIntent, systemUiUid(),
            request?.let { readField(it, "resultTo") } != null,
            (readField(starter, "mCallingPackage") as? String)
                ?: (request?.let { readField(it, "callingPackage") } as? String),
            runCatching {
                val ai = request?.let { readField(it, "activityInfo") }
                Rt.get(ai, "packageName") as? String
            }.getOrNull()
        ) ?: return false
        if (!reason.startsWith("control_center") && reason != "pending_intent") return false

        val target = intent.component?.packageName ?: intent.`package` ?: return false
        if (target in SKIP_TARGETS || target in SKIP_TARGETS_FOR_TILES) return false

        // 1) 先收控制中心（SystemUI 自己走收起动画）
        val collapsed = runCatching { CrossZoom.collapsePanels() }.getOrDefault(false)
        // 2) 延时后自己开小窗（面板正在收的时候窗口才出来）
        val copy = Intent(intent)
        val opts = optionsBundle(starter, request)
        val wait = runCatching {
            (Cfg.getProp("persist.oupao.tile_preempt_delay", "160").toLongOrNull() ?: 160L).coerceIn(0L, 800L)
        }.getOrDefault(160L)

        preempting.set(true)
        SLog.r("XZinj", "XZinj: 磁贴 preempt 收面板=" + collapsed + " 延时=" + wait + " -> " + target, 400L)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                val ok = runCatching { startZoomWindow(copy, target) }.getOrDefault(false)
                SLog.r("XZinj", "XZinj: 磁贴 preempt 开小窗 ok=" + ok, 400L)
                if (!ok && opts != null) {
                    runCatching { Cfg.sysContext()?.startActivity(copy, opts) }
                    SLog.i("XZoom: 磁贴 preempt 兜底 startActivity")
                }
            } finally {
                preempting.set(false)
            }
        }, wait)
        return true
    }

    private fun preemptWithZoomWindow(starter: Any?): Boolean {
        if (starter == null) return false
        if (preempting.get() == true) return false
        if (!enabled()) return false

        val request = readField(starter, "mRequest")
        val intent = (readField(starter, "mIntent") as? Intent)
            ?: (request?.let { readField(it, "intent") } as? Intent)
            ?: return false
        val pendingIntent = request?.let { readField(it, "originatingPendingIntent") }
        val callingUid = intField(starter, "mCallingUid").takeIf { it > 0 }
            ?: request?.let { intField(it, "callingUid") }
            ?: 0
        val realCallingUid = intField(starter, "mRealCallingUid")
        val requestRealUid = request?.let { intField(it, "realCallingUid") } ?: -1
        val systemUiUid = systemUiUid()
        val fromSystemUi = systemUiUid > 0 &&
            (callingUid == systemUiUid || realCallingUid == systemUiUid || requestRealUid == systemUiUid)

        val action = intent.action ?: ""
        val target = intent.component?.packageName ?: intent.`package` ?: return false

        // 2886\uff1a**\u53ea\u63a5\u7ba1\u78c1\u8d34\u7c7b**\uff08\u901a\u77e5\u70b9\u51fb\u4e0d\u8fdb\u6765\uff0c\u5426\u5219\u4f1a\u767d\u5c4f\uff09
        val looksLikeTile = isTileLike(intent, target)

        val eligible = when {
            pendingIntent != null -> looksLikeTile && target !in SKIP_TARGETS
            fromSystemUi -> looksLikeTile && target !in SKIP_TARGETS_FOR_TILES
            else -> false
        }
        if (!eligible) return false

        preempting.set(true)
        return try {
            val started = startZoomWindow(intent, target)
            if (started) SLog.r("XZinj", "XZinj: preempted " + target, 400L)
            started
        } finally {
            preempting.set(false)
        }
    }

    private fun startZoomWindow(intent: Intent, targetPackage: String): Boolean = try {
        val managerClass = Class.forName(ZOOM_MANAGER_CLASS)
        val instance = managerClass.getMethod("getInstance").invoke(null) ?: return false
        val start = managerClass.getMethod(
            "startZoomWindow",
            Intent::class.java,
            Bundle::class.java,
            Int::class.javaPrimitiveType,
            String::class.java
        )
        val result = start.invoke(instance, intent, zoomBundle(), userId(), targetPackage) as? Int
        result != null && result >= 0
    } catch (t: Throwable) {
        SLog.e("XZoom: startZoomWindow \u5931\u8d25", t)
        false
    }

    private fun zoomBundle(): Bundle =
        zoomSpecBundle(null).apply {
            putInt("android.activity.splashScreenStyle", 1)
            putInt("start_zoom_at_other_side", LAUNCH_AT_RIGHT)
        }

    private fun userId(): Int = try {
        Class.forName("android.os.UserHandle").getMethod("myUserId").invoke(null) as? Int ?: 0
    } catch (t: Throwable) {
        0
    }

    private fun inject(starter: Any?) {
        if (starter == null) return
        if (!enabled()) return
        // ★ 3337：**灭屏 / 锁屏期间一律不注入小窗参数**。
        //   外部用户实测：锁屏相机（`STILL_IMAGE_CAMERA_SECURE` + `StartFromKeyguard`，
        //   由 SystemUI 发起）走到这里时被判成 `reason=control_center` 注入了
        //   `extra_window_mode=100` → 相机开成了**锁屏上根本显示不出来的小窗**。
        //   `CrossZoom.shouldTake` / `NotifFly` 那边一直有 sleeping 判据，只有注入这条漏了。
        //   系统行为同样是「锁屏就不接管」：isGestureAllowed() 里带 !keyguardLocked。
        if (Cfg.sleepingNow()) {
            SLog.r("XZinj", "XZinj: 灭屏/锁屏 → 不注入", 1000L)
            return
        }

        val request = readField(starter, "mRequest")
        val intent = (readField(starter, "mIntent") as? Intent)
            ?: (request?.let { readField(it, "intent") } as? Intent)
            ?: return
        // ★ 3359+：content:// / file:// 的文件打开——不取消原启动，只补 zoom 参数。
        //   （原启动保留才能保住 URI 临时授权；options 带小窗键 → 仍按小窗打开。）
        //   ★ 同应用内部跳转（日历待办卡片 → CreateEventOrTaskActivity）**不注入**，保持全屏。
        if (CrossZoom.isFileOpenIntent(intent)) {
            val caller0 = (readField(starter, "mCallingPackage") as? String)
                ?: (request?.let { readField(it, "callingPackage") } as? String)
            val target0 = CrossZoom.targetPkg(intent)
                ?: CrossZoom.resolveTarget(Cfg.sysContext(), intent)
            if (caller0 != null && caller0 == target0) return
            // 安装器/系统文件处理应用 → 保持全屏（不要开成小窗）
            if (target0 != null && CrossZoom.isSystemApp(target0)) return
            val ok = runCatching { injectZoomOptions(starter, request) }.getOrDefault(false)
            SLog.r("XZinj", "XZinj: fileOpen → 注入小窗放行 inj=" + ok + " -> " + (target0 ?: "?"), 400L)
            return
        }
        // 2933：**去掉「已注入」标记**。
        //   原来在 Intent 上 putExtra(EXTRA_APPLIED, 1)，但磁贴的 Intent 是
        //   PendingIntent 缓存的**同一个对象** → 标记会永远留在上面，
        //   于是第一次小窗、之后每次都被跳过注入 → 全屏打开。
        //   系统实现也没有这个标记，只靠 `alreadyZoom()` 防重复。

        val callingUid = intField(starter, "mCallingUid").takeIf { it > 0 }
            ?: request?.let { intField(it, "callingUid") }
            ?: 0
        val realCallingUid = intField(starter, "mRealCallingUid")
        val requestRealUid = request?.let { intField(it, "realCallingUid") } ?: -1
        val pendingIntent = request?.let { readField(it, "originatingPendingIntent") }

        // ★ 3338：把真实调用方也带进判据 —— 桌面插件引擎（com.heytap.colorfulengine）等
        //   「不接管」来源在 `reasonFor` 里直接放行，否则插件点击会被注入小窗参数，
        //   而原生小窗通道对这类启动会抛 SecurityException → 点了什么都不打开。
        val callerPkg = (readField(starter, "mCallingPackage") as? String)
            ?: (request?.let { readField(it, "callingPackage") } as? String)

        val hasResult = request?.let { readField(it, "resultTo") } != null
        val reason = reasonFor(
            intent, callingUid, realCallingUid, requestRealUid, pendingIntent, systemUiUid(),
            hasResult = hasResult,
            caller = callerPkg
        ) ?: return

        val original = optionsBundle(starter, request)
        if (alreadyZoom(original)) return
        // 2983：注入小窗前置位（宽限窗内 FlexFix 才会告诉系统「这个页面能进小窗」）
        CrossZoom.armZoomSupport()
        val bundle = zoomSpecBundle(original)
        val callingPid = request?.let { intField(it, "callingPid") } ?: 0
        val byStarter = runCatching {
            callSetActivityOptions(starter, bundle, callingPid, callingUid)
        }.getOrDefault(false)
        if (!byStarter) {
            runCatching { writeOptions(starter, bundle) }
        }
        // 2891：磁贴（PendingIntent）启动真正用的是 `mRequest.activityOptions`。
        //   原来只写 starter.mOptions → 注入没落到实际启动上 → 磁贴变全屏打开。
        runCatching { writeOptions(request, bundle, "activityOptions") }
        SLog.r("XZinj", "XZinj: injected " + reason + " byStarter=" + byStarter, 400L)
        // 2930：**所有控制中心/磁贴类**启动都要先收面板
        //   （实测磁贴走的是 reason=control_center / control_center_pending，不是 pending_intent）
        if (reason.startsWith("control_center") || reason == "pending_intent") {
            // 2932：**异步**收面板。
            //   2930 在这里同步调 `collapsePanels()`（跨进程 binder），
            //   结果部分磁贴的启动被搞死（用户：「部分磁贴不小窗了」）。
            //   现在只 post 到主线程，不阻塞也不影响这次启动；面板会在小窗起来的同一瞬间开始收。
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                // 2954：走 SystemUI 侧的带动画收起（ROM 自己的 collapseQSPanel(anim=true)）
                val collapsed = runCatching { CrossZoom.collapsePanelsAnimated() }.getOrDefault(false)
                SLog.r("XZinj", "XZinj: 磁贴后收控制中心(异步,带动画) = " + collapsed, 400L)
            }
        }
    }

    private fun reasonFor(
        intent: Intent,
        callingUid: Int,
        realCallingUid: Int,
        requestRealUid: Int,
        pendingIntent: Any?,
        systemUiUid: Int,
        hasResult: Boolean = false,
        caller: String? = null,
        resolvedPkg: String? = null,
    ): String? {
        if (intent.getIntExtra(EXTRA_SOURCE, 0) == SOURCE_NOTIFICATION) return "notification"
        if (intent.getBooleanExtra(EXTRA_QUICK_SETTINGS, false)) {
            return if (Cfg.lightQs()) "control_center" else null
        }
        val fromSystemUi = systemUiUid > 0 &&
            (callingUid == systemUiUid || realCallingUid == systemUiUid || requestRealUid == systemUiUid)
        val target = intent.component?.packageName ?: intent.`package` ?: return null
        // 同应用内部子页（设置 → 设置子页、QQ → QQ 子页）：resultTo 也不注入小窗。
        // 用户实测：系统设置里点某个选项，子页被 HookStarter 的 settings_result 分支注入成了小窗。
        if (hasResult && !caller.isNullOrEmpty() && caller == target) {
            SLog.r("XZinj", "XZinj: 同应用子页 + resultTo → 全屏 " + caller + " -> " + target, 800L)
            return null
        }
        // ★ 3338：**桌面插件点击**（intent 带 `appWidgetId`）→ 不注入，按原厂全屏打开。
        //   实测外部用户：时钟/天气插件被注入后走原生小窗通道，抛
        //   `SecurityException: Permission Denial: package=android does not belong to uid=…`，
        //   结果点了什么都不打开。
        if (intent.hasExtra("appWidgetId")) {
            SLog.r("XZinj", "XZinj: 桌面插件点击 → 全屏 " + caller + " -> " + target, 800L)
            return null
        }
        // ★ 3338：不接管的调用方（桌面插件引擎 / 相机 / shell / 本模块 …）
        if (caller != null && CrossZoom.isSkipCaller(caller)) return null
        // ★ 3337：**锁屏/安全相机永远全屏**（双保险：sleeping 判据在解锁动画中间可能刚好漏一帧）
        val act = intent.action
        if (act == "android.media.action.STILL_IMAGE_CAMERA" ||
            act == "android.media.action.STILL_IMAGE_CAMERA_SECURE" ||
            intent.getBooleanExtra("StartFromKeyguard", false)
        ) {
            SLog.r("XZinj", "XZinj: 锁屏相机 → 全屏 " + caller + " -> " + target, 800L)
            return null
        }
        // ★ 2984：选择器**自身**不注入（全屏）；
        //   但「刚在选择器里选完」的那一跳必须放行（否则分享落地也全屏）。
        val pickedNow = CrossZoom.chooserArmed(target)
        // 网页 FORWARD_RESULT 是 Chooser 转发出来的链接，不是选择器自身；
        // 不能被下面的「选择器刚弹过」闸门挡住（QQ「其他应用打开」→ 浏览器就是这条）。
        val webForward = hasResult && CrossZoom.isWebLaunch(intent) &&
            (intent.flags and Intent.FLAG_ACTIVITY_FORWARD_RESULT) != 0
        if (CrossZoom.chooserJustSeen() && !pickedNow && !webForward) {
            SLog.r("XZinj", "XZinj: 选择器自身 → 全屏 " + caller + " -> " + target, 400L)
            return null
        }
        // ★ 2970（用户：微信 → 消息通知设置 又变全屏了）：
        //   **设置页 + resultTo → 就地注入 zoom**（不取消启动，= 2939 的行为）。
        //   但**桌面/launcher 发起的启动一律不注入** —— ColorOS 桌面起应用用的是
        //   startActivityForResult，桌面点击全带 resultTo；不挡就会「桌面点什么都是小窗」（2964 踩过）。
        if (CrossZoom.isLauncherLike(caller) && !CrossZoom.fromControlCenter(target)) return null
        // ★ 2973（用户：QQ 分享到微信还是全屏）：
        //   分享带 `FLAG_ACTIVITY_FORWARD_RESULT`，所以 takeReason 会因 `forwardResult` 放行（不能取消）。
        //   但**注入**小窗是安全的：不取消原启动，分享结果照常回 QQ。
        if (CrossZoom.isSdkShare(intent, target)) {
            // ★ 2974：如果这一跳实际落到**选择器**（ColorOS「分身选择器」就是），
            //   或目标应用**有分身**→ 不注入。否则选择器会出现在小窗里（用户实测）。
            // ★ 2976：有分身的应用会先弹「选择打开的应用」。
            //   1) 发起方是原 App（QQ 等）→ 这一跳待会还要弹选择器 → **不注入**；
            //   2) 发起方就是选择器本身（= 选完分身后的真正启动）→ **注入**（这时选择器已经弹完了）。
            val fromChooser = CrossZoom.isChooserCaller(caller)
            val chooser = CrossZoom.isChooserHop(intent, resolvedPkg)
            // 2977：上一次同一目标包「待弹选择器」刚记下来 → 这一次就是选完后的真正启动
            val picked = CrossZoom.chooserArmed(target)
            if (!fromChooser && !picked && (CrossZoom.hasClone(target) || chooser)) {
                SLog.r(
                    "XZoom",
                    "XZoom: 分享跳过注入(待弹选择器) fromChooser=" + fromChooser + " picked=" + picked +
                        " chooser=" + chooser + " clone=" + CrossZoom.hasClone(target) +
                        " caller=" + caller + " -> " + target,
                    400L
                )
                return null
            }
            if (picked) CrossZoom.clearChooserPick()
            SLog.r(
                "XZoom",
                "XZoom: QQ/微信 SDK 分享 → 注入小窗 fromChooser=" + fromChooser +
                    " picked=" + picked + " caller=" + caller + " -> " + target,
                400L
            )
            return "sdk_share"
        }
        // 网页 + FORWARD_RESULT：Chooser 转发出来的链接（QQ「其他应用打开」→ 浏览器）。
        // 这类也是 resultTo，但不能取消；用注入通道，既保留结果又按小窗起。
        if (webForward) {
            SLog.r(
                "XZinj",
                "XZinj: 网页 FORWARD_RESULT → 注入小窗 " + caller + " -> " + target,
                400L,
            )
            return "web_forward"
        }
        if (hasResult && CrossZoom.isSettingsTarget(target)) {
            SLog.r("XZinj", "XZinj: 设置页 + resultTo → 注入小窗 " + caller + " -> " + target, 400L)
            return "settings_result"
        }
        // 2890：磁贴点出来的设置页是走 PendingIntent 的 —— 不抢启动，但必须注入。
        if (pendingIntent != null) {
            if (!isTileLike(intent, target)) return null
            return if (Cfg.lightQs()) "control_center_pending" else null
        }
        if (target in SKIP_TARGETS) return null
        if (fromSystemUi) {
            if (!Cfg.lightQs()) return null
            // 2892：磁贴的 action 不一定带 quicksettings/QS_TILE（实测 com.android.phone 那条是
            //   android.settings.NETWORK_OPERATOR_SETTINGS），所以这里改用同一个 isTileLike 判据，
            //   否则会被判 null、注入不进去 → 磁贴全屏打开。
            // 2932：系统实现里这里**不看 action**，
            //   只要从 SystemUI 发起且目标不在黑名单就当磁贴处理。
            //   原来要求 isTileLike（action 含 quicksettings / android.settings 等），
            //   那些 action 不对得上的磁贴（计算器、手电筒…）就被丢掉 → 「部分磁贴不小窗」。
            return "control_center"
        }
        if (CrossZoom.fromControlCenter(target)) {
            return if (Cfg.lightQs()) "control_center_launcher" else null
        }
        return null
    }

    /** 磁贴 / 控制中心类启动：动作或目标看着像设置、开关页。 */
    private fun isTileLike(intent: Intent, target: String): Boolean {
        val action = intent.action ?: ""
        return action.contains("quicksettings", true) ||
            action.contains("QS_TILE", true) ||
            action.startsWith("android.settings") ||
            target.contains("settings", true) ||
            target == "com.android.phone" ||
            target == "com.oplus.wirelesssettings"
    }

    private fun optionsBundle(starter: Any, request: Any?): Bundle? {
        val options = readField(starter, "mOptions")
            ?: request?.let { readField(it, "activityOptions") }
            ?: return null
        return runCatching {
            val toBundle = options.javaClass.getMethod("toBundle")
            toBundle.isAccessible = true
            toBundle.invoke(options) as? Bundle
        }.getOrNull()
    }

    private fun callSetActivityOptions(
        starter: Any,
        bundle: Bundle,
        callingPid: Int,
        callingUid: Int,
    ): Boolean {
        val method = starter.javaClass.declaredMethods.firstOrNull {
            it.name == "setActivityOptions" && it.parameterCount == 3 &&
                it.parameterTypes[0] == Bundle::class.java
        } ?: return false
        method.isAccessible = true
        method.invoke(starter, bundle, callingPid, callingUid)
        return true
    }

    private fun writeOptions(target: Any?, bundle: Bundle, fieldName: String = "mOptions") {
        if (target == null) return
        val fromBundle =
            Class.forName("android.app.ActivityOptions").getMethod("fromBundle", Bundle::class.java)
        fromBundle.isAccessible = true
        val options = fromBundle.invoke(null, bundle)
        var current: Class<*>? = target.javaClass
        while (current != null) {
            try {
                val field = current.getDeclaredField(fieldName)
                field.isAccessible = true
                field.set(target, options)
                return
            } catch (e: NoSuchFieldException) {
                current = current.superclass
            }
        }
    }

    private fun zoomSpecBundle(original: Bundle?): Bundle {
        val result = Bundle()
        if (original != null) result.putAll(original)
        result.putInt("android:activity.mZoomLaunchFlags", 5)
        result.putInt("extra_window_mode", 100)
        result.putInt("android.activity.windowingMode", 100)
        result.putBoolean("androidx.activity.AdjustInputMethod", true)
        result.putInt("androidx.activity.LaunchScenario", 1)
        return result
    }

    private fun alreadyZoom(original: Bundle?): Boolean =
        original != null && original.getInt("extra_window_mode", 0) == 100

    private fun readField(instance: Any, name: String): Any? {
        var current: Class<*>? = instance.javaClass
        while (current != null) {
            try {
                return current.getDeclaredField(name).also { it.isAccessible = true }.get(instance)
            } catch (e: NoSuchFieldException) {
                current = current.superclass
            }
        }
        return null
    }

    private fun intField(instance: Any, name: String): Int {
        var current: Class<*>? = instance.javaClass
        while (current != null) {
            try {
                val field = current.getDeclaredField(name)
                field.isAccessible = true
                return field.getInt(instance)
            } catch (e: NoSuchFieldException) {
                current = current.superclass
            }
        }
        return 0
    }

    private fun enabled(): Boolean = CrossZoom.enabledForStarter()

    private fun systemUiUid(): Int {
        if (cachedUid > 0) return cachedUid
        cachedUid = runCatching {
            val at = Class.forName("android.app.ActivityThread")
            val cur = at.getMethod("currentActivityThread").invoke(null)
            val ctx = at.getMethod("getSystemContext").invoke(cur) as android.content.Context
            ctx.packageManager.getApplicationInfo("com.android.systemui", 0).uid
        }.getOrDefault(0)
        return cachedUid
    }

    private companion object {
        /** 2890：停用直调接管（它会让小窗抢在面板收起前面出来）。要恢复只改这里。 */
        const val PREEMPT_ENABLED = false
        const val EXTRA_SOURCE = "oupao_zoom_source"
        const val SOURCE_NOTIFICATION = 1
        const val EXTRA_QUICK_SETTINGS = "start_from_systemui_qs"
        const val EXTRA_APPLIED = "oupao_zoom_applied"
        const val ZOOM_MANAGER_CLASS = "com.oplus.zoomwindow.OplusZoomWindowManager"
        const val LAUNCH_AT_RIGHT = 14
        const val SHADE_COLLAPSE_DELAY_MS = 60L

        val SKIP_TARGETS_FOR_TILES = setOf(
            "com.android.systemui",
            "android",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
        )

        val SKIP_TARGETS = setOf(
            "com.android.systemui",
            "com.android.settings",
            "com.coloros.settings",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "android",
        )

        @Volatile
        var cachedUid: Int = 0
    }
}
