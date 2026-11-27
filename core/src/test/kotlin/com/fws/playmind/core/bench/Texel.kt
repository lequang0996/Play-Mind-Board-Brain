package com.fws.playmind.core.bench

import com.fws.playmind.core.*
import java.io.File
import java.util.stream.IntStream
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Tinh chỉnh Texel cho đánh giá tuyến tính của cờ vua ([CW]) và cờ tướng ([XW]); chạy trong CI bởi .github/workflows/tune.yml.
 *   datagen <game> <seed> <games> <nodes> <outFile>   tự đấu (giới hạn nút), ghi các thế "yên" dạng "fen|kết quả theo bên đi trước"
 *   tune    <game> <dataDir> <iters> <outFile> [maxPositions]   Adam toàn lô trên mô hình tuyến tính đúng như engine
 * Kết quả tune là chuỗi "mg,eg;mg,eg;..." để dán vào ChessWeightsData / XiangqiWeightsData.
 */
fun texelMain(args: List<String>) {
    when (args[0]) {
        "datagen" -> datagen(args[1], args[2].toInt(), args[3].toInt(), args[4].toLong(), File(args[5]))
        "tune" -> tune(args[1], File(args[2]), args[3].toInt(), File(args[4]), args.getOrNull(5)?.toInt() ?: Int.MAX_VALUE)
        else -> error("texel datagen|tune ...")
    }
}

/** Bộ chuyển đổi chung cho hai engine. */
private interface TexelEngine {
    val size: Int; val phMax: Int; val mg: IntArray; val eg: IntArray
    fun setFen(fen: String)
    /** Điểm theo góc nhìn bên đi trước (trắng/đỏ), null nếu thế không dùng được (vd. có điểm dồn vua). */
    fun evalTraced(): Int?
    val trace: IntArray; val phase: Int; val scale: Int
}

private class ChessTexel : TexelEngine {
    val e = ChessEngine(10).also { it.tracing = true }
    override val size = CW.SIZE; override val phMax = 24; override val mg = CW.MG; override val eg = CW.EG
    override fun setFen(fen: String) = e.setFen(fen)
    override fun evalTraced(): Int? { val v = e.evaluate(); if (e.traceMopup) return null; return if (e.stm == 0) v else -v }
    override val trace get() = e.trace
    override val phase get() = e.tracePhase
    override val scale get() = e.traceScale
}

private class XqTexel : TexelEngine {
    val e = XiangqiEngine(10).also { it.tracing = true }
    override val size = XW.SIZE; override val phMax = XW.PHMAX; override val mg = XW.MG; override val eg = XW.EG
    override fun setFen(fen: String) = e.setFen(fen)
    override fun evalTraced(): Int? { val v = e.evaluate(); return if (e.stm == 0) v else -v }
    override val trace get() = e.trace
    override val phase get() = e.tracePhase
    override val scale get() = e.traceScale
}

private fun datagen(game: String, seed: Int, games: Int, nodes: Long, out: File) {
    val rnd = Random(seed)
    out.parentFile?.mkdirs()
    out.bufferedWriter().use { w ->
        if (game == "chess") {
            val eng = ChessEngine(18); val probe = ChessEngine(14)
            for (gi in 0 until games) {
                val g = ChessGame()
                val op = OPENINGS[rnd.nextInt(OPENINGS.size)].split(" ")
                for (s in op.take(2 + rnd.nextInt(op.size - 1))) applyUci(g, s)
                repeat(rnd.nextInt(4)) { if (g.result == Outcome.ONGOING) { val l = g.legalMoves(); g.play(l[rnd.nextInt(l.size)]) } }
                eng.clear()
                val fens = ArrayList<String>()
                var result = -1.0; var winStreak = 0; var drawStreak = 0
                while (g.result == Outcome.ONGOING && g.plies < 400) {
                    eng.load(ChessEngine.START_FEN, g.history)
                    val r = eng.search(SearchLimit(nodes = nodes), null, 0)
                    val ws = if (g.turn == 0) r.score else -r.score
                    probe.load(ChessEngine.START_FEN, g.history)
                    if (!probe.inCheck() && abs(r.score) < 2000) {
                        val st = probe.evaluate(); val q = probe.quiescence()
                        if (st == q && !probe.tracingMopup()) fens.add(probe.fen())
                    }
                    if (abs(ws) >= 1000) winStreak++ else winStreak = 0
                    if (g.plies > 140 && abs(ws) <= 15) drawStreak++ else drawStreak = 0
                    if (winStreak >= 6) { result = if (ws > 0) 1.0 else 0.0; break }
                    if (drawStreak >= 12) { result = 0.5; break }
                    g.play(g.findMove(ChessGame.fromSq(r.best), ChessGame.toSq(r.best), ChessGame.promoOf(r.best)))
                }
                if (result < 0) result = finalResult(g)
                for (f in fens) w.write("$f|$result\n")
                if (gi % 20 == 0) w.flush()
            }
        } else {
            val eng = XiangqiEngine(18); val probe = XiangqiEngine(14)
            val start = IntArray(90).also { val g = XiangqiGame(); for (i in 0 until 90) it[i] = g.pieceAt(i) }
            for (gi in 0 until games) {
                val g = XiangqiGame()
                repeat(2 + rnd.nextInt(7)) { if (g.result == Outcome.ONGOING) { val l = g.legalMoves(); g.play(l[rnd.nextInt(l.size)]) } }
                eng.clear()
                val fens = ArrayList<String>()
                var result = -1.0; var winStreak = 0; var drawStreak = 0
                while (g.result == Outcome.ONGOING && g.plies < 400) {
                    eng.load(start, true, g.history)
                    val r = eng.search(SearchLimit(nodes = nodes), null, 0)
                    if (r.best < 0) break
                    val ws = if (g.turn == 0) r.score else -r.score
                    probe.load(start, true, g.history)
                    if (!probe.inCheck() && abs(r.score) < 2000) {
                        val st = probe.evaluate(); val q = probe.quiescence()
                        if (st == q) fens.add(probe.fen())
                    }
                    if (abs(ws) >= 1200) winStreak++ else winStreak = 0
                    if (g.plies > 160 && abs(ws) <= 15) drawStreak++ else drawStreak = 0
                    if (winStreak >= 6) { result = if (ws > 0) 1.0 else 0.0; break }
                    if (drawStreak >= 12) { result = 0.5; break }
                    g.play(r.best)
                }
                if (result < 0) result = finalResult(g)
                for (f in fens) w.write("$f|$result\n")
                if (gi % 20 == 0) w.flush()
            }
        }
    }
}

/** Kết quả theo bên đi trước; thắng nhờ luật lặp thế của app không phản ánh thế cờ nên tính hòa. */
private fun finalResult(g: BoardGame) = when {
    g.result == Outcome.WIN_FIRST && g.reason != EndReason.REPETITION -> 1.0
    g.result == Outcome.WIN_SECOND && g.reason != EndReason.REPETITION -> 0.0
    else -> 0.5
}

private fun tune(game: String, dir: File, iters: Int, outFile: File, maxPos: Int) {
    val te: TexelEngine = if (game == "chess") ChessTexel() else XqTexel()
    val size = te.size; val phMax = te.phMax
    val seen = HashSet<String>()
    val fens = ArrayList<String>(); val res = ArrayList<Double>()
    for (f in dir.walkTopDown().filter { it.isFile && it.name.endsWith(".txt") }.sortedBy { it.path }) {
        f.forEachLine { line ->
            val i = line.lastIndexOf('|'); if (i < 0) return@forEachLine
            val fen = line.substring(0, i)
            val r = line.substring(i + 1).toDoubleOrNull() ?: return@forEachLine
            val key = if (game == "chess") fen.substringBeforeLast(' ').substringBeforeLast(' ') else fen
            if (fens.size < maxPos && seen.add(key)) { fens.add(fen); res.add(r) }
        }
    }
    seen.clear()
    val n0 = fens.size
    println("positions: $n0, params: $size")
    val off = IntArray(n0 + 1); val ph = IntArray(n0); val sc = IntArray(n0); val y = DoubleArray(n0)
    val idxL = ArrayList<ShortArray>(n0); val cofL = ArrayList<ByteArray>(n0)
    var n = 0; var mismatch = 0; var skipped = 0
    for (p in 0 until n0) {
        val ok = runCatching { te.setFen(fens[p]); te.evalTraced() }.getOrNull()
        if (ok == null) { skipped++; continue }
        val tr = te.trace
        val ix = ArrayList<Short>(); val cf = ArrayList<Byte>()
        for (k in 0 until size) { val c = tr[k]; if (c != 0) { ix.add(k.toShort()); cf.add(c.coerceIn(-127, 127).toByte()) } }
        var mg = 0; var eg = 0
        for (k in ix.indices) { mg += te.mg[ix[k].toInt()] * cf[k]; eg += te.eg[ix[k].toInt()] * cf[k] }
        val lin = (mg * te.phase + eg * (phMax - te.phase)) / phMax * te.scale / 64
        if (lin != ok) mismatch++
        idxL.add(ix.toShortArray()); cofL.add(cf.toByteArray())
        ph[n] = te.phase; sc[n] = te.scale; y[n] = res[p]
        off[n + 1] = off[n] + ix.size
        n++
    }
    fens.clear()
    println("used: $n, skipped: $skipped, linear-model mismatches: $mismatch")
    val IDX = ShortArray(off[n]); val COF = ByteArray(off[n])
    for (p in 0 until n) { System.arraycopy(idxL[p], 0, IDX, off[p], idxL[p].size); System.arraycopy(cofL[p], 0, COF, off[p], cofL[p].size) }
    idxL.clear(); cofL.clear()

    val w = DoubleArray(2 * size) { if (it < size) te.mg[it].toDouble() else te.eg[it - size].toDouble() }
    fun evalP(p: Int): Double {
        var mg = 0.0; var eg = 0.0
        for (k in off[p] until off[p + 1]) { val i = IDX[k].toInt(); val c = COF[k].toDouble(); mg += w[i] * c; eg += w[size + i] * c }
        return (mg * ph[p] + eg * (phMax - ph[p])) / phMax * sc[p] / 64.0
    }
    fun sig(k: Double, e: Double) = 1.0 / (1.0 + 10.0.pow(-k * e / 400.0))
    fun error(k: Double): Double = IntStream.range(0, n).parallel().mapToDouble { p -> val d = y[p] - sig(k, evalP(p)); d * d }.sum() / n
    var lo = 0.2; var hi = 3.0
    repeat(40) { val m1 = lo + (hi - lo) / 3; val m2 = hi - (hi - lo) / 3; if (error(m1) < error(m2)) hi = m2 else lo = m1 }
    val K = (lo + hi) / 2
    println("K = %.4f, start error = %.6f".format(K, error(K)))
    val m = DoubleArray(2 * size); val v = DoubleArray(2 * size)
    val lr = 1.0; val b1 = 0.9; val b2 = 0.999
    val threads = Runtime.getRuntime().availableProcessors()
    val chunk = (n + threads - 1) / threads
    for (it in 1..iters) {
        val grads = Array(threads) { DoubleArray(2 * size) }
        IntStream.range(0, threads).parallel().forEach { t ->
            val g = grads[t]; val a = t * chunk; val b = minOf(n, a + chunk)
            for (p in a until b) {
                val s = sig(K, evalP(p))
                val dE = (s - y[p]) * s * (1 - s) * ln(10.0) * K / 400.0
                val fm = dE * ph[p] / phMax * sc[p] / 64.0
                val fe = dE * (phMax - ph[p]) / phMax * sc[p] / 64.0
                for (k in off[p] until off[p + 1]) { val i = IDX[k].toInt(); val c = COF[k].toDouble(); g[i] += fm * c; g[size + i] += fe * c }
            }
        }
        for (j in 0 until 2 * size) {
            var g = 0.0; for (t in 0 until threads) g += grads[t][j]
            g = 2.0 * g / n
            m[j] = b1 * m[j] + (1 - b1) * g; v[j] = b2 * v[j] + (1 - b2) * g * g
            w[j] -= lr * (m[j] / (1 - b1.pow(it))) / (sqrt(v[j] / (1 - b2.pow(it))) + 1e-8)
        }
        if (it % 100 == 0 || it == iters) {
            println("iter $it error = %.6f".format(error(K)))
            outFile.parentFile?.mkdirs()
            outFile.writeText((0 until size).joinToString(";") { "${Math.round(w[it])},${Math.round(w[size + it])}" })
        }
    }
}
