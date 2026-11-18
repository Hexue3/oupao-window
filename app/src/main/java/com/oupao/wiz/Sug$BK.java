package com.oupao.wiz;
public class Sug$BK extends com.oupao.wiz.XMH {
    public Sug$BK()
    {
        return;
    }
    public void beforeHookedMethod(com.oupao.wiz.XMP p4)
    {
        java.lang.Object v0_3 = null;
        try {
            v0_3 = p4.args[4];
        } catch (Throwable __e_v0) {
            return;
        }
        if ((v0_3 instanceof android.content.Intent)) {
            var v0_5 = ((android.content.Intent) v0_3).getPackage();
            if (v0_5 != null) {
                var v1_3 = com.oupao.wiz.Sug.pkg;
                if ((v1_3 != null) && (v1_3.equals(v0_5))) {
                    p4.setResult(false);
                }
            }
        }
        return;
    }
}