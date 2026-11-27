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
 */
object Proto {
    fun hello(gameId: String, name: String, hostSide: Int) = "H|$gameId|$name|$hostSide"
    fun join(name: String) = "J|$name"
    fun start(hostSide: Int, option: Int = 0) = "S|$hostSide|$option"
    fun move(index: Int, move: Int) = "M|$index|$move"
    fun end(result: Int, hash: Int) = "E|$result|$hash"
    const val RESIGN = "R"
    const val QUIT = "Q"
    const val KICK = "K"
    const val REMATCH = "RM"
}
