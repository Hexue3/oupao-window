package com.oupao.wiz;
public class CapR {
    static final int N = 8;
    static Object[] sCap;
    static int sLast;
    static android.graphics.Rect[] sRect;
    static Object[] sTask;
    public CapR()
    {
        return;
    }
    public static Object byTid(int p4)
    {
        com.oupao.wiz.CapR.init();
        int v0_0 = 0;
        while (v0_0 < 8) {
            java.lang.Object v1_2 = com.oupao.wiz.CapR.sCap[v0_0];
            if (v1_2 != null) {
                var v2_0 = com.oupao.wiz.CapR.sTask[v0_0];
                if ((v2_0 != null) && (com.oupao.wiz.Win.taskId(v2_0) == p4)) {
                    return v1_2;
                }
            }
            v0_0++;
        }
        return null;
    }
    private static int idx(Object p3)
    {
        com.oupao.wiz.CapR.init();
        int v0_0 = 0;
        while (v0_0 < 8) {
            if (com.oupao.wiz.CapR.sCap[v0_0] != p3) {
                v0_0++;
            } else {
                return v0_0;
            }
        }
        return -1;
    }
    private static void init()
    {
        if (com.oupao.wiz.CapR.sCap == null) {
            Object[] v0_3 = new Object[8];
            com.oupao.wiz.CapR.sCap = v0_3;
            Object[] v0_5 = new Object[8];
            com.oupao.wiz.CapR.sTask = v0_5;
            android.graphics.Rect[] v0_7 = new android.graphics.Rect[8];
            com.oupao.wiz.CapR.sRect = v0_7;
            com.oupao.wiz.CapR.sLast = -1;
        }
        return;
    }
    public static Object newestCap()
    {
        var v0_0 = com.oupao.wiz.CapR.newestIdx();
        if (v0_0 < 0) {
            return null;
        } else {
            return com.oupao.wiz.CapR.sCap[v0_0];
        }
    }
    public static int newestIdx()
    {
        com.oupao.wiz.CapR.init();
        int v0_0 = com.oupao.wiz.CapR.sLast;
        int v1 = 0;
        while (v1 < 8) {
            if (v0_0 < 0) {
                v0_0 = 7;
            }
            if (com.oupao.wiz.CapR.sCap[v0_0] == null) {
                v0_0--;
                v1++;
            } else {
                return v0_0;
            }
        }
        return -1;
    }
    public static android.graphics.Rect newestRect()
    {
        var v0_0 = com.oupao.wiz.CapR.newestIdx();
        if (v0_0 >= 0) {
            var v1_1 = com.oupao.wiz.CapR.sRect[v0_0];
            if ((v1_1 instanceof android.graphics.Rect)) {
                return ((android.graphics.Rect) v1_1);
            }
        }
        return null;
    }
    public static Object newestTask()
    {
        var v0_0 = com.oupao.wiz.CapR.newestIdx();
        if (v0_0 < 0) {
            return null;
        } else {
            return com.oupao.wiz.CapR.sTask[v0_0];
        }
    }
    public static void put(Object p3, Object p4, Object p5)
    {
        if (p3 != null) {
            com.oupao.wiz.CapR.init();
            int v0_0 = com.oupao.wiz.CapR.idx(p3);
            if (v0_0 < 0) {
                v0_0 = ((com.oupao.wiz.CapR.sLast + 1) & 7);
            }
            com.oupao.wiz.CapR.sLast = v0_0;
            com.oupao.wiz.CapR.sCap[v0_0] = p3;
            com.oupao.wiz.CapR.sTask[v0_0] = p4;
            com.oupao.wiz.CapR.sRect[v0_0] = ((android.graphics.Rect) p5);
        }
        return;
    }
    public static android.graphics.Rect rect(Object p2)
    {
        var v0_0 = com.oupao.wiz.CapR.idx(p2);
        if (v0_0 >= 0) {
            var v1_1 = com.oupao.wiz.CapR.sRect[v0_0];
            if ((v1_1 instanceof android.graphics.Rect)) {
                return ((android.graphics.Rect) v1_1);
            }
        }
        return null;
    }
    public static void remove(Object p3)
    {
        var v0 = com.oupao.wiz.CapR.idx(p3);
        if (v0 >= 0) {
            com.oupao.wiz.CapR.sCap[v0] = null;
            com.oupao.wiz.CapR.sTask[v0] = null;
            com.oupao.wiz.CapR.sRect[v0] = null;
        }
        return;
    }
    public static Object task(Object p2)
    {
        var v0_0 = com.oupao.wiz.CapR.idx(p2);
        if (v0_0 < 0) {
            return null;
        } else {
            return com.oupao.wiz.CapR.sTask[v0_0];
        }
    }
}