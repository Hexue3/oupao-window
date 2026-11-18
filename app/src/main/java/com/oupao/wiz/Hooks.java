package com.oupao.wiz;
public class Hooks {
    public static ClassLoader CL;
    private static android.graphics.Rect miniOrg;
    private static float miniScale;
    private static int miniTid;
    static int sCapH;
    static int sCapW;
    private static Object sCaption;
    private static long sDownT;
    private static float sDownX;
    private static float sDownY;
    /** 3173：最近一次 MOVE 的位移（给小窗手势判定用：区分横向/纵向） */
    static float sMoveDx;
    static float sMoveDy;
    private static long sLastT;
    private static float sLastX;
    private static float sLastY;
    static float sLocX;
    static float sLocY;
    private static android.os.Handler sMain;
    private static int sOutT;
    private static boolean sOutside;
    private static android.graphics.Rect sRawRect;
    private static boolean sTapOK;
    private static Object sTask;
    private static boolean sWriteFixDone;
    public static boolean sysDone;
    private Hooks()
    {
        return;
    }
    static void adoptNewest()
    {
        java.lang.Object v0 = null;
        try {
            v0 = com.oupao.wiz.CapR.newestCap();
        } catch (Throwable __e_v0) {
            return;
        }
        if ((v0 != null) && (v0 != com.oupao.wiz.Hooks.sCaption)) {
            var v1_2 = com.oupao.wiz.CapR.newestTask();
            if (v1_2 != null) {
                com.oupao.wiz.Hooks.sTask = v1_2;
            }
            var v2 = com.oupao.wiz.CapR.newestRect();
            if (v2 != null) {
                com.oupao.wiz.Hooks.sRawRect = v2;
            }
            com.oupao.wiz.Hooks.sCaption = v0;
            com.oupao.wiz.SLog.v("BarTap: \u63a5\u7ba1\u5269\u4e0b\u7684\u5c0f\u7a97\u4e3a\u5f53\u524d\u5c0f\u7a97");
        }
        return;
    }
    public static void applyMiniRect(android.graphics.Rect p6, Object p7, Object p8, float p9)
    {
        try {
            if ((p6 != null) && ((p7 != null) && (p8 != null))) {
                Object[] v0_5 = new Object[3];
                v0_5[0] = p6;
                v0_5[1] = p7;
                v0_5[2] = Float.valueOf(p9);
                com.oupao.wiz.Rt.callLoose(p8, "resizeTaskByVisibleBounds", v0_5);
                Object[] v0_1 = new Object[5];
                v0_1[0] = p7;
                v0_1[1] = Integer.valueOf(2);
                v0_1[2] = p6;
                v0_1[3] = p6;
                v0_1[4] = Float.valueOf(p9);
                com.oupao.wiz.Rt.callLoose(p8, "updateTaskExtraInfo", v0_1);
                Object[] v0_4 = new Object[1];
                v0_4[0] = p7;
                var v0_6 = com.oupao.wiz.Rt.callLoose(p8, "getFlexibleInfoManager", v0_4);
                if (v0_6 != null) {
                    Object[] v1_10 = new Object[1];
                    v1_10[0] = Boolean.TRUE;
                    com.oupao.wiz.Rt.callLoose(v0_6, "markVisibleBoundsManuallyChanged", v1_10);
                }
                var v0_8 = new StringBuilder();
                v0_8.append("MiniBig: apply ");
                v0_8.append(p6);
                com.oupao.wiz.SLog.i(v0_8.toString());
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    static String barInfo()
    {
        if (com.oupao.wiz.Hooks.viewRectNow() == null) {
            return "bar=null";
        } else {
            var v1_1 = new StringBuilder();
            v1_1.append("bar=");
            v1_1.append(com.oupao.wiz.Hooks.viewRectNow());
            return v1_1.toString();
        }
    }
    /**
     * 关闭/全屏校验的公共流程（原来 checkClosed/checkFull 各一份，只差日志与 verify 目标）。
     *
     * @param tag       日志前缀（"关闭小窗" / "全屏"）
     * @param timeoutMsg 超时时要补在 ": 校验超时" 后面的内容
     * @param done      已确认时打的消息
     * @param n         当前递归层数
     * @param which     1=verifyClosed 2=verifyFull
     */
    private static void checkX(String tag, String timeoutMsg, String done, int n, int which)
    {
        if (com.oupao.wiz.Hooks.sCaption != null) {
            if (n >= 2) {
                com.oupao.wiz.SLog.i(tag + ": \u6821\u9a8c\u8d85\u65f6(" + timeoutMsg + ")");
                return;
            } else {
                if (which == 1) {
                    com.oupao.wiz.Hooks.verifyClosed((n + 1));
                } else {
                    com.oupao.wiz.Hooks.verifyFull((n + 1));
                }
                return;
            }
        } else {
            com.oupao.wiz.SLog.i(tag + ": " + done);
            return;
        }
    }
    public static void checkClosed(int p3)
    {
        com.oupao.wiz.Hooks.checkX("\u5173\u95ed\u5c0f\u7a97",
                "\u539f\u751f\u7b56\u7565\u5df2\u6267\u884c, \u53ef\u80fd\u4ecd\u5728\u8fc7\u6e21",
                "\u5df2\u786e\u8ba4\u5173\u95ed", p3, 1);
    }
    public static void checkFull(int p3)
    {
        com.oupao.wiz.Hooks.checkX("\u5168\u5c4f",
                "\u53ef\u80fd\u4ecd\u5728\u8fc7\u6e21",
                "\u5df2\u786e\u8ba4\u79bb\u5f00\u5c0f\u7a97", p3, 2);
    }
    static android.graphics.Rect clampRect(android.graphics.Rect p8)
    {
        com.oupao.wiz.FlyH.dims();
        var v0 = com.oupao.wiz.FlyH.sw;
        var v1 = com.oupao.wiz.FlyH.sh;
        var v2 = p8.width();
        var v3 = p8.height();
        if ((v2 > v0) || (v3 > v1)) {
            return p8;
        } else {
            int v4 = p8.left;
            if (v4 < 0) {
                v4 = 0;
            }
            if ((v4 + v2) > v0) {
                v4 = (v0 - v2);
                if (v4 < 0) {
                    v4 = 0;
                }
            }
            int v5_1 = p8.top;
            if (v5_1 < 0) {
                v5_1 = 0;
            }
            if ((v5_1 + v3) > v1) {
                v5_1 = (v1 - v3);
                if (v5_1 < 0) {
                    v5_1 = 0;
                }
            }
            return new android.graphics.Rect(v4, v5_1, (v4 + v2), (v5_1 + v3));
        }
    }
    static void clickNativeCloseButton()
    {
        var v0_0 = com.oupao.wiz.Hooks.sCaption;
        if ((v0_0 instanceof android.view.View)) {
            var v1_2 = com.oupao.wiz.Rt.get(v0_0, "mCloseView");
            if ((v1_2 instanceof android.view.View)) {
                var v2_2 = ((android.view.View) v0_0);
                var v3_1 = ((android.view.View) v1_2);
                try {
                    if (!v3_1.performClick()) {
                        int[] v4_2 = new int[2];
                        v3_1.getLocationInWindow(v4_2);
                        int[] v5_1 = new int[2];
                        v2_2.getLocationInWindow(v5_1);
                        var v7_1 = v4_2[1];
                        var v9_1 = v5_1[1];
                        var v10_4 = ((Math.max(1, (v3_1.getWidth() / 2)) + v4_2[0]) - v5_1[0]);
                        var v11_5 = ((Math.max(1, (v3_1.getHeight() / 2)) + v7_1) - v9_1);
                        var v12_1 = android.os.SystemClock.uptimeMillis();
                        var v14_1 = (v12_1 + 60);
                        var v17 = ((float) v10_4);
                        var v18 = ((float) v11_5);
                        var v4_3 = android.view.MotionEvent.obtain(v12_1, v14_1, 0, v17, v18, 0);
                        var v5_2 = android.view.MotionEvent.obtain(v12_1, v14_1, 1, v17, v18, 0);
                        v2_2.dispatchTouchEvent(v4_3);
                        v2_2.dispatchTouchEvent(v5_2);
                        v4_3.recycle();
                        v5_2.recycle();
                        com.oupao.wiz.SLog.i("\u5173\u95ed\u5c0f\u7a97: \u5df2\u53d1\u9001\u5408\u6210\u70b9\u51fb");
                    } else {
                        com.oupao.wiz.SLog.i("\u5173\u95ed\u5c0f\u7a97: performClick=true");
                        return;
                    }
                } catch (Throwable __e_v0_1) {
                    com.oupao.wiz.SLog.e("\u5173\u95ed\u5c0f\u7a97: \u5408\u6210\u70b9\u51fb\u5931\u8d25", __e_v0_1);
                    return;
                }
            }
        }
        return;
    }
    static void clipExtra(ClassLoader p3)
    {
        java.lang.Class v0_1 = null;
        try {
            v0_1 = com.oupao.wiz.Rt.cls(p3, "com.android.server.clipboard.ClipboardService$ClipboardImpl");
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_1 != null) {
            var v1_2 = com.oupao.wiz.Rt.methodByName(v0_1, "checkAndSetPrimaryClip", 6);
            if (v1_2 != null) {
                com.oupao.wiz.XBridge.hookRaw(v1_2, new com.oupao.wiz.HookL());
                com.oupao.wiz.SLog.i("Link: checkAndSetPrimaryClip \u5df2\u6302(\u7edf\u4e00\u5165\u53e3)");
            }
        }
        return;
    }
    static void closeFlexible()
    {
        var v0 = com.oupao.wiz.Hooks.main();
        if (v0 == null) {
            com.oupao.wiz.Hooks.doClose();
            return;
        } else {
            v0.post(new com.oupao.wiz.Act(1, 0));
            return;
        }
    }

    /** 多开小窗：关闭指定 caption 的那扇窗（窗外点击必须走这条） */
    static void closeFlexibleFor(final Object caption)
    {
        var h = com.oupao.wiz.Hooks.main();
        if (h == null) {
            com.oupao.wiz.Hooks.doCloseFor(caption);
            return;
        }
        h.post(new Runnable() {
            public void run() {
                com.oupao.wiz.Hooks.doCloseFor(caption);
            }
        });
    }
    public static boolean decide(boolean p2, int p3)
    {
        if (!p2) {
            if (p3 < 1) {
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }
    static void dispatch(int p0, com.oupao.wiz.XMP p1)
    {
        switch (p0) {
            case 1:
            default:
                com.oupao.wiz.Hooks.onAttachInfo(p1);
                return;
            case 2:
                com.oupao.wiz.Hooks.onWarn(p1);
                return;
            case 3:
                com.oupao.wiz.Hooks.onVideoCheck(p1);
                return;
            case 28:
                com.oupao.wiz.Hooks.onToggleRecents(p1);
                return;
            case 4:
                com.oupao.wiz.Hooks.onCaptionCtor(p1);
                return;
            case 5:
                com.oupao.wiz.Hooks.onCaptionRect(p1);
                return;
            case 6:
                com.oupao.wiz.Hooks.onCaptionRect(p1);
                return;
            case 7:
                com.oupao.wiz.Hooks.onCaptionRelease(p1);
                return;
            case 8:
                com.oupao.wiz.Hooks.onCaptionTouch(p1);
                return;
            case 9:
                com.oupao.wiz.Hooks.onCapStyle(p1);
                return;
            case 10:
                com.oupao.wiz.Hooks.onCapStyle(p1);
                return;
            case 11:
                com.oupao.wiz.Hooks.onCornerSetup(p1);
                return;
            case 12:
                com.oupao.wiz.Hooks.onCornerAlpha(p1);
                return;
            case 13:
                com.oupao.wiz.Hooks.onCornerAlpha(p1);
                return;
            case 14:
                com.oupao.wiz.Hooks.onCapStyle(p1);
                return;
            case 15:
                com.oupao.wiz.Hooks.onCapTap(p1);
                return;
            case 16:
                com.oupao.wiz.Hooks.onCornerFlag(p1);
                return;
            case 17:
                com.oupao.wiz.Hooks.onMenuShowing(p1);
                return;
            case 18:
                com.oupao.wiz.Hooks.onGestureMode(p1);
                return;
            case 19:
                return;
            case 20:
                com.oupao.wiz.Hooks.onCapRegion(p1);
                return;
            case 21:
                com.oupao.wiz.Hooks.onMiniAnim(p1);
                return;
            case 22:
                com.oupao.wiz.Hooks.onMiniInfo(p1);
                return;
            case 23:
                // 2817 已废弃: com.oupao.wiz.Hooks.onHandleRegion(p1);
                return;
            case 24:
            case 25:
                // 2817 已废弃: com.oupao.wiz.Hooks.onScaleProbe(p1);
                return;
            case 26:
            case 27:
                // 2817 已废弃: com.oupao.wiz.Hooks.onCapTouch(p1);
                return;
        }
    }
    // 3189：关闭去重。实测一次"窗外单击"会触发两次 关闭小窗: 开始（间隔 ~150ms），
    // 两次都进 tryNativeExit() → 同一个 Task 上连续两次 exitFlexibleTaskWindowInnerLocked，
    // ROM 的退出状态机会卡住（表现：小窗卡死、点不动、过一会一次性关掉多扇窗）。
    private static Object closeCap;
    private static Object closeTask;
    private static long closeAt;

    public static void doClose()
    {
        doCloseTarget(com.oupao.wiz.Hooks.sCaption, com.oupao.wiz.Hooks.sTask);
    }

    /** 多开小窗：按当前 caption 对应的 task 关闭，不能用全局 sCaption/sTask 猜 */
    static void doCloseFor(Object caption)
    {
        Object task = null;
        try {
            task = com.oupao.wiz.CapR.task(caption);
        } catch (Throwable ignore) {
        }
        if (task == null) {
            try {
                task = com.oupao.wiz.Rt.get(caption, "mTask");
            } catch (Throwable ignore) {
            }
        }
        doCloseTarget(caption, task != null ? task : com.oupao.wiz.Hooks.sTask);
    }

    private static void doCloseTarget(Object caption, Object task)
    {
        Object v1_0 = caption;
        Object v0_0 = task;
        long now = android.os.SystemClock.uptimeMillis();
        if ((now - closeAt) < 800L) {
            // 同一个窗口（同一 caption / 同一 Task）800ms 内只允许一次原生退出；
            // 两扇不同的窗各关各的，不受影响。
            boolean sameCap = (v1_0 != null) && (v1_0 == closeCap);
            boolean sameTask = (v0_0 != null) && (v0_0 == closeTask);
            boolean nullish = (v1_0 == null) && (v0_0 == null);
            if (sameCap || sameTask || nullish) {
                com.oupao.wiz.SLog.i("关闭小窗: 忽略重复请求 " + (now - closeAt) + "ms");
                return;
            }
        }
        closeCap = v1_0;
        closeTask = v0_0;
        closeAt = now;
        com.oupao.wiz.SLog.i("\u5173\u95ed\u5c0f\u7a97: \u5f00\u59cb");
        if (!com.oupao.wiz.Hooks.tryNativeExitFor(v1_0, v0_0)) {
            com.oupao.wiz.SLog.i("\u5173\u95ed\u5c0f\u7a97: \u539f\u751f\u7b56\u7565\u4e0d\u53ef\u7528, \u56de\u9000\u5408\u6210\u70b9\u51fb");
            com.oupao.wiz.Hooks.clickNativeCloseButton();
            return;
        } else {
            com.oupao.wiz.Hooks.verifyClosed(0);
            return;
        }
    }
    static void focus(Object p3)
    {
        try {
            if ((p3 != null) && (com.oupao.wiz.Hooks.sCaption != p3)) {
                java.lang.Object v0_1 = com.oupao.wiz.CapR.task(p3);
                var v1 = com.oupao.wiz.CapR.rect(p3);
                if (v0_1 == null) {
                    v0_1 = com.oupao.wiz.Rt.get(p3, "mTask");
                }
                if (v0_1 == null) {
                    if (v1 == null) {
                        com.oupao.wiz.SLog.v("BarTap: focus \u6ca1\u6709\u8be5\u5c0f\u7a97\u8bb0\u5f55");
                    }
                } else {
                }
                com.oupao.wiz.Hooks.sCaption = p3;
                if (v0_1 != null) {
                    com.oupao.wiz.Hooks.sTask = v0_1;
                }
                if (v1 != null) {
                    com.oupao.wiz.Hooks.sRawRect = v1;
                }
                com.oupao.wiz.SLog.v("BarTap: \u5df2\u5207\u5230\u6309\u4e0b\u7684\u90a3\u4e2a\u5c0f\u7a97");
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static String focusedInfo()
    {
        java.lang.String v0_0 = null;
        java.lang.Object v1_1 = null;
        try {
            v1_1 = com.oupao.wiz.Rt.invoke(com.oupao.wiz.Rt.methodByName(Class.forName("android.app.ActivityTaskManager"), "getService", 0), null, null);
        } catch (Throwable __e_v1) {
            return null;
        }
        if (v1_1 != null) {
            var v1_2 = com.oupao.wiz.Rt.callNoArgQ(v1_1, "getFocusedRootTaskInfo");
            if (v1_2 != null) {
                var v2_2 = new StringBuilder();
                v2_2.append(com.oupao.wiz.Rt.get(v1_2, "taskId"));
                v2_2.append("|");
                var v3_4 = com.oupao.wiz.Rt.get(v1_2, "topActivity");
                java.lang.String v5 = "";
                if ((v3_4 instanceof android.content.ComponentName)) {
                    v5 = ((android.content.ComponentName) v3_4).getClassName();
                }
                v2_2.append(v5);
                v0_0 = v2_2.toString();
            }
        }
        return v0_0;
    }
    public static int focusedUserId()
    {
        java.lang.Object v1_1 = null;
        try {
            v1_1 = com.oupao.wiz.Rt.invoke(com.oupao.wiz.Rt.methodByName(Class.forName("android.app.ActivityTaskManager"), "getService", 0), null, null);
        } catch (Throwable __e_v1) {
            return 0;
        }
        if (v1_1 != null) {
            var v1_2 = com.oupao.wiz.Rt.callNoArgQ(v1_1, "getFocusedRootTaskInfo");
            if (v1_2 != null) {
                var v1_3 = com.oupao.wiz.Rt.get(v1_2, "userId");
                if ((v1_3 instanceof Integer)) {
                    return ((Integer) v1_3).intValue();
                }
            }
        }
        return 0;
    }
    static void fullscreenFlexible()
    {
        var v0 = com.oupao.wiz.Hooks.sCaption;
        if (v0 != null) {
            var v1_2 = com.oupao.wiz.Rt.get(v0, "mFlexibleTaskController");
            java.lang.Object v2 = com.oupao.wiz.Hooks.sTask;
            if (v2 == null) {
                v2 = com.oupao.wiz.Rt.get(v0, "mTask");
            }
            if ((v1_2 != null) && (v2 != null)) {
                var v3_1 = com.oupao.wiz.Rt.cls(com.oupao.wiz.Hooks.CL, "com.android.server.wm.ExitFlexibleTaskToFullStrategy");
                if (v3_1 != null) {
                    Object[] v4_2 = new Object[2];
                    v4_2[0] = v2;
                    v4_2[1] = Boolean.TRUE;
                    var v3_2 = com.oupao.wiz.Rt.newInstance(((java.lang.Class) v3_1), v4_2);
                    if (v3_2 != null) {
                        Object[] v4_4 = new Object[1];
                        v4_4[0] = Integer.valueOf(16384);
                        com.oupao.wiz.Rt.callLoose(v3_2, "setSourceFrom", v4_4);
                        Object[] v4_6 = new Object[1];
                        v4_6[0] = v3_2;
                        com.oupao.wiz.Rt.callLoose(v1_2, "exitFlexibleTaskWindowInnerLocked", v4_6);
                        com.oupao.wiz.SLog.i("\u5168\u5c4f: \u5df2\u8c03\u7528\u539f\u751f\u5168\u5c4f\u7b56\u7565(0x4000)");
                        com.oupao.wiz.Hooks.verifyFull(0);
                        return;
                    }
                }
            }
        }
        com.oupao.wiz.SLog.i("\u5168\u5c4f: \u7f3a\u5c11 caption/controller/task \u6216\u7b56\u7565\u4e0d\u53ef\u7528");
        return;
    }
    public static boolean gameBlock()
    {
        boolean v0_0 = false;
        boolean v1 = false;
        try {
            v0_0 = com.oupao.wiz.Cfg.gameBlock();
            v1 = com.oupao.wiz.Sug.gameOn;
            var v2_2 = new StringBuilder();
            v2_2.append("FlyGame: gameBlock=");
            v2_2.append(v0_0);
            v2_2.append(" gameOn=");
            v2_2.append(v1);
            com.oupao.wiz.SLog.r("r-fly-game", v2_2.toString(), 5000L);
        } catch (Throwable __e_v0) {
            return false;
        }
        if ((v0_0 == false) || (!v1)) {
            return false;
        } else {
            return true;
        }
    }
    static void handleTap(Object p12, float p13, float p14, long p15)
    {
        if (!com.oupao.wiz.Hooks.sOutside) {
            var v0_3 = new StringBuilder();
            v0_3.append("BarTap: tap x=");
            v0_3.append(((int) p13));
            v0_3.append(" y=");
            v0_3.append(((int) p14));
            com.oupao.wiz.SLog.v(v0_3.toString());
            var v0_1 = com.oupao.wiz.Cfg.doubleTapClose();
            var v1_2 = com.oupao.wiz.Cfg.landscapeFullscreen();
            if ((v0_1 | v1_2) != false) {
                var v3_8 = com.oupao.wiz.Hooks.sLastT;
                if ((v3_8 > 0) && ((p15 - v3_8) <= 350)) {
                    var v7_4 = ((float) com.oupao.wiz.Ctx.dp(80));
                    if ((Math.abs((p13 - com.oupao.wiz.Hooks.sLastX)) < v7_4) && (Math.abs((p14 - com.oupao.wiz.Hooks.sLastY)) < v7_4)) {
                        if (!com.oupao.wiz.Hooks.onDoubleTapBar(p12, p13, p14)) {
                            var v3_11 = new StringBuilder();
                            v3_11.append("\u53cc\u51fb\u5ffd\u7565: \u4e0d\u5728\u5c0f\u767d\u6761\u4e0a ");
                            v3_11.append(com.oupao.wiz.Hooks.barInfo());
                            v3_11.append(" p1=");
                            v3_11.append(((int) com.oupao.wiz.Hooks.sLastX));
                            v3_11.append(",");
                            v3_11.append(((int) com.oupao.wiz.Hooks.sLastY));
                            v3_11.append(" p2=");
                            v3_11.append(((int) p13));
                            v3_11.append(",");
                            v3_11.append(((int) p14));
                            com.oupao.wiz.SLog.v(v3_11.toString());
                            com.oupao.wiz.Hooks.sLastT = p15;
                            com.oupao.wiz.Hooks.sLastX = p13;
                            com.oupao.wiz.Hooks.sLastY = p14;
                            return;
                        } else {
                            if (com.oupao.wiz.Hooks.sCaption == null) {
                                com.oupao.wiz.SLog.v("\u53cc\u51fb\u5ffd\u7565: \u65e0\u5c0f\u7a97");
                                com.oupao.wiz.Hooks.sLastT = 0;
                                return;
                            } else {
                                if ((!v1_2) || (!com.oupao.wiz.Hooks.isLandscape())) {
                                    // 3172：双击贴边已移除（系统自带的"左右滑小白条贴边"更好用）。
                                    if (!v0_1) {
                                        com.oupao.wiz.SLog.v("\u53cc\u51fb\u5ffd\u7565: \u975e\u6a2a\u5c4f\u4e14\u5173\u95ed\u5df2\u5173(\u6216\u6a2a\u5c4f\u5168\u5c4f\u672a\u5f00)");
                                        com.oupao.wiz.Hooks.sLastT = 0;
                                        return;
                                    } else {
                                        var v3_20 = new StringBuilder();
                                        v3_20.append("\u53cc\u51fb\u5c0f\u767d\u6761\u5173\u95ed\u5c0f\u7a97 ");
                                        v3_20.append(com.oupao.wiz.Hooks.barInfo());
                                        com.oupao.wiz.SLog.v(v3_20.toString());
                                        com.oupao.wiz.Hooks.closeFlexible();
                                        com.oupao.wiz.Hooks.sLastT = 0;
                                        return;
                                    }
                                } else {
                                    var v3_3 = new StringBuilder();
                                    v3_3.append("\u53cc\u51fb\u5c0f\u767d\u6761\u5168\u5c4f(\u6a2a\u5c4f) ");
                                    v3_3.append(com.oupao.wiz.Hooks.barInfo());
                                    com.oupao.wiz.SLog.v(v3_3.toString());
                                    com.oupao.wiz.Hooks.fullscreenFlexible();
                                    com.oupao.wiz.Hooks.sLastT = 0;
                                    return;
                                }
                            }
                        }
                    }
                }
                var v3_7 = new StringBuilder();
                v3_7.append("BarTap: \u5355\u51fb\u8bb0\u5f55 x=");
                v3_7.append(((int) p13));
                v3_7.append(" y=");
                v3_7.append(((int) p14));
                com.oupao.wiz.SLog.v(v3_7.toString());
                com.oupao.wiz.Hooks.sLastT = p15;
                com.oupao.wiz.Hooks.sLastX = p13;
                com.oupao.wiz.Hooks.sLastY = p14;
            }
            return;
        } else {
            return;
        }
    }
    public static void hookHeadsUp(ClassLoader p5)
    {
        java.lang.Class v0_2 = null;
        try {
            v0_2 = com.oupao.wiz.Rt.cls(p5, "com.android.systemui.statusbar.notification.headsup.HeadsUpManagerImpl");
        } catch (Throwable __e_v0_6) {
            com.oupao.wiz.SLog.e("HU: hook \u5f02\u5e38", __e_v0_6);
            return;
        }
        if (v0_2 != null) {
            // 先抓实例：构造时就存下来（简洁横幅不走 showNotification，只能靠这个）
            try {
                var v9 = com.oupao.wiz.XBridge.hookAllConstructors(v0_2, new com.oupao.wiz.Hooks$HUC());
                com.oupao.wiz.SLog.i("HU: ctor hooked n=" + ((v9 == null) ? 0 : v9.size()));
            } catch (Throwable __e_c) {
                com.oupao.wiz.SLog.e("HU: ctor hook 失败", __e_c);
            }
            Class[] v1_4 = new Class[2];
            v1_4[0] = com.oupao.wiz.Rt.cls(p5, "com.android.systemui.statusbar.notification.collection.NotificationEntry");
            v1_4[1] = Boolean.TYPE;
            var v0_1 = com.oupao.wiz.Rt.method(v0_2, "showNotification", v1_4);
            if (v0_1 != null) {
                com.oupao.wiz.XBridge.hookMethod(v0_1, new com.oupao.wiz.Hooks$HU());
                com.oupao.wiz.SLog.i("HU: hooked showNotification");
                return;
            } else {
                com.oupao.wiz.SLog.i("HU: no showNotification");
                return;
            }
        } else {
            com.oupao.wiz.SLog.i("HU: no HeadsUpManagerImpl");
            return;
        }
    }
    public static void hookHeadsUp2(ClassLoader p7)
    {
        java.lang.Class v0_3 = null;
        try {
            v0_3 = com.oupao.wiz.Rt.cls(p7, "com.oplus.systemui.notification.interruption.PeekOplusFilter");
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_3 != null) {
            Class[] v1_7 = new Class[1];
            v1_7[0] = com.oupao.wiz.Rt.cls(p7, "com.android.systemui.statusbar.notification.collection.NotificationEntry");
            var v0_1 = com.oupao.wiz.Rt.method(v0_3, "shouldSuppress", v1_7);
            if (v0_1 != null) {
                com.oupao.wiz.XBridge.hookMethod(v0_1, new com.oupao.wiz.Hooks$P());
                com.oupao.wiz.SLog.i("HU2: hooked Peek");
            }
        }
        var v0_5 = com.oupao.wiz.Rt.cls(p7, "com.oplus.systemui.notification.interruption.PluseOplusFilter");
        if (v0_5 != null) {
            Class[] v1_3 = new Class[1];
            v1_3[0] = com.oupao.wiz.Rt.cls(p7, "com.android.systemui.statusbar.notification.collection.NotificationEntry");
            var v0_6 = com.oupao.wiz.Rt.method(v0_5, "shouldSuppress", v1_3);
            if (v0_6 != null) {
                com.oupao.wiz.XBridge.hookMethod(v0_6, new com.oupao.wiz.Hooks$P());
                com.oupao.wiz.SLog.i("HU2: hooked Pluse");
            }
        }
        return;
    }
    static boolean inStrip(float p8, float p9, android.graphics.Rect p10)
    {
        if ((p10 != null) && ((p10.width() > 0) && (p10.height() > 0))) {
            var v0_5 = com.oupao.wiz.Ctx.dp(32);
            var v1_1 = com.oupao.wiz.Ctx.dp(6);
            var v2_1 = com.oupao.wiz.Ctx.dp(3);
            var v3_2 = ((p10.width() * 15) / 100);
            if ((((int) p8) >= ((p10.left + v3_2) - v1_1)) && (((int) p8) <= ((p10.right - v3_2) + v1_1))) {
                var v4_3 = p10.bottom;
                if ((((int) p9) >= (v4_3 - v0_5)) && (((int) p9) <= (v4_3 + v2_1))) {
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * 2803：输入法是否正在显示（system_server 侧）。
     *
     * 为什么要它：用户实测「**只要键盘弹起，横条上滑就失效；把『上滑横条转迷你窗』开关关掉就正常**」
     * → 说明是我们的重映射（0→4）在键盘弹起时把系统原生行为顶掉了，不是系统不会做。
     * 所以键盘弹起期间**不接管**，交回 ColorOS 原生逻辑。
     */
    /**
     * 2804：输入法是否正在显示。
     *
     * 直接用模块已有的 `Sug.imeVis`（由 `HookIme` 挂 `InputMethodManagerService.setImeWindowStatusLocked`
     * 维护，>=2 视为可见）。之前写的反射版拿不到实例、静默返回 false，所以闸门等于没开。
     */
    static boolean imeShown()
    {
        // 2824：验证用开关（默认关）。`persist.oupao.force_ime=1` 让代码以为键盘在，
        //   用来在没有键盘的情况下验证"重放"这条链路（坐标是否正确、补发的手势能不能真转迷你窗）。
        try {
            if ("1".equals(com.oupao.wiz.Cfg.getProp("persist.oupao.force_ime", ""))) {
                return true;
            }
        } catch (Throwable ignore) {
        }
        return com.oupao.wiz.Sug.imeVis != 0;
    }

    /** 2812：InputMethodManagerService 实例（由 HookIme 抓到） */
    public static Object sIme;

    /** 2807：为本次手势主动收起键盘的时刻（收完就当作"没有键盘"，允许重映射） */
    private static long miniHideImeAt;



    static void installFlyMini(ClassLoader p6)
    {
        java.lang.Class v0_1 = null;
        try {
            // 2817 已废弃: com.oupao.wiz.Hooks.probeHandleRegion(p6);      // 2801 诊断
        } catch (Throwable ignore) {
        }
        try {
            // 2817 已废弃: com.oupao.wiz.Hooks.installScaleProbe(p6);      // 2815 探针
        } catch (Throwable ignore) {
        }
        try {
            // 2817 已废弃: com.oupao.wiz.Hooks.installCapTouchProbe(p6);   // 2816 探针
        } catch (Throwable ignore) {
        }
        try {
            v0_1 = com.oupao.wiz.Rt.cls(p6, "com.android.server.wm.FlexibleTaskScaleManager");
        } catch (Throwable __e_v0_7) {
            com.oupao.wiz.SLog.e("FlyMini: \u6302\u94a9\u5f02\u5e38", __e_v0_7);
            return;
        }
        if (v0_1 != null) {
            Class[] v1_3 = new Class[5];
            v1_3[0] = Double.TYPE;
            v1_3[1] = Integer.TYPE;
            v1_3[2] = Float.TYPE;
            v1_3[3] = android.graphics.Point.class;
            v1_3[4] = android.graphics.Point.class;
            var v0_2 = com.oupao.wiz.Rt.method(v0_1, "getGestureMode", v1_3);
            if (v0_2 == null) {
                com.oupao.wiz.SLog.i("FlyMini: getGestureMode \u672a\u627e\u5230(\u8df3\u8fc7)");
            } else {
                com.oupao.wiz.XBridge.hookMethod(v0_2, new com.oupao.wiz.Hook(18));
                com.oupao.wiz.SLog.i("FlyMini: getGestureMode \u5df2\u6302\u94a9");
                var v0_6 = com.oupao.wiz.Rt.cls(p6, "com.android.server.wm.FlexibleTaskScaleManager");
                Class[] v3_5 = new Class[7];
                v3_5[0] = Integer.TYPE;
                v3_5[1] = float[].class;
                v3_5[2] = Integer.TYPE;
                v3_5[3] = Integer.TYPE;
                v3_5[4] = android.graphics.Point.class;
                v3_5[5] = Boolean.TYPE;
                v3_5[6] = Boolean.TYPE;
                var v2_7 = com.oupao.wiz.Rt.method(v0_6, "getGestureModeByAngleAndDis", v3_5);
                if (v2_7 != null) {
                    com.oupao.wiz.XBridge.hookMethod(v2_7, new com.oupao.wiz.FlyDiag$C1());
                    com.oupao.wiz.SLog.i("FlyMini: getGestureModeByAngleAndDis \u5df2\u6302\u94a9(+\u63a2\u9488)");
                }
            }
        }
        // ★ 3079：键盘弹起时 ROM 用「输入法手势守卫」把上滑挡在迷你窗分支之外
        //   （`FlexiblePointerHandler#isPhoneLandScreenAndImeShowing()`）。这是"开键盘就不转迷你"的真因。
        //   我们只在用户刚上滑的 ~900ms 窗口里把它压成 false（见 onGestureMode），
        //   其余时间完全不动，输入法行为保持原样。
        try {
            java.lang.Class<?> ph = com.oupao.wiz.Rt.cls(p6, "com.android.server.wm.FlexiblePointerHandler");
            if (ph == null) {
                com.oupao.wiz.SLog.i("FlyMini: 找不到 FlexiblePointerHandler（IME 守卫不挂）");
            } else {
                java.lang.reflect.Method mg = com.oupao.wiz.Rt.methodByName(ph, "isPhoneLandScreenAndImeShowing", 0);
                if (mg == null) {
                    com.oupao.wiz.SLog.i("FlyMini: 未找到 isPhoneLandScreenAndImeShowing");
                } else {
                    com.oupao.wiz.XBridge.hookMethod(mg, new com.oupao.wiz.HookImeGuard());
                    com.oupao.wiz.SLog.i("FlyMini: isPhoneLandScreenAndImeShowing 已挂钩（窗口内放行）");
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("FlyMini: IME 守卫挂钩异常", t);
        }
        return;
    }
    static void installMiniAnim(ClassLoader p5)
    {
        java.lang.Class v0_3 = null;
        try {
            v0_3 = com.oupao.wiz.Rt.cls(p5, "com.android.server.wm.FlexibleWindowAnimator");
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.SLog.e("MiniAnim: anim install EX", __e_v0_1);
            java.lang.Class v0_4 = null;
            try {
                v0_4 = com.oupao.wiz.Rt.cls(p5, "com.android.server.wm.FlexibleTaskController");
            } catch (Throwable v0_5) {
                com.oupao.wiz.SLog.e("MiniAnim: info install EX", v0_5);
                return;
            }
            if (v0_4 != null) {
                var v1_3 = com.oupao.wiz.Rt.methodByName(v0_4, "updateTaskExtraInfo", 5);
                if (v1_3 != null) {
                    com.oupao.wiz.XBridge.hookMethod(v1_3, new com.oupao.wiz.Hook(22));
                    com.oupao.wiz.SLog.i("MiniAnim: info \u5df2\u6302\u94a9");
                }
            }
            return;
        }
        if (v0_3 == null) {
        } else {
            var v1_7 = com.oupao.wiz.Rt.methodByName(v0_3, "startBottomHandleReboundTaskSpringAnim", 6);
            if (v1_7 == null) {
            } else {
                com.oupao.wiz.XBridge.hookMethod(v1_7, new com.oupao.wiz.Hook(21));
                com.oupao.wiz.SLog.i("MiniAnim: anim \u5df2\u6302\u94a9");
            }
        }
    }
    public static void installSystem(ClassLoader p10)
    {
        if (com.oupao.wiz.XBridge.xp == null) {
            return;
        }
        if (com.oupao.wiz.Hooks.sysDone) {
            com.oupao.wiz.SLog.i("oupao: installSystem 已锁定(本次启动不再重复安装)");
            com.oupao.wiz.Hooks.clipExtra(p10);
            return;
        }
        com.oupao.wiz.Cfg.dump();
        com.oupao.wiz.Hooks.CL = p10;
        com.oupao.wiz.SLog.i("oupao: installSystem 开始");
        com.oupao.wiz.HookC.install(p10);
        com.oupao.wiz.FlyBar.install(p10);
        try {
            com.oupao.wiz.Idx.start();
        } catch (Throwable ignore) {
        }
        com.oupao.wiz.Diag.put("android", "1");
        com.oupao.wiz.Diag.put("SDK", String.valueOf(com.oupao.wiz.Sys.sdk()));
        boolean hasCapView = com.oupao.wiz.Rt.cls(p10, "com.android.server.wm.FlexibleCaptionView") != null;
        com.oupao.wiz.Diag.put("Bar", hasCapView ? "1" : "0");
        com.oupao.wiz.Diag.put("ZoomSvc",
                com.oupao.wiz.Rt.cls(p10, "com.android.server.wm.OplusZoomWindowManagerService") == null ? "0" : "1");
        // 2829：小窗横条/缩放这套能力位（「上滑横条转迷你窗」「迷你窗尺寸」依赖它）
        com.oupao.wiz.Caps.report("flex", hasCapView);

        // ===== 段0：通知 Sug 延迟初始化 =====
        try {
            com.oupao.wiz.Sug.delayInit();
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Sug初始化异常", t);
        }

        // ===== 段0b：Idle —— 闲鱼链接定向 =====
        try {
            int nIdle = com.oupao.wiz.Idle.install(p10);
            com.oupao.wiz.SLog.i("Idle: 钩子已挂 n=" + nIdle);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Idle: 安装异常", t);
        }

        // ===== 段1：Msg —— 通知钩子 =====
        try {
            Class v0_43 = com.oupao.wiz.Rt.cls(p10, "com.android.server.notification.NotificationManagerService");
            if (v0_43 == null) {
                com.oupao.wiz.SLog.i("Msg: 未找到 NotificationManagerService");
            } else {
                var v0_44 = com.oupao.wiz.Rt.methodByName(v0_43, "enqueueNotificationInternal", 12);
                if (v0_44 == null) {
                    com.oupao.wiz.SLog.i("Msg: 未找到 enqueueNotificationInternal");
                } else {
                    com.oupao.wiz.XBridge.hookMethod(v0_44, new com.oupao.wiz.HookN());
                    // 直接扫方法名（各版本签名不同，别写死 argc）；顺便把候选名字打到日志里
                    java.lang.reflect.Method cancelM = null;
                    try {
                        com.oupao.wiz.SLog.i("Msg: NMS 类 = " + v0_43.getName());
                        java.lang.reflect.Method[] all = null;
                        java.util.ArrayList<java.lang.reflect.Method> pool = new java.util.ArrayList<>();
                        Class<?> cc = v0_43;
                        while (cc != null) {
                            java.lang.reflect.Method[] dm = cc.getDeclaredMethods();
                            for (java.lang.reflect.Method x : dm) pool.add(x);
                            cc = cc.getSuperclass();
                        }
                        all = pool.toArray(new java.lang.reflect.Method[0]);
                        com.oupao.wiz.SLog.i("Msg: NMS 方法总数 = " + all.length);
                        for (java.lang.reflect.Method mm : all) {
                            String nm = mm.getName();
                            if (nm.toLowerCase().contains("cancel") && nm.toLowerCase().contains("notification")) {
                                com.oupao.wiz.SLog.i("Msg: 取消候选 " + nm + "/" + mm.getParameterTypes().length);
                                // 这个 ROM 没有 cancelNotificationWithTag，实际是 cancelNotificationInternal / cancelNotification
                                if ("cancelNotificationInternal".equals(nm) || "cancelNotification".equals(nm)) {
                                    try {
                                        com.oupao.wiz.XBridge.hookMethod(mm, new com.oupao.wiz.HookNC());
                                        com.oupao.wiz.SLog.i("Msg: 取消钩子挂上 " + nm + "/" + mm.getParameterTypes().length);
                                    } catch (Throwable ignore2) {
                                    }
                                }
                            }
                        }
                    } catch (Throwable ignore) {
                    }
                    com.oupao.wiz.SLog.i("Msg: 取消钩子安装结束");
                    com.oupao.wiz.SLog.i("Msg: 通知钩子已挂");
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Msg段异常", t);
        }

        // ===== 段2：Dim —— PowerGroup 屏幕策略 =====
        try {
            Class v0_49 = com.oupao.wiz.Rt.cls(p10, "com.android.server.power.PowerGroup");
            if (v0_49 == null) {
                com.oupao.wiz.SLog.i("Dim: PowerGroup 未找到");
            } else {
                com.oupao.wiz.XBridge.hookAllConstructors(v0_49, new com.oupao.wiz.HookD());
                var v0_51 = com.oupao.wiz.Rt.methodByName(v0_49, "updateScreenPolicyLocked", 5);
                if (v0_51 == null) {
                    com.oupao.wiz.SLog.i("Dim: 未找到 updateScreenPolicyLocked");
                } else if (com.oupao.wiz.XBridge.hookRaw(v0_51, new com.oupao.wiz.HookD()) != null) {
                    com.oupao.wiz.SLog.i("Dim: 屏幕策略钩子已挂");
                } else {
                    com.oupao.wiz.SLog.i("Dim: 钩子安装失败");
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Dim段异常", t);
        }

        // ===== 段3：Link —— 剪贴板 + 额外裁剪 + Act + 输入法 =====
        try {
            Class v0_57 = com.oupao.wiz.Rt.cls(p10, "com.android.server.clipboard.ClipboardService$ClipboardImpl");
            if (v0_57 == null) {
                com.oupao.wiz.SLog.i("Link: ClipboardImpl 未找到");
            } else {
                var v0_58 = com.oupao.wiz.Rt.methodByName(v0_57, "setPrimaryClip", 5);
                if (v0_58 != null) {
                    if (com.oupao.wiz.XBridge.hookRaw(v0_58, new com.oupao.wiz.HookL()) != null) {
                        com.oupao.wiz.SLog.i("Link: 剪贴板钩子已挂");
                    } else {
                        com.oupao.wiz.SLog.i("Link: 钩子安装失败");
                    }
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Link段异常", t);
        }
        try {
            com.oupao.wiz.Hooks.clipExtra(p10);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Link: clipExtra 异常", t);
        }
        try {
            com.oupao.wiz.Act.install(p10);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Act: 安装异常", t);
        }
        try {
            Class v0_6 = com.oupao.wiz.Rt.cls(p10, "com.android.server.inputmethod.InputMethodManagerService");
            if (v0_6 == null) {
                com.oupao.wiz.SLog.i("ImeHook: cls not found");
            } else {
                var v0_7 = com.oupao.wiz.Rt.methodByName(v0_6, "setImeWindowStatusLocked", 3);
                if (v0_7 == null) {
                    com.oupao.wiz.SLog.i("ImeHook: method not found");
                } else if (com.oupao.wiz.XBridge.hookRaw(v0_7, new com.oupao.wiz.HookIme()) != null) {
                    com.oupao.wiz.SLog.i("ImeHook: installed");
                } else {
                    com.oupao.wiz.SLog.i("ImeHook: hook failed");
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("ImeHook: ex", t);
        }

        // ===== 段4：WriteFix + 段5：VideoNoPause =====
        com.oupao.wiz.Caps.report("video", false);   // 下面挂上才改成 true
        try {
            com.oupao.wiz.Hooks.tryWriteFix(
                    com.oupao.wiz.Rt.cls(p10, "com.android.providers.settings.SettingsProvider"));
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("段4(WriteFix)异常", t);
        }
        try {
            Class v0_18 = com.oupao.wiz.Rt.cls(p10, "com.android.server.wm.TaskFragmentExtImpl");
            if (v0_18 == null) {
                com.oupao.wiz.SLog.i("VideoNoPause: 未找到类(跳过)");
            } else {
                var v0_20 = com.oupao.wiz.Rt.methodByName(v0_18, "shouldSkipPauseInRecentsAnim", 0);
                if (v0_20 == null) {
                    com.oupao.wiz.SLog.i("VideoNoPause: 未找到方法");
                } else {
                    com.oupao.wiz.XBridge.hookMethod(v0_20, new com.oupao.wiz.Hook(3));
                    com.oupao.wiz.SLog.i("VideoNoPause: 已挂钩 shouldSkipPauseInRecentsAnim");
                    com.oupao.wiz.Caps.report("video", true);
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("段5(VideoNoPause)异常", t);
        }

        // ===== 段6：BarTap —— FlexibleCaptionView 全套（双击小白条/横屏全屏/窗外点击）=====
        try {
            Class v0_26 = com.oupao.wiz.Rt.cls(p10, "com.android.server.wm.FlexibleCaptionView");
            if (v0_26 == null) {
                com.oupao.wiz.SLog.i("BarTap: 未找到类(跳过)");
            } else {
                com.oupao.wiz.XBridge.hookAllConstructors(v0_26, new com.oupao.wiz.Hook(4));
                com.oupao.wiz.SLog.i("BarTap: caption 构造函数已挂钩");
                Class v6_1 = com.oupao.wiz.Rt.cls(p10, "com.android.server.wm.Task");
                if (v6_1 == null) {
                    com.oupao.wiz.SLog.i("BarTap: 未找到类 Task(跳过)");
                } else {
                    Class[] v1_20 = new Class[2];
                    v1_20[0] = v6_1;
                    v1_20[1] = android.graphics.Rect.class;
                    var v2_7 = com.oupao.wiz.Rt.method(v0_26, "initCaptionViewAndAttachToTask", v1_20);
                    if (v2_7 != null) {
                        com.oupao.wiz.XBridge.hookMethod(v2_7, new com.oupao.wiz.Hook(5));
                        com.oupao.wiz.SLog.i("BarTap: initCaption 已挂钩");
                    }
                    var v2_10 = com.oupao.wiz.Rt.method(v0_26, "relayout", v1_20);
                    if (v2_10 != null) {
                        com.oupao.wiz.XBridge.hookMethod(v2_10, new com.oupao.wiz.Hook(6));
                        com.oupao.wiz.SLog.i("BarTap: relayout 已挂钩");
                    }
                    Class[] v2_13 = new Class[1];
                    v2_13[0] = v6_1;
                    var v3_8 = com.oupao.wiz.Rt.method(v0_26, "releaseFlexibleTaskCaptionView", v2_13);
                    if (v3_8 != null) {
                        com.oupao.wiz.XBridge.hookMethod(v3_8, new com.oupao.wiz.Hook(7));
                        com.oupao.wiz.SLog.i("BarTap: release(Task) 已挂钩");
                    }
                    Class[] v2_15 = new Class[2];
                    v2_15[0] = v6_1;
                    v2_15[1] = String.class;
                    var v3_13 = com.oupao.wiz.Rt.method(v0_26, "releaseFlexibleTaskCaptionView", v2_15);
                    if (v3_13 != null) {
                        com.oupao.wiz.XBridge.hookMethod(v3_13, new com.oupao.wiz.Hook(7));
                        com.oupao.wiz.SLog.i("BarTap: release(Task,String) 已挂钩");
                    }
                }
                var v2_17 = com.oupao.wiz.Rt.cls(p10, "com.android.server.wm.FlexibleTaskCaptionView");
                if (v2_17 != null) {
                    Class[] v3_16 = new Class[0];
                    var v3_17 = com.oupao.wiz.Rt.method(v2_17, "onCaptionViewRelease", v3_16);
                    if (v3_17 != null) {
                        com.oupao.wiz.XBridge.hookMethod(v3_17, new com.oupao.wiz.Hook(7));
                        com.oupao.wiz.SLog.i("BarTap: onCaptionViewRelease 已挂钩");
                    }
                }
                Class[] v3_20 = new Class[1];
                v3_20[0] = android.view.MotionEvent.class;
                var v3_21 = com.oupao.wiz.Rt.method(v0_26, "dispatchTouchEvent", v3_20);
                if (v3_21 != null) {
                    // 3184：**重新挂钩**（必须挂，右滑兜底贴边就在这条回调里）。
                    //   3182 曾关掉它来验证"是不是它在挡原生贴边" —— 结果是关掉后右滑依然没反应，
                    //   说明不是它挡的；而关掉它反而让我们的兜底也失效了。
                    com.oupao.wiz.XBridge.hookMethod(v3_21, new com.oupao.wiz.Hook(8));
                    com.oupao.wiz.SLog.i("BarTap: dispatchTouchEvent 已挂钩");
                }
                Class[] v1_25 = new Class[1];
                v1_25[0] = android.graphics.Region.class;
                var v1_26 = com.oupao.wiz.Rt.method(v0_26, "updateTouchableRegion", v1_25);
                if (v1_26 == null) {
                    com.oupao.wiz.SLog.i("FlyClose: 未找到 updateTouchableRegion");
                } else {
                    com.oupao.wiz.XBridge.hookMethod(v1_26, new com.oupao.wiz.Hook(20));
                    com.oupao.wiz.SLog.i("FlyClose: updateTouchableRegion 已挂钩");
                }
                // 3466 口径：caption 的 LP 只有 NOT_FOCUSABLE|NOT_TOUCH_MODAL，
                // 窗外关闭靠 updateTouchableRegion 扩区 + ACTION_UP 兜底，不补 WATCH_OUTSIDE。
                // 条的显隐不再由我们跟：胶囊在 caption 的 flexible_bottom 里，
                // ROM 淡出/恢复 caption 表面时它自然跟着走（3466 基线）。
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("段6(BarTap)异常", t);
        }

        // ===== 段7：ContentProvider.attachInfo 写修复监听 =====
        try {
            Class v0_33 = com.oupao.wiz.Rt.cls(p10, "android.content.ContentProvider");
            if (v0_33 != null) {
                Class[] v1_31 = new Class[2];
                v1_31[0] = android.content.Context.class;
                v1_31[1] = android.content.pm.ProviderInfo.class;
                var v0_34 = com.oupao.wiz.Rt.method(v0_33, "attachInfo", v1_31);
                if (v0_34 != null) {
                    com.oupao.wiz.XBridge.hookMethod(v0_34, new com.oupao.wiz.Hook(1));
                    com.oupao.wiz.SLog.i("WriteFix: attachInfo 监听已安装");
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("段7(attachInfo)异常", t);
        }

        // ===== 段8：迷你窗 / 迷你动画 / 标题栏 =====
        try {
            com.oupao.wiz.Hooks.installRecentsHook(p10);      // 2821：最近任务兜底信号
        } catch (Throwable ignore) {
        }
        try {
            int b4 = com.oupao.wiz.XBridge.getHookCount();
            com.oupao.wiz.Hooks.installFlyMini(p10);
            // 2829：能力位 —— 「上滑横条转迷你窗」/「迷你窗尺寸」全靠这一段的 getGestureMode
            com.oupao.wiz.Caps.report("mini", com.oupao.wiz.XBridge.getHookCount() > b4);
        } catch (Throwable t) {
            com.oupao.wiz.Caps.report("mini", false);
            com.oupao.wiz.SLog.e("FlyMini异常", t);
        }
        try {
            com.oupao.wiz.Hooks.installMiniAnim(p10);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("MiniAnim异常", t);
        }
        try {
            com.oupao.wiz.Cap.install(p10);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Cap异常", t);
        }

        com.oupao.wiz.Diag.put("SplitFinish",
                com.oupao.wiz.Rt.cls(p10, "com.android.server.wm.ActivityRecord") == null ? "0" : "1");
        com.oupao.wiz.SLog.i("oupao: installSystem 完成");
        com.oupao.wiz.Diag.flush();
        com.oupao.wiz.Hooks.sysDone = true;
    }
    static boolean isLandscape()
    {
        android.content.Context v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Ctx.get();
        } catch (Throwable __e_v0) {
            return false;
        }
        if ((v0_0 == null) || (v0_0.getResources().getConfiguration().orientation != 2)) {
            return false;
        } else {
            return true;
        }
    }
    static android.os.Handler main()
    {
        android.os.Handler v0_0 = com.oupao.wiz.Hooks.sMain;
        if (v0_0 == null) {
            v0_0 = new android.os.Handler(android.os.Looper.getMainLooper());
            com.oupao.wiz.Hooks.sMain = v0_0;
        }
        return v0_0;
    }
    public static boolean markOf(Object p3)
    {
        java.lang.Object v0_7 = null;
        try {
            Object[] v1_2 = new Object[0];
            v0_7 = com.oupao.wiz.Rt.callLoose(p3, "getSbn", v1_2);
        } catch (Throwable __e_v0) {
            return false;
        }
        if (v0_7 != null) {
            Object[] v2_2 = new Object[0];
            var v0_1 = com.oupao.wiz.Rt.callLoose(v0_7, "getNotification", v2_2);
            if (v0_1 != null) {
                var v0_3 = ((android.app.Notification) v0_1).extras;
                if (v0_3 != null) {
                    return v0_3.getBoolean("oupao_nohu", false);
                }
            }
        }
        return false;
    }
    /**
     * SystemUI 侧「屏蔽悬浮通知」要探测的配置键。
     *
     * 判定其实**只靠 `oupao_nohu` 标记**（由 system_server 的 [maybeNoHeadsUp]
     * 按「总开关 + 白名单」算好后写进通知 extras）；这里探测的键只是第二判据，
     * 而 SystemUI 进程读不到模块配置（SELinux），所以 probeSys 恒为负数，
     * 退化后完全依赖标记。统一返回同一个键，避免各钩子判据不一致。
     */
    public static String nohuKey(String p6)
    {
        return "oupao_nohu_all";
    }

    /**
     * 该不该拦这条通知的横幅 —— 三处拦截点（Hooks$HU / onPeek / onSimpleBanner）共用。
     *
     * 判定优先级（踩过两次坑后定下来的）：
     * 1. **能读到开关文件** → 以开关为准（值为 0 就是不拦）。
     *    这一条修的是「开关关了、但通知 extras 里还留着 oupao_nohu 标记 → 照样拦」。
     * 2. 读不到（-1 缺失 / -2 异常，常见于 SystemUI 进程无权限）→ 退回看标记。
     *    这一条修的是「只认开关后，SystemUI 读不到文件 → 完全不拦」。
     * 3. 两者都没有 → 不拦。
     */
    public static boolean nohuShouldBlock(Object p7, String p8)
    {
        if (p8 == null) {
            return false;
        }
        // 能读到全局开关（0/1）→ 用「开关 + 应用名单」判定：
        //   名单留空 = 全部应用（inList 里已处理），选了就只屏蔽选中的。
        // 读不到（-1/-2）→ 退回看通知标记（旧行为），保证 SystemUI 无权限时仍能拦。
        int cfg = com.oupao.wiz.Cfg.probeSys(com.oupao.wiz.Hooks.nohuKey(p8));
        if (cfg >= 0) {
            try {
                // 开关读得到，还要**名单也读得到**才敢用「空名单=全部应用」的语义。
                // 读不到名单（SystemUI 被 SELinux 挡住）时，绝不能当成"空=全部"，
                // 否则只要开关是开的，所有应用的横幅都会被拦掉。
                if (com.oupao.wiz.Cfg.listReadable(com.oupao.wiz.Cfg.K_NOHU_APPS)) {
                    return com.oupao.wiz.Cfg.nohuAppOn(p8);
                }
            } catch (Throwable t) {
                return cfg >= 1;
            }
        }
        return com.oupao.wiz.Hooks.markOf(p7);
    }

    // ---------- HeadsUpManager 实例（[Hooks$HU] 挂钩时顺手抓的） ----------

    private static volatile Object sHuMgr;

    public static void setHuMgr(Object p1)
    {
        if (p1 != null) {
            sHuMgr = p1;
        }
    }

    public static Object huMgr()
    {
        return sHuMgr;
    }

    /**
     * 立刻收掉某条通知的**横幅（HUN）**，返回是否真的收了。
     *
     * 为什么需要：点横幅时「横幅飞走」这个动作原本挂在通知展开转场上，
     * 而 [com.oupao.wiz.NotifFly.markZoom] 为了让小窗直接出现把转场剥掉了
     * → 没人收横幅 → 表现成「小窗先弹、横幅后消失」，顺序和用户要的相反。
     * 这里在发启动请求**之前**同步把横幅收掉，顺序就对了。
     */
    public static boolean dismissHeadsUp(String p4)
    {
        Object v0 = sHuMgr;
        if ((v0 == null) || (p4 == null)) {
            return false;
        }
        try {
            Object v1 = com.oupao.wiz.Rt.callLoose(v0, "isHeadsUpEntry", new Object[]{p4});
            if ((!(v1 instanceof Boolean)) || (!((Boolean) v1).booleanValue())) {
                return false;
            }
            com.oupao.wiz.Rt.callLoose(v0, "removeNotification",
                    new Object[]{p4, Boolean.TRUE, "oupao"});
            com.oupao.wiz.SLog.i("NFly: 横幅已先收起 key=" + p4);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void maybeNoHeadsUp(String p3, android.app.Notification p4)
    {
        boolean v0_4 = false;
        try {
            // 统一判定：总开关（oupao_nohu_all）&& 白名单（oupao_nohu_apps，留空=全部）。
            // 微信/QQ 不再有专用开关 —— 用户要求把这些选项全部改成全局 + 自定义应用。
            v0_4 = com.oupao.wiz.Cfg.nohuAppOn(p3);
        } catch (Throwable __e_v0) {
            return;
        }
        var v2_2 = new StringBuilder();
        v2_2.append("Msg: nofloat pkg=");
        v2_2.append(p3);
        v2_2.append(" on=");
        v2_2.append(v0_4);
        com.oupao.wiz.SLog.i(v2_2.toString());
        if (v0_4) {   // smali: if-eqz v0 → 开启时才打 nohu 标记
            p4.priority = 0;
            var v0_3 = p4.extras;
            if (v0_3 != null) {
                v0_3.putBoolean("oupao_nohu", true);
            }
        } else {
            // ★ 关掉开关时必须把标记摘掉。
            // 微信/QQ 会复用同一个 Notification 对象重新 post（改文字、加条数），
            // extras 里的 oupao_nohu 不清掉就会一直留着 → HU / onPeek / onSimpleBanner
            // 三个钩子都只看这个标记，表现就是「开关关了横幅照样不弹」。
            var v0_5 = p4.extras;
            if (v0_5 != null) {
                v0_5.remove("oupao_nohu");
            }
        }
        return;
    }
    public static boolean miniBig()
    {
        boolean v0_0 = false;
        try {
            v0_0 = com.oupao.wiz.Cfg.miniBig();
        } catch (Throwable __e_v0_1) {
            var v1_10 = new StringBuilder();
            v1_10.append("MiniBig: ex ");
            v1_10.append(__e_v0_1.toString());
            com.oupao.wiz.SLog.i(v1_10.toString());
            return false;
        }
        if (!v0_0) {
            com.oupao.wiz.SLog.i("MiniBig: enter");
            var v0_3 = com.oupao.wiz.Tap.cap();
            if (v0_3 != null) {
                if (com.oupao.wiz.Rt.callNoArg(v0_3, "isMiniZoomState") == Boolean.TRUE) {
                    java.lang.Object v2_2 = com.oupao.wiz.Win.capTask(v0_3);
                    if (v2_2 == null) {
                        v2_2 = com.oupao.wiz.Hooks.task();
                    }
                    if (v2_2 != null) {
                        var v3 = com.oupao.wiz.Rt.get(v0_3, "mFlexibleTaskController");
                        if (v3 != null) {
                            var v4_0 = com.oupao.wiz.Rt.callNoArg(v2_2, "getBounds");
                            if ((v4_0 instanceof android.graphics.Rect)) {
                                var v4_1 = ((android.graphics.Rect) v4_0);
                                var v10_3 = com.oupao.wiz.Rt.callNoArg(v2_2, "getTaskId");
                                if ((v10_3 instanceof Integer)) {
                                    var v5 = ((Integer) v10_3).intValue();
                                    var v6 = com.oupao.wiz.Hooks.scaleOf(v2_2);
                                    if (v6 > 0) {
                                        var v7_2 = com.oupao.wiz.Hooks.shrinkTo(new android.graphics.Rect(v4_1), v6);
                                        int v10_7 = 0;
                                        var v11_1 = com.oupao.wiz.Hooks.miniOrg;
                                        if ((v11_1 != null) && (com.oupao.wiz.Hooks.miniTid == v5)) {
                                            var v12_1 = com.oupao.wiz.Hooks.miniRect(v11_1);
                                            if ((v12_1 != null) && (Math.abs((v7_2.width() - v12_1.width())) <= 4)) {
                                                v10_7 = 1;
                                            }
                                        }
                                        float v9_1 = 0f;
                                        android.graphics.Rect v8_1 = null;
                                        if (v10_7 == 0) {
                                            com.oupao.wiz.Hooks.miniOrg = v7_2;
                                            com.oupao.wiz.Hooks.miniScale = v6;
                                            com.oupao.wiz.Hooks.miniTid = v5;
                                            var v8_0 = com.oupao.wiz.Hooks.miniRect(v7_2);
                                            if (v8_0 == null) {
                                                com.oupao.wiz.SLog.i("MiniBig: bail");
                                                return false;
                                            } else {
                                                v8_1 = com.oupao.wiz.Hooks.clampRect(v8_0);
                                                v9_1 = (((float) v8_1.width()) / ((float) v4_1.width()));
                                            }
                                        } else {
                                            v8_1 = com.oupao.wiz.Hooks.miniOrg;
                                            v9_1 = com.oupao.wiz.Hooks.miniScale;
                                            com.oupao.wiz.Hooks.miniOrg = null;
                                            com.oupao.wiz.Hooks.miniTid = 0;
                                        }
                                        Object[] v10_13 = new Object[3];
                                        v10_13[0] = v8_1;
                                        v10_13[1] = v2_2;
                                        v10_13[2] = Float.valueOf(v9_1);
                                        com.oupao.wiz.Rt.callLoose(v3, "resizeTaskByVisibleBounds", v10_13);
                                        com.oupao.wiz.Hooks.miniInfo(v3, v2_2, v7_2, v8_1, v9_1);
                                        com.oupao.wiz.Hooks.miniLog(v5, v4_1, v7_2, v8_1, v9_1);
                                        return true;
                                    } else {
                                        com.oupao.wiz.SLog.i("MiniBig: no scale");
                                    }
                                } else {
                                    com.oupao.wiz.SLog.i("MiniBig: no taskId");
                                }
                            } else {
                                com.oupao.wiz.SLog.i("MiniBig: no bounds");
                            }
                        } else {
                            com.oupao.wiz.SLog.i("MiniBig: no ctrl");
                        }
                    } else {
                        com.oupao.wiz.SLog.i("MiniBig: no task");
                    }
                } else {
                    com.oupao.wiz.SLog.i("MiniBig: not mini");
                }
            } else {
                com.oupao.wiz.SLog.i("MiniBig: no cap");
            }
            com.oupao.wiz.SLog.i("MiniBig: bail");
            return false;
        } else {
            return v0_0;
        }
    }
    static void miniInfo(Object p6, Object p7, android.graphics.Rect p8, android.graphics.Rect p9, float p10)
    {
        java.lang.Object v0_4 = null;
        try {
            Object[] v0_1 = new Object[5];
            v0_1[0] = p7;
            v0_1[1] = Integer.valueOf(2);
            v0_1[2] = p8;
            v0_1[3] = p9;
            v0_1[4] = Float.valueOf(p10);
            com.oupao.wiz.Rt.callLoose(p6, "updateTaskExtraInfo", v0_1);
            Object[] v0_3 = new Object[1];
            v0_3[0] = p7;
            v0_4 = com.oupao.wiz.Rt.callLoose(p6, "getFlexibleInfoManager", v0_3);
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_4 != null) {
            Object[] v1_7 = new Object[1];
            v1_7[0] = Boolean.TRUE;
            com.oupao.wiz.Rt.callLoose(v0_4, "markVisibleBoundsManuallyChanged", v1_7);
        }
        return;
    }
    static void miniLog(int p5, android.graphics.Rect p6, android.graphics.Rect p7, android.graphics.Rect p8, float p9)
    {
        try {
            var v0_1 = new StringBuilder();
            v0_1.append("MiniBig: tid=");
            v0_1.append(p5);
            v0_1.append(" scale=");
            v0_1.append(p9);
            v0_1.append(" layout=");
            v0_1.append(p6);
            v0_1.append(" cur=");
            v0_1.append(p7);
            v0_1.append(" tgt=");
            v0_1.append(p8);
            com.oupao.wiz.SLog.i(v0_1.toString());
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    static android.graphics.Rect miniRect(android.graphics.Rect p9)
    {
        try {
            var v0_0 = com.oupao.wiz.Cfg.miniScale();
            var v1_2 = ((p9.width() * v0_0) / 100);
            var v2_2 = ((p9.height() * v0_0) / 100);
            var v5_1 = (p9.centerX() - (v1_2 / 2));
            var v6_1 = (p9.centerY() - (v2_2 / 2));
            return new android.graphics.Rect(v5_1, v6_1, (v5_1 + v1_2), (v6_1 + v2_2));
        } catch (Throwable __e_v0) {
            return null;
        }
    }
    static void onAttachInfo(com.oupao.wiz.XMP p4)
    {
        var v0 = p4.thisObject;
        com.oupao.wiz.Rx.lazy(v0);
        if (v0 != null) {
            var v1 = v0.getClass();
            if ("com.android.providers.settings.SettingsProvider".equals(v1.getName())) {
                com.oupao.wiz.Hooks.tryWriteFix(v1);
                com.oupao.wiz.Diag.put("WriteFix", "1");
            }
        }
        return;
    }
    static void onCapRegion(com.oupao.wiz.XMP p9)
    {
        try {
            if (com.oupao.wiz.Cfg.flyClose() == 0) return;
            if ((p9.args == null) || (p9.args.length == 0)) return;
            if (!(p9.args[0] instanceof android.graphics.Region)) return;
            Object capObj = p9.thisObject;
            if (!(capObj instanceof android.view.View)) return;
            android.view.View capView = (android.view.View) capObj;
            android.graphics.Region original = (android.graphics.Region) p9.args[0];

            if (com.oupao.wiz.Rt.callNoArg(capView, "isMiniZoomState") == Boolean.TRUE) {
                com.oupao.wiz.SLog.i("FlyClose: 迷你态 不扩展(保持原区域)");
                return;
            }
            int cw = capView.getWidth();
            int ch = capView.getHeight();
            if (com.oupao.wiz.Hooks.miniScale()) {
                com.oupao.wiz.SLog.i("FlyClose: 迷你窗(缩放) 不扩展");
                return;
            }
            if (com.oupao.wiz.Hooks.mostlyOffScreen(capView)) {
                com.oupao.wiz.SLog.i("FlyClose: 窗口离屏 不扩展(保持原区域)");
                return;
            }
            if ((cw <= 0) || (ch <= 0)) return;

            android.graphics.Region out =
                    new android.graphics.Region(-20000, -20000, 20000, 20000);
            out.op(new android.graphics.Region(0, 0, cw, ch),
                    android.graphics.Region.Op.DIFFERENCE);
            int vg = 0;
            try {
                int[] gi = com.oupao.wiz.Hooks.gestureInsets(capView);
                int[] sc = com.oupao.wiz.Hooks.screenSize(capView);
                if ((gi != null) && (sc[0] > 0) && (sc[1] > 0)) {
                    int[] loc = new int[2];
                    capView.getLocationOnScreen(loc);
                    int sl = -loc[0];
                    int st = -loc[1];
                    int sr = sc[0] - loc[0];
                    int sb = sc[1] - loc[1];
                    if (gi[3] > 0) {
                        out.op(new android.graphics.Region(sl, (sb - gi[3]), sr, sb),
                                android.graphics.Region.Op.DIFFERENCE);
                        vg = (vg | 1);
                    }
                    if (gi[0] > 0) {
                        out.op(new android.graphics.Region(sl, st, (sl + gi[0]), sb),
                                android.graphics.Region.Op.DIFFERENCE);
                        vg = (vg | 2);
                    }
                    if (gi[2] > 0) {
                        out.op(new android.graphics.Region((sr - gi[2]), st, sr, sb),
                                android.graphics.Region.Op.DIFFERENCE);
                        vg = (vg | 4);
                    }
                }
            } catch (Throwable ignore) {
            }
            out.op(original, android.graphics.Region.Op.UNION);
            p9.args[0] = out;
            com.oupao.wiz.SLog.i("FlyClose: \u53ef\u89e6\u533a\u57df\u5df2\u6269\u5c55 w="
                    + cw + " h=" + ch + " \u8ba9\u51fa\u624b\u52bf\u533a=" + vg);
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.SLog.e("FlyClose: onCapRegion \u5f02\u5e38", __e_v0_1);
            return;
        }
        return;
    }
    static void onCapStyle(com.oupao.wiz.XMP p2)
    {
        try {
            com.oupao.wiz.Cap.hideTop(p2.thisObject);
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    static void onCapTap(com.oupao.wiz.XMP p5)
    {
        try {
            if (!com.oupao.wiz.Cfg.capClean()) return;
            var v1_1 = p5.args;
            if ((v1_1 == null) || (v1_1.length < 2)) return;
            var v1_2 = v1_1[1];
            if (((v1_2 instanceof Integer)) && (((Integer) v1_2).intValue() == 1)) {
                p5.setResult(false);
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    static void onCaptionCtor(com.oupao.wiz.XMP p3)
    {
        var v0_0 = p3.thisObject;
        com.oupao.wiz.Hooks.sCaption = v0_0;
        com.oupao.wiz.Cap.hideTop(v0_0);
        com.oupao.wiz.SLog.v("BarTap: \u8bb0\u5f55\u6700\u65b0\u5c0f\u7a97");
        return;
    }
    static void onCaptionRect(com.oupao.wiz.XMP p5)
    {
        java.lang.Object[] v0_0 = null;
        try {
            v0_0 = p5.args;
        } catch (Throwable __e_v0) {
            return;
        }
        if ((v0_0 != null) && (v0_0.length >= 2)) {
            var v1_4 = v0_0[0];
            com.oupao.wiz.Hooks.sTask = v1_4;
            var v2_9 = p5.thisObject;
            com.oupao.wiz.MiniUI.capKeep = v2_9;
            com.oupao.wiz.MiniUI.restore(v2_9, v1_4);
            android.graphics.Rect v1_1 = ((android.graphics.Rect) v0_0[1]);
            if ((v1_1 instanceof android.graphics.Rect)) {
                v1_1 = ((android.graphics.Rect) v1_1);
                com.oupao.wiz.Hooks.sRawRect = v1_1;
                var v2_2 = new StringBuilder();
                v2_2.append("BarTap: \u5c0f\u7a97\u77e9\u5f62 ");
                v2_2.append(v1_1.left);
                v2_2.append(",");
                v2_2.append(v1_1.top);
                v2_2.append(",");
                v2_2.append(v1_1.right);
                v2_2.append(",");
                v2_2.append(v1_1.bottom);
                com.oupao.wiz.SLog.i(v2_2.toString());
                var v4 = com.oupao.wiz.Hooks.viewRectNow();
                var v2_6 = new StringBuilder();
                v2_6.append("BarTap: \u53ef\u89c6\u77e9\u5f62 ");
                v2_6.append(v4);
                com.oupao.wiz.SLog.i(v2_6.toString());
            }
            com.oupao.wiz.CapR.put(p5.thisObject, v0_0[0], v1_1);
            var v0_1 = p5.thisObject;
            com.oupao.wiz.Hooks.sCaption = v0_1;
            com.oupao.wiz.Cap.hideTop(v0_1);
            com.oupao.wiz.SLog.v("BarTap: \u5c0f\u7a97\u77e9\u5f62\u5df2\u66f4\u65b0");
            // ColorOS17 自愈（1953）：开多任务会把迷你窗推出屏幕，此时之前扩到全屏的可触区
            // 会继续吃掉整屏触摸（屏幕点不动）。窗口矩形一变就主动让系统重算一次可触区。
            try {
                final Object cap = p5.thisObject;
                if ((cap != null)
                        && (com.oupao.wiz.Hooks.miniScale() || com.oupao.wiz.Hooks.mostlyOffScreen(cap))) {
                    com.oupao.wiz.SLog.i("FlyClose: \u7a97\u53e3\u53d8\u52a8(\u8ff7\u4f60/\u79bb\u5c4f) \u91cd\u7b97\u53ef\u89e6\u533a");
                    var h = com.oupao.wiz.Hooks.main();
                    if (h != null) {
                        h.post(new java.lang.Runnable() {
                            public void run() {
                                try {
                                    com.oupao.wiz.Rt.callNoArg(cap, "updateTouchableRegion");
                                } catch (Throwable ignore) {
                                }
                            }
                        });
                    }
                }
            } catch (Throwable ignore) {
            }
        }
        return;
    }
    static void onCaptionRelease(com.oupao.wiz.XMP p5)
    {
        com.oupao.wiz.MiniUI.clearMem();
        // 3255：小窗释放时打一份任务栈快照（诊断"黑底 / 图层残留"）
        try {
            com.oupao.wiz.Win.dumpTop("释放");
        } catch (Throwable ignore) {
        }
        var v0_0 = p5.thisObject;
        com.oupao.wiz.CapR.remove(v0_0);
        if (v0_0 == com.oupao.wiz.Hooks.sCaption) {
            com.oupao.wiz.Hooks.sCaption = null;
            com.oupao.wiz.Hooks.sTask = null;
            com.oupao.wiz.Hooks.sRawRect = null;
            com.oupao.wiz.Sug.curChat = null;
            com.oupao.wiz.Sug.curPkg = null;
            com.oupao.wiz.SLog.k("BarTap: \u666e\u901a\u5c0f\u7a97\u91ca\u653e, \u4e0d\u6062\u590d");
            com.oupao.wiz.Hooks.adoptNewest();
        } else {
            var v0_3 = p5.args;
            if ((v0_3 != null) && (v0_3.length > 0)) {
                var v0_1 = v0_3[0];
                if ((v0_1 != null) && (v0_1 == com.oupao.wiz.Hooks.sTask)) {
                    // 原版 Hooks.smali:3984-4004（:cond_1c）是两条路径共用的清空块，
                    // 移植时被反编译成空块 → 非当前 caption 释放时状态残留，
                    // 会让 Dock.isDocked 一直误判（放大 sameTask 那个 bug）。
                    com.oupao.wiz.Hooks.sCaption = null;
                    com.oupao.wiz.Hooks.sTask = null;
                    com.oupao.wiz.Hooks.sRawRect = null;
                    com.oupao.wiz.Sug.curChat = null;
                    com.oupao.wiz.Sug.curPkg = null;
                    com.oupao.wiz.SLog.k("BarTap: \u666e\u901a\u5c0f\u7a97\u91ca\u653e, \u4e0d\u6062\u590d");
                    com.oupao.wiz.Hooks.adoptNewest();
                }
            }
        }
        return;
    }
    static void onCaptionTouch(com.oupao.wiz.XMP p8)
    {
        java.lang.Object[] v0_3 = null;
        try {
            com.oupao.wiz.Rx.lazy(p8.thisObject);
            v0_3 = p8.args;
        } catch (Throwable __e_v0) {
            return;
        }
        if ((v0_3 != null) && (v0_3.length > 0)) {
            var v0_4 = v0_3[0];
            if ((v0_4 instanceof android.view.MotionEvent)) {
                var v0_1 = ((android.view.MotionEvent) v0_4);
                var v1_1 = v0_1.getActionMasked();
                if ((v1_1 == 1) || (v1_1 == 0)) {
                    var v2_7 = new StringBuilder();
                    v2_7.append("BarTap: \u89e6\u6478 a=");
                    v2_7.append(v1_1);
                    v2_7.append(" x=");
                    v2_7.append(((int) v0_1.getRawX()));
                    v2_7.append(" y=");
                    v2_7.append(((int) v0_1.getRawY()));
                    v2_7.append(" t=");
                    v2_7.append(v0_1.getEventTime());
                    com.oupao.wiz.SLog.v(v2_7.toString());
                } else {
                }
                if (v1_1 == 0) {
                    com.oupao.wiz.Hooks.sDownX = v0_1.getRawX();
                    com.oupao.wiz.Hooks.sDownY = v0_1.getRawY();
                    com.oupao.wiz.Hooks.sMoveDx = 0f;
                    com.oupao.wiz.Hooks.sMoveDy = 0f;
                    com.oupao.wiz.Hooks.sDownT = v0_1.getEventTime();
                    com.oupao.wiz.Hooks.sTapOK = true;
                    var v3_10 = p8.thisObject;
                    com.oupao.wiz.Tap.down(v3_10, v0_1.getRawX(), v0_1.getRawY());
                    com.oupao.wiz.MiniUI.arm(v3_10);
                    // 3188：记下按下点 + 当时矩形，拖动的位移从这儿算（arm 之后调用，
                    // 因为它才把 task/tid 填好）
                    com.oupao.wiz.MiniUI.dragDown(v0_1.getRawX(), v0_1.getRawY());
                    // ColorOS17 自愈（1953）：可触区已扩到全屏、窗口随后被推出屏幕时，
                    // 主动让系统重算一次，配合 onCapRegion 的"离屏不扩展"恢复正常触摸。
                    if (com.oupao.wiz.Hooks.mostlyOffScreen(v3_10)) {
                        com.oupao.wiz.SLog.i("FlyClose: \u7a97\u53e3\u79bb\u5c4f \u91cd\u7b97\u53ef\u89e6\u533a");
                        com.oupao.wiz.Rt.callNoArg(v3_10, "updateTouchableRegion");
                    }
                    return;
                } else {
                    // 1820fix: 反编译器把 smali 的三个并列分支
                    //   action==0 -> 记按下 / action==2 -> slop 检查 / action==1 -> UP 处理
                    // 错误地嵌成了 if (action==2) { if (action!=2) { action==1 -> ... } else { slop } }
                    // 恒假的 (action!=2) 让 ACTION_UP 整块不可达 —— 标题栏单击/双击因此完全没反应。
                    // 这里按 smali 还原成三分支。
                    if (v1_1 == 1) {
                        // 3185：**撤掉右滑兜底**。3183/3184 的坐标换算实测会让窗口往上跳
                        // （`shrinkTo` 的缩放原点和我假设的不一致），继续盲调只会越改越乱。
                        // 现在恢复到"左滑交给系统、右滑暂不支持"的稳定状态；
                        // 要修右滑必须先确认 applyMiniRect / shrinkTo 的确切口径，再动。
                        // ACTION_UP
                        // 3188：把松手那一刻的位置也记进预测（最后一小段 MOVE 常常没有）
                        // 一点点滑动模式：UP 时补算总位移，保证小位移也能判到方向。
                        if (com.oupao.wiz.Cfg.flyMiniEasy()) {
                            com.oupao.wiz.Hooks.sMoveDx = v0_1.getRawX() - com.oupao.wiz.Hooks.sDownX;
                            com.oupao.wiz.Hooks.sMoveDy = v0_1.getRawY() - com.oupao.wiz.Hooks.sDownY;
                        }
                        com.oupao.wiz.MiniUI.dragMove(v0_1.getRawX(), v0_1.getRawY());
                        com.oupao.wiz.Tap.cancel();
                        com.oupao.wiz.MiniUI.abort();
                        if ((!com.oupao.wiz.Hooks.sTapOK) || ((v0_1.getEventTime() - com.oupao.wiz.Hooks.sDownT) > 400)) {
                            return;
                        } else {
                            var v2_3 = v0_1.getRawX();
                            var v3_0 = v0_1.getRawY();
                            var v4_2 = v0_1.getEventTime();
                            var v1_0 = p8.thisObject;
                            var v6_1 = v0_1.getX();
                            var v7 = v0_1.getY();
                            com.oupao.wiz.Hooks.sLocX = v6_1;
                            com.oupao.wiz.Hooks.sLocY = v7;
                            com.oupao.wiz.Hooks.onOutsideTap(v1_0, v2_3, v3_0, v4_2);
                            if (!com.oupao.wiz.Hooks.sOutside) {
                                com.oupao.wiz.Hooks.handleTap(v1_0, v2_3, v3_0, v4_2);
                            }
                            return;
                        }
                    } else {
                        if (v1_1 == 2) {
                            // 3172 诊断：确认"左右滑动小白条"的 MOVE 事件到底有没有到我们这里。
                            // 有 → 说明事件没被吞，问题在更下游（ROM 的手势判定被我们改了）；
                            // 没有 → 说明可触区/窗口几何被我们改坏了。
                            try {
                                float ddx = v0_1.getRawX() - com.oupao.wiz.Hooks.sDownX;
                                float ddy = v0_1.getRawY() - com.oupao.wiz.Hooks.sDownY;
                                com.oupao.wiz.Hooks.sMoveDx = ddx;
                                com.oupao.wiz.Hooks.sMoveDy = ddy;
                                // 3188：窗口跟手指 1:1 走 → 用它推"当前可见矩形"，
                                // 松手后立刻长按也不会先画在旧位置
                                com.oupao.wiz.MiniUI.dragMove(v0_1.getRawX(), v0_1.getRawY());
                                if (Math.abs(ddx) > 60 && Math.abs(ddx) > Math.abs(ddy)) {
                                    com.oupao.wiz.SLog.r("r-cap-move",
                                            "BarTap: 横向滑动 dx=" + ((int) ddx) + " dy=" + ((int) ddy), 800L);
                                }
                            } catch (Throwable ignore) {
                            }
                            // ACTION_POINTER_DOWN/UP 之外的 slop 检查分支
                            if (Math.abs((v0_1.getRawX() - com.oupao.wiz.Hooks.sDownX)) <= ((float) 40)) {
                                if (Math.abs((v0_1.getRawY() - com.oupao.wiz.Hooks.sDownY)) <= ((float) 40)) {
                                    return;
                                } else {
                                    com.oupao.wiz.Tap.cancel();
                                    com.oupao.wiz.MiniUI.abort();
                                    com.oupao.wiz.Hooks.sTapOK = false;
                                    return;
                                }
                            } else {
                                com.oupao.wiz.Tap.cancel();
                                com.oupao.wiz.MiniUI.abort();
                                com.oupao.wiz.Hooks.sTapOK = false;
                                return;
                            }
                        } else {
                            com.oupao.wiz.Tap.cancel();
                            com.oupao.wiz.MiniUI.abort();
                            com.oupao.wiz.Hooks.sTapOK = false;
                        }
                    }
                }
            }
        }
        return;
    }
    static void onCornerAlpha(com.oupao.wiz.XMP p3)
    {
        try {
            if (com.oupao.wiz.Cfg.cornerOff()) {
                var v1 = p3.args;
                if ((v1 != null) && (v1.length > 0)) {
                    v1[0] = Float.valueOf(0);
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    static void onCornerFlag(com.oupao.wiz.XMP p4)
    {
        try {
            if (com.oupao.wiz.Cfg.cornerOff()) {
                var v0_1 = p4.result;
                if ((v0_1 instanceof Integer)) {
                    var v1_3 = ((Integer) v0_1).intValue();
                    if ((v1_3 == 4) || (v1_3 == 6)) {
                        p4.setResult(Integer.valueOf(0));
                    }
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    static void onCornerSetup(com.oupao.wiz.XMP p2)
    {
        java.lang.Object[] v0_0 = null;
        try {
            v0_0 = p2.args;
        } catch (Throwable __e_v0) {
            return;
        }
        if ((v0_0 != null) && (v0_0.length > 0)) {
            com.oupao.wiz.Cap.cornerOff(v0_0[0]);
        }
        return;
    }
    static boolean onDoubleTapBar(Object p4, float p5, float p6)
    {
        com.oupao.wiz.Hooks.focus(p4);
        var v0_2 = com.oupao.wiz.Hooks.viewRectNow();
        return (v0_2 != null)
                && com.oupao.wiz.Hooks.inStrip(com.oupao.wiz.Hooks.sLastX, com.oupao.wiz.Hooks.sLastY, v0_2)
                && com.oupao.wiz.Hooks.inStrip(p5, p6, v0_2);
    }
    static void onGestureMode(com.oupao.wiz.XMP p4)
    {
        java.lang.StringBuilder v0_1 = null;
        java.lang.Object[] v1_0 = null;
        try {
            v0_1 = new StringBuilder();
            v0_1.append("FlyMini: res=");
            v0_1.append(String.valueOf(p4.result));
            v0_1.append(" a1=");
            v1_0 = p4.args;
        } catch (Throwable __e_v0) {
            return;
        }
        if ((v1_0 == null) || (v1_0.length < 2)) {
            v0_1.append("?");
        } else {
            v0_1.append(String.valueOf(v1_0[1]));
        }
        com.oupao.wiz.SLog.i(v0_1.toString());
        boolean outer = (v1_0 != null) && (v1_0.length >= 2) && !(v1_0[1] instanceof Integer);
        // ★ 2814：回到 2800 的干净逻辑，只多加一条「键盘弹起时上滑的手势码是 3」。
        //   mode 0（无键盘上滑）/ mode 3（键盘弹起时上滑）→ 4 = 迷你窗。
        //   之前几版加的东西（吞手势重放 / ToFloat 策略）全部撤掉。
        if (com.oupao.wiz.Cfg.flyMini()) {
            var v0_4 = p4.result;
            if (v0_4 instanceof Integer) {
                int mode = ((Integer) v0_4).intValue();
                // ★★ 3078（3077 实测结论，重要）：
                //   3077 把这整段（吞手势 + 调 ROM 的 maySwitchStateToMini）删掉后，**键盘在时上滑彻底没反应** ——
                //   因为键盘弹起时 ROM 对 `mode 4`（迷你窗）是**空操作**，而原生 mode 0 那条又被我们改成了 4，
                //   于是"既不关小窗、也不转迷你"。
                //   ⇒ 「键盘在时转迷你」只能靠 ROM 自己的入口 `maySwitchStateToMini`，而它**内部会先收输入法**
                //     （所以"上滑把键盘收了"其实是那次 ROM 调用的副作用，不是我们直接去收键盘）。
                //   两者在 ROM 层是绑死的：不调它 → 没有迷你窗；调它 → 键盘一定会收。
                boolean ime = com.oupao.wiz.Hooks.imeShown();
                if (ime && ((mode == 0) || (mode == 3))) {
                    // ★★ 3080：**立刻**转迷你，不再等 420ms 兜底（3079 实测：守卫路线没生效，
                    //   420ms 后才退回 ROM 入口，再连试到 step5 ≈ 1.7s 才变迷你 → 手感很拖）。
                    //   现在：① 放行 IME 守卫（顺便让 ROM 自己的管线有 chance）；
                    //        ② 手势结果按迷你窗(4)交回 ROM；
                    //        ③ **同步立刻**调 `maySwitchStateToMini` 并快重试（0→560ms，7 次）。
                    com.oupao.wiz.Hooks.imeMiniUntil = android.os.SystemClock.uptimeMillis() + 900L;
                    p4.setResult(Integer.valueOf(4));
                    com.oupao.wiz.SLog.i("FlyMini: 键盘在 → 立刻转迷你(放行守卫 + 快重试) 原 mode=" + mode);
                    com.oupao.wiz.Hooks.miniRetry(0);
                    com.oupao.wiz.MiniUI.buzz();
                    return;
                }
                // ★★ 3186：**根因**。
                //   这里原来无条件把 mode 0 / mode 3 改写成 4（迷你窗），完全没看方向。
                //   日志实测：左右滑小白条时 ROM 给的就是 `res=3 a1=10`（横向手势），
                //   于是每一次横滑都被改写成了"上滑转迷你窗" ——
                //   左滑只是侥幸走了别的分支，右滑彻底没反应/变成拖动窗口。
                //   现在要求**真实向上**才改写（横向一律不动）。
                float mdy = com.oupao.wiz.Hooks.sMoveDy;
                float mdx = com.oupao.wiz.Hooks.sMoveDx;
                boolean isUp = (mdy < 0f) && (Math.abs(mdy) > Math.abs(mdx) * 1.2f);
                boolean easy = com.oupao.wiz.Cfg.flyMiniEasy();
                if (easy) {
                    // 一点点滑动：小位移也转迷你；外层被内层改成 4 后可能返回 -1/6，
                    // 这里用真实位移兜底（不改快速上滑的旧逻辑）。
                    if (outer && ((mode == -1) || (mode == 6)) && isUp) {
                        p4.setResult(Integer.valueOf(4));
                        com.oupao.wiz.SLog.i("FlyMini: 一点点滑动外层兜底(dy=" + ((int) mdy)
                                + " dx=" + ((int) mdx) + ") → 迷你窗");
                        return;
                    }
                    if (isUp && ((mode == 0) || (mode == 3))) {
                        p4.setResult(Integer.valueOf(4));
                        com.oupao.wiz.SLog.i("FlyMini: 一点点滑动(dy=" + ((int) mdy)
                                + " dx=" + ((int) mdx) + ") → 迷你窗");
                    }
                } else {
                    // 快速上滑：完整保留 3383 旧逻辑，不加速度/外层兜底。
                    // 兜底：完全没跟踪到位移（极少见）时，只对原来的 0 号模式保留旧行为，
                    // 避免"上滑转迷你窗"整体失效；横滑一定会产生 MOVE，不会落到这个分支。
                    boolean unknownUp = (mdy == 0f && mdx == 0f && mode == 0);
                    if ((isUp || unknownUp) && ((mode == 0) || (mode == 3))) {
                        p4.setResult(Integer.valueOf(4));
                        com.oupao.wiz.SLog.i("FlyMini: 快速上滑(dy=" + ((int) mdy)
                                + " dx=" + ((int) mdx) + ") 重映射为迷你窗(原 mode=" + mode + ")");
                    }
                }
            }
        }
        // 3389：真正落到迷你窗结果时震一下（内层/外层可能都调用，buzz 内部 250ms 去重）
        if (outer && (p4.result instanceof Integer)
                && (((Integer) p4.result).intValue() == 4)) {
            com.oupao.wiz.MiniUI.buzz();
        }
        return;
    }

    static void onMenuShowing(com.oupao.wiz.XMP p3)
    {
        try {
            if (com.oupao.wiz.Cfg.capClean()) {
                var v1 = p3.args;
                if ((v1 != null) && (v1.length > 0)) {
                    var v0_1 = false;
                    v1[0] = v0_1;
                    p3.setResult(v0_1);
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    static void onMiniAnim(com.oupao.wiz.XMP p8)
    {
        try {
            if ((com.oupao.wiz.Cfg.miniBig()) && ((com.oupao.wiz.MiniUI.memW != 0) && (com.oupao.wiz.MiniUI.memTid != 0))) {
                var v1_1 = p8.args;
                if ((v1_1 != null) && (v1_1.length >= 6)) {
                    var v2_0 = v1_1[1];
                    if ((v2_0 instanceof android.graphics.Rect)) {
                        var v4 = v1_1[2];
                        if (v4 != null) {
                            var v2_1 = ((android.graphics.Rect) v2_0);
                            if ((v2_1.width() < (com.oupao.wiz.MiniUI.memW - 16)) && ((com.oupao.wiz.Win.taskId(v4) == com.oupao.wiz.MiniUI.memTid) && (com.oupao.wiz.MiniUI.fit(v2_1, v4)))) {
                                v1_1[1] = com.oupao.wiz.MiniUI.trR;
                                v1_1[3] = Float.valueOf(com.oupao.wiz.MiniUI.trS);
                                com.oupao.wiz.MiniUI.lg2("MiniUI: tr anim ", v2_1.width(), com.oupao.wiz.MiniUI.trR.width());
                            }
                        }
                    }
                }
            }
        } catch (Throwable __e_v0_2) {
            com.oupao.wiz.SLog.e("MiniAnim: onMiniAnim EX", __e_v0_2);
            return;
        }
        return;
    }
    static void onMiniInfo(com.oupao.wiz.XMP p8)
    {
        try {
            if ((com.oupao.wiz.Cfg.miniBig()) && ((com.oupao.wiz.MiniUI.memW != 0) && (com.oupao.wiz.MiniUI.memTid != 0))) {
                var v1_1 = p8.args;
                if ((v1_1 != null) && (v1_1.length >= 5)) {
                    var v2_0 = v1_1[0];
                    if (v2_0 != null) {
                        var v3_1 = v1_1[1];
                        if (((v3_1 instanceof Integer)) && (((Integer) v3_1).intValue() == 2)) {
                            var v3_4 = v1_1[3];
                            if ((v3_4 instanceof android.graphics.Rect)) {
                                var v3_5 = ((android.graphics.Rect) v3_4);
                                if ((v3_5.width() < (com.oupao.wiz.MiniUI.memW - 16)) && ((com.oupao.wiz.Win.taskId(v2_0) == com.oupao.wiz.MiniUI.memTid) && (com.oupao.wiz.MiniUI.fit(v3_5, v2_0)))) {
                                    v1_1[3] = com.oupao.wiz.MiniUI.trR;
                                    v1_1[4] = Float.valueOf(com.oupao.wiz.MiniUI.trS);
                                    com.oupao.wiz.MiniUI.lg2("MiniUI: tr info ", v3_5.width(), com.oupao.wiz.MiniUI.trR.width());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable __e_v0_3) {
            com.oupao.wiz.SLog.e("MiniAnim: onMiniInfo EX", __e_v0_3);
            return;
        }
        return;
    }
    /**
     * 通知被取消（cancelNotificationWithTag）：微信/QQ 进聊天页会 cancel 自己那条通知，
     * 系统通知也是这么消失的 → 我们跟着把气泡收掉（点通知/桌面/应用内导航都覆盖）。
     * 防误杀：只在气泡已经显示超过 1.2 秒时才收（刚弹出就更新/重发的通知不动）。
     */
    public static void onNotifCancel(com.oupao.wiz.XMP p6)
    {
        try {
            Object[] a = p6.args;
            if (a == null) {
                return;
            }
            String cur = com.oupao.wiz.Sug.pkg;
            if (cur == null) {
                return;
            }
            boolean hit = false;
            for (Object o : a) {
                if (o instanceof String && cur.equals((String) o)) {
                    hit = true;
                    break;
                }
            }
            long age = android.os.SystemClock.uptimeMillis() - com.oupao.wiz.Sug.showStamp;
            if (hit) {
                com.oupao.wiz.SLog.i("NotifCancel: 命中 pkg=" + cur + " age=" + age);
                // ★常驻语义：只有用户**真的进了那个应用**才收，后台被取消的通知不算
                // 3192：还要比用户 —— 主应用和分身同包名，只看包名会把对方的气泡收掉
                String fg = com.oupao.wiz.Sug.fgPkg();
                boolean inApp = (fg != null) && fg.equals(cur)
                        && (com.oupao.wiz.Hooks.focusedUserId() == com.oupao.wiz.Sug.bubbleUser);
                if (!inApp) {
                    com.oupao.wiz.SLog.i("NotifCancel: 不在前台 fg=" + fg + " cur=" + cur
                            + " fu=" + com.oupao.wiz.Hooks.focusedUserId()
                            + " bu=" + com.oupao.wiz.Sug.bubbleUser + " → 气泡保留");
                    return;
                }
                if (age > 1200L) {
                    // 微信/QQ 发新消息时会先批量 cancel 旧通知（汇总）。这里不能立刻收，
                    // 否则每次来消息都会「拆窗口 -> 重建」。改成延时收：期间有新消息就撤销。
                    com.oupao.wiz.SLog.i("NotifCancel: 通知被取消 -> 延时收气泡 " + cur);
                    com.oupao.wiz.Bub.hideSoon(3000L);
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
    }

    public static void onNotify(com.oupao.wiz.XMP p6)
    {
        java.lang.Object[] v0 = null;
        try {
            v0 = p6.args;
        } catch (Throwable __e_v0) {
            return;
        }
        if ((v0 != null) && (v0.length >= 7)) {
            var v1_2 = v0[0];
            if ((v1_2 instanceof String)) {
                var v1_3 = ((String) v1_2);
                // 原来这里只放行微信/QQ，其它包直接 return —— 于是 Sug.onNotify 里
                // 「其它应用走 Cfg.sugMsg()」那一支永远走不到，「左下角气泡」被写死成
                // 只有微信/QQ 才有。现在放开：
                //   微信/QQ  → 各自开关（sugWx / sugQq），行为不变；
                //   其它应用 → 看「其他应用消息提醒」(oupao_sug_msg)，默认开 = 全局气泡。
                com.oupao.wiz.SLog.i("NotifHook: enter pkg=" + v1_3 + " len=" + v0.length
                        + " bubbleOn=" + com.oupao.wiz.Cfg.bubbleAppOn(v1_3));
                boolean v1_4 = ("com.tencent.mm".equals(v1_3)) || ("com.tencent.mobileqq".equals(v1_3));
                var v2_5 = v0[6];
                if ((v2_5 instanceof android.app.Notification)) {
                    int v3_3 = 0;
                    var v2_6 = ((android.app.Notification) v2_5);
                    // 「屏蔽悬浮通知」对**任何**应用都生效（总开关 + 白名单），
                    // 必须放在下面「气泡」的早退之前。
                    com.oupao.wiz.Hooks.maybeNoHeadsUp(v1_3, v2_6);
                    // 气泡同样改成「总开关 + 白名单」，微信/QQ 不再特殊对待
                    if (!com.oupao.wiz.Cfg.bubbleAppOn(v1_3)) {
                        com.oupao.wiz.SLog.k("NotifHook: bubbleAppOn=false, 不弹气泡 pkg=" + v1_3);
                        return;
                    }
                    var v3_2 = v0[2];
                    if (!(v3_2 instanceof Integer)) {
                        v3_3 = 0;
                    } else {
                        v3_3 = ((Integer) v3_2).intValue();
                    }
                    int v4_3 = 0;
                    var v4_2 = v0[7];
                    if (!(v4_2 instanceof Integer)) {
                        v4_3 = 0;
                    } else {
                        v4_3 = ((Integer) v4_2).intValue();
                    }
                    com.oupao.wiz.Sug.onNotify(v1_3, v2_6, v3_3, v4_3);
                }
            }
        }
        return;
    }
    static void onOutsideTap(Object p10, float p11, float p12, long p13)
    {
        int v0_3 = 0;
        try {
            com.oupao.wiz.Hooks.sOutside = false;
            v0_3 = com.oupao.wiz.Cfg.flyClose();
        } catch (Throwable __e_v0) {
            return;
        }
        if ((v0_3 != 0) && (p10 != null)) {
            // 3245：窗口自己还在过渡动画里（ROM 的 shouldIgnoreTouchEvent 这时也会忽略触摸）
            //   → 这次"窗外点击"不关窗。实测快速"开小窗→点窗外关→再开"时，
            //   关闭正好落在 TRANSIT_FULL_SWITCH_TO_DEFAULT 中途，会把 ROM 的窗口状态机卡住，
            //   下一次开窗就没有动画、甚至只画出半个窗口。
            try {
                int ft = com.oupao.wiz.Win.flexTransition(com.oupao.wiz.Win.capTask(p10));
                if (ft != 0) {
                    com.oupao.wiz.SLog.i("FlyClose: 窗口过渡中(0x" + Integer.toHexString(ft)
                            + ") -> 本次不关");
                    return;
                }
            } catch (Throwable ignore) {
            }
            // 3248：扇形菜单展开中 / 刚收起 400ms 内 → 这一下不是"窗外点击"。
            //   选中应用的那一下 UP 会同时落进小窗的扩展可触区，原来会被当成窗外单击，
            //   于是"开一个应用 = 开窗 + 立刻关窗"演两遍动画（用户体感：卡顿、动画丢失、前台应用被卡退）。
            try {
                long since = android.os.SystemClock.uptimeMillis() - com.oupao.wiz.FlyFanH.hideAt;
                if (com.oupao.wiz.FlyState.open() || (since < 400L)) {
                    com.oupao.wiz.SLog.i("FlyClose: 扇形菜单展开/刚收起(" + since
                            + "ms) → 不算窗外，不关");
                    return;
                }
            } catch (Throwable ignore) {
            }
            // 窗口已被推出屏幕（ColorOS17 开多任务）：既不该关窗，也不该继续吃掉触摸
            if (com.oupao.wiz.Hooks.mostlyOffScreen(p10)) {
                // ColorOS17 自愈（1953）：离屏既不关窗，也要把可触区收回去，否则整屏点不动
                com.oupao.wiz.SLog.i("FlyClose: 窗口离屏 不关 + 重算可触区");
                com.oupao.wiz.Rt.callNoArg(p10, "updateTouchableRegion");
                return;
            }
            // 缩放迷你窗不该被"窗外点击"关掉（原来这段判据写成 scale<=0 && scale>=0.35，永远进不来）
            if (com.oupao.wiz.Hooks.miniScale()) {
                com.oupao.wiz.SLog.i("FlyClose: \u8ff7\u4f60\u7a97(\u7f29\u653e) \u4e0d\u5173");
                return;
            }
            if (com.oupao.wiz.Rt.callNoArg(p10, "isMiniZoomState") != Boolean.TRUE) {
                var v2_1 = ((android.view.View) p10).getWidth();
                var v3_3 = ((android.view.View) p10).getHeight();
                com.oupao.wiz.Hooks.sCapW = v2_1;
                com.oupao.wiz.Hooks.sCapH = v3_3;
                if ((v2_1 > 0) && (v3_3 > 0)) {
                    // 窗内外判定回到 3466 之前的口径：caption 的屏幕矩形 + caption 本地坐标双检。
                    //   ① 屏幕矩形取不到 / 不含触点 → ② 再看本地坐标是否落在 [0,w)×[0,h)。
                    //   两条都成立才算「窗外」，任一条说明在窗内就不关。
                    var vb = com.oupao.wiz.Hooks.viewRectNowFor(p10);
                    if ((vb == null) || (!vb.contains((int) p11, (int) p12))) {
                        var v4_1 = com.oupao.wiz.Hooks.sLocX;
                        var v5_4 = com.oupao.wiz.Hooks.sLocY;
                        if ((v4_1 < 0) || ((v5_4 < 0) || ((v4_1 >= ((float) v2_1)) || (v5_4 >= ((float) v3_3))))) {
                            com.oupao.wiz.Hooks.sOutside = true;
                            // 单击关闭（flyClose==1）：官方是"命中窗外立刻关闭"，此处原先整段丢失
                            if (v0_3 == 1) {
                                com.oupao.wiz.Hooks.sOutT = 0;
                                com.oupao.wiz.SLog.i("FlyClose: 窗外单击 -> 关闭小窗 x=" + ((int) p11)                                        + " y=" + ((int) p12)
                                        + " w=" + com.oupao.wiz.Hooks.sCapW
                                        + " h=" + com.oupao.wiz.Hooks.sCapH);
                                com.oupao.wiz.Hooks.closeFlexibleFor(p10);
                                return;
                            }
                            if (v0_3 != 1) {
                                var v4_2 = com.oupao.wiz.Hooks.sOutT;
                                var v8_1 = new StringBuilder();
                                v8_1.append("Dbl: now=");
                                v8_1.append(((int) p13));
                                v8_1.append(" last=");
                                v8_1.append(v4_2);
                                com.oupao.wiz.SLog.i(v8_1.toString());
                                if (v4_2 > 0) {
                                    if ((((int) p13) - v4_2) <= 500) {
                                        com.oupao.wiz.Hooks.sOutT = 0;
                                        com.oupao.wiz.Hooks.sOutT = 0;
                                        var v1_22 = new StringBuilder();
                                        v1_22.append("FlyClose: \u7a97\u5916\u70b9\u51fb -> \u5173\u95ed\u5c0f\u7a97 x=");
                                        v1_22.append(((int) p11));
                                        v1_22.append(" y=");
                                        v1_22.append(((int) p12));
                                        v1_22.append(" w=");
                                        v1_22.append(com.oupao.wiz.Hooks.sCapW);
                                        v1_22.append(" h=");
                                        v1_22.append(com.oupao.wiz.Hooks.sCapH);
                                        com.oupao.wiz.SLog.i(v1_22.toString());
                                        com.oupao.wiz.Hooks.closeFlexibleFor(p10);
                                        return;
                                    } else {
                                        com.oupao.wiz.Hooks.sOutT = ((int) p13);
                                    }
                                } else {
                                    com.oupao.wiz.Hooks.sOutT = ((int) p13);
                                }
                                var v0_1 = new StringBuilder();
                                v0_1.append("FlyClose: \u7a97\u5916\u7b2c\u4e00\u6b21\u70b9\u51fb x=");
                                v0_1.append(((int) p11));
                                v0_1.append(" y=");
                                v0_1.append(((int) p12));
                                v0_1.append(" loc=");
                                v0_1.append(((int) com.oupao.wiz.Hooks.sLocX));
                                v0_1.append(",");
                                v0_1.append(((int) com.oupao.wiz.Hooks.sLocY));
                                com.oupao.wiz.SLog.i(v0_1.toString());
                                return;
                            }
                        } else {
                            com.oupao.wiz.SLog.i("FlyClose: \u7a97\u5185(\u672c\u5730) \u4e0d\u5173");
                        }
                    } else {
                        com.oupao.wiz.SLog.i("FlyClose: \u7a97\u5185(\u77e9\u5f62) \u4e0d\u5173");
                    }
                }
            } else {
                com.oupao.wiz.SLog.i("FlyClose: \u8ff7\u4f60\u7a97(\u72b6\u6001) \u4e0d\u5173");
            }
        }
        return;
    }
    public static void onPeek(com.oupao.wiz.XMP p11)
    {
        try {
            if (p11 != null) {
                var v0 = p11.args;
                if ((v0 != null) && (v0.length > 0)) {
                    var v1_2 = v0[0];
                    if (v1_2 != null) {
                        var v2 = com.oupao.wiz.Hooks.pkgOfEntry(v1_2);
                        // 移植 bug 修复：原来是 `(!isWx) || (isQQ)` —— 对微信恒为 false，
                        // 等于「微信从来不走 peek 过滤器的拦截」，只有 QQ/其它应用会走。
                        // 现在统一用 nohuKey()：微信/QQ 各自开关，其余走全局 oupao_nohu_all，
                        // 与 Hooks$HU、onSimpleBanner 完全一致。
                        // ★ 混合判定：开关读得到就以开关为准，读不到退回标记（详见 nohuShouldBlock）
                        if ((v2 != null)
                                && (com.oupao.wiz.Hooks.nohuShouldBlock(v1_2, v2))) {
                            Object[] v4_1 = new Object[0];
                            var v3_6 = com.oupao.wiz.Rt.callLoose(v1_2, "getSbn", v4_1);
                            if (v3_6 != null) {
                                Object[] v5_1 = new Object[0];
                                var v3_7 = com.oupao.wiz.Rt.callLoose(v3_6, "getNotification", v5_1);
                                if ((v3_7 instanceof android.app.Notification)) {
                                    var v3_8 = ((android.app.Notification) v3_7);
                                    var v4_4 = v3_8.category;
                                    if (((v4_4 == null) || (!"call".equals(v4_4))) && (v3_8.fullScreenIntent == null)) {
                                        p11.setResult(true);
                                        var v3_12 = new StringBuilder();
                                        v3_12.append("HU2: peek blocked ");
                                        v3_12.append(v2);
                                        com.oupao.wiz.SLog.i(v3_12.toString());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    /**
     * 装「简洁横幅」拦截钩子。
     *
     * ColorOS 16 的横幅有两种样式：普通横幅走 HeadsUpManagerImpl（已被
     * [hookHeadsUp] 拦掉），而**简洁横幅**是 OPlus 自己的一套
     * FullScreenBanner（日志里叫 "Simple Banner Window"），完全不走那条路，
     * 所以只开「屏蔽悬浮通知」时简洁横幅仍会弹出来。
     *
     * 判定入口是 `FullScreenBannerHelper#isSimpleBannerEnable(NotificationEntry, Bundle)`：
     * `OplusHeadsUpCoordinator` 在决定「走简单横幅还是普通抬头」时就是看它的返回值。
     * 这里对已打标记（`oupao_nohu`，由 system_server 的 maybeNoHeadsUp 写入，
     * 只有微信/QQ 且开关打开才会打）的通知返回 false → 简洁横幅不再出现。
     */
    public static void hookSimpleBanner(ClassLoader p5)
    {
        try {
            var c = com.oupao.wiz.Rt.cls(p5, "com.oplus.systemui.notification.interruption.fullscreenbanner.FullScreenBannerHelper");
            if (c == null) {
                com.oupao.wiz.SLog.i("SB: 未找到 FullScreenBannerHelper");
                return;
            }
            var m = com.oupao.wiz.Rt.methodByName(c, "isSimpleBannerEnable", 2);
            if (m == null) {
                com.oupao.wiz.SLog.i("SB: 未找到 isSimpleBannerEnable");
                return;
            }
            if (com.oupao.wiz.XBridge.hookMethod(m, new com.oupao.wiz.Hooks$SB()) != null) {
                com.oupao.wiz.SLog.i("SB: 简洁横幅钩子已挂");
            } else {
                com.oupao.wiz.SLog.i("SB: 简洁横幅钩子安装失败");
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("SB: hook 异常", t);
        }
    }

    /** isSimpleBannerEnable 的 before 回调：微信/QQ 已打标记时返回 false */
    public static void onSimpleBanner(com.oupao.wiz.XMP p6)
    {
        try {
            var v0 = p6.args;
            if ((v0 == null) || (v0.length < 1)) {
                return;
            }
            var v1 = v0[0];
            if (v1 == null) {
                return;
            }
            var v2 = com.oupao.wiz.Hooks.pkgOfEntry(v1);
            if (v2 == null) {
                return;
            }
            // 诊断：命中了就会打一条（1s 限流），用来确认「简洁横幅判定确实走到这里」。
            // 只在通知到达时触发，频率很低，留作常备排查线索。
            com.oupao.wiz.SLog.r("SBask", "SB: ask " + v2, 1000L);
            // 与 Hooks$HU / onPeek 用**同一套判据**：nohuKey() 决定看哪个开关
            // （微信/QQ 各自，其余走全局 oupao_nohu_all），decide() 合并标记与配置。
            var v2k = com.oupao.wiz.Hooks.nohuKey(v2);
            // ★ 混合判定（同上）
            if (!com.oupao.wiz.Hooks.nohuShouldBlock(v1, v2)) {
                return;
            }
            // 来电 / 全屏 intent 不拦（与 onPeek 的豁免一致）
            Object[] v3_0 = new Object[0];
            var v3_1 = com.oupao.wiz.Rt.callLoose(v1, "getSbn", v3_0);
            if (v3_1 != null) {
                Object[] v4_0 = new Object[0];
                var v3_2 = com.oupao.wiz.Rt.callLoose(v3_1, "getNotification", v4_0);
                if ((v3_2 instanceof android.app.Notification)) {
                    var v3_3 = ((android.app.Notification) v3_2);
                    var v4_1 = v3_3.category;
                    if ((("call".equals(v4_1))) || (v3_3.fullScreenIntent != null)) {
                        return;
                    }
                }
            }
            p6.setResult(Boolean.FALSE);
            com.oupao.wiz.SLog.i("SB: 简洁横幅已拦 " + v2);
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    /** 2817：最近任务（多任务）界面活动时间戳 —— 给「多任务里不呼出引索条」用 */
    private static long recentsAt;

    static void noteRecents()
    {
        recentsAt = android.os.SystemClock.uptimeMillis();
    }

    /**
     * 3436：距离上次 `toggleRecentApps` 过了多久（ms），没见过返回 Long.MAX_VALUE。
     *
     * 扇形菜单"停留中自动收起"用它做**一手信号**：桌面写的 `oupao_recents_visible`
     * 在这台 C17 上要过 ~10s 才刷新（实测 17:32:49 开多任务、17:33:00 标志位才变 1），
     * 而 `toggleRecentApps` 是 system_server 自己调的，立刻就能看到。
     */
    public static long recentsAgeMs()
    {
        if (recentsAt <= 0L) {
            return Long.MAX_VALUE;
        }
        return android.os.SystemClock.uptimeMillis() - recentsAt;
    }

    /** 3455：上次 `toggleRecentApps` 的时刻（uptimeMillis，没见过返回 0）。用于"这次手势之后才算"的判定。 */
    public static long recentsAtMs()
    {
        return recentsAt;
    }

        /** 2834：400ms 缓存——这个函数在**触摸路径**上（Idx.open → inRecentsWindow），
     *  以前每次都要走一趟 Settings 读取 + 文件回退，实测出现过 `Idx: feed slow 302ms`。
     *  概览状态变化是秒级的，400ms 缓存完全够。 */
    private static volatile long ovCacheAt;
    private static volatile boolean ovCacheVal;

    public static boolean inRecentsWindow()
    {
        long nowMs = android.os.SystemClock.uptimeMillis();
        if ((nowMs - ovCacheAt) < 400L) {
            return ovCacheVal;
        }
        boolean r = inRecentsWindowRaw();
        ovCacheAt = nowMs;
        ovCacheVal = r;
        return r;
    }

    private static boolean inRecentsWindowRaw()
    {
        // 2842：桌面自己的 onStateSetEnd 会把 oupao_recents_visible 写成 "0"。
        //   以前只有 "1" 会短路，其余一律落到「recentsAt 8 秒内算在多任务」的兜底，
        //   于是**从多任务回到桌面后要等好几秒引索条才肯呼出**（用户实测）。
        //   现在 "0" 也短路（明确不在多任务）；兜底只留给「读不到值」的情况。
        try {
            android.content.Context c0 = com.oupao.wiz.Cfg.sysContext();
            if (c0 != null) {
                String v0 = com.oupao.wiz.Cfg.ggetC(c0, "oupao_recents_visible");
                if ("0".equals(v0)) {
                    return false;
                }
            }
        } catch (Throwable ignore) {
        }
        // 桌面进程（FlyGate）把"是否在多任务概览"写进 Settings.Global
        try {
            android.content.Context c = com.oupao.wiz.Cfg.sysContext();
            if (c != null) {
                String v = com.oupao.wiz.Cfg.ggetC(c, "oupao_recents_visible");
                if ("1".equals(v)) {
                    // 2829：留一行实证（限流 3s），确认 system_server 侧真的读到了桌面写的值
                    long now = android.os.SystemClock.uptimeMillis();
                    if (now - ovLogAt > 3000L) {
                        ovLogAt = now;
                        com.oupao.wiz.SLog.i("FlyRecents: 判定在多任务界面(oupao_recents_visible=1)");
                    }
                    return true;
                }
            }
        } catch (Throwable ignore) {
        }
        return (android.os.SystemClock.uptimeMillis() - recentsAt) < 8000L;
    }

    private static long ovLogAt;

    static void onToggleRecents(com.oupao.wiz.XMP p)
    {
        try {
            com.oupao.wiz.Hooks.noteRecents();
            com.oupao.wiz.SLog.i("FlyRecents: toggleRecentApps");
        } catch (Throwable ignore) {
        }
    }

    /** 2821：装 `toggleRecentApps` 钩子（ATMS/AMS 里都可能有，挨个试） */
    static void installRecentsHook(ClassLoader p6)
    {
        String[] cls = new String[]{
                // 3436：桌面/SystemUI 走的是 IStatusBarService.toggleRecentApps → 这个服务最稳；
                //   实测这台 C17 的 ATMS 没有 toggleRecentApps（日志里从来没出现过挂钩成功）。
                "com.android.server.statusbar.StatusBarManagerService",
                "com.android.server.wm.ActivityTaskManagerService",
                "com.android.server.am.ActivityManagerService"
        };
        for (int i = 0; i < cls.length; i++) {
            try {
                java.lang.Class c = com.oupao.wiz.Rt.cls(p6, cls[i]);
                if (c == null) {
                    continue;
                }
                java.lang.reflect.Method[] ms = c.getDeclaredMethods();
                for (int k = 0; k < ms.length; k++) {
                    if (!"toggleRecentApps".equals(ms[k].getName())) {
                        continue;
                    }
                    ms[k].setAccessible(true);
                    if (com.oupao.wiz.XBridge.hookMethod(ms[k], new com.oupao.wiz.Hook(28)) != null) {
                        com.oupao.wiz.SLog.i("FlyRecents: toggleRecentApps 已挂钩@" + cls[i]);
                    }
                }
            } catch (Throwable ignore) {
            }
        }
    }

    static void onVideoCheck(com.oupao.wiz.XMP p2)    {
        try {
            com.oupao.wiz.Hooks.noteRecents();
        } catch (Throwable ignore) {
        }
        if (com.oupao.wiz.Cfg.videoNoPause()) {
            p2.setResult(Boolean.TRUE);
            com.oupao.wiz.SLog.i("VideoNoPause: \u8df3\u8fc7\u8fc7\u6e21\u6682\u505c");
        }
        return;
    }
    static void onWarn(com.oupao.wiz.XMP p4)
    {
        var v0_0 = p4.args;
        if ((v0_0 != null) && (v0_0.length >= 3)) {
            var v0_3 = v0_0[2];
            if ((v0_3 instanceof String)) {
                var v0_4 = ((String) v0_3);
                if (v0_4.startsWith("oupao_")) {
                    var v1_3 = new StringBuilder();
                    v1_3.append("WriteFix: \u653e\u884c ");
                    v1_3.append(v0_4);
                    com.oupao.wiz.SLog.i(v1_3.toString());
                    p4.setResult(null);
                }
            }
        }
        return;
    }
    public static String pkgOfEntry(Object p3)
    {
        Object[] v1_2 = null;
        java.lang.Object v0_5 = null;
        try {
            v1_2 = new Object[0];
            v0_5 = com.oupao.wiz.Rt.callLoose(p3, "getSbn", v1_2);
        } catch (Throwable __e_v0) {
            return null;
        }
        if (v0_5 != null) {
            Object[] v2_1 = new Object[0];
            var v0_1 = com.oupao.wiz.Rt.callLoose(v0_5, "getPackageName", v2_1);
            if ((v0_1 instanceof String)) {
                return ((String) v0_1);
            }
        }
        return null;
    }
    static float scaleOf(Object p8)
    {
        if (p8 != null) {
            var v1_1 = com.oupao.wiz.Rt.callNoArg(p8, "getWrapper");
            if (v1_1 != null) {
                var v1_2 = com.oupao.wiz.Rt.callNoArg(v1_1, "getExtImpl");
                if (v1_2 != null) {
                    String[] v2_1 = new String[3];
                    v2_1[0] = "getScale";
                    v2_1[1] = "getCurrentScale";
                    v2_1[2] = "getAdjustedScale";
                    int v3_3 = 0;
                    while (v3_3 < 3) {
                        var v5_2 = com.oupao.wiz.Rt.callNoArg(v1_2, v2_1[v3_3]);
                        if ((v5_2 instanceof Number)) {
                            var v6_3 = ((Number) v5_2).floatValue();
                            if ((v6_3 > 0) && (v6_3 <= 2.0f)) {
                                return v6_3;
                            }
                        }
                        v3_3++;
                    }
                    String[] v2_3 = new String[3];
                    v2_3[0] = "mScale";
                    v2_3[1] = "mCurrentScale";
                    v2_3[2] = "mAdjustedScale";
                    int v3_7 = 0;
                    while (v3_7 < 3) {
                        var v5_0 = com.oupao.wiz.Rt.get(v1_2, v2_3[v3_7]);
                        if ((v5_0 instanceof Number)) {
                            var v6_1 = ((Number) v5_0).floatValue();
                            if ((v6_1 > 0) && (v6_1 <= 2.0f)) {
                                return v6_1;
                            }
                        }
                        v3_7++;
                    }
                }
            }
        }
        return 0;
    }
    static android.graphics.Rect shrinkTo(android.graphics.Rect p6, float p7)
    {
        var v3 = p6.left;
        var v4 = p6.top;
        return new android.graphics.Rect(v3, v4, (v3 + Math.round((((float) p6.width()) * p7))), (Math.round((((float) p6.height()) * p7)) + v4));
    }
    public static Object task()
    {
        return com.oupao.wiz.Hooks.sTask;
    }
    /**
     * 2810：直接转「迷你窗 / 悬浮」—— 照 `tryNativeExit()`（双击小白条关窗）的套路，
     * 只是把策略对象换成系统里的兄弟类 `ExitFlexibleTaskToFloatStrategy`：
     *   caption.mFlexibleTaskController + task + 策略(Task, boolean, Rect)
     *   → controller.exitFlexibleTaskWindowInnerLocked(strategy)
     *
     * 走这条就不用"重放一次上滑"，键盘在时也能一步转迷你窗。
     */
    /**
     * 2942：直接调 ROM 自己的「转迷你窗」入口。
     *
     * 上一版用的是 `ExitFlexibleTaskToFloatStrategy`（=弹出/贴边浮窗），用户反馈
     * 「上滑变成了贴边小窗，不是迷你窗」——策略选错了。
     *
     * 迷你窗在这个 ROM 里不是一个 "exit strategy"，而是 `FlexibleTaskController`
     * 的 **`maySwitchStateToMini(Task, ActivityRecord, ActivityOptions)`**（反编译确认的签名）。
     */
    static boolean tryNativeMini()
    {
        try {
            if (com.oupao.wiz.Hooks.miniScale()) {
                com.oupao.wiz.SLog.i("转迷你窗: 已经是迷你态，跳过");
                return true;
            }
            Object cap = com.oupao.wiz.Hooks.sCaption;
            Object ctrl = (cap == null) ? null : com.oupao.wiz.Rt.get(cap, "mFlexibleTaskController");
            Object task = com.oupao.wiz.Hooks.sTask;
            if ((task == null) && (cap != null)) {
                task = com.oupao.wiz.Rt.get(cap, "mTask");
            }
            if (ctrl == null) {
                // caption 没抓到（键盘在时常见）：从 WMS 里取顶层 zoom task
                Object w = com.oupao.wiz.Hooks.wms();
                if (w != null) {
                    ctrl = com.oupao.wiz.Rt.callNoArg(w, "getFlexibleTaskController");
                }
            }
            if (task == null) {
                Object w2 = com.oupao.wiz.Hooks.wms();
                if (w2 != null) {
                    task = com.oupao.wiz.Rt.callNoArg(w2, "getTopFlexibleTask");
                }
            }
            if ((ctrl == null) || (task == null)) {
                com.oupao.wiz.SLog.i("转迷你窗: 缺 controller/task ctrl=" + (ctrl != null) + " task=" + (task != null));
                return false;
            }
            Object ar = null;
            String[] tops = new String[]{"getTopActivity", "topRunningActivity", "getTopNonFinishingActivity"};
            for (int i = 0; (i < tops.length) && (ar == null); i++) {
                ar = com.oupao.wiz.Rt.callNoArg(task, tops[i]);
            }
            if (ar == null) {
                com.oupao.wiz.SLog.i("转迷你窗: 拿不到 top ActivityRecord");
                return false;
            }
            android.app.ActivityOptions opts = android.app.ActivityOptions.makeBasic();
            // ★ 3076：**不能写死参数个数**。以前固定 methodByName(...,3)，
            //   一旦某台机器的签名是 2 个或 4 个参数，就会"方法没找到 → invoke 返回 null →
            //   日志只有一行 ret=null"，完全看不出原因（对方机器就是这么失效的）。
            //   现在：按名字把控制类里所有 *Mini* 方法列出来（打一次日志），
            //   然后按**参数类型**自动拼实参调用。
            java.lang.reflect.Method miniM = null;
            StringBuilder cand = new StringBuilder();
            Class<?> cc = ctrl.getClass();
            int depth = 0;
            while ((cc != null) && (cc != java.lang.Object.class) && (depth < 6)) {
                java.lang.reflect.Method[] ms;
                try {
                    ms = cc.getDeclaredMethods();
                } catch (Throwable t2) {
                    ms = new java.lang.reflect.Method[0];
                }
                for (int i = 0; i < ms.length; i++) {
                    String nm = ms[i].getName();
                    if (nm.indexOf("Mini") < 0) continue;
                    if (cand.length() > 0) cand.append(" | ");
                    cand.append(nm).append('(');
                    Class<?>[] ps = ms[i].getParameterTypes();
                    for (int j = 0; j < ps.length; j++) {
                        if (j > 0) cand.append(',');
                        cand.append(ps[j].getSimpleName());
                    }
                    cand.append(')');
                    if (miniM == null) miniM = ms[i];
                    // 优先精确同名
                    if (nm.equals("maySwitchStateToMini")) miniM = ms[i];
                }
                cc = cc.getSuperclass();
                depth++;
            }
            if (!miniSigLogged) {
                miniSigLogged = true;
                com.oupao.wiz.SLog.i("转迷你窗: 控制类 " + ctrl.getClass().getSimpleName()
                        + " 的 Mini 方法 = " + ((cand.length() == 0) ? "无" : cand.toString()));
            }
            if (miniM == null) {
                com.oupao.wiz.SLog.i("转迷你窗: 控制类里没有 *Mini* 方法，放弃");
                return false;
            }
            try {
                miniM.setAccessible(true);
            } catch (Throwable t3) {
            }
            Class<?>[] pts = miniM.getParameterTypes();
            Object[] argv = new Object[pts.length];
            for (int i = 0; i < pts.length; i++) {
                Class<?> t = pts[i];
                if (t.isInstance(task)) argv[i] = task;
                else if (t.isInstance(ar)) argv[i] = ar;
                else if (android.app.ActivityOptions.class.isAssignableFrom(t)) argv[i] = opts;
                else if (t == Integer.TYPE) argv[i] = Integer.valueOf(0);
                else if (t == Boolean.TYPE) argv[i] = Boolean.FALSE;
                else argv[i] = null;
            }
            Object r;
            try {
                r = miniM.invoke(ctrl, argv);
            } catch (Throwable t4) {
                com.oupao.wiz.SLog.e("转迷你窗: 调用 " + miniM.getName() + "/" + argv.length + " 失败", t4);
                return false;
            }
            com.oupao.wiz.SLog.i("转迷你窗: 调 " + miniM.getName() + "/" + argv.length + " ret=" + r);
            return true;
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("转迷你窗: tryNativeMini 异常", t);
            return false;
        }
    }

    static boolean tryNativeExit()
    {
        return tryNativeExitFor(com.oupao.wiz.Hooks.sCaption, com.oupao.wiz.Hooks.sTask);
    }

    static boolean tryNativeExitFor(Object caption, Object task)
    {
        var v1_0 = caption;
        if (v1_0 != null) {
            var v2_1 = com.oupao.wiz.Rt.get(v1_0, "mFlexibleTaskController");
            java.lang.Object v3 = task;
            if (v3 == null) {
                v3 = com.oupao.wiz.Rt.get(v1_0, "mTask");
            }
            if ((v2_1 != null) && (v3 != null)) {
                Object[] v4_1 = new Object[1];
                v4_1[0] = v3;
                com.oupao.wiz.Rt.callLoose(v2_1, "handleCloseEventForLightOpenIfNeed", v4_1);
                var v4_3 = com.oupao.wiz.Rt.cls(com.oupao.wiz.Hooks.CL, "com.android.server.wm.ExitFlexibleTaskToBackStrategy");
                if (v4_3 != null) {
                    Object[] v5_4 = new Object[2];
                    v5_4[0] = v3;
                    v5_4[1] = Boolean.TRUE;
                    var v4_4 = com.oupao.wiz.Rt.newInstance(v4_3, v5_4);
                    if (v4_4 != null) {
                        Object[] v5_6 = new Object[1];
                        v5_6[0] = Integer.valueOf(512);
                        com.oupao.wiz.Rt.callLoose(v4_4, "setSourceFrom", v5_6);
                        Object[] v5_8 = new Object[1];
                        v5_8[0] = v4_4;
                        com.oupao.wiz.Rt.callLoose(v2_1, "exitFlexibleTaskWindowInnerLocked", v5_8);
                        com.oupao.wiz.SLog.i("\u5173\u95ed\u5c0f\u7a97: \u5df2\u8c03\u7528\u539f\u751f\u9000\u51fa\u7b56\u7565");
                        return true;
                    }
                }
            }
        }
        com.oupao.wiz.SLog.i("\u5173\u95ed\u5c0f\u7a97: \u7f3a\u5c11 caption/controller/task \u6216\u7b56\u7565\u4e0d\u53ef\u7528");
        return false;
    }

    // ────────────────────────────────────────────────────────────────
    // 3226：「限制小窗数量」超上限时**关闭最早的小窗**，而不是让 ROM 自动贴边。
    //
    // 系统默认行为：到上限后把上一个小窗转成"贴边"，贴边满两个才关最旧的。
    // 用户要的是：到上限就直接关最早的，不要贴边。
    // 做法：在每次 startZoomWindow 之前先数一下现有迷你窗（缩放态），
    //      达到上限就先关掉最旧的那个，ROM 就不会触发贴边了。
    // ────────────────────────────────────────────────────────────────

    /**
     * 3238：这个迷你窗是不是**贴边小窗**（浮条状态）。
     *
     * 不再用几何猜（宽度/是否贴边都会误判）。用 ROM 自己的状态位：
     *   `FlexibleCaptionView.isFloatState()` = `ITaskExt.getFlexibleZoomState() == 4`
     * 也就是说 **zoomState==4 就是"贴边/浮条"状态**，这是系统判定，最可靠。
     */
    static boolean isDockedZoom(Object t)
    {
        try {
            return com.oupao.wiz.Win.zoomState(t) == 4;
        } catch (Throwable e) {
            return false;
        }
    }

    /** 当前活着的迷你窗数量（CapR 注册表 + 缩放判定；**不含手动贴边的**） */
    static int liveZoomCount()
    {
        int n = 0;
        try {
            com.oupao.wiz.CapR.newestIdx();               // 触发 init
            Object[] caps = com.oupao.wiz.CapR.sCap;
            Object[] tasks = com.oupao.wiz.CapR.sTask;
            if ((caps == null) || (tasks == null)) {
                return 0;
            }
            for (int i = 0; i < caps.length; i++) {
                Object c = caps[i];
                Object t = tasks[i];
                if ((c == null) || (t == null)) {
                    continue;
                }
                if (com.oupao.wiz.Win.taskId(t) <= 0) {
                    continue;
                }
                // 3228：不再只看"缩放态" —— 快速连开时，新窗的 scale 还没落下来（仍≈1），
                //   之前会漏数 → 数量不够 → ROM 就按默认行为把旧窗贴边了。
                //   现在凡是 CapR 里还活着的 caption/task 都算（贴边的**也计数**）。
                // 3257：**只算真的还在小窗态的**。
                //   实测（3256）：CapR 里留着已经释放/已经转全屏的 task（日志 Dump 里 vis=false），
                //   被反复计数 → `到上限(3/1)` → 一次连关两个 → 两条退场动画互相打断，
                //   用户看到"上一个窗瞬间消失、没有动画、闪一下"。
                if (com.oupao.wiz.Win.zoomState(t) == 0) {
                    continue;
                }
                n++;
            }
        } catch (Throwable ignore) {
        }
        return n;
    }

    /**
     * 3249：这个包（可选指定用户）是不是**已经有一个活着的小窗**。
     *
     * 场景（实测日志）：微信分身已经是小窗了，用户又从扇形菜单选同一个分身 →
     * 3244 起不再用 `winOpen()` 挡过渡，于是把全屏任务又转成一个小窗，
     * 同时 ROM 到上限把原来那个关掉/贴边（`NoDock: 已关掉本要贴边的窗口`）。
     * 用户看到的就是"开一个 → 关一个"两套动画叠在一起，连续操作时明显卡顿。
     */
    static boolean samePkgZoomOpen(String pkg, int userId)
    {
        try {
            if (pkg == null) {
                return false;
            }
            com.oupao.wiz.CapR.newestIdx();               // 触发 init
            Object[] caps = com.oupao.wiz.CapR.sCap;
            Object[] tasks = com.oupao.wiz.CapR.sTask;
            if ((caps == null) || (tasks == null)) {
                return false;
            }
            for (int i = 0; i < caps.length; i++) {
                Object c = caps[i];
                Object t = tasks[i];
                if ((c == null) || (t == null)) {
                    continue;
                }
                if (com.oupao.wiz.Win.taskId(t) <= 0) {
                    continue;
                }
                if (!pkg.equals(com.oupao.wiz.Win.taskPkg(t))) {
                    continue;
                }
                // 3259：**贴边窗不算"该应用已有小窗"**。
                //   实测：微信已经贴在边上，用户再把全屏的微信呼出成小窗 →
                //   这里判成"已有小窗"跳过过渡 → 退回 startZoomWindow，
                //   ROM 只是把贴边那个提到前面，全屏的那个根本没变小窗（用户看到"失效"）。
                if (com.oupao.wiz.Hooks.isDockedZoom(t)) {
                    continue;
                }
                if (userId >= 0) {
                    int tu = com.oupao.wiz.Win.taskUid(t) / 100000;
                    if ((tu != 0) && (tu != userId)) {
                        continue;                          // 主应用 / 别的分身，不算同一个
                    }
                }
                return true;
            }
        } catch (Throwable ignore) {
        }
        return false;
    }

    /** 3298：拿到当前所有 root task（真实 Task 对象，不依赖 CapR） */
    static java.util.ArrayList<Object> allRootTasks()
    {
        try {
            Object w = com.oupao.wiz.Hooks.wms();
            Object atms = com.oupao.wiz.Rt.get(w, "mAtms");
            if (atms == null) {
                atms = com.oupao.wiz.Rt.get(w, "mAtmService");
            }
            Object root = com.oupao.wiz.Rt.get(atms, "mRootWindowContainer");
            Object list = com.oupao.wiz.Rt.callNoArg(root, "getAllRootTasks");
            if (!(list instanceof java.util.List)) {
                list = com.oupao.wiz.Rt.callNoArg(root, "getTasks");
            }
            if (!(list instanceof java.util.List)) {
                return null;
            }
            java.util.ArrayList<Object> out = new java.util.ArrayList<Object>();
            for (Object o : (java.util.List<?>) list) {
                if (o != null) {
                    out.add(o);
                }
            }
            return out;
        } catch (Throwable ignore) {
            return null;
        }
    }

    static void tryWriteFix(Class p4)
    {
        if (p4 == null) {
            com.oupao.wiz.SLog.i("WriteFix: \u672a\u627e\u5230\u7c7b(\u8df3\u8fc7)");
            return;
        } else {
            if (!com.oupao.wiz.Hooks.sWriteFixDone) {
                Class[] v0_7 = new Class[3];
                v0_7[0] = android.content.Context.class;
                v0_7[1] = Integer.TYPE;
                v0_7[2] = String.class;
                var v0_1 = com.oupao.wiz.Rt.method(p4, "warnOrThrowForUndesiredSecureSettingsMutationForTargetSdk", v0_7);
                if (v0_1 == null) {
                    com.oupao.wiz.SLog.i("WriteFix: \u672a\u627e\u5230 warnOrThrow \u65b9\u6cd5");
                } else {
                    com.oupao.wiz.XBridge.hookMethod(v0_1, new com.oupao.wiz.Hook(2));
                    com.oupao.wiz.Hooks.sWriteFixDone = true;
                    com.oupao.wiz.SLog.i("WriteFix: \u8bbe\u7f6e\u5199\u5165\u653e\u884c\u5df2\u5b89\u88c5");
                    return;
                }
            }
            return;
        }
    }
    static void verifyClosed(int p4)
    {
        var v0 = com.oupao.wiz.Hooks.main();
        if (v0 != null) {
            v0.postDelayed(new com.oupao.wiz.Act(2, p4), 400);
        }
        return;
    }
    static void verifyFull(int p4)
    {
        var v0 = com.oupao.wiz.Hooks.main();
        if (v0 != null) {
            v0.postDelayed(new com.oupao.wiz.Act(3, p4), 400);
        }
        return;
    }
    /** 当前显示尺寸（px）；拿不到返回 {0,0} */
    static int[] screenSize(android.view.View v)
    {
        int[] r = new int[2];
        try {
            var d = v.getDisplay();
            if (d != null) {
                var dm = new android.util.DisplayMetrics();
                d.getRealMetrics(dm);
                r[0] = dm.widthPixels;
                r[1] = dm.heightPixels;
            }
        } catch (Throwable ignore) {
        }
        return r;
    }

    /**
     * 系统手势区 insets，返回 {左,上,右,下}（窗口本地坐标）。
     *
     * 必须让出来：可触区扩到全屏时如果把手势区也盖住，
     * "上滑开多任务 / 侧滑返回"这些系统手势会被本窗口吃掉（表现为屏幕点不动、划不动）。
     */
    static int[] gestureInsets(android.view.View v)
    {
        try {
            var wi = v.getRootWindowInsets();
            if (wi == null) {
                return null;
            }
            int[] r = new int[4];
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                var g = wi.getInsets(android.view.WindowInsets.Type.systemGestures());
                var n = wi.getInsets(android.view.WindowInsets.Type.navigationBars());
                r[0] = Math.max(g.left, n.left);
                r[1] = Math.max(g.top, n.top);
                r[2] = Math.max(g.right, n.right);
                r[3] = Math.max(g.bottom, n.bottom);
            } else {
                r[3] = wi.getSystemWindowInsetBottom();
            }
            if ((r[0] <= 0) && (r[1] <= 0) && (r[2] <= 0) && (r[3] <= 0)) {
                return null;
            }
            return r;
        } catch (Throwable t) {
            return null;
        }
    }

    /** 是否处于"缩放迷你窗"状态（任务缩放 < 0.35）——迷你窗不该有"窗外点击关闭" */
    /** 3076：控制类里 *Mini* 方法的签名只在第一次打印（每 4 次重试都打会刷屏） */
    static boolean miniSigLogged = false;

    /** 3076：`maySwitchStateToMini` 实际使用的 task（键盘在时 sTask 可能为空） */
    static Object lastMiniTask = null;

    /** 3076：键盘在时"等着转迷你"的标记（手指抬起时再补一次） */
    static boolean miniPending = false;
    static long miniPendingAt = 0L;

    /**
     * 3079：IME 守卫放行窗口。> 当前时间时，`HookImeGuard` 会把
     * `FlexiblePointerHandler#isPhoneLandScreenAndImeShowing()` 压成 false，
     * 让 ROM 在"键盘弹起"状态下也走迷你窗的缩放手势管线。
     */
    static volatile long imeMiniUntil = 0L;

    /** 2948：键盘在时转迷你的多次尝试，一旦成功就停。 */
    static void miniRetry(final int step)
    {
        // ★ 3080：把节奏压到"跟手"。3079 的 0/120/300/520/760/1000/1400 实测要 1.7s 才落定
        //   （ROM 在手势进行中会提前返回，只能靠后几次命中），现在改成前密后疏：
        //   0/60/120/200/300/420/560ms —— 覆盖手指还在滑的整段，松手前后就能落定。
        final long[] DELAYS = new long[]{0L, 60L, 120L, 200L, 300L, 420L, 560L};
        try {
            if (com.oupao.wiz.Hooks.miniScale()) {
                com.oupao.wiz.SLog.i("FlyMini: 已是迷你态 ✓ (step=" + step + ")");
                com.oupao.wiz.Hooks.miniPending = false;
                return;
            }
            com.oupao.wiz.Hooks.tryNativeMini();
            if (step + 1 >= DELAYS.length) {
                com.oupao.wiz.Hooks.miniPending = false;
                com.oupao.wiz.SLog.i("FlyMini: 试完 " + DELAYS.length
                        + " 次都没进迷你态（看上面「控制类的 Mini 方法」那行签名）");
                return;
            }
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                public void run() {
                    com.oupao.wiz.Hooks.miniRetry(step + 1);
                }
            }, DELAYS[step]);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("FlyMini: miniRetry 异常", t);
        }
    }

    static boolean miniScale()
    {
        try {
            // 3076：键盘在时 `sTask` 可能抓不到（caption 没拿到），那么"成功与否"永远判断不出来 →
            //   退回到 tryNativeMini 实际用的那个 task。
            Object t = com.oupao.wiz.Hooks.sTask;
            if (t == null) t = lastMiniTask;
            float s = com.oupao.wiz.Hooks.scaleOf(t);
            return (s > 0.0f) && (s < 0.35f);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 小窗是否已经基本不在屏幕上。
     *
     * 用途：ColorOS17 打开多任务时会把迷你窗推到屏幕外（ColorOS16 不会）。
     * 这时如果可触区还扩到全屏，整屏触摸都会被这个看不见的窗口吃掉（屏幕点不动）。
     *
     * 注意：caption view 自身尺寸始终是"整窗尺寸"（缩放是窗口层面做的），
     * 所以优先用任务 bounds（`viewRectNow()` 已按缩放折算），退化才用 view 几何。
     */
    static boolean mostlyOffScreen(Object p3)
    {
        try {
            int sw = 0;
            int sh = 0;
            if (p3 instanceof android.view.View) {
                var s = com.oupao.wiz.Hooks.screenSize(((android.view.View) p3));
                sw = s[0];
                sh = s[1];
            }
            var r = com.oupao.wiz.Hooks.viewRectNow();
            if ((r == null) || (r.width() <= 0) || (r.height() <= 0)) {
                if (!(p3 instanceof android.view.View)) {
                    return false;
                }
                var v = ((android.view.View) p3);
                int w = v.getWidth();
                int h = v.getHeight();
                if ((w <= 0) || (h <= 0)) {
                    return true;
                }
                int[] loc = new int[2];
                v.getLocationOnScreen(loc);
                r = new android.graphics.Rect(loc[0], loc[1], loc[0] + w, loc[1] + h);
            }
            if ((sw <= 0) || (sh <= 0)) {
                return false;
            }
            int vw = Math.min(r.right, sw) - Math.max(r.left, 0);
            int vh = Math.min(r.bottom, sh) - Math.max(r.top, 0);
            if ((vw <= 0) || (vh <= 0)) {
                return true;                        // 完全在屏幕外
            }
            long vis = ((long) vw) * ((long) vh);
            long all = ((long) r.width()) * ((long) r.height());
            if (all <= 0) {
                return true;
            }
            return ((vis * 2L) < all);              // 可见面积不到一半
        } catch (Throwable t) {
            return false;
        }
    }
    /**
     * 2830：横条（FlexibleCaptionView）此刻在屏幕上的中心点。
     *
     * 为什么还要这个：`viewRectNow()` 是按 **task 的 bounds × scale** 反推的，
     * 键盘收起后窗口重新布局时它不一定同步更新；而 caption view 自己知道真实位置
     * （`getLocationOnScreen`），拿它更准。取不到就返回 null，调用方回退到矩形算。
     */
    private static int[] captionPointNow()
    {
        Object o = sCaption;
        if (!(o instanceof android.view.View)) {
            return null;
        }
        android.view.View v = (android.view.View) o;
        if ((v.getWidth() <= 0) || (v.getHeight() <= 0) || (!v.isAttachedToWindow())) {
            return null;
        }
        int[] loc = new int[2];
        v.getLocationOnScreen(loc);
        return new int[]{ loc[0] + (v.getWidth() / 2), loc[1] + (v.getHeight() / 2) };
    }

    /**
     * 2830：等窗口矩形稳定，再拿来做坐标。
     *
     * 键盘收起后小窗会重新布局（要变回原尺寸），**必须等它变完再量**；
     * 早量一步就会把补发的手势打在窗口中间（用户实测：只关了键盘 + 页面往上滚了一下）。
     *
     * @param oldRect 变化前的矩形（键盘还在时量的）；为 null 时"连续两帧一致"就算稳定
     * @return 稳定后的矩形；一直没变化/读不到就返回最后一次读到的（可能为 null）
     */
    private static android.graphics.Rect settleRect(android.graphics.Rect oldRect, int tries, long gap)
    {
        android.graphics.Rect prev = null;
        android.graphics.Rect cur = null;
        for (int i = 0; i < tries; i++) {
            try {
                Thread.sleep(gap);
            } catch (Throwable ignore) {
            }
            android.graphics.Rect now = curRect();
            if (now == null) {
                continue;
            }
            boolean moved = (oldRect == null) || (!now.equals(oldRect));
            if (moved && (prev != null) && now.equals(prev)) {
                return now;
            }
            prev = now;
            cur = now;
        }
        return cur;
    }

    /**
     * 2831：拿 WindowManagerService 本体。
     *
     * 为什么能这么拿：我们跑在 system_server 里，`ServiceManager.getService("window")`
     * 返回的就是本进程里的 WMS 实例（不是 binder proxy），可以直接读它的字段。
     */
    private static Object sWms;

    static Object wms()
    {
        if (sWms != null) {
            return sWms;
        }
        try {
            java.lang.Class<?> sm = java.lang.Class.forName("android.os.ServiceManager");
            java.lang.reflect.Method m = sm.getMethod("getService", java.lang.String.class);
            java.lang.Object b = m.invoke(null, "window");
            if (b != null) {
                sWms = b;
            }
        } catch (Throwable ignore) {
        }
        return sWms;
    }

    /** 2831：输入法窗口的上边界（屏幕坐标；拿不到返回 -1） */
    static int imeTop()
    {
        try {
            Object w = wms();
            if (w == null) {
                return -1;
            }
            Object ime = com.oupao.wiz.Rt.get(w, "mInputMethodWindow");
            if (ime == null) {
                ime = com.oupao.wiz.Rt.callNoArg(w, "getInputMethodWindowLocked");
            }
            Object f = com.oupao.wiz.Rt.get(ime, "mFrame");
            if (f instanceof android.graphics.Rect) {
                android.graphics.Rect r = (android.graphics.Rect) f;
                if ((r.top > 0) && (r.height() > 0)) {
                    return r.top;
                }
            }
        } catch (Throwable ignore) {
        }
        return -1;
    }

    /**
     * 2831：当前小窗的整体矩形（含底部横条）。
     *
     * 老的 `viewRectNow()` 靠 `sCaption/sTask/sRawRect` 那套缓存，而实测**这套缓存经常是空的**
     * （日志里 `win=?`）—— 于是 `inStrip()` 永远判不中，整个「键盘弹起横条上滑」链路根本没触发。
     * 所以这里加一条**直接问 WMS** 的路：取当前聚焦窗口的 `mFrame`
     * （浮窗小窗时它就是小窗整体矩形，横条在最底部）。
     */
    static android.graphics.Rect curRect()
    {
        android.graphics.Rect r = viewRectNow();
        if ((r != null) && (r.width() > 0) && (r.height() > 0)) {
            return r;
        }
        try {
            Object w = wms();
            if (w != null) {
                Object win = com.oupao.wiz.Rt.get(w, "mFocusedWindow");
                Object f = com.oupao.wiz.Rt.get(win, "mFrame");
                if (f instanceof android.graphics.Rect) {
                    android.graphics.Rect rr = new android.graphics.Rect((android.graphics.Rect) f);
                    if ((rr.width() > 0) && (rr.height() > 0)) {
                        return rr;
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    /**
     * 2831：判横条用的矩形。
     *
     * 小窗矩形拿不到时，用**键盘上沿当小窗底边** —— 键盘弹起时小窗被压到键盘上方，
     * 底边基本就等于键盘上沿；横条就在紧贴这条边的那 32dp 里。
     */
    static android.graphics.Rect stripRect()
    {
        int dispW = 1216;
        int dispH = 2640;
        try {
            android.content.Context c = com.oupao.wiz.Cfg.sysContext();
            if (c != null) {
                android.util.DisplayMetrics dm = c.getResources().getDisplayMetrics();
                dispW = dm.widthPixels;
                dispH = dm.heightPixels;
            }
        } catch (Throwable ignore) {
        }
        android.graphics.Rect r = curRect();
        // 满屏矩形说明取到的是全屏窗口（不是浮窗小窗），不能拿来判横条
        if ((r != null) && (r.height() >= dispH - 8)) {
            r = null;
        }
        if (r != null) {
            return r;
        }
        int top = imeTop();
        if (top <= 0) {
            return null;
        }
        return new android.graphics.Rect(0, 0, dispW, top);
    }

        // 2940：键盘弹出时每次 DOWN 都要算一次横条矩形（WMS 反射 + getDisplayMetrics），
    //   输入法按键体感延迟就是从这里来的。矩形 350ms 内复用。
    private static long srAt;
    private static android.graphics.Rect srCache;
    private static boolean srValid;

    static android.graphics.Rect stripRectCached()
    {
        long now = android.os.SystemClock.uptimeMillis();
        if (srValid && ((now - srAt) < 350L)) {
            return srCache;
        }
        android.graphics.Rect r = null;
        try {
            r = stripRect();
        } catch (Throwable ignore) {
        }
        srCache = r;
        srAt = now;
        srValid = true;
        return r;
    }

static android.graphics.Rect viewRectNow()
    {
        return viewRectOfTask(com.oupao.wiz.Hooks.sTask);
    }

    /**
     * 3325：按「当前这个 caption 对应的 task」取可见矩形。
     *
     * 多开小窗时 `Hooks.sTask` 只指向最近一次 focus/record 的那扇窗；
     * 窗外点击可能落在另一扇窗的扩展可触区里，用 sTask 的矩形去 contains()
     * 会把窗外误判成「窗内」→ 偶现点了没关。
     */
    static android.graphics.Rect viewRectNowFor(Object caption)
    {
        Object t = null;
        try {
            t = com.oupao.wiz.CapR.task(caption);
        } catch (Throwable ignore) {
        }
        if (t == null) {
            try {
                t = com.oupao.wiz.Rt.get(caption, "mTask");
            } catch (Throwable ignore) {
            }
        }
        return viewRectOfTask(t != null ? t : com.oupao.wiz.Hooks.sTask);
    }

    private static android.graphics.Rect viewRectOfTask(Object v1)
    {
        android.graphics.Rect v0_0 = com.oupao.wiz.Hooks.sRawRect;
        if (v1 != null) {
            var v2_1 = com.oupao.wiz.Rt.callNoArg(v1, "getBounds");
            if (v2_1 instanceof android.graphics.Rect) {
                var v2_2 = ((android.graphics.Rect) v2_1);
                var v3_0 = com.oupao.wiz.Hooks.scaleOf(v1);
                if ((v3_0 > 0) && (v3_0 < 1.0f)) {
                    v0_0 = new android.graphics.Rect(v2_2.left, v2_2.top, (v2_2.left + Math.round((((float) v2_2.width()) * v3_0))), (v2_2.top + Math.round((((float) v2_2.height()) * v3_0))));
                } else {
                    v0_0 = new android.graphics.Rect(v2_2);
                }
            }
        }
        return v0_0;
    }
    public static boolean winOpen()
    {
        if (com.oupao.wiz.Hooks.sTask == null) {
            return false;
        } else {
            return true;
        }
    }
}
