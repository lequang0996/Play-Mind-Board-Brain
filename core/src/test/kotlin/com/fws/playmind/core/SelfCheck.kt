package com.fws.playmind.core

import kotlin.system.exitProcess

private var failures = 0
private fun expect(name: String, cond: Boolean) {
    println((if (cond) "PASS  " else "FAIL  ") + name)
    if (!cond) failures++
}

private fun idx(x: Int, y: Int) = y * 15 + x
private fun sqName(m: Int) = "" + ('a' + ChessGame.fromSq(m) % 8) + ('1' + ChessGame.fromSq(m) / 8) + ('a' + ChessGame.toSq(m) % 8) + ('1' + ChessGame.toSq(m) / 8)

fun main() {
    // ── Cờ vua: perft theo bộ số chuẩn ──
    val c = ChessGame()
    expect("chess perft start 1-4", (1..4).map { c.perft(it) } == listOf(20L, 400L, 8902L, 197281L))
    c.setFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1")
    expect("chess perft kiwipete 1-3", (1..3).map { c.perft(it) } == listOf(48L, 2039L, 97862L))
    c.setFen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1")
    expect("chess perft pos3 1-4", (1..4).map { c.perft(it) } == listOf(14L, 191L, 2812L, 43238L))
    c.setFen("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1")
    expect("chess perft pos4 1-3 (phong cap, nhap thanh)", (1..3).map { c.perft(it) } == listOf(6L, 264L, 9467L))
    c.setFen("6k1/5ppp/8/8/8/8/5PPP/R5K1 w - - 0 1")
    expect("chess AI chieu het 1 nuoc", c.bestMove(1500) == ChessGame.mk(0, 56))
    c.setFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1")
    expect("chess hoa do het nuoc (stalemate)", c.result == Outcome.DRAW && c.reason == EndReason.STALEMATE)
    val f = ChessGame()
    fun sq(s: String) = (s[1] - '1') * 8 + (s[0] - 'a')
    val fool = listOf("f2" to "f3", "e7" to "e5", "g2" to "g4", "d8" to "h4").map { f.play(ChessGame.mk(sq(it.first), sq(it.second))) }
    expect("chess fool's mate", fool.all { it } && f.result == Outcome.WIN_SECOND && f.reason == EndReason.CHECKMATE)
    // chiến thuật (bộ WAC), mỗi thế cho 3 giây
    val puzzles = listOf(
        "2rr3k/pp3pp1/1nnqbN1p/3pN3/2pP4/2P3Q1/PPB4P/R4RK1 w - - 0 1" to "g3g6",
        "5rk1/1ppb3p/p1pb4/6q1/3P1p1r/2P1R2P/PP1BQ1P1/5RKN w - - 0 1" to "e3g3",
        "r1bq2rk/pp3pbp/2p1p1pQ/7P/3P4/2PB1N2/PP3PPR/2KR4 w - - 0 1" to "h6h7",
        "5k2/6pp/p1qN4/1p1p4/3P4/2PKP2Q/PP3r2/3R4 b - - 0 1" to "c6c4",
    )
    var solved = 0
    for ((fen, best) in puzzles) { c.setFen(fen); if (sqName(c.bestMove(3000)) == best) solved++ }
    expect("chess tactics $solved/${puzzles.size} (can >= 3)", solved >= 3)
    // sách khai cuộc: nước đầu tiên phải hợp lệ
    val bk = ChessGame(); val first = bk.bestMove(500)
    expect("chess nuoc dau (sach khai cuoc) hop le", first in bk.legalMoves())

    // ── Cờ tướng ──
    val x = XiangqiGame()
    expect("xiangqi perft start 1-3", (1..3).map { x.perft(it) } == listOf(44L, 1920L, 79666L))
    val pin = XiangqiGame()
    pin.setPosition(mapOf(4 to 1, 85 to -1, 40 to 5, 8 to 5), true)
    expect("xiangqi tuong doi mat: xe bi ghim chi di tren cot", pin.legalMoves().filter { XiangqiGame.fromSq(it) == 40 }.all { XiangqiGame.toSq(it) % 9 == 4 })
    val mate = XiangqiGame()
    mate.setPosition(mapOf(3 to 1, 85 to -1, 81 to 5, 72 to 5), false)
    expect("xiangqi chieu bi", mate.result == Outcome.WIN_FIRST && mate.reason == EndReason.CHECKMATE)
    // xe ăn xe đối phương không được bảo vệ: Đỏ xe a1(0) ăn xe đen a10 (81) trên cột a đang mở
    val cap = XiangqiGame()
    cap.setPosition(mapOf(4 to 1, 85 to -1, 0 to 5, 81 to -5, 40 to 7), true)
    val capMove = cap.bestMove(1500)
    expect("xiangqi AI an xe khong duoc bao ve", XiangqiGame.toSq(capMove) == 81 && XiangqiGame.fromSq(capMove) == 0)
    val move = x.bestMove(800)
    expect("xiangqi AI tra nuoc hop le", move in x.legalMoves())

    // ── Cờ caro ──
    val g = GomokuGame()
    for (xx in 3..7) { g.play(idx(xx, 7)); if (xx < 7) g.play(idx(xx, (xx - 3) * 2)) }
    expect("caro 5 quan thang", g.result == Outcome.WIN_FIRST)
    val bb = GomokuGame()
    listOf(idx(3, 5), idx(2, 5), idx(4, 5), idx(8, 5), idx(5, 5), idx(0, 0), idx(6, 5), idx(14, 14), idx(7, 5)).forEach { bb.play(it) }
    expect("caro chan hai dau khong thang", bb.result == Outcome.ONGOING)
    val a = GomokuGame()
    listOf(idx(4, 7), idx(4, 3), idx(5, 7), idx(9, 3), idx(6, 7), idx(12, 12), idx(7, 7), idx(1, 12)).forEach { a.play(it) }
    val m1 = a.bestMove(800)
    expect("caro AI hoan thanh 5", m1 == idx(3, 7) || m1 == idx(8, 7))
    val d = GomokuGame()
    listOf(idx(4, 7), idx(3, 7), idx(5, 7), idx(12, 1), idx(6, 7), idx(12, 12), idx(7, 7)).forEach { d.play(it) }
    expect("caro AI chan day 4", d.bestMove(800) == idx(8, 7))
    // VCF: đen đi (7,7) tạo bốn-bốn (hàng ngang và cột dọc đều bị chặn một đầu) => thắng
    val v = GomokuGame()
    listOf(idx(4, 7), idx(3, 7), idx(5, 7), idx(7, 3), idx(6, 7), idx(0, 0), idx(7, 4), idx(14, 0), idx(7, 5), idx(0, 14), idx(7, 6), idx(14, 14))
        .forEach { v.play(it) }
    expect("caro AI tim bon-bon (VCF)", v.bestMove(1500) == idx(7, 7))
    // phòng thủ ba hở
    val o = GomokuGame()
    listOf(idx(7, 7), idx(7, 8), idx(8, 7), idx(8, 8), idx(9, 7)).forEach { o.play(it) }
    val ob = o.bestMove(800)
    expect("caro AI chan ba ho", ob == idx(6, 7) || ob == idx(10, 7) || ob == idx(5, 7) || ob == idx(11, 7))

    println(if (failures == 0) "ALL CHECKS PASSED" else "$failures CHECK(S) FAILED")
    if (failures > 0) exitProcess(1)
}
