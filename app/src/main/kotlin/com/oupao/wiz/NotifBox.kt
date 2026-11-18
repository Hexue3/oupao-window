package com.oupao.wiz

import android.content.Context
import android.service.notification.StatusBarNotification
import android.view.View
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * Flyme 式「通知收纳」（FlymeUI 页 → `oupao_notif_box`，默认关）。
 *
 * ## 用户要的语义（2026-11-07 拍板）
 * - **手动**：长按通知 → 系统那个菜单里多一项「收纳」；
 * - **彻底静默**：收纳后该通知不再出现在通知面板列表，**同时**状态栏图标 / 锁屏 / 横幅也没有；
 * - **入口**：通知面板顶部、时钟日期那一行右侧（铃铛左边）一个按钮；
 * - **收纳箱**：点入口把列表显示在通知面板的**通知列表区域**里，点一条 = 恢复，长按 = 删掉。
 *
 * ## 机制（全部有本机 dex 实证，见 notifbox/FINDINGS-pipeline.md）
 * 1. **隐藏**：`RankingCoordinator$4#shouldFilterOut(NotificationEntry,long)` 是注册进
 *    `NotifPipeline.addPreGroupFilter` 的系统过滤器（语义 `ranking.isSuspended()`）。
 *    我们 hook 它，对「已收纳的 key」直接 `setResult(true)`。
 *    被 pre-group filter 丢掉的条目**不进入渲染列表**，而渲染列表是 SystemUI 侧的唯一真相
 *    （状态栏图标 / 锁屏 / 悬浮横幅 / 通知面板全部由它派生）→ 天然「彻底静默」。
 * 2. **数据不丢**：`NotifCollection.mNotificationSet` 里条目原样保留（filter 只 detach，不删），
 *    所以收纳箱可以直接从 `NotifPipeline.getAllNotifs()` 里按 key 取回，**无需自存快照**。
 * 3. **恢复**：把 key 从集合里删掉 → 触发一次列表重建（异步、安全；
 *    切勿同步递归 rebuild，连 3 次会抛 IllegalStateException 把 SystemUI 崩掉）。
 *    ★ 3359+：重建入口必须跨版本查找 —— SystemUI 16.00.12 **没有**
 *    `NotifPipeline.requestListRebuild(String)`，而 `ShadeListBuilder.rebuildList(String)`
 *    两边都不存在（老代码调它只是静默失败，日志却打「已发出」）。见 [resolveRebuild]。
 * 4. **持久化**：只存 key 列表（`notifbox_list.txt`）+ 宽松身份（`notifbox_loose.txt`）。
 *
 * ## 集合
 * - [keys]      收纳箱里的（面板可见、可恢复）
 * - [looseByKey] 宽松身份兜底 `pkg|通知id|用户id`（exact key 对不上时仍能认人）
 * - ~~dropped~~ **3358 已删除**：那是早期「长按删掉」的集合，界面早没入口了，
 *   但残留数据会继续静默同名 key 的通知、而且收纳盒按设计不显示它 ——
 *   外部用户报的「通知被吞了、收纳盒里又没有」就是这么来的。加载时顺手清掉旧文件。
 *
 * ## 为什么不自己实现一个 NotifFilter
 * 本机 ROM 里 `NotifFilter` 是**抽象类**（不是接口），运行时造不出匿名子类；
 * 而 `RankingCoordinator$4` 本来就注册在 pre-group filter 列表里，
 * 改它的返回值等价于「追加一条规则」，且不用碰私有字段。
 */
object NotifBox {

    /** 独立 tag：`SLog.r` 是按 tag 限流的，重要日志别混进别人的 tag */
    private const val TAG = "NBX"

    /** 落盘文件名（[Cfg.gsetC] 会自动补 `oupao_` 前缀） */
    private const val G_LIST = "notifbox_list"
    /** 3358：已废弃的「删除集」文件名 —— 只在加载时用来删掉历史残留 */
    private const val G_DROP_LEGACY = "notifbox_dropped"
    /** 3356：宽松身份（`pkg|id|user`），防 key 格式不一致导致收纳失效 */
    private const val G_LOOSE = "notifbox_loose"

    private const val COORD_PKG = "com.android.systemui.statusbar.notification.collection.coordinator."
    private const val ENTRY_CLS = "com.android.systemui.statusbar.notification.collection.NotificationEntry"

    /** 收纳箱里的通知 key */
    private val keys: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * ★ 3356：**宽松身份兜底**。外部用户（C16）「点收纳没反应」的根因是
     * `shouldFilterOut` 收到的 entry key 和我们存下的 key 对不上（tag/summary/user
     * 段的差异），exact 匹配永远命中不了。这里额外按 `包名|通知id|用户id` 记一份，
     * 只要这三项一致就认定是**同一条通知** → 照样隐藏。
     *
     * `looseByKey` 记 key→宽松身份（恢复时按 key 一起删）；`looseIds` 是它的值集合（过滤时 O(1)）。
     */
    private val looseByKey: MutableMap<String, String> = java.util.concurrent.ConcurrentHashMap()
    private val looseIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

    @Volatile
    private var keysLoaded = false

    /** NotifPipeline 实例（`RankingCoordinator#attach` 里白拿） */
    @Volatile
    private var pipeline: Any? = null

    // ---------------- 重建通道（跨 SystemUI 版本） ----------------
    //
    // C16 的两套 SystemUI 实测差异（2026-11-12，用户日志）：
    //   · 本机 16.99.12：NotifPipeline 有 `requestListRebuild(String)`；
    //   · 外部 16.00.12：**没有**这个方法；但两边都有
    //     `ShadeListBuilder.rebuildListIfBefore(int)` 和
    //     `NotifCollection.dispatchEventsAndRebuildList(String)`。
    //   老代码主路径失败后去调 `ShadeListBuilder.rebuildList(String)` —— 两边都不存在，
    //   于是「重建已发出」是假日志，收纳后的列表从来没刷新过（对方机器上的核心故障）。

    @Volatile
    private var rebuildMethod: Method? = null

    @Volatile
    private var rebuildTarget: Any? = null

    /** 1 = requestListRebuild(String)；2 = rebuildListIfBefore(int)；3 = dispatchEventsAndRebuildList(String) */
    @Volatile
    private var rebuildKind = 0

    @Volatile
    private var rebuildResolved = false

    /** `NotificationEntry#getKey()` 的缓存（热路径，别每次查方法表） */
    private val keyMethod = ConcurrentHashMap<Class<*>, Method>()

    // ---------------- 开关 ----------------

    @Volatile
    private var onCache = false

    @Volatile
    private var onAt = 0L

    /** SystemUI 读不到模块私有文件 → 走 Settings.Global，800ms 缓存（shouldFilterOut 每条都调） */
    private fun enabled(): Boolean {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - onAt < 800L) return onCache
        onAt = now
        // 自动收纳监听注册失败过就 30 秒后补一次：attach 那一刻 NotifPipeline 内部
        // 的 mNotifCollection 可能还没建好（C16/C17 都是 InvocationTargetException）
        if (!autoListenerDone && now - autoTriedAt > 30_000L) {
            val p = pipeline
            if (p != null) {
                autoTriedAt = now
                runCatching { installAutoListener(p) }
            }
        }
        val v = runCatching { Cfg.notifBox() }.getOrDefault(false)
        if (v != onCache) SLog.i("$TAG: 开关=" + v)
        onCache = v
        return v
    }

    // ---------------- 安装 ----------------

    fun install(cl: ClassLoader?): Int {
        loader = cl
        var n = 0
        n += hookAttach(cl)
        n += hookHideFilter(cl)
        n += hookRemoval(cl)
        n += NotifBoxUi.hookLongPress(cl)
        n += NotifBoxUi.hookHeader(cl)
        SLog.i("$TAG: 安装完成 挂上=$n")
        return n
    }

    @Volatile
    private var loader: ClassLoader? = null

    @Volatile
    private var autoListenerDone = false

    /** 上次尝试注册自动收纳监听的时间（失败后 30s 重试一次） */
    @Volatile
    private var autoTriedAt = 0L

    /**
     * 自动收纳「静默通知」：注册一个 `NotifCollectionListener`（接口，全是 default 方法 → 可用 Proxy），
     * 在 `onEntryAdded` 里判断 importance/ambient，命中就直接进收纳箱。
     *
     * ★ 这里**不要**再 `requestListRebuild`：`onEntryAdded` 是在 `dispatchEventsAndRebuildList` 里派发的，
     *   紧接着系统自己就会重建一次，我们只需要把 key 记进集合。多重启反而会踩 reentrant 保护。
     */
    private fun installAutoListener(p: Any) {
        if (autoListenerDone) return
        val cl = loader ?: return
        val lCls = runCatching {
            Rt.cls(cl, "com.android.systemui.statusbar.notification.collection.notifcollection.NotifCollectionListener")
        }.getOrNull() ?: return
        val proxy = runCatching {
            java.lang.reflect.Proxy.newProxyInstance(lCls.classLoader, arrayOf<Class<*>>(lCls)) { _, m, a ->
                runCatching {
                    if (m.name == "onEntryAdded") maybeAuto(a?.getOrNull(0))
                }
                // ★★ 必须按返回类型给默认值：这个接口里全是 default 方法，
                //   有些 default 方法返回 **boolean**，我们的代理一律返回 null 时
                //   ROM 调用处会 "Expected to unbox a 'boolean' primitive type but was returned null"
                //   → NullPointerException（C16/C17 两份日志里 addCollectionListener 失败的真因）。
                defaultValue(m.returnType)
            }
        }.getOrNull() ?: return
        // ★ 以前这里用 `Rt.callLoose`：它内部把异常吞掉、返回 null，于是外层 runCatching
        //   永远走 onSuccess —— 日志里出现「Ref: invoke FAILED addCollectionListener」
        //   紧接着「自动收纳监听已注册」，等于自己骗自己（C16/C17 两份日志都有）。
        //   改成自己找方法、自己 invoke，并把 InvocationTargetException 的 **targetException** 打出来。
        val mm = runCatching {
            var c: Class<*>? = p.javaClass
            var found: Method? = null
            while (c != null && c != Any::class.java && found == null) {
                found = c.declaredMethods.firstOrNull {
                    it.name == "addCollectionListener" && it.parameterCount == 1
                }
                c = c.superclass
            }
            found?.also { runCatching { it.isAccessible = true } }
        }.getOrNull()
        if (mm == null) {
            autoTriedAt = android.os.SystemClock.elapsedRealtime()
            SLog.e("$TAG: 注册自动收纳监听失败：找不到 addCollectionListener", null)
            return
        }
        try {
            mm.invoke(p, proxy)
            autoListenerDone = true
            SLog.i("$TAG: 自动收纳监听已注册")
        } catch (t: Throwable) {
            autoTriedAt = android.os.SystemClock.elapsedRealtime()
            val cause = (t as? java.lang.reflect.InvocationTargetException)?.targetException ?: t
            SLog.e("$TAG: 注册自动收纳监听失败(" + cause.javaClass.simpleName + ")", cause)
        }
    }

    /** 反射代理用：基本类型必须给默认值；返回 null 会在调用处 unbox 时 NPE */
    private fun defaultValue(t: Class<*>): Any? = when (t) {
        java.lang.Boolean.TYPE -> false
        Integer.TYPE -> 0
        java.lang.Long.TYPE -> 0L
        java.lang.Float.TYPE -> 0f
        java.lang.Double.TYPE -> 0.0
        java.lang.Short.TYPE -> 0.toShort()
        java.lang.Byte.TYPE -> 0.toByte()
        java.lang.Character.TYPE -> '\u0000'
        else -> null
    }

    /** 静默通知 → 自动进收纳箱（有声音的不收） */
    private fun maybeAuto(entry: Any?) {
        if (entry == null) return
        if (!enabled()) return
        val key = keyOf(entry) ?: return
        if (keys.contains(key)) return

        // ★★★ 规则优先：用户手动记下的「这个 App 的这类通知以后自动收纳」永久生效。
        //   规则命中 → 不管 importance / 是不是静默，一律直接进收纳盒。
        if (matchesRule(entry, key)) return

        if (!runCatching { Cfg.notifBoxAuto() }.getOrDefault(false)) return
        // ★ 常驻/前台服务/不可清除的通知**不要自动收**：它们本来就"清不掉"，
        //   收进箱子等于让用户找不到（实测会把「正在通过 USB 传输文件」这种也收进去）。
        //   自己发的（模块/SystemUI 自身）也跳过，免得互相打架。
        val sbn = NotifBoxRules.sbnOf(entry)
        if (sbn != null) {
            val pkg = runCatching { Rt.callNoArg(sbn, "getPackageName") }.getOrNull() as? String
            if (pkg == "com.android.systemui" || pkg == "com.oupao.wiz") return
            val ongoing = (runCatching { Rt.callNoArg(sbn, "isOngoing") }.getOrNull() as? Boolean) ?: false
            if (ongoing) return
            val flags = (runCatching { Rt.callNoArg(sbn, "getFlags") }.getOrNull() as? Int) ?: 0
            val keep = 0x2 or 0x20 or 0x40        // FLAG_ONGOING_EVENT | FLAG_NO_CLEAR | FLAG_FOREGROUND_SERVICE
            if (flags and keep != 0) return
        }
        val imp = (runCatching { Rt.callNoArg(entry, "getImportance") }.getOrNull() as? Int) ?: 3
        val ranking = runCatching { Rt.callNoArg(entry, "getRanking") }.getOrNull()
        val ambient = (runCatching { ranking?.let { Rt.callNoArg(it, "isAmbient") } }.getOrNull() as? Boolean) ?: false
        val noisy = (runCatching { ranking?.let { Rt.callNoArg(it, "isNoisy") } }.getOrNull() as? Boolean) ?: false
        if (noisy) return                       // 会响/会振的不收
        if (imp >= 3 && !ambient) return        // 3 = IMPORTANCE_DEFAULT，以上算重要
        archive(key, entry, rebuildNow = false)
        dbg("自动收纳 key=" + shortKey(key) + " imp=" + imp + " ambient=" + ambient)
    }

    /**
     * 规则命中？命中就直接进收纳盒（永久生效，不受「自动收纳静默通知」开关影响）。
     */
    private fun matchesRule(entry: Any?, key: String): Boolean {
        val sig = NotifBoxRules.signatureOf(entry) ?: return false
        if (!NotifBoxRules.matches(sig.first, sig.second)) return false
        archive(key, entry, rebuildNow = false)
        dbg("规则收纳 key=" + shortKey(key) + " " + sig.first + "/" + sig.second)
        return true
    }

    /**
     * 在 `RankingCoordinator#attach(NotifPipeline)` 的 after 里拿到 pipeline。
     * 备份靶子：`DataStoreCoordinator` / `BubbleCoordinator`（三个签名同为 `attach(NotifPipeline)`）。
     */
    private fun hookAttach(cl: ClassLoader?): Int {
        val pipeCls = runCatching {
            Rt.cls(cl, "com.android.systemui.statusbar.notification.collection.NotifPipeline")
        }.getOrNull() ?: return 0
        var n = 0
        for (short in arrayOf("RankingCoordinator", "DataStoreCoordinator", "BubbleCoordinator")) {
            val k = runCatching { Rt.cls(cl, COORD_PKG + short) }.getOrNull() ?: continue
            val m = runCatching { k.getDeclaredMethod("attach", pipeCls) }.getOrNull() ?: continue
            runCatching { m.isAccessible = true }
            val cb = object : XMH() {
                override fun afterHookedMethod(param: XMP) {
                    onPipeline(param.args.getOrNull(0), short)
                }
            }
            if (XBridge.hookMethod(m, cb) != null) n++
        }
        if (n == 0) SLog.i("$TAG: 没找到任何 attach(NotifPipeline)")
        return n
    }

    private fun onPipeline(p: Any?, from: String) {
        if (p == null) return
        if (pipeline != null) return          // 只认第一个，别重复覆盖
        pipeline = p
        resolveRebuild(p)
        runCatching {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                runCatching {
                    ensureKeys()
                    NotifBoxRules.ensureLoaded()
                    installAutoListener(p)
                    SLog.i(
                        "$TAG: pipeline 就绪($from) 收纳=" + keys.size +
                            " 规则=" + NotifBoxRules.count()
                    )
                }
            }
        }
    }

    // ---------------- 隐藏（= 彻底静默） ----------------

    /**
     * `RankingCoordinator$4` = IsSuspendedFilter（本机 attach() 里唯一真正注册的 $4 实例），
     * `$5` = DndVisualEffects 过滤。两个都挂上，任一命中都能藏。
     * 注意：本机 `$4` 被 new 了两次（classId 0/1），hook 是按类的，两个实例都会走到。
     */
    private fun hookHideFilter(cl: ClassLoader?): Int {
        var n = 0
        for (short in arrayOf("RankingCoordinator\$4", "RankingCoordinator\$5")) {
            val k = runCatching { Rt.cls(cl, COORD_PKG + short) }.getOrNull() ?: continue
            val m = runCatching { Rt.methodByName(k, "shouldFilterOut", 2) }.getOrNull() ?: continue
            runCatching { m.isAccessible = true }
            val cb = object : XMH() {
                override fun beforeHookedMethod(param: XMP) {
                    val entry = param.args.getOrNull(0)
                    val key = keyOf(entry) ?: return
                    if (!enabled()) return
                    // ★★★ 规则命中也要隐藏（用户 15:2x 报障：规则记了但通知没被藏）。
                    //   之前只在 `onEntryAdded` 里把规则命中的通知加进 keys，
                    //   但那条路**只在"新通知到达"时走**；系统已存在的通知（常驻通知、
                    //   重新发出的通知）永远命中不了 → 规则形同虚设。
                    //   现在直接在这里判规则：只要这条通知命中「App+类型」规则，就隐藏。
                    val ruleHit = ruleMatches(entry)
                    // ★ 3356：宽松身份兜底（exact key 对不上时，按 pkg+id+user 认人）
                    val lo = looseOf(entry)
                    val looseHit = lo != null && looseIds.contains(lo)
                    val exactHit = keys.contains(key)
                    val hide = if (boxModeOn) {
                        // ── 收纳箱浏览模式：只留「收纳中的」——含规则命中的那些 ──
                        !exactHit && !looseHit && !ruleHit
                    } else {
                        // ── 默认模式：收纳掉的 + 规则命中的都不显示 ──
                        exactHit || looseHit || ruleHit
                    }
                    if (!hide) {
                        // ★ 3354：定点诊断 —— 同一个包已经有收纳 key、但这条 entry 的 key
                        //   不在集合里 → 说明**key 对不上**（外部用户「点了收纳没反应」的头号嫌疑）。
                        //   3356 起宽松身份已兜底，所以这里只在这条也没命中时才有意义。
                        if (!boxModeOn) {
                            val pk = pkgOfKey(key)
                            if (pk != null) {
                                val same = keys.filter { pkgOfKey(it) == pk }
                                if (same.isNotEmpty()) {
                                    val nowB = android.os.SystemClock.uptimeMillis()
                                    if (nowB - keyMissAt > 2000L) {
                                        keyMissAt = nowB
                                        SLog.i(
                                            "$TAG: key不符且宽松身份也没命中 收到=[" + key +
                                                "] 宽松=" + (lo ?: "null") +
                                                " 同包已收纳(" + same.size + ")=[" + same.first() + "]"
                                        )
                                    }
                                }
                            }
                        }
                        return
                    }
                    // ★ 规则命中的也**收进 keys**（这样它在收纳盒浏览模式里能翻出来，
                    //   用户能看到"我到底收了哪些"）。只在浏览模式外做，避免频繁写盘。
                    if (ruleHit && !boxModeOn && !keys.contains(key)) {
                        keys.add(key)
                        save()
                    }
                    param.setResult(true)
                    // ★ 3358：隐藏日志改成**按 key 限流**（5s/条）。以前是全局限流 1s/条，
                    //   同一时刻藏了 3 条只打得出来 1 条 —— 排查「某条被吞了」时正好缺证据。
                    val nowMs = android.os.SystemClock.uptimeMillis()
                    val lastAt = hideLogAt[key] ?: 0L
                    if (nowMs - lastAt > 5000L) {
                        if (hideLogAt.size > 256) hideLogAt.clear()
                        hideLogAt[key] = nowMs
                        SLog.i(
                            "$TAG: " + (if (boxModeOn) "浏览模式隐藏 " else "已隐藏 ") +
                                key +
                                (if (ruleHit) " [规则]" else if (!exactHit && looseHit) " [宽松收纳]" else " [收纳]")
                        )
                    }
                }
            }
            if (XBridge.hookMethod(m, cb) != null) n++
        }
        if (n == 0) SLog.i("$TAG: 没找到 shouldFilterOut（通知收纳将无效）")
        return n
    }

    /** 该 entry 是否命中「App + 类型」规则 */
    private fun ruleMatches(entry: Any?): Boolean {
        val sig = NotifBoxRules.signatureOf(entry) ?: return false
        return NotifBoxRules.matches(sig.first, sig.second)
    }

    /**
     * 通知被系统/应用真正移除时，把 key 从两个集合里清掉（避免同 key 复用时被误收纳）。
     *
     * ★★ 坑一（15:2x 报障）：`tryRemoveNotification` 不只在通知真的消失时调用 ——
     *   常驻通知（「开发者选项已开启」等）被系统重新发出时也会走这里，
     *   于是刚收纳的 key 被立刻清掉 → "收纳了但通知还在盒外，再收纳一次也没用"。
     * ★★ 坑二（3359，外部 C16 16.00.12 日志）：判断"还在不在"用的
     *   `getList()/getNotificationList()` **两个 SystemUI 版本都没有** → 永远判成"没了"，
     *   每次回调都清 key。现在改成 `getAllNotifs()`，并且**延后 300ms** 再判断
     *   （应用"先 cancel 再马上重发同 key"时，新 entry 要等当前调用栈结束才进集合）。
     */
    private fun hookRemoval(cl: ClassLoader?): Int {
        val k = runCatching {
            Rt.cls(cl, "com.android.systemui.statusbar.notification.collection.NotifCollection")
        }.getOrNull() ?: return 0
        val m = runCatching { Rt.methodByName(k, "tryRemoveNotification", 1) }.getOrNull() ?: return 0
        runCatching { m.isAccessible = true }
        val cb = object : XMH() {
            override fun afterHookedMethod(param: XMP) {
                val key = keyOf(param.args.getOrNull(0)) ?: return
                // ★ 3359+：**延后一拍**再判断。应用「先 cancel 再马上重发同 key」时，
                //   新 entry 是在当前调用栈结束之后才进 mNotificationSet 的；
                //   同步查 stillPresent 会误判成"已消失"，把刚收纳的 key 清掉 → 通知又冒出来。
                ui.postDelayed({ purgeIfGone(key) }, 300L)
            }
        }
        return if (XBridge.hookMethod(m, cb) != null) 1 else 0
    }

    /** `tryRemoveNotification` 之后延迟执行：确认通知真的没了才清 key */
    private fun purgeIfGone(key: String) {
        if (stillPresent(key)) {
            dbg("通知仍存在，不清 key=" + shortKey(key))
            return
        }
        val a = keys.remove(key)
        // 3356：宽松身份也跟着清（通知真没了）
        val lo = looseByKey.remove(key)
        if (lo != null) looseIds.remove(lo)
        if (!a) return
        save()
        dbg("原通知已消失，清 key=" + shortKey(key))
        refreshBadge()
    }

    /** 该 key 的通知此刻是否还在系统里（还在就别清集合） */
    private fun stillPresent(key: String): Boolean {
        val p = pipeline ?: return false
        // ★ 3359+：只能走 `getAllNotifs()`。
        //   老代码用的 `getList()` / `getNotificationList()` 在 C16 两个 SystemUI
        //   （16.00.12 / 16.99.12）**都不存在** → 永远返回 false，导致：
        //   ① tryRemoveNotification 一回调就把收纳 key 清掉（取消后重发同一条就"又显示"）；
        //   ② 盒子快照永远打「其中仍存在=0」。
        return runCatching {
            val list = Rt.callNoArg(p, "getAllNotifs") as? Collection<*> ?: return@runCatching false
            for (e in list) {
                if (e == null) continue
                if (keyOf(e) == key) return true
            }
            false
        }.getOrDefault(false)
    }

    // ---------------- 收纳集合 ----------------

    // ---------------- 收纳箱浏览模式 ----------------
    //
    // 用户定稿：**不另造面板**。点头部入口 = 换一套过滤规则，让通知列表里只显示「收纳中的通知」
    // （还是系统原生卡片）；再点一次回到默认（显示未收纳的、不显示收纳的）。
    // 面板收起（fraction→0）时自动退出，下次拉开还是默认视图。

    @Volatile
    private var boxModeOn = false

    /** 收起过程中退出浏览模式但**先不重建**（否则收起动画里内容会跳一下），等完全收起再重建 */
    @Volatile
    private var boxExitPending = false

    fun boxMode(): Boolean = boxModeOn

    /** @return 切换后的状态：true = 正在浏览收纳箱 */
    fun toggleBoxMode(): Boolean {
        boxModeOn = !boxModeOn
        boxExitPending = false
        rebuild("boxMode")
        refreshBadge()
        SLog.i("$TAG: 收纳箱浏览模式=" + boxModeOn + " 收纳=" + keys.size)
        return boxModeOn
    }

    /**
     * 退出浏览模式。
     * @param rebuildNow false = 只翻标志位（正在收起的动画途中），等面板完全收起再 [flushBoxExit]。
     */
    fun exitBoxMode(reason: String, rebuildNow: Boolean = true) {
        if (!boxModeOn) return
        boxModeOn = false
        if (rebuildNow) {
            rebuild("boxMode-exit")
        } else {
            boxExitPending = true
        }
        refreshBadge()
        SLog.i("$TAG: 退出收纳箱浏览模式(" + reason + ") rebuild=" + rebuildNow)
    }

    /** 面板已经完全收起（不可见）时把积压的那次重建补上 */
    fun flushBoxExit() {
        if (!boxExitPending) return
        boxExitPending = false
        rebuild("boxMode-exit")
        SLog.i("$TAG: 收起完成，补重建（回默认视图）")
    }

    fun isArchived(key: String?): Boolean = key != null && keys.contains(key)

    fun archivedCount(): Int = keys.size

    /**
     * 收纳：把 key 记下 → 立刻重建列表（该通知从所有地方消失）。
     *
     * 3356：多存一份**宽松身份**（`pkg|id|user`）。exact key 在这台 ROM / 外部用户
     * 机器上会因为 tag/summary/user 段差异对不上，宽松身份是兜底。
     */
    fun archive(key: String?, entry: Any? = null, rebuildNow: Boolean = true) {
        if (key.isNullOrEmpty()) return
        if (!enabled()) return
        // ★ 用户 04:0x 定稿：**一条一条收纳**（之前按 groupKey 整组收，但恢复是一条一条，两边不对称）。
        // ★★ key 已在集合里也**要继续走完**（重建 + 记规则）：
        //   之前这里 `if (!keys.add(key)) return` —— 用户对同一条通知再点一次「收纳」时
        //   什么都发生不了（key 早就被 tryRemoveNotification 清掉了 → add 返回 true；
        //   但如果 key 还在，add 返回 false 就直接 return → 规则没记上、列表也没重建）。
        val fresh = keys.add(key)
        val lo = looseOf(entry)
        if (lo != null) {
            looseByKey[key] = lo
            looseIds.add(lo)
        }
        save()
        // ★ 3355：这条 key 在系统通知里到底存不存在？
        //   存在=false 说明「点收纳时拿到的 key」根本不是当前这条通知的 key
        //   （菜单里插的是旧 row / 延后解析取错），后面怎么过滤都不可能命中。
        if (rebuildNow) {
            SLog.i("$TAG: 收纳时系统里有该 key=" + stillPresent(key))
            rebuild("archive")
        }
        refreshBadge()
        // ★ 3354：打全 key（截断版没法跟 shouldFilterOut 收到的 key 逐字比对）
        SLog.i("$TAG: 收纳 key=[" + key + "] 共=" + archivedCount() + " fresh=" + fresh)
    }

    /** 恢复：把 key 删掉 → 重建列表（通知重新出现在面板/状态栏/锁屏） */
    fun restore(key: String?) {
        if (key.isNullOrEmpty()) return
        val removed = keys.remove(key)
        // 3356：宽松身份一起清（否则恢复后仍会被宽松规则挡住）
        val lo = looseByKey.remove(key)
        if (lo != null) looseIds.remove(lo)
        save()
        rebuild("restore")
        refreshBadge()
        if (removed) SLog.i("$TAG: 恢复 key=" + shortKey(key) + " 剩=" + archivedCount())
    }

    /** 全部恢复 */
    fun restoreAll() {
        val n = keys.size
        if (n == 0) return
        keys.clear()
        looseByKey.clear()
        looseIds.clear()
        save()
        rebuild("restoreAll")
        refreshBadge()
        SLog.i("$TAG: 全部恢复 n=$n")
    }

    /**
     * 重建通知列表。
     *
     * ★ 3357（外部用户日志实锤）：收纳是在长按菜单 `access$onClick` 的**回调栈里**进来的，
     *   那一刻菜单正在收起、ShadeListBuilder 往往正在 build —— 同步调重建会被直接丢掉。
     *   日志证据：
     *
     *   ```
     *   20.635  ShadeListBuilder.rebuildList(archive) -> null
     *   21.236  重建后检查(archive) 展示列表=1 其中已收纳/删除=1   ← 第一次重建没生效
     *   21.546  补重建已发出
     *   22.009  已隐藏 0|com.tencent.mobileqq|518|null|10402 [规则]  ← 补的那次才生效
     *   ```
     *
     *   所以：① 一律丢到主线程队列里执行，不在回调栈里直接调；
     *         ② 发完之后**校验实际展示列表**，没滤掉就错峰再补，最多 [REBUILD_MAX] 次；
     *         ③ 具体往哪打由 [resolveRebuild] 按 SystemUI 版本决定。
     */
    private fun rebuild(why: String) {
        val p = pipeline ?: run {
            // ★ 3354：以前这里静默 return —— 外部用户排查时**完全看不出来**
            //   「根本没拿到 pipeline」和「拿到了但过滤没重跑」的区别。
            SLog.i("$TAG: 重建跳过(" + why + ") —— pipeline=null（attach 钩子没拿到）")
            return
        }
        ui.post { doRebuild(p, why, 0) }
    }

    /**
     * 解析一次「往哪打才能让通知列表重建」。
     *
     * 顺序：
     *  1. `ShadeListBuilder.rebuildListIfBefore(int)` —— **两个 C16 版本都有**，且本机
     *     `NotifPipeline.requestListRebuild` 反编译出来就是这一句（`rebuildListIfBefore(0)`），
     *     所以优先用它：外部 16.00.12 也能走同一条路，行为可在我这台机上实测。
     *  2. `NotifPipeline.requestListRebuild(String)` —— 新版 16.99.12 有，作为兼容兜底；
     *  3. `NotifCollection.dispatchEventsAndRebuildList(String)` —— 完整派发路径，最后兜底。
     *
     * 三条都找不到才算死路（会在日志里明确报出来，不再假装「已发出」）。
     */
    private fun resolveRebuild(p: Any) {
        if (rebuildResolved) return
        rebuildResolved = true
        val lb = Rt.get(p, "mShadeListBuilder")
        Rt.methodByName(lb?.javaClass, "rebuildListIfBefore", 1)?.let {
            rebuildMethod = it
            rebuildTarget = lb
            rebuildKind = 2
            SLog.i("$TAG: 重建通道=rebuildListIfBefore(0)")
            return
        }
        Rt.methodByName(p.javaClass, "requestListRebuild", 1)?.let {
            rebuildMethod = it
            rebuildTarget = p
            rebuildKind = 1
            SLog.i("$TAG: 重建通道=requestListRebuild")
            return
        }
        val nc = Rt.get(p, "mNotifCollection")
        Rt.methodByName(nc?.javaClass, "dispatchEventsAndRebuildList", 1)?.let {
            rebuildMethod = it
            rebuildTarget = nc
            rebuildKind = 3
            SLog.i("$TAG: 重建通道=dispatchEventsAndRebuildList")
            return
        }
        SLog.e("$TAG: 找不到任何重建入口（收纳后列表不会刷新）", null)
    }

    private fun doRebuild(p: Any, why: String, attempt: Int) {
        resolveRebuild(p)
        val reason = "oupao-nb-" + why + "#" + attempt
        val ok = runCatching {
            val m = rebuildMethod ?: return@runCatching false
            val target = rebuildTarget ?: return@runCatching false
            m.isAccessible = true
            // rebuildListIfBefore 只要 int（0 = 不管当前处于哪一步都排一次重建）
            if (rebuildKind == 2) m.invoke(target, Integer.valueOf(0))
            else m.invoke(target, reason)
            true
        }.getOrElse {
            SLog.e("$TAG: 重建(" + why + ") 调用失败", it)
            false
        }
        if (attempt == 0) SLog.i("$TAG: 重建(" + why + ") 已发出 ok=" + ok)
        // 浏览模式：展示列表本来就只装「收纳中的」，校验方向相反 → 只打一份现场快照
        if (boxModeOn) {
            ui.postDelayed({ dumpBox(p) }, 400L)
            return
        }
        ui.postDelayed({ verifyBuilt(p, why, attempt) }, if (attempt == 0) 450L else 350L)
    }

    /**
     * 3358：收纳箱浏览模式的现场快照。
     * 排查「被吞了但收纳盒里没有」时要看的三件事：盒子实际展示了几条、收纳集合里有几个 key、
     * 其中多少条此刻还真的存在（`stillPresent`）。盒子里空 + 收纳集合非空 = 那些通知已经不在了
     * （或者 key 对不上），不是被藏了。
     */
    private fun dumpBox(p: Any) {
        runCatching {
            val lb = Rt.get(p, "mShadeListBuilder")
            val built = (Rt.get(lb, "mNotifList") ?: Rt.get(lb, "mNotifListContainer"))
                as? Collection<*> ?: return@runCatching
            var inBox = 0
            for (e in built) {
                val k = keyOf(e)
                if (k != null && keys.contains(k)) inBox++
            }
            var alive = 0
            for (k in keys) if (stillPresent(k)) alive++
            SLog.i(
                "$TAG: 盒子快照 展示=" + built.size + " 其中收纳=" + inBox +
                    " 收纳集合=" + keys.size + " 其中仍存在=" + alive
            )
        }
    }

    /** 重建后校验：`mShadeListBuilder.mNotifList` = 过滤后真正要展示的列表 */
    private fun verifyBuilt(p: Any, why: String, attempt: Int) {
        runCatching {
            val lb = Rt.get(p, "mShadeListBuilder")
            val built = (Rt.get(lb, "mNotifList") ?: Rt.get(lb, "mNotifListContainer"))
                as? Collection<*> ?: return@runCatching
            var hit = 0
            for (e in built) {
                val k = keyOf(e)
                if (k != null && keys.contains(k)) hit++
            }
            if (hit == 0) {
                if (attempt > 0) {
                    SLog.i("$TAG: 重建(" + why + ") 第" + (attempt + 1) + "次生效 展示列表=" + built.size)
                }
                return@runCatching
            }
            if (attempt >= REBUILD_MAX) {
                SLog.i(
                    "$TAG: 重建(" + why + ") 补" + attempt + "次仍未滤掉 展示列表=" +
                        built.size + " 命中=" + hit
                )
                return@runCatching
            }
            SLog.i(
                "$TAG: 重建(" + why + ") 第" + (attempt + 1) + "次没滤掉(展示=" +
                    built.size + " 命中=" + hit + ") → 再补一次"
            )
            doRebuild(p, why, attempt + 1)
        }
    }

    // ---------------- 持久化 ----------------
    //
    // ★ 实测（3005 真机）：SystemUI 进程**写不进** `Settings.Global`（属主校验直接拒，
    //   `Cfg.gsetC` 里 runCatching 吞掉了，表现为收纳生效但重启后全丢）。
    //   改成写 **SystemUI 自己的 data 目录**（`context.filesDir`），本进程 uid 一定可写。

    private fun ctx(): Context? =
        runCatching { Ctx.sysUi() }.getOrNull() ?: runCatching { Rx.ctxB() }.getOrNull()

    private fun storeFile(c: Context, name: String): java.io.File =
        java.io.File(c.filesDir ?: c.dataDir, name)

    private fun ensureKeys() {
        if (keysLoaded) return
        keysLoaded = true
        runCatching {
            val c = ctx() ?: return
            readSet(c, G_LIST, keys)
            readLoose(c)
            // ★ 3358：清掉历史遗留的「删除集」（功能已删除，留着只会继续吞通知）
            val stale = runCatching {
                val f = storeFile(c, G_DROP_LEGACY + ".txt")
                val n = if (f.exists()) f.readLines().count { it.isNotEmpty() } else 0
                if (f.exists()) f.delete()
                n
            }.getOrDefault(0)
            SLog.i(
                "$TAG: 读回收纳 收纳=" + keys.size +
                    " 宽松=" + looseByKey.size + " 清掉删除集=" + stale + " dir=" + c.filesDir
            )
        }.onFailure { SLog.e("$TAG: 读回收纳失败", it) }
    }

    /** 3356：`key<TAB>pkg|id|user` 一行一条 */
    private fun readLoose(c: Context) {
        runCatching {
            val f = storeFile(c, G_LOOSE + ".txt")
            if (!f.exists()) return
            looseByKey.clear()
            looseIds.clear()
            f.readLines().forEach { line ->
                val i = line.indexOf('\t')
                if (i > 0 && i < line.length - 1) {
                    val k = line.substring(0, i)
                    val v = line.substring(i + 1)
                    looseByKey[k] = v
                    looseIds.add(v)
                }
            }
        }
    }

    private fun readSet(c: Context, name: String, into: MutableSet<String>) {
        runCatching {
            val f = storeFile(c, name + ".txt")
            if (!f.exists()) return
            into.clear()
            f.readLines().forEach { if (it.isNotEmpty()) into.add(it) }
        }
    }

    private fun save() {
        runCatching {
            ensureKeys()
            val c = ctx() ?: return
            writeSet(c, G_LIST, keys)
            runCatching {
                storeFile(c, G_LOOSE + ".txt")
                    .writeText(looseByKey.entries.joinToString("\n") { it.key + "\t" + it.value })
            }
        }.onFailure { SLog.e("$TAG: 保存失败", it) }
    }

    private fun writeSet(c: Context, name: String, from: MutableSet<String>) {
        runCatching {
            storeFile(c, name + ".txt").writeText(from.joinToString("\n"))
        }.onFailure { SLog.e("$TAG: 保存 $name 失败", it) }
    }


    // ---------------- 取数 ----------------
    //
    // 03:2x 用户定稿：不另造面板 —— 收纳箱直接复用系统原生通知卡片（靠「浏览模式」换过滤规则），
    // 所以这里原来那套「自己取 sbn + 拼列表」的代码已经不需要了（getAllNotifs/Item 已删除）。

    // ---------------- 小工具 ----------------

    /** 日志配额：只在排查时打前几十条，别把 logd 刷爆 */
    @Volatile
    private var dbgLeft = 60

    /** 3336：隐藏动作的独立限流（不受 dbgLeft 配额影响）。3358 起按 key 各记一份 */
    private val hideLogAt = java.util.concurrent.ConcurrentHashMap<String, Long>()

    /** 3354：key 不匹配诊断的独立限流 */
    @Volatile
    private var keyMissAt = 0L

    /** 3357：重建最多补几次（首发 + 3 次 = 共 4 次，最坏 ~1.5s 内落地） */
    private val REBUILD_MAX = 3

    /** 主线程 Handler（重建后自检 / 重试用） */
    private val ui = android.os.Handler(android.os.Looper.getMainLooper())

    private fun dbg(msg: String) {
        if (dbgLeft <= 0) return
        dbgLeft--
        SLog.i("$TAG: " + msg)
    }

    private fun shortKey(k: String): String = if (k.length <= 42) k else k.substring(0, 42) + "…"

    /**
     * 3356：entry 的**宽松身份** `包名|通知id|用户id`。
     * 直接从 SBN 取，不依赖 key 的字符串格式 —— 这就是它比 exact key 稳的原因。
     */
    private fun looseOf(entry: Any?): String? {
        if (entry == null) return null
        val sbn = runCatching { NotifBoxRules.sbnOf(entry) }.getOrNull() ?: return null
        val pkg = runCatching { sbn.javaClass.getMethod("getPackageName").invoke(sbn) as? String }
            .getOrNull() ?: return null
        val id = runCatching { sbn.javaClass.getMethod("getId").invoke(sbn) as? Int }.getOrNull()
            ?: return null
        val u = runCatching {
            val uh = sbn.javaClass.getMethod("getUser").invoke(sbn)
            uh.javaClass.getMethod("getIdentifier").invoke(uh) as? Int
        }.getOrNull() ?: 0
        return "$pkg|$id|$u"
    }

    /**
     * 3354：从通知 key 里取「包名」段。
     * 系统 key 形如 `0|com.foo|10086|tag|id|uid|...`，第二段是包名；
     * 拿不到（非该格式）就返回 null，诊断直接跳过。
     */
    private fun pkgOfKey(k: String): String? {
        val i = k.indexOf('|')
        if (i <= 0) return null
        val j = k.indexOf('|', i + 1)
        if (j <= i + 1) return null
        val pk = k.substring(i + 1, j)
        return pk.takeIf { it.contains('.') && !it.contains('|') }
    }

    /** `NotificationEntry#getKey()`（本机未混淆）；方法按类缓存，热路径每次只做一次 invoke */
    fun keyOf(entry: Any?): String? {
        if (entry == null) return null
        val c = entry.javaClass
        if (noKey.contains(c)) return null
        var m = keyMethod[c]
        if (m == null) {
            m = runCatching { Rt.methodByName(c, "getKey", 0) }.getOrNull()
            if (m == null) {
                noKey.add(c)
                return null
            }
            keyMethod[c] = m
        }
        return runCatching { m.invoke(entry) as? String }.getOrNull()
    }

    /** 找不到 getKey 的类，记下来别反复查 */
    private val noKey: MutableSet<Class<*>> = ConcurrentHashMap.newKeySet()

    /** 头部入口徽标 */
    @Volatile
    private var badge: Any? = null

    fun bindBadge(v: View?) {
        badge = v
        refreshBadge()
    }

    private fun refreshBadge() {
        val v = badge as? View ?: return
        runCatching {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                runCatching { NotifBoxUi.update(v, archivedCount()) }
            }
        }
    }
}
