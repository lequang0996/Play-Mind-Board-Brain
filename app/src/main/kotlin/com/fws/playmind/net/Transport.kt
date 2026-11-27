package com.fws.playmind.net

import kotlinx.coroutines.flow.StateFlow

enum class LinkState { IDLE, CONNECTING, CONNECTED, CLOSED, FAILED }

/** Kênh truyền văn bản giữa hai máy. Có hai cài đặt: Nearby (gần nhau) và WebRTC (internet). */
interface Transport {
    val state: StateFlow<LinkState>
    var onMessage: ((String) -> Unit)?
    fun send(text: String)
    fun close()
}

/**
 * Giao thức ván đấu (văn bản, ngăn cách bằng '|'):
 *  H|gameId|tên|bênCủaChủPhòng(0/1)   chủ phòng chào sau khi kết nối
 *  J|tên                               khách trả lời
 *  S|bênCủaChủPhòng                    chủ phòng bắt đầu ván
 *  M|số_thứ_tự|nướcĐi                 một nước đi
 *  R                                   đầu hàng
 *  Q                                   rời phòng / thoát ván
 *  K                                   chủ phòng mời khách ra
 *  RM                                  muốn chơi lại
 *  E|kếtQuả|băm                        xác nhận kết quả ván (đối chiếu hai máy)
 *  T                                   người gửi hết thời gian cho nước đi
 *
 * Phòng 3-4 người (chủ phòng là trạm trung tâm, mọi nước đi đi qua chủ phòng rồi được phát lại cho các khách):
 *  H|gameId|tên|0|sốNgười              lời chào có thêm số người chơi
 *  P|tên0;tên1;...                     danh sách người trong phòng
 *  S3|ghếCủaBạn|sốNgười|tên0;tên1;...|t  bắt đầu ván, tên theo thứ tự ghế, t = mức thời gian mỗi nước do chủ phòng chọn
 *  B|ghế                               ghế này từ giờ do máy chơi (người đó đã rời/đầu hàng/hết giờ)
 *  X                                   chủ phòng báo: bạn đã bị xử thua (hết giờ)
 */
object Proto {
    fun hello(gameId: String, name: String, hostSide: Int, players: Int = 2) = "H|$gameId|$name|$hostSide|$players"
    fun roster(names: List<String>) = "P|" + names.joinToString(";")
    fun startMulti(seat: Int, players: Int, names: List<String>, time: Int) = "S3|$seat|$players|" + names.joinToString(";") + "|$time"
    fun botSeat(seat: Int) = "B|$seat"
    const val ELIMINATED = "X"
    /** Bỏ các ký tự dùng làm dấu ngăn cách khỏi biệt danh. */
    fun clean(name: String) = name.replace('|', ' ').replace(';', ' ').trim().take(16).ifEmpty { "?" }
    fun join(name: String) = "J|$name"
    /** time = mức thời gian mỗi nước do chủ phòng chọn (chỉ số trong Catalog.LEVEL_MOVE_SECONDS). */
    fun start(hostSide: Int, option: Int = 0, time: Int = 3) = "S|$hostSide|$option|$time"
    fun move(index: Int, move: Int) = "M|$index|$move"
    fun end(result: Int, hash: Int) = "E|$result|$hash"
    const val RESIGN = "R"
    const val QUIT = "Q"
    const val KICK = "K"
    const val REMATCH = "RM"
    /** Người gửi đã hết thời gian suy nghĩ cho nước đi hiện tại (tự xử thua). */
    const val TIMEOUT = "T"
}
