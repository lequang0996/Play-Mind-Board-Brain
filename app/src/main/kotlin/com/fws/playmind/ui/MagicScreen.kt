package com.fws.playmind.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fws.playmind.PlayMindApp
import com.fws.playmind.R
import com.fws.playmind.catalog.GameInfo
import com.fws.playmind.core.MagicSquareGame
import com.fws.playmind.match.Phase
import com.fws.playmind.match.SessionViewModel

private val MgInk = Color(0xFF2A2218)        // số cho sẵn
private val MgMine = Color(0xFF1F4FB8)       // số người chơi điền
private val MgHint = Color(0xFF8A5A00)       // số gợi ý (đã khóa)
private val MgOk = Color(0xFF1E8C4E)         // tổng đúng 175
private val MgOver = Color(0xFFD12E2E)       // tổng vượt 175 hoặc đủ ô mà sai

private fun fmtTime(sec: Int) = if (sec >= 3600) "%d:%02d:%02d".format(sec / 3600, sec / 60 % 60, sec % 60) else "%d:%02d".format(sec / 60, sec % 60)

private fun sumColor(sum: Int, full: Boolean, soft: Color): Color = when {
    sum > MagicSquareGame.TARGET || (full && sum != MagicSquareGame.TARGET) -> MgOver
    full -> MgOk
    else -> soft
}

/**
 * Màn hình chơi Ma phương 7x7: bàn gỗ 7x7 như Sudoku, cột tổng bên phải mỗi hàng, hàng tổng dưới mỗi cột, tổng hai đường
 * chéo ở thanh trên. Bàn phím 49 số (số đã dùng mờ đi), Xóa và Gợi ý (gói tháng 10 lần / ngày, gói năm 20 lần / ngày).
 */
@Composable
fun MagicScreen(app: PlayMindApp, vm: SessionViewModel, info: GameInfo, onExit: () -> Unit) {
    DisposableEffect(Unit) { app.audio.calm = true; onDispose { app.audio.calm = false } }
    val g = vm.magic
    @Suppress("UNUSED_VARIABLE") val dep = vm.magicTick
    var sel by remember { mutableIntStateOf(-1) }
    val over = vm.phase == Phase.OVER
    // thoát giữa chừng không mất gì: ván được lưu, vào lại giải tiếp trong ngày
    BackHandler { onExit() }

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(info.name), onExit)
        if (g == null) { Spacer(Modifier.weight(1f)); return@Column }
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            StatChip { Icon(Ic.Hourglass, null, Modifier.size(16.dp), tint = pm.gold); Text(fmtTime(vm.magicSeconds), style = MaterialTheme.typography.titleSmall, color = pm.ink) }
            for (k in 0..1) StatChip {
                Text(if (k == 0) "↘" else "↙", style = MaterialTheme.typography.titleSmall, color = pm.gold)
                Text(g.diagSum(k).toString(), style = MaterialTheme.typography.titleSmall, color = sumColor(g.diagSum(k), g.diagFull(k), pm.ink))
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            val boardSide = if (maxWidth < maxHeight - 290.dp) maxWidth else (maxHeight - 290.dp).coerceAtLeast(200.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MagicBoard(g, sel, vm.magicTick, boardSide) { sel = it }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ToolButton(Ic.Eraser, stringResource(R.string.sd_erase), { if (sel >= 0) vm.magicEdit { it.erase(sel) } }, Modifier.weight(1f), enabled = !over)
                    val left = vm.magicHintsLeft()
                    ToolButton(Ic.Bulb, stringResource(R.string.mg_hint_n, left), { val c = vm.magicHint(sel); if (c >= 0) sel = c }, Modifier.weight(1f), enabled = !over)
                }
                MagicPad(g, enabled = !over) { v -> if (sel >= 0) vm.magicEdit { it.place(sel, v) } }
            }
        }
        Spacer(Modifier.height(10.dp))
    }

    if (over && g != null) {
        val reward = vm.reward
        PmDialog(
            title = stringResource(R.string.mg_won),
            text = stringResource(R.string.mg_won_text, fmtTime(vm.magicSeconds), g.hintsUsed),
            confirm = stringResource(R.string.back_to_lobby), onConfirm = onExit,
            tone = DialogTone.GEM, icon = Ic.Trophy,
            body = {
                if (reward != null && reward.diamonds > 0) {
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(pm.surfaceHigh).border(1.dp, pm.goldBright.copy(alpha = .7f), RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GemMark(20.dp)
                        Text(pluralStringResource(R.plurals.reward_diamonds, reward.diamonds, reward.diamonds), style = MaterialTheme.typography.titleMedium, color = pm.ink)
                    }
                    if (reward.bonus > 0) Text(pluralStringResource(R.plurals.streak_bonus, reward.bonus, reward.bonus), style = MaterialTheme.typography.labelMedium, color = pm.gold)
                }
            },
        )
    }
}

@Composable
private fun StatChip(content: @Composable () -> Unit) {
    val chip = RoundedCornerShape(50)
    Row(
        Modifier.lift(chip, 6.dp).clip(chip).background(cardBrush()).border(1.dp, hairline(), chip).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) { content() }
}

/** Bàn 7x7 + cột tổng (phải) + hàng tổng (dưới): lưới 8x8 ô, ô góc dưới phải để trống. */
@Composable
private fun MagicBoard(g: MagicSquareGame, sel: Int, tick: Int, side: Dp, onSelect: (Int) -> Unit) {
    val tm = rememberTextMeasurer()
    val n = MagicSquareGame.N
    fun geo(w: Float): Triple<Float, Float, Float> {   // lề, cỡ ô, khe
        val m = w * .03f; val gap = w * .008f; val c = (w - 2 * m - gap * n) / (n + 1)
        return Triple(m, c, gap)
    }
    Canvas(
        Modifier.size(side).clip(RoundedCornerShape(16.dp)).pointerInput(tick, sel) {
            detectTapGestures { off ->
                val (m, c, gap) = geo(size.width.toFloat())
                val col = ((off.x - m) / (c + gap)).toInt(); val row = ((off.y - m) / (c + gap)).toInt()
                if (off.x >= m && off.y >= m && col in 0 until n && row in 0 until n) onSelect(row * n + col)
            }
        },
    ) {
        @Suppress("UNUSED_VARIABLE") val dep = tick
        val w = size.width
        val (m, c, gap) = geo(w)
        drawRect(Brush.linearGradient(listOf(Color(0xFF1B2744), Color(0xFF0F1830)), Offset.Zero, Offset(w, w)))
        val boardW = n * (c + gap) - gap + m * .8f
        drawRoundRect(Brush.linearGradient(listOf(Color(0xFFC59A5A), Color(0xFFA77C3E)), Offset.Zero, Offset(w, w)), Offset(m * .6f, m * .6f), Size(boardW, boardW), CornerRadius(w * .02f))
        drawRoundRect(Brush.linearGradient(listOf(Lux.goldHi, Color(0xFF8C6A2E), Lux.goldHi), Offset.Zero, Offset(w, w)), Offset(m * .45f, m * .45f), Size(boardW + m * .3f, boardW + m * .3f), CornerRadius(w * .024f), style = Stroke(w * .004f))
        val selVal = if (sel >= 0) g.cells[sel] else 0
        fun at(r: Int, col: Int) = Offset(m + col * (c + gap), m + r * (c + gap))
        for (i in 0 until n * n) {
            val r = i / n; val col = i % n
            val tl = at(r, col)
            val peer = sel >= 0 && i != sel && (r == sel / n || col == sel % n)
            drawWoodCell(tl, c, tone = if (peer) .25f else 0f)
            if (i == sel) drawRoundRect(Lux.goldHi, tl, Size(c, c), CornerRadius(c * .12f), style = Stroke(c * .08f))
            val v = g.cells[i]
            if (v != 0) {
                val color = if (g.isGiven(i)) MgInk else if (g.isLocked(i)) MgHint else MgMine
                val layout = tm.measure(v.toString(), TextStyle(fontSize = (c * .46f).toSp(), fontWeight = if (g.isLocked(i)) FontWeight.ExtraBold else FontWeight.Bold, color = color, fontFamily = BodyFont))
                drawText(layout, topLeft = Offset(tl.x + (c - layout.size.width) / 2f, tl.y + (c - layout.size.height) / 2f))
                if (v == selVal && i != sel) drawRoundRect(Lux.gold, tl, Size(c, c), CornerRadius(c * .12f), style = Stroke(c * .04f))
            }
        }
        // tổng từng hàng (cột phải) và từng cột (hàng dưới)
        for (k in 0 until n) {
            for (isRow in listOf(true, false)) {
                val sum = if (isRow) g.rowSum(k) else g.colSum(k)
                val full = if (isRow) g.rowFull(k) else g.colFull(k)
                val tl = if (isRow) at(k, n) else at(n, k)
                val color = sumColor(sum, full, Color(0xFFE9E2D2))
                val layout = tm.measure(sum.toString(), TextStyle(fontSize = (c * .36f).toSp(), fontWeight = FontWeight.Bold, color = if (color == Color(0xFFE9E2D2)) color.copy(alpha = .8f) else color, fontFamily = BodyFont))
                if (full && sum == MagicSquareGame.TARGET) drawRoundRect(Color(0xFF1E8C4E).copy(alpha = .25f), tl + Offset(c * .08f, c * .08f), Size(c * .84f, c * .84f), CornerRadius(c * .2f))
                drawText(layout, topLeft = Offset(tl.x + (c - layout.size.width) / 2f, tl.y + (c - layout.size.height) / 2f))
            }
        }
        // ô góc: hằng số 175 làm mốc
        val corner = at(n, n)
        val tgt = tm.measure("175", TextStyle(fontSize = (c * .3f).toSp(), fontWeight = FontWeight.ExtraBold, color = Lux.goldHi, fontFamily = BodyFont))
        drawText(tgt, topLeft = Offset(corner.x + (c - tgt.size.width) / 2f, corner.y + (c - tgt.size.height) / 2f))
    }
}

/** Bàn phím 49 số (10 phím mỗi hàng). Số đã có trên bàn mờ đi (chạm vẫn được: số đó chuyển sang ô đang chọn), số đã khóa thì tắt. */
@Composable
private fun MagicPad(g: MagicSquareGame, enabled: Boolean, onNumber: (Int) -> Unit) {
    val perRow = 10
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        for (row in 0 until 5) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (k in 0 until perRow) {
                    val v = row * perRow + k + 1
                    if (v > MagicSquareGame.CELLS) { Spacer(Modifier.weight(1f)); continue }
                    val pos = g.cells.indexOf(v)
                    val used = pos >= 0
                    val lockedHere = used && g.isLocked(pos)
                    val shape = RoundedCornerShape(9.dp)
                    val src = rememberPress()
                    Box(
                        Modifier.weight(1f).bounce(src, .9f).height(38.dp).then(if (used) Modifier else Modifier.lift(shape, 4.dp, Color(0xFF8B5E2B))).clip(shape)
                            .background(if (used) Brush.verticalGradient(listOf(pm.surfaceHigh, pm.bgDeep)) else Brush.verticalGradient(listOf(Color(0xFFF2D7A4), Color(0xFFDDB574), Color(0xFFC99A58))))
                            .border(1.dp, if (used) hairline() else Brush.verticalGradient(listOf(Color(0xFFFFF1D2), Color(0xFF8B5E2B))), shape)
                            .tap(src, enabled = enabled && !lockedHere) { onNumber(v) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(v.toString(), style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, lineHeight = 16.sp), color = if (used) pm.outlineDeep else MgInk, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
