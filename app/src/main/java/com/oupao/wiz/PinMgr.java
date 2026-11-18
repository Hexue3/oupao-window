package com.oupao.wiz;
public class PinMgr implements android.view.View.OnClickListener {
    public PinMgr()
    {
        return;
    }
    public void onClick(android.view.View p3)
    {
        int v0_1 = 0;
        try {
            if (com.oupao.wiz.Pins.appMgr) {
                v0_1 = 0;
            } else {
                v0_1 = 1;
            }
        } catch (Throwable __e_v0) {
            return;
        }
        com.oupao.wiz.Pins.appMgr = v0_1 != 0;
        // 第 10 页已删除，重建当前页就是 build()（内部只走 desk 构建）
        com.oupao.wiz.Pins.build();
        return;
    }
}
