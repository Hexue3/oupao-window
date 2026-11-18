package com.oupao.wiz;
public class FlyBar {
    static android.view.ViewGroup lastC;
    static int lastS;
    public static android.graphics.drawable.Drawable origBg;
    static java.util.ArrayList refs;
    public FlyBar()
    {
        return;
    }
    static void apply(android.view.View p10)
    {
        try {
            if (p10 == null) return;
            var v1 = p10.getRootView();
            if (v1 == null) return;
            var v3_0 = com.oupao.wiz.Rt.get(v1, "mFloatIconListAnimationLayout");
            if (!(v3_0 instanceof android.view.View)) return;
            var v3_1 = ((android.view.View) v3_0);
            var v4_0 = v3_1.getParent();
            if (!(v4_0 instanceof android.view.ViewGroup)) return;
            var v4_1 = ((android.view.ViewGroup) v4_0);
            com.oupao.wiz.FlyBar.remember(v4_1);

            int v6 = 0;
            if ((!com.oupao.wiz.Cfg.flyUi()) || (!com.oupao.wiz.Cfg.flyUiBar())) {
                // :cond_7a 开关关 → 还原
                com.oupao.wiz.FlyBar.lastS = 0;
                com.oupao.wiz.FlyBar.lastC = null;
                v3_1.setVisibility(0);
                com.oupao.wiz.FlyBar.exitBar(v4_1);
                v6 = -1;
            } else {
                var v5_2 = com.oupao.wiz.Rt.get(v3_1, "mState");
                if ((v5_2 instanceof Integer)) {
                    v6 = ((Integer) v5_2).intValue();
                }
                boolean enter;
                if ((v6 & 1) != 0) {
                    enter = true;                     // :goto_61 ENTER
                } else if ((v6 & 2) != 0) {
                    enter = false;                    // :goto_6e EXIT
                } else {
                    if (com.oupao.wiz.FlyBar.lastC == v4_1) return;   // :cond_48
                    int v8_2 = 0;
                    var v5_5 = com.oupao.wiz.Rt.get(v1, "mCurrentMode");
                    if ((v5_5 instanceof Integer)) {
                        v8_2 = ((Integer) v5_5).intValue();
                    }
                    enter = (v8_2 == 4);
                }
                if (enter) {
                    com.oupao.wiz.FlyBar.lastS = 1;
                    com.oupao.wiz.FlyBar.lastC = v4_1;
                    v3_1.setVisibility(4);
                    com.oupao.wiz.FlyBar.enterBar(v4_1);
                } else {
                    com.oupao.wiz.FlyBar.lastS = 0;
                    com.oupao.wiz.FlyBar.lastC = v4_1;
                    v3_1.setVisibility(0);
                    com.oupao.wiz.FlyBar.exitBar(v4_1);
                }
            }
            com.oupao.wiz.SLog.i("FlyBar ST=" + v6 + " -> " + com.oupao.wiz.FlyBar.lastS);
        } catch (Throwable __e_v0) {
        }
        return;
    }
    public static void enterBar(android.view.ViewGroup p3)
    {
        try {
            if (p3 != null) {
                if (com.oupao.wiz.FlyBar.origBg == null) {
                    com.oupao.wiz.FlyBar.origBg = p3.getBackground();
                }
                p3.setBackground(null);
                com.oupao.wiz.FlyLine.add(p3);
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static void exitBar(android.view.ViewGroup p2)
    {
        try {
            if (p2 != null) {
                com.oupao.wiz.FlyLine.remove(p2);
                var v0 = com.oupao.wiz.FlyBar.origBg;
                if (v0 != null) {
                    p2.setBackground(v0);
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static void install(ClassLoader p7)
    {
        java.lang.Class v0_2 = null;
        try {
            com.oupao.wiz.SLog.i("FlyBar install ENTER");
            v0_2 = com.oupao.wiz.Rt.cls(p7, "com.android.server.wm.floathandle.FloatHandleView");
        } catch (Throwable __e_v0_5) {
            com.oupao.wiz.SLog.e("FlyBar install EX", __e_v0_5);
            return;
        }
        if (v0_2 != null) {
            com.oupao.wiz.SLog.i("FlyBar FloatHandleView FOUND");
            var v1_0 = com.oupao.wiz.Rt.methodByName(v0_2, "getView", 0);
            if (v1_0 != null) {
                com.oupao.wiz.XBridge.hookMethod(v1_0, new com.oupao.wiz.FlyBar$C9());
                com.oupao.wiz.SLog.i("FlyBar PROBE getView hooked");
            }
        }
        if (v0_2 != null) {
            var v1_3 = com.oupao.wiz.Rt.methodByName(v0_2, "updateForMultiIconReduce", 2);
            if (v1_3 != null) {
                com.oupao.wiz.XBridge.hookMethod(v1_3, new com.oupao.wiz.FlyBar$C10());
                com.oupao.wiz.SLog.i("FlyBar HOOK reduce = OK");
            }
        }
        if (v0_2 != null) {
            var v1_6 = com.oupao.wiz.Rt.methodByName(v0_2, "setCurrentMode", 1);
            if (v1_6 != null) {
                com.oupao.wiz.XBridge.hookMethod(v1_6, new com.oupao.wiz.FlyBar$C11());
                com.oupao.wiz.SLog.i("FlyBar HOOK mode = OK");
            }
        }
        if (v0_2 != null) {
            var v1_9 = com.oupao.wiz.Rt.methodByName(v0_2, "refreshFloatHandleView", 2);
            if (v1_9 != null) {
                java.lang.String v3_0 = null;
                if (com.oupao.wiz.XBridge.hookMethod(v1_9, new com.oupao.wiz.FlyBar$C3()) == null) {
                    v3_0 = "FlyBar HOOK refresh = NULL";
                } else {
                    v3_0 = "FlyBar HOOK refresh = OK";
                }
                com.oupao.wiz.SLog.i(v3_0);
            }
        }
        var v0_4 = com.oupao.wiz.Rt.cls(p7, "com.android.server.wm.floathandle.FloatIconListAnimationLayout");
        if (v0_4 != null) {
            var v1_12 = com.oupao.wiz.Rt.methodByName(v0_4, "animateToStacking", 0);
            if (v1_12 != null) {
                java.lang.String v3_1 = null;
                if (com.oupao.wiz.XBridge.hookMethod(v1_12, new com.oupao.wiz.FlyBar$C1()) == null) {
                    v3_1 = "FlyBar HOOK stacking = NULL";
                } else {
                    v3_1 = "FlyBar HOOK stacking = OK";
                }
                com.oupao.wiz.SLog.i(v3_1);
            }
            var v1_14 = com.oupao.wiz.Rt.methodByName(v0_4, "animateToExpand", 0);
            if (v1_14 != null) {
                java.lang.String v3_2 = null;
                if (com.oupao.wiz.XBridge.hookMethod(v1_14, new com.oupao.wiz.FlyBar$C2()) == null) {
                    v3_2 = "FlyBar HOOK expand = NULL";
                } else {
                    v3_2 = "FlyBar HOOK expand = OK";
                }
                com.oupao.wiz.SLog.i(v3_2);
            }
        }
        return;
    }
    public static void refreshAll()
    {
        java.util.ArrayList v0 = null;
        try {
            v0 = com.oupao.wiz.FlyBar.refs;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 != null) {
            int v1 = 0;
            while (v1 < v0.size()) {
                var v4_0 = ((java.lang.ref.WeakReference) v0.get(v1)).get();
                if (v4_0 != null) {
                    com.oupao.wiz.FlyBar.apply(((android.view.ViewGroup) v4_0));
                }
                v1++;
            }
        }
        return;
    }
    static void remember(android.view.ViewGroup p5)
    {
        // 2906fix：原来写成「refs 非空就换成新空表」，导致列表永远只有一个元素
        // → refreshAll() 只处理最后一个容器，多个窗口/实例下开关只有最后一个生效。
        // 顺带把可能死循环的 while 换成倒序遍历 + 去重 + 清弱引用。
        try {
            if (p5 == null) {
                return;
            }
            if (com.oupao.wiz.FlyBar.refs == null) {
                com.oupao.wiz.FlyBar.refs = new java.util.ArrayList();
            }
            java.util.ArrayList list = com.oupao.wiz.FlyBar.refs;
            for (int i = list.size() - 1; i >= 0; i--) {
                Object o = ((java.lang.ref.WeakReference) list.get(i)).get();
                if (o == null) {
                    list.remove(i);
                    continue;
                }
                if (o == p5) {
                    return;
                }
            }
            list.add(new java.lang.ref.WeakReference(p5));
        } catch (Throwable ignore) {
        }
        return;
    }

    public static void sync(android.view.View p1)
    {
        com.oupao.wiz.FlyBar.apply(p1);
        return;
    }
}
