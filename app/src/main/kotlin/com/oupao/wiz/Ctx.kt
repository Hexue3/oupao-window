package com.oupao.wiz

import android.content.Context

/**
 * 系统 Context 获取。对应原 smali 的 com.oupao.wiz.Ctx。
 *
 * 在模块自身进程里拿不到目标进程的 Context，因此通过反射
 * ActivityThread.currentActivityThread().getSystemContext() 取系统 Context。
 * 结果缓存，失败返回 null（调用方需自行判空）。
 */
object Ctx {

    @Volatile
    private var sCtx: Context? = null

    /** getSystemUiContext() 的缓存，见 [sysUi] */
    @Volatile
    private var uiCtx: Context? = null

    @JvmStatic
    fun get(): Context? {
        sCtx?.let { return it }
        runCatching {
            val atClass = Class.forName("android.app.ActivityThread")
            val current = atClass.getDeclaredMethod("currentActivityThread")
            current.isAccessible = true
            val at = current.invoke(null) ?: return@runCatching
            val sys = at.javaClass.getMethod("getSystemContext").invoke(at)
            if (sys is Context) sCtx = sys
        }
        return sCtx
    }

    /**
     * SystemUI 自己的 Context（包名 `com.android.systemui`）。
     *
     * 为什么要单独一个：`getSystemContext()` 拿到的 Context 包名是 **"android"**，
     * 在 SystemUI 进程（uid 10249）里用它访问 Settings / ContentProvider 会被拒：
     *   `SecurityException: Package android does not belong to 10249`
     * 实测这条路把 `Settings.Global` 的读取整个废掉（读回来恒为 null）。
     *
     * 取法按可靠性排序：
     *  1) `ActivityThread.getSystemUiContext()` —— 系统自己给 SystemUI 准备的 Context；
     *  2) 退回 `createPackageContext("com.android.systemui", 0)` —— 公开 API，
     *     拿到的 Context 包名与进程 uid 一致，归属校验能过。
     */
    @JvmStatic
    fun sysUi(): Context? {
        uiCtx?.let { return it }
        // 1) getSystemUiContext()
        runCatching {
            val atClass = Class.forName("android.app.ActivityThread")
            val current = atClass.getDeclaredMethod("currentActivityThread")
            current.isAccessible = true
            val at = current.invoke(null) ?: return@runCatching
            val c = at.javaClass.getMethod("getSystemUiContext").invoke(at)
            if (c is Context && c.packageName != "android") uiCtx = c
        }
        uiCtx?.let { return it }
        // 2) createPackageContext
        runCatching {
            val base = get() ?: return@runCatching
            val c = base.createPackageContext("com.android.systemui", 0)
            if (c != null) uiCtx = c
        }
        return uiCtx
    }

    /** dp → px；取不到 Context 时原值返回 */
    @JvmStatic
    fun dp(value: Int): Int {
        return runCatching {
            val c = get() ?: return value
            (value * c.resources.displayMetrics.density + 0.5f).toInt()
        }.getOrDefault(value)
    }

    @JvmStatic
    fun clear() {
        sCtx = null
        uiCtx = null
    }
}
