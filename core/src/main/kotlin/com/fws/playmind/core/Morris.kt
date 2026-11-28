package com.fws.playmind.core

import kotlin.random.Random

/**
 * Cờ cối xay (Nine Men's Morris, trò chơi dân gian): bàn 3 hình vuông đồng tâm, 24 điểm, 16 đường 3 điểm (cối xay).
 * Mỗi bên 9 quân; Đen (ghế 0) đi trước.
 *
 * - Đặt quân: lần lượt đặt 9 quân vào điểm trống; chưa được đi quân cho tới khi đặt xong.
 * - Đi quân: mỗi lượt đi một quân dọc đường nối sang điểm trống kề bên. Còn đúng 3 quân (đã đặt xong) thì được bay tới
 *   điểm trống bất kỳ.
 * - Cối xay: xếp 3 quân trên một đường thì ăn 1 quân đối thủ không nằm trong cối xay; nếu mọi quân đối thủ đều nằm trong
 *   cối xay thì không được ăn (luật của đề). Tạo 2 cối xay cùng lúc vẫn chỉ ăn 1 quân. Mở rồi đóng lại cùng cối xay vẫn tính.
 * - Thắng: đối thủ còn 2 quân (gồm cả quân chưa đặt), hoặc tới lượt đối thủ mà không có nước đi.
 * - Bế tắc: khi đã có bên chỉ còn 3 quân, 10 lượt mỗi bên ([STALL_PLIES] nửa nước) không có quân bị ăn thì kết thúc:
 *   bên ít quân hơn thua, bằng nhau thì hòa. Thêm luật chống ván vô tận: [DRAW_PLIES] nửa nước không ăn quân thì hòa.
 *
 * Mã nước đi: ((from + 1) × 24 + to) × 25 + cap; from = -1 khi đặt quân, cap = 24 khi không ăn.
 */
class MorrisGame : BoardGame, LeveledEngine {
    companion object {
        const val N = 24
        const val PIECES = 9
        const val NO_CAP = 24
        const val STALL_PLIES = 20
        const val DRAW_PLIES = 120

        fun enc(from: Int, to: Int, cap: Int) = ((from + 1) * 24 + to) * 25 + cap
        fun fromOf(m: Int) = m / 25 / 24 - 1
        fun toOf(m: Int) = (m / 25) % 24
        fun capOf(m: Int) = m % 25

        /** Tọa độ lưới 7×7 của điểm (vòng r = 0 ngoài cùng .. 2 trong cùng, k = 0..7 theo chiều kim đồng hồ từ góc trên trái). */
        val XY: Array<IntArray> = Array(N) { i ->
            val r = i / 8; val k = i % 8; val s = r; val e = 6 - r; val m = 3
            when (k) { 0 -> intArrayOf(s, s); 1 -> intArrayOf(m, s); 2 -> intArrayOf(e, s); 3 -> intArrayOf(e, m)
                4 -> intArrayOf(e, e); 5 -> intArrayOf(m, e); 6 -> intArrayOf(s, e); else -> intArrayOf(s, m) }
        }
        val ADJ: Array<IntArray> = Array(N) { i ->
            val r = i / 8; val k = i % 8
            val l = arrayListOf(r * 8 + (k + 1) % 8, r * 8 + (k + 7) % 8)
            if (k % 2 == 1) { if (r > 0) l.add((r - 1) * 8 + k); if (r < 2) l.add((r + 1) * 8 + k) }
            l.toIntArray()
        }
        val MILLS: Array<IntArray> = buildList {
            for (r in 0..2) for (c in 0 until 4) add(intArrayOf(r * 8 + 2 * c, r * 8 + 2 * c + 1, r * 8 + (2 * c + 2) % 8))
            for (k in intArrayOf(1, 3, 5, 7)) add(intArrayOf(k, 8 + k, 16 + k))
        }.toTypedArray()
        val MILLS_OF: Array<IntArray> = Array(N) { p -> MILLS.indices.filter { p in MILLS[it] }.toIntArray() }

        private val ZB: Array<LongArray> = Random(424242).let { r -> Array(N) { LongArray(2) { r.nextLong() } } }
        private val ZSIDE = Random(7).nextLong()
        private val ZHAND: Array<LongArray> = Random(99).let { r -> Array(2) { LongArray(PIECES + 1) { r.nextLong() } } }
    }

    override val id = "morris"
    val board = IntArray(N) { -1 }
    val hand = intArrayOf(PIECES, PIECES)
    val count = intArrayOf(0, 0)
    var side = 0; private set
    /** Nửa nước liên tiếp không ăn quân. */
    var quiet = 0; private set
    /** Nửa nước kể từ khi luật bế tắc bắt đầu (có bên còn 3 quân) mà không ăn quân; -1 = chưa bắt đầu. */
    var stall = -1; private set
    private val hist = ArrayList<Int>()
    override var result = Outcome.ONGOING; private set
    override var reason = EndReason.NONE; private set
    override val turn: Int get() = side
    override val plies: Int get() = hist.size
    override val lastMove: Int get() = if (hist.isEmpty()) -1 else hist[hist.size - 1]
    override val history: List<Int> get() = hist
    override fun inCheck() = false

    fun placing(p: Int = side) = hand[p] > 0
    fun flying(p: Int) = hand[p] == 0 && count[p] == 3
    fun total(p: Int) = hand[p] + count[p]

    override fun reset() {
        board.fill(-1); hand[0] = PIECES; hand[1] = PIECES; count[0] = 0; count[1] = 0
        side = 0; quiet = 0; stall = -1; hist.clear(); result = Outcome.ONGOING; reason = EndReason.NONE
    }

    override fun copyGame(): MorrisGame {
        val g = MorrisGame()
        board.copyInto(g.board); hand.copyInto(g.hand); count.copyInto(g.count)
        g.side = side; g.quiet = quiet; g.stall = stall; g.hist.addAll(hist); g.result = result; g.reason = reason
        return g
    }

    /** Điểm [p] có nằm trong cối xay của chủ quân đang đứng ở đó không. */
    fun inMill(p: Int, b: IntArray = board): Boolean {
        val c = b[p]; if (c < 0) return false
        for (mi in MILLS_OF[p]) { val m = MILLS[mi]; if (b[m[0]] == c && b[m[1]] == c && b[m[2]] == c) return true }
        return false
    }

    /** Quân [owner] đặt ở [to] (đã đặt lên bàn) có tạo cối xay không. */
    private fun makesMill(to: Int, owner: Int): Boolean {
        for (mi in MILLS_OF[to]) { val m = MILLS[mi]; if (board[m[0]] == owner && board[m[1]] == owner && board[m[2]] == owner) return true }
        return false
    }

    /** Quân đối thủ có thể bị ăn; rỗng nếu mọi quân đều trong cối xay (luật của đề: khi đó không ăn được). */
    private fun capturable(opp: Int, out: IntArray): Int {
        var n = 0
        for (p in 0 until N) if (board[p] == opp && !inMill(p)) out[n++] = p
        return n
    }

    private val capBuf = IntArray(N)

    override fun legalMoves(): IntArray {
        if (result != Outcome.ONGOING) return IntArray(0)
        val out = ArrayList<Int>(64)
        gen(out)
        return out.toIntArray()
    }

    private fun gen(out: MutableList<Int>) {
        val me = side; val opp = 1 - me
        fun add(from: Int, to: Int) {
            // thử đặt để biết có cối xay không
            if (from >= 0) board[from] = -1
            board[to] = me
            if (makesMill(to, me)) {
                val n = capturable(opp, capBuf)
                if (n == 0) out.add(enc(from, to, NO_CAP)) else for (i in 0 until n) out.add(enc(from, to, capBuf[i]))
            } else out.add(enc(from, to, NO_CAP))
            board[to] = -1
            if (from >= 0) board[from] = me
        }
        if (hand[me] > 0) { for (t in 0 until N) if (board[t] < 0) add(-1, t); return }
        val fly = count[me] == 3
        for (f in 0 until N) {
            if (board[f] != me) continue
            if (fly) { for (t in 0 until N) if (board[t] < 0) add(f, t) }
            else for (t in ADJ[f]) if (board[t] < 0) add(f, t)
        }
    }

    override fun play(move: Int): Boolean {
        if (result != Outcome.ONGOING) return false
        if (move !in legalMoves()) return false
        apply(move)
        hist.add(move)
        checkEnd()
        return true
    }

    /** Trạng thái để hoàn tác: quiet, stall. */
    private fun apply(m: Int): Long {
        val me = side; val opp = 1 - me
        val f = fromOf(m); val t = toOf(m); val c = capOf(m)
        val saved = (quiet.toLong() shl 32) or (stall + 1).toLong()
        if (f < 0) { hand[me]--; count[me]++ } else board[f] = -1
        board[t] = me
        if (c != NO_CAP) { board[c] = -1; count[opp]-- }
        // bế tắc chỉ tính sau khi cả hai đã đặt xong và có bên còn 3 quân
        if (c != NO_CAP) { quiet = 0; if (stall >= 0) stall = 0 } else quiet++
        val trigger = hand[0] == 0 && hand[1] == 0 && (count[0] <= 3 || count[1] <= 3)
        if (stall < 0 && trigger) stall = 0
        else if (stall >= 0 && c == NO_CAP) stall++
        side = opp
        return saved
    }

    private fun undo(m: Int, saved: Long) {
        side = 1 - side
        val me = side; val opp = 1 - me
        val f = fromOf(m); val t = toOf(m); val c = capOf(m)
        if (c != NO_CAP) { board[c] = opp; count[opp]++ }
        board[t] = -1
        if (f < 0) { hand[me]++; count[me]-- } else board[f] = me
        quiet = (saved ushr 32).toInt(); stall = (saved and 0xffffffffL).toInt() - 1
    }

    /** Kết quả từ góc nhìn người vừa đi xong (side là người đi kế tiếp): 1 thắng, -1 thua, 0 hòa, null chưa xong. */
    private fun terminal(): Int? {
        val mover = 1 - side
        if (total(side) <= 2) return 1
        if (total(mover) <= 2) return -1
        if (stall >= STALL_PLIES) return when { count[mover] > count[side] -> 1; count[mover] < count[side] -> -1; else -> 0 }
        if (quiet >= DRAW_PLIES) return 0
        if (hand[side] == 0 && count[side] > 3 && !hasStep(side)) return 1
        return null
    }

    private fun hasStep(p: Int): Boolean {
        for (f in 0 until N) if (board[f] == p) for (t in ADJ[f]) if (board[t] < 0) return true
        return false
    }

    private fun checkEnd() {
        val r = terminal() ?: return
        val mover = 1 - side
        val noMove = hand[side] == 0 && count[side] > 3 && !hasStep(side) && total(side) > 2
        reason = when {
            total(side) <= 2 || total(mover) <= 2 -> EndReason.MATERIAL
            stall >= STALL_PLIES -> EndReason.STALL
            quiet >= DRAW_PLIES -> EndReason.MOVE_LIMIT
            noMove -> EndReason.NOMOVES
            else -> EndReason.NOMOVES
        }
        result = when (r) { 1 -> if (mover == 0) Outcome.WIN_FIRST else Outcome.WIN_SECOND
            -1 -> if (mover == 0) Outcome.WIN_SECOND else Outcome.WIN_FIRST
            else -> Outcome.DRAW }
    }

    override fun forfeit(loser: Int, reason: EndReason) {
        if (result != Outcome.ONGOING) return
        this.reason = reason
        result = if (loser == 0) Outcome.WIN_SECOND else Outcome.WIN_FIRST
    }

    // ───────────────────────── tìm kiếm alpha-beta ─────────────────────────

    private val INF = 1_000_000
    private val WIN = 100_000
    private var nodes = 0L
    private var nodeLimit = Long.MAX_VALUE
    private var deadline = Long.MAX_VALUE
    private var aborted = false
    private val TT_SIZE = 1 shl 18
    private val ttKey = LongArray(TT_SIZE)
    private val ttVal = IntArray(TT_SIZE)
    private val ttDepth = ByteArray(TT_SIZE)
    private val ttFlag = ByteArray(TT_SIZE)
    private val ttMove = IntArray(TT_SIZE)

    private fun key(): Long {
        var h = if (side == 1) ZSIDE else 0L
        for (p in 0 until N) if (board[p] >= 0) h = h xor ZB[p][board[p]]
        h = h xor ZHAND[0][hand[0]] xor ZHAND[1][hand[1]]
        return h xor (stall.toLong() shl 48) xor (if (quiet > DRAW_PLIES - 12) quiet.toLong() shl 40 else 0L)
    }

    /** Đánh giá từ góc nhìn bên đang đi. */
    private fun evaluate(): Int {
        val me = side; val opp = 1 - me
        var s = (total(me) - total(opp)) * 120
        var open = 0; var closed = 0
        for (m in MILLS) {
            var a = 0; var b = 0; var e = 0
            for (p in m) when (board[p]) { me -> a++; opp -> b++; else -> e++ }
            if (a == 3) closed++ else if (b == 3) closed--
            if (a == 2 && e == 1) open++ else if (b == 2 && e == 1) open--
        }
        s += closed * 14 + open * 22
        if (hand[me] == 0 && hand[opp] == 0) {
            var mobMe = 0; var mobOpp = 0; var blkMe = 0; var blkOpp = 0
            for (p in 0 until N) {
                val c = board[p]; if (c < 0) continue
                var free = 0; for (t in ADJ[p]) if (board[t] < 0) free++
                if (c == me) { mobMe += free; if (free == 0) blkMe++ } else { mobOpp += free; if (free == 0) blkOpp++ }
            }
            if (!flying(me) && !flying(opp)) s += (mobMe - mobOpp) * 4 + (blkOpp - blkMe) * 10
            if (!flying(opp) && mobOpp == 0 && count[opp] > 3) s += WIN / 2
        }
        // vị trí ngã tư (4 đường nối) quý hơn khi đang đặt quân
        if (hand[me] > 0 || hand[opp] > 0) for (p in 0 until N) if (board[p] >= 0 && ADJ[p].size == 4) s += if (board[p] == me) 6 else -6
        return s
    }

    private fun ab(depth: Int, alphaIn: Int, beta: Int, ply: Int): Int {
        nodes++
        if ((nodes and 1023L) == 0L && (nodes > nodeLimit || System.nanoTime() > deadline)) aborted = true
        if (aborted) return 0
        var alpha = alphaIn
        val k = key(); val slot = (k and (TT_SIZE - 1).toLong()).toInt()
        var ttm = -1
        if (ttKey[slot] == k) {
            ttm = ttMove[slot]
            if (ttDepth[slot] >= depth) {
                val v = ttVal[slot]
                when (ttFlag[slot].toInt()) { 0 -> return v; 1 -> if (v >= beta) return v; 2 -> if (v <= alpha) return v }
            }
        }
        if (depth <= 0) return quiesce(alpha, beta, ply, 4)
        val moves = ArrayList<Int>(48); gen(moves)
        if (moves.isEmpty()) return -WIN + ply
        order(moves, ttm)
        var best = -INF; var bm = -1
        for (m in moves) {
            val sv = apply(m)
            val t = terminal()
            val v = if (t != null) (if (t > 0) WIN - ply else if (t < 0) -WIN + ply else 0) else -ab(depth - 1, -beta, -alpha, ply + 1)
            undo(m, sv)
            if (aborted) return 0
            if (v > best) { best = v; bm = m }
            if (v > alpha) alpha = v
            if (alpha >= beta) break
        }
        ttKey[slot] = k; ttVal[slot] = best; ttDepth[slot] = depth.toByte(); ttMove[slot] = bm
        ttFlag[slot] = (if (best <= alphaIn) 2 else if (best >= beta) 1 else 0).toByte()
        return best
    }

    /** Tìm tiếp các nước ăn quân để tránh đánh giá sai giữa chừng một chuỗi cối xay. */
    private fun quiesce(alphaIn: Int, beta: Int, ply: Int, left: Int): Int {
        nodes++
        val stand = evaluate()
        if (left == 0 || stand >= beta) return stand
        var alpha = maxOf(alphaIn, stand)
        val moves = ArrayList<Int>(48); gen(moves)
        if (moves.isEmpty()) return -WIN + ply
        var best = stand
        for (m in moves) {
            if (capOf(m) == NO_CAP) continue
            val sv = apply(m)
            val t = terminal()
            val v = if (t != null) (if (t > 0) WIN - ply else if (t < 0) -WIN + ply else 0) else -quiesce(-beta, -alpha, ply + 1, left - 1)
            undo(m, sv)
            if (v > best) best = v
            if (v > alpha) alpha = v
            if (alpha >= beta) break
        }
        return best
    }

    private fun order(moves: MutableList<Int>, ttm: Int) {
        moves.sortByDescending { m ->
            var s = 0
            if (m == ttm) s += 100000
            if (capOf(m) != NO_CAP) s += 1000
            val t = toOf(m)
            // chặn cối xay của đối thủ
            val opp = 1 - side
            for (mi in MILLS_OF[t]) { val ml = MILLS[mi]; var b = 0; for (p in ml) if (board[p] == opp) b++; if (b == 2) s += 300 }
            s + ADJ[t].size
        }
    }

    private fun searchRoot(lim: SearchLimit, allowed: IntArray?, delta: Int): RootResult {
        val start = System.nanoTime()
        deadline = if (lim.timeMs > 0) start + lim.timeMs * 1_000_000L else Long.MAX_VALUE
        val soft = if (lim.timeMs > 0) start + lim.timeMs * 500_000L else Long.MAX_VALUE
        val nl = if (lim.nodes > 0) lim.nodes else Long.MAX_VALUE
        val maxDepth = if (lim.depth > 0) lim.depth else 40
        nodes = 0; aborted = false
        var moves = ArrayList<Int>(64).also { gen(it) }.toMutableList()
        if (allowed != null) moves = moves.filter { it in allowed }.toMutableList().ifEmpty { allowed.toMutableList() }
        if (moves.isEmpty()) return RootResult(-1, 0, 0, 0)
        if (moves.size == 1) return RootResult(moves[0], 0, 0, 0)
        order(moves, -1)
        var best = moves[0]; var bestScore = 0; var completed = 0
        val scores = HashMap<Int, Int>()
        for (depth in 1..maxDepth) {
            nodeLimit = if (depth == 1) Long.MAX_VALUE else nl
            aborted = false
            var alpha = -INF; var iterBest = -1
            moves.remove(best); moves.add(0, best)
            for (m in moves) {
                val sv = apply(m)
                val t = terminal()
                val v = if (t != null) (if (t > 0) WIN else if (t < 0) -WIN else 0) else -ab(depth - 1, -INF, -alpha, 1)
                undo(m, sv)
                if (aborted) break
                if (v > alpha) { alpha = v; iterBest = m }
            }
            if (aborted) { if (iterBest >= 0 && completed > 0 && alpha > bestScore) best = iterBest; break }
            if (iterBest < 0) break
            best = iterBest; bestScore = alpha; completed = depth
            if (alpha >= WIN - 100 || alpha <= -WIN + 100) break
            if (System.nanoTime() > soft) break
        }
        var near = IntArray(0)
        if (delta > 0 && completed > 0) {
            aborted = false; nodeLimit = Long.MAX_VALUE; deadline = Long.MAX_VALUE
            val thr = bestScore - delta; val out = ArrayList<Int>()
            for (m in moves) {
                if (m == best) continue
                val sv = apply(m)
                val t = terminal()
                val v = if (t != null) (if (t > 0) WIN else if (t < 0) -WIN else 0) else -ab(completed - 1, -thr, -thr + 1, 1)
                undo(m, sv)
                if (v >= thr) out.add(m)
            }
            near = out.toIntArray()
        }
        scores.clear()
        return RootResult(best, bestScore, completed, nodes, near)
    }

    override fun bestMove(timeMs: Long): Int = levelMove(BotLevel.SUPREME, timeMs, Random)

    override fun levelMove(level: BotLevel, timeMs: Long, rnd: Random): Int {
        if (result != Outcome.ONGOING) return -1
        val cfg = BotLevels.config(id, level, timeMs)
        val w = copyGame()
        return Bot.pick(w, cfg, rnd,
            winNow = { Bot.genericWin(w) },
            losesNext = { m -> Bot.genericLosesNext(w, m) },
            search = { lim, allowed, delta -> w.searchRoot(lim, allowed, delta) })
    }
}
