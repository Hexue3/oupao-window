package com.oupao.wiz

/**
 * Hook 回调参数。
 *
 * 对应原 smali 中的 com.oupao.wiz.XMP。
 * Kotlin 化改进：args 改为可空数组（原版 Object[] 无法表达"参数被置空"），
 * 并提供类型安全的取值辅助，避免在 175 个调用点里写 (Integer) param.args[2] 这类裸强转。
 */
class XMP {

    /** 方法实参。修改后需调用 [XposedInterface.Chain] 的 proceed(args) 才会生效 */
    @JvmField
    var args: Array<Any?> = emptyArray()

    /** 方法返回值 */
    @JvmField
    var result: Any? = null

    /** 为 true 时跳过原方法调用，直接返回 [result] */
    @JvmField
    var returnEarly: Boolean = false

    /** 被调用对象；静态方法为 null */
    @JvmField
    var thisObject: Any? = null

    /** 拦截返回值（等价于老式 Xposed 的 param.setResult） */
    fun setResult(value: Any?) {
        result = value
        returnEarly = true
    }

    // ---- 类型安全的参数取值 ----

    fun argOrNull(index: Int): Any? = args.getOrNull(index)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> arg(index: Int, type: Class<T>): T? =
        args.getOrNull(index)?.takeIf(type::isInstance) as T?

    inline fun <reified T : Any> arg(index: Int): T? = argOrNull(index) as? T

    fun argInt(index: Int, default: Int = 0): Int = (argOrNull(index) as? Number)?.toInt() ?: default

    fun argLong(index: Int, default: Long = 0L): Long = (argOrNull(index) as? Number)?.toLong() ?: default

    fun argFloat(index: Int, default: Float = 0f): Float = (argOrNull(index) as? Number)?.toFloat() ?: default

    fun argDouble(index: Int, default: Double = 0.0): Double = (argOrNull(index) as? Number)?.toDouble() ?: default

    fun argBool(index: Int, default: Boolean = false): Boolean = (argOrNull(index) as? Boolean) ?: default

    fun argString(index: Int): String? = argOrNull(index) as? String

    /** 原地修改某个参数（在 before 回调里改参数后，XBridge 会用新参数 proceed） */
    fun setArg(index: Int, value: Any?) {
        if (index in args.indices) args[index] = value
    }
}
