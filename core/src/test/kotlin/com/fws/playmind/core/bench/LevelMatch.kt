package com.fws.playmind.core.bench

import com.fws.playmind.core.*
import kotlin.random.Random

/**
 * Đấu hai cấp bot của cùng một game theo cặp ván đổi màu (BOT_GUIDE mục 2.5, 3.4).
 *   ladder <game> <A> <B> <pairs> <svMs> <seed0> [players]
 *   <game> chess | xiangqi | caro | quoridor | wallgo | go (KataGo, cần KATAGO/KG_B6/KG_B10);  <A>,<B> cấp 0..6 (0 = Dễ ... 6 = Siêu việt)
 *   <svMs> ngân sách thời gian của Siêu việt trên máy đo (= botMillis ÷ k, mục 3.2)
 * In mỗi ván: "R <i> <A đi trước 1/0> <điểm của A> <số nửa nước> <lý do>", cuối cùng "TOTAL W D L".
 */
fun ladderMain(args: List<String>) {
    val game = args[0]; val a = args[1].toInt(); val b = args[2].toInt()
    val pairs = args[3].toInt(); val ms = args[4].toLong(); val seed0 = args[5].toInt()
    val players = args.getOrNull(6)?.toInt() ?: 2
    if (game == "go") { goLadder(a, b, pairs, ms, seed0); return }   // KataGo (GoKata.kt), chỉ trên CI
    if (game == "hexagon") { hexLadder(a, b, pairs, seed0); return }
    if (game == "meadow" || game == "hilo") { multiLadder(game, a, b, pairs, ms, seed0, players); return }
    ChessGame.threads = 1   // đo cấp: 1 luồng (nhiều tiến trình chạy song song trên một máy)
    var w = 0; var d = 0; var l = 0
    for (i in 0 until pairs) {
        val seed = seed0 + i
        for (aFirst in listOf(true, false)) {
            val g = benchOpening(game, players, seed)
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
    "quoridor" -> QuoridorGame(players); "wallgo" -> WallGoGame(players); "morris" -> MorrisGame()
    else -> error("game?")
}

/** Thế mở đầu có seed: vài nước ngẫu nhiên hợp lệ (mục 3.1). */
internal fun benchOpening(game: String, players: Int, seed: Int): BoardGame {
    val g = create(game, players)
    val r = Random(seed * 7919 + 13)
    // Quoridor: 4 nước đi quân rồi mỗi bên một tường ngẫu nhiên (chỉ đi quân thì có khoảng 9 thế mở đầu, bot giới hạn nút
    // chơi lại y hệt nên các ván trùng nhau và sai số thật rất lớn; BOT_GUIDE mục 3.1)
    val k = when (game) { "chess", "xiangqi" -> 2 + r.nextInt(3); "caro" -> 0; "quoridor" -> 6; "morris" -> 2 + r.nextInt(3); else -> 2 }
    repeat(k) { ply ->
        val ms = g.legalMoves().let { all -> if (game == "quoridor") all.filter { if (ply < 4) it < 128 else it >= 128 }.toIntArray() else all }
        // chỉ nhận nước không kết thúc ván (ví dụ đi quân về chỗ cũ tạo lại thế ban đầu = thua do lặp thế)
        val ok = ms.filter { m -> g.copyGame().let { c -> c.play(m) && c.result == Outcome.ONGOING } }
        if (ok.isNotEmpty() && g.result == Outcome.ONGOING) g.play(ok[r.nextInt(ok.size)])
    }
    if (game == "caro") while (g.plies < 3) g.play(GomokuGame.pack(r.nextInt(5) - 2, r.nextInt(5) - 2))
    return g
}

/**
 * Lãnh địa đồng cỏ nhiều người: [players] ghế, nửa ghế cấp A, nửa ghế cấp B xen kẽ (A ở ghế chẵn rồi đổi sang ghế lẻ).
 * Điểm của A trong một ván: 1 nếu ghế A tốt nhất hơn ghế B tốt nhất (điểm hình vuông, rồi diện tích), 0 nếu kém, 0,5 nếu bằng.
 * Hai cấp ngang nhau cho 50%, nên dùng chung ngưỡng với cặp hai bên (mục 3.4). Dòng "R" giống ladder thường.
 */
private fun multiLadder(game: String, a: Int, b: Int, pairs: Int, ms: Long, seed0: Int, players: Int) {
    var w = 0; var d = 0; var l = 0
    for (i in 0 until pairs) {
        val seed = seed0 + i
        for (aFirst in listOf(true, false)) {
            val g: BoardGame = if (game == "hilo") HiLoGame(players) else MeadowGame(players)
            val rnd = Random(seed * 2 + (if (aFirst) 0 else 1))
            fun isA(p: Int) = (p % 2 == 0) == aFirst
            var guard = 0
            while (g.result == Outcome.ONGOING && guard++ < 5000) {
                val mv = (g as LeveledEngine).levelMove(BotLevel.of(if (isA(g.turn)) a else b), ms, rnd)
                if (mv < 0 || !g.play(mv)) { g.play((g as AutoOnTimeout).timeoutMove()) || break }
            }
            // Công thức cao thấp: người còn chip cuối cùng thắng; Lãnh địa: điểm hình vuông rồi diện tích
            fun key(p: Int): Long = if (g is HiLoGame) (if (g.winner == p) 1L else 0L) * 1000 + g.chips[p]
                else (g as MeadowGame).finalPoints[p] * 10000L + g.finalArea[p]
            val bestA = (0 until players).filter { isA(it) }.maxOf { key(it) }
            val bestB = (0 until players).filter { !isA(it) }.maxOf { key(it) }
            val sA = if (bestA > bestB) 1.0 else if (bestA < bestB) 0.0 else 0.5
            when (sA) { 1.0 -> w++; 0.0 -> l++; else -> d++ }
            val info = if (g is HiLoGame) "hands=${g.hand} w=${g.winner}" else "pts=${(g as MeadowGame).finalPoints.joinToString(",")}"
            println("R $seed ${if (aFirst) 1 else 0} $sA ${g.plies} ${g.reason} $info")
            System.out.flush()
        }
    }
    println("TOTAL $w $d $l")
}

/** Lục giác trí nhớ: hai bot đấu nguyên trận (10 vòng + vòng phụ), A ở ghế 0 rồi ghế 1. */
private fun hexLadder(a: Int, b: Int, pairs: Int, seed0: Int) {
    var w = 0; var d = 0; var l = 0
    for (i in 0 until pairs) {
        val seed = seed0 + i
        for (aFirst in listOf(true, false)) {
            val m = HexagonMatch(if (aFirst) intArrayOf(a, b) else intArrayOf(b, a), seed * 2L + (if (aFirst) 0 else 1), botPickMs = 300)
            m.simulate()
            val aSeat = if (aFirst) 0 else 1
            val sA = if (m.draw) 0.5 else if (m.winner == aSeat) 1.0 else 0.0
            when (sA) { 1.0 -> w++; 0.0 -> l++; else -> d++ }
            println("R $seed ${if (aFirst) 1 else 0} $sA ${m.round} ${if (m.draw) "DRAW" else "SCORE"} ${m.scores.joinToString(":")}")
            System.out.flush()
        }
    }
    println("TOTAL $w $d $l")
}
