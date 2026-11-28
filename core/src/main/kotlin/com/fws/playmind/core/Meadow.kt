package com.fws.playmind.core

import kotlin.random.Random

/**
 * Lãnh địa đồng cỏ (2-8 người): bàn 30×30 có ô đặc biệt, 96 mảnh gạch cỏ (polyomino 1-6 ô) xếp theo một hàng chờ cố định,
 * công khai. Mỗi người một ô nền 1×1 và một phiếu đổi gạch; 9 vòng, mỗi lượt lấy mảnh kế tiếp (hoặc dùng phiếu để chọn
 * 1 trong 5 mảnh tới) và đặt sao cho chạm cạnh lãnh thổ của mình nhưng không chạm cạnh lãnh thổ đối thủ hay đá.
 * Bao kín 4 cạnh một ô đặc biệt thì chiếm ô đó và nhận hiệu ứng (phiếu, Piece, đặt đá, cướp mảnh).
 * Sau vòng 9, Piece còn lại tự mua ô lẻ 1×1. Điểm = cạnh hình vuông lớn nhất toàn đất của mình trừ 1 (2×2 = 1 ... 8×8 = 7).
 *
 * Mã nước đi (Int):
 * - đặt mảnh: hướng × 900 + ô neo (góc trên trái khung của hướng đó)            [0, 7200)
 * - [DISCARD] bỏ mảnh; [BUY_COUPON] mua phiếu (1 Piece); [USE_COUPON]+k chọn mảnh thứ k (1..4) trong 5 mảnh tới
 * - [REMOVE_STONE]+ô dọn đá (1 Piece); [STONE]+ô đặt đá; [STEAL]+id lấy mảnh của đối thủ; [STEAL_SKIP]; [BASE]+ô đặt ô nền
 */
/** Giai đoạn trong lượt: đặt ô nền, lượt chính, đặt đá, chọn mảnh để cướp, đặt mảnh cướp, hết ván. */
enum class MeadowPhase { BASE, TURN, STONE, STEAL_PICK, STEAL_PLACE, OVER }

class MeadowGame(val players: Int = 8) : BoardGame, LeveledEngine {
    companion object {
        const val N = 30
        const val CELLS = 900
        const val ROUNDS = 9
        const val CHOICE = 5
        const val START_COUPONS = 1
        const val START_PIECES = 2
        /** Piece nhận được khi chiếm ô Piece. */
        const val PIECE_GAIN = 2

        const val SP_NONE = 0; const val SP_COUPON = 1; const val SP_PIECE = 2; const val SP_STONE = 3; const val SP_STEAL = 4

        const val DISCARD = 7200
        const val BUY_COUPON = 7201
        const val USE_COUPON = 7210
        const val REMOVE_STONE = 8000
        const val STONE = 9000
        const val STEAL = 10000
        const val STEAL_SKIP = 10999
        const val BASE = 11000

        private fun sh(vararg xy: Int) = xy
        /** Thư viện hình (cặp x, y). */
        val SHAPES: List<IntArray> = listOf(
            sh(0, 0),                                                     // 0 ô đơn
            sh(0, 0, 1, 0),                                               // 1 domino
            sh(0, 0, 1, 0, 2, 0), sh(0, 0, 1, 0, 0, 1),                   // 2-3 tromino I, L
            sh(0, 0, 1, 0, 2, 0, 3, 0), sh(0, 0, 1, 0, 0, 1, 1, 1),       // 4-5 tetromino I, O
            sh(0, 0, 1, 0, 2, 0, 1, 1), sh(0, 0, 0, 1, 0, 2, 1, 2), sh(1, 0, 2, 0, 0, 1, 1, 1),   // 6-8 T, L, S
            sh(1, 0, 2, 0, 0, 1, 1, 1, 1, 2), sh(0, 0, 1, 0, 2, 0, 3, 0, 4, 0), sh(0, 0, 0, 1, 0, 2, 0, 3, 1, 3), // 9-11 F I L
            sh(0, 0, 0, 1, 0, 2, 1, 2, 1, 3), sh(0, 0, 1, 0, 0, 1, 1, 1, 0, 2), sh(0, 0, 1, 0, 2, 0, 1, 1, 1, 2), // 12-14 N P T
            sh(0, 0, 2, 0, 0, 1, 1, 1, 2, 1), sh(0, 0, 0, 1, 0, 2, 1, 2, 2, 2), sh(0, 0, 0, 1, 1, 1, 1, 2, 2, 2), // 15-17 U V W
            sh(1, 0, 0, 1, 1, 1, 2, 1, 1, 2), sh(0, 1, 1, 0, 1, 1, 1, 2, 1, 3), sh(0, 0, 1, 0, 1, 1, 1, 2, 2, 2), // 18-20 X Y Z
            sh(0, 0, 1, 0, 0, 1, 1, 1, 0, 2, 1, 2), sh(0, 0, 0, 1, 0, 2, 0, 3, 0, 4, 1, 4),                       // 21-22 chữ nhật 2×3, L dài
            sh(0, 0, 1, 0, 2, 0, 1, 1, 1, 2, 1, 3), sh(0, 0, 1, 0, 1, 1, 2, 1, 2, 2, 3, 2),                       // 23-24 T dài, bậc thang
            sh(0, 0, 1, 0, 2, 0, 0, 1, 0, 2, 1, 2), sh(1, 0, 0, 1, 1, 1, 2, 1, 1, 2, 1, 3),                       // 25-26 chữ C, chữ thập có đuôi
        )

        /** Các hướng khác nhau (xoay 90°/180°/270°, lật) của từng hình, chuẩn hóa về góc (0, 0). */
        val ORIENTS: List<List<IntArray>> = SHAPES.map { s ->
            val out = ArrayList<IntArray>(); val seen = HashSet<String>()
            for (f in 0 until 2) for (r in 0 until 4) {
                val pts = ArrayList<Pair<Int, Int>>()
                for (i in 0 until s.size / 2) {
                    var x = s[2 * i]; var y = s[2 * i + 1]
                    if (f == 1) x = -x
                    repeat(r) { val t = x; x = -y; y = t }
                    pts.add(x to y)
                }
                val mx = pts.minOf { it.first }; val my = pts.minOf { it.second }
                val norm = pts.map { (it.first - mx) to (it.second - my) }.sortedWith(compareBy({ it.second }, { it.first }))
                val key = norm.joinToString(";")
                if (seen.add(key)) out.add(norm.flatMap { listOf(it.first, it.second) }.toIntArray())
            }
            out
        }

        fun size(shape: Int) = SHAPES[shape].size / 2

        private fun keyOf(pts: List<Pair<Int, Int>>): String {
            val mx = pts.minOf { it.first }; val my = pts.minOf { it.second }
            return pts.map { (it.first - mx) to (it.second - my) }.sortedWith(compareBy({ it.second }, { it.first })).joinToString(";")
        }
        private fun transformOrient(shape: Int, o: Int, f: (Int, Int) -> Pair<Int, Int>): Int {
            val c = ORIENTS[shape][o]
            val key = keyOf((0 until c.size / 2).map { f(c[2 * it], c[2 * it + 1]) })
            return ORIENTS[shape].indexOfFirst { arr -> keyOf((0 until arr.size / 2).map { arr[2 * it] to arr[2 * it + 1] }) == key }.coerceAtLeast(0)
        }
        /** Hướng có được khi xoay hướng [o] của hình [shape] 90° theo chiều kim đồng hồ. */
        fun rotateOrient(shape: Int, o: Int): Int = transformOrient(shape, o) { x, y -> -y to x }
        /** Hướng có được khi lật ngang hướng [o]. */
        fun flipOrient(shape: Int, o: Int): Int = transformOrient(shape, o) { x, y -> -x to y }
        /** Khung (rộng, cao) của một hướng. */
        fun extent(shape: Int, o: Int): Pair<Int, Int> {
            val c = ORIENTS[shape][o]; var w = 0; var h = 0
            for (i in 0 until c.size / 2) { w = maxOf(w, c[2 * i] + 1); h = maxOf(h, c[2 * i + 1] + 1) }
            return w to h
        }

        /** 96 mảnh: số lượng mỗi hình, theo thứ tự [SHAPES]. */
        private val COUNTS = intArrayOf(4, 6, 4, 6, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 2, 2, 1, 1, 1, 1)
        /** Hàng chờ cố định, công khai (giống nhau mọi ván, mọi máy). */
        val ORDER: IntArray = run {
            val l = ArrayList<Int>(); for ((s, c) in COUNTS.withIndex()) repeat(c) { l.add(s) }
            l.shuffle(Random(20261128)); l.toIntArray()
        }

        /** Ô đặc biệt (đối xứng theo 9 khu 10×10). */
        val SPECIAL_AT: IntArray = IntArray(CELLS).also { a ->
            fun put(t: Int, vararg xy: Int) { for (i in 0 until xy.size / 2) a[xy[2 * i + 1] * N + xy[2 * i]] = t }
            put(SP_COUPON, 4, 4, 14, 4, 25, 4, 5, 15, 15, 15, 24, 15, 4, 25, 14, 25, 25, 25)
            put(SP_STEAL, 9, 4, 20, 4, 4, 9, 25, 9, 4, 20, 25, 20, 9, 25, 20, 25)
            put(SP_STONE, 11, 11, 18, 18)
            put(SP_PIECE, 18, 11, 11, 18)
        }
        val SPECIAL_CELLS: IntArray = (0 until CELLS).filter { SPECIAL_AT[it] != SP_NONE }.toIntArray()

        fun place(orient: Int, cell: Int) = orient * CELLS + cell
        fun isPlace(m: Int) = m in 0 until DISCARD
        fun placeOrient(m: Int) = m / CELLS
        fun placeCell(m: Int) = m % CELLS

        private val DX = intArrayOf(1, -1, 0, 0)
        private val DY = intArrayOf(0, 0, 1, -1)
    }

    /** Một mảnh đã đặt trên bàn. */
    class Inst(val shape: Int, var owner: Int, val cells: IntArray, var alive: Boolean = true)

    override val id = "meadow"
    val owner = IntArray(CELLS) { -1 }
    val stone = BooleanArray(CELLS)
    /** Loại ô đặc biệt còn chưa bị chiếm (SP_NONE sau khi bị chiếm). */
    val special = SPECIAL_AT.copyOf()
    val tileOf = IntArray(CELLS) { -1 }
    val insts = ArrayList<Inst>()
    val queue = ArrayList<Int>(ORDER.toList())
    val coupons = IntArray(players) { START_COUPONS }
    val pieces = IntArray(players) { START_PIECES }
    var round = 0; private set
    var phase = MeadowPhase.BASE; private set
    private var cur = 0
    /** Vị trí trong hàng chờ của mảnh sẽ đặt ở lượt này (0 = mảnh kế tiếp; 1..4 khi đã dùng phiếu). */
    var pickOff = 0; private set
    var couponUsed = false; private set
    var pendingStones = 0; private set
    var pendingSteals = 0; private set
    /** Hình của mảnh vừa cướp, đang chờ đặt (-1 nếu không có). */
    var stealShape = -1; private set
    private val hist = ArrayList<Int>()
    override var result = Outcome.ONGOING; private set
    override var reason = EndReason.NONE; private set
    var winner = -1; private set
    var finalPoints = IntArray(players); private set
    var finalArea = IntArray(players); private set

    override val turn: Int get() = cur
    override val plies: Int get() = hist.size
    override val lastMove: Int get() = if (hist.isEmpty()) -1 else hist[hist.size - 1]
    override val history: List<Int> get() = hist
    override fun inCheck() = false
    val over: Boolean get() = phase == MeadowPhase.OVER

    /** Hình mảnh đang chờ đặt ở lượt này (-1 nếu không có). */
    val currentShape: Int get() = when (phase) {
        MeadowPhase.TURN -> queue.getOrElse(pickOff) { -1 }
        MeadowPhase.STEAL_PLACE -> stealShape
        else -> -1
    }

    override fun reset() {
        java.util.Arrays.fill(owner, -1); java.util.Arrays.fill(stone, false); System.arraycopy(SPECIAL_AT, 0, special, 0, CELLS)
        java.util.Arrays.fill(tileOf, -1); insts.clear(); queue.clear(); queue.addAll(ORDER.toList())
        java.util.Arrays.fill(coupons, START_COUPONS); java.util.Arrays.fill(pieces, START_PIECES)
        round = 0; phase = MeadowPhase.BASE; cur = 0; pickOff = 0; couponUsed = false; pendingStones = 0; pendingSteals = 0; stealShape = -1
        hist.clear(); result = Outcome.ONGOING; reason = EndReason.NONE; winner = -1
        finalPoints = IntArray(players); finalArea = IntArray(players)
    }

    override fun copyGame(): MeadowGame {
        val g = MeadowGame(players)
        System.arraycopy(owner, 0, g.owner, 0, CELLS); System.arraycopy(stone, 0, g.stone, 0, CELLS)
        System.arraycopy(special, 0, g.special, 0, CELLS); System.arraycopy(tileOf, 0, g.tileOf, 0, CELLS)
        for (i in insts) g.insts.add(Inst(i.shape, i.owner, i.cells, i.alive))
        g.queue.clear(); g.queue.addAll(queue)
        System.arraycopy(coupons, 0, g.coupons, 0, players); System.arraycopy(pieces, 0, g.pieces, 0, players)
        g.round = round; g.phase = phase; g.cur = cur; g.pickOff = pickOff; g.couponUsed = couponUsed
        g.pendingStones = pendingStones; g.pendingSteals = pendingSteals; g.stealShape = stealShape
        g.hist.addAll(hist); g.result = result; g.reason = reason; g.winner = winner
        g.finalPoints = finalPoints.copyOf(); g.finalArea = finalArea.copyOf()
        return g
    }

    // ───────────────────────── luật đặt ─────────────────────────

    /** Các ô của hình [shape] hướng [orient] neo ở [cell]; null nếu tràn bàn. */
    fun cellsOf(shape: Int, orient: Int, cell: Int): IntArray? {
        val o = ORIENTS[shape].getOrNull(orient) ?: return null
        val ax = cell % N; val ay = cell / N
        val out = IntArray(o.size / 2)
        for (i in out.indices) {
            val x = ax + o[2 * i]; val y = ay + o[2 * i + 1]
            if (x !in 0 until N || y !in 0 until N) return null
            out[i] = y * N + x
        }
        return out
    }

    /** Ô [c] trống, không phải ô đặc biệt chưa chiếm, không có đá. */
    fun isFree(c: Int) = owner[c] == -1 && special[c] == SP_NONE && !stone[c]

    /** Đặt được các ô [cells] cho người [p]: ô trống, không chạm cạnh đất đối thủ hay đá, chạm cạnh đất của mình. */
    fun canPlace(p: Int, cells: IntArray): Boolean {
        var touch = false
        for (c in cells) if (!isFree(c)) return false
        for (c in cells) {
            val x = c % N; val y = c / N
            for (d in 0 until 4) {
                val nx = x + DX[d]; val ny = y + DY[d]
                if (nx !in 0 until N || ny !in 0 until N) continue
                val nb = ny * N + nx
                if (stone[nb]) return false
                val o = owner[nb]
                if (o >= 0) { if (o != p) return false else touch = true }
            }
        }
        return touch
    }

    /** Mọi nước đặt hợp lệ của hình [shape] cho người [p]. */
    fun placements(p: Int, shape: Int): IntArray {
        if (shape < 0) return IntArray(0)
        // ô biên: ô trống chạm cạnh đất của p; nước đặt hợp lệ phải có ít nhất một ô thuộc biên
        val frontier = ArrayList<Int>()
        for (c in 0 until CELLS) if (isFree(c)) {
            val x = c % N; val y = c / N
            for (d in 0 until 4) {
                val nx = x + DX[d]; val ny = y + DY[d]
                if (nx in 0 until N && ny in 0 until N && owner[ny * N + nx] == p) { frontier.add(c); break }
            }
        }
        val out = ArrayList<Int>(); val seen = HashSet<Int>()
        for ((oi, o) in ORIENTS[shape].withIndex()) {
            for (f in frontier) {
                val fx = f % N; val fy = f / N
                for (i in 0 until o.size / 2) {
                    val ax = fx - o[2 * i]; val ay = fy - o[2 * i + 1]
                    if (ax < 0 || ay < 0) continue
                    val m = place(oi, ay * N + ax)
                    if (!seen.add(m)) continue
                    val cs = cellsOf(shape, oi, ay * N + ax) ?: continue
                    if (canPlace(p, cs)) out.add(m)
                }
            }
        }
        return out.toIntArray()
    }

    fun canPlaceBase(c: Int): Boolean {
        if (!isFree(c)) return false
        val x = c % N; val y = c / N
        for (d in 0 until 4) {
            val nx = x + DX[d]; val ny = y + DY[d]
            if (nx in 0 until N && ny in 0 until N) { val nb = ny * N + nx; if (owner[nb] >= 0 || stone[nb]) return false }
        }
        return true
    }

    fun opponentInsts(p: Int): List<Int> = insts.indices.filter { insts[it].alive && insts[it].owner != p }

    override fun legalMoves(): IntArray {
        val out = ArrayList<Int>()
        when (phase) {
            MeadowPhase.OVER -> {}
            MeadowPhase.BASE -> for (c in 0 until CELLS) if (canPlaceBase(c)) out.add(BASE + c)
            MeadowPhase.TURN -> {
                for (m in placements(cur, currentShape)) out.add(m)
                out.add(DISCARD)
                if (pieces[cur] > 0) out.add(BUY_COUPON)
                if (!couponUsed && coupons[cur] > 0) for (k in 1 until CHOICE) if (k < queue.size) out.add(USE_COUPON + k)
                if (pieces[cur] > 0) for (c in 0 until CELLS) if (stone[c]) out.add(REMOVE_STONE + c)
            }
            MeadowPhase.STONE -> for (c in 0 until CELLS) if (isFree(c)) out.add(STONE + c)
            MeadowPhase.STEAL_PICK -> { for (i in opponentInsts(cur)) out.add(STEAL + i); out.add(STEAL_SKIP) }
            MeadowPhase.STEAL_PLACE -> { for (m in placements(cur, stealShape)) out.add(m); out.add(DISCARD) }
        }
        return out.toIntArray()
    }

    override fun play(move: Int): Boolean {
        if (phase == MeadowPhase.OVER || move < 0) return false
        val ok = when (phase) {
            MeadowPhase.BASE -> {
                val c = move - BASE
                if (c !in 0 until CELLS || !canPlaceBase(c)) false
                else {
                    owner[c] = cur; hist.add(move)
                    if (cur == players - 1) { cur = 0; round = 1; phase = MeadowPhase.TURN } else cur++
                    true
                }
            }
            MeadowPhase.TURN -> playTurn(move)
            MeadowPhase.STONE -> {
                val c = move - STONE
                if (c !in 0 until CELLS || !isFree(c)) false
                else { stone[c] = true; pendingStones--; hist.add(move); continuePending(); true }
            }
            MeadowPhase.STEAL_PICK -> when {
                move == STEAL_SKIP -> { pendingSteals--; hist.add(move); continuePending(); true }
                move - STEAL in insts.indices && insts[move - STEAL].alive && insts[move - STEAL].owner != cur -> {
                    val inst = insts[move - STEAL]
                    inst.alive = false
                    for (c in inst.cells) { owner[c] = -1; tileOf[c] = -1 }
                    stealShape = inst.shape; phase = MeadowPhase.STEAL_PLACE; hist.add(move); true
                }
                else -> false
            }
            MeadowPhase.STEAL_PLACE -> when {
                move == DISCARD -> { stealShape = -1; pendingSteals--; hist.add(move); continuePending(); true }
                isPlace(move) -> {
                    val cs = cellsOf(stealShape, placeOrient(move), placeCell(move))
                    if (cs == null || !canPlace(cur, cs)) false
                    else {
                        hist.add(move); applyTile(cur, stealShape, cs); stealShape = -1; pendingSteals--
                        captureAround(cur); continuePending(); true
                    }
                }
                else -> false
            }
            MeadowPhase.OVER -> false
        }
        return ok
    }

    private fun playTurn(move: Int): Boolean {
        when {
            move == BUY_COUPON -> { if (pieces[cur] <= 0) return false; pieces[cur]--; coupons[cur]++; hist.add(move); return true }
            move in USE_COUPON + 1 until USE_COUPON + CHOICE -> {
                val k = move - USE_COUPON
                if (couponUsed || coupons[cur] <= 0 || k >= queue.size) return false
                coupons[cur]--; couponUsed = true; pickOff = k; hist.add(move); return true
            }
            move in REMOVE_STONE until REMOVE_STONE + CELLS -> {
                val c = move - REMOVE_STONE
                if (!stone[c] || pieces[cur] <= 0) return false
                stone[c] = false; pieces[cur]--; hist.add(move); return true
            }
            move == DISCARD -> { takeTile(); hist.add(move); endTurn(); return true }
            isPlace(move) -> {
                val shape = currentShape; if (shape < 0) return false
                val cs = cellsOf(shape, placeOrient(move), placeCell(move)) ?: return false
                if (!canPlace(cur, cs)) return false
                hist.add(move); takeTile(); applyTile(cur, shape, cs)
                captureAround(cur); continuePending(); return true
            }
        }
        return false
    }

    /** Lấy mảnh ở vị trí [pickOff]: các mảnh bị bỏ qua về cuối hàng chờ (chỉ quay lại khi các mảnh còn lại đã hết). */
    private fun takeTile() {
        if (queue.isEmpty()) return
        val k = pickOff.coerceAtMost(queue.size - 1)
        val skipped = ArrayList(queue.subList(0, k))
        queue.subList(0, k + 1).clear()
        queue.addAll(skipped)
    }

    private fun applyTile(p: Int, shape: Int, cs: IntArray) {
        val id = insts.size
        insts.add(Inst(shape, p, cs))
        for (c in cs) { owner[c] = p; tileOf[c] = id }
    }

    /** Chiếm các ô đặc biệt đã bị [p] bao kín 4 cạnh; trả về số ô chiếm được. */
    private fun captureAround(p: Int, autoPhase: Boolean = false): Int {
        var n = 0
        for (s in SPECIAL_CELLS) {
            if (special[s] == SP_NONE) continue
            val x = s % N; val y = s / N
            var all = true
            for (d in 0 until 4) {
                val nx = x + DX[d]; val ny = y + DY[d]
                if (nx !in 0 until N || ny !in 0 until N || owner[ny * N + nx] != p) { all = false; break }
            }
            if (!all) continue
            val t = special[s]; special[s] = SP_NONE; owner[s] = p; n++
            when (t) {
                SP_COUPON -> coupons[p]++
                SP_PIECE -> pieces[p] += PIECE_GAIN
                SP_STONE -> if (!autoPhase) pendingStones++
                SP_STEAL -> if (!autoPhase) pendingSteals++
            }
        }
        return n
    }

    private fun continuePending() {
        if (pendingStones > 0 && (0 until CELLS).any { isFree(it) }) { phase = MeadowPhase.STONE; return }
        pendingStones = 0
        if (pendingSteals > 0 && opponentInsts(cur).isNotEmpty()) { phase = MeadowPhase.STEAL_PICK; return }
        pendingSteals = 0
        endTurn()
    }

    private fun endTurn() {
        phase = MeadowPhase.TURN; pickOff = 0; couponUsed = false; stealShape = -1
        if (cur == players - 1) {
            cur = 0; round++
            if (round > ROUNDS) { autoBuy(); finish() }
        } else cur++
    }

    /** Sau vòng 9: lần lượt từng người dùng Piece còn lại mua ô lẻ 1×1 (chọn ô làm hình vuông lớn nhất tăng nhiều nhất). */
    private fun autoBuy() {
        var progress = true
        while (progress) {
            progress = false
            for (p in 0 until players) {
                if (pieces[p] <= 0) continue
                var best = -1; var bestV = Int.MIN_VALUE
                for (c in 0 until CELLS) {
                    if (!canPlace(p, intArrayOf(c))) continue
                    owner[c] = p
                    val v = largestSquare(p) * 1000 + ownNeighbors(p, c) * 10 - (c % 7)
                    owner[c] = -1
                    if (v > bestV) { bestV = v; best = c }
                }
                if (best < 0) continue
                owner[best] = p; pieces[p]--; progress = true
                captureAround(p, autoPhase = true)
            }
        }
    }

    private fun ownNeighbors(p: Int, c: Int): Int {
        val x = c % N; val y = c / N; var n = 0
        for (d in 0 until 4) { val nx = x + DX[d]; val ny = y + DY[d]; if (nx in 0 until N && ny in 0 until N && owner[ny * N + nx] == p) n++ }
        return n
    }

    /** Cạnh hình vuông lớn nhất toàn đất của [p]. */
    fun largestSquare(p: Int): Int {
        val dp = IntArray(CELLS); var best = 0
        for (y in 0 until N) for (x in 0 until N) {
            val c = y * N + x
            if (owner[c] != p) continue
            val v = if (x == 0 || y == 0) 1 else 1 + minOf(dp[c - 1], dp[c - N], dp[c - N - 1])
            dp[c] = v; if (v > best) best = v
        }
        return best
    }

    /** Điểm hiện tại: 2×2 = 1, 3×3 = 2, ... */
    fun points(p: Int): Int = (largestSquare(p) - 1).coerceAtLeast(0)
    fun area(p: Int): Int = owner.count { it == p }

    private fun finish() {
        phase = MeadowPhase.OVER; reason = EndReason.TERRITORY
        finalPoints = IntArray(players) { points(it) }; finalArea = IntArray(players) { area(it) }
        var best = 0
        for (p in 1 until players) if (finalPoints[p] > finalPoints[best] || (finalPoints[p] == finalPoints[best] && finalArea[p] > finalArea[best])) best = p
        val tied = (0 until players).filter { finalPoints[it] == finalPoints[best] && finalArea[it] == finalArea[best] }
        if (tied.size > 1) { winner = -1; result = if (0 in tied) Outcome.DRAW else Outcome.WIN_SECOND }
        else { winner = best; result = if (best == 0) Outcome.WIN_FIRST else Outcome.WIN_SECOND }
    }

    override fun forfeit(loser: Int, reason: EndReason) {
        if (phase == MeadowPhase.OVER) return
        phase = MeadowPhase.OVER; this.reason = reason
        result = if (loser == 0) Outcome.WIN_SECOND else Outcome.WIN_FIRST
        winner = if (players == 2) 1 - loser else -1
    }

    // ───────────────────────── bot 7 cấp ─────────────────────────

    override fun bestMove(timeMs: Long): Int = levelMove(BotLevel.SUPREME, timeMs, Random)

    override fun levelMove(level: BotLevel, timeMs: Long, rnd: Random): Int {
        if (phase == MeadowPhase.OVER) return -1
        val cfg = BotLevels.config(id, level, timeMs)
        return MeadowBot(copyGame(), level.ordinal, cfg.noiseP, rnd).move()
    }
}

/**
 * Bot Lãnh địa đồng cỏ: đánh giá một nước đặt bằng điểm hình vuông thật, "tiềm năng hình vuông" (cửa sổ k×k không vướng
 * đất đối thủ, đá hay ô sát đất đối thủ; tiềm năng = (k-1) × độ lấp đầy²) và ô đặc biệt chiếm được.
 * Cấp: Dễ/TB/Khá đi ngẫu nhiên với xác suất [noise]; từ Cao thủ biết dùng phiếu đổi gạch; từ Thách đấu cản người dẫn đầu,
 * đặt đá và cướp mảnh có tính toán; Siêu việt nhìn thêm mảnh kế tiếp của mình trong hàng chờ.
 */
internal class MeadowBot(private val g: MeadowGame, private val lv: Int, private val noise: Double, private val rnd: Random) {
    private val me = g.turn
    private val N = MeadowGame.N

    fun move(): Int = when (g.phase) {
        MeadowPhase.BASE -> baseMove()
        MeadowPhase.TURN -> turnMove()
        MeadowPhase.STONE -> stoneMove()
        MeadowPhase.STEAL_PICK -> stealPick()
        MeadowPhase.STEAL_PLACE -> placeBest(g.stealShape, allowRandom = true) ?: MeadowGame.DISCARD
        else -> -1
    }

    private fun random() = noise > 0 && rnd.nextDouble() < noise

    // ── ô nền: nhiều chỗ trống quanh, xa người khác, gần ô đặc biệt
    private fun baseMove(): Int {
        val cand = (0 until MeadowGame.CELLS).filter { g.canPlaceBase(it) }
        if (cand.isEmpty()) return -1
        if (random() || lv == 0) return MeadowGame.BASE + cand[rnd.nextInt(cand.size)]
        var best = cand[0]; var bv = Double.NEGATIVE_INFINITY
        for (c in cand) {
            val x = c % N; val y = c / N
            var v = 0.0
            for (yy in y - 4..y + 4) for (xx in x - 4..x + 4) {
                if (xx !in 0 until N || yy !in 0 until N) { v -= 1.0; continue }
                val cc = yy * N + xx
                if (g.owner[cc] >= 0) v -= 6.0 else if (g.special[cc] != MeadowGame.SP_NONE) v += 2.5
            }
            v += rnd.nextDouble() * 3
            if (v > bv) { bv = v; best = c }
        }
        return MeadowGame.BASE + best
    }

    // ── lượt chính
    private fun turnMove(): Int {
        val shape = g.currentShape
        val base = placementsScored(shape)
        if (random() && base.isNotEmpty()) return base[rnd.nextInt(base.size)].first
        var bestMove = base.maxByOrNull { it.second }
        // Cao thủ trở lên: dùng phiếu nếu một trong 4 mảnh tới tốt hơn rõ
        if (lv >= 4 && !g.couponUsed && g.coupons[me] > 0) {
            var bestK = -1; var bestV = (bestMove?.second ?: -100.0) + 0.8
            for (k in 1 until MeadowGame.CHOICE) {
                val s = g.queue.getOrNull(k) ?: break
                val v = placementsScored(s).maxOfOrNull { it.second } ?: continue
                if (v > bestV) { bestV = v; bestK = k }
            }
            if (bestK > 0) return MeadowGame.USE_COUPON + bestK
        }
        if (bestMove == null) {
            // không đặt được: thử dùng phiếu cho mảnh khác (mọi cấp từ Khá)
            if (lv >= 2 && !g.couponUsed && g.coupons[me] > 0) {
                for (k in 1 until MeadowGame.CHOICE) {
                    val s = g.queue.getOrNull(k) ?: break
                    if (g.placements(me, s).isNotEmpty()) return MeadowGame.USE_COUPON + k
                }
            }
            return MeadowGame.DISCARD
        }
        // Siêu việt: cộng giá trị mảnh kế tiếp của mình (ước lượng vị trí trong hàng chờ sau một vòng)
        if (lv >= 6 && base.size > 1) {
            val next = g.queue.getOrNull(g.pickOff + g.players)
            if (next != null) {
                var bv = Double.NEGATIVE_INFINITY
                for ((m, v) in base.sortedByDescending { it.second }.take(6)) {
                    val cs = g.cellsOf(shape, MeadowGame.placeOrient(m), MeadowGame.placeCell(m)) ?: continue
                    for (c in cs) g.owner[c] = me
                    val v2 = g.placements(me, next).maxOfOrNull { evalAfter(next, it) } ?: v
                    for (c in cs) g.owner[c] = -1
                    val tot = v + .6 * (v2 - v).coerceAtLeast(0.0)
                    if (tot > bv) { bv = tot; bestMove = m to v }
                }
            }
        }
        return bestMove!!.first
    }

    private fun placeBest(shape: Int, allowRandom: Boolean): Int? {
        val l = placementsScored(shape)
        if (l.isEmpty()) return null
        if (allowRandom && random()) return l[rnd.nextInt(l.size)].first
        return l.maxByOrNull { it.second }!!.first
    }

    private fun placementsScored(shape: Int): List<Pair<Int, Double>> {
        if (shape < 0) return emptyList()
        val ms = g.placements(me, shape)
        val leader = if (lv >= 5) leader() else -1
        val leadBefore = if (leader >= 0) potential(leader) else 0.0
        return ms.map { m -> m to (evalAfter(shape, m) + rnd.nextDouble() * 1e-3 - interference(shape, m, leader, leadBefore)) }
    }

    /** Giá trị sau khi đặt nước [m] của hình [shape] (đặt thử rồi hoàn lại). */
    private fun evalAfter(shape: Int, m: Int): Double {
        val cs = g.cellsOf(shape, MeadowGame.placeOrient(m), MeadowGame.placeCell(m)) ?: return -1e9
        for (c in cs) g.owner[c] = me
        var v = 4.0 * g.points(me) + potential(me)
        if (lv >= 2) v += specialBonus()
        for (c in cs) g.owner[c] = -1
        return v
    }

    /** Thưởng ô đặc biệt: bao kín được ngay, hoặc đã có 3 cạnh. */
    private fun specialBonus(): Double {
        var b = 0.0
        for (s in MeadowGame.SPECIAL_CELLS) {
            val t = g.special[s]; if (t == MeadowGame.SP_NONE) continue
            val x = s % N; val y = s / N; var k = 0; var blocked = false
            for ((dx, dy) in arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                val nx = x + dx; val ny = y + dy
                if (nx !in 0 until N || ny !in 0 until N) { blocked = true; continue }
                val o = g.owner[ny * N + nx]
                if (o == me) k++ else if (o >= 0 || g.stone[ny * N + nx]) blocked = true
            }
            if (blocked) continue
            val w = when (t) { MeadowGame.SP_PIECE -> .8; MeadowGame.SP_STEAL -> .8; MeadowGame.SP_COUPON -> .5; else -> .3 }
            b += when (k) { 4 -> w; 3 -> w * .35; 2 -> w * .1; else -> 0.0 }
        }
        return b
    }

    /** Tiềm năng hình vuông của [p]: max (k-1)·lấp² trên các cửa sổ k×k (k = 2..8) không vướng ô cấm. */
    private fun potential(p: Int): Double {
        val W = N + 1
        val own = IntArray(W * W); val bad = IntArray(W * W)
        for (y in 0 until N) for (x in 0 until N) {
            val c = y * N + x
            val o = g.owner[c]
            val isOwn = if (o == p) 1 else 0
            var isBad = if ((o >= 0 && o != p) || g.stone[c]) 1 else 0
            if (isBad == 0 && o == -1) {
                // ô trống sát đất đối thủ hoặc đá: p không đặt được vào
                for ((dx, dy) in arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                    val nx = x + dx; val ny = y + dy
                    if (nx !in 0 until N || ny !in 0 until N) continue
                    val nb = ny * N + nx; val oo = g.owner[nb]
                    if ((oo >= 0 && oo != p) || g.stone[nb]) { isBad = 1; break }
                }
            }
            val i = (y + 1) * W + (x + 1)
            own[i] = isOwn + own[i - 1] + own[i - W] - own[i - W - 1]
            bad[i] = isBad + bad[i - 1] + bad[i - W] - bad[i - W - 1]
        }
        val maxK = if (lv <= 1) 4 else 8
        var best = 0.0
        for (k in 2..maxK) {
            val area = (k * k).toDouble()
            for (y in 0..N - k) for (x in 0..N - k) {
                val a = y * W + x; val b = y * W + x + k; val c = (y + k) * W + x; val d = (y + k) * W + x + k
                if (bad[d] - bad[b] - bad[c] + bad[a] != 0) continue
                val o = own[d] - own[b] - own[c] + own[a]
                if (o == 0) continue
                val f = o / area
                val v = (k - 1) * f * f
                if (v > best) best = v
            }
        }
        return best
    }

    private fun leader(): Int {
        var best = -1; var bv = -1.0
        for (p in 0 until g.players) if (p != me) {
            val v = 4.0 * g.points(p) + potential(p)
            if (v > bv) { bv = v; best = p }
        }
        return best
    }

    /** Thách đấu trở lên: nước làm giảm tiềm năng của người dẫn đầu được cộng điểm (trả về số âm = có lợi). */
    private fun interference(shape: Int, m: Int, leader: Int, before: Double): Double {
        if (leader < 0) return 0.0
        val cs = g.cellsOf(shape, MeadowGame.placeOrient(m), MeadowGame.placeCell(m)) ?: return 0.0
        for (c in cs) g.owner[c] = me
        val after = potential(leader)
        for (c in cs) g.owner[c] = -1
        return -.25 * (before - after)
    }

    // ── đặt đá: cấp cao chặn cửa sổ tốt nhất của người dẫn đầu; cấp thấp đặt xa đất mình
    private fun stoneMove(): Int {
        val free = (0 until MeadowGame.CELLS).filter { g.isFree(it) }
        if (free.isEmpty()) return -1
        if (lv >= 5) {
            val l = leader()
            if (l >= 0) {
                val before = potential(l)
                var best = -1; var bv = 0.0
                // chỉ thử ô gần đất người dẫn đầu (bán kính 3) cho nhanh
                for (c in free) {
                    if (!near(c, l, 3) || near(c, me, 2)) continue
                    g.stone[c] = true
                    val drop = before - potential(l)
                    g.stone[c] = false
                    if (drop > bv) { bv = drop; best = c }
                }
                if (best >= 0) return MeadowGame.STONE + best
            }
        }
        val far = free.filter { !near(it, me, 3) }
        val pool = far.ifEmpty { free }
        return MeadowGame.STONE + pool[rnd.nextInt(pool.size)]
    }

    private fun near(c: Int, p: Int, r: Int): Boolean {
        val x = c % N; val y = c / N
        for (yy in y - r..y + r) for (xx in x - r..x + r) if (xx in 0 until N && yy in 0 until N && g.owner[yy * N + xx] == p) return true
        return false
    }

    // ── cướp mảnh: cấp Khá trở lên chọn mảnh đặt lại được tốt nhất cho mình (ưu tiên mảnh của người dẫn đầu)
    private fun stealPick(): Int {
        val opts = g.opponentInsts(me)
        if (opts.isEmpty()) return MeadowGame.STEAL_SKIP
        if (lv < 2 || random()) return MeadowGame.STEAL + opts[rnd.nextInt(opts.size)]
        val leader = leader()
        var best = opts[0]; var bv = Double.NEGATIVE_INFINITY
        for (i in opts) {
            val inst = g.insts[i]
            for (c in inst.cells) g.owner[c] = -1
            val v = (g.placements(me, inst.shape).maxOfOrNull { evalAfter(inst.shape, it) } ?: 0.0) +
                inst.cells.size * .05 + (if (inst.owner == leader) .5 else 0.0)
            for (c in inst.cells) g.owner[c] = inst.owner
            if (v > bv) { bv = v; best = i }
        }
        return MeadowGame.STEAL + best
    }
}
