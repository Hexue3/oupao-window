package com.oupao.wiz;
public class Mute {
    static Object sPam;
    static java.util.HashSet sSet;
    private Mute()
    {
        return;
    }
    public static void install(ClassLoader p3)
    {
        java.lang.Class v0_5 = null;
        try {
            com.oupao.wiz.Mute.sSet = new java.util.HashSet();
            v0_5 = com.oupao.wiz.Rt.cls(p3, "com.android.server.audio.PlaybackActivityMonitorExtImpl");
        } catch (Throwable __e_v0_3) {
            com.oupao.wiz.SLog.e("Mute: install \u5f02\u5e38", __e_v0_3);
            return;
        }
        if (v0_5 == null) {
            com.oupao.wiz.Diag.put("Mute", "0");
            com.oupao.wiz.SLog.i("Mute: \u672a\u627e\u5230 PlaybackActivityMonitorExtImpl");
            return;
        } else {
            com.oupao.wiz.XBridge.hookAllConstructors(v0_5, new com.oupao.wiz.Mute$C1());
            com.oupao.wiz.SLog.i("Mute: \u9759\u97f3\u901a\u9053\u6302\u94a9\u5b8c\u6210");
            return;
        }
    }
    private static void logToggle(boolean p4, int p5, String p6)
    {
        java.lang.String v1_2 = null;
        var vtmp4 = new StringBuilder("\u957f\u6309\u9759\u97f3: pkg=").append(p6).append(" uid=").append(p5).append(" -> ");
        if (!p4) {
            v1_2 = "\u5df2\u9759\u97f3";
        } else {
            v1_2 = "\u5df2\u6062\u590d";
        }
        com.oupao.wiz.SLog.i(vtmp4.append(v1_2).toString());
        return;
    }
    private static boolean setVol(float p7, int p8, String p9)
    {
        java.lang.Object v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Mute.sPam;
        } catch (Throwable __e_v0_2) {
            com.oupao.wiz.SLog.e("Mute: setVolumeForUid \u5f02\u5e38", __e_v0_2);
            return false;
        }
        if (v0_0 == null) {
            return false;
        } else {
            var v1_1 = v0_0.getClass();
            Class[] v2_4 = new Class[4];
            v2_4[0] = Float.TYPE;
            v2_4[1] = Integer.TYPE;
            v2_4[2] = String.class;
            v2_4[3] = Boolean.TYPE;
            var v1_0 = com.oupao.wiz.Rt.method(v1_1, "setVolumeForUid", v2_4);
            if (v1_0 == null) {
                return false;
            } else {
                v1_0.setAccessible(true);
                Object[] v2_2 = new Object[4];
                v2_2[0] = Float.valueOf(p7);
                v2_2[1] = Integer.valueOf(p8);
                java.lang.String v4_5 = p9;
                if (p9 == null) {
                    v4_5 = "";
                } else {
                }
                v2_2[2] = v4_5;
                v2_2[3] = Boolean.TRUE;
                try {
                    v1_0.invoke(v0_0, v2_2);
                } catch (Throwable __e_io) {
                }
                return true;
            }
        }
    }
    public static void toggle(int p6, String p7)
    {
        if (com.oupao.wiz.Mute.sPam != null) {
            java.util.HashSet v1_0 = com.oupao.wiz.Mute.sSet;
            if (v1_0 == null) {
                v1_0 = new java.util.HashSet();
                com.oupao.wiz.Mute.sSet = v1_0;
            }
            var v2 = Integer.valueOf(p6);
            var v3 = v1_0.contains(v2);
            float v4 = 0;
            if (v3) {
                v4 = 1.0f;
            }
            if (com.oupao.wiz.Mute.setVol(v4, p6, p7)) {
                if (!v3) {
                    v1_0.add(v2);
                } else {
                    v1_0.remove(v2);
                }
                com.oupao.wiz.Mute.logToggle(v3, p6, p7);
                return;
            } else {
                com.oupao.wiz.SLog.i(new StringBuilder("\u957f\u6309\u9759\u97f3: \u8c03\u7528\u5931\u8d25 pkg=").append(p7).toString());
                return;
            }
        } else {
            com.oupao.wiz.SLog.i("Mute: \u901a\u9053\u672a\u5c31\u7eea");
            return;
        }
    }
}