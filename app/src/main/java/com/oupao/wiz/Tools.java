package com.oupao.wiz;
public class Tools {
    public static volatile String pend;
    public Tools()
    {
        return;
    }
    private static boolean act(android.content.Context p2, android.content.Intent p3)
    {
        try {
            p3.addFlags(268435456);
            p2.startActivity(p3);
            return true;
        } catch (Throwable __e_v0) {
            return false;
        }
    }
    /**
     * 呼出系统「中转站」（TransferDock）。
     *
     * 中转站不是 Activity，而是侧边栏面板里的一个页面，外面没有 Intent 能打开它，
     * 所以这里只发广播，由侧边栏进程里的 SideDock 去调
     * `UserPanelView.toShowTransferPanelOrDialog(true)`。
     */
    private static boolean dock(android.content.Context p4)
    {
        try {
            var v0 = new android.content.Intent("com.oupao.wiz.DOCK");
            v0.addFlags(16777216);          // FLAG_RECEIVER_FOREGROUND
            p4.sendBroadcast(v0);
            com.oupao.wiz.SLog.i("Tools: 已请求呼出中转站");
            return true;
        } catch (Throwable __e_dock) {
            com.oupao.wiz.SLog.e("Tools: 呼出中转站失败", __e_dock);
            return false;
        }
    }
    private static String aliasOf(String p4)
    {
        try {
            if (!p4.startsWith("tool#")) {
                return p4;
            } else {
                return p4.substring(5);
            }
        } catch (Throwable __e_v3) {
            return p4;
        }
    }
    private static ClassLoader classLoader()
    {
        return com.oupao.wiz.Tools.class.getClassLoader();
    }
    public static String[] defs()
    {
        var v0 = com.oupao.wiz.Tools.names();
        var v1 = com.oupao.wiz.Tools.tags();
        var v2 = com.oupao.wiz.Tools.icons();
        var v3 = v0.length;
        String[] v4 = new String[v3];
        int v5 = 0;
        while (v5 < v3) {
            var v6_1 = new StringBuilder();
            v6_1.append(v0[v5]);
            v6_1.append("\u0001");
            v6_1.append(v1[v5]);
            v6_1.append("\u0001");
            v6_1.append(v2[v5]);
            v4[v5] = v6_1.toString();
            v5++;
        }
        return v4;
    }
    private static boolean dir(android.content.Context p5, String p6, boolean p7)
    {
        android.content.Intent v0_1 = null;
        try {
            v0_1 = new android.content.Intent("oplus.intent.action.DIRECT_SIDEBAR_SERVICE");
            v0_1.setPackage("com.coloros.colordirectservice");
            v0_1.putExtra("extra_entrance_function", p6);
        } catch (Throwable __e_v0) {
            return false;
        }
        if (!p7) {
            v0_1.putExtra("from_package", "com.coloros.smartsidebar");
        } else {
            v0_1.putExtra("triggered_app", "com.coloros.smartsidebar");
            v0_1.putExtra("first_point_down_x", ((float) 0));
            v0_1.putExtra("first_point_down_y", ((float) 0));
        }
        p5.startForegroundService(v0_1);
        return true;
    }
    private static final java.util.HashMap<String, android.graphics.drawable.Drawable> DCACHE
            = new java.util.HashMap<String, android.graphics.drawable.Drawable>();

    /**
     * 2834：加缓存。
     *
     * 原来每次调用都要 getResourcesForApplication(com.coloros.smartsidebar) + 若干次
     * getIdentifier + getDrawable（跨包资源查找 + inflate），实测索引条**每 6 秒**
     * 的空闲刷新都会把这 11 个工具图标重新建一遍（日志里 icon tool#... -> …@新对象
     * 一波接一波），既费 CPU 又刷日志。图标是静态资源，缓存起来即可。
     */
    public static android.graphics.drawable.Drawable drawable(android.content.Context p11, String p12)
    {
        if ((p11 == null) || (p12 == null)) {
            return null;
        }
        synchronized (DCACHE) {
            var hit = DCACHE.get(p12);
            if (hit != null) {
                return hit;
            }
        }
        var d = drawableRaw(p11, p12);
        if (d != null) {
            synchronized (DCACHE) {
                if (DCACHE.size() > 64) {
                    DCACHE.clear();
                }
                DCACHE.put(p12, d);
            }
        }
        return d;
    }

    public static android.graphics.drawable.Drawable drawableRaw(android.content.Context p11, String p12){
        android.graphics.drawable.Drawable v0_0 = null;
        try {
            if (p11 != null) {
                var v1 = com.oupao.wiz.Tools.aliasOf(p12);
                if (v1 != null) {
                    var v2_5 = com.oupao.wiz.Tools.resNames(v1);
                    if (v2_5 != null) {
                        var v3_4 = p11.getPackageManager();
                        if (v3_4 != null) {
                            var v3_0 = v3_4.getResourcesForApplication("com.coloros.smartsidebar");
                            if (v3_0 != null) {
                                var v4_1 = v2_5.length;
                                int v5 = 0;
                                while (v5 < v4_1) {
                                    var v7_1 = v3_0.getIdentifier(v2_5[v5], "drawable", "com.coloros.smartsidebar");
                                    if (v7_1 > 0) {
                                        v0_0 = v3_0.getDrawable(v7_1);
                                        if (v0_0 != null) {
                                            break;
                                        }
                                    }
                                    v5++;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable __e_v9) {
            var v2_1 = new StringBuilder();
            v2_1.append("icon ERR ");
            v2_1.append(__e_v9);
            com.oupao.wiz.SLog.i(v2_1.toString());
            return null;
        }
        var v2_4 = new StringBuilder();
        v2_4.append("icon ");
        v2_4.append(p12);
        v2_4.append(" -> ");
        v2_4.append(v0_0);
        com.oupao.wiz.SLog.i(v2_4.toString());
        return v0_0;
    }
    /**
     * 截图 / 录屏 / 识屏 / 全屏翻译这类「要抓屏」的工具。
     *
     * ★ 为什么要单独拎出来：这些工具由系统后台服务**立刻截图**，
     * 而扇形菜单的窗口不是当场摘掉的 —— `FlyH.hide()` 里是
     * `postDelayed(new BarOut(...), 150)`，150ms 后才 removeView。
     * 结果就是：从扇形菜单点「识屏 / 截图」，系统截图时菜单还在屏幕上，
     * 识屏把菜单里的图标和文字一起识别进去了（用户反馈的问题）。
     * 所以这类工具要**等窗口真的消失**再触发（见 [fire]）。
     */
    private static boolean captureTool(String p1)
    {
        return "screen_shot".equals(p1)
                || "screen_shot_area".equals(p1)
                || "screen_record".equals(p1)
                || "screen_record_area".equals(p1)
                || "breeno_screen_identify".equals(p1)
                || "global_translation".equals(p1)
                || "translation".equals(p1);
    }
    public static boolean fire(android.content.Context p4, String p5)
    {
        if ((p4 == null) || (p5 == null)) {
            return false;
        }
        var v2_2 = new StringBuilder();
        v2_2.append("fire ");
        v2_2.append(p5);
        com.oupao.wiz.SLog.i(v2_2.toString());
        if (captureTool(p5)) {
            // 等扇形菜单的窗口真正离开屏幕（FlyH.hide 是 150ms 后 removeView，
            // 这里留 220ms 余量），否则识屏/截图会把菜单一起抓进去。
            final android.content.Context v0_1 = p4;
            final String v1_1 = p5;
            var v2_3 = new java.lang.Runnable() {
                public void run() {
                    com.oupao.wiz.SLog.i("fire 延后触发（等菜单窗口消失）" + v1_1);
                    com.oupao.wiz.Tools.fireNow(v0_1, v1_1);
                }
            };
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(v2_3, 220L);
            return true;
        }
        return com.oupao.wiz.Tools.fireNow(p4, p5);
    }
    /**
     * 3221：这些工具交回**侧边栏自己**启动（ROM 的 `PageRoutUtils.startSysTool(EntryBean)`），
     * 我们不再各自实现一遍 —— 面板里也就不会缺项。
     */
    private static final String[] VIA_SIDEBAR = {
            "recent_files", "clean_storage", "wechat_scan", "wechat_paycode",
            "alipay_scan", "alipay_paycode", "music_mss_panel", "omoji_effect", "read_article"
    };

    private static boolean runViaSidebar(android.content.Context c, String id)
    {
        for (String a : VIA_SIDEBAR) {
            if (!a.equals(id)) {
                continue;
            }
            try {
                c.sendBroadcast(new android.content.Intent("com.oupao.wiz.TOOLRUN")
                        .putExtra("alias", a).setPackage("com.coloros.smartsidebar"));
                com.oupao.wiz.SLog.i("Tools: 转交侧边栏启动 " + a);
                return true;
            } catch (Throwable t) {
                com.oupao.wiz.SLog.e("Tools: 转交侧边栏失败 " + a, t);
                return false;
            }
        }
        return false;
    }

    private static boolean fireNow(android.content.Context p4, String p5)
    {
        if ((p4 != null) && (p5 != null)) {
            if (com.oupao.wiz.Tools.runViaSidebar(p4, p5)) {
                return true;
            }
            if ("transfer_dock".equals(p5)) {
                return com.oupao.wiz.Tools.dock(p4);
            }
            if (!"screen_shot".equals(p5)) {
                if (!"screen_shot_area".equals(p5)) {
                    if (!"screen_record".equals(p5)) {
                        if (!"screen_record_area".equals(p5)) {
                            if (!"sound_record".equals(p5)) {
                                if (!"voice_to_text".equals(p5)) {
                                    if (!"global_translation".equals(p5)) {
                                        if (!"translation".equals(p5)) {
                                            if (!"breeno_screen_identify".equals(p5)) {
                                                if (!"recognize_bg_music".equals(p5)) {
                                                    if ("quick_notes".equals(p5)) {
                                                        return com.oupao.wiz.Tools.memo(p4);
                                                    }
                                                } else {
                                                    return com.oupao.wiz.Tools.dir(p4, "recognize_bgm", false);
                                                }
                                            } else {
                                                return com.oupao.wiz.Tools.dir(p4, "full_screen_ocr", true);
                                            }
                                        } else {
                                            return com.oupao.wiz.Tools.tra(p4);
                                        }
                                    } else {
                                        return com.oupao.wiz.Tools.trans(p4);
                                    }
                                } else {
                                    return com.oupao.wiz.Tools.svc(p4, "com.coloros.accessibilityassistant", "action.oplus.accessibilityassistant.subtitle.video");
                                }
                            } else {
                                return com.oupao.wiz.Tools.snd(p4);
                            }
                        } else {
                            return com.oupao.wiz.Tools.rec(p4, true);
                        }
                    } else {
                        return com.oupao.wiz.Tools.rec(p4, false);
                    }
                } else {
                    return com.oupao.wiz.Tools.shot(p4, "AreaScreenshot_smartsidebar");
                }
            } else {
                return com.oupao.wiz.Tools.shot(p4, "EdgePanel");
            }
        }
        return false;
    }
    public static void fireFrom(android.content.Intent p3)
    {
        java.lang.String v0_2 = null;
        try {
            v0_2 = p3.getStringExtra("id");
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.SLog.e("Fly: \u5de5\u5177\u8bf7\u6c42\u5f02\u5e38", __e_v0_1);
            return;
        }
        if (v0_2 != null) {
            com.oupao.wiz.Tools.pend = v0_2;
            new Thread(new com.oupao.wiz.ToolR()).start();
        }
        return;
    }
    public static void firePend()
    {
        java.lang.String v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Tools.pend;
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.SLog.e("Fly: \u5de5\u5177\u6267\u884c\u5931\u8d25", __e_v0_1);
            return;
        }
        if (v0_0 != null) {
            var v1_1 = com.oupao.wiz.Cfg.sysContext();
            if (v1_1 != null) {
                var v2 = com.oupao.wiz.Tools.fire(v1_1, v0_0);
                var v3_2 = new StringBuilder();
                v3_2.append("Fly: \u5de5\u5177 ");
                v3_2.append(v0_0);
                v3_2.append(" ret=");
                v3_2.append(v2);
                com.oupao.wiz.SLog.i(v3_2.toString());
            }
        }
        return;
    }
    private static boolean have(android.content.Context p3, String p4)
    {
        int v0_0 = 0;
        try {
            if (p3.getPackageManager().getApplicationInfo(p4, 0) != null) {
                v0_0 = 1;
            }
        } catch (Throwable __e_v1) {
            return false;
        }
        return v0_0 != 0;
    }
    public static String[] icons()
    {
        String[] v0_1 = new String[21];
        v0_1[0] = "\u2702";
        v0_1[1] = "\u25a3";
        v0_1[2] = "\u23fa";
        v0_1[3] = "\u25a3";
        v0_1[4] = "\ud83c\udf99";
        v0_1[5] = "\ud83c\udf99";
        v0_1[6] = "\u6587A";
        v0_1[7] = "\u6587A";
        v0_1[8] = "\ud83d\udc41";
        v0_1[9] = "\ud83d\udc41";
        v0_1[10] = "\ud83d\udcdd";
        v0_1[11] = "\ud83d\uddc2";
        // 3221：兜底字形（真图标从侧边栏资源里取，见 resNames）
        v0_1[12] = "\ud83d\udcc1";                       // 最近文件
        v0_1[13] = "\ud83e\uddf9";                       // 清理存储
        v0_1[14] = "\ud83d\udcac";                       // 微信扫一扫
        v0_1[15] = "\ud83d\udcb0";                       // 微信支付
        v0_1[16] = "\ud83d\udd0d";                       // 支付宝扫一扫
        v0_1[17] = "\ud83d\udcb3";                       // 支付宝付款
        v0_1[18] = "\ud83c\udfb5";                       // 声音分轨
        v0_1[19] = "\ud83d\ude00";                       // Omoji 特效
        v0_1[20] = "\ud83c\udfa7";                       // AI 妙听
        return v0_1;
    }
    /** 工具 tag 在 tags() 里的下标，找不到返回 -1（原来 icon/label 各写一遍同样的遍历） */
    private static int idxOf(String tag)
    {
        try {
            java.lang.String[] v0 = com.oupao.wiz.Tools.tags();
            for (int i = 0; i < v0.length; i++) {
                if (v0[i].equals(tag)) {
                    return i;
                }
            }
        } catch (Throwable __e_v4) {
            return -1;
        }
        return -1;
    }
    public static String icon(String p5)
    {
        int i = com.oupao.wiz.Tools.idxOf(p5);
        if (i < 0) {
            return "\u2022";
        }
        return com.oupao.wiz.Tools.icons()[i];
    }
    public static String label(String p5)
    {
        int i = com.oupao.wiz.Tools.idxOf(p5);
        if (i < 0) {
            return p5;
        }
        return com.oupao.wiz.Tools.names()[i];
    }
    private static ClassLoader loader(android.content.Context p2)
    {
        java.lang.ClassLoader v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Hooks.CL;
        } catch (Throwable __e_v1) {
            v0_0 = null;
        }
        if (v0_0 == null) {
            try {
                if (p2 != null) {
                    v0_0 = p2.getClassLoader();
                }
            } catch (Throwable __e_v1_) {
                v0_0 = null;
            }
            if (v0_0 == null) {
                return com.oupao.wiz.Tools.classLoader();
            } else {
                return v0_0;
            }
        } else {
            return v0_0;
        }
    }
    private static boolean memo(android.content.Context p5)
    {
        try {
            var v0_1 = com.oupao.wiz.Tools.pick(p5, "com.coloros.note", "com.oneplus.note");
            var v1_2 = new android.content.Intent("action.nearme.note.quicknote");
            v1_2.setPackage(v0_1);
            v1_2.putExtra("path", "com.coloros.smartsidebar");
            v1_2.putExtra("caller", "com.coloros.smartsidebar");
            v1_2.putExtra("touchToolType", false);
            return com.oupao.wiz.Tools.act(p5, v1_2);
        } catch (Throwable __e_v0) {
            return false;
        }
    }
    public static String[] names()
    {
        String[] v0_1 = new String[21];
        v0_1[0] = "\u622a\u5c4f";
        v0_1[1] = "\u533a\u57df\u622a\u5c4f";
        v0_1[2] = "\u5c4f\u5e55\u5f55\u5236";
        v0_1[3] = "\u533a\u57df\u5f55\u5236";
        v0_1[4] = "\u5f55\u97f3";
        v0_1[5] = "\u8bed\u97f3\u8f6c\u6587";
        v0_1[6] = "\u5168\u5c40\u7ffb\u8bd1";
        v0_1[7] = "\u7ffb\u8bd1";
        v0_1[8] = "\u8bc6\u5c4f";
        v0_1[9] = "\u542c\u66f2\u8bc6\u522b";
        v0_1[10] = "\u5feb\u901f\u5907\u5fd8";
        v0_1[11] = "\u4e2d\u8f6c\u7ad9";
        // 3221：补齐侧边栏「全部」页里的工具（镜像侧边栏自己的清单）
        v0_1[12] = "\u6700\u8fd1\u6587\u4ef6";                 // 最近文件
        v0_1[13] = "\u6e05\u7406\u5b58\u50a8";                 // 清理存储
        v0_1[14] = "\u5fae\u4fe1\u626b\u4e00\u626b";           // 微信扫一扫
        v0_1[15] = "\u5fae\u4fe1\u652f\u4ed8";                 // 微信支付
        v0_1[16] = "\u652f\u4ed8\u5b9d\u626b\u4e00\u626b";     // 支付宝扫一扫
        v0_1[17] = "\u652f\u4ed8\u5b9d\u4ed8\u6b3e";           // 支付宝付款
        v0_1[18] = "\u58f0\u97f3\u5206\u8f68";                 // 声音分轨
        v0_1[19] = "Omoji \u7279\u6548";                        // Omoji 特效
        v0_1[20] = "AI \u5999\u542c";                           // AI 妙听
        return v0_1;
    }
    private static String pick(android.content.Context p2, String p3, String p4)
    {
        if (!com.oupao.wiz.Tools.have(p2, p3)) {
            return p4;
        } else {
            return p3;
        }
    }
    private static boolean rec(android.content.Context p6, boolean p7)
    {
        android.content.Intent v1_2 = null;
        try {
            var v0_1 = com.oupao.wiz.Tools.pick(p6, "com.oplus.screenrecorder", "com.coloros.screenrecorder");
            v1_2 = new android.content.Intent();
            var v3_4 = new StringBuilder();
            v3_4.append(v0_1);
            v3_4.append(".MainActivity");
            v1_2.setComponent(new android.content.ComponentName(v0_1, v3_4.toString()));
            v1_2.putExtra("recorder_entrance", "SystemUIScreenAssistant");
        } catch (Throwable __e_v0) {
            return false;
        }
        if (p7) {
            v1_2.putExtra("use_area_record", true);
        }
        return com.oupao.wiz.Tools.act(p6, v1_2);
    }
    private static String[] resNames(String p5)
    {
        try {
            // 3219：中转站的图标。侧边栏资源里确实有（aapt2 dump 出来的名字）：
            //   ic_transfer_dock_icon（主）、ic_file_dock（旧版）
            // 以前这里没有 transfer_dock 分支 → 图标恒为 null，扇形菜单里就是空图标。
            if ("transfer_dock".equals(p5)) {
                String[] v0 = new String[2];
                v0[0] = "ic_transfer_dock_icon";
                v0[1] = "ic_file_dock";
                return v0;
            }
            // 3221：补齐的 9 个工具——图标名来自侧边栏 EntryBean.getIconRes() 的
            //   getResourceName() 实测结果（见 SideDock 的 TOOL 日志）
            if ("recent_files".equals(p5)) {
                return new String[]{"ic_recent_file"};
            }
            if ("clean_storage".equals(p5)) {
                return new String[]{"coloros_ep_tool_clean_storage"};
            }
            if ("wechat_scan".equals(p5)) {
                return new String[]{"coloros_ep_tool_wechat_scan"};
            }
            if ("wechat_paycode".equals(p5)) {
                return new String[]{"coloros_ep_tool_wechat_paycode"};
            }
            if ("alipay_scan".equals(p5)) {
                return new String[]{"coloros_ep_tool_alipay_scan"};
            }
            if ("alipay_paycode".equals(p5)) {
                return new String[]{"coloros_ep_tool_alipay_paycode"};
            }
            if ("music_mss_panel".equals(p5)) {
                return new String[]{"coloros_ep_tool_music_msspanel"};
            }
            if ("omoji_effect".equals(p5)) {
                return new String[]{"coloros_ep_tool_omoji"};
            }
            if ("read_article".equals(p5)) {
                return new String[]{"coloros_ep_tool_read_article_new"};
            }
            if (!"screen_shot".equals(p5)) {
                if (!"screen_shot_area".equals(p5)) {
                    if (!"screen_record".equals(p5)) {
                        if (!"screen_record_area".equals(p5)) {
                            if (!"sound_record".equals(p5)) {
                                if (!"voice_to_text".equals(p5)) {
                                    if (!"global_translation".equals(p5)) {
                                        if (!"translation".equals(p5)) {
                                            if (!"breeno_screen_identify".equals(p5)) {
                                                if (!"recognize_bg_music".equals(p5)) {
                                                    if (!"quick_notes".equals(p5)) {
                                                        return null;
                                                    } else {
                                                        String[] v4_31 = new String[2];
                                                        v4_31[0] = "coloros_ep_tool_quick_notes_os13";
                                                        v4_31[1] = "coloros_ep_tool_quick_notes";
                                                        return v4_31;
                                                    }
                                                } else {
                                                    String[] v4_33 = new String[1];
                                                    v4_33[0] = "coloros_ep_tool_recognize_bgm";
                                                    return v4_33;
                                                }
                                            } else {
                                                String[] v4_36 = new String[1];
                                                v4_36[0] = "coloros_ep_tool_breeno_screen_identify_new";
                                                return v4_36;
                                            }
                                        } else {
                                            String[] v4_38 = new String[1];
                                            v4_38[0] = "coloros_ep_tool_translate";
                                            return v4_38;
                                        }
                                    } else {
                                        String[] v4_40 = new String[3];
                                        v4_40[0] = "coloros_ep_tool_global_translation";
                                        v4_40[1] = "coloros_ep_tool_box_selected_translation";
                                        v4_40[2] = "coloros_ep_tool_screen_translation";
                                        return v4_40;
                                    }
                                } else {
                                    String[] v4_43 = new String[3];
                                    v4_43[0] = "coloros_ep_tool_smart_voice";
                                    v4_43[1] = "coloros_ep_tool_voice_to_text";
                                    v4_43[2] = "coloros_ep_tool_subtitle_transfer";
                                    return v4_43;
                                }
                            } else {
                                String[] v4_45 = new String[1];
                                v4_45[0] = "coloros_ep_tool_sound_record";
                                return v4_45;
                            }
                        } else {
                            String[] v4_3 = new String[1];
                            v4_3[0] = "coloros_ep_tool_screen_record_area";
                            return v4_3;
                        }
                    } else {
                        String[] v4_5 = new String[1];
                        v4_5[0] = "coloros_ep_tool_screen_record";
                        return v4_5;
                    }
                } else {
                    String[] v4_7 = new String[1];
                    v4_7[0] = "coloros_ep_tool_screen_shot_area";
                    return v4_7;
                }
            } else {
                String[] v4_10 = new String[1];
                v4_10[0] = "coloros_ep_tool_screen_shot";
                return v4_10;
            }
        } catch (Throwable __e_v3) {
            return null;
        }
    }
    public static android.graphics.Bitmap round(android.graphics.drawable.Drawable p8, int p9)
    {
        android.graphics.Bitmap v0_0 = null;
        try {
            if ((p8 != null) && (p9 > 0)) {
                var v1 = com.oupao.wiz.Sug.drawableToBitmap(p8, p9);
                if (v1 != null) {
                    v0_0 = android.graphics.Bitmap.createBitmap(p9, p9, android.graphics.Bitmap.Config.ARGB_8888);
                    if (v0_0 != null) {
                        var v2_1 = new android.graphics.Canvas(v0_0);
                        var v3_1 = new android.graphics.Paint(1);
                        v3_1.setShader(new android.graphics.BitmapShader(v1, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP));
                        v2_1.drawCircle((((float) p9) * 0.5f), (((float) p9) * 0.5f), (((float) p9) * 0.5f), v3_1);
                    }
                }
            }
        } catch (Throwable __e_v7) {
            return null;
        }
        return v0_0;
    }
    private static boolean shot(android.content.Context p10, String p11)
    {
        java.lang.Class v3_0 = null;
        try {
            v3_0 = com.oupao.wiz.Rt.cls(com.oupao.wiz.Tools.loader(p10), "com.oplus.screenshot.OplusLongshotUtils");
        } catch (Throwable __e_v1) {
            return false;
        }
        if (v3_0 == null) {
            v3_0 = com.oupao.wiz.Rt.cls(com.oupao.wiz.Tools.ui(), "com.oplus.screenshot.OplusLongshotUtils");
        }
        if (v3_0 != null) {
            var v6_0 = v3_0;
            Class[] v3_2 = new Class[1];
            v3_2[0] = android.content.Context.class;
            var v3_3 = com.oupao.wiz.Rt.method(v6_0, "getScreenshotManager", ((java.lang.Class[]) v3_2));
            if (v3_3 != null) {
                Object[] v4_3 = new Object[1];
                v4_3[0] = p10;
                try {
                    var v3_4 = com.oupao.wiz.Rt.invoke(((java.lang.reflect.Method) v3_3), null, v4_3);
                    if (v3_4 != null) {
                        var v4_4 = v3_4.getClass();
                        Class[] v5_4 = new Class[1];
                        v5_4[0] = android.os.Bundle.class;
                        var v4_5 = com.oupao.wiz.Rt.method(v4_4, "takeScreenshot", v5_4);
                        if (v4_5 != null) {
                            var v5_6 = new android.os.Bundle();
                            v5_6.putString("screenshot_relation_id", java.util.UUID.randomUUID().toString());
                            v5_6.putLong("screenshot_start_time", android.os.SystemClock.uptimeMillis());
                            v5_6.putString("screenshot_source", p11);
                            v5_6.putBoolean("statusbar_visible", false);
                            v5_6.putBoolean("navigationbar_visible", false);
                            v5_6.putBoolean("global_action_visible", false);
                            v5_6.putBoolean("screenshot_orientation", false);
                            Object[] v6_8 = new Object[1];
                            v6_8[0] = v5_6;
                            try {
                                com.oupao.wiz.Rt.invoke(v4_5, v3_4, v6_8);
                            } catch (Throwable __e_io) {
                            }
                            return true;
                        }
                    }
                } catch (Throwable __e_io) {
                }
            }
        }
        return com.oupao.wiz.Tools.dir(p10, p11, true);
    }
    private static boolean snd(android.content.Context p5)
    {
        java.lang.String v0_1 = null;
        android.content.Intent v1_2 = null;
        try {
            v0_1 = com.oupao.wiz.Tools.pick(p5, "com.coloros.soundrecorder", "com.oneplus.soundrecorder");
            v1_2 = new android.content.Intent("com.oplus.soundrecorder.open_quick_recording");
            v1_2.setPackage(v0_1);
        } catch (Throwable __e_v0) {
            return false;
        }
        if (p5.getPackageManager().resolveActivity(v1_2, 0) == null) {
            v1_2 = new android.content.Intent();
            v1_2.setComponent(new android.content.ComponentName(v0_1, "com.coloros.soundrecorder.TransparentActivity"));
        }
        return com.oupao.wiz.Tools.act(p5, v1_2);
    }
    /**
     * 翻译应用包名，**顺序沿用系统侧边栏**
     * （`com.oplus.smartsidebar…models.tools.GlobalTranslationTool.checkAvailablePackage()`）：
     * <ol>
     *   <li>{@code com.oplus.translate}（metaData {@code support_screen_translation} = 1）</li>
     *   <li>{@code com.coloros.translate}（同上）</li>
     *   <li>{@code com.coloros.ocrscanner}（支持 {@code supportGlobalTranslation} /
     *       {@code supportScreenTranslate} —— 新版「屏幕翻译」在它里面）</li>
     * </ol>
     * 以前这里硬编码 {@code com.coloros.translate}：在只有 com.oplus.translate 或只有
     * ocrscanner 的机器上 `startForegroundService` 直接抛异常 → 从扇形菜单点「全局翻译」
     * 一点反应都没有（用户反馈的「翻译不在后台就呼不出」）。
     */
    private static String transPkg(android.content.Context p3)
    {
        String[] v0 = new String[]{"com.oplus.translate", "com.coloros.translate", "com.coloros.ocrscanner"};
        for (int i = 0; i < v0.length; i++) {
            if (com.oupao.wiz.Tools.have(p3, v0[i])) {
                return v0[i];
            }
        }
        return null;
    }
    /**
     * 全局翻译。
     *
     * ★ 关键结论（反编译侧边栏确认）：`GlobalTranslationTool.handle()` 就是
     * `Intent("oplus.intent.action.GLOBAL_TRANSLATION") + setPackage(翻译包) +
     * putExtra("extra_from_package","com.coloros.smartsidebar") + startForegroundService`，
     * 跟我们自己发的**一模一样**。差别只在**调用方身份**：
     * 翻译应用按调用方决定浮窗/全屏，冷启动（进程不在后台）时也按调用方放行。
     * 从 system_server 发 → 全屏 / 冷启动没反应；从侧边栏进程发 → 和侧边栏完全一致。
     * 所以这里改成：**先请侧边栏代劳**，1.5s 后翻译应用还没起来才自己发（兜底）。
     */
    private static boolean trans(android.content.Context p3)
    {
        var v0 = com.oupao.wiz.Tools.transPkg(p3);
        if (v0 == null) {
            com.oupao.wiz.SLog.i("Tools: \u6ca1\u6709\u53ef\u7528\u7684\u7ffb\u8bd1\u5e94\u7528");
            return false;
        }
        // ⚠️ 回归说明（22:21 用户反馈）：2632 那套「先拉前台再发服务 + 一堆 if 判断」
        //   反而让「全局翻译」彻底呼不出了 —— 因为 2630 起加的 screenSvcRunning()
        //   判断把「服务在跑」当成「已经成功」，可服务常年常驻（日志：
        //   `Tools: 翻译进程服务=[com.coloros.translate.screen.ScreenTranslationService]`）
        //   → 每次都跳过发送，等于什么都没干。
        // 现在回到**最朴素的打法**：只放开 appop + 侧边栏代劳 + 直接发两次意图，
        // 不做任何「已成功」判断（2632 之前「至少有时能呼出」就是这套）。
        com.oupao.wiz.Tools.allowFgs(p3, v0);
        final android.content.Context fc = p3;
        final String fp = v0;
        var h = new android.os.Handler(android.os.Looper.getMainLooper());
        com.oupao.wiz.Tools.viaSidebar(fc, "global_translation");
        h.postDelayed(new java.lang.Runnable() {
            public void run() {
                com.oupao.wiz.SLog.i("Tools: \u76f4\u63a5\u53d1\u7ffb\u8bd1 service (1) pkg=" + fp);
                com.oupao.wiz.Tools.fireService(fc, fp, true);
            }
        }, 250L);
        h.postDelayed(new java.lang.Runnable() {
            public void run() {
                com.oupao.wiz.SLog.i("Tools: \u518d\u53d1\u7ffb\u8bd1 service (2) pkg=" + fp);
                com.oupao.wiz.Tools.fireService(fc, fp, true);
            }
        }, 1100L);
        return true;
    }
    /** 诊断：把这个包里正在跑的服务名打出来（用来确认「屏幕翻译」到底起没起） */
    private static void logSvcNames(android.content.Context p3, String p4)
    {
        try {
            var am = (android.app.ActivityManager) p3.getSystemService("activity");
            if (am == null) {
                return;
            }
            var list = am.getRunningServices(400);
            var sb = new StringBuilder();
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    var si = (android.app.ActivityManager.RunningServiceInfo) list.get(i);
                    if ((si != null) && (si.service != null)
                            && p4.equals(si.service.getPackageName())) {
                        if (sb.length() > 0) {
                            sb.append(',');
                        }
                        sb.append(si.service.getClassName());
                    }
                }
            }
            com.oupao.wiz.SLog.i("Tools: \u7ffb\u8bd1\u8fdb\u7a0b\u670d\u52a1=[" + sb + "]");
        } catch (Throwable ignore) {
        }
    }
    private static void tryFireService(android.content.Context p3, String p4, int p5, boolean fg)
    {
        if (com.oupao.wiz.Tools.screenSvcRunning(p3, p4)) {
            com.oupao.wiz.SLog.i("Tools: \u5c4f\u5e55\u7ffb\u8bd1\u670d\u52a1\u5df2\u5728\u8dd1\uff0c\u4e0d\u518d\u53d1 attempt=" + p5);
            return;
        }
        if (fg) {
            com.oupao.wiz.Tools.allowFgs(p3, p4);
        }
        com.oupao.wiz.SLog.i("Tools: \u53d1\u5c4f\u5e55\u7ffb\u8bd1 service attempt=" + p5
                + " fgs=" + fg + " pkg=" + p4);
        com.oupao.wiz.Tools.fireService(p3, p4, fg);
    }
    /**
     * 放开翻译应用的「前台服务启动 / 悬浮窗」两个 appop。
     *
     * `OP_START_FOREGROUND`（`android:start_foreground`）就是 ColorOS
     * `mAllowStartForeground` 判 false 时那道闸门的开关；`SYSTEM_ALERT_WINDOW`
     * 是 Android 12+ 允许后台起前台服务的豁免之一（翻译浮窗本来也要用它）。
     * 两个都设成 MODE_ALLOWED，失败就算了（有的版本签名是 int op）。
     */
    private static void allowFgs(android.content.Context p3, String p4)
    {
        try {
            var pm = p3.getPackageManager();
            int uid = pm.getApplicationInfo(p4, 0).uid;
            var ops = (android.app.AppOpsManager) p3.getSystemService("appops");
            if (ops == null) {
                return;
            }
            String[] names = new String[]{"android:start_foreground", "android:system_alert_window"};
            java.lang.reflect.Method m = null;
            try {
                m = android.app.AppOpsManager.class.getMethod("setMode", String.class, int.class, String.class, int.class);
            } catch (Throwable ignore) {
            }
            int ok = 0;
            for (int i = 0; i < names.length; i++) {
                try {
                    if (m != null) {
                        m.invoke(ops, names[i], uid, p4, 0);
                        ok++;
                    }
                } catch (Throwable ignore) {
                }
            }
            com.oupao.wiz.SLog.i("Tools: appop \u653e\u5bbd " + p4 + " uid=" + uid + " ok=" + ok);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Tools: appop \u653e\u5bbd\u5931\u8d25", t);
        }
    }
    /**
     * 翻译应用的**「屏幕翻译」服务**是否在跑（用来判断上一步到底成没成）。
     *
     * ⚠️ 必须按服务名过滤：2629 写成「该包有任何服务在跑就算起来了」，
     * 而翻译应用平时就常驻一些服务 → 永远判 true → 一次都没真正发出去
     * （用户反馈：2629 之后点全局翻译完全没反应，日志里全是
     * 「屏幕翻译服务已在跑，不再发 attempt=…」）。
     */
    private static boolean screenSvcRunning(android.content.Context p3, String p4)
    {
        try {
            var am = (android.app.ActivityManager) p3.getSystemService("activity");
            if (am != null) {
                var list = am.getRunningServices(400);
                if (list != null) {
                    for (int i = 0; i < list.size(); i++) {
                        var si = (android.app.ActivityManager.RunningServiceInfo) list.get(i);
                        if ((si == null) || (si.service == null)) {
                            continue;
                        }
                        if (!p4.equals(si.service.getPackageName())) {
                            continue;
                        }
                        String cn = si.service.getClassName();
                        // ⚠️ 只认「屏幕翻译」那个服务：翻译应用还有别的常驻服务（名字里
                        // 也带 translation），2629/2630 就是被它们骗了 → 永远判「已在跑」。
                        if (cn != null) {
                            String lc = cn.toLowerCase().replace(".", "");
                            if (lc.contains("screentranslation")) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        return false;
    }
    /** 请侧边栏进程代劳：广播 `com.oupao.wiz.TRANS`（SideDock 在侧边栏进程里收） */
    private static boolean viaSidebar(android.content.Context p3, String p4)
    {
        try {
            var v0 = new android.content.Intent("com.oupao.wiz.TRANS");
            v0.setPackage("com.coloros.smartsidebar");
            v0.putExtra("id", p4);
            p3.sendBroadcast(v0);
            com.oupao.wiz.SLog.i("Tools: \u8bf7\u4fa7\u8fb9\u680f\u4ee3\u52b3 " + p4);
            return true;
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Tools: \u53d1\u4fa7\u8fb9\u680f\u5e7f\u64ad\u5931\u8d25", t);
            return false;
        }
    }
    /** 目标应用进程是否在跑（system_server 能拿到全量进程列表） */
    private static boolean procAlive(android.content.Context p3, String p4)
    {
        try {
            var v0 = (android.app.ActivityManager) p3.getSystemService("activity");
            if (v0 == null) {
                return false;
            }
            var v1 = v0.getRunningAppProcesses();
            if (v1 != null) {
                for (int i = 0; i < v1.size(); i++) {
                    var v2 = (android.app.ActivityManager.RunningAppProcessInfo) v1.get(i);
                    if ((v2 != null) && (p4.equals(v2.processName))) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        return false;
    }
    /**
     * 发「全局翻译」的意图给翻译应用。
     *
     * fg=true → startForegroundService（ColorOS 的闸门经常会拒：
     *   `startForegroundService() not allowed due to mAllowStartForeground false`）；
     * fg=false → 普通 startService：**system_server 自己不受后台起服务限制**，
     *   服务起来后由它自己 startForeground，绕开那道闸门。
     */
    private static void fireService(android.content.Context p3, String p4, boolean fg)
    {
        try {
            var v0 = new android.content.Intent("oplus.intent.action.GLOBAL_TRANSLATION");
            v0.setPackage(p4);
            v0.putExtra("extra_from_package", "com.coloros.smartsidebar");
            if (fg) {
                p3.startForegroundService(v0);
            } else {
                p3.startService(v0);
            }
            com.oupao.wiz.SLog.i("Tools: \u5df2\u53d1\u7ffb\u8bd1 service fgs=" + fg + " pkg=" + p4);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Tools: \u7ffb\u8bd1 service \u53d1\u9001\u5931\u8d25 fgs=" + fg, t);
        }
    }
    private static boolean svc(android.content.Context p3, String p4, String p5)
    {
        android.content.Intent v0_1 = null;
        try {
            v0_1 = new android.content.Intent(p5);
            v0_1.setPackage(p4);
            p3.startForegroundService(v0_1);
            return true;
        } catch (Throwable __e_v0) {
            return false;
        }
    }
    public static String[] tags()
    {
        String[] v0_1 = new String[21];
        v0_1[0] = "tool#screen_shot";
        v0_1[1] = "tool#screen_shot_area";
        v0_1[2] = "tool#screen_record";
        v0_1[3] = "tool#screen_record_area";
        v0_1[4] = "tool#sound_record";
        v0_1[5] = "tool#voice_to_text";
        v0_1[6] = "tool#global_translation";
        v0_1[7] = "tool#translation";
        v0_1[8] = "tool#breeno_screen_identify";
        v0_1[9] = "tool#recognize_bg_music";
        v0_1[10] = "tool#quick_notes";
        v0_1[11] = "tool#transfer_dock";
        // 3221：补齐的工具（启动时转交侧边栏 PageRoutUtils.startSysTool）
        v0_1[12] = "tool#recent_files";
        v0_1[13] = "tool#clean_storage";
        v0_1[14] = "tool#wechat_scan";
        v0_1[15] = "tool#wechat_paycode";
        v0_1[16] = "tool#alipay_scan";
        v0_1[17] = "tool#alipay_paycode";
        v0_1[18] = "tool#music_mss_panel";
        v0_1[19] = "tool#omoji_effect";
        v0_1[20] = "tool#read_article";
        return v0_1;
    }
    /**
     * 「翻译」：按用户要求 —— **不走 translate 自己的工具入口，和别的 app 走同一条路**。
     *
     * 也就是直接 `FlyH.launch("pkg#启动Activity")`：这条路径是扇形菜单点普通 app 走的
     * （`FlyH.go()` → ColorOS 小窗 → 不支持就全屏兜底），行为和别的 app 完全一致。
     *
     * 之前走 `coloros.intent.action.TRANSLATION.COMPASS` 或
     * `OplusZoomWindowManager.startZoomWindow` 都不行：日志里
     * `FlexibleTaskController: isSupportFlexibleWindow … support = false`
     * —— translate 的 MainActivity 是 nonResizable，小窗被系统直接拒、静默退全屏。
     */
    private static boolean tra(android.content.Context p5)
    {
        var v0_1 = com.oupao.wiz.Tools.transPkg(p5);
        if (v0_1 == null) {
            com.oupao.wiz.SLog.i("Tools: \u6ca1\u6709\u53ef\u7528\u7684\u7ffb\u8bd1\u5e94\u7528");
            return false;
        }
        com.oupao.wiz.Tools.forceResizable(p5);
        // ① 首选：COMPASS 意图 + ColorOS 小窗。
        //    以前这条会被 FlexibleTaskController 拒（MainActivity 是 nonResizable，
        //    support=false → 静默全屏）；2634 起装了 FlexFix 钩子，对翻译应用强制
        //    support=true，所以小窗应该真的能开了。
        if (com.oupao.wiz.Tools.fireActZoom(p5, v0_1)) {
            com.oupao.wiz.SLog.i("Tools: \u7ffb\u8bd1\u5df2\u7528\u5c0f\u7a97\u6253\u5f00(COMPASS) pkg=" + v0_1);
            return true;
        }
        // ② 退回「和别的 app 走同一条路」（桌面入口；实测这条应用会自己 finish，聊胜于无）
        String cls = com.oupao.wiz.Tools.launchCls(p5, v0_1);
        if (cls != null) {
            String tag = v0_1 + "#" + cls;
            com.oupao.wiz.SLog.i("Tools: \u7ffb\u8bd1\u6309\u666e\u901a app \u542f\u52a8 tag=" + tag);
            com.oupao.wiz.FlyH.launch(tag);
            return true;
        }
        com.oupao.wiz.SLog.i("Tools: \u7ffb\u8bd1\u6ca1\u6709\u684c\u9762\u5165\u53e3\uff0c\u76f4\u63a5\u5f00 Activity");
        com.oupao.wiz.Tools.fireAct(p5, v0_1);
        return true;
    }
    /**
     * 最近应用兜底：侧边栏那份抠不出来时，用**系统最近任务**凑一份。
     *
     * 侧边栏 `RecentAppSource.mCacheRecentList` 的元素类型各版本不同，
     * 2326 版就出现「有 2 个元素但抠不出包名」的情况（日志 `n=0 size=2`）。
     * 系统最近任务在 system_server 里能直接拿到，够用且稳定。
     */
    public static void fallbackRecents(android.content.Context p3)
    {
        try {
            var am = (android.app.ActivityManager) p3.getSystemService("activity");
            if (am == null) {
                return;
            }
            var tasks = am.getRecentTasks(40, android.app.ActivityManager.RECENT_IGNORE_UNAVAILABLE);
            if (tasks == null) {
                return;
            }
            var pm = p3.getPackageManager();
            var seen = new java.util.HashSet<String>();
            var sb = new StringBuilder();
            for (int i = 0; (i < tasks.size()) && (seen.size() < 12); i++) {
                var ti = (android.app.ActivityManager.RecentTaskInfo) tasks.get(i);
                if ((ti == null) || (ti.baseIntent == null)) {
                    continue;
                }
                var cn = ti.baseIntent.getComponent();
                if (cn == null) {
                    continue;
                }
                String pkg = cn.getPackageName();
                if (pkg == null) {
                    continue;
                }
                if (pkg.equals("android") || pkg.equals("com.android.systemui")
                        || pkg.equals("com.oupao.wiz") || pkg.contains("launcher")) {
                    continue;
                }
                if (!seen.add(pkg)) {
                    continue;
                }
                String label = null;
                try {
                    label = String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
                } catch (Throwable ignore) {
                }
                if ((label == null) || (label.length() == 0)) {
                    continue;
                }
                sb.append(label).append('\u0001').append(pkg).append('\n');
            }
            if (sb.length() > 0) {
                String s = sb.toString();
                com.oupao.wiz.Cfg.saveStr("recents", s);
                // ★ 系统侧文件一定要写：MorePanel / IdxBar 都在 system_server，
                //   `Cfg.getStrFresh` 取「两份文件里较新的那份」；APP_DIR 那份由
                //   system_server 写不一定成功（属主/权限），而 SYS_DIR 肯定能写。
                //   2639 就因为只写了 APP_DIR（还写了 Settings.Global 给 App 用），
                //   结果**小窗版更新、面板/引索条不更新**。
                try {
                    com.oupao.wiz.Cfg.writeStr("/data/system/oupao_cfg/recents", s);
                } catch (Throwable t2) {
                    // 2639 那次「小窗版更新、面板/引索条不更新」就是因为这条静默失败
                    com.oupao.wiz.SLog.e("Tools: 写 /data/system/oupao_cfg/recents 失败(引索条/面板会拿到旧数据)", t2);
                }
                try {
                    com.oupao.wiz.Cfg.gset("recents", s);
                } catch (Throwable t3) {
                    com.oupao.wiz.SLog.e("Tools: 写 Settings.Global recents 失败(App 进程会拿到旧数据)", t3);
                }
                com.oupao.wiz.SLog.i("Tools: \u6700\u8fd1\u5e94\u7528\u56de\u9000\u7cfb\u7edf\u6700\u8fd1\u4efb\u52a1 n=" + seen.size());
            } else {
                com.oupao.wiz.SLog.i("Tools: \u7cfb\u7edf\u6700\u8fd1\u4efb\u52a1\u4e5f\u662f\u7a7a\u7684");
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Tools: \u7cfb\u7edf\u6700\u8fd1\u4efb\u52a1\u56de\u9000\u5931\u8d25", t);
        }
    }

    private static long recAt;

    /** 事件驱动的「有人要看最近应用」标记：由界面（引索条 show / 面板）置位，
     *  后台 heavyTick 看到就立刻刷一次，不必再靠固定 5s 轮询。 */
    private static volatile boolean recWanted;

    /** 界面上马上要显示「最近应用」了 → 让下一次后台 tick 立刻刷新（不阻塞主/手势线程）。 */
    public static void wantRecents() {
        recWanted = true;
    }

    /**
     * 刷新「最近应用」。
     *
     * ★ 2639 及以前只问侧边栏 → **数据不会自动更新**：侧边栏那份
     * `RecentAppSource.mCacheRecentList` 只有它自己被打开/刷新时才更新，
     * 所以用户必须手动开一次侧边栏，我们才拿到新列表。
     *
     * 现在改成：**先用系统最近任务**（`fallbackRecents`，实时、按最近使用排序），
     * 再顺手问一下侧边栏（它可能更全，例如带小程序），但侧边栏的数据只在
     * 我们手上没有数据时才会被采用（见 `Ev` 的 RECENTS 分支）。
     *
     * 节流 5s（原来 20s，用户觉得更新慢）；`force=true` 时不受节流限制（面板打开时用）。
     */
    public static void refreshRecents(android.content.Context p3) {
        refreshRecents(p3, false);
    }

    public static void refreshRecents(android.content.Context p3, boolean force)
    {
        // 2975：灭屏/锁屏期间不刷新最近应用
        if (com.oupao.wiz.Cfg.sleepingNow()) {
            return;
        }
        try {
            long now = android.os.SystemClock.elapsedRealtime();
            // ★ 2918 之后：节流 5s（原来 20s，用户觉得更新慢）改回「事件驱动 + 30s 兜底」。
            //   以前后台每 5s 就跑一次 getRecentTasks + 一条广播，纯粹为了「反正会有人看」。
            //   现在界面（IdxBar.show）会调 wantRecents() 置位，下一次后台 tick 立刻刷；
            //   没人要的时候 30s 才刷一次，只是保底。
            long gap = recWanted ? 0L : 30000L;
            if (!force && (recAt != 0L) && ((now - recAt) < gap)) {
                return;
            }
            recWanted = false;
            recAt = now;
            // ① 系统最近任务：每次都重新算，天然是最新的
            com.oupao.wiz.Tools.fallbackRecents(p3);
            // ② 顺便问侧边栏（只在我们没数据时采用它的，见 Ev）
            var v0 = new android.content.Intent("com.oupao.wiz.RECENTS_REQ");
            v0.setPackage("com.coloros.smartsidebar");
            p3.sendBroadcast(v0);
            com.oupao.wiz.SLog.i("Tools: 已刷新最近应用 force=" + force + " gap=" + gap);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Tools: \u5237\u65b0\u6700\u8fd1\u5e94\u7528\u5931\u8d25", t);
        }
    }

    /** 取某个包的桌面启动 Activity（ACTION_MAIN + CATEGORY_LAUNCHER），取不到返回 null */
    private static String launchCls(android.content.Context p3, String p4)
    {
        try {
            var pm = p3.getPackageManager();
            var it = new android.content.Intent(android.content.Intent.ACTION_MAIN);
            it.addCategory(android.content.Intent.CATEGORY_LAUNCHER);
            it.setPackage(p4);
            var list = pm.queryIntentActivities(it, 0);
            if ((list != null) && (list.size() > 0)) {
                var ri = (android.content.pm.ResolveInfo) list.get(0);
                if ((ri != null) && (ri.activityInfo != null)) {
                    return ri.activityInfo.name;
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }
    /**
     * 打开「强制可调整大小」（开发者选项里那个开关）。
     *
     * 日志实锤（22:12）：
     * ```
     * FlexibleTaskController: isSupportFlexibleWindow target = com.coloros.translate/.ui.MainActivity support = false
     * FlexibleTaskController: current activity unsupport flexible window
     * Task{… A=10343:com.coloros.translate nonResizable}
     * ```
     * translate 的 `MainActivity` 是 **nonResizable**，ColorOS 的小窗管理器直接拒绝、
     * 静默退回全屏（所以 zoomOpen 返回成功但看着还是全屏）。
     * 打开这个全局开关后，non-resizable 的 Activity 也能进小窗/自由窗口。
     */
    private static void forceResizable(android.content.Context p3)
    {
        try {
            android.provider.Settings.Global.putInt(p3.getContentResolver(),
                    "force_resizable_activities", 1);
            com.oupao.wiz.SLog.i("Tools: \u5df2\u5f00 force_resizable_activities");
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Tools: \u5f00 force_resizable_activities \u5931\u8d25", t);
        }
    }
    /** 以 **ColorOS 小窗** 打开翻译（COMPASS → com.coloros.translate/.ui.MainActivity） */
    private static boolean fireActZoom(android.content.Context p5, String p4)
    {
        try {
            com.oupao.wiz.Tools.forceResizable(p5);
            var v0 = new android.content.Intent("coloros.intent.action.TRANSLATION.COMPASS");
            v0.setPackage(p4);
            v0.putExtra("extra_from_package", "com.coloros.smartsidebar");
            v0.addFlags(0x10000000);            // FLAG_ACTIVITY_NEW_TASK
            if (p5.getPackageManager().resolveActivity(v0, 0) == null) {
                com.oupao.wiz.SLog.i("Tools: \u7ffb\u8bd1 Activity \u4e0d\u5b58\u5728 pkg=" + p4);
                return false;
            }
            return com.oupao.wiz.FlyH.zoomOpen(v0, 0);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Tools: \u7ffb\u8bd1\u5c0f\u7a97\u5f02\u5e38", t);
            return false;
        }
    }
    private static void fireAct(android.content.Context p5, String p4)
    {
        try {
            var v0 = new android.content.Intent("coloros.intent.action.TRANSLATION.COMPASS");
            v0.setPackage(p4);
            v0.putExtra("extra_from_package", "com.coloros.smartsidebar");
            v0.addFlags(335544320);
            p5.startActivity(v0);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Tools: \u7ffb\u8bd1 Activity \u5931\u8d25", t);
        }
    }
    private static ClassLoader ui()
    {
        try {
            return new dalvik.system.PathClassLoader("/system_ext/priv-app/SystemUI/SystemUI.apk", ClassLoader.getSystemClassLoader());
        } catch (Throwable __e_v1) {
            return null;
        }
    }
}