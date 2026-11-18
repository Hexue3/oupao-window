package com.oupao.wiz;
public class PinW implements android.view.View.OnClickListener {
    private String c;
    public PinW(String p1)
    {
        this.c = p1;
        return;
    }
    public void onClick(android.view.View p6)
    {
        java.lang.String v4 = null;
        java.lang.String v0_2 = null;
        java.lang.String v1_2 = null;
        try {
            v0_2 = com.oupao.wiz.Cfg.deskPins();   // 2842：小窗模式自己那套（不再共用面板数据）
            v1_2 = com.oupao.wiz.Pins.withAdd(v0_2, this.c);
        } catch (Throwable __e_v0) {
            return;
        }
        if (v1_2.equals(v0_2)) {
            v4 = "\u6247\u5f62\u83dc\u5355\u6700\u591a\u663e\u793a 5 \u4e2a\uff08\u6216\u5df2\u5b58\u5728\uff09";
        } else {
            com.oupao.wiz.Pins.saveDesk(v1_2);   // 2842
            v4 = "\u5df2\u52a0\u5165\u56fa\u5b9a\u5217\u8868";
        }
        android.widget.Toast.makeText(p6.getContext(), v4, 0).show();
        // 原来这里一律 navTo(9)（跳回固定应用页）。
        // 在小窗模式的「更多应用」页上点 + 时，页面不该跳走，
        // 否则看起来就像“点了没反应”。留在本页重建即可。
        com.oupao.wiz.Pins.build();
        if (!com.oupao.wiz.MainActivity.deskOn) {
            com.oupao.wiz.MainActivity.navTo(9);
        }
        return;
    }
}