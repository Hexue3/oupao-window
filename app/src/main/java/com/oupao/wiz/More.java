package com.oupao.wiz;
public class More {
    public More()
    {
        return;
    }
    public static boolean fireTool(String p8)
    {
        int v0 = 0;
        android.content.Context v1 = null;
        try {
            v1 = com.oupao.wiz.Cfg.sysContext();
        } catch (Throwable __e_v7) {
            return v0 != 0;
        }
        if (v1 != null) {
            java.lang.ClassLoader v2 = com.oupao.wiz.Hooks.CL;
            if (v2 == null) {
                v2 = v1.getClassLoader();
            }
            var v3_0 = com.oupao.wiz.Rt.cls(v2, "com.oplus.smartsidebar.panelview.edgepanel.data.entrybeans.ToolEntryHelper");
            if (v3_0 != null) {
                java.lang.reflect.Method v4_1 = com.oupao.wiz.Rt.methodByName(v3_0, "getActiveInstance", 0);
                if (v4_1 == null) {
                    v4_1 = com.oupao.wiz.Rt.methodByName(v3_0, "createAndGetInstance", 0);
                }
                if (v4_1 != null) {
                    try {
                        var v6_0 = com.oupao.wiz.Rt.invoke(v4_1, null, null);
                        if (v6_0 != null) {
                            Class[] v7_0 = new Class[1];
                            v7_0[0] = String.class;
                            var v4_5 = com.oupao.wiz.Rt.method(v6_0.getClass(), "getToolByAlias", v7_0);
                            if (v4_5 != null) {
                                Object[] v7_1 = new Object[1];
                                v7_1[0] = p8;
                                try {
                                    var v6_1 = com.oupao.wiz.Rt.invoke(v4_5, v6_0, v7_1);
                                    if (v6_1 != null) {
                                        var v6_2 = v6_1.getClass();
                                        var v4_7 = com.oupao.wiz.Rt.methodByName(v6_2, "handle", 0);
                                        if (v4_7 != null) {
                                            try {
                                                com.oupao.wiz.Rt.invoke(v4_7, v6_2, null);
                                                var v4_9 = new StringBuilder();
                                                v4_9.append("More: tool fired alias=");
                                                v4_9.append(p8);
                                                com.oupao.wiz.SLog.i(v4_9.toString());
                                                v0 = 1;
                                            } catch (Throwable __e_io) {
                                            }
                                        }
                                    }
                                } catch (Throwable __e_io) {
                                }
                            }
                        }
                    } catch (Throwable __e_io) {
                    }
                }
            }
            return v0 != 0;
        } else {
            return false;
        }
    }
}