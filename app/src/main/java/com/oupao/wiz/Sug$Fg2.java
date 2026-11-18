package com.oupao.wiz;
public class Sug$Fg2 implements java.lang.Runnable {
    public Sug$Fg2()
    {
        return;
    }
    public void run()
    {
        try {
            if (!com.oupao.wiz.Sug.pendGame) {
                com.oupao.wiz.Sug.gameStop();
                return;
            } else {
                java.lang.String v0_1 = com.oupao.wiz.Sug.pendPkg;
                if (v0_1 == null) {
                    v0_1 = com.oupao.wiz.Sug.fgPkg();
                }
                com.oupao.wiz.Sug.gameStart(v0_1);
                return;
            }
        } catch (Throwable __e___ex) {
            return;
        }
    }
}