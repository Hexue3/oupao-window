package com.oupao.wiz;
public class MA$P implements android.view.View.OnClickListener {
    private final String p;
    public MA$P(String p1)
    {
        this.p = p1;
        return;
    }
    public void onClick(android.view.View p3)
    {
        try {
            com.oupao.wiz.Cfg.saveStr("oupao_music_pkg", this.p);
            com.oupao.wiz.Cfg.push();
            // 不再跳回上一页（原版是 backTo(2)）：选中后就地刷新音乐页，
            // 被选中的应用会被标记为「当前」
            com.oupao.wiz.MainActivity.reMusic();
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}