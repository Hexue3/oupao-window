package com.oupao.wiz

import android.content.Intent

/**
 * 诊断用钩子（**只记录、不接管**）：抓「任何一次 Activity 启动」。
 *
 * 为什么需要它：微信「用浏览器打开」实测不进 ATMS 的任何一个 `startActivity*` 入口
 * （`XZdiag`/`XZmiss`/`XZpath` 全无记录，而 QQ 走同一场景却能进）。
 * 所以只能到更下游、更唯一的地方去看 —— `ActivityStarter.startActivityInner`：
 * 那里拿到的是 **ActivityRecord**，其 `launchedFromPackage`（谁发起的）和
 * `packageName`（目标）一定有效，跟走哪个入口无关。
 *
 * 两种入参形态都处理：
 *  - 形参里有 `ActivityRecord`（ActivityStarter 系列）→ 读它的字段；
 *  - 形参里有 `Intent`（ATMS 系列）→ 读 Intent。
 */
class HookXzPath : XMH() {

    override fun beforeHookedMethod(param: XMP) {
        runCatching {
            if (!Cfg.crossZoom()) return@runCatching

            // ── 形态 A：ActivityRecord ──
            val rec = param.args.firstOrNull { it != null && it.javaClass.name.endsWith("ActivityRecord") }
            if (rec != null) {
                val from = Rt.get(rec, "launchedFromPackage") as? String
                val target = (Rt.get(rec, "packageName") as? String)
                    ?: (Rt.get(rec, "mActivityComponent")?.toString())
                val intent = Rt.get(rec, "intent") as? Intent
                if (from.isNullOrEmpty() && target.isNullOrEmpty()) return@runCatching
                // ★ 诊断默认关闭：Thread.getStackTrace 很贵，且 SLog 是写文件，
                // 放在每次启动的热路径上会明显拖慢跳转（用户实测「打开很慢」就是它）。
                if (!CrossZoom.diagOn()) return@runCatching
                SLog.r(
                    "XZrec",
                    "XZrec: from=$from -> $target act=${intent?.action}" +
                        " dat=${intent?.dataString} flags=" +
                        Integer.toHexString(intent?.flags ?: 0),
                    700L
                )
                // 跨应用启动时打一次调用栈（仅诊断）
                if (!from.isNullOrEmpty() && !target.isNullOrEmpty() && from != target &&
                    from != "com.android.launcher" && from != "com.android.systemui"
                ) {
                    SLog.r("XZstk", "XZstk: $from -> $target | " + stack(), 3000L)
                }
                return@runCatching
            }

            // ── 形态 B：Intent ──
            val it = param.args.firstOrNull { it is Intent } as? Intent ?: return@runCatching
            if (!CrossZoom.diagOn()) return@runCatching
            val caller = callerOf(param)
            SLog.r(
                "XZpath",
                "XZpath: caller=$caller act=${it.action} comp=${it.component}" +
                    " pkg=${it.`package`} dat=${it.dataString} flags=" + Integer.toHexString(it.flags),
                700L
            )
        }
    }

    private fun callerOf(param: XMP): String? {
        for (a in param.args) {
            if (a is String && a.contains('.') && !a.startsWith(".")) return a
        }
        return null
    }

    /** 精简调用栈（只保留类名末段+方法名），用来定位入口。取够 30 帧：前 10 帧是本模块/LSPosed 的转发层。 */
    private fun stack(): String = runCatching {
        Thread.currentThread().stackTrace
            .drop(1)
            .take(30)
            .joinToString(" <- ") {
                it.className.substringAfterLast('.') + "." + it.methodName
            }
    }.getOrDefault("-")
}
