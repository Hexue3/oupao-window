package com.oupao.wiz

import android.content.Context
import android.view.View

/**
 * 3430：**SystemUI 侧的面板展开信号**（给扇形菜单「停留中自动收起」用）。
 *
 * 需求：扇形菜单停在原地等选择时，如果用户去拉通知中心 / 控制中心，菜单要自己收起。
 * 但 `FlyFanH` 跑在 system_server，看不到 SystemUI 的展开状态 —— 所以在这一侧
 * hook `NotificationPanelViewController.setExpandedFraction(float)`（AOSP 公开方法，
 * C16/C17 都存在），把「是否已经拉开」写进 Settings.Global 的 `oupao_shade_open`，
 * 由 system_server 轮询。
 *
 * 只写标志，**不改任何参数/返回值**（这个类里的 hook 全部是 after 旁听）。
 */
object ShadeWatch {

    private const val TAG = "OupaoShade"
    private const val PANEL = "com.android.systemui.shade.NotificationPanelViewController"
    private const val KEY = "oupao_shade_open"

    /** 面板拉开超过这个比例就当"已打开"（拉一点点不算） */
    private const val OPEN_AT = 0.12f

    @Volatile
    private var done = false

    @Volatile
    private var lastVal = -1

    @JvmStatic
    fun install(cl: ClassLoader?): Int {
        if (done) return 0
        val loader = cl ?: runCatching { ShadeWatch::class.java.classLoader }.getOrNull()
        val cls = runCatching { Class.forName(PANEL, false, loader) }.getOrNull()
        if (cls == null) {
            SLog.i("$TAG: 找不到 $PANEL，跳过")
            return 0
        }
        var n = 0
        runCatching {
            val m = cls.getDeclaredMethod("setExpandedFraction", Float::class.javaPrimitiveType)
            runCatching { m.isAccessible = true }
            if (XBridge.hookMethod(m, H()) != null) n++
        }.onFailure { SLog.e("$TAG: hook setExpandedFraction 失败", it) }

        // 3439：**不再挂 setExpandedHeight**。
        //   两个写入方会互相打架：setExpandedFraction 判"已打开(0.28)"的同时，
        //   setExpandedHeight 可能带着很小的像素高度回调 → 把标志位又写回 0，
        //   于是 system_server 侧看门狗读到的永远是 0（用户："连续性测试根本不会关菜单"）。
        //   现在只留 setExpandedFraction 这一个权威写入方；控制中心那条由
        //   `FlyFanH` 的"焦点窗口标题 = NotificationShade"判据兜底（控制中心不走这个回调）。

        done = n > 0
        SLog.i("$TAG: 面板展开监听 挂上=$n")
        return n
    }

    private class H : XMH() {
        override fun afterHookedMethod(param: XMP) {
            runCatching {
                val f = param.args.getOrNull(0) as? Float ?: return
                put(param.thisObject, f)
            }
        }
    }

    private fun put(target: Any?, frac: Float) {
        val ctx = if (target is Context) target else ctxOf(target)
        put(ctx, frac)
    }

    private fun put(ctx: Context?, frac: Float) {
        if (ctx == null) return
        val on = if (frac >= OPEN_AT) 1 else 0
        if (on == lastVal) return
        lastVal = on
        Cfg.gsetC(ctx, KEY, if (on == 1) "1" else "0")
        SLog.i("$TAG: 面板 ${if (on == 1) "已打开" else "已收起"} frac=" + frac)
    }

    private fun ctxOf(o: Any?): Context? {
        if (o is Context) return o
        if (o is View) return o.context
        val v = runCatching { Rt.get(o, "mView") }.getOrNull()
        if (v is View) return v.context
        return runCatching { Ctx.get() }.getOrNull() ?: runCatching { Cfg.sysContext() }.getOrNull()
    }
}
