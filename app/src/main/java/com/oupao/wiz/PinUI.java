package com.oupao.wiz;
public class PinUI {
    /** 3168：标题区顶部留白（进程内固定，避免重建时变来变去；-1 = 还没算过） */
    private static int topPadOnce = -1;
    public static boolean barDone;
    public static android.view.View railView;
    public PinUI()
    {
        return;
    }
    public static void cardGrid(android.content.Context p12, android.widget.LinearLayout p13, java.util.List p14, int p15)
    {
        int v0_0 = 0;
        try {
            v0_0 = p14.size();
            com.oupao.wiz.SLog.v(String.valueOf(v0_0));
        } catch (Throwable __e_v11) {
            com.oupao.wiz.SLog.e(__e_v11.toString());
            return;
        }
        if (v0_0 > 0) {
            var v8 = com.oupao.wiz.Ui.card(p12);
            v8.setShowDividers(0);
            v8.setPadding(com.oupao.wiz.Ui.dp(p12, 16), com.oupao.wiz.Ui.dp(p12, 14), com.oupao.wiz.Ui.dp(p12, 16), com.oupao.wiz.Ui.dp(p12, 14));
            com.oupao.wiz.PgGrid.reset();
            com.oupao.wiz.PgGrid.lastLetter = null;
            if ((p15 == 2) || (p15 == 6)) {
                java.util.Collections.sort(p14, new com.oupao.wiz.PinCmp());
            } else {
            }
            var v6 = com.oupao.wiz.Pins.listDesk();
            var v7 = p14.iterator();
            while (v7.hasNext()) {
                java.lang.String v3 = null;
                java.lang.String v2_1 = null;
                var v9_1 = ((String) v7.next());
                var v1_25 = v9_1.indexOf("\u0001");
                if (v1_25 < 0) {
                    v3 = v9_1;
                    v2_1 = com.oupao.wiz.PinUI.labelOf(p12, v9_1);
                } else {
                    v2_1 = v9_1.substring(0, v1_25);
                    var v1_26 = (v1_25 + 1);
                    var v10_0 = v9_1.indexOf("\u0001", v1_26);
                    if (v10_0 < 0) {
                        v3 = v9_1.substring(v1_26);
                    } else {
                        v3 = v9_1.substring(v1_26, v10_0);
                        com.oupao.wiz.PgGrid.setEmoji(v9_1.substring((v10_0 + 1)));
                    }
                }
                // 已固定的 app 不再出现在「全部应用」的字母分区里（它已经在「已添加」区）。
                // 注意：不能靠下面的 10001 分支去跳，因为 mode 5/6 会把徽标置回 0，
                // 那个分支就失效了（之前就是这个原因导致已固定 app 重复出现）。
                // 只有 mode 2 / 6 是「全部应用」列表（已固定的要跳过）；
                // mode 1 / 5 是「已添加」区本身（管理模式=1、普通模式=5），必须显示。
                // 1977 那版把 5 也算进去了，结果关掉管理模式后「已添加」图标全没了。
                if (((p15 == 2) || (p15 == 6)) && v6.contains(v3)) {
                    continue;
                }
                int v5_1 = 0;
                if (p15 != 3) {
                    v5_1 = 10000;
                    if (p15 != 1) {
                        if (v6.contains(v3)) {
                            v5_1 = 10001;
                        }
                    } else {
                        v5_1 = 10003;
                    }
                } else {
                }
                if ((p15 == 5) || (p15 == 6)) {
                    v5_1 = 0;
                } else {
                }
                var v10_3 = v5_1;
                if ((p15 == 1) || ((p15 == 5) || (v10_3 != 10001))) {
                    if ((p15 == 2) || (p15 == 6)) {
                        var v0_35 = com.oupao.wiz.PinUI.firstLetter(v2_1);
                        if (!v0_35.equals(com.oupao.wiz.PgGrid.lastLetter)) {
                            com.oupao.wiz.PgGrid.lastLetter = v0_35;
                            com.oupao.wiz.PgGrid.gcol = 0;
                            com.oupao.wiz.PgGrid.grow = null;
                            com.oupao.wiz.PinUI.compactTitle(p12, v8, v0_35);
                        }
                    } else {
                    }
                    android.graphics.drawable.Drawable v4_0 = null;
                    if (p15 != 3) {
                        if (!v3.startsWith("tool#")) {
                            var v0_6 = v3.indexOf("#");
                            if (v0_6 >= 0) {
                                v4_0 = com.oupao.wiz.Bar.cloneDrawable(p12, v3, com.oupao.wiz.Ico.d(p12, v3.substring(0, v0_6)));
                            }
                            if (v4_0 == null) {
                                com.oupao.wiz.PgGrid.setEmoji(v2_1.substring(0, 1).toUpperCase());
                                v4_0 = null;
                            }
                        } else {
                            v4_0 = com.oupao.wiz.Tools.drawable(p12, v3);
                            if (v4_0 != null) {
                                com.oupao.wiz.PgGrid.setEmoji(null);
                            }
                        }
                    }
                    // 1820fix: smali :cond_102 → PgGrid.cell 无条件调用（原来被嵌进 if，功能列表全空）
                    com.oupao.wiz.PgGrid.cell(p12, v8, v2_1, v3, v4_0, v10_3);
                    var v0_14 = com.oupao.wiz.PgGrid.lastCell;
                    java.lang.Object v4_2 = null;
                    if (p15 == 3) {
                        v4_2 = new com.oupao.wiz.PinT(v3);
                    } else if ((p15 == 5) || (p15 == 6)) {
                        v4_2 = new com.oupao.wiz.PinL(v3);
                    } else if (p15 == 1) {
                        v4_2 = new com.oupao.wiz.PinR(v3);
                    } else if (v10_3 != 10001) {
                        v4_2 = new com.oupao.wiz.PinW(v3);
                    }
                    if (v4_2 != null) {
                        var v1_12 = com.oupao.wiz.PgGrid.lastBadge;
                        if (v1_12 == null) {
                            var v1_13 = com.oupao.wiz.PgGrid.lastCell;
                            if (v1_13 != null) {
                                v1_13.setOnClickListener((android.view.View.OnClickListener) v4_2);
                            }
                        } else {
                            v1_12.setOnClickListener((android.view.View.OnClickListener) v4_2);
                        }
                    }
                    // 长按拖动排序：mode==1 且 (appMgr 或 非桌面构建时在 pgPins)
                    // 长按拖动排序：小窗页（pgPins）无条件可用；其它页需要「管理」模式。
                    // 不再依赖 deskBuild（那个字段全工程没赋值，恒 false，纯干扰）。
                    if ((p15 == 1) && (v0_14 != null)) {
                        final boolean vNeedMgr = (p13 != com.oupao.wiz.MainActivity.pgPins);
                        // 自己实现的长按+移动排序（不用系统 drag-and-drop，避免 system_server 崩溃）
                        // 注意：小窗页的点击是绑在 **badge** 上的，badge 才是触摸流持有者；
                        // 绑到 cell 上的话事件根本不会进来（表现就是拖一次后必须退出管理模式才能再拖）。
                        final String vTag = v3;
                        var vBadge = com.oupao.wiz.PgGrid.lastBadge;
                        final android.view.View vTouch = (vBadge != null) ? vBadge : v0_14;
                        // 自绘拖动（PinDrag）：拖动只跟手、松手才换位，不用系统 drag-and-drop。
                        // 触摸流可能落在 cell 上，也可能落在 badge 上（badge 可点时它才是持有者），
                        // 两边都绑一次，保证哪儿按下去都能拖。
                        final android.view.View vCell = v0_14;
                        final com.oupao.wiz.PinDrag.Commit vCommit = new com.oupao.wiz.PinDrag.Commit() {
                            public void onMove(String from, String to) {
                                com.oupao.wiz.PinDrop.applyOrder(from, to, vCell);
                            }
                        };
                        com.oupao.wiz.PinDrag.attach(v8, vCell, vCell, vNeedMgr, vCommit);
                        if ((vBadge != null) && (vBadge != vCell)) {
                            com.oupao.wiz.PinDrag.attach(v8, vBadge, vCell, vNeedMgr, vCommit);
                        }
                    }
                    continue;
                } else {
                }
            }
            com.oupao.wiz.PgGrid.reset();
            if ((p15 == 1) && (p13 == com.oupao.wiz.MainActivity.pgPins)) {
                com.oupao.wiz.Pins.card = v8;
            }
            p13.addView(v8);
        }
        return;
    }
    private static void compactTitle(android.content.Context p6, android.widget.LinearLayout p7, String p8)
    {
        android.widget.TextView v0_1 = null;
        try {
            v0_1 = com.oupao.wiz.Ui.t(p6, p8, 14.0f, -8750470, false);
            com.oupao.wiz.Ui.pad(v0_1, 0, 4, 0, 4);
        } catch (Throwable __e_v3) {
            return;
        }
        if (p7.getChildCount() == 0) {
            p7.addView(v0_1);
        } else {
            var v1_3 = new android.widget.LinearLayout.LayoutParams(-2, -2);
            v1_3.topMargin = com.oupao.wiz.Ui.dp(p6, 14);
            p7.addView(v0_1, v1_3);
        }
        return;
    }
    public static String firstLetter(String p5)
    {
        // 原实现用 GBK 一级字库的拼音区间表推首字母，但 GBK 二级字库（0xD8A1 以上）
        // 是按部首/笔画排序的，不是拼音，所以像「哔」(0xDFD9)「浏」这类字全被兜到了 Z。
        // 现在改用 Collator(Locale.CHINA 的拼音排序) 做二分定位，对二级字也正确；GBK 表作为异常时的兜底。
        if ((p5 == null) || (p5.length() <= 0)) {
            return "A";
        }
        char v0_3 = p5.charAt(0);
        // ASCII 快路径
        if (v0_3 < 128) {
            if (((v0_3 >= 65) && (v0_3 <= 90)) || ((v0_3 >= 97) && (v0_3 <= 122))) {
                return String.valueOf(v0_3).toUpperCase();
            }
            return "#";
        }
        try {
            var cached = com.oupao.wiz.PinUI.cFirst.get(Character.valueOf(v0_3));
            if (cached != null) {
                return cached;
            }
        } catch (Throwable ignore) {
        }
        String v4_0 = "#";
        try {
            String one = String.valueOf(v0_3);
            if (com.oupao.wiz.PinUI.col() != null) {
                if (com.oupao.wiz.PinUI.col().compare(one, "\u963f") >= 0) {
                    v4_0 = "A";
                    int i = (com.oupao.wiz.PinUI.BOUND.length() - 1);
                    while (i >= 0) {
                        if (com.oupao.wiz.PinUI.col().compare(one,
                                String.valueOf(com.oupao.wiz.PinUI.BOUND.charAt(i))) >= 0) {
                            v4_0 = String.valueOf(com.oupao.wiz.PinUI.BOUND_L.charAt(i));
                            break;
                        }
                        i--;
                    }
                } else {
                    v4_0 = "#";
                }
            } else {
                v4_0 = com.oupao.wiz.PinUI.firstLetterGbk(p5);
            }
        } catch (Throwable ignore) {
            try {
                v4_0 = com.oupao.wiz.PinUI.firstLetterGbk(p5);
            } catch (Throwable ignore2) {
                v4_0 = "#";
            }
        }
        try {
            com.oupao.wiz.PinUI.cFirst.put(Character.valueOf(v0_3), v4_0);
        } catch (Throwable ignore) {
        }
        return v4_0;
    }

    /** 拼音初始字母的区间边界字（按拼音升序）与对应字母 */
    static final String BOUND = "\u963f\u516b\u64e6\u642d\u86fe\u53d1\u5676\u54c8\u51fb\u5580\u5783\u5988\u62ff\u54e6\u5564\u671f\u7136\u6492\u584c\u6316\u6614\u538b\u531d";
    static final String BOUND_L = "ABCDEFGHJKLMNOPQRSTWXYZ";
    private static java.text.Collator coll;
    private static final java.util.HashMap<Character, String> cFirst = new java.util.HashMap<Character, String>();

    static java.text.Collator col()
    {
        if (com.oupao.wiz.PinUI.coll == null) {
            try {
                var c = java.text.Collator.getInstance(java.util.Locale.CHINA);
                c.setStrength(java.text.Collator.PRIMARY);
                com.oupao.wiz.PinUI.coll = c;
            } catch (Throwable t) {
                return null;
            }
        }
        return com.oupao.wiz.PinUI.coll;
    }

    /** 原 GBK 区间表实现（仅作兜底） */
    static String firstLetterGbk(String p5)
    {
        char v0_3 = p5.charAt(0);
        if (v0_3 < 19968) {
            if ((v0_3 < 65) || ((v0_3 > 90) && ((v0_3 < 97) || (v0_3 > 122)))) {
                return "#";
            }
            return p5.substring(0, 1).toUpperCase();
        }
        try {
            var b = p5.substring(0, 1).getBytes("GBK");
            int v = (((b[0] & 255) << 8) | (b[1] & 255));
            String[] th = new String[]{"54481:Z","53689:Y","52980:X","52698:W","52218:T","51446:S","51387:R","50906:Q"};
            for (int i = 0; i < th.length; i++) {
                int t = Integer.parseInt(th[i].substring(0, th[i].indexOf(':')));
                if (v >= t) {
                    return th[i].substring(th[i].indexOf(':') + 1);
                }
            }
        } catch (Throwable ignore) {
        }
        return "#";
    }
    private static String labelOf(android.content.Context p6, String p7)
    {
        // 工具条目：不在 Pins.apps() 里，直接取工具名
        try {
            if ((p7 != null) && p7.startsWith("tool#")) {
                String l = com.oupao.wiz.Tools.label(p7);
                if ((l != null) && (l.length() > 0)) {
                    return l;
                }
                return p7;
            }
        } catch (Throwable ignore) {
        }
        java.lang.String[] v0 = null;
        int v1 = 0;
        try {
            v0 = com.oupao.wiz.Pins.apps(p6);
            v1 = 0;
        } catch (Throwable __e_v5) {
            return p7;
        }
        while (v1 < v0.length) {
            var v2_1 = v0[v1];
            var v3_1 = v2_1.indexOf("\u0001");
            if ((v3_1 < 0) || (!v2_1.substring((v3_1 + 1)).equals(p7))) {
                v1++;
            } else {
                return v2_1.substring(0, v3_1);
            }
        }
        return p7;
    }
    public static String letterAt(int p2)
    {
        if (p2 != 0) {
            if (p2 == 27) {
                return "#";
            } else {
                return String.valueOf(((char) (64 + p2)));
            }
        } else {
            return "\u2605";
        }
    }
    public static android.view.View letterBar(android.content.Context p9, android.view.View p10)
    {
        var v0_1 = new android.widget.LinearLayout(p9);
        v0_1.setOrientation(1);
        v0_1.setGravity(16);
        int v1_3 = 0;
        while (v1_3 < 28) {
            java.lang.String v3 = null;
            if (v1_3 != 0) {
                if (v1_3 != 27) {
                    v3 = String.valueOf(((char) (64 + v1_3)));
                } else {
                    v3 = "#";
                }
            } else {
                v3 = "\u2605";
            }
            var v4_1 = new android.widget.TextView(p9);
            v4_1.setText(v3);
            v4_1.setTextSize(11.0f);
            v4_1.setTextColor(com.oupao.wiz.Ui.adaptText(-7303024));
            v4_1.setGravity(17);
            v4_1.setPadding(com.oupao.wiz.Ui.dp(p9, 5), 1, com.oupao.wiz.Ui.dp(p9, 5), 1);
            var v5_5 = com.oupao.wiz.PinUI.sl(p10, v1_3);
            if (v5_5 != null) {
                v4_1.setOnTouchListener(v5_5);
            }
            v4_1.setOnClickListener(new com.oupao.wiz.PinIdx(v3, p10));
            v0_1.addView(v4_1);
            v1_3++;
        }
        v0_1.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-2, -2));
        return v0_1;
    }
    public static java.util.List order(java.util.List p10)
    {
        java.util.ArrayList v0_1 = null;
        try {
            v0_1 = new java.util.ArrayList();
        } catch (Throwable __e_v9) {
            return p10;
        }
        if (p10 != null) {
            var v1 = com.oupao.wiz.Pins.listDesk();
            var v2 = v1.size();
            int v3 = 0;
            while (v3 < v2) {
                var v4_0 = v1.get(v3);
                if ((v4_0 instanceof String)) {
                    var v4_1 = ((String) v4_0);
                    int v5_1 = 0;
                    while (v5_1 < p10.size()) {
                        var v6_1 = p10.get(v5_1);
                        if ((v6_1 instanceof String)) {
                            var v6_2 = ((String) v6_1);
                            var v7_1 = com.oupao.wiz.PinUI.tagOf(v6_2);
                            if ((v7_1 != null) && (v4_1.equals(v7_1))) {
                                v0_1.add(v6_2);
                                p10.remove(v5_1);
                                break;
                            }
                        }
                        v5_1++;
                    }
                }
                v3++;
            }
            v0_1.addAll(p10);
        }
        return v0_1;
    }
    public static java.util.List pick(android.content.Context p8)
    {
        var v1_1 = new java.util.ArrayList();
        java.lang.String[] v0_1 = null;
        java.util.ArrayList v2 = null;
        int v3_1 = 0;
        try {
            v0_1 = com.oupao.wiz.Pins.apps(p8);
            v2 = com.oupao.wiz.Pins.listDesk();
            v3_1 = 0;
        } catch (Throwable __e_v7) {
            return v1_1;
        }
        while (v3_1 < v0_1.length) {
            var v4_1 = v0_1[v3_1];
            var v5_4 = v4_1.indexOf("\u0001");
            if ((v5_4 >= 0) && (v2.contains(v4_1.substring((v5_4 + 1))))) {
                v1_1.add(v4_1);
            }
            v3_1++;
        }
        var v0_0 = com.oupao.wiz.Tools.tags();
        var v3_0 = com.oupao.wiz.Tools.defs();
        int v4_0 = 0;
        while (v4_0 < v0_0.length) {
            if (v2.contains(v0_0[v4_0])) {
                v1_1.add(v3_0[v4_0]);
            }
            v4_0++;
        }
        return com.oupao.wiz.PinUI.order(v1_1);
    }
    public static void rail(android.content.Context p6, android.view.View p7)
    {
        android.view.View v0_2 = null;
        try {
            com.oupao.wiz.SLog.v("start");
            v0_2 = com.oupao.wiz.PinUI.railView;
        } catch (Throwable __e_v4_5) {
            var v3_4 = new StringBuilder();
            v3_4.append("ERR ");
            v3_4.append(__e_v4_5);
            com.oupao.wiz.SLog.v(v3_4.toString());
            return;
        }
        if (v0_2 != null) {
            var v1_2 = v0_2.getParent();
            if (v1_2 != null) {
                ((android.view.ViewGroup) v1_2).removeView(v0_2);
            }
        }
        if (p7 != null) {
            var v1_1 = com.oupao.wiz.PinUI.letterBar(p6, p7);
            if (v1_1 != null) {
                // p6 可能是 ContextThemeWrapper（页面容器的 getContext()），直接强转 Activity 会抛
                // ClassCastException，导致索引条根本加不上（更多应用页就是这么没的）。这里先逐层解包。
                android.content.Context vc = p6;
                int guard = 0;
                while ((!(vc instanceof android.app.Activity))
                        && (vc instanceof android.content.ContextWrapper) && (guard < 8)) {
                    vc = ((android.content.ContextWrapper) vc).getBaseContext();
                    guard++;
                }
                if (!(vc instanceof android.app.Activity)) {
                    com.oupao.wiz.SLog.v("rail: ctx 不是 Activity, 跳过");
                    return;
                }
                var v2_0 = ((android.app.Activity) vc).getWindow();
                if (v2_0 != null) {
                    var v2_1 = v2_0.getDecorView();
                    if (v2_1 != null) {
                        var v2_2 = v2_1.findViewById(16908290);
                        if (v2_2 != null) {
                            com.oupao.wiz.SLog.v("content ok");
                            var v3_2 = new android.widget.FrameLayout.LayoutParams(-2, -2);
                            v3_2.gravity = 21;
                            v3_2.rightMargin = 6;
                            ((android.view.ViewGroup) v2_2).addView(v1_1, v3_2);
                            com.oupao.wiz.PinUI.railView = v1_1;
                            com.oupao.wiz.SLog.v("added");
                        }
                    }
                }
            }
        }
        return;
    }
    public static void sectionTitle(android.content.Context p5, android.widget.LinearLayout p6, String p7)    {
        var v0_1 = com.oupao.wiz.Ui.t(p5, p7, 13.0f, -8750470, false);
        com.oupao.wiz.Ui.pad(v0_1, 16, 2, 16, 2);
        p6.addView(v0_1);
        return;
    }
    public static android.view.View.OnTouchListener sl(android.view.View p4, int p5)
    {
        com.oupao.wiz.PinSl v0_1 = null;
        try {
            v0_1 = new com.oupao.wiz.PinSl(p4, p5);
        } catch (Throwable __e_v1) {
            com.oupao.wiz.SLog.e(__e_v1.toString());
            v0_1 = null;
        }
        return v0_1;
    }
    public static String tagOf(String p6)
    {
        int v1 = 0;
        try {
            v1 = p6.indexOf(1);
        } catch (Throwable __e_v5) {
            return null;
        }
        if (v1 < 0) {
            return null;
        } else {
            var v2 = (v1 + 1);
            var v3 = p6.indexOf(1, v2);
            if (v3 < 0) {
                return p6.substring(v2);
            } else {
                return p6.substring(v2, v3);
            }
        }
    }
        public static void topBarMgr(android.content.Context p8, android.widget.LinearLayout p9, String p10)
    {
        java.lang.String v1_7 = null;
        android.widget.LinearLayout v0_1 = null;
        try {
            v0_1 = new android.widget.LinearLayout(p8);
            v0_1.setOrientation(0);
            v0_1.setGravity(16);
            // 3165：顶部留白改成**确定性**取值。
            // 原来直接用 MainActivity.sbH，而这个静态值在部分构建路径里还是 0，
            // 于是「标题 ↔ 已添加」的间距忽大忽小（切模式必现 —— 切模式会重建本页）。
            int vSb = com.oupao.wiz.MainActivity.sbH;
            if (vSb <= 0) {
                try {
                    int id = p8.getResources().getIdentifier("status_bar_height", "dimen", "android");
                    if (id > 0) vSb = p8.getResources().getDimensionPixelSize(id);
                } catch (Throwable ignore) {
                }
            }
            if (vSb < 0) vSb = 0;
            // 3168：**首次算出来后固定住**。
            // 现象：小窗模式里点一下「管理」触发重建，标题区留白就变了（再切回小窗才恢复）。
            // 说明不同重建路径拿到的 sbH/父容器内边距不一致；把它缓存成进程级常量，
            // 无论哪条路径重建都用同一个值，间距就恒定。
            if (topPadOnce < 0) topPadOnce = vSb;
            v0_1.setPadding(0, topPadOnce, 0, 0);
            vSb = topPadOnce;
            v0_1.addView(com.oupao.wiz.MainActivity.mkArrow(p8));
            var v1_2 = com.oupao.wiz.Ui.t(p8, p10, 22.0f, -14935012, true);
            com.oupao.wiz.Ui.pad(v1_2, 8, 4, 18, 4);
            // 3166 诊断：切模式后间距会变，先量出"标题行高度 / 容器顶部留白"到底差在哪
            final android.widget.LinearLayout fRow = v0_1;
            final int fSb = vSb;
            v0_1.post(new Runnable() {
                public void run() {
                    try {
                        int ph = 0;
                        if (fRow.getParent() instanceof android.view.View) {
                            ph = ((android.view.View) fRow.getParent()).getPaddingTop();
                        }
                        // 3167：本页跑在**模块 App 进程**里，写不了 /data/system/oupao.log，
                        // 这条诊断必须用 logcat（tag OupaoCfg）才看得到。
                        android.util.Log.i("OupaoCfg", "PinUI: topBar h=" + fRow.getHeight()
                                + " padTop=" + fRow.getPaddingTop() + " sb=" + fSb
                                + " parentPadTop=" + ph
                                + " appMgr=" + com.oupao.wiz.Pins.appMgr
                                + " mode=" + com.oupao.wiz.Cfg.moreMode());
                    } catch (Throwable ignore) {
                    }
                }
            });
            // 长按标题：切换「更多应用」的呼出方式（面板 / 小窗）。
            // 面板那边一直是长按标题切换；小窗模式（本页）以前没有，所以切不了。
            // 关掉 haptic，避免点旁边时误触发长按震动。
            v1_2.setHapticFeedbackEnabled(false);
            v1_2.setOnLongClickListener(new android.view.View.OnLongClickListener() {
                public boolean onLongClick(android.view.View v) {
                    try {
                        // ★ 不再读配置判断当前模式：Settings.Global 在进程内有缓存，
                        //   system_server 改完 App 这边可能还是旧值 → 「第一次长按不切换」。
                        //   本页就是「小窗模式」的界面，所以长按必然是切到面板。
                        String next = "panel";
                        com.oupao.wiz.Cfg.setMoreMode(next);
                        com.oupao.wiz.SLog.i("PinUI: \u547c\u51fa\u65b9\u5f0f -> " + next);
                        try {
                            var mi = new android.content.Intent("com.oupao.wiz.MODE");
                            mi.setPackage("android");
                            mi.putExtra("mode", next);
                            v.getContext().sendBroadcast(mi);
                        } catch (Throwable ignore) {
                        }
                        android.widget.Toast.makeText(v.getContext(),
                                "\u547c\u51fa\u65b9\u5f0f\uff1a" + ("window".equals(next) ? "\u5c0f\u7a97" : "\u9762\u677f"),
                                android.widget.Toast.LENGTH_SHORT).show();
                        // 模块界面里「更多应用方式」那一行就地刷新（不然要重开模块才变）
                        // 立刻切过去：切到「面板」就关掉当前小窗页，让 system_server 弹面板
                        if (!"window".equals(next)) {
                            try {
                                com.oupao.wiz.FlyH.openMore();
                            } catch (Throwable ignore) {
                            }
                            try {
                                var ctx2 = v.getContext();
                                if (ctx2 instanceof android.app.Activity) {
                                    ((android.app.Activity) ctx2).finish();
                                }
                            } catch (Throwable ignore) {
                            }
                        }
                        return true;
                    } catch (Throwable t) {
                        return false;
                    }
                }
            });
            v0_1.addView(v1_2);
            v0_1.addView(new android.view.View(p8), new android.widget.LinearLayout.LayoutParams(0, -1, 1.0f));
        } catch (Throwable __e_v0) {
            return;
        }
        if (com.oupao.wiz.Pins.appMgr) {
            v1_7 = "\u5b8c\u6210";
        } else {
            v1_7 = "\u7ba1\u7406";
        }
        var v1_9 = com.oupao.wiz.Ui.t(p8, v1_7, 15.0f, -13669377, false);
        com.oupao.wiz.Ui.pad(v1_9, 8, 4, 16, 4);
        v1_9.setOnClickListener(new com.oupao.wiz.PinMgr());
        v0_1.addView(v1_9);
        p9.addView(v0_1);
        return;
    }
}
