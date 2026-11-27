package com.fws.playmind.core

import kotlin.random.Random

/**
 * Engine AI cho cờ caro trên bàn VUÔNG CÓ KÍCH THƯỚC CỐ ĐỊNH (tối đa [MAXW]x[MAXW]).
 * Bàn cờ thật của app là vô hạn ([GomokuGame]); mỗi lần bot suy nghĩ, [GomokuGame] cắt một "cửa sổ" quanh các quân
 * đang có, đưa vào engine này để tìm nước, rồi dịch tọa độ ngược lại.
 *
 * Bên đi trước (đen) = 1, bên sau (trắng) = 2. Nước đi = chỉ số ô y*size + x.
 * Luật: 5 quân liên tiếp trở lên thắng, trừ khi cả hai đầu dãy bị chặn (quân đối phương hoặc mép bàn).
 *
 * Engine v2 (cấp cao thủ): đánh giá theo cửa sổ 5 ô cập nhật tăng dần, tìm kiếm chuỗi đe dọa VCF
 * (liên tục đánh bốn), phòng thủ trước VCF của đối phương, alpha-beta lặp sâu với bảng băm Zobrist.
 * (VCT bốn + ba hở có sẵn nhưng tắt, xem USE_VCT.) Xem docs/BOT_GUIDE.md.
 */
class GomokuEngine(val size: Int = 15) : BoardGame {
    /** Hình học các cửa sổ 5 ô (4 hướng) của bàn cờ cỡ [size]. */
    private class Geo(val size: Int) {
        val nWin: Int
        val cells: IntArray
        val cellWins: Array<IntArray>
        init {
            val dirs = intArrayOf(1, 0, 0, 1, 1, 1, 1, -1)
            val list = ArrayList<IntArray>()
            for (d in 0 until 4) {
                val dx = dirs[d * 2]; val dy = dirs[d * 2 + 1]
                for (y in 0 until size) for (x in 0 until size) {
                    val ex = x + 4 * dx; val ey = y + 4 * dy
                    if (ex !in 0 until size || ey !in 0 until size) continue
                    list.add(IntArray(5) { (y + it * dy) * size + (x + it * dx) })
                }
            }
            nWin = list.size
            cells = IntArray(nWin * 5)
            val per = Array(size * size) { ArrayList<Int>() }
            for ((w, arr) in list.withIndex()) for (i in 0 until 5) { cells[w * 5 + i] = arr[i]; per[arr[i]].add(w) }
            cellWins = Array(size * size) { per[it].toIntArray() }
        }
    }

    companion object {
        private val DIRS = intArrayOf(1, 0, 0, 1, 1, 1, 1, -1)
        private val W = intArrayOf(0, 1, 8, 60, 700, 100000)
        private const val WIN = 1_000_000
        private const val MAXPLY = 48
        private val geos = HashMap<Int, Geo>()
        private fun geo(size: Int): Geo = synchronized(geos) { geos.getOrPut(size) { Geo(size) } }
        private val zr = Random(0x60A0C1)
        const val MAXW = 24
        private val Z = Array(3) { LongArray(MAXW * MAXW) { zr.nextLong() } }
        private val Z_TURN = zr.nextLong()
        private const val TT_BITS = 18
        private const val TT_SIZE = 1 shl TT_BITS
        private val ttKey = LongArray(TT_SIZE)
        private val ttMove = IntArray(TT_SIZE)
        private val ttScore = IntArray(TT_SIZE)
        private val ttInfo = IntArray(TT_SIZE)
        private val lock = Any()
        @Volatile var lastDepth = 0; private set
        @Volatile var lastNodes = 0L; private set
        @Volatile var lastMode = ""; private set
        /** VCT (ba hở + bốn) đang TẮT: đo A/B cho thấy tốn thời gian hơn giá trị mang lại. Chỉ bật lại khi A/B chứng minh có lợi. */
        private const val USE_VCT = false
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

    override fun reset() { cells.fill(0); hist.clear(); result = Outcome.ONGOING; reason = EndReason.NONE; winLine = IntArray(0) }
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

    /**
     * Nạp thế cờ có sẵn: [owner] (size*size, 0 = trống, 1/2 = chủ quân), [toMove] = 0/1 bên sắp đi.
     * Quân nằm ngoài cửa sổ có thể đã bị bỏ nên nhịp đi được ép theo [toMove] bằng một phần tử đệm (-1) trong lịch sử.
     */
    fun setPosition(owner: IntArray, toMove: Int) {
        reset()
        System.arraycopy(owner, 0, cells, 0, cells.size)
        for (i in cells.indices) if (cells[i] != 0) hist.add(i)
        if (hist.size % 2 != toMove) hist.add(-1)
    }

    override fun forfeit(loser: Int, reason: EndReason) {
        if (result != Outcome.ONGOING) return
        result = if (loser == 0) Outcome.WIN_SECOND else Outcome.WIN_FIRST
        this.reason = reason
    }

    override fun copyGame(): GomokuEngine {
        val g = GomokuEngine(size)
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

    // ═════════════════════════ AI ═════════════════════════
    private lateinit var g: Geo
    private lateinit var c1: IntArray       // số quân đen trong mỗi cửa sổ
    private lateinit var c2: IntArray       // số quân trắng trong mỗi cửa sổ
    private val tot = IntArray(3)           // tổng điểm cửa sổ của từng bên
    private lateinit var nb: IntArray
    private lateinit var actList: Array<IntArray>   // [người chơi] danh sách cửa sổ "đang hoạt động": ≥ 2 quân của mình và 0 quân đối phương
    private lateinit var actPos: Array<IntArray>
    private val actLen = IntArray(3)
    private var zh = 0L
    private var deadline = 0L
    private var nodes = 0L
    private var abort = false
    private lateinit var candBuf: Array<IntArray>
    private lateinit var candScore: Array<IntArray>
    private val tmpW = Array(MAXPLY + 8) { IntArray(8) }
    private val tmpO = Array(MAXPLY + 8) { IntArray(8) }
    private val tmpX = Array(MAXPLY + 8) { IntArray(8) }
    private val tmpF = Array(MAXPLY + 8) { IntArray(48) }
    private val tmpT = Array(MAXPLY + 8) { IntArray(48) }
    private val tmpD = Array(MAXPLY + 8) { IntArray(32) }
    private val sW = Array(MAXPLY + 8) { IntArray(8) }    // dùng riêng cho search() để không đè lên vcf()/vct()
    private val sO = Array(MAXPLY + 8) { IntArray(8) }

    override fun bestMove(timeMs: Long): Int = synchronized(lock) { copyGame().searchRoot(timeMs) }

    private fun prepareAi() {
        g = geo(size)
        c1 = IntArray(g.nWin); c2 = IntArray(g.nWin); nb = IntArray(size * size)
        actList = Array(3) { IntArray(g.nWin) }; actPos = Array(3) { IntArray(g.nWin) { -1 } }; actLen[1] = 0; actLen[2] = 0
        candBuf = Array(MAXPLY) { IntArray(size * size) }; candScore = Array(MAXPLY) { IntArray(size * size) }
        val stones = ArrayList<Int>()
        for (i in cells.indices) if (cells[i] != 0) stones.add(i)
        val saved = IntArray(cells.size); System.arraycopy(cells, 0, saved, 0, cells.size)
        cells.fill(0); tot[1] = 0; tot[2] = 0; zh = 0L
        for (c in stones) aiPut(c, saved[c])
    }

    private fun nbUpdate(c: Int, delta: Int) {
        val cx = c % size; val cy = c / size
        for (dy in -2..2) for (dx in -2..2) { val x = cx + dx; val y = cy + dy; if (inb(x, y)) nb[y * size + x] += delta }
    }

    private fun setAct(p: Int, w: Int, on: Boolean) {
        val pos = actPos[p]
        if (on) {
            if (pos[w] < 0) { pos[w] = actLen[p]; actList[p][actLen[p]++] = w }
        } else if (pos[w] >= 0) {
            val i = pos[w]; val last = actList[p][--actLen[p]]
            actList[p][i] = last; pos[last] = i; pos[w] = -1
        }
    }

    private fun refresh(w: Int) {
        val a = c1[w]; val b = c2[w]
        setAct(1, w, a >= 2 && b == 0)
        setAct(2, w, b >= 2 && a == 0)
    }

    private fun aiPut(c: Int, p: Int) {
        cells[c] = p; zh = zh xor Z[p][c]
        for (w in g.cellWins[c]) {
            val a = c1[w]; val b = c2[w]
            if (b == 0 && a > 0) tot[1] -= W[a]
            if (a == 0 && b > 0) tot[2] -= W[b]
            val na: Int; val nbb: Int
            if (p == 1) { na = a + 1; nbb = b; c1[w] = na } else { na = a; nbb = b + 1; c2[w] = nbb }
            if (nbb == 0 && na > 0) tot[1] += W[na]
            if (na == 0 && nbb > 0) tot[2] += W[nbb]
            refresh(w)
        }
        nbUpdate(c, 1)
    }

    private fun aiTake(c: Int) {
        val p = cells[c]
        for (w in g.cellWins[c]) {
            val a = c1[w]; val b = c2[w]
            if (b == 0 && a > 0) tot[1] -= W[a]
            if (a == 0 && b > 0) tot[2] -= W[b]
            val na: Int; val nbb: Int
            if (p == 1) { na = a - 1; nbb = b; c1[w] = na } else { na = a; nbb = b - 1; c2[w] = nbb }
            if (nbb == 0 && na > 0) tot[1] += W[na]
            if (na == 0 && nbb > 0) tot[2] += W[nbb]
            refresh(w)
        }
        zh = zh xor Z[p][c]; cells[c] = 0
        nbUpdate(c, -1)
    }

    private fun evaluate(p: Int) = tot[p] - tot[3 - p]

    private fun wouldWin(c: Int, p: Int): Boolean {
        cells[c] = p
        val r = winAt(c) != null
        cells[c] = 0
        return r
    }

    private fun contains(a: IntArray, n: Int, v: Int): Boolean { for (i in 0 until n) if (a[i] == v) return true; return false }

    /** Các ô mà người chơi p đặt vào là thắng ngay (đủ 5 hợp lệ). */
    private fun winCells(p: Int, out: IntArray): Int {
        val cp = if (p == 1) c1 else c2
        var n = 0
        val lst = actList[p]
        for (k in 0 until actLen[p]) {
            val w = lst[k]
            if (cp[w] != 4) continue
            val b = w * 5
            for (i in 0 until 5) {
                val c = g.cells[b + i]
                if (cells[c] == 0) { if (n < out.size && !contains(out, n, c) && wouldWin(c, p)) out[n++] = c; break }
            }
        }
        return n
    }

    /** Các ô tạo ra một "bốn" (đe dọa thắng ở nước kế tiếp). */
    private fun fourMoves(p: Int, out: IntArray): Int {
        val cp = if (p == 1) c1 else c2
        var n = 0
        val lst = actList[p]
        for (k in 0 until actLen[p]) {
            val w = lst[k]
            if (cp[w] != 3) continue
            val b = w * 5
            for (i in 0 until 5) {
                val c = g.cells[b + i]
                if (cells[c] == 0 && n < out.size && !contains(out, n, c)) out[n++] = c
            }
        }
        return n
    }

    /** Các ô tạo ra một "ba hở" thực sự (sau khi đặt có ≥ 2 cửa sổ bốn-tiềm-năng đi qua ô đó). */
    private fun threeMoves(p: Int, out: IntArray, scratch: IntArray): Int {
        val cp = if (p == 1) c1 else c2; val co = if (p == 1) c2 else c1
        var cand = 0
        val lst = actList[p]
        for (k in 0 until actLen[p]) {
            val w = lst[k]
            if (cp[w] != 2) continue
            val b = w * 5
            for (i in 0 until 5) {
                val c = g.cells[b + i]
                if (cells[c] == 0 && cand < scratch.size && !contains(scratch, cand, c)) scratch[cand++] = c
            }
        }
        var n = 0
        for (k in 0 until cand) {
            val c = scratch[k]
            aiPut(c, p)
            var live = 0
            for (w in g.cellWins[c]) if (cp[w] == 3 && co[w] == 0) live++
            aiTake(c)
            if (live >= 2 && n < out.size) out[n++] = c
        }
        return n
    }

    /** Sau khi p vừa đặt một "ba": p có nước tạo bốn-hở/bốn-kép (≥ 2 ô thắng) nếu đối phương bỏ lượt không? */
    private fun hasOpenFourThreat(p: Int, depth: Int): Boolean {
        val fc = IntArray(32); val nf = fourMoves(p, fc)
        val w = tmpX[depth]
        for (i in 0 until nf) {
            aiPut(fc[i], p)
            val nw = winCells(p, w)
            aiTake(fc[i])
            if (nw >= 2) return true
        }
        return false
    }

    // ── VCF: chuỗi nước "bốn" liên tục buộc đối phương chỉ có một cách đỡ ──
    private var vNodes = 0
    private var vLimit = 0
    private var vDeadline = 0L
    private var vMove = -1

    private fun vcf(p: Int, depth: Int): Boolean {
        if (++vNodes > vLimit || ((vNodes and 63) == 0 && System.nanoTime() > vDeadline)) return false
        val o = 3 - p
        val wp = tmpW[depth]
        if (winCells(p, wp) > 0) { vMove = wp[0]; return true }
        if (depth <= 0) return false
        val wo = tmpO[depth]
        val on = winCells(o, wo)
        if (on >= 2) return false
        val fc = tmpF[depth]
        val nf = fourMoves(p, fc)
        for (k in 0 until nf) {
            val c = fc[k]
            if (on == 1 && c != wo[0]) continue
            aiPut(c, p)
            val w2 = tmpX[depth]
            val nw = winCells(p, w2)
            var ok = false
            if (nw >= 2) ok = true
            else if (nw == 1) {
                val r = w2[0]
                aiPut(r, o)
                if (winAt(r) == null) ok = vcf(p, depth - 1)
                aiTake(r)
            }
            aiTake(c)
            if (ok) { vMove = c; return true }
            if (vNodes > vLimit) return false
        }
        return false
    }

    // ── VCT: thêm các nước "ba hở"; đối phương có nhiều cách đỡ nên phải thắng ở mọi nhánh ──
    private fun vct(p: Int, depth: Int): Boolean {
        if (++vNodes > vLimit || ((vNodes and 63) == 0 && System.nanoTime() > vDeadline)) return false
        val o = 3 - p
        val wp = tmpW[depth]
        if (winCells(p, wp) > 0) { vMove = wp[0]; return true }
        if (depth <= 0) return false
        val wo = tmpO[depth]
        val on = winCells(o, wo)
        if (on >= 2) return false
        val fc = tmpF[depth]; val tc = tmpT[depth]
        val nf = fourMoves(p, fc)
        val nt = if (on == 0) threeMoves(p, tc, tmpD[depth]) else 0
        for (k in 0 until nf + nt) {
            val isFour = k < nf
            val c = if (isFour) fc[k] else tc[k - nf]
            if (on == 1 && c != wo[0]) continue
            aiPut(c, p)
            var ok: Boolean
            val w2 = tmpX[depth]
            val nw = winCells(p, w2)
            if (nw >= 2) ok = true
            else if (nw == 1) {
                val r = w2[0]
                aiPut(r, o)
                ok = winAt(r) == null && vct(p, depth - 1)
                aiTake(r)
            } else if (!hasOpenFourThreat(p, depth)) {
                ok = false   // không phải đe dọa thật: đối phương có thể đi chỗ khác
            } else {
                // ba hở thật: đối phương phải đỡ bằng một ô thuộc các cửa sổ ≥ 2 quân của p đi qua c, hoặc phản công bằng "bốn"
                val cp = if (p == 1) c1 else c2; val co = if (p == 1) c2 else c1
                val ds = tmpD[depth]; var nd = 0
                for (w in g.cellWins[c]) {
                    if (cp[w] >= 2 && co[w] == 0) for (i in 0 until 5) {
                        val e = g.cells[w * 5 + i]
                        if (cells[e] == 0 && nd < ds.size && !contains(ds, nd, e)) ds[nd++] = e
                    }
                }
                val of = IntArray(24); val nof = fourMoves(o, of)
                for (i in 0 until nof) if (nd < ds.size && !contains(ds, nd, of[i])) ds[nd++] = of[i]
                val dl = ds.copyOf(nd)
                ok = nd > 0
                for (d in dl) {
                    aiPut(d, o)
                    val dWin = winAt(d) != null
                    val res = !dWin && vct(p, depth - 1)
                    aiTake(d)
                    if (!res) { ok = false; break }
                    if (vNodes > vLimit) { ok = false; break }
                }
            }
            aiTake(c)
            if (ok) { vMove = c; return true }
            if (vNodes > vLimit) return false
        }
        return false
    }

    // ── Sinh ứng viên và sắp xếp theo mức "ảnh hưởng" ──
    private fun gain(c: Int, p: Int): Int {
        val cp = if (p == 1) c1 else c2; val co = if (p == 1) c2 else c1
        var s = 0
        for (w in g.cellWins[c]) if (co[w] == 0) { val a = cp[w]; s += W[a + 1] - W[a] }
        return s
    }

    private fun genCands(ply: Int, p: Int, noise: Boolean): Int {
        val buf = candBuf[ply]; val sc = candScore[ply]
        var n = 0
        val o = 3 - p
        for (c in cells.indices) {
            if (cells[c] != 0 || nb[c] == 0) continue
            var s = gain(c, p) + gain(c, o) * 9 / 10
            if (noise) s += Random.nextInt(0, 3)
            buf[n] = c; sc[n] = s; n++
        }
        for (i in 1 until n) {
            val c = buf[i]; val s = sc[i]; var j = i - 1
            while (j >= 0 && sc[j] < s) { buf[j + 1] = buf[j]; sc[j + 1] = sc[j]; j-- }
            buf[j + 1] = c; sc[j + 1] = s
        }
        return n
    }

    private fun search(depth: Int, alphaIn: Int, betaIn: Int, p: Int, ply: Int): Int {
        var alpha = alphaIn; val beta = betaIn
        if ((++nodes and 15L) == 0L && (nodes >= nodeLimit || System.nanoTime() > deadline)) abort = true
        if (abort) return 0
        val o = 3 - p
        val wp = sW[ply]
        if (winCells(p, wp) > 0) return WIN - ply
        val wo = sO[ply]
        val on = winCells(o, wo)
        if (on >= 2) return -(WIN - ply - 1)
        if (depth <= 0 || ply >= MAXPLY - 2) return evaluate(p)

        val key = zh xor (if (p == 2) Z_TURN else 0L)
        val slot = (key and (TT_SIZE - 1).toLong()).toInt()
        var ttm = -1
        if (ttKey[slot] == key) {
            ttm = ttMove[slot]
            val td = ttInfo[slot] shr 2; val fl = ttInfo[slot] and 3
            if (td >= depth) {
                val s = ttScore[slot]
                if (fl == 0 || (fl == 1 && s >= beta) || (fl == 2 && s <= alpha)) return s
            }
        }
        // chuỗi đe dọa nhanh cho bên đang đi
        if (on == 0 && depth >= 2) {
            vNodes = 0; vLimit = 80; vDeadline = deadline
            if (actLen[p] > 0 && vcf(p, 8)) return WIN - ply - 2
        }
        val limit: Int; val buf = candBuf[ply]
        var n: Int
        if (on == 1) { buf[0] = wo[0]; n = 1; limit = 1 }
        else {
            n = genCands(ply, p, false)
            limit = minOf(n, if (depth >= 5) 12 else if (depth >= 3) 10 else 8)
            if (ttm >= 0) {   // đưa nước của bảng băm lên đầu
                val idx = (0 until n).firstOrNull { buf[it] == ttm }
                if (idx != null && idx > 0) { System.arraycopy(buf, 0, buf, 1, idx); buf[0] = ttm }
            }
        }
        if (n == 0) return 0
        var best = -WIN * 2; var bestMove = -1
        val origAlpha = alpha
        for (k in 0 until limit) {
            val c = buf[k]
            aiPut(c, p)
            val s = -search(depth - 1, -beta, -alpha, o, ply + 1)
            aiTake(c)
            if (abort) return 0
            if (s > best) { best = s; bestMove = c }
            if (s > alpha) alpha = s
            if (alpha >= beta) break
        }
        val flag = if (best >= beta) 1 else if (best > origAlpha) 0 else 2
        ttKey[slot] = key; ttMove[slot] = bestMove; ttScore[slot] = best; ttInfo[slot] = (depth shl 2) or flag
        return best
    }

    private var nodeLimit = Long.MAX_VALUE

    private fun searchRoot(timeMs: Long): Int = levelRoot(SearchLimit(timeMs = timeMs), 24, true, 0).best

    /**
     * Tìm nước ở gốc theo cấp (BOT_GUIDE mục 7.4): luôn thắng ngay / chặn thắng ngay (lớp an toàn), rồi VCF của mình
     * tới độ sâu [vcfDepth] (0 = tắt), phòng VCF của đối phương nếu [defend], cuối cùng alpha-beta trong giới hạn [lim].
     * [delta] > 0: trả thêm các nước gốc có điểm kém nước tốt nhất không quá [delta] (để chọn nhiễu).
     */
    private fun levelRoot(lim: SearchLimit, vcfDepth: Int, defend: Boolean, delta: Int): RootResult {
        val start = System.nanoTime()
        val timeMs = lim.timeMs
        deadline = if (timeMs > 0) start + timeMs * 1_000_000 else Long.MAX_VALUE
        fun frac(f: Double) = if (timeMs > 0) start + (timeMs * 1_000_000 * f).toLong() else Long.MAX_VALUE
        val nl = if (lim.nodes > 0) lim.nodes else Long.MAX_VALUE
        val maxDepth = if (lim.depth > 0) lim.depth else 14
        abort = false; nodes = 0
        val stones = hist.size
        val mid = (size / 2) * size + size / 2
        lastMode = "book"
        if (stones == 0) return RootResult(mid, 0, 0, 0)
        if (stones == 1) {
            val opts = ArrayList<Int>()
            val c0 = hist[0]
            for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val x = c0 % size + dx; val y = c0 / size + dy
                if (inb(x, y)) opts.add(y * size + x)
            }
            return RootResult(opts[Random.nextInt(opts.size)], 0, 0, 0)
        }
        prepareAi()
        val me = turn + 1; val op = 3 - me
        // 1) thắng ngay / 2) chặn thắng ngay (mọi cấp)
        val w0 = IntArray(8)
        if (winCells(me, w0) > 0) { lastMode = "win"; return RootResult(w0[0], WIN, 0, 0) }
        val on0 = winCells(op, w0)
        if (on0 >= 1) {
            lastMode = "block"
            if (on0 == 1) return RootResult(w0[0], 0, 0, 0)
        }
        // 3) VCF của mình, rồi VCT (chỉ khi bật)
        if (vcfDepth > 0) {
            vNodes = 0; vLimit = 400_000; vDeadline = frac(0.10); vMove = -1
            if (on0 == 0 && vcf(me, vcfDepth) && vMove >= 0) { lastMode = "vcf"; return RootResult(vMove, WIN, 0, 0) }
            var depthT = 2
            vDeadline = frac(0.20)
            while (USE_VCT && on0 == 0 && depthT <= 8 && System.nanoTime() < vDeadline) {
                vNodes = 0; vLimit = 60_000 * depthT; vMove = -1
                if (vct(me, depthT) && vMove >= 0) { lastMode = "vct"; return RootResult(vMove, WIN, 0, 0) }
                depthT += 2
            }
        }
        // 4) phòng thủ: nếu đối phương có VCF thì chỉ xét các nước phá được VCF (kết quả "hết giờ/hết nút" không được coi là an toàn)
        val n = genCands(0, me, true)
        var cand = candBuf[0].copyOf(minOf(n, 40))
        if (defend) {
            val defEnd = frac(0.35)
            vNodes = 0; vLimit = 20_000; vDeadline = defEnd
            if (actLen[op] > 0 && vcf(op, 16)) {
                val safe = ArrayList<Int>()
                for (c in cand) {
                    if (System.nanoTime() > defEnd) break
                    aiPut(c, me)
                    vNodes = 0; vLimit = 6_000; vDeadline = defEnd
                    val opVcf = vcf(op, 16)
                    val exhausted = vNodes > vLimit || System.nanoTime() > defEnd
                    aiTake(c)
                    if (!opVcf && !exhausted) safe.add(c)
                }
                if (safe.isNotEmpty()) { cand = safe.toIntArray(); lastMode = "defend" }
            }
        }
        if (cand.size == 1) return RootResult(cand[0], 0, 0, 0)
        if (lastMode != "defend") lastMode = "search"
        // 5) alpha-beta lặp sâu trên các ứng viên gốc
        val limit = minOf(cand.size, 16)
        val root = cand.copyOf(limit)
        var best = root[0]; var bestScore = 0; var completed = 0
        for (depth in 1..maxDepth) {
            nodeLimit = if (depth == 1) Long.MAX_VALUE else nl
            var alpha = -WIN * 2; var iterBest = -1; var iterScore = -WIN * 2
            for (k in 0 until limit) {
                val c = root[k]
                aiPut(c, me)
                val s = -search(depth - 1, -WIN * 2, -alpha, op, 1)
                aiTake(c)
                if (abort) break
                if (s > iterScore) { iterScore = s; iterBest = c }
                if (s > alpha) alpha = s
            }
            if (abort) break
            best = iterBest; bestScore = iterScore; completed = depth; lastDepth = depth
            val bi = root.indexOf(best)
            if (bi > 0) { System.arraycopy(root, 0, root, 1, bi); root[0] = best }
            if (iterScore >= WIN - 100 || iterScore <= -WIN + 100) break
            if (timeMs > 0 && System.nanoTime() - start > (deadline - start) * 0.6) break
        }
        lastNodes = nodes
        var near = IntArray(0)
        if (delta > 0 && completed > 0) {
            abort = false; nodeLimit = Long.MAX_VALUE; deadline = Long.MAX_VALUE
            val thr = bestScore - delta
            val out = ArrayList<Int>()
            for (k in 0 until limit) {
                val c = root[k]
                if (c == best) continue
                aiPut(c, me)
                val s = -search(completed - 1, -thr, -thr + 1, op, 1)
                aiTake(c)
                if (s >= thr) out.add(c)
            }
            near = out.toIntArray()
        }
        return RootResult(best, bestScore, completed, nodes, near)
    }

    /** Nước theo cấp trên bàn cửa sổ này (GomokuGame dịch tọa độ). */
    fun levelSearch(cfg: LevelConfig, level: BotLevel): RootResult = synchronized(lock) {
        copyGame().levelRoot(cfg.limit, cfg.extra, level >= BotLevel.STRONG, if (cfg.noiseP > 0) cfg.delta else 0)
    }
}
