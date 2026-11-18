package com.oupao.wiz

/**
 * 移除应用启动遮罩 —— **跳过启动窗**（starting window）。
 *
 * ## 结论（踩过的坑，别再重复）
 * 用户要的是「点下去立刻有反馈 + 没有遮罩」，但在这台 ColorOS 16 上做不到，原因：
 *
 * 1. `ActivityOptions` 写 `android.activity.splashScreenStyle = 0`：日志证明写入成功，
 *    界面**无任何变化** → ColorOS 不认这个 Bundle 键。
 * 2. 启动窗入口 `ActivityRecord.showStartingWindow` 的签名是
 *    `(ActivityRecord, boolean, boolean, boolean, ActivityRecord)` —— **参数里没有
 *    StartingWindowInfo**，拿不到可改的样式/内容。
 * 3. ColorOS 的真实实现是 `com.android.server.wm.OplusStartingWindowManager` +
 *    `OplusAppQuickStartingSurface`（含 `StartingSurfaceParams$Builder`、
 *    `IPreviewBitmapLoadListener`）。
 *    挂上 20 个「含 preview/bitmap/snapshot 的方法」、把 Bitmap 参数抹成 null
 *    （日志：`去掉预览位图 writeSurfaceContent`）后，**遮罩依旧显示** ——
 *    说明内容是 SurfaceControl 层直接画的，不走这些参数。
 *
 * 所以只能二选一：
 *   - 关开关：原样（有预览遮罩，点击立刻有反馈）
 *   - 开开关：无遮罩（启动瞬间没有过渡画面，慢应用会像「卡一下」）
 *
 * 本文件实现「开」的那一侧：直接跳过启动窗创建。
 * 只跳过启动窗，不动启动本身；异常全部吞掉。
 */
class HookNoSplash(private val entry: String = "?") : XMH() {

    override fun beforeHookedMethod(param: XMP) {
        runCatching {
            if (!Cfg.noSplash()) return@runCatching
            param.result = null
            param.returnEarly = true
            SLog.r("NSplash", "NSplash: 跳过启动窗 $entry", 1500L)
        }
    }

    companion object {
        /**
         * 只挂名字正好是 `showStartingWindow` 的方法（各重载），
         * 排除 `lambda$showStartingWindow$16` 这类合成方法。
         */
        @JvmStatic
        fun install(cl: ClassLoader?): Int {
            var n = 0
            runCatching {
                val c = Rt.cls(cl, "com.android.server.wm.ActivityRecord") ?: return 0
                for (m in c.declaredMethods) {
                    // ⚠️ 必须用「名字含 showStartingWindow」而不是精确匹配：
                    //    实测真正触发启动窗的是 `lambda$showStartingWindow$16/$17`
                    //    这类合成方法（2313 能去掉遮罩正是因为它俩也被拦了；
                    //    2318 收窄成精确匹配后遮罩又回来了）。
                    if (!m.name.contains("showStartingWindow") &&
                        !m.name.contains("addStartingWindow")
                    ) continue
                    if (XBridge.hookMethod(m, HookNoSplash(m.name)) != null) {
                        n++
                        SLog.i(
                            "NSplash: 已挂 ActivityRecord." + m.name + "(" +
                                m.parameterTypes.joinToString(",") { it.simpleName } + ")"
                        )
                    }
                }
            }.onFailure { SLog.e("NSplash: 安装失败", it) }
            if (n == 0) SLog.i("NSplash: 未找到启动窗入口，移除遮罩不可用")
            // 开机自检：把「开关值 / 总开关」都打出来，便于确认到底是哪一层没开
            runCatching {
                SLog.i(
                    "NSplash: 自检 钩子数=" + n +
                        " 开关=" + Cfg.noSplash() +
                        " 原始值=" + Cfg.getInt(Cfg.K_NO_SPLASH, -1) +
                        " 总开关=" + Cfg.masterOn()
                )
            }
            return n
        }
    }
}
