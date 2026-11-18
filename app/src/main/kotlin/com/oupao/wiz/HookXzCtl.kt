package com.oupao.wiz

import android.content.Intent

/**
 * 更早的接管点：`ActivityStartController.startActivityInPackage`。
 *
 * ## 为什么要有它（微信为什么慢）
 *
 * 微信「用浏览器打开」不进 ATMS 的任何公开入口，实测只有两处能看到它：
 *  - `ActivityStartController.startActivityInPackage`（能拿到真实 Intent + callingPackage）
 *  - `ActivityStarter.startActivityInner`（只剩 ActivityRecord）
 *
 * 之前只在**后者**接管，那是启动流水线的很后段：微信自己的转场/状态已经推进，
 * 我们再取消 + 重新发小窗，就得等它那套收尾 —— 用户实测「微信打开很慢」。
 *
 * 改到 `startActivityInPackage` 接管后，取消发生在真正排布 task 之前，
 * 与 QQ 走 ATMS 那条路的时机接近，延迟应明显下降。
 *
 * ## 判据不变
 *
 * 与 [CrossZoom.shouldTake] 完全一致（跨应用、非桌面、非同应用、开关打开、
 * 非选择器、非带结果）。`fire()` 失败就**不取消**，原启动照常（全屏），不会打不开。
 */
class HookXzCtl(
    private val intentIdx: Int = -1,
    private val callerIdx: Int = -1,
    private val userIdx: Int = -1,
    private val resultIdx: Int = -1
) : XMH() {

    override fun beforeHookedMethod(param: XMP) {
        runCatching {
            if (CrossZoom.inZoomNow()) return@runCatching
            if (!Cfg.crossZoom()) return@runCatching

            val it = (param.args.getOrNull(intentIdx) as? Intent)
                ?: param.args.firstOrNull { a -> a is Intent } as? Intent
                ?: return@runCatching

            val caller = (param.args.getOrNull(callerIdx) as? String) ?: callerOf(param)
                ?: return@runCatching

            val target = CrossZoom.targetPkg(it) ?: CrossZoom.resolveTarget(ctxOf(param), it)
            val hasResult = resultIdx >= 0 && param.args.getOrNull(resultIdx) != null

            CrossZoom.diagRes("Ctl", caller, target, hasResult, param.args, resultIdx)
            // 2979：选择器类启动不接管
            if (CrossZoom.chooserJustSeen()) return@runCatching
            val take = CrossZoom.shouldTake(it, caller, target, hasResult)
            if (CrossZoom.diagOn() && target != null) {
                SLog.r(
                    "XZCtl",
                    "XZCtl: t=${android.os.SystemClock.uptimeMillis()} $caller -> $target" +
                        " take=$take act=${it.action} res=$hasResult",
                    300L
                )
            }
            if (!take) return@runCatching

            val uid = if (userIdx >= 0) (param.args.getOrNull(userIdx) as? Int) ?: 0 else 0
            if (CrossZoom.fire(it, uid, it.javaClass.classLoader, null, "Ctl")) {
                param.result = 0
                param.returnEarly = true
            }
        }
    }

    private fun callerOf(param: XMP): String? {
        for (a in param.args) {
            if (a is String && a.contains('.') && !a.startsWith(".")) return a
        }
        return null
    }

    private fun ctxOf(param: XMP): android.content.Context? {
        (Rt.get(param.thisObject, "mContext") as? android.content.Context)?.let { return it }
        (Rt.get(param.thisObject, "mAtm")?.let { Rt.get(it, "mContext") } as? android.content.Context)
            ?.let { return it }
        return runCatching { Cfg.sysContext() }.getOrNull()
    }
}
