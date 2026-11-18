package com.oupao.wiz;
public class SlCb implements android.widget.SeekBar.OnSeekBarChangeListener {
    private Object ex;
    private String k;
    private int mn;
    public android.widget.TextView tv;
    public SlCb(String p1, int p2, Object p3)
    {
        this.k = p1;
        this.mn = p2;
        this.ex = p3;
        return;
    }
    public void onProgressChanged(android.widget.SeekBar p5, int p6, boolean p7)
    {
        android.widget.TextView v0_0 = null;
        try {
            v0_0 = this.tv;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_0 != null) {
            var v6_1 = (p6 + this.mn);
            v0_0.setText(com.oupao.wiz.Sl.text(v6_1, this.mn, this.ex));
            if (this.k.equals("oupao_fly_range")) {
                com.oupao.wiz.FlyArea.setLive(v6_1);
            }
            if (this.k.equals("oupao_fly_scale")) {
                com.oupao.wiz.FlyPrev.setLive(v6_1);
            }
        }
        return;
    }
    public void onStartTrackingTouch(android.widget.SeekBar p4)
    {
        try {
            if (this.k.equals("oupao_fly_scale")) {
                com.oupao.wiz.FlyPrev.setLive((p4.getProgress() + this.mn));
            }
        } catch (Throwable __e_v0) {
            return;
        }
        if (this.k.equals("oupao_fly_range")) {
            com.oupao.wiz.FlyArea.setLive((p4.getProgress() + this.mn));
        }
        return;
    }
    public void onStopTrackingTouch(android.widget.SeekBar p5)
    {
        try {
            var v0_0 = this.k;
            var v1_4 = String.valueOf((this.mn + p5.getProgress()));
            com.oupao.wiz.Cfg.saveStr(v0_0, v1_4);
            com.oupao.wiz.Ev.send(com.oupao.wiz.Ev.it("com.oupao.wiz.CFG").putExtra(v0_0, v1_4));
            com.oupao.wiz.Cfg.push();
        } catch (Throwable __e_v0) {
            return;
        }
        if (this.k.equals("oupao_fly_range")) {
            com.oupao.wiz.FlyArea.clear();
        }
        if (this.k.equals("oupao_fly_scale")) {
            com.oupao.wiz.FlyPrev.clear();
        }
        return;
    }
}