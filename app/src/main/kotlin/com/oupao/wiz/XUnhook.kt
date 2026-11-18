package com.oupao.wiz

import io.github.libxposed.api.XposedInterface

/**
 * Hook 句柄。
 *
 * 原 smali 的 XUnhook 是个空壳类 —— 它从未持有 libxposed 的 HookHandle，
 * 因此 unhook() 事实上从未生效（历史遗留 bug）。Kotlin 版在此修复。
 */
class XUnhook(private val handle: XposedInterface.HookHandle? = null) {

    /** 取消 hook。libxposed 的 unhook() 幂等，可重复调用 */
    fun unhook() {
        runCatching { handle?.unhook() }
    }

    /**
     * 用新 hooker 原子替换当前 hook，返回新句柄。
     * 需要 libxposed API 102+（对应 setId/replaceHook）。
     */
    fun replace(hooker: XposedInterface.Hooker): XUnhook? =
        runCatching { handle?.replaceHook(hooker)?.let(::XUnhook) }.getOrNull()

    /** 被 hook 的方法/构造方法 */
    val executable: java.lang.reflect.Executable?
        get() = runCatching { handle?.executable }.getOrNull()

    /** 该 hook 的 id（若设置过） */
    val id: String?
        get() = runCatching { handle?.id }.getOrNull()
}
