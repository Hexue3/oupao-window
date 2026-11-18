package com.oupao.wiz;
public class Sl$C implements android.view.View.OnClickListener {
    static int fx;
    static int sx;
    static int sy;
    private final android.content.Context ctx;
    private final android.widget.TextView tv;
    public Sl$C(android.content.Context p1, android.widget.TextView p2)
    {
        this.ctx = p1;
        this.tv = p2;
        return;
    }
    public void onClick(android.view.View p13)
    {
        android.content.Context v0 = null;
        try {
            v0 = this.ctx;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 != null) {
            var v1_1 = new android.widget.LinearLayout(v0);
            v1_1.setOrientation(1);
            v1_1.setPadding(com.oupao.wiz.Ui.dp(v0, 6), com.oupao.wiz.Ui.dp(v0, 6), com.oupao.wiz.Ui.dp(v0, 6), com.oupao.wiz.Ui.dp(v0, 6));
            var v3_1 = new android.graphics.drawable.GradientDrawable();
            v3_1.setColor(-1);
            v3_1.setCornerRadius(((float) com.oupao.wiz.Ui.dp(v0, 16)));
            v1_1.setBackground(v3_1);
            String[] v2_1 = new String[3];
            v2_1[0] = "\u5173\u95ed\u6b64\u529f\u80fd";
            v2_1[1] = "\u5355\u51fb\u5173\u95ed";
            v2_1[2] = "\u53cc\u51fb\u5173\u95ed";
            var v3_6 = new android.app.Dialog(v0);
            v3_6.setContentView(v1_1);
            int v4_3 = 0;
            while (v4_3 < v2_1.length) {
                var v5_2 = com.oupao.wiz.Ui.t(v0, v2_1[v4_3], 16.0f, -14935012, false);
                v5_2.setPadding(com.oupao.wiz.Ui.dp(v0, 6), com.oupao.wiz.Ui.dp(v0, 10), com.oupao.wiz.Ui.dp(v0, 6), com.oupao.wiz.Ui.dp(v0, 10));
                v5_2.setOnClickListener(new com.oupao.wiz.Sl$E(v4_3, this.tv, v3_6));
                v1_1.addView(v5_2);
                v4_3++;
            }
            v1_1.measure(0, 0);
            var v8_1 = v1_1.getMeasuredHeight();
            var v9 = v1_1.getMeasuredWidth();
            var v4_10 = v3_6.getWindow();
            if (v4_10 != null) {
                v4_10.setFormat(-3);
                v4_10.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
                var v5_7 = v4_10.getAttributes();
                v5_7.width = com.oupao.wiz.Ui.dp(v0, 133);
                v5_7.height = -2;
                v5_7.gravity = 51;
                v5_7.dimAmount = 0.3f;
                int[] v6_16 = new int[2];
                p13.getLocationOnScreen(v6_16);
                int v7_6 = com.oupao.wiz.Sl$C.fx;
                com.oupao.wiz.Sl$C.fx = 0;
                if (v7_6 <= 0) {
                    v7_6 = v6_16[0];
                }
                var v10_3 = (v7_6 - (v9 / 2));
                if (v10_3 < 0) {
                    v10_3 = 0;
                }
                v5_7.x = v10_3;
                com.oupao.wiz.Sl$C.sx = v10_3;
                var v11_4 = new StringBuilder();
                v11_4.append("SlC fx=");
                v11_4.append(v7_6);
                v11_4.append(" sx=");
                v11_4.append(v10_3);
                com.oupao.wiz.SLog.i(v11_4.toString());
                int v7_1 = ((v6_16[1] + p13.getHeight()) + com.oupao.wiz.Ui.dp(v0, 2));
                var v11_2 = v0.getResources().getDisplayMetrics().heightPixels;
                if ((v7_1 + v8_1) > v11_2) {
                    v7_1 = ((v7_1 - ((v7_1 + v8_1) - v11_2)) - v8_1);
                }
                v5_7.y = v7_1;
                com.oupao.wiz.Sl$C.sy = v7_1;
                v4_10.setAttributes(v5_7);
                v4_10.addFlags(2);
            }
            v3_6.show();
            var v4_0 = v3_6.getWindow();
            if (v4_0 != null) {
                var v5_0 = v4_0.getAttributes();
                v5_0.x = com.oupao.wiz.Sl$C.sx;
                v5_0.y = com.oupao.wiz.Sl$C.sy;
                v5_0.gravity = 51;
                v4_0.setAttributes(v5_0);
            }
        }
        return;
    }
}