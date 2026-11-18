package com.oupao.wiz;
public class Sug$L implements java.lang.Runnable {
    private android.graphics.Bitmap bmp;
    private String link;
    private String pkg;
    public Sug$L(String p1, String p2, android.graphics.Bitmap p3)
    {
        this.pkg = p1;
        this.link = p2;
        this.bmp = p3;
        return;
    }
    public void run()
    {
        int v0_4 = 4;
        boolean wasWord = false;
        try {
            var v0_0 = this.pkg;
            com.oupao.wiz.Sug.pkg = v0_0;
            com.oupao.wiz.Sug.kPkg = v0_0;
            com.oupao.wiz.Sug.kChat = null;
            com.oupao.wiz.Sug.kUin = null;
            com.oupao.wiz.Sug.kType = null;
            com.oupao.wiz.Sug.kNi = null;
            com.oupao.wiz.Sug.kPi = null;
            com.oupao.wiz.Sug.link = this.link;
            com.oupao.wiz.Sug.bmp = this.bmp;
            com.oupao.wiz.Sug.txt = "";
            // smali: sget-boolean v0, wordBubble（旧值必须先取）→ sput false → if-eqz 决定 7 还是 4
            wasWord = com.oupao.wiz.Sug.wordBubble;
            com.oupao.wiz.Sug.wordBubble = false;
        } catch (Throwable __e_v0) {
            return;
        }
        v0_4 = wasWord ? 7 : 4;
        com.oupao.wiz.Sug.show(v0_4);
        return;
    }
}
