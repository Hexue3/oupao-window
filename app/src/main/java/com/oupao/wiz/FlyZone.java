package com.oupao.wiz;

/**
 * 角落热区窗口的 View（system_server，2038 + trusted，**可触摸=消费**）。
 *
 * 职责只有两件：
 *  1. **消费**这条流 → 底层 App 收不到，菜单划选时不会跟着滚动；
 *  2. 这一下没有叫出菜单（纯点击）→ UP 时把点击重放给下层。
 *
 * 自绘菜单由 WMS 旁听（FlyFanBr → FlyFanH）驱动，跟这里无关。
 * 旧菜单模式（sys.oupao.fly.legacy=1）下仍把事件喂给 FlyInput 以保持兼容。
 *
 * 历史坑：重放时要临时设 NOT_TOUCHABLE，早期用"读改写 flags + 延时恢复"，
 * 两次重放重叠时会把 NOT_TOUCHABLE 永久留下 → 表现为"刚开机第一次不穿透、用几次后底层开始滚"。
 * 现在恢复一律走 ensureTouchable()（固定清位）+ 序号防陈旧回调。
 */
public class FlyZone extends android.view.View {

    public static final int FLAG_NOT_TOUCHABLE = 0x10;

    private static final android.os.Handler H =
            new android.os.Handler(android.os.Looper.getMainLooper());

    public int s;

    /** FlyS.mk 在 addView 之后注入 */
    public android.view.WindowManager wm;
    public android.view.WindowManager.LayoutParams wp;

    private float downX;
    private float downY;
    private int seq;

    /** 3265：本进程的热区窗口是 SPY（只旁观）——FlyS.mk 设置 */
    public static volatile boolean spy;

    /** 3265：本次手势是否已经 pilfer 抢流（抢过就不再重复抢） */
    private boolean pilfered;

    public FlyZone(android.content.Context p2, int p3) {
        super(p2);
        this.s = p3;
        super.setWillNotDraw(false);
    }

    public boolean onTouchEvent(android.view.MotionEvent p2) {
        try {
            // 3125：游戏模式热区不能消费触摸；正常应该在游戏开始时已切成 NOT_TOUCHABLE，
            // 这里兜底一次——万一没来得及切，也立刻退出，不让游戏摇杆/技能键被吃掉。
            if (com.oupao.wiz.Hooks.gameBlock()) {
                ensureNotTouchable();
                return false;
            }
            // 3501：输入法打开期间热区只旁观（SPY 窗口本来就只收副本）：
            //   不 pilfer —— 否则会把输入法的指针流抢走，系统直接 input_cancel 掉键盘触摸；
            //   不重放点击 —— 否则"从角落上滑"会被补成一次按键，等于往输入框里乱打字。
            if (com.oupao.wiz.Hooks.imeShown()) {
                this.pilfered = false;
                return true;
            }
            if (com.oupao.wiz.FlyH.legacyOn()) {
                com.oupao.wiz.FlyInput.zone(p2);
                return true;
            }
            int a = p2.getActionMasked();
            if (a == android.view.MotionEvent.ACTION_DOWN) {
                this.downX = p2.getRawX();
                this.downY = p2.getRawY();
                this.pilfered = false;
                ensureTouchable();
                // 3267：**DOWN 就抢**。实测在 MOVE 里抢（dy=-23）已经晚了——
                //   系统的边缘返回手势早就启动、预测性返回的「<」箭头也已经画出来了，
                //   这时 pilfer 只能截断后续事件，拦不住已经开始的返回动画
                //   （用户截图：菜单和返回箭头同时出现）。
                // 3270：**只在手势圆内抢**。窗口是 2×半径的方形，但真正能呼出菜单的
                //   只有"距下角 ≤ 半径"的圆（`FlyGeom.side` 同口径）。在圆外不抢 →
                //   那片区域侧滑返回照常，也不会出现"屏蔽范围比触发范围大一倍"。
                // 3312：**圆内也要分位置**。系统返回手势只从屏幕侧边起手，圆内部
                //   （键盘空格/回车一带）根本不会有返回冲突；在 DOWN 就抢会把这些
                //   普通点按吃掉，只能等 UP 注入重放 → 用户感觉"键盘点一下要等一下"。
                //   所以：贴侧边 → DOWN 就抢；圆内部 → 等 MOVE 确认是上滑菜单手势再抢。
                if (spy && inGestureCircle(this.downX, this.downY) && nearSideEdge(this.downX)) {
                    this.pilfered = pilfer();
                }
                com.oupao.wiz.SLog.i("FlyZone: DOWN " + ((int) this.downX) + "," + ((int) this.downY)
                        + " s=" + this.s + (spy ? (" spy pilfer=" + this.pilfered) : ""));   // 2774
            } else if (a == android.view.MotionEvent.ACTION_MOVE) {
                // 3265：SPY 模式下事件是"副本"，系统那边同时也在处理。
                //   一旦判定成菜单手势（明显向上/斜上），立刻 pilferPointers 抢流，
                //   系统的边缘返回/App 滚动从此收不到；纯横向滑动不抢 → 返回照常。
                if (spy && !this.pilfered && inGestureCircle(this.downX, this.downY)) {
                    float dx = p2.getRawX() - this.downX;
                    float dy = p2.getRawY() - this.downY;
                    // 3265b：门槛要**很小**。实测 24dp(≈80px) 时系统已经判定成"侧滑返回"了，
                    //   再 pilfer 也来不及（返回照样执行）。这里取 ≈0.6×touchSlop(8dp)。
                    float slop = 8f * getResources().getDisplayMetrics().density * 0.6f;
                    if ((dy < -slop) && (Math.abs(dy) > Math.abs(dx) * 0.6f)) {
                        this.pilfered = pilfer();
                        com.oupao.wiz.SLog.i("FlyZone: 菜单手势 dy=" + ((int) dy)
                                + " dx=" + ((int) dx) + " → pilfer=" + this.pilfered);
                    }
                }
            } else if (a == android.view.MotionEvent.ACTION_UP) {
                // 2776：菜单停在角落等选择时，这一下点击交给菜单（否则会被热区吃掉、什么都点不到）
                if (com.oupao.wiz.FlyFanH.staying()) {
                    float fdx = p2.getRawX() - this.downX;
                    float fdy = p2.getRawY() - this.downY;
                    // ★ 划选（手指从按下点移动过）→ 松手不再补"点击振动"：
                    //   sel 变化时已经振过一次，点击振动不能窜到滑动状态（用户 2026-11-16）。
                    com.oupao.wiz.SLog.i("FlyZone: 交给停留中的菜单 "
                            + ((int) p2.getRawX()) + "," + ((int) p2.getRawY()));
                    com.oupao.wiz.FlyFanH.tap(p2.getRawX(), p2.getRawY());
                    return true;
                }
                boolean used = com.oupao.wiz.FlyState.open()
                        || (com.oupao.wiz.FlyH.win != null)
                        || (com.oupao.wiz.FlyH.st == 2)
                        || com.oupao.wiz.FlyFanH.alive()
                        || com.oupao.wiz.FlyFanBr.gestured;   // 2774：菜单吃过的这一下别再重放点击
                com.oupao.wiz.SLog.i("FlyZone: UP used=" + (used ? 1 : 0)
                        + " at " + ((int) p2.getRawX()) + "," + ((int) p2.getRawY()));   // 2774
                // 3267：DOWN 已经 pilfer 抢流了 → 这一下没到下层，点击仍然要自己重放
                // 3312：**只有真的抢过流才重放**。SPY 模式下没抢流时 App 自己已经收到
                //   这一下，再重放一次就是"点一下触发两次/延迟一次"（键盘输入最明显）。
                if (!used && this.pilfered) {
                    replayTap(this.downX, this.downY);
                }
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("FlyZone: onTouchEvent 异常", t);
        }
        return true;
    }

    /**
     * 3270：这个点是不是在"能呼出菜单"的手势圆里（距下角 ≤ 半径）。
     *
     * 口径和 `FlyGeom.side()` 完全一致：圆心是屏幕左下/右下角，半径 = flyRange×density。
     * 只有圆内才 pilfer（屏蔽系统返回）；圆外不抢，返回照常。
     */
    private boolean inGestureCircle(float x, float y) {
        try {
            int r = com.oupao.wiz.FlyS.radiusNow();
            if (r <= 0) {
                return false;
            }
            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            float w = dm.widthPixels;
            float h = dm.heightPixels;
            float dx = (this.s == 1) ? x : (w - x);
            float dy = h - y;
            return (dx * dx + dy * dy) <= ((float) r * r);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 3312：起手点是不是贴着屏幕左右侧边（系统边缘返回手势只会从这里开始）。
     *
     * 圆内部（键盘空格/回车一带）没有返回冲突，不需要在 DOWN 抢流；
     * 等 MOVE 确认是上滑菜单手势再抢，普通点按就能原样落到 App。
     */
    private boolean nearSideEdge(float x) {
        try {
            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            int w = dm.widthPixels;
            if (w <= 0) {
                w = com.oupao.wiz.FlyH.sw;
            }
            int edge = android.view.ViewConfiguration.get(getContext()).getScaledEdgeSlop();
            if (edge <= 0) {
                edge = (int) (24f * dm.density);
            }
            return (x <= edge) || (x >= (w - edge));
        } catch (Throwable t) {
            return true;                               // 取不到就保守：沿用旧行为
        }
    }

    /**
     * 3265：把这条触摸流从别人手里**抢过来**（系统实际行为）。
     *
     * SPY 窗口只拿到事件副本，系统/App 同时在处理；一旦手势判定成"菜单手势"，
     * 调 `InputManager.pilferPointers(viewRootImpl.getInputToken())` 之后就只有我们收得到。
     * 这样：纯横向滑动不抢 → 系统侧滑返回照常；斜上滑就抢 → 返回被取消、菜单独占。
     */
    private boolean pilfer() {
        try {
            java.lang.reflect.Method gv = android.view.View.class.getDeclaredMethod("getViewRootImpl");
            gv.setAccessible(true);
            Object vr = gv.invoke(this);
            if (vr == null) {
                return false;
            }
            java.lang.reflect.Method gi = vr.getClass().getMethod("getInputToken");
            gi.setAccessible(true);
            Object token = gi.invoke(vr);
            if (!(token instanceof android.os.IBinder)) {
                return false;
            }
            Object im = getContext().getSystemService("input");
            if (im == null) {
                return false;
            }
            java.lang.reflect.Method pm = android.hardware.input.InputManager.class
                    .getDeclaredMethod("pilferPointers", android.os.IBinder.class);
            pm.setAccessible(true);
            pm.invoke(im, (android.os.IBinder) token);
            return true;
        } catch (Throwable t) {
            com.oupao.wiz.SLog.i("FlyZone: pilfer 失败 " + t);
            return false;
        }
    }

    /** 清掉 NOT_TOUCHABLE（固定清位，不读改写，避免竞态把状态写坏） */
    private void ensureTouchable() {
        try {
            // 3125：游戏模式里 replayTap 的延迟恢复不能把热区又恢复成可触摸
            if (com.oupao.wiz.Hooks.gameBlock()) {
                ensureNotTouchable();
                return;
            }
            android.view.WindowManager.LayoutParams p = this.wp;
            if ((p != null) && ((p.flags & FLAG_NOT_TOUCHABLE) != 0)) {
                p.flags &= ~FLAG_NOT_TOUCHABLE;
                if (this.wm != null) {
                    this.wm.updateViewLayout(this, p);
                }
            }
        } catch (Throwable ignore) {
        }
    }

    /** 游戏模式：热区加 NOT_TOUCHABLE，触摸直接落到下层游戏 */
    private void ensureNotTouchable() {
        try {
            android.view.WindowManager.LayoutParams p = this.wp;
            if ((p != null) && ((p.flags & FLAG_NOT_TOUCHABLE) == 0)) {
                p.flags |= FLAG_NOT_TOUCHABLE;
                if (this.wm != null) {
                    this.wm.updateViewLayout(this, p);
                }
            }
        } catch (Throwable ignore) {
        }
    }

    /** 临时摘掉可触摸 → 注入干净点击 → 恢复（序号防陈旧回调覆盖） */
    private void replayTap(final float x, final float y) {
        final android.view.WindowManager w = this.wm;
        final android.view.WindowManager.LayoutParams lp = this.wp;
        if ((w == null) || (lp == null)) {
            return;
        }
        com.oupao.wiz.SLog.i("FlyZone: 点击重放 " + ((int) x) + "," + ((int) y));   // 2774
        final int my = ++this.seq;
        try {
            lp.flags |= FLAG_NOT_TOUCHABLE;
            w.updateViewLayout(this, lp);
        } catch (Throwable t) {
            return;
        }
        H.postDelayed(new Runnable() {
            public void run() {
                long t = android.os.SystemClock.uptimeMillis();
                if (com.oupao.wiz.IdxTouch.send(t, t, android.view.MotionEvent.ACTION_DOWN, x, y)) {
                    // 3237：DOWN→UP 留 80ms。原来只差 1ms，Aicy 建议气泡这类自绘浮层
                    //   会把"瞬时抬起"当无效点击（表现：只振动、不展开）。
                    com.oupao.wiz.IdxTouch.send(t, t + 80L, android.view.MotionEvent.ACTION_UP, x, y);
                }
            }
        }, 90L);
        H.postDelayed(new Runnable() {
            public void run() {
                if (my != FlyZone.this.seq) {
                    return;
                }
                ensureTouchable();
            }
        }, 200L);
    }
}
