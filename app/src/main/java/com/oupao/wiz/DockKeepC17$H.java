package com.oupao.wiz;

/** 3377：C17 浮标出现 → 记录对应 Task 为“贴边”。 */
public class DockKeepC17$H extends com.oupao.wiz.XMH {

    public DockKeepC17$H() {
    }

    public void beforeHookedMethod(com.oupao.wiz.XMP p) {
        try {
            if (p.args == null || p.args.length < 1) {
                com.oupao.wiz.SLog.i("DockKeepC17: addFloatHandle hook args 为空");
                return;
            }
            Object info = p.args[0];
            Object id = com.oupao.wiz.Rt.get(info, "taskId");
            if (!(id instanceof Number)) {
                id = com.oupao.wiz.Rt.get(info, "mTaskId");
            }
            if (!(id instanceof Number)) {
                com.oupao.wiz.SLog.i("DockKeepC17: addFloatHandle 无 taskId cls="
                        + (info == null ? "null" : info.getClass().getName()));
                return;
            }
            int tid = ((Number) id).intValue();
            Object task = com.oupao.wiz.FlyH.taskById(tid);
            com.oupao.wiz.SLog.i("DockKeepC17: addFloatHandle hook tid=" + tid
                    + " task=" + (task != null));
            if (task != null) {
                com.oupao.wiz.Dock.mark(task);
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("DockKeepC17: addFloatHandle 回调异常", t);
        }
    }
}
