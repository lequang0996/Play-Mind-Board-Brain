package com.fws.playmind.core

import kotlin.random.Random

/** Bốn mức độ của Sudoku. [clues] là số ô cho sẵn mục tiêu (bộ sinh đề dừng sớm nếu không thể bớt thêm mà vẫn giữ nghiệm duy nhất). */
enum class SudokuLevel(val clues: Int) { EASY(42), MEDIUM(34), HARD(29), EXPERT(24) }

/** Bộ giải bằng quay lui + bitmask, chọn ô ít khả năng nhất trước (MRV). Dùng để sinh đề và kiểm tra nghiệm duy nhất. */
internal class SudokuSolver {
    private val rows = IntArray(9); private val cols = IntArray(9); private val boxes = IntArray(9)
    private val g = IntArray(81)
    private var found = 0
    private var limit = 1
    private var keep: IntArray? = null

    private fun boxOf(i: Int) = (i / 27) * 3 + (i % 9) / 3

    private fun load(grid: IntArray): Boolean {
        rows.fill(0); cols.fill(0); boxes.fill(0)
        for (i in 0 until 81) {
            g[i] = grid[i]
            val d = grid[i]
            if (d == 0) continue
            val bit = 1 shl (d - 1)
            if ((rows[i / 9] and bit) != 0 || (cols[i % 9] and bit) != 0 || (boxes[boxOf(i)] and bit) != 0) return false
            rows[i / 9] = rows[i / 9] or bit; cols[i % 9] = cols[i % 9] or bit; boxes[boxOf(i)] = boxes[boxOf(i)] or bit
        }
        return true
    }

    /** Đếm nghiệm (dừng khi đạt [max]); nếu [out] khác null thì lưu nghiệm đầu tiên vào đó. [rnd] khác null: thử chữ số theo thứ tự ngẫu nhiên. */
    fun count(grid: IntArray, max: Int, out: IntArray? = null, rnd: Random? = null): Int {
        if (!load(grid)) return 0
        found = 0; limit = max; keep = out
        search(rnd)
        return found
    }

    private fun search(rnd: Random?) {
        var best = -1; var bestMask = 0; var bestCnt = 10
        for (i in 0 until 81) {
            if (g[i] != 0) continue
            val used = rows[i / 9] or cols[i % 9] or boxes[boxOf(i)]
            val mask = used.inv() and 0x1FF
            val c = Integer.bitCount(mask)
            if (c < bestCnt) { bestCnt = c; best = i; bestMask = mask; if (c <= 1) break }
        }
        if (best < 0) {
            found++
            if (found == 1) keep?.let { System.arraycopy(g, 0, it, 0, 81) }
            return
        }
        if (bestCnt == 0) return
        val digits = ArrayList<Int>(bestCnt)
        for (d in 1..9) if ((bestMask and (1 shl (d - 1))) != 0) digits.add(d)
        if (rnd != null) digits.shuffle(rnd)
        val r = best / 9; val c = best % 9; val b = boxOf(best)
        for (d in digits) {
            val bit = 1 shl (d - 1)
            g[best] = d; rows[r] = rows[r] or bit; cols[c] = cols[c] or bit; boxes[b] = boxes[b] or bit
            search(rnd)
            g[best] = 0; rows[r] = rows[r] and bit.inv(); cols[c] = cols[c] and bit.inv(); boxes[b] = boxes[b] and bit.inv()
            if (found >= limit) return
        }
    }
}

/** Sinh đề Sudoku có đúng một nghiệm. */
object SudokuGenerator {
    /**
     * Đề dễ/trung bình luôn giải được chỉ bằng "ô duy nhất" (không cần đoán); đề khó/rất khó thì không đảm bảo điều đó.
     * Trả về cặp (đề, lời giải).
     */
    fun generate(level: SudokuLevel, rnd: Random = Random): Pair<IntArray, IntArray> {
        var attempt = 0
        while (true) {
            attempt++
            val solution = IntArray(81)
            SudokuSolver().count(IntArray(81), 1, solution, rnd)
            val puzzle = solution.copyOf()
            val solver = SudokuSolver()
            // Bớt ô theo cặp đối xứng tâm cho đẹp mắt, giữ nguyên tắc nghiệm duy nhất.
            val order = (0..40).shuffled(rnd)
            var clues = 81
            for (i in order) {
                if (clues <= level.clues) break
                val j = 80 - i
                val a = puzzle[i]; val b = puzzle[j]
                puzzle[i] = 0; puzzle[j] = 0
                val removed = if (i == j) 1 else 2
                if (solver.count(puzzle, 2) == 1) clues -= removed else { puzzle[i] = a; puzzle[j] = b }
            }
            // Hai mức khó: thử bớt thêm từng ô lẻ (không đối xứng) để đạt gần số ô mục tiêu.
            if (level == SudokuLevel.HARD || level == SudokuLevel.EXPERT) {
                for (i in (0 until 81).shuffled(rnd)) {
                    if (clues <= level.clues) break
                    if (puzzle[i] == 0) continue
                    val a = puzzle[i]; puzzle[i] = 0
                    if (solver.count(puzzle, 2) == 1) clues-- else puzzle[i] = a
                }
            }
            val bySingles = solvableBySingles(puzzle)
            val ok = when (level) {
                SudokuLevel.EASY, SudokuLevel.MEDIUM -> bySingles       // luôn giải được không cần đoán
                SudokuLevel.EXPERT -> !bySingles                         // buộc phải dùng kỹ thuật cao hơn
                SudokuLevel.HARD -> true
            }
            if (ok || attempt >= 60) return puzzle to solution
        }
    }

    /** true nếu chỉ dùng "ô duy nhất" (naked/hidden single) là giải xong. */
    fun solvableBySingles(puzzle: IntArray): Boolean {
        val g = puzzle.copyOf()
        val cand = IntArray(81)
        fun peers(i: Int, f: (Int) -> Unit) {
            val r = i / 9; val c = i % 9
            for (k in 0 until 9) { if (k != c) f(r * 9 + k); if (k != r) f(k * 9 + c) }
            val br = r / 3 * 3; val bc = c / 3 * 3
            for (dr in 0 until 3) for (dc in 0 until 3) { val p = (br + dr) * 9 + bc + dc; if (p / 9 != r && p % 9 != c) f(p) }
        }
        for (i in 0 until 81) cand[i] = if (g[i] != 0) 0 else 0x1FF
        for (i in 0 until 81) if (g[i] != 0) { val bit = 1 shl (g[i] - 1); peers(i) { p -> cand[p] = cand[p] and bit.inv() } }
        var progress = true
        while (progress) {
            progress = false
            for (i in 0 until 81) {
                if (g[i] != 0) continue
                if (cand[i] == 0) return false
                if (Integer.bitCount(cand[i]) == 1) { place(g, cand, i, Integer.numberOfTrailingZeros(cand[i]) + 1, ::peers); progress = true }
            }
            if (progress) continue
            // ô duy nhất ẩn trong hàng / cột / khối
            for (unit in 0 until 27) {
                val cells = IntArray(9) { k ->
                    when (unit / 9) { 0 -> (unit % 9) * 9 + k; 1 -> k * 9 + (unit % 9); else -> ((unit % 9) / 3 * 3 + k / 3) * 9 + (unit % 9) % 3 * 3 + k % 3 }
                }
                for (d in 1..9) {
                    val bit = 1 shl (d - 1)
                    if (cells.any { g[it] == d }) continue
                    val spots = cells.filter { g[it] == 0 && (cand[it] and bit) != 0 }
                    if (spots.size == 1) { place(g, cand, spots[0], d, ::peers); progress = true }
                }
            }
        }
        return g.all { it != 0 }
    }

    private fun place(g: IntArray, cand: IntArray, i: Int, d: Int, peers: (Int, (Int) -> Unit) -> Unit) {
        g[i] = d; cand[i] = 0
        val bit = 1 shl (d - 1)
        peers(i) { p -> cand[p] = cand[p] and bit.inv() }
    }
}

enum class SudokuStatus { PLAYING, WON, LOST }

/** Kết quả của một thao tác điền số. */
enum class SudokuPlace { IGNORED, OK, MISTAKE }

/**
 * Một ván Sudoku: ô cho sẵn, ô người chơi điền, ghi chú nháp (bitmask bit0 = số 1 ... bit8 = số 9), đếm lỗi, gợi ý, hoàn tác.
 * Điền sai vẫn hiện (màu đỏ) để người chơi tự xóa; mỗi lần điền sai được đếm vào [mistakes] để hiện ở kết quả, nhưng không bao giờ làm thua ván.
 */
class SudokuGame(val level: SudokuLevel, puzzle: IntArray, val solution: IntArray) {
    companion object {
        const val MAX_HINTS = 3

        fun create(level: SudokuLevel, rnd: Random = Random): SudokuGame {
            val (p, s) = SudokuGenerator.generate(level, rnd)
            return SudokuGame(level, p, s)
        }
        fun peerCells(i: Int): IntArray {
            val r = i / 9; val c = i % 9; val out = LinkedHashSet<Int>()
            for (k in 0 until 9) { out.add(r * 9 + k); out.add(k * 9 + c) }
            val br = r / 3 * 3; val bc = c / 3 * 3
            for (dr in 0 until 3) for (dc in 0 until 3) out.add((br + dr) * 9 + bc + dc)
            out.remove(i)
            return out.toIntArray()
        }
    }

    val givens: IntArray = puzzle.copyOf()
    val cells: IntArray = puzzle.copyOf()
    val notes = IntArray(81)
    var mistakes = 0; private set
    var hintsUsed = 0; private set
    var status = SudokuStatus.PLAYING; private set

    private class Snap(val cells: IntArray, val notes: IntArray)
    private val undoStack = ArrayList<Snap>()
    val canUndo: Boolean get() = undoStack.isNotEmpty() && status == SudokuStatus.PLAYING
    val hintsLeft: Int get() = MAX_HINTS - hintsUsed

    fun isGiven(i: Int) = givens[i] != 0
    fun isWrong(i: Int) = cells[i] != 0 && cells[i] != solution[i]
    /** Số lần chữ số [d] đã được điền ĐÚNG trên bàn (kể cả ô cho sẵn). */
    fun correctCount(d: Int): Int { var n = 0; for (i in 0 until 81) if (cells[i] == d && solution[i] == d) n++; return n }

    private fun push() {
        undoStack.add(Snap(cells.copyOf(), notes.copyOf()))
        if (undoStack.size > 300) undoStack.removeAt(0)
    }

    private fun clearNotesAround(i: Int, d: Int) {
        val bit = 1 shl (d - 1)
        for (p in peerCells(i)) notes[p] = notes[p] and bit.inv()
    }

    private fun checkWin() {
        if (status != SudokuStatus.PLAYING) return
        for (i in 0 until 81) if (cells[i] != solution[i]) return
        status = SudokuStatus.WON
    }

    /** Điền chữ số [d] (1..9) vào ô [i]. */
    fun place(i: Int, d: Int): SudokuPlace {
        if (status != SudokuStatus.PLAYING || i !in 0..80 || d !in 1..9 || isGiven(i) || cells[i] == d) return SudokuPlace.IGNORED
        push()
        cells[i] = d; notes[i] = 0
        if (d == solution[i]) {
            clearNotesAround(i, d)
            checkWin()
            return SudokuPlace.OK
        }
        mistakes++
        return SudokuPlace.MISTAKE
    }

    /** Bật/tắt ghi chú nháp [d] ở ô trống [i]. */
    fun toggleNote(i: Int, d: Int): Boolean {
        if (status != SudokuStatus.PLAYING || i !in 0..80 || d !in 1..9 || isGiven(i) || cells[i] != 0) return false
        push()
        notes[i] = notes[i] xor (1 shl (d - 1))
        return true
    }

    fun erase(i: Int): Boolean {
        if (status != SudokuStatus.PLAYING || i !in 0..80 || isGiven(i) || (cells[i] == 0 && notes[i] == 0)) return false
        push()
        cells[i] = 0; notes[i] = 0
        return true
    }

    fun undo(): Boolean {
        if (!canUndo) return false
        val s = undoStack.removeAt(undoStack.size - 1)
        System.arraycopy(s.cells, 0, cells, 0, 81); System.arraycopy(s.notes, 0, notes, 0, 81)
        return true
    }

    /** Điền đúng một ô: ưu tiên ô đang chọn [preferred] nếu nó chưa đúng, nếu không thì ô trống đầu tiên. Trả về ô đã điền hoặc -1. */
    fun hint(preferred: Int = -1): Int {
        if (status != SudokuStatus.PLAYING || hintsUsed >= MAX_HINTS) return -1
        var target = -1
        if (preferred in 0..80 && !isGiven(preferred) && cells[preferred] != solution[preferred]) target = preferred
        if (target < 0) target = (0 until 81).firstOrNull { cells[it] != solution[it] } ?: return -1
        push()
        cells[target] = solution[target]; notes[target] = 0
        clearNotesAround(target, solution[target])
        hintsUsed++
        checkWin()
        return target
    }

    /** Đầu hàng / thoát giữa ván. */
    fun giveUp() { if (status == SudokuStatus.PLAYING) status = SudokuStatus.LOST }

    /** Các ô đang xung đột luật với ô [i] (cùng số ở hàng/cột/khối) - dùng để tô sáng. */
    fun conflictsWith(i: Int): List<Int> {
        val d = cells[i]; if (d == 0) return emptyList()
        return peerCells(i).filter { cells[it] == d }
    }

    /** Tiến độ 0..1 theo số ô đã điền đúng. */
    fun progress(): Float { var n = 0; for (i in 0 until 81) if (cells[i] == solution[i]) n++; return n / 81f }
}
