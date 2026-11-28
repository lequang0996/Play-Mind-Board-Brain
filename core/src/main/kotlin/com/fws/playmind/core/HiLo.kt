package com.fws.playmind.core

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/** Game có nước "tự động" khi hết giờ thay vì xử thua cả ván (Lãnh địa đồng cỏ, Công thức cao thấp). */
interface AutoOnTimeout {
    /** Nước đi thay người hết giờ ở bước hiện tại, -1 nếu không có. */
    fun timeoutMove(): Int
}

enum class HiLoPhase { DEAL, DISCARD, BET, DECLARE, BUILD, OVER }

/** Kết quả một ván bài để hiện cho người chơi. [high]/[low]: giá trị biểu thức (NaN nếu không có). */
class HiLoShowdown(
    val hand: Int, val sides: IntArray, val high: DoubleArray, val low: DoubleArray, val exprs: IntArray,
    val failed: BooleanArray, val folded: BooleanArray, val payout: IntArray, val highWinner: Int, val lowWinner: Int,
    val uncontested: Boolean, val nums: List<IntArray>, val ops: List<IntArray>, val sqrtN: IntArray,
)

/**
 * Công thức cao thấp: 2-8 người, mỗi người 50 chip. Mỗi ván bài, ghép 4 thẻ số và 3 thẻ phép toán (+ − ÷, có thể đổi một thẻ
 * lấy ×) thành một biểu thức, cộng thêm √ cho từng số nếu có thẻ √; cửa High thắng khi kết quả gần 20 nhất, cửa Low gần 1 nhất.
 *
 * Bộ bài: thẻ số 0..10 ở 4 chất (vàng > bạc > đồng > đen, dùng xét hòa) = 44 thẻ, cùng 4 thẻ × và 4 thẻ √.
 * Một ván (theo Devil's Plan mùa 1): góp ante; mỗi người 1 thẻ số úp và 1 thẻ ngửa; vòng cược 1; thêm 2 thẻ ngửa; vòng cược 2; chọn cửa kín
 * (High / Low / Swing); lập biểu thức (Swing: hai biểu thức, phải thắng cả hai cửa); lật bài, chia pot (mỗi cửa một nửa).
 * Rút trúng thẻ đặc biệt khi chia thẻ ngửa: giữ thẻ đó và rút tiếp tới khi được thẻ số. Thẻ ×: bắt buộc bỏ một trong + − ÷.
 * Tính theo thứ tự thông thường (× ÷ trước + −), không có ngoặc; chia cho 0 là biểu thức không hợp lệ.
 * Hòa khoảng cách: High xét thẻ số lớn nhất, Low xét thẻ số nhỏ nhất (cùng số thì xét chất).
 * Hết tiền thì ra khỏi bàn; còn một người thì người đó thắng. Ante tăng dần theo số ván để ván đấu luôn kết thúc.
 *
 * Mã nước đi: biểu thức = ((hoán vị 4 số) × 6 + (hoán vị 3 phép)) × 16 + mặt nạ √ (< 2304); Swing = cao × 2304 + thấp;
 * [FOLD], [CALL] (theo / xem), [RAISE]+n (tố thêm n), [DISCARD]+phép, [DECLARE]+cửa, [EXPR_FAIL], [DEAL]+hạt giống chia bài.
 * Chia bài là một nước của người chia (hạt giống ngẫu nhiên) để mọi máy trong phòng online có cùng bộ bài.
 */
class HiLoGame(val players: Int = 8) : BoardGame, LeveledEngine, AutoOnTimeout {
    companion object {
        const val START_CHIPS = 50
        const val HIGH = 20.0
        const val LOW = 1.0
        const val NUMS = 44
        const val MUL_CARD = 44          // 44..47
        const val SQRT_CARD = 48         // 48..51
        const val CARDS = 52
        const val OP_ADD = 0; const val OP_SUB = 1; const val OP_DIV = 2; const val OP_MUL = 3
        const val SIDE_HIGH = 0; const val SIDE_LOW = 1; const val SIDE_SWING = 2
        const val CODES = 2304
        const val FOLD = 6_000_000
        const val CALL = 6_000_001
        const val RAISE = 6_000_100      // + số chip tố thêm (1..899)
        const val DISCARD = 6_001_000    // + phép bỏ đi (0..2)
        const val DECLARE = 6_002_000    // + cửa
        const val EXPR_FAIL = 6_003_000
        const val DEAL = 7_000_000       // + hạt giống 0..999999
        const val MAX_RAISES = 3
        const val MAX_HANDS = 300

        fun value(card: Int) = card % 11
        fun suit(card: Int) = card / 11
        fun isNumber(card: Int) = card < NUMS
        /** Ante gấp đôi sau mỗi 4 ván bài (1, 2, 4, ... tối đa 64) để trận đấu kết thúc trong khoảng 15-25 ván bài. */
        fun ante(hand: Int) = 1 shl ((hand - 1) / 4).coerceAtMost(6)

        val PERM4: Array<IntArray> = perms(4)
        val PERM3: Array<IntArray> = perms(3)
        private fun perms(n: Int): Array<IntArray> {
            val out = ArrayList<IntArray>()
            fun rec(cur: IntArray, used: BooleanArray, k: Int) {
                if (k == n) { out.add(cur.copyOf()); return }
                for (i in 0 until n) if (!used[i]) { used[i] = true; cur[k] = i; rec(cur, used, k + 1); used[i] = false }
            }
            rec(IntArray(n), BooleanArray(n), 0)
            return out.toTypedArray()
        }

        fun code(perm: Int, opPerm: Int, mask: Int) = (perm * 6 + opPerm) * 16 + mask

        /** Giá trị biểu thức: số theo thứ tự [v] (đã áp √ theo [mask]), phép [o]. NaN nếu chia cho 0. */
        fun evaluate(v: IntArray, mask: Int, o: IntArray): Double {
            var total = 0.0; var sign = 1.0
            var term = num(v[0], mask, 0)
            for (i in 0 until 3) {
                val nx = num(v[i + 1], mask, i + 1)
                when (o[i]) {
                    OP_MUL -> term *= nx
                    OP_DIV -> { if (nx == 0.0) return Double.NaN; term /= nx }
                    OP_ADD -> { total += sign * term; sign = 1.0; term = nx }
                    else -> { total += sign * term; sign = -1.0; term = nx }
                }
            }
            return total + sign * term
        }
        private fun num(v: Int, mask: Int, i: Int): Double = if (mask and (1 shl i) != 0) sqrt(v.toDouble()) else v.toDouble()

        /** Giá trị của biểu thức [code] với 4 thẻ số [cards] (giá trị lấy từ thẻ) và 3 phép [ops]; NaN nếu không hợp lệ. */
        fun valueOf(code: Int, cards: IntArray, ops: IntArray, sqrtN: Int): Double {
            if (code !in 0 until CODES || cards.size != 4 || ops.size != 3) return Double.NaN
            val mask = code % 16; val op = (code / 16) % 6; val pm = code / 96
            if (Integer.bitCount(mask) != sqrtN) return Double.NaN
            val p = PERM4[pm]; val q = PERM3[op]
            val v = IntArray(4) { value(cards[p[it]]) }
            val o = IntArray(3) { ops[q[it]] }
            return evaluate(v, mask, o)
        }

        /** Mặt nạ √ hợp lệ cho [k] thẻ √. */
        fun masks(k: Int): IntArray = (0 until 16).filter { Integer.bitCount(it) == k }.toIntArray()

        /** Khoảng cách nhỏ nhất tới 20 và tới 1 cùng mã biểu thức đạt được: [bestHiDist, bestLoDist] và [hiCode, loCode]. */
        fun best(cards: IntArray, ops: IntArray, sqrtN: Int, codesOut: IntArray? = null): DoubleArray {
            var bh = Double.MAX_VALUE; var bl = Double.MAX_VALUE; var ch = -1; var cl = -1
            val ms = masks(sqrtN.coerceIn(0, 4))
            val v = IntArray(4); val o = IntArray(3)
            for (pm in 0 until 24) {
                val p = PERM4[pm]
                for (i in 0 until 4) v[i] = value(cards[p[i]])
                for (op in 0 until 6) {
                    val q = PERM3[op]
                    for (i in 0 until 3) o[i] = ops[q[i]]
                    for (m in ms) {
                        val x = evaluate(v, m, o)
                        if (x.isNaN()) continue
                        val dh = abs(x - HIGH); val dl = abs(x - LOW)
                        if (dh < bh) { bh = dh; ch = code(pm, op, m) }
                        if (dl < bl) { bl = dl; cl = code(pm, op, m) }
                    }
                }
            }
            codesOut?.let { it[0] = ch; it[1] = cl }
            return doubleArrayOf(bh, bl)
        }

        /** Thẻ xét hòa: High lấy thẻ lớn nhất, Low lấy thẻ nhỏ nhất (cùng số thì chất tốt hơn: vàng 0 > bạc > đồng > đen). */
        fun tieKeyHigh(cards: IntArray): Int = cards.maxOf { value(it) * 4 + (3 - suit(it)) }
        fun tieKeyLow(cards: IntArray): Int = cards.maxOf { (10 - value(it)) * 4 + (3 - suit(it)) }
        const val EPS = 1e-9
    }

    override val id = "hilo"

    // ── trạng thái ──
    val chips = IntArray(players) { START_CHIPS }
    val out = BooleanArray(players)
    val inHand = BooleanArray(players)
    val folded = BooleanArray(players)
    val contrib = IntArray(players)
    val roundBet = IntArray(players)
    private val acted = BooleanArray(players)
    /** Thẻ số của mỗi người: [0] là thẻ úp, sau đó thẻ ngửa theo thứ tự nhận. */
    val nums = Array(players) { ArrayList<Int>() }
    /** Thẻ ngửa theo thứ tự nhận (gồm thẻ đặc biệt), không gồm thẻ úp. */
    val openCards = Array(players) { ArrayList<Int>() }
    val ops = Array(players) { arrayListOf(OP_ADD, OP_SUB, OP_DIV) }
    val sqrtN = IntArray(players)
    val pendingDiscard = IntArray(players)
    val side = IntArray(players) { -1 }
    val expr = IntArray(players) { -1 }
    val failed = BooleanArray(players)
    private var deck = ArrayList<Int>()

    var phase = HiLoPhase.DEAL; private set
    var hand = 0; private set
    var dealer = 0; private set
    var betRound = 0; private set
    var currentBet = 0; private set
    var raises = 0; private set
    private var cur = 0
    var winner = -1; private set
    /** Ghế mà khi hết tiền thì ván dừng ngay (chơi với máy: người chơi), -1 = không. */
    var stopSeat = -1
    var lastShowdown: HiLoShowdown? = null; private set

    private val hist = ArrayList<Int>()
    override var result = Outcome.ONGOING; private set
    override var reason = EndReason.NONE; private set
    override val turn: Int get() = cur
    override val plies: Int get() = hist.size
    override val lastMove: Int get() = if (hist.isEmpty()) -1 else hist[hist.size - 1]
    override val history: List<Int> get() = hist
    override fun inCheck() = false

    val pot: Int get() = contrib.sum()
    fun alive(p: Int) = !out[p]
    fun active(p: Int) = inHand[p] && !folded[p]
    fun canBet(p: Int) = active(p) && chips[p] > 0
    fun toCall(p: Int) = (currentBet - roundBet[p]).coerceAtLeast(0)
    fun canRaise(p: Int) = raises < MAX_RAISES && chips[p] > toCall(p)
    fun cardsOf(p: Int): IntArray = nums[p].toIntArray()
    fun opsOf(p: Int): IntArray = ops[p].toIntArray()

    init { reset() }

    override fun reset() {
        chips.fill(START_CHIPS); out.fill(false); hand = 0; dealer = 0; cur = 0; winner = -1; lastShowdown = null
        hist.clear(); result = Outcome.ONGOING; reason = EndReason.NONE
        clearHand(); phase = HiLoPhase.DEAL
    }

    private fun clearHand() {
        inHand.fill(false); folded.fill(false); contrib.fill(0); roundBet.fill(0); acted.fill(false)
        for (p in 0 until players) { nums[p].clear(); openCards[p].clear(); ops[p].clear(); ops[p].addAll(listOf(OP_ADD, OP_SUB, OP_DIV)) }
        sqrtN.fill(0); pendingDiscard.fill(0); side.fill(-1); expr.fill(-1); failed.fill(false)
        betRound = 0; currentBet = 0; raises = 0; deck.clear()
    }

    override fun copyGame(): HiLoGame {
        val g = HiLoGame(players)
        chips.copyInto(g.chips); out.copyInto(g.out); inHand.copyInto(g.inHand); folded.copyInto(g.folded)
        contrib.copyInto(g.contrib); roundBet.copyInto(g.roundBet); acted.copyInto(g.acted)
        for (p in 0 until players) {
            g.nums[p].clear(); g.nums[p].addAll(nums[p]); g.openCards[p].clear(); g.openCards[p].addAll(openCards[p])
            g.ops[p].clear(); g.ops[p].addAll(ops[p])
        }
        sqrtN.copyInto(g.sqrtN); pendingDiscard.copyInto(g.pendingDiscard); side.copyInto(g.side); expr.copyInto(g.expr)
        failed.copyInto(g.failed); g.deck = ArrayList(deck)
        g.phase = phase; g.hand = hand; g.dealer = dealer; g.betRound = betRound; g.currentBet = currentBet; g.raises = raises
        g.cur = cur; g.winner = winner; g.stopSeat = stopSeat; g.lastShowdown = lastShowdown
        g.hist.addAll(hist); g.result = result; g.reason = reason
        return g
    }

    private fun nextFrom(p: Int, ok: (Int) -> Boolean): Int {
        for (k in 1..players) { val q = (p + k) % players; if (ok(q)) return q }
        return -1
    }

    // ── nước đi hợp lệ ──
    override fun legalMoves(): IntArray {
        val p = cur
        return when (phase) {
            HiLoPhase.DEAL -> intArrayOf(DEAL)
            HiLoPhase.DISCARD -> ops[p].filter { it != OP_MUL }.distinct().map { DISCARD + it }.toIntArray()
            HiLoPhase.BET -> buildList {
                add(FOLD); add(CALL)
                if (canRaise(p)) { val room = chips[p] - toCall(p); for (r in listOf(1, 2, 5, 10, room).distinct()) if (r in 1..room) add(RAISE + r) }
            }.toIntArray()
            HiLoPhase.DECLARE -> intArrayOf(DECLARE + SIDE_HIGH, DECLARE + SIDE_LOW, DECLARE + SIDE_SWING)
            HiLoPhase.BUILD -> {
                val c = cardsOf(p); val o = opsOf(p)
                val single = (0 until CODES).filter { !valueOf(it, c, o, sqrtN[p]).isNaN() }
                if (side[p] == SIDE_SWING) {
                    val bc = IntArray(2); best(c, o, sqrtN[p], bc)
                    single.map { it * CODES + bc[1] }.toIntArray()
                } else single.toIntArray()
            }
            HiLoPhase.OVER -> IntArray(0)
        }
    }

    override fun play(move: Int): Boolean {
        if (result != Outcome.ONGOING) return false
        val p = cur
        when (phase) {
            HiLoPhase.DEAL -> {
                if (move !in DEAL until DEAL + 1_000_000) return false
                hist.add(move); startHand(move - DEAL); return true
            }
            HiLoPhase.DISCARD -> {
                val op = move - DISCARD
                if (op !in 0..2 || op !in ops[p] || pendingDiscard[p] <= 0) return false
                hist.add(move); ops[p].remove(op); pendingDiscard[p]--
                afterDeal(); return true
            }
            HiLoPhase.BET -> {
                when {
                    move == FOLD -> { folded[p] = true }
                    move == CALL -> pay(p, toCall(p))
                    move in RAISE + 1 until RAISE + 900 -> {
                        val r = move - RAISE
                        if (!canRaise(p) || r > chips[p] - toCall(p)) return false
                        pay(p, toCall(p) + r)
                        currentBet = roundBet[p]; raises++
                        for (q in 0 until players) if (q != p) acted[q] = false
                    }
                    else -> return false
                }
                acted[p] = true; hist.add(move)
                afterBet(); return true
            }
            HiLoPhase.DECLARE -> {
                val s = move - DECLARE
                if (s !in 0..2) return false
                hist.add(move); side[p] = s
                val nx = nextFrom(p) { active(it) && side[it] < 0 }
                if (nx >= 0) cur = nx else { phase = HiLoPhase.BUILD; cur = nextFrom(dealer) { active(it) } }
                return true
            }
            HiLoPhase.BUILD -> {
                if (move == EXPR_FAIL) failed[p] = true
                else {
                    val c = cardsOf(p); val o = opsOf(p)
                    if (side[p] == SIDE_SWING) {
                        if (move !in 0 until CODES * CODES) return false
                        if (valueOf(move / CODES, c, o, sqrtN[p]).isNaN() || valueOf(move % CODES, c, o, sqrtN[p]).isNaN()) return false
                    } else if (valueOf(move, c, o, sqrtN[p]).isNaN()) return false
                    expr[p] = move
                }
                hist.add(move)
                val nx = nextFrom(p) { active(it) && expr[it] < 0 && !failed[it] }
                if (nx >= 0) cur = nx else showdown()
                return true
            }
            HiLoPhase.OVER -> return false
        }
    }

    private fun pay(p: Int, amount: Int) {
        val a = amount.coerceIn(0, chips[p])
        chips[p] -= a; contrib[p] += a; roundBet[p] += a
    }

    // ── chia bài ──
    private fun startHand(seed: Int) {
        clearHand(); hand++
        for (p in 0 until players) inHand[p] = !out[p]
        val a = ante(hand)
        for (p in 0 until players) if (inHand[p]) { val x = minOf(a, chips[p]); chips[p] -= x; contrib[p] += x }
        deck = ArrayList((0 until CARDS).toList()); deck.shuffle(Random(seed))
        val order = (1..players).map { (dealer + it) % players }.filter { inHand[it] }
        for (p in order) { val c = drawNumber(); nums[p].add(c) }
        for (p in order) dealOpen(p)          // như Devil's Plan: 1 thẻ úp + 1 thẻ ngửa rồi cược vòng 1
        betRound = 1
        afterDeal()
    }

    private fun drawNumber(): Int {
        val i = deck.indexOfFirst { isNumber(it) }
        return deck.removeAt(i)
    }

    private fun dealOpen(p: Int) {
        while (deck.isNotEmpty()) {
            val c = deck.removeAt(0)
            if (isNumber(c)) { nums[p].add(c); openCards[p].add(c); return }
            if (c >= SQRT_CARD) {
                if (sqrtN[p] < 4) { sqrtN[p]++; openCards[p].add(c) }
            } else if (ops[p].any { it != OP_MUL } && ops[p].count { it != OP_MUL } > pendingDiscard[p]) {
                ops[p].add(OP_MUL); pendingDiscard[p]++; openCards[p].add(c)
            }
        }
    }

    /** Sau khi chia: ai rút được × thì bỏ một phép, rồi vào vòng cược. */
    private fun afterDeal() {
        val nx = nextFrom(dealer) { active(it) && pendingDiscard[it] > 0 }
        if (nx >= 0) { phase = HiLoPhase.DISCARD; cur = nx; return }
        startBetting()
    }

    private fun startBetting() {
        roundBet.fill(0); acted.fill(false); currentBet = 0; raises = 0
        phase = HiLoPhase.BET
        val nx = nextFrom(dealer) { needsAct(it) }
        // không ai cần cược, hoặc chỉ còn một người có chip (những người khác đã tất tay): bỏ qua vòng cược
        if (nx < 0 || (0 until players).count { canBet(it) } < 2) { endBetting(); return }
        cur = nx
    }

    private fun needsAct(p: Int) = canBet(p) && (!acted[p] || roundBet[p] < currentBet)

    private fun afterBet() {
        if ((0 until players).count { active(it) } == 1) { uncontested(); return }
        val nx = nextFrom(cur) { needsAct(it) }
        // chỉ còn một người có thể cược và người đó đã theo đủ: hết vòng
        if (nx < 0 || ((0 until players).count { canBet(it) } < 2 && toCall(nx) == 0)) endBetting() else cur = nx
    }

    private fun endBetting() {
        if (betRound == 1) {
            betRound = 2
            // thêm 2 thẻ ngửa rồi cược vòng 2 (đủ 4 thẻ số)
            repeat(2) { for (p in (1..players).map { (dealer + it) % players }) if (active(p)) dealOpen(p) }
            afterDeal()
        } else {
            phase = HiLoPhase.DECLARE
            cur = nextFrom(dealer) { active(it) }
        }
    }

    // ── lật bài ──
    private fun uncontested() {
        val w = (0 until players).first { active(it) }
        val pay = IntArray(players); pay[w] = pot
        record(pay, w, -1, true, DoubleArray(players) { Double.NaN }, DoubleArray(players) { Double.NaN })
        chips[w] += pot
        nextHand()
    }

    /** a tốt hơn b ở cửa High? (cùng khoảng cách thì xét thẻ lớn nhất) */
    private fun betterHigh(a: Int, b: Int, hv: DoubleArray): Boolean {
        val da = abs(hv[a] - HIGH); val db = abs(hv[b] - HIGH)
        if (abs(da - db) > EPS) return da < db
        return tieKeyHigh(cardsOf(a)) > tieKeyHigh(cardsOf(b))
    }
    private fun betterLow(a: Int, b: Int, lv: DoubleArray): Boolean {
        val da = abs(lv[a] - LOW); val db = abs(lv[b] - LOW)
        if (abs(da - db) > EPS) return da < db
        return tieKeyLow(cardsOf(a)) > tieKeyLow(cardsOf(b))
    }

    private fun showdown() {
        val hv = DoubleArray(players) { Double.NaN }; val lv = DoubleArray(players) { Double.NaN }
        for (p in 0 until players) {
            if (!active(p) || failed[p] || expr[p] < 0) continue
            val c = cardsOf(p); val o = opsOf(p)
            when (side[p]) {
                SIDE_HIGH -> hv[p] = valueOf(expr[p], c, o, sqrtN[p])
                SIDE_LOW -> lv[p] = valueOf(expr[p], c, o, sqrtN[p])
                else -> { hv[p] = valueOf(expr[p] / CODES, c, o, sqrtN[p]); lv[p] = valueOf(expr[p] % CODES, c, o, sqrtN[p]) }
            }
        }
        val pay = IntArray(players)
        // pot phụ theo mức góp của người còn trong ván
        val levels = (0 until players).filter { active(it) }.map { contrib[it] }.filter { it > 0 }.distinct().sorted()
        var prev = 0; var topHi = -1; var topLo = -1
        for ((k, L) in levels.withIndex()) {
            var layer = 0
            for (q in 0 until players) layer += minOf(contrib[q], L) - minOf(contrib[q], prev)
            if (k == levels.size - 1) for (q in 0 until players) layer += (contrib[q] - L).coerceAtLeast(0)   // phần thừa của người đã bỏ
            val elig = (0 until players).filter { active(it) && contrib[it] >= L && !failed[it] && expr[it] >= 0 }
            val (hw, lw) = resolve(elig, hv, lv)
            if (k == 0) { topHi = hw; topLo = lw }
            when {
                hw >= 0 && lw >= 0 -> { val h = (layer + 1) / 2; pay[hw] += h; pay[lw] += layer - h }
                hw >= 0 -> pay[hw] += layer
                lw >= 0 -> pay[lw] += layer
                else -> for (q in 0 until players) pay[q] += minOf(contrib[q], L) - minOf(contrib[q], prev)   // không ai hợp lệ: trả lại
            }
            prev = L
        }
        if (levels.isEmpty()) for (q in 0 until players) pay[q] += contrib[q]
        record(pay, topHi, topLo, false, hv, lv)
        for (q in 0 until players) chips[q] += pay[q]
        nextHand()
    }

    /** Người thắng cửa High và cửa Low trong nhóm [elig]; người đánh Swing phải thắng cả hai, thua một cửa thì bị loại khỏi cả hai. */
    private fun resolve(elig: List<Int>, hv: DoubleArray, lv: DoubleArray): Pair<Int, Int> {
        val hi = elig.filter { side[it] != SIDE_LOW && !hv[it].isNaN() }.toMutableList()
        val lo = elig.filter { side[it] != SIDE_HIGH && !lv[it].isNaN() }.toMutableList()
        while (true) {
            val hw = hi.fold(-1) { b, p -> if (b < 0 || betterHigh(p, b, hv)) p else b }
            val lw = lo.fold(-1) { b, p -> if (b < 0 || betterLow(p, b, lv)) p else b }
            val bad = elig.firstOrNull { side[it] == SIDE_SWING && (it in hi || it in lo) && !(hw == it && lw == it) }
            if (bad == null) return hw to lw
            hi.remove(bad); lo.remove(bad)
        }
    }

    private fun record(pay: IntArray, hw: Int, lw: Int, unc: Boolean, hv: DoubleArray, lv: DoubleArray) {
        lastShowdown = HiLoShowdown(
            hand, side.copyOf(), hv, lv, expr.copyOf(), failed.copyOf(), BooleanArray(players) { !active(it) }, pay, hw, lw, unc,
            List(players) { cardsOf(it) }, List(players) { opsOf(it) }, sqrtN.copyOf(),
        )
    }

    private fun nextHand() {
        for (p in 0 until players) if (inHand[p] && chips[p] <= 0) out[p] = true
        val alive = (0 until players).filter { !out[it] }
        when {
            alive.size <= 1 -> { finish(alive.firstOrNull() ?: richest()); return }
            stopSeat in 0 until players && out[stopSeat] -> { finish(richest()); return }
            hand >= MAX_HANDS -> { finish(richest()); return }
        }
        clearHand()
        dealer = nextFrom(dealer) { !out[it] }
        phase = HiLoPhase.DEAL; cur = dealer
    }

    private fun richest(): Int = (0 until players).maxByOrNull { chips[it] } ?: 0

    private fun finish(w: Int) {
        phase = HiLoPhase.OVER; reason = EndReason.CHIPS; winner = w
        result = if (w == 0) Outcome.WIN_FIRST else Outcome.WIN_SECOND
    }

    override fun forfeit(loser: Int, reason: EndReason) {
        if (phase == HiLoPhase.OVER) return
        phase = HiLoPhase.OVER; this.reason = reason
        result = if (loser == 0) Outcome.WIN_SECOND else Outcome.WIN_FIRST
        winner = if (players == 2) 1 - loser else -1
    }

    override fun timeoutMove(): Int = when (phase) {
        HiLoPhase.DEAL -> DEAL + Random.nextInt(1_000_000)
        HiLoPhase.DISCARD -> DISCARD + ops[cur].first { it != OP_MUL }
        HiLoPhase.BET -> if (toCall(cur) == 0) CALL else FOLD
        HiLoPhase.DECLARE -> DECLARE + SIDE_HIGH
        HiLoPhase.BUILD -> EXPR_FAIL            // luật: hết 90 giây chưa nộp biểu thức thì thua ván bài này
        HiLoPhase.OVER -> -1
    }

    // ── bot 7 cấp ──
    override fun bestMove(timeMs: Long): Int = levelMove(BotLevel.SUPREME, timeMs, Random)

    override fun levelMove(level: BotLevel, timeMs: Long, rnd: Random): Int {
        if (phase == HiLoPhase.OVER) return -1
        val cfg = BotLevels.config(id, level, timeMs)
        return HiLoBot(copyGame(), level.ordinal, cfg.noiseP, rnd).move()
    }
}

/**
 * Bot Công thức cao thấp. Ước lượng cơ hội thắng mỗi cửa bằng Monte Carlo trên các thẻ chưa thấy (thẻ úp của đối thủ,
 * thẻ ngửa còn chưa chia), mỗi mẫu tìm biểu thức tốt nhất của từng người (duyệt hết 2304 biểu thức).
 * Cấp càng cao càng nhiều mẫu và càng ít nước ngẫu nhiên; Dễ/TB/Khá có thể lập biểu thức chưa tối ưu.
 */
internal class HiLoBot(private val g: HiLoGame, private val lv: Int, private val noise: Double, private val rnd: Random) {
    private val me = g.turn
    private val samples = intArrayOf(4, 8, 16, 40, 100, 200, 400)[lv.coerceIn(0, 6)]

    fun move(): Int = when (g.phase) {
        HiLoPhase.DEAL -> HiLoGame.DEAL + rnd.nextInt(1_000_000)
        HiLoPhase.DISCARD -> discard()
        HiLoPhase.BET -> bet()
        HiLoPhase.DECLARE -> HiLoGame.DECLARE + declare()
        HiLoPhase.BUILD -> build()
        else -> -1
    }

    private fun noisy() = noise > 0 && rnd.nextDouble() < noise

    /** [pH, pL, pBoth]: xác suất mình tốt nhất ở cửa High / Low / cả hai giữa những người còn trong ván. */
    private fun odds(myOps: IntArray = g.opsOf(me), n: Int = samples): DoubleArray {
        val act = (0 until g.players).filter { g.active(it) }
        val seen = HashSet<Int>()
        seen.addAll(g.nums[me])
        for (p in act) if (p != me) seen.addAll(g.openCards[p].filter { HiLoGame.isNumber(it) })
        for (p in 0 until g.players) if (!g.active(p)) seen.addAll(g.openCards[p].filter { HiLoGame.isNumber(it) })
        val pool = (0 until HiLoGame.NUMS).filter { it !in seen }.toIntArray()
        var h = 0; var l = 0; var b = 0
        repeat(n) {
            // xáo một phần pool
            var k = 0
            fun take(): Int { val j = k + rnd.nextInt(pool.size - k); val t = pool[j]; pool[j] = pool[k]; pool[k] = t; k++; return t }
            var myH = 0.0; var myL = 0.0; var myCards = IntArray(0)
            val res = HashMap<Int, DoubleArray>(); val cardsOf = HashMap<Int, IntArray>()
            for (p in act) {
                val c = ArrayList<Int>()
                if (p == me) c.addAll(g.nums[me]) else { c.add(take()); c.addAll(g.openCards[p].filter { HiLoGame.isNumber(it) }) }
                while (c.size < 4) c.add(take())
                val cards = c.toIntArray()
                val o = if (p == me) myOps else opponentOps(p)
                val d = HiLoGame.best(cards, o, g.sqrtN[p])
                res[p] = d; cardsOf[p] = cards
                if (p == me) { myH = d[0]; myL = d[1]; myCards = cards }
            }
            var winH = true; var winL = true
            for (p in act) {
                if (p == me) continue
                val d = res[p]!!; val c = cardsOf[p]!!
                if (d[0] < myH - HiLoGame.EPS || (abs(d[0] - myH) <= HiLoGame.EPS && HiLoGame.tieKeyHigh(c) > HiLoGame.tieKeyHigh(myCards))) winH = false
                if (d[1] < myL - HiLoGame.EPS || (abs(d[1] - myL) <= HiLoGame.EPS && HiLoGame.tieKeyLow(c) > HiLoGame.tieKeyLow(myCards))) winL = false
            }
            if (winH) h++; if (winL) l++; if (winH && winL) b++
        }
        return doubleArrayOf(h.toDouble() / n, l.toDouble() / n, b.toDouble() / n)
    }

    private fun opponentOps(p: Int): IntArray {
        val o = ArrayList(g.ops[p])
        repeat(g.pendingDiscard[p]) { o.remove(HiLoGame.OP_DIV) || o.remove(HiLoGame.OP_SUB) || o.remove(HiLoGame.OP_ADD) }
        return o.take(3).toIntArray()
    }

    /** Phần pot kỳ vọng (0..1) nếu chọn cửa tốt nhất. */
    private fun share(o: DoubleArray) = maxOf(0.55 * o[0], 0.55 * o[1], o[2])

    private fun discard(): Int {
        val opts = g.ops[me].filter { it != HiLoGame.OP_MUL }.distinct()
        if (noisy() || lv == 0) return HiLoGame.DISCARD + opts[rnd.nextInt(opts.size)]
        var best = opts[0]; var bv = -1.0
        for (d in opts) {
            val o = ArrayList(g.ops[me]); o.remove(d)
            val rest = ArrayList(o)
            repeat(g.pendingDiscard[me] - 1) { rest.remove(HiLoGame.OP_DIV) || rest.remove(HiLoGame.OP_SUB) || rest.remove(HiLoGame.OP_ADD) }
            val v = share(odds(rest.take(3).toIntArray(), maxOf(4, samples / 2)))
            if (v > bv) { bv = v; best = d }
        }
        return HiLoGame.DISCARD + best
    }

    private fun bet(): Int {
        val call = g.toCall(me)
        if (noisy()) {
            val r = rnd.nextDouble()
            return when {
                r < 0.12 && call > 0 -> HiLoGame.FOLD
                r < 0.30 && g.canRaise(me) -> HiLoGame.RAISE + minOf(2, g.chips[me] - call).coerceAtLeast(1)
                else -> HiLoGame.CALL
            }
        }
        val o = odds()
        val s = share(o)
        val nAct = (0 until g.players).count { g.active(it) }
        val fair = 1.0 / nAct
        val pot = g.pot
        val room = g.chips[me] - call
        fun raise(f: Double): Int {
            val r = (pot * f).toInt().coerceIn(1, room)
            return HiLoGame.RAISE + r
        }
        val bluff = lv >= 5 && rnd.nextDouble() < 0.05
        if (call == 0) {
            if ((s > 1.7 * fair || bluff) && g.canRaise(me) && room > 0) return raise(if (s > 2.5 * fair) 0.6 else 0.35)
            return HiLoGame.CALL
        }
        if (s > 2.4 * fair && g.canRaise(me) && room > 0 && lv >= 2) return raise(0.5)
        val ev = s * (pot + call) - call
        // cấp thấp theo nhiều hơn (dễ đoán), cấp cao tính theo tỉ lệ pot
        val slack = if (lv <= 2) 0.15 * call else 0.0
        return if (ev + slack >= 0) HiLoGame.CALL else HiLoGame.FOLD
    }

    private fun declare(): Int {
        if (noisy() || lv == 0) return rnd.nextInt(2)
        val o = odds()
        val vH = 0.55 * o[0]; val vL = 0.55 * o[1]
        // Swing chỉ khi rất chắc thắng cả hai (cấp cao)
        if (lv >= 3 && o[2] > maxOf(vH, vL) && o[2] > 0.5) return HiLoGame.SIDE_SWING
        return if (vH >= vL) HiLoGame.SIDE_HIGH else HiLoGame.SIDE_LOW
    }

    private fun build(): Int {
        val c = g.cardsOf(me); val o = g.opsOf(me); val k = g.sqrtN[me]
        val bc = IntArray(2); HiLoGame.best(c, o, k, bc)
        fun pick(sideCode: Int): Int {
            if (!noisy()) return sideCode
            // biểu thức hợp lệ ngẫu nhiên (người mới chơi chưa tìm ra cách ghép tốt nhất)
            val valid = (0 until HiLoGame.CODES).filter { !HiLoGame.valueOf(it, c, o, k).isNaN() }
            return if (valid.isEmpty()) sideCode else valid[rnd.nextInt(valid.size)]
        }
        if (bc[0] < 0) return HiLoGame.EXPR_FAIL
        return when (g.side[me]) {
            HiLoGame.SIDE_HIGH -> pick(bc[0])
            HiLoGame.SIDE_LOW -> pick(bc[1])
            else -> pick(bc[0]) * HiLoGame.CODES + pick(bc[1])
        }
    }
}
