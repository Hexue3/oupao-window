package com.oupao.wiz;
public class FlyBar$C9 extends com.oupao.wiz.XMH {
    public static int last;
    public FlyBar$C9()
    {
        return;
    }
    public void afterHookedMethod(com.oupao.wiz.XMP p7)
    {
        try {
            if (com.oupao.wiz.Cfg.flyUi()) {
                if (com.oupao.wiz.Cfg.flyUiBar()) {
                    var v0_3 = p7.thisObject;
                    if (v0_3 != null) {
                        int v1_0 = 0;
                        var v2_0 = com.oupao.wiz.Rt.get(v0_3, "mIsMultiIconExpand");
                        if (((v2_0 instanceof Boolean)) && (((Boolean) v2_0).booleanValue())) {
                            v1_0 = 1;
                        }
                        if (com.oupao.wiz.FlyBar$C9.last != v1_0) {
                            com.oupao.wiz.FlyBar$C9.last = v1_0;
                            var v3_4 = com.oupao.wiz.Rt.get(v0_3, "mFloatIconListAnimationLayout");
                            if ((v3_4 instanceof android.view.View)) {
                                var v3_5 = ((android.view.View) v3_4);
                                var v4_1 = v3_5.getParent();
                                if ((v4_1 instanceof android.view.ViewGroup)) {
                                    java.lang.String v5_2 = null;
                                    var v4_2 = ((android.view.ViewGroup) v4_1);
                                    if (v1_0 != 0) {
                                        v3_5.setVisibility(0);
                                        com.oupao.wiz.FlyBar.exitBar(v4_2);
                                        v5_2 = "FlyBar ICON";
                                    } else {
                                        v3_5.setVisibility(4);
                                        com.oupao.wiz.FlyBar.enterBar(v4_2);
                                        v5_2 = "FlyBar BAR";
                                    }
                                    com.oupao.wiz.SLog.i(v5_2);
                                } else {
                                    return;
                                }
                            } else {
                                return;
                            }
                        } else {
                            return;
                        }
                    } else {
                        return;
                    }
                }
                return;
            } else {
                return;
            }
        } catch (Throwable __e_v0_2) {
            com.oupao.wiz.SLog.e("FlyBar PROBE-EX", __e_v0_2);
            return;
        }
    }
}
