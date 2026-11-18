package com.oupao.wiz;

/** 通知被取消（cancel/remove）时回调：微信/QQ 进聊天页会 cancel 自己的通知，正好用来收气泡 */
public class HookNC extends com.oupao.wiz.XMH {
    public HookNC()
    {
        return;
    }
    public void beforeHookedMethod(com.oupao.wiz.XMP p1)
    {
        try {
            com.oupao.wiz.Hooks.onNotifCancel(p1);
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
}
