package com.oupao.wiz;
public class PgGrid {
    public static int gcol;
    public static android.widget.LinearLayout grow;
    public static android.view.View lastBadge;
    public static android.view.View lastCell;
    public static String lastLetter;
    private static String pendingEmoji;
    public PgGrid()
    {
        return;
    }
    public static void cell(android.content.Context p10, android.widget.LinearLayout p11, String p12, String p13, android.graphics.drawable.Drawable p14, int p15)
    {
        try {
            if (com.oupao.wiz.PgGrid.gcol == 0) {
                var v1_3 = new android.widget.LinearLayout(p10);
                v1_3.setOrientation(0);
                v1_3.setGravity(0);
                com.oupao.wiz.PgGrid.grow = v1_3;
                if (p11.getChildCount() <= 0) {
                    p11.addView(v1_3);
                } else {
                    var v2_2 = new android.widget.LinearLayout.LayoutParams(-2, -2);
                    v2_2.topMargin = com.oupao.wiz.Ui.dp(p10, 14);
                    p11.addView(v1_3, v2_2);
                }
            }
        } catch (Throwable __e_v9_0) {
            com.oupao.wiz.SLog.e(__e_v9_0.toString());
            return;
        }
        var v1_2 = com.oupao.wiz.PgGrid.grow;
        v1_2.setClipChildren(false);
        var v0_5 = ((com.oupao.wiz.PgGrid.gcol + 1) % 4);
        com.oupao.wiz.PgGrid.gcol = v0_5;
        if (v0_5 == 0) {
            com.oupao.wiz.PgGrid.grow = null;
        }
        android.view.View v4_2 = null;
        var v2_4 = new android.widget.LinearLayout(p10);
        v2_4.setOrientation(1);
        v2_4.setGravity(17);
        v2_4.setClipChildren(false);
        var v3_7 = new android.widget.FrameLayout(p10);
        v3_7.setClipChildren(false);
        if (p14 == null) {
            v4_2 = new android.widget.TextView(p10);
            ((android.widget.TextView) v4_2).setGravity(17);
            ((android.widget.TextView) v4_2).setTextSize(26.0f);
            var v6_5 = new android.widget.FrameLayout.LayoutParams(com.oupao.wiz.Ui.dp(p10, 34), com.oupao.wiz.Ui.dp(p10, 34));
            v6_5.gravity = 81;
            ((android.widget.TextView) v4_2).setLayoutParams(v6_5);
            var v5_22 = com.oupao.wiz.PgGrid.pendingEmoji;
            if (v5_22 != null) {
                ((android.widget.TextView) v4_2).setText(v5_22);
                com.oupao.wiz.PgGrid.pendingEmoji = null;
            }
            ((android.widget.TextView) v4_2).setBackground(com.oupao.wiz.Ui.oval(-2829100));
        } else {
            v4_2 = new android.widget.ImageView(p10);
            var v5_27 = com.oupao.wiz.Ui.dp(p10, 34);
            var v6_7 = android.graphics.Bitmap.createBitmap(v5_27, v5_27, android.graphics.Bitmap.Config.ARGB_8888);
            var v8_12 = new android.graphics.Canvas(v6_7);
            var v0_8 = new android.graphics.Path();
            v0_8.addCircle((((float) v5_27) * 0.5f), (((float) v5_27) * 0.5f), (((float) v5_27) * 0.5f), android.graphics.Path.Direction.CW);
            v8_12.clipPath(v0_8);
            p14.setBounds(0, 0, v5_27, v5_27);
            p14.draw(v8_12);
            ((android.widget.ImageView) v4_2).setImageBitmap(v6_7);
            var v6_9 = new android.widget.FrameLayout.LayoutParams(v5_27, v5_27);
            v6_9.gravity = 81;
            ((android.widget.ImageView) v4_2).setLayoutParams(v6_9);
        }
        android.widget.TextView v7_1 = null;
        var v6_0 = new android.widget.FrameLayout.LayoutParams(com.oupao.wiz.Ui.dp(p10, 18), com.oupao.wiz.Ui.dp(p10, 18));
        v6_0.gravity = 53;
        if (p15 != 0) {
            java.lang.String v5_6 = null;
            int v0_1 = 0;
            v7_1 = new android.widget.TextView(p10);
            v7_1.setGravity(17);
            v7_1.setTextSize(9.0f);
            v7_1.setTextColor(com.oupao.wiz.Ui.adaptText(-1));
            if (p15 != 10000) {
                if (p15 != 10001) {
                    v5_6 = "\u2212";
                    v0_1 = -50384;
                } else {
                    v5_6 = "\u2713";
                    v0_1 = -7434605;
                }
            } else {
                v5_6 = "+";
                v0_1 = -13318311;
            }
            v7_1.setText(v5_6);
            v7_1.setBackground(com.oupao.wiz.Ui.oval(v0_1));
            com.oupao.wiz.PgGrid.lastBadge = v7_1;
            v7_1.setElevation(((float) com.oupao.wiz.Ui.dp(p10, 6)));
        }
        v3_7.addView(v4_2);
        if (v7_1 != null) {
            v3_7.addView(v7_1, v6_0);
        }
        v2_4.addView(v3_7, new android.widget.LinearLayout.LayoutParams(com.oupao.wiz.Ui.dp(p10, 42), com.oupao.wiz.Ui.dp(p10, 42)));
        var v8_7 = com.oupao.wiz.Ui.t(p10, p12, 12.0f, -12303292, false);
        v8_7.setGravity(17);
        // 3161：名称固定两行高度（单行的也占两行位）。
        // 之前这里没有固定高度，1 行/2 行的名字会让每个格子的高度不一样，
        // 同一屏两次打开时卡片高度、区块间距就跟着变（用户报的"间距有时不一致"）。
        v8_7.setMaxLines(2);
        v8_7.setEllipsize(android.text.TextUtils.TruncateAt.END);
        v2_4.addView(v8_7, new android.widget.LinearLayout.LayoutParams(-1, com.oupao.wiz.Ui.dp(p10, 32)));
        v1_2.addView(v2_4, new android.widget.LinearLayout.LayoutParams(((p10.getResources().getDisplayMetrics().widthPixels - com.oupao.wiz.Ui.dp(p10, 68)) / 4), -2));
        com.oupao.wiz.PgGrid.lastCell = v2_4;
        v2_4.setTag(p13);
        return;
    }
    public static void reset()
    {
        android.widget.LinearLayout v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.PgGrid.grow;
        } catch (Throwable __e_v7) {
            return;
        }
        if (v0_0 != null) {
            // 3162：修正条件写反了（原来是 `== null` 才补位，等于永远 NPE/不补）。
            // 最后一行不满 4 格时补空 View，保证每行宽度/高度一致，
            // 否则卡片高度会随最后一行内容变化 → 区块间距忽大忽小。
            var v1 = com.oupao.wiz.PgGrid.gcol;
            if (v1 != 0) {
                var v2 = v0_0.getContext();
                while (v1 < 4) {
                    v0_0.addView(new android.view.View(v2), new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
                    v1++;
                }
            }
        }
        com.oupao.wiz.PgGrid.gcol = 0;
        com.oupao.wiz.PgGrid.grow = null;
        com.oupao.wiz.PgGrid.lastBadge = null;
        com.oupao.wiz.PgGrid.lastCell = null;
        return;
    }
    public static void setEmoji(String p1)
    {
        com.oupao.wiz.PgGrid.pendingEmoji = p1;
        return;
    }
}