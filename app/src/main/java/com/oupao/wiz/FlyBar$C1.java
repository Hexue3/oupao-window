package com.oupao.wiz;
public class FlyBar$C1 extends com.oupao.wiz.XMH {
    public FlyBar$C1()
    {
        return;
    }
    public void afterHookedMethod(com.oupao.wiz.XMP p5)
    {
        try {
            if (com.oupao.wiz.Cfg.flyUi()) {
                if (com.oupao.wiz.Cfg.flyUiBar()) {
                    var v0_3 = p5.thisObject;
                    if ((v0_3 instanceof android.view.View)) {
                        var v0_4 = ((android.view.View) v0_3);
                        v0_4.setVisibility(4);
                        if ((v0_4.getParent() instanceof android.view.ViewGroup)) {
                            com.oupao.wiz.FlyBar.sync(v0_4);
                            com.oupao.wiz.SLog.i("STACK-AFTER");
                        }
                    }
                }
                return;
            } else {
                return;
            }
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.SLog.e("STACK-EX", __e_v0_1);
            return;
        }
    }
}