package com.oupao.wiz

import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Executable
import java.lang.reflect.Member
import java.lang.reflect.Method

/**
 * libxposed 适配层。对应原 smali 的 com.oupao.wiz.XBridge + XBridge$1。
 *
 * 职责：把 libxposed 的**拦截器链模型**（Hooker.intercept(Chain)）
 * 适配成老式 Xposed 的**两阶段模型**（before / after），
 * 这样其余 170 多个类可以沿用 XMH/XMP 写法，不必整体改造成 Chain 风格。
 *
 * Kotlin 化改进：
 *  1. defFor 从 50 行 if-else 链 → when 表达式（原 smali 里这段最易写错）
 *  2. XBridge$1 匿名内部类 → SAM lambda，少一个 class 文件
 *  3. hookMethod 返回真正的 [XUnhook]（原实现返回的是空壳，unhook 从未生效）
 */
object XBridge {

    /**
     * 由 XInit 在 onModuleLoaded / onPackageLoaded 等回调里注入。
     *
     * 原 smali 里是 `.field public static xp:Lio/github/libxposed/api/XposedInterface;`，
     * 供 Java 侧（Hooks/Diag 等）直接读取，所以必须是公开字段而不是私有 setter。
     */
    @Volatile
    @JvmField
    var xp: XposedInterface? = null

    /** 已注册 hook 计数（诊断用） */
    @JvmStatic
    var hookCount = 0
        private set

    @JvmStatic
    fun attach(iface: XposedInterface) {
        xp = iface
    }

    // ---------- hook 入口 ----------

    @JvmStatic
    fun hookMethod(member: Member?, cb: XMH): XUnhook? {
        if (member == null) return null
        reg(member, cb)
        val exec = member as? Executable ?: return null
        val iface = xp ?: return null
        return runCatching {
            val def = defaultValueFor(exec)
            val handle = iface.hook(exec).intercept { chain -> intercept(chain, cb, def) }
            XUnhook(handle)
        }.onFailure { log(it) }.getOrNull()
    }

    /** 与 hookMethod 同义，保留以对齐原 API */
    @JvmStatic
    fun hookRaw(member: Member?, cb: XMH): XUnhook? = hookMethod(member, cb)

    @JvmStatic
    fun hookAllConstructors(clazz: Class<*>?, cb: XMH): Set<XUnhook> {
        val out = HashSet<XUnhook>()
        if (clazz == null) return out
        runCatching {
            clazz.declaredConstructors.forEach { ctor ->
                hookMethod(ctor, cb)?.let(out::add)
            }
        }
        return out
    }

    // ---------- 拦截器：before → proceed → after ----------

    private fun intercept(chain: XposedInterface.Chain, cb: XMH, def: Any?): Any? {
        val p = XMP().apply {
            thisObject = chain.thisObject
            args = chain.args.toTypedArray()
        }

        // before；异常时仍要让原方法执行，不能吞掉调用
        try {
            cb.beforeHookedMethod(p)
        } catch (t: Throwable) {
            // 1820fix: before 自身出错时仍要正常调用原方法，但**不要**把原方法的异常吞成 null
            SLog.e("oupao: hook before 异常", t)
            return chain.proceed() ?: def
        }

        // 未要求提前返回则调用原实现（用可能被 before 改过的参数）
        if (!p.returnEarly) {
            // 1820fix: 原实现的异常要原样抛回业务代码（smali 就是这么做的）。
            // 用 runCatching 吞成 null 会让 system_server 里被 hook 的方法状态不一致。
            p.result = chain.proceed(p.args)
        }

        // after 异常不影响返回值
        runCatching { cb.afterHookedMethod(p) }

        return p.result ?: def
    }

    /**
     * 基本类型返回值在 null 时的兜底值。
     * 因为 intercept 返回 Object，void/构造方法返回 null，
     * 而基本类型（boolean/int/...）返回 null 会导致框架侧 NPE，故需给默认值。
     */
    private fun defaultValueFor(exec: Executable): Any? {
        val rt = (exec as? Method)?.returnType ?: return null
        return when (rt) {
            java.lang.Boolean.TYPE -> false
            java.lang.Integer.TYPE -> 0
            java.lang.Byte.TYPE -> 0.toByte()
            java.lang.Short.TYPE -> 0.toShort()
            java.lang.Long.TYPE -> 0L
            java.lang.Float.TYPE -> 0f
            java.lang.Double.TYPE -> 0.0
            java.lang.Character.TYPE -> 0.toChar()
            else -> null
        }
    }

    // ---------- 注册日志 ----------

    /** 记录 hook 注册（模式号越界会额外提示，对应原 reg() 的校验） */
    private fun reg(member: Member, cb: XMH) {
        runCatching {
            hookCount++
            val sb = StringBuilder("Hook注册 ")
                .append(member.declaringClass?.simpleName)
                .append(".")
                .append(member.name)
            if (member is Method) sb.append("(").append(member.parameterTypes.size).append(")")

            if (cb is Hook) {
                val mode = cb.mode
                sb.append(" 模式=").append(mode)
                // 有效模式号 1~22，越界说明分发表里漏了分支
                if (mode !in 1..22) sb.append("  <== 模式号越界(有效 1~22)")
            }
            SLog.i(sb.toString())
        }
    }

    // ---------- 日志转发 ----------

    /** 同时写框架日志（LSPosed 管理器可见）和模块日志 */
    @JvmStatic
    fun log(msg: String) {
        fwLog(msg)
        runCatching { SLog.i(msg) }
    }

    @JvmStatic
    fun log(t: Throwable) {
        fwLog("exception ${t}")
        runCatching { SLog.e("exception", t) }
    }

    @JvmStatic
    fun flog(msg: String) = fwLog(msg)

    @JvmStatic
    fun flog(t: Throwable?) {
        runCatching { fwLog("exception ${t?.toString()}") }
    }

    /** 框架日志：优先级 3 = Log.WARN，tag "oupao" */
    private fun fwLog(msg: String) {
        runCatching { xp?.log(3, "oupao", msg) }
    }
}
