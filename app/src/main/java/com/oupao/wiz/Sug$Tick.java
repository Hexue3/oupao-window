package com.oupao.wiz;
public class Sug$Tick implements java.lang.Runnable {
    public Sug$Tick()
    {
        return;
    }
    public void run()
    {
        try {
            com.oupao.wiz.Sug.tick();
        } catch (Throwable __e_v0) {
        }
        com.oupao.wiz.Sug.repostTick(this);
        return;
    }
}