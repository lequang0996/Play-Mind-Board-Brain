package com.fws.playmind.core

import kotlin.math.abs
import kotlin.random.Random

/**
 * Cờ vua. Ô = rank*8 + file, a1 = 0, h8 = 63. Quân trắng dương, đen âm.
 * Mã hóa nước đi: from | to<<6 | promo<<12 | flag<<16.
 *
 * Lớp này giữ luật cờ (đã kiểm bằng perft) và sách khai cuộc nhỏ; việc tìm kiếm do [ChessEngine] (bitboard) đảm nhận.
 * Xem docs/BOT_GUIDE.md.
 */
class ChessGame : BoardGame, LeveledEngine {
    companion object {
        const val P = 1; const val N = 2; const val B = 3; const val R = 4; const val Q = 5; const val K = 6
        private const val F_EP = 1; private const val F_CASTLE = 2; private const val F_DOUBLE = 4
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

        private val lock = Any()

        @Volatile var lastDepth = 0; private set
        @Volatile var lastNodes = 0L; private set
        /** Engine tìm kiếm dùng chung (bảng băm giữ giữa các nước); chỉ một luồng dùng tại một thời điểm (khóa [lock]). */
        private val engine by lazy { ChessEngine(20) }
        /**
         * Số luồng tìm kiếm của Siêu việt (Lazy SMP; các cấp dưới luôn 1 luồng). Mặc định min(4, số nhân − 2);
         * đặt bằng thuộc tính hệ thống pm.chess.threads khi đo.
         */
        @Volatile var threads: Int = System.getProperty("pm.chess.threads")?.toIntOrNull()
            ?: minOf(4, maxOf(1, Runtime.getRuntime().availableProcessors() - 2))
        const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

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

    private var startFen = START_FEN

    fun setFen(fen: String) {
        startFen = fen
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

    /**
     * Luật của app: nước đi tạo lại một thế cờ đã có (cùng bên tới lượt) thì bên vừa đi THUA ngay.
     * Thế cờ trùng chỉ có thể nằm sau nước "không thể đảo ngược" cuối cùng, nhưng so cả danh sách cho đơn giản.
     */
    private fun repeatedPosition(): Boolean {
        val k = keys[keys.size - 1]
        var i = keys.size - 3
        while (i >= 0) { if (keys[i] == k) return true; i -= 2 }
        return false
    }

    private fun updateResult() {
        val moves = legalList()
        if (moves.isEmpty()) {
            if (inCheck()) { result = if (side > 0) Outcome.WIN_SECOND else Outcome.WIN_FIRST; reason = EndReason.CHECKMATE }
            else { result = Outcome.DRAW; reason = EndReason.STALEMATE }
            return
        }
        if (repeatedPosition()) { result = if (side > 0) Outcome.WIN_FIRST else Outcome.WIN_SECOND; reason = EndReason.REPETITION; return }
        if (half >= 100) { result = Outcome.DRAW; reason = EndReason.FIFTY; return }
        if (insufficient()) { result = Outcome.DRAW; reason = EndReason.MATERIAL; return }
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
        g.result = result; g.reason = reason; g.sp = 0; g.startFen = startFen
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

    /** Tìm kiếm bằng [ChessEngine] trên thế hiện tại (gọi trong khóa [lock]). */
    private fun think(lim: SearchLimit, allowed: IntArray?, delta: Int): RootResult {
        val r = engine.searchSmp(startFen, hist, lim, allowed, delta, threads)
        lastDepth = r.depth; lastNodes = r.nodes
        return r
    }

    override fun bestMove(timeMs: Long): Int {
        val book = bookMove()
        if (book >= 0) return book
        return synchronized(lock) { think(SearchLimit(timeMs = timeMs), null, 0).best }
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

    /** Bên đang đi bị chiếu hết / hết nước. */
    private fun matedNow(): Boolean = legalList().isEmpty() && inCheck()

    private fun fastWin(): Int {
        for (mv in legalList()) { make(mv); val w = matedNow(); unmake(); if (w) return mv }
        return -1
    }

    private fun fastLosesNext(mv: Int): Boolean {
        make(mv)
        var lose = false
        if (repeated()) lose = true else for (r in legalList()) { make(r); if (matedNow()) lose = true; unmake(); if (lose) break }
        unmake()
        return lose
    }

    override fun levelMove(level: BotLevel, timeMs: Long, rnd: Random): Int {
        if (result != Outcome.ONGOING) return -1
        if (level != BotLevel.EASY) { val b = bookMove(); if (b >= 0) return b }
        val cfg = BotLevels.config(id, level, timeMs)
        return synchronized(lock) {
            val work = copyGame()
            Bot.pick(work, cfg, rnd, { work.fastWin() }, { work.fastLosesNext(it) }) { l, allowed, delta -> think(l, allowed, delta) }
        }
    }
}
