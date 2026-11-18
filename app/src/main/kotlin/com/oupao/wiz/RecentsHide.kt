package com.oupao.wiz

import android.util.Log

/**
 * 多任务界面里隐藏「迷你窗应用」（FlymeUI 页 → `oupao_recents_hide_freeform`）。
 *
 * ## 做法（本模块自己的实现）
 * 多任务列表是桌面（Quickstep）自己拼的，最终每条记录都要过一遍它自己的过滤器
 * `OplusRecentTasksFilter.filterTaskInfo(...)`（返回 true = 这条不要）。
 * 我们在过滤器前面拦一道：**只对「迷你窗状态、且不是分屏组」的记录返回 true**。
 *
 * ## 只隐藏迷你窗，不隐藏正常小窗（2026-11-17 用户明确要求）
 * `key_flexible_task_state` 来自 `TaskExtImpl.mFlexibleZoomState`，取值见
 * `OplusZoomWindowManager$ZoomConstant`：
 * ```
 * 0 = IDLE（普通/全屏）      1 = ZOOM_STATE（正常小窗）
 * 2 = ZOOM_MINI_STATE（迷你窗）  3 = FLOAT（悬浮）
 * 4 = SCALE（手动缩放）       5 = SUPER_MINI（贴边迷你）
 * 8 = READY（迷你窗稳定态）   9 = TILING（分屏）
 * ```
 * 旧实现用 `state != 0`，把正常小窗（1）也当成小窗隐藏了 → 用户关掉小窗后
 * 在多任务里看不到刚用过的应用。现在只认 2/5/8。
 *
 * ## 本机（C16 / ColorOS 16）反编译核对过的接口
 * ```
 * com.oplus.quickstep.data.OplusRecentTasksFilter
 *     filterTaskInfo(int, int, GroupedTaskInfo, ArrayList<String>) : boolean   // true = 过滤掉
 * com.android.wm.shell.shared.GroupedTaskInfo
 *     getTaskInfo1() : TaskInfo     // 主任务
 *     getTaskInfo2() : TaskInfo     // 分屏里的第二个任务（不是分屏时为 null）
 * com.android.systemui.shared.recents.utilities.TaskUtils
 *     isFlexibleFloatingWindow(TaskInfo) : boolean
 * ```
 * 判断「是不是小窗」优先用 `TaskUtils.isFlexibleFloatingWindow`（桌面自己也在用它，
 * 语义最准），拿不到再退回看 `configuration.windowConfiguration.windowingMode == 5`。
 *
 * ## 为什么挂在桌面进程
 * 列表只在桌面进程里生成，system_server 侧看不到这一步，所以挂在 [Boot.installLauncher]。
 *
 * ## 开关怎么读
 * 桌面是普通应用进程：读不到模块私有文件、也写不了 `/data/system/oupao.log`。
 * 唯一两边都能读的通道是 `Settings.Global`（见 [Cfg.gsetC]），所以这个键登记在
 * [Cfg] 的 `BOOL_KEYS` 里、由 system_server 收到 CFG 广播后镜像过去；
 * 这里用 [Cfg.recentsHideFreeform] 读（Global 优先，文件兜底），并做 800ms 缓存。
 *
 * 思路来源：同机的开源模块 ColorOS Mod（MIT，© Rikumi Yu）里有同类功能，
 * 这里只借「该拦哪个方法、算什么算小窗」这类接口事实，代码为本模块自行实现。
 */
internal object RecentsHide {

    private const val TAG = "OupaoRecents"

    /**
     * 过滤器类的名字：**每一版 ROM 都可能不一样**，所以按「候选类名 + 方法形状」解析。
     *
     * - C16（ColorOS 16）：`com.oplus.quickstep.data.OplusRecentTasksFilter`（没混淆）
     * - C17（ColorOS 17 / 桌面 17.3.12）：**被 R8 混淆成 `ko.a`**
     *   dexdump 实证：`Lko/a;#d(I,I,Lcom/android/wm/shell/shared/GroupedTaskInfo;Ljava/util/ArrayList;)Z`
     * - C17 上这两个类名**都不存在**：`OplusRecentTasksFilter`、`OplusRecentTasksListImpl`
     *   （C17 的桌面重新实现过 recents，旧的 `getFilter()` 路线也没了）
     *
     * 找不到就只打一行日志、不崩；下一版 ROM 只需往这个数组里加名字。
     */
    private val FILTER_CANDIDATES = arrayOf(
        "com.oplus.quickstep.data.OplusRecentTasksFilter",
        "ko.a"
    )

    private const val CLS_TASKUTILS = "com.android.systemui.shared.recents.utilities.TaskUtils"
    private const val CLS_FLEXMGR = "com.oplus.flexiblewindow.FlexibleWindowManager"

    /** 分屏第二个任务的取值方法（GroupedTaskInfo.getTaskInfo2） */
    private var taskInfo2M: java.lang.reflect.Method? = null

    /** 主任务的取值方法（GroupedTaskInfo.getTaskInfo1） */
    private var taskInfo1M: java.lang.reflect.Method? = null

    /**
     * C17 新增：`GroupedTaskInfo.d()` → `SplitBounds`（非空 = 这是一条「分屏组」记录）。
     * C16 没有这个方法，取不到就是 null。
     * ★ 17.5.5 起 `d()` 变成「第二个 TaskInfo」，SplitBounds 改成了**字段 `t`** → 见 [splitF]。
     */
    private var splitM: java.lang.reflect.Method? = null

    /** C17 17.5.5：`GroupedTaskInfo.t`（SplitBounds 字段）；非空 = 分屏组 */
    private var splitF: java.lang.reflect.Field? = null

    /** C17 17.5.5：`GroupedTaskInfo.r`；== 4 表示 mixed/分屏组 */
    private var typeF: java.lang.reflect.Field? = null

    /** TaskUtils.isFlexibleFloatingWindow(TaskInfo) */
    private var isFreeformM: java.lang.reflect.Method? = null

    /** FlexibleWindowManager.getInstance().getFlexibleWindowState(TaskInfo) == 1（本机 ColorOS 自己那套） */
    private var flexCl: ClassLoader? = null
    private var flexCls: Class<*>? = null
    private var flexInst: Any? = null
    private var flexStateM: java.lang.reflect.Method? = null
    private var flexTried = false

    /** 沿继承链找方法（实例的类可能是子类，方法在父类里） */
    private fun walkMethod(c: Class<*>, name: String, argc: Int): java.lang.reflect.Method? {
        var k: Class<*>? = c
        while (k != null && k != Any::class.java) {
            k.declaredMethods.firstOrNull { it.name == name && it.parameterCount == argc }?.let {
                runCatching { it.isAccessible = true }
                return it
            }
            k = k.superclass
        }
        return null
    }

    /**
     * 惰性解析 ColorOS 的窗口服务：`FlexibleWindowManager.getInstance()`。
     * 模块加载那一刻服务可能还没建好（getInstance 返回 null），所以每次判据前都可重试。
     */
    private fun ensureFlex() {
        if (flexInst != null && flexStateM != null) return
        val cl = flexCl ?: return
        if (flexCls == null) flexCls = runCatching { Rt.cls(cl, CLS_FLEXMGR) }.getOrNull()
        val k = flexCls ?: return
        if (flexInst == null) {
            flexInst = runCatching {
                k.getDeclaredMethod("getInstance").also { it.isAccessible = true }.invoke(null)
            }.getOrNull()
        }
        if (flexInst != null && flexStateM == null) {
            flexStateM = walkMethod(flexInst!!.javaClass, "getFlexibleWindowState", 1)
        }
        if (!flexTried) {
            flexTried = true
            dbg("flex inst=" + (flexInst != null) + " stateM=" + (flexStateM != null))
        }
    }

    /** 开关 800ms 缓存（`Settings.Global` 每读一次都是一次跨进程调用） */
    @Volatile
    private var onCache = false

    @Volatile
    private var onAt = 0L

    /** 装过一次就别再装（LSPosed 对同一进程可能回调多次，重复挂会让同一条记录被过滤两遍） */
    @Volatile
    private var installedPid: Int = -1

    /** 钩子是否真的挂上了（桌面侧能力位/日志都以它为准） */
    @Volatile
    var installed = false
        private set

    /** 当前是否 C17 分支：C17 专属的新判据只在这里启用，保证 C16 走原来的代码路径 */
    @Volatile
    private var c17Mode = false

    /** 诊断计数（只打前几条，别刷屏） */
    private var dbgLeft = 30

    /**
     * 诊断 dump 的**时间窗配额**：每 15 秒最多 12 组。
     * 用配额而不是"前 N 条"，否则开机那次 recents 就把额度耗光，
     * 用户后来开小窗再进多任务时什么都打不出来（已踩过）。
     */
    private var dumpWindowAt = 0L
    private var dumpWindowLeft = 0

    private fun canDump(): Boolean {
        // 默认不打全量 dump（每次开多任务会刷几十行）；排查时 setprop persist.oupao.recents_diag 1
        if (Cfg.getProp("persist.oupao.recents_diag", "0") != "1") return false
        val now = android.os.SystemClock.uptimeMillis()
        if (now - dumpWindowAt > 15_000L) {
            dumpWindowAt = now
            dumpWindowLeft = 12
        }
        if (dumpWindowLeft <= 0) return false
        dumpWindowLeft--
        return true
    }

    /** 每 15 秒最多给 80 条记录打一行 bundle 摘要（覆盖整个 recents 列表） */
    private var compactAt = 0L
    private var compactLeft = 0

    private fun canLogCompact(): Boolean {
        if (Cfg.getProp("persist.oupao.recents_diag", "0") != "1") return false
        val now = android.os.SystemClock.uptimeMillis()
        if (now - compactAt > 15_000L) {
            compactAt = now
            compactLeft = 80
        }
        if (compactLeft <= 0) return false
        compactLeft--
        return true
    }

    private fun dbg(msg: String) {
        if (dbgLeft <= 0) return
        dbgLeft--
        // 走 LSPosed 的模块日志（桌面进程写不了 oupao.log，纯 Log.i 又抓不到）
        runCatching { XBridge.flog("OupaoRecents: " + msg) }
    }

    /** 不加配额地打一行（只给"全量 dump"这种一次性信息用） */
    private fun log1(msg: String) {
        runCatching { XBridge.flog("OupaoRecents: " + msg) }
    }

    /**
     * 装钩子。返回是否真的挂上了（[Caps] 的「支持/不支持」就看这个）。
     */
    fun install(cl: ClassLoader?): Boolean {
        if (cl == null) return false
        // 3315：**C17 与 C16 隔离**。C17 的入口在 RecentsHideC17.kt，
        // 里面复用本文件的扫描/判据（只是类名与 GroupedTaskInfo 取值按 17.5.5 适配）。
        if (Cfg.isC17()) {
            return runCatching { RecentsHideC17.install(cl) }.getOrDefault(false)
        }
        return installInternal(cl, false)
    }

    /** C17 专用入口（由 [RecentsHideC17] 调；不改 C16 的候选类与取值路径） */
    internal fun installC17(cl: ClassLoader?): Boolean {
        if (cl == null) return false
        return installInternal(cl, true)
    }

    private fun installInternal(cl: ClassLoader, c17: Boolean): Boolean {
        val pid = android.os.Process.myPid()
        if (pid == installedPid) return true
        c17Mode = c17
        // ★ 一条诊断抵十次装机：loader 是什么、GroupedTaskInfo 在不在、每个候选类能不能解析。
        //   C16/C17 都出现过「类找不到」，先看清是"类名变了"还是"loader 里没有这个 dex"。
        val grpCls = runCatching {
            Rt.cls(cl, "com.android.wm.shell.shared.GroupedTaskInfo")
                ?: Rt.cls(cl, "com.android.wm.shell.util.GroupedRecentTaskInfo")
        }.getOrNull()
        // C17 17.5.5：过滤器被 R8 混淆成 `jn7`（日志 tag 仍是 OplusRecentTasksFilter）；
        // 17.3.12 是 `ko.a`。C16 用未混淆的类名。
        val candidates = if (c17) arrayOf("jn7", "ko.a") else FILTER_CANDIDATES
        val found = candidates.map { it to runCatching { Rt.cls(cl, it) }.getOrNull() }
        dbg(
            "install: c17=" + c17 +
                " loader=" + cl.javaClass.name +
                " grouped=" + (grpCls != null) +
                " 候选=" + found.joinToString(",") { it.first + "=" + (it.second != null) }
        )

        // 预取方法，hook 里直接 invoke（避免每条记录都重新做一次方法查找）
        //   C16: getTaskInfo1/getTaskInfo2
        //   C17 17.3.12: e()/f()
        //   C17 17.5.5: c()/d()（都返回 TaskInfo；d() 不再是 SplitBounds）
        if (grpCls != null) {
            taskInfo1M = firstMethod(grpCls, "getTaskInfo1", "e", "c")
            taskInfo2M = firstMethod(grpCls, "getTaskInfo2", "f", "d")
            if (c17) {
                // 17.3.12: d() -> SplitBounds；17.5.5: 字段 t -> SplitBounds + int 字段 r（4=mixed）
                splitM = firstMethodReturning(grpCls, "SplitBounds")
                splitF = findFieldByType(grpCls, "SplitBounds")
                typeF = findFieldByName(grpCls, "r")
            } else {
                // ★ C16 保持原行为：只按老的 d() 取（不存在就是 null），不引入新字段
                splitM = firstMethod(grpCls, "d")
            }
        }
        runCatching {
            val tu = Rt.cls(cl, CLS_TASKUTILS) ?: return@runCatching
            isFreeformM = tu.declaredMethods.firstOrNull {
                it.name == "isFlexibleFloatingWindow" && it.parameterCount == 1
            }?.also { runCatching { it.isAccessible = true } }
        }
        // ColorOS 窗口服务（惰性解析，见 ensureFlex）
        flexCl = cl

        var n = 0
        for ((name, k) in found) {
            if (k == null) continue
            val target = pickFilterMethod(k)
            if (target == null) {
                dbg("install: " + name + " 里没有 (II,GroupedTaskInfo,List)->boolean")
                continue
            }
            runCatching { target.isAccessible = true }
            val cb = object : XMH() {
                override fun beforeHookedMethod(param: XMP) = filter(param)
            }
            if (XBridge.hookMethod(target, cb) != null) {
                n++
                dbg("install: 已挂准 " + name + "#" + target.name)
            }
        }
        Log.i(TAG, "install: hooked=$n")
        dbg(
            "install hooked=" + n +
                " taskInfo1=" + (taskInfo1M != null) +
                " taskInfo2=" + (taskInfo2M != null) +
                " taskUtils=" + (isFreeformM != null) +
                " split=" + (splitM != null)
        )
        if (n > 0) {
            installedPid = pid
            installed = true
        }
        return n > 0
    }

    /** 沿继承链找「无参、返回类型名以 typeHint 结尾」的方法（17.3.12 的 d()->SplitBounds） */
    private fun firstMethodReturning(c: Class<*>, typeHint: String): java.lang.reflect.Method? {
        var k: Class<*>? = c
        while (k != null && k != Any::class.java) {
            runCatching { k!!.declaredMethods }.getOrDefault(emptyArray())
                .firstOrNull { it.parameterCount == 0 && it.returnType.name.endsWith(typeHint) }?.let {
                    runCatching { it.isAccessible = true }
                    return it
                }
            k = k.superclass
        }
        return null
    }

    /** 按字段类型名找字段（17.5.5 的 GroupedTaskInfo.t -> SplitBounds） */
    private fun findFieldByType(c: Class<*>, typeHint: String): java.lang.reflect.Field? {
        var k: Class<*>? = c
        while (k != null && k != Any::class.java) {
            runCatching { k!!.declaredFields }.getOrDefault(emptyArray())
                .firstOrNull { it.type.name.endsWith(typeHint) }?.let {
                    runCatching { it.isAccessible = true }
                    return it
                }
            k = k.superclass
        }
        return null
    }

    /** 按名字找字段（17.5.5 的 GroupedTaskInfo.r） */
    private fun findFieldByName(c: Class<*>, name: String): java.lang.reflect.Field? {
        var k: Class<*>? = c
        while (k != null && k != Any::class.java) {
            runCatching { k!!.getDeclaredField(name) }.getOrNull()?.let {
                runCatching { it.isAccessible = true }
                return it
            }
            k = k.superclass
        }
        return null
    }

    /**
     * 过滤器方法的「形状」：(int, int, GroupedTaskInfo, ArrayList&lt;String&gt;) -> boolean。
     * **不看方法名**（C16 叫 filterTaskInfo，C17 17.5.5 混淆成 b），只看签名形状。
     */
    private fun pickFilterMethod(k: Class<*>): java.lang.reflect.Method? =
        runCatching { k.declaredMethods }.getOrDefault(emptyArray()).firstOrNull { m ->
            m.returnType == java.lang.Boolean.TYPE &&
                m.parameterCount == 4 &&
                m.parameterTypes[0] == Integer.TYPE &&
                m.parameterTypes[1] == Integer.TYPE &&
                java.util.List::class.java.isAssignableFrom(m.parameterTypes[3])
        }

    /** 按「方法名候选」沿继承链找无参方法（C17 的名字都是单字母，容错靠这里） */
    private fun firstMethod(c: Class<*>, vararg names: String): java.lang.reflect.Method? {
        var k: Class<*>? = c
        while (k != null && k != Any::class.java) {
            for (nm in names) {
                runCatching { k!!.declaredMethods }.getOrDefault(emptyArray())
                    .firstOrNull { it.name == nm && it.parameterCount == 0 }?.let {
                        runCatching { it.isAccessible = true }
                        return it
                    }
            }
            k = k.superclass
        }
        return null
    }

    /**
     * 过滤主体：只有「非分屏 + 小窗状态」才吃掉。判定顺序（每一步都有理由）：
     * 1. `args[2]`（GroupedTaskInfo）为空 → 不是一条正常记录，不碰；
     * 2. `getTaskInfo2() != null` = 分屏组 → 不碰（分屏的过滤原厂另有逻辑）；
     * 3. `getTaskInfo1()` 为空 → 没有主任务，不碰；
     * 4. 该任务是 flexible/floating 小窗 → 返回 true（这条不进列表）。
     */
    private fun filter(param: XMP) {
        try {
            val on = enabled()
            val grouped = param.argOrNull(2)
            // C17 分屏判据：17.3.12 是 d() -> SplitBounds；17.5.5 是字段 t + 类型 r==4
            val splitB = splitM?.let { runCatching { it.invoke(grouped) }.getOrNull() }
                ?: splitF?.let { runCatching { it.get(grouped) }.getOrNull() }
            val type = typeF?.let { runCatching { it.getInt(grouped) }.getOrNull() }
            val second = taskInfo2M?.let { runCatching { it.invoke(grouped) }.getOrNull() }
            val task = if (second == null) {
                taskInfo1M?.let { runCatching { it.invoke(grouped) }.getOrNull() }
            } else null
            val info = if (task != null) freeform(task) else null
            // C17 诊断：每条记录一行 bundle 摘要（覆盖整个列表，不受"前 N 条"限制）
            if (canLogCompact()) {
                if (task != null) {
                    val tid = runCatching { task.javaClass.getMethod("getTaskId").invoke(task) }.getOrNull()
                    log1(
                        "row taskId=" + tid + " wm=" + windowingMode(task) +
                            " flex=" + scanFlexDiag(task) + " bundle=" + bundleDesc(task)
                    )
                }
                if (second != null) {
                    val tid2 = runCatching { second.javaClass.getMethod("getTaskId").invoke(second) }.getOrNull()
                    log1(
                        "row2 taskId=" + tid2 + " wm=" + windowingMode(second) +
                            " flex=" + scanFlexDiag(second) + " bundle=" + bundleDesc(second)
                    )
                }
            }
            // C17 判据未定：每 15 秒最多 dump 12 组（含 mOplusExtraBundle / configuration）。
            if (canDump() && grouped != null) {
                dumpGroup(grouped)
                if (task != null) dumpTask(task)
            }
            dbg(
                "filter on=" + on + " grp=" + (grouped != null) +
                    " split=" + (second != null || splitB != null || type == 4) +
                    " splitB=" + (splitB != null) + " type=" + type +
                    " task=" + (task != null) +
                    " ff=" + (info?.first ?: false) + " by=" + (info?.second ?: "-") +
                    " wm=" + (info?.third ?: -1)
            )
            if (!on) return
            if (grouped == null) return
            if (second != null || splitB != null || type == 4) return
            if (task == null) return
            if (info?.first != true) return
            // 命中就记一行（不占 debug 配额）：以后「多任务看不到某应用」时，
            // 能直接确认是不是过滤器吃掉的，以及命中的是哪种状态。
            val hitTid = runCatching {
                task.javaClass.getMethod("getTaskId").invoke(task)
            }.getOrNull()
            log1("HIT taskId=" + hitTid + " by=" + info.second)
            param.result = java.lang.Boolean.TRUE
            param.returnEarly = true
        } catch (t: Throwable) {
            Log.i(TAG, "filter error: " + t)
        }
    }

    /** TaskInfo 是不是「迷你窗」状态；返回 (结论, 用了哪条判据/诊断, windowingMode) */
    private fun freeform(task: Any): Triple<Boolean, String, Int> {
        val wm = windowingMode(task)
        // ⓪ C17：**只认** `TaskInfo.mOplusExtraBundle.key_flexible_task_state` 里的迷你窗状态。
        //    2/5/8 = 迷你窗；1 = 正常小窗、3 = 悬浮、4 = 手动缩放，都不能命中。
        //    ⚠️ 不能继续往下走 flexStateOf/wmName/scanFlex 兜底：
        //    ① getFlexibleWindowState() 对普通小窗也返回 1；② VisualBounds 对普通小窗
        //    同样是 visual < real。旧代码就是在这里把正常小窗一起隐藏的。
        if (c17Mode) {
            val hit = oplusBundleHit(task)
            if (hit != null) return Triple(true, hit, wm)
            return Triple(false, "c17:notMini", wm)
        }
        // ① 桌面自己用的判据（注意：它内部先看一个 lazy 开关，false 就直接返回 false，
        //    所以单靠它可能永远判不出来 —— 实机日志里 11 条全是 false；C17 这个类还没了）
        if (isFreeformM?.let { runCatching { it.invoke(null, task) as? Boolean }.getOrNull() } == true) {
            return Triple(true, "taskUtils", wm)
        }
        // ② 直接问 ColorOS 窗口服务：getFlexibleWindowState(taskInfo) == 1
        val st = flexStateOf(task)
        if (st == 1) return Triple(true, "flexMgr", wm)
        // ③ 老写法：windowingMode 是不是 FREEFORM(5)
        if (wm == 5) return Triple(true, "wm5", wm)
        // ④ C16：windowingMode 可能换了写法，这里再看名字和 flex 字段
        val wmName = windowingModeName(task)
        val low = wmName.lowercase()
        if (low.contains("free") || low.contains("float") || low.contains("multi") ||
            low.contains("small") || low.contains("zoom")
        ) return Triple(true, "wmName:" + wmName, wm)
        scanFlexHit(task)?.let { return Triple(true, it, wm) }
        return Triple(
            false,
            "st=" + st + (if (flexStateM == null) "/nom" else "") +
                " wmName=" + wmName + " scan=" + (scanFlexDiag(task) ?: "-"),
            wm
        )
    }

    /**
     * C17：读 `TaskInfo.mOplusExtraBundle` 判断**迷你窗**。返回命中的判据字符串，null = 不隐藏。
     *
     * 取值实证（`OplusZoomWindowManager$ZoomConstant`）：
     *   1=正常小窗(ZOOM_STATE)  2=迷你窗(ZOOM_MINI_STATE)  3=悬浮  4=手动缩放
     *   5=贴边迷你(SUPER_MINI)  8=迷你窗稳定态(READY)      9=分屏
     *
     * ⚠️ 不能再用 VisualBounds < RealBounds 兜底：正常小窗同样是“视觉框小于实际框”，
     *    那样会把普通小窗的卡片也隐藏掉。这里只认迷你窗状态。
     */
    private fun oplusBundleHit(task: Any): String? = runCatching {
        val b = Rt.get(task, "mOplusExtraBundle") as? android.os.Bundle ?: return null
        val state = runCatching { b.getInt("key_flexible_task_state", 0) }.getOrDefault(0)
        if (state == 2 || state == 5 || state == 8) return "extra:miniState=" + state
        null
    }.getOrNull()

    /** C17 诊断：把这一组里**所有** TaskInfo 的关键字段打出来（含 flex 相关字段） */
    private fun dumpGroup(grouped: Any?) {
        if (grouped == null) return
        runCatching {
            log1("group cls=" + grouped.javaClass.name)
            var k: Class<*>? = grouped.javaClass
            var d = 0
            while (k != null && k != Any::class.java && d < 3) {
                for (f in k.declaredFields) {
                    runCatching { f.isAccessible = true }
                    val v = runCatching { f.get(grouped) }.getOrNull() ?: continue
                    when (v) {
                        is List<*> -> {
                            log1("  " + f.name + "[list " + v.size + "]")
                            v.forEachIndexed { i, t ->
                                if (i < 6 && t != null) log1("    " + i + ") " + taskDesc(t))
                            }
                        }
                        is Array<*> -> {
                            log1("  " + f.name + "[array " + v.size + "]")
                            v.forEachIndexed { i, t ->
                                if (i < 6 && t != null) log1("    " + i + ") " + taskDesc(t))
                            }
                        }
                        is Int, is Boolean, is String -> log1("  " + f.name + "=" + v)
                        is Enum<*> -> log1("  " + f.name + "=" + v.name)
                        else -> {}
                    }
                }
                k = k.superclass
                d++
            }
        }
    }

    /** 单个 TaskInfo 的关键信息（C17 定判据用） */
    private fun taskDesc(t: Any): String = runCatching {
        t.javaClass.simpleName +
            " taskId=" + runCatching { t.javaClass.getMethod("getTaskId").invoke(t) }.getOrNull() +
            " wm=" + windowingMode(t) +
            " wmName=" + windowingModeName(t) +
            " flex=" + scanFlexDiag(t) +
            " extra=" + bundleDesc(t) +
            " desc=" + t.toString().take(400)
    }.getOrDefault("?")

    /** `TaskInfo.mOplusExtraBundle`（OPLUS 私有 Bundle）拼成 key=value 一行 */
    private fun bundleDesc(t: Any): String = runCatching {
        val b = Rt.get(t, "mOplusExtraBundle") as? android.os.Bundle ?: return ""
        b.keySet().joinToString(",") { k ->
            k + "=" + runCatching { b.get(k) }.getOrNull()
        }
    }.getOrDefault("")

    /** C17 诊断：一条 TaskInfo 的完整现场（toString + OPLUS bundle + windowConfiguration） */
    private fun dumpTask(t: Any) {
        runCatching {
            log1("  TASK " + t.toString().take(700))
            val b = Rt.get(t, "mOplusExtraBundle") as? android.os.Bundle
            if (b != null) {
                for (k in b.keySet()) {
                    log1("    extra " + k + "=" + runCatching { b.get(k) }.getOrNull())
                }
            }
            val cfg = runCatching { t.javaClass.getMethod("getConfiguration").invoke(t) }.getOrNull()
            val wc = cfg?.let { Rt.get(it, "windowConfiguration") }
            if (wc != null) {
                log1(
                    "    wc wm=" + Rt.callNoArg(wc, "getWindowingMode") +
                        " bounds=" + Rt.callNoArg(wc, "getBounds") +
                        " appBounds=" + Rt.callNoArg(wc, "getAppBounds")
                )
            }
        }
    }

    /** `getFlexibleWindowState` 的几种可能签名都试一遍；异常只记一次 */
    private var flexErrLogged = false

    private fun flexStateOf(task: Any): Int? {
        ensureFlex()
        val m = flexStateM ?: return null
        // 方式 1：直接传 TaskInfo
        try {
            return (m.invoke(flexInst, task) as? Number)?.toInt()
        } catch (t: Throwable) {
            if (!flexErrLogged) {
                flexErrLogged = true
                log1("flex invoke(TaskInfo) 异常: " + t)
            }
        }
        // 方式 2：传 taskId（个别版本签名是 int）
        val tid = runCatching { task.javaClass.getMethod("getTaskId").invoke(task) as? Int }.getOrNull()
        if (tid != null) {
            try {
                return (m.invoke(flexInst, tid) as? Number)?.toInt()
            } catch (t: Throwable) {
                if (!flexErrLogged) {
                    flexErrLogged = true
                    log1("flex invoke(int) 异常: " + t)
                }
            }
        }
        return null
    }

    /**
     * C17：`windowingMode` 可能是枚举 / 字符串而不是 int。这里拿到它的「名字」用于判定与日志。
     */
    private val wmNameM = HashMap<Class<*>, java.lang.reflect.Method?>()

    private fun windowingModeName(task: Any): String = runCatching {
        val k = task.javaClass
        val g = synchronized(wmNameM) {
            wmNameM.getOrPut(k) { runCatching { k.getMethod("getWindowingMode") }.getOrNull() }
        } ?: return ""
        when (val v = g.invoke(task)) {
            null -> ""
            is Int -> v.toString()
            is Enum<*> -> v.name
            else -> v.toString()
        }
    }.getOrDefault("")

    /** 在 TaskInfo 对象里找「flexible / freeform / zoom / smallwindow」相关字段，命中返回字段名 */
    private fun scanFlexHit(task: Any): String? = runCatching {
        var k: Class<*>? = task.javaClass
        var depth = 0
        while (k != null && k != Any::class.java && depth < 6) {
            for (f in k.declaredFields) {
                val n = f.name.lowercase()
                if (!(n.contains("flex") || n.contains("freeform") || n.contains("zoom") ||
                        n.contains("smallwindow"))
                ) continue
                runCatching { f.isAccessible = true }
                val v = runCatching { f.get(task) }.getOrNull() ?: continue
                val on = when (v) {
                    is Boolean -> v
                    is Int -> v != 0
                    is String -> v.lowercase().contains("free") || v.lowercase().contains("float")
                    is Enum<*> -> v.name.lowercase().let {
                        it.contains("free") || it.contains("float") || it.contains("small")
                    }
                    else -> false
                }
                if (on) return "field:" + f.name
            }
            k = k.superclass
            depth++
        }
        null
    }.getOrNull()

    /** 在 TaskInfo 里找「无参、返回 boolean、名字带 flex/freeform/zoom/small」的方法，命中返回方法名 */
    private fun scanFlexMethod(task: Any): String? = runCatching {
        var k: Class<*>? = task.javaClass
        var depth = 0
        while (k != null && k != Any::class.java && depth < 6) {
            for (m in runCatching { k!!.declaredMethods }.getOrDefault(emptyArray())) {
                if (m.parameterCount != 0 || m.returnType != java.lang.Boolean.TYPE) continue
                val n = m.name.lowercase()
                if (!(n.contains("flex") || n.contains("freeform") || n.contains("zoom") ||
                        n.contains("smallwindow"))
                ) continue
                runCatching { m.isAccessible = true }
                if (runCatching { m.invoke(task) as? Boolean }.getOrNull() == true) {
                    return "method:" + m.name
                }
            }
            k = k.superclass
            depth++
        }
        null
    }.getOrNull()

    /** 诊断用：把扫到的 flex 相关字段值拼成一行（前几条记录才打） */
    private fun scanFlexDiag(task: Any): String? = runCatching {
        val sb = StringBuilder()
        var k: Class<*>? = task.javaClass
        var depth = 0
        while (k != null && k != Any::class.java && depth < 4 && sb.length < 120) {
            for (f in k.declaredFields) {
                val n = f.name.lowercase()
                if (!(n.contains("flex") || n.contains("freeform") || n.contains("zoom"))) continue
                runCatching { f.isAccessible = true }
                val v = runCatching { f.get(task) }.getOrNull() ?: continue
                if (sb.isNotEmpty()) sb.append(',')
                sb.append(f.name).append('=').append(v)
            }
            k = k.superclass
            depth++
        }
        if (sb.isEmpty()) null else sb.toString()
    }.getOrNull()

    private val cfgM = HashMap<Class<*>, java.lang.reflect.Method?>()
    private val wcF = HashMap<Class<*>, java.lang.reflect.Field?>()
    private val wmM = HashMap<Class<*>, java.lang.reflect.Method?>()

    /** TaskInfo.configuration.windowConfiguration.windowingMode（拿不到返回 -1） */
    private fun windowingMode(task: Any): Int = runCatching {
        val k = task.javaClass
        val gcfg = synchronized(cfgM) {
            cfgM.getOrPut(k) { runCatching { k.getMethod("getConfiguration") }.getOrNull() }
        } ?: return -1
        val cfg = gcfg.invoke(task) ?: return -1
        val ck = cfg.javaClass
        val fwc = synchronized(wcF) {
            wcF.getOrPut(ck) {
                runCatching { ck.getField("windowConfiguration").also { it.isAccessible = true } }.getOrNull()
            }
        } ?: return -1
        val wc = fwc.get(cfg) ?: return -1
        val wk = wc.javaClass
        val gwm = synchronized(wmM) {
            wmM.getOrPut(wk) { runCatching { wk.getMethod("getWindowingMode") }.getOrNull() }
        } ?: return -1
        (gwm.invoke(wc) as? Int) ?: -1
    }.getOrDefault(-1)

    /** 开关（800ms 缓存，避免每条记录都读一次 Settings） */
    private fun enabled(): Boolean {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - onAt < 800L) return onCache
        onAt = now
        // 桌面进程拿不到 system_server 的 Context，必须传桌面自己的（FlyGate 抓到的那个）
        onCache = runCatching { Cfg.recentsHideFreeform(FlyGate.reportCtx()) }.getOrDefault(false)
        return onCache
    }
}
