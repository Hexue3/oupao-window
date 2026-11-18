package com.oupao.wiz;
public class MA$MR implements android.view.View.OnClickListener, android.view.View.OnLongClickListener {
    private final android.app.Dialog d;
    public MA$MR(android.app.Dialog p1)
    {
        this.d = p1;
        return;
    }
    private static void run(com.oupao.wiz.MA$MR p3)
    {
        // 必须两侧一起清（只删 App 侧会被 /data/system/oupao_cfg 的旧值盖住）
        // 恢复内置应用：只清「内置隐藏」列表。
        // 但 oupao_music_hidden 里可能混着「手动添加后被移除」的条目（历史数据），
        // 它们要迁到 oupao_music_custom_hid，否则会跟着一起被放回来。
        try {
            String hid = com.oupao.wiz.Cfg.getStr("oupao_music_hidden", "");
            String cust = com.oupao.wiz.Cfg.getStr("oupao_music_custom", "");
            if ((hid != null) && (hid.length() > 0) && (cust != null) && (cust.length() > 0)) {
                StringBuilder keep = new StringBuilder();
                StringBuilder move = new StringBuilder();
                String[] lines = hid.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    String t = lines[i].trim();
                    if (t.length() <= 0) {
                        continue;
                    }
                    if (com.oupao.wiz.MusTxt.has(cust, t)) {
                        // 曾经是手动添加的 → 迁到 custom_hid，继续保持「已移除」
                        if (!com.oupao.wiz.MusTxt.has(move.toString(), t)) {
                            move.append(t).append("\n");
                        }
                    } else {
                        keep.append(t).append("\n");
                    }
                }
                if (move.length() > 0) {
                    String oldch = com.oupao.wiz.Cfg.getStr("oupao_music_custom_hid", "");
                    com.oupao.wiz.Cfg.saveStr("oupao_music_custom_hid", (oldch == null ? "" : oldch) + move.toString());
                }
                // 其余（被隐藏的内置应用）**清掉 = 恢复**，
                // 不能把 keep 再写回 hidden，否则内置应用还是隐藏着的（2105 的 bug）
                com.oupao.wiz.Cfg.clearKey("oupao_music_hidden");
            } else {
                com.oupao.wiz.Cfg.clearKey("oupao_music_hidden");
            }
        } catch (Throwable ignore) {
            com.oupao.wiz.Cfg.clearKey("oupao_music_hidden");
        }
        var v0_2 = p3.d;
        if (v0_2 != null) {
            v0_2.dismiss();
        }
        com.oupao.wiz.MainActivity.reMusic();
        return;
    }
    public void onClick(android.view.View p4)
    {
        try {
            com.oupao.wiz.MA$MR.run(this);
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public boolean onLongClick(android.view.View p4)
    {
        try {
            com.oupao.wiz.MA$MR.run(this);
        } catch (Throwable __e_v0) {
        }
        return true;
    }
}