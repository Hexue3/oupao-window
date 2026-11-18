package com.oupao.wiz

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator

/**
 * 合并气泡（**只改形态，不改信息流**），支持**多个气泡同时存在**。
 *
 * 信息流沿用 `Sug.show(kind)`：判定（同会话跳过 / keepOld / 游戏屏蔽）、
 * 头像（应用图标兜底 + 微信QQ 角标）、文本（`Sug.txt`）、点击（`Sug.fire()`）都在 Sug 里。
 * 本类只负责形态：圆泡 + 向右/左延伸的信息长条（信息是画出来的虚空）、光晕、
 * 信息超时收回、常驻、上下滑切换、左右滑展开收起、长按拖动 + 记忆位置、
 * **双击关闭**（单击 = 打开小窗，要等 300ms 确认没有第二下才执行；
 * 原来的「下滑关闭」已移除 —— 它跟上下滑切换抢手势，多条消息时永远关不掉）。
 *
 * ## 多窗口语义
 * - **常驻(persist)**：每条新通知都新开一个气泡窗口（最多 3 个，超出丢最老的），
 *   互相不覆盖、各自独立拖动/点击/收起 → 常驻气泡不会被后来者"占位"。
 * - **非常驻**：只有一个气泡窗口，新通知替换旧的（和以前一致）。
 * - `Sug.dismiss()`（通知被清等）→ 收掉全部窗口。
 *
 * 接线：`Sug$S.run()` → `Sug.show(2)`；`Sug.show()` 末尾 `if (kind==2) Bub.attach();`
 */
object Bub {

    // ================= 配置 =================

    const val K_V2 = "oupao_bub_v2"
    const val K_PERSIST = "oupao_bub_persist"
    const val K_INFO_SEC = "oupao_bub_info_sec"
    const val K_SEC = "oupao_bub_sec"

    private const val POS_KEY = "oupao_bub2_pos"
    private const val POS_KEY_LAND = "oupao_bub2_pos_land"
    private const val SYS_CFG_DIR = "/data/system/oupao_cfg"
    private const val MAX_WINS = 3

    /** 双击判定窗口（ms）：两次点按间隔小于它 = 双击（= 关闭气泡） */
    private const val DOUBLE_TAP_MS = 300L

    @JvmStatic
    fun v2On(): Boolean = Cfg.getInt(K_V2, 0) == 1

    @JvmStatic
    fun persistOn(): Boolean = Cfg.getInt(K_PERSIST, 0) == 1

    @JvmStatic
    fun infoMs(): Long = (Cfg.getStr(K_INFO_SEC, "5").toIntOrNull() ?: 5).coerceIn(1, 30) * 1000L

    @JvmStatic
    fun bubbleMs(): Long = (Cfg.getStr(K_SEC, "4").toIntOrNull() ?: 4).coerceIn(1, 30) * 1000L

    // ================= 窗口集合 =================

    private val wins = ArrayList<Win>()
    private val main = Handler(Looper.getMainLooper())

    /** 最近消息环：**全局**的（通知气泡虽然每次替换窗口，但环要跨通知累积，上下滑才有得切） */
    private val ring = ArrayList<Snap>()
    private var cur = 0
    private var lastShowAt = 0L

    @JvmStatic
    fun mergedShown(): Boolean = wins.isNotEmpty()

    /** 是否允许「多个气泡同时存在」（开启合并 + 常驻） */
    @JvmStatic
    fun coexistOn(): Boolean = v2On() && persistOn()

    /**
     * **只对「合并的通知气泡」**不理会输入法：键盘打开时也照样显示、也不再因为键盘弹起把它收起来。
     * 分离气泡 / 建议气泡（kind≠2）必须保持原有输入法行为（用户明确要求）。
     */
    @JvmStatic
    fun ignoreImeFor(kind: Int): Boolean = v2On() && kind == 2

    /** 由 `Sug.show()` 末尾调用（kind==2） */
    @JvmStatic
    fun attach(): Boolean {
        if (!v2On()) { SLog.i("Bub: attach 跳过 v2Off"); return false }
        if (Cfg.getProp("persist.oupao.bub_v2", "1") == "0") { SLog.i("Bub: attach 跳过 propOff"); return false }
        val ctx = Sug.ctx ?: run { SLog.i("Bub: attach 跳过 ctx=null"); return false }
        val wm = Sug.bubWm() ?: run { SLog.i("Bub: attach 跳过 wm=null"); return false }
        cancelPendingHide()   // 撤销 show() 开头那次 dismiss 的延时销毁
        cancelHideSoon()      // 撤销「通知被取消」触发的延时收气泡

        // ① 已有气泡 → **原地更新**：窗口只延长/缩短，绝不重建
        val exist = wins.lastOrNull()
        if (exist != null) {
            runCatching { Sug.hideBanner() }
            if (exist.updateInPlace(Sug.bubView())) {
                Sug.cancelTimer()
                return true
            }
            SLog.i("Bub: 原地更新失败 → 退回重建")
        }

        // ② 没有气泡（或更新失败）→ 用 Sug 这次建好的窗口，换成我们自绘的
        val tv = Sug.bubView() ?: run { SLog.i("Bub: attach 跳过 view=null"); hide(); return false }
        val oldLp = tv.layoutParams as? WindowManager.LayoutParams
        if (oldLp == null) { SLog.i("Bub: attach 跳过 lp=" + tv.layoutParams); hide(); return false }

        return runCatching {
            hideAll()
            runCatching { Sug.hideBanner() }
            val w = Win(ctx, wm, tv, oldLp, wins.size)
            if (!w.show()) return@runCatching false
            wins.add(w)
            Sug.cancelTimer()
            true
        }.onFailure { SLog.e("Bub: attach 异常", it); hide() }.getOrDefault(false)
    }

    /** 由 `Sug.dismiss()` 调用：收掉全部气泡 */
    private var hideSoonRun: Runnable? = null

    /** 通知被取消时用：延时收气泡；期间来新消息会撤销（避免来消息就拆窗口） */
    @JvmStatic
    fun hideSoon(delayMs: Long) {
        main.post {
            hideSoonRun?.let { main.removeCallbacks(it) }
            val r = Runnable { hideSoonRun = null; hideAll() }
            hideSoonRun = r
            main.postDelayed(r, delayMs.coerceIn(0L, 5000L))
        }
    }

    private fun cancelHideSoon() {
        val r = hideSoonRun ?: return
        hideSoonRun = null
        main.post { main.removeCallbacks(r) }
    }

    private var pendingHide: Runnable? = null
    private var hideSeq = 0

    /** 撤销「延时销毁」（show() 紧接着会 attach，这中间不能把窗口拆了） */
    private fun cancelPendingHide() {
        pendingHide?.let { main.removeCallbacks(it) }
        pendingHide = null
    }

    @JvmStatic
    fun hide() {
        // 原来这里是**立刻**销毁：Sug.show() 第一件事就是 dismiss()->hide()，
        // 等 attach() 跑到时窗口已经没了 → 每次都重建。这里改成短暂延时 + 可取消。
        cancelPendingHide()
        val seq = ++hideSeq
        val doIt = Runnable {
            if (seq != hideSeq) return@Runnable
            pendingHide = null
            hideAll()
        }
        pendingHide = doIt
        main.postDelayed(doIt, 180L)
    }

    private fun hideAll() {
        pendingHide = null
        for (w in wins.toList()) w.destroy()
        wins.clear()
    }

    // ================= 位置（合并气泡独立一份） =================

    private fun posKey(ctx: Context): String =
        if (runCatching { BubPos.land(ctx) }.getOrDefault(false)) POS_KEY_LAND else POS_KEY

    private fun loadPos(ctx: Context): IntArray? = runCatching {
        val raw = Cfg.getStrFresh(posKey(ctx), "")
        if (raw.isNullOrEmpty()) return@runCatching null
        val xy = BubPos.parse(raw) ?: return@runCatching null
        val scr = runCatching { BubPos.screen(ctx) }.getOrNull() ?: return@runCatching xy
        val w = Ui.dp(ctx, 46) + Ui.dp(ctx, 12)
        val h = Ui.dp(ctx, 46) + Ui.dp(ctx, 12)
        intArrayOf(
            xy[0].coerceIn(0, (scr[0] - w).coerceAtLeast(0)),
            xy[1].coerceIn(0, (scr[1] - h).coerceAtLeast(0))
        )
    }.getOrNull()

    private fun savePosAt(x: Int, y: Int, ctx: Context) {
        runCatching {
            val val_ = x.toString() + "," + y.toString()
            val primary = SYS_CFG_DIR + "/" + posKey(ctx)
            Cfg.writeStr(primary, val_)
            // 读回校验：目录里若留着别的 uid 建的旧文件，system_server 覆盖不了且不抛异常 → 位置记不住
            if (Cfg.getStrFresh(posKey(ctx), "") != val_) {
                runCatching { java.io.File(primary).delete() }
                Cfg.writeStr(primary, val_)
                SLog.i("Bub: 位置写回不一致 -> 删除重写")
            }
            SLog.i("Bub: 位置已保存 " + posKey(ctx) + "=" + val_)
        }.onFailure { SLog.e("Bub: 保存位置失败", it) }
    }

    /**
     * 给 `Sug.applyPos()` 用：只有本次 show 是消息气泡(kind==2)且开启合并时，才用合并气泡自己的位置；
     * 其它 kind 继续用旧键 → 两类气泡位置互不影响。
     */
    @JvmStatic
    fun posForSug(): IntArray? = runCatching {
        if (Sug.showKind != 2) return null
        if (!v2On()) return null
        val ctx = Sug.ctx ?: return null
        loadPos(ctx)
    }.getOrNull()

    // ================= 单个气泡窗口 =================

    private class Snap(
        val pkg: String?, val txt: String?, val bmp: Bitmap?,
        val kChat: String?, val kUin: String?, val kType: String?,
        val kNi: android.content.Intent?, val kPi: android.app.PendingIntent?, val kUid: Int
    )

    private class Win(
        val ctx: Context,
        val wm: WindowManager,
        val sugView: android.widget.TextView,
        val oldLp: WindowManager.LayoutParams,
        private val index: Int
    ) {
        private var lp: WindowManager.LayoutParams? = null
        private var view: BubView? = null
        private var circleLeft = 0
        private var screenW = 0
        private var screenH = 0

        private var hideRunnable: Runnable? = null
        private var collapseRunnable: Runnable? = null
        private var collapseStep: Runnable? = null

        /**
         * 「信息阶段」的计时起点（uptimeMillis）。
         *
         * ★ 信息收起与气泡消失**都从这一刻算**。
         * 以前是串行的两个定时器：信息收完 → 再等 bubbleMs 才销毁，
         * 所以两个滑杆设成一样的值时，仍然是「信息先消失、气泡还要再过一会才没」。
         * 现在：信息 = 起点 + infoMs；气泡 = 起点 + max(infoMs, bubbleMs)
         * （有文字时气泡至少活到信息收完，没文字时就按气泡停留时间）。
         */
        private var shownAt = 0L

        private var gen = 0
        private var lastToggleAt = 0L
        private var resizeSeq = 0

        fun show(): Boolean = runCatching {
        // 2975：灭屏/锁屏不弹气泡
        if (Cfg.sleepingNow()) return false
            gen++
            armInfoClock()   // 没文字的分支也要有起点，否则 left 会算成负数
            if (SystemClock.uptimeMillis() - lastShowAt > 60_000L) ring.clear()
            lastShowAt = SystemClock.uptimeMillis()
            ring.add(
                Snap(Sug.pkg, Sug.txt, Sug.bmp, Sug.kChat, Sug.kUin, Sug.kType,
                    Sug.kNi ?: Sug.ni, Sug.kPi ?: Sug.pi, Sug.kUid)
            )
            while (ring.size > 6) ring.removeAt(0)
            cur = ring.size - 1

            runCatching { wm.removeView(sugView) }
            val v = BubView(ctx)
            v.owner = this
            v.pad = Ui.dp(ctx, 6)
            v.circle = Ui.dp(ctx, 46)
            v.setItem(Sug.bmp, Sug.txt)

            val scr = runCatching { BubPos.screen(ctx) }.getOrNull()
            if (scr != null) { screenW = scr[0]; screenH = scr[1] }

            val p = oldLp
            p.height = v.minH()

            var topX = p.x
            var topY = p.y
            if (scr != null) {
                val vg = p.gravity and Gravity.VERTICAL_GRAVITY_MASK
                val hg = p.gravity and Gravity.HORIZONTAL_GRAVITY_MASK
                if (vg == Gravity.BOTTOM) topY = scr[1] - p.y - p.height
                if (hg == Gravity.RIGHT) topX = scr[0] - p.x - p.width
            }
            p.gravity = Gravity.TOP or Gravity.START
            p.flags = p.flags or 0x100 or 0x200      // IN_SCREEN + NO_LIMITS（后者拖动必需）

            val saved = loadPos(ctx)
            p.x = saved?.get(0) ?: topX.coerceAtLeast(0)
            p.y = saved?.get(1) ?: topY.coerceAtLeast(0)
            if (saved == null) savePosAt(p.x, p.y, ctx)
            // 多气泡时错开，避免完全重叠
            if (index > 0) {
                val dy = (v.minH() + Ui.dp(ctx, 10)) * index
                p.y = (p.y - dy).coerceAtLeast(0)
            }
            circleLeft = p.x + v.pad
            v.baseX = p.x

            val sw = if (screenW > 0) screenW else ctx.resources.displayMetrics.widthPixels
            val leftSpace = p.x - Ui.dp(ctx, 4)
            val rightSpace = sw - (p.x + v.circle) - Ui.dp(ctx, 4)
            v.dir = if (rightSpace >= leftSpace) 1 else -1
            v.limitText((if (v.dir > 0) rightSpace else leftSpace) - v.pad)

            v.setClosedInstant()
            p.width = v.minW() + v.textW.toInt()
            p.x = if (v.dir > 0) circleLeft - v.pad else circleLeft + v.circle + v.pad - p.width
            if (screenW > 0 && v.dir > 0) p.x = p.x.coerceIn(0, (screenW - p.width).coerceAtLeast(0))

            wm.addView(v, p)
            lp = p
            view = v
            SLog.i(
                "Bub: 形态合并 kind=" + Sug.kind + " pkg=" + Sug.pkg + " idx=" + index +
                    " dir=" + v.dir + " x=" + p.x + " y=" + p.y + " w=" + p.width +
                    " txt=" + (Sug.txt ?: "")
            )

            runCatching { v.popFromCenter() }
            v.startHalo {
                if (Sug.infoOn() && !Sug.txt.isNullOrEmpty()) expand(true)
                else { v.haloDone(); armInfoClock(); scheduleBubbleHide() }
            }
            true
        }.onFailure { SLog.e("Bub: Win.show 异常", it) }.getOrDefault(false)

        /** 前台是否就是「这条气泡对应的应用/会话」 */
        fun matchesFg(fg: String): Boolean {
            val p = ring.getOrNull(cur)?.pkg ?: return false
            if (p != fg) return false

            // 聊天类应用：只要前台是**聊天页**就收。
            // 之前用 Sug.sameChatId()（依赖 startActivity 的 Intent 里带会话 id），
            // 应用内自己导航过去时判不出来 → 用户反馈「进了聊天页气泡还在」。
            val cls = runCatching { Sug.fgActivityClass() }.getOrNull()
            if (cls != null) {
                val lc = cls.lowercase()
                if (lc.contains("chatting") || lc.contains("chatactivity") || lc.contains("chatscreen")) {
                    SLog.i("Bub: 前台是聊天页 " + cls + " → 收气泡")
                    return true
                }
            }
            if (p == "com.tencent.mm" || p == "com.tencent.mobileqq") {
                val same = runCatching { Sug.sameChatId(p) }.getOrDefault(false)
                if (!same) SLog.i("Bub: 前台=" + fg + " 类=" + cls + " 不是同一会话 → 暂不收")
                return same
            }
            return true
        }

        /**
         * **原地**更新已有气泡（不销毁/不新建窗口）：
         * 换内容 → 窗口宽度就地延长到新文本需要的大小 → 文字从圆泡逐渐显示出来 → 弹一下。
         * 位置（圆心）保持不动。
         */
        fun updateInPlace(newSugView: android.widget.TextView?): Boolean = runCatching {
            val v = view ?: return false
            gen++
            if (SystemClock.uptimeMillis() - lastShowAt > 60_000L) ring.clear()
            lastShowAt = SystemClock.uptimeMillis()
            ring.add(
                Snap(Sug.pkg, Sug.txt, Sug.bmp, Sug.kChat, Sug.kUin, Sug.kType,
                    Sug.kNi ?: Sug.ni, Sug.kPi ?: Sug.pi, Sug.kUid)
            )
            while (ring.size > 6) ring.removeAt(0)
            cur = ring.size - 1

            // Sug 这次又新建了一个自己的气泡窗口，收掉它（不显示），我们只留自己这一个
            runCatching { if (newSugView != null) wm.removeView(newSugView) }

            val oldExtra = v.curExtra()          // ★必须在换内容之前取旧宽度
            v.setItem(Sug.bmp, Sug.txt)
            if (screenW <= 0) screenW = ctx.resources.displayMetrics.widthPixels
            val leftSpace = circleLeft - v.pad - Ui.dp(ctx, 4)
            val rightSpace = screenW - (circleLeft + v.circle) - Ui.dp(ctx, 4)
            v.dir = if (rightSpace >= leftSpace) 1 else -1
            v.limitText((if (v.dir > 0) rightSpace else leftSpace) - v.pad)

            val showTxt = Sug.infoOn() && !Sug.txt.isNullOrEmpty()
            if (showTxt) v.setShownInstant() else v.setClosedInstant()
            val newExtra = v.curExtra()
            // 窗口先撑到两者较大值（窗口透明），再让**气泡本身**原地延长/缩短
            resizeWindow(maxOf(oldExtra, newExtra))
            v.animateExtra(oldExtra, newExtra, 360L)
            if (newExtra < oldExtra) {
                val seq = ++resizeSeq
                main.postDelayed({
                    if (seq == resizeSeq) resizeWindow(v.curExtra())
                }, 400L)
            }
            runCatching { v.popFromCenter() }   // 来信息弹一下（弹簧，无 alpha 闪烁）
            SLog.i("Bub: 原地更新 txt=" + (Sug.txt ?: "") + " oldW=" + oldExtra.toInt() + " newW=" + newExtra.toInt() + " winW=" + (lp?.width ?: -1))
            armInfoClock()   // 新消息：两个计时器一起从头算
            if (showTxt) scheduleCollapse() else scheduleBubbleHide()
            true
        }.onFailure { SLog.e("Bub: 原地更新异常", it) }.getOrDefault(false)

        /** 以**圆心**为锚点做一次放大脉冲（0.86 → 略过 1 → 1） */
        fun destroy() {
            gen++
            cancelTimers()
            val v = view
            val w = lp
            view = null
            if (v != null && w != null) runCatching { wm.removeView(v) }
            lp = null
        }

        // ---------- 动画 / 布局 ----------

        private fun expand(open: Boolean, seedVel: Float = Float.NaN) {
            val v = view ?: return
            cancelTimers()
            // ★先给窗口留出目标宽度，否则展开过程会被窗口裁掉 → 看起来"没有动效"
            if (open) resizeWindow(v.textW * 1.06f + Ui.dp(ctx, 6).toFloat())
            v.animateOpen(
                open,
                seedVel,
                onFrame = { v.invalidate() },
                onEnd = {
                    resizeWindow(v.curExtra())     // 宽窗口跟着收回/展开
                    applyPosition()
                    if (open) { armInfoClock(); scheduleCollapse() } else scheduleBubbleHide()
                }
            )
        }

        private fun collapseForDrag() {
            val v = view ?: return
            val steps = 6
            var i = 0
            val start = v.openFrac()
            val r = object : Runnable {
                override fun run() {
                    i++
                    v.setOpenFrac((start * (1f - i.toFloat() / steps)).coerceIn(0f, 1f))
                    if (i >= steps) { collapseStep = null; cancelTimers(); return }
                    main.postDelayed(this, 30)
                }
            }
            collapseStep = r
            main.postDelayed(r, 40)
        }

        /** 只移动，不改窗口尺寸（拖动专用） */
        private fun applyPosition() {
            val v = view ?: return
            val p = lp ?: return
            if (screenW > 0) {
                circleLeft = circleLeft.coerceIn(v.pad, (screenW - v.pad - v.circle).coerceAtLeast(v.pad))
                p.y = p.y.coerceIn(0, (screenH - p.height).coerceAtLeast(0))
            }
            p.x = if (v.dir > 0) circleLeft - v.pad else circleLeft + v.circle + v.pad - p.width
            v.baseX = circleLeft - v.pad
            runCatching { wm.updateViewLayout(v, p) }
        }

        /** 按"文字露出宽度"直接设置窗口宽度（窗口透明，视觉上只有气泡在变） */
        private fun resizeWindow(extra: Float) {
            resizeSeq++
            val v = view ?: return
            val p = lp ?: return
            // 弹簧会过冲，窗口按 1.10 倍 + 余量留宽，避免被裁
            val target = v.minW() + (extra * 1.10f).toInt() + Ui.dp(ctx, 4)
            if (p.width != target) {
                p.width = target
                p.x = if (v.dir > 0) circleLeft - v.pad else circleLeft + v.circle + v.pad - p.width
                runCatching { wm.updateViewLayout(v, p) }
            }
            v.invalidate()
        }

        private fun applySize(full: Boolean) {
            val v = view ?: return
            val p = lp ?: return
            val target = v.minW() + (if (full) v.textW.toInt() else 0)
            if (p.width != target) {
                p.width = target
                p.x = if (v.dir > 0) circleLeft - v.pad else circleLeft + v.circle + v.pad - p.width
                runCatching { wm.updateViewLayout(v, p) }
            } else {
                applyPosition()
            }
            v.invalidate()
        }

        private fun showFix() {
            val v = view ?: return
            if (screenW <= 0) screenW = ctx.resources.displayMetrics.widthPixels
            val leftSpace = circleLeft - v.pad - Ui.dp(ctx, 4)
            val rightSpace = screenW - (circleLeft + v.circle) - Ui.dp(ctx, 4)
            v.dir = if (rightSpace >= leftSpace) 1 else -1
            v.limitText((if (v.dir > 0) rightSpace else leftSpace) - v.pad)
            v.setClosedInstant()
            runCatching { v.updatePivot() }
            applySize(Sug.infoOn() && !Sug.txt.isNullOrEmpty())
            if (Sug.infoOn() && !Sug.txt.isNullOrEmpty()) expand(true)
            else { armInfoClock(); scheduleBubbleHide() }
        }

        private fun scheduleCollapse() {
            cancelTimers()
            // 从「信息阶段起点」算，而不是「从现在再等 infoMs」：
            // 这样信息收起与气泡消失是同一个刻度（两个滑杆设成一样时同时消失）
            val left = (shownAt + infoMs() - SystemClock.uptimeMillis()).coerceAtLeast(0L)
            val r = Runnable { SLog.i("Bub: 超时 → 收起信息"); expand(false) }
            collapseRunnable = r
            main.postDelayed(r, left)
        }

        private fun scheduleBubbleHide() {
            cancelTimers()
            if (persistOn()) { SLog.i("Bub: 常驻 → 不自动消失"); return }
            // 气泡总寿命：与「信息显示时间」同一时刻起算。
            // 有文字 → 至少活到信息收完那一刻（取两者较大值）；
            // 没文字 → 就按「气泡停留时间」。
            val total = if (hasText()) maxOf(infoMs(), bubbleMs()) else bubbleMs()
            val left = (shownAt + total - SystemClock.uptimeMillis()).coerceAtLeast(0L)
            val r = Runnable {
                SLog.i("Bub: 超时 → 气泡消失")
                destroy()
            }
            hideRunnable = r
            SLog.i("Bub: 气泡计时 total=" + total + " left=" + left + " 有文字=" + hasText())
            main.postDelayed(r, left)
        }

        /** 有没有要显示的信息文字（决定气泡要不要活到信息结束） */
        private fun hasText(): Boolean = Sug.infoOn() && !Sug.txt.isNullOrEmpty()

        /** 信息阶段开始：把计时起点对齐到「文字开始显示」这一刻 */
        private fun armInfoClock() {
            shownAt = SystemClock.uptimeMillis()
        }

        private fun cancelTimers() {
            hideRunnable?.let { main.removeCallbacks(it) }
            collapseRunnable?.let { main.removeCallbacks(it) }
            collapseStep?.let { main.removeCallbacks(it) }
            hideRunnable = null
            collapseRunnable = null
            collapseStep = null
        }

        // ---------- 手势 ----------

        private fun onTap() {
            SLog.i("Bub: 点击 → Sug.fire() pkg=" + Sug.pkg)
            annotateCur()
            runCatching { Sug.fire() }
            ring.clear(); cur = 0; lastShowAt = 0L
        }

        private fun toggleInfo(vxPx: Float = 0f) {
            val v = view ?: return
            val nowOpen = v.isOpen()
            SLog.i("Bub: toggleInfo open=" + nowOpen)
            view?.dropPreviewState()      // 清掉可能的预览残留（否则会互相打架）
            val nowMs = android.os.SystemClock.uptimeMillis()
            if (nowMs - lastToggleAt < 150L) return
            lastToggleAt = nowMs
            haptic()                                  // 手势反馈先给，别让横滑"像死了一样"
            if (!Sug.infoOn()) {
                // 信息气泡总开关关着 → 没有内容可展开，但也给一个轻微脉搏当反馈
                view?.popFromCenter()
                SLog.i("Bub: toggleInfo 跳过，信息气泡开关=off")
                return
            }
            // 手指速度 → open 弹簧初始速度（进度/秒）：滑得越快弹得越狠（handfeel §2）
            val w = if (v.textW > 1f) v.textW else 1f
            val seed = (kotlin.math.abs(vxPx) / w).coerceIn(0.4f, 7f) *
                (if (vxPx >= 0f) 1f else -1f)
            SLog.i("Bub: toggle 速度 vx=" + vxPx.toInt() + "px/s seed=" + String.format("%.2f", seed))
            expand(!nowOpen, seed)
        }

        private fun switchItem(next: Boolean) {
            if (ring.size <= 1) return
            cur = if (next) (cur + 1) % ring.size else (cur - 1 + ring.size) % ring.size
            val s = ring[cur]
            SLog.i("Bub: 切换消息 cur=" + cur + "/" + ring.size + " txt=" + s.txt)
            haptic()
            Sug.pkg = s.pkg; Sug.txt = s.txt; Sug.bmp = s.bmp
            val v = view ?: return
            val oldExtra = v.curExtra()
            v.setItem(s.bmp, s.txt)
            if (screenW <= 0) screenW = ctx.resources.displayMetrics.widthPixels
            val leftSpace = circleLeft - v.pad - Ui.dp(ctx, 4)
            val rightSpace = screenW - (circleLeft + v.circle) - Ui.dp(ctx, 4)
            v.dir = if (rightSpace >= leftSpace) 1 else -1
            v.limitText((if (v.dir > 0) rightSpace else leftSpace) - v.pad)
            v.setShownInstant()
            val newExtra = v.curExtra()
            resizeWindow(maxOf(oldExtra, newExtra))
            v.animateExtra(oldExtra, newExtra, 300L)   // 长则延长、短则缩短（原地）
            if (newExtra < oldExtra) {
                val seq = ++resizeSeq
                main.postDelayed({ if (seq == resizeSeq) resizeWindow(v.curExtra()) }, 380L)
            }
            armInfoClock()   // 换了一条消息：重新从头计时
            scheduleCollapse()
        }

        private fun annotateCur() {
            val s = ring.getOrNull(cur) ?: return
            Sug.pkg = s.pkg; Sug.txt = s.txt; Sug.bmp = s.bmp
            Sug.kind = 2
            Sug.kChat = s.kChat; Sug.kUin = s.kUin; Sug.kType = s.kType
            Sug.kNi = s.kNi; Sug.kPi = s.kPi; Sug.kUid = s.kUid
        }

        private fun haptic() {
            runCatching {
                if (Cfg.vibMaster() && Cfg.vibOn()) {
                    Sug.vib(Cfg.vibLevel())
                    SLog.i("Bub: haptic lv=" + Cfg.vibLevel())
                }
            }
        }

        private fun savePos() {
            val p = lp ?: return
            val v = view ?: return
            runCatching {
                val snap = Ui.dp(ctx, 16)
                if (screenW > 0) {
                    val leftEdge = circleLeft - v.pad
                    val rightEdge = circleLeft + v.circle + v.pad
                    if (leftEdge <= snap) circleLeft = v.pad
                    else if (rightEdge >= screenW - snap) circleLeft = screenW - v.pad - v.circle
                    applyPosition()
                }
            }
            savePosAt(circleLeft - v.pad, p.y, ctx)
        }

        private fun moveBy(dx: Int, dy: Int) {
            val p = lp ?: return
            circleLeft += dx
            p.y += dy
            applyPosition()
        }

        // ---------- 自绘泡体 ----------

        class BubView(ctx: Context) : View(ctx) {

            var owner: Win? = null
            var pad = 0
            var circle = 0
            var dir = 1
            var baseX = 0

            private var avatar: Bitmap? = null
            private var text: String? = null
            private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            private val txtPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
            private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            private val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            private val box = RectF()
            private var layoutCache: StaticLayout? = null
            private var textAvail = 0
            private var lastSlot = 1
            private var cacheMsg1: String? = null
            private var cacheAvail1 = 0
            private var cacheLayout1: StaticLayout? = null
            private var cacheMsg2: String? = null
            private var cacheAvail2 = 0
            private var cacheLayout2: StaticLayout? = null

            private var open = 0f
            private var halo = 0f
            private var haloOn = false
            private var textAlpha = 0f
            private var anim: ValueAnimator? = null

            private var downX = 0f
            private var downY = 0f
            private var downT = 0L
            private var moved = false
            private var dragging = false
            private var lastRawX = 0f
            private var lastRawY = 0f
            private var curRawX = 0f
            private var curRawY = 0f
            private var arm: Runnable? = null
            private var wasOpenBeforeDrag = false
            private var pendingCollapse = false
            private var ignore = false

            /**
             * 双击关闭相关。
             *
             * 原来「下滑关闭」跟「上下滑切换消息」抢同一个手势（多条消息时永远走切换，
             * 想关关不掉），所以下滑关闭**整个去掉**，改成**双击关闭**。
             * 单击仍是打开小窗，但要等 DOUBLE_TAP_MS 确认「没有第二下」才执行，
             * 否则第一下就会把界面打开，第二下再关气泡就没意义了。
             */
            private var lastTapAt = 0L
            private var tapRunnable: Runnable? = null
            private var secondTap = false

            // 上下滑「跟手预览」：0 = 只看当前条，1 = 下一条完全显示
            private var prevP = 0f
            private var prevNext = false
            private var prevText: String? = null
            private var prevBmp: Bitmap? = null
            private val thMoved = Ui.dp(ctx, 18)
            private val thCancel = Ui.dp(ctx, 30)
            private val thSwipe = Ui.dp(ctx, 16)

            var textW = 0f
                private set

            init {
                bgPaint.color = 0xE6242424.toInt()
                txtPaint.color = Ui.adaptText(-1)
                txtPaint.textSize = Ui.dp(ctx, 15).toFloat()
                haloPaint.style = Paint.Style.STROKE
                haloPaint.strokeWidth = Ui.dp(ctx, 2).toFloat()
                haloPaint.color = 0x66FFFFFF
            }

            fun setItem(bmp: Bitmap?, msg: String?) {
                avatar = bmp
                text = msg
                layoutCache = null
                runCatching {
                    txtPaint.textSize = Ui.dp(context, if ((msg?.length ?: 0) > 7) 14 else 15).toFloat()
                }
                textW = if (msg.isNullOrEmpty()) 0f else {
                    val maxW = Ui.dp(context, 220).toFloat()
                    minOf(txtPaint.measureText(msg) + Ui.dp(context, 16), maxW)
                }
                runCatching { updatePivot() }
                invalidate()
            }

            fun limitText(maxPx: Int) {
                if (maxPx <= 0) { textW = 0f; return }
                val m = maxPx - Ui.dp(context, 8)
                if (textW > m) textW = m.coerceAtLeast(0).toFloat()
            }

            fun minW(): Int = circle + pad * 2
            fun minH(): Int = circle + pad * 2
            fun isOpen(): Boolean = open > 0.5f
            fun openFrac(): Float = open

            /** 手指上下滑动时：按进度 p（0~1）把相邻那条消息「逐渐」淡入/滑入 */
            fun setPreview(p: Float, next: Boolean) {
                if (p <= 0.02f) { clearPreview(); return }
                if (prevText == null || prevNext != next) {
                    prevNext = next
                    prevText = owner?.neighborTextPublic(next)
                    prevBmp = owner?.neighborBmpPublic(next)
                }
                if (prevText == null) { clearPreview(); return }
                // 收起状态（只有圆泡）时看不到文字，先展开，这样滑动过程中才能看到文字渐隐渐现
                if (open < 0.95f) {
                    wasCollapsed = true
                    owner?.expandForPreviewPublic()
                }
                prevP = p.coerceIn(0f, 1f)
                if ((dbgN++ % 8) == 0) {
                    SLog.i("Bub: 预览 p=" + String.format("%.2f", prevP) + " next=" + next +
                        " cur=" + (text ?: "-") + " 邻居=" + (prevText ?: "-") + " open=" + open)
                }
                invalidate()
            }

            private var wasCollapsed = false
            private var dbgN = 0
            // 手势速度（px/s，指数平滑），用于把"滑得快"喂给弹簧当初始速度
            private var velX = 0f
            private var lastVX = 0f
            private var lastVT = 0L

            /** 预览被打断（没切）时恢复原状 */
            fun restoreIfCollapsed() {
                if (!wasCollapsed) return
                wasCollapsed = false
                owner?.collapseAfterPreviewPublic()
            }

            /** 横滑展开/收起前调用：清掉竖滑预览状态，两边别打架 */
            fun dropPreviewState() {
                clearPreview()
                wasCollapsed = false
            }

            fun clearPreview() {
                if (prevP == 0f && prevText == null) return
                prevP = 0f
                prevText = null
                prevBmp = null
                invalidate()
            }

            /** 立即处于"已展开、文字全不透明"状态（原地更新时用，配合宽度动画） */
            fun setShownInstant() {
                releaseSpring()
                open = 1f
                textAlpha = 1f
                invalidate()
            }

            // ===== 弹簧动画（motion-web / handfeel.md §1 欠阻尼弹簧 + §2 速度耦合 + §4 次级动作）=====
            private var sx = -1f          // 当前"文字露出宽度"（弹簧位移）
            private var sv = 0f           // 速度
            private var sxTarget = -1f
            private var pulseX = 1f       // 整体缩放（果冻）
            private var pulseV = 0f
            private var lastT = 0L
            private var springOn = false
            private var oTarget = -1f     // open 的弹簧目标
            private var ov = 0f
            private var oFrame: (() -> Unit)? = null
            private var oEnd: (() -> Unit)? = null

            /** 系统关了动画（无障碍/开发者选项）时直接到位，别做弹簧 */
            private var animOk = -1
            private fun animationsEnabled(): Boolean {
                if (animOk < 0) {
                    animOk = runCatching {
                        val f = android.provider.Settings.Global.getFloat(
                            context.contentResolver,
                            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE
                        )
                        if (f <= 0.01f) 0 else 1
                    }.getOrDefault(1)
                }
                return animOk == 1
            }

            /** 来消息/切换时的"弹一下"：从圆心放大，欠阻尼回弹（替掉 Bar.jelly 的 5 关键帧 + alpha 闪） */
            fun popFromCenter() {
                alpha = 1f
                if (!animationsEnabled()) { pulseX = 1f; pulseV = 0f; return }
                pulseX = 0.62f
                pulseV = 3.6f
                springOn = true
                lastT = 0L
                invalidate()
            }

            /** 从 from 弹到 to（单位 = 像素宽度）；给一点初速度，起手就有劲 */
            /** 把宽度交还给 open 动画（展开/收起时用），清掉弹簧状态 */
            fun releaseSpring() {
                sx = -1f
                sv = 0f
                sxTarget = -1f
            }

            fun springExtraTo(from: Float, to: Float) {
                alpha = 1f                       // 防止任何残留 alpha=0 导致"看不见"
                if (!animationsEnabled()) { sx = to; sv = 0f; sxTarget = to; invalidate(); return }
                sx = from; sv = (to - from) * 2.2f; sxTarget = to
                springOn = true
                lastT = android.os.SystemClock.uptimeMillis()
                invalidate()
            }

            /** 新消息"从中心点弹一下"：给一个负位移 + 向上初速度，欠阻尼回 1 */
            fun pulseJelly() {
                pulseX = 0.86f
                pulseV = 2.6f
                springOn = true
                lastT = android.os.SystemClock.uptimeMillis()
                invalidate()
            }

            private fun springActive(): Boolean =
                kotlin.math.abs(sv) > 0.6f || kotlin.math.abs(sxTarget - sx) > 0.6f ||
                kotlin.math.abs(pulseV) > 0.004f || kotlin.math.abs(1f - pulseX) > 0.004f ||
                oTarget >= 0f

            /** 每帧推进；dt 夹在 1/120~1/30，保证 30/60/120Hz 手感一致（handfeel §7.1） */
            private fun stepSprings(): Boolean {
                val now = android.os.SystemClock.uptimeMillis()
                var dt = if (lastT == 0L) 1f / 60f else (now - lastT) / 1000f
                lastT = now
                dt = dt.coerceIn(1f / 120f, 1f / 30f)
                if (sx < 0f) { sx = sxTarget; sv = 0f }
                // 宽度：stiffness 90 / 每帧阻尼 0.90（≈果冻感）
                sv += (sxTarget - sx) * 90f * dt
                sv *= Math.pow(0.90f.toDouble(), (dt * 60f).toDouble()).toFloat()
                sx += sv * dt
                // 缩放：临界阻尼附近（k=200 的尺寸生长，handfeel §7.4）
                pulseV += (1f - pulseX) * 200f * dt
                pulseV *= Math.pow(0.86f.toDouble(), (dt * 60f).toDouble()).toFloat()
                pulseX += pulseV * dt
                // open 弹簧：展开 0→1 / 收起 1→0（文字透明度跟着 open）
                if (oTarget >= 0f) {
                    ov += (oTarget - open) * 95f * dt
                    ov *= Math.pow(0.87f.toDouble(), (dt * 60f).toDouble()).toFloat()
                    open = (open + ov * dt).coerceIn(0f, 1.12f)
                    textAlpha = ((open - 0.30f) / 0.55f).coerceIn(0f, 1f)
                    oFrame?.invoke()
                    if (kotlin.math.abs(oTarget - open) < 0.004f && kotlin.math.abs(ov) < 0.03f) {
                        open = oTarget
                        ov = 0f
                        oTarget = -1f
                        textAlpha = if (open > 0.5f) 1f else 0f
                        val e = oEnd; oEnd = null; oFrame = null
                        e?.invoke()
                    }
                }
                // ★兜底：任何 NaN/离谱值都可能让整帧画不出来（"动效消失"的元凶之一）
                if (!sx.isFinite() || sx > 20000f || sx < -1f) { sx = sxTarget.coerceAtLeast(0f); sv = 0f }
                if (!sv.isFinite()) sv = 0f
                if (!pulseX.isFinite() || pulseX < 0.2f || pulseX > 3f) { pulseX = 1f; pulseV = 0f }
                return springActive()
            }

            /** 当前"文字露出宽度" */
            fun curExtra(): Float = textW * open

            /** 原地延长/缩短：只动画绘制宽度，窗口尺寸另外调（更省） */
            fun animateExtra(from: Float, to: Float, dur: Long) {
                springExtraTo(from, to)
            }

            fun setOpenFrac(f: Float) {
                releaseSpring()
                open = f.coerceIn(0f, 1f)
                textAlpha = ((open - 0.35f) / 0.65f).coerceIn(0f, 1f)
                invalidate()
            }

            fun setClosedInstant() {
                releaseSpring()
                anim?.cancel(); anim = null
                open = 0f; textAlpha = 0f
                invalidate()
            }

            /** 缩放枢轴钉在圆心（否则按下/放大时整个泡体朝窗口中心收一下） */
            fun updatePivot() {
                pivotX = if (dir > 0) (pad + circle / 2f) else (width - pad - circle / 2f)
                pivotY = height / 2f
            }

            override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
                super.onSizeChanged(w, h, ow, oh)
                updatePivot()
            }

            /** 气泡被销毁（或窗口被摘掉）：撤掉挂着的「单击打开」和长按拖动 */
            override fun onDetachedFromWindow() {
                tapRunnable?.let { removeCallbacks(it) }
                tapRunnable = null
                arm?.let { removeCallbacks(it) }
                arm = null
                lastTapAt = 0L
                secondTap = false
                super.onDetachedFromWindow()
            }

            fun startHalo(onEnd: () -> Unit) {
                haloOn = true; halo = 0f
                val a = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 720
                    interpolator = LinearInterpolator()
                    addUpdateListener { halo = it.animatedValue as Float; invalidate() }
                    doOnEnd { haloOn = false; halo = 0f; invalidate(); onEnd() }
                }
                anim?.cancel(); anim = a; a.start()
            }

            fun haloDone() { haloOn = false; halo = 0f; invalidate() }

            fun animateUpdate(onEnd: () -> Unit) {
                val a = ValueAnimator.ofFloat(1f, 0.88f, 1f).apply {
                    duration = 200
                    addUpdateListener {
                        scaleX = it.animatedValue as Float
                        scaleY = it.animatedValue as Float
                    }
                    doOnEnd { scaleX = 1f; scaleY = 1f; onEnd() }
                }
                anim?.cancel(); anim = a; a.start()
            }

            fun animateOpen(
                toOpen: Boolean,
                seedVel: Float = Float.NaN,
                onFrame: () -> Unit,
                onEnd: () -> Unit
            ) {
                anim?.cancel(); anim = null
                releaseSpring()          // 宽度弹簧让位：open 是宽度的唯一主人
                alpha = 1f
                if (!animationsEnabled()) {
                    open = if (toOpen) 1f else 0f
                    textAlpha = if (toOpen) 1f else 0f
                    onFrame(); onEnd(); return
                }
                val from = open
                val to = if (toOpen) 1f else 0f
                if (from == to) { textAlpha = to; onFrame(); onEnd(); return }
                // 弹簧驱动 open（handfeel §1）：起手给初速度，落点轻微过冲
                oTarget = to
                // 默认冲量与宽度弹簧一致；横滑时改用**手指速度**（越快越弹）
                ov = if (seedVel.isNaN()) (to - from) * 3.0f
                     else if (toOpen) kotlin.math.abs(seedVel) * 1.25f
                     else -kotlin.math.abs(seedVel) * 1.25f
                oFrame = onFrame
                oEnd = onEnd
                springOn = true
                lastT = 0L
                invalidate()
            }

            override fun onDraw(c: Canvas) {
                val active = stepSprings()          // 弹簧：每帧推进（帧率无关）
                val r = circle / 2f
                val cy = height / 2f
                val cx = if (dir > 0) pad + r else width - pad - r

                // handfeel §2 速度耦合 + §4 体积守恒挤压拉伸：延长的瞬间被"拉长"，停下回弹
                // 两个源分别定标再相加：宽度速度(px/s) + 展开进度速度(1/s)，避免展开时常驻在夹紧值
                val stretch = ((sv * 0.00042f) + (ov * 0.012f)).coerceIn(-0.045f, 0.045f)
                val baseSc = (1f + 0.14f * prevP) * pulseX

                val save0 = c.save()
                c.scale(baseSc * (1f + stretch), baseSc * (1f - stretch), cx, cy)

                if (prevP > 0f && prevText != null) {
                    val p = prevP
                    drawBubbleBg(c)
                    drawAvatar(c, avatar, 1f - p)      // 旧头像渐隐
                    drawAvatar(c, prevBmp, p)          // 新头像渐现
                    drawMsg(c, text, 1f - p)           // 旧文字渐隐
                    drawMsg(c, prevText, p)            // 新文字渐现
                } else {
                    drawBubbleBg(c)
                    drawAvatar(c, avatar, 1f)
                    drawMsg(c, text, 1f)
                }
                c.restoreToCount(save0)
                drawHalo(c)

                if (active) postInvalidateOnAnimation()
            }

            /** 只画气泡背景 */
            private fun drawBubbleBg(c: Canvas) {
                val r = circle / 2f
                val cy = height / 2f
                val cx = if (dir > 0) pad + r else width - pad - r
                val extra = if (sx >= 0f && open > 0.995f) sx else textW * open
                val bodyLeft = if (dir > 0) cx - r else cx - r - extra
                val bodyRight = if (dir > 0) cx + r + extra else cx + r
                box.set(bodyLeft, cy - r, bodyRight, cy + r)
                c.drawRoundRect(box, r, r, bgPaint)
            }

            private fun drawAvatar(c: Canvas, bmp: Bitmap?, alphaMul: Float) {
                if (bmp == null || alphaMul <= 0.01f) return
                val r = circle / 2f
                val cy = height / 2f
                val cx = if (dir > 0) pad + r else width - pad - r
                box.set(cx - r, cy - r, cx + r, cy + r)
                bmpPaint.alpha = (alphaMul * 255).toInt().coerceIn(0, 255)
                runCatching { c.drawBitmap(bmp, null, box, bmpPaint) }
                bmpPaint.alpha = 255
            }

            /** 圆心（view 坐标），给缩放动画当 pivot */
            fun centerX(): Float = if (dir > 0) pad + circle / 2f else width - pad - circle / 2f
            fun centerY(): Float = height / 2f

            /** 只画文字（带透明度，用于渐隐/渐现） */
            private fun drawMsg(c: Canvas, msg: String?, alphaMul: Float) {
                if (open <= 0.2f || msg.isNullOrEmpty() || alphaMul <= 0.01f) return
                val r = circle / 2f
                val cy = height / 2f
                val cx = if (dir > 0) pad + r else width - pad - r
                val extra = if (sx >= 0f && open > 0.995f) sx else textW * open
                val bodyLeft = if (dir > 0) cx - r else cx - r - extra
                val bodyRight = if (dir > 0) cx + r + extra else cx + r
                val avail = extra - Ui.dp(context, 12)
                if (avail <= Ui.dp(context, 8)) return
                val x = if (dir > 0) bodyRight - Ui.dp(context, 12) - avail
                else bodyLeft + Ui.dp(context, 12)
                drawTextLayout(c, msg, x, cy, avail.toInt(), 0f, alphaMul * textAlpha)
            }

            private fun drawHalo(c: Canvas) {
                if (!haloOn) return
                val r = circle / 2f
                val cy = height / 2f
                val cx = if (dir > 0) pad + r else width - pad - r
                box.set(cx - r - pad * 0.55f, cy - r - pad * 0.55f,
                    cx + r + pad * 0.55f, cy + r + pad * 0.55f)
                val head = -90f + halo * 360f
                val fade = 1f - halo * 0.55f
                haloPaint.alpha = (120 * fade).toInt(); c.drawArc(box, head, 90f, false, haloPaint)
                haloPaint.alpha = (80 * fade).toInt(); c.drawArc(box, head - 70f, 70f, false, haloPaint)
                haloPaint.alpha = (45 * fade).toInt(); c.drawArc(box, head - 60f, 60f, false, haloPaint)
            }

            private fun drawTextLayout(
                c: Canvas, msg: String, x: Float, cy: Float, avail: Int,
                dy: Float = 0f, alphaMul: Float = 1f
            ) {
                if (avail <= 0) return
                val a0 = txtPaint.alpha
                if (alphaMul < 1f) txtPaint.alpha = (a0 * alphaMul).toInt().coerceIn(0, 255)
                val layout = layoutFor(msg, avail)
                val save = c.save()
                c.translate(x, cy - layout.height / 2f + dy)
                layout.draw(c)
                c.restoreToCount(save)
                txtPaint.alpha = a0
            }

            /** 双槽布局缓存：命中必须**文字和宽度都一致**，否则会串（之前就是这里串了） */
            private fun layoutFor(msg: String, avail: Int): StaticLayout {
                cacheLayout1?.let { if (cacheMsg1 == msg && cacheAvail1 == avail) return it }
                cacheLayout2?.let { if (cacheMsg2 == msg && cacheAvail2 == avail) return it }
                val built = StaticLayout.Builder
                    .obtain(msg, 0, msg.length, txtPaint, avail)
                    .setMaxLines(2)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .setLineSpacing(0f, 1.05f)
                    .setIncludePad(false)
                    .build()
                if (lastSlot == 2) {
                    cacheMsg1 = msg; cacheAvail1 = avail; cacheLayout1 = built; lastSlot = 1
                } else {
                    cacheMsg2 = msg; cacheAvail2 = avail; cacheLayout2 = built; lastSlot = 2
                }
                return built
            }

            private fun hitVisible(x: Float, y: Float): Boolean {
                val r = circle / 2f
                val cy = height / 2f
                val cx = if (dir > 0) pad + r else width - pad - r
                val dx = x - cx
                val dy = y - cy
                val rr = r + pad * 0.8f
                if (dx * dx + dy * dy <= rr * rr) return true
                val extra = textW * open
                if (extra <= 0f) return false
                val left = if (dir > 0) cx - r else cx - r - extra
                val right = if (dir > 0) cx + r + extra else cx + r
                return x >= left && x <= right && kotlin.math.abs(dy) <= r
            }

            override fun onTouchEvent(e: MotionEvent): Boolean {
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        if (!hitVisible(e.x, e.y)) { ignore = true; return true }
                        ignore = false
                        downX = e.rawX; downY = e.rawY
                        curRawX = e.rawX; curRawY = e.rawY
                        velX = 0f; lastVX = e.rawX; lastVT = SystemClock.uptimeMillis()
                        downT = SystemClock.uptimeMillis()
                        moved = false; dragging = false
                        lastRawX = e.rawX; lastRawY = e.rawY
                        // 双击判定：这一下和上一拍的间隔够近 → 这是第二下，
                        // 立刻撤掉「单击打开」，抬手时直接关气泡
                        // ★ 3081：开关关掉时**完全不认双击** → 单击不用等 300ms
                        secondTap = Cfg.bubDblClose() &&
                            (lastTapAt != 0L) && (downT - lastTapAt <= DOUBLE_TAP_MS)
                        if (secondTap) {
                            tapRunnable?.let { removeCallbacks(it) }
                            tapRunnable = null
                            lastTapAt = 0L
                            SLog.i("Bub: 第二下 → 准备关闭气泡")
                        }
                        runCatching { Bar.press(this, true) }
                        val r = Runnable {
                            if (!dragging) {
                                // 长按要拖动了：撤掉可能挂着的「单击打开」
                                tapRunnable?.let { removeCallbacks(it) }
                                tapRunnable = null
                                lastTapAt = 0L
                                dragging = true
                                lastRawX = curRawX; lastRawY = curRawY
                                wasOpenBeforeDrag = isOpen()
                                owner?.cancelTimersPublic()
                                pendingCollapse = wasOpenBeforeDrag
                                owner?.hapticPublic()
                                runCatching { animate().scaleX(1.03f).scaleY(1.03f).setDuration(120).start() }
                                SLog.i("Bub: 进入可拖动（长按 500ms）open=" + wasOpenBeforeDrag)
                            }
                        }
                        arm = r
                        postDelayed(r, 500)
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (ignore) return true
                        run {
                            val nowV = SystemClock.uptimeMillis()
                            if (lastVT != 0L && nowV > lastVT) {
                                val inst = (e.rawX - lastVX) / (nowV - lastVT) * 1000f
                                velX = velX * 0.55f + inst * 0.45f
                            }
                            lastVX = e.rawX; lastVT = nowV
                        }
                        curRawX = e.rawX; curRawY = e.rawY
                        val dx0 = e.rawX - downX
                        val dy0 = e.rawY - downY
                        if (!moved && (kotlin.math.abs(dx0) > thMoved ||
                                kotlin.math.abs(dy0) > thMoved)) moved = true
                        if (!dragging) {
                            if (kotlin.math.abs(dx0) > thCancel ||
                                kotlin.math.abs(dy0) > thCancel) {
                                arm?.let { removeCallbacks(it) }
                                arm = null
                            }
                            // 竖向滑动 → 相邻消息跟手渐显（横向滑动是展开/收起，不预览）
                            val adx = kotlin.math.abs(dx0)
                            val ady = kotlin.math.abs(dy0)
                            if (ady > thSwipe && ady >= adx && (owner?.ringSizePublic() ?: 0) > 1) {
                                val commitPx = Ui.dp(context, 48).toFloat()
                                setPreview(ady / commitPx, dy0 < 0)
                            } else if (prevP > 0f && ady <= thSwipe) {
                                clearPreview()
                                owner?.restoreIfCollapsedPublic()
                            }
                        }
                        if (dragging) {
                            if (pendingCollapse) { pendingCollapse = false; owner?.collapseForDragPublic() }
                            val ddx = (e.rawX - lastRawX).toInt()
                            val ddy = (e.rawY - lastRawY).toInt()
                            lastRawX = e.rawX; lastRawY = e.rawY
                            if (ddx != 0 || ddy != 0) owner?.moveByPublic(ddx, ddy)
                        }
                        return true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        arm?.let { removeCallbacks(it) }
                        arm = null
                        if (ignore) { ignore = false; return true }
                        runCatching { Bar.press(this, false) }
                        if (dragging) {
                            dragging = false
                            pendingCollapse = false
                            owner?.cancelTimersPublic()
                            runCatching { animate().scaleX(1f).scaleY(1f).setDuration(120).start() }
                            owner?.savePosPublic()
                            SLog.i("Bub: 拖动结束 → 已保存位置 open=" + wasOpenBeforeDrag)
                            if (wasOpenBeforeDrag) owner?.showFixPublic() else owner?.scheduleHidePublic()
                            return true
                        }
                        val dx = e.rawX - downX
                        val dy = e.rawY - downY
                        val adx = kotlin.math.abs(dx)
                        val ady = kotlin.math.abs(dy)
                        SLog.i("Bub: touch up dx=" + adx.toInt() + " dy=" + dy.toInt() + " moved=" + moved)
                        clearPreview()
                        when {
                            // 下滑关闭已移除（跟「上下滑切换消息」抢手势，多条消息时永远关不掉）。
                            // 上下滑现在**只**负责切换上一条/下一条。
                            ady > thSwipe && ady >= adx -> owner?.switchItemPublic(dy < 0)
                            adx > thSwipe && adx > ady -> owner?.toggleInfoPublic(velX)
                            !moved && SystemClock.uptimeMillis() - downT < 600 -> {
                                if (secondTap) {
                                    // 双击 → 关闭气泡
                                    secondTap = false
                                    lastTapAt = 0L
                                    SLog.i("Bub: 双击 → 关闭气泡")
                                    owner?.hapticPublic()
                                    owner?.destroyPublic()
                                } else if (!Cfg.bubDblClose()) {
                                    // ★ 3081：关了「双击关闭」→ 单击**立刻**打开，不等那 300ms
                                    SLog.i("Bub: 单击 → 立刻打开（双击关闭已关）")
                                    owner?.onTapPublic()
                                } else {
                                    // 单击 → 延时确认没有第二下，再打开小窗
                                    lastTapAt = SystemClock.uptimeMillis()
                                    val tr = Runnable {
                                        tapRunnable = null
                                        lastTapAt = 0L
                                        owner?.onTapPublic()
                                    }
                                    tapRunnable = tr
                                    postDelayed(tr, DOUBLE_TAP_MS)
                                }
                            }
                        }
                        // 只有"既不是竖滑切换、也不是横滑展开"的动作才恢复预览状态，
                        // 否则会把刚刚启动的展开/收起动画立刻掐掉（横滑"偶尔没动效"就是这个）
                        val verticalGesture = ady > thSwipe && ady >= adx
                        val horizontalGesture = adx > thSwipe && adx > ady
                        secondTap = false    // 兜底：任何一条路径走完都别把「第二下」留到下次
                        if (!verticalGesture && !horizontalGesture) owner?.restoreIfCollapsedPublic()
                        return true
                    }
                }
                return true
            }
        }

        // ----- 供 BubView 回调（Kotlin private 内部类互访的公开桥） -----
        fun cancelTimersPublic() = cancelTimers()
        fun hapticPublic() = haptic()
        fun collapseForDragPublic() = collapseForDrag()
        fun moveByPublic(dx: Int, dy: Int) = moveBy(dx, dy)
        fun savePosPublic() = savePos()
        fun showFixPublic() = showFix()
        fun scheduleHidePublic() = scheduleBubbleHide()
        fun toggleInfoPublic(vx: Float) = toggleInfo(vx)
        fun switchItemPublic(next: Boolean) = switchItem(next)
        fun onTapPublic() = onTap()
        fun destroyPublic() = destroy()
        fun ringSizePublic(): Int = ring.size


        /** 上下滑预览时：把气泡展开（不带动画），好让文字参与渐隐渐现 */
        fun restoreIfCollapsedPublic() {
            view?.restoreIfCollapsed()
            // 预览过程中 cancelTimers() 过，这里补回收起定时器，否则会一直展开不收回
            armInfoClock()
            if (Sug.infoOn()) scheduleCollapse() else scheduleBubbleHide()
        }

        fun expandForPreviewPublic() {
            val v = view ?: return
            cancelTimers()
            v.setShownInstant()
            resizeWindow(v.curExtra())
        }

        /** 预览没切成时：回到收起态 */
        fun collapseAfterPreviewPublic() {
            val v = view ?: return
            armInfoClock()
            if (!Sug.infoOn()) { expand(false); return }
            expand(false)
        }

        /** 上下滑预览用：下一条/上一条的文本与头像 */
        fun neighborTextPublic(next: Boolean): String? {
            if (ring.size <= 1) return null
            val i = if (next) (cur + 1) % ring.size else (cur - 1 + ring.size) % ring.size
            return ring[i].txt
        }

        fun neighborBmpPublic(next: Boolean): Bitmap? {
            if (ring.size <= 1) return null
            val i = if (next) (cur + 1) % ring.size else (cur - 1 + ring.size) % ring.size
            return ring[i].bmp
        }
    }

    // ================= 配置界面 =================
    // 旧「消息提醒」页的 View 版本（ui()/sliderRow()）已随该页迁到 Compose 而删除；
    // 四个开关/滑杆现在由 Pages.kt 的 page 12 规格渲染，写入路径仍是 Cfg.save/saveStr。

    private fun ValueAnimator.doOnEnd(block: () -> Unit) {
        addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) = block()
        })
    }
}
