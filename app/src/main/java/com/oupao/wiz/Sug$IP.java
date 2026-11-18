package com.oupao.wiz;
public class Sug$IP implements java.lang.Runnable {
    private final String p;
    public Sug$IP(String p1)
    {
        this.p = p1;
        return;
    }
    public void run()
    {
        try {
            com.oupao.wiz.Sug.install(this.p);
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}