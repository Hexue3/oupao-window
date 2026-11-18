package com.oupao.wiz;
public class MA$PB implements android.view.View.OnClickListener {
    private int what;
    public MA$PB(int p1)
    {
        this.what = p1;
        return;
    }
    public void onClick(android.view.View p3)
    {
        int v0 = 0;
        try {
            v0 = this.what;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 == 4) {
            // 切到另一个方向编辑（强制旋转界面）
            com.oupao.wiz.MainActivity.switchPosDir();
            return;
        }
        if (v0 != 0) {
            if (v0 != 1) {
                if (v0 != 2) {
                    com.oupao.wiz.MainActivity.closePosEditor();
                    return;
                } else {
                    com.oupao.wiz.MainActivity.resetPos();
                    return;
                }
            } else {
                com.oupao.wiz.MainActivity.savePos();
                return;
            }
        } else {
            com.oupao.wiz.MainActivity.openPosEditor();
            return;
        }
    }
}