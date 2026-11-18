package com.oupao.wiz;
public class WarmK extends android.content.BroadcastReceiver {
    public WarmK()
    {
        return;
    }
    public void onReceive(android.content.Context p2, android.content.Intent p3)
    {
        try {
            if ((p3 != null) && com.oupao.wiz.SoftReboot.ACTION.equals(p3.getAction())) {
                com.oupao.wiz.SoftReboot.runFromApp();
                return;
            }
            com.oupao.wiz.Warm.go(p2);
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}