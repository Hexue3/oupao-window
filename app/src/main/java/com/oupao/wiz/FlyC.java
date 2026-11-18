package com.oupao.wiz;
public class FlyC implements java.lang.Runnable {
    public FlyC()
    {
        return;
    }
    public void run()
    {
        try {
            com.oupao.wiz.FlyS.syncMain();
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}