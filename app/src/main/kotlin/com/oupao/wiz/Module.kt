package com.oupao.wiz

/**
 * 包加载分发。对应原 smali 的 com.oupao.wiz.Module。
 *
 * 所有被 hook 的进程最终都汇聚到这里：
 *   Boot.dispatch(lp)  —— 按包名分发到各功能模块的 hook 安装
 *   Web.tryHook(...)   —— 浏览器侧（via / heytap）单独处理
 */
class Module {

    fun handleLoadPackage(lp: LPP) {
        try {
            Boot.dispatch(lp)
            runCatching { Web.tryHook(lp.packageName, lp.classLoader) }
        } catch (t: Throwable) {
            SLog.e("oupao: handleLoadPackage 异常", t)
        }
    }
}
