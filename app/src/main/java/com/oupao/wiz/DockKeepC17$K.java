package com.oupao.wiz;

/** 3380：C17 解锁（keyguardGoingAway）后恢复贴边任务。 */
public class DockKeepC17$K extends com.oupao.wiz.XMH {

    public DockKeepC17$K() {
    }

    public void afterHookedMethod(com.oupao.wiz.XMP p) {
        com.oupao.wiz.DockKeepC17.onUnlock();
    }
}
