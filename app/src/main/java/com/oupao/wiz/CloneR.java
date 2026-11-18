package com.oupao.wiz;
public class CloneR implements java.lang.Runnable {
    public CloneR()
    {
        return;
    }
    public void run()
    {
        try {
            com.oupao.wiz.FlyH.cloneScan();
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}