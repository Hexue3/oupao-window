package com.oupao.wiz

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * 「通知收纳」的跳转入口。
 *
 * 模块「Flyme式UI」页里那一行「通知收纳」已经不是开关了（开关在 ColorOS 设置页里），
 * 点它就打开 **设置 → 通知与控制中心**（`android.settings.NOTIFICATION_SETTINGS` 在本机正好
 * 由 `com.oplus.notificationmanager/.NotificationCenterActivity` 接住，也就是我们插了
 * 「通知收纳」那一行的那页）。
 */
object NotifBoxLauncher {

    @JvmStatic
    fun openNotificationSettings() {
        val ctx: Context? = runCatching { Ui.appCtx }.getOrNull() ?: runCatching { Ctx.get() }.getOrNull()
        if (ctx == null) {
            SLog.i("NBX: 打开通知设置失败（拿不到 Context）")
            return
        }
        runCatching {
            val i = Intent("android.settings.NOTIFICATION_SETTINGS")
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // 让设置页打开后自动滚到我们的「通知收纳」卡片并闪一下。
            // ★ 用 Settings.Global 做一次性标记（intent extra 在 singleTop 复用实例时会丢）
            runCatching { android.provider.Settings.Global.putInt(ctx.contentResolver, "oupao_notifbox_scroll", 1) }
            i.putExtra("oupao_scroll_to_notifbox", true)
            ctx.startActivity(i)
            SLog.i("NBX: 跳到 设置→通知与控制中心（带 scroll 标记）")
        }.onFailure { SLog.e("NBX: 跳转通知设置失败", it) }
    }
}
