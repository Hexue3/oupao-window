package com.oupao.wiz;

/**
 * 自绘扇形菜单。
 *
 * 为什么自绘：这台 ROM 上只要对菜单窗口/子 View 做 transform（scale/translation），
 * 菜单就不渲染（2736~2739 反复实测）。用 Canvas 自己画则完全绕开这个问题。
 *
 * 绘制规则：
 *  - 圆心 = 屏幕那个下角；图标沿一条 74° 圆弧（8°~82°）均匀分布；
 *  - 弧半径 = `rMin + (rMax - rMin) * p`，图标尺寸同比例缩放；
 *  - p 由 [FlyFanH] 按手指到角的距离每帧更新 → 跟手。
 *
 * ★ 2779：把绘制逻辑抽成**静态** `drawFan(...)`，让设置页的预览（[FlyPrev]）用**同一份代码**画，
 *   "预览 = 实际菜单"因此是结构上保证的，而不是两边各写一套公式。
 */
public class FlyFan extends android.view.View {

    /** 每个 item 的包名（"@more" 表示"更多"） */
    public String[] pkgs;

    /** 1 = 左角，2 = 右角 */
    public int side = 2;

    public float rMin;
    public float rMax;
    public float iconPx;

    /** 0~1 呼出进度 */
    public float p = 1.0f;

    /** 当前选中的下标，-1 = 无 */
    public int sel = -1;

    /**
     * 2791：每个图标的**最终缩放倍数**（含选中放大、呼出落定的错峰弹跳）。
     * 由 [FlyFanH] 每帧算好塞进来；null = 退回"选中 1.28、其余 1.0"的旧行为。
     * 只影响绘制，**不参与命中判定**（hit() 仍用 iconPx），免得动效把选择搞飘。
     */
    public float[] iconScale;

    /**
     * 3418：每个图标的**不透明度**（0~1，含呼出 bloom、选中失焦压暗、收起淡出）。
     * 由 [FlyFanH] 每帧算好塞进来；null = 全部 1.0（预览用）。
     * 只影响绘制，不参与命中判定。
     */
    public float[] iconAlpha;

    /** 3418：背景压暗强度 0~1（绘制时映射到 25% 黑）。由 [FlyFanH] 每帧算，随呼出/收起淡入淡出。 */
    public float dim = 1.0f;

    /** 3418：半径收缩倍率（1.0 = 正常；收起时趋 0.9，让扇形缩回角落） */
    public float rMul = 1.0f;

    /**
     * 3419：**每颗图标各自的半径**。
     *
     * 以前所有图标共享一个 `r = rMin + (rMax-rMin)*p`，于是整排是被一起"推"出去的
     * （用户原话：「不要这种全部图标一起往外推的感觉」）。
     * 现在每颗有自己的半径：`@more` 先长出、其余依次从角落冒出来飞到自己位置。
     * null（预览）= 退回共享半径的统一圆弧。
     */
    public float[] iconR;

    /** 全局缩放（速度耦合的整体呼吸，1.0 = 不缩放） */
    public float gscale = 1.0f;

    private static final android.graphics.Paint DOT = new android.graphics.Paint();
    private static final android.graphics.Paint TXT = new android.graphics.Paint();
    private static final android.graphics.Paint SHADER = new android.graphics.Paint();
    private static final android.graphics.RectF TMP = new android.graphics.RectF();
    private static final java.util.HashMap<String, android.graphics.Bitmap> BMP = new java.util.HashMap<>();
    private static final java.util.HashMap<String, android.graphics.drawable.Drawable> ICONS =
            new java.util.HashMap<>();
    private static final android.graphics.Paint MORE_BG = new android.graphics.Paint();
    private static final android.graphics.Paint MORE_DOT = new android.graphics.Paint();
    private static boolean paintsReady;

    public FlyFan(android.content.Context c) {
        super(c);
        ensurePaints();
        this.setWillNotDraw(false);
    }

    private static void ensurePaints() {
        if (paintsReady) {
            return;
        }
        paintsReady = true;
        TXT.setAntiAlias(true);
        TXT.setColor(0xffffffff);
        DOT.setAntiAlias(true);
        DOT.setStyle(android.graphics.Paint.Style.FILL);
        DOT.setColor(0x66ffffff);
        SHADER.setAntiAlias(true);
        SHADER.setFilterBitmap(true);
        MORE_BG.setAntiAlias(true);
        MORE_BG.setStyle(android.graphics.Paint.Style.FILL);
        MORE_BG.setColor(-13335809);            // 与 Bar.more 的底色同一个蓝
        MORE_DOT.setAntiAlias(true);
        MORE_DOT.setStyle(android.graphics.Paint.Style.FILL);
        MORE_DOT.setColor(0xFFFFFFFF);
        TXT.setTextAlign(android.graphics.Paint.Align.CENTER);
        TXT.setTextSize(28.0f);
    }

    /** 屏幕角点（像素） */
    private static float cxOf(android.content.Context c, int side) {
        return (side == 1) ? 0.0f : ((float) c.getResources().getDisplayMetrics().widthPixels);
    }

    private static float cyOf(android.content.Context c) {
        return (float) c.getResources().getDisplayMetrics().heightPixels;
    }

    /** 第 i 个图标的方向角（0 = 水平指向屏幕内侧，90 = 正上方） */
    public static float ang(int i, int n) {
        return (n <= 1) ? 45.0f : (8.0f + (((float) i) * (74.0f / ((float) (n - 1)))));
    }

    /** 第 i 个图标中心（按给定半径） */
    public static void centerOf(android.content.Context c, int side, int i, int n, float r, float[] out) {
        double a = Math.toRadians((double) ang(i, n));
        float dx = (float) (Math.cos(a) * (double) r);
        float dy = (float) (Math.sin(a) * (double) r);
        out[0] = (side == 1) ? (cxOf(c, side) + dx) : (cxOf(c, side) - dx);
        out[1] = cyOf(c) - dy;
    }

    private float rDraw() {
        return this.rMin + ((this.rMax - this.rMin) * this.p);
    }

    /**
     * 3419：**图标尺寸随半径缩放的形状下限**。
     *
     * 之前 icon 尺寸 = `iconPx × (r/rMax)`，而 rMin 只有 rMax 的 11%，再叠上冒出时的
     * BLOOM_FROM(0.55) → 刚冒出时只有原尺寸的 6%，等于一个看不清的小点。
     * 加下限后：从角落往外走时**始终保持可辨认的大小**，只在接近满半径时才继续长大。
     */
    static final float ICON_SCALE_MIN = 0.72f;

    /** 半径 → 图标尺寸倍率（含下限；绘制和命中必须用同一个值） */
    static float iconScaleOf(float r, float rMax) {
        float s = (rMax > 0.0f) ? (r / rMax) : 1.0f;
        if (s < ICON_SCALE_MIN) {
            s = ICON_SCALE_MIN;
        }
        if (s > 1.0f) {
            s = 1.0f;
        }
        return s;
    }

    @Override
    protected void onDraw(android.graphics.Canvas cv) {
        drawFan(cv, getContext(), this.side, this.pkgs, this.rMin, this.rMax, this.iconPx, this.p, this.sel,
                this.iconScale, this.gscale, this.iconAlpha, this.dim, this.rMul, this.iconR);
    }

    /** 兼容旧调用（预览用） */
    public static void drawFan(android.graphics.Canvas cv, android.content.Context ctx,
                               int side, String[] ps,
                               float rMin, float rMax, float iconPx, float p, int sel) {
        drawFan(cv, ctx, side, ps, rMin, rMax, iconPx, p, sel, null, 1.0f);
    }

    /**
     * ★ 2779：真菜单和设置页预览（[FlyPrev]）共用的绘制入口。
     *
     * @param p   0~1 展开进度（预览固定传 1.0）
     * @param sel 选中下标，-1 = 无
     */
    public static void drawFan(android.graphics.Canvas cv, android.content.Context ctx,
                               int side, String[] ps,
                               float rMin, float rMax, float iconPx, float p, int sel,
                               float[] scales, float gscale) {
        drawFan(cv, ctx, side, ps, rMin, rMax, iconPx, p, sel, scales, gscale, null, 1.0f, 1.0f, null);
    }

    /**
     * 3418：带动效参数的完整版（真菜单用）。
     *
     * @param alphas 每个图标的不透明度（null = 全 1）
     * @param dim    背景压暗 0~1（0 = 全透明，1 = 25% 黑）
     * @param rMul   半径倍率（收起时 <1，扇形缩回角落）
     * @param rs     3419：每颗图标各自的半径（null = 统一圆弧，预览用）
     */
    public static void drawFan(android.graphics.Canvas cv, android.content.Context ctx,
                               int side, String[] ps,
                               float rMin, float rMax, float iconPx, float p, int sel,
                               float[] scales, float gscale, float[] alphas, float dim, float rMul,
                               float[] rs) {
        if ((ps == null) || (ps.length <= 0) || (p <= 0.02f)) {
            return;
        }
        ensurePaints();
        // 3418：背景压暗由我们自绘（原来靠 WMS 的 FLAG_DIM_BEHIND，只能一步到位，没法淡入）
        if (dim > 0.001f) {
            int a = (int) (Math.min(1.0f, dim) * 0.25f * 255.0f);
            cv.drawColor((a << 24) | 0x000000);
        }
        int n = ps.length;
        float rShared = (rMin + ((rMax - rMin) * p)) * rMul;
        // 3422：**位图缓存用固定基准尺寸**（每帧不变）。
        //   以前缓存键里带 `(int) half`，而 half 在冒出动画里每帧都在变（90→70→…），
        //   于是每帧都给每颗图标新建一张 bitmap（circleFitIcon 内部还要全图扫像素）
        //   → 动画期间疯狂分配 + GC，这就是"帧率很低"的真因。
        //   现在按 iconPx 算一次基准尺寸，绘制时由 Canvas 缩放到实际 half。
        int basePx = Math.max(24, ((((int) iconPx) + 15) / 16) * 16);
        float[] pt = new float[2];
        for (int i = 0; i < n; i++) {
            float ia = ((alphas != null) && (i < alphas.length)) ? alphas[i] : 1.0f;
            if (ia <= 0.004f) {
                continue;                       // 完全透明 → 连图都不必画
            }
            // 3419：每颗用自己的半径 —— 这是"一颗一颗从角落冒出来"的关键
            float r = ((rs != null) && (i < rs.length)) ? (rs[i] * rMul) : rShared;
            if (r <= 0.5f) {
                continue;
            }
            float scale = iconScaleOf(r, rMax);
            float isz = iconPx * scale;
            centerOf(ctx, side, i, n, r, pt);
            float cell = isz / 2.0f;
            boolean on = (i == sel);
            android.graphics.drawable.Drawable d = iconOf(ctx, ps[i], side);
            float m = ((scales != null) && (i < scales.length)) ? scales[i] : (on ? 1.28f : 1.0f);
            float half = cell * m * gscale;
            if (d == null) {
                if ("@more".equals(pkgOf(ps[i]))) {
                    // ★ 2783：恢复旧菜单（`Bar.more`）的自绘「三个点」样式 ——
                    //   蓝底圆 + 三个白点；点径 = 直径×0.125（最小 6px），间距 = (直径 - 3×点径)/4。
                    drawMore(cv, pt[0], pt[1], half, ia);
                } else {
                    cv.drawCircle(pt[0], pt[1], half, DOT);
                    TXT.setTextSize(Math.max(20.0f, half * 1.1f));
                    cv.drawText("?", pt[0], pt[1] + (half * 0.38f), TXT);
                }
                continue;
            }
            // ★ 2830：位图缓存键必须用**完整 entry**。以前用 pkgOf(entry)，
            //   所有工具都变成 "tool@114" → 第二个工具开始直接复用第一个工具的图（串图）。
            // ★ 2961：**键里必须带 side** —— 分身角标要跟着呼出侧（左呼出→左下、右呼出→右下），
            //   键里不带 side 时，先渲染过的那一侧会被复用，另一侧永远显示反的（实测：
            //   左边呼出角标仍在右下）。`iconOf` 的 ICONS 缓存早就带 L/R 了，这里是另一层缓存。
            // ★ 3050fix：只有「分身条目」（pkg#Activity#userId）的 drawable 是提前合成好的
            //   「圆图 + 角标」位图；普通应用、侧边栏工具都必须过 circleFitIcon 裁圆。
            boolean preComposed = userOf(ps[i]) > 0;
            drawCircularIcon(cv, d, pt[0], pt[1], half, ps[i] + "@" + basePx + "@" + side,
                    basePx, preComposed, ia);
        }
    }

    /** 「更多应用」：蓝底圆 + 三个白点（旧菜单 Bar.more 的样子） */
    private static void drawMore(android.graphics.Canvas cv, float cx, float cy, float half, float alpha) {
        float d = half * 2.0f;
        float dot = d * 0.125f;
        if (dot < 6.0f) {
            dot = 6.0f;
        }
        float gap = (d - (3.0f * dot)) / 4.0f;
        if (gap < 4.0f) {
            gap = 4.0f;
        }
        int oldBg = MORE_BG.getAlpha();
        int oldDot = MORE_DOT.getAlpha();
        MORE_BG.setAlpha((int) (oldBg * alpha));
        MORE_DOT.setAlpha((int) (oldDot * alpha));
        cv.drawCircle(cx, cy, half, MORE_BG);
        float total = (3.0f * dot) + (2.0f * gap);
        float x = cx - (total / 2.0f) + (dot / 2.0f);
        for (int i = 0; i < 3; i++) {
            cv.drawCircle(x, cy, dot / 2.0f, MORE_DOT);
            x += dot + gap;
        }
        MORE_BG.setAlpha(oldBg);
        MORE_DOT.setAlpha(oldDot);
    }

    /**
     * 圆形图标：复用 `Sug.circleFitIcon`（旧菜单同款裁切）。
     *
     * @param preComposed true = 这个 drawable **已经是**「圆形底图 + 角标」合成好的位图（只可能是分身条目），
     *                    直接缩放画、**不能**再裁圆（否则角标会被裁掉）。
     *
     * ★ 3050fix（重要）：以前这里判的是「key 里含 '#'」—— 但**普通应用条目也是 `pkg#Activity`**
     *   （实测 pins：`com.tencent.mm#com.tencent.mm.ui.LauncherUI`），侧边栏工具是 `tool#xxx`，
     *   于是只要 drawable 恰好是 BitmapDrawable（**用第三方图标包 / 老式 PNG 图标时就是这样**）
     *   就走进这条快路，把整块原图直接缩放画出来 → **不裁圆**。
     *   这就是「工具图标不是圆形」和「第三方图标下 App 图标不是圆形」的共同真因。
     */
    private static void drawCircularIcon(android.graphics.Canvas cv,
                                         android.graphics.drawable.Drawable d,
                                         float cx, float cy, float half, String key,
                                         int size, boolean preComposed, float alpha) {
        // 3422：`size` 由调用方给**固定基准尺寸**（按 iconPx 算，每帧不变），
        //   绘制时由 Canvas 缩放到实际 half。以前 key/size 都带 `(int) half`，
        //   half 在冒出动画里每帧在变 → 每帧新建 bitmap + circleFitIcon 全图扫像素，
        //   动画期间疯狂分配/GC，就是"帧率很低"的真因。
        android.graphics.Bitmap bmp = BMP.get(key);
        // 2919fix2：分身条目（key 带 '＃<用户号>'）：iconOf 已经合成好
        //   「圆形图标 + 裁切角标」的位图，这里**直接缩放画**，
        //   不能再过 circleFitIcon（它会按不透明包围盒重新裁切/缩放，角标会被裁掉）。
        if ((bmp == null) && preComposed && (d instanceof android.graphics.drawable.BitmapDrawable)) {
            try {
                var src = ((android.graphics.drawable.BitmapDrawable) d).getBitmap();
                if (src != null) {
                    bmp = android.graphics.Bitmap.createScaledBitmap(src, size, size, true);
                    BMP.put(key, bmp);
                }
            } catch (Throwable t) {
                com.oupao.wiz.SLog.e("FlyFan: 分身图标缩放异常", t);
            }
        }
        if (bmp == null) {
            bmp = com.oupao.wiz.Sug.circleFitIcon(d, size);
            if (bmp == null) {
                // 3419：circleFitIcon 失败时**必须自己裁圆**再画。
                //   以前这里直接 `d.draw(cv)` —— 画出来是方块，小尺寸时一眼就能看出来
                //   （用户截图：混在圆形图标里的几颗"方角"图标）。
                bmp = circleClip(d, size);
            }
            if (bmp != null) {
                BMP.put(key, bmp);
                if (BMP.size() > 160) {
                    BMP.clear();      // 上限保护：尺寸档位 + side + 图标数，正常远达不到
                }
            }
        }
        if (bmp == null) {
            return;                   // 实在画不出来就跳过，也不要画成方块
        }
        TMP.set(cx - half, cy - half, cx + half, cy + half);
        // 3418：逐图标 alpha（呼出 bloom / 收起淡出）—— 走同一个 SHADER，画完复位
        int old = SHADER.getAlpha();
        SHADER.setAlpha((int) (old * alpha));
        cv.drawBitmap(bmp, null, TMP, SHADER);
        SHADER.setAlpha(old);
    }

    /** 兜底裁圆：把 drawable 铺进方形再按圆形裁切（保证任何图标都不会画成方块） */
    private static android.graphics.Bitmap circleClip(android.graphics.drawable.Drawable d, int size) {
        try {
            if ((d == null) || (size <= 0)) {
                return null;
            }
            var out = android.graphics.Bitmap.createBitmap(size, size,
                    android.graphics.Bitmap.Config.ARGB_8888);
            var c = new android.graphics.Canvas(out);
            var path = new android.graphics.Path();
            float r = ((float) size) / 2.0f;
            path.addCircle(r, r, r, android.graphics.Path.Direction.CW);
            c.clipPath(path);
            d.setBounds(0, 0, size, size);
            d.draw(c);
            return (com.oupao.wiz.Sug.opaque(out) < 2) ? null : out;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 手指位置 → 选中下标。**必须真的划到图标上**（圆心距 < 图标半径×0.85），
     * 否则返回 -1（只是"悬空"）。
     *
     * 3419：**还没冒出来的图标不参与命中** —— 否则会出现"划到一片空白却选中了某个
     * 透明图标"的诡异手感（用户看不到它，松手却启动了）。
     */
    public int hit(float x, float y) {
        String[] ps = this.pkgs;
        if ((ps == null) || (ps.length <= 0)) {
            return -1;
        }
        int n = ps.length;
        float rShared = rDraw();
        float[] pt = new float[2];
        android.content.Context c = getContext();
        int best = -1;
        float bestD = Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            // 3421：几乎还没出现（<12%）的不参与命中；门槛不能高，
            //   否则"快速一划就松手"时图标还在渐显途中，划到了却选不中（松手没反应）。
            if ((this.iconAlpha != null) && (i < this.iconAlpha.length)
                    && (this.iconAlpha[i] < 0.12f)) {
                continue;
            }
            // 3419：命中位置必须和**画出来的位置**一致 —— 每颗有自己的半径
            float r = ((this.iconR != null) && (i < this.iconR.length)) ? this.iconR[i] : rShared;
            if (r <= 0.5f) {
                continue;
            }
            float scale = iconScaleOf(r, rMax);
            float limit = Math.max(this.iconPx * scale * 0.85f, 56.0f);
            float limit2 = limit * limit;
            centerOf(c, this.side, i, n, r, pt);
            float dx = x - pt[0];
            float dy = y - pt[1];
            float d2 = (dx * dx) + (dy * dy);
            if ((d2 < limit2) && (d2 < bestD)) {
                bestD = d2;
                best = i;
            }
        }
        return best;
    }

    /** 2776：停留等选择时，窗口可触摸 → 自己接一下点击（点到图标启动，点别处收起） */
    /** 2794：停留时的按下起点，用来识别"上滑手势" */
    private float tDownX;
    private float tDownY;
    /** 3462：CANCEL 前最后一次 MOVE 的位置（判断被系统手势接管时手指往哪走了） */
    private float tLastX;
    private float tLastY;

    @Override
    public boolean onTouchEvent(android.view.MotionEvent e) {
        if (!com.oupao.wiz.FlyFanH.staying()) {
            return false;
        }
        int a = e.getActionMasked();
        if (a == android.view.MotionEvent.ACTION_DOWN) {
            this.tDownX = e.getX();
            this.tDownY = e.getY();
            this.tLastX = this.tDownX;
            this.tLastY = this.tDownY;
            return true;
        }
        if (a == android.view.MotionEvent.ACTION_MOVE) {
            this.tLastX = e.getX();
            this.tLastY = e.getY();
            return true;
        }
        if (a == android.view.MotionEvent.ACTION_UP) {
            float up = this.tDownY - e.getY();
            float side = Math.abs(e.getX() - this.tDownX);
            // 上滑（竖直为主、位移够大）→ 只收菜单，不当作点击
            if ((up >= 60.0f) && (up > (side * 1.5f))) {
                com.oupao.wiz.SLog.i("FlyFan: 上滑 → 收起停留中的菜单");
                com.oupao.wiz.FlyFanH.hide();
                return true;
            }
            com.oupao.wiz.FlyFanH.tap(e.getX(), e.getY());
            return true;
        }
        if (a == android.view.MotionEvent.ACTION_CANCEL) {
            // 3462：**系统手势接管**（桌面底部上滑 = 回桌面/多任务）。
            //   桌面上这条手势会被桌面自己的 InputConsumer 抢走，我们只收到 CANCEL，
            //   拿不到 UP —— 以前这里直接 return，菜单就一直等那个慢半拍的标志位
            //   （用户："桌面呼出菜单停留再呼出多任务，要等几秒才关；全屏 App 里是秒关"）。
            //   现在：CANCEL 前手指确实往上走了 → 立刻收。
            float up = this.tDownY - this.tLastY;
            float side = Math.abs(this.tLastX - this.tDownX);
            if ((up >= 40.0f) && (up > (side * 1.2f))) {
                com.oupao.wiz.SLog.i("FlyFan: 系统手势接管(CANCEL 上滑 " + ((int) up) + "px) → 收起");
                com.oupao.wiz.FlyFanH.hide();
            }
            return true;
        }
        return true;
    }

    public static String pkgOf(String entry) {
        if (entry == null) {
            return null;
        }
        int i = entry.indexOf('#');
        return (i < 0) ? entry : entry.substring(0, i);
    }

    private static android.graphics.drawable.Drawable iconOf(android.content.Context ctx, String entry, int side) {
        if (entry == null) {
            return null;
        }
        // ★ 2830：侧边栏工具（tool#xxx）必须在**切 # 之前**认出来。
        //   以前先 pkgOf() 把 "tool#screen_shot" 切成 "tool"，于是下面
        //   `pkg.startsWith("tool#")` 永远不成立 → 落到 getApplicationIcon("tool") 失败 →
        //   菜单里这一格画成「?」/圆点。这就是「扇形菜单里的工具图标不显示」的真因。
        //   整串交给 Tools.drawable（它内部 aliasOf() 会去掉 "tool#" 前缀去 smartsidebar 资源里找）。
        if (entry.startsWith("tool#")) {
            android.graphics.drawable.Drawable td = ICONS.get(entry);
            if (td != null) {
                return td;
            }
            try {
                td = com.oupao.wiz.Tools.drawable(ctx, entry);
            } catch (Throwable t) {
                td = null;
            }
            if (td != null) {
                ICONS.put(entry, td);
            }
            com.oupao.wiz.SLog.i("FlyFan: 工具图标 " + entry + " -> " + (td != null));
            return td;
        }
        String pkg = pkgOf(entry);
        if (pkg == null || "@more".equals(pkg)) {
            return null;
        }
        // 2919：分身条目 = `pkg#Activity#userId`。
        //   和旧菜单（`Bar.badgeClone` / 桌面 / 引索条）一样：
        //   用该用户下的**系统 badged icon**（里面自带系统画的分身角标）。
        int entryUser = userOf(entry);
        // 分身的角标要跟着菜单出现的边走：左下角出菜单 → 角标在图标**左下角**；
        // 右下角出菜单 → 右下角。（旧菜单 Bar.badgeClone 就是 `left = (FlyH.side == 1)`）
        boolean badgeLeft = (side == 1);
        String cacheKey = (entryUser > 0) ? (pkg + "#" + entryUser + (badgeLeft ? "L" : "R")) : pkg;
        android.graphics.drawable.Drawable d = ICONS.get(cacheKey);
        if (d != null) {
            return d;
        }
        if (entryUser > 0) {
            // 3416：分身角标直接取系统原生 drawable（不再裁切合成图标）。
            int sz = 336;
            try {
                var bmp = com.oupao.wiz.Sug.cloneIconCircle(pkg, entryUser, sz, badgeLeft);
                if (bmp != null) {
                    d = new android.graphics.drawable.BitmapDrawable(ctx.getResources(), bmp);
                    ICONS.put(cacheKey, d);
                    com.oupao.wiz.SLog.i("FlyFan: 分身图标 pkg=" + pkg + " user=" + entryUser
                            + " left=" + badgeLeft + " ok=true");
                    return d;
                }
                com.oupao.wiz.SLog.i("FlyFan: 分身图标合成失败 pkg=" + pkg + " user=" + entryUser);
            } catch (Throwable t) {
                com.oupao.wiz.SLog.e("FlyFan: 分身角标异常", t);
            }
        }
        try {
            d = ctx.getPackageManager().getApplicationIcon(pkg);
            ICONS.put(cacheKey, d);
            return d;
        } catch (Throwable t) {
            return null;
        }
    }

    /** entry 里的分身用户号：`pkg#Activity#998` → 998；没有第三段就是 0（主用户）。 */
    private static int userOf(String entry) {
        try {
            if (entry == null) {
                return 0;
            }
            String[] segs = entry.split("#");
            if (segs.length < 3) {
                return 0;
            }
            return Integer.parseInt(segs[2].trim());
        } catch (Throwable t) {
            return 0;
        }
    }

    /** 取指定用户下的系统带角标图标（LauncherApps.getBadgedIcon，含分身角标）。 */
    private static android.graphics.drawable.Drawable badgedIcon(android.content.Context ctx, String pkg, int userId) {
        try {
            var la = (android.content.pm.LauncherApps) ctx.getSystemService("launcherapps");
            if (la == null) {
                return null;
            }
            var acts = la.getActivityList(pkg, android.os.UserHandle.getUserHandleForUid(userId * 100000));
            if ((acts == null) || (acts.isEmpty())) {
                return null;
            }
            int dens = ctx.getResources().getDisplayMetrics().densityDpi;
            var bd = acts.get(0).getBadgedIcon(dens);
            com.oupao.wiz.SLog.i("FlyFan: 分身图标 pkg=" + pkg + " user=" + userId + " ok=" + (bd != null));
            return bd;
        } catch (Throwable t) {
            com.oupao.wiz.SLog.e("FlyFan: 分身图标异常", t);
            return null;
        }
    }
}
