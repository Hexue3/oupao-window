package com.oupao.wiz;
public class Hooks$SB extends com.oupao.wiz.XMH {
    public Hooks$SB()
    {
        return;
    }
    public void beforeHookedMethod(com.oupao.wiz.XMP p1)
    {
        try {
            com.oupao.wiz.Hooks.onSimpleBanner(p1);
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}
