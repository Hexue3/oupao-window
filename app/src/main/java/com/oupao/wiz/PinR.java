package com.oupao.wiz;
public class PinR implements android.view.View.OnClickListener {
    private String c;
    public PinR(String p1)
    {
        this.c = p1;
        return;
    }
    public void onClick(android.view.View p5)
    {
        java.util.ArrayList v0 = null;
        java.lang.String v1 = null;
        try {
            v0 = com.oupao.wiz.Pins.listDesk();   // 2842：小窗模式自己那套
            v1 = this.c;
        } catch (Throwable __e_v3) {
            return;
        }
        if (v0.contains(v1)) {
            v0.remove(v0.indexOf(v1));
            com.oupao.wiz.Pins.saveDesk(com.oupao.wiz.Pins.join(v0));   // 2842
            com.oupao.wiz.Pins.build();
        }
        return;
    }
}