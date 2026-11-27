package com.fws.playmind.core.bench

import com.fws.playmind.core.*
import kotlin.random.Random

/**
 * Đấu hai cấp bot của cùng một game theo cặp ván đổi màu (BOT_GUIDE mục 2.5, 3.4).
 *   ladder <game> <A> <B> <pairs> <svMs> <seed0> [players]
 *   <game> chess | xiangqi | caro | quoridor | wallgo;  <A>,<B> cấp 0..6 (0 = Dễ ... 6 = Siêu việt)
 *   <svMs> ngân sách thời gian của Siêu việt trên máy đo (= botMillis ÷ k, mục 3.2)
 * In mỗi ván: "R <i> <A đi trước 1/0> <điểm của A> <số nửa nước> <lý do>", cuối cùng "TOTAL W D L".
 */
fun ladderMain(args: List<String>) {
    val game = args[0]; val a = args[1].toInt(); val b = args[2].toInt()
    val pairs = args[3].toInt(); val ms = args[4].toLong(); val seed0 = args[5].toInt()
    val players = args.getOrNull(6)?.toInt() ?: 2
    ChessGame.threads = 1   // đo cấp: 1 luồng (nhiều tiến trình chạy song song trên một máy)
    var w = 0; var d = 0; var l = 0
    for (i in 0 until pairs) {
        val seed = seed0 + i
        for (aFirst in listOf(true, false)) {
            val g = opening(game, players, seed)
            val rnd = Random(seed * 2 + (if (aFirst) 0 else 1))
            var guard = 0
            while (g.result == Outcome.ONGOING && guard++ < 1200) {
                val aTurn = (g.turn == 0) == aFirst
                val mv = (g as LeveledEngine).levelMove(BotLevel.of(if (aTurn) a else b), ms, rnd)
                if (mv < 0 || !g.play(mv)) { g.forfeit(g.turn, EndReason.RESIGN); break }
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
    println("TOTAL $w $d $l")
}

private fun create(game: String, players: Int): BoardGame = when (game) {
    "chess" -> ChessGame(); "xiangqi" -> XiangqiGame(); "caro" -> GomokuGame()
    "quoridor" -> QuoridorGame(players); "wallgo" -> WallGoGame(players)
    else -> error("game?")
}

/** Thế mở đầu có seed: vài nước ngẫu nhiên hợp lệ (mục 3.1). */
private fun opening(game: String, players: Int, seed: Int): BoardGame {
    val g = create(game, players)
    val r = Random(seed * 7919 + 13)
    val k = when (game) { "chess", "xiangqi" -> 2 + r.nextInt(3); "caro" -> 0; else -> 2 }
    repeat(k) {
        val ms = g.legalMoves().let { all -> if (game == "quoridor") all.filter { it < 128 }.toIntArray() else all }
        if (ms.isNotEmpty() && g.result == Outcome.ONGOING) g.play(ms[r.nextInt(ms.size)])
    }
    if (game == "caro") while (g.plies < 3) g.play(GomokuGame.pack(r.nextInt(5) - 2, r.nextInt(5) - 2))
    return g
}
