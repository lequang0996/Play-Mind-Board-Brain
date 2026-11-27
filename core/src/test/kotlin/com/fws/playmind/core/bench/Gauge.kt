package com.fws.playmind.core.bench

import com.fws.playmind.core.*
import java.io.BufferedReader
import java.io.BufferedWriter
import kotlin.random.Random

/**
 * Đo bot cờ vua của app với Stockfish giới hạn sức (UCI_LimitStrength + UCI_Elo), BOT_GUIDE mục 3.5.
 * Stockfish (GPL-3.0) chỉ là engine đo, chạy trong CI, không phát hành cùng app.
 *
 *   bench gauge chess <stockfish> <elo> <games> <ourMs> <sfMs> <seed> [threads]   (xem BenchMain.kt)
 * [threads] = số luồng tìm kiếm của Siêu việt (Lazy SMP, như trên điện thoại); mặc định 1.
 * In mỗi ván một dòng "G <i> <màu của bot> <kết quả 1/0.5/0> <số nửa nước> <lý do>" rồi dòng "TOTAL <W> <D> <L>".
 */
internal val OPENINGS = listOf(
    "e2e4 e7e5 g1f3 b8c6 f1b5 a7a6", "e2e4 e7e5 g1f3 b8c6 f1c4 f8c5", "e2e4 e7e5 g1f3 g8f6 f3e5 d7d6",
    "e2e4 c7c5 g1f3 d7d6 d2d4 c5d4", "e2e4 c7c5 g1f3 b8c6 d2d4 c5d4", "e2e4 c7c5 b1c3 b8c6 g2g3 g7g6",
    "e2e4 e7e6 d2d4 d7d5 b1c3 g8f6", "e2e4 e7e6 d2d4 d7d5 e4e5 c7c5", "e2e4 c7c6 d2d4 d7d5 b1c3 d5e4",
    "e2e4 c7c6 d2d4 d7d5 e4e5 c8f5", "e2e4 d7d5 e4d5 d8d5 b1c3 d5a5", "e2e4 g8f6 e4e5 f6d5 d2d4 d7d6",
    "e2e4 d7d6 d2d4 g8f6 b1c3 g7g6", "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6", "d2d4 d7d5 c2c4 c7c6 g1f3 g8f6",
    "d2d4 d7d5 c2c4 d5c4 g1f3 g8f6", "d2d4 g8f6 c2c4 g7g6 b1c3 f8g7", "d2d4 g8f6 c2c4 e7e6 b1c3 f8b4",
    "d2d4 g8f6 c2c4 e7e6 g1f3 b7b6", "d2d4 g8f6 c2c4 c7c5 d4d5 e7e6", "d2d4 f7f5 g2g3 g8f6 f1g2 g7g6",
    "d2d4 d7d5 g1f3 g8f6 c1f4 e7e6", "c2c4 e7e5 b1c3 g8f6 g1f3 b8c6", "c2c4 c7c5 g1f3 g8f6 b1c3 b8c6",
    "c2c4 g8f6 b1c3 e7e6 e2e4 d7d5", "g1f3 d7d5 g2g3 g8f6 f1g2 e7e6", "g1f3 g8f6 c2c4 g7g6 b1c3 f8g7",
    "e2e4 e7e5 b1c3 g8f6 f2f4 d7d5", "e2e4 e7e5 f2f4 e5f4 g1f3 g7g5", "d2d4 g8f6 g1f3 e7e6 c1g5 h7h6",
    "e2e4 c7c5 c2c3 g8f6 e4e5 f6d5", "b2b3 e7e5 c1b2 b8c6 e2e3 g8f6",
)

class Stockfish(path: String, elo: Int) {
    private val p = ProcessBuilder(path).redirectErrorStream(true).start()
    private val w: BufferedWriter = p.outputStream.bufferedWriter()
    private val r: BufferedReader = p.inputStream.bufferedReader()
    init {
        send("uci"); wait("uciok")
        send("setoption name Threads value 1"); send("setoption name Hash value 64")
        send("setoption name UCI_LimitStrength value true"); send("setoption name UCI_Elo value $elo")
        send("isready"); wait("readyok")
    }
    fun send(s: String) { w.write(s); w.newLine(); w.flush() }
    fun wait(tok: String): String { while (true) { val l = r.readLine() ?: error("stockfish exited"); if (l.startsWith(tok)) return l } }
    fun newGame() { send("ucinewgame"); send("isready"); wait("readyok") }
    fun best(moves: List<String>, ms: Int): String {
        send("position startpos" + if (moves.isEmpty()) "" else " moves " + moves.joinToString(" "))
        send("go movetime $ms"); return wait("bestmove").split(" ")[1]
    }
    fun close() { runCatching { send("quit") }; p.destroy() }
}

fun uciOf(m: Int): String {
    val f = ChessGame.fromSq(m); val t = ChessGame.toSq(m)
    return "" + ('a' + (f and 7)) + ('1' + (f shr 3)) + ('a' + (t and 7)) + ('1' + (t shr 3)) +
        when (ChessGame.promoOf(m)) { 5 -> "q"; 4 -> "r"; 3 -> "b"; 2 -> "n"; else -> "" }
}

fun applyUci(g: ChessGame, s: String): Boolean {
    fun sq(a: Char, b: Char) = (b - '1') * 8 + (a - 'a')
    val pr = when (s.getOrNull(4)) { 'q' -> 5; 'r' -> 4; 'b' -> 3; 'n' -> 2; else -> 0 }
    val m = g.findMove(sq(s[0], s[1]), sq(s[2], s[3]), pr)
    return m >= 0 && g.play(m)
}

fun gaugeMain(args: List<String>) {
    require(args.size >= 7 && args[0] == "chess") { "args: chess <stockfish> <elo> <games> <ourMs> <sfMs> <seed>" }
    val sfPath = args[1]; val elo = args[2].toInt(); val games = args[3].toInt()
    val ourMs = args[4].toLong(); val sfMs = args[5].toInt(); val seed = args[6].toInt()
    ChessGame.threads = args.getOrNull(7)?.toInt() ?: 1
    val sf = Stockfish(sfPath, elo)
    var w = 0; var d = 0; var l = 0
    for (i in 0 until games) {
        val opening = OPENINGS[((seed + i / 2) % OPENINGS.size + OPENINGS.size) % OPENINGS.size].split(" ")
        val ourWhite = i % 2 == 0
        val g = ChessGame(); val moves = ArrayList<String>()
        for (mv in opening) { check(applyUci(g, mv)) { "bad opening move $mv" }; moves.add(mv) }
        sf.newGame()
        val rnd = Random(seed * 1000 + i)
        while (g.result == Outcome.ONGOING && g.plies < 400) {
            val ourTurn = (g.turn == 0) == ourWhite
            if (ourTurn) {
                val m = (g as LeveledEngine).levelMove(BotLevel.SUPREME, ourMs, rnd)
                check(m >= 0) { "engine returned no move" }
                moves.add(uciOf(m)); check(g.play(m))
            } else {
                val s = sf.best(moves, sfMs)
                check(applyUci(g, s)) { "illegal stockfish move $s" }
                moves.add(s)
            }
        }
        val score = when {
            // Luật lặp thế = thua là luật riêng của app; Stockfish không biết luật này nên khi đo tính là hòa như luật FIDE.
            g.result == Outcome.DRAW || g.result == Outcome.ONGOING || g.reason == EndReason.REPETITION -> 0.5
            (g.result == Outcome.WIN_FIRST) == ourWhite -> 1.0
            else -> 0.0
        }
        when (score) { 1.0 -> w++; 0.5 -> d++; else -> l++ }
        println("G $i ${if (ourWhite) "w" else "b"} $score ${g.plies} ${g.reason}")
        System.out.flush()
    }
    sf.close()
    println("TOTAL $w $d $l")
}
