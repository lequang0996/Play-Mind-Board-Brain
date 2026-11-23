import com.fws.playmind.core.*
fun main(args: Array<String>) {
    val games = args[1].toInt(); val ms = args[2].toLong()   // args[0] chỉ để tương thích với bản gỡ lỗi cũ
    var nw = 0; var dr = 0; var ol = 0
    for (i in 0 until games) {
        val newBlack = i % 2 == 0
        val n = GomokuGame(); val o = GomokuOld()
        val rnd = kotlin.random.Random(i + 11)
        repeat(2) { val lm = n.legalMoves().filter { x -> x % 15 in 5..9 && x / 15 in 5..9 }; val mv = lm[rnd.nextInt(lm.size)]; n.play(mv); o.play(mv) }
        while (n.result == Outcome.ONGOING) {
            val newTurn = (n.turn == 0) == newBlack
            if (newTurn) { val mv = n.bestMove(ms); n.play(mv); o.play(mv) } else { val mv = o.bestMove(ms); n.play(mv); o.play(mv) }
        }
        val r = if (n.result == Outcome.DRAW) 0 else if ((n.result == Outcome.WIN_FIRST) == newBlack) 1 else -1
        if (r > 0) nw++ else if (r == 0) dr++ else ol++
    }
    println("new ${nw}W-${dr}D-${ol}L")
}
