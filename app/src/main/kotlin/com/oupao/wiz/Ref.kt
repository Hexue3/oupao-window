package com.oupao.wiz

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * 反射工具（带缓存）。对应原 smali 的 com.oupao.wiz.Ref。
 *
 * 这是整个模块调用 SystemUI / ColorOS 私有 API 的地基。
 * hook 回调里高频走这些路径，所以做了三层缓存：
 *   mc   —— 方法缓存，key = "类名#方法名#参数个数"
 *   fc   —— 字段缓存，key = "类名#字段名"
 *   sbTl —— 每线程复用 StringBuilder，避免拼 key 时反复分配
 *
 * 所有方法都吞异常返回 null，绝不向上冒泡 ——
 * SystemUI 各版本类名/方法名差异极大，找不到是常态而不是错误。
 */
object Ref {

    private val mc = ConcurrentHashMap<String, Method>()
    private val fc = ConcurrentHashMap<String, Field>()
    private val sbTl = ThreadLocal<StringBuilder>()

    // ---------- 类加载 ----------

    /** Class.forName(name, false, loader)，失败返回 null */
    @JvmStatic
    fun cls(loader: ClassLoader?, name: String?): Class<*>? {
        if (name == null) return null
        return runCatching { Class.forName(name, false, loader) }.getOrNull()
    }

    // ---------- 方法查找 ----------

    /**
     * 按「方法名 + 参数个数」查找（不校验参数类型）。
     * 沿继承链向上找，命中后写入缓存。
     */
    @JvmStatic
    fun methodByName(cls: Class<*>?, name: String?, argc: Int): Method? {
        if (name == null) return null
        var key = ""
        if (cls != null) {
            key = "${cls.name}#$name#$argc"
            mc[key]?.let { return it }
        }
        var c = cls
        while (c != null) {
            val methods = runCatching { c.declaredMethods }.getOrNull() ?: break
            for (m in methods) {
                val ok = runCatching { m.name == name && m.parameterTypes.size == argc }
                    .getOrDefault(false)
                if (ok) {
                    if (cls != null) mc[key] = m
                    return m
                }
            }
            c = c.superclass
        }
        return null
    }

    /**
     * 精确查找：先 getDeclaredMethod，再 getMethod，最后沿父类递归。
     * 与 [methodByName] 的区别是校验参数类型。
     */
    @JvmStatic
    fun method(cls: Class<*>?, name: String, params: Array<Class<*>>?): Method? {
        if (cls == null) return null
        val p = params ?: emptyArray()
        runCatching { cls.getDeclaredMethod(name, *p) }.getOrNull()?.let { return it }
        runCatching { cls.getMethod(name, *p) }.getOrNull()?.let { return it }
        val sup = cls.superclass ?: return null
        return method(sup, name, params)
    }

    // ---------- 字段读取 ----------

    /** 读取对象字段（含父类私有字段），带缓存 */
    @JvmStatic
    fun get(target: Any?, name: String?): Any? {
        if (target == null || name == null) return null
        val cls = target.javaClass
        val key = "${cls.name}#$name"

        var f = fc[key]
        if (f == null) {
            var c: Class<*>? = cls
            while (c != null) {
                f = runCatching {
                    c!!.getDeclaredField(name).apply { isAccessible = true }
                }.getOrNull()
                if (f != null) {
                    fc[key] = f
                    break
                }
                c = c.superclass
            }
        }
        return runCatching { f?.get(target) }.getOrNull()
    }

    // ---------- 调用 ----------

    /** 调用方法，失败打日志（用于需要排查的调用点） */
    @JvmStatic
    fun invoke(m: Method?, target: Any?, args: Array<Any?>?): Any? {
        if (m == null) return null
        return runCatching {
            m.isAccessible = true
            m.invoke(target, *(args ?: emptyArray()))
        }.onFailure { SLog.e("Ref: invoke FAILED ${m.name}", it) }.getOrNull()
    }

    /** 静默版 invoke：失败不打日志（用于高频探测） */
    @JvmStatic
    fun invokeQ(m: Method?, target: Any?, args: Array<Any?>?): Any? {
        if (m == null) return null
        return runCatching {
            m.isAccessible = true
            m.invoke(target, *(args ?: emptyArray()))
        }.getOrNull()
    }

    /** 按名字调用目标对象上的方法（按参数个数自动匹配） */
    @JvmStatic
    fun callLoose(target: Any?, name: String?, args: Array<Any?>?): Any? {
        if (target == null) return null
        val m = methodByName(target.javaClass, name, args?.size ?: 0)
        return invoke(m, target, args)
    }

    /** 调用无参方法 */
    @JvmStatic
    fun callNoArg(target: Any?, name: String?): Any? {
        if (target == null) return null
        val m = methodByName(target.javaClass, name, 0) ?: return null
        return invoke(m, target, emptyArray())
    }

    /** 静默版 callNoArg */
    @JvmStatic
    fun callNoArgQ(target: Any?, name: String?): Any? {
        if (target == null) return null
        val m = methodByName(target.javaClass, name, 0) ?: return null
        return invokeQ(m, target, emptyArray())
    }

    // ---------- 构造 ----------

    /** 按参数个数挑构造器实例化 */
    @JvmStatic
    fun newInstance(cls: Class<*>?, args: Array<Any?>?): Any? {
        if (cls == null) return null
        val want = args?.size ?: 0
        val ctors: Array<Constructor<*>> =
            runCatching { cls.declaredConstructors }.getOrNull() ?: return null
        for (c in ctors) {
            if (c.parameterTypes.size != want) continue
            val r = runCatching {
                c.isAccessible = true
                c.newInstance(*(args ?: emptyArray()))
            }
            if (r.isSuccess) return r.getOrNull()
        }
        return null
    }

    // ---------- 内部 ----------

    private fun sb(): StringBuilder {
        return runCatching {
            val s = sbTl.get() ?: StringBuilder().also { sbTl.set(it) }
            s.setLength(0)
            s
        }.getOrDefault(StringBuilder())
    }

    /** 诊断：当前缓存了多少方法/字段 */
    @JvmStatic
    fun stat(): String = "Ref: mc=${mc.size} fc=${fc.size}"
}
