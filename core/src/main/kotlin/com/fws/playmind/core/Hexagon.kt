package com.fws.playmind.core

import kotlin.random.Random

/**
 * Lục giác trí nhớ (2 người, ghi nhớ): 19 ô lục giác xếp hàng 3-4-5-4-3, mỗi ô có một số (mặt trước) và một chữ A..S (mặt sau).
 * Mỗi vòng: nhớ số trong 30 giây; các ô lật sang chữ và hiện số mục tiêu; tìm 3 ô liên tiếp trên một đường thẳng (ngang
 * hoặc hai đường chéo, 27 đường) có tổng bằng mục tiêu. Bấm chuông trước thì có 10 giây chọn đúng 3 ô (theo chữ).
 * Đúng +1, sai -1 (bộ đã gọi rồi, chọn sai, hoặc quá 10 giây). Bộ đã gọi không được đánh dấu: phải tự nhớ.
 * Vòng kết thúc khi tìm hết đáp án hoặc 90 giây không ai bấm chuông (đếm lại sau mỗi lần trả lời). Số đáp án không công bố.
 * 10 vòng, hòa thì đấu thêm vòng phụ (tối đa [MAX_EXTRA], hết vẫn hòa thì hòa).
 */
object Hexagon {
    const val CELLS = 19
    const val ROUNDS = 10
    const val MAX_EXTRA = 5
    const val MEMO_MS = 30_000L
    const val CALL_MS = 10_000L
    const val IDLE_MS = 90_000L
    const val REVEAL_MS = 4_000L
    val ROWS = intArrayOf(3, 4, 5, 4, 3)

    /** Tọa độ trục (q, r) của 19 ô theo thứ tự A..S (hàng từ trên xuống, trái sang phải). */
    val AX: Array<IntArray> = buildList {
        for (r in -2..2) for (q in -2..2) if (kotlin.math.abs(q + r) <= 2) add(intArrayOf(q, r))
    }.toTypedArray()

    /** 27 đường 3 ô liên tiếp (mặt nạ bit theo chỉ số ô). */
    val LINES: IntArray = run {
        val idx = HashMap<Long, Int>()
        AX.forEachIndexed { i, a -> idx[a[0] * 100L + a[1]] = i }
        val out = ArrayList<Int>()
        for ((i, a) in AX.withIndex()) for (d in arrayOf(intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(-1, 1))) {
            val b = idx[(a[0] + d[0]) * 100L + (a[1] + d[1])] ?: continue
            val c = idx[(a[0] + 2 * d[0]) * 100L + (a[1] + 2 * d[1])] ?: continue
            out.add((1 shl i) or (1 shl b) or (1 shl c))
        }
        out.toIntArray()
    }

    fun letter(i: Int) = ('A' + i).toString()
    fun cellsOf(mask: Int): IntArray = (0 until CELLS).filter { mask and (1 shl it) != 0 }.toIntArray()
    fun sum(values: IntArray, mask: Int): Int { var s = 0; for (i in 0 until CELLS) if (mask and (1 shl i) != 0) s += values[i]; return s }
    fun solutions(values: IntArray, target: Int): IntArray = LINES.filter { sum(values, it) == target }.toIntArray()

    /** Số ngẫu nhiên 1..6 và mục tiêu có từ 2 đến 5 đáp án (ưu tiên). */
    fun deal(rnd: Random): Pair<IntArray, Int> {
        repeat(200) {
            val v = IntArray(CELLS) { 1 + rnd.nextInt(6) }
            val counts = HashMap<Int, Int>()
            for (l in LINES) counts.merge(sum(v, l), 1, Int::plus)
            val ok = counts.filter { it.value in 2..5 }.keys.toList()
            if (ok.isNotEmpty()) return v to ok[rnd.nextInt(ok.size)]
        }
        val v = IntArray(CELLS) { 1 + rnd.nextInt(6) }
        return v to sum(v, LINES[0])
    }
}

enum class HexPhase { MEMO, FIND, CALL, REVEAL, OVER }

/**
 * Bot 7 cấp: nhớ đúng mỗi ô với xác suất [ACC]; ô nhớ sai lệch 1-2; thời gian tìm ra một bộ trung bình [FIND_MS];
 * nhớ được bộ đã gọi với xác suất [RECALL] (quên thì có thể gọi lặp và bị trừ điểm).
 */
class HexBot(val level: Int, private val rnd: Random) {
    companion object {
        // chỉnh trên máy (11/2026, 150 cặp mỗi cặp cấp): 6 cặp kề và 5 cặp cách hai cấp đều nằm trong khoảng đạt của cờ vua
        val ACC = doubleArrayOf(0.45, 0.60, 0.72, 0.82, 0.86, 0.90, 0.93)
        val FIND_MS = longArrayOf(34_000, 30_000, 22_000, 16_000, 13_500, 11_500, 10_000)
        val RECALL = doubleArrayOf(0.35, 0.60, 0.70, 0.80, 0.85, 0.90, 0.95)
        /** Cho phép bộ đo thử tham số: -Dpm.hex.<cấp>=acc,findMs,recall */
        private fun param(lv: Int): Triple<Double, Long, Double> {
            val p = System.getProperty("pm.hex.$lv")?.split(',')
            return if (p != null && p.size == 3) Triple(p[0].toDouble(), p[1].toLong(), p[2].toDouble()) else Triple(ACC[lv], FIND_MS[lv], RECALL[lv])
        }
    }
    private val prm = param(level.coerceIn(0, 6))
    private var belief = IntArray(Hexagon.CELLS)
    private val known = HashSet<Int>()
    var nextBuzzAt = Long.MAX_VALUE; private set

    fun memorize(values: IntArray) {
        known.clear()
        belief = IntArray(Hexagon.CELLS) { i ->
            if (rnd.nextDouble() < prm.first) values[i]
            else (values[i] + (if (rnd.nextBoolean()) 1 else -1) * (1 + rnd.nextInt(2))).coerceIn(1, 6)
        }
    }

    fun heard(mask: Int) { if (rnd.nextDouble() < prm.third) known.add(mask) }

    private fun candidates(target: Int) = Hexagon.LINES.filter { Hexagon.sum(belief, it) == target && it !in known }

    fun plan(now: Long, target: Int, foundCount: Int) {
        nextBuzzAt = if (candidates(target).isEmpty()) Long.MAX_VALUE
        else now + (prm.second * (0.4 + 1.2 * rnd.nextDouble()) * (1 + 0.15 * foundCount)).toLong()
    }

    fun answer(target: Int): Int { val c = candidates(target); return if (c.isEmpty()) 0 else c[rnd.nextInt(c.size)] }
}

/**
 * Một trận Lục giác trí nhớ. [bots][s] = cấp bot của ghế s, hoặc -1 nếu là người. Thời gian chạy bằng [tick] (ms).
 * Người: [buzz] rồi [pick] 3 ô. Bot tự bấm chuông theo kế hoạch; khi bot trả lời, bộ 3 ô hiện dần trong [botPickMs].
 */
class HexagonMatch(private val bots: IntArray, seed: Long, private val botPickMs: Long = 1500) {
    private val rnd = Random(seed)
    private val brains = Array(2) { s -> if (bots[s] >= 0) HexBot(bots[s], Random(seed * 31 + s)) else null }
    val scores = IntArray(2)
    var round = 0; private set
    var phase = HexPhase.MEMO; private set
    var now = 0L; private set
    var phaseEnds = 0L; private set
    var idleEnds = 0L; private set
    var values = IntArray(Hexagon.CELLS); private set
    var target = 0; private set
    var solutions = IntArray(0); private set
    val found = HashSet<Int>()
    val called = HashSet<Int>()
    var caller = -1; private set
    val picks = ArrayList<Int>()
    /** Lần trả lời gần nhất: ghế, mặt nạ, đúng/sai (để giao diện hiện thông báo). */
    var lastSeat = -1; private set
    var lastMask = 0; private set
    var lastOk = false; private set
    var answers = 0; private set
    var winner = -1; private set
    var draw = false; private set
    private var botAnswer = 0

    val totalRounds: Int get() = maxOf(Hexagon.ROUNDS, round)
    fun isBot(s: Int) = bots[s] >= 0

    init { startRound() }

    private fun startRound() {
        round++
        val (v, t) = Hexagon.deal(rnd)
        values = v; target = t; solutions = Hexagon.solutions(v, t)
        found.clear(); called.clear(); picks.clear(); caller = -1
        phase = HexPhase.MEMO; phaseEnds = now + Hexagon.MEMO_MS
        for (b in brains) b?.memorize(v)
    }

    private fun startFind() {
        phase = HexPhase.FIND; idleEnds = now + Hexagon.IDLE_MS
        for (b in brains) b?.plan(now, target, found.size)
    }

    fun tick(dt: Long) {
        if (phase == HexPhase.OVER) return
        now += dt
        when (phase) {
            HexPhase.MEMO -> if (now >= phaseEnds) startFind()
            HexPhase.FIND -> {
                // bot nào tới giờ bấm chuông trước thì giành quyền
                var s = -1; var t = Long.MAX_VALUE
                for (i in 0..1) brains[i]?.let { if (it.nextBuzzAt < t) { t = it.nextBuzzAt; s = i } }
                if (s >= 0 && now >= t) { buzz(s); botAnswer = brains[s]!!.answer(target) }
                else if (now >= idleEnds) endRound()
            }
            HexPhase.CALL -> {
                val b = brains[caller]
                if (b != null) {
                    // bot chọn dần 3 ô
                    val cells = Hexagon.cellsOf(botAnswer)
                    val elapsed = now - (phaseEnds - Hexagon.CALL_MS)
                    val want = if (cells.isEmpty()) 0 else ((elapsed * 3) / botPickMs).toInt().coerceAtMost(3)
                    while (picks.size < want && picks.size < cells.size) picks.add(cells[picks.size])
                    if (picks.size == 3 || (cells.isEmpty() && elapsed >= botPickMs)) judge()
                } else if (now >= phaseEnds) judge()
            }
            HexPhase.REVEAL -> if (now >= phaseEnds) nextRoundOrEnd()
            HexPhase.OVER -> {}
        }
    }

    fun buzz(seat: Int): Boolean {
        if (phase != HexPhase.FIND) return false
        caller = seat; picks.clear(); phase = HexPhase.CALL; phaseEnds = now + Hexagon.CALL_MS
        return true
    }

    /** Người đang gọi chạm một ô (chạm lại để bỏ). Đủ 3 ô thì chấm. */
    fun pick(seat: Int, cell: Int) {
        if (phase != HexPhase.CALL || seat != caller || brains[seat] != null || cell !in 0 until Hexagon.CELLS) return
        if (cell in picks) { picks.remove(cell); return }
        picks.add(cell)
        if (picks.size == 3) judge()
    }

    private fun judge() {
        var mask = 0; for (c in picks) mask = mask or (1 shl c)
        val ok = picks.size == 3 && mask in solutions && mask !in called
        if (picks.size == 3) called.add(mask)
        if (ok) { found.add(mask); scores[caller]++ } else scores[caller]--
        lastSeat = caller; lastMask = mask; lastOk = ok; answers++
        if (picks.size == 3) for (b in brains) b?.heard(mask)
        picks.clear(); caller = -1
        if (found.size >= solutions.size) endRound()
        else { phase = HexPhase.FIND; idleEnds = now + Hexagon.IDLE_MS; for (b in brains) b?.plan(now, target, found.size) }
    }

    private fun endRound() { phase = HexPhase.REVEAL; phaseEnds = now + Hexagon.REVEAL_MS }

    private fun nextRoundOrEnd() {
        if (round >= Hexagon.ROUNDS && scores[0] != scores[1]) { finish(); return }
        if (round >= Hexagon.ROUNDS + Hexagon.MAX_EXTRA) { finish(); return }
        startRound()
    }

    private fun finish() {
        phase = HexPhase.OVER
        if (scores[0] == scores[1]) draw = true else winner = if (scores[0] > scores[1]) 0 else 1
    }

    /** Người chơi bỏ cuộc. */
    fun resign(seat: Int) { if (phase != HexPhase.OVER) { phase = HexPhase.OVER; winner = 1 - seat } }

    /** Bot đấu bot tới hết trận (bộ đo). */
    fun simulate() { while (phase != HexPhase.OVER) tick(100) }
}
