package com.fws.playmind.ui

import android.app.Activity
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fws.playmind.PlayMindApp
import com.fws.playmind.R
import com.fws.playmind.catalog.GameInfo
import com.fws.playmind.core.SudokuGame
import com.fws.playmind.core.SudokuStatus
import com.fws.playmind.match.Phase
import com.fws.playmind.match.SessionViewModel

private val SdInk = Color(0xFF2A2218)        // số cho sẵn: khắc đậm
private val SdMine = Color(0xFF1F4FB8)       // số người chơi điền: xanh mực
private val SdWrong = Color(0xFFD12E2E)      // số sai: đỏ
private val SdNote = Color(0xFFD32F2F)       // ghi chú nháp: màu đỏ để khác với số đã điền

private fun fmtTime(sec: Int) = "%d:%02d".format(sec / 60, sec % 60)

/** Màn hình chơi Sudoku: bàn gỗ sồi như bộ cờ gỗ thật (ô cho sẵn đen, số điền xanh, số sai đỏ, ghi chú đỏ), bàn phím số, xóa và ghi chú. */
@Composable
fun SudokuScreen(app: PlayMindApp, vm: SessionViewModel, info: GameInfo, onExit: () -> Unit) {
    val activity = LocalContext.current as Activity
    val g = vm.sudoku
    @Suppress("UNUSED_VARIABLE") val dep = vm.sudokuTick
    var sel by remember { mutableIntStateOf(-1) }
    var notes by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    val over = vm.phase == Phase.OVER
    BackHandler { if (over) onExit() else confirmLeave = true }

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(info.name), { if (over) onExit() else confirmLeave = true })
        if (g == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = pm.gold)
                    Text(stringResource(R.string.sd_generating), style = MaterialTheme.typography.bodyMedium, color = pm.inkSoft)
                }
            }
        } else {
            // thanh trạng thái: chỉ còn đồng hồ
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp), horizontalArrangement = Arrangement.Center) {
                val chip = RoundedCornerShape(50)
                Row(
                    Modifier.lift(chip, 6.dp).clip(chip).background(cardBrush()).border(1.dp, hairline(), chip).padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Ic.Hourglass, null, Modifier.size(16.dp), tint = pm.gold)
                    Text(fmtTime(vm.sudokuSeconds), style = MaterialTheme.typography.titleMedium, color = pm.ink)
                }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                val boardSide = if (maxWidth < maxHeight - 140.dp) maxWidth else maxHeight - 140.dp
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SudokuBoard(g, sel, vm.sudokuTick, boardSide) { sel = it }
                    SudokuTools(
                        notes = notes,
                        onErase = { if (sel >= 0) vm.sudokuEdit { it.erase(sel) } },
                        onNotes = { notes = !notes },
                    )
                    SudokuPad(g) { d ->
                        if (sel >= 0 && !over) vm.sudokuEdit { if (notes) it.toggleNote(sel, d) else it.place(sel, d) }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }

    if (over && g != null) {
        val won = g.status == SudokuStatus.WON
        val reward = vm.reward
        val exit = onExit
        PmDialog(
            title = stringResource(if (won) R.string.sd_won else R.string.sd_lost),
            text = if (won) stringResource(R.string.sd_won_text, fmtTime(vm.sudokuSeconds), g.mistakes) else stringResource(R.string.sd_lost_text),
            confirm = stringResource(R.string.play_again), onConfirm = { vm.rematch() },
            dismiss = stringResource(R.string.back_to_lobby), onDismiss = exit,
            tone = if (won) DialogTone.GEM else DialogTone.WARN,
            icon = if (won) Ic.Trophy else Ic.Heart,
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
    if (confirmLeave) SimpleDialog(stringResource(R.string.leave_game), stringResource(R.string.leave_confirm), stringResource(R.string.leave), { confirmLeave = false; onExit() }, stringResource(R.string.cancel), { confirmLeave = false }, tone = DialogTone.WARN)
}

@Composable
private fun SudokuBoard(g: SudokuGame, sel: Int, tick: Int, side: androidx.compose.ui.unit.Dp, onSelect: (Int) -> Unit) {
    val tm = rememberTextMeasurer()
    // Vị trí ô: 6 khe nhỏ + 2 khe giữa các khối 3x3
    fun pos(w: Float, i: Int): Pair<Float, Float> {
        val m = w * .03f; val c = (w - 2 * m) / 9.64f; val g1 = c * .06f; val g2 = c * .14f
        val before = (i - i / 3) * g1 + (i / 3) * g2
        return (m + i * c + before) to c
    }
    Canvas(
        Modifier.size(side).clip(RoundedCornerShape(16.dp)).pointerInput(tick, sel) {
            detectTapGestures { off ->
                val w = size.width.toFloat()
                var col = -1; var row = -1
                for (i in 0..8) {
                    val (p, c) = pos(w, i)
                    if (off.x >= p - c * .06f && off.x <= p + c * 1.06f) col = i
                    if (off.y >= p - c * .06f && off.y <= p + c * 1.06f) row = i
                }
                if (col >= 0 && row >= 0) onSelect(row * 9 + col)
            }
        },
    ) {
        @Suppress("UNUSED_VARIABLE") val dep = tick
        val w = size.width
        drawRect(Brush.linearGradient(listOf(Color(0xFF1B2744), Color(0xFF0F1830)), Offset.Zero, Offset(w, w)))
        val m = w * .03f
        drawRoundRect(Brush.linearGradient(listOf(Color(0xFFC59A5A), Color(0xFFA77C3E)), Offset.Zero, Offset(w, w)), Offset(m * .6f, m * .6f), Size(w - m * 1.2f, w - m * 1.2f), CornerRadius(w * .02f))
        // viền vàng mảnh quanh mặt gỗ + vệt sáng chéo rất nhẹ cho cảm giác sơn bóng cao cấp
        drawRoundRect(Brush.linearGradient(listOf(Lux.goldHi, Color(0xFF8C6A2E), Lux.goldHi), Offset.Zero, Offset(w, w)), Offset(m * .45f, m * .45f), Size(w - m * .9f, w - m * .9f), CornerRadius(w * .024f), style = Stroke(w * .004f))
        val selVal = if (sel >= 0) g.cells[sel] else 0
        for (i in 0 until 81) {
            val (px, c) = pos(w, i % 9); val (py, _) = pos(w, i / 9)
            val tl = Offset(px, py)
            val peer = sel >= 0 && i != sel && (i / 9 == sel / 9 || i % 9 == sel % 9 || (i / 27 == sel / 27 && (i % 9) / 3 == (sel % 9) / 3))
            val same = selVal != 0 && g.cells[i] == selVal
            drawWoodCell(tl, c, tone = if (same) .55f else if (peer) .25f else 0f)
            if (g.isWrong(i)) drawRoundRect(SdWrong.copy(alpha = .28f), tl, Size(c, c), CornerRadius(c * .12f))
            if (i == sel) drawRoundRect(Lux.goldHi, tl, Size(c, c), CornerRadius(c * .12f), style = Stroke(c * .07f))
            val v = g.cells[i]
            if (v != 0) {
                val color = if (g.isGiven(i)) SdInk else if (g.isWrong(i)) SdWrong else SdMine
                val layout = tm.measure(v.toString(), TextStyle(fontSize = (c * .62f).toSp(), fontWeight = if (g.isGiven(i)) FontWeight.ExtraBold else FontWeight.Bold, color = color, fontFamily = BodyFont))
                drawText(layout, topLeft = Offset(tl.x + (c - layout.size.width) / 2f, tl.y + (c - layout.size.height) / 2f))
            } else if (g.notes[i] != 0) {
                for (d in 1..9) if ((g.notes[i] shr (d - 1)) and 1 == 1) {
                    val cx = tl.x + c * (((d - 1) % 3) * .31f + .19f); val cy = tl.y + c * (((d - 1) / 3) * .31f + .19f)
                    val layout = tm.measure(d.toString(), TextStyle(fontSize = (c * .26f).toSp(), fontWeight = FontWeight.Bold, color = SdNote, fontFamily = BodyFont))
                    drawText(layout, topLeft = Offset(cx - layout.size.width / 2f, cy - layout.size.height / 2f))
                }
            }
        }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, active: Boolean = false, enabled: Boolean = true) {
    val shape = RoundedCornerShape(18.dp)
    val src = rememberPress()
    Row(
        modifier.bounce(src).height(50.dp).lift(shape, 6.dp).clip(shape)
            .background(if (active) Brush.verticalGradient(listOf(pm.goldSoft, pm.goldHi.copy(alpha = .6f))) else cardBrush())
            .border(1.dp, if (active) Brush.verticalGradient(listOf(pm.goldHi, pm.goldBright)) else hairline(), shape)
            .tap(src, enabled = enabled, onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, label, Modifier.size(20.dp), tint = if (!enabled) pm.outline else if (active) pm.gold else pm.ink)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (!enabled) pm.outline else if (active) pm.gold else pm.ink)
    }
}

@Composable
private fun SudokuTools(notes: Boolean, onErase: () -> Unit, onNotes: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ToolButton(Ic.Eraser, stringResource(R.string.sd_erase), onErase, Modifier.weight(1f))
        ToolButton(Ic.Pencil, stringResource(R.string.sd_notes), onNotes, Modifier.weight(1f), active = notes)
    }
}

/** Bàn phím số dạng phím gỗ bóng: số lớn + số lượng còn thiếu nhỏ bên dưới; số đã đủ 9 thì mờ đi. */
@Composable
private fun SudokuPad(g: SudokuGame, onDigit: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        for (d in 1..9) {
            val left = (9 - g.correctCount(d)).coerceAtLeast(0)
            val done = left == 0
            val shape = RoundedCornerShape(12.dp)
            val src = rememberPress()
            Column(
                Modifier.weight(1f).bounce(src, .9f).height(56.dp).then(if (done) Modifier else Modifier.lift(shape, 5.dp, Color(0xFF8B5E2B))).clip(shape)
                    .background(if (done) Brush.verticalGradient(listOf(pm.surfaceHigh, pm.bgDeep)) else Brush.verticalGradient(listOf(Color(0xFFF2D7A4), Color(0xFFDDB574), Color(0xFFC99A58))))
                    .background(Brush.verticalGradient(0f to Color.White.copy(alpha = if (done) 0f else .35f), .45f to Color.Transparent))
                    .border(1.dp, if (done) hairline() else Brush.verticalGradient(listOf(Color(0xFFFFF1D2), Color(0xFF8B5E2B))), shape)
                    .tap(src, enabled = !done) { onDigit(d) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Text(d.toString(), style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp, lineHeight = 22.sp), color = if (done) pm.outlineDeep else SdInk, textAlign = TextAlign.Center)
                if (!done) Text(left.toString(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 10.sp), color = SdInk.copy(alpha = .55f))
            }
        }
    }
}
