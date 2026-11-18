package com.oupao.wiz;
public class Dock {
    static int sId;
    static int sMiss;
    public static String sPkg;
    static Object sTask;

    /**
     * 3260：最近一次贴边动画开始的时刻。
     *
     * 实测：贴边动画刚播完的那几百毫秒里，ROM 的过渡通道还是忙的，
     * 我们的 `switchFocusedTaskToZoom` 会被当成"已在进行中的过渡"合并掉 ——
     * 窗口 12ms 就落到小窗位置、**没有任何动画**（用户反馈"有贴边小窗时转小窗没动效"）。
     */
    static volatile long markedAt;
    private Dock()
    {
        return;
    }
    public static void clear(String p2)
    {
        com.oupao.wiz.Dock.sMiss = 0;
        com.oupao.wiz.Dock.sTask = null;
        com.oupao.wiz.Dock.sPkg = null;
        if (com.oupao.wiz.Dock.sTask != null) {
            com.oupao.wiz.SLog.i(new StringBuilder("DockKeep \u6e05\u9664(").append(p2).append(")").toString());
        }
        return;
    }
    private static void hookFloat(ClassLoader p3)
    {
        java.lang.Class v0_4 = null;
        try {
            v0_4 = com.oupao.wiz.Rt.cls(p3, "com.android.server.wm.ExitFlexibleTaskToFloatStrategy");
        } catch (Throwable __e_v0_5) {
            com.oupao.wiz.SLog.e("Dock: hookFloat \u5f02\u5e38", __e_v0_5);
            return;
        }
        if (v0_4 == null) {
            com.oupao.wiz.SLog.i("Dock: \u672a\u627e\u5230 ExitFlexibleTaskToFloatStrategy");
            return;
        } else {
            Class[] v2_1 = new Class[0];
            var v0_1 = com.oupao.wiz.Rt.method(v0_4, "startToFloatAnimation", v2_1);
            if (v0_1 == null) {
                com.oupao.wiz.SLog.i("Dock: \u672a\u627e\u5230 startToFloatAnimation");
                return;
            } else {
                com.oupao.wiz.XBridge.hookMethod(v0_1, new com.oupao.wiz.Dock$C1());
                com.oupao.wiz.SLog.i("Dock: \u5df2\u6302\u94a9 startToFloatAnimation");
                return;
            }
        }
    }
    private static void hookHandle(ClassLoader p6)
    {
        java.lang.Class v0_2 = null;
        try {
            v0_2 = com.oupao.wiz.Rt.cls(p6, "com.android.server.wm.FloatHandleController");
        } catch (Throwable __e_v0_6) {
            com.oupao.wiz.SLog.e("Dock: hookHandle \u5f02\u5e38", __e_v0_6);
            return;
        }
        if (v0_2 != null) {
            var v1_4 = com.oupao.wiz.Rt.cls(p6, "com.android.server.wm.Task");
            var v2_1 = com.oupao.wiz.Rt.cls(p6, "com.android.server.wm.floathandle.FloatHandleInfo");
            if ((v1_4 != null) && (v2_1 != null)) {
                Class[] v3_1 = new Class[4];
                v3_1[0] = v1_4;
                v3_1[1] = Boolean.TYPE;
                v3_1[2] = android.graphics.Rect.class;
                v3_1[3] = v2_1;
                var v0_3 = com.oupao.wiz.Rt.method(v0_2, "onFloatOpenAnimationStartInner", v3_1);
                if (v0_3 == null) {
                    com.oupao.wiz.SLog.i("Dock: \u672a\u627e\u5230 onFloatOpenAnimationStartInner");
                    return;
                } else {
                    com.oupao.wiz.XBridge.hookMethod(v0_3, new com.oupao.wiz.Dock$C2());
                    com.oupao.wiz.SLog.i("Dock: \u5df2\u6302\u94a9 onFloatOpenAnimationStartInner");
                    return;
                }
            }
        }
        com.oupao.wiz.SLog.i("Dock: FloatHandle \u4f9d\u8d56\u7c7b\u7f3a\u5931");
        return;
    }
    private static void hookSleep(ClassLoader p7)
    {
        java.lang.Class v0_1 = null;
        try {
            v0_1 = com.oupao.wiz.Rt.cls(p7, "com.android.server.wm.TaskFragment");
        } catch (Throwable __e_v0_2) {
            com.oupao.wiz.SLog.e("Dock: hookSleep \u5f02\u5e38", __e_v0_2);
            return;
        }
        if (v0_1 != null) {
            var v1_1 = com.oupao.wiz.Rt.cls(p7, "com.android.server.wm.ActivityRecord");
            if (v1_1 != null) {
                Class[] v2_1 = new Class[4];
                v2_1[0] = Boolean.TYPE;
                v2_1[1] = Boolean.TYPE;
                v2_1[2] = v1_1;
                v2_1[3] = String.class;
                var v2_2 = com.oupao.wiz.Rt.method(v0_1, "startPausing", v2_1);
                if (v2_2 != null) {
                    com.oupao.wiz.XBridge.hookMethod(v2_2, new com.oupao.wiz.Dock$C7());
                    com.oupao.wiz.SLog.i("Dock: \u5df2\u6302\u94a9 startPausing(4)");
                }
                Class[] v2_5 = new Class[3];
                v2_5[0] = Boolean.TYPE;
                v2_5[1] = v1_1;
                v2_5[2] = String.class;
                var v2_6 = com.oupao.wiz.Rt.method(v0_1, "startPausing", v2_5);
                if (v2_6 != null) {
                    com.oupao.wiz.XBridge.hookMethod(v2_6, new com.oupao.wiz.Dock$C8());
                    com.oupao.wiz.SLog.i("Dock: \u5df2\u6302\u94a9 startPausing(3)");
                }
                var v2_9 = com.oupao.wiz.Rt.methodByName(v1_1, "stopIfPossible", 0);
                if (v2_9 != null) {
                    com.oupao.wiz.XBridge.hookMethod(v2_9, new com.oupao.wiz.Dock$C9());
                    com.oupao.wiz.SLog.i("Dock: \u5df2\u6302\u94a9 stopIfPossible");
                }
                return;
            }
        }
        com.oupao.wiz.SLog.i("Dock: \u606f\u5c4f\u4fdd\u6d3b\u4f9d\u8d56\u7c7b\u7f3a\u5931");
        return;
    }
    public static String info()
    {
        return new StringBuilder("task#").append(com.oupao.wiz.Dock.sId).append(" pkg=").append(com.oupao.wiz.Win.taskPkg(com.oupao.wiz.Dock.sTask)).toString();
    }
    /** 3314/3377：C17 复用 C16 已验证的标记/暂停 hook。
     *  不直接改 C16 的 install 流程，只把这几个安装函数暴露给 DockKeepC17。 */
    static void installC16Hooks(ClassLoader p1)
    {
        com.oupao.wiz.Dock.hookFloat(p1);
        com.oupao.wiz.Dock.hookHandle(p1);
        com.oupao.wiz.Dock.hookSleep(p1);
        return;
    }
    public static void install(ClassLoader p1)
    {
        // 3314：**C17 与 C16 隔离**。
        //   「小窗贴边时继续播放 / 贴边小窗息屏后继续播放」在 C17 上不生效
        //   （ROM 的 pause/stop 路径变了），这两个功能的 C17 实现单独放
        //   DockKeepC17.java；下面这套 C16 已验证过的 hook 一个字都不动。
        if (com.oupao.wiz.Cfg.isC17()) {
            try {
                com.oupao.wiz.DockKeepC17.install(p1);
            } catch (Throwable t) {
                com.oupao.wiz.SLog.e("Dock: DockKeepC17 安装异常", t);
            }
            try {
                com.oupao.wiz.Diag.flush();
            } catch (Throwable __e_io17) {
            }
            com.oupao.wiz.SLog.i("Dock: C17 分支（DockKeepC17 已安装）");
            return;
        }
        com.oupao.wiz.Dock.hookFloat(p1);
        com.oupao.wiz.Dock.hookHandle(p1);
        com.oupao.wiz.Dock.hookSleep(p1);
        try {
            com.oupao.wiz.Diag.flush();
            com.oupao.wiz.SLog.i("Dock: \u5b89\u88c5\u6d41\u7a0b\u7ed3\u675f");
        } catch (Throwable __e_io) {
        }
        return;
    }
    public static boolean isDocked(Object p3)
    {
        // 3377：C17 的贴边窗口由 DockKeepC17 维护。
        //   C16 这里依赖 Hooks.sTask（最近一个小窗的 caption）；C17 贴边后
        //   caption 可能被释放，sTask 变 null 会把状态误清掉。C17 只认
        //   Dock.sTask + taskId 是否还存在，见 DockKeepC17.isDocked。
        if (com.oupao.wiz.Cfg.isC17()) {
            return com.oupao.wiz.DockKeepC17.isDocked(p3);
        }
        int v0_0 = 0;
        try {
            if (com.oupao.wiz.Win.sameTask(com.oupao.wiz.Dock.sTask, p3)) {
                if (com.oupao.wiz.Win.sameTask(com.oupao.wiz.Hooks.task(), p3)) {
                    com.oupao.wiz.Dock.sMiss = 0;
                    v0_0 = 1;
                } else {
                    var v1_3 = (com.oupao.wiz.Dock.sMiss + 1);
                    com.oupao.wiz.Dock.sMiss = v1_3;
                    if (v1_3 <= 20) {
                        v0_0 = 1;
                    } else {
                        com.oupao.wiz.SLog.i("DockKeep \u81ea\u52a8\u6e05\u9664(\u7a97\u53e3\u5df2\u4e0d\u5728)");
                        com.oupao.wiz.Dock.clear("DockKeep \u81ea\u52a8\u6e05\u9664(\u7a97\u53e3\u5df2\u4e0d\u5728)");
                    }
                }
            } else {
                com.oupao.wiz.Dock.sMiss = 0;
                // 注意：这里**不要**再加"边缘浮动小窗也算贴边"的兜底（1955 试过，已回退）。
                // 原因：兜底会让更多窗口被当成贴边 → 上滑时系统要暂停它、被我们挡下 →
                // 小窗一直 RESUMED，系统无法完成多任务过渡，表现为"上滑手势失效、直接回桌面"。
            }
        } catch (Throwable __e___ex) {
            return false;
        }
        return v0_0 != 0;
    }
    public static void mark(Object p3)
    {
        com.oupao.wiz.Dock.sMiss = 0;
        com.oupao.wiz.Dock.markedAt = android.os.SystemClock.uptimeMillis();   // 3260
        com.oupao.wiz.Dock.sTask = p3;
        com.oupao.wiz.Dock.sPkg = com.oupao.wiz.Win.taskPkg(p3);
        com.oupao.wiz.Dock.sId = com.oupao.wiz.Win.taskId(p3);
        com.oupao.wiz.SLog.i(new StringBuilder("DockKeep \u8bb0\u5f55: ").append(com.oupao.wiz.Dock.info()).toString());
        return;
    }
    /**
     * 3379：C17 的两个开关必须独立生效。
     *
     * 旧逻辑 `dockKeep() || (screenKeep() && screenOff())`：
     *   只开「小窗贴边时继续播放」时，息屏也会被 dockKeep 挡住暂停，
     *   导致「贴边小窗息屏后继续播放」开关关掉仍生效。
     *
     * C17：
     *   - 非息屏：只看 dockKeep（小窗贴边）
     *   - 息屏：只看 screenKeep（贴边小窗息屏）
     * C16 保持原逻辑不变。
     */
    static boolean keepOn()
    {
        if (com.oupao.wiz.Cfg.isC17()) {
            if (com.oupao.wiz.Dock.screenOff()) {
                return com.oupao.wiz.Cfg.screenKeep();
            }
            return com.oupao.wiz.Cfg.dockKeep();
        }
        return com.oupao.wiz.Cfg.dockKeep()
                || ((com.oupao.wiz.Cfg.screenKeep()) && (com.oupao.wiz.Dock.screenOff()));
    }
    public static boolean screenOff()
    {
        int v0 = 0;
        var v1_2 = com.oupao.wiz.Ctx.get();
        if (v1_2 != null) {
            var v1_3 = v1_2.getSystemService("power");
            if ((v1_3 != null) && (Boolean.FALSE.equals(com.oupao.wiz.Rt.callNoArg(v1_3, "isInteractive")))) {
                v0 = 1;
            }
        }
        return v0 != 0;
    }
}