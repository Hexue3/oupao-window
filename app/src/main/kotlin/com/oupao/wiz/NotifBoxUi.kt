package com.oupao.wiz

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

/**
 * 「通知收纳」的界面部分（SystemUI 进程内，只做两件事：头部一个入口按钮 + 长按菜单加一项）。
 *
 * ## 用户定稿的交互
 * - **不另造面板**：点收纳箱入口后，直接把「未收纳的通知」过滤掉、只显示「收纳中的通知」，
 *   用的还是系统原生通知卡片（同一条渲染流水线，只是换了一套过滤规则）。
 * - **再点一次**回到默认（显示未收纳的、不显示收纳的）。
 * - 长按菜单里多一项「收纳 / 移出收纳」。
 *
 * ## 入口按钮
 * 宿主是通知中心头部「时钟 / 日期 / 铃铛」那一行 —— `id/settings_button_container`（ConstraintLayout，
 * 36dp，只有 `qs_footer_clock` / `oplus_date` / `settings_button` 三个孩子），
 * 由 `com.oplus.systemui.separate.OplusQSSimpleHeader#onInit()` find 完子 View 之后注入。
 *
 * ## 出现/消失动画（2026-11-07 05:1x 定稿，依据 `notifbox/FINDINGS-headeranim.md`）
 * - `OplusQSSimpleHeader` 自己不跑任何动画，整行只做 translationY（我们的 View 自动跟随）；
 * - 邻居的出现/消失是 `OplusSimpleQSManagerImpl$showOrHideView$1/$2/$3$1` 每帧对
 *   clock / date / **settingsButton** 逐个 `setTransitionAlpha(弹簧进度)`；
 * - f 撞到 0 / 1 时 `OplusSimpleQSFakeController#resetAllViewsTransitionAlpha` 会把那 5 个真子 View
 *   **硬设** transitionAlpha —— 我们的 View 不在名单里，这就是"时机对不上"的根因。
 * - 对策：不自己算时机，**直接同步铃铛的 `transitionAlpha`**（必须用 transitionAlpha，用 alpha 会与它相乘更暗），
 *   并在 `resetAllViewsTransitionAlpha` 之后补一次（[hookFakeAlpha]）。
 */
object NotifBoxUi {

    private const val TAG = "NBX"

    // ---- 头部行里的 id（先按名字解析，解析不到再用兜底数值）----
    private const val N_ROW = "settings_button_container"
    private const val N_BELL = "settings_button"
    private const val N_DATE = "oplus_date"
    private const val ID_ROW = 0x7f0a0c9e
    private const val ID_BELL = 0x7f0a0c9d
    private const val ID_DATE = 0x7f0a0990

    /** 我们自己的 View id（稳定即可） */
    private const val ID_MINE = 0x0f0b0001

    private const val PKG_SELF = "com.android.systemui"
    /** 3086：TrayDrawable 的字形占 `s` 的比例（托盘 x∈±0.30、箭头 y∈−0.30~+0.28 → 约 0.6） */
    private const val TRAY_CONTENT = 0.6f

    /**
     * 3090：字形占"框"的比例，**两个面板统一 0.82**。
     *
     * 稳态截图逐像素量：
     * | 面板 | 邻居框 | 邻居字形 | 字形/框 |
     * |---|---|---|---|
     * | 合并(经典) 齿轮 | 126 | 70×74 | 0.556 |
     * | 分离 铃铛 | 105 | 53×51 | 0.505 |
     * 我们的可见宽 = 0.651×s（0.6 字形 + 0.051 笔画）→ glyph = 0.53/0.651 ≈ 0.82。
     * 3089 试过"从邻居 ImageView 推目标"，实测那是含留白的框，会推成 84/83（真值 70/53），偏大。
     */
    private const val TRAY_GLYPH = 0.82f

    /**
     * 3091：入口图标的目标可见宽度（dp）。
     * 实测：合并面板齿轮 70×75px、分离面板铃铛+齿轮 72×74px —— **两个面板邻居几乎一样大**。
     * 本机 density≈2.75 → 25.5dp ≈ 70px。
     */
    private const val ICON_DP = 19f
    private const val PKG_MENU = "com.oplus.systemui.statusbar.notification.row.menupanel.popupmenu."

    @Volatile
    private var headerRow: ViewGroup? = null

    /**
     * ★ 3083：**按实例配对**记录「我们插进去的按钮 → 它旁边的邻居（铃铛/齿轮）」。
     *
     * 为什么必须成对：经典模式走 `OplusQSFooterImpl`（bell=FrameLayout），
     * 分体式走 `OplusQSSimpleHeader`（bell=AlphaOptimizedImageView），**两套 header 各插一个**。
     * 以前只保存"最后一个"的 entryView/bellView，于是同步时把 A 的位置/尺寸同步给了 B
     * → 用户看到"每次位置都不一样"。
     */
    private val entryPairs = java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<ImageView, View?>()
    )

    /** 屏幕坐标复用数组（每帧要用，别新建） */
    private val locA = IntArray(2)
    private val locB = IntArray(2)

    /** 入口跟随日志的时间戳（2s 一条） */
    private var shapeFixAt = 0L

    /** 入口跟随的日志开关（排障用，量很小） */
    private val dbgPos = true

    @Volatile
    private var lastF = 1f

    /** 3099：面板是否正在收起（展开进度在减小）—— 用来让图标"一收就走" */
    @Volatile
    private var closingNow = false

    /** 3102：上一次的显隐判定（滞回：>0.08 显示、<0.03 隐藏，中间保持） */
    @Volatile
    private var panelShown = false

    @Volatile
    private var prevF = 1f

    @Volatile
    private var syncAt = 0L

    @Volatile
    private var fakeAlphaHooked = false

    // ---------------- 安装入口 ----------------

    fun hookHeader(cl: ClassLoader?): Int {
        hookFakeAlpha(cl)
        var n = hookPanelSignal(cl)
        // 分体式（separate 通知中心）：时钟 + 日期 + 铃铛同一行
        val h = runCatching { Rt.cls(cl, "com.oplus.systemui.separate.OplusQSSimpleHeader") }.getOrNull()
        if (h != null) {
            for (name in arrayOf("onInit", "onFinishInflate")) {
                val m = runCatching { Rt.methodByName(h, name, 0) }.getOrNull() ?: continue
                runCatching { m.isAccessible = true }
                val cb = object : XMH() {
                    override fun afterHookedMethod(param: XMP) {
                        injectRow(param.thisObject as? ViewGroup, "header." + name)
                    }
                }
                if (XBridge.hookMethod(m, cb) != null) n++
            }
        }
        // 一体式兜底：QS footer 里也有同名的 settings_button_container
        val f = runCatching { Rt.cls(cl, "com.oplus.systemui.qs.OplusQSFooterImpl") }.getOrNull()
        if (f != null) {
            val m = runCatching { Rt.methodByName(f, "onFinishInflate", 0) }.getOrNull()
            if (m != null) {
                runCatching { m.isAccessible = true }
                val cb = object : XMH() {
                    override fun afterHookedMethod(param: XMP) {
                        injectRow(param.thisObject as? ViewGroup, "footer")
                    }
                }
                if (XBridge.hookMethod(m, cb) != null) n++
            }
        }
        if (n == 0) SLog.i("$TAG: 头部入口没挂上（找不到 OplusQSSimpleHeader/OplusQSFooterImpl）")
        return n
    }

    /**
     * 面板展开/收起信号（两个 hook 互为兜底）：
     * 1. `ShadeExpansionStateManager#onPanelExpansionChanged(F Z Z String PanelType)` —— 每帧触发；
     *    fraction < 0.99 就退出「收纳箱浏览模式」（先不重建），== 0 时补上重建。
     * 2. `NotificationPanelViewController#setExpandedHeight(float)` —— 兜底收起信号。
     */
    private fun hookPanelSignal(cl: ClassLoader?): Int {
        var n = 0
        val sem = runCatching { Rt.cls(cl, "com.android.systemui.shade.ShadeExpansionStateManager") }.getOrNull()
        val m1 = sem?.let { runCatching { Rt.methodByName(it, "onPanelExpansionChanged", 5) }.getOrNull() }
        if (m1 != null) {
            runCatching { m1.isAccessible = true }
            val cb = object : XMH() {
                override fun afterHookedMethod(param: XMP) {
                    val f = param.args.getOrNull(0) as? Float ?: return
                    // 一上滑（还没完全收起）就退出浏览模式，但先不重建 —— 让收纳列表跟着面板一起滑走
                    if (f < 0.99f) NotifBox.exitBoxMode("collapsing", rebuildNow = false)
                    // 完全收起（此时列表已经不可见）再把重建补上，下次拉开就是默认视图
                    if (f <= 0.01f) NotifBox.flushBoxExit()
                    fadeEntry(f)
                    syncEntry()
                }
            }
            if (XBridge.hookMethod(m1, cb) != null) n++
        }

        val k = runCatching { Rt.cls(cl, "com.android.systemui.shade.NotificationPanelViewController") }.getOrNull()
        val m2 = k?.let { runCatching { Rt.methodByName(it, "setExpandedHeight", 1) }.getOrNull() }
        if (m2 != null) {
            runCatching { m2.isAccessible = true }
            val cb = object : XMH() {
                override fun afterHookedMethod(param: XMP) {
                    val h = param.args.getOrNull(0) as? Float ?: 0f
                    if (h <= 1f) {
                        NotifBox.exitBoxMode("panel-collapsed", rebuildNow = false)
                        NotifBox.flushBoxExit()
                        fadeEntry(0f)
                    }
                    syncEntry()
                }
            }
            if (XBridge.hookMethod(m2, cb) != null) n++
        }
        return n
    }

    /** 每帧同步一次（入口图标跟铃铛一模一样地出现/消失） */
    private fun fadeEntry(f: Float) {
        // ★ 3099：记录"是不是正在收起"。上滑关通知中心时，我们的图标必须**立刻**消失，
        //   不能等面板完全关掉（用户实测：会残留到关完）。展开方向不受影响。
        closingNow = f < prevF - 0.0005f
        prevF = f
        lastF = f
        syncWithBell()
    }

    /** 把铃铛当前的可见性 / 过渡透明度 / 位置 / 尺寸原样同步给我们的图标 */
    private fun syncWithBell() {
        val on = runCatching { Cfg.notifBox() }.getOrDefault(false)
        synchronized(entryPairs) {
            val it = entryPairs.entries.iterator()
            while (it.hasNext()) {
                val e = it.next()
                val v = e.key ?: continue
                val b = e.value
                if (!v.isAttachedToWindow) {
                    it.remove()
                    continue
                }
                if (!on) {
                    if (v.visibility != View.GONE) v.visibility = View.GONE
                    continue
                }
                // ★ 3095：**可见性 / 透明度全部镜像邻居**，不再自己"强制 VISIBLE + alpha=1"。
                //   用户报："控制中心左滑切到通知中心时，图标要等切换完才出现"——
                //   原因是同步里把 alpha 硬设成 1、又强制 VISIBLE，而切换动画是**交叉淡入淡出**：
                //   这时另一半 header 的那个图标反而一直亮着（截图里那个"飘在中间的重影"就是它），
                //   而真正该显示的那一半要等 lastF≥0.99（syncEntry）才被设为 VISIBLE。
                if (b != null) {
                    // ★★ 3103（用户点名：3100 + 3101 合起来）：
                    //   **可见性用 3101 的做法**（保持 VISIBLE，只在"完全收起且邻居不可见"时 GONE）
                    //     → 好处：切换过渡用快照渲染 header 时，我们一定在快照里，不会"要等停稳才出现"。
                    //   **透明度用 3100 的做法**（逐帧同步邻居的 alpha / transitionAlpha）
                    //     → 好处：淡入淡出、收起消失的节奏和铃铛完全一致，不会残留、也不会自己判趋势导致闪。
                    if (lastF <= 0.001f && !b.isShown) {
                        if (v.visibility != View.GONE) v.visibility = View.GONE
                    } else {
                        if (v.visibility != View.VISIBLE) v.visibility = View.VISIBLE
                        // ★ 3105：阈值从 0.5 收到 **0.98**。
                        //   0.5 太宽：上下滑收起时 lastF 从 1 往 0 掉，1→0.5 这段我们还固定 alpha=1
                        //   → 面板都快没了图标还留着（用户报"上下滑不立刻消失/显示"）。
                        //   只要 `lastF ≥ 0.98`（真正"完全拉开"，横滑切换期间也一直保持 1）才固定 1；
                        //   一旦开始收起（<0.98）立刻回到"同步邻居"，和铃铛同帧淡出。
                        if (lastF >= 0.98f) {
                            if (v.alpha != 1f) v.alpha = 1f
                            if (v.transitionAlpha != 1f) v.transitionAlpha = 1f
                        } else {
                            val ba = b.alpha
                            if (v.alpha != ba) v.alpha = ba
                            val bta = b.transitionAlpha
                            if (v.transitionAlpha != bta) v.transitionAlpha = bta
                        }
                        followBellPosition(v, b)
                    }
                } else {
                    if (v.visibility != View.VISIBLE) v.visibility = View.VISIBLE
                    if (v.alpha != 1f) v.alpha = 1f
                }
            }
        }
    }

    /**
     * ★ 3083：把入口图标摆到"邻居左边缘 − 我们的宽度 − 间距"的位置（**绝对赋值**，不做累加）。
     *
     * 3082 用 `translationX += dx` 是错的：读数来自上一帧的布局，累加会**自激振荡**，
     * 表现为用户看到的"每次位置都不一样"。现在改成：
     *   目标位移 = (邻居屏幕左边缘 − 我们的宽度 − 间距) − 我们"没有位移时"的屏幕左边缘
     * 已经对时就恒等于当前值，不动；不对时一帧就到位。
     *
     * 另外经典模式与分体式是**两套 header**（经典的是 `OplusQSFooterImpl`，分体式是 `OplusQSSimpleHeader`），
     * 各自都插了一个按钮 —— 所以同步必须**按实例配对**（见 [entryPairs]），
     * 以前只记最后一个，等于把 A 的位置同步给了 B。
     */
    private fun followBellPosition(v: View, b: View) {
        runCatching {
            if (v.width <= 0 || v.height <= 0) return@runCatching
            if (!v.isShown || !b.isShown) return@runCatching
            // ★ 3091：**目标可见宽按物理尺寸定**（≈25.5dp ≈ 70px），不再按"框的比例"。
            //   实测两个面板的邻居图标几乎一样大（合并齿轮 70×75、分离铃铛+齿轮 72×74），
            //   但邻居的**框**不一样（126 vs 105）→ 按框比例算，分离面板必然偏小（实测 57 vs 72）。
            val dm = v.resources.displayMetrics
            val targetPx = ICON_DP * dm.density
            val needS = targetPx / TRAY_K               // 需要的 s（可见宽 = K × s）
            val needBox = (needS + 2f).toInt()
            // 1) 尺寸：至少 needBox（否则字形会被 View 边界裁掉）；够大就跟邻居一致
            val wantW = maxOf(b.width, needBox)
            val wantH = maxOf(b.height, needBox)
            if (kotlin.math.abs(v.layoutParams.width - wantW) > 2 ||
                kotlin.math.abs(v.layoutParams.height - wantH) > 2
            ) {
                v.layoutParams.width = wantW
                v.layoutParams.height = wantH
                v.requestLayout()
            }
            b.getLocationOnScreen(locB)
            v.getLocationOnScreen(locA)
            // ★ 3098：如果我们是邻居的**兄弟**（同一个父容器），位置交给布局/容器平移自己处理，
            //   绝不能再用屏幕坐标补 translation —— 那样在横滑切换时会有 1 帧滞后，
            //   表现就是"图标随着滑动微微向左平移"。
            // ★ 3328：但**布局不会跟着 translation 走**。实测合并模式展开时 ROM 给铃铛设了
            //   `translationX=-168`（整排左移让位），我们作为兄弟仍留在 layout 位置 →
            //   正好压到设置齿轮上（用户截图：齿轮+托盘箭头重叠，日志 `bellTx=-168 tx=0`）。
            //   所以同父容器时也要同步 translation——直接赋值同一个数，在 onPreDraw 里做，
            //   与 ROM 的动画器同帧生效，不会出现 3098 那种"按屏幕坐标反算"的滞后。
            if (v.parent === b.parent) {
                if (v.translationX != b.translationX) v.translationX = b.translationX
                if (v.translationY != b.translationY) v.translationY = b.translationY
            } else {
                val gap = (b.width * 0.12f).toInt().coerceAtLeast(2)
                val vLeftNoTrans = locA[0] - v.translationX.toInt()
                val want = locB[0] - v.width - gap
                val tx = (want - vLeftNoTrans).toFloat()
                if (kotlin.math.abs(v.translationX - tx) > 1f) v.translationX = tx
                // 竖向按"中心对齐"
                val vTopNoTrans = locA[1] - v.translationY.toInt()
                val ty = locB[1] + b.height / 2f - v.height / 2f - vTopNoTrans
                if (kotlin.math.abs(v.translationY - ty) > 1f) v.translationY = ty
            }
            // 3) ★ 3093：直接把"可见宽"以像素交给 drawable（View 大小不再影响字形大小）
            val td = (v as? android.widget.ImageView)?.drawable as? TrayDrawable
            if (td != null) {
                if (kotlin.math.abs(td.pxContent - targetPx) > 0.5f) {
                    td.pxContent = targetPx
                    if (dbgPos) SLog.i(
                        "$TAG: 字形对齐 目标宽=" + targetPx.toInt() + " 框=" + v.width +
                            "x" + v.height + " (邻居框=" + b.width + ")"
                    )
                }
            }
            if (dbgPos && android.os.SystemClock.elapsedRealtime() - shapeFixAt > 2000L) {
                shapeFixAt = android.os.SystemClock.elapsedRealtime()
                SLog.i(
                    "$TAG: 入口跟随 " + v.hashCode().toString(16) +
                        " bellTx=" + b.translationX.toInt() +
                        " tx=" + v.translationX.toInt() + " ty=" + v.translationY.toInt() +
                        " v=" + v.width + "x" + v.height + " bell=" + b.width + "x" + b.height
                )
            }
        }
    }

    /** f==0 / f==1 那两个"硬设 alpha"的帧：ROM 只处理它认识的那 5 个 View，我们在这里补同步 */
    private fun hookFakeAlpha(cl: ClassLoader?) {
        if (fakeAlphaHooked) return
        val c = runCatching { Rt.cls(cl, "com.oplus.systemui.separate.OplusSimpleQSFakeController") }.getOrNull()
        if (c == null) {
            SLog.i("$TAG: 找不到 OplusSimpleQSFakeController（只靠每帧同步 alpha）")
            return
        }
        val m = runCatching { Rt.methodByName(c, "resetAllViewsTransitionAlpha", 1) }.getOrNull()
        if (m == null) {
            SLog.i("$TAG: 找不到 resetAllViewsTransitionAlpha（只靠每帧同步 alpha）")
            return
        }
        runCatching { m.isAccessible = true }
        val cb = object : XMH() {
            override fun afterHookedMethod(param: XMP) {
                runCatching { syncWithBell() }
            }
        }
        if (XBridge.hookMethod(m, cb) != null) {
            fakeAlphaHooked = true
            SLog.i("$TAG: 已挂钩 resetAllViewsTransitionAlpha（图标与邻居同帧同步）")
        }
    }

    /**
     * 3085：量出"邻居图标字形"的真实像素宽（用来让我们的字形跟它一样大）。
     * 邻居在分体式是 `AlphaOptimizedImageView`，经典模式是 `FrameLayout` 里包一个 ImageView，
     * 所以先看自己，再看子树里的第一个有 drawable 的 ImageView。
     * 优先用 drawable 的 bounds（布局后才有），退回 `ImageView 宽 − padding`。
     */
    private fun glyphPxOf(b: View): Int {
        var iv: android.widget.ImageView? = b as? android.widget.ImageView
        if (iv == null && b is ViewGroup) {
            val q = ArrayDeque<View>()
            q.add(b)
            var guard = 0
            while (q.isNotEmpty() && guard++ < 32) {
                val cur = q.removeFirst()
                if (cur is android.widget.ImageView && cur.drawable != null) {
                    iv = cur
                    break
                }
                if (cur is ViewGroup) {
                    for (i in 0 until cur.childCount) cur.getChildAt(i)?.let { q.add(it) }
                }
            }
        }
        val image = iv ?: return -1
        val db = image.drawable?.bounds
        if (db != null && db.width() > 4) return db.width()
        if (image.width > 4) {
            return (image.width - image.paddingLeft - image.paddingRight).coerceAtLeast(1)
        }
        return -1
    }

    /**
     * 3093：`可见宽 / s`（托盘 x∈±0.30s → 0.6s，再加笔画两端）——**按截图实测反推 ≈0.60**。
     *
     * 反推过程（每版都量了 `可见宽` 与 `笔画`，笔画 = 0.051×s 可解出 s）：
     * | 版本 | 可见宽 | 笔画 | s | K |
     * |---|---|---|---|---|
     * | 3088 | 70 | 6 | 118 | 0.59 |
     * | 3090 | 68 | 6 | 118 | 0.58 |
     * | 3091 | 89 | 7 | 137 | 0.65 |
     * | 3092 | 85 | 7 | 137 | 0.62 |
     * 取 **0.61**（四版均值）。这样 `s = 目标可见宽 / 0.60`，画出来就是目标像素宽，View 多大都不影响。
     */
    private const val TRAY_K = 0.61f

    /** 3089：从邻居 View 里找出真正承载图标的 ImageView（经典模式是 FrameLayout 包一个） */
    private fun firstImageView(b: View): android.widget.ImageView? {
        (b as? android.widget.ImageView)?.let { return it }
        if (b is ViewGroup) {
            val q = ArrayDeque<View>()
            q.add(b)
            var guard = 0
            while (q.isNotEmpty() && guard++ < 40) {
                val cur = q.removeFirst()
                if (cur is android.widget.ImageView && cur.drawable != null) return cur
                if (cur is ViewGroup) {
                    for (i in 0 until cur.childCount) cur.getChildAt(i)?.let { q.add(it) }
                }
            }
        }
        return null
    }

    /**
     * 3089：算出"邻居图标可见宽"（px）。优先级：
     * ① ImageView 的内容框（宽 − 左右 padding）—— ROM 图标基本都是画满内容框；
     * ② drawable 的 intrinsic / bounds（取较小，bounds 含内部留白会偏大）；
     * ③ 兜底 0.55 × 框。
     */
    private fun glyphTargetPx(b: View, box: Float): Float {
        val img = firstImageView(b)
        if (img != null) {
            val content = (img.width - img.paddingLeft - img.paddingRight).toFloat()
            if (content > 4f) return content
            val d = img.drawable
            if (d != null) {
                var best = Float.MAX_VALUE
                val bb = d.bounds
                if (bb != null && bb.width() > 4) best = minOf(best, bb.width().toFloat())
                if (d.intrinsicWidth > 4) best = minOf(best, d.intrinsicWidth.toFloat())
                if (best != Float.MAX_VALUE) return best
            }
        }
        if (b.width > 4) return b.width * 0.55f
        return box * 0.55f
    }

    /** 3096：某个 View 的**有效透明度** = 它自己和所有祖先的 alpha × transitionAlpha 连乘 */
    private fun chainAlpha(from: View?): Float {
        var a = 1f
        var cur: View? = from
        var guard = 0
        while (cur != null && guard++ < 16) {
            a *= cur.alpha * cur.transitionAlpha
            cur = cur.parent as? View
        }
        return a.coerceIn(0f, 1f)
    }

    /**
     * 3098：复制邻居的 LayoutParams 当"兄弟"（copy 构造器），这样竖约束/尺寸类型/
     * 甚至 ConstraintLayout 的链式参数都和邻居一致；水平只保留 `endToStart = 邻居 id`。
     */
    private fun siblingLp(bell: View?, host: ViewGroup, w: Int, h: Int): ViewGroup.LayoutParams {
        val blp = bell?.layoutParams
        if (blp != null) {
            runCatching {
                val ctor = blp.javaClass.getConstructor(blp.javaClass)
                val lp = ctor.newInstance(blp) as ViewGroup.LayoutParams
                lp.width = w
                lp.height = h
                if (lp is ViewGroup.MarginLayoutParams) {
                    lp.marginEnd = (2 * host.resources.displayMetrics.density + 0.5f).toInt()
                }
                return lp
            }
        }
        return makeLp(host, w, h)
    }

    /** 入口按钮：可见性跟开关对齐，配色跟「收纳箱浏览模式」对齐（800ms 节流） */
    private fun syncEntry() {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - syncAt < 800L) return
        syncAt = now
        val on = runCatching { Cfg.notifBox() }.getOrDefault(false)
        val mode = NotifBox.boxMode()
        synchronized(entryPairs) {
            for (v in entryPairs.keys) {
                runCatching {
                    if (!v.isAttachedToWindow) return@runCatching
                    // ★ 3095：可见性交给 per-frame 的"镜像邻居"（syncWithBell），这里只管总开关和配色。
                    //   以前这里用 `lastF >= 0.99` 决定可见 → 左滑切换过程中图标是隐藏的，
                    //   要等动画结束才出现（用户报的现象）。
                    if (!on) {
                        if (v.visibility != View.GONE) v.visibility = View.GONE
                        return@runCatching
                    }
                    val d = v.drawable as? TrayDrawable
                    if (d != null && d.active != mode) {
                        d.active = mode
                        v.invalidate()
                    }
                }
            }
        }
    }

    /** 往「时钟 / 日期 / 铃铛」那一行插一个自己的按钮（贴铃铛左边） */
    private fun injectRow(root: ViewGroup?, from: String) {
        if (root == null) return
        runCatching {
            val ctx = root.context
            val row = findById(root, N_ROW, ID_ROW) as? ViewGroup ?: return
            if (row.findViewById<View>(ID_MINE) != null) return   // 幂等：配置变化会重新 inflate

            val bell = findById(row, N_BELL, ID_BELL)
            val date = findById(row, N_DATE, ID_DATE)

            val density = ctx.resources.displayMetrics.density
            val w = (30 * density + 0.5f).toInt()
            val hgt = (36 * density + 0.5f).toInt()

            val btn = ImageView(ctx)
            btn.id = ID_MINE
            btn.scaleType = ImageView.ScaleType.CENTER_INSIDE
            btn.contentDescription = "通知收纳"
            btn.setImageDrawable(
                TrayDrawable(ctx.getColorSafe(0xFFFFFFFF.toInt())).also {
                    it.glyph = TRAY_GLYPH
                    // 3093：一开始就按"像素"定尺寸，第一帧就不会错
                    it.pxContent = ICON_DP * ctx.resources.displayMetrics.density
                }
            )
            btn.setOnClickListener { onEntryClick(btn) }
            // ★★ 3098：**插到邻居（铃铛）的同一个父容器里当兄弟**，不再插到 row 顶层。
            //   原因：通知中心↔控制中心横滑时，两个 header 的容器是**各自平移**的；
            //   我们挂在别的容器里，就只能靠每帧算屏幕坐标去"追"，必然有 1 帧滞后
            //   → 用户看到"图标随着滑动微微向左平移"。做成兄弟后，位置完全由 ConstraintLayout
            //   和 ROM 的容器平移决定，和邻居严格同步，不需要任何补偿。
            val host = (if (bell != null) bell.parent as? ViewGroup else null) ?: row
            val lp = siblingLp(bell, host, w, hgt)
            host.addView(btn, lp)

            if (bell != null) {
                setIntField(btn.layoutParams, "endToStart", bell.id)
                setIntField(btn.layoutParams, "startToStart", -1)
                setIntField(btn.layoutParams, "startToEnd", -1)
                setIntField(btn.layoutParams, "endToEnd", -1)
                setIntField(btn.layoutParams, "leftToLeft", -1)
                setIntField(btn.layoutParams, "rightToRight", -1)
            }
            // ★ 3083：只有当「日期」和「我们」在**同一个父容器**时才改它的约束。
            //   3082 之前无条件改，经典模式下日期不在这个容器里 → 它被拽向时钟（截图里
            //   "20:38:48" 和 "11月7日 周六" 粘在一起就是这个）。
            if (date != null && bell != null && date.parent === row && bell.parent === row) {
                setIntField(date.layoutParams, "endToStart", ID_MINE)
                // 3390：只改 endToStart 时日期会在「父容器左边缘 / 我们按钮左边缘」
                //   之间按默认 bias=0.5 居中（截图里日期跑到中间）。这里强制靠左。
                setFloatField(date.layoutParams, "horizontalBias", 0f)
                date.requestLayout()
            }
            btn.requestLayout()
            row.requestLayout()

            headerRow = row
            // ★ 3083：按实例配对（经典模式 / 分体式各一个）
            entryPairs[btn] = bell
            // ★★ 3084：**每帧跟随**。
            //   挖到 ROM 的机制了：经典模式那排向左让位是 `OplusQSFooterImpl#createAndUpdateExpandAnimators(Z)`
            //   里跑的 **translationX 动画器**（还有 `updateQsMoreButton()` 把「⋮」变出来）。
            //   也就是说：它不发生在"面板展开进度回调"里 —— 点一下自动展开时，
            //   `onPanelExpansionChanged` 早就收完了，动画还在跑，我们当然跟不上（所以位置每次都不同）。
            //   解法：给我们的按钮挂一个 `OnPreDrawListener`，**每帧绘制前**把邻居的当前 translation 同步过来
            //   （绝对赋值，见 followBellPosition），跟 ROM 的动画器同一帧生效。
            runCatching {
                btn.viewTreeObserver.addOnPreDrawListener(
                    object : android.view.ViewTreeObserver.OnPreDrawListener {
                        override fun onPreDraw(): Boolean {
                            entryPairs[btn]?.let { bb -> followBellPosition(btn, bb) }
                            return true
                        }
                    }
                )
                SLog.i("$TAG: 入口按钮已挂每帧跟随($from)")
            }.onFailure { SLog.e("$TAG: 挂每帧跟随失败", it) }
            btn.visibility = if (runCatching { Cfg.notifBox() }.getOrDefault(false)) View.VISIBLE else View.GONE
            NotifBox.bindBadge(btn)
            // ★ C17 适配：头部那排按钮在 C17 上**带圆形底板**（铃铛外面那层浅色圆盘），
            //   而我们是裸图标，摆在旁边只有我们"不合群"。不去猜系统版本，直接**同步邻居的样式**：
            //   底板 drawable / 内边距 / 宽高都跟铃铛一致 —— C16 上铃铛没有底板，那就自动保持裸图标。
            styleLikeNeighbor(btn, bell)
            SLog.i(
                "$TAG: 头部入口已插入($from) row=" + row.javaClass.simpleName +
                    " bell=" + (bell?.javaClass?.simpleName ?: "-")
            )
        }.onFailure { SLog.e("$TAG: 插入头部入口失败($from)", it) }
    }

    /**
     * 让入口按钮长得像它的邻居（铃铛）。
     *
     * 背景：C16 的头部图标是**裸图标**；C17 换成了**圆盘底板 + 图标**（截图对照：
     * 铃铛外面有一层浅色圆形）。我们不可能为每版系统写一套尺寸，所以这里直接同步：
     * 1. 底板：铃铛自己没有就往上找 1~2 层父容器（`settings_button_container` / `SpotLightFrameLayout`）；
     * 2. 内边距：跟铃铛一致（决定图标字形大小）；
     * 3. 宽高：跟铃铛一致（C17 的圆盘是方的 ~36×36，比我们写死的 30×36 宽）。
     */
    private fun styleLikeNeighbor(btn: ImageView, bell: View?) {
        if (bell == null) return
        runCatching {
            var bg = bell.background
            var p: View? = bell.parent as? View
            var depth = 0
            while (bg == null && p != null && depth < 2) {
                bg = p.background
                p = p.parent as? View
                depth++
            }
            if (bg != null) {
                // 同一个 Drawable 实例不能挂到两个 View 上（bounds/state 会互相污染），复制一份
                val copy = bg.constantState?.newDrawable(btn.resources)?.mutate() ?: bg
                btn.background = copy
            }
            // ★ 3085：**不再同步邻居的 padding**。字号大小改由 TrayDrawable.glyph 按
            //   「邻居字形像素宽 / 我们的宽」算（见 followBellPosition），padding 留着会让两者相乘、缩过头。
            btn.setPadding(0, 0, 0, 0)
            val lb = bell.layoutParams
            val lm = btn.layoutParams
            if (lb != null && lm != null && lb.width > 0 && lb.height > 0) {
                lm.width = lb.width
                lm.height = lb.height
                btn.layoutParams = lm
            }
            SLog.i(
                "$TAG: 入口按钮已对齐邻居 bg=" + (bg != null) +
                    " pad=" + bell.paddingLeft + "/" + bell.paddingTop +
                    " size=" + (lb?.width ?: -1) + "x" + (lb?.height ?: -1)
            )
        }.onFailure { SLog.e("$TAG: 对齐邻居样式失败", it) }
    }

    /** 点入口 = 切换「收纳箱浏览模式」（不新建面板，直接换过滤规则） */
    private fun onEntryClick(v: View) {
        val on = NotifBox.toggleBoxMode()
        val d = (v as? ImageView)?.drawable as? TrayDrawable
        d?.active = on
        v.invalidate()
    }

    /** 造一个 ConstraintLayout.LayoutParams（反射，避免编译期依赖 constraintlayout） */
    private fun makeLp(parent: ViewGroup, w: Int, h: Int): ViewGroup.LayoutParams {
        var c: Class<*>? = parent.javaClass
        while (c != null && c != ViewGroup::class.java) {
            val lpCls = runCatching { Class.forName(c!!.name + "\$LayoutParams", false, c.classLoader) }.getOrNull()
            if (lpCls != null && ViewGroup.LayoutParams::class.java.isAssignableFrom(lpCls)) {
                runCatching {
                    val ctor = lpCls.getConstructor(Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                    return ctor.newInstance(w, h) as ViewGroup.LayoutParams
                }
            }
            c = c.superclass
        }
        return ViewGroup.LayoutParams(w, h)
    }

    private fun setIntField(o: Any?, name: String, v: Int) {
        if (o == null) return
        runCatching { o.javaClass.getField(name).setInt(o, v) }
            .recoverCatching { o.javaClass.getDeclaredField(name).also { it.isAccessible = true }.setInt(o, v) }
    }

    private fun setFloatField(o: Any?, name: String, v: Float) {
        if (o == null) return
        runCatching { o.javaClass.getField(name).setFloat(o, v) }
            .recoverCatching { o.javaClass.getDeclaredField(name).also { it.isAccessible = true }.setFloat(o, v) }
    }

    private fun findById(root: View, name: String, fallback: Int): View? {
        val id = runCatching { root.context.resources.getIdentifier(name, "id", PKG_SELF) }.getOrDefault(0)
        val v = if (id != 0) root.findViewById<View>(id) else null
        return v ?: runCatching { root.findViewById<View>(fallback) }.getOrNull()
    }

    // ---------------- 长按菜单里的「收纳 / 移出收纳」 ----------------

    /** 我们自己塞进菜单的 item → (通知 key, 是否"收纳"动作) */
    private val ourItems: MutableMap<Any, Pair<String, Boolean>> =
        java.util.Collections.synchronizedMap(java.util.WeakHashMap<Any, Pair<String, Boolean>>())

    /** 规则项 → (包名, 类型签名) */
    private val ruleItems: MutableMap<Any, Pair<String, String>> =
        java.util.Collections.synchronizedMap(java.util.WeakHashMap<Any, Pair<String, String>>())

    /** 收纳项 → 该通知的 (包名, 类型签名)（插入菜单时就算好，点击时直接用） */
    private val itemSigs: MutableMap<Any, Pair<String, String>> =
        java.util.Collections.synchronizedMap(java.util.WeakHashMap<Any, Pair<String, String>>())

    @Volatile
    private var opSetting: Any? = null

    @Volatile
    private var itemCtor: java.lang.reflect.Constructor<*>? = null

    /** 最近一次长按的 row（suspend 异步返回时，给 setItemList 兜底用） */
    @Volatile
    private var pendingRow: Any? = null

    @Volatile
    private var pendingAt = 0L

    /**
     * 长按通知 → 那个菜单（置顶通知/静默通知/浮窗打开/更多设置/关闭通知），我们往里插一项。
     *
     * 生成处（唯一）：`NotificationPopupMenuItemCreatorImpl#createMenuList(Context, ExpandableNotificationRow, Continuation)`
     * 点击：`NotificationPopupMenuImpl#access$onClick(row, item, impl)` 是三条点击路径的唯一汇合点。
     */
    fun hookLongPress(cl: ClassLoader?): Int {
        var n = 0
        val opCls = runCatching { Rt.cls(cl, PKG_MENU + "NotificationPopupMenuOperation\$Operation") }.getOrNull()
        val itemCls = runCatching { Rt.cls(cl, PKG_MENU + "NotificationPopupMenu\$MenuItem") }.getOrNull()
        val creator = runCatching { Rt.cls(cl, PKG_MENU + "NotificationPopupMenuItemCreatorImpl") }.getOrNull()
        val impl = runCatching { Rt.cls(cl, PKG_MENU + "NotificationPopupMenuImpl") }.getOrNull()

        if (opCls != null) {
            opSetting = runCatching { opCls.getField("NOTIFICATION_SETTING").get(null) }.getOrNull()
                ?: runCatching {
                    opCls.getDeclaredField("NOTIFICATION_SETTING").also { it.isAccessible = true }.get(null)
                }.getOrNull()
        }
        if (itemCls != null && opCls != null) {
            menuItemCls = itemCls
            itemCtor = runCatching {
                itemCls.getDeclaredConstructor(opCls, String::class.java, Drawable::class.java)
                    .also { it.isAccessible = true }
            }.getOrNull()
        }

        val m = creator?.let { runCatching { Rt.methodByName(it, "createMenuList", 3) }.getOrNull() }
        if (m != null && itemCtor != null) {
            runCatching { m.isAccessible = true }
            val cb = object : XMH() {
                // ★ C17（ColorOS 17）实测：`NotificationPopupMenuImpl#prepare` **一次都不被调用**，
                //   只剩 `setItemList` 兜底，而兜底时拿不到 row（日志：`setItemList 兜底 row=null`）。
                //   这里 before 的**第 2 个参数就是被长按的 row**
                //   （签名 `createMenuList(Context, ExpandableNotificationRow, Continuation)`），
                //   在进协程之前先记下来 —— after 拿到 COROUTINE_SUSPENDED 也不影响。
                override fun beforeHookedMethod(param: XMP) {
                    param.args.getOrNull(1)?.let { rememberRow(it, "createMenuList") }
                }

                override fun afterHookedMethod(param: XMP) = insertMenuItem(param)
            }
            if (XBridge.hookMethod(m, cb) != null) n++
        } else {
            SLog.i("$TAG: 长按菜单没挂上 creator=" + (creator != null) + " itemCtor=" + (itemCtor != null))
        }

        // ★★★ 抓 row 的**可靠**入口：`NotificationPopupMenuImpl#prepare(row, rect, parent, dismiss, cont)`。
        //   `createMenuList` 是 suspend 方法：异步时它的 after 拿到的是 COROUTINE_SUSPENDED，
        //   菜单列表要等协程恢复才被消费 —— 所以之前有一半通知插不进「收纳」。
        //   `prepare` 的**第 0 个参数就是 row**，而且它在协程启动**之前**被调用（同步），
        //   所以这里 100% 能拿到被长按的那一行。
        runCatching {
            val pm = Rt.methodByName(impl, "prepare", 5)
            if (pm == null) {
                SLog.i("$TAG: 找不到 NotificationPopupMenuImpl.prepare（row 可能抓不到）")
                return@runCatching
            }
            pm.isAccessible = true
            val cb = object : XMH() {
                override fun beforeHookedMethod(param: XMP) {
                    val row = param.args.getOrNull(0)
                    // ★ 只在拿到真 row 时才记 —— 同一时刻会有别的 prepare 调用（row=null），
                    //   覆盖掉就把真 row 冲掉了（3065 实测踩到）。
                    if (row != null) {
                        pendingRow = row
                        pendingAt = android.os.SystemClock.elapsedRealtime()
                        SLog.i("$TAG: prepare 抓到 row=" + row.javaClass.simpleName)
                    } else {
                        SLog.i("$TAG: prepare row=null（忽略，保留上一个）")
                    }
                }
            }
            if (XBridge.hookMethod(pm, cb) != null) n++
        }.onFailure { SLog.e("$TAG: prepare 挂钩失败", it) }

        // ★★★ 第二路抓 row：**长按监听器**（C17 的主力）
        //   C17 上 `prepare` 已经不存在调用，但"长按这一下"一定会走
        //   `ExpandableNotificationRow$LongPressListener#onLongPress(row, x, y, item)`。
        //   我们不去追那个 lambda 的混淆类名（每版都变），而是把 row 的
        //   `setLongPressListener(listener)` 拦下来，换成一个「先记 row、再原样转给原 listener」的代理。
        runCatching {
            val rowCls = Rt.cls(cl, "com.android.systemui.statusbar.notification.row.ExpandableNotificationRow")
            if (rowCls == null) {
                SLog.i("$TAG: 找不到 ExpandableNotificationRow（长按抓 row 这条路没有）")
                return@runCatching
            }
            val setM = Rt.methodByName(rowCls, "setLongPressListener", 1)
            if (setM == null) {
                SLog.i("$TAG: 找不到 setLongPressListener（长按抓 row 这条路没有）")
                return@runCatching
            }
            setM.isAccessible = true
            val cb = object : XMH() {
                override fun beforeHookedMethod(param: XMP) {
                    val orig = param.args.getOrNull(0) ?: return
                    if (java.lang.reflect.Proxy.isProxyClass(orig.javaClass)) return  // 已经是我们的代理
                    val iface = orig.javaClass.interfaces.firstOrNull {
                        it.name.endsWith("LongPressListener")
                    } ?: return
                    val proxy = runCatching {
                        java.lang.reflect.Proxy.newProxyInstance(
                            iface.classLoader, arrayOf<Class<*>>(iface)
                        ) { _, mm, aa ->
                            if (mm.name == "onLongPress") {
                                aa?.getOrNull(0)?.let { rememberRow(it, "onLongPress") }
                            }
                            try {
                                mm.invoke(orig, *(aa ?: emptyArray()))
                            } catch (t: java.lang.reflect.InvocationTargetException) {
                                throw (t.targetException ?: t)
                            }
                        }
                    }.getOrNull() ?: return
                    param.args[0] = proxy
                }
            }
            if (XBridge.hookMethod(setM, cb) != null) n++
        }.onFailure { SLog.e("$TAG: setLongPressListener 挂钩失败", it) }

        // 兜底路：`COUIPopupListWindow#setItemList(List)`（菜单列表最终都从这里进）
        runCatching {
            val winCls = Rt.cls(cl, "com.coui.appcompat.poplist.COUIPopupListWindow") ?: return@runCatching
            val setM = Rt.methodByName(winCls, "setItemList", 1) ?: return@runCatching
            setM.isAccessible = true
            val cb = object : XMH() {
                override fun beforeHookedMethod(param: XMP) {
                    val list = param.args.getOrNull(0) as? MutableList<Any?> ?: return
                    var row = pendingRow
                    if (row != null &&
                        android.os.SystemClock.elapsedRealtime() - pendingAt > 8000L
                    ) row = null
                    val use = row ?: rowFromWindow(param.thisObject)
                    SLog.i(
                        "$TAG: setItemList 兜底#" + (++setListSeq) +
                            " row=" + (use?.javaClass?.simpleName ?: "null") +
                            " n=" + list.size
                    )
                    // ★ 3334：`setItemList` 也服务于**别的** COUI 弹窗 —— 实测控制中心右上角
                    //   「⋮ → 控制中心设置」这个菜单也是从这里进的，以前被无脑插了「收纳」。
                    //   拿不到 row 时先看菜单内容像不像通知长按菜单，不像就跳过。
                    if (use == null && !looksLikeNotifMenu(list)) {
                        SLog.i("$TAG: setItemList 跳过（不是通知长按菜单）")
                        return
                    }
                    // ★ row 可能真的拿不到（C17 实测）→ 也照样插，key 延后到点击时解析
                    runCatching { insertIntoList(list, use, null) }
                    if (row != null) pendingRow = null
                }
            }
            if (XBridge.hookMethod(setM, cb) != null) n++
        }.onFailure { SLog.e("$TAG: setItemList 兜底挂钩失败", it) }

        val oc = impl?.let { runCatching { Rt.methodByName(it, "access\$onClick", 3) }.getOrNull() }
        if (oc != null) {
            runCatching { oc.isAccessible = true }
            val cb = object : XMH() {
                override fun beforeHookedMethod(param: XMP) {
                    val item = param.args.getOrNull(1) ?: return
                    // 规则项优先（「以后都收纳这类」）
                    val rule = ruleItems[item]
                    if (rule != null) {
                        param.setResult(null)
                        toggleRule(rule.first, rule.second)
                        return
                    }
                    val act = ourItems[item] ?: return
                    param.setResult(null)
                    var key = act.first
                    var sig = itemSigs[item]
                    // ★ 3356：不管 key 是不是延后解析，都先把被长按的 row 的 entry 拿到 ——
                    //   `archive()` 要用它记宽松身份（pkg|id|user），这是 key 对不上时的兜底。
                    val rowArg = param.args.getOrNull(2)
                    val ent0 = runCatching { Rt.callNoArg(rowArg, "getEntry") }.getOrNull()
                    if (key.isEmpty()) {
                        // ★ C17 延后解析：`access$onClick(impl, item, row)` 的第 3 个参数就是被长按的 row
                        val rr = rowArg
                        val ent = ent0
                        val k2 = NotifBox.keyOf(ent)
                        if (k2 == null) {
                            SLog.i(
                                "$TAG: 点击时仍取不到 key（row=" +
                                    (rr?.javaClass?.simpleName ?: "null") + "）"
                            )
                            return
                        }
                        key = k2
                        sig = NotifBoxRules.signatureOf(ent)
                    }
                    // 以"当前是否已收纳"为准（延后解析时插入那一刻的状态不可靠）
                    if (!NotifBox.isArchived(key)) {
                        // ★ 收纳 = 收下这一条 **+ 记一条永久规则**（这个 App 这类通知以后都自动收纳）
                        NotifBox.archive(key, ent0)
                        if (sig != null) {
                            NotifBoxRules.add(
                                NotifBoxRules.Rule(
                                    pkg = sig.first,
                                    sig = sig.second,
                                    appName = NotifBoxRules.appNameOf(sig.first),
                                    typeName = ""
                                )
                            )
                            SLog.i("$TAG: 收纳并记规则 " + sig.first + " / " + sig.second)
                        } else {
                            // ★ 3354：只收了 key、没记规则 —— 外部用户「点收纳没反应」的
                            //   第二大嫌疑（没有规则兜底，就只剩 key 命中这一条路）。
                            SLog.i("$TAG: 收纳但没记规则（sig=null，只存了 key）")
                        }
                    } else {
                        // ★ 移出收纳 = 放回这一条 **+ 取消那条规则**（同一个操作，两个效果）
                        NotifBox.restore(key)
                        if (sig != null && NotifBoxRules.matches(sig.first, sig.second)) {
                            NotifBoxRules.remove(sig.first, sig.second)
                            SLog.i("$TAG: 移出收纳并取消规则 " + sig.first + " / " + sig.second)
                        }
                    }
                }
            }
            if (XBridge.hookMethod(oc, cb) != null) n++
        } else {
            SLog.i("$TAG: 长按菜单点击钩子没挂上（impl=" + (impl != null) + "）")
        }
        SLog.i("$TAG: 长按菜单「收纳」挂上=$n")
        return n
    }

    /** 记住「被长按的那一行」（多路来源共用：prepare / createMenuList / onLongPress） */
    private var rowSeq = 0

    /** setItemList 兜底的序号（同样为了绕开 SLog 去重） */
    private var setListSeq = 0

    private fun rememberRow(row: Any, from: String) {
        pendingRow = row
        pendingAt = android.os.SystemClock.elapsedRealtime()
        // ★ 日志带序号：SLog 是按 tag 去重的，同一条文案第二次不打印（吃了两次亏）
        SLog.i("$TAG: 抓到 row#" + (++rowSeq) + "(" + from + ")=" + row.javaClass.simpleName)
    }

    @Suppress("UNCHECKED_CAST")
    private fun insertMenuItem(param: XMP) {
        val res = param.result
        val list = res as? MutableList<Any?>
        if (list == null) {
            // `createMenuList` 是 **suspend** 方法：真正异步时这里拿到的是 COROUTINE_SUSPENDED，
            // 列表要等协程恢复后才会被消费 → 交给 `setItemList` 那条兜底路（见 hookLongPress）。
            SLog.i("$TAG: 菜单 result=" + (res?.javaClass?.name ?: "null") + "（等 setItemList 兜底）")
            return
        }
        insertIntoList(list, param.args.getOrNull(1) ?: pendingRow, param.args.getOrNull(0) as? Context)
    }

    /**
     * 往菜单列表里插「收纳 / 移出收纳」（两条路共用）：
     * 1. `createMenuList` after-hook（协程**同步**返回列表时）；
     * 2. `COUIPopupListWindow#setItemList` before-hook（协程**异步**恢复时 —— 实测这才是多数情况）。
     */
    /** 3334：通知长按菜单项的类（`NotificationPopupMenu$MenuItem`），用来区分别的 COUI 弹窗 */
    private var menuItemCls: Class<*>? = null

    /**
     * 3334：判断这份 COUI 弹窗菜单是不是「通知长按菜单」。
     *
     * `COUIPopupListWindow#setItemList` 是**所有** COUI 弹窗的公共入口 ——
     * 控制中心右上角「⋮ → 控制中心设置」也走这里（实测 n=5），以前被无脑插了「收纳」。
     * 判断**按菜单项类型**（`NotificationPopupMenu$MenuItem`），比匹配中文标题稳：
     * 其它 COUI 弹窗的 item 是另外的类。
     */
    private fun looksLikeNotifMenu(list: MutableList<Any?>): Boolean {
        val c = menuItemCls ?: return false
        for (o in list) {
            if (o != null && c.isInstance(o)) return true
        }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    private fun insertIntoList(list: MutableList<Any?>, row: Any?, ctx0: Context?) {
        if (list.isEmpty()) {
            SLog.i("$TAG: 菜单 list 为空（跳过）")
            return
        }
        if (list.any { ourItems.containsKey(it) }) return         // 幂等（两条路都可能进来）
        val ctor = itemCtor ?: return
        if (!runCatching { Cfg.notifBox() }.getOrDefault(false)) {
            SLog.i("$TAG: 菜单插入跳过：开关关着")
            return
        }
        val ctx = ctx0 ?: (row as? View)?.context ?: Rt.ctx() ?: return
        val entry = runCatching { Rt.callNoArg(row, "getEntry") }.getOrNull()
        var key = NotifBox.keyOf(entry)
        // ★ C17（3073 实测）：长按时拿不到 row（`setItemList 兜底 row=null`），
        //   于是这里以前直接 return → 菜单里永远没有「收纳」。
        //   现在改成**延后解析**：先插一项「收纳」，等用户点它时再从
        //   `access$onClick(impl, item, row)` 的第 3 个参数拿到 row → 反查 key。
        val deferred = (key == null)
        if (deferred) {
            SLog.i(
                "$TAG: 菜单插入：拿不到 row，先插「收纳」并延后解析 key" +
                    " row=" + (row?.javaClass?.name ?: "null")
            )
            key = ""
        }
        val archived = if (deferred) false else NotifBox.isArchived(key)
        val label = if (archived) "移出收纳" else "收纳"
        val item = runCatching { ctor.newInstance(opSetting, label, menuIcon(ctx, archived)) as Any }.getOrNull()
            ?: run {
                SLog.i("$TAG: 菜单插入跳过：MenuItem 造不出来")
                return
            }
        ourItems[item] = key!! to !archived
        // 插入时就把签名算好（点击时手里只有 key，反查 entry 不可靠）
        if (!deferred) NotifBoxRules.signatureOf(entry)?.let { itemSigs[item] = it }

        var idx = -1
        for (i in list.indices) {
            if (isSettingItem(list[i])) {
                idx = i
                break
            }
        }
        if (idx < 0) idx = (list.size - 2).coerceAtLeast(1)
        runCatching { list.add(idx, item) }
        SLog.i("$TAG: 长按菜单已插入「" + label + "」 idx=" + idx + " size=" + list.size)

        // ★★ 菜单里只有「收纳 / 移出收纳」一项（用户 15:0x 定稿：两个功能合成一个操作）：
        //   「收纳」= 收下这一条 + 记一条「这个 App 这类通知以后都自动收纳」的永久规则；
        //   「移出收纳」= 放回这一条 + 取消这条规则。
        //   所以这里**不再**额外插「以后都收纳这类 / 不再自动收纳这类」。
    }

    /**
     * 切换一条「按 App + 类型自动收纳」的规则。
     * 规则永久保存在 SystemUI 的 data 目录里（`notifbox_rules.txt`），
     * 命中规则的新通知会被直接收进收纳盒（静默）。
     */
    private fun toggleRule(pkg: String, sig: String) {
        if (NotifBoxRules.matches(pkg, sig)) {
            NotifBoxRules.remove(pkg, sig)
            SLog.i("$TAG: 已取消规则 $pkg / $sig")
        } else {
            NotifBoxRules.add(
                NotifBoxRules.Rule(
                    pkg = pkg,
                    sig = sig,
                    appName = NotifBoxRules.appNameOf(pkg),
                    typeName = ""
                )
            )
            SLog.i("$TAG: 已加规则 $pkg / $sig")
        }
    }

    /**
     * 从弹窗窗口对象里反查被长按的通知行（`prepare` 没抓到时的兜底）。
     * `NotificationPopupMenuImpl` 里有 `menuAnchorView` / `popupMenu`，
     * 顺着 view 树往上找 `ExpandableNotificationRow`。
     */
    private fun rowFromWindow(win: Any?): Any? {
        if (win == null) return null
        val views = ArrayList<View>()
        for (name in arrayOf("menuAnchorView", "mAnchorView", "anchorView")) {
            runCatching { Rt.get(win, name) }.getOrNull()?.let { if (it is View) views.add(it) }
        }
        for (v in views) {
            val hit = findRowInTree(v)
            if (hit != null) return hit
        }
        return null
    }

    /** 沿 view 树往上/往下找带 `getEntry()` 的通知行 */
    private fun findRowInTree(v: View): Any? {
        if (v == null) return null
        if (hasGetEntry(v)) return v
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                val c = v.getChildAt(i) ?: continue
                val hit = findRowInTree(c)
                if (hit != null) return hit
            }
        }
        var p: Any? = v.parent
        var guard = 0
        while (p != null && guard++ < 12) {
            if (p is View && hasGetEntry(p)) return p
            p = (p as? View)?.parent
        }
        return null
    }

    private fun hasGetEntry(v: Any): Boolean =
        runCatching { Rt.methodByName(v.javaClass, "getEntry", 0) != null }.getOrDefault(false)

    /** 判定「更多设置」那一项：operation == NOTIFICATION_SETTING（字段名是 `getMenuOperation`） */
    private fun isSettingItem(o: Any?): Boolean {
        if (o == null || opSetting == null) return false
        val op = runCatching { Rt.callNoArg(o, "getMenuOperation") }.getOrNull()
            ?: runCatching { o.javaClass.getField("menuOperation").get(o) }.getOrNull()
            ?: runCatching {
                o.javaClass.getDeclaredField("menuOperation").also { it.isAccessible = true }.get(o)
            }.getOrNull()
        return op != null && op === opSetting
    }

    /** 菜单图标：优先复用 SystemUI 自己的 drawable，失败退回自绘托盘 */
    private fun menuIcon(ctx: Context, archived: Boolean): Drawable {
        val id = runCatching {
            ctx.resources.getIdentifier("notification_popup_menu_close_notification_icon", "drawable", PKG_SELF)
        }.getOrDefault(0)
        if (id != 0) {
            runCatching { ctx.getDrawable(id) }.getOrNull()?.let { return it }
        }
        return TrayDrawable(0x99000000.toInt()).also { it.active = archived }
    }

    // ---------------- 入口图标 ----------------

    /** 自绘「收纳箱」图标：托盘 + 箭头；[active] = 正在浏览收纳箱（描边变蓝）。
     *  用户 04:0x 定稿：**不要数字角标**。 */
    class TrayDrawable(private val tint: Int) : Drawable() {

        var count = 0
            set(v) {
                field = v
                invalidateSelf()
            }

        var active = false
            set(v) {
                field = v
                invalidateSelf()
            }

        /**
         * ★ 3085：字形相对边框的占比（1f = 铺满）。
         * 邻居（铃铛/齿轮）的图标是"框里一个更小的字形"，我们是按框铺满画的 →
         * 看上去比邻居大一圈。这里由 [glyphPxOf] 量出邻居字形的真实像素宽，换算成占比。
         */
        var glyph = 1f
            set(v) {
                field = v
                invalidateSelf()
            }

        /**
         * 3093：**直接指定"可见宽"（px）**，不再依赖 View 的大小/比例。
         * ≤0 时退回 [glyph] 的比例算法。
         */
        var pxContent = -1f
            set(v) {
                field = v
                invalidateSelf()
            }

        private val p = Paint(Paint.ANTI_ALIAS_FLAG)
        private val text = Paint(Paint.ANTI_ALIAS_FLAG)

        override fun draw(canvas: Canvas) {
            val b = bounds
            // ★ 3093：可见宽 = K × s（K 由三版截图反推 ≈0.60），所以 s = 目标可见宽 / K。
            //   指定了 pxContent 就完全按"像素"画，View 多大都不影响字形大小。
            val sBase = if (pxContent > 0f) {
                pxContent / TRAY_K
            } else {
                minOf(b.width(), b.height()).toFloat() * glyph
            }
            val s = sBase.coerceAtMost(minOf(b.width(), b.height()).toFloat() * 2.2f)
            val cx = b.exactCenterX()
            val cy = b.exactCenterY()

            p.style = Paint.Style.STROKE
            // ★ 3088：笔画也按实测校准。截图逐像素量（3087）：
            //   邻居（齿轮/铃铛）笔画 ≈6px、可见字形 ≈70px → 笔画/字形 ≈ 0.086；
            //   我们的字形 = 0.6×s（+ 笔画两端各半），所以 stroke = s × 0.051 才和邻居一样粗。
            p.strokeWidth = s * 0.069f
            p.strokeCap = Paint.Cap.ROUND
            p.strokeJoin = Paint.Join.ROUND
            p.color = if (active) 0xFF3A7AFE.toInt() else tint

            // 托盘
            val l = cx - s * 0.30f
            val r = cx + s * 0.30f
            val top = cy - s * 0.10f
            val bot = cy + s * 0.28f
            val rad = s * 0.10f
            val path = android.graphics.Path()
            path.moveTo(l, top)
            path.lineTo(l, bot - rad)
            path.quadTo(l, bot, l + rad, bot)
            path.lineTo(r - rad, bot)
            path.quadTo(r, bot, r, bot - rad)
            path.lineTo(r, top)
            canvas.drawPath(path, p)

            // 向下箭头（收纳 = 收进去）
            val ax = cx
            canvas.drawLine(ax, cy - s * 0.30f, ax, cy + s * 0.04f, p)
            canvas.drawLine(ax - s * 0.10f, cy - s * 0.06f, ax, cy + s * 0.05f, p)
            canvas.drawLine(ax + s * 0.10f, cy - s * 0.06f, ax, cy + s * 0.05f, p)
        }

        override fun setAlpha(alpha: Int) {
            p.alpha = alpha
            invalidateSelf()
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            p.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }

    fun update(v: View, count: Int) {
        val d = (v as? ImageView)?.drawable as? TrayDrawable ?: return
        d.count = count
        v.invalidate()
    }

    private fun Context.getColorSafe(c: Int) = runCatching {
        if (android.os.Build.VERSION.SDK_INT >= 23) getColor(c) else c
    }.getOrDefault(c)
}
