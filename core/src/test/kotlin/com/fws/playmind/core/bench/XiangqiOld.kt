package com.fws.playmind.core.bench

import com.fws.playmind.core.*

import kotlin.math.abs
import kotlin.math.ln
import kotlin.random.Random

/**
 * Cờ tướng (Xiangqi). Bàn 9 cột x 10 hàng, ô = rank*9 + file, rank 0 là đáy bên Đỏ.
 * Đỏ (dương) đi trước. Mã hóa nước đi: from | to<<7.
 * Quy tắc hòa đơn giản hóa: lặp lại 3 lần hoặc 120 nước không ăn quân.
 *
 * Engine v2 (cấp cao thủ): PVS + bảng băm Zobrist, null-move, reverse-futility, futility/LMP, LMR,
 * aspiration window, tìm kiếm tĩnh, đánh giá (vật chất theo giai đoạn, cơ động, tốt qua sông, an toàn cung,
 * sĩ/tượng phòng thủ, pháo/xe ghim tướng) và sách khai cuộc nhỏ. Xem docs/BOT_GUIDE.md.
 */
/** BẢN SAO engine cờ tướng CŨ (trước XiangqiEngine), chỉ làm mốc so A/B trong bộ đo. Không dùng trong app. */
class XiangqiOld : BoardGame, LeveledEngine {
    companion object {
        const val K = 1; const val A = 2; const val E = 3; const val H = 4; const val R = 5; const val C = 6; const val P = 7
        private const val MATE = 30000
        private const val INF = 32000
        private const val MAXD = 50
        private const val MAXPLY = 90
        private val VAL = intArrayOf(0, 0, 200, 200, 430, 950, 460, 100)
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

        private const val TT_BITS = 19
        private const val TT_SIZE = 1 shl TT_BITS
        private val ttKey = LongArray(TT_SIZE)
        private val ttMove = IntArray(TT_SIZE)
        private val ttScore = IntArray(TT_SIZE)
        private val ttInfo = IntArray(TT_SIZE)
        private val lock = Any()

        @Volatile var lastDepth = 0; private set
        @Volatile var lastNodes = 0L; private set

        private val LMR = Array(64) { d -> IntArray(64) { m -> if (d < 3 || m < 3) 0 else (0.6 + ln(d.toDouble()) * ln(m.toDouble()) / 2.4).toInt() } }

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
    }

    /** Dùng cho kiểm thử: thiết lập thế cờ từ bản đồ ô -> quân. */
    fun setPosition(pieces: Map<Int, Int>, redToMove: Boolean) {
        board.fill(0); hist.clear(); keys.clear(); sp = 0; quiet = 0
        result = Outcome.ONGOING; reason = EndReason.NONE
        for ((sq, p) in pieces) { board[sq] = p; if (p == K) rk = sq; if (p == -K) bk = sq }
        side = if (redToMove) 1 else -1
        hash = computeHash(); keys.add(hash); updateResult()
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

    private fun makeNull() {
        ensure()
        uMove[sp] = 0; uCap[sp] = 0; uQuiet[sp] = quiet; uHash[sp] = hash; sp++
        hash = hash xor Z_SIDE; quiet++; side = -side
    }

    private fun unmakeNull() { sp--; side = -side; quiet = uQuiet[sp]; hash = uHash[sp] }

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

    override fun copyGame(): XiangqiOld {
        val g = XiangqiOld()
        System.arraycopy(board, 0, g.board, 0, 90)
        g.side = side; g.quiet = quiet; g.rk = rk; g.bk = bk; g.hash = hash
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

    // ───────────────────────── Đánh giá ─────────────────────────
    private val defenders = IntArray(2)   // số sĩ+tượng mỗi bên
    private val attackers = IntArray(2)   // số xe/pháo/mã mỗi bên (trọng số)
    private val palaceHits = IntArray(2)  // số lần quân của bên đó chiếu vào cung đối phương

    private fun evaluate(): Int {
        var mg = 0; var eg = 0
        var phase = 0
        defenders[0] = 0; defenders[1] = 0; attackers[0] = 0; attackers[1] = 0; palaceHits[0] = 0; palaceHits[1] = 0
        for (sq in 0 until 90) {
            val p = board[sq]; if (p == 0) continue
            val t = abs(p)
            if (t == A || t == E) defenders[if (p > 0) 0 else 1]++
            else if (t == R) { phase += 4; attackers[if (p > 0) 0 else 1] += 3 }
            else if (t == C) { phase += 2; attackers[if (p > 0) 0 else 1] += 2 }
            else if (t == H) { phase += 2; attackers[if (p > 0) 0 else 1] += 2 }
        }
        val ph = minOf(32, phase)
        for (sq in 0 until 90) {
            val p = board[sq]; if (p == 0) continue
            val t = abs(p); val red = p > 0; val c = if (red) 0 else 1
            val f = sq % 9; val r = sq / 9
            val rr = if (red) r else 9 - r
            val cen = 4 - abs(f - 4)
            var m = 0; var e = 0
            when (t) {
                P -> {
                    if (rr >= 5) {
                        m = 170 + (rr - 5) * 8 + cen * 6; e = 200 + (rr - 5) * 14 + cen * 4
                        if (rr >= 7 && f in 3..5) { m += 25; e += 20 }
                    } else { m = 90 + cen * 2; e = 100 }
                }
                A -> { m = 200; e = 160 }
                E -> { m = 200; e = 170 }
                H -> {
                    var mob = 0
                    var i = 0
                    while (i < HORSE.size) {
                        val nf = f + HORSE[i]; val nr = r + HORSE[i + 1]
                        val lf = f + HORSE[i + 2]; val lr = r + HORSE[i + 3]; i += 4
                        if (nf !in 0..8 || nr !in 0..9) continue
                        if (board[lr * 9 + lf] != 0) continue
                        val tp = board[nr * 9 + nf]
                        if (tp == 0 || (tp > 0) != red) {
                            mob++
                            if (inPalace(nf, nr, if (red) -1 else 1)) palaceHits[c]++
                        }
                    }
                    m = 400 + 5 * (mob - 4) + cen * 4 + minOf(rr, 7) * 3
                    e = 440 + 6 * (mob - 4) + cen * 2
                }
                C -> {
                    var mob = 0
                    var d = 0
                    while (d < 8) {
                        val df = ORTH[d]; val dr = ORTH[d + 1]; d += 2
                        var nf = f + df; var nr = r + dr
                        var screen = false
                        while (nf in 0..8 && nr in 0..9) {
                            val tp = board[nr * 9 + nf]
                            if (!screen) {
                                if (tp == 0) mob++ else screen = true
                            } else if (tp != 0) {
                                if ((tp > 0) != red) {
                                    mob += 2
                                    if (abs(tp) == K) { m += 28; e += 10 }                 // pháo nhắm tướng qua một ngòi
                                    if (inPalace(nf, nr, if (red) -1 else 1)) palaceHits[c]++
                                }
                                break
                            }
                            nf += df; nr += dr
                        }
                    }
                    m += 470 + 2 * (mob - 8) + cen * 3
                    e += 410 + 2 * (mob - 8)
                    if (f == 4 && rr in 2..4) m += 10      // pháo đầu
                }
                R -> {
                    var mob = 0
                    var d = 0
                    while (d < 8) {
                        val df = ORTH[d]; val dr = ORTH[d + 1]; d += 2
                        var nf = f + df; var nr = r + dr
                        while (nf in 0..8 && nr in 0..9) {
                            val tp = board[nr * 9 + nf]
                            if (inPalace(nf, nr, if (red) -1 else 1)) palaceHits[c]++
                            if (tp == 0) mob++ else { if ((tp > 0) != red) mob++; break }
                            nf += df; nr += dr
                        }
                    }
                    m = 950 + 3 * (mob - 8) + (if (rr >= 7) 12 else 0)
                    e = 960 + 4 * (mob - 8)
                }
                else -> {
                    m = 0; e = 0
                    // tướng nên ở trong cung, không ra cao nếu đối phương còn nhiều quân tấn công
                    if (rr >= 1 && attackers[1 - c] >= 6) m -= 10 * rr
                }
            }
            if (red) { mg += m; eg += e } else { mg -= m; eg -= e }
        }
        // phòng thủ cung: sĩ tượng càng đủ càng an toàn khi đối phương còn nhiều quân công
        for (c in 0..1) {
            val enemyAtt = attackers[1 - c]
            val dflt = defenders[c]
            val pen = enemyAtt * (4 - dflt) * 5 + palaceHits[1 - c] * 6 * (4 - minOf(dflt, 4)) / 2
            if (c == 0) mg -= pen else mg += pen
            val bonus = dflt * (6 + enemyAtt * 2)
            if (c == 0) mg += bonus else mg -= bonus
        }
        val score = (mg * ph + eg * (32 - ph)) / 32 + (if (side > 0) 10 else -10)
        return if (side > 0) score else -score
    }

    // ───────────────────────── Tìm kiếm ─────────────────────────
    private var deadline = 0L
    private var rootSide = 1
    private var nodes = 0L
    private var abort = false
    private val killers = Array(MAXPLY) { IntArray(2) }
    private val hh = Array(2) { IntArray(90 * 90) }
    private val bufs = Array(MAXPLY) { IntArray(128) }
    private val scores = Array(MAXPLY) { IntArray(128) }
    private val quiets = Array(MAXPLY) { IntArray(64) }

    override fun bestMove(timeMs: Long): Int {
        val book = bookMove()
        if (book >= 0) return book
        return synchronized(lock) { copyGame().searchRoot(timeMs) }
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

    private fun hasAttackers(color: Int): Boolean {
        var n = 0
        for (p in board) if (p * color > 0) { val t = abs(p); if (t == R || t == C || t == H) n++ }
        return n >= 2
    }

    private fun scoreMoves(ply: Int, n: Int, ttm: Int) {
        val mv = bufs[ply]; val sc = scores[ply]
        val sd = if (side > 0) 0 else 1
        for (i in 0 until n) {
            val m = mv[i]; val to = (m shr 7) and 127; val from = m and 127
            val cap = board[to]
            sc[i] = if (m == ttm) 2_000_000
            else if (cap != 0) 1_000_000 + VAL[abs(cap)] * 16 - VAL[abs(board[from])]
            else if (killers[ply][0] == m) 900_000
            else if (killers[ply][1] == m) 890_000
            else hh[sd][from * 90 + to]
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
    private fun drawScore() = if (side == rootSide) -30 else 30

    private fun scoreToTT(s: Int, ply: Int) = if (s > MATE - 300) s + ply else if (s < -MATE + 300) s - ply else s
    private fun scoreFromTT(s: Int, ply: Int) = if (s > MATE - 300) s - ply else if (s < -MATE + 300) s + ply else s

    private fun quiesce(alphaIn: Int, beta: Int, ply: Int): Int {
        var alpha = alphaIn
        if ((++nodes and 255L) == 0L && (nodes >= nodeLimit || System.nanoTime() > deadline)) abort = true
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
            val to = (m shr 7) and 127
            val cap = board[to]
            if (!inChk && cap != 0 && stand + VAL[abs(cap)] + 150 < alpha) continue   // delta
            val att = abs(board[m and 127])
            make(m)
            if (isAttacked(kingSquare(us), -us)) { unmake(); continue }
            if (!inChk && cap != 0 && VAL[abs(cap)] < VAL[att] && isAttacked(to, -us)) { unmake(); continue }   // ăn quân bị ăn lại, thiệt
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
        if ((++nodes and 255L) == 0L && (nodes >= nodeLimit || System.nanoTime() > deadline)) abort = true
        if (abort) return 0
        val pv = beta - alpha > 1
        if (ply > 0) {
            if (repeated()) return MATE - ply                 // bên vừa đi lặp lại thế cũ -> thua
            if (quiet >= 120) return drawScore()
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
            if (depth <= 5 && staticEval - 110 * depth >= beta && staticEval < MATE - 300) return staticEval
            if (nullOk && depth >= 3 && staticEval >= beta && hasAttackers(us)) {
                val r = 2 + depth / 4
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
            val to = (m shr 7) and 127; val from = m and 127
            val cap = board[to]
            val isQuiet = cap == 0
            make(m)
            if (isAttacked(kingSquare(us), -us)) { unmake(); continue }
            legal++
            val gives = isAttacked(kingSquare(-us), us)
            if (isQuiet && !inChk && !gives && legal > 1 && best > -MATE + 300) {
                if (depth <= 3 && staticEval + 120 * depth + 60 <= alpha) { unmake(); continue }
                if (depth <= 4 && legal > 8 + depth * depth) { unmake(); continue }
            }
            if (isQuiet && quietCount < 63) quiets[ply][quietCount++] = m
            var s: Int
            val nd = depth - 1
            if (legal == 1) s = -search(nd, -beta, -alpha, ply + 1, true)
            else {
                var red = 0
                if (depth >= 3 && isQuiet && !inChk && !gives) {
                    red = LMR[minOf(depth, 63)][minOf(legal, 63)]
                    if (pv) red--
                    if (killers[ply][0] == m || killers[ply][1] == m) red--
                    red -= hh[sd][from * 90 + to] / 6000
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
                        if (isQuiet) {
                            if (killers[ply][0] != m) { killers[ply][1] = killers[ply][0]; killers[ply][0] = m }
                            val bonus = depth * depth
                            hh[sd][from * 90 + to] = minOf(30000, hh[sd][from * 90 + to] + bonus)
                            for (q in 0 until quietCount - 1) {
                                val qm = quiets[ply][q]; val idx = (qm and 127) * 90 + ((qm shr 7) and 127)
                                hh[sd][idx] = maxOf(0, hh[sd][idx] - bonus / 2)
                            }
                        }
                        break
                    }
                }
            }
        }
        if (legal == 0) return -(MATE - ply)
        val flag = if (best >= beta) 1 else if (best > origAlpha) 0 else 2
        if (ttKey[slot] != hash || (ttInfo[slot] shr 2) <= depth + 1 || flag == 0) {
            ttKey[slot] = hash; ttMove[slot] = bestMove; ttScore[slot] = scoreToTT(best, ply); ttInfo[slot] = (depth shl 2) or flag
        }
        return best
    }

    private var nodeLimit = Long.MAX_VALUE

    private fun searchRoot(timeMs: Long): Int = searchRoot(SearchLimit(timeMs = timeMs), null, 0).best

    /**
     * Tìm kiếm có giới hạn (thời gian / số nút / độ sâu, mục 4.2 của BOT_GUIDE) trên các nước gốc [allowed] (null = mọi nước).
     * [delta] > 0: trả thêm các nước có điểm không kém nước tốt nhất quá [delta] (để chọn nhiễu), kiểm bằng tìm kiếm cửa sổ hẹp.
     */
    private fun searchRoot(lim: SearchLimit, allowed: IntArray?, delta: Int): RootResult {
        val start = System.nanoTime()
        deadline = if (lim.timeMs > 0) start + lim.timeMs * 1_000_000 else Long.MAX_VALUE
        val soft = if (lim.timeMs > 0) start + (lim.timeMs * 1_000_000 * 0.55).toLong() else Long.MAX_VALUE
        val nl = if (lim.nodes > 0) lim.nodes else Long.MAX_VALUE
        val maxDepth = if (lim.depth > 0) lim.depth else MAXD
        abort = false; nodes = 0; rootSide = side
        var root = legalList()
        if (allowed != null) { val f = root.filter { it in allowed }.toIntArray(); if (f.isNotEmpty()) root = f }
        if (root.isEmpty()) return RootResult(-1, 0, 0, 0)
        if (root.size == 1) return RootResult(root[0], 0, 0, 0)
        var best = root[0]
        var prevScore = 0
        for (i in 0 until MAXPLY) { killers[i][0] = 0; killers[i][1] = 0 }
        val rsc = IntArray(root.size)
        var completed = 0
        for (depth in 1..maxDepth) {
            nodeLimit = if (depth == 1) Long.MAX_VALUE else nl     // độ sâu 1 luôn chạy xong
            var delta = if (depth >= 5) 30 else INF
            var alpha = if (depth >= 5) maxOf(-INF, prevScore - delta) else -INF
            var beta = if (depth >= 5) minOf(INF, prevScore + delta) else INF
            var iterBest = -1; var iterScore = -INF
            while (true) {
                for (i in root.indices) {
                    val m = root[i]; val cap = board[(m shr 7) and 127]
                    rsc[i] = (if (m == best) 1_000_000 else 0) + (if (cap != 0) VAL[abs(cap)] * 10 - VAL[abs(board[m and 127])] else 0)
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
        var near = IntArray(0)
        if (delta > 0 && completed > 0) {
            abort = false; nodeLimit = Long.MAX_VALUE; deadline = Long.MAX_VALUE
            val thr = prevScore - delta
            val out = ArrayList<Int>()
            for (mv in root) {
                if (mv == best) continue
                make(mv)
                val sc = -search(completed - 1, -thr, -thr + 1, 1, true)
                unmake()
                if (sc >= thr) out.add(mv)
            }
            near = out.toIntArray()
        }
        return RootResult(best, prevScore, completed, nodes, near)
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
            Bot.pick(work, cfg, rnd, { work.fastWin() }, { work.fastLosesNext(it) }) { l, allowed, delta -> work.searchRoot(l, allowed, delta) }
        }
    }
}
