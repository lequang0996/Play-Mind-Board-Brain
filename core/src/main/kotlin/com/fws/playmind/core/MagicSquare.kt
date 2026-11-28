package com.fws.playmind.core

import kotlin.random.Random

enum class MagicStatus { PLAYING, WON }

/**
 * Ma phương 7×7 (game giải đố một người): điền đủ các số 1..49, mỗi số đúng một lần, sao cho mỗi hàng, mỗi cột và hai
 * đường chéo chính đều có tổng [TARGET] = 175. Ván bắt đầu với MỘT số cho sẵn ở một ô; mỗi ngày một đề (cùng đề cho mọi
 * người chơi, sinh từ số ngày), sang ngày hôm sau mới có đề mới.
 *
 * Luôn có lời giải: dùng họ ma phương "toàn chéo" dạng M[i][j] = 7·σ(L1) + τ(L2) + 1, với L1 = (i + b·j) mod 7,
 * L2 = (i + d·j) mod 7 và σ, τ là hoán vị bất kỳ của 0..6. Mỗi hàng, cột, đường chéo chứa mỗi ký hiệu của L1 và L2 đúng
 * một lần nên luôn có tổng 175; chọn σ, τ phù hợp thì đặt được bất kỳ số nào vào bất kỳ ô nào.
 *
 * Kết quả được chấp nhận theo luật (mọi bảng đúng luật đều thắng), không so với một lời giải cố định.
 * Gợi ý: tìm trong họ trên lời giải giữ đúng mọi ô đã khóa (số cho sẵn + gợi ý trước) và khớp nhiều ô người chơi đã điền
 * nhất, rồi điền một ô theo lời giải đó (ô được gợi ý bị khóa).
 */
class MagicSquareGame(val givenCell: Int, val givenValue: Int) {
    companion object {
        const val N = 7
        const val CELLS = 49
        const val TARGET = 175

        /** (b, d) của L1 = i + b·j, L2 = i + d·j: b, d ∉ {0, 1, 6} (đường chéo đủ ký hiệu) và b ≠ d (L1, L2 trực giao). */
        private val FAMILY: List<IntArray> = buildList {
            for (b in 2..5) for (d in 2..5) if (b != d) add(intArrayOf(b, d))
        }

        /** Đề của ngày [day] (số ngày kể từ 1/1/1970 theo giờ địa phương). */
        fun daily(day: Long): MagicSquareGame {
            val r = Random(day * 1_000_003L + 0x5A17L)
            return MagicSquareGame(r.nextInt(CELLS), 1 + r.nextInt(CELLS))
        }

        /** Bảng [cells] (0 = trống) có phải ma phương đầy đủ hợp lệ không. */
        fun isMagic(cells: IntArray): Boolean {
            if (cells.size != CELLS) return false
            val seen = BooleanArray(CELLS + 1)
            for (v in cells) { if (v !in 1..CELLS || seen[v]) return false; seen[v] = true }
            for (k in 0 until N) {
                var r = 0; var c = 0
                for (t in 0 until N) { r += cells[k * N + t]; c += cells[t * N + k] }
                if (r != TARGET || c != TARGET) return false
            }
            var d1 = 0; var d2 = 0
            for (t in 0 until N) { d1 += cells[t * N + t]; d2 += cells[t * N + (N - 1 - t)] }
            return d1 == TARGET && d2 == TARGET
        }

        /**
         * Một lời giải trong họ toàn chéo giữ đúng mọi cặp (ô, số) trong [must] và khớp nhiều cặp trong [prefer] nhất
         * (ưu tiên theo thứ tự). Trả null nếu không có thành viên nào của họ giữ được [must].
         */
        fun solve(must: List<Pair<Int, Int>>, prefer: List<Pair<Int, Int>>, rnd: Random): IntArray? {
            var best: IntArray? = null; var bestScore = -1
            for (f in FAMILY.shuffled(rnd)) {
                val b = f[0]; val d = f[1]
                val sg = IntArray(N) { -1 }; val sgInv = IntArray(N) { -1 }
                val ta = IntArray(N) { -1 }; val taInv = IntArray(N) { -1 }
                fun tryAssign(cell: Int, v: Int): Boolean {
                    val i = cell / N; val j = cell % N
                    val s1 = (i + b * j) % N; val s2 = (i + d * j) % N
                    val hi = (v - 1) / N; val lo = (v - 1) % N
                    val okHi = sg[s1] == hi || (sg[s1] == -1 && sgInv[hi] == -1)
                    val okLo = ta[s2] == lo || (ta[s2] == -1 && taInv[lo] == -1)
                    if (!okHi || !okLo) return false
                    sg[s1] = hi; sgInv[hi] = s1; ta[s2] = lo; taInv[lo] = s2
                    return true
                }
                if (!must.all { (c, v) -> tryAssign(c, v) }) continue
                var score = 0
                for ((c, v) in prefer) if (tryAssign(c, v)) score++
                if (score <= bestScore) continue
                // ký hiệu chưa gán: gán ngẫu nhiên các giá trị còn lại
                val freeHi = (0 until N).filter { sgInv[it] == -1 }.shuffled(rnd).iterator()
                val freeLo = (0 until N).filter { taInv[it] == -1 }.shuffled(rnd).iterator()
                for (s in 0 until N) { if (sg[s] == -1) sg[s] = freeHi.next(); if (ta[s] == -1) ta[s] = freeLo.next() }
                best = IntArray(CELLS) { cell ->
                    val i = cell / N; val j = cell % N
                    N * sg[(i + b * j) % N] + ta[(i + d * j) % N] + 1
                }
                bestScore = score
            }
            return best
        }

        /** Khôi phục ván đang chơi dở từ chuỗi [encode]. */
        fun decode(text: String): MagicSquareGame? = try {
            val parts = text.split(';')
            val g = MagicSquareGame(parts[0].toInt(), parts[1].toInt())
            val cs = parts[2].split(',').map { it.toInt() }
            val lk = parts[3]
            if (cs.size != CELLS || lk.length != CELLS) null else {
                for (i in 0 until CELLS) { g.cells[i] = cs[i]; g.locked[i] = lk[i] == '1' }
                g.cells[g.givenCell] = g.givenValue; g.locked[g.givenCell] = true
                g.hintsUsed = parts.getOrNull(4)?.toIntOrNull() ?: 0
                g.refresh(); g
            }
        } catch (_: Exception) { null }
    }

    val cells = IntArray(CELLS)
    /** Ô không sửa được: số cho sẵn và ô đã gợi ý. */
    val locked = BooleanArray(CELLS)
    /** Thứ tự người chơi điền (ô), dùng để ưu tiên giữ các ô điền sớm khi tìm lời giải cho gợi ý. */
    private val order = ArrayList<Int>()
    var hintsUsed = 0; private set
    var status = MagicStatus.PLAYING; private set

    init {
        require(givenCell in 0 until CELLS && givenValue in 1..CELLS)
        cells[givenCell] = givenValue; locked[givenCell] = true
    }

    fun isGiven(cell: Int) = cell == givenCell
    fun isLocked(cell: Int) = locked[cell]

    /** Số [v] đã có trên bàn chưa. */
    fun isUsed(v: Int) = cells.any { it == v }
    fun filledCount() = cells.count { it != 0 }

    fun rowSum(r: Int) = (0 until N).sumOf { cells[r * N + it] }
    fun colSum(c: Int) = (0 until N).sumOf { cells[it * N + c] }
    /** k = 0: chéo chính (trên trái → dưới phải); k = 1: chéo phụ. */
    fun diagSum(k: Int) = (0 until N).sumOf { t -> cells[t * N + if (k == 0) t else N - 1 - t] }
    fun rowFull(r: Int) = (0 until N).all { cells[r * N + it] != 0 }
    fun colFull(c: Int) = (0 until N).all { cells[it * N + c] != 0 }
    fun diagFull(k: Int) = (0 until N).all { t -> cells[t * N + if (k == 0) t else N - 1 - t] != 0 }

    /**
     * Đặt số [v] vào ô [cell]. Nếu số đó đang nằm ở ô khác (chưa khóa) thì được chuyển sang ô mới. Trả false nếu ô bị khóa
     * hoặc số đang nằm ở một ô đã khóa.
     */
    fun place(cell: Int, v: Int): Boolean {
        if (status != MagicStatus.PLAYING || cell !in 0 until CELLS || v !in 1..CELLS || locked[cell]) return false
        val other = cells.indexOf(v)
        if (other == cell) return true
        if (other >= 0) { if (locked[other]) return false; cells[other] = 0; order.remove(other) }
        cells[cell] = v; order.remove(cell); order.add(cell)
        refresh()
        return true
    }

    fun erase(cell: Int): Boolean {
        if (status != MagicStatus.PLAYING || cell !in 0 until CELLS || locked[cell] || cells[cell] == 0) return false
        cells[cell] = 0; order.remove(cell)
        return true
    }

    /**
     * Gợi ý một ô: ô [selected] nếu đang chọn một ô chưa khóa và khác lời giải, không thì một ô trống ngẫu nhiên, không thì
     * một ô khác lời giải. Ô được gợi ý bị khóa; số đó nếu đang nằm ở ô khác thì bị gỡ khỏi ô kia. Trả về ô đã gợi ý hoặc -1.
     */
    fun hint(selected: Int, rnd: Random): Int {
        if (status != MagicStatus.PLAYING) return -1
        val must = (0 until CELLS).filter { locked[it] }.map { it to cells[it] }
        val prefOrder = order.filter { !locked[it] && cells[it] != 0 } + (0 until CELLS).filter { !locked[it] && cells[it] != 0 && it !in order }
        val sol = solve(must, prefOrder.map { it to cells[it] }, rnd) ?: return -1
        val target = when {
            selected in 0 until CELLS && !locked[selected] && cells[selected] != sol[selected] -> selected
            else -> (0 until CELLS).filter { cells[it] == 0 }.randomOrNull(rnd)
                ?: (0 until CELLS).filter { !locked[it] && cells[it] != sol[it] }.randomOrNull(rnd) ?: return -1
        }
        val v = sol[target]
        val other = cells.indexOf(v)
        if (other >= 0 && other != target) { cells[other] = 0; order.remove(other) }
        cells[target] = v; locked[target] = true; order.remove(target)
        hintsUsed++
        refresh()
        return target
    }

    private fun refresh() { if (isMagic(cells)) status = MagicStatus.WON }

    /** Chuỗi lưu ván (ô cho sẵn; số cho sẵn; 49 ô; cờ khóa; số gợi ý đã dùng). */
    fun encode(): String = "$givenCell;$givenValue;${cells.joinToString(",")};${locked.joinToString("") { if (it) "1" else "0" }};$hintsUsed"
}
