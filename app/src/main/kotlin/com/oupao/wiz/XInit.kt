package com.oupao.wiz

import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.util.concurrent.ConcurrentHashMap

/**
 * 模块入口。对应原 smali 的 com.oupao.wiz.XInit。
 * 注册于 META-INF/xposed/java_init.list。
 *
 * 注意：用 libxposed API 102 的**无参构造**（已由原 dex 字节码
 * `invoke-direct XposedModule;-><init>()V` 确认），不要写成
 * (XposedInterface, ModuleLoadedParam) 形式。
 */
class XInit : XposedModule() {

    companion object {

        /** 已分发过的包名，避免同一进程内重复安装 hook */
        private val fired = ConcurrentHashMap.newKeySet<String>()

        @Volatile
        private var started = false

        /**
         * 统一分发入口：包名 + ClassLoader → Module.handleLoadPackage。
         * launcher 例外，允许重复（原实现行为）。
         */
        /**
         * 1820fix: 去重键必须带进程名。
         * 原来只用 pkg 做键，而 LSPosed 会在**每个**进程回调 scope 里的 "android" 包，
         * 于是 systemui 进程先占了这个键，system_server 进程再进来就被判成"重复"直接跳过
         * → system_server 里一个钩子都没装 → 模块表现为"完全不生效"。
         */
        @JvmStatic
        fun dispatch(pkg: String, loader: ClassLoader?, proc: String = ""): Boolean {
            val key = pkg + "@" + proc
            if (fired.add(key) || "com.android.launcher" == pkg) {
                val lp = LPP(pkg, loader, proc)
                Module().handleLoadPackage(lp)
                return true
            } else {
                SLog.v("XInit: dup dispatch skipped " + key)
                return false
            }
        }

        @JvmStatic
        fun logPhase(pkg: String, loader: ClassLoader?) {
            runCatching {
                SLog.i("XInit: pkg=$pkg cl=${if (loader == null) "null" else "ok"}")
            }
        }
    }

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        raw("onModuleLoaded proc=" + runCatching { param.processName }.getOrDefault("?"))
        runCatching {
            XBridge.attach(this)
            SLog.i("XInit: onModuleLoaded proc=${param.processName}")
            // 3145：配置权威源 —— LSPosed 远程 SharedPreferences（读 App 那份 oupao_cfg.xml）
            Cfg.attachRemote(this)
        }
    }

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        raw("onPackageLoaded pkg=" + runCatching { param.packageName }.getOrDefault("?") +
            " proc=" + currentProcessName())
        val pkg: String
        val loader: ClassLoader?
        try {
            XBridge.attach(this)
            // 3145：有些框架版本不回调 onModuleLoaded，这里补一次（幂等）
            Cfg.attachRemote(this)
            pkg = param.packageName
            loader = param.defaultClassLoader
            logPhase(pkg, loader)
        } catch (_: Throwable) {
            return
        }
        if (loader != null) {
            // 3146：模块**自己**也在作用域里（LSPosed 靠这个把 service binder / 远程 prefs
            // 送进 App 进程）。这里只接配置，绝不能把功能钩子装进自己：
            // 那些 hook 会拦自己的 AMS/窗口调用，UI 直接崩。
            if (pkg == "com.oupao.wiz") {
                Cfg.attachRemote(this)
                return
            }
            dispatch(pkg, loader, currentProcessName())
        }
    }

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        val pkg: String
        val loader: ClassLoader?
        try {
            XBridge.attach(this)
            pkg = param.packageName
            loader = param.classLoader
            logPhase(pkg, loader)
        } catch (_: Throwable) {
            return
        }
        if (loader != null) {
            if (pkg == "com.oupao.wiz") {
                Cfg.attachRemote(this)
                return
            }
            dispatch(pkg, loader, currentProcessName())
        }
    }

    override fun onSystemServerStarting(param: XposedModuleInterface.SystemServerStartingParam) {
        raw("onSystemServerStarting")
        if (started) return
        started = true
        runCatching {
            SLog.i("XInit: onSystemServerStarting")
            XBridge.attach(this)
            // 3145：system_server 侧同样接上远程 prefs（onModuleLoaded 若已接过，这里直接返回）
            Cfg.attachRemote(this)
            // 1820fix: system_server 必须无条件装 —— 先把 "android" 的去重标记清掉，
            // 否则同进程里 onPackageLoaded 先回调过 android 时会被判成重复而跳过。
            fired.remove("android@")
            dispatch("android", param.classLoader, "system")
        }.onFailure { SLog.e("oupao: XInit system_server 异常", it) }
    }

    /**
     * 裸日志：只依赖 android.util.Log，不读配置、不过 SLog 的级别/去重门控。
     * 排查「模块到底有没有被 LSPosed 调起来」时只用这个 tag：`adb logcat -s OupaoBoot`
     */
        private fun raw(msg: String) {
        try {
            android.util.Log.i("OupaoBoot", msg)
        } catch (_: Throwable) {
        }
    }

    /** 取当前进程名；取不到就返回空串（上层会按包名兜底判断） */
    private fun currentProcessName(): String =
        runCatching {
            java.io.File("/proc/self/cmdline").readText().trim('\u0000', '\n', ' ')
        }.getOrDefault("")
}
