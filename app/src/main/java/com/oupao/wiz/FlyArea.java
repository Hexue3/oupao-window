package com.oupao.wiz;
public class FlyArea extends android.view.View {
    public static int lv;
    public static com.oupao.wiz.FlyArea one;
    public FlyArea(android.content.Context p2)
    {
        super(p2);
        super.setFocusable(false);
        super.setClickable(false);
        return;
    }
    private static void arc(android.graphics.Canvas p6, android.graphics.Paint p7, float p8, float p9, float p10)
    {
        float v2_0 = 0;
        android.graphics.Path v0_1 = null;
        android.graphics.RectF v1_1 = null;
        try {
            v0_1 = new android.graphics.Path();
            v0_1.moveTo(p8, p9);
            v1_1 = new android.graphics.RectF((p8 - p10), (p9 - p10), (p8 + p10), (p9 + p10));
        } catch (Throwable __e_v0) {
            return;
        }
        if (p8 > 0) {
            v2_0 = 180.0f;
        } else {
            v2_0 = -90.0f;
        }
        v0_1.arcTo(v1_1, v2_0, 90.0f);
        try {
            v0_1.close();
            p6.drawPath(v0_1, p7);
        } catch (Throwable __e_io) {
        }
        return;
    }
    public static void clear()
    {
        com.oupao.wiz.FlyArea.lv = 0;
        com.oupao.wiz.FlyArea.hide();
        return;
    }
    public static void hide()
    {
        com.oupao.wiz.FlyArea v0 = null;
        try {
            v0 = com.oupao.wiz.FlyArea.one;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 != null) {
            v0.setVisibility(8);
        }
        return;
    }
    public static void setLive(int p0)
    {
        com.oupao.wiz.FlyArea.lv = p0;
        com.oupao.wiz.FlyArea.show();
        return;
    }
    public static void show()
    {
        android.widget.FrameLayout v0_2 = null;
        try {
            com.oupao.wiz.SLog.i("show() \u8fdb\u5165");
            v0_2 = com.oupao.wiz.MainActivity.pageBox;
        } catch (Throwable __e_v0_0) {
            com.oupao.wiz.SLog.i(String.valueOf(__e_v0_0));
            return;
        }
        if (v0_2 != null) {
            com.oupao.wiz.FlyArea v1_1 = com.oupao.wiz.FlyArea.one;
            if (v1_1 == null) {
                v1_1 = new com.oupao.wiz.FlyArea(v0_2.getContext());
                com.oupao.wiz.FlyArea.one = v1_1;
                v0_2.addView(v1_1, new android.widget.FrameLayout.LayoutParams(-1, -1));
            }
            com.oupao.wiz.SLog.i("show(): \u89c6\u56fe\u5c31\u7eea");
            v1_1.setVisibility(0);
            v1_1.setTranslationZ(100.0f);
            v1_1.bringToFront();
            v1_1.invalidate();
        }
        return;
    }
    public void onDraw(android.graphics.Canvas p15)
    {
        int v0 = com.oupao.wiz.FlyArea.lv;
        if (v0 == 0) {
            v0 = com.oupao.wiz.Cfg.flyRange();
        }
        var v2_0 = (((float) v0) * this.getResources().getDisplayMetrics().density);
        var v3 = this.getWidth();
        var v4 = this.getHeight();
        var v5_1 = new android.graphics.Paint();
        v5_1.setAntiAlias(true);
        v5_1.setColor(872377088);
        v5_1.setStyle(android.graphics.Paint.Style.FILL);
        // 3261：边条形状 → 预览画"贴上/下边的竖条"（宽 r、高 2r），和真实热区一致
        if (com.oupao.wiz.Cfg.flyZoneMode() == 1) {
            float bw = v2_0;
            float bh = v2_0 * 2f;
            if (com.oupao.wiz.Cfg.flyLeft()) {
                p15.drawRect(0f, ((float) v4) - bh, bw, (float) v4, v5_1);
            }
            if (com.oupao.wiz.Cfg.flyRight()) {
                p15.drawRect(((float) v3) - bw, ((float) v4) - bh, (float) v3, (float) v4, v5_1);
            }
            return;
        }
        if (com.oupao.wiz.Cfg.flyLeft()) {
            com.oupao.wiz.FlyArea.arc(p15, v5_1, 0, ((float) v4), v2_0);
        }
        if (com.oupao.wiz.Cfg.flyRight()) {
            com.oupao.wiz.FlyArea.arc(p15, v5_1, ((float) v3), ((float) v4), v2_0);
        }
        return;
    }
}