package com.fws.playmind.core.bench

import com.fws.playmind.core.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlin.random.Random

/**
 * Bot cờ vây KataGo cho bộ đo (chỉ chạy trên CI): cùng bảng cấp [KataLevels] và cùng cách chọn nước với app (match/KataGo.kt).
 * Cần biến môi trường KATAGO (tệp chạy Linux), KG_B6, KG_B10 (đường dẫn hai mạng g170). Mỗi cấp một tiến trình GTP 1 luồng.
 */
class GoKata(private val level: Int, private val svMs: Long) {
    val lv = KataLevels.of(level)
    private val proc: Process
    private val out: OutputStreamWriter
    private val inp: BufferedReader
    private val synced = ArrayList<Int>()
    var genmoves = 0; private set
    var genmoveNanos = 0L; private set
    private val errTail = ArrayDeque<String>()
    private fun errText() = synchronized(errTail) { errTail.joinToString(" | ") }

    init {
        val bin = System.getenv("KATAGO") ?: error("thiếu biến KATAGO")
        val model = System.getenv(if (lv.model == "b6") "KG_B6" else "KG_B10") ?: error("thiếu mạng ${lv.model}")
        val cfg = File.createTempFile("pm-kata-$level-", ".cfg")
        cfg.deleteOnExit()
        cfg.writeText(KataLevels.configText(lv, 1, (svMs / 1000.0).coerceAtLeast(1.0)).replace("logToStderr = false", "logToStderr = true"))
        proc = ProcessBuilder(bin, "gtp", "-model", model, "-config", cfg.absolutePath).start()
        Thread({ try { proc.errorStream.bufferedReader().forEachLine { synchronized(errTail) { errTail.addLast(it); if (errTail.size > 12) errTail.removeFirst() } } } catch (_: Exception) {} }, "kg-err").apply { isDaemon = true }.start()
        out = OutputStreamWriter(proc.outputStream); inp = BufferedReader(InputStreamReader(proc.inputStream))
        cmd("boardsize 19"); cmd("komi ${KataLevels.KOMI}")
    }

    private fun cmd(c: String): String {
        out.write(c); out.write("\n"); out.flush()
        val sb = StringBuilder(); var first: String? = null
        while (true) {
            val line = inp.readLine() ?: run { Thread.sleep(300); error("KataGo đã thoát khi chạy '$c': ${errText()}") }
            if (first == null) { if (line.isBlank()) continue; first = line } else if (line.isBlank()) break
            sb.append(line).append('\n')
        }
        val s = sb.toString().trim()
        check(s.startsWith("=")) { "KataGo lỗi '$c': $s / ${errText()}" }
        return s.substring(1).trim()
    }

    /** Đồng bộ lịch sử ván với tiến trình: chỉ gửi phần mới nếu phần đầu khớp, không thì làm lại từ đầu. */
    private fun sync(g: GoGame) {
        val h = g.history
        val prefixOk = synced.size <= h.size && synced.indices.all { synced[it] == h[it] }
        if (!prefixOk) { cmd("clear_board"); synced.clear() }
        for (i in synced.size until h.size) {
            cmd("play ${if (i % 2 == 0) "B" else "W"} ${KataLevels.vertex(h[i], g.size)}")
            synced.add(h[i])
        }
    }

    fun move(g: GoGame, rnd: Random): Int {
        sync(g)
        val toMove = if (g.turn == 0) "B" else "W"
        if (lv.raw) return KataLevels.pickFromPolicy(g, cmd("kata-raw-nn ${toMove.lowercase()} 0"), lv.temperature, rnd) ?: KataLevels.safest(g, rnd)
        val t0 = System.nanoTime()
        val res = cmd("genmove $toMove")
        genmoveNanos += System.nanoTime() - t0; genmoves++
        // genmove đã đặt quân trong tiến trình: ghi nhận để lần sau không gửi lại
        val c = KataLevels.cellOf(res, g.size)
        synced.add(c)
        if (c < 0 || !g.isLegal(c)) return KataLevels.safest(g, rnd)
        if (g.isBlunder(c, false)) return KataLevels.safest(g, rnd)
        return c
    }

    fun close() {
        try { out.write("quit\n"); out.flush() } catch (_: Exception) {}
        proc.destroy()
    }
}

/** Thế mở đầu cờ vây có seed: 2 nước ngẫu nhiên trong vùng giữa (tránh hai cấp tìm cố định chơi lại y hệt nhau). */
internal fun goOpening(seed: Int): GoGame {
    val g = GoGame(19)
    val r = Random(seed * 7919 + 13)
    while (g.plies < 2) {
        val c = (2 + r.nextInt(15)) * 19 + 2 + r.nextInt(15)
        if (g.isLegal(c)) g.play(c)
    }
    return g
}

/** ladder go: như ladderMain nhưng dùng KataGo ở cả hai bên. In thêm "# engine" thời gian trung bình mỗi genmove của mỗi cấp. */
fun goLadder(a: Int, b: Int, pairs: Int, ms: Long, seed0: Int) {
    val ka = GoKata(a, ms); val kb = GoKata(b, ms)
    println("# engine A=${ka.lv} B=${kb.lv}")
    var w = 0; var d = 0; var l = 0
    for (i in 0 until pairs) {
        val seed = seed0 + i
        for (aFirst in listOf(true, false)) {
            val g = goOpening(seed)
            val rnd = Random(seed * 2 + (if (aFirst) 0 else 1))
            var guard = 0
            while (g.result == Outcome.ONGOING && guard++ < 1200) {
                val aTurn = (g.turn == 0) == aFirst
                val mv = (if (aTurn) ka else kb).move(g, rnd)
                if (!g.play(mv)) { g.forfeit(g.turn, EndReason.RESIGN); break }
            }
            val sA = when (g.result) {
                Outcome.WIN_FIRST -> if (aFirst) 1.0 else 0.0
                Outcome.WIN_SECOND -> if (aFirst) 0.0 else 1.0
                else -> 0.5
            }
            when (sA) { 1.0 -> w++; 0.0 -> l++; else -> d++ }
            println("R $seed ${if (aFirst) 1 else 0} $sA ${g.plies} ${g.reason}")
            System.out.flush()
        }
    }
    fun avg(k: GoKata) = if (k.genmoves == 0) 0 else (k.genmoveNanos / k.genmoves / 1_000_000)
    println("# engine msPerGenmove A=${avg(ka)} B=${avg(kb)}")
    println("TOTAL $w $d $l")
    ka.close(); kb.close()
}
