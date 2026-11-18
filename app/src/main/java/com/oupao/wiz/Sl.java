package com.oupao.wiz;
public class Sl {
    public Sl()
    {
        return;
    }
    public static android.widget.LinearLayout close(android.content.Context p11)
    {
        try {
            var v0_1 = new android.widget.LinearLayout(p11);
            v0_1.setOrientation(1);
            v0_1.setPadding(com.oupao.wiz.Ui.dp(p11, 20), com.oupao.wiz.Ui.dp(p11, 13), com.oupao.wiz.Ui.dp(p11, 20), com.oupao.wiz.Ui.dp(p11, 13));
            var v1_1 = new android.widget.LinearLayout(p11);
            v1_1.setOrientation(0);
            v1_1.setGravity(16);
            var v2_3 = com.oupao.wiz.Ui.t(p11, "\u7a97\u5916\u70b9\u51fb\u5173\u95ed", 16.0f, -14935012, false);
            v2_3.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
            v1_1.addView(v2_3);
            String[] v9_0 = new String[3];
            v9_0[0] = "\u5173\u95ed\u6b64\u529f\u80fd";
            v9_0[1] = "\u5355\u51fb\u5173\u95ed";
            v9_0[2] = "\u53cc\u51fb\u5173\u95ed";
            var v7 = com.oupao.wiz.Ui.t(p11, com.oupao.wiz.Sl.text(com.oupao.wiz.Cfg.flyClose(), 0, v9_0), 17.0f, -13335809, false);
            v1_1.addView(v7);
            v0_1.addView(v1_1);
            v0_1.addView(com.oupao.wiz.Ui.t(p11, "\u70b9\u5c0f\u7a97\u5916\u90e8\u65f6\u600e\u4e48\u5904\u7406\uff5c\u70b9\u51fb\u6b64\u884c\u9009\u62e9\uff08\u9ed8\u8ba4\u53cc\u51fb\uff09", 14.0f, -7434605, false));
            var v8_1 = new com.oupao.wiz.Sl$C(p11, v7);
            v1_1.setOnClickListener(v8_1);
            v7.setOnClickListener(v8_1);
            v0_1.setOnClickListener(v8_1);
            var v9_2 = new com.oupao.wiz.Sl$G();
            v0_1.setOnTouchListener(v9_2);
            v1_1.setOnTouchListener(v9_2);
            v7.setOnTouchListener(v9_2);
            return v0_1;
        } catch (Throwable __e_v0) {
            return null;
        }
    }
    public static android.widget.LinearLayout range(android.content.Context p11)
    {
        try {
            var v0_1 = new android.widget.LinearLayout(p11);
            v0_1.setOrientation(1);
            v0_1.setPadding(com.oupao.wiz.Ui.dp(p11, 20), com.oupao.wiz.Ui.dp(p11, 13), com.oupao.wiz.Ui.dp(p11, 20), com.oupao.wiz.Ui.dp(p11, 13));
            var v1_1 = new android.widget.LinearLayout(p11);
            v1_1.setOrientation(0);
            v1_1.setGravity(16);
            var v2_11 = com.oupao.wiz.Ui.t(p11, "\u89e6\u53d1\u8303\u56f4", 16.0f, -14935012, false);
            v2_11.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
            v1_1.addView(v2_11);
            var v7_1 = com.oupao.wiz.Ui.t(p11, com.oupao.wiz.Sl.text(com.oupao.wiz.Cfg.flyRange(), 40, "dp"), 17.0f, -13335809, false);
            v1_1.addView(v7_1);
            v0_1.addView(v1_1);
            v0_1.addView(com.oupao.wiz.Ui.t(p11, "\u89d2\u4e0a\u5f00\u59cb\u6ed1\u52a8\u7684\u533a\u57df\u5927\u5c0f\uff0c\u8d8a\u5927\u8d8a\u4e0d\u5bb9\u6613\u8bef\u89e6", 14.0f, -7434605, false));
            var v8_3 = new android.widget.SeekBar(p11);
            v8_3.setMax(40);
            v8_3.setProgress((com.oupao.wiz.Cfg.flyRange() - 40));
            var v2_20 = new com.oupao.wiz.SlCb("oupao_fly_range", 40, "dp");
            v2_20.tv = v7_1;
            v8_3.setOnSeekBarChangeListener(v2_20);
            com.oupao.wiz.Ui.pad(v8_3, 0, 0, 0, 0);
            com.oupao.wiz.Ui.sl(v8_3, p11);
            v0_1.addView(v8_3);
            return v0_1;
        } catch (Throwable __e_v0) {
            return null;
        }
    }
    /**
     * 「图标大小」滑块（原来和「触发范围」挤在同一个容器里，分隔线管不到）。
     * 现在拆成独立方法，由调用方各给一张 Ui.card。
     */
    public static android.widget.LinearLayout icon(android.content.Context p11)
    {
        try {
            var v0_1 = new android.widget.LinearLayout(p11);
            v0_1.setOrientation(1);
            v0_1.setPadding(com.oupao.wiz.Ui.dp(p11, 20), com.oupao.wiz.Ui.dp(p11, 13), com.oupao.wiz.Ui.dp(p11, 20), com.oupao.wiz.Ui.dp(p11, 13));
            var v1_6 = new android.widget.LinearLayout(p11);
            v1_6.setOrientation(0);
            v1_6.setGravity(16);
            var v2_26 = com.oupao.wiz.Ui.t(p11, "\u56fe\u6807\u5927\u5c0f", 16.0f, -14935012, false);
            v2_26.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
            v1_6.addView(v2_26);
            var v7_0 = com.oupao.wiz.Ui.t(p11, com.oupao.wiz.Sl.text(com.oupao.wiz.Cfg.iconSize(), 50, "%"), 17.0f, -13335809, false);
            v1_6.addView(v7_0);
            v0_1.addView(v1_6);
            v0_1.addView(com.oupao.wiz.Ui.t(p11, "\u53ea\u6539\u56fe\u6807\uff0c\u4e0d\u6539\u6247\u533a\u5927\u5c0f", 14.0f, -7434605, false));
            var v8_1 = new android.widget.SeekBar(p11);
            v8_1.setMax(40);
            v8_1.setProgress((com.oupao.wiz.Cfg.iconSize() - 80));
            var v2_6 = new com.oupao.wiz.SlCb("oupao_icon_size", 80, "%");
            v2_6.tv = v7_0;
            v8_1.setOnSeekBarChangeListener(v2_6);
            com.oupao.wiz.Ui.pad(v8_1, 0, 0, 0, 0);
            com.oupao.wiz.Ui.sl(v8_1, p11);
            v0_1.addView(v8_1);
            return v0_1;
        } catch (Throwable __e_v0) {
            return null;
        }
    }
    public static android.widget.LinearLayout size(android.content.Context p11)
    {
        try {
            var v0_1 = new android.widget.LinearLayout(p11);
            v0_1.setOrientation(1);
            v0_1.setPadding(com.oupao.wiz.Ui.dp(p11, 20), com.oupao.wiz.Ui.dp(p11, 13), com.oupao.wiz.Ui.dp(p11, 20), com.oupao.wiz.Ui.dp(p11, 13));
            var v1_1 = new android.widget.LinearLayout(p11);
            v1_1.setOrientation(0);
            v1_1.setGravity(16);
            var v2_3 = com.oupao.wiz.Ui.t(p11, "\u6247\u533a\u5927\u5c0f", 16.0f, -14935012, false);
            v2_3.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
            v1_1.addView(v2_3);
            var v7 = com.oupao.wiz.Ui.t(p11, com.oupao.wiz.Sl.text(com.oupao.wiz.Cfg.flyScale(), 50, "%"), 17.0f, -13335809, false);
            v1_1.addView(v7);
            v0_1.addView(v1_1);
            v0_1.addView(com.oupao.wiz.Ui.t(p11, "\u6247\u5f62\u83dc\u5355\u7684\u6574\u4f53\u5927\u5c0f\uff0c\u56fe\u6807\u8ddf\u7740\u4e00\u8d77\u7f29\u653e", 14.0f, -7434605, false));
            var v8_1 = new android.widget.SeekBar(p11);
            v8_1.setMax(40);
            v8_1.setProgress((com.oupao.wiz.Cfg.flyScale() - 80));
            var v2_12 = new com.oupao.wiz.SlCb("oupao_fly_scale", 80, "%");
            v2_12.tv = v7;
            v8_1.setOnSeekBarChangeListener(v2_12);
            com.oupao.wiz.Ui.pad(v8_1, 0, 0, 0, 0);
            com.oupao.wiz.Ui.sl(v8_1, p11);
            v0_1.addView(v8_1);
            return v0_1;
        } catch (Throwable __e_v0) {
            return null;
        }
    }
    public static String text(int p4, int p5, Object p6)
    {
        if (!(p6 instanceof String)) {
            int v4_1 = (p4 - p5);
            if (v4_1 < 0) {
                v4_1 = 0;
            }
            var v1_5 = (((String[]) p6).length - 1);
            if (v4_1 > v1_5) {
                v4_1 = v1_5;
            }
            return ((String[]) p6)[v4_1];
        } else {
            if (((String) p6).equals("%")) {
                var v0_3 = new StringBuilder();
                v0_3.append(p4);
                return v0_3.append("%").toString();
            } else {
                var v0_7 = new StringBuilder();
                v0_7.append(p4);
                return v0_7.append(" ").append(((String) p6)).toString();
            }
        }
    }
    public static android.widget.LinearLayout vib(android.content.Context p11, String p12, String p13, int p14)
    {
        try {
            var v0_1 = new android.widget.LinearLayout(p11);
            v0_1.setOrientation(1);
            v0_1.setPadding(com.oupao.wiz.Ui.dp(p11, 20), com.oupao.wiz.Ui.dp(p11, 13), com.oupao.wiz.Ui.dp(p11, 20), com.oupao.wiz.Ui.dp(p11, 13));
            var v1_1 = new android.widget.LinearLayout(p11);
            v1_1.setOrientation(0);
            v1_1.setGravity(16);
            var v2_2 = com.oupao.wiz.Ui.t(p11, p13, 16.0f, -14935012, false);
            v2_2.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
            v1_1.addView(v2_2);
            var v7 = com.oupao.wiz.Ui.t(p11, com.oupao.wiz.Sl.text(p14, 0, "%"), 17.0f, -13335809, false);
            v1_1.addView(v7);
            v0_1.addView(v1_1);
            var v8_1 = new android.widget.SeekBar(p11);
            v8_1.setMax(100);
            v8_1.setProgress(p14);
            var v2_5 = new com.oupao.wiz.SlCb(p12, 0, "%");
            v2_5.tv = v7;
            v8_1.setOnSeekBarChangeListener(v2_5);
            com.oupao.wiz.Ui.pad(v8_1, 0, 0, 0, 0);
            com.oupao.wiz.Ui.sl(v8_1, p11);
            v0_1.addView(v8_1);
            return v0_1;
        } catch (Throwable __e_v0) {
            return null;
        }
    }
}