package com.oupao.wiz;
public class Web$H extends com.oupao.wiz.XMH {
    public Web$H()
    {
        return;
    }
    public void afterHookedMethod(com.oupao.wiz.XMP p7)
    {
        java.lang.Object[] v0_0 = null;
        try {
            v0_0 = p7.args;
        } catch (Throwable __e_v0_2) {
            com.oupao.wiz.XBridge.log(__e_v0_2);
            return;
        }
        if ((v0_0 != null) && (v0_0.length > 0)) {
            var v0_1 = v0_0[0];
            if ((v0_1 instanceof String)) {
                var v0_3 = ((String) v0_1);
                if ((v0_3 != null) && ((v0_3.length() >= 8) && (v0_3.startsWith("http")))) {
                    com.oupao.wiz.XBridge.log("Web: load ".concat(v0_3));
                    var v1_6 = com.oupao.wiz.Web.lastUrl;
                    if ((v1_6 == null) || (!v1_6.equals(v0_3))) {
                        com.oupao.wiz.Web.lastUrl = v0_3;
                        var v1_7 = com.oupao.wiz.Sug.mapPkg(v0_3);
                        com.oupao.wiz.XBridge.log("Web: map=".concat(String.valueOf(v1_7)));
                        if (v1_7 != null) {
                            com.oupao.wiz.XBridge.log("Web: match ".concat(v1_7));
                            var v2_8 = ((android.webkit.WebView) p7.thisObject).getContext();
                            if (v2_8 != null) {
                                var v3_1 = com.oupao.wiz.Ev.it("com.oupao.wiz.URL");
                                v3_1.putExtra("url", v0_3);
                                v2_8.sendBroadcast(v3_1);
                                com.oupao.wiz.XBridge.log("Web: sent");
                            }
                        }
                    }
                }
            }
        }
        return;
    }
}