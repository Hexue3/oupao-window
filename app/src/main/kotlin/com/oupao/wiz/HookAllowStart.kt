package com.oupao.wiz

/**
 * 直接放行应用跳转，跳过 ColorOS 的「即将离开 XX 打开其他应用」确认。
 *
 * ## 调用链（dexdump 实证，oplus-services）
 *
 * ```
 * OplusSecurityPermissionManager.checkAllowStartActivity(…): I
 *   ├─ MiniProgramController.checkAllowStartActivity      → 非 0 就返回
 *   ├─ SecurityPayController.checkAllowStartActivity      → 非 0/-1 就返回
 *   ├─ OplusCooperativeGamePayController.checkAllowStart…  → 非 0 就返回
 *   └─ OplusActivityStartController.checkAllowStartActivity(…)  ← 最终决策
 * ```
 *
 * 返回值语义：**0 = 直接放行**（上游正是 `if-nez v0` 才继续往下走），
 * 非 0 才进入拦截/弹确认。
 *
 * 所以把这个方法压成 0，就不会弹窗。之前只挂
 * `isInterceptActivityStart()` 之类的布尔闸门不够，因为最终值来自这里。
 *
 * 受「应用间跳转小窗打开」开关 [Cfg.crossZoom] 控制：开着就抑制，关掉立刻恢复系统行为。
 */
class HookAllowStart : XMH() {

    override fun beforeHookedMethod(param: XMP) {
        runCatching {
            if (!Cfg.crossZoom()) return@runCatching
            // ★ 3359+（外部 C16 日志）：同一个 `checkAllowStartActivity` 有两套返回值语义。
            //   · 旧链路 OplusActivityStartController：0 = 放行；
            //   · 新链路 OplusAppStartConfirmManager.checkStartActivityForConfirm：
            //     -1 = 不用弹确认（直接放行）、9 = 恶意拦截、**0 = 继续走确认流程**。
            //   老代码一律压 0，在新链路上等于"要求确认"，随后
            //   `atms_intercept: executeRequest abort:-91` 把启动整个取消
            //   → QQ 里选 MT / 安装器点了没反应（外部用户 2026-11-12 实测）。
            //   这里按调用栈区分：来自 AppStartConfirmManager 就返回 -1，其它保持 0。
            val fromConfirm = runCatching {
                Throwable().stackTrace.any { it.className.endsWith("OplusAppStartConfirmManager") }
            }.getOrDefault(false)
            val ret = if (fromConfirm) -1 else 0
            if (CrossZoom.diagOn()) {
                // 只记「真的被调用」的次数与类型，用来验证弹窗链路是否经过这里
                SLog.r(
                    "XZAllow",
                    "XZAllow: t=${android.os.SystemClock.uptimeMillis()}" +
                        " ret=" + ret + " confirm=" + fromConfirm +
                        " type=" + (param.args.getOrNull(4) ?: "?") +
                        " caller=" + (param.args.getOrNull(0) ?: "?"),
                    300L
                )
            }
            param.result = ret
            param.returnEarly = true
        }
    }
}

/**
 * 直接旁路 OPLUS 新版「应用启动确认」：
 * `OplusAppStartConfirmManager.checkStartActivityForConfirm(...)` 返回 **null = 不拦截**，
 * 非 null = 需要弹确认/拦截。只把方法压掉，后面的 AppLock 检查照常走。
 *
 * 只挂在返回引用类型的方法上（Pair）；参数个数按实际声明匹配，不写死。
 */
class HookAppStartConfirm : XMH() {

    override fun beforeHookedMethod(param: XMP) {
        runCatching {
            if (!Cfg.crossZoom()) return@runCatching
            param.result = null
            param.returnEarly = true
        }
    }
}
