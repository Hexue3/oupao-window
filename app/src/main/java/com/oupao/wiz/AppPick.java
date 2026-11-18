package com.oupao.wiz;

/**
 * 「生效应用」名单页的内容构建器（**模块内普通子页**，不是弹窗）。
 *
 * 页面外壳（← 返回 + 标题 + 卡片）由 MainActivity 的 page 6 提供，
 * 这里只负责往卡片里铺「应用行」，样式与模块其它页的行保持一致：
 * 左右 20dp、上下 10dp、图标 24dp、名称 16sp、右侧开关。
 *
 * 语义：**名单为空 = 全部应用生效**。在「全部」状态下关掉某个应用时，
 * 会把它展开成「全部 - 该应用」再落盘，行为等价于官方那种「每个应用一个开关」。
 */
public class AppPick {

    /** 入口行/标题用：把当前状态写出来 */
    public static String label(String title, String key) {
        java.util.List<String> l = com.oupao.wiz.Cfg.pkgList(key);
        return title + (l.isEmpty() ? "\uff08\u5168\u90e8\u5e94\u7528\uff09" : "\uff08" + l.size() + " \u4e2a\uff09");
    }

    /** 当前状态文案 */
    public static String summary(String key) {
        java.util.List<String> l = com.oupao.wiz.Cfg.pkgList(key);
        return l.isEmpty() ? "\u5f53\u524d\uff1a\u672a\u9009\u62e9\uff08= \u5168\u90e8\u5e94\u7528\u751f\u6548\uff09" : "\u5f53\u524d\uff1a\u5df2\u9009 " + l.size() + " \u4e2a\u5e94\u7528";
    }

    /**
     * 把名单铺进卡片。每次进入该页都会重建一次（应用列表不常变，够用）。
     *
     * @param search    搜索框（可空）。非空时会把过滤逻辑挂上去，输入即筛选。
     * @param onChanged 任何改动后的回调（可空），用来刷新标题/摘要
     */
    public static void buildList(final android.content.Context c,
                                 final android.widget.LinearLayout card,
                                 final String key,
                                 final android.widget.EditText search,
                                 final Runnable onChanged) {
        if ((c == null) || (card == null)) {
            return;
        }
        try {
            card.removeAllViews();
            final int dp = com.oupao.wiz.Ui.dp(c, 1);
            final java.util.List<String> sel = com.oupao.wiz.Cfg.pkgList(key);
            final java.util.List<Object[]> apps = appNames(c);

            final java.util.List<String> allPkgs = new java.util.ArrayList<String>();
            for (int i = 0; i < apps.size(); i++) {
                allPkgs.add(String.valueOf(apps.get(i)[1]));
            }

            // ---------- 顶部：当前状态 + 恢复全部 ----------
            android.widget.LinearLayout head = new android.widget.LinearLayout(c);
            head.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            head.setGravity(16);
            head.setPadding(dp * 20, dp * 12, dp * 20, dp * 12);

            final android.widget.TextView sum = com.oupao.wiz.Ui.t(c, summary(key), 13.0f,
                    com.oupao.wiz.Ui.textSub(), false);
            sum.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
            head.addView(sum);

            android.widget.TextView reset = com.oupao.wiz.Ui.t(c, "\u6062\u590d\u5168\u90e8", 14.0f,
                    com.oupao.wiz.Ui.textMain(), false);
            com.oupao.wiz.Ui.ripple(reset);
            reset.setOnClickListener(new android.view.View.OnClickListener() {
                public void onClick(android.view.View v) {
                    try {
                        com.oupao.wiz.Cfg.saveList(key, new java.util.ArrayList<String>());
                        sel.clear();
                        sum.setText(summary(key));
                        // 开关全部回到「开」
                        for (int k = 0; k < card.getChildCount(); k++) {
                            android.view.View r = card.getChildAt(k);
                            android.widget.Switch sw = findSwitch(r);
                            if (sw != null) {
                                Object tag = sw.getTag();
                                if (tag == null) {
                                    continue;
                                }
                                sw.setOnCheckedChangeListener(null);
                                sw.setChecked(false);
                                sw.setOnCheckedChangeListener(
                                        listenerFor(key, allPkgs, sel, tag.toString(), sum, onChanged));
                            }
                        }
                        if (onChanged != null) {
                            onChanged.run();
                        }
                    } catch (Throwable ignored) {
                    }
                }
            });
            head.addView(reset);
            card.addView(head);

            // ---------- 应用行 ----------
            final java.util.List<Object[]> rows = new java.util.ArrayList<Object[]>();
            for (int i = 0; i < apps.size(); i++) {
                Object[] it = apps.get(i);
                final String pkg = String.valueOf(it[1]);
                final String label = String.valueOf(it[0]);

                android.widget.LinearLayout row = new android.widget.LinearLayout(c);
                row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                row.setGravity(16);
                row.setPadding(dp * 20, dp * 10, dp * 20, dp * 10);
                com.oupao.wiz.Ui.ripple(row);

                android.widget.ImageView ic = new android.widget.ImageView(c);
                ic.setLayoutParams(new android.widget.LinearLayout.LayoutParams(dp * 24, dp * 24));
                try {
                    ic.setImageDrawable(c.getPackageManager().getApplicationIcon(pkg));
                } catch (Throwable ignored) {
                }
                row.addView(ic);

                android.widget.TextView tv = com.oupao.wiz.Ui.t(c, label, 16.0f,
                        com.oupao.wiz.Ui.textMain(), false);
                android.widget.LinearLayout.LayoutParams tp =
                        new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
                tp.leftMargin = dp * 14;
                tv.setLayoutParams(tp);
                row.addView(tv);

                android.widget.Switch sw = new android.widget.Switch(c);
                sw.setChecked(sel.contains(pkg));
                // 与其它页保持一致的主题色（Ui.row / mkMaster 用的就是这两个值）
                sw.setThumbTintList(com.oupao.wiz.Ui.csl(-13335809, -1));
                sw.setTrackTintList(com.oupao.wiz.Ui.csl(1026851583, -1447445));
                sw.setTag(pkg);
                sw.setOnCheckedChangeListener(listenerFor(key, allPkgs, sel, pkg, sum, onChanged));
                row.addView(sw);

                // 整行可点（与官方一致）
                row.setOnClickListener(new android.view.View.OnClickListener() {
                    public void onClick(android.view.View v) {
                        android.widget.Switch s = findSwitch(v);
                        if (s != null) {
                            s.setChecked(!s.isChecked());
                        }
                    }
                });

                card.addView(row);
                rows.add(new Object[]{label, pkg, row});
            }

            // ---------- 搜索过滤 ----------
            if (search != null) {
                final android.text.TextWatcher watcher = new android.text.TextWatcher() {
                    public void afterTextChanged(android.text.Editable e) {
                    }

                    public void beforeTextChanged(CharSequence s, int a, int b, int cc) {
                    }

                    public void onTextChanged(CharSequence s, int a, int b, int cc) {
                        String q = (s == null) ? "" : s.toString().trim().toLowerCase();
                        for (int k = 0; k < rows.size(); k++) {
                            Object[] r = rows.get(k);
                            boolean hit = q.isEmpty()
                                    || String.valueOf(r[0]).toLowerCase().contains(q)
                                    || String.valueOf(r[1]).toLowerCase().contains(q);
                            ((android.view.View) r[2]).setVisibility(hit ? 0 : 8);
                        }
                    }
                };
                // 先摘掉上一次挂的（重复进页会重建，避免叠加）
                android.text.TextWatcher old = null;
                Object tag = search.getTag();
                if (tag instanceof android.text.TextWatcher) {
                    old = (android.text.TextWatcher) tag;
                }
                if (old != null) {
                    search.removeTextChangedListener(old);
                }
                search.setTag(watcher);
                search.addTextChangedListener(watcher);
                // 立即按当前输入过滤一次
                String q = search.getText() == null ? "" : search.getText().toString().trim().toLowerCase();
                for (int k = 0; k < rows.size(); k++) {
                    Object[] r = rows.get(k);
                    boolean hit = q.isEmpty()
                            || String.valueOf(r[0]).toLowerCase().contains(q)
                            || String.valueOf(r[1]).toLowerCase().contains(q);
                    ((android.view.View) r[2]).setVisibility(hit ? 0 : 8);
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("AppPick: 构建失败", t);
        }
    }

    private static android.widget.CompoundButton.OnCheckedChangeListener listenerFor(
            final String key, final java.util.List<String> allPkgs,
            final java.util.List<String> sel, final String pkg,
            final android.widget.TextView sum, final Runnable onChanged) {
        return new android.widget.CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(android.widget.CompoundButton cb, boolean on) {
                setOne(key, allPkgs, sel, pkg, on);
                if (sum != null) {
                    sum.setText(summary(key));
                }
                if (onChanged != null) {
                    onChanged.run();
                }
            }
        };
    }

    /**
     * 写单个应用的状态。
     *
     * 白名单语义：开关 = 是否选中。空名单仍表示「不限制 = 全部生效」，
     * 但界面上默认全是关的（用户要求「默认全部关闭，要哪个开哪个」），
     * 所以这里只做最朴素的增删。
     */
    private static void setOne(String key, java.util.List<String> allPkgs,
                               java.util.List<String> sel, String pkg, boolean on) {
        try {
            if (on) {
                if (!sel.contains(pkg)) {
                    sel.add(pkg);
                }
            } else {
                sel.remove(pkg);
            }
            com.oupao.wiz.Cfg.saveList(key, sel);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("AppPick: 保存失败", t);
        }
    }

    private static android.widget.Switch findSwitch(android.view.View row) {
        if (!(row instanceof android.view.ViewGroup)) {
            return null;
        }
        android.view.ViewGroup g = (android.view.ViewGroup) row;
        for (int i = 0; i < g.getChildCount(); i++) {
            android.view.View ch = g.getChildAt(i);
            if (ch instanceof android.widget.Switch) {
                return (android.widget.Switch) ch;
            }
        }
        return null;
    }

    /** 返回 {label, pkg} 列表，只取有启动入口的应用，按名称排序 */
    private static java.util.List<Object[]> appNames(android.content.Context c) {
        java.util.ArrayList<Object[]> out = new java.util.ArrayList<Object[]>();
        try {
            android.content.pm.PackageManager pm = c.getPackageManager();
            android.content.Intent main = new android.content.Intent(android.content.Intent.ACTION_MAIN);
            main.addCategory(android.content.Intent.CATEGORY_LAUNCHER);
            java.util.List<android.content.pm.ResolveInfo> ris = pm.queryIntentActivities(main, 0);
            java.util.HashSet<String> seen = new java.util.HashSet<String>();
            for (int i = 0; i < ris.size(); i++) {
                android.content.pm.ResolveInfo ri = ris.get(i);
                if ((ri == null) || (ri.activityInfo == null)) {
                    continue;
                }
                String pkg = ri.activityInfo.packageName;
                if ((pkg == null) || (seen.contains(pkg))) {
                    continue;
                }
                // 模块自己也列出来：它的「测试通知」可以用来做功能自测（TestNotif）
                seen.add(pkg);
                String label = String.valueOf(ri.loadLabel(pm));
                out.add(new Object[]{label, pkg});
            }
            java.util.Collections.sort(out, new java.util.Comparator<Object[]>() {
                public int compare(Object[] a, Object[] b) {
                    return String.valueOf(a[0]).compareTo(String.valueOf(b[0]));
                }
            });
        } catch (Throwable ignored) {
        }
        return out;
    }
}
