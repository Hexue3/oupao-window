package com.oupao.wiz

import android.content.Context
import android.util.Log
import android.view.MotionEvent

/**
 * SystemUI 侧：**旁听**系统侧滑/角落手势的触摸流，驱动自绘扇形菜单。
 *
 * 为什么不自己建窗口抢触摸（2734~2747 的教训）：
 *  - 屏幕最外侧那圈被 ColorOS 自己的角落手势先吃掉，我们的角落窗口只有在靠内一点才拿得到 DOWN，
 *    参数一调严就"完全呼不出来"；
 *  - 消费型窗口还会把点击吃掉，得靠注入重放兜底（时序脆弱）。
 *
 * 现在改成：hook `com.oplus.systemui...sidegesture.SideGestureDetector`，**只读不拦**
 * （绝不 `returnEarly` / 绝不改返回值），把事件交给 [FlyFanH] 画菜单。
 * 点击完全不受影响（我们没干预系统分发），起手位置也不受角落手势区限制。
 *
 * 注意：2712 那次把 `onMotionEventImpl` 用 `returnEarly` 顶掉，结果连纯 tap 都废了；
 * 这里只做"旁听"，绝不动返回值和参数。
 */
object FlySideHook {

    private const val TAG = "OupaoFlySide"

    private const val CLS = "com.oplus.systemui.navigationbar.gesture.sidegesture.SideGestureDetector"

    @Volatile
    private var done = false

    @Volatile
    private var lastLog = 0L

    @Volatile
    private var downIn = false

    @JvmStatic
    fun install(cl: ClassLoader?) {
        if (done) return
        val loader = cl ?: runCatching { FlySideHook::class.java.classLoader }.getOrNull()
        val cls = runCatching { Class.forName(CLS, false, loader) }.getOrNull()
        if (cls == null) {
            log("找不到 SideGestureDetector，跳过")
            return
        }
        var n = 0
        for (m in cls.declaredMethods) {
            val ps = m.parameterTypes
            if (ps.isEmpty() || ps[0] != MotionEvent::class.java) continue
            if (XBridge.hookMethod(m, Watch()) != null) n++
        }
        done = n > 0
        log("旁听 hook=${n} 个方法（类=" + cls.simpleName + "）")
    }

    private class Watch : XMH() {
        override fun afterHookedMethod(param: XMP) {
            try {
                val ev = param.args.getOrNull(0) as? MotionEvent ?: return
                val ctx = param.thisObject as? Context
                feed(ev, ctx)
            } catch (_: Throwable) {
            }
        }
    }

    private fun feed(ev: MotionEvent, ctx: Context?) {
        // 2912：拿到 SystemUI 的 Context 就把「小白条隐藏」广播注册上（只注册一次）
        runCatching { NavPill.registerWith(ctx) }
        val x = ev.rawX
        val y = ev.rawY
        // 3436 探针：SystemUI 侧这条手势流收到了哪些 action（桌面上 UP 丢失时对照用）
        logOnce("ev a=" + ev.actionMasked + " x=" + x.toInt() + " y=" + y.toInt() + " in=" + downIn)
        val lr = FlyState.lr()
        val r = FlyState.radius()
        if (lr == 0 || r <= 0) return
        val dm = ctx?.resources?.displayMetrics
        val w = dm?.widthPixels?.toFloat() ?: return
        val h = dm?.heightPixels?.toFloat() ?: return
        val side = FlyGeom.side(x, y, w, h, r.toFloat(), lr)
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downIn = side != FlyGeom.SIDE_NONE
                if (downIn) {
                    FlyFanH.show(ctx, side)
                    FlyFanH.move(x, y, false)
                    logOnce("down x=" + x.toInt() + " y=" + y.toInt() + " side=" + side)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (downIn && FlyFanH.alive()) FlyFanH.move(x, y, FlyFanH.progressOf(x, y) >= 0.999f)
            }
            MotionEvent.ACTION_UP -> {
                if (downIn && FlyFanH.alive()) {
                    // 3436：和 FlyFanBr 的 UP 分支对齐 ——
                    //   划满且选中了 → 启动；否则按「未选应用时松手收起」开关决定收起还是停留。
                    //   以前这里只要 p>0.15 就 release()（等于永远不进入"停留"），
                    //   于是这条路径呼出的菜单既不会停留、也进不了"多任务/通知中心自动收起"。
                    if (FlyFanH.progressOf(x, y) >= 0.999f && FlyFanH.sel() >= 0) {
                        FlyFanH.release()
                    } else if (FlyFanH.staying()) {
                        // 3438：已经停住 → 这一下是点击，别再 stay()（会把选中重置）
                    } else {
                        val stayOff = runCatching { Cfg.flyStayOff() }.getOrDefault(false)
                        if (stayOff) FlyFanH.hide() else FlyFanH.stay()
                    }
                }
                downIn = false
            }
            MotionEvent.ACTION_CANCEL -> {
                downIn = false
                FlyFanH.hide()
            }
        }
    }

    private fun logOnce(msg: String) {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastLog < 300L) return
        lastLog = now
        log(msg)
    }

    private fun log(msg: String) {
        try {
            Log.i(TAG, msg)
        } catch (_: Throwable) {
        }
    }
}
