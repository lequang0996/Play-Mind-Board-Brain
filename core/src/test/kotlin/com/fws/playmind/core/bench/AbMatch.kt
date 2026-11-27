package com.fws.playmind.core.bench

import com.fws.playmind.core.*
import kotlin.random.Random

/**
 * So hai biến thể engine Siêu việt của cùng một game (thử thay đổi, BOT_GUIDE mục 2.6), theo cặp ván đổi màu:
 *   ab <game> <A> <B> <games> <ms> <seed> [nodes]
 *   game: chess | xiangqi.  A, B: new (bản hiện tại, 1 luồng) | smpN (N luồng) | ref (trọng số khởi điểm, chưa tinh chỉnh)
 *         | old (engine cũ, chỉ cờ tướng).  nodes > 0: giới hạn nút thay cho thời gian.
 *         Cờ vua thêm "@tham số": ví dụ new@rfpm70+asp14 hoặc smp3@futb100 (tên tham số xem SP trong ChessEngine.kt).
 * In mỗi ván "R <i> <A đi trước 1/0> <điểm của A> <số nửa nước> <lý do>", cuối cùng "TOTAL W D L".
 */
private interface AbPlayer { fun move(hist: List<Int>, ms: Long, nodes: Long): Int; fun newGame() }

private class ChessP(val threads: Int, ref: Boolean, spec: String) : AbPlayer {
    val e = ChessEngine(20)
    init { if (ref) { val w = defaultWeights(); e.wMg = w.first; e.wEg = w.second }; e.prm = SP.parse(spec) }
    override fun move(hist: List<Int>, ms: Long, nodes: Long) =
        e.searchSmp(ChessEngine.START_FEN, hist, SearchLimit(timeMs = if (nodes > 0) 0 else ms, nodes = nodes), null, 0, threads).best
    override fun newGame() = e.clear()
    companion object {
        fun defaultWeights(): Pair<IntArray, IntArray> = synchronized(CW) {
            val mg = CW.MG.copyOf(); val eg = CW.EG.copyOf(); CW.defaults()
            val r = CW.MG.copyOf() to CW.EG.copyOf(); mg.copyInto(CW.MG); eg.copyInto(CW.EG); r
        }
    }
}

private class XqP(val threads: Int, ref: Boolean) : AbPlayer {
    val e = XiangqiEngine(20)
    private val start = IntArray(90).also { val g = XiangqiGame(); for (i in 0 until 90) it[i] = g.pieceAt(i) }
    init {
        if (ref) synchronized(XW) {
            val mg = XW.MG.copyOf(); val eg = XW.EG.copyOf(); XW.defaults()
            e.wMg = XW.MG.copyOf(); e.wEg = XW.EG.copyOf(); mg.copyInto(XW.MG); eg.copyInto(XW.EG)
        }
    }
    override fun move(hist: List<Int>, ms: Long, nodes: Long) =
        e.searchSmp(start, true, hist, SearchLimit(timeMs = if (nodes > 0) 0 else ms, nodes = nodes), null, 0, threads).best
    override fun newGame() = e.clear()
}

private class XqOldP : AbPlayer {
    override fun move(hist: List<Int>, ms: Long, nodes: Long): Int { val g = XiangqiOld(); for (m in hist) g.play(m); return g.bestMove(ms) }
    override fun newGame() {}
}

fun abMain(args: List<String>) {
    val game = args[0]
    fun mk(full: String): AbPlayer {
        val s = full.substringBefore('@'); val spec = if ('@' in full) full.substringAfter('@') else ""
        val th = if (s.startsWith("smp")) s.removePrefix("smp").toInt() else 1
        return when (game) {
            "chess" -> ChessP(th, s == "ref", spec)
            "xiangqi" -> if (s == "old") XqOldP() else XqP(th, s == "ref")
            else -> error("ab: game $game chưa hỗ trợ")
        }
    }
    val a = mk(args[1]); val b = mk(args[2])
    val games = args[3].toInt(); val ms = args[4].toLong(); val seed = args[5].toInt()
    val nodes = args.getOrNull(6)?.toLong() ?: 0L
    var w = 0; var d = 0; var l = 0
    for (i in 0 until games) {
        val gi = seed + i
        val aFirst = gi % 2 == 0
        val g: BoardGame = if (game == "chess") ChessGame().also { cg ->
            for (mv in OPENINGS[(gi / 2) % OPENINGS.size].split(" ")) check(applyUci(cg, mv))
        } else XiangqiGame().also { xg ->
            val r = Random((gi / 2) * 7919L + 17)
            repeat(3 + r.nextInt(4)) { val ms0 = xg.legalMoves(); if (ms0.isNotEmpty()) xg.play(ms0[r.nextInt(ms0.size)]) }
        }
        a.newGame(); b.newGame()
        while (g.result == Outcome.ONGOING && g.plies < 400) {
            val aTurn = (g.turn == 0) == aFirst
            val m = (if (aTurn) a else b).move(g.history, ms, nodes)
            if (m < 0 || !g.play(m)) { g.forfeit(g.turn, EndReason.RESIGN); break }
        }
        val sa = when (g.result) {
            Outcome.WIN_FIRST -> if (aFirst) 1.0 else 0.0
            Outcome.WIN_SECOND -> if (aFirst) 0.0 else 1.0
            else -> 0.5
        }
        when (sa) { 1.0 -> w++; 0.0 -> l++; else -> d++ }
        println("R $gi ${if (aFirst) 1 else 0} $sa ${g.plies} ${g.reason}"); System.out.flush()
    }
    println("TOTAL $w $d $l")
}
