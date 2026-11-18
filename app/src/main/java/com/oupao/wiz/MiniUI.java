package com.oupao.wiz;
public class MiniUI {
    private static float bScale;
    private static int bTid;
    private static boolean bigger;
    private static Object cap;
    public static Object capKeep;
    private static com.oupao.wiz.MiniR hideR;
    private static com.oupao.wiz.MiniR hold;
    private static int idle;
    private static long lastAct;
    /** 3389：转迷你窗震动去重（内外两层 hook 同一手势只震一次） */
    private static long lastBuzzAt;
    private static String memAff;
    private static android.graphics.Rect memOrg;
    private static float memScale;
    private static Object memTask;
    static int memTid;
    static int memW;
    private static android.graphics.Rect miniOrg;
    private static boolean muted;
    private static boolean on;
    private static Object pendCtrl;
    private static android.graphics.Rect pendR;
    private static float pendS;
    private static Object pendTask;
    private static com.oupao.wiz.MiniR pnd;
    private static boolean poll;
    private static com.oupao.wiz.MiniR pollR;
    private static android.graphics.Rect prev;
    private static com.oupao.wiz.MiniR rmR;
    private static Object task;
    private static com.oupao.wiz.MiniR tick;
    private static int tid;
    private static android.graphics.Rect tmp;
    static android.graphics.Rect trR;
    static float trS;
    private static com.oupao.wiz.MiniView v;
    private static android.view.WindowManager wm;
    public static void abort()
    {
        com.oupao.wiz.MiniR v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.MiniUI.hold;
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.MiniUI.ex(__e_v0_1);
            return;
        }
        if (v0_0 != null) {
            com.oupao.wiz.MiniUI.lg("MiniUI: abort");
            var v1 = com.oupao.wiz.Hooks.main();
            com.oupao.wiz.MiniUI.hold = null;
            if (v1 != null) {
                v1.removeCallbacks(v0_0);
            }
        }
        return;
    }
    public static void act(int p3)
    {
        com.oupao.wiz.MiniUI.idle = 0;
        try {
            if (p3 != 1) {
                if (p3 == 2) {
                    com.oupao.wiz.MiniUI.size();
                }
            } else {
                com.oupao.wiz.MiniUI.mute();
            }
        } catch (Throwable __e_v0_6) {
            com.oupao.wiz.MiniUI.ex(__e_v0_6);
            com.oupao.wiz.MiniUI.hide();
            return;
        }
        var v0_2 = new StringBuilder();
        v0_2.append("MiniUI: act ");
        v0_2.append(p3);
        com.oupao.wiz.MiniUI.lg(v0_2.toString());
        com.oupao.wiz.MiniUI.hide();
        return;
    }
    static android.graphics.Rect anchor(android.graphics.Rect p14, android.graphics.Rect p15)
    {
        try {
            if ((p14 != null) && (p15 != null)) {
                com.oupao.wiz.FlyH.dims();
                var v0 = com.oupao.wiz.FlyH.sw;
                var v1 = com.oupao.wiz.FlyH.sh;
                if ((v0 > 0) && (v1 > 0)) {
                    var v2 = p14.width();
                    var v3 = p14.height();
                    int v6_0 = p15.left;
                    if (p15.centerX() > (v0 / 2)) {
                        v6_0 = (p15.right - v2);
                    }
                    int v7 = p15.top;
                    if (v6_0 < 0) {
                        v6_0 = 0;
                    }
                    if ((v6_0 + v2) > v0) {
                        v6_0 = (v0 - v2);
                        if (v6_0 < 0) {
                            v6_0 = 0;
                        }
                    }
                    if (v7 < 0) {
                        v7 = 0;
                    }
                    if ((v7 + v3) > v1) {
                        v7 = (v1 - v3);
                        if (v7 < 0) {
                            v7 = 0;
                        }
                    }
                    var v10_1 = new android.graphics.Rect(v6_0, v7, (v6_0 + v2), (v7 + v3));
                    var v12_1 = new StringBuilder();
                    v12_1.append("MiniUI: anc ");
                    v12_1.append(p15);
                    v12_1.append(" -> ");
                    v12_1.append(v10_1);
                    com.oupao.wiz.MiniUI.lg(v12_1.toString());
                    return v10_1;
                }
            }
        } catch (Throwable __e_v0) {
            return p14;
        }
        return p14;
    }
    public static void arm(Object p4)
    {
        com.oupao.wiz.MiniUI.idle = 0;
        try {
            if (((com.oupao.wiz.Cfg.miniBig()) || (com.oupao.wiz.Cfg.muteDirect())) && (p4 != null)) {
                var v0_1 = com.oupao.wiz.Win.capTask(p4);
                if ((v0_1 != null) && (com.oupao.wiz.MiniUI.isMini(v0_1))) {
                    com.oupao.wiz.MiniUI.cap = p4;
                    com.oupao.wiz.MiniUI.task = v0_1;
                    com.oupao.wiz.MiniUI.tid = com.oupao.wiz.Win.taskId(v0_1);
                    var v1_2 = com.oupao.wiz.Hooks.main();
                    if (v1_2 != null) {
                        com.oupao.wiz.MiniUI.abort();
                        var v2_1 = new com.oupao.wiz.MiniR(1);
                        com.oupao.wiz.MiniUI.hold = v2_1;
                        // 3162：再减 0.2 秒（800 → 600 → 400ms）
                        v1_2.postDelayed(v2_1, 400);
                        com.oupao.wiz.MiniUI.lg("MiniUI: arm");
                    }
                }
            }
        } catch (Throwable __e_v0_4) {
            com.oupao.wiz.MiniUI.ex(__e_v0_4);
            return;
        }
        return;
    }
    static void build()
    {
        try {
            if ((com.oupao.wiz.MiniUI.v == null) || (com.oupao.wiz.MiniUI.tmp == null)) {
                android.content.Context v0_3 = com.oupao.wiz.Cfg.sysContext();
                if (v0_3 != null) {
                    v0_3 = com.oupao.wiz.FlyH.ctx;
                }
                if (v0_3 != null) {
                    android.view.WindowManager v1_1 = ((android.view.WindowManager) v0_3.getSystemService("window"));
                    if (v1_1 != null) {
                        v1_1 = com.oupao.wiz.FlyH.wm;
                    }
                    if (v1_1 != null) {
                        com.oupao.wiz.MiniUI.wm = v1_1;
                        com.oupao.wiz.MiniUI.v = new com.oupao.wiz.MiniView(v0_3);
                        com.oupao.wiz.MiniUI.tmp = new android.graphics.Rect();
                    }
                }
                return;
            } else {
                return;
            }
        } catch (Throwable __e_v0_2) {
            com.oupao.wiz.MiniUI.ex(__e_v0_2);
            return;
        }
    }
    static void buzz()
    {
        try {
            long now = android.os.SystemClock.uptimeMillis();
            if ((now - com.oupao.wiz.MiniUI.lastBuzzAt) < 250L) {
                return;
            }
            com.oupao.wiz.MiniUI.lastBuzzAt = now;
            // 诊断：哪一次振动来自 MiniUI（转迷你/小窗手势），便于和扇形菜单的振动区分
            com.oupao.wiz.SLog.i("MiniUI: buzz");
            if (com.oupao.wiz.Cfg.vibOn()) {
                var v0_2 = com.oupao.wiz.Cfg.sysContext();
                if (v0_2 != null) {
                    var v0_3 = v0_2.getSystemService("vibrator");
                    if ((v0_3 instanceof android.os.Vibrator)) {
                        ((android.os.Vibrator) v0_3).vibrate(android.os.VibrationEffect.createOneShot(((long) com.oupao.wiz.Sug.vibDurMs(com.oupao.wiz.Cfg.vibLevel())), com.oupao.wiz.Cfg.sysHapticAmp()));
                    }
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    static void clearMem()
    {
        com.oupao.wiz.MiniUI.poll = false;
        com.oupao.wiz.MiniUI.memTid = 0;
        com.oupao.wiz.MiniUI.memTask = null;
        com.oupao.wiz.MiniUI.memAff = null;
        com.oupao.wiz.MiniUI.memOrg = null;
        com.oupao.wiz.MiniUI.memW = 0;
        com.oupao.wiz.MiniUI.lg("MiniUI: \u8bb0\u5fc6\u5df2\u6e05");
        return;
    }
    static void commit()
    {
        java.lang.Object v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.MiniUI.pendTask;
        } catch (Throwable __e_v0_3) {
            com.oupao.wiz.MiniUI.ex(__e_v0_3);
            return;
        }
        if (v0_0 != null) {
            var v1 = com.oupao.wiz.MiniUI.pendR;
            if (v1 != null) {
                com.oupao.wiz.Hooks.applyMiniRect(v1, v0_0, com.oupao.wiz.MiniUI.pendCtrl, com.oupao.wiz.MiniUI.pendS);
                com.oupao.wiz.MiniUI.pendTask = null;
                var v0_1 = new StringBuilder();
                v0_1.append("MiniUI: commit ");
                v0_1.append(v1);
                com.oupao.wiz.MiniUI.lg(v0_1.toString());
            }
        }
        return;
    }
    static void ex(Throwable p2)
    {
        try {
            var v0_1 = new StringBuilder();
            v0_1.append("MiniUI: EX ");
            v0_1.append(p2);
            com.oupao.wiz.SLog.i(v0_1.toString());
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    static boolean fit(android.graphics.Rect p6, Object p7)
    {
        int v0_0 = 0;
        try {
            if ((p6 != null) && (p7 != null)) {
                var v1_1 = com.oupao.wiz.Hooks.miniRect(p6);
                if (v1_1 != null) {
                    var v1_2 = com.oupao.wiz.Hooks.clampRect(v1_1);
                    if (v1_2 != null) {
                        var v1_0 = com.oupao.wiz.MiniUI.anchor(v1_2, p6);
                        if (v1_0 != null) {
                            var v2_1 = com.oupao.wiz.Rt.callNoArg(p7, "getBounds");
                            if ((v2_1 instanceof android.graphics.Rect)) {
                                var v2_3 = ((android.graphics.Rect) v2_1).width();
                                if (v2_3 > 0) {
                                    var v3_3 = (((float) v1_0.width()) / ((float) v2_3));
                                    com.oupao.wiz.MiniUI.trR = v1_0;
                                    com.oupao.wiz.MiniUI.trS = v3_3;
                                    v0_0 = 1;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable __e_v1) {
            return false;
        }
        return v0_0 != 0;
    }
    public static void hide()
    {
        android.os.Handler v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Hooks.main();
        } catch (Throwable __e_v0_2) {
            com.oupao.wiz.MiniUI.ex(__e_v0_2);
            return;
        }
        if (v0_0 != null) {
            var v1_7 = com.oupao.wiz.MiniUI.tick;
            if (v1_7 != null) {
                v0_0.removeCallbacks(v1_7);
            }
        }
        if (v0_0 != null) {
            var v1_9 = com.oupao.wiz.MiniUI.hideR;
            if (v1_9 != null) {
                v0_0.removeCallbacks(v1_9);
            }
        }
        var v0_1 = com.oupao.wiz.MiniUI.v;
        if ((v0_1 != null) && (com.oupao.wiz.MiniUI.on)) {
            com.oupao.wiz.MiniUI.on = false;
            com.oupao.wiz.MiniUI.lg("MiniUI: hide");
            v0_1.animate().alpha(0).setDuration(150).setInterpolator(new android.view.animation.AccelerateInterpolator()).start();
            var v1_8 = com.oupao.wiz.Hooks.main();
            if (v1_8 != null) {
                com.oupao.wiz.MiniR v2_4 = com.oupao.wiz.MiniUI.rmR;
                if (v2_4 == null) {
                    v2_4 = new com.oupao.wiz.MiniR(5);
                    com.oupao.wiz.MiniUI.rmR = v2_4;
                }
                v1_8.removeCallbacks(v2_4);
                v1_8.postDelayed(v2_4, 200);
            }
        }
        com.oupao.wiz.MiniUI.cap = null;
        com.oupao.wiz.MiniUI.task = null;
        // 3188：按下点只对本次长按有效，留着会让下一次露面被旧点挡住
        dnX = -1f;
        dnY = -1f;
        return;
    }
    static boolean isBig()
    {
        int v4 = 0;
        android.graphics.Rect v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.MiniUI.miniOrg;
        } catch (Throwable __e_v0_2) {
            com.oupao.wiz.MiniUI.ex(__e_v0_2);
            return false;
        }
        if ((v0_0 != null) && (com.oupao.wiz.MiniUI.bTid == com.oupao.wiz.MiniUI.tid)) {
            var v1_1 = com.oupao.wiz.MiniUI.tmp;
            if (v1_1 != null) {
                var v2_4 = com.oupao.wiz.MiniUI.task;
                if ((v2_4 != null) && (com.oupao.wiz.MiniUI.rectOf(v2_4, v1_1))) {
                    var v0_1 = com.oupao.wiz.Hooks.miniRect(v0_0);
                    if ((v0_1 != null) && (Math.abs((v1_1.width() - v0_1.width())) <= 4)) {
                        v4 = 1;
                    }
                }
            }
        }
        return v4 != 0;
    }
    static boolean isMini(Object p4)
    {
        int v3 = 0;
        try {
            if (p4 != null) {
                var v1_4 = new android.graphics.Rect();
                if (com.oupao.wiz.MiniUI.rectOf(p4, v1_4)) {
                    int v0_0 = v1_4.width();
                    var v2_0 = v1_4.height();
                    if (v0_0 > v2_0) {
                        v0_0 = v2_0;
                    }
                    com.oupao.wiz.FlyH.dims();
                    int v1_0 = com.oupao.wiz.FlyH.sw;
                    var v2_1 = com.oupao.wiz.FlyH.sh;
                    if (v1_0 > v2_1) {
                        v1_0 = v2_1;
                    }
                    if (((float) v0_0) < (((float) v1_0) * 0.45f)) {
                        v3 = 1;
                    }
                }
            }
        } catch (Throwable __e_v0_2) {
            com.oupao.wiz.MiniUI.ex(__e_v0_2);
            return false;
        }
        return v3 != 0;
    }
    /**
     * MiniUI 内部诊断日志统一入口。
     *
     * ⚠️ 这里是 verbose：调用方包括 MiniR(2) 的 **32ms 帧 tick**（`MiniR.java:40`
     * 「run step」每帧一条）、rstPoll 的 150ms 轮询（`:683`）、以及 `show()` 里
     * 连打的 9 条定值串（`:711-749`）。用 info 级会在小窗 UI 可见期间持续刷屏，
     * 而 SLog 的 1s 去重对「同 tag + 同 msg」有效、对这些每条都一样的定值串
     * 倒是能挡住一部分 —— 但带变量的（Rect/尺寸）挡不住。
     * 统一降到 verbose：开 oupao_dbg 才出，平时完全不占日志。
     */
    static void lg(String p0)
    {
        try {
            com.oupao.wiz.SLog.v(p0);
            return;
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.MiniUI.ex(__e_v0_1);
            return;
        }
    }
    static void lg2(String p2, int p3, int p4)
    {
        try {
            var v0_1 = new StringBuilder();
            v0_1.append(p2);
            v0_1.append(p3);
            v0_1.append("/");
            v0_1.append(p4);
            com.oupao.wiz.SLog.v(v0_1.toString());
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static boolean lp()
    {
        int v2_0 = 0;
        java.lang.Object v0_1 = null;
        try {
            v0_1 = com.oupao.wiz.Tap.cap();
        } catch (Throwable __e_v0_0) {
            com.oupao.wiz.MiniUI.ex(__e_v0_0);
            return false;
        }
        if (v0_1 != null) {
            var v1 = com.oupao.wiz.Win.capTask(v0_1);
            if ((v1 != null) && (com.oupao.wiz.MiniUI.isMini(v1))) {
                v2_0 = 1;
            }
        }
        return v2_0 != 0;
    }
    static android.view.WindowManager.LayoutParams mkLp()
    {
        try {
            var v0_1 = new android.view.WindowManager.LayoutParams();
            v0_1.type = 2024;
            v0_1.flags = 1312;
            v0_1.width = -1;
            v0_1.height = -1;
            v0_1.format = -3;
            v0_1.setFitInsetsTypes(0);
            v0_1.setTitle("oupao_mini_ui");
            com.oupao.wiz.FlyS.trust2(v0_1);
            return v0_1;
        } catch (Throwable __e_v1) {
            return null;
        }
    }
    static void mute()
    {
        try {
            if (com.oupao.wiz.Cfg.muteDirect()) {
                var v0_1 = com.oupao.wiz.MiniUI.task;
                if (v0_1 != null) {
                    var v1 = com.oupao.wiz.MiniUI.pkgOf(v0_1);
                    if (v1 == null) {
                        com.oupao.wiz.MiniUI.lg("MiniUI: mute bail: \u65e0\u5305\u540d");
                    } else {
                        int v4_0 = com.oupao.wiz.Win.taskUid(v0_1);
                        if (v4_0 < 0) {
                            v4_0 = com.oupao.wiz.MiniUI.uidOf(v1);
                        } else {
                        }
                        if (v4_0 < 0) {
                            var v5_1 = new StringBuilder();
                            v5_1.append("MiniUI: mute bail \u65e0 uid pkg=");
                            v5_1.append(v1);
                            v5_1.append(" u=");
                            v5_1.append(v4_0);
                            com.oupao.wiz.MiniUI.lg(v5_1.toString());
                        } else {
                            var v5_4 = new StringBuilder();
                            v5_4.append("MiniUI: mute uid=");
                            v5_4.append(v4_0);
                            v5_4.append(" pkg=");
                            v5_4.append(v1);
                            com.oupao.wiz.MiniUI.lg(v5_4.toString());
                            com.oupao.wiz.Mute.toggle(v4_0, v1);
                            if (!com.oupao.wiz.MiniUI.muted) {
                                com.oupao.wiz.MiniUI.muted = true;
                            } else {
                                com.oupao.wiz.MiniUI.muted = false;
                            }
                        }
                    }
                }
            }
        } catch (Throwable __e_v0_2) {
            com.oupao.wiz.MiniUI.ex(__e_v0_2);
            return;
        }
        return;
    }
    public static String pkgOf(Object p7)
    {
        java.lang.Object v0_0 = null;
        if (p7 != null) {
            var v0_1 = com.oupao.wiz.Win.taskPkg(p7);
            if (v0_1 == null) {
                java.lang.String v0_2 = ((java.lang.String) com.oupao.wiz.Rt.get(p7, "affinity"));
                if (!(v0_2 instanceof String)) {
                    v0_0 = null;
                    var v1_1 = com.oupao.wiz.Rt.callNoArg(p7, "getBaseIntent");
                    if (v1_1 != null) {
                        var v2_1 = com.oupao.wiz.Rt.callNoArg(v1_1, "getComponent");
                        if (v2_1 != null) {
                            com.oupao.wiz.Rt.callNoArg(v2_1, "getPackageName");
                            v0_0 = "getPackageName";
                            if (!("getPackageName" instanceof String)) {
                                com.oupao.wiz.MiniUI.lg("MiniUI: pkgOf cmp.pkg \u975e\u5b57\u7b26\u4e32");
                                v0_0 = null;
                            }
                        } else {
                            com.oupao.wiz.MiniUI.lg("MiniUI: pkgOf cmp=null");
                        }
                        if (v0_0 == null) {
                            com.oupao.wiz.MiniUI.lg("MiniUI: pkgOf \u7528 baseIntent.getPackage");
                            var v1_2 = com.oupao.wiz.Rt.callNoArg(v1_1, "getPackage");
                            if ((v1_2 instanceof String)) {
                                v0_0 = v1_2;
                            }
                        }
                    } else {
                        com.oupao.wiz.MiniUI.lg("MiniUI: pkgOf bi=null");
                    }
                } else {
                    com.oupao.wiz.MiniUI.lg("MiniUI: pkgOf by affinity");
                    return v0_2;
                }
            } else {
                com.oupao.wiz.MiniUI.lg("MiniUI: pkgOf by taskPkg");
                return v0_1;
            }
        }
        return ((java.lang.String) v0_0);
    }
    /**
     * 3169：从 caption 视图取"屏幕上真实位置"。
     *
     * 加了两道合理性校验，避免拿到"只有小白条"或未附着时的错误值：
     *  - 必须已 attach、有实际尺寸；
     *  - 宽高要和 Task bounds 同量级（0.6~1.8 倍），否则判定不是整窗、退回 Task bounds。
     */
    private static android.graphics.Rect capRect() {
        try {
            Object c = cap;
            if (!(c instanceof android.view.View)) return null;
            android.view.View v = (android.view.View) c;
            if (!v.isAttachedToWindow() || v.getWidth() <= 0 || v.getHeight() <= 0) return null;
            int[] loc = new int[2];
            v.getLocationOnScreen(loc);
            android.graphics.Rect r = new android.graphics.Rect(
                    loc[0], loc[1], loc[0] + v.getWidth(), loc[1] + v.getHeight());
            if (r.width() < 120 || r.height() < 120) return null;
            Object t = task;
            if (t != null) {
                Object tb = Rt.callNoArg(t, "getBounds");
                if (tb instanceof android.graphics.Rect) {
                    android.graphics.Rect b = (android.graphics.Rect) tb;
                    if (b.width() > 0 && b.height() > 0) {
                        float rw = ((float) r.width()) / b.width();
                        float rh = ((float) r.height()) / b.height();
                        if (rw < 0.6f || rw > 1.8f || rh < 0.6f || rh > 1.8f) return null;
                    }
                }
            }
            return r;
        } catch (Throwable ignore) {
        }
        return null;
    }

    /**
     * 3171：竖屏双击小白条 → 把当前小窗**吸附到最近的左/右边缘**（保持尺寸与高度）。
     *
     * 复用拖动提交用的同一套接口（[Hooks.applyMiniRect]），所以不会触发"重建窗口"，
     * 也不会动最近任务。
     */
    static boolean dockNow() {
        return dockTask(com.oupao.wiz.MiniUI.task, com.oupao.wiz.MiniUI.cap, 0);
    }

    /** 3175：dir>0 贴右、dir<0 贴左、0 = 按离哪边近自动选 */
    static boolean dockTask(Object t, Object c, int dir) {
        try {
            if (t == null) {
                return false;
            }
            com.oupao.wiz.FlyH.dims();
            int sw = com.oupao.wiz.FlyH.sw;
            android.graphics.Rect r = new android.graphics.Rect();
            if (!com.oupao.wiz.MiniUI.rectOf(t, r)) {
                return false;
            }
            if (sw <= 0) {
                sw = r.right;
            }
            int toRight;
            if (dir > 0) {
                toRight = 1;
            } else if (dir < 0) {
                toRight = 0;
            } else {
                toRight = (r.centerX() > (sw / 2)) ? 1 : 0;
            }
            // ★ 3183：`rectOf` 返回的是**缩放后的可见矩形**，而 applyMiniRect 要的是
            // **未缩放的 bounds**（框架自己按 scale 以中心放大绘制）。
            // 3180 直接把可见矩形传回去，于是"贴右"只挪了 64px（1216-970 → 246），
            // 看起来就像没反应。这里先把可见宽度换算回 bounds 宽度，再按中心对齐。
            float sc2 = com.oupao.wiz.Hooks.scaleOf(t);
            if (sc2 <= 0f || sc2 > 1f) {
                sc2 = 1f;
            }
            int bw = Math.round(r.width() / sc2);
            int bh = Math.round(r.height() / sc2);
            int cx = (toRight != 0) ? (sw - (r.width() / 2)) : (r.width() / 2);
            int left = cx - (bw / 2);
            int top = r.centerY() - (bh / 2);
            android.graphics.Rect nr = new android.graphics.Rect(left, top, left + bw, top + bh);
            Object ctrl = null;
            if (c != null) {
                ctrl = com.oupao.wiz.Rt.get(c, "mFlexibleTaskController");
            }
            com.oupao.wiz.Hooks.applyMiniRect(nr, t, ctrl, sc2);
            com.oupao.wiz.MiniUI.lg("MiniUI: dock vis=" + r + " bounds=" + nr
                    + " sw=" + sw + " sc=" + sc2);
            return true;
        } catch (Throwable __e) {
            com.oupao.wiz.MiniUI.ex(__e);
        }
        return false;
    }

    static boolean rectOf(Object p6, android.graphics.Rect p7)
    {
        int v3 = 0;
        // 3170：caption 取位置这条路**先不用**了 —— 实测拿到的 frame 不对，
        // 叠层被画到别处，表现就是"长按没反应"。先退回 Task bounds（3168 行为），
        // 之后要重启这条优化，得先把两种 rect 都打进日志对比，确认坐标口径再启用。
        try {
            if ((p6 != null) && (p7 != null)) {
                var v0_4 = com.oupao.wiz.Rt.callNoArg(p6, "getBounds");
                if ((v0_4 instanceof android.graphics.Rect)) {
                    var v0_5 = ((android.graphics.Rect) v0_4);
                    var v1_0 = com.oupao.wiz.Hooks.scaleOf(p6);
                    if ((v1_0 > 0) && (v1_0 < 1.0f)) {
                        var v0_0 = com.oupao.wiz.Hooks.shrinkTo(v0_5, v1_0);
                        if (v0_0 != null) {
                            p7.set(v0_0);
                            v3 = 1;
                        }
                    } else {
                        p7.set(v0_5);
                        v3 = 1;
                    }
                }
            }
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.MiniUI.ex(__e_v0_1);
            return false;
        }
        return v3 != 0;
    }
    public static void restore(Object p14, Object p15)
    {
        try {
            if (p14 != null) {
                if (p15 != null) {
                    if (com.oupao.wiz.Win.zoomState(p15) == 0) {
                        var v0_7 = com.oupao.wiz.MiniUI.memTid;
                        if ((v0_7 != 0) && (com.oupao.wiz.Win.taskId(p15) == v0_7)) {
                            com.oupao.wiz.MiniUI.clearMem();
                        }
                    }
                    if (com.oupao.wiz.MiniUI.memTask == p15) {
                        var v12_9 = com.oupao.wiz.MiniUI.memAff;
                        if (v12_9 != null) {
                            var v13_0 = com.oupao.wiz.MiniUI.pkgOf(p15);
                            if (v13_0 != null) {
                                if (!v12_9.equals(v13_0)) {
                                    com.oupao.wiz.MiniUI.lg("rst: \u5305\u540d\u4e0d\u5339\u914d");
                                    return;
                                }
                                if ((((int) android.os.SystemClock.uptimeMillis()) - ((int) com.oupao.wiz.MiniUI.lastAct)) >= 1500) {
                                    if (com.oupao.wiz.MiniUI.isMini(p15)) {
                                        var v4_1 = new android.graphics.Rect();
                                        if (com.oupao.wiz.MiniUI.rectOf(p15, v4_1)) {
                                            if (v4_1.width() < (com.oupao.wiz.MiniUI.memW - 16)) {
                                                var v5_0 = com.oupao.wiz.Rt.callNoArg(p15, "getBounds");
                                                if ((v5_0 instanceof android.graphics.Rect)) {
                                                    var v3 = ((android.graphics.Rect) v5_0).width();
                                                    if (v3 > 0) {
                                                        var v6 = com.oupao.wiz.Rt.get(p14, "mFlexibleTaskController");
                                                        if (v6 != null) {
                                                            var v5_2 = com.oupao.wiz.Hooks.miniRect(v4_1);
                                                            if (v5_2 != null) {
                                                                var v5_4 = com.oupao.wiz.MiniUI.anchor(com.oupao.wiz.Hooks.clampRect(v5_2), v4_1);
                                                                com.oupao.wiz.Hooks.applyMiniRect(v5_4, p15, v6, (((float) v5_4.width()) / ((float) v3)));
                                                                com.oupao.wiz.MiniUI.lastAct = android.os.SystemClock.uptimeMillis();
                                                                var v12_13 = new StringBuilder();
                                                                v12_13.append("MiniUI: rst big \u2714 ");
                                                                v12_13.append(v5_4);
                                                                com.oupao.wiz.MiniUI.lg(v12_13.toString());
                                                                return;
                                                            } else {
                                                                com.oupao.wiz.MiniUI.lg("rst: \u76ee\u6807\u77e9\u5f62\u4e3a\u7a7a");
                                                                return;
                                                            }
                                                        } else {
                                                            com.oupao.wiz.MiniUI.lg("rst: \u65e0 ctrl");
                                                            return;
                                                        }
                                                    } else {
                                                        com.oupao.wiz.MiniUI.lg("rst: \u5e03\u5c40\u5bbd\u4e3a0");
                                                        return;
                                                    }
                                                } else {
                                                    com.oupao.wiz.MiniUI.lg("rst: \u5e03\u5c40\u77e9\u5f62\u5f02\u5e38");
                                                    return;
                                                }
                                            } else {
                                                com.oupao.wiz.MiniUI.lg("rst: \u5df2\u662f\u5927\u53f7");
                                                return;
                                            }
                                        } else {
                                            com.oupao.wiz.MiniUI.lg("rst: \u53d6\u4e0d\u5230\u53ef\u89c1\u77e9\u5f62");
                                            return;
                                        }
                                    } else {
                                        com.oupao.wiz.MiniUI.lg("rst: \u975e\u8ff7\u4f60\u6001");
                                        return;
                                    }
                                } else {
                                    com.oupao.wiz.MiniUI.lg("rst: \u51b7\u5374\u4e2d");
                                    return;
                                }
                            }
                        }
                        var v0_1 = com.oupao.wiz.MiniUI.memTid;
                        if (v0_1 == 0) {
                            com.oupao.wiz.MiniUI.lg("rst: \u65e0\u8bb0\u5fc6");
                            return;
                        } else {
                            if (com.oupao.wiz.Win.taskId(p15) == v0_1) {
                                com.oupao.wiz.MiniUI.lg("rst: tid \u4e0d\u5339\u914d");
                                return;
                            }
                        }
                    }
                } else {
                    com.oupao.wiz.MiniUI.lg("rst: task=null");
                    return;
                }
            } else {
                com.oupao.wiz.MiniUI.lg("rst: cap=null");
                return;
            }
        } catch (Throwable __e_v0_0) {
            com.oupao.wiz.MiniUI.ex(__e_v0_0);
            return;
        }
    }
    static void rm()
    {
        com.oupao.wiz.MiniView v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.MiniUI.v;
        } catch (Throwable __e_v0_2) {
            com.oupao.wiz.MiniUI.ex(__e_v0_2);
            return;
        }
        if ((v0_0 != null) && (!com.oupao.wiz.MiniUI.on)) {
            var v1_1 = com.oupao.wiz.MiniUI.wm;
            if (v1_1 != null) {
                v1_1.removeView(v0_0);
                com.oupao.wiz.MiniUI.lg("MiniUI: rm");
            }
        }
        return;
    }
    static void rstPoll()
    {
        try {
            if ((com.oupao.wiz.MiniUI.poll) && (com.oupao.wiz.MiniUI.memAff != null)) {
                var v0_2 = com.oupao.wiz.MiniUI.memTid;
                if (v0_2 != 0) {
                    java.lang.Object v1 = com.oupao.wiz.CapR.byTid(v0_2);
                    if (v1 == null) {
                        v1 = com.oupao.wiz.Tap.cap();
                        if (v1 == null) {
                            v1 = com.oupao.wiz.MiniUI.capKeep;
                        } else {
                        }
                    } else {
                    }
                    if (v1 != null) {
                        java.lang.Object v2 = com.oupao.wiz.CapR.task(v1);
                        if (v2 == null) {
                            v2 = com.oupao.wiz.Hooks.task();
                        } else {
                        }
                        if ((v2 != null) && (com.oupao.wiz.MiniUI.isMini(v2))) {
                            var v3_1 = com.oupao.wiz.MiniUI.tmp;
                            if ((v3_1 != null) && ((com.oupao.wiz.MiniUI.rectOf(v2, v3_1)) && (v3_1.width() < (com.oupao.wiz.MiniUI.memW - 16)))) {
                                com.oupao.wiz.MiniUI.lg("MiniUI: poll rst");
                                com.oupao.wiz.MiniUI.restore(v1, v2);
                            }
                        }
                    }
                    var v3_4 = com.oupao.wiz.Hooks.main();
                    if (v3_4 != null) {
                        com.oupao.wiz.MiniR v4_3 = com.oupao.wiz.MiniUI.pollR;
                        if (v4_3 == null) {
                            v4_3 = new com.oupao.wiz.MiniR(6);
                            com.oupao.wiz.MiniUI.pollR = v4_3;
                        }
                        v3_4.postDelayed(v4_3, 150);
                    }
                }
            }
        } catch (Throwable __e_v0_4) {
            com.oupao.wiz.MiniUI.ex(__e_v0_4);
            return;
        }
        return;
    }
    static void show()
    {
        try {
            if (com.oupao.wiz.MiniUI.cap != null) {
                var v0_3 = com.oupao.wiz.MiniUI.task;
                if ((v0_3 != null) && (com.oupao.wiz.MiniUI.isMini(v0_3))) {
                    com.oupao.wiz.MiniUI.lg("show: 3mini ok");
                    com.oupao.wiz.MiniUI.build();
                    var v0_1 = com.oupao.wiz.MiniUI.v;
                    if (v0_1 != null) {
                        com.oupao.wiz.MiniUI.lg("show: 4build ok");
                        var v1_2 = com.oupao.wiz.MiniUI.tmp;
                        if (v1_2 != null) {
                            com.oupao.wiz.MiniUI.lg("show: 5tmp ok");
                            // 3188：拖动刚松手时 Task bounds 还没追上，用预测矩形先对齐
                            if (com.oupao.wiz.MiniUI.rectBest(com.oupao.wiz.MiniUI.task, v1_2)) {
                                com.oupao.wiz.MiniUI.lg("show: 6rect ok");
                                var v3_1 = com.oupao.wiz.MiniUI.isBig();
                                com.oupao.wiz.MiniUI.bigger = v3_1;
                                v0_1.setState(com.oupao.wiz.MiniUI.muted, v3_1);
                                v0_1.setMini(v1_2);
                                if (com.oupao.wiz.MiniUI.on) {
                                    v0_1.setAlpha(1.0f);
                                    v0_1.setScaleX(1.0f);
                                    v0_1.setScaleY(1.0f);
                                    com.oupao.wiz.MiniUI.lg("MiniUI: show keep");
                                } else {
                                    com.oupao.wiz.MiniUI.lg("show: 7isBig ok");
                                    var v3_5 = com.oupao.wiz.MiniUI.wm;
                                    if (v3_5 == null) {
                                    } else {
                                        com.oupao.wiz.MiniUI.lg("show: 8wm ok");
                                        var v4_1 = com.oupao.wiz.MiniUI.mkLp();
                                        if (v4_1 == null) {
                                        } else {
                                            com.oupao.wiz.MiniUI.lg("show: 9lp ok");
                                            v3_5.addView(v0_1, v4_1);
                                            com.oupao.wiz.MiniUI.on = true;
                                            // 3466 基线：小窗出现时振动一次
                                            com.oupao.wiz.MiniUI.buzz();
                                            v0_1.setAlpha(0);
                                            v0_1.setScaleX(0.86f);
                                            v0_1.setScaleY(0.86f);
                                            v0_1.setPivotX(((float) v1_2.centerX()));
                                            v0_1.setPivotY(((float) v1_2.centerY()));
                                            // 3164：淡入交给 syncNow（连续两次读到同一矩形才露面），
                                            // 这里只把状态复位，不再自己定时淡入。
                                            com.oupao.wiz.MiniUI.revealed = false;
                                            com.oupao.wiz.MiniUI.stableCnt = 0;
                                            com.oupao.wiz.MiniUI.lastSig = 0L;
                                            com.oupao.wiz.MiniUI.showAt = android.os.SystemClock.uptimeMillis();
                                            // 3166：如果这次的位置和上一次露面的位置不同，说明刚拖过窗口
                                            // （系统写回 bounds 会滞后），给更长的最短延迟，避免在旧位置露面。
                                            // 3168：用户反馈"振动了但按钮要过一会才出来"，不跟手比偶尔错位更难受。
                                            // 改成 100ms 就露面（只淡入，不缩放），万一之后 bounds 才更新，
                                            // 32ms 的跟随刷新会把叠层挪到正确位置。
                                            com.oupao.wiz.MiniUI.revealMinMs = 100L;
                                            com.oupao.wiz.MiniUI.lastShownRect = new android.graphics.Rect(v1_2);
                                            com.oupao.wiz.MiniUI.lg("MiniUI: revealMin=" + com.oupao.wiz.MiniUI.revealMinMs
                                                    + " rect=" + v1_2);
                                            com.oupao.wiz.MiniUI.lg("MiniUI: show add");
                                            // 3145：用户反馈「拖动迷你窗后立刻长按，按钮/遮罩和窗口错位」。
                                            // 原因：show() 用的是拖动**刚结束那一瞬**的 bounds，而系统此时
                                            // 可能还没把最终位置写回 Task（缩放/贴边动画也没结束），
                                            // 叠层就停在旧位置。这里在 0/60/180ms 各补一次同步，
                                            // 让按钮和遮罩跟着窗口走完最后一次位移。
                                            com.oupao.wiz.MiniUI.prev = null;
                                            com.oupao.wiz.MiniUI.idle = 0;
                                            // 3162：拖动结束后系统把最终 bounds 写回 Task 需要几百毫秒，
                                            // 只在 0/60/180ms 补同步不够 → 叠层先停在旧位置、
                                            // 等下一次 step 才"闪"到新位置。这里覆盖到 1.1 秒。
                                            for (long d : new long[]{0, 50, 100, 160, 240, 340, 460, 600, 780, 1000, 1100}) {
                                                com.oupao.wiz.MiniUI.resync(d);
                                            }
                                            // 兜底：800ms 内一直没稳定也强制露面，避免"长按了却什么都没有"
                                            v0_1.postDelayed(new Runnable() {
                                                public void run() {
                                                    if (!com.oupao.wiz.MiniUI.revealed) {
                                                        // 3188：兜底也要先确认矩形包得住按下点；
                                                        // 包不住说明位置彻底没对上，宁可这次不露面，
                                                        // 也不要在错的地方闪一下（用户已经反馈过这个）。
                                                        android.graphics.Rect rr = com.oupao.wiz.MiniUI.tmp;
                                                        if (!com.oupao.wiz.MiniUI.hitOK(rr)) {
                                                            com.oupao.wiz.MiniUI.lg("MiniUI: 兜底位置不成立 → 放弃本次露面");
                                                            com.oupao.wiz.MiniUI.hide();
                                                            return;
                                                        }
                                                        com.oupao.wiz.MiniUI.revealed = true;
                                                        try {
                                                            if (rr != null) {
                                                                v0_1.setPivotX(((float) rr.centerX()));
                                                                v0_1.setPivotY(((float) rr.centerY()));
                                                            }
                                                        } catch (Throwable ignore) {
                                                        }
                                                        v0_1.setScaleX(1.0f);
                                                        v0_1.setScaleY(1.0f);
                                                        v0_1.animate().alpha(1.0f).setDuration(120).start();
                                                        com.oupao.wiz.MiniUI.lg("MiniUI: 超时强制露面");
                                                    }
                                                }
                                            }, 800);
                                        }
                                    }
                                }
                                v0_1.invalidate();
                                var v0_4 = com.oupao.wiz.Hooks.main();
                                if (v0_4 != null) {
                                    com.oupao.wiz.MiniR v1_4 = com.oupao.wiz.MiniUI.tick;
                                    if (v1_4 == null) {
                                        v1_4 = new com.oupao.wiz.MiniR(2);
                                        com.oupao.wiz.MiniUI.tick = v1_4;
                                    }
                                    v0_4.removeCallbacks(v1_4);
                                    v0_4.postDelayed(v1_4, 32);
                                    com.oupao.wiz.MiniR v1_1 = com.oupao.wiz.MiniUI.hideR;
                                    if (v1_1 == null) {
                                        v1_1 = new com.oupao.wiz.MiniR(3);
                                        com.oupao.wiz.MiniUI.hideR = v1_1;
                                    }
                                    v0_4.removeCallbacks(v1_1);
                                    v0_4.postDelayed(v1_1, 5000);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable __e_v0_2) {
            com.oupao.wiz.MiniUI.ex(__e_v0_2);
            return;
        }
        return;
    }
    static void size()
    {
        java.lang.Object v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.MiniUI.task;
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.MiniUI.ex(__e_v0_1);
            return;
        }
        if (v0_0 != null) {
            var v1_0 = com.oupao.wiz.MiniUI.cap;
            if (v1_0 != null) {
                var v1_1 = com.oupao.wiz.Rt.get(v1_0, "mFlexibleTaskController");
                if (v1_1 != null) {
                    var v2_0 = com.oupao.wiz.Rt.callNoArg(v0_0, "getBounds");
                    if ((v2_0 instanceof android.graphics.Rect)) {
                        var v3_1 = ((android.graphics.Rect) v2_0).width();
                        if (v3_1 > 0) {
                            var v4_1 = new android.graphics.Rect();
                            if (com.oupao.wiz.MiniUI.rectOf(v0_0, v4_1)) {
                                float v5_4 = 0f;
                                android.graphics.Rect v6_1 = null;
                                var v5_1 = com.oupao.wiz.MiniUI.memOrg;
                                if (v5_1 == null) {
                                    com.oupao.wiz.MiniUI.miniOrg = new android.graphics.Rect(v4_1);
                                    com.oupao.wiz.MiniUI.memOrg = new android.graphics.Rect(v4_1);
                                    com.oupao.wiz.MiniUI.bScale = com.oupao.wiz.Hooks.scaleOf(v0_0);
                                    com.oupao.wiz.MiniUI.bTid = com.oupao.wiz.MiniUI.tid;
                                    var v6_2 = com.oupao.wiz.Hooks.miniRect(v4_1);
                                    if (v6_2 == null) {
                                        return;
                                    } else {
                                        v6_1 = com.oupao.wiz.MiniUI.anchor(com.oupao.wiz.Hooks.clampRect(v6_2), v4_1);
                                        v5_4 = (((float) v6_1.width()) / ((float) v3_1));
                                        com.oupao.wiz.MiniUI.memTid = com.oupao.wiz.MiniUI.tid;
                                        var v7_20 = com.oupao.wiz.MiniUI.task;
                                        com.oupao.wiz.MiniUI.memTask = v7_20;
                                        com.oupao.wiz.MiniUI.memAff = com.oupao.wiz.MiniUI.pkgOf(v7_20);
                                        com.oupao.wiz.MiniUI.memScale = v5_4;
                                        com.oupao.wiz.MiniUI.memW = v6_1.width();
                                        com.oupao.wiz.MiniUI.lastAct = android.os.SystemClock.uptimeMillis();
                                        com.oupao.wiz.MiniUI.poll = true;
                                        com.oupao.wiz.MiniUI.lg("MiniUI: poll on");
                                        var v7_26 = com.oupao.wiz.Hooks.main();
                                        if (v7_26 != null) {
                                            com.oupao.wiz.MiniR v8_9 = com.oupao.wiz.MiniUI.pollR;
                                            if (v8_9 == null) {
                                                v8_9 = new com.oupao.wiz.MiniR(6);
                                                com.oupao.wiz.MiniUI.pollR = v8_9;
                                            }
                                            v7_26.postDelayed(v8_9, 150);
                                        }
                                    }
                                } else {
                                    if (v4_1.width() >= (com.oupao.wiz.MiniUI.memW - 16)) {
                                        if (com.oupao.wiz.MiniUI.memTask == v0_0) {
                                            var v7_9 = com.oupao.wiz.MiniUI.memTid;
                                            if ((v7_9 == 0) || (com.oupao.wiz.Win.taskId(v0_0) != v7_9)) {
                                            }
                                        }
                                        v6_1 = new android.graphics.Rect(v5_1);
                                        v5_4 = (((float) v6_1.width()) / ((float) v3_1));
                                        com.oupao.wiz.MiniUI.clearMem();
                                        com.oupao.wiz.MiniUI.lastAct = android.os.SystemClock.uptimeMillis();
                                    } else {
                                    }
                                }
                                if (v6_1 != null) {
                                    var v7_1 = new StringBuilder();
                                    v7_1.append("MiniUI: size cur=");
                                    v7_1.append(v4_1);
                                    v7_1.append(" tgt=");
                                    v7_1.append(v6_1);
                                    v7_1.append(" sc=");
                                    v7_1.append(v5_4);
                                    com.oupao.wiz.MiniUI.lg(v7_1.toString());
                                    var v7_4 = com.oupao.wiz.Rt.callNoArg(v1_1, "getAnimator");
                                    if (v7_4 != null) {
                                        Object[] v8_4 = new Object[6];
                                        v8_4[0] = v4_1;
                                        v8_4[1] = v6_1;
                                        v8_4[2] = v0_0;
                                        v8_4[3] = Float.valueOf(v5_4);
                                        v8_4[4] = Integer.valueOf(0);
                                        v8_4[5] = Integer.valueOf(0);
                                        com.oupao.wiz.Rt.callLoose(v7_4, "startBottomHandleReboundTaskSpringAnim", v8_4);
                                    }
                                    com.oupao.wiz.MiniUI.pendR = v6_1;
                                    com.oupao.wiz.MiniUI.pendS = v5_4;
                                    com.oupao.wiz.MiniUI.pendTask = v0_0;
                                    com.oupao.wiz.MiniUI.pendCtrl = v1_1;
                                    var v7_5 = com.oupao.wiz.Hooks.main();
                                    if (v7_5 != null) {
                                        com.oupao.wiz.MiniR v8_5 = com.oupao.wiz.MiniUI.pnd;
                                        if (v8_5 == null) {
                                            v8_5 = new com.oupao.wiz.MiniR(4);
                                            com.oupao.wiz.MiniUI.pnd = v8_5;
                                        }
                                        v7_5.removeCallbacks(v8_5);
                                        v7_5.postDelayed(v8_5, 450);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return;
    }
    /**
     * 3145：叠层**立刻**按当前窗口矩形重新对齐一次（拖动结束后的补同步）。
     * 只做绘制层同步，不动系统窗口，也不会触发动画。
     */
    static void resync(long p5) {
        try {
            var v0 = com.oupao.wiz.Hooks.main();
            if (v0 == null) {
                return;
            }
            v0.postDelayed(new com.oupao.wiz.MiniR(7), p5);
        } catch (Throwable __e) {
            com.oupao.wiz.MiniUI.ex(__e);
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 3188：拖动"预测矩形"
    //
    // 问题：拖动迷你窗松手后，系统要几百毫秒才把最终位置写回 Task bounds /
    // 大窗动画结束时才 relayout caption。这期间长按 → show()/syncNow()/step()
    // 读到的还是旧 bounds，叠层先画在旧位置，等 bounds 追上来再"闪"过去。
    // 前几版只是把"露面延迟"从 100ms 调到 800ms 又调回来，治不了根。
    //
    // 做法：caption 的触摸流里自己记"窗口跟着手指平移了多少"（拖动是 1:1 的），
    // 得到当前可见矩形的预测值。规则：
    //   - 系统上报的矩形**还没变过**（obsAt 早于最近一次拖动）→ 用预测值；
    //   - 系统一更新（obsAt 变新）→ 立刻改用它，绝不压着系统值不放。
    // 这样既不会先错位，也不会在系统追上后还停在预测位置。
    // ────────────────────────────────────────────────────────────────
    private static android.graphics.Rect predBase;
    private static android.graphics.Rect predRect;
    private static float predX;
    private static float predY;
    private static int predTid;
    private static long predAt;
    private static android.graphics.Rect obsLast;
    private static long obsAt;
    private static float dnX = -1f;
    private static float dnY = -1f;

    /** 按下 caption：记下按下点与当时的可见矩形，作为拖动预测的基点。 */
    static void dragDown(float x, float y) {
        try {
            dnX = x;
            dnY = y;
            var t = com.oupao.wiz.MiniUI.task;
            if (t == null) {
                return;
            }
            var obs = new android.graphics.Rect();
            if (!com.oupao.wiz.MiniUI.rectOf(t, obs)) {
                return;
            }
            long now = android.os.SystemClock.uptimeMillis();
            // obsAt 只在"系统值确实变了"时才更新（=这条上报的生效时刻）
            if (obsLast == null) {
                obsLast = new android.graphics.Rect(obs);
                obsAt = 0L;
            } else if (!obsLast.equals(obs)) {
                obsLast = new android.graphics.Rect(obs);
                obsAt = now;
            }
            int tid2 = com.oupao.wiz.MiniUI.tid;
            // 3188b：上一次拖动留下的预测如果仍然比系统值新，说明窗口实际还在那儿
            // ——**接着拿它当基点**。否则这里会把基点重置成系统那份过期 bounds，
            // 于是"拖完马上长按"时 rectBest 没预测可用、hitOK 又拦着不许露面，
            // 表现就是"振动了但叠层要等一会才出来"。
            if ((predRect != null) && (predTid == tid2) && (predAt > obsAt)
                    && ((now - predAt) < 3000L)) {
                predBase = new android.graphics.Rect(predRect);
                predAt = now;                 // 续期：系统真写回时 obsAt 会变得更新
                com.oupao.wiz.MiniUI.lg("MiniUI: 长按续用拖动预测 " + predBase);
            } else {
                predBase = obs;
                predRect = null;
            }
            predX = x;
            predY = y;
            predTid = tid2;
        } catch (Throwable __e) {
            com.oupao.wiz.MiniUI.ex(__e);
        }
    }

    /** 拖动中：窗口跟手指 1:1 平移 → 按位移推当前可见矩形。 */
    static void dragMove(float x, float y) {
        try {
            if ((predBase == null) || (com.oupao.wiz.MiniUI.task == null)) {
                return;
            }
            if (com.oupao.wiz.MiniUI.tid != predTid) {
                return;
            }
            float dx = x - predX;
            float dy = y - predY;
            if ((Math.abs(dx) < 6f) && (Math.abs(dy) < 6f)) {
                return;
            }
            // 手指一直压在窗口上 → 按下点跟着走，"包住按下点"的校验才不会被拖动误判
            dnX = x;
            dnY = y;
            var r = new android.graphics.Rect(predBase);
            r.offset(Math.round(dx), Math.round(dy));
            predRect = r;
            predAt = android.os.SystemClock.uptimeMillis();
        } catch (Throwable __e) {
            com.oupao.wiz.MiniUI.ex(__e);
        }
    }

    /**
     * 取"当前可见矩形"：系统上报比预测新就用系统的，否则用预测的。
     * 只有 `rectOf` 这条路会被替换，其余（isBig / size / dock）仍用原值。
     */
    static boolean rectBest(Object t, android.graphics.Rect out) {
        try {
            if (!com.oupao.wiz.MiniUI.rectOf(t, out)) {
                return false;
            }
            long now = android.os.SystemClock.uptimeMillis();
            if ((obsLast == null) || (!obsLast.equals(out))) {
                obsLast = new android.graphics.Rect(out);
                obsAt = now;
            }
            if ((predRect != null) && (predTid == com.oupao.wiz.Win.taskId(t))
                    && (predAt > obsAt) && ((now - predAt) < 3000L)) {
                out.set(predRect);
                com.oupao.wiz.MiniUI.lg("MiniUI: rectBest 用预测 " + out
                        + " (系统值还停在拖动前)");
            }
            return true;
        } catch (Throwable __e) {
            com.oupao.wiz.MiniUI.ex(__e);
            return false;
        }
    }

    /**
     * 叠层矩形至少要把"按下点"包住 —— 包不住说明拿到的是过期 bounds
     * （窗口已经挪走），这时先别露面，等下一次同步；否则用户看到的就是
     * "遮罩/按钮先出现在旧位置，然后闪过去"。
     */
    static boolean hitOK(android.graphics.Rect r) {
        try {
            if ((dnX < 0f) || (dnY < 0f) || (r == null)) {
                return true;                      // 没有按下点信息就不拦
            }
            return new android.graphics.Rect(r.left - 12, r.top - 12,
                    r.right + 12, r.bottom + 12).contains((int) dnX, (int) dnY);
        } catch (Throwable __e) {
            return true;
        }
    }

    // 3164：叠层是否已经"露面"过；以及上一次同步到的矩形签名
    private static boolean revealed;
    private static long lastSig;
    private static int stableCnt;
    private static long showAt;
    /** 3166：最近一次移动/拖动的时间；决定叠层"最短延迟" */
    private static long lastDragAt;
    private static long revealMinMs = 200L;
    private static android.graphics.Rect lastShownRect;

    /**
     * 3145：立刻按当前窗口矩形重新对齐叠层（不动定时器、不动系统窗口）。
     *
     * 拖动迷你窗结束后，系统的 Task bounds 可能晚一拍才更新；show() 那一帧用的
     * 就是旧值 → 静音/放大按钮和遮罩停在旧位置。这里按 0/50…1100ms 补同步。
     *
     * 3164：**连续两次读到同一个矩形才算稳定**，此时才淡入。
     * 只要手指快一点（拖动刚松手就长按），第一次读到的还是旧 bounds，
     * 之前会先淡入再被 resync 挪走 → 看起来"错位后闪一下"。
     */
    static void syncNow() {
        try {
            if (!com.oupao.wiz.MiniUI.on) {
                return;
            }
            Object t = com.oupao.wiz.MiniUI.task;
            com.oupao.wiz.MiniView vv = com.oupao.wiz.MiniUI.v;
            android.graphics.Rect r = com.oupao.wiz.MiniUI.tmp;
            if ((t == null) || (vv == null) || (r == null)) {
                return;
            }
            if (com.oupao.wiz.MiniUI.rectBest(t, r)) {
                vv.setMini(r);
                if (com.oupao.wiz.MiniUI.prev == null) {
                    com.oupao.wiz.MiniUI.prev = new android.graphics.Rect(r);
                } else {
                    com.oupao.wiz.MiniUI.prev.set(r);
                }
                com.oupao.wiz.MiniUI.idle = 0;
                long sig = (((long) r.left) << 48) ^ (((long) r.top) << 32)
                        ^ (((long) r.right) << 16) ^ ((long) r.bottom);
                if (sig == com.oupao.wiz.MiniUI.lastSig) {
                    com.oupao.wiz.MiniUI.stableCnt++;
                } else {
                    com.oupao.wiz.MiniUI.stableCnt = 0;
                    com.oupao.wiz.MiniUI.lastSig = sig;
                }
                // 3188：矩形还得盖得住按下点，否则就是过期 bounds —— 先别露面，
                // 等下一次同步（预测值/系统值总有一个是对的）。
                // 有拖动预测时矩形本身就是可信的，露面门槛放低（48ms）：
                // 用户反馈"振动了还要等一会"，等的就是这几十毫秒到几百毫秒。
                boolean trusted = (predRect != null) && (predTid == com.oupao.wiz.MiniUI.tid)
                        && (predAt > obsAt);
                long need = trusted ? 48L : com.oupao.wiz.MiniUI.revealMinMs;
                if (!com.oupao.wiz.MiniUI.revealed && com.oupao.wiz.MiniUI.stableCnt >= 1
                        && com.oupao.wiz.MiniUI.hitOK(r)
                        && android.os.SystemClock.uptimeMillis() - com.oupao.wiz.MiniUI.showAt
                        >= need) {
                    com.oupao.wiz.MiniUI.revealed = true;
                    // 3167：露面前把**缩放**归位并按当前矩形重设 pivot，然后只做淡入。
                    // 以前沿用 show() 时的旧 pivot 做 0.86→1 的缩放动画，
                    // 拖动过窗口时视觉上就像"从别的地方飞过来"。
                    vv.setScaleX(1.0f);
                    vv.setScaleY(1.0f);
                    vv.setPivotX(((float) r.centerX()));
                    vv.setPivotY(((float) r.centerY()));
                    vv.animate().alpha(1.0f).setDuration(120).start();
                    com.oupao.wiz.MiniUI.lg("MiniUI: 位置稳定 → 淡入 " + r);
                }
            }
        } catch (Throwable __e) {
            com.oupao.wiz.MiniUI.ex(__e);
        }
    }

    static void step()
    {
        try {
            if (com.oupao.wiz.MiniUI.on) {
                // 3188b：还没露面时，跟着 32ms 节拍判一次 —— 只等 resync 的
                // 50ms 网格会让"该露了"最晚多拖到下一个整 50ms。
                if (!com.oupao.wiz.MiniUI.revealed) {
                    com.oupao.wiz.MiniUI.syncNow();
                }
                var v0_1 = com.oupao.wiz.MiniUI.task;
                if (v0_1 != null) {
                    var v1_0 = com.oupao.wiz.MiniUI.v;
                    if (v1_0 != null) {
                        var v2 = com.oupao.wiz.MiniUI.tmp;
                        if ((v2 != null) && (com.oupao.wiz.MiniUI.rectBest(v0_1, v2))) {
                            v1_0.setMini(v2);
                            var v4_0 = com.oupao.wiz.MiniUI.prev;
                            int v5_0 = 1;
                            if (v4_0 != null) {
                                if (v2.left != v4_0.left) {
                                    if (v2.top != v4_0.top) {
                                        if (v2.right != v4_0.right) {
                                            if (v2.bottom != v4_0.bottom) {
                                                v5_0 = 0;
                                            } else {
                                            }
                                        } else {
                                        }
                                    } else {
                                    }
                                } else {
                                }
                            }
                            if (v4_0 == null) {
                                com.oupao.wiz.MiniUI.prev = new android.graphics.Rect(v2);
                            } else {
                                v4_0.set(v2);
                            }
                            if (v5_0 == 0) {
                                com.oupao.wiz.MiniUI.idle = (com.oupao.wiz.MiniUI.idle + 1);
                            } else {
                                com.oupao.wiz.MiniUI.idle = 0;
                            }
                            int v6_4 = 32;
                            // 3145：叠层只活 5 秒，没必要为省电把刷新降到 200ms——
                            // 那会让「拖动后立刻长按」出现最长 200ms 的错位。
                            var v0_2 = com.oupao.wiz.Hooks.main();
                            if (v0_2 != null) {
                                var v1_1 = com.oupao.wiz.MiniUI.tick;
                                if (v1_1 != null) {
                                    v0_2.postDelayed(v1_1, v6_4);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable __e_v0_3) {
            com.oupao.wiz.MiniUI.ex(__e_v0_3);
            return;
        }
        return;
    }
    static int uidOf(String p4)
    {
        int v0_0 = 0;
        try {
            if (p4 != null) {
                android.content.Context v1_2 = com.oupao.wiz.FlyH.ctx;
                if (v1_2 == null) {
                    v1_2 = com.oupao.wiz.Cfg.sysContext();
                } else {
                }
                if (v1_2 == null) {
                    v0_0 = com.oupao.wiz.UChk.procUid(p4);
                    if (v0_0 < 0) {
                        v0_0 = com.oupao.wiz.UChk.uidFromList(p4);
                    }
                } else {
                    v0_0 = com.oupao.wiz.UChk.pmUid(v1_2, p4);
                    if (v0_0 < 0) {
                        v0_0 = com.oupao.wiz.UChk.aiUid(v1_2, p4);
                        if (v0_0 < 0) {
                        }
                    }
                }
                var v1_1 = new StringBuilder();
                v1_1.append("MiniUI: uidOf ");
                v1_1.append(p4);
                v1_1.append(" -> ");
                v1_1.append(v0_0);
                com.oupao.wiz.MiniUI.lg(v1_1.toString());
            }
        } catch (Throwable __e_v1) {
            com.oupao.wiz.MiniUI.lg("MiniUI: uidOf EX");
            return -1;
        }
        return v0_0;
    }
}