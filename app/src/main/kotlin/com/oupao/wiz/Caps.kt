package com.oupao.wiz

import android.content.Context
import android.util.Log

/**
 * 2829：能力检测 —— 「当前这套系统到底支不支持这个功能」。
 *
 * ## 用户诉求
 * 「开关开启时要检测系统是否支持这个钩子，不支持就显示不支持，而不是不管支不支持都能开启」
 *
 * ## 做法
 * 装钩子的进程（system_server / SystemUI / 侧边栏 / 桌面）在**装完之后**，把每个功能
 * 「真的挂上了几个 hook」写进 Settings.Global：`oupao_cap_<name>` = `1` / `0`。
 * App 侧 [supported] 读它 → 设置页那一行显示「当前系统不支持此功能」、开关置灰、点了也不给开。
 *
 * ## 为什么用 hookCount 增量而不是 try/catch
 * 各 installer 内部大量「找不到类就 SLog 一句然后 return」，**不会抛异常**，
 * 所以 catch 不到失败。而 `XBridge.hookCount` 只在真的注册了 hook 时才自增，
 * 用它前后差值判断「这一段有没有真的挂上东西」才是可信的。
 *
 * ## 读不到时一律放行
 * 写 Settings.Global 需要权限，某些进程（侧边栏）可能写不进去。
 * 这时 App 读回来是 null → 当作「未知」→ 开关照常可用。
 * 宁可少拦，也不要因为读不到就把好功能灰掉。
 */
object Caps {

    private const val TAG = "OupaoCaps"

    /** 置灰那一行显示的文案（Java 侧 Ui.java 也会用） */
    @JvmField
    val NO_SUPPORT = "当前系统不支持此功能"

    /** 键名：Cfg.gsetC 会再补一个 "oupao_" 前缀 → 最终 Settings 键是 `oupao_cap_<name>` */
    private const val PREFIX = "cap_"

    /** 待上报：name -> 值。写不进去（还没拿到可用 Context）就留在这里，下次再试。 */
    private val pending = LinkedHashMap<String, Boolean>()

    // ---------------- 上报侧（跑在各系统进程里） ----------------

    @JvmStatic
    fun report(name: String, ok: Boolean) = reportWith(null, name, ok)

    /**
     * @param c 调用方自己的 Context。桌面（launcher）必须传自己的 —— 用 ActivityThread
     *          的 systemContext（包名 "android"）写 Settings 会被属主校验拒掉。
     */
    @JvmStatic
    fun reportWith(c: Context?, name: String, ok: Boolean) {
        runCatching {
            synchronized(pending) {
                // 已经按这个值写成功过就不用再写
                if (!pending.containsKey(name) && reportedPassed(name, ok)) return
                pending[name] = ok
            }
            flushPending(c)
        }
    }

    private val sentOk = HashSet<String>()

    /** 重试计数（每个进程最多补 6 次），避免无限 post */
    private var retries = 0

    private fun reportedPassed(name: String, ok: Boolean) = sentOk.contains(name + "=" + ok)

    /**
     * 把待上报的能力位全部写一遍。装钩子那一刻很多进程还拿不到能写设置的 Context
     * （system_server 的 systemContext 常是 null，`Cfg.ctx` 要等第一份广播才被填上），
     * 所以这里做完还会排一次延迟重试；Rx 收到广播时也会再调一次。
     */
    @JvmStatic
    fun flushPending(c: Context? = null) {
        runCatching {
            val names = synchronized(pending) { pending.keys.toList() }
            if (names.isEmpty()) return
            val ctxs = candidates(c)
            if (ctxs.isEmpty()) {
                scheduleRetry()
                return
            }
            val done = ArrayList<String>()
            // 逐个候选 Context 试：写成功就说明这个 Context 的包名/uid 是匹配的
            for (ctx in ctxs) {
                for (n in names) {
                    if (done.contains(n)) continue
                    val ok = synchronized(pending) { pending[n] } ?: continue
                    if (Cfg.gsetC(ctx, PREFIX + n, if (ok) "1" else "0")) done.add(n)
                }
                if (done.size == names.size) break
            }
            if (done.isNotEmpty()) {
                synchronized(pending) {
                    for (n in done) {
                        val ok = pending.remove(n)
                        if (ok != null) sentOk.add(n + "=" + ok)
                    }
                }
                Log.i(TAG, "cap flushed " + done)
            }
            if (synchronized(pending) { pending.isNotEmpty() }) scheduleRetry()
        }
    }

    /**
     * 能拿去写 Settings.Global 的 Context 候选（按优先级）。
     *
     * 关键坑：`ActivityThread.getSystemContext()` 拿到的 Context 包名是 **"android"**，
     * 在别的进程里用它写 Settings 会被属主校验拒（`SecurityException`），
     * 所以要优先用"本进程自己的" Application/SystemUI Context。
     */
    private fun candidates(c: Context?): List<Context> {
        val out = ArrayList<Context>()
        c?.let { out.add(it) }
        // 3392：桌面作用域里 FlyGate 可能因为新桌面没有
        //   OplusBaseTouchInteractionService 直接 return，launcherCtx 永远拿不到。
        //   这时 RecentsHide 仍然装成功了，但 recents 能力位写不出去 → 设置页误报“不支持”。
        //   ActivityThread.currentApplication() 是当前进程自己的 Application，包名/uid 正确。
        runCatching {
            val at = Class.forName("android.app.ActivityThread")
            at.getMethod("currentApplication").invoke(null) as? Context
        }.getOrNull()?.let { out.add(it) }
        runCatching { Rx.ctxB() }.getOrNull()?.let { out.add(it) }      // 本进程 Application
        runCatching { Ctx.sysUi() }.getOrNull()?.let { out.add(it) }    // SystemUI
        runCatching { Cfg.ctx }.getOrNull()?.let { out.add(it) }        // App 注入的
        runCatching { Cfg.sysContext() }.getOrNull()?.let { out.add(it) }
        return out.filter { runCatching { it.packageName != "android" }.getOrDefault(false) }
    }

    private fun scheduleRetry() {
        if (retries >= 6) return
        retries++
        runCatching {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                runCatching { flushPending(null) }
            }, 3000L)
        }
    }

    // ---------------- 读取侧（跑在 App 进程里） ----------------

    /**
     * @return true=支持，false=系统明确上报过不支持
     *
     * 注意：**读不到（未知）必须返回 true**。两个 UI 调用方都写成
     * `Caps.supported(cap) == true`，旧实现返回 null 会被当成“不支持”。
     * C17 新桌面（17.0.1.201）没有 `OplusBaseTouchInteractionService`，
     * launcher 侧能力位写不出去；但 `RecentsHideC17: hooked=true` 功能实际生效，
     * 结果设置页一直灰着。只有系统明确写过 "0" 才置灰。
     */
    @JvmStatic
    fun supported(name: String): Boolean? {
        // 3313：C17 系统默认「上滑不暂停」，这一项强制标记为不支持（设置页置灰、无法打开）
        if (name == "video" && Cfg.isC17()) return false
        // 3313+/3392：C17 新桌面（17.0.1.201）没有 OplusBaseTouchInteractionService，
        //   launcher 侧能力位写不出去；但 RecentsHideC17 已实测 hooked=true。
        //   这条能力位不能因为写不出去/旧值 0 把功能锁死。
        if (name == "recents" && Cfg.isC17()) return true
        val c = Ui.appCtx ?: Cfg.ctx ?: return true
        val v = runCatching {
            android.provider.Settings.Global.getString(c.contentResolver, "oupao_" + PREFIX + name)
        }.getOrNull()
        return v != "0"
    }

    /** 设置键 → 能力名。没有登记的能力按前缀兜底，保证每个用户开关都有检测。 */
    private val KEY_CAP: Map<String, String> = mapOf(
        // 扇形菜单
        "oupao_fly_master" to "fly",
        "oupao_flyop_master" to "fly",
        "oupao_fly_pins" to "fly",
        "oupao_fly_stay_off" to "fly",
        "oupao_fly_scale" to "fly",
        "oupao_fly_range" to "fly",
        "oupao_icon_size" to "fly",
        "oupao_fly_ui" to "flyb",
        "oupao_fly_ui_bar" to "flyb",
        "oupao_fly_border_off" to "flyb",
        "oupao_fly_mini" to "mini",
        "oupao_fly_mini_easy" to "mini",
        // 字母引索（Boot 没有单独的 idx 能力位，Hooks 里包含它）
        "oupao_idx_master" to "hooks",
        "oupao_idx_global" to "hooks",
        "oupao_idx_game" to "hooks",
        "oupao_idx_vib" to "hooks",
        // 通知气泡 / 抬头拦截
        "oupao_sug_master" to "ui",
        "oupao_nohu_all" to "ui",
        "oupao_bub_v2" to "ui",
        "oupao_bub_persist" to "ui",
        "oupao_bub_dbl_close" to "ui",
        "oupao_bub_sec" to "ui",
        "oupao_bub_info_sec" to "ui",
        // 轻量打开（通知点击 / 应用间跳转 / 设置页）
        "oupao_light_open" to "zoom",
        // 侧边栏
        "oupao_sidebar_noswipe" to "sidebar",
        // 小窗数量/叠放
        "oupao_zoom_max_on" to "flexfix",
        "oupao_zoom_max" to "flexfix",
        "oupao_zoom_cascade" to "flexfix",
        // 锁屏滑动输入
        "oupao_keyguard_slide" to "slide",
        // 通知收纳
        "oupao_notif_box" to "notifbox",
        "oupao_notif_box_auto" to "notifbox",
        // 游戏模式
        "oupao_game_master" to "hooks",
        "oupao_game_block" to "hooks",
        "oupao_game_block_sug" to "hooks",
        // 视频/小窗相关
        "oupao_video_no_pause" to "video",
        "oupao_mini_big" to "mini",
        "oupao_dock_keep_alive" to "dock",
        "oupao_longpress_mute" to "mute",
        "oupao_no_splash" to "hooks",
        "oupao_screen_keep" to "dock",
        "oupao_cap_clean" to "hooks",
        "oupao_corner_off" to "hooks",
        "oupao_landscape_fullscreen" to "hooks",
        "oupao_double_tap_close" to "hooks",
        // 桌面进程
        "oupao_recents_hide_freeform" to "recents",
        // 3502：重启后清空多任务列表（system_server 的 Boot 任务）
        "oupao_recents_clear_boot" to "hooks",
        // SystemUI 电源菜单
        "oupao_power_soft_reboot" to "power",
        // 其它：按 hooks 能力兜底
        "oupao_hide_icon" to "hooks",
    )

    /** 这一行依赖哪个能力；没有登记就按前缀给一个可检测的默认能力。 */
    @JvmStatic
    fun capOf(key: String?): String? {
        if (key == null) return null
        KEY_CAP[key]?.let { return it }
        return when {
            key.startsWith("oupao_fly") -> "fly"
            key.startsWith("oupao_idx") -> "hooks"
            key.startsWith("oupao_sug") || key.startsWith("oupao_bub") -> "ui"
            key.startsWith("oupao_game") -> "hooks"
            key == "oupao_light_open" -> "zoom"
            key == "oupao_power_soft_reboot" -> "power"
            else -> "hooks"
        }
    }
}
