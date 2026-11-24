package com.fws.playmind.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.fws.playmind.R
import com.fws.playmind.core.ChessGame
import com.fws.playmind.core.GomokuGame
import com.fws.playmind.core.Outcome
import com.fws.playmind.core.XiangqiGame
import com.fws.playmind.match.SessionViewModel
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
private fun PieceChoice(type: Int, white: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Canvas(Modifier.size(62.dp).clip(shape).background(pm.surfaceHigh).border(1.dp, pm.outline, shape).clickable(onClick = onClick)) {
        drawChessPiece(type, white, Offset.Zero, size.width)
    }
}

/** Bóng đổ mềm dưới bàn cờ để bàn "nổi" lên khỏi nền. */
private fun Modifier.boardShadow(radius: Int = 16) = this.shadow(
    14.dp, RoundedCornerShape(radius.dp), ambientColor = Color(0x33000000), spotColor = Color(0x59000000),
)

private fun Modifier.boardFrame() = this.fillMaxWidth().padding(4.dp)

private fun DrawScope.ring(c: Offset, r: Float, color: Color, w: Float) = drawCircle(color, r, c, style = Stroke(w))

/** Chấm gợi ý nước đi: viên vàng có viền tối nhẹ để thấy rõ trên cả ô trắng lẫn ô đen. */
private fun DrawScope.drawTargetDot(c: Offset, r: Float) {
    drawCircle(Color.Black.copy(alpha = .35f), r * 1.18f, c)
    drawCircle(Brush.radialGradient(0f to Lux.goldHi, 1f to Lux.gold, center = c - Offset(r * .3f, r * .3f), radius = r * 1.6f), r, c)
}

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
    val tm = rememberTextMeasurer()

    Canvas(
        modifier.boardFrame().boardShadow(18).aspectRatio(1f).clip(RoundedCornerShape(18.dp))
            .pointerInput(tick, flip, sel) {
                detectTapGestures { off ->
                    if (!vm.myTurn) return@detectTapGestures
                    val side = size.width.toFloat()
                    val fr = chessFrame(side); val s = (side - 2 * fr) / 8f
                    val col = floor((off.x - fr) / s).toInt(); val row = floor((off.y - fr) / s).toInt()
                    if (col !in 0..7 || row !in 0..7) return@detectTapGestures
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
        val side = size.width
        val fr = chessFrame(side); val s = (side - 2 * fr) / 8f
        fun pos(sq: Int): Offset { val f = sq and 7; val r = sq shr 3; return if (flip) Offset(fr + (7 - f) * s, fr + r * s) else Offset(fr + f * s, fr + (7 - r) * s) }
        drawChessBoard(tm, flip)
        val last = g.lastMove
        if (last >= 0) for (q in listOf(ChessGame.fromSq(last), ChessGame.toSq(last))) drawRect(Lux.gold.copy(alpha = .50f), pos(q), Size(s, s))
        if (g.result == Outcome.ONGOING && g.inCheck()) {
            val k = pos(g.kingSquare(g.side)) + Offset(s / 2, s / 2)
            drawCircle(Brush.radialGradient(0f to Color(0xFFE5392B).copy(alpha = .85f), 1f to Color.Transparent, center = k, radius = s * .66f), s * .66f, k)
        }
        if (sel >= 0) {
            drawRect(Lux.goldBright.copy(alpha = .62f), pos(sel), Size(s, s))
            drawRect(Lux.goldHi, pos(sel), Size(s, s), style = Stroke(s * .05f))
        }
        for (sq in 0 until 64) {
            val p = g.board[sq]; if (p == 0) continue
            drawChessPiece(abs(p), p > 0, pos(sq), s)
        }
        for (t in targets) {
            val o = pos(t) + Offset(s / 2, s / 2)
            if (g.board[t] != 0 || (g.board[sel] != 0 && abs(g.board[sel]) == ChessGame.P && (t and 7) != (sel and 7)))
                ring(o, s * .45f, Lux.goldBright, s * .08f)
            else drawTargetDot(o, s * .12f)
        }
    }
    promo?.let { list ->
        AlertDialog(
            onDismissRequest = { promo = null },
            shape = RoundedCornerShape(28.dp), tonalElevation = 0.dp,
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
@Composable
fun XiangqiBoardView(vm: SessionViewModel, modifier: Modifier = Modifier) {
    val g = vm.game as? XiangqiGame ?: return
    val tick = vm.tick
    val flip = vm.mySide == 1
    var sel by remember { mutableIntStateOf(-1) }
    LaunchedEffect(tick) { sel = -1 }
    val legal = remember(tick) { g.legalMoves() }
    val targets = if (sel >= 0) legal.filter { XiangqiGame.fromSq(it) == sel }.map { XiangqiGame.toSq(it) } else emptyList()
    val tm = rememberTextMeasurer()

    Canvas(
        modifier.boardFrame().boardShadow(14).aspectRatio(XQ_ASPECT).clip(RoundedCornerShape(14.dp))
            .pointerInput(tick, flip, sel) {
                detectTapGestures { off ->
                    if (!vm.myTurn) return@detectTapGestures
                    val s = size.width / (8f + 2 * XQ_MARGIN); val m = XQ_MARGIN * s
                    val col = ((off.x - m) / s).roundToInt().coerceIn(0, 8); val row = ((off.y - m) / s).roundToInt().coerceIn(0, 9)
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
        val s = size.width / (8f + 2 * XQ_MARGIN); val m = XQ_MARGIN * s
        fun ctr(sq: Int): Offset {
            val f = sq % 9; val r = sq / 9
            val x = if (flip) 8 - f else f; val y = if (flip) r else 9 - r
            return Offset(m + x * s, m + y * s)
        }
        drawXiangqiBoard(tm, flip)
        val last = g.lastMove
        if (last >= 0) for (q in listOf(XiangqiGame.fromSq(last), XiangqiGame.toSq(last))) {
            val c = ctr(q)
            drawCircle(Brush.radialGradient(0f to Color.Transparent, .78f to Color.Transparent, 1f to Color(0xFFFFF3B0).copy(alpha = .95f), center = c, radius = s * .56f), s * .56f, c)
            ring(c, s * .5f, Lux.goldLo.copy(alpha = .85f), s * .045f)
        }
        if (g.result == Outcome.ONGOING && g.inCheck()) {
            val k = ctr(g.kingSquare(g.side))
            drawCircle(Brush.radialGradient(0f to Color(0xFFE5392B).copy(alpha = .9f), 1f to Color.Transparent, center = k, radius = s * .78f), s * .78f, k)
        }
        for (sq in 0 until 90) {
            val p = g.board[sq]; if (p == 0) continue
            drawXiangqiPiece(ctr(sq), s * .44f, p > 0, abs(p), tm)
        }
        if (sel >= 0) { ring(ctr(sel), s * .52f, Lux.goldLo, s * .085f); ring(ctr(sel), s * .52f, Lux.goldHi, s * .045f) }
        for (t in targets) {
            val o = ctr(t)
            if (g.board[t] != 0) { ring(o, s * .52f, Lux.goldLo, s * .085f); ring(o, s * .52f, Color(0xFFE5392B), s * .045f) } else drawTargetDot(o, s * .13f)
        }
    }
}

// ───────────────────────── Cờ caro: bàn gỗ vô hạn ─────────────────────────
private const val MIN_ZOOM = 0.45f
private const val MAX_ZOOM = 2.8f

/**
 * Bàn caro không giới hạn số ô: kéo một ngón để di chuyển, chụm hai ngón để thu phóng, chạm để đặt quân.
 * (cx, cy) là toạ độ thế giới (đơn vị = ô) đang nằm ở giữa màn hình.
 */
@Composable
fun GomokuBoardView(vm: SessionViewModel, modifier: Modifier = Modifier) {
    val g = vm.game as? GomokuGame ?: return
    val tick = vm.tick
    val density = LocalDensity.current
    val baseCell = with(density) { 42.dp.toPx() }
    var zoom by remember(g) { mutableFloatStateOf(1f) }
    var cx by remember(g) { mutableFloatStateOf(.5f) }
    var cy by remember(g) { mutableFloatStateOf(.5f) }
    var viewW by remember { mutableFloatStateOf(0f) }
    var viewH by remember { mutableFloatStateOf(0f) }
    val shape = RoundedCornerShape(20.dp)

    // Nước vừa đi (của bot hoặc đối thủ) nằm ngoài tầm nhìn thì trượt bàn cờ để thấy nó.
    LaunchedEffect(tick) {
        val last = g.lastMove
        if (last < 0 || viewW <= 0f) return@LaunchedEffect
        val cell = baseCell * zoom
        val mx = GomokuGame.px(last) + .5f; val my = GomokuGame.py(last) + .5f
        val halfW = viewW / 2f / cell - 1.6f; val halfH = viewH / 2f / cell - 1.6f
        val dx = mx - cx; val dy = my - cy
        if (abs(dx) <= halfW && abs(dy) <= halfH) return@LaunchedEffect
        val sx = cx; val sy = cy
        val tx = if (abs(dx) > halfW) mx else cx; val ty = if (abs(dy) > halfH) my else cy
        animate(0f, 1f, animationSpec = tween(380)) { v, _ -> cx = sx + (tx - sx) * v; cy = sy + (ty - sy) * v }
    }

    Box(
        modifier.boardShadow(20).clip(shape).border(BorderStroke(1.2.dp, pm.goldBright.copy(alpha = .7f)), shape)
            .onSizeChanged { viewW = it.width.toFloat(); viewH = it.height.toFloat() },
    ) {
        Canvas(
            Modifier.fillMaxSize()
                .pointerInput(g) {
                    detectTransformGestures { centroid, pan, zoomChange, _ ->
                        val oldCell = baseCell * zoom
                        val newZoom = (zoom * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
                        val newCell = baseCell * newZoom
                        // giữ nguyên điểm thế giới đang nằm dưới ngón tay khi thu phóng
                        val wx = cx + (centroid.x - size.width / 2f) / oldCell
                        val wy = cy + (centroid.y - size.height / 2f) / oldCell
                        zoom = newZoom
                        cx = wx - (centroid.x - size.width / 2f) / newCell - pan.x / newCell
                        cy = wy - (centroid.y - size.height / 2f) / newCell - pan.y / newCell
                    }
                }
                .pointerInput(tick) {
                    detectTapGestures { off ->
                        if (!vm.myTurn) return@detectTapGestures
                        val cell = baseCell * zoom
                        val x = floor(cx + (off.x - size.width / 2f) / cell).toInt()
                        val y = floor(cy + (off.y - size.height / 2f) / cell).toInt()
                        if (GomokuGame.inRange(x, y) && g.at(x, y) == 0) vm.playMove(GomokuGame.pack(x, y))
                    }
                },
        ) {
            @Suppress("UNUSED_VARIABLE") val dep = tick
            val cell = baseCell * zoom
            val offX = size.width / 2f - cx * cell; val offY = size.height / 2f - cy * cell
            drawCaroBoard(cell, offX, offY)
            val last = g.lastMove
            if (last >= 0) drawCaroLast(Offset(offX + (GomokuGame.px(last) + .5f) * cell, offY + (GomokuGame.py(last) + .5f) * cell), cell)
            val hist = g.history
            for (i in hist.indices) {
                val m = hist[i]
                val sx = offX + (GomokuGame.px(m) + .5f) * cell; val sy = offY + (GomokuGame.py(m) + .5f) * cell
                if (sx < -cell || sy < -cell || sx > size.width + cell || sy > size.height + cell) continue
                drawCaroPiece(Offset(sx, sy), cell, i % 2 == 0)
            }
            if (g.winLine.isNotEmpty()) {
                val a = g.winLine.first(); val b = g.winLine.last()
                drawWinLine(
                    Offset(offX + (GomokuGame.px(a) + .5f) * cell, offY + (GomokuGame.py(a) + .5f) * cell),
                    Offset(offX + (GomokuGame.px(b) + .5f) * cell, offY + (GomokuGame.py(b) + .5f) * cell), cell,
                )
            }
        }
        // gợi ý thao tác ở những nước đầu
        if (g.plies < 4) {
            Text(
                stringResource(R.string.caro_hint),
                Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
                    .clip(RoundedCornerShape(50)).background(pm.surface.copy(alpha = .88f)).border(1.dp, pm.outline, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium, color = pm.inkSoft,
            )
        }
        Column(Modifier.align(Alignment.BottomEnd).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FloatingRoundButton(Ic.Plus, stringResource(R.string.caro_zoom_in), { zoom = min(MAX_ZOOM, zoom * 1.25f) })
            FloatingRoundButton(Ic.Minus, stringResource(R.string.caro_zoom_out), { zoom = max(MIN_ZOOM, zoom / 1.25f) })
            FloatingRoundButton(Ic.Target, stringResource(R.string.caro_recenter), {
                val last = g.lastMove
                cx = if (last >= 0) GomokuGame.px(last) + .5f else .5f
                cy = if (last >= 0) GomokuGame.py(last) + .5f else .5f
            })
        }
    }
}
