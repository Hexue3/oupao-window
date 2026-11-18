package com.oupao.wiz;

/**
 * 自绘扇形菜单的控制器（跑在 system_server）。
 *
 * 职责：建一个全屏、**不可触摸**的窗口（只负责画），把进度/选中/生命周期管好。
 * 触摸事件仍由角落那个消费型热区窗口（[FlyZone]）提供 —— 它拿得到 MOVE，天然跟手。
 */
public final class FlyFanH {

    private static android.view.WindowManager wm;
    private static FlyFan fan;
    private static android.view.WindowManager.LayoutParams lp;
    private static android.content.Context ctx;

    private static float cornerX;
    private static float cornerY;
    private static float rMin;
    private static float rMax;
    private static boolean staying;
    /** 3445：进入"停留"的时刻 —— 桌面写的 `oupao_away_at` 比它旧就不算（防止刚呼出就被旧手势收掉） */
    private static long aStayAt;
    /** 3455：进入停留那一刻是否**已经**在多任务界面（是的话标志位不算数） */
    private static boolean aRecAtStay;

    /**
     * 3248：菜单最后一次收起的时刻。
     *
     * 选中应用的那一下 UP 会同时落进小窗的"扩展可触区"，
     * `Hooks.onOutsideTap` 把它当成"窗外单击"→ 刚开的小窗立刻被关掉。
     * 实测日志：`FlyFanBr: UP … sel=N` → `startZoomWindow` → 同一秒
     * `FlyClose: 窗外单击 -> 关闭小窗`，于是每开一个应用就"开→关"演一遍。
     */
    static volatile long hideAt;

    // ===== 3418：动效状态（全部只影响绘制，不影响命中） =====
    /** 每图标的选中缩放（选中 1.28 / 未选中 0.96，帧率无关指数逼近） */
    private static float[] aScale;
    /** 每图标「焦点不透明度」：选中=1.0，未选中=0.92（轻微后退，突出选中项） */
    private static float[] aFade;
    /**
     * 3419：每图标的**出现度**（0 = 还没冒出来，1 = 完全到位）。
     *
     * 由**手指进度 p** 驱动（不是时间）：刚呼出 p≈0.12 时只有 `@more`（三个点），
     * 往外滑越过各自阈值才依次冒出来，滑到最外刚好全部到齐。
     * 另有一层**时间上下限**做保护：即使 p 瞬间跳到 1，也按 i 错峰依次放开，
     * 不会"六个一起糊出来"。
     */
    private static float[] aAppear;
    /** 3447：每颗**自己的出场行程**（0..1，与手指位置解耦，保证"从角上飞出来"） */
    private static float[] aTravel;
    /** 背景压暗的实际值（跟 p 走 + 指数平滑，不再一呼出就暗） */
    private static float aDim;
    /** 速度耦合：拖动越快整体略微鼓一下 */
    private static float aLean;
    private static float aLeanTarget;
    private static long aLastFrame;
    private static float aLastX;
    private static float aLastY;
    private static long aLastMove;
    private static boolean aTicking;
    /**
     * 3426：**呼出瞬间的手指进度作为基准**（-1 = 还没取）。
     *
     * 手势要先划过 slop 才会建菜单，所以 `show()` 那一刻手指已经离开角落一段距离
     * （实测 p≈0.3~0.4）。如果直接用 p 算位置，图标一露面就在半路 ——
     * 用户："一呼出就是在这个位置，我想的是刚呼出时在角落"。
     * 以这一刻为 0 点：呼出时图标都在角落，继续外划才展开。
     */
    private static float aBaseP;
    /** 3450：第一帧时刻（时间兜底用） */
    private static long aShowAt;
    /**
     * 3429：触顶回弹的弹簧状态。
     * `aBounceX` = 半径偏移（0.05 = 多鼓 5%），`aBounceV` = 它的速度。
     * 触发只在 tS 首次到顶那一下，且**每手势一次**（aBounceDone）。
     */
    private static float aBounceX;
    private static float aBounceV;
    private static boolean aBounceDone;
    /**
     * 3428：**「松手收起」模式**（`Cfg.flyStayOff()`=1）。
     *
     * 那个模式下必须一口气划到底、松手菜单就没了（不会停住等你选），
     * 所以出现节奏慢就等于"必须多按住一会儿"，用户说累手。
     * 开启时把展开进度**中段**加速（见 `FAST_MID_BOOST`，3431 起两端不改）。
     */
    private static boolean aFastOpen;
    /** 3436：平滑后的手指速度（px/s），由 move() 更新，供「松手收起」模式做速度跟随 */
    private static float aSpeed;
    /** 正在播收起动画的那扇菜单（已经从 fan 摘下来，但窗口还没 remove） */
    private static FlyFan aClosing;
    private static long aCloseAt;
    private static float[] aCloseAlpha;
    /** 系统「动画时长缩放」是否为 0（无障碍/开发者选项关动画）→ 全部直出 */
    private static boolean aNoAnim;

    /**
     * 3424：每颗的**出现进度**独立平滑（指数逼近，时间常数 ≈ 1/k）。
     *
     * 为什么不是"整体进度 s 平滑跟随 p"：那样快滑到底时 s 还在追，最后几颗被落在后面
     * （用户实测：滑到最右那颗了，底下几颗还没显示完）。改成每颗各自逼近自己的目标值，
     * 各颗互不拖累，且天然是柔和淡入（不是二值跳变）。
     */
    private static final float APPEAR_K = 11.0f;
    /**
     * 3443：**往外划时的出现系数**（≈24 → τ≈42ms，三帧左右跟上手指）。
     * 3444：34 → 24（用户："整体速度有点过于快了，稍微慢一点"；34 时几乎瞬时）。
     *
     * 为什么要和 APPEAR_K 分开：半径是出现度的三次方，
     * 往外划若还用 11，会出现"趴角落 → 猛冲"的滞后感（用户："呼出没跟随，只有收回跟随"）；
     * 往回划则希望保持柔和淡出，所以只把**上升方向**加速。
     */
    private static final float APPEAR_K_OUT = 24.0f;
    /**
     * 每颗的起跑线间隔（沿弧，**从靠屏幕外缘那颗往里排**）。
     * 取值保证「最后一颗的起跑线 + 跨度 = 1.0」：6 颗时 5×0.10 + 0.50 = 1.00，
     * 即手指划到最外时**刚好全部展开**（不会有一颗卡在 85%）。
     */
    private static final float APPEAR_STEP = 0.10f;
    /**
     * 每颗淡入所占的**行程宽度**（手指行程，不是时间）。
     * 0.30 → 0.45 → 0.50：用户要「淡入更柔」—— 每颗要走掉一半的滑动行程才算完全显形，
     * 所以手指停在半路时看到的一定是**半透明**的中间态。
     * 3433：配合 STEP 0.10 保持 `5×0.10 + 0.50 = 1.0`（划到底刚好全部展开）。
     */
    private static final float APPEAR_SPAN = 0.50f;
    /**
     * 3425：**起点贴角程度**。出现度 0 时半径 = rMin × 0.25（≈17px），
     * 基本就是压在屏幕那个角上。用户："第一个显示的位置还是太远了，应该更接近角落"。
     */
    private static final float APPEAR_R_FROM = 0.25f;
    /**
     * 3429：**触顶回弹**（气球撞顶）。
     *
     * 用**弹簧**而不是"到点后播正弦"：
     *  - 触发时刻 = 手指划到最外（tS 到 1）的那一帧，此时半径还在长 →
     *    给一个**向外的初速度**，位移从 0 连续地冲到 +7% 再被阻尼拉回。
     *  - 因此没有"停住再弹"的断点（旧实现要等所有图标淡完才起弹，那一段就是停顿）。
     *  - `aBounceDone` 保证**每个手势只弹一次**：以前衰减结束就把标志清掉、
     *    而 tS 还是 1.0 → 下一帧又判定"刚触顶" → 划选应用时整菜单反复弹。
     */
    private static final float BOUNCE_K = 220.0f;      // 弹簧刚度
    private static final float BOUNCE_C = 13.0f;       // 阻尼（ζ≈0.44，明显过冲 + 一次小回摆）
    private static final float BOUNCE_V0 = 2.0f;        // 向外初速度（3455：2.0 × 慢 0.28 / 快 0.40）
    /**
     * 3431：「松手收起」模式下的**中段加速系数**（B）。
     * 钟形偏置 `t + B·4t²(1-t)²`：两端位置与速度都不变，中段最多快 ≈1+0.77B。
     */
    private static final float FAST_MID_BOOST = 0.45f;
    /**
     * 3434：**尾段额外增益**（用户追加："把尾段速度也加快一点"）。
     *
     * 上一版尾段刻意保持原样（h(1)=1、h'(1)=1），结果手指要划到最末端才完全展开，
     * 那个模式下（松手就收起）最后一段显得拖。
     * 现在再乘一个 `1 + G·smoothstep(0.25, 0.80, t)` 的增益：
     *  - 起点 w=0 且 w'=0 → **起步手感完全不变**（用户只放开了尾段）
     *  - 越靠后增益越大 → 进展提前到 ~84% 行程就吃满（之后 clamp 在 1）
     */
    private static final float FAST_TAIL_GAIN = 0.25f;
    /**
     * 3436：**速度跟随**（用户："整体速度要跟随手滑动的速度，滑动速度越快呼出越快"）。
     *
     * 「松手收起」模式下，中段/尾段的加速量和每颗的淡入速度都乘以
     * `vf = 手指速度 / SPEED_REF`（限幅 0.55~2.0）：
     *   · 慢划 → vf<1 → 加速量变小，几乎退回线性（看得清一颗颗冒出来）
     *   · 快划 → vf>1 → 加速量变大 + 淡入更快（一口气到位，不拖手）
     * 参考速度取 1600px/s（≈一次利落的中等速度划动），此时各项系数与 3435 完全一致。
     */
    private static final float SPEED_REF = 1600.0f;
    /** 冒出时的起始缩放（3424：0.78 → 0.62 → 3433：0.34，出生时是角上一个小点） */
    private static final float BLOOM_FROM = 0.34f;
    /**
     * 3433：**半径的「起步驻留」**——出现度 ≤ 这个值之前半径不动（就贴在角落）。
     *
     * 用户："刚呼出菜单右边第一个图标冒出来的位置还是太靠外了"。
     * 原因是半径用 `appear³`：出现度 0.5 时已经走了 12.5% 的行程（≈90px），
     * 而这时候图标已经半透明了 —— 眼睛看到的"出生点"就在半路。
     * 现在前 30% 的出现度完全驻留在 `fromR`（≈角上 17px），**先把淡入走完，再往外滑**。
     */
    private static final float RADIUS_DWELL = 0.15f;
    /** dim 跟随 p 的逼近系数（让它比图标稍慢一点，压暗感更"沉"） */
    private static final float DIM_K = 14.0f;
    private static final float CLOSE_MS = 150.0f;
    private static final float CLOSE_TO = 0.94f;
    /** 选中缩放 / 失焦压暗的逼近系数（越大越快；55 ≈ 70ms 到位，45 ≈ 90ms） */
    private static final float SEL_K = 55.0f;
    private static final float FOCUS_K = 45.0f;

    private static final android.os.Handler AH =
            new android.os.Handler(android.os.Looper.getMainLooper());

    private static final Runnable TICK = new Runnable() {
        public void run() {
            tick();
        }
    };

    /**
     * 3444：vsync 对齐的下一帧（拿不到 Choreographer 就退回 16ms 定时）。
     * 每次只挂一个帧回调（`aTicking` 保证重入安全）。
     */
    private static final android.view.Choreographer.FrameCallback FRAME =
            new android.view.Choreographer.FrameCallback() {
                public void doFrame(long nanos) {
                    tick();
                }
            };

    private static void scheduleNextFrame() {
        try {
            android.view.Choreographer.getInstance().postFrameCallback(FRAME);
        } catch (Throwable t) {
            AH.postDelayed(TICK, 16L);
        }
    }

    private static void animInit(FlyFan v) {
        int n = (v == null || v.pkgs == null) ? 0 : v.pkgs.length;
        aScale = new float[n];
        aFade = new float[n];
        aAppear = new float[n];
        aTravel = new float[n];
        for (int i = 0; i < n; i++) {
            aScale[i] = 1.0f;
            aFade[i] = 1.0f;
            aAppear[i] = 0.0f;      // 3419：全部从"还没冒出来"开始
        }
        aLean = 0.0f;
        aLeanTarget = 0.0f;
        aSpeed = 0.0f;                  // 3436：速度跟随从 0 起（第一帧 move 就抬起来）
        aLastFrame = 0L;
        aLastMove = 0L;
        // 3450：0 点一律由**第一帧**的手指进度决定（见 tick 里的 aBaseP 捕获）。
        //   这里只把它清成 -1：不管 DOWN 点还是 show 时刻都比第一帧更早，
        //   建窗那 100~300ms 里手指已经走出去了，拿它们当 0 点会让第一颗出生在半路。
        aBaseP = -1.0f;
        aShowAt = 0L;
        aBounceX = 0.0f;                // 3429：触顶回弹状态重置
        aBounceV = 0.0f;
        aBounceDone = false;
        // 3428：「松手收起」模式 → 整体呼出加速（那个模式下必须一口气划到底）
        try {
            aFastOpen = com.oupao.wiz.Cfg.flyStayOff();
        } catch (Throwable ignore) {
            aFastOpen = false;
        }
        aNoAnim = animDisabled();
        v.iconScale = aScale;
        // 3419：首帧 alpha 必须显式给 0（null 会被 drawFan 当成"全不透明"）
        v.iconAlpha = new float[n];
        // 3419：每颗自己的半径 —— 全部从"贴角落"出发，再按各自的出现度往外长
        v.iconR = new float[n];
        for (int i = 0; i < n; i++) {
            v.iconR[i] = v.rMin;
        }
        aDim = 0.0f;
        v.dim = aNoAnim ? 1.0f : 0.0f;
        v.rMul = 1.0f;
        v.gscale = 1.0f;
    }

    /** 系统是否关掉了窗口动画（设置 → 无障碍 / 开发者选项「动画时长缩放」= 关） */
    private static boolean animDisabled() {
        try {
            var c = com.oupao.wiz.Cfg.sysContext();
            if (c == null) {
                return false;
            }
            return android.provider.Settings.Global.getFloat(
                    c.getContentResolver(),
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f) <= 0.01f;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void kick() {
        if (aTicking) {
            return;
        }
        aTicking = true;
        aLastFrame = 0L;
        AH.post(TICK);
    }

    private static void tick() {
        long now = android.os.SystemClock.uptimeMillis();
        float dt = (aLastFrame == 0L) ? 0.016f : (((float) (now - aLastFrame)) / 1000.0f);
        if (dt > 0.033f) {
            dt = 0.033f;
        }
        aLastFrame = now;
        boolean busy = false;

        // ───── ① 打开中的那扇：bloom + 选中焦点 + 速度耦合 ─────
        FlyFan v = fan;
        if (v != null && v.pkgs != null) {
            int n = v.pkgs.length;
            if ((aScale == null) || (aScale.length != n)) {
                animInit(v);
            }
            if (v.iconAlpha == null || v.iconAlpha.length != n) {
                v.iconAlpha = new float[n];
            }
            // 3423：dim 也跟着手指 p 走（刚呼出只一点点暗，随展开加深），再做指数平滑
            float dimTarget = aNoAnim ? 1.0f : Math.min(1.0f, v.p * 1.15f);
            aDim += (dimTarget - aDim) * (aNoAnim ? 1.0f
                    : (1.0f - (float) Math.exp(-DIM_K * (double) dt)));
            v.dim = aDim;
            float kSel = aNoAnim ? 1.0f : (1.0f - (float) Math.exp(-SEL_K * (double) dt));
            float kFoc = aNoAnim ? 1.0f : (1.0f - (float) Math.exp(-FOCUS_K * (double) dt));
            // 3450：0 点取**第一帧**的手指进度（不是 DOWN、也不是 show 时刻）。
            //   show() 到第一帧之间要建窗（100~300ms），这期间手指一直在走；
            //   拿 show 时刻当 0 点时，快划会得到 tS≈1 → 第一颗一露面就在弧最外端。
            if (aBaseP < 0.0f && v.p > 0.0f) {
                aBaseP = Math.min(v.p, 0.75f);
                aShowAt = now;
            }
            float span = 1.0f - Math.max(aBaseP, 0.0f);
            if (span < 0.02f) {
                span = 0.02f;
            }
            float tS = ((aBaseP < 0.0f) ? 0.0f : (v.p - aBaseP) / span);
            if (tS < 0.0f) {
                tS = 0.0f;
            } else if (tS > 1.0f) {
                tS = 1.0f;
            }
            // 3450：时间兜底 —— 手指停在原地时也要让第一颗冒出来（否则菜单只是"压暗"）。
            //   150ms 内把 tS 顶到 0.15，之后完全由手指位置决定。
            if (aShowAt > 0L) {
                float tTime = ((float) (now - aShowAt)) * 0.001f;
                if (tTime > 0.15f) {
                    tTime = 0.15f;
                }
                if (tS < tTime) {
                    tS = tTime;
                }
            }
            // 3434：「松手收起」模式 → **中段 + 尾段**加速，只有**起点**保持原样。
            //
            //   中段：钟形偏置 `t + B·4t²(1-t)²`（两端位置/斜率都不变）
            //   尾段：再乘 `1 + G·smoothstep(0.25, 0.80, t)`
            //         · 起点 w=0 且 w'=0 → 起步斜率依旧是 1（用户只要加快尾段）
            //         · 后段增益最大 → 行程到 ~84% 就吃满 1.0，剩下 16% 不用再划
            //
            //   为什么不是"提高末端斜率"：曲线要始终在 t 之上（整体更快）又必须落在 (1,1)，
            //   数学上末端斜率只能 ≤1；所以"尾段更快"只能表现为**提前到顶**。
            // 3439：速度跟随系数 **两种模式都生效**（用户："你只改了收回那边的速度跟随，
            //   没改呼出时的" —— 上一版只在 aFastOpen 里算，停留模式完全没跟随）。
            // vf=1 ≈ 参考速度 1600px/s；慢划 <1、快划 >1。
            float vf = aSpeed / SPEED_REF;
            if (vf < 0.55f) {
                vf = 0.55f;
            } else if (vf > 2.0f) {
                vf = 2.0f;
            }
            // 3442：**呼出时的速度跟随**（两种模式都生效）。
            //   之前只把"淡入速度"跟手速绑了，行程本身还是纯位置驱动，
            //   于是手划得再快也感觉不到"呼出更快"（用户连着两次提这条）。
            //   vf=1 不动；vf=2 → 行程 ×1.3（77% 行程就完全展开）。
            // 3444：手速对**行程**的加成 0.30 → 0.20（用户："整体速度有点过于快了，稍微慢一点"）
            if ((tS > 0.0f) && (tS < 1.0f) && (vf > 1.05f)) {
                tS = Math.min(1.0f, tS * (1.0f + (0.20f * (vf - 1.0f))));
            }
            if (aFastOpen && (tS > 0.0f) && (tS < 1.0f)) {
                float t = tS;
                float u = 1.0f - t;
                float mid = (FAST_MID_BOOST * vf) * 4.0f * t * t * u * u;
                float w = (t - 0.25f) / 0.55f;
                if (w < 0.0f) {
                    w = 0.0f;
                } else if (w > 1.0f) {
                    w = 1.0f;
                }
                w = w * w * (3.0f - (2.0f * w));                 // smoothstep
                t = (t + mid) * (1.0f + ((FAST_TAIL_GAIN * vf) * w));
                if (t > 1.0f) {
                    t = 1.0f;
                }
                tS = t;
            }
            // 3431：**触顶回弹**。
            //   两个必要条件，缺一个就会"看起来不会回弹"：
            //     ① tS ≥ 0.99 —— 原来要 0.999，而 progressOf 只有在手指离角
            //        ≥ rMax·0.5 时才正好等于 1.0，差一点就永远不触发；
            //     ② **所有图标都到位**（minAppear ≥ 0.95）—— 否则手指划满那一刻
            //        图标还在淡入/半径还在长，回弹被生长掩盖（用户在拖动中就"用完"了
            //        这一次弹的机会，松手停留时反而没得弹）。
            //   aBounceDone 只在 tS<0.90（手指退回）时复位 → 每手势绝对只弹一次。
            float minAppear = 1.0f;
            for (int i = 0; i < n; i++) {
                if (aAppear[i] < minAppear) {
                    minAppear = aAppear[i];
                }
            }
            // 3455：**快划保持现在的幅度，慢划更小**
            //   （用户："保留现在的快速滑动回弹幅度，然后改滑的慢时回弹的幅度就更小"）。
            //   梯度因此和 3452~3454 相反：慢划最小、快划保持 3453 的 ×0.40（等效 V0≈0.80）。
            //     ≤600px/s（慢划）→ ×0.28（等效 0.56，峰值 ≈2%）
            //     ≥1500px/s（快划）→ ×0.40（等效 0.80，与现在一致）
            float sp0 = aSpeed;
            float bw = (sp0 - 600.0f) / 900.0f;
            if (bw < 0.0f) {
                bw = 0.0f;
            } else if (bw > 1.0f) {
                bw = 1.0f;
            }
            float bScale = 0.28f + (0.12f * bw);
            if (tS >= 0.99f) {
                if (!aBounceDone && (minAppear >= 0.95f)) {
                    aBounceDone = true;
                    aBounceV = BOUNCE_V0 * bScale;
                    com.oupao.wiz.SLog.i("FlyFanH: 触顶回弹 tS=" + Math.round(tS * 1000) / 1000.0f
                            + " minAppear=" + Math.round(minAppear * 100) / 100.0f
                            + " sp=" + ((int) aSpeed) + " b=" + (Math.round(bScale * 100) / 100.0f));
                }
            } else if ((tS >= 0.94f) && (minAppear >= 0.95f)
                    && ((now - aLastMove) > 180L) && !aBounceDone) {
                // 3431b：手指没划到 100% 就停住了（大多数人不会正好划满）——
                //   只要"图标已全出现 + 位置已到 94% 以上 + 手指停住 180ms"，
                //   也认作触顶。否则"完全展开"永远等不到那一下回弹。
                aBounceDone = true;
                aBounceV = BOUNCE_V0 * bScale;
                com.oupao.wiz.SLog.i("FlyFanH: 触顶回弹(停住) tS=" + Math.round(tS * 1000) / 1000.0f
                        + " minAppear=" + Math.round(minAppear * 100) / 100.0f
                        + " sp=" + ((int) aSpeed) + " b=" + (Math.round(bScale * 100) / 100.0f));
            } else if (tS < 0.90f) {
                aBounceDone = false;
                aBounceX = 0.0f;
                aBounceV = 0.0f;
            }
            if ((aBounceX != 0.0f) || (aBounceV != 0.0f)) {
                if (aNoAnim) {
                    aBounceX = 0.0f;
                    aBounceV = 0.0f;
                } else {
                    // 半隐式欧拉（dt 已限幅 33ms，稳定）
                    float pv = aBounceV;
                    aBounceV += (((-BOUNCE_K * aBounceX) - (BOUNCE_C * aBounceV)) * dt);
                    aBounceX += (aBounceV * dt);
                    if ((pv > 0.0f) && (aBounceV <= 0.0f)) {
                        // 3432：峰值诊断（每次回弹只一行）—— 用来核对"到底鼓了多少"
                        com.oupao.wiz.SLog.i("FlyFanH: 回弹峰值 x=" + (Math.round(aBounceX * 1000) / 1000.0f));
                    }
                    if ((Math.abs(aBounceX) < 0.0008f) && (Math.abs(aBounceV) < 0.01f)) {
                        aBounceX = 0.0f;
                        aBounceV = 0.0f;
                    } else {
                        busy = true;
                    }
                }
            }
            float bounce = 1.0f + aBounceX;
            // 3424：每颗**各自**指数逼近（不再整体平滑跟随）——
            //   快滑到底时各颗互不拖累，不会"滑到最右那颗了、底下几颗还没显示完"。
            // 3439：淡入速度也跟手速走（两种模式都生效）：滑得快 → 每颗更快追上目标出现度，
            //   整片扇形一口气展开；滑得慢 → 更慢更细。
            // 3443：**快出慢回**（非对称平滑）。
            //   原来"出现"和"收回"用同一个系数：往外划时 aAppear 慢半拍，而半径是它的三次方
            //   → 观感是"先趴角落、后猛冲"，看着完全不跟手；往回划时三次方塌得很快 → 反而跟手。
            //   用户实测："只有往角落里收回菜单的速度跟随，呼出没跟随"。
            //   现在：raw>appear（往外划）用 APPEAR_K_OUT（τ≈30ms，≈2 帧），
            //        raw<appear（往回划）保持 APPEAR_K（τ≈90ms，柔和淡出）。
            float kApp = aNoAnim ? 1.0f
                    : (1.0f - (float) Math.exp(-(APPEAR_K * (0.70f + (0.30f * vf))) * (double) dt));
            float kAppOut = aNoAnim ? 1.0f
                    : (1.0f - (float) Math.exp(-(APPEAR_K_OUT * (0.80f + (0.20f * vf))) * (double) dt));
            int sel = v.sel;
            for (int i = 0; i < n; i++) {
                // ── 出现 = f(手指归一化进度 s)，不是时间轴。
                //   **顺序：从靠近屏幕外缘的那颗开始**（右侧呼出 = 弧上角度最大、
                //   离右边缘最近的那颗），依次往里。
                //   每颗在 APPEAR_SPAN(0.45) 的行程里连续 0→1，且相邻重叠很大 →
                //   淡入柔和；手指停半路 → 半透明；往回划 → 已出来的淡回去。
                float raw = (tS - (((float) ((n - 1) - i)) * APPEAR_STEP)) / APPEAR_SPAN;
                if (raw < 0.0f) {
                    raw = 0.0f;
                } else if (raw > 1.0f) {
                    raw = 1.0f;
                }
                aAppear[i] += (raw - aAppear[i]) * ((raw > aAppear[i]) ? kAppOut : kApp);
                if (Math.abs(raw - aAppear[i]) > 0.003f) {
                    busy = true;
                } else {
                    aAppear[i] = raw;
                }
                boolean on = (i == sel);
                float tScale = (sel < 0) ? 1.0f : (on ? 1.28f : 0.96f);
                aScale[i] += (tScale - aScale[i]) * kSel;
                if (Math.abs(tScale - aScale[i]) > 0.003f) {
                    busy = true;
                } else {
                    aScale[i] = tScale;
                }
                float tFade = (sel < 0) ? 1.0f : (on ? 1.0f : 0.92f);
                aFade[i] += (tFade - aFade[i]) * kFoc;
                if (Math.abs(tFade - aFade[i]) > 0.003f) {
                    busy = true;
                } else {
                    aFade[i] = tFade;
                }
                v.iconScale[i] = aScale[i] * (BLOOM_FROM + ((1.0f - BLOOM_FROM) * aAppear[i]));
                // 3424：**起点更贴角** —— 出现度 0 时半径只有 rMin 的 45%（≈31px，
                // 3426：**目标半径也用归一化后的 tS**（不是原始 p）。
                //   原来用 p：而 p 起步就是 0.12、手指一动就 0.5+，所以第一颗一露头目标
                //   就已经在半路（用户："一呼出就是在这个位置，我想的是刚呼出时在角落"）。
                //   现在 p=0.12（刚呼出）→ tS=0 → targetR = rMin（角落），
                //   手指划到底才长到 rMax。
                if (v.iconR == null || v.iconR.length != n) {
                    v.iconR = new float[n];
                }
                float targetR = v.rMin + ((v.rMax - v.rMin) * tS);
                float fromR = v.rMin * APPEAR_R_FROM;
                // 3433：**先淡入、后位移**（前 RADIUS_DWELL 完全不动 → 出生在角上）。
                // 3443：**驻留之后的行程改成线性**（原来是三次方）——
                //   三次方让图标前 2/3 的行程里几乎不动，最后猛冲，看起来"不跟手"。
                //   现在手划多少，图标就走多少。
                float tr = (aAppear[i] - RADIUS_DWELL) / (1.0f - RADIUS_DWELL);
                if (tr < 0.0f) {
                    tr = 0.0f;
                } else if (tr > 1.0f) {
                    tr = 1.0f;
                }
                float er = tr;
                // 3448：**只限速、不做指数追踪**（3447 用 τ=110ms 追踪 → 慢划也一直拖尾，不跟手）。
                //   向外每帧最多走 `TRAVEL_STEP`：只在"菜单刚建好、目标一下跳到 1"时生效
                //   （建窗要 100~300ms，快划时建好那一刻 `tS` 已经≈1，否则第一颗 150ms 就冲到
                //   600px，眼睛抓到的是"半路出现"）。慢划时单帧变化远小于这个值 → 完全跟手。
                //   往回缩**不限速** → 保持原来"一收就跟着回"的手感。
                if (aTravel == null || aTravel.length != n) {
                    aTravel = new float[n];
                }
                float dTr = er - aTravel[i];
                // 3450：速率 4.5 → 10.0/秒（≈100ms 走完全程）。
                //   4.5 时快划明显"追不上手指"（用户："快速往外划的时候还是不跟手"）。
                float stepTr = aNoAnim ? 1.0f : Math.min(0.5f, 10.0f * dt);
                if (dTr > stepTr) {
                    dTr = stepTr;               // 只削"向外冲"的那一下
                }
                aTravel[i] += dTr;
                if (Math.abs(er - aTravel[i]) > 0.004f) {
                    busy = true;
                }
                float trav = aTravel[i];
                v.iconR[i] = fromR + ((targetR - fromR) * trav);
                // 3428：触顶回弹（只乘在半径上，命中位置跟着走，观感一致）
                v.iconR[i] *= bounce;
                // 3446：透明度**跟着出场行程走**（用户："要展开三分之二后模糊才完全消失"）。
                //   0.62 之前保持"发虚"（平方缓入），过了 ≈2/3 才完全实。
                float soft = trav / 0.62f;
                if (soft > 1.0f) {
                    soft = 1.0f;
                }
                soft = soft * soft;
                v.iconAlpha[i] = soft * aFade[i];
            }
            // 3444：**删掉逐帧的 geo 诊断日志**。
            //   它每帧都拼字符串 + `iconRInt()` 新建 int[] + `Arrays.toString()`
            //   （限流只挡了"写文件"，参数早就造出来了）——
            //   60fps 下就是持续分配/GC，正是"呼出收回掉帧"的一个来源。
            // 速度耦合：拖得越快整体微微鼓一下（手感受限 2%，克制）
            float k = 1.0f - (float) Math.exp(-12.0 * (double) dt);
            aLean += (aLeanTarget - aLean) * k;
            if (Math.abs(aLeanTarget - aLean) > 0.0015f) {
                busy = true;
            }
            // 3431：回弹同时作用在**半径**（整条弧往外鼓）和**图标尺寸**（略微膨胀）；
            //   只推半径时观感像"散开"，带上尺寸才有"整块撞了一下"的弹性。
            //   3432：尺寸取 0.7 倍幅度（12% 全量会让图标看着"跳大小"）。
            v.gscale = (1.0f + aLean) * (1.0f + (aBounceX * 0.7f));
            v.invalidate();
        }

        // ───── ② 收起中的那扇：150ms ease-in 淡出 + 缩回角落，结束后 removeView ─────
        FlyFan cl = aClosing;
        if (cl != null) {
            if (aNoAnim) {
                removeClosing();
            } else {
                float ct = ((float) (now - aCloseAt)) / CLOSE_MS;
                if (ct >= 1.0f) {
                    removeClosing();
                } else {
                    float e = ct * ct;                     // ease-in：慢起快走
                    if ((cl.iconAlpha != null) && (aCloseAlpha != null)) {
                        int n = Math.min(cl.iconAlpha.length, aCloseAlpha.length);
                        for (int i = 0; i < n; i++) {
                            cl.iconAlpha[i] = aCloseAlpha[i] * (1.0f - e);
                        }
                    }
                    cl.dim = 1.0f - e;
                    cl.rMul = 1.0f - ((1.0f - CLOSE_TO) * e);
                    cl.invalidate();
                    busy = true;
                }
            }
        }

        if (busy || ((android.os.SystemClock.uptimeMillis() - aLastMove) < 400L)) {
            // 3444：**改成 vsync 对齐**（Choreographer）。原来是固定 16ms 的 postDelayed，
            //   在 system_server 主线程上很容易和 WMS/AMS 的事务挤在一起 →
            //   连掉两帧（用户："呼出收回动画有点掉帧"）。拿不到就退回原来的定时。
            scheduleNextFrame();
        } else {
            aTicking = false;
            try {
                android.view.Choreographer.getInstance().removeFrameCallback(FRAME);
            } catch (Throwable ignore) {
            }
        }
    }


    /**
     * 3424：扇形是否**已经真正完全展开**（所有图标都显形了）。
     *
     * 命中判定的闸门：不满这个条件时 `sel` 恒为 -1 → 不会振动、松手也不会启动，
     * 避免"快速呼出时手指正好压在还没冒出来的图标上"造成误触。
     */
    static boolean fullyAppeared() {
        FlyFan v = fan;
        if ((v == null) || (v.pkgs == null)) {
            return false;
        }
        if (aAppear == null || aAppear.length != v.pkgs.length) {
            return false;
        }
        if (v.p < 0.995f) {
            return false;
        }
        for (int i = 0; i < aAppear.length; i++) {
            if (aAppear[i] < 0.98f) {
                return false;
            }
        }
        return true;
    }

    /** 收起动画结束：真正把窗口摘掉 */
    private static void removeClosing() {
        FlyFan cl = aClosing;
        aClosing = null;
        aCloseAlpha = null;
        aCloseAt = 0L;
        if ((cl != null) && (wm != null)) {
            try {
                wm.removeView(cl);
            } catch (Throwable ignore) {
            }
        }
        try { com.oupao.wiz.NavPill.requestShow(); } catch (Throwable ignore) { }
    }

    private FlyFanH() {
    }

    public static boolean alive() {
        return fan != null;
    }

    /**
     * ★ 2778：扇形半径的**唯一真值来源**（真菜单和设置页的预览都走这里）。
     *
     * 之前两处各写一套：FlyFanH 用 `160dp × 扇区%`，而设置页的紫色预览（FlyPrev）用的是
     * `200dp × 扇区% + 36dp×1.3/2` —— 预览比真菜单大约 1.4 倍，
     * 用户看到的就是"我设的扇区大小和实际的不一致"。
     */
    /**
     * ★ 2800：扇区半径基准 **160dp → 176dp**（= +10%）。
     * 用户："把扇区大小 110% 的值改为 100%，现在 100% 还是有点小" ——
     * 即让滑块 100% 画出来 = 改之前 110% 的大小。仍然是严格线性。
     */
    public static final float R_BASE_DP = 176.0f;    // 原来写死 160
    public static final float R_MIN_DP = 19.8f;      // 起手小弧，同步 +10%（原来 18）

    public static float fanRadiusPx(android.content.Context c, int scalePct) {
        android.util.DisplayMetrics dm = c.getResources().getDisplayMetrics();
        int pct = (scalePct < 10) ? 100 : scalePct;
        float r = R_BASE_DP * dm.density * (((float) pct) / 100.0f);
        float limit = 0.95f * Math.min((float) dm.widthPixels, (float) dm.heightPixels);
        if (r > limit) {
            r = limit;
        }
        return r;
    }

    /**
     * ★ 2790：图标直径 = **绝对 dp**，和「扇区大小」彻底解绑。
     *
     * 之前它是由"弧上相邻间距"推出来的（`spacing × 0.88 × 0.9`），于是改扇区→间距变→图标跟着变，
     * 用户明确要求"两个滑块不要绑定"。
     * 现在：`直径 = 32.7dp × density × 图标%`，只跟「图标大小」有关。
     *   32.7dp 是让「扇区 100% + 图标 100%」的观感和改之前一致（=114px @density 3.5）。
     */
    public static final float ICON_BASE_DP = 32.7f * 1.1f;   // 3272：用户要求 100% 显示成原来 110% 的大小

    public static float fanIconPx(android.content.Context c, int iconPct) {
        int pct = (iconPct < 10) ? 100 : iconPct;
        float d = c.getResources().getDisplayMetrics().density;
        return ICON_BASE_DP * d * (((float) pct) / 100.0f);
    }

    /** 同上，用当前保存的「扇区大小」 */
    public static float fanRadiusPx(android.content.Context c) {
        int pct = 100;
        try {
            pct = com.oupao.wiz.Cfg.flyScale();
        } catch (Throwable ignore) {
        }
        return fanRadiusPx(c, pct);
    }

    /** 手指到角的距离 → 0~1 进度 */
    public static float progressOf(float x, float y) {
        float dx = (float) Math.abs((double) (x - cornerX));
        float dy = (float) Math.abs((double) (y - cornerY));
        float d = (float) Math.sqrt((double) ((dx * dx) + (dy * dy)));
        // 2751：菜单**一出来就基本是完整扇形**（起手 0.55），再随手指长到 1.0。
        //   之前 p 从 0 起，用户划不到 rMax*0.8 就一直是"半残"的扇形。
        float k = d / Math.max(1.0f, rMax * 0.5f);
        if (k < 0.0f) {
            k = 0.0f;
        }
        if (k > 1.0f) {
            k = 1.0f;
        }
        return 0.12f + (0.88f * k);   // 2760：刚呼出时几乎是贴在角落的小弧，随手指长大
    }

    /** 建立（或复用）绘制窗口 */
    public static void show(android.content.Context c, int side) {
        show(c, side, Float.NaN, Float.NaN);
    }

    /**
     * @param x,y 呼出这一刻的手指位置（3433）。不知道时传 NaN，退回「DOWN 点」基准。
     */
    public static void show(android.content.Context c, int side, float x, float y) {
        try {
            // 3501：最后一道闸——任何路径在输入法打开时都不许建菜单。
            //   FlyFanBr 已在 DOWN 拦过；这里防止旧路径/停留态/其他调用方绕过。
            if (com.oupao.wiz.Hooks.imeShown()) {
                com.oupao.wiz.SLog.r("r-fan-ime", "FlyFanH: 输入法打开 不呼出", 1000L);
                return;
            }
            if (fan != null) {
                // 3430：**菜单还开着（停留等选择）时，不允许重新呼出**。
                //   以前这里会把 staying 清掉、把窗口改回 NOT_TOUCHABLE，
                //   于是"再划一次角落"又把这扇菜单拖着走（用户说的"重新呼出"）。
                //   现在：停留状态直接忽略新手势，别再动它 —— 要关它请点空白处/上滑。
                if (staying) {
                    return;
                }
                // 非停留（还在划选手势中）→ 保持原行为：回到普通状态
                setTouchable(false);
                return;
            }
            // 3418：上一扇还在退场 → 立刻摘掉，别让两扇叠在一起
            if (aClosing != null) {
                removeClosing();
            }
            android.content.Context base = (c != null) ? c : com.oupao.wiz.Cfg.sysContext();
            if (base == null) {
                return;
            }
            ctx = base;
            wm = (android.view.WindowManager) base.getSystemService("window");
            if (wm == null) {
                return;
            }
            android.util.DisplayMetrics dm = base.getResources().getDisplayMetrics();
            float density = dm.density;
            // ★ 2776：「扇区大小」滑块（flyScale 80~120%）真正接上 —— 之前这里是写死的 18dp/160dp，
            //   滑块只影响 FlyPrev 的预览，真菜单一点没变（用户报的就是这个）。
            int scalePct = 100;
            try {
                scalePct = com.oupao.wiz.Cfg.flyScale();
            } catch (Throwable ignore) {
            }
            if (scalePct < 10) {
                scalePct = 100;
            }
            float sk = ((float) scalePct) / 100.0f;
            rMin = R_MIN_DP * density * sk;      // 起手小弧（贴着角）
            rMax = fanRadiusPx(base, scalePct);  // 满展开半径（与设置页预览同一公式）

            try {
                if (com.oupao.wiz.FlyH.win != null) {
                    com.oupao.wiz.FlyH.hide();
                }
            } catch (Throwable ignore) {
            }
            FlyFan v = new FlyFan(base);
            v.side = side;
            v.rMin = rMin;
            v.rMax = rMax;
            // ★ 2776：「图标大小」滑块（iconSize 80~120%）真正接上。
            //   原来把百分比当成 dp 用（iconSize=100 → 100dp = 350px），再被下面的 cap 一压，
            //   100% 和 120% 都被压成同一个值 → 滑块看着"完全没反应"。
            //   现在：100% = 弧上相邻间距的 0.88 倍（也就是原来看到的那个大小），
            //         滑块在这个格子上等比缩放，硬上限 1.02×间距（只挡"糊成一坨"）。
            int iconPct = 100;
            try {
                iconPct = com.oupao.wiz.Cfg.iconSize();
            } catch (Throwable ignore) {
            }
            if (iconPct < 10) {
                iconPct = 100;
            }
            int cnt = (v.pkgs == null) ? 6 : v.pkgs.length;
            // 2778：上限（0.95×短边）已经在 fanRadiusPx() 里做了，这里不再重复封顶。
            // 2779：图标尺寸走 fanIconPx()，和设置页预览同一个公式。
            v.iconPx = fanIconPx(base, iconPct);   // 2790：与扇区无关

            com.oupao.wiz.SLog.i("FlyFanH: iconPx=" + ((int) v.iconPx) + " rMax=" + ((int) rMax)
                    + " rMin=" + ((int) rMin) + " scale=" + scalePct + "% icon=" + iconPct + "%");
            v.pkgs = pins(base);
            animInit(v);
            // ★ 2783：把"固定应用列表"快照写进 Settings.Global。
            //   设置页的预览跑在 App 进程，读不到 /data/system/oupao_cfg（权限），
            //   所以以前预览里 pins 是空的（只剩一个 @more）→ 预览画不出图标。
            try {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < v.pkgs.length; i++) {
                    if ("@more".equals(com.oupao.wiz.FlyFan.pkgOf(v.pkgs[i]))) {
                        continue;
                    }
                    if (sb.length() > 0) {
                        sb.append(';');
                    }
                    sb.append(v.pkgs[i]);
                }
                com.oupao.wiz.Cfg.gset("fan_pins_snapshot", sb.toString());
            } catch (Throwable ignore) {
            }
            v.p = 0.0f;
            v.sel = -1;

            android.view.WindowManager.LayoutParams p = new android.view.WindowManager.LayoutParams();
            p.type = 2003;
            p.flags = 0x00000118;      // NOT_FOCUSABLE | NOT_TOUCHABLE | LAYOUT_IN_SCREEN
            p.width = -1;
            p.height = -1;
            p.format = -3;
            // 3418：**不再用 WMS 的 FLAG_DIM_BEHIND** —— 它只能一步到位（没法淡入），
            //   改由 FlyFan.drawFan 自绘 dim（180ms 淡入 / 150ms 淡出）。
            p.setFitInsetsTypes(0);
            com.oupao.wiz.FlyS.trust2(p);
            wm.addView(v, p);
            fan = v;
            try { com.oupao.wiz.NavPill.requestHide(); } catch (Throwable ignore) { }   // 2909：别让底部小白条跟着动
            try { com.oupao.wiz.FlyState.setOpen(true); } catch (Throwable ignore) { }
            lp = p;
            staying = false;
            cornerX = (side == 1) ? 0.0f : ((float) dm.widthPixels);
            cornerY = (float) dm.heightPixels;
            // 3450：这里**不再设 0 点** —— 建窗还要 100~300ms，那一刻的手指位置
            //   早已不是"呼出瞬间"了。0 点统一交给 tick 的第一帧（aBaseP < 0 时捕获）。
            kick();      // 3418：bloom 从这里开始跑（之前要等手指划满 p=1 才动）
            AH.removeCallbacks(HANG_WD);       // 3436：手势断流兜底
            AH.postDelayed(HANG_WD, HANG_MS);
            com.oupao.wiz.SLog.i("FlyFanH: 自绘菜单已建 side=" + side + " n="
                    + ((v.pkgs == null) ? 0 : v.pkgs.length));
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("FlyFanH: 建立失败", t);
            fan = null;
        }
    }

    /** 每帧跟手 */
    public static void move(float x, float y, boolean selectable) {
        FlyFan v = fan;
        if (v == null) {
            return;
        }
        try {
            // 3442：**停留中不再接受驱动**。
            //   以前 `show()` 里挡住了"重新呼出"，但 `move()` 还会照常跟着手指走 ——
            //   于是菜单停住之后再划一次角落，它就被**拖着走**（用户："现在还是可以再次呼出菜单"）。
            if (staying) {
                return;
            }
            AH.removeCallbacks(HANG_WD);       // 3436：有 MOVE 就是心跳，重新计时
            AH.postDelayed(HANG_WD, HANG_MS);
            v.p = progressOf(x, y);
            // 3426：基准点必须在**第一次 move()** 就取（比 tick 更早）——
            //   放到 tick 里会晚一两帧，那时手指已经又走了一段，基准偏大。
            // 3450：0 点不在这里取（这里比第一帧早），交给 tick。
            // 3424：**完全展开前不允许选中**。
            //   以前只看 `p >= 0.999`：快速呼出时 p 一下冲到 1，手指正好压在某个图标上
            //   → 立刻命中 → 振动（用户："每次呼出快一点就会误触到图标导致振动"）。
            //   现在还要所有图标都真正显形（aAppear≈1），才算"展开完成、可以选"。
            v.sel = (selectable && fullyAppeared()) ? v.hit(x, y) : -1;

            // 2791：速度耦合（拖得越快，图标整体略微鼓一点 → 有"被拖着走"的惯性感）
            long now = android.os.SystemClock.uptimeMillis();
            float dt = (aLastMove == 0L) ? 0.016f : (((float) (now - aLastMove)) / 1000.0f);
            if (dt > 0.001f && dt < 0.2f) {
                float sp = (float) Math.sqrt((double) (((x - aLastX) * (x - aLastX)) + ((y - aLastY) * (y - aLastY)))) / dt;
                aLeanTarget = Math.min(0.02f, sp * 0.00002f);   // Flyme 克制：≤2%
                // 3436：速度跟随 —— 平滑（τ≈100ms）后给 tick 用
                aSpeed += (sp - aSpeed) * (1.0f - (float) Math.exp(-10.0 * (double) dt));
            }
            aLastX = x;
            aLastY = y;
            aLastMove = now;

            // 3418：原来的「划满 p>=0.999 才弹一次」已删除 —— bloom 在 show() 就开始跑，
            //   不再有"长出来之后又莫名弹一下"的第二跳。
            v.gscale = 1.0f + aLean;
            v.invalidate();
            kick();
        } catch (Throwable ignore) {
        }
    }

    /**
     * 2776：「未选应用时松手收起 = 关」时的行为 —— 菜单**停在原地等选择**。
     *
     * 做法：扇形补满到 100%，并把绘制窗口从 NOT_TOUCHABLE 改成可触摸（全屏、带 dim），
     * 这样后面的点击由我们接住：点到图标就启动，点到别处就收起。
     */
    public static void stay() {
        FlyFan v = fan;
        if (v == null) {
            return;
        }
        try {
            v.p = 1.0f;
            v.sel = -1;
            v.invalidate();
            staying = true;
            aStayAt = android.os.SystemClock.uptimeMillis();   // 3445：只认"停住之后"发生的离开手势
            // 3455：记下"停住这一刻是不是已经在多任务里" —— 已在的话，标志位不能当成"刚打开多任务"
            try {
                aRecAtStay = com.oupao.wiz.Sug.recentsOpen();
            } catch (Throwable ignore) {
                aRecAtStay = false;
            }
            setTouchable(true);
            kick();
            com.oupao.wiz.SLog.i("FlyFanH: 停留等选择");
        } catch (Throwable ignore) {
        } finally {
            // 3436：看门狗**必须**挂上 —— 以前这几句在 try 里，前面任何一句抛异常
            //   就会得到"菜单停在屏幕上、但没人盯着多任务/通知中心"的死局
            //   （用户："在桌面上呼出多任务/控制中心根本不会关闭菜单"）。
            AH.removeCallbacks(STAY_WATCH);
            AH.postDelayed(STAY_WATCH, STAY_MS);
        }
    }

    /**
     * 3436：**手势事件断流兜底**（桌面/侧边手势路径实测会丢 UP）。
     *
     * `FlyFanBr` 那条 WMS 旁听有自己的 700ms 看门狗，但它只在"确实收到过事件"时才会挂上；
     * 如果菜单是由 `FlySideHook`（SystemUI 侧手势检测）建起来的、而那条流的 UP 又丢了，
     * 菜单就会永远挂在屏幕上（`staying=false`，也就没有 STAY_WATCH）。
     * 这里以 `move()` 为心跳：900ms 没有新的 MOVE 就当作已松手 ——
     * 「松手收起=开」→ 收起，「关」→ 停留（交给 STAY_WATCH 继续盯）。
     */
    private static final long HANG_MS = 900L;
    private static final Runnable HANG_WD = new Runnable() {
        public void run() {
            try {
                if ((fan == null) || staying) {
                    return;
                }
                boolean stayOff = false;
                try {
                    stayOff = com.oupao.wiz.Cfg.flyStayOff();
                } catch (Throwable ignore) {
                }
                com.oupao.wiz.SLog.i("FlyFanH: 手势断流 900ms → " + (stayOff ? "收起" : "停留"));
                if (stayOff) {
                    hide();
                } else {
                    stay();
                }
            } catch (Throwable ignore) {
            }
        }
    };

    /**
     * 停留状态下点到屏幕：命中图标就启动，否则收起。
     */
    public static void tap(float x, float y) {
        FlyFan v = fan;
        if (v == null) {
            return;
        }
        try {
            v.p = 1.0f;
            // 3424：还没完全展开时不接受选择（同 move() 的闸门），点哪都只收起
            int i = fullyAppeared() ? v.hit(x, y) : -1;
            if (i < 0) {
                hide();
                return;
            }
            v.sel = i;
            v.invalidate();
            kick();
            try {
                com.oupao.wiz.FlyH.buzz();
            } catch (Throwable ignore) {
            }
            // 3438：**把要启动的条目先抓下来**。
            //   3436 只记了 `v.sel`，但同一个 UP 还会走到 FlyFanBr 的 UP 分支 → stay()
            //   → `stay()` 里 `v.sel = -1` → 110ms 后 TAP_GO 看到 sel<0，什么都不启动
            //   （用户实测："菜单展开后点击图标没法打开，划选正常"）。
            aTapFan = v;
            aTapPkg = ((v.pkgs != null) && (i < v.pkgs.length)) ? v.pkgs[i] : null;
            AH.removeCallbacks(TAP_GO);
            AH.postDelayed(TAP_GO, TAP_MS);
        } catch (Throwable t) {
            hide();
        }
    }

    /** 3436：点击图标后延迟启动（先播按下反馈）。hide() 里会把它撤掉。 */
    private static final long TAP_MS = 110L;
    /** 3438：待启动的菜单 + 条目（不依赖 `v.sel`，避免被 stay() 重置） */
    private static FlyFan aTapFan;
    private static String aTapPkg;
    private static final Runnable TAP_GO = new Runnable() {
        public void run() {
            try {
                FlyFan v = aTapFan;
                String pkg = aTapPkg;
                aTapFan = null;
                aTapPkg = null;
                if ((v == null) || (pkg == null)) {
                    return;
                }
                // 菜单已经被别的路径收掉了就别补发（用户已经切走了）
                if (fan != v) {
                    return;
                }
                com.oupao.wiz.SLog.i("FlyFanH: 点击启动 " + pkg);
                launch(v, pkg);
                hide();
            } catch (Throwable ignore) {
            }
        }
    };

    /**
     * 3430：**停留中自动收起**的看门狗。
     *
     * 需求：菜单停在原地等选择时，如果用户去呼出多任务 / 通知中心 / 控制中心，
     * 菜单要自己收起（否则它会压在上面，观感像"卡住"）。
     *
     * 判据两条，任一中就收：
     *   · `Sug.recentsOpen()` —— 多任务（FlyGate 在桌面 onStateSetEnd 时写的全局位）；
     *   · `oupao_shade_open` —— 通知中心/控制中心（SystemUI 侧 [ShadeWatch] 写的全局位）。
     * 400ms 一轮：代价很小（两次 Settings 读，已带 300ms 缓存），只在"停留"期间跑。
     */
    private static final Runnable STAY_WATCH = new Runnable() {
        public void run() {
            boolean keep = false;
            try {
                if (!staying || (fan == null)) {
                    return;
                }
                String why = null;
                // ① `toggleRecentApps` 刚被调用过（system_server 自己调的，最及时）
                //    3455：必须**晚于进入停留那一刻**才认 —— 否则用户上一次开过多任务、
                //    2.5 秒内呼出菜单会被立刻收掉（用户："概率呼出的菜单也会自动关闭"）。
                if ((com.oupao.wiz.Hooks.recentsAtMs() >= aStayAt)
                        && (com.oupao.wiz.Hooks.recentsAgeMs() < 2500L)) {
                    why = "多任务(toggle)";
                }
                // ② 多任务（桌面 FlyGate 写的标志位）
                //    3455：同理 —— 进入停留时**本来就在**多任务里不算（标志位可能是上一轮留下的）。
                if (why == null && !aRecAtStay && com.oupao.wiz.Sug.recentsOpen()) {
                    why = "多任务";
                }
                // ②b 3442：桌面底部上滑（回桌面/多任务）——桌面自己写的时间戳。
                //   这条手势被桌面的 InputConsumer 吃掉，我们的窗口收不到 UP/CANCEL，
                //   只能靠它（用户："桌面呼出菜单后上滑多任务界面不会关闭菜单"）。
                if (why == null) {
                    android.content.Context c0 = com.oupao.wiz.Cfg.sysContext();
                    if (c0 != null) {
                        String av = com.oupao.wiz.Cfg.ggetC(c0, "oupao_away_at");
                        if ((av != null) && (av.length() > 0)) {
                            try {
                                long ts = Long.parseLong(av);
                                long age = android.os.SystemClock.uptimeMillis() - ts;
                                // 3461：**age 必须是 0..3000**。
                                //   这个值存在 Settings.Global 里、会跨重启保留，而 uptimeMillis
                                //   重启后归零 → 上个开机周期的旧值会比现在还大（实测 age=-66079ms），
                                //   `age < 3000` 恒成立 → 菜单每次停留都被当成"用户刚离开"立刻收掉
                                //   （用户："概率呼出的菜单也会自动关闭"）。
                                if ((ts >= aStayAt) && (age >= 0L) && (age < 3000L)) {
                                    why = "桌面底部手势";
                                }
                            } catch (Throwable ignore) {
                            }
                        }
                    }
                }
                // ② **焦点窗口**（一手信号，不依赖任何标志位）：
                //    3436：通知中心/控制中心展开时 mCurrentFocus = Window{… NotificationShade}
                if (why == null) {
                    String t = com.oupao.wiz.Sug.fgWinTitle();
                    if ((t != null) && t.contains("NotificationShade")) {
                        why = "通知中心/控制中心";
                    }
                }
                // ③ 兜底：SystemUI 侧 ShadeWatch 写的标志位
                if (why == null) {
                    android.content.Context c = com.oupao.wiz.Cfg.sysContext();
                    if ((c != null) && "1".equals(com.oupao.wiz.Cfg.ggetC(c, "oupao_shade_open"))) {
                        why = "通知中心/控制中心(标志位)";
                    }
                }
                // 3444：诊断探针已撤（`看门狗 n=…` 每 2.5s 一次完整反射链，
                //   菜单停留期间白烧 CPU；判据本身已经验证过了）。
                if (why != null) {
                    com.oupao.wiz.SLog.i("FlyFanH: 停留中 → " + why + "打开，收起菜单");
                    hide();
                    return;
                }
                keep = true;
            } catch (Throwable ignore) {
                // 3436：**绝不能因为一次异常就停掉看门狗**（否则菜单永远收不起来）
                keep = true;
            } finally {
                if (keep) {
                    AH.postDelayed(STAY_WATCH, STAY_MS);
                }
            }
        }
    };

    /** 3436：看门狗轮次（探针用） */    /** 3436：看门狗轮询间隔（400 太慢，用户体感关得慢；250ms 代价仍然很小） */
    private static final long STAY_MS = 250L;

    private static void setTouchable(boolean on) {        android.view.WindowManager.LayoutParams p = lp;
        FlyFan v = fan;
        if ((p == null) || (v == null) || (wm == null)) {
            return;
        }
        try {
            if (on) {
                p.flags = p.flags & (~android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            } else {
                p.flags = p.flags | android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            }
            wm.updateViewLayout(v, p);
        } catch (Throwable ignore) {
        }
    }

    /** 松手：启动选中的应用，然后关掉 */
    public static void release() {
        FlyFan v = fan;
        try {
            if (v != null && v.sel >= 0 && v.pkgs != null && v.sel < v.pkgs.length) {
                launch(v, v.pkgs[v.sel]);
            }
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("FlyFanH: 启动失败", t);
        }
        hide();
    }

    /**
     * 2759：**完全复用旧菜单的启动逻辑**（`FlyH.launch`）。
     *
     * 它已经处理了：
     *  - `包名#Activity[#分身id]` → 分身走 `cloneLaunch`，普通走 `go(...)`（= 小窗模式启动）；
     *  - `@more` → 按 `Cfg.moreMode()`：`window` 用小窗打开 MainActivity「更多应用」页，
     *    否则调 `MorePanel.show()` 打开原来的面板；
     *  - `tool#xxx` → 系统工具项。
     * 我们只负责把选中的条目交给它，不再自己写一套。
     */
    private static void launch(FlyFan v, String entry) {
        if (entry == null) {
            return;
        }
        com.oupao.wiz.FlyH.launch(entry);
        com.oupao.wiz.SLog.i("FlyFanH: FlyH.launch " + entry);
    }

    /** 是不是"停留等选择"状态（此时窗口可触摸、由我们接住点击） */
    public static boolean staying() {
        return staying && (fan != null);
    }

    /** 当前选中下标（-1 = 没划到任何图标） */
    public static int sel() {
        FlyFan v = fan;
        return (v == null) ? -1 : v.sel;
    }

    /**
     * 3418：收起不再硬切 —— 先播 150ms `ease-in` 收拢（alpha→0、半径缩回 94%、dim 淡出），
     * 播完再 `removeView`。
     *
     * 关键：`fan` 立刻置 null（`alive()` 马上变 false，避免上层逻辑把"正在退场的菜单"
     * 当成还开着 → 误收手势/误判点击）；真正移除交给 `aClosing` + TICK。
     * 重复调用是安全的（`aClosing` 只会有一份，且已在播就不再重启）。
     */
    public static void hide() {
        try { com.oupao.wiz.FlyState.setOpen(false); } catch (Throwable ignore) { }
        hideAt = android.os.SystemClock.uptimeMillis();   // 3248：给 onOutsideTap 的宽限窗
        staying = false;
        AH.removeCallbacks(TAP_GO);        // 3436：退场就别再补一次启动
        aTapFan = null;                    // 3438：待启动的条目也一并清掉
        aTapPkg = null;
        AH.removeCallbacks(HANG_WD);       // 3436：手势断流兜底也撤掉
        FlyFan v = fan;
        // 3418：退场期间窗口要立刻失去触摸能力（它可能刚从"停留等选择"的可触摸态过来，
        //   不收回来的话这 150ms 的淡出层会把用户的点击吃掉）
        setTouchable(false);
        fan = null;
        // 清掉"打开中"的动画状态（看清：aClosing 用的是另一套，不受影响）
        aScale = null;
        aFade = null;
        aAppear = null;
        aTravel = null;
        aLean = 0.0f;
        aLeanTarget = 0.0f;
        aLastMove = 0L;
        if (v == null) {
            // 没有正在显示的菜单（可能已经在退场）→ 什么都不用做
            return;
        }
        // 退场期间不允许再触摸（它本来也该是 NOT_TOUCHABLE）
        aNoAnim = aNoAnim || animDisabled();
        if (aNoAnim) {
            try {
                if (wm != null) {
                    wm.removeView(v);
                }
            } catch (Throwable ignore) {
            }
            try { com.oupao.wiz.NavPill.requestShow(); } catch (Throwable ignore) { }
            return;
        }
        // 同一时刻只允许一份退场动画：上一份还没结束就直接摘掉
        if (aClosing != null) {
            removeClosing();
        }
        aClosing = v;
        aCloseAt = android.os.SystemClock.uptimeMillis();
        // 记住退场起点的不透明度（从当前 bloom 值平滑接着淡出）
        if (v.iconAlpha != null) {
            aCloseAlpha = new float[v.iconAlpha.length];
            System.arraycopy(v.iconAlpha, 0, aCloseAlpha, 0, v.iconAlpha.length);
        } else {
            aCloseAlpha = null;
        }
        // 停掉"打开中"的 TICK，让下一帧只跑退场那支
        aTicking = false;
        AH.removeCallbacks(TICK);
        try {
            android.view.Choreographer.getInstance().removeFrameCallback(FRAME);   // 3444
        } catch (Throwable ignore) {
        }
        AH.removeCallbacks(STAY_WATCH);     // 3430：退场后不再盯多任务/面板
        kick();
    }

    /** 固定应用列表（复用模块现有配置） */
    public static String[] pins(android.content.Context c) {
        try {
            java.util.ArrayList<String> raw = new java.util.ArrayList<>();
            raw.addAll(com.oupao.wiz.Pins.parse(com.oupao.wiz.Cfg.fanPins()));
            java.util.List<String> l = com.oupao.wiz.Pins.cap5(raw, raw.size());
            // 2755：@more 放在**最前**（角度最小 → 贴近底边），和旧菜单一样在底部
            String[] out = new String[l.size() + 1];
            out[0] = "@more";
            for (int i = 0; i < l.size(); i++) {
                out[i + 1] = l.get(i);
            }
            // 2835 诊断：把扇形菜单**实际拿到的**列表打出来，和「更多应用」页面对比用
            com.oupao.wiz.SLog.r("r-fanpins", "FlyFanH: pins=" + java.util.Arrays.toString(out)
                    + " raw=" + com.oupao.wiz.Cfg.fanPins(), 2000L);
            return out;
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("FlyFanH: 取固定应用失败", t);
            return new String[0];
        }
    }
}
