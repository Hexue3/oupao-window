package com.oupao.wiz;
public class Sug$S implements java.lang.Runnable {
    private android.graphics.Bitmap bmp;
    private String pkg;
    private String txt;
    public Sug$S(String p1, String p2, android.graphics.Bitmap p3)
    {
        this.pkg = p1;
        this.txt = p2;
        this.bmp = p3;
        return;
    }
    public void run()
    {
        try {
            if (!com.oupao.wiz.Dock.screenOff()) {
                com.oupao.wiz.Sug.pkg = this.pkg;
                com.oupao.wiz.Sug.bmp = this.bmp;
                com.oupao.wiz.Sug.txt = this.txt;
                // 形态合并已统一放在 Sug.show() 末尾（覆盖所有显示路径），这里不再重复调用
                com.oupao.wiz.Sug.show(2);
                return;
            } else {
                com.oupao.wiz.SLog.k("Sug: \u606f\u5c4f -> skip notify bubble");
                return;
            }
        } catch (Throwable __e_v0) {
            return;
        }
    }
}