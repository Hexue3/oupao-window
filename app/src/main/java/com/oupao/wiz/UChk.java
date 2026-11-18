package com.oupao.wiz;
public class UChk {
    public UChk()
    {
        return;
    }
    static int aiUid(android.content.Context p3, String p4)
    {
        int v0_0 = -1001;
        try {
            if ((p3 != null) && (p4 != null)) {
                var v1_0 = p3.getPackageManager();
                if (v1_0 != null) {
                    var v1_1 = v1_0.getApplicationInfo(p4, 0);
                    if (v1_1 != null) {
                        v0_0 = v1_1.uid;
                    }
                }
            }
        } catch (Throwable __e_v0) {
            return -1002;
        }
        return v0_0;
    }
    static Object am()
    {
        try {
            return com.oupao.wiz.Rt.invoke(com.oupao.wiz.Rt.methodByName(Class.forName("android.app.ActivityManager"), "getService", 0), null, null);
        } catch (Throwable __e_v0) {
            return null;
        }
    }
    public static void go()
    {
        try {
            var v0 = com.oupao.wiz.FlyH.ctx;
            com.oupao.wiz.UChk.ls("flyCtx=", com.oupao.wiz.UChk.nm(v0));
            var v3 = com.oupao.wiz.Cfg.sysContext();
            com.oupao.wiz.UChk.ls("sysCtx=", com.oupao.wiz.UChk.nm(v3));
            com.oupao.wiz.UChk.ls("pmA=", com.oupao.wiz.UChk.nmPm(v0));
            com.oupao.wiz.UChk.ls("pmB=", com.oupao.wiz.UChk.nmPm(v3));
            com.oupao.wiz.UChk.li("A_pmUid=", com.oupao.wiz.UChk.pmUid(v0, "com.tencent.mm"));
            com.oupao.wiz.UChk.li("B_pmUid=", com.oupao.wiz.UChk.pmUid(v3, "com.tencent.mm"));
            com.oupao.wiz.UChk.li("A_aiUid=", com.oupao.wiz.UChk.aiUid(v0, "com.tencent.mm"));
            com.oupao.wiz.UChk.li("B_aiUid=", com.oupao.wiz.UChk.aiUid(v3, "com.tencent.mm"));
            com.oupao.wiz.UChk.ls("am=", com.oupao.wiz.UChk.nm(com.oupao.wiz.UChk.am()));
            com.oupao.wiz.UChk.li("C_procs=", com.oupao.wiz.UChk.procCount());
            com.oupao.wiz.UChk.li("C_uid=", com.oupao.wiz.UChk.procUid("com.tencent.mm"));
            com.oupao.wiz.UChk.li("uidOf(mtm)=", com.oupao.wiz.MiniUI.uidOf("com.tencent.mm"));
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
        /** li/ls 的公共实现（原来两份只差 p3 的类型，StringBuilder.append 两者都吃） */
    private static void l2(String p2, Object p3)
    {
        try {
            var v0_1 = new StringBuilder();
            v0_1.append("UChk: ");
            v0_1.append(p2);
            v0_1.append(p3);
            com.oupao.wiz.SLog.i(v0_1.toString());
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    static void li(String p2, int p3)
    {
        com.oupao.wiz.UChk.l2(p2, Integer.valueOf(p3));
    }
    static void ls(String p2, String p3)
    {
        com.oupao.wiz.UChk.l2(p2, p3);
    }
    static String nm(Object p2)
    {
        java.lang.String v0_0 = "null";
        try {
            if (p2 != null) {
                v0_0 = p2.getClass().getName();
            }
        } catch (Throwable __e_v0) {
            return "err";
        }
        return v0_0;
    }
    static String nmPm(android.content.Context p2)
    {
        java.lang.String v0_0 = "null";
        try {
            if (p2 != null) {
                v0_0 = com.oupao.wiz.UChk.nm(p2.getPackageManager());
            }
        } catch (Throwable __e_v0) {
            return "err";
        }
        return v0_0;
    }
        static int pmUid(android.content.Context p3, String p4)
    {
        int v0_0 = -1000;
        try {
            if ((p3 != null) && (p4 != null)) {
                var v1 = p3.getPackageManager();
                if (v1 != null) {
                    v0_0 = v1.getPackageUid(p4, 0);
                }
            }
        } catch (Throwable __e_v0) {
            return -1000;
        }
        return v0_0;
    }
    static int procCount()
    {
        int v0_0 = 0;
        java.lang.Object v1_0 = null;
        try {
            v1_0 = com.oupao.wiz.UChk.am();
        } catch (Throwable __e_v0) {
            return -1;
        }
        if (v1_0 != null) {
            var v1_1 = com.oupao.wiz.Rt.callNoArg(v1_0, "getRunningAppProcesses");
            if ((v1_1 instanceof java.util.List)) {
                v0_0 = ((java.util.List) v1_1).size();
            }
        }
        return v0_0;
    }
    public static int procUid(String p7)
    {
        int v0_0 = 0;
        try {
            if (p7 != null) {
                var v1_1 = com.oupao.wiz.UChk.am();
                if (v1_1 != null) {
                    var v1_2 = com.oupao.wiz.Rt.callNoArg(v1_1, "getRunningAppProcesses");
                    if ((v1_2 instanceof java.util.List)) {
                        var v2_0 = ((java.util.List) v1_2).iterator();
                        while (v2_0.hasNext()) {
                            var v3_1 = v2_0.next();
                            if ((v3_1 instanceof android.app.ActivityManager.RunningAppProcessInfo)) {
                                var v3_2 = ((android.app.ActivityManager.RunningAppProcessInfo) v3_1);
                                var v4_1 = v3_2.processName;
                                if ((v4_1 != null) && (v4_1.startsWith(p7))) {
                                    v0_0 = v3_2.uid;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable __e_v0) {
            return -1;
        }
        return v0_0;
    }
    static int uidFromList(String p6)
    {
        java.lang.String[] v3_1 = null;
        int v0_0 = 0;
        if (p6 != null) {
            java.io.BufferedReader v1_1 = null;
            try {
                v1_1 = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("/data/system/packages.list")));
            } catch (Throwable __e_v1) {
                return -1;
            }
            do {
                try {
                    var v2_0 = v1_1.readLine();
                    if (v2_0 != null) {
                        v3_1 = v2_0.split(" ");
                    }
                    try {
                        v1_1.close();
                    } catch (Throwable __e_io) {
                    }
                } catch (Throwable __e_io) {
                }
            } while((v3_1.length < 2) || (!v3_1[0].equals(p6)));
            v0_0 = Integer.parseInt(v3_1[1]);
        }
        return v0_0;
    }
}