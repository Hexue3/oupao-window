package com.oupao.wiz;
public class Pins {
    public static boolean appMgr;
    private static String[] appsC;
    private static long appsE;
    private static int appsR;
    public static android.widget.LinearLayout card;
    static boolean deskBuild;
    /**
     * 2841：当前是不是在「小窗版更多应用」页里读写固定应用。
     *
     * 小窗页跑在 **App 进程**，面板/引索条跑在 **system_server**，两个进程不存在互相污染，
     * 所以用一个静态标志分流最省事：小窗页读写 `Cfg.deskPins()`，其它一律 `Cfg.flyPins()`。
     */
    public static boolean deskMode;

    /** 2842：小窗版「更多应用」专用读取（不依赖 deskMode，避免页面构建顺序问题） */
    public static java.util.ArrayList listDesk() {
        try {
            return com.oupao.wiz.Pins.parse(com.oupao.wiz.Cfg.deskPins());
        } catch (Throwable ignore) {
        }
        return new java.util.ArrayList<String>();
    }

    /** 2842：小窗版「更多应用」专用保存 */
    public static void saveDesk(String v) {
        try {
            if (v == null || v.isEmpty()) {
                v = com.oupao.wiz.Cfg.PINS_EMPTY;
            }
            com.oupao.wiz.SLog.r("r-pins", "Pins.saveDesk -> " + v, 1000L);
            com.oupao.wiz.Cfg.setDeskPins(v);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Pins: saveDesk 异常", t);
        }
    }
    static int tab;
    static android.widget.TextView tab0;
    static android.widget.TextView tab1;
    static android.widget.LinearLayout tabBox;
    public Pins()
    {
        return;
    }
    public static String[] apps(android.content.Context p10)
    {
        android.content.pm.PackageManager v1_2 = null;
        java.util.List v0_3 = null;
        var v0_0 = com.oupao.wiz.Pins.appsC;
        if ((v0_0 == null) || (Long.compare((com.oupao.wiz.Pins.appsE - android.os.SystemClock.uptimeMillis()), 0) < 0)) {
            com.oupao.wiz.Tm.enter();
            java.lang.String[] v9_1 = new String[0];
            try {
                var v0_2 = new android.content.Intent("android.intent.action.MAIN");
                v0_2.addCategory("android.intent.category.LAUNCHER");
                v1_2 = p10.getPackageManager();
                v0_3 = v1_2.queryIntentActivities(v0_2, 0);
            } catch (Throwable __e_v0) {
                com.oupao.wiz.Tm.exit();
                var v0_7 = com.oupao.wiz.Pins.appsC;
                if (v0_7 != null) {
                    var v7_5 = (com.oupao.wiz.Pins.appsR + 1);
                    com.oupao.wiz.Pins.appsR = v7_5;
                    if (v7_5 < 2) {
                        var v0_9 = com.oupao.wiz.Cfg.ctx;
                        if (v0_9 != null) {
                            return com.oupao.wiz.Pins.apps(v0_9);
                        }
                    }
                    return v9_1;
                } else {
                    return v0_7;
                }
            }
            if (v0_3 != null) {
                var v2_1 = v0_3.size();
                String[] v3_0 = new String[v2_1];
                int v4 = 0;
                while (v4 < v2_1) {
                    var v5_1 = ((android.content.pm.ResolveInfo) v0_3.get(v4));
                    var v6 = v5_1.activityInfo;
                    if (v6 != null) {
                        var v7_2 = new StringBuilder();
                        v7_2.append(v5_1.loadLabel(v1_2));
                        v7_2.append("\u0001");
                        v7_2.append(v6.packageName);
                        v7_2.append("#");
                        v7_2.append(v6.name);
                        v3_0[v4] = v7_2.toString();
                    }
                    v4++;
                }
                java.util.Arrays.sort(v3_0);
                v9_1 = com.oupao.wiz.Pins.merge(v3_0, com.oupao.wiz.Pins.clones(p10));
            }
            com.oupao.wiz.Tm.exit();
            com.oupao.wiz.Pins.appsR = 0;
            if (v9_1.length > 0) {
                com.oupao.wiz.Pins.appsC = v9_1;
                com.oupao.wiz.Pins.appsE = (android.os.SystemClock.uptimeMillis() + 30000);
            }
            return v9_1;
        } else {
            return v0_0;
        }
    }
    public static void build()
    {
        // 原「菜单管理」页（非 desk 构建）已删除：页面只在小窗模式下用 desk 构建。
        try {
            com.oupao.wiz.Pins.buildDesk();
        } catch (Throwable __e_v11) {
            com.oupao.wiz.SLog.e(__e_v11.toString());
        }
        return;
    }
        public static void buildDesk()
    {
        android.widget.LinearLayout v0 = null;
        try {
            com.oupao.wiz.Pins.deskBuild = true;
            com.oupao.wiz.Pins.deskMode = true;   // 2842：buildDesk 只服务小窗页
            // 不要重置 tab：每次重建都把分区拉回「应用分区」。
            // 添加/删除固定应用后重建，用户看的分区应该保留。
            com.oupao.wiz.Tm.reset();
            com.oupao.wiz.Tm.mark();
            v0 = com.oupao.wiz.MainActivity.pgPins;
        } catch (Throwable __e_v7) {
            com.oupao.wiz.Pins.deskBuild = false;
            return;
        }
        if (v0 != null) {
            v0.removeAllViews();
            var v1 = v0.getContext();
            if (v1 != null) {
                com.oupao.wiz.PinUI.topBarMgr(v1, v0, "\u66f4\u591a\u5e94\u7528");
                var v2_1 = com.oupao.wiz.PinUI.pick(v1);
                var v3_0 = v2_1.size();
                var v4_1 = com.oupao.wiz.Pins.list().size();
                var v5_1 = com.oupao.wiz.Pins.apps(v1).length;
                var v6_1 = new StringBuilder();
                v6_1.append("desk pick=");
                v6_1.append(v3_0);
                v6_1.append(" list=");
                v6_1.append(v4_1);
                v6_1.append(" apps=");
                v6_1.append(v5_1);
                v6_1.append(" cfg=");
                v6_1.append(com.oupao.wiz.Cfg.getStr("oupao_fly_pins", "").length());
                com.oupao.wiz.MainActivity.dlog(v6_1.toString());
                com.oupao.wiz.Tm.log("p1.data");
                if (v3_0 > 0) {
                    int v3_3 = 0;
                    com.oupao.wiz.PinUI.sectionTitle(v1, v0, "\u5df2\u6dfb\u52a0");
                    if (!com.oupao.wiz.Pins.appMgr) {
                        v3_3 = 5;
                    } else {
                        v3_3 = 1;
                    }
                    com.oupao.wiz.PinUI.cardGrid(v1, v0, v2_1, v3_3);
                    com.oupao.wiz.Tm.log("p2.pins");
                } else {
                    com.oupao.wiz.PinUI.sectionTitle(v1, v0, "\u8fd8\u6ca1\u6709\u56fa\u5b9a\u5e94\u7528\uff08\u70b9\u4e0b\u9762\u7684 + \u6dfb\u52a0\uff09");
                }
                // 「应用 / 工具」两个分区：不管有没有固定应用都要建。
                // 原来这段写在 if (v3_0 > 0) 里面，没固定应用时整页只剩一行提示 + 索引条。
                com.oupao.wiz.Pins.tabBar(v1, v0);
                var v3_5 = new android.widget.LinearLayout(v1);
                v3_5.setOrientation(1);
                com.oupao.wiz.Pins.tabBox = v3_5;
                v0.addView(v3_5);
                com.oupao.wiz.Pins.buildTab();
                com.oupao.wiz.Tm.log("p3.tools");
            }
        }
        com.oupao.wiz.Pins.deskBuild = false;
        return;
    }
    public static void buildTab()
    {
        android.widget.LinearLayout v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Pins.tabBox;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_0 != null) {
            v0_0.removeAllViews();
            var v1 = v0_0.getContext();
            if (v1 != null) {
                int v2 = 0;
                if ((!com.oupao.wiz.MainActivity.deskOn) || (com.oupao.wiz.Pins.appMgr)) {
                    v2 = 1;
                }
                int v5 = 0;
                if (com.oupao.wiz.Pins.tab != 0) {
                    var v4_1 = new java.util.ArrayList(java.util.Arrays.asList(com.oupao.wiz.Tools.defs()));
                    v5 = 5;
                    if (v2 != 0) {
                        v5 = 4;
                    }
                    com.oupao.wiz.PinUI.cardGrid(v1, v0_0, v4_1, v5);
                } else {
                    // 「最近应用」（数据来自侧边栏）——放在字母分区上方
                    try {
                        var v4_r = com.oupao.wiz.Pins.recents(v1);
                        if (v4_r.size() > 0) {
                            com.oupao.wiz.PinUI.sectionTitle(v1, v0_0, "\u6700\u8fd1\u5e94\u7528");
                            // 3160：**恢复"跟随管理模式"**（3159 强制带角标是错的，用户明确要求角标只在管理模式出现）。
                            // 间距要不要统一是另一件事：1/5 两种格子的高度本来就不同，
                            // 想彻底一致得给 5 号格子也留出角标位（待做，见记忆）。
                            com.oupao.wiz.PinUI.cardGrid(v1, v0_0, v4_r,
                                    com.oupao.wiz.Pins.appMgr ? 1 : 5);
                            com.oupao.wiz.SLog.i("Pins: \u6700\u8fd1\u5e94\u7528 n=" + v4_r.size());
                        }
                    } catch (Throwable ignore) {
                    }
                    var v4_3 = new java.util.ArrayList(java.util.Arrays.asList(com.oupao.wiz.Pins.apps(v1)));
                    v5 = 6;
                    if (v2 != 0) {
                        v5 = 2;
                    }
                    com.oupao.wiz.PinUI.cardGrid(v1, v0_0, v4_3, v5);
                }
                com.oupao.wiz.MainActivity.dlog(new StringBuilder().append("tabGrid tab=").append(com.oupao.wiz.Pins.tab).append(" mode=").append(v5).append(" mgr=").append(v2).toString());
            }
        }
        return;
    }
    /**
     * 最近应用（tag 列表），数据来自侧边栏（system_server 的 SideDock 拉取 → Cfg "recents"）。
     *
     * 每行 `label\u0001pkg`，这里把 pkg 解析成 `pkg#cls` 的 tag（和其它应用一致，
     * 点击走 MainActivity/FlyH 的普通启动路径）。
     */
    public static java.util.ArrayList<String> recents(android.content.Context p5)
    {
        var out = new java.util.ArrayList<String>();
        try {
            String s = com.oupao.wiz.Cfg.getStrFresh("recents", "");
            if ((s == null) || (s.length() == 0)) {
                // 旁路：system_server 写的 Settings.Global（App 进程读这个更稳）
                s = com.oupao.wiz.Cfg.ggetC(p5, "recents");
            }
            com.oupao.wiz.SLog.i("Pins: recents raw len=" + ((s == null) ? -1 : s.length()));
            if ((s == null) || (s.length() == 0)) {
                return out;
            }
            var pm = p5.getPackageManager();
            var seen = new java.util.HashSet<String>();
            // ★ 已固定的应用不要出现在「最近应用」里（面板模式 / 引索条都是这么过滤的，
            //   小窗版之前漏了这一步，导致固定过的 app 在最近应用里又出现一次）
            var pins = com.oupao.wiz.Pins.list();
            var lines = s.split("\n");
            for (int i = 0; (i < lines.length) && (out.size() < 8); i++) {
                String line = lines[i];
                int c = line.indexOf('\u0001');
                if (c <= 0) {
                    continue;
                }
                String pkg = line.substring(c + 1).trim();
                if (pkg.length() == 0) {
                    continue;
                }
                String cls = null;
                try {
                    var it = new android.content.Intent(android.content.Intent.ACTION_MAIN);
                    it.addCategory(android.content.Intent.CATEGORY_LAUNCHER);
                    it.setPackage(pkg);
                    var list = pm.queryIntentActivities(it, 0);
                    if ((list != null) && (list.size() > 0)) {
                        var ri = (android.content.pm.ResolveInfo) list.get(0);
                        if ((ri != null) && (ri.activityInfo != null)) {
                            cls = ri.activityInfo.name;
                        }
                    }
                } catch (Throwable ignore) {
                }
                if (cls == null) {
                    continue;
                }
                String tag = pkg + "#" + cls;
                if (pins.contains(tag)) {
                    continue;           // 已固定 → 不进「最近应用」
                }
                if (!seen.add(tag)) {
                    continue;
                }
                out.add(tag);
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Pins: recents 失败", t);
        }
        return out;
    }

    public static java.util.ArrayList cap5(java.util.ArrayList p7, int p8)
    {
        try {
            if ((p8 > 0) && (p8 > 5)) {
                var v1_1 = new java.util.ArrayList();
                int v2 = 0;
                while (v2 < 5) {
                    v1_1.add(p7.get(v2));
                    v2++;
                }
                return v1_1;
            } else {
                return p7;
            }
        } catch (Throwable __e_v5) {
            return p7;
        }
    }
    public static String[] clones(android.content.Context p5)
    {
        try {
            p5.getFilesDir();
        } catch (Throwable __e_v0) {
        }
        java.lang.String[] v0_0 = new String[0];
        java.lang.String v1_1 = null;
        try {
            // 用 getStrFresh **绕过进程级缓存**：clones 是 system_server 扫描后写的，
            // App 进程早先读过一次空值就会把 "" 缓存进 cS，之后一直读不到分身
            // （和 oupao_bub_pos 是同一类坑）。取不到再退回 ggetC / getStr。
            v1_1 = com.oupao.wiz.Cfg.getStrFresh("clones", "");
            if ((v1_1 == null) || (v1_1.length() == 0)) {
                v1_1 = com.oupao.wiz.Cfg.ggetC(p5, "clones");
            }
        } catch (Throwable __e_v1) {
            return v0_0;
        }
        if ((v1_1 != null) && (v1_1.length() > 0)) {
            v0_0 = v1_1.split("\n");
        }
        return v0_0;
    }
    public static String del(String p2, String p3)
    {
        var v0 = com.oupao.wiz.Pins.parse(p2);
        v0.remove(p3);
        return com.oupao.wiz.Pins.join(v0);
    }
    public static android.view.View hit(android.view.MotionEvent p13)
    {
        var v0_0 = com.oupao.wiz.Pins.card;
        if (v0_0 != null) {
            int[] v1_1 = new int[2];
            var v2_1 = ((int) p13.getRawX());
            var v3_1 = ((int) p13.getRawY());
            int v4 = 0;
            var v5 = v0_0.getChildCount();
            while (v4 < v5) {
                var v6_0 = v0_0.getChildAt(v4);
                if ((v6_0 instanceof android.view.ViewGroup)) {
                    var v6_1 = ((android.view.ViewGroup) v6_0);
                    int v7_1 = 0;
                    var v8 = v6_1.getChildCount();
                    while (v7_1 < v8) {
                        var v9 = v6_1.getChildAt(v7_1);
                        if (v9.getTag() != null) {
                            v9.getLocationOnScreen(v1_1);
                            var v11_1 = v1_1[0];
                            if ((v2_1 >= v11_1) && (v2_1 <= (v9.getWidth() + v11_1))) {
                                var v11_2 = v1_1[1];
                                if ((v3_1 >= v11_2) && (v3_1 <= (v9.getHeight() + v11_2))) {
                                    return v9;
                                }
                            }
                        }
                        v7_1++;
                    }
                }
                v4++;
            }
        }
        return null;
    }
    public static String join(java.util.ArrayList p4)
    {
        var v0_1 = new StringBuilder();
        int v1 = 0;
        while ((p4 != null) && (v1 < p4.size())) {
            if (v1 != 0) {
                v0_1.append(";");
            }
            v0_1.append(((String) p4.get(v1)));
            v1++;
        }
        return v0_1.toString();
    }
    public static String label(android.content.Context p3, String p4)
    {
        java.lang.String v0_1 = null;
        try {
            var v0_0 = p3.getPackageManager();
            v0_1 = v0_0.getApplicationLabel(v0_0.getApplicationInfo(p4, 0)).toString();
        } catch (Throwable __e_v0) {
            v0_1 = p4;
        }
        return v0_1;
    }
    public static java.util.ArrayList list()
    {
        try {
            // 固定应用只有一个真正来源：Cfg.flyPins()（Global 优先）。
            // 绝不能回落 MainActivity.deskPins：那是启动快照，
            // 旧键 "oupao_pins" 里残留的历史数据会在「清空固定应用」后被当成已固定显示出来。
            return com.oupao.wiz.Pins.parse(com.oupao.wiz.Pins.deskMode
                    ? com.oupao.wiz.Cfg.deskPins() : com.oupao.wiz.Cfg.flyPins());
        } catch (Throwable ignore) {
        }
        return new java.util.ArrayList<String>();
    }
    public static String[] merge(String[] p4, String[] p5)
    {
        if (p4 == null) {
            return p5;
        } else {
            if ((p5 == null) || (p5.length <= 0)) {
                return p4;
            } else {
                try {
                    var v0_1 = p4.length;
                    var v1 = p5.length;
                    String[] v2_1 = new String[(v0_1 + v1)];
                    System.arraycopy(p4, 0, v2_1, 0, v0_1);
                    System.arraycopy(p5, 0, v2_1, v0_1, v1);
                    java.util.Arrays.sort(v2_1);
                    return v2_1;
                } catch (Throwable __e_v0) {
                    return p4;
                }
            }
        }
    }
    public static android.widget.LinearLayout navRow(android.content.Context p8, String p9, String p10)
    {
        var v0_1 = new android.widget.LinearLayout(p8);
        v0_1.setOrientation(0);
        v0_1.setGravity(16);
        v0_1.setPadding(com.oupao.wiz.Ui.dp(p8, 20), com.oupao.wiz.Ui.dp(p8, 14), com.oupao.wiz.Ui.dp(p8, 20), com.oupao.wiz.Ui.dp(p8, 14));
        com.oupao.wiz.Ui.ripple(v0_1);
        var v3_1 = new android.widget.LinearLayout(p8);
        v3_1.setOrientation(1);
        v3_1.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        v3_1.addView(com.oupao.wiz.Ui.t(p8, p9, 16.0f, -14935012, false));
        if (p10 != null) {
            v3_1.addView(com.oupao.wiz.Ui.t(p8, p10, 13.0f, -7434605, false));
        }
        v0_1.addView(v3_1);
        v0_1.addView(com.oupao.wiz.Ui.t(p8, "\u203a", 28.0f, -7434605, true));
        return v0_1;
    }
    public static java.util.ArrayList parse(String p5)
    {
        var v0_1 = new java.util.ArrayList();
        if ((p5 != null) && (p5.length() != 0)) {
            var v1_2 = p5.split(";");
            int v2 = 0;
            while (v2 < v1_2.length) {
                var v3_1 = v1_2[v2];
                if (v3_1.length() != 0) {
                    v0_1.add(v3_1);
                }
                v2++;
            }
        }
        return v0_1;
    }
    public static String pkg(String p3)
    {
        return p3.split("#")[0];
    }
    /**
     * 只改顺序并保存，**不重建页面**。
     *
     * reorder() 会调 build() 重建整页：拖拽源 View 被销毁 → 拖拽阴影的 Surface
     * 被 release → system_server 在 endDragLocked 里 reparent 已 release 的 surface → NPE → 手机重启。
     * 且重建本身还会带来替换延迟。
     */
    /**
     * 算出把 from 挪到 to 之后的新顺序（**不保存、不重建**）。
     *
     * 用于拖拽排序的「就地重排视图」：先得到新顺序，
     * 再把已有的格子 view 按这个顺序放回去。
     */
    /**
     * 「互换」顺序：把 from 和 to 两个位置直接对调，其余不动。
     * （用户要求：拖动是替换目标位置，而不是把目标挤到下一格）
     */
    public static java.util.ArrayList<String> swapOrder(String from, String to) {
        var out = new java.util.ArrayList<String>();
        try {
            out.addAll(com.oupao.wiz.Pins.list());
            if ((from == null) || (to == null) || from.equals(to)) {
                return out;
            }
            int i = out.indexOf(from);
            int j = out.indexOf(to);
            if ((i < 0) || (j < 0)) {
                return out;
            }
            out.set(i, to);
            out.set(j, from);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Pins: swapOrder \u5f02\u5e38", t);
        }
        return out;
    }

    public static java.util.ArrayList<String> computeOrder(String from, String to) {
        var out = new java.util.ArrayList<String>();
        try {
            var v0_2 = com.oupao.wiz.Pins.list();
            out.addAll(v0_2);
            if ((from == null) || (to == null) || from.equals(to)) {
                return out;
            }
            int v1_1 = out.indexOf(from);
            int v2 = out.indexOf(to);
            if ((v1_1 < 0) || (v2 < 0)) {
                return out;
            }
            out.remove(v1_1);
            int v3 = v2;
            if (v3 < 0) {
                v3 = 0;
            }
            int size = out.size();
            if (v3 > size) {
                v3 = size;
            }
            out.add(v3, from);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Pins: computeOrder \u5f02\u5e38", t);
        }
        return out;
    }

    public static void saveOrderNoBuild(String from, String to) {
        try {
            if ((from == null) || (to == null) || from.equals(to)) {
                return;
            }
            var v0_2 = com.oupao.wiz.Pins.list();
            int v1_1 = v0_2.indexOf(from);
            int v2 = v0_2.indexOf(to);
            if ((v1_1 < 0) || (v2 < 0)) {
                return;
            }
            v0_2.remove(v1_1);
            int v3 = v2;
            if (v3 < 0) {
                v3 = 0;
            }
            int v1_0 = v0_2.size();
            if (v3 > v1_0) {
                v3 = v1_0;
            }
            v0_2.add(v3, from);
            com.oupao.wiz.Pins.save(com.oupao.wiz.Pins.join(v0_2));
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Pins: saveOrderNoBuild \u5f02\u5e38", t);
        }
    }

    public static void reorder(String p4, String p5)
    {
        try {
            if ((p4 != null) && ((p5 != null) && (!p4.equals(p5)))) {
                var v0_2 = com.oupao.wiz.Pins.list();
                var v1_1 = v0_2.indexOf(p4);
                var v2 = v0_2.indexOf(p5);
                if ((v1_1 >= 0) && (v2 >= 0)) {
                    v0_2.remove(v1_1);
                    int v3 = v2;
                    if (v3 < 0) {
                        v3 = 0;
                    }
                    var v1_0 = v0_2.size();
                    if (v3 > v1_0) {
                        v3 = v1_0;
                    }
                    v0_2.add(v3, p4);
                    com.oupao.wiz.Pins.save(com.oupao.wiz.Pins.join(v0_2));
                    com.oupao.wiz.Pins.build();
                            }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }

    public static void save(String p3)
    {
        try {
            // 空列表用哨兵值 "0" 表示「显式清空」：
            // Cfg.saveStr() 会拒绝真空串（防 App 启动时把系统侧清零），
            // 不转换的话删到最后一个就写不进去，旧值还在。
            if (p3 == null || p3.isEmpty()) {
                p3 = com.oupao.wiz.Cfg.PINS_EMPTY;
            }
            com.oupao.wiz.SLog.r("r-pins", "Pins.save(" + (deskMode ? "desk" : "panel") + ") -> " + p3, 1000L);
            if (deskMode) {
                // 2841：小窗模式那套独立保存
                com.oupao.wiz.Cfg.setDeskPins(p3);
                return;
            }
            // 2846：统一走 writePinsKey（文件 + Global + 广播 + push + 三处刷新）
            com.oupao.wiz.Cfg.writePinsKey("oupao_fly_pins", p3);
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    static void styleOne(android.widget.TextView p3, boolean p4)
    {
        try {
            if (p3 != null) {
                var v0 = p3.getContext();
                if (!p4) {
                    p3.setTextColor(com.oupao.wiz.Ui.adaptText(-6710887));
                    p3.setBackground(com.oupao.wiz.Ui.bg(-855310, ((float) com.oupao.wiz.Ui.dp(v0, 19))));
                } else {
                    p3.setTextColor(com.oupao.wiz.Ui.adaptText(-1));
                    p3.setBackground(com.oupao.wiz.Ui.bg(-13669377, ((float) com.oupao.wiz.Ui.dp(v0, 19))));
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static void styleTabs()
    {
        int v1_2 = 0;
        if (com.oupao.wiz.Pins.tab == 0) {
            v1_2 = 1;
        } else {
            v1_2 = 0;
        }
        int v1_1 = 0;
        com.oupao.wiz.Pins.styleOne(com.oupao.wiz.Pins.tab0, v1_2 != 0);
        if (com.oupao.wiz.Pins.tab == 1) {
            v1_1 = 1;
        } else {
            v1_1 = 0;
        }
        com.oupao.wiz.Pins.styleOne(com.oupao.wiz.Pins.tab1, v1_1 != 0);
        return;
    }
    public static void tabBar(android.content.Context p8, android.widget.LinearLayout p9)
    {
        try {
            var v0_1 = new android.widget.LinearLayout(p8);
            v0_1.setOrientation(0);
            v0_1.setGravity(17);
            var v1_3 = com.oupao.wiz.Pins.tabBtn(p8, "\u5e94\u7528", 0);
            com.oupao.wiz.Pins.tab0 = v1_3;
            var v2_1 = com.oupao.wiz.Pins.tabBtn(p8, "\u529f\u80fd", 1);
            com.oupao.wiz.Pins.tab1 = v2_1;
            var v3_2 = new android.widget.LinearLayout.LayoutParams(0, com.oupao.wiz.Ui.dp(p8, 38), 1.0f);
            v3_2.rightMargin = com.oupao.wiz.Ui.dp(p8, 6);
            v0_1.addView(v1_3, v3_2);
            var v3_4 = new android.widget.LinearLayout.LayoutParams(0, com.oupao.wiz.Ui.dp(p8, 38), 1.0f);
            v3_4.leftMargin = com.oupao.wiz.Ui.dp(p8, 6);
            v0_1.addView(v2_1, v3_4);
            v0_1.setPadding(com.oupao.wiz.Ui.dp(p8, 20), com.oupao.wiz.Ui.dp(p8, 8), com.oupao.wiz.Ui.dp(p8, 20), com.oupao.wiz.Ui.dp(p8, 8));
            p9.addView(v0_1);
            com.oupao.wiz.Pins.styleTabs();
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static android.widget.TextView tabBtn(android.content.Context p3, String p4, int p5)
    {
        try {
            var v0_1 = new android.widget.TextView(p3);
            v0_1.setText(p4);
            v0_1.setGravity(17);
            v0_1.setTextSize(15.0f);
            v0_1.setTag(new Integer(p5));
            v0_1.setOnClickListener(new com.oupao.wiz.PinTab());
            return v0_1;
        } catch (Throwable __e_v0) {
            return null;
        }
    }
    public static String withAdd(String p3, String p4)
    {
        var v0 = com.oupao.wiz.Pins.parse(p3);
        if (v0.contains(p4)) {
            return p3;
        } else {
            v0.add(p4);
            return com.oupao.wiz.Pins.join(v0);
        }
    }
}