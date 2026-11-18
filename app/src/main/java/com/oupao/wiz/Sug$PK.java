package com.oupao.wiz;
public class Sug$PK extends android.content.BroadcastReceiver {
    public Sug$PK()
    {
        return;
    }
    public void onReceive(android.content.Context p4, android.content.Intent p5)
    {
        try {
            com.oupao.wiz.SLog.i("Inst: \u5e7f\u64ad\u5230\u8fbe");
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.SLog.e("Inst: onReceive \u5f02\u5e38", __e_v0_1);
            return;
        }
        if (p5 == null) {
            com.oupao.wiz.SLog.i("Inst: intent null");
        } else {
            com.oupao.wiz.SLog.i("Inst: data=".concat(p5.getDataString()));
        }
        var v0_2 = com.oupao.wiz.Cfg.sugInstall();
        var v1_2 = new StringBuilder();
        v1_2.append("Inst: sw=");
        v1_2.append(v0_2);
        com.oupao.wiz.SLog.i(v1_2.toString());
        com.oupao.wiz.Sug.onPkg(p5);
        return;
    }
}