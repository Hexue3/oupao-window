package com.oupao.wiz;
public class Sug$IH extends com.oupao.wiz.XMH {
    public Sug$IH()
    {
        return;
    }
    public void afterHookedMethod(com.oupao.wiz.XMP p2)
    {
        try {
            com.oupao.wiz.Sug.onHookPkg(p2.args);
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}