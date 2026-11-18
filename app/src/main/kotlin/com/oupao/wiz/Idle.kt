package com.oupao.wiz

import android.content.Intent
import android.os.Bundle
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 闲鱼链接定向。
 *
 * ## 要解决的问题
 *
 * 点开 / 复制闲鱼分享的链接时，会落到淘宝而不是闲鱼。
 *
 * ## 根因（都是 `cmd package query-activities` 实测出来的，不是猜的）
 *
 * 1. **淘宝声明了一个匹配任意 https 的兜底过滤器**（`com.taobao.browser.BrowserActivity`）。
 *    所以任何 https 链接淘宝都「认」——包括 `example.com`。闲鱼只对自己那几个域名精确声明。
 * 2. **闲鱼分享出来的链接是淘宝通用短链 `m.tb.cn/h.xxx`**。这个域名：
 *    - 闲鱼 App **完全不认**（VIEW 查不到，`android.intent.action.idlefish` 也查不到）；
 *    - 全系统只有**浏览器**认它。
 * 3. `m.tb.cn` 其实是**阿里自己的跳转页**：页面 JS 里写着
 *    `var url = 'https://h5.m.goofish.com/item?...'`，然后 `window.location.replace(url)`。
 *    跳到 `h5.m.goofish.com` 之后，系统才按这个域名分派给闲鱼 `InitActivity`。
 *
 * 所以「闲鱼链接」= 一个 http 跳转页 + 落到闲鱼自己的域名。
 * **拿 `m.tb.cn` 直接判成闲鱼是假阳性** —— 气泡点了会打不开（闲鱼没这个过滤器），
 * 然后兜底逻辑再弹一次淘宝、淘宝跳转后又弹一次闲鱼。
 *
 * ## 做法（沿用系统原生）
 *
 * - 闲鱼域名 → 直接定向到闲鱼（改 `startActivity` 的 Intent 包名）。
 * - `tb.cn` 短链 → **先异步解析出真实地址**，再按真实域名判归属。
 * - 短链解析失败 → 交给淘宝（它就是原行为，也不会误伤淘宝商品）。
 *
 * **没有开关**：只在「闲鱼自己的域名」和「淘宝短链解析结果落在闲鱼域名」时动手，
 * 其余链接一律不碰，恒开不会误伤。
 *
 * ## 为什么短链要「拦下来再自己开」
 *
 * 闲鱼域名好办：直接改 `startActivity` 的 Intent 包名即可，点击即时生效。
 *
 * 但 `m.tb.cn` 短链**改包名是没用的**（闲鱼没这个过滤器，改了等于打不开）。
 * 而浏览器的做法是：拿到短链 → 加载页面 → **页面自己 `location.replace()` 跳转**，
 * 全程在浏览器内部，**不会再发一次 Intent**，所以钩子根本看不到真实地址。
 *
 * 所以要做「三次点击里的第一次」：在 `startActivity` 阶段把它**拦下来（取消原跳转）**，
 * 后台解析出真实地址，再由我们自己发起。代价是首次点击会多约 1 秒。
 *
 * ## 集成方式：只占一个点
 *
 * 剪贴板 `HookL` 调 [onClip]，返回 true 表示「这条我接管了」，就不再走 `Sug.onClip`。
 * **不改 `Sug` 里的任何逻辑** —— 避免和它自己的 `mapPkg` / 气泡 / 兜底互相打架。
 */
object Idle {

    private const val PKG_IDLE = "com.taobao.idlefish"
    private const val PKG_TAOBAO = "com.taobao.taobao"
    /** 闲鱼给 `2.taobao.com/item.htm` 这类链接用的自定义 action */
    private const val ACTION_IDLE = "android.intent.action.idlefish"
    /** 自己重放时打的标记，防止把自己发起的 Intent 再拦一次 */
    private const val EXTRA_DONE = "oupao_idle_done"

    /** 跳转页里目标地址的正则：`var url = '...'` */
    private val SHORT_RE = Regex("""var\s+url\s*=\s*'([^']+)'""")

    // ================= 域名判定 =================

    /**
     * 闲鱼自己的域名。
     *
     * 不含 `market.m.taobao.com`：闲鱼虽然也声明了，但这个名字太通用，接管会误伤淘宝营销页。
     */
    @JvmStatic
    fun isIdleHost(host: String?): Boolean {
        if (host == null) return false
        val h = host.lowercase()
        if (h.isEmpty()) return false
        if (h == "goofish.com" || h.endsWith(".goofish.com")) return true
        if (h == "fleamarket.taobao.com" || h.endsWith(".fleamarket.taobao.com")) return true
        return h == "2.taobao.com"
    }

    /** 淘宝通用短链（阿里的跳转页，闲鱼不认） */
    @JvmStatic
    fun isTbShortHost(host: String?): Boolean {
        if (host == null) return false
        val h = host.lowercase()
        return h == "tb.cn" || h.endsWith(".tb.cn")
    }

    // ================= 剪贴板入口 =================

    /**
     * 剪贴板里这段文字要不要由我接管。
     *
     * @param src 复制来源包名（`ClipboardService.checkAndSetPrimaryClip` 的 callingPackage）。
     *            目前只用于日志，留着方便排查。
     * @return true = 已接管（气泡由我弹），false = 交回 `Sug.onClip` 原有逻辑
     */
    @JvmStatic
    fun onClip(text: String?, src: String?): Boolean {
        val url = urlOf(text) ?: return false
        val host = hostOf(url) ?: return false

        if (isIdleHost(host)) {
            // 必须闲鱼自己能接住才弹它的气泡；接不住就交回原逻辑（否则点了打不开）
            if (probe(host, url) != Intent.ACTION_VIEW) {
                log("Idle: 闲鱼接不住 $host，交回原逻辑")
                return false
            }
            log("Idle: 闲鱼域名 $host src=$src")
            bubble(PKG_IDLE, url)
            return true
        }

        if (isTbShortHost(host)) {
            log("Idle: 短链 $host 解析中 src=$src")
            bubbleAfterResolve(url)
            return true
        }

        return false
    }

    // ================= 短链解析 =================

    /** 短链 → 真实地址（"" = 解析失败，缓存避免重复联网） */
    private val cache = ConcurrentHashMap<String, String>()

    @Volatile
    private var pool: ExecutorService? = null

    private fun executor(): ExecutorService {
        pool?.let { return it }
        synchronized(this) {
            pool?.let { return it }
            // 用**多线程**池：短链解析是阻塞网络请求，单线程会被一个卡住的 URL 堵死，
            // 后面的解析全部排队 → 表现为「复制了但气泡一直不出来」。
            val p = Executors.newCachedThreadPool { r ->
                Thread(r, "oupao-idle-resolve").apply { isDaemon = true }
            }
            pool = p
            return p
        }
    }

    /**
     * 解析短链再弹气泡。
     *
     * **必须异步**：剪贴板钩子跑在 `system_server` 主线程，同步联网会卡整个系统。
     */
    private fun bubbleAfterResolve(shortUrl: String) {
        executor().execute {
            runCatching {
                val real = resolve(shortUrl)
                val realHost = hostOf(real)
                val toIdle = real != null &&
                    isIdleHost(realHost) &&
                    probe(realHost!!, real) == Intent.ACTION_VIEW
                // 解析成功且落在闲鱼域名 → 用**真实地址**打开闲鱼；
                // 否则按原样给淘宝（淘宝接得住短链，行为和以前一致，不误伤淘宝商品）。
                val pkg = if (toIdle) PKG_IDLE else PKG_TAOBAO
                val openUrl = if (toIdle) real!! else shortUrl
                log("Idle: 短链 $shortUrl -> ${real ?: "(失败)"} 归 $pkg")
                bubble(pkg, openUrl)
            }.onFailure {
                // 解析/判定任何一步出错，都要保证「至少别什么都不弹」
                log("Idle: 解析流程异常 $it，按淘宝弹")
                bubble(PKG_TAOBAO, shortUrl)
            }
        }
    }

    /** 抓跳转页里的 `var url = '...'` */
    private fun resolve(shortUrl: String): String? {
        cache[shortUrl]?.let { return it.ifEmpty { null } }
        var out: String? = null
        runCatching {
            val c = (java.net.URL(shortUrl).openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", UA)
            }
            c.connect()
            val body = c.inputStream.bufferedReader().use { r ->
                val sb = StringBuilder()
                val buf = CharArray(8192)
                // 跳转页很小；设个上限，避免异常页面把内存吃满
                while (sb.length < 200_000) {
                    val n = r.read(buf)
                    if (n <= 0) break
                    sb.append(buf, 0, n)
                }
                sb.toString()
            }
            c.disconnect()
            out = SHORT_RE.find(body)?.groupValues?.getOrNull(1)
        }.onFailure { log("Idle: 解析异常 $it") }
        cache[shortUrl] = out ?: ""
        return out
    }

    // ================= 弹气泡 =================

    /** 复用 `Sug` 现成的链接气泡（图标、圆角、点击打开都由它负责） */
    private fun bubble(pkg: String, url: String) {
        val ctx = Cfg.sysContext() ?: Sug.ctx
        if (ctx == null) {
            log("Idle: 无 ctx，放弃弹气泡")
            return
        }
        val post = Runnable {
            runCatching {
                var bmp: android.graphics.Bitmap? = null
                runCatching {
                    val d = ctx.packageManager.getApplicationIcon(pkg)
                    if (d != null) bmp = Sug.circleFitIcon(d, Ui.dp(ctx, 96))
                }
                Sug.showLink(pkg, url, bmp)
                // 气泡自己点开会再次经过 startActivity → onWebUrl；
                // 标记一下，避免再建议一次同样的链接。
                markFired(url)
            }.onFailure { log("Idle: 弹气泡异常 $it") }
        }
        val h = Sug.main
        if (h != null) h.post(post) else post.run()
    }

    // ================= startActivity 定向 =================

    /**
     * 命中闲鱼域名就把 Intent 定向到闲鱼。**原地改**（Intent 是引用传递，下游拿到同一个对象）。
     *
     * @return 是否改写
     */
    internal fun retarget(src: Intent): Boolean {
        if (src.action != Intent.ACTION_VIEW) return false
        // 已经指定明确组件 = 调用方就是要开某个 Activity（比如自己的 WebView 页），不插手
        if (src.component != null) return false
        val data = src.data ?: return false
        val scheme = data.scheme ?: return false
        if (scheme != "http" && scheme != "https") return false
        val host = data.host ?: return false
        // tb.cn 短链**不在这里改**：闲鱼不认这个域名，改包只会让页面打不开。
        if (!isIdleHost(host)) return false

        val act = probe(host, data.toString()) ?: return false
        src.setPackage(PKG_IDLE)
        if (act != Intent.ACTION_VIEW) src.setAction(act)
        log("Idle: 定向 $host act=$act")
        return true
    }

    // ================= 抑制「打开原应用」重复提示 =================

    /** 我们自己刚发起的 URL → 发起时刻（用来抑制 `Sug.onWebUrl` 的重复建议气泡） */
    private val fired = ConcurrentHashMap<String, Long>()

    private fun markFired(url: String) {
        runCatching {
            val now = android.os.SystemClock.uptimeMillis()
            // 顺手清掉过期项，避免 Map 无限增长
            if (fired.size > 64) fired.entries.removeAll { now - it.value > 60_000L }
            fired[url] = now
        }
    }

    /**
     * `Sug.onWebUrl`（网页链接 →「打开原应用」建议气泡）要不要跳过这个 URL。
     *
     * ★ 3359+：**不再整段跳过 tb.cn 短链**。以前 `m.tb.cn` 一律 return true，
     *   指望 Idle 解析后直接打开；结果外部用户「点 m.tb.cn 不弹气泡，s.click.taobao 就弹」。
     *   现在短链也走 `Sug.onWebUrl` 弹气泡（Xianyu 由 Idle 解析后换成真实地址再弹）。
     *
     * 仍然跳过：**我们自己刚发起过的同一个 URL**（避免一次点击建议两次）。
     */
    @JvmStatic
    fun suppressWebTip(url: String?): Boolean {
        if (url == null) return false
        val at = fired[url] ?: return false
        return android.os.SystemClock.uptimeMillis() - at < 15_000L
    }


    /** host → 闲鱼能接住该 URL 时用的 action（缓存；null = 接不住） */
    private val probeCache = ConcurrentHashMap<String, String>()

    // ================= 短链「直接点击」拦截 =================

    /**
     * 处理**直接点击**短链（浏览器/微信里点链接，不经剪贴板）。
     *
     * 三条路：
     * 1. 已经解析过、且落在闲鱼域名 → 把 Intent 的 data 换成真实地址，返回 false，
     *    交给 [retarget] 设包名（同步、零延迟）；
     * 2. 没解析过 → 返回 true 表示**取消原来的跳转**，后台解析后自己发起；
     * 3. 解析过但不是闲鱼 → 返回 false，原样放行（浏览器/淘宝），行为不变。
     *
     * @param callerArgs hook 实参，用来认出「是不是淘宝/闲鱼自己发的」——
     *                   它们内部跳转短链时不能拦，否则会破坏它们自己的流程。
     * @return true = 已接管（调用方需要取消原跳转）
     */
    internal fun takeOverShortClick(intent: Intent, callerArgs: Array<Any?>?): Boolean {
        if (intent.hasExtra(EXTRA_DONE)) return false
        if (intent.action != Intent.ACTION_VIEW) return false
        if (intent.component != null) return false
        val data = intent.data ?: return false
        val scheme = data.scheme ?: return false
        if (scheme != "http" && scheme != "https") return false
        val host = data.host ?: return false
        if (!isTbShortHost(host)) return false
        // 注意：**不能**要求 CATEGORY_BROWSABLE。
        // 微信/QQ 里点链接发出来的 Intent 常常没有这个类目，
        // 要求它会导致「点击没被拦到 → 落到浏览器 → 再弹淘宝气泡」。
        // 淘宝/闲鱼自己发起的链接不拦（位置不固定，直接扫实参里的字符串）
        if (callerArgs != null &&
            callerArgs.any { (it as? String) == PKG_TAOBAO || (it as? String) == PKG_IDLE }
        ) return false

        val url = data.toString()
        val cached = cache[url]
        if (!cached.isNullOrEmpty()) {
            // 已知结果：是闲鱼就换地址交给 retarget；不是就放行
            if (isIdleHost(hostOf(cached))) {
                intent.data = android.net.Uri.parse(cached)
                log("Idle: 短链(已缓存) $host -> 闲鱼")
            }
            return false
        }

        // 没解析过：取消原跳转，解析完自己开
        val original = Intent(intent)
        executor().execute {
            val real = resolve(url)
            if (real != null && isIdleHost(hostOf(real)) &&
                probe(hostOf(real)!!, real) == Intent.ACTION_VIEW
            ) {
                log("Idle: 点击短链 $url -> $real 转闲鱼")
                fire(real, PKG_IDLE)
            } else {
                log("Idle: 点击短链 $url -> ${real ?: "(失败)"} 原样重放")
                fireOriginal(original)
            }
        }
        return true
    }

    /** 自己发起一次跳转（打标记，避免被再次拦截） */
    private fun fire(url: String, pkg: String) {
        val ctx = Cfg.sysContext()
        if (ctx == null) {
            log("Idle: 无 ctx，放弃发起")
            return
        }
        runCatching {
            val i = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
            i.setPackage(pkg)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            i.putExtra(EXTRA_DONE, true)
            markFired(url)
            ctx.startActivity(i)
        }.onFailure { log("Idle: 发起失败 $it") }
    }

    /** 原样重放被取消的跳转（解析失败 / 其实不是闲鱼） */
    private fun fireOriginal(original: Intent) {
        val ctx = Cfg.sysContext()
        if (ctx == null) return
        runCatching {
            val i = Intent(original)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            i.putExtra(EXTRA_DONE, true)
            ctx.startActivity(i)
        }.onFailure { log("Idle: 重放失败 $it") }
    }

    private fun probe(host: String, url: String): String? {
        probeCache[host]?.let { return it.ifEmpty { null } }
        var act: String? = null
        runCatching {
            val pm = Cfg.sysContext()?.packageManager ?: return@runCatching
            val base = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).setPackage(PKG_IDLE)
            if (canHandle(pm, base)) {
                act = Intent.ACTION_VIEW
            } else if (canHandle(pm, Intent(base).setAction(ACTION_IDLE))) {
                act = ACTION_IDLE
            }
        }
        probeCache[host] = act ?: ""
        return act
    }

    private fun canHandle(pm: android.content.pm.PackageManager, i: Intent): Boolean =
        runCatching {
            pm.queryIntentActivities(i, 0).any {
                it.activityInfo != null && PKG_IDLE == it.activityInfo.packageName
            }
        }.getOrDefault(false)

    // ================= 安装 =================

    /**
     * 挂 `ActivityTaskManagerService` 里带 Intent 参数的 `startActivity*`。
     * 所有 App 的 startActivity 最终都走这里，挂这一处就够（实测命中 3 个重载）。
     */
    @JvmStatic
    fun install(cl: ClassLoader?): Int {
        var n = 0
        runCatching {
            val c = Rt.cls(cl, "com.android.server.wm.ActivityTaskManagerService")
                ?: return@runCatching
            for (m in c.declaredMethods) {
                if (m.name != "startActivity" && m.name != "startActivityAsUser") continue
                val ps = m.parameterTypes
                val iIdx = ps.indexOfFirst { it == Intent::class.java }
                if (iIdx < 0) continue
                // bOptions：取**唯一**的 Bundle 形参下标。
                // ⚠️ 绝不能「运行时扫实参找第一个 null 塞 Bundle」——
                // resolvedType/resultWho/profilerInfo 常为 null，会被误当成 options 槽位，
                // 真机表现就是「应用打不开」。
                val oIdx = ps.indexOfFirst { it == Bundle::class.java }
                // callingPackage 紧随 IApplicationThread 之后（标准签名：
                // (IApplicationThread caller, String callingPackage, String callingFeatureId, Intent, …)）。
                // 用参数类型判定，而不是「取第一个 String」——后者遇到别的重载会认错。
                val cIdx = if (ps.isNotEmpty() && ps.size > 1 &&
                    ps[0].name.endsWith("IApplicationThread")
                ) 1 else -1
                // userId：startActivityAsUser 的签名是 (…, Bundle bOptions, int userId, boolean)。
                // 分身应用必须用对用户，否则 startZoomWindow 找不到应用。
                val uIdx = if (oIdx >= 0 && oIdx + 1 < ps.size && ps[oIdx + 1] == Integer.TYPE) {
                    oIdx + 1
                } else -1
                // resultTo(IBinder)：非空即 startActivityForResult，这类不接管（回调会丢）
                val rIdx = ps.indexOfFirst { it == android.os.IBinder::class.java }
                if (XBridge.hookMethod(m, HookIdle(iIdx, oIdx, cIdx, uIdx, rIdx)) != null) n++
            }

            // ===== 其它「正常启动」入口也要接管 =====
            // 实测微信「用浏览器打开」不走 startActivity/startActivityAsUser
            // （日志里只有 XZpath/XZrec，没有 XZdiag），
            // 所以把同为「Intent + 调用方 + options」语义的入口一并接管。
            val takeNames = setOf(
                "startActivityAsCaller", "startActivityWithConfig", "startActivityAndWait"
            )
            for (m in c.declaredMethods) {
                if (m.name !in takeNames) continue
                val ps = m.parameterTypes
                val iIdx2 = ps.indexOfFirst { it == Intent::class.java }
                if (iIdx2 < 0) continue
                val oIdx2 = ps.indexOfFirst { it == Bundle::class.java }
                val cIdx2 = if (ps.isNotEmpty() && ps.size > 1 &&
                    ps[0].name.endsWith("IApplicationThread")
                ) 1 else -1
                val uIdx2 = if (oIdx2 >= 0 && oIdx2 + 1 < ps.size && ps[oIdx2 + 1] == Integer.TYPE) {
                    oIdx2 + 1
                } else -1
                val rIdx2 = ps.indexOfFirst { it == android.os.IBinder::class.java }
                if (XBridge.hookMethod(m, HookIdle(iIdx2, oIdx2, cIdx2, uIdx2, rIdx2)) != null) n++
            }

            // ===== 诊断：ATMS 里其它启动入口（只记录，不接管）=====
            val diagNames = setOf(
                "startActivityIntentSender", "startActivityFromGameSession"
            )
            for (m in c.declaredMethods) {
                if (m.name !in diagNames) continue
                if (m.parameterTypes.none {
                        it == Intent::class.java || it == Array<Intent>::class.java
                    }
                ) continue
                if (XBridge.hookMethod(m, HookXzDiag(m.name)) != null) n++
            }

            // ===== startActivities（Intent[]）：单个时接管 =====
            runCatching {
                for (m in c.declaredMethods) {
                    if (m.name != "startActivities") continue
                    val ps = m.parameterTypes
                    val aIdx = ps.indexOfFirst { it == Array<Intent>::class.java }
                    if (aIdx < 0) continue
                    val cIdx3 = if (ps.isNotEmpty() && ps.size > 1 &&
                        ps[0].name.endsWith("IApplicationThread")
                    ) 1 else -1
                    val oIdx3 = ps.indexOfFirst { it == Bundle::class.java }
                    val uIdx3 = if (oIdx3 >= 0 && oIdx3 + 1 < ps.size && ps[oIdx3 + 1] == Integer.TYPE) {
                        oIdx3 + 1
                    } else -1
                    val rIdx3 = ps.indexOfFirst { it == android.os.IBinder::class.java }
                    if (XBridge.hookMethod(m, HookXzActs(aIdx, cIdx3, uIdx3, rIdx3)) != null) n++
                }
            }

            // ===== 抑制「应用跳转确认」弹窗 =====
            // 决策在 system_server：OplusActivityStartController.isInterceptActivityStart()
            // 与 OplusSecurityPermissionManager 的若干闸门。改设置会被系统改回 1，
            // 所以直接把判据按成 false（见 HookNoIntercept）。
            runCatching {
                val targets = listOf(
                    "com.android.server.am.OplusActivityStartController" to
                        listOf("isInterceptActivityStart"),
                    "com.android.server.am.OplusSecurityPermissionManager" to
                        listOf("isInterceptActivityStart", "needCheckPermission", "isPermissionInterceptEnabled")
                )
                for ((cn, names) in targets) {
                    val k = Rt.cls(cl, cn) ?: continue
                    for (m in k.declaredMethods) {
                        if (!names.contains(m.name)) continue
                        if (m.parameterTypes.isNotEmpty()) continue
                        if (m.returnType != java.lang.Boolean.TYPE) continue
                        if (XBridge.hookMethod(m, HookNoIntercept()) != null) {
                            n++
                            SLog.i("XZNoIntercept: 已挂 " + cn.substringAfterLast('.') + "." + m.name)
                        }
                    }
                }
            }

            // ===== 兜底：把「跳转检查」的最终结果压成 0（放行）=====
            // 微信那条我们自己接管了所以不弹；QQ 的「用浏览器打开」没被接管，
            // 走系统原生流程才弹。压这里保证即使不接管也不会弹。
            //
            // ⚠️ 日志证明：QQ→浏览器 / 微信→更多打开方式 这两次**根本没到 ActivityStarter**
            // （无 XZrec/XZdiag），弹窗是在提交启动**之前**由安全模块拦下的。
            // 所以要把这条链上**所有** checkAllowStartActivity 都压成 0：
            //   OplusSecurityPermissionManager.checkAllowStartActivity      ← binder 入口
            //   OplusActivityStartController.checkAllowStartActivity        ← 最终判定
            //   OplusActivityStartController$ActivityStartInterceptor.…     ← 内部私有
            runCatching {
                val names = listOf(
                    "com.android.server.am.OplusSecurityPermissionManager",
                    "com.android.server.am.OplusActivityStartController",
                    "com.android.server.am.OplusActivityStartController\$ActivityStartInterceptor"
                )
                for (cn in names) {
                    val k = Rt.cls(cl, cn) ?: continue
                    for (m in k.declaredMethods) {
                        if (m.name != "checkAllowStartActivity") continue
                        if (m.returnType != Integer.TYPE) continue
                        if (m.parameterTypes.size != 5) continue
                        if (XBridge.hookMethod(m, HookAllowStart()) != null) {
                            n++
                            SLog.i("XZNoIntercept: 已挂 " + cn.substringAfterLast('.') + ".checkAllowStartActivity")
                        }
                    }
                }
            }

            // 3359+（外部 C16 日志）：新版「应用启动确认」直接旁路。
            //   OplusAppStartConfirmManager.checkStartActivityForConfirm(...) 返回 null = 不拦截；
            //   非 null = 弹确认/拦截。老链路把 checkAllowStartActivity 压成 0，在新链路上
            //   等于"继续走确认"，随后 atms_intercept abort:-91 把 QQ→MT / 安装器的启动取消。
            //   这里直接让确认管理器返回 null，AppLock 检查仍在其后照常执行。
            runCatching {
                val k = Rt.cls(cl, "com.android.server.wm.OplusAppStartConfirmManager") ?: return@runCatching
                var hit = 0
                for (m in k.declaredMethods) {
                    if (m.name != "checkStartActivityForConfirm") continue
                    if (m.returnType.isPrimitive || m.returnType == Void.TYPE) continue
                    if (XBridge.hookMethod(m, HookAppStartConfirm()) != null) hit++
                }
                if (hit > 0) SLog.i("XZNoIntercept: 已挂 OplusAppStartConfirmManager.checkStartActivityForConfirm ×$hit")
                else SLog.i("XZNoIntercept: 未找到 checkStartActivityForConfirm（旧版无此机制）")
            }

            // ===== 诊断：ActivityStarter 层（抓任何启动，含 ActivityRecord 形态）=====
            // 微信那种「所有 ATMS 入口都不经过」的情况，只能在这里看。
            // 2872：实测的注入挂点 ——
            //   execute / executeRequest 才是 mOptions 生效的层级，
            //   在这里补参数后放行，收面板与过渡动画全部交给系统。
            runCatching {
                val asc = Rt.cls(cl, "com.android.server.wm.ActivityStarter") ?: return@runCatching
                var hit = 0
                for (m in asc.declaredMethods) {
                    if (m.name != "execute" && m.name != "executeRequest") continue
                    if (XBridge.hookMethod(m, HookStarter()) != null) hit++
                }
                if (hit > 0) SLog.i("XZoom: Starter 注入钩子已挂 " + hit + " 个")
                else SLog.i("XZoom: ActivityStarter.execute/executeRequest 未找到")
            }
            runCatching {
                val asc = Rt.cls(cl, "com.android.server.wm.ActivityStarter") ?: return@runCatching
                for (m in asc.declaredMethods) {
                    if (m.name != "startActivityInner" && m.name != "startActivityUnchecked") continue
                    if (m.parameterTypes.none { it.name.endsWith("ActivityRecord") }) continue
                    // 只挂日志回调（HookXzPath）；接管改由 ATMS 层负责
                    if (XBridge.hookMethod(m, HookXzPath()) != null) n++
                    // ⚠️ HookXzTake 默认停用（取消会让调用方 App 闪退，见 MEMORY 补记 14）。
                    //    但「选择器转发」这类启动只有这一层看得到 → 做成属性开关，
                    //    `setprop persist.oupao.xz_starter 1` 才启用（实验）。
                    if (m.name == "startActivityInner" &&
                        Cfg.getProp("persist.oupao.xz_starter", "1") != "0" &&   // 默认开启；设为 0 可关
                        XBridge.hookMethod(m, HookXzTake()) != null
                    ) {
                        n++
                        SLog.i("XZtake: startActivityInner 接管已启用(实验)")
                    }
                }
            }
            runCatching {
                val ctl = Rt.cls(cl, "com.android.server.wm.ActivityStartController") ?: return@runCatching
                for (m in ctl.declaredMethods) {
                    if (m.name != "startActivityInPackage" && m.name != "startActivityInTaskFragment") continue
                    if (m.parameterTypes.none { it == Intent::class.java }) continue
                    if (XBridge.hookMethod(m, HookXzPath()) != null) n++
                    // startActivityInPackage 是微信那条路能拿到真实 Intent + 调用方的最早位置，
                    // 在这里接管比 startActivityInner 快得多（见 HookXzCtl 注释）。
                    if (m.name == "startActivityInPackage") {
                        val ps2 = m.parameterTypes
                        val iIdx4 = ps2.indexOfFirst { it == Intent::class.java }
                        // callingPackage：Intent 之前最后一个 String（签名里紧跟 taskId 之后）
                        var cIdx4 = -1
                        for (k in 0 until iIdx4) if (ps2[k] == String::class.java) cIdx4 = k
                        val oIdx4 = ps2.indexOfFirst { it == Bundle::class.java }
                        val uIdx4 = if (oIdx4 >= 0 && oIdx4 + 1 < ps2.size && ps2[oIdx4 + 1] == Integer.TYPE) {
                            oIdx4 + 1
                        } else {
                            // SafeActivityOptions 形态：user id 通常紧跟其后
                            val sIdx = ps2.indexOfFirst { it.name.endsWith("SafeActivityOptions") }
                            if (sIdx >= 0 && sIdx + 1 < ps2.size && ps2[sIdx + 1] == Integer.TYPE) sIdx + 1 else -1
                        }
                        val rIdx4 = ps2.indexOfFirst { it == android.os.IBinder::class.java }
                        if (XBridge.hookMethod(m, HookXzCtl(iIdx4, cIdx4, uIdx4, rIdx4)) != null) n++
                    }
                }
            }
        }
        // ===== 移除应用启动遮罩（跳过启动窗 / starting window）=====
        // 2312 用 ActivityOptions 的 splashScreenStyle=0 验证过无效（日志有、肉眼无变化），
        // 所以改成在窗口管理器里直接不建启动窗。开关由 Cfg.noSplash() 控制。
        runCatching { HookNoSplash.install(cl) }

        // ===== 不可缩放页面（resizeableActivity=false）→ 允许小窗 =====
        // 只在「我们自己发小窗」的 4s 内把目标组件按成可缩放；构造函数只走 after，绝不 returnEarly
        runCatching { HookResizeFix.install(cl) }

        return n
    }

    // ================= 小工具 =================

    @JvmStatic
    fun urlOf(text: String?): String? {
        if (text == null) return null
        return runCatching {
            val i = text.indexOf("http")
            if (i < 0) return null
            var e = i
            while (e < text.length && !text[e].isWhitespace()) e++
            text.substring(i, e)
        }.getOrNull()
    }

    @JvmStatic
    fun hostOf(url: String?): String? =
        runCatching { android.net.Uri.parse(url)?.host }.getOrNull()

    private fun log(msg: String) {
        runCatching { SLog.i(msg) }
    }

    private const val UA =
        "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36"
}
