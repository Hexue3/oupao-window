package com.oupao.wiz;

/** 3378：C17 创建贴边浮标信息 → 直接拿第 4 个参数 Task 记录。 */
public class DockKeepC17$T extends com.oupao.wiz.XMH {

    public DockKeepC17$T() {
    }

    public void beforeHookedMethod(com.oupao.wiz.XMP p) {
        try {
            if (p.args == null || p.args.length < 4) {
                return;
            }
            Object task = p.args[3];
            if (task != null) {
                com.oupao.wiz.Dock.mark(task);
                com.oupao.wiz.SLog.i("DockKeepC17: createFloatHandleInfo 记录 tid="
                        + com.oupao.wiz.Win.taskId(task));
            }
        } catch (Throwable ignore) {
        }
    }
}
