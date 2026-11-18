package com.oupao.wiz;
public class Lp implements java.lang.Runnable {
    public Lp()
    {
        return;
    }
    public void run()
    {
        if (!com.oupao.wiz.MiniUI.lp()) {
            if (!com.oupao.wiz.Hooks.miniBig()) {
                com.oupao.wiz.Tap.fire();
                return;
            } else {
                return;
            }
        } else {
            return;
        }
    }
}