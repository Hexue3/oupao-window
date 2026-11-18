package com.oupao.wiz;
public class PinIdx implements android.view.View.OnClickListener {
    private android.view.View card;
    private String letter;
    public PinIdx(String p1, android.view.View p2)
    {
        this.letter = p1;
        this.card = p2;
        return;
    }
    private static android.view.View find(android.view.View p6, String p7)
    {
        try {
            if ((p6 instanceof android.widget.TextView)) {
                android.widget.TextView p6_c = ((android.widget.TextView) p6);
                if (((android.widget.TextView) p6_c).getText().toString().equalsIgnoreCase(p7)) {
                    return ((android.widget.TextView) p6_c);
                }
            }
        } catch (Throwable __e_v5) {
            return null;
        }
        if ((p6 instanceof android.view.ViewGroup)) {
            var v1 = ((android.view.ViewGroup) p6).getChildCount();
            int v2 = 0;
            while (v2 < v1) {
                var v4_1 = com.oupao.wiz.PinIdx.find(((android.view.ViewGroup) p6).getChildAt(v2), p7);
                if (v4_1 == null) {
                    v2++;
                } else {
                    return v4_1;
                }
            }
        }
        return null;
    }
    public static void go(android.view.View p2, String p3)
    {
        try {
            if ((p2 != null) && (p3 != null)) {
                com.oupao.wiz.PinIdx.vib(p2);
                if (!"\u2605".equals(p3)) {
                    var v0_2 = com.oupao.wiz.PinIdx.find(p2, p3);
                    var v1_0 = new StringBuilder();
                    v1_0.append("go ");
                    v1_0.append(p3);
                    v1_0.append(" -> ");
                    v1_0.append(v0_2);
                    com.oupao.wiz.SLog.v(v1_0.toString());
                    if (v0_2 != null) {
                        com.oupao.wiz.PinIdx.jump(v0_2);
                    }
                } else {
                    com.oupao.wiz.PinIdx.jump(p2);
                    return;
                }
            }
        } catch (Throwable __e_v1) {
            return;
        }
        return;
    }
    public static void goQ(android.view.View p2, String p3)
    {
        try {
            if ((p2 != null) && (p3 != null)) {
                if (!"\u2605".equals(p3)) {
                    var v0_2 = com.oupao.wiz.PinIdx.find(p2, p3);
                    var v1_2 = new StringBuilder();
                    v1_2.append("go ");
                    v1_2.append(p3);
                    v1_2.append(" -> ");
                    v1_2.append(v0_2);
                    com.oupao.wiz.SLog.v(v1_2.toString());
                    if (v0_2 != null) {
                        com.oupao.wiz.PinIdx.jump(v0_2);
                    }
                } else {
                    com.oupao.wiz.PinIdx.jump(p2);
                    return;
                }
            }
        } catch (Throwable __e_v1) {
            return;
        }
        return;
    }
    public static void jump(android.view.View p6)
    {
        int v0 = 0;
        android.view.View v1 = null;
        try {
            v0 = 0;
            v1 = p6;
        } catch (Throwable __e_v4) {
            return;
        }
        while (v1 != null) {
            var v2_2 = v1.getParent();
            if (v2_2 == null) {
                break;
            }
            if (!(v2_2 instanceof android.widget.ScrollView)) {
                v0 += v1.getTop();
                v1 = ((android.view.View) v2_2);
            } else {
                var v2_1 = ((android.widget.ScrollView) v2_2);
                var v5_1 = new StringBuilder();
                v5_1.append("jump y=");
                v5_1.append(v0);
                com.oupao.wiz.SLog.v(v5_1.toString());
                v2_1.smoothScrollTo(0, v0);
                break;
            }
        }
        return;
    }
    public static void vib(android.view.View p7)
    {
        try {
            if (com.oupao.wiz.Cfg.pinVib()) {
                var v0_1 = p7.getContext();
                if (v0_1 != null) {
                    var v0_2 = v0_1.getSystemService("vibrator");
                    if ((v0_2 instanceof android.os.Vibrator)) {
                        var v0_0 = ((android.os.Vibrator) v0_2);
                        if (com.oupao.wiz.Sys.sdk() < 26) {
                            v0_0.vibrate(16);
                        } else {
                            v0_0.vibrate(android.os.VibrationEffect.createOneShot(16, com.oupao.wiz.Cfg.sysHapticAmp()));
                            com.oupao.wiz.SLog.v("ok");
                        }
                    }
                }
            }
        } catch (Throwable __e_v2) {
            return;
        }
        return;
    }
    public void onClick(android.view.View p5)
    {
        java.lang.String v0 = null;
        try {
            v0 = this.letter;
        } catch (Throwable __e_v3) {
            return;
        }
        if (v0 != null) {
            var v2_1 = new StringBuilder();
            v2_1.append("click ");
            v2_1.append(v0);
            com.oupao.wiz.SLog.v(v2_1.toString());
            var v1 = this.card;
            if (v1 != null) {
                com.oupao.wiz.PinIdx.go(v1, v0);
            }
        }
        return;
    }
}