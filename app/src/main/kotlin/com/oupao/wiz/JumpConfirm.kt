package com.oupao.wiz

import java.io.File

/**
 * 关掉 ColorOS 的「应用跳转确认」弹窗。
 *
 * 现象：跳到别的应用时系统先弹「即将离开 XX 打开其他应用」
 * （`AppStartConfirmDialogActivity`），要人点一下才继续。
 * 来源：`Settings.Global.app_start_confirm_rus_enable`（出厂 1），置 0 即消失。
 *
 * ## ⚠️ 只能在 **App 进程** 执行
 *
 * 实测在 system_server 里 **读不到也写不进** `Settings.Global`
 * （`getInt` 返回默认值、`putInt` 抛异常），而且 system_server **执行不了 `su`**
 * （SELinux 拒）。所以这里只用 root 通道，并且由 App 进程调用：
 * 用户拨「应用间跳转小窗」开关时（[Cfg.save]）触发。
 *
 * ## 策略：跟着开关走，不永久改用户设备
 *
 * - 开关开 → 写 0，并先把原值记到 `/data/system/oupao_cfg/app_start_confirm_prev`；
 * - 开关关 → 把原值写回、删掉记录。
 */
object JumpConfirm {

    private const val KEY = "app_start_confirm_rus_enable"
    /**
     * 记录原值的位置：**必须放 App 自己的目录**。
     * 早先放 `/data/system/oupao_cfg/` 写不进去（App 进程没有 /data/system 写权限，
     * 那条路径只有 system_server 能写），导致还原时读不到原值、错误地回落成 1。
     */
    private const val REMEMBER = "/data/data/com.oupao.wiz/files/cfg/app_start_confirm_prev"

    /** 由 App 进程在开关变化时调用（后台线程里跑，别阻塞 UI） */
    @JvmStatic
    fun applyFromApp(on: Boolean) {
        runCatching {
            val now = readGlobal()
            if (on) {
                if (readPrev() == null && now >= 0) writePrev(now)
                writeGlobal(0)
                SLog.i("JumpConfirm: 目标=关 原值=" + readPrev() + " 现值=" + readGlobal())
            } else {
                val prev = readPrev() ?: 1
                writeGlobal(prev)
                SLog.i("JumpConfirm: 目标=还原($prev) 现值=" + readGlobal())
            }
        }.onFailure { SLog.e("JumpConfirm 失败", it) }
    }

    private fun writePrev(v: Int) {
        runCatching {
            val f = File(REMEMBER)
            f.parentFile?.mkdirs()
            f.writeText(v.toString())
        }
    }

    private fun readGlobal(): Int = runCatching {
        exec("settings get global $KEY")?.trim()?.toIntOrNull() ?: -1
    }.getOrDefault(-1)

    private fun writeGlobal(v: Int): Boolean = runCatching {
        exec("settings put global $KEY $v")
        readGlobal() == v
    }.getOrDefault(false)

    private fun readPrev(): Int? = runCatching {
        File(REMEMBER).readText().trim().toIntOrNull()
    }.getOrNull()

    /** 跑一条 root 命令并返回 stdout（与 [Cfg.writeFlyProp] 同一通道，已验证可用） */
    private fun exec(cmd: String): String? = runCatching {
        val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
        val out = p.inputStream.bufferedReader().readText()
        p.waitFor()
        out
    }.getOrNull()
}
