package com.fws.playmind.match

import android.content.Context
import android.util.Log
import com.fws.playmind.core.BotLevel
import com.fws.playmind.core.GoGame
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.exp
import kotlin.math.ln
import kotlin.random.Random

/**
 * Bot cờ vây dùng KataGo (mã MIT, mạng g170 CC0; xem docs/licenses) chạy như tiến trình con nói giao thức GTP.
 *
 *  - Tệp chạy được đóng gói thành `libkatago.so` trong jniLibs (biên dịch bằng NDK trên CI, xem .github/workflows/build.yml)
 *    để Android cho phép chạy từ thư mục thư viện của app.
 *  - Hai mạng trong assets/katago: b6 (6 khối, nhỏ) cho các cấp thấp, b10 (10 khối) cho các cấp cao (BOT_GUIDE mục 8.3a).
 *  - Luật khớp với GoGame: luật diện tích không gỡ quân chết (friendlyPassOk = false: phải bắt hết quân chết trước khi bỏ lượt),
 *    siêu ko theo vị trí, cấm tự sát, komi 7,5, không đầu hàng.
 *
 * Không chạy được (thiếu tệp, ABI không hỗ trợ, lỗi tiến trình) thì [move] trả null và app dùng bot MCTS dự phòng của core.
 */
class KataGo(private val ctx: Context) {

    /** Cấu hình một cấp. visits = 0 ở Siêu việt nghĩa là dùng thời gian (botMillis). raw = lấy mẫu trực tiếp theo chính sách mạng. */
    class Lv(val model: String, val visits: Int, val temperature: Double, val raw: Boolean)

    companion object {
        private const val TAG = "KataGo"
        private const val KOMI = 7.5

        /**
         * 7 cấp (BOT_GUIDE mục 8.3a; giả thuyết khởi điểm, CHƯA đo trên điện thoại thật).
         * Dễ/Trung bình: 1 lần chạy mạng nhỏ rồi lấy mẫu theo chính sách với nhiệt độ cao, kèm lọc nước tự lấp mắt / tự atari.
         * Từ Khá trở lên: tìm kiếm MCTS của KataGo với số lượt tăng dần (mỗi bậc ×4 ≈ 200 Elo với g ≈ 100 ở mục 4.3).
         */
        val LEVELS = listOf(
            Lv("b6", 1, 1.6, true),
            Lv("b6", 1, 0.8, true),
            Lv("b6", 12, 0.5, false),
            Lv("b10", 40, 0.2, false),
            Lv("b10", 160, 0.0, false),
            Lv("b10", 480, 0.0, false),
            Lv("b10", 0, 0.0, false),
        )

        private val COLS = "ABCDEFGHJKLMNOPQRST"
        fun vertex(cell: Int, size: Int): String =
            if (cell == GoGame.PASS) "pass" else "${COLS[cell % size]}${size - cell / size}"
        fun cellOf(v: String, size: Int): Int {
            val t = v.trim().uppercase()
            if (t == "PASS") return GoGame.PASS
            if (t.length < 2) return -1
            val x = COLS.indexOf(t[0]); val row = t.substring(1).toIntOrNull() ?: return -1
            if (x < 0 || row !in 1..size) return -1
            return (size - row) * size + x
        }
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

    private fun configFile(lv: Lv, timeMs: Long): File {
        dir.mkdirs()
        val threads = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
        val maxTime = (timeMs / 1000.0).coerceAtLeast(1.0)
        val text = buildString {
            appendLine("logToStderr = false")
            appendLine("logAllGTPCommunication = false")
            appendLine("logSearchInfo = false")
            appendLine("koRule = POSITIONAL")
            appendLine("scoringRule = AREA")
            appendLine("taxRule = NONE")
            appendLine("multiStoneSuicideLegal = false")
            appendLine("hasButton = false")
            appendLine("whiteHandicapBonus = 0")
            appendLine("friendlyPassOk = false")
            appendLine("allowResignation = false")
            appendLine("ponderingEnabled = false")
            appendLine("numSearchThreads = ${if (lv.visits in 1..64) 1 else threads}")
            appendLine("nnMaxBatchSize = ${if (lv.visits in 1..64) 1 else threads}")
            appendLine("nnCacheSizePowerOfTwo = 17")
            appendLine("nnMutexPoolSizePowerOfTwo = 12")
            appendLine("chosenMoveTemperatureEarly = ${lv.temperature}")
            appendLine("chosenMoveTemperature = ${lv.temperature}")
            // Siêu việt: theo thời gian; các cấp khác: theo số lượt, kèm trần an toàn chống treo bằng botMillis (mục 3.2)
            if (lv.visits > 0) appendLine("maxVisits = ${lv.visits}") else appendLine("maxVisits = 1000000")
            appendLine("maxTime = $maxTime")
        }
        val f = File(dir, "pm.cfg"); f.writeText(text)
        return f
    }

    private fun ensure(lv: Lv, timeMs: Long): Boolean {
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
        val lv = LEVELS[level.ordinal]
        if (!ensure(lv, timeMs)) return null
        val size = g.size
        if (cmd("clear_board", 10_000) == null) return null
        var color = "B"
        for (m in g.history) {
            if (cmd("play $color ${vertex(m, size)}", 10_000) == null) return null
            color = if (color == "B") "W" else "B"
        }
        val toMove = if (g.turn == 0) "B" else "W"
        if (lv.raw) return sampleFromPolicy(g, toMove, lv.temperature, rnd)
        val res = cmd("genmove $toMove", timeMs + 60_000) ?: return null
        val c = cellOf(res, size)
        if (c < 0 || !g.isLegal(c)) return null
        // bất biến cứng ở mọi cấp: không tự lấp mắt thật (mục 8.2)
        if (g.isBlunder(c, false)) return safest(g, rnd)
        return c
    }

    /** Dễ / Trung bình: một lần chạy mạng, lấy mẫu theo chính sách^(1/T) trên các nước hợp lệ đã lọc tự lấp mắt / tự atari. */
    private fun sampleFromPolicy(g: GoGame, toMove: String, temperature: Double, rnd: Random): Int? {
        val res = cmd("kata-raw-nn ${toMove.lowercase()} 0", 30_000) ?: return null
        val tok = res.split(Regex("\\s+"))
        val size = g.size
        val pi = tok.indexOf("policy")
        if (pi < 0 || pi + size * size >= tok.size) return null
        val pol = DoubleArray(size * size) { tok[pi + 1 + it].toDoubleOrNull()?.takeIf { v -> !v.isNaN() } ?: 0.0 }
        val passIdx = tok.indexOf("policyPass")
        val passP = if (passIdx >= 0) tok.getOrNull(passIdx + 1)?.toDoubleOrNull() ?: 0.0 else 0.0
        val cells = ArrayList<Int>(); val w = ArrayList<Double>()
        for (c in 0 until size * size) {
            val p = pol[c]
            if (p <= 1e-6 || g.isBlunder(c, true)) continue
            cells.add(c); w.add(exp(ln(p) / temperature))
        }
        // chỉ bỏ lượt khi mạng cho rằng bỏ lượt là nước chính (hết nước hữu ích)
        if (cells.isEmpty() || (passP > 0.5 && g.lastWasPass)) return GoGame.PASS
        val total = w.sum()
        var r = rnd.nextDouble() * total
        for (k in cells.indices) { r -= w[k]; if (r <= 0) return cells[k] }
        return cells.last()
    }

    /** Nước hợp lệ an toàn bất kỳ (dự phòng hiếm khi KataGo chọn nước tự lấp mắt). */
    private fun safest(g: GoGame, rnd: Random): Int {
        val ok = g.legalMoves().filter { it != GoGame.PASS && !g.isBlunder(it, true) }
        return if (ok.isEmpty()) GoGame.PASS else ok[rnd.nextInt(ok.size)]
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
