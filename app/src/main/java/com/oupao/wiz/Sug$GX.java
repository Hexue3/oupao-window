package com.oupao.wiz;
public class Sug$GX extends com.oupao.wiz.XMH {
    public Sug$GX()
    {
        return;
    }
    public void beforeHookedMethod(com.oupao.wiz.XMP p4)
    {
        java.lang.Object[] v0_4 = null;
        try {
            com.oupao.wiz.Sug.pendGame = false;
            v0_4 = p4.args;
        } catch (Throwable __e___ex) {
            return;
        }
        if ((v0_4 != null) && (v0_4.length >= 1)) {
            var v0_1 = com.oupao.wiz.Rt.get(v0_4[0], "mTopPkg");
            if ((v0_1 instanceof String)) {
                com.oupao.wiz.Sug.pendPkg = ((String) v0_1);
            }
        }
        var v0_3 = com.oupao.wiz.Sug.main;
        if (v0_3 != null) {
            v0_3.post(new com.oupao.wiz.Sug$Fg2());
        }
        return;
    }
}