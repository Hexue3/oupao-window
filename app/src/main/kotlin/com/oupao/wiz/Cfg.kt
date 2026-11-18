package com.oupao.wiz

import android.content.Context
import android.provider.Settings
import android.system.Os
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.util.concurrent.ConcurrentHashMap

/**
 * 全局配置中心。对应原 smali 的 com.oupao.wiz.Cfg（1570 行 → 本文件约 320 行）。
 *
 * 存储双通道（与原实现保持一致，顺序不可调换）：
 *   1. /data/data/com.oupao.wiz/files/cfg/<key>   —— 模块 App 侧写入（用户改设置）
 *   2. /data/system/oupao_cfg/<key>               —— 系统侧（SystemUI/sidebar 进程读取）
 * 布尔值以单字节 '0'/'1' 落盘；字符串值直接写文本。
 * 另有一条 Settings.Global 旁路（键名前缀 oupao_），用于跨进程即时下发。
 *
 * 迁移说明：原实现中每个开关 getter 都重复写
 *   if (masterOn()) { if (!flyopMaster()) return false; else return getInt(k,1)==1 } else return false
 * 本文件抽成 [flyopGate]/[sugGate]/[gameGate]/[vibGate]/[plainGate] 五个门控，
 * 语义逐条对齐，未改变任何默认值。
 */
object Cfg {

    // ---------- 配置键 ----------
    const val K_EGG_MASTER = "oupao_egg_master"
    const val K_FLYOP_MASTER = "oupao_flyop_master"
    const val K_SUG_MASTER = "oupao_sug_master"
    const val K_GAME_MASTER = "oupao_game_master"

    /** 3116：一次性把旧版本遗留的开关全部改成关闭（新安装默认关闭）。 */
    const val K_DEFAULTS_OFF_V1 = "oupao_defaults_off_v1"

    /**
     * 3140：一次性把「通知收纳」两个开关复位为关闭。
     *
     * 起因：3133–3136 调试「重启后开关保持」时把 Global/系统侧写成了 1，而
     * [notifBox] 是「Global 优先」，于是这台机器上永远显示开启，看起来像默认开。
     */
    const val K_NOTIFBOX_RESET_V1 = "oupao_notifbox_reset1"

    /** masterOn() 的 TTL 缓存时长：1s。见 [masterOn] 的注释 */
    private const val MASTER_TTL = 1000L

    const val K_DOUBLE_TAP_CLOSE = "oupao_double_tap_close"
    const val K_VIDEO_NO_PAUSE = "oupao_video_no_pause"
    const val K_LANDSCAPE_FULLSCREEN = "oupao_landscape_fullscreen"
    const val K_LONG_PRESS_MUTE = "oupao_longpress_mute"
    const val K_DOCK_KEEP_ALIVE = "oupao_dock_keep_alive"
    const val K_SCREEN_KEEP = "oupao_screen_keep"
    const val K_CAP_CLEAN = "oupao_cap_clean"
    const val K_CORNER_OFF = "oupao_corner_off"
    const val K_HIDE_ICON = "oupao_hide_icon"
    const val K_ICON_SIZE = "oupao_icon_size"

    const val K_FLY_MASTER = "oupao_fly_master"
    const val K_FLY_CLOSE = "oupao_fly_close"
    const val K_FLY_LEFT = "oupao_fly_left"
    const val K_FLY_RIGHT = "oupao_fly_right"
    const val K_FLY_MINI = "oupao_fly_mini"
    const val K_FLY_MINI_EASY = "oupao_fly_mini_easy"
    const val K_FLY_MINI_SWIPE = "oupao_fly_mini_swipe"
    const val K_FLY_PINS = "oupao_fly_pins"
    const val K_FLY_RANGE = "oupao_fly_range"
    const val K_FLY_SCALE = "oupao_fly_scale"
    const val K_FLY_STAY_OFF = "oupao_fly_stay_off"
    const val K_FLY_UI = "oupao_fly_ui"
    const val K_FLY_UI_BAR = "oupao_fly_ui_bar"
    const val K_FLY_BORDER_OFF = "oupao_fly_border_off"
    /** 移除应用启动遮罩（启动窗/预览图）：把 ActivityOptions 的 splashScreenStyle 置空 */
    const val K_NO_SPLASH = "oupao_no_splash"

    // ===== 新增（FlymeUI 页）：多任务隐藏小窗应用 / 小窗数量上限 =====
    /** 多任务界面里隐藏「迷你窗应用」（判定方在桌面/Quickstep 进程） */
    const val K_RECENTS_HIDE_FREEFORM = "oupao_recents_hide_freeform"
    /** 3502：重启后清空多任务列表（判定方在 system_server，开机完成后执行一次） */
    const val K_RECENTS_CLEAR_BOOT = "oupao_recents_clear_boot"
    /** 小窗数量上限：开关（判定方在 system_server） */
    const val K_ZOOM_MAX_ON = "oupao_zoom_max_on"
    /** 小窗数量上限：具体数字（1~10，字符串存） */
    const val K_ZOOM_MAX = "oupao_zoom_max"
    /** 3352：平行小窗（全屏二级页保留 + 另开小窗显示主界面）；默认关 */
    const val K_SPLIT_WIN = "oupao_split_win"
    /** 锁屏数字键盘「滑动输入密码」（判定方在 SystemUI 进程） */
    const val K_KG_SLIDE = "oupao_keyguard_slide"
    /** Flyme 式「通知收纳」：手动把通知收进收纳箱（判定方在 SystemUI 进程） */
    const val K_NOTIF_BOX = "oupao_notif_box"
    /** 通知收纳：自动把「静默通知」收进箱子里（判定方同上） */
    const val K_NOTIF_BOX_AUTO = "oupao_notif_box_auto"
    /** 小窗叠放自动错开（配合数量上限；判定方在 system_server） */
    const val K_ZOOM_CASCADE = "oupao_zoom_cascade"

    const val K_MINI_BIG = "oupao_mini_big"
    const val K_MINI_SCALE = "oupao_mini_scale"

    // ===== 全局字母引索 =====
    const val K_IDX_MASTER = "oupao_idx_master"
    const val K_IDX_RIGHT = "oupao_idx_right"
    const val K_IDX_LEFT = "oupao_idx_left"
    /** 旧开关：`K_IDX_GLOBAL` 没设过时用它反推「仅在桌面生效」，保证升级后行为不变 */
    const val K_IDX_LAUNCHER = "oupao_idx_launcher"
    const val K_IDX_GLOBAL = "oupao_idx_global"       // 新：全局生效
    const val K_IDX_GAME = "oupao_idx_game"
    const val K_IDX_VIB = "oupao_idx_vib"
    const val K_IDX_SIDE = "oupao_idx_side"
    const val K_IDX_PINS = "oupao_idx_pins"
    /** 2841：小窗模式那份固定应用（与面板模式、引索条面板完全独立） */
    const val K_DESK_PINS = "oupao_desk_pins"
    /** 固定应用的「显式清空」标记：saveStr 拒绝空串，用 "0" 代表空列表 */
    const val PINS_EMPTY = "0"   // 索引条自己的固定应用（与面板/小窗独立）
    const val K_GAME_BLOCK = "oupao_game_block"
    const val K_GAME_BLOCK_SUG = "oupao_game_block_sug"

    const val K_SUG_BROWSER = "oupao_sug_browser"
    const val K_SUG_INSTALL = "oupao_sug_install"
    const val K_SUG_KEEP = "oupao_sug_keep"
    const val K_SUG_LINK = "oupao_sug_link"
    const val K_SUG_MSG = "oupao_sug_msg"
    /** 头像气泡右侧显示消息内容（旧版是微信/QQ 各一个，现统一全局；默认开） */
    const val K_SUG_INFO = "oupao_sug_info"
    // 新版合并气泡（Bub.kt）
    const val K_BUB_V2 = "oupao_bub_v2"
    const val K_BUB_PERSIST = "oupao_bub_persist"
    const val K_BUB_INFO_SEC = "oupao_bub_info_sec"
    const val K_BUB_SEC = "oupao_bub_sec"
    /**
     * 3081：气泡「双击关闭」。
     * 开着（默认）= 双击气泡关掉它，代价是**单击要等 300ms** 确认没有第二下才打开；
     * 关掉 = 单击立刻打开（不再有双击关闭，也就没有那 300ms 等待）。
     */
    const val K_BUB_DBL_CLOSE = "oupao_bub_dbl_close"
    /** 长按电源键菜单里的「软重启」按钮（默认开） */
    const val K_POWER_SOFT_REBOOT = "oupao_power_soft_reboot"
    /** 信息气泡的生效应用名单（留空 = 全部应用，与屏蔽悬浮通知同一套语义） */
    const val K_SUG_INFO_APPS = "oupao_sug_info_apps"
    const val K_SUG_MUSIC = "oupao_sug_music"
    const val K_SUG_TORCH = "oupao_sug_torch"
    const val K_SUG_WEB = "oupao_sug_web"
    const val K_SUG_WORD = "oupao_sug_word"

    const val K_BUB_POS = "oupao_bub_pos"
    /** 横屏气泡位置（竖屏沿用 K_BUB_POS，两个方向各存一份） */
    const val K_BUB_POS_LAND = "oupao_bub_pos_land"

    const val K_NOHU_ALL = "oupao_nohu_all"

    /// 白名单：只有名单里的应用弹「消息头像气泡」。**留空 = 全部应用**。
    /// 分号分隔的包名，例如 `com.tencent.mm;com.tencent.mobileqq`
    const val K_BUBBLE_APPS = "oupao_bubble_apps"

    /// 白名单：只有名单里的应用被「屏蔽悬浮通知」。**留空 = 全部应用**。
    const val K_NOHU_APPS = "oupao_nohu_apps"

    /// 白名单的存储前缀，见 [saveList] 的说明（用来绕开"空串不落盘"的保护）
    const val LIST_V1 = "v1:"

    const val K_APP_KEEP = "oupao_app_keep"
    const val K_MUSIC_PKG = "oupao_music_pkg"
    const val K_MUSIC_LIST = "oupao_music_list"
    const val K_MUSIC_CUSTOM = "oupao_music_custom"
    const val K_MUSIC_HIDDEN = "oupao_music_hidden"
    const val K_MUSIC_CUSTOM_HID = "oupao_music_custom_hid"   // 手动添加后被移除的（与内置隐藏分开）

    const val K_DBG = "oupao_dbg"
    const val K_LOG_QUIET = "oupao_log_quiet"
    /** 3114：轻量打开（合并 通知点击/应用间跳转/设置页 三个小窗开关） */
    const val K_LIGHT_OPEN = "oupao_light_open"

    // 轻量打开的场景开关（默认值见 DEFAULTS；默认尽量保持 3466 行为）
    const val K_LIGHT_NOTIF_PORT = "oupao_light_notif_port"
    const val K_LIGHT_NOTIF_LAND = "oupao_light_notif_land"
    const val K_LIGHT_QS = "oupao_light_qs"
    const val K_LIGHT_JUMP_SETTINGS = "oupao_light_jump_settings"
    const val K_LIGHT_JUMP_THIRD = "oupao_light_jump_third"
    const val K_LIGHT_SHARE = "oupao_light_share"
    const val K_LIGHT_TEXT = "oupao_light_text"
    const val K_LIGHT_GAME = "oupao_light_game"

    /** 屏蔽侧边栏「边缘滑动」入口（默认开 = 屏蔽）；中转站仍可从更多应用→工具呼出 */
    const val K_SIDEBAR_NOSWIPE = "oupao_sidebar_noswipe"


    // ---------- 路径 ----------
    private const val APP_DIR = "/data/data/com.oupao.wiz/files/cfg"
    private const val SYS_DIR = "/data/system/oupao_cfg"
    private const val LOG_PATH = "/data/system/oupao.log"
    private const val DBG_PATH = "/data/data/com.oupao.wiz/files/dbg.log"

    private const val SETTINGS_PREFIX = "oupao_"

    // ================= 3145：权威配置源 = SharedPreferences =================
    /**
     * 开关真值统一存进 SharedPreferences（**唯一权威源**）。
     *
     * 为什么换：原来有 4 份读源（App 私有文件 / `/data/system/oupao_cfg` /
     * `Settings.Global` / `persist.*` 属性）互相覆盖，"关了还生效"这类问题能长期存在
     * 就是因为 Global 优先盖过文件。现在：
     *
     *  - **App 进程**：`getSharedPreferences("oupao_cfg", MODE_PRIVATE)`；
     *  - **被 hook 的进程**（system_server / SystemUI / 桌面 / 侧边栏）：
     *    `XposedModule.getRemotePreferences("oupao_cfg")` —— LSPosed 直读 App 那份文件，
     *    读写都走 Binder，**不需要 root、不需要文件权限**。
     *
     * 两条通道读的是同一个文件，不存在两份数据。
     *
     * 老框架不支持远程 prefs（无 `PROP_CAP_REMOTE`）时 `prefs()` 返回 null，
     * [rawInt]/[rawStr] 自动退回 3144 的文件读法 —— 不会把功能读没。
     */
    const val PREFS_NAME = "oupao_cfg"
    const val ACTION_SET = "com.oupao.wiz.SET"
    private const val P_MIGRATED = "_migrated_v1"
    private const val P_PROBE = "_probe"
    private const val P_REV = "_rev"
    private const val PKG_SELF = "com.oupao.wiz"

    @Volatile private var rp: android.content.SharedPreferences? = null
    @Volatile private var lp: android.content.SharedPreferences? = null
    @Volatile private var prefWritable = false
    /** 远程 prefs（hook 进程里拿到的）是否可写；实测多数为只读 */
    @Volatile private var rpWritable = false
    /** App 侧通过框架 service 拿到的 prefs 是否可写 */
    @Volatile private var lpWritable = false

    /** 能用来**写**的那一份：App 的 service 通道优先（hook 进程里的远程 prefs 是只读的） */
    private fun writablePrefs(): android.content.SharedPreferences? {
        if (lpWritable) lp?.let { return it }
        if (rpWritable) rp?.let { return it }
        return null
    }
    @Volatile private var prefAttachDone = false
    @Volatile private var serviceRequested = false
    /** App 进程里"还没连上 service 时收到的改动"，连上之后一次性补写 */
    private val pendingSets = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    private fun isSelfProcess(): Boolean = runCatching {
        val c = ctx ?: Rx.ctxB()
        c?.packageName == PKG_SELF
    }.getOrDefault(false)

    private fun flushPending() {
        if (pendingSets.isEmpty()) return
        runCatching {
            val e = writablePrefs()?.edit() ?: return@runCatching
            var n = 0
            for ((k, v) in pendingSets) { e.putInt(k, if (v) 1 else 0); n++ }
            if (e.commit()) {
                pendingSets.clear()
                invalidate()
                rawLog("pending 已补写 n=$n")
            }
        }
    }
    @Volatile private var migrating = false
    @Volatile private var revSeen = Int.MIN_VALUE
    @Volatile private var revAt = 0L

    /**
     * 被 hook 的进程：由 [XInit] 在 onModuleLoaded 里调用。
     *
     * 参数故意用 `Any`（而不是 XposedModule）：libxposed 的 api 是 compileOnly，
     * App 进程运行时根本没有这些类，把类型写进签名会让 App 侧加载 Cfg 时解析失败。
     */
    @JvmStatic
    fun attachRemote(m: Any) {
        if (rp != null) return
        runCatching {
            val meth = m.javaClass.methods.firstOrNull {
                it.name == "getRemotePreferences" && it.parameterTypes.size == 1 &&
                    it.parameterTypes[0] == String::class.java
            } ?: throw IllegalStateException("框架没有 getRemotePreferences")
            val p = meth.invoke(m, PREFS_NAME) as? android.content.SharedPreferences
                ?: throw IllegalStateException("getRemotePreferences 返回 null")
            rp = p
            prefAttachDone = true
            SLog.i("Cfg: 远程 prefs 已获取 group=$PREFS_NAME keys=${runCatching { p.all.size }.getOrDefault(-1)}")
            // 诊断：确认远程 prefs 到底能不能读到 App 那份 xml 里的键
            runCatching {
                val m = runCatching { p.getBoolean(P_MIGRATED, false) }.getOrDefault(false)
                rawLog("prefs 探针 keys=" + p.all.size + " migrated=" + m +
                    " contains=" + p.contains(P_MIGRATED) +
                    " sug_msg=" + p.all[K_SUG_MSG])
            }.onFailure {
                rawLog("prefs 探针异常 " + it)
                SLog.e("Cfg: prefs 探针异常", it)
            }
            // 可写性探测：写一个时间戳再读回（LSPosed 的远程 prefs 写会同步回 App 文件）
            val ts = (System.currentTimeMillis() and 0x3FFFFFFF).toInt()
            val putOk = runCatching { p.edit().putInt(P_PROBE, ts).commit() }.getOrDefault(false)
            prefWritable = runCatching { p.getInt(P_PROBE, -1) == ts }.getOrDefault(false)
            rpWritable = prefWritable
            SLog.i("Cfg: prefs 接管(remote) write=$prefWritable putOk=$putOk keys=${runCatching { p.all.size }.getOrDefault(-1)}")
            migratePrefs(p)
            backfillTs(p)
            flushPending()
        }.onFailure { SLog.e("Cfg: 远程 prefs 不可用，退回旧读法", it) }
    }

    /** 配置层裸日志：App 进程写不了 /data/system/oupao.log，排查时看 logcat -s OupaoCfg */
    private fun rawLog(msg: String) {
        runCatching { android.util.Log.i("OupaoCfg", msg) }
    }

    /** App 进程：注入自己的 Context 后建本地 prefs（与远程读的是同一个文件） */
    @JvmStatic
    fun attachLocal(c: Context?) {
        if (c == null) return
        if (runCatching { c.packageName }.getOrNull() != PKG_SELF) return
        // 3146：**App 侧也不能用 getSharedPreferences**。
        // 实测系统行为：它从不建自己的 pref 文件，而是
        // XposedServiceHelper → XposedService.getRemotePreferences(group)，
        // 由框架（LSPosed）提供那个存储；hook 进程读的也是同一个。
        // 之前我们用 getSharedPreferences 建了一份"看起来一样"的文件，
        // 框架那边 keys=0 —— 根因就在这里。
        if (!serviceRequested) {
            serviceRequested = true
            runCatching {
                io.github.libxposed.service.XposedServiceHelper.registerListener(
                    object : io.github.libxposed.service.XposedServiceHelper.OnServiceListener {
                        override fun onServiceBind(service: io.github.libxposed.service.XposedService) {
                            runCatching {
                                val p = service.getRemotePreferences(PREFS_NAME)
                                lp = p
                                prefWritable = true
                                lpWritable = true
                                rawLog("service prefs 已连接 keys=" + p.all.size)
                                SLog.i("Cfg: service prefs 已连接 keys=" + p.all.size)
                                migratePrefs(p)
                                backfillTs(p)
                                flushPending()
                            }.onFailure {
                                rawLog("service prefs 读取失败 " + it)
                                SLog.e("Cfg: service prefs 读取失败", it)
                            }
                        }

                        override fun onServiceDied(service: io.github.libxposed.service.XposedService) {
                            lp = null
                            prefWritable = false
                            SLog.i("Cfg: service prefs 断开")
                        }
                    })
                SLog.i("Cfg: XposedServiceHelper 已注册")
                rawLog("XposedServiceHelper 已注册")
            }.onFailure {
                SLog.e("Cfg: XposedServiceHelper 注册失败", it)
                rawLog("XposedServiceHelper 注册失败 " + it)
            }
        }
    }

    /** 当前进程是否已经接上权威 prefs（false = 还在用 3144 的旧读法） */
    @JvmStatic
    fun prefsReady(): Boolean = usablePrefs() != null

    /**
     * 能拿来**判定**的 prefs：必须已完成一次性迁移（有 `_migrated_v1` 标记）。
     *
     * 为什么这样兜底：升级后第一次开机时 prefs 还是空的，如果直接按"空 = 全默认"
     * 判定，所有开关会瞬间变回默认值（实测：气泡直接不弹了）。加这个门之后，
     * 迁移完成前一律走旧读法，迁移完成后才切到 prefs —— 不会把用户配置读没。
     */
    private fun usablePrefs(): android.content.SharedPreferences? {
        val p = prefs() ?: return null
        return if (runCatching { p.getBoolean(P_MIGRATED, false) }.getOrDefault(false)) p else null
    }

    /** 当前进程能用的权威 prefs；App 进程按需惰性初始化 */
    private fun prefs(): android.content.SharedPreferences? {
        rp?.let { return it }
        lp?.let { return it }
        val c = ctx ?: runCatching { Rx.ctxB() }.getOrNull() ?: return null
        attachLocal(c)
        return lp
    }
    /**
     * 一次性迁移：把旧的 4 份数据搬进 prefs。
     *
     * ⚠️ 必须按**键的类型**分别取值：
     *  - 布尔键：只认 "1"/"0"（Global 里存的就是这个）；
     *  - 字符串键（尺寸/位置/固定应用/名单…）：必须原样搬字符串，
     *    绝不能走 [legacyBool] —— 那样 "100"、"-1,-1" 都会被当成 true 变成 1
     *    （第一版就踩了这个坑，整份配置被写成一堆 1）。
     *
     * 优先级沿用 3133–3140 定下的「Global 优先」：用户在 ColorOS 设置页改过的
     * 通知收纳等键只在 Global 里，若按 App 文件迁移会把用户的开启状态吞掉。
     */
    private fun migratePrefs(p: android.content.SharedPreferences) {
        if (migrating) return
        if (runCatching { p.getBoolean(P_MIGRATED, false) }.getOrDefault(false)) return
        if (!prefWritable) return   // 只读进程不迁移，交给能写的进程
        migrating = true
        runCatching {
            val boolKeys = BOOL_KEYS.toHashSet()
            val e = p.edit()
            var n = 0
            for (k in ALL_KEYS) {
                if (runCatching { p.contains(k) }.getOrDefault(false)) continue
                // 取值优先级：**配置文件的较新一份** > Settings.Global。
                // 反过来的话，Global 里被历史 bug 写脏的值（比如通知收纳恒为 1）
                // 会被当成用户意图搬进来；而 App 文件才是用户在模块界面里真正拨过的状态。
                val raw = runCatching { rawStrLegacy(k, null) }.getOrNull()
                    ?: globalRaw(k)
                if (raw == null || raw.isEmpty()) continue
                val t = raw.trim()
                // 脏值不迁：历史 bug 会把数字/名单键写成 "1"（例如 fly_range=1、bubble_apps=1），
                // 搬进权威源后就会一直错下去。宁可留空走默认值。
                if (!migrationValueOk(k, t)) {
                    SLog.i("Cfg: 迁移跳过脏值 $k=$t")
                    continue
                }
                if (k in boolKeys) {
                    e.putInt(k, if (raw.trim() == "0") 0 else 1)
                } else {
                    // 数字/字符串统一按字符串存，读的时候 prefStr/prefInt 都能解析
                    e.putString(k, raw.trim())
                }
                e.putLong(k + "#t", System.currentTimeMillis())
                n++
            }
            e.putInt(P_REV, runCatching { p.getInt(P_REV, 0) }.getOrDefault(0) + 1)
            e.putBoolean(P_MIGRATED, true)
            e.commit()
            cI.clear(); cS.clear(); mOnAt = 0L
            SLog.i("Cfg: prefs 迁移完成 n=$n")
        }.onFailure { SLog.e("Cfg: prefs 迁移失败", it) }
        migrating = false
    }

    /** 读 Settings.Global 的**原始字符串**（迁移用，别拿它当布尔） */
    private fun globalRaw(key: String): String? {
        val name = SETTINGS_PREFIX + key
        for (c in globalCtxs()) {
            val v = runCatching { Settings.Global.getString(c.contentResolver, name) }.getOrNull()
            if (!v.isNullOrEmpty()) return v
        }
        return null
    }

    /** 数字型配置键：迁移时必须能解析成整数，否则视为脏值 */
    private val NUM_KEYS = setOf(
        K_FLY_RANGE, K_FLY_CLOSE, K_FLY_SCALE, K_ICON_SIZE, K_BUB_SEC, K_BUB_INFO_SEC,
        K_ZOOM_MAX, K_FLY_MINI_SWIPE
    )

    /** 列表型配置键：迁移时至少要像个包名列表（或空列表标记 "v1:"/"0"） */
    private val LIST_KEYS = setOf(
        K_BUBBLE_APPS, K_NOHU_APPS, K_SUG_INFO_APPS, K_FLY_PINS, K_IDX_PINS, K_DESK_PINS,
        "oupao_pins", "clones"
    )

    /** 坐标型配置键：必须是 "x,y" */
    private val POS_KEYS = setOf(K_BUB_POS, K_BUB_POS_LAND, "oupao_bub2_pos", "oupao_bub2_pos_land")

    private fun migrationValueOk(key: String, v: String): Boolean = when {
        key in NUM_KEYS -> v.toIntOrNull() != null
        key in POS_KEYS -> v.contains(",")
        key in LIST_KEYS -> v.startsWith(LIST_V1) || v.contains(".") || v == PINS_EMPTY || v.isEmpty()
        else -> true
    }

    private fun prefInt(p: android.content.SharedPreferences, key: String): Int? = runCatching {
        if (!p.contains(key)) return null
        when (val v = p.all[key]) {
            is Int -> v
            is Boolean -> if (v) 1 else 0
            is Long -> v.toInt()
            is Float -> v.toInt()
            is String -> v.trim().toIntOrNull()
            else -> null
        }
    }.getOrNull()

    private fun prefStr(p: android.content.SharedPreferences, key: String): String? = runCatching {
        if (!p.contains(key)) return null
        when (val v = p.all[key]) {
            is String -> v
            is Int -> v.toString()
            is Boolean -> if (v) "1" else "0"
            else -> v.toString()
        }
    }.getOrNull()

    /** 写权威源；写到就返回 true */
    private fun prefPut(key: String, value: Boolean): Boolean {
        val p = writablePrefs() ?: return false
        return runCatching {
            val e = p.edit().putInt(key, if (value) 1 else 0).putLong(key + "#t", System.currentTimeMillis())
            var ok = e.commit()
            if (ok) {
                val r = runCatching { p.getInt(P_REV, 0) }.getOrDefault(0) + 1
                p.edit().putInt(P_REV, r).apply()
                revSeen = r
            }
            ok
        }.getOrDefault(false)
    }

    private fun prefPutStr(key: String, value: String): Boolean {
        val p = writablePrefs() ?: return false
        return runCatching {
            var ok = p.edit().putString(key, value)
                .putLong(key + "#t", System.currentTimeMillis()).commit()
            if (ok) {
                val r = runCatching { p.getInt(P_REV, 0) }.getOrDefault(0) + 1
                p.edit().putInt(P_REV, r).apply()
                revSeen = r
            }
            ok
        }.getOrDefault(false)
    }

    /** 供外部（Activity）注入 Context */
    @JvmField
    var ctx: Context? = null

    // ---------- 缓存 ----------
    private val cI = ConcurrentHashMap<String, Int>()
    private val cS = ConcurrentHashMap<String, String>()
    private val warned = HashSet<String>()

    @Volatile
    private var cts = 0L

    @Volatile
    private var cFp = 0

    // ================= 主开关 =================

    /**
     * 总开关。读取优先级：App 侧文件 → 系统侧文件 → 默认开(true)。
     * 注意：系统侧显式为 "0" 时尊重用户设置返回 false；两侧都不存在时默认开。
     *
     * ⚠️ 性能：这个函数**不走 cI/cS 缓存**（它本身就是所有 gate 的第一道判据），
     * 而 [flyopGate] = `masterOn() && flyopMaster() && getInt(key)`，
     * [flyopMaster] 内部又一次 [masterOn] → 一次 gate = 3 次 masterOn = 6 次 readStr。
     * 而 dockKeep/capClean/miniBig/doubleTapClose/videoNoPause/flyClose/flyMaster
     * 分别挂在 system_server 最高频的 hook 上（TaskFragment.startPausing、
     * ActivityRecord.stopIfPossible、FlexibleCaptionView.relayout、dispatchTouchEvent、
     * updateTouchableRegion、动画 hook、以及 FlyH.handle 的每次触摸 MOVE）。
     *
     * 所以这里加一个 1s TTL 缓存（与 Idx.feed 已有的 masterAt 同思路）。
     * 失效靠两处，都与现有机制同构：
     *  1) [invalidate] —— 本进程写配置时立即清；
     *  2) [probe] 的 .gen 变更探测 —— 别的进程（App 侧）改配置时清。
     * 也就是说「改开关 → 生效」的延迟最多 1s，与原来的文件读语义一致。
     */
    @Volatile private var mOnAt = 0L
    @Volatile private var mOnV = true

    // ===== 2975（用户：锁屏时所有功能不生效，省功耗）=====
    @Volatile private var sleepAt = 0L
    @Volatile private var sleepVal = false

    /**
     * 灭屏 或 锁屏 → true。带 1 秒缓存（热路径：抦截回调、启动钩子都会问）。
     * 用法：所有「我们主动做事」的入口先 `if (Cfg.sleepingNow()) return`。
     */
    @JvmStatic
    fun sleepingNow(): Boolean {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - sleepAt < 1000L) return sleepVal
        val v = runCatching {
            val c = sysContext() ?: return@runCatching false
            val pm = c.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
            if (pm != null && !pm.isInteractive) return@runCatching true
            val km = c.getSystemService(android.content.Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
            km != null && km.isKeyguardLocked
        }.getOrDefault(false)
        sleepVal = v
        sleepAt = now
        return v
    }

    @JvmStatic
    fun masterOn(): Boolean {
        val now = android.os.SystemClock.uptimeMillis()
        val v = mOnV
        if (now - mOnAt <= MASTER_TTL) return v
        val r = masterOnRaw()
        mOnV = r
        mOnAt = now
        return r
    }

    private fun masterOnRaw(): Boolean {
        val app = readStr("$APP_DIR/$K_EGG_MASTER")
        if (app != null) return app == "1"
        val sys = readStr("$SYS_DIR/$K_EGG_MASTER")
        if (sys == null) return true
        if (sys == "0") {
            SLog.i("Cfg: 主开关=关(系统侧=0，遵照用户设置)")
            return false
        }
        return true
    }

    @JvmStatic
    fun eggMaster(): Boolean = masterOn() && getStr(K_EGG_MASTER, "1") == "1"

    @JvmStatic
    fun flyopMaster(): Boolean = masterOn() && getInt(K_FLYOP_MASTER, 0) == 1

    @JvmStatic
    fun sugMaster(): Boolean = masterOn() && getStr(K_SUG_MASTER, "0") == "1"

    @JvmStatic
    fun gameMaster(): Boolean = masterOn() && getInt(K_GAME_MASTER, 0) == 1

    private var vibSysAt = 0L
    private var vibSysVal = true

    /**
     * 3273：**振动完全跟随系统**（用户要求：移除「振动效果」页所有键）。
     *
     * 只看系统的触感/振动总开关：
     *  - ColorOS 实测键是 `Settings.System.haptic_feedback_enabled`（设置-声音与振动-触感反馈）；
     *    `vibrate_on` 在这台机器上根本不存在（get 回来是 null）。
     * 系统开 → 我们照常振；系统关 → 我们一次都不振。
     * 强度也不再由我们决定：`VibrationEffect.createOneShot(dur, -1)` 用的是系统默认振幅。
     *
     * 1 秒缓存：索引条滑动时每次换字母都会问一次，不能每次都去读 Settings 的 ContentProvider。
     */
    @JvmStatic
    fun vibMaster(): Boolean {
        if (!masterOn()) return false
        val now = android.os.SystemClock.uptimeMillis()
        if (now - vibSysAt < 1000L) return vibSysVal
        val v = runCatching {
            val c = sysContext()
            if (c == null) {
                false
            } else {
                val cr = c.contentResolver
                val haptic = android.provider.Settings.System.getInt(cr, "haptic_feedback_enabled", 1)
                val vib = android.provider.Settings.System.getInt(cr, "vibrate_on", 1)
                (haptic != 0) && (vib != 0)
            }
        }.getOrDefault(false)
        vibSysAt = now
        vibSysVal = v
        return v
    }

    /** 供 UI 层按 key 查询主开关状态 */
    @JvmStatic
    fun masterChecked(key: String): Boolean = when (key) {
        K_EGG_MASTER -> eggMaster()
        K_SUG_MASTER -> sugMaster()
        K_GAME_MASTER -> gameMaster()
        K_FLYOP_MASTER -> flyopMaster()
        else -> getInt(key, 0) == 1
    }

    // ================= 门控 =================

    // 3121：门控默认值必须与 UI 的「默认关闭」一致。以前这些函数缺省 1，
    // 配置文件还没落盘时（刚装完、还没打开过模块）功能会先按“开”跑一段时间；
    // 现在统一缺省 0，只有用户明确打开并落盘后才生效。
    private fun flyopGate(key: String, def: Int = 0) =
        masterOn() && flyopMaster() && getInt(key, def) == 1

    private fun sugGate(key: String, def: Int = 0) =
        masterOn() && sugMaster() && getInt(key, def) == 1

    private fun gameGate(key: String, def: Int = 0) =
        masterOn() && gameMaster() && getInt(key, def) == 1

    private fun plainGate(key: String, def: Int = 0) =
        masterOn() && getInt(key, def) == 1

    // ================= 功能开关 =================

    @JvmStatic fun doubleTapClose() = flyopGate(K_DOUBLE_TAP_CLOSE)
    @JvmStatic fun videoNoPause() = !isC17() && flyopGate(K_VIDEO_NO_PAUSE)

    /**
     * 3313：ColorOS 17（Android 17 / API 37）。
     *
     * C17 的系统默认行为就是「上滑手势不暂停播放」，模块这一项既不需要、也不应该再开
     * （开了反而可能和系统的新逻辑打架）。设置页据此把开关置灰。
     */
    @JvmStatic
    fun isC17(): Boolean = runCatching {
        android.os.Build.VERSION.SDK_INT >= 37
    }.getOrDefault(false)
    @JvmStatic fun landscapeFullscreen() = flyopGate(K_LANDSCAPE_FULLSCREEN)
    @JvmStatic fun muteDirect() = flyopGate(K_LONG_PRESS_MUTE)
    @JvmStatic fun dockKeep() = flyopGate(K_DOCK_KEEP_ALIVE)
    @JvmStatic fun screenKeep() = flyopGate(K_SCREEN_KEEP)
    @JvmStatic fun capClean() = flyopGate(K_CAP_CLEAN)
    @JvmStatic fun cornerOff() = flyopGate(K_CORNER_OFF)
    @JvmStatic fun hideIcon() = plainGate(K_HIDE_ICON, 0)

    /**
     * 应用间跳转 → 小窗打开。
     *
     * 判定方在 **system_server**（`ATMS.startActivity`），和 [notifFly] 不同 ——
     * 后者在 SystemUI 里读不到文件/设置，只能绕系统属性。这里直接用普通文件通道，
     * 走 `getInt` → `cI` 缓存 + [probe] 的 `.gen` 变更失效，App 拨开关后最多 1s 生效。
     *
     * 默认 **关**：这个改动面很大（任何 App 的跳转都会变），先让用户自己开。
     */
    @JvmStatic fun crossZoom() = plainGate(K_LIGHT_OPEN, 0)

    /** 3114：三个「小窗打开」合并成一个「轻量打开」（通知点击 / 应用间跳转 / 设置页） */
    @JvmStatic fun settingsZoom() = plainGate(K_LIGHT_OPEN, 0)

    // ===== 轻量打开：按场景细分 =====
    @JvmStatic fun lightNotifPort() = plainGate(K_LIGHT_NOTIF_PORT, 1)
    @JvmStatic fun lightNotifLand() = plainGate(K_LIGHT_NOTIF_LAND, 1)
    @JvmStatic fun lightQs() = plainGate(K_LIGHT_QS, 1)
    @JvmStatic fun lightJumpSettings() = plainGate(K_LIGHT_JUMP_SETTINGS, 1)
    @JvmStatic fun lightJumpThird() = plainGate(K_LIGHT_JUMP_THIRD, 1)
    @JvmStatic fun lightShare() = plainGate(K_LIGHT_SHARE, 1)
    @JvmStatic fun lightText() = plainGate(K_LIGHT_TEXT, 1)
    @JvmStatic fun lightGame() = plainGate(K_LIGHT_GAME, 0)

    @JvmStatic fun appKeep() = masterOn() && sugMaster() && getInt(K_APP_KEEP, 0) == 1

    @JvmStatic fun flyMaster() = plainGate(K_FLY_MASTER, 0)

    // ===== 字母引索 =====
    @JvmStatic fun idxMaster() = idxBool(K_IDX_MASTER, false)
    /** 呼出边缘：R=右 L=左 B=两侧（空=未设，回退到旧的两个开关） */
    @JvmStatic
    fun idxSide(): String {
        val v = runCatching { getStrFresh(K_IDX_SIDE, "") }.getOrNull()
        if (!v.isNullOrEmpty()) return v.trim()
        return ""
    }

    @JvmStatic
    fun setIdxSide(v: String) {
        runCatching {
            saveStr(K_IDX_SIDE, v)
            push()
        }
    }

    @JvmStatic fun idxRight(): Boolean {
        val s0 = idxSide()
        if (s0.isNotEmpty()) return s0.contains("R") || s0.contains("B")
        return idxBool(K_IDX_RIGHT, true)
    }

    @JvmStatic fun idxLeft(): Boolean {
        val s0 = idxSide()
        if (s0.isNotEmpty()) return s0.contains("L") || s0.contains("B")
        return idxBool(K_IDX_LEFT, false)
    }
    @JvmStatic fun idxLauncherOnly() = idxBool(K_IDX_LAUNCHER, false)

    /**
     * 是否全局生效（任何应用里都能呼出索引条）。默认 true。
     *
     * 新键没设过时按旧的「仅在桌面生效」反推，保证升级后行为不变。
     */
    @JvmStatic
    fun idxGlobal(): Boolean {
        // 3289：改用带 300ms 缓存的 getStr（原 getStrFresh 每个 tick 直读两份文件）
        val v = runCatching { getStr(K_IDX_GLOBAL, "") }.getOrNull()
        if (!v.isNullOrEmpty()) {
            val t = v.trim()
            return (t == "1") || t.equals("true", true)
        }
        return !idxLauncherOnly()
    }
    @JvmStatic fun idxGameBlock() = idxBool(K_IDX_GAME, true)
    @JvmStatic fun idxVib() = idxBool(K_IDX_VIB, true)

    /** 引索用的布尔读取：权威 prefs → 文件 */
    private fun idxBool(key: String, def: Boolean): Boolean {
        val v = runCatching { getStrFresh(key, "") }.getOrNull()
        if (!v.isNullOrEmpty()) {
            val t = v.trim()
            return (t == "1") || t.equals("true", true)
        }
        return def
    }
    @JvmStatic fun flyLeft() = plainGate(K_FLY_LEFT)
    @JvmStatic fun flyRight() = plainGate(K_FLY_RIGHT)
    @JvmStatic fun flyUi() = plainGate(K_FLY_UI, 0)
    @JvmStatic fun flyUiBar() = plainGate(K_FLY_UI_BAR)

    /**
     * 移除应用启动遮罩。
     *
     * 判定方在 **system_server**（[HookIdle] 的 ATMS.startActivity*）：
     * 给每次启动的 ActivityOptions Bundle 写入
     * `android.activity.splashScreenStyle = 0`（SPLASH_SCREEN_STYLE_EMPTY），
     * 系统就不会再画启动窗/预览遮罩，应用直接出现在画面上。
     *
     * 默认 **关**（改动面很大，用户自己开）。
     */
    @JvmStatic fun noSplash() = plainGate(K_NO_SPLASH, 0)

    /**
     * 多任务界面里隐藏「小窗应用」。
     *
     * 判定方在**桌面（Quickstep）进程**，那里既读不到模块私有文件、也写不了模块日志，
     * 只能读 `Settings.Global`。桌面的 Context 只能由调用方给（[FlyGate.reportCtx]），
     * 所以这里带一个可选 Context：给了就用它读 Global，读不到再回退文件通道。
     */
    @JvmStatic
    fun recentsHideFreeform(c: Context? = null): Boolean {
        // 3146：只保留 权威 prefs → 文件；Global / 属性通道已删除
        if (prefsReady()) return crossProcGate(K_RECENTS_HIDE_FREEFORM)
        if (!masterOn()) return false
        val app = readOne("$APP_DIR/$K_RECENTS_HIDE_FREEFORM")
        if (app >= 0) return app == 1
        val sys = probeSys(K_RECENTS_HIDE_FREEFORM)
        return sys == 1
    }

    /**
     * 3502：重启后清空多任务列表。
     *
     * 判定方在 **system_server**（Boot 里等 `sys.boot_completed=1` 后执行一次），
     * 普通文件通道即可；默认关。
     */
    @JvmStatic fun recentsClearBoot() = plainGate(K_RECENTS_CLEAR_BOOT, 0)

    /**
     * 小窗数量上限（1~10）的开关。判定方在 system_server，普通文件通道即可。
     */
    @JvmStatic fun zoomMaxOn() = plainGate(K_ZOOM_MAX_ON, 0)

    /** 3352：平行小窗（全屏二级页 + 小窗主界面），默认关 */
    @JvmStatic fun splitWin() = plainGate(K_SPLIT_WIN, 0)

    /**
     * 3308：**只有上限=1** 时才启用模块自己的「屏蔽贴边 + 串行关旧窗」。
     *
     * 用户要求：2 及以上完全恢复原生行为，只解除数量上限
     * （原生到上限会贴边、贴边满两个关最旧，这套不要动）。
     */
    @JvmStatic
    fun zoomMaxSingle(): Boolean = zoomMaxOn() && zoomMax() == 1

    /**
     * 小窗数量上限的数字（1~10，默认 3）。
     *
     * 1 秒缓存：`getMaxWinNum` 在开/关/切换小窗时会被频繁调用，
     * 而 [getStrFresh] 每次要读两个文件 + 比 mtime，放在这个热路径上不合适。
     */
    @Volatile
    private var zoomMaxCache = -1

    @Volatile
    private var zoomMaxAt = 0L

    @JvmStatic
    fun zoomMax(): Int {
        val now = android.os.SystemClock.elapsedRealtime()
        val c = zoomMaxCache
        if (c > 0 && now - zoomMaxAt < 1000L) return c
        val v = runCatching {
            getStrFresh(K_ZOOM_MAX, "3").trim().toIntOrNull() ?: 3
        }.getOrDefault(3).coerceIn(1, 10)
        zoomMaxCache = v
        zoomMaxAt = now
        return v
    }

    /** 小窗叠放自动错开（默认开；只有数量上限开着时才生效）。 */
    @JvmStatic fun zoomCascade() = plainGate(K_ZOOM_CASCADE, 0)

    /**
     * 锁屏数字键盘「滑动输入密码」。
     *
     * 判定方是 **SystemUI** —— 它读不到模块私有文件（实测），只能读 `Settings.Global`；
     * [globalFlag] 的候选里已经包含 [Ctx.sysUi]，所以这里不用额外传 Context。
     */
    @JvmStatic
    fun keyguardSlide(): Boolean = crossProcGate(K_KG_SLIDE)

    /**
     * Flyme 式「通知收纳」总开关。
     *
     * 判定方是 **SystemUI** 进程（长按通知菜单插一项「收纳」+ 通知面板头部入口按钮 +
     * 收纳箱列表，见 NotifBox.kt）。3145 起统一读权威源（prefs → App 文件 → 系统侧文件
     * → Global 兜底），Global 只给"什么都读不到"的进程兜底，不再优先。
     */
    @JvmStatic
    fun notifBox(): Boolean = crossProcGate(K_NOTIF_BOX)

    /** 通知收纳：自动收纳「静默通知」（importance < IMPORTANCE_DEFAULT 或 ambient）。 */
    @JvmStatic
    fun notifBoxAuto(): Boolean = crossProcGate(K_NOTIF_BOX_AUTO)

    /** 屏蔽侧边栏边缘滑动（判定方 com.coloros.smartsidebar） */
    @JvmStatic
    fun sidebarNoSwipe(): Boolean = crossProcGate(K_SIDEBAR_NOSWIPE)

    /**
     * 「判定方在别的进程」的布尔开关统一读法。
     *
     * 顺序：权威 prefs → App 私有文件 → 系统侧文件 → `Settings.Global`（最后兜底）。
     *
     * 为什么 Global 只能垫底：3133–3140 那一串「开关关了还生效」全部是
     * Global 优先盖过用户刚写进文件的值造成的。现在它只服务一种情况——
     * 目标进程既读不到模块私有文件、也读不到 /data/system/oupao_cfg
     * （老框架 + 老版本的镜像仍然留着）。
     */
    @JvmStatic
    fun crossProcGate(key: String): Boolean {
        if (!masterOn()) return false
        usablePrefs()?.let { p -> return prefInt(p, key)?.let { it == 1 } ?: false }
        val app = readOne("$APP_DIR/$key")
        if (app >= 0) return app == 1
        val sys = probeSys(key)
        if (sys >= 0) return sys == 1
        // 3146：Global 兜底也删了 —— 所有判定进程都在作用域里，能读远程 prefs；
        // 读不到就按默认（关），不再拿历史脏值当真。
        return false
    }

    // 3146：hasProp / globalFlagC（persist 属性 / Global 读取）已删除 —— 两条通道都退役。

    /**
     * 隐藏桌面图标。
     *
     * 实现沿用同机上另一应用的做法：入口拆成三个组件后，
     * 只禁用 `LauncherAlias`（MAIN+LAUNCHER）就能去掉图标，
     * 而 LSPosed 的「打开」走 `ModuleSettingsAlias`
     * （MAIN + `de.robv.android.xposed.category.MODULE_SETTINGS`）→ 不受影响，
     * 也不需要给模块加桌面作用域。
     */
    @JvmStatic
    fun applyHideIcon(on: Boolean) {
        applyHideIconState(on)
        save(K_HIDE_ICON, on)
    }

    /**
     * 只把开关状态落到桌面图标组件上，不写配置。
     *
     * ⚠️ 必须与 [applyHideIcon] 分开：`save(K_HIDE_ICON, …)` 会在后台线程回调
     * `applyHideIconState()`，若回调里再 `save()` 就是无尾递归——实测一次启动能打出
     * 14000+ 次 `setComponentEnabledSetting`，把模块界面卡死。
     */
    @JvmStatic
    fun applyHideIconState(on: Boolean) {
        runCatching {
            val ctx = Ui.appCtx ?: sysContext() ?: return
            val cn = android.content.ComponentName(ctx.packageName, ctx.packageName + ".LauncherAlias")
            val state = if (on)
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            else
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            ctx.packageManager.setComponentEnabledSetting(
                cn, state, android.content.pm.PackageManager.DONT_KILL_APP
            )
            SLog.i("Cfg: 桌面图标 hidden=" + on + " 已应用")
        }.onFailure { SLog.e("Cfg: 隐藏桌面图标失败", it) }
    }

    /**
     * 一次性把「移除应用启动遮罩」重置为关闭。
     *
     * 为什么需要：早期版本（2312~2317）`BOOL_DEFAULTS` 里没登记这个键，
     * `push()` 会把它当 true 推到 App 侧文件，导致**开关默认是开的**，
     * 用户看到的默认状态就错了。这里做一次性纠正，之后仍可手动开启。
     *
     * 幂等：靠 APP_DIR 里的 marker 文件，只跑一次。
     */
    @JvmStatic
    fun resetNoSplashOnce() {
        runCatching {
            val marker = java.io.File("$APP_DIR/oupao_no_splash_reset1")
            if (marker.exists()) return
            writeOne("$APP_DIR/$K_NO_SPLASH", false)
            cI.remove(K_NO_SPLASH)
            push()
            runCatching {
                marker.parentFile?.mkdirs()
                marker.writeText("1")
            }
        }
    }
    @JvmStatic fun flyBordOff() = plainGate(K_FLY_BORDER_OFF)
    @JvmStatic fun flyMini() = flyopGate(K_FLY_MINI)
    @JvmStatic fun flyMiniEasy() = flyopGate(K_FLY_MINI_EASY)
    @JvmStatic fun miniBig() = flyopGate(K_MINI_BIG)
    @JvmStatic fun flyStayOff() = runCatching { getBoolRaw(K_FLY_STAY_OFF, false) }.getOrDefault(false)

    // 振动页的「子选项开关」已从界面移除（只保留强度滑条），
    // 所以这里不再看各自的布尔键，统一只受页面主开关 vibMaster() 控制。
    @JvmStatic fun flyVib() = vibMaster()
    @JvmStatic fun pinVib() = vibMaster()
    @JvmStatic fun vibOn() = vibMaster()

    @JvmStatic fun gameBlock() = gameGate(K_GAME_BLOCK)
    @JvmStatic fun gameBlockSug() = gameGate(K_GAME_BLOCK_SUG)

    @JvmStatic fun sugBrowser() = sugGate(K_SUG_BROWSER)
    @JvmStatic fun sugInstall() = sugGate(K_SUG_INSTALL)
    @JvmStatic fun sugKeep() = sugGate(K_SUG_KEEP)
    @JvmStatic fun sugLink() = sugGate(K_SUG_LINK)
    @JvmStatic fun sugMsg() = sugGate(K_SUG_MSG, 0)
    @JvmStatic fun sugMusic() = sugGate(K_SUG_MUSIC)
    @JvmStatic fun sugTorch() = sugGate(K_SUG_TORCH)
    @JvmStatic fun sugWeb() = sugGate(K_SUG_WEB)

    /**
     * 「其他应用消息提醒」：任意应用来通知都在左下角弹头像气泡。
     *
     * 原来这个键（[K_SUG_MSG]）只在 [Sug.onNotify] 里被读，而入口
     * [Hooks.onNotify] 把包名写死成微信/QQ → 等于永远走不到。
     * 现在入口放开：微信/QQ 走各自的开关，其余应用一律看这个键。
     */
    @JvmStatic fun sugMsgAll() = sugGate(K_SUG_MSG, 0)

    /** 信息气泡：头像气泡右侧显示消息内容（跟随气泡总开关；默认开） */
    @JvmStatic fun sugInfo() = sugGate(K_SUG_INFO, 0)

    /** 该包要不要显示消息内容 = 信息气泡开关 && 生效应用名单（留空=全部） */
    @JvmStatic fun sugInfoAppOn(pkg: String) = sugInfo() && inList(K_SUG_INFO_APPS, pkg)

    /**
     * 通知点击 → 小窗打开。**固定常开**。
     *
     * 试过并**全部失败**的「开关」方案（别再重复，结论都在这里）：
     *  1) 系统侧文件 `/data/system/oupao_cfg/<key>` → SystemUI(uid 10249)
     *     读 `File.exists()` 恒 false（SELinux 静默拒）；
     *  2) `Settings.Global.getString(...)` → 恒 null（`Package android does not
     *     belong to 10249`，换正确包名的 Context 也不行）；
     *  3) 广播推给 SystemUI → `registerReceiver` 在 SystemUI 里直接抛；
     *  4) 属性 `persist.oupao.oupao_notif_fly`（root setprop 已被验证可读可用，
     *     SystemUI 每次点击现读），但**写入端断了**：
     *     App `Cfg.save()` → `push()` 的 CFG 广播实测没到 system_server
     *     （`Ev: com.oupao.wiz.CFG` 停在 03:50，之后系统侧文件 mtime 不再更新），
     *     所以 `apply()` 里的 setprop 从来跑不到。
     *  5) 「重启作用域」也救不了 —— 问题不是时机，是这个进程压根没读权限。
     *
     * 结论（2830 已解决，下面这段是历史记录）：之前**界面不提供开关**，判定只读属性（默认开）。
     * 2830 的解法是「system_server 写 Settings.Global → SystemUI 用自己 Context 读」，
     * 见下面 [notifFly] 的说明；属性作为 adb/root 覆盖保留。
     */
    /**
     * 通知点击 → 小窗打开。
     *
     * ## 2830：改成**先读 Settings.Global**，不再依赖 root
     *
     * 老实现只有一条通道：系统属性 `persist.oupao.oupao_notif_fly`。
     * 实测（2830）：
     *  - App 侧 `su -c setprop` 要 root —— 用户把模块的 root 权限关掉后就写不进去；
     *  - system_server 侧的 `SystemProperties.set` 被 SELinux 拒（日志
     *    `Cfg: 收到广播 oupao_notif_fly=0 prop:oupao_notif_fly=0(FAIL)`）。
     * 结果：开关只改了配置文件、属性停在旧值 → **看着关了其实功能还开着**。
     *
     * 现在这条路是通的：App 拨开关 → `push()` 广播 → system_server `Cfg.apply()`
     * 把值写进 `Settings.Global["oupao_oupao_notif_fly"]`（system_server 有写权），
     * SystemUI 用自己的 Context **直接读**（读设置不需要权限、也不需要 root）。
     * 属性仍然读，只作为 adb / root 的手动覆盖（Global 没值时生效）。
     */
    @JvmStatic
    fun notifFly(): Boolean = crossProcGate(K_LIGHT_OPEN)

    /** 能拿来读 Settings.Global 的 Context 候选（包名必须属于本进程 uid，否则会被拒） */
    private fun globalCtxs(): List<Context> {
        val out = ArrayList<Context>()
        runCatching { Rx.ctxB() }.getOrNull()?.let { out.add(it) }   // 本进程 Application
        runCatching { ctx }.getOrNull()?.let { out.add(it) }         // App 注入的
        runCatching { Ctx.sysUi() }.getOrNull()?.let { out.add(it) } // SystemUI
        return out.filter { runCatching { it.packageName != "android" }.getOrDefault(false) }
    }

    /** SystemProperties.get(String, String) —— @hide，走反射（只剩 adb 诊断用途） */
    @JvmStatic
    fun getProp(name: String, def: String): String = runCatching {
        val c = Class.forName("android.os.SystemProperties")
        val m = c.getMethod("get", String::class.java, String::class.java)
        (m.invoke(null, name, def) as? String) ?: def
    }.getOrDefault(def)

    // 3146：propFlag / setProp（persist.oupao.* 读写）已删除。

    @JvmStatic
    fun wordOn(): Boolean = runCatching { getInt(K_SUG_WORD, 0) == 1 }.getOrDefault(false)

    /// 其它应用（非微信/QQ）是否也要屏蔽悬浮通知。默认关。
    @JvmStatic
    fun nohuAll(): Boolean = masterOn() && eggMaster() && getInt(K_NOHU_ALL, 0) != 0

    // ================= 生效应用白名单（留空 = 全部应用） =================

    /** 读白名单：分号分隔的包名列表，空列表表示「不限制」 */
    @JvmStatic
    fun pkgList(key: String): MutableList<String> {
        val out = ArrayList<String>()
        runCatching {
            var raw = getStr(key, "")
            if (raw.startsWith(LIST_V1)) raw = raw.substring(LIST_V1.length)
            raw.split(';').forEach { val s = it.trim(); if (s.isNotEmpty()) out.add(s) }
        }
        return out
    }

    /**
     * 写白名单。
     *
     * ★ 为什么包一层 `v1:` 前缀：`saveStr` 和 `putOneStr` 都会**跳过空串**
     * （当初为防止 App 启动时的空 push 把系统侧已配置项清零而加的保护），
     * 于是「清空名单」如果直接写 ""，广播和系统侧文件都不会更新 →
     * 界面上清了、实际还在拦。加前缀后 `v1:` 是非空值，能正常一路传下去；
     * [pkgList] 读的时候再把前缀剥掉。
     */
    @JvmStatic
    fun saveList(key: String, pkgs: List<String>) {
        runCatching {
            val v = LIST_V1 + pkgs.joinToString(";")
            saveStr(key, v)
            push()
        }
    }

    /** 某包是否在名单内（名单为空 → 视为全部命中） */
        /**
     * 名单是否「读得到」（文件里带 `v1:` 前缀）。
     *
     * 为什么要这个：`inList` 的语义是「**空名单 = 全部应用**」，
     * 但如果某个进程根本读不到名单（SystemUI 的 SELinux 限制），
     * `getStr` 会返回 ""，被当成空名单 → 变成「屏蔽所有应用」。
     * 所以调用方必须先问一句：读得到吗？读不到就退回别的判据。
     */
    @JvmStatic
    fun listReadable(key: String): Boolean = runCatching {
        getStr(key, "").startsWith(LIST_V1)
    }.getOrDefault(false)

    @JvmStatic
    fun inList(key: String, pkg: String): Boolean {
        val l = pkgList(key)
        return l.isEmpty() || l.contains(pkg)
    }

    /** 该包要不要弹「消息头像气泡」= 总开关 && 白名单命中 */
    @JvmStatic
    fun bubbleAppOn(pkg: String): Boolean = sugMsgAll() && inList(K_BUBBLE_APPS, pkg)

    /**
     * 3081：气泡「双击关闭」开关（默认开）。
     * 关掉之后：单击气泡**立刻**打开小窗，不再等 300ms 判定双击（也就没有双击关闭）。
     */
    @JvmStatic
    fun bubDblClose(): Boolean = getBoolRaw(K_BUB_DBL_CLOSE, false)

    /** 长按电源键菜单里的「软重启」按钮（默认开） */
    @JvmStatic
    fun powerSoftReboot(): Boolean =
        runCatching { getStrFresh(K_POWER_SOFT_REBOOT, "0").trim() == "1" }.getOrDefault(false)

    /** 该包要不要「屏蔽悬浮通知」= 总开关 && 白名单命中 */
    @JvmStatic
    fun nohuAppOn(pkg: String): Boolean = nohuAll() && inList(K_NOHU_APPS, pkg)

    // ================= 数值型配置 =================

    private fun intOf(key: String, def: Int, min: Int, max: Int): Int {
        val v = runCatching { getStr(key, def.toString()).trim().toInt() }.getOrNull() ?: def
        return v.coerceIn(min, max)
    }

    /**
     * 3273：振动强度固定 100（不再读 `oupao_*_vib_level` 三个滑块）。
     * 具体强度由系统默认振幅决定（`createOneShot(dur, -1)`），我们只决定时长。
     */
    /**
     * 3275：**振幅跟随系统的触感强度**。
     *
     * ColorOS 的强度滑条存在 `Settings.System.touch_stepless_vibration_intensity`
     * （实测本机 = 2000）。按 0..2000 映射到 1..255：
     * 滑条拉低 → 我们跟着变轻；拉满 → 255（最大）。
     */
    @JvmStatic
    fun sysHapticAmp(): Int {
        return runCatching {
            val c = sysContext() ?: return@runCatching 255
            val v = android.provider.Settings.System.getInt(
                c.contentResolver, "touch_stepless_vibration_intensity", 2000
            )
            val amp = (v * 255 / 2000).coerceIn(1, 255)
            SLog.v("Cfg: 系统触感强度=" + v + " → amp=" + amp)
            amp
        }.getOrDefault(255)
    }

    @JvmStatic
    fun vibLevel(): Int = if (vibMaster()) 100 else 0

    @JvmStatic
    fun flyVibLevel(): Int = if (vibMaster()) 100 else 0

    @JvmStatic
    fun pinVibLevel(): Int = if (vibMaster()) 100 else 0

    @JvmStatic fun iconSize() = intOf(K_ICON_SIZE, 100, 80, 120)
    @JvmStatic fun flyScale() = intOf(K_FLY_SCALE, 100, 80, 120)
    @JvmStatic fun miniScale() = intOf(K_MINI_SCALE, 135, 110, 200)

    /**
     * 侧边栏触发区宽度。
     *
     * 已对照 1741 smali 核实：下界 0x18=24、上界 0x28=40，默认值字符串为 "84"。
     * 也就是说默认值 84 会被夹到 40 —— 这是原实现里的既存矛盾（疑似上界本应是 400）。
     * 为保持行为完全一致，此处**原样保留**上界 40，未在迁移中"顺手修正"。
     * 若确认是笔误，改成 400 即可，但那属于功能变更，请单独验证。
     */
    @JvmStatic fun flyRange() = intOf(K_FLY_RANGE, 40, 40, 80)

    /** 3263：边条形状已移除（用户要求），这里恒返回 0，仅保留调用点兼容 */
    @JvmStatic fun flyZoneMode() = 0

    /** 小窗关闭方式：0=默认 1=? 2=?（取值域 0..2，语义见 Hooks 分发） */
    @JvmStatic
    fun flyClose(): Int {
        if (!(masterOn() && flyopMaster())) return 0
        return intOf(K_FLY_CLOSE, 0, 0, 2)
    }

    @JvmStatic fun miniSwipe() = getNumber(K_FLY_MINI_SWIPE, 150)
    /**
     * 固定应用列表：优先读 Settings.Global（面板在 system_server、菜单管理页在 App 进程，
     * 两边都能读写的共享存储），为空再回退到文件配置。
     */
    /** 「更多应用」呼出方式：panel=悬浮面板 / window=原页面小窗 */
    @JvmStatic
    fun moreMode(): String {
        // 3145+：newest-wins（面板里切换模式写系统侧文件；属主已修正，写入能落地）
        val v = runCatching { rawStr("oupao_more_mode", null) }.getOrNull()
        return if (v.isNullOrEmpty()) "panel" else v
    }

    @JvmStatic
    fun setMoreMode(v: String) {
        runCatching { gset("oupao_more_mode", v) }
        // 2845：App 进程**没有 WRITE_SECURE_SETTINGS**，上面的 gset 在 App 侧是静默失败的，
        //   只能写自己那份文件；而 system_server 读的是 Settings.Global → 一直读到旧值，
        //   表现就是「切到面板模式没反应，永远走小窗」。
        //   → 发 MODE 广播，由 system_server 代写 Global（Ev 里已有接收分支）。
        runCatching {
            com.oupao.wiz.Ev.send(
                com.oupao.wiz.Ev.it("com.oupao.wiz.MODE").putExtra("mode", v)
            )
        }
        // system_server 里能写；App 进程里会被 SELinux 拒，忽略即可
        runCatching { writeStr("$SYS_DIR/oupao_more_mode", v) }
        runCatching { saveStr("oupao_more_mode", v) }
    }

    // ================= 三套固定应用（严格独立） =================
    //  2846：用户明确要求「面板模式 / 小窗模式 / 引索条固定应用面板」三套数据互相独立。
    //  关键：**每套只读自己那个 key，绝不回退到别的 key**。
    //  上几版出"串"就是因为在为空时回退到 flyPins()，再经 push() 灌进另一个 key。

    /** 读某一套固定应用（只看 key 自己） */
    @JvmStatic
    fun pinsOf(key: String): String {
        // 3145+：prefs 与文件按写入时间取较新的那份。
        // 之所以先前"面板 +/- 没反应"：系统侧 `oupao_fly_pins` 被 su 写成了 root:root 0640，
        // system_server 根本写不进去（mtime 一直停在 11-03）→ 写入丢失。
        // 3145+ 已用 su 把 /data/system/oupao_cfg 属主改回 system，写入能落地后，
        // 这里按时间取新值即可（不再需要"只读文件"）。
        val v = runCatching { rawStr(key, "") }.getOrNull() ?: ""
        if (v.isEmpty()) return ""
        return if (v == PINS_EMPTY) "" else pinsDedup(v)
        // **从未写过**这个 key（清空会写哨兵 "0"，不会走到这里）：
        //   对「小窗 / 引索条」两套做**一次性迁移**——拿面板那份当初始值写进自己的 key，
        //   写完就彼此独立了（用户要求三套独立，但不能一升级就变空）。
        // 2847：**不做任何迁移/回退**。空就是空 —— 这是三套独立的前提。
        //   （2846 里这里迁移过 fly→idx/desk，再加上 push() 携带，
        //    导致三个 key 被写成同一份，用户看到的就是「一切换模式就串」。）
        return ""
    }

    /** 写某一套固定应用（文件 + Global + 广播 + push，两条通道都写全） */
    @JvmStatic
    fun writePinsKey(key: String, v: String) {
        val store = if (v.isEmpty()) PINS_EMPTY else v
        runCatching { gset(key, store) }
        runCatching { saveStr(key, store) }
        runCatching { writeStr("$SYS_DIR/$key", store) }
        runCatching {
            com.oupao.wiz.Ev.send(
                com.oupao.wiz.Ev.it("com.oupao.wiz.CFG").putExtra(key, store)
            )
        }
        runCatching { com.oupao.wiz.Ev.send(com.oupao.wiz.Ev.it("com.oupao.wiz.PINS_CHANGED")) }
        runCatching { push() }
        // 3149 诊断：确认"写了"和"回头读到的"是不是同一份（面板 +/- 没反应时看这条）
        runCatching {
            val back = getStrFresh(key, "")
            SLog.i("Cfg: pins 写入 $key len=" + store.length + " 回读 len=" + back.length +
                " 一致=" + (back == store) + " sysMtime=" + File("$SYS_DIR/$key").lastModified())
        }
    }

    /** 面板模式那套 */
    @JvmStatic
    fun flyPins(): String = pinsOf(K_FLY_PINS)

    /** 小窗模式那套 */
    @JvmStatic
    fun deskPins(): String = pinsOf(K_DESK_PINS)

    /** 引索条固定应用面板那套 */
    @JvmStatic
    fun idxPins(): String = pinsOf(K_IDX_PINS)

    @JvmStatic
    fun setIdxPins(v: String) = writePinsKey(K_IDX_PINS, v)

    @JvmStatic
    fun setDeskPins(v: String) = writePinsKey(K_DESK_PINS, v)

    /**
     * 扇形菜单用的固定应用。
     *
     * 2834：**两种模式统一走 [flyPins]。**
     * 以前小窗模式（moreMode=window）直接 getStr(K_FLY_PINS)，走的是**进程内缓存**，
     * 而「更多应用」面板在 system_server 里改完只把新值写进文件/Global，
     * 缓存没失效 → 扇形菜单一直显示旧列表（用户反馈「小窗模式下数据不同步」）。
     * 旧版扇形菜单走的就是 flyPins()，所以旧版是同步的。
     */
    @JvmStatic
    fun fanPins(): String {
        // 2846：按「更多应用」的呼出方式取**对应那一套**（三套彼此独立）
        val v = if ("window".equals(moreMode())) deskPins() else flyPins()
        return if (v == PINS_EMPTY) "" else v
    }

    /**
     * 2841：小窗模式那套固定应用（`K_DESK_PINS`）。
     *
     * 与小窗版「更多应用」页共用；Global 优先（跨进程唯一可靠通道），文件兜底。
     */


    /** 2841：写小窗模式那套（App 侧写文件+广播；system_server 侧直接写 Global/文件） */


    /**
     * 2844：**固定应用的唯一写入口**。
     *
     * App 进程：写自己的文件（仅本进程可读）+ 发广播；
     * system_server 收到广播后回写 Settings.Global ——
     * 那是**唯一两边都能读**的通道（文件权限已实测：互相读不到）。
     */


    /**
     * 2834：**最新的那份优先**（文件 mtime 较新就用文件，否则用 Global）。
     *
     * 以前固定 Global 优先，但「更多应用」小窗页跑在 App 进程，
     * 它改完只能写 App 文件 + 发广播；而广播那条路对 pins 是**不回写 Global**的（见 putOneStr），
     * 于是 Global 永远停在旧值 → 读到的是旧列表。现在按 mtime 取新，两条通道都能生效。
     */


    /**
     * 2834：固定应用去重。
     *
     * 实测同一份配置里会同时存在 `com.tencent.mobileqq`（旧格式，裸包名）和
     * `com.tencent.mobileqq#com.tencent.mobileqq.activity.SplashActivity`（新格式，带 Activity）
     * —— 旧格式是早期版本写进去的残留。不去重的话扇形菜单会画**两个 QQ 图标**，
     * 而且 `Pins.cap5` 只保留 5 个，等于把后面那个（微信）挤掉。
     *
     * 规则：同一包名只留一条，优先留带 `#Activity` 的那条；顺序按首次出现的顺序。
     */
    @JvmStatic
    fun pinsDedup(src: String?): String {
        if (src.isNullOrEmpty()) return ""
        val parts = src.split(';')
        // pkg -> 选中的条目（带 # 的优先）
        val pick = LinkedHashMap<String, String>()
        for (p0 in parts) {
            val p = p0.trim()
            if (p.isEmpty()) continue
            // 2919fix：去重键必须带上分身用户号。
            //   条目格式：主应用 `pkg#Activity`，分身 `pkg#Activity#998`（见 IdxBar/FlyH.cloneScan）。
            //   原来一律只取 `substringBefore('#')`，于是「微信」和「微信分身」同键 →
            //   只能留一个（用户报：加了分身就加不了主应用）。
            val pkg = if (p.startsWith("tool#")) p.substringBefore('#') + "#" + p.substringAfter('#').substringBefore('#')
            else run {
                val segs = p.split('#')
                if (segs.size >= 3) segs[0] + "#" + segs[2] else segs[0]   // pkg + 分身用户号
            }
            val old = pick[pkg]
            if (old == null) {
                pick[pkg] = p
            } else if (!old.contains('#') && p.contains('#')) {
                // 已有裸包名，来了带 Activity 的 → 顶掉（保持原位置）
                pick[pkg] = p
            }
        }
        return pick.values.joinToString(";")
    }

    /**
     * 索引条自己的固定应用（与「更多应用」面板/小窗那套互相独立）。
     *
     * 读取：Settings.Global 优先、文件兜底；**两者都为空时回落 flyPins()**，
     * 这样首次升级不会把索引条已有的 ★ 分组一下子清空。
     * 一旦在索引条面板里增删过，就用自己这份，不再跟随面板。
     */


    /** 保存索引条自己的固定应用（system_server 写 Global、App 侧写文件） */

    @JvmStatic fun getNumber(key: String, def: Int) = intOf(key, def, Int.MIN_VALUE, Int.MAX_VALUE)

    // ================= 读写核心 =================

    @JvmStatic
    fun getInt(key: String, def: Int): Int {
        return runCatching {
            probe()
            cI[key] ?: rawInt(key, def).also { cI[key] = it }
        }.getOrDefault(def)
    }

    /**
     * 读字符串。**默认值可以是 null**（与原版 smali 一致）。
     *
     * 为什么：Java 侧有些调用会传 null 作为默认值（如 Mus.pick 的 getStr(key, null)）。
     * 如果参数声明为非空 String，Kotlin 会在进入函数体前做非空检查并抛 NPE，
     * 异常在调用点抛出、被 catch 吞掉 → 函数等于永返回默认值，表现就是「功能永远不生效」。
     */
    @JvmStatic
    fun getStr(key: String, def: String?): String {
        return runCatching {
            probe()
            cS[key] ?: rawStr(key, def).also { if (it != null) cS[key] = it }
        }.getOrDefault(def) ?: def ?: ""
    }

    @JvmStatic
    fun getBoolRaw(key: String, def: Boolean): Boolean =
        runCatching { getInt(key, if (def) 1 else 0) == 1 }.getOrDefault(def)

    /**
     * **只给"运行时数据"用**（最近应用 `recents`、分身 `clones`、扇形快照…）：
     * system_server 把它们镜像到 Settings.Global，App/桌面直接读。
     *
     * ⚠️ 开关判定**不要**再用它 —— 配置真值现在只有 prefs（→ 文件兜底）。
     */
    @JvmStatic
    fun ggetC(context: Context?, key: String): String {
        val fromSettings = runCatching {
            context?.contentResolver?.let {
                Settings.Global.getString(it, SETTINGS_PREFIX + key)
            }
        }.getOrNull()
        if (!fromSettings.isNullOrEmpty()) return fromSettings
        return getStr(key, "")
    }

    /**
     * 绕过进程级缓存 `cS` 直接读文件（取 App 侧/系统侧里较新的那份）。
     *
     * 用于「App 写、system_server 读」的键——典型是 `oupao_bub_pos`：
     * `getStr` 一旦把空值缓存进 `cS`，只有 `.gen` 变更才会失效；
     * 气泡位置改完不生效、永远是默认位置，就是踩了这个坑。
     */
    @JvmStatic
    fun getStrFresh(key: String, def: String): String =
        runCatching { rawStr(key, def) }.getOrDefault(def) ?: def

    @JvmStatic
    fun gset(key: String, value: String) {
        gsetC(sysContext() ?: ctx, key, value)
    }

    /**
     * 2829：用**调用方给的 Context** 写 Settings.Global。
     *
     * 为什么要多这一个：`gset` 依赖 `sysContext()/ctx`，而那两个只在 App / system_server 里
     * 被初始化过；桌面（launcher）进程里都是 null，于是 `gset` 静默 return，
     * 「多任务界面不呼出引索条」永远写不出值。桌面进程必须用桌面自己的 Context
     * （包名要属于该 uid，否则 Settings 的属主校验会拒）。
     *
     * @return 真的写进去了才 true（写权限不足时 putString 会抛 SecurityException）
     */
    @JvmStatic
    fun gsetC(context: Context?, key: String, value: String): Boolean {
        if (context == null) return false
        return runCatching {
            Settings.Global.putString(context.contentResolver, SETTINGS_PREFIX + key, value)
        }.getOrDefault(false)
    }

    /** 读值：**权威 prefs 优先**；没有 prefs（未迁移 / 老框架 / 无 Context）才退回旧文件通道 */
    /**
     * 3147：prefs 与文件按**写入时间**取较新的那份。
     *
     * 为什么不能无条件信 prefs：hook 进程里的远程 prefs 是**只读**的，
     * system_server / SystemUI 里改的值只能落到系统侧文件（面板固定应用、气泡位置、
     * 更多应用模式…）。如果永远以 prefs 为准，这些改动就会被旧值盖掉
     * ——用户报的「+- 没反应」「小窗模式切不过去」「气泡位置记不住」全是这个。
     */
    private fun rawInt(key: String, def: Int): Int {
        val p = usablePrefs()
        val pv = p?.let { prefInt(it, key) }
        val pt = p?.let { prefTs(it, key) } ?: 0L
        val ft = fileTs(key)
        if (pv != null && (pt >= ft || ft == 0L)) return pv
        val app = readOne("$APP_DIR/$key")
        if (app >= 0) return app
        val sys = probeSys(key)
        if (sys >= 0) return sys
        if (pv != null) return pv
        warn(key)
        return def
    }

    private fun rawStr(key: String, def: String?): String? {
        val p = usablePrefs()
        val pv = p?.let { prefStr(it, key) }
        val pt = p?.let { prefTs(it, key) } ?: 0L
        val ft = fileTs(key)
        if (pv != null && (pt >= ft || ft == 0L)) return pv
        val f = rawStrLegacy(key, null)
        if (f != null && f.isNotEmpty()) return f
        return pv ?: def
    }

    /** prefs 里这条的写入时间戳（3147 起每次写都会带一个 `<key>#t`） */
    private fun prefTs(p: android.content.SharedPreferences, key: String): Long =
        runCatching { p.getLong(key + "#t", 0L) }.getOrDefault(0L)

    /**
     * 3147 一次性补齐时间戳：老键（3145/3146 迁移进来的）没有 `#t`，
     * 若按 0 处理会让**旧的系统侧文件**反过来盖掉 prefs（又是「开关关了还生效」）。
     * 这里给所有已有键盖一个"现在"的时间戳，之后谁写得更晚就听谁的。
     */
    private fun backfillTs(p: android.content.SharedPreferences) {
        runCatching {
            if (p.getBoolean("_ts_v1", false)) return
            val e = p.edit()
            val now = System.currentTimeMillis()
            var n = 0
            for (k in p.all.keys) {
                if (k.endsWith("#t") || k.startsWith("_")) continue
                e.putLong(k + "#t", now)
                n++
            }
            e.putBoolean("_ts_v1", true)
            if (e.commit()) SLog.i("Cfg: 时间戳补齐 n=$n")
        }
    }

    /** App 侧/系统侧文件里较新的那个 mtime */
    private fun fileTs(key: String): Long {
        val a = runCatching { File("$APP_DIR/$key").lastModified() }.getOrDefault(0L)
        val s = runCatching { File("$SYS_DIR/$key").lastModified() }.getOrDefault(0L)
        return maxOf(a, s)
    }

    /** 3144 及以前的读法（只给 prefs 不可用时兜底 / 迁移用） */
    private fun rawStrLegacy(key: String, def: String?): String? = runCatching {
        // 取「较新」的那份：面板(system侧)与菜单管理页(App侧)各写一份，
        // 固定优先 App 侧会导致另一侧的写入永远读不到（表现为改完回档）。
        val appPath = APP_DIR + "/" + key
        val sysPath = SYS_DIR + "/" + key
        val app = readStr(appPath)
        val sys = readStr(sysPath)
        when {
            app == null && sys == null -> def
            app == null -> sys!!
            sys == null -> app
            else -> {
                val at = File(appPath).lastModified()
                val st = File(sysPath).lastModified()
                if (st > at) sys else app
            }
        }
    }.getOrDefault(def)

    /** 读单字节布尔文件：'0'→0，其他→1，不存在→-1 */
    private fun readOne(path: String): Int {
        return runCatching {
            val f = File(path)
            if (!f.exists()) return -1
            FileInputStream(f).use { if (it.read() == '0'.code) 0 else 1 }
        }.getOrDefault(-1)
    }

    /**
     * 直接探测系统侧配置文件的原始值（不做 App 侧回退）。
     *
     * 与 [rawInt] 的区别：rawInt 会先查 App 侧目录，而这里只读
     * `/data/system/oupao_cfg/<key>`，用于「确认系统侧文件到底有没有落盘」。
     * 读不到返回 -2（smali 原实现用的是 -2 而不是 -1）。
     */
    @JvmStatic
    fun probeSys(key: String): Int {
        return try {
            readOne("$SYS_DIR/$key")
        } catch (t: Throwable) {
            -2
        }
    }

    private fun readStr(path: String): String? {
        return runCatching {
            val f = File(path)
            if (!f.exists()) return null
            val len = f.length().toInt()
            if (len <= 0) return null
            val buf = ByteArray(len)
            FileInputStream(f).use { it.read(buf) }
            String(buf).trim()
        }.getOrNull()
    }

    /**
     * 自愈：配置目录里若有 **属主不是本 App** 的文件（例如被 root/su 直接写过），
     * App 进程写不进去 → 开关点了没反应（实测：关了开关仍小窗，因为
     * `oupao_cross_zoom` 被 root 写成 644 root:root）。
     *
     * 这里用一次 su 把它们 chown 回来，每个进程只做一次。
     */
    private var ownerChecked = false

    private fun ensureOwnership() {
        if (ownerChecked) return
        ownerChecked = true
        runCatching {
            val dir = File(APP_DIR)
            val bad = dir.listFiles()?.any { it.isFile && !it.canWrite() } ?: false
            if (!bad) return@runCatching
            val uid = android.os.Process.myUid()
            Runtime.getRuntime()
                .exec(arrayOf("su", "-c", "chown $uid:$uid " + APP_DIR + "/*"))
                .waitFor()
            SLog.i("Cfg: 已修复配置属主 uid=$uid")
        }
    }

    /** 3116：旧版本升级后一次性把所有功能开关写成关闭（主开关保留）。 */
    @JvmStatic
    fun applyDefaultsOffOnce() {
        runCatching {
            // ★ 一次性标记只认**文件**：不能走 getInt()。
            //   3145 把读源切到 prefs 后，这个标记没在 prefs 里 → 每次开机都判成"没跑过"
            //   → 把用户所有开关重新写成关闭（实测：整份配置被清零）。
            if (readOne("$APP_DIR/$K_DEFAULTS_OFF_V1") == 1) return
            chmod("/data/data/com.oupao.wiz", 0x1ED)
            chmod("/data/data/com.oupao.wiz/files", 0x1ED)
            chmod(APP_DIR, 0x1ED)
            ensureOwnership()
            for (k in BOOL_KEYS) {
                if (k == K_EGG_MASTER) continue
                writeOne("$APP_DIR/$k", false)
            }
            writeOne("$APP_DIR/$K_DEFAULTS_OFF_V1", true)
            push()
            SLog.i("Cfg: 已将旧版本功能开关全部改为关闭")
        }.onFailure { SLog.e("Cfg: applyDefaultsOffOnce 失败", it) }
    }

    /**
     * 3140：一次性把「通知收纳」两个开关复位为关闭（App 文件 / Settings.Global / 系统侧文件）。
     *
     * 只复位这两个 key，不动其它开关。用 `K_NOTIFBOX_RESET_V1` 标记保证只跑一次。
     * 复位后用户再手动打开仍会生效（只是不会再“黏”着默认开）。
     */
    @JvmStatic
    fun resetNotifBoxOffOnce() {
        runCatching {
            // 同上：一次性标记只认文件
            if (readOne("$APP_DIR/$K_NOTIFBOX_RESET_V1") == 1) return
            val t = Thread({
                val log = StringBuilder()
                // 1) App 私有文件（uid 自己的，肯定能写）
                runCatching {
                    writeOne("$APP_DIR/$K_NOTIF_BOX", false)
                    writeOne("$APP_DIR/$K_NOTIF_BOX_AUTO", false)
                }.onFailure { log.append("app=").append(it.message).append(' ') }
                // 2) 广播通道：一次性把两个 override 都置 false 再 push
                //    （分开 save 会互相用「Global 优先」把对方读回 1 覆盖掉）
                runCatching { pushNotifBoxBothOff() }
                    .onFailure { log.append("push=").append(it.message).append(' ') }
                // 3) 系统侧文件（prefs 不可用时的兜底）
                runCatching { writeOne("$SYS_DIR/$K_NOTIF_BOX", false); writeOne("$SYS_DIR/$K_NOTIF_BOX_AUTO", false) }
                // 4) 3146：不再校验 Settings.Global（通道已删除），直接落标记
                runCatching { Thread.sleep(300) }
                dbg("notifreset: $log")
                SLog.i("Cfg: notifreset $log")
                writeOne("$APP_DIR/$K_NOTIFBOX_RESET_V1", true)
            }, "Oupao-NotifBoxReset").also { it.isDaemon = true; it.start() }
        }.onFailure { SLog.e("Cfg: resetNotifBoxOffOnce 失败", it) }
    }

    /**
     * 一次性把「通知收纳」两个开关都按 false 发出去。
     *
     * 不能连续调两次 [save]：第二次 push 时第一个 key 的临时覆盖已被清空，
     * `notifBox()` 又会「Global 优先」读回还没被系统侧更新的旧值 1，
     * 结果两个键互相覆盖（实测：一个变 0、另一个又回到 1）。
     */
    private fun pushNotifBoxBothOff() {
        notifBoxPushValue = false
        notifBoxAutoPushValue = false
        try {
            writeOne("$APP_DIR/$K_NOTIF_BOX", false)
            writeOne("$APP_DIR/$K_NOTIF_BOX_AUTO", false)
            push()
        } finally {
            notifBoxPushValue = null
            notifBoxAutoPushValue = null
        }
    }

    /**
     * 3125fix：通知收纳开关由 Settings.Global 作为第一数据源，但 `Cfg.save()` 刚写完 App 文件时，
     * Global 还是旧值；`push()` 如果按 Global 取值就会把旧值原样发回去（点了没反应）。
     * 这里在本次 save 期间临时覆盖 push 的取值，保证系统侧收到用户刚点的值。
     */
    @Volatile private var notifBoxPushValue: Boolean? = null
    @Volatile private var notifBoxAutoPushValue: Boolean? = null

    /** 写 App 侧配置并使缓存失效 + 广播下发 */
    /**
     * 3145：**任意被 hook 的进程**改开关的统一入口（SystemUI 的通知收纳卡片、
     * 桌面、侧边栏设置页…）。
     *
     * 只写权威 prefs + 一份 Settings.Global 兼容镜像：不碰 App 私有目录、
     * 不触发 push()（那些在别人的进程里本来就会失败）。
     */
    @JvmStatic
    fun setBool(key: String, value: Boolean): Boolean {
        val ok = prefPut(key, value)
        // prefs 写不进去时（本机 LSPosed 的远程 prefs 返回空/只读，见 attachRemote 日志）：
        // uid 1000 的进程（SystemUI / 通知管理设置页）能直接写系统侧文件，
        // system_server 读的就是它 —— 否则"设置页里打开通知收纳"会写不进任何一条生效通道。
        if (!ok) runCatching { writeOne("$SYS_DIR/$key", value) }
        // **hook 进程里的远程 prefs 是只读的**（实测 putOk=false），所以非 App 进程的改动
        // 要让 App 用 service 通道写进同一个存储；否则读 prefs 的进程永远看到旧值。
        if (!ok) runCatching {
            if (isSelfProcess()) {
                pendingSets[key] = value          // App 自己还没连上 service：等连上再落
            } else {
                val i = android.content.Intent(ACTION_SET)
                i.setPackage(PKG_SELF)
                i.putExtra("key", key)
                i.putExtra("type", "b")
                i.putExtra("value", value)
                (sysContext() ?: ctx)?.sendBroadcast(i)
            }
        }
        invalidate()
        runCatching { gset(key, if (value) "1" else "0") }
        SLog.i("Cfg: setBool $key=$value prefs=$ok")
        rawLog("setBool $key=$value ok=$ok lpWrite=$lpWritable rpWrite=$rpWritable")
        return ok
    }

    @JvmStatic
    fun save(key: String, value: Boolean) {
        when (key) {
            K_NOTIF_BOX -> notifBoxPushValue = value
            K_NOTIF_BOX_AUTO -> notifBoxAutoPushValue = value
        }
        runCatching {
            // 3145：先写权威源（App 进程/被 hook 进程都能写；写不到再走旧通道）
            prefPut(key, value)
            // 目录权限要 0755、文件 0644，系统侧（uid 1000）才可能直接读到
            // App 侧文件；读不到也没关系，广播会把它写到系统侧文件。
            chmod("/data/data/com.oupao.wiz", 0x1ED)      // 0755
            chmod("/data/data/com.oupao.wiz/files", 0x1ED)
            chmod(APP_DIR, 0x1ED)
            ensureOwnership()
            writeOne("$APP_DIR/$key", value)
            // 3147：App 目录写不进去（别的进程）→ 落到系统侧文件，否则这个开关等于没改
            if (readOne("$APP_DIR/$key") < 0) writeOne("$SYS_DIR/$key", value)
            // 3289：只把改动的这一个键发给 system_server，不再全量 push()。
            //   主开关类仍走全量（push 用的是语义 getter，单键会丢掉 masterOn 门控）。
            if (key in BOOL_KEYS && key !in FULL_PUSH_KEYS) pushOne(key, value) else push()
            // 3291：屏蔽侧边栏开关变化 → 立刻通知侧边栏进程（看门狗不再常驻空转）
            if (key == K_SIDEBAR_NOSWIPE) {
                runCatching {
                    val i = android.content.Intent(com.oupao.wiz.SideDock.ACTION_GATE)
                    i.setPackage("com.coloros.smartsidebar")
                    (sysContext() ?: ctx)?.sendBroadcast(i)
                }
            }
        }
        when (key) {
            K_NOTIF_BOX -> notifBoxPushValue = null
            K_NOTIF_BOX_AUTO -> notifBoxAutoPushValue = null
        }
        // 3146：`persist.oupao.*` 通道**整段删除**。
        // 实测 `SystemProperties.set` 在 system_server 里一直 FAIL，历史上从没生效过；
        // 现在所有判定进程都能读远程 prefs，不需要这条假通道。
        // 「应用间跳转小窗」开关变化时，同步 ColorOS 的 app_start_confirm 设置
        // （开=关掉系统确认弹窗，关=还原）。⚠️ 只能在 **App 进程**做：
        // system_server 读写 Settings.Global 与执行 su 都会被拒（见 JumpConfirm）。
        // ⚠️ 对 QQ 自己画的确认框无效（那是 QQ 进程内的对话框）。
        // 「隐藏桌面图标」：只禁用/启用桌面图标那个 alias，不需要重启桌面，
        // 也不需要桌面作用域（LSPosed 的「打开」走 MODULE_SETTINGS 别名）。
        if (K_HIDE_ICON == key) {
            runCatching {
                val v = value
                Thread({ applyHideIconState(v) }, "Oupao-HideIcon").also { it.isDaemon = true; it.start() }
            }
        }
        if (K_LIGHT_OPEN == key) {
            runCatching {
                val on = value
                val t = Thread({ JumpConfirm.applyFromApp(on) }, "Oupao-JumpConfirm")
                t.isDaemon = true
                t.start()
            }
        }
        // 3146：通知收纳不再需要 su 写 Global —— 它现在走权威 prefs（见 setBool/push 的注释）。
    }

    // 3146：writeFlyProp / writeSwipeProp（persist.oupao.* 属性通道）已整段删除。
    // 判定进程现在都读远程 prefs，属性那条路从来没成功过（setProp 一直 FAIL）。

    @JvmStatic
    fun saveStr(key: String, value: String) {        runCatching {
            // 空串不落盘：否则会把已有的 App 侧配置（如扇形菜单固定应用 oupao_fly_pins）
            // 清成 0 字节文件，而 App 进程又读不到 /data/system/oupao_cfg（目录 0770 system），
            // 结果就是"加了固定应用但页面/扇形菜单一直是空的"。
            if (value.isEmpty()) return@runCatching
            // 3145：先写权威源
            prefPutStr(key, value)
            chmod("/data/data/com.oupao.wiz", 0x1ED)      // 0755
            chmod("/data/data/com.oupao.wiz/files", 0x1ED)
            chmod(APP_DIR, 0x1ED)
            writeStr("$APP_DIR/$key", value)
            // 3147：**hook 进程（system_server / SystemUI / 侧边栏）写不了 App 私有目录**，
            // 但能写 /data/system/oupao_cfg。以前这里写完就算了，于是面板改固定应用、
            // 气泡拖位置、切换更多应用模式在别的进程里全都没落地（用户报的三个"没反应"）。
            if (readStr("$APP_DIR/$key") == null) {
                writeStr("$SYS_DIR/$key", value)
            }
            invalidate()
        }
    }

    private fun writeOne(path: String, value: Boolean) {
        runCatching {
            val f = File(path)
            f.parentFile?.let {
                it.mkdirs()
                it.setReadable(true, false)
                it.setExecutable(true, false)
            }
            FileOutputStream(f).use { it.write(if (value) '1'.code else '0'.code) }
            f.setReadable(true, false)
            chmod(f.absolutePath, 0x1A4) // 0644
            invalidate()
        }
    }

    /** 供 Boot 在系统侧写入主开关值，打通 App 侧 → 系统侧配置通道 */
    @JvmStatic
    fun writeStr(path: String, value: String) {
        runCatching {
            val f = File(path)
            f.parentFile?.let {
                it.mkdirs()
                it.setReadable(true, false)
                it.setExecutable(true, false)
            }
            FileOutputStream(f).use { it.write(value.toByteArray()) }
            f.setReadable(true, false)
            chmod(f.absolutePath, 0x1A4)
            invalidate()
        }
    }

    /** 清缓存并更新 .gen 标记（用于让其他进程感知配置变更） */
    /**
     * 彻底清空一个键：删 App 侧与系统侧两份文件 + 清缓存 + 清 Global。
     *
     * 不能只 delete App 侧文件：getStr() 会读两侧较新的那份，
     * /data/system/oupao_cfg 里的旧值会继续生效（导致「恢复内置应用」无效）。
     * 又因为 saveStr() 拒绝空串，所以只能直接删文件。
     */
    @JvmStatic
    fun clearKey(key: String) {
        runCatching { prefs()?.edit()?.remove(key)?.commit() }
        runCatching { java.io.File("$APP_DIR/$key").delete() }
        runCatching { java.io.File("$SYS_DIR/$key").delete() }
        runCatching { gset(key, "") }
        invalidate()
    }

    private fun invalidate() {
        runCatching {
            cI.clear()
            cS.clear()
            // masterOn 的 TTL 缓存也要清：否则本进程刚写完配置，
            // 最长还要等 1s 才看得到（虽然只影响本进程的即时回显）
            mOnAt = 0L
            FileOutputStream(File("$APP_DIR/.gen")).use { it.write('1'.code) }
        }
    }
    @JvmStatic
    fun inv() = invalidate()

    private fun chmod(path: String, mode: Int) = runCatching { Os.chmod(path, mode) }

    /**
     * 缓存失效探测。
     *
     * 3145 之前靠 `.gen` 文件的 mtime 变化来清缓存 —— 实测**不可靠**：
     * App 侧连续两次写配置落在同一毫秒时 mtime 不变，system_server 的缓存就永远不失效，
     * 表现为「开关关了但气泡还弹」「常驻气泡还是被收掉」这类"偶尔"问题。
     *
     * 现在改成**按时间强制过期**：最多 300ms 就一定重新读文件。
     * 热路径（每次触摸 MOVE 都会问 flyMaster 之类）仍在 300ms 窗口内命中缓存，
     * 不会比原来的 stat() 更慢。
     */
    @Volatile private var cacheAt = 0L

    private fun probe() {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - cacheAt < 300L) return
        cacheAt = now
        cI.clear()
        cS.clear()
        mOnAt = 0L
    }

    private fun warn(key: String) {
        runCatching {
            if (warned.add(key)) SLog.i("Cfg: 读取失败(用默认值) $key")
        }
    }

    // ================= 系统侧 Context =================

    /**
     * 系统侧 Context。
     * 原实现依次尝试 ActivityThread 反射 → FlyH.ctx（hook 到的 SystemUI Context）。
     * FlyH 尚未迁移时先退化为 [ctx]，迁移完成后会补回 FlyH.ctx 兜底。
     */
    @JvmStatic
    fun sysContext(): Context? {
        Ctx.get()?.let { return it }
        return ctx
    }

    @JvmStatic
    fun setCtx(c: Context?) {
        ctx = c
        // 3145：App 进程一进来就建好 prefs（顺手做一次性迁移）
        attachLocal(c)
    }

    // ================= 诊断 =================

    @JvmStatic
    fun dump() {
        runCatching {
            SLog.i("Cfg: 双击关窗=${doubleTapClose()}")
            SLog.i("Cfg: 上滑不暂停=${videoNoPause()}")
            SLog.i("Cfg: 横屏全屏=${landscapeFullscreen()}")
            SLog.i("Cfg: 长按静音=${muteDirect()}")
            SLog.i("Cfg: 贴边保活=${dockKeep()}")
            SLog.i("Cfg: 息屏保活=${screenKeep()}")
            SLog.i("Cfg: 隐藏图标=${hideIcon()}")
            SLog.i("Cfg: 配置目录可读=${File(APP_DIR).exists()}")
            SLog.i("Cfg: sysCfg=${File("$SYS_DIR/$K_EGG_MASTER").exists()}")
            rawDump()
        }
    }

    @JvmStatic
    fun rawDump() {
        listOf(
            K_DOUBLE_TAP_CLOSE, K_VIDEO_NO_PAUSE, K_LANDSCAPE_FULLSCREEN, K_LONG_PRESS_MUTE,
            K_DOCK_KEEP_ALIVE, K_SCREEN_KEEP, K_CAP_CLEAN, K_CORNER_OFF, K_HIDE_ICON,
            // 新增三个功能的开关（排查时一眼能看到系统侧到底写没写进去）
            K_RECENTS_HIDE_FREEFORM, K_ZOOM_MAX_ON, K_ZOOM_MAX, K_ZOOM_CASCADE, K_KG_SLIDE, K_NOTIF_BOX, K_NOTIF_BOX_AUTO
        ).forEach {
            SLog.i("CfgRaw: $it=${readOne("$SYS_DIR/$it")}")
        }
    }

    @JvmStatic
    fun masterProbe(): String {
        return runCatching {
            val app = readStr("$APP_DIR/$K_EGG_MASTER") ?: "-"
            val sys = readStr("$SYS_DIR/$K_EGG_MASTER") ?: "-"
            "oupao: 配置 app=$app sys=$sys 实际=${getStr(K_EGG_MASTER, "1")}"
        }.getOrDefault("oupao: 配置探测失败")
    }

    @JvmStatic
    fun dbg(msg: String) {
        runCatching {
            FileWriter(DBG_PATH, true).use {
                it.write(msg)
                it.write("\n")
            }
        }
    }

    @JvmStatic
    fun fixPerms() {
        chmod("/data/data/com.oupao.wiz", 0x1C9)        // 0711
        chmod("/data/data/com.oupao.wiz/files", 0x1C9)
        chmod(APP_DIR, 0x1ED)                            // 0755
        listOf(
            K_DOUBLE_TAP_CLOSE, K_VIDEO_NO_PAUSE, K_LANDSCAPE_FULLSCREEN, K_LONG_PRESS_MUTE,
            K_DOCK_KEEP_ALIVE, K_SCREEN_KEEP, K_CAP_CLEAN, K_CORNER_OFF, K_SUG_TORCH,
            K_SUG_MSG, K_SUG_KEEP, K_SUG_LINK, K_SUG_MUSIC, K_MUSIC_PKG, K_MUSIC_LIST,
        K_HIDE_ICON
    ).forEach { permOne(it) }
        fixSysDirOwnership()
    }

    /**
     * 3145+：**系统侧配置目录必须归 system**，否则 system_server / SystemUI 写不进去。
     *
     * 实机踩到的：`/data/system/oupao_cfg/oupao_fly_pins` 是 `root:root 0640`（历史上被
     * su 写出来的），于是面板里 +/- 的所有写入都静默失败 —— 引索条/小窗模式那两套
     * 文件属主正常，所以"只有面板模式不生效"。这里用一次 su 把属主改回 system，
     * 并把 0640 放宽到 0664。
     */
    private fun fixSysDirOwnership() {
        runCatching {
            // 3291：只在第一次跑 su。App 每次启动都 chown -R 一遍太慢，
            //   目录属主修好一次就够了；marker 删掉会再修一次。
            val marker = File("$APP_DIR/.perms_sys_v1")
            if (marker.exists()) return
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c",
                "chown -R system:system $SYS_DIR 2>/dev/null; chmod 775 $SYS_DIR; chmod 664 $SYS_DIR/* 2>/dev/null; echo ok"))
            val code = p.waitFor()
            if (code == 0) {
                runCatching {
                    marker.parentFile?.mkdirs()
                    marker.writeText("1")
                }
            }
            SLog.i("Cfg: 系统侧配置目录属主已修正 code=" + code + " owner=" + runCatching {
                java.io.File("$SYS_DIR/$K_FLY_PINS").canWrite()
            }.getOrDefault(false))
        }.onFailure { SLog.e("Cfg: 修正系统侧属主失败", it) }
    }

    private fun permOne(key: String) {
        runCatching {
            val f = File("$APP_DIR/$key")
            if (f.exists()) {
                f.setReadable(true, false)
                chmod(f.absolutePath, 0x1A4)
            }
        }
    }

    /**
     * 【接收方】把广播里带来的开关值落到 /data/system/oupao_cfg/。
     *
     * 与原 smali 逐条对齐（54 个键）：
     *   putOne(intent, key)    —— 布尔量：getIntExtra(key, -1)，<0 忽略，
     *                              否则 writeOne(SYS_DIR/key, v == 1)
     *   putOneStr(intent, key) —— 字符串量：getStringExtra(key)，null 忽略，
     *                              否则 writeStr(SYS_DIR/key, value)
     *   putNumber(intent, key) —— 数字量：Integer 或可解析的 String，
     *                              否则 writeStr(SYS_DIR/key, String.valueOf(v))
     *
     * 注意：这是「从 Intent 读值写本地」，和 push() 的「读本地写 Intent」
     * 正好相反——之前两个方向写反了，系统侧永远收不到用户设置。
     */
    /**
     * apply() 的日志缓冲。
     *
     * 为什么要缓冲：apply() 对 45 个 BOOL_KEYS + 11 个 STR_KEYS + 1 个 number
     * 逐个调 putOne/putOneStr/putNumber，而每个命中原来都打一条
     * `SLog.i("Cfg: 收到广播 $key=$v")` → **一条 CFG 广播最多 57 条日志**。
     * 而 push() 有 9 处 UI 触发点（MA$P/MA$ME/MA$MQ/Sl$E/SlCb/PushR/…），
     * 用户每切一个开关就是一小屏日志。
     *
     * 改成累积到 [applyLog]，在 apply() 末尾汇总成一条，诊断力不减。
     * 只在本线程内使用（apply 是同步单线程执行的），所以不需要加锁。
     */
    private val applyLog = StringBuilder()

    private fun applyLogFlush() {
        runCatching {
            if (applyLog.isNotEmpty()) {
                SLog.i("Cfg: 收到广播 " + applyLog.toString().trim())
                applyLog.setLength(0)
            }
        }
    }

    @JvmStatic
    fun apply(intent: android.content.Intent) {
        BOOL_KEYS.forEach { putOne(intent, it) }
        STR_KEYS.forEach { putOneStr(intent, it) }
        // 原版同序：refreshAll 在 flyop_master 之后
        runCatching { FlyBar.refreshAll() }
        putNumber(intent, K_FLY_MINI_SWIPE)
        // 3146：apply() 不再镜像 `persist.oupao.*` 属性（通道已删除）。
        // 广播本身仍然有两条用处：① 把值写进系统侧文件（prefs 不可用时的兜底）；
        // ② 触发 FlyS/FlyBar 按新配置重建。真正的权威存储是远程 prefs。
        applyLogFlush()
        // 原版最后一句：让角落热区按新的 fly_master/fly_range 立即重建
        runCatching { FlyS.sync() }
        // 3206：开关拨动后立刻同步「侧边栏占用区」，否则要等重启才生效
        runCatching { Cfg.applySidebarRegion() }
        // 跳转确认弹窗的同步放在 App 侧的 save() 里做（system_server 无权改），见 JumpConfirm。
    }

    /**
     * 3206：**把侧边栏占用的那段边缘还给系统**。
     *
     * 实测（`dumpsys input`）：
     *   Remote : 0=RemoteObserver{tag=sidebar, validRegion=[RectF(1143,266,1217,796)]}
     *   GestureMode 排除区 = [1149,135][1216,266] + [1149,796][1216,2640]
     * 即系统的「边缘返回」区域被挖掉了 266~796 这一段 —— 那一段既弹不出侧边栏
     * （被我们屏蔽了），也不响应侧滑返回。
     *
     * 把 `Settings.Secure.sidebar_scene_region` 写成 `0 0 0 0` 后，排除区立刻变成
     * 整条边 [1149,135][1216,2640]，系统原生侧滑返回（带动效）恢复。
     * ROM 的 `FloatHandleController.updateSideBarSceneRegion` 读的就是这个键。
     *
     * 开关关掉时把键删掉，让 ROM 自己重新写回（自愈）。
     */
    @JvmStatic
    fun applySidebarRegion() {
        runCatching {
            val c = sysContext() ?: return
            val cr = c.contentResolver
            val key = "sidebar_scene_region"
            val cur = android.provider.Settings.Secure.getString(cr, key)
            if (sidebarNoSwipe()) {
                if (cur != null && cur != "0 0 0 0") {
                    android.provider.Settings.Secure.putString(cr, key, "0 0 0 0")
                    SLog.i("SideBar: 已清空 sidebar_scene_region（边缘还给系统返回） was=" + cur)
                }
            } else if (cur == "0 0 0 0") {
                android.provider.Settings.Secure.putString(cr, key, null)
                SLog.i("SideBar: 开关关闭，已删除 sidebar_scene_region 让系统重写")
            }
        }
    }

    /**
     * 【发送方】把本地配置按原版语义打包进广播。
     *
     * 原版 push() 是 53 个键逐个 putB / putExtra，取的是**语义值**
     * （eggMaster()、getBoolRaw(key, 默认) …），不是裸的 getInt。
     * 之前用 getInt(key, 0) 会把「文件不存在」当成关，推给系统后主开关恒 0，
     * 表现为「主开关=关 → 跳过功能安装 → 钩子=0 → 功能全失效」。
     */
    @JvmStatic
    fun push() {
        runCatching {
            val c = sysContext() ?: return
            // 原版用 Ev.it(action)：setPackage("android") 定向给 system_server
            val i = com.oupao.wiz.Ev.it("com.oupao.wiz.CFG")
            // —— 主开关类：语义取值（默认开）——
            putB(i, K_EGG_MASTER, eggMaster())
            putB(i, K_FLYOP_MASTER, flyopMaster())
            putB(i, K_SUG_MASTER, sugMaster())
            putB(i, K_APP_KEEP, appKeep())
            putB(i, K_FLY_STAY_OFF, flyStayOff())
            // 字母引索：用 idxBool 取值（文件优先、有默认值），
            // 否则会被当成 false 发出去把系统侧覆盖掉
            putB(i, K_IDX_MASTER, idxMaster())
            putB(i, K_IDX_RIGHT, idxRight())
            putB(i, K_IDX_LEFT, idxLeft())
            putB(i, K_IDX_GLOBAL, idxGlobal())
            putB(i, K_IDX_GAME, idxGameBlock())
            putB(i, K_IDX_VIB, idxVib())
            val sideV = idxSide()
            if (sideV.isNotEmpty()) {
                putS(i, K_IDX_SIDE, sideV)
            }
            // —— 普通布尔开关：getBoolRaw(key, 默认) ——
            for (k in BOOL_KEYS) {
                if (k == K_EGG_MASTER || k == K_FLYOP_MASTER || k == K_SUG_MASTER ||
                    k == K_APP_KEEP || k == K_FLY_STAY_OFF) continue
                // 3125fix：通知收纳的开关写在 ColorOS 设置页，只落了 Settings.Global，
                // 不写 App 私有目录。如果这里按 App 文件（0）push，会把 Global 覆盖回 0，
                // 表现就是“重启/随便动一下别的开关后，通知收纳自己变回关闭”。
                // 这两个 key 必须按“读的时候也是 Global 优先”的语义值发出去。
                val v = when (k) {
                    K_NOTIF_BOX -> notifBoxPushValue ?: notifBox()
                    K_NOTIF_BOX_AUTO -> notifBoxAutoPushValue ?: notifBoxAuto()
                    else -> getBoolRaw(k, BOOL_DEFAULTS[k] ?: false)
                }
                putB(i, k, v)
            }
            // —— 字符串量 ——
            putS(i, K_MUSIC_PKG, getStr(K_MUSIC_PKG, ""))
            putS(i, K_BUB_POS, getStr(K_BUB_POS, ""))
            putS(i, K_BUB_POS_LAND, getStr(K_BUB_POS_LAND, ""))
            // 2848：**push 不再带任何 pins key**。
            //   App 进程读不到最新 Global 时 flyPins() 会回退到它自己那份过期私有文件，
            //   被 push 广播出去 → system_server gsetIfChanged 写回 Global
            //   → 面板重开「复原成旧数据」（实测日志：同一份 len=129 被反复写回）。
            //   三套各自靠 writePinsKey 广播同步，不需要 push 参与。
            // 2847：**push 不再携带小窗/引索两套**。
            //   它们各自写入时走 writePinsKey → 广播 → system_server 回写自己那个 key。
            //   以前放在 push 里会在切换模式时把三套互相灌成一份。
            // 2844：不再把 idx/desk 两个 key 发出去。
            //   它们以前会 `takeIf{isNotEmpty()}`，而 idxPins()/deskPins() 空时
            //   **回退 flyPins()** → 把面板数据灌进另一个 key，
            //   两边互相覆盖——这就是用户看到的「串」。
            putS(i, K_FLY_RANGE, getStr(K_FLY_RANGE, "40"))
            putS(i, K_FLY_CLOSE, getStr(K_FLY_CLOSE, "0"))
            putS(i, K_FLY_SCALE, getStr(K_FLY_SCALE, "100"))
            putS(i, K_ICON_SIZE, getStr(K_ICON_SIZE, "100"))
            // 生效应用白名单：必须显式发出去，否则 apply() 的 putOneStr 收不到，
            // 系统侧文件永远是空的 → 气泡/屏蔽判定在 system_server 里读到空名单。
            putS(i, K_BUBBLE_APPS, getStrFresh(K_BUBBLE_APPS, LIST_V1))   // ★ 必须用新鲜值，否则缓存空值会把系统侧名单永远钉在旧值
            putS(i, K_NOHU_APPS, getStrFresh(K_NOHU_APPS, LIST_V1))   // ★ 必须用新鲜值，否则缓存空值会把系统侧名单永远钉在旧值
            putS(i, K_SUG_INFO_APPS, getStrFresh(K_SUG_INFO_APPS, LIST_V1))   // ★ 必须用新鲜值，否则缓存空值会把系统侧名单永远钉在旧值
            putS(i, K_BUB_INFO_SEC, getStr(K_BUB_INFO_SEC, "5"))
            putS(i, K_BUB_SEC, getStr(K_BUB_SEC, "4"))
            c.sendBroadcast(i)
        }
    }

    @JvmStatic
    fun pushLater() {
        runCatching { android.os.Handler(android.os.Looper.getMainLooper()).post { push() } }
    }

    private fun putB(intent: android.content.Intent, key: String, value: Boolean) {
        runCatching { intent.putExtra(key, if (value) 1 else 0) }
    }

    /** 3289：只下发一个改动过的布尔键（system_server 的 apply 会按 extras 逐键处理） */
    private fun pushOne(key: String, value: Boolean) {
        runCatching {
            val c = sysContext() ?: return
            val i = com.oupao.wiz.Ev.it("com.oupao.wiz.CFG")
            putB(i, key, value)
            c.sendBroadcast(i)
        }
    }

    private fun putS(intent: android.content.Intent, key: String, value: String) {
        runCatching { intent.putExtra(key, value) }
    }

    private fun putOne(intent: android.content.Intent, key: String) {
        runCatching {
            val v = intent.getIntExtra(key, -1)
            if (v < 0) return@runCatching          // 广播里没带这个键
            writeOne("$SYS_DIR/$key", v == 1)
            applyLog.append(key).append('=').append(v).append(' ')
        }
    }

    /** 上一次触发索引条重建的固定应用值（key=value），避免同值重复重建 */
    private var lastPinsBcast: String = ""

    /** 2841：三套固定应用的 key 集合 */
    private fun isPinsKey(k: String) = k == K_FLY_PINS || k == K_IDX_PINS || k == K_DESK_PINS

    private fun putOneStr(intent: android.content.Intent, key: String) {
        runCatching {
            val v = intent.getStringExtra(key) ?: return@runCatching
            // 空串不写：否则 App 启动 push 的空值会把系统侧已配置项清零（扇形固定应用丢失）
            if (v.isEmpty()) return@runCatching
            writeStr("$SYS_DIR/$key", v)
            // 3289：配置键不再镜像 Settings.Global —— 3146 起所有判定进程都读远程 prefs，
            //   crossProcGate 也已删掉 Global 兜底；这里只写系统侧文件。
            if (isPinsKey(key)) {
                runCatching { com.oupao.wiz.SLog.r("r-pins", "apply " + key + " -> " + v, 1000L) }
            }
            // 固定应用变了 → 让字母索引条立刻重建 ★ 分组（否则要等 60s 过期）。
            // 同值不重复触发：push() 每次都会带上固定应用，不比较的话会反复重建
            //（重建要枚举全部应用，跑在 system_server 主线程上）。
            if (isPinsKey(key)) {
                val tag = key + "=" + v
                if (tag != lastPinsBcast) {
                    lastPinsBcast = tag
                    runCatching { com.oupao.wiz.IdxBar.invalidate() }
                    // 2845：面板正在显示时也要重建，否则「已选择」勾选状态是旧的
                    runCatching { com.oupao.wiz.MorePanel.refreshDelayed() }
                }
            }
            applyLog.append(key).append('=').append(v).append(' ')
        }
    }

    private fun putNumber(intent: android.content.Intent, key: String) {
        runCatching {
            val e = intent.extras ?: return@runCatching
            val raw = e.get(key) ?: return@runCatching
            val n = when (raw) {
                is Int -> raw
                is String -> raw.trim().toIntOrNull() ?: return@runCatching
                else -> return@runCatching
            }
            if (n < 0) return@runCatching
            writeStr("$SYS_DIR/$key", n.toString())
            applyLog.append(key).append('=').append(n).append(' ')
        }
    }

    /** 3289：这些键的 push 语义值带 master 门控，必须走全量 push()（单键会丢掉门控） */
    private val FULL_PUSH_KEYS = setOf(
        K_EGG_MASTER, K_FLYOP_MASTER, K_SUG_MASTER, K_APP_KEEP, K_FLY_STAY_OFF
    )

    /** 原版 apply() 里的布尔键 */    private val BOOL_KEYS = listOf(
        K_EGG_MASTER, K_SUG_MASTER, K_DOUBLE_TAP_CLOSE, K_VIDEO_NO_PAUSE,
        K_LANDSCAPE_FULLSCREEN, K_LONG_PRESS_MUTE, K_DOCK_KEEP_ALIVE, K_SCREEN_KEEP,
        K_CAP_CLEAN, K_CORNER_OFF, K_DBG, K_LOG_QUIET, K_SUG_TORCH, K_SUG_MSG, K_SUG_INFO,
        K_BUB_V2, K_BUB_PERSIST, K_BUB_DBL_CLOSE,
        K_POWER_SOFT_REBOOT,
        K_SUG_KEEP, K_APP_KEEP, K_SUG_LINK, K_SUG_WORD,
        K_SUG_MUSIC, K_SUG_INSTALL, K_SUG_WEB, K_SUG_BROWSER,
        K_FLY_STAY_OFF, K_NOHU_ALL,
        K_FLY_MASTER, K_FLY_UI, K_FLY_UI_BAR, K_FLY_BORDER_OFF, K_NO_SPLASH,
        K_HIDE_ICON,
        K_FLY_LEFT,
        K_FLY_RIGHT, K_FLY_MINI, K_FLY_MINI_EASY, K_MINI_BIG, K_GAME_MASTER,
        K_GAME_BLOCK_SUG, K_GAME_BLOCK, K_FLYOP_MASTER,
        // 3114：轻量打开（通知/跳转/设置页三个小窗开关合并）
        K_LIGHT_OPEN,
        K_LIGHT_NOTIF_PORT, K_LIGHT_NOTIF_LAND, K_LIGHT_QS,
        K_LIGHT_JUMP_SETTINGS, K_LIGHT_JUMP_THIRD, K_LIGHT_SHARE, K_LIGHT_TEXT, K_LIGHT_GAME,
        // 2830：屏蔽侧边栏滑动 —— 判定方在 com.coloros.smartsidebar 进程，
        //   以前只有 root 写的系统属性一条通道；现在走「system_server 写 Global、
        //   侧边栏进程自己读」，所以必须登记在这里，否则广播里没这个键、Global 里也没有。
        K_SIDEBAR_NOSWIPE,

        // 字母引索：必须在这里，否则系统侧收不到广播、落不了文件
        K_IDX_MASTER, K_IDX_RIGHT, K_IDX_LEFT, K_IDX_LAUNCHER, K_IDX_GLOBAL, K_IDX_GAME, K_IDX_VIB,
        // 多任务隐藏小窗应用：判定方在**桌面进程**，只能靠 system_server 把值镜像到
        //   Settings.Global（桌面读不到模块私有文件，见 Cfg.gsetC 的注释），
        //   所以必须登记在这里，otherwise 广播里没这个键 → Global 里也没有。
        K_RECENTS_HIDE_FREEFORM,
        // 3502：重启后清空多任务列表（判定方在 system_server，但同样要落文件/广播）
        K_RECENTS_CLEAR_BOOT,
        // 小窗数量上限：判定方在 system_server，读系统侧文件即可，但同样要落文件
        K_ZOOM_MAX_ON,
        // 3352：平行小窗（判定方在 system_server）
        K_SPLIT_WIN,
        // 锁屏滑动输入：判定方在 **SystemUI** 进程（读不到模块私有文件），靠 Global 镜像
        K_KG_SLIDE,
        // 小窗叠放自动错开（system_server 侧判定，普通文件通道即可）
        K_ZOOM_CASCADE,
        // 通知收纳：判定方在 **SystemUI** 进程（同锁屏滑动，靠 Global 镜像 + 属性兜底）
        K_NOTIF_BOX,
        K_NOTIF_BOX_AUTO
    )

    /** 原版 apply() 里的字符串键 */
    private val STR_KEYS = listOf(
        K_MUSIC_PKG, K_BUB_POS, K_BUB_POS_LAND, K_FLY_PINS, K_IDX_PINS, K_DESK_PINS,
        K_FLY_RANGE, K_FLY_CLOSE, K_FLY_SCALE, K_ICON_SIZE, K_IDX_SIDE,
        // 生效应用白名单（留空 = 全部应用）
        K_BUBBLE_APPS, K_NOHU_APPS, K_SUG_INFO_APPS,
        // 气泡时长（秒）：气泡画在 system_server，必须落到系统侧
        K_BUB_INFO_SEC, K_BUB_SEC,
        // 小窗数量上限的数字：判定方在 system_server，必须落到系统侧文件
        K_ZOOM_MAX
    )

    /**
     * 迁移用的全量键集合：布尔 + 字符串 + 少量"不在两个清单里"的历史键。
     * （迁移只搬旧通道里**确实存在**的值，不存在的不写，保持"未设置 → 走默认值"。）
     */
    private val EXTRA_KEYS = listOf(
        K_EGG_MASTER, K_APP_KEEP, K_FLY_STAY_OFF,
        K_MUSIC_CUSTOM, K_MUSIC_HIDDEN, K_MUSIC_LIST, K_MUSIC_CUSTOM_HID,
        "oupao_pins",
        "oupao_more_mode", "clones",
        "oupao_bub2_pos", "oupao_bub2_pos_land",
        "oupao_fan_pins_snapshot", "oupao_recents_visible"
    )

    private val ALL_KEYS: List<String> = (BOOL_KEYS + STR_KEYS + EXTRA_KEYS).distinct()

    /** 原版 getBoolRaw(key, 默认) 里的默认值（true = 开） */
    private val BOOL_DEFAULTS = mapOf(
        K_DBG to false, K_LOG_QUIET to false, K_FLY_MASTER to false,
        // 字母引索：没有配置文件时的默认值（push 会用它发广播，
        // 不写这里会被当成 false 发出去，把系统侧的默认值覆盖掉）
        K_IDX_MASTER to false, K_IDX_RIGHT to false, K_IDX_LEFT to false,
        K_IDX_LAUNCHER to false, K_IDX_GLOBAL to false, K_IDX_GAME to false, K_IDX_VIB to false,
        // 移除应用启动遮罩：默认**关**（不登记会被当成 true 发出去 = 默认开启）
        K_NO_SPLASH to false,
        // 隐藏桌面图标：默认关
        K_HIDE_ICON to false,
        // 消息头像气泡：默认关（用户要求默认关闭，需要时再开）
        K_SUG_MSG to false,
        // 信息气泡默认关
        K_SUG_INFO to false,
        // 新版合并气泡：默认关
        K_BUB_V2 to false,
        // 气泡双击关闭：默认关
        K_BUB_DBL_CLOSE to false,
        // 电源菜单软重启按钮：默认关
        K_POWER_SOFT_REBOOT to false,
        // 气泡常驻：默认关
        K_BUB_PERSIST to false,
        // 全局屏蔽悬浮通知：默认关（很激进，用户主动开才生效）
        K_NOHU_ALL to false,
        // 3114：轻量打开（通知/跳转/设置页合并）默认关
        K_LIGHT_OPEN to false,
        // 场景开关：默认尽量保持 3466 行为（通知/设置/应用间跳转/控制中心开；游戏内跳转关）
        K_LIGHT_NOTIF_PORT to true, K_LIGHT_NOTIF_LAND to true, K_LIGHT_QS to true,
        K_LIGHT_JUMP_SETTINGS to true, K_LIGHT_JUMP_THIRD to true,
        K_LIGHT_SHARE to true, K_LIGHT_TEXT to true, K_LIGHT_GAME to false,
        // 2830：屏蔽侧边栏滑动，默认**关**（2799 起默认不屏蔽；必须与
        // SideDock.swipeAllowed() 的「没写过=放行」保持一致，否则 push 会把它当 true 发出去）
        K_SIDEBAR_NOSWIPE to false,
        // 多任务隐藏小窗应用：默认关（默认行为与出厂一致）
        K_RECENTS_HIDE_FREEFORM to false,
        // 3502：重启后清空多任务列表，默认关（用户主动开才清）
        K_RECENTS_CLEAR_BOOT to false,
        // 小窗数量上限：默认关（= 不干预原厂的并发小窗数量）
        K_ZOOM_MAX_ON to false,
        // 3352：平行小窗默认关（行为与出厂/旧版一致，用户自己开）
        K_SPLIT_WIN to false,
        // 锁屏滑动输入：默认关（会改变锁屏输入方式，用户自己开）
        K_KG_SLIDE to false,
        // 小窗叠放自动错开：默认关（用户要求所有功能开关默认关闭；只在「限制小窗数量」开着时才起作用）
        K_ZOOM_CASCADE to false,
        // 通知收纳：默认关（会改变通知面板的显示逻辑，用户自己开）
        K_NOTIF_BOX to false,
        // 自动收纳静默通知：默认关（用户自己要开才自动收）
        K_NOTIF_BOX_AUTO to false,

    )
}
