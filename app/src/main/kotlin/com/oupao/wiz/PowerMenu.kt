package com.oupao.wiz

import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView

/**
 * 长按电源键 → 关机界面里的「软重启」按钮。
 *
 * 系统实现  的做法：hook `OplusGlobalActionsDialog$ActionsDialog.onStart`，
 * 在电源菜单的 `mContainer` 里加一个自己的按钮。ColorOS 默认不显示「手动锁定」，
 * 那块 `OplusShutdownView.mLockRect` 就是截图红圈的空白位置；如果系统显示手动锁定，就不占用。
 *
 * 点击只发广播给模块 App，由 App 用 root 执行 `setprop ctl.restart zygote`
 * （SystemUI uid 1000 拿不到 KernelSU 的 su 授权）。
 */
object PowerMenu {

    private const val TAG = "oupao_soft_reboot_view"
    private const val CLASS_ACTIONS = "com.oplus.systemui.shutdown.OplusGlobalActionsDialog\$ActionsDialog"
    private const val CLASS_SHUTDOWN = "com.oplus.systemui.shutdown.OplusShutdownView"

    fun install(cl: ClassLoader?): Boolean {
        val cls = runCatching { Rt.cls(cl, CLASS_ACTIONS) }.getOrNull()
        if (cls == null) {
            SLog.i("PowerMenu: 找不到 ActionsDialog")
            return false
        }
        val m = runCatching {
            cls.getDeclaredMethod("onStart").also { it.isAccessible = true }
        }.getOrNull()
        if (m == null) {
            SLog.i("PowerMenu: ActionsDialog 没有 onStart")
            return false
        }
        val ok = XBridge.hookMethod(m, object : XMH() {
            override fun afterHookedMethod(param: XMP) = addButton(param.thisObject)
        }) != null
        SLog.i("PowerMenu: 软重启按钮 hook=" + ok)
        return ok
    }

    private fun addButton(dialog: Any?) {
        if (!Cfg.powerSoftReboot()) {
            SLog.i("PowerMenu: onStart 但开关=false")
            return
        }
        SLog.i("PowerMenu: onStart obj=" + dialog?.javaClass?.name)
        // 实测系统：从 ActionsDialog 的 Window 拿 decorView，加到整层菜单上；
        // mContainer 只作为兜底（有些 ROM 里 OplusShutdownView 不在 mContainer 子树里）。
        val dlg = dialog as? android.app.Dialog
        val root = dlg?.window?.decorView as? ViewGroup
            ?: Rt.get(dialog, "mContainer") as? ViewGroup
            ?: run {
                SLog.i("PowerMenu: onStart 没有 decorView/mContainer")
                return
            }
        SLog.i("PowerMenu: root=" + root.javaClass.name + " children=" + root.childCount)
        root.post { addButtonNow(root, 0) }
    }

    private fun addButtonNow(container: ViewGroup, attempt: Int) {
        if (!Cfg.powerSoftReboot()) return
        val shutdown = findShutdown(container) ?: staticShutdown()
        val rect = shutdown?.let { Rt.get(it, "mLockRect") as? Rect }
        SLog.i("PowerMenu: addNow attempt=" + attempt + " shutdown=" + (shutdown != null) +
            " size=" + (shutdown?.width ?: -1) + "x" + (shutdown?.height ?: -1) + " rect=" + rect)
        // onStart 时可能还没量完；稍等一帧再试一次
        if (shutdown == null || shutdown.width <= 0 || shutdown.height <= 0) {
            if (attempt < 3) container.postDelayed({ addButtonNow(container, attempt + 1) }, 120L)
            return
        }
        if (Rt.get(shutdown, "mManuallyLockEnable") == true) {
            SLog.i("PowerMenu: 手动锁定已显示，跳过软重启按钮")
            return
        }

        // 同一个对话框重建时先清掉旧按钮
        for (i in container.childCount - 1 downTo 0) {
            val child = container.getChildAt(i)
            if ((child?.tag as? String) == TAG) container.removeViewAt(i)
        }

        val d = container.resources.displayMetrics.density
        val size = (52f * d).toInt()

        // 3122：按 ROM 自己画出来的几何位置摆，不再拿 OplusShutdownView 的宽高猜。
        // mBarRectF = 中间胶囊，mEmergencyBarRectF = 底部「紧急求助」圆钮；
        // 关机键就是胶囊底部那个白色圆钮，圆心 = bar.bottom - barWidth/2。
        // 目标位置：屏幕水平中心，垂直取「关机键圆心」和「紧急求助圆心」的中点。
        val bar = Rt.get(shutdown, "mBarRectF") as? RectF
        val emergency = Rt.get(shutdown, "mEmergencyBarRectF") as? RectF
        val hasBar = bar != null && bar.width() > 0f && bar.height() > 0f
        val hasEmergency = emergency != null && emergency.width() > 0f && emergency.height() > 0f
        if ((!hasBar || !hasEmergency) && attempt < 4) {
            container.postDelayed({ addButtonNow(container, attempt + 1) }, 120L)
            return
        }

        val tv = TextView(container.context)
        tv.text = "软重启"
        tv.contentDescription = "软重启"
        tv.setTextColor(Color.WHITE)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        tv.gravity = Gravity.CENTER
        tv.includeFontPadding = false
        val bg = GradientDrawable()
        bg.shape = GradientDrawable.OVAL
        bg.setColor(0x18FFFFFF)
        bg.setStroke((1.2f * d).toInt(), 0x66FFFFFF)
        tv.background = bg
        tv.tag = TAG
        tv.setOnClickListener { SoftReboot.request(container.context) }
        val lp = if (container is FrameLayout)
            FrameLayout.LayoutParams(size, size)
        else
            ViewGroup.LayoutParams(size, size)
        container.addView(tv, lp)

        tv.post {
            val sl = IntArray(2)
            val pl = IntArray(2)
            shutdown.getLocationInWindow(sl)
            container.getLocationInWindow(pl)
            val barWidth = (Rt.get(shutdown, "mBarWidth") as? Int)
                ?.takeIf { it > 0 } ?: (bar?.width()?.toInt() ?: shutdown.width)
            // 关机键图标中心比胶囊底部再高约 16dp；紧急求助圆心直接读 ROM 的矩形。
            val powerCy = if (hasBar) bar!!.bottom - barWidth / 2f + 16f * d else shutdown.height * 0.50f
            val emergencyCy = if (hasEmergency) emergency!!.centerY() else shutdown.height * 0.887f
            // 左右以屏幕宽度为准（OplusShutdownView 自身在窗口里可能带 61px 偏移，
            // 用它的宽高算会整体偏右；实测用户截图就是这个问题）。
            val targetX = pl[0] + container.resources.displayMetrics.widthPixels / 2f
            val targetY = if (hasBar && hasEmergency) {
                sl[1] + (powerCy + emergencyCy) / 2f
            } else {
                pl[1] + container.height * 0.694f
            }
            tv.x = targetX - pl[0] - size / 2f
            tv.y = targetY - pl[1] - size / 2f
            SLog.i(
                "PowerMenu: 软重启按钮已加 target=" + targetX + "," + targetY +
                    " bar=" + bar + " emergency=" + emergency
            )
        }
    }

    /** 兜底：OplusGlobalActionsDialog 里有个静态 mOplusShutdownView */
    private fun staticShutdown(): View? = runCatching {
        val c = Class.forName("com.oplus.systemui.shutdown.OplusGlobalActionsDialog")
        val f = c.getDeclaredField("mOplusShutdownView")
        f.isAccessible = true
        f.get(null) as? View
    }.getOrNull()

    /** mContainer 里找 OplusShutdownView（避免直接依赖 outer dialog 的字段名） */
    private fun findShutdown(vg: ViewGroup): View? {
        for (i in 0 until vg.childCount) {
            val child = vg.getChildAt(i) ?: continue
            if (child.javaClass.name == CLASS_SHUTDOWN) return child
            if (child is ViewGroup) findShutdown(child)?.let { return it }
        }
        return null
    }
}
