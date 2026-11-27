package com.fws.playmind.core

import java.lang.Long.bitCount
import java.lang.Long.numberOfTrailingZeros
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Bảng tấn công bitboard dùng chung. Quân trượt dùng magic bitboard; số magic được tìm lúc khởi động
 * với seed cố định (thuật toán bảo đảm đúng: chỉ nhận magic không gây va chạm sai).
 * Ô: a1 = 0, h8 = 63.
 */
@OptIn(ExperimentalUnsignedTypes::class)
internal object BB {
    val KNIGHT = LongArray(64)
    val KING = LongArray(64)
    /** PAWN[c][sq]: các ô mà tốt màu c (0 trắng, 1 đen) đứng ở sq tấn công. */
    val PAWN = Array(2) { LongArray(64) }
    val FILE = LongArray(8) { 0x0101010101010101L shl it }
    val RANK = LongArray(8) { 0xFFL shl (8 * it) }
    val ADJ = LongArray(8)
    /** PASSED[c][sq]: ô phía trước (theo màu c) trên cột của sq và hai cột bên. */
    val PASSED = Array(2) { LongArray(64) }
    /** FRONT[c][sq]: ô phía trước trên cùng cột. */
    val FRONT = Array(2) { LongArray(64) }
    /** UPTO[c][r]: các hàng từ phía mình tới hàng r (tính cả r). */
    val UPTO = Array(2) { LongArray(8) }
    val DIST = Array(64) { IntArray(64) }
    const val DARK = -0x55aa55aa55aa55abL   // 0xAA55AA55AA55AA55: a1, c1, ... là ô đen

    private val RM = LongArray(64); private val BM = LongArray(64)
    internal val RMAG = LongArray(64); internal val BMAG = LongArray(64)
    private val RSH = IntArray(64); private val BSH = IntArray(64)
    private val ROFF = IntArray(64); private val BOFF = IntArray(64)
    private val TABLE: LongArray

    // Số magic đã tìm sẵn (cùng thuật toán findMagic, seed cố định) để khởi động nhanh trên điện thoại.
    private val ROOK_MAGIC = ulongArrayOf(
            0x80002810804002uL, 0x2840100820004000uL, 0x3100084020001100uL, 0x500100100a42008uL,
            0x3200050a00502820uL, 0x4500080100240002uL, 0x880010000801200uL, 0x4a80008004204500uL,
            0x1013801040048220uL, 0x4045400048201002uL, 0x801000802000uL, 0x2002000812004022uL,
            0x441800400080080uL, 0x242000830220184uL, 0x4009024082102uL, 0x20b2800480004100uL,
            0x2200848000400020uL, 0x40008040802002uL, 0x881010040200012uL, 0x9000420009220210uL,
            0x4000808004000800uL, 0x2902008080040002uL, 0x4881008080020001uL, 0x220001538904uL,
            0xd250400080008821uL, 0x8480200040005000uL, 0x100080200088uL, 0x8040100080800800uL,
            0x6010c0180080080uL, 0x6020080800400uL, 0x4c80080400100201uL, 0x80408200010044uL,
            0x2000400024800480uL, 0x408106002203uL, 0x400200101001040uL, 0x200100009002101uL,
            0x49c810800800400uL, 0x4000020080800400uL, 0x4000800100800200uL, 0x8800092082000044uL,
            0x2000400080008020uL, 0x400020008080uL, 0x230200010008080uL, 0x4000100008008080uL,
            0x55000408010010uL, 0x30b402004080110uL, 0x4008121841040010uL, 0x1012080420004uL,
            0x824400480102080uL, 0x4893100420200uL, 0x1000110420004100uL, 0x80080080100080uL,
            0x8000100408010100uL, 0x8006020004008080uL, 0x801001200040500uL, 0x182854010c8a00uL,
            0xb0800040210417uL, 0x202108d00400021uL, 0x1006900a00a18042uL, 0x10081001842101uL,
            0x4141001800103205uL, 0x1000802040001uL, 0x20620088011004uL, 0x1244022a8102uL,
    )
    private val BISHOP_MAGIC = ulongArrayOf(
            0x604011002008108uL, 0x350100218842812uL, 0x83081004b000064uL, 0x44040882004000uL,
            0x102121000600020uL, 0x10420040c0320uL, 0x46021024840024uL, 0x801222210042020uL,
            0x80e2040408080108uL, 0x8002045084050020uL, 0x1000088084148001uL, 0x4073110400800014uL,
            0x2011040420800200uL, 0x902431012101400uL, 0x8020124034040uL, 0x2210008400821001uL,
            0x8808109002880800uL, 0x4002412408820402uL, 0x4000808202200uL, 0x2c000a02120020uL,
            0x4002a11040000uL, 0x8000822410008803uL, 0x1000400884400uL, 0x8000440e8885uL,
            0x4008090020021060uL, 0x4200204281080uL, 0x85480511180500uL, 0x18080000220020uL,
            0x84004b802001uL, 0xc8084000806000uL, 0x204004609380202uL, 0x4050a0011048080uL,
            0x8308241034410209uL, 0x854c2000022801uL, 0x8002004040040100uL, 0x1004020080080080uL,
            0x2040008020620202uL, 0x1042480840120040uL, 0x208064040c28800uL, 0x8104900048080uL,
            0x104100407c040uL, 0x120a01a0000400uL, 0x3044422005000uL, 0xc405104200800800uL,
            0x128c02082000120uL, 0x600230b101000200uL, 0x501010888189810buL, 0x2140100204206uL,
            0x1209004200010uL, 0x81010090040040uL, 0x46000420804004cuL, 0x404dc0484042202uL,
            0x8000a40f0410000uL, 0x8202202022008c80uL, 0x891104109041000uL, 0x20010200850241uL,
            0x112002104100400uL, 0x1021080845000uL, 0x800080052089000uL, 0x2000000208800uL,
            0x4a80204008030400uL, 0x845280488700102uL, 0x1000404848009080uL, 0x203060080041404auL,
    )

    private val RD = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))
    private val BD = arrayOf(intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1))

    private fun slide(sq: Int, occ: Long, dirs: Array<IntArray>): Long {
        var a = 0L
        val f0 = sq and 7; val r0 = sq shr 3
        for (d in dirs) {
            var f = f0 + d[0]; var r = r0 + d[1]
            while (f in 0..7 && r in 0..7) {
                val s = r * 8 + f
                a = a or (1L shl s)
                if ((occ ushr s) and 1L != 0L) break
                f += d[0]; r += d[1]
            }
        }
        return a
    }

    init {
        for (sq in 0 until 64) {
            val f = sq and 7; val r = sq shr 3
            for ((df, dr) in listOf(1 to 2, 2 to 1, -1 to 2, -2 to 1, 1 to -2, 2 to -1, -1 to -2, -2 to -1)) {
                val nf = f + df; val nr = r + dr
                if (nf in 0..7 && nr in 0..7) KNIGHT[sq] = KNIGHT[sq] or (1L shl (nr * 8 + nf))
            }
            for (df in -1..1) for (dr in -1..1) {
                if (df == 0 && dr == 0) continue
                val nf = f + df; val nr = r + dr
                if (nf in 0..7 && nr in 0..7) KING[sq] = KING[sq] or (1L shl (nr * 8 + nf))
            }
            for (df in intArrayOf(-1, 1)) {
                val nf = f + df
                if (nf !in 0..7) continue
                if (r < 7) PAWN[0][sq] = PAWN[0][sq] or (1L shl ((r + 1) * 8 + nf))
                if (r > 0) PAWN[1][sq] = PAWN[1][sq] or (1L shl ((r - 1) * 8 + nf))
            }
            for (s2 in 0 until 64) DIST[sq][s2] = max(abs((s2 and 7) - f), abs((s2 shr 3) - r))
        }
        for (f in 0..7) ADJ[f] = (if (f > 0) FILE[f - 1] else 0L) or (if (f < 7) FILE[f + 1] else 0L)
        for (r in 0..7) {
            var w = 0L; for (k in 0..r) w = w or RANK[k]; UPTO[0][r] = w
            var b = 0L; for (k in r..7) b = b or RANK[k]; UPTO[1][r] = b
        }
        for (sq in 0 until 64) {
            val f = sq and 7; val r = sq shr 3
            var wf = 0L; var bf = 0L
            for (k in r + 1..7) wf = wf or RANK[k]
            for (k in 0 until r) bf = bf or RANK[k]
            FRONT[0][sq] = wf and FILE[f]; FRONT[1][sq] = bf and FILE[f]
            PASSED[0][sq] = wf and (FILE[f] or ADJ[f]); PASSED[1][sq] = bf and (FILE[f] or ADJ[f])
        }
        // magic bitboard
        var total = 0
        val rnd = java.util.Random(0x9E3779B97F4A7C15uL.toLong())
        for (sq in 0 until 64) {
            val f = sq and 7; val r = sq shr 3
            val edges = ((RANK[0] or RANK[7]) and RANK[r].inv()) or ((FILE[0] or FILE[7]) and FILE[f].inv())
            RM[sq] = slide(sq, 0L, RD) and edges.inv()
            BM[sq] = slide(sq, 0L, BD) and edges.inv()
            RSH[sq] = 64 - bitCount(RM[sq]); BSH[sq] = 64 - bitCount(BM[sq])
            ROFF[sq] = total; total += 1 shl bitCount(RM[sq])
        }
        for (sq in 0 until 64) { BOFF[sq] = total; total += 1 shl bitCount(BM[sq]) }
        TABLE = LongArray(total)
        for (sq in 0 until 64) {
            RMAG[sq] = if (fill(sq, RM[sq], RD, ROOK_MAGIC[sq].toLong(), ROFF[sq])) ROOK_MAGIC[sq].toLong() else findMagic(sq, RM[sq], RD, rnd, ROFF[sq])
            BMAG[sq] = if (fill(sq, BM[sq], BD, BISHOP_MAGIC[sq].toLong(), BOFF[sq])) BISHOP_MAGIC[sq].toLong() else findMagic(sq, BM[sq], BD, rnd, BOFF[sq])
        }
    }

    /** Điền bảng bằng magic có sẵn; trả false nếu magic không hợp lệ (khi đó tìm lại). */
    private fun fill(sq: Int, mask: Long, dirs: Array<IntArray>, magic: Long, off: Int): Boolean {
        val bits = bitCount(mask); var sub = 0L
        val filled = BooleanArray(1 shl bits)
        do {
            val idx = ((sub * magic) ushr (64 - bits)).toInt(); val a = slide(sq, sub, dirs)
            if (filled[idx] && TABLE[off + idx] != a) { for (i in filled.indices) TABLE[off + i] = 0L; return false }
            filled[idx] = true; TABLE[off + idx] = a
            sub = (sub - mask) and mask
        } while (sub != 0L)
        return true
    }

    private fun findMagic(sq: Int, mask: Long, dirs: Array<IntArray>, rnd: java.util.Random, off: Int): Long {
        val bits = bitCount(mask); val n = 1 shl bits
        val occs = LongArray(n); val atts = LongArray(n)
        var sub = 0L; var i = 0
        do { occs[i] = sub; atts[i] = slide(sq, sub, dirs); i++; sub = (sub - mask) and mask } while (sub != 0L)
        val used = LongArray(n); val epoch = IntArray(n); var ep = 0
        while (true) {
            val magic = rnd.nextLong() and rnd.nextLong() and rnd.nextLong()
            if (bitCount((mask * magic) and -0x100000000000000L) < 6) continue
            ep++
            var ok = true
            for (j in 0 until n) {
                val idx = ((occs[j] * magic) ushr (64 - bits)).toInt()
                if (epoch[idx] != ep) { epoch[idx] = ep; used[idx] = atts[j] }
                else if (used[idx] != atts[j]) { ok = false; break }
            }
            if (ok) {
                for (idx in 0 until n) if (epoch[idx] == ep) TABLE[off + idx] = used[idx]
                return magic
            }
        }
    }

    fun rook(sq: Int, occ: Long): Long = TABLE[ROFF[sq] + (((occ and RM[sq]) * RMAG[sq]) ushr RSH[sq]).toInt()]
    fun bishop(sq: Int, occ: Long): Long = TABLE[BOFF[sq] + (((occ and BM[sq]) * BMAG[sq]) ushr BSH[sq]).toInt()]
}

/**
 * Bố cục trọng số đánh giá (mỗi đặc trưng có trọng số trung cuộc MG và tàn cuộc EG).
 * Các trọng số được tinh chỉnh tự động bằng Texel tuning (tools/bot-bench, ChessTune.kt) và ghi vào [ChessWeightsData].
 */
internal object CW {
    private var n = 0
    private fun alloc(k: Int): Int { val s = n; n += k; return s }
    val MAT = alloc(7)            // theo loại quân 1..5
    val PST = alloc(6 * 64)       // (loại - 1) * 64 + ô (góc nhìn trắng)
    val MOB_N = alloc(9); val MOB_B = alloc(14); val MOB_R = alloc(15); val MOB_Q = alloc(28)
    val DOUBLED = alloc(1); val ISOLATED = alloc(1); val BACKWARD = alloc(1)
    val PHALANX = alloc(8); val SUPPORTED = alloc(8)
    val PASSED = alloc(8); val PASSED_BLOCKED = alloc(8); val PASSED_OWNK = alloc(8); val PASSED_OPPK = alloc(8)
    val PASSED_FREE = alloc(8)
    val BISHOP_PAIR = alloc(1); val ROOK_OPEN = alloc(1); val ROOK_SEMI = alloc(1); val ROOK_7TH = alloc(1)
    val KNIGHT_OUTPOST = alloc(1); val BISHOP_OUTPOST = alloc(1); val BAD_BISHOP = alloc(1)
    val SHIELD1 = alloc(1); val SHIELD2 = alloc(1); val KING_OPEN = alloc(1)
    val KATT = alloc(7); val SAFE_CHECK = alloc(7)
    val THREAT_PAWN = alloc(1); val THREAT_MINOR_MAJOR = alloc(1); val THREAT_ROOK_QUEEN = alloc(1); val HANGING = alloc(1)
    val TEMPO = alloc(1)
    val SIZE = n
    val MG = IntArray(SIZE)
    val EG = IntArray(SIZE)

    init {
        if (!ChessWeightsData.load(MG, EG)) defaults()
    }

    /** Giá trị khởi điểm (trước khi tinh chỉnh): lấy từ engine cũ. */
    fun defaults() {
        val mat = intArrayOf(0, 82, 337, 365, 477, 1025, 0); val matE = intArrayOf(0, 94, 281, 297, 512, 936, 0)
        for (t in 1..5) { MG[MAT + t] = mat[t]; EG[MAT + t] = matE[t] }
        val pst = arrayOf(
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
                -20,-30,-30,-40,-40,-30,-30,-20, -10,-20,-20,-20,-20,-20,-20,-10, 20,20,0,0,0,0,20,20, 20,30,10,0,0,10,30,20))
        val kingEnd = intArrayOf(-50,-40,-30,-20,-20,-30,-40,-50, -30,-20,-10,0,0,-10,-20,-30, -30,-10,20,30,30,20,-10,-30, -30,-10,30,40,40,30,-10,-30,
            -30,-10,30,40,40,30,-10,-30, -30,-10,20,30,30,20,-10,-30, -30,-30,0,0,0,0,-30,-30, -50,-30,-30,-30,-30,-30,-30,-50)
        val pawnEg = intArrayOf(0, 0, 4, 12, 26, 48, 85, 0)
        for (t in 0..5) for (sq in 0 until 64) {
            val idx = (7 - (sq shr 3)) * 8 + (sq and 7)   // bảng cũ viết từ hàng 8 xuống
            MG[PST + t * 64 + sq] = pst[t][idx]
            EG[PST + t * 64 + sq] = when (t) { 0 -> pawnEg[sq shr 3]; 5 -> kingEnd[idx]; else -> pst[t][idx] }
        }
        for (m in 0 until 9) { MG[MOB_N + m] = 4 * (m - 4); EG[MOB_N + m] = 4 * (m - 4) }
        for (m in 0 until 14) { MG[MOB_B + m] = 5 * (m - 6); EG[MOB_B + m] = 5 * (m - 6) }
        for (m in 0 until 15) { MG[MOB_R + m] = 2 * (m - 6); EG[MOB_R + m] = 4 * (m - 6) }
        for (m in 0 until 28) { MG[MOB_Q + m] = m - 12; EG[MOB_Q + m] = 2 * (m - 12) }
        MG[DOUBLED] = -8; EG[DOUBLED] = -12; MG[ISOLATED] = -10; EG[ISOLATED] = -14; MG[BACKWARD] = -5; EG[BACKWARD] = -5
        val passed = intArrayOf(0, 5, 10, 20, 38, 65, 105, 0)
        for (r in 0..7) {
            MG[PHALANX + r] = 3 * r; EG[PHALANX + r] = 3 * r; MG[SUPPORTED + r] = 2 * r; EG[SUPPORTED + r] = 2 * r
            MG[PASSED + r] = passed[r] / 2; EG[PASSED + r] = passed[r]
            MG[PASSED_BLOCKED + r] = -3 * r; EG[PASSED_BLOCKED + r] = -5 * r
            EG[PASSED_OWNK + r] = -2 * r; EG[PASSED_OPPK + r] = 3 * r
            MG[PASSED_FREE + r] = r; EG[PASSED_FREE + r] = 3 * r
        }
        MG[BISHOP_PAIR] = 30; EG[BISHOP_PAIR] = 50; MG[ROOK_OPEN] = 22; EG[ROOK_OPEN] = 14; MG[ROOK_SEMI] = 11; EG[ROOK_SEMI] = 8
        MG[ROOK_7TH] = 10; EG[ROOK_7TH] = 20; MG[KNIGHT_OUTPOST] = 20; EG[KNIGHT_OUTPOST] = 10; MG[BISHOP_OUTPOST] = 10; EG[BISHOP_OUTPOST] = 5
        MG[BAD_BISHOP] = -2; EG[BAD_BISHOP] = -4
        MG[SHIELD1] = 10; MG[SHIELD2] = 5; MG[KING_OPEN] = -14
        val katt = intArrayOf(0, 0, 6, 5, 7, 8, 0); val chk = intArrayOf(0, 0, 20, 15, 25, 20, 0)
        for (t in 0..6) { MG[KATT + t] = katt[t]; MG[SAFE_CHECK + t] = chk[t]; EG[SAFE_CHECK + t] = chk[t] / 4 }
        MG[THREAT_PAWN] = 40; EG[THREAT_PAWN] = 30; MG[THREAT_MINOR_MAJOR] = 30; EG[THREAT_MINOR_MAJOR] = 25
        MG[THREAT_ROOK_QUEEN] = 25; EG[THREAT_ROOK_QUEEN] = 20; MG[HANGING] = 20; EG[HANGING] = 15
        MG[TEMPO] = 12; EG[TEMPO] = 12
    }
}

/**
 * Engine cờ vua (Siêu việt): bitboard + magic, PVS với bảng băm có thế hệ, aspiration window, null-move,
 * reverse futility, razoring, futility, late move pruning, cắt tỉa theo SEE và history, LMR, singular extension,
 * internal iterative reduction, killer / countermove / history / continuation history / capture history,
 * tìm kiếm tĩnh có SEE. Đánh giá tuyến tính theo [CW] (tinh chỉnh bằng Texel tuning).
 *
 * Mã hóa nước đi giống [ChessGame]: from | to<<6 | promo<<12 | flag<<16 (flag 1 = bắt tốt qua đường, 2 = nhập thành, 4 = tốt đi hai ô).
 * Luật của app: nước tạo lại thế đã có (cùng bên tới lượt) thì bên vừa đi thua.
 * Mỗi instance có bảng băm riêng, không dùng chung giữa các luồng.
 */
/**
 * Bảng băm dùng chung giữa các luồng tìm kiếm (Lazy SMP). Mỗi mục lưu khóa XOR dữ liệu để phát hiện mục bị ghi dở
 * khi hai luồng ghi cùng lúc (khi đó coi như không trúng).
 */
class ChessTT(bits: Int) {
    val mask = (1 shl bits) - 1
    val key = LongArray(1 shl bits)
    val data = LongArray(1 shl bits)
    @Volatile var gen = 0
    /** Cờ dừng cho các luồng phụ. */
    @Volatile var stop = false
    fun clear() { key.fill(0); data.fill(0) }
}

class ChessEngine(private val tt: ChessTT) {
    constructor(ttBits: Int = 20) : this(ChessTT(ttBits))
    companion object {
        const val MATE = 30000
        const val INF = 32000
        private const val NONE = -32500
        const val MAXPLY = 120
        private const val P = 1; private const val N = 2; private const val B = 3; private const val R = 4; private const val Q = 5; private const val K = 6
        private const val F_EP = 1; private const val F_CASTLE = 2; private const val F_DOUBLE = 4
        private const val EXACT = 0; private const val LOWER = 1; private const val UPPER = 2
        private val SEEV = intArrayOf(0, 100, 320, 330, 500, 950, 20000, 0)
        private val PHASE = intArrayOf(0, 0, 1, 1, 2, 4, 0, 0)
        private val zr = java.util.Random(0x0C0FFEE12345L)
        private val ZP = Array(16) { LongArray(64) { zr.nextLong() } }
        private val ZSIDE = zr.nextLong()
        private val ZCASTLE = LongArray(16) { zr.nextLong() }
        private val ZEP = LongArray(8) { zr.nextLong() }
        private val CMASK = IntArray(64) { 15 }.also {
            it[0] = 15 and 2.inv(); it[7] = 15 and 1.inv(); it[4] = 15 and 3.inv()
            it[56] = 15 and 8.inv(); it[63] = 15 and 4.inv(); it[60] = 15 and 12.inv()
        }
        private val LMR = Array(64) { d -> IntArray(64) { m -> if (d == 0 || m == 0) 0 else (0.8 + ln(d.toDouble()) * ln(m.toDouble()) / 2.3).toInt() } }
        const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        private const val NOT_A = -0x0101010101010102L   // ~FILE_A
        private const val NOT_H = 0x7F7F7F7F7F7F7F7FL    // ~FILE_H
    }

    // ───── trạng thái bàn cờ ─────
    private val pc = IntArray(64)           // mã quân: loại | (màu << 3), 0 = trống
    private val bb = LongArray(16)
    private val occC = LongArray(2)
    private var occ = 0L
    var stm = 0; private set
    private var castling = 0
    private var ep = -1
    private var half = 0
    private var hash = 0L
    private val kingSq = IntArray(2)

    private val STACK = 2048 + MAXPLY + 8
    private val uMove = IntArray(STACK); private val uCap = IntArray(STACK); private val uCast = IntArray(STACK)
    private val uEp = IntArray(STACK); private val uHalf = IntArray(STACK); private val uHash = LongArray(STACK)
    private var sp = 0
    private val posHash = LongArray(STACK)
    private var hp = 0

    // ───── bảng băm ─────
    private val ttMask = tt.mask
    private val ttKey = tt.key
    private val ttData = tt.data
    /** 0 = luồng chính; > 0 = luồng phụ của Lazy SMP. */
    private var helperId = 0
    private val helpers = ArrayList<ChessEngine>()

    // ───── bảng thứ tự nước ─────
    private val hist = Array(2) { IntArray(4096) }
    private val cont = Array(1024) { IntArray(1024) }
    private val capHist = Array(16) { Array(64) { IntArray(8) } }
    private val killers = Array(MAXPLY + 2) { IntArray(2) }
    private val counter = IntArray(1024)

    // ───── ngăn xếp tìm kiếm ─────
    private val bufs = Array(MAXPLY + 2) { IntArray(256) }
    private val scr = Array(MAXPLY + 2) { IntArray(256) }
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
    private fun put(sq: Int, p: Int) {
        pc[sq] = p; val b = 1L shl sq
        bb[p] = bb[p] or b; occC[p shr 3] = occC[p shr 3] or b; occ = occ or b
        hash = hash xor ZP[p][sq]
    }

    private fun remove(sq: Int) {
        val p = pc[sq]; val b = (1L shl sq).inv()
        pc[sq] = 0; bb[p] = bb[p] and b; occC[p shr 3] = occC[p shr 3] and b; occ = occ and b
        hash = hash xor ZP[p][sq]
    }

    fun setFen(fen: String) {
        pc.fill(0); bb.fill(0); occC.fill(0); occ = 0; hash = 0; sp = 0
        val parts = fen.trim().split(Regex("\\s+"))
        var rank = 7; var file = 0
        for (c in parts[0]) {
            when {
                c == '/' -> { rank--; file = 0 }
                c.isDigit() -> file += c.digitToInt()
                else -> {
                    val t = when (c.lowercaseChar()) { 'p' -> P; 'n' -> N; 'b' -> B; 'r' -> R; 'q' -> Q; else -> K }
                    val col = if (c.isUpperCase()) 0 else 1
                    put(rank * 8 + file, t or (col shl 3))
                    if (t == K) kingSq[col] = rank * 8 + file
                    file++
                }
            }
        }
        stm = if (parts.getOrElse(1) { "w" } == "w") 0 else 1
        val c = parts.getOrElse(2) { "-" }
        castling = (if ('K' in c) 1 else 0) or (if ('Q' in c) 2 else 0) or (if ('k' in c) 4 else 0) or (if ('q' in c) 8 else 0)
        val e = parts.getOrElse(3) { "-" }
        ep = if (e.length == 2) (e[1] - '1') * 8 + (e[0] - 'a') else -1
        half = parts.getOrElse(4) { "0" }.toIntOrNull() ?: 0
        if (stm == 1) hash = hash xor ZSIDE
        hash = hash xor ZCASTLE[castling]
        if (ep >= 0) hash = hash xor ZEP[ep and 7]
        hp = 0; posHash[0] = hash
    }

    /** Nạp thế cờ: thế đầu [fen] rồi đi lần lượt [moves] (mã nước của ChessGame; khớp theo ô đi, ô đến, quân phong). */
    fun load(fen: String, moves: List<Int>) {
        setFen(fen)
        for (m in moves) {
            val mv = match(m)
            require(mv != 0) { "nước không hợp lệ khi nạp: $m" }
            make(mv)
        }
        // giữ lịch sử thế cờ (cho luật lặp thế) nhưng xả ngăn xếp hoàn tác để tìm kiếm có đủ chỗ
        if (sp > 0) {
            val keep = minOf(hp, half)
            System.arraycopy(posHash, hp - keep, posHash, 0, keep + 1)
            hp = keep; sp = 0
        }
    }

    private fun match(m: Int): Int {
        val buf = IntArray(256); val n = gen(buf, false)
        for (i in 0 until n) {
            val x = buf[i]
            if ((x and 0x7FFF) == (m and 0x7FFF) && legal(x)) return x
        }
        return 0
    }

    private fun legal(m: Int): Boolean { make(m); val ok = !attacked(kingSq[stm xor 1], stm); unmake(); return ok }

    fun inCheck(): Boolean = attacked(kingSq[stm], stm xor 1)

    private fun attacked(sq: Int, by: Int): Boolean {
        val o = by shl 3
        if (BB.PAWN[by xor 1][sq] and bb[P or o] != 0L) return true
        if (BB.KNIGHT[sq] and bb[N or o] != 0L) return true
        if (BB.KING[sq] and bb[K or o] != 0L) return true
        if (BB.bishop(sq, occ) and (bb[B or o] or bb[Q or o]) != 0L) return true
        return BB.rook(sq, occ) and (bb[R or o] or bb[Q or o]) != 0L
    }

    private fun attackersTo(sq: Int, o: Long): Long =
        (BB.PAWN[1][sq] and bb[P]) or (BB.PAWN[0][sq] and bb[P or 8]) or
            (BB.KNIGHT[sq] and (bb[N] or bb[N or 8])) or (BB.KING[sq] and (bb[K] or bb[K or 8])) or
            (BB.bishop(sq, o) and (bb[B] or bb[B or 8] or bb[Q] or bb[Q or 8])) or
            (BB.rook(sq, o) and (bb[R] or bb[R or 8] or bb[Q] or bb[Q or 8]))

    private fun mv(from: Int, to: Int, promo: Int = 0, flag: Int = 0) = from or (to shl 6) or (promo shl 12) or (flag shl 16)

    private fun gen(out: IntArray, capsOnly: Boolean): Int {
        var n = 0
        val us = stm; val them = us xor 1
        val enemy = occC[them]; val empty = occ.inv()
        val fwd = if (us == 0) 8 else -8
        val promoRank = if (us == 0) 7 else 0; val startRank = if (us == 0) 1 else 6
        var b = bb[P or (us shl 3)]
        while (b != 0L) {
            val s = numberOfTrailingZeros(b); b = b and (b - 1)
            val to = s + fwd
            if ((empty ushr to) and 1L != 0L) {
                if ((to shr 3) == promoRank) {
                    out[n++] = mv(s, to, Q)
                    if (!capsOnly) { out[n++] = mv(s, to, N); out[n++] = mv(s, to, R); out[n++] = mv(s, to, B) }
                } else if (!capsOnly) {
                    out[n++] = mv(s, to)
                    if ((s shr 3) == startRank && (empty ushr (to + fwd)) and 1L != 0L) out[n++] = mv(s, to + fwd, 0, F_DOUBLE)
                }
            }
            var a = BB.PAWN[us][s] and enemy
            while (a != 0L) {
                val t = numberOfTrailingZeros(a); a = a and (a - 1)
                if ((t shr 3) == promoRank) {
                    out[n++] = mv(s, t, Q)
                    if (!capsOnly) { out[n++] = mv(s, t, N); out[n++] = mv(s, t, R); out[n++] = mv(s, t, B) }
                } else out[n++] = mv(s, t)
            }
            if (ep >= 0 && (BB.PAWN[us][s] ushr ep) and 1L != 0L) out[n++] = mv(s, ep, 0, F_EP)
        }
        val target = if (capsOnly) enemy else occC[us].inv()
        for (t in N..K) {
            var p = bb[t or (us shl 3)]
            while (p != 0L) {
                val s = numberOfTrailingZeros(p); p = p and (p - 1)
                var a = when (t) {
                    N -> BB.KNIGHT[s]; B -> BB.bishop(s, occ); R -> BB.rook(s, occ)
                    Q -> BB.bishop(s, occ) or BB.rook(s, occ); else -> BB.KING[s]
                } and target
                while (a != 0L) { val d = numberOfTrailingZeros(a); a = a and (a - 1); out[n++] = mv(s, d) }
            }
        }
        if (!capsOnly && castling != 0) {
            val base = if (us == 0) 0 else 56
            val ks = if (us == 0) 1 else 4; val qs = if (us == 0) 2 else 8
            if (kingSq[us] == base + 4 && !attacked(base + 4, them)) {
                if (castling and ks != 0 && pc[base + 5] == 0 && pc[base + 6] == 0 && pc[base + 7] == (R or (us shl 3)) &&
                    !attacked(base + 5, them) && !attacked(base + 6, them)) out[n++] = mv(base + 4, base + 6, 0, F_CASTLE)
                if (castling and qs != 0 && pc[base + 3] == 0 && pc[base + 2] == 0 && pc[base + 1] == 0 && pc[base] == (R or (us shl 3)) &&
                    !attacked(base + 3, them) && !attacked(base + 2, them)) out[n++] = mv(base + 4, base + 2, 0, F_CASTLE)
            }
        }
        return n
    }

    private fun make(m: Int) {
        val from = m and 63; val to = (m shr 6) and 63; val promo = (m shr 12) and 7; val flag = m shr 16
        val p = pc[from]; val us = stm
        var cap = pc[to]
        uMove[sp] = m; uCast[sp] = castling; uEp[sp] = ep; uHalf[sp] = half; uHash[sp] = hash
        hash = hash xor ZSIDE xor ZCASTLE[castling]
        if (ep >= 0) hash = hash xor ZEP[ep and 7]
        if (flag and F_EP != 0) {
            val cs = if (us == 0) to - 8 else to + 8
            cap = pc[cs]; remove(cs)
        } else if (cap != 0) remove(to)
        uCap[sp] = cap; sp++
        remove(from)
        put(to, if (promo != 0) promo or (us shl 3) else p)
        if (flag and F_CASTLE != 0) {
            if (to > from) { val r = pc[from + 3]; remove(from + 3); put(from + 1, r) }
            else { val r = pc[from - 4]; remove(from - 4); put(from - 1, r) }
        }
        if ((p and 7) == K) kingSq[us] = to
        castling = castling and CMASK[from] and CMASK[to]
        hash = hash xor ZCASTLE[castling]
        if (flag and F_DOUBLE != 0) { ep = (from + to) / 2; hash = hash xor ZEP[ep and 7] } else ep = -1
        half = if ((p and 7) == P || cap != 0) 0 else half + 1
        stm = us xor 1
        hp++; posHash[hp] = hash
    }

    private fun unmake() {
        sp--; hp--
        val m = uMove[sp]
        val from = m and 63; val to = (m shr 6) and 63; val promo = (m shr 12) and 7; val flag = m shr 16
        stm = stm xor 1
        val us = stm
        val moved = pc[to]
        remove(to)
        put(from, if (promo != 0) P or (us shl 3) else moved)
        val cap = uCap[sp]
        if (cap != 0) { if (flag and F_EP != 0) put(if (us == 0) to - 8 else to + 8, cap) else put(to, cap) }
        if (flag and F_CASTLE != 0) {
            if (to > from) { val r = pc[from + 1]; remove(from + 1); put(from + 3, r) }
            else { val r = pc[from - 1]; remove(from - 1); put(from - 4, r) }
        }
        if ((moved and 7) == K) kingSq[us] = from
        castling = uCast[sp]; ep = uEp[sp]; half = uHalf[sp]; hash = uHash[sp]
    }

    private fun makeNull() {
        uMove[sp] = 0; uCap[sp] = 0; uCast[sp] = castling; uEp[sp] = ep; uHalf[sp] = half; uHash[sp] = hash; sp++
        hash = hash xor ZSIDE
        if (ep >= 0) { hash = hash xor ZEP[ep and 7]; ep = -1 }
        half++
        stm = stm xor 1
        hp++; posHash[hp] = hash
    }

    private fun unmakeNull() {
        sp--; hp--
        stm = stm xor 1
        ep = uEp[sp]; half = uHalf[sp]; hash = uHash[sp]
    }

    fun perft(depth: Int): Long {
        if (depth == 0) return 1
        val buf = IntArray(256); val n = gen(buf, false)
        var total = 0L
        for (i in 0 until n) {
            make(buf[i])
            if (!attacked(kingSq[stm xor 1], stm)) total += perft(depth - 1)
            unmake()
        }
        return total
    }

    /** Thế hiện tại lặp lại một thế cũ (cùng bên tới lượt) kể từ nước không thể đảo ngược gần nhất. */
    private fun isRepetition(): Boolean {
        var i = hp - 4; val lim = hp - half
        while (i >= lim && i >= 0) { if (posHash[i] == hash) return true; i -= 2 }
        return false
    }

    private fun nonPawn(c: Int): Boolean { val o = c shl 3; return (bb[N or o] or bb[B or o] or bb[R or o] or bb[Q or o]) != 0L }

    private fun insufficient(): Boolean {
        if ((bb[P] or bb[P or 8] or bb[R] or bb[R or 8] or bb[Q] or bb[Q or 8]) != 0L) return false
        return bitCount(bb[N] or bb[N or 8] or bb[B] or bb[B or 8]) <= 1
    }

    // ───────────────────────── SEE ─────────────────────────
    private val seeGain = IntArray(40)

    private fun see(m: Int): Int {
        val from = m and 63; val to = (m shr 6) and 63; val flag = m shr 16
        if (flag and F_CASTLE != 0) return 0
        var o = occ xor (1L shl from)
        val target: Int
        if (flag and F_EP != 0) { target = P; o = o xor (1L shl (if (stm == 0) to - 8 else to + 8)) } else target = pc[to] and 7
        var d = 0
        seeGain[0] = SEEV[target]
        var lastVal = SEEV[pc[from] and 7]
        var side = stm xor 1
        var att = attackersTo(to, o) and o
        val diag = bb[B] or bb[B or 8] or bb[Q] or bb[Q or 8]
        val orth = bb[R] or bb[R or 8] or bb[Q] or bb[Q or 8]
        while (true) {
            val mine = att and occC[side] and o
            if (mine == 0L) break
            var sq = -1; var v = 0
            for (t in P..K) {
                val x = mine and bb[t or (side shl 3)]
                if (x != 0L) { sq = numberOfTrailingZeros(x); v = SEEV[t]; break }
            }
            d++
            seeGain[d] = lastVal - seeGain[d - 1]
            if (max(-seeGain[d - 1], seeGain[d]) < 0) break
            lastVal = v
            o = o xor (1L shl sq)
            att = (att or (BB.bishop(to, o) and diag) or (BB.rook(to, o) and orth)) and o
            side = side xor 1
            if (d >= 36) break
        }
        while (d > 0) { seeGain[d - 1] = -max(-seeGain[d - 1], seeGain[d]); d-- }
        return seeGain[0]
    }

    // ───────────────────────── đánh giá ─────────────────────────
    /** true: ghi hệ số của từng đặc trưng vào [trace] (dùng cho Texel tuning). */
    var tracing = false
    val trace = IntArray(CW.SIZE)
    var tracePhase = 0; private set
    var traceScale = 64; private set
    var traceMopup = false; private set
    private var mgS = 0; private var egS = 0
    /** Trọng số đánh giá đang dùng (mặc định CW; bộ đo A/B có thể gán bộ khác). */
    var wMg: IntArray = CW.MG; var wEg: IntArray = CW.EG
    private val pawnAtt = LongArray(2)
    private val attAll = LongArray(2)
    private val attT = Array(2) { LongArray(8) }
    private val zone = LongArray(2)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun add(idx: Int, coef: Int) {
        mgS += wMg[idx] * coef; egS += wEg[idx] * coef
        if (tracing) trace[idx] += coef
    }

    private val evKey = LongArray(1 shl 16); private val evVal = IntArray(1 shl 16)

    /** Điểm đánh giá theo góc nhìn bên tới lượt (có bộ nhớ đệm theo khóa băm). */
    fun evaluate(): Int {
        if (tracing) return evaluateFull()
        val i = (hash and 0xFFFF).toInt()
        if (evKey[i] == hash) return evVal[i]
        val v = evaluateFull()
        evKey[i] = hash; evVal[i] = v
        return v
    }

    private fun evaluateFull(): Int {
        mgS = 0; egS = 0
        if (tracing) trace.fill(0)
        val wP = bb[P]; val bP = bb[P or 8]
        pawnAtt[0] = ((wP and NOT_A) shl 7) or ((wP and NOT_H) shl 9)
        pawnAtt[1] = ((bP and NOT_A) ushr 9) or ((bP and NOT_H) ushr 7)
        var phase = 0
        for (c in 0..1) {
            zone[c] = BB.KING[kingSq[c]] or (1L shl kingSq[c])
            attAll[c] = pawnAtt[c] or BB.KING[kingSq[c]]
            for (t in 0..7) attT[c][t] = 0L
            attT[c][P] = pawnAtt[c]
        }
        for (c in 0..1) {
            val s = if (c == 0) 1 else -1; val o = c shl 3; val e = c xor 1
            val area = occC[c].inv() and pawnAtt[e].inv()
            val ownP = bb[P or o]; val oppP = bb[P or (e shl 3)]
            // vật chất + bảng ô
            for (t in P..K) {
                var x = bb[t or o]
                while (x != 0L) {
                    val sq = numberOfTrailingZeros(x); x = x and (x - 1)
                    val rel = if (c == 0) sq else sq xor 56
                    if (t != K) add(CW.MAT + t, s)
                    add(CW.PST + (t - 1) * 64 + rel, s)
                    phase += PHASE[t]
                    if (t == P || t == K) continue
                    val att = when (t) {
                        N -> BB.KNIGHT[sq]; B -> BB.bishop(sq, occ xor bb[Q or o])
                        R -> BB.rook(sq, occ xor bb[Q or o] xor bb[R or o]); else -> BB.bishop(sq, occ) or BB.rook(sq, occ)
                    }
                    attAll[c] = attAll[c] or att; attT[c][t] = attT[c][t] or att
                    val mob = bitCount(att and area)
                    add(when (t) { N -> CW.MOB_N; B -> CW.MOB_B; R -> CW.MOB_R; else -> CW.MOB_Q } + mob, s)
                    val kz = bitCount(att and zone[e])
                    if (kz != 0) add(CW.KATT + t, s * kz)
                    val f = sq and 7; val rr = rel shr 3
                    when (t) {
                        N, B -> {
                            if (rr in 3..5 && (BB.PAWN[e][sq] and ownP) != 0L && (oppP and BB.PASSED[c][sq] and BB.ADJ[f]) == 0L)
                                add(if (t == N) CW.KNIGHT_OUTPOST else CW.BISHOP_OUTPOST, s)
                            if (t == B) {
                                val same = if ((BB.DARK ushr sq) and 1L != 0L) BB.DARK else BB.DARK.inv()
                                add(CW.BAD_BISHOP, s * bitCount(ownP and same))
                            }
                        }
                        R -> {
                            if ((ownP and BB.FILE[f]) == 0L) add(if ((oppP and BB.FILE[f]) == 0L) CW.ROOK_OPEN else CW.ROOK_SEMI, s)
                            if (rr == 6) add(CW.ROOK_7TH, s)
                        }
                    }
                }
            }
            if (bitCount(bb[B or o]) >= 2) add(CW.BISHOP_PAIR, s)
            // cấu trúc tốt
            var x = ownP
            val fwd = if (c == 0) 8 else -8
            while (x != 0L) {
                val sq = numberOfTrailingZeros(x); x = x and (x - 1)
                val f = sq and 7; val rr = if (c == 0) sq shr 3 else 7 - (sq shr 3)
                val doubled = (ownP and BB.FRONT[c][sq]) != 0L
                if (doubled) add(CW.DOUBLED, s)
                val isolated = (ownP and BB.ADJ[f]) == 0L
                if (isolated) add(CW.ISOLATED, s)
                val supported = (BB.PAWN[e][sq] and ownP) != 0L
                val phalanx = ((if (f > 0) 1L shl (sq - 1) else 0L) or (if (f < 7) 1L shl (sq + 1) else 0L)) and ownP != 0L
                if (phalanx) add(CW.PHALANX + rr, s)
                if (supported) add(CW.SUPPORTED + rr, s)
                val stop = sq + fwd
                if (!isolated && !supported && !phalanx && (ownP and BB.ADJ[f] and BB.UPTO[c][sq shr 3]) == 0L &&
                    (BB.PAWN[c][stop] and oppP) != 0L) add(CW.BACKWARD, s)
                if (!doubled && (oppP and BB.PASSED[c][sq]) == 0L) {
                    add(CW.PASSED + rr, s)
                    if ((occ ushr stop) and 1L != 0L) add(CW.PASSED_BLOCKED + rr, s)
                    add(CW.PASSED_OWNK + rr, s * BB.DIST[kingSq[c]][stop])
                    add(CW.PASSED_OPPK + rr, s * BB.DIST[kingSq[e]][stop])
                    if ((BB.FRONT[c][sq] and occ) == 0L) add(CW.PASSED_FREE + rr, s)
                }
            }
            // lá chắn vua
            val k = kingSq[c]; val kf = k and 7; val kr = if (c == 0) k shr 3 else 7 - (k shr 3)
            if (kr <= 1) {
                for (f in max(0, kf - 1)..min(7, kf + 1)) {
                    val r1 = k + fwd - kf + f; val r2 = r1 + fwd
                    if (r1 in 0..63 && (ownP ushr r1) and 1L != 0L) add(CW.SHIELD1, s)
                    else if (r2 in 0..63 && (ownP ushr r2) and 1L != 0L) add(CW.SHIELD2, s)
                    if ((ownP and BB.FILE[f]) == 0L) add(CW.KING_OPEN, s)
                }
            }
        }
        // đe dọa và chiếu an toàn (cần bản đồ tấn công đầy đủ của cả hai bên)
        for (c in 0..1) {
            val s = if (c == 0) 1 else -1; val e = c xor 1; val eo = e shl 3
            val safe = attAll[e].inv() and occC[c].inv()
            val ek = kingSq[e]
            val chkB = BB.bishop(ek, occ); val chkR = BB.rook(ek, occ)
            add(CW.SAFE_CHECK + N, s * bitCount(attT[c][N] and BB.KNIGHT[ek] and safe))
            add(CW.SAFE_CHECK + B, s * bitCount(attT[c][B] and chkB and safe))
            add(CW.SAFE_CHECK + R, s * bitCount(attT[c][R] and chkR and safe))
            add(CW.SAFE_CHECK + Q, s * bitCount(attT[c][Q] and (chkB or chkR) and safe))
            val nonPawn = occC[e] and bb[P or eo].inv() and bb[K or eo].inv()
            add(CW.THREAT_PAWN, s * bitCount(pawnAtt[c] and nonPawn))
            add(CW.THREAT_MINOR_MAJOR, s * bitCount((attT[c][N] or attT[c][B]) and (bb[R or eo] or bb[Q or eo])))
            add(CW.THREAT_ROOK_QUEEN, s * bitCount(attT[c][R] and bb[Q or eo]))
            add(CW.HANGING, s * bitCount(nonPawn and attAll[c] and attAll[e].inv()))
        }
        add(CW.TEMPO, if (stm == 0) 1 else -1)
        val ph = min(24, phase)
        var score = (mgS * ph + egS * (24 - ph)) / 24
        // hệ số hòa: tượng khác màu, bên mạnh không còn tốt mà hơn ít vật chất
        val scale = scaleFactor(if (egS > 0) 0 else 1)
        score = score * scale / 64
        tracePhase = ph; traceScale = scale
        traceMopup = false
        val mop = mopup(ph)
        if (mop != 0) { score += mop; traceMopup = true }
        return if (stm == 0) score else -score
    }

    private fun material(c: Int): Int {
        val o = c shl 3
        return bitCount(bb[N or o]) * 320 + bitCount(bb[B or o]) * 330 + bitCount(bb[R or o]) * 500 + bitCount(bb[Q or o]) * 950
    }

    private fun scaleFactor(strong: Int): Int {
        val weak = strong xor 1
        val so = strong shl 3
        if (bb[P or so] == 0L && material(strong) - material(weak) <= 330) return if (material(strong) <= 330) 0 else 8
        val onlyBishops = (bb[N] or bb[N or 8] or bb[R] or bb[R or 8] or bb[Q] or bb[Q or 8]) == 0L &&
            bitCount(bb[B]) == 1 && bitCount(bb[B or 8]) == 1
        if (onlyBishops && ((bb[B] and BB.DARK) != 0L) != ((bb[B or 8] and BB.DARK) != 0L)) return 32
        return 64
    }

    /** Dồn vua ở tàn cuộc thắng không còn tốt (để chiếu hết được KQK, KRK...). Không tinh chỉnh. */
    private fun mopup(ph: Int): Int {
        if (ph > 12) return 0
        val diff = material(0) - material(1)
        if (abs(diff) < 400) return 0
        val win = if (diff > 0) 0 else 1; val lose = win xor 1
        if (bb[P or (lose shl 3)] != 0L) return 0
        val lk = kingSq[lose]; val wk = kingSq[win]
        val lf = lk and 7; val lr = lk shr 3
        val cd = max(3 - lf, lf - 4) + max(3 - lr, lr - 4)
        val bonus = 10 * cd + 4 * (14 - (abs((wk and 7) - lf) + abs((wk shr 3) - lr)))
        return if (win == 0) bonus else -bonus
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
        return if (p < 0 || curMove[p] == 0) -1 else curPc[p] * 64 + ((curMove[p] shr 6) and 63)
    }

    private fun quietScore(m: Int, ply: Int): Int {
        val from = m and 63; val to = (m shr 6) and 63
        val pidx = pc[from] * 64 + to
        var s = hist[stm][from * 64 + to]
        val c1 = contIdx(ply, 1); if (c1 >= 0) s += cont[c1][pidx]
        val c2 = contIdx(ply, 2); if (c2 >= 0) s += cont[c2][pidx]
        return s
    }

    private fun scoreMoves(ply: Int, n: Int, ttm: Int) {
        val mvs = bufs[ply]; val sc = scr[ply]
        val k0 = killers[ply][0]; val k1 = killers[ply][1]
        val c1 = contIdx(ply, 1)
        val cm = if (c1 >= 0) counter[c1] else 0
        for (i in 0 until n) {
            val m = mvs[i]
            val to = (m shr 6) and 63; val promo = (m shr 12) and 7
            val cap = if (m shr 16 and F_EP != 0) P else pc[to] and 7
            sc[i] = when {
                m == ttm -> 30_000_000
                cap != 0 || promo == Q -> {
                    val base = SEEV[cap] * 16 + capHist[pc[m and 63]][to][cap] / 32 + (if (promo == Q) 8000 else 0)
                    if (see(m) >= 0) 20_000_000 + base else -10_000_000 + base
                }
                promo != 0 -> -20_000_000 + promo
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
            val d = d0
            ttm = (d and 0xFFFFF).toInt()
            val s = scoreFromTT(((d ushr 32) and 0xFFFF).toInt() - 32768, ply)
            val fl = ((d ushr 20) and 3).toInt()
            ttEval = ((d ushr 48) and 0xFFFF).toInt() - 32768
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
        scoreMoves(ply, n, ttm)
        var legalN = 0; var bestMove = 0
        val origAlpha = alpha
        for (k in 0 until n) {
            pick(ply, n, k)
            val m = bufs[ply][k]
            if (!inChk) {
                if (scr[ply][k] < 0) continue     // SEE < 0 (nước ăn thiệt) hoặc phong thấp
                val cap = if (m shr 16 and F_EP != 0) P else pc[(m shr 6) and 63] and 7
                if (((m shr 12) and 7) == 0 && stand + SEEV[cap] + 180 <= alpha) continue
            }
            make(m)
            if (attacked(kingSq[stm xor 1], stm)) { unmake(); continue }
            legalN++
            curMove[ply] = m; curPc[ply] = pc[(m shr 6) and 63]
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
            if (half >= 100 || insufficient()) return 0
            alpha = max(alpha, -MATE + ply); beta = min(beta, MATE - ply - 1)
            if (alpha >= beta) return alpha
        }
        val key = if (excluded != 0) hash xor (excluded.toLong() * -0x61c8864680b583ebL) else hash
        val ti = (key and ttMask.toLong()).toInt()
        var ttm = 0; var ttS = NONE; var ttD = -1; var ttF = UPPER; var ttEval = NONE
        val d0 = ttData[ti]
        val ttHit = (ttKey[ti] xor d0) == key
        if (ttHit) {
            val d = d0
            ttm = (d and 0xFFFFF).toInt()
            ttF = ((d ushr 20) and 3).toInt()
            ttD = ((d ushr 22) and 255).toInt() - 1
            ttS = scoreFromTT(((d ushr 32) and 0xFFFF).toInt() - 32768, ply)
            ttEval = ((d ushr 48) and 0xFFFF).toInt() - 32768
            if (!pv && ttD >= depth && (ttF == EXACT || (ttF == LOWER && ttS >= beta) || (ttF == UPPER && ttS <= alpha))) {
                if (ttm != 0 && ttS >= beta && isQuiet(ttm)) updateQuietBonus(ttm, ply, bonus(depth))
                return ttS
            }
        }
        var eval: Int
        if (inChk) { eval = NONE; sEval[ply] = NONE }
        else {
            eval = if (ttHit && ttEval != NONE && ttF != 3) ttEval else evaluate()
            sEval[ply] = eval
            if (ttHit && ttS != NONE && ((ttF == LOWER && ttS > eval) || (ttF == UPPER && ttS < eval) || ttF == EXACT)) eval = ttS
        }
        val improving = !inChk && ply >= 2 && sEval[ply - 2] != NONE && sEval[ply] > sEval[ply - 2]
        killers[ply + 1][0] = 0; killers[ply + 1][1] = 0

        if (!pv && !inChk && excluded == 0) {
            // reverse futility
            if (depth <= 8 && eval - (80 * depth - (if (improving) 60 else 0)) >= beta && eval < MATE - 500) return (eval + beta) / 2
            // razoring
            if (depth <= 3 && eval + 220 + 180 * depth <= alpha) {
                val q = qsearch(alpha, alpha + 1, ply)
                if (q <= alpha) return q
            }
            // null move
            if (depth >= 3 && eval >= beta && ply > 0 && curMove[ply - 1] != 0 && nonPawn(stm) && beta > -MATE + 500) {
                val r = 3 + depth / 3 + min(3, (eval - beta) / 200)
                curMove[ply] = 0; curPc[ply] = 0
                makeNull()
                val s = -search(depth - r, -beta, -beta + 1, ply + 1, !cut)
                unmakeNull()
                if (abort) return 0
                if (s >= beta) return if (s >= MATE - 500) beta else s
            }
        }
        // internal iterative reduction
        if (ttm == 0 && depth >= 4 && (pv || cut)) depth--

        // singular extension (tính trước khi sinh nước vì tìm kiếm loại trừ dùng chung bộ đệm của ply này)
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
            val to = (m shr 6) and 63
            val quiet = isQuiet(m)
            if (quiet && skipQuiets && m != ttm) continue
            val hscore = if (quiet) quietScore(m, ply) else 0
            // cắt tỉa ở độ sâu thấp
            if (ply > 0 && best > -MATE + 500 && nonPawn(us)) {
                val lmrD = max(0, depth - 1 - LMR[min(depth, 63)][min(legalN + 1, 63)])
                if (quiet) {
                    if (!inChk && legalN >= (3 + depth * depth) / (if (improving) 1 else 2)) { skipQuiets = true; continue }
                    if (!inChk && lmrD <= 6 && eval != NONE && eval + 120 + 100 * lmrD <= alpha) { skipQuiets = true; continue }
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
                        } else {
                            val cap = if (m shr 16 and F_EP != 0) P else pc[to] and 7
                            upd(capHist[pc[m and 63]][to], cap, b)
                        }
                        for (q in 0 until nCap) {
                            val cm = capsTried[ply][q]; val ct = (cm shr 6) and 63
                            val cc = if (cm shr 16 and F_EP != 0) P else pc[ct] and 7
                            upd(capHist[pc[cm and 63]][ct], cc, -b)
                        }
                        break
                    }
                }
            }
            if (quiet) { if (nQuiet < 64) quietsTried[ply][nQuiet++] = m }
            else if (nCap < 32) capsTried[ply][nCap++] = m
        }
        if (legalN == 0) return if (excluded != 0) alpha else if (inChk) -(MATE - ply) else 0
        if (excluded == 0) {
            ttStore(key, bestMove, scoreToTT(best, ply), if (sEval[ply] == NONE) 0 else sEval[ply], depth,
                if (best >= beta) LOWER else if (best > origAlpha) EXACT else UPPER)
        }
        return best
    }

    private fun isQuiet(m: Int): Boolean = pc[(m shr 6) and 63] == 0 && (m shr 12) and 7 == 0 && (m shr 16) and F_EP == 0

    /** Nước trong bảng băm có phải nước giả hợp lệ ở thế hiện tại không (tránh va chạm khóa). */
    private fun legalTT(m: Int): Boolean {
        val from = m and 63
        val p = pc[from]
        if (p == 0 || (p shr 3) != stm) return false
        val buf = bufs[MAXPLY + 1]; val n = gen(buf, false)
        for (i in 0 until n) if (buf[i] == m) return true
        return false
    }

    private fun updateQuietBonus(m: Int, ply: Int, b: Int) {
        val from = m and 63; val to = (m shr 6) and 63
        upd(hist[stm], from * 64 + to, b)
        val pidx = pc[from] * 64 + to
        val c1 = contIdx(ply, 1); if (c1 >= 0) { upd(cont[c1], pidx, b); if (b > 0) counter[c1] = m }
        val c2 = contIdx(ply, 2); if (c2 >= 0) upd(cont[c2], pidx, b)
        if (b > 0 && killers[ply][0] != m) { killers[ply][1] = killers[ply][0]; killers[ply][0] = m }
    }

    /** Danh sách nước hợp lệ ở thế hiện tại. */
    fun legalMoves(): IntArray {
        val buf = IntArray(256); val n = gen(buf, false)
        val out = IntArray(n); var c = 0
        for (i in 0 until n) if (legal(buf[i])) out[c++] = buf[i]
        return out.copyOf(c)
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
        for (c in 0..1) for (i in 0 until 4096) hist[c][i] = hist[c][i] / 2
        var root = legalMoves()
        if (allowed != null) { val f = root.filter { it in allowed }.toIntArray(); if (f.isNotEmpty()) root = f }
        if (root.isEmpty()) return RootResult(-1, 0, 0, 0)
        if (root.size == 1 && delta == 0) return RootResult(root[0], 0, 0, 0)
        val rs = IntArray(root.size) { -INF }
        // thứ tự ban đầu: nước băm, rồi ăn quân lớn
        val ti = (hash and ttMask.toLong()).toInt()
        val td = ttData[ti]
        val ttm = if ((ttKey[ti] xor td) == hash) (td and 0xFFFFF).toInt() else 0
        for (i in root.indices) { val m = root[i]; rs[i] = (if (m == ttm) 100000 else 0) + SEEV[pc[(m shr 6) and 63] and 7] }
        sortRoot(root, rs)
        var best = root[0]; var bestScore = 0; var completed = 0
        var stable = 0
        sEval[0] = if (inCheck()) NONE else evaluate()
        for (depth in 1..maxDepth) {
            if (helperId > 0 && depth > 2 && (depth + helperId) % 3 == 0) continue   // luồng phụ bỏ bớt độ sâu để đa dạng
            if (helperId > 0 && tt.stop) break
            rootDepth = depth
            nodeLimit = if (depth == 1) Long.MAX_VALUE else nl
            var window = if (depth >= 4) 18 else INF
            var alpha = if (depth >= 4) max(-INF, bestScore - window) else -INF
            var beta = if (depth >= 4) min(INF, bestScore + window) else INF
            var iterBest = -1; var iterScore = -INF
            while (true) {
                var a = alpha
                iterBest = -1; iterScore = -INF
                for (k in root.indices) {
                    val m = root[k]
                    make(m)
                    curMove[0] = m; curPc[0] = pc[(m shr 6) and 63]
                    val gives = inCheck()
                    val nd = depth - 1 + (if (gives) 1 else 0)
                    var s: Int
                    if (k == 0) s = -search(nd, -beta, -a, 1, false)
                    else {
                        var r = 0
                        if (depth >= 4 && k >= 3 && isQuietAfter(m) && !gives) r = min(nd - 1, LMR[min(depth, 63)][min(k + 1, 63)] / 2).coerceAtLeast(0)
                        s = -search(nd - r, -a - 1, -a, 1, true)
                        if (s > a && r > 0 && !abort) s = -search(nd, -a - 1, -a, 1, false)
                        if (s > a && s < beta && !abort) s = -search(nd, -beta, -a, 1, false)
                    }
                    unmake()
                    if (abort) break
                    rs[k] = if (s > a || k == 0) s else -INF + k   // nước không vượt alpha: giữ thứ tự cũ
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
            // xếp lại nước gốc: nước tốt nhất lên đầu, còn lại theo điểm
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
                make(m); curMove[0] = m; curPc[0] = pc[(m shr 6) and 63]
                val s = -search(d - 1, -thr, -thr + 1, 1, false)
                unmake()
                if (s >= thr) out.add(m)
            }
            near = out.toIntArray()
        }
        return RootResult(best, bestScore, completed, nodes, near)
    }

    private fun isQuietAfter(m: Int): Boolean = uCap[sp - 1] == 0 && (m shr 12) and 7 == 0

    private fun sortRoot(root: IntArray, rs: IntArray) {
        for (i in 1 until root.size) {
            val m = root[i]; val s = rs[i]; var j = i - 1
            while (j >= 0 && rs[j] < s) { root[j + 1] = root[j]; rs[j + 1] = rs[j]; j-- }
            root[j + 1] = m; rs[j + 1] = s
        }
    }

    /**
     * Tìm kiếm Lazy SMP: [threads] luồng cùng tìm trên một bảng băm; kết quả lấy từ luồng chính.
     * Chỉ dùng cho tìm kiếm theo thời gian (giới hạn nút / độ sâu phải chạy 1 luồng để kết quả cố định).
     */
    fun searchSmp(fen: String, moves: List<Int>, lim: SearchLimit, allowed: IntArray?, delta: Int, threads: Int): RootResult {
        load(fen, moves)
        if (threads <= 1 || lim.nodes > 0 || lim.depth > 0 || lim.timeMs <= 0) return search(lim, allowed, delta)
        while (helpers.size < threads - 1) helpers.add(ChessEngine(tt).also { it.helperId = helpers.size + 1 })
        for (h in helpers) { h.wMg = wMg; h.wEg = wEg }
        tt.stop = false
        val ths = (0 until threads - 1).map { k ->
            val h = helpers[k]
            h.load(fen, moves)
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

    /** Xóa bảng băm và bảng thứ tự nước (ván mới). */
    fun clear() {
        tt.clear(); evKey.fill(0)
        for (h in hist) h.fill(0)
        for (c in cont) c.fill(0)
        for (a in capHist) for (b in a) b.fill(0)
        counter.fill(0)
    }

    /** FEN rút gọn của thế hiện tại (để ghi dữ liệu tinh chỉnh). */
    fun fen(): String {
        val sb = StringBuilder()
        for (r in 7 downTo 0) {
            var empty = 0
            for (f in 0..7) {
                val p = pc[r * 8 + f]
                if (p == 0) { empty++; continue }
                if (empty > 0) { sb.append(empty); empty = 0 }
                val ch = " pnbrqk"[p and 7]
                sb.append(if (p shr 3 == 0) ch.uppercaseChar() else ch)
            }
            if (empty > 0) sb.append(empty)
            if (r > 0) sb.append('/')
        }
        sb.append(if (stm == 0) " w " else " b ")
        val c = (if (castling and 1 != 0) "K" else "") + (if (castling and 2 != 0) "Q" else "") + (if (castling and 4 != 0) "k" else "") + (if (castling and 8 != 0) "q" else "")
        sb.append(if (c.isEmpty()) "-" else c).append(' ')
        sb.append(if (ep >= 0) "" + ('a' + (ep and 7)) + ('1' + (ep shr 3)) else "-")
        sb.append(' ').append(half).append(" 1")
        return sb.toString()
    }

    /** Điểm tìm kiếm tĩnh (để lọc thế "yên" khi tạo dữ liệu tinh chỉnh). */
    fun quiescence(): Int { abort = false; nodeLimit = Long.MAX_VALUE; deadline = Long.MAX_VALUE; return qsearch(-INF, INF, 0) }

    fun makeMove(m: Int) = make(m)

    /** Thế hiện tại có dùng điểm dồn vua (không tinh chỉnh) hay không. */
    fun tracingMopup(): Boolean { evaluateFull(); return traceMopup }
}
