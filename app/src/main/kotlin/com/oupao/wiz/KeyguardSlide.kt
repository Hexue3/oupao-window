package com.oupao.wiz

import android.view.MotionEvent
import android.view.View

/**
 * 锁屏数字键盘「滑动输入密码」（FlymeUI 页 → `oupao_keyguard_slide`）。
 *
 * ## 做法（本模块自己的实现）
 * 只拦**一个**入口：键盘自己的 `onTouchEvent(MotionEvent)`。
 * 这样不依赖 ROM 内部把事件分给哪个私有方法（`handleActionDown(FFI)` 还是
 * `handleActionDown(MotionEvent,int)`），改版也不会失效。
 *
 * ```
 * ACTION_DOWN / POINTER_DOWN   → checkForNewHit(x,y) 命中数字格 → 自己按下 + 输入 + 震动，吃掉事件
 * ACTION_MOVE                  → 每根「我们接管的手指」滑到新格就换格输入；旧格的按压取消
 * ACTION_UP / POINTER_UP       → 松开我们按住的格，吃掉事件
 * ACTION_CANCEL                → 全部收干净
 * ```
 * 输入走原厂 `callback(index)`（0~8→1~9、10→0），所以密码圆点、错误抖动、自动校验都照旧。
 *
 * ## 边界
 * - **起滑位置不受限**：数字键整块区域按下都能进入滑动；
 * - **换格只认按键中心直径 2/3 的圆**（`TRIGGER_DIAMETER_SCALE`）：滑到下一个键的边缘不算命中，
 *   必须进入它的中心圈才输入，避免到键边缘就误选；
 * - 侧键（左 9 / 右 11）**不接管**：DOWN 落在侧键原样放行，滑过侧键只取消我们的按压、不触发；
 * - 触摸探索（读屏）开启时不接管；
 * - 键盘被禁用（校验中）时不接管，并且会先把我们留下的按压状态收干净（否则原厂抬手时
 *   会看 `cell.pointerId` 再输入一次 → 数字重复）。
 */
object KeyguardSlide {

    private const val TAG = "OupaoKGS"

    private val INT = Integer.TYPE

    // ---------------- 安装 ----------------

    /**
     * 候选键盘类名，**顺序即优先级**（C16 的写在最前，保持既有行为完全不变）：
     * - `COUINumericKeyboard`：C16 上真正带 `onTouchEvent` 的父类；
     * - `NumericKeyboard(Widget)`：C16 上实际被摸到的子类（实机日志见过两个写法），它自己没重写
     *   `onTouchEvent`，所以父类钩子能收到；万一以后重写了这里也能挂上；
     * - 后面几个是 C17 可能的新名字（PJZ110 / ColorOS 17 报障：钩子挂上了但一次都没被调用，
     *   说明真正的键盘类换了名）。全部 `runCatching`，找不到就只打一行日志。
     */
    private val CANDIDATES = arrayOf(
        "com.coui.appcompat.lockview.COUINumericKeyboard",
        "com.oplus.keyguard.security.widget.NumericKeyboard",
        "com.oplus.keyguard.security.widget.NumericKeyboardWidget",
        "com.coui.appcompat.lockview.COUINumericKeyboardWidget",
        "com.oplus.keyguard.security.widget.COUINumericKeyboard",
        "com.oplus.keyguard.security.widget.PinKeyboardView",
        "com.oplus.keyguard.security.widget.PasswordKeyboardView",
        "com.oplus.keyguard.security.widget.COUIPinKeyboardView",
        "com.oplus.keyguard.security.widget.NumericKeypadView",
        "com.coui.appcompat.lockview.COUINumericKeypad"
    )

    fun install(cl: ClassLoader?): Boolean {
        var n = 0
        val hooked = ArrayList<Class<*>>()
        val exists = ArrayList<String>()
        for (name in CANDIDATES) {
            val c = runCatching { Rt.cls(cl, name) }.getOrNull()
            SLog.i("KGS: 候选 " + name + " exists=" + (c != null))
            if (c == null) continue
            exists.add(name)
            // 父子都挂：子类若自己重写了 onTouchEvent，父类钩子根本收不到事件（C17 很可能就是这样）；
            // 同一个 MotionEvent 实例在 onTouch 里按身份去重，所以不会重复输入。
            val k = hookTouch(c)
            if (k > 0) {
                n += k
                hooked.add(c)
            }
        }
        // ★★ C17（Android 17 / ColorOS 17，SystemUI 17.99.02 反编译实证）：
        //   锁屏密码键盘换成了 **`COUIPasswordDial` 体系** —— 表盘是 FrameLayout + 子 View 按键
        //   （`PasswordDialKeyView` / `PasswordDialIconKeyView`），触摸入口是
        //   `PasswordUnlockGestureLayout#dispatchTouchEvent(MotionEvent)`。
        //   `COUINumericKeyboard` 在 C17 上**仍然存在**，但整份日志里它的 onTouchEvent 一次都没被调到
        //   → 用户摸的不是它。这一版先**只挂诊断钩子**（不改任何行为，只打前几条触摸 + 坐标），
        //   拿一份日志把「谁在收触摸、坐标系多大」钉死，下一版再做坐标→按键的接管。
        runCatching {
            val diag = arrayOf(
                "com.coui.appcompat.passworddial.PasswordUnlockGestureLayout" to "dispatchTouchEvent",
                "com.coui.appcompat.passworddial.key.PasswordDialMatrixLayout" to "onTouchEvent",
                "com.coui.appcompat.passworddial.key.PasswordDialKeyView" to "dispatchTouchEvent"
            )
            for ((cn, mn) in diag) {
                val c = runCatching { Rt.cls(cl, cn) }.getOrNull()
                SLog.i("KGS: C17 诊断候选 " + cn.substringAfterLast('.') + " exists=" + (c != null))
                if (c == null) continue
                val m = runCatching { c.getDeclaredMethod(mn, MotionEvent::class.java) }.getOrNull()
                if (m == null) {
                    SLog.i("KGS: C17 诊断候选 " + cn.substringAfterLast('.') + " 没有 " + mn)
                    continue
                }
                runCatching { m.isAccessible = true }
                val cb = object : XMH() {
                    override fun beforeHookedMethod(param: XMP) {
                        if (diagLeft <= 0) return
                        val ev = param.args.getOrNull(0) as? MotionEvent ?: return
                        diagLeft--
                        val o = param.thisObject
                        val v = o as? android.view.View
                        SLog.i(
                            "KGS: 诊断触摸 " + (o?.javaClass?.simpleName ?: "-") + "#" + mn +
                                " a=" + ev.actionMasked +
                                " x=" + ev.x.toInt() + " y=" + ev.y.toInt() +
                                " p=" + ev.pointerCount +
                                " w=" + (v?.width ?: -1) +
                                " h=" + (v?.height ?: -1)
                        )
                    }
                }
                if (XBridge.hookMethod(m, cb) != null) {
                    SLog.i("KGS: C17 诊断钩子已挂 " + cn.substringAfterLast('.') + "#" + mn)
                }
            }
        }.onFailure { SLog.e("KGS: C17 诊断钩子失败", it) }

        // ★★ C17 真正的滑动输入：`PasswordDialKeyView#dispatchTouchEvent`
        //   3071 的实测日志（`KGS: 诊断触摸`）给了完整事实：
        //   - 手指按下后，**同一个** key 会继续收到所有 MOVE（local 坐标会跑到 272 框外），
        //     所以"换格"只能我们自己按**屏幕坐标**做命中测试；
        //   - 每个 key 上都装了 ROM 自己的 OnClickListener
        //     （`COUIPasswordDial$$ExternalSyntheticLambda5/6#onClick(View)`），
        //     所以 `view.performClick()` 就等于"按了一下这个数字"，走的是原厂输入路径。
        //   策略：按下在**数字键**上就整条手势由我们吃（和 C16 一致），滑到别的数字键就替它 click；
        //         按下在侧键（删除/确认）上不接管，原样交给原厂。
        runCatching {
            val k = Rt.cls(cl, "com.coui.appcompat.passworddial.key.PasswordDialKeyView")
            if (k == null) {
                SLog.i("KGS: 找不到 PasswordDialKeyView（C17 滑动输入不挂）")
                return@runCatching
            }
            val m = k.getDeclaredMethod("dispatchTouchEvent", MotionEvent::class.java)
            m.isAccessible = true
            val cb = object : XMH() {
                override fun beforeHookedMethod(param: XMP) = onTouchC17(param)
            }
            if (XBridge.hookMethod(m, cb) != null) {
                n++
                SLog.i("KGS: C17 滑动输入已挂 PasswordDialKeyView#dispatchTouchEvent")
            } else {
                SLog.i("KGS: C17 滑动输入挂钩失败（hookMethod 返回 null）")
            }
        }.onFailure { SLog.e("KGS: C17 滑动输入挂钩异常", it) }

        if (n > 0) {
            SLog.i("KGS: 滑动输入挂上=$n " + hooked.joinToString { it.simpleName + "#onTouchEvent" })
        } else {
            SLog.i(
                "KGS: 滑动输入没挂上（候选里没有一个声明 onTouchEvent）exists=" + exists.joinToString()
            )
        }
        return n > 0
    }

    private fun hookTouch(k: Class<*>): Int {
        val m = runCatching { k.getDeclaredMethod("onTouchEvent", MotionEvent::class.java) }.getOrNull() ?: return 0
        runCatching { m.isAccessible = true }
        val cb = object : XMH() {
            override fun beforeHookedMethod(param: XMP) = onTouch(param)
        }
        return if (XBridge.hookMethod(m, cb) != null) 1 else 0
    }

    // ---------------- 开关 ----------------

    @Volatile
    private var onCache = false

    @Volatile
    private var onAt = 0L

    /** 第一次收到触摸就记一行日志（排查「钩子到底有没有被调到」） */
    @Volatile
    private var firstTouchLogged = false

    /** 去重：同一个 MotionEvent 实例只处理一次（父类+子类都挂了钩子时） */
    @Volatile
    private var lastSig = Long.MIN_VALUE

    /** 诊断配额：只打前面几十条，免得刷屏 */
    private var dbgLeft = 40

    /** C17 诊断钩子的打印配额（`PasswordDial*` 那三个） */
    private var diagLeft = 30

    private fun dbg(msg: String) {
        if (dbgLeft <= 0) return
        dbgLeft--
        SLog.i("KGS: " + msg)
    }

    /**
     * SystemUI 读不到模块私有文件，走 `Settings.Global`（system_server 收到 CFG 广播后镜像过来）。
     * 800ms 缓存：滑动时 MOVE 一秒几十次，不能每次都跨进程读设置。
     */
    private fun enabled(): Boolean {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - onAt < 800L) return onCache
        onAt = now
        val v = runCatching { Cfg.keyguardSlide() }.getOrDefault(false)
        if (v != onCache) SLog.i("KGS: 开关=" + v)
        onCache = v
        return v
    }

    // ---------------- 接管状态 ----------------

    /** 哪些 pointerId 正被我们接管（一旦接管，这根手指的 MOVE/UP 必须一直由我们吃掉） */
    private val ownedMap = java.util.WeakHashMap<Any, HashSet<Int>>()

    private fun set(kb: Any): HashSet<Int> = synchronized(ownedMap) { ownedMap.getOrPut(kb) { HashSet() } }

    private fun own(kb: Any, id: Int) {
        synchronized(ownedMap) { set(kb).add(id) }
    }

    private fun unown(kb: Any, id: Int) {
        synchronized(ownedMap) { set(kb).remove(id) }
    }

    private fun isOwned(kb: Any, id: Int): Boolean = synchronized(ownedMap) { set(kb).contains(id) }

    // ---------------- C17（PasswordDial 体系）----------------

    /** 当前手势是否由我们接管（C17） */
    private var c17Owned = false

    /**
     * 上一次已经**输入过**的 key（C17）。
     *
     * ⚠️ 不能在一瞬间滑出中心圈（`hit == null`）时清空它：手指原地长按会有细微 MOVE，
     * 每抖出中心圈再抖回来就会被当成新键，表现为「长按一直连续输入」。
     * 只有真的滑到另一个数字键才更新；抬手才清空。
     */
    private var c17Last: android.view.View? = null

    /** 当前正亮着按压动画的 key（C17）；和 [c17Last] 分开，允许动画先熄灭但输入仍去重 */
    private var c17Visual: android.view.View? = null

    /** C17 按下时的屏幕坐标；用来区分「真的在滑动」和「长按时的细微抖动」 */
    private var c17DownRawX = 0f
    private var c17DownRawY = 0f

    /** C17 防抖阈值（px），第一次用时按当前屏幕密度算 */
    private var c17SlopPx = -1f

    /** C17 输入次数（日志里带序号，绕开 SLog 去重） */
    private var c17Seq = 0

    /** C17 诊断配额：只在"按下却没接管"时打几条，用来定位「为什么没接管」 */
    private var c17DiagLeft = 6

    /**
     * 我们给 C17 按键补发「假触摸」时置 true。
     *
     * `PasswordDialKeyView#dispatchTouchEvent` 本身除了输入，还负责：
     * 1. 按下震动（`performHapticFeedback`）；
     * 2. 原厂的按压光效/形变（`materialHelper.dispatchTouchEvent`，内部走 IOLA 弹簧动画）；
     * 3. `super.dispatchTouchEvent` 里的 TextView pressed/ripple 状态。
     *
     * 之前 C17 分支只 `performClick()`，等于把这三样全绕过了 → 用户看到的就是
     * 「点按和滑动都没有动效」。这里用合成事件重新走一遍原厂 dispatchTouchEvent；
     * 钩子看到合成事件就直接放行，避免递归二次输入。
     */
    private val c17SynthTouch = object : ThreadLocal<Boolean>() {
        override fun initialValue(): Boolean = false
    }

    /**
     * C17 里被摸到的是哪个类（3071 实测）：`PasswordDialKeyView`（表盘上的一个键），
     * 宽高实测 238×238；侧键是 `PasswordDialIconKeyView`。
     */
    private fun onTouchC17(param: XMP) {
        // 我们自己补发的 DOWN/CANCEL 必须走原厂实现（震动、光效、pressed 状态），不能再被接管
        if (c17SynthTouch.get() == true) return
        val v = param.thisObject as? android.view.View ?: return
        val ev = param.args.getOrNull(0) as? MotionEvent ?: return
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!enabled()) {
                    if (c17DiagLeft > 0) {
                        c17DiagLeft--
                        SLog.i("KGS: C17 按下但开关=关（oupao_keyguard_slide）")
                    }
                    return
                }
                if (!isDigitKey(v)) {
                    if (c17DiagLeft > 0) {
                        c17DiagLeft--
                        SLog.i(
                            "KGS: C17 按下不接管 cls=" + v.javaClass.simpleName +
                                " text=[" + textOf(v) + "] click=" + runCatching { v.hasOnClickListeners() }
                                    .getOrDefault(false) +
                                " cd=[" + (v.contentDescription ?: "") + "]"
                        )
                    }
                    return                 // 侧键/非数字键：不接管，交回原厂
                }
                // 上一次手势如果异常中断，先把残留的按压光效收掉，避免两个键同时亮着
                releaseC17(c17Visual)
                c17Owned = true
                c17DownRawX = ev.rawX
                c17DownRawY = ev.rawY
                c17Last = v
                c17Visual = v
                tapKey(v, "down")
                eatC17(param)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!c17Owned) return
                // 手指几乎没动（长按的细微抖动）→ 不做换格判定。
                // 原厂按压光效会让按键缩放，按键在屏幕上的 bounds 会跟着变；如果这时还按
                // 像素位置做命中，手指明明没动也会“滑到别的键/滑出中心圈”，产生连续输入或抖动。
                val rawDx = ev.rawX - c17DownRawX
                val rawDy = ev.rawY - c17DownRawY
                val slop = c17Slop(v)
                if (rawDx * rawDx + rawDy * rawDy < slop * slop) {
                    eatC17(param)
                    return
                }
                val hit = keyAtScreen(v, ev.rawX, ev.rawY)
                if (hit == null) {
                    // 滑到两个键之间的死区 / 下一个键的外圈：**保持当前键的按压动画**。
                    // 这里绝不能 CANCEL 再 DOWN：慢慢滑出数字时手指会在中心圈边界反复进出，
                    // 一收一发就会看到按键疯狂抽搐；输入去重则交给 c17Last。
                    eatC17(param)
                    return
                }
                if (hit === c17Last) {
                    // 还是已经输入过的键：只保证动画在它身上，绝不再次输入
                    if (c17Visual !== hit) {
                        synthTouchC17(hit, MotionEvent.ACTION_DOWN)
                        c17Visual = hit
                    }
                    eatC17(param)
                    return
                }
                // 真的滑到了另一个键：先结束旧键动画，再让新键走原厂 DOWN + 输入一次
                releaseC17(c17Visual)
                c17Visual = null
                if (isDigitKey(hit)) {
                    c17Last = hit
                    c17Visual = hit
                    tapKey(hit, "move")
                }
                eatC17(param)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!c17Owned) return
                c17Owned = false
                releaseC17(c17Visual)
                c17Visual = null
                c17Last = null
                eatC17(param)
            }
        }
    }

    /** 吃掉这条事件（原厂就不会再按一次） */
    private fun eat(param: XMP) {
        param.result = false
        param.returnEarly = true
    }

    /**
     * C17 专用「吃掉」：必须返回 **true**。
     *
     * 我们挂的是 `PasswordDialKeyView#dispatchTouchEvent`。如果这里返回 false，
     * 父容器会认为这个子 View **没在处理**这条手势，后续 MOVE/UP 就不再派发给它 ——
     * 结果就是「按下吃到一次，滑动时收不到 MOVE，永远不换格」。
     * 返回 true + 不执行原厂代码，既保住事件流，又不会重复输入。
     */
    private fun eatC17(param: XMP) {
        param.result = java.lang.Boolean.TRUE
        param.returnEarly = true
    }

    /**
     * 给按键补发一条原厂触摸事件（C17）。
     *
     * 只补发 **DOWN/CANCEL**，不补 UP：
     * - DOWN 负责触发原厂震动、按压光效、pressed/ripple；
     * - 输入仍由 [tapKey] 里的 `performClick()` 负责（否则松手位置/多指会让原厂再判一次）；
     * - 换格或抬手时补 CANCEL，让光效/形变按原厂逻辑回弹。
     *
     * 合成事件走 `dispatchTouchEvent` 而不是 `onTouchEvent`，因为原厂的震动和
     * `materialHelper` 都写在 `PasswordDialKeyView#dispatchTouchEvent` 里。
     */
    private fun synthTouchC17(v: android.view.View, action: Int) {
        runCatching {
            val w = v.width
            val h = v.height
            if (w <= 0 || h <= 0) return
            val now = android.os.SystemClock.uptimeMillis()
            val ev = MotionEvent.obtain(now, now, action, w / 2f, h / 2f, 0)
            val prev = c17SynthTouch.get()
            c17SynthTouch.set(true)
            try {
                v.dispatchTouchEvent(ev)
            } finally {
                c17SynthTouch.set(prev)
                ev.recycle()
            }
        }
    }

    /** 长按判抖阈值：系统 touchSlop 再放宽一点，避免按压光效缩放带来的像素级漂移误判 */
    private fun c17Slop(v: android.view.View): Float {
        if (c17SlopPx < 0f) {
            c17SlopPx = runCatching {
                android.view.ViewConfiguration.get(v.context).scaledTouchSlop * 1.5f
            }.getOrDefault(48f)
        }
        return c17SlopPx
    }

    /** 结束某个键的按压动画（C17） */
    private fun releaseC17(v: android.view.View?) {
        if (v != null) synthTouchC17(v, MotionEvent.ACTION_CANCEL)
    }

    /** 替某个键"按一下"（先走原厂 DOWN 动画，再走原厂 OnClickListener 输入） */
    private fun tapKey(v: android.view.View, why: String) {
        val txt = textOf(v)
        synthTouchC17(v, MotionEvent.ACTION_DOWN)
        val ok = runCatching { v.performClick() }.getOrDefault(false)
        if (dbgLeft > 0) {
            dbgLeft--
            SLog.i("KGS: C17 输入#" + (++c17Seq) + " 「" + txt + "」(" + why + ") ok=" + ok)
        }
    }

    /**
     * 这个键是不是数字键（0~9）。
     *
     * ⚠️ C17 实测（2026-11-08 用户日志 3127）：`PasswordDialKeyView` 的触摸**是收到了的**
     * （`KGS: 诊断触摸 PasswordDialKeyView#dispatchTouchEvent a=0 x=120 y=124 w=238 h=238`），
     * 但整份日志里一条 `KGS: C17 输入` 都没有 → 说明这里判成 false 直接 return 了。
     * 原来 `textOf()` 把它当 TextView 取 `.text`，而 C17 表盘键不靠 `text` 放数字
     * （自绘 / 图片 / 文字在子 View 里）→ 取到空串 → 当成"非数字键"。
     *
     * 现在**不依赖文字**：是 `PasswordDialKeyView`（不是 `...IconKeyView`）且有自己的点击回调，
     * 就按数字键处理 —— 输入仍然走原厂的 OnClickListener（`performClick()`），不需要知道是几。
     */
    private fun isDigitKey(v: android.view.View): Boolean {
        val cn = v.javaClass.name
        if (cn.endsWith("PasswordDialIconKeyView")) return false      // 侧键（删除/确认）
        if (cn.endsWith("PasswordDialKeyView")) {
            val t = textOf(v)
            if (t.length == 1 && t[0] in '0'..'9') return true
            return runCatching { v.hasOnClickListeners() }.getOrDefault(false) || v.isClickable
        }
        val t = textOf(v)
        return t.length == 1 && t[0] in '0'..'9'
    }

    /** 数字键上的文字（只用于日志/兼容老键盘；C17 上可能取不到，取不到返回 ""） */
    private fun textOf(v: android.view.View): String {
        runCatching {
            val t = (v as? android.widget.TextView)?.text?.toString()
            if (!t.isNullOrBlank()) return t.trim()
        }
        runCatching {
            if (v is android.view.ViewGroup) {
                for (i in 0 until v.childCount) {
                    val t = (v.getChildAt(i) as? android.widget.TextView)?.text?.toString()
                    if (!t.isNullOrBlank()) return t.trim()
                }
            }
        }
        runCatching {
            val m = v.javaClass.methods.firstOrNull { it.name == "getText" && it.parameterTypes.isEmpty() }
            val s = (m?.invoke(v) as? CharSequence)?.toString()
            if (!s.isNullOrBlank()) return s.trim()
        }
        return runCatching { v.contentDescription?.toString()?.trim().orEmpty() }.getOrDefault("")
    }

    /**
     * **换格命中区**：按键中心、直径 2/3 的圆。起滑位置不受这个限制（数字键整块都能起滑）。
     *
     * 旧行为是整块按键（272×272，C16 还会被 `mAdditionalPressableArea` 向外扩）都算命中 ——
     * 滑动到下一个键的边缘就会立刻选中（用户报：命中范围太大）。
     * 现在只有滑进下一个键的中心圈才换格输入；同一格内滑出中心圈仍保持按压，避免闪烁。
     *
     * 「三分之二」指**直径**：半径 = 键宽 × 2/3 ÷ 2 = 键宽 / 3。
     * 拿不到几何信息（ROM 改版/反射失败）时返回 true，退回旧行为，保证功能不会整个失效。
     */
    private const val TRIGGER_DIAMETER_SCALE = 2f / 3f

    /** C17：`v` 是单个数字键，`x/y` 是相对该键的坐标 */
    private fun inCenterZone(v: android.view.View, x: Float, y: Float): Boolean {
        val w = v.width
        val h = v.height
        if (w <= 0 || h <= 0) return true
        val r = minOf(w, h) * TRIGGER_DIAMETER_SCALE / 2f
        val dx = x - w / 2f
        val dy = y - h / 2f
        return dx * dx + dy * dy <= r * r
    }

    /**
     * C16：格子没有自己的矩形，用原厂的 `getCenterXForColumn/Row` + `mCellWidth/Height` 还原圆心。
     *
     * ⚠️ 2026-11-08 实测（另一台 C17 机器，3142）：
     * `KGS: MOVE id=0 cell=7 trig=false` / `cell=10 trig=false` —— **换格全被这里判 false**，
     * 表现就是「锁屏滑动输入用不了」。说明 C17 上 `getCenterXForColumn/Row` 或
     * `mCellWidth/Height` 与真实按键位置对不上（坐标系/内边距不同），算出来的圆心根本不在手指附近。
     *
     * 兜底：算出来的圆心离「当前命中点」超过一格的尺寸时，认为几何不可信 → 返回 true（退回旧行为：
     * 命中即换格）。正常的机型（我们自己 C16）圆心和命中点必然在同一格内，这条不会误伤。
     */
    private fun inCenterZone(kb: Any, cell: Any, x: Float, y: Float): Boolean {
        val row = gi(cell, "row", -1)
        val col = gi(cell, "column", -1)
        if (row < 0 || col < 0) return true
        val cx = runCatching {
            mthLoose(kb, "getCenterXForColumn", 1)?.invoke(kb, col) as? Float
        }.getOrNull() ?: return true
        val cy = runCatching {
            mthLoose(kb, "getCenterYForRow", 1)?.invoke(kb, row) as? Float
        }.getOrNull() ?: return true
        val size = minOf(gi(kb, "mCellWidth", 0), gi(kb, "mCellHeight", 0))
        if (size <= 0) return true
        val dx = x - cx
        val dy = y - cy
        // ★ 几何可信性校验：圆心离命中点超过一格 ⇒ 这套反射坐标和真实按键对不上，直接放行
        if (kotlin.math.abs(dx) > size * 1.01f || kotlin.math.abs(dy) > size * 1.01f) {
            if (dbgLeft > 0) {
                dbgLeft--
                SLog.i(
                    "KGS: 圆心与命中点偏差过大，放行 r=" + row + "/c=" + col +
                        " center=" + cx.toInt() + "," + cy.toInt() +
                        " point=" + x.toInt() + "," + y.toInt() + " size=" + size
                )
            }
            return true
        }
        val r = size * TRIGGER_DIAMETER_SCALE / 2f
        return dx * dx + dy * dy <= r * r
    }

    /**
     * 屏幕坐标 → 命中的数字键。
     * 从当前 key 往上找几层容器，在每层子树里找 `PasswordDialKeyView` 且屏幕矩形包含该点的，
     * 取面积最小的那个（防止父层级误命中）。
     */
    private fun keyAtScreen(from: android.view.View, rx: Float, ry: Float): android.view.View? {
        var node: android.view.View? = from
        var best: android.view.View? = null
        var bestArea = Int.MAX_VALUE
        var level = 0
        while (node != null && level < 6) {
            if (node is android.view.ViewGroup) {
                val hit = findKeyIn(node, rx, ry)
                if (hit != null) {
                    val a = hit.first.width * hit.first.height
                    if (a < bestArea) {
                        best = hit.first
                        bestArea = a
                    }
                }
            }
            node = node.parent as? android.view.View
            level++
        }
        return best
    }

    /** 在子树里找命中的 key（返回 view + 面积） */
    private fun findKeyIn(g: android.view.ViewGroup, rx: Float, ry: Float): Pair<android.view.View, Int>? {
        var best: android.view.View? = null
        var bestArea = Int.MAX_VALUE
        val loc = IntArray(2)
        fun rec(vg: android.view.ViewGroup, depth: Int) {
            for (i in 0 until vg.childCount) {
                val ch = vg.getChildAt(i) ?: continue
                val name = ch.javaClass.name
                if (name.endsWith("PasswordDialKeyView") || name.endsWith("PasswordDialIconKeyView")) {
                    ch.getLocationOnScreen(loc)
                    if (rx >= loc[0] && rx <= loc[0] + ch.width &&
                        ry >= loc[1] && ry <= loc[1] + ch.height &&
                        // ★ 换格也必须落在该键的**中心 2/3 圆**内，键边缘不算命中
                        inCenterZone(ch, rx - loc[0], ry - loc[1])
                    ) {
                        val a = ch.width * ch.height
                        if (a > 0 && a < bestArea) {
                            best = ch
                            bestArea = a
                        }
                    }
                }
                if (ch is android.view.ViewGroup && depth < 6) rec(ch, depth + 1)
            }
        }
        rec(g, 0)
        val b = best ?: return null
        return b to bestArea
    }

    // ---------------- 触摸 ----------------

    private fun onTouch(param: XMP) {
        try {
            val kb = param.thisObject ?: return
            val ev = param.argOrNull(0) as? MotionEvent ?: return
            // ★★ 去重要用「事件时间 + 动作」，**不能**用 MotionEvent 实例身份！
            //   MotionEvent 是**池化复用**的：DOWN 派发完被回收后，UP/MOVE 很可能拿到
            //   **同一个实例**。之前用 `ev === lastEvent` 去重，结果：
            //     DOWN → 处理并吃掉（原厂没跑），
            //     UP   → 被误判成"重复"→ 没吃掉 → 原厂 UP 接手 → 原厂看到 cell.pointerId
            //            还在 → **又输入一次**（用户报的"点一次输入两次"）；
            //     MOVE → 同样被误判 → 没吃掉 → 原厂 MOVE 接手 → 原厂语义是
            //            "换格 = 取消按压"（不是换格输入）→ **滑动失效**。
            //   （2026-11-07 用户报障：滑动失效 + 点击双输入。）
            val sig = ev.eventTime.toLong() * 16L + ev.actionMasked
            if (sig == lastSig) return
            lastSig = sig
            if (!firstTouchLogged) {
                firstTouchLogged = true
                // 第一次真被摸到时打一行：能区分「钩子没挂上/不是这个键盘类」和「挂了但逻辑没生效」
                SLog.i("KGS: 首次触摸 " + kb.javaClass.name + " action=" + ev.actionMasked)
            }

            // 不接管的情况：先把我们自己的按压收干净再放行（否则会重复输入）
            if (!enabled() || (kb is View && !kb.isEnabled) || touchExploration(kb)) {
                releaseAll(kb, ev)
                return
            }

            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    val i = ev.actionIndex
                    if (i < 0 || i >= ev.pointerCount) return
                    val id = ev.getPointerId(i)
                    val x = ev.getX(i)
                    val y = ev.getY(i)
                    val cell = hitCell(kb, x, y)
                    dbg("DOWN id=$id cell=" + idxOf(cell) + " side=" + isSideCell(cell))
                    if (cell == null || isSideCell(cell)) {
                        unown(kb, id)
                        return                      // 侧键/格子外：交给原厂
                    }
                    own(kb, id)
                    press(kb, cell, id)
                    consume(param)
                }

                MotionEvent.ACTION_MOVE -> {
                    var touched = false
                    for (i in 0 until ev.pointerCount) {
                        val id = ev.getPointerId(i)
                        if (!isOwned(kb, id)) continue
                        touched = true
                        val x = ev.getX(i)
                        val y = ev.getY(i)
                        val cell = hitCell(kb, x, y)
                        // ★ 换格也只用「中心 2/3 圆」：手指到下一个键的边缘时不算命中，
                        //   必须滑到它的中心圈内才输入（用户报：到第二个数字边缘就选中，范围太大）。
                        val trig = cell != null && inCenterZone(kb, cell, x, y)
                        dbg("MOVE id=$id cell=" + idxOf(cell) + " trig=" + trig)
                        if (cell != null && !trig) dbgCenter(kb, cell, x, y)
                        moveTo(kb, id, cell, trig)
                    }
                    if (touched) consume(param)
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    val i = ev.actionIndex
                    if (i < 0 || i >= ev.pointerCount) return
                    val id = ev.getPointerId(i)
                    dbg("UP id=" + id + " owned=" + isOwned(kb, id))
                    if (!isOwned(kb, id)) return
                    release(kb, id)
                    unown(kb, id)
                    consume(param)
                    // ★ 保险：即使 UP 没被我们吃掉（比如中途有别的路径抢了事件），
                    //   原厂的 UP 也会看 `cell.pointerId` 决定要不要再输入一次 ——
                    //   我们已经在 release() 里把 pointerId 置成 -1 了，这里再确认一次，
                    //   彻底杜绝"点一下输入两次"。
                    pressedCell(kb, id)?.let { si(it, "pointerId", -1) }
                }

                MotionEvent.ACTION_CANCEL -> {
                    releaseAll(kb, ev)
                    consume(param)
                }
            }
        } catch (t: Throwable) {
            SLog.r(TAG, "onTouch error: $t", 1000L)
        }
    }

    private fun consume(param: XMP) {
        param.result = java.lang.Boolean.TRUE      // 事件已被我们处理
        param.returnEarly = true
    }

    /**
     * 诊断：MOVE 换格被"中心圈"拒掉时，把圆心/尺寸/手指位置打出来（最多 [centerDbgLeft] 条）。
     * 3142 的实测日志里只看到 `trig=false`，没有几何数值，无法判断到底是"手指在边缘"还是
     * "反射出来的圆心根本不对" —— 加上这个就能一次定死。
     */
    private var centerDbgLeft = 6

    private fun dbgCenter(kb: Any, cell: Any, x: Float, y: Float) {
        if (centerDbgLeft <= 0) return
        centerDbgLeft--
        runCatching {
            val row = gi(cell, "row", -1)
            val col = gi(cell, "column", -1)
            val cx = runCatching {
                mthLoose(kb, "getCenterXForColumn", 1)?.invoke(kb, col) as? Float
            }.getOrNull()
            val cy = runCatching {
                mthLoose(kb, "getCenterYForRow", 1)?.invoke(kb, row) as? Float
            }.getOrNull()
            SLog.i(
                "KGS: 中心圈未命中 r=" + row + "/c=" + col +
                    " center=" + cx + "," + cy +
                    " point=" + x.toInt() + "," + y.toInt() +
                    " cellW=" + gi(kb, "mCellWidth", -1) +
                    " cellH=" + gi(kb, "mCellHeight", -1) +
                    " kbW=" + ((kb as? View)?.width ?: -1) +
                    " kbH=" + ((kb as? View)?.height ?: -1)
            )
        }
    }

    /** 按下并输入 */
    private fun press(kb: Any, cell: Any, id: Int) {
        si(cell, "pointerId", id)
        showPress(kb, cell)
        haptic(kb)
        inputCell(kb, cell)
        invalidate(kb)
    }

    /** 滑到新格：只有滑进新格的**中心 2/3 圆**才换格输入；同一格内保持按压，滑到外面只取消按压 */
    private fun moveTo(kb: Any, id: Int, cell: Any?, inZone: Boolean) {
        val pressed = pressedCell(kb, id)
        if (cell != null && cell === pressed) return
        if (pressed != null) cancelPress(kb, pressed)
        if (cell != null && inZone && !isSideCell(cell)) press(kb, cell, id) else invalidate(kb)
    }

    /** 松开一根手指 */
    private fun release(kb: Any, id: Int) {
        val pressed = pressedCell(kb, id) ?: return
        cancelPress(kb, pressed)
        if (kb is View && kb.isEnabled && !kb.hasOnClickListeners()) {
            callLoose(kb, "setTouchSoundFeedBack", 0)
        }
        invalidate(kb)
    }

    /** 把我们接管过的所有手指都收干净 */
    private fun releaseAll(kb: Any, ev: MotionEvent) {
        runCatching {
            val ids = synchronized(ownedMap) { set(kb).toList() }
            for (id in ids) {
                pressedCell(kb, id)?.let { cancelPress(kb, it) }
                unown(kb, id)
            }
            if (ids.isNotEmpty()) invalidate(kb)
        }
    }

    // ---------------- 借原厂的方法/字段 ----------------

    /**
     * 反射取值**必须沿继承链往上找**：
     * 锁屏上真正被摸到的对象是 `com.oplus.keyguard.security.widget.NumericKeyboard`
     * （`COUINumericKeyboard` 的子类，自己没重写这些方法/字段），
     * 而我们挂的是父类的 `onTouchEvent`。`getDeclaredMethod` 只看当前类 → 全是 null，
     * 表现就是「钩子进来了但什么都不做」。实机日志已证实这一点。
     */
    private fun resolveMethod(c: Class<*>, name: String, params: Array<Class<*>>): java.lang.reflect.Method? {
        var k: Class<*>? = c
        while (k != null && k != Any::class.java) {
            runCatching { k.getDeclaredMethod(name, *params) }.getOrNull()?.let {
                runCatching { it.isAccessible = true }
                return it
            }
            k = k.superclass
        }
        return null
    }

    private fun resolveField(c: Class<*>, name: String): java.lang.reflect.Field? {
        var k: Class<*>? = c
        while (k != null && k != Any::class.java) {
            runCatching { k.getDeclaredField(name) }.getOrNull()?.let {
                runCatching { it.isAccessible = true }
                return it
            }
            k = k.superclass
        }
        return null
    }

    private val methodCache = HashMap<String, java.lang.reflect.Method?>()
    private val fieldCache = HashMap<String, java.lang.reflect.Field?>()

    private fun mth(o: Any, name: String, params: Array<Class<*>>): java.lang.reflect.Method? {
        val key = o.javaClass.name + "#" + name + "/" + params.size
        synchronized(methodCache) { if (methodCache.containsKey(key)) return methodCache[key] }
        val m = resolveMethod(o.javaClass, name, params)
        synchronized(methodCache) { methodCache[key] = m }
        return m
    }

    private fun fld(o: Any, name: String): java.lang.reflect.Field? {
        val key = o.javaClass.name + "#" + name
        synchronized(fieldCache) { if (fieldCache.containsKey(key)) return fieldCache[key] }
        val f = resolveField(o.javaClass, name)
        synchronized(fieldCache) { fieldCache[key] = f }
        return f
    }

    private fun callObj(o: Any, name: String, params: Array<Class<*>>, args: Array<Any?>): Any? =
        runCatching { mth(o, name, params)?.invoke(o, *args) }.getOrNull()

    private fun callVoid(o: Any, name: String, params: Array<Class<*>>, args: Array<Any?>) {
        runCatching { mth(o, name, params)?.invoke(o, *args) }
    }

    /**
     * 按「名字 + 参数个数」找方法（不比对参数类型）。
     *
     * 为什么要这个：`initShowAnimator(Cell)` 这类方法的形参是父类的 `Cell`，
     * 而实机上格子对象可能是子类 → 用 `arrayOf(cell.javaClass)` 当签名会找不到方法，
     * 结果只有「按压动画不见了」这种莫名其妙的半残。按名字+个数取最稳。
     */
    private fun mthLoose(o: Any, name: String, argc: Int): java.lang.reflect.Method? {
        val key = o.javaClass.name + "#" + name + "~" + argc
        synchronized(methodCache) { if (methodCache.containsKey(key)) return methodCache[key] }
        var found: java.lang.reflect.Method? = null
        var k: Class<*>? = o.javaClass
        while (k != null && k != Any::class.java) {
            found = k.declaredMethods.firstOrNull { it.name == name && it.parameterCount == argc }
            if (found != null) {
                runCatching { found!!.isAccessible = true }
                break
            }
            k = k.superclass
        }
        synchronized(methodCache) { methodCache[key] = found }
        return found
    }

    private fun callLoose(o: Any, name: String, argc: Int, vararg args: Any?) {
        runCatching { mthLoose(o, name, argc)?.invoke(o, *args) }
    }

    private fun gi(o: Any?, name: String, def: Int = -1): Int =
        runCatching { fld(o!!, name)!!.getInt(o) }.getOrDefault(def)

    private fun si(o: Any?, name: String, v: Int) {
        runCatching { fld(o!!, name)!!.setInt(o, v) }
    }

    private fun gb(o: Any?, name: String, def: Boolean = false): Boolean =
        runCatching { fld(o!!, name)!!.getBoolean(o) }.getOrDefault(def)

    /** 原厂的行列命中测试：这一下按在第几格（格子外返回 null） */
    private fun hitCell(kb: Any, x: Float, y: Float): Any? =
        runCatching { mthLoose(kb, "checkForNewHit", 2)?.invoke(kb, x, y) }.getOrNull()

    /** 原厂记录的「这根手指正按着哪一格」 */
    private fun pressedCell(kb: Any, id: Int): Any? =
        if (id < 0) null else runCatching { mthLoose(kb, "findCellByPointerId", 1)?.invoke(kb, id) }.getOrNull()

    /** 格子编号（原厂就是 row*3+column）：0~8 = 1~9，9 = 左键，10 = 0，11 = 右键 */
    private fun idxOf(cell: Any?): Int {
        if (cell == null) return -1
        return gi(cell, "row", 0) * 3 + gi(cell, "column", 0)
    }

    private fun isSideCell(cell: Any?): Boolean {
        val i = idxOf(cell)
        return i == 9 || i == 11
    }

    /** 原厂的按压/取消动画（样式 0 = 涟漪，1 = 光效） */
    private fun showPress(kb: Any, cell: Any) {
        when (gi(kb, "mPressEffectStyle", 0)) {
            0 -> callLoose(kb, "initShowAnimator", 1, cell)
            1 -> callLoose(kb, "executeLightEffectAnimator", 2, cell, true)
        }
    }

    private fun cancelPress(kb: Any, cell: Any) {
        when (gi(kb, "mPressEffectStyle", 0)) {
            0 -> callLoose(kb, "initFadeAnimator", 1, cell)
            1 -> callLoose(kb, "executeLightEffectAnimator", 2, cell, false)
        }
        si(cell, "pointerId", -1)
    }

    private fun inputCell(kb: Any, cell: Any) {
        val i = idxOf(cell)
        if (i in 0..8 || i == 10) {
            callLoose(kb, "callback", 1, i)
            SLog.r(TAG, "slide in idx=$i", 800L)
        }
    }

    private fun haptic(kb: Any) {
        if (gb(kb, "mEnableHapticFeedback")) {
            callLoose(kb, "setTouchFeedback", 0)
        }
    }

    private fun touchExploration(kb: Any): Boolean = runCatching {
        val am = fld(kb, "mAccessibilityManagerService")?.get(kb) ?: return false
        am.javaClass.getMethod("isTouchExplorationEnabled").invoke(am) as? Boolean == true
    }.getOrDefault(false)

    private fun invalidate(kb: Any) {
        runCatching { (kb as? View)?.invalidate() }
    }
}
