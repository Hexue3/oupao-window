package com.oupao.wiz;
public final class UiInit extends com.oupao.wiz.XMH {
    private static boolean installed;
    private static boolean started;
    private boolean instrumentation;
    private UiInit(boolean p1)
    {
        this.instrumentation = p1;
        return;
    }
    public static synchronized void install(ClassLoader p6)
    {
        java.lang.reflect.Method v6_2 = null;
        java.lang.Class v6_1 = null;
        try {
            if (!com.oupao.wiz.UiInit.installed) {
                com.oupao.wiz.Diag.put("FlyUiReady", "0");
                try {
                    v6_1 = com.oupao.wiz.Rt.cls(p6, "com.android.systemui.SystemUIApplication");
                } catch (Throwable __e_v6_6) {
                    com.oupao.wiz.SLog.e("FlyS: SystemUI \u539f\u5165\u53e3\u5b89\u88c5\u5931\u8d25", __e_v6_6);
                }
                if (v6_1 != null) {
                    v6_2 = com.oupao.wiz.Rt.methodByName(v6_1, "onCreate", 0);
                } else {
                    v6_2 = null;
                }
                if ((v6_2 == null) || (com.oupao.wiz.XBridge.hookMethod(v6_2, new com.oupao.wiz.UiInit(false)) == null)) {
                    try {
                        if ((!com.oupao.wiz.UiInit.installed) && (com.oupao.wiz.XBridge.hookMethod(android.app.Instrumentation.class.getDeclaredMethod("callApplicationOnCreate", new Class[] {android.app.Application.class}), new com.oupao.wiz.UiInit(true)) != null)) {
                            com.oupao.wiz.UiInit.installed = true;
                            com.oupao.wiz.Diag.put("FlyUiEntry", "Instrumentation.callApplicationOnCreate");
                            com.oupao.wiz.SLog.i("FlyS: SystemUI \u539f\u5165\u53e3\u672a\u5339\u914d\uff0c\u5df2\u6302 Application \u751f\u547d\u5468\u671f\u56de\u9000");
                        }
                    } catch (Throwable __e_v6_13) {
                        com.oupao.wiz.SLog.e("FlyS: SystemUI \u56de\u9000\u5165\u53e3\u5b89\u88c5\u5931\u8d25", __e_v6_13);
                    }
                    if (!com.oupao.wiz.UiInit.installed) {
                        com.oupao.wiz.Diag.put("FlyUiEntry", "failed");
                        com.oupao.wiz.SLog.i("FlyS: SystemUI \u5165\u53e3\u672a\u5b89\u88c5");
                    }
                    try {
                        Class[] v4_8 = new Class[0];
                        Object[] v2_5 = new Object[0];
                        com.oupao.wiz.UiInit.start(Class.forName("android.app.ActivityThread").getDeclaredMethod("currentApplication", v4_8).invoke(0, v2_5));
                    } catch (Throwable __e_v6_21) {
                        com.oupao.wiz.SLog.e("FlyS: \u67e5\u8be2\u5f53\u524d SystemUI Application \u5931\u8d25", __e_v6_21);
                    }
                    return;
                } else {
                    com.oupao.wiz.UiInit.installed = true;
                    com.oupao.wiz.Diag.put("FlyUiEntry", "SystemUIApplication.onCreate");
                    com.oupao.wiz.SLog.i("FlyS: SystemUI onCreate \u94a9\u5b50\u5df2\u6302");
                }
            } else {
                return;
            }
        } catch (Throwable __e_v6_22) {
            throw __e_v6_22;
        }
    }
    private static synchronized void start(Object p3)
    {
        try {
            if ((!com.oupao.wiz.UiInit.started) && ((p3 instanceof android.content.Context))) {
                try {
                    if ("com.android.systemui".equals(((android.content.Context) p3).getPackageName())) {
                        if (android.os.Looper.getMainLooper() != null) {
                            if ((((android.content.Context) p3).getSystemService("window") instanceof android.view.WindowManager)) {
                                com.oupao.wiz.FlyS.start(((android.content.Context) p3));
                                com.oupao.wiz.UiInit.started = true;
                                com.oupao.wiz.Diag.put("FlyUiReady", "1");
                                com.oupao.wiz.SLog.i(new StringBuilder().append("FlyS: SystemUI \u521d\u59cb\u5316\u5165\u53e3\u5b8c\u6210\uff0cApplication=").append(((android.content.Context) p3).getClass().getName()).toString());
                            } else {
                            }
                        }
                        com.oupao.wiz.SLog.i("FlyS: SystemUI Context/\u4e3b\u7ebf\u7a0b\u5c1a\u672a\u5c31\u7eea\uff0c\u7b49\u5f85\u521b\u5efa\u56de\u8c03");
                        return;
                    } else {
                        return;
                    }
                } catch (Throwable __e_v3_5) {
                    com.oupao.wiz.SLog.e("FlyS: SystemUI \u521d\u59cb\u5316\u5931\u8d25", __e_v3_5);
                }
                return;
            } else {
                return;
            }
        } catch (Throwable __e_v3_6) {
            throw __e_v3_6;
        }
    }
    public void afterHookedMethod(com.oupao.wiz.XMP p3)
    {
        if (p3 != null) {
            java.lang.Object v0_0 = p3.thisObject;
            if (this.instrumentation) {
                if ((p3.args != null) && (p3.args.length != 0)) {
                    v0_0 = p3.args[0];
                } else {
                    return;
                }
            }
            com.oupao.wiz.UiInit.start(v0_0);
            return;
        } else {
            return;
        }
    }
}