package com.oupao.wiz;
public class Diag {
    /** 实际构建版本（原来是写死的 "3.28.7-1820"，日志里会误导排查） */
    public static final String BUILD = com.oupao.wiz.BuildConfig.VERSION_NAME;
    public static String appData;
    private static android.widget.TextView cardTxt;
    private static java.util.concurrent.ConcurrentHashMap map;
    private static boolean over;
    public static android.view.View pending;
    private static int tries;
    static
    {
        com.oupao.wiz.Diag.map = new java.util.concurrent.ConcurrentHashMap();
    }
    public Diag()
    {
        return;
    }
    public static String bodyText()
    {
        java.lang.String v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Diag.appData;
        } catch (Throwable __e___ex) {
            return "\u8bca\u65ad\u8bfb\u53d6\u5931\u8d25";
        }
        if (v0_0 != null) {
            if (v0_0.contains("Master=0")) {
                v0_0 = new StringBuilder().append(v0_0).append("\n\n\u26a0 \u300c\u5c0f\u7a97\u529f\u80fd\u300d\u9875\u9876\u90e8\u7684\u300c\u542f\u7528\u529f\u80fd\u300d\u4e3b\u5f00\u5173\u73b0\u5728\u662f\u5173\u7684\uff1a\n\u7cfb\u7edf\u4fa7\u53ea\u88c5\u4e86\u901a\u9053\uff0c\u529f\u80fd\u6ca1\u6709\u5b89\u88c5\u3002\n\u6253\u5f00\u5b83\uff08App \u91cc\u70b9\u4e00\u4e0b\uff09\u7136\u540e\u91cd\u542f\u5373\u53ef\u3002").toString();
            }
        } else {
            if (!com.oupao.wiz.Diag.over) {
                v0_0 = "\u6b63\u5728\u8bfb\u53d6\u6a21\u5757\u72b6\u6001\u2026\uff08\u6700\u591a\u7b49\u7ea6 3 \u79d2\uff0c\u671f\u95f4\u81ea\u52a8\u91cd\u8bd5\uff09";
            } else {
                v0_0 = "\u8d85\u65f6\u672a\u6536\u5230\u56de\u5305\uff08\u5df2\u81ea\u52a8\u91cd\u8bd5 3 \u6b21\uff09\n\u2460 LSPosed \u91cc\u6a21\u5757\u8981\u300c\u542f\u7528\u300d\uff08\u53ea\u52fe\u4f5c\u7528\u57df\u4e0d\u7b97\uff09\n\u2461 \u88c5\u5b8c\u6216\u66f4\u65b0 APK \u540e\u5fc5\u987b\u91cd\u542f\n\u2462 \u4ecd\u5931\u8d25\u8bf7\u628a\u672c\u5361\u7247\u622a\u56fe\u53d1\u6211\n\uff08\u672c\u673a dex \u4e0e system_server dex \u4e0d\u540c = \u9700\u91cd\u542f\uff09";
            }
        }
        return v0_0;
    }
    public static String cliInfo(android.content.Context p6)
    {
        try {
            var v0_2 = new StringBuilder().append("\u672c\u673a App \u6784\u5efa ").append(com.oupao.wiz.Diag.BUILD).append(" \u00b7 ver ");
            var v1_1 = p6.getPackageManager().getPackageInfo(p6.getPackageName(), 0);
            return v0_2.append(v1_1.versionName).append("(").append(v1_1.versionCode).append(") apk ").append(new java.io.File(p6.getPackageCodePath()).length()).append("B").toString();
        } catch (Throwable __e___ex) {
            return "\u672c\u673a\u4fe1\u606f\u8bfb\u53d6\u5931\u8d25";
        }
    }
    public static void flush()
    {
        java.lang.StringBuilder v2_1 = null;
        java.util.Iterator v3 = null;
        try {
            com.oupao.wiz.Diag.put("ROM", com.oupao.wiz.FrameworkExt.sysProp("ro.build.version.oplusrom", ""));
            v2_1 = new StringBuilder();
            v3 = com.oupao.wiz.Diag.map.entrySet().iterator();
        } catch (Throwable __e___ex) {
            return;
        }
        while (v3.hasNext()) {
            var v0_10 = ((java.util.Map.Entry) v3.next());
            v2_1.append(((String) v0_10.getKey()));
            v2_1.append("=");
            v2_1.append(((String) v0_10.getValue()));
            v2_1.append("\n");
        }
        var v4 = v2_1.toString().getBytes();
        try {
            var v0_5 = new java.io.FileOutputStream("/data/system/oupao_diag");
            v0_5.write(v4);
            v0_5.close();
            android.system.Os.chmod("/data/system/oupao_diag", 420);
            var v0_8 = new java.io.FileOutputStream("/data/data/com.oupao.wiz/files/diag");
            v0_8.write(v4);
            v0_8.close();
        } catch (Throwable __e_io) {
        }
        return;
    }
    public static void onReq(android.content.Intent p9)
    {
        java.lang.String v1_6 = null;
        try {
            com.oupao.wiz.Diag.put("Rt", com.oupao.wiz.Rt.stat());
            com.oupao.wiz.Diag.put("Ev", com.oupao.wiz.Ev.stat());
            com.oupao.wiz.Diag.put("SvrBuild", com.oupao.wiz.Diag.BUILD);
            v1_6 = p9.getStringExtra("build");
        } catch (Throwable __e_v0_2) {
            com.oupao.wiz.SLog.i(__e_v0_2.toString());
            return;
        }
        if (v1_6 == null) {
            v1_6 = "(\u672a\u77e5)";
        } else {
        }
        java.lang.String v2_8 = null;
        com.oupao.wiz.Diag.put("CliBuild", v1_6);
        com.oupao.wiz.SLog.i(new StringBuilder().append("Diag: onReq cli=").append(v1_6).toString());
        com.oupao.wiz.Diag.put("Uptime", String.valueOf((android.os.SystemClock.elapsedRealtime() / 1000)));
        com.oupao.wiz.Diag.putMod();
        com.oupao.wiz.Diag.put("android", "1");
        com.oupao.wiz.Diag.put("SDK", String.valueOf(com.oupao.wiz.Sys.sdk()));
        com.oupao.wiz.Diag.put("ROM", com.oupao.wiz.FrameworkExt.sysProp("ro.build.version.oplusrom", ""));
        var v0_9 = com.oupao.wiz.Hooks.CL;
        if (com.oupao.wiz.Rt.cls(v0_9, "com.android.server.wm.FlexibleCaptionView") == null) {
            v2_8 = "0";
        } else {
            v2_8 = "1";
        }
        java.lang.String v2_10 = null;
        com.oupao.wiz.Diag.put("Bar", v2_8);
        if (com.oupao.wiz.Rt.cls(v0_9, "com.android.server.wm.OplusZoomWindowManagerService") == null) {
            v2_10 = "0";
        } else {
            v2_10 = "1";
        }
        com.oupao.wiz.Diag.put("ZoomSvc", v2_10);
        var v0_11 = new StringBuilder();
        var v2_11 = com.oupao.wiz.Diag.map.entrySet().iterator();
        while (v2_11.hasNext()) {
            var v1_4 = ((java.util.Map.Entry) v2_11.next());
            v0_11.append(((String) v1_4.getKey()));
            v0_11.append("=");
            v0_11.append(((String) v1_4.getValue()));
            v0_11.append("\n");
        }
        var v0_16 = new android.content.Intent("com.oupao.wiz.DIAG").setPackage("com.oupao.wiz").putExtra("data", v0_11.toString());
        // 先把诊断数据落盘：回包的 extra 一旦丢失，App 侧还能从 /data/system/oupao_diag 读到
        try {
            com.oupao.wiz.Diag.flush();
        } catch (Throwable ignore) {
        }
        try {
            var v6 = Class.forName("android.app.ActivityThread").getMethod("currentActivityThread", new Class[0]).invoke(null, new Object[0]);
            var v6_1 = v6.getClass().getMethod("getSystemContext", new Class[0]).invoke(v6, new Object[0]);
            if (v6_1 != null) {
                ((android.content.Context) v6_1).sendBroadcast(v0_16);
                com.oupao.wiz.SLog.i("Diag: resp sent");
            } else {
                com.oupao.wiz.SLog.i("Diag: 无系统 Context，回包未发");
            }
        } catch (Throwable __e_io) {
        }
        return;
    }
    public static void put(String p2, String p3)
    {
        try {
            com.oupao.wiz.Diag.map.put(p2, p3);
        } catch (Throwable __e___ex) {
        }
        return;
    }
    public static void putMod()
    {
        io.github.libxposed.api.XposedInterface v0 = null;
        try {
            v0 = com.oupao.wiz.XBridge.xp;
        } catch (Throwable __e___ex) {
            return;
        }
        if (v0 != null) {
            com.oupao.wiz.Diag.put("Fw", new StringBuilder().append(v0.getFrameworkName()).append(" ").append(v0.getFrameworkVersion()).append(" api ").append(v0.getApiVersion()).toString());
            var v1_7 = v0.getModuleApplicationInfo().sourceDir;
            com.oupao.wiz.Diag.put("ModApk", new StringBuilder().append(v1_7).append(" ").append(new java.io.File(v1_7).length()).append("B").toString());
        }
        return;
    }
    public static void readAll(StringBuilder p5)
    {
        try {
            com.oupao.wiz.Diag.readOne(p5, "/data/data/com.oupao.wiz/files/diag");
        } catch (Throwable __e_v0) {
            return;
        }
        if (p5.length() <= 0) {
            com.oupao.wiz.Diag.readOne(p5, "/data/system/oupao_diag");
            return;
        } else {
            return;
        }
    }
    private static void readOne(StringBuilder p5, String p6)
    {
        java.io.File v0_1 = null;
        try {
            v0_1 = new java.io.File(p6);
        } catch (Throwable __e_v0) {
            return;
        }
        if (!v0_1.exists()) {
            return;
        } else {
            var v1_2 = ((int) v0_1.length());
            if (v1_2 <= 0) {
                return;
            } else {
                byte[] v1_3 = new byte[v1_2];
                try {
                    var v2_1 = new java.io.FileInputStream(v0_1);
                    v2_1.read(v1_3);
                    v2_1.close();
                    p5.append(new String(v1_3));
                } catch (Throwable __e_io) {
                }
                return;
            }
        }
    }
    public static void refresh()
    {
        android.widget.TextView v0 = null;
        try {
            v0 = com.oupao.wiz.Diag.cardTxt;
        } catch (Throwable __e___ex) {
            return;
        }
        if (v0 != null) {
            v0.setText(com.oupao.wiz.Diag.bodyText());
        }
        return;
    }
    public static void sendReq(android.content.Context p3)
    {
        try {
            p3.sendBroadcast(com.oupao.wiz.Ev.it("com.oupao.wiz.DIAG_REQ").putExtra("build", com.oupao.wiz.Diag.BUILD));
            com.oupao.wiz.SLog.i("Diag: req sent");
            return;
        } catch (Throwable __e___ex) {
            return;
        }
    }
    public static void showDiag(android.app.Activity p8)
    {
        java.lang.String v2 = null;
        java.lang.StringBuilder v0_1 = null;
        try {
            v0_1 = new StringBuilder();
            com.oupao.wiz.Diag.readAll(v0_1);
            v2 = "\u672a\u6536\u96c6\u5230\u8bca\u65ad\u6570\u636e\uff1a\u591a\u534a\u662f\u88c5\u5b8c\u6ca1\u91cd\u542f\uff0c\u6216\u6ca1\u52fe\u9009\u201c\u7cfb\u7edf\u6846\u67b6\u201d\u4f5c\u7528\u57df";
        } catch (Throwable __e___ex) {
            return;
        }
        if (v0_1.length() > 0) {
            v2 = v0_1.toString();
        }
        var v1_0 = new StringBuilder();
        v1_0.append("\u8bca\u65ad\u6570\u636e\uff08\u6765\u81ea\u4e0a\u6b21\u5f00\u673a\uff09\uff1a\n");
        v1_0.append(v2);
        new android.app.AlertDialog.Builder(p8).setTitle("\u8fd0\u884c\u73af\u5883\u8bca\u65ad").setMessage(v1_0.toString()).setPositiveButton("\u77e5\u9053\u4e86", new com.oupao.wiz.Diag$DL()).show();
        return;
    }
    public static void showOverlay(android.view.View p5)
    {
        try {
            com.oupao.wiz.SLog.i("Diag: click");
            com.oupao.wiz.Diag.pending = p5;
            com.oupao.wiz.Diag.appData = null;
            com.oupao.wiz.Diag.tries = 0;
            com.oupao.wiz.Diag.over = false;
            com.oupao.wiz.Diag.cardTxt = null;
            com.oupao.wiz.Diag.sendReq(p5.getContext());
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new com.oupao.wiz.Diag$Wait(), 250);
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.SLog.i(__e_v0_1.toString());
        }
        return;
    }
    public static void showPending()
    {
        try {
            if (com.oupao.wiz.Diag.cardTxt == null) {
                com.oupao.wiz.SLog.i("Diag: pending shot");
                var v0_2 = com.oupao.wiz.Diag.pending;
                if (v0_2 == null) {
                    return;
                } else {
                    var v0_4 = ((android.view.ViewGroup) v0_2.getRootView());
                    var v1 = v0_4.getContext();
                    var v2_1 = new android.widget.FrameLayout(v1);
                    v2_1.setBackgroundColor(-1342177280);
                    var v3_2 = new android.widget.LinearLayout(v1);
                    v3_2.setOrientation(1);
                    var v4_3 = new android.graphics.drawable.GradientDrawable();
                    v4_3.setShape(0);
                    v4_3.setCornerRadius(((float) com.oupao.wiz.Ui.dp(v1, 12)));
                    v4_3.setColor(-1);
                    v3_2.setBackground(v4_3);
                    var v4_5 = new android.widget.FrameLayout.LayoutParams(-1, -2);
                    v4_5.gravity = 17;
                    v4_5.leftMargin = com.oupao.wiz.Ui.dp(v1, 24);
                    v4_5.rightMargin = com.oupao.wiz.Ui.dp(v1, 24);
                    v3_2.setLayoutParams(v4_5);
                    com.oupao.wiz.Ui.pad(v3_2, 18, 18, 18, 18);
                    var v4_8 = new android.widget.TextView(v1);
                    v4_8.setText("\u8fd0\u884c\u73af\u5883\u8bca\u65ad");
                    v4_8.setTextSize(17.0f);
                    v4_8.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                    v4_8.setTextColor(com.oupao.wiz.Ui.adaptText(-15658735));
                    v3_2.addView(v4_8);
                    var v4_10 = new android.widget.TextView(v1);
                    v4_10.setText(com.oupao.wiz.Diag.bodyText());
                    v4_10.setTextSize(13.0f);
                    v4_10.setTextColor(com.oupao.wiz.Ui.adaptText(-13421773));
                    com.oupao.wiz.Diag.cardTxt = v4_10;
                    v3_2.addView(v4_10);
                    var v4_12 = new android.widget.TextView(v1);
                    v4_12.setText(com.oupao.wiz.Diag.cliInfo(v1));
                    v4_12.setTextSize(11.0f);
                    v4_12.setTextColor(com.oupao.wiz.Ui.adaptText(-10066330));
                    v3_2.addView(v4_12);
                    var v4_14 = new android.widget.TextView(v1);
                    v4_14.setText("\u77e5\u9053\u4e86");
                    v4_14.setTextSize(14.0f);
                    v4_14.setTextColor(com.oupao.wiz.Ui.adaptText(-15658735));
                    v4_14.setGravity(17);
                    v4_14.setOnClickListener(new com.oupao.wiz.Diag$Close(v2_1));
                    v3_2.addView(v4_14);
                    v2_1.addView(v3_2);
                    v2_1.setOnClickListener(new com.oupao.wiz.Diag$Close(v2_1));
                    v0_4.addView(v2_1, new android.view.ViewGroup.LayoutParams(-1, -1));
                }
            } else {
                com.oupao.wiz.Diag.refresh();
                return;
            }
        } catch (Throwable __e_v0_0) {
            com.oupao.wiz.SLog.i(__e_v0_0.toString());
        }
        return;
    }
}