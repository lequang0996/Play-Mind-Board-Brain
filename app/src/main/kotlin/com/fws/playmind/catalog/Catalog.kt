package com.fws.playmind.catalog

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.fws.playmind.R
import com.fws.playmind.core.BoardGame
import com.fws.playmind.core.ChessGame
import com.fws.playmind.core.GomokuGame
import com.fws.playmind.core.XiangqiGame

/** 4 chủ đề của app. Mỗi chủ đề có một màu nhận diện dùng xuyên suốt giao diện. */
enum class Topic(@StringRes val title: Int, val color: Color, val colorDark: Color) {
    PUZZLE(R.string.topic_puzzle, Color(0xFF14937A), Color(0xFF3FD0B0)),
    MIND(R.string.topic_mind, Color(0xFF5348C9), Color(0xFF9A93FF)),
    MATH(R.string.topic_math, Color(0xFFC27A0A), Color(0xFFF2B84B)),
    MEMORY(R.string.topic_memory, Color(0xFFC93E6B), Color(0xFFFF86A8)),
}

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
    /** Thời gian suy nghĩ tối đa của bot cho mỗi nước (ms). Bot luôn ở mức mạnh nhất. */
    val botMillis: Long,
    val create: () -> BoardGame,
)

object Catalog {
    val games: List<GameInfo> = listOf(
        GameInfo("chess", Topic.MIND, R.string.game_chess, R.string.game_chess_desc, R.string.guide_chess,
            R.string.side_white, R.string.side_black, 12, 4500) { ChessGame() },
        GameInfo("xiangqi", Topic.MIND, R.string.game_xiangqi, R.string.game_xiangqi_desc, R.string.guide_xiangqi,
            R.string.side_red, R.string.side_black, 12, 4500) { XiangqiGame() },
        GameInfo("caro", Topic.MIND, R.string.game_caro, R.string.game_caro_desc, R.string.guide_caro,
            R.string.stone_black, R.string.stone_white, 9, 3500) { GomokuGame() },
    )

    fun byId(id: String) = games.first { it.id == id }
    fun byTopic(t: Topic) = games.filter { it.topic == t }
}
