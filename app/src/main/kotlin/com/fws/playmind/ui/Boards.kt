package com.fws.playmind.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.fws.playmind.core.ChessGame
import com.fws.playmind.core.GomokuGame
import com.fws.playmind.core.Outcome
import com.fws.playmind.core.XiangqiGame
import com.fws.playmind.match.SessionViewModel
import kotlin.math.abs
import kotlin.math.min

// ───────────────────────── Quân cờ vua (vẽ vector, hệ tọa độ 100x100) ─────────────────────────
private object PieceArt {
    private fun p(vararg d: String) = d.map { PathParser().parsePathString(it).toPath() }
    private const val BASE = "M24 90H76V82Q76 78 72 78H28Q24 78 24 82Z"
    val fills: Map<Int, List<Path>> by lazy {
        mapOf(
            ChessGame.P to p(BASE, "M38 30a12 12 0 1 0 24 0a12 12 0 1 0 -24 0Z", "M43 40Q40 52 34 62Q30 70 30 78H70Q70 70 66 62Q60 52 57 40Z"),
            ChessGame.R to p(BASE, "M32 42H68L72 78H28Z", "M27 20H39V28H45V20H55V28H61V20H73V42H27Z"),
            ChessGame.N to p(BASE, "M72 78H28Q28 62 38 52Q44 46 42 40Q36 42 30 46L24 44Q24 38 34 28Q38 24 40 16L46 22Q50 18 56 18Q74 22 72 50Q70 64 72 78Z"),
            ChessGame.B to p(BASE, "M46 11a4 4 0 1 0 8 0a4 4 0 1 0 -8 0Z", "M50 15Q64 26 62 40Q61 46 56 50H44Q39 46 38 40Q36 26 50 15Z", "M40 52H60V58H40Z", "M44 58H56Q58 68 66 78H34Q42 68 44 58Z"),
            ChessGame.Q to p(BASE, "M30 78L22 34L38 52L36 26L46 50L50 22L54 50L64 26L62 52L78 34L70 78Z",
                "M18 33a4 4 0 1 0 8 0a4 4 0 1 0 -8 0Z", "M32 25a4 4 0 1 0 8 0a4 4 0 1 0 -8 0Z", "M46 21a4 4 0 1 0 8 0a4 4 0 1 0 -8 0Z",
                "M60 25a4 4 0 1 0 8 0a4 4 0 1 0 -8 0Z", "M74 33a4 4 0 1 0 8 0a4 4 0 1 0 -8 0Z"),
            ChessGame.K to p(BASE, "M47 6H53V13H60V19H53V26H47V19H40V13H47Z", "M50 27Q72 31 74 52Q74 58 68 60L72 78H28L32 60Q26 58 26 52Q28 31 50 27Z"),
        )
    }
    val details: Map<Int, Path> by lazy {
        mapOf(
            ChessGame.N to PathParser().parsePathString("M48 31a2.4 2.4 0 1 0 4.8 0a2.4 2.4 0 1 0 -4.8 0Z").toPath(),
            ChessGame.B to PathParser().parsePathString("M46 38L54 28").toPath(),
            ChessGame.Q to PathParser().parsePathString("M34 64H66").toPath(),
            ChessGame.K to PathParser().parsePathString("M34 64H66").toPath(),
            ChessGame.R to PathParser().parsePathString("M32 52H68").toPath(),
        )
    }
}

fun DrawScope.drawChessPiece(type: Int, white: Boolean, topLeft: Offset, cell: Float) {
    val sc = cell * 0.84f / 100f
    val fill = if (white)
        Brush.linearGradient(listOf(Color(0xF5FFFFFF), Color(0xE0D3E8FF), Color(0xEBA3C2F2)), Offset(10f, 5f), Offset(90f, 100f))
    else
        Brush.linearGradient(listOf(GlassPalette.obsHi, GlassPalette.obs, GlassPalette.obsLo), Offset(10f, 5f), Offset(90f, 100f))
    val edge = if (white) Color(0xFF5A82C0) else Color(0xFFC9C3F5)
    val thick = if (white) Color(0x669BB4E0) else Color(0xAA05061A)
    val sheen = Brush.linearGradient(listOf(Color.White.copy(alpha = if (white) .9f else .42f), Color.Transparent), Offset(15f, 5f), Offset(65f, 60f))
    // bóng đổ dưới chân quân
    drawOval(Color.Black.copy(alpha = .2f), Offset(topLeft.x + cell * .2f, topLeft.y + cell * .83f), Size(cell * .6f, cell * .09f))
    withTransform({ translate(topLeft.x + cell * 0.08f, topLeft.y + cell * 0.1f); scale(sc, sc, pivot = Offset.Zero) }) {
        val parts = PieceArt.fills[type]
        // độ dày của khối kính (bản sao lệch xuống dưới-phải)
        parts?.forEach { path -> withTransform({ translate(1.6f, 2.6f) }) { drawPath(path, thick) } }
        parts?.forEach { path ->
            drawPath(path, fill)
            clipPath(path) { drawRect(sheen, Offset.Zero, Size(100f, 100f)) }
            drawPath(path, edge, style = Stroke(width = 2.6f, join = StrokeJoin.Round, cap = StrokeCap.Round))
        }
        PieceArt.details[type]?.let { d ->
            if (type == ChessGame.N) drawPath(d, edge) else drawPath(d, edge, style = Stroke(width = 2.4f, cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun PieceChoice(type: Int, white: Boolean, onClick: () -> Unit) {
    Canvas(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(pm.surfaceHigh).clickable(onClick = onClick)) {
        drawChessPiece(type, white, Offset.Zero, size.width)
    }
}

private fun Modifier.boardFrame() = this.fillMaxWidth().padding(2.dp)

// ───────────────────────── Cờ vua ─────────────────────────
@Composable
fun ChessBoardView(vm: SessionViewModel, modifier: Modifier = Modifier) {
    val g = vm.game as? ChessGame ?: return
    val tick = vm.tick
    val flip = vm.mySide == 1
    var sel by remember { mutableIntStateOf(-1) }
    var promo by remember { mutableStateOf<List<Int>?>(null) }
    LaunchedEffect(tick) { sel = -1; promo = null }
    val legal = remember(tick) { g.legalMoves() }
    val targets = if (sel >= 0) legal.filter { ChessGame.fromSq(it) == sel }.map { ChessGame.toSq(it) } else emptyList()
    val c = pm
    val tm = rememberTextMeasurer()

    Canvas(
        modifier.boardFrame().aspectRatio(1f).clip(RoundedCornerShape(10.dp))
            .pointerInput(tick, flip, sel) {
                detectTapGestures { off ->
                    if (!vm.myTurn) return@detectTapGestures
                    val s = size.width / 8f
                    val col = (off.x / s).toInt().coerceIn(0, 7); val row = (off.y / s).toInt().coerceIn(0, 7)
                    val sq = if (flip) row * 8 + (7 - col) else (7 - row) * 8 + col
                    if (sel >= 0 && sq in targets) {
                        val ms = legal.filter { ChessGame.fromSq(it) == sel && ChessGame.toSq(it) == sq }
                        if (ms.size > 1) promo = ms else vm.playMove(ms[0])
                        sel = -1
                    } else {
                        val p = g.board[sq]
                        sel = if (p != 0 && (p > 0) == (vm.mySide == 0) && legal.any { ChessGame.fromSq(it) == sq }) sq else -1
                    }
                }
            },
    ) {
        @Suppress("UNUSED_VARIABLE") val dep = tick
        val s = size.width / 8f
        fun pos(sq: Int): Offset { val f = sq and 7; val r = sq shr 3; return if (flip) Offset((7 - f) * s, r * s) else Offset(f * s, (7 - r) * s) }
        val lightSq = if (c.dark) Color(0xFF7388C9) else Color(0xFFE3EEFA)
        val darkSq = if (c.dark) Color(0xFF34417F) else Color(0xFF9BBFE3)
        for (sq in 0 until 64) {
            val light = ((sq and 7) + (sq shr 3)) % 2 == 1
            val o = pos(sq)
            drawRect(if (light) lightSq else darkSq, o, Size(s, s))
            // vát kính: sáng ở trên-trái
            drawRect(Brush.linearGradient(listOf(Color.White.copy(alpha = if (light) .7f else .35f), Color.Transparent), o, o + Offset(s, s)), o, Size(s, s))
        }
        drawGlassSheen(c.dark)
        val last = g.lastMove
        if (last >= 0) for (q in listOf(ChessGame.fromSq(last), ChessGame.toSq(last))) drawRect(c.gold.copy(alpha = .38f), pos(q), Size(s, s))
        if (g.result == Outcome.ONGOING && g.inCheck()) {
            val k = pos(g.kingSquare(g.side))
            drawCircle(Brush.radialGradient(listOf(c.danger.copy(alpha = .85f), Color.Transparent), k + Offset(s / 2, s / 2), s * .62f), s * .62f, k + Offset(s / 2, s / 2))
        }
        if (sel >= 0) drawRect(c.gold.copy(alpha = .55f), pos(sel), Size(s, s))
        // toạ độ
        val lab = TextStyle(color = (if (c.dark) Color.White else Color(0xFF2F4F7A)).copy(alpha = .6f), fontSize = (s * .17f).toSp(), fontWeight = FontWeight.SemiBold)
        for (i in 0 until 8) {
            val fileChar = ('a' + (if (flip) 7 - i else i)).toString()
            drawText(tm.measure(fileChar, lab), topLeft = Offset(i * s + s * .06f, size.height - s * .24f))
            val rankChar = ('1' + (if (flip) i else 7 - i)).toString()
            drawText(tm.measure(rankChar, lab), topLeft = Offset(size.width - s * .16f, i * s + s * .04f))
        }
        for (sq in 0 until 64) {
            val p = g.board[sq]; if (p == 0) continue
            drawChessPiece(abs(p), p > 0, pos(sq), s)
        }
        drawRect(if (c.dark) Color(0xFF9AA8E0).copy(alpha = .5f) else Color(0xFF8FB0D8), Offset.Zero, size, style = Stroke(s * .07f))
        for (t in targets) {
            val o = pos(t) + Offset(s / 2, s / 2)
            if (g.board[t] != 0 || (g.board[sel] != 0 && abs(g.board[sel]) == ChessGame.P && (t and 7) != (sel and 7)))
                drawCircle(c.gold, s * .46f, o, style = Stroke(width = s * .07f))
            else drawCircle(c.gold.copy(alpha = .85f), s * .13f, o)
        }
    }
    promo?.let { list ->
        AlertDialog(
            onDismissRequest = { promo = null },
            containerColor = pm.surface,
            confirmButton = {},
            text = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (pr in intArrayOf(ChessGame.Q, ChessGame.R, ChessGame.B, ChessGame.N)) {
                        val mv = list.firstOrNull { ChessGame.promoOf(it) == pr } ?: continue
                        PieceChoice(pr, vm.mySide == 0) { promo = null; vm.playMove(mv) }
                    }
                }
            },
        )
    }
}

// ───────────────────────── Cờ tướng ─────────────────────────
/** Quân cờ tướng: hồng ngọc (đỏ) hoặc hắc ngọc (đen), chữ khắc màu kem có bóng. */
private fun DrawScope.drawXiangqiPiece(c: Offset, rad: Float, red: Boolean, ch: String, tm: TextMeasurer) {
    if (red) drawGlassDisc(c, rad, GlassPalette.rubyHi, GlassPalette.ruby, GlassPalette.rubyLo)
    else drawGlassDisc(c, rad, GlassPalette.obsHi, GlassPalette.obs, GlassPalette.obsLo)
    val r = tm.measure(ch, TextStyle(color = GlassPalette.cream, fontSize = (rad * 1.16f).toSp(), fontWeight = FontWeight.Bold,
        shadow = Shadow(Color(0x99000000), Offset(rad * .03f, rad * .06f), rad * .08f)))
    drawText(r, topLeft = c - Offset(r.size.width / 2f, r.size.height / 2f))
}

private val RED_CHARS = arrayOf("", "帥", "仕", "相", "傌", "俥", "炮", "兵")
private val BLK_CHARS = arrayOf("", "將", "士", "象", "馬", "車", "砲", "卒")

@Composable
fun XiangqiBoardView(vm: SessionViewModel, modifier: Modifier = Modifier) {
    val g = vm.game as? XiangqiGame ?: return
    val tick = vm.tick
    val flip = vm.mySide == 1
    var sel by remember { mutableIntStateOf(-1) }
    LaunchedEffect(tick) { sel = -1 }
    val legal = remember(tick) { g.legalMoves() }
    val targets = if (sel >= 0) legal.filter { XiangqiGame.fromSq(it) == sel }.map { XiangqiGame.toSq(it) } else emptyList()
    val c = pm
    val tm = rememberTextMeasurer()

    Canvas(
        modifier.boardFrame().aspectRatio(0.9f).clip(RoundedCornerShape(12.dp))
            .pointerInput(tick, flip, sel) {
                detectTapGestures { off ->
                    if (!vm.myTurn) return@detectTapGestures
                    val s = size.width / 9f
                    val col = (off.x / s).toInt().coerceIn(0, 8); val row = (off.y / s).toInt().coerceIn(0, 9)
                    val f = if (flip) 8 - col else col
                    val r = if (flip) row else 9 - row
                    val sq = r * 9 + f
                    if (sel >= 0 && sq in targets) { vm.playMove(XiangqiGame.mk(sel, sq)); sel = -1 }
                    else {
                        val p = g.board[sq]
                        sel = if (p != 0 && (p > 0) == (vm.mySide == 0) && legal.any { XiangqiGame.fromSq(it) == sq }) sq else -1
                    }
                }
            },
    ) {
        @Suppress("UNUSED_VARIABLE") val dep = tick
        val s = size.width / 9f
        fun ctr(sq: Int): Offset {
            val f = sq % 9; val r = sq / 9
            val x = (if (flip) 8 - f else f) + .5f; val y = (if (flip) r else 9 - r) + .5f
            return Offset(x * s, y * s)
        }
        drawRect(Brush.linearGradient(if (c.dark) listOf(Color(0xFF3A4F8C), Color(0xFF202E5E)) else listOf(Color(0xFFF3F9FF), Color(0xFFCBE1F5)), Offset.Zero, Offset(size.width, size.height)))
        drawGlassSheen(c.dark)
        val line = if (c.dark) Color(0xFFC4D2F7) else Color(0xFF4A7CAD)
        val lw = s * .035f
        for (r in 0..9) drawLine(line, ctr(r * 9), ctr(r * 9 + 8), lw)
        drawLine(line, ctr(0), ctr(81), lw); drawLine(line, ctr(8), ctr(89), lw)
        for (f in 1..7) { drawLine(line, ctr(f), ctr(36 + f), lw); drawLine(line, ctr(45 + f), ctr(81 + f), lw) }
        // cung
        for (base in intArrayOf(0, 63)) {
            drawLine(line, ctr(base + 3), ctr(base + 23), lw); drawLine(line, ctr(base + 5), ctr(base + 21), lw)
        }
        // sông
        val riverStyle = TextStyle(color = line.copy(alpha = .7f), fontSize = (s * .5f).toSp(), fontWeight = FontWeight.Bold)
        val left = tm.measure("楚 河", riverStyle); val right = tm.measure("漢 界", riverStyle)
        val ry = size.height / 2 - left.size.height / 2
        drawText(left, topLeft = Offset(s * 1.2f, ry)); drawText(right, topLeft = Offset(size.width - s * 1.2f - right.size.width, ry))
        // nước đi gần nhất
        val last = g.lastMove
        if (last >= 0) for (q in listOf(XiangqiGame.fromSq(last), XiangqiGame.toSq(last)))
            drawCircle(c.gold.copy(alpha = .75f), s * .47f, ctr(q), style = Stroke(width = s * .06f))
        if (g.result == Outcome.ONGOING && g.inCheck()) {
            val k = ctr(g.kingSquare(g.side))
            drawCircle(Brush.radialGradient(listOf(c.danger.copy(alpha = .9f), Color.Transparent), k, s * .7f), s * .7f, k)
        }
        // quân
        for (sq in 0 until 90) {
            val p = g.board[sq]; if (p == 0) continue
            drawXiangqiPiece(ctr(sq), s * .43f, p > 0, (if (p > 0) RED_CHARS else BLK_CHARS)[abs(p)], tm)
        }
        if (sel >= 0) drawCircle(c.gold, s * .5f, ctr(sel), style = Stroke(width = s * .07f))
        drawRect(if (c.dark) Color(0xFF9AA8E0).copy(alpha = .5f) else Color(0xFF8FB0D8), Offset.Zero, size, style = Stroke(s * .08f))
        for (t in targets) {
            val o = ctr(t)
            if (g.board[t] != 0) drawCircle(c.gold, s * .5f, o, style = Stroke(width = s * .07f)) else drawCircle(c.gold.copy(alpha = .9f), s * .13f, o)
        }
    }
}

// ───────────────────────── Cờ caro ─────────────────────────
@Composable
fun GomokuBoardView(vm: SessionViewModel, modifier: Modifier = Modifier) {
    val g = vm.game as? GomokuGame ?: return
    val tick = vm.tick
    val n = g.size
    val c = pm

    Canvas(
        modifier.boardFrame().aspectRatio(1f).clip(RoundedCornerShape(12.dp))
            .pointerInput(tick) {
                detectTapGestures { off ->
                    if (!vm.myTurn) return@detectTapGestures
                    val s = size.width / n.toFloat()
                    val x = (off.x / s).toInt().coerceIn(0, n - 1); val y = (off.y / s).toInt().coerceIn(0, n - 1)
                    vm.playMove(y * n + x)
                }
            },
    ) {
        @Suppress("UNUSED_VARIABLE") val dep = tick
        val s = size.width / n
        fun ctr(i: Int) = Offset((i % n + .5f) * s, (i / n + .5f) * s)
        // nền khung: kính xanh băng nhạt, các ô trống là những viên gạch kính trắng
        drawRect(if (c.dark) Color(0xFF151B45) else Color(0xFFC9D8EA))
        val gap = s * .07f
        val corner = CornerRadius(s * .16f)
        val cellTop = if (c.dark) Color(0xFF3B4890) else Color.White
        val cellBottom = if (c.dark) Color(0xFF2A3470) else Color(0xFFE4ECF7)
        val cellEdge = if (c.dark) Color(0xFF6F7CC4).copy(alpha = .6f) else Color(0xFFB9CBE0)
        for (i in 0 until n * n) {
            val y0 = (i / n) * s
            val tl = Offset((i % n) * s + gap / 2, y0 + gap / 2)
            val sz = Size(s - gap, s - gap)
            drawRoundRect(Brush.verticalGradient(listOf(cellTop, cellBottom), y0, y0 + s), tl, sz, corner)
            drawRoundRect(Color.White.copy(alpha = if (c.dark) .14f else .9f), tl + Offset(s * .05f, s * .04f), Size(sz.width - s * .1f, sz.height * .3f), CornerRadius(s * .12f))
            drawRoundRect(cellEdge, tl, sz, corner, style = Stroke(s * .02f))
        }
        val last = g.lastMove
        if (last >= 0) {
            val tl = Offset((last % n) * s + gap / 2, (last / n) * s + gap / 2)
            drawRoundRect(c.gold.copy(alpha = .22f), tl, Size(s - gap, s - gap), corner)
            drawRoundRect(c.gold, tl, Size(s - gap, s - gap), corner, style = Stroke(s * .06f))
        }
        // X (đỏ, đi trước) và O (đen, đi sau)
        for (i in 0 until n * n) {
            val v = g.cells[i]; if (v == 0) continue
            if (v == 1) drawGlassX(ctr(i), s * .3f) else drawGlassO(ctr(i), s * .3f)
        }
        if (g.winLine.isNotEmpty()) {
            val a = ctr(g.winLine.first()); val b = ctr(g.winLine.last())
            drawLine(c.gold.copy(alpha = .35f), a, b, s * .5f, cap = StrokeCap.Round)
            drawLine(c.gold, a, b, s * .14f, cap = StrokeCap.Round)
        }
    }
}

/** Vẽ nhỏ quân đại diện cho một bên (dùng ở thanh người chơi và ô game). */
@Composable
fun SideBadge(gameId: String, side: Int, modifier: Modifier = Modifier) {
    val rm = rememberTextMeasurer()
    Canvas(modifier.size(30.dp)) {
        val s = size.width
        when (gameId) {
            "chess" -> drawChessPiece(ChessGame.K, side == 0, Offset.Zero, s)
            "xiangqi" -> drawXiangqiPiece(Offset(s / 2, s / 2), s * .44f, side == 0, if (side == 0) "帥" else "將", rm)
            else -> if (side == 0) drawGlassX(Offset(s / 2, s / 2), s * .34f) else drawGlassO(Offset(s / 2, s / 2), s * .38f)
        }
    }
}
