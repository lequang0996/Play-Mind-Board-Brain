package com.fws.playmind.core

import kotlin.math.abs
import kotlin.math.ln
import kotlin.random.Random

/**
 * Cờ vua. Ô = rank*8 + file, a1 = 0, h8 = 63. Quân trắng dương, đen âm.
 * Mã hóa nước đi: from | to<<6 | promo<<12 | flag<<16.
 *
 * Engine v2 (cấp cao thủ): PVS + bảng băm Zobrist, cắt tỉa null-move / reverse-futility / futility / LMP,
 * LMR, SEE, tìm kiếm tĩnh, aspiration window, đánh giá tapered (vật chất, PST, cơ động, cấu trúc tốt,
 * tốt thông, an toàn vua, tàn cuộc) và sách khai cuộc nhỏ. Xem docs/BOT_GUIDE.md.
 */
class ChessGame : BoardGame {
    companion object {
        const val P = 1; const val N = 2; const val B = 3; const val R = 4; const val Q = 5; const val K = 6
        private const val F_EP = 1; private const val F_CASTLE = 2; private const val F_DOUBLE = 4
        private const val MATE = 30000
        private const val INF = 32000
        private const val MAXPLY = 100
        private val VAL = intArrayOf(0, 100, 320, 330, 500, 900, 0)
        private val SEE_VAL = intArrayOf(0, 100, 320, 330, 500, 900, 20000)
        private val MG_VAL = intArrayOf(0, 82, 337, 365, 477, 1025, 0)
        private val EG_VAL = intArrayOf(0, 94, 281, 297, 512, 936, 0)
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

        // ── Zobrist ──
        private val zr = Random(0x5EEDC0DE)
        private val Z_PIECE = Array(13) { LongArray(64) { zr.nextLong() } }
        private val Z_SIDE = zr.nextLong()
        private val Z_CASTLE = LongArray(16) { zr.nextLong() }
        private val Z_EP = LongArray(8) { zr.nextLong() }
        private fun zp(p: Int, sq: Int) = Z_PIECE[p + 6][sq]

        // ── Bảng băm dùng chung (chỉ một luồng tìm kiếm tại một thời điểm, có khóa) ──
        private const val TT_BITS = 19
        private const val TT_SIZE = 1 shl TT_BITS
        private val ttKey = LongArray(TT_SIZE)
        private val ttMove = IntArray(TT_SIZE)
        private val ttScore = IntArray(TT_SIZE)
        private val ttInfo = IntArray(TT_SIZE)   // depth<<2 | flag (0 chính xác, 1 cận dưới, 2 cận trên)
        private val lock = Any()

        @Volatile var lastDepth = 0; private set
        @Volatile var lastNodes = 0L; private set

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
        private val PAWN_EG_RANK = intArrayOf(0, 0, 4, 12, 26, 48, 85, 0)
        private val PASSED = intArrayOf(0, 5, 10, 20, 38, 65, 105, 0)
        private val LMR = Array(64) { d -> IntArray(64) { m -> if (d < 3 || m < 2) 0 else (0.7 + ln(d.toDouble()) * ln(m.toDouble()) / 2.3).toInt() } }

        // ── Sách khai cuộc nhỏ (UCI). Mỗi nước được kiểm tra lại bằng luật cờ khi dùng. ──
        private val BOOK: List<List<String>> = listOf(
            "e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5a4 g8f6 e1g1 f8e7 f1e1 b7b5 a4b3 d7d6 c2c3 e8g8 h2h3",
            "e2e4 e7e5 g1f3 b8c6 f1c4 f8c5 c2c3 g8f6 d2d3 d7d6 e1g1",
            "e2e4 e7e5 g1f3 b8c6 d2d4 e5d4 f3d4 g8f6 d4c6 b7c6 e4e5",
            "e2e4 e7e5 g1f3 g8f6 f3e5 d7d6 e5f3 f6e4 d2d4 d6d5 f1d3 b8c6 e1g1 f8e7",
            "e2e4 e7e5 g1f3 b8c6 f1b5 g8f6 e1g1 f6e4 d2d4 e4d6 b5c6 d7c6 d4e5 d6f5 d1d8 e8d8",
            "e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 a7a6 c1e3 e7e5 d4b3 c8e6",
            "e2e4 c7c5 g1f3 b8c6 d2d4 c5d4 f3d4 g7g6 b1c3 f8g7 c1e3 g8f6 f1c4 e8g8 c4b3",
            "e2e4 e7e6 d2d4 d7d5 b1c3 g8f6 c1g5 f8e7 e4e5 f6d7 g5e7 d8e7 f2f4 e8g8",
            "e2e4 c7c6 d2d4 d7d5 b1c3 d5e4 c3e4 c8f5 e4g3 f5g6 h2h4 h7h6 g1f3 b8d7",
            "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 c1g5 f8e7 e2e3 e8g8 g1f3 h7h6 g5h4 b7b6",
            "d2d4 d7d5 c2c4 c7c6 g1f3 g8f6 b1c3 d5c4 a2a4 c8f5 e2e3 e7e6 f1c4 f8b4",
            "d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 e2e4 d7d6 g1f3 e8g8 f1e2 e7e5 e1g1 b8c6 d4d5 c6e7",
            "d2d4 g8f6 c2c4 e7e6 b1c3 f8b4 e2e3 e8g8 f1d3 d7d5 g1f3 c7c5 e1g1 b8c6",
            "c2c4 e7e5 b1c3 g8f6 g1f3 b8c6 g2g3 d7d5 c4d5 f6d5 f1g2 d5b6 e1g1 f8e7",
            "g1f3 d7d5 g2g3 g8f6 f1g2 e7e6 e1g1 f8e7 d2d3 e8g8 b1d2 c7c5",
            "d2d4 d7d5 g1f3 g8f6 c1f4 e7e6 e2e3 c7c5 c2c3 b8c6 b1d2 f8d6 f4g3 e8g8 f1d3",
            "e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5a4 g8f6 e1g1 b7b5 a4b3 f8e7 f1e1 d7d6 c2c3 e8g8",
        ).map { it.split(" ") }
    }

    override val id = "chess"
    val board = IntArray(64)
    var side = 1; private set
    private var castling = 0
    private var ep = -1
    private var half = 0
    private var wk = 4
    private var bk = 60
    private var hash = 0L

    private var uMove = IntArray(1024); private var uCap = IntArray(1024); private var uCast = IntArray(1024)
    private var uEp = IntArray(1024); private var uHalf = IntArray(1024); private var uHash = LongArray(1024)
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
        hash = computeHash()
        keys.add(hash)
        updateResult()
    }

    private fun computeHash(): Long {
        var h = 0L
        for (i in 0 until 64) if (board[i] != 0) h = h xor zp(board[i], i)
        if (side < 0) h = h xor Z_SIDE
        h = h xor Z_CASTLE[castling]
        if (ep >= 0) h = h xor Z_EP[ep and 7]
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
            uMove = uMove.copyOf(ns); uCap = uCap.copyOf(ns); uCast = uCast.copyOf(ns); uEp = uEp.copyOf(ns)
            uHalf = uHalf.copyOf(ns); uHash = uHash.copyOf(ns)
        }
    }

    private fun make(m: Int) {
        ensure()
        val from = m and 63; val to = (m shr 6) and 63; val promo = (m shr 12) and 7; val flag = m shr 16
        val p = board[from]
        var cap = board[to]
        uMove[sp] = m; uCast[sp] = castling; uEp[sp] = ep; uHalf[sp] = half; uHash[sp] = hash
        hash = hash xor Z_SIDE xor Z_CASTLE[castling]
        if (ep >= 0) hash = hash xor Z_EP[ep and 7]
        if (flag and F_EP != 0) {
            val cs = (from shr 3) * 8 + (to and 7)
            cap = board[cs]; board[cs] = 0
            hash = hash xor zp(cap, cs)
        } else if (cap != 0) hash = hash xor zp(cap, to)
        uCap[sp] = cap; sp++
        val np = if (promo != 0) side * promo else p
        hash = hash xor zp(p, from) xor zp(np, to)
        board[to] = np
        board[from] = 0
        if (flag and F_CASTLE != 0) {
            val rook = side * R
            if (to > from) { board[from + 1] = board[from + 3]; board[from + 3] = 0; hash = hash xor zp(rook, from + 3) xor zp(rook, from + 1) }
            else { board[from - 1] = board[from - 4]; board[from - 4] = 0; hash = hash xor zp(rook, from - 4) xor zp(rook, from - 1) }
        }
        if (abs(p) == K) { if (side > 0) wk = to else bk = to }
        castling = castling and CMASK[from] and CMASK[to]
        hash = hash xor Z_CASTLE[castling]
        if (flag and F_DOUBLE != 0) { ep = (from + to) / 2; hash = hash xor Z_EP[ep and 7] } else ep = -1
        half = if (abs(p) == P || cap != 0) 0 else half + 1
        side = -side
    }

    private fun unmake() {
        sp--
        val m = uMove[sp]
        val from = m and 63; val to = (m shr 6) and 63; val promo = (m shr 12) and 7; val flag = m shr 16
        side = -side
        val cap = uCap[sp]
        castling = uCast[sp]; ep = uEp[sp]; half = uHalf[sp]; hash = uHash[sp]
        val moved = if (promo != 0) side * P else board[to]
        board[from] = moved
        if (flag and F_EP != 0) { board[to] = 0; board[(from shr 3) * 8 + (to and 7)] = cap } else board[to] = cap
        if (flag and F_CASTLE != 0) {
            if (to > from) { board[from + 3] = board[from + 1]; board[from + 1] = 0 }
            else { board[from - 4] = board[from - 1]; board[from - 1] = 0 }
        }
        if (abs(moved) == K) { if (side > 0) wk = from else bk = from }
    }

    private fun makeNull() {
        ensure()
        uMove[sp] = 0; uCap[sp] = 0; uCast[sp] = castling; uEp[sp] = ep; uHalf[sp] = half; uHash[sp] = hash; sp++
        hash = hash xor Z_SIDE
        if (ep >= 0) { hash = hash xor Z_EP[ep and 7]; ep = -1 }
        half++
        side = -side
    }

    private fun unmakeNull() {
        sp--
        side = -side
        ep = uEp[sp]; half = uHalf[sp]; hash = uHash[sp]
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
        make(mv); hist.add(mv); keys.add(hash)
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
        g.side = side; g.castling = castling; g.ep = ep; g.half = half; g.wk = wk; g.bk = bk; g.hash = hash
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

    // ───────────────────────── Đánh giá ─────────────────────────
    private val pawnMin = Array(2) { IntArray(8) }   // [0] trắng, [1] đen: hạng thấp/cao nhất của tốt trên mỗi cột
    private val pawnMax = Array(2) { IntArray(8) }
    private val pawnCnt = Array(2) { IntArray(8) }
    private val zoneStamp = Array(2) { IntArray(64) }
    private var stamp = 0
    private val attW = IntArray(2); private val hits = IntArray(2); private val cntAtt = IntArray(2)

    private fun evaluate(): Int {
        var mg = 0; var eg = 0; var phase = 0
        for (c in 0..1) for (f in 0..7) { pawnMin[c][f] = 8; pawnMax[c][f] = -1; pawnCnt[c][f] = 0 }
        var wbishops = 0; var bbishops = 0
        var wmat = 0; var bmat = 0; var pawns = 0
        for (sq in 0 until 64) {
            val p = board[sq]; if (p == 0) continue
            val t = abs(p)
            if (t == P) {
                val c = if (p > 0) 0 else 1; val f = sq and 7; val r = sq shr 3
                if (r < pawnMin[c][f]) pawnMin[c][f] = r
                if (r > pawnMax[c][f]) pawnMax[c][f] = r
                pawnCnt[c][f]++; pawns++
            } else if (t != K) {
                if (p > 0) wmat += VAL[t] else bmat += VAL[t]
                if (t == B) { if (p > 0) wbishops++ else bbishops++ }
            }
        }
        // vùng nguy hiểm quanh vua mỗi bên
        stamp++
        for (c in 0..1) {
            val ksq = if (c == 0) wk else bk; val kf = ksq and 7; val kr = ksq shr 3
            val fwd = if (c == 0) 1 else -1
            for (df in -1..1) for (dr in -1..2) {
                val nf = kf + df; val nr = kr + dr * fwd
                if (nf in 0..7 && nr in 0..7) zoneStamp[c][nr * 8 + nf] = stamp
            }
            attW[c] = 0; hits[c] = 0; cntAtt[c] = 0
        }

        for (sq in 0 until 64) {
            val p = board[sq]; if (p == 0) continue
            val t = abs(p); val white = p > 0; val c = if (white) 0 else 1
            val f = sq and 7; val r = sq shr 3
            val idx = if (white) (7 - r) * 8 + f else sq
            var m = 0; var e = 0
            when (t) {
                P -> {
                    m = MG_VAL[P] + PST[P][idx]
                    val rr = if (white) r else 7 - r
                    e = EG_VAL[P] + PAWN_EG_RANK[rr]
                    var passed = true
                    for (g in maxOf(0, f - 1)..minOf(7, f + 1)) {
                        if (white) { if (pawnMax[1][g] > r) { passed = false; break } }
                        else { if (pawnMin[0][g] < r) { passed = false; break } }
                    }
                    if (passed) { m += PASSED[rr] / 2; e += PASSED[rr] }
                    val own = pawnCnt[c]
                    if ((f == 0 || own[f - 1] == 0) && (f == 7 || own[f + 1] == 0)) { m -= 10; e -= 14 }
                    if (own[f] > 1) { m -= 8; e -= 12 }
                }
                N, B, R, Q -> {
                    phase += if (t == N || t == B) 1 else if (t == R) 2 else 4
                    m = MG_VAL[t] + PST[t][idx]; e = EG_VAL[t] + PST[t][idx]
                    var mob = 0
                    var anyZone = false
                    val ez = zoneStamp[1 - c]
                    if (t == N) {
                        var i = 0
                        while (i < 16) {
                            val nf = f + KN[i]; val nr = r + KN[i + 1]; i += 2
                            if (nf in 0..7 && nr in 0..7) {
                                val to = nr * 8 + nf; val tp = board[to]
                                if (tp == 0 || (tp > 0) != white) mob++
                                if (ez[to] == stamp) { hits[1 - c]++; anyZone = true }
                            }
                        }
                        m += 4 * (mob - 4); e += 4 * (mob - 4)
                    } else {
                        var i = 0
                        while (i < 16) {
                            val df = KG[i]; val dr = KG[i + 1]; i += 2
                            val diag = df != 0 && dr != 0
                            if (t == B && !diag) continue
                            if (t == R && diag) continue
                            var nf = f + df; var nr = r + dr
                            while (nf in 0..7 && nr in 0..7) {
                                val to = nr * 8 + nf; val tp = board[to]
                                if (ez[to] == stamp) { hits[1 - c]++; anyZone = true }
                                if (tp == 0) mob++ else { if ((tp > 0) != white) mob++; break }
                                nf += df; nr += dr
                            }
                        }
                        when (t) {
                            B -> { m += 5 * (mob - 6); e += 5 * (mob - 6) }
                            R -> { m += 2 * (mob - 6); e += 4 * (mob - 6) }
                            else -> { m += (mob - 12); e += 2 * (mob - 12) }
                        }
                    }
                    if (anyZone) { cntAtt[1 - c]++; attW[1 - c] += if (t == Q) 5 else if (t == R) 3 else 2 }
                    if (t == R) {
                        val own = pawnCnt[c][f]; val opp = pawnCnt[1 - c][f]
                        if (own == 0) { if (opp == 0) { m += 22; e += 14 } else { m += 11; e += 8 } }
                        if (if (white) r == 6 else r == 1) { m += 10; e += 20 }
                    }
                }
                else -> {
                    m = PST[K][idx]; e = KING_END[idx]
                    val dir = if (white) 1 else -1
                    var shield = 0
                    for (df in -1..1) {
                        val nf = f + df; if (nf !in 0..7) continue
                        val r1 = r + dir; val r2 = r + 2 * dir
                        if (r1 in 0..7 && board[r1 * 8 + nf] == (if (white) P else -P)) shield += 10
                        else if (r2 in 0..7 && board[r2 * 8 + nf] == (if (white) P else -P)) shield += 5
                        else if (pawnCnt[c][nf] == 0) shield -= 14
                    }
                    if ((white && r <= 1) || (!white && r >= 6)) m += shield
                }
            }
            if (white) { mg += m; eg += e } else { mg -= m; eg -= e }
        }
        if (wbishops >= 2) { mg += 30; eg += 50 }
        if (bbishops >= 2) { mg -= 30; eg -= 50 }
        for (c in 0..1) {
            if (cntAtt[c] >= 2) {
                val danger = attW[c] * hits[c]
                val pen = minOf(500, danger * danger / 6)
                if (c == 0) mg -= pen else mg += pen
            }
        }
        val ph = minOf(24, phase)
        var score = (mg * ph + eg * (24 - ph)) / 24
        val diff = wmat - bmat
        if (ph <= 12 && abs(diff) >= 300) {   // tàn cuộc: dồn vua đối phương ra mép
            val win = if (diff > 0) 0 else 1
            val loserK = if (win == 0) bk else wk; val winK = if (win == 0) wk else bk
            val lf = loserK and 7; val lr = loserK shr 3
            val cd = maxOf(3 - lf, lf - 4) + maxOf(3 - lr, lr - 4)
            val kd = abs((winK and 7) - lf) + abs((winK shr 3) - lr)
            val bonus = 6 * cd + 3 * (14 - kd)
            score += if (win == 0) bonus else -bonus
        }
        if (pawns == 0 && abs(diff) < 400 && ph <= 8) score /= 3
        score += if (side > 0) 12 else -12
        return if (side > 0) score else -score
    }

    // ───────────────────────── SEE ─────────────────────────
    private val seeGain = IntArray(40)
    private val seeSq = IntArray(40)
    private val seePc = IntArray(40)

    private fun leastAttacker(to: Int, color: Int): Int {
        val f = to and 7; val r = to shr 3
        val pr = r - color
        if (pr in 0..7) {
            if (f > 0 && board[pr * 8 + f - 1] == color * P) return pr * 8 + f - 1
            if (f < 7 && board[pr * 8 + f + 1] == color * P) return pr * 8 + f + 1
        }
        var i = 0
        while (i < 16) {
            val nf = f + KN[i]; val nr = r + KN[i + 1]; i += 2
            if (nf in 0..7 && nr in 0..7 && board[nr * 8 + nf] == color * N) return nr * 8 + nf
        }
        var bq = -1; var rk = -1; var qn = -1; var kg = -1
        i = 0
        while (i < 16) {
            val df = KG[i]; val dr = KG[i + 1]; i += 2
            val diag = df != 0 && dr != 0
            var nf = f + df; var nr = r + dr
            var first = true
            while (nf in 0..7 && nr in 0..7) {
                val sq = nr * 8 + nf; val p = board[sq]
                if (p != 0) {
                    if (p * color > 0) {
                        val t = abs(p)
                        if (t == Q) { if (qn < 0) qn = sq }
                        else if (diag && t == B) { if (bq < 0) bq = sq }
                        else if (!diag && t == R) { if (rk < 0) rk = sq }
                        else if (t == K && first) { if (kg < 0) kg = sq }
                    }
                    break
                }
                first = false
                nf += df; nr += dr
            }
        }
        if (bq >= 0) return bq
        if (rk >= 0) return rk
        if (qn >= 0) return qn
        return kg
    }

    /** SEE của một nước ăn quân: giá trị thu được sau chuỗi trao đổi tốt nhất (âm nếu thiệt). */
    private fun see(m: Int): Int {
        val from = m and 63; val to = (m shr 6) and 63
        val attacker = board[from]
        val target = if ((m shr 16 and F_EP) != 0) P else abs(board[to])
        var d = 0
        seeGain[0] = SEE_VAL[target]
        var color = if (attacker > 0) -1 else 1
        var cur = SEE_VAL[abs(attacker)]
        var removed = 0
        seeSq[removed] = from; seePc[removed] = attacker; removed++; board[from] = 0
        while (true) {
            d++
            val a = leastAttacker(to, color)
            if (a < 0) break
            seeGain[d] = cur - seeGain[d - 1]
            if (maxOf(-seeGain[d - 1], seeGain[d]) < 0) break
            cur = SEE_VAL[abs(board[a])]
            seeSq[removed] = a; seePc[removed] = board[a]; removed++; board[a] = 0
            color = -color
            if (d >= 30) break
        }
        while (--d > 0) seeGain[d - 1] = -maxOf(-seeGain[d - 1], seeGain[d])
        while (removed > 0) { removed--; board[seeSq[removed]] = seePc[removed] }
        return seeGain[0]
    }

    // ───────────────────────── Tìm kiếm ─────────────────────────
    private var deadline = 0L
    private var rootSide = 1
    private var nodes = 0L
    private var abort = false
    private val killers = Array(MAXPLY) { IntArray(2) }
    private val hh = Array(2) { Array(64) { IntArray(64) } }
    private val bufs = Array(MAXPLY) { IntArray(256) }
    private val scores = Array(MAXPLY) { IntArray(256) }
    private val quiets = Array(MAXPLY) { IntArray(64) }

    override fun bestMove(timeMs: Long): Int {
        val book = bookMove()
        if (book >= 0) return book
        return synchronized(lock) { copyGame().searchRoot(timeMs) }
    }

    private fun uci(m: Int): String {
        val f = fromSq(m); val t = toSq(m)
        return "" + ('a' + (f and 7)) + ('1' + (f shr 3)) + ('a' + (t and 7)) + ('1' + (t shr 3))
    }

    private fun bookMove(): Int {
        if (hist.size > 18 || result != Outcome.ONGOING) return -1
        val cand = ArrayList<String>()
        for (line in BOOK) {
            if (line.size <= hist.size) continue
            var ok = true
            for (i in hist.indices) if (line[i] != uci(hist[i])) { ok = false; break }
            if (ok) cand.add(line[hist.size])
        }
        if (cand.isEmpty()) return -1
        val pick = cand[Random.nextInt(cand.size)]
        val f = (pick[1] - '1') * 8 + (pick[0] - 'a'); val t = (pick[3] - '1') * 8 + (pick[2] - 'a')
        return findMove(f, t)
    }

    private fun repeated(): Boolean {
        val lim = minOf(half, sp + keys.size - 1)
        var k = 2
        while (k <= lim) {
            val h = if (k <= sp) uHash[sp - k] else {
                val idx = keys.size - 1 - (k - sp); if (idx < 0) break; keys[idx]
            }
            if (h == hash) return true
            k += 2
        }
        return false
    }

    private fun hasPieces(color: Int): Boolean {
        for (p in board) if (p * color > 0 && abs(p) in N..Q) return true
        return false
    }

    private fun scoreMoves(ply: Int, n: Int, ttm: Int) {
        val mv = bufs[ply]; val sc = scores[ply]
        val sd = if (side > 0) 0 else 1
        for (i in 0 until n) {
            val m = mv[i]; val to = (m shr 6) and 63; val from = m and 63
            val cap = board[to]; val pr = (m shr 12) and 7
            val s: Int
            if (m == ttm) s = 2_000_000
            else if (cap != 0 || (m shr 16 and F_EP) != 0) {
                val vic = if (cap != 0) abs(cap) else P
                val att = abs(board[from])
                s = if (VAL[vic] + 30 >= VAL[att] || see(m) >= 0) 1_000_000 + VAL[vic] * 16 - att else 100_000 + VAL[vic] * 16 - att
            } else if (pr != 0) s = if (pr == Q) 990_000 else 50_000 + pr
            else if (killers[ply][0] == m) s = 900_000
            else if (killers[ply][1] == m) s = 890_000
            else s = hh[sd][from][to]
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

    /** Điểm hòa nhìn từ bên đang đi: hơi âm cho bên của bot (contempt) để không chấp nhận hòa khi thế ngang. */
    private fun drawScore() = if (side == rootSide) -15 else 15

    private fun scoreToTT(s: Int, ply: Int) = if (s > MATE - 300) s + ply else if (s < -MATE + 300) s - ply else s
    private fun scoreFromTT(s: Int, ply: Int) = if (s > MATE - 300) s - ply else if (s < -MATE + 300) s + ply else s

    private fun quiesce(alphaIn: Int, beta: Int, ply: Int): Int {
        var alpha = alphaIn
        if ((++nodes and 1023L) == 0L && System.nanoTime() > deadline) abort = true
        if (abort) return 0
        val us = side
        val inChk = isAttacked(kingSquare(us), -us)
        var stand = -INF
        if (!inChk) {
            stand = evaluate()
            if (ply >= MAXPLY - 2) return stand
            if (stand >= beta) return stand
            if (stand > alpha) alpha = stand
        } else if (ply >= MAXPLY - 2) return evaluate()
        val n = gen(bufs[ply], !inChk)
        scoreMoves(ply, n, 0)
        var legal = 0
        for (i in 0 until n) {
            pick(ply, n, i)
            val m = bufs[ply][i]
            val cap = board[(m shr 6) and 63]
            if (!inChk && ((m shr 12) and 7) == 0) {
                if (scores[ply][i] < 1_000_000) continue                                           // SEE < 0
                if (stand + VAL[if (cap != 0) abs(cap) else P] + 200 < alpha) continue             // delta
            }
            make(m)
            if (isAttacked(kingSquare(us), -us)) { unmake(); continue }
            legal++
            val sc = -quiesce(-beta, -alpha, ply + 1)
            unmake()
            if (abort) return 0
            if (sc >= beta) return sc
            if (sc > alpha) alpha = sc
        }
        if (inChk && legal == 0) return -(MATE - ply)
        return alpha
    }

    private fun search(depthIn: Int, alphaIn: Int, betaIn: Int, ply: Int, nullOk: Boolean): Int {
        var alpha = alphaIn; var beta = betaIn; var depth = depthIn
        if ((++nodes and 1023L) == 0L && System.nanoTime() > deadline) abort = true
        if (abort) return 0
        val pv = beta - alpha > 1
        if (ply > 0) {
            if (half >= 100 || repeated()) return drawScore()
            alpha = maxOf(alpha, -MATE + ply); beta = minOf(beta, MATE - ply - 1)
            if (alpha >= beta) return alpha
        }
        val us = side
        val inChk = isAttacked(kingSquare(us), -us)
        if (inChk && ply < MAXPLY - 10) depth++
        if (depth <= 0 || ply >= MAXPLY - 4) return quiesce(alpha, beta, ply)

        val slot = (hash and (TT_SIZE - 1).toLong()).toInt()
        var ttm = 0
        if (ttKey[slot] == hash) {
            ttm = ttMove[slot]
            val td = ttInfo[slot] shr 2; val fl = ttInfo[slot] and 3
            if (td >= depth && !pv && ply > 0) {
                val s = scoreFromTT(ttScore[slot], ply)
                if (fl == 0 || (fl == 1 && s >= beta) || (fl == 2 && s <= alpha)) return s
            }
        }
        val staticEval = if (inChk) -INF else evaluate()
        if (!pv && !inChk) {
            if (depth <= 6 && staticEval - 90 * depth >= beta && staticEval < MATE - 300) return staticEval   // reverse futility
            if (nullOk && depth >= 3 && staticEval >= beta && hasPieces(us)) {                                // null move
                val r = 2 + depth / 4 + minOf(2, (staticEval - beta) / 200)
                makeNull()
                val s = -search(depth - 1 - r, -beta, -beta + 1, ply + 1, false)
                unmakeNull()
                if (abort) return 0
                if (s >= beta) return if (s > MATE - 300) beta else s
            }
        }
        if (ttm == 0 && depth >= 5 && pv) depth--

        val n = gen(bufs[ply], false)
        scoreMoves(ply, n, ttm)
        val sd = if (us > 0) 0 else 1
        var legal = 0; var quietCount = 0
        var best = -INF; var bestMove = 0
        val origAlpha = alpha
        for (i in 0 until n) {
            pick(ply, n, i)
            val m = bufs[ply][i]
            val to = (m shr 6) and 63; val from = m and 63
            val cap = board[to]; val promo = (m shr 12) and 7
            val quiet = cap == 0 && promo == 0 && (m shr 16 and F_EP) == 0
            make(m)
            if (isAttacked(kingSquare(us), -us)) { unmake(); continue }
            legal++
            val gives = isAttacked(kingSquare(-us), us)
            if (quiet && !inChk && !gives && legal > 1 && best > -MATE + 300) {
                if (depth <= 3 && staticEval + 110 * depth + 40 <= alpha) { unmake(); continue }   // futility
                if (depth <= 4 && legal > 6 + depth * depth) { unmake(); continue }                // late move pruning
            }
            if (!quiet && !inChk && !gives && depth <= 3 && legal > 1 && cap != 0 && promo == 0 && scores[ply][i] < 1_000_000 && best > -MATE + 300) {
                unmake(); continue
            }
            if (quiet && quietCount < 63) quiets[ply][quietCount++] = m
            var s: Int
            val nd = depth - 1
            if (legal == 1) s = -search(nd, -beta, -alpha, ply + 1, true)
            else {
                var red = 0
                if (depth >= 3 && quiet && !inChk && !gives) {
                    red = LMR[minOf(depth, 63)][minOf(legal, 63)]
                    if (pv) red--
                    if (killers[ply][0] == m || killers[ply][1] == m) red--
                    red -= hh[sd][from][to] / 6000
                    if (red < 0) red = 0
                    if (red > nd - 1) red = maxOf(0, nd - 1)
                }
                s = -search(nd - red, -alpha - 1, -alpha, ply + 1, true)
                if (s > alpha && red > 0) s = -search(nd, -alpha - 1, -alpha, ply + 1, true)
                if (s > alpha && s < beta) s = -search(nd, -beta, -alpha, ply + 1, true)
            }
            unmake()
            if (abort) return 0
            if (s > best) {
                best = s; bestMove = m
                if (s > alpha) {
                    alpha = s
                    if (alpha >= beta) {
                        if (quiet) {
                            if (killers[ply][0] != m) { killers[ply][1] = killers[ply][0]; killers[ply][0] = m }
                            val bonus = depth * depth
                            hh[sd][from][to] = minOf(30000, hh[sd][from][to] + bonus)
                            for (q in 0 until quietCount - 1) {
                                val qm = quiets[ply][q]; val qf = qm and 63; val qt = (qm shr 6) and 63
                                hh[sd][qf][qt] = maxOf(0, hh[sd][qf][qt] - bonus / 2)
                            }
                        }
                        break
                    }
                }
            }
        }
        if (legal == 0) return if (inChk) -(MATE - ply) else 0
        val flag = if (best >= beta) 1 else if (best > origAlpha) 0 else 2
        if (ttKey[slot] != hash || (ttInfo[slot] shr 2) <= depth + 1 || flag == 0) {
            ttKey[slot] = hash; ttMove[slot] = bestMove; ttScore[slot] = scoreToTT(best, ply); ttInfo[slot] = (depth shl 2) or flag
        }
        return best
    }

    private fun searchRoot(timeMs: Long): Int {
        val start = System.nanoTime()
        deadline = start + timeMs * 1_000_000
        val soft = start + (timeMs * 1_000_000 * 0.55).toLong()
        abort = false; nodes = 0; rootSide = side
        val root = legalList()
        if (root.isEmpty()) return -1
        if (root.size == 1) return root[0]
        var best = root[0]
        var prevScore = 0
        for (i in 0 until MAXPLY) { killers[i][0] = 0; killers[i][1] = 0 }
        val us = side
        val rsc = IntArray(root.size)
        var completed = 0
        for (depth in 1..60) {
            var delta = if (depth >= 5) 28 else INF
            var alpha = if (depth >= 5) maxOf(-INF, prevScore - delta) else -INF
            var beta = if (depth >= 5) minOf(INF, prevScore + delta) else INF
            var iterBest = -1; var iterScore = -INF
            while (true) {
                for (i in root.indices) {
                    val m = root[i]; val cap = board[(m shr 6) and 63]
                    rsc[i] = (if (m == best) 1_000_000 else 0) + (if (cap != 0) VAL[abs(cap)] * 10 - VAL[abs(board[m and 63])] else 0) + (if (promoOf(m) != 0) 800 else 0)
                }
                val idx = root.indices.sortedByDescending { rsc[it] }
                val sorted = IntArray(root.size) { root[idx[it]] }
                var a = alpha
                iterBest = -1; iterScore = -INF
                for ((k, m) in sorted.withIndex()) {
                    make(m)
                    var sc: Int
                    if (k == 0) sc = -search(depth - 1, -beta, -a, 1, true)
                    else {
                        sc = -search(depth - 1, -a - 1, -a, 1, true)
                        if (sc > a && sc < beta && !abort) sc = -search(depth - 1, -beta, -a, 1, true)
                    }
                    unmake()
                    if (abort) break
                    if (sc > iterScore) { iterScore = sc; iterBest = m }
                    if (sc > a) a = sc
                    if (a >= beta) break
                }
                if (abort) break
                if (iterScore <= alpha && alpha > -INF) { alpha = maxOf(-INF, alpha - delta * 3); delta *= 3; continue }
                if (iterScore >= beta && beta < INF) { beta = minOf(INF, beta + delta * 3); delta *= 3; continue }
                for (i in root.indices) root[i] = sorted[i]
                break
            }
            if (abort) {
                if (iterBest >= 0 && iterScore > prevScore - 40 && completed > 0) best = iterBest
                break
            }
            best = iterBest; prevScore = iterScore; completed = depth
            lastDepth = depth
            if ((iterScore > MATE - 200 || iterScore < -MATE + 200) && depth >= 6) break
            if (System.nanoTime() > soft) break
        }
        lastNodes = nodes
        check(us == side)
        return best
    }
}
