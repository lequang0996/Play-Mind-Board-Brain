package com.fws.playmind.core

import kotlin.math.exp
import kotlin.math.ln
import kotlin.random.Random

/**
 * BẢNG 7 CẤP của bot cờ vây chạy KataGo (BOT_GUIDE mục 8.3a), dùng chung cho app (match/KataGo.kt, tiến trình trên điện thoại)
 * và bộ đo (bench GoKata, tiến trình trên CI) để hai nơi luôn cùng một cấu hình.
 *
 *  - Dễ / Trung bình ([Lv.raw]): một lần chạy mạng nhỏ rồi lấy mẫu theo chính sách^(1/T), lọc nước tự lấp mắt / tự atari.
 *  - Từ Khá trở lên: tìm kiếm MCTS của KataGo với số lượt cố định.
 *  - Siêu việt: visits = 0 nghĩa là theo thời gian (botMillis). Khi đo dùng số lượt cố định (ghi đè pm.kata.6=...).
 *
 * Ghi đè khi đo: -Dpm.kata.<cấp>=b6,v12,t0.5[,raw] (trường không ghi giữ giá trị gốc).
 */
object KataLevels {
    data class Lv(val model: String, val visits: Int, val temperature: Double, val raw: Boolean)

    val base = listOf(
        Lv("b6", 1, 1.6, true),
        Lv("b6", 1, 0.8, true),
        Lv("b6", 12, 0.5, false),
        Lv("b10", 40, 0.2, false),
        Lv("b10", 160, 0.0, false),
        Lv("b10", 480, 0.0, false),
        Lv("b10", 0, 0.0, false),
    )

    fun of(level: Int): Lv {
        val b = base[level]
        val spec = try { System.getProperty("pm.kata.$level") } catch (_: Exception) { null } ?: return b
        var lv = b.copy(raw = false)
        var raw = false
        for (f in spec.split(',').map { it.trim() }.filter { it.isNotEmpty() }) when {
            f == "raw" -> raw = true
            f.startsWith("b") -> lv = lv.copy(model = f)
            f.startsWith("v") -> lv = lv.copy(visits = f.substring(1).toInt())
            f.startsWith("t") -> lv = lv.copy(temperature = f.substring(1).toDouble())
        }
        return lv.copy(raw = raw)
    }

    const val KOMI = 7.5
    private const val COLS = "ABCDEFGHJKLMNOPQRST"

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

    /**
     * Nội dung tệp cấu hình GTP của KataGo cho một cấp; luật khớp GoGame (diện tích, siêu ko vị trí, cấm tự sát, komi 7,5).
     * Đã chạy thử trên CI với KataGo 1.18.2 (bot-bench "ladder go"): thiếu khóa bắt buộc là tiến trình thoát ngay.
     */
    fun configText(lv: Lv, threads: Int, maxTimeSec: Double): String = buildString {
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
        // KataGo 1.18 bắt buộc có các khóa này khi tệp cấu hình có allowResignation (thiếu thì tiến trình thoát ngay)
        appendLine("resignThreshold = -0.99")
        appendLine("resignConsecTurns = 3")
        appendLine("ponderingEnabled = false")
        val t = if (lv.visits in 1..64) 1 else threads
        appendLine("numSearchThreads = $t")
        appendLine("nnMaxBatchSize = $t")
        appendLine("nnCacheSizePowerOfTwo = 17")
        appendLine("nnMutexPoolSizePowerOfTwo = 12")
        appendLine("chosenMoveTemperatureEarly = ${lv.temperature}")
        appendLine("chosenMoveTemperature = ${lv.temperature}")
        if (lv.visits > 0) appendLine("maxVisits = ${lv.visits}") else appendLine("maxVisits = 1000000")
        appendLine("maxTime = $maxTimeSec")
    }

    /**
     * Dễ / Trung bình: chọn nước từ kết quả lệnh "kata-raw-nn" (chính sách mạng), lấy mẫu theo chính sách^(1/T) trên các nước
     * hợp lệ đã lọc tự lấp mắt / tự atari. Trả null nếu không đọc được kết quả.
     */
    fun pickFromPolicy(g: GoGame, rawNn: String, temperature: Double, rnd: Random): Int? {
        val tok = rawNn.split(Regex("\\s+"))
        val size = g.size
        val pi = tok.indexOf("policy")
        if (pi < 0 || pi + size * size >= tok.size) return null
        val pol = DoubleArray(size * size) { tok[pi + 1 + it].toDoubleOrNull()?.takeIf { v -> !v.isNaN() } ?: 0.0 }
        val passIdx = tok.indexOf("policyPass")
        val passP = if (passIdx >= 0) tok.getOrNull(passIdx + 1)?.toDoubleOrNull() ?: 0.0 else 0.0
        val cells = ArrayList<Int>(); val w = ArrayList<Double>()
        for (c in 0 until size * size) {
            val p = pol[c]
            if (p <= 1e-6 || !g.isLegal(c) || g.isBlunder(c, true)) continue
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
    fun safest(g: GoGame, rnd: Random): Int {
        val ok = g.legalMoves().filter { it != GoGame.PASS && !g.isBlunder(it, true) }
        return if (ok.isEmpty()) GoGame.PASS else ok[rnd.nextInt(ok.size)]
    }
}
