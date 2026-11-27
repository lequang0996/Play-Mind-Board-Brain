package com.fws.playmind.catalog

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.fws.playmind.R
import com.fws.playmind.core.BoardGame
import com.fws.playmind.core.ChessGame
import com.fws.playmind.core.GoGame
import com.fws.playmind.core.GomokuGame
import com.fws.playmind.core.QuoridorGame
import com.fws.playmind.core.WallGoGame
import com.fws.playmind.core.XiangqiGame

/** 4 chủ đề của app, theo thứ tự hiển thị: Trí tuệ, Tính toán, Giải đố, Ghi nhớ. Mỗi chủ đề có một màu nhận diện dùng xuyên suốt giao diện. */
enum class Topic(@StringRes val title: Int, val color: Color) {
    MIND(R.string.topic_mind, Color(0xFF8B7BFF)),
    MATH(R.string.topic_math, Color(0xFF3FB6E8)),
    PUZZLE(R.string.topic_puzzle, Color(0xFF2DD4A0)),
    MEMORY(R.string.topic_memory, Color(0xFFFF6B8B)),
}

/** DUEL = hai bên đấu (có phòng online); SOLO = một người giải đố (Sudoku). */
enum class GameKind { DUEL, SOLO }

/** Tùy chọn trước ván: NONE, PLAYERS (số người chơi) hoặc BOARD (cỡ bàn). */
enum class OptionKind { NONE, PLAYERS, BOARD }

/** Một game trong danh mục. Thêm game mới = thêm một dòng vào [Catalog.games] và một bàn cờ trong ui. */
data class GameInfo(
    val id: String,
    val topic: Topic,
    @StringRes val name: Int,
    @StringRes val desc: Int,
    @StringRes val guide: Int,
    @StringRes val firstSide: Int,
    @StringRes val secondSide: Int,
    /** Số nước tối thiểu để ván đầu hàng/rời đi vẫn được tính thưởng. */
    val minPlies: Int,
    /** Thời gian suy nghĩ tối đa của bot cho mỗi nước (ms), theo tùy chọn đang chọn. Bot luôn ở mức mạnh nhất. */
    val botMillis: (Int) -> Long,
    val kind: GameKind = GameKind.DUEL,
    val optionKind: OptionKind = OptionKind.NONE,
    val options: List<Int> = emptyList(),
    val defaultOption: Int = 0,
    /** Cho phép chơi chung một máy (không tính thưởng, không tốn lượt). */
    val localPlay: Boolean = false,
    val create: ((Int) -> BoardGame)? = null,
) {
    fun make(option: Int): BoardGame = (create ?: error("game $id không phải game đối kháng"))(option)
}

object Catalog {
    val games: List<GameInfo> = listOf(
        GameInfo("chess", Topic.MIND, R.string.game_chess, R.string.game_chess_desc, R.string.guide_chess,
            R.string.side_white, R.string.side_black, 12, { 4500L }, localPlay = true, create = { ChessGame() }),
        GameInfo("xiangqi", Topic.MIND, R.string.game_xiangqi, R.string.game_xiangqi_desc, R.string.guide_xiangqi,
            R.string.side_red, R.string.side_black, 12, { 4500L }, localPlay = true, create = { XiangqiGame() }),
        GameInfo("go", Topic.MIND, R.string.game_go, R.string.game_go_desc, R.string.guide_go,
            R.string.stone_black, R.string.stone_white, 20, { size -> when (size) { 9 -> 5000L; 13 -> 7000L; else -> 9000L } },
            optionKind = OptionKind.BOARD, options = listOf(9, 13, 19), defaultOption = 9, localPlay = true,
            create = { size -> GoGame(size) }),
        GameInfo("quoridor", Topic.MIND, R.string.game_quoridor, R.string.game_quoridor_desc, R.string.guide_quoridor,
            R.string.side_red, R.string.side_blue, 10, { n -> if (n == 2) 3500L else 2500L },
            optionKind = OptionKind.PLAYERS, options = listOf(2, 4), defaultOption = 2, localPlay = true,
            create = { n -> QuoridorGame(n) }),
        GameInfo("wallgo", Topic.MIND, R.string.game_wallgo, R.string.game_wallgo_desc, R.string.guide_wallgo,
            R.string.side_red, R.string.side_blue, 12, { 3500L },
            optionKind = OptionKind.PLAYERS, options = listOf(2, 3, 4), defaultOption = 2, localPlay = true,
            create = { n -> WallGoGame(n) }),
        GameInfo("caro", Topic.MIND, R.string.game_caro, R.string.game_caro_desc, R.string.guide_caro,
            R.string.stone_black, R.string.stone_white, 9, { 3500L }, localPlay = true, create = { GomokuGame() }),
        GameInfo("sudoku", Topic.MATH, R.string.game_sudoku, R.string.game_sudoku_desc, R.string.guide_sudoku,
            R.string.side_white, R.string.side_black, 0, { 0L }, kind = GameKind.SOLO,
            options = listOf(0, 1, 2, 3), defaultOption = 0),
    )

    fun byId(id: String) = games.first { it.id == id }
    fun byTopic(t: Topic) = games.filter { it.topic == t }
}
