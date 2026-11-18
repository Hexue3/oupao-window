package com.oupao.wiz;
public class Diag$Close implements android.view.View.OnClickListener {
    private android.view.View overlay;
    public Diag$Close(android.view.View p1)
    {
        this.overlay = p1;
        return;
    }
    public void onClick(android.view.View p4)
    {
        try {
            var v0 = this.overlay;
            ((android.view.ViewGroup) v0.getParent()).removeView(v0);
        } catch (Throwable __e___ex) {
        }
        return;
    }
}