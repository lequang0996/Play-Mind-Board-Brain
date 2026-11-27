package com.fws.playmind.core

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
 *
 * Mã nước đi: đi quân = ô đích (0..80); đặt tường = 128 + hướng * 64 + r * 8 + c (hướng 0 = ngang, 1 = dọc).
 */
class QuoridorGame(val players: Int = 2) : BoardGame {
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
        for (m in pawnMoves(cur)) out.add(m)
        if (wallsLeft[cur] > 0) for (h in booleanArrayOf(true, false)) for (r in 0..7) for (c in 0..7) {
            if (tryWall(h, r, c)) { setWall(h, r, c, false); out.add(mvWall(h, r, c)) }
        }
        return out.toIntArray()
    }

    override fun play(move: Int): Boolean {
        if (result != Outcome.ONGOING || move < 0) return false
        if (!isWall(move)) {
            if (move > 80 || move !in pawnMoves(cur)) return false
            pos[cur] = move
            hist.add(move)
            if (atGoal(move, goalSide(players, cur))) {
                result = if (cur == 0) Outcome.WIN_FIRST else Outcome.WIN_SECOND; reason = EndReason.NONE; winner = cur
                return true
            }
        } else {
            if (move > 255 || wallsLeft[cur] <= 0) return false
            if (!tryWall(wallH(move), wallR(move), wallC(move))) return false
            wallsLeft[cur]--
            hist.add(move)
        }
        cur = (cur + 1) % players
        if (hist.size >= MAX_PLIES) { result = Outcome.DRAW; reason = EndReason.REPETITION }
        return true
    }

    override fun forfeit(loser: Int, reason: EndReason) {
        if (result != Outcome.ONGOING) return
        this.reason = reason
        result = if (loser == 0) Outcome.WIN_SECOND else Outcome.WIN_FIRST
        winner = if (players == 2) 1 - loser else -1
    }

    override fun copyGame(): QuoridorGame {
        val g = QuoridorGame(players)
        System.arraycopy(open, 0, g.open, 0, open.size)
        System.arraycopy(hWalls, 0, g.hWalls, 0, 64); System.arraycopy(vWalls, 0, g.vWalls, 0, 64)
        System.arraycopy(pos, 0, g.pos, 0, players); System.arraycopy(wallsLeft, 0, g.wallsLeft, 0, players)
        g.hist.addAll(hist); g.cur = cur; g.result = result; g.reason = reason; g.winner = winner
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
            return (opp - my) * 100 + (wallsLeft[me] - wallsLeft[1 - me]) * wallWeight + tempo
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

    private var deadline = 0L
    private var aborted = false

    /** Negamax alpha-beta cho 2 người, [seat] là bên đang đi. */
    private fun negamax(seat: Int, depth: Int, alphaIn: Int, beta: Int): Int {
        val dists = allDists()
        if (dists[0] == 0 || dists[1] == 0 || depth == 0) return evaluate(seat, dists) + depth
        if (System.nanoTime() > deadline) { aborted = true; return 0 }
        var alpha = alphaIn
        var best = -INF
        val moves = orderedMoves(seat, dists, 9)
        for (m in moves) {
            val old = apply(seat, m)
            val won = !isWall(m) && atGoal(m, goalSide(players, seat))
            val v = if (won) INF - 1 + depth else -negamax(1 - seat, depth - 1, -beta, -alpha)
            undo(seat, m, old)
            if (aborted) return 0
            if (v > best) best = v
            if (best > alpha) alpha = best
            if (alpha >= beta) break
        }
        return best
    }

    override fun bestMove(timeMs: Long): Int {
        if (result != Outcome.ONGOING) return -1
        val me = cur
        val g = copyGame()          // tìm kiếm trên bản sao để không đụng tới ván thật
        return g.searchRoot(me, timeMs)
    }

    private fun searchRoot(me: Int, timeMs: Long): Int {
        deadline = System.nanoTime() + timeMs * 1_000_000L
        val dists = allDists()
        // thắng ngay nếu có thể
        for (m in pawnMoves(me)) if (atGoal(m, goalSide(players, me))) return m
        val moves = orderedMoves(me, dists, if (players == 2) 10 else 8)
        if (moves.isEmpty()) return pawnMoves(me).firstOrNull() ?: -1
        var bestMove = moves[0]
        if (players != 2) {
            // 4 người: đánh giá 1 nước + phản ứng đơn giản (bên kế tiếp chặn/đi tốt nhất theo đánh giá của chính nó)
            var bestV = -INF * 2
            for (m in moves) {
                val old = apply(me, m)
                var v = evaluate(me, allDists())
                undo(me, m, old)
                v += Random.nextInt(0, 6)
                if (v > bestV) { bestV = v; bestMove = m }
            }
            return bestMove
        }
        var depth = 1
        var bestSoFar = bestMove
        while (depth <= 12) {
            aborted = false
            var alpha = -INF * 2; var curBest = -1
            val ordered = if (bestSoFar != moves[0]) listOf(bestSoFar) + moves.filter { it != bestSoFar } else moves
            for (m in ordered) {
                val old = apply(me, m)
                val won = !isWall(m) && atGoal(m, goalSide(players, me))
                val v = if (won) INF else -negamax(1 - me, depth - 1, -INF * 2, -alpha)
                undo(me, m, old)
                if (aborted) break
                if (v > alpha) { alpha = v; curBest = m }
            }
            if (aborted || curBest < 0) break
            bestSoFar = curBest
            lastDepthReached = depth
            if (alpha >= INF - 50) break
            depth++
        }
        return bestSoFar
    }
}
