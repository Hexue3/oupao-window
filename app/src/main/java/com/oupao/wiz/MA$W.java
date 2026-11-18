package com.oupao.wiz;
public class MA$W implements java.lang.Runnable {
    public MA$W()
    {
        return;
    }
    public static void start()
    {
        try {
            new Thread(new com.oupao.wiz.MA$W()).start();
        } catch (Throwable __e___ex) {
        }
        return;
    }
    public void run()
    {
        try {
            com.oupao.wiz.MainActivity.scanMusic();
            com.oupao.wiz.MA$W2.go();
        } catch (Throwable __e___ex) {
        }
        return;
    }
}