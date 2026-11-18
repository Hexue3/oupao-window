package com.oupao.wiz;
public class FlyBar$C3 extends com.oupao.wiz.XMH {
    public FlyBar$C3()
    {
        return;
    }
    public void afterHookedMethod(com.oupao.wiz.XMP p5)
    {
        try {
            com.oupao.wiz.SLog.i("FlyBar REFRESH");
        } catch (Throwable __e_v0_3) {
            com.oupao.wiz.SLog.e("REFRESH-EX", __e_v0_3);
            return;
        }
        if (com.oupao.wiz.Cfg.flyUi()) {
            if (com.oupao.wiz.Cfg.flyUiBar()) {
                var v0_6 = p5.thisObject;
                if (v0_6 != null) {
                    var v0_1 = com.oupao.wiz.Rt.get(v0_6, "mFloatIconListAnimationLayout");
                    if ((v0_1 instanceof android.view.View)) {
                        var v0_2 = ((android.view.View) v0_1);
                        v0_2.setVisibility(4);
                        if ((v0_2.getParent() instanceof android.view.ViewGroup)) {
                            com.oupao.wiz.FlyBar.sync(v0_2);
                        }
                    }
                }
            }
            return;
        } else {
            return;
        }
    }
}
