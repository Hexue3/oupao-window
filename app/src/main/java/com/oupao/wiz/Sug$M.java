package com.oupao.wiz;
public class Sug$M implements java.lang.Runnable {
    private android.graphics.Bitmap bmp;
    private String pkg;
    public Sug$M(String p1, android.graphics.Bitmap p2)
    {
        this.pkg = p1;
        this.bmp = p2;
        return;
    }
    public void run()
    {
        try {
            var v0_0 = this.pkg;
            com.oupao.wiz.Sug.pkg = v0_0;
            com.oupao.wiz.Sug.kPkg = v0_0;
            com.oupao.wiz.Sug.kChat = null;
            com.oupao.wiz.Sug.kUin = null;
            com.oupao.wiz.Sug.kType = null;
            com.oupao.wiz.Sug.kNi = null;
            com.oupao.wiz.Sug.kPi = null;
            com.oupao.wiz.Sug.bmp = this.bmp;
            com.oupao.wiz.Sug.txt = "";
            com.oupao.wiz.Sug.show(5);
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}