import com.fws.playmind.core.*
import java.io.*
import kotlin.random.Random

fun uciOf(m: Int): String {
    val f = ChessGame.fromSq(m); val t = ChessGame.toSq(m); val p = ChessGame.promoOf(m)
    return "" + ('a' + (f and 7)) + ('1' + (f shr 3)) + ('a' + (t and 7)) + ('1' + (t shr 3)) + (when (p) { 5 -> "q"; 4 -> "r"; 3 -> "b"; 2 -> "n"; else -> "" })
}
fun uciOfOld(m: Int): String {
    val f = ChessOld.fromSq(m); val t = ChessOld.toSq(m); val p = ChessOld.promoOf(m)
    return "" + ('a' + (f and 7)) + ('1' + (f shr 3)) + ('a' + (t and 7)) + ('1' + (t shr 3)) + (when (p) { 5 -> "q"; 4 -> "r"; 3 -> "b"; 2 -> "n"; else -> "" })
}
fun parseSq(s: String) = (s[1] - '1') * 8 + (s[0] - 'a')

class SF(elo: Int?) {
    val p = ProcessBuilder("/usr/games/stockfish").redirectErrorStream(true).start()
    val w = p.outputStream.bufferedWriter(); val r = p.inputStream.bufferedReader()
    init {
        send("uci"); wait("uciok")
        if (elo != null) { send("setoption name UCI_LimitStrength value true"); send("setoption name UCI_Elo value $elo") }
        send("setoption name Threads value 1"); send("isready"); wait("readyok")
    }
    fun send(s: String) { w.write(s + "\n"); w.flush() }
    fun wait(tok: String): String { while (true) { val l = r.readLine() ?: return ""; if (l.startsWith(tok)) return l } }
    fun best(moves: List<String>, ms: Int): String { send("position startpos moves " + moves.joinToString(" ")); send("go movetime $ms"); return wait("bestmove").split(" ")[1] }
    fun close() { send("quit"); p.destroy() }
}

// Chơi một ván: ChessGame mới (ourMs) vs đối thủ; ourWhite = mới cầm trắng
fun playVsSF(ourWhite: Boolean, ourMs: Long, sf: SF, sfMs: Int, rnd: Random): Int {
    val g = ChessGame(); val moves = ArrayList<String>()
    // 2 nước ngẫu nhiên để đa dạng
    repeat(2) { val l = g.legalMoves(); val m = l[rnd.nextInt(l.size)]; moves.add(uciOf(m)); g.play(m) }
    while (g.result == Outcome.ONGOING && g.plies < 240) {
        val ourTurn = (g.turn == 0) == ourWhite
        if (ourTurn) { val m = g.bestMove(ourMs); moves.add(uciOf(m)); g.play(m) }
        else {
            val s = sf.best(moves, sfMs); moves.add(s)
            val m = g.findMove(parseSq(s.substring(0, 2)), parseSq(s.substring(2, 4)), when (s.getOrNull(4)) { 'q' -> 5; 'r' -> 4; 'b' -> 3; 'n' -> 2; else -> 0 })
            if (m < 0 || !g.play(m)) error("illegal SF move $s")
        }
    }
    return when {
        g.result == Outcome.DRAW || g.result == Outcome.ONGOING -> 0
        (g.result == Outcome.WIN_FIRST) == ourWhite -> 1
        else -> -1
    }
}

// Mới vs cũ
fun playNewOld(newWhite: Boolean, ms: Long, rnd: Random): Int {
    val a = ChessGame(); val b = ChessOld()
    repeat(2) { val l = a.legalMoves(); val m = l[rnd.nextInt(l.size)]; a.play(m); b.play(m) }
    while (a.result == Outcome.ONGOING && a.plies < 240) {
        val newTurn = (a.turn == 0) == newWhite
        if (newTurn) { val m = a.bestMove(ms); a.play(m); b.play(m) }
        else { val m = b.bestMove(ms); val f = ChessOld.fromSq(m); val t = ChessOld.toSq(m); val mm = a.findMove(f, t, ChessOld.promoOf(m)); b.play(m); a.play(mm) }
    }
    return when {
        a.result == Outcome.DRAW || a.result == Outcome.ONGOING -> 0
        (a.result == Outcome.WIN_FIRST) == newWhite -> 1
        else -> -1
    }
}

fun main(args: Array<String>) {
    val mode = args[0]; val rnd = Random(args.getOrNull(3)?.toInt() ?: 7)
    if (mode == "ab") {
        val games = args[1].toInt(); val ms = args[2].toLong(); var w = 0; var d = 0; var l = 0
        for (i in 0 until games) { val r = playNewOld(i % 2 == 0, ms, rnd); if (r > 0) w++ else if (r == 0) d++ else l++; println("A/B game $i: ${if (r > 0) "MỚI thắng" else if (r == 0) "hòa" else "CŨ thắng"}  (mới ${w}W-${d}D-${l}L)") }
    } else {
        val games = args[1].toInt(); val ms = args[2].toLong(); val elo = args[4].toInt()
        val sf = SF(elo); var w = 0; var d = 0; var l = 0
        for (i in 0 until games) { val r = playVsSF(i % 2 == 0, ms, sf, 60, rnd); if (r > 0) w++ else if (r == 0) d++ else l++; println("vs SF@$elo game $i: ${if (r > 0) "thắng" else if (r == 0) "hòa" else "thua"}  (${w}W-${d}D-${l}L)") }
        sf.close()
    }
}
