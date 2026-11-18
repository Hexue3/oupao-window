package com.oupao.wiz;
public class MainActivity extends androidx.activity.ComponentActivity {
    public static android.view.View[] aFrom;
    public static int aNext;
    public static boolean aOn;
    public static Runnable aPend;
    public static android.view.View[] aTo;
    public static float aW;
    static Object backCb;
    public static android.view.View bg0;
    public static android.view.View bg1;
    public static android.view.View bg10;
    public static android.view.View bg11;
    public static android.view.View bg12;
    public static android.view.View bg13;
    public static android.view.View bg2;
    public static android.view.View bg3;
    public static android.view.View bg4;
    public static android.view.View bg5;
    public static android.view.View bg6;
    public static android.view.View bg7;
    public static android.view.View bg8;
    public static android.view.View bg9;
    public static android.view.View[] bgList;
    public static int cur;
    public static boolean deskOn;
    public static String deskPins;
    static boolean diagRx;
    public static com.oupao.wiz.MainActivity inst;
    public static android.widget.LinearLayout logCard;
    public static int navReq;
    public static android.widget.FrameLayout pageBox;
    public static android.widget.LinearLayout pageLog;
    public static LogCenter logCenter;
    public static android.widget.LinearLayout pageSug;
    public static android.widget.LinearLayout pg0;
    public static android.widget.LinearLayout pg3;
    public static android.widget.LinearLayout pg7;
    public static android.view.View[] pgList;
    public static android.widget.LinearLayout pgPins;
    public static android.widget.ScrollView[] pgSv;
    public static android.widget.TextView posBubble;
    public static android.widget.FrameLayout posOverlay;
    /** 位置编辑器是否打开着：切方向会旋转重建界面，重建后要自动把编辑器再打开 */
    public static boolean posEditWanted;
    /** 为编辑另一个方向而强制请求的屏幕方向（0=横屏 1=竖屏）；-1 = 没锁 */
    public static int posReqOrientation = -1;
    /** 打开编辑器时所在的页面：切方向会 recreate，重建后要切回这一页 */
    public static int posPage = -1;
    /** 退出编辑器时会放开方向锁，可能再触发一次 recreate；重建后要回到这一页（一次性） */
    public static int retPage = -1;
    public static android.graphics.drawable.Drawable rdDraw;
    public static android.widget.LinearLayout sAic;
    public static android.widget.LinearLayout sDiag;
    public static android.widget.LinearLayout sGame;
    public static android.widget.TextView sK;
    public static android.widget.TextView sL;
    public static android.widget.LinearLayout sLog;
    public static android.widget.LinearLayout sM;
    public static android.widget.LinearLayout sMsg;
    public static String[] sMus;
    public static android.widget.LinearLayout sQ;
    public static android.widget.TextView sSub;
    public static android.widget.ScrollView sSv;
    public static android.widget.TextView sTitle;
    public static android.view.View sTool;
    public static android.widget.TextView sU;
    public static android.widget.LinearLayout sVib;
    public static int sbH;
    public static android.view.View scrim;
    public static android.widget.LinearLayout sugCardQq;
    public static android.widget.LinearLayout sugCardWx;
    // 「生效应用」名单页（复用 page 6 这个子页槽位；sugFrom: 0=气泡 1=屏蔽横幅）
    public static android.widget.TextView pgSugTitle;
    public static android.widget.LinearLayout sugAppCard;
    public static android.widget.EditText sugSearch;
    public static int sugFrom;
    public static boolean sugFromList;
    public static int[] svPos;
    private android.widget.LinearLayout root;
    public MainActivity()
    {
        return;
    }
    /** 给「临时开启屏幕常亮」那一行加 › 箭头，点行进第 7 页（屏幕常亮页）。
     *  原 smali 取 getChildAt(3) 且要求 childCount>=4，但调用时卡片只有 2 行（手电筒、屏幕常亮），
     *  判据永远不成立 → 箭头加不上、第 7 页进不去。这里改成按行文本定位。 */
    public static void addKeepArrow(android.widget.LinearLayout p8, android.content.Context p9)
    {
        try {
            int v0 = p8.getChildCount();
            int v1 = 0;
            while (v1 < v0) {
                var v2 = p8.getChildAt(v1);
                v1 = v1 + 1;
                if (!(v2 instanceof android.widget.LinearLayout)) {
                    continue;
                }
                var v3 = ((android.widget.LinearLayout) v2);
                if (v3.getChildCount() < 2) {
                    continue;
                }
                var v4 = v3.getChildAt(0);
                if (v4 instanceof android.widget.LinearLayout) {
                    var v4g = ((android.widget.LinearLayout) v4);
                    if (v4g.getChildCount() < 1) {
                        continue;
                    }
                    v4 = v4g.getChildAt(0);
                }
                if (!(v4 instanceof android.widget.TextView)) {
                    continue;
                }
                var v5 = ((android.widget.TextView) v4).getText();
                if (v5 == null || !v5.toString().startsWith("\u4e34\u65f6\u5f00\u542f\u5c4f\u5e55\u5e38\u4eae")) {
                    continue;
                }
                var v6 = com.oupao.wiz.Ui.t(p9, "\u203a", 26.0f, com.oupao.wiz.Ui.textSub(), true);
                v6.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-2, -2));
                v3.addView(v6, (v3.getChildCount() - 1));
                v3.setOnClickListener(new com.oupao.wiz.MA$PH(7));
                return;
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    /**
     * 纯导航行（无开关）：标题 + 说明，右侧箭头由 [addNavArrow] 补上。
     * 「生效应用」这类入口用它，避免 Ui.row 顺手绑一个不存在的配置键。
     */
    public static void mkNavRow(android.widget.LinearLayout p9, String p11)
    {
        try {
            var v0_1 = p9.getContext();
            var v1_1 = new android.widget.LinearLayout(v0_1);
            v1_1.setOrientation(0);
            v1_1.setGravity(16);
            v1_1.setPadding(com.oupao.wiz.Ui.dp(v0_1, 20), com.oupao.wiz.Ui.dp(v0_1, 14),
                    com.oupao.wiz.Ui.dp(v0_1, 20), com.oupao.wiz.Ui.dp(v0_1, 14));
            com.oupao.wiz.Ui.ripple(v1_1);
            int v2_1 = p11.indexOf(124);
            android.view.View v3_1;
            if (v2_1 >= 0) {
                v3_1 = com.oupao.wiz.Ui.two(v0_1, p11.substring(0, v2_1), p11.substring(v2_1 + 1));
            } else {
                v3_1 = com.oupao.wiz.Ui.t(v0_1, p11, 17.0f, com.oupao.wiz.Ui.textMain(), true);
            }
            v3_1.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
            v1_1.addView(v3_1);
            // 占位尾元素：addNavArrow 会把「›」插在最后一个子视图之前，
            // 没有它箭头会跑到标题左边。
            var v4_1 = new android.view.View(v0_1);
            v4_1.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, 1));
            v1_1.addView(v4_1);
            p9.addView(v1_1);
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static void addNavArrow(android.widget.LinearLayout p8, android.content.Context p9, int p10)
    {
        int v0_1 = 0;
        try {
            v0_1 = (p8.getChildCount() - 1);
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_1 >= 0) {
            var v0_2 = p8.getChildAt(v0_1);
            if ((v0_2 instanceof android.widget.LinearLayout)) {
                var v0_3 = ((android.widget.LinearLayout) v0_2);
                var v1_1 = (v0_3.getChildCount() - 1);
                if (v1_1 >= 0) {
                    v0_3.addView(com.oupao.wiz.Ui.t(p9, "\u203a", 26.0f, com.oupao.wiz.Ui.textSub(), true), v1_1);
                    v0_3.setOnClickListener(new com.oupao.wiz.MA$PG(p10));
                }
            }
        }
        return;
    }
    public static void animCancel()
    {
        try {
            com.oupao.wiz.MainActivity.logSt("animEnd");
            com.oupao.wiz.MainActivity.toolSync(com.oupao.wiz.MainActivity.cur);
        } catch (Throwable __e_v0) {
            return;
        }
        if (com.oupao.wiz.MainActivity.aOn) {
            com.oupao.wiz.MainActivity.aOn = false;
            var v0_4 = com.oupao.wiz.MainActivity.aFrom;
            var v1 = com.oupao.wiz.MainActivity.aTo;
            if ((v0_4 != null) && (v1 != null)) {
                com.oupao.wiz.MainActivity.fadeTo(v0_4, 0, 1.0f);
                com.oupao.wiz.MainActivity.fadeTo(v1, 0, 1.0f);
                com.oupao.wiz.MainActivity.scrimTo(0);
                com.oupao.wiz.MainActivity.aNext = com.oupao.wiz.MainActivity.cur;
                var v2_4 = new android.os.Handler(android.os.Looper.getMainLooper());
                var v3_5 = new com.oupao.wiz.MA$BC();
                v2_4.postDelayed(v3_5, 700);
                com.oupao.wiz.MainActivity.aPend = v3_5;
            }
        }
        return;
    }
    public static void animClean()
    {
        try {
            com.oupao.wiz.MainActivity.aOn = false;
            com.oupao.wiz.MainActivity.show(com.oupao.wiz.MainActivity.cur);
            com.oupao.wiz.MainActivity.settleTf(com.oupao.wiz.MainActivity.aFrom);
            com.oupao.wiz.MainActivity.settleTf(com.oupao.wiz.MainActivity.aTo);
            com.oupao.wiz.MainActivity.aPend = null;
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static void animEnd()
    {
        try {
            if (!com.oupao.wiz.MainActivity.posOn()) {
                if (!com.oupao.wiz.MainActivity.deskOn) {
                    if (!com.oupao.wiz.MainActivity.aOn) {
                        com.oupao.wiz.MainActivity.backAnim();
                    } else {
                        com.oupao.wiz.MainActivity.aOn = false;
                        var v0_4 = com.oupao.wiz.MainActivity.aFrom;
                        var v1_0 = com.oupao.wiz.MainActivity.aTo;
                        if ((v0_4 == null) || (v1_0 == null)) {
                        } else {
                            com.oupao.wiz.MainActivity.fadeTo(v0_4, com.oupao.wiz.MainActivity.aW, 1.0f);
                            com.oupao.wiz.MainActivity.fadeTo(v1_0, 0, 1.0f);
                            com.oupao.wiz.MainActivity.scrimTo(0);
                            var v2_2 = new android.os.Handler(android.os.Looper.getMainLooper());
                            var v3_5 = new com.oupao.wiz.MA$BN();
                            v2_2.postDelayed(v3_5, 700);
                            com.oupao.wiz.MainActivity.aPend = v3_5;
                        }
                    }
                    return;
                } else {
                    com.oupao.wiz.MainActivity.aOn = false;
                    com.oupao.wiz.MainActivity.killPend();
                    com.oupao.wiz.MainActivity.finishNow();
                    return;
                }
            } else {
                com.oupao.wiz.MainActivity.closePosEditor();
                return;
            }
        } catch (Throwable __e_v0) {
            com.oupao.wiz.MainActivity.backAnim();
            return;
        }
    }
    public static void animProg(android.window.BackEvent p2)
    {
        try {
            com.oupao.wiz.MainActivity.animProgF(p2.getProgress());
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static void animProgF(float p7)
    {
        try {
            if (!com.oupao.wiz.MainActivity.posOn()) {
                if (!com.oupao.wiz.MainActivity.deskOn) {
                    if (com.oupao.wiz.MainActivity.aOn) {
                        com.oupao.wiz.MainActivity.killPend();
                        var v2 = com.oupao.wiz.MainActivity.aFrom;
                        if ((v2 != null) && (com.oupao.wiz.MainActivity.aTo != null)) {
                            com.oupao.wiz.MainActivity.setTx(v2, (p7 * com.oupao.wiz.MainActivity.aW));
                            com.oupao.wiz.MainActivity.setAl(v2, 1.0f);
                            com.oupao.wiz.MainActivity.scrim.setAlpha(((1.0f - p7) * 0.3f));
                        }
                    }
                    return;
                } else {
                    com.oupao.wiz.MainActivity.aOn = false;
                    com.oupao.wiz.MainActivity.killPend();
                    com.oupao.wiz.MainActivity.finishNow();
                    return;
                }
            } else {
                return;
            }
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static void animStart(android.window.BackEvent p7)
    {
        try {
            if (!com.oupao.wiz.MainActivity.posOn()) {
                if (com.oupao.wiz.MainActivity.sSv != null) {
                    com.oupao.wiz.MainActivity.saveScroll(com.oupao.wiz.MainActivity.cur);
                }
                com.oupao.wiz.MainActivity.navEnd();
                com.oupao.wiz.MainActivity.killPend();
                com.oupao.wiz.MainActivity.aOn = false;
                var v0_1 = com.oupao.wiz.MainActivity.cur;
                var v1_0 = com.oupao.wiz.MainActivity.backTarget(v0_1);
                if (v1_0 >= 0) {
                    var v2_0 = com.oupao.wiz.MainActivity.viewsOf(v0_1);
                    var v3 = com.oupao.wiz.MainActivity.viewsOf(v1_0);
                    com.oupao.wiz.MainActivity.aFrom = v2_0;
                    com.oupao.wiz.MainActivity.aTo = v3;
                    com.oupao.wiz.MainActivity.aNext = v1_0;
                    com.oupao.wiz.MainActivity.logSt("animStart");
                    com.oupao.wiz.MainActivity.aW = (((float) com.oupao.wiz.Cfg.ctx.getResources().getDisplayMetrics().widthPixels) * 1.0f);
                    com.oupao.wiz.MainActivity.setVs(v3, 0);
                    com.oupao.wiz.MainActivity.setTx(v3, 0);
                    com.oupao.wiz.MainActivity.setAl(v3, 1.0f);
                    com.oupao.wiz.MainActivity.setTx(v2_0, 0);
                    com.oupao.wiz.MainActivity.setAl(v2_0, 1.0f);
                    com.oupao.wiz.MainActivity.frontTo(v3);
                    com.oupao.wiz.MainActivity.scrimAt(v2_0);
                    com.oupao.wiz.MainActivity.roundTo(v2_0, true);
                    com.oupao.wiz.MainActivity.aOn = true;
                    var v0_3 = new android.os.Handler(android.os.Looper.getMainLooper());
                    var v1_3 = new com.oupao.wiz.MA$BN();
                    v0_3.postDelayed(v1_3, 900);
                    com.oupao.wiz.MainActivity.aPend = v1_3;
                }
                return;
            } else {
                return;
            }
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static void animTo(android.view.View[] p8, float p9, float p10, float p11)
    {
        var v0 = p8.length;
        int v1 = 0;
        while (v1 < v0) {
            var v2 = p8[v1];
            if (v2 != null) {
                var v4_6 = v2.animate().translationX(p9).alpha(p10).scaleX(p11).scaleY(p11).setDuration(400).setInterpolator(new android.view.animation.PathInterpolator(0.4f, 0, 0.2f, 1.0f));
                v4_6.withEndAction(new com.oupao.wiz.MA$BN());
                v4_6.start();
            }
            v1++;
        }
        return;
    }
    public static void applySugPage()
    {
        // page 6 = 「消息气泡 / 屏蔽悬浮通知」子页：sugFrom 0=消息气泡，1=屏蔽悬浮通知
        try {
            // sugFrom: 0=消息气泡 1=屏蔽悬浮通知 3=信息气泡（2 被别的页面占用）
            int vF = com.oupao.wiz.MainActivity.sugFrom;
            String v1_1 = (vF == 0) ? com.oupao.wiz.Cfg.K_BUBBLE_APPS
                    : ((vF == 1) ? com.oupao.wiz.Cfg.K_NOHU_APPS : com.oupao.wiz.Cfg.K_SUG_INFO_APPS);
            String v2_1 = (vF == 0) ? "\u6d88\u606f\u6c14\u6ce1"
                    : ((vF == 1) ? "\u5c4f\u853d\u60ac\u6d6e\u901a\u77e5" : "\u4fe1\u606f\u6c14\u6ce1");
            var v3_1 = com.oupao.wiz.MainActivity.pgSugTitle;
            if (v3_1 != null) {
                v3_1.setText(v2_1);
            }
            // 开关留在「消息提醒」页，子页只放「生效应用」名单（用户要求）
            var v5_1 = com.oupao.wiz.MainActivity.inst;
            var v4_1 = com.oupao.wiz.MainActivity.sugAppCard;
            if ((v4_1 != null) && (v5_1 != null)) {
                com.oupao.wiz.AppPick.buildList(v5_1, v4_1, v1_1,
                        com.oupao.wiz.MainActivity.sugSearch, null);
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static void backAnim()
    {
        // 弹窗打开期间（以及刚关闭的手势尾巴）不允许走旧的整页返回：
        // 侧滑返回应该只关闭 OverlayDialog，不能把当前页滑回功能页。
        try {
            if (com.oupao.wiz.MiuixShell.popupBlocksPageBack()) {
                return;
            }
        } catch (Throwable ignore) {
        }
        try {
            if (!com.oupao.wiz.MainActivity.posOn()) {
                if (com.oupao.wiz.MainActivity.cur != com.oupao.wiz.MainActivity.aNext) {
                    com.oupao.wiz.MainActivity.navEnd();
                }
                com.oupao.wiz.MainActivity.logSt("backAnim");
                var v1_0 = com.oupao.wiz.MainActivity.backTarget(com.oupao.wiz.MainActivity.aNext);
                if (v1_0 < 0) {
                    com.oupao.wiz.SLog.i("finish");
                    var v0_1 = com.oupao.wiz.MainActivity.inst;
                    if (v0_1 == null) {
                        com.oupao.wiz.SLog.i("finish-skip inst=null");
                    } else {
                        v0_1.finish();
                        com.oupao.wiz.SLog.i("finish-called");
                    }
                } else {
                    com.oupao.wiz.MainActivity.backTo(v1_0);
                }
                return;
            } else {
                com.oupao.wiz.MainActivity.closePosEditor();
                return;
            }
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static int backTarget(int p2)
    {
        // 页面编号已前移（原 11/12/13 -> 10/11/12，原第 10 页删除）
        // 注意：返回 <0 表示"退出到桌面"（backAnim 里收到 <0 就 finish()），
        // 不能对所有未知页都返回 0，否则主页按返回会原地不动。
        // ★ 根页面（功能目录/主页/日志）必须先判定：否则 cur==8 的旧判断会把
        //   功能页的返回改写成「回 Flyme 菜单」，表现就是「回不到桌面、返回像回上一页」。
        if ((p2 == 14) || (p2 == 0) || (p2 == 5)) { return -1; }
        if (p2 == 15) { return 1; }          // 轻量打开二级页 → 回 Flyme式UI 页
        if (p2 == 3) { return 2; }
        if (p2 == 7) { return 2; }
        if (p2 == 6) { return 12; }              // 菜单页（原 13）
        if (p2 == 9) {
            return com.oupao.wiz.MainActivity.deskOn ? -1 : 8;
        }
        if (com.oupao.wiz.MainActivity.cur == 8) { return 14; }
        // The Miuix host's three roots exit on back; subpages still return to 14.
        if ((p2 == 1) || (p2 == 2) || (p2 == 4)) { return 14; }
        if (p2 == 8) { return 14; }
        if ((p2 == 10) || (p2 == 11) || (p2 == 12) || (p2 == 13)) { return 14; }
        // 主页(0) 以及其它未知页：退出到桌面
        return -1;
    }
    public static void backTo(int p1)
    {
        com.oupao.wiz.MainActivity.slide(p1, false);
        return;
    }
    public static void buildAicMsgCard(android.content.Context p9)
    {
        try {
            var v8 = com.oupao.wiz.Ui.card(p9);
            com.oupao.wiz.MainActivity.sAic = v8;
            var v0_10 = com.oupao.wiz.Ui.t(p9, "Aicy\u5efa\u8bae", 18.0f, com.oupao.wiz.Ui.textMain(), true);
            var v7_0 = new android.widget.LinearLayout(p9);
            v7_0.setOrientation(0);
            v7_0.setGravity(16);
            v7_0.setPadding(com.oupao.wiz.Ui.dp(p9, 20), com.oupao.wiz.Ui.dp(p9, 13), com.oupao.wiz.Ui.dp(p9, 20), com.oupao.wiz.Ui.dp(p9, 13));
            v0_10.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
            v7_0.addView(v0_10);
            v7_0.addView(com.oupao.wiz.Ui.t(p9, "\u203a", 28.0f, com.oupao.wiz.Ui.textSub(), true));
            com.oupao.wiz.Ui.ripple(v7_0);
            v7_0.setOnClickListener(new com.oupao.wiz.MainActivity$C4());
            v8.addView(v7_0);
            var v0_6 = com.oupao.wiz.Ui.t(p9, "\u6d88\u606f\u63d0\u9192", 18.0f, com.oupao.wiz.Ui.textMain(), true);
            var v7_2 = new android.widget.LinearLayout(p9);
            v7_2.setOrientation(0);
            v7_2.setGravity(16);
            v7_2.setPadding(com.oupao.wiz.Ui.dp(p9, 20), com.oupao.wiz.Ui.dp(p9, 13), com.oupao.wiz.Ui.dp(p9, 20), com.oupao.wiz.Ui.dp(p9, 13));
            v0_6.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
            v7_2.addView(v0_6);
            v7_2.addView(com.oupao.wiz.Ui.t(p9, "\u203a", 28.0f, com.oupao.wiz.Ui.textSub(), true));
            com.oupao.wiz.Ui.ripple(v7_2);
            com.oupao.wiz.Ui.navRow2(v7_2, 2);
            v8.addView(v7_2);
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static void buildDiagCard(android.content.Context p5, android.widget.LinearLayout p6)
    {
        var v0 = com.oupao.wiz.Ui.card(p5);
        p6.addView(v0);
        v0.addView(com.oupao.wiz.MainActivity.mkPick(p5, "\u8bca\u65ad", "\u8fd0\u884c\u72b6\u6001 / \u65e5\u5fd7 / \u81ea\u68c0", "", new com.oupao.wiz.MainActivity$C6()));
        // 隐藏桌面图标：独立卡片（只禁用 LauncherAlias；LSPosed 的「打开」走 MODULE_SETTINGS 别名，
        // 所以图标隐藏后仍能从 LSPosed 进入，也不需要桌面作用域）
        var vHide = com.oupao.wiz.Ui.card(p5);
        p6.addView(vHide);
        com.oupao.wiz.Ui.row(vHide, "oupao_hide_icon", "\u9690\u85cf\u684c\u9762\u56fe\u6807|\u684c\u9762\u4e0d\u518d\u663e\u793a\u672c\u6a21\u5757\u56fe\u6807\uff0cLSPosed \u91cc\u7684\u300c\u6253\u5f00\u300d\u7167\u5e38\u53ef\u7528");

        return;
    }
    public static void buildLog()
    {
        var v0 = com.oupao.wiz.MainActivity.logCard;
        if (v0 != null) {
            v0.removeAllViews();
            var v1 = v0.getContext();
            if (v1 != null) {
                int v4 = 1;
                if (com.oupao.wiz.Cfg.getInt("oupao_dbg", 0) != 1) {
                    if (com.oupao.wiz.Cfg.getInt("oupao_log_quiet", 0) == 1) {
                        v4 = 2;
                    }
                } else {
                    v4 = 0;
                }
                java.lang.String v7_0 = "";
                if (v4 == 0) {
                    v7_0 = "\u2713";
                }
                v0.addView(com.oupao.wiz.MainActivity.mkPick(v1, "\u65e5\u5fd7\uff1a\u8be6\u7ec6", "\u8bb0\u5f55\u5168\u90e8\u8fc7\u7a0b\u65e5\u5fd7\uff08\u8c03\u8bd5\u7528\uff09", v7_0, new com.oupao.wiz.MA$LG(0)));
                java.lang.String v7_1 = "";
                if (v4 == 1) {
                    v7_1 = "\u2713";
                }
                v0.addView(com.oupao.wiz.MainActivity.mkPick(v1, "\u65e5\u5fd7\uff1a\u5e38\u89c4", "\u9ed8\u8ba4\uff0c\u8bb0\u5e38\u89c4\u4fe1\u606f", v7_1, new com.oupao.wiz.MA$LG(1)));
                java.lang.String v7_2 = "";
                if (v4 == 2) {
                    v7_2 = "\u2713";
                }
                v0.addView(com.oupao.wiz.MainActivity.mkPick(v1, "\u65e5\u5fd7\uff1a\u5173\u95ed", "\u4e0d\u8bb0\u5f55\u4efb\u4f55\u65e5\u5fd7", v7_2, new com.oupao.wiz.MA$LG(2)));
            }
        }
        return;
    }
    public static void buildMusic()
    {
        android.widget.LinearLayout v0 = null;
        try {
            v0 = com.oupao.wiz.MainActivity.sQ;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 != null) {
            v0.removeAllViews();
            v0.setBackground(null);
            v0.setShowDividers(0);
            var v10_10 = ((android.view.ViewGroup.MarginLayoutParams) v0.getLayoutParams());
            v10_10.setMargins(0, v10_10.topMargin, 0, 0);
            v0.setLayoutParams(v10_10);
            var v1 = v0.getContext();
            var v5 = com.oupao.wiz.Ui.card(v1);
            var v9_1 = ((android.view.ViewGroup.MarginLayoutParams) v5.getLayoutParams());
            v9_1.setMargins(com.oupao.wiz.Ui.dp(v1, 18), 0, com.oupao.wiz.Ui.dp(v1, 18), 0);
            v5.setLayoutParams(v9_1);
            var v9_2 = com.oupao.wiz.Ui.card(v1);
            var v10_3 = com.oupao.wiz.MainActivity.mkMusRow(v1, "\u81ea\u5b9a\u4e49\u5305\u540d", "\u9009\u62e9\u5e94\u7528\u6dfb\u52a0 \u00b7 \u957f\u6309\u6b64\u884c\u6062\u590d\u5185\u7f6e\u5e94\u7528", false);
            v10_3.setOnClickListener(new com.oupao.wiz.MA$MC(null));
            v10_3.setOnLongClickListener(new com.oupao.wiz.MA$MR(null));
            v9_2.addView(v10_3);
            v0.addView(v9_2);
            var v2 = com.oupao.wiz.MainActivity.scanMusic();
            var v3 = com.oupao.wiz.Cfg.getStr("oupao_music_pkg", "");
            java.lang.String v4 = com.oupao.wiz.Cfg.getStr("oupao_music_custom", "");
            if (v4 == null) {
                v4 = "";
            }
            var v6 = com.oupao.wiz.Ui.card(v1);
            if (v2 != null) {
                var v7 = v2.length;
                int v8 = 0;
                while (v8 < v7) {
                    var v9_9 = v2[v8];
                    var v10_8 = v9_9.indexOf("|");
                    if (v10_8 > 0) {
                        var v11_7 = v9_9.substring(0, v10_8);
                        var v13 = com.oupao.wiz.MainActivity.mkMusRow(v1, v9_9.substring((v10_8 + 1)), v11_7, v11_7.equals(v3));
                        var v12_4 = v4.contains(v11_7);
                        v13.setOnClickListener(new com.oupao.wiz.MA$P(v11_7));
                        v13.setOnLongClickListener(new com.oupao.wiz.MA$MQ(v11_7));
                        if (!v12_4) {
                            v5.addView(v13);
                        } else {
                            v6.addView(v13);
                        }
                    }
                    v8++;
                }
            }
            if (v5.getChildCount() > 0) {
                v0.addView(v5, 0);
            }
            if (v6.getChildCount() > 0) {
                v0.addView(v6);
            }
        }
        return;
    }
    public static void closePosEditor()
    {
        try {
            // 退出编辑器后放开方向锁，可能马上再 recreate 一次；
            // 记下当前页，重建后切回来，别让用户"回到首页"
            if (com.oupao.wiz.MainActivity.posEditWanted) {
                com.oupao.wiz.MainActivity.retPage = com.oupao.wiz.MainActivity.cur;
            }
        } catch (Throwable ignore) {
        }
        try {
            if (com.oupao.wiz.MainActivity.inst != null) {
                unregPosBack(com.oupao.wiz.MainActivity.inst);
            }
        } catch (Throwable ignore) {
        }
        com.oupao.wiz.MainActivity.detachPosOverlay();
        com.oupao.wiz.MainActivity.posEditWanted = false;
        com.oupao.wiz.MainActivity.posPage = -1;
        com.oupao.wiz.MainActivity.restoreOrientation();
        return;
    }

    /** 编辑器打开期间独享返回：只关闭编辑器，不退出模块界面。 */
    public static Object posBackCb;
    static void regPosBack(android.app.Activity a)
    {
        if (a == null || android.os.Build.VERSION.SDK_INT < 33) return;
        try {
            unregPosBack(a);
            android.window.OnBackInvokedCallback cb = new android.window.OnBackInvokedCallback() {
                public void onBackInvoked() {
                    closePosEditor();
                }
            };
            a.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY, cb);
            posBackCb = cb;
        } catch (Throwable ignore) {
        }
    }
    static void unregPosBack(android.app.Activity a)
    {
        try {
            Object cb = posBackCb;
            if (cb != null && a != null) {
                a.getOnBackInvokedDispatcher()
                        .unregisterOnBackInvokedCallback(((android.window.OnBackInvokedCallback) cb));
            }
        } catch (Throwable ignore) {
        }
        posBackCb = null;
    }
    static void detachPosOverlay()
    {
        android.widget.FrameLayout v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.MainActivity.posOverlay;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_0 != null) {
            var v1 = com.oupao.wiz.MainActivity.pageBox;
            if (v1 != null) {
                v1.removeView(v0_0);
            }
        }
        com.oupao.wiz.MainActivity.posOverlay = null;
        com.oupao.wiz.MainActivity.posBubble = null;
        try {
            if (com.oupao.wiz.MainActivity.inst != null) {
                unregPosBack(com.oupao.wiz.MainActivity.inst);
            }
        } catch (Throwable ignore) {
        }
        return;
    }
    /** 编辑器里切方向：强制把界面转到另一个方向（不依赖系统自动旋转开关） */
    public static void switchPosDir()
    {
        try {
            var a = com.oupao.wiz.MainActivity.inst;
            if (a == null) {
                return;
            }
            boolean land = com.oupao.wiz.BubPos.land(a);
            com.oupao.wiz.MainActivity.posEditWanted = true;
            // 1 = SCREEN_ORIENTATION_PORTRAIT, 0 = SCREEN_ORIENTATION_LANDSCAPE
            com.oupao.wiz.MainActivity.posReqOrientation = land ? 1 : 0;
            com.oupao.wiz.SLog.i("Pos: switch dir -> " + (land ? "portrait" : "landscape"));
            a.setRequestedOrientation(com.oupao.wiz.MainActivity.posReqOrientation);
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    /** 退出编辑器时放开方向锁，恢复系统/用户的旋转策略 */
    static void restoreOrientation()
    {
        try {
            if (com.oupao.wiz.MainActivity.posReqOrientation != -1) {
                com.oupao.wiz.MainActivity.posReqOrientation = -1;
                var a = com.oupao.wiz.MainActivity.inst;
                if (a != null) {
                    a.setRequestedOrientation(-1);   // SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static void dlog(String p4)
    {
        android.content.Context v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Cfg.ctx;
        } catch (Throwable __e_v3) {
            return;
        }
        if (v0_0 != null) {
            var v0_2 = v0_0.getExternalFilesDir(null);
            if (v0_2 != null) {
                try {
                    var v0_1 = new java.io.FileWriter(new java.io.File(v0_2, "desk.log"), true);
                    v0_1.write(p4);
                    v0_1.write("\n");
                    v0_1.close();
                } catch (Throwable __e_io) {
                }
            }
        }
        return;
    }
    public static void fadeTo(android.view.View[] p6, float p7, float p8)
    {
        var v0 = p6.length;
        int v1 = 0;
        while (v1 < v0) {
            var v2 = p6[v1];
            if (v2 != null) {
                v2.animate().translationX(p7).alpha(p8).setDuration(400).start();
            }
            v1++;
        }
        return;
    }
    public static void finishNow()
    {
        com.oupao.wiz.MainActivity v0_1 = null;
        try {
            com.oupao.wiz.MainActivity.deskOn = false;
            v0_1 = com.oupao.wiz.MainActivity.inst;
        } catch (Throwable __e_v1) {
            return;
        }
        if (v0_1 != null) {
            v0_1.finishAndRemoveTask();
        }
        return;
    }

    /** 根页面返回：明确回桌面，不依赖系统默认的“上一个任务”。 */
    public static void exitToHome()
    {
        com.oupao.wiz.MainActivity v0_1 = null;
        try {
            com.oupao.wiz.MainActivity.deskOn = false;
            v0_1 = com.oupao.wiz.MainActivity.inst;
        } catch (Throwable __e_v1) {
            return;
        }
        if (v0_1 == null) {
            return;
        }
        try {
            android.content.Intent home = new android.content.Intent(android.content.Intent.ACTION_MAIN);
            home.addCategory(android.content.Intent.CATEGORY_HOME);
            home.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            v0_1.startActivity(home);
        } catch (Throwable ignore) {
        }
        try {
            v0_1.finishAndRemoveTask();
        } catch (Throwable ignore) {
        }
        return;
    }
    public static void frontTo(android.view.View[] p3)
    {
        int v0 = 0;
        int v1 = 0;
        try {
            v0 = p3.length;
            v1 = 0;
        } catch (Throwable __e_v0) {
            return;
        }
        while (v1 < v0) {
            var v2 = p3[v1];
            if (v2 != null) {
                v2.bringToFront();
            }
            v1++;
        }
        return;
    }
    public static void initBgs()
    {
        try {
            android.view.View[] v0_1 = new android.view.View[14];
            v0_1[0] = com.oupao.wiz.MainActivity.bg0;
            v0_1[1] = com.oupao.wiz.MainActivity.bg1;
            v0_1[2] = com.oupao.wiz.MainActivity.bg2;
            v0_1[3] = com.oupao.wiz.MainActivity.bg3;
            v0_1[4] = com.oupao.wiz.MainActivity.bg4;
            v0_1[5] = com.oupao.wiz.MainActivity.bg5;
            v0_1[6] = com.oupao.wiz.MainActivity.bg6;
            v0_1[7] = com.oupao.wiz.MainActivity.bg7;
            v0_1[8] = com.oupao.wiz.MainActivity.bg8;
            v0_1[9] = com.oupao.wiz.MainActivity.bg9;
            // 原第 10 页（pgApps）已删除：bg10 不再对应任何页，bg11..bg13 前移一位
            v0_1[10] = com.oupao.wiz.MainActivity.bg11;
            v0_1[11] = com.oupao.wiz.MainActivity.bg12;
            v0_1[12] = com.oupao.wiz.MainActivity.bg13;
            v0_1[13] = null;
            com.oupao.wiz.MainActivity.bgList = v0_1;
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static void initPages()
    {
        try {
            android.view.View[] v0_1 = new android.view.View[14];
            v0_1[0] = com.oupao.wiz.MainActivity.pg0;
            v0_1[1] = null; // Compose-owned page (1 · Flyme 小窗)
            v0_1[2] = null; // Compose-owned page (2 · Aicy 建议)
            v0_1[3] = com.oupao.wiz.MainActivity.pg3; // legacy page 3 (选择音乐 App)
            v0_1[4] = null; // Compose-owned page (MiuixShell/Pages.kt)
            v0_1[5] = com.oupao.wiz.MainActivity.pageLog;
            v0_1[6] = com.oupao.wiz.MainActivity.pageSug;
            v0_1[7] = com.oupao.wiz.MainActivity.pg7;
            v0_1[8] = null; // Compose-owned page (MiuixShell/Pages.kt)
            v0_1[9] = com.oupao.wiz.MainActivity.pgPins;
            v0_1[10] = null; // Compose-owned page (MiuixShell/Pages.kt)
            v0_1[11] = null; // Compose-owned page (MiuixShell/Pages.kt)
            v0_1[12] = null; // Compose-owned page (MiuixShell/Pages.kt)
            v0_1[13] = null; // Compose-owned page (MiuixShell/Pages.kt)
            com.oupao.wiz.MainActivity.pgList = v0_1;
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static void killPend()
    {
        var v0 = com.oupao.wiz.MainActivity.aPend;
        if (v0 != null) {
            com.oupao.wiz.MainActivity.aPend = null;
            new android.os.Handler(android.os.Looper.getMainLooper()).removeCallbacks(v0);
        }
        return;
    }
    public static void logSt(String p3)
    {
        int v1_2 = 0;
        var v0_1 = new StringBuilder();
        v0_1.append(p3);
        v0_1.append(" cur=");
        v0_1.append(com.oupao.wiz.MainActivity.cur);
        v0_1.append(" next=");
        v0_1.append(com.oupao.wiz.MainActivity.aNext);
        v0_1.append(" pend=");
        if (com.oupao.wiz.MainActivity.aPend == null) {
            v1_2 = 0;
        } else {
            v1_2 = 1;
        }
        v0_1.append(v1_2);
        com.oupao.wiz.SLog.i(v0_1.toString());
        return;
    }
    public static android.widget.TextView mkArrow(android.content.Context p5)
    {
        var v0_1 = com.oupao.wiz.Ui.t(p5, "\u2190", 20.0f, com.oupao.wiz.Ui.textMain(), false);
        com.oupao.wiz.Ui.pad(v0_1, 18, 0, 0, 0);
        v0_1.setOnClickListener(new com.oupao.wiz.MainActivity$C5());
        return v0_1;
    }
    public static android.widget.LinearLayout mkMaster(android.content.Context p7, String p8, String p9)
    {
        int v3_6 = 0;
        var v0 = com.oupao.wiz.Ui.card(p7);
        var v1_1 = new android.widget.LinearLayout(p7);
        v1_1.setOrientation(0);
        v1_1.setGravity(16);
        v1_1.setPadding(com.oupao.wiz.Ui.dp(p7, 20), com.oupao.wiz.Ui.dp(p7, 14), com.oupao.wiz.Ui.dp(p7, 20), com.oupao.wiz.Ui.dp(p7, 14));
        var v2_2 = com.oupao.wiz.Ui.t(p7, p9, 17.0f, com.oupao.wiz.Ui.textMain(), true);
        v2_2.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        v1_1.addView(v2_2);
        var v2_4 = new android.widget.Switch(p7);
        if (com.oupao.wiz.Cfg.masterChecked(p8) != true) {
            v3_6 = 0;
        } else {
            v3_6 = 1;
        }
        v2_4.setChecked(v3_6 != 0);
        v2_4.setOnCheckedChangeListener(new com.oupao.wiz.MainActivity$C1(((com.oupao.wiz.MainActivity) p7), p8));
        v2_4.setThumbTintList(com.oupao.wiz.Ui.csl(-13335809, -1));
        v2_4.setTrackTintList(com.oupao.wiz.Ui.csl(1026851583, -1447445));
        var v3_14 = new android.widget.LinearLayout.LayoutParams(-2, -2);
        v3_14.setMargins(com.oupao.wiz.Ui.dp(p7, 12), 0, 0, 0);
        v2_4.setLayoutParams(v3_14);
        v1_1.addView(v2_4);
        v1_1.setOnClickListener(new com.oupao.wiz.Ui$C1(v2_4));
        v0.addView(v1_1);
        return v0;
    }
    public static android.widget.LinearLayout mkMusRow(android.content.Context p9, String p10, String p11, boolean p12)
    {
        int v5_0 = 0;
        var v0_1 = new android.widget.LinearLayout(p9);
        v0_1.setOrientation(0);
        v0_1.setGravity(16);
        v0_1.setPadding(com.oupao.wiz.Ui.dp(p9, 20), com.oupao.wiz.Ui.dp(p9, 14), com.oupao.wiz.Ui.dp(p9, 20), com.oupao.wiz.Ui.dp(p9, 14));
        com.oupao.wiz.Ui.ripple(v0_1);
        var v3_1 = new android.widget.LinearLayout(p9);
        v3_1.setOrientation(1);
        if (!p12) {
            v5_0 = com.oupao.wiz.Ui.textMain();
        } else {
            v5_0 = -13335809;
        }
        v3_1.addView(com.oupao.wiz.Ui.t(p9, p10, 16.0f, v5_0, false));
        var v4_4 = com.oupao.wiz.Ui.t(p9, p11, 13.0f, com.oupao.wiz.Ui.textSub(), false);
        var v5_3 = new android.widget.LinearLayout.LayoutParams(-2, -2);
        v5_3.setMargins(0, com.oupao.wiz.Ui.dp(p9, 3), 0, 0);
        v4_4.setLayoutParams(v5_3);
        v3_1.addView(v4_4);
        v3_1.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        v0_1.addView(v3_1);
        if (p12) {
            var v4_8 = com.oupao.wiz.Ui.t(p9, "\u2713", 18.0f, -13335809, false);
            com.oupao.wiz.Ui.pad(v4_8, 18, 0, 0, 0);
            v0_1.addView(v4_8);
        }
        return v0_1;
    }
    public static android.widget.LinearLayout mkPick(android.content.Context p9, String p10, String p11, String p12, android.view.View.OnClickListener p13)
    {
        var v0_1 = new android.widget.LinearLayout(p9);
        v0_1.setOrientation(0);
        v0_1.setGravity(16);
        v0_1.setPadding(com.oupao.wiz.Ui.dp(p9, 20), com.oupao.wiz.Ui.dp(p9, 14), com.oupao.wiz.Ui.dp(p9, 20), com.oupao.wiz.Ui.dp(p9, 14));
        var v3_1 = new android.widget.LinearLayout(p9);
        v3_1.setOrientation(1);
        v3_1.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        v3_1.addView(com.oupao.wiz.Ui.t(p9, p10, 16.0f, com.oupao.wiz.Ui.textMain(), false));
        var v4_6 = com.oupao.wiz.Ui.t(p9, p11, 13.0f, com.oupao.wiz.Ui.textSub(), false);
        var v5_4 = new android.widget.LinearLayout.LayoutParams(-2, -2);
        v5_4.setMargins(0, com.oupao.wiz.Ui.dp(p9, 3), 0, 0);
        v4_6.setLayoutParams(v5_4);
        v3_1.addView(v4_6);
        v0_1.addView(v3_1);
        if ((p12 != null) && (p12.length() != 0)) {
            var v4_9 = com.oupao.wiz.Ui.t(p9, p12, 18.0f, -13335809, false);
            com.oupao.wiz.Ui.pad(v4_9, 18, 0, 0, 0);
            v0_1.addView(v4_9);
        }
        com.oupao.wiz.Ui.ripple(v0_1);
        v0_1.setOnClickListener(p13);
        return v0_1;
    }
    public static android.widget.TextView mkPosBtn(android.content.Context p5, String p6, int p7)
    {
        var v0_1 = com.oupao.wiz.Ui.t(p5, p6, 15.0f, -1, false);
        v0_1.setGravity(17);
        com.oupao.wiz.Ui.pad(v0_1, 20, 10, 20, 10);
        int bg = (p7 == 1) ? 0xFF3482FF : 0x33FFFFFF;
        v0_1.setBackground(com.oupao.wiz.Ui.bg(bg, ((float) com.oupao.wiz.Ui.dp(p5, 22))));
        v0_1.setElevation(com.oupao.wiz.Ui.dp(p5, 4));
        v0_1.setOnClickListener(new com.oupao.wiz.MA$PB(p7));
        return v0_1;
    }
    public static void movePage(android.view.ViewGroup p7, android.view.View p8)
    {
        if ((p7 != null) && (p8 != null)) {
            int v0 = 0;
            int v1 = 0;
            int v2 = 0;
            int v3 = 0;
            var v4_4 = p8.getLayoutParams();
            if ((v4_4 != null) && ((v4_4 instanceof android.view.ViewGroup.MarginLayoutParams))) {
                var v4_0 = ((android.view.ViewGroup.MarginLayoutParams) v4_4);
                v0 = v4_0.leftMargin;
                v1 = v4_0.topMargin;
                v2 = v4_0.rightMargin;
                v3 = v4_0.bottomMargin;
            }
            var v4_1 = p8.getParent();
            if (v4_1 != null) {
                ((android.view.ViewGroup) v4_1).removeView(p8);
            }
            if (!(p7 instanceof android.widget.FrameLayout)) {
                var v6_2 = new android.widget.LinearLayout.LayoutParams(-1, -2);
                v6_2.setMargins(v0, v1, v2, v3);
                p7.addView(p8, v6_2);
            } else {
                var v6_4 = new android.widget.FrameLayout.LayoutParams(-1, -2);
                v6_4.setMargins(v0, v1, v2, v3);
                p7.addView(p8, v6_4);
            }
        }
        return;
    }
    /**
     * 1820fix: 切页/返回时把所有页面背景层重置为 pageBg。
     *
     * 从 navEnd/navTo/navToSug 三处逐字节相同的样板抽出来
     * （原来每处各一份，改一处忘两处是必然的）。
     * 注意：只重置 bgN 背景层；pg0/pg1/pg3/pg7 是页面容器本身，
     * smali 里从不对它们设背景色。
     */
    private static void resetPageBgs()
    {
        try {
            if (com.oupao.wiz.MainActivity.pageBox != null) {
                com.oupao.wiz.MainActivity.pageBox.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
            }
            if (com.oupao.wiz.MainActivity.bg0 != null) {
                com.oupao.wiz.MainActivity.bg0.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
            }
            if (com.oupao.wiz.MainActivity.bg1 != null) {
                com.oupao.wiz.MainActivity.bg1.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
            }
            if (com.oupao.wiz.MainActivity.bg2 != null) {
                com.oupao.wiz.MainActivity.bg2.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
            }
            if (com.oupao.wiz.MainActivity.bg3 != null) {
                com.oupao.wiz.MainActivity.bg3.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
            }
            if (com.oupao.wiz.MainActivity.bg4 != null) {
                com.oupao.wiz.MainActivity.bg4.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
            }
            if (com.oupao.wiz.MainActivity.bg5 != null) {
                com.oupao.wiz.MainActivity.bg5.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
            }
            if (com.oupao.wiz.MainActivity.bg6 != null) {
                com.oupao.wiz.MainActivity.bg6.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
            }
            if (com.oupao.wiz.MainActivity.bg7 != null) {
                com.oupao.wiz.MainActivity.bg7.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
            }
            if (com.oupao.wiz.MainActivity.bg8 != null) {
                com.oupao.wiz.MainActivity.bg8.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
            }
            if (com.oupao.wiz.MainActivity.bg9 != null) {
                com.oupao.wiz.MainActivity.bg9.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
            }
            if (com.oupao.wiz.MainActivity.bg10 != null) {
                com.oupao.wiz.MainActivity.bg10.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
            }
            if (com.oupao.wiz.MainActivity.bg11 != null) {
                com.oupao.wiz.MainActivity.bg11.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
            }
            if (com.oupao.wiz.MainActivity.bg12 != null) {
                com.oupao.wiz.MainActivity.bg12.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
            }
            if (com.oupao.wiz.MainActivity.bg13 != null) {
                com.oupao.wiz.MainActivity.bg13.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
            }
        } catch (Throwable ignore) {
        }
    }
    public static void navEnd()
    {
        // 1820fix: 切页/返回时把所有页面背景层重置为 pageBg
        try {
            com.oupao.wiz.MainActivity.resetPageBgs();
        } catch (Throwable ignore) {
        }
        try {
            if (com.oupao.wiz.MainActivity.sSv != null) {
                com.oupao.wiz.MainActivity.saveScroll(com.oupao.wiz.MainActivity.cur);
            }
        } catch (Throwable __e_v0) {
            return;
        }
        com.oupao.wiz.MainActivity.logSt("navEnd");
        if (com.oupao.wiz.MainActivity.aPend != null) {
            com.oupao.wiz.MainActivity.aPend = null;
            com.oupao.wiz.MainActivity.show(com.oupao.wiz.MainActivity.aNext);
            com.oupao.wiz.MainActivity.settleTf(com.oupao.wiz.MainActivity.aFrom);
            com.oupao.wiz.MainActivity.settleTf(com.oupao.wiz.MainActivity.aTo);
        }
        return;
    }
    public static void navTo(int p1)
    {
        // 1820fix: 切页/返回时把所有页面背景层重置为 pageBg
        try {
            com.oupao.wiz.MainActivity.resetPageBgs();
        } catch (Throwable ignore) {
        }
        // Compose-owned pages have no legacy View array, so slide() would no-op: hand them
        // straight to show(), which routes to the MiuixShell host. The set comes from
        // MiuixShell.ownsPage(), so migrating another page needs no edit here.
        if (com.oupao.wiz.MiuixShell.ownsPage(p1)) {
            com.oupao.wiz.MainActivity.show(p1);
            return;
        }
        com.oupao.wiz.MainActivity.slide(p1, true);
        return;
    }
    public static void navToSug(int p3)
    {
        // 1820fix: 切页/返回时把所有页面背景层重置为 pageBg
        try {
            com.oupao.wiz.MainActivity.resetPageBgs();
        } catch (Throwable ignore) {
        }
        try {
            com.oupao.wiz.MainActivity.sugFrom = p3;
        } catch (Throwable __e_v0) {
            return;
        }
        if (p3 != 2) {
            com.oupao.wiz.MainActivity.navTo(6);
            com.oupao.wiz.MainActivity.applySugPage();
            return;
        } else {
            com.oupao.wiz.MainActivity.navTo(12);
            return;
        }
    }
    public static int openPage()
    {
        int v0_0 = 0;
        try {
            v0_0 = com.oupao.wiz.MainActivity.navReq;
        } catch (Throwable __e_v0) {
            return 0;
        }
        if (v0_0 < 0) {
            if (!com.oupao.wiz.MainActivity.deskOn) {
                return 14;
            } else {
                return 9;
            }
        } else {
            return v0_0;
        }
    }
    public static void openPosEditor()
    {
        android.widget.FrameLayout v0 = null;
        try {
            v0 = com.oupao.wiz.MainActivity.pageBox;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 != null) {
            com.oupao.wiz.MainActivity.detachPosOverlay();
            com.oupao.wiz.MainActivity.posEditWanted = true;
            com.oupao.wiz.MainActivity.posPage = com.oupao.wiz.MainActivity.cur;
            com.oupao.wiz.SLog.i("Pos: open editor pageBoxH=" + v0.getHeight());
            var v1 = v0.getContext();
            var v2_1 = new android.widget.FrameLayout(v1);
            // 与模块界面一致的柔和暗色遮罩，不再是一整块纯黑。
            v2_1.setBackgroundColor(0xFF14151A);
            v2_1.setClickable(true);
            v0.addView(v2_1, new android.widget.FrameLayout.LayoutParams(-1, -1));
            com.oupao.wiz.MainActivity.posOverlay = v2_1;
            try {
                if (v1 instanceof android.app.Activity) {
                    com.oupao.wiz.MainActivity.regPosBack((android.app.Activity) v1);
                } else if (com.oupao.wiz.MainActivity.inst != null) {
                    com.oupao.wiz.MainActivity.regPosBack(com.oupao.wiz.MainActivity.inst);
                }
            } catch (Throwable ignore) {
            }
            var v3_8 = new android.widget.TextView(v1);
            android.graphics.drawable.GradientDrawable bubbleBg = new android.graphics.drawable.GradientDrawable();
            bubbleBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            bubbleBg.setColor(0xFFF2F6FF);
            bubbleBg.setStroke(com.oupao.wiz.Ui.dp(v1, 2), 0xFF3482FF);
            v3_8.setBackground(bubbleBg);
            v3_8.setElevation(com.oupao.wiz.Ui.dp(v1, 10));
            v3_8.setText("\u25CF");
            v3_8.setTextColor(0xFF3482FF);
            v3_8.setTextSize(18.0f);
            v3_8.setGravity(17);
            int[] v4_10 = new int[2];
            v4_10[0] = -1;
            v4_10[1] = -1;
            com.oupao.wiz.MainActivity.posInit(v4_10);
            int[] v5_24 = new int[2];
            v2_1.getLocationOnScreen(v5_24);
            int v6_9;
            int v7_1;
            if ((v4_10[0] >= 0) && (v4_10[1] >= 0)) {
                // 有保存值：存的是屏幕绝对坐标，减掉 overlay 的屏幕原点即得预览边距
                v6_9 = (v4_10[0] - v5_24[0]);
                v7_1 = (v4_10[1] - v5_24[1]);
            } else {
                // 没保存过：默认位置必须和气泡默认**完全一致**。
                // 气泡默认 = gravity BOTTOM|START、x=12dp、y=96dp、边长 46dp
                //   → 屏幕坐标 (12dp, 屏幕高 - 96dp - 46dp) = (12dp, 屏幕高 - 142dp)
                // 注意用【显示尺寸】而不是 pageBox 高度：模块界面横屏会被 letterbox，
                // 内容高度 ≠ 屏幕高（竖屏状态栏那块变黑），拿 pageBox 算就会差一截。
                int[] sc = com.oupao.wiz.BubPos.screen(v1);
                v6_9 = (com.oupao.wiz.Ui.dp(v1, 12) - v5_24[0]);
                v7_1 = ((sc[1] - com.oupao.wiz.Ui.dp(v1, 142)) - v5_24[1]);
                com.oupao.wiz.SLog.i("Pos: default screen=" + sc[0] + "x" + sc[1]
                        + " overlayOrg=" + v5_24[0] + "," + v5_24[1]
                        + " boxH=" + v0.getHeight());
            }
            if (v6_9 < 0) {
                v6_9 = 0;
            }
            if (v7_1 < 0) {
                v7_1 = 0;
            }
            com.oupao.wiz.SLog.i("Pos: editor init " + v6_9 + "," + v7_1
                    + " saved=" + com.oupao.wiz.BubPos.raw(v1)
                    + " overlayOrg=" + v5_24[0] + "," + v5_24[1]
                    + " boxH=" + v0.getHeight());
            var v4_12 = new android.widget.FrameLayout.LayoutParams(com.oupao.wiz.Ui.dp(v1, 46), com.oupao.wiz.Ui.dp(v1, 46));
            v4_12.leftMargin = v6_9;
            v4_12.topMargin = v7_1;
            v3_8.setOnTouchListener(new com.oupao.wiz.PosDrag());
            v2_1.addView(v3_8, v4_12);
            com.oupao.wiz.MainActivity.posBubble = v3_8;
            // 实测预览气泡的屏幕落点，方便和 Sug: bubble actual= 对照
            final android.widget.TextView pv = v3_8;
            pv.postDelayed(new java.lang.Runnable() {
                public void run() {
                    try {
                        int[] p = new int[2];
                        pv.getLocationOnScreen(p);
                        com.oupao.wiz.SLog.i("Pos: preview screen=" + p[0] + "," + p[1]);
                    } catch (Throwable ignore) {
                    }
                }
            }, 300);
            var vTitle = com.oupao.wiz.Ui.t(v1,
                    "\u6c14\u6ce1\u4f4d\u7f6e \u00b7 " + (com.oupao.wiz.BubPos.land(v1) ? "\u6a2a\u5c4f" : "\u7ad6\u5c4f"),
                    22.0f, -1, true);
            vTitle.setGravity(17);
            var vTlp = new android.widget.FrameLayout.LayoutParams(-1, -2);
            vTlp.gravity = 49;
            vTlp.topMargin = com.oupao.wiz.Ui.dp(v1, 108);
            v2_1.addView(vTitle, vTlp);
            var v3_14 = com.oupao.wiz.MainActivity.mkPosBtn(v1, "\u53d6\u6d88", 3);
            var v4_19 = new android.widget.FrameLayout.LayoutParams(-2, -2);
            v4_19.gravity = 51;
            v4_19.topMargin = com.oupao.wiz.Ui.dp(v1, 36);
            v4_19.leftMargin = com.oupao.wiz.Ui.dp(v1, 24);
            v2_1.addView(v3_14, v4_19);
            var v3_2 = com.oupao.wiz.MainActivity.mkPosBtn(v1, "\u91cd\u7f6e", 2);
            var v4_2 = new android.widget.FrameLayout.LayoutParams(-2, -2);
            v4_2.gravity = 49;
            v4_2.topMargin = com.oupao.wiz.Ui.dp(v1, 36);
            com.oupao.wiz.Ui.dp(v1, 24);
            v2_1.addView(v3_2, v4_2);
            var v3_5 = com.oupao.wiz.MainActivity.mkPosBtn(v1, "\u4fdd\u5b58", 1);
            var v4_5 = new android.widget.FrameLayout.LayoutParams(-2, -2);
            v4_5.gravity = 53;
            v4_5.topMargin = com.oupao.wiz.Ui.dp(v1, 36);
            v4_5.rightMargin = com.oupao.wiz.Ui.dp(v1, 24);
            v2_1.addView(v3_5, v4_5);
            // 方向切换：编辑"另一个方向"的位置。会强制旋转界面（不受系统自动旋转开关影响），
            // 只有真转到那个方向，预览坐标才和那个方向的实际屏幕坐标一致。
            boolean curLand = com.oupao.wiz.BubPos.land(v1);
            var v3_dir = com.oupao.wiz.MainActivity.mkPosBtn(v1,
                    curLand ? "\u5207\u5230\u7ad6\u5c4f\u7f16\u8f91" : "\u5207\u5230\u6a2a\u5c4f\u7f16\u8f91", 4);
            var v4_dir = new android.widget.FrameLayout.LayoutParams(-2, -2);
            v4_dir.gravity = 81;
            v4_dir.bottomMargin = com.oupao.wiz.Ui.dp(v1, 36);
            v2_1.addView(v3_dir, v4_dir);
        }
        return;
    }
    public static void pageShow(int p5)
    {
        android.view.View v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.PinUI.railView;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_0 != null) {
            var v1_2 = 8;
            if (p5 == 9) {
                v1_2 = 0;
            }
            v0_0.setVisibility(v1_2);
            var v3_3 = v0_0.getParent();
            if (v3_3 != null) {
                ((android.view.ViewGroup) v3_3).bringChildToFront(v0_0);
            }
        }
        var v0_1 = com.oupao.wiz.MainActivity.pgSv;
        if (v0_1 != null) {
            var v1_0 = v0_1.length;
            int v2_0 = 0;
            while (v2_0 < v1_0) {
                var v3_2 = v0_1[v2_0];
                if (v3_2 != null) {
                    var v4_1 = 8;
                    if (v2_0 == p5) {
                        v4_1 = 0;
                    }
                    v3_2.setVisibility(v4_1);
                }
                v2_0++;
            }
        }
        var v0_2 = com.oupao.wiz.MainActivity.bgList;
        if (v0_2 != null) {
            var v1_1 = v0_2.length;
            int v2_1 = 0;
            while (v2_1 < v1_1) {
                var v3_1 = v0_2[v2_1];
                if (v3_1 != null) {
                    var v4_0 = 8;
                    if (v2_1 == p5) {
                        v4_0 = 0;
                    }
                    v3_1.setVisibility(v4_0);
                }
                v2_1++;
            }
        }
        return;
    }
    public static void polishPage(android.widget.LinearLayout p5)
    {
        try {
            if (p5 != null) {
                com.oupao.wiz.Ui.pad(p5, 0, 10, 0, 24);
                var v0_2 = p5.getChildAt(1);
                if (v0_2 != null) {
                    var v1_0 = v0_2.getLayoutParams();
                    if (v1_0 != null) {
                        var vtmp4 = com.oupao.wiz.Ui.dp(p5.getContext(), 24);
                        if ((v1_0 instanceof android.widget.LinearLayout.LayoutParams)) {
                            var v1_2 = ((android.widget.LinearLayout.LayoutParams) v1_0);
                            v1_2.topMargin = vtmp4;
                            v0_2.setLayoutParams(v1_2);
                        }
                    }
                }
            }
        } catch (Throwable __e___ex) {
            return;
        }
        return;
    }
    public static void polishPages()
    {
        android.view.View[] v0 = null;
        try {
            v0 = com.oupao.wiz.MainActivity.pgList;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 != null) {
            var v1 = v0.length;
            int v2 = 0;
            while (v2 < v1) {
                var v3_0 = v0[v2];
                if (v3_0 != null) {
                    com.oupao.wiz.MainActivity.polishPage(((android.widget.LinearLayout) v3_0));
                }
                v2++;
            }
        }
        return;
    }
    public static void posInit(int[] p4)
    {
        try {
            // 按当前方向取对应的那份（竖屏 oupao_bub_pos / 横屏 oupao_bub_pos_land）
            int[] xy = com.oupao.wiz.BubPos.parse(com.oupao.wiz.BubPos.raw(com.oupao.wiz.Cfg.ctx));
            if (xy != null) {
                p4[0] = xy[0];
                p4[1] = xy[1];
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static boolean posOn()
    {
        android.widget.FrameLayout v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.MainActivity.posOverlay;
        } catch (Throwable __e_v0) {
            return false;
        }
        if (v0_0 != null) {
            var v1 = com.oupao.wiz.MainActivity.pageBox;
            if ((v1 != null) && (v0_0.getParent() == v1)) {
                return true;
            }
        }
        return false;
    }
    public static void postScroll(int p4)
    {
        android.widget.ScrollView v0 = null;
        try {
            v0 = com.oupao.wiz.MainActivity.sSv;
        } catch (Throwable __e___ex) {
            return;
        }
        if (v0 != null) {
            v0.postDelayed(new com.oupao.wiz.MA$RS(p4), 1500);
        }
        return;
    }
    static void prepDirs()
    {
        java.io.File v0_2 = null;
        try {
            var v0_3 = new java.io.File("/data/data/com.oupao.wiz/files");
            v0_3.mkdirs();
            var v1_4 = new java.io.File("/storage/emulated/0/Android/data/com.oupao.wiz/files/cfg");
            v1_4.mkdirs();
            v0_3.setReadable(true, false);
            v0_3.setWritable(true, false);
            v1_4.setReadable(true, false);
            v1_4.setWritable(true, false);
            v0_2 = new java.io.File("/data/system/oupao.log");
        } catch (Throwable __e_v0) {
            return;
        }
        if (!v0_2.exists()) {
            try {
                v0_2.createNewFile();
            } catch (Throwable __e_io) {
            }
        }
        v0_2.setReadable(true, false);
        v0_2.setWritable(true, false);
        com.oupao.wiz.Cfg.fixPerms();
        // 3288：App 进程能删自己私有目录里的历史死键文件（system_server / root 都删不了）
        com.oupao.wiz.LegacyPurge.run();
        return;
    }
    public static void reMusic()
    {
        com.oupao.wiz.MainActivity.sMus = null;
        com.oupao.wiz.MainActivity.buildMusic();
        return;
    }
    public static void regBack(android.app.Activity p4)
    {
        java.lang.Object v1_2 = null;
        int v0_0 = 0;
        try {
            com.oupao.wiz.MainActivity.unregBack(p4);
            v0_0 = com.oupao.wiz.Sys.sdk();
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_0 >= 34) {
            v1_2 = new com.oupao.wiz.MA$BA();
            p4.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0, ((android.window.OnBackInvokedCallback) v1_2));
            com.oupao.wiz.MainActivity.backCb = v1_2;
        } else {
            if (v0_0 >= 33) {
                v1_2 = new com.oupao.wiz.MA$B();
            } else {
            }
        }
        return;
    }
    public static void resetPos()
    {
        android.widget.FrameLayout v0_2 = null;
        try {
            v0_2 = com.oupao.wiz.MainActivity.pageBox;
            var c = (v0_2 != null) ? v0_2.getContext() : com.oupao.wiz.Cfg.ctx;
            // 只重置"当前方向"那一份，另一个方向保持不动
            String posKey = com.oupao.wiz.BubPos.key(c);
            com.oupao.wiz.Cfg.saveStr(posKey, "-1,-1");
            com.oupao.wiz.SLog.i("Pos: reset[" + posKey + "]");
            com.oupao.wiz.Cfg.push();
            com.oupao.wiz.MainActivity.closePosEditor();
            if (v0_2 != null) {
                android.widget.Toast.makeText(c, com.oupao.wiz.BubPos.label(c) + "\u5df2\u6062\u590d\u9ed8\u8ba4", 0).show();
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static void restoreScroll(int p3)
    {
        int[] v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.MainActivity.svPos;
        } catch (Throwable __e___ex) {
            return;
        }
        if ((v0_0 != null) && (p3 < v0_0.length)) {
            var v2 = com.oupao.wiz.MainActivity.sSv;
            if (v2 != null) {
                v2.scrollTo(0, v0_0[p3]);
                v2.getScrollY();
            }
        }
        return;
    }
    public static void roundTo(android.view.View[] p4, boolean p5)
    {
        var v0 = p4.length;
        int v1 = 0;
        while (v1 < v0) {
            var v2 = p4[v1];
            if (v2 != null) {
                if (p5 && (com.oupao.wiz.MainActivity.rdDraw != null)) {
                    v2.setBackground(com.oupao.wiz.MainActivity.rdDraw);
                    v2.setClipToOutline(true);
                } else {
                    v2.setClipToOutline(false);
                    v2.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
                }
            }
            v1++;   // 1820fix: 自增必须在循环尾，否则元素为 null 时主线程死循环（消息提醒页卡死）
        }
        return;
    }
    public static void savePos()
    {
        android.widget.TextView v0 = null;
        try {
            v0 = com.oupao.wiz.MainActivity.posBubble;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 != null) {
            var v1_3 = v0.getLayoutParams();
            if ((v1_3 instanceof android.widget.FrameLayout.LayoutParams)) {
                var v1_7 = ((android.widget.FrameLayout.LayoutParams) v1_3);
                var v2_5 = v1_7.leftMargin;
                var v3_2 = v1_7.topMargin;
                int[] v4_0 = new int[2];
                ((android.view.View) v0.getParent()).getLocationOnScreen(v4_0);
                var v2_0 = (v2_5 + v4_0[0]);
                var v3_0 = (v3_2 + v4_0[1]);
                var v1_1 = new StringBuilder();
                v1_1.append(v2_0);
                // 原版 smali 是 StringBuilder->append(C) 追加字符 ','（0x2c=44），
                // 这里写成 append(44) 会被解析成 append(int) → 追加字符串 "44"，
                // 存成 "60441528" 这种垃圾，applyPos 里 indexOf(',') 找不到 → 永远默认位置。
                v1_1.append(',');
                v1_1.append(v3_0);
                // 竖屏/横屏各存一份，只写"当前方向"那个键
                String posKey = com.oupao.wiz.BubPos.key(v0.getContext());
                com.oupao.wiz.Cfg.saveStr(posKey, v1_1.toString());
                com.oupao.wiz.SLog.i("Pos: save[" + posKey + "] raw=" + v1_1.toString()
                        + " lm=" + v2_5 + " tm=" + v3_2 + " org=" + v4_0[0] + "," + v4_0[1]
                        + " readback=" + com.oupao.wiz.Cfg.getStrFresh(posKey, ""));
                com.oupao.wiz.Cfg.push();
                com.oupao.wiz.SLog.i("Pos: push sent");
                com.oupao.wiz.MainActivity.closePosEditor();
                var v1_4 = com.oupao.wiz.MainActivity.pageBox;
                if (v1_4 != null) {
                    android.widget.Toast.makeText(v1_4.getContext(),
                            com.oupao.wiz.BubPos.label(v0.getContext()) + "\u5df2\u4fdd\u5b58", 0).show();
                }
            } else {
                com.oupao.wiz.SLog.i("Pos: save skipped lp=" + v1_3);
            }
        } else {
            com.oupao.wiz.SLog.i("Pos: save skipped posBubble=null");
        }
        return;
    }
    public static void saveScroll(int p3)
    {
        int[] v0 = null;
        try {
            v0 = com.oupao.wiz.MainActivity.svPos;
        } catch (Throwable __e___ex) {
            return;
        }
        if ((v0 != null) && (p3 < v0.length)) {
            var v1_1 = com.oupao.wiz.MainActivity.sSv;
            if (v1_1 != null) {
                v0[p3] = v1_1.getScrollY();
            }
        }
        return;
    }
    public static String[] scanMusic()
    {
        java.lang.String[] v0 = com.oupao.wiz.MainActivity.sMus;
        if (v0 == null) {
            v0 = com.oupao.wiz.MainActivity.scanMusicNow();
            com.oupao.wiz.MainActivity.sMus = v0;
        }
        return v0;
    }
    public static String[] scanMusicNow()
    {
        String[] v6_5 = null;
        android.content.Context v0 = null;
        try {
            v0 = com.oupao.wiz.Cfg.ctx;
        } catch (Throwable __e_v0) {
            String[] v6_7 = new String[0];
            return v6_7;
        }
        if (v0 == null) {
            v6_5 = new String[0];
        } else {
            var v1 = v0.getPackageManager();
            var v2_1 = new java.util.ArrayList();
            var v3_1 = new java.util.ArrayList();
            com.oupao.wiz.Mus.scanCand(v1, v2_1, v3_1);
            com.oupao.wiz.Mus.scanLabel(v1, v2_1, v3_1);
            var v4_1 = new java.util.ArrayList();
            var v5 = v2_1.size();
            int v6_0 = 0;
            while (v6_0 < v5) {
                var v7_1 = ((String) v2_1.get(v6_0));
                if ((v7_1 != null) && (v1.getLaunchIntentForPackage(v7_1) != null)) {
                    var v8_2 = String.valueOf(v3_1.get(v6_0));
                    var v9_1 = new StringBuilder();
                    v9_1.append(v7_1);
                    v9_1.append("|");
                    v9_1.append(v8_2);
                    v4_1.add(v9_1.toString());
                }
                v6_0++;
            }
            String[] v6_2 = new String[0];
            v6_5 = ((String[]) v4_1.toArray(v6_2));
        }
        return v6_5;
    }
    public static void scrimAt(android.view.View[] p4)
    {
        android.view.View v0 = null;
        try {
            v0 = com.oupao.wiz.MainActivity.scrim;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 != null) {
            int v1 = 0;
            var v2 = p4.length;
            if (v2 != 0) {
                v0.setAlpha(0);
                v0.setVisibility(0);
                v0.bringToFront();
                while (v1 < v2) {
                    var v3_0 = p4[v1];
                    if (v3_0 != null) {
                        v3_0.bringToFront();
                    }
                    v1++;
                }
            }
        }
        return;
    }
    public static void scrimTo(float p3)
    {
        android.view.View v0 = null;
        try {
            v0 = com.oupao.wiz.MainActivity.scrim;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 != null) {
            v0.animate().alpha(p3).setDuration(400).start();
        }
        return;
    }
    public static void setAl(android.view.View[] p4, float p5)
    {
        var v0 = p4.length;
        int v1 = 0;
        while (v1 < v0) {
            var v2 = p4[v1];
            if (v2 != null) {
                v2.animate().cancel();
                v2.setAlpha(p5);
            }
            v1++;
        }
        return;
    }
    public static void setSc(android.view.View[] p4, float p5)
    {
        var v0 = p4.length;
        int v1 = 0;
        while (v1 < v0) {
            var v2 = p4[v1];
            if (v2 != null) {
                v2.animate().cancel();
                v2.setScaleX(p5);
                v2.setScaleY(p5);
            }
            v1++;
        }
        return;
    }
    public static void setTx(android.view.View[] p4, float p5)
    {
        var v0 = p4.length;
        int v1 = 0;
        while (v1 < v0) {
            var v2 = p4[v1];
            if (v2 != null) {
                v2.animate().cancel();
                v2.setTranslationX(p5);
            }
            v1++;
        }
        return;
    }
    public static void setVs(android.view.View[] p3, int p4)
    {
        var v0 = p3.length;
        int v1 = 0;
        while (v1 < v0) {
            var v2 = p3[v1];
            if (v2 != null) {
                v2.setVisibility(p4);
            }
            v1++;
        }
        return;
    }
    public static void settleTf(android.view.View[] p4)
    {
        var v0 = p4.length;
        int v1 = 0;
        while (v1 < v0) {
            var v2 = p4[v1];
            if (v2 != null) {
                v2.animate().cancel();
                v2.setTranslationX(0);
                v2.setAlpha(1.0f);
                v2.setScaleX(1.0f);
                v2.setScaleY(1.0f);
                v2.setClipToOutline(false);
                v2.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
            }
            v1++;
        }
        return;
    }
    public static void show(int p7)
    {
        // 诊断：谁把它切到这一页的（复现"呼出更多应用却跳到别的页"时看这条）
        try {
            var st = Thread.currentThread().getStackTrace();
            var sb = new StringBuilder("show(" + p7 + ") cur=" + com.oupao.wiz.MainActivity.cur + " from");
            int k = 3;
            while ((k < st.length) && (k < 9)) {
                sb.append(" < ").append(st[k].getMethodName());
                k++;
            }
            com.oupao.wiz.SLog.i(sb.toString());
        } catch (Throwable ignore) {
        }
        // Pages that the Miuix shell owns are rendered in Compose. The set is decided by
        // MiuixShell.ownsPage() (roots + every page already in MIGRATED_PAGES), so migrating a page
        // never needs an edit here; remaining legacy pages keep their proven View logic.
        if (com.oupao.wiz.MiuixShell.ownsPage(p7)) {
            com.oupao.wiz.MainActivity.killPend();
            com.oupao.wiz.MainActivity.aOn = false;
            com.oupao.wiz.MainActivity.aNext = p7;
            com.oupao.wiz.MainActivity.cur = p7;
            var sv = com.oupao.wiz.MainActivity.pgSv;
            if (sv != null) {
                for (var v : sv) {
                    if (v != null) {
                        v.animate().cancel();
                        v.setVisibility(8);
                        v.setTranslationX(0);
                        v.setAlpha(1.0f);
                        v.setScaleX(1.0f);
                        v.setScaleY(1.0f);
                        v.setClipToOutline(false);
                    }
                }
            }
            MiuixShell.sync();
            MiuixShell.onLogVisibility(p7 == 5, logCenter);
            // Do not call pageShow() here: it would re-show the legacy View page
            // underneath the Miuix host. The three roots are Compose-owned now.
            try { com.oupao.wiz.PinUI.rail(pageBox.getContext(), null); } catch (Throwable ignore) { }
            return;
        }
        try {
            com.oupao.wiz.MainActivity.toolSync(p7);
            com.oupao.wiz.MainActivity.cur = p7;
            MiuixShell.sync();
            MiuixShell.onLogVisibility(p7 == 5, logCenter);
            com.oupao.wiz.MainActivity.postScroll(p7);
            com.oupao.wiz.MainActivity.pageShow(p7);
            com.oupao.wiz.MainActivity.scrimAt(com.oupao.wiz.MainActivity.viewsOf(p7));
            com.oupao.wiz.MainActivity.restoreScroll(p7);
            // 索引条：只有「固定应用」(9) 和「更多应用」(10) 两页需要；其它页摘掉。
            // 放在 show() 里是因为这里才是页面真正可见的时机（buildPick 可能早于内容视图存在）。
            try {
                var page = (p7 == 9) ? com.oupao.wiz.MainActivity.pgPins : null;
                var c = (com.oupao.wiz.MainActivity.pageBox == null)
                        ? null : com.oupao.wiz.MainActivity.pageBox.getContext();
                if (c != null) {
                    com.oupao.wiz.PinUI.rail(c, page);
                    com.oupao.wiz.SLog.i("Rail: page=" + p7 + " rail=" + (page != null ? 1 : 0));
                }
            } catch (Throwable ignore) {
            }
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static void slide(int p10, boolean p11)
    {
        int v0 = 0;
        try {
            if (com.oupao.wiz.MainActivity.sSv != null) {
                com.oupao.wiz.MainActivity.saveScroll(com.oupao.wiz.MainActivity.cur);
            }
        } catch (Throwable __e_v0) {
            com.oupao.wiz.Cfg.dbg("slide-ex");
            com.oupao.wiz.MainActivity.show(p10);
            return;
        }
        com.oupao.wiz.MainActivity.restoreScroll(p10);
        if (p10 == 0) {
            com.oupao.wiz.MainActivity.toolSync(0);
        }
        v0 = com.oupao.wiz.MainActivity.aNext;
        com.oupao.wiz.MainActivity.aNext = p10;
        com.oupao.wiz.MainActivity.logSt("slide");
        if ((v0 != p10) || (com.oupao.wiz.MainActivity.aPend == null)) {
            if (v0 != com.oupao.wiz.MainActivity.cur) {
                com.oupao.wiz.MainActivity.aNext = v0;
                com.oupao.wiz.MainActivity.navEnd();
                com.oupao.wiz.MainActivity.aNext = p10;
            }
            if (v0 == p10) {
                com.oupao.wiz.MainActivity.show(p10);
            } else {
                var v1_0 = com.oupao.wiz.MainActivity.viewsOf(v0);
                var v2 = com.oupao.wiz.MainActivity.viewsOf(p10);
                com.oupao.wiz.MainActivity.aFrom = v1_0;
                com.oupao.wiz.MainActivity.aTo = v2;
                if ((v1_0.length == 0) || (v2.length == 0)) {
                    com.oupao.wiz.MainActivity.show(p10);
                    return;
                } else {
                    var v3_10 = ((float) com.oupao.wiz.Cfg.ctx.getResources().getDisplayMetrics().widthPixels);
                    com.oupao.wiz.MainActivity.setVs(v2, 0);
                    if (!p11) {
                        com.oupao.wiz.MainActivity.setTx(v1_0, 0);
                        com.oupao.wiz.MainActivity.setAl(v1_0, 1.0f);
                        com.oupao.wiz.MainActivity.setSc(v1_0, 1.0f);
                        com.oupao.wiz.MainActivity.setTx(v2, 0);
                        com.oupao.wiz.MainActivity.setAl(v2, 1.0f);
                        com.oupao.wiz.MainActivity.setSc(v2, 1.0f);
                        com.oupao.wiz.MainActivity.frontTo(v2);
                        com.oupao.wiz.MainActivity.scrimAt(v1_0);
                        com.oupao.wiz.MainActivity.roundTo(v1_0, true);
                        com.oupao.wiz.MainActivity.scrim.setAlpha(0.3f);
                        com.oupao.wiz.MainActivity.scrimTo(0);
                        com.oupao.wiz.MainActivity.animTo(v1_0, v3_10, 1.0f, 1.0f);
                    } else {
                        com.oupao.wiz.MainActivity.setTx(v1_0, 0);
                        com.oupao.wiz.MainActivity.setAl(v1_0, 1.0f);
                        com.oupao.wiz.MainActivity.setSc(v1_0, 1.0f);
                        com.oupao.wiz.MainActivity.setTx(v2, v3_10);
                        com.oupao.wiz.MainActivity.setAl(v2, 1.0f);
                        com.oupao.wiz.MainActivity.setSc(v2, 1.0f);
                        com.oupao.wiz.MainActivity.scrimAt(v2);
                        com.oupao.wiz.MainActivity.roundTo(v2, true);
                        com.oupao.wiz.MainActivity.animTo(v2, 0, 1.0f, 1.0f);
                        com.oupao.wiz.MainActivity.scrimTo(0.3f);
                    }
                    var v6_5 = new android.os.Handler(android.os.Looper.getMainLooper());
                    var v7_3 = new com.oupao.wiz.MA$BN();
                    v6_5.postDelayed(v7_3, 600);
                    com.oupao.wiz.MainActivity.aPend = v7_3;
                }
            }
            return;
        } else {
            return;
        }
    }
    public static void sugTop(android.widget.LinearLayout p4, int p5)
    {
        try {
            if (p4 != null) {
                var v0_1 = p4.getLayoutParams();
                if (v0_1 != null) {
                    var v2 = com.oupao.wiz.Ui.dp(p4.getContext(), p5);
                    if ((v0_1 instanceof android.widget.LinearLayout.LayoutParams)) {
                        var v0_0 = ((android.widget.LinearLayout.LayoutParams) v0_1);
                        v0_0.topMargin = v2;
                        p4.setLayoutParams(v0_0);
                    }
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static void toolSync(int p2)
    {
        android.view.View v0 = null;
        try {
            v0 = com.oupao.wiz.MainActivity.sTool;
        } catch (Throwable __e___ex) {
            return;
        }
        if (v0 == null) {
            return;
        } else {
            var v1 = 8;
            if (p2 == 0) {
                v1 = 0;
            }
            v0.setVisibility(v1);
            return;
        }
    }
    public static void unregBack(android.app.Activity p4)
    {
        java.lang.Object v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.MainActivity.backCb;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_0 != null) {
            var v1 = p4.getOnBackInvokedDispatcher();
            if (v1 != null) {
                v1.unregisterOnBackInvokedCallback(((android.window.OnBackInvokedCallback) v0_0));
            }
            com.oupao.wiz.MainActivity.backCb = null;
        }
        return;
    }
    public static android.view.View[] viewsOf(int p6)
    {
        android.view.View[] v1 = new android.view.View[0];
        var v2 = com.oupao.wiz.MainActivity.pgList;
        if ((v2 == null) || ((p6 < 0) || (p6 >= v2.length))) {
            return v1;
        } else {
            android.view.View[] v3_2 = new android.view.View[2];
            var v4_0 = com.oupao.wiz.MainActivity.bgList;
            if (v4_0 != null) {
                v3_2[0] = v4_0[p6];
            }
            var v4_1 = com.oupao.wiz.MainActivity.pgSv;
            if (v4_1 != null) {
                v3_2[1] = v4_1[p6];
            }
            return v3_2;
        }
    }
    public static void wrapPage(android.view.View p7, int p8)
    {
        try {
            if (p7 != null) {
                var v0_0 = p7.getParent();
                if ((v0_0 != null) && ((v0_0 instanceof android.view.ViewGroup))) {
                    var v0_4 = ((android.view.ViewGroup) v0_0);
                    var v2 = v0_4.indexOfChild(p7);
                    v0_4.removeView(p7);
                    var v3_1 = new android.widget.ScrollView(p7.getContext());
                    v3_1.setFillViewport(false);
                    v3_1.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
                    v3_1.setVisibility(8);
                    v3_1.addView(p7, new android.widget.FrameLayout.LayoutParams(-1, -2));
            v0_4.addView(v3_1, v2, new android.widget.FrameLayout.LayoutParams(-1, -1));
                    if (p8 != 5) {
                        // 小窗模式（desk）的「更多应用」页底部不需要 108dp 的留白
                        // （那 108dp 是给主页底部栏留的），否则内容短时底部一大块空白。
                        int v1_2 = 108;
                        if ((p8 == 9) && com.oupao.wiz.MainActivity.deskOn) {
                            v1_2 = 12;
                        }
                        v3_1.setPadding(0, 0, 0, com.oupao.wiz.Ui.dp(p7.getContext(), v1_2));
                    }
                    var v0_1 = com.oupao.wiz.MainActivity.pgSv;
                    if (v0_1 != null) {
                        v0_1[p8] = v3_1;
                    }
                }
            }
        } catch (Throwable __e_v0_2) {
            com.oupao.wiz.SLog.i(__e_v0_2.toString());
            return;
        }
        return;
    }
    public static void wrapPages()
    {
        android.view.View[] v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.MainActivity.pgList;
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.SLog.i(__e_v0_1.toString());
            return;
        }
        if (v0_0 != null) {
            var v1 = v0_0.length;
            int v2 = 0;
            while (v2 < v1) {
                var v3 = v0_0[v2];
                if (v3 != null) {
                    com.oupao.wiz.MainActivity.wrapPage(v3, v2);
                }
                v2++;
            }
        }
        return;
    }
    public boolean dispatchTouchEvent(android.view.MotionEvent p2)
    {
        return super.dispatchTouchEvent(p2);
    }

    /** 系统暗色档位/模式变化时，实时刷新界面颜色 */
    public void onConfigurationChanged(android.content.res.Configuration cfg)
    {
        super.onConfigurationChanged(cfg);
        try {
            com.oupao.wiz.Ui.nightFlag = (((cfg.uiMode & 48) == 32) ? 1 : 0);
            com.oupao.wiz.Ui.appCtx = this;
        } catch (Throwable ignore) {
        }
        try {
            recreate();     // 重建界面，让所有颜色按新档位重新计算
        } catch (Throwable ignore) {
        }
    }
    public void onBackPressed()
    {
        if (!com.oupao.wiz.MainActivity.posOn()) {
            if (!com.oupao.wiz.MainActivity.deskOn) {
                com.oupao.wiz.MainActivity.backAnim();
                return;
            } else {
                com.oupao.wiz.MainActivity.finishNow();
                return;
            }
        } else {
            com.oupao.wiz.MainActivity.closePosEditor();
            return;
        }
    }
    protected void onCreate(android.os.Bundle p13)
    {
        // 深链：别的进程（ColorOS「通知与控制中心」里那行「通知收纳」）带 oupao_page=<页 id> 进来
        try {
            android.content.Intent it0 = getIntent();
            if (it0 != null) {
                int pg = it0.getIntExtra("oupao_page", 0);
                if (pg > 0) {
                    com.oupao.wiz.MiuixShell.openPage(pg);
                }
            }
        } catch (Throwable ignore) {
        }
        // 让系统主题跟随日夜（未显式设背景的 View 会继承主题底色，暗色下才不会是白色）
        try {
            if (com.oupao.wiz.Ui.night()) {
                setTheme(android.R.style.Theme_DeviceDefault_NoActionBar);
            }
        } catch (Throwable ignore) {
        }
        try {
            com.oupao.wiz.Ui.appCtx = this;
        } catch (Throwable ignore) {
        }
        // 「移除应用启动遮罩」早期版本被误当成默认开启，这里一次性纠正为关闭
        try {
            com.oupao.wiz.Cfg.resetNoSplashOnce();
        } catch (Throwable ignore) {
        }
        // 3116：旧版本升级后一次性把所有功能开关改为关闭
        try {
            com.oupao.wiz.Cfg.applyDefaultsOffOnce();
        } catch (Throwable ignore) {
        }
        // 3140：通知收纳两个 key 曾被测试写成 1（notifBox() 是 Global 优先 → 看起来像默认开），
        // 升级后一次性把三路（App 文件 / Settings.Global / 系统侧文件）复位成关闭。
        try {
            com.oupao.wiz.Cfg.resetNotifBoxOffOnce();
        } catch (Throwable ignore) {
        }
        // 每次打开模块都按配置同步一次桌面图标（禁用/启用 LauncherAlias）。
        // 只应用状态、不写配置：走 applyHideIcon() 会形成 save→apply→save 的无尾递归。
        try {
            com.oupao.wiz.Cfg.applyHideIconState(com.oupao.wiz.Cfg.hideIcon());
        } catch (Throwable ignore) {
        }

        // 监听系统暗色档位设置，实时重建界面
        try {
            var obs = new android.database.ContentObserver(
                    new android.os.Handler(android.os.Looper.getMainLooper())) {
                public void onChange(boolean self) {
                    try {
                        recreate();
                    } catch (Throwable ignore) {
                    }
                }
            };
            var cr = getContentResolver();
            cr.registerContentObserver(
                    android.provider.Settings.Global.getUriFor("DarkMode_BackgroundMaxL"), false, obs);
            cr.registerContentObserver(
                    android.provider.Settings.Global.getUriFor("DarkMode_DialogBgMaxL"), false, obs);
            cr.registerContentObserver(
                    android.provider.Settings.System.getUriFor("DarkMode_style_key"), false, obs);
        } catch (Throwable ignore) {
        }
        try {
            com.oupao.wiz.Ui.nightFlag = (((getResources().getConfiguration().uiMode & 48) == 32) ? 1 : 0);
        } catch (Throwable ignore) {
        }
        com.oupao.wiz.Tm.reset();
        com.oupao.wiz.Tm.mark();
        com.oupao.wiz.MainActivity.prepDirs();
        com.oupao.wiz.Tm.log("oc.dirs");
        com.oupao.wiz.Cfg.setCtx(this);
        // 1820fix: 窗口底色跟随主题（返回动画期间露出的就是它），避免露出卡片色
        try {
            getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(com.oupao.wiz.Ui.pageBg()));
        } catch (Throwable ignore) {
        }
        try {
            com.oupao.wiz.Cfg.inv();   // 打开菜单管理页：清缓存，重新读最新配置
        } catch (Throwable ignore) {
        }
        try {
            com.oupao.wiz.Cfg.ctx = this.getApplicationContext();   // App 进程的 Context（Settings.Global 读写需要）
        } catch (Throwable ignore) {
        }
        com.oupao.wiz.Cfg.pushLater();
        com.oupao.wiz.Tm.log("oc.push");
        super.onCreate(p13);
        var v0_6 = this.getIntent();
        if ((v0_6 != null) && (v0_6.getBooleanExtra("diag", false))) {
            com.oupao.wiz.Diag.showDiag(this);
        }
        var v0_9 = this.getIntent();
        if (v0_9 != null) {
            int v2_34 = 0;
            com.oupao.wiz.MainActivity.navReq = v0_9.getIntExtra("oupao_nav", -1);
            com.oupao.wiz.MainActivity.deskOn = v0_9.getBooleanExtra("oupao_desk", false);
            com.oupao.wiz.MainActivity.deskPins = v0_9.getStringExtra("oupao_pins");
        com.oupao.wiz.Pins.deskMode = com.oupao.wiz.MainActivity.deskOn;   // 2841：小窗页读写自己那套
            // 「呼出方式」切换的悬浮提示：由本页（模块 App）弹，
            // 保证「面板→小窗」和「小窗→面板」两次提示的样式（圆角等）完全一致。
            try {
                var vToast = v0_9.getStringExtra("oupao_toast");
                if ((vToast != null) && (vToast.length() > 0)) {
                    final String vT = vToast;
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                            new Runnable() {
                                public void run() {
                                    try {
                                        android.widget.Toast.makeText(com.oupao.wiz.MainActivity.inst,
                                                vT, android.widget.Toast.LENGTH_SHORT).show();
                                    } catch (Throwable ignore) {
                                    }
                                }
                            }, 350L);
                }
            } catch (Throwable ignore) {
            }
            var v1_17 = new StringBuilder();
            v1_17.append("onCreate desk=");
            v1_17.append(com.oupao.wiz.MainActivity.deskOn);
            v1_17.append(" pins=");
            var v2_33 = com.oupao.wiz.MainActivity.deskPins;
            if (v2_33 == null) {
                v2_34 = -1;
            } else {
                v2_34 = v2_33.length();
            }
            v1_17.append(v2_34);
            com.oupao.wiz.MainActivity.dlog(v1_17.toString());
        }
        var v1_19 = this.getActionBar();
        if (v1_19 != null) {
            v1_19.hide();
        }
        var v1_20 = this.getWindow();
        v1_20.setStatusBarColor(com.oupao.wiz.Ui.pageBg());
        v1_20.getDecorView().setSystemUiVisibility(8192);
        var v0_14 = new android.widget.LinearLayout(this);
        v0_14.setOrientation(1);
        v0_14.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
        this.root = v0_14;
        com.oupao.wiz.MainActivity.sSv = null;
        var v1_27 = v0_14;
        int[] v2_54 = new int[16];
        com.oupao.wiz.MainActivity.svPos = v2_54;
        android.widget.ScrollView[] v3_64 = new android.widget.ScrollView[16];
        com.oupao.wiz.MainActivity.pgSv = v3_64;
        this.setContentView(v0_14);
        androidx.lifecycle.ViewTreeLifecycleOwner.set(getWindow().getDecorView(), this);
        androidx.savedstate.ViewTreeSavedStateRegistryOwner.set(getWindow().getDecorView(), this);
        this.getWindow().getDecorView();
        var v6_57 = new android.widget.TextView(this);
        v6_57.setText("\ud83d\udd27");
        v6_57.setGravity(17);
        v6_57.setTextSize(16.0f);
        var v8_19 = new android.widget.FrameLayout.LayoutParams(com.oupao.wiz.Ui.dp(this, 20), com.oupao.wiz.Ui.dp(this, 20));
        v8_19.gravity = 53;
        v8_19.rightMargin = com.oupao.wiz.Ui.dp(this, 24);
        v8_19.topMargin = com.oupao.wiz.Ui.dp(this, 78);
        v6_57.setLayoutParams(v8_19);
        v6_57.setBackground(null);
        v6_57.setOnClickListener(new com.oupao.wiz.MainActivity$C6());
        com.oupao.wiz.MainActivity.sTool = v6_57;
        var v2_12 = this.getWindow();
        v2_12.setStatusBarColor(0);
        v2_12.getDecorView().setSystemUiVisibility(9472);
        var v2_15 = this.getResources();
        // 顶部安全高度：edge-to-edge 之后优先用真实的系统栏 inset（横屏状态栏隐藏时会是 0），
        // 拿不到再退回 status_bar_height 这个固定 dimen
        int v2_sbTop = 0;
        try {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                var im = this.getWindowManager().getCurrentWindowMetrics().getWindowInsets();
                v2_sbTop = im.getInsets(android.view.WindowInsets.Type.systemBars()).top;
            }
        } catch (Throwable ignore) {
        }
        com.oupao.wiz.MainActivity.sbH = (v2_sbTop > 0) ? v2_sbTop
                : v2_15.getDimensionPixelSize(v2_15.getIdentifier("status_bar_height", "dimen", "android"));
        // 真正的 edge-to-edge：让窗口铺到状态栏/挖孔底下。
        // 实测窗口 appBounds 是 (0,135)-(1216,2640)：那条 135px 不归应用画。
        // 竖屏靠 setStatusBarColor 涂成页面底色看不出来，横屏状态栏一隐藏就露出系统黑底（"黑边"）。
        // 本模块界面本来就是按 edge-to-edge 写的（页面自己加了 sbH 上内边距），放开即可；
        // 另外必须允许画进挖孔区（LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES），否则挖孔那条仍然留黑。
        try {
            var w = this.getWindow();
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                w.setDecorFitsSystemWindows(false);
            } else {
                w.getDecorView().setSystemUiVisibility(
                        9472
                        | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
            }
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                var lp = w.getAttributes();
                lp.layoutInDisplayCutoutMode =
                        android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                w.setAttributes(lp);
            }
        } catch (Throwable ignore) {
        }
        com.oupao.wiz.MainActivity.rdDraw = com.oupao.wiz.Ui.bg(com.oupao.wiz.Ui.pageBg(), ((float) com.oupao.wiz.Ui.dp(this, 42)));
        var v2_20 = com.oupao.wiz.Ui.t(this, "\u6b27\u6ce1\u5c0f\u7a97\u4f18\u5316", 26.0f, com.oupao.wiz.Ui.textMain(), true);
        com.oupao.wiz.Ui.pad(v2_20, 18, 20, 18, 0);
        com.oupao.wiz.MainActivity.sTitle = v2_20;
        var v2_22 = com.oupao.wiz.Ui.t(this, "\u4f5c\u8005\uff1a\u559b\u96ea v3.0", 13.0f, com.oupao.wiz.Ui.textSub(), false);
        com.oupao.wiz.Ui.pad(v2_22, 18, 6, 18, 10);
        com.oupao.wiz.MainActivity.sSub = v2_22;
        var v2_23 = com.oupao.wiz.Ui.card(this);
        com.oupao.wiz.MainActivity.sLog = v2_23;
        var v3_24 = com.oupao.wiz.Ui.t(this, "\u65e5\u5fd7\uff1a/data/system/oupao.log", 12.0f, com.oupao.wiz.Ui.textSub(), false);
        com.oupao.wiz.Ui.pad(v3_24, 20, 4, 20, 0);
        v2_23.addView(v3_24);
        var v2_25 = com.oupao.wiz.Ui.t(this, "Flyme\u5c0f\u7a97", 22.0f, com.oupao.wiz.Ui.textMain(), true);
        com.oupao.wiz.Ui.pad(v2_25, 8, 4, 18, 4);
        com.oupao.wiz.MainActivity.sL = v2_25;
        var v2_27 = com.oupao.wiz.Ui.card(this);
        com.oupao.wiz.MainActivity.sM = v2_27;
        var v3_30 = new android.widget.LinearLayout(this);
        v3_30.setOrientation(0);
        v3_30.setGravity(16);
        v3_30.setPadding(com.oupao.wiz.Ui.dp(this, 20), com.oupao.wiz.Ui.dp(this, 13), com.oupao.wiz.Ui.dp(this, 20), com.oupao.wiz.Ui.dp(this, 13));
        var v4_46 = com.oupao.wiz.Ui.t(this, "Flyme\u5c0f\u7a97", 18.0f, com.oupao.wiz.Ui.textMain(), true);
        v4_46.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        v3_30.addView(v4_46);
        v3_30.addView(com.oupao.wiz.Ui.t(this, "\u203a", 28.0f, com.oupao.wiz.Ui.textSub(), true));
        com.oupao.wiz.Ui.ripple(v3_30);
        v3_30.setOnClickListener(new com.oupao.wiz.MainActivity$C3());
        v2_27.addView(v3_30);
        var v3_32 = new android.widget.LinearLayout(this);
        v3_32.setOrientation(0);
        v3_32.setGravity(16);
        v3_32.setPadding(com.oupao.wiz.Ui.dp(this, 20), com.oupao.wiz.Ui.dp(this, 13), com.oupao.wiz.Ui.dp(this, 20), com.oupao.wiz.Ui.dp(this, 13));
        var v4_56 = com.oupao.wiz.Ui.t(this, "Flyme\u83dc\u5355", 18.0f, com.oupao.wiz.Ui.textMain(), true);
        v4_56.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        v3_32.addView(v4_56);
        v3_32.addView(com.oupao.wiz.Ui.t(this, "\u203a", 28.0f, com.oupao.wiz.Ui.textSub(), true));
        com.oupao.wiz.Ui.ripple(v3_32);
        v3_32.setOnClickListener(new com.oupao.wiz.FlyE());
        v2_27.addView(v3_32);
        var v3_34 = new android.widget.LinearLayout(this);
        v3_34.setOrientation(0);
        v3_34.setGravity(16);
        v3_34.setPadding(com.oupao.wiz.Ui.dp(this, 20), com.oupao.wiz.Ui.dp(this, 13), com.oupao.wiz.Ui.dp(this, 20), com.oupao.wiz.Ui.dp(this, 13));
        var v4_66 = com.oupao.wiz.Ui.t(this, "FlymeUI", 18.0f, com.oupao.wiz.Ui.textMain(), true);
        v4_66.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        v3_34.addView(v4_66);
        v3_34.addView(com.oupao.wiz.Ui.t(this, "\u203a", 28.0f, com.oupao.wiz.Ui.textSub(), true));
        com.oupao.wiz.Ui.ripple(v3_34);
        v3_34.setOnClickListener(new com.oupao.wiz.FlyUIE());
        v2_27.addView(v3_34);
        var v3_36 = new android.widget.LinearLayout(this);
        v3_36.setOrientation(0);
        v3_36.setGravity(16);
        v3_36.setPadding(com.oupao.wiz.Ui.dp(this, 20), com.oupao.wiz.Ui.dp(this, 13), com.oupao.wiz.Ui.dp(this, 20), com.oupao.wiz.Ui.dp(this, 13));
        var v2_30 = com.oupao.wiz.Ui.card(this);
        com.oupao.wiz.MainActivity.sGame = v2_30;
        var v3_38 = new android.widget.LinearLayout(this);
        v3_38.setOrientation(0);
        v3_38.setGravity(16);
        v3_38.setPadding(com.oupao.wiz.Ui.dp(this, 20), com.oupao.wiz.Ui.dp(this, 13), com.oupao.wiz.Ui.dp(this, 20), com.oupao.wiz.Ui.dp(this, 13));
        var v4_80 = com.oupao.wiz.Ui.t(this, "\u6e38\u620f\u6a21\u5f0f", 18.0f, com.oupao.wiz.Ui.textMain(), true);
        v4_80.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        v3_38.addView(v4_80);
        v3_38.addView(com.oupao.wiz.Ui.t(this, "\u203a", 28.0f, com.oupao.wiz.Ui.textSub(), true));
        com.oupao.wiz.Ui.ripple(v3_38);
        v3_38.setOnClickListener(new com.oupao.wiz.GameE());
        v2_30.addView(v3_38);
        var v9_0 = com.oupao.wiz.Ui.card(this);
        com.oupao.wiz.MainActivity.sVib = v9_0;
        var v3_40 = new android.widget.LinearLayout(this);
        v3_40.setOrientation(0);
        v3_40.setGravity(16);
        v3_40.setPadding(com.oupao.wiz.Ui.dp(this, 20), com.oupao.wiz.Ui.dp(this, 13), com.oupao.wiz.Ui.dp(this, 20), com.oupao.wiz.Ui.dp(this, 13));
        var v4_90 = com.oupao.wiz.Ui.t(this, "\u632f\u52a8\u6548\u679c", 18.0f, com.oupao.wiz.Ui.textMain(), true);
        v4_90.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        v3_40.addView(v4_90);
        v3_40.addView(com.oupao.wiz.Ui.t(this, "\u203a", 28.0f, com.oupao.wiz.Ui.textSub(), true));
        com.oupao.wiz.Ui.ripple(v3_40);
        v3_40.setOnClickListener(new com.oupao.wiz.MA$V());
        v9_0.addView(v3_40);
        var v3_42 = new android.widget.LinearLayout(this);
        v3_42.setOrientation(0);
        v3_42.setGravity(16);
        v3_42.setPadding(com.oupao.wiz.Ui.dp(this, 20), com.oupao.wiz.Ui.dp(this, 13), com.oupao.wiz.Ui.dp(this, 20), com.oupao.wiz.Ui.dp(this, 13));
        var v4_100 = com.oupao.wiz.Ui.t(this, "\u8bca\u65ad\u4e0e\u65e5\u5fd7", 18.0f, com.oupao.wiz.Ui.textMain(), true);
        v4_100.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.0f));
        v3_42.addView(v4_100);
        v3_42.addView(com.oupao.wiz.Ui.t(this, "\u203a", 28.0f, com.oupao.wiz.Ui.textSub(), true));
        com.oupao.wiz.Ui.ripple(v3_42);
        v3_42.setOnClickListener(new com.oupao.wiz.MA$LGX());
        var v9_1 = com.oupao.wiz.Ui.card(this);
        com.oupao.wiz.MainActivity.sDiag = v9_1;
        v9_1.addView(v3_42);
        var v2_43 = com.oupao.wiz.Ui.t(this, "\u2190", 20.0f, -15000801, false);
        com.oupao.wiz.Ui.pad(v2_43, 18, 0, 0, 0);
        v2_43.setOnClickListener(new com.oupao.wiz.MainActivity$C5());
        com.oupao.wiz.MainActivity.sK = v2_43;
        var v2_45 = com.oupao.wiz.Ui.t(this, "\u9009\u62e9\u97f3\u4e50 App", 22.0f, com.oupao.wiz.Ui.textMain(), true);
        com.oupao.wiz.Ui.pad(v2_45, 8, 4, 18, 4);
        com.oupao.wiz.MainActivity.sU = v2_45;
        v1_27.addView(v2_45);
        com.oupao.wiz.MainActivity.sQ = com.oupao.wiz.Ui.card(this);
        var v0_12 = new android.widget.FrameLayout(this);
        com.oupao.wiz.MainActivity.pageBox = v0_12;
        var v1_23 = new android.widget.FrameLayout.LayoutParams(-1, -1);
        var v4_126 = new android.view.View(this);
        v4_126.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
        v0_12.addView(v4_126, 0, v1_23);
        com.oupao.wiz.MainActivity.bg0 = v4_126;
        var v4_128 = new android.view.View(this);
        v4_128.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
        v0_12.addView(v4_128, 0, v1_23);
        com.oupao.wiz.MainActivity.bg1 = v4_128;
        var v4_130 = new android.view.View(this);
        v4_130.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
        v0_12.addView(v4_130, 0, v1_23);
        com.oupao.wiz.MainActivity.bg2 = v4_130;
        var v4_132 = new android.view.View(this);
        v4_132.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
        v0_12.addView(v4_132, 0, v1_23);
        com.oupao.wiz.MainActivity.bg3 = v4_132;
        var v4_134 = new android.view.View(this);
        v4_134.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
        v0_12.addView(v4_134, 0, v1_23);
        com.oupao.wiz.MainActivity.bg4 = v4_134;
        var v4_136 = new android.view.View(this);
        v4_136.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
        v0_12.addView(v4_136, 0, v1_23);
        com.oupao.wiz.MainActivity.bg5 = v4_136;
        var v4_138 = new android.view.View(this);
        v4_138.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
        v0_12.addView(v4_138, 0, v1_23);
        com.oupao.wiz.MainActivity.bg6 = v4_138;
        var v4_140 = new android.view.View(this);
        v4_140.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
        v0_12.addView(v4_140, 0, v1_23);
        com.oupao.wiz.MainActivity.bg7 = v4_140;
        var v4_142 = new android.view.View(this);
        v4_142.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
        v0_12.addView(v4_142, 0, v1_23);
        com.oupao.wiz.MainActivity.bg8 = v4_142;
        var v4_144 = new android.view.View(this);
        v4_144.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
        v0_12.addView(v4_144, 0, v1_23);
        com.oupao.wiz.MainActivity.bg9 = v4_144;
        var v4_146 = new android.view.View(this);
        v4_146.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
        v0_12.addView(v4_146, 0, v1_23);
        com.oupao.wiz.MainActivity.bg10 = v4_146;
        var v4_148 = new android.view.View(this);
        v4_148.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
        v0_12.addView(v4_148, 0, v1_23);
        com.oupao.wiz.MainActivity.bg11 = v4_148;
        var v4_150 = new android.view.View(this);
        v4_150.setBackgroundColor(com.oupao.wiz.Ui.pageBg());
        v0_12.addView(v4_150, 0, v1_23);
        com.oupao.wiz.MainActivity.bg12 = v4_150;
        var v4_152 = new android.widget.LinearLayout(this);
        v4_152.setOrientation(1);
        com.oupao.wiz.MainActivity.pg0 = v4_152;
        v0_12.addView(v4_152, new android.widget.FrameLayout.LayoutParams(-1, -2));
        com.oupao.wiz.Ui.pad(v4_152, 0, 10, 0, 0);
        v4_152.addView(com.oupao.wiz.MainActivity.sTitle, new android.widget.LinearLayout.LayoutParams(-1, -2));
        var v5_116 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        v5_116.bottomMargin = com.oupao.wiz.Ui.dp(this, 26);
        v4_152.addView(com.oupao.wiz.MainActivity.sSub, v5_116);
        var v4_154 = new android.view.View(this);
        v4_154.setBackgroundColor(-16777216);
        v4_154.setAlpha(0);
        v0_12.addView(v4_154, v1_23);
        com.oupao.wiz.MainActivity.scrim = v4_154;
        v0_12.postDelayed(new com.oupao.wiz.MA$PZ(), 800);
        var v3_62 = new android.widget.LinearLayout(this);
        v3_62.setOrientation(1);
        com.oupao.wiz.MainActivity.pg3 = v3_62;
        var v4_160 = new android.widget.LinearLayout(this);
        v4_160.setOrientation(1);
        com.oupao.wiz.MainActivity.pageLog = v4_160;
        var v4_162 = new android.widget.LinearLayout(this);
        v4_162.setOrientation(1);
        com.oupao.wiz.MainActivity.pageSug = v4_162;
        var v4_164 = new android.widget.LinearLayout(this);
        v4_164.setOrientation(1);
        com.oupao.wiz.MainActivity.pg7 = v4_164;
        com.oupao.wiz.Ui.pad(v4_164, 0, 0, 0, 0);
        com.oupao.wiz.MainActivity.buildAicMsgCard(this);
        com.oupao.wiz.MainActivity.movePage(com.oupao.wiz.MainActivity.pg0, com.oupao.wiz.MainActivity.sM);
        com.oupao.wiz.MainActivity.movePage(com.oupao.wiz.MainActivity.pg0, com.oupao.wiz.MainActivity.sAic);
        com.oupao.wiz.MainActivity.movePage(com.oupao.wiz.MainActivity.pg0, com.oupao.wiz.MainActivity.sGame);
        com.oupao.wiz.MainActivity.movePage(com.oupao.wiz.MainActivity.pg0, com.oupao.wiz.MainActivity.sVib);
        com.oupao.wiz.MainActivity.movePage(com.oupao.wiz.MainActivity.pg0, com.oupao.wiz.MainActivity.sDiag);
        var v4_170 = com.oupao.wiz.MainActivity.pageBox;
        var v6_53 = com.oupao.wiz.MainActivity.pageLog;
        var v5_138 = com.oupao.wiz.Ui.t(this, "\u8bca\u65ad\u4e0e\u65e5\u5fd7", 22.0f, com.oupao.wiz.Ui.textMain(), true);
        com.oupao.wiz.Ui.pad(v5_138, 8, 4, 18, 4);
        v6_53.addView(v5_138);
        var v1_37 = new android.widget.LinearLayout(this);
        v1_37.setOrientation(0);
        v1_37.setGravity(16);
        v1_37.setPadding(0, com.oupao.wiz.MainActivity.sbH, 0, 0);
        v1_37.addView(com.oupao.wiz.MainActivity.mkArrow(this));
        com.oupao.wiz.MainActivity.movePage(v1_37, v5_138);
        v6_53.addView(v1_37, 0);
        var v5_140 = com.oupao.wiz.Ui.card(this);
        com.oupao.wiz.MainActivity.logCard = v5_140;
        v6_53.addView(v5_140);
        com.oupao.wiz.MainActivity.buildLog();
        com.oupao.wiz.MainActivity.buildDiagCard(this, v6_53);
        var v6_55 = com.oupao.wiz.MainActivity.pg3;
        com.oupao.wiz.MainActivity.movePage(v6_55, com.oupao.wiz.MainActivity.sU);
        com.oupao.wiz.MainActivity.movePage(v6_55, com.oupao.wiz.MainActivity.sQ);
        v4_170.addView(com.oupao.wiz.MainActivity.pg3, new android.widget.FrameLayout.LayoutParams(-1, -2));
        v4_170.addView(com.oupao.wiz.MainActivity.pageLog, new android.widget.FrameLayout.LayoutParams(-1, -2));
        v4_170.addView(com.oupao.wiz.MainActivity.pageSug, new android.widget.FrameLayout.LayoutParams(-1, -2));
        v4_170.addView(com.oupao.wiz.MainActivity.pg7, new android.widget.FrameLayout.LayoutParams(-1, -2));
        var v5_156 = new android.widget.LinearLayout(this);
        v5_156.setOrientation(1);
        com.oupao.wiz.MainActivity.pgPins = v5_156;
        v4_170.addView(v5_156, new android.widget.FrameLayout.LayoutParams(-1, -2));
        var v6_85 = com.oupao.wiz.MainActivity.pageSug;
        var v5_160 = com.oupao.wiz.Ui.t(this, "\u6c14\u6ce1\u751f\u6548\u5e94\u7528", 22.0f, com.oupao.wiz.Ui.textMain(), true);
        com.oupao.wiz.Ui.pad(v5_160, 8, 4, 18, 4);
        com.oupao.wiz.MainActivity.pgSugTitle = v5_160;
        v6_85.addView(v5_160);
        var v1_41 = new android.widget.LinearLayout(this);
        v1_41.setOrientation(0);
        v1_41.setGravity(16);
        v1_41.setPadding(0, com.oupao.wiz.MainActivity.sbH, 0, 0);
        v1_41.addView(com.oupao.wiz.MainActivity.mkArrow(this));
        com.oupao.wiz.MainActivity.movePage(v1_41, v5_160);
        var v2_74 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        v2_74.topMargin = com.oupao.wiz.Ui.dp(this, 0);
        v6_85.addView(v1_41, v2_74);
        // 搜索框（模块风格：圆角输入框，输入即过滤应用列表）
        var v5_159 = new android.widget.EditText(this);
        v5_159.setHint("\u641c\u7d22\u5e94\u7528");
        v5_159.setSingleLine(true);
        // ★ 必须显式设置 inputType：new EditText() 默认 TYPE_NULL，
        //   输入法不会接管，adb input text / 键盘都打不进字。
        v5_159.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        v5_159.setTextSize(15.0f);
        v5_159.setTextColor(com.oupao.wiz.Ui.textMain());
        v5_159.setHintTextColor(com.oupao.wiz.Ui.textSub());
        v5_159.setBackground(com.oupao.wiz.Ui.bg(
                com.oupao.wiz.Ui.night() ? 0x33FFFFFF : 0x12000000,
                (float) com.oupao.wiz.Ui.dp(this, 12)));
        com.oupao.wiz.Ui.pad(v5_159, 14, 10, 14, 10);
        var v2_75 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        v2_75.leftMargin = com.oupao.wiz.Ui.dp(this, 16);
        v2_75.rightMargin = com.oupao.wiz.Ui.dp(this, 16);
        v2_75.bottomMargin = com.oupao.wiz.Ui.dp(this, 8);
        v6_85.addView(v5_159, v2_75);
        com.oupao.wiz.MainActivity.sugSearch = v5_159;
        var v5_161 = com.oupao.wiz.Ui.card(this);
        v6_85.addView(v5_161);
        com.oupao.wiz.MainActivity.sugAppCard = v5_161;
        this.root.addView(v0_12, new android.widget.LinearLayout.LayoutParams(-1, -1));
        com.oupao.wiz.MainActivity.sL.setVisibility(0);
        com.oupao.wiz.MainActivity.sU.setVisibility(0);
        com.oupao.wiz.MainActivity.sQ.setVisibility(0);
        ((android.view.ViewGroup.MarginLayoutParams) com.oupao.wiz.MainActivity.sM.getLayoutParams()).topMargin = 0;
        var v6_7 = com.oupao.wiz.MainActivity.sTitle;
        v6_7.setPadding(v6_7.getPaddingLeft(), (v6_7.getPaddingTop() + com.oupao.wiz.MainActivity.sbH), v6_7.getPaddingRight(), v6_7.getPaddingBottom());
        var v6_9 = new android.widget.LinearLayout(this);
        v6_9.setOrientation(0);
        v6_9.setGravity(16);
        v6_9.setPadding(0, com.oupao.wiz.MainActivity.sbH, 0, 0);
        v6_9.addView(com.oupao.wiz.MainActivity.sK);
        com.oupao.wiz.MainActivity.movePage(v6_9, com.oupao.wiz.MainActivity.sL);
        var v6_11 = new android.widget.LinearLayout(this);
        v6_11.setOrientation(0);
        v6_11.setGravity(16);
        v6_11.setPadding(0, com.oupao.wiz.MainActivity.sbH, 0, 0);
        v6_11.addView(com.oupao.wiz.MainActivity.mkArrow(this));
        var v6_13 = new android.widget.LinearLayout(this);
        v6_13.setOrientation(0);
        v6_13.setGravity(16);
        v6_13.setPadding(0, com.oupao.wiz.MainActivity.sbH, 0, 0);
        v6_13.addView(com.oupao.wiz.MainActivity.mkArrow(this));
        com.oupao.wiz.MainActivity.movePage(v6_13, com.oupao.wiz.MainActivity.sU);
        com.oupao.wiz.MainActivity.pg3.addView(v6_13, 0);
        var v6_14 = com.oupao.wiz.MainActivity.pg7;
        var v5_16 = com.oupao.wiz.Ui.t(this, "\u5c4f\u5e55\u5e38\u4eae", 22.0f, com.oupao.wiz.Ui.textMain(), true);
        com.oupao.wiz.Ui.pad(v5_16, 8, 4, 18, 4);
        v6_14.addView(v5_16);
        var v1_5 = new android.widget.LinearLayout(this);
        v1_5.setOrientation(0);
        v1_5.setGravity(16);
        v1_5.setPadding(0, com.oupao.wiz.MainActivity.sbH, 0, 0);
        v1_5.addView(com.oupao.wiz.MainActivity.mkArrow(this));
        com.oupao.wiz.MainActivity.movePage(v1_5, v5_16);
        var v2_10 = new android.widget.LinearLayout.LayoutParams(-1, -2);
        v2_10.topMargin = com.oupao.wiz.Ui.dp(this, 0);
        // 与其他子页一致：返回栏插到最上面（含状态栏高度），标题紧随其后
        v6_14.addView(v1_5, 0, v2_10);
        var v5_17 = com.oupao.wiz.Ui.card(this);
        v6_14.addView(v5_17);
        com.oupao.wiz.Ui.row(v5_17, "oupao_sug_keep", "\u4e34\u65f6\u5f00\u542f\u5168\u5c40\u5c4f\u5e55\u5e38\u4eae|\u70b9\u51fb\u534a\u6697\u7684\u5c4f\u5e55\u65f6\uff0c\u5efa\u8bae\u4e34\u65f6\u5f00\u542f\u5c4f\u5e55\u5e38\u4eae\uff0c\u606f\u5c4f\u540e\u6062\u590d");
        // 功能页（Flyme 小窗/菜单/FlymeUI/Aicy/消息提醒/字母引索/游戏模式/振动）已全部由
        // MiuixShell + Pages.kt 用 Compose 渲染，旧的 View 构建器（Fly/FlyUI/Game/IdxPage）
        // 与 IdxPage.addEntry(pg0) 已删除。
        com.oupao.wiz.Pins.build();
        com.oupao.wiz.MainActivity.initPages();
        com.oupao.wiz.MainActivity.wrapPages();
        com.oupao.wiz.MainActivity.initBgs();
        com.oupao.wiz.PinUI.rail(this, com.oupao.wiz.MainActivity.pgPins);
        com.oupao.wiz.MainActivity.polishPages();
        com.oupao.wiz.MainActivity.logCenter = new LogCenter(getApplicationContext());
        v4_170.addView(MiuixShell.create(this), new android.widget.FrameLayout.LayoutParams(-1, -1));
        com.oupao.wiz.MA$W.start();
        var v2_13 = com.oupao.wiz.MainActivity.openPage();
        if (v2_13 != 5) {
            if (v2_13 == 3) {
                com.oupao.wiz.MainActivity.buildMusic();
            }
        } else {
            com.oupao.wiz.MainActivity.buildLog();
        }
        com.oupao.wiz.MainActivity.show(v2_13);
        // 退出编辑器放开方向锁引起的重建：把页面切回去（一次性）
        if ((com.oupao.wiz.MainActivity.retPage >= 0)
                && (com.oupao.wiz.MainActivity.retPage != v2_13)) {
            com.oupao.wiz.MainActivity.show(com.oupao.wiz.MainActivity.retPage);
        }
        com.oupao.wiz.MainActivity.retPage = -1;
        com.oupao.wiz.MainActivity.inst = this;
        com.oupao.wiz.MainActivity.regBack(this);
        // 编辑器里切过方向 → 界面已按新方向重建，这里自动把编辑器再打开
        if (com.oupao.wiz.MainActivity.posEditWanted) {
            var pb = com.oupao.wiz.MainActivity.pageBox;
            if (pb != null) {
                // 延后一点，等 pageBox 量出高度（编辑器默认位置要用它）
                pb.postDelayed(new java.lang.Runnable() {
                    public void run() {
                        try {
                            // 先把页面切回切方向前那一页（recreate 后默认会回首页）
                            int p = com.oupao.wiz.MainActivity.posPage;
                            if ((p >= 0) && (p != com.oupao.wiz.MainActivity.cur)) {
                                com.oupao.wiz.MainActivity.show(p);
                            }
                            com.oupao.wiz.MainActivity.openPosEditor();
                        } catch (Throwable ignore) {
                        }
                    }
                }, 200);
            }
        }
        return;
    }
    protected void onDestroy()
    {
        try {
            com.oupao.wiz.MainActivity.unregBack(this);
        } catch (Throwable __e_v0) {
        }
        // 真退出（不是旋转重建）时把方向锁和"要重开编辑器"的标记清掉，
        // 免得下次进模块界面还被锁在横屏
        try {
            if (isFinishing()) {
                com.oupao.wiz.MainActivity.posEditWanted = false;
                com.oupao.wiz.MainActivity.posReqOrientation = -1;
            }
        } catch (Throwable ignore) {
        }
        com.oupao.wiz.MainActivity.diagRx = false;
        if (com.oupao.wiz.MainActivity.inst == this) {
            MiuixShell.onLogVisibility(false, logCenter);
            com.oupao.wiz.MainActivity.inst = null;
        }
        super.onDestroy();
        return;
    }
    protected void onNewIntent(android.content.Intent p3)
    {
        int v2_4 = 0;
        java.lang.StringBuilder v1_1 = null;
        java.lang.String v2_3 = null;
        super.onNewIntent(p3);
        this.setIntent(p3);
        try {
            var v0_7 = p3.getBooleanExtra("oupao_desk", false);
            com.oupao.wiz.MainActivity.deskOn = v0_7;
            com.oupao.wiz.MainActivity.deskPins = p3.getStringExtra("oupao_pins");
        com.oupao.wiz.Pins.deskMode = com.oupao.wiz.MainActivity.deskOn;   // 2841
            v1_1 = new StringBuilder();
            v1_1.append("onNewIntent desk=");
            v1_1.append(v0_7);
            v1_1.append(" pins=");
            v2_3 = com.oupao.wiz.MainActivity.deskPins;
        } catch (Throwable __e_v3) {
            return;
        }
        if (v2_3 == null) {
            v2_4 = -1;
        } else {
            v2_4 = v2_3.length();
        }
        v1_1.append(v2_4);
        com.oupao.wiz.MainActivity.dlog(v1_1.toString());
        if (!com.oupao.wiz.MainActivity.deskOn) {
            var v0_3 = p3.getIntExtra("oupao_nav", -1);
            // 关键：先把切页动画的状态复位。
            // slide() 会用 aNext（上一次的目标页）当“从哪一页滑过来”的起点，
            // 如果上一次导航残留了 aNext（比如刚看过第 12 页），
            // 这次呼出「更多应用」(9) 就会从 12 滑过来，navEnd() 甚至直接把 12 显示出来
            // —— 表现就是“有时候呼出的是别的页”。
            try {
                com.oupao.wiz.MainActivity.killPend();
                com.oupao.wiz.MainActivity.aPend = null;
                com.oupao.wiz.MainActivity.aNext = com.oupao.wiz.MainActivity.cur;
            } catch (Throwable ignore) {
            }
            if (v0_3 < 0) {
                if (com.oupao.wiz.MainActivity.cur == 9) {
                    com.oupao.wiz.MainActivity.navTo(0);
                }
            } else {
                com.oupao.wiz.MainActivity.navTo(v0_3);
            }
        } else {
            com.oupao.wiz.Pins.build();
            com.oupao.wiz.MainActivity.navEnd();
            com.oupao.wiz.MainActivity.killPend();
            com.oupao.wiz.MainActivity.show(9);
        }
        return;
    }
    protected void onResume()
    {

        try {
        } catch (Throwable ignore) {
        }
        try {
        } catch (Throwable ignore) {
        }        int v0 = 0;
        com.oupao.wiz.MainActivity.inst = this;
        super.onResume();
        MiuixShell.onLogVisibility(cur == 5, logCenter);
        try {
            v0 = com.oupao.wiz.MainActivity.navReq;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 != -1) {
            com.oupao.wiz.MainActivity.navReq = -1;
            if (v0 != com.oupao.wiz.MainActivity.cur) {
                // 同 onNewIntent：复位切页动画状态，避免从残留的 aNext 页滑过来
                try {
                    com.oupao.wiz.MainActivity.killPend();
                    com.oupao.wiz.MainActivity.aPend = null;
                    com.oupao.wiz.MainActivity.aNext = com.oupao.wiz.MainActivity.cur;
                } catch (Throwable ignore) {
                }
                com.oupao.wiz.MainActivity.navTo(v0);
            }
        }
        return;
    }

    /** 1820fix: 把页面容器（pageBox 直接子层）刷成 pageBg，内层卡片保持深灰。 */
    

    /** 递归：高度 >= 屏幕 85% 的容器视为"铺满整屏的页面层"→ 页面底色 */
    


    

    /** 探针：每 500ms dump 一次视图树，共 20 次 */
    

    

    

    /** 屏幕常亮页（照 1820 smali 第 6990-7068 行还原） */
    }
