package com.oupao.wiz;
public class WarmR implements java.lang.Runnable {
    public WarmR()
    {
        return;
    }
    public void run()
    {
        try {
            com.oupao.wiz.Warm.run();
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}