package com.oupao.wiz;
public class Mute$C1 extends com.oupao.wiz.XMH {
    public Mute$C1()
    {
        return;
    }
    public void afterHookedMethod(com.oupao.wiz.XMP p3)
    {
        com.oupao.wiz.Mute.sPam = p3.thisObject;
        com.oupao.wiz.SLog.i(new StringBuilder("Mute: \u9759\u97f3\u901a\u9053\u5df2\u6355\u83b7 ").append(p3.thisObject).toString());
        return;
    }
}