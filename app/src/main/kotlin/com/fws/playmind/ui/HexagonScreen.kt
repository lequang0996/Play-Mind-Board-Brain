package com.fws.playmind.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fws.playmind.PlayMindApp
import com.fws.playmind.R
import com.fws.playmind.catalog.GameInfo
import com.fws.playmind.core.HexPhase
import com.fws.playmind.core.Hexagon
import com.fws.playmind.match.Phase
import com.fws.playmind.match.SessionViewModel
import kotlin.math.sqrt

private val HexDark = Color(0xFF17151F)
private val HexPaper = Color(0xFFF7F3EA)
private val HexPurple = Color(0xFF5B3FA8)

/** Tâm và bán kính của 19 ô trong vùng [w]×[h] (lục giác đỉnh nhọn hướng lên, hàng 3-4-5-4-3). */
private fun hexLayout(w: Float, h: Float): Pair<Array<Offset>, Float> {
    // 5 hàng; bề ngang lớn nhất = 5 ô; bề cao = 2r + 4 × 1.5r
    val r = minOf(w / (5 * sqrt(3f) + .4f), h / 8.2f)
    val cx = w / 2; val cy = h / 2
    val pts = Array(Hexagon.CELLS) { i ->
        val q = Hexagon.AX[i][0]; val rr = Hexagon.AX[i][1]
        Offset(cx + sqrt(3f) * r * (q + rr / 2f), cy + 1.5f * r * rr)
    }
    return pts to r
}

private fun hexPath(c: Offset, r: Float): Path = Path().apply {
    for (k in 0 until 6) {
        val a = Math.toRadians(60.0 * k - 90.0)
        val x = c.x + r * kotlin.math.cos(a).toFloat(); val y = c.y + r * kotlin.math.sin(a).toFloat()
        if (k == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

@Composable
fun HexagonScreen(app: PlayMindApp, vm: SessionViewModel, info: GameInfo, onExit: () -> Unit) {
    DisposableEffect(Unit) { app.audio.calm = true; onDispose { app.audio.calm = false } }
    val t = vm.hexTick
    val m = vm.hex ?: return
    var confirmLeave by remember { mutableStateOf(false) }
    val over = vm.phase == Phase.OVER
    BackHandler { if (over) onExit() else confirmLeave = true }
    val tm = rememberTextMeasurer()
    val botName = stringResource(R.string.computer)
    val you = stringResource(R.string.hx_you)

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(info.name), { if (over) onExit() else confirmLeave = true })
        Column(Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // điểm hai bên và vòng
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScorePill(you, m.scores[0], m.caller == 0, Modifier.weight(1f))
                Text(
                    if (m.round > Hexagon.ROUNDS) stringResource(R.string.hx_extra) else stringResource(R.string.hx_round, m.round, Hexagon.ROUNDS),
                    Modifier.clip(RoundedCornerShape(50)).background(HexPurple).padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium, color = Color.White, maxLines = 1,
                )
                ScorePill(botName, m.scores[1], m.caller == 1, Modifier.weight(1f))
            }
            // thẻ trạng thái: đồng hồ vòng tròn, lời nhắc, số mục tiêu (bề ngang cố định để không chèn ép nhau)
            val (label, left, total) = when (m.phase) {
                HexPhase.MEMO -> Triple(stringResource(R.string.hx_memorize), m.phaseEnds - m.now, Hexagon.MEMO_MS)
                HexPhase.FIND -> Triple(stringResource(R.string.hx_find), m.idleEnds - m.now, Hexagon.IDLE_MS)
                HexPhase.CALL -> Triple(if (m.caller == 0) stringResource(R.string.hx_call) else stringResource(R.string.hx_bot_buzzed, botName), m.phaseEnds - m.now, Hexagon.CALL_MS)
                HexPhase.REVEAL -> Triple(stringResource(R.string.hx_reveal), m.phaseEnds - m.now, Hexagon.REVEAL_MS)
                HexPhase.OVER -> Triple("", 0L, 1L)
            }
            val frac = (left.coerceAtLeast(0).toFloat() / total).coerceIn(0f, 1f)
            val secs = (left.coerceAtLeast(0) + 999) / 1000
            val urgent = m.phase == HexPhase.CALL || (m.phase != HexPhase.REVEAL && secs <= 10)
            val cardShape = RoundedCornerShape(18.dp)
            Row(
                Modifier.fillMaxWidth().lift(cardShape, 6.dp).clip(cardShape).background(cardBrush()).border(1.dp, hairline(), cardShape).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                    val ringBg = pm.outline
                    val ringFg = if (urgent) pm.danger else pm.goldBright
                    Canvas(Modifier.fillMaxSize()) {
                        val st = size.minDimension * .1f
                        drawArc(ringBg, 0f, 360f, false, topLeft = Offset(st / 2, st / 2), size = androidx.compose.ui.geometry.Size(size.width - st, size.height - st), style = Stroke(st))
                        drawArc(ringFg, -90f, 360f * frac, false, topLeft = Offset(st / 2, st / 2), size = androidx.compose.ui.geometry.Size(size.width - st, size.height - st), style = Stroke(st, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    }
                    Text("$secs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (urgent) pm.danger else pm.ink)
                }
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = pm.ink, maxLines = 3)
                Column(Modifier.width(84.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFF0B3)), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.hx_target), Modifier.fillMaxWidth().background(HexPurple).padding(vertical = 3.dp), color = Color.White, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
                    Text(if (m.phase == HexPhase.MEMO) "?" else "${m.target}", Modifier.padding(vertical = 2.dp), fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = HexDark)
                }
            }
            // bàn lục giác: khóa chạm theo trận (không theo nhịp đồng hồ, nếu không cử chỉ bị hủy mỗi 100 ms)
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Canvas(
                    Modifier.fillMaxSize().pointerInput(m) {
                        detectTapGestures { off ->
                            if (m.phase != HexPhase.CALL || m.caller != 0) return@detectTapGestures
                            val (pts, r) = hexLayout(size.width.toFloat(), size.height.toFloat())
                            val i = pts.indices.minByOrNull { (pts[it] - off).getDistance() } ?: return@detectTapGestures
                            if ((pts[i] - off).getDistance() <= r) vm.hexPick(i)
                        }
                    },
                ) {
                    @Suppress("UNUSED_VARIABLE") val dep = t
                    val (pts, r) = hexLayout(size.width, size.height)
                    val showNums = m.phase == HexPhase.MEMO || m.phase == HexPhase.REVEAL || m.phase == HexPhase.OVER
                    val reveal = if (m.phase == HexPhase.REVEAL || m.phase == HexPhase.OVER) m.solutions.fold(0) { a, b -> a or b } else 0
                    val myCall = m.phase == HexPhase.CALL && m.caller == 0
                    for (i in 0 until Hexagon.CELLS) {
                        val picked = i in m.picks
                        val inSol = reveal and (1 shl i) != 0
                        val fill = when { picked -> Color(0xFFFFD54F); showNums -> if (inSol && m.phase != HexPhase.MEMO) Color(0xFFBFE8C4) else HexPaper; myCall -> Color(0xFF2A2540); else -> HexDark }
                        val path = hexPath(pts[i], r * .94f)
                        drawPath(path, Color.Black.copy(alpha = .18f), style = Stroke(r * .12f))
                        drawPath(path, fill)
                        drawPath(path, if (myCall && !picked) Color(0xFFFFD54F).copy(alpha = .6f) else Color.Black.copy(alpha = .25f), style = Stroke(r * .05f))
                        val txt = if (showNums) "${m.values[i]}" else Hexagon.letter(i)
                        val col = if (picked || showNums) HexDark else Color.White
                        val lay = tm.measure(txt, TextStyle(fontSize = (r * .78f / density).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = col))
                        drawText(lay, topLeft = Offset(pts[i].x - lay.size.width / 2f, pts[i].y - lay.size.height / 2f))
                    }
                }
            }
            // phản hồi lần trả lời gần nhất (giữ chỗ cố định để bàn không nhảy)
            Box(Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) {
                if (m.answers > 0 && m.lastSeat >= 0) {
                    val who = if (m.lastSeat == 0) you else botName
                    val letters = Hexagon.cellsOf(m.lastMask).joinToString("-") { Hexagon.letter(it) }
                    Text(
                        "$who: ${letters.ifEmpty { "—" }}  " + if (m.lastOk) stringResource(R.string.hx_correct) else stringResource(R.string.hx_wrong),
                        style = MaterialTheme.typography.labelLarge, color = if (m.lastOk) pm.good else pm.danger, textAlign = TextAlign.Center,
                    )
                }
            }
            // chuông (khi mình đang trả lời: hiện số ô đã chọn)
            val canBuzz = m.phase == HexPhase.FIND && !over
            val shape = CircleShape
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(92.dp).lift(shape, 10.dp).clip(shape)
                        .background(if (canBuzz) Brush.radialGradient(listOf(Color(0xFFFF7A7A), Color(0xFFC62828))) else Brush.radialGradient(listOf(Color(0xFFB9B4C7), Color(0xFF8E88A3))))
                        .border(3.dp, Color.White.copy(alpha = .7f), shape)
                        .clickable(enabled = canBuzz) { vm.hexBuzz() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (m.phase == HexPhase.CALL && m.caller == 0) "${m.picks.size}/3" else stringResource(R.string.hx_buzz),
                        color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
    if (over) {
        val win = m.winner == 0
        SimpleDialog(
            stringResource(if (m.draw) R.string.result_draw else if (win) R.string.result_win else R.string.result_lose),
            "${m.scores[0]} : ${m.scores[1]}",
            stringResource(R.string.play_again), { vm.rematch() }, stringResource(R.string.leave), onExit,
        )
    }
    if (confirmLeave) SimpleDialog(stringResource(R.string.leave_game), stringResource(R.string.leave_confirm), stringResource(R.string.leave), { confirmLeave = false; onExit() }, stringResource(R.string.cancel), { confirmLeave = false }, tone = DialogTone.WARN)
}

@Composable
private fun ScorePill(name: String, score: Int, calling: Boolean, modifier: Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.clip(shape).background(if (calling) pm.goldSoft else pm.surface).border(if (calling) 1.5.dp else 1.dp, if (calling) pm.goldBright else pm.outline, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = pm.ink, maxLines = 1)
        Text("$score", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (score < 0) pm.danger else pm.ink)
    }
}

/** Ảnh game: tổ ong 7 ô, ô giữa có số. */
fun DrawScope.drawHexagonArt(ox: Float, oy: Float, s: Float) {
    val r = s * .17f; val c = Offset(ox + s / 2, oy + s / 2)
    val ring = listOf(Offset(0f, 0f)) + (0 until 6).map { k -> val a = Math.toRadians(60.0 * k); Offset((sqrt(3f) * r * kotlin.math.cos(a)).toFloat(), (sqrt(3f) * r * kotlin.math.sin(a)).toFloat()) }
    for ((i, d) in ring.withIndex()) {
        val p = hexPath(c + d, r * .93f)
        drawPath(p, if (i == 0) HexPaper else HexDark)
        drawPath(p, Color.Black.copy(alpha = .3f), style = Stroke(s * .01f))
    }
    drawCircle(HexPurple, r * .3f, c)
}
