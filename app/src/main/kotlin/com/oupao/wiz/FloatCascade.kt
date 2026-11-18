package com.oupao.wiz

import android.graphics.Rect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 小窗「叠放自动错开」（FlymeUI 页 → `oupao_zoom_cascade`，配合「限制小窗数量」用）。
 *
 * ## 为什么要它
 * ColorOS 的窗口布局策略只处理**横向**让位；上限调到 4 个以上时，新的小窗顶边常常落在
 * 和已有小窗一样的高度上，看起来就是「全叠在一处」。这里在布局算完之后补一次**纵向**错开。
 *
 * ## 规则（本模块自己的简单策略，本模块自己的简单策略）
 * ```
 * step = 窗口高度的 12%（夹在 60~240px 之间）
 * 从当前 top 开始，最多试 12 次：
 *     如果有「横向和本窗重叠」的小窗，其 top 与当前 top 的距离 < step/2 → 往下挪一个 step
 *     否则就停
 * 结果必须落在 [状态栏下沿, 还能完整放下本窗的最低位置] 之内，否则**保持原样不动**
 * ```
 *
 * ## 安全阀
 * - 只有「限制小窗数量」开着时才生效（`zoomMaxOn()`），关着时一个像素都不改；
 * - 另有独立开关 `oupao_zoom_cascade`，觉得位置怪可以单独关掉；
 * - 任何一步读不到（参数/字段不对）就直接返回，等于原厂行为；
 * - 日志 `XZcasc`（限流 3s）能看到挪了多少。
 */
object FloatCascade {

    private const val TAG = "XZcasc"

    private const val CLS = "com.android.server.wm.OplusFlexibleTaskLayoutPolicy"

    /** 每次往下挪的比例 / 上下限 / 最多试几次 */
    private const val STEP_RATIO = 0.12f
    private const val STEP_MIN_PX = 60
    private const val STEP_MAX_PX = 240
    private const val MAX_TRIES = 12

    @JvmStatic
    fun install(cl: ClassLoader?): Boolean {
        val k = runCatching { Rt.cls(cl, CLS) }.getOrNull()
        if (k == null) {
            SLog.i("FloatCascade: 类未找到 $CLS")
            return false
        }
        val intT = Integer.TYPE
        val floatT = Float::class.javaPrimitiveType!!
        val m = k.declaredMethods.firstOrNull {
            it.name == "avoidOverlappingAreas" && it.parameterCount == 5 &&
                it.parameterTypes[0] == Rect::class.java &&
                java.util.List::class.java.isAssignableFrom(it.parameterTypes[1]) &&
                it.parameterTypes[2] == intT && it.parameterTypes[3] == floatT &&
                it.parameterTypes[4] == intT
        }
        if (m == null) {
            SLog.i("FloatCascade: 没找到 avoidOverlappingAreas(Rect,List,int,float,int)")
            return false
        }
        runCatching { m.isAccessible = true }
        val cb = object : XMH() {
            override fun afterHookedMethod(param: XMP) = cascade(param)
        }
        val ok = XBridge.hookMethod(m, cb) != null
        SLog.i("FloatCascade: avoidOverlappingAreas 挂上=$ok")
        return ok
    }

    private fun field(o: Any, name: String): Any? = runCatching {
        o.javaClass.getDeclaredField(name).also { it.isAccessible = true }.get(o)
    }.getOrNull()

    private fun cascade(param: XMP) {
        try {
            if (!Cfg.zoomMaxOn() || !Cfg.zoomCascade()) return
            // 3240：上限只有 1 个时**不做错开** —— 单个小窗应该居中，
            //   错开逻辑只对"同时存在多个小窗"有意义。
            if (Cfg.zoomMax() <= 1) return
            val self = param.thisObject ?: return
            val rect = param.argOrNull(0) as? Rect ?: return
            if (rect.isEmpty) return
            val others = param.argOrNull(1) as? List<*> ?: return
            if (others.isEmpty()) return
            val scale = param.argFloat(3).let { if (it.isFinite() && it > 0f) it else 1f }

            val win = field(self, "mWindowBounds") as? Rect ?: return
            if (win.isEmpty) return
            val status = field(self, "mStatusBarFrame") as? Rect ?: Rect()
            val navi = field(self, "mNaviBarFrame") as? Rect ?: Rect()

            val h = (rect.height() * scale).roundToInt()
            if (h <= 0) return
            val step = (h * STEP_RATIO).roundToInt().coerceIn(STEP_MIN_PX, STEP_MAX_PX)
            val topMin = max(win.top, status.bottom)
            var bottom = win.bottom
            // 底部导航栏占了整行时，最低位置要抬到导航栏之上
            if (!navi.isEmpty && navi.width() >= win.width() / 2 && navi.bottom >= win.bottom) {
                bottom = min(bottom, navi.top)
            }
            val topMax = bottom - h
            if (topMax < topMin) return

            // 只和「横向确实重叠」的小窗比纵向位置
            val right = rect.left + (rect.width() * scale).roundToInt()
            val overlaps = ArrayList<Rect>()
            for (o in others) {
                val r = o as? Rect ?: continue
                if (r.isEmpty) continue
                if (rect.left < r.right && right > r.left) overlaps.add(r)
            }
            if (overlaps.isEmpty()) return

            val origTop = rect.top
            var top = origTop
            var moved = 0
            var tries = 0
            while (tries < MAX_TRIES) {
                var conflict = false
                for (r in overlaps) {
                    if (abs(r.top - top) < max(1, step / 2)) {
                        conflict = true
                        break
                    }
                }
                if (!conflict) break
                top += step
                if (top > topMax) return          // 放不下了 → 原样不动，交回原厂
                tries++
                moved++
            }
            if (moved > 0 && top != origTop) {
                rect.offsetTo(rect.left, top)
                SLog.r(TAG, "cascade $origTop -> $top (step=$step n=${overlaps.size})", 3000L)
            }
        } catch (t: Throwable) {
            SLog.r(TAG, "cascade error: $t", 3000L)
        }
    }
}
