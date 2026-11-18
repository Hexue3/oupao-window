package com.oupao.wiz;
public class Mus {
    public Mus()
    {
        return;
    }
    public static void addCustom(android.content.pm.PackageManager p9, java.util.ArrayList p10, java.util.ArrayList p11)
    {
        java.lang.String v0_2 = null;
        try {
            v0_2 = com.oupao.wiz.Cfg.getStr("oupao_music_custom", "");
        } catch (Throwable __e_v0) {
            return;
        }
        if ((v0_2 != null) && (v0_2.length() > 0)) {
            var v0_1 = v0_2.split("\n");
            var v1_1 = v0_1.length;
            int v2 = 0;
            while (v2 < v1_1) {
                var v3_1 = v0_1[v2].trim();
                if ((v3_1.length() > 0) && ((!com.oupao.wiz.Mus.isHid(p9, v3_1)) && ((!p10.contains(v3_1)) && (p9.getLaunchIntentForPackage(v3_1) != null)))) {
                    p10.add(v3_1);
                    p11.add(com.oupao.wiz.Mus.label(p9, v3_1));
                }
                v2++;
            }
        }
        return;
    }
    public static String[] cand()
    {
        String[] v0_1 = new String[15];
        v0_1[0] = "com.kugou.android";
        v0_1[1] = "com.netease.cloudmusic";
        v0_1[2] = "com.tencent.qqmusic";
        v0_1[3] = "cn.kuwo.player";
        v0_1[4] = "com.spotify.music";
        v0_1[5] = "com.google.android.apps.youtube.music";
        v0_1[6] = "com.ximalaya.ting.android";
        v0_1[7] = "com.tencent.karaoke";
        v0_1[8] = "com.heytap.music";
        v0_1[9] = "com.oppo.music";
        v0_1[10] = "com.miui.player";
        v0_1[11] = "com.android.music";
        v0_1[12] = "com.soundcloud.android";
        v0_1[13] = "com.apple.android.music";
        v0_1[14] = "com.ting.mp3.android";
        return v0_1;
    }
    public static boolean isHid(android.content.pm.PackageManager p3, String p4)
    {
        java.lang.String v0_3 = null;
        java.lang.String v0_4 = null;
        try {
            v0_3 = com.oupao.wiz.Cfg.getStr("oupao_music_hidden", "");
            v0_4 = com.oupao.wiz.Cfg.getStr("oupao_music_custom_hid", "");
        } catch (Throwable __e_v0) {
            return false;
        }
        // 两个键都算隐藏：内置隐藏 + 手动添加后被移除的
        if (com.oupao.wiz.MusTxt.has(v0_3, p4)) {
            return true;
        }
        if (com.oupao.wiz.MusTxt.has(v0_4, p4)) {
            return true;
        }
        return false;
    }
    public static boolean isMusic(String p2)
    {
        if (!p2.contains("\u97f3\u4e50")) {
            if (!p2.contains("Music")) {
                if (!p2.contains("music")) {
                    if (!p2.contains("\u968f\u8eab\u542c")) {
                        return false;
                    } else {
                        return true;
                    }
                } else {
                    return true;
                }
            } else {
                return true;
            }
        } else {
            return true;
        }
    }
    public static String label(android.content.pm.PackageManager p3, String p4)
    {
        android.content.pm.ApplicationInfo v0_0 = null;
        try {
            v0_0 = p3.getApplicationInfo(p4, 0);
        } catch (Throwable __e_v0) {
            return p4;
        }
        if (v0_0 == null) {
            return p4;
        } else {
            return String.valueOf(p3.getApplicationLabel(v0_0));
        }
    }
    public static String pick()
    {
        java.lang.String v0_5 = null;
        try {
            v0_5 = com.oupao.wiz.Cfg.getStr("oupao_music_pkg", "");
        } catch (Throwable __e_v0) {
            return null;
        }
        if ((v0_5 != null) && (v0_5.length() > 0)) {
            return v0_5;
        }
        var v0_1 = com.oupao.wiz.Cfg.getStr("oupao_music_list", "");
        if (v0_1 != null) {
            var v1_2 = v0_1.indexOf("|");
            if (v1_2 > 0) {
                return v0_1.substring(0, v1_2);
            }
        }
        // 兜底：还没在音乐页点选过任何应用时，用扫到的第一个可用音乐应用。
        // （原来返回 null，使得音乐气泡建议根本不会出现）
        try {
            var ctx = com.oupao.wiz.Sug.ctx;
            if (ctx == null) {
                ctx = com.oupao.wiz.Cfg.sysContext();
            }
            if (ctx != null) {
                var pkgs = new java.util.ArrayList<String>();
                var labels = new java.util.ArrayList<String>();
                var pm = ctx.getPackageManager();
                scanCand(pm, pkgs, labels);
                scanLabel(pm, pkgs, labels);
                for (int i = 0; i < pkgs.size(); i++) {
                    var pk = String.valueOf(pkgs.get(i));
                    if ((pk != null) && (pk.length() > 0) && (pm.getLaunchIntentForPackage(pk) != null)) {
                        return pk;
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }
    public static void scanCand(android.content.pm.PackageManager p8, java.util.ArrayList p9, java.util.ArrayList p10)
    {
        java.lang.String[] v0 = null;
        int v1 = 0;
        int v2 = 0;
        try {
            com.oupao.wiz.Mus.addCustom(p8, p9, p10);
            v0 = com.oupao.wiz.Mus.cand();
            v1 = v0.length;
            v2 = 0;
        } catch (Throwable __e_v0) {
            return;
        }
        while (v2 < v1) {
            var v3 = v0[v2];
            if ((!com.oupao.wiz.Mus.isHid(p8, v3)) && ((!p9.contains(v3)) && (p8.getLaunchIntentForPackage(v3) != null))) {
                p9.add(v3);
                p10.add(com.oupao.wiz.Mus.label(p8, v3));
            }
            v2++;
        }
        return;
    }
    public static void scanLabel(android.content.pm.PackageManager p9, java.util.ArrayList p10, java.util.ArrayList p11)
    {
        java.util.List v0_1 = null;
        try {
            v0_1 = p9.getInstalledApplications(0);
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_1 != null) {
            var v1 = v0_1.size();
            int v2 = 0;
            while (v2 < v1) {
                var v3_1 = ((android.content.pm.ApplicationInfo) v0_1.get(v2));
                if (v3_1 != null) {
                    var v4 = v3_1.packageName;
                    if ((v4 != null) && ((!p10.contains(v4)) && (!com.oupao.wiz.Mus.isHid(p9, v4)))) {
                        try {
                            var v6_1 = String.valueOf(p9.getApplicationLabel(v3_1));
                            if (com.oupao.wiz.Mus.isMusic(v6_1)) {
                                p10.add(v4);
                                p11.add(v6_1);
                            }
                        } catch (Throwable __e_io) {
                        }
                    }
                }
                v2++;
            }
        }
        return;
    }
}