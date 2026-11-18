package com.oupao.wiz;
public class Hooks$HUC extends com.oupao.wiz.XMH {
    public Hooks$HUC()
    {
        return;
    }
    public void afterHookedMethod(com.oupao.wiz.XMP p2)
    {
        try {
            // 构造完成时把 HeadsUpManagerImpl 实例存起来，供 NotifFly 收横幅用。
            // 用构造函数钩子而不是 showNotification，是为了「简洁横幅」那种
            // 不走 showNotification 的路径也能拿到实例。
            com.oupao.wiz.Hooks.setHuMgr(p2.thisObject);
            com.oupao.wiz.SLog.i("HU: HeadsUpManager 实例已捕获");
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
}
