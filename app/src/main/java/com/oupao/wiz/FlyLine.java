package com.oupao.wiz;
public class FlyLine extends android.view.View {
    public FlyLine(android.content.Context p1)
    {
        super(p1);
        return;
    }
    public static void add(android.view.ViewGroup p9)
    {
        try {
            if (p9 != null) {
                com.oupao.wiz.FlyLine.remove(p9);
                var v0 = p9.getContext();
                if (v0 != null) {
                    int v7 = 0;
                    var v1 = com.oupao.wiz.FlyLine.sideOf(p9);
                    var v2_1 = new android.graphics.drawable.GradientDrawable();
                    v2_1.setColor(-1);
                    v2_1.setCornerRadius(2.0f);
                    var v3_2 = new com.oupao.wiz.FlyLine(v0);
                    v3_2.setBackground(v2_1);
                    var v6_1 = new android.widget.FrameLayout.LayoutParams(com.oupao.wiz.Ui.dp(v0, 4), com.oupao.wiz.Ui.dp(v0, 40));
                    if (v1 != 0) {
                        v7 = 19;
                    } else {
                        v7 = 21;
                    }
                    v6_1.gravity = v7;
                    p9.addView(v3_2, v6_1);
                    com.oupao.wiz.FlyLine.probe(p9, v1);
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    static void probe(android.view.ViewGroup p7, int p8)
    {
        android.view.View v5_4 = null;
        try {
            var v0_1 = new StringBuilder();
            v0_1.append("FlyLine side=");
            v0_1.append(p8);
            v0_1.append(" w=");
            v0_1.append(p7.getWidth());
            v0_1.append(" h=");
            v0_1.append(p7.getHeight());
            int[] v3 = new int[2];
            p7.getLocationOnScreen(v3);
            v0_1.append(" loc=");
            v0_1.append(v3[0]);
            v0_1.append(",");
            v0_1.append(v3[1]);
            v0_1.append(" dm=");
            v0_1.append(p7.getContext().getResources().getDisplayMetrics().widthPixels);
            com.oupao.wiz.SLog.i(v0_1.toString());
            v5_4 = p7.getRootView();
        } catch (Throwable __e_v0) {
            return;
        }
        if (v5_4 != null) {
            var v0_3 = new StringBuilder();
            v0_3.append("FlyLine root=");
            v0_3.append(v5_4.getClass().getName());
            v0_3.append(" mode=");
            v0_3.append(com.oupao.wiz.Rt.get(v5_4, "mCurrentMode"));
            v0_3.append(" side=");
            v0_3.append(com.oupao.wiz.Rt.get(v5_4, "mCurrentSide"));
            com.oupao.wiz.SLog.i(v0_3.toString());
        }
        return;
    }
    public static void remove(android.view.ViewGroup p5)
    {
        try {
            if (p5 != null) {
                int v0 = 0;
                while (v0 < p5.getChildCount()) {
                    var v2 = p5.getChildAt(v0);
                    if (!(v2 instanceof com.oupao.wiz.FlyLine)) {
                        v0++;
                    } else {
                        p5.removeView(v2);
                    }
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static int sideOf(android.view.ViewGroup p7)
    {
        android.view.View v0_0 = null;
        try {
            v0_0 = p7.getRootView();
        } catch (Throwable __e_v0) {
            return 1;
        }
        if (v0_0 != null) {
            var v2_0 = com.oupao.wiz.Rt.get(v0_0, "mCurrentSide");
            if ((v2_0 instanceof Integer)) {
                var v3_0 = ((Integer) v2_0).intValue();
                if ((v3_0 >= 0) && (v3_0 <= 1)) {
                    return v3_0;
                }
            }
        }
        int[] v4_2 = new int[2];
        p7.getLocationOnScreen(v4_2);
        if ((v4_2[0] + (p7.getWidth() / 2)) >= (p7.getContext().getResources().getDisplayMetrics().widthPixels / 2)) {
            return 1;
        } else {
            return 0;
        }
    }
}