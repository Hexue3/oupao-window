package com.oupao.wiz

import io.github.libxposed.api.XposedInterface

/**
 * Hook 回调基类。
 *
 * 原 smali 里为了避开手写匿名内部类，用 Hook(mode) 整数魔法号 + Hooks.dispatch 集中 switch 分发。
 * 迁移到 Kotlin 后推荐直接用 lambda（见 [XBridge.hook]），魔法号方案仅作兼容保留。
 */
open class XMH {

    open fun beforeHookedMethod(param: XMP) = Unit

    open fun afterHookedMethod(param: XMP) = Unit

    companion object {

        /** 只关心 before 阶段 */
        @JvmStatic
        fun before(block: (XMP) -> Unit): XMH = object : XMH() {
            override fun beforeHookedMethod(param: XMP) = block(param)
        }

        /** 只关心 after 阶段 */
        @JvmStatic
        fun after(block: (XMP) -> Unit): XMH = object : XMH() {
            override fun afterHookedMethod(param: XMP) = block(param)
        }

        /** 两阶段都关心 */
        @JvmStatic
        fun around(
            before: ((XMP) -> Unit)? = null,
            after: ((XMP) -> Unit)? = null
        ): XMH = object : XMH() {
            override fun beforeHookedMethod(param: XMP) { before?.invoke(param) }
            override fun afterHookedMethod(param: XMP) { after?.invoke(param) }
        }

        /**
         * 直接替换原方法（不调用原实现）。
         * 注意：返回值仍需符合原方法签名，基本类型返回值请返回对应包装类。
         */
        @JvmStatic
        fun replace(block: (XMP) -> Any?): XMH = object : XMH() {
            override fun beforeHookedMethod(param: XMP) {
                param.result = block(param)
                param.returnEarly = true
            }
        }
    }
}

/**
 * 函数式 hook：直接对 Executable 安装一个 lambda 回调。
 * 这是 Kotlin 版推荐的写法，替代 Hook(1..22) 魔法号。
 */
fun java.lang.reflect.Executable.hook(
    bridge: XposedInterface? = XBridge.xp,
    block: XMP.() -> Unit
): XUnhook? = XBridge.hookMethod(this, object : XMH() {
    override fun beforeHookedMethod(param: XMP) = param.block()
})
