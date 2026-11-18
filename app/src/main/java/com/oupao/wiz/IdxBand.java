package com.oupao.wiz;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/**
 * 边缘感应条 —— **可触摸的真实窗口，用来吃掉手势（防穿透）**。
 *
 * ## 为什么这样设计
 *
 * 之前它是 `FLAG_NOT_TOUCHABLE` + 靠 {@link FlyInput} 的全局旁听驱动，
 * 结果滑引索条时事件照样下发给下面那个 app：桌面翻页、列表跟着滚动，也就是「穿透」。
 *
 * 根因很硬：`InputManager.InputMonitor`（{@link FlyIh} 用的就是它）**只能看、不能消费**。
 * 想不穿透，只能让窗口本身在触摸链路里挡一刀。
 *
 * ## 系统的悬浮感应条做法
 *
 * 1. 感应条是**可触摸**的 overlay（`TYPE_APPLICATION_OVERLAY`），手势被它吃掉；
 * 2. 纯点击这种「本来该给下层」的情况，临时切成 `FLAG_NOT_TOUCHABLE`，
 *    再用 `InputManager.injectInputEvent` 把这一下**注入回输入流**；
 * 3. `setSystemGestureExclusionRects` 避开系统边缘返回手势。
 *
 * 这里沿用第 1、2 条（第 3 条在 system_server 侧的 overlay 上不可靠，先不做）。
 *
 * ## 安全阀
 *
 * 注入能力拿不到时（没有 `INJECT_EVENTS`），**保持原来的 NOT_TOUCHABLE**——
 * 宁可穿透，也不能把边缘的点击永久吃掉。
 */
public final class IdxBand {

    /**
     * 感应带宽度，**必须和 {@link Idx#feed} 里的边缘判定宽度（18dp）一致**。
     * 比它宽 → 多出来的那几 dp 被吃掉却判不成手势，只能走「等 45ms 再注入」的慢路；
     * 比它窄 → 判成手势的地方反而没吃掉，又穿透。
     */
    private static final int WIDTH_DP = 18;
    private static final int BAR_PERCENT = 42;   // 与 IdxBar 的条高一致
    private static final int BAR_OFFSET = 12;    // 与 IdxBar 的下移一致

    /**
     * 窗口基础标志，**完全对齐 FlyZone**（扇形菜单那块确定能收到触摸的窗口）：
     * `0x08000128` = SPLIT_TOUCH | LAYOUT_IN_SCREEN | NOT_TOUCH_MODAL | NOT_FOCUSABLE。
     *
     * 注意**不含** `0x10`（FLAG_NOT_TOUCHABLE）——可触摸才能吃掉手势。
     */
    private static final int FLAGS_TOUCHABLE = 0x08000128;
    /** 不能触摸时再加上 FLAG_NOT_TOUCHABLE */
    private static final int FLAG_NOT_TOUCHABLE = 0x10;

    private static final Handler H = new Handler(Looper.getMainLooper());

    private static WindowManager wm;
    private static Strip right;
    private static Strip left;
    private static int sw;
    private static int sh;
    /** 能不能把吃掉的点击注入回下层（false 时退回旧的「不可触摸」行为） */
    private static boolean canInject;

    private IdxBand() {
    }

    /** 按当前配置创建/移除两侧感应带 */
    static void sync(Context ctx) {
        try {
            if (ctx == null) {
                return;
            }
            var dm = ctx.getResources().getDisplayMetrics();
            // 横屏一律不启用：感应条是按竖屏「竖直中段」算的几何，
            // 横屏下那块区域既不合适、又容易和侧滑返回打架。
            // 用 Display.getRotation() 判（系统上下文的 DisplayMetrics 可能不随旋转变化）。
            if (com.oupao.wiz.Idx.isLandscape(ctx)
                    || (dm.widthPixels > dm.heightPixels)) {
                // 只在「真的还挂着」时才撤/复位——sync 每 0.7~1.5s 跑一次，
                // 无条件 reset 会反复调 IdxBar.hide() 刷日志。
                if ((right != null) || (left != null)
                        || (sw != dm.widthPixels) || (sh != dm.heightPixels)) {
                    removeAll();
                    com.oupao.wiz.Idx.reset();
                    com.oupao.wiz.SLog.i("IdxBand: 横屏不启用，已撤掉感应条");
                }
                sw = dm.widthPixels;
                sh = dm.heightPixels;
                return;
            }
            boolean on = com.oupao.wiz.Cfg.idxMaster();
            // 「仅在桌面生效」时，非桌面就不能留感应条——
            // 感应条是真窗口、会吃触摸，留着就是一块死区。
            if (on && (!com.oupao.wiz.Cfg.idxGlobal()) && (!com.oupao.wiz.Idx.isLauncher())) {
                on = false;
            }
            boolean needR = on && com.oupao.wiz.Cfg.idxRight();
            boolean needL = on && com.oupao.wiz.Cfg.idxLeft();
            if ((!needR) && (!needL)) {
                removeAll();
                com.oupao.wiz.Idx.reset();   // 开关关掉：浮层和手势状态一起清
                return;
            }
            if (wm == null) {
                wm = (WindowManager) ctx.getSystemService("window");
            }
            if (wm == null) {
                return;
            }
            canInject = com.oupao.wiz.IdxTouch.ready();
            if ((sw != dm.widthPixels) || (sh != dm.heightPixels)) {
                removeAll();
                sw = dm.widthPixels;
                sh = dm.heightPixels;
            }
            if (needR && (right == null)) {
                right = add(ctx, 1);
            }
            if (needL && (left == null)) {
                left = add(ctx, -1);
            }
            if ((!needR) && (right != null)) {
                del(right);
                right = null;
                com.oupao.wiz.Idx.reset();
            }
            if ((!needL) && (left != null)) {
                del(left);
                left = null;
                com.oupao.wiz.Idx.reset();
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("IdxBand: sync \u5f02\u5e38", t);
        }
    }

    /**
     * 这一侧是否由感应条**真正接收触摸**（该侧有感应条且能注入）。
     *
     * 为 true 时手势由 {@link Strip#onTouchEvent} 驱动，
     * {@link FlyInput} 那条旁听就不该再喂一遍 {@link Idx#feed}——
     * 否则同一条 MOVE 会被两路各处理一次，`setByY` / `showGroup` 跑两遍，
     * 拖动明显不跟手。
     *
     * 按 x 判断左右：只开了一侧时，另一侧还得靠旁听才能用。
     */
    static boolean activeAt(float x) {
        if (!canInject) {
            return false;
        }
        if (sw <= 0) {
            return (right != null) || (left != null);
        }
        if (x >= ((float) sw / 2f)) {
            return right != null;
        }
        return left != null;
    }

    private static Strip add(Context ctx, int side) {
        try {
            var v = new Strip(ctx);
            v.side = side;
            var lp = new WindowManager.LayoutParams();
            // 必须和 FlyZone（扇形菜单那块确定能收到触摸的窗口）用同一套参数：
            // 2038 = TYPE_APPLICATION_OVERLAY，再加 setTrustedOverlay()。
            // 之前用的 2003 从 system_server 加出来**收不到触摸**（日志里旁听层看得到
            // 手指、这层一条 onTouchEvent 都没有），换成 2038 + trusted 才进触摸链路。
            lp.type = 2038;
            lp.format = -3;
            lp.width = Math.max(com.oupao.wiz.Ui.dp(ctx, WIDTH_DP), 8);
            int barH2 = ((sh * BAR_PERCENT) / 100);
            // 感应带**和条本身等高**，不再上下各多留 3%。
            // 多出来的那圈起不了引索手势，却也要走「吃掉 → 等一会 → 注入」的慢路，
            // 表现就是「条下方那块点一下反应慢」。和条等高后，条外的点击根本不经我们，
            // 直接落到下层，零延迟。
            lp.height = barH2;
            int barTop2 = (((sh - barH2) / 2) + ((sh * BAR_OFFSET) / 100));
            lp.gravity = (Gravity.TOP | (side > 0 ? Gravity.RIGHT : Gravity.LEFT));
            lp.y = barTop2;
            // 注入拿不到 → 不能可触摸，否则纯点击会被永久吃掉
            lp.flags = canInject ? FLAGS_TOUCHABLE : (FLAGS_TOUCHABLE | FLAG_NOT_TOUCHABLE);
            // 背景是全透明、什么都不画，所以 alpha 保持 1.0 也看不见；
            // 用极低 alpha 反而可能被当成「不可见窗口」排除出触摸链路。
            lp.alpha = 1.0f;
            try {
                lp.setFitInsetsTypes(0);
            } catch (Throwable ignore) {
            }
            // 只要 setTrustedOverlay()，**不能**用 FlyS.trust()：
            // trust() 还会 `inputFeatures |= 4`，那一位是 INPUT_FEATURE_SPY
            //（间谍窗口：收得到事件副本但不消费）——用了就「收得到但照样穿透」。
            // 实测：2003 无 trusted → 完全收不到（死区）；2038+trusted+spy → 收到但不消费；
            // 2038+trusted（不加 spy）才是「收到并吃掉」。
            try {
                lp.getClass().getMethod("setTrustedOverlay").invoke(lp);
            } catch (Throwable ignore) {
            }
            try {
                lp.setTitle("oupao_idxband" + (side > 0 ? "R" : "L"));
            } catch (Throwable ignore) {
            }
            wm.addView(v, lp);
            com.oupao.wiz.SLog.i("IdxBand: \u521b\u5efa side=" + side + " w=" + lp.width
                    + " h=" + lp.height + " \u53ef\u89e6\u6478=" + canInject);
            return v;
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("IdxBand: add \u5931\u8d25", t);
            return null;
        }
    }

    /** 切换感应条是否接收触摸（纯点击透传时临时关一下） */
    private static void setTouchable(Strip v, boolean on) {
        try {
            if ((v == null) || (wm == null)) {
                return;
            }
            var lp = v.getLayoutParams();
            if (!(lp instanceof WindowManager.LayoutParams)) {
                return;
            }
            var w = (WindowManager.LayoutParams) lp;
            int f = on ? FLAGS_TOUCHABLE : (FLAGS_TOUCHABLE | FLAG_NOT_TOUCHABLE);
            if (w.flags == f) {
                return;
            }
            w.flags = f;
            wm.updateViewLayout(v, w);
        } catch (Throwable ignore) {
        }
    }

    private static void del(Strip v) {
        try {
            if ((v != null) && (wm != null)) {
                wm.removeView(v);
                com.oupao.wiz.SLog.i("IdxBand: \u79fb\u9664 side=" + v.side);
            }
        } catch (Throwable t) {
        }
    }

    static void removeAll() {
        if (right != null) {
            del(right);
            right = null;
        }
        if (left != null) {
            del(left);
            left = null;
        }
    }

    private static final class Strip extends View {
        int side;
        /** 缓冲这一笔触摸（最多 24 个点），用于原样重放给下层 */
        private final float[] bx = new float[24];
        private final float[] by = new float[24];
        private int bn;
        private boolean moved;
        private boolean busy;
        /** 注入的事件又回到自己身上了 = 窗口还没脱离触摸链路，需要补一次 */
        private boolean sunk;

        Strip(Context c) {
            super(c);
            setBackgroundColor(Color.TRANSPARENT);
            setClickable(true);
        }

        private void clear() {
            bn = 0;
            moved = false;
        }

        private void push(float x, float y) {
            if (bn < 24) {
                bx[bn] = x;
                by[bn] = y;
                bn++;
            }
        }

        public boolean onTouchEvent(MotionEvent e) {
            try {
                // 注入回来的事件（deviceId < 0）不接，否则自己吃自己。
                // 走到这里说明「切成 NOT_TOUCHABLE」还没生效就注入了 ——
                // 标记一下，passThrough 会补发一次。
                if (e.getDeviceId() < 0) {
                    sunk = true;
                    return false;
                }
                if (busy) {
                    return true;
                }
                int a = e.getActionMasked();
                float x = e.getRawX();
                float y = e.getRawY();
                int slop = com.oupao.wiz.Ui.dp(getContext(), 6);

                if (a == MotionEvent.ACTION_DOWN) {
                    clear();
                    push(x, y);
                    com.oupao.wiz.SLog.i("IdxBand: down x=" + (int) x + " y=" + (int) y
                            + " inj=" + canInject);
                    com.oupao.wiz.Idx.feed(0, x, y, side);
                    return true;
                }
                if ((!moved) && ((Math.abs(x - bx[0]) > slop) || (Math.abs(y - by[0]) > slop))) {
                    moved = true;
                }
                if (a == MotionEvent.ACTION_MOVE) {
                    push(x, y);
                    com.oupao.wiz.Idx.feed(2, x, y, side);
                    return true;
                }
                if (a == MotionEvent.ACTION_UP) {
                    push(x, y);
                    // 必须在 feed(1) 之前取：feed(1) 会把状态清成 NONE
                    boolean fired = com.oupao.wiz.Idx.firedThisGesture();
                    com.oupao.wiz.Idx.feed(1, x, y, side);
                    com.oupao.wiz.SLog.i("IdxBand: up fired=" + fired + " moved=" + moved
                            + " n=" + bn);
                    if (!fired) {
                        // 这次不是引索手势 —— 本来该给下面那个 app，原样还回去
                        passThrough();
                    }
                    return true;
                }
                com.oupao.wiz.Idx.feed(3, x, y, side);
                return true;
            } catch (Throwable t) {
                com.oupao.wiz.SLog.e("IdxBand: feed \u5f02\u5e38", t);
                return true;
            }
        }

        /**
         * 把这笔被吃掉的触摸还给下层。
         *
         * **时序是关键**：`updateViewLayout` 改窗口触摸区域到 InputDispatcher 生效是**异步**的。
         * 切完立刻注入，注入的事件会被自己这层还"可触摸"的窗口再吃一次，下层永远收不到
         * —— 表现就是「这块区域点不动」。所以必须「切 → 等够 → 注入 → 再等 → 恢复」。
         */
        private void passThrough() {
            final int n = bn;
            if (n <= 0) {
                return;
            }
            final float[] xs = new float[n];
            final float[] ys = new float[n];
            System.arraycopy(bx, 0, xs, 0, n);
            System.arraycopy(by, 0, ys, 0, n);
            clear();
            if (!canInject) {
                com.oupao.wiz.SLog.i("IdxBand: 不能注入，这次触摸丢失");
                return;
            }
            busy = true;
            sunk = false;
            IdxBand.setTouchable(this, false);
            // 45ms 就注入（原来 90ms，用户能感觉到慢）。
            // 如果这时窗口还没脱离触摸链路，注入的事件会回到自己身上（sunk=true），
            // 那就再补一次——用「先快、被吞了再补」换掉「一律死等」。
            H.postDelayed(new Runnable() {
                public void run() {
                    boolean ok = com.oupao.wiz.IdxTouch.replay(xs, ys, n);
                    H.postDelayed(new Runnable() {
                        public void run() {
                            boolean again = false;
                            if (sunk) {
                                sunk = false;
                                again = com.oupao.wiz.IdxTouch.replay(xs, ys, n);
                            }
                            com.oupao.wiz.SLog.i("IdxBand: 透传点击 ok=" + ok
                                    + " 补发=" + again + " n=" + n);
                            H.postDelayed(new Runnable() {
                                public void run() {
                                    IdxBand.setTouchable(Strip.this, true);
                                    busy = false;
                                }
                            }, 160L);
                        }
                    }, 70L);
                }
            }, 45L);
        }
    }
}
