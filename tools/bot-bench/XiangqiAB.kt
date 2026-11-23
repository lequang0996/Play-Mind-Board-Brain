import com.fws.playmind.core.*
import kotlin.random.Random
fun main(args: Array<String>) {
    val games = args[0].toInt(); val ms = args[1].toLong(); val rnd = Random(args[2].toInt())
    var w = 0; var d = 0; var l = 0
    for (i in 0 until games) {
        val newRed = i % 2 == 0
        val a = XiangqiGame(); val b = XiangqiOld()
        repeat(4) { val lm = a.legalMoves(); val m = lm[rnd.nextInt(lm.size)]; a.play(m); b.play(m) }
        while (a.result == Outcome.ONGOING && a.plies < 300) {
            val newTurn = (a.turn == 0) == newRed
            if (newTurn) { val m = a.bestMove(ms); a.play(m); b.play(m) } else { val m = b.bestMove(ms); a.play(m); b.play(m) }
        }
        val r = if (a.result == Outcome.DRAW || a.result == Outcome.ONGOING) 0 else if ((a.result == Outcome.WIN_FIRST) == newRed) 1 else -1
        if (r > 0) w++ else if (r == 0) d++ else l++
        println("xq A/B game $i: ${if (r > 0) "NEW" else if (r == 0) "draw" else "OLD"} plies=${a.plies} ${a.reason} (new ${w}W-${d}D-${l}L)")
    }
}
