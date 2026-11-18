package com.oupao.wiz;
public class FlyS {
    public static android.content.Context ctx;
    public static boolean pil;
    public static android.view.WindowManager wm;
    public static android.view.View zone;
    public static android.view.View zone2;
    public static int zs;
    /** 3261：上一次建热区用的形状（0=角形 1=边条），形状变了也要重建 */
    public static int zoneMode = -1;

    /**
     * 3130：几何的**本地副本**（pubR = 热区半径 px，pubLr = 1 左下 / 2 右下 / 3 两侧）。
     *
     * 为什么要有：热区半径/开关位原来写进 `sys.oupao.fly.r/.lr` 属性给**别的进程**读，
     * 但 C17 实测 system_server 写完这两个属性、读回来是空的（`FlyState.radius()/lr()` 恒为 0），
     * 于是 FlyFanBr 第一行 `if ((lr == 0) || (r <= 0)) return;` 直接退出 ——
     * 表现就是「左右下角怎么划都出不来扇形菜单，热区只把点击吃掉再重放」。
     * FlyFanBr / FlyH 和 FlyS 都在 system_server 同一个进程，直接用这份本地值即可。
     */
    public static volatile int pubR;
    public static volatile int pubLr;

    /** 本地几何兜底：属性读不到时用 [pubR]/[pubLr] */
    public static int lrNow()
    {
        try {
            int v = com.oupao.wiz.FlyState.lr();
            if (v != 0) {
                return v;
            }
        } catch (Throwable ignore) {
        }
        return com.oupao.wiz.FlyS.pubLr;
    }

    /** 本地几何兜底：属性读不到时用 [pubR] */
    public static int radiusNow()
    {
        try {
            int v = com.oupao.wiz.FlyState.radius();
            if (v > 0) {
                return v;
            }
        } catch (Throwable ignore) {
        }
        return com.oupao.wiz.FlyS.pubR;
    }
    public FlyS()
    {
        return;
    }
    public static void boot()
    {
        android.content.Context v0 = null;
        try {
            v0 = com.oupao.wiz.Cfg.sysContext();
            com.oupao.wiz.FlyS.ctx = v0;
            com.oupao.wiz.FlyH.ctx = v0;
            com.oupao.wiz.FlyS.wm = null;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 != null) {
            com.oupao.wiz.FlyS.wm = ((android.view.WindowManager) v0.getSystemService("window"));
        }
        if (com.oupao.wiz.FlyS.wm == null) {
            var v2_0 = com.oupao.wiz.FlyH.wms;
            if (v2_0 != null) {
                var v2_1 = com.oupao.wiz.Rt.get(v2_0, "mContext");
                if (v2_1 != null) {
                    var v2_2 = ((android.content.Context) v2_1);
                    com.oupao.wiz.FlyS.ctx = v2_2;
                    com.oupao.wiz.FlyH.ctx = v2_2;
                    com.oupao.wiz.FlyS.wm = ((android.view.WindowManager) v2_2.getSystemService("window"));
                }
            }
        }
        if (com.oupao.wiz.FlyS.wm == null) {
            com.oupao.wiz.SLog.i("FlyS: \u62ff\u4e0d\u5230 WindowManager\uff08\u70ed\u533a\u65e0\u6cd5\u5efa\uff09");
        } else {
            var v1_4 = com.oupao.wiz.Hooks.main();
            if (v1_4 != null) {
                v1_4.postDelayed(new com.oupao.wiz.FlyZ(), 15000);
                com.oupao.wiz.SLog.i("FlyS: \u70ed\u533a\u5df2\u5b89\u6392\uff0815 \u79d2\u540e\uff09");
                return;
            }
        }
        return;
    }
    public static void install(ClassLoader p0)
    {
        com.oupao.wiz.UiInit.install(p0);
        return;
    }
    public static void kill()
    {
        android.view.WindowManager v0_0 = null;
        // ★ 热区撤掉 = 扇形菜单不可用：把发布出去的开关位一起清零，
        //   否则「关掉扇形菜单」后底角上滑仍然回不到桌面。
        try {
            com.oupao.wiz.FlyState.put(com.oupao.wiz.FlyState.P_LR, "0");
        } catch (Throwable ignore) {
        }
        try {
            v0_0 = com.oupao.wiz.FlyS.wm;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_0 != null) {
            var v1_1 = com.oupao.wiz.FlyS.zone;
            if (v1_1 != null) {
                v0_0.removeViewImmediate(v1_1);
                com.oupao.wiz.FlyS.zone = null;
            }
            var v1_3 = com.oupao.wiz.FlyS.zone2;
            if (v1_3 != null) {
                v0_0.removeViewImmediate(v1_3);
                com.oupao.wiz.FlyS.zone2 = null;
            }
            com.oupao.wiz.FlyS.zs = 0;
        }
        return;
    }
    public static void lg(String p2)
    {
        try {
            com.oupao.wiz.SLog.i(p2);
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }

    /**
     * 3125：游戏模式屏蔽热区触摸。
     *
     * 以前只在 FlyFanBr 里判了 gameBlock —— 那只管「菜单弹不弹」，
     * 真正消费触摸的是这两个 FlyZone 窗口。游戏里菜单虽然不弹，
     * 但 280px 的热区仍会把左下摇杆/右下技能的 DOWN/MOVE 吃掉，UP 后只补一个点击，
     * 表现就是「游戏点不动、扇形菜单还在影响触摸」。
     *
     * @param blocked true = 游戏模式，热区改成 FLAG_NOT_TOUCHABLE，触摸直接给下层游戏
     */
    public static void setGameBlocked(final boolean blocked)
    {
        try {
            android.os.Handler h = com.oupao.wiz.Hooks.main();
            if (h != null && android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
                h.post(new Runnable() {
                    public void run() {
                        applyGameBlocked(blocked);
                    }
                });
                return;
            }
        } catch (Throwable ignore) {
        }
        applyGameBlocked(blocked);
    }

    private static void applyGameBlocked(boolean blocked)
    {
        try {
            applyZoneFlag(zone, blocked);
        } catch (Throwable ignore) {
        }
        try {
            applyZoneFlag(zone2, blocked);
        } catch (Throwable ignore) {
        }
        if (blocked) {
            // 进游戏时如果菜单正好停着，收掉，别留一层可触摸窗口
            try {
                com.oupao.wiz.FlyFanH.hide();
                com.oupao.wiz.FlyState.setOpen(false);
            } catch (Throwable ignore) {
            }
        }
        try {
            com.oupao.wiz.SLog.i("FlyS: 游戏模式热区 touchable=" + (!blocked)
                    + " z1=" + (zone != null) + " z2=" + (zone2 != null));
        } catch (Throwable ignore) {
        }
    }

    private static void applyZoneFlag(android.view.View v, boolean blocked)
    {
        if (!(v instanceof com.oupao.wiz.FlyZone)) {
            return;
        }
        com.oupao.wiz.FlyZone z = (com.oupao.wiz.FlyZone) v;
        android.view.WindowManager.LayoutParams p = z.wp;
        android.view.WindowManager w = z.wm;
        if ((p == null) || (w == null)) {
            return;
        }
        int before = p.flags;
        if (blocked) {
            p.flags |= com.oupao.wiz.FlyZone.FLAG_NOT_TOUCHABLE;
        } else {
            p.flags &= ~com.oupao.wiz.FlyZone.FLAG_NOT_TOUCHABLE;
        }
        if (p.flags != before) {
            try {
                w.updateViewLayout(z, p);
            } catch (Throwable ignore) {
            }
        }
    }
    public static android.view.View mk(int p9)
    {
        android.content.Context v0_9 = null;
        android.view.WindowManager v1_2 = null;
        try {
            var v0_1 = new StringBuilder();
            v0_1.append("FlyS: mk a side=");
            v0_1.append(p9);
            com.oupao.wiz.SLog.i(v0_1.toString());
            v0_9 = com.oupao.wiz.FlyS.ctx;
            v1_2 = com.oupao.wiz.FlyS.wm;
        } catch (Throwable __e_v0_4) {
            var v1_1 = new StringBuilder();
            v1_1.append("FlyS: mk fail ");
            v1_1.append(String.valueOf(__e_v0_4));
            com.oupao.wiz.SLog.i(v1_1.toString());
            return null;
        }
        if (v0_9 != null) {
            if (v1_2 != null) {
                int v2_0 = com.oupao.wiz.FlyS.zs;
                if (v2_0 <= 0) {
                    v2_0 = 300;
                }
                android.view.View v3_1 = new com.oupao.wiz.FlyZone(v0_9, p9);
                // 3289：mk 的三条过程日志已移除（热区重建本身有「已建 size」汇总）
                var v4_1 = new android.view.WindowManager.LayoutParams();
                v4_1.width = v2_0;
                v4_1.height = v2_0;
                // 2720：窗口类型回到 2038（= 2705 原始行为）。
                //   2713 试过提到 2024(NAVIGATION_BAR_PANEL) 想压住侧滑手势区，实测对点击没有帮助，
                //   反而多一个不必要的行为差异 → 退回原值。
                v4_1.type = 2038;
                v4_1.flags = 134218024;
                v4_1.format = -3;
                if (p9 != 1) {
                    v4_1.gravity = 85;
                } else {
                    v4_1.gravity = 83;
                }
                v4_1.setFitInsetsTypes(0);
                // 2774：★ 窗口尺寸的真 bug —— 原来这里是 `v4_1.width = (p9 * 4)`，
                //   p9 是 side（1/2），所以窗口只有 4×4 / 8×8 **像素**！
                //   结果：热区窗口等于不存在 → 手势全被前台 App 吃掉 → "菜单呼出后滑动照样穿透"。
                //   现在：边长 = 热区直径（flyRange×density×2），最低 300px。
                //   ★ 只要 DOWN 落在窗口里，**整条手势都归我们**（实测拖出窗口边界也不会还给下层），
                //     所以窗口不需要很大 —— 盖住热区圆（半径 = flyRange×density）就够，死区最小。
                int zn = com.oupao.wiz.FlyS.zs;      // 3270：= 热区半径（FlyS.sync 里算好）
                if (zn <= 0) {
                    try {
                        int hr = (int) (((float) com.oupao.wiz.Cfg.flyRange())
                                * v0_9.getResources().getDisplayMetrics().density);
                        zn = Math.max(hr, 80);
                    } catch (Throwable ignore) {
                        zn = 200;
                    }
                }
                // 3261：热区形状。0 = 角形（zn × zn 正方形）；1 = 边条（宽 zn/2、高 zn，贴边竖条）。
                int zw = zn;
                int zh = zn;
                try {
                    if (com.oupao.wiz.Cfg.flyZoneMode() == 1) {
                        zw = Math.max(zn / 2, 80);
                        zh = zn;
                    }
                } catch (Throwable ignore) {
                }
                // 自检：setprop sys.oupao.fly.zone_r <px> 可临时指定窗口边长
                try {
                    int zr = Integer.parseInt(com.oupao.wiz.FlyState.raw("sys.oupao.fly.zone_r").trim());
                    if (zr > 0) {
                        v4_1.width = zr;
                        v4_1.height = zr;
                    } else {
                        v4_1.width = zw;
                        v4_1.height = zh;
                    }
                } catch (Throwable ignore) {
                    v4_1.width = zw;
                    v4_1.height = zh;
                }
                com.oupao.wiz.SLog.i("FlyS: 热区窗口边长=" + v4_1.width);
                // 官方 smali 无 y：gravity 底对齐、y=0，窗口底边正好压在屏幕最底边
                // ★ 2721：from trust(SPY) → trust2(消费)
                //   SPY 只收副本不消费 → 呼出菜单那一段拖动会同时落到前台 App（穿透）。
                //   `2038 + setTrustedOverlay + 不加 SPY` 正是 IdxBand 2227 验证过"能收到且能消费"的配置；
                //   点击由 FlyZone 自己在 UP 时用 IdxTouch 重放回下层，所以热区不会变死区。
                // 自检：setprop sys.oupao.fly.wtype <type> 可临时改窗口类型，用来找"能收到触摸"的那一层
                try {
                    int wt = Integer.parseInt(com.oupao.wiz.FlyState.raw("sys.oupao.fly.wtype").trim());
                    if (wt > 0) {
                        v4_1.type = wt;
                    }
                } catch (Throwable ignore) {
                }
                // 2772：窗口保持**可触摸（消费）**——这样才能不让底层 App 跟着滚。
                //   "用几次后开始穿透"的真因是重放时 flags 的读改写竞态（两次重放重叠时
                //   会把 NOT_TOUCHABLE 永久留下），所以恢复逻辑改成"永远清掉该位"。
                // ★ 3265：改成系统实际行为 —— **SPY 只旁观**，
                //   等手势判定成菜单手势时再由 FlyZone 调 `pilferPointers` 抢流。
                //   好处：热区里的「侧滑返回」不再被我们吃掉（没抢就是系统的），
                //   菜单手势一确认就抢过来，App 也不会跟着滚。
                boolean spy = false;
                try {
                    com.oupao.wiz.FlyS.trust(v4_1);   // setTrustedOverlay + inputFeatures|=SPY
                    spy = true;
                } catch (Throwable ignore) {
                }
                com.oupao.wiz.FlyZone.spy = spy;
                if (!spy) {
                    com.oupao.wiz.FlyS.trust2(v4_1);  // 兜底：原来的消费型
                    com.oupao.wiz.SLog.i("FlyS: SPY 失败 → 退回消费型热区");
                }
                // 3125：游戏模式中新建的热区直接不可触摸，否则建出来就会吃游戏触摸
                try {
                    if (com.oupao.wiz.Hooks.gameBlock()) {
                        v4_1.flags |= com.oupao.wiz.FlyZone.FLAG_NOT_TOUCHABLE;
                    }
                } catch (Throwable ignore) {
                }
                // 3289：mk trust 日志已移除
                try {
                    v1_2.addView(v3_1, v4_1);
                if (v3_1 instanceof com.oupao.wiz.FlyZone) {
                    com.oupao.wiz.FlyZone fz = ((com.oupao.wiz.FlyZone) v3_1);
                    fz.wm = v1_2;
                    fz.wp = v4_1;
                }
                // 3268：**把热区本身登记成系统手势排除区**。
                //   前两版都在跟系统"抢"（MOVE 里 pilfer、DOWN 里 pilfer）都晚一步：
                //   Oplus 的边缘返回监听器在窗口之前就拿到事件，预测性返回的箭头已经画出来了。
                //   这里是正规接口（API 29+）：窗口登记排除区后，输入分发的「手势区」会把这
                //   块挖掉（dumpsys input 的 gesture_displayid_0 能直接看到），
                //   系统不会在这块启动返回手势 —— 而且**每个窗口各自生效，两个角都能覆盖**。
                try {
                    java.util.List<android.graphics.Rect> ex = new java.util.ArrayList<>();
                    ex.add(new android.graphics.Rect(0, 0, v4_1.width, v4_1.height));
                    v3_1.setSystemGestureExclusionRects(ex);
                    com.oupao.wiz.SLog.i("FlyS: 手势排除区已登记 " + v4_1.width + "x" + v4_1.height);
                } catch (Throwable t) {
                    com.oupao.wiz.SLog.i("FlyS: 手势排除区登记失败 " + t);
                }
                } catch (Throwable e) {
                    com.oupao.wiz.SLog.i("FlyS: 热区窗口加不上 " + e);
                    return null;
                }
                // 3289：mk added 日志已移除
                return v3_1;
            } else {
                com.oupao.wiz.SLog.i("FlyS: mk b wm null");
                return null;
            }
        } else {
            com.oupao.wiz.SLog.i("FlyS: mk b ctx null");
            return null;
        }
    }
    public static android.content.Context pick()
    {
        android.content.Context v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.FlyS.ctx;
        } catch (Throwable __e_v0) {
            return null;
        }
        if (!com.oupao.wiz.FlyS.tryCtx(v0_0)) {
            var v0_6 = com.oupao.wiz.Cfg.sysContext();
            if (!com.oupao.wiz.FlyS.tryCtx(v0_6)) {
                var v0_7 = com.oupao.wiz.Sug.ctx;
                if (!com.oupao.wiz.FlyS.tryCtx(v0_7)) {
                    var v0_1 = com.oupao.wiz.FlyH.wms;
                    if (v0_1 != null) {
                        var v0_3 = ((android.content.Context) com.oupao.wiz.Rt.get(v0_1, "mContext"));
                        if (com.oupao.wiz.FlyS.tryCtx(v0_3)) {
                            return v0_3;
                        }
                    }
                    return null;
                } else {
                    return v0_7;
                }
            } else {
                return v0_6;
            }
        } else {
            return v0_0;
        }
    }
    public static void pilfer(android.view.View p7)
    {
        try {
            com.oupao.wiz.SLog.i("FlyS: pilfer 进入 pil=" + (com.oupao.wiz.FlyS.pil ? 1 : 0) + " v=" + (p7 != null));
            if ((!com.oupao.wiz.FlyS.pil) && (p7 != null)) {
                Class[] v2_7 = new Class[0];
                var v0_8 = android.view.View.class.getDeclaredMethod("getViewRootImpl", v2_7);
                v0_8.setAccessible(true);
                Object[] v1_1 = new Object[0];
                var v0_1 = v0_8.invoke(p7, v1_1);
                if (v0_1 != null) {
                    Class[] v3_1 = new Class[0];
                    Object[] v2_2 = new Object[0];
                    var v0_3 = ((android.os.IBinder) v0_1.getClass().getMethod("getInputToken", v3_1).invoke(v0_1, v2_2));
                    if (v0_3 != null) {
                        var v1_5 = com.oupao.wiz.FlyS.ctx.getSystemService("input");
                        var v2_4 = v1_5.getClass();
                        Class[] v4_1 = new Class[1];
                        v4_1[0] = android.os.IBinder.class;
                        var v2_5 = v2_4.getDeclaredMethod("pilferPointers", v4_1);
                        v2_5.setAccessible(true);
                        Object[] v3_5 = new Object[1];
                        v3_5[0] = v0_3;
                        v2_5.invoke(v1_5, v3_5);
                        com.oupao.wiz.FlyS.pil = true;
                        com.oupao.wiz.SLog.i("FlyS: \u5df2\u62a2\u4e0b\u89e6\u6478\u6d41");
                    }
                }
            }
        } catch (Throwable __e_v0_7) {
            com.oupao.wiz.FlyS.lg(String.valueOf(__e_v0_7));
            com.oupao.wiz.SLog.e("FlyS: \u62a2\u6d41\u5931\u8d25", __e_v0_7);
            return;
        }
        return;
    }
        public static void start(android.content.Context p5)
    {
        android.os.Handler v0_0 = null;
        try {
            com.oupao.wiz.FlyS.ctx = p5;
            com.oupao.wiz.FlyH.ctx = p5;
            com.oupao.wiz.FlyS.wm = ((android.view.WindowManager) p5.getSystemService("window"));
            com.oupao.wiz.SLog.i("FlyS: start() \u62ff\u5230 ctx");
            v0_0 = com.oupao.wiz.Hooks.main();
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_0 != null) {
            v0_0.postDelayed(new com.oupao.wiz.FlyZ(), 10000);
            com.oupao.wiz.SLog.i("FlyS: SystemUI \u5c31\u7eea\uff0c10 \u79d2\u540e\u5efa\u89d2\u843d\u70ed\u533a");
        }
        return;
    }
    public static void sync()
    {
        try {
            if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
                var v0_2 = com.oupao.wiz.Hooks.main();
                if (v0_2 != null) {
                    v0_2.post(new com.oupao.wiz.FlyC());
                    com.oupao.wiz.SLog.i("FlyS: sync -> main");
                    return;
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        com.oupao.wiz.FlyS.syncMain();
        return;
    }
    public static void syncMain()
    {
        try {
            if (com.oupao.wiz.Cfg.flyMaster()) {
                com.oupao.wiz.SLog.r("r-fly-sync-in", "FlyS: sync 进入", 5000L);
                android.content.Context v1_0 = com.oupao.wiz.FlyS.pick();
                if (v1_0 == null) {
                    v1_0 = com.oupao.wiz.FlyS.ctx;
                    if (v1_0 == null) {
                        com.oupao.wiz.SLog.i("FlyS: sync no ctx");
                        return;
                    }
                }
                com.oupao.wiz.FlyS.ctx = v1_0;
                if (com.oupao.wiz.FlyS.wm == null) {
                    com.oupao.wiz.FlyS.wm = ((android.view.WindowManager) v1_0.getSystemService("window"));
                }
                // ★ 2777：zs = **热区窗口边长** = 2 × 热区半径（flyRange×density）。
                //   原来这里把值夹到 `>= 300`，造成两个"设置与实际不一致"：
                //   ① 设 24dp(84px) 时窗口仍是 300px → 实际被拦截的区域是 300 而不是 84；
                //   ② 范围改 24→40 时 zs 恒等于 300 → `zs != v4_2` 不成立 → 窗口根本不重建，改了也不生效。
                // ★ 3270：窗口仍用 2×半径（太小会整块落进"上滑回桌面"的带子里），
                //   但**抢流/屏蔽返回只在手势圆内做**（见 FlyZone.onTouchEvent），
                //   所以屏蔽区域 = 触发范围，会跟着设置变。
                int zrpx = ((int) (((float) com.oupao.wiz.Cfg.flyRange())
                        * v1_0.getResources().getDisplayMetrics().density));
                int v4_2 = Math.max(zrpx * 2, 160);
                // 3261：形状切换也要重建热区窗口
                int v4mode = com.oupao.wiz.Cfg.flyZoneMode();
                boolean modeChanged = (com.oupao.wiz.FlyS.zoneMode != v4mode);
                // ★ 热区几何发布给其它进程（FlyState，sys.oupao.fly.r / .lr）：
                //   SystemUI 的热区窗口（FlyZoneUi）和 Launcher 的闸门（FlyGate）都按它建窗口/判区域。
                //   真值只有一个：flyRange × density（与 FlyH.handle 的判定同值）。
                try {
                    int rpx = ((int) (((float) com.oupao.wiz.Cfg.flyRange()) * v1_0.getResources().getDisplayMetrics().density));
                    // 测试用：`setprop sys.oupao.fly.test_r 400` 可临时把热区半径放大，
                    // 方便 adb 注入的点击坐标确定落在热区内（普通使用不设这个属性）。
                    try {
                        int tr = Integer.parseInt(com.oupao.wiz.FlyState.raw("sys.oupao.fly.test_r").trim());
                        if (tr > 0) {
                            rpx = tr;
                        }
                    } catch (Throwable ignore) {
                    }
                    int lr = 0;
                    if (com.oupao.wiz.Cfg.flyLeft()) {
                        lr = (lr | 1);
                    }
                    if (com.oupao.wiz.Cfg.flyRight()) {
                        lr = (lr | 2);
                    }
                    com.oupao.wiz.FlyState.publish(rpx, lr);
                    // 3130：同进程本地副本（C17 上属性往返读回 0，见字段注释）
                    com.oupao.wiz.FlyS.pubR = rpx;
                    com.oupao.wiz.FlyS.pubLr = lr;
                    // 安全阀：菜单窗口已经不在（比如状态没走到 hide()），但闸门还停在「展开」→ 复位。
                    if ((com.oupao.wiz.FlyH.win == null) && (com.oupao.wiz.FlyH.cat == null)
                            && com.oupao.wiz.FlyState.open()) {
                        com.oupao.wiz.FlyState.setOpen(false);
                    }
                } catch (Throwable ignore) {
                }
                var v0_3 = new StringBuilder();
                v0_3.append("FlyS: sync zs=");
                v0_3.append(com.oupao.wiz.FlyS.zs);
                v0_3.append(" new=");
                v0_3.append(v4_2);
                com.oupao.wiz.SLog.r("r-fly-sync", v0_3.toString(), 3000L);
                if ((com.oupao.wiz.FlyS.zs != v4_2) || modeChanged) {
                com.oupao.wiz.FlyS.kill();
                    com.oupao.wiz.FlyS.zs = v4_2;
                    com.oupao.wiz.FlyS.zoneMode = v4mode;
                    com.oupao.wiz.FlyS.zone = com.oupao.wiz.FlyS.mk(1);
                    com.oupao.wiz.FlyS.zone2 = com.oupao.wiz.FlyS.mk(2);
                    com.oupao.wiz.SLog.i(new StringBuilder("FlyS: \u70ed\u533a\u5df2\u5efa size=").append(v4_2)
                            .append(" mode=").append(v4mode).toString());
                // 诊断：把热区关键状态暴露出来
                try {
                    int r = (int) (((float) com.oupao.wiz.Cfg.flyRange()) * com.oupao.wiz.FlyH.den);
                    var z1 = com.oupao.wiz.FlyS.zone;
                    var z2 = com.oupao.wiz.FlyS.zone2;
                    com.oupao.wiz.Diag.put("FlyM", (com.oupao.wiz.Cfg.flyMaster() ? "1" : "0")
                            + "/L" + (com.oupao.wiz.Cfg.flyLeft() ? "1" : "0")
                            + "R" + (com.oupao.wiz.Cfg.flyRight() ? "1" : "0"));
                    com.oupao.wiz.Diag.put("FlyR", String.valueOf(r));
                    com.oupao.wiz.Diag.put("Zone",
                            "z1=" + ((z1 != null) ? (z1.isAttachedToWindow() ? "att" : "no") : "null")
                            + " z2=" + ((z2 != null) ? (z2.isAttachedToWindow() ? "att" : "no") : "null")
                            + " zs=" + com.oupao.wiz.FlyS.zs
                            + " pil=" + (com.oupao.wiz.FlyS.pil ? 1 : 0));
                    com.oupao.wiz.SLog.i("FlyS: 状态 fly=" + (com.oupao.wiz.Cfg.flyMaster() ? 1 : 0)
                            + " L" + (com.oupao.wiz.Cfg.flyLeft() ? 1 : 0)
                            + "R" + (com.oupao.wiz.Cfg.flyRight() ? 1 : 0)
                            + " r=" + r + " zs=" + com.oupao.wiz.FlyS.zs
                            + " zone=" + (z1 != null) + "/" + (z2 != null));
                } catch (Throwable ignore) {
                }
                } else {
                    // 1820fix: smali 里是三条并列条件都要重建（尺寸变 / zone==null / 未挂载）
                    var v5_2 = com.oupao.wiz.FlyS.zone;
                    if ((v5_2 == null) || (!v5_2.isAttachedToWindow())) {
                        com.oupao.wiz.FlyS.kill();
                        com.oupao.wiz.FlyS.zs = v4_2;
                        com.oupao.wiz.FlyS.zone = com.oupao.wiz.FlyS.mk(1);
                        com.oupao.wiz.FlyS.zone2 = com.oupao.wiz.FlyS.mk(2);
                        com.oupao.wiz.SLog.i("FlyS: 热区重建(此前缺失) z1="
                                + (com.oupao.wiz.FlyS.zone != null)
                                + " z2=" + (com.oupao.wiz.FlyS.zone2 != null));
                    }
                }
                return;
            } else {
                com.oupao.wiz.FlyS.kill();
                return;
            }
        } catch (Throwable __e_v0_5) {
            com.oupao.wiz.SLog.i(String.valueOf(__e_v0_5));
            return;
        }
    }
    public static void syncWith(android.content.Context p3)
    {
        try {
            if (p3 != null) {
                com.oupao.wiz.FlyS.ctx = p3;
                com.oupao.wiz.FlyH.ctx = p3;
                var v0_2 = ((android.view.WindowManager) p3.getSystemService("window"));
                com.oupao.wiz.FlyS.wm = v0_2;
                if (v0_2 != null) {
                    com.oupao.wiz.SLog.r("r-fly-syncwith", "FlyS: syncWith 拿到 wm", 5000L);
                    com.oupao.wiz.FlyS.sync();
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static void trust(android.view.WindowManager.LayoutParams p5)
    {
        java.lang.reflect.Field v0_2 = null;
        try {
            Class[] v2_1 = new Class[0];
            Object[] v1_6 = new Object[0];
            p5.getClass().getMethod("setTrustedOverlay", v2_1).invoke(p5, v1_6);
            v0_2 = null;
            try {
                v0_2 = p5.getClass().getField("inputFeatures");
                v0_2.setAccessible(true);
                v0_2.setInt(p5, (v0_2.getInt(p5) | 4));
                return;
            } catch (Throwable __e_v0) {
                return;
            }
        } catch (Throwable __e_v0_) {
            return;
        }
    }
    public static void trust2(android.view.WindowManager.LayoutParams p5)
    {
        java.lang.reflect.Field v0_2 = null;
        try {
            Class[] v2_1 = new Class[0];
            Object[] v1_5 = new Object[0];
            p5.getClass().getMethod("setTrustedOverlay", v2_1).invoke(p5, v1_5);
            try {
                v0_2 = p5.getClass().getField("inputFeatures");
                v0_2.setAccessible(true);
                v0_2.setInt(p5, 0);
                return;
            } catch (Throwable __e_v0) {
                return;
            }
        } catch (Throwable __e_v0_) {
            return;
        }
    }
    public static boolean tryCtx(android.content.Context p3)
    {
        try {
            if (p3 != null) {
                var v0_4 = ((android.view.WindowManager) p3.getSystemService("window"));
                if (v0_4 != null) {
                    com.oupao.wiz.FlyS.ctx = p3;
                    com.oupao.wiz.FlyH.ctx = p3;
                    com.oupao.wiz.FlyS.wm = v0_4;
                    return true;
                }
            }
        } catch (Throwable __e_v0) {
            return false;
        }
        return false;
    }
    public static void unpil()
    {
        com.oupao.wiz.FlyS.pil = false;
        return;
    }
}