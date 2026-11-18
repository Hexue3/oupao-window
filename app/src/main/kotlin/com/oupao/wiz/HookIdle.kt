package com.oupao.wiz

/**
 * 闲鱼链接定向 + 应用间跳转小窗 的 startActivity 回调。
 *
 * 三件事按顺序做：
 * 1. `tb.cn` 短链首次见 → 取消原跳转，由 [Idle] 后台解析真实地址后自己发起；
 * 2. 闲鱼域名 → 改包名（[Idle.retarget]），即时生效；
 * 3. 应用间跳转 → 调 `startZoomWindow` 让小窗启动（[CrossZoom.fire]），
 *    成功后**取消原跳转**，避免同一个 Intent 开两次。
 *
 * 全程包在 try 里：这里挂在 system_server 的 startActivity 上，
 * 绝不能因为自己让任何 App 打不开页面。
 */
class HookIdle(
    /** Intent 在形参里的下标；<0 表示运行时再扫实参 */
    private val intentIdx: Int = -1,
    /** bOptions 在形参里的下标；仅用于诊断/回退方案 */
    private val optsIdx: Int = -1,
    /** callingPackage 在形参里的下标；<0 表示运行时退化 */
    private val callerIdx: Int = -1,
    /** userId 在形参里的下标（分身要用对用户） */
    private val userIdx: Int = -1,
    /** resultTo(IBinder) 在形参里的下标；非空表示 startActivityForResult */
    private val resultIdx: Int = -1
) : XMH() {

    override fun beforeHookedMethod(param: XMP) {
        runCatching {
            val it = pick(param) ?: return@runCatching
            // ── 移除应用启动遮罩（FlymeUI 页的开关）──
            // 只改 ActivityOptions 里的 splashScreenStyle，不动启动本身。
            if (Cfg.noSplash()) noSplash(param)
            if (Idle.takeOverShortClick(it, param.args)) {
                param.result = 0
                param.returnEarly = true
                return@runCatching
            }
            Idle.retarget(it)

            // ── 应用间跳转 → 小窗 ──
            val caller = callerOf(param)
            // 目标：显式 component/package 直接取；隐式 Intent（点链接的常见形态）
            // 需要让 PackageManager 解析出真正会落到的包，否则判据一律放弃。
            val target = CrossZoom.targetPkg(it) ?: CrossZoom.resolveTarget(ctxOf(param), it)
            // ★ 2978：这一层拿得到真实 caller。
            //   「选择器发起的启动」= 用户刚选完分身/应用 → 给注入层一个一次性信号，
            //   让它对这一跳做小窗注入（选择器本身已经弹完了）。
            //   用户**取消**选择器时根本不会有这一跳 → 不会误置位。
            if (CrossZoom.isChooserCaller(caller) && !target.isNullOrEmpty()) {
                CrossZoom.noteChooserPick(target)
                // 2980：选完了 → 把「刚弹过选择器」标记清掉，让这一跳能被注入
                CrossZoom.clearChooserLaunch()
            }
            // 2979：这一跳就是选择器本身 → 全局置位，后面几层一律不接管
            // 2983：SystemUI（NotifFly）注入的小窗不经过我们的 fire，
            //   这里看到「已带小窗参数」就补一次 arm，否则 FlexFix 会把它挡成全屏。
            if (runCatching { it.getIntExtra("extra_window_mode", 0) == 100 }.getOrDefault(false)) {
                CrossZoom.armZoomSupport()
            }
            if (CrossZoom.isChooserHop(it, target)) {
                CrossZoom.noteChooserLaunch()
                // ★ 2981：彩蛋在这里 —— ColorOS 会给分享选择器带上
                //   `oplus.intent.extra.allow_flexible_share=true`，系统自己就把选择器开成了小窗（
                //   不是我们注入的！所以光挡注入没用）。把它摘掉即可。
                val hadFlex = runCatching { it.hasExtra("oplus.intent.extra.allow_flexible_share") }
                    .getOrDefault(false)
                if (hadFlex) runCatching { it.removeExtra("oplus.intent.extra.allow_flexible_share") }
                val flexibleShare = hadFlex
                SLog.r(
                    "XZoom",
                    "XZoom: 选择器一律全屏(已摘 allow_flexible_share) stripped=" + flexibleShare +
                        " caller=" + caller + " -> " + target,
                    600L
                )
            }
            val take = CrossZoom.shouldTake(it, caller, target, hasResult(param))
            CrossZoom.diagRes("Idle", caller, target, hasResult(param), param.args, resultIdx)
            if (CrossZoom.diagOn()) {
                if (target != null) {
                    SLog.r(
                        "XZdiag",
                        "XZdiag: $caller -> $target take=$take res=${hasResult(param)}" +
                            " u=${uidOf(param)} comp=${it.component} optsIdx=$optsIdx" +
                            " act=${it.action} switch=${Cfg.crossZoom()}",
                        800L
                    )
                } else if (!caller.isNullOrEmpty()) {
                    SLog.r(
                        "XZmiss",
                        "XZmiss: $caller -> ? act=${it.action} comp=${it.component}" +
                            " dat=${it.dataString} flags=${Integer.toHexString(it.flags)}",
                        2000L
                    )
                }
            }
            // QQ 分享：注入对 JumpActivity 不生效（QQ 会再起全屏 task），直接走 startZoomWindow。
            // 有分身时第一跳先让选择器弹（take=false/clone），选完后 caller=multiapp 再走这里。
            val qqShare = CrossZoom.isQqShare(it, target)
            val fromMulti = caller == "com.oplus.multiapp" || caller == "com.coloros.multiapp"
            if (qqShare && (fromMulti || !CrossZoom.hasClone(target))) {
                val ok = CrossZoom.fire(it, uidOf(param), param.thisObject, "QqShare")
                SLog.r(
                    "XZoom",
                    "XZoom: QQ分享 → 取消原启动走小窗 ok=" + ok + " " + caller + " -> " + target,
                    800L,
                )
                if (ok) {
                    param.result = 0
                    param.returnEarly = true
                    return@runCatching
                }
            }
            if (!take) return@runCatching

            // SDK 分享（QQ/微信）：带 resultTo，不能取消；
            // 交给 ActivityStarter 层的 HookStarter 深注入（保留结果，比这里补 Bundle 可靠）。
            if (CrossZoom.isSdkShare(it, target)) {
                SLog.r(
                    "XZinj",
                    "XZinj: SDK 分享 + resultTo → 放行交给 inject " + caller + " -> " + target,
                    800L,
                )
                return@runCatching
            }

            // 2906：**带 resultTo（startActivityForResult）的启动不能取消**。
            //   实测：QQ 下载 apk →「其他应用打开」→ MT管理器 →「定位所在位置」
            //   这一下是 `com.tencent.mobileqq -> bin.mt.plus act=VIEW res=true`，
            //   我们原来在这里 `CrossZoom.fire()` 成功 → 取消原启动 → 结果回调没人收 →
            //   MT管理器永远等不到结果 → 点了没反应（既不小窗也不全屏）。
            //   → 改成「补 zoom 参数 + 放行」：回调还在，窗口仍按小窗起。
            if (hasResult(param) && CrossZoom.isWebLaunch(it)) {
                // 2946：网页/App-Link 式的 for-result 启动（如 Gemini 入口）不小窗
                SLog.r("XZinj", "XZinj: resultTo + 网页 → 直接放行 " + caller + " -> " + target, 800L)
                return@runCatching
            }
            val textSearch = CrossZoom.isTextSearchTarget(it, target)
            if (textSearch) {
                SLog.r(
                    "XZoom",
                    "XZoom: 文本搜索 + resultTo → 取消原启动走小窗 " + caller + " -> " + target,
                    800L,
                )
            }
            if (hasResult(param) && !textSearch) {
                // 2937（用户：微信→消息通知小窗，但要按两次返回）：
                //   目标是**系统设置页**时，这个 resultTo 根本没人用，
                //   但「补参数放行」会让原启动照常发生（全屏设置页），
                //   同时后面 HookXzPath/HookStarter 又 fire 了一个小窗 → **两次启动**，
                //   所以要先关小窗、再关那个全屏 Activity，表现就是「要按两次返回」。
                //   → 设置类目标：直接 fire（取消原启动），只开一个窗口。
                if (CrossZoom.isSettingsTarget(target)) {
                    // 2938（用户：还是要返回两次）：
                    //   不能在这里 fire + 取消 —— 取消一个 startActivityForResult 会让微信自己那个
                    //   等结果的 PermissionActivity 一直挂着，关掉小窗后它顶在上面 → 点不了微信。
                    //   改成：放行原启动（不取消，结果照常回去），
                    //   zoom 参数交给 ActivityStarter 层的 HookStarter.inject 去写（和磁贴同一条路）。
                    SLog.r(
                        "XZoom",
                        "XZoom: 设置页 + resultTo → 放行交给 inject " + caller + " -> " + target,
                        800L,
                    )
                    return@runCatching
                }
                val wrote = CrossZoom.bundleZoom(param.args, optsIdx, it)
                SLog.r(
                    "XZoom",
                    "XZoom: \u5e26 resultTo \u2192 \u8865\u53c2\u6570\u653e\u884c wrote=" + wrote +
                        " " + caller + " -> " + target + " act=" + it.action,
                    800L,
                )
                return@runCatching
            }

            // ★ 控制中心磁贴 / 通知设置按钮（SystemUI 发起）：直接给这次启动的 ActivityOptions
            //   写 zoom 键，让系统自己按小窗启动 —— 与 NotifFly 在 SystemUI 里的做法一致，
            //   不取消原启动、不重建 task。自测通道（shell）允许时也走这条，方便 adb 验收。
            if ("com.android.systemui" == caller || "com.android.shell" == caller) {
                // 2969：网页手接（VIEW http/https）**绝不**走「取消原启动 + 延时重发」
                //   —— 重发的是 Intent 拷贝，resultTo 会丢，App-Link 入口变成小窗。
                if (CrossZoom.isWebLaunch(it)) {
                    SLog.i("XZoom: 磁贴通道跳过网页手接 " + it.dataString)
                    return@runCatching
                }
                if (CrossZoom.bundleZoom(param.args, optsIdx, it)) {
                    if ("com.android.systemui" == caller) {
                        // 用户要求
                        // 可关掉「先收面板」这套拦截，回到原始行为（bundleZoom 直接放行、磁贴锚点过渡动画）
                        if (Cfg.getProp("persist.oupao.zoom_intercept", "1") == "0") return@runCatching
                        // 做法：先收面板，拦住这次立即启动，延时 ~280ms（面板收起动画时长）后再自己启动，
                        // 这样就不会出现「小窗先出来、面板后收」。
                        // 2954：收面板改走**带动画**那条（system_server 的 IStatusBarService 只有无动画的
                        //   collapsePanels，带画的是 SystemUI 的 OplusSeparateQSPluginImpl.collapseQSPanel →
                        //   广播 QS_COLLAPSE 让它去调）。
                        CrossZoom.collapsePanelsAnimated()
                        val intentCopy = android.content.Intent(it)
                        val bIdx = runCatching {
                            var k = optsIdx
                            if (k < 0 || k >= param.args.size || param.args[k] !is android.os.Bundle)
                                k = param.args.indexOfFirst { a -> a is android.os.Bundle }
                            k
                        }.getOrDefault(-1)
                        val opts = if (bIdx >= 0) (param.args[bIdx] as? android.os.Bundle) else null
                        val uid2 = uidOf(param)
                        val atms2 = param.thisObject
                        param.result = 0
                        param.returnEarly = true
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            runCatching {
                                val ok = CrossZoom.fire(intentCopy, uid2, atms2, "IdleTile")
                                SLog.i("XZoom: 面板收起后启动 zoom ok=" + ok)
                                if (!ok && opts != null) {
                                    // 兜底：用同一份 zoom 参数走普通 startActivity，别把这次点击吞掉
                                    val ctx = runCatching { CrossZoom.sysCtxOf(atms2) }.getOrNull()
                                    ctx?.startActivity(intentCopy, opts)
                                    SLog.i("XZoom: 兜底 startActivity 已调用")
                                }
                            }
                        }, runCatching {
                            // 可调延时：面板收起动画播完再出小窗。
                            // 3331：合并模式（caller=systemui）改用 `zoom_delay_sysui`，默认 **320ms**。
                            //   原来 160/180ms 时，ROM 的收起动画才播到一半，小窗就压上来了，
                            //   用户体感就是「长按磁贴没有动画」（同一台机器上滑/返回关闭是有动画的）。
                            //   分离模式不走这里（它的收起由 smartsidebar 自己完成），不受影响。
                            (Cfg.getProp("persist.oupao.zoom_delay_sysui", "320").toLongOrNull() ?: 320L)
                                .coerceIn(0L, 800L)
                        }.getOrDefault(320L))
                        return@runCatching
                    }
                    return@runCatching
                }
            }

            // 控制中心/通知面板发起的：先把面板收起来，否则会「小窗先出现、面板后收」
            //   2954：同样走带动画那条
            if ("com.android.systemui" == caller) CrossZoom.collapsePanelsAnimated()
            if (CrossZoom.fire(it, uidOf(param), param.thisObject, "Idle")) {
                // 已交给小窗：取消原调用并返回 START_SUCCESS，避免开两次
                param.result = 0
                param.returnEarly = true
            }
            // 失败 → 什么都不做，原方法照常执行（全屏打开），绝不丢请求
        }
    }

    /**
     * 移除应用启动遮罩：把这次启动的 ActivityOptions Bundle 里
     * `android.activity.splashScreenStyle` 写成 0（SPLASH_SCREEN_STYLE_EMPTY）。
     *
     * 只动这一个键，不动启动参数/Intent/task 模式，失败也不影响启动本身。
     * bOptions 参数为 null 时补一个空 Bundle 再写（等价于 ActivityOptions.makeBasic()）。
     */
    private fun noSplash(param: XMP) {
        runCatching {
            var idx = optsIdx
            if (idx < 0 || idx >= param.args.size) {
                idx = param.args.indexOfFirst { it is android.os.Bundle }
            }
            if (idx < 0) {
                // 部分重载（如 startActivityFromRecents）没有 bOptions 形参，跳过
                return@runCatching
            }
            val b = (param.args.getOrNull(idx) as? android.os.Bundle)
                ?: android.os.Bundle().also { param.args[idx] = it }
            if (!b.containsKey(K_SPLASH_STYLE)) b.putInt(K_SPLASH_STYLE, 0)
            // 限流日志：开关开着时每 2s 最多一条，方便确认确实生效
            SLog.r("NSplash", "NSplash: 已置 splashScreenStyle=0 idx=$idx", 2000L)
        }
    }

    /** callingPackage：优先用 install 时算好的下标，退化时扫实参里第一个「像包名」的 String */
    private fun callerOf(param: XMP): String? {
        if (callerIdx >= 0) {
            (param.args.getOrNull(callerIdx) as? String)?.let { return it }
        }
        for (a in param.args) {
            if (a is String && a.contains('.') && !a.startsWith(".")) return a
        }
        return null
    }

    /** userId：分身应用必须用对用户，否则 startZoomWindow 找不到应用 */
    private fun uidOf(param: XMP): Int {
        if (userIdx < 0) return 0
        return (param.args.getOrNull(userIdx) as? Int) ?: 0
    }

    /** 是否带返回结果（resultTo 非空 = startActivityForResult） */
    private fun hasResult(param: XMP): Boolean {
        if (resultIdx < 0) return false
        return param.args.getOrNull(resultIdx) != null
    }

    /**
     * 取 Context 用于解析隐式 Intent。
     * 优先 ATMS 自己的 `mContext`（同在 system_server，零跨进程），退 [Cfg.sysContext]。
     */
    private fun ctxOf(param: XMP): android.content.Context? {
        (Rt.get(param.thisObject, "mContext") as? android.content.Context)?.let { return it }
        return runCatching { Cfg.sysContext() }.getOrNull()
    }

    /** 取 Intent：优先用已知下标，退化时再扫实参 */
    private fun pick(param: XMP): android.content.Intent? {
        if (intentIdx >= 0) {
            (param.args.getOrNull(intentIdx) as? android.content.Intent)?.let { return it }
        }
        for (a in param.args) if (a is android.content.Intent) return a
        val self = param.thisObject ?: return null
        (Rt.get(self, "mIntent") as? android.content.Intent)?.let { return it }
        Rt.get(self, "mRequest")?.let {
            (Rt.get(it, "intent") as? android.content.Intent)?.let { r -> return r }
        }
        Rt.get(self, "mStartActivity")?.let {
            (Rt.get(it, "intent") as? android.content.Intent)?.let { r -> return r }
        }
        return null
    }

    private companion object {
        /** framework 的 @hide 键名：ActivityOptions.setSplashScreenStyle 写的就是它 */
        const val K_SPLASH_STYLE = "android.activity.splashScreenStyle"
    }}
