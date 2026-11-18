package com.oupao.wiz;

/**
 * 音乐应用「自定义包名」的应用选择器。
 *
 * 原来只能手输包名（com.xxx.yyy），很难记。这里直接列出可启动应用，点一下即选中。
 * 保留「手动输入」作为兜底（老流程，弹 EditText）。
 *
 * 注意：弹窗必须使用**带窗口 token 的 Context**（Activity），
 *      不能用 Cfg.ctx（应用级），否则 Dialog.show() 抛 BadTokenException。
 */
public class MusPick {

    public static void show(final android.content.Context ctx) {
        if (ctx == null) {
            return;
        }
        try {
            final android.content.pm.PackageManager pm = ctx.getPackageManager();

            // 解析 Pins.apps() 的 "label\u0001pkg#activity"，按包名去重
            final java.util.ArrayList<Object[]> items = new java.util.ArrayList<Object[]>();
            java.util.HashSet<String> seen = new java.util.HashSet<String>();
            String[] arr = com.oupao.wiz.Pins.apps(ctx);
            if (arr != null) {
                for (int i = 0; i < arr.length; i++) {
                    String s = arr[i];
                    if (s == null) {
                        continue;
                    }
                    int c = s.indexOf('\u0001');
                    if (c <= 0) {
                        continue;
                    }
                    String label = s.substring(0, c);
                    String rest = s.substring(c + 1);
                    int h = rest.indexOf('#');
                    String pkg = (h > 0) ? rest.substring(0, h) : rest;
                    if ((pkg.length() == 0) || (!seen.add(pkg))) {
                        continue;
                    }
                    items.add(new Object[]{label, pkg});
                }
            }
            // 按显示名排序（中文按拼音）
            try {
                final java.text.Collator col = java.text.Collator.getInstance(java.util.Locale.CHINA);
                java.util.Collections.sort(items, new java.util.Comparator<Object[]>() {
                    public int compare(Object[] a, Object[] b) {
                        return col.compare(String.valueOf(a[0]), String.valueOf(b[0]));
                    }
                });
            } catch (Throwable ignore) {
            }

            final android.widget.BaseAdapter ad = new android.widget.BaseAdapter() {
                public int getCount() {
                    return items.size();
                }

                public Object getItem(int i) {
                    return items.get(i);
                }

                public long getItemId(int i) {
                    return i;
                }

                public android.view.View getView(int pos, android.view.View cv, android.view.ViewGroup parent) {
                    android.widget.LinearLayout row;
                    if (cv instanceof android.widget.LinearLayout) {
                        row = (android.widget.LinearLayout) cv;
                    } else {
                        row = new android.widget.LinearLayout(ctx);
                        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                        int pad = com.oupao.wiz.Ui.dp(ctx, 10);
                        row.setPadding(pad, pad, pad, pad);

                        android.widget.ImageView iv = new android.widget.ImageView(ctx);
                        iv.setId(1001);
                        row.addView(iv, new android.widget.LinearLayout.LayoutParams(
                                com.oupao.wiz.Ui.dp(ctx, 34), com.oupao.wiz.Ui.dp(ctx, 34)));

                        android.widget.TextView tv = new android.widget.TextView(ctx);
                        tv.setId(1002);
                        tv.setTextSize(14.0f);
                        tv.setSingleLine(true);
                        android.widget.LinearLayout.LayoutParams lp =
                                new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f);
                        lp.leftMargin = com.oupao.wiz.Ui.dp(ctx, 12);
                        row.addView(tv, lp);

                        android.widget.TextView tk = new android.widget.TextView(ctx);
                        tk.setId(1003);
                        tk.setTextSize(10.0f);
                        tk.setTextColor(0xFF8A8A8A);
                        tk.setSingleLine(true);
                        row.addView(tk, new android.widget.LinearLayout.LayoutParams(-2, -2));
                    }
                    Object[] it = items.get(pos);
                    String label = String.valueOf(it[0]);
                    String pkg = String.valueOf(it[1]);
                    try {
                        ((android.widget.TextView) row.findViewById(1002)).setText(label);
                        ((android.widget.TextView) row.findViewById(1003)).setText(pkg);
                        ((android.widget.ImageView) row.findViewById(1001))
                                .setImageDrawable(pm.getApplicationIcon(pkg));
                    } catch (Throwable ignore) {
                    }
                    return row;
                }
            };

            new android.app.AlertDialog.Builder(ctx)
                    .setTitle("\u9009\u62e9\u97f3\u4e50\u5e94\u7528")
                    .setAdapter(ad, new android.content.DialogInterface.OnClickListener() {
                        public void onClick(android.content.DialogInterface d, int which) {
                            try {
                                Object[] it = items.get(which);
                                apply(String.valueOf(it[1]));
                            } catch (Throwable t) {
                                com.oupao.wiz.SLog.e("MusPick: \u4fdd\u5b58\u5931\u8d25", t);
                            }
                            try {
                                d.dismiss();
                            } catch (Throwable ignore) {
                            }
                            // 对话框关了再刷新音乐页（与原版 MA$ME 一致）
                            try {
                                com.oupao.wiz.MainActivity.reMusic();
                            } catch (Throwable ignore) {
                            }
                        }
                    })
                    .setNeutralButton("\u624b\u52a8\u8f93\u5165", new android.content.DialogInterface.OnClickListener() {
                        public void onClick(android.content.DialogInterface d, int which) {
                            try {
                                d.dismiss();
                            } catch (Throwable ignore) {
                            }
                            manual(ctx);
                        }
                    })
                    .setNegativeButton("\u53d6\u6d88", null)
                    .show();
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("MusPick: show \u5f02\u5e38", t);
        }
    }

    /** 老流程：手输包名 */
    private static void manual(android.content.Context ctx) {
        try {
            var et = new android.widget.EditText(ctx);
            et.setHint("com.example.music");
            et.setSingleLine(true);
            new android.app.AlertDialog.Builder(ctx)
                    .setTitle("\u81ea\u5b9a\u4e49\u97f3\u4e50\u5305\u540d")
                    .setView(et)
                    .setPositiveButton("\u6dfb\u52a0", new com.oupao.wiz.MA$ME(et, null))
                    .setNegativeButton("\u53d6\u6d88", null)
                    .show();
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("MusPick: manual \u5f02\u5e38", t);
        }
    }

    /** 把 pkg 从 oupao_music_hidden 里剔除（重新添加时用） */
    private static void unHide(String pkg) {
        try {
            // 两个键都要清：内置隐藏 + 手动添加后被移除的
            String[] keys = new String[]{"oupao_music_hidden", "oupao_music_custom_hid"};
            for (int k = 0; k < keys.length; k++) {
                String key = keys[k];
                String hid = com.oupao.wiz.Cfg.getStr(key, "");
                if ((hid == null) || (hid.length() == 0)
                        || (!com.oupao.wiz.MusTxt.has(hid, pkg))) {
                    continue;
                }
                String sb = com.oupao.wiz.MusTxt.remove(hid, pkg);
                if (sb.length() == 0) {
                    // 删干净：必须走 clearKey（同时清两侧文件和 cS 缓存）
                    com.oupao.wiz.Cfg.clearKey(key);
                } else {
                    com.oupao.wiz.Cfg.saveStr(key, sb);
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("MusPick: unHide 异常", t);
        }
    }

    /** 写入配置并刷新音乐页 */
    private static void apply(String pkg) {
        if ((pkg == null) || (pkg.length() == 0)) {
            return;
        }
        String cur = com.oupao.wiz.Cfg.getStr("oupao_music_custom", "");
        if (cur == null) {
            cur = "";
        }
        // 行级精确匹配：contains 是子串匹配，会误判「已存在」导致添加无效
        if (!com.oupao.wiz.MusTxt.has(cur, pkg)) {
            com.oupao.wiz.Cfg.saveStr("oupao_music_custom",
                    com.oupao.wiz.MusTxt.add(cur, pkg));
        }
        // 从 hidden 里去掉：否则刚添加的应用会因为之前被隐藏而看不见
        unHide(pkg);
        com.oupao.wiz.Cfg.saveStr("oupao_music_pkg", pkg);
        com.oupao.wiz.Cfg.push();
        // 不在这里调 reMusic：对话框还开着时重建页面会让当前页看起来像退回上一页。
        // 改成 dismiss 之后再调（与原版 MA$ME 的顺序一致）。
        com.oupao.wiz.SLog.i("MusPick: 已选择 " + pkg);
    }
}
