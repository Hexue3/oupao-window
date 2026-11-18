package com.oupao.wiz;
public class FlyB$C1 extends com.oupao.wiz.XMH {
    public FlyB$C1()
    {
        return;
    }
    public void afterHookedMethod(com.oupao.wiz.XMP p2)
    {
        try {
            if ((com.oupao.wiz.Cfg.flyUi()) && ((com.oupao.wiz.Cfg.flyBordOff()) && ((p2.result instanceof Integer)))) {
                p2.setResult(Integer.valueOf(0));
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
}