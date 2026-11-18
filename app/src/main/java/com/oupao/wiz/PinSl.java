package com.oupao.wiz;
public class PinSl implements android.view.View.OnTouchListener {
    private static int last;
    private int base;
    private android.view.View page;
    static
    {
        com.oupao.wiz.PinSl.last = -1;
    }
    public PinSl(android.view.View p2, int p3)
    {
        this.page = p2;
        this.base = p3;
        return;
    }
    public boolean onTouch(android.view.View p13, android.view.MotionEvent p14)
    {
        var v0_0 = p14.getActionMasked();
        if ((v0_0 == 1) || (v0_0 == 3)) {
            com.oupao.wiz.PinSl.last = -1;
            return false;
        } else {
            int v4_0 = 0;
            var v2_1 = ((int) p14.getY());
            var v3 = p13.getHeight();
            if (v3 > 0) {
                if (v2_1 >= 0) {
                    v4_0 = (v2_1 / v3);
                } else {
                    v4_0 = (0 - ((((0 - v2_1) + v3) - 1) / v3));
                }
            } else {
                v4_0 = 0;
            }
            int v4_6 = (v4_0 + this.base);
            if (v4_6 < 0) {
                v4_6 = 0;
            }
            if (v4_6 > 27) {
                v4_6 = 27;
            }
            if (v4_6 != com.oupao.wiz.PinSl.last) {
                com.oupao.wiz.PinSl.last = v4_6;
                var v6 = com.oupao.wiz.PinUI.letterAt(v4_6);
                var v7_1 = new StringBuilder();
                v7_1.append("slide idx=");
                v7_1.append(v4_6);
                v7_1.append(" letter=");
                v7_1.append(v6);
                com.oupao.wiz.SLog.i(v7_1.toString());
                var v7_2 = this.page;
                if (v0_0 != 0) {
                    com.oupao.wiz.PinIdx.go(v7_2, v6);
                } else {
                    com.oupao.wiz.PinIdx.goQ(v7_2, v6);
                }
            }
            return false;
        }
    }
}