package com.oupao.wiz;
public class PosDrag implements android.view.View.OnTouchListener {
    private static boolean logged;
    public PosDrag()
    {
        return;
    }
    public boolean onTouch(android.view.View p9, android.view.MotionEvent p10)
    {
        try {
            if (p10.getActionMasked() == 2) {
                int[] v3_7 = new int[2];
                ((android.view.View) p9.getParent()).getLocationOnScreen(v3_7);
                var v6_1 = v3_7[0];
                var v7_1 = v3_7[1];
                var v1_0 = p10.getRawX();
                var v2 = p10.getRawY();
                var v4_2 = ((((int) v1_0) - (p9.getWidth() / 2)) - v6_1);
                var v5_2 = ((((int) v2) - (p9.getHeight() / 2)) - v7_1);
                if (v4_2 < 0) {
                    v4_2 = 0;
                }
                if (v5_2 < 0) {
                    v5_2 = 0;
                }
                var v3_4 = p9.getLayoutParams();
                if ((v3_4 instanceof android.widget.FrameLayout.LayoutParams)) {
                    var v3_5 = ((android.widget.FrameLayout.LayoutParams) v3_4);
                    v3_5.leftMargin = v4_2;
                    v3_5.topMargin = v5_2;
                    p9.setLayoutParams(v3_5);
                    if (!logged) {
                        logged = true;
                        com.oupao.wiz.SLog.i("Pos: drag to " + v4_2 + "," + v5_2
                                + " parent=" + v6_1 + "," + v7_1
                                + " w/h=" + p9.getWidth() + "/" + p9.getHeight());
                    }
                } else {
                    if (!logged) {
                        logged = true;
                        com.oupao.wiz.SLog.i("Pos: drag lp=" + v3_4);
                    }
                }
            }
        } catch (Throwable __e_v0) {
            return true;
        }
        return true;
    }
}