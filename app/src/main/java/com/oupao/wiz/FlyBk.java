package com.oupao.wiz;
public class FlyBk implements android.window.OnBackInvokedCallback {
    public FlyBk()
    {
        return;
    }
    public void onBackInvoked()
    {
        try {
            com.oupao.wiz.FlyH.close();
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}