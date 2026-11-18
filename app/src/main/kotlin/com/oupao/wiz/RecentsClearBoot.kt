package com.oupao.wiz

import android.os.SystemClock

/**
 * 3518：开关「重启后清空多任务列表」的 system_server 侧。
 *
 * 现在只做一件事：开机完成 + user 0 解锁 + 开关打开时，写
 * `sys.oupao.recents.clear_at = elapsedRealtime`。
 *
 * 真正的清空交给 launcher 进程的 [RecentsBootClearLauncher]：Recents 视图可见时
 * 调原生 `dismissAllTasks(view)`。3517 实测这条路径有效；system_server 直接清
 * ATMS/RecentTasks 会和未初始化的 `OplusZoomTaskCloneController` /
 * `OplusFlexibleDCSManager` 打架（NPE，且要 15~25s 才勉强成功），已经全部删除。
 *
 * 跨进程只走 `sys.` 属性：`Settings.Global` system_server 写会被 SELinux 拒
 * （实测 global=false），`/data/system/oupao_cfg` 桌面读不到（实测 at=0）。
 */
internal object RecentsClearBoot {

    private const val TAG = "RecentsClearBoot"
    private const val P_CLEAR_AT = "sys.oupao.recents.clear_at"
    /** 3502~3508 用过的旧标记文件；现在不再需要，启动时清掉。 */
    private const val OLD_MARKER = "/data/system/oupao_cfg/oupao_recents_clear_boot_id"

    /** 每个 system_server 进程只排一次；软重启/开机都是新进程，所以都会写一次。 */
    private val scheduled = java.util.concurrent.atomic.AtomicBoolean(false)

    @JvmStatic
    fun schedule() {
        if (!scheduled.compareAndSet(false, true)) {
            return
        }
        val t = Thread({ run() }, "Oupao-RecentsClearBoot")
        t.isDaemon = true
        t.start()
    }

    private fun run() {
        try {
            runCatching { java.io.File(OLD_MARKER).delete() }
            if (!waitBootCompleted()) {
                SLog.i("$TAG: 未等到 boot_completed，本次不清理")
                return
            }
            if (!waitUserUnlocked()) {
                SLog.i("$TAG: user 0 未解锁，本次不清理")
                return
            }
            if (!switchOnWithRetry()) {
                SLog.i("$TAG: 开关关，跳过")
                return
            }
            val at = SystemClock.elapsedRealtime()
            val ok = FlyState.put(P_CLEAR_AT, at.toString())
            SLog.i("$TAG: 已写时间戳 at=" + at + " ok=" + ok)
        } catch (t: Throwable) {
            SLog.e("$TAG: 异常", t)
        }
    }

    private fun waitBootCompleted(): Boolean {
        val deadline = SystemClock.elapsedRealtime() + 180_000L
        while (SystemClock.elapsedRealtime() < deadline) {
            val v = runCatching { Cfg.getProp("sys.boot_completed", "") }.getOrDefault("")
            if (v == "1") {
                return true
            }
            Thread.sleep(500L)
        }
        SLog.i("$TAG: 等 sys.boot_completed 超时")
        return false
    }

    private fun waitUserUnlocked(): Boolean {
        val deadline = SystemClock.elapsedRealtime() + 120_000L
        while (SystemClock.elapsedRealtime() < deadline) {
            val v = runCatching { Cfg.getProp("sys.user.0.ce_available", "") }.getOrDefault("")
            if (v == "true" || v == "1") {
                return true
            }
            Thread.sleep(500L)
        }
        SLog.i("$TAG: 等 user 0 解锁超时")
        return false
    }

    private fun switchOnWithRetry(): Boolean {
        repeat(3) { i ->
            if (Cfg.recentsClearBoot()) {
                return true
            }
            if (i < 2) {
                Thread.sleep(1000L)
            }
        }
        return false
    }
}
