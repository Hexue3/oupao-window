package com.oupao.wiz;
public class MA$LG implements android.view.View.OnClickListener {
    private final int l;
    public MA$LG(int p1)
    {
        this.l = p1;
        return;
    }
    public void onClick(android.view.View p5)
    {
        int v0_0 = 0;
        int v1 = 0;
        int v2 = 0;
        try {
            v0_0 = this.l;
            v1 = 0;
            v2 = 0;
        } catch (Throwable __e_v0) {
            com.oupao.wiz.MainActivity.buildLog();
            return;
        }
        if (v0_0 != 0) {
            if (v0_0 == 2) {
                v2 = 1;
            }
        } else {
            v1 = 1;
        }
        com.oupao.wiz.Cfg.save("oupao_dbg", v1 != 0);
        com.oupao.wiz.Cfg.save("oupao_log_quiet", v2 != 0);
        com.oupao.wiz.MainActivity.buildLog();
        return;
    }
}