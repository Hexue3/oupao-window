package com.oupao.wiz;
public class CloneT implements java.lang.Runnable {
    public CloneT()
    {
        return;
    }
    public void run()
    {
        try {
            com.oupao.wiz.FlyH.cloneTick();
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}