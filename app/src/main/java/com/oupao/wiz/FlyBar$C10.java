package com.oupao.wiz;
public class FlyBar$C10 extends com.oupao.wiz.XMH {
    public FlyBar$C10()
    {
        return;
    }
    public void afterHookedMethod(com.oupao.wiz.XMP p3)
    {
        java.lang.Object v0_0 = null;
        try {
            v0_0 = p3.thisObject;
        } catch (Throwable __e_v0) {
            return;
        }
        if ((v0_0 instanceof android.view.View)) {
            com.oupao.wiz.FlyBar.sync(((android.view.View) v0_0));
        }
        return;
    }
}