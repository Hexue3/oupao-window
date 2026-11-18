package com.oupao.wiz

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 3146：跨进程改开关的落点。
 *
 * 被 hook 的进程里 `getRemotePreferences` 是**只读**的（实测 putOk=false），
 * 所以 SystemUI / ColorOS 设置页里改的通知收纳开关不能直接写进权威存储。
 * 它们发 `com.oupao.wiz.SET` 给模块 App，由 App 通过框架 service 写进去，
 * 所有进程随后读到同一个值。
 */
class CfgSet : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        runCatching {
            val app = context.applicationContext ?: context
            Cfg.setCtx(app)
            val key = intent.getStringExtra("key") ?: return
            val v = intent.getBooleanExtra("value", false)
            Cfg.setBool(key, v)
            android.util.Log.i("OupaoCfg", "SET 收到 $key=$v")
        }.onFailure {
            android.util.Log.e("OupaoCfg", "SET 失败", it)
        }
    }
}
