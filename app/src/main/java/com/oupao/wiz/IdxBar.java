package com.oupao.wiz;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 全局字母引索浮层（性能版）。
 *
 * 贴边透明字母条 + 条正上方大气泡 + 旁边自绘的应用网格（无容器背景、全局模糊）。
 * 性能要点：
 *   1. 网格是**一个自绘 View**，换字母只 setData + invalidate，不再逐格换 ImageView；
 *   2. 图标一次性转成小 Bitmap 缓存（AdaptiveIconDrawable 直接画很贵）；
 *   3. 字母条预渲染成 Bitmap；
 *   4. 窗口/视图/数据预建复用，手势里只切可见性；
 *   5. 命中测试纯几何计算，不碰视图树。
 */
public final class IdxBar {

    private static final int BAR_W_DP = 16;
    private static final int ICON_DP = 52;
    private static final int COLS = 4;
    private static final int ROWS = 4;
    /** ★ 分组可能有「固定应用/最近应用」两个标题 + 两段内容，最多允许这么多行 */
    private static final int MAX_ROWS = 12;
    private static final int BAR_PERCENT = 42;   // 原 38：间距增大，顶边不变、底边下延
    private static final int BAR_OFFSET = 12;

    private static WindowManager wm;
    private static FrameLayout rootView;
    private static FrameLayout layer;
    private static BarView barView;
    private static TextView bubble;
    private static TextView letterTitle;
    private static GridView grid;

    private static int side = 1;
    private static int sw;
    private static int sh;
    private static int barW;
    private static int barH;
    private static int barTop;
    private static int gridBaseTop;
    private static int bubbleH;
    /** 字母字号（按旧 barH 38% 算，不跟随间距变大） */
    private static float letterTextPx;
    private static int builtSide;
    private static int builtW;
    private static int builtH;
    private static long dataAt;
    private static int curIdx = -1;
    private static int hoveredIdx = -1;
    private static long lastBuzzAt;
    private static long lastMoveAt;
    /** 振动等级缓存（2s），避免每换一个字母都读一次配置 */
    private static long vibCfgAt;
    private static int vibCfgVal = -1;

    static final ArrayList<String> letters = new ArrayList<String>();
    static final HashMap<String, ArrayList<Object[]>> groups =
            new HashMap<String, ArrayList<Object[]>>();
    private static final HashMap<String, Bitmap> iconCache = new HashMap<String, Bitmap>();
    private static volatile boolean prefetching;
    private static volatile long prefetchSig;
    private static int dataGen;
    private static ArrayList<String> lastApps;
    /** 最近应用文件的最后修改时间（变了就提前重建 ★ 分组） */
    private static long recMt;
    /** 3291：recents 文件 mtime 检查节流（每 5s 才 stat 一次） */
    private static long recMtAt;
    private static Context appCtx;

    private IdxBar() {
    }

    private static void L(String m) {
        try {
            com.oupao.wiz.SLog.i(m);
        } catch (Throwable ignore) {
        }
    }

    /** oupao_idxbar 的窗口参数（用于动态开关背景模糊） */
    private static WindowManager.LayoutParams barLp;

    /** 把索引条窗口挂回 WindowManager（主线程） */
    private static void attachBar() {
        try {
            if ((wm == null) || (rootView == null) || (barLp == null)) {
                return;
            }
            if (rootView.getParent() == null) {
                wm.addView(rootView, barLp);
            }
        } catch (Throwable ignore) {
        }
    }

    /**
     * 把索引条窗口从 WindowManager 摘掉。
     *
     * 背景模糊是窗口级的：只改标志位的话系统还要重算/淡出图层，
     * 总会比内容慢一拍。直接摘掉窗口，内容和模糊同一帧消失。
     */
    private static void detachBar() {
        try {
            if ((wm == null) || (rootView == null)) {
                return;
            }
            if (rootView.getParent() != null) {
                wm.removeViewImmediate(rootView);
            }
        } catch (Throwable ignore) {
        }
    }

    /**
     * 开/关背景模糊。FLAG_BLUR_BEHIND = 0x4。
     *
     * 必须跟着 show/hide 同步开关：applyBlur 只在 build 时设了一次，
     * 之后 hide() 只把 view 设成 GONE，窗口的模糊标志还在，
     * 表现就是「模糊和 app 不是一起消失」。
     */
    private static void setBlur(boolean on) {
        try {
            if ((rootView == null) || (wm == null) || (barLp == null)) {
                return;
            }
            if (on) {
                barLp.flags |= 0x4;
            } else {
                barLp.flags &= ~0x4;
            }
            try {
                int r = on ? com.oupao.wiz.Ui.dp(rootView.getContext(), 26) : 0;
                barLp.getClass().getMethod("setBlurBehindRadius", int.class).invoke(barLp, r);
            } catch (Throwable ignore) {
            }
            wm.updateViewLayout(rootView, barLp);
        } catch (Throwable ignore) {
        }
    }

    private static void applyBlur(Context ctx, WindowManager.LayoutParams lp) {
        try {
            if (android.os.Build.VERSION.SDK_INT < 31) {
                return;
            }
            lp.flags |= 0x4;
            int r = com.oupao.wiz.Ui.dp(ctx, 26);
            try {
                var m = lp.getClass().getMethod("setBlurBehindRadius", int.class);
                m.invoke(lp, r);
                return;
            } catch (Throwable ignore) {
            }
            try {
                var f = lp.getClass().getField("blurBehindRadius");
                f.setInt(lp, r);
            } catch (Throwable ignore) {
            }
        } catch (Throwable ignore) {
        }
    }

    // ---------------- 预建 / 显示 / 隐藏 ----------------

    static void prewarm(Context ctx) {
        try {
            if ((layer == null) && (ctx != null) && com.oupao.wiz.Cfg.idxMaster()) {
                build(ctx, 1);
                if (!letters.isEmpty()) {
                    showGroup(0);
                }
                hide();
                L("IdxBar: prewarm done");
            }
        } catch (Throwable t) {
        }
    }

    /** 2836：是否被要求显示（build 可能被投递到主线程异步执行，回来时要按这个决定显不显示） */
    private static volatile boolean wantShown;

    static void show(Context ctx, int triggerSide) {
        try {
            wantShown = true;
            // 事件驱动（2918 审计第 5 项）：用户要看引索条了 → 让后台 tick 立刻刷一次「最近应用」，
            // 不再靠后台每 5s 无脑轮询 getRecentTasks。
            com.oupao.wiz.Tools.wantRecents();
            prewarm(ctx);
            if ((rootView == null) || (builtSide != triggerSide)) {
                drop();
                build(ctx, triggerSide);
            }
            if (rootView == null) {
                return;
            }
            // 手势里绝不重建数据（枚举包 + 取图标会卡 100ms+）；真空才建一次
            if (letters.isEmpty()) {
                buildData(ctx);
                dataAt = android.os.SystemClock.uptimeMillis();
            } else if (letters.size() < 10) {
                dataAt = 0;   // 不完整：让下一次空闲刷新尽快补齐
            }
            side = triggerSide;
            curIdx = -1;
            hoveredIdx = -1;
            starArmed = false;
            starDueAt = 0L;
            panelOpened = false;
            attachBar();            // 同步挂回窗口（带模糊）
            setBlur(true);
            rootView.setVisibility(0);
            // 抢下触摸流：否则在 app 里滑索引条时，app 会同时收到这串手势（跟着滚动/点击）
            // 2849：**不要再做淡入动画**。
            //   show() 短时间内可能连跑好几次（日志里 22 次 shown），
            //   上一次的 90ms 渐变常被下一次的 setAlpha(0f) / hide() 打断，
            //   alpha 停在 0.2~0.6 → 用户看到就是「字母显示不完全」。
            try {
                rootView.animate().cancel();
            } catch (Throwable ignore) {
            }
            rootView.setAlpha(1f);
            L("IdxBar: shown side=" + side + " letters=" + letters.size());
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("IdxBar: show fail", t);
            hide();
        }
    }

    static boolean isShown() {
        return (rootView != null) && (rootView.getVisibility() == 0);
    }

    static void hide() {
        wantShown = false;
        try {
            if (rootView != null) {
                // 2849：把 alpha 归位，免得下次 attach 时带着半透明过来
                try {
                    rootView.animate().cancel();
                } catch (Throwable ignore) {
                }
                rootView.setAlpha(1f);
                rootView.setVisibility(8);
            }
        } catch (Throwable t) {
        }
        // 内容先消失，紧接着把窗口整个摘掉 → 模糊不会残留
        detachBar();
        setBlur(false);
        curIdx = -1;
        hoveredIdx = -1;
    }

        private static void drop() {
        // 2834：**兜底多卸一次**。以前只卸 rootView，一旦这次 removeView 失败
        // （窗口已被系统摘掉 / 非主线程 / VRI abandoned），旧窗口就留在屏上，
        // 下一次 build() 再挂一个新的 → 用户看到「两条引索条重叠」。
        // 所以这里把「上一次建出来的 layer」也一并尝试摘掉，并且按 parent 判断而不是只认 rootView。
        try {
            if ((wm != null) && (rootView != null) && (rootView.getParent() != null)) {
                wm.removeViewImmediate(rootView);
            }
        } catch (Throwable ignore) {
        }
        try {
            if ((wm != null) && (lastLayer != null) && (lastLayer != rootView)
                    && (lastLayer.getParent() != null)) {
                wm.removeViewImmediate(lastLayer);
            }
        } catch (Throwable ignore) {
        }
        rootView = null;
        layer = null;
    }

    /** 上一次 build() 建出来的容器（用于 drop() 兜底卸载） */
    private static android.view.View lastLayer;

    /**
     * 固定应用变了：让数据立刻失效，下一个 tick（≤1.5s）就重建 ★ 分组与字母分区。
     *
     * 原来只能等 refreshIfIdle 的 60s 过期阈值，表现就是「加了固定应用要等好一会」。
     */
    static void invalidate() {
        try {
            dataAt = 0L;
            if ((rootView == null) || (rootView.getVisibility() != 0)) {
                letters.clear();
                groups.clear();
                dataGen++;
            }
        } catch (Throwable t) {
        }
    }

    /** 仅在浮层隐藏时调用：后台把数据和图标都准备好 */
    static void refreshIfIdle(Context ctx) {
        try {
            if ((ctx == null) || (rootView == null) || (rootView.getVisibility() == 0)) {
                return;
            }
            long now = android.os.SystemClock.uptimeMillis();
            // 「最近应用」变了就立刻重建，不用等 60s 的 idle 间隔
            // 3291：mtime 每 5s 才 stat 一次（原来 refreshIfIdle 每个 tick 都 stat）
            if ((now - recMtAt) >= 5000L) {
                recMtAt = now;
                try {
                    long mt = new java.io.File("/data/system/oupao_cfg/recents").lastModified();
                    if (mt != recMt) {
                        recMt = mt;
                        dataAt = 0L;
                    }
                } catch (Throwable ignore) {
                }
            }
            // 字母太少说明应用列表还没就绪（开机时常见），3s 就重试
            long gap = (letters.size() < 10) ? 10000L : 60000L;   // 2906fix：3s → 10s
            if ((letters.isEmpty()) || ((now - dataAt) > gap)) {
                buildData(ctx);
                appCtx = ctx;
                prefetchIcons();
                if (!letters.isEmpty()) {
                    showGroup(0);
                }
                hide();
                dataAt = now;
                L("IdxBar: idle refresh letters=" + letters.size());
            }
        } catch (Throwable t) {
        }
    }

    private static void build(Context ctx, int triggerSide) {
        try {
            if (ctx == null) {
                return;
            }
            // 2834：**必须在主线程建窗口**。
            //   实测日志：`IdxBar: build fail : Can't create handler inside thread
            //   Thread[Oupao-IdxHeavy,…] that has not called Looper.prepare()` ——
            //   heavyTick 里的 prewarm()/refreshIfIdle() 跑在 Oupao-IdxHeavy 后台线程，
            //   那边建窗口必然抛异常；更糟的是窗口/视图状态被打断后，
            //   下一次 show() 可能留下上一份窗口 → 看到「两条引索条重叠」。
            if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
                com.oupao.wiz.SLog.i("IdxBar: build 不在主线程 → 转投主线程");
                final Context c2 = ctx;
                final int s2 = triggerSide;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        build(c2, s2);
                        // 2836：**建完立刻按调用方的意图收拾**。
                        //   开机时 prewarm() 是先 hide() 再让这次 build() 落到主线程执行的，
                        //   build() 里 wm.addView() 默认 VISIBLE → 引索条会自己挂在屏幕上，
                        //   一直到用户第一次触摸屏幕才被 hide() 掉（用户实测的「开机自动显示」）。
                        if (!wantShown) {
                            hide();
                        }
                    }
                });
                return;
            }
            side = triggerSide;
            wm = (WindowManager) ctx.getSystemService("window");
            if (wm == null) {
                return;
            }
            var dm = ctx.getResources().getDisplayMetrics();
            sw = dm.widthPixels;
            sh = dm.heightPixels;
            builtSide = triggerSide;
            builtW = sw;
            builtH = sh;
            barW = com.oupao.wiz.Ui.dp(ctx, BAR_W_DP);
            barH = ((sh * BAR_PERCENT) / 100);
            // 字号锁定在旧间距（38%）下的大小，间距变大了字不变大
            int oldBarH = ((sh * 38) / 100);
            int oldCell = Math.max(1, oldBarH / Math.max(1, letters.size()));
            letterTextPx = Math.min((oldCell * 0.95f), (barW * 0.74f));
            barTop = (((sh - barH) / 2) + ((sh * BAR_OFFSET) / 100));
            bubbleH = ((int) (sh * 0.085f));

            buildData(ctx);
            appCtx = ctx;
            prefetchIcons();

            layer = new FrameLayout(ctx);
            layer.setBackgroundColor(0x44000000);

            barView = new BarView(ctx);
            var blp = new FrameLayout.LayoutParams(barW, barH);
            blp.gravity = (Gravity.TOP | (side > 0 ? Gravity.RIGHT : Gravity.LEFT));
            blp.topMargin = barTop;
            layer.addView(barView, blp);

            bubble = new TextView(ctx);
            bubble.setTextSize(TypedValue.COMPLEX_UNIT_PX, (bubbleH * 0.5f));
            bubble.setTypeface(Typeface.DEFAULT_BOLD);
            bubble.setTextColor(Color.WHITE);
            bubble.setGravity(Gravity.CENTER);
            var bg = new android.graphics.drawable.GradientDrawable();
            bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            bg.setColor(0xE63478F6);
            bubble.setBackground(bg);
            var blp2 = new FrameLayout.LayoutParams(bubbleH, bubbleH);
            blp2.gravity = (Gravity.TOP | (side > 0 ? Gravity.RIGHT : Gravity.LEFT));
            blp2.topMargin = Math.max((barTop - bubbleH - com.oupao.wiz.Ui.dp(ctx, 12)),
                    com.oupao.wiz.Ui.dp(ctx, 10));
            layer.addView(bubble, blp2);
            bubble.setVisibility(8);

            var box = new android.widget.LinearLayout(ctx);
            box.setOrientation(android.widget.LinearLayout.VERTICAL);
            letterTitle = new TextView(ctx);
            letterTitle.setTextSize(13.0f);
            letterTitle.setTextColor(0xFFE8EAED);
            letterTitle.setShadowLayer(5f, 0f, 1f, 0xE6000000);
            box.addView(letterTitle, new android.widget.LinearLayout.LayoutParams(-1, -2));

            grid = new GridView(ctx);
            int gw = grid.getWantedW();
            int gh = grid.getWantedH();
            box.addView(grid, new android.widget.LinearLayout.LayoutParams(gw, gh));

            gridBaseTop = (barTop + ((barH - gh) / 2));
            var clp = new FrameLayout.LayoutParams(gw, -2);
            clp.gravity = (Gravity.TOP | (side > 0 ? Gravity.RIGHT : Gravity.LEFT));
            clp.topMargin = gridBaseTop;
            if (side > 0) {
                clp.rightMargin = (barW + com.oupao.wiz.Ui.dp(ctx, 12));
            } else {
                clp.leftMargin = (barW + com.oupao.wiz.Ui.dp(ctx, 12));
            }
            layer.addView(box, clp);
            box.setVisibility(8);

            var lp = new WindowManager.LayoutParams();
            lp.type = 2003;
            lp.format = -3;
            lp.width = -1;
            lp.height = -1;
            lp.flags = (0x100 | 0x8 | 0x400);
            lp.gravity = Gravity.TOP | Gravity.LEFT;
            barLp = lp;
            applyBlur(ctx, lp);
            try {
                lp.setTitle("oupao_idxbar");
            } catch (Throwable ignore) {
            }
            layer.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent e) {
                    try {
                        com.oupao.wiz.Idx.feed(e.getActionMasked(), e.getRawX(), e.getRawY(), side);
                    } catch (Throwable ignore) {
                    }
                    return true;
                }
            });
            wm.addView(layer, lp);
            rootView = layer;
            lastLayer = layer;   // 2834：留一份引用，drop() 兜底卸载用
            L("IdxBar: built " + sw + "x" + sh);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("IdxBar: build fail", t);
            hide();
        }
    }

    // ---------------- 数据 ----------------

    /**
     * 取某个分身用户下某包的启动器条目（拿到 getLabel() 和**系统 badged icon**）。
     *
     * ⚠️ 不能用 {@code LauncherApps.getApplicationInfo(...)}：它返回的是
     * {@link android.content.pm.ApplicationInfo}（没有 getLabel()/getBadgedIcon(int)），
     * 编译期就报「找不到符号」。要 LauncherActivityInfo 必须走 getActivityList(pkg, user)。
     */
    private static android.content.pm.LauncherActivityInfo laiOf(Context ctx, String pkg, int user) {
        try {
            var la = (android.content.pm.LauncherApps) ctx.getSystemService("launcherapps");
            var uh = com.oupao.wiz.FrameworkExt.userHandleOf(user);
            if ((la == null) || (uh == null)) {
                return null;
            }
            var acts = la.getActivityList(pkg, uh);
            if ((acts != null) && (acts.size() > 0)) {
                return acts.get(0);
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static String labelOfTag(Context ctx, String tag) {
        // ★ 工具（tool#xxx）：包名那套解不出来，以前会显示成字面量 "tool"。
        //   名字要走 Tools.label(tag)（工具名），和「更多应用」面板一致。
        if (tag != null && tag.startsWith("tool#")) {
            try {
                String l = com.oupao.wiz.Tools.label(tag);
                if ((l != null) && (l.length() > 0)) {
                    return l;
                }
            } catch (Throwable ignore) {
            }
            return tag.substring(5);
        }
        var pkg = com.oupao.wiz.Pins.pkg(tag);
        // 分身条目（pkg#cls#userId）：标签要问那个用户，否则拿到的是主用户的名字
        int user = userOfTag(tag);
        if (user > 0) {
            try {
                var info = laiOf(ctx, pkg, user);
                if (info != null) {
                    return String.valueOf(info.getLabel());
                }
            } catch (Throwable ignore) {
            }
        }
        try {
            var pm = ctx.getPackageManager();
            return String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
        } catch (Throwable t) {
            return pkg;
        }
    }

    /** 取某个包的桌面启动 Activity（最近应用那边只给了包名，cls 得自己解析） */
    private static String launchClsOf(Context ctx, String pkg) {
        // 最近应用最多 8 条，每次 buildData 都问一遍 PM 太浪费；同一个包极少变 → 直接缓存
        String hit = clsCache.get(pkg);
        if (hit != null) {
            return hit;
        }
        try {
            var pm = ctx.getPackageManager();
            var it = new android.content.Intent(android.content.Intent.ACTION_MAIN);
            it.addCategory(android.content.Intent.CATEGORY_LAUNCHER);
            it.setPackage(pkg);
            var list = pm.queryIntentActivities(it, 0);
            if ((list != null) && (list.size() > 0)) {
                var ri = (android.content.pm.ResolveInfo) list.get(0);
                if ((ri != null) && (ri.activityInfo != null)) {
                    clsCache.put(pkg, ri.activityInfo.name);
                    return ri.activityInfo.name;
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    /** 从 tag（`pkg#cls` 或 `pkg#cls#userId`）取分身用户号，主用户返回 0。 */
    private static int userOfTag(String tag) {
        try {
            var parts = tag.split("#");
            if (parts.length >= 3) {
                return Integer.parseInt(parts[2]);
            }
        } catch (Throwable ignore) {
        }
        return 0;
    }

    /**
     * 自己枚举可启动应用。
     *
     * 为什么不用 Pins.apps()：它有 60s 缓存，开机时枚举到不完整的列表后会一直返回那份残缺数据，
     * 重试也无效。这里直接问 PackageManager + LauncherApps（两路合并去重），
     * 并且**保留历史最多的一份**，避免某次返回少量时把已有数据变小。
     */
    /** loadApps() 的结果缓存（2918 审计第 5 项：buildData 增量）。
     *  以前每次 buildData 都全量枚举：queryIntentActivities + LauncherApps(当前用户) +
     *  LauncherApps(每个分身用户)，上百次 PM/LauncherApps IPC；而 buildData 在空闲时
     *  每 60s 就为「最近应用变了」跑一遍。现在枚举只做一次，包变化（PACKAGE_*）时失效。 */
    private static volatile ArrayList<String> appsCache;
    private static volatile long appsCacheAt;

    /** 包变化（安装/卸载/更新）→ 丢掉应用枚举缓存，下一次 buildData 重新枚举。
     *  调用方：Sug.onHookPkg（system_server 里 hook 了 sendPackageBroadcast）。 */
    static void invalidateApps() {
        try {
            if ((appsCache == null) && clsCache.isEmpty()) {
                return;          // 本来就没缓存，不用打日志（开机频繁来包广播时避免刷屏）
            }
            appsCache = null;
            appsCacheAt = 0L;
            dataAt = 0L;
            clsCache.clear();
            com.oupao.wiz.SLog.r("IdxApps", "IdxBar: 应用缓存已失效(包变化)", 3000L);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("IdxBar: invalidateApps 失败", t);
        }
    }

    /** launchClsOf 的小缓存：最近应用最多 8 条，每 60s 重建一次也会重复问 PM。
     *  ★ 必须并发安全：buildData 既在 Oupao-IdxHeavy 后台线程跑（refreshIfIdle），
     *    也在主线程跑（show），普通 HashMap 并发读写会损坏（甚至自旋）。 */
    private static final java.util.concurrent.ConcurrentHashMap<String, String> clsCache =
            new java.util.concurrent.ConcurrentHashMap<String, String>();

    private static ArrayList<String> loadApps(Context ctx) {
        var cached = appsCache;
        if ((cached != null) && ((android.os.SystemClock.elapsedRealtime() - appsCacheAt) < 600000L)) {
            return cached;   // 10 分钟内且没有包变化事件 → 直接复用
        }
        var out = new ArrayList<String>();
        var seen = new java.util.HashSet<String>();
        try {
            var pm = ctx.getPackageManager();
            var it = new android.content.Intent(android.content.Intent.ACTION_MAIN);
            it.addCategory(android.content.Intent.CATEGORY_LAUNCHER);
            var list = pm.queryIntentActivities(it, 0);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    var ri = (android.content.pm.ResolveInfo) list.get(i);
                    var ai = ri.activityInfo;
                    if (ai == null) {
                        continue;
                    }
                    var tag = ai.packageName + "#" + ai.name;
                    if (!seen.add(tag)) {
                        continue;
                    }
                    out.add(String.valueOf(ri.loadLabel(pm)) + "\u0001" + tag);
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("IdxBar: queryIntentAccounts fail", t);
        }
        try {
            var la = (android.content.pm.LauncherApps) ctx.getSystemService("launcherapps");
            if (la != null) {
                var acts = la.getActivityList(null, android.os.Process.myUserHandle());
                if (acts != null) {
                    for (int i = 0; i < acts.size(); i++) {
                        var ai = (android.content.pm.LauncherActivityInfo) acts.get(i);
                        try {
                            var act = ai.getComponentName();
                            var tag = act.getPackageName() + "#" + act.getClassName();
                            if (!seen.add(tag)) {
                                continue;
                            }
                            out.add(String.valueOf(ai.getLabel()) + "\u0001" + tag);
                        } catch (Throwable ignore) {
                        }
                    }
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("IdxBar: launcherApps fail", t);
        }
        // ★ 分身应用（ColorOS MultiApp 用户 997/998/999…）：
        //   上面两路都只问**当前用户**，分身一条也拿不到 ——
        //   用户反馈的「全局引索条没有分身 app」就是这个原因。
        //   和「更多应用」同一条路子（FlyH.cloneScan 也是这么枚举的），
        //   tag 带上 `#userId`，点击时 FlyH.launch 会自动走 cloneLaunch。
        //   名字保持原样（不加「（分身）」）：格子只有 4 个字宽，
        //   分身靠系统 badged icon 上的角标区分，与桌面/扇形菜单一致。
        try {
            var la2 = (android.content.pm.LauncherApps) ctx.getSystemService("launcherapps");
            var um = (android.os.UserManager) ctx.getSystemService("user");
            if ((la2 != null) && (um != null)) {
                var users = com.oupao.wiz.FrameworkExt.usersOf(um);
                var self = com.oupao.wiz.FrameworkExt.myUserId();
                int nc = 0;
                for (int ui = 0; ((users != null) && (ui < users.size())); ui++) {
                    int u = com.oupao.wiz.FrameworkExt.userInfoId(users.get(ui));
                    if ((u <= 0) || (u == self)) {
                        continue;
                    }
                    var uh = com.oupao.wiz.FrameworkExt.userHandleOf(u);
                    if (uh == null) {
                        continue;
                    }
                    var acts2 = la2.getActivityList(null, uh);
                    if (acts2 == null) {
                        continue;
                    }
                    for (int i = 0; i < acts2.size(); i++) {
                        try {
                            var ai2 = (android.content.pm.LauncherActivityInfo) acts2.get(i);
                            var cn = ai2.getComponentName();
                            if (cn == null) {
                                continue;
                            }
                            var tag = cn.getPackageName() + "#" + cn.getClassName() + "#" + u;
                            if (!seen.add(tag)) {
                                continue;
                            }
                            out.add(String.valueOf(ai2.getLabel()) + "\u0001" + tag);
                            nc++;
                        } catch (Throwable ignore) {
                        }
                    }
                }
                com.oupao.wiz.SLog.i("IdxBar: clones n=" + nc);
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("IdxBar: clones fail", t);
        }
        if ((lastApps != null) && (lastApps.size() > out.size())) {
            com.oupao.wiz.SLog.i("IdxBar: keep bigger list " + lastApps.size() + " > " + out.size());
            appsCache = lastApps;
            appsCacheAt = android.os.SystemClock.elapsedRealtime();
            return lastApps;
        }
        lastApps = out;
        // 缓存这次枚举结果：之后 buildData 只做分组（不再枚举包），直到包变化或 10 分钟过期
        appsCache = out;
        appsCacheAt = android.os.SystemClock.elapsedRealtime();
        com.oupao.wiz.SLog.i("IdxBar: loadApps n=" + out.size());
        return out;
    }

    private static void buildData(Context ctx) {
        letters.clear();
        groups.clear();
        dataGen++;
        try {
            // 按当前「更多应用方式」取对应那一套固定应用
            // 索引条自己的固定应用（与「更多应用」面板那套独立）
            var pinned = com.oupao.wiz.Pins.parse(com.oupao.wiz.Cfg.idxPins());
            // ★ 常驻：它既是「已固定」分组，也是长按 2 秒呼出索引条面板的唯一入口。
            // 一旦固定应用全部清空就把 ★ 也去掉的话，就再也进不去面板了。
            var list = new ArrayList<Object[]>();
            if (pinned != null) {
                for (int i = 0; i < pinned.size(); i++) {
                    var tag = (String) pinned.get(i);
                    list.add(new Object[]{tag, labelOfTag(ctx, tag)});
                }
            }
            // 最近应用（数据来自系统侧边栏，见 SideDock.dumpRecents → Cfg "recents"）
            // ★ 用户要求：放到 **#** 分组里（不再挂在 ★ 下）。
            var recItems = new ArrayList<Object[]>();
            try {
                var rec = com.oupao.wiz.Cfg.getStrFresh("recents", "");
                if ((rec != null) && (rec.trim().length() > 0)) {
                    var seenR = new java.util.HashSet<String>();
                    if (pinned != null) {
                        for (int i = 0; i < pinned.size(); i++) {
                            seenR.add(String.valueOf(pinned.get(i)));
                        }
                    }
                    var lines = rec.split("\n");
                    for (int i = 0; (i < lines.length) && (recItems.size() < 8); i++) {
                        var line = lines[i];
                        int c = line.indexOf('\u0001');
                        if (c <= 0) {
                            continue;
                        }
                        var label = line.substring(0, c).trim();
                        var pkg = line.substring(c + 1).trim();
                        if ((pkg.length() == 0) || (label.length() == 0)) {
                            continue;
                        }
                        var cls = launchClsOf(ctx, pkg);
                        if (cls == null) {
                            continue;
                        }
                        var tag = pkg + "#" + cls;
                        if (!seenR.add(tag)) {
                            continue;
                        }
                        recItems.add(new Object[]{tag, label});
                    }
                    L("IdxBar: 最近应用 n=" + recItems.size());
                }
            } catch (Throwable ignore) {
            }
            letters.add("★");
            groups.put("★", list);
            var apps = loadApps(ctx);
            var pinnedSet = new java.util.HashSet<String>(
                    pinned == null ? new ArrayList<String>() : pinned);
            var tmp = new HashMap<String, ArrayList<Object[]>>();
            for (int i = 0; ((apps != null) && (i < apps.size())); i++) {
                String pin = apps.get(i);
                int c = pin.indexOf('\u0001');
                String label = (c > 0) ? pin.substring(0, c) : pin;
                String tag = (c > 0) ? pin.substring(c + 1) : pin;
                // ★ 不再因为「已固定」就把应用从字母分组里剔除：
                //   否则某个字母下的应用全被固定时，那个字母会整块消失
                //   （用户反馈「全局引索条字母 A 没有了」就是这么来的）。
                //   现在 ★ 只是一个快捷分组，应用同时保留在它自己的字母下。
                String lt = com.oupao.wiz.PinUI.firstLetter(label);
                var l = tmp.get(lt);
                if (l == null) {
                    l = new ArrayList<Object[]>();
                    tmp.put(lt, l);
                }
                l.add(new Object[]{tag, label});
            }
            String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ#";
            for (int i = 0; i < alpha.length(); i++) {
                String ch = String.valueOf(alpha.charAt(i));
                var l = tmp.get(ch);
                if ((l != null) && (l.size() > 0)) {
                    letters.add(ch);
                    groups.put(ch, l);
                }
            }
            var sb = new StringBuilder();
            for (int i = 0; i < letters.size(); i++) {
                var g = groups.get(letters.get(i));
                sb.append(letters.get(i)).append((g == null) ? 0 : g.size()).append(' ');
            }
            // ★ 最近应用挂到 # 分组末尾（用户要求），带一个分区标题
            if (recItems.size() > 0) {
                var g = groups.get("#");
                if (g == null) {
                    g = new ArrayList<Object[]>();
                    groups.put("#", g);
                    letters.add("#");
                }
                g.add(new Object[]{"@h:\u6700\u8fd1\u5e94\u7528", "\u6700\u8fd1\u5e94\u7528"});
                g.addAll(recItems);
                sb.append("#+最近").append(recItems.size());
            }
            com.oupao.wiz.SLog.i("IdxBar: groups " + sb);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("IdxBar: buildData fail", t);
        }
    }

    // ---------------- 手势 ----------------

    // ---------------- ★ 长按 2 秒 → 呼出索引条自己的固定应用面板 ----------------


    private static final long STAR_HOLD_MS = 2000L;
    private static boolean starArmed;
    private static boolean panelOpened;
    /** 期望触发时刻（uptimeMillis）；重新计时会推后，旧定时器因此作废 */
    private static long starDueAt;

    private static final Runnable STAR_OPEN = new Runnable() {
        public void run() {
            try {
                if (!starArmed) {
                    return;
                }
                long now = android.os.SystemClock.uptimeMillis();
                if ((now + 40L) < starDueAt) {
                    // 旧定时器提前醒了（手指重新进入过 ★）→ 按剩余时间重挂
                    if (rootView != null) {
                        rootView.postDelayed(this, starDueAt - now);
                    }
                    return;
                }
                starArmed = false;
                starDueAt = 0L;
                panelOpened = true;
                hide();
                L("IdxBar: ★ 长按 2s 呼出索引条固定应用面板");
                com.oupao.wiz.MorePanel.showIdx();
            } catch (Throwable t) {
            }
        }
    };

    /** 手指停在 ★ 上开始计时；离开 ★ 立刻作废 */
    private static void armStar(boolean on) {
        try {
            if (on) {
                if (starArmed) {
                    return;
                }
                starArmed = true;
                starDueAt = android.os.SystemClock.uptimeMillis() + STAR_HOLD_MS;
                if (rootView != null) {
                    rootView.postDelayed(STAR_OPEN, STAR_HOLD_MS);
                }
            } else {
                starArmed = false;
                starDueAt = 0L;
            }
        } catch (Throwable t) {
        }
    }

    static boolean setByY(int y) {
        try {
            long t = android.os.SystemClock.uptimeMillis();
            if ((t - lastMoveAt) < 6L) {
                return false;
            }
            lastMoveAt = t;
            int n = letters.size();
            if ((n <= 0) || (rootView == null) || (rootView.getVisibility() != 0)) {
                return false;
            }
            int i = (int) ((((long) (y - barTop)) * n) / Math.max(1, barH));
            if (i < 0) {
                i = 0;
            }
            if (i >= n) {
                i = (n - 1);
            }
            if (i == curIdx) {
                return false;
            }
            // 迟滞（死区）：刚进入新格子时不立刻切换，手指要越过格子中线附近一段才算数。
            // 不然指尖一抖就会跳到下一个字母。
            if (curIdx >= 0) {
                int cell = Math.max(1, barH / Math.max(1, n));
                int slop = (int) (cell * 0.30f);
                if (i > curIdx) {
                    // 向下跨：需越过新格子上边界 + slop
                    int boundary = barTop + (i * cell);
                    if (y < (boundary + slop)) {
                        return false;
                    }
                } else {
                    // 向上跨：需越过新格子下边界 - slop
                    int boundary = barTop + ((i + 1) * cell);
                    if (y > (boundary - slop)) {
                        return false;
                    }
                }
            }
            curIdx = i;
            armStar("\u2605".equals(letters.get(i)));
            if (barView != null) {
                barView.invalidate();
            }
            showGroup(i);
            if ((t - lastBuzzAt) > 60L) {
                lastBuzzAt = t;
                try {
                    // 振动等级 2s 内复用：以前每换一个字母都去读一次配置（走文件），
                    // 快速拖动时这就是实打实的抖动来源。
                    if ((t - vibCfgAt) > 2000L) {
                        vibCfgAt = t;
                        vibCfgVal = com.oupao.wiz.Cfg.pinVibLevel();
                    }
                    int lv = vibCfgVal;
                    if (lv > 0) {
                        com.oupao.wiz.Idx.buzzAsync(lv);
                    }
                } catch (Throwable ignore) {
                }
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    // ---------------- 停留 1.5s → 全屏打开 ----------------

    /** 停留多久算「选中」→ 全屏打开（松手 = 小窗，见 Idx.launch） */
    private static final long HOLD_MS = 1500L;
    private static android.os.Handler holdH;
    private static Runnable holdRun;
    private static int holdIdx = -1;

    private static void armHold(Object[] app, int idx) {
        cancelHold();
        if ((app == null) || (idx < 0)) {
            return;
        }
        final String tag = String.valueOf(app[0]);
        holdIdx = idx;
        if (holdH == null) {
            holdH = new android.os.Handler(android.os.Looper.getMainLooper());
        }
        holdRun = new Runnable() {
            public void run() {
                holdRun = null;
                try {
                    if ((hoveredIdx != holdIdx) || (rootView == null)
                            || (rootView.getVisibility() != 0)) {
                        return;
                    }
                    L("IdxBar: 停留 1.5s → 全屏打开 " + tag);
                    try {
                        com.oupao.wiz.Idx.buzzAsync(2);
                    } catch (Throwable ignore) {
                    }
                    hide();
                    launchFull(tag);
                } catch (Throwable t) {
                    com.oupao.wiz.SLog.e("IdxBar: 停留启动失败", t);
                }
            }
        };
        holdH.postDelayed(holdRun, HOLD_MS);
    }

    private static void cancelHold() {
        try {
            if ((holdRun != null) && (holdH != null)) {
                holdH.removeCallbacks(holdRun);
            }
        } catch (Throwable ignore) {
        }
        holdRun = null;
        holdIdx = -1;
    }

    /**
     * 以**全屏**打开（不在小窗里）。
     *
     * 走 `Cfg.sysContext().startActivity(...)`：调用方是 system uid，
     * `CrossZoom` 的 SKIP_CALLERS 会放行，不会把小窗规则套上去。
     * 分身条目用 `LauncherApps.startMainActivity`（带 userId），同样不带窗口模式 = 全屏。
     */
    private static void launchFull(String tag) {
        try {
            var ctx = (appCtx != null) ? appCtx : com.oupao.wiz.Cfg.sysContext();
            if (ctx == null) {
                return;
            }
            if (tag.startsWith("tool#")) {
                com.oupao.wiz.Tools.fire(ctx, tag.substring(5));
                return;
            }
            var parts = tag.split("#");
            if (parts.length < 2) {
                return;
            }
            var pkg = parts[0];
            var cls = parts[1];
            int user = 0;
            if (parts.length >= 3) {
                try {
                    user = Integer.parseInt(parts[2]);
                } catch (Throwable ignore) {
                }
            }
            var cn = new android.content.ComponentName(pkg, cls);
            if (user > 0) {
                var la = (android.content.pm.LauncherApps) ctx.getSystemService("launcherapps");
                var uh = com.oupao.wiz.FrameworkExt.userHandleOf(user);
                if ((la != null) && (uh != null)) {
                    var opts = android.app.ActivityOptions.makeBasic();
                    opts.setLaunchDisplayId(0);
                    la.startMainActivity(cn, uh, null, opts.toBundle());
                    L("IdxBar: 全屏(分身)启动 " + tag);
                    return;
                }
            }
            var it = new android.content.Intent(android.content.Intent.ACTION_MAIN);
            it.addCategory(android.content.Intent.CATEGORY_LAUNCHER);
            it.setComponent(cn);
            it.setPackage(pkg);
            it.addFlags(268435456);
            ctx.startActivity(it);
            L("IdxBar: 全屏启动 " + tag);
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("IdxBar: 全屏启动失败", t);
        }
    }

    static void hover(int x, int y) {
        try {
            if ((grid == null) || (rootView == null) || (rootView.getVisibility() != 0)) {
                return;
            }
            var loc = new int[2];
            grid.getLocationOnScreen(loc);
            int h = grid.hitTest((x - loc[0]), (y - loc[1]));
            if (h == hoveredIdx) {
                return;
            }
            hoveredIdx = h;
            grid.setHover(h);
            // ★ 选中（悬停到）一个应用时给一次振动（用户要求）。
            //   强度沿用「索引滑动」的振动设置（Cfg.pinVibLevel），和换字母一致。
            if (h >= 0) {
                try {
                    long now = android.os.SystemClock.uptimeMillis();
                    if ((now - lastBuzzAt) > 40L) {
                        lastBuzzAt = now;
                        if ((now - vibCfgAt) > 2000L) {
                            vibCfgAt = now;
                            vibCfgVal = com.oupao.wiz.Cfg.pinVibLevel();
                        }
                        int lv = vibCfgVal;
                        if (lv > 0) {
                            com.oupao.wiz.Idx.buzzAsync(lv);
                        }
                    }
                } catch (Throwable ignore) {
                }
            }
            // ★ 停在某个 app 上 1.5s → 直接全屏打开（松手仍然是原来的小窗打开）
            try {
                var list2 = ((curIdx >= 0) && (curIdx < letters.size()))
                        ? groups.get(letters.get(curIdx)) : null;
                Object[] app = ((list2 != null) && (h >= 0) && (h < list2.size())) ? list2.get(h) : null;
                armHold(app, h);
            } catch (Throwable ignore) {
            }
        } catch (Throwable t) {
        }
    }

    static void release() {
        try {
            armStar(false);
            if (panelOpened) {
                panelOpened = false;
                hoveredIdx = -1;
                return;
            }
            int h = hoveredIdx;
            var list = ((curIdx >= 0) && (curIdx < letters.size()))
                    ? groups.get(letters.get(curIdx)) : null;
            Object[] app = null;
            if ((list != null) && (h >= 0) && (h < list.size())) {
                app = list.get(h);
            }
            hoveredIdx = -1;
            hide();
            if (app != null) {
                // 只有真的选中了才打日志：release() 会被感应条区域内任何一次抬手触发，
                // 无条件打印会把 oupao.log 刷爆（实测半小时 458 条全是没有悬停的）。
                L("IdxBar: release hovered=" + app[1]);
                com.oupao.wiz.Idx.launch(app);
            }
        } catch (Throwable t) {
            hide();
        }
    }

    private static void showGroup(int i) {
        try {
            if ((grid == null) || (i < 0) || (i >= letters.size())) {
                return;
            }
            cancelHold();
            var lt = letters.get(i);
            var list = groups.get(lt);
            hoveredIdx = -1;
            var box = (android.view.View) grid.getParent();
            if (box != null) {
                box.setVisibility(0);
            }
            letterTitle.setText(lt.equals("\u2605") ? "\u56fa\u5b9a\u5e94\u7528" : lt);
            if (bubble != null) {
                bubble.setText(lt);
                bubble.setVisibility(lt.equals("\u2605") ? 8 : 0);
            }
            grid.setData(list, lt);
            // 卡片按选中字母定位（居中在该字母旁边）
            try {
                int cell = (barH / Math.max(1, letters.size()));
                int cy = (barTop + (i * cell) + (cell / 2));
                var gp = (FrameLayout.LayoutParams) box.getLayoutParams();
                // box = 标题 + 网格；要让「app 网格」与所选字母同高，
                // 原来对齐的是 box 顶部（标题占了约 20dp），所以 app 整体偏下
                int titleH = 0;
                try {
                    if (letterTitle != null) {
                        titleH = letterTitle.getHeight();
                    }
                } catch (Throwable ignore) {
                }
                if (titleH <= 0) {
                    titleH = com.oupao.wiz.Ui.dp(box.getContext(), 22);
                }
                // ★ 高度必须用**当前这组数据**的实际高度（grid.setData 刚按内容算过）。
                //   原来用的是 build 时那个空网格的 LayoutParams 高度，
                //   内容多的字母（比如 ★ 有固定应用+最近应用）会被判断成"放得下"，
                //   结果往下溢出屏幕、底部被切掉。
                int gridH = grid.getWantedH();
                int boxH = titleH + gridH;
                // ★ box 的高度必须**显式**改成当前内容的实际高度：
                //   以前 box 是 wrap_content，只改 grid 的 LayoutParams 时布局不一定及时重跑，
                //   结果 ★ 这种行数多的分组被按 4 行高度裁剪 —— 最底下的文字被切掉
                //   （用户反馈「星号页最底下文字被遮挡」）。
                try {
                    var blp = (FrameLayout.LayoutParams) box.getLayoutParams();
                    if ((blp != null) && (blp.height != boxH)) {
                        blp.height = boxH;
                        box.setLayoutParams(blp);
                    }
                    box.requestLayout();
                } catch (Throwable ignore) {
                }
                // ★ 让「分区字母」的中心对准引索条上该字母的中心（以前是网格顶对齐字母，
                //   看着总是偏上；后半段还会因为下面这步被压得更上）。
                int target = (cy - (titleH / 2));
                int top = com.oupao.wiz.Ui.dp(box.getContext(), 30);
                int margin = com.oupao.wiz.Ui.dp(box.getContext(), 24);
                if (target < top) {
                    target = top;
                }
                // ★ 用户要求：内容太多放不下时**自动往上顶**（而不是硬停在某个 max 上），
                //   保证这一组整体始终在屏幕内，同时尽量贴着当前字母。
                int bottomLimit = (sh - boxH - margin);
                if ((bottomLimit > top) && (target > bottomLimit)) {
                    target = bottomLimit;
                }
                box.setTranslationY((target - gridBaseTop));
            } catch (Throwable ignore) {
            }

        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("IdxBar: showGroup fail", t);
        }
    }

    /** 后台预取全部图标：否则第一次换到某字母要现调 PackageManager（每次 100ms+ 的卡顿） */
    private static void prefetchIcons() {
        try {
            final int icon = com.oupao.wiz.Ui.dp(appCtx, ICON_DP);
            final long sig = dataGen;
            if (prefetching || (prefetchSig == sig)) {
                return;
            }
            prefetchSig = sig;
            prefetching = true;
            new Thread(new Runnable() {
                public void run() {
                    try {
                        var ctx = appCtx;
                        int n = 0;
                        for (int i = 0; i < letters.size(); i++) {
                            var list = groups.get(letters.get(i));
                            if (list == null) {
                                continue;
                            }
                            for (int k = 0; k < list.size(); k++) {
                                iconOf(ctx, String.valueOf(list.get(k)[0]), icon);
                                n++;
                                if (n > 400) {
                                    break;
                                }
                            }
                        }
                        L("IdxBar: prefetch icons n=" + n);
                    } catch (Throwable ignore) {
                    } finally {
                        prefetching = false;
                    }
                }
            }, "oupao-idx-prefetch").start();
        } catch (Throwable t) {
        }
    }

    // ---------------- 图标位图缓存 ----------------

    /**
     * 图标位图缓存。
     *
     * 上限 96 个（约 96 × 44dp² ≈ 7MB ARGB_8888）：设备上通常 100+ app，
     * 每个字母分组最多也就十几个，不设上限的话 HashMap 会随用户滑过的字母一直涨。
     * 与 {@link Sug} 的 badgeIcons 保持同一策略（超限整体清空，简单且不会抖）。
     */
    private static final int ICON_CACHE_MAX = 96;

    private static Bitmap iconOf(Context ctx, String tag, int size) {
        try {
            boolean isTool = (tag != null) && tag.startsWith("tool#");
            var pkg = isTool ? tag : com.oupao.wiz.Pins.pkg(tag);
            int user = userOfTag(tag);
            // 分身的图标和主应用不同（带角标），缓存键必须带 userId
            var key = pkg + "@" + user + "@" + size;
            var b = iconCache.get(key);
            if (b != null) {
                return b;
            }
            Drawable d = null;
            if (isTool) {
                // ★ 工具（tool#xxx）：图标来自侧边栏的资源，走 Tools.drawable（和面板一致）。
                //   以前按包名取 → "tool" 不是包名 → 图标为空，只剩一行字。
                try {
                    d = com.oupao.wiz.Tools.drawable(ctx, tag);
                } catch (Throwable ignore) {
                }
            } else if (user > 0) {
                // 分身：用该用户下的系统 badged icon —— 里面自带系统画的分身角标
                // （与桌面、扇形菜单、消息气泡同一套做法，不自己画「分」字）
                try {
                    var info = laiOf(ctx, pkg, user);
                    if (info != null) {
                        d = info.getBadgedIcon(ctx.getResources().getDisplayMetrics().densityDpi);
                    }
                } catch (Throwable ignore) {
                }
            }
            if ((d == null) && !isTool) {
                d = ctx.getPackageManager().getApplicationIcon(pkg);
            }
            if (d == null) {
                return null;
            }
            b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            try {
                var c = new Canvas(b);
                if (isTool) {
                    // ★ 工具图标要「和 app 图标一样的观感」：app 图标（自适应图标）本身
                    //   自带留白 + 圆角，而侧边栏的工具图标是**满幅方块**（直接画会又大又是直角，
                    //   和旁边的 app 图标完全不搭）。这里按同一比例缩进去 + 圆角裁剪。
                    float inset = (size * 0.06f);
                    float r = ((size - (inset * 2f)) * 0.24f);
                    var rect = new android.graphics.RectF(inset, inset, (size - inset), (size - inset));
                    var path = new android.graphics.Path();
                    path.addRoundRect(rect, r, r, android.graphics.Path.Direction.CW);
                    int save = c.save();
                    c.clipPath(path);
                    d.setBounds((int) rect.left, (int) rect.top, (int) rect.right, (int) rect.bottom);
                    d.draw(c);
                    c.restoreToCount(save);
                } else {
                    d.setBounds(0, 0, size, size);
                    d.draw(c);
                }
            } catch (Throwable t) {
                // 画到一半失败：这个 bitmap 没进缓存，必须自己回收，否则每次重试都漏一个
                b.recycle();
                return null;
            }
            iconCache.put(key, b);
            if (iconCache.size() > ICON_CACHE_MAX) {
                iconCache.clear();
            }
            return b;
        } catch (Throwable t) {
            return null;
        }
    }

    // ---------------- 自绘网格 ----------------

    static final class GridView extends View {
        private final ArrayList<Object[]> apps = new ArrayList<Object[]>();
        private final ArrayList<Bitmap> bmps = new ArrayList<Bitmap>();
        private final Paint pText = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pBitmap = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final Rect rTmp = new Rect();
        private int icon;
        private int cellW;
        private int cellH;
        private int labelH;
        /** 分区标题行的高度：比图标行矮，这样「标题→下面图标」的间距和其它分区一致 */
        private int headH;
        /** 标题字号：和「固定应用」（外层 letterTitle 13sp）保持一致 */
        private float headTextSize;
        private float labelTextSize;
        private int hover = -1;
        private int[] appPos = new int[0];
        private int[] posApp = new int[COLS * MAX_ROWS];
        private int[] rowHead = new int[MAX_ROWS];
        private int[] rowTop = new int[MAX_ROWS];
        private int[] rowHeight = new int[MAX_ROWS];
        private int rowsTotal = ROWS;
        private int totalH;

        GridView(Context c) {
            super(c);
            icon = com.oupao.wiz.Ui.dp(c, ICON_DP);
            cellW = (icon + com.oupao.wiz.Ui.dp(c, 10));
            labelH = com.oupao.wiz.Ui.dp(c, 18);
            cellH = (icon + labelH);
            headH = com.oupao.wiz.Ui.dp(c, 30);
            headTextSize = android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_SP, 13f,
                    c.getResources().getDisplayMetrics());
            labelTextSize = com.oupao.wiz.Ui.dp(c, 10);
            pText.setColor(Color.WHITE);
            pText.setTextAlign(Paint.Align.CENTER);
            pText.setTextSize(labelTextSize);
            pText.setShadowLayer(4f, 0f, 1f, 0xE6000000);
            java.util.Arrays.fill(rowHeight, cellH);
            totalH = (cellH * ROWS);
        }

        int getWantedW() {
            return (cellW * COLS);
        }

        int getWantedH() {
            return (totalH > 0) ? totalH : (cellH * ROWS);
        }

        void setData(ArrayList<Object[]> list, String lt) {
            apps.clear();
            bmps.clear();
            if (list != null) {
                apps.addAll(list);
            }
            for (int i = 0; i < apps.size(); i++) {
                String t = String.valueOf(apps.get(i)[0]);
                if (t.startsWith("@h:")) {
                    bmps.add(null);          // 标题行不取图标
                } else {
                    bmps.add(iconOf(getContext(), t, icon));
                }
            }
            layout();
            hover = -1;
            // 行数变了要把自身高度也改掉（分区标题/内容多少都会影响）
            try {
                var lp = getLayoutParams();
                int want = getWantedH();
                if ((lp != null) && (lp.height != want)) {
                    lp.height = want;
                    setLayoutParams(lp);
                    requestLayout();
                }
            } catch (Throwable ignore) {
            }
            invalidate();
        }

        /**
         * 计算每个条目落在第几行、以及**每行的 y 与行高**。
         *
         * 标题行用较矮的 `headH`（不是整格 cellH），这样「分区标题 → 下面图标」
         * 的间距和「固定应用」那种外层标题一致，不会显得空一大截。
         */
        private void layout() {
            appPos = new int[apps.size()];
            java.util.Arrays.fill(appPos, -1);
            posApp = new int[COLS * MAX_ROWS];
            java.util.Arrays.fill(posApp, -1);
            rowHead = new int[MAX_ROWS];
            java.util.Arrays.fill(rowHead, -1);
            rowTop = new int[MAX_ROWS];
            rowHeight = new int[MAX_ROWS];
            java.util.Arrays.fill(rowHeight, cellH);
            int row = 0;
            int col = 0;
            int y = 0;
            for (int i = 0; i < apps.size(); i++) {
                String t = String.valueOf(apps.get(i)[0]);
                if (t.startsWith("@h:")) {
                    if (col != 0) {
                        col = 0;
                        y += cellH;
                        row++;
                    }
                    if (row < MAX_ROWS) {
                        rowHead[row] = i;
                        rowTop[row] = y;
                        rowHeight[row] = headH;
                        y += headH;
                    }
                    row++;
                } else {
                    if (row >= MAX_ROWS) {
                        continue;
                    }
                    if (col == 0) {
                        rowTop[row] = y;
                        rowHeight[row] = cellH;
                    }
                    int pos = (row * COLS) + col;
                    appPos[i] = pos;
                    posApp[pos] = i;
                    col++;
                    if (col >= COLS) {
                        col = 0;
                        y += cellH;
                        row++;
                    }
                }
            }
            if (col > 0) {
                y += cellH;
                row++;
            }
            rowsTotal = Math.max(1, row);
            totalH = Math.max(cellH, y);
        }

        void setHover(int h) {
            hover = h;
            invalidate();
        }

        int hitTest(int lx, int ly) {
            if ((lx < 0) || (ly < 0)) {
                return -1;
            }
            int col = (lx / cellW);
            if ((col < 0) || (col >= COLS)) {
                return -1;
            }
            int row = -1;
            for (int r = 0; r < rowsTotal; r++) {
                if ((ly >= rowTop[r]) && (ly < (rowTop[r] + rowHeight[r]))) {
                    row = r;
                    break;
                }
            }
            if ((row < 0) || (row >= rowHead.length)) {
                return -1;
            }
            if (rowHead[row] >= 0) {
                return -1;          // 标题行不可点
            }
            int pos = ((row * COLS) + col);
            if ((pos < 0) || (pos >= posApp.length)) {
                return -1;
            }
            return posApp[pos];
        }

        protected void onDraw(Canvas cv) {
            for (int r = 0; r < rowsTotal; r++) {
                int ry = rowTop[r];
                if ((r < rowHead.length) && (rowHead[r] >= 0)) {
                    // 分区标题：整行、左对齐；字号/颜色与外层「固定应用」标题一致
                    int hi = rowHead[r];
                    String label = String.valueOf(apps.get(hi)[1]);
                    pText.setTextAlign(Paint.Align.LEFT);
                    pText.setColor(0xFFE8EAED);
                    pText.setTextSize(headTextSize);
                    cv.drawText(label, com.oupao.wiz.Ui.dp(getContext(), 2),
                            (ry + (rowHeight[r] * 0.78f)), pText);
                    pText.setTextSize(labelTextSize);
                    pText.setTextAlign(Paint.Align.CENTER);
                    continue;
                }
                for (int col = 0; col < COLS; col++) {
                    int pos = (r * COLS) + col;
                    int i = ((pos >= 0) && (pos < posApp.length)) ? posApp[pos] : -1;
                    if (i < 0) {
                        continue;
                    }
                    boolean on = (i == hover);
                    int cx = (col * cellW);
                    var bmp = ((i < bmps.size()) ? bmps.get(i) : null);
                    if (bmp != null) {
                        int px = (cx + ((cellW - icon) / 2));
                        int py = (ry + (on ? (-com.oupao.wiz.Ui.dp(getContext(), 3)) : 0));
                        rTmp.set(px, py, (px + icon), (py + icon));
                        cv.drawBitmap(bmp, null, rTmp, pBitmap);
                    }
                    String label = String.valueOf(apps.get(i)[1]);
                    if (label.length() > 4) {
                        label = label.substring(0, 4);
                    }
                    pText.setColor(on ? 0xFF7FB2FF : Color.WHITE);
                    cv.drawText(label, (cx + (cellW / 2f)), (ry + icon + (labelH * 0.72f)), pText);
                }
            }
        }
    }

    // ---------------- 字母条（预渲染） ----------------

    static final class BarView extends View {
        private Bitmap cache;
        /** 选中字母用的画笔（字段化：以前每次 onDraw 都 new 一个，拖动时会掉帧） */
        private final Paint sel = new Paint(Paint.ANTI_ALIAS_FLAG);

        BarView(Context c) {
            super(c);
            sel.setTypeface(Typeface.DEFAULT_BOLD);
            sel.setTextAlign(Paint.Align.CENTER);
            // 选中字母：**只把字母本身染成蓝色，不画圆**
            sel.setColor(0xFF7FB2FF);
        }

        protected void onDraw(Canvas cv) {
            int n = letters.size();
            if (n <= 0) {
                return;
            }
            int w = getWidth();
            int h = getHeight();
            if ((w <= 0) || (h <= 0)) {
                return;
            }
            if ((cache == null) || (cache.getWidth() != w) || (cache.getHeight() != h)) {
                cache = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                var c2 = new Canvas(cache);
                var p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setTypeface(Typeface.DEFAULT_BOLD);
                p.setTextAlign(Paint.Align.CENTER);
                p.setColor(0xFFDADCE0);
                p.setShadowLayer(4f, 0f, 1f, 0xF2000000);
                float cell = (((float) h) / n);
                // 2849：字号按**当前格子高度**现算。
                //   letterTextPx 是 build() 时算的，而 letters 之后可能变化（idle refresh），
                //   格子变矮而字号不变 → 字母互相重叠、或最后几个被边界裁掉。
                float ts = Math.min(letterTextPx, cell * 0.86f);
                if (ts < 8f) {
                    ts = Math.min(8f, cell);
                }
                p.setTextSize(ts);
                for (int i = 0; i < n; i++) {
                    float cy = ((i * cell) + (cell / 2f));
                    float base = (cy - ((p.descent() + p.ascent()) / 2f));
                    c2.drawText(letters.get(i), (w / 2f), base, p);
                }
            }
            cv.drawBitmap(cache, 0, 0, null);
            if ((curIdx >= 0) && (curIdx < n)) {
                float cell = (((float) h) / n);
                float cy = ((curIdx * cell) + (cell / 2f));
                // 只重画选中的那个字母（蓝色），不画圆形背景
                sel.setTextSize(Math.min(letterTextPx, cell * 0.86f));
                float base = (cy - ((sel.descent() + sel.ascent()) / 2f));
                cv.drawText(letters.get(curIdx), (w / 2f), base, sel);
            }
        }
    }
}
