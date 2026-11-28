package com.fws.playmind.core

import kotlin.random.Random

/**
 * Cờ caro (Gomoku) trên bàn cờ VÔ HẠN: không có mép bàn, số ô không bị giới hạn.
 *
 * - Bên đi trước (X, đỏ) = 1, bên đi sau (O, đen) = 2.
 * - Tọa độ (x, y) là số nguyên có dấu, ô đầu tiên thường là (0, 0), y tăng xuống dưới.
 * - Nước đi là một số Int "đóng gói" 2 tọa độ: xem [pack], [px], [py]. Phạm vi ±[LIMIT] ô quanh gốc (hơn 32 nghìn ô mỗi chiều).
 * - Luật: 5 quân liên tiếp trở lên thắng, trừ khi cả hai đầu dãy bị quân đối phương chặn (không còn mép bàn để chặn).
 *
 * Bot dùng [CaroEngine]: mỗi lần suy nghĩ, cắt một cửa sổ 32×32 quanh các quân đang có, tìm nước trong cửa sổ rồi dịch
 * ngược về tọa độ thật. Ngoài cửa sổ coi là ô trống (không chặn), đúng với bàn vô hạn. Xem docs/BOT_GUIDE.md.
 */
class GomokuGame : BoardGame, LeveledEngine {
    companion object {
        /** Phạm vi tọa độ hợp lệ: -LIMIT .. LIMIT-1. */
        const val LIMIT = 16384
        private const val BITS = 15
        private const val MASK = (1 shl BITS) - 1

        fun pack(x: Int, y: Int): Int = ((y + LIMIT) shl BITS) or (x + LIMIT)
        fun px(move: Int): Int = (move and MASK) - LIMIT
        fun py(move: Int): Int = (move shr BITS) - LIMIT
        fun inRange(x: Int, y: Int): Boolean = x >= -LIMIT && x < LIMIT && y >= -LIMIT && y < LIMIT

        private val DIRS = intArrayOf(1, 0, 0, 1, 1, 1, 1, -1)
        private const val MARGIN = 5
        /** delta từ mức này trở lên = nhiễu rộng (chọn ngẫu nhiên quanh quân), xem [levelMove]. */
        const val RANDOM_DELTA = 10_000

        @Volatile var lastMode = ""; private set
        @Volatile var lastDepth = 0; private set
        private val engine by lazy { CaroEngine() }
    }

    override val id = "caro"
    /** Engine riêng (chỉ bộ đo A/B dùng); null = engine dùng chung. */
    internal var engineOverride: CaroEngine? = null

    private val stones = HashMap<Int, Int>()
    private val hist = ArrayList<Int>()
    override val history: List<Int> get() = hist
    override var result = Outcome.ONGOING; private set
    override var reason = EndReason.NONE; private set
    override val turn: Int get() = hist.size % 2
    override val plies: Int get() = hist.size
    override val lastMove: Int get() = if (hist.isEmpty()) -1 else hist[hist.size - 1]

    /** Các ô tạo thành dãy thắng (mã [pack]), rỗng nếu chưa thắng. */
    var winLine: IntArray = IntArray(0); private set

    // Khung bao các quân đã đặt (để giao diện biết nên đưa camera về đâu).
    var minX = 0; private set
    var maxX = 0; private set
    var minY = 0; private set
    var maxY = 0; private set

    /** Chủ của ô (x, y): 0 = trống, 1 = X, 2 = O. Ngoài phạm vi coi như trống. */
    fun at(x: Int, y: Int): Int = if (inRange(x, y)) stones[pack(x, y)] ?: 0 else 0

    override fun reset() {
        stones.clear(); hist.clear(); result = Outcome.ONGOING; reason = EndReason.NONE; winLine = IntArray(0)
        minX = 0; maxX = 0; minY = 0; maxY = 0
    }

    override fun inCheck() = false

    /**
     * Bàn cờ vô hạn nên có vô số nước hợp lệ; hàm này chỉ trả về các ô trống nằm trong vòng 2 ô quanh quân đã có
     * (hoặc ô gốc nếu bàn còn trống) - đủ cho bot, thống kê và kiểm thử. Mọi ô trống trong phạm vi đều được [play] chấp nhận.
     */
    override fun legalMoves(): IntArray {
        if (result != Outcome.ONGOING) return IntArray(0)
        if (hist.isEmpty()) return intArrayOf(pack(0, 0))
        val seen = HashSet<Int>()
        for (m in hist) {
            val cx = px(m); val cy = py(m)
            for (dy in -2..2) for (dx in -2..2) {
                val x = cx + dx; val y = cy + dy
                if (inRange(x, y) && at(x, y) == 0) seen.add(pack(x, y))
            }
        }
        return seen.toIntArray()
    }

    override fun play(move: Int): Boolean {
        if (result != Outcome.ONGOING || move < 0) return false
        val x = px(move); val y = py(move)
        if (!inRange(x, y) || pack(x, y) != move || stones.containsKey(move)) return false
        val p = turn + 1
        stones[move] = p; hist.add(move)
        if (hist.size == 1) { minX = x; maxX = x; minY = y; maxY = y }
        else { if (x < minX) minX = x; if (x > maxX) maxX = x; if (y < minY) minY = y; if (y > maxY) maxY = y }
        val line = winAt(x, y, p)
        if (line != null) {
            winLine = line
            result = if (p == 1) Outcome.WIN_FIRST else Outcome.WIN_SECOND; reason = EndReason.FIVE
        }
        return true
    }

    override fun forfeit(loser: Int, reason: EndReason) {
        if (result != Outcome.ONGOING) return
        result = if (loser == 0) Outcome.WIN_SECOND else Outcome.WIN_FIRST
        this.reason = reason
    }

    override fun copyGame(): GomokuGame {
        val g = GomokuGame()
        g.stones.putAll(stones); g.hist.addAll(hist)
        g.result = result; g.reason = reason; g.winLine = winLine
        g.minX = minX; g.maxX = maxX; g.minY = minY; g.maxY = maxY
        return g
    }

    /** Nếu quân tại (x, y) của người chơi [p] tạo dãy thắng thì trả về các ô của dãy, ngược lại null. */
    private fun winAt(x: Int, y: Int, p: Int): IntArray? {
        var d = 0
        while (d < 8) {
            val dx = DIRS[d]; val dy = DIRS[d + 1]; d += 2
            var fx = x + dx; var fy = y + dy; var nf = 0
            while (at(fx, fy) == p) { nf++; fx += dx; fy += dy }
            val blockedF = at(fx, fy) != 0
            var bx = x - dx; var by = y - dy; var nbk = 0
            while (at(bx, by) == p) { nbk++; bx -= dx; by -= dy }
            val blockedB = at(bx, by) != 0
            val len = 1 + nf + nbk
            if (len >= 5 && !(blockedF && blockedB)) {
                return IntArray(len) { i -> pack(x - nbk * dx + i * dx, y - nbk * dy + i * dy) }
            }
        }
        return null
    }

    // ═════════════════════════ AI (cửa sổ trượt) ═════════════════════════
    override fun bestMove(timeMs: Long): Int = levelMove(BotLevel.SUPREME, timeMs, Random)

    override fun levelMove(level: BotLevel, timeMs: Long, rnd: Random): Int {
        if (result != Outcome.ONGOING) return -1
        val n = hist.size
        lastMode = "book"
        if (n == 0) return pack(0, 0)
        if (n == 1) {
            val c0 = hist[0]
            val opts = ArrayList<Int>()
            for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val x = px(c0) + dx; val y = py(c0) + dy
                if (inRange(x, y)) opts.add(pack(x, y))
            }
            return opts[rnd.nextInt(opts.size)]
        }
        val snap = copyGame()
        val (size, ox, oy) = snap.chooseWindow()
        val owner = IntArray(size * size)
        for ((m, p) in snap.stones) {
            val lx = px(m) - ox; val ly = py(m) - oy
            if (lx in 0 until size && ly in 0 until size) owner[ly * size + lx] = p
        }
        val eng = engineOverride ?: engine
        val cfg = BotLevels.config(id, level, timeMs)
        val r = synchronized(eng) { eng.setPosition(owner, turn + 1); eng.levelSearch(cfg, level) }
        Bot.lastNodes = r.nodes; Bot.lastDepth = r.depth
        lastMode = CaroEngine.lastMode; lastDepth = CaroEngine.lastDepth
        var c = r.best
        // Nhiễu "rộng" (delta >= RANDOM_DELTA, cấp Dễ): chọn ngẫu nhiên một ô trống trong vòng 2 ô quanh quân, trừ khi phải
        // thắng / chặn năm / đi VCF (lớp an toàn, BOT_GUIDE mục 4.2 và 7.4: Dễ chỉ chặn bốn trực tiếp).
        val forcedMode = CaroEngine.lastMode in setOf("win", "block", "vcf")
        if (cfg.noiseP > 0 && cfg.delta >= RANDOM_DELTA && !forcedMode && rnd.nextDouble() < cfg.noiseP) {
            val all = legalMoves()
            if (all.isNotEmpty()) return all[rnd.nextInt(all.size)]
        }
        if (cfg.noiseP > 0 && r.near.isNotEmpty() && rnd.nextDouble() < cfg.noiseP) c = r.near[rnd.nextInt(r.near.size)]
        if (c < 0) return legalMoves().firstOrNull() ?: -1
        return pack(ox + c % size, oy + c / size)
    }

    /** Cửa sổ vuông (cỡ, gốc x, gốc y) bao các quân với lề [MARGIN]; nếu quá lớn thì giữ vùng đang đánh nhau. */
    private fun chooseWindow(): Triple<Int, Int, Int> {
        val span = maxOf(maxX - minX, maxY - minY) + 1
        val wanted = span + 2 * MARGIN
        if (wanted <= CaroEngine.P) {
            val size = CaroEngine.P
            val cx = Math.floorDiv(minX + maxX, 2); val cy = Math.floorDiv(minY + maxY, 2)
            return Triple(size, cx - size / 2, cy - size / 2)
        }
        // Ván rất dài / quân rải rộng: dùng cửa sổ tối đa, đặt vào cụm quân "nóng" nhất.
        // Trọng số mỗi quân = 1 + số quân kề (cụm dày quan trọng hơn quân lẻ) + thưởng cho 2 nước vừa đi.
        val size = CaroEngine.P
        val n = hist.size
        val xs = IntArray(n) { px(hist[it]) }; val ys = IntArray(n) { py(hist[it]) }
        val w = IntArray(n) { 1 }
        for (i in 0 until n) {
            var k = 0
            for (dy in -1..1) for (dx in -1..1) if ((dx != 0 || dy != 0) && at(xs[i] + dx, ys[i] + dy) != 0) k++
            w[i] += k
        }
        w[n - 1] += 6; if (n >= 2) w[n - 2] += 3
        var bx = xs[n - 1]; var by = ys[n - 1]; var bestScore = -1
        fun consider(cx: Int, cy: Int) {
            val ox = cx - size / 2; val oy = cy - size / 2
            var sc = 0
            for (i in 0 until n) {
                val x = xs[i] - ox; val y = ys[i] - oy
                if (x in 2 until size - 2 && y in 2 until size - 2) sc += w[i]
            }
            if (sc > bestScore) { bestScore = sc; bx = cx; by = cy }
        }
        for (i in 0 until n) consider(xs[i], ys[i])
        consider(Math.floorDiv(minX + maxX, 2), Math.floorDiv(minY + maxY, 2))
        return Triple(size, bx - size / 2, by - size / 2)
    }
}
