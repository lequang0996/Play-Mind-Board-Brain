package com.fws.playmind.core

import kotlin.math.abs

/**
 * Cờ vua. Ô = rank*8 + file, a1 = 0, h8 = 63. Quân trắng dương, đen âm.
 * Mã hóa nước đi: from | to<<6 | promo<<12 | flag<<16.
 */
class ChessGame : BoardGame {
    companion object {
        const val P = 1; const val N = 2; const val B = 3; const val R = 4; const val Q = 5; const val K = 6
        private const val F_EP = 1; private const val F_CASTLE = 2; private const val F_DOUBLE = 4
        private const val MATE = 100000
        private const val INF = 1_000_000
        private const val MAXPLY = 96
        private val VAL = intArrayOf(0, 100, 320, 330, 500, 900, 0)
        private val KN = intArrayOf(1, 2, 2, 1, 2, -1, 1, -2, -1, 2, -2, 1, -2, -1, -1, -2)
        private val KG = intArrayOf(1, 0, -1, 0, 0, 1, 0, -1, 1, 1, 1, -1, -1, 1, -1, -1)
        private val CMASK = IntArray(64) { 15 }.also {
            it[0] = 15 and 2.inv(); it[7] = 15 and 1.inv(); it[4] = 15 and 3.inv()
            it[56] = 15 and 8.inv(); it[63] = 15 and 4.inv(); it[60] = 15 and 12.inv()
        }

        fun fromSq(m: Int) = m and 63
        fun toSq(m: Int) = (m shr 6) and 63
        fun promoOf(m: Int) = (m shr 12) and 7
        fun mk(from: Int, to: Int, promo: Int = 0) = from or (to shl 6) or (promo shl 12)

        private val PST = arrayOf(
            IntArray(64),
            intArrayOf(0,0,0,0,0,0,0,0, 50,50,50,50,50,50,50,50, 10,10,20,30,30,20,10,10, 5,5,10,25,25,10,5,5,
                0,0,0,20,20,0,0,0, 5,-5,-10,0,0,-10,-5,5, 5,10,10,-20,-20,10,10,5, 0,0,0,0,0,0,0,0),
            intArrayOf(-50,-40,-30,-30,-30,-30,-40,-50, -40,-20,0,0,0,0,-20,-40, -30,0,10,15,15,10,0,-30, -30,5,15,20,20,15,5,-30,
                -30,0,15,20,20,15,0,-30, -30,5,10,15,15,10,5,-30, -40,-20,0,5,5,0,-20,-40, -50,-40,-30,-30,-30,-30,-40,-50),
            intArrayOf(-20,-10,-10,-10,-10,-10,-10,-20, -10,0,0,0,0,0,0,-10, -10,0,5,10,10,5,0,-10, -10,5,5,10,10,5,5,-10,
                -10,0,10,10,10,10,0,-10, -10,10,10,10,10,10,10,-10, -10,5,0,0,0,0,5,-10, -20,-10,-10,-10,-10,-10,-10,-20),
            intArrayOf(0,0,0,0,0,0,0,0, 5,10,10,10,10,10,10,5, -5,0,0,0,0,0,0,-5, -5,0,0,0,0,0,0,-5,
                -5,0,0,0,0,0,0,-5, -5,0,0,0,0,0,0,-5, -5,0,0,0,0,0,0,-5, 0,0,0,5,5,0,0,0),
            intArrayOf(-20,-10,-10,-5,-5,-10,-10,-20, -10,0,0,0,0,0,0,-10, -10,0,5,5,5,5,0,-10, -5,0,5,5,5,5,0,-5,
                0,0,5,5,5,5,0,-5, -10,5,5,5,5,5,0,-10, -10,0,5,0,0,0,0,-10, -20,-10,-10,-5,-5,-10,-10,-20),
            intArrayOf(-30,-40,-40,-50,-50,-40,-40,-30, -30,-40,-40,-50,-50,-40,-40,-30, -30,-40,-40,-50,-50,-40,-40,-30, -30,-40,-40,-50,-50,-40,-40,-30,
                -20,-30,-30,-40,-40,-30,-30,-20, -10,-20,-20,-20,-20,-20,-20,-10, 20,20,0,0,0,0,20,20, 20,30,10,0,0,10,30,20)
        )
        private val KING_END = intArrayOf(-50,-40,-30,-20,-20,-30,-40,-50, -30,-20,-10,0,0,-10,-20,-30, -30,-10,20,30,30,20,-10,-30, -30,-10,30,40,40,30,-10,-30,
            -30,-10,30,40,40,30,-10,-30, -30,-10,20,30,30,20,-10,-30, -30,-30,0,0,0,0,-30,-30, -50,-30,-30,-30,-30,-30,-30,-50)
    }

    override val id = "chess"
    val board = IntArray(64)
    var side = 1; private set
    private var castling = 0
    private var ep = -1
    private var half = 0
    private var wk = 4
    private var bk = 60

    private var uMove = IntArray(512); private var uCap = IntArray(512); private var uCast = IntArray(512)
    private var uEp = IntArray(512); private var uHalf = IntArray(512)
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
        setFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")
    }

    fun setFen(fen: String) {
        board.fill(0); hist.clear(); keys.clear(); sp = 0; result = Outcome.ONGOING; reason = EndReason.NONE
        val parts = fen.trim().split(" ")
        var rank = 7; var file = 0
        for (c in parts[0]) {
            when {
                c == '/' -> { rank--; file = 0 }
                c.isDigit() -> file += c.digitToInt()
                else -> {
                    val t = when (c.lowercaseChar()) { 'p' -> P; 'n' -> N; 'b' -> B; 'r' -> R; 'q' -> Q; else -> K }
                    board[rank * 8 + file] = if (c.isUpperCase()) t else -t
                    file++
                }
            }
        }
        side = if (parts.getOrElse(1) { "w" } == "w") 1 else -1
        val c = parts.getOrElse(2) { "-" }
        castling = (if ('K' in c) 1 else 0) or (if ('Q' in c) 2 else 0) or (if ('k' in c) 4 else 0) or (if ('q' in c) 8 else 0)
        val e = parts.getOrElse(3) { "-" }
        ep = if (e.length == 2) (e[1] - '1') * 8 + (e[0] - 'a') else -1
        half = parts.getOrElse(4) { "0" }.toIntOrNull() ?: 0
        for (i in 0 until 64) { if (board[i] == K) wk = i; if (board[i] == -K) bk = i }
        keys.add(key())
        updateResult()
    }

    private fun key(): Long {
        var h = 1469598103934665603L
        for (i in 0 until 64) h = (h xor (board[i] + 8L)) * 1099511628211L
        h = (h xor (side + 2L)) * 1099511628211L
        h = (h xor castling.toLong()) * 1099511628211L
        h = (h xor (ep + 2L)) * 1099511628211L
        return h
    }

    fun pieceAt(sq: Int) = board[sq]
    fun kingSquare(color: Int) = if (color > 0) wk else bk

    override fun inCheck() = isAttacked(kingSquare(side), -side)

    private fun add(out: IntArray, n: Int, from: Int, to: Int, promo: Int = 0, flag: Int = 0): Int {
        out[n] = from or (to shl 6) or (promo shl 12) or (flag shl 16); return n + 1
    }

    fun isAttacked(sq: Int, by: Int): Boolean {
        val f = sq and 7; val r = sq shr 3
        // tốt
        val pr = r - by
        if (pr in 0..7) {
            if (f > 0 && board[pr * 8 + f - 1] == by * P) return true
            if (f < 7 && board[pr * 8 + f + 1] == by * P) return true
        }
        var i = 0
        while (i < 16) {
            val nf = f + KN[i]; val nr = r + KN[i + 1]
            if (nf in 0..7 && nr in 0..7 && board[nr * 8 + nf] == by * N) return true
            i += 2
        }
        i = 0
        while (i < 16) {
            val df = KG[i]; val dr = KG[i + 1]
            val nf = f + df; val nr = r + dr
            if (nf in 0..7 && nr in 0..7 && board[nr * 8 + nf] == by * K) return true
            // quân trượt
            var cf = nf; var cr = nr
            val diag = df != 0 && dr != 0
            while (cf in 0..7 && cr in 0..7) {
                val p = board[cr * 8 + cf]
                if (p != 0) {
                    if (p == by * Q || p == by * (if (diag) B else R)) return true
                    break
                }
                cf += df; cr += dr
            }
            i += 2
        }
        return false
    }

    private fun gen(out: IntArray, caps: Boolean): Int {
        var n = 0
        val us = side
        for (sq in 0 until 64) {
            val p = board[sq]
            if (p == 0 || (p > 0) != (us > 0)) continue
            val t = abs(p); val f = sq and 7; val r = sq shr 3
            when (t) {
                P -> {
                    val dir = us; val startRank = if (us > 0) 1 else 6; val promoRank = if (us > 0) 7 else 0
                    val r1 = r + dir
                    if (r1 !in 0..7) continue
                    val to = r1 * 8 + f
                    if (board[to] == 0) {
                        if (r1 == promoRank) { for (pr in 5 downTo 2) n = add(out, n, sq, to, pr) }
                        else if (!caps) {
                            n = add(out, n, sq, to)
                            if (r == startRank && board[to + 8 * dir] == 0) n = add(out, n, sq, to + 8 * dir, 0, F_DOUBLE)
                        }
                    }
                    var df = -1
                    while (df <= 1) {
                        val nf = f + df
                        if (nf in 0..7) {
                            val t2 = r1 * 8 + nf; val tp = board[t2]
                            if (tp != 0 && (tp > 0) != (us > 0)) {
                                if (r1 == promoRank) { for (pr in 5 downTo 2) n = add(out, n, sq, t2, pr) } else n = add(out, n, sq, t2)
                            } else if (t2 == ep && tp == 0) n = add(out, n, sq, t2, 0, F_EP)
                        }
                        df += 2
                    }
                }
                N -> {
                    var i = 0
                    while (i < 16) {
                        val nf = f + KN[i]; val nr = r + KN[i + 1]
                        if (nf in 0..7 && nr in 0..7) {
                            val to = nr * 8 + nf; val tp = board[to]
                            if (tp == 0) { if (!caps) n = add(out, n, sq, to) }
                            else if ((tp > 0) != (us > 0)) n = add(out, n, sq, to)
                        }
                        i += 2
                    }
                }
                K -> {
                    var i = 0
                    while (i < 16) {
                        val nf = f + KG[i]; val nr = r + KG[i + 1]
                        if (nf in 0..7 && nr in 0..7) {
                            val to = nr * 8 + nf; val tp = board[to]
                            if (tp == 0) { if (!caps) n = add(out, n, sq, to) }
                            else if ((tp > 0) != (us > 0)) n = add(out, n, sq, to)
                        }
                        i += 2
                    }
                    if (!caps) {
                        val base = if (us > 0) 0 else 56
                        if (sq == base + 4 && !isAttacked(sq, -us)) {
                            val ks = if (us > 0) 1 else 4; val qs = if (us > 0) 2 else 8
                            if (castling and ks != 0 && board[base + 5] == 0 && board[base + 6] == 0 && board[base + 7] == us * R &&
                                !isAttacked(base + 5, -us) && !isAttacked(base + 6, -us)) n = add(out, n, sq, base + 6, 0, F_CASTLE)
                            if (castling and qs != 0 && board[base + 3] == 0 && board[base + 2] == 0 && board[base + 1] == 0 && board[base] == us * R &&
                                !isAttacked(base + 3, -us) && !isAttacked(base + 2, -us)) n = add(out, n, sq, base + 2, 0, F_CASTLE)
                        }
                    }
                }
                else -> {
                    var i = 0
                    while (i < 16) {
                        val df = KG[i]; val dr = KG[i + 1]
                        i += 2
                        val diag = df != 0 && dr != 0
                        if (t == B && !diag) continue
                        if (t == R && diag) continue
                        var nf = f + df; var nr = r + dr
                        while (nf in 0..7 && nr in 0..7) {
                            val to = nr * 8 + nf; val tp = board[to]
                            if (tp == 0) { if (!caps) n = add(out, n, sq, to) }
                            else { if ((tp > 0) != (us > 0)) n = add(out, n, sq, to); break }
                            nf += df; nr += dr
                        }
                    }
                }
            }
        }
        return n
    }

    private fun ensure() {
        if (sp >= uMove.size) {
            val ns = uMove.size * 2
            uMove = uMove.copyOf(ns); uCap = uCap.copyOf(ns); uCast = uCast.copyOf(ns); uEp = uEp.copyOf(ns); uHalf = uHalf.copyOf(ns)
        }
    }

    private fun make(m: Int) {
        ensure()
        val from = m and 63; val to = (m shr 6) and 63; val promo = (m shr 12) and 7; val flag = m shr 16
        val p = board[from]
        var cap = board[to]
        if (flag and F_EP != 0) {
            val cs = (from shr 3) * 8 + (to and 7)
            cap = board[cs]; board[cs] = 0
        }
        uMove[sp] = m; uCap[sp] = cap; uCast[sp] = castling; uEp[sp] = ep; uHalf[sp] = half; sp++
        board[to] = if (promo != 0) side * promo else p
        board[from] = 0
        if (flag and F_CASTLE != 0) {
            if (to > from) { board[from + 1] = board[from + 3]; board[from + 3] = 0 }
            else { board[from - 1] = board[from - 4]; board[from - 4] = 0 }
        }
        if (abs(p) == K) { if (side > 0) wk = to else bk = to }
        castling = castling and CMASK[from] and CMASK[to]
        ep = if (flag and F_DOUBLE != 0) (from + to) / 2 else -1
        half = if (abs(p) == P || cap != 0) 0 else half + 1
        side = -side
    }

    private fun unmake() {
        sp--
        val m = uMove[sp]
        val from = m and 63; val to = (m shr 6) and 63; val promo = (m shr 12) and 7; val flag = m shr 16
        side = -side
        val cap = uCap[sp]
        castling = uCast[sp]; ep = uEp[sp]; half = uHalf[sp]
        val moved = if (promo != 0) side * P else board[to]
        board[from] = moved
        if (flag and F_EP != 0) { board[to] = 0; board[(from shr 3) * 8 + (to and 7)] = cap } else board[to] = cap
        if (flag and F_CASTLE != 0) {
            if (to > from) { board[from + 3] = board[from + 1]; board[from + 1] = 0 }
            else { board[from - 4] = board[from - 1]; board[from - 1] = 0 }
        }
        if (abs(moved) == K) { if (side > 0) wk = from else bk = from }
    }

    private fun legalList(): IntArray {
        val buf = IntArray(256)
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

    /** Tìm nước hợp lệ (đã mang cờ nhập thành/en passant) từ ô đi, ô đến và quân phong. */
    fun findMove(f: Int, t: Int, pr: Int = 0): Int {
        for (m in legalMoves()) if (fromSq(m) == f && toSq(m) == t && promoOf(m) == pr) return m
        return -1
    }

    override fun play(move: Int): Boolean {
        if (result != Outcome.ONGOING) return false
        val legal = legalList()
        val mv = if (move in legal) move else {
            var f = -1
            for (m in legal) if (fromSq(m) == fromSq(move) && toSq(m) == toSq(move) && promoOf(m) == promoOf(move)) f = m
            f
        }
        if (mv < 0) return false
        val move = mv
        make(move); hist.add(move); keys.add(key())
        updateResult()
        return true
    }

    private fun updateResult() {
        val moves = legalList()
        if (moves.isEmpty()) {
            if (inCheck()) { result = if (side > 0) Outcome.WIN_SECOND else Outcome.WIN_FIRST; reason = EndReason.CHECKMATE }
            else { result = Outcome.DRAW; reason = EndReason.STALEMATE }
            return
        }
        if (half >= 100) { result = Outcome.DRAW; reason = EndReason.FIFTY; return }
        if (insufficient()) { result = Outcome.DRAW; reason = EndReason.MATERIAL; return }
        val k = keys[keys.size - 1]
        var c = 0
        for (x in keys) if (x == k) c++
        if (c >= 3) { result = Outcome.DRAW; reason = EndReason.REPETITION }
    }

    private fun insufficient(): Boolean {
        var minors = 0
        for (p in board) {
            val t = abs(p)
            if (t == P || t == R || t == Q) return false
            if (t == N || t == B) minors++
        }
        return minors <= 1
    }

    override fun forfeit(loser: Int, reason: EndReason) {
        if (result != Outcome.ONGOING) return
        result = if (loser == 0) Outcome.WIN_SECOND else Outcome.WIN_FIRST
        this.reason = reason
    }

    override fun copyGame(): ChessGame {
        val g = ChessGame()
        System.arraycopy(board, 0, g.board, 0, 64)
        g.side = side; g.castling = castling; g.ep = ep; g.half = half; g.wk = wk; g.bk = bk
        g.hist.clear(); g.hist.addAll(hist); g.keys.clear(); g.keys.addAll(keys)
        g.result = result; g.reason = reason; g.sp = 0
        return g
    }

    fun perft(depth: Int): Long {
        if (depth == 0) return 1
        val buf = IntArray(256)
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

    // ───────────────────────── Tìm nước (AI) ─────────────────────────
    private var deadline = 0L
    private var nodes = 0L
    private var abort = false
    private val killers = Array(MAXPLY) { IntArray(2) }
    private val hh = Array(64) { IntArray(64) }
    private val bufs = Array(MAXPLY) { IntArray(256) }
    private val scores = Array(MAXPLY) { IntArray(256) }

    override fun bestMove(timeMs: Long): Int = copyGame().searchRoot(timeMs)

    private fun evaluate(): Int {
        var s = 0
        var material = 0
        for (sq in 0 until 64) { val t = abs(board[sq]); if (t in N..Q) material += VAL[t] }
        val endgame = material < 2600
        for (sq in 0 until 64) {
            val p = board[sq]; if (p == 0) continue
            val t = abs(p); val white = p > 0
            val idx = if (white) (7 - (sq shr 3)) * 8 + (sq and 7) else sq
            val pst = if (t == K && endgame) KING_END[idx] else PST[t][idx]
            val v = VAL[t] + pst
            s += if (white) v else -v
        }
        return if (side > 0) s else -s
    }

    private fun order(ply: Int, n: Int, pvMove: Int) {
        val mv = bufs[ply]; val sc = scores[ply]
        for (i in 0 until n) {
            val m = mv[i]; val to = (m shr 6) and 63; val from = m and 63
            val cap = board[to]
            var s = if (m == pvMove) 1_000_000 else 0
            if (cap != 0) s += 10000 + VAL[abs(cap)] * 10 - VAL[abs(board[from])] / 10
            else if (m shr 16 and F_EP != 0) s += 10100
            val pr = (m shr 12) and 7
            if (pr != 0) s += 9000 + pr
            if (cap == 0 && pr == 0) {
                if (killers[ply][0] == m) s += 8000 else if (killers[ply][1] == m) s += 7900 else s += hh[from][to]
            }
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
        order(ply, n, 0)
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
        if (half >= 100) return 0
        val us = side
        val check = isAttacked(kingSquare(us), -us)
        if (check && ply < MAXPLY - 8) depth++
        if (depth <= 0 || ply >= MAXPLY - 4) return quiesce(alpha, beta, ply)
        val n = gen(bufs[ply], false)
        order(ply, n, 0)
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
                if (board[(m shr 6) and 63] == 0) {
                    if (killers[ply][0] != m) { killers[ply][1] = killers[ply][0]; killers[ply][0] = m }
                    hh[m and 63][(m shr 6) and 63] += depth * depth
                }
                return beta
            }
            if (sc > alpha) alpha = sc
        }
        if (legal == 0) return if (check) -(MATE - ply) else 0
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
        val us = side
        val rsc = IntArray(root.size)
        for (depth in 1..40) {
            var alpha = -INF; var iterBest = -1; var iterScore = -INF
            for (i in root.indices) {
                val m = root[i]
                val cap = board[(m shr 6) and 63]
                rsc[i] = (if (m == best) 1_000_000 else 0) + (if (cap != 0) VAL[abs(cap)] else 0) + (if (promoOf(m) != 0) 800 else 0)
            }
            // sắp xếp gốc
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
            val el = System.nanoTime() - start
            if (el > (deadline - start) * 0.45) break
        }
        check(us == side)
        return best
    }
}
