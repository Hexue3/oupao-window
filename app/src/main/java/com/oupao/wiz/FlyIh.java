package com.oupao.wiz;
public class FlyIh implements java.lang.reflect.InvocationHandler {
    public FlyIh()
    {
        return;
    }
    public static Object def(Class p2)
    {
        try {
            if (p2 != null) {
                if (!p2.equals(Boolean.TYPE)) {
                    if (!p2.equals(Integer.TYPE)) {
                        if (!p2.equals(Long.TYPE)) {
                            if (!p2.equals(Float.TYPE)) {
                                if (p2.equals(Double.TYPE)) {
                                    return Double.valueOf(0);
                                }
                            } else {
                                return Float.valueOf(0);
                            }
                        } else {
                            return Long.valueOf(0);
                        }
                    } else {
                        return Integer.valueOf(0);
                    }
                } else {
                    return false;
                }
            }
        } catch (Throwable __e_v0) {
            return null;
        }
        return null;
    }
    public Object invoke(Object p5, java.lang.reflect.Method p6, Object[] p7)
    {
        java.lang.String v0_0 = null;
        java.lang.Class v1 = null;
        try {
            v0_0 = p6.getName();
            v1 = p6.getReturnType();
        } catch (Throwable __e_v0) {
            return null;
        }
        if ((v0_0.equals("onPointerEvent")) && ((p7 != null) && (p7.length > 0))) {
            var v2_1 = p7[0];
            if (v2_1 != null) {
                com.oupao.wiz.FlyH.onEvent(((android.view.MotionEvent) v2_1));
                return com.oupao.wiz.FlyIh.def(v1);
            }
        }
        if (!v0_0.equals("hashCode")) {
            if (!v0_0.equals("toString")) {
                if ((!v0_0.equals("equals")) || ((p7 == null) || ((p7.length <= 0) || (p7[0] != p5)))) {
                    return com.oupao.wiz.FlyIh.def(v1);
                } else {
                    return true;
                }
            } else {
                return "oupao-fly-pointer";
            }
        } else {
            return Integer.valueOf(System.identityHashCode(p5));
        }
    }
}