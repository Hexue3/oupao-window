package com.oupao.wiz;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;

/**
 * 自测用：以 `startActivityForResult` 打开一个链接。
 *
 * 为什么需要它：微信/QQ 的「用浏览器打开」实测是**带 resultTo** 发的
 * （日志 `com.tencent.mobileqq -> mark.via ... act=VIEW`），
 * 而普通 `Context.startActivity` / `am start` 都不带。
 * 只有真正的 Activity + startActivityForResult 才能复现那条路径。
 *
 * 用法（需先开自测属性）：
 * ```
 * setprop persist.oupao.xz_allow_self 1
 * adb shell am start -n com.oupao.wiz/.XzAct --es url "https://www.coolapk.com/feed/74633413"
 * ```
 */
public class XzAct extends Activity {

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        try {
            if (!"1".equals(Cfg.getProp("persist.oupao.xz_allow_self", "0"))) {
                SLog.i("XzAct: 未开启 xz_allow_self，直接退出");
                finish();
                return;
            }
            String url = getIntent().getStringExtra("url");
            if (url == null || url.length() == 0) {
                url = "https://www.coolapk.com/feed/74633413";
            }
            Intent t = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
            Bundle b = ActivityOptions.makeBasic().toBundle();

            // mode=pending：用 PendingIntent 发起，模拟「不进 ATMS 公开入口」的启动路径
            // （微信实测就是这种：只有 ActivityStarter 层有记录）
            String mode = getIntent().getStringExtra("mode");
            if ("pending".equals(mode)) {
                SLog.i("XzAct: PendingIntent 模式 url=" + url);
                android.app.PendingIntent pi = android.app.PendingIntent.getActivity(
                        this, 0, t, android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                | android.app.PendingIntent.FLAG_IMMUTABLE);
                pi.send(this, 0, null, null, null, null, b);
                finish();
                return;
            }

            SLog.i("XzAct: startActivityForResult url=" + url);
            startActivityForResult(t, 1234, b);
            finish();
        } catch (Throwable e) {
            SLog.e("XzAct 失败", e);
            finish();
        }
    }
}
