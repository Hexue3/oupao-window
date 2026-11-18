package com.oupao.wiz;
public class FlyUIE implements android.view.View.OnClickListener {
    public FlyUIE()
    {
        return;
    }
    public void onClick(android.view.View p2)
    {
        try {
            com.oupao.wiz.MainActivity.navTo(11);
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}