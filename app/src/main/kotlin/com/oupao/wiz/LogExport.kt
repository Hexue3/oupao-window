package com.oupao.wiz

import android.content.Context
import android.content.Intent
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 3284：模块界面「一键导出日志」——导出后直接拉起系统分享。
 *
 * 打包内容（都是为了"排除我们自己的 bug"能一次看全）：
 *  ```
 *  oupao.log                    模块日志（默认详细、上限 8MB）
 *  modules_*.log ×2             LSPosed 模块日志（最新 2 个）
 *  verbose_*.log ×1             LSPosed verbose（最新 1 个）
 *  logcat.txt                   logcat -b all -d 最后 3 万行（系统侧窗口/输入/崩溃）
 *  dmesg.txt                    内核日志（重启原因、watchdog、OOM）
 *  input.txt                    dumpsys input（手势区、RemoteObserver）
 *  windows.txt                  dumpsys window windows（悬浮窗/层级）
 *  activity.txt                 dumpsys activity activities（前台 task）
 *  anr/                         全部 ANR trace
 *  tombstone/                   全部 native tombstone
 *  dropbox/                     最近 40 条系统 dropbox（watchdog / SYSTEM_BOOT / crash）
 *  props.txt / cfg.txt / version.txt / _files.txt
 *  ```
 *
 * **为什么必须 root**：`/data/system/`、`/data/adb/lspd/log/`、`/data/anr/`、`/data/tombstones/`
 * 都是 root/system 私有目录，普通 App 连目录都打不开。
 *
 * 流程：root 拷到 App 的 cache → App 递归打包 → FileProvider 分享 → 中间目录立即删、
 * zip 十分钟后自动删（3283），下次导出前也会先清一次。
 */
object LogExport {

    @JvmStatic
    fun exportAndShare(ctx: Context): String {
        val ts = SimpleDateFormat("MMdd_HHmmss", Locale.US).format(Date())
        // 先清上一次的残留（中间目录 + 旧 zip）
        runCatching {
            ctx.cacheDir.listFiles()?.forEach { f ->
                if (f.name.startsWith("export_") ||
                    (f.name.startsWith("oupao_logs_") && f.name.endsWith(".zip"))
                ) {
                    f.deleteRecursively()
                }
            }
        }
        val d = File(ctx.cacheDir, "export_$ts").absolutePath
        // LF 换行的 sh 脚本，别改成 CRLF
        val script = """
            rm -rf "$d"
            mkdir -p "$d/anr" "$d/tombstone" "$d/dropbox"
            cp -f /data/system/oupao.log "$d/" 2>/dev/null
            cp -f $(ls -t /data/adb/lspd/log/modules_*.log 2>/dev/null | head -2) "$d/" 2>/dev/null
            cp -f $(ls -t /data/adb/lspd/log/verbose_*.log 2>/dev/null | head -1) "$d/" 2>/dev/null
            logcat -b all -d -t 30000 > "$d/logcat.txt" 2>/dev/null
            dmesg > "$d/dmesg.txt" 2>/dev/null
            dumpsys input 2>/dev/null | head -c 1200000 > "$d/input.txt"
            dumpsys window windows 2>/dev/null | head -c 1200000 > "$d/windows.txt"
            dumpsys activity activities 2>/dev/null | head -c 400000 > "$d/activity.txt"
            cp -f /data/anr/* "$d/anr/" 2>/dev/null
            cp -f /data/tombstones/* "$d/tombstone/" 2>/dev/null
            for f in $(ls -t /data/system/dropbox/* 2>/dev/null | head -40); do cp -f "${'$'}f" "$d/dropbox/" 2>/dev/null; done
            getprop > "$d/props.txt" 2>/dev/null
            dumpsys package com.oupao.wiz | grep versionName > "$d/version.txt" 2>/dev/null
            settings list system | grep oupao_cfg_ > "$d/cfg.txt" 2>/dev/null
            chmod -R 777 "$d" 2>/dev/null
            ls -lR "$d" > "$d/_files.txt" 2>/dev/null
            echo EXPORT_OK $(find "$d" -type f | wc -l)
        """.trimIndent()
        return runCatching {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", script))
            val out = p.inputStream.bufferedReader().readText()
            val err = p.errorStream.bufferedReader().readText()
            val code = p.waitFor()
            val n = out.lineSequence().firstOrNull { it.startsWith("EXPORT_OK") }
                ?.removePrefix("EXPORT_OK")?.trim()?.toIntOrNull() ?: 0
            SLog.i("LogExport: code=$code files=$n err=${err.trim()}")
            if (code != 0 || n <= 0) {
                // 具体错误只写日志（err 里可能是 shell 的英文报错，界面上统一提示授权）
                return "导出失败：请授权root权限后重新尝试"
            }
            // 打包（递归，包含 anr/ tombstone/ dropbox/ 子目录）
            val zip = File(ctx.cacheDir, "oupao_logs_$ts.zip")
            val base = File(d)
            FileOutputStream(zip).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    base.walkTopDown().filter { it.isFile }.sortedBy { it.absolutePath }.forEach { f ->
                        val name = f.absolutePath.removePrefix(base.absolutePath).trimStart('/')
                        zos.putNextEntry(ZipEntry(name))
                        f.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
            SLog.i("LogExport: zip=" + zip.absolutePath + " size=" + zip.length())
            runCatching { base.deleteRecursively() }   // 中间目录打完包就删
            share(ctx, zip, ts)
            // 给接收方几分钟取文件，再自动删掉这份 cache
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                runCatching {
                    if (zip.exists() && zip.delete()) SLog.i("LogExport: 已自动删除 " + zip.name)
                }
            }, 10 * 60 * 1000L)
            "已分享 $n 个文件（${zip.length() / 1024}KB）\n${zip.name}（10 分钟后自动清理）"
        }.getOrElse {
            SLog.e("LogExport: 异常", it)
            "导出失败：请授权root权限后重新尝试"
        }
    }

    private fun share(ctx: Context, zip: File, ts: String) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            ctx, ctx.packageName + ".fileprovider", zip
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "欧泡小窗优化 日志 $ts")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "分享日志").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            runCatching { ctx.startActivity(chooser) }
                .onFailure { SLog.e("LogExport: 拉起分享失败", it) }
        }
    }
}
