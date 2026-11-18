package com.oupao.wiz

import android.content.Context
import android.view.ViewGroup

/**
 * 在 ColorOS 的「通知与控制中心」设置页（`com.oplus.notificationmanager`，进程 `:ui`）里，
 * 插一张**独立的卡片**「通知收纳」，卡片里是两个开关（通知收纳 / 自动收纳静默通知）。
 *
 * ## 为什么是"同页卡片"而不是"新建一页"
 * 用户要的是"像设置页自己的页"，但**真 fragment 子页做不到**：LSPosed 给模块的是独立 ClassLoader，
 * 我们的 Fragment 不是目标 App 的 `androidx.fragment.Fragment`，`FragmentManager` 直接拒收
 * （真机日志 `NBX-FRG: 不是 FragmentActivity`，compileOnly 也一样）。所以把开关直接放进这一页，
 * 用系统自己的 `COUIPreferenceCategory` + `COUISwitchPreference`，外观/交互与原生一致，且不用跳页。
 *
 * ## 落点（本机 dump 实证）
 * - 页面：`NotificationCenterActivity` + `fragments.main.NotificationCenterFragment`，标准 PreferenceScreen；
 * - 建树：`BasePreferenceFragment#onCreatePreferences(Bundle,String)`；
 * - 锚点：`key_lock_screen_notification` 所在的那张卡（我们插到它前面）。
 *
 * ## 数据通道
 * 本进程 uid=1000 但**写不了模块 App 的私有目录**（`Cfg.save()` 在这边等于白写），
 * 所以开关直接写 `Settings.Global` 的 `oupao_oupao_<key>` —— 这正是 `Cfg.notifBox()/notifBoxAuto()`
 * 的第一优先数据源，SystemUI 与模块 UI 都会立刻读到新值。
 */
object NotifBoxNc {

    private const val TAG = "NBX-NC"
    private const val KEY_ANCHOR = "key_lock_screen_notification"
    private const val KEY_GROUP = "app_notification_settings"
    private const val KEY_CAT = "oupao_notif_box_cat"
    private const val G_SCROLL = "oupao_notifbox_scroll"

    fun install(cl: ClassLoader?): Int {
        val f = runCatching { Rt.cls(cl, "com.oplus.notificationmanager.fragments.BasePreferenceFragment") }
            .getOrNull()
        if (f == null) {
            SLog.i("$TAG: 找不到 BasePreferenceFragment（设置页入口不生效）")
            return 0
        }
        var n = hookCreate(f, cl)
        n += hookResume(f)
        n += hookRecycler(cl)
        // ★ 掉帧优化（3075）：`hookAdapter` 是**纯诊断**，钉在 `PreferenceGroupAdapter#getItemCount`
        //   这个超热方法上。RecyclerView 滚动时它每帧要被调几十次，而 Xposed 每次调用都要
        //   构造一个 XMP 回调对象 + 跑我们的 lambda（里面还要反射取 mPreferenceList、拼字符串）
        //   → 每秒几百次分配 → GC → 设置页滚动掉帧。
        //   **默认根本不挂**；只有开了 `oupao_nbx_dbg=1` 才会在页面建好后补挂（见 inject()）。
        SLog.i("$TAG: 设置页钩子=$n（adapter 诊断钩子按需挂）")
        return n
    }

    /**
     * 诊断：adapter 到底把哪些 Preference 当成列表项。
     * 用来判断"运行期新建的 category + 子项"是**没进 flattened list**，还是进了但渲染成 0 高。
     */
    private fun hookAdapter(cl: ClassLoader?): Int {
        val c = runCatching { Rt.cls(cl, "androidx.preference.PreferenceGroupAdapter") }.getOrNull() ?: return 0
        val m = runCatching { Rt.methodByName(c, "getItemCount", 0) }.getOrNull() ?: return 0
        runCatching { m.isAccessible = true }
        val cb = object : XMH() {
            override fun afterHookedMethod(param: XMP) {
                if (!dbg) return      // 非调试模式：一次布尔读就退出（这条钩子本来就不该挂）
                runCatching {
                    val n = param.result as? Int ?: return
                    val list = Ref.get(param.thisObject, "mPreferenceList") as? List<*> ?: return
                    val sig = "$n/" + list.size
                    if (sig == lastAdapterSig) return
                    lastAdapterSig = sig
                    val sb = StringBuilder()
                    for ((i, p) in list.withIndex()) {
                        if (i >= 45) { sb.append(" …"); break }
                        if (i > 0) sb.append(' ')
                        sb.append(i).append(':').append(p?.javaClass?.simpleName)
                        val k = Rt.callNoArgQ(p, "getKey")
                        if (k != null) sb.append('/').append(k)
                        if (Rt.callNoArgQ(p, "isVisible") == false) sb.append("(hid)")
                    }
                    SLog.i("$TAG: adapter items=$n list=${list.size} [$sb]")
                }
            }
        }
        val ok = XBridge.hookMethod(m, cb) != null
        if (ok) SLog.i("$TAG: 已挂钩 PreferenceGroupAdapter.getItemCount（诊断）")
        return if (ok) 1 else 0
    }

    @Volatile
    private var lastAdapterSig = ""

    /** adapter 诊断钩子是否装过（只在调试模式下补挂一次） */
    private val adapterHookTried = java.util.concurrent.atomic.AtomicBoolean(false)

    @Volatile
    private var dbg = false

    /** 建树后插卡片 */
    private fun hookCreate(f: Class<*>, cl: ClassLoader?): Int {
        val m = runCatching { Rt.methodByName(f, "onCreatePreferences", 2) }.getOrNull() ?: return 0
        runCatching { m.isAccessible = true }
        val cb = object : XMH() {
            override fun afterHookedMethod(param: XMP) {
                runCatching { inject(param.thisObject, cl) }
                    .onFailure { SLog.e("$TAG: 插行失败", it) }
            }
        }
        return if (XBridge.hookMethod(m, cb) != null) 1 else 0
    }

    /** resume 之后再滚动 + 闪烁（此时 RecyclerView 已布局，View.post 可靠；比 Handler 定时器稳） */
    private fun hookResume(f: Class<*>): Int {
        val m = runCatching { Rt.methodByName(f, "onResume", 0) }.getOrNull() ?: return 0
        runCatching { m.isAccessible = true }
        val cb = object : XMH() {
            override fun afterHookedMethod(param: XMP) {
                val frag = param.thisObject ?: return
                if (!frag.javaClass.name.contains("NotificationCenterFragment")) return
                if (takeScrollFlag(frag)) {
                    pendingScroll = true
                    SLog.i("$TAG: 等 RecyclerView 布局完再滚")
                }
                // 诊断（`oupao_nbx_dbg=1` 才开）：resume 之后再查一次树 + adapter
                if (dbg) runCatching {
                    val v = Rt.callNoArg(frag, "getView") as? android.view.View
                    v?.postDelayed({
                        runCatching {
                            SLog.i("$TAG: --- resume 诊断 ---")
                            dumpTree(Rt.callNoArg(frag, "getPreferenceScreen"), 0)
                            diagAdapter(Rt.callNoArg(frag, "getListView") as? ViewGroup)
                        }
                    }, 700L)
                }
            }
        }
        return if (XBridge.hookMethod(m, cb) != null) 1 else 0
    }

    // ---------------- 插卡片 ----------------

    private fun inject(frag: Any?, cl: ClassLoader?) {
        if (frag == null) return
        if (!frag.javaClass.name.contains("NotificationCenterFragment")) return
        val ctx = (Rt.callNoArg(frag, "getContext") as? Context)
            ?: (Rt.callNoArg(frag, "getActivity") as? Context) ?: return
        val screen = Rt.callNoArg(frag, "getPreferenceScreen") ?: return
        injectedScreen = screen
        dbg = runCatching {
            android.provider.Settings.Global.getInt(ctx.contentResolver, "oupao_nbx_dbg", 0) == 1
        }.getOrDefault(false)
        // 调试开关打开时，才补挂那条超热的 adapter 诊断钩子（默认不挂，见 install）。
        if (dbg && adapterHookTried.compareAndSet(false, true)) {
            SLog.i("$TAG: 调试模式 → 补挂 adapter 诊断钩子 n=" + hookAdapter(cl))
        }

        if (Rt.callLoose(screen, "findPreference", arrayOf<Any?>(KEY_CAT)) != null) return   // 幂等

        val anchor = Rt.callLoose(screen, "findPreference", arrayOf<Any?>(KEY_ANCHOR))
        val anchorGroup = (anchor?.let { runCatching { Rt.callNoArg(it, "getParent") }.getOrNull() })
        SLog.i("$TAG: 结构 screen=" + screen.javaClass.name + " group=" + anchorGroup?.javaClass?.name + " anchor=" + anchor?.javaClass?.name)
            ?: Rt.callLoose(screen, "findPreference", arrayOf<Any?>(KEY_GROUP))

        val rows = listOf(
            Triple("通知收纳", "长按通知即可收纳，收纳后彻底静默；点通知面板顶部的收纳图标可翻回来", Cfg.K_NOTIF_BOX),
            Triple("自动收纳静默通知", "静默通知自动进收纳箱，重要通知照常提醒", Cfg.K_NOTIF_BOX_AUTO)
        )
        // ================= 卡片分组：反编译 + 真机实证（别再试别的写法） =================
        // 真机规则（`COUICardListHelper.getPositionInGroup(Preference)` 反编译，本机 dexdump 实证）：
        //   取 `pref.getParent()` 的**可见**子项列表 → 看紧邻的前/后兄弟，`isSupportCard(group, sibling)`
        //   为真才算"同卡邻居"：prev 支持 → MIDDLE/TAIL；prev 不支持 → HEAD/FULL；next 不支持 → 断卡。
        //   `isSupportCard`：group 是 PreferenceScreen 时看 `COUICardSupportInterface.isSupportCardUse()`
        //   （**默认 false = 支持连卡**，XML 里写 true 的反而**不连**）；group 不是 screen 时，
        //   兄弟是 PreferenceCategory → 不支持（标题行天然断卡），其它 → 支持。
        // ⇒ 结论（3055 真机验证过）：
        //   a. 两行必须在**同一个 PreferenceScreen 下当兄弟**才会连成一张卡；塞进一个 Category 里虽然
        //      父组只有彼此，但 `getPositionInGroup` 走的另一分支，实测渲染成两张独立圆角卡。
        //   b. 想在中间断卡，就插一条 **PreferenceCategory（标题行）** —— 它对邻居恒"不支持"。
        //   c. ★ 运行期新建的 Preference **必须显式 setOrder**：默认 order = DEFAULT(MAX)，页面随后
        //      动态补内容（应用列表那 100 行）时会按 order 重排 → 我们整块被甩到页面**最末尾**
        //      （3050–3054 的"完全不渲染"其实是在最底部，滚到底才看得到）。
        //   d. `setIsSupportCardUse(true)` 会让它**不**连卡，别设（保持默认 false）。
        val baseOrder = (anchorGroup?.let { runCatching { Rt.callNoArg(it, "getOrder") }.getOrNull() } as? Int)
            ?: (anchor?.let { runCatching { Rt.callNoArg(it, "getOrder") }.getOrNull() } as? Int)
            ?: Int.MAX_VALUE
        // 锚点那张卡（③）的 order 减 1：正好落在 ② 分类设置 与 ③ 之间（order 相同则按插入顺序追加）
        val sectionOrder = if (baseOrder == Int.MAX_VALUE) Int.MAX_VALUE - 100 else baseOrder - 1

        val section = newSectionCategory(ctx, cl, "Flyme 式通知收纳")
        val added = ArrayList<Any?>()
        val inserts = ArrayList<Any?>()
        if (section != null) {
            runCatching { Rt.callLoose(section, "setOrder", arrayOf<Any?>(sectionOrder)) }
            inserts.add(section)
        }
        for ((i, row) in rows.withIndex()) {
            val (title, summary, key) = row
            val pref = newSwitchPreference(ctx, cl, title, summary, key) ?: continue
            // `isSupportCardUse = true` 才会被当成"可以连卡"的邻居（真机实测：false 时两行各自 FULL 成两张卡）
            runCatching { Rt.callLoose(pref, "setIsSupportCardUse", arrayOf<Any?>(true)) }
            runCatching { Rt.callLoose(pref, "setOrder", arrayOf<Any?>(sectionOrder)) }
            inserts.add(pref)
            added.add(pref)
        }
        // 一律挂在 screen 上（不是塞进 ③），然后整体挪到 ③ 前面
        for (p in inserts) {
            val okAdd = runCatching { Rt.callLoose(screen, "addPreference", arrayOf<Any?>(p)) }.isSuccess
            SLog.i("$TAG: 已插入 " + p?.javaClass?.simpleName + " add=$okAdd order=$sectionOrder")
        }
        val host = anchorGroup ?: anchor
        if (host != null && inserts.isNotEmpty()) {
            SLog.i("$TAG: 挪到锚点卡前 ok=" + moveBefore(screen, inserts, host))
        }
        // ★ 关键：把 screen 顶层子项的 order 重排成「当前列表顺序」0..n-1。
        //   COUI 的适配器在页面后续动态补内容时会按 order 重排，运行期插入的项若 order 与别人相同
        //   （或都是 DEFAULT=MAX）就会出现"标题跑到上一张卡前面 / 两行顺序颠倒"这类漂移。
        //   重排后 order 与列表顺序严格一致 → 无论谁再排都不会动。
        renumber(screen)
        runCatching { Rt.callLoose(screen, "notifyHierarchyChanged", null) }
        runCatching { if (dbg) dumpTree(screen, 0) }

        // 诊断（`oupao_nbx_dbg=1` 才开）：2s 后再看一次，确认节点还在、order 没被重排
        if (dbg) runCatching {
            val act = activityOf(ctx)
            act?.window?.decorView?.postDelayed({
                runCatching {
                    SLog.i("$TAG: --- 2s 诊断 ---")
                    dumpTree(Rt.callNoArg(frag, "getPreferenceScreen"), 0)
                    val helperCls = Rt.cls(cl, "com.coui.appcompat.cardlist.COUICardListHelper")
                    val prefCls = Rt.cls(cl, "androidx.preference.Preference")
                    val posM = if (helperCls != null && prefCls != null) {
                        Ref.method(helperCls, "getPositionInGroup", arrayOf(prefCls))
                    } else null
                    for (p in added) {
                        val pos = Ref.invokeQ(posM, null, arrayOf<Any?>(p))
                        SLog.i(
                            "$TAG: pos key=" + Rt.callNoArgQ(p, "getKey") +
                                " cardPos=" + pos + " supportUse=" + Rt.callNoArgQ(p, "isSupportCardUse") +
                                " parent=" + (Rt.callNoArgQ(p, "getParent"))?.javaClass?.simpleName
                        )
                    }
                    diagAdapter(Rt.callNoArg(frag, "getListView") as? ViewGroup)
                }
            }, 2000L)
        }
    }

    /** 把 [group] 的子项 order 重排成当前列表顺序 0..n-1（让 order 与视觉顺序一致，抗后续重排） */
    private fun renumber(group: Any) {
        runCatching {
            val list = Ref.get(group, "mPreferences") as? List<*> ?: return
            for ((i, p) in list.withIndex()) {
                runCatching { Rt.callLoose(p, "setOrder", arrayOf<Any?>(i)) }
            }
            SLog.i("$TAG: 已重排 order n=" + list.size)
        }
    }

    /** 诊断：把 Preference 树打出来（层级 + 类名 + key + 可见性 + 子项数） */
    private fun dumpTree(group: Any?, depth: Int) {
        if (group == null || depth > 3) return
        val n = (Rt.callNoArg(group, "getPreferenceCount") as? Int) ?: return
        for (i in 0 until n) {
            val p = Rt.callLoose(group, "getPreference", arrayOf<Any?>(i)) ?: continue
            val key = Rt.callNoArg(p, "getKey")
            val vis = Rt.callNoArg(p, "isVisible")
            val ord = Rt.callNoArg(p, "getOrder")
            val cc = (Rt.callNoArg(p, "getPreferenceCount") as? Int) ?: -1
            SLog.i(
                "$TAG: tree " + "  ".repeat(depth) + i + " " + p.javaClass.simpleName +
                    " key=" + key + " order=" + ord + " vis=" + vis + " children=" + cc
            )
            if (cc > 0) dumpTree(p, depth + 1)
        }
    }

    /** 把 [items] 挪到 [before] 之前（直接操作 PreferenceGroup 的 mPreferences，保持相邻） */
    private fun moveBefore(group: Any, items: List<Any?>, before: Any): Boolean = runCatching {
        val list = Ref.get(group, "mPreferences") as? MutableList<Any?> ?: return false
        list.removeAll(items.toSet())
        val at = list.indexOf(before)
        if (at < 0) return false
        list.addAll(at, items)
        SLog.i("$TAG: 已挪到 anchor 前 at=" + at + " n=" + items.size)
        true
    }.getOrDefault(false)

    /**
     * 分卡用的灰色小节标题行 = 页面自己那套 `CustomCategory`
     * （`extends COUIPreferenceCategory`，ctor 里 `setLayoutResource(R.layout.category_layout)`，
     *  `onBindViewHolder` 把字段 `mTitleString` 塞进 `id/category_title`、并把 `mMarginTop` 设成上边距）。
     * 拿不到 `CustomCategory` 就退回 `COUIPreferenceCategory`（用标准 `setTitle(CharSequence)`）。
     *
     * 它是 `PreferenceCategory` ⇒ `COUICardListHelper.isSupportCard` 恒 false ⇒ 天然把上下两张卡断开，
     * 我们的两行作为它的子项 ⇒ 父组里只有彼此 ⇒ 自成一张卡。
     */
    private fun newSectionCategory(ctx: Context, cl: ClassLoader?, title: String): Any? {
        val c = runCatching { Rt.cls(cl, "com.oplus.notificationmanager.view.CustomCategory") }.getOrNull()
            ?: runCatching { Rt.cls(cl, "com.coui.appcompat.preference.COUIPreferenceCategory") }.getOrNull()
            ?: return null
        val cat = newWithCtor(c, ctx) ?: return null
        setField(cat, "mTitleString", title)
        setField(cat, "mMarginTop", (8 * ctx.resources.displayMetrics.density).toInt())
        setText(cat, "setTitle", title)
        runCatching { Rt.callLoose(cat, "setKey", arrayOf<Any?>(KEY_CAT)) }
        runCatching { Rt.callLoose(cat, "setPersistent", arrayOf<Any?>(false)) }
        return cat
    }

    /** 写实例字段（沿继承链找，失败只记日志） */
    private fun setField(target: Any, name: String, value: Any?) {
        var c: Class<*>? = target.javaClass
        while (c != null) {
            val f = runCatching { c!!.getDeclaredField(name).apply { isAccessible = true } }.getOrNull()
            if (f != null) {
                runCatching { f.set(target, value) }
                    .onFailure { SLog.e("$TAG: 写字段失败 $name", it) }
                return
            }
            c = c.superclass
        }
        SLog.i("$TAG: 找不到字段 $name")
    }

    private fun newSwitchPreference(
        ctx: Context,
        cl: ClassLoader?,
        title: String,
        summary: String,
        key: String
    ): Any? {
        val c = runCatching { Rt.cls(cl, "com.coui.appcompat.preference.COUISwitchPreference") }.getOrNull()
            ?: runCatching { Rt.cls(cl, "androidx.preference.SwitchPreference") }.getOrNull()
            ?: return null
        val pref = newWithCtor(c, ctx) ?: return null

        setText(pref, "setTitle", title)
        setText(pref, "setSummary", summary)
        runCatching { Rt.callLoose(pref, "setKey", arrayOf<Any?>(key + "_page")) }
        runCatching { Rt.callLoose(pref, "setPersistent", arrayOf<Any?>(false)) }
        // 3145：配置权威源是远程 prefs（LSPosed 直读 App 那份 oupao_cfg.xml），
        // 本进程也在作用域里 → Cfg.notifBox() 走 prefs，不再依赖 Settings.Global 抢答。
        val on = runCatching {
            if (key == Cfg.K_NOTIF_BOX) Cfg.notifBox() else Cfg.notifBoxAuto()
        }.getOrDefault(false)
        runCatching { Rt.callLoose(pref, "setChecked", arrayOf<Any?>(on)) }

        runCatching {
            val ifaceCls = Rt.cls(cl, "androidx.preference.Preference\$OnPreferenceChangeListener")
                ?: return@runCatching
            val proxy = java.lang.reflect.Proxy.newProxyInstance(
                ifaceCls.classLoader, arrayOf<Class<*>>(ifaceCls)
            ) { _, m, a ->
                if (m.name == "onPreferenceChange") {
                    val v = a?.getOrNull(1) as? Boolean ?: false
                    onSwitchChanged(ctx, key, v)
                    runCatching { Rt.callLoose(a?.getOrNull(0), "setChecked", arrayOf<Any?>(v)) }
                    true
                } else null
            }
            val setter = pref.javaClass.methods.firstOrNull {
                it.name == "setOnPreferenceChangeListener" && it.parameterTypes.size == 1
            }
            setter?.isAccessible = true
            setter?.invoke(pref, proxy)
        }
        return pref
    }

    /** 3145：写权威 prefs（跨进程同步），并镜像一份 Settings.Global 兼容老读法 */
    private fun onSwitchChanged(ctx: Context, key: String, v: Boolean) {
        val ok = runCatching { Cfg.setBool(key, v) }.getOrDefault(false)
        if (!ok) {
            // 远程 prefs 不可用（老框架）→ 退回原行为：写 Global
            runCatching {
                android.provider.Settings.Global.putInt(ctx.contentResolver, "oupao_" + key, if (v) 1 else 0)
            }.onFailure { SLog.e("$TAG: 写 Settings.Global 失败 $key", it) }
        }
        SLog.i("$TAG: 设置页开关 $key=$v prefs=$ok")
    }

    /** 按常见 Preference 构造签名依次试：(Context) / (Context, AttributeSet) / (Context, AttributeSet, int) */
    private fun newWithCtor(c: Class<*>, ctx: Context): Any? {
        val asCls = android.util.AttributeSet::class.java
        val sigs = listOf(
            arrayOf<Class<*>>(Context::class.java) to arrayOf<Any?>(ctx),
            arrayOf<Class<*>>(Context::class.java, asCls) to arrayOf<Any?>(ctx, null),
            arrayOf<Class<*>>(Context::class.java, asCls, Integer.TYPE) to arrayOf<Any?>(ctx, null, 0)
        )
        for ((sig, args) in sigs) {
            val v = runCatching {
                val ctor = c.getDeclaredConstructor(*sig)
                ctor.isAccessible = true
                ctor.newInstance(*args)
            }.getOrNull()
            if (v != null) return v
        }
        return null
    }

    /** 按「名字 + 参数是 CharSequence」挑重载（COUI 同时有 setTitle(int)，按个数找会撞） */
    private fun setText(target: Any, name: String, value: String) {
        val cs = CharSequence::class.java
        val m = runCatching {
            target.javaClass.methods.firstOrNull { mm ->
                mm.name == name && mm.parameterTypes.size == 1 && mm.parameterTypes[0].isAssignableFrom(cs)
            }
        }.getOrNull()
        if (m == null) {
            SLog.i("$TAG: 找不到 $name(CharSequence)")
            return
        }
        runCatching { m.isAccessible = true; m.invoke(target, value) }
            .onFailure { SLog.e("$TAG: $name 调用失败", it) }
    }

    // ---------------- 打开后滚到卡片并闪一下 ----------------

    /** Preference 的 Context 是 ContextThemeWrapper，沿链剥到 Activity */
    private fun activityOf(c: Context?): android.app.Activity? {
        var x = c
        while (x != null) {
            if (x is android.app.Activity) return x
            x = (x as? android.content.ContextWrapper)?.baseContext
        }
        return null
    }

    /**
     * 真正可靠的时机：**RecyclerView 每次 onLayout 之后**（这时行已经量好、也一定在主线程）。
     * 之前用 Handler / View.post 轮询拿到的是 `0,0-0,0` 的未 attach 实例，回调根本不会执行。
     */
    private fun hookRecycler(cl: ClassLoader?): Int {
        val c = runCatching { Rt.cls(cl, "androidx.recyclerview.widget.RecyclerView") }.getOrNull() ?: return 0
        val m = runCatching { Rt.methodByName(c, "onLayout", 5) }.getOrNull() ?: return 0
        runCatching { m.isAccessible = true }
        val cb = object : XMH() {
            override fun afterHookedMethod(param: XMP) {
                // ★ 掉帧优化（3075）：非调试 + 不需要滚动时，这里只做一次布尔判断就返回。
                //   以前是「每次 onLayout 都 runCatching{diagAdapter(rv)}」，
                //   RecyclerView 的每次布局都会进来（设置页里 RecyclerView 不止一个），白白付反射/分配成本。
                if (!pendingScroll && !dbg) return
                if (dbg) runCatching { diagAdapter(param.thisObject as? ViewGroup) }
                if (!pendingScroll) return
                val rv = param.thisObject as? ViewGroup ?: return
                runCatching { tryScrollNow(rv) }
                    .onFailure { SLog.e("$TAG: 滚动失败", it) }
            }
        }
        val ok = XBridge.hookMethod(m, cb) != null
        if (ok) SLog.i("$TAG: 已挂钩 RecyclerView.onLayout（用来滚到收纳卡片）")
        return if (ok) 1 else 0
    }

    // ---------------- 诊断：可见的 adapter 里到底有什么 ----------------

    @Volatile
    private var injectedScreen: Any? = null

    @Volatile
    private var lastDiagAt = 0L

    /**
     * 关键诊断：adapter 的 item 数 + 它自己持有的 `mPreferenceScreen` 身份 + flattened list。
     * 用来区分两种失败：
     *   a) 树里有我们的节点，但 adapter 的 `mPreferenceScreen` **不是**同一棵树（页面被重建/两个 fragment 实例）；
     *   b) 树和 adapter 都是同一棵，只是我们的节点没进 flattened list。
     */
    private fun diagAdapter(rv: ViewGroup?) {
        if (rv == null) return
        val now = System.currentTimeMillis()
        if (now - lastDiagAt < 1500) return
        lastDiagAt = now
        val ad = runCatching { rv.javaClass.getMethod("getAdapter").invoke(rv) }.getOrNull() ?: return
        val cnt = runCatching { ad.javaClass.getMethod("getItemCount").invoke(ad) }.getOrNull()
        val adScreen = Ref.get(ad, "mPreferenceScreen")
        val list = Ref.get(ad, "mPreferenceList") as? List<*>
        SLog.i(
            "$TAG: adapter cls=${ad.javaClass.name} items=$cnt rvChildren=${rv.childCount} " +
                "adScreen=${System.identityHashCode(adScreen)} injScreen=${System.identityHashCode(injectedScreen)} " +
                "listN=${list?.size}"
        )
        if (list != null) {
            val sb = StringBuilder()
            for ((i, p) in list.withIndex()) {
                if (i >= 40) { sb.append(" …"); break }
                if (i > 0) sb.append(' ')
                sb.append(i).append(':').append(p?.javaClass?.simpleName)
                val k = Rt.callNoArgQ(p, "getKey")
                if (k != null) sb.append('/').append(k)
            }
            SLog.i("$TAG: adapter list [$sb]")
        }
    }

    @Volatile
    private var pendingScroll = false

    /** 我们的两行（找到就是"我们那张卡"里的行） */
    private val OUR_TITLES = arrayOf("通知收纳", "自动收纳静默通知")

    private fun tryScrollNow(rv: ViewGroup?) {
        if (rv == null) return
        val rows = OUR_TITLES.mapNotNull { findRowWithText(rv, it) }
        if (rows.isEmpty()) return
        // 1) 把"卡片"顶端滚到视口顶部（用第一行的 top 算，保证整张卡都在屏幕内）
        val first = rows.first()
        runCatching {
            val dy = first.top - rv.paddingTop
            if (dy != 0) rv.javaClass.getMethod("scrollBy", Integer.TYPE, Integer.TYPE).invoke(rv, 0, dy)
        }
        pendingScroll = false
        SLog.i("$TAG: 已滚到收纳卡片 rows=" + rows.size + " dy=" + (first.top - rv.paddingTop))
        // 2) 滚动落定后，整张卡（两行）一起闪
        rv.postDelayed({
            runCatching {
                val again = OUR_TITLES.mapNotNull { findRowWithText(rv, it) }
                for (row in again) flashRow(row)
            }
        }, 320L)
    }
    /** 一次性标记：模块那一行点进来时写 1，这里读走并清零 */
    private fun takeScrollFlag(frag: Any): Boolean = runCatching {
        val ctx = Rt.callNoArg(frag, "getContext") as? Context ?: return false
        val v = runCatching {
            android.provider.Settings.Global.getInt(ctx.contentResolver, G_SCROLL, 0)
        }.getOrDefault(0)
        if (v != 1) return false
        runCatching { android.provider.Settings.Global.putInt(ctx.contentResolver, G_SCROLL, 0) }
        SLog.i("$TAG: 检测到 scroll 标记")
        true
    }.getOrDefault(false)

    /**
     * ⚠ 坑：onResume 时 `getListView()` 拿到的是**还没 attach、尺寸 0x0 的 RecyclerView**
     * （实测 `COUIPercentWidthRecyclerView ... 0,0-0,0`），对它 `post` 的 Runnable 排在 ViewRootImpl
     * 的 run queue 里，而这个实例随后被真正的那一个替换掉 → **永远不执行**。
     * 所以改成把轮询挂在 **Activity 的 decorView**（已 attach、生命周期长）上，每次重新取 ListView。
     */
    private fun scrollAndFlash(frag: Any) {
        val act = activityOf(Rt.callNoArg(frag, "getContext") as? Context)
            ?: activityOf(Rt.callNoArg(frag, "getActivity") as? Context)
        val host: android.view.View = act?.window?.decorView
            ?: (runCatching { Rt.callNoArg(frag, "getView") as? android.view.View }.getOrNull())
            ?: run {
                SLog.i("$TAG: 拿不到宿主 View，跳过滚动")
                return
            }
        android.util.Log.i("OUPAO", "NBX-NC: scroll host=" + host)
        step(frag, host, 0)
    }

    private fun step(frag: Any, host: android.view.View, attempt: Int) {
        android.util.Log.i("OUPAO", "NBX-NC: step schedule attempt=" + attempt)
        host.postDelayed({
            if (attempt > 25) {
                SLog.i("$TAG: 放弃滚动（找不到那一行）")
                return@postDelayed
            }
            runCatching {
                val rv = Rt.callNoArg(frag, "getListView") as? ViewGroup
                android.util.Log.i("OUPAO", "NBX-NC: step fired attempt=" + attempt + " rv=" + rv)
                val row = rv?.let { findRowWithText(it, "通知收纳") }
                if (rv == null || row == null) {
                    step(frag, host, attempt + 1)
                    return@runCatching
                }
                val pos = runCatching {
                    rv.javaClass.getMethod("getChildAdapterPosition", android.view.View::class.java)
                        .invoke(rv, row) as Int
                }.getOrNull() ?: -1
                if (pos >= 0) runCatching {
                    rv.javaClass.getMethod("smoothScrollToPosition", Integer.TYPE).invoke(rv, pos)
                }
                host.postDelayed({ flashRow(row) }, 260L)
                SLog.i("$TAG: 已滚到「通知收纳」 pos=$pos attempt=$attempt")
            }.onFailure { SLog.e("$TAG: 滚动失败", it) }
        }, if (attempt == 0) 250L else 120L)
    }

    /**
     * 找「通知收纳」那一行：优先**带开关的那一行**（页面里小节标题也叫「通知收纳」，
     * 直接取第一个会命中标题行 —— 实测闪的就是标题，闪错地方）。
     */
    private fun findRowWithText(rv: ViewGroup, text: String): android.view.View? {
        var first: android.view.View? = null
        for (i in 0 until rv.childCount) {
            val child = rv.getChildAt(i) ?: continue
            if (!hasText(child, text)) continue
            if (hasSwitch(child)) return child
            if (first == null) first = child
        }
        return first
    }

    private fun hasSwitch(v: android.view.View): Boolean {
        if (v.javaClass.name.contains("Switch")) return true
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                val c = v.getChildAt(i) ?: continue
                if (hasSwitch(c)) return true
            }
        }
        return false
    }

    private fun hasText(v: android.view.View, text: String): Boolean {
        if (v is android.widget.TextView && v.text?.toString() == text) return true
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                val c = v.getChildAt(i) ?: continue
                if (hasText(c, text)) return true
            }
        }
        return false
    }

    /** 闪三下「按下态 + 半透明圆角遮罩」，提示开关就在这儿 */
    private fun flashRow(row: android.view.View) {
        val ctx = row.context
        val radius = (12 * ctx.resources.displayMetrics.density).toFloat()
        val overlay = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = radius
            setColor(0x26000000)
        }
        for (i in 0 until 3) {
            val on = (i * 460).toLong()
            row.postDelayed({ runCatching { row.isPressed = true; row.foreground = overlay } }, on)
            row.postDelayed({ runCatching { row.isPressed = false; row.foreground = null } }, on + 300L)
        }
    }
}
