package com.oupao.wiz

import android.content.Context
import android.content.Intent

/**
 * 软重启（只重启 zygote / system_server，不重启内核）。
 *
 * 3131：**不再使用 `ksud soft-reboot`**。
 * 实测在 Zygisk Next + LSPosed 环境里，ksud 会执行各模块的 `emulated-soft-reboot.sh`：
 * Zygisk Next 的脚本调用 `zygiskd exit` 并留下 `/data/adb/modules/zygisksu/disable`，
 * 结果 LSPosed 不再注入模块，电源菜单按钮等所有 hook 全部消失（3128-3130 实测踩到）。
 *
 * 所以回到 init 的 `ctl.restart zygote`：它只重启 zygote 依赖链，zygiskd / lspd 保持存活，
 * LSPosed 仍能注入；代价是模块 APK 更新后可能仍跑旧 dex，模块更新后请完整重启一次。
 *
 * 为什么不在 SystemUI 里直接 `su`：电源菜单按钮挂在 SystemUI（uid 1000），
 * KernelSU 的 su 授权是按 App 包名给的，SystemUI 拿不到 root。
 * 所以按钮只发一个广播：SystemUI → system_server；SELinux 拒绝时再转发 App 用 root 兜底。
 */
object SoftReboot {

    const val ACTION = "com.oupao.wiz.SOFT_REBOOT"

    /** SystemUI 侧调用：先让 system_server 走 ctl.restart；失败再由它转发给 App 用 root 兜底 */
    @JvmStatic
    fun request(ctx: Context?) {
        runCatching {
            ctx?.sendBroadcast(Intent(ACTION).setPackage("android"))
            SLog.i("SoftReboot: 已请求软重启")
        }.onFailure { SLog.e("SoftReboot: request 失败", it) }
    }

    /** system_server 侧执行：直接写 ctl.restart。SELinux 拒绝时转发给 App 兜底。 */
    @JvmStatic
    fun runFromSystem() {
        Thread {
            val ok = runCatching {
                val c = Class.forName("android.os.SystemProperties")
                val m = c.getDeclaredMethod("set", String::class.java, String::class.java)
                m.isAccessible = true
                m.invoke(null, "ctl.restart", "zygote")
                true
            }.getOrDefault(false)
            SLog.i("SoftReboot: system_server set ctl.restart ok=$ok")
            if (!ok) {
                runCatching {
                    val i = Intent(ACTION)
                        .setPackage("com.oupao.wiz")
                        .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES or Intent.FLAG_RECEIVER_FOREGROUND)
                    Cfg.sysContext()?.sendBroadcast(i)
                }
            }
        }.start()
    }

    /** App 侧执行（WarmK 收到广播后调用；不能在主线程跑） */
    @JvmStatic
    fun runFromApp() {
        Thread {
            runCatching {
                SLog.i("SoftReboot: 执行 setprop ctl.restart zygote")
                val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "setprop ctl.restart zygote"))
                val code = p.waitFor()
                SLog.i("SoftReboot: setprop 退出码=$code")
            }.onFailure { SLog.e("SoftReboot: 执行失败", it) }
        }.start()
    }
}
