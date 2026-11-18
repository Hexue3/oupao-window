package com.oupao.wiz;
public class Sl$E implements android.view.View.OnClickListener {
    private final android.app.Dialog dlg;
    private final int idx;
    private final android.widget.TextView tv;
    public Sl$E(int p2, android.widget.TextView p3, android.app.Dialog p4)
    {
        this.idx = p2;
        this.tv = p3;
        this.dlg = p4;
        return;
    }
    public void onClick(android.view.View p5)
    {
        java.lang.String v2_0 = null;
        int v0_0 = 0;
        try {
            v0_0 = this.idx;
            com.oupao.wiz.Cfg.saveStr("oupao_fly_close", String.valueOf(v0_0));
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_0 != 0) {
            if (v0_0 != 1) {
                v2_0 = "\u53cc\u51fb\u5173\u95ed";
            } else {
                v2_0 = "\u5355\u51fb\u5173\u95ed";
            }
        } else {
            v2_0 = "\u5173\u95ed\u6b64\u529f\u80fd";
        }
        var v3_0 = this.tv;
        if (v3_0 != null) {
            v3_0.setText(v2_0);
        }
        com.oupao.wiz.Cfg.push();
        var v0_1 = this.dlg;
        if (v0_1 != null) {
            v0_1.dismiss();
        }
        return;
    }
}