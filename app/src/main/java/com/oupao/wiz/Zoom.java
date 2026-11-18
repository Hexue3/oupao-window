package com.oupao.wiz;
public class Zoom {
    public Zoom()
    {
        return;
    }
    public static void install(ClassLoader p3)
    {
        java.lang.Class v0_2 = null;
        try {
            v0_2 = com.oupao.wiz.Rt.cls(p3, "com.android.server.wm.OplusZoomWindowManagerService");
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_2 != null) {
            var v0_3 = com.oupao.wiz.Rt.methodByName(v0_2, "startZoomWindow", 4);
            if (v0_3 != null) {
                com.oupao.wiz.XBridge.hookMethod(v0_3, new com.oupao.wiz.Zoom$H());
                com.oupao.wiz.Diag.put("Zoom", "1");
                com.oupao.wiz.SLog.i("Zoom: \u5df2\u6302\u94a9 startZoomWindow");
            }
        }
        return;
    }
}