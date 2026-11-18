package com.oupao.wiz;

import java.lang.reflect.Method;

/**
 * 3303：**禁止 ROM 的「自动贴边」**，只保留手动贴边。
 *
 * ## 背景
 *
 * 系统默认：到上限时把上一个小窗转成"贴边"（细条浮在屏幕边），贴边满两个才关最旧的。
 * 用户要的是「到上限直接关最早的，不贴边」。
 *
 * 自动贴边只有一个入口：
 *
 * ```
 * FlexibleTaskController.determineFlexibleTaskToExit(...)
 *   └─ exitTaskToFloat(task)                       // sourceFrom = 8
 *        └─ new ExitFlexibleTaskToFloatStrategy(...) → exitFlexibleTaskWindowInnerLocked(...)
 *             → strategy.handleEvent()              ← 我们在这里拦
 * ```
 *
 * `handleEvent()` 返回 false 是 ROM 自己认可的结果（它内部就有一条
 * 「task is not flexible float window, should not goto float」也返回 false，
 * 调用方随后走 `resetWhenEndExit(...)` 收尾，窗口原样留着）。
 *
 * ## 3303 起这里**只拦不关**
 *
 * 上限收敛全部由 {@link ZoomGate} 负责（串行开窗 + 开窗前/后收敛）。
 * 这里再关窗只会和 ZoomGate 抢着关、把刚打开的那扇误关掉
 * （实测 12:55:57：ROM 点名要贴边的正是用户刚打开的那个小窗）。
 */
public final class NoDock {

    /** AbsFlexibleTaskExitStrategy.SOURCE_FROM_HANDLE_REPLACE_TO_FLOAT */
    private static final int SRC_REPLACE_TO_FLOAT = 8;

    private NoDock() {
    }

    public static void install(ClassLoader cl) {
        Class<?> c = com.oupao.wiz.Rt.cls(cl, "com.android.server.wm.ExitFlexibleTaskToFloatStrategy");
        if (c == null) {
            com.oupao.wiz.SLog.i("NoDock: 未找到 ExitFlexibleTaskToFloatStrategy");
            return;
        }
        Method m = com.oupao.wiz.Rt.methodByName(c, "handleEvent", 0);
        if (m == null) {
            com.oupao.wiz.SLog.i("NoDock: 未找到 handleEvent");
            return;
        }
        if (com.oupao.wiz.XBridge.hookMethod(m, new com.oupao.wiz.XMH() {
            @Override
            public void beforeHookedMethod(com.oupao.wiz.XMP p) {
                try {
                    if (!com.oupao.wiz.Cfg.zoomMaxSingle()) {
                        return;                            // 上限!=1 / 开关关着 → 完全原厂行为
                    }
                    Object src = com.oupao.wiz.Rt.callNoArg(p.thisObject, "getSourceFrom");
                    int s = (src instanceof Number) ? ((Number) src).intValue() : -1;
                    // 手动贴边的策略都会带「起始矩形」(setStartBounds)，自动的没有
                    Object sb = com.oupao.wiz.Rt.get(p.thisObject, "mStartBounds");
                    boolean hasStart = false;
                    if (sb instanceof android.graphics.Rect) {
                        hasStart = !((android.graphics.Rect) sb).isEmpty();
                    }
                    if ((s != SRC_REPLACE_TO_FLOAT) || hasStart) {
                        return;                            // 手动贴边 / 最小化 → 放行
                    }
                    // 让 ROM 认为「这次不贴边」，窗口原样保留；
                    // 上限由 ZoomGate 在开窗前后收敛。
                    p.result = Boolean.FALSE;
                    p.returnEarly = true;
                    com.oupao.wiz.SLog.i("NoDock: 中止自动贴边 tid="
                            + com.oupao.wiz.Win.taskId(com.oupao.wiz.Rt.get(p.thisObject, "mTask"))
                            + " pkg=" + com.oupao.wiz.Win.taskPkg(
                                    com.oupao.wiz.Rt.get(p.thisObject, "mTask")));
                } catch (Throwable t) {
                    com.oupao.wiz.SLog.e("NoDock: 拦截异常", t);
                }
            }
        }) != null) {
            com.oupao.wiz.SLog.i("NoDock: 已挂「禁止自动贴边」");
        } else {
            com.oupao.wiz.SLog.i("NoDock: handleEvent 挂钩失败");
        }
    }
}
