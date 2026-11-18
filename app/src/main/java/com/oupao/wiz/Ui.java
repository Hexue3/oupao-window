package com.oupao.wiz;
public class Ui {
    private Ui()
    {
        return;
    }
    public static android.graphics.drawable.GradientDrawable bg(int p2, float p3)
    {
        var v0_1 = new android.graphics.drawable.GradientDrawable();
        v0_1.setShape(0);
        v0_1.setColor(p2);
        v0_1.setCornerRadius(p3);
        return v0_1;
    }
    public static android.widget.LinearLayout box(android.content.Context p3)
    {
        var v0_1 = new android.widget.LinearLayout(p3);
        v0_1.setOrientation(1);
        v0_1.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, -2));
        return v0_1;
    }
    public static android.widget.Button btn(android.content.Context p4, String p5, android.view.View.OnClickListener p6)
    {
        var v0_1 = new android.widget.Button(p4);
        v0_1.setText(p5);
        v0_1.setTypeface(android.graphics.Typeface.create("sans-serif-medium", 0));
        v0_1.setAllCaps(false);
        v0_1.setTextSize(16.0f);
        v0_1.setBackground(com.oupao.wiz.Ui.bg(-13335809, ((float) com.oupao.wiz.Ui.dp(p4, 28))));
        v0_1.setTextColor(com.oupao.wiz.Ui.adaptText(-1));
        v0_1.setPadding(0, com.oupao.wiz.Ui.dp(p4, 18), 0, com.oupao.wiz.Ui.dp(p4, 18));
        v0_1.setOnClickListener(p6);
        return v0_1;
    }
    public static android.widget.LinearLayout card(android.content.Context p6)
    {
        var v0_1 = new android.widget.LinearLayout(p6);
        v0_1.setOrientation(1);
        v0_1.setBackground(com.oupao.wiz.Ui.bg(cardBg(), ((float) com.oupao.wiz.Ui.dp(p6, 24))));
        v0_1.setPadding(0, 0, 0, 0);
        v0_1.setShowDividers(2);
        var v3_2 = new android.graphics.drawable.GradientDrawable();
        // 1820fix: 暗色下分隔线用浅色（原来的淡黑在黑底上看不见）
        v3_2.setColor(night() ? 0x33FFFFFF : 520093696);
        v3_2.setSize(1, 1);
        v0_1.setDividerDrawable(v3_2);
        v0_1.setDividerPadding(com.oupao.wiz.Ui.dp(p6, 20));
        var v2_2 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        v2_2.setMargins(com.oupao.wiz.Ui.dp(p6, 18), com.oupao.wiz.Ui.dp(p6, 8), com.oupao.wiz.Ui.dp(p6, 18), 0);
        v0_1.setLayoutParams(v2_2);
        return v0_1;
    }
    public static android.content.res.ColorStateList csl(int p6, int p7)
    {
        int v0 = 0;
        try {
            v0 = Class.forName("android.R$attr").getField("state_checked").getInt(0);
        } catch (Throwable __e_v1) {
        }
        int[][] v1_2 = new int[2][];
        int[] v2_1 = new int[1];
        v2_1[0] = v0;
        v1_2[0] = v2_1;
        int[] v2_3 = new int[0];
        v1_2[1] = v2_3;
        int[] v2_5 = new int[2];
        v2_5[0] = p6;
        v2_5[1] = p7;
        return new android.content.res.ColorStateList(v1_2, v2_5);
    }
    public static int dp(android.content.Context p4, int p5)
    {
        return ((int) ((((float) p5) * p4.getResources().getDisplayMetrics().density) + 0.5f));
    }
    public static void navRow(android.widget.LinearLayout p9, String p10, int p11)
    {
        var v0 = p9.getContext();
        var v1_1 = new android.widget.LinearLayout(v0);
        v1_1.setOrientation(0);
        v1_1.setGravity(16);
        v1_1.setPadding(com.oupao.wiz.Ui.dp(v0, 20), com.oupao.wiz.Ui.dp(v0, 14), com.oupao.wiz.Ui.dp(v0, 20), com.oupao.wiz.Ui.dp(v0, 14));
        com.oupao.wiz.Ui.ripple(v1_1);
        var v3_3 = com.oupao.wiz.Ui.t(v0, p10, 16.0f, -14935012, true);
        v3_3.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        v1_1.addView(v3_3);
        v1_1.addView(com.oupao.wiz.Ui.t(v0, "\u203a", 26.0f, -7434605, true));
        v1_1.setOnClickListener(new com.oupao.wiz.MA$PG(p11));
        p9.addView(v1_1);
        return;
    }
    public static void navRow2(android.widget.LinearLayout p2, int p3)
    {
        p2.setOnClickListener(new com.oupao.wiz.MA$PG(p3));
        return;
    }
    public static android.graphics.drawable.GradientDrawable oval(int p2)
    {
        var v0_1 = new android.graphics.drawable.GradientDrawable();
        v0_1.setShape(1);
        v0_1.setColor(p2);
        return v0_1;
    }
    public static void pad(android.view.View p5, int p6, int p7, int p8, int p9)
    {
        var v0 = p5.getContext();
        p5.setPadding(com.oupao.wiz.Ui.dp(v0, p6), com.oupao.wiz.Ui.dp(v0, p7), com.oupao.wiz.Ui.dp(v0, p8), com.oupao.wiz.Ui.dp(v0, p9));
        return;
    }
    public static void ripple(android.view.View p4)
    {
        try {
            if (p4 != null) {
                p4.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(436207616), null, new android.graphics.drawable.ColorDrawable(-1)));
            }
        } catch (Throwable __e___ex) {
            return;
        }
        return;
    }
    public static void row(android.widget.LinearLayout p9, String p10, String p11)
    {
        var v9_1 = p9.getContext();
        var v0_1 = new android.widget.LinearLayout(v9_1);
        v0_1.setOrientation(0);
        v0_1.setGravity(16);
        v0_1.setPadding(com.oupao.wiz.Ui.dp(v9_1, 20), com.oupao.wiz.Ui.dp(v9_1, 14), com.oupao.wiz.Ui.dp(v9_1, 20), com.oupao.wiz.Ui.dp(v9_1, 14));
        var v2_7 = new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(436207616), null, new android.graphics.drawable.ColorDrawable(-1));
        v0_1.setBackground(v2_7);
        try {
            Object[] v3_5 = new Object[1];
            v3_5[0] = Integer.valueOf(com.oupao.wiz.Ui.dp(v9_1, 30));
            com.oupao.wiz.Rt.callLoose(v2_7, "setMaxRadius", v3_5);
        } catch (Throwable __e_v1) {
        }
        var v2_8 = p11.indexOf(124);
        java.lang.String v7_0 = p11;
        java.lang.String v8 = null;
        if (v2_8 >= 0) {
            v7_0 = p11.substring(0, v2_8);
            v8 = p11.substring((v2_8 + 1));
        }
        android.view.View v1_6 = null;
        if (v8 != null) {
            v1_6 = com.oupao.wiz.Ui.two(v9_1, v7_0, v8);
        } else {
            v1_6 = com.oupao.wiz.Ui.t(v9_1, v7_0, 16.0f, -14935012, false);
        }
        v1_6.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        v0_1.addView(v1_6);
        var v1_9 = new android.widget.Switch(v9_1);
        int v3_9 = 0;
        if ("oupao_app_keep".equals(p10)) {
            v3_9 = 0;
            if ("oupao_fly_stay_off".equals(p10)) {
                v3_9 = 0;
            }
        }
        // 「屏蔽所有应用悬浮通知」默认关：全局拦所有横幅很激进，
        // 必须与 Cfg.nohuAll() 的默认值(getInt(...,0))保持一致，否则界面显示开、实际没开。
        if ("oupao_nohu_all".equals(p10)) {
            v3_9 = 0;
        }
        // 消息头像气泡：默认关
        if ("oupao_sug_msg".equals(p10)) {
            v3_9 = 0;
        }
        // 移除应用启动遮罩：默认关。必须与 Cfg.noSplash() 的 plainGate(...,0)
        // 以及 BOOL_DEFAULTS 一致，否则界面显示开、实际没开。
        if ("oupao_no_splash".equals(p10)) {
            v3_9 = 0;
        }
        // 隐藏桌面图标：默认关
        if ("oupao_hide_icon".equals(p10)) {
            v3_9 = 0;
        }
        // 气泡常驻：默认关
        if ("oupao_bub_persist".equals(p10)) {
            v3_9 = 0;
        }

        int v2_14 = 0;
        if (com.oupao.wiz.Cfg.getInt(p10, v3_9) != 1) {
            v2_14 = 0;
        } else {
            v2_14 = 1;
        }
        // 2829：能力位说「这套系统不支持」→ 开关置灰、摘要换成「当前系统不支持此功能」、点了只弹提示。
        // 用户要求：开关开启前就要判系统支不支持，不能不管支不支持都能点亮。
        java.lang.String capName = com.oupao.wiz.Caps.capOf(p10);
        boolean capped = (capName != null)
                && !Boolean.TRUE.equals(com.oupao.wiz.Caps.supported(capName));
        if (capped) {
            v2_14 = 0;
            try {
                v0_1.removeViewAt(0);
                android.view.View nv = com.oupao.wiz.Ui.two(v9_1, v7_0,
                        com.oupao.wiz.Caps.NO_SUPPORT);
                nv.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
                v0_1.addView(nv, 0);
            } catch (Throwable ignore) {
            }
            v1_9.setEnabled(false);
            v1_9.setAlpha(0.4f);
            v0_1.setOnClickListener(new android.view.View.OnClickListener() {
                public void onClick(android.view.View x) {
                    try {
                        android.widget.Toast.makeText(x.getContext(),
                                com.oupao.wiz.Caps.NO_SUPPORT,
                                android.widget.Toast.LENGTH_SHORT).show();
                    } catch (Throwable ignore) {
                    }
                }
            });
        } else {
            v0_1.setOnClickListener(new com.oupao.wiz.Ui$C1(v1_9));
        }
        v1_9.setChecked(v2_14 != 0);
        v1_9.setOnCheckedChangeListener(new com.oupao.wiz.MainActivity$C1(((com.oupao.wiz.MainActivity) v9_1), p10));
        v1_9.setThumbTintList(com.oupao.wiz.Ui.csl(-13335809, -1));
        v1_9.setTrackTintList(com.oupao.wiz.Ui.csl(1026851583, -1447445));
        var v2_1 = new android.widget.LinearLayout.LayoutParams(-2, -2);
        v2_1.setMargins(com.oupao.wiz.Ui.dp(v9_1, 12), 0, 0, 0);
        v1_9.setLayoutParams(v2_1);
        v0_1.addView(v1_9);
        v0_1.setOnClickListener(new com.oupao.wiz.Ui$C1(v1_9));
        p9.addView(v0_1);
        return;
    }
    public static void rowNav(android.widget.LinearLayout p9, String p10, String p11)
    {
        var v9_1 = p9.getContext();
        var v0_1 = new android.widget.LinearLayout(v9_1);
        v0_1.setOrientation(0);
        v0_1.setGravity(16);
        v0_1.setPadding(com.oupao.wiz.Ui.dp(v9_1, 20), com.oupao.wiz.Ui.dp(v9_1, 14), com.oupao.wiz.Ui.dp(v9_1, 20), com.oupao.wiz.Ui.dp(v9_1, 14));
        var v2_8 = new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(436207616), null, new android.graphics.drawable.ColorDrawable(-1));
        v0_1.setBackground(v2_8);
        try {
            Object[] v3_5 = new Object[1];
            v3_5[0] = Integer.valueOf(com.oupao.wiz.Ui.dp(v9_1, 30));
            com.oupao.wiz.Rt.callLoose(v2_8, "setMaxRadius", v3_5);
        } catch (Throwable __e_v1) {
        }
        var v2_9 = p11.indexOf(124);
        java.lang.String v7 = p11;
        java.lang.String v8 = null;
        if (v2_9 >= 0) {
            v7 = p11.substring(0, v2_9);
            v8 = p11.substring((v2_9 + 1));
        }
        android.view.View v1_6 = null;
        if (v8 != null) {
            v1_6 = com.oupao.wiz.Ui.two(v9_1, v7, v8);
        } else {
            v1_6 = com.oupao.wiz.Ui.t(v9_1, v7, 17.0f, -14935012, true);
        }
        int v2_18 = 0;
        v1_6.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        v0_1.addView(v1_6);
        var v1_10 = com.oupao.wiz.Ui.t(v9_1, "\u203a", 26.0f, -7434605, true);
        v1_10.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-2, -2));
        v0_1.addView(v1_10);
        var v1_12 = new android.widget.Switch(v9_1);
        if (com.oupao.wiz.Cfg.getInt(p10, 0) != 1) {
            v2_18 = 0;
        } else {
            v2_18 = 1;
        }
        v1_12.setChecked(v2_18 != 0);
        v1_12.setOnCheckedChangeListener(new com.oupao.wiz.MainActivity$C1(((com.oupao.wiz.MainActivity) v9_1), p10));
        v1_12.setThumbTintList(com.oupao.wiz.Ui.csl(-13335809, -1));
        v1_12.setTrackTintList(com.oupao.wiz.Ui.csl(1026851583, -1447445));
        var v2_2 = new android.widget.LinearLayout.LayoutParams(-2, -2);
        v2_2.setMargins(com.oupao.wiz.Ui.dp(v9_1, 12), 0, 0, 0);
        v1_12.setLayoutParams(v2_2);
        v0_1.addView(v1_12);
        v0_1.setOnClickListener(new com.oupao.wiz.Ui$C2());
        p9.addView(v0_1);
        return;
    }
    public static void sl(android.widget.SeekBar p5, android.content.Context p6)
    {
        try {
            if (p5 != null) {
                var v0_9 = com.oupao.wiz.Ui.oval(-13335809);
                var v2_1 = com.oupao.wiz.Ui.dp(p6, 22);
                v0_9.setSize(v2_1, v2_1);
                v0_9.setBounds(0, 0, v2_1, v2_1);
                p5.setThumb(v0_9);
                p5.setProgressTintList(android.content.res.ColorStateList.valueOf(1026851583));
                p5.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(-1447445));
                p5.setMinimumHeight(com.oupao.wiz.Ui.dp(p6, 30));
                var v0_8 = com.oupao.wiz.Ui.dp(p6, 11);
                p5.setThumbOffset(v0_8);
                p5.setPadding(v0_8, p5.getPaddingTop(), v0_8, p5.getPaddingBottom());
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static android.widget.TextView t(android.content.Context p3, String p4, float p5, int p6, boolean p7)
    {
        var v0_1 = new android.widget.TextView(p3);
        v0_1.setText(p4);
        v0_1.setTextSize(p5);
        v0_1.setTextColor(adaptText(p6));
        if (p7) {
            v0_1.setTypeface(android.graphics.Typeface.create("sans-serif-medium", 0));
        }
        if ((p4 != null) && ((p4.length() == 1) && (p4.charAt(0) == 8250))) {
            v0_1.setTranslationY(((float) (- com.oupao.wiz.Ui.dp(p3, 3))));
        }
        return v0_1;
    }
    public static android.widget.LinearLayout two(android.content.Context p5, String p6, String p7)
    {
        var v0_1 = new android.widget.LinearLayout(p5);
        v0_1.setOrientation(1);
        v0_1.addView(com.oupao.wiz.Ui.t(p5, p6, 16.0f, -14935012, false));
        var v1_1 = com.oupao.wiz.Ui.t(p5, p7, 13.0f, -7434605, false);
        var v2_2 = new android.widget.LinearLayout.LayoutParams(-2, -2);
        v2_2.setMargins(0, com.oupao.wiz.Ui.dp(p5, 3), 0, 0);
        v1_1.setLayoutParams(v2_2);
        v0_1.addView(v1_1);
        return v0_1;
    }

    /** 系统是否处于暗色模式（无需 Context） */
    public static int nightFlag = -1;
    public static android.content.Context appCtx;

    public static boolean night()
    {
        try {
            if (nightFlag >= 0) {
                return (nightFlag == 1);
            }
            int mode = android.content.res.Resources.getSystem().getConfiguration().uiMode & 48;
            return (mode == 32);
        } catch (Throwable t) {
            return false;
        }
    }

    /** 页面底色 */
    public static int pageBg()
    {
        return night() ? lGray(sysL("DarkMode_BackgroundMaxL", 0)) : -657929;
    }

    /** 卡片/分组底色 */
    public static int cardBg()
    {
        int bgL = sysL("DarkMode_BackgroundMaxL", 0);
        int cardL = sysL("DarkMode_DialogBgMaxL", 14);
        int rel = (bgL + 14);                       // 相对页面底 +14 亮度
        if (cardL > rel) {
            cardL = rel;                            // 跟随系统但不超过相对值，避免过亮
        }
        if (cardL < 8) {
            cardL = 8;
        }
        return night() ? lGray(cardL) : -1;
    }

    /** 主文字色 */
    public static int textMain()
    {
        return night() ? -1 : -14935012;           // #FFFFFF : #1C1C1C
    }

    /** 次文字色 */
    public static int textSub()
    {
        return night() ? -8355712 : -7434605;      // #808080 : #8E8E93
    }

    /** 暗色下把"浅色底"换成深灰卡片色（保留原 alpha），白色 → #232323 */
        /** 暗色下深色文字 → 浅色；白色/浅色文字保持不变 */
    public static int adaptText(int c)
    {
        try {
            if (!night()) {
                return c;
            }
            int a = ((c >>> 24) & 0xFF);
            if (a < 40) {
                return c;
            }
            int r = ((c >>> 16) & 0xFF);
            int g = ((c >>> 8) & 0xFF);
            int b = (c & 0xFF);
            int lum = (((r * 299) + (g * 587) + (b * 114)) / 1000);
            if (lum < 90) {
                return ((a << 24) | 0xEDEDED);        // 深色字 → 浅灰白
            }
            if (lum < 150) {
                return ((a << 24) | 0xB0B0B0);        // 中灰字 → 提亮
            }
            return c;
        } catch (Throwable t) {
            return c;
        }
    }

    /** 读系统暗色亮度阈值（ColorOS: DarkMode_BackgroundMaxL / DialogBgMaxL / ForegroundMinL） */
    static int sysL(String key, int def)
    {
        try {
            var ctx = (appCtx != null) ? appCtx : com.oupao.wiz.Cfg.sysContext();
            if (ctx == null) {
                return def;
            }
            float v = android.provider.Settings.Global.getFloat(ctx.getContentResolver(), key, (float) def);
            return ((int) v);
        } catch (Throwable t) {
            return def;
        }
    }

    /** 亮度 L(0~100) → 灰色 ARGB */
    static int lGray(int l)
    {
        if (l < 0) {
            l = 0;
        }
        if (l > 100) {
            l = 100;
        }
        int g = ((l * 255) / 100);
        return (((0xFF << 24) | (g << 16)) | (g << 8)) | g;
    }
}