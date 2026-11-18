package com.oupao.wiz;
public class Sug {
    static java.util.HashMap badgeIcons;
    /** 通知所属 uid 的快照（show2 时记录；绘制是延迟到主线程的，直接读 Sug.uid 会被别的通知覆盖） */
    static int kUid;
    public static android.view.View banner;
    /**
     * 4.0-fix：**所有气泡/横幅窗口都登记在这里**，dismiss 时逐个强删。
     *
     * 为什么需要：`dismiss()` 原来只删 `Sug.view` 一个引用。只要有一次状态错位
     * （Bub 接管窗口、removeView 抛异常、连续 show 覆盖引用），那个窗口就再也没人删得掉 ——
     * 屏上一直挂着，点击只打 `Sug: fire kind=0`（kind 已被清 0），下滑也无效。
     * 用户 2026-11-11 19:17 的日志就是连点 10 次 kind=0。
     */
    public static final java.util.ArrayList<android.view.View> winViews =
            new java.util.ArrayList<android.view.View>();
    public static int bbX1;
    public static int bbX2;
    public static int bbY1;
    public static int bbY2;
    public static android.graphics.Bitmap bmp;
    public static long btGuard;
    public static int bubUid;
    public static android.hardware.camera2.CameraManager cam;
    public static String chatUser;
    public static android.content.Context ctx;
    public static String curChat;
    public static String curPkg;
    public static boolean dbg;
    public static boolean dim;
    public static boolean dimLocked;
    public static boolean errd;
    public static String gameFgPkg;
    public static boolean gameOn;
    public static android.view.View hintV;
    public static boolean hit;
    public static boolean hit2;
    public static int imeVis;
    public static String kChat;
    public static android.content.Intent kNi;
    public static android.app.PendingIntent kPi;
    public static String kPkg;
    public static String kType;
    public static String kUin;
    public static boolean keepActive;
    public static String keepPkg;
    public static int keepSrc;
    public static int keepTick;
    public static android.os.PowerManager.WakeLock keepWL;
    public static int kind;
    public static long lastAt;
    public static String link;
    public static long linkGuardAt;
    public static String linkGuardPkg;
    public static String lurl;
    public static android.os.Handler main;
    public static android.content.Intent ni;
    public static boolean pendGame;
    public static int pendKind;
    public static boolean pendOn;
    public static String pendPkg;
    public static Object pg;
    public static android.app.PendingIntent pi;
    public static String pkg;
    /** 3192：当前气泡属于哪个用户（0=主应用 / 999=分身），收气泡/屏蔽判定要按用户比，不能只看包名 */
    public static int bubbleUser = 0;
    public static java.lang.reflect.Method pm;
    public static int pol;
    private static boolean prs;
    public static String qqType;
    public static String qqUin;
    private static boolean shown;
    private static boolean tickOn;
    public static android.graphics.Bitmap ticon;
    public static Runnable timer;
    // 3320：还没弹出的「手电筒 5 秒延时任务」（关灯时要能撤掉，开灯重复回调时不能重复排）
    public static Runnable torchPend;
    public static String torchId;
    public static boolean torchOn;
    public static boolean tried;
    public static String txt;
    public static int uid;
    private static android.widget.TextView view;
    private static android.view.WindowManager wm;
    public static boolean wordBubble;
    public Sug()
    {
        return;
    }
    public static android.graphics.Bitmap appIcon(String p5)
    {
        android.graphics.Bitmap v4 = null;
        java.util.HashMap v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Sug.badgeIcons;
        } catch (Throwable __e_v1) {
            return v4;
        }
        if (v0_0 == null) {
            v0_0 = new java.util.HashMap();
            com.oupao.wiz.Sug.badgeIcons = v0_0;
        }
        var v1_3 = v0_0.get(p5);
        if (!(v1_3 instanceof android.graphics.Bitmap)) {
            android.content.Context v1_0 = com.oupao.wiz.Sug.ctx;
            if (v1_0 == null) {
                v1_0 = com.oupao.wiz.Ctx.get();
            }
            if (v1_0 != null) {
                var v2_0 = v1_0.getPackageManager();
                if (v2_0 != null) {
                    try {
                        var v2_1 = v2_0.getApplicationIcon(p5);
                        if (v2_1 != null) {
                            var v2_2 = com.oupao.wiz.Sug.iconOf(v2_1, com.oupao.wiz.Ui.dp(v1_0, 96));
                            if (v2_2 != null) {
                                v0_0.put(p5, v2_2);
                                v4 = v2_2;
                                if (v0_0.size() > 48) {
                                    v0_0.clear();
                                }
                            }
                        }
                    } catch (Throwable __e_io) {
                    }
                }
            }
            return v4;
        } else {
            return ((android.graphics.Bitmap) v1_3);
        }
    }
    public static void appKeepTick()
    {
        try {
            if ((com.oupao.wiz.Sug.keepSrc == 2) && (com.oupao.wiz.Sug.keepActive)) {
                var v0_6 = (com.oupao.wiz.Sug.keepTick + 1);
                com.oupao.wiz.Sug.keepTick = v0_6;
                if ((v0_6 % 10) == 0) {
                    com.oupao.wiz.Sug.fgLog("SugK tick");
                }
                // smali 里 keepOff() 只在 4 个"该释放"的分支里通过 goto 走到，
                // 移植时被拍平成无条件调用 → 点气泡后下一个 tick 就把常亮 WakeLock 放掉，
                // 表现为"点了有提示、但屏幕照样熄灭"。
                // 释放判据只用"点击时记下的 keepPkg vs 当前前台包名"。
                // 原来第一关是 fgApp()，而 fgApp() 把 com.oupao.wiz（模块自己）、systemui、桌面
                // 都算成"不是应用" —— 在模块自己的「屏幕常亮」页里点 ☀ 时 fg 就是 com.oupao.wiz，
                // 于是第一个 tick 就"已离开应用"把锁放掉（日志：开→0.4s 后释放）。
                boolean release = false;
                if (!com.oupao.wiz.Cfg.appKeep()) {
                    com.oupao.wiz.SLog.i("SugK 释放: 总开关已关");
                    release = true;
                } else {
                    var v2 = com.oupao.wiz.Sug.keepPkg;
                    if (v2 == null) {
                        // 点击时没取到包名：退回原逻辑，不在"真应用"里就释放
                        if (!com.oupao.wiz.Sug.fgApp()) {
                            com.oupao.wiz.SLog.i("SugK 释放: 已离开应用(桌面/系统界面)");
                            release = true;
                        }
                    } else {
                        var v3 = com.oupao.wiz.Sug.fgPkg();
                        if (v3 == null) {
                            // 取不到就再等一拍，别因为一次瞬时失败把常亮掐掉
                            com.oupao.wiz.SLog.i("SugK tick: 前台包名取不到, 继续常亮");
                        } else if (!v3.equals(v2)) {
                            var v1_6 = new StringBuilder();
                            v1_6.append("SugK 释放: 已切换到其他应用 fg=");
                            v1_6.append(v3);
                            v1_6.append(" keep=");
                            v1_6.append(v2);
                            com.oupao.wiz.SLog.i(v1_6.toString());
                            release = true;
                        }
                    }
                }
                if (release) {
                    com.oupao.wiz.Sug.keepOff();
                }
            }
        } catch (Throwable __e_v0) {
            com.oupao.wiz.SLog.e("Sug: 常亮 tick 异常", __e_v0);
            return;
        }
        return;
    }
    public static void applyPos(android.view.WindowManager.LayoutParams p5)
    {
        p5.gravity = 8388691;
        p5.x = com.oupao.wiz.Ctx.dp(12);
        p5.y = com.oupao.wiz.Ctx.dp(96);
        java.lang.String v0_2 = "";
        try {
            // 竖屏/横屏各存一份（BubPos.key 按当前屏幕方向选键），
            // 且必须绕过 Cfg 的进程级缓存：气泡位置是 App 侧写的，
            // system_server 这边的 cS 一旦缓存了空串就再也不刷新（表现为位置改不动）
            var c = com.oupao.wiz.Sug.ctx;
            // 合并气泡模式下，位置以 Bub 自己保存的为准（拖动时也同步写回旧键），
            // 这样 show() 第一次摆窗口就在正确位置，不会「先闪一下默认位置再跳过去」。
            int[] bub2 = com.oupao.wiz.Bub.posForSug();
            if (bub2 != null) {
                p5.gravity = 8388659;
                p5.flags = (p5.flags | 0x100);
                p5.x = bub2[0];
                p5.y = bub2[1];
                com.oupao.wiz.SLog.i("Sug: 用合并气泡位置 x=" + bub2[0] + " y=" + bub2[1]);
                return;
            }
            v0_2 = com.oupao.wiz.BubPos.raw(c);
            int[] xy = com.oupao.wiz.BubPos.parse(v0_2);
            if (xy != null) {
                xy = com.oupao.wiz.BubPos.clamp(c, xy, com.oupao.wiz.Ctx.dp(46));
                p5.gravity = 8388659;
                p5.x = xy[0];
                p5.y = xy[1];
                // savePos 存的是 getLocationOnScreen() 的【屏幕绝对坐标】，
                // 而 2003 窗口默认以"状态栏下方"为坐标原点 → 实际会多下移一个状态栏高度
                // （实测 d=(64,199)，其中 64 是 jelly 缩放动画的测量假象，135 才是状态栏）。
                // FLAG_LAYOUT_NO_LIMITS(0x200) 只放开越界限制、不改原点，没用；
                // 真正把原点拉到整屏的是 FLAG_LAYOUT_IN_SCREEN(0x100)。
                p5.flags = (p5.flags | 0x100);
            }
        } catch (Throwable __e_v0) {
            return;
        }
        try {
            com.oupao.wiz.SLog.i("Sug: bub_pos[" + com.oupao.wiz.BubPos.key(com.oupao.wiz.Sug.ctx)
                    + "] raw=" + v0_2 + " -> gravity=" + p5.gravity + " x=" + p5.x + " y=" + p5.y);
        } catch (Throwable ignore) {
        }
        return;
    }
    /**
     * 取「指定用户下」的系统带角标图标（LauncherApps.getBadgedIcon）。
     *
     * 分身应用（user=999）的 badged icon 里**自带系统画的分身角标**，
     * 所以用它就不用自己画「分」字了，视觉与桌面完全一致。
     */
    public static android.graphics.Bitmap appIconUser(String pkg, int userId) {
        try {
            android.content.Context c = com.oupao.wiz.Sug.ctx;
            if (c == null) {
                c = com.oupao.wiz.Ctx.get();
            }
            if ((c == null) || (pkg == null)) {
                return null;
            }
            var la = (android.content.pm.LauncherApps) c.getSystemService("launcherapps");
            if (la == null) {
                return null;
            }
            // UserHandle.of(int) 在当前 compileSdk 的 android.jar 里没有，用老 API：
            // getUserId() = uid / 100000，所以反过来 uid = userId * 100000
            var uh = android.os.UserHandle.getUserHandleForUid(userId * 100000);
            var acts = la.getActivityList(pkg, uh);
            if ((acts == null) || (acts.isEmpty())) {
                return null;
            }
            int dens = c.getResources().getDisplayMetrics().densityDpi;
            var d = acts.get(0).getBadgedIcon(dens);
            if (d == null) {
                return null;
            }
            // 用户确认「方的就方的」，保持 2206 的做法（iconOf 原样渲染系统 badged icon）
            var bm = com.oupao.wiz.Sug.iconOf(d, com.oupao.wiz.Ui.dp(c, 96));
            com.oupao.wiz.SLog.i("appIconUser: pkg=" + pkg + " user=" + userId
                    + " bmp=" + ((bm == null) ? -1 : bm.getWidth()));
            return bm;
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("appIconUser \u5f02\u5e38", t);
            return null;
        }
    }

    public static android.graphics.Bitmap badgeApp(android.graphics.Bitmap p2, int p3)
    {
        java.lang.String v0 = null;
        try {
            v0 = com.oupao.wiz.Sug.pkg;
        } catch (Throwable __e_v0) {
            return p2;
        }
        if (v0 != null) {
            // 分身（用户号 != 0）要用该用户下的 badged icon —— 里面自带系统分身角标
            int vUser = com.oupao.wiz.Sug.kUid;
            var v1 = (vUser != 0) ? com.oupao.wiz.Sug.appIconUser(v0, vUser)
                    : com.oupao.wiz.Sug.appIcon(v0);
            if ((v1 == null) && (vUser != 0)) {
                v1 = com.oupao.wiz.Sug.appIcon(v0);
            }
            if ((v1 == null) || (com.oupao.wiz.Sug.opaque(v1) < 4)) {
                // 角标图取不到/为空 → 直接用应用图标自算包围盒重画，保证角标一定画得出来
                try {
                    var c0 = com.oupao.wiz.Sug.ctx;
                    if (c0 != null) {
                        var d0 = c0.getPackageManager().getApplicationIcon(v0);
                        v1 = com.oupao.wiz.Sug.circleFitIcon(d0, com.oupao.wiz.Ui.dp(c0, 96));
                    }
                } catch (Throwable tb) {
                    com.oupao.wiz.SLog.e("badgeApp: 取图标异常", tb);
                }
                com.oupao.wiz.SLog.i("badgeApp: 兜底 pkg=" + v0 + " bmp=" + (v1 != null));
            }
            if (v1 != null) {
                p2 = com.oupao.wiz.Sug.badgeInto(p2, p3, v1);
            } else {
                com.oupao.wiz.SLog.i("badgeApp: 角标图仍为空 pkg=" + v0);
            }
        }
        return p2;
    }
    /**
     * 1820fix: 分身编号跟随系统 —— 枚举该包在各分身用户下的安装情况，按 userId 升序取序号。
     * 返回 0 表示不是分身（主用户）或无法判定。
     */
    /**
     * 1820fix: 分身编号跟随系统 —— 取 UserManager 的非主用户（分身用户）升序序号。
     * ColorOS 上第 1 个分身 user=999，与系统桌面显示的 1 完全一致。
     */
    /**
     * 1820fix: 分身编号跟随系统 —— 读系统真实分身用户（ColorOS 为 997/998/999 MultiApp），
     * 只统计该包已安装的分身用户，按 userId 升序 → 1/2/3…，与系统 MultiApp 顺序一致。
     */
    // ───────────────── 3416：系统原生分身角标（不再裁切/差分） ─────────────────
    //
    // 系统侧实证链路（C16/C17 一致）：
    //   framework.jar  ApplicationPackageManager.getUserBadgeForDensity(UserHandle,int)
    //     → oplus-framework.jar  ApplicationPackageManagerExtImpl.getMultiAppUserBadgeId(user,true)
    //     → OplusMultiAppManager.getMultiAppBadgeResourceId(user, 2)
    //     → system_server  OplusMultiAppManagerService 的 packed-switch
    //     → CloneUserUtils.BADGE_WITH_BG_RESOURCES[999 - user]
    //     → /system_ext/framework/oplus-framework-res.apk  (动态包 id 0x0c)
    //       drawable/oplus_ic_corp_badge_case_multiapp{,_1.._5}  (绿底白数字 1..5)
    //
    // 也就是说：只要把 user 交给 framework，它自己就会按 999−user 选好带编号的那张。
    // 以前模块是「取系统合成好的 badged icon，再裁 40px / 逐像素差分抠角标」，
    // 尺寸一变就抠歪、遇到白色图标还会啃掉角标。这里直接取**角标本身**的 drawable，
    // 按系统比例贴到图标角落，形状/颜色/编号与桌面 100% 一致。

    /**
     * 取该分身用户的系统原生角标 drawable（不含应用图标）。
     * 优先 framework 的 getUserBadgeForDensity；拿不到再走 OPlus 资源 id 兜底。
     */
    public static android.graphics.drawable.Drawable nativeCloneBadge(int user)
    {
        try {
            if (user <= 0) {
                return null;
            }
            int uid = (user >= 100000) ? (user / 100000) : user;
            var ctx = com.oupao.wiz.Cfg.sysContext();
            if (ctx == null) {
                return null;
            }
            var pm = ctx.getPackageManager();
            var uh = com.oupao.wiz.FrameworkExt.userHandleOf(uid);
            if ((pm == null) || (uh == null)) {
                return null;
            }
            // 1) framework 正规通道：内部就是 ExtImpl.getMultiAppUserBadgeId → 999−user
            try {
                var m = pm.getClass().getMethod("getUserBadgeForDensity",
                        new Class[]{android.os.UserHandle.class, Integer.TYPE});
                Object d = m.invoke(pm, new Object[]{uh, Integer.valueOf(0)});
                if ((d instanceof android.graphics.drawable.Drawable)) {
                    com.oupao.wiz.SLog.r("clone", "clone: 系统角标 getUserBadgeForDensity u=" + uid, 5000L);
                    return (android.graphics.drawable.Drawable) d;
                }
            } catch (Throwable t1) {
                com.oupao.wiz.SLog.v("clone: getUserBadgeForDensity 不可用 " + t1);
            }
            // 2) 直接反射 OplusMultiAppManager.getMultiAppBadgeResourceId(user,2) 拿资源 id，
            //    再由 Resources.getDrawable(id) 取图（跨版本 id 变化时仍能命中）。
            try {
                var mc = Class.forName("com.oplus.multiapp.OplusMultiAppManager");
                var inst = com.oupao.wiz.Rt.invoke(
                        com.oupao.wiz.Rt.methodByName(mc, "getInstance", 0), null, null);
                var mm = com.oupao.wiz.Rt.methodByName(mc, "getMultiAppBadgeResourceId", 2);
                Object idObj = com.oupao.wiz.Rt.invoke(mm, inst,
                        new Object[]{Integer.valueOf(uid), Integer.valueOf(2)});
                int id = (idObj instanceof Number) ? ((Number) idObj).intValue() : -1;
                if (id > 0) {
                    var d = ctx.getResources().getDrawable(id, ctx.getTheme());
                    if (d != null) {
                        com.oupao.wiz.SLog.r("clone", "clone: 系统角标 资源id=0x"
                                + Integer.toHexString(id) + " u=" + uid, 5000L);
                        return d;
                    }
                }
            } catch (Throwable t2) {
                com.oupao.wiz.SLog.v("clone: OplusMultiAppManager 不可用 " + t2);
            }
            // 3) 硬编码兜底：0x0c080120 + (999-uid)，即 BADGE_WITH_BG_RESOURCES[999-uid]
            int idx = 999 - uid;
            if ((idx >= 0) && (idx <= 4)) {
                try {
                    var d = ctx.getResources().getDrawable(0x0c080120 + idx, ctx.getTheme());
                    if (d != null) {
                        com.oupao.wiz.SLog.r("clone", "clone: 系统角标 硬编码 idx=" + idx
                                + " u=" + uid, 5000L);
                        return d;
                    }
                } catch (Throwable t3) {
                    com.oupao.wiz.SLog.v("clone: 硬编码角标不可用 " + t3);
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.v("clone: 取系统角标异常 " + t);
        }
        return null;
    }

    /**
     * 把系统原生角标 drawable 按系统比例贴到底图上。
     * 系统桌面角标约占图标 40% 宽、贴右下（左呼出贴左下），角标本身不镜像。
     */
    public static android.graphics.Bitmap badgeWithNative(android.graphics.Bitmap base,
            int size, int user, boolean left)
    {
        try {
            if ((base == null) || (size <= 0)) {
                return null;
            }
            var d = nativeCloneBadge(user);
            if (d == null) {
                return null;
            }
            // 角标原始图 96px 对应桌面图标约 1/2.5；这里取 42% 与系统桌面一致
            int bs = ((size * 42) / 100);
            if (bs < 8) {
                bs = 8;
            }
            var bmp = drawableToBitmap(d, bs);
            if (bmp == null) {
                return null;
            }
            var out = base.copy(android.graphics.Bitmap.Config.ARGB_8888, true);
            var c = new android.graphics.Canvas(out);
            int m = ((size * 2) / 100);
            float dx = left ? ((float) m) : ((float) (size - bs - m));
            c.drawBitmap(bmp, dx, ((float) (size - bs - m)), null);
            com.oupao.wiz.SLog.i("clone: 系统原生角标贴好了 u=" + user + " bs=" + bs
                    + " left=" + left + " size=" + size);
            // 诊断开关（默认关）：把合成结果写成 PNG，用于肉眼核对角标形状/编号。
            //   setprop persist.oupao.badge_dbg 1 → 拉 /data/system/oupao_badge_dbg_<user>.png
            if ("1".equals(com.oupao.wiz.Cfg.getProp("persist.oupao.badge_dbg", "0"))) {
                try {
                    int uu = (user >= 100000) ? (user / 100000) : user;
                    var fo = new java.io.FileOutputStream(
                            "/data/system/oupao_badge_dbg_" + uu + ".png");
                    out.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fo);
                    fo.close();
                } catch (Throwable ignore) {
                }
            }
            return out;
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("clone: 贴系统角标失败", t);
            return null;
        }
    }

    /** 3416：分身图标 = 底图（圆形或方形）+ 系统原生角标；取不到系统角标才回退自绘数字。 */
    public static android.graphics.Bitmap cloneBitmap(android.graphics.drawable.Drawable icon,
            int size, int user, boolean circle, boolean left)
    {
        try {
            if ((icon == null) || (size <= 0)) {
                return null;
            }
            var base = circle ? circleFitIcon(icon, size) : drawableToBitmap(icon, size);
            if (base == null) {
                return null;
            }
            var withNative = badgeWithNative(base, size, user, left);
            if (withNative != null) {
                return withNative;
            }
            int n = cloneIndexOf(null, user);   // 不需要包名，按 user 推算系统编号
            if (n <= 0) {
                n = 1;
            }
            var num = numBadge(base, size, n, left);
            com.oupao.wiz.SLog.i("clone: 系统角标取不到 → 自绘数字 #" + n + " u=" + user);
            return (num != null) ? num : base;
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("clone: cloneBitmap 异常", t);
            return null;
        }
    }

    public static int cloneIndexOf(String pkg, int uid)
    {
        try {
            if (uid <= 0) {
                return 0;
            }
            // 系统口径（OplusMultiAppManagerService.getResourceForUser）：
            //   下标 = 999 - userId，即 user=999→1、998→2、997→3。
            //   以前这里是「按 userId 升序取序号」，正好和桌面反着来。
            int user = (uid >= 100000) ? (uid / 100000) : uid;
            int idx = 999 - user;
            if ((idx >= 0) && (idx <= 4)) {
                return idx + 1;
            }
            return 0;
        } catch (Throwable t) {
            return 0;
        }
    }


    /** 3416：分身图标（圆形）= 圆底图 + 系统原生角标；取不到才回退自绘数字。 */
    public static android.graphics.Bitmap cloneIconCircle(String pkg, int user, int size, boolean left)
    {
        try {
            var ctx = com.oupao.wiz.Cfg.sysContext();
            if (ctx == null) return null;
            android.graphics.drawable.Drawable art = null;
            var la = (android.content.pm.LauncherApps) ctx.getSystemService("launcherapps");
            var uh = com.oupao.wiz.FrameworkExt.userHandleOf(user);
            if (la != null && uh != null) {
                var acts = la.getActivityList(pkg, uh);
                if (acts != null && !acts.isEmpty())
                    art = ((android.content.pm.LauncherActivityInfo) acts.get(0))
                            .getIcon(ctx.getResources().getDisplayMetrics().densityDpi);
            }
            if (art == null) art = ctx.getPackageManager().getApplicationIcon(pkg);
            return cloneBitmap(art, size, user, true, left);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("clone: cloneIconCircle 异常", t);
            return null;
        }
    }

    /** Drawable → Bitmap（固定尺寸，铺满） */

    /** 1820fix: 在图标右下角最边画数字角标（分身编号 1/2/3…）。 */
    public static android.graphics.Bitmap numBadge(android.graphics.Bitmap src, int size, int n, boolean left)
    {
        if ((src == null) || (size <= 0) || (n <= 0)) {
            return src;
        }
        try {
            var out = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888);
            var c = new android.graphics.Canvas(out);
            c.drawBitmap(src, 0.0f, 0.0f, null);
            int w = ((size * 46) / 100);
            if (w < 8) {
                w = 8;
            }
            var bg = android.graphics.Bitmap.createBitmap(w, w, android.graphics.Bitmap.Config.ARGB_8888);
            var cb = new android.graphics.Canvas(bg);
            var p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            p.setColor(-1722953);                 // 分身角标底色
            cb.drawCircle((((float) w) / 2.0f), (((float) w) / 2.0f), (((float) w) / 2.0f), p);
            var tp = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            tp.setColor(-1);
            tp.setTextSize(((float) w) * 0.60f);
            tp.setTextAlign(android.graphics.Paint.Align.CENTER);
            tp.setFakeBoldText(true);
            var fm = tp.getFontMetrics();
            float ty = ((((float) w) / 2.0f) - ((fm.ascent + fm.descent) / 2.0f));
            cb.drawText(String.valueOf(n), (((float) w) / 2.0f), ty, tp);
            int m = ((size * 2) / 100);
            float bx = left ? ((float) m) : ((float) (size - w - m));
            c.drawBitmap(bg, bx, ((float) (size - w - m)), null);
            return out;
        } catch (Throwable t) {
            return src;
        }
    }

    public static android.graphics.Bitmap badgeInto(android.graphics.Bitmap p10, int p11, android.graphics.Bitmap p12)
    {
        try {
            if ((p10 != null) && (p12 != null)) {
                var v0_1 = ((p11 * 48) / 100);   // 1820fix: 角标略大
                var v1_1 = ((p11 * 2) / 100);    // 1820fix: 贴到气泡右下最边（原 9%）
                var v2_2 = (((p11 * 6) / 100) + 1);
                var v3_0 = (v0_1 - (v2_2 * 2));
                if (v3_0 >= 1) {
                    var v4_2 = android.graphics.Bitmap.createBitmap(v0_1, v0_1, android.graphics.Bitmap.Config.ARGB_8888);
                    var v5_1 = new android.graphics.Canvas(v4_2);
                    new android.graphics.Paint(3);
                    var v6_1 = com.oupao.wiz.Sug.circle(p12, v3_0);
                    if (v6_1 != null) {
                        v5_1.drawBitmap(v6_1, ((float) v2_2), ((float) v2_2), null);
                    }
                    new android.graphics.Canvas(p10).drawBitmap(v4_2, ((float) ((p11 - v0_1) - v1_1)), ((float) ((p11 - v0_1) - v1_1)), null);
                }
            }
        } catch (Throwable __e_v0) {
            return p10;
        }
        return p10;
    }
    public static void boot()
    {
        try {
            new Thread(new com.oupao.wiz.Sug$Boot()).start();
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    static String browserPkg(android.content.pm.PackageManager p6)
    {
        android.content.pm.ResolveInfo v1_1 = null;
        try {
            var v1_6 = new android.content.Intent("android.intent.action.VIEW");
            v1_6.setData(android.net.Uri.parse("https://example.com/"));
            v1_1 = p6.resolveActivity(v1_6, 65536);
        } catch (Throwable __e_v0) {
            return null;
        }
        if (v1_1 != null) {
            var v2_0 = v1_1.activityInfo;
            if (v2_0 != null) {
                var v2_1 = v2_0.packageName;
                if ((v2_1 != null) && (p6.getLaunchIntentForPackage(v2_1) != null)) {
                    return v2_1;
                }
            }
        }
        String[] v0_0 = new String[8];
        v0_0[0] = "mark.via";
        v0_0[1] = "com.heytap.browser";
        v0_0[2] = "com.android.chrome";
        v0_0[3] = "com.android.browser";
        v0_0[4] = "org.mozilla.firefox";
        v0_0[5] = "com.quark.browser";
        v0_0[6] = "com.UCMobile";
        v0_0[7] = "com.tencent.mtt";
        int v1_12 = 0;
        while (v1_12 < 8) {
            var v2_13 = v0_0[v1_12];
            if (p6.getLaunchIntentForPackage(v2_13) == null) {
                v1_12++;
            } else {
                return v2_13;
            }
        }
        return null;
    }
    public static int bubY()
    {
        var c = com.oupao.wiz.Sug.ctx;
        try {
            int[] xy = com.oupao.wiz.BubPos.parse(com.oupao.wiz.BubPos.raw(c));
            if (xy != null) {
                return xy[1];
            }
        } catch (Throwable __e_v0) {
        }
        if (c == null) {
            return 96;
        }
        return com.oupao.wiz.Ui.dp(c, 96);
    }
    private static void bubbleFor(android.content.Context p4, String p5, String p6)
    {
        try {
            if (com.oupao.wiz.Sug.main != null) {
                var v0_6 = p4.getPackageManager();
                var v1_2 = com.oupao.wiz.Ui.dp(p4, 96);
                var v2 = v0_6.getApplicationIcon(p5);
                android.graphics.Bitmap v3_1 = null;
                if (v2 != null) {
                    v3_1 = com.oupao.wiz.Sug.iconRound(com.oupao.wiz.Sug.iconOf(v2, v1_2), v1_2);
                }
                if (v3_1 != null) {
                    com.oupao.wiz.Sug.wordBubble = true;
                    com.oupao.wiz.Sug.link = com.oupao.wiz.Sug.scheme(p5);
                    com.oupao.wiz.Sug.linkGuardPkg = p5;
                    com.oupao.wiz.Sug.linkGuardAt = System.currentTimeMillis();
                    com.oupao.wiz.SLog.i("Clip: \u53e3\u4ee4\u6c14\u6ce1\u5df2\u8bf7\u6c42");
                    com.oupao.wiz.Sug.showLink(p5, p6, v3_1);
                    return;
                }
            }
        } catch (Throwable __e_v0_7) {
            com.oupao.wiz.SLog.e("Clip: bubbleFor \u5f02\u5e38", __e_v0_7);
            return;
        }
        com.oupao.wiz.SLog.i("Clip: \u53e3\u4ee4\u6c14\u6ce1\u672a\u663e\u793a(main/\u56fe\u6807\u4e0d\u53ef\u7528), \u4e0d\u515c\u5e95");
        return;
    }
    public static void cancelTimer()
    {
        java.lang.Runnable v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Sug.timer;
        } catch (Throwable __e_v0) {
            com.oupao.wiz.Sug.timer = null;
            return;
        }
        if (v0_0 != null) {
            var v1 = com.oupao.wiz.Sug.main;
            if (v1 != null) {
                v1.removeCallbacks(v0_0);
            }
            com.oupao.wiz.Sug.timer = null;
            com.oupao.wiz.SLog.k("Sug: timer cancelled");
        }
        return;
    }
    public static void capLog()
    {
        java.io.File v0_1 = null;
        try {
            v0_1 = new java.io.File("/data/system/oupao.log");
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_1.length() >= 2097152) {
            try {
                new java.io.FileOutputStream(v0_1, false).close();
                com.oupao.wiz.SLog.i("SLog: \u65e5\u5fd7\u8d85\u8fc7 2MB\uff0c\u5df2\u6e05\u7a7a");
            } catch (Throwable __e_io) {
            }
        }
        return;
    }
    public static android.content.Intent chatIntent(String p3)
    {
        var v0_1 = new android.content.Intent();
        v0_1.setClassName("com.tencent.mm", "com.tencent.mm.ui.chatting.variants.ChattingMainUI");
        v0_1.putExtra("Chat_User", p3);
        v0_1.putExtra("Main_UI_From_Deeplink", true);
        v0_1.addFlags(872415232);
        return v0_1;
    }
    public static android.graphics.Bitmap circle(android.graphics.Bitmap p9, int p10)
    {
        android.graphics.Bitmap v1 = null;
        int v0 = 0;
        int v2_1 = 0;
        try {
            v0 = p9.getWidth();
            v2_1 = p9.getHeight();
        } catch (Throwable __e_v3) {
            return v1;
        }
        if ((v0 > 0) && (v2_1 > 0)) {
            v1 = android.graphics.Bitmap.createBitmap(p10, p10, android.graphics.Bitmap.Config.ARGB_8888);
            var v3_1 = new android.graphics.Canvas(v1);
            var v4_1 = new android.graphics.Paint(1);
            var v5_2 = new android.graphics.BitmapShader(p9, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP);
            var v6_2 = new android.graphics.Matrix();
            v6_2.setScale((((float) p10) / ((float) v0)), (((float) p10) / ((float) v2_1)));
            v5_2.setLocalMatrix(v6_2);
            v4_1.setShader(v5_2);
            v3_1.drawCircle((((float) p10) / 2.0f), (((float) p10) / 2.0f), (((float) p10) / 2.0f), v4_1);
        }
        return v1;
    }
    public static String clean(String p5)
    {
        java.lang.String v4 = null;
        try {
            if (p5 != null) {
                java.lang.String v0_1 = p5.trim();
                if ((v0_1.length() >= 1) && (v0_1.charAt(0) == 91)) {
                    var v1_2 = v0_1.indexOf(93);
                    if ((v1_2 >= 1) && (20 >= v1_2)) {
                        v0_1 = v0_1.substring((v1_2 + 1)).trim();
                    }
                }
                var v3_0 = com.oupao.wiz.Sug.cut2(v0_1, v0_1.indexOf(": "), 2);
                if (v3_0 == null) {
                    var v3_1 = com.oupao.wiz.Sug.cut2(v0_1, v0_1.indexOf("\uff1a"), 1);
                    if (v3_1 != null) {
                        v0_1 = v3_1;
                    }
                } else {
                    v0_1 = v3_0;
                }
                if (v0_1.length() >= 1) {
                    v4 = v0_1;
                }
            }
        } catch (Throwable __e_v0) {
            return p5;
        }
        if (v4 == null) {
            return p5;
        } else {
            return v4;
        }
    }
    /**
     * 带标题的 clean：`仲夏: 正文` 这种前缀，
     * - 前缀 == 标题的一部分（单聊：标题就是对方）→ 照旧剥掉
     * - 前缀不是标题（群聊：标题是群名）→ **保留发信人**
     */
    static String cleanWithTitle(String p5, String title)
    {
        try {
            if (p5 == null) return null;
            String t = p5.trim();
            if (t.length() >= 1 && t.charAt(0) == 91) {
                int e = t.indexOf(93);
                if ((e >= 1) && (e <= 20)) t = t.substring(e + 1).trim();
            }
            int ci = t.indexOf(": ");
            int cut = 2;
            if (ci < 0) {
                ci = t.indexOf("\uff1a");
                cut = 1;
            }
            if ((ci >= 1) && (ci <= 12)) {
                String cand = t.substring(0, ci).trim();
                boolean nameLike = (cand.length() >= 1) && (cand.length() <= 12)
                        && (!cand.matches("[0-9]+")) && (!cand.contains("/")) && (!cand.contains(" "));
                boolean keep = nameLike && (title != null) && (title.length() > 0)
                        && (!title.contains(cand));
                if (keep) {
                    com.oupao.wiz.SLog.i("Sug: 保留发信人 " + cand + " title=" + title);
                    return t;
                }
                String rest = t.substring(Math.min(t.length(), ci + cut)).trim();
                if (rest.length() >= 1) t = rest;
            }
            return (t.length() >= 1) ? t : p5;
        } catch (Throwable __e) {
            return p5;
        }
    }

    public static String cut2(String p4, int p5, int p6)
    {
        java.lang.String v3_0 = null;
        try {
            if ((p4 != null) && ((p5 >= 1) && (13 >= p5))) {
                var v0_4 = (p5 + p6);
                if (p4.length() >= v0_4) {
                    var v0_1 = p4.substring(v0_4).trim();
                    if (v0_1.length() >= 1) {
                        v3_0 = v0_1;
                    }
                }
            }
        } catch (Throwable __e_v0) {
            return null;
        }
        return v3_0;
    }
    public static void delayInit()
    {
        try {
            com.oupao.wiz.Sug.capLog();
            new Thread(new com.oupao.wiz.Sug$T()).start();
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    /** 新版气泡（Bub）要接管 Sug 已经建好的头像气泡 View */
    public static android.widget.TextView bubView() {
        return view;
    }

    /** 本次 show() 的 kind：Bub.posForSug() 用它判断该不该用「合并气泡」的位置 */
    public static int showKind = 0;

    /** 仅自测用：绕过「同一应用不弹」「游戏模式屏蔽」等环境判定（TestNotif 临时置 true） */
    public static boolean testBypass = false;

    /** 新版气泡（Bub）要复用同一个 WindowManager */
    public static android.view.WindowManager bubWm() {
        return wm;
    }

    /** 新版气泡自己管窗口，需要同步 shown 标记 */
    public static void bubShown(boolean b) {
        shown = b;
    }

    public static void dismiss()
    {
        try {
            // 只有「被收掉的是消息气泡(kind=2)」或「当前没有气泡(kind=0)」时才连带收合并气泡。
            // 否则建议气泡（kind 1/3/4/5/6）自己消失时会把常驻的通知气泡一起收掉
            // （实测：剪贴板建议气泡 3s 后超时 → 通知气泡跟着消失）。
            int k = com.oupao.wiz.Sug.kind;
            if ((k == 2) || (k == 0)) {
                com.oupao.wiz.Bub.hide();
            }
        } catch (Throwable ignore) {
        }
        // 原来这里每次都 new Throwable("dismiss") 只为打调用栈，且用 error 级 ——
        // 既是临时调试残留，也有实打实的堆栈捕获开销。改成 verbose（开 oupao_dbg 才输出）。
        if (com.oupao.wiz.SLog.dbgOn()) {
            com.oupao.wiz.SLog.v("Sug: dismiss 调用栈", new Throwable("dismiss"));
        }

        // 4.0-fix：不再只删 `Sug.view`，把登记过的窗口全部强删。
        //   （原来若 removeView 抛异常 / 引用被覆盖，窗口就永远留在屏幕上，点它只有 fire kind=0。）
        try {
            com.oupao.wiz.Sug.removeAllWinViews();
        } catch (Throwable ignore) {
        }
        com.oupao.wiz.Sug.cancelTimer();
        com.oupao.wiz.Sug.view = null;
        com.oupao.wiz.Sug.shown = false;
        com.oupao.wiz.Sug.kind = 0;
        return;
    }
    private static android.graphics.Bitmap drawTorchIcon(int p9)
    {
        // 3319：重绘手电筒图标（原来的灰底白图标没颜色、偏大、不居中）。
        //   暖黄→橙渐变圆底 + 居中白色手电筒（灯头梯形 + 圆角灯身 + 暖黄镜片/防滑纹）。
        android.graphics.Bitmap cached = com.oupao.wiz.Sug.ticon;
        if (cached != null) {
            return cached;
        }
        int s = p9;
        if (s < 24) {
            s = 24;
        }
        android.graphics.Bitmap bmp =
                android.graphics.Bitmap.createBitmap(s, s, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas c = new android.graphics.Canvas(bmp);
        float cx = s * 0.5f;
        float cy = s * 0.5f;

        // 圆底：暖黄 → 橙
        android.graphics.Paint bg = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        bg.setShader(new android.graphics.LinearGradient(
                0f, 0f, 0f, (float) s,
                0xFFFFCA28, 0xFFFB8C00, android.graphics.Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, s * 0.5f, bg);

        float headW = s * 0.34f;          // 灯头最宽
        float bodyW = s * 0.20f;          // 灯身宽
        float top = cy - s * 0.25f;       // 图标上边
        float neck = top + s * 0.10f;     // 灯头下沿
        float bottom = cy + s * 0.25f;    // 图标下边

        android.graphics.Paint white = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        white.setColor(0xFFFFFFFF);

        // 灯头（上宽下窄）
        android.graphics.Path head = new android.graphics.Path();
        head.moveTo(cx - headW * 0.5f, top);
        head.lineTo(cx + headW * 0.5f, top);
        head.lineTo(cx + bodyW * 0.5f, neck);
        head.lineTo(cx - bodyW * 0.5f, neck);
        head.close();
        c.drawPath(head, white);

        // 灯身（圆角矩形）
        android.graphics.RectF body = new android.graphics.RectF(
                cx - bodyW * 0.5f, neck, cx + bodyW * 0.5f, bottom);
        c.drawRoundRect(body, bodyW * 0.22f, bodyW * 0.22f, white);

        // 镜片（暖黄）
        android.graphics.Paint lens = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        lens.setColor(0xFFFFB300);
        c.drawRect(cx - headW * 0.32f, top + s * 0.030f,
                cx + headW * 0.32f, top + s * 0.065f, lens);

        // 灯身防滑纹（两道暖黄细线）
        android.graphics.Paint grip = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        grip.setColor(0xFFFFB300);
        grip.setStrokeWidth(Math.max(1f, s * 0.022f));
        c.drawLine(cx - bodyW * 0.30f, neck + s * 0.09f,
                cx + bodyW * 0.30f, neck + s * 0.09f, grip);
        c.drawLine(cx - bodyW * 0.30f, neck + s * 0.14f,
                cx + bodyW * 0.30f, neck + s * 0.14f, grip);

        com.oupao.wiz.Sug.ticon = bmp;
        return bmp;
    }
    /** 1820fix: 把任意 Drawable 画成 size×size 的圆形位图（绕开 scan 包围盒，保证圆形与尺寸一致）。 */
    /**
     * 1820fix: 自算包围盒 → 内容放大到填满圆（×1.08）→ 裁圆。
     * 不依赖 scan()/iconRound()（其包围盒逻辑在本移植版里不可靠，会产出全透明位图）。
     */
    public static android.graphics.Bitmap circleFitIcon(android.graphics.drawable.Drawable d, int size)
    {
        try {
            if ((d == null) || (size <= 0)) {
                return null;
            }
            android.graphics.Bitmap raw = android.graphics.Bitmap.createBitmap(size, size,
                    android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas c0 = new android.graphics.Canvas(raw);
            d.setBounds(0, 0, size, size);
            d.draw(c0);

            // 自算不透明包围盒（>>> 无符号右移）
            // 性能：一次性取像素（避免 size² 次 getPixel 调用），并按 2px 步进采样
            int x1 = size, y1 = size, x2 = -1, y2 = -1;
            int[] px = new int[size * size];
            raw.getPixels(px, 0, size, 0, 0, size, size);
            int step = (size > 128) ? 2 : 1;
            for (int y = 0; y < size; y += step) {
                int row = y * size;
                for (int x = 0; x < size; x += step) {
                    if (((px[row + x] >>> 24) > 32)) {
                        if (x < x1) {
                            x1 = x;
                        }
                        if (x > x2) {
                            x2 = x;
                        }
                        if (y < y1) {
                            y1 = y;
                        }
                        if (y > y2) {
                            y2 = y;
                        }
                    }
                }
            }
            if ((x2 < 0) || (y2 < 0)) {
                // verbose：全透明图标属于正常情况（很多自适应图标的外角就是透明的），
                // 每个缓存未命中都会走到，不该用 info 级刷屏。
                com.oupao.wiz.SLog.v("circleFitIcon: 全透明 size=" + size);
                return null;
            }
            int bw = x2 - x1 + 1;
            int bh = y2 - y1 + 1;
            float scale = ((((float) size) * 1.08f) / ((float) Math.max(bw, bh)));
            float dx = ((((float) size) / 2.0f) - ((((float) (x1 + x2)) / 2.0f) * scale));
            float dy = ((((float) size) / 2.0f) - ((((float) (y1 + y2)) / 2.0f) * scale));

            android.graphics.Bitmap out = android.graphics.Bitmap.createBitmap(size, size,
                    android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas c = new android.graphics.Canvas(out);
            android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            android.graphics.BitmapShader sh = new android.graphics.BitmapShader(raw,
                    android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP);
            android.graphics.Matrix m = new android.graphics.Matrix();
            m.setScale(scale, scale);
            m.postTranslate(dx, dy);
            sh.setLocalMatrix(m);
            p.setShader(sh);
            float r = (((float) size) / 2.0f);
            c.drawCircle(r, r, r, p);
            if (opaque(out) < 4) {
                return null;
            }
            // verbose：只在开 oupao_dbg 时输出。这个方法每次图标缓存未命中都会跑
            // （面板刷新、字母索引预取、角标兜底），info 级会把正常日志冲掉。
            com.oupao.wiz.SLog.v("circleFitIcon: ok size=" + size + " bb=" + bw + "x" + bh
                    + " scale=" + scale);
            return out;
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("circleFitIcon 异常", t);
            return null;
        }
    }

    public static android.graphics.Bitmap drawableToBitmap(android.graphics.drawable.Drawable p4, int p5)
    {
        try {
            var v0_1 = android.graphics.Bitmap.createBitmap(p5, p5, android.graphics.Bitmap.Config.ARGB_8888);
            var v1_1 = new android.graphics.Canvas(v0_1);
            p4.setBounds(0, 0, p5, p5);
            p4.draw(v1_1);
            return v0_1;
        } catch (Throwable t) {
            return null;
        }
    }
    public static void dumpNotif(android.app.Notification p6)
    {
        android.app.PendingIntent v0_2 = null;
        try {
            com.oupao.wiz.Sug.pi = null;
            com.oupao.wiz.Sug.ni = null;
            com.oupao.wiz.Sug.qqUin = null;
            com.oupao.wiz.Sug.qqType = null;
            v0_2 = p6.contentIntent;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_2 != null) {
            com.oupao.wiz.Sug.pi = v0_2;
            var v2 = android.os.Binder.clearCallingIdentity();
            android.content.Intent v0_0 = null;
            try {
                v0_0 = com.oupao.wiz.FrameworkExt.pendingIntentGetIntent(v0_2);
                android.os.Binder.restoreCallingIdentity(v2);
            } catch (Throwable __e_v5) {
                android.os.Binder.restoreCallingIdentity(v2);
                throw __e_v5;
            }
            if (v0_0 != null) {
                com.oupao.wiz.Sug.ni = v0_0;
                com.oupao.wiz.Sug.pi = p6.contentIntent;
                var v4_2 = v0_0.getExtras();
                if (v4_2 != null) {
                    var v4_3 = v4_2.get("key_peerUin");
                    if (v4_3 != null) {
                        com.oupao.wiz.Sug.qqUin = v4_3.toString();
                    }
                    var v4_5 = v0_0.getExtras();
                    if (v4_5 != null) {
                        var v4_6 = v4_5.get("key_chat_type");
                        if (v4_6 != null) {
                            com.oupao.wiz.Sug.qqType = v4_6.toString();
                        }
                    }
                }
                com.oupao.wiz.Sug.chatUser = v0_0.getStringExtra("Chat_User");
            }
        }
        return;
    }
    public static String extUrl(String p5)
    {
        int v0_3 = 0;
        try {
            v0_3 = p5.indexOf("http");
        } catch (Throwable __e_v4) {
            return null;
        }
        if (v0_3 >= 0) {
            var v1 = p5.length();
            var v2 = v0_3;
            while ((v2 < v1) && (!Character.isWhitespace(p5.charAt(v2)))) {
                v2++;
            }
            return p5.substring(v0_3, v2);
        } else {
            return null;
        }
    }
    public static boolean fgApp()
    {
        java.lang.String v0_2 = null;
        try {
            v0_2 = com.oupao.wiz.Sug.fgPkg();
        } catch (Throwable __e_v1) {
            return false;
        }
        if ((v0_2 == null) || ((v0_2.equals("com.oupao.wiz")) || ((v0_2.equals("com.android.systemui")) || (com.oupao.wiz.Sug.fgHome())))) {
            return false;
        } else {
            return true;
        }
    }
    public static boolean fgFull()
    {
        int v0_0 = 0;
        java.lang.Object v1_1 = null;
        try {
            v1_1 = com.oupao.wiz.Rt.invoke(com.oupao.wiz.Rt.methodByName(Class.forName("android.app.ActivityTaskManager"), "getService", 0), null, null);
        } catch (Throwable __e_v1) {
            return false;
        }
        if (v1_1 != null) {
            var v1_2 = com.oupao.wiz.Rt.callNoArgQ(v1_1, "getFocusedRootTaskInfo");
            if (v1_2 != null) {
                var v1_3 = com.oupao.wiz.Rt.get(v1_2, "configuration");
                if (v1_3 != null) {
                    var v1_4 = com.oupao.wiz.Rt.get(v1_3, "windowConfiguration");
                    if (v1_4 != null) {
                        var v1_5 = com.oupao.wiz.Rt.callNoArg(v1_4, "getWindowingMode");
                        if (((v1_5 instanceof Integer)) && (((Integer) v1_5).intValue() == 1)) {
                            v0_0 = 1;
                        }
                    }
                }
            }
        }
        return v0_0 != 0;
    }
    public static boolean fgHome()
    {
        int v0_0 = 0;
        java.lang.String v1_0 = null;
        try {
            v1_0 = com.oupao.wiz.Sug.fgPkg();
        } catch (Throwable __e_v1) {
            return false;
        }
        if (v1_0 != null) {
            var v2_5 = com.oupao.wiz.Cfg.sysContext();
            if (v2_5 != null) {
                var v3_2 = new android.content.Intent("android.intent.action.MAIN");
                v3_2.addCategory("android.intent.category.HOME");
                var v2_1 = v2_5.getPackageManager().resolveActivity(v3_2, 65536);
                if (v2_1 != null) {
                    var v2_2 = v2_1.activityInfo;
                    if (v2_2 != null) {
                        var v2_3 = v2_2.packageName;
                        if ((v2_3 != null) && (v1_0.equals(v2_3))) {
                            v0_0 = 1;
                            return v0_0 != 0;
                        }
                    }
                }
            }
            if (v1_0.contains("launcher")) {
                v0_0 = 1;
            }
        }
        return v0_0 != 0;
    }
    /**
     * 3195：当前是否在**多任务（概览）**界面。
     *
     * 为什么需要：进多任务时桌面会被 resume 成 focused task
     * （`wm_focused_root_task … startExistingRecents` + `wm_set_resumed_activity com.android.launcher`），
     * 于是 `fgHome()` 为真 → "在多任务里点气泡"被当成"在桌面"→ 开成全屏。
     * 这个标志由 `FlyGate` 在桌面每次状态回调/触摸时写进 Settings.Global。
     */
    public static boolean recentsOpen()
    {
        try {
            var v0 = com.oupao.wiz.Cfg.sysContext();
            var v1 = com.oupao.wiz.Cfg.ggetC(v0, "oupao_recents_visible");
            return "1".equals(v1);
        } catch (Throwable __e) {
            return false;
        }
    }

    /**
     * 3197：**输入焦点窗口**的包名（WMS.mFocusedWindow.mAttrs.packageName）。
     *
     * 为什么不用 `fgPkg()`：它读 `ActivityTaskManager.getFocusedRootTaskInfo().topActivity`，
     * 那是 root task 的快照 —— 进概览/回桌面转场时桌面 task 会被 resume，而画面还停在应用上，
     * 于是"人在应用里"被误判成"在桌面"→ 点气泡被开成全屏。
     * `mFocusedWindow` 是此刻真正接收触摸的那个窗口，最贴近"用户看到的是什么"。
     */
    public static String fgPkgWin()
    {
        try {
            var v0 = com.oupao.wiz.Rt.get(com.oupao.wiz.Hooks.wms(), "mFocusedWindow");
            if (v0 == null) {
                return null;
            }
            var v1 = com.oupao.wiz.Rt.get(v0, "mAttrs");
            var v2 = com.oupao.wiz.Rt.get(v1, "packageName");
            if ((v2 instanceof String) && (((String) v2).length() > 0)) {
                return ((String) v2);
            }
        } catch (Throwable __e) {
        }
        return null;
    }

    /**
     * 3436：**当前焦点窗口的标题**（`mCurrentFocus.mAttrs.getTitle()`）。
     *
     * 为什么需要：扇形菜单"停留中自动收起"原来只看两个**标志位**
     * （多任务的 `oupao_recents_visible`、通知中心/控制中心的 `oupao_shade_open`），
     * 两个都是"别的进程写、system_server 读"，任一侧漏写就永远收不起来
     * （用户："连续性测试很大概率不会关闭菜单"）。
     * 这里直接从 WMS 读**此刻真正拿到焦点的窗口**：
     *   通知中心/控制中心 = `NotificationShade`（实测 `mCurrentFocus=Window{… NotificationShade}`），
     * 是"用户看到什么"的一手信息，不依赖任何标志位。
     */
    public static String fgWinTitle()
    {
        try {
            var w = focusedWin();
            if (w == null) {
                return null;
            }
            var attrs = com.oupao.wiz.Rt.get(w, "mAttrs");
            if (attrs == null) {
                return null;
            }
            // 字段优先（`WindowManager.LayoutParams.title` 是 public field），再退回 getTitle()
            var t = com.oupao.wiz.Rt.get(attrs, "title");
            if (!(t instanceof CharSequence)) {
                t = com.oupao.wiz.Rt.callNoArgQ(attrs, "getTitle");
            }
            if (t instanceof CharSequence) {
                return t.toString();
            }
        } catch (Throwable __e) {
        }
        return null;
    }

    /**
     * 3441：取"此刻拿到焦点的窗口"（WindowState），多路径兜底。
     *
     * 为什么不能只读 `wms.mCurrentFocus`：Android 13+ 这两个字段已经挪到
     * **DisplayContent** 上了（实测系统是 `OplusWindowManagerService`，
     * `wms.mCurrentFocus` / `wms.mFocusedWindow` 都是 null），
     * 要走 `mRoot.getTopFocusedDisplayContent().mCurrentFocus`。
     */
    public static Object focusedWin()
    {
        var wms = com.oupao.wiz.FlyH.wms;
        if (wms == null) {
            wms = com.oupao.wiz.Hooks.wms();
        }
        if (wms == null) {
            return null;
        }
        var w = com.oupao.wiz.Rt.get(wms, "mCurrentFocus");
        if (w == null) {
            w = com.oupao.wiz.Rt.get(wms, "mFocusedWindow");
        }
        if (w != null) {
            return w;
        }
        var root = com.oupao.wiz.Rt.get(wms, "mRoot");
        var dc = com.oupao.wiz.Rt.callNoArgQ(root, "getTopFocusedDisplayContent");
        if (dc == null) {
            dc = com.oupao.wiz.Rt.get(root, "mTopFocusedDisplayContent");
        }
        if (dc == null) {
            dc = com.oupao.wiz.Rt.get(root, "mFocusedDisplayContent");
        }
        w = com.oupao.wiz.Rt.get(dc, "mCurrentFocus");
        if (w == null) {
            w = com.oupao.wiz.Rt.get(dc, "mFocusedWindow");
        }
        return w;
    }

        /** 3197：输入焦点窗口所属用户（0=主应用 / 999=分身）。拿不到返回 -1 */
    public static int fgUserWin()
    {
        try {
            var v0 = com.oupao.wiz.Rt.get(com.oupao.wiz.Hooks.wms(), "mFocusedWindow");
            if (v0 == null) {
                return -1;
            }
            var v1 = com.oupao.wiz.Rt.get(v0, "mSession");
            var v2 = com.oupao.wiz.Rt.get(v1, "mUid");
            if (v2 instanceof Number) {
                return (((Number) v2).intValue() / 100000);
            }
        } catch (Throwable __e) {
        }
        return -1;
    }

    /** 桌面（HOME）的包名；拿不到返回 null */
    public static String homePkg()
    {
        try {
            var c = com.oupao.wiz.Cfg.sysContext();
            if (c == null) {
                return null;
            }
            var i = new android.content.Intent("android.intent.action.MAIN");
            i.addCategory("android.intent.category.HOME");
            var ri = c.getPackageManager().resolveActivity(i, 65536);
            if ((ri != null) && (ri.activityInfo != null)) {
                return ri.activityInfo.packageName;
            }
        } catch (Throwable __e) {
        }
        return null;
    }

    /**
     * 3197：此刻是否真的"停在桌面"上（不是概览、不是转场里还在显示应用）。
     * 以输入焦点窗口为准，拿不到才退回 root task 那套。
     */
    public static boolean onDesktop()
    {
        try {
            String p = com.oupao.wiz.Sug.fgPkgWin();
            // 焦点落在我自己的气泡/系统窗口上时，它不代表"用户看到的是什么" → 退回 root task
            if ((p != null) && (p.equals("android") || p.equals("com.oupao.wiz")
                    || p.equals("com.android.systemui"))) {
                p = null;
            }
            if (p == null) {
                p = com.oupao.wiz.Sug.fgPkg();
            }
            if (p == null) {
                return false;
            }
            String hp = com.oupao.wiz.Sug.homePkg();
            if (hp != null) {
                return p.equals(hp);
            }
            return p.contains("launcher");
        } catch (Throwable __e) {
            return false;
        }
    }

    /**
     * 3198：桌面判定要"稳"——**最近 1.2 秒内还看到过非桌面状态就不算在桌面**。
     *
     * 为什么：容器切换（进概览/回桌面/应用退到后台）时，桌面 activity 会先被 resume、
     * 画面还在应用上，用户看到的是"我还在应用里"，而我们的判据已经是"桌面"了。
     * 只有"连续一段时间都确实停在桌面"才走全屏；其余一律小窗。
     */
    public static long lastHomeObs = 0L;
    public static long lastAppObs = 0L;

    /** 采样一次"此刻是不是桌面"（由 fgWatch 每 700ms 调一次 + 点气泡时调一次） */
    public static void sampleFg()
    {
        try {
            long now = android.os.SystemClock.uptimeMillis();
            if (com.oupao.wiz.Sug.onDesktop()) {
                com.oupao.wiz.Sug.lastHomeObs = now;
            } else {
                com.oupao.wiz.Sug.lastAppObs = now;
            }
        } catch (Throwable __e) {
        }
    }

    /** 是否"稳稳地停在桌面"（桌面 + 最近 1.2s 内没有出现过非桌面状态） */
    public static boolean desktopSettled()
    {
        try {
            if (!com.oupao.wiz.Sug.onDesktop()) {
                return false;
            }
            long now = android.os.SystemClock.uptimeMillis();
            long la = com.oupao.wiz.Sug.lastAppObs;
            return (la == 0L) || ((now - la) >= 1200L);
        } catch (Throwable __e) {
            return false;
        }
    }

    /**
     * 4.0-fix：**前台是否还挂着一个可见的全屏应用**（点气泡要不要全屏的判定用）。
     *
     * 为什么需要：3194~3198 那套只看 root task 的 topActivity 和输入焦点窗口，
     * 有两种情况都会读到 launcher 而画面上其实还是应用：
     *   ① 输入焦点落在我自己的气泡窗口（type=2003）上 → `onDesktop()` 回退到 root task 快照；
     *   ② 刚关掉小窗 / 容器切换的转场里，launcher 会先被 resume。
     * 用户 2026-11-11 16:01:20 复现：明明还在全屏应用里，点气泡却开成全屏
     * （日志 `FireT fg=com.android.launcher home=true full=true win=false`）。
     *
     * 这里绕开快照，直接枚举任务：只要有一个「可见 + 全屏 + 不是桌面/本模块/SystemUI」的任务，
     * 就说明前台还有全屏应用 → 不走全屏支路。
     *
     * @return 命中的包名；没有则 null
     */
    public static String fgFullAppPkg()
    {
        try {
            var list = com.oupao.wiz.Sug.taskInfos();
            if (list == null) {
                return null;
            }
            for (Object t : list) {
                if (!com.oupao.wiz.Sug.taskVisible(t)) {
                    continue;
                }
                if (com.oupao.wiz.Sug.taskWindowingMode(t) != 1) {   // 1 = WINDOWING_MODE_FULLSCREEN
                    continue;
                }
                String pkg = com.oupao.wiz.Sug.taskPkg(t);
                if (pkg == null) {
                    continue;
                }
                if (pkg.equals("com.oupao.wiz") || pkg.equals("com.android.systemui")) {
                    continue;
                }
                if (com.oupao.wiz.Sug.isHomePkg(pkg)) {
                    continue;
                }
                return pkg;
            }
        } catch (Throwable __e) {
        }
        return null;
    }

    /** 333x：任务列表（RunningTaskInfo），按"最近"排序；拿不到返回 null */
    static java.util.List<?> taskInfos()
    {
        try {
            var svc = com.oupao.wiz.Rt.invoke(
                    com.oupao.wiz.Rt.methodByName(Class.forName("android.app.ActivityTaskManager"), "getService", 0),
                    null, null);
            if (svc == null) {
                return null;
            }
            var list = com.oupao.wiz.Rt.callLoose(svc, "getTasks", new Object[]{Integer.valueOf(30)});
            if (list instanceof java.util.List) {
                return ((java.util.List<?>) list);
            }
        } catch (Throwable __e) {
        }
        return null;
    }

    static boolean taskVisible(Object t)
    {
        try {
            var v = com.oupao.wiz.Rt.get(t, "isVisible");
            return (v instanceof Boolean) && ((Boolean) v).booleanValue();
        } catch (Throwable __e) {
            return false;
        }
    }

    /** RunningTaskInfo.windowingMode；字段缺失时退回 configuration；都拿不到返回 -1 */
    static int taskWindowingMode(Object t)
    {
        try {
            var mode = com.oupao.wiz.Rt.get(t, "windowingMode");
            if (mode instanceof Number) {
                return ((Number) mode).intValue();
            }
            var cfg = com.oupao.wiz.Rt.get(t, "configuration");
            var wc = com.oupao.wiz.Rt.get(cfg, "windowConfiguration");
            var m2 = com.oupao.wiz.Rt.callNoArg(wc, "getWindowingMode");
            if (m2 instanceof Number) {
                return ((Number) m2).intValue();
            }
        } catch (Throwable __e) {
        }
        return -1;
    }

    /** RunningTaskInfo 的顶层包名 */
    static String taskPkg(Object t)
    {
        try {
            var top = com.oupao.wiz.Rt.get(t, "topActivity");
            if (top instanceof android.content.ComponentName) {
                return ((android.content.ComponentName) top).getPackageName();
            }
            if (top != null) {
                var p = com.oupao.wiz.Rt.callNoArg(top, "getPackageName");
                if (p instanceof String) {
                    return ((String) p);
                }
            }
        } catch (Throwable __e) {
        }
        return null;
    }

    /** 333x：任务所属用户（0=主应用 / 999=分身）；找不到返回 -1 */
    public static int fgTaskUser(String pkg)
    {
        try {
            var list = com.oupao.wiz.Sug.taskInfos();
            if ((list == null) || (pkg == null)) {
                return -1;
            }
            for (Object t : list) {
                if (!com.oupao.wiz.Sug.taskVisible(t)) {
                    continue;
                }
                if (!pkg.equals(com.oupao.wiz.Sug.taskPkg(t))) {
                    continue;
                }
                var u = com.oupao.wiz.Rt.get(t, "userId");
                if (u instanceof Number) {
                    return ((Number) u).intValue();
                }
            }
        } catch (Throwable __e) {
        }
        return -1;
    }

    /** 是不是桌面（HOME）包名 */
    static boolean isHomePkg(String p)
    {
        if (p == null) {
            return false;
        }
        String hp = com.oupao.wiz.Sug.homePkg();
        if ((hp != null) && p.equals(hp)) {
            return true;
        }
        return p.contains("launcher");
    }

    public static void fgLog(String p3)
    {
        try {
            var v0_1 = new StringBuilder();
            v0_1.append(p3);
            v0_1.append(" fg=");
            v0_1.append(com.oupao.wiz.Sug.fgPkg());
            v0_1.append(" bgFull=");
            v0_1.append(com.oupao.wiz.Sug.fgFullAppPkg());
            v0_1.append(" home=");
            v0_1.append(com.oupao.wiz.Sug.fgHome());
            v0_1.append(" full=");
            v0_1.append(com.oupao.wiz.Sug.fgFull());
            v0_1.append(" win=");
            v0_1.append(com.oupao.wiz.Hooks.winOpen());
            v0_1.append(" fu=");
            v0_1.append(com.oupao.wiz.Hooks.focusedUserId());
            v0_1.append(" nu=");
            v0_1.append(com.oupao.wiz.Sug.notifUserId());
            v0_1.append(" bp=");
            v0_1.append(com.oupao.wiz.Sug.pkg);
            com.oupao.wiz.SLog.i(v0_1.toString());
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    /**
     * 333x：后台普通 App 的 `getFocusedRootTaskInfo()` 会**报桌面**。
     *
     * 实测（2026-11-11 16:11:50，分身微信在聊天页）：
     * `SugT fg=com.android.launcher bgFull=com.tencent.mm home=true win=false fu=0 nu=999`
     * —— root task 快照是 launcher，但任务列表里那个分身微信是可见全屏。
     * 后果有两处：① 进了聊天页气泡不收；② 点气泡被当成"在桌面"开成全屏。
     *
     * 所以前台包名的取值顺序改成：
     *   ① 输入焦点窗口（最贴近"用户看到的是什么"，气泡自己的窗口不算）；
     *   ② root task 快照（不是桌面就直接用）；
     *   ③ 任务列表里最靠前的「可见全屏、非桌面/本模块/SystemUI」任务（分身场景兜底）。
     */
    public static String fgPkg()
    {
        try {
            String win = com.oupao.wiz.Sug.fgPkgWin();
            if ((win != null) && (!win.equals("android")) && (!win.equals("com.oupao.wiz"))
                    && (!win.equals("com.android.systemui"))) {
                return win;
            }
        } catch (Throwable ignore) {
        }
        String root = com.oupao.wiz.Sug.fgPkgRoot();
        if ((root != null) && (!com.oupao.wiz.Sug.isHomePkg(root))) {
            return root;
        }
        String full = com.oupao.wiz.Sug.fgFullAppPkg();
        return (full != null) ? full : root;
    }

    /** root task 快照（3197 之前的实现，保留给 c) 兜底与对照用） */
    public static String fgPkgRoot()
    {
        java.lang.Object v1_1 = null;
        java.lang.String v0_0 = null;
        try {
            v1_1 = com.oupao.wiz.Rt.invoke(com.oupao.wiz.Rt.methodByName(Class.forName("android.app.ActivityTaskManager"), "getService", 0), null, null);
        } catch (Throwable __e___ex) {
            return null;
        }
        if (v1_1 != null) {
            var v1_2 = com.oupao.wiz.Rt.callNoArgQ(v1_1, "getFocusedRootTaskInfo");
            if (v1_2 != null) {
                var v1_3 = com.oupao.wiz.Rt.get(v1_2, "topActivity");
                if ((v1_3 instanceof android.content.ComponentName)) {
                    v0_0 = ((android.content.ComponentName) v1_3).getPackageName();
                }
            }
        }
        return v0_0;
    }
    /** 最近一次 show() 的时间戳（用于通知取消钩子防误杀） */
    public static long showStamp = 0L;

    /** 前台监听（进入对应应用/聊天页就收气泡），null 表示没在跑 */
    public static Runnable fgWatch = null;

    /**
     * 有气泡时每 700ms 检查一次前台：进了「气泡对应的应用」或「该应用的聊天页」就收掉气泡
     * （和系统通知一致）。合并气泡与分离气泡都覆盖。
     */
    public static void startFgWatch()
    {
        try {
            if (com.oupao.wiz.Sug.fgWatch != null) {
                return;
            }
            final Runnable r = new Runnable() {
                public void run()
                {
                    try {
                        // 3198：顺手采一次"此刻是不是桌面"，给"桌面要稳 1.2s"的判定用
                        com.oupao.wiz.Sug.sampleFg();
                        if ((!com.oupao.wiz.Sug.shown) && (!com.oupao.wiz.Bub.mergedShown())) {
                            com.oupao.wiz.Sug.fgWatch = null;
                            return;
                        }
                        String fg = com.oupao.wiz.Sug.fgPkg();
                        String pkg = com.oupao.wiz.Sug.pkg;
                        // 3192：同一包名的主应用/分身是**两个不同的人**，收气泡必须连用户一起比，
                        // 否则「主微信在前台」会把分身微信的气泡收掉（用户实测过两个方向）。
                        // 333x：用户也按"那个可见任务自己的 userId"取，焦点在桌面时 focusedUserId 会报 0，
                        //   分身的气泡就永远收不掉（实测：在分身微信聊天页里气泡照挂）。
                        int fu = com.oupao.wiz.Sug.fgTaskUser(fg);
                        if (fu < 0) {
                            fu = com.oupao.wiz.Hooks.focusedUserId();
                        }
                        if ((fg != null) && (pkg != null) && fg.equals(pkg)
                                && (fu == com.oupao.wiz.Sug.bubbleUser)) {
                            String cls = com.oupao.wiz.Sug.fgActivityClass();
                            com.oupao.wiz.SLog.i("Sug: 前台 " + fg + " cls=" + cls
                                    + " chatPage=" + com.oupao.wiz.Sug.isChatPage(cls)
                                    + " sameChat=" + com.oupao.wiz.Sug.sameChatId(fg)
                                    + " curChat=" + com.oupao.wiz.Sug.curChat
                                    + " bubbleChat=" + com.oupao.wiz.Sug.chatUser);
                            // 333x：微信也改成「在前台（同用户）就收」，和 QQ 一致。
                            //   原来微信要「进到聊天页」才收（chatPage/sameChat），但实测微信聊天页
                            //   经常还是 LauncherUI + Fragment（类名判不出来），而 curChat 恒为 null
                            //   （sameChat 也恒 false）→ 从桌面点进聊天页气泡永远不收。
                            //   用户 2026-11-11 反馈：「QQ 没问题，微信进聊天页也不收」。
                            //   既然应用已经在前台，气泡的作用（提醒去看）已经完成，直接收掉。
                            com.oupao.wiz.SLog.i("Sug: 已进入 " + fg + "(" + cls + ") -> 收气泡");
                            com.oupao.wiz.Sug.fgWatch = null;
                            com.oupao.wiz.Sug.dismiss();
                            return;
                        }
                        if ((fg != null) && (pkg != null) && fg.equals(pkg)) {
                            com.oupao.wiz.SLog.i("Sug: 前台同包但用户不符 fg=" + fg + " fu=" + fu
                                    + " bu=" + com.oupao.wiz.Sug.bubbleUser + " -> 气泡保留");
                        }
                    } catch (Throwable ignore) {
                    }
                    try {
                        com.oupao.wiz.Sug.main.postDelayed(this, 700);
                    } catch (Throwable ignore) {
                    }
                }
            };
            com.oupao.wiz.Sug.fgWatch = r;
            com.oupao.wiz.Sug.main.postDelayed(r, 700);
        } catch (Throwable ignore) {
        }
    }

    /** 当前前台 Activity 的类名（用于判断「是不是进了聊天页」） */
    public static String fgActivityClass()
    {
        // 333x：先按"前台包名"去任务列表里找它自己的顶层 Activity。
        //   原来只读 root task，分身/转场里 root task 是 launcher → 类名也变成桌面的，
        //   "进了微信聊天页"判不出来 → 气泡一直不收。
        try {
            String fg = com.oupao.wiz.Sug.fgPkg();
            var list = com.oupao.wiz.Sug.taskInfos();
            if ((fg != null) && (list != null)) {
                for (Object t : list) {
                    if (!com.oupao.wiz.Sug.taskVisible(t)) {
                        continue;
                    }
                    if (!fg.equals(com.oupao.wiz.Sug.taskPkg(t))) {
                        continue;
                    }
                    var top = com.oupao.wiz.Rt.get(t, "topActivity");
                    if (top instanceof android.content.ComponentName) {
                        return ((android.content.ComponentName) top).getClassName();
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        try {
            Object atms = com.oupao.wiz.Rt.invoke(
                    com.oupao.wiz.Rt.methodByName(Class.forName("android.app.ActivityTaskManager"), "getService", 0),
                    null, null);
            if (atms != null) {
                Object info = com.oupao.wiz.Rt.callNoArgQ(atms, "getFocusedRootTaskInfo");
                if (info != null) {
                    Object top = com.oupao.wiz.Rt.get(info, "topActivity");
                    if (top instanceof android.content.ComponentName) {
                        return ((android.content.ComponentName) top).getClassName();
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    /** 短信小窗启动后 1s 校验：startZoomWindow 没拉起来才全屏兜底（避免 1.8s 那么慢）。 */
    private static void ensureMmsOpened(final String pkg)
    {
        try {
            var h = com.oupao.wiz.Hooks.main();
            if (h == null) return;
            h.postDelayed(new java.lang.Runnable() {
                public void run() {
                    try {
                        var fg = com.oupao.wiz.Sug.fgPkg();
                        if ((fg != null) && fg.startsWith(pkg)) {
                            com.oupao.wiz.SLog.i("Fire: 短信小窗已拉起 " + pkg + " fg=" + fg);
                            return;
                        }
                        com.oupao.wiz.SLog.i("Fire: 短信小窗未拉起，全屏兜底 pkg=" + pkg + " fg=" + fg);
                        if (!com.oupao.wiz.Sug.launchFull(pkg)) {
                            com.oupao.wiz.Sug.launch(pkg);
                        }
                    } catch (Throwable t) {
                        com.oupao.wiz.SLog.e("Fire: 短信兜底异常", t);
                    }
                }
            }, 1000L);
        } catch (Throwable ignore) {
        }
    }

    /** 系统短信/信息应用：用 contentIntent 的 Intent 走 startZoomWindow。 */
    private static boolean isSystemMms(String p)
    {
        if (p == null) {
            return false;
        }
        return "com.android.mms".equals(p)
                || "com.oplus.mms".equals(p)
                || "com.android.messaging".equals(p)
                || "com.google.android.apps.messaging".equals(p);
    }

    /**
     * 通用通知的 contentIntent 已经 send() 成功时，延迟检查目标包是否真的到了前台。
     * 没到就用普通 launch 兜底（短信等系统应用的 PendingIntent 常见“send 成功但不拉起”）。
     */
    private static void ensureOpenedByPackage(final String pkg)
    {
        try {
            var h = com.oupao.wiz.Hooks.main();
            if (h == null) {
                return;
            }
            // 3326：原来是固定 1800ms 才校验一次 → PendingIntent 没拉起时用户要干等 1.8 秒。
            //   改成 200ms 轮询、最多 3 次（600ms）：真拉起来了立刻收工，没拉起也早 1.2 秒兜底。
            h.postDelayed(new java.lang.Runnable() {
                int tries = 0;
                public void run() {
                    try {
                        var fg = com.oupao.wiz.Sug.fgPkg();
                        if ((fg != null) && fg.startsWith(pkg)) {
                            com.oupao.wiz.SLog.i("Fire: contentIntent 已拉起 " + pkg + " fg=" + fg);
                            return;
                        }
                        tries++;
                        if (tries < 3) {
                            var hh = com.oupao.wiz.Hooks.main();
                            if (hh != null) {
                                hh.postDelayed(this, 200L);
                                return;
                            }
                        }
                        com.oupao.wiz.SLog.i("Fire: contentIntent 未拉起 pkg=" + pkg
                                + " fg=" + fg + " -> 普通 launch 兜底");
                        com.oupao.wiz.Sug.launch(pkg);
                    } catch (Throwable t) {
                        com.oupao.wiz.SLog.e("Fire: 校验 contentIntent 异常", t);
                    }
                }
            }, 200L);
        } catch (Throwable ignore) {
        }
    }

    public static void fire()
    {
        try {
            com.oupao.wiz.SLog.i("Sug: fire kind=" + com.oupao.wiz.Sug.kind
                    + " pkg=" + com.oupao.wiz.Sug.pkg
                    + " ctx=" + ((com.oupao.wiz.Sug.ctx != null) ? "ok" : "null")
                    + " cam=" + ((com.oupao.wiz.Sug.cam != null) ? "ok" : "null"));
        } catch (Throwable ignore) {
        }
        var v0_0 = com.oupao.wiz.Sug.kPkg;
        if (v0_0 != null) {
            com.oupao.wiz.Sug.pkg = v0_0;
        }
        com.oupao.wiz.Sug.chatUser = com.oupao.wiz.Sug.kChat;
        com.oupao.wiz.Sug.qqUin = com.oupao.wiz.Sug.kUin;
        com.oupao.wiz.Sug.qqType = com.oupao.wiz.Sug.kType;
        com.oupao.wiz.Sug.ni = com.oupao.wiz.Sug.kNi;
        com.oupao.wiz.Sug.pi = com.oupao.wiz.Sug.kPi;
        com.oupao.wiz.Sug.lurl = null;
        com.oupao.wiz.Sug.useBubUid();
        try {
            if ((com.oupao.wiz.Cfg.vibMaster()) && (com.oupao.wiz.Cfg.vibOn())) {
                com.oupao.wiz.Sug.vib(com.oupao.wiz.Cfg.vibLevel());
            }
            com.oupao.wiz.SLog.i("Fire: kind=" + com.oupao.wiz.Sug.kind + " pkg=" + com.oupao.wiz.Sug.pkg);
            int k = com.oupao.wiz.Sug.kind;
            if (k == 1) {
                var cam = com.oupao.wiz.Sug.cam;
                if ((cam != null) && (com.oupao.wiz.Sug.torchId != null)) {
                    try {
                        cam.setTorchMode(com.oupao.wiz.Sug.torchId, false);
                    } catch (Throwable __e_torch) {
                    }
                }
            } else if ((k == 2) || (k == 6) || (k == 5)) {
                com.oupao.wiz.Sug.firePkg();
            } else if (k == 7) {
                var p7 = com.oupao.wiz.Sug.pkg;
                if (p7 != null) {
                    com.oupao.wiz.Sug.openSmall(p7);
                }
            } else if (k == 4) {
                var p4 = com.oupao.wiz.Sug.pkg;
                if (p4 != null) {
                    com.oupao.wiz.Sug.linkGuardPkg = p4;
                    com.oupao.wiz.Sug.linkGuardAt = System.currentTimeMillis();
                    com.oupao.wiz.Sug.lurl = com.oupao.wiz.Sug.link;
                    com.oupao.wiz.Sug.launch(p4);
                }
            } else if (k == 3) {
                com.oupao.wiz.Sug.keepOnBy();
            }
        } catch (Throwable __e_fire) {
            com.oupao.wiz.SLog.e("Fire: 异常", __e_fire);
        }
        com.oupao.wiz.Sug.dismiss();
    }

    private static void firePkg()
    {
        var p = com.oupao.wiz.Sug.pkg;
        if (p == null) {
            return;
        }
        if ((!p.startsWith("com.tencent.mm")) && (!p.startsWith("com.tencent.mobileqq"))) {
            // 3125fix-speed：系统短信走原生 startZoomWindow（不是 PendingIntent.send）。
            // 3132 的 PendingIntent + 1.8s 兜底能开但慢；3137 的普通 send 快但变成全屏。
            // 短信 contentIntent 的 Intent 可以直接喂给 OplusZoomWindowManager.startZoomWindow，
            // 既保留小窗、又不用等 PendingIntent 的 1.8s 超时。
            if ((com.oupao.wiz.Sug.kind == 2) && isSystemMms(p)) {
                var mmsIntent = com.oupao.wiz.Sug.ni;
                if ((mmsIntent != null) && com.oupao.wiz.Sug.zoomOpen(mmsIntent)) {
                    com.oupao.wiz.SLog.k("Fire: 短信 contentIntent -> startZoomWindow 小窗");
                    ensureMmsOpened(p);
                    return;
                }
                com.oupao.wiz.SLog.k("Fire: 短信没有 contentIntent -> 按包名小窗 launch");
                com.oupao.wiz.Sug.launch(p);
                return;
            }
            // 通用应用（「其他应用消息提醒」打开后才会弹气泡）：
            // 优先用通知自带的 contentIntent 小窗打开 —— 能落到具体页面/聊天，
            // 比 getLaunchIntentForPackage 只开首页更接近「悬浮回复」的手感。
            //
            // 3326：**先走和短信/微信同一条快路径** —— 把 contentIntent 里的 Intent（`ni`）
            //   直接喂给 startZoomWindow。实测 PendingIntent.send() 带 extra_window_mode
            //   在这台 ROM 上对部分系统/普通应用会延迟（用户反馈「点开要等一会」），
            //   而 startZoomWindow 是同步返回的，和 QQ/微信速度一致。
            //   失败（ni 为空 / 返回非 0）再退回原来的 PendingIntent 路径。
            if ((com.oupao.wiz.Sug.kind == 2) && (com.oupao.wiz.Sug.ni != null)
                    && com.oupao.wiz.Sug.zoomOpen(com.oupao.wiz.Sug.ni)) {
                com.oupao.wiz.SLog.k("Fire: 通用通知 contentIntent -> startZoomWindow 小窗");
                ensureOpenedByPackage(p);
                return;
            }
            if ((com.oupao.wiz.Sug.kind == 2) && (com.oupao.wiz.Sug.pi != null)
                    && (com.oupao.wiz.Sug.piSendZoom())) {
                com.oupao.wiz.SLog.k("Fire: 通用通知 PendingIntent -> 小窗");
                // 3125fix：部分系统应用（例如短信 com.android.mms）的 contentIntent
                // 在 system_server 里 send() 不抛异常，但实际并没有把界面拉起来。
                // 这里延迟校验前台包名，没起来就用普通 launch 兜底，避免“点了没反应”。
                ensureOpenedByPackage(p);
                return;
            }
            com.oupao.wiz.SLog.k("Fire: 非微信/QQ 包 -> 小窗 launch");
            com.oupao.wiz.Sug.launch(p);
            return;
        }
        com.oupao.wiz.Sug.fgLog("FireT");
        // 3194：全屏只有两种情况 ——
        //   ① "真在桌面、且没有小窗挂着、且不在多任务界面"；
        //   ② 前台就是**这个应用本身**（同一包名 + 同一用户）且是全屏 → 应用内跳转（用户要求保留）。
        // 其余一律小窗。
        //
        // 为什么要 ① 里的两个附加条件：
        //  - `!winOpen()`：`fgHome()` 在"应用在小窗里、桌面在底下"时可能仍报 launcher；
        //  - `!recentsOpen()`：**进多任务（概览）时桌面会被 resume 成 focused**
        //    （日志：`wm_focused_root_task … startExistingRecents` + `wm_set_resumed_activity launcher`），
        //    于是"在多任务里点气泡"也被当成"在桌面"→ 全屏。用户实测就是这个。
        com.oupao.wiz.Sug.sampleFg();                 // 3198：记一次采样
        boolean onHome = com.oupao.wiz.Sug.desktopSettled();
        boolean haveWin = com.oupao.wiz.Hooks.winOpen();
        boolean recents = com.oupao.wiz.Sug.recentsOpen();
        // 4.0-fix：直接看"前台还有没有可见的全屏应用"。
        // 只要还有，就绝不走全屏（用户 2026-11-11 复现：在别的全屏应用里点气泡被开成全屏）。
        String bgFull = com.oupao.wiz.Sug.fgFullAppPkg();
        boolean sameApp = false;
        if (onHome && (!haveWin) && (!recents) && (bgFull == null)) {
            com.oupao.wiz.SLog.k("Fire: 前台是桌面且无小窗、不在多任务 → 全屏打开");
        } else {
            // 3193/3197：同应用判定必须连用户一起比（0=主应用 / 999=分身）；
            // 包名/用户都以 **输入焦点窗口** 为准（root task 快照会滞后）。
            String fg = com.oupao.wiz.Sug.fgPkgWin();
            if (fg == null) {
                fg = com.oupao.wiz.Sug.fgPkg();
            }
            int nu = com.oupao.wiz.Sug.notifUserId();
            int fu = com.oupao.wiz.Sug.fgUserWin();
            if (fu < 0) {
                fu = com.oupao.wiz.Hooks.focusedUserId();
            }
            sameApp = (fg != null) && (com.oupao.wiz.Sug.pkg != null)
                    && fg.equals(com.oupao.wiz.Sug.pkg) && (nu == fu)
                    && com.oupao.wiz.Sug.fgFull();
            if (!sameApp) {
                com.oupao.wiz.SLog.k("Fire: 非桌面且非同一应用 → 小窗"
                        + " home=" + onHome + " win=" + haveWin + " recents=" + recents
                        + " bgFull=" + bgFull
                        + " full=" + com.oupao.wiz.Sug.fgFull()
                        + " fg=" + fg + " nu=" + nu + " fu=" + fu);
                com.oupao.wiz.Sug.launch(p);
                return;
            }
            com.oupao.wiz.SLog.k("Fire: 前台是同一应用(同用户) → 应用内全屏跳转 fg=" + fg + " u=" + fu);
        }
        if (!com.oupao.wiz.Sug.openAsUser()) {
            return;
        }
        if (com.oupao.wiz.Sug.piNativeSend()) {
            return;
        }
        com.oupao.wiz.Sug.launchFull(com.oupao.wiz.Sug.pkg);
    }
    public static void gameDbg(boolean p4, boolean p5)
    {
        try {
            var v0_1 = new StringBuilder();
            v0_1.append("Sug: \u5224\u5b9a gbs=");
            v0_1.append(p4);
            v0_1.append(" gameOn=");
            v0_1.append(p5);
            v0_1.append(" gm=");
            v0_1.append(com.oupao.wiz.Cfg.gameMaster());
            com.oupao.wiz.SLog.k(v0_1.toString());
            return;
        } catch (Throwable __e_v3) {
            return;
        }
    }
    public static void gameStart(String p3)
    {
        try {
            com.oupao.wiz.Sug.gameOn = true;
            com.oupao.wiz.Sug.gameFgPkg = p3;
            // 3125：只有「屏蔽左右下角呼出菜单」开着时才禁用热区；没开时游戏里仍可呼出扇形菜单
            com.oupao.wiz.FlyS.setGameBlocked(com.oupao.wiz.Cfg.gameBlock());
            com.oupao.wiz.SLog.i(new StringBuilder().append("Sug: \u6e38\u620f\u6a21\u5f0f\u5f00\u59cb(").append(p3).append(")").toString());
            return;
        } catch (Throwable __e___ex) {
            return;
        }
    }
    public static void gameStop()
    {
        try {
            com.oupao.wiz.Sug.gameOn = false;
            com.oupao.wiz.Sug.gameFgPkg = null;
            // 3125：退出游戏恢复热区可触摸
            com.oupao.wiz.FlyS.setGameBlocked(false);
            com.oupao.wiz.SLog.i("Sug: \u6e38\u620f\u6a21\u5f0f\u7ed3\u675f, \u6062\u590d\u5efa\u8bae");
            return;
        } catch (Throwable __e___ex) {
            return;
        }
    }
    public static void guardBt()
    {
        com.oupao.wiz.Sug.btGuard = (System.currentTimeMillis() + 3000);
        return;
    }
    /** 4.0-fix：删掉一个具体窗口（触摸回调里用，保证"谁被点就删谁"） */
    public static void dropView(android.view.View v)
    {
        if (v == null) {
            return;
        }
        try {
            var w = com.oupao.wiz.Sug.wm;
            if (w != null) {
                w.removeViewImmediate(v);
            }
        } catch (Throwable ignore) {
        }
        try {
            com.oupao.wiz.Sug.winViews.remove(v);
        } catch (Throwable ignore) {
        }
        if (v == com.oupao.wiz.Sug.banner) {
            com.oupao.wiz.Sug.banner = null;
        }
        if (v == com.oupao.wiz.Sug.hintV) {
            com.oupao.wiz.Sug.hintV = null;
        }
        if (v == com.oupao.wiz.Sug.view) {
            com.oupao.wiz.Sug.view = null;
            com.oupao.wiz.Sug.shown = false;
            com.oupao.wiz.Sug.kind = 0;
        }
    }

    /** 4.0-fix：把所有登记过的气泡/横幅窗口一次性强删（dismiss 用） */
    static void removeAllWinViews()
    {
        var w = com.oupao.wiz.Sug.wm;
        var copy = new java.util.ArrayList<android.view.View>(com.oupao.wiz.Sug.winViews);
        com.oupao.wiz.Sug.winViews.clear();
        for (android.view.View v : copy) {
            if (v == null) {
                continue;
            }
            if (w != null) {
                try {
                    w.removeViewImmediate(v);
                } catch (Throwable ignore) {
                }
            }
            try {
                v.setVisibility(android.view.View.GONE);
            } catch (Throwable ignore) {
            }
        }
        com.oupao.wiz.Sug.banner = null;
        com.oupao.wiz.Sug.hintV = null;
    }

    public static void hideBanner()
    {
        android.view.View v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Sug.banner;
        } catch (Throwable __e_v0) {
            com.oupao.wiz.Sug.banner = null;
            return;
        }
        if (v0_0 != null) {
            var v1 = com.oupao.wiz.Sug.wm;
            if (v1 != null) {
                try {
                    v1.removeViewImmediate(v0_0);
                } catch (Throwable ignore) {
                }
            }
            com.oupao.wiz.Sug.winViews.remove(v0_0);
            com.oupao.wiz.Sug.banner = null;
        }
        return;
    }
    public static void hideHint()
    {
        android.view.View v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Sug.hintV;
        } catch (Throwable __e_v0) {
            com.oupao.wiz.Sug.hintV = null;
            return;
        }
        if (v0_0 != null) {
            var v1 = com.oupao.wiz.Sug.wm;
            if (v1 != null) {
                try {
                    v1.removeViewImmediate(v0_0);
                } catch (Throwable ignore) {
                }
            }
            com.oupao.wiz.Sug.winViews.remove(v0_0);
            com.oupao.wiz.Sug.hintV = null;
        }
        return;
    }
    public static void hookInstall()
    {
        java.lang.Class v0_1 = null;
        try {
            com.oupao.wiz.SLog.i("Inst: hookInstall");
            com.oupao.wiz.SLog.i(new StringBuilder().append("Sug: build=").append(com.oupao.wiz.Diag.BUILD).toString());
            v0_1 = Class.forName("com.android.server.app.GameManagerServiceExtImpl", false, com.oupao.wiz.Hooks.CL);
        } catch (Throwable __e_v0_10) {
            com.oupao.wiz.SLog.e("Inst: hook\u5931\u8d25", __e_v0_10);
            return;
        }
        if (v0_1 != null) {
            com.oupao.wiz.XBridge.hookMethod(com.oupao.wiz.Rt.methodByName(v0_1, "publishGameEnter", 3), new com.oupao.wiz.Sug$GE());
            com.oupao.wiz.SLog.i("Sug: \u5df2\u6302\u94a9 publishGameEnter(\u6e38\u620f\u6a21\u5f0f\u5f00\u59cb)");
            com.oupao.wiz.XBridge.hookMethod(com.oupao.wiz.Rt.methodByName(v0_1, "publishGameExit", 2), new com.oupao.wiz.Sug$GX());
            com.oupao.wiz.SLog.i("Sug: \u5df2\u6302\u94a9 publishGameExit(\u6e38\u620f\u6a21\u5f0f\u7ed3\u675f)");
        } else {
        }
        try {
            var v0_4 = Class.forName("com.android.server.pm.PackageManagerService", false, com.oupao.wiz.Hooks.CL);
            com.oupao.wiz.SLog.i("Inst: forName ok");
            if (v0_4 != null) {
                var v0_5 = v0_4.getDeclaredMethods();
                if (v0_5 != null) {
                    com.oupao.wiz.SLog.i("Inst: methods ok");
                    var v1_2 = v0_5.length;
                    int v2_2 = 0;
                    int v3 = 0;
                    while (v2_2 < v1_2) {
                        var v4_3 = v0_5[v2_2];
                        if ((v4_3 != null) && ("sendPackageBroadcast".equals(v4_3.getName()))) {
                            com.oupao.wiz.XBridge.hookMethod(v4_3, new com.oupao.wiz.Sug$IH());
                            v3++;
                        }
                        v2_2++;
                    }
                    try {
                        com.oupao.wiz.XBridge.hookMethod(com.oupao.wiz.Rt.methodByName(Class.forName("com.android.server.wm.ActivityStarterExtImpl", false, com.oupao.wiz.Hooks.CL), "isInterceptBgActivityStart", 7), new com.oupao.wiz.Sug$BK());
                        com.oupao.wiz.SLog.i("Inst: hookStart ok");
                        var v4_1 = new StringBuilder();
                        v4_1.append("Inst: hook\u6570\u91cf=");
                        v4_1.append(v3);
                        com.oupao.wiz.SLog.i(v4_1.toString());
                    } catch (Throwable tStart) {
                        // 「应用跳转确认」的抑制钩子没挂上就会弹窗，原来这里静默 → 用户只看到「点了没反应」
                        com.oupao.wiz.SLog.e("Inst: hook isInterceptBgActivityStart \u5931\u8d25(\u8df3\u8f6c\u786e\u8ba4\u4f1a\u5f39)", tStart);
                    }
                }
            }
            return;
        } catch (Throwable tPkg) {
            com.oupao.wiz.SLog.e("Inst: hook sendPackageBroadcast \u5931\u8d25(\u5305\u53d8\u5316\u4e8b\u4ef6/\u5b89\u88c5\u63d0\u793a\u4f1a\u5931\u6548)", tPkg);
        }
    }
    public static android.graphics.Bitmap iconAda(android.graphics.drawable.Drawable p14, int p15)
    {
        android.graphics.Bitmap v0 = null;
        try {
            if (com.oupao.wiz.FrameworkExt.drawableLayerCount(((android.graphics.drawable.AdaptiveIconDrawable) p14)) != 2) {
                v0 = android.graphics.Bitmap.createBitmap(p15, p15, android.graphics.Bitmap.Config.ARGB_8888);
                var v2_1 = new android.graphics.Canvas(v0);
                var v3_1 = new android.graphics.Path();
                v3_1.addCircle((((float) p15) / 2.0f), (((float) p15) / 2.0f), (((float) p15) / 2.0f), android.graphics.Path.Direction.CW);
                v2_1.clipPath(v3_1);
                var v5_2 = com.oupao.wiz.FrameworkExt.drawableLayer(((android.graphics.drawable.AdaptiveIconDrawable) p14), 0);
                var v6 = com.oupao.wiz.FrameworkExt.drawableLayer(((android.graphics.drawable.AdaptiveIconDrawable) p14), 1);
                if ((v5_2 != null) && (v6 != null)) {
                    var v7 = (p15 / 4);
                    v5_2.setBounds((- v7), (- v7), (p15 + v7), (p15 + v7));
                    v6.setBounds(0, 0, p15, p15);
                    v5_2.draw(v2_1);
                    v2_1.save();
                    v2_1.scale(1.375f, 1.375f, (((float) p15) / 2.0f), (((float) p15) / 2.0f));
                    v6.draw(v2_1);
                    v2_1.restore();
                }
            }
        } catch (Throwable __e_v1) {
            return v0;
        }
        return v0;
    }
    public static android.graphics.Bitmap iconBmp(android.graphics.drawable.Drawable p9, int p10)
    {
        android.graphics.Bitmap v0 = null;
        try {
            v0 = android.graphics.Bitmap.createBitmap(p10, p10, android.graphics.Bitmap.Config.ARGB_8888);
        } catch (Throwable __e_v1) {
            return v0;
        }
        if (p9 != null) {
            var v1_2 = new android.graphics.Canvas(v0);
            p9.setBounds(0, 0, p10, p10);
            p9.draw(v1_2);
            if (((p9 instanceof android.graphics.drawable.AdaptiveIconDrawable)) && (com.oupao.wiz.FrameworkExt.drawableLayerCount(((android.graphics.drawable.AdaptiveIconDrawable) p9)) == 2)) {
                var v5 = com.oupao.wiz.FrameworkExt.drawableLayer(((android.graphics.drawable.AdaptiveIconDrawable) p9), 0);
                var v6 = com.oupao.wiz.FrameworkExt.drawableLayer(((android.graphics.drawable.AdaptiveIconDrawable) p9), 1);
                if ((v5 != null) && (v6 != null)) {
                    var v7 = (p10 / 4);
                    var v8 = (- v7);
                    var v3_3 = (p10 + v7);
                    v5.setBounds(v8, v8, v3_3, v3_3);
                    v6.setBounds(v8, v8, v3_3, v3_3);
                    v5.draw(v1_2);
                    v6.draw(v1_2);
                }
            }
        }
        return v0;
    }
    public static android.graphics.Bitmap iconOf(android.graphics.drawable.Drawable p5, int p6)
    {

        android.graphics.Bitmap v0 = null;
        try {
            if ((p5 instanceof android.graphics.drawable.AdaptiveIconDrawable)) {
                var v2_4 = com.oupao.wiz.Sug.iconAda(p5, p6);
                if (v2_4 != null) {
                    return v2_4;
                }
            }
        } catch (Throwable __e_v1) {
            return v0;
        }
        v0 = com.oupao.wiz.Sug.drawableToBitmap(p5, p6);
        var v1_1 = com.oupao.wiz.Sug.opaque(v0);
        if (v1_1 < 8) {
            var v3_0 = com.oupao.wiz.Sug.iconBmp(p5, p6);
            if (v3_0 != null) {
                    v0 = v3_0;
                }
        }
        var v2_2 = new StringBuilder();
        v2_2.append("Sug: icon o=");
        v2_2.append(v1_1);
        v2_2.append(" size=");
        v2_2.append(p6);
        com.oupao.wiz.SLog.i(v2_2.toString());
        return v0;
    }
    /**
     * 给「分身应用」的气泡在右下角加一个分身角标（蓝底白字「分」）。
     * 判定见调用方：`Sug.uid / 100000 != 0` 表示跑在分身/工作资料用户里。
     */
    public static android.graphics.Bitmap cloneBadge(android.graphics.Bitmap p2, int p3)
    {
        try {
            if ((p2 == null) || (p3 <= 0)) {
                return p2;
            }
            var out = android.graphics.Bitmap.createBitmap(p3, p3,
                    android.graphics.Bitmap.Config.ARGB_8888);
            var c = new android.graphics.Canvas(out);
            c.drawBitmap(p2, 0.0f, 0.0f, null);

            // 与已有的分身角标（numBadge）保持同一套视觉：粉色底 + 白色粗体字 + 右下 2% 边距
            int w = ((p3 * 46) / 100);
            if (w < 8) {
                w = 8;
            }
            var bg = android.graphics.Bitmap.createBitmap(w, w,
                    android.graphics.Bitmap.Config.ARGB_8888);
            var cb = new android.graphics.Canvas(bg);
            var p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            p.setColor(-1722953);
            cb.drawCircle((((float) w) / 2.0f), (((float) w) / 2.0f), (((float) w) / 2.0f), p);
            var tp = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            tp.setColor(-1);
            tp.setTextSize(((float) w) * 0.60f);
            tp.setTextAlign(android.graphics.Paint.Align.CENTER);
            tp.setFakeBoldText(true);
            var fm = tp.getFontMetrics();
            float ty = ((((float) w) / 2.0f) - ((fm.ascent + fm.descent) / 2.0f));
            cb.drawText("\u5206", (((float) w) / 2.0f), ty, tp);
            int m = ((p3 * 2) / 100);
            c.drawBitmap(bg, ((float) (p3 - w - m)), ((float) (p3 - w - m)), null);
            return out;
        } catch (Throwable t) {
            return p2;
        }
    }

    public static android.graphics.Bitmap iconRound(android.graphics.Bitmap p14, int p15)
    {
        android.graphics.Bitmap v0 = null;
        int v1 = 0;
        int v2_1 = 0;
        android.graphics.Bitmap src = p14;
        try {
            v1 = src.getWidth();
            v2_1 = src.getHeight();
        } catch (Throwable __e_v3) {
            return v0;
        }
        if ((v1 > 0) && (v2_1 > 0)) {
            int v11_0 = 0;
            int v9_0 = 0;
            int v10_0 = 0;
            int v8 = 0;
            if (!com.oupao.wiz.Sug.scan(src)) {
                v8 = 0;
                v9_0 = 0;
                v10_0 = v1;
                v11_0 = v2_1;
            } else {
                v8 = com.oupao.wiz.Sug.bbX1;
                v9_0 = com.oupao.wiz.Sug.bbY1;
                v10_0 = ((com.oupao.wiz.Sug.bbX2 - v8) + 1);
                v11_0 = ((com.oupao.wiz.Sug.bbY2 - v9_0) + 1);
            }
            var v12_2 = ((((float) p15) * 1.08f) / ((float) Math.max(v10_0, v11_0)));
            var v7_5 = ((((float) p15) / 2.0f) - ((((float) v8) + (((float) v10_0) / 2.0f)) * v12_2));
            var v13_9 = ((((float) p15) / 2.0f) - ((((float) v9_0) + (((float) v11_0) / 2.0f)) * v12_2));
            v0 = android.graphics.Bitmap.createBitmap(p15, p15, android.graphics.Bitmap.Config.ARGB_8888);
            var v3_1 = new android.graphics.Canvas(v0);
            var v4_3 = new android.graphics.Paint(1);
            var v5_1 = new android.graphics.BitmapShader(src, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP);
            var v6_1 = new android.graphics.Matrix();
            v6_1.setScale(v12_2, v12_2);
            v6_1.postTranslate(v7_5, v13_9);
            v5_1.setLocalMatrix(v6_1);
            v4_3.setShader(v5_1);
            v3_1.drawCircle((((float) p15) / 2.0f), (((float) p15) / 2.0f), (((float) p15) / 2.0f), v4_3);
        }
        return v0;
    }
    public static void imeCame()
    {
        try {
            // 3501：输入法弹出 → 收掉还开着的扇形菜单（它可能正停在角落等选择）。
            //   主线程操作窗口，这里只投递。
            if (com.oupao.wiz.FlyFanH.alive() || com.oupao.wiz.FlyState.open()) {
                com.oupao.wiz.SLog.i("Sug: 输入法弹出 → 收扇形菜单");
                com.oupao.wiz.Sug.postMain(new Runnable() {
                    public void run() {
                        try {
                            com.oupao.wiz.FlyFanH.hide();
                            com.oupao.wiz.FlyState.setOpen(false);
                        } catch (Throwable ignore) {
                        }
                    }
                });
            }
            // 只有「正在显示的是合并的通知气泡」才不动；分离气泡/建议气泡保持原逻辑（收起→键盘收起后重放）
            if (com.oupao.wiz.Bub.ignoreImeFor(com.oupao.wiz.Sug.kind)) {
                com.oupao.wiz.SLog.i("Sug: 键盘弹出（合并通知气泡 → 不动气泡）");
                return;
            }
            if (com.oupao.wiz.Sug.shown) {
                com.oupao.wiz.Sug.pendKind = com.oupao.wiz.Sug.kind;
                com.oupao.wiz.Sug.pendOn = true;
                com.oupao.wiz.Sug.shown = false;
                com.oupao.wiz.Sug.postMain(new com.oupao.wiz.Sug$C3());
                com.oupao.wiz.SLog.i("Sug: \u8f93\u5165\u6cd5\u5f39\u51fa \u2192 \u6c14\u6ce1\u5148\u6536\u8d77(\u6536\u8d77\u540e\u91cd\u663e\u793a)");
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static void imeGone()
    {
        try {
            if (!com.oupao.wiz.Sug.pendOn) {
                if (com.oupao.wiz.Sug.shown) {
                    var v0_6 = com.oupao.wiz.Sug.main;
                    if (v0_6 != null) {
                        com.oupao.wiz.Sug.cancelTimer();
                        // 3321：手电筒气泡不自动消失，输入法收起也不需要重新计时
                        if (com.oupao.wiz.Sug.kind != 1) {
                            var v1_3 = new com.oupao.wiz.Sug$C3();
                            com.oupao.wiz.Sug.timer = v1_3;
                            v0_6.postDelayed(v1_3, 5000);
                        }
                        com.oupao.wiz.SLog.k("Sug: \u8f93\u5165\u6cd5\u6536\u8d77 -> \u6c14\u6ce1\u91cd\u65b0\u8ba1\u65f6\u663e\u793a");
                    }
                }
                return;
            } else {
                com.oupao.wiz.Sug.pendOn = false;
                com.oupao.wiz.Sug.postMain(new com.oupao.wiz.Sug$C4(com.oupao.wiz.Sug.pendKind));
                com.oupao.wiz.SLog.k("Sug: \u8f93\u5165\u6cd5\u6536\u8d77 -> \u4e3b\u7ebf\u7a0b\u8865\u663e\u793a\u6302\u8d77\u7684\u6c14\u6ce1");
                return;
            }
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static boolean imeUp()
    {
        int v0_0 = 0;
        try {
            if (com.oupao.wiz.Sug.imeVis > 0) {
                v0_0 = 1;
            }
        } catch (Throwable __e_v1) {
            return false;
        }
        return v0_0 != 0;
    }
    private static void infoLog(String p1, String p2)
    {
        try {
            com.oupao.wiz.SLog.i(new StringBuilder().append(p1).append(p2).toString());
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static boolean infoOn()
    {
        // 旧版是微信/QQ 各自一个「信息气泡」开关（oupao_wx_info / oupao_qq_info）。
        // 气泡做成全局后这两个键被丢掉了 → 用户找不到「头像气泡右侧显示消息内容」。
        // 现在接成一个**全局**开关 oupao_sug_info（默认开，行为与旧版默认一致）。
        try {
            // 与「屏蔽悬浮通知」同构：开关 + 生效应用名单（名单留空 = 全部应用）
            if (com.oupao.wiz.Cfg.sugInfoAppOn(String.valueOf(com.oupao.wiz.Sug.pkg))) {
                return true;
            }
            com.oupao.wiz.Sug.infoLog("Sug: 信息气泡已关 pkg=", String.valueOf(com.oupao.wiz.Sug.pkg));
            return false;
        } catch (Throwable t) {
            return true;
        }
    }
    public static void init()
    {
        // 1820fix: ctx 为空时必须允许重试（原来 tried=true 后就永不再试 → 气泡动作全部失效）
        if ((!com.oupao.wiz.Sug.tried) || (com.oupao.wiz.Sug.ctx == null)) {
            com.oupao.wiz.SLog.i("Sug: init \u8fdb\u5165");
            com.oupao.wiz.Sug.tried = true;
            if (com.oupao.wiz.Sug.ctx == null) {
                android.content.Context v0_1 = null;
                try {
                    v0_1 = com.oupao.wiz.Rt.ctx();
                } catch (Throwable __e_v0_5) {
                    com.oupao.wiz.SLog.e("Sug: init\u5f02\u5e38", __e_v0_5);
                    return;
                }
                if (v0_1 == null) {
                    v0_1 = com.oupao.wiz.Cfg.sysContext();
                }
                if (v0_1 == null) {
                    com.oupao.wiz.SLog.i("Sug: \u65e0\u4e0a\u4e0b\u6587(\u8df3\u8fc7)");
                    return;
                } else {
                    com.oupao.wiz.Sug.ctx = v0_1;
                    com.oupao.wiz.FlyS.syncWith(v0_1);
                    com.oupao.wiz.Sug.hookInstall();
                    var v1_0 = com.oupao.wiz.Hooks.main();
                    com.oupao.wiz.Sug.main = v1_0;
                    com.oupao.wiz.Sug.wm = ((android.view.WindowManager) v0_1.getSystemService("window"));
                    android.hardware.camera2.CameraManager v2_5 = ((android.hardware.camera2.CameraManager) v0_1.getSystemService("camera"));
                    com.oupao.wiz.Sug.cam = v2_5;
                    if (v2_5 != null) {
                        com.oupao.wiz.Sug.txt = "\ud83d\ude0f";
                        com.oupao.wiz.Sug.startTick();
                        v0_1.registerReceiver(new com.oupao.wiz.Sug$R(), new android.content.IntentFilter("android.intent.action.SCREEN_OFF"));
                        var v2_10 = new com.oupao.wiz.Sug$PK();
                        var v3_4 = new android.content.IntentFilter("android.intent.action.PACKAGE_ADDED");
                        v3_4.addDataScheme("package");
                        v0_1.registerReceiver(v2_10, v3_4);
                        com.oupao.wiz.Sug.guardBt();
                        com.oupao.wiz.Sug.regAudio();
                        v2_5 = com.oupao.wiz.Sug.cam;
                    }
                    if (v2_5 == null) {
                        com.oupao.wiz.SLog.i("Sug: camera \u670d\u52a1\u4e3a\u7a7a");
                        return;
                    } else {
                        v2_5.registerTorchCallback(new com.oupao.wiz.Sug$TB(), v1_0);
                        com.oupao.wiz.SLog.i("Sug: \u521d\u59cb\u5316\u5b8c\u6210(\u624b\u7535\u7b52\u76d1\u542c\u5df2\u6ce8\u518c)");
                        return;
                    }
                }
            } else {
                return;
            }
        } else {
            return;
        }
    }
    public static void install(String p7)
    {
        android.content.Context v0_1 = null;
        try {
            com.oupao.wiz.SLog.i("Inst: install ".concat(p7));
            v0_1 = com.oupao.wiz.Sug.ctx;
        } catch (Throwable __e_v0_0) {
            com.oupao.wiz.SLog.e("Inst: install \u5f02\u5e38", __e_v0_0);
            return;
        }
        if (v0_1 != null) {
            var v1_1 = v0_1.getPackageManager();
            if (v1_1.getLaunchIntentForPackage(p7) != null) {
                android.graphics.Bitmap v3_0 = null;
                try {
                    var v4_0 = v1_1.getApplicationIcon(p7);
                    if (v4_0 != null) {
                        var v5_1 = com.oupao.wiz.Ui.dp(v0_1, 96);
                        v3_0 = com.oupao.wiz.Sug.iconRound(com.oupao.wiz.Sug.iconOf(v4_0, v5_1), v5_1);
                    }
                    var v4_2 = new StringBuilder();
                    v4_2.append("Inst: \u65b0\u5e94\u7528\u6c14\u6ce1 ");
                    v4_2.append(p7);
                    com.oupao.wiz.SLog.i(v4_2.toString());
                    com.oupao.wiz.Sug.showInstall(p7, v3_0);
                } catch (Throwable __e_io) {
                }
            }
        }
        return;
    }
    public static void installSoon(String p4)
    {
        android.os.Handler v0_0 = null;
        try {
            com.oupao.wiz.SLog.i("Inst: installSoon");
            v0_0 = com.oupao.wiz.Sug.main;
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.SLog.e("Inst: installSoon \u5f02\u5e38", __e_v0_1);
            return;
        }
        if (v0_0 != null) {
            v0_0.postDelayed(new com.oupao.wiz.Sug$IP(p4), 0);
        }
        return;
    }
    public static boolean isCall(android.app.Notification p2)
    {
        try {
            if (p2 != null) {
                var v0_0 = p2.category;
                if ((v0_0 != null) && ("call".equals(v0_0))) {
                    return true;
                }
            }
        } catch (Throwable __e_v0) {
            return false;
        }
        return false;
    }
    public static boolean isGameFg()
    {
        try {
            return com.oupao.wiz.Sug.gameOn;
        } catch (Throwable __e___ex) {
            return false;
        }
    }
    public static void keepOff()
    {
        android.os.PowerManager.WakeLock v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Sug.keepWL;
        } catch (Throwable __e_v0) {
            return;
        }
        if ((v0_0 != null) && (v0_0.isHeld())) {
            v0_0.release();
        }
        com.oupao.wiz.Sug.keepActive = false;
        com.oupao.wiz.Sug.keepSrc = 0;
        com.oupao.wiz.Sug.keepPkg = null;
        com.oupao.wiz.SLog.i("Sug: \u5e94\u7528\u5185\u5e38\u4eae\u5df2\u7ed3\u675f");
        return;
    }
    public static boolean keepOld(int p2, int p3)
    {
        int v0_0 = 1;
        if (p2 != 2) {
            if (p2 != p3) {
                v0_0 = 0;
            }
            return v0_0 != 0;
        } else {
            return false;
        }
    }
    public static void keepOn()
    {
        try {
            if (!com.oupao.wiz.Sug.keepActive) {
                var v0_1 = com.oupao.wiz.Sug.ctx;
                if (v0_1 != null) {
                    var v1_3 = ((android.os.PowerManager) v0_1.getSystemService("power"));
                    if (v1_3 != null) {
                        if (com.oupao.wiz.Sug.keepWL == null) {
                            var v2_1 = v1_3.newWakeLock(10, "oupao:sug");
                            com.oupao.wiz.Sug.keepWL = v2_1;
                            v2_1.setReferenceCounted(false);
                        }
                        com.oupao.wiz.Sug.keepWL.acquire();
                        com.oupao.wiz.Sug.keepActive = true;
                        com.oupao.wiz.SLog.i("Sug: \u5c4f\u5e55\u5e38\u4eae\u5df2\u5f00\u542f");
                    }
                }
            }
        } catch (Throwable __e_v0_2) {
            com.oupao.wiz.SLog.e("Sug: \u5e38\u4eae\u5f02\u5e38", __e_v0_2);
            return;
        }
        return;
    }
    public static void keepOnBy()
    {
        try {
            if (com.oupao.wiz.Cfg.sugKeep()) {
                com.oupao.wiz.Sug.keepSrc = 0;
                com.oupao.wiz.Sug.keepPkg = null;
                com.oupao.wiz.SLog.i("SugK \u5168\u5c40\u5e38\u4eae\u5f00\u59cb(\u76f4\u5230\u606f\u5c4f)");
                com.oupao.wiz.Sug.keepOn();
                return;
            } else {
                com.oupao.wiz.Sug.keepSrc = 2;
                com.oupao.wiz.Sug.keepPkg = com.oupao.wiz.Sug.fgPkg();
                com.oupao.wiz.SLog.i("SugK \u5e94\u7528\u5185\u5e38\u4eae\u5f00\u59cb");
                com.oupao.wiz.Sug.keepOn();
                com.oupao.wiz.Sug.showHint();
                return;
            }
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static void launch(String p15)
    {
        try {
            com.oupao.wiz.Sug.lastAt = android.os.SystemClock.uptimeMillis();
        } catch (Throwable ignore) {
        }
        if (p15 == null) {
            return;
        }
        try {
            com.oupao.wiz.SLog.i("Launch[b" + com.oupao.wiz.Diag.BUILD + "]: " + p15);
            android.content.Context c = com.oupao.wiz.Sug.ctx;
            if (c == null) {
                com.oupao.wiz.SLog.i("Launch: ctx null，尝试补取");
                try {
                    com.oupao.wiz.Sug.init();
                } catch (Throwable ignore) {
                }
                c = com.oupao.wiz.Sug.ctx;
                if (c == null) {
                    c = com.oupao.wiz.Ctx.get();
                }
                if (c == null) {
                    c = com.oupao.wiz.Cfg.sysContext();
                }
                if (c != null) {
                    com.oupao.wiz.Sug.ctx = c;
                }
            }
            if (c == null) {
                com.oupao.wiz.SLog.i("Launch: 仍无 ctx，放弃打开 " + p15);
                return;
            }
            if ("com.tencent.mm".equals(p15)) {
                int u = com.oupao.wiz.Sug.notifUserId();
                if (u > 0) {
                    var niW = com.oupao.wiz.Sug.ni;
                    if ((niW != null) && (com.oupao.wiz.Sug.zoomOpenAs(niW, u))) {
                        return;
                    }
                    if (com.oupao.wiz.Sug.openAsUser()) {
                        com.oupao.wiz.SLog.i("Launch: 分身微信 -> 全屏兜底打开");
                        return;
                    }
                }
                com.oupao.wiz.Sug.wxLog();
                if (com.oupao.wiz.Sug.ni == null) {
                    if ((com.oupao.wiz.Sug.pi != null) && (com.oupao.wiz.Sug.piSendZoom())) {
                        com.oupao.wiz.SLog.i("Launch: 用通知 PendingIntent 小窗打开");
                        return;
                    }
                }
                var niW3 = com.oupao.wiz.Sug.ni;
                if (niW3 != null) {
                    var comp = niW3.getComponent();
                    if ((comp != null)
                            && (!comp.getClassName().contains("Chatting"))
                            && (!com.oupao.wiz.Sug.zoomOpen(niW3))) {
                        com.oupao.wiz.SLog.i("Launch: 主微信 -> 原始 Intent");
                        return;
                    }
                }
                var chat = com.oupao.wiz.Sug.chatUser;
                com.oupao.wiz.Sug.noteChatId("com.tencent.mm", chat);
                if (chat != null) {
                    com.oupao.wiz.Sug.chatUser = null;
                    var ci = com.oupao.wiz.Sug.chatIntent(chat);
                    if (ci == null) {
                        com.oupao.wiz.SLog.i("Launch: no intent");
                        return;
                    }
                    com.oupao.wiz.Sug.launchTail(ci, p15);
                    return;
                }
            }
            if (("com.tencent.mobileqq".equals(p15)) && (com.oupao.wiz.Sug.kind == 2)) {
                var niQ = com.oupao.wiz.Sug.ni;
                if (niQ != null) {
                    if (com.oupao.wiz.Sug.zoomOpen(niQ)) {
                        return;
                    }
                    if (com.oupao.wiz.Sug.piBoundsSend()) {
                        return;
                    }
                } else if ((com.oupao.wiz.Sug.qqUin != null) && (com.oupao.wiz.Sug.qqType != null)) {
                    if (com.oupao.wiz.Sug.piBoundsSend()) {
                        return;
                    }
                } else {
                    var qq = new android.content.Intent("android.intent.action.VIEW");
                    String ct = "2".equals(com.oupao.wiz.Sug.qqType) ? "group" : "wpa";
                    qq.setData(android.net.Uri.parse("mqqwpa://im/chat?chat_type=" + ct
                            + "&uin=" + com.oupao.wiz.Sug.qqUin + "&version=1&src_type=web"));
                    qq.setPackage(p15);
                    if (com.oupao.wiz.Sug.zoomOpen(qq)) {
                        return;
                    }
                    if (com.oupao.wiz.Sug.piBoundsSend()) {
                        return;
                    }
                }
            }
            com.oupao.wiz.Sug.launchTail(null, p15);
            return;
        } catch (Throwable __e_launch) {
            com.oupao.wiz.SLog.e("Sug: 打开异常", __e_launch);
            return;
        }
    }
    /**
     * 1820fix: smali 的"统一出台"(:cond_115 → :goto_136 → :goto_141)。
     * 迁移时这段被错嵌进 QQ 分支，导致 v1_1 建好却从不启动 → 除手电筒/常亮外的动作全部无效。
     */
    /**
     * 1820 官方语义（Sug.smali launch 4376-4498）：
     *   建 ActivityOptions(extra_window_mode=100, splashScreenStyle=1) → 反射 startZoomWindow(...,user,"com.coloros.smartsidebar")
     *   → 返回 0 表示成功 → 1200ms 后由 Sug$ZFb 校验是否到前台，未到则普通打开（同一个 Intent，含 dat 链接）。
     */
    public static void launchTail(android.content.Intent v1_1, String p15)
    {
        android.content.Context ctx = null;
        try {
            ctx = com.oupao.wiz.Sug.ctx;
            if (ctx == null) {
                com.oupao.wiz.Sug.init();
                ctx = com.oupao.wiz.Sug.ctx;
            }
            if (ctx == null) {
                ctx = com.oupao.wiz.Cfg.sysContext();
            }
            if (ctx == null) {
                com.oupao.wiz.SLog.i("Launch: 无 ctx，放弃 " + p15);
                return;
            }
            if (v1_1 == null) {
                var lurl = com.oupao.wiz.Sug.lurl;
                com.oupao.wiz.Sug.lurl = null;
                if (lurl != null) {
                    v1_1 = new android.content.Intent("android.intent.action.VIEW");
                    v1_1.setData(android.net.Uri.parse(lurl));
                    v1_1.setPackage(p15);
                } else {
                    v1_1 = ctx.getPackageManager().getLaunchIntentForPackage(p15);
                }
            }
            if (v1_1 == null) {
                com.oupao.wiz.SLog.i("Launch: no intent");
                return;
            }
            v1_1.addFlags(268435456);   // NEW_TASK
            v1_1.addFlags(32);          // INCLUDE_STOPPED_PACKAGES
            com.oupao.wiz.SLog.i("Launch: intent=" + v1_1);

            var lao = android.app.ActivityOptions.makeBasic().setLaunchDisplayId(0);
            android.graphics.Rect lb = com.oupao.wiz.ZoomGate.notificationLaunchBounds();
            if (lb != null) {
                lao.setLaunchBounds(lb);
            }
            var opts = lao.toBundle();
            opts.putInt("extra_window_mode", 100);
            // C17：和已验证正常的 zoomOpenAs 对齐。只给 extra_window_mode、不给
            //   android:activity.windowingMode 时，通知 Intent 偶发按普通启动处理 →
            //   任务用异常 bounds/scale 出现（细条小窗）。C16 保持原 splash 配置不变。
            if (com.oupao.wiz.Cfg.isC17()) {
                opts.putInt("android:activity.windowingMode", 100);
            } else {
                opts.putInt("android.activity.splashScreenStyle", 1);
            }
            // ⚠️ 变量名以前叫 uid，实际是 **userId**（2893 那次就是拿 uid 当 userId 传给
            //    startZoomWindow → 返回 0 但窗口根本没建出来）。这里改成 user，别再混。
            int user = com.oupao.wiz.Sug.notifUserId();
            Object zoomRet = null;
            try {
                var zc = Class.forName("com.oplus.zoomwindow.OplusZoomWindowManager");
                var inst = com.oupao.wiz.Rt.invoke(com.oupao.wiz.Rt.methodByName(zc, "getInstance", 0), null, null);
                var m = com.oupao.wiz.Rt.methodByName(zc, "startZoomWindow", 4);
                Object[] args = new Object[4];
                args[0] = v1_1;
                args[1] = opts;
                args[2] = Integer.valueOf(user);
                args[3] = "com.coloros.smartsidebar";
                zoomRet = com.oupao.wiz.Rt.invoke(m, inst, args);
            } catch (Throwable tz) {
                com.oupao.wiz.SLog.e("Sug: startZoomWindow 异常", tz);
            }
            com.oupao.wiz.SLog.i("Sug: zoom result=" + String.valueOf(zoomRet)
                    + " dat=" + v1_1.getDataString() + " user=" + user);

            // 1200ms 后校验是否真的到前台，未到则普通打开（官方 Sug$ZFb）
            try {
                com.oupao.wiz.Sug.lastAt = 0L;   // 让 ZFb 的“刚发过”门槛不再直接吞掉
                var h = com.oupao.wiz.Hooks.main();
                if (h != null) {
                    h.postDelayed(new com.oupao.wiz.Sug$ZFb(v1_1, p15), 1200);
                    com.oupao.wiz.SLog.i("Sug: 已请求小窗打开(1200ms 后校验)");
                }
            } catch (Throwable tz2) {
                com.oupao.wiz.SLog.e("Sug: 调度 ZFb 异常", tz2);
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Sug: launchTail 异常", t);
        }
    }

    public static boolean launchFull(String p5)
    {
        try {
            if (p5 != null) {
                var v0_1 = com.oupao.wiz.Cfg.sysContext();
                if (v0_1 != null) {
                    android.content.Intent v1_0 = null;
                    if ("com.tencent.mm".equals(p5)) {
                        var v2_5 = com.oupao.wiz.Sug.chatUser;
                        if (v2_5 != null) {
                            v1_0 = com.oupao.wiz.Sug.chatIntent(v2_5);
                        }
                    }
                    if (v1_0 == null) {
                        v1_0 = v0_1.getPackageManager().getLaunchIntentForPackage(p5);
                    }
                    if (v1_0 != null) {
                        v1_0.addFlags(268435456);
                        var v3 = com.oupao.wiz.Sug.notifUserId();
                        if (v3 == 0) {
                            v0_1.startActivity(v1_0);
                        } else {
                            com.oupao.wiz.FrameworkExt.startActivityAsUser(v0_1, v1_0, com.oupao.wiz.FrameworkExt.userHandleOf(v3));
                        }
                        com.oupao.wiz.SLog.k("Fire: \u5168\u5c4f\u515c\u5e95\u542f\u52a8(pkg \u542f\u52a8 intent)");
                        return true;
                    }
                }
            }
        } catch (Throwable __e_v1_1) {
            com.oupao.wiz.SLog.e("Launch: full fallback failed", __e_v1_1);
            return false;
        }
        return false;
    }
    public static void lg(String p3, int p4)
    {
        try {
            com.oupao.wiz.SLog.i(new StringBuilder().append(p3).append(p4).toString());
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    static boolean linkDup(String p6)
    {
        java.lang.String v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Sug.linkGuardPkg;
        } catch (Throwable __e_v0) {
            return false;
        }
        if ((v0_0 == null) || ((!v0_0.equals(p6)) || ((System.currentTimeMillis() - com.oupao.wiz.Sug.linkGuardAt) >= 8000))) {
            return false;
        } else {
            return true;
        }
    }
    public static boolean lockNow()
    {
        boolean v0_0 = false;
        android.content.Context v1_0 = null;
        try {
            v1_0 = com.oupao.wiz.Sug.ctx;
        } catch (Throwable __e___ex) {
            return false;
        }
        if (v1_0 != null) {
            var v1_1 = v1_0.getSystemService("keyguard");
            if ((v1_1 instanceof android.app.KeyguardManager)) {
                v0_0 = ((android.app.KeyguardManager) v1_1).isKeyguardLocked();
            }
        }
        return v0_0;
    }
    public static String mapPkg(String p3)
    {
        java.lang.String v0_10 = null;
        try {
            // 3359+：天猫/闲鱼新域名。淘宝分享现在经常是 detail.tmall.com / www.goofish.com，
            // 只认 taobao.com 会匹配不到 → 点链接不弹气泡。
            v0_10 = com.oupao.wiz.Sug.mk(p3, "tmall.com", "com.taobao.taobao");
            if (v0_10 == null) v0_10 = com.oupao.wiz.Sug.mk(p3, "tmall.hk", "com.taobao.taobao");
            if (v0_10 == null) v0_10 = com.oupao.wiz.Sug.mk(p3, "taobao.hk", "com.taobao.taobao");
            if (v0_10 == null) v0_10 = com.oupao.wiz.Sug.mk(p3, "goofish.com", "com.taobao.idlefish");
            if (v0_10 == null) v0_10 = com.oupao.wiz.Sug.mk(p3, "taobao.com", "com.taobao.taobao");
        } catch (Throwable __e_v0) {
            return null;
        }
        if (v0_10 == null) {
            var v0_32 = com.oupao.wiz.Sug.mk(p3, "tb.cn", "com.taobao.taobao");
            if (v0_32 == null) {
                var v0_3 = com.oupao.wiz.Sug.mk(p3, "jd.com", "com.jingdong.app.mall");
                if (v0_3 == null) {
                    var v0_5 = com.oupao.wiz.Sug.mk(p3, "v.douyin.com", "com.ss.android.ugc.aweme");
                    if (v0_5 == null) {
                        var v0_7 = com.oupao.wiz.Sug.mk(p3, "iesdouyin.com", "com.ss.android.ugc.aweme");
                        if (v0_7 == null) {
                            var v0_9 = com.oupao.wiz.Sug.mk(p3, "bilibili.com", "tv.danmaku.bili");
                            if (v0_9 == null) {
                                var v0_12 = com.oupao.wiz.Sug.mk(p3, "b23.tv", "tv.danmaku.bili");
                                if (v0_12 == null) {
                                    var v0_14 = com.oupao.wiz.Sug.mk(p3, "weibo.com", "com.sina.weibo");
                                    if (v0_14 == null) {
                                        var v0_16 = com.oupao.wiz.Sug.mk(p3, "xiaohongshu", "com.xingin.xhs");
                                        if (v0_16 == null) {
                                            var v0_18 = com.oupao.wiz.Sug.mk(p3, "xhslink", "com.xingin.xhs");
                                            if (v0_18 == null) {
                                                var v0_21 = com.oupao.wiz.Sug.mk(p3, "yangkeduo.com", "com.xunmeng.pinduoduo");
                                                if (v0_21 == null) {
                                                    var v0_23 = com.oupao.wiz.Sug.mk(p3, "coolapk.com", "com.coolapk.market");
                                                    if (v0_23 == null) {
                                                        var v0_25 = com.oupao.wiz.Sug.mk(p3, "pan.quark.cn", "com.quark.browser");
                                                        if (v0_25 == null) {
                                                            var v0_27 = com.oupao.wiz.Sug.mk(p3, "h5.m.goofish.com", "com.taobao.idlefish");
                                                            if (v0_27 == null) {
                                                                var v0_29 = com.oupao.wiz.Sug.mk(p3, "1688.com", "com.alibaba.wireless");
                                                                if (v0_29 == null) {
                                                                    var v0_31 = com.oupao.wiz.Sug.mk(p3, "//amap.com/", "com.autonavi.minimap");
                                                                    if (v0_31 == null) {
                                                                        var v0_34 = com.oupao.wiz.Sug.mk(p3, "qunar.com", "com.Qunar");
                                                                        if (v0_34 == null) {
                                                                            var v0_36 = com.oupao.wiz.Sug.mk(p3, "kugou.com", "com.kugou.android");
                                                                            if (v0_36 == null) {
                                                                                return null;
                                                                            } else {
                                                                                return v0_36;
                                                                            }
                                                                        } else {
                                                                            return v0_34;
                                                                        }
                                                                    } else {
                                                                        return v0_31;
                                                                    }
                                                                } else {
                                                                    return v0_29;
                                                                }
                                                            } else {
                                                                return v0_27;
                                                            }
                                                        } else {
                                                            return v0_25;
                                                        }
                                                    } else {
                                                        return v0_23;
                                                    }
                                                } else {
                                                    return v0_21;
                                                }
                                            } else {
                                                return v0_18;
                                            }
                                        } else {
                                            return v0_16;
                                        }
                                    } else {
                                        return v0_14;
                                    }
                                } else {
                                    return v0_12;
                                }
                            } else {
                                return v0_9;
                            }
                        } else {
                            return v0_7;
                        }
                    } else {
                        return v0_5;
                    }
                } else {
                    return v0_3;
                }
            } else {
                return v0_32;
            }
        } else {
            return v0_10;
        }
    }
    public static String mk(String p2, String p3, String p4)
    {
        try {
            if (!p2.toLowerCase().contains(p3.toLowerCase())) {
                return null;
            } else {
                return p4;
            }
        } catch (Throwable __e_v0) {
            return null;
        }
    }
    /**
     * 群聊发信人：MessagingStyle 的最后一条消息带 sender，且 sender != 通知标题（标题是群名）
     * 时才返回发信人名字；单聊（标题就是对方名字）不加前缀。
     */
    private static String senderOf(android.app.Notification n, String title, String text)
    {
        try {
            if (n == null) return null;
            android.os.Bundle ex = n.extras;
            if (ex == null) return null;
            // MessagingStyle 会把每条消息塞进 extras["android.messages"]（Bundle 数组），
            // 用这个比 getStyle() 稳（后者不是公开 SDK 方法）
            android.os.Parcelable[] arr = ex.getParcelableArray("android.messages");
            if ((arr == null) || (arr.length == 0)) return null;
            Object last = arr[arr.length - 1];
            if (!(last instanceof android.os.Bundle)) return null;
            android.os.Bundle b = (android.os.Bundle) last;
            String sender = null;
            try {
                Object o = b.getParcelable("sender_person");
                if (o instanceof android.app.Person) {
                    CharSequence nm = ((android.app.Person) o).getName();
                    if (nm != null) sender = nm.toString();
                }
            } catch (Throwable ignore) {
            }
            if (sender == null) {
                CharSequence cs = b.getCharSequence("sender");
                if (cs != null) sender = cs.toString();
            }
            if ((sender == null) || (sender.length() == 0) || (sender.length() > 20)) return null;
            // 自己发的就别加前缀
            try {
                Object me = ex.getParcelable("android.messagingUser");
                if (me instanceof android.app.Person) {
                    CharSequence mn = ((android.app.Person) me).getName();
                    if ((mn != null) && sender.equals(mn.toString())) return null;
                }
            } catch (Throwable ignore) {
            }
            // 单聊：通知标题就是对方名字 → 不用加
            if ((title != null) && (title.length() > 0) && title.contains(sender)) return null;
            // 正文已经带了"某某："
            if ((text != null) && text.startsWith(sender)) return null;
            return sender;
        } catch (Throwable __e) {
            return null;
        }
    }

    public static String msgOf(android.app.Notification p10)
    {
        java.lang.String v9_0 = null;
        try {
            if (p10 != null) {
                var v0 = p10.extras;
                if (v0 != null) {
                    var v1_1 = v0.getCharSequence("android.text");
                    var v2_1 = v0.getCharSequence("android.bigText");
                    var v3_1 = v0.getCharSequence("android.subText");
                    var v4_1 = v0.getCharSequence("android.title");
                    var v5_1 = new StringBuilder();
                    v5_1.append("Sug: keys text=");
                    v5_1.append(v1_1);
                    v5_1.append(" big=");
                    v5_1.append(v2_1);
                    v5_1.append(" sub=");
                    v5_1.append(v3_1);
                    v5_1.append(" title=");
                    v5_1.append(v4_1);
                    com.oupao.wiz.SLog.i(v5_1.toString());
                    if (v1_1 == null) {
                        if (v2_1 == null) {
                            if (v3_1 == null) {
                                if (v4_1 != null) {
                                    v9_0 = v4_1.toString();
                                }
                            } else {
                                v9_0 = v3_1.toString();
                            }
                        } else {
                            v9_0 = v2_1.toString();
                        }
                    } else {
                        v9_0 = v1_1.toString();
                    }
                }
            }
        } catch (Throwable __e_v0) {
            return null;
        }
        CharSequence titleCs0 = (p10 == null || p10.extras == null) ? null : p10.extras.getCharSequence("android.title");
        String title0 = (titleCs0 == null) ? null : titleCs0.toString();
        String out = com.oupao.wiz.Sug.cleanWithTitle(v9_0 == null ? null : v9_0.toString(), title0);
        try {
            String title = title0;
            String sender = com.oupao.wiz.Sug.senderOf(p10, title, out);
            if ((sender != null) && (out != null) && (out.length() > 0)) {
                com.oupao.wiz.SLog.i("Sug: 群聊发信人=" + sender + " title=" + title);
                out = sender + ": " + out;
            }
        } catch (Throwable ignore) {
        }
        return out;
    }
    public static void noteChat(Object p6)
    {
        if ((p6 instanceof android.content.Intent)) {
            var v1 = ((android.content.Intent) p6).getComponent();
            if (v1 != null) {
                var v2 = v1.getPackageName();
                if (v2 != null) {
                    java.lang.String v3 = null;
                    if (!"com.tencent.mm".equals(v2)) {
                        if (!"com.tencent.mobileqq".equals(v2)) {
                            return;
                        } else {
                            v3 = ((android.content.Intent) p6).getStringExtra("key_peerUin");
                            if (v3 == null) {
                                v3 = ((android.content.Intent) p6).getStringExtra("uin");
                            }
                        }
                    } else {
                        v3 = ((android.content.Intent) p6).getStringExtra("Chat_User");
                    }
                    if ((v3 == null) || (v3.length() <= 0)) {
                        com.oupao.wiz.Sug.curChat = null;
                        com.oupao.wiz.Sug.curPkg = null;
                    } else {
                        com.oupao.wiz.Sug.curPkg = v2;
                        com.oupao.wiz.Sug.curChat = v3;
                        var v4_10 = new StringBuilder();
                        v4_10.append("Sug: curChat=");
                        v4_10.append(v3);
                        com.oupao.wiz.SLog.i(v4_10.toString());
                        return;
                    }
                }
            }
        }
        return;
    }
    public static void noteChatId(String p2, String p3)
    {
        if ((p3 != null) && ((p3.length() > 0) && (p2 != null))) {
            com.oupao.wiz.Sug.curPkg = p2;
            com.oupao.wiz.Sug.curChat = p3;
            var v0_3 = new StringBuilder();
            v0_3.append("Sug: curChat=");
            v0_3.append(p3);
            com.oupao.wiz.SLog.i(v0_3.toString());
        }
        return;
    }
    public static boolean notifOn(String p5, int p6, int p7)
    {
        int v0_0 = 1;
        android.content.Context v1_0 = null;
        try {
            v1_0 = com.oupao.wiz.Sug.ctx;
        } catch (Throwable __e_v1) {
            return true;
        }
        if (v1_0 != null) {
            var v1_2 = v1_0.getSystemService("appops");
            if ((v1_2 instanceof android.app.AppOpsManager)) {
                if (p6 > 0) {
                    var v1_1 = com.oupao.wiz.FrameworkExt.appOpsCheck(v1_2, 11, p6, p5);
                    if (v1_1 == 0) {
                        v0_0 = 1;
                    } else {
                        v0_0 = 0;
                    }
                    var v2_3 = new StringBuilder();
                    v2_3.append("Sug: notif ");
                    v2_3.append(p5);
                    v2_3.append(" mode=");
                    v2_3.append(v1_1);
                    v2_3.append(" on=");
                    v2_3.append(v0_0);
                    v2_3.append(" uid=");
                    v2_3.append(p6);
                    v2_3.append(" u=");
                    v2_3.append(p7);
                    com.oupao.wiz.SLog.v(v2_3.toString());
                }
            } else {
            }
        } else {
        }
        return v0_0 != 0;
    }
    public static int notifUserId()
    {
        int v0_0 = 0;
        try {
            v0_0 = com.oupao.wiz.Sug.uid;
        } catch (Throwable __e_v0) {
            return 0;
        }
        if (v0_0 != 0) {
            if (v0_0 >= 10000) {
                if (v0_0 >= 100000) {
                    return (v0_0 / 100000);
                }
            } else {
                return v0_0;
            }
        }
        return 0;
    }
    public static void onBt()
    {
        try {
            // 诊断：把所有相关状态一次性打出，一眼看出卡在哪里
            String dbg;
            try {
                dbg = "Bt: 状态 master=" + com.oupao.wiz.Cfg.masterOn()
                        + " sug=" + com.oupao.wiz.Cfg.sugMaster()
                        + " music=" + com.oupao.wiz.Cfg.getInt("oupao_sug_music", 1)
                        + " guard=" + com.oupao.wiz.Sug.btGuard
                        + " now=" + System.currentTimeMillis()
                        + " ctx=" + (com.oupao.wiz.Sug.ctx != null)
                        + " pkg=[" + com.oupao.wiz.Cfg.getStr("oupao_music_pkg", "") + "]"
                        + " list=[" + com.oupao.wiz.Cfg.getStr("oupao_music_list", "") + "]";
            } catch (Throwable ignore) {
                dbg = "Bt: 状态(读取失败)";
            }
            com.oupao.wiz.SLog.i(dbg);

            if (System.currentTimeMillis() < com.oupao.wiz.Sug.btGuard) {
                return;
            }
            if (!com.oupao.wiz.Cfg.sugMusic()) {
                return;
            }
            var v0_2 = com.oupao.wiz.Sug.ctx;
            if (v0_2 == null) {
                return;
            }
            var v1 = com.oupao.wiz.Mus.pick();
            if (v1 == null) {
                com.oupao.wiz.SLog.i("Bt: pick() 返回 null");
                return;
            }
            var v2_0 = v0_2.getPackageManager();
            if (v2_0.getLaunchIntentForPackage(v1) == null) {
                com.oupao.wiz.SLog.i("Bt: " + v1 + " 无启动入口");
                return;
            }
            var v2_1 = v2_0.getApplicationIcon(v1);
            android.graphics.Bitmap v3_1 = null;
            if (v2_1 != null) {
                var v4_1 = com.oupao.wiz.Ui.dp(v0_2, 96);
                v3_1 = com.oupao.wiz.Sug.iconRound(com.oupao.wiz.Sug.iconOf(v2_1, v4_1), v4_1);
            }
            var v4_4 = new StringBuilder();
            v4_4.append("Bt: 弹音乐气泡 ");
            v4_4.append(v1);
            com.oupao.wiz.SLog.i(v4_4.toString());
            com.oupao.wiz.Sug.showMusic(v1, v3_1);
        } catch (Throwable __e_v0) {
            com.oupao.wiz.SLog.e("Bt: onBt 异常", __e_v0);
            return;
        }
        return;
    }

    public static void onClip(String p2)
    {
        try {
            if (!p2.contains("http")) {
                if (com.oupao.wiz.Cfg.wordOn()) {
                    var v0_4 = com.oupao.wiz.Sug.pickPkg(p2);
                    if (v0_4 != null) {
                        android.content.Context v1_0 = com.oupao.wiz.Sug.ctx;
                        if (v1_0 == null) {
                            v1_0 = com.oupao.wiz.Ctx.get();
                        } else {
                        }
                        if (v1_0 != null) {
                            com.oupao.wiz.Sug.ctx = v1_0;
                            com.oupao.wiz.Sug.bubbleFor(v1_0, v0_4, p2);
                        }
                    }
                }
                return;
            } else {
                com.oupao.wiz.Sug.onClipOld(p2);
                return;
            }
        } catch (Throwable __e_v0_2) {
            com.oupao.wiz.SLog.e("Clip: onClip \u5f02\u5e38", __e_v0_2);
            return;
        }
    }
    public static void onClipOld(String p8)
    {
        try {
            if (!p8.contains("http")) {
                if (p8.contains("\u6296\u97f3")) {
                    android.content.Context v1_0 = com.oupao.wiz.Sug.ctx;
                    if (v1_0 != null) {
                        v1_0 = com.oupao.wiz.Ctx.get();
                        if (v1_0 != null) {
                            com.oupao.wiz.SLog.i("Clip: \u53e3\u4ee4\u547d\u4e2d\u4f46 ctx \u4e3a\u7a7a, \u653e\u5f03");
                            return;
                        }
                    } else {
                    }
                    var v2_5 = new StringBuilder();
                    v2_5.append("Clip: \u53e3\u4ee4 -> \u6296\u97f3\u6c14\u6ce1 len=");
                    v2_5.append(p8.length());
                    com.oupao.wiz.SLog.i(v2_5.toString());
                    com.oupao.wiz.SLog.flushNow();
                    com.oupao.wiz.Sug.word(v1_0, p8);
                    return;
                }
            } else {
            }
        } catch (Throwable __e_v0) {
            return;
        }
        if (com.oupao.wiz.Cfg.sugLink()) {
            var v1_2 = com.oupao.wiz.Sug.ctx;
            if (v1_2 != null) {
                var v3_2 = com.oupao.wiz.Sug.extUrl(p8);
                if (v3_2 != null) {
                    var v4_0 = v1_2.getPackageManager();
                    java.lang.String v2_7 = com.oupao.wiz.Sug.mapPkg(p8);
                    int v5 = 0;
                    if ((v2_7 == null) || (v4_0.getLaunchIntentForPackage(v2_7) == null)) {
                        v5 = 1;
                        if (com.oupao.wiz.Cfg.sugBrowser()) {
                            v2_7 = com.oupao.wiz.Sug.browserPkg(v4_0);
                            if (v2_7 != null) {
                                java.lang.String v6_2 = "Link: \u68c0\u6d4b\u5230\u94fe\u63a5 -> ";
                                if (v5 != 0) {
                                    v6_2 = "Link: \u65e0\u5339\u914d App -> \u6d4f\u89c8\u5668 ";
                                }
                                com.oupao.wiz.SLog.i(new StringBuilder().append(v6_2).append(v2_7).append(" ").append(v3_2).toString());
                                try {
                                    var v4_1 = v4_0.getApplicationIcon(v2_7);
                                    android.graphics.Bitmap v6_4 = null;
                                    if (v4_1 != null) {
                                        var v7_8 = com.oupao.wiz.Ui.dp(v1_2, 96);
                                        v6_4 = com.oupao.wiz.Sug.iconRound(com.oupao.wiz.Sug.iconOf(v4_1, v7_8), v7_8);
                                    }
                                    com.oupao.wiz.Sug.showLink(v2_7, v3_2, v6_4);
                                } catch (Throwable __e_io) {
                                }
                            }
                        }
                    } else {
                    }
                    // 1820fix: 匹配到已安装应用时也要弹气泡（此前直接 return，所以复制链接不弹）
                    if (v5 == 0) {
                        try {
                            var dAp = v4_0.getApplicationIcon(v2_7);
                            android.graphics.Bitmap bmAp = null;
                            if (dAp != null) {
                                int szAp = com.oupao.wiz.Ui.dp(v1_2, 96);
                                bmAp = com.oupao.wiz.Sug.circleFitIcon(dAp, szAp);
                            }
                            com.oupao.wiz.SLog.i("Clip: 已匹配应用 -> 弹气泡 pkg=" + v2_7 + " url=" + v3_2);
                            com.oupao.wiz.Sug.showLink(v2_7, v3_2, bmAp);
                        } catch (Throwable tbAp) {
                            com.oupao.wiz.SLog.e("Clip: 匹配应用弹气泡异常", tbAp);
                        }
                    }
                    return;

                } else {
                    // 只打长度，不打原文：/data/system/oupao.log 累积不截断，
                    // 写剪贴板内容等于长期留存用户隐私。
                    // （原来这里是 substring(0,40)，且长度不足 40 时会抛
                    //   StringIndexOutOfBoundsException —— v2_9 被设成 40 而非取 min。）
                    var v0_12 = new StringBuilder();
                    v0_12.append("Clip: \u6ca1\u8bc6\u522b\u51fa\u94fe\u63a5, \u653e\u5f03 len=");
                    v0_12.append(p8.length());
                    com.oupao.wiz.SLog.i(v0_12.toString());
                    return;
                }
            } else {
                var v0_2 = new StringBuilder();
                v0_2.append("Clip: ctx \u4e3a\u7a7a, \u653e\u5f03 len=");
                v0_2.append(p8.length());
                com.oupao.wiz.SLog.i(v0_2.toString());
                return;
            }
        } else {
            var v0_5 = new StringBuilder();
            v0_5.append("Clip: \u94fe\u63a5\u6c14\u6ce1\u5f00\u5173=\u5173, \u653e\u5f03 len=");
            v0_5.append(p8.length());
            com.oupao.wiz.SLog.i(v0_5.toString());
            return;
        }
    }
    public static void onHookPkg(Object[] p5)
    {
        try {
            com.oupao.wiz.SLog.i("Inst: hookPkg");
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.SLog.e("Inst: hookPkg \u5f02\u5e38", __e_v0_1);
            return;
        }
        // 2918 审计第 5 项：包变化 → 引索条的应用枚举缓存失效（装上/卸掉/改名/换图标都要重扫）。
        // 这里 hook 的是 system_server 的 sendPackageBroadcast，是「装/卸包」最省事的事件源。
        try {
            if ((p5 != null) && (p5.length >= 1) && (p5[0] instanceof String)) {
                String act = (String) p5[0];
                if (act.startsWith("android.intent.action.PACKAGE_")
                        || "android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE".equals(act)
                        || "android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE".equals(act)) {
                    com.oupao.wiz.IdxBar.invalidateApps();
                }
            }
        } catch (Throwable tInv) {
            com.oupao.wiz.SLog.e("Inst: 应用缓存失效失败", tInv);
        }
        if (p5 != null) {
            var v0_0 = p5.length;
            if (v0_0 >= 2) {
                var v2_2 = p5[0];
                if (((v2_2 instanceof String)) && ("android.intent.action.PACKAGE_ADDED".equals(v2_2))) {
                    var v2_0 = p5[1];
                    if ((v2_0 instanceof String)) {
                        var v2_1 = ((String) v2_0);
                        if ((com.oupao.wiz.Cfg.sugInstall()) && (!"com.oupao.wiz".equals(v2_1))) {
                            if (v0_0 >= 2) {
                                var v1_7 = p5[2];
                                if ((v1_7 instanceof android.os.Bundle)) {
                                    var v1_8 = ((android.os.Bundle) v1_7);
                                    if ((v1_8 != null) && (v1_8.getBoolean("android.intent.extra.REPLACING"))) {
                                        return;
                                    }
                                }
                            }
                            com.oupao.wiz.Sug.installSoon(v2_1);
                        }
                    }
                }
            }
        }
        return;
    }
    public static void onNotify(String p7, android.app.Notification p8, int p9, int p10)
    {
        try {
            com.oupao.wiz.SLog.i("Sug.onNotify enter pkg=" + p7 + " id=" + p9 + " uid=" + p10
                    + " ctx=" + (com.oupao.wiz.Sug.ctx != null)
                    + " on=" + com.oupao.wiz.Sug.notifOn(p7, p9, p10));
        } catch (Throwable ignore) {
        }
        com.oupao.wiz.Sug.uid = p10;
        if (com.oupao.wiz.Sug.notifOn(p7, p9, p10)) {
            try {
                if (((p8.flags & 2) == 0) || (com.oupao.wiz.Sug.isCall(p8))) {
                    if ((com.oupao.wiz.Sug.uid / 100000) == 0) {
                        // 气泡开关：统一走「总开关 oupao_sug_msg + 白名单 oupao_bubble_apps」，
                        // 微信/QQ 不再有专用开关（用户要求改成全局 + 自定义应用）。
                        boolean v0_8 = com.oupao.wiz.Cfg.bubbleAppOn(p7);
                        com.oupao.wiz.SLog.i("Sug.onNotify bubbleAppOn=" + v0_8 + " pkg=" + p7);
                        if (v0_8) {
                            var v0_9 = com.oupao.wiz.Sug.ctx;
                            if (v0_9 != null) {
                                android.graphics.Bitmap v1_1 = null;
                                com.oupao.wiz.Sug.dumpNotif(p8);
                                if ((com.oupao.wiz.Sug.isCall(p8)) || (!com.oupao.wiz.Sug.sameChat(p7))) {
                                    android.graphics.drawable.Drawable v2_1 = null;
                                    int v4_0 = 0;
                                    var v2_0 = p8.getLargeIcon();
                                    if (v2_0 == null) {
                                        v2_1 = v0_9.getPackageManager().getApplicationIcon(p7);
                                    } else {
                                        v2_1 = v2_0.loadDrawable(v0_9);
                                        if (v2_1 == null) {
                                        } else {
                                            v4_0 = 1;
                                        }
                                    }
                                    if (v2_1 != null) {
                                        var v3_1 = com.oupao.wiz.Ui.dp(v0_9, 96);
                                        v1_1 = com.oupao.wiz.Sug.iconRound(com.oupao.wiz.Sug.iconOf(v2_1, v3_1), v3_1);
                                    }
                                    int v6_2 = 0;
                                    var v5_6 = new StringBuilder().append("Sug: avatar head=").append(v4_0).append(" w=");
                                    if (v1_1 == null) {
                                        v6_2 = -1;
                                    } else {
                                        v6_2 = v1_1.getWidth();
                                    }
                                    com.oupao.wiz.SLog.i(v5_6.append(v6_2).toString());
                                    var v3_2 = com.oupao.wiz.Sug.msgOf(p8);
                                    var v4_2 = new StringBuilder();
                                    v4_2.append("Sug: msg=");
                                    v4_2.append(v3_2);
                                    com.oupao.wiz.SLog.i(v4_2.toString());
                                    com.oupao.wiz.Sug.show2(p7, v3_2, v1_1);
                                } else {
                                    com.oupao.wiz.SLog.k("Sug: same chat page -> skip bubble");
                                    return;
                                }
                            }
                        }
                        return;
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            } catch (Throwable __e_v0_13) {
                com.oupao.wiz.SLog.e("Sug: \u901a\u77e5\u5904\u7406\u5f02\u5e38", __e_v0_13);
                return;
            }
        } else {
            com.oupao.wiz.SLog.k("Sug: notif off -> skip bubble");
            return;
        }
    }
    public static void onPkg(android.content.Intent p7)
    {
        try {
            com.oupao.wiz.SLog.i("Inst: onPkg enter");
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.SLog.e("Inst: onPkg \u5f02\u5e38", __e_v0_1);
            return;
        }
        if (p7 != null) {
            var v0_0 = com.oupao.wiz.Cfg.sugInstall();
            var v1_2 = p7.getBooleanExtra("android.intent.extra.REPLACING", false);
            var v2_0 = p7.getDataString();
            var v3_1 = new StringBuilder();
            v3_1.append("Inst: on=");
            v3_1.append(v0_0);
            v3_1.append(" rep=");
            v3_1.append(v1_2);
            v3_1.append(" data=");
            v3_1.append(v2_0);
            com.oupao.wiz.SLog.i(v3_1.toString());
            if ((v0_0) && ((!v1_2) && ((v2_0 != null) && (!v2_0.startsWith("com.oupao.wiz"))))) {
                java.lang.String v5 = null;
                if (v2_0.startsWith("package:")) {
                    v5 = v2_0.substring(8);
                } else {
                    v5 = v2_0;
                }
                if (v5 != null) {
                    com.oupao.wiz.Sug.installSoon(v5);
                }
            }
        }
        return;
    }
    public static void onPolicy()
    {
        java.lang.Object v0 = null;
        try {
            v0 = com.oupao.wiz.Sug.pg;
        } catch (Throwable __e_v9) {
            if (!com.oupao.wiz.Sug.errd) {
                com.oupao.wiz.Sug.errd = true;
                com.oupao.wiz.SLog.e("DimDbg: onPolicy\u5f02\u5e38", __e_v9);
            }
            return;
        }
        if (v0 != null) {
            if (!com.oupao.wiz.Sug.dbg) {
                com.oupao.wiz.Sug.dbg = true;
                com.oupao.wiz.SLog.i("DimDbg: \u5df2\u62ff\u5230 PowerGroup");
            }
            java.lang.reflect.Method v4 = com.oupao.wiz.Sug.pm;
            if (v4 == null) {
                v4 = com.oupao.wiz.Rt.methodByName(v0.getClass(), "getPolicyLocked", 0);
                if (v4 == null) {
                    return;
                } else {
                    com.oupao.wiz.Sug.pm = v4;
                }
            }
            try {
                var v5 = ((Integer) com.oupao.wiz.Rt.invoke(v4, v0, null)).intValue();
                if (v5 != com.oupao.wiz.Sug.pol) {
                    com.oupao.wiz.Sug.pol = v5;
                    com.oupao.wiz.Sug.lg("DimDbg: policy=", v5);
                }
                if (v5 != 2) {
                    if (com.oupao.wiz.Sug.dim) {
                        com.oupao.wiz.Sug.dim = false;
                        if ((com.oupao.wiz.Sug.dimLocked) || (com.oupao.wiz.Sug.lockNow())) {
                            com.oupao.wiz.SLog.i("DimDbg: \u534a\u6697\u53d1\u751f\u5728\u9501\u5c4f/\u5f53\u524d\u4ecd\u9501\u5c4f, \u4e0d\u5efa\u8bae\u4e34\u65f6\u5e38\u4eae");
                            return;
                        } else {
                            if ((v5 == 3) && (com.oupao.wiz.Cfg.appKeep())) {
                                com.oupao.wiz.SLog.i("DimDbg: \u534a\u6697\u4e2d\u70b9\u4eae\u5c4f\u5e55 \u2192 \u5f39");
                                com.oupao.wiz.Sug.txt = "\u2600";
                                com.oupao.wiz.Sug.bmp = null;
                                var v6_12 = com.oupao.wiz.Sug.main;
                                if (v6_12 == null) {
                                    com.oupao.wiz.Sug.show(3);
                                    return;
                                } else {
                                    v6_12.post(new com.oupao.wiz.Sug$K());
                                    return;
                                }
                            }
                        }
                    }
                    return;
                } else {
                    com.oupao.wiz.Sug.dim = true;
                    com.oupao.wiz.Sug.dimLocked = com.oupao.wiz.Sug.lockNow();
                    return;
                }
            } catch (Throwable __e_io) {
            }
        } else {
            if (!com.oupao.wiz.Sug.dbg) {
                com.oupao.wiz.Sug.dbg = true;
                com.oupao.wiz.SLog.i("DimDbg: pg\u4e3a\u7a7a(\u94a9\u5b50\u672a\u89e6\u53d1)");
            }
        }
        return;
    }
    public static void onScreenOff()
    {
        android.os.PowerManager.WakeLock v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Sug.keepWL;
        } catch (Throwable __e_v0) {
            return;
        }
        if ((v0_0 != null) && (v0_0.isHeld())) {
            v0_0.release();
        }
        com.oupao.wiz.Sug.keepActive = false;
        com.oupao.wiz.Sug.keepSrc = 0;
        com.oupao.wiz.Sug.dismiss();
        com.oupao.wiz.SLog.i("Sug: \u606f\u5c4f\uff0c\u5df2\u6062\u590d");
        return;
    }
    public static void onTorch(String p4, boolean p5)
    {
        boolean was;
        try {
            was = com.oupao.wiz.Sug.torchOn;
            com.oupao.wiz.Sug.torchId = p4;
            com.oupao.wiz.Sug.torchOn = p5;
        } catch (Throwable __e_v0) {
            return;
        }
        if (!p5) {
            // 3320：关灯 → 撤掉还没弹的 5 秒任务；正在显示的手电筒气泡立刻收掉
            cancelTorchFire();
            if (com.oupao.wiz.Sug.kind == 1) {
                com.oupao.wiz.Sug.dismiss();
            }
        } else {
            // 3320：系统可能把同一个「已开灯」状态重复回调（相机重连/重新注册等）。
            //   之前每回调一次就排一次 5 秒任务 → 气泡反复弹、看起来"不消失"。
            //   只认 off → on 这一次。
            if (was) {
                return;
            }
            if (com.oupao.wiz.Cfg.sugTorch()) {
                com.oupao.wiz.Sug.pkg = null;
                com.oupao.wiz.Sug.kPkg = null;
                com.oupao.wiz.Sug.txt = "\ud83d\udd26";
                com.oupao.wiz.Sug.bmp = null;
                var v1_2 = com.oupao.wiz.Sug.ctx;
                if (v1_2 != null) {
                    com.oupao.wiz.Sug.bmp = com.oupao.wiz.Sug.circle(com.oupao.wiz.Sug.torchIcon(com.oupao.wiz.Ui.dp(v1_2, 46)), com.oupao.wiz.Ui.dp(v1_2, 46));
                }
                var v0_0 = com.oupao.wiz.Sug.main;
                if (v0_0 != null) {
                    cancelTorchFire();
                    final java.lang.Runnable r = new com.oupao.wiz.Sug$Torch();
                    com.oupao.wiz.Sug.torchPend = r;
                    v0_0.postDelayed(r, 5000);
                } else {
                    com.oupao.wiz.Sug.show(1);
                }
            }
        }
        return;
    }
    /** 3320：撤掉还没执行的「手电筒 5 秒延时任务」 */
    static void cancelTorchFire()
    {
        try {
            java.lang.Runnable r = com.oupao.wiz.Sug.torchPend;
            com.oupao.wiz.Sug.torchPend = null;
            if ((r != null) && (com.oupao.wiz.Sug.main != null)) {
                com.oupao.wiz.Sug.main.removeCallbacks(r);
            }
        } catch (Throwable ignore) {
        }
    }
    public static void onWebUrl(String p7)
    {
        try {
            if (com.oupao.wiz.Cfg.sugWeb()) {
                // 3359+：tb.cn 短链也走这里弹气泡（外部用户：m.tb.cn 不弹、s.click.taobao 弹）。
                // 只跳过「我们自己刚发起过的同一个 URL」，不再整段跳过 tb.cn。
                if (com.oupao.wiz.Idle.suppressWebTip(p7)) {
                    return;
                }
                // 已经在淘宝/闲鱼里点链接 → 不再建议打开自己
                String fg = com.oupao.wiz.Sug.fgPkg();
                if ("com.taobao.taobao".equals(fg) || "com.taobao.idlefish".equals(fg)) {
                    return;
                }
                var v0_1 = com.oupao.wiz.Sug.mapPkg(p7);
                if (v0_1 != null) {
                    if (!com.oupao.wiz.Sug.linkDup(v0_1)) {
                        var v1_2 = com.oupao.wiz.Sug.ctx;
                        if (v1_2 != null) {
                            var v2 = v1_2.getPackageManager();
                            if (v2.getLaunchIntentForPackage(v0_1) != null) {
                                android.graphics.Bitmap v4_0 = null;
                                var v5_0 = v2.getApplicationIcon(v0_1);
                                if (v5_0 != null) {
                                    var v6_1 = com.oupao.wiz.Ui.dp(v1_2, 96);
                                    v4_0 = com.oupao.wiz.Sug.iconRound(com.oupao.wiz.Sug.iconOf(v5_0, v6_1), v6_1);
                                }
                                var v5_2 = new StringBuilder();
                                v5_2.append("Web: \u5339\u914d\u5230 ");
                                v5_2.append(v0_1);
                                com.oupao.wiz.SLog.i(v5_2.toString());
                                com.oupao.wiz.Sug.showLink(v0_1, p7, v4_0);
                            }
                        }
                    } else {
                        com.oupao.wiz.SLog.i("Web: \u521a\u4ece\u5c0f\u7a97\u6253\u5f00\u8fc7, \u8df3\u8fc7\u91cd\u590d\u6c14\u6ce1");
                        return;
                    }
                } else {
                    // 3359+：没匹配到任何 App 时打出 host，下次淘宝再换域名能直接从日志补 mapPkg
                    String host = "";
                    try {
                        host = android.net.Uri.parse(p7).getHost();
                    } catch (Throwable ignore) {
                    }
                    com.oupao.wiz.SLog.r("Web", "Web: \u672a\u5339\u914d host=" + host, 3000L);
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static int opaque(android.graphics.Bitmap p8)
    {
        int v0 = 0;
        try {
            if (p8 != null) {
                var v1 = p8.getWidth();
                var v2 = p8.getHeight();
                int v3 = 0;
                while (v3 < v2) {
                    int v4 = 0;
                    while (v4 < v1) {
                        if ((p8.getPixel(v4, v3) >>> 24) > 8) {
                            v0++;
                        }
                        v4 += 32;
                    }
                    v3 += 32;
                }
            }
        } catch (Throwable __e_v1) {
            return v0;
        }
        return v0;
    }
    public static boolean openAsUser()
    {
        android.content.Intent v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Sug.ni;
        } catch (Throwable __e_v0) {
            return false;
        }
        if (v0_0 != null) {
            v0_0.addFlags(268435456);
            var v1_1 = com.oupao.wiz.Sug.ctx;
            if (v1_1 != null) {
                var v2 = com.oupao.wiz.Sug.notifUserId();
                if (v2 == 0) {
                    v1_1.startActivity(v0_0);
                    return true;
                } else {
                    return com.oupao.wiz.Sug.openCrossUser(v2);
                }
            }
        }
        return false;
    }
    public static boolean openCrossUser(int p3)
    {
        android.content.Intent v0_0 = null;
        android.content.Context v1_0 = null;
        try {
            v0_0 = com.oupao.wiz.Sug.ni;
            v1_0 = com.oupao.wiz.Sug.ctx;
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.SLog.e("Launch: cross-user open failed", __e_v0_1);
            return false;
        }
        if ((v0_0 == null) || (v1_0 == null)) {
            return false;
        } else {
            com.oupao.wiz.FrameworkExt.startActivityAsUser(v1_0, v0_0, com.oupao.wiz.FrameworkExt.userHandleOf(p3));
            return true;
        }
    }
    private static void openSmall(String p4)
    {
        try {
            if (p4 != null) {
                android.content.Context v2 = com.oupao.wiz.Sug.ctx;
                if (v2 == null) {
                    v2 = com.oupao.wiz.Ctx.get();
                    if (v2 == null) {
                        v2 = com.oupao.wiz.Cfg.sysContext();
                    } else {
                    }
                } else {
                }
                if (v2 != null) {
                    android.content.Intent v0_0 = v2.getPackageManager().getLaunchIntentForPackage(p4);
                    if (v0_0 == null) {
                        v0_0 = new android.content.Intent("android.intent.action.MAIN").addCategory("android.intent.category.LAUNCHER").setPackage(p4);
                    } else {
                    }
                    com.oupao.wiz.SLog.i("Clip: \u53e3\u4ee4\u6c14\u6ce1\u5c0f\u7a97\u6253\u5f00");
                    com.oupao.wiz.FlyH.go(v0_0, p4);
                }
            }
        } catch (Throwable __e_v0_4) {
            com.oupao.wiz.SLog.e("Clip: openSmall \u5f02\u5e38", __e_v0_4);
            return;
        }
        return;
    }
    public static boolean piBoundsSend()
    {
        android.app.PendingIntent v0 = null;
        try {
            v0 = com.oupao.wiz.Sug.pi;
        } catch (Throwable __e_v2_6) {
            com.oupao.wiz.SLog.e("piBoundsSend error", __e_v2_6);
            return false;
        }
        if (v0 != null) {
            var v1 = com.oupao.wiz.Sug.ctx;
            if (v1 != null) {
                var v2_9 = v1.getResources().getDisplayMetrics();
                var v3_0 = v2_9.widthPixels;
                var v4_0 = v2_9.heightPixels;
                var v5_1 = ((v3_0 * 85) / 100);
                var v6_1 = ((v4_0 * 85) / 100);
                var v7_1 = ((v3_0 - v5_1) / 2);
                var v8_1 = ((v4_0 - v6_1) / 2);
                boolean sent = com.oupao.wiz.FrameworkExt.pendingIntentSendFull(v0, v1, 0, null, null, null, null, android.app.ActivityOptions.makeBasic().setLaunchBounds(new android.graphics.Rect(v7_1, v8_1, (v7_1 + v5_1), (v8_1 + v6_1))).toBundle());
                com.oupao.wiz.SLog.i("Launch: msg PI bounds sent=" + sent);
                return sent;
            }
        }
        return false;
    }
    public static boolean piNativeSend()
    {
        android.app.PendingIntent v0 = null;
        try {
            v0 = com.oupao.wiz.Sug.pi;
        } catch (Throwable __e_v0) {
            return false;
        }
        if (v0 != null) {
            var v1 = com.oupao.wiz.Sug.ctx;
            if (v1 != null) {
                boolean sent = com.oupao.wiz.FrameworkExt.pendingIntentSend(v0, v1);
                com.oupao.wiz.Sug.pi = null;
                com.oupao.wiz.SLog.i("Launch: piNativeSend sent=" + sent);
                return sent;
            }
        }
        return false;
    }
    public static boolean piSendZoom()
    {
        android.app.PendingIntent v0 = null;
        try {
            v0 = com.oupao.wiz.Sug.pi;
        } catch (Throwable __e_v0) {
            return false;
        }
        if (v0 != null) {
            var v1 = com.oupao.wiz.Sug.ctx;
            if (v1 != null) {
                var v2_1 = android.app.ActivityOptions.makeBasic().setLaunchDisplayId(0).toBundle();
                v2_1.putInt("extra_window_mode", 100);
                v2_1.putInt("android:activity.windowingMode", 100);
                boolean sent = com.oupao.wiz.FrameworkExt.pendingIntentSendFull(v0, v1, 0, null, null, null, null, v2_1);
                com.oupao.wiz.Sug.pi = null;
                com.oupao.wiz.SLog.i("Launch: piSendZoom sent=" + sent);
                return sent;
            }
        }
        return false;
    }
    private static String pickPkg(String p2)
    {
        try {
            if (p2 != null) {
                if (!p2.contains("\u6296\u97f3")) {
                    if (!p2.contains("\u6dd8\u5b9d")) {
                        if (!p2.contains("\u6dd8\u53e3\u4ee4")) {
                            if (!p2.contains("\u4eac\u4e1c")) {
                                if (!p2.contains("\u62fc\u591a\u591a")) {
                                    if (!p2.contains("\u5c0f\u7ea2\u4e66")) {
                                        if (!p2.contains("\u5feb\u624b")) {
                                            if (!p2.contains("\u5fae\u535a")) {
                                                if (!p2.contains("\u95f2\u9c7c")) {
                                                    if (!p2.contains("\u54d4\u54e9\u54d4\u54e9")) {
                                                        if (!p2.contains("B\u7ad9")) {
                                                            if (p2.contains("\u5938\u514b")) {
                                                                return "com.quark.browser";
                                                            }
                                                        } else {
                                                            return "tv.danmaku.bili";
                                                        }
                                                    } else {
                                                        return "tv.danmaku.bili";
                                                    }
                                                } else {
                                                    return "com.taobao.idlefish";
                                                }
                                            } else {
                                                return "com.sina.weibo";
                                            }
                                        } else {
                                            return "com.smile.gifmaker";
                                        }
                                    } else {
                                        return "com.xingin.xhs";
                                    }
                                } else {
                                    return "com.xunmeng.pinduoduo";
                                }
                            } else {
                                return "com.jingdong.app.mall";
                            }
                        } else {
                            return "com.taobao.taobao";
                        }
                    } else {
                        return "com.taobao.taobao";
                    }
                } else {
                    return "com.ss.android.ugc.aweme";
                }
            }
        } catch (Throwable __e_v0) {
            return null;
        }
        return null;
    }
    static void postMain(Runnable p2)
    {
        try {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(p2);
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static void press()
    {
        try {
            if (com.oupao.wiz.Sug.prs) {
                com.oupao.wiz.Sug.prs = false;
                var v0_3 = com.oupao.wiz.Sug.view;
                if (v0_3 != null) {
                    com.oupao.wiz.Bar.press(v0_3, false);
                }
            } else {
                com.oupao.wiz.Sug.prs = true;
                var v0_1 = com.oupao.wiz.Sug.view;
                if (v0_1 != null) {
                    com.oupao.wiz.Bar.press(v0_1, true);
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static void regAudio()
    {
        android.content.Context v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Sug.ctx;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_0 != null) {
            var v0_2 = ((android.media.AudioManager) v0_0.getSystemService("audio"));
            if (v0_2 != null) {
                v0_2.registerAudioDeviceCallback(new com.oupao.wiz.BtCb(), com.oupao.wiz.Sug.main);
                com.oupao.wiz.SLog.i("Bt: \u84dd\u7259\u97f3\u9891\u76d1\u542c\u5df2\u6ce8\u518c");
            }
        }
        return;
    }
    public static void repostTick(com.oupao.wiz.Sug$Tick p4)
    {
        android.os.Handler v0 = null;
        try {
            v0 = com.oupao.wiz.Sug.main;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0 == null) {
            com.oupao.wiz.Sug.tickOn = false;
        } else {
            int v1_0 = 1000;
            if (!com.oupao.wiz.Cfg.appKeep()) {
                v1_0 = 2000;
            } else {
                if (com.oupao.wiz.Sug.dim) {
                    v1_0 = 250;
                }
            }
            v0.postDelayed(p4, ((long) v1_0));
        }
        return;
    }
    public static boolean sameChat(String p3)
    {
        if (com.oupao.wiz.Sug.sameChatId(p3)) {
            if (!com.oupao.wiz.Hooks.winOpen()) {
                var v1_1 = com.oupao.wiz.Hooks.focusedInfo();
                if (v1_1 != null) {
                    if (!v1_1.contains("chatting")) {
                        if (v1_1.contains("ChatActivity")) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            } else {
                return true;
            }
        }
        return false;
    }
    /** 类名看起来是不是聊天页（只用于日志/诊断；收气泡已不再依赖它） */
    public static boolean isChatPage(String cls)
    {
        if (cls == null) {
            return false;
        }
        String lc = cls.toLowerCase();
        return lc.contains("chatting") || lc.contains("chatactivity") || lc.contains("chatscreen");
    }

    public static boolean sameChatId(String p3)
    {
        var v0_0 = com.oupao.wiz.Sug.curChat;
        if (v0_0 != null) {
            var v1_3 = com.oupao.wiz.Sug.curPkg;
            if ((v1_3 != null) && (v1_3.equals(p3))) {
                java.lang.String v1_2 = null;
                if (!"com.tencent.mm".equals(p3)) {
                    if (!"com.tencent.mobileqq".equals(p3)) {
                        return false;
                    } else {
                        v1_2 = com.oupao.wiz.Sug.qqUin;
                    }
                } else {
                    v1_2 = com.oupao.wiz.Sug.chatUser;
                }
                if (v1_2 != null) {
                    return v1_2.equals(v0_0);
                }
            }
        }
        return false;
    }
    /**
     * 算不透明包围盒，结果写入 bbX1/bbY1/bbX2/bbY2。
     *
     * 性能：原来是逐点 `getPixel(x,y)`（步进 3），96dp@3x = 288px 时约 9216 次 JNI 调用，
     * 一次 10~30ms 纯 JNI，而它是 iconRound 的依赖 —— 气泡/通知/剪贴板/安装提示/蓝牙音乐
     * 所有图标路径的公共出口。改成批量 `getPixels` 一次取回 + 数组索引（与
     * {@link #circleFitIcon} 的做法一致），语义完全相同（同样步进 3、同样阈值 32）。
     */
    public static boolean scan(android.graphics.Bitmap p11)
    {
        int v0_0 = 0;
        int v1 = 0;
        int v2 = 0;
        try {
            v1 = p11.getWidth();
            v2 = p11.getHeight();
        } catch (Throwable __e_v9) {
            return false;
        }
        if ((v1 > 0) && (v2 > 0)) {
            int v3 = v1;
            int v4 = v2;
            int v5 = -1;
            int v6 = -1;
            int v7 = 0;
            int[] px = null;
            try {
                px = new int[v1 * v2];
                p11.getPixels(px, 0, v1, 0, 0, v1, v2);
            } catch (Throwable __e_px) {
                return false;
            }
            while (v7 < v2) {
                int row = (v7 * v1);
                int v8 = 0;
                while (v8 < v1) {
                    if ((px[row + v8] >>> 24) > 32) {
                        if (v8 < v3) {
                            v3 = v8;
                        }
                        if (v8 > v5) {
                            v5 = v8;
                        }
                        if (v7 < v4) {
                            v4 = v7;
                        }
                        if (v7 > v6) {
                            v6 = v7;
                        }
                    }
                    v8 += 3;
                }
                v7 += 3;
            }
            if (v5 >= 0) {
                com.oupao.wiz.Sug.bbX1 = v3;
                com.oupao.wiz.Sug.bbY1 = v4;
                com.oupao.wiz.Sug.bbX2 = v5;
                com.oupao.wiz.Sug.bbY2 = v6;
                v0_0 = 1;
            }
        }
        return v0_0 != 0;
    }
    private static String scheme(String p2)
    {
        try {
            if (p2 != null) {
                if (!p2.equals("com.ss.android.ugc.aweme")) {
                    if (!p2.equals("com.taobao.taobao")) {
                        if (!p2.equals("com.xingin.xhs")) {
                            if (!p2.equals("com.smile.gifmaker")) {
                                if (!p2.equals("com.xunmeng.pinduoduo")) {
                                    if (!p2.equals("com.jingdong.app.mall")) {
                                        if (!p2.equals("com.taobao.idlefish")) {
                                            if (!p2.equals("com.sina.weibo")) {
                                                if (!p2.equals("tv.danmaku.bili")) {
                                                    if (p2.equals("com.quark.browser")) {
                                                        return "quark://";
                                                    }
                                                } else {
                                                    return "bilibili://";
                                                }
                                            } else {
                                                return "sinaweibo://";
                                            }
                                        } else {
                                            return "fleamarket://";
                                        }
                                    } else {
                                        return "openapp.jdmobile://";
                                    }
                                } else {
                                    return "pinduoduo://";
                                }
                            } else {
                                return "kwai://";
                            }
                        } else {
                            return "xhsdiscover://";
                        }
                    } else {
                        return "taobao://";
                    }
                } else {
                    return "snssdk1128://";
                }
            }
        } catch (Throwable __e_v0) {
            return null;
        }
        return null;
    }
    static void setBubUid(int p2)
    {
        int v0 = 0;
        if (p2 == 2) {
            v0 = com.oupao.wiz.Sug.uid;
        }
        com.oupao.wiz.Sug.bubUid = v0;
        return;
    }
    public static void show(int p11)
    {
        com.oupao.wiz.Sug.showKind = p11;
        com.oupao.wiz.Sug.showStamp = android.os.SystemClock.uptimeMillis();
        // 合并气泡模式：键盘打开也照样显示（原来会被 defer 到键盘收起，表现为「要开关一次键盘才出来」）
        if (com.oupao.wiz.Bub.ignoreImeFor(p11) || !com.oupao.wiz.Sug.imeUp()) {
            com.oupao.wiz.Sug.fgLog("SugT");
            if (p11 != 1) {
                var v0_17 = com.oupao.wiz.Sug.pkg;
                if (v0_17 != null) {
                    var v1_0 = com.oupao.wiz.Sug.fgPkg();
                    if ((!com.oupao.wiz.Sug.testBypass) && (v1_0 != null) && ((v0_17.equals(v1_0)) && ((!v0_17.equals("com.tencent.mm")) && (!v0_17.equals("com.tencent.mobileqq"))))) {
                        com.oupao.wiz.SLog.i("Sug: \u540c\u4e00\u5e94\u7528\u590d\u5236\u94fe\u63a5,\u4e0d\u5f39\u6c14\u6ce1(show)");
                        return;
                    }
                }
            }
            com.oupao.wiz.SLog.k("Sug: \u8f93\u5165\u6cd5\u4e0d\u5728 \u2192 \u6b63\u5e38\u663e\u793a\u6c14\u6ce1");
            var v0_9 = com.oupao.wiz.Cfg.gameBlockSug();
            com.oupao.wiz.Sug.gameDbg(v0_9, com.oupao.wiz.Sug.isGameFg());
            if (com.oupao.wiz.Sug.testBypass || !((v0_9) && (com.oupao.wiz.Sug.isGameFg()))) {   // 仅开了屏蔽且在游戏前台才跳过
                if (com.oupao.wiz.Sug.ctx == null) {
                    return;
                } else {
                    if (com.oupao.wiz.Sug.shown) {
                        // ⚠️「保留合并气泡」必须排在 keepOld 之前：keepOld(2,5) 会直接 return，
                        //    那样另一种气泡永远显示不出来（用户要的是两类气泡能同时显示）。
                        if (com.oupao.wiz.Bub.mergedShown() && (p11 != 2)) {
                            // 合并气泡正在显示（常驻），而这次是**另一种**气泡：
                            // 不要 dismiss（那会把合并气泡也一起收掉），只清掉 Sug 自己的显示状态，
                            // 让下面正常新建一个独立窗口 → 两种气泡可以同时存在。
                            com.oupao.wiz.SLog.i("Sug: 保留合并气泡，另开 kind=" + p11);
                            com.oupao.wiz.Sug.shown = false;
                            com.oupao.wiz.Sug.view = null;
                        } else if (com.oupao.wiz.Sug.keepOld(com.oupao.wiz.Sug.kind, p11)) {
                            return;
                        } else {
                            com.oupao.wiz.Sug.dismiss();
                        }
                    }
                    var v0_15 = com.oupao.wiz.Sug.ctx;
                    try {
                        var v1_7 = com.oupao.wiz.Ui.dp(v0_15, 46);
                        android.widget.TextView v2_8 = null;
                        try {
                            v2_8 = new android.widget.TextView(v0_15);
                        } catch (Throwable __e_v2_0) {
                            com.oupao.wiz.SLog.e("Sug: \u663e\u793a\u5f02\u5e38", __e_v2_0);
                            return;
                        }
                        // 1820fix: 气泡图标兜底——bmp 为空或全透明时，直接取应用图标自绘圆形
                        // （扇形菜单已用同一套 circleFitIcon，气泡这里之前漏了，导致图标与角标都没有）
                        try {
                            int opX = (com.oupao.wiz.Sug.bmp == null) ? -1
                                    : com.oupao.wiz.Sug.opaque(com.oupao.wiz.Sug.bmp);
                            com.oupao.wiz.SLog.r("bmp-op", "Sug: bmp opaque=" + opX + " kind=" + p11
                                    + " pkg=" + com.oupao.wiz.Sug.pkg, 1000L);
                            // kind==2 的头像是"通知 largeIcon"，绝不能被应用图标顶掉（否则角标与头像同图）
                            // kind==3 是「临时开启屏幕常亮」的 ☀ 气泡：smali 里 txt="☀"、bmp=null，
                            // 本来就不该有图标。兜底会拿 Sug.pkg（残留的包名，常常是 QQ）当图标，
                            // 于是常亮气泡变成 QQ 头像 —— 这里排除掉，恢复 ☀ 字形。
                            // ★ 原来把 kind==2 排除在外（担心头像被应用图标顶掉），
                            //   但其它应用的通知没有 largeIcon → bmp 是全透明 → 气泡没有图标。
                            //   现在只要「图几乎是空的」就兜底；kind==3（☀）仍排除。
                            boolean need = (p11 != 3) && (opX < 4);
                            if (need) {
                                String pkgX = com.oupao.wiz.Sug.pkg;
                                android.content.Context ctxX = com.oupao.wiz.Sug.ctx;
                                if ((pkgX != null) && (ctxX != null)) {
                                    int szX = com.oupao.wiz.Ui.dp(ctxX, 46);
                                    android.graphics.drawable.Drawable adX =
                                            ctxX.getPackageManager().getApplicationIcon(pkgX);
                                    var bmX = com.oupao.wiz.Sug.circleFitIcon(adX, szX);
                                    if (bmX != null) {
                                        com.oupao.wiz.Sug.bmp = bmX;
                                    }
                                    com.oupao.wiz.SLog.i("Sug: bmp 兜底 pkg=" + pkgX
                                            + " icon=" + (adX != null) + " bmp=" + (bmX != null));
                                }
                            }
                        } catch (Throwable tbx) {
                            com.oupao.wiz.SLog.e("Sug: bmp 兜底异常", tbx);
                        }
                        if (com.oupao.wiz.Sug.bmp == null) {
                            v2_8.setText(com.oupao.wiz.Sug.txt);
                            v2_8.setTextSize(22.0f);
                            v2_8.setGravity(17);
                            v2_8.setTextColor(com.oupao.wiz.Ui.adaptText(-13421779));
                            v2_8.setBackground(com.oupao.wiz.Ui.bg(-218103809, ((float) v1_7)));
                        } else {
                            android.graphics.Bitmap v3_14 = null;
                            v2_8.setText("");
                            var v3_13 = com.oupao.wiz.Sug.bmp;
                            // 角标只给微信/QQ（badgeApp 会把「带角标图标的右下 48%」贴到圆圈上，
                            // 普通应用没有真角标，贴上去就是小图标+留白）。
                            // 其它应用改用 circleFitIcon：先画到软件位图、自算不透明包围盒，
                            // 再裁掉自适应启动器图标自带的大留白 → 图标铺满圆圈。
                            boolean vWxQq = ("com.tencent.mm".equals(com.oupao.wiz.Sug.pkg))
                                    || ("com.tencent.mobileqq".equals(com.oupao.wiz.Sug.pkg));
                            if ((p11 == 2) && vWxQq) {
                                v3_14 = com.oupao.wiz.Sug.badgeApp(com.oupao.wiz.Sug.circle(v3_13, v1_7), v1_7);
                            } else if (v3_13.getWidth() == v1_7) {
                                // 兜底产出的就是「这个尺寸的圆形图」，直接用，别再二次裁剪
                                v3_14 = v3_13;
                            } else {
                                v3_14 = com.oupao.wiz.Sug.circleFitIcon(
                                        new android.graphics.drawable.BitmapDrawable(v0_15.getResources(), v3_13),
                                        v1_7);
                                if (v3_14 == null) {
                                    v3_14 = com.oupao.wiz.Sug.iconRound(v3_13, v1_7);
                                }
                            }
                            // 分身角标改由「系统 badged icon」提供（见 badgeApp → appIconUser），
                            // 这里不再自绘「分」字。
                            v2_8.setBackground(new android.graphics.drawable.BitmapDrawable(v3_14));
                        }
                        var v3_17 = new android.view.WindowManager.LayoutParams();
                        v3_17.type = 2003;
                        v3_17.flags = 40;
                        v3_17.format = 1;
                        com.oupao.wiz.Sug.applyPos(v3_17);
                        v3_17.width = v1_7;
                        v3_17.height = v1_7;
                        v2_8.setOnTouchListener(new com.oupao.wiz.Sug$C1());
                        com.oupao.wiz.SLog.i("Sug: bubble win type=2003");
                        com.oupao.wiz.Sug.wm.addView(v2_8, v3_17);
                        com.oupao.wiz.Sug.winViews.add(v2_8);
                        // 实测落点：编辑器存的是屏幕绝对坐标，这里把真实位置打出来核对。
                        // 延后 800ms 再测，避开 Bar.jelly 的缩放动画（getLocationOnScreen 会带上矩阵，
                        // 动画期间测出来会偏 half*(1-scale)，实测假象是 +64）。
                        // p-w 就是窗口原点相对屏幕的偏移（= 状态栏/挖孔等 inset），d 是最终偏差。
                        final android.widget.TextView bubV = v2_8;
                        final int wantX = v3_17.x;
                        final int wantY = v3_17.y;
                        bubV.postDelayed(new java.lang.Runnable() {
                            public void run() {
                                try {
                                    int[] p = new int[2];
                                    bubV.getLocationOnScreen(p);
                                    int[] w = new int[2];
                                    bubV.getLocationInWindow(w);
                                    com.oupao.wiz.SLog.i("Sug: bubble actual=" + p[0] + "," + p[1]
                                            + " win=" + w[0] + "," + w[1]
                                            + " inset=" + (p[0] - w[0]) + "," + (p[1] - w[1])
                                            + " want=" + wantX + "," + wantY
                                            + " d=" + (p[0] - wantX) + "," + (p[1] - wantY));
                                } catch (Throwable ignore) {
                                }
                            }
                        }, 800);
                        com.oupao.wiz.Bar.jelly(v2_8);
                        com.oupao.wiz.Sug.view = v2_8;
                        com.oupao.wiz.Sug.showBanner(v0_15, v3_17, p11);
                        com.oupao.wiz.Sug.shown = true;
                        com.oupao.wiz.Sug.kind = p11;
                        com.oupao.wiz.Sug.setBubUid(p11);
                        var v3_1 = com.oupao.wiz.Sug.main;
                        if (v3_1 != null) {
                            int v5_1 = 0;
                            com.oupao.wiz.Sug.cancelTimer();
                            // 3321：**手电筒建议气泡（kind=1）不自动消失** ——
                            //   按用户要求：显示后一直留着，点一下关闭、或者下滑隐藏。
                            if (p11 != 1) {
                                var v4_1 = new com.oupao.wiz.Sug$C3();
                                com.oupao.wiz.Sug.timer = v4_1;
                                if (p11 != 2) {
                                    v5_1 = 3000;
                                } else {
                                    v5_1 = 5000;
                                }
                                v3_1.postDelayed(v4_1, v5_1);
                            }
                        }
                        var v3_4 = new StringBuilder("Sug: \u6c14\u6ce1\u5df2\u663e\u793a kind=");
                        v3_4.append(p11);
                        com.oupao.wiz.SLog.i(v3_4.toString());
                        // 形态合并统一在这里接入：show() 是**所有**显示路径的收敛点
                        // （Sug$S 新通知、Sug$C4 输入法收起后补显示、Sug$L 等）。
                        // 只在消息类（kind=2）合并；kind 1/3/5/6 是手电筒/剪贴板等，保持原样。
                        if (p11 == 2) {
                            com.oupao.wiz.Bub.attach();
                        }
                        com.oupao.wiz.Sug.startFgWatch();
                        return;
                    } catch (Throwable __e_v1) {
                        com.oupao.wiz.SLog.e("Sug: 显示异常", __e_v1);
                        return;
                    }
                }
            } else {
                com.oupao.wiz.SLog.k(new StringBuilder().append("Sug: \u6e38\u620f\u6a21\u5f0f\u751f\u6548(").append(com.oupao.wiz.Sug.gameFgPkg).append("), \u5c4f\u853d\u5efa\u8bae").toString());
                return;
            }
        } else {
            com.oupao.wiz.Sug.pendOn = true;
            com.oupao.wiz.Sug.pendKind = p11;
            com.oupao.wiz.SLog.k("Sug: \u8f93\u5165\u6cd5\u5728 \u2192 \u6c14\u6ce1\u5148\u6302\u8d77, \u6536\u8d77\u540e\u663e\u793a");
            return;
        }
    }
    public static void show2(String p7, String p8, android.graphics.Bitmap p9)
    {
        if (p7 != null) {
            var v1_5 = com.oupao.wiz.Sug.fgPkg();
            // 2834：QQ 不再豁免 —— 用户要求「QQ 在前台时不要弹气泡，像微信那样」。
                //   微信保留豁免（它的气泡会被 fgWatch 按聊天页/会话立刻收掉，表现正常）；
                //   QQ 读不到当前会话，豁免后气泡会一直挂着。
                // 3192：**必须同时比用户**（0=主应用 / 999=分身）。原来只比包名，
                //   于是"主 QQ 在前台"会把**分身 QQ** 的通知气泡当成同一个应用丢掉，
                //   反过来也一样（微信同理，只是微信还有聊天页判断兜着）。
                int fuS = com.oupao.wiz.Sug.fgTaskUser(p7);
                if (fuS < 0) {
                    fuS = com.oupao.wiz.Hooks.focusedUserId();
                }
                if ((v1_5 != null) && p7.equals(v1_5)
                        && (fuS == com.oupao.wiz.Sug.notifUserId())) {
                var v2_4 = new StringBuilder();
                v2_4.append("Sug: \u540c\u4e00\u5e94\u7528\u590d\u5236\u94fe\u63a5,\u4e0d\u5f39\u6c14\u6ce1 ");
                v2_4.append(p7);
                v2_4.append(" fu=");
                v2_4.append(com.oupao.wiz.Hooks.focusedUserId());
                v2_4.append(" nu=");
                v2_4.append(com.oupao.wiz.Sug.notifUserId());
                com.oupao.wiz.SLog.i(v2_4.toString());
                return;
            }
        }
        com.oupao.wiz.Sug.pkg = p7;
        // 3192：把这个气泡属于哪个用户记下来（0=主应用 / 999=分身），
        // 后面的「进了应用就收气泡 / 通知被取消就收气泡」都按用户比，不能只看包名。
        com.oupao.wiz.Sug.bubbleUser = com.oupao.wiz.Sug.notifUserId();
        com.oupao.wiz.Sug.kPkg = p7;
        com.oupao.wiz.Sug.kChat = com.oupao.wiz.Sug.chatUser;
        com.oupao.wiz.Sug.kUin = com.oupao.wiz.Sug.qqUin;
        com.oupao.wiz.Sug.kType = com.oupao.wiz.Sug.qqType;
        com.oupao.wiz.Sug.kNi = com.oupao.wiz.Sug.ni;
        com.oupao.wiz.Sug.kPi = com.oupao.wiz.Sug.pi;
        com.oupao.wiz.Sug.kUid = com.oupao.wiz.Sug.uid;
        var v0_1 = com.oupao.wiz.Sug.main;
        if (v0_1 != null) {
            v0_1.post(new com.oupao.wiz.Sug$S(p7, p8, p9));
        }
        return;
    }
    public static void showBanner(android.content.Context p13, android.view.WindowManager.LayoutParams p14, int p15)
    {
        var v5_0 = com.oupao.wiz.Sug.pkg;
        var v6_0 = com.oupao.wiz.Sug.fgPkg();
        var v7_19 = new StringBuilder();
        v7_19.append("Sug DBG pkg=");
        v7_19.append(v5_0);
        v7_19.append(" fg=");
        v7_19.append(v6_0);
        v7_19.append(" eq=");
        boolean v8_4 = false;
        if ((v5_0 != null) && (v6_0 != null)) {
            v8_4 = v5_0.equals(v6_0);
        }
        v7_19.append(v8_4);
        com.oupao.wiz.SLog.i(v7_19.toString());
        var v0_0 = com.oupao.wiz.Sug.pkg;
        if (v0_0 != null) {
            var v1_1 = com.oupao.wiz.Sug.fgPkg();
            // 333x：微信也按"同应用同用户在前后台"屏蔽（原来只豁免了微信）。
            int fuB = com.oupao.wiz.Sug.fgTaskUser(v1_1);
            if (fuB < 0) {
                fuB = com.oupao.wiz.Hooks.focusedUserId();
            }
            if ((v1_1 != null) && v0_0.equals(v1_1)
                    && (fuB == com.oupao.wiz.Sug.notifUserId())) {
                var v2_1 = new StringBuilder();
                v2_1.append("Sug: \u540c\u4e00\u5e94\u7528\u590d\u5236\u94fe\u63a5,\u4e0d\u5f39\u6c14\u6ce1 ");
                v2_1.append(v0_0);
                com.oupao.wiz.SLog.i(v2_1.toString());
                return;
            }
        }
        try {
            com.oupao.wiz.Sug.hideBanner();
            var v0_3 = new StringBuilder();
            v0_3.append("Sug: banner enter kind=");
            v0_3.append(p15);
            v0_3.append(" txt=");
            v0_3.append(com.oupao.wiz.Sug.txt);
            com.oupao.wiz.SLog.i(v0_3.toString());
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.SLog.e("Sug: banner \u5f02\u5e38", __e_v0_1);
            return;
        }
        if (p15 == 2) {
            var v1_6 = com.oupao.wiz.Sug.txt;
            if ((v1_6 != null) && ((v1_6.length() >= 1) && (com.oupao.wiz.Sug.infoOn()))) {
                var v2_9 = com.oupao.wiz.Sug.wm;
                if (v2_9 != null) {
                    var v3_3 = new android.widget.TextView(p13);
                    v3_3.setText(v1_6);
                    v3_3.setTextSize(2, 15.0f);
                    var v6_1 = ((int) v3_3.getPaint().measureText(v1_6));
                    var v5_2 = ((((int) (android.util.TypedValue.applyDimension(2, 15.0f, p13.getResources().getDisplayMetrics()) * ((float) 7))) + (com.oupao.wiz.Ui.dp(p13, 11) + com.oupao.wiz.Ui.dp(p13, 11))) + com.oupao.wiz.Ui.dp(p13, 1));
                    v3_3.setMaxWidth(v5_2);
                    v3_3.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    var v7_1 = new StringBuilder();
                    v7_1.append("Sug: banner tw=");
                    v7_1.append(v6_1);
                    v7_1.append(" max=");
                    v7_1.append(v5_2);
                    com.oupao.wiz.SLog.i(v7_1.toString());
                    var v8_3 = v1_6.length();
                    if (v8_3 > 7) {
                        var v10_1 = new android.text.SpannableString(v1_6);
                        v10_1.setSpan(new android.text.style.RelativeSizeSpan(0.92f), 7, v8_3, 33);
                        v3_3.setText(v10_1);
                    }
                    v3_3.setSingleLine(false);
                    v3_3.setMaxLines(2);
                    v3_3.setGravity(17);
                    v3_3.setTextColor(com.oupao.wiz.Ui.adaptText(-1));
                    v3_3.setBackground(com.oupao.wiz.Ui.bg(-870704614, ((float) com.oupao.wiz.Ui.dp(p13, 12))));
                    v3_3.setPadding(com.oupao.wiz.Ui.dp(p13, 11), com.oupao.wiz.Ui.dp(p13, 4), com.oupao.wiz.Ui.dp(p13, 11), com.oupao.wiz.Ui.dp(p13, 4));
                    v3_3.setOnTouchListener(new com.oupao.wiz.Sug$C1());
                    var v7_16 = new android.view.WindowManager.LayoutParams();
                    v7_16.type = 2003;
                    v7_16.flags = 40;
                    v7_16.format = 1;
                    v7_16.width = -2;
                    v7_16.height = com.oupao.wiz.Ui.dp(p13, 46);
                    com.oupao.wiz.Sug.applyPos(v7_16);
                    var v8_16 = p13.getResources().getDisplayMetrics().widthPixels;
                    v3_3.measure(0, 0);
                    var v9_2 = v3_3.getMeasuredWidth();
                    int v10_3 = (com.oupao.wiz.Ui.dp(p13, 52) + p14.x);
                    if (((v10_3 + v9_2) + com.oupao.wiz.Ui.dp(p13, 4)) > v8_16) {
                        v10_3 = ((p14.x - v9_2) - com.oupao.wiz.Ui.dp(p13, 6));
                        var v11_11 = com.oupao.wiz.Ui.dp(p13, 4);
                        if (v10_3 < v11_11) {
                            v10_3 = v11_11;
                        }
                    }
                    v7_16.x = v10_3;
                    var v11_13 = new StringBuilder();
                    v11_13.append("Sug: banner span x=");
                    v11_13.append(v10_3);
                    v11_13.append(" mw=");
                    v11_13.append(v9_2);
                    v11_13.append(" sw=");
                    v11_13.append(v8_16);
                    v11_13.append(" bx=");
                    v11_13.append(p14.x);
                    com.oupao.wiz.SLog.i(v11_13.toString());
                    v2_9.addView(v3_3, v7_16);
                    com.oupao.wiz.Sug.winViews.add(v3_3);
                    com.oupao.wiz.SLog.i("Sug: banner ok");
                    com.oupao.wiz.Sug.banner = v3_3;
                }
            }
        }
        return;
    }
    public static void showHint()
    {
        android.view.WindowManager v0_0 = null;
        try {
            v0_0 = com.oupao.wiz.Sug.wm;
        } catch (Throwable __e_v0) {
            return;
        }
        if (v0_0 != null) {
            var v1_0 = com.oupao.wiz.Sug.ctx;
            if (v1_0 != null) {
                com.oupao.wiz.Sug.hideHint();
                var v2_1 = new android.widget.TextView(v1_0);
                v2_1.setText("\u5c4f\u5e55\u5e38\u4eae\u5df2\u4e34\u65f6\u5f00\u542f\n\u79bb\u5f00\u5e94\u7528\u540e\u6062\u590d\u81ea\u52a8\u9501\u5c4f");
                v2_1.setTextSize(0, 28.0f);
                v2_1.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                v2_1.setTextColor(com.oupao.wiz.Ui.adaptText(-16777216));
                v2_1.setGravity(17);
                v2_1.setBackground(com.oupao.wiz.Ui.bg((com.oupao.wiz.Ui.night() ? 0x33FFFFFF : -1711276033), ((float) com.oupao.wiz.Ui.dp(v1_0, 14))));
                com.oupao.wiz.Ui.pad(v2_1, 8, 6, 8, 6);
                v2_1.measure(0, 0);
                var v4_8 = ((v1_0.getResources().getDisplayMetrics().widthPixels - v2_1.getMeasuredWidth()) / 2);
                int v5_2 = com.oupao.wiz.Sug.bubY();
                var v7_0 = com.oupao.wiz.Sug.view;
                if (v7_0 != null) {
                    int[] v8_1 = new int[2];
                    v7_0.getLocationOnScreen(v8_1);
                    v5_2 = (v8_1[1] + ((v7_0.getHeight() - v2_1.getMeasuredHeight()) / 2));
                }
                var v6_2 = new android.view.WindowManager.LayoutParams();
                v6_2.type = 2003;
                // 0x100 = FLAG_LAYOUT_IN_SCREEN：提示条的 x/y 是按屏幕绝对坐标算的
                // （v4_8 用屏幕宽居中、v5_2 用气泡的 getLocationOnScreen），不加会多下移一个状态栏
                v6_2.flags = 296;
                v6_2.format = 1;
                v6_2.width = -2;
                v6_2.height = -2;
                v6_2.gravity = 8388659;
                v6_2.x = v4_8;
                v6_2.y = v5_2;
                v0_0.addView(v2_1, v6_2);
                com.oupao.wiz.Sug.winViews.add(v2_1);
                com.oupao.wiz.Sug.hintV = v2_1;
                var v0_1 = com.oupao.wiz.Sug.main;
                if (v0_1 != null) {
                    v0_1.postDelayed(new com.oupao.wiz.Sug$H(), 6000);
                }
            }
        }
        return;
    }
    public static void showInstall(String p2, android.graphics.Bitmap p3)
    {
        var v0 = com.oupao.wiz.Sug.main;
        if (v0 != null) {
            v0.post(new com.oupao.wiz.Sug$I(p2, p3));
        }
        return;
    }
    public static void showLink(String p2, String p3, android.graphics.Bitmap p4)
    {
        var v0 = com.oupao.wiz.Sug.main;
        if (v0 != null) {
            v0.post(new com.oupao.wiz.Sug$L(p2, p3, p4));
        }
        return;
    }
    public static void showMusic(String p2, android.graphics.Bitmap p3)
    {
        var v0 = com.oupao.wiz.Sug.main;
        if (v0 != null) {
            v0.post(new com.oupao.wiz.Sug$M(p2, p3));
        }
        return;
    }
    public static void startTick()
    {
        try {
            if (!com.oupao.wiz.Sug.tickOn) {
                var v0 = com.oupao.wiz.Sug.main;
                if (v0 != null) {
                    com.oupao.wiz.Sug.tickOn = true;
                    v0.post(new com.oupao.wiz.Sug$Tick());
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    public static void tick()
    {
        try {
            com.oupao.wiz.Sug.onPolicy();
            com.oupao.wiz.Sug.appKeepTick();
            return;
        } catch (Throwable __e_v0) {
            return;
        }
    }
    public static void torchFire()
    {
        com.oupao.wiz.Sug.torchPend = null;
        try {
            // 原来硬性要求「当前没有任何气泡(kind==0)」才弹手电筒，
            // 于是合并的通知气泡一显示，手电筒气泡就永远不出来了。
            // 现在允许「当前显示的是合并通知气泡」的情况下也弹（各自独立窗口）。
            boolean torchOk = (com.oupao.wiz.Sug.kind == 0)
                    || (com.oupao.wiz.Bub.mergedShown() && (com.oupao.wiz.Sug.kind == 2));
            if ((com.oupao.wiz.Sug.torchOn) && torchOk && (com.oupao.wiz.Sug.ctx != null)) {
                // 必须清掉 pkg：否则 show() 会按 Sug.pkg（上一条通知的应用）做「图标兜底」，
                // 把手电筒图标换成那个应用的图标（实测手电筒气泡变成微信头像）。
                com.oupao.wiz.Sug.pkg = null;
                com.oupao.wiz.Sug.kPkg = null;
                com.oupao.wiz.SLog.k("Sug: \u624b\u7535\u7b52\u5f00\u542f\u6ee1 5 \u79d2, \u5f39\u6c14\u6ce1");
                com.oupao.wiz.Sug.txt = "\ud83d\udd26";
                com.oupao.wiz.Sug.bmp = com.oupao.wiz.Sug.circle(com.oupao.wiz.Sug.torchIcon(com.oupao.wiz.Ui.dp(com.oupao.wiz.Sug.ctx, 46)), com.oupao.wiz.Ui.dp(com.oupao.wiz.Sug.ctx, 46));
                com.oupao.wiz.Sug.show(1);
            }
        } catch (Throwable __e___ex) {
            return;
        }
        return;
    }
    public static android.graphics.Bitmap torchIcon(int p6)
    {
        // 3318：不再读 assets/torch.png —— 直接用 drawTorchIcon() 代码绘制
        //   （和「常亮显示建议」的图标同一套画法），省掉 102KB 图片资源。
        android.graphics.Bitmap v0 = com.oupao.wiz.Sug.ticon;
        if (v0 == null) {
            v0 = com.oupao.wiz.Sug.drawTorchIcon(p6);
            com.oupao.wiz.Sug.ticon = v0;
        }
        return v0;
    }
    static void useBubUid()
    {
        com.oupao.wiz.Sug.uid = com.oupao.wiz.Sug.bubUid;
        return;
    }
    public static void vib(int p6)
    {
        try {
            if (com.oupao.wiz.Cfg.vibOn()) {
                if (p6 != 0) {
                    var v2 = com.oupao.wiz.Sug.vibAmp(p6);
                    var v3 = com.oupao.wiz.Sug.vibDurMs(p6);
                    var v0_3 = com.oupao.wiz.Sug.ctx;
                    if (v0_3 != null) {
                        var v0_0 = v0_3.getSystemService("vibrator");
                        if ((v0_0 instanceof android.os.Vibrator)) {
                            ((android.os.Vibrator) v0_0).vibrate(android.os.VibrationEffect.createOneShot(((long) v3), v2));
                            var v1_4 = new StringBuilder();
                            v1_4.append("Sug: vib ok lv=");
                            v1_4.append(p6);
                            v1_4.append(" amp=");
                            v1_4.append(v2);
                            v1_4.append(" dur=");
                            v1_4.append(v3);
                            com.oupao.wiz.SLog.i(v1_4.toString());
                        } else {
                            com.oupao.wiz.SLog.i("Sug: vib skip no-vibrator");
                            return;
                        }
                    } else {
                        com.oupao.wiz.SLog.i("Sug: vib skip ctx=null");
                        return;
                    }
                }
                return;
            } else {
                return;
            }
        } catch (Throwable __e_v0_2) {
            com.oupao.wiz.SLog.e("Sug: vib error", __e_v0_2);
            return;
        }
    }
    public static int vibAmp(int p1)
    {
        // 3275：振幅**跟随系统的触感强度**（设置-声音与振动-触感反馈），
        //   不再用我们自己的 0~100 换算；p1 现在只决定时长（vibDurMs）。
        return com.oupao.wiz.Cfg.sysHapticAmp();
    }
    public static int vibDurMs(int p1)
    {
        return (((p1 * 28) / 100) + 12);
    }
    public static void vibObj(Object p10)
    {
        try {
            if (p10 != null) {
                var v1 = com.oupao.wiz.Cfg.flyVibLevel();
                if (v1 != 0) {
                    var v2 = com.oupao.wiz.Sug.vibAmp(v1);
                    int v3_0 = (com.oupao.wiz.Sug.vibDurMs(v1) * 2);
                    if (v3_0 < 30) {
                        v3_0 = 30;
                    }
                    var v5_1 = new StringBuilder();
                    v5_1.append("VibObj: lv=");
                    v5_1.append(v1);
                    v5_1.append(" amp=");
                    v5_1.append(v2);
                    v5_1.append(" dur=");
                    v5_1.append(v3_0);
                    // verbose：vibObj 每次震动都会走（字母索引换字母、菜单点开），
                    // info 级会把正常日志冲掉。
                    com.oupao.wiz.SLog.r("r-vibobj", v5_1.toString(), 1000L);
                    int v0 = 0;
                    var v4_1 = android.os.VibrationEffect.createOneShot(((long) v3_0), v2);
                    if (v4_1 != null) {
                        Class[] v5_4 = new Class[1];
                        v5_4[0] = android.os.VibrationEffect.class;
                        var v5_5 = p10.getClass().getMethod("vibrate", v5_4);
                        Object[] v6_6 = new Object[1];
                        v6_6[0] = v4_1;
                        v5_5.invoke(p10, v6_6);
                        com.oupao.wiz.SLog.r("r-vibobj-ok", "VibObj: VibrationEffect ok", 1000L);
                        v0 = 1;
                    }
                    if (v0 == 0) {
                        // 这条保留 info：走到 fallback 说明设备缺 VibrationEffect 支持，值得知道
                        com.oupao.wiz.SLog.i("VibObj: fallback vibrate(long)");
                        Class[] v5_9 = new Class[1];
                        v5_9[0] = Long.TYPE;
                        var v5_10 = p10.getClass().getMethod("vibrate", v5_9);
                        Object[] v6_10 = new Object[1];
                        v6_10[0] = Long.valueOf(((long) v3_0));
                        v5_10.invoke(p10, v6_10);
                    }
                }
            }
        } catch (Throwable __e_v0) {
            return;
        }
        return;
    }
    static void word(android.content.Context p6, String p7)
    {
        android.content.pm.PackageManager v1_0 = null;
        try {
            v1_0 = p6.getPackageManager();
        } catch (Throwable __e_v0) {
            return;
        }
        if (v1_0.getLaunchIntentForPackage("com.ss.android.ugc.aweme") == null) {
            com.oupao.wiz.SLog.i("Clip2: \u6296\u97f3 launchIntent \u4e3a\u7a7a(\u672a\u5b89\u88c5/\u88ab\u7981\u7528)");
        } else {
            var v3_0 = com.oupao.wiz.Ui.dp(p6, 96);
            try {
                var v4 = v1_0.getApplicationIcon("com.ss.android.ugc.aweme");
                android.graphics.Bitmap v5_0 = null;
                if (v4 == null) {
                    v5_0 = com.oupao.wiz.Sug.iconRound(com.oupao.wiz.Sug.iconOf(v4, v3_0), v3_0);
                }
                com.oupao.wiz.Sug.showLink("com.ss.android.ugc.aweme", p7, v5_0);
                com.oupao.wiz.SLog.i("Clip2: showLink \u5df2\u8c03\u7528");
                com.oupao.wiz.SLog.flushNow();
                var v1_2 = new StringBuilder();
                v1_2.append("Clip: \u53e3\u4ee4 -> \u6296\u97f3\u6c14\u6ce1 len=");
                v1_2.append(p7.length());
                com.oupao.wiz.SLog.i(v1_2.toString());
            } catch (Throwable __e_io) {
            }
        }
        return;
    }
    public static void wxLog()
    {
        android.content.ComponentName v1 = null;
        android.content.Intent v0 = null;
        try {
            v0 = com.oupao.wiz.Sug.ni;
        } catch (Throwable __e_v2_1) {
            com.oupao.wiz.SLog.e("LaunchWX: \u5f02\u5e38", __e_v2_1);
            return;
        }
        if (v0 != null) {
            v1 = null;
        } else {
            v1 = v0.getComponent();
        }
        var v2_3 = new StringBuilder();
        v2_3.append("LaunchWX: cu=");
        v2_3.append(com.oupao.wiz.Sug.chatUser);
        v2_3.append(" ni=");
        v2_3.append(v1);
        v2_3.append(" act=");
        if (v0 != null) {
            v2_3.append("null");
        } else {
            v2_3.append(v0.getAction());
        }
        v2_3.append(" data=");
        if (v0 != null) {
            v2_3.append("null");
        } else {
            v2_3.append(v0.getDataString());
        }
        v2_3.append(" flg=");
        if (v0 != null) {
            v2_3.append("null");
        } else {
            v2_3.append(Integer.toHexString(v0.getFlags()));
        }
        v2_3.append(" extras=");
        // 原来这里的逻辑是反的：intent 非 null 时打 "none"，为 null 时才去
        // append(v0.getExtras().keySet()) —— 既永远打不出 extras，又埋了个 NPE
        // （被外层 catch 吞掉，表现为这条日志莫名消失）。
        // 改成正常语义，且只打 key 数量、不打内容（Bundle 的 toString 可能含
        // Parcelable/Bitmap，开销不可控）。
        if (v0 == null) {
            v2_3.append("none");
        } else {
            var v3_12 = v0.getExtras();
            if (v3_12 != null) {
                v2_3.append(v3_12.keySet().size());
            } else {
                v2_3.append("none");
            }
        }
        com.oupao.wiz.SLog.v(v2_3.toString());
        return;
    }
    /**
     * 3191：这个包在本机有"应用分身"吗（ColorOS 分身 = 用户 999）。
     *
     * 为什么要判断：打开通知时我们用
     * `startZoomWindow(intent, opts, user, "com.coloros.smartsidebar")`，
     * 而 ROM 的 `MultiApp-CMAService.replaceIntentForMultiApp` 看到"调用方=smartsidebar / uid1000、
     * 目标=一个有分身的应用"时无法判断用户要主应用还是分身，于是弹出
     * `com.oplus.multiapp.chooser.MultiAppResolverActivity`（主应用/分身 选择器）。
     * 日志实证（19:08:35.468，system_server 主线程）：
     *   callingPackage=com.coloros.smartsidebar/0 targetPackage=com.tencent.mobileqq/0
     *   callingUid=1000 callingPid=3659 → getChooserIntent → MultiAppResolverActivity
     * 有分身时改走通知自带的 PendingIntent（创建者是应用本人，ROM 能正确归属）。
     */
    static boolean hasClone(String pkg) {
        try {
            if ((pkg == null) || pkg.isEmpty()) {
                return false;
            }
            android.content.Context c = com.oupao.wiz.Sug.ctx;
            if (c == null) {
                c = com.oupao.wiz.Cfg.sysContext();
            }
            if (c == null) {
                return false;
            }
            android.content.pm.PackageManager pm = c.getPackageManager();
            // getApplicationInfoAsUser 是 @hide（公开 SDK 看不到）→ 反射；
            // 我们跑在 system_server，不受隐藏 API 限制。
            java.lang.reflect.Method m = android.content.pm.PackageManager.class.getMethod(
                    "getApplicationInfoAsUser", String.class, int.class, android.os.UserHandle.class);
            m.invoke(pm, pkg, 0, com.oupao.wiz.FrameworkExt.userHandleOf(999));
            return true;
        } catch (Throwable t) {
            return false;                        // 没有分身 / 查不到都按"没有"处理
        }
    }

    public static boolean zoomOpen(android.content.Intent p9)
    {
        try {
            if (com.oupao.wiz.Sug.ctx == null) {
                return false;
            } else {
                // 3199：有分身的应用**不能放弃小窗**（3192 那段 `return false` 会让调用方退到
                // piBoundsSend()，那条是 PendingIntent + setLaunchBounds = **全屏**，
                // 用户实测：只有 QQ 变成全屏，微信因为后面还有 launchTail 兜住所以正常）。
                // 现在改成：有分身时把 callPkg 换成**目标应用自己**，ROM 的
                // MultiApp-CMAService 就不会再问"主应用还是分身"，但仍然走小窗启动。
                String tp = p9.getPackage();
                if (tp == null) {
                    var cn = p9.getComponent();
                    if (cn != null) {
                        tp = cn.getPackageName();
                    }
                }
                boolean clone = com.oupao.wiz.Sug.hasClone(tp);
                String caller = clone ? tp : "com.coloros.smartsidebar";
                p9.addFlags(268435456);
                p9.addFlags(32);
                var ao = android.app.ActivityOptions.makeBasic().setLaunchDisplayId(0);
                android.graphics.Rect lb = com.oupao.wiz.ZoomGate.notificationLaunchBounds();
                if (lb != null) {
                    ao.setLaunchBounds(lb);
                }
                var v1_1 = ao.toBundle();
                v1_1.putInt("extra_window_mode", 100);
                // C17 通知气泡这条路径和 zoomOpenAs 对齐：补 windowingMode，不再塞 splash。
                //   C17 上只给 extra_window_mode 时偶发把通知 Intent 当普通启动 → 细条小窗。
                if (com.oupao.wiz.Cfg.isC17()) {
                    v1_1.putInt("android:activity.windowingMode", 100);
                } else {
                    v1_1.putInt("android.activity.splashScreenStyle", 1);
                }
                var v2_4 = Integer.valueOf(com.oupao.wiz.Sug.notifUserId());
                var v3_3 = Class.forName("com.oplus.zoomwindow.OplusZoomWindowManager");
                var v4_2 = com.oupao.wiz.Rt.invoke(com.oupao.wiz.Rt.methodByName(v3_3, "getInstance", 0), null, null);
                var v3_4 = com.oupao.wiz.Rt.methodByName(v3_3, "startZoomWindow", 4);
                Object[] v5_4 = new Object[4];
                v5_4[0] = p9;
                v5_4[1] = v1_1;
                v5_4[2] = v2_4;
                v5_4[3] = caller;
                var v6_7 = com.oupao.wiz.Rt.invoke(v3_4, v4_2, v5_4);
                // 3414：2/3（TASK_TO_FRONT / DELIVERED_TO_TOP）也算成功，见 ZoomGate.zoomRetOk
                boolean zoomOk = com.oupao.wiz.ZoomGate.zoomRetOk(v6_7);
                var v7_2 = new StringBuilder();
                v7_2.append("Launch: msg zoom result=");
                v7_2.append(String.valueOf(v6_7));
                v7_2.append(" ok=");
                v7_2.append(zoomOk);
                v7_2.append(" caller=");
                v7_2.append(caller);
                v7_2.append(" clone=");
                v7_2.append(clone);
                com.oupao.wiz.SLog.i(v7_2.toString());
                // 3199：真的失败了才让调用方兜底（以前无条件 return true，失败会"点了没反应"）
                return zoomOk;
            }
        } catch (Throwable __e_v0_1) {
            com.oupao.wiz.SLog.e("zoomOpen error", __e_v0_1);
            return false;
        }
    }
    public static boolean zoomOpenAs(android.content.Intent p8, int p9)
    {
        try {
            if ((com.oupao.wiz.Sug.ctx != null) && (p8 != null)) {
                var v3_0 = android.app.ActivityOptions.makeBasic();
                v3_0.setLaunchDisplayId(0);
                var v3_1 = v3_0.toBundle();
                v3_1.putInt("extra_window_mode", 100);
                v3_1.putInt("android:activity.windowingMode", 100);
                Object[] v4_2 = new Object[4];
                v4_2[0] = p8;
                v4_2[1] = v3_1;
                v4_2[2] = Integer.valueOf(p9);
                v4_2[3] = "com.coloros.smartsidebar";
                var v5_6 = Class.forName("com.oplus.zoomwindow.OplusZoomWindowManager");
                var v6_4 = com.oupao.wiz.Rt.invoke(com.oupao.wiz.Rt.methodByName(v5_6, "getInstance", 0), null, null);
                if (v6_4 != null) {
                    var v7_3 = com.oupao.wiz.Rt.methodByName(v5_6, "startZoomWindow", 4);
                    if (v7_3 != null) {
                        var v4_3 = com.oupao.wiz.Rt.invoke(v7_3, v6_4, v4_2);
                        // 3414：ret 2/3 也是成功（TASK_TO_FRONT / DELIVERED_TO_TOP）
                        if (com.oupao.wiz.ZoomGate.zoomRetOk(v4_3)) {
                            com.oupao.wiz.SLog.i("Sug: zoomOpenAs ok ret=" + String.valueOf(v4_3));
                            return true;
                        }
                        com.oupao.wiz.SLog.i("Sug: zoomOpenAs 被拒 ret=" + String.valueOf(v4_3));
                    }
                }
            }
        } catch (Throwable __e_v4_9) {
            com.oupao.wiz.SLog.e("Sug: zoomOpenAs \u5931\u8d25", __e_v4_9);
            return false;
        }
        return false;
    }
}
