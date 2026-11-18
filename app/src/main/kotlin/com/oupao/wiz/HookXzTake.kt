package com.oupao.wiz

import android.content.Intent

/**
 * 通用接管点：`ActivityStarter.startActivityInner`。
 *
 * ## 为什么需要它
 *
 * 微信「用浏览器打开」实测**绕过 ATMS 的所有公开入口**
 * （`startActivity` / `startActivityAsUser` / `startActivityAsCaller` /
 *  `startActivityWithConfig` / `startActivities` / `startActivityIntentSender` 全无记录），
 * 却一定会走到 `ActivityStarter.startActivityInner` —— 那里日志 `XZrec` 有记录。
 * 也就是说启动请求是 **system_server 内部**生成的。
 *
 * 所以在这一层继续拦：拿到 `ActivityRecord` 上的 `launchedFromPackage`（谁发起）
 * 和 `packageName`（目标），判据与 [CrossZoom.shouldTake] 完全一致，
 * 命中就让 [CrossZoom.fire] 走小窗，并**取消原启动**（返回 START_SUCCESS）。
 *
 * ## 风险控制
 *
 * - 判据同一套：只接管「跨应用 + 非桌面 + 非同应用 + 开关打开」；
 * - `fire()` 失败 → 什么都不做，原方法照常执行（照旧全屏），不会打不开；
 * - `inZoom` 护栏：我们自己发起的小窗启动不会被再次接管。
 */
class HookXzTake : XMH() {

    override fun beforeHookedMethod(param: XMP) {
        runCatching {
            if (CrossZoom.inZoomNow()) return@runCatching
            if (!Cfg.crossZoom()) return@runCatching

            val rec = param.args.firstOrNull {
                it != null && it.javaClass.name.endsWith("ActivityRecord")
            } ?: return@runCatching

            val from = Rt.get(rec, "launchedFromPackage") as? String
            val target = Rt.get(rec, "packageName") as? String
            val it = Rt.get(rec, "intent") as? Intent ?: return@runCatching
            val userId = (Rt.get(rec, "mUserId") as? Int) ?: 0

            if (from.isNullOrEmpty() || target.isNullOrEmpty()) return@runCatching

            // 2850：**带 resultTo（startActivityForResult）的一律不接管**。
            //   实测：QQ 下载文件 → 「用其他应用打开」→ MT 管理器
            //   → 「定位所在位置」没任何反应：那一下是带回调发的，
            //   我们把它接走放进小窗 → 回调丢失 → 对方永远等不到结果（既不小窗也不全屏）。
            //   这类请求必须放行原启动（fail-open）。
            val resultTo = runCatching { Rt.get(rec, "resultTo") }.getOrNull()
            if (resultTo != null) {
                if (CrossZoom.diagOn()) {
                    SLog.r("XZtake", "XZtake: 带 resultTo 不接管 " + from + " -> " + target + " act=" + it.action, 500L)
                }
                return@runCatching
            }

            // ★ 就地记录「刚出现过选择器」：不要依赖 shouldTake 里打点，
            //   实测那样跨钩子读不到（2298 仍然 take=false）。
            //   选择器自身的记录也会走到这里（act=CHOOSER / target=选择器包）。
            if (Intent.ACTION_CHOOSER == it.action ||
                "com.android.intentresolver" == target || "android" == target ||
                "com.oplus.multiapp" == target
            ) {
                CrossZoom.noteChooser()
            }

            CrossZoom.diagRes("Take", from, target, resultTo != null, param.args, -1)
            // 2979：选择器类启动不接管
            if (CrossZoom.chooserJustSeen()) return@runCatching
            val take = CrossZoom.shouldTake(it, from, target, resultTo != null)
            if (CrossZoom.diagOn()) {
                SLog.r(
                    "XZtake",
                    "XZtake: t=${android.os.SystemClock.uptimeMillis()}" +
                        " $from -> $target take=$take act=${it.action} u=$userId" +
                        " chAge=${CrossZoom.chooserAge()}" +
                        " rsn=${CrossZoom.takeReason(it, from, target, false)}",
                    300L
                )
            }
            if (!take) return@runCatching

            // 2870：**磁贴类（来自 SystemUI）改成「补 options + 放行」**。
            //   之前我们取消原启动、自己直调 startZoomWindow，结果 SystemUI 那个
            //   「收起控制中心（带动画）」的流程也被一并取消了 → 用户看到「没有收回动画」。
            //   开源对磁贴就是补参数后放行，收面板与过渡全交给系统。
            // 2872：磁贴的注入已挪到正确层级（HookStarter @ ActivityStarter.execute）。
            //   这里不再做任何磁贴相关的事（那是错层级的兜底）。

            val fired = CrossZoom.fire(it, userId, rec.javaClass.classLoader, null, "Take")
            // 2853b：**不看 caller**，只要小窗真的启动成功就收一次面板。
            //   实测发现磁贴那条路径的 caller 不一定是 com.android.systemui（ColorOS 会转由桌面代理），
            //   按 caller 判会漏掉；而 collapsePanels 在面板未展开时是空操作，多调一次无害。
            // 2872：**不再手动收面板**。那是因为取消了原启动而只能自己补的兜底；
            //   磁贴现在走「补参数 + 放行」，收面板由系统完成。
            if (fired) {
                // 已交给小窗：取消这次启动。
                // 这里必须把 ActivityStarter 的几个字段补成「有记录」的状态，
                // 否则后续 ActivityRecord.getTask() 会 NPE 并把异常 parcel 回调用方（App 闪退）。
                param.result = 0
                param.returnEarly = true
                setField(param.thisObject, "mLastStartActivityRecord", rec)
                setField(param.thisObject, "mLastStartActivityResult", 0)
                setField(param.thisObject, "mStartActivity", rec)
            }
        }
    }

    /** 反射写字段（沿继承链找）。失败静默 —— 只是尽力减少下游 NPE。 */
    private fun setField(obj: Any?, name: String, value: Any?) {
        if (obj == null) return
        runCatching {
            var c: Class<*>? = obj.javaClass
            while (c != null) {
                val f = runCatching { c!!.getDeclaredField(name) }.getOrNull()
                if (f != null) {
                    f.isAccessible = true
                    f.set(obj, value)
                    return
                }
                c = c.superclass
            }
        }
    }}
