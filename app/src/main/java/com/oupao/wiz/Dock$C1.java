package com.oupao.wiz;
public class Dock$C1 extends com.oupao.wiz.XMH {
    public Dock$C1()
    {
        return;
    }
    public void beforeHookedMethod(com.oupao.wiz.XMP p4)
    {
        java.lang.Object v0_1 = null;
        try {
            v0_1 = com.oupao.wiz.Rt.get(p4.thisObject, "mTask");
        } catch (Throwable __e_v0_2) {
            com.oupao.wiz.SLog.e("Dock: \u8bb0\u5f55\u5f02\u5e38", __e_v0_2);
            return;
        }
        if (v0_1 == null) {
            com.oupao.wiz.SLog.i("Dock: startToFloatAnimation mTask=null cls="
                    + (p4.thisObject == null ? "null" : p4.thisObject.getClass().getName()));
            return;
        } else {
            com.oupao.wiz.Dock.mark(v0_1);
            return;
        }
    }
}