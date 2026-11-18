package com.oupao.wiz

import android.content.Intent

/**
 * `ATMS.startActivities(...)`（数组形态）的接管钩子。
 *
 * 为什么单独写：它的形参是 `Intent[]` 而不是单个 Intent，
 * 复用 [HookIdle] 会把数组当成 Intent 处理而失效。
 *
 * 只在**数组里恰好 1 个 Intent** 时接管 —— 多个时取消原调用会丢掉后面的启动，
 * 那种情况宁可保持原样（全屏）。
 */
class HookXzActs(
    private val intentsIdx: Int = -1,
    private val callerIdx: Int = -1,
    private val userIdx: Int = -1,
    private val resultIdx: Int = -1
) : XMH() {

    override fun beforeHookedMethod(param: XMP) {
        runCatching {
            if (!Cfg.crossZoom()) return@runCatching
            val arr = param.args.getOrNull(intentsIdx) as? Array<*> ?: return@runCatching
            if (arr.size != 1) return@runCatching
            val it = arr[0] as? Intent ?: return@runCatching

            val caller = (param.args.getOrNull(callerIdx) as? String)
                ?: return@runCatching
            val hasResult = resultIdx >= 0 && param.args.getOrNull(resultIdx) != null
            val target = CrossZoom.targetPkg(it)
                ?: CrossZoom.resolveTarget(ctxOf(param), it)

            CrossZoom.diagRes("Acts", caller, target, hasResult, param.args, resultIdx)
            // 2979：选择器类启动不接管
            if (CrossZoom.chooserJustSeen()) return@runCatching
            val take = CrossZoom.shouldTake(it, caller, target, hasResult)
            if (CrossZoom.diagOn() && target != null) {
                SLog.r(
                    "XZdiag",
                    "XZdiag[acts]: $caller -> $target take=$take res=$hasResult act=${it.action}",
                    800L
                )
            }
            if (!take) return@runCatching

            val uid = if (userIdx >= 0) (param.args.getOrNull(userIdx) as? Int) ?: 0 else 0
            if ("com.android.systemui" == caller) CrossZoom.collapsePanels()
            if (CrossZoom.fire(it, uid, param.thisObject, "Acts")) {
                param.result = 0
                param.returnEarly = true
            }
        }
    }

    private fun ctxOf(param: XMP): android.content.Context? {
        (Rt.get(param.thisObject, "mContext") as? android.content.Context)?.let { return it }
        return runCatching { Cfg.sysContext() }.getOrNull()
    }
}
