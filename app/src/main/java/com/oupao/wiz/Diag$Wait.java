package com.oupao.wiz;
public class Diag$Wait implements java.lang.Runnable {
    public Diag$Wait()
    {
        return;
    }
    public void run()
    {
        try {
            com.oupao.wiz.Diag.showPending();
        } catch (Throwable __e___ex) {
        }
        return;
    }
}