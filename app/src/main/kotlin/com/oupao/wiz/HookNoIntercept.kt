package com.oupao.wiz

/**
 * 抑制 ColorOS 的「应用跳转确认」弹窗（"即将离开 XX 打开其他应用"）。
 *
 * ## 为什么不用改设置
 *
 * `Settings.Global.app_start_confirm_rus_enable` 改了会被系统改回 1
 * （实测置 0 → 重启后仍是 0 → 过一会儿又变 1）。
 * 因为真正的决策在 system_server 的
 * `com.android.server.am.OplusActivityStartController` 里：
 * 它 `init` 时读一次该设置存到字段 `mEnableActivityStartIntercept`，
 * 再用 ContentObserver 更新；真正决定弹不弹的是
 * **`isInterceptActivityStart(): Z`**。
 *
 * 所以直接把这个方法按成 `false` —— 不碰任何系统设置，也不会被改回。
 *
 * ## 范围
 *
 * 
 */
class HookNoIntercept : XMH() {

    override fun beforeHookedMethod(param: XMP) {
        runCatching {
            if (!Cfg.crossZoom()) return@runCatching
            param.result = false
            param.returnEarly = true
        }
    }
}
