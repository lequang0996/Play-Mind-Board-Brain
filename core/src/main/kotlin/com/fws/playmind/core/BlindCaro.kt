package com.fws.playmind.core

import kotlin.random.Random

/**
 * Cờ caro mù (chủ đề Ghi nhớ): luật thắng giống hệt cờ caro ([GomokuGame]: bàn vô hạn, 5 quân liên tiếp, bị chặn cả hai
 * đầu thì không tính), nhưng hai bên dùng CHUNG bộ quân 4 màu (xanh, đỏ, tím, vàng) và bàn cờ không cho biết quân nào
 * của ai. Người chơi phải tự nhớ quân của mình.
 *
 *  - Người đi trước (X) mỗi lượt chọn đặt một quân với màu bất kỳ.
 *  - Người đi sau (O) đáp lại bằng một quân CÙNG MÀU với quân người đi trước vừa đặt.
 *  - X / O chỉ quyết định thắng thua; giao diện chỉ vẽ màu, hết ván mới lộ X / O.
 *
 * Mã nước đi: [pack] (x, y trong ±[LIMIT], màu 0..3). Bot dùng nguyên bot caro (cùng bảng cấp "caro"), màu: X chọn
 * ngẫu nhiên, O theo màu bắt buộc.
 */
class BlindCaroGame : BoardGame, LeveledEngine {
    companion object {
        const val COLORS = 4
        const val LIMIT = 4096
        private const val BITS = 13
        private const val MASK = (1 shl BITS) - 1

        fun pack(x: Int, y: Int, color: Int): Int = ((((y + LIMIT) shl BITS) or (x + LIMIT)) shl 2) or (color and 3)
        fun px(move: Int): Int = ((move shr 2) and MASK) - LIMIT
        fun py(move: Int): Int = (move shr (2 + BITS)) - LIMIT
        fun color(move: Int): Int = move and 3
        fun inRange(x: Int, y: Int): Boolean = x >= -LIMIT && x < LIMIT && y >= -LIMIT && y < LIMIT
        /** Ô của nước [move] theo mã của [GomokuGame]. */
        fun cell(move: Int): Int = GomokuGame.pack(px(move), py(move))
    }

    override val id = "blindcaro"
    /** Ván caro bên trong (vị trí, chủ quân, dãy thắng). Chỉ đọc. */
    val inner = GomokuGame()
    private val hist = ArrayList<Int>()
    override val history: List<Int> get() = hist
    override val turn: Int get() = inner.turn
    override val plies: Int get() = hist.size
    override val result: Int get() = inner.result
    override val reason: EndReason get() = inner.reason
    override val lastMove: Int get() = if (hist.isEmpty()) -1 else hist[hist.size - 1]

    /** Màu bắt buộc của lượt hiện tại (người đi sau phải theo màu quân vừa đặt), hoặc -1 nếu được chọn tự do. */
    val forcedColor: Int get() = if (turn == 1 && hist.isNotEmpty()) color(hist[hist.size - 1]) else -1

    /** Chủ của ô (x, y): 0 = trống, 1 = X, 2 = O. */
    fun at(x: Int, y: Int): Int = inner.at(x, y)

    /** Màu quân ở ô (x, y), -1 nếu trống. */
    fun colorAt(x: Int, y: Int): Int {
        for (m in hist) if (px(m) == x && py(m) == y) return color(m)
        return -1
    }

    override fun reset() { inner.reset(); hist.clear() }
    override fun inCheck() = false

    /** Nước hợp lệ: như caro (ô quanh quân đã có), với mọi màu được phép ở lượt này. */
    override fun legalMoves(): IntArray {
        val cells = inner.legalMoves()
        val f = forcedColor
        val out = ArrayList<Int>(cells.size * (if (f >= 0) 1 else COLORS))
        for (c in cells) {
            val x = GomokuGame.px(c); val y = GomokuGame.py(c)
            if (!inRange(x, y)) continue
            if (f >= 0) out.add(pack(x, y, f)) else for (k in 0 until COLORS) out.add(pack(x, y, k))
        }
        return out.toIntArray()
    }

    override fun play(move: Int): Boolean {
        if (result != Outcome.ONGOING || move < 0) return false
        val x = px(move); val y = py(move); val c = color(move)
        if (!inRange(x, y) || pack(x, y, c) != move) return false
        val f = forcedColor
        if (f >= 0 && c != f) return false
        if (!inner.play(GomokuGame.pack(x, y))) return false
        hist.add(move)
        return true
    }

    override fun forfeit(loser: Int, reason: EndReason) = inner.forfeit(loser, reason)

    override fun copyGame(): BlindCaroGame {
        val g = BlindCaroGame()
        for (m in hist) g.play(m)
        if (result != Outcome.ONGOING && g.result == Outcome.ONGOING) g.forfeit(if (result == Outcome.WIN_FIRST) 1 else 0, reason)
        return g
    }

    override fun bestMove(timeMs: Long): Int = levelMove(BotLevel.SUPREME, timeMs, Random)

    override fun levelMove(level: BotLevel, timeMs: Long, rnd: Random): Int {
        if (result != Outcome.ONGOING) return -1
        val m = inner.copyGame().levelMove(level, timeMs, rnd)
        if (m < 0) return -1
        val x = GomokuGame.px(m); val y = GomokuGame.py(m)
        if (!inRange(x, y)) return legalMoves().firstOrNull() ?: -1
        val f = forcedColor
        return pack(x, y, if (f >= 0) f else rnd.nextInt(COLORS))
    }
}
