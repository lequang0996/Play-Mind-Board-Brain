package com.fws.playmind.core

import kotlin.random.Random

/**
 * Cờ caro (Gomoku) 15x15. Bên đi trước (đen) = 1, bên sau (trắng) = 2. Nước đi = chỉ số ô y*size + x.
 * Luật: 5 quân liên tiếp trở lên thắng, trừ khi cả hai đầu dãy bị chặn (quân đối phương hoặc mép bàn).
 */
class GomokuGame(val size: Int = 15) : BoardGame {
    companion object {
        private val DIRS = intArrayOf(1, 0, 0, 1, 1, 1, 1, -1)
        private val W = intArrayOf(0, 1, 10, 100, 1000, 100000)
        private const val WIN = 1_000_000
        private const val MAXPLY = 40
    }

    override val id = "caro"
    val cells = IntArray(size * size)
    private val hist = ArrayList<Int>()
    override val history: List<Int> get() = hist
    override var result = Outcome.ONGOING; private set
    override var reason = EndReason.NONE; private set
    override val turn: Int get() = hist.size % 2
    override val plies: Int get() = hist.size
    override val lastMove: Int get() = if (hist.isEmpty()) -1 else hist[hist.size - 1]

    override fun reset() { cells.fill(0); hist.clear(); result = Outcome.ONGOING; reason = EndReason.NONE }
    override fun inCheck() = false

    /** Các ô tạo thành dãy thắng của nước vừa đi (để vẽ vạch thắng), rỗng nếu chưa thắng. */
    var winLine: IntArray = IntArray(0); private set

    override fun legalMoves(): IntArray {
        if (result != Outcome.ONGOING) return IntArray(0)
        val out = IntArray(cells.size); var n = 0
        for (i in cells.indices) if (cells[i] == 0) out[n++] = i
        return out.copyOf(n)
    }

    override fun play(move: Int): Boolean {
        if (result != Outcome.ONGOING || move !in cells.indices || cells[move] != 0) return false
        val p = turn + 1
        cells[move] = p; hist.add(move)
        val line = winAt(move)
        if (line != null) {
            winLine = line
            result = if (p == 1) Outcome.WIN_FIRST else Outcome.WIN_SECOND; reason = EndReason.FIVE
        } else if (hist.size == cells.size) { result = Outcome.DRAW; reason = EndReason.FULL }
        return true
    }

    override fun forfeit(loser: Int, reason: EndReason) {
        if (result != Outcome.ONGOING) return
        result = if (loser == 0) Outcome.WIN_SECOND else Outcome.WIN_FIRST
        this.reason = reason
    }

    override fun copyGame(): GomokuGame {
        val g = GomokuGame(size)
        System.arraycopy(cells, 0, g.cells, 0, cells.size)
        g.hist.addAll(hist); g.result = result; g.reason = reason; g.winLine = winLine
        return g
    }

    private fun inb(x: Int, y: Int) = x in 0 until size && y in 0 until size

    /** Nếu quân tại [c] tạo dãy thắng thì trả về các ô của dãy, ngược lại null. */
    private fun winAt(c: Int): IntArray? {
        val p = cells[c]; if (p == 0) return null
        val cx = c % size; val cy = c / size
        var d = 0
        while (d < 8) {
            val dx = DIRS[d]; val dy = DIRS[d + 1]; d += 2
            var fx = cx + dx; var fy = cy + dy; var nf = 0
            while (inb(fx, fy) && cells[fy * size + fx] == p) { nf++; fx += dx; fy += dy }
            val blockedF = !inb(fx, fy) || cells[fy * size + fx] != 0
            var bx = cx - dx; var by = cy - dy; var nbk = 0
            while (inb(bx, by) && cells[by * size + bx] == p) { nbk++; bx -= dx; by -= dy }
            val blockedB = !inb(bx, by) || cells[by * size + bx] != 0
            val len = 1 + nf + nbk
            if (len >= 5 && !(blockedF && blockedB)) {
                val out = IntArray(len)
                for (i in 0 until len) out[i] = (cy - nbk * dy + i * dy) * size + (cx - nbk * dx + i * dx)
                return out
            }
        }
        return null
    }

    // ───────────────────────── AI ─────────────────────────
    private var deadline = 0L
    private var nodes = 0L
    private var abort = false
    private val nb = IntArray(size * size)
    private val candBuf = Array(MAXPLY) { IntArray(size * size) }
    private val candScore = Array(MAXPLY) { IntArray(size * size) }

    override fun bestMove(timeMs: Long): Int = copyGame().searchRoot(timeMs)

    private fun nbUpdate(c: Int, delta: Int) {
        val cx = c % size; val cy = c / size
        for (dy in -2..2) for (dx in -2..2) { val x = cx + dx; val y = cy + dy; if (inb(x, y)) nb[y * size + x] += delta }
    }

    private fun put(c: Int, p: Int) { cells[c] = p; nbUpdate(c, 1) }
    private fun take(c: Int) { cells[c] = 0; nbUpdate(c, -1) }

    /** Điểm tăng thêm nếu người chơi p đặt quân vào ô c (tính theo mọi cửa sổ 5 ô đi qua c). */
    private fun gain(c: Int, p: Int): Int {
        val cx = c % size; val cy = c / size
        val o = 3 - p
        var g = 0
        var d = 0
        while (d < 8) {
            val dx = DIRS[d]; val dy = DIRS[d + 1]; d += 2
            for (k in -4..0) {
                val sx = cx + k * dx; val sy = cy + k * dy
                val ex = sx + 4 * dx; val ey = sy + 4 * dy
                if (!inb(sx, sy) || !inb(ex, ey)) continue
                var own = 0; var opp = 0
                for (i in 0 until 5) {
                    val v = cells[(sy + i * dy) * size + (sx + i * dx)]
                    if (v == p) own++ else if (v == o) opp++
                }
                if (opp == 0) g += W[own + 1] - W[own]
            }
        }
        return g
    }

    private fun evaluate(p: Int): Int {
        var mine = 0; var theirs = 0
        var d = 0
        while (d < 8) {
            val dx = DIRS[d]; val dy = DIRS[d + 1]; d += 2
            for (sy in 0 until size) for (sx in 0 until size) {
                val ex = sx + 4 * dx; val ey = sy + 4 * dy
                if (!inb(ex, ey)) continue
                var a = 0; var b = 0
                for (i in 0 until 5) {
                    val v = cells[(sy + i * dy) * size + (sx + i * dx)]
                    if (v == 1) a++ else if (v == 2) b++
                }
                if (a > 0 && b == 0) { if (p == 1) mine += W[a] else theirs += W[a] }
                else if (b > 0 && a == 0) { if (p == 2) mine += W[b] else theirs += W[b] }
            }
        }
        return mine - theirs
    }

    private fun wins(c: Int, p: Int): Boolean {
        cells[c] = p
        val r = winAt(c) != null
        cells[c] = 0
        return r
    }

    /** Sinh ứng viên đã sắp xếp theo điểm giảm dần; trả về số ứng viên. */
    private fun gen(ply: Int, p: Int, rnd: Boolean): Int {
        val buf = candBuf[ply]; val sc = candScore[ply]
        var n = 0
        val o = 3 - p
        for (c in cells.indices) {
            if (cells[c] != 0 || nb[c] == 0) continue
            var s = gain(c, p) + gain(c, o) * 9 / 10
            if (rnd) s += Random.nextInt(0, 3)
            buf[n] = c; sc[n] = s; n++
        }
        // sắp xếp chèn giảm dần (n nhỏ)
        for (i in 1 until n) {
            val c = buf[i]; val s = sc[i]; var j = i - 1
            while (j >= 0 && sc[j] < s) { buf[j + 1] = buf[j]; sc[j + 1] = sc[j]; j-- }
            buf[j + 1] = c; sc[j + 1] = s
        }
        return n
    }

    private fun search(depth: Int, alphaIn: Int, beta: Int, p: Int, ply: Int): Int {
        var alpha = alphaIn
        if ((++nodes and 1023L) == 0L && System.nanoTime() > deadline) abort = true
        if (abort) return 0
        val n = gen(ply, p, false)
        if (n == 0) return 0
        val buf = candBuf[ply]; val sc = candScore[ply]
        val o = 3 - p
        // thắng ngay?
        var i = 0
        while (i < n && i < 12) { if (gain(buf[i], p) >= 99000 && wins(buf[i], p)) return WIN - ply; i++ }
        if (depth <= 0 || ply >= MAXPLY - 2) return evaluate(p)
        // buộc chặn?
        var forced = -1
        i = 0
        while (i < n) { if (gain(buf[i], o) >= 99000 && wins(buf[i], o)) { forced = buf[i]; break }; i++ }
        val limit = if (forced >= 0) 1 else minOf(n, if (depth >= 3) 9 else 7)
        for (k in 0 until limit) {
            val c = if (forced >= 0) forced else buf[k]
            put(c, p)
            val s = -search(depth - 1, -beta, -alpha, o, ply + 1)
            take(c)
            if (abort) return 0
            if (s >= beta) return beta
            if (s > alpha) alpha = s
        }
        return alpha
    }

    private fun searchRoot(timeMs: Long): Int {
        val start = System.nanoTime()
        deadline = start + timeMs * 1_000_000
        abort = false; nodes = 0
        val stones = hist.size
        val mid = (size / 2) * size + size / 2
        if (stones == 0) return mid
        val p = turn + 1; val o = 3 - p
        for (c in cells.indices) if (cells[c] != 0) nbUpdate(c, 1)
        if (stones == 1) {
            val opts = ArrayList<Int>()
            val c0 = hist[0]
            for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val x = c0 % size + dx; val y = c0 / size + dy
                if (inb(x, y)) opts.add(y * size + x)
            }
            return opts[Random.nextInt(opts.size)]
        }
        val n = gen(0, p, true)
        if (n == 0) return -1
        val root = candBuf[0].copyOf(n)
        // thắng ngay / buộc chặn ở gốc
        for (c in root) if (gain(c, p) >= 99000 && wins(c, p)) return c
        for (c in root) if (gain(c, o) >= 99000 && wins(c, o)) return c
        val limit = minOf(n, 14)
        var best = root[0]
        val scoreOf = IntArray(limit)
        val cand = root.copyOf(limit)
        for (depth in 2..12) {
            var alpha = -WIN * 2; var iterBest = -1; var iterScore = -WIN * 2
            for (k in 0 until limit) {
                val c = cand[k]
                put(c, p)
                val s = -search(depth - 1, -WIN * 2, -alpha, o, 1)
                take(c)
                if (abort) break
                scoreOf[k] = s
                if (s > iterScore) { iterScore = s; iterBest = c }
                if (s > alpha) alpha = s
            }
            if (abort) break
            best = iterBest
            // đưa nước tốt nhất lên đầu cho vòng sau
            val bi = cand.indexOf(best)
            if (bi > 0) { System.arraycopy(cand, 0, cand, 1, bi); cand[0] = best }
            if (iterScore >= WIN - 100) break
            if (System.nanoTime() - start > (deadline - start) * 0.5) break
        }
        return best
    }
}
