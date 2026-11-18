package com.oupao.wiz;
public class Win {
    private Win()
    {
        return;
    }
    public static String actPkg(Object p2)
    {
        java.lang.String v0_0 = null;
        if (p2 != null) {
            var v1_1 = com.oupao.wiz.Rt.get(p2, "packageName");
            if (!(v1_1 instanceof String)) {
                v0_0 = null;
            } else {
                return ((String) v1_1);
            }
        }
        return v0_0;
    }
    public static Object actTask(Object p3)
    {
        java.lang.Object v0_0 = null;
        if (p3 != null) {
            java.lang.Object v1_3 = com.oupao.wiz.Rt.callNoArg(p3, "getTask");
            if (v1_3 == null) {
                var v1_5 = com.oupao.wiz.Rt.get(p3, "mTaskFragment");
                if (v1_5 != null) {
                    java.lang.Object v1_0 = com.oupao.wiz.Rt.callNoArg(v1_5, "getTask");
                    if (v1_0 != null) {
                        return v1_0;
                    }
                }
                v0_0 = com.oupao.wiz.Rt.get(p3, "mTask");
            } else {
                return v1_3;
            }
        }
        return v0_0;
    }
    public static int actUid(Object p3)
    {
        int v0 = 0;
        if (p3 != null) {
            var v1_1 = com.oupao.wiz.Rt.callNoArg(p3, "getUid");
            if ((v1_1 instanceof Number)) {
                v0 = ((Number) v1_1).intValue();
            }
        }
        return v0;
    }
    public static Object capTask(Object p1)
    {
        return com.oupao.wiz.Rt.get(p1, "mTask");
    }
    public static Object fragAct(Object p2)
    {
        java.lang.Object v0_0 = null;
        if (p2 != null) {
            java.lang.Object v1_1 = com.oupao.wiz.Rt.get(p2, "mResumedActivity");
            if (v1_1 == null) {
                v0_0 = com.oupao.wiz.Rt.get(p2, "mLastPausedActivity");
            } else {
                return v1_1;
            }
        }
        return v0_0;
    }
    public static boolean sameTask(Object p3, Object p4)
    {
        // 原版 Win.smali:164-200 —— 移植时这里丢了 return false，
        // 导致对任意两个非 null 对象都返回 true，进而 Dock.isDocked 把所有任务都当成贴边任务、
        // 全局挡 startPausing（多任务过渡卡死 → 上滑直接回桌面）。这里按 smali 还原。
        try {
            if ((p3 == null) || (p4 == null)) {
                return false;
            }
            if (p3 == p4) {
                return true;
            }
            var v1 = com.oupao.wiz.Win.taskId(p3);
            var v2 = com.oupao.wiz.Win.taskId(p4);
            if (v1 < 0) {
                return false;
            }
            return (v1 == v2);
        } catch (Throwable __e___ex) {
            return false;
        }
    }
    public static Object taskAct(Object p4)
    {
        if (p4 != null) {
            String[] v1_1 = new String[3];
            v1_1[0] = "topRunningActivity";
            v1_1[1] = "getRootActivity";
            v1_1[2] = "getTopActivity";
            int v2_1 = 0;
            while (v2_1 < 3) {
                java.lang.Object v3_3 = com.oupao.wiz.Rt.callNoArg(p4, v1_1[v2_1]);
                if (v3_3 == null) {
                    v2_1++;
                } else {
                    return v3_3;
                }
            }
        }
        return null;
    }
    public static int taskId(Object p3)
    {
        int v0 = 0;
        if (p3 != null) {
            var v1_1 = com.oupao.wiz.Rt.get(p3, "mTaskId");
            if ((v1_1 instanceof Number)) {
                v0 = ((Number) v1_1).intValue();
            }
        }
        return v0;
    }
    public static String taskPkg(Object p2)
    {
        java.lang.String v0 = null;
        var v1 = com.oupao.wiz.Win.taskAct(p2);
        if (v1 != null) {
            v0 = com.oupao.wiz.Win.actPkg(v1);
        }
        return v0;
    }
    public static int taskUid(Object p2)
    {
        int v0 = 0;
        var v1 = com.oupao.wiz.Win.taskAct(p2);
        if (v1 != null) {
            v0 = com.oupao.wiz.Win.actUid(v1);
        }
        return v0;
    }
    /**
     * 3245：这个 task 当前是否处在"弹性窗口自定义过渡"里（非 0 = 在过渡）。
     *
     * 对应 ROM 自己的判定（`FlexibleCaptionView.shouldIgnoreTouchEvent`）：
     * ```
     * mTask.getWrapper().getExtImpl().hasFlexibleCustomTransitionType(2048 /* TRANSIT_FULL_SWITCH_TO_DEFAULT *\/)
     * ```
     * 实测日志：`shouldIgnoreTouchEvent: task is in TRANSIT_FULL_SWITCH_TO_DEFAULT`
     * —— 快速"开小窗 → 点窗外关 → 再开"时，关窗/开窗那一刻窗口还在做过渡动画。
     * 过渡中途去开/关会被 ROM 排队或合并，表现就是"卡一下然后没动画直接变窗口 / 窗口显示不全"。
     */
    public static int flexTransition(Object task)
    {
        try {
            Object w = com.oupao.wiz.Rt.callNoArg(task, "getWrapper");
            Object ext = com.oupao.wiz.Rt.callNoArg(w, "getExtImpl");
            Object v = com.oupao.wiz.Rt.callNoArg(ext, "getFlexibleCustomTransitionType");
            if (v instanceof Number) {
                return ((Number) v).intValue();
            }
        } catch (Throwable ignore) {
        }
        return 0;
    }

    /**
     * 3255：把当前最近的几个任务打到日志里（诊断"黑底/图层残留"用）。
     *
     * 出问题时我们只能看到自己的日志，看不到 ROM 的图层栈；
     * 用 `ActivityTaskManager.getTasks(n)` 拿 RunningTaskInfo（taskId / 包名 / bounds / 是否可见），
     * 就能判断"小窗还在、后面那个 task 还在不在、它的 bounds 是不是已经变了但没重排"。
     */
    public static void dumpTop(String tag)
    {
        try {
            Class<?> c = Class.forName("android.app.ActivityTaskManager");
            Object svc = com.oupao.wiz.Rt.invoke(
                    com.oupao.wiz.Rt.methodByName(c, "getService", 0), null, null);
            Object list = com.oupao.wiz.Rt.callLoose(svc, "getTasks",
                    new Object[]{Integer.valueOf(8)});
            if (!(list instanceof java.util.List)) {
                com.oupao.wiz.SLog.i("Dump(" + tag + "): getTasks 失败");
                return;
            }
            StringBuilder sb = new StringBuilder("Dump(" + tag + "):");
            for (Object t : (java.util.List<?>) list) {
                Object id = com.oupao.wiz.Rt.get(t, "taskId");
                Object top = com.oupao.wiz.Rt.get(t, "topActivity");
                Object bounds = com.oupao.wiz.Rt.get(t, "bounds");
                Object vis = com.oupao.wiz.Rt.get(t, "isVisible");
                sb.append(" [").append(id)
                        .append(' ').append(com.oupao.wiz.Rt.callNoArg(top, "getPackageName"))
                        .append(' ').append(bounds)
                        .append(" vis=").append(vis).append(']');
            }
            com.oupao.wiz.SLog.i(sb.toString());
        } catch (Throwable t) {
            com.oupao.wiz.SLog.i("Dump(" + tag + ") 异常: " + t);
        }
    }

    public static int zoomState(Object p4)
    {
        int v0_0 = 0;
        if (p4 != null) {
            var v4_1 = com.oupao.wiz.Rt.callNoArg(p4, "getWrapper");
            if (v4_1 != null) {
                var v4_2 = com.oupao.wiz.Rt.callNoArg(v4_1, "getExtImpl");
                if (v4_2 != null) {
                    var v2_0 = com.oupao.wiz.Rt.callNoArg(v4_2, "getFlexibleZoomState");
                    if (!(v2_0 instanceof Number)) {
                        var v2_1 = com.oupao.wiz.Rt.get(v4_2, "mFlexibleZoomState");
                        if ((v2_1 instanceof Number)) {
                            v0_0 = ((Number) v2_1).intValue();
                        }
                    } else {
                        return ((Number) v2_0).intValue();
                    }
                }
            }
        }
        return v0_0;
    }
}