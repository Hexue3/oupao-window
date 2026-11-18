package com.oupao.wiz;
public class CloneK extends android.content.BroadcastReceiver {
    public CloneK()
    {
        return;
    }
    public static void handle(android.content.Intent p7)
    {
        java.lang.String v0_1 = null;
        java.lang.String v1_2 = null;
        int v2_1 = 0;
        try {
            v0_1 = p7.getStringExtra("pkg");
            v1_2 = p7.getStringExtra("cls");
            v2_1 = p7.getIntExtra("user", 0);
            var v3_1 = new StringBuilder();
            v3_1.append("Fly: \u5206\u8eab\u8bf7\u6c42 ");
            v3_1.append(v0_1);
            v3_1.append(" cls=");
            v3_1.append(v1_2);
            v3_1.append(" u=");
            v3_1.append(v2_1);
            com.oupao.wiz.SLog.i(v3_1.toString());
        } catch (Throwable __e_v0_2) {
            com.oupao.wiz.SLog.e("Fly: \u5206\u8eab\u8bf7\u6c42\u5931\u8d25", __e_v0_2);
            return;
        }
        if ((v0_1 != null) && ((v1_2 != null) && (v2_1 >= 0))) {
            com.oupao.wiz.FlyH.cloneLaunch(v0_1, v1_2, v2_1);
        }
        return;
    }
    public void onReceive(android.content.Context p1, android.content.Intent p2)
    {
        try {
            com.oupao.wiz.CloneK.handle(p2);
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}