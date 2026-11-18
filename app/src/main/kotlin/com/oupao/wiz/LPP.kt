package com.oupao.wiz

/**
 * 包加载参数。对应原 smali 的 com.oupao.wiz.LPP。
 */
class LPP {

    @JvmField
    var packageName: String = ""

    @JvmField
    var classLoader: ClassLoader? = null

    /**
     * 真实进程名（android.os.Process.myProcessName()）。
     * 1820fix: LSPosed 对 scope 里的 "android" 包会在**每个**进程回调 onPackageLoaded，
     * 只靠 packageName 判断会把 systemui 误当成 system_server。
     */
    @JvmField
    var processName: String = ""

    constructor()

    constructor(packageName: String, classLoader: ClassLoader?, processName: String = "") {
        this.packageName = packageName
        this.classLoader = classLoader
        this.processName = processName
    }

    /** 按类名加载目标类；失败返回 null（SystemUI hook 大量依赖此行为，不能抛异常冒泡） */
    fun findClass(name: String): Class<*>? =
        runCatching { Class.forName(name, false, classLoader) }.getOrNull()
}
