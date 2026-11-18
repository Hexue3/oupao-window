package com.oupao.wiz

/**
 * 让「不可缩放（resizeableActivity=false）」的页面也能真正进小窗。
 *
 * ## 为什么需要（实测证据，2026-10-29）
 * - `startZoomWindow` 对这类页面返回 0，但系统最终按全屏显示：
 *   `D/FlexibleTaskController: … prevTopTask:Task{… com.coloros.oshare nonResizable}`；
 * - 换 FlyH 那条「原生小窗启动」（`sysContext.startActivity` + `windowingMode=100`
 *   + `mZoomLaunchFlags=4`）也一样：调用成功（`XZoom: 原生小窗通道 ok=true`），窗口仍是全屏。
 * → 根子在 **Task 建成 nonResizable**，只能让它按「可缩放」建出来。
 *
 * ## 本 ROM 的可缩放判据
 * `android.content.pm.ActivityInfo` **没有** `isResizeable()` 方法（反射实测），只有：
 * - 静态 `isResizeableMode(int)`
 * - 字段 `resizeMode`；`android.app.TaskInfo` 上是 `isResizeable` / `resizeMode` 字段。
 *
 * ## 两条钩子（都只在「我们自己发小窗」的 4s 宽限窗内生效）
 * 1. `ActivityInfo.isResizeableMode(int)` → 宽限窗内返回 true（静态方法，安全）；
 * 2. `ActivityRecord` **构造完成之后** → 若是本次 zoom 目标：
 *    `info.resizeMode = FORCE_RESIZABLE`，并把记录上所有名字含 `resiz` 的 boolean 字段按成 true，
 *    再给它的 Task 调一次 `setResizeable(true)`（best-effort，全部打日志）。
 *
 * ⚠️⚠️ **构造函数这条绝对不能 `returnEarly`** —— 2330 就是因为在 before 里对构造函数
 * `returnEarly`，ActivityRecord 没初始化（info=null），system_server 里
 * `hansActivityIfNeeded` NPE → SystemUI 崩、system_server 连续重启。
 * 本类用 [kind] 把两种钩子彻底分开：构造钩子只走 after，永不 returnEarly。
 *
 * 关掉：`setprop persist.oupao.zoom_resize 0`
 */
class HookResizeFix(
    private val entry: String = "?",
    /** 0 = ActivityInfo.isResizeableMode（静态判据）；1 = ActivityRecord 构造后改标记 */
    private val kind: Int = KIND_MODE
) : XMH() {

    override fun beforeHookedMethod(param: XMP) {
        if (kind != KIND_MODE) return          // 构造钩子绝不在这里动 result
        runCatching {
            if (!CrossZoom.resizeGraceActive()) return@runCatching
            param.result = true
            param.returnEarly = true
            SLog.r(
                "NResize",
                "NResize: isResizeableMode 放行 $entry 目标=" + CrossZoom.resizeTargetDesc(),
                1500L
            )
        }
    }

    override fun afterHookedMethod(param: XMP) {
        if (kind != KIND_REC) return
        runCatching {
            if (!CrossZoom.resizeGraceActive()) return@runCatching
            val rec = param.thisObject ?: return@runCatching
            val info = infoOf(rec) ?: return@runCatching
            if (!CrossZoom.resizeOverrideHit(info)) return@runCatching

            val sb = StringBuilder()
            for (f in rec.javaClass.declaredFields) {
                if (!f.name.lowercase().contains("resiz")) continue
                runCatching {
                    f.isAccessible = true
                    if (f.type == java.lang.Boolean.TYPE) {
                        if (!f.getBoolean(rec)) {
                            f.setBoolean(rec, true)
                            sb.append(f.name).append("=true ")
                        }
                    } else {
                        sb.append(f.name).append(':').append(f.type.simpleName).append(' ')
                    }
                }
            }
            val task = runCatching { Rt.callNoArg(rec, "getTask") }.getOrNull()
                ?: runCatching { Rt.get(rec, "mTask") }.getOrNull()
            val taskOk = runCatching {
                val m = task?.javaClass?.methods?.firstOrNull {
                    it.name == "setResizeable" && it.parameterTypes.size == 1 &&
                        it.parameterTypes[0] == java.lang.Boolean.TYPE
                } ?: return@runCatching false
                m.isAccessible = true
                m.invoke(task, true)
                true
            }.getOrDefault(false)

            SLog.r(
                "NResize",
                "NResize: ActivityRecord 已按成可缩放 $entry 改=[$sb] taskSetResizeable=$taskOk " +
                    CrossZoom.descOf(info),
                1500L
            )
        }
    }

    private fun infoOf(rec: Any): Any? {
        for (n in arrayOf("info", "mActivityInfo", "mInfo")) {
            val v = runCatching { Rt.get(rec, n) }.getOrNull()
            if (v != null && v.javaClass.name.endsWith("ActivityInfo")) return v
        }
        return rec.javaClass.declaredFields.firstNotNullOfOrNull { f ->
            runCatching {
                if (!f.type.name.endsWith("ActivityInfo")) return@runCatching null
                f.isAccessible = true
                f.get(rec)
            }.getOrNull()
        }
    }

    companion object {

        const val KIND_MODE = 0
        const val KIND_REC = 1

        @JvmStatic
        fun install(cl: ClassLoader?): Int {
            var n = 0

            // ① 静态判据
            runCatching {
                val c = Rt.cls(cl, "android.content.pm.ActivityInfo") ?: return@runCatching
                for (m in c.declaredMethods) {
                    if (m.name != "isResizeableMode") continue
                    if (m.returnType != java.lang.Boolean.TYPE) continue
                    if (m.parameterTypes.size != 1) continue
                    if (XBridge.hookMethod(m, HookResizeFix("mode", KIND_MODE)) != null) {
                        n++
                        SLog.i("NResize: 已挂 ActivityInfo.isResizeableMode(int)")
                    }
                }
            }.onFailure { SLog.e("NResize: isResizeableMode 安装失败", it) }

            // ② 记录/任务上的可缩放标记（只在 after 动，绝不 returnEarly）
            runCatching {
                val c = Rt.cls(cl, "com.android.server.wm.ActivityRecord") ?: return@runCatching
                var cn = 0
                for (ctor in c.declaredConstructors) {
                    if (XBridge.hookMethod(ctor, HookResizeFix("ctor" + ctor.parameterTypes.size, KIND_REC)) != null) {
                        cn++
                    }
                }
                n += cn
                SLog.i("NResize: 已挂 ActivityRecord 构造函数 " + cn + " 个（仅 after，不动构造）")
            }.onFailure { SLog.e("NResize: ActivityRecord 钩子安装失败", it) }

            if (n == 0) SLog.i("NResize: 一个都没挂上（不可缩放页面仍只能全屏）")
            return n
        }
    }
}
