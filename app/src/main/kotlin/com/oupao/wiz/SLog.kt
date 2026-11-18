package com.oupao.wiz

import android.os.SystemClock
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日志。对应原 smali 的 com.oupao.wiz.SLog。
 *
 * 行为要点（已逐一对照 1741 smali 还原）：
 *  - 级别：oupao_dbg=1 → 2(verbose)；oupao_log_quiet=1 → 0(静默)；否则 1(info)。
 *    级别结果缓存 5536ms(0x15a0)，避免每次都读文件。
 *  - 去重：同 tag + 同 msg 在 1000ms 内只输出一次（防高频 hook 刷屏）。
 *  - 落盘：/data/system/oupao.log，超过 2MB 删除重建；每 16 条 flush 一次。
 *  - [x] 用于异常限流：同一 tag 最多打 3 次。
 */
object SLog {

    private const val TAG = "oupao"
    private const val LOG_PATH = "/data/system/oupao.log"
    // 1744 起日志上限从 2MB 放宽到 4MB（对齐原 smali 的 0x400000）
    // 3279：用户要求再放宽到 8MB（默认详细级别下 V 日志较多，4MB 太快被截断）
    private const val MAX_LOG_SIZE = 8 * 1024 * 1024
    private const val LEVEL_TTL = 5536L
    private const val DEDUP_WINDOW = 1000L

    private val tl = ThreadLocal<FileWriter>()

    @Volatile private var cnt = 0
    @Volatile private var dirOk = false
    @Volatile private var lastMsg: String? = null
    @Volatile private var lastTag: String? = null
    @Volatile private var lastTs = 0L
    @Volatile private var lvl = 1
    @Volatile private var lvlTs = 0L
    @Volatile private var tsSec = 0L
    @Volatile private var tsStr = ""

    /** 异常计数：tag → 已打印次数 */
    private val xc = HashMap<String, Int>()

    // ---------- 级别 ----------

    private fun level(): Int {
        return runCatching {
            val now = SystemClock.uptimeMillis()
            if (now - lvlTs > LEVEL_TTL) {
                lvlTs = now
                // 3278：**默认详细**（用户要求）；只有显式“关闭日志”才静默。
                lvl = if (Cfg.getInt("oupao_log_quiet", 0) == 1) 0 else 2
            }
            lvl
        }.getOrDefault(2)
    }

    // ---------- 对外接口（语义与原实现一致） ----------
    fun interface Listener {
        fun onLog(tag: String, message: String, line: String)
    }

    /** Only the visible in-app log page subscribes; no writer-thread UI work. */
    @Volatile @JvmStatic var listener: Listener? = null

    /** 错误级：始终输出（除非静默） */
    @JvmStatic
    fun e(msg: String) = e(msg, null)

    @JvmStatic
    fun e(msg: String, t: Throwable?) {
        // t 为 null 时不要走 flog：XBridge.flog 会拼出 "exception null"，
        // 旧版（KT_PACKAGE_BUG_REPORT P1）就是这么污染框架日志的。
        if (t != null) runCatching { XBridge.flog(t) }
        // 同样，消息尾巴不要留 " : "（t 为 null 时）
        w("E", if (t != null) "$msg : $t" else msg)
    }

    /** info 级：level >= 1 */
    @JvmStatic
    fun i(msg: String) {
        runCatching { if (level() >= 1) w("I", msg) }
    }

    /** verbose 级：level >= 2（需开 oupao_dbg） */
    @JvmStatic
    fun v(msg: String) {
        runCatching { if (level() >= 2) w("V", msg) }
    }

    /**
     * verbose 级 + 异常。给「要 new Throwable 才打得出」的调用栈类日志用：
     * 调用方可以先 [dbgOn] 判断，避免在不该输出时仍付出建 Throwable 的代价。
     */
    @JvmStatic
    fun v(msg: String, t: Throwable?) {
        runCatching {
            if (level() < 2) return
            w("V", if (t != null) "$msg : $t" else msg)
            if (t != null) XBridge.flog(t)
        }
    }

    /**
     * 当前是否处于 verbose 级别（oupao_dbg=1）。
     * 用于「日志参数构造很贵」的场景（new Throwable、大集合 toString、Bitmap 信息等），
     * 先判断再构造，避免白白付出开销。
     */
    @JvmStatic
    fun dbgOn(): Boolean = runCatching { level() >= 2 }.getOrDefault(false)

    /** 关键级：level > 0 */
    @JvmStatic
    fun k(msg: String) {
        runCatching { if (level() > 0) w("K", msg) }
    }

    /**
     * 异常限流日志：同一 tag 最多输出 3 次，之后静默。
     * 用于 hook 回调里高频抛异常的场景，避免日志爆炸。
     */
    @JvmStatic
    fun x(tag: String, t: Throwable) {
        runCatching {
            val n = (xc[tag] ?: 0) + 1
            xc[tag] = n
            if (xc.size > 64) xc.clear()
            if (n <= 3) w("E", "$tag x$n : ${t}")
        }
    }

    @JvmStatic
    fun flushNow() {
        runCatching { tl.get()?.flush() }
    }

    @JvmStatic
    fun reset() {
        runCatching { tl.remove() }
    }

    // ---------- 核心写入 ----------

    private fun w(tag: String, msg: String) {
        if (level() == 0) return

        val now = SystemClock.uptimeMillis()
        // 去重：同 tag + 同 msg 且 1s 内
        if (msg == lastMsg && tag == lastTag && now - lastTs <= DEDUP_WINDOW) return
        lastMsg = msg
        lastTag = tag
        lastTs = now

        runCatching { listener?.onLog(tag, msg, line(tag, msg)) }

        runCatching {
            Log.i(TAG, msg)
            XBridge.flog(msg)
        }

        try {
            if (!dirOk) {
                File("/data/system").mkdirs()
                dirOk = true
            }
            val fw = fw() ?: return
            fw.write(line(tag, msg))
            cnt++
            if (cnt and 0xF == 0) fw.flush()
        } catch (_: Throwable) {
            reset()
            Log.i(TAG, msg)
        }
    }

    /**
     * 每线程一个 FileWriter（追加模式），日志超 8MB 先清空。
     *
     * ★ 2949：**清空不要用 delete()**。以前这里是 `f.delete()`：文件被删掉后，
     *   其它线程手上那份（已打开的）FileWriter 还指着「已经 unlink 的 inode」，
     *   它们之后的日志会全部写进一个看不见的文件里 —— 表现就是**某个线程的日志整段消失**
     *   （排查 2949 的「空闲刷新/最近应用刷新到底跑没跑」时就被这个坑了很久：
     *   实际一直在跑，只是那几条日志落在被删掉的 inode 里）。
     *   改成截断（保持同一个 inode），所有 writer 继续有效，日志只是从头再来。
     */
    private fun fw(): FileWriter? {
        return runCatching {
            tl.get() ?: run {
                val f = File(LOG_PATH)
                if (f.length() > MAX_LOG_SIZE) {
                    runCatching { java.io.FileOutputStream(f, false).close() }
                }
                FileWriter(LOG_PATH, true).also { tl.set(it) }
            }
        }.getOrNull()
    }

    /** 时间戳每秒只格式化一次 */
    private fun line(tag: String, msg: String): String {
        return try {
            val now = System.currentTimeMillis()
            if (now - tsSec >= 1000) {
                tsSec = now
                tsStr = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(now))
            }
            "$tsStr $tag $msg\n"
        } catch (_: Throwable) {
            " $tag $msg\n"
        }
    }

    /** 按 tag 节流：同一 tag 每 everyMs 只输出一次（触摸/移动级热日志用，避免日志风暴） */
    private val rTs = HashMap<String, Long>()

    @JvmStatic
    fun r(tag: String, msg: String, everyMs: Long = 500L) {
        runCatching {
            if (level() <= 0) return
            val now = SystemClock.uptimeMillis()
            val last = rTs[tag] ?: 0L
            if (now - last < everyMs) return
            rTs[tag] = now
            w("I", msg)
        }
    }
}
