package com.oupao.wiz

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import java.io.File

/** UI-owned log state. The tested file, configuration and diagnostic channels stay in SLog/Diag. */
class LogCenter(private val context: Context) {
    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    val text = mutableStateOf("等待读取日志…")
    val diagnostic = mutableStateOf("尚未运行诊断")
    val running = mutableStateOf(false)
    val revision = mutableIntStateOf(0)
    private var attached = false
    private var diagnosticRequested = 0L
    private val logListener = SLog.Listener { _, _, line ->
        if (attached) {
            handler.post {
                if (attached) {
                    text.value = (text.value + line).trimEnd().takeLast(MAX_TEXT)
                    revision.intValue++
                }
            }
        }
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            running.value = false
            val raw = intent?.getStringExtra("data").orEmpty()
            val safe = raw.replace('\u0000', ' ').filter { it == '\n' || it == '\t' || it >= ' ' }.take(4000).trim()
            diagnostic.value = tidyDiag(
                safe.ifBlank {
                    StringBuilder().also(Diag::readAll).toString().take(4000).ifBlank { "未收到诊断内容，请确认模块已启用并重启。" }
                }
            )
            revision.intValue++
        }
    }
    private val timeout = Runnable {
        if (running.value && System.currentTimeMillis() - diagnosticRequested >= 4500L) {
            running.value = false
            diagnostic.value = "诊断暂时无响应。请确认 LSPosed 已启用模块、勾选系统框架作用域，并在更新后重启。"
            revision.intValue++
        }
    }

    /**
     * 诊断回包里有一批对用户没意义（甚至误导）的条目，显示前过滤掉：
     * 空值行（如 `RxError=`）、纯调试计数（Rt/Ev 统计、UpTime、分辨率和区域参数）、
     * 以及 `ModApk`（App 进程读不到 system_server 的 apk 大小，永远显示 0B，容易误判）。
     * 其余「键=值」原样保留，将来新加的键不用改这里也会显示。
     */
    private fun tidyDiag(raw: String): String {
        val noise = setOf(
            "android", "Uptime", "ModApk", "Rt", "Ev", "Rx", "RxRetries", "RxBuild", "Zone",
        )
        // 诊断键 -> 中文说明；保留原键名方便对照日志。
        val labels = mapOf(
            "ZoomSvc" to "小窗服务",
            "RxState" to "广播接收状态",
            "Zoom" to "小窗能力",
            "SDK" to "系统 API 版本",
            "CliBuild" to "App 构建版本",
            "SvrBuild" to "系统侧构建版本",
            "WriteFix" to "设置写入修复",
            "SplitFinish" to "分屏收尾",
            "Master" to "功能总开关",
            "ROM" to "系统版本",
            "Fw" to "LSPosed 框架",
            "Bar" to "小白条钩子",
            "FlyM" to "扇形菜单",
            "FlyR" to "菜单状态",
        )
        val boolKeys = setOf("ZoomSvc", "Zoom", "WriteFix", "SplitFinish", "Master", "Bar")
        return raw.lineSequence()
            .map { it.trim() }
            .filter { line ->
                if (line.isEmpty()) return@filter false
                val cut = line.indexOf('=')
                if (cut <= 0) return@filter true
                val key = line.substring(0, cut).trim()
                val value = line.substring(cut + 1).trim()
                value.isNotEmpty() && key !in noise
            }
            .map { line ->
                val cut = line.indexOf('=')
                if (cut <= 0) return@map line
                val key = line.substring(0, cut).trim()
                val value = line.substring(cut + 1).trim()
                val prettyValue = when {
                    key in boolKeys && (value == "0" || value == "1") ->
                        if (value == "1") "已启用" else "未启用"
                    key == "FlyM" && value.length >= 2 ->
                        if (value.startsWith("1")) "已开启（" + value.substring(2) + "）" else "已关闭"
                    key == "RxState" && value.startsWith("registered") ->
                        "已注册（" + value.removePrefix("registered-") + "）"
                    key == "Fw" -> value.replace(" api ", "（API ") + "）"
                    else -> value
                }
                val label = labels[key]
                if (label != null) "$label（$key）=$prettyValue" else "$key=$prettyValue"
            }
            .joinToString("\n")
    }

    /**
     * 3278：只剩两档 —— 0 = 详细（默认）、2 = 关闭。
     * 用户要求默认详细、移除「常规」。
     */
    val mode: Int
        get() = if (Cfg.getInt("oupao_log_quiet", 0) == 1) 2 else 0

    fun setMode(value: Int) {
        // value: 0 = 详细，其它（1/2）= 关闭
        Cfg.save("oupao_log_quiet", value != 0)
        Cfg.save("oupao_dbg", value == 0)
        Cfg.push()
        revision.intValue++
    }

    fun attach() {
        if (attached) return
        attached = true
        refresh()
        SLog.listener = logListener
        runCatching {
            val filter = IntentFilter("com.oupao.wiz.DIAG")
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION") appContext.registerReceiver(receiver, filter)
            }
        }
    }

    fun detach() {
        attached = false
        if (SLog.listener === logListener) SLog.listener = null
        runCatching { appContext.unregisterReceiver(receiver) }
        handler.removeCallbacks(timeout)
        running.value = false
    }

    fun refresh() {
        text.value = readTail().ifBlank { "等待模块日志…新的模块事件会实时显示在这里。" }
        revision.intValue++
    }

    fun runDiagnostic() {
        diagnosticRequested = System.currentTimeMillis()
        running.value = true
        diagnostic.value = "正在读取 system_server、LSPosed 与本机版本…"
        revision.intValue++
        Diag.sendReq(appContext)
        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, 4600L)
    }

    fun copy() {
        runCatching {
            val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("欧泡模块日志", text.value))
            Toast.makeText(appContext, "日志已复制", Toast.LENGTH_SHORT).show()
        }.onFailure { Toast.makeText(appContext, "复制失败，请稍后重试", Toast.LENGTH_SHORT).show() }
    }

    fun clear() {
        SLog.listener = null
        Thread({
            runCatching { File(LOG_PATH).writeText("") }
                .recoverCatching { Runtime.getRuntime().exec(arrayOf("su", "-c", ": > $LOG_PATH")).waitFor() }
                .onFailure {
                    handler.post { Toast.makeText(appContext, "清空失败，请检查 root 授权", Toast.LENGTH_SHORT).show() }
                }
            handler.post {
                if (attached) SLog.listener = logListener
                refresh()
            }
        }, "Oupao-LogClear").start()
    }

    private fun readTail(): String {
        val direct = runCatching {
            val file = File(LOG_PATH)
            if (!file.isFile || file.length() == 0L) return@runCatching ""
            file.inputStream().use { stream ->
                val length = stream.available().toLong()
                if (length > TAIL_BYTES) stream.skip(length - TAIL_BYTES)
                stream.readBytes().toString(Charsets.UTF_8).lineSequence().toList().takeLast(180).joinToString("\n")
            }
        }.getOrNull()
        if (!direct.isNullOrBlank()) return direct
        // /data/system is unreadable from the app process; live events still arrive via SLog.
        return ""
    }

    companion object {
        private const val LOG_PATH = "/data/system/oupao.log"
        private const val TAIL_BYTES = 96_000L
        private const val MAX_TEXT = 120_000
    }
}
