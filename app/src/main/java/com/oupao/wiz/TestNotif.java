package com.oupao.wiz;

/**
 * 测试用的通知发送器（导出广播）。
 *
 * 为什么需要它：`adb shell cmd notification post` 发的是 `shell_cmd` 渠道、
 * 优先级太低，**根本不会进入横幅判定**，所以没法用它验证
 * 「屏蔽悬浮通知」/「简洁横幅」这类功能。这里用模块自己的包发一条
 * 高优先级（IMPORTANCE_HIGH）通知，走的是真正的横幅链路。
 *
 * 用法：
 *   adb shell am broadcast -a com.oupao.wiz.TEST_NOTIF -n com.oupao.wiz/.TestNotif
 *   adb shell am broadcast -a com.oupao.wiz.TEST_NOTIF_CANCEL -n com.oupao.wiz/.TestNotif
 *
 * 注意：模块包名不在微信/QQ 之内，所以它命中的是**全局开关 oupao_nohu_all**，
 * 正好用来验证全局屏蔽。
 */
public class TestNotif extends android.content.BroadcastReceiver {

    private static final String CHANNEL = "oupao_test";
    private static final int ID = 0x0F10;

    public void onReceive(android.content.Context c, android.content.Intent i) {
        if (c == null) {
            return;
        }
        try {
            android.app.NotificationManager nm =
                    (android.app.NotificationManager) c.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
            if (nm == null) {
                return;
            }
            String act = (i == null) ? null : i.getAction();
            if ("com.oupao.wiz.TEST_NOTIF_CANCEL".equals(act)) {
                nm.cancel((i == null) ? ID : i.getIntExtra("nid", ID));
                SLog.i("TestNotif: 已撤销测试通知");
                return;
            }
            // 调试用：把两个「生效应用」名单清空（= 界面默认全关），并关掉屏蔽总开关。
            // 因为 /data/system/oupao_cfg 只有 system_server 能写，root 也写不了，
            // 所以必须借 App 进程走 Cfg.save*/push 这条正规通道。
            if ("com.oupao.wiz.RESET_LISTS".equals(act)) {
                Cfg.saveList(Cfg.K_BUBBLE_APPS, new java.util.ArrayList<String>());
                Cfg.saveList(Cfg.K_NOHU_APPS, new java.util.ArrayList<String>());
                Cfg.save(Cfg.K_NOHU_ALL, false);
                Cfg.save(Cfg.K_SUG_MSG, false);
                SLog.i("TestNotif: 已清空两个名单并关闭屏蔽总开关");
                return;
            }
            // 调试用：开关「轻量打开」（通知点击/应用间跳转/设置页 合并后的开关）
            if ("com.oupao.wiz.FLY_ON".equals(act) || "com.oupao.wiz.FLY_OFF".equals(act)) {
                boolean on = "com.oupao.wiz.FLY_ON".equals(act);
                Cfg.save(Cfg.K_LIGHT_OPEN, on);
                SLog.i("TestNotif: light_open=" + on);
                return;
            }

            // 3288：一次性清掉 App 私有目录里的历史死键文件（root/system_server 删不了）
            if ("com.oupao.wiz.PURGE_LEGACY".equals(act)) {
                com.oupao.wiz.LegacyPurge.run();
                SLog.i("TestNotif: LegacyPurge 已执行");
                return;
            }

            // 高优先级渠道 —— 这是能弹横幅的关键
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                android.app.NotificationChannel ch = new android.app.NotificationChannel(
                        CHANNEL, "测试通知", android.app.NotificationManager.IMPORTANCE_HIGH);
                ch.setDescription("欧泡小窗优化 · 功能自测");
                ch.enableVibration(true);
                nm.createNotificationChannel(ch);
            }

            android.content.Intent open = new android.content.Intent(c, MainActivity.class);
            open.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            int pf = 0x04000000;   // FLAG_IMMUTABLE (API28+)
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                pf |= 0x08000000;  // FLAG_UPDATE_CURRENT
            }
            android.app.PendingIntent pi =
                    android.app.PendingIntent.getActivity(c, 0, open, pf);

            android.app.Notification.Builder b;
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                b = new android.app.Notification.Builder(c, CHANNEL);
            } else {
                b = new android.app.Notification.Builder(c);
            }
            // 自测可传文案：--es txt "长文本" / --es title "标题"
            // 自测：--es clip "文本" → 从 App 进程写剪贴板，触发模块的剪贴板链接气泡(kind=4)
            String clip = (i == null) ? null : i.getStringExtra("clip");
            if (clip != null && !clip.isEmpty()) {
                try {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager)
                            c.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("t", clip));
                    com.oupao.wiz.SLog.i("TestNotif: clip 已写入 len=" + clip.length());
                } catch (Throwable t) {
                    com.oupao.wiz.SLog.e("TestNotif: clip 失败", t);
                }
                com.oupao.wiz.SLog.flushNow();
                return;
            }
            // 自测：--ei fake 5 直接跑一次非消息类气泡（kind=5），用来验证「形态合并」不影响它们
            int fake = (i == null) ? 0 : i.getIntExtra("fake", 0);
            if (fake != 0) {
                // 3323：允许自测「点气泡时前台有全屏应用」这条链路：
                //   am broadcast ... --ei fake 2 --es pkg com.tencent.mm
                // 不指定 pkg 时保持原来的"自己给自己弹"行为。
                String fp = (i == null) ? null : i.getStringExtra("pkg");
                if ((fp == null) || (fp.length() == 0)) {
                    fp = "com.oupao.wiz";
                }
                com.oupao.wiz.Sug.pkg = fp;
                com.oupao.wiz.Sug.kPkg = fp;
                // 3325：模拟分身气泡 —— --ei uid 99910386（0=主应用）
                int fuid = (i == null) ? -1 : i.getIntExtra("uid", -1);
                if (fuid > 0) {
                    com.oupao.wiz.Sug.uid = fuid;
                }
                com.oupao.wiz.Sug.kChat = null;
                com.oupao.wiz.Sug.kUin = null;
                com.oupao.wiz.Sug.kType = null;
                com.oupao.wiz.Sug.kNi = null;
                com.oupao.wiz.Sug.kPi = null;
                com.oupao.wiz.Sug.txt = "";
                com.oupao.wiz.Sug.bmp = com.oupao.wiz.Sug.circleFitIcon(
                        ((android.content.pm.PackageManager) c.getPackageManager())
                                .getApplicationIcon("com.oupao.wiz"), com.oupao.wiz.Ui.dp(c, 46));
                com.oupao.wiz.Sug.testBypass = true;
                com.oupao.wiz.SLog.i("TestNotif: fake 准备 kind=" + fake);
                com.oupao.wiz.SLog.flushNow();
                try {
                    com.oupao.wiz.Sug.show(fake);
                    com.oupao.wiz.SLog.i("TestNotif: fake 已调用 show kind=" + fake);
                } catch (Throwable t) {
                    com.oupao.wiz.SLog.e("TestNotif: fake show 异常", t);
                }
                com.oupao.wiz.Sug.testBypass = false;
                com.oupao.wiz.SLog.flushNow();
                return;
            }
            String xt = (i == null) ? null : i.getStringExtra("txt");
            String xtt = (i == null) ? null : i.getStringExtra("title");
            b.setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle((xtt == null || xtt.isEmpty()) ? "欧泡测试通知" : xtt)
                    .setContentText((xt == null || xt.isEmpty()) ? "用来验证横幅屏蔽是否生效" : xt)
                    .setAutoCancel(true)
                    .setContentIntent(pi)
                    .setCategory(android.app.Notification.CATEGORY_MESSAGE);
            if (android.os.Build.VERSION.SDK_INT < 26) {
                b.setPriority(android.app.Notification.PRIORITY_HIGH);
            }
            nm.notify((i == null) ? ID : i.getIntExtra("nid", ID), b.build());
            SLog.i("TestNotif: 已发送测试通知");
        } catch (Throwable t) {
            SLog.e("TestNotif: 发送失败", t);
        }
    }
}
