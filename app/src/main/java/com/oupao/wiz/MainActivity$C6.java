package com.oupao.wiz;
public class MainActivity$C6 implements android.view.View.OnClickListener {
    public MainActivity$C6()
    {
        return;
    }
    public void onClick(android.view.View p5)
    {
        try {
            if (!com.oupao.wiz.MainActivity.diagRx) {
                p5.getContext().registerReceiver(new com.oupao.wiz.Diag$Recv(), new android.content.IntentFilter("com.oupao.wiz.DIAG"));
                com.oupao.wiz.MainActivity.diagRx = true;
            }
        } catch (Throwable __e___ex) {
            return;
        }
        com.oupao.wiz.Diag.showOverlay(p5);
        return;
    }
}