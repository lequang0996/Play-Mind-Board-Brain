package com.fws.playmind.core

import kotlin.math.abs
import kotlin.random.Random

/**
 * Cờ tướng (Xiangqi). Bàn 9 cột x 10 hàng, ô = rank*9 + file, rank 0 là đáy bên Đỏ.
 * Đỏ (dương) đi trước. Mã hóa nước đi: from | to<<7.
 * Quy tắc hòa đơn giản hóa: lặp lại 3 lần hoặc 120 nước không ăn quân.
 *
 * Bot: [XiangqiEngine] (tìm kiếm hiện đại + đánh giá tinh chỉnh Texel, Lazy SMP cho Siêu việt) và sách khai cuộc nhỏ.
 * Xem docs/BOT_GUIDE.md.
 */
class XiangqiGame : BoardGame, LeveledEngine {
    companion object {
        const val K = 1; const val A = 2; const val E = 3; const val H = 4; const val R = 5; const val C = 6; const val P = 7
        private val ORTH = intArrayOf(1, 0, -1, 0, 0, 1, 0, -1)
        private val DIAG = intArrayOf(1, 1, 1, -1, -1, 1, -1, -1)
        // df, dr, legdf, legdr
        private val HORSE = intArrayOf(1, 2, 0, 1, -1, 2, 0, 1, 1, -2, 0, -1, -1, -2, 0, -1, 2, 1, 1, 0, 2, -1, 1, 0, -2, 1, -1, 0, -2, -1, -1, 0)

        fun fromSq(m: Int) = m and 127
        fun toSq(m: Int) = (m shr 7) and 127
        fun mk(f: Int, t: Int) = f or (t shl 7)

        private val zr = Random(0x71A9C1)
        private val Z_PIECE = Array(15) { LongArray(90) { zr.nextLong() } }
        private val Z_SIDE = zr.nextLong()
        private fun zp(p: Int, sq: Int) = Z_PIECE[p + 7][sq]

        private val lock = Any()

        @Volatile var lastDepth = 0; private set
        @Volatile var lastNodes = 0L; private set
        private val engine by lazy { XiangqiEngine(20) }
        /** Số luồng của Siêu việt (Lazy SMP). Mặc định theo số nhân CPU; khi đo đặt bằng thuộc tính pm.xiangqi.threads. */
        @Volatile var threads: Int = System.getProperty("pm.xiangqi.threads")?.toIntOrNull()
            ?: minOf(4, maxOf(1, Runtime.getRuntime().availableProcessors() - 2))

        /** Sách khai cuộc: mỗi nước là "cột(a-i) + hàng(0-9)" cho ô đi và ô đến, Đỏ ở hàng 0. */
        private val BOOK: List<List<String>> = listOf(
            "h2e2 h9g7 h0g2 b9c7 i0h0 i9h9",          // pháo đầu - mã bình phong
            "h2e2 b9c7 h0g2 h9g7 i0h0 i9h9",
            "h2e2 h9g7 h0g2 g6g5 i0h0 b9c7",
            "c0e2 h7e7 h0g2 h9g7 i0h0 i9h9",          // tượng điền - pháo đầu của Đen
            "h0g2 h9g7 b0c2 b9c7 i0h0 i9h9",          // mã hoàng
            "g3g4 g6g5 h0g2 h9g7 b0c2 b9c7",          // tiên nhân chỉ lộ (tốt 7)
            "b2e2 b9c7 b0c2 h9g7 a0b0 i9h9",          // pháo đầu bên kia
        ).map { it.split(" ") }
    }

    override val id = "xiangqi"
    val board = IntArray(90)
    var side = 1; private set
    private var quiet = 0
    private var rk = 4
    private var bk = 85
    private var hash = 0L
    private val startBoard = IntArray(90)
    private var startRed = true

    private var uMove = IntArray(1024); private var uCap = IntArray(1024); private var uQuiet = IntArray(1024); private var uHash = LongArray(1024)
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
        hash = computeHash(); keys.add(hash)
        System.arraycopy(board, 0, startBoard, 0, 90); startRed = true
    }

    /** Dùng cho kiểm thử: thiết lập thế cờ từ bản đồ ô -> quân. */
    fun setPosition(pieces: Map<Int, Int>, redToMove: Boolean) {
        board.fill(0); hist.clear(); keys.clear(); sp = 0; quiet = 0
        result = Outcome.ONGOING; reason = EndReason.NONE
        for ((sq, p) in pieces) { board[sq] = p; if (p == K) rk = sq; if (p == -K) bk = sq }
        side = if (redToMove) 1 else -1
        hash = computeHash(); keys.add(hash)
        System.arraycopy(board, 0, startBoard, 0, 90); startRed = redToMove
        updateResult()
    }

    private fun computeHash(): Long {
        var h = 0L
        for (i in 0 until 90) if (board[i] != 0) h = h xor zp(board[i], i)
        if (side < 0) h = h xor Z_SIDE
        return h
    }

    fun pieceAt(sq: Int) = board[sq]
    fun kingSquare(color: Int) = if (color > 0) rk else bk
    override fun inCheck() = isAttacked(kingSquare(side), -side)

    private fun inPalace(f: Int, r: Int, color: Int) = f in 3..5 && (if (color > 0) r in 0..2 else r in 7..9)

    /** Ô [sq] có bị bên [by] (1 = Đỏ, -1 = Đen) chiếu tới không (kể cả tướng đối mặt). */
    fun isAttacked(sq: Int, by: Int): Boolean {
        val f = sq % 9; val r = sq / 9
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
        var i = 0
        while (i < HORSE.size) {
            val df = HORSE[i]; val dr = HORSE[i + 1]; val lf = HORSE[i + 2]; val lr = HORSE[i + 3]
            i += 4
            val hf = f - df; val hr = r - dr
            if (hf in 0..8 && hr in 0..9 && board[hr * 9 + hf] == by * H && board[(hr + lr) * 9 + (hf + lf)] == 0) return true
        }
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

    private fun ensure() {
        if (sp >= uMove.size) {
            val ns = uMove.size * 2
            uMove = uMove.copyOf(ns); uCap = uCap.copyOf(ns); uQuiet = uQuiet.copyOf(ns); uHash = uHash.copyOf(ns)
        }
    }

    private fun make(m: Int) {
        ensure()
        val from = m and 127; val to = (m shr 7) and 127
        val p = board[from]; val cap = board[to]
        uMove[sp] = m; uCap[sp] = cap; uQuiet[sp] = quiet; uHash[sp] = hash; sp++
        hash = hash xor Z_SIDE xor zp(p, from) xor zp(p, to)
        if (cap != 0) hash = hash xor zp(cap, to)
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
        board[from] = p; board[to] = uCap[sp]; quiet = uQuiet[sp]; hash = uHash[sp]
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
        make(move); hist.add(move); keys.add(hash)
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
        if (legalList().isEmpty()) {
            result = if (side > 0) Outcome.WIN_SECOND else Outcome.WIN_FIRST
            reason = if (inCheck()) EndReason.CHECKMATE else EndReason.NOMOVES
            return
        }
        if (repeatedPosition()) { result = if (side > 0) Outcome.WIN_FIRST else Outcome.WIN_SECOND; reason = EndReason.REPETITION; return }
        if (quiet >= 120) { result = Outcome.DRAW; reason = EndReason.FIFTY; return }
    }

    override fun forfeit(loser: Int, reason: EndReason) {
        if (result != Outcome.ONGOING) return
        result = if (loser == 0) Outcome.WIN_SECOND else Outcome.WIN_FIRST
        this.reason = reason
    }

    override fun copyGame(): XiangqiGame {
        val g = XiangqiGame()
        System.arraycopy(board, 0, g.board, 0, 90)
        g.side = side; g.quiet = quiet; g.rk = rk; g.bk = bk; g.hash = hash
        g.hist.clear(); g.hist.addAll(hist); g.keys.clear(); g.keys.addAll(keys)
        g.result = result; g.reason = reason; g.sp = 0
        System.arraycopy(startBoard, 0, g.startBoard, 0, 90); g.startRed = startRed
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

    override fun bestMove(timeMs: Long): Int {
        val book = bookMove()
        if (book >= 0) return book
        return synchronized(lock) { think(SearchLimit(timeMs = timeMs), null, 0).best }
    }

    /** Tìm kiếm bằng [XiangqiEngine] từ thế đầu + lịch sử (giữ luật lặp thế). Gọi trong synchronized(lock). */
    private fun think(lim: SearchLimit, allowed: IntArray?, delta: Int): RootResult {
        val r = engine.searchSmp(startBoard, startRed, hist, lim, allowed, delta, threads)
        lastDepth = r.depth; lastNodes = r.nodes
        return r
    }

    private fun name(sq: Int) = "" + ('a' + sq % 9) + (sq / 9)

    private fun bookMove(): Int {
        if (hist.size > 14 || result != Outcome.ONGOING) return -1
        val cand = ArrayList<String>()
        for (line in BOOK) {
            if (line.size <= hist.size) continue
            var ok = true
            for (i in hist.indices) if (line[i] != name(fromSq(hist[i])) + name(toSq(hist[i]))) { ok = false; break }
            if (ok) cand.add(line[hist.size])
        }
        if (cand.isEmpty()) return -1
        val s = cand[Random.nextInt(cand.size)]
        val f = (s[1] - '0') * 9 + (s[0] - 'a'); val t = (s[3] - '0') * 9 + (s[2] - 'a')
        return findMove(f, t)
    }

    private fun repeated(): Boolean {
        val lim = minOf(quiet, sp + keys.size - 1)
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
    private fun matedNow(): Boolean = legalList().isEmpty()

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
