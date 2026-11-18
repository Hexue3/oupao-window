package com.oupao.wiz;
public class MA$MQ implements android.view.View.OnLongClickListener {
    private final String p;
    public MA$MQ(String p1)
    {
        this.p = p1;
        return;
    }
    public boolean onLongClick(android.view.View p8)
    {
        java.lang.String v0_0 = null;
        java.lang.String v1_5 = null;
        try {
            v0_0 = this.p;
            v1_5 = com.oupao.wiz.Cfg.getStr("oupao_music_custom", "");
        } catch (Throwable __e_v0) {
            return true;
        }
        if (!com.oupao.wiz.MusTxt.has(v1_5, v0_0)) {
            String v1_1 = com.oupao.wiz.Cfg.getStr("oupao_music_hidden", "");
            if (!com.oupao.wiz.MusTxt.has(v1_1, v0_0)) {
                com.oupao.wiz.Cfg.saveStr("oupao_music_hidden",
                        com.oupao.wiz.MusTxt.add(v1_1, v0_0));
            }
        } else {
            com.oupao.wiz.Cfg.saveStr("oupao_music_custom",
                    com.oupao.wiz.MusTxt.remove(v1_5, v0_0));
            // 同时写进 hidden：否则它会被「内置候选 / 名字含音乐」重新加回来，
            // 表现就是「长按了却删不掉」。
            // （原版 smali 只从 custom 删；但那时没有「所有应用可选」的添加入口，
            //   现在可以手动选任意应用，必须保证长按后真的不再出现。）
            String v1_9 = com.oupao.wiz.Cfg.getStr("oupao_music_custom_hid", "");
            if (!com.oupao.wiz.MusTxt.has(v1_9, v0_0)) {
                com.oupao.wiz.Cfg.saveStr("oupao_music_custom_hid",
                        com.oupao.wiz.MusTxt.add(v1_9, v0_0));
            }
        }
        com.oupao.wiz.Cfg.push();
        com.oupao.wiz.MainActivity.reMusic();
        return true;
    }
}