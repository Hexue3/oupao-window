package com.oupao.wiz;
public class Sug$Boot implements java.lang.Runnable {
    public Sug$Boot()
    {
        return;
    }
    public void run()
    {
        try {
            Thread.sleep(15000);
        } catch (Throwable __e_v0) {
        }
        com.oupao.wiz.Sug.init();
        return;
    }
}