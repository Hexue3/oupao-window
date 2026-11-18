package com.oupao.wiz;
public class MA$W2 implements java.lang.Runnable {
    public MA$W2()
    {
        return;
    }
    public static void go()
    {
        try {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(new com.oupao.wiz.MA$W2());
        } catch (Throwable __e___ex) {
        }
        return;
    }
    public void run()
    {
        try {
            com.oupao.wiz.MainActivity.buildMusic();
        } catch (Throwable __e___ex) {
        }
        return;
    }
}