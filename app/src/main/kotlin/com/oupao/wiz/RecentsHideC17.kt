package com.oupao.wiz

/**
 * C17 专用的「多任务隐藏小窗应用」入口。
 *
 * C16/C17 的差异（2026-11-13，C17 17.5.5 反编译实证）：
 *   - 过滤器类：C16 `com.oplus.quickstep.data.OplusRecentTasksFilter`
 *                C17 17.5.5 `jn7`（日志 tag 仍是 OplusRecentTasksFilter）
 *                17.3.12 `ko.a`
 *   - `GroupedTaskInfo`：17.5.5 是 `c()/d()` 两个 TaskInfo + 字段 `t`(SplitBounds) +
 *     字段 `r`（== 4 表示 mixed/分屏组）；不再有 `d() -> SplitBounds`。
 *
 * 具体扫描/判据复用 [RecentsHide.installC17]（C16 已验证的代码路径不动，
 * 只在 c17=true 时切换候选类与取值方法）。
 */
internal object RecentsHideC17 {

    fun install(cl: ClassLoader?): Boolean {
        val ok = runCatching { RecentsHide.installC17(cl) }.getOrDefault(false)
        SLog.i("RecentsHideC17: hooked=" + ok + "（17.5.5 走 jn7）")
        return ok
    }
}
