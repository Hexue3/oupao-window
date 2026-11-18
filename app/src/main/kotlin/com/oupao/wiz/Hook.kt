package com.oupao.wiz

/**
 * 按模式号分发的 hook 回调。对应原 smali 的 com.oupao.wiz.Hook。
 *
 * 背景：手写 smali 时写匿名内部类代价很高，原实现改用 1~22 的整数模式号，
 * 把逻辑集中到 Hooks.dispatch(mode, param) 里 switch 分发。
 *
 * 迁移建议：**新代码请不要再使用本类**，改用 lambda：
 *   XBridge.hookMethod(m) { before { ... } }   // 见 XMH.before / XMH.after
 * 本类仅为 transition 期保留，等 170 余个调用点改造完即可删除。
 */
class Hook(val mode: Int) : XMH() {

    override fun beforeHookedMethod(param: XMP) {
        if (mode in BEFORE_MODES) {
            runCatching { Hooks.dispatch(mode, param) }
                .onFailure { SLog.x("hook.before", it) }
        }
    }

    override fun afterHookedMethod(param: XMP) {
        if (mode in AFTER_MODES) {
            runCatching { Hooks.dispatch(mode, param) }
                .onFailure { SLog.x("hook.after", it) }
        }
    }

    companion object {
        /** 在 before 阶段分发的模式号（原 smali 里的常量集合） */
        private val BEFORE_MODES = setOf(2, 3, 7, 8, 12, 13, 15, 17, 20, 21, 22)

        /** 在 after 阶段分发的模式号 */
        private val AFTER_MODES = setOf(1, 4, 5, 6, 9, 10, 11, 14, 16, 18, 19)
    }
}
