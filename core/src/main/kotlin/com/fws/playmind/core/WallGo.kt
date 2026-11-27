package com.fws.playmind.core

import kotlin.random.Random

/** Kết quả phân tích bàn: chia bàn thành các "vùng" bị tường ngăn cách. */
internal class WgAnalysis(val region: IntArray, val size: IntArray, val mask: IntArray, val count: Int)

/**
 * Trạng thái thuần của bàn Wall Go 7x7 (quân + tường), không có lượt/lịch sử. Bot dùng lớp này để thử nước đi nhanh.
 * Ô đánh số 0..48 (hàng * 7 + cột). Tường lưu theo bitmask từng ô: bit0 = trên, bit1 = phải, bit2 = dưới, bit3 = trái.
 * Mép bàn luôn là tường vĩnh viễn nên không lưu.
 */
internal class WgBoard(val players: Int) {
    val owner = IntArray(WallGoGame.CELLS) { -1 }
    val walls = IntArray(WallGoGame.CELLS)

    fun copy(): WgBoard { val b = WgBoard(players); System.arraycopy(owner, 0, b.owner, 0, owner.size); System.arraycopy(walls, 0, b.walls, 0, walls.size); return b }

    fun neighbor(c: Int, side: Int): Int {
        val r = c / 7 + WallGoGame.DR[side]; val col = c % 7 + WallGoGame.DC[side]
        return if (r < 0 || r > 6 || col < 0 || col > 6) -1 else r * 7 + col
    }

    fun open(c: Int, side: Int): Boolean = (walls[c] and (1 shl side)) == 0 && neighbor(c, side) >= 0

    /** Bitmask các cạnh của ô [c] còn đặt được tường (chưa có tường, không phải mép bàn). */
    fun wallSides(c: Int): Int { var m = 0; for (s in 0 until 4) if (open(c, s)) m = m or (1 shl s); return m }

    fun addWall(c: Int, side: Int) {
        walls[c] = walls[c] or (1 shl side)
        val n = neighbor(c, side)
        if (n >= 0) walls[n] = walls[n] or (1 shl ((side + 2) % 4))
    }

    /** Các ô quân ở [from] có thể tới trong tối đa 2 bước (gồm chính [from] = đứng yên). Không xuyên tường, không xuyên/đứng lên quân khác. */
    fun reach(from: Int): IntArray {
        val out = ArrayList<Int>(13); out.add(from)
        val seen = BooleanArray(WallGoGame.CELLS); seen[from] = true
        for (s1 in 0 until 4) {
            if (!open(from, s1)) continue
            val n1 = neighbor(from, s1)
            if (owner[n1] != -1 || seen[n1]) continue
            seen[n1] = true; out.add(n1)
            for (s2 in 0 until 4) {
                if (!open(n1, s2)) continue
                val n2 = neighbor(n1, s2)
                if (owner[n2] != -1 || seen[n2]) continue
                seen[n2] = true; out.add(n2)
            }
        }
        return out.toIntArray()
    }

    fun hasMove(p: Int): Boolean {
        for (c in 0 until WallGoGame.CELLS) if (owner[c] == p) for (d in reach(c)) if (wallSides(d) != 0) return true
        return false
    }

    fun analyze(): WgAnalysis {
        val region = IntArray(WallGoGame.CELLS) { -1 }
        val size = IntArray(WallGoGame.CELLS); val mask = IntArray(WallGoGame.CELLS)
        val stack = IntArray(WallGoGame.CELLS)
        var cnt = 0
        for (s in 0 until WallGoGame.CELLS) {
            if (region[s] >= 0) continue
            var sp = 0; stack[sp++] = s; region[s] = cnt
            var sz = 0; var mk = 0
            while (sp > 0) {
                val c = stack[--sp]; sz++
                if (owner[c] >= 0) mk = mk or (1 shl owner[c])
                for (side in 0 until 4) {
                    if (!open(c, side)) continue
                    val n = neighbor(c, side)
                    if (region[n] < 0) { region[n] = cnt; stack[sp++] = n }
                }
            }
            size[cnt] = sz; mask[cnt] = mk; cnt++
        }
        return WgAnalysis(region, size, mask, cnt)
    }

    /** Ván kết thúc khi không còn vùng nào chứa quân của từ hai người chơi trở lên. */
    fun finished(a: WgAnalysis = analyze()): Boolean {
        for (r in 0 until a.count) if (Integer.bitCount(a.mask[r]) >= 2) return false
        return true
    }

    /** Lãnh thổ chắc chắn của từng người: tổng số ô của các vùng chỉ chứa quân của họ. */
    fun territory(a: WgAnalysis = analyze()): IntArray {
        val t = IntArray(players)
        for (r in 0 until a.count) if (Integer.bitCount(a.mask[r]) == 1) t[Integer.numberOfTrailingZeros(a.mask[r])] += a.size[r]
        return t
    }

    fun largest(a: WgAnalysis = analyze()): IntArray {
        val t = IntArray(players)
        for (r in 0 until a.count) if (Integer.bitCount(a.mask[r]) == 1) { val p = Integer.numberOfTrailingZeros(a.mask[r]); if (a.size[r] > t[p]) t[p] = a.size[r] }
        return t
    }

    /**
     * Điểm ước lượng cho bot: lãnh thổ chắc chắn + phần vùng tranh chấp mà mỗi người tới được sớm hơn
     * (số lượt đi = ceil(số bước / 2), hòa thì chia đều).
     */
    fun scoreVector(): DoubleArray {
        val a = analyze()
        val sc = DoubleArray(players)
        var mixed = false
        for (r in 0 until a.count) {
            val bc = Integer.bitCount(a.mask[r])
            if (bc == 1) sc[Integer.numberOfTrailingZeros(a.mask[r])] += a.size[r].toDouble() else if (bc >= 2) mixed = true
        }
        if (!mixed) return sc
        val inf = 99
        val dist = Array(players) { IntArray(WallGoGame.CELLS) { inf } }
        val queue = IntArray(WallGoGame.CELLS)
        for (p in 0 until players) {
            var head = 0; var tail = 0
            for (c in 0 until WallGoGame.CELLS) if (owner[c] == p) { dist[p][c] = 0; queue[tail++] = c }
            while (head < tail) {
                val c = queue[head++]
                for (s in 0 until 4) {
                    if (!open(c, s)) continue
                    val n = neighbor(c, s)
                    if (dist[p][n] == inf) { dist[p][n] = dist[p][c] + 1; queue[tail++] = n }
                }
            }
        }
        for (c in 0 until WallGoGame.CELLS) {
            if (Integer.bitCount(a.mask[a.region[c]]) < 2) continue
            var best = inf; var ties = 0
            for (p in 0 until players) { val t = if (dist[p][c] >= inf) inf else (dist[p][c] + 1) / 2; if (t < best) { best = t; ties = 1 } else if (t == best) ties++ }
            if (best >= inf) continue
            for (p in 0 until players) { val t = if (dist[p][c] >= inf) inf else (dist[p][c] + 1) / 2; if (t == best) sc[p] += 0.85 / ties }
        }
        return sc
    }

    /** Áp dụng nước đi (đã hợp lệ). Trả về người vừa đi. */
    fun applyPlay(from: Int, to: Int, side: Int) {
        val p = owner[from]
        owner[from] = -1; owner[to] = p
        addWall(to, side)
    }
}

/**
 * Wall Go (trò chơi trong The Devil's Plan) cho 2-4 người trên bàn 7x7.
 *
 *  - Giai đoạn đặt quân: lần lượt đặt quân lên ô trống. 2 người: mỗi người 4 quân, thứ tự A-B-B-A-A-B-B-A. 3 người: mỗi người 3 quân,
 *    4 người: mỗi người 2 quân (thứ tự "rắn": 1-2-3-4-4-3-2-1...). Hai mức 3 và 4 người là quy ước của app.
 *  - Giai đoạn chơi: mỗi lượt chọn một quân của mình, đi 0, 1 hoặc 2 ô vuông góc (không xuyên tường/quân khác), rồi BẮT BUỘC đặt
 *    một tường lên một trong bốn cạnh của ô quân vừa đứng (cạnh chưa có tường, không phải mép bàn).
 *  - Kết thúc khi mọi vùng bị tường ngăn chỉ còn quân của một người. Lãnh thổ = số ô của các vùng chỉ chứa quân của mình.
 *    Nhiều lãnh thổ nhất thắng; bằng nhau thì xét vùng đơn lớn nhất; vẫn bằng thì hòa.
 *  - Người không còn nước đi hợp lệ bị bỏ lượt.
 *
 * Nước đi mã hóa thành Int: đặt quân = số ô (0..48); đi + đặt tường = [mvMove].
 * Với [BoardGame]: bên 0 là "người chơi chính"; với 3-4 người, [result] được nhìn từ phía bên 0.
 */
class WallGoGame(val players: Int = 2) : BoardGame, LeveledEngine {
    companion object {
        const val N = 7
        const val CELLS = 49
        const val UP = 0; const val RIGHT = 1; const val DOWN = 2; const val LEFT = 3
        internal val DR = intArrayOf(-1, 0, 1, 0)
        internal val DC = intArrayOf(0, 1, 0, -1)
        private const val PLAY_FLAG = 1 shl 14
        private const val WIN = 10_000_000
        private const val INF = 100_000_000

        fun stonesPerPlayer(players: Int) = when (players) { 2 -> 4; 3 -> 3; else -> 2 }

        fun setupOrder(players: Int): IntArray {
            if (players == 2) return intArrayOf(0, 1, 1, 0, 0, 1, 1, 0)
            val out = ArrayList<Int>()
            for (round in 0 until stonesPerPlayer(players)) {
                if (round % 2 == 0) for (p in 0 until players) out.add(p) else for (p in players - 1 downTo 0) out.add(p)
            }
            return out.toIntArray()
        }

        fun mvPlace(cell: Int) = cell
        fun mvMove(from: Int, to: Int, side: Int) = PLAY_FLAG or from or (to shl 6) or (side shl 12)
        fun isPlay(m: Int) = m >= 0 && (m and PLAY_FLAG) != 0
        fun mvFrom(m: Int) = m and 63
        fun mvTo(m: Int) = (m shr 6) and 63
        fun mvSide(m: Int) = (m shr 12) and 3
        fun mvCell(m: Int) = m and 63
    }

    init { require(players in 2..4) }

    override val id = "wallgo"
    private val order = setupOrder(players)
    internal val board = WgBoard(players)
    private val hist = ArrayList<Int>()
    /** Người đặt từng bức tường: wallOwner[ô * 4 + cạnh] (-1 = chưa có), để vẽ tường theo màu người chơi. */
    internal val wallOwner = IntArray(CELLS * 4) { -1 }
    private var placed = 0
    private var cur = 0
    private var over = false
    override var result = Outcome.ONGOING; private set
    override var reason = EndReason.NONE; private set

    /** Người thắng (0..players-1), -1 nếu hòa hoặc chưa kết thúc. */
    var winner = -1; private set
    /** Điểm cuối cùng của từng người (chỉ có nghĩa khi ván đã kết thúc bằng lãnh thổ). */
    var finalScores = IntArray(players); private set
    var finalLargest = IntArray(players); private set

    override val history: List<Int> get() = hist
    override val turn: Int get() = cur
    override val plies: Int get() = hist.size
    override val lastMove: Int get() = if (hist.isEmpty()) -1 else hist[hist.size - 1]
    override fun inCheck() = false

    val inSetup: Boolean get() = placed < order.size
    val stonesTotal: Int get() = order.size
    val stonesPlaced: Int get() = placed

    fun ownerAt(cell: Int) = board.owner[cell]
    fun wallsAt(cell: Int) = board.walls[cell]
    /** Người đã đặt tường ở cạnh [side] của ô [cell], -1 nếu chưa có. */
    fun wallOwnerAt(cell: Int, side: Int) = wallOwner[cell * 4 + side]
    fun reach(from: Int): IntArray = if (board.owner[from] == cur && !inSetup && !over) board.reach(from) else IntArray(0)
    fun wallSides(cell: Int): Int = board.wallSides(cell)
    fun stonesOf(p: Int): IntArray = (0 until CELLS).filter { board.owner[it] == p }.toIntArray()
    /** Chủ vùng của từng ô nếu vùng chỉ chứa quân một người, ngược lại -1 (để tô màu lãnh thổ). */
    fun regionOwners(): IntArray {
        val a = board.analyze(); val out = IntArray(CELLS) { -1 }
        for (c in 0 until CELLS) { val m = a.mask[a.region[c]]; if (Integer.bitCount(m) == 1) out[c] = Integer.numberOfTrailingZeros(m) }
        return out
    }
    /** Lãnh thổ hiện tại (chỉ các vùng đã khép kín cho một người). */
    fun territory(): IntArray = board.territory()

    override fun reset() {
        java.util.Arrays.fill(board.owner, -1); java.util.Arrays.fill(board.walls, 0)
        java.util.Arrays.fill(wallOwner, -1)
        hist.clear(); placed = 0; cur = order[0]; over = false; result = Outcome.ONGOING; reason = EndReason.NONE
        winner = -1; finalScores = IntArray(players); finalLargest = IntArray(players)
    }

    init { cur = order[0] }

    override fun legalMoves(): IntArray {
        if (over) return IntArray(0)
        if (inSetup) return (0 until CELLS).filter { board.owner[it] == -1 }.toIntArray()
        val out = ArrayList<Int>()
        for (c in 0 until CELLS) {
            if (board.owner[c] != cur) continue
            for (d in board.reach(c)) { val ws = board.wallSides(d); for (s in 0 until 4) if ((ws shr s) and 1 == 1) out.add(mvMove(c, d, s)) }
        }
        return out.toIntArray()
    }

    override fun play(move: Int): Boolean {
        if (over || move < 0) return false
        if (inSetup) {
            if (move !in 0 until CELLS || board.owner[move] != -1) return false
            board.owner[move] = cur; placed++; hist.add(move)
            cur = if (inSetup) order[placed] else 0
            if (!inSetup) settleTurn(0)
            return true
        }
        if (!isPlay(move)) return false
        val from = mvFrom(move); val to = mvTo(move); val side = mvSide(move)
        if (from !in 0 until CELLS || to !in 0 until CELLS || move != mvMove(from, to, side)) return false
        if (board.owner[from] != cur) return false
        if (to != from && to !in board.reach(from)) return false
        if (((board.wallSides(to) shr side) and 1) == 0) return false
        board.applyPlay(from, to, side)
        wallOwner[to * 4 + side] = cur
        val nb = board.neighbor(to, side); if (nb >= 0) wallOwner[nb * 4 + (side + 2) % 4] = cur
        hist.add(move)
        settleTurn(cur + 1)
        return true
    }

    /** Kiểm tra kết thúc rồi chuyển lượt cho người kế tiếp còn nước đi (bắt đầu tìm từ [start]). */
    private fun settleTurn(start: Int) {
        val a = board.analyze()
        if (board.finished(a)) { finish(a); return }
        for (k in 0 until players) {
            val p = (start + k) % players
            if (board.hasMove(p)) { cur = p; return }
        }
        finish(a)   // không ai đi được nữa: chốt điểm hiện tại
    }

    private fun finish(a: WgAnalysis) {
        over = true; reason = EndReason.TERRITORY
        finalScores = board.territory(a); finalLargest = board.largest(a)
        var best = 0
        for (p in 1 until players) {
            if (finalScores[p] > finalScores[best] || (finalScores[p] == finalScores[best] && finalLargest[p] > finalLargest[best])) best = p
        }
        val tied = (0 until players).filter { finalScores[it] == finalScores[best] && finalLargest[it] == finalLargest[best] }
        if (tied.size > 1) {
            winner = -1
            result = if (0 in tied) Outcome.DRAW else Outcome.WIN_SECOND
        } else {
            winner = best
            result = if (best == 0) Outcome.WIN_FIRST else Outcome.WIN_SECOND
        }
    }

    /** Người thắng sau [forfeit] (nếu có). */
    override fun forfeit(loser: Int, reason: EndReason) {
        if (over) return
        over = true; this.reason = reason
        result = if (loser == 0) Outcome.WIN_SECOND else Outcome.WIN_FIRST
        winner = if (players == 2) 1 - loser else -1
    }

    override fun copyGame(): WallGoGame {
        val g = WallGoGame(players)
        System.arraycopy(board.owner, 0, g.board.owner, 0, CELLS); System.arraycopy(board.walls, 0, g.board.walls, 0, CELLS)
        System.arraycopy(wallOwner, 0, g.wallOwner, 0, wallOwner.size)
        g.hist.addAll(hist); g.placed = placed; g.cur = cur; g.over = over; g.result = result; g.reason = reason
        g.winner = winner; g.finalScores = finalScores.copyOf(); g.finalLargest = finalLargest.copyOf()
        return g
    }

    // ═════════════════════════ AI: Siêu việt (thời gian) và 6 cấp giảm sức (độ sâu / số nút) ═════════════════════════
    private fun heur(sc: DoubleArray, me: Int): Double {
        if (players == 2) return sc[me] - sc[1 - me]
        var sum = 0.0; var mx = -1e9
        for (p in 0 until players) if (p != me) { sum += sc[p]; if (sc[p] > mx) mx = sc[p] }
        return sc[me] - 0.5 * mx - 0.5 * sum / (players - 1)
    }

    private var deadline = Long.MAX_VALUE
    private var nodeLimit = Long.MAX_VALUE
    private var nodes = 0L
    private var aborted = false

    private fun checkStop() {
        if ((++nodes and 31L) == 0L && (nodes >= nodeLimit || System.nanoTime() > deadline)) aborted = true
    }

    /** Điểm cuối ván cho [me]: thắng/thua rõ ràng cộng chênh lệch lãnh thổ (đơn vị: 100 = 1 ô). */
    private fun terminal(b: WgBoard, a: WgAnalysis, me: Int, ply: Int): Int {
        val t = b.territory(a); val lg = b.largest(a)
        var bestOther = -1; var bestLg = -1
        for (p in 0 until players) if (p != me && (t[p] > bestOther || (t[p] == bestOther && lg[p] > bestLg))) { bestOther = t[p]; bestLg = lg[p] }
        val diff = t[me] - bestOther
        val win = diff > 0 || (diff == 0 && lg[me] > bestLg)
        val draw = diff == 0 && lg[me] == bestLg
        return (if (draw) 0 else if (win) WIN - ply else -(WIN - ply)) + diff * 100
    }

    private fun evalBoard(b: WgBoard, me: Int): Int = (heur(b.scoreVector(), me) * 100).toInt()

    /** Người đi sau [seat] (bỏ qua người không còn nước); -1 nếu không ai đi được. */
    private fun nextSeat(b: WgBoard, seat: Int): Int {
        for (k in 1..players) { val p = (seat + k) % players; if (b.hasMove(p)) return p }
        return -1
    }

    /** Các nước của [seat] trên [b], xếp theo đánh giá tĩnh (từ phía [me]: người của mình xếp giảm dần, đối thủ xếp tăng dần). */
    private fun ordered(b: WgBoard, seat: Int, me: Int, limit: Int): IntArray {
        val ms = ArrayList<Int>(); val vs = ArrayList<Int>()
        for (c in 0 until CELLS) {
            if (b.owner[c] != seat) continue
            for (d in b.reach(c)) {
                val ws = b.wallSides(d)
                for (s in 0 until 4) if ((ws shr s) and 1 == 1) {
                    val nb = b.copy(); nb.applyPlay(c, d, s)
                    ms.add(mvMove(c, d, s)); vs.add(evalBoard(nb, me))
                }
            }
        }
        val idx = ms.indices.sortedBy { if (seat == me) -vs[it] else vs[it] }
        val n = minOf(limit, idx.size)
        return IntArray(n) { ms[idx[it]] }
    }

    private fun applyTo(b: WgBoard, m: Int): WgBoard { val nb = b.copy(); nb.applyPlay(mvFrom(m), mvTo(m), mvSide(m)); return nb }

    /** Minimax alpha-beta nhìn từ [me] (2 người: đối thủ tối thiểu hóa; 3-4 người: paranoid, mọi đối thủ cùng tối thiểu hóa). */
    private fun mm(b: WgBoard, seat: Int, me: Int, depth: Int, alphaIn: Int, betaIn: Int, ply: Int): Int {
        checkStop(); if (aborted) return 0
        val a = b.analyze()
        if (b.finished(a)) return terminal(b, a, me, ply)
        if (depth <= 0) return evalBoard(b, me)
        var alpha = alphaIn; var beta = betaIn
        val moves = ordered(b, seat, me, if (seat == me) 12 else 10)
        if (moves.isEmpty()) return evalBoard(b, me)
        var best = if (seat == me) -INF else INF
        for (m in moves) {
            val nb = applyTo(b, m)
            val nx = nextSeat(nb, seat)
            val v = if (nx < 0) terminal(nb, nb.analyze(), me, ply + 1) else mm(nb, nx, me, depth - 1, alpha, beta, ply + 1)
            if (aborted) return 0
            if (seat == me) { if (v > best) best = v; if (v > alpha) alpha = v }
            else { if (v < best) best = v; if (v < beta) beta = v }
            if (alpha >= beta) break
        }
        return best
    }

    private fun setupMove(cfg: LevelConfig, rnd: Random): Int {
        val me = cur
        val ms = ArrayList<Int>(); val vs = ArrayList<Int>()
        for (c in 0 until CELLS) {
            if (board.owner[c] != -1) continue
            val b = board.copy(); b.owner[c] = me
            val dr = kotlin.math.abs(c / 7 - 3); val dc = kotlin.math.abs(c % 7 - 3)
            ms.add(c); vs.add(((heur(b.scoreVector(), me) + 0.06 * (6 - dr - dc)) * 100).toInt() + rnd.nextInt(5))
        }
        if (ms.isEmpty()) return -1
        val bi = vs.indices.maxByOrNull { vs[it] }!!
        if (cfg.noiseP > 0 && rnd.nextDouble() < cfg.noiseP) {
            val near = vs.indices.filter { it != bi && vs[it] >= vs[bi] - cfg.delta }
            if (near.isNotEmpty()) return ms[near[rnd.nextInt(near.size)]]
        }
        return ms[bi]
    }

    private fun searchRoot(lim: SearchLimit, allowed: IntArray?, delta: Int): RootResult {
        val me = cur
        val start = System.nanoTime()
        deadline = if (lim.timeMs > 0) start + lim.timeMs * 1_000_000L else Long.MAX_VALUE
        val soft = if (lim.timeMs > 0) start + lim.timeMs * 500_000L else Long.MAX_VALUE
        val nl = if (lim.nodes > 0) lim.nodes else Long.MAX_VALUE
        val maxDepth = if (lim.depth > 0) lim.depth else 30
        nodes = 0
        val root = board
        var moves = ordered(root, me, me, 400).toMutableList()
        if (allowed != null) { val f = moves.filter { it in allowed }.toMutableList(); if (f.isNotEmpty()) moves = f }
        if (moves.isEmpty()) return RootResult(-1, 0, 0, 0)
        if (moves.size == 1) return RootResult(moves[0], 0, 0, 0)
        var best = moves[0]; var bestScore = 0; var completed = 0
        for (depth in 1..maxDepth) {
            nodeLimit = if (depth == 1) Long.MAX_VALUE else nl
            aborted = false
            val i0 = moves.indexOf(best); if (i0 > 0) { moves.removeAt(i0); moves.add(0, best) }
            var alpha = -INF; var iterBest = -1
            for (m in moves) {
                val nb = applyTo(root, m)
                val nx = nextSeat(nb, me)
                val v = if (nx < 0 || nb.finished()) terminal(nb, nb.analyze(), me, 1) else mm(nb, nx, me, depth - 1, alpha, INF, 1)
                if (aborted) break
                if (v > alpha) { alpha = v; iterBest = m }
            }
            if (aborted) { if (iterBest >= 0 && completed > 0 && alpha > bestScore - 50) best = iterBest; break }
            if (iterBest < 0) break
            best = iterBest; bestScore = alpha; completed = depth
            if (alpha >= WIN - 200 || alpha <= -WIN + 200) break
            if (System.nanoTime() > soft) break
        }
        var near = IntArray(0)
        if (delta > 0 && completed > 0) {
            aborted = false; nodeLimit = Long.MAX_VALUE; deadline = Long.MAX_VALUE
            val thr = bestScore - delta
            val out = ArrayList<Int>()
            for (m in moves) {
                if (m == best) continue
                val nb = applyTo(root, m)
                val nx = nextSeat(nb, me)
                val v = if (nx < 0 || nb.finished()) terminal(nb, nb.analyze(), me, 1) else mm(nb, nx, me, completed - 1, thr - 1, thr, 1)
                if (v >= thr) out.add(m)
            }
            near = out.toIntArray()
        }
        return RootResult(best, bestScore, completed, nodes, near)
    }

    /** Nước [m] của người đang đi có kết thúc ván với người đó thắng không. */
    private fun winsNow(m: Int): Boolean {
        val nb = applyTo(board, m); val a = nb.analyze()
        return (nb.finished(a) || nextSeat(nb, cur) < 0) && terminal(nb, a, cur, 0) > 0
    }

    override fun bestMove(timeMs: Long): Int = levelMove(BotLevel.SUPREME, timeMs, Random)

    override fun levelMove(level: BotLevel, timeMs: Long, rnd: Random): Int {
        if (over) return -1
        val cfg = BotLevels.config(id, level, timeMs)
        val work = copyGame()
        if (work.inSetup) return work.setupMove(cfg, rnd)
        val me = work.cur
        return Bot.pick(work, cfg, rnd,
            winNow = { work.legalMoves().firstOrNull { work.winsNow(it) } ?: -1 },
            losesNext = { m ->
                // sau nước m, người đi kế tiếp có nước kết thúc ván mà mình không thắng không
                val nb = work.applyTo(work.board, m); val a = nb.analyze()
                if (nb.finished(a)) work.terminal(nb, a, me, 0) < 0
                else {
                    val nx = work.nextSeat(nb, me)
                    if (nx < 0 || nx == me) false
                    else {
                        var lose = false
                        loop@ for (c in 0 until CELLS) {
                            if (nb.owner[c] != nx) continue
                            for (d in nb.reach(c)) { val ws = nb.wallSides(d); for (s in 0 until 4) if ((ws shr s) and 1 == 1) {
                                val b2 = nb.copy(); b2.applyPlay(c, d, s); val a2 = b2.analyze()
                                if (b2.finished(a2) && work.terminal(b2, a2, me, 0) < 0) { lose = true; break@loop }
                            } }
                        }
                        lose
                    }
                }
            },
        ) { l, allowed, delta -> work.searchRoot(l, allowed, delta) }
    }
}
