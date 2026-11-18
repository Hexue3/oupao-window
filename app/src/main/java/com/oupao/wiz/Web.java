package com.oupao.wiz;
public class Web {
    private static boolean hooked;
    public static String lastUrl;
    public Web()
    {
        return;
    }
    public static void tryHook(String p7, ClassLoader p8)
    {
        if (!com.oupao.wiz.Web.hooked) {
            com.oupao.wiz.Web.hooked = true;
            try {
                com.oupao.wiz.XBridge.log("Web: tryHook ".concat(p7));
            } catch (Throwable __e_v0) {
                com.oupao.wiz.XBridge.log("Web: tryHook fail");
                return;
            }
            if (!"com.oupao.wiz".equals(p7)) {
                try {
                    var v0_3 = Class.forName("android.webkit.WebView", false, p8);
                    com.oupao.wiz.XBridge.log("Web: forName ok");
                    var v0_4 = v0_3.getDeclaredMethods();
                    var v1_2 = v0_4.length;
                    int v2 = 0;
                    int v3 = 0;
                    while (v2 < v1_2) {
                        var v4_3 = v0_4[v2];
                        if ("loadUrl".equals(v4_3.getName())) {
                            com.oupao.wiz.XBridge.hookMethod(v4_3, new com.oupao.wiz.Web$H());
                            v3++;
                        }
                        v2++;
                    }
                    var v4_1 = new StringBuilder();
                    v4_1.append("Web: hooked n=");
                    v4_1.append(v3);
                    com.oupao.wiz.XBridge.log(v4_1.toString());
                    return;
                } catch (Throwable __e_io) {
                }
            } else {
                return;
            }
        } else {
            return;
        }
    }
}