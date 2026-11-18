package com.oupao.wiz;
public class PushR implements java.lang.Runnable {
    public PushR()
    {
        return;
    }
    public void run()
    {
        try {
            com.oupao.wiz.Cfg.push();
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}