package com.oupao.wiz;
public class Side {
    public Side()
    {
        return;
    }
    public static void install(ClassLoader p4)
    {
        java.lang.Class v0_8 = null;
        try {
            com.oupao.wiz.SLog.i("Side: install \u8fdb\u5165");
            // 中转站呼出通道（扇形菜单 → 广播 → 这里）
            try {
                com.oupao.wiz.SideDock.install(p4);
            } catch (Throwable __e_dock) {
                com.oupao.wiz.SLog.e("Side: SideDock.install 异常", __e_dock);
            }
            v0_8 = com.oupao.wiz.Rt.cls(p4, "com.coloros.edgepanel.utils.EdgePanelUtils");
        } catch (Throwable __e_v0_7) {
            com.oupao.wiz.SLog.e("Side: install \u5f02\u5e38", __e_v0_7);
            return;
        }
        if (v0_8 == null) {
            com.oupao.wiz.SLog.i("Side: \u7c7b\u672a\u627e\u5230 EdgePanelUtils");
        } else {
            Class[] v1_7 = new Class[3];
            v1_7[0] = String.class;
            v1_7[1] = Integer.TYPE;
            v1_7[2] = android.os.Bundle.class;
            var v0_2 = com.oupao.wiz.Rt.method(v0_8, "isAlreadyOpenedInTop", v1_7);
            if (v0_2 == null) {
                com.oupao.wiz.SLog.i("Side: \u65b9\u6cd5\u672a\u627e\u5230 isAlreadyOpenedInTop");
            } else {
                com.oupao.wiz.SLog.i(new StringBuilder().append("Side: \u5df2\u6302\u94a9 isAlreadyOpenedInTop hook=").append(com.oupao.wiz.XBridge.hookMethod(v0_2, new com.oupao.wiz.Side$H())).toString());
            }
        }
        return;
    }
}