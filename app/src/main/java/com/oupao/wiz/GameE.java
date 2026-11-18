package com.oupao.wiz;
public class GameE implements android.view.View.OnClickListener {
    public GameE()
    {
        return;
    }
    public void onClick(android.view.View p2)
    {
        try {
            com.oupao.wiz.MainActivity.navTo(10);
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}