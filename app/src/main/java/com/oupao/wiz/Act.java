package com.oupao.wiz;
public class Act implements java.lang.Runnable {
    public static int found;
    public static long lastAt;
    public static String lastComp;
    public static String lastUrl;
    public static StringBuilder pend;
    private int arg;
    private int op;
    public Act()
    {
        return;
    }
    public Act(int p1, int p2)
    {
        this.op = p1;
        this.arg = p2;
        return;
    }
    public static void flushIfLong()
    {
        java.lang.StringBuilder v0 = null;
        try {
            v0 = com.oupao.wiz.Act.pend;
        } catch (Throwable __e_v0) {
            return;
        }
        if ((v0 != null) && (v0.length() >= 1)) {
            com.oupao.wiz.SLog.i(new StringBuilder().append("Act: \u626b\u63cf\u5230 ").append(v0).toString());
            com.oupao.wiz.Act.pend = null;
        }
        return;
    }
    public static String fromBundle(android.os.Bundle p6)
    {
        java.lang.String v0 = null;
        try {
            if (p6 != null) {
                var v1_0 = p6.keySet();
                if (v1_0 != null) {
                    var v1_1 = v1_0.iterator();
                    if (v1_1 == null) {
                    }
                    while (v1_1.hasNext()) {
                        var v2_0 = v1_1.next();
                        if ((v2_0 instanceof String)) {
                            var v3_0 = p6.get(((String) v2_0));
                            if (!(v3_0 instanceof String)) {
                                if ((v3_0 instanceof android.os.Bundle)) {
                                    v0 = com.oupao.wiz.Act.fromBundle(((android.os.Bundle) v3_0));
                                    if (v0 != null) {
                                        break;
                                    }
                                }
                            } else {
                                v0 = com.oupao.wiz.Act.http(((String) v3_0));
                                if (v0 != null) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable __e_v1) {
            return v0;
        }
        return v0;
    }
    public static void hit(String p3)
    {
        java.lang.StringBuilder v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Act.pend;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_0 == null) {
            v0_0 = new StringBuilder();
            com.oupao.wiz.Act.pend = v0_0;
        } else {
        }
        v0_0.append(" ");
        v0_0.append(p3);
        com.oupao.wiz.Act.flushIfLong();
        return;
    }
    public static String http(String p3)
    {
        try {
            if (p3 != null) {
                var v1 = p3.trim();
                if (v1.startsWith("http")) {
                    return v1;
                }
            }
        } catch (Throwable __e_v1) {
            return null;
        }
        return null;
    }
    public static String info(android.content.Intent p4)
    {
        java.lang.String v0 = null;
        try {
            if (p4 != null) {
                v0 = new StringBuilder().append("act=").append(p4.getAction()).append(" comp=").append(p4.getComponent()).append(" data=").append(p4.getDataString()).append(" extras=").append(p4.getExtras()).toString();
            }
        } catch (Throwable __e_v1) {
            return v0;
        }
        return v0;
    }
    public static void install(ClassLoader p14)
    {
        String[] v6 = null;
        int v4 = 0;
        int v5 = 0;
        try {
            com.oupao.wiz.Act.found = 0;
            v6 = new String[4];
            v6[0] = "com.android.server.wm.ActivityTaskManagerService";
            v6[1] = "com.android.server.wm.ActivityStartController";
            v6[2] = "com.android.server.wm.ActivityStarter";
            v6[3] = "com.android.server.wm.ActivityTaskSupervisor";
            v4 = 0;
        } catch (Throwable __e_v0_18) {
            com.oupao.wiz.SLog.e("Act: install \u5f02\u5e38", __e_v0_18);
            return;
        }
        while (v4 < v6.length) {
            var v0_10 = com.oupao.wiz.Rt.cls(p14, v6[v4]);
            if (v0_10 != null) {
                com.oupao.wiz.Act.found = (com.oupao.wiz.Act.found + 1);
                com.oupao.wiz.Act.hit(v6[v4]);
                var v1_7 = v0_10.getDeclaredMethods();
                var v2 = v1_7.length;
                int v3 = 0;
                while (v3 < v2) {
                    var v7 = v1_7[v3];
                    var v8_0 = v7.getName();
                    if ((v8_0.startsWith("startActivity")) || (v8_0.equals("execute"))) {
                        var v9 = v7.getParameterTypes();
                        int v11 = 0;
                        while (v11 < v9.length) {
                            if (v9[v11] != android.content.Intent.class) {
                                if (com.oupao.wiz.XBridge.hookRaw(v7, new com.oupao.wiz.HookW()) != null) {
                                    break;
                                }
                                v5++;
                                com.oupao.wiz.Act.hit(v6[v4]);
                                com.oupao.wiz.Act.hit(v7.getName());
                                break;
                            } else {
                                v11++;
                            }
                        }
                    } else {
                    }
                    v3++;
                }
            }
            v4++;
        }
        com.oupao.wiz.SLog.i(new StringBuilder().append("Act: \u6302\u94a9\u5b8c\u6210 n=").append(v5).toString());
        return;
    }
    public static boolean on(String p7)
    {
        int v6_0 = 0;
        try {
            if (p7 != null) {
                var v0 = System.currentTimeMillis();
                var v2_6 = com.oupao.wiz.Act.lastUrl;
                if ((v2_6 == null) || ((!p7.equals(v2_6)) || ((v0 - com.oupao.wiz.Act.lastAt) >= 15000))) {
                    com.oupao.wiz.Act.lastUrl = p7;
                    com.oupao.wiz.Act.lastAt = v0;
                    com.oupao.wiz.SLog.i(new StringBuilder().append("Act: \u94fe\u63a5 ").append(p7).toString());
                    com.oupao.wiz.Sug.onWebUrl(p7);
                    v6_0 = 1;
                } else {
                    return false;
                }
            }
        } catch (Throwable __e_v0) {
            return false;
        }
        return v6_0 != 0;
    }
    public static void probe(android.content.Intent p5)
    {
        try {
            if (p5 != null) {
                var v0_0 = p5.getComponent();
                if (v0_0 != null) {
                    var v0_1 = v0_0.getClassName();
                    if ((v0_1 != null) && ((!v0_1.contains("Web")) || (v0_1.contains("rowser")))) {
                        var v1_2 = com.oupao.wiz.Act.lastComp;
                        if ((v1_2 == null) || (!v0_1.equals(v1_2))) {
                            com.oupao.wiz.Act.lastComp = v0_1;
                            com.oupao.wiz.FlyS.sync();
                            com.oupao.wiz.SLog.i(new StringBuilder().append("Act: probe ").append(v0_1).append(" ").append(com.oupao.wiz.Act.info(p5)).toString());
                        } else {
                            return;
                        }
                    }
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static String url(android.content.Intent p3)
    {
        java.lang.String v0 = null;
        try {
            if (p3 != null) {
                v0 = com.oupao.wiz.Act.http(p3.getDataString());
                if (v0 == null) {
                    v0 = com.oupao.wiz.Act.fromBundle(p3.getExtras());
                } else {
                }
            }
        } catch (Throwable __e_v1) {
            return v0;
        }
        return v0;
    }
    public void run()
    {
        var v0_0 = this.op;
        if (v0_0 == 1) {
            com.oupao.wiz.Hooks.doClose();
            return;
        } else {
            if (v0_0 == 2) {
                com.oupao.wiz.Hooks.checkClosed(this.arg);
                return;
            } else {
                com.oupao.wiz.Hooks.checkFull(this.arg);
                return;
            }
        }
    }
}