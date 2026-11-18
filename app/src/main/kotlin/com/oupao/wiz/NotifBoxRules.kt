package com.oupao.wiz

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

/**
 * 「通知收纳」的**按应用 + 通知类型**规则表（用户 2026-11-07 13:5x 提出）。
 *
 * ## 规则是什么
 * 用户长按某条通知 → 选「收纳」时，可以顺带记一条**永久规则**：
 * 「这个包的这个通知类型，以后来了直接进收纳盒」。
 * 规则键 = `包名|类型`（类型 = 通知的 channelId / 或者一个"类型签名"）。
 *
 * ## 为什么要"类型"而不是只按包
 * 同一个 App 往往有多种通知（微信：聊天消息 / 朋友圈 / 公众号 / 撤回…）。
 * 只按包收会把所有通知都吞掉，用户想"只收公众号推送"就做不到。
 * 所以规则按 `包名 + 类型签名` 存，一条规则只影响同一类型。
 *
 * ## 类型签名怎么算（尽量稳定，不随文案变化）
 * 优先级：
 *  1. `sbn.getNotification().getChannelId()` —— 系统通知渠道，最接近"通知类型"，但 channelId
 *     可能是应用自己随机生成的 id，仍然比文案稳；
 *  2. 退一步用 `sbn.getTag()`（App 自己打的 tag，多数 App 用它区分类型）；
 *  3. 再退一步用 `sbn.getId()`（同一 App 的多条通知会撞 id，所以只在前两者都没有时用）。
 * 拿不到 sbn 时退回 `entry.getKey()` 前缀（= 包名）。
 *
 * ## 存哪
 * ★ SystemUI 进程**写不进** `Settings.Global`（属主校验直接拒，3005 实测），
 *   但它**能写自己的 `context.filesDir`**。所以规则表存在 SystemUI 的 data 目录里
 *   （`notifbox_rules.txt`），模块 App 侧要显示/编辑时通过**广播**让 SystemUI 回传。
 *
 * ## 规则集合
 * - [rules]：键 = `pkg|typeSig`，值 = 规则记录（含 App 名/类型名，便于列表显示）。
 * - 命中规则的新通知：**直接进收纳盒**（静默），和手动收纳一样。
 */
object NotifBoxRules {

    private const val TAG = "NBX-RULE"

    /** SystemUI data 目录里的文件名 */
    private const val FILE = "notifbox_rules.txt"

    /** 键分隔符（包名里不会出现 '|'） */
    private const val SEP = "|"

    /** 一条规则 */
    data class Rule(
        /** 包名 */
        val pkg: String,
        /** 类型签名（见文件头说明） */
        val sig: String,
        /** App 显示名（列表里好看） */
        val appName: String,
        /** 类型显示名（列表里好看，可能是空） */
        val typeName: String
    ) {
        val key: String get() = pkg + SEP + sig

        fun serialize(): String = listOf(pkg, sig, appName, typeName)
            .joinToString(SEP) { it.replace('\n', ' ') }
    }

    // ---------------- 内存表 ----------------

    private val rules: MutableMap<String, Rule> = ConcurrentHashMap()

    @Volatile
    private var loaded = false

    // ---------------- 存储 ----------------

    private fun ctx(): Context? =
        runCatching { Ctx.sysUi() }.getOrNull() ?: runCatching { Rx.ctxB() }.getOrNull()

    private fun file(c: Context) = java.io.File(c.filesDir ?: c.dataDir, FILE)

    fun ensureLoaded() {
        if (loaded) return
        loaded = true
        runCatching {
            val c = ctx() ?: return
            val f = file(c)
            if (!f.exists()) {
                SLog.i("$TAG: 规则表还不存在（首次使用）")
                return
            }
            rules.clear()
            f.readLines().forEach { line ->
                val r = parse(line)
                if (r != null) rules[r.key] = r
            }
            SLog.i("$TAG: 读回规则 " + rules.size + " 条")
        }.onFailure { SLog.e("$TAG: 读规则失败", it) }
    }

    private fun parse(line: String): Rule? {
        if (line.isBlank()) return null
        val p = line.split(SEP)
        if (p.size < 2) return null
        return Rule(p[0], p[1], p.getOrElse(2) { "" }, p.getOrElse(3) { "" })
    }

    private fun save() {
        runCatching {
            ensureLoaded()
            val c = ctx() ?: return
            file(c).writeText(rules.values.joinToString("\n") { it.serialize() })
        }.onFailure { SLog.e("$TAG: 写规则失败", it) }
    }

    // ---------------- 增删查 ----------------

    /** 加一条规则（同键覆盖） */
    fun add(r: Rule) {
        ensureLoaded()
        if (r.pkg.isBlank()) return
        rules[r.key] = r
        save()
        SLog.i("$TAG: 加规则 " + r.pkg + " / " + r.typeName.ifBlank { r.sig } + " 共=" + rules.size)
    }

    /** 删一条规则 */
    fun remove(pkg: String, sig: String) {
        ensureLoaded()
        val k = pkg + SEP + sig
        if (rules.remove(k) == null) return
        save()
        SLog.i("$TAG: 删规则 $pkg / $sig 剩=" + rules.size)
    }

    /** 是否命中规则（该包 + 该类型已被标记为自动收纳） */
    fun matches(pkg: String?, sig: String?): Boolean {
        if (pkg.isNullOrEmpty() || sig.isNullOrEmpty()) return false
        ensureLoaded()
        return rules.containsKey(pkg + SEP + sig)
    }

    /** 当前所有规则（列表用） */
    fun all(): List<Rule> {
        ensureLoaded()
        return rules.values.sortedWith(compareBy({ it.appName }, { it.pkg }, { it.typeName }))
    }

    fun count(): Int {
        ensureLoaded()
        return rules.size
    }

    /** 清空全部规则 */
    fun clearAll() {
        ensureLoaded()
        rules.clear()
        save()
        SLog.i("$TAG: 规则清空")
    }

    // ---------------- 签名计算 ----------------

    /**
     * 从 `NotificationEntry` 算出 `pkg|sig`。
     * 返回 null 表示取不到（例如 entry 为空）。
     */
    fun signatureOf(entry: Any?): Pair<String, String>? {
        val sbn = sbnOf(entry) ?: return null
        val pkg = runCatching { Rt.callNoArg(sbn, "getPackageName") }.getOrNull() as? String
            ?: return null
        val sig = sigOf(sbn)
        return pkg to sig
    }

    /** `NotificationEntry#getSbn()`（本机未混淆） */
    fun sbnOf(entry: Any?): Any? {
        if (entry == null) return null
        return runCatching { Rt.callNoArg(entry, "getSbn") }.getOrNull()
            ?: runCatching { Rt.callNoArg(entry, "getSBN") }.getOrNull()
    }

    /**
     * 类型签名：channelId > tag > id > key 前缀。
     * 全部取不到时返回空串（调用方据此跳过规则）。
     */
    private fun sigOf(sbn: Any): String {
        // 1) channelId —— 最接近"通知类型"
        val ch = runCatching {
            val n = Rt.callNoArg(sbn, "getNotification")
            if (n == null) null else Rt.callNoArg(n, "getChannelId")
        }.getOrNull() as? String
        if (!ch.isNullOrEmpty()) return "c:" + ch

        // 2) tag —— 应用自己打的类型标记
        val tg = runCatching { Rt.callNoArg(sbn, "getTag") }.getOrNull() as? String
        if (!tg.isNullOrEmpty()) return "t:" + tg

        // 3) id —— 同一 App 内可能撞车，作为最后的兜底
        val id = runCatching { Rt.callNoArg(sbn, "getId") }.getOrNull() as? Int
        if (id != null) return "i:" + id

        // 4) 实在没有 → 用 key 前缀（= 包名）
        val key = runCatching { Rt.callNoArg(sbn, "getKey") }.getOrNull() as? String
        return if (!key.isNullOrEmpty()) "k:" + key.substringBefore('#') else ""
    }

    /** 显示用的类型名：尽量取通知标题（用户能认出来） */
    fun typeNameOf(entry: Any?): String {
        val sbn = sbnOf(entry) ?: return ""
        val n = runCatching { Rt.callNoArg(sbn, "getNotification") }.getOrNull() ?: return ""
        val t = runCatching { Rt.callNoArg(n, "getTitle") }.getOrNull() ?: return ""
        val s = t.toString()
        return if (s.length <= 24) s else s.substring(0, 24) + "…"
    }

    /** 显示用的 App 名 */
    fun appNameOf(pkg: String?): String {
        if (pkg.isNullOrEmpty()) return ""
        return runCatching {
            val c = ctx() ?: return@runCatching pkg
            val pm = c.packageManager
            val ai = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(ai).toString()
        }.getOrDefault(pkg)
    }
}
