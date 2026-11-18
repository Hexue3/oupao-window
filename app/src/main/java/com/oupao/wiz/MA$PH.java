package com.oupao.wiz;
public class MA$PH implements android.view.View.OnClickListener {
    private int pg;
    public MA$PH(int p1)
    {
        this.pg = p1;
        return;
    }
    public void onClick(android.view.View p2)
    {
        try {
            com.oupao.wiz.MainActivity.navTo(this.pg);
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}