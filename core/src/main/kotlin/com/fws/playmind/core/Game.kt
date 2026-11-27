package com.fws.playmind.core

/** Kết quả ván cờ. */
object Outcome {
    const val ONGOING = 0
    const val WIN_FIRST = 1
    const val WIN_SECOND = 2
    const val DRAW = 3
}

enum class EndReason { NONE, CHECKMATE, STALEMATE, NOMOVES, REPETITION, FIFTY, MATERIAL, FIVE, FULL, RESIGN, LEAVE, TERRITORY, TIMEOUT, MOVE_LIMIT }

/**
 * Giao diện chung cho mọi game bàn cờ. Nước đi luôn được mã hóa thành Int
 * để truyền qua mạng và băm xác nhận kết quả.
 */
interface BoardGame {
    val id: String
    /** 0 = bên đi trước, 1 = bên đi sau. */
    val turn: Int
    val plies: Int
    val result: Int
    val reason: EndReason
    val lastMove: Int
    val history: List<Int>
    fun legalMoves(): IntArray
    fun play(move: Int): Boolean
    /** Tìm nước tốt nhất trong [timeMs] ms (chạy trên bản sao, an toàn khi gọi từ thread nền). */
    fun bestMove(timeMs: Long): Int
    fun copyGame(): BoardGame
    fun reset()
    fun inCheck(): Boolean
    /** Kết thúc ván do một bên đầu hàng / rời đi. loser = 0 hoặc 1. */
    fun forfeit(loser: Int, reason: EndReason)
}

/** Băm danh sách nước đi để hai máy đối chiếu kết quả ván. */
fun movesHash(history: List<Int>): Int {
    var h = 0x811C9DC5.toInt()
    for (m in history) {
        h = (h xor m) * 16777619
        h = h xor (h ushr 15)
    }
    return h
}
