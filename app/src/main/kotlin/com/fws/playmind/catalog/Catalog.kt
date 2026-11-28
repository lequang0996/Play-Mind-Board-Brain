package com.fws.playmind.catalog

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.fws.playmind.R
import com.fws.playmind.core.BlindCaroGame
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

/** DUEL = hai bên đấu (có phòng online); SOLO = một người giải đố (Sudoku, Ma phương 7x7). */
enum class GameKind { DUEL, SOLO }

/** Tùy chọn trước ván: NONE, PLAYERS (số người chơi) hoặc BOARD (cỡ bàn). */
enum class OptionKind { NONE, PLAYERS, BOARD }

/**
 * Thẻ mô tả nội dung game, hiển thị ở trang chủ đề và dùng để sắp xếp (chọn thẻ thì game có thẻ đó lên đầu).
 * Ba thẻ thời lượng (TIME_*) tự suy ra từ [GameInfo.minutes].
 */
enum class Tag(@StringRes val label: Int) {
    SOLO(R.string.tag_solo), DUEL(R.string.tag_duel), PARTY(R.string.tag_party),
    BOARD(R.string.tag_board), STRATEGY(R.string.tag_strategy), CLASSIC(R.string.tag_classic),
    WORDS(R.string.tag_words), NUMBERS(R.string.tag_numbers), SHAPES(R.string.tag_shapes), MEMORY(R.string.tag_memory),
    DAILY(R.string.tag_daily), COUNTDOWN(R.string.tag_countdown),
    TIME_SHORT(R.string.tag_time_short), TIME_MID(R.string.tag_time_mid), TIME_LONG(R.string.tag_time_long),
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
    /** Ngân sách thời gian mỗi nước của bot cấp Siêu việt (ms), theo tùy chọn đang chọn. Sáu cấp dưới giới hạn theo nút/độ sâu (core BotLevels). */
    val botMillis: (Int) -> Long,
    val kind: GameKind = GameKind.DUEL,
    val optionKind: OptionKind = OptionKind.NONE,
    val options: List<Int> = emptyList(),
    val defaultOption: Int = 0,
    /** Thứ tự trong trang "Xem thêm": số nhỏ = dễ chơi và ván ngắn hơn nên xếp trước. */
    val order: Int = 0,
    /** Thời lượng một ván điển hình (phút), hiển thị ở trang "Xem thêm". */
    val minutes: Int = 10,
    val create: ((Int) -> BoardGame)? = null,
    /** Độ phổ biến trên thế giới (0-100). Trang chủ đề xếp game ít phổ biến (độc lạ) lên trước. */
    val popularity: Int = 50,
    /** Thẻ nội dung (không gồm thẻ thời lượng, xem [tags]). */
    val contentTags: List<Tag> = emptyList(),
) {
    /** Thẻ nội dung + một thẻ thời lượng. */
    val tags: List<Tag> get() = contentTags + when { minutes <= 10 -> Tag.TIME_SHORT; minutes <= 20 -> Tag.TIME_MID; else -> Tag.TIME_LONG }
    fun make(option: Int): BoardGame = (create ?: error("game $id không phải game đối kháng"))(option)
}

object Catalog {
    /** Sudoku chỉ có một chế độ chuẩn: bàn 9x9 gồm 9 khối 3x3, mỗi hàng/cột/khối đủ 1-9 và đề luôn có đúng một lời giải. */
    const val SUDOKU_LEVEL = 1   // SudokuLevel.MEDIUM

    /** Ma phương 7x7: mỗi ngày một đề (core MagicSquareGame). Gợi ý mỗi ngày: gói tháng 10 lần, gói năm 20 lần. */
    const val MAGIC_ID = "magic7"
    /** Xếp hình 7 mảnh (tangram): 10 hình ngẫu nhiên mỗi lượt, mỗi hình 3 phút (core Tangram). */
    const val TANGRAM_ID = "tangram"
    /** Khối gỗ ghép chữ: ghép 7 từ tiếng Anh cùng chủ đề trong 10 phút (core WordBlocks). */
    const val WORDS_ID = "wordblocks"
    const val MAGIC_HINTS_MONTHLY = 10
    const val MAGIC_HINTS_YEARLY = 20

    /**
     * 7 mức thời gian tối đa mỗi nước (giây), dùng chung cho MỌI game: chơi với máy theo cấp bot Dễ → Siêu việt,
     * chơi với người thì người chơi (chủ phòng) chọn một trong 7 mức. Hết giờ mà chưa đi thì người đang đến lượt bị xử thua.
     */
    val LEVEL_MOVE_SECONDS = intArrayOf(60, 120, 180, 300, 600, 1800, 3600)
    const val LEVELS = 7

    /** Cờ vây luôn chơi trên bàn chuẩn 19x19. */
    const val GO_SIZE = 19

    val games: List<GameInfo> = listOf(
        GameInfo("chess", Topic.MIND, R.string.game_chess, R.string.game_chess_desc, R.string.guide_chess,
            R.string.side_white, R.string.side_black, 12, { 8000L }, order = 4, minutes = 20, create = { ChessGame() },
            popularity = 100, contentTags = listOf(Tag.DUEL, Tag.BOARD, Tag.STRATEGY, Tag.CLASSIC)),
        GameInfo("xiangqi", Topic.MIND, R.string.game_xiangqi, R.string.game_xiangqi_desc, R.string.guide_xiangqi,
            R.string.side_red, R.string.side_black, 12, { 8000L }, order = 5, minutes = 20, create = { XiangqiGame() },
            popularity = 80, contentTags = listOf(Tag.DUEL, Tag.BOARD, Tag.STRATEGY, Tag.CLASSIC)),
        GameInfo("go", Topic.MIND, R.string.game_go, R.string.game_go_desc, R.string.guide_go,
            R.string.stone_black, R.string.stone_white, 20, { 15000L }, order = 6, minutes = 40,
            create = { GoGame(GO_SIZE) }, popularity = 85, contentTags = listOf(Tag.DUEL, Tag.BOARD, Tag.STRATEGY, Tag.CLASSIC)),
        GameInfo("quoridor", Topic.MIND, R.string.game_quoridor, R.string.game_quoridor_desc, R.string.guide_quoridor,
            R.string.side_red, R.string.side_blue, 10, { n -> if (n == 2) 7000L else 5000L },
            optionKind = OptionKind.PLAYERS, options = listOf(2, 4), defaultOption = 2, order = 3, minutes = 12,
            create = { n -> QuoridorGame(n) }, popularity = 30, contentTags = listOf(Tag.DUEL, Tag.PARTY, Tag.BOARD, Tag.STRATEGY)),
        GameInfo("wallgo", Topic.MIND, R.string.game_wallgo, R.string.game_wallgo_desc, R.string.guide_wallgo,
            R.string.side_red, R.string.side_blue, 12, { 7000L },
            optionKind = OptionKind.PLAYERS, options = listOf(2, 3, 4), defaultOption = 2, order = 2, minutes = 8,
            create = { n -> WallGoGame(n) }, popularity = 10, contentTags = listOf(Tag.DUEL, Tag.PARTY, Tag.BOARD, Tag.STRATEGY)),
        GameInfo("caro", Topic.MIND, R.string.game_caro, R.string.game_caro_desc, R.string.guide_caro,
            R.string.stone_black, R.string.stone_white, 9, { 6000L }, order = 1, minutes = 5, create = { GomokuGame() },
            popularity = 75, contentTags = listOf(Tag.DUEL, Tag.BOARD, Tag.CLASSIC)),
        // Ghi nhớ: luật như cờ caro nhưng quân chỉ có màu, phải tự nhớ quân nào của mình (bot dùng nguyên bot caro)
        GameInfo("blindcaro", Topic.MEMORY, R.string.game_blindcaro, R.string.game_blindcaro_desc, R.string.guide_blindcaro,
            R.string.side_x, R.string.side_o, 9, { 6000L }, order = 1, minutes = 8, create = { BlindCaroGame() },
            popularity = 5, contentTags = listOf(Tag.DUEL, Tag.BOARD, Tag.MEMORY)),
        GameInfo("sudoku", Topic.PUZZLE, R.string.game_sudoku, R.string.game_sudoku_desc, R.string.guide_sudoku,
            R.string.side_white, R.string.side_black, 0, { 0L }, kind = GameKind.SOLO, order = 1, minutes = 10,
            popularity = 90, contentTags = listOf(Tag.SOLO, Tag.NUMBERS, Tag.CLASSIC)),
        GameInfo(TANGRAM_ID, Topic.PUZZLE, R.string.game_tangram, R.string.game_tangram_desc, R.string.guide_tangram,
            R.string.side_white, R.string.side_black, 0, { 0L }, kind = GameKind.SOLO, order = 3, minutes = 30,
            popularity = 50, contentTags = listOf(Tag.SOLO, Tag.SHAPES, Tag.COUNTDOWN, Tag.CLASSIC)),
        GameInfo(MAGIC_ID, Topic.PUZZLE, R.string.game_magic7, R.string.game_magic7_desc, R.string.guide_magic7,
            R.string.side_white, R.string.side_black, 0, { 0L }, kind = GameKind.SOLO, order = 2, minutes = 30,
            popularity = 20, contentTags = listOf(Tag.SOLO, Tag.NUMBERS, Tag.DAILY)),
        // Trí tuệ: ghép 7 từ tiếng Anh cùng chủ đề từ các khối gỗ chữ cái, 10 phút
        GameInfo(WORDS_ID, Topic.MIND, R.string.game_wordblocks, R.string.game_wordblocks_desc, R.string.guide_wordblocks,
            R.string.side_white, R.string.side_black, 0, { 0L }, kind = GameKind.SOLO, order = 0, minutes = 10,
            popularity = 15, contentTags = listOf(Tag.SOLO, Tag.WORDS, Tag.COUNTDOWN)),
    )

    fun byId(id: String) = games.first { it.id == id }
    /** Game của một chủ đề, xếp theo độ phổ biến tăng dần: game độc lạ lên trước. */
    fun byTopic(t: Topic) = games.filter { it.topic == t }.sortedWith(compareBy({ it.popularity }, { it.order }))

    /** Như [byTopic], nhưng game có thẻ [tag] lên đầu (giữ thứ tự độ phổ biến trong mỗi nhóm). */
    fun byTopicTagFirst(t: Topic, tag: Tag?) = byTopic(t).sortedBy { if (tag == null || tag in it.tags) 0 else 1 }

    /** Các thẻ có mặt trong chủ đề, theo thứ tự khai báo của [Tag]. */
    fun tagsOf(t: Topic): List<Tag> = byTopic(t).flatMap { it.tags }.toSet().sortedBy { it.ordinal }
}
