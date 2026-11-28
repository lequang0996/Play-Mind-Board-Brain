package com.fws.playmind.match

import android.content.Context
import android.util.Log
import com.fws.playmind.core.BotLevel
import com.fws.playmind.core.GoGame
import com.fws.playmind.core.KataLevels
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Bot cờ vây dùng KataGo (mã MIT, mạng g170 CC0; xem docs/licenses) chạy như tiến trình con nói giao thức GTP.
 *
 *  - Tệp chạy được đóng gói thành `libkatago.so` trong jniLibs (biên dịch bằng NDK trên CI, xem .github/workflows/build.yml)
 *    để Android cho phép chạy từ thư mục thư viện của app.
 *  - Bảng 7 cấp nằm ở core (KataLevels) để bộ đo trên CI dùng đúng cấu hình này.
 *  - Hai mạng trong assets/katago: b6 (6 khối, nhỏ) cho các cấp thấp, b10 (10 khối) cho các cấp cao (BOT_GUIDE mục 8.3a).
 *  - Luật khớp với GoGame: luật diện tích không gỡ quân chết (friendlyPassOk = false: phải bắt hết quân chết trước khi bỏ lượt),
 *    siêu ko theo vị trí, cấm tự sát, komi 7,5, không đầu hàng.
 *
 * Không chạy được (thiếu tệp, ABI không hỗ trợ, lỗi tiến trình) thì [move] trả null và app dùng bot MCTS dự phòng của core.
 */
class KataGo(private val ctx: Context) {

    companion object {
        private const val TAG = "KataGo"
        private const val KOMI = KataLevels.KOMI
        fun vertex(cell: Int, size: Int): String = KataLevels.vertex(cell, size)
        fun cellOf(v: String, size: Int): Int = KataLevels.cellOf(v, size)
    }

    private var proc: Process? = null
    private var out: OutputStreamWriter? = null
    private var inp: BufferedReader? = null
    private var procKey = ""
    private val io = Executors.newSingleThreadExecutor { r -> Thread(r, "katago-io").apply { isDaemon = true } }
    @Volatile private var broken = false

    private val dir: File get() = File(ctx.filesDir, "katago")
    private val binary: File get() = File(ctx.applicationInfo.nativeLibraryDir, "libkatago.so")

    /** Có đủ tệp chạy và mạng không (kiểm tra nhanh, không khởi động tiến trình). */
    fun available(): Boolean {
        if (broken || !binary.exists()) return false
        return try { ctx.assets.list("katago")?.contains("b10.bin.gz") == true } catch (_: Exception) { false }
    }

    private fun modelFile(name: String): File {
        val f = File(dir, "$name.bin.gz")
        if (!f.exists() || f.length() == 0L) {
            dir.mkdirs()
            val tmp = File(dir, "$name.tmp")
            ctx.assets.open("katago/$name.bin.gz").use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
            tmp.renameTo(f)
        }
        return f
    }

    private fun configFile(lv: KataLevels.Lv, timeMs: Long): File {
        dir.mkdirs()
        val threads = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
        val maxTime = (timeMs / 1000.0).coerceAtLeast(1.0)
        val f = File(dir, "pm.cfg"); f.writeText(KataLevels.configText(lv, threads, maxTime))
        return f
    }

    private fun ensure(lv: KataLevels.Lv, timeMs: Long): Boolean {
        val key = "${lv.model}/${lv.visits}/${lv.temperature}/$timeMs"
        if (proc != null && procKey == key && alive(proc!!)) return true
        stop()
        return try {
            val model = modelFile(lv.model)
            val cfg = configFile(lv, timeMs)
            val p = ProcessBuilder(binary.absolutePath, "gtp", "-model", model.absolutePath, "-config", cfg.absolutePath)
                .directory(dir).start()
            // xả stderr để tiến trình không bị chặn khi bộ đệm đầy
            Thread({ try { p.errorStream.bufferedReader().forEachLine { } } catch (_: Exception) {} }, "katago-err").apply { isDaemon = true }.start()
            proc = p; out = OutputStreamWriter(p.outputStream); inp = BufferedReader(InputStreamReader(p.inputStream))
            procKey = key
            val ok = cmd("boardsize 19", 60_000) != null && cmd("komi $KOMI", 10_000) != null
            if (!ok) { stop(); broken = true }
            ok
        } catch (e: Exception) {
            Log.w(TAG, "không khởi động được KataGo: ${e.message}")
            stop(); broken = true; false
        }
    }

    /** Gửi một lệnh GTP, trả về phần nội dung sau "=" (null nếu lỗi / quá thời gian). */
    private fun cmd(c: String, timeoutMs: Long): String? {
        val o = out ?: return null; val i = inp ?: return null
        val fut = io.submit<String?> {
            o.write(c); o.write("\n"); o.flush()
            val sb = StringBuilder()
            var first: String? = null
            while (true) {
                val line = i.readLine() ?: return@submit null
                if (first == null) { if (line.isBlank()) continue; first = line } else if (line.isBlank()) break
                sb.append(line).append('\n')
            }
            val s = sb.toString().trim()
            if (s.startsWith("=")) s.substring(1).trim() else null
        }
        return try { fut.get(timeoutMs, TimeUnit.MILLISECONDS) } catch (e: Exception) {
            Log.w(TAG, "lệnh '$c' lỗi: ${e.message}"); fut.cancel(true); stop(); null
        }
    }

    /**
     * Nước của bot ở cấp [level] cho thế [g] (19x19), hoặc null nếu KataGo không dùng được.
     * [timeMs] là ngân sách của Siêu việt (botMillis).
     */
    @Synchronized
    fun move(g: GoGame, level: BotLevel, timeMs: Long, rnd: Random): Int? {
        if (!available()) return null
        val lv = KataLevels.of(level.ordinal)
        if (!ensure(lv, timeMs)) return null
        val size = g.size
        if (cmd("clear_board", 10_000) == null) return null
        var color = "B"
        for (m in g.history) {
            if (cmd("play $color ${vertex(m, size)}", 10_000) == null) return null
            color = if (color == "B") "W" else "B"
        }
        val toMove = if (g.turn == 0) "B" else "W"
        if (lv.raw) {
            val res = cmd("kata-raw-nn ${toMove.lowercase()} 0", 30_000) ?: return null
            return KataLevels.pickFromPolicy(g, res, lv.temperature, rnd)
        }
        val res = cmd("genmove $toMove", timeMs + 60_000) ?: return null
        val c = cellOf(res, size)
        if (c < 0 || !g.isLegal(c)) return null
        // bất biến cứng ở mọi cấp: không tự lấp mắt thật (mục 8.2)
        if (g.isBlunder(c, false)) return KataLevels.safest(g, rnd)
        return c
    }

    /** Process.isAlive() chỉ có từ API 26; app hỗ trợ từ API 24. */
    private fun alive(p: Process): Boolean = try { p.exitValue(); false } catch (_: IllegalThreadStateException) { true }

    private fun stop() {
        try { out?.write("quit\n"); out?.flush() } catch (_: Exception) {}
        try { proc?.destroy() } catch (_: Exception) {}
        proc = null; out = null; inp = null; procKey = ""
    }

    fun close() { synchronized(this) { stop() }; io.shutdownNow() }
}
