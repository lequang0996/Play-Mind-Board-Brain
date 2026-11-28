package com.fws.playmind.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fws.playmind.R
import com.fws.playmind.core.MorrisGame
import com.fws.playmind.match.SessionViewModel
import kotlin.math.hypot

private val MoBoardA = Color(0xFFE9DCC0)
private val MoBoardB = Color(0xFFD6C29A)
private val MoLine = Color(0xFF4A3A26)

/** Tâm điểm [i] trên bàn cạnh [w] (lề [pad], lưới 7×7). */
private fun moPos(i: Int, w: Float): Offset {
    val pad = w * .08f; val cs = (w - 2 * pad) / 6f
    return Offset(pad + MorrisGame.XY[i][0] * cs, pad + MorrisGame.XY[i][1] * cs)
}

/** Vẽ bàn cờ cối xay: 3 hình vuông đồng tâm, 4 đường nối trung điểm, 24 điểm. */
fun DrawScope.drawMorrisBoard(w: Float) {
    drawRoundRect(Brush.linearGradient(listOf(MoBoardA, MoBoardB)), Offset.Zero, Size(w, w), CornerRadius(w * .05f))
    val sw = w * .012f
    for (r in 0..2) {
        val a = moPos(r * 8, w); val b = moPos(r * 8 + 4, w)
        drawRect(MoLine, a, Size(b.x - a.x, b.y - a.y), style = Stroke(sw))
    }
    for (k in intArrayOf(1, 3, 5, 7)) drawLine(MoLine, moPos(k, w), moPos(16 + k, w), sw)
    for (i in 0 until MorrisGame.N) drawCircle(MoLine, w * .018f, moPos(i, w))
}

@Composable
fun MorrisBoardView(vm: SessionViewModel, modifier: Modifier = Modifier) {
    val g = vm.game as? MorrisGame ?: return
    val tick = vm.tick
    val act = vm.canAct
    var selFrom by remember { mutableIntStateOf(-1) }
    var pendTo by remember { mutableIntStateOf(-1) }
    LaunchedEffect(tick) { selFrom = -1; pendTo = -1 }
    val legal = remember(tick) { g.legalMoves() }
    val me = g.turn
    val placing = g.hand[me] > 0
    // nước khớp với lựa chọn hiện tại
    fun movesTo(to: Int) = legal.filter { MorrisGame.toOf(it) == to && MorrisGame.fromOf(it) == (if (placing) -1 else selFrom) }
    val targets: Set<Int> = if (!act) emptySet() else if (pendTo >= 0) movesTo(pendTo).map { MorrisGame.capOf(it) }.toSet()
        else if (placing) legal.map { MorrisGame.toOf(it) }.toSet()
        else if (selFrom >= 0) legal.filter { MorrisGame.fromOf(it) == selFrom }.map { MorrisGame.toOf(it) }.toSet() else emptySet()
    val movable: Set<Int> = if (act && !placing && pendTo < 0) legal.map { MorrisGame.fromOf(it) }.toSet() else emptySet()
    val last = g.lastMove

    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val side = if (maxWidth < maxHeight - 64.dp) maxWidth else maxHeight - 64.dp
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Canvas(
                Modifier.size(side).clip(RoundedCornerShape(18.dp)).pointerInput(tick, selFrom, pendTo, act) {
                    detectTapGestures { off ->
                        if (!act) return@detectTapGestures
                        val w = size.width.toFloat()
                        val p = (0 until MorrisGame.N).minByOrNull { hypot(moPos(it, w).x - off.x, moPos(it, w).y - off.y) } ?: return@detectTapGestures
                        if (hypot(moPos(p, w).x - off.x, moPos(p, w).y - off.y) > w * .08f) { selFrom = -1; pendTo = -1; return@detectTapGestures }
                        if (pendTo >= 0) {
                            movesTo(pendTo).firstOrNull { MorrisGame.capOf(it) == p }?.let { vm.playMove(it) }
                            return@detectTapGestures
                        }
                        if (!placing && g.board[p] == me) { selFrom = p; return@detectTapGestures }
                        if (!placing && selFrom < 0) return@detectTapGestures
                        val ms = movesTo(p)
                        if (ms.isEmpty()) return@detectTapGestures
                        if (ms.all { MorrisGame.capOf(it) == MorrisGame.NO_CAP }) vm.playMove(ms[0]) else pendTo = p
                    }
                },
            ) {
                @Suppress("UNUSED_VARIABLE") val dep = tick
                val w = size.width
                drawMorrisBoard(w)
                val r = w * .045f
                // nước vừa đi
                if (last >= 0) {
                    val f = MorrisGame.fromOf(last); val t = MorrisGame.toOf(last)
                    if (f >= 0) drawCircle(Color(0x66FFB300), r * .55f, moPos(f, w))
                    drawCircle(Color(0xFFFFB300), r * 1.3f, moPos(t, w), style = Stroke(w * .008f))
                    val c = MorrisGame.capOf(last)
                    if (c != MorrisGame.NO_CAP) { val o = moPos(c, w); drawLine(Color(0xAAD32F2F), o - Offset(r * .6f, r * .6f), o + Offset(r * .6f, r * .6f), w * .01f); drawLine(Color(0xAAD32F2F), o + Offset(-r * .6f, r * .6f), o + Offset(r * .6f, -r * .6f), w * .01f) }
                }
                for (i in 0 until MorrisGame.N) {
                    var owner = g.board[i]
                    if (pendTo >= 0) { if (i == selFrom) owner = -1; if (i == pendTo) owner = me }
                    if (owner >= 0) drawStone(moPos(i, w), r, owner == 0)
                    if (i == selFrom && pendTo < 0) drawCircle(Color(0xFF2E7D32), r * 1.25f, moPos(i, w), style = Stroke(w * .01f))
                    else if (i in movable && selFrom < 0) drawCircle(Color(0x882E7D32), r * 1.15f, moPos(i, w), style = Stroke(w * .006f))
                }
                // điểm có thể đi tới / quân có thể ăn
                for (t in targets) {
                    if (pendTo >= 0) drawCircle(Color(0xFFD32F2F), r * 1.25f, moPos(t, w), style = Stroke(w * .012f))
                    else drawCircle(Color(0x992E7D32), r * .45f, moPos(t, w))
                }
            }
            val prompt = when {
                !act -> ""
                pendTo >= 0 -> stringResource(R.string.mo_capture)
                placing -> stringResource(R.string.mo_place, g.hand[me])
                g.flying(me) -> stringResource(R.string.mo_fly)
                else -> stringResource(R.string.mo_move)
            }
            if (prompt.isNotEmpty()) Text(
                prompt, Modifier.clip(RoundedCornerShape(50)).background(pm.goldSoft).padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge, color = pm.gold, textAlign = TextAlign.Center,
            )
            if (g.stall >= 0) Text(stringResource(R.string.mo_stall, ((MorrisGame.STALL_PLIES - g.stall) + 1) / 2), style = MaterialTheme.typography.labelMedium, color = pm.danger)
        }
    }
}

/** Ảnh game: bàn 3 hình vuông với vài quân đen trắng. */
fun DrawScope.drawMorrisArt(ox: Float, oy: Float, s: Float) {
    val w = s * .86f
    drawContext.transform.translate(ox + s * .07f, oy + s * .07f)
    drawMorrisBoard(w)
    val r = w * .06f
    for ((i, b) in listOf(0 to true, 1 to true, 2 to true, 9 to false, 17 to false, 12 to false, 20 to true)) drawStone(moPos(i, w), r, b)
    drawContext.transform.translate(-(ox + s * .07f), -(oy + s * .07f))
}
