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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fws.playmind.R
import com.fws.playmind.core.BlindCaroGame
import com.fws.playmind.core.GomokuGame
import com.fws.playmind.core.Outcome
import com.fws.playmind.match.SessionViewModel
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** 4 màu quân của cờ caro mù: xanh, đỏ, tím, vàng. */
val BlindColors = listOf(Color(0xFF2F7BEA), Color(0xFFE0453A), Color(0xFF8B4FD8), Color(0xFFF2B51C))
private val BlindColorNames = intArrayOf(R.string.bc_blue, R.string.bc_red, R.string.bc_purple, R.string.bc_yellow)

/** Viên quân tròn bóng một màu (không có dấu X / O). */
fun DrawScope.drawColorStone(c: Offset, r: Float, color: Color) {
    drawCircle(Color.Black.copy(alpha = .18f), r, c + Offset(r * .08f, r * .16f))
    drawCircle(Brush.radialGradient(listOf(lerp(color, Color.White, .45f), color, lerp(color, Color.Black, .35f)), c - Offset(r * .35f, r * .4f), r * 1.6f), r, c)
    drawCircle(Color.White.copy(alpha = .45f), r * .28f, c - Offset(r * .34f, r * .38f))
    drawCircle(lerp(color, Color.Black, .45f).copy(alpha = .6f), r, c, style = Stroke(r * .07f))
}

/** Tranh minh hoạ cờ caro mù: bốn viên quân bốn màu xếp chéo. */
fun DrawScope.drawBlindCaroArt(ox: Float, oy: Float, s: Float) {
    val r = s * .15f
    for (k in 0 until 4) drawColorStone(Offset(ox + s * (.2f + .2f * k), oy + s * (.75f - .17f * k)), r, BlindColors[k])
}

/** Hết ván: lộ dấu X / O trên từng viên. */
private fun DrawScope.drawReveal(c: Offset, r: Float, isX: Boolean) {
    val w = r * .2f
    if (isX) {
        val k = r * .42f
        drawLine(Color.White, Offset(c.x - k, c.y - k), Offset(c.x + k, c.y + k), w, StrokeCap.Round)
        drawLine(Color.White, Offset(c.x + k, c.y - k), Offset(c.x - k, c.y + k), w, StrokeCap.Round)
    } else drawCircle(Color.White, r * .42f, c, style = Stroke(w))
}

/**
 * Bàn cờ caro mù: như bàn caro vô hạn (kéo, thu phóng, chạm để đặt quân) nhưng quân chỉ có màu, không cho biết của ai.
 * Bảng chọn màu ở góc dưới: người đi trước chọn màu bất kỳ, người đi sau chỉ được dùng màu quân vừa đặt. Hết ván mới lộ X / O.
 */
@Composable
fun BlindCaroBoardView(vm: SessionViewModel, modifier: Modifier = Modifier) {
    val g = vm.game as? BlindCaroGame ?: return
    val tick = vm.tick
    val density = LocalDensity.current
    val baseCell = with(density) { 42.dp.toPx() }
    var zoom by remember(g) { mutableFloatStateOf(1f) }
    var cx by remember(g) { mutableFloatStateOf(.5f) }
    var cy by remember(g) { mutableFloatStateOf(.5f) }
    var viewW by remember { mutableFloatStateOf(0f) }
    var viewH by remember { mutableFloatStateOf(0f) }
    var picked by remember(g) { mutableIntStateOf(0) }
    val forced = g.forcedColor
    val color = if (forced >= 0) forced else picked
    val over = g.result != Outcome.ONGOING
    val shape = RoundedCornerShape(20.dp)

    LaunchedEffect(tick) {
        val last = g.lastMove
        if (last < 0 || viewW <= 0f) return@LaunchedEffect
        val cell = baseCell * zoom
        val mx = BlindCaroGame.px(last) + .5f; val my = BlindCaroGame.py(last) + .5f
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
                        val newZoom = (zoom * zoomChange).coerceIn(.45f, 2.8f)
                        val newCell = baseCell * newZoom
                        val wx = cx + (centroid.x - size.width / 2f) / oldCell
                        val wy = cy + (centroid.y - size.height / 2f) / oldCell
                        zoom = newZoom
                        cx = wx - (centroid.x - size.width / 2f) / newCell - pan.x / newCell
                        cy = wy - (centroid.y - size.height / 2f) / newCell - pan.y / newCell
                    }
                }
                .pointerInput(tick, color) {
                    detectTapGestures { off ->
                        if (!vm.myTurn) return@detectTapGestures
                        val cell = baseCell * zoom
                        val x = floor(cx + (off.x - size.width / 2f) / cell).toInt()
                        val y = floor(cy + (off.y - size.height / 2f) / cell).toInt()
                        if (BlindCaroGame.inRange(x, y) && g.at(x, y) == 0) vm.playMove(BlindCaroGame.pack(x, y, color))
                    }
                },
        ) {
            @Suppress("UNUSED_VARIABLE") val dep = tick
            val cell = baseCell * zoom
            val offX = size.width / 2f - cx * cell; val offY = size.height / 2f - cy * cell
            drawCaroBoard(cell, offX, offY)
            val last = g.lastMove
            if (last >= 0) drawCaroLast(Offset(offX + (BlindCaroGame.px(last) + .5f) * cell, offY + (BlindCaroGame.py(last) + .5f) * cell), cell)
            val hist = g.history
            for (i in hist.indices) {
                val m = hist[i]
                val sx = offX + (BlindCaroGame.px(m) + .5f) * cell; val sy = offY + (BlindCaroGame.py(m) + .5f) * cell
                if (sx < -cell || sy < -cell || sx > size.width + cell || sy > size.height + cell) continue
                drawColorStone(Offset(sx, sy), cell * .38f, BlindColors[BlindCaroGame.color(m)])
                if (over || vm.bcRevealing) drawReveal(Offset(sx, sy), cell * .38f, i % 2 == 0)
            }
            val wl = g.inner.winLine
            if (wl.isNotEmpty()) {
                val a = wl.first(); val b = wl.last()
                drawWinLine(
                    Offset(offX + (GomokuGame.px(a) + .5f) * cell, offY + (GomokuGame.py(a) + .5f) * cell),
                    Offset(offX + (GomokuGame.px(b) + .5f) * cell, offY + (GomokuGame.py(b) + .5f) * cell), cell,
                )
            }
        }
        if (vm.bcRevealing && !over) {
            Text(
                stringResource(R.string.bc_revealing),
                Modifier.align(Alignment.TopCenter).padding(top = 12.dp, start = 12.dp, end = 12.dp)
                    .clip(RoundedCornerShape(50)).background(pm.goldSoft).border(1.dp, pm.goldBright, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium, color = pm.gold,
            )
        } else if (g.plies < 4) {
            Text(
                stringResource(R.string.bc_hint),
                Modifier.align(Alignment.TopCenter).padding(top = 12.dp, start = 12.dp, end = 12.dp)
                    .clip(RoundedCornerShape(50)).background(pm.surface.copy(alpha = .9f)).border(1.dp, pm.outline, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium, color = pm.inkSoft,
            )
        }
        // bảng chọn màu
        if (!over) Column(
            Modifier.align(Alignment.BottomStart).padding(12.dp).clip(RoundedCornerShape(18.dp))
                .background(pm.surface.copy(alpha = .92f)).border(1.dp, pm.outline, RoundedCornerShape(18.dp)).padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                stringResource(if (forced >= 0) R.string.bc_forced else R.string.bc_pick),
                style = MaterialTheme.typography.labelSmall, color = pm.inkSoft,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (k in 0 until BlindCaroGame.COLORS) {
                    val enabled = forced < 0 || forced == k
                    val sel = k == color
                    val desc = stringResource(BlindColorNames[k])
                    Box(
                        Modifier.size(38.dp).alpha(if (enabled) 1f else .25f).clip(CircleShape)
                            .border(if (sel) 3.dp else 1.dp, if (sel) pm.gold else pm.outline, CircleShape)
                            .clickable(enabled = enabled && forced < 0) { picked = k }
                            .semantics { contentDescription = desc },
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.size(28.dp)) { drawColorStone(Offset(size.width / 2f, size.height / 2f), size.width * .46f, BlindColors[k]) }
                    }
                }
            }
        }
        Column(Modifier.align(Alignment.BottomEnd).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // gói tháng 1 lần, gói năm 3 lần mỗi ván: hiện X/O thật trong 10 giây
            val left = vm.bcRevealsLeft()
            if (!over && vm.bcRevealAllowance() > 0) Box {
                FloatingRoundButton(Ic.Bulb, stringResource(R.string.bc_reveal), { vm.bcReveal() })
                Text(
                    left.toString(),
                    Modifier.align(Alignment.TopEnd).size(18.dp).clip(CircleShape).background(if (left > 0) pm.goldBright else pm.outline).padding(top = 1.dp),
                    style = MaterialTheme.typography.labelSmall, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            FloatingRoundButton(Ic.Plus, stringResource(R.string.caro_zoom_in), { zoom = min(2.8f, zoom * 1.25f) })
            FloatingRoundButton(Ic.Minus, stringResource(R.string.caro_zoom_out), { zoom = max(.45f, zoom / 1.25f) })
            FloatingRoundButton(Ic.Target, stringResource(R.string.caro_recenter), {
                val last = g.lastMove
                cx = if (last >= 0) BlindCaroGame.px(last) + .5f else .5f
                cy = if (last >= 0) BlindCaroGame.py(last) + .5f else .5f
            })
        }
    }
}
