package com.oupao.wiz;
public class MA$MC implements android.view.View.OnClickListener {
    private final android.app.Dialog d;
    public MA$MC(android.app.Dialog p1)
    {
        this.d = p1;
        return;
    }
    public void onClick(android.view.View p7)
    {
        android.content.Context v0 = null;
        try {
            // 弹窗必须用带窗口 token 的 Context（点击视图所在的 Activity）。
            // Cfg.ctx 是应用级 Context，Dialog.show() 会抛 BadTokenException。
            if (p7 != null) {
                v0 = p7.getContext();
            }
            if (v0 == null) {
                v0 = com.oupao.wiz.Cfg.ctx;
            }
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 == null) {
            return;
        }
        try {
            if ((v0 instanceof android.app.Activity) && (((android.app.Activity) v0).isFinishing())) {
                return;
            }
        } catch (Throwable ignore) {
        }
        // 直接弹出应用选择器（原来只能手输包名）；手输入作为选择器里的一个按钮保留
        com.oupao.wiz.MusPick.show(v0);
        return;
    }
}
