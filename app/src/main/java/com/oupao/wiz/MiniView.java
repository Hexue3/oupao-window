package com.oupao.wiz;
public class MiniView extends android.view.View {
    private boolean bg;
    private boolean bigger;
    private float cx;
    private float cx2;
    private float cy1;
    private float cy2;
    private float d;
    private android.graphics.Paint.FontMetrics fm;
    private float[] g;
    private String ico1;
    private String ico2;
    private android.graphics.Rect mini;
    private boolean mu;
    private boolean muted;
    private int mv;
    private android.graphics.Paint pDim;
    private android.graphics.Paint pFill;
    private android.graphics.Paint pIco;
    private android.graphics.Paint pIco2;
    private int press;
    private float rad;
    private float sx;
    private float sy;
    public MiniView(android.content.Context p7)
    {
        super(p7);
        this.mini = new android.graphics.Rect();
        this.fm = new android.graphics.Paint.FontMetrics();
        float[] v0_18 = new float[6];
        this.g = v0_18;
        this.ico1 = "\u00d7";
        this.ico2 = "+";
        super.setWillNotDraw(false);
        this.press = 0;
        this.mv = 0;
        super.setFocusable(true);
        super.setFocusableInTouchMode(true);
        var v0_7 = new android.graphics.Paint();
        v0_7.setAntiAlias(true);
        v0_7.setColor(1711276032);
        this.pDim = v0_7;
        var v0_9 = new android.graphics.Paint();
        v0_9.setAntiAlias(true);
        v0_9.setColor(-335544321);
        this.pFill = v0_9;
        var v0_11 = new android.graphics.Paint();
        v0_11.setAntiAlias(true);
        v0_11.setFakeBoldText(true);
        v0_11.setColor(-13421773);
        v0_11.setTextAlign(android.graphics.Paint.Align.CENTER);
        this.pIco = v0_11;
        var v0_14 = new android.graphics.Paint();
        v0_14.setAntiAlias(true);
        v0_14.setStyle(android.graphics.Paint.Style.STROKE);
        v0_14.setColor(-13421773);
        v0_14.setStrokeWidth(0.16f);
        v0_14.setStrokeCap(android.graphics.Paint.Cap.ROUND);
        v0_14.setStrokeJoin(android.graphics.Paint.Join.ROUND);
        this.pIco2 = v0_14;
        this.press = 0;
        return;
    }
    private void btn(android.graphics.Canvas p12, int p13)
    {
        int v1_0 = 0;
        try {
            if (this.press == p13) {
                v1_0 = 153;
            } else {
                v1_0 = 235;
            }
        } catch (Throwable __e_v0_15) {
            com.oupao.wiz.MiniUI.ex(__e_v0_15);
            return;
        }
        float v5 = 0f;
        float v4 = 0f;
        var v2 = this.pFill;
        v2.setAlpha(v1_0);
        if (p13 != 1) {
            v4 = this.cx2;
            v5 = this.cy2;
        } else {
            v4 = this.cx;
            v5 = this.cy1;
        }
        var v8_1 = (this.d * 0.5f);
        p12.drawCircle(v4, v5, v8_1, v2);
        var v3_0 = (v8_1 * 0.85f);
        var v10_1 = new android.graphics.Path();
        if (p13 != 1) {
            if (this.bigger) {
                v10_1.moveTo(-0.5f, -0.5f);
                v10_1.lineTo(-0.15f, -0.15f);
                v10_1.lineTo(-0.15f, -0.45f);
                v10_1.moveTo(-0.15f, -0.15f);
                v10_1.lineTo(-0.45f, -0.15f);
                v10_1.moveTo(0.5f, 0.5f);
                v10_1.lineTo(0.15f, 0.15f);
                v10_1.lineTo(0.15f, 0.45f);
                v10_1.moveTo(0.15f, 0.15f);
                v10_1.lineTo(0.45f, 0.15f);
            } else {
                v10_1.moveTo(-0.15f, -0.15f);
                v10_1.lineTo(-0.5f, -0.5f);
                v10_1.lineTo(-0.5f, -0.15f);
                v10_1.moveTo(-0.5f, -0.5f);
                v10_1.lineTo(-0.15f, -0.5f);
                v10_1.moveTo(0.15f, 0.15f);
                v10_1.lineTo(0.5f, 0.5f);
                v10_1.lineTo(0.5f, 0.15f);
                v10_1.moveTo(0.5f, 0.5f);
                v10_1.lineTo(0.15f, 0.5f);
            }
        } else {
            if (!this.muted) {
                com.oupao.wiz.MiniView.spkPath(v10_1);
                v10_1.moveTo(0.28f, -0.25f);
                v10_1.lineTo(0.58f, 0.28f);
                v10_1.moveTo(0.58f, -0.25f);
                v10_1.lineTo(0.28f, 0.28f);
            } else {
                com.oupao.wiz.MiniView.spkPath(v10_1);
                v10_1.moveTo(0.2f, -0.2f);
                v10_1.lineTo(0.2f, 0.2f);
                v10_1.moveTo(0.4f, -0.4f);
                v10_1.lineTo(0.4f, 0.4f);
            }
        }
        p12.save();
        p12.translate(v4, v5);
        p12.scale(v3_0, v3_0);
        p12.drawPath(v10_1, this.pIco2);
        p12.restore();
        return;
    }
    public static void geo(android.graphics.Rect p8, float[] p9)
    {
        if ((p8 != null) && (p9 != null)) {
            int v1_3 = p8.width();
            var v2_5 = p8.height();
            if (v1_3 > v2_5) {
                v1_3 = v2_5;
            }
            var v1_2 = Math.min(Math.max((((float) v1_3) * 0.38f), 80.0f), 180.0f);
            p9[0] = v1_2;
            var v2_4 = ((float) p8.centerX());
            p9[1] = v2_4;
            p9[5] = v2_4;
            var v3_3 = ((float) p8.centerY());
            var v4_1 = (v1_2 * 0.85f);
            p9[2] = (v3_3 - v4_1);
            p9[3] = (v3_3 + v4_1);
            p9[4] = (((float) p8.width()) * 0.06f);
            if (p8.width() > p8.height()) {
                // 3415：横屏 / 宽迷你窗（视频类，w>h）走「两个按钮左右并排」。
                //   ⚠️ 这里原本写的是 `p9[0] * 1062836634` —— 1062836634 是反编译把
                //   float 1.0625f 的位模式(0x3F880000)当成了整数字面量，实际算出
                //   按钮偏移 ≈ 1.1e11 px → 遮罩照常盖住窗口、**两个按钮全在屏幕外**，
                //   用户看到的就是「横屏迷你窗长按只出遮罩、没有放大/静音按钮」。
                float off = p9[0] * 1.0625f;
                // 再兜一层：按钮不许跑出窗口本身（窄窗时 1.0625d 会顶到边界外）
                float maxOff = Math.max(0f, (p8.width() * 0.5f) - (p9[0] * 0.5f) - 6f);
                if (off > maxOff) {
                    off = maxOff;
                }
                var v0 = p9[1];
                p9[1] = (v0 - off);
                p9[5] = (v0 + off);
                var v5_8 = ((float) p8.centerY());
                p9[2] = v5_8;
                p9[3] = v5_8;
            } else {
                // 竖窗：上下排；同样别让按钮跑出窗口
                float maxV = Math.max(0f, (p8.height() * 0.5f) - (p9[0] * 0.5f) - 6f);
                if (v4_1 > maxV) {
                    v4_1 = maxV;
                    p9[2] = (v3_3 - v4_1);
                    p9[3] = (v3_3 + v4_1);
                }
            }
        }
        return;
    }
    public static int hitIn(float[] p9, float p10, float p11)
    {
        int v0 = 0;
        if (p9 != null) {
            var v1_3 = p9[0];
            if (v1_3 > 0) {
                var v8 = (((v1_3 * 0.5f) + 10.0f) * ((v1_3 * 0.5f) + 10.0f));
                if ((((p10 - p9[1]) * (p10 - p9[1])) + ((p11 - p9[2]) * (p11 - p9[2]))) >= v8) {
                    if ((((p10 - p9[5]) * (p10 - p9[5])) + ((p11 - p9[3]) * (p11 - p9[3]))) < v8) {
                        v0 = 2;
                    }
                } else {
                    v0 = 1;
                }
            }
        }
        return v0;
    }
    private static String pick(android.graphics.Paint p2, String p3, String p4)
    {
        try {
            if (!p2.hasGlyph(p3)) {
                return p4;
            } else {
                return p3;
            }
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.MiniUI.ex(__e_v0_1);
            return p3;
        }
    }
    private static void spkPath(android.graphics.Path p8)
    {
        p8.moveTo(-0.5f, -0.2f);
        p8.lineTo(-0.25f, -0.2f);
        p8.lineTo(0, -0.5f);
        p8.lineTo(0, 0.5f);
        p8.lineTo(-0.25f, 0.2f);
        p8.lineTo(-0.5f, 0.2f);
        try {
            p8.close();
        } catch (Throwable __e_io) {
        }
        return;
    }
    int hit(float p4, float p5)
    {
        int v0_1 = 0;
        try {
            v0_1 = com.oupao.wiz.MiniView.hitIn(this.g, p4, p5);
        } catch (Throwable __e_v0_2) {
            com.oupao.wiz.MiniUI.ex(__e_v0_2);
            return 0;
        }
        if (v0_1 != 1) {
            if (v0_1 == 2) {
                if (!this.bg) {
                    v0_1 = 0;
                } else {
                }
            }
        } else {
            // 照 MiniView.smali 的 hit(FF)I 还原：
            //   if (hitIn==1) { if (!mu) goto cond_16 }   // cond_16: v0 = 0
            // 原来这里两个分支都是空的 → !mu 时恒返回 1（把不可点的按钮也判成命中）。
            if (!this.mu) {
                v0_1 = 0;
            } else {
            }
        }
        return v0_1;
    }
    public void onDraw(android.graphics.Canvas p13)
    {
        android.graphics.Rect v8 = null;
        try {
            v8 = this.mini;
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.MiniUI.ex(__e_v0_1);
            return;
        }
        if (!v8.isEmpty()) {
            var v5 = this.rad;
            p13.drawRoundRect(((float) v8.left), ((float) v8.top), ((float) v8.right), ((float) v8.bottom), v5, v5, this.pDim);
            if (this.mu) {
                this.btn(p13, 1);
            }
            if (this.bg) {
                this.btn(p13, 2);
            }
            return;
        } else {
            return;
        }
    }
    public boolean onKeyDown(int p2, android.view.KeyEvent p3)
    {
        try {
            if (p2 != 4) {
                return super.onKeyDown(p2, p3);
            } else {
                com.oupao.wiz.MiniUI.hide();
                return true;
            }
        } catch (Throwable __e_v0_4) {
            com.oupao.wiz.MiniUI.ex(__e_v0_4);
            return true;
        }
    }
    public boolean onTouchEvent(android.view.MotionEvent p7)
    {
        int v0_0 = 0;
        float v1_0 = 0;
        float v2_1 = 0;
        try {
            v0_0 = p7.getActionMasked();
            v1_0 = p7.getRawX();
            v2_1 = p7.getRawY();
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.MiniUI.ex(__e_v0_1);
            return true;
        }
        if (v0_0 != 0) {
            if (v0_0 != 1) {
                if ((v0_0 == 2) && (Math.abs((v1_0 - this.sx)) > 60.0f)) {
                    var v3_4 = this.getWidth();
                    if (v3_4 > 0) {
                        var v4_1 = this.sx;
                        if (v4_1 >= 30.0f) {
                            if (v4_1 >= (((float) v3_4) - 30.0f)) {
                                this.mv = 1;
                            }
                        } else {
                            this.mv = 1;
                        }
                        if (this.mv != 0) {
                            com.oupao.wiz.MiniUI.lg2("MiniView: edge-back", 0, 0);
                            com.oupao.wiz.MiniUI.hide();
                            return true;
                        }
                    }
                }
                if (v0_0 == 3) {
                    this.press = 0;
                    this.invalidate();
                }
            } else {
                var v1_1 = this.hit(v1_0, v2_1);
                var v2_0 = this.press;
                this.press = 0;
                this.invalidate();
                com.oupao.wiz.MiniUI.lg2("MiniView: up hit=", v1_1, v2_0);
                if ((v1_1 != v2_0) || (v1_1 == 0)) {
                    com.oupao.wiz.MiniUI.hide();
                } else {
                    com.oupao.wiz.MiniUI.act(v1_1);
                }
            }
        } else {
            this.sx = v1_0;
            this.sy = v2_1;
            this.mv = 0;
            var v3_17 = this.hit(v1_0, v2_1);
            this.press = v3_17;
            com.oupao.wiz.MiniUI.lg2("MiniView: dn hit=", v3_17, v3_17);
            this.invalidate();
            if (v3_17 == 0) {
                com.oupao.wiz.MiniUI.hide();
            }
        }
        return true;
    }
    public void setMini(android.graphics.Rect p7)
    {
        try {
            var v0_0 = this.mini;
            v0_0.set(p7);
            var v1 = this.g;
            com.oupao.wiz.MiniView.geo(v0_0, v1);
            var v2_1 = v1[0];
            this.d = v2_1;
            this.cx = v1[1];
            this.cx2 = v1[5];
            this.cy1 = v1[2];
            this.cy2 = v1[3];
            this.rad = v1[4];
            this.pIco.setTextSize((v2_1 * 0.5f));
            this.invalidate();
            return;
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.MiniUI.ex(__e_v0_1);
            return;
        }
    }
    public void setState(boolean p6, boolean p7)
    {
        this.muted = p6;
        this.bigger = p7;
        this.mu = com.oupao.wiz.Cfg.muteDirect();
        this.bg = com.oupao.wiz.Cfg.miniBig();
        var v0_2 = this.pIco;
        var v3_0 = com.oupao.wiz.MiniView.pick(v0_2, "\ud83d\udd07", "\u00d7");
        var vtmp4 = com.oupao.wiz.MiniView.pick(v0_2, "\ud83d\udd0a", "\u266a");
        if (!p6) {
            this.ico1 = v3_0;
        } else {
            this.ico1 = vtmp4;
        }
        var v3_1 = com.oupao.wiz.MiniView.pick(v0_2, "\u2922", "+");
        var vtmp6 = com.oupao.wiz.MiniView.pick(v0_2, "\u2921", "-");
        if (!p7) {
            this.ico2 = v3_1;
        } else {
            this.ico2 = vtmp6;
        }
        this.invalidate();
        return;
    }
}