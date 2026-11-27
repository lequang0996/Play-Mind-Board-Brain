package com.fws.playmind.core

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Bảng hình cờ caro (theo luật dự án: 5 quân trở lên thắng, trừ khi cả hai đầu dãy bị quân đối phương chặn).
 *
 * Mỗi ô trống, mỗi hướng có một "khóa" = màu 10 ô lân cận (5 ô mỗi phía) mã cơ số 3 (0 trống, 1 quân X, 2 quân O).
 * [PAT] cho biết nếu quân của bên p đặt vào ô giữa thì đường đó thành hình gì (năm, bốn hở, bốn, ba hở, ba, hai hở, hai).
 * Hình được tính bằng định nghĩa đệ quy (bốn = một ô thành năm; bốn hở = hai ô thành năm; ba hở = một ô thành bốn hở...),
 * nên tự đúng với luật chặn hai đầu và với hình gãy (X_XX, XX_XX...). Ô ngoài cửa sổ 11 ô coi là chưa bị chặn (chỉ ảnh hưởng dãy từ 6 quân trở lên).
 */
internal object CaroPat {
    const val DEAD = 0; const val B2 = 1; const val F2 = 2; const val B3 = 3; const val F3 = 4; const val B4 = 5; const val F4 = 6; const val F5 = 7
    // lớp của ô (gộp 4 hướng)
    const val C0 = 0; const val C1 = 1; const val CF3 = 2; const val C33 = 3; const val CB4 = 4; const val C43 = 5; const val C4 = 6; const val C5 = 7

    /** Tầm nhìn mỗi phía (5 ô: đủ để biết đầu dãy năm có bị chặn không). */
    const val R = 5
    const val L = 2 * R + 1
    val POW3 = IntArray(2 * R).also { it[0] = 1; for (i in 1 until 2 * R) it[i] = it[i - 1] * 3 }
    const val NKEY = 59049
    /** PAT[p - 1][key] */
    val PAT = Array(2) { IntArray(NKEY) }
    /** Lớp ô và điểm ô theo chỉ số gộp code0 | code1 << 3 | code2 << 6 | code3 << 9. */
    val CLS = IntArray(4096)
    val SCORE = IntArray(4096)
    /** Điểm thứ tự nước (tấn công) theo chỉ số gộp. */
    val ORDER = IntArray(4096)

    private val memo = IntArray(NKEY) { -1 }

    private fun five(a: IntArray, i: Int, mustHave: Int): Boolean {
        var l = i; while (l > 0 && a[l - 1] == 1) l--
        var r = i; while (r < L - 1 && a[r + 1] == 1) r++
        if (r - l + 1 < 5 || mustHave < l || mustHave > r) return false
        val bl = l > 0 && a[l - 1] == 2
        val br = r < L - 1 && a[r + 1] == 2
        return !(bl && br)
    }

    private fun keyOf(a: IntArray): Int {
        var k = 0
        for (j in 0 until L) if (j != R) k += a[j] * POW3[if (j < R) j else j - 1]
        return k
    }

    private fun cls(a: IntArray): Int {
        val k = keyOf(a)
        if (memo[k] >= 0) return memo[k]
        val res: Int
        if (five(a, R, R)) res = F5
        else {
            var n5 = 0
            for (e in 0 until L) if (a[e] == 0) { a[e] = 1; if (five(a, e, R)) n5++; a[e] = 0 }
            res = if (n5 >= 2) F4 else if (n5 == 1) B4 else {
                var best = DEAD
                for (e in 0 until L) if (a[e] == 0) {
                    a[e] = 1; val t = cls(a); a[e] = 0
                    val lv = when (t) { F4 -> F3; B4 -> B3; F3 -> F2; B3 -> B2; else -> DEAD }
                    if (lv > best) best = lv
                }
                best
            }
        }
        memo[k] = res
        return res
    }

    // giá trị từng hình (điểm đánh giá) và điểm thưởng khi gộp
    private val PV = intArrayOf(0, 2, 10, 12, 50, 60, 400, 2000)

    init {
        val a = IntArray(L)
        for (k in 0 until NKEY) {
            var t = k
            for (j in 0 until L) {
                if (j == R) { a[j] = 1; continue }
                a[j] = t % 3; t /= 3
            }
            PAT[0][k] = cls(a)
            // góc nhìn của O: đổi 1 <-> 2
            var sk = 0; var u = k
            for (pos in 0 until 2 * R) { val d = u % 3; u /= 3; sk += (if (d == 0) 0 else 3 - d) * POW3[pos] }
            PAT[1][sk] = PAT[0][k]
        }
        val n = IntArray(8)
        for (idx in 0 until 4096) {
            n.fill(0)
            var s = 0
            for (d in 0 until 4) { val c = (idx shr (3 * d)) and 7; n[c]++; s += PV[c] }
            val cl = when {
                n[F5] > 0 -> C5
                n[F4] > 0 || n[B4] >= 2 -> C4
                n[B4] > 0 && n[F3] > 0 -> C43
                n[B4] > 0 -> CB4
                n[F3] >= 2 -> C33
                n[F3] > 0 -> CF3
                n[F2] + n[B3] > 0 -> C1
                else -> C0
            }
            val bonus = when (cl) { C4 -> if (n[F4] > 0) 0 else 300; C43 -> 250; C33 -> 150; else -> 0 }
            CLS[idx] = cl; SCORE[idx] = s + bonus
            ORDER[idx] = when (cl) {
                C5 -> 1_000_000; C4 -> 200_000; C43 -> 50_000; CB4 -> 4_000 + s; C33 -> 20_000; CF3 -> 2_000 + s
                else -> s
            }
        }
    }
}

/**
 * Engine caro v3 (BOT_GUIDE mục 7). Bàn trong 32×32 ô (lề 5 ô để tra hình), mã ô = y * 42 + x.
 * Bên 1 = X (đi trước), 2 = O. Đánh giá theo hình cập nhật tăng dần, VCF đúng luật chặn hai đầu, phòng VCF, PVS lặp sâu
 * với bảng băm, killer/history, cắt nước theo đe dọa (đối phương có ba hở thì chỉ xét nước chặn và nước đánh bốn).
 * [GomokuGame] cắt cửa sổ quanh các quân rồi gọi [levelSearch].
 */
/** Tham số tìm kiếm của [CaroEngine] (để thử A/B: "beam6=16+lmr0"). */
object CP {
    val NAMES = arrayOf("oppw", "tempo", "defo", "qext", "nearwin", "ivcf", "beam6", "beam3", "beam1", "lmr", "rootn", "stop")
    val DEF = intArrayOf(10, 20, 14, 0, 1, 80, 14, 11, 8, 3, 30, 70)
    const val OPPW = 0; const val TEMPO = 1; const val DEFO = 2; const val QEXT = 3; const val NEARWIN = 4; const val IVCF = 5
    const val BEAM6 = 6; const val BEAM3 = 7; const val BEAM1 = 8; const val LMR = 9; const val ROOTN = 10; const val STOP = 11
    fun parse(spec: String): IntArray {
        val p = DEF.copyOf()
        for (item in spec.split('+').filter { it.isNotEmpty() }) {
            val (name, v) = if ('=' in item) item.substringBefore('=') to item.substringAfter('=')
                else item.indexOfFirst { it == '-' || it.isDigit() }.let { item.substring(0, it) to item.substring(it) }
            val k = NAMES.indexOf(name); require(k >= 0) { "tham số lạ: $item" }
            p[k] = v.toInt()
        }
        return p
    }
}

class CaroEngine {
    var prm: IntArray = CP.DEF.copyOf()
    companion object {
        const val P = 32
        const val W = P + 2 * CaroPat.R
        const val N = W * W
        private val OFF = intArrayOf(1, W, W + 1, W - 1)
        const val WIN = 1_000_000
        private const val MAXPLY = 64
        private val zr = Random(0x5CA7)
        private val Z = Array(3) { LongArray(N) { zr.nextLong() } }
        private val Z_TURN = zr.nextLong()
        private const val TT_BITS = 19
        private const val TT_SIZE = 1 shl TT_BITS
        private val lock = Any()
        @Volatile var lastDepth = 0; private set
        @Volatile var lastNodes = 0L; private set
        @Volatile var lastMode = ""; private set
        @Volatile var lastScore = 0; private set
        private const val B = CaroPat.R
        val INNER = BooleanArray(N).also { for (y in B until B + P) for (x in B until B + P) it[y * W + x] = true }
        fun cell(x: Int, y: Int) = (y + B) * W + (x + B)
        fun cx(c: Int) = c % W - B
        fun cy(c: Int) = c / W - B
        init { CaroPat.CLS.size }
    }

    private val ttKey = LongArray(TT_SIZE)
    private val ttMove = IntArray(TT_SIZE)
    private val ttScore = IntArray(TT_SIZE)
    private val ttInfo = IntArray(TT_SIZE)
    private val board = IntArray(N)
    private val key = IntArray(N * 4)
    /** comb[p][c]: chỉ số gộp 4 hình của bên p (p = 1, 2) tại ô trống c. */
    private val comb = Array(3) { IntArray(N) }
    private val sum = IntArray(3)
    private val cnt = Array(3) { IntArray(8) }
    // danh sách ô "nóng" (lớp >= CB4) mỗi bên
    private val hot = Array(3) { IntArray(N) }
    private val hotPos = Array(3) { IntArray(N) { -1 } }
    private val hotLen = IntArray(3)
    // ô trống gần quân (trong vòng 2 ô)
    private val nb = IntArray(N)
    private val cand = IntArray(N)
    private val candPos = IntArray(N) { -1 }
    private var candLen = 0
    private var zh = 0L
    var stones = 0; private set
    var toMove = 1; private set

    private val hist = Array(3) { IntArray(N) }
    private val killer = Array(MAXPLY + 2) { IntArray(2) { -1 } }
    private val mbuf = Array(MAXPLY + 2) { IntArray(N) }
    private val sbuf = Array(MAXPLY + 2) { IntArray(N) }

    private var nodes = 0L
    private var nodeLimit = Long.MAX_VALUE
    private var deadline = Long.MAX_VALUE
    private var abort = false
    private var vNodes = 0; private var vLimit = 0; private var vDeadline = Long.MAX_VALUE
    private var vMove = -1
    private var qLimit = MAXPLY - 4
    /** Tổng số nút VCF (chẩn đoán). */
    var vTotal = 0L

    init { for (c in 0 until N) if (INNER[c]) { comb[1][c] = 0; comb[2][c] = 0 } }

    // ───────────────────────── cập nhật bàn ─────────────────────────
    private fun hotSet(p: Int, c: Int, on: Boolean) {
        val pos = hotPos[p]
        if (on) { if (pos[c] < 0) { pos[c] = hotLen[p]; hot[p][hotLen[p]++] = c } }
        else if (pos[c] >= 0) { val i = pos[c]; val last = hot[p][--hotLen[p]]; hot[p][i] = last; pos[last] = i; pos[c] = -1 }
    }

    private fun addCell(c: Int) {
        for (p in 1..2) {
            var idx = 0
            val pat = CaroPat.PAT[p - 1]
            for (d in 0 until 4) idx = idx or (pat[key[c * 4 + d]] shl (3 * d))
            comb[p][c] = idx
            sum[p] += CaroPat.SCORE[idx]
            val cl = CaroPat.CLS[idx]; cnt[p][cl]++
            if (cl >= CaroPat.CB4) hotSet(p, c, true)
        }
    }

    private fun dropCell(c: Int) {
        for (p in 1..2) {
            val idx = comb[p][c]
            sum[p] -= CaroPat.SCORE[idx]
            val cl = CaroPat.CLS[idx]; cnt[p][cl]--
            if (cl >= CaroPat.CB4) hotSet(p, c, false)
        }
    }

    /** Ô trống [t] đổi khóa hướng [d] thành [nk]: chỉ cập nhật hình của hướng đó. */
    private fun updDir(t: Int, d: Int, nk: Int) {
        key[t * 4 + d] = nk
        val sh = 3 * d
        for (p in 1..2) {
            val old = comb[p][t]
            val nw = (old and (7 shl sh).inv()) or (CaroPat.PAT[p - 1][nk] shl sh)
            if (nw == old) continue
            comb[p][t] = nw
            sum[p] += CaroPat.SCORE[nw] - CaroPat.SCORE[old]
            val oc = CaroPat.CLS[old]; val nc = CaroPat.CLS[nw]
            if (oc != nc) {
                cnt[p][oc]--; cnt[p][nc]++
                if ((oc >= CaroPat.CB4) != (nc >= CaroPat.CB4)) hotSet(p, t, nc >= CaroPat.CB4)
            }
        }
    }

    private fun candSet(c: Int, on: Boolean) {
        if (on) { if (candPos[c] < 0) { candPos[c] = candLen; cand[candLen++] = c } }
        else if (candPos[c] >= 0) { val i = candPos[c]; val last = cand[--candLen]; cand[i] = last; candPos[last] = i; candPos[c] = -1 }
    }

    private fun nbUpdate(c: Int, delta: Int) {
        for (dy in -2..2) for (dx in -2..2) {
            val t = c + dy * W + dx
            if (!INNER[t]) continue
            nb[t] += delta
            if (board[t] == 0) candSet(t, nb[t] > 0)
        }
    }

    private fun put(c: Int, p: Int) {
        if (board[c] == 0 && INNER[c]) dropCell(c)
        candSet(c, false)
        board[c] = p; zh = zh xor Z[p][c]; stones++
        for (d in 0 until 4) {
            val o = OFF[d]
            for (k in -B..B) {
                if (k == 0) continue
                val t = c - k * o              // c = t + k*o
                val pos = if (k < 0) k + B else k + B - 1
                val nk = key[t * 4 + d] + p * CaroPat.POW3[pos]
                if (board[t] == 0 && INNER[t]) updDir(t, d, nk) else key[t * 4 + d] = nk
            }
        }
        nbUpdate(c, 1)
        toMove = 3 - p
    }

    private fun take(c: Int) {
        val p = board[c]
        for (d in 0 until 4) {
            val o = OFF[d]
            for (k in -B..B) {
                if (k == 0) continue
                val t = c - k * o
                val pos = if (k < 0) k + B else k + B - 1
                val nk = key[t * 4 + d] - p * CaroPat.POW3[pos]
                if (board[t] == 0 && INNER[t]) updDir(t, d, nk) else key[t * 4 + d] = nk
            }
        }
        board[c] = 0; zh = zh xor Z[p][c]; stones--
        addCell(c)
        nbUpdate(c, -1)
        if (nb[c] > 0) candSet(c, true)
        toMove = p
    }

    /** Nạp thế: owner[y * 32 + x] (0 trống, 1, 2) trong cửa sổ 32×32, [toMove] bên đi. */
    fun setPosition(owner: IntArray, toMove: Int) {
        board.fill(0); key.fill(0); sum.fill(0); for (a in cnt) a.fill(0)
        for (p in 1..2) { hotPos[p].fill(-1); hotLen[p] = 0 }
        nb.fill(0); candPos.fill(-1); candLen = 0; zh = 0; stones = 0
        for (c in 0 until N) if (INNER[c]) addCell(c)
        for (y in 0 until P) for (x in 0 until P) { val v = owner[y * P + x]; if (v != 0) put(cell(x, y), v) }
        this.toMove = toMove
    }

    private fun clsOf(p: Int, c: Int) = CaroPat.CLS[comb[p][c]]
    /** Bên [p] có nước tạo bốn (bốn thường, bốn hở hoặc năm) không. */
    private fun hasFour(p: Int) = cnt[p][CaroPat.CB4] + cnt[p][CaroPat.C43] + cnt[p][CaroPat.C4] + cnt[p][CaroPat.C5] > 0

    /** Kiểm tra dữ liệu tăng dần khớp với tính lại từ đầu (dùng trong kiểm thử). */
    internal fun consistent(): Boolean {
        val s = IntArray(3); val ct = Array(3) { IntArray(8) }
        for (c in 0 until N) {
            if (!INNER[c]) continue
            for (d in 0 until 4) {
                var k = 0
                for (j in -B..B) if (j != 0) k += board[c + j * OFF[d]] * CaroPat.POW3[if (j < 0) j + B else j + B - 1]
                if (k != key[c * 4 + d]) return false
            }
            if (board[c] != 0) { if (candPos[c] >= 0) return false; continue }
            for (p in 1..2) {
                var idx = 0
                for (d in 0 until 4) idx = idx or (CaroPat.PAT[p - 1][key[c * 4 + d]] shl (3 * d))
                if (idx != comb[p][c]) return false
                s[p] += CaroPat.SCORE[idx]; val cl = CaroPat.CLS[idx]; ct[p][cl]++
                if ((cl >= CaroPat.CB4) != (hotPos[p][c] >= 0)) return false
            }
            var near = 0
            for (dy in -2..2) for (dx in -2..2) { val t = c + dy * W + dx; if (t in 0 until N && board[t] != 0 && INNER[t]) near++ }
            if ((near > 0) != (candPos[c] >= 0)) return false
        }
        for (p in 1..2) { if (s[p] != sum[p]) return false; for (i in 0 until 8) if (ct[p][i] != cnt[p][i]) return false }
        return true
    }

    /** Thử đặt/gỡ ngẫu nhiên và kiểm tra [consistent] (kiểm thử). */
    internal fun fuzz(seed: Int, steps: Int): Boolean {
        val r = Random(seed)
        setPosition(IntArray(P * P), 1)
        val st = ArrayList<Int>()
        repeat(steps) {
            if (st.isNotEmpty() && r.nextInt(3) == 0) take(st.removeAt(st.size - 1))
            else {
                val c = cell(8 + r.nextInt(16), 8 + r.nextInt(16))
                if (board[c] == 0) { put(c, 1 + r.nextInt(2)); st.add(c) }
            }
            if (!consistent()) return false
        }
        return true
    }

    private fun findHot(p: Int, cl: Int): Int {
        for (i in 0 until hotLen[p]) { val c = hot[p][i]; if (clsOf(p, c) == cl) return c }
        return -1
    }

    private fun evaluate(p: Int): Int = sum[p] - sum[3 - p] * prm[CP.OPPW] / 16 + prm[CP.TEMPO]

    // ───────────────────────── VCF ─────────────────────────
    /** Bên [p] (đang đi) có thắng bằng chuỗi bốn liên tục trong [depth] nước bốn không. Kết quả true là chắc chắn. */
    private fun vcf(p: Int, depth: Int, root: Boolean): Boolean {
        vTotal++
        if (++vNodes > vLimit || ((vNodes and 63) == 0 && System.nanoTime() > vDeadline)) return false
        val o = 3 - p
        if (cnt[p][CaroPat.C5] > 0) { if (root) vMove = findHot(p, CaroPat.C5); return true }
        if (cnt[o][CaroPat.C5] > 0) return false
        if (cnt[p][CaroPat.C4] > 0 && !hasFour(o)) { if (root) vMove = findHot(p, CaroPat.C4); return true }
        if (depth <= 0) return false
        // các nước đánh bốn (lớp CB4, C43); sao chép vì danh sách thay đổi khi đặt quân
        var n = 0
        val buf = vbuf[depth]
        for (i in 0 until hotLen[p]) { val c = hot[p][i]; val cl = clsOf(p, c); if (cl == CaroPat.CB4 || cl == CaroPat.C43 || cl == CaroPat.C4) buf[n++] = c }
        // C43 trước
        var a = 0
        for (i in 0 until n) if (clsOf(p, buf[i]) == CaroPat.C43) { val t = buf[a]; buf[a] = buf[i]; buf[i] = t; a++ }
        for (i in 0 until n) {
            val c = buf[i]
            put(c, p)
            var ok = false
            if (cnt[p][CaroPat.C5] > 0 && cnt[o][CaroPat.C5] == 0) {
                // mọi cách đỡ (chiếm ô thành năm, hoặc chặn đầu kia cho dãy năm bị chặn hai đầu) đều phải thua
                val db = dbuf[depth]; val nd = defenses(p, db)
                ok = true
                for (j in 0 until nd) {
                    put(db[j], o)
                    val r = vcf(p, depth - 1, false)
                    take(db[j])
                    if (!r) { ok = false; break }
                }
            }
            take(c)
            if (ok) { if (root) vMove = c; return true }
            if (vNodes > vLimit) return false
        }
        return false
    }
    private val vbuf = Array(40) { IntArray(256) }
    private val dbuf = Array(40) { IntArray(128) }
    private val sdef = Array(MAXPLY + 2) { IntArray(128) }
    private val mark = IntArray(N)
    private var stamp = 0
    private val fives = IntArray(16)
    private val dcand = IntArray(256)

    /**
     * Các nước của bên phòng thủ làm mất MỌI ô thành năm của bên [a]: chiếm ô thành năm, hoặc (luật chặn hai đầu)
     * đặt quân ở đầu kia để dãy năm bị chặn hai đầu. Thử từng ô trên các đường qua ô thành năm. Trả số nước, ghi vào [out].
     */
    private fun defenses(a: Int, out: IntArray): Int {
        val d0 = 3 - a
        var nf = 0
        for (i in 0 until hotLen[a]) { val c = hot[a][i]; if (clsOf(a, c) == CaroPat.C5 && nf < fives.size) fives[nf++] = c }
        if (++stamp == Int.MAX_VALUE) { mark.fill(0); stamp = 1 }
        var nc = 0
        fun add(t: Int) { if (INNER[t] && board[t] == 0 && mark[t] != stamp && nc < dcand.size) { mark[t] = stamp; dcand[nc++] = t } }
        for (i in 0 until nf) {
            val f = fives[i]
            add(f)
            for (d in 0 until 4) {
                if (((comb[a][f] shr (3 * d)) and 7) != CaroPat.F5) continue
                for (k in 1..B) { add(f + k * OFF[d]); add(f - k * OFF[d]) }
            }
        }
        var n = 0
        for (i in 0 until nc) {
            val t = dcand[i]
            put(t, d0)
            val ok = cnt[a][CaroPat.C5] == 0
            take(t)
            if (ok && n < out.size) out[n++] = t
        }
        return n
    }

    // ───────────────────────── tìm kiếm ─────────────────────────
    private fun genMoves(ply: Int, p: Int, ttm: Int, sort: Boolean = true): Int {
        val o = 3 - p
        val buf = mbuf[ply]; val sc = sbuf[ply]
        var n = 0
        if (cnt[o][CaroPat.C4] > 0) {
            // đối phương dọa bốn hở / bốn-bốn: chỉ nước chặn (ô nóng của đối phương) và nước đánh bốn của mình
            for (i in 0 until hotLen[o]) buf[n++] = hot[o][i]
            for (i in 0 until hotLen[p]) { val c = hot[p][i]; if (hotPos[o][c] < 0) buf[n++] = c }
        } else {
            for (i in 0 until candLen) buf[n++] = cand[i]
        }
        val k0 = killer[ply][0]; val k1 = killer[ply][1]
        val hp = hist[p]
        for (i in 0 until n) {
            val c = buf[i]
            var s = CaroPat.ORDER[comb[p][c]] + CaroPat.ORDER[comb[o][c]] * prm[CP.DEFO] / 16 + hp[c] / 64
            if (c == ttm) s += 10_000_000 else if (c == k0) s += 30_000 else if (c == k1) s += 20_000
            sc[i] = s
        }
        if (!sort) return n
        // sắp xếp chèn giảm dần
        for (i in 1 until n) {
            val c = buf[i]; val s = sc[i]; var j = i - 1
            while (j >= 0 && sc[j] < s) { buf[j + 1] = buf[j]; sc[j + 1] = sc[j]; j-- }
            buf[j + 1] = c; sc[j + 1] = s
        }
        return n
    }

    private fun search(depthIn: Int, alphaIn: Int, beta: Int, ply: Int): Int {
        var depth = depthIn
        var alpha = alphaIn
        if ((++nodes and 1023L) == 0L && (nodes >= nodeLimit || System.nanoTime() > deadline)) abort = true
        if (nodes >= nodeLimit) abort = true
        if (abort) return 0
        val p = toMove; val o = 3 - p
        if (cnt[p][CaroPat.C5] > 0) return WIN - ply
        if (ply >= MAXPLY - 2) return evaluate(p)
        if (cnt[o][CaroPat.C5] > 0) {
            // nước bắt buộc: đỡ năm (không giảm độ sâu)
            val db = sdef[ply]; val nd = defenses(o, db)
            if (nd == 0) return -(WIN - ply - 1)
            var best = -WIN * 2
            for (j in 0 until nd) {
                put(db[j], p)
                val s = -search(depth, -beta, -alpha, ply + 1)
                take(db[j])
                if (abort) return 0
                if (s > best) best = s
                if (s > alpha) alpha = s
                if (alpha >= beta) break
            }
            return best
        }
        if (cnt[p][CaroPat.C4] > 0 && !hasFour(o)) return WIN - ply - 2
        if (depth <= 0) {
            // đi tiếp chuỗi bốn ngắn để không đánh giá sai thế đang có đòn
            if (cnt[p][CaroPat.CB4] + cnt[p][CaroPat.C43] + cnt[p][CaroPat.C4] > 0) {
                vNodes = 0; vLimit = 40; vDeadline = deadline
                if (vcf(p, 6, false)) return WIN - ply - 4
            }
            // đối phương đang có ba hở (dọa bốn hở): phải đỡ, tìm tiếp trên các nước chặn (tìm kiếm tĩnh theo đe dọa)
            if (cnt[o][CaroPat.C4] > 0 && ply < qLimit && prm[CP.QEXT] > 0) depth = 1
            else {
                // mình có nước 4-3 / 3-3 mà đối phương không có bốn để phản công: gần như thắng
                if (prm[CP.NEARWIN] > 0 && !hasFour(o)) {
                    if (cnt[p][CaroPat.C43] > 0) return WIN / 2 - ply
                    if (cnt[p][CaroPat.C33] > 0 && cnt[o][CaroPat.C33] + cnt[o][CaroPat.CF3] == 0) return WIN / 4 - ply
                }
                return evaluate(p)
            }
        }
        val tkey = zh xor (if (p == 2) Z_TURN else 0L)
        val slot = (tkey and (TT_SIZE - 1).toLong()).toInt()
        var ttm = -1
        if (ttKey[slot] == tkey) {
            ttm = ttMove[slot]
            val td = ttInfo[slot] shr 2; val fl = ttInfo[slot] and 3
            if (td >= depth && beta - alpha == 1) {
                val s = ttScore[slot]
                if (fl == 0 || (fl == 1 && s >= beta) || (fl == 2 && s <= alpha)) return s
            }
        }
        if (depth >= 2 && prm[CP.IVCF] > 0 && cnt[p][CaroPat.CB4] + cnt[p][CaroPat.C43] + cnt[p][CaroPat.C4] > 0) {
            vNodes = 0; vLimit = prm[CP.IVCF]; vDeadline = deadline
            if (vcf(p, 8, false)) return WIN - ply - 4
        }
        val n = genMoves(ply, p, ttm, false)
        if (n == 0) return 0
        val buf = mbuf[ply]; val sc = sbuf[ply]
        // thế phòng thủ (đối phương dọa bốn hở): xét mọi nước chặn; thế thường: xét [limit] nước tốt nhất,
        // nhưng nếu mọi nước đã xét đều thua thì xét tiếp các nước còn lại (tránh kết luận "thua chắc" sai do cắt nước)
        val restricted = cnt[o][CaroPat.C4] > 0
        val limit = if (restricted) n else min(n, if (depth >= 6) prm[CP.BEAM6] else if (depth >= 3) prm[CP.BEAM3] else prm[CP.BEAM1])
        var best = -WIN * 2; var bestMove = -1
        val origAlpha = alpha
        for (k in 0 until n) {
            if (k >= limit && best > -WIN + 1000) break
            // chọn dần nước tốt nhất còn lại (chỉ cần [limit] nước đầu)
            var bi = k
            for (i in k + 1 until n) if (sc[i] > sc[bi]) bi = i
            if (bi != k) { val tc = buf[k]; buf[k] = buf[bi]; buf[bi] = tc; val ts = sc[k]; sc[k] = sc[bi]; sc[bi] = ts }
            val c = buf[k]
            val quiet = CaroPat.CLS[comb[p][c]] < CaroPat.CF3 && CaroPat.CLS[comb[o][c]] < CaroPat.CF3
            put(c, p)
            var s: Int
            if (k == 0) s = -search(depth - 1, -beta, -alpha, ply + 1)
            else {
                val r = if (prm[CP.LMR] > 0 && depth >= 3 && k >= prm[CP.LMR] && quiet) (if (k >= 2 * prm[CP.LMR]) 2 else 1) else 0
                s = -search(depth - 1 - r, -alpha - 1, -alpha, ply + 1)
                if (s > alpha && r > 0) s = -search(depth - 1, -alpha - 1, -alpha, ply + 1)
                if (s > alpha && s < beta) s = -search(depth - 1, -beta, -alpha, ply + 1)
            }
            take(c)
            if (abort) return 0
            if (s > best) {
                best = s; bestMove = c
                if (s > alpha) {
                    alpha = s
                    if (s >= beta) {
                        if (quiet && killer[ply][0] != c) { killer[ply][1] = killer[ply][0]; killer[ply][0] = c }
                        hist[p][c] += depth * depth
                        if (hist[p][c] > 1_000_000) for (q in 1..2) for (i in 0 until N) hist[q][i] /= 2
                        break
                    }
                }
            }
        }
        val flag = if (best >= beta) 1 else if (best > origAlpha) 0 else 2
        ttKey[slot] = tkey; ttMove[slot] = bestMove; ttScore[slot] = best; ttInfo[slot] = (depth shl 2) or flag
        return best
    }

    /**
     * Tìm nước theo cấp (BOT_GUIDE mục 7.4): thắng ngay / chặn năm, VCF của mình tới độ sâu [vcfDepth] (0 = tắt),
     * phòng VCF của đối phương nếu [defend], rồi PVS lặp sâu trong giới hạn [lim]. Trả nước là mã ô trong cửa sổ (y*32+x).
     */
    private fun root(lim: SearchLimit, vcfDepth: Int, defend: Boolean, delta: Int): RootResult {
        val start = System.nanoTime()
        val timeMs = lim.timeMs
        deadline = if (timeMs > 0) start + timeMs * 1_000_000 else Long.MAX_VALUE
        fun frac(f: Double) = if (timeMs > 0) start + (timeMs * 1_000_000 * f).toLong() else Long.MAX_VALUE
        val nl = if (lim.nodes > 0) lim.nodes else Long.MAX_VALUE
        val maxDepth = if (lim.depth > 0) lim.depth else 40
        abort = false; nodes = 0; nodeLimit = Long.MAX_VALUE
        for (k in killer) { k[0] = -1; k[1] = -1 }
        for (q in 1..2) for (i in 0 until N) hist[q][i] /= 4
        val me = toMove; val op = 3 - me
        fun out(c: Int) = cy(c) * P + cx(c)
        fun res(c: Int, s: Int, d: Int = 0, near: IntArray = IntArray(0)): RootResult { lastScore = s; return RootResult(out(c), s, d, nodes, near) }
        if (cnt[me][CaroPat.C5] > 0) { lastMode = "win"; return res(findHot(me, CaroPat.C5), WIN) }
        var forced: IntArray? = null
        if (cnt[op][CaroPat.C5] > 0) {
            lastMode = "block"
            val db = IntArray(128); val nd = defenses(op, db)
            if (nd <= 1) return res(if (nd == 1) db[0] else findHot(op, CaroPat.C5), 0)
            forced = db.copyOf(nd)
        }
        if (vcfDepth > 0 && forced == null) {
            vNodes = 0; vLimit = 500_000; vDeadline = frac(0.15); vMove = -1
            if (vcf(me, vcfDepth, true) && vMove >= 0) { lastMode = "vcf"; return res(vMove, WIN) }
        }
        var n = genMoves(0, me, -1)
        if (n == 0) { lastMode = "none"; return res(cell(P / 2, P / 2), 0) }
        var rootMoves = forced ?: mbuf[0].copyOf(min(n, prm[CP.ROOTN]))
        lastMode = if (forced != null) "block" else "search"
        if (defend && forced == null && cnt[op][CaroPat.C4] == 0 && cnt[op][CaroPat.CB4] + cnt[op][CaroPat.C43] > 0) {
            val defEnd = frac(0.35)
            vNodes = 0; vLimit = 30_000; vDeadline = defEnd
            // đối phương có VCF nếu được đi? (giả lập bằng cách cho đối phương đi)
            toMove = op
            val opHas = vcf(op, 16, false)
            toMove = me
            if (opHas) {
                val safe = ArrayList<Int>()
                // ứng viên: mọi ô nóng hai bên + nước tốt
                val pool = LinkedHashSet<Int>()
                for (c in rootMoves) pool.add(c)
                for (i in 0 until hotLen[op]) pool.add(hot[op][i])
                for (c in pool) {
                    if (System.nanoTime() > defEnd) break
                    put(c, me)
                    vNodes = 0; vLimit = 8_000; vDeadline = defEnd
                    val opV = vcf(op, 16, false)
                    val exhausted = vNodes > vLimit || System.nanoTime() > defEnd
                    take(c)
                    if (!opV && !exhausted) safe.add(c)
                }
                if (safe.isNotEmpty()) { rootMoves = safe.toIntArray(); lastMode = "defend" }
            }
        }
        if (rootMoves.size == 1) return res(rootMoves[0], 0)
        val m = rootMoves.size
        val scores = IntArray(m)
        var best = rootMoves[0]; var bestScore = 0; var completed = 0
        for (depth in 1..maxDepth) {
            nodeLimit = if (depth <= 2) Long.MAX_VALUE else nl
            qLimit = min(MAXPLY - 4, depth + prm[CP.QEXT])
            var alpha = -WIN * 2; var iterBest = -1; var iterScore = -WIN * 2
            for (k in 0 until m) {
                val c = rootMoves[k]
                put(c, me)
                var s: Int
                if (k == 0) s = -search(depth - 1, -WIN * 2, -alpha, 1)
                else {
                    s = -search(depth - 1, -alpha - 1, -alpha, 1)
                    if (s > alpha && !abort) s = -search(depth - 1, -WIN * 2, -alpha, 1)
                }
                take(c)
                if (abort) break
                scores[k] = s
                if (s > iterScore) { iterScore = s; iterBest = c }
                if (s > alpha) alpha = s
            }
            if (abort) {
                // dùng kết quả dở dang nếu nước đầu đã xong và có nước khác vượt nó
                if (iterBest >= 0 && iterBest != best && iterScore > bestScore) { best = iterBest; bestScore = iterScore }
                break
            }
            best = iterBest; bestScore = iterScore; completed = depth; lastDepth = depth
            // đưa nước tốt nhất lên đầu, các nước khác giữ thứ tự theo điểm
            val order = (0 until m).sortedByDescending { scores[it] }
            val nr = IntArray(m) { rootMoves[order[it]] }; val ns = IntArray(m) { scores[order[it]] }
            nr.copyInto(rootMoves); ns.copyInto(scores)
            if (iterScore >= WIN - 200 || iterScore <= -WIN + 200) break
            // vòng sau thường tốn gấp 2-3 lần; vòng dở dang vẫn được dùng nên chỉ dừng khi đã qua 70% ngân sách
            if (timeMs > 0 && System.nanoTime() - start > (deadline - start) * prm[CP.STOP] / 100) break
        }
        lastNodes = nodes
        var near = IntArray(0)
        if (delta > 0 && completed > 0) {
            abort = false; nodeLimit = Long.MAX_VALUE; deadline = Long.MAX_VALUE
            val thr = bestScore - delta
            val outL = ArrayList<Int>()
            for (k in 0 until m) {
                val c = rootMoves[k]
                if (c == best) continue
                put(c, me)
                val s = -search(completed - 1, -thr, -thr + 1, 1)
                take(c)
                if (s >= thr) outL.add(out(c))
            }
            near = outL.toIntArray()
        }
        return res(best, bestScore, completed, near)
    }

    /** Nước theo cấp trên cửa sổ đã nạp (mã y*32+x). */
    fun levelSearch(cfg: LevelConfig, level: BotLevel): RootResult = synchronized(lock) {
        root(cfg.limit, cfg.extra, level >= BotLevel.STRONG, if (cfg.noiseP > 0) cfg.delta else 0)
    }
}
