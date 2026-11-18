package com.oupao.wiz;

import android.content.Intent;
import android.os.Handler;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * 3303：**小窗开窗总调度（重构版）**。
 *
 * ## 为什么重写
 *
 * 之前「关旧窗 / 上限收敛 / 聚焦已有窗 / 拦截 ROM 自动贴边」分散在
 * FlyH、Hooks、NoDock 三处，各自数窗口、各自关窗，互相打断：
 *
 *  - 连续呼出时前一次开窗还在入场动画里，下一次已经调 `startZoomWindow`；
 *  - 收敛按 taskId 排序，但 taskId 会被 ROM 复用（旧应用重新开出来 id 比新窗还小），
 *    偶尔把刚打开的那扇关掉；
 *  - 残留 task 被当成"已有小窗"→ `moveTaskToFront` 只把全屏应用提到前台，
 *    小窗根本没开出来（用户反馈"连小窗都打不开了"）。
 *
 * 3308：本调度**只在「上限=1」时启用**（`Cfg.zoomMaxSingle()`）。
 * 上限 >= 2 时完全走原生：原生到上限贴边、贴边满两个关最旧；模块只解除数量上限。
 *
 * ## 现在的模型：只有一条路（对齐官方时序）
 *
 *  1. 所有开窗请求都进 {@link #request}，**串行**执行；
 *  2. **先开新窗**，等它真正出现在屏幕上（最长 2s）；
 *  3. **再关掉超出的旧窗**（无动画、在新窗后面消失），保留刚打开的那扇；
 *  4. 忙的时候只保留**最后一个**请求（最新选择才是用户要的）；
 *  5. 目标应用已经有真实可见的小窗 → 只 `moveTaskToFront`，不重复开。
 *
 * 为什么必须先开再关：官方到上限时是"新窗先开出来、旧窗再转贴边/关闭"，
 * 画面始终有窗口。先关旧窗会让桌面回前台，新窗入场正好撞上桌面转场，
 * 动画被打断就留下半透明的"阴影"窗口（点一下才重排显示）。
 *
 * 「活着的小窗」判定用 ROM 自己的 `isFlexibleTaskAndHasCaption(task)` +
 * `Task.isVisible()`；手动贴边（zoomState==4）不算、不关。
 */
public final class ZoomGate {

    public interface Opener {
        void open();
    }

    private static final long DUP_WINDOW_MS = 3000L;
    private static final long CLOSE_DEDUP_MS = 1500L;

    private static final class Req {
        final String pkg;
        final int user;
        final Opener opener;

        Req(String pkg, int user, Opener opener) {
            this.pkg = pkg;
            this.user = user;
            this.opener = opener;
        }
    }

    private static Req sPending;
    private static boolean sBusy;
    private static String sBusyPkg;
    private static int sBusyUser = -1;
    private static long sBusyAt;

    private static int lastClosedTid;
    private static long lastClosedAt;

    /** 最近一次正常小窗的 Task bounds（仅用于通知气泡路径强制默认大小，避免继承聊天页的窄高比例） */
    private static android.graphics.Rect sGoodBounds;
    private static int sGoodScreenW = -1;
    private static int sGoodScreenH = -1;

    private ZoomGate() {
    }

    /**
     * 唯一入口。`opener` 里只做"真正调 startZoomWindow / 全屏转小窗"，
     * 不要在里面再做收敛或计数。
     *
     * @param user -1 = 主应用；>=0 = 分身用户
     */
    public static void request(final String pkg, final int user, final Opener opener) {
        // 3308：**只有上限=1** 才走本调度。
        //   2 及以上恢复原生行为（原生贴边/满两个关最旧），模块只负责解除上限。
        if (!com.oupao.wiz.Cfg.zoomMaxSingle()) {
            final Handler h0 = com.oupao.wiz.Hooks.main();
            if ((h0 != null) && (android.os.Looper.myLooper() != android.os.Looper.getMainLooper())) {
                h0.post(new Runnable() {
                    public void run() {
                        opener.open();
                    }
                });
            } else {
                opener.open();
            }
            return;
        }
        final Handler h = com.oupao.wiz.Hooks.main();
        if (h == null) {
            opener.open();
            return;
        }
        h.post(new Runnable() {
            public void run() {
                try {
                    onRequest(new Req(pkg, user, opener));
                } catch (Throwable t) {
                    com.oupao.wiz.SLog.e("Gate: 请求处理异常 pkg=" + pkg, t);
                    sBusy = false;
                    sPending = null;
                }
            }
        });
    }

    private static void onRequest(Req r) {
        long now = SystemClock.uptimeMillis();
        // 1) 同一个应用正在开 → 丢弃重复请求
        //    （实测扇形菜单一次选择会触发两次 launch；不丢就会开两扇）
        if (sBusy && (r.pkg != null) && r.pkg.equals(sBusyPkg)
                && (r.user < 0 || sBusyUser < 0 || r.user == sBusyUser)
                && ((now - sBusyAt) < DUP_WINDOW_MS)) {
            com.oupao.wiz.SLog.i("Gate: 丢弃重复请求(正在开) " + r.pkg);
            return;
        }
        // 2) 该应用已经有真实可见的小窗 → 只聚焦
        if (focusLive(r.pkg, r.user)) {
            return;
        }
        // 3) 忙 → 只保留最新请求
        if (sBusy) {
            sPending = r;
            com.oupao.wiz.SLog.i("Gate: 忙，暂存最新请求 " + r.pkg);
            return;
        }
        // 4) 空闲 → 执行
        execute(r);
    }

    /** 3466：每次执行递增。延迟收尾只在"还是最新一次请求"时才跑 */
    private static long sSeq;

    private static void execute(final Req r) {
        sBusy = true;
        sBusyPkg = r.pkg;
        sBusyUser = r.user;
        sBusyAt = SystemClock.uptimeMillis();
        final Handler h = com.oupao.wiz.Hooks.main();
        if (h == null) {
            r.opener.open();
            finish();
            return;
        }
        final long seq = ++sSeq;
        h.post(new Runnable() {
            public void run() {
                final Runnable open = new Runnable() {
                    public void run() {
                        try {
                            r.opener.open();
                        } catch (Throwable t) {
                            com.oupao.wiz.SLog.e("Gate: 开窗异常 " + r.pkg, t);
                        }
                        finish();
                        postOpenCheck(r, seq);
                    }
                };
                // 用户要求：限制=1 时固定「先关旧窗、等旧窗真的消失，再开新窗」。
                int closed = closeOthersNow(r.pkg, r.user);
                if (closed > 0) {
                    waitOthersGone(r.pkg, r.user, open);
                } else {
                    open.run();
                }
            }
        });
    }

    /** 开窗成功后 900ms 记录 bounds + 兜底收敛；seq 变了（用户又选了别的）就跳过。 */
    private static void postOpenCheck(final Req r, final long seq) {
        final Handler h = com.oupao.wiz.Hooks.main();
        if (h == null) {
            return;
        }
        h.postDelayed(new Runnable() {
            public void run() {
                if (seq != com.oupao.wiz.ZoomGate.sSeq) {
                    return;
                }
                Object t = findLiveTask(r.pkg, r.user);
                if (t == null) {
                    return;
                }
                noteGoodBounds(t);
                trimToMax(com.oupao.wiz.Win.taskId(t));
            }
        }, 900L);
    }

    /** 固定先关后开：等其它可见小窗都消失（最多 220ms）再执行 then。 */
    private static void waitOthersGone(final String pkg, final int user, final Runnable then) {
        final Handler h = com.oupao.wiz.Hooks.main();
        if (h == null) {
            then.run();
            return;
        }
        final long start = SystemClock.uptimeMillis();
        final Runnable[] poll = new Runnable[1];
        poll[0] = new Runnable() {
            public void run() {
                long spent = SystemClock.uptimeMillis() - start;
                if (!hasOtherLive(pkg, user) || spent >= 220L) {
                    com.oupao.wiz.SLog.i("Gate: 旧窗已清(" + spent + "ms) → 再开新窗 " + pkg);
                    then.run();
                } else {
                    h.postDelayed(poll[0], 16L);
                }
            }
        };
        h.post(poll[0]);
    }

    private static boolean hasOtherLive(String pkg, int user) {
        try {
            for (Object t : liveWindows()) {
                if (matches(t, pkg, user)) {
                    continue;
                }
                int zs = com.oupao.wiz.Win.zoomState(t);
                if ((zs != 0) && (zs != 4)) {
                    return true;
                }
            }
        } catch (Throwable ignore) {
        }
        return false;
    }

    /**
     * 3466：不等新窗可见，同一帧清掉其他"缩放态"小窗。
     *
     * 只关 `Win.zoomState != 0` 的 task：全屏/后台 task 不动（否则会杀掉应用）。
     * 用 `removeTask`（无退场动画），和原生"超限当场处理"一致；
     * 手动贴边浮条（zoomState==4）跳过。
     */
    private static int closeOthersNow(String pkg, int user) {
        try {
            ArrayList<Object> all = new ArrayList<Object>();
            HashSet<Integer> seen = new HashSet<Integer>();
            try {
                ArrayList<Object> roots = com.oupao.wiz.Hooks.allRootTasks();
                if (roots != null) {
                    for (Object t : roots) {
                        int id = com.oupao.wiz.Win.taskId(t);
                        if ((id > 0) && seen.add(Integer.valueOf(id))) {
                            all.add(t);
                        }
                    }
                }
            } catch (Throwable ignore) {
            }
            try {
                Class<?> c = Class.forName("android.app.ActivityTaskManager");
                Object svc = com.oupao.wiz.Rt.invoke(
                        com.oupao.wiz.Rt.methodByName(c, "getService", 0), null, null);
                Object list = com.oupao.wiz.Rt.callLoose(svc, "getTasks",
                        new Object[]{Integer.valueOf(30)});
                if (list instanceof List) {
                    for (Object info : (List<?>) list) {
                        Object ido = com.oupao.wiz.Rt.get(info, "taskId");
                        if (!(ido instanceof Number)) {
                            continue;
                        }
                        int id = ((Number) ido).intValue();
                        if (!seen.add(Integer.valueOf(id))) {
                            continue;
                        }
                        Object t = com.oupao.wiz.FlyH.taskById(id);
                        if (t != null) {
                            all.add(t);
                        }
                    }
                }
            } catch (Throwable ignore) {
            }
            int n = 0;
            for (Object t : all) {
                int id = com.oupao.wiz.Win.taskId(t);
                if (id <= 0) {
                    continue;
                }
                if (matches(t, pkg, user)) {
                    continue;                       // 目标应用自己的 task 不动
                }
                int zs = com.oupao.wiz.Win.zoomState(t);
                if ((zs == 0) || (zs == 4)) {
                    continue;                       // 全屏/后台 / 手动贴边
                }
                if (closeToBack(t)) {
                    n++;
                }
            }
            if (n > 0) {
                com.oupao.wiz.SLog.i("Gate: 先关旧窗 n=" + n + " 新=" + pkg);
            }
            return n;
        } catch (Throwable ignore) {
            return 0;
        }
    }

    private static void finish() {
        sBusy = false;
        sBusyPkg = null;
        sBusyUser = -1;
        Req next = sPending;
        sPending = null;
        if (next != null) {
            com.oupao.wiz.SLog.i("Gate: 执行排队请求 " + next.pkg);
            execute(next);
        }
    }

    // ───────────────────────────── 收敛 ─────────────────────────────

    /**
     * 开窗后：收敛到 `want`，**保留刚打开的那扇**（keepId）+ 焦点窗口。
     *
     * 3304：不再"开窗前先关"。官方时序是新窗先出来、旧窗后处理；
     * 先关会让桌面回前台，新窗入场撞上桌面转场 → 半透明阴影。
     */
    private static int trimToMax(int keepId) {
        try {
            if (!com.oupao.wiz.Cfg.zoomMaxOn()) {
                return 0;
            }
            int want = com.oupao.wiz.Cfg.zoomMax();
            if (want <= 0) {
                return 0;
            }
            ArrayList<Object> live = liveWindows();
            if (live.size() <= want) {
                if (com.oupao.wiz.SLog.dbgOn()) {
                    com.oupao.wiz.SLog.v("Gate: 收敛检查 live=" + live.size()
                            + " want=" + want + " → 正常");
                }
                return 0;
            }
            if (keepId <= 0) {
                keepId = focusedTaskId();
            }
            sortOldestFirst(live);
            int closed = 0;
            for (Object t : live) {
                if ((live.size() - closed) <= want) {
                    break;
                }
                int tid = com.oupao.wiz.Win.taskId(t);
                if ((keepId > 0) && (tid == keepId)) {
                    continue;                          // 刚打开的那扇不动
                }
                if (close(t)) {
                    closed++;
                }
            }
            com.oupao.wiz.SLog.i("Gate: 收敛 closed=" + closed + " live=" + live.size()
                    + " want=" + want + " keep=" + keepId);
            return closed;
        } catch (Throwable ignore) {
            return 0;
        }
    }

    // ───────────────────── 小窗跳转：新窗出现后收掉来源小窗 ─────────────────────

    /** 在 CrossZoom.fire 之前快照当前活着的小窗 id（C17） */
    static int[] snapshotLiveTaskIds() {
        try {
            // 只有「限制小窗数量=1」才需要收掉来源窗；>=2 保留多窗原生行为
            if (!com.oupao.wiz.Cfg.isC17() || !com.oupao.wiz.Cfg.zoomMaxSingle()) {
                return new int[0];
            }
            ArrayList<Object> live = liveWindows();
            int[] ids = new int[live.size()];
            for (int i = 0; i < live.size(); i++) {
                ids[i] = com.oupao.wiz.Win.taskId(live.get(i));
            }
            return ids;
        } catch (Throwable ignore) {
            return new int[0];
        }
    }

    /**
     * 应用 A（小窗）里点链接打开应用 B（也以小窗启动）后，关闭来源小窗 A。
     *
     * 只处理 fire 前就已经存在、且不属于目标应用 B 的旧任务；目标 B 出现前绝不关，
     * 避免 B 启动失败时把 A 也关掉（fail-open）。
     */
    static void closeOldAfterCrossZoom(final int[] oldIds, final String targetPkg, final int userId) {
        try {
            if (!com.oupao.wiz.Cfg.isC17() || !com.oupao.wiz.Cfg.zoomMaxSingle()
                    || oldIds == null || oldIds.length == 0 || targetPkg == null) {
                return;
            }
            final android.os.Handler h = com.oupao.wiz.Hooks.main();
            if (h == null) {
                return;
            }
            closeOldAfterCrossStep(oldIds, targetPkg, userId, h, 0);
        } catch (Throwable ignore) {
        }
    }

    private static void closeOldAfterCrossStep(final int[] oldIds, final String targetPkg,
                                               final int userId, final android.os.Handler h,
                                               final int attempt) {
        h.postDelayed(new Runnable() {
            public void run() {
                try {
                    Object target = findLiveTask(targetPkg, userId);
                    if (target == null) {
                        if (attempt < 4) {
                            closeOldAfterCrossStep(oldIds, targetPkg, userId, h, attempt + 1);
                        }
                        return;
                    }
                    int keepId = com.oupao.wiz.Win.taskId(target);
                    ArrayList<Object> live = liveWindows();
                    int closed = 0;
                    long now = android.os.SystemClock.uptimeMillis();
                    for (Object t : live) {
                        int id = com.oupao.wiz.Win.taskId(t);
                        if (id <= 0 || id == keepId || !containsId(oldIds, id)) {
                            continue;
                        }
                        if (closeViaStrategy(t, id, now)) {
                            closed++;
                        }
                    }
                    if (closed > 0) {
                        com.oupao.wiz.SLog.i("Gate: 小窗跳转已收来源窗 closed=" + closed
                                + " target=" + targetPkg + " keep=" + keepId);
                    }
                } catch (Throwable ignore) {
                }
            }
        }, attempt == 0 ? 600L : 300L);
    }

    private static boolean containsId(int[] ids, int id) {
        for (int v : ids) {
            if (v == id) {
                return true;
            }
        }
        return false;
    }

    // ───────────────────── startZoomWindow 的返回码语义 ─────────────────────

    /**
     * `OplusZoomWindowManager.startZoomWindow` 返回的是 `ActivityStarter.execute()` 的
     * start result：**0 = START_SUCCESS，2 = START_TASK_TO_FRONT，3 = START_DELIVERED_TO_TOP**。
     * 2/3 表示目标任务已经在 / 已提到前台，同样是**启动成功**；只有负数才是失败。
     *
     * 3391 只在 FlyH 里按这个口径判过；Sug 的通知气泡路径一直用 `!= 0 = 失败` →
     * 3413 用户日志实锤（竖屏/横屏各一次）：
     * ```
     * Launch: msg zoom result=2 caller=com.tencent.mobileqq clone=true
     * Launch: msg PI bounds sent=true
     * ```
     * 即：startZoomWindow 已经把小窗开出来了，却又被当成失败去走
     * {@code piBoundsSend()} 的 85%×85% 居中兜底 → **同一个通知被开两次**，
     * 最后露出来的是 85% 那个窗口（竖屏约 68% 宽、横屏约 50% 宽），两个方向都不对。
     */
    static boolean zoomRetOk(Object ret) {
        if (!(ret instanceof Number)) {
            return true;             // 没拿到返回值（null）时按「已调用」处理
        }
        int code = ((Number) ret).intValue();
        return (code == 0) || (code == 2) || (code == 3);
    }

    // ───────────────────────── 通知气泡默认小窗尺寸 ─────────────────────────

    /**
     * C17 通知气泡打开小窗时，聊天详情页会按自己的竖屏比例算出“窄高”窗口，
     * 看起来像细条。这里给通知气泡路径一个正常的默认 LaunchBounds：
     * 优先用最近一次正常小窗的 bounds；没有缓存时按本机竖屏默认比例算。
     *
     * 只对 C17 生效，C16 原样不动。
     */
    static android.graphics.Rect notificationLaunchBounds() {
        try {
            if (!com.oupao.wiz.Cfg.isC17()) {
                return null;
            }
            android.util.DisplayMetrics dm = com.oupao.wiz.Cfg.sysContext()
                    .getResources().getDisplayMetrics();
            int w = dm.widthPixels;
            int h = dm.heightPixels;
            if ((w <= 0) || (h <= 0)) {
                return null;
            }
            synchronized (ZoomGate.class) {
                if ((sGoodBounds != null) && (sGoodScreenW == w) && (sGoodScreenH == h)) {
                    return new android.graphics.Rect(sGoodBounds);
                }
            }
            // 竖屏兜底：ROM 默认小窗的 Task bounds 约为
            // left=10.1%W, top=11.5%H, width=W, height=81.9%H（scale≈0.798 后即为可视窗）。
            if (w < h) {
                int left = Math.round(w * 0.101f);
                int top = Math.round(h * 0.115f);
                return new android.graphics.Rect(left, top, left + w,
                        top + Math.round(h * 0.819f));
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    /** 记录一次正常小窗的 Task bounds，供通知气泡路径复用 */
    static void noteGoodBounds(Object task) {
        try {
            android.graphics.Rect b = taskBounds(task);
            if (b == null) {
                return;
            }
            android.util.DisplayMetrics dm = com.oupao.wiz.Cfg.sysContext()
                    .getResources().getDisplayMetrics();
            int w = dm.widthPixels;
            int h = dm.heightPixels;
            if ((w <= 0) || (h <= 0)) {
                return;
            }
            int bw = b.width();
            int bh = b.height();
            // 正常小窗：宽接近屏宽、高在屏幕一半以上；排除贴边细条/迷你残留
            if ((bw < w * 0.75f) || (bh < h * 0.45f)) {
                return;
            }
            synchronized (ZoomGate.class) {
                sGoodBounds = new android.graphics.Rect(b);
                sGoodScreenW = w;
                sGoodScreenH = h;
            }
        } catch (Throwable ignore) {
        }
    }

    private static android.graphics.Rect taskBounds(Object task) {
        try {
            Object b = com.oupao.wiz.Rt.callNoArg(task, "getBounds");
            return (b instanceof android.graphics.Rect)
                    ? new android.graphics.Rect((android.graphics.Rect) b) : null;
        } catch (Throwable ignore) {
            return null;
        }
    }

    // ───────────────────────── 全屏→小窗前先关旧窗 ─────────────────────────

    /**
     * 全屏应用转小窗前，先关掉上一个小窗（上限=1）。
     *
     * 只给 `FlyH.tryFullscreenToZoom()` 用：它已经确认目标就是当前全屏 task，
     * 此时关掉旧小窗，露出来的是目标全屏应用，不是桌面，所以不会出现
     * “先关旧窗 → 桌面回前台 → 新窗撞桌面转场”的阴影问题。
     *
     * @return 被关掉的旧 taskId（0 = 没有旧窗可关）。
     *         调用方必须等这个 task 真正退场结束（{@link #whenOldClosed}）再发起过渡。
     *
     * 3464：不再用固定 300ms。实测 C17 的 `ExitFlexibleTaskToBackStrategy` 退场过渡
     * 要 **~530ms**（logcat：`Transition requested (#165): TO_BACK` 31.950 →
     * `Transition animation finished (#165)` 32.497），而原来 300ms 就发
     * `switchFocusedTaskToZoom`，Shell 明确打：
     * ```
     * Transition (#166) ready while (#165) is still animating.
     * Notify the animating transition in case they can be merged
     * ```
     * 合并后 fullscreen→zoom 的动画被并进关窗过渡，窗口直接落位成小窗
     * （用户："全屏前台转小窗动画丢失、直接闪成小窗"）。
     */
    static int preCloseForFullscreenToZoom(int keepTid) {
        try {
            if (!com.oupao.wiz.Cfg.zoomMaxSingle()) {
                return 0;
            }
            ArrayList<Object> live = liveWindows();
            if (live.isEmpty()) {
                return 0;
            }
            sortOldestFirst(live);
            for (Object t : live) {
                int tid = com.oupao.wiz.Win.taskId(t);
                if ((tid <= 0) || (tid == keepTid)) {
                    continue;
                }
                if (closeAnimated(t)) {
                    com.oupao.wiz.SLog.i("Gate: 全屏→小窗 先关旧窗 tid=" + tid
                            + "（等退场结束再转）");
                    return tid;
                }
            }
        } catch (Throwable ignore) {
        }
        return 0;
    }

    // ─────────────── 3464：等旧窗退场结束 / 等过渡空闲 ───────────────

    /** 旧窗退场最长等这么久（实测 ~530ms，留一倍余量；超时也必须继续，不能卡死） */
    private static final long EXIT_WAIT_MS = 1000L;
    /** 没有旧窗时，若上一条过渡还在播，最多再等这么久（避免拖慢正常手感） */
    private static final long IDLE_WAIT_MS = 240L;
    private static final long EXIT_POLL_MS = 40L;

    /**
     * 3464：等旧窗的退场过渡真正结束再执行 `then`。
     *
     * 判据（必须同时满足）：
     *  1. 旧 task 已经取不到 / 不可见 / 已回到全屏（ROM 在 finishTransition 里
     *     `resetFlexibleTaskToFullScreen`）；
     *  2. TransitionController 没有 collecting/playing/queued 的过渡。
     *
     * 只看时间不够：同一台机上退场动画会被速度/应用启动拖长，固定值必然有时撞上。
     */
    static void whenOldClosed(final int tid, final int gen, final Runnable then) {
        final Handler h = com.oupao.wiz.Hooks.main();
        if (h == null) {
            then.run();
            return;
        }
        final long start = SystemClock.uptimeMillis();
        h.post(new Runnable() {
            public void run() {
                pollOldClosed(tid, gen, then, start);
            }
        });
    }

    private static void pollOldClosed(final int tid, final int gen,
                                      final Runnable then, final long start) {
        if (gen != com.oupao.wiz.FlyH.zoomGen) {
            com.oupao.wiz.SLog.i("Gate: 等旧窗退场时已被后一次呼出取代 tid=" + tid);
            return;
        }
        long waited = SystemClock.uptimeMillis() - start;
        Object task = com.oupao.wiz.FlyH.taskById(tid);
        boolean exited = (task == null) || (!taskVisible(task))
                || (com.oupao.wiz.Win.zoomState(task) == 0);
        boolean idle = !transitionBusy(task);
        if ((exited && idle) || (waited >= EXIT_WAIT_MS)) {
            com.oupao.wiz.SLog.i("Gate: 旧窗退场" + ((exited && idle) ? "结束" : "等超时")
                    + " " + waited + "ms tid=" + tid
                    + " exited=" + exited + " idle=" + idle);
            then.run();
            return;
        }
        Handler h = com.oupao.wiz.Hooks.main();
        if (h == null) {
            then.run();
            return;
        }
        h.postDelayed(new Runnable() {
            public void run() {
                pollOldClosed(tid, gen, then, start);
            }
        }, EXIT_POLL_MS);
    }

    /**
     * 3464：没有旧窗时，若 ROM 还有正在播的过渡，也稍等一下再发起。
     * 只等很短（240ms）：正常呼出时 TransitionController 已经是空闲的，不会拖慢手感；
     * 连续呼出时这点等待能避免新过渡被并进上一条（合并 = 没动画）。
     */
    static void whenTransitionsIdle(final int gen, final Runnable then) {
        // 正常情况（没有过渡在播）0ms 直接执行，不给手感加一帧
        if (!transitionBusy(null)) {
            then.run();
            return;
        }
        final Handler h = com.oupao.wiz.Hooks.main();
        if (h == null) {
            then.run();
            return;
        }
        final long start = SystemClock.uptimeMillis();
        h.post(new Runnable() {
            public void run() {
                pollIdle(gen, then, start);
            }
        });
    }

    private static void pollIdle(final int gen, final Runnable then, final long start) {
        if (gen != com.oupao.wiz.FlyH.zoomGen) {
            com.oupao.wiz.SLog.i("Gate: 等过渡空闲时已被后一次呼出取代");
            return;
        }
        long waited = SystemClock.uptimeMillis() - start;
        if (!transitionBusy(null) || (waited >= IDLE_WAIT_MS)) {
            if (waited > EXIT_POLL_MS) {
                com.oupao.wiz.SLog.i("Gate: 过渡空闲 等了 " + waited + "ms");
            }
            then.run();
            return;
        }
        Handler h = com.oupao.wiz.Hooks.main();
        if (h == null) {
            then.run();
            return;
        }
        h.postDelayed(new Runnable() {
            public void run() {
                pollIdle(gen, then, start);
            }
        }, EXIT_POLL_MS);
    }

    /**
     * ROM 是否还有 collecting / playing / queued 的窗口过渡。
     *
     * 用 `WindowContainer.mTransitionController`（C17 services 反编译实证字段）：
     * `mCollectingTransition`(Transition) / `mPlayingTransitions`(ArrayList) /
     * `mQueuedTransitions`(ArrayList)。取不到一律当空闲 —— 反射失败不能把开窗卡死。
     */
    static boolean transitionBusy(Object anyTask) {
        try {
            Object tc = (anyTask != null)
                    ? com.oupao.wiz.Rt.get(anyTask, "mTransitionController") : null;
            if (tc == null) {
                Object t = com.oupao.wiz.FlyH.topTask();
                if (t != null) {
                    tc = com.oupao.wiz.Rt.get(t, "mTransitionController");
                }
            }
            if (tc == null) {
                return false;
            }
            if (com.oupao.wiz.Rt.get(tc, "mCollectingTransition") != null) {
                return true;
            }
            if (nonEmpty(com.oupao.wiz.Rt.get(tc, "mPlayingTransitions"))) {
                return true;
            }
            if (nonEmpty(com.oupao.wiz.Rt.get(tc, "mQueuedTransitions"))) {
                return true;
            }
        } catch (Throwable ignore) {
        }
        return false;
    }

    private static boolean nonEmpty(Object list) {
        return (list instanceof java.util.Collection)
                && !((java.util.Collection<?>) list).isEmpty();
    }

    /** 关旧窗但保留 ROM 的退场动画（不能走 removeTask，否则会瞬间消失） */
    private static boolean closeAnimated(Object task) {
        try {
            if (task == null) {
                return false;
            }
            final int tid = com.oupao.wiz.Win.taskId(task);
            if (tid <= 0) {
                return false;
            }
            long now = android.os.SystemClock.uptimeMillis();
            if ((tid == lastClosedTid) && ((now - lastClosedAt) < CLOSE_DEDUP_MS)) {
                return false;
            }
            Object ctrl = flexibleController();
            if (ctrl == null) {
                return false;
            }
            Class<?> strat = com.oupao.wiz.Rt.cls(com.oupao.wiz.Hooks.CL,
                    "com.android.server.wm.ExitFlexibleTaskToBackStrategy");
            if (strat == null) {
                return false;
            }
            // 2 参就是「退到后台、带过渡动画」；4 参是后面几个 ROM 的兼容写法
            Object st = com.oupao.wiz.Rt.newInstance(strat,
                    new Object[]{task, Boolean.TRUE});
            if (st == null) {
                st = com.oupao.wiz.Rt.newInstance(strat,
                        new Object[]{task, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE});
            }
            if (st == null) {
                return false;
            }
            com.oupao.wiz.Rt.callLoose(st, "setSourceFrom",
                    new Object[]{Integer.valueOf(512)});
            com.oupao.wiz.Rt.callLoose(ctrl, "exitFlexibleTaskWindowInnerLocked",
                    new Object[]{st});
            lastClosedTid = tid;
            lastClosedAt = now;
            com.oupao.wiz.SLog.i("Gate: 已先关旧窗 tid=" + tid);
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }

    // ───────────────────────────── 聚焦 ─────────────────────────────

    /** 该应用已经有真实可见的小窗 → 提到前台（不会把后台全屏任务提上来） */
    private static boolean focusLive(String pkg, int user) {
        try {
            if (pkg == null) {
                return false;
            }
            for (Object t : liveWindows()) {
                if (!matches(t, pkg, user)) {
                    continue;
                }
                final int tid = com.oupao.wiz.Win.taskId(t);
                Class<?> c = Class.forName("android.app.ActivityTaskManager");
                Object svc = com.oupao.wiz.Rt.invoke(
                        com.oupao.wiz.Rt.methodByName(c, "getService", 0), null, null);
                com.oupao.wiz.Rt.callLoose(svc, "moveTaskToFront",
                        new Object[]{Integer.valueOf(tid), Integer.valueOf(0), null});
                com.oupao.wiz.SLog.i("Gate: 已有小窗 → 聚焦 tid=" + tid + " pkg=" + pkg);
                return true;
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("Gate: 聚焦失败 pkg=" + pkg, t);
        }
        return false;
    }

    private static Object findLiveTask(String pkg, int user) {
        try {
            if (pkg == null) {
                return null;
            }
            for (Object t : liveWindows()) {
                if (matches(t, pkg, user)) {
                    return t;
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    // ───────────────────────────── 枚举 ─────────────────────────────

    /** 当前**真实可见**的小窗任务（ROM 判定 + isVisible，贴边浮条不算） */
    static ArrayList<Object> liveWindows() {
        ArrayList<Object> out = new ArrayList<Object>();
        HashSet<Integer> seen = new HashSet<Integer>();
        try {
            ArrayList<Object> all = com.oupao.wiz.Hooks.allRootTasks();
            if (all != null) {
                for (Object t : all) {
                    addLive(out, seen, t);
                }
            }
        } catch (Throwable ignore) {
        }
        // 兜底：公共 getTasks + anyTaskForId（root 列表取不到时也能看到全部小窗）
        try {
            Class<?> c = Class.forName("android.app.ActivityTaskManager");
            Object svc = com.oupao.wiz.Rt.invoke(
                    com.oupao.wiz.Rt.methodByName(c, "getService", 0), null, null);
            Object list = com.oupao.wiz.Rt.callLoose(svc, "getTasks",
                    new Object[]{Integer.valueOf(30)});
            if (list instanceof List) {
                for (Object info : (List<?>) list) {
                    Object ido = com.oupao.wiz.Rt.get(info, "taskId");
                    if (!(ido instanceof Number)) {
                        continue;
                    }
                    int id = ((Number) ido).intValue();
                    if (seen.contains(Integer.valueOf(id))) {
                        continue;
                    }
                    addLive(out, seen, com.oupao.wiz.FlyH.taskById(id));
                }
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    private static void addLive(ArrayList<Object> out, HashSet<Integer> seen, Object t) {
        if (t == null) {
            return;
        }
        int id = com.oupao.wiz.Win.taskId(t);
        if (id <= 0) {
            return;
        }
        if (com.oupao.wiz.Hooks.isDockedZoom(t)) {
            return;                                    // 手动贴边的浮条不算、不关
        }
        if (!isFlexibleWindow(t)) {
            return;
        }
        if (!taskVisible(t)) {
            return;                                    // 已退场的残留不算
        }
        if (!seen.add(Integer.valueOf(id))) {
            return;
        }
        out.add(t);
    }

    /** ROM 自己的"这是小窗"判定；取不到时退回 zoomState */
    private static boolean isFlexibleWindow(Object t) {
        try {
            Object ctrl = flexibleController();
            if (ctrl != null) {
                // 3476：用「当前还在 FlexibleTaskController 列表里」作为活小窗判据。
                // 旧的 isFlexibleTaskAndHasCaption() 只看 launchScenario + hasCaption，
                // 是静态标记：task 已经退出小窗、回到后台后仍然返回 true，
                // 导致 closeOthersNow 反复去关同一个 task（日志里同一 tid 被关很多次）。
                Object list = com.oupao.wiz.Rt.get(ctrl, "mFlexibleTasks");
                if (list instanceof java.util.List) {
                    return ((java.util.List<?>) list).contains(t);
                }
                Object r = com.oupao.wiz.Rt.callLoose(ctrl, "isFlexibleTaskAndHasCaption",
                        new Object[]{t});
                if (r instanceof Boolean) {
                    return ((Boolean) r);
                }
            }
        } catch (Throwable ignore) {
        }
        return com.oupao.wiz.Win.zoomState(t) != 0;
    }

    private static boolean taskVisible(Object t) {
        try {
            Object r = com.oupao.wiz.Rt.callNoArg(t, "isVisible");
            if (r instanceof Boolean) {
                return ((Boolean) r);
            }
        } catch (Throwable ignore) {
        }
        return true;                                   // 取不到时不误杀
    }

    static Object flexibleController() {
        try {
            Class<?> svcCls = com.oupao.wiz.Rt.cls(com.oupao.wiz.Hooks.CL,
                    "com.android.server.wm.FlexibleWindowManagerService");
            if (svcCls == null) {
                return null;
            }
            var gi = com.oupao.wiz.Rt.methodByName(svcCls, "getInstance", 1);
            Object svc = (gi == null) ? null
                    : com.oupao.wiz.Rt.invoke(gi, null, new Object[]{null});
            return com.oupao.wiz.Rt.callNoArg(svc, "getFlexibleTaskController");
        } catch (Throwable ignore) {
            return null;
        }
    }

    private static int focusedTaskId() {
        try {
            Class<?> c = Class.forName("android.app.ActivityTaskManager");
            Object svc = com.oupao.wiz.Rt.invoke(
                    com.oupao.wiz.Rt.methodByName(c, "getService", 0), null, null);
            Object info = com.oupao.wiz.Rt.callLoose(svc, "getFocusedRootTaskInfo", new Object[]{});
            Object id = com.oupao.wiz.Rt.get(info, "taskId");
            return (id instanceof Number) ? ((Number) id).intValue() : -1;
        } catch (Throwable ignore) {
            return -1;
        }
    }

    private static boolean matches(Object t, String pkg, int user) {
        if ((t == null) || (pkg == null)) {
            return false;
        }
        if (!pkg.equals(com.oupao.wiz.Win.taskPkg(t))) {
            return false;
        }
        int tu = com.oupao.wiz.Win.taskUid(t) / 100000;
        if (user >= 0) {
            return tu == user;
        }
        return tu == 0;                                // 主应用只认主用户
    }

    private static void sortOldestFirst(ArrayList<Object> live) {
        Collections.sort(live, new Comparator<Object>() {
            public int compare(Object a, Object b) {
                return Integer.compare(com.oupao.wiz.Win.taskId(a),
                        com.oupao.wiz.Win.taskId(b));
            }
        });
    }

    // ───────────────────────────── 关窗 ─────────────────────────────

    /**
     * 关掉指定小窗（退出到后台，withTransition=false 即时消失），
     * 并在退场后清掉 ROM 的柔性登记（ROM 正常关窗会做，我们走原生策略跳过了）。
     */
    static boolean close(Object task) {
        try {
            if (task == null) {
                return false;
            }
            final int tid = com.oupao.wiz.Win.taskId(task);
            if (tid <= 0) {
                return false;
            }
            long now = android.os.SystemClock.uptimeMillis();
            if ((tid == lastClosedTid) && ((now - lastClosedAt) < CLOSE_DEDUP_MS)) {
                return false;                          // 刚关过，别重复
            }
            // 3306：**优先直接移除任务**。柔性退场会在 handleEvent 里
            //   `pauseInRecentsAnim()`，把正在入场的新窗动画一起冻住 → 半透明"阴影"。
            //   `ActivityTaskManagerService.removeTask(int)` 是 system_server 内的
            //   同步方法（返回 boolean），不参与柔性动画。
            //   ⚠ 这个类里有两个 removeTask：`removeTask(Task)` 和 `removeTask(int)`，
            //     必须按参数类型精确查找，不能按参数个数（会命中 Task 那个）。
            Object a = atms();
            Object r = null;
            if (a != null) {
                java.lang.reflect.Method rm = com.oupao.wiz.Rt.method(a.getClass(),
                        "removeTask", new Class[]{Integer.TYPE});
                if (rm != null) {
                    r = com.oupao.wiz.Rt.invoke(rm, a, new Object[]{Integer.valueOf(tid)});
                }
            }
            boolean ok = (r instanceof Boolean) && ((Boolean) r);
            if (ok) {
                lastClosedTid = tid;
                lastClosedAt = now;
                com.oupao.wiz.SLog.i("Gate: 已移除旧窗 tid=" + tid
                        + " pkg=" + com.oupao.wiz.Win.taskPkg(task));
                return true;
            }
            com.oupao.wiz.SLog.i("Gate: removeTask 未生效 tid=" + tid + " ret=" + r
                    + " → 退回柔性退场");
            return closeViaStrategy(task, tid, now);
        } catch (Throwable e) {
            com.oupao.wiz.SLog.e("Gate: 移除旧窗失败", e);
            return false;
        }
    }

    /**
     * 限制=1 连续切换：把旧小窗退回后台，**保留 task/应用进程**。
     *
     * 不能用 `removeTask(int)`：它会把 task 从最近任务里删掉，应用等于被杀，
     * 下次切回来只能冷启动（用户实测：每次切换都要重新打开 app）。
     * 优先走 ROM 原生的 `ExitFlexibleTaskToBackStrategy`，它只退出小窗状态。
     */
    private static boolean closeToBack(Object task) {
        try {
            if (task == null) {
                return false;
            }
            int tid = com.oupao.wiz.Win.taskId(task);
            if (tid <= 0) {
                return false;
            }
            long now = android.os.SystemClock.uptimeMillis();
            if ((tid == lastClosedTid) && ((now - lastClosedAt) < CLOSE_DEDUP_MS)) {
                return false;
            }
            if (closeViaStrategy(task, tid, now)) {
                com.oupao.wiz.SLog.i("Gate: 退后台(保留 task) tid=" + tid
                        + " pkg=" + com.oupao.wiz.Win.taskPkg(task));
                return true;
            }
            com.oupao.wiz.SLog.i("Gate: 柔性退场不可用 → removeTask tid=" + tid);
            return close(task);
        } catch (Throwable e) {
            com.oupao.wiz.SLog.e("Gate: 退后台失败", e);
            return false;
        }
    }

    /** 退回路径：ROM 原生的柔性退场（withTransition=false） */
    private static boolean closeViaStrategy(Object task, int tid, long now) {
        try {
            Object ctrl = flexibleController();
            if (ctrl == null) {
                return false;
            }
            Class<?> strat = com.oupao.wiz.Rt.cls(com.oupao.wiz.Hooks.CL,
                    "com.android.server.wm.ExitFlexibleTaskToBackStrategy");
            if (strat == null) {
                return false;
            }
            Object st = com.oupao.wiz.Rt.newInstance(strat,
                    new Object[]{task, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE});
            if (st == null) {
                st = com.oupao.wiz.Rt.newInstance(strat, new Object[]{task, Boolean.TRUE});
            }
            if (st == null) {
                return false;
            }
            com.oupao.wiz.Rt.callLoose(st, "setSourceFrom", new Object[]{Integer.valueOf(512)});
            com.oupao.wiz.Rt.callLoose(ctrl, "exitFlexibleTaskWindowInnerLocked",
                    new Object[]{st});
            // 3476：个别时序下 TaskExt.mFlexibleZoomState 会残留在 1，
            // 桌面「多任务隐藏迷你窗」读的就是这个值 → 任务已回后台仍被判成小窗。
            clearZoomStateIfExited(task, tid);
            lastClosedTid = tid;
            lastClosedAt = now;
            com.oupao.wiz.SLog.i("Gate: 已柔性退场 tid=" + tid);
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }

    /**
     * 3476：退场后清掉 TaskExt 里残留的 `mFlexibleZoomState`。
     *
     * 桌面侧「多任务隐藏迷你窗」读的就是它（`TaskInfo.mOplusExtraBundle.
     * key_flexible_task_state`）。正常退出时 caption 的 `onStateChanged(0)` 会归零，
     * 但我们从 system_server 直接走 `ExitFlexibleTaskToBackStrategy` 时，
     * 个别时序会留下 1 → 任务已经回到后台，多任务里却仍被判定成小窗而隐藏。
     *
     * 只在这条 task 已经不在 `FlexibleTaskController.mFlexibleTasks` 里时清，
     * 避免动到还在场的小窗。
     */
    private static void clearZoomStateIfExited(Object task, int tid) {
        try {
            if (task == null) {
                return;
            }
            Object ctrl = flexibleController();
            if (ctrl == null) {
                return;
            }
            Object list = com.oupao.wiz.Rt.get(ctrl, "mFlexibleTasks");
            if (!(list instanceof java.util.List)) {
                return;                     // 取不到列表就不冒险改状态
            }
            if (((java.util.List<?>) list).contains(task)) {
                return;                     // 还在小窗控制器里 = 没退干净
            }
            Object w = com.oupao.wiz.Rt.callNoArg(task, "getWrapper");
            Object ext = com.oupao.wiz.Rt.callNoArg(w, "getExtImpl");
            if (ext == null) {
                return;
            }
            Object cur = com.oupao.wiz.Rt.callNoArg(ext, "getFlexibleZoomState");
            int zs = (cur instanceof Number) ? ((Number) cur).intValue() : 0;
            if (zs == 0) {
                return;
            }
            com.oupao.wiz.Rt.callLoose(ext, "setFlexibleZoomState",
                    new Object[]{Integer.valueOf(0)});
            com.oupao.wiz.SLog.i("Gate: 已清残留小窗状态 tid=" + tid + " was=" + zs);
        } catch (Throwable ignore) {
        }
    }

    /** system_server 内的 ATMS 实例（不是 binder 代理） */
    private static Object atms() {
        try {
            Object w = com.oupao.wiz.Hooks.wms();
            Object a = com.oupao.wiz.Rt.get(w, "mAtms");
            if (a == null) {
                a = com.oupao.wiz.Rt.get(w, "mAtmService");
            }
            return a;
        } catch (Throwable ignore) {
            return null;
        }
    }
}
