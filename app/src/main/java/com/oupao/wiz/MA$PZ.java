package com.oupao.wiz;
public class MA$PZ implements java.lang.Runnable {
    public MA$PZ()
    {
        return;
    }
    private static int rcOf(android.content.res.Resources p4, String p5)
    {
        int v2_0 = p4.getIdentifier(p5, "dimen", "android");
        if (v2_0 > 0) {
            v2_0 = p4.getIdentifier("rounded_corner_radius_top", "dimen", "android");
            if (v2_0 > 0) {
                v2_0 = p4.getIdentifier("rounded_corner_radius_bottom", "dimen", "android");
                if (v2_0 > 0) {
                    return 0;
                }
            }
        }
        return p4.getDimensionPixelSize(v2_0);
    }
    public void run()
    {
        android.widget.FrameLayout v0 = null;
        try {
            com.oupao.wiz.SLog.i("run-start");
            v0 = com.oupao.wiz.MainActivity.pageBox;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 != null) {
            int[] v1_3 = new int[2];
            v0.getLocationOnScreen(v1_3);
            var v2_1 = com.oupao.wiz.Cfg.ctx;
            if (v2_1 != null) {
                var v2_5 = (v2_1.getResources().getDisplayMetrics().heightPixels - v1_3[1]);
                if (v2_5 > 0) {
                    v0.setMinimumHeight(v2_5);
                    var v1_0 = com.oupao.wiz.Cfg.ctx;
                    if (v1_0 != null) {
                        var v2_7 = com.oupao.wiz.MA$PZ.rcOf(v1_0.getResources(), "rounded_corner_radius");
                        if (v2_7 <= 0) {
                            com.oupao.wiz.SLog.i("oupao: screen corner radius=".concat(String.valueOf(v2_7)));
                            if (com.oupao.wiz.Cfg.ctx != null) {
                                com.oupao.wiz.MainActivity.rdDraw = com.oupao.wiz.Ui.bg(com.oupao.wiz.Ui.pageBg(), ((float) v2_7));
                            }
                        }
                    }
                }
            }
        }
        return;
    }
}