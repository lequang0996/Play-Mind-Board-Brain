package bench

import com.fws.playmind.core.*
import kotlin.random.Random

/**
 * Đấu hai bot của cùng một game theo cặp ván đổi màu (BOT_GUIDE mục 2.5, 3.4).
 *
 *   java -cp <classpath> bench.LevelMatchKt <game> <A> <B> <pairs> <svMs> <seed0> [players]
 *
 *   <game>  chess | xiangqi | caro | quoridor | wallgo
 *   <A>,<B> số cấp 0..6 (0 = Dễ ... 6 = Siêu việt), hoặc "old" = bot cũ (bestMove theo thời gian, chỉ cho A/B)
 *   <pairs> số cặp ván (mỗi cặp: cùng thế mở đầu, đổi màu)
 *   <svMs>  ngân sách thời gian của Siêu việt (và của bot "old")
 *   <seed0> seed của cặp đầu; cặp i dùng seed0 + i cho thế mở đầu và nhiễu (mục 3.1)
 *
 * In một dòng mỗi ván: "R <i> <A đi trước?> <kết quả cho A: 1 / 0.5 / 0> <số nước> <lý do>", cuối cùng in tổng W D L.
 */
fun main(args: Array<String>) {
    val game = args[0]; val a = args[1]; val b = args[2]
    val pairs = args[3].toInt(); val ms = args[4].toLong(); val seed0 = args[5].toInt()
    val players = args.getOrNull(6)?.toInt() ?: 2
    var w = 0; var d = 0; var l = 0
    for (i in 0 until pairs) {
        val seed = seed0 + i
        for (aFirst in listOf(true, false)) {
            val g = opening(game, players, seed)
            val rnd = Random(seed * 2 + (if (aFirst) 0 else 1))
            var guard = 0
            while (g.result == Outcome.ONGOING && guard++ < 1200) {
                val aTurn = (g.turn == 0) == aFirst
                val who = if (aTurn) a else b
                val mv = move(g, who, ms, rnd)
                if (mv < 0 || !g.play(mv)) { g.forfeit(g.turn, EndReason.RESIGN); break }
            }
            val sA = when (g.result) {
                Outcome.WIN_FIRST -> if (aFirst) 1.0 else 0.0
                Outcome.WIN_SECOND -> if (aFirst) 0.0 else 1.0
                else -> 0.5
            }
            when (sA) { 1.0 -> w++; 0.0 -> l++; else -> d++ }
            println("R $i ${if (aFirst) 1 else 0} $sA ${g.plies} ${g.reason}")
            System.out.flush()
        }
    }
    println("TOTAL $w $d $l")
}

private fun create(game: String, players: Int): BoardGame = when (game) {
    "chess" -> ChessGame(); "xiangqi" -> XiangqiGame(); "caro" -> GomokuGame()
    "quoridor" -> QuoridorGame(players); "wallgo" -> WallGoGame(players)
    "quoridor_old" -> QuoridorOld(players); "wallgo_old" -> WallGoOld(players)
    else -> error("game?")
}

/** Thế mở đầu có seed: vài nước ngẫu nhiên hợp lệ (game không có sách khai cuộc, mục 3.1). */
private fun opening(game: String, players: Int, seed: Int): BoardGame {
    val g = create(game, players)
    val r = Random(seed * 7919 + 13)
    val k = when (game) { "chess", "xiangqi" -> 2 + r.nextInt(3); "caro" -> 0; "quoridor" -> 2; else -> 2 }
    repeat(k) {
        val ms = g.legalMoves().let { all -> if (game == "quoridor") all.filter { it < 128 }.toIntArray() else all }
        if (ms.isNotEmpty() && g.result == Outcome.ONGOING) g.play(ms[r.nextInt(ms.size)])
    }
    if (game == "caro") {   // 3 quân đầu ngẫu nhiên trong ô 5x5 quanh gốc
        while (g.plies < 3) { val m = GomokuGame.pack(r.nextInt(5) - 2, r.nextInt(5) - 2); g.play(m) }
    }
    return g
}

private fun move(g: BoardGame, who: String, ms: Long, rnd: Random): Int {
    if (who == "old") {
        // bot cũ chơi trên bản sao kiểu cũ của cùng thế cờ
        val old = create(g.id + "_old", (g as? QuoridorGame)?.players ?: (g as? WallGoGame)?.players ?: 2)
        for (m in g.history) old.play(m)
        return old.bestMove(ms)
    }
    val lv = BotLevel.of(who.toInt())
    return (g as LeveledEngine).levelMove(lv, ms, rnd)
}
