import com.fws.playmind.core.*
// Bọc bot cờ vây của app thành động cơ GTP tối thiểu để đấu với GNU Go.
fun main(args: Array<String>) {
    val ms = (args.getOrNull(0) ?: "1000").toLong()
    var g = GoGame(9)
    fun toCell(v: String): Int { if (v.equals("pass", true)) return GoGame.PASS; val c = v[0].uppercaseChar(); val x = c - 'A' - (if (c > 'I') 1 else 0); val y = 9 - v.substring(1).toInt(); return y * 9 + x }
    fun fromCell(m: Int): String { if (m == GoGame.PASS) return "pass"; val x = m % 9; val y = m / 9; val c = 'A' + x + (if (x >= 8) 1 else 0); return "$c${9 - y}" }
    while (true) {
        val line = readLine() ?: break
        val t = line.trim().split(" "); if (t.isEmpty() || t[0].isEmpty()) continue
        var id = ""; var cmd = t
        if (t[0].all { it.isDigit() }) { id = t[0]; cmd = t.drop(1) }
        val resp: String = when (cmd[0]) {
            "boardsize" -> { g = GoGame(cmd[1].toInt()); "" }
            "clear_board" -> { g = GoGame(g.size); "" }
            "komi" -> ""
            "name" -> "playmind"
            "version" -> "1"
            "protocol_version" -> "2"
            "play" -> { val m = toCell(cmd[2]); if (g.play(m)) "" else "?illegal" }
            "genmove" -> { val m = g.bestMove(ms); if (!g.play(m)) { g.play(GoGame.PASS); "pass" } else fromCell(m) }
            "quit" -> { println("=$id \n"); return }
            else -> ""
        }
        if (resp.startsWith("?")) println("?$id ${resp.drop(1)}\n") else println("=$id $resp\n")
        System.out.flush()
    }
}
