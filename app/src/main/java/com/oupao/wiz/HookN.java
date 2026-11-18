package com.oupao.wiz;
public class HookN extends com.oupao.wiz.XMH {
    public HookN()
    {
        return;
    }
    public void beforeHookedMethod(com.oupao.wiz.XMP p1)
    {
        try {
            com.oupao.wiz.Hooks.onNotify(p1);
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}