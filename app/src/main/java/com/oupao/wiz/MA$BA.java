package com.oupao.wiz;
public class MA$BA implements android.window.OnBackAnimationCallback {
    public MA$BA()
    {
        return;
    }
    public void onBackCancelled()
    {
        com.oupao.wiz.MainActivity.animCancel();
        return;
    }
    public void onBackInvoked()
    {
        com.oupao.wiz.MainActivity.animEnd();
        return;
    }
    public void onBackProgressed(android.window.BackEvent p1)
    {
        com.oupao.wiz.MainActivity.animProg(p1);
        return;
    }
    public void onBackStarted(android.window.BackEvent p1)
    {
        com.oupao.wiz.MainActivity.animStart(p1);
        return;
    }
}