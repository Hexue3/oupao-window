package com.oupao.wiz;
public class ToolR implements java.lang.Runnable {
    public ToolR()
    {
        return;
    }
    public void run()
    {
        try {
            com.oupao.wiz.Tools.firePend();
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}