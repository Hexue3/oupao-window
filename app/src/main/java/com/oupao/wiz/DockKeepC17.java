package com.oupao.wiz;

/**
 * 3377：C17 专用的「小窗贴边时继续播放 / 贴边小窗息屏后继续播放」。
 *
 * ## C16 与 C17 的差异（反编译 C17 services.jar / oplus-services.jar 实证）
 *
 * C16（SDK 36）的贴边/息屏暂停链路：
 *   - 贴边动画结束：`ExitFlexibleTaskToFloatStrategy.startToFloatAnimation()`
 *   - ActivityRecord.makeInvisible() → `TaskFragment.startPausing(Z, ActivityRecord, String)`
 *     → 4 参数重载
 *   - 息屏：`Task.goToSleepIfPossible` → `TaskFragment.sleepIfPossible`
 *     → `startPausing(Z, ActivityRecord, String)`
 *   - stop：`ActivityRecord.stopIfPossible()`
 *   所以 C16 的 Dock.java 挂 3 参数 / 4 参数 startPausing + stopIfPossible 就够了。
 *
 * C17（SDK 37，Android 17）新增了一个 **5 参数** 重载：
 *   `TaskFragment.startPausing(ZZ, ActivityRecord, ActivityRecord, String)`
 *   并且 `ActivityRecord.makeInvisible()` 在 RESUMED 状态下会先走
 *   `IActivityRecordExt.pauseSelfIfInvisibleResumedInFlexibleTask(TaskFragment, ActivityRecord)`，
 *   该方法 **直接调用 5 参数重载**（reason = "makeInvisible-invisibleResumed"），
 *   不再经过 C16 已挂的 3/4 参数入口 → 贴边时视频仍被 pause。
 *   5 参数重载是完整实现，不会回调 4 参数，所以 C16 的钩子拦不住。
 *
 * 另外 C17 的 `ActivityRecordExtImpl.pauseFlexibleResumedActivityIfNeeded`
 * 由 `ActivityRecord.addToStopping()` 调用（息屏/stop 链路），走 3 参数入口，
 * C16 的 3 参数钩子可以继续覆盖。
 *
 * ## 本文件的实现
 *   1. 复用 C16 已验证的标记/暂停 hook：`Dock.installC16Hooks()`；
 *   2. 新增 C17 的 5 参数 `startPausing` 钩子（贴边 makeInvisible 的真正入口）；
 *   3. 新增 `FloatHandleController.addFloatHandle` 钩子，按 FloatHandleInfo.taskId
 *      记录贴边任务（比只依赖 startToFloatAnimation 更早、更稳）；
 *   4. `isDocked()` 只认 `Dock.sTask` + taskId 是否还存在，不依赖
 *      `Hooks.sTask`（C17 贴边后 caption 可能释放 → 旧判断会误清）。
 */
public final class DockKeepC17 {

    /** C17 的 ActivityRecord class（解锁恢复时反射 resumeTopActivityInnerLocked 用） */
    private static volatile Class<?> sActCls;

    private DockKeepC17() {
    }

    /**
     * C17 的“当前贴边任务”判断。
     *
     * 不能用 `Hooks.task()`：C17 贴边后 caption 可能被释放，Hooks.sTask 会变 null。
     * 这里用 Dock.mark 记录的 task + taskId 查找，任务真的被移除才清除。
     */
    static boolean isDocked(Object task) {
        try {
            if (task == null || com.oupao.wiz.Dock.sTask == null) {
                return false;
            }
            if (!com.oupao.wiz.Win.sameTask(com.oupao.wiz.Dock.sTask, task)) {
                com.oupao.wiz.Dock.sMiss = 0;
                return false;
            }
            // 贴边窗口虽然不可见，但 Task 必须还在任务栈里；否则是旧状态。
            if (com.oupao.wiz.Dock.sId != 0
                    && com.oupao.wiz.FlyH.taskById(com.oupao.wiz.Dock.sId) == null) {
                com.oupao.wiz.SLog.i("DockKeepC17 自动清除(窗口已不在)");
                com.oupao.wiz.Dock.clear("DockKeepC17 自动清除(窗口已不在)");
                return false;
            }
            com.oupao.wiz.Dock.sMiss = 0;
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 安装 C17 版的「贴边保活 / 息屏保活」。
     *
     * @return 本函数实际挂上的 hook 数
     */
    static int install(ClassLoader cl) {
        int before = com.oupao.wiz.XBridge.getHookCount();

        // 1) C16 已验证的 3/4 参数 startPausing、stopIfPossible、
        //    以及 startToFloatAnimation / onFloatOpenAnimationStartInner 标记。
        try {
            com.oupao.wiz.Dock.installC16Hooks(cl);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("DockKeepC17: 安装 C16 基础 hook 异常", t);
        }

        // 2) C17 新增：5 参数 startPausing（makeInvisible 直调，C16 的 3/4 参数覆盖不到）
        hookPause5(cl);

        // 3) C17 贴边浮标出现时记录任务（补充 startToFloatAnimation 的时机）
        hookAddFloatHandle(cl);
        hookCreateFloatHandleInfo(cl);

        // 4) 解锁恢复：息屏时按开关暂停了贴边任务，解锁后系统不会自动恢复
        //    （浮标不是前台任务），这里在解锁/亮屏后手动 resume。
        hookUnlock(cl);
        hookKeyguard(cl);
        hookScreenAwake(cl);

        int n = com.oupao.wiz.XBridge.getHookCount() - before;
        com.oupao.wiz.SLog.i("DockKeepC17: 安装完成 hooks=" + n);
        return n;
    }

    /** C17：FloatHandleController.createFloatHandleInfo(FlexibleTaskInfo, int, int, Task) */
    private static boolean hookCreateFloatHandleInfo(ClassLoader cl) {
        try {
            Class<?> fhc = com.oupao.wiz.Rt.cls(cl, "com.android.server.wm.FloatHandleController");
            Class<?> fti = com.oupao.wiz.Rt.cls(cl, "com.android.server.wm.FlexibleTaskInfo");
            Class<?> task = com.oupao.wiz.Rt.cls(cl, "com.android.server.wm.Task");
            if (fhc == null || fti == null || task == null) {
                com.oupao.wiz.SLog.i("DockKeepC17: createFloatHandleInfo 依赖类缺失");
                return false;
            }
            java.lang.reflect.Method m = com.oupao.wiz.Rt.method(fhc, "createFloatHandleInfo",
                    new Class[]{fti, Integer.TYPE, Integer.TYPE, task});
            if (m == null) {
                com.oupao.wiz.SLog.i("DockKeepC17: 未找到 createFloatHandleInfo");
                return false;
            }
            com.oupao.wiz.XBridge.hookMethod(m, new com.oupao.wiz.DockKeepC17$T());
            com.oupao.wiz.SLog.i("DockKeepC17: 已挂 createFloatHandleInfo");
            return true;
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("DockKeepC17: hookCreateFloatHandleInfo 异常", t);
            return false;
        }
    }

    /** C17：解锁后恢复贴边任务（keyguardGoingAway 回调 → 延迟 resume） */
    private static boolean hookUnlock(ClassLoader cl) {
        try {
            sActCls = com.oupao.wiz.Rt.cls(cl, "com.android.server.wm.ActivityRecord");
            Class<?> atms = com.oupao.wiz.Rt.cls(cl, "com.android.server.wm.ActivityTaskManagerService");
            if (atms == null) {
                com.oupao.wiz.SLog.i("DockKeepC17: ActivityTaskManagerService 缺失");
                return false;
            }
            java.lang.reflect.Method m = com.oupao.wiz.Rt.method(atms, "keyguardGoingAway",
                    new Class[]{Integer.TYPE});
            if (m == null) {
                com.oupao.wiz.SLog.i("DockKeepC17: 未找到 keyguardGoingAway");
                return false;
            }
            com.oupao.wiz.XBridge.hookMethod(m, new com.oupao.wiz.DockKeepC17$K());
            com.oupao.wiz.SLog.i("DockKeepC17: 已挂 keyguardGoingAway(解锁恢复)");
            return true;
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("DockKeepC17: hookUnlock 异常", t);
            return false;
        }
    }

    /** C17：OPLUS 自己的锁屏状态回调（keyguardShowing=false 即解锁） */
    private static boolean hookKeyguard(ClassLoader cl) {
        try {
            Class<?> ftc = com.oupao.wiz.Rt.cls(cl, "com.android.server.wm.FlexibleTaskController");
            if (ftc == null) {
                com.oupao.wiz.SLog.i("DockKeepC17: FlexibleTaskController 缺失");
                return false;
            }
            java.lang.reflect.Method m = com.oupao.wiz.Rt.method(ftc, "notifyKeyguardStateChanged",
                    new Class[]{Boolean.TYPE, Boolean.TYPE, Integer.TYPE});
            if (m == null) {
                com.oupao.wiz.SLog.i("DockKeepC17: 未找到 notifyKeyguardStateChanged");
                return false;
            }
            com.oupao.wiz.XBridge.hookMethod(m, new com.oupao.wiz.DockKeepC17$K2());
            com.oupao.wiz.SLog.i("DockKeepC17: 已挂 notifyKeyguardStateChanged(解锁恢复)");
            return true;
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("DockKeepC17: hookKeyguard 异常", t);
            return false;
        }
    }

    /** C17：亮屏回调（无锁屏时靠这条） */
    private static boolean hookScreenAwake(ClassLoader cl) {
        try {
            Class<?> atms = com.oupao.wiz.Rt.cls(cl, "com.android.server.wm.ActivityTaskManagerService");
            if (atms == null) {
                return false;
            }
            java.lang.reflect.Method m = com.oupao.wiz.Rt.method(atms, "onScreenAwakeChanged",
                    new Class[]{Boolean.TYPE});
            if (m == null) {
                com.oupao.wiz.SLog.i("DockKeepC17: 未找到 onScreenAwakeChanged");
                return false;
            }
            com.oupao.wiz.XBridge.hookMethod(m, new com.oupao.wiz.DockKeepC17$S());
            com.oupao.wiz.SLog.i("DockKeepC17: 已挂 onScreenAwakeChanged(亮屏恢复)");
            return true;
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("DockKeepC17: hookScreenAwake 异常", t);
            return false;
        }
    }

    /** 解锁回调：只有「息屏时按开关暂停」的场景才需要恢复 */
    static void onUnlock() {
        try {
            if (!com.oupao.wiz.Cfg.isC17()) {
                return;
            }
            // screenKeep 开：息屏时没暂停（播放没断），不需要 resume
            if (com.oupao.wiz.Cfg.screenKeep()) {
                return;
            }
            if (keyguardLocked()) {
                com.oupao.wiz.SLog.i("DockKeepC17: onUnlock 亮屏但仍锁屏，等待解锁");
                return;
            }
            final int id = com.oupao.wiz.Dock.sId;
            com.oupao.wiz.SLog.i("DockKeepC17: onUnlock 触发 id=" + id
                    + " screenKeep=" + com.oupao.wiz.Cfg.screenKeep());
            if (id == 0) {
                return;
            }
            android.os.Handler h = com.oupao.wiz.Hooks.main();
            if (h == null) {
                return;
            }
            h.postDelayed(new Runnable() {
                public void run() {
                    resumeDocked(id);
                }
            }, 600L);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("DockKeepC17: onUnlock 异常", t);
        }
    }

    /** 当前是否仍锁屏（system_server 里通过 KeyguardManager 判断） */
    private static boolean keyguardLocked() {
        try {
            android.content.Context c = com.oupao.wiz.Ctx.get();
            if (c == null) {
                return false;
            }
            android.app.KeyguardManager km =
                    (android.app.KeyguardManager) c.getSystemService(android.content.Context.KEYGUARD_SERVICE);
            return km != null && km.isKeyguardLocked();
        } catch (Throwable t) {
            return false;
        }
    }

    /** 恢复暂停前的非全屏 bounds（贴边小窗尺寸/位置） */
    private static void restoreBounds(Object task) {
        try {
            Object b = com.oupao.wiz.Rt.get(task, "mLastNonFullscreenBounds");
            com.oupao.wiz.SLog.i("DockKeepC17: restoreBounds lastNonFull=" + b);
            if (b instanceof android.graphics.Rect) {
                com.oupao.wiz.Rt.callLoose(task, "resize",
                        new Object[]{b, Integer.valueOf(2), Boolean.FALSE});
                com.oupao.wiz.SLog.i("DockKeepC17: 已恢复 bounds=" + b);
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("DockKeepC17: restoreBounds 异常", t);
        }
    }

    /** 对贴边 Task 调 resumeTopActivityInnerLocked，把它从 PAUSED 拉回 RESUMED */
    private static void resumeDocked(int id) {
        try {
            Object task = com.oupao.wiz.FlyH.taskById(id);
            if (task == null) {
                com.oupao.wiz.Dock.clear("DockKeepC17 解锁时窗口已不在");
                return;
            }
            if (com.oupao.wiz.Dock.sTask != null
                    && !com.oupao.wiz.Win.sameTask(com.oupao.wiz.Dock.sTask, task)) {
                return;
            }
            Object ar = com.oupao.wiz.Win.taskAct(task);
            if (ar != null) {
                Object state = com.oupao.wiz.Rt.callNoArg(ar, "getState");
                if (state != null && "RESUMED".equals(state.toString())) {
                    com.oupao.wiz.SLog.i("DockKeepC17: 解锁恢复跳过(已 RESUMED) tid=" + id);
                    return;
                }
            }
            java.lang.reflect.Method m = com.oupao.wiz.Rt.method(task.getClass(),
                    "resumeTopActivityInnerLocked",
                    new Class[]{sActCls, android.app.ActivityOptions.class, Boolean.TYPE});
            Object ret = com.oupao.wiz.Rt.invoke(m, task,
                    new Object[]{null, null, Boolean.FALSE});
            restoreBounds(task);
            com.oupao.wiz.SLog.i("DockKeepC17: 解锁恢复 tid=" + id
                    + " ret=" + ret + " state=" + (ar == null ? "null"
                    : String.valueOf(com.oupao.wiz.Rt.callNoArg(ar, "getState"))));
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("DockKeepC17: 解锁恢复异常 tid=" + id, t);
        }
    }

    /** C17：`TaskFragment.startPausing(ZZ, ActivityRecord, ActivityRecord, String)Z` */
    private static boolean hookPause5(ClassLoader cl) {
        try {
            Class<?> tf = com.oupao.wiz.Rt.cls(cl, "com.android.server.wm.TaskFragment");
            Class<?> ar = com.oupao.wiz.Rt.cls(cl, "com.android.server.wm.ActivityRecord");
            if (tf == null || ar == null) {
                com.oupao.wiz.SLog.i("DockKeepC17: 5参数 startPausing 依赖类缺失");
                return false;
            }
            java.lang.reflect.Method m = com.oupao.wiz.Rt.method(tf, "startPausing",
                    new Class[]{Boolean.TYPE, Boolean.TYPE, ar, ar, String.class});
            if (m == null) {
                com.oupao.wiz.SLog.i("DockKeepC17: 未找到 5参数 startPausing");
                return false;
            }
            com.oupao.wiz.XBridge.hookMethod(m, new com.oupao.wiz.Dock$C7());
            com.oupao.wiz.SLog.i("DockKeepC17: 已挂 5参数 startPausing");
            return true;
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("DockKeepC17: hookPause5 异常", t);
            return false;
        }
    }

    /** C17：FloatHandleController.addFloatHandle(FloatHandleInfo, int, boolean) */
    private static boolean hookAddFloatHandle(ClassLoader cl) {
        try {
            Class<?> fhc = com.oupao.wiz.Rt.cls(cl, "com.android.server.wm.FloatHandleController");
            Class<?> info = com.oupao.wiz.Rt.cls(cl, "com.android.server.wm.floathandle.FloatHandleInfo");
            if (fhc == null || info == null) {
                com.oupao.wiz.SLog.i("DockKeepC17: FloatHandleController 依赖类缺失");
                return false;
            }
            java.lang.reflect.Method m = com.oupao.wiz.Rt.method(fhc, "addFloatHandle",
                    new Class[]{info, Integer.TYPE, Boolean.TYPE});
            if (m == null) {
                com.oupao.wiz.SLog.i("DockKeepC17: 未找到 addFloatHandle");
                return false;
            }
            com.oupao.wiz.XBridge.hookMethod(m, new com.oupao.wiz.DockKeepC17$H());
            com.oupao.wiz.SLog.i("DockKeepC17: 已挂 addFloatHandle");
            return true;
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("DockKeepC17: hookAddFloatHandle 异常", t);
            return false;
        }
    }
}
