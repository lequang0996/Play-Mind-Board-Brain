package com.fws.playmind.core.bench

import com.fws.playmind.core.*
// Bản sao engine trước khi nâng cấp (mốc so A/B). Không sửa.

import kotlin.random.Random

/**
 * Quoridor 9x9 cho 2 hoặc 4 người.
 *
 *  - Ô đánh số 0..80 (hàng * 9 + cột), hàng 0 ở TRÊN. Tường dài 2 ô, neo ở lưới 8x8 (r, c):
 *    tường NGANG (H) nằm giữa hàng r và r+1, phủ cột c và c+1; tường DỌC (V) nằm giữa cột c và c+1, phủ hàng r và r+1.
 *  - 2 người: bên 0 ở giữa cạnh dưới (đích: hàng 0), bên 1 ở giữa cạnh trên (đích: hàng 8); mỗi người 10 tường.
 *    4 người: thứ tự đi dưới -> trái -> trên -> phải, mỗi người 5 tường, đích là cạnh đối diện.
 *  - Mỗi lượt: đi quân 1 ô (nhảy qua quân đối thủ kề bên, hoặc chéo nếu bị chặn sau lưng) HOẶC đặt một tường.
 *    Không được đặt tường chồng/cắt tường khác, và không được chặn kín mọi đường tới đích của BẤT KỲ quân nào.
 *  - Ai chạm cạnh đích trước thắng. Quá [MAX_PLIES] nước chưa ai thắng thì hòa.
 *  - Lặp thế (luật của app): 2 người, đi quân tạo lại một thế đã có (cùng người tới lượt) thì người vừa đi THUA ngay;
 *    4 người, nước đi quân như vậy bị cấm.
 *
 * Mã nước đi: đi quân = ô đích (0..80); đặt tường = 128 + hướng * 64 + r * 8 + c (hướng 0 = ngang, 1 = dọc).
 */
class QuoridorGameOld(val players: Int = 2) : BoardGame, LeveledEngine {
    companion object {
        const val N = 9
        const val MAX_PLIES = 300
        fun mvWall(horizontal: Boolean, r: Int, c: Int) = 128 + (if (horizontal) 0 else 64) + r * 8 + c
        fun isWall(m: Int) = m >= 128
        fun wallH(m: Int) = ((m - 128) shr 6) == 0
        fun wallR(m: Int) = ((m - 128) and 63) / 8
        fun wallC(m: Int) = ((m - 128) and 63) % 8
        @JvmStatic var lastDepthReached = 0
        /** Trọng số mỗi tường còn lại trong đánh giá (đo bằng trận A/B). */
        @JvmStatic var wallWeight = 75
        @JvmStatic var tempo = 30
        fun wallsPerPlayer(players: Int) = if (players == 2) 10 else 5

        private val lock = Any()
        private const val TT_BITS = 17
        private const val TT_MASK = (1L shl TT_BITS) - 1
        private const val EXACT = 0; private const val LOWER = 1; private const val UPPER = 2
        private val ttKey = LongArray(1 shl TT_BITS); private val ttMv = IntArray(1 shl TT_BITS)
        private val ttSc = IntArray(1 shl TT_BITS); private val ttInf = IntArray(1 shl TT_BITS)
        private val ZP: Array<LongArray>; private val ZH: LongArray; private val ZV: LongArray; private val ZS: LongArray
        init {
            val r = java.util.Random(0x51C0D1L)
            ZP = Array(4) { LongArray(81) { r.nextLong() } }; ZH = LongArray(64) { r.nextLong() }
            ZV = LongArray(64) { r.nextLong() }; ZS = LongArray(4) { r.nextLong() }
        }

        private val DR = intArrayOf(-1, 0, 1, 0)   // 0 = lên, 1 = phải, 2 = xuống, 3 = trái
        private val DC = intArrayOf(0, 1, 0, -1)

        /** Ô xuất phát và cạnh đích của từng bên. goal: 0 = hàng 0, 1 = cột 8, 2 = hàng 8, 3 = cột 0 */
        fun startCell(players: Int, seat: Int): Int =
            if (players == 2) (if (seat == 0) 8 * 9 + 4 else 4)
            else when (seat) { 0 -> 8 * 9 + 4; 1 -> 4 * 9; 2 -> 4; else -> 4 * 9 + 8 }
        fun goalSide(players: Int, seat: Int): Int =
            if (players == 2) (if (seat == 0) 0 else 2)
            else when (seat) { 0 -> 0; 1 -> 1; 2 -> 2; else -> 3 }
    }

    init { require(players == 2 || players == 4) }

    override val id = "quoridor"
    /** open[cell * 4 + dir] = true nếu đi từ ô theo hướng dir không bị tường/mép chặn. */
    internal val open = BooleanArray(81 * 4)
    private val hWalls = BooleanArray(64)
    private val vWalls = BooleanArray(64)
    internal val pos = IntArray(players)
    internal val wallsLeft = IntArray(players) { wallsPerPlayer(players) }
    private val hist = ArrayList<Int>()
    /** Khóa của mọi thế đã xuất hiện trong ván (để xử luật lặp thế). */
    private val keys = ArrayList<Long>()
    private var cur = 0
    override var result = Outcome.ONGOING; private set
    override var reason = EndReason.NONE; private set
    var winner = -1; private set

    init { reset0() }

    private fun reset0() {
        for (c in 0 until 81) for (d in 0 until 4) {
            val r = c / 9 + DR[d]; val cc = c % 9 + DC[d]
            open[c * 4 + d] = r in 0..8 && cc in 0..8
        }
        java.util.Arrays.fill(hWalls, false); java.util.Arrays.fill(vWalls, false)
        for (p in 0 until players) { pos[p] = startCell(players, p); wallsLeft[p] = wallsPerPlayer(players) }
        hist.clear(); cur = 0; result = Outcome.ONGOING; reason = EndReason.NONE; winner = -1
        keys.clear(); keys.add(key(0))
    }

    /** Khóa Zobrist của thế hiện tại với [seat] tới lượt (quân + tường + người đi). */
    private fun key(seat: Int): Long {
        var k = ZS[seat]
        for (p in 0 until players) k = k xor ZP[p][pos[p]]
        for (i in 0 until 64) { if (hWalls[i]) k = k xor ZH[i]; if (vWalls[i]) k = k xor ZV[i] }
        return k
    }

    /** Nước đi quân [m] của [seat] có tạo lại một thế đã có không. */
    private fun wouldRepeat(seat: Int, m: Int): Boolean {
        val old = pos[seat]; pos[seat] = m
        val k = key((seat + 1) % players); pos[seat] = old
        return k in keys
    }

    override fun reset() = reset0()
    override val history: List<Int> get() = hist
    override val turn: Int get() = cur
    override val plies: Int get() = hist.size
    override val lastMove: Int get() = if (hist.isEmpty()) -1 else hist[hist.size - 1]
    override fun inCheck() = false

    fun pawnAt(seat: Int) = pos[seat]
    fun wallsOf(seat: Int) = wallsLeft[seat]
    fun hasWallH(r: Int, c: Int) = hWalls[r * 8 + c]
    fun hasWallV(r: Int, c: Int) = vWalls[r * 8 + c]
    fun seatAt(cell: Int): Int { for (p in 0 until players) if (pos[p] == cell) return p; return -1 }
    fun reachedGoal(seat: Int): Boolean = atGoal(pos[seat], goalSide(players, seat))

    private fun atGoal(cell: Int, goal: Int) = when (goal) { 0 -> cell / 9 == 0; 1 -> cell % 9 == 8; 2 -> cell / 9 == 8; else -> cell % 9 == 0 }

    /** Độ dài đường ngắn nhất (bỏ qua quân cản) từ ô [from] tới cạnh đích; 99 nếu bị chặn kín. */
    internal fun pathLen(from: Int, goal: Int): Int {
        if (atGoal(from, goal)) return 0
        val dist = IntArray(81) { -1 }; val q = IntArray(81)
        var h = 0; var t = 0; q[t++] = from; dist[from] = 0
        while (h < t) {
            val c = q[h++]
            for (d in 0 until 4) {
                if (!open[c * 4 + d]) continue
                val n = (c / 9 + DR[d]) * 9 + c % 9 + DC[d]
                if (dist[n] >= 0) continue
                dist[n] = dist[c] + 1
                if (atGoal(n, goal)) return dist[n]
                q[t++] = n
            }
        }
        return 99
    }

    /** Các ô quân [seat] có thể đi tới (một nước), tính cả nhảy qua quân khác. */
    fun pawnMoves(seat: Int): IntArray {
        val from = pos[seat]; val out = ArrayList<Int>(6)
        for (d in 0 until 4) {
            if (!open[from * 4 + d]) continue
            val n = (from / 9 + DR[d]) * 9 + from % 9 + DC[d]
            if (seatAt(n) < 0) { out.add(n); continue }
            // nhảy thẳng qua quân đối thủ
            if (open[n * 4 + d]) {
                val j = (n / 9 + DR[d]) * 9 + n % 9 + DC[d]
                if (seatAt(j) < 0) { out.add(j); continue }
            }
            // bị chặn sau lưng (tường / mép / quân khác): nhảy chéo sang hai bên quân đó
            for (side in intArrayOf((d + 1) % 4, (d + 3) % 4)) {
                if (!open[n * 4 + side]) continue
                val j = (n / 9 + DR[side]) * 9 + n % 9 + DC[side]
                if (seatAt(j) < 0 && j != from) out.add(j)
            }
        }
        return out.toIntArray()
    }

    private fun setWall(horizontal: Boolean, r: Int, c: Int, on: Boolean) {
        if (horizontal) {
            hWalls[r * 8 + c] = on
            val a = r * 9 + c; val b = r * 9 + c + 1          // hàng r, hai cột c và c+1: chặn đi xuống
            open[a * 4 + 2] = !on; open[(a + 9) * 4 + 0] = !on
            open[b * 4 + 2] = !on; open[(b + 9) * 4 + 0] = !on
        } else {
            vWalls[r * 8 + c] = on
            val a = r * 9 + c; val b = (r + 1) * 9 + c        // cột c và c+1, hai hàng r và r+1: chặn đi sang phải
            open[a * 4 + 1] = !on; open[(a + 1) * 4 + 3] = !on
            open[b * 4 + 1] = !on; open[(b + 1) * 4 + 3] = !on
        }
    }

    /** Kiểm tra hình học (chồng/cắt) của tường, chưa xét chặn kín đường. */
    private fun wallFits(horizontal: Boolean, r: Int, c: Int): Boolean {
        if (r !in 0..7 || c !in 0..7) return false
        if (hWalls[r * 8 + c] || vWalls[r * 8 + c]) return false       // trùng neo hoặc cắt chéo
        return if (horizontal) !((c > 0 && hWalls[r * 8 + c - 1]) || (c < 7 && hWalls[r * 8 + c + 1]))
        else !((r > 0 && vWalls[(r - 1) * 8 + c]) || (r < 7 && vWalls[(r + 1) * 8 + c]))
    }

    /** Đặt thử tường: true nếu hợp lệ (và để nguyên tường trên bàn); false thì trả bàn về như cũ. */
    private fun tryWall(horizontal: Boolean, r: Int, c: Int): Boolean {
        if (!wallFits(horizontal, r, c)) return false
        setWall(horizontal, r, c, true)
        for (p in 0 until players) if (pathLen(pos[p], goalSide(players, p)) >= 99) { setWall(horizontal, r, c, false); return false }
        return true
    }

    /** Người chơi có thể đặt tường này không (đủ tường, đúng luật)? Không thay đổi bàn. */
    fun canPlaceWall(horizontal: Boolean, r: Int, c: Int): Boolean {
        if (result != Outcome.ONGOING || wallsLeft[cur] <= 0) return false
        if (!tryWall(horizontal, r, c)) return false
        setWall(horizontal, r, c, false)
        return true
    }

    override fun legalMoves(): IntArray {
        if (result != Outcome.ONGOING) return IntArray(0)
        val out = ArrayList<Int>()
        for (m in pawnMoves(cur)) if (players == 2 || !wouldRepeat(cur, m)) out.add(m)
        if (wallsLeft[cur] > 0) for (h in booleanArrayOf(true, false)) for (r in 0..7) for (c in 0..7) {
            if (tryWall(h, r, c)) { setWall(h, r, c, false); out.add(mvWall(h, r, c)) }
        }
        return out.toIntArray()
    }

    override fun play(move: Int): Boolean {
        if (result != Outcome.ONGOING || move < 0) return false
        if (!isWall(move)) {
            if (move > 80 || move !in pawnMoves(cur)) return false
            if (players != 2 && wouldRepeat(cur, move)) return false
            pos[cur] = move
            hist.add(move)
            if (atGoal(move, goalSide(players, cur))) {
                result = if (cur == 0) Outcome.WIN_FIRST else Outcome.WIN_SECOND; reason = EndReason.NONE; winner = cur
                return true
            }
            val k = key((cur + 1) % players)
            if (players == 2 && k in keys) {                 // lặp thế: người vừa đi thua
                result = if (cur == 0) Outcome.WIN_SECOND else Outcome.WIN_FIRST; reason = EndReason.REPETITION; winner = 1 - cur
                keys.add(k); cur = 1 - cur
                return true
            }
        } else {
            if (move > 255 || wallsLeft[cur] <= 0) return false
            if (!tryWall(wallH(move), wallR(move), wallC(move))) return false
            wallsLeft[cur]--
            hist.add(move)
        }
        cur = (cur + 1) % players
        keys.add(key(cur))
        if (hist.size >= MAX_PLIES) { result = Outcome.DRAW; reason = EndReason.MOVE_LIMIT }
        return true
    }

    override fun forfeit(loser: Int, reason: EndReason) {
        if (result != Outcome.ONGOING) return
        this.reason = reason
        result = if (loser == 0) Outcome.WIN_SECOND else Outcome.WIN_FIRST
        winner = if (players == 2) 1 - loser else -1
    }

    override fun copyGame(): QuoridorGameOld {
        val g = QuoridorGameOld(players)
        System.arraycopy(open, 0, g.open, 0, open.size)
        System.arraycopy(hWalls, 0, g.hWalls, 0, 64); System.arraycopy(vWalls, 0, g.vWalls, 0, 64)
        System.arraycopy(pos, 0, g.pos, 0, players); System.arraycopy(wallsLeft, 0, g.wallsLeft, 0, players)
        g.hist.addAll(hist); g.keys.clear(); g.keys.addAll(keys); g.cur = cur; g.result = result; g.reason = reason; g.winner = winner
        return g
    }

    // ═════════════════════════ AI ═════════════════════════
    private val INF = 1_000_000

    /** Điểm cho bên [me] (càng cao càng tốt): chênh lệch đường đi tới đích + tường còn lại. */
    private fun evaluate(me: Int, dists: IntArray): Int {
        val my = dists[me]
        if (my == 0) return INF - 1
        for (p in 0 until players) if (p != me && dists[p] == 0) return -INF + 1
        if (players == 2) {
            val opp = dists[1 - me]
            var v = (opp - my) * 100 + (wallsLeft[me] - wallsLeft[1 - me]) * wallWeight + tempo
            // Chạy đua đã ngã ngũ: bên kia hết tường mà mình (đang tới lượt) không chậm hơn -> thắng chắc; ngược lại thì thua chắc.
            if (wallsLeft[1 - me] == 0 && my <= opp) v += 3000
            else if (wallsLeft[me] == 0 && opp < my) v -= 3000
            return v
        }
        var minOpp = 99; var sum = 0
        for (p in 0 until players) if (p != me) { if (dists[p] < minOpp) minOpp = dists[p]; sum += dists[p] }
        return (minOpp - my) * 70 + (sum / (players - 1) - my) * 40 + wallsLeft[me] * 6
    }

    private fun allDists(): IntArray = IntArray(players) { pathLen(pos[it], goalSide(players, it)) }

    /** Các tường đáng thử cho bên [seat]: những tường chắn lên đường đi ngắn nhất của đối thủ gần đích nhất (hoặc của đối thủ duy nhất). */
    private fun wallCandidates(seat: Int, dists: IntArray): List<Int> {
        if (wallsLeft[seat] <= 0) return emptyList()
        // chọn đối thủ cần chặn: gần đích nhất
        var target = -1; var best = 100
        for (p in 0 until players) if (p != seat && dists[p] < best) { best = dists[p]; target = p }
        if (target < 0) return emptyList()
        val goal = goalSide(players, target)
        val prev = IntArray(81) { -1 }; val seen = BooleanArray(81); val q = IntArray(81)
        var h = 0; var t = 0; q[t++] = pos[target]; seen[pos[target]] = true
        var end = -1
        if (atGoal(pos[target], goal)) return emptyList()
        loop@ while (h < t) {
            val c = q[h++]
            for (d in 0 until 4) {
                if (!open[c * 4 + d]) continue
                val n = (c / 9 + DR[d]) * 9 + c % 9 + DC[d]
                if (seen[n]) continue
                seen[n] = true; prev[n] = c; q[t++] = n
                if (atGoal(n, goal)) { end = n; break@loop }
            }
        }
        val cand = LinkedHashSet<Int>()
        fun add(h: Boolean, r: Int, c: Int) { if (r in 0..7 && c in 0..7) cand.add(mvWall(h, r, c)) }   // neo ngoài lưới 8x8 thì bỏ
        var c = end
        while (c >= 0 && prev[c] >= 0) {
            val a = prev[c]; val b = c
            if (a / 9 != b / 9) {            // đi dọc: chắn bằng tường ngang ở hai neo kề
                val r = minOf(a, b) / 9; val col = a % 9
                add(true, r, col - 1); add(true, r, col)
            } else {                         // đi ngang: chắn bằng tường dọc
                val r = a / 9; val col = minOf(a, b) % 9
                add(false, r - 1, col); add(false, r, col)
            }
            c = prev[c]
        }
        return cand.toList()
    }

    private class Scored(val move: Int, val score: Int)

    /** Sinh nước đi có thứ tự (nước đi quân trước, rồi tường theo mức "gây khó" cho đối thủ). */
    private fun orderedMoves(seat: Int, dists: IntArray, maxWalls: Int): List<Int> {
        val out = ArrayList<Scored>()
        for (m in pawnMoves(seat)) {
            val old = pos[seat]; pos[seat] = m
            val d = pathLen(m, goalSide(players, seat)); pos[seat] = old
            out.add(Scored(m, (dists[seat] - d) * 1000))
        }
        val walls = ArrayList<Scored>()
        for (m in wallCandidates(seat, dists)) {
            val h = wallH(m); val r = wallR(m); val c = wallC(m)
            if (!tryWall(h, r, c)) continue
            val nd = allDists()
            setWall(h, r, c, false)
            var gain = 0
            for (p in 0 until players) if (p != seat) gain += (nd[p] - dists[p]) * (if (players == 2) 100 else 60)
            gain -= (nd[seat] - dists[seat]) * 110
            walls.add(Scored(m, gain))
        }
        walls.sortByDescending { it.score }
        for (w in walls.take(maxWalls)) out.add(w)
        out.sortByDescending { it.score }
        return out.map { it.move }
    }

    private fun apply(seat: Int, m: Int): Int {          // trả về giá trị cũ để hoàn tác (ô cũ, hoặc -1 khi là tường)
        return if (!isWall(m)) { val old = pos[seat]; pos[seat] = m; old }
        else { setWall(wallH(m), wallR(m), wallC(m), true); wallsLeft[seat]--; -1 }
    }
    private fun undo(seat: Int, m: Int, old: Int) {
        if (!isWall(m)) pos[seat] = old else { setWall(wallH(m), wallR(m), wallC(m), false); wallsLeft[seat]++ }
    }

    // ═════════════════ Tìm kiếm: Siêu việt (thời gian) và 6 cấp giảm sức (độ sâu / số nút) ═════════════════
    private var deadline = Long.MAX_VALUE
    private var nodeLimit = Long.MAX_VALUE
    private var nodes = 0L
    private var aborted = false
    /** Khóa thế cờ: lịch sử ván ([rootKeys]) nối với đường tìm kiếm ([path], path[ply] = thế sau ply nước tính từ gốc). */
    private var rootKeys = LongArray(0)
    private val path = LongArray(160)
    private val killers = Array(160) { IntArray(2) }

    private fun checkStop() {
        if ((++nodes and 63L) == 0L && (nodes >= nodeLimit || System.nanoTime() > deadline)) aborted = true
    }

    /** Thế ở path[ply] (vừa có sau một nước đi quân) đã từng xuất hiện trong ván hoặc trên đường tìm kiếm? */
    private fun repeatedAt(ply: Int): Boolean {
        val k = path[ply]
        val base = rootKeys.size - 1                     // chỉ số của gốc trong chuỗi khóa chung
        var idx = base + ply - players                    // cùng người tới lượt: cách nhau đúng [players] nước
        while (idx >= 0) {
            val other = if (idx >= base) path[idx - base] else rootKeys[idx]
            if (other == k) return true
            idx -= players
        }
        return false
    }

    private fun ttStore(k: Long, depth: Int, score: Int, flag: Int, move: Int) {
        val slot = (k and TT_MASK).toInt()
        if (ttKey[slot] == k && (ttInf[slot] shr 2) > depth && move < 0) return
        ttKey[slot] = k; ttMv[slot] = move
        val mate = score > INF - 1000 || score < -INF + 1000
        ttSc[slot] = score; ttInf[slot] = ((if (mate) 0 else depth) shl 2) or flag
    }

    private fun front(moves: MutableList<Int>, m: Int) { val i = moves.indexOf(m); if (i > 0) { moves.removeAt(i); moves.add(0, m) } }

    /** Negamax alpha-beta + bảng băm + PVS + killer cho 2 người. [seat] là bên đang đi; path[ply] là khóa của thế hiện tại. */
    private fun ab(seat: Int, depth: Int, alphaIn: Int, beta: Int, ply: Int): Int {
        checkStop(); if (aborted) return 0
        val dists = allDists()
        if (depth <= 0 || ply >= 150) return evaluate(seat, dists)
        var alpha = alphaIn
        val k = path[ply]
        val slot = (k and TT_MASK).toInt()
        var ttMove = -1
        if (ttKey[slot] == k) {
            ttMove = ttMv[slot]
            val td = ttInf[slot] shr 2; val fl = ttInf[slot] and 3; val s = ttSc[slot]
            if (td >= depth && (fl == EXACT || (fl == LOWER && s >= beta) || (fl == UPPER && s <= alpha))) return s
        }
        val moves = orderedMoves(seat, dists, 10).toMutableList()
        if (moves.isEmpty()) return evaluate(seat, dists)
        for (km in killers[ply].reversed()) if (km >= 0) front(moves, km)
        if (ttMove >= 0) front(moves, ttMove)
        val a0 = alpha
        var best = -INF * 2; var bestMove = -1; var first = true
        for (m in moves) {
            val old = apply(seat, m)
            var v: Int
            if (!isWall(m) && atGoal(m, goalSide(players, seat))) v = INF - ply - 1
            else {
                path[ply + 1] = key(1 - seat)
                if (!isWall(m) && repeatedAt(ply + 1)) v = -(INF - ply - 1)          // tự lặp thế: thua
                else if (first) v = -ab(1 - seat, depth - 1, -beta, -alpha, ply + 1)
                else {
                    v = -ab(1 - seat, depth - 1, -alpha - 1, -alpha, ply + 1)
                    if (v > alpha && v < beta && !aborted) v = -ab(1 - seat, depth - 1, -beta, -alpha, ply + 1)
                }
            }
            undo(seat, m, old)
            if (aborted) return 0
            first = false
            if (v > best) { best = v; bestMove = m }
            if (v > alpha) alpha = v
            if (alpha >= beta) {
                if (killers[ply][0] != m) { killers[ply][1] = killers[ply][0]; killers[ply][0] = m }
                break
            }
        }
        if (bestMove < 0) return evaluate(seat, dists)
        ttStore(k, depth, best, if (best <= a0) UPPER else if (best >= beta) LOWER else EXACT, bestMove)
        return best
    }

    /** Tường có đặt được ở thế hiện tại không (không để lại thay đổi). */
    private fun tryWallCheck(m: Int): Boolean {
        val h = wallH(m); val r = wallR(m); val c = wallC(m)
        if (!tryWall(h, r, c)) return false
        setWall(h, r, c, false); return true
    }

    /** Paranoid (3-4 người): [me] tối đa hóa đánh giá của mình, mọi đối thủ cùng tối thiểu hóa. Lặp thế ở chế độ này là nước cấm. */
    private fun para(seat: Int, me: Int, depth: Int, alphaIn: Int, betaIn: Int, ply: Int): Int {
        checkStop(); if (aborted) return 0
        val dists = allDists()
        if (depth <= 0 || ply >= 150) return evaluate(me, dists)
        var alpha = alphaIn; var beta = betaIn
        val next = (seat + 1) % players
        val moves = orderedMoves(seat, dists, if (seat == me) 8 else 5)
        var best = if (seat == me) -INF * 2 else INF * 2
        var any = false
        for (m in moves) {
            val old = apply(seat, m)
            var v: Int
            if (!isWall(m) && atGoal(m, goalSide(players, seat))) v = if (seat == me) INF - ply - 1 else -(INF - ply - 1)
            else {
                path[ply + 1] = key(next)
                if (!isWall(m) && repeatedAt(ply + 1)) { undo(seat, m, old); continue }
                v = para(next, me, depth - 1, alpha, beta, ply + 1)
            }
            undo(seat, m, old)
            if (aborted) return 0
            any = true
            if (seat == me) { if (v > best) best = v; if (v > alpha) alpha = v }
            else { if (v < best) best = v; if (v < beta) beta = v }
            if (alpha >= beta) break
        }
        return if (any) best else evaluate(me, dists)
    }

    /** Tìm kiếm có giới hạn ở gốc. [allowed] = các nước được xét (null = mọi nước). [delta] > 0: trả thêm các nước gần tốt nhất. */
    private fun searchRoot(me: Int, lim: SearchLimit, allowed: IntArray?, delta: Int): RootResult {
        val start = System.nanoTime()
        deadline = if (lim.timeMs > 0) start + lim.timeMs * 1_000_000L else Long.MAX_VALUE
        val soft = if (lim.timeMs > 0) start + lim.timeMs * 550_000L else Long.MAX_VALUE
        val nl = if (lim.nodes > 0) lim.nodes else Long.MAX_VALUE
        val maxDepth = if (lim.depth > 0) lim.depth else 40
        nodes = 0; aborted = false
        rootKeys = keys.toLongArray(); path[0] = key(me)
        for (row in killers) { row[0] = -1; row[1] = -1 }
        if (players == 2) java.util.Arrays.fill(ttKey, 0L)
        val dists = allDists()
        var moves = orderedMoves(me, dists, if (players == 2) 14 else 10).toMutableList()
        // các tường ngoài danh sách ứng viên vẫn có thể được phép khi lọc an toàn chỉ còn chúng
        if (allowed != null) {
            val f = moves.filter { it in allowed }.toMutableList()
            moves = if (f.isNotEmpty()) f else allowed.toMutableList()
        }
        if (players != 2) moves.removeAll { !isWall(it) && wouldRepeat(me, it) }
        if (moves.isEmpty()) moves = legalMoves().toMutableList()
        if (moves.isEmpty()) return RootResult(-1, 0, 0, 0)
        if (moves.size == 1) return RootResult(moves[0], 0, 0, 0)
        val next = (me + 1) % players
        var best = moves[0]; var bestScore = 0; var completed = 0
        for (depth in 1..maxDepth) {
            nodeLimit = if (depth == 1) Long.MAX_VALUE else nl
            aborted = false
            var alpha = -INF * 2; var iterBest = -1
            front(moves, best)
            for ((i, m) in moves.withIndex()) {
                val old = apply(me, m)
                var v: Int
                if (!isWall(m) && atGoal(m, goalSide(players, me))) v = INF
                else {
                    path[1] = key(next)
                    v = if (players == 2) {
                        if (!isWall(m) && repeatedAt(1)) -INF
                        else if (i == 0) -ab(next, depth - 1, -INF * 2, -alpha, 1)
                        else {
                            var t = -ab(next, depth - 1, -alpha - 1, -alpha, 1)
                            if (t > alpha && !aborted) t = -ab(next, depth - 1, -INF * 2, -alpha, 1)
                            t
                        }
                    } else para(next, me, depth - 1, alpha, INF * 2, 1)
                }
                undo(me, m, old)
                if (aborted) break
                if (v > alpha) { alpha = v; iterBest = m }
            }
            if (aborted) { if (iterBest >= 0 && completed > 0 && alpha > bestScore - 40) best = iterBest; break }
            if (iterBest < 0) break
            best = iterBest; bestScore = alpha; completed = depth
            lastDepthReached = depth
            if (alpha >= INF - 200 || alpha <= -INF + 200) break
            if (System.nanoTime() > soft) break
        }
        var near = IntArray(0)
        if (delta > 0 && completed > 0) {
            aborted = false; nodeLimit = Long.MAX_VALUE; deadline = Long.MAX_VALUE
            val thr = bestScore - delta
            val out = ArrayList<Int>()
            for (m in moves) {
                if (m == best) continue
                val old = apply(me, m)
                val v = if (!isWall(m) && atGoal(m, goalSide(players, me))) INF else {
                    path[1] = key(next)
                    if (players == 2) { if (!isWall(m) && repeatedAt(1)) -INF else -ab(next, completed - 1, -thr, -thr + 1, 1) }
                    else para(next, me, completed - 1, thr - 1, thr, 1)
                }
                undo(me, m, old)
                if (v >= thr) out.add(m)
            }
            near = out.toIntArray()
        }
        return RootResult(best, bestScore, completed, nodes, near)
    }

    override fun bestMove(timeMs: Long): Int = levelMove(BotLevel.SUPREME, timeMs, Random)

    override fun levelMove(level: BotLevel, timeMs: Long, rnd: Random): Int {
        if (result != Outcome.ONGOING) return -1
        val cfg = BotLevels.config(id, level, timeMs)
        return synchronized(lock) {
            val work = copyGame()
            val me = work.cur
            Bot.pick(work, cfg, rnd,
                winNow = { work.pawnMoves(me).firstOrNull { work.atGoal(it, goalSide(players, me)) } ?: -1 },
                losesNext = { m ->
                    val c = work.copyGame()
                    if (!c.play(m) || c.result != Outcome.ONGOING) c.result != Outcome.ONGOING && c.winner != me
                    else { val nx = c.cur; c.pawnMoves(nx).any { c.atGoal(it, goalSide(players, nx)) } }
                },
            ) { l, allowed, delta -> work.searchRoot(me, l, allowed, delta) }
        }
    }
}
