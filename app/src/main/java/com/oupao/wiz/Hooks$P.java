package com.oupao.wiz;
public class Hooks$P extends com.oupao.wiz.XMH {
    public Hooks$P()
    {
        return;
    }
    public void beforeHookedMethod(com.oupao.wiz.XMP p1)
    {
        try {
            com.oupao.wiz.Hooks.onPeek(p1);
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}