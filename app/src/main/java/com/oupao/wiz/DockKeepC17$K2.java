package com.oupao.wiz;

/** 3381：C17 OPLUS 锁屏状态变化，keyguardShowing=false 时恢复贴边任务。 */
public class DockKeepC17$K2 extends com.oupao.wiz.XMH {

    public DockKeepC17$K2() {
    }

    public void afterHookedMethod(com.oupao.wiz.XMP p) {
        try {
            if (p.args != null && p.args.length > 0) {
                Object showing = p.args[0];
                if (Boolean.FALSE.equals(showing)) {
                    com.oupao.wiz.SLog.i("DockKeepC17: notifyKeyguardStateChanged keyguard=false");
                    com.oupao.wiz.DockKeepC17.onUnlock();
                }
            }
        } catch (Throwable ignore) {
        }
    }
}
