package com.oupao.wiz

/**
 * 扇形菜单的**跨进程状态**——只用系统属性（`sys.` 前缀 → `system_prop`）。
 *
 * 为什么不用别的通道（都实测过，记在这里免得后人再走一遍）：
 *  - `Settings.Global`：system_server 侧读写会被拒（SELinux），SystemUI 也读不稳。
 *  - `/data/system/oupao_cfg/` 目录：只有 system_server 能写，SystemUI/Launcher 读不到。
 *  - 广播：一次手势要每帧同步，延迟不可接受。
 *  - 属性名**必须**以 `sys.` 开头：未定义名会落到 `default_prop`，
 *    system_server 写会 `PROP_ERROR_PERMISSION_DENIED (0x18)`（2706 踩过）。
 *    读侧任何进程都可以。
 *
 * 三条状态：
 *  - [P_OPEN] 菜单是否展开（1/0）
 *  - [P_R]    热区半径 px（= flyRange(dp) × density）
 *  - [P_LR]   热区开关位 1=左下 2=右下（0=关，此时整套判定直接失效）
 */
object FlyState {

    const val P_OPEN = "sys.oupao.fly.open"
    const val P_R = "sys.oupao.fly.r"
    const val P_LR = "sys.oupao.fly.lr"

    @Volatile
    private var get: java.lang.reflect.Method? = null

    @Volatile
    private var set: java.lang.reflect.Method? = null

    @JvmStatic
    fun raw(name: String): String {
        try {
            var m = get
            if (m == null) {
                val c = Class.forName("android.os.SystemProperties")
                m = c.getMethod("get", String::class.java, String::class.java)
                get = m
            }
            return (m.invoke(null, name, "") as? String) ?: ""
        } catch (_: Throwable) {
            return ""
        }
    }

    @JvmStatic
    fun open(): Boolean = raw(P_OPEN) == "1"

    @JvmStatic
    fun radius(): Int = raw(P_R).toIntOrNull() ?: 0

    @JvmStatic
    fun lr(): Int = raw(P_LR).toIntOrNull() ?: 0

    /** 只有 system_server 写成功 */
    @JvmStatic
    fun put(name: String, value: String): Boolean {
        try {
            var m = set
            if (m == null) {
                val c = Class.forName("android.os.SystemProperties")
                m = c.getMethod(
                    "set",
                    String::class.java,
                    String::class.java,
                )
                set = m
            }
            m.invoke(null, name, value)
            return true
        } catch (_: Throwable) {
            return false
        }
    }

    @JvmStatic
    fun setOpen(open: Boolean) = put(P_OPEN, if (open) "1" else "0")

    /**
     * FlyS 在 sync 时发布一次热区几何。
     *
     * 3290：**以实际属性值为准**再决定写不写。
     * 3289 那种"内存里记上次值"的做法，一旦开机时 `SystemProperties.set` 失败
     * 就会把内存值记成已发布 → 之后永远不再重试 → `sys.oupao.fly.lr` 恒为 0
     * → 桌面 FlyGate 判 `gate=false` → 手势穿透到桌面/应用。
     * 现在每次读一下属性，不一致就重写；写失败下一轮 sync 会自动补。
     */
    @JvmStatic
    fun publish(radiusPx: Int, lr: Int) {
        val r = radiusPx.toString()
        if (raw(P_R) != r) put(P_R, r)
        val l = lr.toString()
        if (raw(P_LR) != l) put(P_LR, l)
    }
}
