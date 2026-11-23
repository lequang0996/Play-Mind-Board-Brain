import com.fws.playmind.core.*
import kotlin.random.Random

class PF(val nodes: Int) {
    val p = ProcessBuilder("/tmp/pf/Pikafish-Linux-x86-64-universal").directory(java.io.File("/tmp/pf")).redirectErrorStream(true).start()
    val w = p.outputStream.bufferedWriter(); val r = p.inputStream.bufferedReader()
    init { send("uci"); wait("uciok"); send("setoption name Hash value 16"); send("isready"); wait("readyok") }
    fun send(s: String) { w.write(s + "\n"); w.flush() }
    fun wait(t: String): String { while (true) { val l = r.readLine() ?: return ""; if (l.startsWith(t)) return l } }
    fun best(moves: List<String>): String { send("position startpos moves " + moves.joinToString(" ")); send("go nodes $nodes"); return wait("bestmove").split(" ")[1] }
    fun close() { send("quit"); p.destroy() }
}
fun nm(sq: Int) = "" + ('a' + sq % 9) + (sq / 9)
fun mv(m: Int) = nm(XiangqiGame.fromSq(m)) + nm(XiangqiGame.toSq(m))
fun parse(s: String) = ((s[1] - '0') * 9 + (s[0] - 'a')) to ((s[3] - '0') * 9 + (s[2] - 'a'))

fun main(args: Array<String>) {
    val games = args[0].toInt(); val ms = args[1].toLong(); val nodes = args[2].toInt(); val rnd = Random(args[3].toInt())
    val pf = PF(nodes); var w = 0; var d = 0; var l = 0
    for (i in 0 until games) {
        val ourRed = i % 2 == 0
        val g = XiangqiGame(); val moves = ArrayList<String>()
        repeat(2) { val lm = g.legalMoves(); val m = lm[rnd.nextInt(lm.size)]; moves.add(mv(m)); g.play(m) }
        while (g.result == Outcome.ONGOING && g.plies < 300) {
            val ours = (g.turn == 0) == ourRed
            if (ours) { val m = g.bestMove(ms); moves.add(mv(m)); g.play(m) }
            else {
                val s = pf.best(moves)
                if (s == "(none)" || s.length < 4) break
                val (a, b) = parse(s); val m = g.findMove(a, b)
                if (m < 0) { println("PF illegal in our rules: $s"); break }
                moves.add(s); g.play(m)
            }
        }
        val r = if (g.result == Outcome.DRAW || g.result == Outcome.ONGOING) 0 else if ((g.result == Outcome.WIN_FIRST) == ourRed) 1 else -1
        if (r > 0) w++ else if (r == 0) d++ else l++
        println("vs Pikafish@${nodes}n game $i: ${if (r > 0) "WIN" else if (r == 0) "draw" else "LOSS"} plies=${g.plies} ${g.reason} (${w}W-${d}D-${l}L)")
    }
    pf.close()
}
