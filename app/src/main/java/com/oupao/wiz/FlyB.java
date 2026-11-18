package com.oupao.wiz;
public class FlyB {
    public FlyB()
    {
        return;
    }
    public static void install(ClassLoader p3)
    {
        java.lang.Class v0_2 = null;
        try {
            v0_2 = com.oupao.wiz.Rt.cls(p3, "com.android.server.wm.FlexibleTaskIndicatorWindowManager");
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.SLog.e("FlyB: install fail", __e_v0_1);
            return;
        }
        if (v0_2 != null) {
            var v1_4 = com.oupao.wiz.Rt.methodByName(v0_2, "getBorderColor", 2);
            if (v1_4 != null) {
                com.oupao.wiz.XBridge.hookMethod(v1_4, new com.oupao.wiz.FlyB$C1());
                com.oupao.wiz.SLog.i("FlyB: border color hooked");
            }
            return;
        } else {
            com.oupao.wiz.SLog.i("FlyB: no FlexibleTaskIndicatorWindowManager");
            return;
        }
    }
}