package com.oupao.wiz;
public class PinL implements android.view.View.OnClickListener {
    private String c;
    public PinL(String p1)
    {
        this.c = p1;
        return;
    }
    public void onClick(android.view.View p13)
    {
        java.lang.String v0 = null;
        android.content.Context v1 = null;
        try {
            v0 = this.c;
            v1 = p13.getContext();
        } catch (Throwable __e_v5) {
            com.oupao.wiz.MainActivity.finishNow();
            return;
        }
        if (!v0.startsWith("tool#")) {
            var v6_6 = v0.split("#");
            if (v6_6.length < 2) {
                var v2_0 = com.oupao.wiz.Pins.pkg(v0);
                if (v2_0 != null) {
                    var v3_1 = v1.getPackageManager().getLaunchIntentForPackage(v2_0);
                    if (v3_1 != null) {
                        v3_1.addFlags(268435456);
                        v1.startActivity(v3_1);
                    }
                }
            } else {
                var v8_1 = v6_6[0];
                var v9 = v6_6[1];
                int v10 = 0;
                if (v6_6.length >= 3) {
                    v10 = Integer.parseInt(v6_6[2]);
                }
                v1.sendBroadcast(new android.content.Intent("com.oupao.wiz.CLONE").putExtra("pkg", v8_1).putExtra("cls", v9).putExtra("user", v10));
            }
        } else {
            v1.sendBroadcast(new android.content.Intent("com.oupao.wiz.TOOL").putExtra("id", v0.substring(5)));
        }
        com.oupao.wiz.MainActivity.finishNow();
        return;
    }
}