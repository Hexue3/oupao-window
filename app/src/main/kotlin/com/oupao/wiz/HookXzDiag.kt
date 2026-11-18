package com.oupao.wiz

import android.content.Intent

/**
 * 诊断用钩子：只记录，不接管。
 *
 * 背景：微信「用浏览器打开」实测**完全没进** [HookIdle] 挂的那两个入口
 * （`startActivity` / `startActivityAsUser`），日志里一条 `com.tencent.mm -> mark.via` 都没有。
 * 而 QQ 走的是同一场景却能进 —— 说明两者用的 ATMS 入口不同。
 *
 * 所以把 ATMS 里其它启动入口也挂上，先看清微信到底走哪条、带什么 Intent，
 * 再决定怎么处理。**不在这里做任何接管**，避免影响真实启动。
 */
class HookXzDiag(private val entry: String = "?") : XMH() {

    override fun beforeHookedMethod(param: XMP) {
        runCatching {
            if (!Cfg.crossZoom()) return@runCatching
            val picked = pick(param)
            if (picked == null) {
                // ★ 拿不到 Intent 也要记一笔：用来确认「这个入口确实被走到了」，
                //   比如 PendingIntent / IntentSender 这类把真实 Intent 藏在别处的调用。
                val types = param.args.joinToString(",") { a ->
                    a?.javaClass?.simpleName ?: "null"
                }
                SLog.r("XZpath", "XZpath[$entry]: intent=null args=$types", 1200L)
                return@runCatching
            }
            val it = picked
            val caller = callerOf(param)
            val sb = StringBuilder("XZpath[").append(entry).append("]: caller=").append(caller)
            sb.append(" act=").append(it.action)
            sb.append(" comp=").append(it.component)
            sb.append(" pkg=").append(it.`package`)
            sb.append(" dat=").append(it.dataString)
            sb.append(" flags=").append(Integer.toHexString(it.flags))
            SLog.r("XZpath", sb.toString(), 1200L)
        }
    }

    /** 扫实参里的 Intent（含 Intent[]，`startActivities` 用）；各入口形参布局不同，按类型找最稳 */
    private fun pick(param: XMP): Intent? {
        for (a in param.args) {
            if (a is Intent) return a
            if (a is Array<*>) {
                (a.firstOrNull { it is Intent } as? Intent)?.let { return it }
            }
        }
        val self = param.thisObject ?: return null
        (Rt.get(self, "mIntent") as? Intent)?.let { return it }
        Rt.get(self, "mRequest")?.let {
            (Rt.get(it, "intent") as? Intent)?.let { r -> return r }
        }
        return null
    }

    /** callingPackage：第一个「像包名」的 String */
    private fun callerOf(param: XMP): String? {
        for (a in param.args) {
            if (a is String && a.contains('.') && !a.startsWith(".")) return a
        }
        return null
    }
}
