package com.oupao.wiz

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 3520：桌面进程静默清空多任务（唯一路径，没有 UI 兜底）。
 *
 * system_server 在开机完成 + user 0 解锁 + 开关打开时写
 * `sys.oupao.recents.clear_at`（见 [RecentsClearBoot]）。
 *
 * 这里 hook `com.android.launcher.Launcher.onCreate/onResume`，起后台线程：
 *  - 等时间戳出现；
 *  - **立即开始尝试** `ActivityTaskManager.getService().removeAllVisibleRecentTasks()`，
 *    失败每秒重试（vendor controller 未初始化时会 NPE，一就绪就成功）；
 *  - 成功后写 `cleared_at`，不再重复。
 *
 * 这条路就是桌面原生 `ActivityManagerWrapper.removeAllRecentTasks()` 的 binder 调用，
 * launcher 有 REMOVE_TASKS 权限，且进程已初始化；不需要打开多任务界面。
 * 3519 实测 +20s 能成功；3520 改成从时间戳出现就开始重试，尽量提前。
 */
internal object RecentsBootClearLauncher {

    private const val P_CLEAR_AT = "sys.oupao.recents.clear_at"
    private const val SP = "oupao_recents_boot"
    private const val KEY_AT = "cleared_at"
    /** 时间戳后 10 分钟内处理，避免很久以后 launcher 重启又清一次 */
    private const val WINDOW_MS = 600_000L

    private val silentStarted = AtomicBoolean(false)

    @Volatile
    private var installed = false

    fun install(cl: ClassLoader?): Boolean {
        if (cl == null || installed) {
            return false
        }
        var hooked = 0
        val launcherCls = runCatching {
            Rt.cls(cl, "com.android.launcher.Launcher")
        }.getOrNull()
        if (launcherCls != null) {
            val resume = Rt.method(launcherCls, "onResume", emptyArray())
            if (resume != null && XBridge.hookMethod(resume, object : XMH() {
                    override fun afterHookedMethod(param: XMP) {
                        val a = param.thisObject as? android.app.Activity ?: return
                        startSilent(a.applicationContext)
                    }
                }) != null) {
                hooked++
            }
            val create = Rt.method(launcherCls, "onCreate",
                arrayOf<Class<*>>(android.os.Bundle::class.java))
            if (create != null && XBridge.hookMethod(create, object : XMH() {
                    override fun afterHookedMethod(param: XMP) {
                        val a = param.thisObject as? android.app.Activity ?: return
                        startSilent(a.applicationContext)
                    }
                }) != null) {
                hooked++
            }
        }
        installed = hooked > 0
        log("install hooked=" + hooked + " launcher=" + (launcherCls != null))
        return installed
    }

    private fun startSilent(ctx: android.content.Context) {
        if (!silentStarted.compareAndSet(false, true)) {
            return
        }
        val t = Thread({ silentLoop(ctx) }, "Oupao-RecentsSilent")
        t.isDaemon = true
        t.start()
    }

    private fun silentLoop(ctx: android.content.Context) {
        try {
            // 等 system_server 写完时间戳（解锁后才写）
            var at = 0L
            for (i in 0 until 90) {
                at = clearAt()
                if (at > 0L) {
                    break
                }
                Thread.sleep(500L)
            }
            if (at <= 0L) {
                log("silent: 等不到 clear_at，放弃")
                return
            }
            val now = SystemClock.elapsedRealtime()
            if (now < at || (now - at) > WINDOW_MS) {
                log("silent: 时间戳不在窗口 at=" + at + " now=" + now)
                return
            }
            val sp = runCatching {
                ctx.getSharedPreferences(SP, android.content.Context.MODE_PRIVATE)
            }.getOrNull()
            if (sp != null && sp.getLong(KEY_AT, 0L) == at) {
                log("silent: 已处理过 at=" + at)
                return
            }

            var failCount = 0
            for (i in 0 until 90) {
                if (callRemoveAllRecentTasks()) {
                    runCatching { sp?.edit()?.putLong(KEY_AT, at)?.apply() }
                    val cost = SystemClock.elapsedRealtime() - at
                    log("silent: removeAllVisibleRecentTasks 成功 at=" + at +
                        " 用时=" + cost + "ms 重试=" + failCount)
                    return
                }
                failCount++
                // 失败日志只打第一次，避免刷屏
                Thread.sleep(1000L)
            }
            log("silent: removeAllVisibleRecentTasks 90 次都失败")
        } catch (t: Throwable) {
            log("silent: 异常 " + t)
        }
    }

    private fun callRemoveAllRecentTasks(): Boolean {
        return try {
            val cls = Class.forName("android.app.ActivityTaskManager")
            val get = cls.getDeclaredMethod("getService")
            get.isAccessible = true
            val svc = get.invoke(null) ?: return false
            val m = Rt.methodByName(svc.javaClass, "removeAllVisibleRecentTasks", 0)
                ?: return false
            m.isAccessible = true
            try {
                m.invoke(svc)
                true
            } catch (e: java.lang.reflect.InvocationTargetException) {
                val cause = e.cause ?: e
                // 只记第一次，后面同一原因重试不打
                if (!firstFailLogged) {
                    firstFailLogged = true
                    log("silent: 首次失败（会继续重试）: " + cause)
                }
                false
            }
        } catch (t: Throwable) {
            if (!firstFailLogged) {
                firstFailLogged = true
                log("silent: 首次调用失败（会继续重试）: " + t)
            }
            false
        }
    }

    @Volatile
    private var firstFailLogged = false

    private fun clearAt(): Long = runCatching {
        FlyState.raw(P_CLEAR_AT).trim().toLong()
    }.getOrDefault(0L)

    private fun log(msg: String) {
        runCatching { XBridge.flog("RecentsBootClear: " + msg) }
    }
}
