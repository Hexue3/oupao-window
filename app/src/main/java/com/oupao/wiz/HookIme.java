package com.oupao.wiz;
public class HookIme extends com.oupao.wiz.XMH {
    public HookIme()
    {
        return;
    }
    public void beforeHookedMethod(com.oupao.wiz.XMP p4)
    {
        java.lang.Object[] v0_0 = null;
        try {
            v0_0 = p4.args;
        } catch (Throwable __e_v0) {
            return;
        }
        try {
            // 2812：把 IMMS 实例存下来（system_server 里唯一能直接收键盘的抓手）
            Object self = p4.thisObject;
            if (self != null) {
                com.oupao.wiz.Hooks.sIme = self;
            }
        } catch (Throwable ignore) {
        }
        if (v0_0 != null) {
            var v0_1 = v0_0[0];
            if ((v0_1 instanceof Integer)) {
                int v1_0 = 0;
                if (((Integer) v0_1).intValue() >= 2) {
                    v1_0 = 1;
                }
                com.oupao.wiz.Sug.imeVis = v1_0;
                if (v1_0 != 0) {
                    com.oupao.wiz.Sug.imeCame();
                } else {
                    com.oupao.wiz.Sug.imeGone();
                }
                return;
            } else {
                return;
            }
        } else {
            return;
        }
    }
}