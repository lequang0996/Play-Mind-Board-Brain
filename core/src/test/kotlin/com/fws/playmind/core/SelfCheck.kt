package com.fws.playmind.core

import kotlin.system.exitProcess

private var failures = 0
private fun expect(name: String, cond: Boolean) {
    println((if (cond) "PASS  " else "FAIL  ") + name)
    if (!cond) failures++
}

/** Tọa độ trong các bài test cũ là 0..14 trên bàn 15x15; trên bàn vô hạn dịch về quanh gốc (0,0). */
private fun idx(x: Int, y: Int) = GomokuGame.pack(x - 7, y - 7)
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
    // engine bitboard (ChessEngine) phải sinh nước giống hệt luật cờ
    val ce = ChessEngine(12)
    val ceCases = listOf(
        Triple(ChessEngine.START_FEN, 4, 197281L),
        Triple("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1", 3, 97862L),
        Triple("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1", 4, 43238L),
        Triple("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1", 3, 9467L),
        Triple("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8", 3, 62379L),
    )
    expect("chess engine bitboard perft (5 the chuan)", ceCases.all { (fen, d, n) -> ce.setFen(fen); ce.perft(d) == n })
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
    // engine XiangqiEngine phải sinh nước giống hệt luật trong XiangqiGame (thế đầu + 60 thế đánh ngẫu nhiên)
    val xe = XiangqiEngine(12)
    var xeOk = xe.perft(3) == 79666L
    val xr = kotlin.random.Random(7)
    val xStart = IntArray(90) { XiangqiGame().pieceAt(it) }
    repeat(60) {
        val xg = XiangqiGame()
        repeat(xr.nextInt(100)) { val ms = xg.legalMoves(); if (ms.isNotEmpty()) xg.play(ms[xr.nextInt(ms.size)]) }
        if (xg.result == Outcome.ONGOING) {
            xe.load(xStart, true, xg.history)
            if (xe.legalMoves().sorted() != xg.legalMoves().sorted() || xe.perft(2) != xg.perft(2)) xeOk = false
        }
    }
    expect("xiangqi engine sinh nuoc khop luat (perft + 60 the ngau nhien)", xeOk)
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

    // ── Cờ caro bàn vô hạn ──
    val fx = 5000; val fy = -3000
    val far = GomokuGame()
    for (i in 0 until 5) { far.play(GomokuGame.pack(fx + i, fy)); if (i < 4) far.play(GomokuGame.pack(fx + i, fy + 2 + i * 3)) }
    expect("caro vo han: thang o toa do xa goc", far.result == Outcome.WIN_FIRST && far.winLine.size == 5)
    val neg = GomokuGame()
    for (i in 0 until 5) { neg.play(GomokuGame.pack(-20, -30 - i)); if (i < 4) neg.play(GomokuGame.pack(-10 + i * 2, -3)) }
    expect("caro vo han: thang theo cot, toa do am", neg.result == Outcome.WIN_FIRST)
    val six = GomokuGame()
    listOf(1, 2, 3, 5, 6).forEachIndexed { i, x -> six.play(GomokuGame.pack(x, 0)); six.play(GomokuGame.pack(x, 10 + i * 3)) }
    six.play(GomokuGame.pack(4, 0))
    expect("caro vo han: 6 quan lien tiep van thang", six.result == Outcome.WIN_FIRST && six.winLine.size == 6)
    val dup = GomokuGame()
    dup.play(GomokuGame.pack(0, 0))
    expect("caro vo han: khong dat len o da co / ngoai pham vi", !dup.play(GomokuGame.pack(0, 0)) && !dup.play(Int.MAX_VALUE) && !dup.play(-1))
    // AI trên bàn vô hạn: hoàn thành 5 ở xa gốc tọa độ
    val fa = GomokuGame()
    listOf(4 to 7, 4 to 3, 5 to 7, 9 to 3, 6 to 7, 12 to 12, 7 to 7, 1 to 12).forEach { (x, y) -> fa.play(GomokuGame.pack(fx + x, fy + y)) }
    val fm = fa.bestMove(800)
    expect("caro vo han: AI hoan thanh 5 o xa goc", fm == GomokuGame.pack(fx + 3, fy + 7) || fm == GomokuGame.pack(fx + 8, fy + 7))
    // AI khi quân rải rộng hơn cửa sổ 24: vẫn thấy hàng bốn của mình dù nước vừa đi của đối thủ ở rất xa
    val wide = GomokuGame()
    val xs = listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0)
    val os = listOf(40 to 40, -40 to 35, 35 to -38, -30 to -30)
    for (i in xs.indices) { wide.play(GomokuGame.pack(xs[i].first, xs[i].second)); wide.play(GomokuGame.pack(os[i].first, os[i].second)) }
    val wm = wide.bestMove(800)
    expect("caro vo han: AI thang hang bon khi quan rai rong", wm == GomokuGame.pack(-1, 0) || wm == GomokuGame.pack(4, 0))
    val ai = GomokuGame()
    var legalAll = true
    var n = 0
    while (n < 30 && ai.result == Outcome.ONGOING) { val m = ai.bestMove(60); if (!ai.play(m)) legalAll = false; n++ }
    expect("caro vo han: bot tu choi toi 30 nuoc deu hop le", legalAll && ai.plies >= 9)

    // ── Sudoku ──
    for (lv in SudokuLevel.values()) {
        var unique = true; var valid = true
        for (i in 0 until 6) {
            val sg = SudokuGame.createClassic(lv, kotlin.random.Random(100L * lv.ordinal + i))
            if (SudokuSolver().count(sg.givens, 2) != 1) unique = false
            for (k in 0 until 81) if (sg.givens[k] != 0 && sg.givens[k] != sg.solution[k]) valid = false
            for (r in 0 until 9) if ((0 until 9).map { sg.solution[r * 9 + it] }.toSet().size != 9) valid = false
        }
        expect("sudoku $lv: de co dung 1 nghiem va loi giai hop le", unique && valid)
    }
    expect("sudoku EASY/MEDIUM giai duoc bang o duy nhat", listOf(SudokuLevel.EASY, SudokuLevel.MEDIUM).all { lv ->
        (0 until 5).all { SudokuGenerator.solvableBySingles(SudokuGame.createClassic(lv, kotlin.random.Random(900L + it)).givens) } })
    // Đề chuẩn: mỗi khối 3x3 đúng 1 số cho sẵn
    expect("sudoku: moi khoi 3x3 chi goi y 1 so (ca ban 9 so)", (0 until 20).all { seed ->
        val g = SudokuGame.create(SudokuLevel.MEDIUM, kotlin.random.Random(500L + seed))
        g.givens.count { it != 0 } == 9 && (0 until 9).all { b ->
            (0 until 9).count { k -> g.givens[((b / 3) * 3 + k / 3) * 9 + (b % 3) * 3 + k % 3] != 0 } == 1 } &&
            (0 until 81).all { g.givens[it] == 0 || g.givens[it] == g.solution[it] }
    })
    val sd = SudokuGame.create(SudokuLevel.MEDIUM, kotlin.random.Random(3))
    val se = (0 until 81).first { sd.givens[it] == 0 }
    // số trùng với một số cho sẵn cùng hàng/cột/khối => chắc chắn sai
    val sw = SudokuGame.peerCells(se).map { sd.givens[it] }.first { it != 0 }
    expect("sudoku: dien sai tinh 1 loi va hien do", sd.place(se, sw) == SudokuPlace.MISTAKE && sd.mistakes == 1 && sd.isWrong(se))
    expect("sudoku: hoan tac", sd.undo() && sd.cells[se] == 0)
    expect("sudoku: dien dung va thang khi du 81 o", run { for (i in 0 until 81) if (sd.cells[i] != sd.solution[i]) sd.place(i, sd.solution[i]); sd.status == SudokuStatus.WON })
    val sl = SudokuGame.create(SudokuLevel.EASY, kotlin.random.Random(4))
    for (e in (0 until 81).filter { sl.givens[it] == 0 && SudokuGame.peerCells(it).any { p -> sl.givens[p] != 0 } }.take(3))
        sl.place(e, SudokuGame.peerCells(e).map { sl.givens[it] }.first { it != 0 })
    // một lời giải khác lời giải mẫu vẫn được chấp nhận: hoán đổi hai chữ số không xuất hiện trong ô cho sẵn
    val alt = SudokuGame.create(SudokuLevel.MEDIUM, kotlin.random.Random(8))
    val freeDigits = (1..9).filter { d -> alt.givens.none { it == d } }
    val altOk = freeDigits.size < 2 || run {
        val a = freeDigits[0]; val b = freeDigits[1]
        for (i in 0 until 81) if (!alt.isGiven(i)) alt.place(i, when (alt.solution[i]) { a -> b; b -> a; else -> alt.solution[i] })
        alt.status == SudokuStatus.WON && alt.mistakes == 0
    }
    expect("sudoku: chap nhan moi loi giai dung luat", altOk)
    // gợi ý điền nốt ô cuối cùng và thắng ván
    val dz = SudokuGame.create(SudokuLevel.MEDIUM, kotlin.random.Random(9))
    val lastFree = (0 until 81).last { !dz.isGiven(it) }
    for (i in 0 until 81) if (!dz.isGiven(i) && i != lastFree) dz.place(i, dz.solution[i])
    expect("sudoku: o hien tai hop le khi dien theo loi giai", (0 until 81).none { dz.isWrong(it) } && dz.hint() >= 0 && dz.status == SudokuStatus.WON)
    expect("sudoku: sai nhieu loi van khong thua, chi dem loi", sl.status == SudokuStatus.PLAYING && sl.mistakes == 3)

    // ── Wall Go ──
    expect("wallgo: thu tu dat quan 2/3/4 nguoi", (2..4).all { p -> val o = WallGoGame.setupOrder(p); (0 until p).all { q -> o.count { it == q } == WallGoGame.stonesPerPlayer(p) } })
    val wgw = WallGoGame(2)
    expect("wallgo: tu choi nuoc di bat hop le", !wgw.play(49) && !wgw.play(-1) && !wgw.play(WallGoGame.mvMove(0, 1, 0)) && wgw.play(24) && !wgw.play(24))
    var wgOk = true; var wgFinished = true; var wgReplay = true
    val wgRnd = kotlin.random.Random(11)
    for (players in 2..4) {
        val wg = WallGoGame(players)
        var guard = 0
        while (wg.result == Outcome.ONGOING && guard++ < 500) {
            val legal = wg.legalMoves()
            if (legal.isEmpty()) { wgOk = false; break }
            val mv = if (guard % 3 == 0) wg.bestMove(300) else legal[wgRnd.nextInt(legal.size)]
            if (mv !in legal || !wg.play(mv)) { wgOk = false; break }
        }
        if (wg.result == Outcome.ONGOING) wgFinished = false
        val rp = WallGoGame(players); for (m in wg.history) rp.play(m)
        if (rp.result != wg.result || rp.winner != wg.winner || movesHash(rp.history) != movesHash(wg.history)) wgReplay = false
    }
    expect("wallgo: 2/3/4 nguoi luon con nuoc hop le va bot di hop le", wgOk)
    expect("wallgo: van dau luon ket thuc theo luat lanh tho", wgFinished)
    expect("wallgo: phat lai lich su cho ket qua giong het (dong bo online)", wgReplay)
    var aiWins = 0
    for (gi in 0 until 6) {
        val wg = WallGoGame(2); val seat = gi % 2; var guard = 0
        while (wg.result == Outcome.ONGOING && guard++ < 500) {
            val lg = wg.legalMoves(); wg.play(if (wg.turn == seat) wg.bestMove(500) else lg[wgRnd.nextInt(lg.size)])
        }
        if (wg.winner == seat) aiWins++
    }
    expect("wallgo: bot thang >= 5/6 van truoc nguoi choi ngau nhien ($aiWins/6)", aiWins >= 5)

    // ── Quoridor ──
    val qd0 = QuoridorGame(2)
    expect("quoridor: nuoc di quan ban dau", qd0.pawnMoves(0).toSet() == setOf(67, 75, 77))
    expect("quoridor: tuong cat cheo/chong bi tu choi", qd0.play(QuoridorGame.mvWall(true, 0, 0)) && !qd0.play(QuoridorGame.mvWall(false, 0, 0)) && !qd0.play(QuoridorGame.mvWall(true, 0, 1)))
    var qOk = true; var qFin = true; var qReplay = true
    val qRnd = kotlin.random.Random(31)
    for (players in intArrayOf(2, 4)) {
        val q = QuoridorGame(players); var guard = 0
        while (q.result == Outcome.ONGOING && guard++ < 400) {
            val legal = q.legalMoves(); if (legal.isEmpty()) { qOk = false; break }
            val mv = if (guard % 4 == 0) q.bestMove(200) else legal[qRnd.nextInt(legal.size)]
            if (mv !in legal || !q.play(mv)) { qOk = false; break }
        }
        if (q.result == Outcome.ONGOING) qFin = false
        val rp = QuoridorGame(players); for (m in q.history) rp.play(m)
        if (rp.result != q.result || movesHash(rp.history) != movesHash(q.history)) qReplay = false
    }
    expect("quoridor: 2/4 nguoi luon con nuoc hop le, bot di hop le", qOk)
    expect("quoridor: van dau luon ket thuc (thang hoac hoa do gioi han nuoc)", qFin)
    expect("quoridor: phat lai lich su cho ket qua giong het", qReplay)
    var qAi = 0
    for (gi in 0 until 4) {
        val q = QuoridorGame(2); val seat = gi % 2; var guard = 0
        while (q.result == Outcome.ONGOING && guard++ < 300) { val lg = q.legalMoves(); q.play(if (q.turn == seat) q.bestMove(300) else lg[qRnd.nextInt(lg.size)]) }
        if (q.winner == seat) qAi++
    }
    expect("quoridor: bot thang >= 3/4 van truoc nguoi choi ngau nhien ($qAi/4)", qAi >= 3)

    // ── Co vay ──
    fun gx(x: Int, y: Int) = y * 9 + x
    expect("go: bat quan (quan trang o goc het khi den vay du hai phia)", run { val t = GoGame(9); t.play(gx(1, 0)); t.play(gx(0, 0)); t.play(gx(0, 1)); t.stoneAt(gx(0, 0)) == 0 })
    val gs = GoGame(9); gs.play(gx(1, 0)); gs.play(gx(4, 4)); gs.play(gx(0, 1))
    expect("go: cam tu sat", !gs.isLegal(gx(0, 0)))
    val gScore = GoGame(9); gScore.play(gx(4, 4)); gScore.play(GoGame.PASS); gScore.play(GoGame.PASS)
    expect("go: hai lan bo luot ket thuc van va tinh dien tich (81-0, den thang)", gScore.result == Outcome.WIN_FIRST && gScore.finalBlack == 81)
    var goOk = true
    val goRnd = kotlin.random.Random(41)
    for (size in intArrayOf(9, 13)) {
        val gg = GoGame(size); var guard = 0
        while (gg.result == Outcome.ONGOING && guard++ < 160) {
            val legal = gg.legalMoves().filter { it != GoGame.PASS }
            val mv = if (legal.isEmpty() || guard > 120) GoGame.PASS else if (guard % 40 == 0) gg.bestMove(100) else legal[goRnd.nextInt(legal.size)]
            if (!gg.play(mv)) { goOk = false; break }
        }
        val rp = GoGame(size); for (m in gg.history) if (!rp.play(m)) goOk = false
        if (movesHash(rp.history) != movesHash(gg.history) || rp.result != gg.result) goOk = false
    }
    expect("go: 9x9 va 13x13 choi ngau nhien + bot hop le, phat lai giong het (co superko)", goOk)

    // ── Luật lặp thế của app: nước tạo lại thế cũ (cùng bên tới lượt) thì bên vừa đi THUA ──
    run {
        val c = ChessGame()
        for ((f, t) in listOf(6 to 21, 62 to 45, 21 to 6)) c.play(c.findMove(f, t))
        val okBefore = c.result == Outcome.ONGOING
        c.play(c.findMove(45, 62))                       // Đen đưa mã về: thế ban đầu lặp lại -> Đen thua
        expect("chess: lap the lan 2 -> ben vua di thua", okBefore && c.result == Outcome.WIN_FIRST && c.reason == EndReason.REPETITION)
    }
    run {
        val x = XiangqiGame()
        val seq = listOf(1 to 20, 82 to 65, 20 to 1, 65 to 82)
        var ok = true
        for ((f, t) in seq) { val m = x.findMove(f, t); if (m < 0 || !x.play(m)) ok = false }
        expect("xiangqi: lap the lan 2 -> ben vua di thua", ok && x.result == Outcome.WIN_FIRST && x.reason == EndReason.REPETITION)
    }
    run {
        val q = QuoridorGame(2)
        for (m in listOf(67, 13, 76)) q.play(m)
        val okBefore = q.result == Outcome.ONGOING
        q.play(4)                                         // bên 1 quay về: thế đầu lặp lại -> bên 1 thua
        expect("quoridor 2 nguoi: lap the -> ben vua di thua", okBefore && q.result == Outcome.WIN_FIRST && q.reason == EndReason.REPETITION)
        val q4 = QuoridorGame(4)
        var ok = true
        for (m in listOf(67, 37, 13, 43, 76, 36, 4)) if (!q4.play(m)) ok = false
        expect("quoridor 4 nguoi: nuoc lap the bi cam", ok && 44 !in q4.legalMoves() && !q4.play(44) && q4.result == Outcome.ONGOING)
    }

    // ── 7 cấp bot: mọi cấp đều đi nước hợp lệ ──
    run {
        val makers: List<() -> BoardGame> = listOf({ ChessGame() }, { XiangqiGame() }, { GomokuGame() }, { QuoridorGame(2) }, { QuoridorGame(4) }, { WallGoGame(2) }, { WallGoGame(3) })
        var ok = true; val bad = ArrayList<String>()
        val rnd = kotlin.random.Random(5)
        for (mk in makers) {
            val g = mk()
            // đi tới giữa ván bằng nước ngẫu nhiên hợp lệ
            repeat(10) { val ms = g.legalMoves(); if (ms.isNotEmpty() && g.result == Outcome.ONGOING) g.play(ms[rnd.nextInt(ms.size)]) }
            if (g.result != Outcome.ONGOING) continue
            for (lv in BotLevel.entries) {
                val c = g.copyGame()
                val m = (c as LeveledEngine).levelMove(lv, 200, kotlin.random.Random(lv.ordinal))
                if (m < 0 || !c.play(m)) { ok = false; bad.add("${g.id}/$lv") }
            }
        }
        expect("7 cap bot: moi cap di nuoc hop le " + bad, ok)
    }

    // ── Lớp an toàn (BOT_GUIDE mục 4.2): cấp Dễ luôn đi nước thắng ngay và không để đối phương thắng ngay ──
    run {
        var win = 0
        for (s in 0 until 10) {
            val c = ChessGame(); c.setFen("6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1")
            val m = c.levelMove(BotLevel.EASY, 200, kotlin.random.Random(s))
            if (c.play(m) && c.result == Outcome.WIN_FIRST) win++
        }
        expect("an toan: cap De luon chieu het trong 1 nuoc ($win/10)", win == 10)
        var safe = 0
        for (s in 0 until 10) {
            val c = ChessGame(); c.setFen("3r2k1/5ppp/8/8/8/8/5PPP/6K1 w - - 0 1")
            val m = c.levelMove(BotLevel.EASY, 200, kotlin.random.Random(s))
            if (c.play(m) && !Bot.genericLosesNext(ChessGame().also { it.setFen("3r2k1/5ppp/8/8/8/8/5PPP/6K1 w - - 0 1") }, m)) safe++
        }
        expect("an toan: cap De khong de bi chieu het ngay ($safe/10)", safe == 10)
    }

    println(if (failures == 0) "ALL CHECKS PASSED" else "$failures CHECK(S) FAILED")
    if (failures > 0) exitProcess(1)
}
