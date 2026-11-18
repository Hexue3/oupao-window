package com.oupao.wiz;
public class MA$ME implements android.content.DialogInterface.OnClickListener {
    private final android.app.Dialog d;
    private final android.widget.EditText e;
    public MA$ME(android.widget.EditText p1, android.app.Dialog p2)
    {
        this.e = p1;
        this.d = p2;
        return;
    }
    public void onClick(android.content.DialogInterface p6, int p7)
    {
        java.lang.String v0_3 = null;
        try {
            v0_3 = this.e.getText().toString().trim();
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_3.length() > 0) {
            var v1_1 = com.oupao.wiz.Cfg.getStr("oupao_music_custom", "");
            if (!v1_1.contains(v0_3)) {
                var v2_3 = new StringBuilder();
                v2_3.append(v1_1);
                v2_3.append(v0_3);
                v2_3.append("\n");
                com.oupao.wiz.Cfg.saveStr("oupao_music_custom", v2_3.toString());
            }
            com.oupao.wiz.Cfg.saveStr("oupao_music_pkg", v0_3);
            com.oupao.wiz.Cfg.push();
            var v2_6 = this.d;
            if (v2_6 != null) {
                v2_6.dismiss();
            }
            com.oupao.wiz.MainActivity.reMusic();
        }
        return;
    }
}