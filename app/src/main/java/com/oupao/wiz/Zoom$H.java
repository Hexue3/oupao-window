package com.oupao.wiz;
public class Zoom$H extends com.oupao.wiz.XMH {
    public Zoom$H()
    {
        return;
    }
    public void beforeHookedMethod(com.oupao.wiz.XMP p7)
    {
        java.lang.Object[] v0 = null;
        try {
            v0 = p7.args;
        } catch (Throwable __e_v0) {
            return;
        }
        if ((v0 != null) && (v0.length >= 4)) {
            var v2_1 = v0[0];
            // 原来直接 String.valueOf(args[0]) = Intent 全量 toString（含 extras），
            // extras 带 Parcelable/Bitmap 时开销不可控。改成只打组件名 + extras 数量。
            // 三条都降 verbose：startZoomWindow 每次小窗启动走一次。
            if (v2_1 instanceof android.content.Intent) {
                var it = (android.content.Intent) v2_1;
                var ex = it.getExtras();
                com.oupao.wiz.SLog.v("Zoom: intent comp=" + it.getComponent()
                        + " extras=" + (ex == null ? "none" : Integer.valueOf(ex.keySet().size())));
            } else {
                com.oupao.wiz.SLog.v("Zoom: intent type=" + (v2_1 == null ? "null" : v2_1.getClass().getName()));
            }
            com.oupao.wiz.SLog.v("Zoom: call=" + String.valueOf(v0[3]));
            com.oupao.wiz.SLog.v("Zoom: user=" + String.valueOf(v0[2]));
            com.oupao.wiz.Sug.noteChat(v2_1);
        }
        return;
    }
}