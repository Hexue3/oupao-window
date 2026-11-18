package com.oupao.wiz;
public class Sug$GE extends com.oupao.wiz.XMH {
    public Sug$GE()
    {
        return;
    }
    public void beforeHookedMethod(com.oupao.wiz.XMP p5)
    {
        java.lang.Object[] v0_6 = null;
        try {
            com.oupao.wiz.Sug.pendGame = false;
            com.oupao.wiz.Sug.pendGame = true;
            v0_6 = p5.args;
        } catch (Throwable __e___ex) {
            return;
        }
        if ((v0_6 != null) && (v0_6.length >= 1)) {
            var v0_2 = com.oupao.wiz.Rt.get(v0_6[0], "mTopPkg");
            if ((v0_2 instanceof String)) {
                com.oupao.wiz.Sug.pendPkg = ((String) v0_2);
            }
        }
        var v0_4 = com.oupao.wiz.Sug.main;
        if (v0_4 != null) {
            v0_4.post(new com.oupao.wiz.Sug$Fg2());
        }
        return;
    }
}