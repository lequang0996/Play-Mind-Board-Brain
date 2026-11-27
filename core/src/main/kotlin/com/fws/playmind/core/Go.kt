package com.fws.playmind.core

import kotlin.random.Random

/**
 * Bàn cờ vây dạng mảng có viền (cạnh n + 2) để kiểm tra láng giềng không cần xét biên. Dùng chung cho luật chơi và bot.
 * Ô trống = 0, đen = 1, trắng = 2, viền = 3.
 */
internal class GoBoard(val n: Int) {
    val w = n + 2
    val cells = IntArray(w * w)
    var ko = -1
    var hash = 0L
    private val stamp = IntArray(w * w)
    private var stampId = 0
    private val stack = IntArray(w * w)
    private val group = IntArray(w * w)
    private var groupSize = 0

    companion object {
        private val zob: Array<LongArray> = run {
            val r = java.util.Random(0x5EED_60L)
            Array(3) { LongArray(21 * 21) { r.nextLong() } }
        }
        fun opp(c: Int) = 3 - c
    }

    /** Danh sách ô trống duy trì tăng dần (để chọn nước ngẫu nhiên trong playout rất nhanh). */
    val emptyList = IntArray(n * n)
    private val emptyIdx = IntArray(w * w) { -1 }
    var emptyN = 0; private set

    init {
        for (i in 0 until w * w) {
            val r = i / w; val c = i % w
            if (r == 0 || c == 0 || r == w - 1 || c == w - 1) cells[i] = 3 else { emptyIdx[i] = emptyN; emptyList[emptyN++] = i }
        }
    }

    private fun addEmpty(p: Int) { emptyIdx[p] = emptyN; emptyList[emptyN++] = p }
    private fun delEmpty(p: Int) {
        val i = emptyIdx[p]; val last = emptyList[--emptyN]
        emptyList[i] = last; emptyIdx[last] = i; emptyIdx[p] = -1
    }
    /** Đổi chỗ hai phần tử trong danh sách ô trống (dùng khi loại tạm một ô khỏi cửa sổ chọn). */
    fun swapEmpty(i: Int, j: Int) {
        val a = emptyList[i]; val b = emptyList[j]
        emptyList[i] = b; emptyList[j] = a; emptyIdx[b] = i; emptyIdx[a] = j
    }

    /** Ô trong bàn (0..n*n-1) <-> chỉ số có viền. */
    fun pad(cell: Int) = (cell / n + 1) * w + cell % n + 1
    fun unpad(p: Int) = (p / w - 1) * n + p % w - 1

    fun copyFrom(o: GoBoard) {
        System.arraycopy(o.cells, 0, cells, 0, cells.size); ko = o.ko; hash = o.hash
        System.arraycopy(o.emptyList, 0, emptyList, 0, emptyList.size); System.arraycopy(o.emptyIdx, 0, emptyIdx, 0, emptyIdx.size); emptyN = o.emptyN
    }
    fun copy(): GoBoard { val b = GoBoard(n); b.copyFrom(this); return b }

    private val dirs = intArrayOf(1, -1, w, -w)
    private val d8 = intArrayOf(1, -1, w, -w, w + 1, w - 1, -w + 1, -w - 1)
    private val diag = intArrayOf(w + 1, w - 1, -w + 1, -w - 1)

    /** Duyệt nhóm quân chứa [p]; trả về số "khí" (ô trống kề, không đếm trùng). Các quân nằm trong [group] (số lượng [groupSize]). */
    private fun scan(p: Int): Int {
        val color = cells[p]
        stampId++
        var sp = 0; var libs = 0; groupSize = 0
        stack[sp++] = p; stamp[p] = stampId
        while (sp > 0) {
            val c = stack[--sp]
            group[groupSize++] = c
            for (d in dirs) {
                val q = c + d
                if (stamp[q] == stampId) continue
                val v = cells[q]
                if (v == 0) { stamp[q] = stampId; libs++ }
                else if (v == color) { stamp[q] = stampId; stack[sp++] = q }
            }
        }
        return libs
    }

    fun liberties(p: Int): Int = scan(p)

    private fun removeGroup() { for (i in 0 until groupSize) { val q = group[i]; hash = hash xor zob[cells[q]][q]; cells[q] = 0; addEmpty(q) } }

    /** Có đặt được quân [color] vào [p] không (ô trống, không phạm ko đơn, không tự sát)? Không thay đổi bàn. */
    fun legal(color: Int, p: Int): Boolean {
        if (cells[p] != 0 || p == ko) return false
        val o = opp(color)
        for (d in dirs) { if (cells[p + d] == 0) return true }
        for (d in dirs) {
            val q = p + d; val v = cells[q]
            if (v == o) { if (scan(q) == 1) return true }
            else if (v == color) { if (scan(q) >= 2) return true }
        }
        return false
    }

    /** Đặt quân và bắt quân. Trả về số quân bắt được, hoặc -1 nếu bất hợp lệ (bàn không đổi). */
    fun place(color: Int, p: Int): Int {
        if (!legal(color, p)) return -1
        val o = opp(color)
        cells[p] = color; hash = hash xor zob[color][p]; delEmpty(p)
        var captured = 0; var lastCap = -1
        for (d in dirs) {
            val q = p + d
            if (cells[q] == o && scan(q) == 0) { captured += groupSize; lastCap = group[0]; removeGroup() }
        }
        ko = -1
        if (captured == 1) {
            val libs = scan(p)
            if (groupSize == 1 && libs == 1) ko = lastCap
        }
        return captured
    }

    /** Điểm theo luật diện tích (Tromp-Taylor): quân trên bàn + vùng trống chỉ tiếp giáp một màu. Trả về (đen, trắng). */
    fun area(): IntArray {
        var b = 0; var wt = 0
        stampId++
        val mark = stampId
        for (r in 1..n) for (c in 1..n) {
            val p = r * w + c
            val v = cells[p]
            if (v == 1) b++ else if (v == 2) wt++
            else if (stamp[p] != mark) {
                var sp = 0; var size = 0; var touchB = false; var touchW = false
                stack[sp++] = p; stamp[p] = mark
                while (sp > 0) {
                    val x = stack[--sp]; size++
                    for (d in dirs) {
                        val q = x + d; val cv = cells[q]
                        if (cv == 0) { if (stamp[q] != mark) { stamp[q] = mark; stack[sp++] = q } }
                        else if (cv == 1) touchB = true else if (cv == 2) touchW = true
                    }
                }
                if (touchB && !touchW) b += size else if (touchW && !touchB) wt += size
            }
        }
        return intArrayOf(b, wt)
    }

    /** Điểm tựa của quân [color] ở [p] là "mắt thật" (không nên tự lấp): mọi láng giềng là quân mình/viền và đủ góc chéo. */
    fun ownEye(color: Int, p: Int): Boolean {
        for (d in dirs) { val v = cells[p + d]; if (v != color && v != 3) return false }
        var bad = 0; var edge = false
        for (d in diag) {
            val v = cells[p + d]
            if (v == 3) edge = true else if (v != color) bad++
        }
        return if (edge) bad == 0 else bad <= 1
    }

    /** Điểm nằm ở hàng biên (hàng 1). */
    fun isEdge(p: Int): Boolean { for (d in dirs) if (cells[p + d] == 3) return true; return false }

    /** Có quân nào trong 8 ô kề không? */
    fun stoneNear1(p: Int): Boolean { for (d in d8) if (cells[p + d] == 1 || cells[p + d] == 2) return true; return false }

    /** Có quân nào trong vòng 2 ô (khung 5x5) không? */
    fun stoneNear2(p: Int): Boolean {
        for (dr in -2..2) for (dc in -2..2) {
            if (dr == 0 && dc == 0) continue
            val q = p + dr * w + dc
            if (q in cells.indices && (cells[q] == 1 || cells[q] == 2)) return true
        }
        return false
    }

    fun emptyCount(): Int { var k = 0; for (r in 1..n) for (c in 1..n) if (cells[r * w + c] == 0) k++; return k }

    // ───── hỗ trợ chính sách playout ─────
    /** Tìm nước bắt quân / cứu quân quanh [last]: trả về chỉ số có viền hoặc -1. */
    fun tacticalMove(color: Int, last: Int, rnd: Random): Int {
        if (last < 0) return -1
        val o = opp(color)
        var save = -1
        // quân đối thủ kề nước vừa đi đang bị atari -> bắt; quân mình đang bị atari -> chạy
        for (d in dirs) {
            val q = last + d; val v = cells[q]
            if (v != o && v != color) continue
            if (scan(q) != 1) continue
            var lib = -1
            for (i in 0 until groupSize) { val s = group[i]; for (dd in dirs) if (cells[s + dd] == 0) lib = s + dd }
            if (lib < 0 || !legal(color, lib)) continue
            if (v == o) return lib
            save = lib
        }
        if (save >= 0 && !ownEye(color, save)) {
            // chạy thật sự chỉ khi sau khi đặt nhóm có ≥ 2 khí
            val b = copy(); b.place(color, save)
            if (b.liberties(save) >= 2) return save
        }
        // mẫu đơn giản: ô kề nước vừa đi (hay là ô sát cạnh/ô nối) có xác suất cao
        val start = rnd.nextInt(8)
        for (k in 0 until 8) {
            val d = d8[(start + k) % 8]
            val q = last + d
            if (cells[q] == 0 && !ownEye(color, q) && legal(color, q) && rnd.nextInt(100) < 18) return q
        }
        return -1
    }
}

/**
 * Cờ vây cho 2 người, bàn 9x9, 13x13 hoặc 19x19.
 *
 *  - Bên 0 = ĐEN (đi trước), bên 1 = TRẮNG. Luật diện tích (Tromp-Taylor): điểm = quân trên bàn + vùng trống chỉ tiếp giáp một màu.
 *    KHÔNG có bước gỡ quân chết: quân chết phải được bắt hẳn trước khi cả hai bỏ lượt. Komi 7,5 nên không có hòa.
 *  - Cấm tự sát, cấm lặp lại thế cờ cũ (superko theo vị trí). Hai lần bỏ lượt liên tiếp kết thúc ván.
 *
 * Mã nước đi: ô (0..n*n-1) hoặc [PASS].
 */
class GoGame(val size: Int = 9) : BoardGame, LeveledEngine {
    companion object {
        const val PASS = 511
        const val KOMI = 7.5
        @JvmStatic var lastSims = 0
    }

    init { require(size in intArrayOf(9, 13, 19)) }
    override val id = "go"
    internal val board = GoBoard(size)
    private val seen = HashSet<Long>()
    private val hist = ArrayList<Int>()
    private var passes = 0
    private var cur = 0
    private var captures = IntArray(2)
    override var result = Outcome.ONGOING; private set
    override var reason = EndReason.NONE; private set
    /** Điểm cuối (đen, trắng) khi ván kết thúc bằng 2 lần bỏ lượt. */
    var finalBlack = 0; private set
    var finalWhite = 0; private set

    init { seen.add(board.hash) }

    override val history: List<Int> get() = hist
    override val turn: Int get() = cur
    override val plies: Int get() = hist.size
    override val lastMove: Int get() = if (hist.isEmpty()) -1 else hist[hist.size - 1]
    override fun inCheck() = false
    fun stoneAt(cell: Int) = board.cells[board.pad(cell)]       // 0 trống, 1 đen, 2 trắng
    fun capturedBy(seat: Int) = captures[seat]                  // số quân đã bắt của bên [seat]
    /** Hai bên bỏ lượt liên tiếp ở nước vừa rồi? */
    val lastWasPass: Boolean get() = hist.isNotEmpty() && hist[hist.size - 1] == PASS

    override fun reset() {
        board.copyFrom(GoBoard(size))
        seen.clear(); seen.add(0L); hist.clear(); passes = 0; cur = 0; captures = IntArray(2)
        result = Outcome.ONGOING; reason = EndReason.NONE; finalBlack = 0; finalWhite = 0
    }

    /** Điểm tạm tính hiện tại theo luật diện tích: (đen, trắng). */
    fun areaScore(): IntArray = board.area()

    /** Nước [m] hợp lệ cho bên đang đi? (kể cả luật lặp thế) */
    fun isLegal(m: Int): Boolean {
        if (result != Outcome.ONGOING) return false
        if (m == PASS) return true
        if (m !in 0 until size * size) return false
        val p = board.pad(m); val color = cur + 1
        if (!board.legal(color, p)) return false
        val t = board.copy(); t.place(color, p)
        return t.hash !in seen
    }

    /**
     * Nước nên tránh theo BOT_GUIDE mục 8.2: tự lấp mắt thật của mình (bất biến cứng, mọi cấp), hoặc khi [selfAtari]
     * thì cả nước tự đưa nhóm của mình vào thế chỉ còn 1 khí (bộ lọc nhiễu của các cấp thấp). Bỏ lượt không tính.
     */
    fun isBlunder(m: Int, selfAtari: Boolean): Boolean {
        if (m == PASS) return false
        if (!isLegal(m)) return true
        val p = board.pad(m); val color = cur + 1
        if (board.ownEye(color, p)) return true
        if (!selfAtari) return false
        val t = board.copy()
        if (t.place(color, p) < 0) return true
        return t.liberties(p) <= 1
    }

    override fun legalMoves(): IntArray {
        if (result != Outcome.ONGOING) return IntArray(0)
        val out = ArrayList<Int>()
        for (c in 0 until size * size) if (isLegal(c)) out.add(c)
        out.add(PASS)
        return out.toIntArray()
    }

    override fun play(move: Int): Boolean {
        if (!isLegal(move)) return false
        if (move == PASS) {
            passes++; hist.add(PASS); cur = 1 - cur
            if (passes >= 2) endByScore()
            return true
        }
        passes = 0
        val cap = board.place(cur + 1, board.pad(move))
        captures[cur] += cap
        seen.add(board.hash); hist.add(move); cur = 1 - cur
        if (hist.size >= size * size * 3) endByScore()
        return true
    }

    private fun endByScore() {
        val a = board.area(); finalBlack = a[0]; finalWhite = a[1]
        reason = EndReason.TERRITORY
        result = if (a[0] > a[1] + KOMI) Outcome.WIN_FIRST else Outcome.WIN_SECOND
    }

    override fun forfeit(loser: Int, reason: EndReason) {
        if (result != Outcome.ONGOING) return
        this.reason = reason
        result = if (loser == 0) Outcome.WIN_SECOND else Outcome.WIN_FIRST
    }

    override fun copyGame(): GoGame {
        val g = GoGame(size)
        g.board.copyFrom(board); g.seen.clear(); g.seen.addAll(seen)
        g.hist.addAll(hist); g.passes = passes; g.cur = cur; g.captures = captures.copyOf()
        g.result = result; g.reason = reason; g.finalBlack = finalBlack; g.finalWhite = finalWhite
        return g
    }

    // ═════════════════════ Bot MCTS + RAVE ═════════════════════
    private class Node(val moves: IntArray, val player: Int) {
        val n = IntArray(moves.size); val w = FloatArray(moves.size)
        val rn = IntArray(moves.size); val rw = FloatArray(moves.size)
        val kids = arrayOfNulls<Node>(moves.size)
        var total = 0
    }

    /** Các nước ứng viên (chỉ số có viền) cho [color]: hợp lệ và không tự lấp mắt. */
    private fun candidates(b: GoBoard, color: Int): IntArray {
        val out = ArrayList<Int>(b.emptyN)
        for (i in 0 until b.emptyN) {
            val p = b.emptyList[i]
            if (b.isEdge(p) && !b.stoneNear2(p)) continue          // hàng biên khi chưa có quân gần: gần như luôn vô nghĩa
            if (b.legal(color, p) && !b.ownEye(color, p)) out.add(p)
        }
        return out.toIntArray()
    }

    override fun bestMove(timeMs: Long): Int = mcts(timeMs, 0)

    /**
     * 7 cấp cho bot MCTS dự phòng (khi không chạy được KataGo): giới hạn số lượt mô phỏng theo BotLevels.go.
     * Bất biến cứng ở mọi cấp (mục 8.2): ứng viên đã loại nước tự lấp mắt (candidates) và nước tự sát (luật).
     */
    override fun levelMove(level: BotLevel, timeMs: Long, rnd: Random): Int {
        val cfg = BotLevels.config(id, level, timeMs)
        return if (level == BotLevel.SUPREME) mcts(timeMs, 0) else mcts(0, cfg.extra)
    }

    private fun mcts(timeMs: Long, simLimit: Int): Int {
        if (result != Outcome.ONGOING) return -1
        // Khai cuộc: nước đầu vào thiên nguyên / sao; trắng chiếm thiên nguyên nếu còn trống (bàn nhỏ)
        if (hist.isEmpty()) return when (size) { 9 -> 40; 13 -> 84; else -> 3 * 19 + 15 }
        if (hist.size == 1 && hist[0] != PASS && size < 19) { val c = (size / 2) * size + size / 2; if (stoneAt(c) == 0) return c }
        val rnd = Random
        val color = cur + 1
        val root = board
        // Ứng viên gốc: lọc luật lặp thế thật sự
        val rootCands = candidates(root, color).filter { p ->
            val t = root.copy(); t.place(color, p); t.hash !in seen
        }.toIntArray()
        // Bỏ lượt khi: đối thủ vừa bỏ lượt và mình đang hơn theo điểm diện tích, hoặc không còn nước hữu ích
        val area = root.area()
        val myLead = (if (color == 1) area[0] - area[1] - KOMI else area[1] - area[0] + KOMI)
        if (rootCands.isEmpty()) return PASS
        if (lastWasPass && myLead > 0) return PASS
        val deadline = if (timeMs > 0) System.nanoTime() + timeMs * 1_000_000L else Long.MAX_VALUE
        // Song song hóa gốc: mỗi luồng một cây riêng, cuối cùng cộng dồn số lượt thăm / thắng của các nước ở gốc.
        val threads = minOf(Runtime.getRuntime().availableProcessors(), 4).coerceAtLeast(1)
        val perThread = if (simLimit > 0) (simLimit + threads - 1) / threads else Int.MAX_VALUE
        val totalN = IntArray(rootCands.size); val totalW = FloatArray(rootCands.size)
        var totalSims = 0
        val workers = ArrayList<Thread>()
        val lock = Any()
        for (t in 0 until threads) {
            val job = Runnable {
                val n = IntArray(rootCands.size); val w = FloatArray(rootCands.size)
                val sims = searchTree(root, rootCands, cur, deadline, perThread, n, w)
                synchronized(lock) { for (k in n.indices) { totalN[k] += n[k]; totalW[k] += w[k] }; totalSims += sims }
            }
            if (t == threads - 1) job.run() else { val th = Thread(job); th.isDaemon = true; th.start(); workers.add(th) }
        }
        for (th in workers) th.join()
        lastSims = totalSims
        var best = 0; var bestN = -1
        for (k in rootCands.indices) if (totalN[k] > bestN) { bestN = totalN[k]; best = k }
        if (bestN <= 0) return root.unpad(rootCands[rnd.nextInt(rootCands.size)])
        val wr = totalW[best] / bestN
        if (lastWasPass && wr > 0.9f) return PASS
        return root.unpad(rootCands[best])
    }

    /** Một luồng MCTS-RAVE: chạy tới [deadline], ghi thống kê các nước ở gốc vào [outN]/[outW], trả về số mô phỏng. */
    private fun searchTree(root: GoBoard, rootCands: IntArray, rootPlayer: Int, deadline: Long, simLimit: Int, outN: IntArray, outW: FloatArray): Int {
        val rnd = Random
        val rootNode = Node(rootCands, rootPlayer)
        val sim = GoBoard(size)
        val playedStep = Array(2) { IntArray((size + 2) * (size + 2)) }   // bước đi của từng bên theo ô có viền (-1 = chưa)
        val path = ArrayList<Node>(64); val pathIdx = ArrayList<Int>(64)
        var sims = 0
        val maxLen = size * size * 2
        while (true) {
            if (sims >= simLimit || ((sims and 15) == 0 && System.nanoTime() > deadline)) break
            sims++
            sim.copyFrom(root)
            path.clear(); pathIdx.clear()
            java.util.Arrays.fill(playedStep[0], -1); java.util.Arrays.fill(playedStep[1], -1)
            var node: Node? = rootNode
            var toPlay = rootPlayer     // 0 = đen, 1 = trắng
            var step = 0
            var last = -1
            var passesInRow = 0
            // ── chọn xuống cây ──
            while (node != null) {
                val idx = select(node)
                if (idx < 0) { break }
                val mv = node.moves[idx]
                if (sim.place(toPlay + 1, mv) < 0) {       // phát sinh do lặp thế ngầm: loại nước này khỏi nút
                    node.n[idx] = Int.MAX_VALUE / 2; node.w[idx] = 0f; continue
                }
                path.add(node); pathIdx.add(idx)
                playedStep[toPlay][mv] = step; step++
                last = mv; toPlay = 1 - toPlay
                val child = node.kids[idx]
                if (child == null) {
                    // chỉ mở rộng nút con khi nước này đã được thăm đủ nhiều (tiết kiệm bộ nhớ trên bàn lớn)
                    if (node.n[idx] >= 8) node.kids[idx] = Node(candidates(sim, toPlay + 1), toPlay)
                    break
                }
                node = child
            }
            // ── playout ──
            var len = 0
            while (len < maxLen && passesInRow < 2) {
                val c = toPlay + 1
                var mv = sim.tacticalMove(c, last, rnd)
                if (mv < 0) {
                    // chọn ngẫu nhiên trong danh sách ô trống; ô không dùng được thì loại khỏi cửa sổ chọn
                    var m = sim.emptyN
                    while (m > 0) {
                        val i = rnd.nextInt(m); val p = sim.emptyList[i]
                        if (!(sim.isEdge(p) && !sim.stoneNear1(p)) && !sim.ownEye(c, p) && sim.legal(c, p)) { mv = p; break }
                        sim.swapEmpty(i, m - 1); m--
                    }
                }
                if (mv < 0) { passesInRow++; last = -1 } else {
                    sim.place(c, mv); passesInRow = 0; last = mv
                    if (playedStep[toPlay][mv] < 0) playedStep[toPlay][mv] = step
                }
                toPlay = 1 - toPlay; step++; len++
            }
            val a = sim.area()
            val blackWins = a[0] > a[1] + KOMI
            // ── cập nhật thống kê (UCT + RAVE) ──
            for (i in path.indices) {
                val nd = path[i]; val ci = pathIdx[i]
                val win = if (blackWins == (nd.player == 0)) 1f else 0f
                nd.n[ci] = if (nd.n[ci] >= Int.MAX_VALUE / 4) nd.n[ci] else nd.n[ci] + 1
                nd.w[ci] += win; nd.total++
                // RAVE: các nước của cùng bên đi SAU thời điểm này trong ván mô phỏng
                val ps = playedStep[nd.player]
                for (k in nd.moves.indices) {
                    val s = ps[nd.moves[k]]
                    if (s > i) { nd.rn[k]++; nd.rw[k] += win }
                }
            }
        }
        for (k in rootNode.moves.indices) { val v = rootNode.n[k]; if (v < Int.MAX_VALUE / 4) { outN[k] = v; outW[k] = rootNode.w[k] } }
        return sims
    }

    private fun select(node: Node): Int {
        val m = node.moves.size
        if (m == 0) return -1
        var best = -1; var bestV = -1e9f
        val logT = Math.log((node.total + 1).toDouble()).toFloat()
        for (k in 0 until m) {
            val n = node.n[k]
            if (n >= Int.MAX_VALUE / 4) continue
            val v: Float
            if (n == 0 && node.rn[k] == 0) v = 10f + Random.nextFloat()          // chưa thử: ưu tiên
            else {
                val q = if (n > 0) node.w[k] / n else 0.5f
                val rq = if (node.rn[k] > 0) node.rw[k] / node.rn[k] else 0.5f
                val beta = node.rn[k].toFloat() / (n + node.rn[k] + 0.0025f * 4f * n * node.rn[k] + 1e-6f)
                v = (1 - beta) * q + beta * rq + 0.12f * Math.sqrt((logT / (n + 1)).toDouble()).toFloat()
            }
            if (v > bestV) { bestV = v; best = k }
        }
        return best
    }
}
