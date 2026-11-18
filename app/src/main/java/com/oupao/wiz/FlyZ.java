package com.oupao.wiz;
public class FlyZ implements java.lang.Runnable {
    public FlyZ()
    {
        return;
    }
    public void run()
    {
        try {
            com.oupao.wiz.SLog.i("FlyZ: run \u8fdb\u5165");
            com.oupao.wiz.FlyS.sync();
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}