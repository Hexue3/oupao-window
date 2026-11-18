package com.oupao.wiz;
public class HookW extends com.oupao.wiz.XMH {
    public HookW()
    {
        return;
    }
    public void afterHookedMethod(com.oupao.wiz.XMP p7)
    {
        try {
            if (p7 != null) {
                android.content.Intent v0_0 = null;
                var v1_2 = p7.args;
                if (v1_2 != null) {
                    int v2_0 = 0;
                    while (v2_0 < v1_2.length) {
                        var v4 = v1_2[v2_0];
                        if (!(v4 instanceof android.content.Intent)) {
                            v2_0++;
                        } else {
                            v0_0 = ((android.content.Intent) v4);
                            break;
                        }
                    }
                }
                if (v0_0 == null) {
                    var v1_0 = p7.thisObject;
                    if (v1_0 == null) {
                    } else {
                        var v1_1 = com.oupao.wiz.Rt.get(v1_0, "mIntent");
                        if (!(v1_1 instanceof android.content.Intent)) {
                        } else {
                            v0_0 = ((android.content.Intent) v1_1);
                        }
                    }
                } else {
                }
                if (v0_0 != null) {
                    var v1_3 = com.oupao.wiz.Act.url(v0_0);
                    if (v1_3 == null) {
                        com.oupao.wiz.Act.probe(v0_0);
                    } else {
                        com.oupao.wiz.Act.on(v1_3);
                        return;
                    }
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
}