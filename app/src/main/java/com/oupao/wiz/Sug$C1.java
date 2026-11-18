package com.oupao.wiz;
public class Sug$C1 implements android.view.View.OnTouchListener {
    private float downX;
    private float downY;
    public Sug$C1()
    {
        return;
    }
    public boolean onTouch(android.view.View p8, android.view.MotionEvent p9)
    {
        int v0 = 0;
        try {
            v0 = p9.getActionMasked();
        } catch (Throwable __e_v0) {
            return true;
        }
        if (v0 != 0) {
            if (v0 != 1) {
                if (v0 != 3) {
                    var v1_0 = (p9.getRawX() - this.downX);
                    var v4 = Math.abs((p9.getRawY() - this.downY));
                    if ((v4 > 60.0f) && (v4 > Math.abs(v1_0))) {
                        com.oupao.wiz.Sug.press();
                        com.oupao.wiz.Sug.dismiss();
                        // 4.0-fix：状态错位时 dismiss 可能删不到这个窗口 → 直接删"被滑的这个"
                        com.oupao.wiz.Sug.dropView(p8);
                    }
                } else {
                    com.oupao.wiz.Sug.press();
                }
            } else {
                com.oupao.wiz.Sug.press();
                com.oupao.wiz.Sug.fire();
                // 4.0-fix：同上 —— 被点的窗口必须消失，不能依赖 Sug.view 是否还指向它
                com.oupao.wiz.Sug.dropView(p8);
            }
        } else {
            this.downX = p9.getRawX();
            this.downY = p9.getRawY();
            com.oupao.wiz.Sug.press();
        }
        return true;
    }
}