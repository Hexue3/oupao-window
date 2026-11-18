package com.oupao.wiz

import java.io.BufferedReader
import java.io.InputStreamReader

/** 主页运行环境检测：LSPosed 框架 / Root 权限。 */
internal object EnvCheck {

    /** @return connected to detail（本机实际安装的 LSPosed 版本 + API） */
    fun lsp(): Pair<Boolean, String> {
        // 1) 直接读本机 LSPosed 模块自带的 module.prop，版本号/构建号都是真实安装值。
        val prop = runCatching {
            execAll("su", "-c", "cat /data/adb/modules/zygisk_lsposed/module.prop")
        }.getOrDefault("")
        val realVersion = prop.lineSequence()
            .firstOrNull { it.startsWith("version=") }
            ?.removePrefix("version=")?.trim().orEmpty()

        // 2) API 版本来自 system_server 回传的真实框架信息。
        val diag = MainActivity.logCenter?.diagnostic?.value.orEmpty()
        val diagLine = diag.lineSequence().firstOrNull { it.contains("LSPosed") }
        val api = Regex("API\\s*(\\d+)").find(diagLine.orEmpty())
            ?.groupValues?.getOrNull(1)

        if (realVersion.isNotEmpty()) {
            val detail = if (api != null) "$realVersion · API $api" else realVersion
            return true to detail
        }
        if (!diagLine.isNullOrBlank()) {
            val value = diagLine.substringAfter('=', "").trim()
            if (value.isNotEmpty()) return true to value
        }
        val cls = runCatching {
            Class.forName("de.robv.android.xposed.XposedBridge")
        }.getOrNull()
        return if (cls != null) true to "已加载" else false to "未连接"
    }

    /** @return granted to detail（本机 root 管理器版本 + uid） */
    fun root(): Pair<Boolean, String> = runCatching {
        val id = exec("su", "-c", "id").trim()
        if (!id.contains("uid=0")) {
            val installed = java.io.File("/system/bin/su").exists() ||
                java.io.File("/data/adb/ksud").exists()
            return false to if (installed) "已安装，但未授权" else "未检测到 Root"
        }
        // 直接使用本机 root 管理器自己返回的版本串（KernelSU 带 :KernelSU 后缀）。
        val ksud = exec("su", "-c", "ksud -V").removePrefix("ksud ").trim()
        val suV = exec("su", "-c", "su -v").trim()
            .ifBlank { exec("su", "-v").trim() }
        val version = when {
            ksud.isNotEmpty() && !ksud.contains("denied", true) ->
                if (ksud.contains("KernelSU", true)) ksud else "$ksud:KernelSU"
            suV.isNotEmpty() && !suV.contains("denied", true) -> suV
            else -> ""
        }
        val uid = exec("su", "-c", "su -V").trim()
            .ifBlank { exec("su", "-V").trim() }
        val manager = when {
            ksud.isNotEmpty() -> "KernelSU"
            java.io.File("/data/adb/apd").exists() -> "APatch"
            java.io.File("/data/adb/magisk").exists() -> "Magisk"
            else -> ""
        }
        val detail = when {
            version.isNotEmpty() && uid.isNotEmpty() -> "$version ($uid)"
            version.isNotEmpty() -> version
            manager.isNotEmpty() -> "$manager 已授权"
            else -> "已授权"
        }
        true to detail
    }.getOrDefault(false to "未检测到 Root")

    private fun exec(vararg cmd: String): String {
        return execAll(*cmd).lines().firstOrNull { it.isNotBlank() }.orEmpty()
    }

    private fun execAll(vararg cmd: String): String {
        val p = ProcessBuilder(*cmd).redirectErrorStream(true).start()
        val text = BufferedReader(InputStreamReader(p.inputStream)).use { it.readText() }
        p.waitFor()
        return text
    }
}
