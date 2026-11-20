package com.fws.playmind.core

import kotlin.system.exitProcess

private var failures = 0
private fun expect(name: String, cond: Boolean) {
    println((if (cond) "PASS  " else "FAIL  ") + name)
    if (!cond) failures++
}

private fun idx(x: Int, y: Int) = y * 15 + x

fun main() {
    // ── Cờ vua: perft theo bộ số chuẩn ──
    val c = ChessGame()
    expect("chess perft start 1-4", (1..4).map { c.perft(it) } == listOf(20L, 400L, 8902L, 197281L))
    c.setFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1")
    expect("chess perft kiwipete 1-3", (1..3).map { c.perft(it) } == listOf(48L, 2039L, 97862L))
    c.setFen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1")
    expect("chess perft pos3 1-4", (1..4).map { c.perft(it) } == listOf(14L, 191L, 2812L, 43238L))
    c.setFen("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1")
    expect("chess perft pos4 1-3 (phong cấp, nhập thành)", (1..3).map { c.perft(it) } == listOf(6L, 264L, 9467L))
    c.setFen("6k1/5ppp/8/8/8/8/5PPP/R5K1 w - - 0 1")
    expect("chess AI chiếu hết 1 nước", c.bestMove(1500) == ChessGame.mk(0, 56))
    c.setFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1")
    expect("chess hòa do hết nước (stalemate)", c.result == Outcome.DRAW && c.reason == EndReason.STALEMATE)
    val f = ChessGame()
    fun sq(s: String) = (s[1] - '1') * 8 + (s[0] - 'a')
    val fool = listOf("f2" to "f3", "e7" to "e5", "g2" to "g4", "d8" to "h4").map { f.play(ChessGame.mk(sq(it.first), sq(it.second))) }
    expect("chess fool's mate", fool.all { it } && f.result == Outcome.WIN_SECOND && f.reason == EndReason.CHECKMATE)

    // ── Cờ tướng ──
    val x = XiangqiGame()
    expect("xiangqi perft start 1-3", (1..3).map { x.perft(it) } == listOf(44L, 1920L, 79666L))
    val pin = XiangqiGame()
    pin.setPosition(mapOf(4 to 1, 85 to -1, 40 to 5, 8 to 5), true)
    expect("xiangqi tướng đối mặt: xe bị ghim chỉ đi trên cột", pin.legalMoves().filter { XiangqiGame.fromSq(it) == 40 }.all { XiangqiGame.toSq(it) % 9 == 4 })
    val mate = XiangqiGame()
    mate.setPosition(mapOf(3 to 1, 85 to -1, 81 to 5, 72 to 5), false)
    expect("xiangqi chiếu bí", mate.result == Outcome.WIN_FIRST && mate.reason == EndReason.CHECKMATE)
    val move = x.bestMove(800)
    expect("xiangqi AI trả nước hợp lệ", move in x.legalMoves())

    // ── Cờ caro ──
    val g = GomokuGame()
    for (xx in 3..7) { g.play(idx(xx, 7)); if (xx < 7) g.play(idx(xx, (xx - 3) * 2)) }
    expect("caro 5 quân thắng", g.result == Outcome.WIN_FIRST)
    val bb = GomokuGame()
    listOf(idx(3, 5), idx(2, 5), idx(4, 5), idx(8, 5), idx(5, 5), idx(0, 0), idx(6, 5), idx(14, 14), idx(7, 5)).forEach { bb.play(it) }
    expect("caro chặn hai đầu không thắng", bb.result == Outcome.ONGOING)
    val a = GomokuGame()
    listOf(idx(4, 7), idx(4, 3), idx(5, 7), idx(9, 3), idx(6, 7), idx(12, 12), idx(7, 7), idx(1, 12)).forEach { a.play(it) }
    val m1 = a.bestMove(800)
    expect("caro AI hoàn thành 5", m1 == idx(3, 7) || m1 == idx(8, 7))
    val d = GomokuGame()
    listOf(idx(4, 7), idx(3, 7), idx(5, 7), idx(12, 1), idx(6, 7), idx(12, 12), idx(7, 7)).forEach { d.play(it) }
    expect("caro AI chặn dãy 4", d.bestMove(800) == idx(8, 7))

    println(if (failures == 0) "ALL CHECKS PASSED" else "$failures CHECK(S) FAILED")
    if (failures > 0) exitProcess(1)
}
