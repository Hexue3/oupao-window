package com.oupao.wiz;
public class FlyH {
    /** 旧菜单状态机总开关：2745 起永假（旧菜单由 FlyFanH 自绘版接管） */
    public static boolean legacy = false;

    /** `sys.oupao.fly.legacy=1` → 回到旧菜单（自绘版出问题时的退路） */
    public static boolean legacyOn() {
        try {
            return "1".equals(com.oupao.wiz.FlyState.raw("sys.oupao.fly.legacy").trim());
        } catch (Throwable t) {
            return false;
        }
    }
    /** 本次手势是否已经激活过菜单（用来判断"要不要把这一下当点击重放"） */
    public static boolean act;
    public static float[] FAN;
    public static float[] POS;
    public static int arcH;
    public static int arcS;
    public static Object bk;
    public static android.view.View cat;
    public static long cloneAt;
    public static int cnt;
    public static String[] comps;
    public static android.content.Context ctx;
    public static float den;
    public static float dthr;
    public static android.view.View[] items;
    public static float mx;
    public static float ox;
    public static float oy;
    public static boolean pend;
    public static boolean pilOk;
    public static android.view.View prs;
    public static float rad;
    public static boolean reg;
    public static int rox;
    public static int roy;
    public static boolean sched;
    /** 菜单最近一次显示的时刻（FlyCat 用它做"刚弹出那一瞬"的点击豁免） */
    public static long shownAt;
    public static int sel;
    public static int sh;
    public static int side;
    /** 触摸 slop（px），判定用的两轴阈值由 FlyGeom 从这里推 */
    public static float slop;
    public static int st;
    public static boolean stick;
    public static int sw;
    public static long warmT;
    public static android.view.View win;
    public static android.view.WindowManager wm;
    public static Object wms;
    public static boolean zdrv;
    public FlyH()
    {
        return;
    }
    public static void bindBack()
    {
        android.view.View v0 = null;
        try {
            v0 = com.oupao.wiz.FlyH.cat;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 != null) {
            var v1 = v0.findOnBackInvokedDispatcher();
            if (v1 != null) {
                var v2_1 = new com.oupao.wiz.FlyBk();
                v1.registerOnBackInvokedCallback(0, v2_1);
                com.oupao.wiz.FlyH.bk = v2_1;
            }
        }
        return;
    }
    public static void buzz()
    {
        try {
            // buzz 是高频路径：字母索引每换一个字母、扇形菜单每次点开都会震一下。
            // 正常路径全部降到 verbose（开 oupao_dbg 才看），只保留「跳过的原因」为 info，
            // 否则滑动一次索引条就能刷出十几条日志。
            com.oupao.wiz.SLog.r("r-buzz", "Fly: buzz in", 1000L);
        } catch (Throwable __e_v0_7) {
            var v1_4 = new StringBuilder();
            v1_4.append("Fly: vib err ");
            v1_4.append(String.valueOf(__e_v0_7));
            com.oupao.wiz.SLog.i(v1_4.toString());
            return;
        }
        if (!com.oupao.wiz.Cfg.flyVib()) {
            com.oupao.wiz.SLog.i("Fly: vib skip off");
            return;
        } else {
            android.content.Context v0_1 = com.oupao.wiz.Cfg.sysContext();
            if (v0_1 == null) {
                v0_1 = com.oupao.wiz.FlyH.ctx;
                if (v0_1 == null) {
                    v0_1 = com.oupao.wiz.Sug.ctx;
                    if (v0_1 == null) {
                        com.oupao.wiz.SLog.i("Fly: vib skip no ctx");
                        return;
                    }
                }
            }
            // 原移植逻辑是「先取 vibrator，再取 vibrator_manager，取不到 manager 就 return」——
            // 这会让没有 VibratorManager（API < 31）的设备直接震不了，且取到的 vibrator 被丢弃。
            // 正确做法：优先 manager（API 31+，defVib 的 getDefaultVibrator 只在它上面有），
            // 拿不到 manager 就退回 Vibrator（vibObj 反射的 vibrate(VibrationEffect) 在
            // API 26+ 的 Vibrator 上也有，所以这条路依然能震）。
            java.lang.Object v1_1 = null;
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                v1_1 = v0_1.getSystemService("vibrator_manager");
            }
            if (v1_1 == null) {
                v1_1 = v0_1.getSystemService("vibrator");
            }
            if (v1_1 == null) {
                com.oupao.wiz.SLog.i("Fly: vib skip no service");
                return;
            }
            java.lang.Object v2_0 = v1_1;
            var v3_0 = com.oupao.wiz.FlyH.defVib(v1_1);
            if (v3_0 != null) {
                v2_0 = v3_0;
            }
            var v3_1 = com.oupao.wiz.Cfg.flyVibLevel();
            if (v3_1 == 0) {
                com.oupao.wiz.SLog.i("Fly: vib skip lv=0");
                return;
            } else {
                int v3_3 = (com.oupao.wiz.Sug.vibDurMs(v3_1) * 2);
                if (v3_3 < 30) {
                    v3_3 = 30;
                }
                com.oupao.wiz.Sug.vibObj(v2_0);
                var v4_2 = new StringBuilder();
                v4_2.append("Fly: vib ok ms=");
                v4_2.append(v3_3);
                v4_2.append(" ");
                v4_2.append(v2_0.getClass().getName());
                com.oupao.wiz.SLog.v(v4_2.toString());
                return;
            }
        }
    }
    public static boolean callReg(java.lang.reflect.Method p3, Object p4, Object[] p5)
    {
        try {
            p3.setAccessible(true);
            p3.invoke(p4, p5);
            return true;
        } catch (Throwable v0) {
            com.oupao.wiz.SLog.e("Fly: \u6ce8\u518c\u6307\u9488\u76d1\u542c\u5931\u8d25", v0);
            var v1 = v0.getCause();
            if (v1 != null) {
                com.oupao.wiz.SLog.e("Fly: \u6ce8\u518c\u5931\u8d25\u539f\u56e0", v1);
            }
            return false;
        }
    }
    public static void cancel()
    {
        try {
            com.oupao.wiz.FlyH.hide();
            com.oupao.wiz.FlyH.st = 0;
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static void cloneLaunch(String p11, String p12, int p13)
    {
        android.content.ComponentName v0_3 = null;
        android.content.Intent v1_1 = null;
        try {
            var v0_1 = new StringBuilder();
            v0_1.append("Fly: \u5206\u8eab\u542f\u52a8 ");
            v0_1.append(p11);
            v0_1.append(" u=");
            v0_1.append(p13);
            com.oupao.wiz.SLog.i(v0_1.toString());
            v0_3 = new android.content.ComponentName(p11, p12);
            v1_1 = new android.content.Intent("android.intent.action.MAIN");
            v1_1.addCategory("android.intent.category.LAUNCHER");
            v1_1.setComponent(v0_3);
            v1_1.setPackage(p11);
            v1_1.addFlags(268435456);
        } catch (Throwable __e_v0_4) {
            com.oupao.wiz.SLog.e("Fly: \u5206\u8eab\u542f\u52a8\u5931\u8d25", __e_v0_4);
            return;
        }
        // 3246：分身应用正在前台全屏时，先走 ROM 的「全屏→小窗」过渡（和普通应用同一条路）。
        //   之前分身直接 `zoomOpen` → `startZoomWindow`，所以 QQ/微信分身"全屏转小窗"没有动效。
        // 3303：所有开窗请求统一交给 ZoomGate 串行调度（关旧窗 / 收敛 / 聚焦都在那里）。
        final android.content.Intent fIntent = v1_1;
        final android.content.ComponentName fCn = v0_3;
        final String fPkg = p11;
        final int fUser = p13;
        com.oupao.wiz.ZoomGate.request(fPkg, fUser, new com.oupao.wiz.ZoomGate.Opener() {
            public void open() {
                com.oupao.wiz.FlyH.cloneLaunchNow(fIntent, fCn, fPkg, fUser);
            }
        });
        return;
    }

    /** 3258：分身启动的实际动作（完成分辨率/过渡后调 cloneOpen） */
    static void cloneLaunchNow(final android.content.Intent fIntent, final android.content.ComponentName fCn,
                               final String fPkg, final int fUser)
    {
        final int fGen = (++com.oupao.wiz.FlyH.zoomGen);
        // ★ 3351：**分身**全屏停在二级页 → 走平行小窗（和主应用同一条路），
        //   布防时带上 userId，返回兜底才不会跳到主应用上。
        if (com.oupao.wiz.SplitGuard.isSecondaryFullscreen(fPkg, fUser)) {
            com.oupao.wiz.SplitGuard.armFromCurrent(fPkg, fUser);
            com.oupao.wiz.SLog.k("Fly: 二级页(分身) " + fPkg + " u=" + fUser + " -> 平行小窗(主页)");
            com.oupao.wiz.FlyH.cloneOpen(fIntent, fCn, fUser);
            return;
        }
        if (com.oupao.wiz.FlyH.tryFullscreenToZoom(fPkg, fUser, null, null, null,
                new Runnable() {
                    public void run() {
                        com.oupao.wiz.FlyH.cloneOpen(fIntent, fCn, fUser);
                    }
                }, fGen)) {
            return;
        }
        com.oupao.wiz.FlyH.cloneOpen(fIntent, fCn, fUser);
    }

    /** 3246：分身开窗的实际动作（过渡不可用/没生效时的兜底，抽出来给 fallback 用） */
    static void cloneOpen(android.content.Intent intent, android.content.ComponentName cn, int userId)
    {
        cloneOpen(intent, cn, userId, 1);
    }

    /** 3251：`zoomOpen` 返回非 0（ROM 拒绝，通常是上一个窗口还在退场）时补一次 */
    static void cloneOpen(final android.content.Intent intent, final android.content.ComponentName cn,
                          final int userId, final int retry)
    {
        if (!com.oupao.wiz.FlyH.zoomOpen(intent, userId)) {
            if (retry > 0) {
                com.oupao.wiz.SLog.i("Fly: 分身开窗被拒 → 300ms 后重试 u=" + userId + " 剩 " + retry);
                android.os.Handler h = com.oupao.wiz.Hooks.main();
                if (h != null) {
                    h.postDelayed(new Runnable() {
                        public void run() {
                            com.oupao.wiz.FlyH.cloneOpen(intent, cn, userId, retry - 1);
                        }
                    }, 300L);
                    return;
                }
            }
            android.content.Context v2_4 = com.oupao.wiz.FlyH.ctx;
            if (v2_4 == null) {
                v2_4 = com.oupao.wiz.Cfg.sysContext();
            }
            if (v2_4 != null) {
                var v3_1 = v2_4.getSystemService("launcherapps");
                if (v3_1 != null) {
                    var v3_2 = ((android.content.pm.LauncherApps) v3_1);
                    var v4 = com.oupao.wiz.FrameworkExt.userHandleOf(userId);
                    var v5_0 = android.app.ActivityOptions.makeBasic();
                    v5_0.setLaunchDisplayId(0);
                    var v5_1 = v5_0.toBundle();
                    v5_1.putInt("extra_window_mode", 100);
                    v3_2.startMainActivity(cn, v4, null, v5_1);
                    com.oupao.wiz.SLog.i("Fly: \u5206\u8eab\u5df2\u542f\u52a8(LauncherApps)");
                }
            }
            return;
        } else {
            com.oupao.wiz.SLog.i("Fly: \u5206\u8eab\u5df2\u5f00\u5c0f\u7a97");
            return;
        }
    }
    public static void cloneScan()
    {
        android.content.Context v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Cfg.sysContext();
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.SLog.e("Fly: \u5206\u8eab\u626b\u63cf\u5f02\u5e38", __e_v0_1);
            return;
        }
        if (v0_0 != null) {
            var v1_2 = v0_0.getSystemService("launcherapps");
            if (v1_2 != null) {
                var v1_3 = ((android.content.pm.LauncherApps) v1_2);
                var v2_0 = v0_0.getSystemService("user");
                if (v2_0 != null) {
                    var v3 = com.oupao.wiz.FrameworkExt.usersOf(((android.os.UserManager) v2_0));
                    if (v3 != null) {
                        com.oupao.wiz.SLog.i("Fly: \u5206\u8eab\u626b\u63cf\u8fdb\u5165");
                        var v4_1 = new StringBuilder();
                        var v5 = com.oupao.wiz.FrameworkExt.myUserId();
                        var v6 = v3.iterator();
                        while (v6.hasNext()) {
                            var v8 = com.oupao.wiz.FrameworkExt.userInfoId((v6.next()));
                            if (v8 != v5) {
                                var v9_2 = v1_3.getActivityList(null, com.oupao.wiz.FrameworkExt.userHandleOf(v8));
                                int v13_2 = 0;
                                if (v9_2 != null) {
                                    v13_2 = v9_2.size();
                                }
                                com.oupao.wiz.FlyH.lg("Fly: \u5206\u8eab\u7528\u6237 u=", v8, v13_2, v13_2);
                                if (v9_2 != null) {
                                    var v10_3 = v9_2.iterator();
                                    while (v10_3.hasNext()) {
                                        var v11_7 = ((android.content.pm.LauncherActivityInfo) v10_3.next());
                                        var v12_8 = v11_7.getComponentName();
                                        if (v12_8 != null) {
                                            v4_1.append(v11_7.getLabel());
                                            v4_1.append("\uff08\u5206\u8eab\uff09");
                                            v4_1.append("\u0001");
                                            v4_1.append(v12_8.getPackageName());
                                            v4_1.append("#");
                                            v4_1.append(v12_8.getClassName());
                                            v4_1.append("#");
                                            v4_1.append(v8);
                                            v4_1.append("\n");
                                        }
                                    }
                                }
                            }
                        }
                        var v13_1 = v4_1.toString();
                        com.oupao.wiz.Cfg.saveStr("clones", v13_1);
                        try {
                            new java.io.File("/data/system/oupao_cfg").mkdirs();
                            var v12_5 = new java.io.File("/data/system/oupao_cfg/clones");
                            var v11_3 = new java.io.FileOutputStream(v12_5);
                            v11_3.write(v13_1.getBytes());
                            v11_3.close();
                            v12_5.setReadable(true, false);
                            com.oupao.wiz.Cfg.gset("clones", v13_1);
                            com.oupao.wiz.FlyH.lg("Fly: \u5206\u8eab\u5217\u8868\u5df2\u5199\u5165 len=", v13_1.length(), v13_1.length(), v13_1.length());
                        } catch (Throwable __e_io) {
                            // 写不进系统通道 → 「更多应用/引索条」会看不到分身（2639 同款坑），不能再静默
                            com.oupao.wiz.SLog.e("Fly: \u5206\u8eab\u5217\u8868\u5199 /data/system/oupao_cfg/clones \u5931\u8d25", __e_io);
                        }
                    }
                }
            }
        }
        return;
    }
    public static void cloneTick()
    {
        long v0_0 = 0;
        long v2 = 0;
        try {
            v0_0 = android.os.SystemClock.elapsedRealtime();
            v2 = com.oupao.wiz.FlyH.cloneAt;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v2 != 0) {
            if ((v0_0 - v2) >= 60000) {
                com.oupao.wiz.SLog.i("Fly: \u5206\u8eab\u626b\u63cf\u542f\u52a8\u7ebf\u7a0b");
                com.oupao.wiz.FlyH.cloneAt = v0_0;
                new Thread(new com.oupao.wiz.CloneR()).start();
            }
        } else {
            // 第一次（cloneAt 还是 0）也必须扫一次。
            // 原来这里是个空 else，而 cloneAt 只会在这个 if 分支里被赋值 ——
            // 于是 cloneAt 永远是 0，分身扫描**一次都不会启动**，
            // 表现就是「更多应用」里一个分身应用都没有。
            com.oupao.wiz.SLog.i("Fly: \u5206\u8eab\u626b\u63cf\u9996\u6b21\u542f\u52a8\u7ebf\u7a0b");
            com.oupao.wiz.FlyH.cloneAt = (v0_0 != 0) ? v0_0 : 1;
            new Thread(new com.oupao.wiz.CloneR()).start();
        }
        return;
    }
    public static void close()
    {
        try {
            com.oupao.wiz.FlyH.buzz();
            com.oupao.wiz.FlyH.sel = -1;
            com.oupao.wiz.FlyH.hide();
            com.oupao.wiz.FlyH.st = 0;
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static void commit()
    {
        int v0_1 = 0;
        try {
            com.oupao.wiz.FlyH.reveal(1.0f);
            com.oupao.wiz.FlyH.hide();
            com.oupao.wiz.FlyH.st = 0;
            v0_1 = com.oupao.wiz.FlyH.sel;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_1 >= 0) {
            var v1_1 = com.oupao.wiz.FlyH.comps;
            if ((v1_1 != null) && (v0_1 < v1_1.length)) {
                var v0_2 = v1_1[v0_1];
                com.oupao.wiz.FlyH.lg("Fly: commit sel", com.oupao.wiz.FlyH.sel, com.oupao.wiz.FlyH.sel, com.oupao.wiz.FlyH.sel);
                com.oupao.wiz.FlyH.launch(v0_2);
            }
        }
        return;
    }
    public static Object defVib(Object p4)
    {
        try {
            Class[] v0_1 = new Class[0];
            Object[] v0_5 = new Object[0];
            return p4.getClass().getMethod("getDefaultVibrator", v0_1).invoke(p4, v0_5);
        } catch (Throwable __e_v0) {
            return null;
        }
    }
    public static void dims()
    {
        android.content.Context v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Cfg.sysContext();
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_0 != null) {
            var v0_9 = v0_0.getResources().getDisplayMetrics();
            var v1_3 = v0_9.widthPixels;
            var v2 = v0_9.heightPixels;
            var v3 = v0_9.density;
            if ((v1_3 >= 200) && (v2 >= 200)) {
                if ((com.oupao.wiz.FlyH.sw != v1_3) || (com.oupao.wiz.FlyH.sh != v2)) {
                    com.oupao.wiz.SLog.v(new StringBuilder("Fly: dims ").append(v1_3).append("x").append(v2).toString());
                } else {
                }
                com.oupao.wiz.FlyH.sw = v1_3;
                com.oupao.wiz.FlyH.sh = v2;
                com.oupao.wiz.FlyH.den = v3;
                try {
                    // scaledTouchSlop 在公开 SDK 里不可见 → 反射取；取不到按 8dp 兜底
                    var vc = android.view.ViewConfiguration.get(v0_0);
                    var mSlop = android.view.ViewConfiguration.class.getMethod("getScaledTouchSlop");
                    com.oupao.wiz.FlyH.slop = ((Number) mSlop.invoke(vc)).floatValue();
                } catch (Throwable ignore) {
                    com.oupao.wiz.FlyH.slop = (v3 * 8.0f);
                }
                return;
            }
        }
        com.oupao.wiz.FlyH.sw = 1216;
        com.oupao.wiz.FlyH.sh = 2640;
        com.oupao.wiz.FlyH.den = 3.5f;
        com.oupao.wiz.SLog.v("Fly: dims \u7528\u515c\u5e95 1216x2640");
        return;
    }
    public static void go(final android.content.Intent p10, final String p11)
    {
        // 3303：所有开窗请求统一走 ZoomGate：
        //   - 串行执行（上一扇窗真正出现后再开下一扇）；
        //   - 开窗前收敛 / 开窗后复查都在那里；
        //   - 同一应用已有可见小窗 → 只聚焦；
        //   - 忙时只保留最新的请求。
        com.oupao.wiz.ZoomGate.request(p11, -1, new com.oupao.wiz.ZoomGate.Opener() {
            public void open() {
                com.oupao.wiz.FlyH.goNow(p10, p11);
            }
        });
    }

    static void goNow(android.content.Intent p10, String p11)
    {
        android.content.Context v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.FlyH.ctx;
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.SLog.e("Fly: \u5c0f\u7a97\u542f\u52a8\u5931\u8d25\uff0c\u9000\u56de\u666e\u901a\u542f\u52a8", __e_v0_1);
            try {
                var v1_3 = new android.os.Bundle();
                v1_3.putInt("android.activity.windowingMode", 100);
                v1_3.putInt("android:activity.mZoomLaunchFlags", 5);
                // 2851：ColorOS 的启动契约——LaunchScenario 缺了就退全屏
                v1_3.putInt("androidx.activity.LaunchScenario", 1);
                v1_3.putBoolean("androidx.activity.AdjustInputMethod", true);
                com.oupao.wiz.Cfg.sysContext().startActivity(p10, v1_3);
                return;
            } catch (Throwable v0) {
                return;
            }
        }
        if (v0_0 == null) {
            v0_0 = com.oupao.wiz.Cfg.sysContext();
        }
        try {
            v0_0.getPackageManager().getApplicationInfo(p11, 0);
            var v3_0 = android.app.ActivityOptions.makeBasic();
            v3_0.setLaunchDisplayId(0);
            var v3_1 = v3_0.toBundle();
            v3_1.putInt("extra_window_mode", 100);
            v3_1.putInt("android.activity.splashScreenStyle", 1);
            try {
                var v4_4 = Class.forName("com.oplus.zoomwindow.OplusZoomWindowManager");
                var v5_4 = com.oupao.wiz.Rt.invoke(com.oupao.wiz.Rt.methodByName(v4_4, "getInstance", 0), null, null);
                Object[] v6_3 = new Object[4];
                v6_3[0] = p10;
                v6_3[1] = v3_1;
                v6_3[2] = Integer.valueOf(0);
                v6_3[3] = "com.coloros.smartsidebar";
                var v4_5 = com.oupao.wiz.Rt.methodByName(v4_4, "startZoomWindow", 4);
                if (v4_5 != null) {
                    // 3189：见 startZoomWhenReady 的注释 —— ROM 在"桌面转场中"会把这次启动
                    // 推迟到转场结束（另有 2 秒兜底 timer），推迟执行时 topTask 已经变了。
                    // 连续呼出时只保留最后一次请求（后一次取代前一次），避免同时开出多扇窗。
                    int myGen = (++com.oupao.wiz.FlyH.zoomGen);
                    // ★ 3339：**全屏停在二级页**（微信朋友圈/聊天页、QQ 聊天页）时不走原地转换。
                    //   原地转会把"二级页本身"变成小窗；用户要的是「全屏二级页留着 + 小窗显示主页」
                    //   （老 smali 版 v212 的 Split 机制）。这里先布防（记 splitTask/splitClass），
                    //   再新开一扇小窗 —— 之后在二级页按返回会被 SplitGuard 接住，回到主界面。
                    if (com.oupao.wiz.SplitGuard.isSecondaryFullscreen(p11, 0)) {
                        com.oupao.wiz.SplitGuard.armFromCurrent(p11);
                        com.oupao.wiz.SLog.k("Fly: 二级页 " + p11 + " -> 平行小窗(主页)");
                        com.oupao.wiz.FlyH.startZoomWhenReady(v5_4, v4_5, v6_3, 38, myGen, p11);
                        return;
                    }
                    // 3241：目标就是"当前全屏前台应用"时，优先走 ROM 自己的全屏→小窗过渡
                    //   （FlexibleWindowManagerService.switchFocusedTaskToZoom → fullscreenToZoom，
                    //    内部会建一个 Transition(1106) 并 setFullTaskChangingToZoom(true)，有动画）。
                    //   直接 startZoomWindow 的话 ROM 是把整屏任务瞬间缩成小窗，没有任何过渡。
                    if (com.oupao.wiz.FlyH.tryFullscreenToZoom(p11, v5_4, v4_5, v6_3, myGen)) {
                        return;
                    }
                    com.oupao.wiz.FlyH.startZoomWhenReady(v5_4, v4_5, v6_3, 38, myGen, p11);
                    return;
                } else {
                    com.oupao.wiz.SLog.i("Fly: zw no method startZoomWindow");
                    return;
                }
            } catch (Throwable tcls) {
                com.oupao.wiz.SLog.e("Fly: \u5c0f\u7a97\u542f\u52a8\u53cd\u5c04\u5931\u8d25(OplusZoomWindowManager)", tcls);
            }
        } catch (Throwable tgo) {
            com.oupao.wiz.SLog.e("Fly: \u5c0f\u7a97\u542f\u52a8\u5931\u8d25(applicationInfo/opts) pkg=" + p11, tgo);
        }
    }

    /**
     * 3189：等"桌面转场"结束再启动小窗。
     *
     * 反编译 `OplusZoomWindowManagerService.startZoomWindow` 后确认：它在启动前会看
     * `getHomeActivity().isInTransition()`，在转场中就**不启动**，而是把这次请求挂到
     * `setOnHomeTransitionListener` 上（另有 2000ms 兜底 timer），**并且立刻返回 0**。
     * 日志实证：`OplusZoomWindowManagerService: wait for HomeTransitionFinish startZoomWindow`
     * → `startZoomWindow when HomeTransitionFinish`。表现就是"选了应用没反应，过一会才弹出来"。
     *
     * 更糟的是推迟执行时 topTask 已经不是按下那一刻的任务了，ROM 的 `updateTaskForZoom`
     * 会按"当时的 top 任务"做 setWindowingMode(0) / exitZoomWindow /
     * restartProcessIfVisibleIfNeeded —— 用户看到的就是"顺带把正在用的全屏应用关掉了"。
     *
     * 所以我们自己按 40ms 轮询等转场结束（最多 1.5s）再调；启动时刻的 top 任务就是桌面，
     * ROM 没有机会动错窗口。
     */
    static boolean homeInTransition() {
        try {
            Object w = com.oupao.wiz.Hooks.wms();
            Object atms = com.oupao.wiz.Rt.get(w, "mAtms");
            if (atms == null) {
                atms = com.oupao.wiz.Rt.get(w, "mAtmService");
            }
            Object sup = com.oupao.wiz.Rt.get(atms, "mTaskSupervisor");
            Object root = com.oupao.wiz.Rt.get(sup, "mRootWindowContainer");
            Object da = com.oupao.wiz.Rt.callNoArg(root, "getDefaultTaskDisplayArea");
            Object home = com.oupao.wiz.Rt.callNoArg(da, "getHomeActivity");
            if (home == null) {
                return false;
            }
            return Boolean.TRUE.equals(com.oupao.wiz.Rt.callNoArg(home, "isInTransition"));
        } catch (Throwable t) {
            return false;
        }
    }

    /** 3189：启动请求代号。连续呼出时只有最后一次有效（避免一次开出多扇小窗）。 */
    static volatile int zoomGen = 0;

    // 3254：上一次「全屏→小窗」过渡的 task 与时刻。
    //   实测（3253，快速来回）：同一个 task 在 2 秒内被连续发起两次过渡，
    //   ROM 把后一次合并/丢弃（`No transition roots … so abort`），
    //   结果窗口瞬间变成小窗 → 用户看到"没动效 + 闪一下"。
    private static volatile int lastTransTid;
    private static volatile long lastTransAt;

    /** 同一个 task 的过渡还在播 → 返回还需要等的毫秒数（0 = 可以立刻发起） */
    static long sameTaskTransitionBusy(int tid)
    {
        if ((tid > 0) && (tid == lastTransTid)) {
            long d = 800L - (android.os.SystemClock.uptimeMillis() - lastTransAt);
            if (d > 0L) {
                return d;
            }
        }
        return 0L;
    }

    /**
     * 3294：按 taskId 取 Task（用于从 RunningTaskInfo 反查真实 Task）。
     * `RootWindowContainer.anyTaskForId(int)` / `anyTaskForId(int,int)` 两个版本都试。
     */
    static Object taskById(int id) {
        try {
            Object w = com.oupao.wiz.Hooks.wms();
            Object atms = com.oupao.wiz.Rt.get(w, "mAtms");
            if (atms == null) {
                atms = com.oupao.wiz.Rt.get(w, "mAtmService");
            }
            Object root = com.oupao.wiz.Rt.get(atms, "mRootWindowContainer");
            Object t = com.oupao.wiz.Rt.callLoose(root, "anyTaskForId",
                    new Object[]{Integer.valueOf(id)});
            if (t != null) {
                return t;
            }
            return com.oupao.wiz.Rt.callLoose(root, "anyTaskForId",
                    new Object[]{Integer.valueOf(id), Integer.valueOf(0)});
        } catch (Throwable ignore) {
            return null;
        }
    }

    /**
     * 3294：按包名/用户找**全屏（非缩放态）**的 task。
     *
     * 场景：全屏 A 后面有个普通小窗 B 在前台，选 A 时 `topTask()` 是 B、
     * `resumedTask()` 也可能还是 B；但 ROM 的 `switchFocusedTaskToZoom(A)` 只要求
     * 传进去的是 `getRunningTaskWithoutZoomAndPip()` 那个全屏 task。
     * 这里用 `ActivityTaskManager.getTasks()` 找到 A 的 taskId，再反查 Task。
     */
    static Object fullscreenTaskOf(final String pkg, final int userId) {
        try {
            if (pkg == null) {
                return null;
            }
            Class<?> c = Class.forName("android.app.ActivityTaskManager");
            Object svc = com.oupao.wiz.Rt.invoke(
                    com.oupao.wiz.Rt.methodByName(c, "getService", 0), null, null);
            Object list = com.oupao.wiz.Rt.callLoose(svc, "getTasks",
                    new Object[]{Integer.valueOf(30)});
            if (!(list instanceof java.util.List)) {
                return null;
            }
            for (Object info : (java.util.List<?>) list) {
                Object vis = com.oupao.wiz.Rt.get(info, "isVisible");
                if ((vis instanceof Boolean) && (!((Boolean) vis))) {
                    continue;
                }
                Object top = com.oupao.wiz.Rt.get(info, "topActivity");
                Object pn = com.oupao.wiz.Rt.callNoArg(top, "getPackageName");
                if (!(pn instanceof String) || (!pkg.equals(pn))) {
                    continue;
                }
                Object id = com.oupao.wiz.Rt.get(info, "taskId");
                if (!(id instanceof Number)) {
                    continue;
                }
                Object t = taskById(((Number) id).intValue());
                if (t == null) {
                    continue;
                }
                if (!pkg.equals(com.oupao.wiz.Win.taskPkg(t))) {
                    continue;
                }
                if ((userId >= 0)
                        && ((com.oupao.wiz.Win.taskUid(t) / 100000) != userId)) {
                    continue;
                }
                if (com.oupao.wiz.Win.zoomState(t) != 0) {
                    continue;                       // 只要全屏 task，小窗 task 不算
                }
                com.oupao.wiz.SLog.i("Fly: fullscreenTaskOf 命中 pkg=" + pkg
                        + " tid=" + com.oupao.wiz.Win.taskId(t)
                        + (userId >= 0 ? (" u=" + userId) : ""));
                return t;
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    /** 3241：当前焦点的 root task（应用任务），拿不到返回 null */
    static Object topTask() {
        try {
            Object w = com.oupao.wiz.Hooks.wms();
            Object atms = com.oupao.wiz.Rt.get(w, "mAtms");
            if (atms == null) {
                atms = com.oupao.wiz.Rt.get(w, "mAtmService");
            }
            Object root = com.oupao.wiz.Rt.get(atms, "mRootWindowContainer");
            Object t = com.oupao.wiz.Rt.callNoArgQ(root, "getTopDisplayFocusedRootTask");
            if (t != null) {
                return t;
            }
            Object da = com.oupao.wiz.Rt.callNoArgQ(root, "getDefaultTaskDisplayArea");
            t = com.oupao.wiz.Rt.callNoArgQ(da, "getTopRootTask");
            if (t != null) {
                return t;
            }
            Object act = com.oupao.wiz.Rt.get(atms, "mLastResumedActivity");
            t = com.oupao.wiz.Rt.callNoArgQ(act, "getTask");
            if (t != null) {
                return t;
            }
            com.oupao.wiz.SLog.i("Fly: topTask 取不到 w=" + (w != null)
                    + " atms=" + (atms != null) + " root=" + (root != null)
                    + " da=" + (da != null) + " act=" + (act != null));
        } catch (Throwable ignore) {
        }
        return null;
    }

    /**
     * 3244：最近 resume 的 Activity 所在的 task。
     *
     * 有贴边小窗时 `getTopDisplayFocusedRootTask()` 可能返回那个浮条任务，
     * 用它兜一层来定位"真正的全屏前台应用"。
     */
    static Object resumedTask() {
        try {
            Object w = com.oupao.wiz.Hooks.wms();
            Object atms = com.oupao.wiz.Rt.get(w, "mAtms");
            if (atms == null) {
                atms = com.oupao.wiz.Rt.get(w, "mAtmService");
            }
            Object act = com.oupao.wiz.Rt.get(atms, "mLastResumedActivity");
            return com.oupao.wiz.Rt.callNoArgQ(act, "getTask");
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 3241：**全屏 → 小窗的过渡动画**（用户反馈"直接被拉成小窗，没有任何过渡，很生硬"）。
     *
     * 反编译 `FlexibleWindowManagerService`：
     * ```
     * switchFocusedTaskToZoom(Task)        // 焦点任务在全屏时
     *   └─ fullscreenToZoom(Task)
     *        ├─ notifyFlexibleTaskEvent(2015)
     *        ├─ new Transition(1106, …) + startCollectOrQueue(…)
     *        └─ setFullTaskChangingToZoom(true) / collect / setReady
     * ```
     * 也就是说 ROM 自己有一条「整屏任务变成小窗」的过渡动画（快捷键/手势用的就是它），
     * 我们直接借用，比 `startZoomWindow` 把任务瞬间缩成小窗要自然得多。
     *
     * 只在**目标包 == 当前全屏前台应用、且当前没有小窗**时启用；其余情况原路返回 false。
     * 发起后 700ms 校验：没出现小窗就回退到 `startZoomWindow`（不会卡死）。
     */
    static boolean tryFullscreenToZoom(final String pkg, final Object mgr,
                                       final java.lang.reflect.Method method,
                                       final Object[] args, final int gen) {
        // 3250：普通（非分身）启动也要校验用户 = 主用户 0。
        //   否则"分身微信全屏 + 从菜单选主微信"会把分身那个 task 转成小窗（和 3249 的坑对称）。
        return tryFullscreenToZoom(pkg, 0, mgr, method, args, null, gen, 8);
    }

    /**
     * 3246：分身（u=999）也要能走这条过渡。
     *
     * 分身走的是 `cloneLaunch` → `zoomOpen` → `startZoomWindow`，根本没进这里，
     * 所以 QQ/微信分身"全屏→小窗"一直没有动效（普通版有）。
     * 现在 `cloneLaunch` 先调本方法：`userId >= 0` 时同时校验**焦点窗口的用户**和
     * **目标 task 的用户**，避免把主应用的分身/主应用搞混。
     *
     * @param fallback 过渡没生效时的兜底（分身传 `zoomOpen(intent, userId)`；普通路径传 null，
     *                 用 `startZoomWhenReady` 兜底）
     */
    static boolean tryFullscreenToZoom(final String pkg, final int userId,
                                       final Object mgr, final java.lang.reflect.Method method,
                                       final Object[] args, final Runnable fallback,
                                       final int gen) {
        return tryFullscreenToZoom(pkg, userId, mgr, method, args, fallback, gen, 8);
    }

    /**
     * 3245：`tries` = 目标 task 还在自己的过渡动画里时，还能重试几次（每次 60ms）。
     *
     * 实测：从迷你窗上滑放成全屏（`swipeDownToMaximized` → `resetFlexibleTaskToFullScreen`
     * → `fullscreenFlexibleTask sourceFrom:65536`）大约 0.6 秒内，用户再从扇形菜单选应用，
     * 这时 ROM 的 `fullscreenToZoom` 会被 `startCollectOrQueue` 排队/合并 → 合并掉就没有动画，
     * 极端情况窗口只画出一半。所以先等这个 task 的自定义过渡结束再发起。
     */
    static boolean tryFullscreenToZoom(final String pkg, final int userId,
                                       final Object mgr, final java.lang.reflect.Method method,
                                       final Object[] args, final Runnable fallback,
                                       final int gen, final int tries) {
        try {
            if ((pkg == null) || ((mgr == null) && (fallback == null))) {
                return false;
            }
            // 3244：**不再用"有没有小窗"挡过渡**。
            //   只要目标就是当前全屏前台应用（下面两条门），ROM 的 switchFocusedTaskToZoom
            //   就能用：它取 getRunningTaskWithoutZoomAndPip()（排除缩放态任务），
            //   fullscreenToZoom 本身也不检查已有浮窗 —— 贴边小窗、甚至普通小窗浮在前面时，
            //   后面的全屏应用仍然是那个 running task，过渡照样成立。
            //   3241~3243 在这里用 winOpen() 一刀切，导致"有贴边小窗时全屏转小窗没有动效"。
            // 3293：有浮窗时焦点窗口可能就是那个小窗（普通小窗可获焦，不只是贴边浮条），
            //   此时 `fgPkgWin()` 返回的是小窗应用而不是后面的全屏应用。
            //   下面两个“焦点必须是目标”的门不能一刀切，否则全屏 A + 浮窗 B 时选 A
            //   会被判成“前台不是目标”而跳过过渡 → 用户看到的就是“没动效”。
            //   有浮窗时放行，真正的目标校验交给后面的 topTask/resumedTask + zoomState + taskUid。
            final boolean floatOpen = com.oupao.wiz.Hooks.winOpen();
            if (floatOpen) {
                com.oupao.wiz.SLog.i("Fly: 全屏→小窗 已有小窗，仍尝试 ROM 过渡 pkg=" + pkg);
            }
            // 3249：**同一个应用**已经有小窗了 → 不能再转一个小窗出来。
            //   否则 ROM 到上限会把原来那个关掉/贴边（实测 `NoDock: 已关掉本要贴边的窗口`），
            //   一开一关两套动画叠着演 = 连续呼出时明显卡顿。交给 startZoomWindow 聚焦已有窗口。
            if (com.oupao.wiz.Hooks.samePkgZoomOpen(pkg, userId)) {
                com.oupao.wiz.SLog.i("Fly: 全屏→小窗 跳过(该应用已有小窗) pkg=" + pkg
                        + (userId >= 0 ? (" u=" + userId) : ""));
                return false;
            }
            String fg = com.oupao.wiz.Sug.fgPkgWin();
            if (fg == null) {
                fg = com.oupao.wiz.Sug.fgPkg();
            }
            if (!pkg.equals(fg)) {
                // 3294：**不要再用焦点包名否决**。普通小窗可获焦，fg 会是小窗应用；
                //   真伪交给下面的 topTask/resumedTask/fullscreenTaskOf + zoomState + taskUid。
                com.oupao.wiz.SLog.i("Fly: 全屏→小窗 前台不是目标(" + fg
                        + ") → 用目标全屏 task 继续尝试 pkg=" + pkg);
            }
            if (userId >= 0) {
                int fu = com.oupao.wiz.Sug.fgUserWin();
                // 3247：`fgUserWin()` 取不到时返回 -1（扇形菜单刚收起、焦点窗口还没落定就会这样），
                //   这种情况**不能当成"用户不符"**，否则分身永远进不来（日志 `跳过(前台用户不符) fg=-1`）。
                //   真正的权威判据是下面"目标 task 自己的用户"。
                if (fu >= 0 && fu != userId) {
                    // 3294：同理，焦点可能在别的小窗/别的用户上，后面用 task 的 uid 严格判定。
                    com.oupao.wiz.SLog.i("Fly: 全屏→小窗 焦点用户不是目标(u=" + fu
                            + ") → 用 task 用户判定 pkg=" + pkg + " want=" + userId);
                }
                if (fu < 0) {
                    com.oupao.wiz.SLog.i("Fly: 全屏→小窗 焦点用户未知(-1) → 用 task 用户判定 pkg=" + pkg
                            + " want=" + userId);
                }
            }
            // 3244：先取任务，再判"是不是全屏"。
            //   有贴边小窗时 `getTopDisplayFocusedRootTask()` 可能指向那个浮条任务，
            //   所以要再用"最近 resume 的 Activity 所在 task"兜一层。
            Object task = com.oupao.wiz.FlyH.topTask();
            if ((task == null) || !pkg.equals(com.oupao.wiz.Win.taskPkg(task))) {
                Object alt = com.oupao.wiz.FlyH.resumedTask();
                if ((alt != null) && pkg.equals(com.oupao.wiz.Win.taskPkg(alt))) {
                    com.oupao.wiz.SLog.i("Fly: 全屏→小窗 用 resumedTask 定位 pkg=" + pkg
                            + " tid=" + com.oupao.wiz.Win.taskId(alt));
                    task = alt;
                } else {
                    // 3294：top/resumed 都被小窗占了 → 直接按包名找全屏 task
                    Object fs = com.oupao.wiz.FlyH.fullscreenTaskOf(pkg, userId);
                    if (fs != null) {
                        task = fs;
                    }
                }
            } else if (com.oupao.wiz.Win.zoomState(task) != 0) {
                // topTask 命中了同包的小窗 task；如果还有一个全屏 task，优先转全屏那个
                Object fs = com.oupao.wiz.FlyH.fullscreenTaskOf(pkg, userId);
                if (fs != null) {
                    task = fs;
                }
            }
            if (task == null) {
                com.oupao.wiz.SLog.i("Fly: 全屏→小窗 跳过(取不到 topTask)");
                return false;
            }
            if (!pkg.equals(com.oupao.wiz.Win.taskPkg(task))) {
                com.oupao.wiz.SLog.i("Fly: 全屏→小窗 跳过(task 包名不符) pkg=" + pkg
                        + " task=" + com.oupao.wiz.Win.taskPkg(task));
                return false;
            }
            if (userId >= 0) {
                int tu = com.oupao.wiz.Win.taskUid(task) / 100000;
                // 3250：**必须严格相等**。
                //   3247 为了兼容"取不到用户"放过了 tu==0，结果主微信全屏时选分身 →
                //   把主微信那个 task（user 0）转成了小窗（实测日志 want=998 / tid=33119）。
                if (tu != userId) {
                    com.oupao.wiz.SLog.i("Fly: 全屏→小窗 跳过(task 用户不符) pkg=" + pkg
                            + " want=" + userId + " task=" + tu);
                    return false;
                }
            }
            // 3254：同一个 task 上一次过渡还在播（<800ms）→ 等它演完再发起。
            //   否则 ROM 会合并/丢弃（日志 `No transition roots … so abort`），
            //   窗口瞬间变小窗 = 用户说的"没动效 + 闪一下"。
            {
                long busy = com.oupao.wiz.FlyH.sameTaskTransitionBusy(com.oupao.wiz.Win.taskId(task));
                if (busy > 0L) {
                    if ((gen == com.oupao.wiz.FlyH.zoomGen) && (tries > 0)) {
                        com.oupao.wiz.SLog.i("Fly: 全屏→小窗 同一窗口过渡还没完 → " + busy
                                + "ms 后再试 pkg=" + pkg);
                        android.os.Handler bh = com.oupao.wiz.Hooks.main();
                        if (bh != null) {
                            final long bdelay = busy;
                            bh.postDelayed(new Runnable() {
                                public void run() {
                                    com.oupao.wiz.FlyH.tryFullscreenToZoom(pkg, userId, mgr, method,
                                            args, fallback, gen, tries - 1);
                                }
                            }, bdelay);
                            return true;                   // 已接管
                        }
                    }
                    com.oupao.wiz.SLog.i("Fly: 全屏→小窗 跳过(该窗口过渡中, 等不了) pkg=" + pkg);
                    return false;
                }
            }
            // 3260：刚做完贴边动画（<700ms）→ 等通道空下来再发起。
            //   否则 ROM 会把这次过渡合并掉：窗口 12ms 就落位、完全没有动画
            //   （实测：`DockKeep 贴边…挡下暂停` 后紧接着的那次转换就是这种）。
            {
                long dockAt = com.oupao.wiz.Dock.markedAt;
                if (dockAt > 0L) {
                    long left = 700L - (android.os.SystemClock.uptimeMillis() - dockAt);
                    if (left > 0L) {
                        if ((gen == com.oupao.wiz.FlyH.zoomGen) && (tries > 0)) {
                            com.oupao.wiz.SLog.i("Fly: 全屏→小窗 刚贴边完 → " + left
                                    + "ms 后再发起 pkg=" + pkg);
                            android.os.Handler dh = com.oupao.wiz.Hooks.main();
                            if (dh != null) {
                                final long ddelay = left;
                                dh.postDelayed(new Runnable() {
                                    public void run() {
                                        com.oupao.wiz.FlyH.tryFullscreenToZoom(pkg, userId, mgr, method,
                                                args, fallback, gen, tries - 1);
                                    }
                                }, ddelay);
                                return true;
                            }
                        }
                        com.oupao.wiz.SLog.i("Fly: 全屏→小窗 跳过(刚贴边完, 等不了) pkg=" + pkg);
                        return false;
                    }
                }
            }
            if (!com.oupao.wiz.Sug.fgFull()) {
                // 有贴边小窗时焦点 root task 可能就是那个浮条 → fgFull() 会为假。
                // 只要目标 task 自己不在缩放态，就仍然让 ROM 去判（它内部还会再校验一次
                // getRunningTaskWithoutZoomAndPip，不匹配就直接 return，不会乱开窗）。
                if (com.oupao.wiz.Win.zoomState(task) != 0) {
                    com.oupao.wiz.SLog.i("Fly: 全屏→小窗 跳过(前台非全屏) pkg=" + pkg);
                    return false;
                }
                com.oupao.wiz.SLog.i("Fly: 全屏→小窗 焦点不在全屏任务(有浮窗) → 仍尝试 ROM 过渡 pkg=" + pkg);
            }
            // 3245：目标 task 还在自己的过渡里（迷你→全屏 / 缩放调整…）→ 等它结束再发起，
            //   否则 ROM 会把这次 fullscreenToZoom 合并进上一条过渡（没动画）或画一半。
            int ft = com.oupao.wiz.Win.flexTransition(task);
            if (ft != 0) {
                if (gen != com.oupao.wiz.FlyH.zoomGen) {
                    return true;                       // 已被后一次呼出取代
                }
                if (tries > 0) {
                    com.oupao.wiz.SLog.i("Fly: 全屏→小窗 目标还在过渡中(0x" + Integer.toHexString(ft)
                            + ") → 60ms 后重试 pkg=" + pkg + " 剩 " + tries);
                    android.os.Handler h = com.oupao.wiz.Hooks.main();
                    if (h != null) {
                        h.postDelayed(new Runnable() {
                            public void run() {
                                com.oupao.wiz.FlyH.tryFullscreenToZoom(pkg, userId, mgr, method,
                                        args, fallback, gen, tries - 1);
                            }
                        }, 60L);
                        return true;                   // 已接管（goNow 不要再走 startZoomWindow）
                    }
                }
                com.oupao.wiz.SLog.i("Fly: 全屏→小窗 过渡一直没结束 → 直接 startZoomWindow pkg=" + pkg);
                if (tries < 8) {
                    // 这次是"延迟重试"进来的：goNow 早已 return，必须自己兜底开窗
                    if ((mgr != null) && (method != null)) {
                        com.oupao.wiz.FlyH.startZoomWhenReady(mgr, method, args, 38, gen, pkg);
                    } else if (fallback != null) {
                        fallback.run();
                    }
                    return true;
                }
                return false;
            }
            Class<?> c = com.oupao.wiz.Rt.cls(com.oupao.wiz.Hooks.CL,
                    "com.android.server.wm.FlexibleWindowManagerService");
            if (c == null) {
                com.oupao.wiz.SLog.i("Fly: 全屏→小窗 跳过(无 FlexibleWindowManagerService)");
                return false;
            }
            var gi = com.oupao.wiz.Rt.methodByName(c, "getInstance", 1);
            if (gi == null) {
                com.oupao.wiz.SLog.i("Fly: 全屏→小窗 跳过(无 getInstance/1)");
                return false;
            }
            Object svc = com.oupao.wiz.Rt.invoke(gi, null, new Object[]{null});
            if (svc == null) {
                com.oupao.wiz.SLog.i("Fly: 全屏→小窗 跳过(getInstance 返回 null)");
                return false;
            }
            var m = com.oupao.wiz.Rt.methodByName(c, "switchFocusedTaskToZoom", 1);
            if (m == null) {
                com.oupao.wiz.SLog.i("Fly: 未找到 switchFocusedTaskToZoom");
                return false;
            }
            final Object ftask = task;
            final int ftid = com.oupao.wiz.Win.taskId(task);
            // ★ 全屏→小窗：上限=1 时先关旧小窗，再发起 ROM 的整屏→小窗过渡。
            //   3304 的 ZoomGate 是“先开新窗再关旧窗”，这里如果照做，用户会看到
            //   新小窗已经出来了、旧小窗才在后面退场。此时目标全屏 task 还在，
            //   先关旧窗不会露桌面，正好符合 C16 的旧时序。
            //
            // 3464：**不再固定等 300ms**。C17 实测旧窗的 TO_BACK 退场过渡要 ~530ms，
            //   300ms 就发 switchFocusedTaskToZoom 必然和它在同时播，Shell 会把
            //   后者 merge 进前一条（"ready while … still animating … can be merged"）
            //   → fullscreen→zoom 动画被吞、窗口直接落位成小窗。
            //   现在等旧窗真正退场结束（task 不可见/回到全屏 + 过渡控制器空闲）再转。
            final int oldTid = com.oupao.wiz.ZoomGate.preCloseForFullscreenToZoom(ftid);
            final Runnable fire = new Runnable() {
                public void run() {
                    com.oupao.wiz.FlyH.fireFullscreenToZoom(
                            m, svc, ftask, ftid, pkg, mgr, method, args, gen, fallback);
                }
            };
            if (oldTid > 0) {
                com.oupao.wiz.SLog.i("Fly: 全屏→小窗 先关旧窗 tid=" + oldTid
                        + "，等退场结束再转 pkg=" + pkg + " tid=" + ftid);
                com.oupao.wiz.ZoomGate.whenOldClosed(oldTid, gen, fire);
            } else {
                // 没有旧窗：若上一条过渡还在播，也稍等（很短，避免被 merge）
                com.oupao.wiz.ZoomGate.whenTransitionsIdle(gen, fire);
            }
            return true;
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Fly: 全屏→小窗 过渡异常", t);
            return false;
        }
    }

    /** 真正发起 `switchFocusedTaskToZoom`，并做 700ms 成败校验/回退 */
    private static void fireFullscreenToZoom(final java.lang.reflect.Method m, final Object svc,
                                             final Object ftask, final int ftid, final String pkg,
                                             final Object mgr, final java.lang.reflect.Method method,
                                             final Object[] args, final int gen,
                                             final Runnable fallback) {
        try {
            // 3254：记下来，800ms 内同一个 task 不再重复发起过渡（会被 ROM 合并成"无动画"）
            com.oupao.wiz.FlyH.lastTransTid = ftid;
            com.oupao.wiz.FlyH.lastTransAt = android.os.SystemClock.uptimeMillis();
            com.oupao.wiz.Rt.invoke(m, svc, new Object[]{ftask});
            com.oupao.wiz.SLog.i("Fly: 全屏→小窗 走 ROM 过渡 pkg=" + pkg
                    + " tid=" + ftid);
            android.os.Handler h = com.oupao.wiz.Hooks.main();
            if (h != null) {
                h.postDelayed(new Runnable() {
                    public void run() {
                        try {
                            // 3244：不能用 winOpen() 判成败 —— 有贴边小窗时它恒为真，
                            //   过渡失败也会被误判成"成功"（窗口其实是 700ms 后才被别的路径开出来）。
                            //   改成看"目标 task 自己有没有进入缩放态 / 有没有注册 caption"。
                            boolean ok = (com.oupao.wiz.Win.zoomState(ftask) != 0)
                                    || (com.oupao.wiz.CapR.byTid(ftid) != null);
                            if (ok) {
                                com.oupao.wiz.SLog.i("Fly: 全屏→小窗 过渡成功 pkg=" + pkg
                                        + " tid=" + ftid);
                                com.oupao.wiz.Win.dumpTop("过渡后");   // 3255 诊断
                                return;
                            }
                            com.oupao.wiz.SLog.i("Fly: 全屏→小窗 过渡没生效 → 回退 pkg=" + pkg);
                            if ((mgr != null) && (method != null)) {
                                com.oupao.wiz.FlyH.startZoomWhenReady(mgr, method, args, 38, gen, pkg);
                            } else if (fallback != null) {
                                fallback.run();
                            }
                        } catch (Throwable ignore) {
                        }
                    }
                }, 700L);
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Fly: 全屏→小窗 过渡异常", t);
        }
    }

    static void startZoomWhenReady(final Object mgr, final java.lang.reflect.Method method,
                                   final Object[] args, final int tries, final int gen,
                                   final String pkg) {
        startZoomWhenReady(mgr, method, args, tries, gen, pkg, 2);
    }

    /**
     * 3251：`retry` = `startZoomWindow` 返回非 0 时还能补几次（每次 300ms）。
     *
     * 实测（3250，连续快速呼出）：窗口正在退场时 ROM 直接 `return 2` 拒绝启动，
     * 我们只打一行 `startZoomWindow ret=2` 就放弃 → 用户看到"点了没反应"，
     * 于是再点一次，又叠一层开/关，越点越卡。退场一般 300~600ms 结束，补两次就够。
     */
    static void startZoomWhenReady(final Object mgr, final java.lang.reflect.Method method,
                                   final Object[] args, final int tries, final int gen,
                                   final String pkg, final int retry) {
        try {
            if (gen != com.oupao.wiz.FlyH.zoomGen) {
                com.oupao.wiz.SLog.i("Fly: 已被后一次呼出取代，放弃 pkg=" + pkg);
                return;
            }
            if ((tries > 0) && com.oupao.wiz.FlyH.homeInTransition()) {
                if (tries == 38) {
                    com.oupao.wiz.SLog.i("Fly: 桌面转场中 → 等转场结束再启动 pkg=" + pkg);
                }
                android.os.Handler h = com.oupao.wiz.Hooks.main();
                if (h != null) {
                    h.postDelayed(new Runnable() {
                        public void run() {
                            com.oupao.wiz.FlyH.startZoomWhenReady(mgr, method, args, tries - 1, gen, pkg, retry);
                        }
                    }, 40L);
                    return;
                }
            }
            if (tries <= 0) {
                com.oupao.wiz.SLog.i("Fly: 等转场超时 1.5s，直接启动 pkg=" + pkg);
            } else if (tries < 38) {
                com.oupao.wiz.SLog.i("Fly: 转场已结束，启动 pkg=" + pkg
                        + " 等了 " + ((38 - tries) * 40) + "ms");
            }
            Object r = com.oupao.wiz.Rt.invoke(method, mgr, args);
            com.oupao.wiz.SLog.i("Fly: startZoomWindow ret=" + r);
            int code = (r instanceof Number) ? ((Number) r).intValue() : 0;
            // 3391：ActivityStarter.execute() 的 2/3 不是失败：
            //   2 = START_TASK_TO_FRONT，3 = START_DELIVERED_TO_TOP，都是已启动成功。
            //   之前把非 0 一律当“被拒”并 300ms 重试，会在第一个小窗正在入场时
            //   再调一次 startZoomWindow → 窗口重复入场的抖动。
            if ((code == 2) || (code == 3)) {
                com.oupao.wiz.SLog.i("Fly: startZoomWindow ret=" + code
                        + " 已成功(TASK_TO_FRONT/DELIVERED) 不再重试");
                return;
            }
            if ((code != 0) && (retry > 0)) {
                com.oupao.wiz.SLog.i("Fly: startZoomWindow 被拒(ret=" + code + ") → 300ms 后重试 pkg="
                        + pkg + " 剩 " + retry);
                android.os.Handler h = com.oupao.wiz.Hooks.main();
                if (h != null) {
                    h.postDelayed(new Runnable() {
                        public void run() {
                            com.oupao.wiz.FlyH.startZoomWhenReady(mgr, method, args, 38, gen, pkg, retry - 1);
                        }
                    }, 300L);
                }
                return;
            }
            com.oupao.wiz.SLog.i("Fly: startZoomWindow \u5df2\u8c03\u7528");
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Fly: startZoomWindow \u8c03\u7528\u5931\u8d25 pkg=" + pkg, t);
        }
    }
    /**
     * 手势期间的两个配置快照（flyMaster / flyRange）。
     *
     * 为什么要缓存：{@link FlyInput#post} 是 `Hooks.main().post(FlyQ)`，
     * 所以 {@link #handle} 跑在 **system_server 主 looper** 上，而它每次 MOVE 事件都被调
     * （120Hz 采样下一次边缘滑动手势就是上百次）。原来每个 MOVE 都读一遍
     * `Cfg.flyMaster()` + `Cfg.flyRange()`，即使 masterOn 已加 TTL，也还有
     * getInt/getStr 的 probe + 文件读。
     *
     * 语义：一次手势内配置恒定（配置在手势中途变的概率极低，且下一次 DOWN 会刷新）。
     * TTL 1s，与 Cfg.masterOn 同思路；action==0（DOWN）时强制刷新。
     */
    private static volatile long hCfgAt = 0L;
    private static volatile boolean hFlyMaster = true;
    private static volatile int hFlyRange = 0;

    private static void hCfgRefresh(boolean force) {
        long now = android.os.SystemClock.uptimeMillis();
        if (!force && (now - hCfgAt) <= 1000L) {
            return;
        }
        hCfgAt = now;
        hFlyMaster = com.oupao.wiz.Cfg.flyMaster();
        hFlyRange = com.oupao.wiz.Cfg.flyRange();
    }

    public static void handle(int p12, float p13, float p14)
    {
        // 2743：自绘菜单接管期间，旧菜单状态机完全不参与（否则两套菜单会同时画出来）
        // ★ 2745：旧菜单（FlyH 自己的 2003 窗口 + Bar 图标）**整体停用**，
        //   改由自绘的 FlyFan/FlyFanH 接管（用户要求"直接移除旧菜单"）。
        //   保留本文件其余能力（配置、图标、分身启动 cloneLaunch 等，FlyFanH 还在用）。
        // 2766：旧菜单恢复（呼出改由旁听驱动，菜单本体一行没改）
        if (p12 != 3) {
            try {
                if (!com.oupao.wiz.FlyH.stick) {
                    // DOWN(p12==0) 强制刷新一次；MOVE/UP 走 1s 快照
                    hCfgRefresh(p12 == 0);
                    if (hFlyMaster) {
                        if (p12 == 0) {
                            com.oupao.wiz.FlyH.dims();
                        }
                        var v0_3 = (((float) hFlyRange) * com.oupao.wiz.FlyH.den);
                        var v2_1 = (((float) com.oupao.wiz.FlyH.sh) - p14);
                        if (p12 != 0) {
                            if (2 != p12) {
                                if (com.oupao.wiz.FlyH.st == 0) {
                                    com.oupao.wiz.FlyH.st = 0;
                                } else {
                                    var v3_19 = ((int) com.oupao.wiz.FlyH.mx);
                                    com.oupao.wiz.FlyH.lg("Fly: up moved(px)", v3_19, 0, 0);
                                    if (v3_19 >= ((int) (com.oupao.wiz.FlyH.den * 6.0f))) {
                                        if (com.oupao.wiz.FlyH.st != 2) {
                                            com.oupao.wiz.FlyH.show(com.oupao.wiz.FlyH.side, p13, p14);
                                            var v0_5 = com.oupao.wiz.Bar.hit(p13, p14);
                                            com.oupao.wiz.FlyH.sel = v0_5;
                                            if (v0_5 >= 0) {
                                                com.oupao.wiz.FlyH.mark(v0_5);
                                                com.oupao.wiz.FlyH.commit();
                                                return;
                                            } else {
                                                com.oupao.wiz.FlyH.sel = -1;
                                                com.oupao.wiz.FlyH.stayOrClose();
                                                return;
                                            }
                                        } else {
                                            var v0_8 = com.oupao.wiz.Bar.hit(p13, p14);
                                            com.oupao.wiz.FlyH.sel = v0_8;
                                            if (v0_8 >= 0) {
                                                com.oupao.wiz.FlyH.mark(v0_8);
                                                com.oupao.wiz.FlyH.commit();
                                                return;
                                            } else {
                                                com.oupao.wiz.FlyH.sel = -1;
                                                com.oupao.wiz.FlyH.stayOrClose();
                                                return;
                                            }
                                        }
                                    } else {
                                        com.oupao.wiz.FlyH.lg("Fly: up too small, cancel", v3_19, v3_19, v3_19);
                                        com.oupao.wiz.FlyH.cancel();
                                        return;
                                    }
                                }
                            } else {
                                if (com.oupao.wiz.FlyH.st != 0) {
                                    float v4_17 = 0;
                                    if (com.oupao.wiz.FlyH.side != 1) {
                                        v4_17 = (com.oupao.wiz.FlyH.ox - p13);
                                    } else {
                                        v4_17 = (p13 - com.oupao.wiz.FlyH.ox);
                                    }
                                    var v5_9 = (com.oupao.wiz.FlyH.oy - p14);
                                    var v7_4 = (- (com.oupao.wiz.FlyH.den * 10.0f));
                                    if (v4_17 >= v7_4) {
                                        if (v5_9 >= v7_4) {
                                            var v6_11 = (v4_17 + v5_9);
                                            if (v6_11 > com.oupao.wiz.FlyH.mx) {
                                                com.oupao.wiz.FlyH.mx = v6_11;
                                            }
                                            if (com.oupao.wiz.FlyH.st != 2) {
                                                // ★ 2714 重构：判定统一走 FlyGeom（与 SystemUI 热区窗口同一套两轴阈值），
                                                //   原来是 dx+dy >= 6dp，太灵敏 —— 手指抖十几 px 就把菜单划出来了。
                                                if (com.oupao.wiz.FlyGeom.claimed(com.oupao.wiz.FlyH.side,
                                                        com.oupao.wiz.FlyH.ox, com.oupao.wiz.FlyH.oy,
                                                        p13, p14, com.oupao.wiz.FlyH.slop, com.oupao.wiz.FlyH.den)) {
                                                    com.oupao.wiz.FlyH.st = 2;
                                                    com.oupao.wiz.FlyH.act = true;
                                                    com.oupao.wiz.FlyH.lg("Fly: activate side/x", com.oupao.wiz.FlyH.side, ((int) p13), ((int) p14));
                                                    com.oupao.wiz.FlyH.warm();
                                                    com.oupao.wiz.FlyH.show(com.oupao.wiz.FlyH.side, p13, p14);
                                                    com.oupao.wiz.FlyH.reveal(com.oupao.wiz.FlyH.progressAt(p13, p14));
                                                    return;
                                                }
                                            } else {
                                                com.oupao.wiz.FlyH.reveal(com.oupao.wiz.FlyH.progressAt(p13, p14));
                                                com.oupao.wiz.FlyH.update(p13, p14);
                                                return;
                                            }
                                        } else {
                                            com.oupao.wiz.FlyH.lg("Fly: cancel up/inward", ((int) v5_9), ((int) v4_17), ((int) v4_17));
                                            com.oupao.wiz.FlyH.cancel();
                                            return;
                                        }
                                    } else {
                                        com.oupao.wiz.FlyH.lg("Fly: cancel inward/up", ((int) v4_17), ((int) v5_9), ((int) v5_9));
                                        com.oupao.wiz.FlyH.cancel();
                                        return;
                                    }
                                }
                            }
                        } else {
                            com.oupao.wiz.FlyH.lg("Fly: down x/y/r", ((int) p13), ((int) p14), ((int) v0_3));
                            com.oupao.wiz.FlyS.unpil();
                            com.oupao.wiz.FlyH.act = false;
                            com.oupao.wiz.FlyH.st = 0;
                            com.oupao.wiz.FlyH.sel = -1;
                            com.oupao.wiz.FlyH.mx = 0;
                            com.oupao.wiz.FlyH.side = 0;
                            if ((!com.oupao.wiz.Cfg.flyLeft()) || ((com.oupao.wiz.Hooks.gameBlock()) || (((p13 * p13) + (v2_1 * v2_1)) >= (v0_3 * v0_3)))) {
                                if ((com.oupao.wiz.Cfg.flyRight()) && ((!com.oupao.wiz.Hooks.gameBlock()) && ((((((float) com.oupao.wiz.FlyH.sw) - p13) * (((float) com.oupao.wiz.FlyH.sw) - p13)) + (v2_1 * v2_1)) < (v0_3 * v0_3)))) {
                                    com.oupao.wiz.FlyH.side = 2;
                                    com.oupao.wiz.FlyH.st = 1;
                                    com.oupao.wiz.FlyH.ox = p13;
                                    com.oupao.wiz.FlyH.oy = p14;
                                    com.oupao.wiz.FlyH.lg("Fly: armed R", 2, 2, 2);
                                    return;
                                }
                            } else {
                                com.oupao.wiz.FlyH.side = 1;
                                com.oupao.wiz.FlyH.st = 1;
                                com.oupao.wiz.FlyH.ox = p13;
                                com.oupao.wiz.FlyH.oy = p14;
                                com.oupao.wiz.FlyH.lg("Fly: armed L", 1, 1, 1);
                                return;
                            }
                        }
                    }
                    return;
                } else {
                    return;
                }
            } catch (Throwable __e_v0) {
                return;
            }
        } else {
            com.oupao.wiz.FlyH.cancel();
            com.oupao.wiz.FlyS.unpil();
            return;
        }
    }
    /**
     * 这一下没把菜单呼出来 → 当作"点击"，由我们注入一次干净的 DOWN+UP 还给下层。
     *
     * 为什么必须我们来做：Launcher 侧的闸门（FlyGate）为了不让"呼出菜单瞬间触发上滑手势"，
     * 把落在热区里的 DOWN 从桌面手势链里摘掉了；而 ColorOS 底部 77px 那一条带的触摸，
     * 本来就是靠桌面的 consumer 判定"不是上滑"之后再重放给下层 App 的。
     * 摘掉之后没人重放 → 热区里的点击会消失，所以在这里补上。
     *
     * 只在「DOWN 落在热区圆内 + 菜单没开 + 本次手势没激活过菜单」时重放，别的地方一律不动。
     */
    public static void replayTapIfMissed(float x, float y) {
        try {
            if (com.oupao.wiz.FlyH.act) {
                return;
            }
            if (com.oupao.wiz.FlyState.open() || (com.oupao.wiz.FlyH.win != null) || (com.oupao.wiz.FlyH.st == 2)) {
                return;
            }
            int lr = com.oupao.wiz.FlyS.lrNow();
            int r = com.oupao.wiz.FlyS.radiusNow();
            if ((lr == 0) || (r <= 0)) {
                return;
            }
            float w = (float) com.oupao.wiz.FlyH.sw;
            float h = (float) com.oupao.wiz.FlyH.sh;
            if ((w <= 0f) || (h <= 0f)) {
                android.content.Context c = com.oupao.wiz.Cfg.sysContext();
                if (c == null) {
                    return;
                }
                var dm = c.getResources().getDisplayMetrics();
                w = (float) dm.widthPixels;
                h = (float) dm.heightPixels;
            }
            if (com.oupao.wiz.FlyGeom.side(x, y, w, h, (float) r, lr) == com.oupao.wiz.FlyGeom.SIDE_NONE) {
                return;   // 不在热区：不归我们管，别乱补点击
            }
            long t = android.os.SystemClock.uptimeMillis();
            if (!com.oupao.wiz.IdxTouch.send(t, t, android.view.MotionEvent.ACTION_DOWN, x, y)) {
                com.oupao.wiz.SLog.r("r-fly-replay", "FlyH: 点击重放失败(注入不可用)", 5000L);
                return;
            }
            com.oupao.wiz.IdxTouch.send(t, t + 1L, android.view.MotionEvent.ACTION_UP, x, y);
            com.oupao.wiz.SLog.r("r-fly-replay", "FlyH: 点击重放 " + ((int) x) + "," + ((int) y), 2000L);
        } catch (Throwable ignore) {
        }
    }
    /**
     * 当前手指离角落有多远 → 呼出进度 0~1。
     *
     * 0 = 刚过触发线（菜单还是个点），1 = 划到扇区半径（完全展开）。
     * 这样菜单的呈现可以**全程跟着手指**，而不是"啪一下"全出来。
     */
    public static float progressAt(float x, float y) {
        try {
            float dx = (com.oupao.wiz.FlyH.side == 1) ? x : (((float) com.oupao.wiz.FlyH.sw) - x);
            float dy = ((float) com.oupao.wiz.FlyH.sh) - y;
            float d = (float) Math.sqrt((double) ((dx * dx) + (dy * dy)));
            float r = com.oupao.wiz.FlyH.rad;
            if (r <= 0.0f) {
                return 1.0f;
            }
            float p = d / (r * 0.92f);
            if (p < 0.0f) {
                p = 0.0f;
            }
            if (p > 1.0f) {
                p = 1.0f;
            }
            return p;
        } catch (Throwable __e) {
            return 1.0f;
        }
    }

    /**
     * 按进度把扇区"长"出来：每个图标有轻微错峰 + 缩放 + 上浮 + 淡入（smoothstep 缓动）。
     * 只在呼出过程中调用（MOVE 每帧一次），完全展开时 p=1 → 恢复原始状态。
     */
    /** 每个图标当前因为"从角落飞出去"而产生的位移（命中判定也要跟着加） */
    /** 当前扇形整体缩放比（1 = 原始大小）；Bar.hit 命中判定要按它换算 */
    public static float rv = 1.0f;
    public static float[] dvx;
    public static float[] dvy;

    public static float dv(int i, boolean x) {
        try {
            float[] a = x ? com.oupao.wiz.FlyH.dvx : com.oupao.wiz.FlyH.dvy;
            if ((a == null) || (i < 0) || (i >= a.length)) {
                return 0.0f;
            }
            return a[i];
        } catch (Throwable __e) {
            return 0.0f;
        }
    }

    /**
     * ★ 2732：跟手呼出**暂时关掉**，回到"一次显示完整扇形"。
     *
     * 原因（实测结论，别再重复踩）：
     *  - 跟手要每帧 MOVE 才能算进度，但热区窗口是 SPY（只收副本）时，
     *    ColorOS 底部拦截带的那条手势流会被桌面接管，**MOVE 根本不到我们这里**
     *    （日志里只有 `Fly: activate`，没有 `Fly: update`）；
     *  - 结果进度停在激活那一帧（很小）→ 只显示 1 个图标，还停在"飞出途中"的错误位置（用户截图）。
     *  - 只有热区窗口改成"消费型"才收得到 MOVE，但那样下层 App 的点击就没了，
     *    这次实验（2721/2722/2726）反复证明"消费 + 自注入重放"在这台机器上和点击互斥。
     *
     * 所以这里退化为：始终保持"完全展开"状态（位移 0、不透明），
     * 行为与老扇形完全一致，不会再出现半残。
     */
    /**
     * 跟手呼出：**一出来就是完整扇形，整体从角落缩放长大**。
     *
     * - 图标一律留在扇形上的最终位置（不位移、不改 alpha，`Bar` 自己的淡入照旧）；
     * - 缩放作用在**菜单容器**上，轴心 = 屏幕角落 → 看起来就是整个扇形从角里"长出来"；
     * - 缩放比同步存进 [rv]，`Bar.hit()` 用它把每个图标的中心点换算到缩放后的位置，
     *   所以任何进度下"手指下是哪个扇区"都算得对。
     */
    /**
     * 跟手呼出：**一出来就是完整扇形，整体从角落缩放长大**。
     *
     * 实现要点（2737 实证：动容器 `win` 的缩放会让整个菜单不渲染，所以一律**逐图标**做）：
     *  - 图标在扇形上的最终中心 c，缩放后的中心 c' = 角落 + (c − 角落) × s；
     *  - 位移 = c' − c，同时图标自身 scale = s（看起来就是整个扇形整体缩小/放大）；
     *  - 只改 translation/scale，**不改 alpha**（`Bar` 的淡入动画保持原样）；
     *  - s 同步进 [rv]，`Bar.hit()` 按同样的缩放算中心点，命中判定与视觉一致。
     */
    /**
     * 跟手呼出：**一出来就是完整扇形，整体从角落缩放长大**。
     *
     * 只缩放**菜单容器**（不逐图标动，逐图标会打断 `Bar` 的淡入 → 只剩一个图标）。
     * 轴心必须用**屏幕尺寸**算（`FlyH.sw/sh`）：菜单刚建时 `win.getWidth()` 还是 0，
     * 用它会得到轴心 (0,0)，整个扇形被缩到屏幕左上角外面（2736 的坑）。
     *
     * 缩放比同步进 [rv]，`Bar.hit()` 按同样比例换算图标中心，命中判定与视觉一致。
     */
    public static void reveal(float p) {
        // 2739 实测：动容器 scale 或逐图标的 translation/scale 都会让菜单渲染异常
        // （只剩一个图标、或整片空白），而完全不动 transform 时扇形渲染完全正常（2737 验证）。
        // 所以这里保持空实现；"跟手缩放"必须改成**自绘**（自定义 View 用 Canvas 画整个扇形）才能安全实现。
        return;
    }

    public static void hide()
    {
        android.view.View v0 = null;
        android.view.View[] v2 = null;
        // ★ 菜单一收就立刻放开闸门（FlyState）。放在最前面、单独兜异常：
        //   后面任何一步失败都不能让其它进程以为「菜单还开着」。
        try {
            com.oupao.wiz.FlyState.setOpen(false);
        } catch (Throwable ignore) {
        }
        try {
            com.oupao.wiz.FlyH.pressUp();
            com.oupao.wiz.FlyH.unstick();
            v0 = com.oupao.wiz.FlyH.win;
            com.oupao.wiz.FlyH.win = null;
            v2 = com.oupao.wiz.FlyH.items;
            com.oupao.wiz.FlyH.items = null;
        } catch (Throwable __e_v7) {
            return;
        }
        if (v0 != null) {
            var v3 = com.oupao.wiz.FlyH.wm;
            if (v3 != null) {
                if (v2 != null) {
                    var v1_0 = v2.length;
                    int v4_0 = 0;
                    while (v4_0 < v1_0) {
                        var v5_1 = v2[v4_0];
                        if (v5_1 != null) {
                            com.oupao.wiz.Bar.fadeOut(v5_1);
                        }
                        v4_0++;
                    }
                }
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new com.oupao.wiz.BarOut(v3, v0), 150);
            }
        }
        return;
    }
    public static void install(ClassLoader p3)
    {
        java.lang.Class v0_2 = null;
        try {
            v0_2 = com.oupao.wiz.Rt.cls(p3, "com.android.server.wm.WindowManagerService");
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_2 == null) {
            com.oupao.wiz.SLog.i("Fly: \u627e\u4e0d\u5230 WindowManagerService");
            return;
        } else {
            com.oupao.wiz.XBridge.hookAllConstructors(v0_2, new com.oupao.wiz.FlyW());
            // 3440：**构造钩子经常抢不到**（模块装载晚于 WMS 构造 → `FlyH.wms` 一直是 null，
            //   于是"焦点窗口标题 = NotificationShade"这条判据始终拿不到值）。
            //   再挂 addWindow/removeWindow：任何窗口增删都会走到，拿到本体就存一份。
            try {
                java.lang.reflect.Method[] wms = v0_2.getDeclaredMethods();
                int nh = 0;
                for (int wi = 0; wi < wms.length; wi++) {
                    String nm = wms[wi].getName();
                    if (!"addWindow".equals(nm) && !"removeWindow".equals(nm)) {
                        continue;
                    }
                    wms[wi].setAccessible(true);
                    if (com.oupao.wiz.XBridge.hookMethod(wms[wi], new com.oupao.wiz.FlyW()) != null) {
                        nh++;
                    }
                }
                com.oupao.wiz.SLog.i("Fly: WMS \u65b9\u6cd5\u515c\u5e95\u94a9\u5b50 " + nh + " \u4e2a");
            } catch (Throwable __e_w) {
                com.oupao.wiz.SLog.e("Fly: WMS \u65b9\u6cd5\u94a9\u5b50\u5931\u8d25", __e_w);
            }
            com.oupao.wiz.SLog.i("Fly: WMS \u6784\u9020\u94a9\u5b50\u5df2\u6302");
            // 3339：平行小窗（全屏二级页 + 小窗主页）的返回兜底
            try {
                com.oupao.wiz.SplitGuard.install(p3);
            } catch (Throwable __e_split) {
                com.oupao.wiz.SLog.e("Split: SplitGuard.install 失败", __e_split);
            }
            com.oupao.wiz.FlyH.regClone();
            return;
        }
    }
    public static void launch(String p7)
    {
        try {
            if (!p7.startsWith("tool#")) {
                if (!p7.equals("@more")) {
                    var v0_1 = p7.split("#");
                    var v2_0 = v0_1[0];
                    var v3_0 = v0_1[1];
                    if (v0_1.length >= 3) {
                        var v1_6 = Integer.parseInt(v0_1[2]);
                        if (v1_6 > 0) {
                            com.oupao.wiz.FlyH.cloneLaunch(v2_0, v3_0, v1_6);
                            return;
                        }
                    }
                    var v4_1 = new android.content.ComponentName(v2_0, v3_0);
                    var v5_1 = new android.content.Intent("android.intent.action.MAIN");
                    v5_1.addCategory("android.intent.category.LAUNCHER");
                    v5_1.setComponent(v4_1);
                    v5_1.setPackage(v2_0);
                    v5_1.addFlags(268435456);
                    com.oupao.wiz.FlyH.go(v5_1, v2_0);
                } else {
                    com.oupao.wiz.SLog.i("Fly: launch @more mode=" + com.oupao.wiz.Cfg.moreMode());
                    if ("window".equals(com.oupao.wiz.Cfg.moreMode())) {
                        // 原版逻辑：以 zoom 小窗方式启动 MainActivity（更多应用页）
                        try {
                            var c1 = com.oupao.wiz.Cfg.sysContext();
                            if (c1 != null) {
                                var i1 = new android.content.Intent();
                                i1.setClassName("com.oupao.wiz", "com.oupao.wiz.MainActivity");
                                // 小窗模式用第 9 页（desk 构建）：它有「已添加 + 应用/工具」两个分区，
                                // 徽标还按管理模式区分（管理=+/-，非管理=无徽标、点击直接打开）。
                                i1.putExtra("oupao_nav", 9);
                                i1.putExtra("oupao_desk", true);
                                i1.putExtra("oupao_pins", com.oupao.wiz.Cfg.deskPins());   // 与真实数据源一致（不再用旧键）
                                i1.addFlags(0x34000000);
                                var o1 = android.app.ActivityOptions.makeBasic();
                                var b1 = o1.toBundle();
                                b1.putInt("extra_window_mode", 100);
                                b1.putInt("android:activity.windowingMode", 100);
                                b1.putInt("android:activity.mZoomLaunchFlags", 5);
                                // 2851：ColorOS 的启动契约——LaunchScenario 缺了就退全屏
                                b1.putInt("androidx.activity.LaunchScenario", 1);
                                b1.putBoolean("androidx.activity.AdjustInputMethod", true);
                                c1.startActivity(i1, b1);
                                com.oupao.wiz.SLog.i("Fly: @more -> window(MainActivity)");
                            }
                        } catch (Throwable t) {
                            com.oupao.wiz.SLog.e("Fly: @more window 失败", t);
                        }
                    } else {
                        com.oupao.wiz.SLog.i("Fly: @more -> panel");
                        com.oupao.wiz.MorePanel.show();
                    }
                }
                return;
            } else {
                com.oupao.wiz.Tools.fire(com.oupao.wiz.Cfg.sysContext(), p7.substring(5));
                return;
            }
        } catch (Throwable __e_v0_7) {
            com.oupao.wiz.SLog.e("FlyH: launch failed", __e_v0_7);
            return;
        }
    }
    /**
     * 按当前「更多应用方式」**立刻**打开对应界面：
     *   window → 以 zoom 小窗启动 MainActivity 第 9 页；
     *   panel  → 在 system_server 里弹悬浮面板（App 进程则发广播请 system_server 弹）。
     * 供「长按标题切换呼出方式」后立即生效用（用户要求：不要等下次呼出）。
     */
    public static void openMore() {
        try {
            if ("window".equals(com.oupao.wiz.Cfg.moreMode())) {
                var c1 = com.oupao.wiz.Cfg.sysContext();
                if (c1 == null) {
                    return;
                }
                var i1 = new android.content.Intent();
                i1.setClassName("com.oupao.wiz", "com.oupao.wiz.MainActivity");
                i1.putExtra("oupao_nav", 9);
                i1.putExtra("oupao_desk", true);
                i1.putExtra("oupao_pins", com.oupao.wiz.Cfg.deskPins());
                // 由小窗页自己弹提示（保证与「小窗→面板」那次提示样式完全一致）
                i1.putExtra("oupao_toast", "\u547c\u51fa\u65b9\u5f0f\uff1a\u5c0f\u7a97");
                i1.addFlags(0x34000000);
                var o1 = android.app.ActivityOptions.makeBasic();
                var b1 = o1.toBundle();
                b1.putInt("extra_window_mode", 100);
                b1.putInt("android:activity.windowingMode", 100);
                b1.putInt("android:activity.mZoomLaunchFlags", 5);
                // 2851：ColorOS 的启动契约——LaunchScenario 缺了就退全屏
                b1.putInt("androidx.activity.LaunchScenario", 1);
                b1.putBoolean("androidx.activity.AdjustInputMethod", true);
                c1.startActivity(i1, b1);
                com.oupao.wiz.SLog.i("Fly: openMore -> window");
            } else {
                if (android.os.Process.myUid() == 1000) {
                    com.oupao.wiz.MorePanel.show();
                    com.oupao.wiz.SLog.i("Fly: openMore -> panel");
                } else {
                    var c2 = com.oupao.wiz.Cfg.sysContext();
                    if (c2 != null) {
                        c2.sendBroadcast(new android.content.Intent("com.oupao.wiz.SHOWMORE")
                                .setPackage("android"));
                        com.oupao.wiz.SLog.i("Fly: openMore -> 请求 system_server 弹面板");
                    }
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Fly: openMore \u5931\u8d25", t);
        }
    }

    public static void lg(String p3, int p4, int p5, int p6)
    {
        try {
            var v0_1 = new StringBuilder(p3);
            v0_1.append(" ");
            v0_1.append(p4);
            v0_1.append(" ");
            v0_1.append(p5);
            v0_1.append(" ");
            v0_1.append(p6);
            com.oupao.wiz.SLog.v(v0_1.toString());
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static void mark(int p8)
    {
        android.view.View[] v0 = null;
        try {
            v0 = com.oupao.wiz.FlyH.items;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 != null) {
            int v1 = 0;
            while (v1 < v0.length) {
                var v2_1 = v0[v1];
                if (v2_1 != null) {
                    com.oupao.wiz.Bar.grow(v2_1, v1);
                    if (v1 == p8) {
                        com.oupao.wiz.Bar.boost(v2_1, v1);
                    }
                    var v3_0 = v2_1.getBackground();
                    if ((v3_0 instanceof android.graphics.drawable.GradientDrawable)) {
                        var v3_1 = ((android.graphics.drawable.GradientDrawable) v3_0);
                        if (v1 != p8) {
                            v3_1.setStroke(0, 0);
                        } else {
                            v3_1.setStroke(10, -13730570);
                        }
                    }
                }
                v1++;
            }
        }
        return;
    }
    public static void onEvent(android.view.MotionEvent p0)
    {
        com.oupao.wiz.FlyInput.post(p0);
        return;
    }
    public static void onWms()
    {
        try {
            if (!com.oupao.wiz.FlyH.sched) {
                com.oupao.wiz.FlyH.sched = true;
                com.oupao.wiz.SLog.i("Fly: \u62ff\u5230 WMS\uff0c1.2 \u79d2\u540e\u6ce8\u518c\u6307\u9488\u76d1\u542c");
                com.oupao.wiz.FlyS.boot();
                var v0_3 = com.oupao.wiz.Hooks.main();
                if (v0_3 != null) {
                    // 3442：原来 20s（开机后菜单要等很久才能划）；现在 1.2s 起 + 每 400ms 重试
                    v0_3.postDelayed(new com.oupao.wiz.FlyR(), 1200);
                    v0_3.postDelayed(new com.oupao.wiz.CloneT(), 25000);
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static void pressDown(float p4, float p5)
    {
        int v0 = 0;
        try {
            v0 = com.oupao.wiz.Bar.hit(p4, p5);
        } catch (Throwable __e_v0) {
            return;
        }
        if ((v0 >= 0) && (v0 < com.oupao.wiz.FlyH.cnt)) {
            var v1_1 = com.oupao.wiz.FlyH.items;
            if (v1_1 != null) {
                var v2 = v1_1[v0];
                if (v2 != null) {
                    com.oupao.wiz.FlyH.pressUp();
                    com.oupao.wiz.FlyH.prs = v2;
                    com.oupao.wiz.Bar.press(v2, true);
                }
            }
        }
        return;
    }
    public static void pressUp()
    {
        android.view.View v0 = null;
        try {
            v0 = com.oupao.wiz.FlyH.prs;
            com.oupao.wiz.FlyH.prs = null;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 != null) {
            com.oupao.wiz.Bar.press(v0, false);
        }
        return;
    }
    public static void reg()
    {
        java.lang.Object v0 = null;
        try {
            com.oupao.wiz.FlyS.sync();
            v0 = com.oupao.wiz.FlyH.wms;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 != null) {
            var v1 = com.oupao.wiz.Cfg.sysContext();
            if (v1 != null) {
                com.oupao.wiz.FlyH.ctx = v1;
                com.oupao.wiz.FlyH.wm = ((android.view.WindowManager) v1.getSystemService("window"));
                var v3_1 = v0.getClass().getClassLoader();
                var v4_1 = com.oupao.wiz.Rt.cls(v3_1, "android.view.WindowManagerPolicyConstants$PointerEventListener");
                if (v4_1 != null) {
                    Class[] v5_1 = new Class[1];
                    v5_1[0] = v4_1;
                    var v5_2 = java.lang.reflect.Proxy.newProxyInstance(v3_1, v5_1, new com.oupao.wiz.FlyIh());
                    var v6_4 = com.oupao.wiz.Rt.methodByName(v0.getClass(), "registerPointerEventListener", 2);
                    if (v6_4 == null) {
                        com.oupao.wiz.SLog.i("Fly: \u6ca1\u6709 registerPointerEventListener");
                    } else {
                        Object[] v7_2 = new Object[2];
                        v7_2[0] = v5_2;
                        v7_2[1] = Integer.valueOf(0);
                        if (!com.oupao.wiz.FlyH.callReg(v6_4, v0, v7_2)) {
                            com.oupao.wiz.SLog.i("Fly: \u6307\u9488\u76d1\u542c\u672a\u6ce8\u518c\uff08\u4e0d\u4f1a\u6536\u4e8b\u4ef6\uff09");
                            return;
                        } else {
                            com.oupao.wiz.FlyH.reg = true;
                            com.oupao.wiz.SLog.i("Fly: \u6307\u9488\u76d1\u542c\u5df2\u6ce8\u518c");
                            return;
                        }
                    }
                }
            }
        }
        return;
    }
    public static void regClone()
    {
        com.oupao.wiz.SLog.i("Fly: \u5206\u8eab\u901a\u9053\u8d70 Rx \u5e7f\u64ad(b856)");
        return;
    }
    public static void selLog(float p3, float p4, int p5)
    {
        try {
            var v0_1 = new StringBuilder();
            v0_1.append("Fly: sel=");
            v0_1.append(p5);
            v0_1.append(" xy=");
            v0_1.append(((int) p3));
            v0_1.append(",");
            v0_1.append(((int) p4));
            com.oupao.wiz.SLog.r("r-fly-sel", v0_1.toString(), 2000L);
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static void show(int p13, float p14, float p15)
    {
        try {
            com.oupao.wiz.SLog.i("FlyH: show side=" + p13 + " st=" + com.oupao.wiz.FlyH.st
                    + " pil=" + (com.oupao.wiz.FlyS.pil ? 1 : 0) + " x=" + ((int) p14) + " y=" + ((int) p15));
        } catch (Throwable ignore) {
        }
        android.content.Context v1_1 = null;
        try {
            com.oupao.wiz.FlyH.pilOk = false;
            com.oupao.wiz.FlyH.hide();
            v1_1 = com.oupao.wiz.FlyH.ctx;
        } catch (Throwable __e_v1_0) {
            com.oupao.wiz.SLog.e("Fly: show \u5931\u8d25", __e_v1_0);
            com.oupao.wiz.FlyH.hide();
            return;
        }
        if (v1_1 == null) {
            v1_1 = com.oupao.wiz.Cfg.sysContext();
        }
        if (v1_1 != null) {
            com.oupao.wiz.FlyH.ctx = v1_1;
            var v2_3 = ((android.view.WindowManager) v1_1.getSystemService("window"));
            com.oupao.wiz.FlyH.wm = v2_3;
            if (v2_3 != null) {
                var v3_1 = com.oupao.wiz.Pins.parse(com.oupao.wiz.Cfg.fanPins());
                var v3_2 = com.oupao.wiz.Pins.cap5(v3_1, v3_1.size());
                var v4_1 = v3_2.size();
                if (v4_1 <= 0) {
                    com.oupao.wiz.SLog.i("Fly: \u6ca1\u6709\u56fa\u5b9a\u5e94\u7528(\u4ecd\u663e\u793a\u66f4\u591a\u5e94\u7528)");
                }
                var v10 = (v4_1 + 1);
                com.oupao.wiz.FlyH.cnt = v10;
                var v5_1 = com.oupao.wiz.FlyH.spx(v1_1, 200);
                com.oupao.wiz.FlyH.rad = ((float) v5_1);
                var v6_6 = (com.oupao.wiz.FlyH.spxR(v1_1, 36) / 2);
                com.oupao.wiz.FlyH.arcH = v6_6;
                com.oupao.wiz.FlyH.dthr = ((float) com.oupao.wiz.FlyH.spx(v1_1, 54));
                var v8_1 = ((v5_1 + v6_6) + com.oupao.wiz.FlyH.spx(v1_1, 24));
                com.oupao.wiz.FlyH.arcS = v8_1;
                com.oupao.wiz.Bar.fan();
                var v9_1 = new android.widget.FrameLayout(v1_1);
                v9_1.setClickable(true);
                com.oupao.wiz.Bar.arc(v1_1, v9_1, v3_2, p13);
                String[] v5_2 = new String[v10];
                com.oupao.wiz.FlyH.comps = v5_2;
                v5_2[0] = "@more";
                int v6_2 = 0;
                while (v6_2 < v4_1) {
                    v5_2[(v6_2 + 1)] = ((String) v3_2.get(v6_2));
                    v6_2++;
                }
                var v5_4 = (com.oupao.wiz.FlyH.sw - v8_1);
                if (1 == p13) {
                    v5_4 = 0;
                }
                com.oupao.wiz.FlyH.rox = v5_4;
                com.oupao.wiz.FlyH.roy = (com.oupao.wiz.FlyH.sh - v8_1);
                com.oupao.wiz.Bar.place();
                var v5_8 = new android.view.WindowManager.LayoutParams();
                v5_8.type = 2003;
                v5_8.flags = 826;
                v5_8.dimAmount = 0.2f;
                v5_8.width = v8_1;
                v5_8.height = v8_1;
                v5_8.format = -3;
                if (1 != p13) {
                    v5_8.gravity = 85;
                } else {
                    v5_8.gravity = 83;
                }
                android.view.View v6_0 = null;
                v5_8.setFitInsetsTypes(0);
                com.oupao.wiz.FlyS.trust2(v5_8);
                v2_3.addView(v9_1, v5_8);
                if (com.oupao.wiz.FlyH.side != 1) {
                    v6_0 = com.oupao.wiz.FlyS.zone2;
                } else {
                    v6_0 = com.oupao.wiz.FlyS.zone;
                }
                com.oupao.wiz.FlyS.pilfer(v6_0);
                com.oupao.wiz.FlyH.win = v9_1;
                // 2768：窗口还没测量完时 Bar.place() 算出来是 0 → 图标画不出来；布局后再排一次
                try {
                    v9_1.post(new Runnable() {
                        public void run() {
                            try {
                                com.oupao.wiz.Bar.place();
                            } catch (Throwable ignore) {
                            }
                        }
                    });
                } catch (Throwable ignore) {
                }
                com.oupao.wiz.FlyH.reveal(0.0f);
                com.oupao.wiz.FlyH.shownAt = android.os.SystemClock.uptimeMillis();
                // ★ 2725：**不要**在这里建全屏接盘窗口（FlyCat）。
                //   2724 试过"菜单一出现就立全屏窗口去接住桌面重放的那一下点击"，
                //   结果全屏窗口在拖动中途插进窗口栈，把正在进行的那条流抢走了 →
                //   扇区不再更新、菜单里什么都选不了。接盘窗口仍只在松手后 stay() 里建。
                com.oupao.wiz.SLog.i("Fly: \u83dc\u5355\u5df2\u663e\u793a");
                // ★ 告诉其余进程「菜单开了」（Launcher 手势闸门 / SystemUI 热区都要看这个）：FlyState
                try {
                    com.oupao.wiz.FlyState.setOpen(true);
                } catch (Throwable ignore) {
                }
                com.oupao.wiz.FlyH.buzz();
                com.oupao.wiz.FlyH.cloneTick();
                com.oupao.wiz.Bar.wobble();
            }
        }
        return;
    }
        /**
     * spx/spxIcon 的公共实现：按给定百分比缩放 dp 值。
     * 原来两份只差用 flyScale 还是 iconSize。
     */
    private static int spxBy(android.content.Context p3, int p4, int pct)
    {
        try {
            return ((com.oupao.wiz.Ui.dp(p3, p4) * pct) / 100);
        } catch (Throwable __e_v0) {
            return com.oupao.wiz.Ui.dp(p3, p4);
        }
    }
    public static int spx(android.content.Context p3, int p4)
    {
        return com.oupao.wiz.FlyH.spxBy(p3, p4, com.oupao.wiz.Cfg.flyScale());
    }
    public static int spxIcon(android.content.Context p3, int p4)
    {
        return com.oupao.wiz.FlyH.spxBy(p3, p4, com.oupao.wiz.Cfg.iconSize());
    }
    public static int spxR(android.content.Context p3, int p4)
    {
        try {
            return ((com.oupao.wiz.FlyH.spx(p3, p4) * 130) / 100);
        } catch (Throwable __e_v0) {
            return com.oupao.wiz.FlyH.spx(p3, p4);
        }
    }
    /**
     * 建全屏"接盘"窗口（FlyCat）：菜单开着的时候由它吃掉整个屏幕的触摸。
     *
     * 2724 起它**在菜单弹出的那一刻就建**（以前只在松手后 stay() 里建）：
     * 因为 ColorOS 底部那条带（homegesture 拦截区）的点击是由桌面 consumer **重放**给下层 App 的，
     * 这个重放有可能在菜单弹出之后才落地 —— 于是会"穿透"到 QQ 底部 tap 栏这种地方，点掉一下。
     * 全屏窗口先立在最上面，重放出来的那一下就被我们接住，落不到 App 上。
     *
     * 注意：这里**只建窗口，不设 stick** —— 设了 stick 会让 handle() 提前 return，拖动中的选扇区就废了。
     */
    public static void catcher()
    {
        try {
            if (com.oupao.wiz.FlyH.cat != null) {
                return;
            }
            var v0_1 = com.oupao.wiz.FlyH.ctx;
            if (v0_1 == null) {
                return;
            }
            var v1 = com.oupao.wiz.FlyH.wm;
            if (v1 == null) {
                return;
            }
            var v2_1 = new com.oupao.wiz.FlyCat(v0_1);
            var v3_1 = new android.view.WindowManager.LayoutParams();
            v3_1.type = 2003;
            v3_1.flags = 1312;
            v3_1.width = -1;
            v3_1.height = -1;
            v3_1.format = -3;
            v3_1.setFitInsetsTypes(0);
            com.oupao.wiz.FlyS.trust2(v3_1);
            v1.addView(v2_1, v3_1);
            com.oupao.wiz.FlyH.cat = v2_1;
            com.oupao.wiz.SLog.i("Fly: catcher 已建");
        } catch (Throwable e) {
            com.oupao.wiz.SLog.e("Fly: catcher 建立失败", e);
        }
        return;
    }
    public static void stay()
    {
        try {
            // 没划满就松手：先把扇区强制补到完全展开，别留半透明图标
            com.oupao.wiz.FlyH.reveal(1.0f);
            com.oupao.wiz.FlyH.catcher();
            if (com.oupao.wiz.FlyH.cat != null) {
                com.oupao.wiz.FlyH.stick = true;
                com.oupao.wiz.FlyH.bindBack();
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static void stayOrClose()
    {
        try {
            if (!com.oupao.wiz.Cfg.flyStayOff()) {
                com.oupao.wiz.FlyH.stay();
                return;
            } else {
                com.oupao.wiz.FlyH.cancel();
                return;
            }
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static void tap(float p0, float p1)
    {
        try {
            com.oupao.wiz.FlyH.sel = com.oupao.wiz.Bar.hit(p0, p1);
            com.oupao.wiz.FlyH.buzz();
            com.oupao.wiz.FlyH.commit();
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static void unstick()
    {
        android.view.View v0 = null;
        try {
            v0 = com.oupao.wiz.FlyH.cat;
            com.oupao.wiz.FlyH.cat = null;
            com.oupao.wiz.FlyH.bk = null;
            com.oupao.wiz.FlyH.stick = false;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 != null) {
            var v1_2 = com.oupao.wiz.FlyH.wm;
            if (v1_2 != null) {
                v1_2.removeView(v0);
            }
        }
        return;
    }
    public static void update(float p5, float p6)
    {
        int v0 = 0;
        int v1 = 0;
        try {
            v0 = com.oupao.wiz.FlyH.sel;
            v1 = com.oupao.wiz.Bar.hit(p5, p6);
            com.oupao.wiz.FlyH.sel = v1;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 != v1) {
            var v3_2 = new StringBuilder();
            v3_2.append("Fly: update old=");
            v3_2.append(v0);
            v3_2.append(" new=");
            v3_2.append(v1);
            com.oupao.wiz.SLog.i(v3_2.toString());
        }
        if ((v0 != v1) && (v1 >= 0)) {
            com.oupao.wiz.FlyH.buzz();
        }
        com.oupao.wiz.FlyH.mark(v1);
        com.oupao.wiz.FlyH.selLog(p5, p6, v1);
        return;
    }
    public static void warm()
    {
        try {
            if (Long.compare((android.os.SystemClock.uptimeMillis() - com.oupao.wiz.FlyH.warmT), 10000) >= 0) {
                com.oupao.wiz.FlyH.warmT = android.os.SystemClock.uptimeMillis();
                var v0_1 = com.oupao.wiz.Cfg.sysContext();
                if (v0_1 != null) {
                    v0_1.sendBroadcast(new android.content.Intent("com.oupao.wiz.WARM").setPackage("com.oupao.wiz").addFlags(32));
                    com.oupao.wiz.SLog.i("Fly: \u9884\u70ed\u5e7f\u64ad\u5df2\u53d1");
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static boolean zoomOpen(android.content.Intent p10, int p11)
    {
        java.lang.Class v0_1 = null;
        java.lang.Object v1_0 = null;
        try {
            v0_1 = Class.forName("com.oplus.zoomwindow.OplusZoomWindowManager");
            v1_0 = com.oupao.wiz.Rt.invoke(com.oupao.wiz.Rt.methodByName(v0_1, "getInstance", 0), null, null);
        } catch (Throwable __e_v0_2) {
            com.oupao.wiz.SLog.e("Fly: zoomOpen \u5931\u8d25", __e_v0_2);
            return false;
        }
        if (v1_0 != null) {
            var v2_1 = com.oupao.wiz.Rt.methodByName(v0_1, "startZoomWindow", 4);
            if (v2_1 != null) {
                var v3_1 = android.app.ActivityOptions.makeBasic();
                v3_1.setLaunchDisplayId(0);
                var v3_2 = v3_1.toBundle();
                v3_2.putInt("extra_window_mode", 100);
                v3_2.putInt("android.activity.splashScreenStyle", 1);
                Object[] v4_4 = new Object[4];
                v4_4[0] = p10;
                v4_4[1] = v3_2;
                v4_4[2] = Integer.valueOf(p11);
                v4_4[3] = "com.coloros.smartsidebar";
                try {
                    var v1_2 = com.oupao.wiz.Rt.invoke(v2_1, v1_0, v4_4);
                    // 3414：ret 2/3 也算成功（TASK_TO_FRONT / DELIVERED_TO_TOP），见 ZoomGate.zoomRetOk。
                    //   以前 `!= 0` 当失败 → 分身体的调用方会再走一遍全屏/PendingIntent 兜底。
                    if (com.oupao.wiz.ZoomGate.zoomRetOk(v1_2)) {
                        com.oupao.wiz.SLog.i("Fly: zoomOpen ret ok=" + String.valueOf(v1_2));
                        return true;
                    }
                    com.oupao.wiz.SLog.i("Fly: zoomOpen ret=" + String.valueOf(v1_2) + " (\u8d1f\u6570 \u2192 \u5f53\u5931\u8d25)");
                } catch (Throwable tz) {
                    com.oupao.wiz.SLog.e("Fly: zoomOpen \u8c03\u7528\u5931\u8d25 user=" + p11, tz);
                }
            }
        }
        return false;
    }
}