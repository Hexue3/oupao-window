package com.oupao.wiz;
public class MA$PG implements android.view.View.OnClickListener {
    private int from;
    public MA$PG(int p1)
    {
        this.from = p1;
        return;
    }
    public void onClick(android.view.View p2)
    {
        try {
            if (!com.oupao.wiz.MainActivity.deskOn) {
                com.oupao.wiz.MainActivity.navToSug(this.from);
                return;
            } else {
                com.oupao.wiz.MainActivity.finishNow();
                return;
            }
        } catch (Throwable __e_v0) {
            return;
        }
    }
}