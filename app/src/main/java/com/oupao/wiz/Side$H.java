package com.oupao.wiz;
public class Side$H extends com.oupao.wiz.XMH {
    public Side$H()
    {
        return;
    }
    public void beforeHookedMethod(com.oupao.wiz.XMP p3)
    {
        java.lang.Object[] v0_0 = null;
        try {
            v0_0 = p3.args;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_0 != null) {
            if (v0_0.length != 0) {
                var v0_2 = ((String) v0_0[0]);
                if ((v0_2 == null) || (!"com.tencent.mm".equals(v0_2))) {
                    return;
                } else {
                    com.oupao.wiz.SLog.i("Side: \u5fae\u4fe1\u5728\u524d\u53f0, \u4fa7\u8fb9\u680f\u653e\u884c");
                    p3.setResult(Boolean.FALSE);
                }
            } else {
            }
            return;
        }
        return;
    }
}