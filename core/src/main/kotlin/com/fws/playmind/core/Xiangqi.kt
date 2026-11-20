package com.fws.playmind.core

import kotlin.math.abs

/**
 * Cờ tướng (Xiangqi). Bàn 9 cột x 10 hàng, ô = rank*9 + file, rank 0 là đáy bên Đỏ.
 * Đỏ (dương) đi trước. Mã hóa nước đi: from | to<<7.
 * Quy tắc hòa đơn giản hóa: lặp lại 3 lần hoặc 120 nước không ăn quân.
 */
class XiangqiGame : BoardGame {
    companion object {
        const val K = 1; const val A = 2; const val E = 3; const val H = 4; const val R = 5; const val C = 6; const val P = 7
        private const val MATE = 100000
        private const val INF = 1_000_000
        private const val MAXPLY = 80
        private val VAL = intArrayOf(0, 0, 200, 200, 450, 1000, 480, 100)
        private val ORTH = intArrayOf(1, 0, -1, 0, 0, 1, 0, -1)
        private val DIAG = intArrayOf(1, 1, 1, -1, -1, 1, -1, -1)
        // df, dr, legdf, legdr
        private val HORSE = intArrayOf(1, 2, 0, 1, -1, 2, 0, 1, 1, -2, 0, -1, -1, -2, 0, -1, 2, 1, 1, 0, 2, -1, 1, 0, -2, 1, -1, 0, -2, -1, -1, 0)

        fun fromSq(m: Int) = m and 127
        fun toSq(m: Int) = (m shr 7) and 127
        fun mk(f: Int, t: Int) = f or (t shl 7)
    }

    override val id = "xiangqi"
    val board = IntArray(90)
    var side = 1; private set
    private var quiet = 0
    private var rk = 4
    private var bk = 85

    private var uMove = IntArray(512); private var uCap = IntArray(512); private var uQuiet = IntArray(512)
    private var sp = 0
    private val hist = ArrayList<Int>()
    override val history: List<Int> get() = hist
    private val keys = ArrayList<Long>()
    override var result = Outcome.ONGOING; private set
    override var reason = EndReason.NONE; private set
    override val turn: Int get() = if (side > 0) 0 else 1
    override val plies: Int get() = hist.size
    override val lastMove: Int get() = if (hist.isEmpty()) -1 else hist[hist.size - 1]

    init { reset() }

    override fun reset() {
        board.fill(0); hist.clear(); keys.clear(); sp = 0; quiet = 0
        result = Outcome.ONGOING; reason = EndReason.NONE; side = 1
        val back = intArrayOf(R, H, E, A, K, A, E, H, R)
        for (f in 0 until 9) { board[f] = back[f]; board[81 + f] = -back[f] }
        board[2 * 9 + 1] = C; board[2 * 9 + 7] = C; board[7 * 9 + 1] = -C; board[7 * 9 + 7] = -C
        for (f in 0 until 9 step 2) { board[3 * 9 + f] = P; board[6 * 9 + f] = -P }
        rk = 4; bk = 85
        keys.add(key())
    }

    /** Dùng cho kiểm thử: thiết lập thế cờ từ mảng quân. */
    fun setPosition(pieces: Map<Int, Int>, redToMove: Boolean) {
        board.fill(0); hist.clear(); keys.clear(); sp = 0; quiet = 0
        result = Outcome.ONGOING; reason = EndReason.NONE
        for ((sq, p) in pieces) { board[sq] = p; if (p == K) rk = sq; if (p == -K) bk = sq }
        side = if (redToMove) 1 else -1
        keys.add(key()); updateResult()
    }

    private fun key(): Long {
        var h = 1469598103934665603L
        for (i in 0 until 90) h = (h xor (board[i] + 9L)) * 1099511628211L
        return (h xor (side + 2L)) * 1099511628211L
    }

    fun pieceAt(sq: Int) = board[sq]
    fun kingSquare(color: Int) = if (color > 0) rk else bk
    override fun inCheck() = isAttacked(kingSquare(side), -side)

    private fun inPalace(f: Int, r: Int, color: Int) = f in 3..5 && (if (color > 0) r in 0..2 else r in 7..9)

    /** Ô [sq] có bị bên [by] (1 = Đỏ, -1 = Đen) chiếu tới không (kể cả tướng đối mặt). */
    fun isAttacked(sq: Int, by: Int): Boolean {
        val f = sq % 9; val r = sq / 9
        // xe, tướng đối mặt, pháo
        var d = 0
        while (d < 8) {
            val df = ORTH[d]; val dr = ORTH[d + 1]; d += 2
            var nf = f + df; var nr = r + dr
            var first = true
            while (nf in 0..8 && nr in 0..9) {
                val p = board[nr * 9 + nf]
                if (p != 0) {
                    if (first) {
                        if (p == by * R) return true
                        if (p == by * K && df == 0) return true
                        first = false
                    } else {
                        if (p == by * C) return true
                        break
                    }
                }
                nf += df; nr += dr
            }
        }
        // mã
        var i = 0
        while (i < HORSE.size) {
            val df = HORSE[i]; val dr = HORSE[i + 1]; val lf = HORSE[i + 2]; val lr = HORSE[i + 3]
            i += 4
            val hf = f - df; val hr = r - dr
            if (hf in 0..8 && hr in 0..9 && board[hr * 9 + hf] == by * H && board[(hr + lr) * 9 + (hf + lf)] == 0) return true
        }
        // tốt
        val pr = r - by
        if (pr in 0..9 && board[pr * 9 + f] == by * P) return true
        if (if (by > 0) r >= 5 else r <= 4) {
            if (f > 0 && board[r * 9 + f - 1] == by * P) return true
            if (f < 8 && board[r * 9 + f + 1] == by * P) return true
        }
        return false
    }

    private fun gen(out: IntArray, caps: Boolean): Int {
        var n = 0
        val us = side
        for (sq in 0 until 90) {
            val p = board[sq]
            if (p == 0 || (p > 0) != (us > 0)) continue
            val t = abs(p); val f = sq % 9; val r = sq / 9
            when (t) {
                K -> {
                    var d = 0
                    while (d < 8) {
                        val nf = f + ORTH[d]; val nr = r + ORTH[d + 1]; d += 2
                        if (nf in 0..8 && nr in 0..9 && inPalace(nf, nr, us)) {
                            val tp = board[nr * 9 + nf]
                            if (tp == 0) { if (!caps) out[n++] = sq or ((nr * 9 + nf) shl 7) }
                            else if ((tp > 0) != (us > 0)) out[n++] = sq or ((nr * 9 + nf) shl 7)
                        }
                    }
                }
                A -> {
                    var d = 0
                    while (d < 8) {
                        val nf = f + DIAG[d]; val nr = r + DIAG[d + 1]; d += 2
                        if (nf in 0..8 && nr in 0..9 && inPalace(nf, nr, us)) {
                            val tp = board[nr * 9 + nf]
                            if (tp == 0) { if (!caps) out[n++] = sq or ((nr * 9 + nf) shl 7) }
                            else if ((tp > 0) != (us > 0)) out[n++] = sq or ((nr * 9 + nf) shl 7)
                        }
                    }
                }
                E -> {
                    var d = 0
                    while (d < 8) {
                        val df = DIAG[d]; val dr = DIAG[d + 1]; d += 2
                        val nf = f + 2 * df; val nr = r + 2 * dr
                        if (nf !in 0..8 || nr !in 0..9) continue
                        if (if (us > 0) nr > 4 else nr < 5) continue
                        if (board[(r + dr) * 9 + (f + df)] != 0) continue
                        val tp = board[nr * 9 + nf]
                        if (tp == 0) { if (!caps) out[n++] = sq or ((nr * 9 + nf) shl 7) }
                        else if ((tp > 0) != (us > 0)) out[n++] = sq or ((nr * 9 + nf) shl 7)
                    }
                }
                H -> {
                    var i = 0
                    while (i < HORSE.size) {
                        val nf = f + HORSE[i]; val nr = r + HORSE[i + 1]
                        val lf = f + HORSE[i + 2]; val lr = r + HORSE[i + 3]
                        i += 4
                        if (nf !in 0..8 || nr !in 0..9) continue
                        if (board[lr * 9 + lf] != 0) continue
                        val tp = board[nr * 9 + nf]
                        if (tp == 0) { if (!caps) out[n++] = sq or ((nr * 9 + nf) shl 7) }
                        else if ((tp > 0) != (us > 0)) out[n++] = sq or ((nr * 9 + nf) shl 7)
                    }
                }
                R -> {
                    var d = 0
                    while (d < 8) {
                        val df = ORTH[d]; val dr = ORTH[d + 1]; d += 2
                        var nf = f + df; var nr = r + dr
                        while (nf in 0..8 && nr in 0..9) {
                            val tp = board[nr * 9 + nf]
                            if (tp == 0) { if (!caps) out[n++] = sq or ((nr * 9 + nf) shl 7) }
                            else { if ((tp > 0) != (us > 0)) out[n++] = sq or ((nr * 9 + nf) shl 7); break }
                            nf += df; nr += dr
                        }
                    }
                }
                C -> {
                    var d = 0
                    while (d < 8) {
                        val df = ORTH[d]; val dr = ORTH[d + 1]; d += 2
                        var nf = f + df; var nr = r + dr
                        var screen = false
                        while (nf in 0..8 && nr in 0..9) {
                            val tp = board[nr * 9 + nf]
                            if (!screen) {
                                if (tp == 0) { if (!caps) out[n++] = sq or ((nr * 9 + nf) shl 7) } else screen = true
                            } else if (tp != 0) {
                                if ((tp > 0) != (us > 0)) out[n++] = sq or ((nr * 9 + nf) shl 7)
                                break
                            }
                            nf += df; nr += dr
                        }
                    }
                }
                P -> {
                    val nr = r + us
                    if (nr in 0..9) {
                        val tp = board[nr * 9 + f]
                        if (tp == 0) { if (!caps) out[n++] = sq or ((nr * 9 + f) shl 7) }
                        else if ((tp > 0) != (us > 0)) out[n++] = sq or ((nr * 9 + f) shl 7)
                    }
                    if (if (us > 0) r >= 5 else r <= 4) {
                        var df = -1
                        while (df <= 1) {
                            val nf = f + df
                            if (nf in 0..8) {
                                val tp = board[r * 9 + nf]
                                if (tp == 0) { if (!caps) out[n++] = sq or ((r * 9 + nf) shl 7) }
                                else if ((tp > 0) != (us > 0)) out[n++] = sq or ((r * 9 + nf) shl 7)
                            }
                            df += 2
                        }
                    }
                }
            }
        }
        return n
    }

    private fun make(m: Int) {
        if (sp >= uMove.size) { val ns = uMove.size * 2; uMove = uMove.copyOf(ns); uCap = uCap.copyOf(ns); uQuiet = uQuiet.copyOf(ns) }
        val from = m and 127; val to = (m shr 7) and 127
        val p = board[from]; val cap = board[to]
        uMove[sp] = m; uCap[sp] = cap; uQuiet[sp] = quiet; sp++
        board[to] = p; board[from] = 0
        if (p == K) rk = to else if (p == -K) bk = to
        quiet = if (cap != 0) 0 else quiet + 1
        side = -side
    }

    private fun unmake() {
        sp--
        val m = uMove[sp]; val from = m and 127; val to = (m shr 7) and 127
        side = -side
        val p = board[to]
        board[from] = p; board[to] = uCap[sp]; quiet = uQuiet[sp]
        if (p == K) rk = from else if (p == -K) bk = from
    }

    private fun legalList(): IntArray {
        val buf = IntArray(128)
        val n = gen(buf, false)
        val out = IntArray(n); var c = 0
        val us = side
        for (i in 0 until n) {
            make(buf[i])
            if (!isAttacked(kingSquare(us), -us)) out[c++] = buf[i]
            unmake()
        }
        return out.copyOf(c)
    }

    override fun legalMoves(): IntArray = if (result != Outcome.ONGOING) IntArray(0) else legalList()

    fun findMove(f: Int, t: Int): Int {
        val m = mk(f, t)
        return if (m in legalMoves()) m else -1
    }

    override fun play(move: Int): Boolean {
        if (result != Outcome.ONGOING) return false
        if (move !in legalList()) return false
        make(move); hist.add(move); keys.add(key())
        updateResult()
        return true
    }

    private fun updateResult() {
        if (legalList().isEmpty()) {
            result = if (side > 0) Outcome.WIN_SECOND else Outcome.WIN_FIRST
            reason = if (inCheck()) EndReason.CHECKMATE else EndReason.NOMOVES
            return
        }
        if (quiet >= 120) { result = Outcome.DRAW; reason = EndReason.FIFTY; return }
        val k = keys[keys.size - 1]
        var c = 0
        for (x in keys) if (x == k) c++
        if (c >= 3) { result = Outcome.DRAW; reason = EndReason.REPETITION }
    }

    override fun forfeit(loser: Int, reason: EndReason) {
        if (result != Outcome.ONGOING) return
        result = if (loser == 0) Outcome.WIN_SECOND else Outcome.WIN_FIRST
        this.reason = reason
    }

    override fun copyGame(): XiangqiGame {
        val g = XiangqiGame()
        System.arraycopy(board, 0, g.board, 0, 90)
        g.side = side; g.quiet = quiet; g.rk = rk; g.bk = bk
        g.hist.clear(); g.hist.addAll(hist); g.keys.clear(); g.keys.addAll(keys)
        g.result = result; g.reason = reason; g.sp = 0
        return g
    }

    fun perft(depth: Int): Long {
        if (depth == 0) return 1
        val buf = IntArray(128)
        val n = gen(buf, false)
        var total = 0L
        val us = side
        for (i in 0 until n) {
            make(buf[i])
            if (!isAttacked(kingSquare(us), -us)) total += if (depth == 1) 1 else perft(depth - 1)
            unmake()
        }
        return total
    }

    // ───────────────────────── AI ─────────────────────────
    private var deadline = 0L
    private var nodes = 0L
    private var abort = false
    private val killers = Array(MAXPLY) { IntArray(2) }
    private val bufs = Array(MAXPLY) { IntArray(128) }
    private val scores = Array(MAXPLY) { IntArray(128) }

    override fun bestMove(timeMs: Long): Int = copyGame().searchRoot(timeMs)

    private fun evaluate(): Int {
        var s = 0
        for (sq in 0 until 90) {
            val p = board[sq]; if (p == 0) continue
            val t = abs(p); val red = p > 0
            val f = sq % 9; val r = sq / 9
            val rr = if (red) r else 9 - r
            val cen = 4 - abs(f - 4)
            var v = VAL[t]
            when (t) {
                P -> if (rr >= 5) v += 100 + (rr - 5) * 12 + cen * 6 else v += cen
                H -> v += cen * 7 + minOf(rr, 6) * 5
                C -> v += cen * 4 + (if (rr in 2..7) 8 else 0)
                R -> v += rr * 3 + (if (cen == 4) 6 else 0)
                A, E -> v += 0
            }
            s += if (red) v else -v
        }
        return if (side > 0) s else -s
    }

    private fun order(ply: Int, n: Int) {
        val mv = bufs[ply]; val sc = scores[ply]
        for (i in 0 until n) {
            val m = mv[i]; val cap = board[(m shr 7) and 127]
            var s = 0
            if (cap != 0) s = 10000 + VAL[abs(cap)] * 10 - VAL[abs(board[m and 127])] / 10
            else if (killers[ply][0] == m) s = 8000 else if (killers[ply][1] == m) s = 7900
            sc[i] = s
        }
    }

    private fun pick(ply: Int, n: Int, i: Int) {
        val mv = bufs[ply]; val sc = scores[ply]
        var best = i
        for (j in i + 1 until n) if (sc[j] > sc[best]) best = j
        if (best != i) {
            val tm = mv[i]; mv[i] = mv[best]; mv[best] = tm
            val ts = sc[i]; sc[i] = sc[best]; sc[best] = ts
        }
    }

    private fun quiesce(alphaIn: Int, beta: Int, ply: Int): Int {
        var alpha = alphaIn
        if ((++nodes and 2047L) == 0L && System.nanoTime() > deadline) abort = true
        if (abort) return 0
        val stand = evaluate()
        if (ply >= MAXPLY - 2) return stand
        if (stand >= beta) return beta
        if (stand > alpha) alpha = stand
        val n = gen(bufs[ply], true)
        order(ply, n)
        val us = side
        for (i in 0 until n) {
            pick(ply, n, i)
            make(bufs[ply][i])
            if (isAttacked(kingSquare(us), -us)) { unmake(); continue }
            val sc = -quiesce(-beta, -alpha, ply + 1)
            unmake()
            if (abort) return 0
            if (sc >= beta) return beta
            if (sc > alpha) alpha = sc
        }
        return alpha
    }

    private fun negamax(depthIn: Int, alphaIn: Int, beta: Int, ply: Int): Int {
        var alpha = alphaIn; var depth = depthIn
        if ((++nodes and 2047L) == 0L && System.nanoTime() > deadline) abort = true
        if (abort) return 0
        if (quiet >= 120) return 0
        val us = side
        val inChk = isAttacked(kingSquare(us), -us)
        if (inChk && ply < MAXPLY - 8) depth++
        if (depth <= 0 || ply >= MAXPLY - 4) return quiesce(alpha, beta, ply)
        val n = gen(bufs[ply], false)
        order(ply, n)
        var legal = 0
        for (i in 0 until n) {
            pick(ply, n, i)
            val m = bufs[ply][i]
            make(m)
            if (isAttacked(kingSquare(us), -us)) { unmake(); continue }
            legal++
            val sc = -negamax(depth - 1, -beta, -alpha, ply + 1)
            unmake()
            if (abort) return 0
            if (sc >= beta) {
                if (board[(m shr 7) and 127] == 0 && killers[ply][0] != m) { killers[ply][1] = killers[ply][0]; killers[ply][0] = m }
                return beta
            }
            if (sc > alpha) alpha = sc
        }
        if (legal == 0) return -(MATE - ply)
        return alpha
    }

    private fun searchRoot(timeMs: Long): Int {
        val start = System.nanoTime()
        deadline = start + timeMs * 1_000_000
        abort = false; nodes = 0
        val root = legalList()
        if (root.isEmpty()) return -1
        if (root.size == 1) return root[0]
        var best = root[0]
        val rsc = IntArray(root.size)
        for (depth in 1..30) {
            var alpha = -INF; var iterBest = -1; var iterScore = -INF
            for (i in root.indices) {
                val m = root[i]; val cap = board[(m shr 7) and 127]
                rsc[i] = (if (m == best) 1_000_000 else 0) + (if (cap != 0) VAL[abs(cap)] else 0)
            }
            val idx = root.indices.sortedByDescending { rsc[it] }
            val sorted = IntArray(root.size) { root[idx[it]] }
            for (m in sorted) {
                make(m)
                val sc = -negamax(depth - 1, -INF, -alpha, 1)
                unmake()
                if (abort) break
                if (sc > iterScore) { iterScore = sc; iterBest = m }
                if (sc > alpha) alpha = sc
            }
            if (abort) break
            best = iterBest
            for (i in root.indices) root[i] = sorted[i]
            if (iterScore > MATE - 200) break
            if (System.nanoTime() - start > (deadline - start) * 0.45) break
        }
        return best
    }
}
