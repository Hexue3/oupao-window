package com.oupao.wiz

/**
 * 扇形菜单的**几何与判定**——三个进程共用的一份真理。
 *
 * 为什么要有这个文件：热区判定原来在三个地方各写了一遍（system_server 的 `FlyH`、
 * Launcher 的 `FlyGate`、SystemUI 的热区窗口），阈值/半径/圆判定各写各的，
 * 于是"SystemUI 认为在手势内、system_server 认为没到线"这类不一致几乎必然发生。
 *
 * 这里的判定阈值与取消逻辑来自对 ColorOS 实际手势行为的实测校准：
 *
 *  - **两轴阈值**，必须同时满足才算手势：
 *      向内 ≥ max(1.75 × touchSlop, 14dp)
 *      向上 ≥ max(0.5  × touchSlop,  4dp)
 *    → 正常点击（手指抖几 px）永远过不了线 → 点击不会被菜单抢走。
 *  - 反向超过 max(touchSlop, 8dp) 直接取消（防"先划出去又划回来"误判）。
 *  - 热区 = 以屏幕左下/右下角为圆心、半径 `radius` 的圆。
 *
 * 本文件**不依赖任何 Android 类**，方便三个进程复用（也能单元测试）。
 */
object FlyGeom {

    /** 1 = 左下，2 = 右下，0 = 不在热区 */
    const val SIDE_NONE = 0
    const val SIDE_LEFT = 1
    const val SIDE_RIGHT = 2

    private const val INWARD_SLOP_MUL = 1.75f
    private const val UPWARD_SLOP_MUL = 0.50f
    private const val INWARD_DP = 14f
    private const val UPWARD_DP = 4f
    private const val REVERSE_TOL_DP = 8f

    /** 向内位移阈值（px） */
    @JvmStatic
    fun inwardThreshold(slopPx: Float, density: Float): Float {
        val d = if (density.isFinite() && density > 0f) density else 1f
        return Math.max(slopPx * INWARD_SLOP_MUL, d * INWARD_DP)
    }

    /** 向上位移阈值（px） */
    @JvmStatic
    fun upwardThreshold(slopPx: Float, density: Float): Float {
        val d = if (density.isFinite() && density > 0f) density else 1f
        return Math.max(slopPx * UPWARD_SLOP_MUL, d * UPWARD_DP)
    }

    /** 反向容忍（px） */
    @JvmStatic
    fun reverseTolerance(slopPx: Float, density: Float): Float {
        val d = if (density.isFinite() && density > 0f) density else 1f
        return Math.max(slopPx, d * REVERSE_TOL_DP)
    }

    /**
     * 这个点落在哪个角落热区里。
     *
     * @param lr 热区开关位：1=左下 2=右下（3=两个都开）、0=关
     */
    @JvmStatic
    fun side(
        x: Float,
        y: Float,
        screenW: Float,
        screenH: Float,
        radius: Float,
        lr: Int,
    ): Int {
        if (lr == 0 || radius <= 0f || screenW <= 0f || screenH <= 0f) return SIDE_NONE
        val dy = screenH - y
        if (dy < 0f) return SIDE_NONE                 // 屏幕外下方不管
        val r2 = radius * radius
        if ((lr and 1) != 0) {
            if (x * x + dy * dy <= r2) return SIDE_LEFT
        }
        if ((lr and 2) != 0) {
            val dx = screenW - x
            if (dx * dx + dy * dy <= r2) return SIDE_RIGHT
        }
        return SIDE_NONE
    }

    /**
     * 从 (ox,oy) 划到 (x,y) 是否已经够"像一个角落手势"（两轴同时达标）。
     *
     * @param side [side] 返回的边
     */
    @JvmStatic
    fun claimed(
        side: Int,
        ox: Float,
        oy: Float,
        x: Float,
        y: Float,
        slopPx: Float,
        density: Float,
    ): Boolean {
        if (side == SIDE_NONE) return false
        val inward = if (side == SIDE_LEFT) (x - ox) else (ox - x)
        val upward = oy - y
        val rev = reverseTolerance(slopPx, density)
        if (inward < -rev || upward < -rev) return false
        return inward >= inwardThreshold(slopPx, density) &&
            upward >= upwardThreshold(slopPx, density)
    }

    /** 这条位移是否已经"反向"到该取消的程度 */
    @JvmStatic
    fun reversed(side: Int, ox: Float, oy: Float, x: Float, y: Float, slopPx: Float, density: Float): Boolean {
        if (side == SIDE_NONE) return false
        val inward = if (side == SIDE_LEFT) (x - ox) else (ox - x)
        val upward = oy - y
        val rev = reverseTolerance(slopPx, density)
        return inward < -rev || upward < -rev
    }
}
