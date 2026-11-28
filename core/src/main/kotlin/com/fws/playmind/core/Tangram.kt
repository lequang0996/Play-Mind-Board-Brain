package com.fws.playmind.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Xếp hình 7 mảnh (tangram, game giải đố một người).
 *
 * Bộ 7 mảnh chuẩn (đơn vị: cạnh góc vuông của tam giác nhỏ = 1, tổng diện tích 8): 2 tam giác lớn, 1 tam giác vừa,
 * 2 tam giác nhỏ, 1 hình vuông, 1 hình bình hành. Mảnh xoay theo bước 45°, hình bình hành lật được.
 *
 * Đề sinh NGẪU NHIÊN: ghép lần lượt 7 mảnh, mỗi mảnh áp một cạnh vào cạnh của mảnh đã đặt, không chồng nhau, hình cuối
 * không có lỗ thủng; hình xám người chơi thấy là hợp của các mảnh nên luôn giải được. Độ khó đo bằng độ "đặc"
 * ([TangramPuzzle.compactness] = diện tích / diện tích bao lồi): hình đặc (gần chữ nhật, tam giác) dễ, hình nhiều mỏm khó.
 * Kết quả chấp nhận MỌI cách ghép phủ kín đúng hình ([coverError]).
 */
object Tangram {
    const val PIECES = 7
    const val PUZZLES = 10
    const val SECONDS_PER_PUZZLE = 180

    /** Bộ 7 mảnh cổ điển theo loại: 0 tam giác lớn, 1 tam giác vừa, 2 tam giác nhỏ, 3 vuông, 4 bình hành. */
    val KINDS = intArrayOf(0, 0, 1, 2, 2, 3, 4)
    /** Diện tích từng loại mảnh. */
    val AREA = doubleArrayOf(2.0, 1.0, 0.5, 1.0, 1.0)

    /**
     * Bộ mảnh của 7 cấp (Dễ → Siêu việt): 4, 5, 6, 7 (bộ cổ điển), 8, 9, 10 mảnh.
     * Cấp trên bộ cổ điển thêm lần lượt tam giác nhỏ, tam giác vừa, hình vuông.
     */
    val LEVEL_KINDS: List<IntArray> = listOf(
        intArrayOf(0, 1, 2, 3),
        intArrayOf(0, 0, 1, 2, 3),
        intArrayOf(0, 0, 1, 2, 2, 3),
        intArrayOf(0, 0, 1, 2, 2, 3, 4),
        intArrayOf(0, 0, 1, 2, 2, 3, 4, 2),
        intArrayOf(0, 0, 1, 2, 2, 3, 4, 2, 1),
        intArrayOf(0, 0, 1, 2, 2, 3, 4, 2, 1, 3),
    )
    fun kindsFor(level: Int): IntArray = LEVEL_KINDS[level.coerceIn(0, LEVEL_KINDS.size - 1)]

    /** Khung tối đa của hình theo số mảnh (diện tích lớn thì khung lớn hơn). */
    fun maxSizeFor(kinds: IntArray): Double = 6.6 * kotlin.math.sqrt(kinds.sumOf { AREA[it] } / 8.0).coerceAtLeast(0.8)

    /** Đa giác gốc (ngược chiều kim đồng hồ) của từng loại. */
    private val BASE: Array<DoubleArray> = arrayOf(
        doubleArrayOf(0.0, 0.0, 2.0, 0.0, 0.0, 2.0),                 // tam giác lớn
        doubleArrayOf(0.0, 0.0, 2.0, 0.0, 1.0, 1.0),                 // tam giác vừa (cạnh huyền 2)
        doubleArrayOf(0.0, 0.0, 1.0, 0.0, 0.0, 1.0),                 // tam giác nhỏ
        doubleArrayOf(0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0),       // vuông
        doubleArrayOf(0.0, 0.0, 1.0, 0.0, 2.0, 1.0, 1.0, 1.0),       // bình hành
    )

    /** Trọng tâm (trung bình đỉnh) của đa giác gốc loại [kind]. */
    private fun center(kind: Int): Pair<Double, Double> {
        val b = BASE[kind]; val n = b.size / 2
        var x = 0.0; var y = 0.0
        for (i in 0 until n) { x += b[2 * i]; y += b[2 * i + 1] }
        return x / n to y / n
    }

    /**
     * Đỉnh của mảnh loại [kind] khi xoay [rot] × 45° quanh tâm, lật nếu [flip], rồi đặt tâm ở (cx, cy).
     * Trả về mảng x0, y0, x1, y1... luôn ngược chiều kim đồng hồ.
     */
    fun shape(kind: Int, rot: Int, flip: Boolean, cx: Double, cy: Double): DoubleArray {
        val b = BASE[kind]; val n = b.size / 2
        val (mx, my) = center(kind)
        val a = (((rot % 8) + 8) % 8) * PI / 4
        val c = cos(a); val s = sin(a)
        val out = DoubleArray(2 * n)
        for (i in 0 until n) {
            var x = b[2 * i] - mx; val y = b[2 * i + 1] - my
            if (flip) x = -x
            out[2 * i] = cx + x * c - y * s
            out[2 * i + 1] = cy + x * s + y * c
        }
        return if (flip) reverse(out) else out
    }

    private fun reverse(p: DoubleArray): DoubleArray {
        val n = p.size / 2
        return DoubleArray(p.size) { k -> val i = k / 2; val j = n - 1 - i; p[2 * j + (k and 1)] }
    }

    /** Điểm (x, y) nằm trong đa giác lồi ngược chiều kim đồng hồ [p] (kể cả biên trong sai số [eps]). */
    fun inside(p: DoubleArray, x: Double, y: Double, eps: Double = 1e-9): Boolean {
        val n = p.size / 2
        for (i in 0 until n) {
            val ax = p[2 * i]; val ay = p[2 * i + 1]
            val bx = p[2 * ((i + 1) % n)]; val by = p[2 * ((i + 1) % n) + 1]
            if ((bx - ax) * (y - ay) - (by - ay) * (x - ax) < -eps) return false
        }
        return true
    }

    /** Hai đa giác lồi chồng lên nhau với diện tích dương (chạm cạnh / chạm đỉnh không tính). */
    fun overlap(a: DoubleArray, b: DoubleArray, eps: Double = 1e-6): Boolean {
        for (poly in arrayOf(a, b)) {
            val n = poly.size / 2
            for (i in 0 until n) {
                val ex = poly[2 * ((i + 1) % n)] - poly[2 * i]; val ey = poly[2 * ((i + 1) % n) + 1] - poly[2 * i + 1]
                val nx = -ey; val ny = ex
                var minA = Double.MAX_VALUE; var maxA = -Double.MAX_VALUE; var minB = Double.MAX_VALUE; var maxB = -Double.MAX_VALUE
                for (k in 0 until a.size / 2) { val d = a[2 * k] * nx + a[2 * k + 1] * ny; if (d < minA) minA = d; if (d > maxA) maxA = d }
                for (k in 0 until b.size / 2) { val d = b[2 * k] * nx + b[2 * k + 1] * ny; if (d < minB) minB = d; if (d > maxB) maxB = d }
                if (maxA <= minB + eps || maxB <= minA + eps) return false
            }
        }
        return true
    }

    /** Một cách đặt mảnh: loại, góc xoay, lật, tâm. */
    class Placement(val kind: Int, val rot: Int, val flip: Boolean, val cx: Double, val cy: Double) {
        val poly: DoubleArray by lazy { shape(kind, rot, flip, cx, cy) }
    }

    /** Ghép ngẫu nhiên đủ 7 mảnh (null nếu lần thử này bị kẹt). */
    /**
     * Ghép ngẫu nhiên đủ 7 mảnh (null nếu bị kẹt). Mỗi mảnh thử nhiều chỗ áp cạnh hợp lệ; với xác suất [greed] chọn chỗ làm
     * bao lồi nhỏ nhất (hình gọn, dễ), còn lại chọn ngẫu nhiên (hình nhiều mỏm, khó).
     */
    private fun randomArrangement(rnd: Random, greed: Double, kinds: IntArray): List<Placement>? {
        val order = kinds.toMutableList().also { it.shuffle(rnd) }
        val placed = ArrayList<Placement>()
        placed.add(Placement(order[0], rnd.nextInt(8), order[0] == 4 && rnd.nextBoolean(), 0.0, 0.0))
        for (idx in 1 until order.size) {
            val kind = order[idx]
            val cands = ArrayList<Placement>()
            for (attempt in 0 until 160) {
                if (cands.size >= 24) break
                val host = placed[rnd.nextInt(placed.size)].poly
                val hn = host.size / 2
                val e = rnd.nextInt(hn)
                val ax = host[2 * e]; val ay = host[2 * e + 1]
                val bx = host[2 * ((e + 1) % hn)]; val by = host[2 * ((e + 1) % hn) + 1]
                val eLen = hypot(bx - ax, by - ay)
                val flip = kind == 4 && rnd.nextBoolean()
                val local = shape(kind, 0, flip, 0.0, 0.0)
                val ln = local.size / 2
                val f = rnd.nextInt(ln)
                val fx = local[2 * ((f + 1) % ln)] - local[2 * f]; val fy = local[2 * ((f + 1) % ln) + 1] - local[2 * f + 1]
                // xoay sao cho cạnh f ngược hướng cạnh e (hai đa giác cùng ngược chiều kim đồng hồ -> nằm hai bên cạnh)
                val need = atan2(by - ay, bx - ax) + PI - atan2(fy, fx)
                val rot = (((need / (PI / 4)).roundToInt() % 8) + 8) % 8
                val p0 = shape(kind, rot, flip, 0.0, 0.0)
                val sx = p0[2 * f]; val sy = p0[2 * f + 1]
                val tx = p0[2 * ((f + 1) % ln)]; val ty = p0[2 * ((f + 1) % ln) + 1]
                val fLen = hypot(tx - sx, ty - sy)
                // hai kiểu áp: đầu cạnh f trùng b, hoặc cuối cạnh f trùng a (cạnh ngắn hơn nằm gọn trên cạnh dài hơn khi có thể)
                val alignStart = rnd.nextBoolean()
                val (dx, dy) = if (alignStart) (bx - sx) to (by - sy) else (ax - tx) to (ay - ty)
                if (fLen > eLen + 1e-6 && rnd.nextInt(3) == 0) continue
                val cand = Placement(kind, rot, flip, dx, dy)
                if (placed.any { overlap(it.poly, cand.poly) }) continue
                cands.add(cand)
            }
            if (cands.isEmpty()) return null
            val pick = if (rnd.nextDouble() < greed) {
                val pts = placed.flatMap { pl -> (0 until pl.poly.size / 2).map { pl.poly[2 * it] to pl.poly[2 * it + 1] } }
                cands.minBy { c -> hull(pts + (0 until c.poly.size / 2).map { c.poly[2 * it] to c.poly[2 * it + 1] }) + rnd.nextDouble() * 0.05 }
            } else cands[rnd.nextInt(cands.size)]
            placed.add(pick)
        }
        return placed
    }

    /** Diện tích bao lồi của tập điểm. */
    fun hull(pts: List<Pair<Double, Double>>): Double {
        val s = pts.map { (Math.round(it.first * 1e6) / 1e6) to (Math.round(it.second * 1e6) / 1e6) }.distinct()
            .sortedWith(compareBy({ it.first }, { it.second }))
        if (s.size < 3) return 0.0
        fun cross(o: Pair<Double, Double>, a: Pair<Double, Double>, b: Pair<Double, Double>) =
            (a.first - o.first) * (b.second - o.second) - (a.second - o.second) * (b.first - o.first)
        val h = ArrayList<Pair<Double, Double>>()
        for (p in s) { while (h.size >= 2 && cross(h[h.size - 2], h[h.size - 1], p) <= 1e-9) h.removeAt(h.size - 1); h.add(p) }
        val lo = h.size + 1
        for (p in s.reversed().drop(1)) { while (h.size >= lo && cross(h[h.size - 2], h[h.size - 1], p) <= 1e-9) h.removeAt(h.size - 1); h.add(p) }
        h.removeAt(h.size - 1)
        var a = 0.0
        for (i in h.indices) { val p = h[i]; val q = h[(i + 1) % h.size]; a += p.first * q.second - q.first * p.second }
        return abs(a) / 2
    }

    /** Có lỗ thủng (vùng trống bị bao kín) không, kiểm bằng lưới điểm bước [h]. */
    private fun hasHole(ps: List<Placement>, h: Double = 0.1): Boolean {
        val xs = ps.flatMap { p -> (0 until p.poly.size / 2).map { p.poly[2 * it] } }
        val ys = ps.flatMap { p -> (0 until p.poly.size / 2).map { p.poly[2 * it + 1] } }
        val x0 = xs.min() - 2 * h; val y0 = ys.min() - 2 * h
        val w = ((xs.max() - x0) / h).toInt() + 3; val hh = ((ys.max() - y0) / h).toInt() + 3
        val cov = BooleanArray(w * hh) { i ->
            val x = x0 + (i % w + .5) * h; val y = y0 + (i / w + .5) * h
            ps.any { inside(it.poly, x, y, 1e-3) }
        }
        val seen = BooleanArray(w * hh); val st = ArrayDeque<Int>()
        st.add(0); seen[0] = true
        while (st.isNotEmpty()) {
            val c = st.removeLast(); val cx = c % w; val cy = c / w
            for ((dx, dy) in arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                val nx = cx + dx; val ny = cy + dy
                if (nx < 0 || ny < 0 || nx >= w || ny >= hh) continue
                val k = ny * w + nx
                if (!seen[k] && !cov[k]) { seen[k] = true; st.add(k) }
            }
        }
        return (0 until w * hh).any { !cov[it] && !seen[it] }
    }

    /**
     * Sinh [PUZZLES] đề tăng dần độ khó: mỗi đề chọn trong nhiều cách ghép ngẫu nhiên theo độ "đặc" (đề đầu đặc nhất).
     * Hình vừa khung [maxSize] × [maxSize] đơn vị.
     */
    fun session(rnd: Random, level: Int = 3): List<TangramPuzzle> = List(PUZZLES) { i -> puzzle(i / (PUZZLES - 1.0), rnd, kindsFor(level)) }

    /** Một đề với độ khó [difficulty] từ 0 (dễ) đến 1 (khó). */
    fun puzzle(difficulty: Double, rnd: Random, kinds: IntArray = KINDS, maxSize: Double = maxSizeFor(kinds)): TangramPuzzle {
        val cands = ArrayList<TangramPuzzle>()
        var tries = 0
        // dễ: gần như luôn chọn chỗ gọn nhất; khó: phần lớn ngẫu nhiên
        val greed = 0.97 - 0.75 * difficulty
        while (cands.size < 40 && tries++ < 2000) {
            val a = randomArrangement(rnd, greed, kinds) ?: continue
            val pz = TangramPuzzle(a)
            if (pz.width > maxSize || pz.height > maxSize) continue
            cands.add(pz)
        }
        cands.sortByDescending { it.compactness }
        // vị trí theo độ khó, kèm chút ngẫu nhiên; bỏ đề có lỗ thủng
        val base = ((difficulty * 0.85 + rnd.nextDouble() * 0.1) * (cands.size - 1)).roundToInt().coerceIn(0, cands.size - 1)
        for (d in 0 until cands.size) {
            for (k in intArrayOf(base + d, base - d)) {
                if (k !in cands.indices) continue
                if (!hasHole(cands[k].pieces)) return cands[k]
            }
        }
        return cands.first()
    }

    /**
     * Sai lệch giữa cách ghép [placed] (các đa giác) và hình đề [target]: tỉ lệ điểm lưới (bước [h]) bị sai — điểm trong hình
     * mà không có đúng một mảnh phủ, hoặc điểm ngoài hình mà có mảnh phủ — trên số điểm trong hình. 0 = khớp hoàn toàn.
     */
    fun coverError(placed: List<DoubleArray>, target: TangramPuzzle, h: Double = 0.05): Double {
        var inSil = 0; var bad = 0
        var y = target.minY - 0.5 + h / 2
        while (y < target.maxY + 0.5) {
            var x = target.minX - 0.5 + h / 2
            while (x < target.maxX + 0.5) {
                val s = target.pieces.any { inside(it.poly, x, y) }
                var c = 0
                for (p in placed) if (inside(p, x, y)) c++
                if (s) { inSil++; if (c != 1) bad++ } else if (c > 0) bad++
                x += h
            }
            y += h
        }
        // mảnh nằm ngoài khung đề cũng là sai: đếm đỉnh mảnh ở xa hình
        for (p in placed) for (k in 0 until p.size / 2) {
            if (p[2 * k] < target.minX - 0.6 || p[2 * k] > target.maxX + 0.6 || p[2 * k + 1] < target.minY - 0.6 || p[2 * k + 1] > target.maxY + 0.6) return 1.0
        }
        return if (inSil == 0) 1.0 else bad.toDouble() / inSil
    }

    /** Ngưỡng chấp nhận của [coverError] (sai số do điểm lưới nằm sát biên). */
    const val SOLVED_ERROR = 0.02

    /**
     * Hít mảnh [poly] vào điểm gần nhất trong [targets] (x0, y0, x1, y1...): trả về độ dịch (dx, dy) nếu có đỉnh của mảnh
     * cách một điểm đích dưới [radius], ngược lại (0, 0).
     */
    fun snap(poly: DoubleArray, targets: DoubleArray, radius: Double): Pair<Double, Double> {
        var best = radius; var dx = 0.0; var dy = 0.0
        for (i in 0 until poly.size / 2) for (j in 0 until targets.size / 2) {
            val ex = targets[2 * j] - poly[2 * i]; val ey = targets[2 * j + 1] - poly[2 * i + 1]
            val d = hypot(ex, ey)
            if (d < best) { best = d; dx = ex; dy = ey }
        }
        return dx to dy
    }
}

/** Một đề: cách ghép đáp án (đã dời để tâm khung bao ở gốc toạ độ). */
class TangramPuzzle(raw: List<Tangram.Placement>) {
    val pieces: List<Tangram.Placement>
    val minX: Double; val maxX: Double; val minY: Double; val maxY: Double
    val width: Double get() = maxX - minX
    val height: Double get() = maxY - minY
    /** Diện tích các mảnh / diện tích bao lồi: 1 = rất đặc (dễ), nhỏ = nhiều mỏm (khó). */
    val compactness: Double

    init {
        val xs = raw.flatMap { p -> (0 until p.poly.size / 2).map { p.poly[2 * it] } }
        val ys = raw.flatMap { p -> (0 until p.poly.size / 2).map { p.poly[2 * it + 1] } }
        val ox = (xs.min() + xs.max()) / 2; val oy = (ys.min() + ys.max()) / 2
        pieces = raw.map { Tangram.Placement(it.kind, it.rot, it.flip, it.cx - ox, it.cy - oy) }
        minX = xs.min() - ox; maxX = xs.max() - ox; minY = ys.min() - oy; maxY = ys.max() - oy
        val pts = pieces.flatMap { p -> (0 until p.poly.size / 2).map { p.poly[2 * it] to p.poly[2 * it + 1] } }
        compactness = raw.sumOf { Tangram.AREA[it.kind] } / Tangram.hull(pts)
    }

    /** Đỉnh nằm trên đường viền ngoài của hình (dùng làm điểm hít khi kéo mảnh), dạng x0, y0, x1, y1... */
    val outlineVertices: DoubleArray by lazy {
        val out = ArrayList<Double>()
        val seen = HashSet<Long>()
        for (p in pieces) for (k in 0 until p.poly.size / 2) {
            val x = p.poly[2 * k]; val y = p.poly[2 * k + 1]
            val key = (Math.round(x * 1000) shl 32) xor (Math.round(y * 1000) and 0xFFFFFFFFL)
            if (!seen.add(key)) continue
            // trên viền nếu có điểm sát quanh nó nằm ngoài mọi mảnh
            var outside = false
            for (a in 0 until 16) {
                val t = a * Math.PI / 8
                val sx = x + 0.04 * Math.cos(t); val sy = y + 0.04 * Math.sin(t)
                if (pieces.none { Tangram.inside(it.poly, sx, sy) }) { outside = true; break }
            }
            if (outside) { out.add(x); out.add(y) }
        }
        out.toDoubleArray()
    }
}
