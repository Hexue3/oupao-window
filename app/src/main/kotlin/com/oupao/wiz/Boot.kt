package com.oupao.wiz

import android.os.Process

/**
 * 按包名分发 hook 安装。对应原 smali 的 com.oupao.wiz.Boot。
 *
 * 模块作用域 6 个包，真正装 hook 的是其中 4 个：
 *   android                    → system_server：装绝大部分功能
 *   com.android.systemui       → 通知抬头（HeadsUp）+ 小窗状态同步
 *   com.coloros.smartsidebar   → 侧边栏
 *   com.android.launcher       → 扇形菜单手势闸门（FlyGate，2709 新增）
 * 其余（via / heytap 浏览器）走 Web.tryHook，不在这里。
 *
 * 重要：主开关关闭时**仍然会走完分发**，只是跳过功能安装、只留通信通道。
 * 这样用户在 App 里重新打开开关后不必重启系统。
 */
object Boot {

    private const val PKG_SYSTEM = "android"
    private const val PKG_SYSTEMUI = "com.android.systemui"
    private const val PKG_SIDEBAR = "com.coloros.smartsidebar"
    private const val PKG_LAUNCHER = "com.android.launcher"
private const val PKG_NCM = "com.oplus.notificationmanager"

    /** 功能钩子是否已安装（幂等标记） */
    @Volatile
    @JvmField
    var featuresInstalled = false

    /**
     * 1820fix: 记录"功能钩子装在哪个进程"。
     * 原来只有一个全局布尔位，而 LSPosed 会在 systemui 等进程里也回调 pkg="android"，
     * 于是 systemui 先把 featuresInstalled 置 true，真正 system_server 进来时直接 return
     * → system_server 里一个钩子都没装 → 模块表现为"完全不生效"。
     */
    @Volatile
    @JvmField
    var featuresInstalledIn: String = ""

    /** 裸日志（不依赖 Cfg/SLog 门控） */
        @JvmStatic
    fun logRaw(msg: String) = raw(msg)

    private fun raw(msg: String) {
        try {
            android.util.Log.i("OupaoBoot", msg)
        } catch (_: Throwable) {
        }
    }

    @JvmStatic
    fun dispatch(lp: LPP) {
        raw("Boot.dispatch pkg=" + lp.packageName + " proc=" + lp.processName +
            " realProc=" + currentProcessName())
        shadow(lp)
        val pkg = lp.packageName
        val cl = lp.classLoader
        val proc = lp.processName

        runCatching {
            when (pkg) {
                // 1820fix: pkg="android" 只有在真正的 system_server 进程里才装。
                // LSPosed 对 scope 里的 "android" 包会在每个进程回调（systemui 也会），
                // 原来只按包名判断 → 在 systemui 里装整套 system_server 钩子，
                // 并污染 Hooks.CL / featuresInstalled，导致真正的 system_server 反而什么都不装。
                PKG_SYSTEM -> {
                    val p = proc.ifEmpty { currentProcessName() }
                    if (isSystemServer(p)) {
                        installSystemServer(cl)
                    } else {
                        SLog.k("oupao: 跳过 android 钩子（当前进程=" + p + "，非 system_server）")
                    }
                }
                PKG_SYSTEMUI -> installSystemUi(cl)
                PKG_SIDEBAR -> {
                    val b = XBridge.hookCount
                    Side.install(cl)
                    Caps.report("sidebar", XBridge.hookCount > b)
                }
                PKG_LAUNCHER -> installLauncher(cl)
                // ColorOS「通知与控制中心」设置页（com.oplus.notificationmanager，:ui 进程）
                PKG_NCM -> installNotifCenter(cl)
                // 其它应用进程：不装任何钩子。
                // ★ 但 SystemUI 进程要按**进程名**兜底：LSPosed 在这个进程里报的 pkg
                //   经常是进程里第一个被加载的包（实测报过 `com.oplus.keyguard.clock.base`），
                //   于是走不到 PKG_SYSTEMUI 分支 → SystemUI 侧钩子（横幅/通知小窗/侧滑闸门）全丢。
                else -> {
                    val p = proc.ifEmpty { currentProcessName() }
                    if (p.startsWith(PKG_SYSTEMUI)) {
                        SLog.k("oupao: SystemUI 走了进程名兜底（pkg=" + pkg + "）")
                        installSystemUi(cl)
                    }
                }
            }
        }.onFailure { SLog.e("oupao: handleLoadPackage 异常", it) }
    }

    // ---------- system_server ----------

    private fun installSystemServer(cl: ClassLoader?) {
        // 1820fix: 只有真的 system_server 才允许写 Hooks.CL / 装功能钩子
        val proc = currentProcessName()
        raw("installSystemServer proc=" + proc + " cl=" + (cl != null))
        if (proc.isNotEmpty() && !isSystemServer(proc)) {
            SLog.k("oupao: installSystemServer 被非 system_server 进程调用（" + proc + "），已忽略")
            return
        }
        Hooks.CL = cl
        val pid = Process.myPid().toString()
        SLog.i("oupao: system_server 已加载 b" + Diag.BUILD + " pid=" + pid)

        // 3206：开机就把侧边栏占用区清掉（开关开着的话），
        // 让系统原生的边缘侧滑返回覆盖那一段。
        // 3207：开机那一刻 sysContext/prefs 往往还没就绪，所以再补两次。
        runCatching { Cfg.applySidebarRegion() }
        val bh = Hooks.main()
        if (bh != null) {
            for (d in longArrayOf(4000L, 12000L, 30000L)) {
                runCatching {
                    bh.postDelayed({ runCatching { Cfg.applySidebarRegion() } }, d)
                }
            }
        }

        val masterVal = Cfg.getStr(Cfg.K_EGG_MASTER, "1")
        Diag.put("Master", masterVal)

        if (!Cfg.masterOn()) {
            // 主开关关闭：只保留通信通道，等用户重新打开后无需重启
            SLog.i("oupao: 主开关(启用功能)值=$masterVal, 跳过功能安装, 仅装通道")
        } else {
            installFeatures(cl)
        }
        // 2829：能走到这里就说明模块在 system_server 里跑起来了（能力位 "sys"）
        Caps.report("sys", true)

        // 3502：重启后清空多任务列表（开关在 FlymeUI 页；线程内部等 boot_completed）
        runCatching { RecentsClearBoot.schedule() }
            .onFailure { SLog.e("oupao: RecentsClearBoot 排程失败", it) }

        // 1820fix：通信通道 + 更新检查各自兜异常，绝不能被功能安装的异常带崩。
        // 原实现把这两步放在最后且不在 try 里 —— 一旦 installFeatures 抛出，
        // 模块就彻底没有广播通道，表现就是"模块完全不生效"。
        runCatching { UChk.go() }.onFailure { SLog.e("oupao: UChk.go 失败", it) }
        runCatching { Rx.install() }.onFailure { SLog.e("oupao: Rx.install 失败", it) }

        summary(PKG_SYSTEM)
        SLog.i(Cfg.masterProbe())
    }

    /**
     * 后台线程安装功能钩子。
     *
     * 钩子安装要做大量反射（找类 + 注册几十个 hook），放在 system_server
     * 主线程上会拖慢它、甚至触发 ANR，所以一律丢到独立线程。
     */
    /** 调试用：清掉幂等标记并立刻重装功能钩子（配合广播 com.oupao.wiz.REINSTALL） */
    @JvmStatic
    fun forceReinstall() {
        raw("forceReinstall：清除标记并重装")
        featuresInstalled = false
        featuresInstalledIn = ""
        Hooks.sysDone = false   // 允许 installSystem 重新走一遍（调试用）
        installFeatures(Hooks.CL)
    }

    @JvmStatic
    fun installFeaturesAsync() {
        raw("iFA 进入 featuresInstalled=" + featuresInstalled)
        if (featuresInstalled) return
        runCatching {
            val t = Thread({ installFeatures() }, "Oupao-InstallFeatures")
            t.isDaemon = true
            t.start()
        }
    }

    /**
     * 按需安装功能钩子（幂等）。
     *
     * 原版是「开机时看主开关决定装不装」，用户改完开关必须重启手机；
     * 这里改成「通道常驻 + 主开关打开时立刻补装」：
     *   - Hooks.installSystem 自带 sysDone 锁，重复调用安全；
     *   - Hooks.CL 只在 system_server 里赋值，其他进程直接跳过。
     *
     * 1820fix：每个 installer 独立兜异常 + 逐个记日志。
     * 原实现六个 installer 共用一个大 runCatching，第一个抛异常就会让后面全部不装，
     * 真机表现就是"模块完全不生效"，而日志里只有一句笼统的失败，无法定位。
     */
    @JvmStatic
    fun installFeatures(cl: ClassLoader? = Hooks.CL) {
        raw("iF 进入 cl=" + (cl != null) + " HooksCL=" + (Hooks.CL != null) +
            " featuresInstalled=" + featuresInstalled + " in=" + featuresInstalledIn)
        val loader = cl ?: run { raw("iF 早退：loader=null"); return }
        val proc = currentProcessName()
        // 1820fix: 幂等标记按进程隔离；且非 system_server 进程一律不装
        if (proc.isNotEmpty() && !isSystemServer(proc)) {
            SLog.k("oupao: installFeatures 跳过（非 system_server：" + proc + "）")
            return
        }
        if (featuresInstalled && featuresInstalledIn == proc) { raw("iF 早退：已装过 proc=" + proc); return }

        val okNames = ArrayList<String>()

        fun one(name: String, body: () -> Unit) {
            val before = XBridge.hookCount
            try {
                body()
                okNames.add(name)
                val n = XBridge.hookCount - before
                // 2829：真的挂上了 hook 才算这套系统支持这个功能（见 Caps）
                Caps.report(name.lowercase(), n > 0)
                SLog.k("oupao: installer ok -> " + name + " hooks=" + n)
            } catch (e: Throwable) {
                Caps.report(name.lowercase(), false)
                SLog.e("oupao: installer FAIL -> " + name, e)
            }
        }

        raw("installFeatures 开始 proc=" + proc)
        one("Hooks") { Hooks.installSystem(loader) }
        one("Mute") { Mute.install(loader) }
        one("Dock") { Dock.install(loader) }
        one("Zoom") { Zoom.install(loader) }
        one("FlyH") { FlyH.install(loader) }
        one("FlyB") { FlyB.install(loader) }
        // 让「不可调整大小」的 app 也能进 ColorOS 小窗（目前只为翻译应用放开）
        one("FlexFix") { com.oupao.wiz.FlexFix.install(loader) }
        // 3242：迷你窗位置记忆（MiniPos）**已移除** —— 无法稳定拦截迷你动画入口，
        //   保留代码文件 MiniPos.java 供以后参考，但不再安装。

        runCatching {
            // 把主开关值同步到系统侧，供 SystemUI/侧边栏进程读取
            Cfg.writeStr("/data/system/oupao_cfg/" + Cfg.K_EGG_MASTER, Cfg.getStr(Cfg.K_EGG_MASTER, "1"))
        }
        // 3288：一次性清掉已废弃的配置键文件（系统侧由 system_server 删，App 侧由 App 进程删）
        runCatching { LegacyPurge.run() }

        // 跳转确认弹窗的关闭只能在 App 进程做（system_server 读写 Settings.Global
        // 和执行 su 都会被拒），见 JumpConfirm；这里不再调用。

        featuresInstalled = true
        featuresInstalledIn = proc
        SLog.k("oupao: 功能钩子安装完成 proc=" + proc + " ok=" + okNames.size + "/8 -> " + okNames)
        raw("installFeatures 完成 ok=" + okNames.size + "/8 " + okNames)
    }

    // ---------- SystemUI ----------

    /** SystemUI 钩子只装一次（进程名兜底可能让 dispatch 进来多次） */
    @Volatile
    private var systemUiDone = false

    private fun installSystemUi(cl: ClassLoader?) {
        if (systemUiDone) return
        systemUiDone = true
        val pid = Process.myPid().toString()
        Diag.put("SystemUI", "1")
        raw("oupao: systemui 已加载 b" + Diag.BUILD + " pid=" + pid)

        // 通知抬头拦截（用于长按小窗静音、气泡等）
        var b = XBridge.hookCount
        runCatching { Hooks.hookHeadsUp(cl) }.onFailure { SLog.e("oupao: hookHeadsUp 失败", it) }
        runCatching { Hooks.hookHeadsUp2(cl) }.onFailure { SLog.e("oupao: hookHeadsUp2 失败", it) }
        // 简洁横幅（ColorOS 的 Simple Banner，不走 HeadsUpManager）也要能屏蔽
        runCatching { Hooks.hookSimpleBanner(cl) }.onFailure { SLog.e("oupao: hookSimpleBanner 失败", it) }
        // 2829：SystemUI 侧能力位（通知抬头/横幅/气泡都挂在这）
        Caps.report("ui", XBridge.hookCount > b)
        // 通知点击 → 小窗打开（改 ActivityOptions，让系统按缩放窗启动）
        b = XBridge.hookCount
        runCatching { NotifFly.install(cl) }.onFailure { SLog.e("oupao: NotifFly.install 失败", it) }
        Caps.report("notif", XBridge.hookCount > b)
        // 2909：系统手势小白条 —— 扇形菜单出现时藏起来，别让它跟着手势动
        runCatching { NavPill.install(cl) }.onFailure { SLog.e("oupao: NavPill.install 失败", it) }
        // 2954：控制中心（磁贴）收面板的**动效** —— system_server 发广播过来，
        //   由这里调 ROM 自己的 OplusSeparateQSPluginImpl.collapseQSPanel(anim=true)。
        b = XBridge.hookCount
        runCatching { QsCollapse.install(cl) }.onFailure { SLog.e("oupao: QsCollapse.install 失败", it) }
        Caps.report("qs", XBridge.hookCount > b)
        // 2906：TileFly（跨进程收面板）已停用 —— 2904 实测会把系统搞卡死
        // 小窗状态同步
        runCatching { FlyS.install(cl) }.onFailure { SLog.e("oupao: FlyS.install 失败", it) }
        // 旁听 SystemUI 的侧滑/角落手势流，驱动自绘扇形菜单（只读不拦，见 FlySideHook）
        b = XBridge.hookCount
        runCatching { FlySideHook.install(cl) }.onFailure { SLog.e("oupao: FlySideHook.install 失败", it) }
        // 2829：扇形菜单能不能呼出，取决于这里有没有旁听到手势
        Caps.report("fly", XBridge.hookCount > b)

        // 3430：面板（通知中心 / 控制中心）展开信号 —— 扇形菜单停留时靠它自动收起
        b = XBridge.hookCount
        runCatching { ShadeWatch.install(cl) }.onFailure { SLog.e("oupao: ShadeWatch.install 失败", it) }
        Caps.report("shade", XBridge.hookCount > b)

        // 新增：锁屏数字键盘滑动输入（COUINumericKeyboard，只在 SystemUI 进程里）
        b = XBridge.hookCount
        runCatching { KeyguardSlide.install(cl) }.onFailure { SLog.e("oupao: KeyguardSlide.install 失败", it) }
        Caps.report("slide", XBridge.hookCount > b)

        // 新增：Flyme 式「通知收纳」（长按通知菜单 + 头部入口 + 通知管线过滤，全在 SystemUI 进程）
        b = XBridge.hookCount
        runCatching { NotifBox.install(cl) }.onFailure { SLog.e("oupao: NotifBox.install 失败", it) }
        Caps.report("notifbox", XBridge.hookCount > b)

        // 新增：长按电源键 → 关机界面里的「软重启」按钮（自绘在 OplusShutdownView 上）
        b = XBridge.hookCount
        runCatching { PowerMenu.install(cl) }.onFailure { SLog.e("oupao: PowerMenu.install 失败", it) }
        Caps.report("power", XBridge.hookCount > b)

        // 扇形菜单热区窗口（SystemUI 进程）。
        // ★ 2714/2716 实测（重要，别再重复）：
        //   - `type=2024 + SPY`：窗口建得出来，但 `onTouchEvent` 一次都不触发（收不到触摸）；
        //   - 去掉 SPY 改成"消费型 + 点击重放"：触摸确实被窗口吃掉（下层也点不到），
        //     但 View 同样收不到事件，重放根本没机会执行 → 热区变成死区。
        //   → 结论：ColorOS 16 上从 SystemUI 加系统层窗口抢这一小片触摸这条路走不通。
        //     先保持不建窗口（= 2705 的原始行为：纯点击正常，带位移的点击由系统侧滑区决定）。
        // runCatching { FlyZoneUi.install(cl) }.onFailure { SLog.e("oupao: FlyZoneUi.install 失败", it) }
    }

    // ---------- ColorOS 通知与控制中心（设置页入口） ----------

    /** 该进程只装一个：在「通知与控制中心」页插「通知收纳」一行（见 NotifBoxNc.kt） */
    @Volatile
    private var notifCenterDone = false

    private fun installNotifCenter(cl: ClassLoader?) {
        if (notifCenterDone) return
        notifCenterDone = true
        val b = XBridge.hookCount
        runCatching { NotifBoxNc.install(cl) }.onFailure { SLog.e("oupao: NotifBoxNc.install 失败", it) }
        Caps.report("notifbox_nc", XBridge.hookCount > b)
    }

    // ---------- launcher ----------
    /**
     * 桌面进程：只装一个「手势闸门」，别的一概不装。
     *
     * 底部上滑（回桌面/多任务）由 Launcher 的 TouchInteractionService 旁听输入后
     * pilfer 抢流实现，窗口层拦不住，所以必须在它进程里挡。见 [FlyGate]。
     *
     * 注意：这里**只打 logcat**（不用 SLog）—— Launcher 是普通应用进程，
     * 写不了 /data/system/oupao.log，而且它是常驻关键进程，绝不能因为日志抛异常。
     */
    private fun installLauncher(cl: ClassLoader?) {
        raw("oupao: launcher 已加载 pid=" + Process.myPid())
        val ok = runCatching { FlyGate.install(cl) }.getOrDefault(false)
        // 2829：桌面作用域没勾上 / FlyGate 装失败 → 设置页那几项显示「不支持」
        Caps.reportWith(FlyGate.reportCtx(), "launcher", ok)
        // 新增：多任务里隐藏「小窗应用」—— 多任务列表是桌面自己画的，只能在这里挂
        var b = XBridge.hookCount
        val recents = runCatching { RecentsHide.install(cl) }.getOrDefault(false)
        Caps.reportWith(FlyGate.reportCtx(), "recents", recents && XBridge.hookCount > b)
        // 3511：system_server 清完 RecentTasks 后，launcher 可能已经缓存了旧卡片。
        //   hook OplusRecentsViewImpl.applyLoadPlan，在首次应用任务列表后调
        //   dismissAllTasks（桌面原生“清空全部”），把 UI/模型里的旧卡片也清掉。
        b = XBridge.hookCount
        val recentsClear = runCatching { RecentsBootClearLauncher.install(cl) }.getOrDefault(false)
        Caps.reportWith(FlyGate.reportCtx(), "recents_clear", recentsClear && XBridge.hookCount > b)
    }

    // ---------- 诊断 ----------

    /**
     * 打印"影子"分支信息：确认模块确实被注入到了目标进程。
     * 排查"模块没生效"时，先看这里有没有输出。
     */
    @JvmStatic
    fun shadow(lp: LPP) {
        runCatching {
            val pkg = lp.packageName
            val branch = when (pkg) {
                PKG_SYSTEM -> "system"
                PKG_SYSTEMUI -> "systemui"
                PKG_SIDEBAR -> "sidebar"
                PKG_LAUNCHER -> "launcher"
                else -> "other"
            }
            SLog.i("影子: pkg=" + pkg + " 分支=" + branch + " 总开关=" + Cfg.masterOn())
        }
    }

    /**
     * 是否运行在 system_server 进程。
     * 注意：/proc/self/cmdline 里 system_server 的名字是 "system_server"，
     * 而 LSPosed 的 processName 常报 "system"，两者都要认。
     */
    @JvmStatic
    fun isSystemServer(proc: String): Boolean =
        proc.isEmpty() || proc == "system" || proc == "system_server"

    /** 当前进程名；取不到返回空串 */
    @JvmStatic
    fun currentProcessName(): String =
        runCatching {
            java.io.File("/proc/self/cmdline").readText().trim('\u0000', '\n', ' ')
        }.getOrDefault("")

    /** 启动自检摘要：进程、已装钩子数、主开关状态 */
    @JvmStatic
    fun summary(proc: String) {
        runCatching {
            val masterVal = Cfg.getStr(Cfg.K_EGG_MASTER, "?")
            SLog.i(
                "启动自检 proc=" + proc + " 钩子=" + XBridge.hookCount +
                    " 总开关=" + Cfg.masterOn() + " 主开关值=" + masterVal
            )
        }
    }
}
