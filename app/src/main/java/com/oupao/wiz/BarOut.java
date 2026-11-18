package com.oupao.wiz;
public class BarOut implements java.lang.Runnable {
    private android.view.WindowManager m;
    private android.view.View v;
    public BarOut(android.view.WindowManager p1, android.view.View p2)
    {
        this.m = p1;
        this.v = p2;
        return;
    }
    public void run()
    {
        android.view.View v0 = null;
        try {
            v0 = this.v;
        } catch (Throwable __e_v2) {
            return;
        }
        if (v0 != null) {
            var v1 = this.m;
            if (v1 != null) {
                v1.removeView(v0);
            }
        }
        return;
    }
}