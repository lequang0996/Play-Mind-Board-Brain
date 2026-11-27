package com.fws.playmind.core

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Engine cờ tướng (Siêu việt và các cấp dưới), cùng thiết kế với [ChessEngine]:
 * danh sách quân + bảng nước đi tính sẵn, PVS + bảng băm dùng chung (Lazy SMP), null move, reverse futility, razoring,
 * LMP, cắt tỉa theo SEE/lịch sử, LMR, singular extension, IIR, killer/countermove/lịch sử/lịch sử nối tiếp/lịch sử ăn quân,
 * tìm kiếm tĩnh có SEE. Đánh giá tuyến tính có tham số ([XW]) để tinh chỉnh Texel.
 *
 * Ô = hàng * 9 + cột, hàng 0 là đáy bên Đỏ. Mã quân: loại | (màu << 3), màu 0 = Đỏ, 1 = Đen.
 * Loại: 1 Tướng, 2 Sĩ, 3 Tượng, 4 Mã, 5 Xe, 6 Pháo, 7 Tốt (giống [XiangqiGame]). Nước đi: từ | (đến << 7).
 * Luật của app: nước tạo lại thế cũ thì bên vừa đi thua; hết nước (kể cả không bị chiếu) là thua.
 */
internal object XQ {
    const val K = 1; const val A = 2; const val E = 3; const val H = 4; const val R = 5; const val C = 6; const val P = 7
    private fun ok(f: Int, r: Int) = f in 0..8 && r in 0..9
    private fun sq(f: Int, r: Int) = r * 9 + f
    fun inPalace(c: Int, s: Int): Boolean { val f = s % 9; val r = s / 9; return f in 3..5 && (if (c == 0) r in 0..2 else r in 7..9) }
    private fun ownHalf(c: Int, s: Int) = if (c == 0) s / 9 <= 4 else s / 9 >= 5
    fun crossed(c: Int, s: Int) = !ownHalf(c, s)

    private val DF = intArrayOf(0, 0, 1, -1); private val DR = intArrayOf(1, -1, 0, 0)   // hướng 0,1 là dọc
    /** RAY[s * 4 + d] = các ô theo hướng d từ s (không gồm s). */
    val RAY = Array(360) { i ->
        val s = i / 4; val d = i % 4
        val out = ArrayList<Int>(); var f = s % 9 + DF[d]; var r = s / 9 + DR[d]
        while (ok(f, r)) { out.add(sq(f, r)); f += DF[d]; r += DR[d] }
        out.toIntArray()
    }
    /** Mã: các cặp (đích, chân mã). */
    val HMOVE = Array(90) { s ->
        val out = ArrayList<Int>(); val f = s % 9; val r = s / 9
        for ((df, dr) in listOf(1 to 2, -1 to 2, 1 to -2, -1 to -2, 2 to 1, 2 to -1, -2 to 1, -2 to -1)) {
            if (!ok(f + df, r + dr)) continue
            val lf = f + (if (abs(df) == 2) df / 2 else 0); val lr = r + (if (abs(dr) == 2) dr / 2 else 0)
            out.add(sq(f + df, r + dr)); out.add(sq(lf, lr))
        }
        out.toIntArray()
    }
    /** Các cặp (ô mã đứng, chân mã) mà mã ở đó đá tới s. */
    val HATT = Array(90) { s ->
        val out = ArrayList<Int>()
        for (from in 0 until 90) { val m = HMOVE[from]; var i = 0; while (i < m.size) { if (m[i] == s) { out.add(from); out.add(m[i + 1]) }; i += 2 } }
        out.toIntArray()
    }
    val KMOVE = Array(2) { c -> Array(90) { s ->
        val out = ArrayList<Int>(); val f = s % 9; val r = s / 9
        for (d in 0..3) { val t = f + DF[d]; val u = r + DR[d]; if (ok(t, u) && inPalace(c, sq(t, u))) out.add(sq(t, u)) }
        out.toIntArray()
    } }
    val AMOVE = Array(2) { c -> Array(90) { s ->
        val out = ArrayList<Int>(); val f = s % 9; val r = s / 9
        for ((df, dr) in listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)) if (ok(f + df, r + dr) && inPalace(c, sq(f + df, r + dr))) out.add(sq(f + df, r + dr))
        out.toIntArray()
    } }
    /** Tượng: các cặp (đích, mắt tượng). */
    val EMOVE = Array(2) { c -> Array(90) { s ->
        val out = ArrayList<Int>(); val f = s % 9; val r = s / 9
        for ((df, dr) in listOf(2 to 2, 2 to -2, -2 to 2, -2 to -2)) {
            if (!ok(f + df, r + dr) || !ownHalf(c, sq(f + df, r + dr))) continue
            out.add(sq(f + df, r + dr)); out.add(sq(f + df / 2, r + dr / 2))
        }
        out.toIntArray()
    } }
    val PMOVE = Array(2) { c -> Array(90) { s ->
        val out = ArrayList<Int>(); val f = s % 9; val r = s / 9; val fw = if (c == 0) 1 else -1
        if (ok(f, r + fw)) out.add(sq(f, r + fw))
        if (crossed(c, s)) { if (f > 0) out.add(sq(f - 1, r)); if (f < 8) out.add(sq(f + 1, r)) }
        out.toIntArray()
    } }
    /** Các ô mà tốt màu c đứng ở đó thì ăn được s. */
    val PATT = Array(2) { c -> Array(90) { s ->
        val out = ArrayList<Int>()
        for (from in 0 until 90) if (s in PMOVE[c][from]) out.add(from)
        out.toIntArray()
    } }
    val PALACE = Array(2) { c -> (0 until 90).filter { inPalace(c, it) }.toIntArray() }
    val PAL = Array(2) { c -> BooleanArray(90) { inPalace(c, it) } }
}

/** Bố cục và giá trị khởi điểm của trọng số đánh giá cờ tướng. */
internal object XW {
    private var n = 0
    private fun alloc(k: Int): Int { val s = n; n += k; return s }
    val MAT = alloc(8)
    val PST = alloc(7 * 90)         // (loại - 1) * 90 + ô (góc nhìn Đỏ)
    val MOB_H = alloc(9); val MOB_R = alloc(18); val MOB_C = alloc(18)
    val ADV_MISS = alloc(3); val ELE_MISS = alloc(3)
    val KATT = alloc(16)
    val CANNON_HOLLOW = alloc(1); val CANNON_DOUBLE = alloc(1)
    val ROOK_OPEN = alloc(1); val PAWN_CONN = alloc(1)
    val HORSE_LEG = alloc(1)        // mã bị cản chân ở hướng tiến
    val TEMPO = alloc(1)
    val SIZE = n
    val MG = IntArray(SIZE)
    val EG = IntArray(SIZE)
    const val PHMAX = 32

    init { if (!XiangqiWeightsData.load(MG, EG)) defaults() }

    /** Giá trị khởi điểm (lấy từ đánh giá của engine cờ tướng cũ). */
    fun defaults() {
        MG.fill(0); EG.fill(0)
        val mat = intArrayOf(0, 0, 200, 200, 400, 950, 470, 90); val matE = intArrayOf(0, 0, 160, 170, 440, 960, 410, 100)
        for (t in 1..7) { MG[MAT + t] = mat[t]; EG[MAT + t] = matE[t] }
        for (s in 0 until 90) {
            val f = s % 9; val r = s / 9; val cen = 4 - abs(f - 4)
            fun set(t: Int, m: Int, e: Int) { MG[PST + (t - 1) * 90 + s] = m; EG[PST + (t - 1) * 90 + s] = e }
            if (r >= 5) {
                var m = 80 + (r - 5) * 8 + cen * 6; var e = 100 + (r - 5) * 14 + cen * 4
                if (r >= 7 && f in 3..5) { m += 25; e += 20 }
                if (r == 9) { m -= 40; e -= 40 }
                set(XQ.P, m, e)
            } else set(XQ.P, cen * 2 - 4, 0)
            set(XQ.H, cen * 4 + min(r, 7) * 3 - 12, cen * 2 - 4)
            set(XQ.C, cen * 3 + (if (f == 4 && r in 2..4) 10 else 0) - 6, 0)
            set(XQ.R, if (r >= 7) 12 else 0, 0)
            set(XQ.K, -8 * r - 4 * abs(f - 4), 0)
        }
        for (k in 0..8) { MG[MOB_H + k] = 5 * (k - 4); EG[MOB_H + k] = 6 * (k - 4) }
        for (k in 0..17) { MG[MOB_R + k] = 3 * (k - 8); EG[MOB_R + k] = 4 * (k - 8) }
        for (k in 0..17) { MG[MOB_C + k] = 2 * (k - 8); EG[MOB_C + k] = 2 * (k - 8) }
        val am = intArrayOf(0, -20, -50); val em = intArrayOf(0, -15, -40)
        for (k in 0..2) { MG[ADV_MISS + k] = am[k]; EG[ADV_MISS + k] = am[k] / 4; MG[ELE_MISS + k] = em[k]; EG[ELE_MISS + k] = em[k] / 4 }
        for (u in 0..15) { MG[KATT + u] = u * (u + 3) / 2; EG[KATT + u] = u * 2 }
        MG[CANNON_HOLLOW] = 60; EG[CANNON_HOLLOW] = 10
        MG[CANNON_DOUBLE] = 20; EG[CANNON_DOUBLE] = 5
        MG[ROOK_OPEN] = 15; EG[ROOK_OPEN] = 10
        MG[PAWN_CONN] = 15; EG[PAWN_CONN] = 20
        MG[HORSE_LEG] = -8; EG[HORSE_LEG] = -8
        MG[TEMPO] = 10; EG[TEMPO] = 5
    }
}

class XiangqiEngine(private val tt: ChessTT) {
    constructor(ttBits: Int = 20) : this(ChessTT(ttBits))
    companion object {
        const val MATE = 30000
        const val INF = 32000
        private const val NONE = -32500
        const val MAXPLY = 120
        private const val K = XQ.K; private const val A = XQ.A; private const val E = XQ.E; private const val H = XQ.H
        private const val R = XQ.R; private const val C = XQ.C; private const val P = XQ.P
        private const val EXACT = 0; private const val LOWER = 1; private const val UPPER = 2
        private val SEEV = intArrayOf(0, 20000, 200, 200, 420, 920, 450, 100)
        private val zr = java.util.Random(0x5EA7C0DE77L)
        private val ZP = Array(16) { LongArray(90) { zr.nextLong() } }
        private val ZSIDE = zr.nextLong()
        private val LMR = Array(64) { d -> IntArray(64) { m -> if (d == 0 || m == 0) 0 else (0.8 + ln(d.toDouble()) * ln(m.toDouble()) / 2.3).toInt() } }
        const val START_FEN = "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w"
        /** Chỉ số gọn của mã quân (0..13) cho bảng lịch sử nối tiếp. */
        private fun pidx(p: Int) = (p and 7) - 1 + 7 * (p shr 3)
    }

    // ───── trạng thái bàn cờ ─────
    private val pc = IntArray(90)
    private val plist = Array(2) { IntArray(16) }
    private val pcount = IntArray(2)
    private val pix = IntArray(90)
    private val cnt = IntArray(16)
    var stm = 0; private set
    private var half = 0
    private var hash = 0L
    private val kingSq = IntArray(2)

    private val STACK = 4096 + MAXPLY + 8
    private val uMove = IntArray(STACK); private val uCap = IntArray(STACK); private val uHalf = IntArray(STACK); private val uHash = LongArray(STACK)
    private var sp = 0
    private val posHash = LongArray(STACK)
    private var hp = 0

    private val ttMask = tt.mask
    private val ttKey = tt.key
    private val ttData = tt.data
    private var helperId = 0
    private val helpers = ArrayList<XiangqiEngine>()

    private val hist = Array(2) { IntArray(8100) }
    private val cont = Array(1260) { IntArray(1260) }
    private val capHist = Array(14) { Array(90) { IntArray(8) } }
    private val killers = Array(MAXPLY + 2) { IntArray(2) }
    private val counter = IntArray(1260)

    private val bufs = Array(MAXPLY + 2) { IntArray(160) }
    private val scr = Array(MAXPLY + 2) { IntArray(160) }
    private val quietsTried = Array(MAXPLY + 2) { IntArray(64) }
    private val capsTried = Array(MAXPLY + 2) { IntArray(32) }
    private val sEval = IntArray(MAXPLY + 2)
    private val curMove = IntArray(MAXPLY + 2)
    private val curPc = IntArray(MAXPLY + 2)
    private val excl = IntArray(MAXPLY + 2)

    private var nodes = 0L
    private var nodeLimit = Long.MAX_VALUE
    private var deadline = Long.MAX_VALUE
    @Volatile private var abort = false
    var lastDepth = 0; private set
    var lastNodes = 0L; private set

    init { setFen(START_FEN) }

    // ───────────────────────── bàn cờ ─────────────────────────
    private fun put(s: Int, p: Int) {
        val c = p shr 3
        pc[s] = p; pix[s] = pcount[c]; plist[c][pcount[c]++] = s; cnt[p]++
        hash = hash xor ZP[p][s]
        if ((p and 7) == K) kingSq[c] = s
    }

    private fun remove(s: Int) {
        val p = pc[s]; val c = p shr 3
        val i = pix[s]; val last = plist[c][--pcount[c]]
        plist[c][i] = last; pix[last] = i
        pc[s] = 0; cnt[p]--
        hash = hash xor ZP[p][s]
    }

    private fun movePiece(from: Int, to: Int) {
        val p = pc[from]; val c = p shr 3
        val i = pix[from]; plist[c][i] = to; pix[to] = i
        pc[to] = p; pc[from] = 0
        hash = hash xor ZP[p][from] xor ZP[p][to]
        if ((p and 7) == K) kingSq[c] = to
    }

    private fun clearBoard() { pc.fill(0); pcount.fill(0); cnt.fill(0); hash = 0; sp = 0; hp = 0; half = 0 }

    /** FEN cờ tướng: hàng 9 (Đen) trước; chữ hoa = Đỏ; K A B N R C P. */
    fun setFen(fen: String) {
        clearBoard()
        val parts = fen.trim().split(Regex("\\s+"))
        var r = 9; var f = 0
        for (ch in parts[0]) {
            when {
                ch == '/' -> { r--; f = 0 }
                ch.isDigit() -> f += ch.digitToInt()
                else -> {
                    val t = when (ch.lowercaseChar()) { 'k' -> K; 'a' -> A; 'b', 'e' -> E; 'n', 'h' -> H; 'r' -> R; 'c' -> C; else -> P }
                    put(r * 9 + f, t or (if (ch.isUpperCase()) 0 else 8)); f++
                }
            }
        }
        stm = if (parts.getOrElse(1) { "w" } == "b") 1 else 0
        if (stm == 1) hash = hash xor ZSIDE
        posHash[0] = hash
    }

    /** Thế từ bảng của [XiangqiGame] (dương = Đỏ). */
    fun setBoard(board: IntArray, redToMove: Boolean) {
        clearBoard()
        for (s in 0 until 90) { val v = board[s]; if (v != 0) put(s, abs(v) or (if (v < 0) 8 else 0)) }
        stm = if (redToMove) 0 else 1
        if (stm == 1) hash = hash xor ZSIDE
        posHash[0] = hash
    }

    fun fen(): String {
        val sb = StringBuilder()
        for (r in 9 downTo 0) {
            var empty = 0
            for (f in 0..8) {
                val p = pc[r * 9 + f]
                if (p == 0) { empty++; continue }
                if (empty > 0) { sb.append(empty); empty = 0 }
                val ch = " kabnrcp"[p and 7]
                sb.append(if (p shr 3 == 0) ch.uppercaseChar() else ch)
            }
            if (empty > 0) sb.append(empty)
            if (r > 0) sb.append('/')
        }
        sb.append(if (stm == 0) " w" else " b")
        return sb.toString()
    }

    /** Nạp thế đầu rồi đi lần lượt [moves]; giữ lịch sử thế cờ cho luật lặp thế. */
    fun load(board: IntArray, redToMove: Boolean, moves: List<Int>) {
        setBoard(board, redToMove)
        for (m in moves) { require(legalMoves().contains(m)) { "nước không hợp lệ khi nạp: $m" }; make(m) }
        if (sp > 0) {
            val keep = minOf(hp, half)
            System.arraycopy(posHash, hp - keep, posHash, 0, keep + 1)
            hp = keep; sp = 0
        }
    }

    fun inCheck(): Boolean = attacked(kingSq[stm], stm xor 1)

    /** Ô [s] có bị bên [by] chiếu/đe dọa không (xe, pháo, mã, tốt, tướng đối mặt). */
    private fun attacked(s: Int, by: Int): Boolean {
        val o = by shl 3
        for (d in 0..3) {
            val ray = XQ.RAY[s * 4 + d]
            var i = 0
            while (i < ray.size && pc[ray[i]] == 0) i++
            if (i >= ray.size) continue
            val p = pc[ray[i]]
            if (p == (R or o) || (d < 2 && p == (K or o))) return true
            i++
            while (i < ray.size && pc[ray[i]] == 0) i++
            if (i < ray.size && pc[ray[i]] == (C or o)) return true
        }
        val ha = XQ.HATT[s]; var j = 0
        while (j < ha.size) { if (pc[ha[j]] == (H or o) && pc[ha[j + 1]] == 0) return true; j += 2 }
        for (f in XQ.PATT[by][s]) if (pc[f] == (P or o)) return true
        return false
    }

    private fun gen(out: IntArray, capsOnly: Boolean): Int {
        var n = 0
        val us = stm; val them = us xor 1
        val lst = plist[us]
        for (k in 0 until pcount[us]) {
            val s = lst[k]; val t = pc[s] and 7
            when (t) {
                R -> for (d in 0..3) {
                    val ray = XQ.RAY[s * 4 + d]
                    for (to in ray) {
                        val q = pc[to]
                        if (q == 0) { if (!capsOnly) out[n++] = s or (to shl 7) }
                        else { if (q shr 3 == them) out[n++] = s or (to shl 7); break }
                    }
                }
                C -> for (d in 0..3) {
                    val ray = XQ.RAY[s * 4 + d]
                    var i = 0
                    while (i < ray.size && pc[ray[i]] == 0) { if (!capsOnly) out[n++] = s or (ray[i] shl 7); i++ }
                    i++
                    while (i < ray.size && pc[ray[i]] == 0) i++
                    if (i < ray.size && pc[ray[i]] shr 3 == them) out[n++] = s or (ray[i] shl 7)
                }
                H -> { val hm = XQ.HMOVE[s]; var i = 0
                    while (i < hm.size) {
                        val to = hm[i]; if (pc[hm[i + 1]] == 0) { val q = pc[to]; if (if (q == 0) !capsOnly else q shr 3 == them) out[n++] = s or (to shl 7) }
                        i += 2
                    }
                }
                E -> { val em = XQ.EMOVE[us][s]; var i = 0
                    while (i < em.size) {
                        val to = em[i]; if (pc[em[i + 1]] == 0) { val q = pc[to]; if (if (q == 0) !capsOnly else q shr 3 == them) out[n++] = s or (to shl 7) }
                        i += 2
                    }
                }
                else -> {
                    val tg = when (t) { K -> XQ.KMOVE[us][s]; A -> XQ.AMOVE[us][s]; else -> XQ.PMOVE[us][s] }
                    for (to in tg) { val q = pc[to]; if (if (q == 0) !capsOnly else q shr 3 == them) out[n++] = s or (to shl 7) }
                }
            }
        }
        return n
    }

    private fun make(m: Int) {
        val from = m and 127; val to = (m shr 7) and 127
        val cap = pc[to]
        uMove[sp] = m; uCap[sp] = cap; uHalf[sp] = half; uHash[sp] = hash; sp++
        hash = hash xor ZSIDE
        if (cap != 0) remove(to)
        movePiece(from, to)
        half = if (cap != 0) 0 else half + 1
        stm = stm xor 1
        hp++; posHash[hp] = hash
    }

    private fun unmake() {
        sp--; hp--
        val m = uMove[sp]; val from = m and 127; val to = (m shr 7) and 127
        stm = stm xor 1
        movePiece(to, from)
        val cap = uCap[sp]
        if (cap != 0) put(to, cap)
        half = uHalf[sp]; hash = uHash[sp]
    }

    private fun makeNull() {
        uMove[sp] = 0; uCap[sp] = 0; uHalf[sp] = half; uHash[sp] = hash; sp++
        hash = hash xor ZSIDE; half++; stm = stm xor 1
        hp++; posHash[hp] = hash
    }

    private fun unmakeNull() { sp--; hp--; stm = stm xor 1; half = uHalf[sp]; hash = uHash[sp] }

    private fun legal(m: Int): Boolean { make(m); val ok = !attacked(kingSq[stm xor 1], stm); unmake(); return ok }

    fun legalMoves(): IntArray {
        val buf = IntArray(160); val n = gen(buf, false)
        val out = IntArray(n); var c = 0
        for (i in 0 until n) if (legal(buf[i])) out[c++] = buf[i]
        return out.copyOf(c)
    }

    fun perft(depth: Int): Long {
        if (depth == 0) return 1
        val buf = IntArray(160); val n = gen(buf, false)
        var total = 0L
        for (i in 0 until n) {
            make(buf[i])
            if (!attacked(kingSq[stm xor 1], stm)) total += perft(depth - 1)
            unmake()
        }
        return total
    }

    private fun isRepetition(): Boolean {
        var i = hp - 4; val lim = hp - half
        while (i >= lim && i >= 0) { if (posHash[i] == hash) return true; i -= 2 }
        return false
    }

    private fun attackers(c: Int): Int { val o = c shl 3; return cnt[R or o] + cnt[H or o] + cnt[C or o] }
    private fun noMaterial(): Boolean = attackers(0) + attackers(1) + cnt[P] + cnt[P or 8] == 0

    // ───────────────────────── SEE ─────────────────────────
    private val seeGain = IntArray(40)
    private val seeRemoved = IntArray(40)

    /** Quân rẻ nhất của bên [by] ăn được ô [s] (ô của quân đó, -1 nếu không có). Không xét ghim. */
    private fun leastAttacker(s: Int, by: Int): Int {
        val o = by shl 3
        for (f in XQ.PATT[by][s]) if (pc[f] == (P or o)) return f
        if (XQ.PAL[by][s]) for (f in XQ.AMOVE[by][s]) if (pc[f] == (A or o)) return f
        if (!XQ.crossed(by, s)) run { val em = XQ.EMOVE[by][s]; var i = 0; while (i < em.size) { if (pc[em[i]] == (E or o) && pc[em[i + 1]] == 0) return em[i]; i += 2 } }
        run { val ha = XQ.HATT[s]; var i = 0; while (i < ha.size) { if (pc[ha[i]] == (H or o) && pc[ha[i + 1]] == 0) return ha[i]; i += 2 } }
        var cannon = -1; var rook = -1
        for (d in 0..3) {
            val ray = XQ.RAY[s * 4 + d]
            var i = 0
            while (i < ray.size && pc[ray[i]] == 0) i++
            if (i >= ray.size) continue
            if (pc[ray[i]] == (R or o) && rook < 0) rook = ray[i]
            i++
            while (i < ray.size && pc[ray[i]] == 0) i++
            if (i < ray.size && pc[ray[i]] == (C or o) && cannon < 0) cannon = ray[i]
        }
        if (cannon >= 0) return cannon
        if (rook >= 0) return rook
        if (XQ.PAL[by][s]) for (f in XQ.KMOVE[by][s]) if (pc[f] == (K or o)) return f
        return -1
    }

    private fun see(m: Int): Int {
        val from = m and 127; val to = (m shr 7) and 127
        val target = pc[to]; val mover = pc[from]
        var d = 0
        seeGain[0] = SEEV[target and 7]
        var lastVal = SEEV[mover and 7]
        // thực hiện tạm trên mảng pc (khôi phục ở cuối)
        pc[from] = 0; pc[to] = mover
        var nRem = 0
        var side = stm xor 1
        while (true) {
            val a = leastAttacker(to, side)
            if (a < 0) break
            d++
            seeGain[d] = lastVal - seeGain[d - 1]
            if (max(-seeGain[d - 1], seeGain[d]) < 0) break
            val p = pc[a]
            lastVal = SEEV[p and 7]
            seeRemoved[nRem++] = a or (p shl 8)
            pc[a] = 0; pc[to] = p
            side = side xor 1
            if (d >= 36) break
        }
        for (i in nRem - 1 downTo 0) { val a = seeRemoved[i] and 255; pc[a] = seeRemoved[i] shr 8 }
        pc[to] = target; pc[from] = mover
        while (d > 0) { seeGain[d - 1] = -max(-seeGain[d - 1], seeGain[d]); d-- }
        return seeGain[0]
    }

    // ───────────────────────── đánh giá ─────────────────────────
    var tracing = false
    val trace = IntArray(XW.SIZE)
    var tracePhase = 0; private set
    var traceScale = 64; private set
    private var mgS = 0; private var egS = 0
    var wMg: IntArray = XW.MG; var wEg: IntArray = XW.EG

    @Suppress("NOTHING_TO_INLINE")
    private inline fun add(idx: Int, coef: Int) {
        mgS += wMg[idx] * coef; egS += wEg[idx] * coef
        if (tracing) trace[idx] += coef
    }

    private val evKey = LongArray(1 shl 16); private val evVal = IntArray(1 shl 16)
    private val filePawns = Array(2) { IntArray(9) }

    fun evaluate(): Int {
        if (tracing) return evaluateFull()
        val i = (hash and 0xFFFF).toInt()
        if (evKey[i] == hash) return evVal[i]
        val v = evaluateFull()
        evKey[i] = hash; evVal[i] = v
        return v
    }

    fun evaluateFull(): Int {
        mgS = 0; egS = 0
        if (tracing) trace.fill(0)
        filePawns[0].fill(0); filePawns[1].fill(0)
        for (c in 0..1) { val o = c shl 3; for (k in 0 until pcount[c]) { val s = plist[c][k]; if (pc[s] == (P or o)) filePawns[c][s % 9]++ } }
        var phase = 0
        for (c in 0..1) {
            val sg = if (c == 0) 1 else -1; val e = c xor 1; val o = c shl 3
            val ek = kingSq[e]
            var units = 0; var adv = 0; var ele = 0
            for (k in 0 until pcount[c]) {
                val s = plist[c][k]; val p = pc[s]; val t = p and 7
                val rel = if (c == 0) s else (9 - s / 9) * 9 + s % 9
                add(XW.MAT + t, sg); add(XW.PST + (t - 1) * 90 + rel, sg)
                when (t) {
                    A -> adv++
                    E -> ele++
                    H -> {
                        phase += 2
                        var mob = 0; var hits = 0
                        val hm = XQ.HMOVE[s]; var i = 0
                        while (i < hm.size) {
                            val to = hm[i]
                            if (pc[hm[i + 1]] == 0) {
                                if (pc[to] == 0 || pc[to] shr 3 == e) mob++
                                if (XQ.PAL[e][to]) hits++
                            }
                            i += 2
                        }
                        add(XW.MOB_H + mob, sg)
                        // chân mã ở hướng tiến bị cản
                        val fwd = s + (if (c == 0) 9 else -9)
                        if (fwd in 0..89 && pc[fwd] != 0) add(XW.HORSE_LEG, sg)
                        if (hits > 0) units += 2
                    }
                    R -> {
                        phase += 4
                        var mob = 0; var hits = 0
                        for (d in 0..3) for (to in XQ.RAY[s * 4 + d]) {
                            val q = pc[to]
                            if (XQ.PAL[e][to]) hits++
                            if (q == 0) mob++ else { if (q shr 3 == e) mob++; break }
                        }
                        add(XW.MOB_R + min(mob, 17), sg)
                        if (filePawns[c][s % 9] == 0) add(XW.ROOK_OPEN, sg)
                        if (hits > 0) units += if (hits >= 3) 3 else 2
                    }
                    C -> {
                        phase += 2
                        var mob = 0; var hits = 0
                        for (d in 0..3) {
                            val ray = XQ.RAY[s * 4 + d]
                            var i = 0
                            while (i < ray.size && pc[ray[i]] == 0) { mob++; i++ }
                            i++
                            while (i < ray.size) {
                                val q = pc[ray[i]]
                                if (XQ.PAL[e][ray[i]]) hits++
                                if (q != 0) { if (q shr 3 == e) mob++; break }
                                i++
                            }
                        }
                        add(XW.MOB_C + min(mob, 17), sg)
                        if (hits > 0) units += 2
                        if (s % 9 == ek % 9) {
                            var between = 0
                            val lo = min(s, ek); val hi = max(s, ek)
                            var q = lo + 9
                            while (q < hi) { if (pc[q] != 0) between++; q += 9 }
                            if (between == 0) add(XW.CANNON_HOLLOW, sg) else if (between == 2) add(XW.CANNON_DOUBLE, sg)
                        }
                    }
                    P -> if (XQ.crossed(c, s)) {
                        var hits = 0
                        for (to in XQ.PMOVE[c][s]) if (XQ.PAL[e][to]) hits++
                        if (hits > 0) units += 1
                        val f = s % 9
                        if (f < 8 && pc[s + 1] == (P or o)) add(XW.PAWN_CONN, sg)
                    }
                }
            }
            add(XW.ADV_MISS + (2 - min(adv, 2)), sg)
            add(XW.ELE_MISS + (2 - min(ele, 2)), sg)
            add(XW.KATT + min(units, 15), sg)
        }
        add(XW.TEMPO, if (stm == 0) 1 else -1)
        val ph = min(XW.PHMAX, phase)
        var score = (mgS * ph + egS * (XW.PHMAX - ph)) / XW.PHMAX
        val strong = if (score > 0) 0 else 1
        val so = strong shl 3
        val scale = if (attackers(strong) == 0 && cnt[P or so] == 0) 0
            else if (attackers(strong) == 0 && cnt[P or so] == 1) 32 else 64
        score = score * scale / 64
        tracePhase = ph; traceScale = scale
        return if (stm == 0) score else -score
    }

    // ───────────────────────── bảng băm ─────────────────────────
    private fun ttStore(key: Long, move: Int, score: Int, eval: Int, depth: Int, flag: Int) {
        val i = (key and ttMask.toLong()).toInt()
        val old = ttData[i]
        val same = (ttKey[i] xor old) == key
        val g = tt.gen
        if (same || ((old ushr 30) and 3L).toInt() != (g and 3) || depth + 3 >= ((old ushr 22) and 255L).toInt() - 1 || flag == EXACT) {
            val mvKeep = if (move == 0 && same) (old and 0xFFFFF).toInt() else move
            val d = (mvKeep.toLong() and 0xFFFFF) or (flag.toLong() shl 20) or ((depth + 1).toLong().coerceIn(0, 255) shl 22) or
                ((g and 3).toLong() shl 30) or ((score + 32768).toLong() shl 32) or ((eval + 32768).toLong() shl 48)
            ttData[i] = d
            ttKey[i] = key xor d
        }
    }

    // ───────────────────────── tìm kiếm ─────────────────────────
    private fun checkTime() {
        if (nodes >= nodeLimit || System.nanoTime() > deadline || (helperId > 0 && tt.stop)) abort = true
    }

    private fun scoreToTT(s: Int, ply: Int) = if (s >= MATE - 500) s + ply else if (s <= -MATE + 500) s - ply else s
    private fun scoreFromTT(s: Int, ply: Int) = if (s >= MATE - 500) s - ply else if (s <= -MATE + 500) s + ply else s
    private fun bonus(depth: Int) = min(1800, 16 * depth * depth + 40 * depth)
    private fun upd(t: IntArray, i: Int, b: Int) { t[i] += b - t[i] * abs(b) / 16384 }

    private fun contIdx(ply: Int, back: Int): Int {
        val p = ply - back
        return if (p < 0 || curMove[p] == 0) -1 else pidx(curPc[p]) * 90 + ((curMove[p] shr 7) and 127)
    }

    private fun quietScore(m: Int, ply: Int): Int {
        val from = m and 127; val to = (m shr 7) and 127
        val pi = pidx(pc[from]) * 90 + to
        var s = hist[stm][from * 90 + to]
        val c1 = contIdx(ply, 1); if (c1 >= 0) s += cont[c1][pi]
        val c2 = contIdx(ply, 2); if (c2 >= 0) s += cont[c2][pi]
        return s
    }

    private fun scoreMoves(ply: Int, n: Int, ttm: Int) {
        val mvs = bufs[ply]; val sc = scr[ply]
        val k0 = killers[ply][0]; val k1 = killers[ply][1]
        val c1 = contIdx(ply, 1)
        val cm = if (c1 >= 0) counter[c1] else 0
        for (i in 0 until n) {
            val m = mvs[i]
            val to = (m shr 7) and 127
            val cap = pc[to] and 7
            sc[i] = when {
                m == ttm -> 30_000_000
                cap != 0 -> {
                    val base = SEEV[cap] * 16 + capHist[pidx(pc[m and 127])][to][cap] / 32
                    if (see(m) >= 0) 20_000_000 + base else -10_000_000 + base
                }
                m == k0 -> 19_000_000
                m == k1 -> 18_900_000
                m == cm -> 18_800_000
                else -> quietScore(m, ply)
            }
        }
    }

    private fun pick(ply: Int, n: Int, i: Int) {
        val mvs = bufs[ply]; val sc = scr[ply]
        var best = i
        for (j in i + 1 until n) if (sc[j] > sc[best]) best = j
        if (best != i) {
            val tm = mvs[i]; mvs[i] = mvs[best]; mvs[best] = tm
            val ts = sc[i]; sc[i] = sc[best]; sc[best] = ts
        }
    }

    private fun isQuiet(m: Int) = pc[(m shr 7) and 127] == 0

    private fun qsearch(alphaIn: Int, beta: Int, ply: Int): Int {
        var alpha = alphaIn
        nodes++
        if ((nodes and 1023L) == 0L) checkTime()
        if (abort) return 0
        if (ply >= MAXPLY) return evaluate()
        val inChk = inCheck()
        val i = (hash and ttMask.toLong()).toInt()
        var ttm = 0; var ttEval = NONE
        val pv = beta - alpha > 1
        val d0 = ttData[i]
        if ((ttKey[i] xor d0) == hash) {
            ttm = (d0 and 0xFFFFF).toInt()
            val s = scoreFromTT(((d0 ushr 32) and 0xFFFF).toInt() - 32768, ply)
            val fl = ((d0 ushr 20) and 3).toInt()
            ttEval = ((d0 ushr 48) and 0xFFFF).toInt() - 32768
            if (!pv && (fl == EXACT || (fl == LOWER && s >= beta) || (fl == UPPER && s <= alpha))) return s
        }
        var best: Int; val stand: Int
        if (!inChk) {
            stand = if (ttEval != NONE) ttEval else evaluate()
            if (stand >= beta) return stand
            if (stand > alpha) alpha = stand
            best = stand
        } else { stand = NONE; best = -INF }
        val n = gen(bufs[ply], !inChk)
        scoreMoves(ply, n, if (ttm != 0 && (inChk || !isQuiet(ttm))) ttm else 0)
        var legalN = 0; var bestMove = 0
        val origAlpha = alpha
        for (k in 0 until n) {
            pick(ply, n, k)
            val m = bufs[ply][k]
            if (!inChk) {
                if (scr[ply][k] < 0) continue
                if (stand + SEEV[pc[(m shr 7) and 127] and 7] + 200 <= alpha) continue
            }
            make(m)
            if (attacked(kingSq[stm xor 1], stm)) { unmake(); continue }
            legalN++
            curMove[ply] = m; curPc[ply] = pc[(m shr 7) and 127]
            val s = -qsearch(-beta, -alpha, ply + 1)
            unmake()
            if (abort) return 0
            if (s > best) {
                best = s; bestMove = m
                if (s > alpha) { alpha = s; if (s >= beta) break }
            }
        }
        if (inChk && legalN == 0) return -(MATE - ply)
        ttStore(hash, bestMove, scoreToTT(best, ply), if (stand == NONE) 0 else stand, 0,
            if (best >= beta) LOWER else if (best > origAlpha) EXACT else UPPER)
        return best
    }

    private var rootDepth = 0

    private fun search(depthIn: Int, alphaIn: Int, betaIn: Int, ply: Int, cut: Boolean): Int {
        var alpha = alphaIn; var beta = betaIn; var depth = depthIn
        val pv = beta - alpha > 1
        val inChk = inCheck()
        if (depth <= 0) { if (!inChk) return qsearch(alpha, beta, ply); depth = 1 }
        nodes++
        if ((nodes and 1023L) == 0L) checkTime()
        if (abort) return 0
        if (ply >= MAXPLY) return evaluate()
        val excluded = excl[ply]
        if (ply > 0 && excluded == 0) {
            if (isRepetition()) return MATE - ply            // bên vừa đi lặp thế -> thua
            if (half >= 120 || noMaterial()) return 0
            alpha = max(alpha, -MATE + ply); beta = min(beta, MATE - ply - 1)
            if (alpha >= beta) return alpha
        }
        val key = if (excluded != 0) hash xor (excluded.toLong() * -0x61c8864680b583ebL) else hash
        val ti = (key and ttMask.toLong()).toInt()
        var ttm = 0; var ttS = NONE; var ttD = -1; var ttF = UPPER; var ttEval = NONE
        val d0 = ttData[ti]
        val ttHit = (ttKey[ti] xor d0) == key
        if (ttHit) {
            ttm = (d0 and 0xFFFFF).toInt()
            ttF = ((d0 ushr 20) and 3).toInt()
            ttD = ((d0 ushr 22) and 255).toInt() - 1
            ttS = scoreFromTT(((d0 ushr 32) and 0xFFFF).toInt() - 32768, ply)
            ttEval = ((d0 ushr 48) and 0xFFFF).toInt() - 32768
            if (!pv && ttD >= depth && (ttF == EXACT || (ttF == LOWER && ttS >= beta) || (ttF == UPPER && ttS <= alpha))) {
                if (ttm != 0 && ttS >= beta && plausible(ttm) && isQuiet(ttm)) updateQuietBonus(ttm, ply, bonus(depth))
                return ttS
            }
        }
        var eval: Int
        if (inChk) { eval = NONE; sEval[ply] = NONE }
        else {
            eval = if (ttHit && ttEval != NONE) ttEval else evaluate()
            sEval[ply] = eval
            if (ttHit && ttS != NONE && ((ttF == LOWER && ttS > eval) || (ttF == UPPER && ttS < eval) || ttF == EXACT)) eval = ttS
        }
        val improving = !inChk && ply >= 2 && sEval[ply - 2] != NONE && sEval[ply] > sEval[ply - 2]
        killers[ply + 1][0] = 0; killers[ply + 1][1] = 0

        if (!pv && !inChk && excluded == 0) {
            if (depth <= 8 && eval - (90 * depth - (if (improving) 70 else 0)) >= beta && eval < MATE - 500) return (eval + beta) / 2
            if (depth <= 3 && eval + 250 + 200 * depth <= alpha) {
                val q = qsearch(alpha, alpha + 1, ply)
                if (q <= alpha) return q
            }
            if (depth >= 3 && eval >= beta && ply > 0 && curMove[ply - 1] != 0 && attackers(stm) > 0 && beta > -MATE + 500) {
                val r = 3 + depth / 3 + min(3, (eval - beta) / 200)
                curMove[ply] = 0; curPc[ply] = 0
                makeNull()
                val s = -search(depth - r, -beta, -beta + 1, ply + 1, !cut)
                unmakeNull()
                if (abort) return 0
                if (s >= beta) return if (s >= MATE - 500) beta else s
            }
        }
        if (ttm != 0 && !plausible(ttm)) ttm = 0
        if (ttm == 0 && depth >= 4 && (pv || cut)) depth--

        var singularExt = 0
        if (ply > 0 && excluded == 0 && depth >= 7 && ttm != 0 && ttD >= depth - 3 && ttF != UPPER && abs(ttS) < MATE - 500 && legalTT(ttm)) {
            val sBeta = ttS - 2 * depth
            excl[ply] = ttm
            val s = search((depth - 1) / 2, sBeta - 1, sBeta, ply, cut)
            excl[ply] = 0
            if (abort) return 0
            if (s < sBeta) singularExt = if (!pv && s < sBeta - 24) 2 else 1
            else if (sBeta >= beta) return sBeta
            else if (ttS >= beta) singularExt = -1
        }

        val n = gen(bufs[ply], false)
        scoreMoves(ply, n, ttm)
        var legalN = 0; var nQuiet = 0; var nCap = 0
        var best = -INF; var bestMove = 0
        val origAlpha = alpha
        val us = stm
        var skipQuiets = false
        val ttCapture = ttm != 0 && !isQuiet(ttm)
        for (k in 0 until n) {
            pick(ply, n, k)
            val m = bufs[ply][k]
            if (m == excluded) continue
            val to = (m shr 7) and 127
            val quiet = isQuiet(m)
            if (quiet && skipQuiets && m != ttm) continue
            val hscore = if (quiet) quietScore(m, ply) else 0
            if (ply > 0 && best > -MATE + 500 && attackers(us) > 0) {
                val lmrD = max(0, depth - 1 - LMR[min(depth, 63)][min(legalN + 1, 63)])
                if (quiet) {
                    if (!inChk && legalN >= (3 + depth * depth) / (if (improving) 1 else 2)) { skipQuiets = true; continue }
                    if (!inChk && lmrD <= 6 && eval != NONE && eval + 130 + 110 * lmrD <= alpha) { skipQuiets = true; continue }
                    if (lmrD <= 3 && hscore < -3500 * depth) continue
                    if (lmrD <= 7 && see(m) < -30 * lmrD * lmrD) continue
                } else if (depth <= 6 && scr[ply][k] < 0 && see(m) < -100 * depth) continue
            }
            make(m)
            if (attacked(kingSq[us], stm)) { unmake(); continue }
            legalN++
            val gives = inCheck()
            curMove[ply] = m; curPc[ply] = pc[to]
            var ext = 0
            if (m == ttm && singularExt != 0) ext = singularExt
            else if (gives && ply < 2 * rootDepth) ext = 1
            val newDepth = depth - 1 + ext
            var s: Int
            if (legalN == 1) s = -search(newDepth, -beta, -alpha, ply + 1, if (pv) false else !cut)
            else {
                var r = 0
                if (depth >= 3 && legalN > 1 + (if (pv) 1 else 0) && (quiet || scr[ply][k] < 0)) {
                    r = LMR[min(depth, 63)][min(legalN, 63)]
                    if (!pv) r++
                    if (cut) r++
                    if (!improving) r++
                    if (gives) r--
                    if (quiet) {
                        if (m == killers[ply][0] || m == killers[ply][1]) r--
                        r -= hscore / 7000
                    }
                    if (ttCapture) r++
                    r = r.coerceIn(0, max(0, newDepth - 1))
                }
                s = -search(newDepth - r, -alpha - 1, -alpha, ply + 1, true)
                if (s > alpha && r > 0) s = -search(newDepth, -alpha - 1, -alpha, ply + 1, !cut)
                if (s > alpha && s < beta) s = -search(newDepth, -beta, -alpha, ply + 1, false)
            }
            unmake()
            if (abort) return 0
            if (s > best) {
                best = s; bestMove = m
                if (s > alpha) {
                    alpha = s
                    if (s >= beta) {
                        val b = bonus(depth)
                        if (quiet) {
                            updateQuietBonus(m, ply, b)
                            for (q in 0 until nQuiet) updateQuietBonus(quietsTried[ply][q], ply, -b)
                        } else upd(capHist[pidx(pc[m and 127])][to], pc[to] and 7, b)
                        for (q in 0 until nCap) {
                            val cm = capsTried[ply][q]; val ct = (cm shr 7) and 127
                            upd(capHist[pidx(pc[cm and 127])][ct], pc[ct] and 7, -b)
                        }
                        break
                    }
                }
            }
            if (quiet) { if (nQuiet < 64) quietsTried[ply][nQuiet++] = m }
            else if (nCap < 32) capsTried[ply][nCap++] = m
        }
        // luật app: hết nước đi là thua (kể cả không bị chiếu)
        if (legalN == 0) return if (excluded != 0) alpha else -(MATE - ply)
        if (excluded == 0) {
            ttStore(key, bestMove, scoreToTT(best, ply), if (sEval[ply] == NONE) 0 else sEval[ply], depth,
                if (best >= beta) LOWER else if (best > origAlpha) EXACT else UPPER)
        }
        return best
    }

    /** Kiểm nhanh: ô đi có quân của bên tới lượt, ô đến không có quân mình. */
    private fun plausible(m: Int): Boolean {
        val from = m and 127; val to = (m shr 7) and 127
        if (from >= 90 || to >= 90) return false
        val p = pc[from]; val q = pc[to]
        return p != 0 && (p shr 3) == stm && (q == 0 || (q shr 3) != stm)
    }

    /** Nước trong bảng băm có phải nước giả hợp lệ ở thế hiện tại không (tránh va chạm khóa). */
    private fun legalTT(m: Int): Boolean {
        val from = m and 127
        if (from >= 90 || ((m shr 7) and 127) >= 90) return false
        val p = pc[from]
        if (p == 0 || (p shr 3) != stm) return false
        val buf = bufs[MAXPLY + 1]; val n = gen(buf, false)
        for (i in 0 until n) if (buf[i] == m) return true
        return false
    }

    private fun updateQuietBonus(m: Int, ply: Int, b: Int) {
        val from = m and 127; val to = (m shr 7) and 127
        upd(hist[stm], from * 90 + to, b)
        val pi = pidx(pc[from]) * 90 + to
        val c1 = contIdx(ply, 1); if (c1 >= 0) { upd(cont[c1], pi, b); if (b > 0) counter[c1] = m }
        val c2 = contIdx(ply, 2); if (c2 >= 0) upd(cont[c2], pi, b)
        if (b > 0 && killers[ply][0] != m) { killers[ply][1] = killers[ply][0]; killers[ply][0] = m }
    }

    /**
     * Tìm kiếm có giới hạn (thời gian / số nút / độ sâu) trên các nước gốc [allowed] (null = mọi nước).
     * [delta] > 0: trả thêm các nước không kém nước tốt nhất quá [delta] (chọn nhiễu cho cấp thấp).
     */
    fun search(lim: SearchLimit, allowed: IntArray?, delta: Int): RootResult {
        val start = System.nanoTime()
        deadline = if (lim.timeMs > 0) start + lim.timeMs * 1_000_000 else Long.MAX_VALUE
        // Ngân sách mỗi nước là cố định (không dồn sang nước sau) nên dùng gần hết; chỉ dừng sớm khi nước tốt nhất đã rất ổn định.
        val soft = if (lim.timeMs > 0) lim.timeMs * 1_000_000.0 else Double.MAX_VALUE
        val nl = if (lim.nodes > 0) lim.nodes else Long.MAX_VALUE
        val maxDepth = if (lim.depth > 0) min(lim.depth, MAXPLY - 10) else MAXPLY - 10
        abort = false; nodes = 0
        if (helperId == 0) tt.gen++
        for (i in 0 until MAXPLY + 2) { killers[i][0] = 0; killers[i][1] = 0; excl[i] = 0; curMove[i] = 0 }
        for (c in 0..1) for (i in 0 until 8100) hist[c][i] = hist[c][i] / 2
        var root = legalMoves()
        if (allowed != null) { val f = root.filter { it in allowed }.toIntArray(); if (f.isNotEmpty()) root = f }
        if (root.isEmpty()) return RootResult(-1, 0, 0, 0)
        if (root.size == 1 && delta == 0) return RootResult(root[0], 0, 0, 0)
        val rs = IntArray(root.size) { -INF }
        val ti = (hash and ttMask.toLong()).toInt()
        val td = ttData[ti]
        val ttm = if ((ttKey[ti] xor td) == hash) (td and 0xFFFFF).toInt() else 0
        for (i in root.indices) { val m = root[i]; rs[i] = (if (m == ttm) 100000 else 0) + SEEV[pc[(m shr 7) and 127] and 7] }
        sortRoot(root, rs)
        var best = root[0]; var bestScore = 0; var completed = 0
        var stable = 0
        sEval[0] = if (inCheck()) NONE else evaluate()
        for (depth in 1..maxDepth) {
            if (helperId > 0 && depth > 2 && (depth + helperId) % 3 == 0) continue
            if (helperId > 0 && tt.stop) break
            rootDepth = depth
            nodeLimit = if (depth == 1) Long.MAX_VALUE else nl
            var window = if (depth >= 4) 20 else INF
            var alpha = if (depth >= 4) max(-INF, bestScore - window) else -INF
            var beta = if (depth >= 4) min(INF, bestScore + window) else INF
            var iterBest = -1; var iterScore = -INF
            while (true) {
                var a = alpha
                iterBest = -1; iterScore = -INF
                for (k in root.indices) {
                    val m = root[k]
                    val wasQuiet = isQuiet(m)
                    make(m)
                    curMove[0] = m; curPc[0] = pc[(m shr 7) and 127]
                    val gives = inCheck()
                    val nd = depth - 1 + (if (gives) 1 else 0)
                    var s: Int
                    if (k == 0) s = -search(nd, -beta, -a, 1, false)
                    else {
                        var r = 0
                        if (depth >= 4 && k >= 3 && wasQuiet && !gives) r = min(nd - 1, LMR[min(depth, 63)][min(k + 1, 63)] / 2).coerceAtLeast(0)
                        s = -search(nd - r, -a - 1, -a, 1, true)
                        if (s > a && r > 0 && !abort) s = -search(nd, -a - 1, -a, 1, false)
                        if (s > a && s < beta && !abort) s = -search(nd, -beta, -a, 1, false)
                    }
                    unmake()
                    if (abort) break
                    rs[k] = if (s > a || k == 0) s else -INF + k
                    if (s > iterScore) { iterScore = s; iterBest = m }
                    if (s > a) a = s
                    if (a >= beta) break
                }
                if (abort) break
                if (iterScore <= alpha && alpha > -INF) { alpha = max(-INF, alpha - window); window += window / 2 + 10; continue }
                if (iterScore >= beta && beta < INF) { beta = min(INF, beta + window); window += window / 2 + 10; continue }
                break
            }
            if (abort) {
                if (iterBest >= 0 && completed > 0 && iterScore > bestScore - 30) best = iterBest
                break
            }
            if (iterBest == best) stable++ else stable = 0
            best = iterBest; bestScore = iterScore; completed = depth
            for (i in root.indices) if (root[i] == best) rs[i] = INF
            sortRoot(root, rs)
            if ((bestScore > MATE - 200 || bestScore < -MATE + 200) && depth >= 8) break
            val elapsed = (System.nanoTime() - start).toDouble()
            val factor = when { stable >= 8 -> 0.5; stable >= 4 -> 0.75; else -> 0.92 }
            if (elapsed > soft * factor) break
        }
        lastDepth = completed; lastNodes = nodes
        var near = IntArray(0)
        if (delta > 0 && completed > 0) {
            abort = false; nodeLimit = Long.MAX_VALUE; deadline = Long.MAX_VALUE
            val thr = bestScore - delta
            val out = ArrayList<Int>()
            val d = max(1, min(completed - 1, 6))
            for (m in root) {
                if (m == best) continue
                make(m); curMove[0] = m; curPc[0] = pc[(m shr 7) and 127]
                val s = -search(d - 1, -thr, -thr + 1, 1, false)
                unmake()
                if (s >= thr) out.add(m)
            }
            near = out.toIntArray()
        }
        return RootResult(best, bestScore, completed, nodes, near)
    }

    private fun sortRoot(root: IntArray, rs: IntArray) {
        for (i in 1 until root.size) {
            val m = root[i]; val s = rs[i]; var j = i - 1
            while (j >= 0 && rs[j] < s) { root[j + 1] = root[j]; rs[j + 1] = rs[j]; j-- }
            root[j + 1] = m; rs[j + 1] = s
        }
    }

    /** Lazy SMP như [ChessEngine.searchSmp]; chỉ dùng cho tìm kiếm theo thời gian. */
    fun searchSmp(board: IntArray, redToMove: Boolean, moves: List<Int>, lim: SearchLimit, allowed: IntArray?, delta: Int, threads: Int): RootResult {
        load(board, redToMove, moves)
        if (threads <= 1 || lim.nodes > 0 || lim.depth > 0 || lim.timeMs <= 0) return search(lim, allowed, delta)
        while (helpers.size < threads - 1) helpers.add(XiangqiEngine(tt).also { it.helperId = helpers.size + 1 })
        for (h in helpers) { h.wMg = wMg; h.wEg = wEg }
        tt.stop = false
        val ths = (0 until threads - 1).map { k ->
            val h = helpers[k]
            h.load(board, redToMove, moves)
            Thread {
                try { h.search(SearchLimit(timeMs = lim.timeMs + 2000), allowed, 0) } catch (_: Throwable) { }
            }.apply { isDaemon = true; start() }
        }
        try {
            return search(lim, allowed, delta)
        } finally {
            tt.stop = true
            for (t in ths) t.join()
        }
    }

    fun clear() {
        tt.clear(); evKey.fill(0)
        for (h in hist) h.fill(0)
        for (c in cont) c.fill(0)
        for (a in capHist) for (b in a) b.fill(0)
        counter.fill(0)
    }

    fun quiescence(): Int { abort = false; nodeLimit = Long.MAX_VALUE; deadline = Long.MAX_VALUE; return qsearch(-INF, INF, 0) }
    fun makeMove(m: Int) = make(m)
}
