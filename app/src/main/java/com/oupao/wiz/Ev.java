package com.oupao.wiz;
public class Ev {
    static int cCfg;
    static int cClone;
    static int cClose;
    static int cDiag;
    static int cTool;
    static int cUrl;
    private Ev()
    {
        return;
    }
    public static String[] actions()
    {
        String[] v0_1 = new String[13];
        v0_1[0] = "com.oupao.wiz.CFG";
        v0_1[1] = "com.oupao.wiz.URL";
        v0_1[2] = "com.oupao.wiz.DIAG_REQ";
        v0_1[3] = "com.oupao.wiz.CLOSE";
        v0_1[4] = "com.oupao.wiz.CLONE";
        v0_1[5] = "com.oupao.wiz.TOOL";
        v0_1[6] = "com.oupao.wiz.PIN";
        v0_1[7] = "com.oupao.wiz.MODE";
        v0_1[8] = "com.oupao.wiz.SHOWMORE";
        v0_1[9] = "com.oupao.wiz.RECENTS";
        v0_1[10] = "com.oupao.wiz.SOFT_REBOOT";
        v0_1[11] = "com.oupao.wiz.ZOOM";
        v0_1[12] = "com.oupao.wiz.MINI";
        return v0_1;
    }
    public static void dispatch(String p2, android.content.Intent p3)
    {
        if (p2 != null) {
            // verbose：CFG 广播在用户每次切开关时都来一条，且 Cfg.apply 末尾已有
            // 汇总日志（"Cfg: 收到广播 ..."），这条属于重复信息。
            com.oupao.wiz.SLog.v(new StringBuilder().append("Ev: ").append(p2).toString());
            if ("com.oupao.wiz.RECENTS".equals(p2)) {
                // 侧边栏进程把「最近应用」回给我们（每行 label\u0001pkg）。
                // 存进配置，MorePanel / IdxBar（都在 system_server）直接读。
                try {
                    String data = p3.getStringExtra("data");
                    if (data == null) {
                        data = "";
                    }
                    // ★ 只有我们手上还没有数据时才采用侧边栏那份：
                    //   侧边栏的 mCacheRecentList 只有它自己被打开时才更新，
                    //   采用它会把手上的实时数据（系统最近任务）覆盖成旧的。
                    String cur = "";
                    try {
                        cur = com.oupao.wiz.Cfg.getStrFresh("recents", "");
                    } catch (Throwable ignore) {
                    }
                    boolean useSidebar = (data.length() > 0)
                            && ((cur == null) || (cur.length() == 0));
                    if (useSidebar) {
                        com.oupao.wiz.Cfg.saveStr("recents", data);
                        try {
                            com.oupao.wiz.Cfg.writeStr("/data/system/oupao_cfg/recents", data);
                        } catch (Throwable ignore) {
                        }
                        // 旁路：写一份 Settings.Global，App 进程（小窗版更多应用）读这个更稳
                        try {
                            com.oupao.wiz.Cfg.gset("recents", data);
                        } catch (Throwable ignore) {
                        }
                    } else {
                        // 侧边栏那份抠不出来（元素结构不认识）→ 用系统最近任务兜底
                        try {
                            com.oupao.wiz.Tools.fallbackRecents(com.oupao.wiz.Cfg.sysContext());
                        } catch (Throwable ignore) {
                        }
                    }
                    com.oupao.wiz.IdxBar.invalidate();
                    com.oupao.wiz.MorePanel.refreshDelayed();
                    // App 进程里也收这条广播：顺手重建小窗版的「更多应用」页，
                    // 否则页面是打开时建的，最近应用要等下次重开才出现。
                    try {
                        if (com.oupao.wiz.MainActivity.pgPins != null) {
                            com.oupao.wiz.Pins.build();
                            com.oupao.wiz.SLog.i("Ev: 已重建更多应用页(小窗版)");
                        }
                    } catch (Throwable ignore) {
                    }
                    com.oupao.wiz.SLog.i("Ev: \u6536\u5230\u6700\u8fd1\u5e94\u7528 len=" + data.length());
                } catch (Throwable t) {
                    com.oupao.wiz.SLog.e("Ev: RECENTS \u5904\u7406\u5931\u8d25", t);
                }
                return;
            }
            if ("com.oupao.wiz.MINI".equals(p2)) {
                // 3242 调试通道：把当前小窗转成迷你窗（走 ROM 自己的 maySwitchStateToMini）。
                //   adb shell am broadcast -a com.oupao.wiz.MINI
                try {
                    com.oupao.wiz.SLog.i("Ev: MINI -> 转迷你窗");
                    com.oupao.wiz.Hooks.miniRetry(0);
                } catch (Throwable t) {
                    com.oupao.wiz.SLog.e("Ev: MINI 失败", t);
                }
                return;
            }
            if ("com.oupao.wiz.ZOOM".equals(p2)) {
                // 3241 调试通道：按包名**按顺序快速开小窗**（走的就是扇形菜单那条 FlyH.go）。
                //   adb shell am broadcast -a com.oupao.wiz.ZOOM \
                //       --es pkgs "com.android.settings,com.android.documentsui" --ei gap 120
                // 只用于真机验证「限制小窗数量 / 全屏→小窗过渡」，不影响正常使用。
                try {
                    String ps = p3.getStringExtra("pkgs");
                    int gap = p3.getIntExtra("gap", 250);
                    if (gap < 0) {
                        gap = 0;
                    }
                    if (ps != null) {
                        String[] arr = ps.split(",");
                        final android.content.Context cx = com.oupao.wiz.Cfg.sysContext();
                        android.os.Handler h = com.oupao.wiz.Hooks.main();
                        if ((cx != null) && (h != null)) {
                            for (int i = 0; i < arr.length; i++) {
                                final String pk = arr[i].trim();
                                if (pk.length() == 0) {
                                    continue;
                                }
                                final long delay = (long) i * gap;
                                h.postDelayed(new Runnable() {
                                    public void run() {
                                        try {
                                            android.content.Intent it =
                                                    cx.getPackageManager().getLaunchIntentForPackage(pk);
                                            if (it == null) {
                                                com.oupao.wiz.SLog.i("Ev: ZOOM 无启动 Intent " + pk);
                                                return;
                                            }
                                            com.oupao.wiz.SLog.i("Ev: ZOOM 开小窗 " + pk);
                                            com.oupao.wiz.FlyH.go(it, pk);
                                        } catch (Throwable t) {
                                            com.oupao.wiz.SLog.e("Ev: ZOOM 失败 " + pk, t);
                                        }
                                    }
                                }, delay);
                            }
                        }
                    }
                } catch (Throwable t) {
                    com.oupao.wiz.SLog.e("Ev: ZOOM 处理失败", t);
                }
                return;
            }
            if ("com.oupao.wiz.SHOWMORE".equals(p2)) {
                // App 进程请求「以面板形式立刻打开更多应用」→ 只有 system_server 能弹面板
                try {
                    if (android.os.Process.myUid() == 1000) {
                        com.oupao.wiz.MorePanel.show();
                        com.oupao.wiz.SLog.i("Ev: SHOWMORE -> 面板已弹出");
                    }
                } catch (Throwable t) {
                    com.oupao.wiz.SLog.e("Ev: SHOWMORE \u5931\u8d25", t);
                }
                return;
            }
            if ("com.oupao.wiz.CFG".equals(p2)) {
                // 2848：App 侧把广播里的固定应用**同步写进自己那份私有副本**。
                //   否则 App 在拿不到 Global 时会回退到过期副本，再经 PIN 分支写回 Global
                //   → 面板重开就「复原成旧数据」（实测：同一份 len=129 被反复写回）。
                try {
                    String[] ks = {"oupao_fly_pins", "oupao_desk_pins", "oupao_idx_pins"};
                    for (String k : ks) {
                        String v = p3.getStringExtra(k);
                        if (v != null && !v.isEmpty()) {
                            com.oupao.wiz.Cfg.saveStr(k, v);
                        }
                    }
                } catch (Throwable ignore) {
                }
                // 2856：**不能 return** —— 后面还有原有的 CFG 处理（气泡/开关状态刷新等），
                //   提前 return 会把它们全部截断。
            }
            if ("com.oupao.wiz.PINS_CHANGED".equals(p2)) {
                // 2845：固定应用变了 → 三处界面统一重建
                try {
                    com.oupao.wiz.IdxBar.invalidate();
                } catch (Throwable ignore) {
                }
                try {
                    com.oupao.wiz.MorePanel.refreshDelayed();
                } catch (Throwable ignore) {
                }
                try {
                    if (com.oupao.wiz.MainActivity.pgPins != null) {
                        com.oupao.wiz.Pins.build();
                        com.oupao.wiz.SLog.i("Ev: 固定应用变了 -> 已重建小窗版更多应用页");
                    }
                } catch (Throwable ignore) {
                }
                com.oupao.wiz.SLog.i("Ev: PINS_CHANGED 三处刷新");
                return;
            }
            if ("com.oupao.wiz.MODE".equals(p2)) {
                String mode = p3.getStringExtra("mode");
                if (mode != null) {
                    try {
                        com.oupao.wiz.Cfg.gset("oupao_more_mode", mode);
                        com.oupao.wiz.SLog.i("Ev: MODE -> " + mode);
                    } catch (Throwable t) {
                        com.oupao.wiz.SLog.e("Ev: MODE 写入失败", t);
                    }
                }
                return;
            }
            if ("com.oupao.wiz.PIN".equals(p2)) {
                // 面板加减固定应用：由收到广播的进程（App）执行，和菜单管理页同一套逻辑
                String tag = p3.getStringExtra("tag");
                String op = p3.getStringExtra("op");
                if (tag != null) {
                    // 2846：按广播里带的 pinsKey 写**对应那一套**（三套互不影响）
                    String pinsKey = p3.getStringExtra("pinsKey");
                    if (pinsKey == null || pinsKey.isEmpty()) {
                        pinsKey = "oupao_fly_pins";
                    }
                    String cur = com.oupao.wiz.Cfg.pinsOf(pinsKey);
                    String next = "del".equals(op)
                            ? com.oupao.wiz.Pins.del(cur, tag)
                            : com.oupao.wiz.Pins.withAdd(cur, tag);
                    if (!next.equals(cur)) {
                        com.oupao.wiz.Cfg.writePinsKey(pinsKey, next);
                        try {
                            // 只在列表真的变了时才重建（避免每次点击都刷新整页）
                            com.oupao.wiz.Pins.build();
                        } catch (Throwable ignore) {
                        }
                    }
                    com.oupao.wiz.SLog.i("Ev: PIN " + op + " " + tag);
                }
                return;
            }
            if (!"com.oupao.wiz.CLONE".equals(p2)) {
                if (!"com.oupao.wiz.TOOL".equals(p2)) {
                    if (!"com.oupao.wiz.CLOSE".equals(p2)) {
                        if (!"com.oupao.wiz.URL".equals(p2)) {
                            if (!"com.oupao.wiz.DIAG_REQ".equals(p2)) {
                                com.oupao.wiz.Ev.cCfg = (com.oupao.wiz.Ev.cCfg + 1);
                                com.oupao.wiz.Cfg.apply(p3);
                            } else {
                                com.oupao.wiz.Ev.cDiag = (com.oupao.wiz.Ev.cDiag + 1);
                                com.oupao.wiz.Diag.onReq(p3);
                                return;
                            }
                        } else {
                            com.oupao.wiz.Ev.cUrl = (com.oupao.wiz.Ev.cUrl + 1);
                            com.oupao.wiz.Sug.onWebUrl(p3.getStringExtra("url"));
                            return;
                        }
                    } else {
                        com.oupao.wiz.Ev.cClose = (com.oupao.wiz.Ev.cClose + 1);
                        com.oupao.wiz.SLog.i("FlyClose: \u6536\u5230 CLOSE \u5e7f\u64ad");
                        com.oupao.wiz.Hooks.doClose();
                        return;
                    }
                } else {
                    com.oupao.wiz.Ev.cTool = (com.oupao.wiz.Ev.cTool + 1);
                    com.oupao.wiz.Tools.fireFrom(p3);
                    return;
                }
            } else {
                com.oupao.wiz.Ev.cClone = (com.oupao.wiz.Ev.cClone + 1);
                com.oupao.wiz.CloneK.handle(p3);
                return;
            }
        }
        return;
    }
    public static android.content.Intent it(String p2)
    {
        return new android.content.Intent(p2).setPackage("android");
    }
    public static void send(android.content.Intent p1)
    {
        var v0 = com.oupao.wiz.Cfg.ctx;
        if (v0 != null) {
            v0.sendBroadcast(p1);
        }
        return;
    }
    public static String stat()
    {
        return new StringBuilder().append("Ev: CFG=").append(com.oupao.wiz.Ev.cCfg).append(" URL=").append(com.oupao.wiz.Ev.cUrl).append(" CLONE=").append(com.oupao.wiz.Ev.cClone).append(" TOOL=").append(com.oupao.wiz.Ev.cTool).append(" CLOSE=").append(com.oupao.wiz.Ev.cClose).append(" DIAG=").append(com.oupao.wiz.Ev.cDiag).toString();
    }
}