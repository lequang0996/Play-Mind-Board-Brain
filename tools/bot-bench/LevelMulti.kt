package bench

import com.fws.playmind.core.*
import kotlin.random.Random

/**
 * Kiểm tra thứ tự cấp ở chế độ 3-4 người (BOT_GUIDE mục 12.1): một bot cấp [hi] với các bot còn lại cùng cấp [lo],
 * vị trí ngồi của bot cấp cao xoay vòng. In số ván bot cấp cao thắng; phải cao rõ rệt hơn tỉ lệ ngẫu nhiên 1/n.
 *   java -cp <cp> bench.LevelMultiKt <quoridor|wallgo> <players> <hi> <lo> <games> <svMs> <seed0>
 */
fun main(args: Array<String>) {
    val game = args[0]; val players = args[1].toInt(); val hi = args[2].toInt(); val lo = args[3].toInt()
    val games = args[4].toInt(); val ms = args[5].toLong(); val seed0 = args[6].toInt()
    var wins = 0; var draws = 0
    for (i in 0 until games) {
        val seat = i % players
        val g: BoardGame = if (game == "quoridor") QuoridorGame(players) else WallGoGame(players)
        val rnd = Random(seed0 + i)
        var guard = 0
        while (g.result == Outcome.ONGOING && guard++ < 1500) {
            val lv = BotLevel.of(if (g.turn == seat) hi else lo)
            val m = (g as LeveledEngine).levelMove(lv, ms, rnd)
            if (m < 0 || !g.play(m)) break
        }
        val winner = when (g) { is QuoridorGame -> g.winner; is WallGoGame -> g.winner; else -> -1 }
        if (winner == seat) wins++ else if (winner < 0) draws++
        println("R $i $seat $winner")
        System.out.flush()
    }
    println("TOTAL $wins $draws $games")
}
