package com.oupao.wiz

import java.io.File

/**
 * 3288：一次性清掉历史遗留的**已废弃配置键文件**。
 *
 * 只删下面白名单里的键；活键（pins / 气泡位置 / recents / clones / 能力位 …）一律不在此列。
 * 两个目录分开处理：
 *  - `/data/system/oupao_cfg`：system_server 进程能删（目录属主 system）；
 *  - App 私有 `files/cfg`：只有 App 进程能删（SELinux 不让 root / system_server 动）。
 *
 * 幂等：各自目录写一个 marker，只跑一次。
 */
object LegacyPurge {

    private const val SYS_DIR = "/data/system/oupao_cfg"
    private const val APP_DIR = "/data/data/com.oupao.wiz/files/cfg"

    /** 两侧都可能存在的死键（旧振动键 + 已合并/已移除的开关 + 旧日志计时键） */
    private val COMMON = arrayOf(
        // 3273 已删除的「振动效果」键
        "oupao_vib_master", "oupao_vib_level",
        "oupao_fly_vib", "oupao_fly_vib_level",
        "oupao_pin_vib", "oupao_pin_vib_level",
        "oupao_bub_vib",
        // 3114 合并进 oupao_light_open 的旧开关
        "oupao_notif_fly", "oupao_cross_zoom", "oupao_settings_zoom",
        // 微信/QQ 专用开关，已统一到 sug_msg / sug_info / nohu_all
        "oupao_wx_info", "oupao_qq_info", "oupao_wx_nofloat", "oupao_qq_nofloat",
        "oupao_sug_qq", "oupao_sug_wx",
        // 已移除的功能 / 从未被读取的键
        "oupao_dt_dock", "oupao_mini_ui",
        "oupao_light_scenes_on1", "oupao_fly_zone_mode",
        // 旧「日志耗时」计时键
        "oupao_lo_t_file", "oupao_lo_t_game", "oupao_lo_t_installer",
        "oupao_lo_t_other", "oupao_lo_t_settings", "oupao_lo_t_sysapp", "oupao_lo_t_web"
    )

    /** 只在系统侧出现过的旧键 */
    private val SYS_ONLY = arrayOf(
        "app_start_confirm_prev",   // 旧位置；JumpConfirm 现在写 App 私有目录
        "bdg_probe",
        "oupao_fly_game",
        "oupao_idle_fish", "oupao_idle_fish_tb"
    )

    @JvmStatic
    fun run() {
        purgeSys()
        purgeApp()
    }

    private fun purgeSys() {
        runCatching {
            val dir = File(SYS_DIR)
            if (!dir.canWrite()) return
            val marker = File(dir, "oupao_purge_legacy_sys1")
            if (marker.exists()) return
            var n = 0
            for (k in COMMON + SYS_ONLY) {
                if (File(dir, k).delete()) n++
            }
            runCatching { marker.writeText("1") }
            SLog.i("LegacyPurge: 系统侧已清理 n=$n")
        }
    }

    private fun purgeApp() {
        runCatching {
            val dir = File(APP_DIR)
            if (!dir.canWrite()) return
            val marker = File(dir, "oupao_purge_legacy_app1")
            if (marker.exists()) return
            var n = 0
            for (k in COMMON) {
                if (File(dir, k).delete()) n++
            }
            runCatching { marker.writeText("1") }
            SLog.i("LegacyPurge: App 侧已清理 n=$n")
        }
    }
}
