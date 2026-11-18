package com.oupao.wiz;
public class PinCmp implements java.util.Comparator {
    public PinCmp()
    {
        return;
    }
    private static String lbl(String p4)
    {
        var v0_1 = p4.indexOf("\u0001");
        if (v0_1 >= 0) {
            return p4.substring(0, v0_1);
        } else {
            return p4;
        }
    }
    public int compare(Object p7, Object p8)
    {
        java.lang.String v0 = null;
        java.lang.String v1 = null;
        java.lang.String v2 = null;
        java.lang.String v3 = null;
        try {
            v0 = com.oupao.wiz.PinCmp.lbl(((String) p7));
            v1 = com.oupao.wiz.PinCmp.lbl(((String) p8));
            v2 = com.oupao.wiz.PinUI.firstLetter(v0);
            v3 = com.oupao.wiz.PinUI.firstLetter(v1);
        } catch (Throwable __e_v4) {
            return 0;
        }
        if (!v2.equals("#")) {
            if (!v3.equals("#")) {
                var v4_2 = v2.compareTo(v3);
                if (v4_2 == 0) {
                    return v0.compareTo(v1);
                } else {
                    return v4_2;
                }
            } else {
                return -1;
            }
        } else {
            if (v3.equals("#")) {
                return v0.compareTo(v1);
            } else {
                return 1;
            }
        }
    }
}