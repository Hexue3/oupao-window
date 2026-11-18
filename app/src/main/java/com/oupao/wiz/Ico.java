package com.oupao.wiz;
public class Ico {
    private static java.util.HashMap m;
    public Ico()
    {
        return;
    }
    public static android.graphics.drawable.Drawable d(android.content.Context p2, String p3)
    {
        java.util.HashMap v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Ico.m;
        } catch (Throwable __e_v0) {
            return null;
        }
        if (v0_0 == null) {
            v0_0 = new java.util.HashMap();
            com.oupao.wiz.Ico.m = v0_0;
        }
        if (p3 != null) {
            var v1_5 = ((android.graphics.drawable.Drawable.ConstantState) v0_0.get(p3));
            if (v1_5 != null) {
                var v1_0 = v1_5.newDrawable();
                if (v1_0 != null) {
                    return v1_0;
                }
            }
        }
        try {
            var v1_2 = p2.getPackageManager().getApplicationIcon(p3);
            if (v1_2 != null) {
                var v2_1 = v1_2.getConstantState();
                if (v2_1 != null) {
                    v0_0.put(p3, v2_1);
                }
            }
            return v1_2;
        } catch (Throwable __e_io) {
            return null;
        }
    }
}