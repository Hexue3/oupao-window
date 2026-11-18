package com.oupao.wiz;

/** 3381：C17 亮屏回调（无锁屏时靠这条）。 */
public class DockKeepC17$S extends com.oupao.wiz.XMH {

    public DockKeepC17$S() {
    }

    public void afterHookedMethod(com.oupao.wiz.XMP p) {
        try {
            if (p.args != null && p.args.length > 0 && Boolean.TRUE.equals(p.args[0])) {
                com.oupao.wiz.SLog.i("DockKeepC17: onScreenAwakeChanged true");
                com.oupao.wiz.DockKeepC17.onUnlock();
            }
        } catch (Throwable ignore) {
        }
    }
}
