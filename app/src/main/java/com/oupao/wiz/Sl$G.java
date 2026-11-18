package com.oupao.wiz;
public class Sl$G implements android.view.View.OnTouchListener {
    public Sl$G()
    {
        return;
    }
    public boolean onTouch(android.view.View p4, android.view.MotionEvent p5)
    {
        try {
            if (p5.getActionMasked() == 0) {
                com.oupao.wiz.Sl$C.fx = ((int) p5.getRawX());
            }
        } catch (Throwable __e___ex) {
            return false;
        }
        return false;
    }
}