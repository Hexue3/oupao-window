package com.oupao.wiz;
public class Sug$ZFb implements java.lang.Runnable {
    private final android.content.Intent i;
    private final String p;
    public Sug$ZFb(android.content.Intent p1, String p2)
    {
        this.i = p1;
        this.p = p2;
        return;
    }
    public void run()
    {
        try {
            var it = this.i;
            var want = this.p;
            if ((it == null) || (want == null)) {
                com.oupao.wiz.SLog.i("ZFb: intent/pkg 为空，跳过");
                return;
            }
            // 原版 smali：目标已在前台 → 说明小窗/聊天页已经起来了，
            // 不能再兜底，否则会开第二次聊天页。
            var fg = com.oupao.wiz.Sug.fgPkg();
            boolean hit = (fg != null) && fg.startsWith(want);
            if (hit) {
                com.oupao.wiz.SLog.i("ZFb: 目标已在前台，跳过兜底 fg=" + fg);
                return;
            }
            var ctx = com.oupao.wiz.Sug.ctx;
            if (ctx == null) {
                com.oupao.wiz.SLog.i("ZFb: ctx 为空，跳过");
                return;
            }
            if (com.oupao.wiz.Sug.piSendZoom()) {
                com.oupao.wiz.SLog.i("ZFb: 已用通知 PendingIntent 打开，跳过");
                return;
            }
            // 2910：必须用**通知来的那个用户**启动；否则 QQ/微信有分身时会弹「主应用/分身应用选择」。
            int u = com.oupao.wiz.Sug.notifUserId();
            if (u != 0) {
                com.oupao.wiz.FrameworkExt.startActivityAsUser(ctx, it,
                        com.oupao.wiz.FrameworkExt.userHandleOf(u));
            } else {
                ctx.startActivity(it);
            }
            com.oupao.wiz.SLog.i("ZFb: 小窗未起来, 已普通打开 dat="
                    + it.getDataString() + " fg=" + fg + " want=" + want);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("ZFb: 异常", t);
        }
    }
}
