package com.oupao.wiz;
public class Hooks$HU extends com.oupao.wiz.XMH {
    public Hooks$HU()
    {
        return;
    }
    public void beforeHookedMethod(com.oupao.wiz.XMP p7)
    {
        java.lang.Object[] v0_0 = null;
        try {
            // 顺手抓住 HeadsUpManagerImpl 实例：NotifFly 收横幅时要用它
            com.oupao.wiz.Hooks.setHuMgr(p7.thisObject);
            v0_0 = p7.args;
        } catch (Throwable __e_v0) {
            return;
        }
        if ((v0_0 != null) && (v0_0.length > 0)) {
            var v0_1 = v0_0[0];
            if (v0_1 != null) {
                var v1_2 = com.oupao.wiz.Hooks.pkgOfEntry(v0_1);
                if (v1_2 != null) {
                    // 微信/QQ 用各自开关，其它应用用全局开关（见 Hooks.nohuKey）
                    var v4_0 = com.oupao.wiz.Hooks.nohuKey(v1_2);
                    var v2_4 = com.oupao.wiz.Cfg.probeSys(v4_0);
                    var v3 = com.oupao.wiz.Hooks.markOf(v0_1);
                    var v4_2 = new StringBuilder();
                    v4_2.append("HU: pkg=");
                    v4_2.append(v1_2);
                    v4_2.append(" key=");
                    v4_2.append(v4_0);
                    v4_2.append(" mark=");
                    v4_2.append(v3);
                    v4_2.append(" sys=");
                    v4_2.append(v2_4);
                    com.oupao.wiz.SLog.i(v4_2.toString());
                    // ★ 混合判定：开关读得到就以开关为准（修「关了还拦」），
                    //   读不到就退回标记（修「SystemUI 读不到文件 → 完全不拦」）。
                    if (com.oupao.wiz.Hooks.nohuShouldBlock(v0_1, v1_2)) {
                        p7.setResult(null);
                        com.oupao.wiz.SLog.i("HU: blocked heads-up");
                    }
                }
            }
        }
        return;
    }
}
