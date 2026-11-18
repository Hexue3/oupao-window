package com.oupao.wiz;
public class PinT implements android.view.View.OnClickListener {
    private String c;
    public PinT(String p1)
    {
        this.c = p1;
        return;
    }
    public void onClick(android.view.View p2)
    {
        try {
            com.oupao.wiz.More.fireTool(this.c);
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}