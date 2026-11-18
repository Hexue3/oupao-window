package com.oupao.wiz;
public class MA$RS implements java.lang.Runnable {
    private int p;
    public MA$RS(int p1)
    {
        this.p = p1;
        return;
    }
    public void run()
    {
        try {
            com.oupao.wiz.MainActivity.restoreScroll(this.p);
        } catch (Throwable __e___ex) {
        }
        return;
    }
}