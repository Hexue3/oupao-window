package com.oupao.wiz

import android.os.Handler
import android.os.Looper

/**
 * 3339：**平行小窗**（全屏二级页 + 同应用小窗主页），对应老 smali 版（v212）的 Split 机制。
 *
 * ## 要解决的问题
 * 微信/QQ 全屏停在二级页（朋友圈 / 聊天页）时，从扇形菜单再选同一个应用：
 * - 原地转换（3284 那套 `switchFocusedTaskToZoom`）会把**二级页本身**变成小窗，用户要的是
 *   「全屏二级页留着，小窗显示**主页**」；
 * - 直接 `startZoomWindow` 开新窗的话，应用（singleTask）会把主页 Activity 迁到新 task，
 *   旧 task 里只剩二级页 → 从小窗回来、或二级页按返回，会**直接掉到桌面**。
 *
 * ## v212 的做法（反编译确认）
 * 1. `markSplitFromBubble()`：记 `splitTask`（二级页 task）+ `splitClass`（栈顶类名）；
 * 2. 小窗建好后记 `zoomTask`、`armed=true`；
 * 3. hook `ActivityRecord.makeFinishingLocked`：命中 splitTask 时**取消 finish** + 把主界面顶前；
 * 4. 800ms 后 `IActivityTaskManager.removeTask(splitTask)` 清理。
 *
 * ## 我们这一版
 * 只做「保住返回链」这一件事，不做 removeTask（用户要的是二级页留着）：
 * 二级页被返回关闭时，取消这次 finish 并**立刻拉起该应用的主界面**，
 * 于是用户看到的是「朋友圈 → 主界面」，而不是回到桌面。
 */
object SplitGuard {

    private const val TAG = "Split"

    private val main = Handler(Looper.getMainLooper())

    @Volatile
    private var armed = false

    @Volatile
    private var splitTask = 0

    @Volatile
    private var splitClass: String? = null

    @Volatile
    private var splitPkg: String? = null

    /** 3351：主应用 / 分身（0 / 998…）。不留用户维度的话，分身二级页返回会被当成主应用处理。 */
    @Volatile
    private var splitUser = -1

    @Volatile
    private var armedAt = 0L

    /** 3350：主界面小窗的 task id（布防后缓存，返回时直接切，省一次任务列表扫描） */
    @Volatile
    private var zoomTaskCache = 0

    /** 超过这个时间自动解除（防止状态残留影响之后的普通返回） */
    private const val ARM_TTL = 60_000L

    // ---------------- 安装 ----------------

    @JvmStatic
    fun install(cl: ClassLoader?): Int {
        var n = 0
        val cls = runCatching { Rt.cls(cl, "com.android.server.wm.ActivityRecord") }.getOrNull()
        if (cls == null) {
            SLog.i("$TAG: 没找到 ActivityRecord（平行小窗的返回兜底不生效）")
            return 0
        }
        val m = runCatching { Rt.methodByName(cls, "makeFinishingLocked", 0) }.getOrNull()
        if (m == null) {
            SLog.i("$TAG: 没找到 makeFinishingLocked")
            return 0
        }
        runCatching { m.isAccessible = true }
        val cb = object : XMH() {
            override fun beforeHookedMethod(param: XMP) {
                runCatching { onFinishCandidate(param, "makeFinishingLocked") }
            }
        }
        if (XBridge.hookMethod(m, cb) != null) n++
        SLog.i("$TAG: makeFinishingLocked 钩子已挂 n=$n")

        // ★ 3343：C16 实测「朋友圈返回」**不走** makeFinishingLocked（日志里一条命中都没有），
        //   所以再挂 finishIfPossible 的几个重载兜底（AOSP 14+ 的返回/关闭都从这里进）。
        for (argc in intArrayOf(5, 4, 3, 2, 1)) {
            val fm = runCatching { Rt.methodByName(cls, "finishIfPossible", argc) }.getOrNull() ?: continue
            runCatching { fm.isAccessible = true }
            val fcb = object : XMH() {
                override fun beforeHookedMethod(param: XMP) {
                    runCatching { onFinishCandidate(param, "finishIfPossible/$argc") }
                }
            }
            if (XBridge.hookMethod(fm, fcb) != null) {
                n++
                SLog.i("$TAG: finishIfPossible/$argc 钩子已挂")
            }
        }
        return n
    }

    /** 统一的「这个记录要被 finish 了」处理：命中就取消 + 回主界面 */
    private fun onFinishCandidate(param: XMP, from: String) {
        val rec = param.thisObject ?: return
        // ★ 3345：字段名在 C16 上可能是 `mTask`（v212 用 "task" 是它那台机器的字段名）。
        //   之前取不到就直接 return，所以日志里一条 `finish事件` 都没有 → 看起来像"钩子没触发"。
        val task = taskOf(rec)
        val tid = if (task != null) Win.taskId(task) else -1
        val clsName = clsOf(rec)
        logSeen(from, tid, clsName)
        if (!armed) return
        if (task == null) {
            SLog.i("$TAG: 命中 finish 但取不到 task rec=$clsName")
            return
        }
        // ★ 3346：**不再死认 taskId**。实测（C16，微信朋友圈）开小窗时应用会把朋友圈
        //   Activity 搬到一个**新 task**（日志 `task=33848` vs 布防的 `33845`），
        //   v212 那台机器不搬所以它按 taskId 判没问题。这里改成「同包 + 类名命中」。
        val pkg = pkgOf(rec)
        if (pkg != null && splitPkg != null && pkg != splitPkg) return
        // ★ 3351：**用户维度**。分身和主应用同包名，只比包名会让分身的二级页返回
        //   跳到主应用上（用户实测）。record 的 task 用户对不上就直接不处理。
        if (splitUser >= 0) {
            val ru = Win.taskUid(task) / 100000
            if (ru != splitUser) {
                SLog.i("$TAG: 用户不符 rec.u=$ru split.u=$splitUser → 忽略")
                return
            }
        }
        if (clsName == null || clsName != splitClass) return
        SLog.i("$TAG: 命中 finish[$from] task=$tid(布防$splitTask) rec=$clsName → 回主界面")
        // ★ 3349：沿用 v212 的时序（连贯、无桌面闪现）：
        //   ① 取消这次 finish（朋友圈先别关，避免露出桌面）；
        //   ② 把**已存在的主界面小窗 task** 顶到前台（同一帧切过去，视觉上是"返回主界面"）；
        //   ③ 800ms 后删掉朋友圈所在 task → 返回栈里只剩主界面，再返回就是桌面。
        param.result = null
        param.returnEarly = true
        // ★ 3350：返回要**尽量即时**：
        //   ① 小窗 task id 在布防后就已经缓存好（见 armFromCurrent），这里不再扫 30 个 task；
        //   ② 直接在当前线程 `moveTaskToFront`，不再 post 一帧。
        val pk = splitPkg ?: pkg
        var zid = zoomTaskCache
        if (zid <= 0 && pk != null) zid = zoomTaskId(pk, splitUser)
        disarm("split-handled")
        if (zid > 0) {
            frontTask(zid)
        } else {
            dashToMain("back")
        }
        main.postDelayed({
            SLog.i("$TAG: 移除二级页 task#$tid")
            removeTask(tid)
        }, 800L)
    }

    /** 该应用当前的小窗（弹性）task id；没有返回 -1 */
    private fun zoomTaskId(pkg: String, user: Int = -1): Int = runCatching {
        val c = Class.forName("android.app.ActivityTaskManager")
        val svc = Rt.invoke(Rt.methodByName(c, "getService", 0), null, null) ?: return -1
        val list = Rt.callLoose(svc, "getTasks", arrayOf<Any?>(Integer.valueOf(30))) as? List<*> ?: return -1
        for (o in list) {
            val info = o ?: continue
            val vis = Rt.get(info, "isVisible")
            if (vis is Boolean && !vis) continue
            val top = Rt.get(info, "topActivity") as? android.content.ComponentName ?: continue
            if (top.packageName != pkg) continue
            val id = (Rt.get(info, "taskId") as? Number)?.toInt() ?: continue
            val t = FlyH.taskById(id) ?: continue
            // 3351：按用户过滤，避免把主应用的小窗当成分身的
            if (user >= 0 && (Win.taskUid(t) / 100000) != user) continue
            if (Win.zoomState(t) != 0) return id
        }
        -1
    }.getOrDefault(-1)

    /** `ActivityTaskManagerService.removeTask(int)`（精确签名，别按参数个数找） */
    private fun removeTask(tid: Int) {
        if (tid <= 0) return
        runCatching {
            val w = Hooks.wms() ?: return@runCatching
            val atms = Rt.get(w, "mAtms") ?: Rt.get(w, "mAtmService") ?: return@runCatching
            val m = Rt.method(atms.javaClass, "removeTask", arrayOf<Class<*>>(Integer.TYPE))
                ?: return@runCatching
            Rt.invoke(m, atms, arrayOf<Any?>(Integer.valueOf(tid)))
        }
    }

    private fun pkgOf(rec: Any): String? = runCatching {
        (Rt.get(rec, "mActivityComponent") as? android.content.ComponentName)?.packageName
    }.getOrNull()

    /** ActivityRecord 的 task：兼容 `task` / `mTask` / `getTask()` */
    private fun taskOf(rec: Any): Any? {
        runCatching { Rt.get(rec, "task") }.getOrNull()?.let { return it }
        runCatching { Rt.get(rec, "mTask") }.getOrNull()?.let { return it }
        return runCatching { Rt.callNoArgQ(rec, "getTask") }.getOrNull()
    }

    @Volatile
    private var lastSeenAt = 0L

    private fun logSeen(from: String, tid: Int, cls: String?) {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastSeenAt < 800L) return
        lastSeenAt = now
        SLog.i("$TAG: finish事件[$from] task=$tid cls=$cls armed=$armed split=$splitTask")
    }

    // ---------------- 判定 / 布防 ----------------

    /**
     * 当前 pkg 是否「全屏、且不在主界面」（= 二级页）。
     *
     * 判据：
     * - 有一个可见的全屏 task（`zoomState == 0`）；
     * - 该 task 的栈顶 Activity 类名 ≠ 应用的启动 Activity 类名（微信：LauncherUI vs ChattingMainUI/Sns…），
     *   或者该 task 里不止一个 Activity（QQ 的聊天页其实是 SplashActivity + Fragment，只能靠这个）。
     */
    @JvmStatic
    fun isSecondaryFullscreen(pkg: String?): Boolean = isSecondaryFullscreen(pkg, -1)

    /** 3351：带用户维度（-1 = 不限，主应用路径传 0，分身传 998/999） */
    @JvmStatic
    fun isSecondaryFullscreen(pkg: String?, userId: Int): Boolean {
        // 3352：开关（默认关）。关着时行为与旧版一致（原地转小窗/普通开窗）。
        if (!Cfg.splitWin()) return false
        val t = info(pkg, userId) ?: return false
        return t.secondary
    }

    /** 记录当前全屏 task + 栈顶类名（由 FlyH 在决定「开新窗」时调用） */
    @JvmStatic
    fun armFromCurrent(pkg: String?) = armFromCurrent(pkg, -1)

    @JvmStatic
    fun armFromCurrent(pkg: String?, userId: Int) {
        val t = info(pkg, userId) ?: return
        splitTask = t.taskId
        splitClass = t.topClass
        splitPkg = pkg
        splitUser = if (userId >= 0) userId else (t.user)
        armedAt = android.os.SystemClock.uptimeMillis()
        armed = true
        SLog.i("$TAG: 布防 task=${t.taskId} u=$splitUser cls=${t.topClass} pkg=$pkg 二级页=${t.secondary}")
        // v212 doRestore()：小窗建好之后再把**原全屏 task 顶回前台**（小窗是浮动窗，仍浮在上面），
        // 否则应用把主界面搬去新 task 后，旧 task 会退到桌面后面甚至变不可见。
        val tid = t.taskId
        val u = splitUser
        main.postDelayed({
            frontTask(tid)
            // 3350：顺手缓存主界面小窗的 task id —— 返回时要立刻切过去，不能再现场扫任务列表
            val z = zoomTaskId(pkg ?: "", u)
            if (z > 0) zoomTaskCache = z
        }, 700L)
        // ★ 3344：不依赖 ROM 走哪条 finish API —— 直接盯「原 task 还在不在」。
        //   没了（= 返回把二级页退掉、task 被销毁）就拉起主界面，避免掉到桌面。
        startWatcher(tid)
    }

    @Volatile
    private var watchGen = 0

    private fun startWatcher(tid: Int) {
        val gen = ++watchGen
        val deadline = android.os.SystemClock.uptimeMillis() + 20_000L
        val poll = object : Runnable {
            override fun run() {
                if (gen != watchGen || !armed) return
                if (android.os.SystemClock.uptimeMillis() > deadline) {
                    SLog.i("$TAG: 看门狗超时，解除")
                    disarm("watch-timeout")
                    return
                }
                val alive = taskAlive(tid)
                if (!alive) {
                    SLog.i("$TAG: 原 task#$tid 已消失 → 回主界面")
                    dashToMain("taskGone")
                    return
                }
                main.postDelayed(this, 400L)
            }
        }
        main.postDelayed(poll, 400L)
    }

    /** task 还在不在（用 ActivityTaskManager.getTasks 查，避免拿到已销毁的壳） */
    private fun taskAlive(tid: Int): Boolean = runCatching {
        val c = Class.forName("android.app.ActivityTaskManager")
        val svc = Rt.invoke(Rt.methodByName(c, "getService", 0), null, null) ?: return@runCatching true
        val list = Rt.callLoose(svc, "getTasks", arrayOf<Any?>(Integer.valueOf(30))) as? List<*> ?: return@runCatching true
        for (o in list) {
            val id = (Rt.get(o ?: continue, "taskId") as? Number)?.toInt() ?: continue
            if (id == tid) return@runCatching true
        }
        false
    }.getOrDefault(true)

    /** `ActivityManager.moveTaskToFront(taskId, 0)` */
    private fun frontTask(tid: Int) {
        if (tid <= 0) return
        runCatching {
            val c = Cfg.sysContext() ?: return@runCatching
            val am = c.getSystemService("activity") ?: return@runCatching
            val m = Rt.methodByName(am.javaClass, "moveTaskToFront", 2) ?: return@runCatching
            Rt.invoke(m, am, arrayOf<Any?>(Integer.valueOf(tid), Integer.valueOf(0)))
            SLog.i("$TAG: 前拉二级页 task#$tid")
        }
    }

    @JvmStatic
    fun disarm(reason: String) {
        if (!armed) return
        armed = false
        zoomTaskCache = 0
        SLog.i("$TAG: 解除($reason)")
    }

    // ---------------- 内部 ----------------

    private class TaskInfo(val taskId: Int, val topClass: String?, val secondary: Boolean, val user: Int)

    private fun info(pkg: String?, userId: Int): TaskInfo? {
        if (pkg.isNullOrEmpty()) return null
        return runCatching {
            // 复用 FlyH 里已经验证过的「可见全屏 task」查找（getTasks + zoomState==0）
            val task = FlyH.fullscreenTaskOf(pkg, userId) ?: run {
                SplitGuard.logDecision("noFullTask", pkg, -1, null, null, 0)
                return null
            }
            val id = Win.taskId(task)
            // ★ 栈顶类名必须从 `RunningTaskInfo.topActivity` 取（Task.getTopActivity() 实测为 null）；
            //   用户的判据：**朋友圈 / 公众号这类才算二级页**，聊天页的类名仍是 LauncherUI，不算。
            val topCls = topClassOf(pkg, id)
            val mainCls = mainClassOf(pkg)
            val secondary = (mainCls != null && topCls != null && topCls != mainCls)
            SplitGuard.logDecision("ok", pkg, id, topCls, mainCls, if (secondary) 1 else 0)
            TaskInfo(id, topCls, secondary, Win.taskUid(task) / 100000)
        }.getOrNull()
    }

    /** 从 `ActivityTaskManager.getTasks()` 里取指定 task 的栈顶 Activity 类名 */
    private fun topClassOf(pkg: String, tid: Int): String? = runCatching {
        val c = Class.forName("android.app.ActivityTaskManager")
        val svc = Rt.invoke(Rt.methodByName(c, "getService", 0), null, null) ?: return null
        val list = Rt.callLoose(svc, "getTasks", arrayOf<Any?>(Integer.valueOf(30))) as? List<*> ?: return null
        for (o in list) {
            val info = o ?: continue
            val id = (Rt.get(info, "taskId") as? Number)?.toInt() ?: continue
            if (id != tid) continue
            val top = Rt.get(info, "topActivity") as? android.content.ComponentName ?: return null
            if (top.packageName != pkg) return null
            return top.className
        }
        null
    }.getOrNull()

    @Volatile
    private var lastDecisionAt = 0L

    private fun logDecision(tag: String, pkg: String, tid: Int, top: String?, main: String?, kids: Int) {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastDecisionAt < 1000L) return
        lastDecisionAt = now
        SLog.i("$TAG: 判定[$tag] pkg=$pkg task=$tid top=$top main=$main kids=$kids")
    }

    private fun mainClassOf(pkg: String): String? = runCatching {
        val c = Cfg.sysContext() ?: return null
        val i = c.packageManager.getLaunchIntentForPackage(pkg) ?: return null
        i.component?.className
    }.getOrNull()

    private fun clsOf(rec: Any): String? = runCatching {
        val cn = Rt.get(rec, "mActivityComponent") as? android.content.ComponentName
        cn?.className
    }.getOrNull()

    /** 拉起该应用主界面（全屏），替代"返回掉到桌面" */
    private fun dashToMain(why: String) {
        val pkg = splitPkg
        val u = splitUser
        disarm("dash-$why")
        if (pkg.isNullOrEmpty()) return
        // ★ 3347：**延后 350ms 再拉**。实测立刻 startActivity 会被正在拆掉的 task 吞掉
        //   （日志里 `命中 finish` 之后没有 `回主界面`，屏幕直接落到桌面）。
        //   等任务拆完、桌面稳定后再启动，微信才会真正到前台。
        main.postDelayed({
            runCatching {
                val c = Cfg.sysContext() ?: return@runCatching
                val i = c.packageManager.getLaunchIntentForPackage(pkg) ?: return@runCatching
                // ★ 3348：**清栈**。取消 finish 后朋友圈仍留在返回栈里 → 主界面再返回又回到朋友圈。
                //   NEW_TASK | CLEAR_TASK 直接把该应用的任务重置成「主界面」一层。
                i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                i.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                // ★ 3351：分身要按用户启动，否则会拉起主应用
                if (u > 0) {
                    com.oupao.wiz.FrameworkExt.startActivityAsUser(
                        c, i, com.oupao.wiz.FrameworkExt.userHandleOf(u)
                    )
                    SLog.i("$TAG: 回主界面(分身 u=$u) pkg=$pkg ($why)")
                } else {
                    c.startActivity(i)
                    SLog.i("$TAG: 回主界面(清栈) pkg=$pkg ($why)")
                }
            }
        }, 350L)
    }
}
