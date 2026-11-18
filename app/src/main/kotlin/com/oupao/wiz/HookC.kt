package com.oupao.wiz

import java.lang.ClassLoader

/**
 * CPU 超额保活。对应原 smali 的 com.oupao.wiz.HookC。
 *
 * 背景：Android 的 ProcessRecord.killLocked() 会因「excessive cpu」/「bgLimit」
 * 杀掉后台进程。本 hook 拦住它——只要被杀的进程名正是当前保活任务所属包名，
 * 就把返回值置空（等价于 setResult(null)），让这次 kill 无效。
 *
 * 原实现全程包在 catchall 里、异常时静默返回。Kotlin 版保留这个
 * "绝不因为自己而搞崩系统服务"的语义，但用 runCatching 表达得更明确。
 */
class HookC : XMH() {

    override fun beforeHookedMethod(param: XMP) {
        runCatching {
            val reason = param.argOrNull(0) as? String ?: return
            val isCpuKill = reason.contains("excessive cpu") || reason.contains("bgLimit")
            if (!isCpuKill) return

            // 优先用 Dock 记录的包名；没有就从当前 task 反查
            val pkg = Dock.sPkg ?: Win.taskPkg(Hooks.task()).takeIf { it.isNotEmpty() } ?: return

            val self = param.thisObject ?: return
            val procName = Rt.get(self, "processName") as? String ?: return
            if (!procName.startsWith(pkg)) return

            param.setResult(null)
            SLog.k("CpuWatch: 放过保活任务, 不按 CPU 超额杀")
        }
    }

    companion object {

        @JvmStatic
        fun install(cl: ClassLoader?) {
            runCatching {
                val pr = Rt.cls(cl, "com.android.server.am.ProcessRecord") ?: return
                val m = Rt.methodByName(pr, "killLocked", 4) ?: return
                XBridge.hookMethod(m, HookC())
                SLog.i("CpuWatch: ProcessRecord.killLocked 已挂钩")
            }.onFailure {
                // 装不上不影响其他功能，静默跳过（对齐原 smali 行为）
            }
        }
    }
}
