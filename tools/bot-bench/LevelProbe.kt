package bench

import com.fws.playmind.core.*
import kotlin.random.Random

/**
 * Đo số nút Siêu việt duyệt trong một nước và thời gian nghĩ của từng cấp (BOT_GUIDE mục 3.2, 11.1).
 *   java -cp <cp> bench.LevelProbeKt <game> <svMs> <positions> [players]
 * In trung vị số nút của Siêu việt và thời gian trung vị / lớn nhất của mỗi cấp trên cùng bộ thế.
 */
fun main(args: Array<String>) {
    val game = args[0]; val ms = args[1].toLong(); val npos = args[2].toInt(); val players = args.getOrNull(3)?.toInt() ?: 2
    val r = Random(7)
    val positions = ArrayList<BoardGame>()
    while (positions.size < npos) {
        val g: BoardGame = when (game) {
            "chess" -> ChessGame(); "xiangqi" -> XiangqiGame(); "caro" -> GomokuGame()
            "quoridor" -> QuoridorGame(players); "wallgo" -> WallGoGame(players); else -> error("game")
        }
        // đi tới giữa ván bằng chính bot cấp Khá có nhiễu để có thế đa dạng
        val target = 6 + r.nextInt(30)
        while (g.result == Outcome.ONGOING && g.plies < target) {
            val m = (g as LeveledEngine).levelMove(BotLevel.of(r.nextInt(3)), ms, r)
            if (m < 0 || !g.play(m)) break
        }
        if (g.result == Outcome.ONGOING) positions.add(g)
    }
    for (lv in BotLevel.entries) {
        val times = ArrayList<Long>(); val nodes = ArrayList<Long>(); val depths = ArrayList<Int>()
        for (g in positions) {
            val t0 = System.nanoTime()
            Bot.lastNodes = 0; Bot.lastDepth = 0
            (g.copyGame() as LeveledEngine).levelMove(lv, ms, Random(1))
            times.add((System.nanoTime() - t0) / 1_000_000); nodes.add(Bot.lastNodes); depths.add(Bot.lastDepth)
        }
        times.sort(); nodes.sort(); depths.sort()
        println("$game $lv time median=${times[times.size / 2]}ms p90=${times[times.size * 9 / 10]}ms max=${times.last()}ms  nodes median=${nodes[nodes.size / 2]}  depth median=${depths[depths.size / 2]}")
    }
}
