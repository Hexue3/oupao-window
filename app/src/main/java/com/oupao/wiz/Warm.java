package com.oupao.wiz;
public class Warm {
    public static android.content.Context ctx;
    public Warm()
    {
        return;
    }
    public static void go(android.content.Context p2)
    {
        try {
            com.oupao.wiz.Warm.ctx = p2;
            new Thread(new com.oupao.wiz.WarmR()).start();
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static void run()
    {
        android.content.Context v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Warm.ctx;
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.SLog.e("warm fail", __e_v0_1);
            return;
        }
        if (v0_0 != null) {
            var v1 = android.os.SystemClock.uptimeMillis();
            var v3_3 = com.oupao.wiz.Pins.apps(v0_0);
            if (v3_3 != null) {
                var v4 = v3_3.length;
                int v5 = 0;
                while (v5 < v4) {
                    java.lang.String v6_2 = v3_3[v5];
                    var v7_1 = v6_2.lastIndexOf("\u0001");
                    if (v7_1 >= 0) {
                        v6_2 = v6_2.substring((v7_1 + 1));
                    }
                    var v7_4 = v6_2.indexOf("#");
                    if (v7_4 >= 0) {
                        v6_2 = v6_2.substring(0, v7_4);
                    }
                    com.oupao.wiz.Ico.d(v0_0, v6_2);
                    v5++;
                }
                var v8_1 = (android.os.SystemClock.uptimeMillis() - v1);
                var v3_1 = new StringBuilder();
                v3_1.append("warm n=");
                v3_1.append(v4);
                v3_1.append(" dt=");
                v3_1.append(v8_1);
                com.oupao.wiz.SLog.i(v3_1.toString());
            }
        }
        return;
    }
}