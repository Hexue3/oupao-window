package com.oupao.wiz

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager

/**
 * Launcher 侧的手势闸门（跑在 `com.android.launcher` 进程）。
 *
 * ## 背景：底部那条带子的真实机制
 *
 * ColorOS 底部约 77px 是 native 的「homegesture 拦截区」，这**一整条带的触摸由桌面接管**：
 * `OplusBaseTouchInteractionService`（Quickstep）先接住，判定是上滑就 `pilferPointers` 抢流并回桌面；
 * 判定**不是**上滑（没越过 slop）就把它**重放**给下层 App —— 所以"点击"能生效，靠的正是桌面这套重放。
 *
 * 这就决定了两个约束，必须同时满足：
 *  1. 不能粗暴地把 DOWN 挡掉（`canRespondGesture=false`）：那样 consumer 不建 → 没人重放 → 热区变死区；
 *  2. 又不能让那条流越过 slop：否则底部上滑手势成立 → 呼出扇形菜单的瞬间会把前台应用拖一下。
 *
 * ## 做法（2723）
 *
 *  - `onDispatchInputEvent(MotionEvent)`：命中热区的这一条流，**只丢 MOVE**（跨过 slop 的那些），
 *    DOWN / UP 照常放行 → 桌面的 consumer 永远"没越过 slop"，于是按它的默认语义把点击重放给下层；
 *    菜单确实被呼出来了（`sys.oupao.fly.open == 1`）时，连 UP 一起丢掉，免得这一下变成对前台 App 的点击。
 *  - `canRespondGesture`：**只在菜单已展开时**压成 false（那时本来就该整屏都不响应手势）。
 */
object FlyGate {

    private const val TAG = "OupaoFlyGate"

    @Volatile
    private var done = false

    @Volatile
    private var lastLog = 0L

    /** 当前被"控流"的那条手势的 downTime（-1 = 没有） */
    @Volatile
    private var gateDown = -1L

    @Volatile
    private var startX = 0f

    @Volatile
    private var startY = 0f

    @JvmStatic
    fun install(cl: ClassLoader?): Boolean {
        if (done) return hooks > 0
        val loader = cl ?: runCatching { FlyGate::class.java.classLoader }.getOrNull()
        // ★ 3364：**回退到 3359 的行为**（用户定稿）。
        //   3360~3363 那套「DEX 反查混淆类 + 丢掉/冻结 MOVE」在 C17 新桌面上会 ANR：
        //     `[Gesture Monitor] swipe-up is not responding ... action=MOVE`
        //     → am_kill launcher（表现为桌面重启、引索条连带重启、点击失效）。
        //   根因是那些消费者要成对 finish 事件，任何吞/改事件的做法都会让前一条永远没人 finish。
        //   所以这里只保留 3359 及以前验证过的路径：老桌面（有 OplusBaseTouchInteractionService）才有闸门；
        //   新版桌面找不到该类 = 不装闸门（穿透问题留待以后用"不改事件"的方案解决）。
        val cls = runCatching {
            Class.forName("com.android.quickstep.OplusBaseTouchInteractionService", false, loader)
        }.getOrNull()
        if (cls == null) {
            log("找不到 OplusBaseTouchInteractionService，跳过")
            return false
        }
        var ok = 0
        // 1)+2) 手势闸门：**按"方法形状"扫，不写死名字**
        //   2832：另一个用户（Android 17 / ColorOS 新版本）上实测到这里两个钩子**一个都没挂上**
        //   （他的 LSPosed 模块日志里只有 Launcher.onCreate / onStateSetEnd / WebView.loadUrl，
        //    没有 OupaoBaseTouchInteractionService.*）→ 他的 `[Gesture Monitor] swipe-up` 照常
        //    完成手势 → 呼出菜单时上滑穿透。他的系统日志暴露了真名：
        //    `OplusBaseTouchInteractionService[u0]: onInputEvent(), ACTION_DOWN, Consumer=...`
        //   → 新版本把入口改名成 onInputEvent（参数也可能换成 InputEvent）。
        //   所以这里改成：沿继承链扫所有「单参数、能把 MotionEvent 传进去」的输入入口，
        //   名字命中白名单/形如 onXxxInputEvent 的，全部挂上（多挂一个只是多重丢 MOVE，无害）。
        runCatching { ok += installGestureHooks(cls) }
            .onFailure { log("安装手势闸门失败 " + it) }
        // 3) 2834：**桌面自己**那条触摸链也要闸住。
        //   用户实测：在 App 里呼出菜单不再穿透了，但**在桌面上**斜滑呼出时，上滑手势
        //   照样穿透（桌面打开了应用抽屉）。原因是桌面在前台时，那条手势根本不经过
        //   TouchInteractionService 的 pilfer 路径，而是桌面 Activity 自己处理的
        //   → 上面那些钩子一次都不会被调用。所以在 Launcher 自己的触摸入口上再挂一遍。
        runCatching { ok += installLauncherTouchHooks(loader) }
            .onFailure { log("安装桌面触摸闸门失败 " + it) }
        // 4) 2829：概览（多任务）状态 —— 只能从桌面 Activity 自己身上拿
        runCatching { if (installStateHooks(loader)) ok++ }
            .onFailure { log("hook 桌面状态 失败 " + it) }
        hooks = ok
        done = ok > 0
        log("FlyGate hooks=" + ok)
        return done
    }

    /**
     * 2834：桌面 Activity 自己的触摸入口（桌面在前台时上滑 = 打开应用抽屉那条路）。
     *
     * 为什么必须单独挂：`TouchInteractionService` 那套 pilfer 只在"桌面向 App 转交手势"时
     * 才参与；桌面**自己**在前台时，斜滑手势由 Launcher 的 Activity/ViewGroup 直接处理，
     * 上面的闸门一次都不会被调用 → 上滑照样穿透。
     *
     * 挂的是 `dispatchTouchEvent` / `onTouchEvent`（在 Activity 上声明，`getMethod` 能取到），
     * 回调复用同一个 [Stream]：只有「DOWN 落在角落热区」那一条手势才会丢 MOVE。
     */
    private fun installLauncherTouchHooks(loader: ClassLoader?): Int {
        val lc = runCatching { Class.forName("com.android.launcher3.Launcher", false, loader) }.getOrNull()
            ?: return 0
        var n = 0
        for (nm in arrayOf("dispatchTouchEvent", "onTouchEvent")) {
            val m = runCatching { lc.getMethod(nm, MotionEvent::class.java) }.getOrNull()
                ?: runCatching { lc.getDeclaredMethod(nm, MotionEvent::class.java) }.getOrNull()
                ?: run {
                    log("桌面触摸 " + nm + " 没找到")
                    continue
                }
            val hooked = runCatching { XBridge.hookMethod(m, Stream(dropDown = true)) != null }
                .getOrDefault(false)
            log("桌面触摸 " + nm + " dropDown -> " + hooked)
            if (hooked) n++
        }
        return n
    }

    /** 能当"输入入口"用的方法名（老版本 / 新版本 / 别名都在这儿） */    private val INPUT_NAMES = arrayOf(
        "onDispatchInputEvent", "onInputEvent", "dispatchInputEvent",
        "handleInputEvent", "onInputEventLocked", "dispatchTouchEventInternal"
    )

    /** 能当"要不要响应手势"判定用的方法名 */
    private val GATE_NAMES = arrayOf(
        "canRespondGesture", "canRespondGestureInternal", "shouldInterceptGesture"
    )

    /**
     * 2832/2833：扫方法挂闸门。返回装上的个数。
     *
     * 匹配规则（宁可多挂一个，也别一个都不挂）：
     *  · 参数只有 1 个，且 `MotionEvent` 能赋给它（`MotionEvent` / `InputEvent` 都算）；
     *  · 名字在 [INPUT_NAMES] 里，或者形如 `onXxxInputEvent`。
     * 判定闸门（[GATE_NAMES]）额外要求返回 boolean。
     *
     * **2833：白名单一个都没命中时，按形状兜底全挂。**
     * 实测（对方 Android 17 机器）这个类是**混淆过**的，触摸入口叫 `I` / `J` / `K`：
     * ```
     * OplusBaseTouchInteractionService#I(MotionEvent)
     * OplusBaseTouchInteractionService#J(MotionEvent)
     * OplusBaseTouchInteractionService#K(InputEvent)
     * ```
     * 名字永远猜不完，所以兜底规则是：只看形状，不看名字。
     */
    private fun installGestureHooks(cls: Class<*>): Int {
        var n = 0
        val motion = MotionEvent::class.java
        val seen = HashSet<String>()
        // ---- 第一遍：按名字（能命中就用它，粒度最准） ----
        var c: Class<*>? = cls
        while (c != null && c != Any::class.java) {
            for (m in runCatching { c.declaredMethods }.getOrDefault(emptyArray())) {
                val ps = m.parameterTypes
                if (ps.size != 1) continue
                if (!ps[0].isAssignableFrom(motion)) continue
                val nm = m.name
                val isGate = GATE_NAMES.contains(nm)
                val isInput = INPUT_NAMES.contains(nm) ||
                    (nm.startsWith("on") && nm.contains("InputEvent"))
                if (!isGate && !isInput) continue
                val key = c.simpleName + "#" + nm + "/" + ps[0].simpleName
                if (!seen.add(key)) continue
                if (isGate && m.returnType != java.lang.Boolean.TYPE) continue
                val hooked = runCatching {
                    XBridge.hookMethod(m, if (isGate) Gate() else Stream()) != null
                }.getOrDefault(false)
                log("闸门 " + key + " -> " + hooked)
                if (hooked) n++
            }
            c = c.superclass
        }
        if (n > 0) return n

        // ---- 第二遍（2833 兜底）：混淆名也能挂，只看"形状" ----
        //  条件：单参数、参数就是 MotionEvent 或 InputEvent 本身、返回 void/boolean；
        //  只在 com.*/非框架类里找（别去挂 Service/ContextWrapper 那些）。
        //  返回 boolean 的一并当"判定闸门"处理（菜单展开时返回 false 掐掉手势链）。
        var tried = 0
        c = cls
        while (c != null && c != Any::class.java && tried < 12) {
            val cn = c.name
            val framework = cn.startsWith("android.") || cn.startsWith("androidx.") ||
                cn.startsWith("java.") || cn.startsWith("kotlin.")
            if (!framework) {
                for (m in runCatching { c.declaredMethods }.getOrDefault(emptyArray())) {
                    val ps = m.parameterTypes
                    if (ps.size != 1) continue
                    val pt = ps[0].name
                    if (pt != "android.view.MotionEvent" && pt != "android.view.InputEvent") continue
                    val key = c.simpleName + "#" + m.name + "/" + ps[0].simpleName
                    if (!seen.add(key)) continue
                    val gateToo = m.returnType == java.lang.Boolean.TYPE
                    val hooked = runCatching {
                        XBridge.hookMethod(m, Stream(gateToo)) != null
                    }.getOrDefault(false)
                    log("闸门(兜底) " + key + " gate=" + gateToo + " -> " + hooked)
                    if (hooked) n++
                    tried++
                    if (tried >= 12) break
                }
            }
            c = c.superclass
        }
        if (n == 0) {
            // 还是没挂上：把类里所有单参方法名打出来，方便下一份日志定位（限 40 条）
            val sb = StringBuilder()
            var cnt = 0
            var cc: Class<*>? = cls
            while (cc != null && cc != Any::class.java && cnt < 40) {
                for (m in runCatching { cc.declaredMethods }.getOrDefault(emptyArray())) {
                    if (m.parameterTypes.size == 1 && cnt < 40) {
                        sb.append(cc.simpleName).append('#').append(m.name)
                            .append('(').append(m.parameterTypes[0].simpleName).append(") ")
                        cnt++
                    }
                }
                cc = cc.superclass
            }
            log("闸门一个都没挂上！候选单参方法：" + sb)
        } else {
            log("闸门兜底装了 " + n + " 个")
        }
        return n
    }

    /**
     * 2837：桌面是否在前台（onResume/onPause 维护）。
     *
     * 为什么要它：热区那一下是被 **FlyZone 窗口**吃掉的，桌面自己的 window 根本收不到
     * （所以 `Launcher.dispatchTouchEvent` 那条钩子在这种情况下压根不会被调用）。
     * 桌面能看到的只有 input monitor（TouchInteractionService）。
     * 而只丢 MOVE 时，桌面收到的是「按下且不动」= **长按** → 文件夹进编辑、底部按住进多任务。
     * 但同一个钩子在 App 里也会被调用，连 DOWN 一起丢就把 App 热区的点击吃掉了。
     * → 用这个标志把「丢 DOWN」限定在「桌面在前台」时。
     */
    @Volatile
    @JvmStatic
    var desktopFg = false

    /** 已装上的钩子数（给能力位用） */
    @Volatile
    private var hooks = 0

    /** 上报能力位时用的 Context：优先桌面自己的（写成 "android" 包名会被属主校验拒） */
    @JvmStatic
    fun reportCtx(): Context? = launcherCtx ?: runCatching { Cfg.sysContext() }.getOrNull()

    /**
     * 2829：把「是否在多任务概览」这件事，改由**桌面 Activity 自己**报告。
     *
     * 2828 及以前走的是 `Launcher.getLauncher(Context)` + `StateManager`，实测恒失败：
     * 反编译 classes.dex 可见 `getLauncher(Context)` 的实现就一句
     * `BaseActivity.fromContext(context)` —— 它要求传进去的 Context **本身就是**那个 Activity；
     * 我们手里只有 TouchInteractionService 的 Context，所以必然是 null（日志 `launcher=null`）。
     * 而且就算判出来了，`Cfg.gset()` 在桌面进程里也没有 Context（`Cfg.setCtx` 只在 App 进程调过），
     * 写 Settings.Global 那一句根本不会执行。
     *
     * 现在的做法（全部实测可用）：
     *  - hook `Launcher.onCreate(Bundle)` → 抓到 Activity 实例（也就抓到能写设置的那个 Context）；
     *  - hook `Launcher.onStateSetEnd(BaseState / LauncherState)` → 状态变化就写一次 Settings.Global。
     * 判据用 `LauncherState.overviewUi`（OVERVIEW / OVERVIEW_MODAL_TASK / OVERVIEW_SPLIT_SELECT
     * 带 FLAG_OVERVIEW_UI，该字段正是由它算出来的），再补一个 QUICK_SWITCH（左右滑切任务时也是概览画面）。
     */
    private fun installStateHooks(loader: ClassLoader?): Boolean {
        val lc = runCatching { Class.forName("com.android.launcher3.Launcher", false, loader) }.getOrNull()
            ?: run {
                log("找不到 com.android.launcher3.Launcher")
                return false
            }
        var n = 0
        runCatching {
            val m = lc.getDeclaredMethod("onCreate", android.os.Bundle::class.java)
            if (XBridge.hookMethod(m, Attach()) != null) n++
        }.onFailure { log("hook Launcher.onCreate 失败 " + it) }
        for (nm in arrayOf("onResume", "onPause")) {
            runCatching {
                val m = lc.getDeclaredMethod(nm)
                if (XBridge.hookMethod(m, FgSink(nm == "onResume")) != null) n++
            }.onFailure { log("hook Launcher." + nm + " 失败 " + it) }
        }
        for (sig in arrayOf(
            "com.android.launcher3.statemanager.BaseState",
            "com.android.launcher3.LauncherState"
        )) {
            runCatching {
                val pc = Class.forName(sig, false, loader)
                val m = lc.getDeclaredMethod("onStateSetEnd", pc)
                if (XBridge.hookMethod(m, StateSink()) != null) n++
            }.onFailure { log("hook onStateSetEnd(" + sig.substringAfterLast('.') + ") 失败 " + it) }
        }
        log("FlyGate state hooks=" + n)
        return n > 0
    }

    /**
     * 2818：当前是不是在「多任务 / 概览」界面。
     *
     * ColorOS 的多任务是桌面(Launcher3/Quickstep)自己画的，system_server 那边看不出来，
     * 所以在这里（桌面进程）判一次，写进 Settings.Global 给 system_server 的引索条读。
     * 判不出来就返回 null（不写，保持上次值由超时兜底）。
     */
    private var ovAt = 0L
    private var ovVal = false

    private var ovFailLogged = false

    /** 2829：桌面 Activity 实例（onCreate / onStateSetEnd 抓到），用来直接读状态 */
    @Volatile
    private var launcherObj: Any? = null

    /** 2829：能写 Settings.Global 的 Context —— 必须是桌面自己的包名（uid 校验） */
    @Volatile
    private var launcherCtx: Context? = null

    private const val K_RECENTS = "oupao_recents_visible"

    /** 状态对象 → 是不是概览界面 */
    private fun isOverviewState(st: Any?): Boolean {
        if (st == null) return false
        val c = st.javaClass
        // LauncherState.overviewUi 是 public final boolean，由 FLAG_OVERVIEW_UI 算出来
        if (runCatching { c.getField("overviewUi").getBoolean(st) }.getOrDefault(false)) return true
        // 再补 QUICK_SWITCH（左右滑切任务时画面同样是概览）
        val lc = runCatching { Class.forName("com.android.launcher3.LauncherState", false, c.classLoader) }
            .getOrNull() ?: return false
        for (n in arrayOf("OVERVIEW", "OVERVIEW_MODAL_TASK", "OVERVIEW_SPLIT_SELECT", "QUICK_SWITCH")) {
            val f = runCatching { lc.getField(n).get(null) }.getOrNull()
            if (f != null && f === st) return true
        }
        return false
    }

    /** 从桌面 Activity 实例读当前状态对象（拿不到返回 null） */
    private fun stateOf(l: Any?): Any? {
        if (l == null) return null
        val sm = runCatching { l.javaClass.getMethod("getStateManager").invoke(l) }.getOrNull() ?: return null
        return runCatching { sm.javaClass.getMethod("getState").invoke(sm) }.getOrNull()
    }

    /** 写一次（值没变就不写，避免状态回调里做多余 binder 调用） */
    private fun setOverview(ov: Boolean) {
        if (ov == ovVal) return
        ovVal = ov
        val c = launcherCtx
        if (c != null) {
            runCatching { Cfg.gsetC(c, K_RECENTS, if (ov) "1" else "0") }
        } else {
            runCatching { Cfg.gset(K_RECENTS, if (ov) "1" else "0") }
        }
        log("overview=" + ov)
    }

    /** 每次触摸顺手刷新一次（限流 250ms），开销可忽略 */
    private fun publishOverview(svc: Any?) {
        val now = SystemClock.uptimeMillis()
        if (now - ovAt < 250L) return
        ovAt = now
        // 服务本身就是 Context（Service extends ContextWrapper），兜底存一份
        (svc as? Context)?.let { if (launcherCtx == null) launcherCtx = it.applicationContext }
        var st = stateOf(launcherObj)
        if (st == null) {
            // 还没抓到实例时，最后再试一次官方入口（svc 恰好是 Activity 才有用）
            val l = runCatching {
                val lc = Class.forName("com.android.launcher3.Launcher", false, (svc as? Context)?.classLoader)
                lc.getMethod("getLauncher", Context::class.java).invoke(null, svc) as? Any
            }.getOrNull()
            if (l != null) {
                launcherObj = l
                (l as? Context)?.let { launcherCtx = it }
                st = stateOf(l)
            }
        }
        if (st == null) {
            if (!ovFailLogged) {
                ovFailLogged = true
                Log.i(TAG, "overview 判定失败: 还没抓到桌面 Activity（onCreate/onStateSetEnd 未触发）")
            }
            return
        }
        setOverview(isOverviewState(st))
    }

    /**
     * 3437：**桌面状态轮询**（200ms，只在抓到桌面 Activity 之后跑）。
     *
     * 为什么必须要：`oupao_recents_visible` 原来只靠两种时机刷新 ——
     * 桌面触摸（`publishOverview`）和 `onStateSetEnd`。菜单停留时我们的**全屏可触摸层**
     * 把桌面的触摸全吃了，状态回调在这台 C17 上又很晚，于是：
     *   实测 17:32:49 呼出多任务 → 17:33:00 标志位才变 1（菜单 11 秒后才收起）。
     * 用户："可以关闭菜单，但不是立刻关"。
     * 这里直接轮询桌面状态机的当前 state（一次两次反射，200ms 一次，代价可忽略），
     * 让标志位在 200ms 内就位。
     */
    private val H by lazy { Handler(Looper.getMainLooper()) }

    @Volatile
    private var polling = false

    /** 3442：上次写 `oupao_away_at` 的时刻（限流，避免每次触摸都写 Settings） */
    @Volatile
    private var awayAt = 0L

    /** 3456：底部中间起手的候选（等 UP 确认是上滑才写 away_at） */
    @Volatile
    private var awayCand = false

    @Volatile
    private var awayDownY = 0f

    /** 3459：awayCand 属于哪条手势流（downTime 配对，防跨手势残留） */
    @Volatile
    private var awayDownTime = -1L

    private val pollTask = object : Runnable {
        override fun run() {
            try {
                val st = stateOf(launcherObj)
                if (st != null) setOverview(isOverviewState(st))
            } catch (_: Throwable) {
            }
            H.postDelayed(this, 200L)
        }
    }

    private fun startPoll() {
        if (polling) return
        polling = true
        H.postDelayed(pollTask, 200L)
    }

    /** 2829：onCreate 抓实例（越早越好，onStateSetEnd 之前可能就有触摸） */
    private class Attach : XMH() {
        override fun beforeHookedMethod(param: XMP) {
            try {
                val o = param.thisObject ?: return
                FlyGate.attach(o)
            } catch (_: Throwable) {
            }
        }
    }

    /** 2829：状态变化 → 写 Settings.Global */
    private class StateSink : XMH() {
        override fun beforeHookedMethod(param: XMP) {
            try {
                val o = param.thisObject ?: return
                FlyGate.attach(o)
                FlyGate.setOverview(FlyGate.isOverviewState(param.args.getOrNull(0)))
            } catch (_: Throwable) {
            }
        }
    }

    /** 记住桌面 Activity（它同时是能写设置的 Context） */
    fun attach(o: Any?) {
        if (o == null) return
        launcherObj = o
        startPoll()
        val c = o as? Context ?: return
        launcherCtx = c
        // 2829：这时才有"包名属于本进程 uid"的 Context，能力位能真正写出去
        runCatching {
            Caps.reportWith(c, "launcher", hooks > 0)
            // 新增：多任务隐藏小窗的能力位也在这里补报一次。
            //   模块加载那一刻 launcherCtx 还是 null、systemContext 的包名是 "android"
            //   会被属主校验拒掉 → 那条上报永远写不出去，设置页就会一直显示「不支持」。
            Caps.reportWith(c, "recents", RecentsHide.installed)
            Caps.flushPending(c)
        }
    }

    /** 控流：热区手势只丢 MOVE */
    private class Stream(
        private val gateToo: Boolean = false,
        /**
         * 2836：**只有在桌面自己那条触摸链上**才连 DOWN 一起丢。
         *
         * 只丢 MOVE 时桌面收到「按下且一直不动」= 长按 → 文件夹进编辑 / 底部按住进多任务。
         * 但这条钩子（TouchInteractionService）在 **App 里也会被调用**，连 DOWN 一起丢
         * 会把 App 里热区的点击也吃掉（用户实测：App 角落点不动了）。
         * 所以「丢 DOWN」只给 Launcher 自己的 dispatchTouchEvent / onTouchEvent。
         */
        private val dropDown: Boolean = false
    ) : XMH() {
        override fun beforeHookedMethod(param: XMP) {
            try {
                val ev = param.args.getOrNull(0) as? MotionEvent ?: return
                // 2835：自己「点击重放」注入的事件一律放行，否则角落点击会被自己丢掉变死区
                if (com.oupao.wiz.IdxTouch.injectingNow()) return
                val ctx = param.thisObject as? Context
                // 2833：兜底挂上的方法里，返回 boolean 的那个很可能就是「能不能响应手势」，
                //   菜单展开时直接返回 false，把这条手势链彻底掐掉。
                if (gateToo && ev.actionMasked == MotionEvent.ACTION_DOWN && FlyState.open()) {
                    param.result = false
                    param.returnEarly = true
                    logOnce("menu open -> block gesture chain")
                    return
                }
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        runCatching { publishOverview(param.thisObject) }
                        startX = ev.rawX
                        startY = ev.rawY
                        gateDown = if (inZone(ev.rawX, ev.rawY, ctx)) ev.downTime else -1L
                        // 2835：热区起手时 **DOWN 也一起丢**。
                        //   只丢 MOVE 的话，桌面收到的是「按下 + 一直不动」——
                        //   在桌面上这正好是**长按**：文件夹会进编辑态、底部按住一会会进多任务。
                        //   （用户实测：菜单滑出来后不松手等一会，就进多任务/文件夹编辑。）
                        //   整条流都不给桌面，它的手势链和长按判定就都不会启动。
                        if ((dropDown || desktopFg) && gateDown >= 0L) {
                            param.returnEarly = true
                        }
                        logOnce("down x=" + ev.rawX.toInt() + " y=" + ev.rawY.toInt() +
                            " gate=" + (gateDown >= 0L) + " fg=" + desktopFg
                                + if ((dropDown || desktopFg) && gateDown >= 0L) " drop" else "")
                        // 3463：候选 = **底部条起手**（y ≥ H-150）。
                        //   不再要求"在角落热区外"——热区是半径 560 的圆，左右两个圆几乎
                        //   把 1216 宽的底边盖满（只剩 x∈(560,656) 一条缝），那条判据实际上
                        //   永远不成立 → 桌面开多任务只能等慢标志位（用户："要等几秒才关"）。
                        //   真正的区分靠 UP：同一个 downTime 上滑 ≥60px 才算"用户离开"。
                        //   （角落手势本身也会写一次，但那时菜单要么没开、要么本来就该收）
                        val dmr = ctx?.resources?.displayMetrics
                        val sh = dmr?.heightPixels ?: 0
                        val sw = dmr?.widthPixels ?: 0
                        awayDownY = ev.rawY
                        awayDownTime = ev.downTime
                        awayCand = (sh > 0) && (sw > 0) && (ev.rawY >= (sh - 150))
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (gateDown < 0L || ev.downTime != gateDown) return
                        val slop = slopOf(ctx)
                        val dx = ev.rawX - startX
                        val dy = ev.rawY - startY
                        if (dx * dx + dy * dy > slop * slop) {
                            // 越过 slop 的 MOVE 一律不发 → 桌面认为"没上滑"，不会回桌面也不会拖前台
                            param.returnEarly = true
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        // 3458：确认是"底部中间起手 + 明显上滑"才写离开时间戳
                        // 3459：**必须同一个 downTime** —— awayCand 是跨手势残留的，
                        //   上一次候选的 UP 被吃掉后，后面任意一次 UP（比如角落手势的 UP）
                        //   都会拿旧的下压点算 dy → 误判成"用户离开"，菜单刚停留就被收掉。
                        if (awayCand) {
                            awayCand = false
                            if ((ev.downTime == awayDownTime) && ((awayDownY - ev.rawY) >= 60f)) {
                                val nowT = SystemClock.uptimeMillis()
                                if (nowT - awayAt > 400L) {
                                    awayAt = nowT
                                    runCatching {
                                        ctx?.let { Cfg.gsetC(it, "oupao_away_at", nowT.toString()) }
                                    }
                                    logOnce("away 上滑 dy=" + (awayDownY - ev.rawY).toInt())
                                }
                            }
                        }
                        if (gateDown >= 0L && ev.downTime == gateDown) {
                            gateDown = -1L
                            // 菜单已经被呼出：这一下别再变成对前台 App 的点击
                            if (FlyState.open()) param.returnEarly = true
                        }
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        awayCand = false
                        gateDown = -1L
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    /** 2837：桌面 onResume/onPause → 维护 desktopFg */
    private class FgSink(private val on: Boolean) : XMH() {
        override fun beforeHookedMethod(param: XMP) {
            try {
                desktopFg = on
                log("桌面 onResume/onPause fg=" + on)
            } catch (_: Throwable) {
            }
        }
    }

    /** 菜单展开时：这条手势完全不给桌面手势链 */
    private class Gate : XMH() {
        override fun beforeHookedMethod(param: XMP) {
            try {
                val ev = param.args.getOrNull(0) as? MotionEvent ?: return
                if (ev.actionMasked != MotionEvent.ACTION_DOWN) return
                if (!FlyState.open()) return
                param.result = false
                param.returnEarly = true
                logOnce("menu open -> block gesture chain")
            } catch (_: Throwable) {
            }
        }
    }

    // ---------- 工具 ----------

    private fun inZone(x: Float, y: Float, ctx: Context?): Boolean {
        val lr = FlyState.lr()
        val r = FlyState.radius()
        if (lr == 0 || r <= 0) return false
        val dm = runCatching { ctx?.resources?.displayMetrics }.getOrNull() ?: return false
        var w = dm.widthPixels.toFloat()
        var h = dm.heightPixels.toFloat()
        runCatching {
            val wm = ctx?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val b = wm?.maximumWindowMetrics?.bounds
            if (b != null && b.width() > 0 && b.height() > 0) {
                w = b.width().toFloat()
                h = b.height().toFloat()
            }
        }
        return FlyGeom.side(x, y, w, h, r.toFloat(), lr) != FlyGeom.SIDE_NONE
    }

    private fun slopOf(ctx: Context?): Float = runCatching {
        val c = ctx ?: return@runCatching 24f
        ViewConfiguration.get(c).scaledTouchSlop.toFloat()
    }.getOrDefault(24f)

    private fun logOnce(msg: String) {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastLog < 250L) return
        lastLog = now
        log(msg)
    }

    private fun log(msg: String) {
        try {
            Log.i(TAG, msg)
        } catch (_: Throwable) {
        }
        // 2832：同时走 SLog —— 它会转发进 LSPosed 的模块日志。
        // 用户导出的 bug 包里 logcat 往往不含桌面进程的 OupaoFlyGate 标签，
        // 但模块日志一定有（实测对方日志里就有 `Hook注册 ...`），
        // 所以闸门装没装上必须能从模块日志里看到。
        runCatching { SLog.i("FlyGate: " + msg) }
    }
}
