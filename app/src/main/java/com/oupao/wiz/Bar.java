package com.oupao.wiz;
public class Bar {
    private static android.view.animation.AccelerateInterpolator ai;
    private static android.view.animation.DecelerateInterpolator di;
    public Bar()
    {
        return;
    }
    /**
     * 插值器单例工厂（原来 acc/dec 各一份，仅类型与缓存字段不同）。
     * @param which 1=Accelerate 2=Decelerate
     */
    private static android.view.animation.Interpolator itp(int which)
    {
        try {
            if (which == 1) {
                if (com.oupao.wiz.Bar.ai == null) {
                    com.oupao.wiz.Bar.ai = new android.view.animation.AccelerateInterpolator();
                }
                return com.oupao.wiz.Bar.ai;
            } else {
                if (com.oupao.wiz.Bar.di == null) {
                    com.oupao.wiz.Bar.di = new android.view.animation.DecelerateInterpolator();
                }
                return com.oupao.wiz.Bar.di;
            }
        } catch (Throwable __e_v0) {
            return null;
        }
    }
    private static android.view.animation.AccelerateInterpolator acc()
    {
        return (android.view.animation.AccelerateInterpolator) com.oupao.wiz.Bar.itp(1);
    }
    private static android.view.animation.DecelerateInterpolator dec()
    {
        return (android.view.animation.DecelerateInterpolator) com.oupao.wiz.Bar.itp(2);
    }
    public static void fadeOut(android.view.View p5)
    {
        try {
            if (p5 != null) {
                var v0 = p5.animate();
                v0.alpha(0);
                v0.scaleX(0.85f);
                v0.scaleY(0.85f);
                v0.setDuration(150);
                v0.setInterpolator(com.oupao.wiz.Bar.acc());
                v0.start();
            }
        } catch (Throwable __e_v4) {
            return;
        }
        return;
    }
    public static void fan()
    {
        float[] v0_1 = new float[2];
        com.oupao.wiz.FlyH.FAN = v0_1;
        float[] v0_3 = new float[2];
        com.oupao.wiz.FlyH.POS = v0_3;
        return;
    }
    public static void grow(android.view.View p1, int p2)
    {
        p1.setScaleX(1.0f);
        p1.setScaleY(1.0f);
        return;
    }
    public static int hit(float p10, float p11)
    {
        int v0 = 0;
        float v1 = Float.MAX_VALUE;
        int v3_0 = 0;
        while (v3_0 < com.oupao.wiz.FlyH.cnt) {
            com.oupao.wiz.Bar.pos(v3_0);
            var v4 = com.oupao.wiz.FlyH.POS;
            var v5_3 = ((float) com.oupao.wiz.FlyH.arcH);
            var hcx = (com.oupao.wiz.FlyH.side == 1) ? 0.0f : ((float) com.oupao.wiz.FlyH.sw);
            var hcy = (float) com.oupao.wiz.FlyH.sh;
            var hrv = com.oupao.wiz.FlyH.rv;
            var vcx = hcx + (((v4[0] + v5_3) - hcx) * hrv) + com.oupao.wiz.FlyH.dv(v3_0, true);
            var vcy = hcy + (((v4[1] + v5_3) - hcy) * hrv) + com.oupao.wiz.FlyH.dv(v3_0, false);
            var v6_4 = (((vcx - p10) * (vcx - p10)) + ((vcy - p11) * (vcy - p11)));
            if (v6_4 < v1) {
                v1 = v6_4;
                v0 = v3_0;
            }
            v3_0++;
        }
        var v8_2 = new StringBuilder();
        v8_2.append("Fly: hit p=");
        v8_2.append(((int) p10));
        v8_2.append(",");
        v8_2.append(((int) p11));
        v8_2.append(" minD2=");
        v8_2.append(((int) v1));
        v8_2.append(" cnt=");
        v8_2.append(com.oupao.wiz.FlyH.cnt);
        v8_2.append(" arcH=");
        v8_2.append(com.oupao.wiz.FlyH.arcH);
        v8_2.append(" P0=");
        v8_2.append(((int) com.oupao.wiz.FlyH.POS[0]));
        v8_2.append(",");
        v8_2.append(((int) com.oupao.wiz.FlyH.POS[1]));
        com.oupao.wiz.SLog.r("r-fly-hit", v8_2.toString(), 2000L);
        if (v1 > ((((float) com.oupao.wiz.FlyH.arcH) * 1.2f) * (((float) com.oupao.wiz.FlyH.arcH) * 1.2f))) {
            v0 = -1;
        }
        return v0;
    }
    public static void jelly(android.view.View p8)
    {

        try {
            float[] v0_1 = new float[5];
            v0_1 = new float[]{0.20000000298023224f, 1.149999976158142f, 0.9200000166893005f, 1.0399999618530273f, 1.0f};
            var v1_8 = android.animation.PropertyValuesHolder.ofFloat("scaleX", v0_1);
            var v2_6 = android.animation.PropertyValuesHolder.ofFloat("scaleY", v0_1);
            android.animation.PropertyValuesHolder[] v3_0 = new android.animation.PropertyValuesHolder[2];
            v3_0[0] = v1_8;
            v3_0[1] = v2_6;
            var v1_0 = android.animation.ObjectAnimator.ofPropertyValuesHolder(p8, v3_0);
            v1_0.setDuration(480);
            v1_0.start();
            p8.setAlpha(((float) 0));
            float[] v1_4 = new float[2];
            v1_4 = new float[]{0.0f, 1.0f};
            android.animation.PropertyValuesHolder[] v2_3 = new android.animation.PropertyValuesHolder[1];
            v2_3[0] = android.animation.PropertyValuesHolder.ofFloat("alpha", v1_4);
            var v1_7 = android.animation.ObjectAnimator.ofPropertyValuesHolder(p8, v2_3);
            v1_7.setDuration(140);
            v1_7.start();
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static android.view.View more(android.content.Context p4)
    {
        // 1820fix: 3 个等距圆点。gap 用公式 (iconPx-3*dot)/4 计算，
        // 使"点间距"与"两侧留白"完全相等（容器居中后自动成立）。
        int iconPx = com.oupao.wiz.FlyH.spxIcon(p4, 36);
        int dot = ((int) (((float) iconPx) * 0.125f));
        if (dot < 6) {
            dot = 6;
        }
        int gap = ((iconPx - (3 * dot)) / 4);
        if (gap < 4) {
            gap = 4;
        }
        var row = new android.widget.LinearLayout(p4);
        row.setOrientation(0);
        row.setGravity(17);
        var bg = new android.graphics.drawable.GradientDrawable();
        bg.setShape(1);
        bg.setColor(-13335809);          // 恢复原底色
        row.setBackground(bg);
        for (int i = 0; i < 3; i++) {
            var v = new android.view.View(p4);
            var gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(1);
            gd.setColor(-1);
            v.setBackground(gd);
            var lp = new android.widget.LinearLayout.LayoutParams(dot, dot);
            lp.setMargins((i == 0) ? 0 : gap, 0, 0, 0);
            row.addView(v, lp);
        }
        com.oupao.wiz.SLog.i("Fly: more 三点 dot=" + dot + " gap=" + gap + " iconPx=" + iconPx
                + " total=" + ((3 * dot) + (2 * gap)) + " bg=#" + Integer.toHexString(-13335809));
        return row;
    }
    public static void place()
    {
        var v0 = com.oupao.wiz.FlyH.items;
        if (v0 != null) {
            var v1 = v0.length;
            int v2 = 0;
            while (v2 < v1) {
                var v3 = v0[v2];
                if (v3 != null) {
                    com.oupao.wiz.Bar.put(v3, v2);
                }
                v2++;
            }
        }
        return;
    }
    public static void pop(android.view.View p8, int p9)
    {
        android.view.ViewPropertyAnimator v0_2 = null;
        int v4_1 = 0;
        try {
            p8.setScaleX(0.6f);
            p8.setScaleY(0.6f);
            p8.setAlpha(0);
            v0_2 = p8.animate();
            v0_2.alpha(1.0f);   // smali 只动 alpha，scale 交给 wobble()
            v0_2.setDuration(200);
            v4_1 = ((int) ((((long) p9) * 25)));
        } catch (Throwable __e_v7) {
            p8.setAlpha(1.0f);      // 动画没建起来也必须恢复不透明，否则永远透明
            p8.setScaleX(1.0f);
            p8.setScaleY(1.0f);
            return;
        }
        if (v4_1 > 75) {
            v4_1 = 75;
        }
        v0_2.setStartDelay(v4_1);
        v0_2.setInterpolator(com.oupao.wiz.Bar.dec());
        v0_2.start();               // start() 必须无条件执行

        return;
    }
    public static void pos(int p8)
    {
        float v7 = 0;
        var v1 = com.oupao.wiz.FlyH.cnt;
        var v5_0 = com.oupao.wiz.FlyH.rad;
        if (v1 > 1) {
            v7 = (((((float) p8) * 72.0f) / ((float) (v1 - 1))) + 9.0f);
        } else {
            v7 = 45.0f;
        }
        float v2_3 = 0f;
        var v2_2 = (com.oupao.wiz.Arc.cosDeg(v7) * v5_0);
        var v3_2 = (com.oupao.wiz.Arc.sinDeg(v7) * v5_0);
        var v6 = ((float) com.oupao.wiz.FlyH.arcH);
        if (1 != com.oupao.wiz.FlyH.side) {
            v2_3 = ((((float) com.oupao.wiz.FlyH.sw) - v2_2) - v6);
        } else {
            v2_3 = (v2_2 - v6);
        }
        var v3_3 = ((((float) com.oupao.wiz.FlyH.sh) - v3_2) - v6);
        var v4_7 = com.oupao.wiz.FlyH.POS;
        v4_7[0] = v2_3;
        v4_7[1] = v3_3;
        return;
    }
    public static void press(android.view.View p4, boolean p5)
    {
        try {
            if (p4 != null) {
                if (p5 == false) {
                    var v1 = p4.animate();
                    v1.scaleX(1.0f);
                    v1.scaleY(1.0f);
                    v1.alpha(1.0f);
                    v1.setDuration(180);
                    v1.setInterpolator(new android.view.animation.OvershootInterpolator(2));
                    v1.start();
                } else {
                    p4.setScaleX(0.85999995f);
                    p4.setScaleY(0.85999995f);
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static void put(android.view.View p7, int p8)
    {
        com.oupao.wiz.Bar.pos(p8);
        var v0 = com.oupao.wiz.FlyH.POS;
        var v3_0 = (v0[1] - ((float) com.oupao.wiz.FlyH.roy));
        p7.setTranslationX((v0[0] - ((float) com.oupao.wiz.FlyH.rox)));
        p7.setTranslationY(v3_0);
        p7.setVisibility(0);
        com.oupao.wiz.Bar.pop(p7, p8);
        try {
            int[] v1_2 = new int[2];
            p7.getLocationOnScreen(v1_2);
            var v2_1 = new StringBuilder();
            v2_1.append("Fly: item ");
            v2_1.append(p8);
            v2_1.append(" on=");
            v2_1.append(v1_2[0]);
            v2_1.append(",");
            v2_1.append(v1_2[1]);
            v2_1.append(" pos=");
            v2_1.append(((int) com.oupao.wiz.FlyH.POS[0]));
            v2_1.append(",");
            v2_1.append(((int) com.oupao.wiz.FlyH.POS[1]));
            com.oupao.wiz.SLog.i(v2_1.toString());
            return;
        } catch (Throwable __e_v1) {
            return;
        }
    }
    public static void wobble()
    {
        android.view.View[] v0 = null;
        try {
            v0 = com.oupao.wiz.FlyH.items;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 != null) {
            var v1 = v0.length;
            int v2 = 0;
            while (v2 < v1) {
                var v3 = v0[v2];
                if (v3 != null) {
                    if (com.oupao.wiz.FlyH.sel != v2) {
                        float[] v4_1 = new float[5];
                        v4_1 = new float[]{0.0f, -13.0f, 9.0f, -4.0f, 0.0f};
                        var v5_1 = android.animation.PropertyValuesHolder.ofFloat("rotation", v4_1);
                        float[] v6_1 = new float[5];
                        v6_1 = new float[]{0.6000000238418579f, 1.100000023841858f, 0.9599999785423279f, 1.0299999713897705f, 1.0f};
                        var v7_1 = android.animation.PropertyValuesHolder.ofFloat("scaleX", v6_1);
                        var v8_1 = android.animation.PropertyValuesHolder.ofFloat("scaleY", v6_1);
                        android.animation.PropertyValuesHolder[] v9_1 = new android.animation.PropertyValuesHolder[3];
                        v9_1[0] = v5_1;
                        v9_1[1] = v7_1;
                        v9_1[2] = v8_1;
                        var v5_2 = android.animation.ObjectAnimator.ofPropertyValuesHolder(v3, v9_1);
                        v5_2.setDuration(520);
                        v5_2.setStartDelay((((long) v2) * 22));
                        v5_2.start();
                    } else {
                    }
                }
                v2++;
            }
        }
        return;
    }
    public static android.view.View[] arc(android.content.Context p8, android.view.ViewGroup p9, java.util.ArrayList p10, int p11)
    {
        var v0_0 = p10.size();
        var v1_1 = com.oupao.wiz.FlyH.spxIcon(p8, 36);
        var v2_1 = new android.view.View[(v0_0 + 1)];
        com.oupao.wiz.FlyH.items = v2_1;
        var v3_1 = com.oupao.wiz.Bar.more(p8);
        p9.addView(v3_1, new android.widget.FrameLayout.LayoutParams(v1_1, v1_1));
        com.oupao.wiz.FlyH.items[0] = v3_1;
        for (int v3_0 = 0; v3_0 < v0_0; v3_0++) {
            var v4_4 = com.oupao.wiz.Bar.bubble(p8, ((String) p10.get(v3_0)));
            p9.addView(v4_4, new android.widget.FrameLayout.LayoutParams(v1_1, v1_1));
            com.oupao.wiz.FlyH.items[(v3_0 + 1)] = v4_4;
        }
        return com.oupao.wiz.FlyH.items;
    }
 private static android.graphics.Bitmap badgeClone(android.graphics.Bitmap p17, int p18, String p19, String p20)
    {
        var v1 = p17;
        var v2 = p20;
        try {
            String[] v4 = p19.split("#");
            if (v4.length < 3) {
                return v1;
            }
            int v5 = Integer.parseInt(v4[2]);
            if (v5 <= 0) {
                return v1;
            }
            int n = com.oupao.wiz.Sug.cloneIndexOf(v2, v5);
            if (n <= 0) n = 1;
            // 3416：直接取系统原生分身角标 drawable（getUserBadgeForDensity →
            //   oplus-framework-res 的 oplus_ic_corp_badge_case_multiapp_N），
            //   不再从合成图标里裁 40px / 逐像素差分抠角标。
            var ctx0 = com.oupao.wiz.Cfg.sysContext();
            if (ctx0 != null) {
                try {
                    boolean left = (com.oupao.wiz.FlyH.side == 1);
                    var icon0 = ctx0.getPackageManager().getApplicationIcon(v2);
                    var bmSys = com.oupao.wiz.Sug.cloneBitmap(icon0, p18, v5, true, left);
                    if (bmSys != null) {
                        com.oupao.wiz.SLog.i("Fly: 分身原生角标 " + v2 + " #" + n + " left=" + left);
                        return bmSys;
                    }
                } catch (Throwable tb) {
                    com.oupao.wiz.SLog.e("Fly: 分身原生角标异常", tb);
                }
            }
            boolean left2 = (com.oupao.wiz.FlyH.side == 1);
            return com.oupao.wiz.Sug.numBadge(v1, p18, n, left2);
        } catch (Throwable t) {
            return v1;
        }
    }
 public static void boost(android.view.View p2, int p3)
    {
        try {
            p2.animate().cancel();
        } catch (Throwable __e_v0) {
        }
        p2.setAlpha(1.0f);
        p2.setScaleX(1.35f);
        p2.setScaleY(1.35f);
        return;
    }
 public static android.view.View bubble(android.content.Context p6, String p7)
    {
        if (!p7.startsWith("tool#")) {
            var v0_4 = new android.widget.ImageView(p6);
            var v1_1 = p7.split("#")[0];
            android.graphics.drawable.Drawable v2_1 = null;
            int v3_1 = 0;
            android.graphics.Bitmap v4 = null;
            try {
                v2_1 = p6.getPackageManager().getApplicationIcon(v1_1);
                v3_1 = com.oupao.wiz.FlyH.spxIcon(p6, 36);
                v4 = com.oupao.wiz.Sug.iconOf(v2_1, v3_1);
            } catch (Throwable __e_v2) {
                return v0_4;
            }
            if (v4 != null) {
                v4 = com.oupao.wiz.Sug.iconRound(v4, v3_1);
            }
            // v4 为空或透明度过低 → 用 circleFitIcon 自算包围盒重裁
            if ((v4 == null) || (com.oupao.wiz.Sug.opaque(v4) < 4)) {
                var v9 = com.oupao.wiz.Sug.circleFitIcon(v2_1, v3_1);
                if (v9 != null) {
                    var bmC9 = com.oupao.wiz.Bar.badgeClone(v9, v3_1, p7, v1_1);
                    v0_4.setImageBitmap(bmC9 != null ? bmC9 : v9);
                    return v0_4;
                }
            }
            if (v4 != null) {
                var bmC4 = com.oupao.wiz.Bar.badgeClone(v4, v3_1, p7, v1_1);
                v0_4.setImageBitmap(bmC4 != null ? bmC4 : v4);
                return v0_4;
            }
            com.oupao.wiz.SLog.i("app-round-NULL:" + v1_1 + " size=" + v3_1);
            if (v2_1 != null) {
                v0_4.setImageDrawable(v2_1);
            }
            return v0_4;
        }
        // tool# 前缀：走 Tools.drawable
        var v2_2 = com.oupao.wiz.Tools.drawable(p6, p7);
        if (v2_2 == null) {
            var v0_1 = new android.widget.TextView(p6);
            v0_1.setGravity(17);
            v0_1.setText(com.oupao.wiz.Tools.icon(p7));
            v0_1.setTextSize(22.0f);
            v0_1.setTextColor(com.oupao.wiz.Ui.adaptText(-1));
            v0_1.setBackground(com.oupao.wiz.Ui.oval(-0x2b2b2c));
            return v0_1;
        }
        var v0_5 = new android.widget.ImageView(p6);
        var v1_10 = com.oupao.wiz.FlyH.spxIcon(p6, 36);
        var v3_2 = com.oupao.wiz.Sug.iconOf(v2_2, v1_10);
        if (v3_2 != null) {
            v3_2 = com.oupao.wiz.Sug.iconRound(v3_2, v1_10);
        }
        if (v3_2 == null) {
            v0_5.setImageDrawable(v2_2);
            return v0_5;
        }
        v0_5.setImageBitmap(v3_2);
        return v0_5;
    }
    public static android.graphics.drawable.Drawable cloneDrawable(android.content.Context p11, String p12, android.graphics.drawable.Drawable p13)
    {
        if ((p11 == null) || (p12 == null)) {
            return p13;
        }
        if (p13 == null) {
            return p13;
        }
        try {
            String[] v0_1 = p12.split("#");
            if (v0_1.length < 3) {
                return p13;
            }
            int v1_2 = Integer.parseInt(v0_1[2]);
            if (v1_2 <= 0) {
                return p13;
            }
            int v5 = com.oupao.wiz.Ui.dp(p11, 40);
            // 3416：直接贴系统原生角标，不再裁切合成图标。
            var bm = com.oupao.wiz.Sug.cloneBitmap(p13, v5, v1_2, false, false);
            if (bm != null) {
                com.oupao.wiz.SLog.i("Fly: \u5361\u7247\u5206\u8eab\u539f\u751f\u89d2\u6807 " + v0_1[0]
                        + " uid=" + v1_2 + " size=" + v5);
                return new android.graphics.drawable.BitmapDrawable(p11.getResources(), bm);
            }
            return p13;
        } catch (Throwable __e_v0_2) {
            return p13;
        }
    }
}