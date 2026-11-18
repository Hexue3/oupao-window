package com.oupao.wiz;
public class HookL extends com.oupao.wiz.XMH {
    private static long lastTs;
    private static String lastTxt;
    public HookL()
    {
        return;
    }
    public void beforeHookedMethod(com.oupao.wiz.XMP p11)
    {
        java.lang.Object[] v0_0 = null;
        try {
            v0_0 = p11.args;
        } catch (Throwable __e_v0) {
            return;
        }
        if ((v0_0 != null) && (v0_0.length > 0)) {
            var v0_4 = v0_0[0];
            if ((v0_4 instanceof android.content.ClipData)) {
                var v0_5 = ((android.content.ClipData) v0_4);
                if (v0_5.getItemCount() > 0) {
                    var v0_1 = v0_5.getItemAt(0);
                    if (v0_1 != null) {
                        var v0_2 = v0_1.getText();
                        if (v0_2 != null) {
                            var v0_3 = v0_2.toString();
                            var v2_1 = android.os.SystemClock.uptimeMillis();
                            var v1_2 = com.oupao.wiz.HookL.lastTxt;
                            if ((v1_2 == null) || ((!v1_2.equals(v0_3)) || ((v2_1 - com.oupao.wiz.HookL.lastTs) > 1000))) {
                                com.oupao.wiz.HookL.lastTxt = v0_3;
                                com.oupao.wiz.HookL.lastTs = v2_1;
                                var v1_5 = new StringBuilder();
                                v1_5.append("Clip: \u94a9\u5b50\u89e6\u53d1 len=");
                                v1_5.append(v0_3.length());
                                com.oupao.wiz.SLog.i(v1_5.toString());
                                // 闲鱼链接先交给 Idle（它认得 m.tb.cn 短链、会先解析真实地址）。
                                // 返回 true = 已接管，不再走 Sug 的通用判定，避免两边各判一次。
                                String src = null;
                                if ((v0_0.length > 1) && (v0_0[1] instanceof String)) {
                                    src = (String) v0_0[1];
                                }
                                if (com.oupao.wiz.Idle.onClip(v0_3, src)) {
                                    return;
                                }
                                com.oupao.wiz.Sug.onClip(v0_3);
                            }
                        }
                    }
                }
            }
        }
        return;
    }
}
