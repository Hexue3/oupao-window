package com.oupao.wiz;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * 自测工具：以**模块自身身份**（真实 App uid）发起一次跨应用跳转。
 *
 * 为什么要它：shell 的 `am start` 与真实 App 有两点不同
 *  1. callingPackage 不同（com.android.shell vs 真实包名）；
 *  2. 真实 App 通常**会传一个非空 ActivityOptions Bundle**，而 `am start` 传 null。
 * 这两点都可能影响小窗参数是否被系统采纳，所以需要这个通道来做对照实验。
 *
 * 用法：
 * ```
 * adb shell am broadcast -a com.oupao.wiz.XZTEST -n com.oupao.wiz/.XzTest \
 *     --es url "https://www.coolapk.com/feed/74633413"
 * ```
 * 注意：判定里默认跳过本模块包名，测试时要先
 * `setprop persist.oupao.xz_allow_self 1`。
 */
public class XzTest extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent i) {
        try {
            // 安全阀：本接收器是导出的自测通道，必须显式打开属性才生效，
            // 否则任何应用都能借它拉起任意 URL。
            if (!"1".equals(Cfg.getProp("persist.oupao.xz_allow_self", "0"))) {
                SLog.i("XzTest: 未开启 xz_allow_self，忽略");
                return;
            }
            String url = i.getStringExtra("url");
            if (url == null || url.length() == 0) {
                url = "https://www.coolapk.com/feed/74633413";
            }
            Intent t = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
            t.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // 传 null 还是非空 Bundle 由 extra 控制，用来隔离「调用方是否自带 options」的影响。
            // 真实 App（微信/QQ）通常传非空；`am start` 传 null。
            String nb = i.getStringExtra("bundle");
            boolean useNull = "null".equalsIgnoreCase(nb);

            if (useNull) {
                c.startActivity(t);
            } else {
                Bundle b = android.app.ActivityOptions.makeBasic().toBundle();
                b.putString("oupao_test_tag", "1");
                c.startActivity(t, b);
            }
            SLog.i("XzTest: 已发起 url=" + url + " nullBundle=" + useNull);
        } catch (Throwable e) {
            SLog.e("XzTest 失败", e);
        }
    }
}
