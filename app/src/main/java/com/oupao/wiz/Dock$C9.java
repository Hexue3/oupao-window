package com.oupao.wiz;
public class Dock$C9 extends com.oupao.wiz.XMH {
    public Dock$C9()
    {
        return;
    }
    public void beforeHookedMethod(com.oupao.wiz.XMP p5)
    {
        try {
            if (com.oupao.wiz.Dock.keepOn()) {
                var v0_4 = p5.thisObject;
                if ((v0_4 != null) && ((!Boolean.TRUE.equals(com.oupao.wiz.Rt.get(v0_4, "finishing"))) && (com.oupao.wiz.Dock.isDocked(com.oupao.wiz.Win.actTask(v0_4))))) {
                    p5.setResult(null);
                    com.oupao.wiz.SLog.i(new StringBuilder("DockKeep \u8d34\u8fb9/\u606f\u5c4f\u6321\u4e0b\u505c\u6b62: ").append(com.oupao.wiz.Dock.info()).toString());
                }
            }
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.SLog.e("Dock: sleepStop \u5f02\u5e38", __e_v0_1);
        }
        return;
    }
}
