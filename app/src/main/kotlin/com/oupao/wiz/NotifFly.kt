package com.oupao.wiz

import android.content.Intent
import android.os.Bundle

/**
 * 2890：**回到「注入路线」**（= 我这边 ≤2850 的做法，也是开源那套的本质）。
 *
 * 两类点击分开处理，谁也不抢谁：
 *
 * 1. **横幅（面板已完全收起）** → `onStartFloatWindow(starter,row,0f)`
 *    横幅点开的「下滑展开」动画就是这条通道自带的 transition，必须留着。
 *
 * 2. **通知中心（面板展开）** → **什么都不抢**：不打 returnEarly，让系统的点击流程原样跑
 *    （系统自己先收面板、收完启动、动画是一体的），我们只在
 *    `setNotificationTypeStarterLog(Bundle, entry)` 上把 zoom 参数**注入到系统算好的 bundle** 里，
 *    于是系统按小窗启动、顺序和动画都跟系统原生一致、也没有任何人为延时。
 *
 * 历史教训：
 *  - 2882 之后改成「抢 onNotificationClicked 再 collapse+220ms 后自己启动」→ 延迟、顺序反。
 *  - 2889 用 `addPostCollapseAction` 是照 ROM 的编排，但仍是「等面板收完才启动」→ 还是延迟。
 *  - 结论：通知中心的点击**不能抢**，只能注入。
 */
object NotifFly {

    private const val STARTER_CLASS =
        "com.android.systemui.statusbar.phone.StatusBarNotificationActivityStarter"

    /** 带 `setNotificationTypeStarterLog(Bundle, NotificationEntry)` 的类（2850 的候选表）。 */
    private val LOG_CANDIDATES = arrayOf(
        "com.oplus.systemui.statusbar.phone.OplusStatusBarNotificationActivityStarterExImpl",
        "com.android.systemui.statusbar.phone.OplusStatusBarNotificationActivityStarterExImpl",
        "com.android.systemui.statusbar.phone.OplusStatusBarNotificationActivityStarterEx",
    )

    /** 这几个来源不该进小窗。 */
    private val SKIP = setOf("com.oupao.wiz", "com.android.systemui", "android")

    @Volatile
    private var installed = false

    /** 横幅那次启动进行中（MarkCB 据此决定不剥转场）。 */
    @Volatile
    private var bannerLaunch = false

    /** 2929：横幅点击要「先关横幅、再出小窗」——记下原方法，横幅收完后重放它。 */
    @Volatile
    private var clickMethod: java.lang.reflect.Method? = null

    /** 重放进行中（避免自己拦自己）。 */
    @Volatile
    private var replaying = false

    @JvmStatic
    fun install(cl: ClassLoader?): Int {
        if (installed) return -1
        var n = 0

        // 1) 横幅通道：onNotificationClicked(entry,row)
        val starter = runCatching { Rt.cls(cl, STARTER_CLASS) }.getOrNull()
        if (starter != null) {
            val click = starter.declaredMethods.firstOrNull {
                it.name == "onNotificationClicked" && it.parameterCount == 2
            }
            if (click != null) {
                runCatching { click.isAccessible = true }
                if (XBridge.hookMethod(click, ClickCB()) != null) {
                    clickMethod = click
                    n++
                }
            }
        }

        // 2) 注入通道：setNotificationTypeStarterLog(Bundle, entry)
        for (name in LOG_CANDIDATES) {
            val cls = runCatching { Rt.cls(cl, name) }.getOrNull() ?: continue
            val m = runCatching { Rt.methodByName(cls, "setNotificationTypeStarterLog", 2) }
                .getOrNull() ?: continue
            if (XBridge.hookMethod(m, MarkCB()) != null) {
                n++
                SLog.r("NFly", "NFly: \u6ce8\u5165\u94a9\u5b50 -> " + name, 3000L)
                break
            }
        }

        installed = n > 0
        SLog.r("NFly", "NFly: \u94a9\u5b50 " + n + " \u4e2a", 3000L)
        return n
    }

    /** 通知中心：把 zoom 参数注入系统算好的 bundle（不抢流程、不加延时）。 */
    class MarkCB : XMH() {
        override fun beforeHookedMethod(param: XMP) {
            runCatching {
                if (!Cfg.notifFly()) return@runCatching
                if (!orientationAllowed()) return@runCatching
                // 2975：灭屏/锁屏不做通知小窗注入
                if (Cfg.sleepingNow()) return@runCatching
                val bundle = param.argOrNull(0) as? Bundle ?: return@runCatching
                val pkg = pkgOfEntry(param.argOrNull(1))
                if (pkg != null && SKIP.contains(pkg)) return@runCatching
                // 2891：通知中心那条**剥掉系统的 remote transition**。
                //   不剥的话就是「Activity 从全屏缩成小窗」那段扯的动画（用户 2890 反馈）。
                //   悬浮横幅那条**不能剥** —— 下滑展开动画就是它（用户 2889 已反馈过）。
                val dropped = if (bannerLaunch) "-" else dropRemoteTransition(bundle)
                bundle.putInt("extra_window_mode", 100)
                bundle.putInt("android:activity.mZoomLaunchFlags", 5)
                bundle.putInt("androidx.activity.LaunchScenario", 1)
                // 2892：**把通知中心的收起提前到注入这一刻**。
                //   剥掉 remote transition 之后，系统那段「面板收起 + 窗口一起动」的转场没了，
                //   于是小窗先冒出来、面板才慢吞吞收（用户 2891 反馈）。
                //   这里直接用 ROM 自己的 shadeController.collapseShade(true)（带动画）先把面板收起来，
                //   Activity 随后才启动 → 顺序就是「先收中心、再出小窗」。
                if (!bannerLaunch) {
                    val shade = Rt.get(param.thisObject, "shadeController")
                    if (shade != null) Rt.callLoose(shade, "collapseShade", arrayOf<Any?>(true))
                }
                SLog.r("NFly", "NFly: \u6ce8\u5165 zoom pkg=" + pkg + " \u5265\u79bb=" + dropped, 1500L)
            }.onFailure { SLog.e("NFly: \u6ce8\u5165\u5f02\u5e38", it) }
        }
    }

    /**
     * 2932：把悬浮横幅（heads-up）真正释放掉。
     * `collapseShade()` 只收面板，heads-up 还挂着 → 小窗先出、横幅后没。
     * 这里把 starter 里所有类型名带 HeadsUp 的字段找出来，调它的 releaseAllImmediately/releaseImmediately。
     */
    /**
     * 找到 starter 里持有的 HeadsUpManagerImpl。
     * ColorOS 17 的 `releaseAllImmediately` 签名是 `(String reason)`，不是无参。
     */
    private fun findHeadsUpMgr(starter: Any?): Any? {
        if (starter == null) return null
        var c: Class<*>? = starter.javaClass
        while (c != null && c.name != "java.lang.Object") {
            for (f in c.declaredFields) {
                if (!f.type.name.contains("HeadsUp", true)) continue
                runCatching { f.isAccessible = true }
                val hum = runCatching { f.get(starter) }.getOrNull()
                if (hum != null) return hum
            }
            c = c.superclass
        }
        return null
    }

    /** 返回是否真的调用成功（不是“找到了字段”）。 */
    private fun releaseHeadsUp(hum: Any?): Boolean {
        if (hum == null) return false
        val withReason = runCatching {
            hum.javaClass.getMethod("releaseAllImmediately", String::class.java)
                .invoke(hum, "oupao zoom click")
            true
        }.getOrDefault(false)
        if (withReason) return true
        val noArg = runCatching {
            hum.javaClass.getMethod("releaseAllImmediately").invoke(hum)
            true
        }.getOrDefault(false)
        if (noArg) return true
        return runCatching {
            hum.javaClass.getMethod("releaseAllImmediately", Boolean::class.javaPrimitiveType)
                .invoke(hum, true)
            true
        }.getOrDefault(false)
    }

    /** 横幅是否还在（或正在上滑消失）。 */
    private fun headsUpBusy(hum: Any?): Boolean {
        if (hum == null) return false
        val has = runCatching {
            com.oupao.wiz.Rt.callNoArg(hum, "hasNotifications") as? Boolean
        }.getOrNull()
        val anim = runCatching {
            (com.oupao.wiz.Rt.callNoArg(hum, "isHeadsUpAnimatingAwayValue") as? Boolean)
                ?: (com.oupao.wiz.Rt.callNoArg(hum, "isHeadsUpAnimatingAway") as? Boolean)
        }.getOrNull()
        return has == true || anim == true
    }

    /** 删掉 bundle 里所有「remote animation/transition」键，返回删掉的键名（没有则 "-"）。 */
    private fun dropRemoteTransition(bundle: Bundle): String {
        val sb = StringBuilder()
        for (key in bundle.keySet().toList()) {
            val lower = key.lowercase(java.util.Locale.ROOT)
            if (!lower.contains("remote")) continue
            if (!lower.contains("animation") && !lower.contains("transition")) continue
            bundle.remove(key)
            if (sb.isNotEmpty()) sb.append(',')
            sb.append(key)
        }
        return if (sb.isEmpty()) "-" else sb.toString()
    }

    /** 横/竖屏通知开关。 */
    private fun orientationAllowed(): Boolean {
        return try {
            val land = android.content.res.Resources.getSystem().configuration.orientation ==
                    android.content.res.Configuration.ORIENTATION_LANDSCAPE
            if (land) Cfg.lightNotifLand() else Cfg.lightNotifPort()
        } catch (t: Throwable) {
            true
        }
    }

    /** 横幅：面板已完全收起时走系统浮窗通道（下滑展开动画）。 */
    class ClickCB : XMH() {
        override fun beforeHookedMethod(param: XMP) {
            runCatching {
                val starter = param.thisObject ?: return@runCatching
                val row = param.args.getOrNull(1) ?: return@runCatching
                // 面板展开（通知中心）→ 一律不抢，交给系统原生流程 + MarkCB 注入
                val collapsed = Rt.callNoArg(
                    Rt.get(starter, "mPanelExpansionInteractor"),
                    "isFullyCollapsed",
                ) as? Boolean
                if (collapsed != true) return@runCatching
                // 2929（用户要求）：悬浮横幅 → 「**先关横幅**、再出小窗」。
                //   2928 让系统自己走，结果小窗先出、横幅后收（用户反馈反了）。
                //   所以这里自己掌控顺序：先 collapseShade 把横幅收掉，取消本次栏截，
                //   靠延时重放原方法（横幅这时已经收完）；重放那一次不再拦截，
                //   由 MarkCB 注入 zoom 参数，并且 bannerLaunch=false → 会剥掉 remote transition。
                if (replaying) return@runCatching
                val m0 = clickMethod
                if (m0 == null) {
                    SLog.r("NFly", "NFly: 没捕到 click 方法，横幅走系统原生", 3000L)
                    return@runCatching
                }
                runCatching {
                    val shade = Rt.get(starter, "shadeController")
                    if (shade != null) Rt.callLoose(shade, "collapseShade", arrayOf<Any?>(true))
                }.onFailure { SLog.e("NFly: 横幅收起失败", it) }
                // 2930：光 collapseShade 有时只是把面板收起来，悬浮横幅本身还挂着。
                //   再把 heads-up 直接释放掉（它自带上滑消失动画）。
                val hum = findHeadsUpMgr(starter)
                val released = releaseHeadsUp(hum)
                if (hum != null) {
                    SLog.r("NFly", "NFly: heads-up 释放 ok=" + released, 1500L)
                } else {
                    SLog.r("NFly", "NFly: 没找到 heads-up 管理器字段", 3000L)
                }
                val entry = param.args.getOrNull(0)
                param.returnEarly = true
                param.result = null
                // 2935：heads-up 已经被 releaseAllImmediately() **瞬间取消**（没有上滑动画），
                //   所以不用再等动画：默认 **40ms** 就重放；没找到 heads-up 才退回 140ms。
                // 2947（用户：还是有点慢）：heads-up 已被瞬间取消 → **0ms**（只走一次 post）立即重放。
                val defWait = if (released) 0L else 120L
                val wait = runCatching {
                    (Cfg.getProp("persist.oupao.banner_delay", defWait.toString()).toLongOrNull() ?: defWait)
                        .coerceIn(0L, 600L)
                }.getOrDefault(defWait)
                SLog.r("NFly", "NFly: 横幅点击 → 先收横幅 released=" + released + " wait=" + wait + "ms 后再开小窗", 1500L)
                val replay: Runnable = Runnable {
                    replaying = true
                    runCatching { m0.invoke(starter, entry, row) }
                        .onSuccess { SLog.r("NFly", "NFly: 横幅已收起，已重放点击", 1500L) }
                        .onFailure { SLog.e("NFly: 横幅重放失败", it) }
                    replaying = false
                }
                val h = android.os.Handler(android.os.Looper.getMainLooper())
                if (released && wait <= 0L) {
                    // 释放成功不等于画面已经消失：轮询到 hasNotifications=false 且不在退场动画，
                    // 再重放点击，保证「先关横幅、后开小窗」。
                    val t0 = android.os.SystemClock.uptimeMillis()
                    val poll = object : Runnable {
                        override fun run() {
                            val spent = android.os.SystemClock.uptimeMillis() - t0
                            if (!headsUpBusy(hum) || spent >= 250L) {
                                SLog.r("NFly", "NFly: 横幅已消失(" + spent + "ms) → 重放点击", 1500L)
                                replay.run()
                            } else {
                                h.postDelayed(this, 16L)
                            }
                        }
                    }
                    h.post(poll)
                } else if (wait <= 0L) {
                    h.post(replay)
                } else {
                    h.postDelayed(replay, wait)
                }
                return@runCatching
            }.onFailure { SLog.e("NFly: \u70b9\u51fb\u5904\u7406\u5f02\u5e38", it) }
        }
    }

    /** `starter.mActivityStarterEx.onStartFloatWindow(starter, row, 0f)` */
    @JvmStatic
    fun startFloatWindow(starter: Any?, row: Any?): Boolean {
        if (starter == null || row == null) return false
        return runCatching {
            val ex = Rt.get(starter, "mActivityStarterEx") ?: return false
            val m = ex.javaClass.methods.firstOrNull {
                it.name == "onStartFloatWindow" && it.parameterCount == 3
            } ?: return false
            m.isAccessible = true
            (m.invoke(ex, starter, row, 0f) as? Boolean) ?: false
        }.getOrDefault(false)
    }

    private fun pkgOfEntry(entry: Any?): String? =
        runCatching {
            Rt.callNoArg(Rt.callNoArg(entry, "getSbn"), "getPackageName") as? String
        }.getOrNull()
}
