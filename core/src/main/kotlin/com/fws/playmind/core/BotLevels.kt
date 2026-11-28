package com.fws.playmind.core

import kotlin.random.Random

/**
 * 7 cấp bot theo docs/BOT_GUIDE.md (mục 1): mỗi cấp là một bot.
 * Siêu việt là bot toàn lực (giới hạn theo thời gian); sáu cấp dưới giới hạn theo độ sâu hoặc số nút để sức mạnh
 * không đổi giữa các điện thoại (mục 3.2), cộng nhiễu có kiểm soát ở các cấp thấp (mục 4.2).
 */
enum class BotLevel { EASY, MEDIUM, GOOD, STRONG, MASTER, CHALLENGER, SUPREME;
    companion object {
        fun of(i: Int): BotLevel = entries[i.coerceIn(0, entries.size - 1)]
    }
}

/** Giới hạn một lần tìm kiếm; 0 = không giới hạn theo tiêu chí đó. */
class SearchLimit(val depth: Int = 0, val nodes: Long = 0, val timeMs: Long = 0) {
    override fun toString() = buildString {
        if (depth > 0) append("depth=$depth ")
        if (nodes > 0) append("nodes=$nodes ")
        if (timeMs > 0) append("time=${timeMs}ms")
    }.trim()
}

/**
 * Cấu hình một cấp (mục 4.4).
 * - [limit]: giới hạn tìm kiếm. Siêu việt dùng [SearchLimit.timeMs] = botMillis (điền lúc chạy).
 * - [noiseP], [delta]: với xác suất [noiseP], chọn ngẫu nhiên một nước khác nước tốt nhất nhưng điểm kém không quá [delta]
 *   (đơn vị điểm của engine đó).
 * - [extra]: tham số riêng của game (ví dụ caro: độ sâu VCF tối đa; cờ vây: số lượt mô phỏng).
 */
class LevelConfig(val limit: SearchLimit, val noiseP: Double = 0.0, val delta: Int = 0, val extra: Int = 0) {
    override fun toString() = "$limit p=$noiseP δ=$delta" + (if (extra != 0) " extra=$extra" else "")
}

/** Kết quả tìm kiếm ở gốc. [near] = các nước khác có điểm không kém nước tốt nhất quá delta. */
class RootResult(val best: Int, val score: Int, val depth: Int, val nodes: Long, val near: IntArray = IntArray(0))

/** Engine có 7 cấp. [timeMs] là ngân sách của cấp Siêu việt (botMillis); các cấp dưới không dùng thời gian. */
interface LeveledEngine {
    fun levelMove(level: BotLevel, timeMs: Long, rnd: Random = Random): Int
}

/**
 * BẢNG CẤU HÌNH DUY NHẤT của 7 cấp cho mọi game (mục 4.4). Giá trị hiện tại là điểm khởi đầu theo mục 5 đến 10
 * của guide và theo số nút/giây đo trên máy phát triển, CHƯA qua đo nghiệm thu (mục 11). Sửa ở đây, không rải rác.
 * Thứ tự phần tử: Dễ, Trung bình, Khá, Giỏi, Cao thủ, Thách đấu, Siêu việt.
 */
object BotLevels {
    private fun d(depth: Int, p: Double = 0.0, delta: Int = 0, extra: Int = 0) = LevelConfig(SearchLimit(depth = depth), p, delta, extra)
    private fun n(nodes: Long, p: Double = 0.0, delta: Int = 0, extra: Int = 0) = LevelConfig(SearchLimit(nodes = nodes), p, delta, extra)
    private fun top(extra: Int = 0) = LevelConfig(SearchLimit(), extra = extra)   // Siêu việt: thời gian điền lúc chạy

    // Số nút/giây đo trên máy phát triển (1 luồng, JVM) với giả định điện thoại chậm hơn k = 4 lần (CHƯA đo trên máy thật,
    // mục 3.2). Với botMillis hiện tại (Catalog), Siêu việt trên điện thoại ước tính duyệt: cờ vua ~900k nút / 8 s,
    // cờ tướng ~1,3M / 8 s, caro ~330k / 6 s, Quoridor ~180k / 7 s, Wall Go ~26k / 7 s.
    // Mọi cấp giới hạn nút đều thỏa node_limit <= 0,5 × botMillis × nps điện thoại.

    // Cờ vua (mục 5.3). Điểm: centipawn.
    // Hiệu chỉnh 11/2026 trên CI (ladder có props, xem tools/bot-bench/results/g5, g7, g9): SV-TĐ(n15000) 89%,
    // TĐ-CT(n4000) 72%, CT-Giỏi(n700) 76%, Giỏi-Khá(d2 p0.4) 87%; hai cấp dưới chờ nghiệm thu.
    val chess = listOf(
        d(1, 1.0, 500), d(1, 0.60, 200), d(2, 0.50, 150),
        n(700, 0.05, 20), n(4_000), n(15_000), top(),
    )

    // Cờ tướng (mục 6.3). Cùng khung với cờ vua, điểm theo thang quân cờ tướng.
    // Hiệu chỉnh 11/2026 (g7, g9): SV-TĐ n8000 khoảng 85% (n6000 88%, n20000 78%); các cấp dưới theo cùng khung với cờ vua.
    val xiangqi = listOf(
        d(1, 0.90, 450), d(1, 0.60, 250), d(2, 0.50, 180),
        n(300, 0.05, 25), n(1_500), n(8_000), top(),
    )

    // Cờ caro (mục 7.4). extra = độ sâu VCF tối đa (0 = tắt); phòng VCF bật từ Giỏi (CaroEngine.levelSearch).
    // Hiệu chỉnh 11/2026 (g7, g9 và máy phát triển): SV-TĐ(n4000) khoảng 80%, TĐ-CT 72%, CT-Giỏi 78%, Giỏi-Khá 84%,
    // Khá-TB 90%, TB-Dễ 94%. delta >= GomokuGame.RANDOM_DELTA = nhiễu rộng (đi ngẫu nhiên quanh quân với xác suất p).
    val caro = listOf(
        d(1, 0.50, 20_000, 0), d(1, 0.15, 20_000, 0), d(1, 0.20, 800, 3),
        n(250, 0.03, 200, 12), n(1_000, 0.0, 0, 24), n(4_000, 0.0, 0, 24), top(24),
    )

    // Quoridor (mục 9.3), alpha-beta. Điểm: 100 = một bước đường đi. Thang sức của Quoridor rất nén: tìm sâu thêm cho ít Elo
    // (Siêu việt hơn n300 khoảng 81%, n1000 khoảng 75%). Nhiễu có delta từ 200 (2 bước) trở lên làm bot đi lòng vòng và thua
    // vì lặp thế, nên chỉ cấp Dễ dùng mức đó; các cấp khác giữ delta <= 150. Đo 11/2026 trên máy phát triển, xem mục 13.
    val quoridor = listOf(
        d(1, 0.80, 300), d(1, 1.0, 150), d(1, 0.50, 150),
        d(1, 0.20, 60), d(1), n(300), top(),
    )

    // Wall Go (mục 10.3), alpha-beta. Điểm: 100 = một ô lãnh thổ. Một nút rất nặng (đánh giá mọi nước con), Siêu việt
    // chỉ duyệt vài nghìn nút mỗi nước, nên các cấp dùng độ sâu (không đổi theo máy). Đo 11/2026 (200 ván mỗi cặp, máy phát triển):
    // SV-TĐ 83%, TĐ-CT 83%, CT-Giỏi 79%, Giỏi-Khá 86%, Khá-TB 90%, TB-Dễ 96%: cả 6 cặp trong khoảng đạt (mục 1.3).
    val wallgo = listOf(
        d(1, 1.0, 700), d(1, 0.9, 400), d(1, 0.7, 200),
        d(1), d(2), d(3), top(),
    )

    // Cờ vây (mục 8.3 / 8.3a). extra = số lượt mô phỏng. Dùng cho MCTS dự phòng khi không chạy được KataGo;
    // cấu hình KataGo nằm ở app (KataGo.kt) vì engine chạy ngoài core.
    val go = listOf(
        n(0, extra = 8), n(0, extra = 40), n(0, extra = 200),
        n(0, extra = 800), n(0, extra = 3_200), n(0, extra = 12_000), top(),
    )

    fun of(gameId: String): List<LevelConfig> = when (gameId) {
        "chess" -> chess; "xiangqi" -> xiangqi; "caro" -> caro
        "quoridor" -> quoridor; "wallgo" -> wallgo; "go" -> go
        else -> chess
    }

    /** Cấu hình thực tế của một cấp: Siêu việt nhận ngân sách thời gian [timeMs]. */
    fun config(gameId: String, level: BotLevel, timeMs: Long): LevelConfig {
        val c = override(gameId, level) ?: of(gameId)[level.ordinal]
        if (level != BotLevel.SUPREME) return c
        return LevelConfig(SearchLimit(timeMs = timeMs), c.noiseP, c.delta, c.extra)
    }

    /**
     * Chỉ dùng khi đo (bộ bench): thuộc tính hệ thống "pm.level.<game>.<cấp 0..6>" = "d<độ sâu>" hoặc "n<số nút>",
     * thêm tùy chọn ",p<xác suất nhiễu>,x<delta>,e<extra>". Ví dụ -Dpm.level.chess.5=n15000. App không đặt thuộc tính này.
     */
    private fun override(gameId: String, level: BotLevel): LevelConfig? {
        val v = System.getProperty("pm.level.$gameId.${level.ordinal}") ?: return null
        val base = of(gameId)[level.ordinal]
        var lim = base.limit; var p = base.noiseP; var dl = base.delta; var ex = base.extra
        for (t in v.split(',')) when (t.firstOrNull()) {
            'd' -> lim = SearchLimit(depth = t.drop(1).toInt())
            'n' -> lim = SearchLimit(nodes = t.drop(1).toLong())
            'p' -> p = t.drop(1).toDouble()
            'x' -> dl = t.drop(1).toInt()
            'e' -> ex = t.drop(1).toInt()
        }
        return LevelConfig(lim, p, dl, ex)
    }
}

/**
 * Lớp an toàn chung (mục 4.2), chạy TRƯỚC nhiễu và độc lập với giới hạn độ sâu:
 *  (a) có nước thắng ngay thì đi ngay;
 *  (b) loại khỏi danh sách được chọn mọi nước để bên kế tiếp thắng ngay (nếu nước nào cũng thua thì bỏ bước này);
 *  (c) tìm kiếm trong các nước còn lại; với xác suất p chọn một nước trong nhóm "gần tốt nhất".
 */
object Bot {
    /** Số nút và độ sâu của lần tìm kiếm gần nhất (để đo, BOT_GUIDE mục 3.2). */
    @Volatile var lastNodes = 0L
    @Volatile var lastDepth = 0

    fun pick(
        g: BoardGame, cfg: LevelConfig, rnd: Random,
        winNow: () -> Int,
        losesNext: (Int) -> Boolean,
        search: (SearchLimit, IntArray?, Int) -> RootResult,
    ): Int {
        if (g.result != Outcome.ONGOING) return -1
        val w = winNow()
        if (w >= 0) return w
        val all = g.legalMoves()
        if (all.isEmpty()) return -1
        if (all.size == 1) return all[0]
        val safe = all.filter { !losesNext(it) }.toIntArray()
        val allowed = if (safe.isEmpty() || safe.size == all.size) null else safe
        val wantNoise = cfg.noiseP > 0 && cfg.delta > 0
        val r = search(cfg.limit, allowed, if (wantNoise) cfg.delta else 0)
        lastNodes = r.nodes; lastDepth = r.depth
        var best = r.best
        if (best < 0 || (allowed != null && best !in allowed)) best = (allowed ?: all)[0]
        if (wantNoise && r.near.isNotEmpty() && rnd.nextDouble() < cfg.noiseP) {
            val pool = if (allowed == null) r.near else r.near.filter { it in allowed }.toIntArray()
            if (pool.isNotEmpty()) return pool[rnd.nextInt(pool.size)]
        }
        return best
    }

    /** Nước thắng ngay bằng cách thử từng nước trên bản sao (dùng cho game ít nước). */
    fun genericWin(g: BoardGame): Int {
        val me = g.turn
        for (m in g.legalMoves()) {
            val c = g.copyGame()
            if (c.play(m) && wins(c, me)) return m
        }
        return -1
    }

    /** Nước [m] có để bên đi kế tiếp thắng ngay không (thử mọi nước đáp trả trên bản sao). */
    fun genericLosesNext(g: BoardGame, m: Int): Boolean {
        val c = g.copyGame()
        if (!c.play(m) || c.result != Outcome.ONGOING) return false
        val opp = c.turn
        for (r in c.legalMoves()) {
            val d = c.copyGame()
            if (d.play(r) && wins(d, opp)) return true
        }
        return false
    }

    /** Ván [g] đã kết thúc với bên [side] thắng (2 người: theo Outcome). */
    fun wins(g: BoardGame, side: Int): Boolean =
        (side == 0 && g.result == Outcome.WIN_FIRST) || (side == 1 && g.result == Outcome.WIN_SECOND)
}
