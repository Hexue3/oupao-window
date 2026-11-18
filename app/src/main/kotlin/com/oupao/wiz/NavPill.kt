package com.oupao.wiz

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.view.View

/**
 * 2911：系统手势小白条（屏幕底部 pill）。
 *
 * 用户报：**呼出扇形菜单时底部横条跟着动**，要求它在这期间保持静止。
 *
 * 进程关系（2910 实测）：
 *  - 扇形菜单 `FlyFanH` 画在 **system_server**（日志 `(system) FlyFanH: 自绘菜单已建`）;
 *  - 小白条 `NavigationHandle` 只在 **SystemUI** 进程里。
 *  → 所以 system_server 侧只能**发广播**让 SystemUI 去改那个 View 的 alpha（只改 alpha，
 *    不像 2904 那样去驱动收面板动画，避免再次把系统搞卡死）。
 */
object NavPill {

    private const val ACTION_HIDE = "com.oupao.wiz.PILL_HIDE"
    private const val ACTION_SHOW = "com.oupao.wiz.PILL_SHOW"

    private val main = Handler(Looper.getMainLooper())

    @Volatile
    private var handle: View? = null

    @Volatile
    private var savedAlpha: Float = 1f

    @Volatile
    private var hiddenByUs = false

    @Volatile
    private var registered = false

    private val CLASSES = arrayOf(
        "com.android.systemui.navigationbar.gestural.NavigationHandle",
        "com.oplus.systemui.navigationbar.gesture.sidegesture.OplusNavigationHandle",
    )

    @JvmStatic
    fun install(cl: ClassLoader?): Int {
        var n = 0
        for (name in CLASSES) {
            val cls = runCatching { Rt.cls(cl, name) }.getOrNull() ?: continue
            for (ctor in cls.declaredConstructors) {
                runCatching { ctor.isAccessible = true }
                if (XBridge.hookMethod(ctor, CB()) != null) n++
            }
        }
        // 2913：用当前 Application 的 Context 注册广播（系统进程 getSystemContext() 的包名是 android，
        //   会被拒："Given caller package android is not running in process com.android.systemui"）。
        registerViaApplication()
        return n
    }

    private fun registerViaApplication(attempt: Int = 0) {
        if (registered) return
        runCatching {
            val at = Class.forName("android.app.ActivityThread")
            val app = at.getMethod("currentApplication").invoke(null) as? Context
            if (app == null) {
                if (attempt < 3) main.postDelayed({ registerViaApplication(attempt + 1) }, 1500L)
                return
            }
            registerWith(app)
        }.onFailure { SLog.e("NavPill: currentApplication 失败", it) }
    }

    /**
     * 2912：用**真正的 SystemUI Context** 注册（2909 用 systemContext 会
     * `SecurityException: Given caller package android is not running in process com.android.systemui`）。
     * 由 SystemUI 侧的手势旁听（FlySideHook）把 ctx 递进来，第一次手势时注册。
     */
    @JvmStatic
    fun registerWith(c: Context?) {
        if (registered || c == null) return
        runCatching {
            val flt = IntentFilter().apply {
                addAction(ACTION_HIDE)
                addAction(ACTION_SHOW)
            }
            c.registerReceiver(Rcv(), flt, Context.RECEIVER_EXPORTED)
            registered = true
            SLog.r("NavPill", "NavPill: \u767d\u6761\u5e7f\u64ad\u5df2\u6ce8\u518c", 3000L)
        }.onFailure { SLog.e("NavPill: \u6ce8\u518c\u5e7f\u64ad\u5931\u8d25", it) }
    }

    class CB : XMH() {
        override fun afterHookedMethod(param: XMP) {
            runCatching {
                val v = param.thisObject as? View ?: return@runCatching
                handle = v
            }
        }
    }

    class Rcv : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val hide = intent?.action == ACTION_HIDE
            main.post { if (hide) hideLocal() else showLocal() }
        }
    }

    /** 任意进程调用：同一进程有 View 就本地改，否则发广播给 SystemUI。 */
    @JvmStatic
    fun requestHide() {
        if (findPillViews().isNotEmpty()) {
            hideLocal()
        } else {
            runCatching {
                Cfg.sysContext()?.sendBroadcast(Intent(ACTION_HIDE)) 
            }.onFailure { SLog.e("NavPill: 发隐藏广播失败", it) }
        }
    }

    @JvmStatic
    fun requestShow() {
        if (findPillViews().isNotEmpty()) {
            showLocal()
        } else {
            runCatching {
                Cfg.sysContext()?.sendBroadcast(Intent(ACTION_SHOW))
            }.onFailure { SLog.e("NavPill: 发显示广播失败", it) }
        }
    }

    private fun hideLocal() {
        runCatching {
            if (hiddenByUs) return@runCatching
            val list = findPillViews()
            if (list.isEmpty()) {
                SLog.r("NavPill", "NavPill: \u6ca1\u627e\u5230\u5c0f\u767d\u6761\u89c6\u56fe", 1500L)
                return@runCatching
            }
            // 记下真实 alpha（桌面常态下并不是 1.0），复原时按原值还回去
            savedAlpha = list[0].alpha
            val names = StringBuilder()
            for (v in list) {
                if (names.isNotEmpty()) names.append(',')
                names.append(v.javaClass.simpleName).append('@').append(v.width).append('x').append(v.height)
                v.animate().cancel()
                v.alpha = 0f
                // 2926：夹住位置。小白条在手势期间仍会被 ROM 拖动
                //   （平移/压扁），夹住后关菜单时就不会「从别的地方飞回来」。
                v.translationX = 0f
                v.translationY = 0f
                v.scaleX = 1f
                v.scaleY = 1f
            }
            hiddenByUs = true
            SLog.r("NavPill", "NavPill: \u9690\u85cf " + list.size + " \u4e2a -> " + names, 1000L)
        }
    }

    private fun showLocal() {
        runCatching {
            if (!hiddenByUs) return@runCatching
            hiddenByUs = false
            // 先把位置/缩放在不可见状态下复位，并让系统自己的"回位"动画在这里跑完，
            // 之后再露出来 —— 否则就能看到小白条从别处飞回来。
            resetTransform(resetAll = true)
            // 2927：恢复要快。先在不可见状态下复位 90ms（足够让系统那段回位动画跑完），
            //   然后立即露出来，并在接下来 220ms 里每帧钳住位置，所以既快又不会「飞回来」。
            main.postDelayed({
                runCatching {
                    resetTransform(resetAll = false)
                    for (v in findPillViews()) {
                        v.animate().cancel()
                        v.alpha = savedAlpha
                    }
                    SLog.r("NavPill", "NavPill: \u5df2\u8fd8\u539f", 1000L)
                    clampFor(220L)
                }
            }, 90L)
        }
    }

    /** 在 ms 毫秒内每帧钳住位置/缩放，防止 ROM 那段回位动画被看见。 */
    private fun clampFor(ms: Long) {
        val until = android.os.SystemClock.uptimeMillis() + ms
        val tick = object : Runnable {
            override fun run() {
                runCatching {
                    resetTransform(resetAll = false)
                    for (v in findPillViews()) v.alpha = savedAlpha
                }
                if (android.os.SystemClock.uptimeMillis() < until) main.postDelayed(this, 16L)
            }
        }
        main.postDelayed(tick, 16L)
    }

    /** 复位平移/缩放（resetAll=false 时只复位、不动 alpha）。 */
    private fun resetTransform(resetAll: Boolean) {
        for (v in findPillViews()) {
            v.animate().cancel()
            v.translationX = 0f
            v.translationY = 0f
            v.scaleX = 1f
            v.scaleY = 1f
            if (resetAll) v.alpha = 0f
        }
    }

    /** 运行时把「像小白条」的视图都收集起来（2913 只隐藏了 OplusNavigationHandle，可见那条其实是别的 View）。 */
    private fun findPillViews(): List<View> {
        val out = ArrayList<View>()
        runCatching {
            val wmgCls = Class.forName("android.view.WindowManagerGlobal")
            val wmg = wmgCls.getMethod("getInstance").invoke(null)
            val views = Rt.get(wmg, "mViews") as? List<*> ?: return out
            for (r in views) {
                val root = r as? View ?: continue
                walk(root, out, 0)
            }
        }
        handle = out.firstOrNull()
        return out
    }

    private fun walk(v: View, out: ArrayList<View>, depth: Int) {
        if (depth > 40) return
        val name = v.javaClass.name.lowercase()
        val idName = runCatching { v.resources.getResourceEntryName(v.id) }.getOrNull() ?: ""
        val hit = name.contains("navigationhandle") ||
            name.contains("navbarhandle") ||
            name.contains("gesturebar") ||
            idName.contains("navigation_bar_handle") ||
            idName.contains("navbar_handle") ||
            idName.contains("gesture_handle")
        if (hit && v.isShown) out.add(v)
        if (v is android.view.ViewGroup) {
            for (i in 0 until v.childCount) walk(v.getChildAt(i), out, depth + 1)
        }
    }
}
