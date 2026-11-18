package com.oupao.wiz

import android.content.Context
import java.lang.reflect.Method

/**
 * 反射门面 + 命中统计。对应原 smali 的 com.oupao.wiz.Rt。
 *
 * 多数方法直接转发到 [Ref]，额外记录 hit/miss。
 * 排查 SystemUI 版本差异时，看 miss 计数比翻日志快得多。
 */
object Rt {

    @JvmStatic
    var hit = 0
        private set

    @JvmStatic
    var miss = 0
        private set

    /** 加载类；命中记 hit，失败记 miss */
    @JvmStatic
    fun cls(loader: ClassLoader?, name: String?): Class<*>? {
        val c = Ref.cls(loader, name)
        if (c != null) hit++ else miss++
        return c
    }

    /** 取字段；命中记 hit，失败记 miss */
    @JvmStatic
    fun get(target: Any?, name: String?): Any? {
        val v = Ref.get(target, name)
        if (v != null) hit++ else miss++
        return v
    }

    /** 按名字找方法；命中记 hit，失败记 miss */
    @JvmStatic
    fun methodByName(cls: Class<*>?, name: String?, argc: Int): Method? {
        val m = Ref.methodByName(cls, name, argc)
        if (m != null) hit++ else miss++
        return m
    }

    // ---------- 直接转发（不计数） ----------

    @JvmStatic
    fun method(cls: Class<*>?, name: String, params: Array<Class<*>>?): Method? =
        Ref.method(cls, name, params)

    @JvmStatic
    fun invoke(m: Method?, target: Any?, args: Array<Any?>?): Any? =
        Ref.invoke(m, target, args)

    @JvmStatic
    fun callLoose(target: Any?, name: String?, args: Array<Any?>?): Any? =
        Ref.callLoose(target, name, args)

    @JvmStatic
    fun callNoArg(target: Any?, name: String?): Any? =
        Ref.callNoArg(target, name)

    @JvmStatic
    fun callNoArgQ(target: Any?, name: String?): Any? =
        Ref.callNoArgQ(target, name)

    @JvmStatic
    fun newInstance(cls: Class<*>?, args: Array<Any?>?): Any? =
        Ref.newInstance(cls, args)

    /**
     * 取 Context：依次尝试 Rx.ctxA → ctxB → ctxC。
     * 三个来源对应不同注入时机（hook 到的 Activity / Service / 系统上下文）。
     */
    @JvmStatic
    fun ctx(): Context? = Rx.ctxA() ?: Rx.ctxB() ?: Rx.ctxC()

    /** 诊断串 */
    @JvmStatic
    fun stat(): String = "Rt: hit=$hit miss=$miss"
}
