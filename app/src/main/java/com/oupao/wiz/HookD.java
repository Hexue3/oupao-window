package com.oupao.wiz;
public class HookD extends com.oupao.wiz.XMH {
    public HookD()
    {
        return;
    }
    public void afterHookedMethod(com.oupao.wiz.XMP p5)
    {
        java.lang.Object v0 = null;
        try {
            v0 = p5.thisObject;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 != null) {
            com.oupao.wiz.Sug.pg = v0;
            var v1 = p5.args;
            if ((v1 != null) && ((v1.length == 5) && (!com.oupao.wiz.Sug.hit2))) {
                com.oupao.wiz.Sug.hit2 = true;
                com.oupao.wiz.SLog.v("DimDbg: \u5c4f\u5e55\u7b56\u7565\u94a9\u5b50\u5df2\u89e6\u53d1");
            }
            com.oupao.wiz.Sug.onPolicy();
        }
        return;
    }
    public void beforeHookedMethod(com.oupao.wiz.XMP p3)
    {
        java.lang.Object v0_0 = null;
        try {
            v0_0 = p3.thisObject;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_0 == null) {
            return;
        } else {
            com.oupao.wiz.Sug.pg = v0_0;
            if (!com.oupao.wiz.Sug.hit) {
                com.oupao.wiz.Sug.hit = true;
                com.oupao.wiz.SLog.v("DimDbg: \u5df2\u6355\u83b7 PowerGroup \u5b9e\u4f8b");
            }
            return;
        }
    }
}