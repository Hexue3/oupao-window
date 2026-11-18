package com.oupao.wiz;
public class Cap {
    private static boolean sCornerLogged;
    static int sHit;
    private static boolean sLogged;

    public Cap()
    {
        return;
    }
    public static void cornerOff(Object p6)
    {
        try {
            if ((com.oupao.wiz.Cfg.cornerOff()) && (p6 != null)) {
                var v0_5 = com.oupao.wiz.Rt.callLoose(p6, "cornerParams", null);
                if (v0_5 != null) {
                    Object[] v1_0 = new Object[1];
                    v1_0[0] = Float.valueOf(0);
                    com.oupao.wiz.Rt.callLoose(v0_5, "eColorA", v1_0);
                    com.oupao.wiz.Rt.callLoose(v0_5, "alphaMultiplier", v1_0);
                    if (!com.oupao.wiz.Cap.sCornerLogged) {
                        com.oupao.wiz.Cap.sCornerLogged = true;
                        com.oupao.wiz.SLog.i("Cap: \u5df2\u6e05\u96f6\u5c0f\u7a97\u89d2\u7f29\u653e\u624b\u67c4");
                    }
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    private static void hideOne(android.view.View p2, int p3)
    {
        var v0 = p2.findViewById(p3);
        if (v0 != null) {
            v0.setAlpha(0);
            v0.setVisibility(4);
            com.oupao.wiz.Cap.sHit = (com.oupao.wiz.Cap.sHit + 1);
        }
        return;
    }
    public static void hideTop(Object p6)
    {
        try {
            if ((com.oupao.wiz.Cfg.capClean()) && ((p6 instanceof android.view.View))) {
                com.oupao.wiz.Cap.sHit = 0;
                com.oupao.wiz.Cap.hideOne(((android.view.View) p6), 201458095);
                com.oupao.wiz.Cap.hideOne(((android.view.View) p6), 201458127);
                com.oupao.wiz.Cap.hideOne(((android.view.View) p6), 201458111);
                com.oupao.wiz.Cap.hideOne(((android.view.View) p6), 201458191);
                com.oupao.wiz.Cap.hideOne(((android.view.View) p6), 201458207);
                com.oupao.wiz.Cap.hideOne(((android.view.View) p6), 201458223);
                com.oupao.wiz.Cap.hideOne(((android.view.View) p6), 201458031);
                com.oupao.wiz.Cap.hideOne(((android.view.View) p6), 201458015);
                com.oupao.wiz.Cap.hideOne(((android.view.View) p6), 201458047);
                com.oupao.wiz.Cap.hideOne(((android.view.View) p6), 201458063);
                com.oupao.wiz.Cap.hideOne(((android.view.View) p6), 201458079);
                if (!com.oupao.wiz.Cap.sLogged) {
                    com.oupao.wiz.Cap.sLogged = true;
                    com.oupao.wiz.SLog.i("Cap: \u5df2\u9690\u85cf\u5c0f\u7a97\u9876\u90e8\u628a\u624b/\u4e09\u4e2a\u70b9");
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static void install(ClassLoader p6)
    {
        // 段1 smali :try_start_0/:try_end_20 —— 角手柄参数（Hook 11 → onCornerSetup）
        try {
            java.lang.Class c = com.oupao.wiz.Rt.cls(p6, "com.android.server.wm.FlexibleTaskIndicatorWindowManager");
            if (c != null) {
                var m = com.oupao.wiz.Rt.methodByName(c, "setupCornerParams", 1);
                if (m != null) {
                    com.oupao.wiz.XBridge.hookMethod(m, new com.oupao.wiz.Hook(11));
                    com.oupao.wiz.SLog.i("Cap: setupCornerParams 已挂钩");
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Cap: 角手柄挂钩异常", t);
        }

        // 段2 smali :try_start_27/:try_end_5f —— 角参数 alpha 复位（Hook 12/13 → onCornerAlpha）
        try {
            java.lang.Class c = com.oupao.wiz.Rt.cls(p6,
                    "com.android.server.wm.FlexibleTaskIndicatorWindowManager$IndicatorParamBuilder$CornerParams");
            if (c != null) {
                var m1 = com.oupao.wiz.Rt.methodByName(c, "alphaMultiplier", 1);
                if (m1 != null) {
                    com.oupao.wiz.XBridge.hookMethod(m1, new com.oupao.wiz.Hook(12));
                    com.oupao.wiz.SLog.i("Cap: alphaMultiplier 已挂钩");
                }
                var m2 = com.oupao.wiz.Rt.methodByName(c, "eColorA", 1);
                if (m2 != null) {
                    com.oupao.wiz.XBridge.hookMethod(m2, new com.oupao.wiz.Hook(13));
                    com.oupao.wiz.SLog.i("Cap: eColorA 已挂钩");
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Cap: 角参数挂钩异常", t);
        }

        // 段3 smali :try_start_66/:try_end_146 —— 顶部把手/控制层可见性（Hook 9/10/14）
        try {
            java.lang.Class c = com.oupao.wiz.Rt.cls(p6, "com.android.server.wm.FlexibleCaptionView");
            if (c != null) {
                var m0 = com.oupao.wiz.Rt.methodByName(c, "updateFlexibleTaskCaptionVisible", 2);
                if (m0 != null) {
                    com.oupao.wiz.XBridge.hookMethod(m0, new com.oupao.wiz.Hook(9));
                    com.oupao.wiz.SLog.i("Cap: updateFlexibleTaskCaptionVisible 已挂钩");
                }
                var m1 = com.oupao.wiz.Rt.methodByName(c, "updateControlViewVisible", 1);
                if (m1 != null) {
                    com.oupao.wiz.XBridge.hookMethod(m1, new com.oupao.wiz.Hook(10));
                    com.oupao.wiz.SLog.i("Cap: updateControlViewVisible 已挂钩");
                    String[][] rest = {
                            {"updateControlEllipseAlpha", "0"}, {"onLayout", "5"},
                            {"startTopBarAnimation", "0"}, {"updateThreeButtonVisibility", "1"},
                            {"onAttachedToWindow", "0"}, {"showCaptionViewWithAnim", "0"}
                    };
                    for (String[] r : rest) {
                        var mm = com.oupao.wiz.Rt.methodByName(c, r[0], Integer.parseInt(r[1]));
                        if (mm != null) {
                            com.oupao.wiz.XBridge.hookMethod(mm, new com.oupao.wiz.Hook(9));
                            com.oupao.wiz.SLog.i("Cap: " + r[0] + " 已挂钩");
                        }
                    }
                }
                var m2 = com.oupao.wiz.Rt.methodByName(c, "updateCaptionViewFocusChange", 1);
                if (m2 != null) {
                    com.oupao.wiz.XBridge.hookMethod(m2, new com.oupao.wiz.Hook(14));
                    com.oupao.wiz.SLog.i("Cap: updateCaptionViewFocusChange 已挂钩");
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Cap: 标题层挂钩异常", t);
        }

        // 段4 smali :try_start_14d/:try_end_16d —— 顶部单击（Hook 15）
        try {
            java.lang.Class c = com.oupao.wiz.Rt.cls(p6, "com.android.server.wm.FlexibleCaptionView");
            if (c != null) {
                var m = com.oupao.wiz.Rt.methodByName(c, "onSingleTapUp", 2);
                if (m != null) {
                    com.oupao.wiz.XBridge.hookMethod(m, new com.oupao.wiz.Hook(15));
                    com.oupao.wiz.SLog.i("Cap: onSingleTapUp(顶部点击) 已挂钩");
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Cap: 顶部点击挂钩异常", t);
        }

        // 段5 smali :try_start_174/:try_end_194 —— ★关闭角落拖拽缩放的关键 hook（Hook 16 → onCornerFlag）
        try {
            java.lang.Class c = com.oupao.wiz.Rt.cls(p6, "com.android.server.wm.FlexibleCaptionViewTouchRegionMgr");
            if (c != null) {
                var m = com.oupao.wiz.Rt.methodByName(c, "getFlexibleTaskActionFlag", 3);
                if (m != null) {
                    com.oupao.wiz.XBridge.hookMethod(m, new com.oupao.wiz.Hook(16));
                    com.oupao.wiz.SLog.i("Cap: getFlexibleTaskActionFlag(角缩放) 已挂钩");
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Cap: 角缩放判定挂钩异常", t);
        }

        // 段6 smali :try_start_19b/:try_end_1bb —— 小窗菜单（Hook 17）
        try {
            java.lang.Class c = com.oupao.wiz.Rt.cls(p6, "com.android.server.wm.FlexibleTaskController");
            if (c != null) {
                var m = com.oupao.wiz.Rt.methodByName(c, "setFlexibleMenuShowing", 1);
                if (m != null) {
                    com.oupao.wiz.XBridge.hookMethod(m, new com.oupao.wiz.Hook(17));
                    com.oupao.wiz.SLog.i("Cap: setFlexibleMenuShowing(小窗菜单) 已挂钩");
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Cap: 小窗菜单挂钩异常", t);
        }
    }
}
