package com.fws.playmind.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fws.playmind.PlayMindApp
import com.fws.playmind.R
import com.fws.playmind.catalog.GameInfo
import com.fws.playmind.core.Tangram
import com.fws.playmind.core.TangramPuzzle
import com.fws.playmind.match.Phase
import com.fws.playmind.match.SessionViewModel
import kotlin.math.min

/** Màu từng mảnh (tối đa 10 mảnh ở cấp Siêu việt). */
private val TgColors = listOf(
    Color(0xFF2EBF6B), Color(0xFFF5B82E), Color(0xFF8E5BD6), Color(0xFFE94B5B), Color(0xFFF08BB0), Color(0xFF29B6E0), Color(0xFF3F7DE8),
    Color(0xFFFF8A3D), Color(0xFF14A38B), Color(0xFFB0794A),
)
private val TgShadow = Color(0xFF6E7380)

private fun fmt(sec: Int) = "%d:%02d".format(sec / 60, sec % 60)

/** Một mảnh người chơi đang giữ: loại, góc (bước 45°), lật, tâm (toạ độ thế giới: đơn vị = cạnh tam giác nhỏ, gốc = tâm hình). */
private class TgPiece(val idx: Int, val kind: Int, var rot: Int, var flip: Boolean, var x: Double, var y: Double) {
    fun poly() = Tangram.shape(kind, rot, flip, x, y)
}

private fun DrawScope.polyPath(p: DoubleArray, ox: Float, oy: Float, sc: Float): Path = Path().apply {
    moveTo(ox + p[0].toFloat() * sc, oy + p[1].toFloat() * sc)
    for (k in 1 until p.size / 2) lineTo(ox + p[2 * k].toFloat() * sc, oy + p[2 * k + 1].toFloat() * sc)
    close()
}

/** Tranh minh hoạ: hình vuông ghép từ 7 mảnh. */
fun DrawScope.drawTangramArt(ox: Float, oy: Float, s: Float) {
    val sc = s * .21f
    // hình vuông 4×4 (đơn vị nửa cạnh): toạ độ đỉnh viết tay
    val polys = listOf(
        doubleArrayOf(0.0, 0.0, 4.0, 0.0, 2.0, 2.0), doubleArrayOf(0.0, 0.0, 2.0, 2.0, 0.0, 4.0),
        doubleArrayOf(4.0, 4.0, 2.0, 4.0, 4.0, 2.0), doubleArrayOf(2.0, 2.0, 3.0, 1.0, 3.0, 3.0),
        doubleArrayOf(0.0, 4.0, 1.0, 3.0, 2.0, 4.0), doubleArrayOf(2.0, 2.0, 3.0, 3.0, 2.0, 4.0, 1.0, 3.0),
        doubleArrayOf(3.0, 1.0, 4.0, 0.0, 4.0, 2.0, 3.0, 3.0),
    )
    val x0 = ox + (s - 4 * sc) / 2; val y0 = oy + (s - 4 * sc) / 2
    for ((i, p) in polys.withIndex()) {
        val path = polyPath(p, x0, y0, sc)
        drawPath(path, TgColors[i])
        drawPath(path, Color.White.copy(alpha = .8f), style = Stroke(s * .012f, join = StrokeJoin.Round))
    }
}

/**
 * Màn hình Xếp hình 7 mảnh: hình xám ở trên, 7 mảnh ở khay dưới. Kéo mảnh vào hình; chạm để xoay 45°, nhấn giữ để lật
 * (hình bình hành). Thả mảnh gần một đỉnh của hình hoặc của mảnh khác thì mảnh tự hít vào. Mỗi hình 3 phút.
 */
@Composable
fun TangramScreen(app: PlayMindApp, vm: SessionViewModel, info: GameInfo, onExit: () -> Unit) {
    DisposableEffect(Unit) { app.audio.calm = true; onDispose { app.audio.calm = false } }
    val over = vm.phase == Phase.OVER
    var confirmLeave by remember { mutableStateOf(false) }
    BackHandler { if (over) onExit() else confirmLeave = true }
    val idx = vm.tgIndex
    val puzzle = vm.tangram.getOrNull(idx)

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(info.name), { if (over) onExit() else confirmLeave = true })
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            TgChip { Icon(Ic.Puzzle, null, Modifier.size(16.dp), tint = pm.gold); Text(stringResource(R.string.tg_progress, idx + 1, Tangram.PUZZLES), style = MaterialTheme.typography.titleSmall, color = pm.ink) }
            TgChip {
                Icon(Ic.Hourglass, null, Modifier.size(16.dp), tint = if (vm.tgSeconds <= 30) pm.danger else pm.gold)
                Text(fmt(vm.tgSeconds), style = MaterialTheme.typography.titleSmall, color = if (vm.tgSeconds <= 30) pm.danger else pm.ink)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
            if (puzzle == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = pm.gold)
                    Text(stringResource(R.string.sd_generating), style = MaterialTheme.typography.bodyMedium, color = pm.inkSoft)
                }
            } else TangramBoard(vm, puzzle, idx, enabled = !over && !vm.tgSolved)
            if (vm.tgSolved) {
                Text(
                    stringResource(R.string.tg_success),
                    Modifier.clip(RoundedCornerShape(50)).background(pm.surface.copy(alpha = .95f)).border(2.dp, pm.goldBright, RoundedCornerShape(50))
                        .padding(horizontal = 26.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.headlineSmall, color = pm.gold,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (over) {
        val won = vm.tgIndex >= Tangram.PUZZLES - 1 && vm.tgSeconds > 0
        val reward = vm.reward
        PmDialog(
            title = stringResource(if (won) R.string.tg_won else R.string.tg_lost),
            text = if (won) stringResource(R.string.tg_won_text, fmt(vm.tgTotal)) else stringResource(R.string.tg_lost_text, vm.tgIndex + 1),
            confirm = stringResource(R.string.play_again), onConfirm = { vm.rematch() },
            dismiss = stringResource(R.string.back_to_lobby), onDismiss = onExit,
            tone = if (won) DialogTone.GEM else DialogTone.WARN, icon = if (won) Ic.Trophy else Ic.Hourglass,
            body = {
                if (vm.unlockedNow >= 0) Text(stringResource(R.string.level_unlocked, levelName(vm.unlockedNow)), style = MaterialTheme.typography.labelLarge, color = pm.gold)
                if (reward != null && reward.diamonds > 0) {
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(pm.surfaceHigh).border(1.dp, pm.goldBright.copy(alpha = .7f), RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GemMark(20.dp)
                        Text(pluralStringResource(R.plurals.reward_diamonds, reward.diamonds, reward.diamonds), style = MaterialTheme.typography.titleMedium, color = pm.ink)
                    }
                }
            },
        )
    }
    if (confirmLeave) SimpleDialog(stringResource(R.string.leave_game), stringResource(R.string.leave_confirm), stringResource(R.string.leave), { confirmLeave = false; onExit() }, stringResource(R.string.cancel), { confirmLeave = false }, tone = DialogTone.WARN)
}

@Composable
private fun TgChip(content: @Composable () -> Unit) {
    val chip = RoundedCornerShape(50)
    Row(
        Modifier.lift(chip, 6.dp).clip(chip).background(cardBrush()).border(1.dp, hairline(), chip).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) { content() }
}

@Composable
private fun TangramBoard(vm: SessionViewModel, puzzle: TangramPuzzle, puzzleIndex: Int, enabled: Boolean) {
    val pieces = remember(puzzleIndex) { mutableStateListOf<TgPiece>() }
    var version by remember(puzzleIndex) { mutableIntStateOf(0) }
    var w by remember { mutableFloatStateOf(0f) }
    var h by remember { mutableFloatStateOf(0f) }
    var dragging by remember(puzzleIndex) { mutableIntStateOf(-1) }

    // thế giới -> màn hình: gốc = tâm hình (ở 30% chiều cao), khay mảnh ở phần dưới (2 hàng)
    val kinds = remember(puzzleIndex) { puzzle.pieces.map { it.kind }.sorted().toIntArray() }
    val n = kinds.size
    val cols = (n + 1) / 2
    val ext = maxOf(puzzle.width, puzzle.height, 5.0).toFloat()
    val sc = if (w > 0) minOf(w / (ext + 2.4f), h * .55f / (ext + 1.0f), w / (cols * 2.35f)) else 1f
    val ox = w / 2f; val oy = h * .30f
    fun toWorld(o: Offset) = ((o.x - ox) / sc).toDouble() to ((o.y - oy) / sc).toDouble()

    if (pieces.isEmpty() && w > 0) {
        // khay: hàng trên các mảnh lớn trước, hàng dưới phần còn lại
        val rowY = doubleArrayOf((h * .71f - oy) / sc.toDouble(), (h * .89f - oy) / sc.toDouble())
        for (i in 0 until n) {
            val row = if (i < cols) 0 else 1; val col = if (i < cols) i else i - cols; val inRow = if (row == 0) cols else n - cols
            val sx = (w * (col + .5f) / inRow - ox) / sc
            pieces.add(TgPiece(i, kinds[i], 0, false, sx.toDouble(), rowY[row]))
        }
    }

    fun topAt(o: Offset): Int {
        val (x, y) = toWorld(o)
        for (k in pieces.indices.reversed()) if (Tangram.inside(pieces[k].poly(), x, y, 0.08)) return k
        return -1
    }
    fun settle(k: Int) {
        val p = pieces[k]
        val targets = ArrayList<Double>()
        puzzle.outlineVertices.forEach { targets.add(it) }
        for (j in pieces.indices) if (j != k) pieces[j].poly().forEach { targets.add(it) }
        val (dx, dy) = Tangram.snap(p.poly(), targets.toDoubleArray(), 0.32)
        p.x += dx; p.y += dy
        version++
        if (Tangram.coverError(pieces.map { it.poly() }, puzzle) < Tangram.SOLVED_ERROR) vm.tangramSolved()
    }

    Box(
        Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(Color(0xFFF4F1EA))
            .border(1.dp, pm.outline, RoundedCornerShape(20.dp))
            .onSizeChanged { w = it.width.toFloat(); h = it.height.toFloat() },
    ) {
        Canvas(
            Modifier.fillMaxSize()
                .pointerInput(puzzleIndex, enabled, w, h) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { o ->
                            val k = topAt(o)
                            if (k >= 0) { val p = pieces.removeAt(k); pieces.add(p); dragging = pieces.size - 1 } else dragging = -1
                        },
                        onDrag = { ch, amt ->
                            val k = dragging
                            if (k >= 0) { ch.consume(); pieces[k].x += amt.x / sc; pieces[k].y += amt.y / sc; version++ }
                        },
                        onDragEnd = { val k = dragging; dragging = -1; if (k >= 0) settle(k) },
                        onDragCancel = { dragging = -1 },
                    )
                }
                .pointerInput(puzzleIndex, enabled, w, h) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onTap = { o -> val k = topAt(o); if (k >= 0) { pieces[k].rot = (pieces[k].rot + 1) % 8; settle(k) } },
                        onLongPress = { o -> val k = topAt(o); if (k >= 0 && pieces[k].kind == 4) { pieces[k].flip = !pieces[k].flip; settle(k) } },
                    )
                },
        ) {
            @Suppress("UNUSED_VARIABLE") val dep = version
            if (w <= 0f) return@Canvas
            // hình đề (xám): tô từng mảnh đáp án cùng màu, viền cùng màu để không lộ đường nối
            for (pl in puzzle.pieces) {
                val path = polyPath(pl.poly, ox, oy, sc)
                drawPath(path, TgShadow)
                drawPath(path, TgShadow, style = Stroke(1.6f, join = StrokeJoin.Round))
            }
            // vạch ngăn khay
            drawLine(Color(0xFFD8D2C4), Offset(w * .06f, h * .6f), Offset(w * .94f, h * .6f), 2f)
            for ((k, p) in pieces.withIndex()) {
                val path = polyPath(p.poly(), ox, oy, sc)
                if (k == dragging) drawPath(polyPath(p.poly(), ox + sc * .06f, oy + sc * .1f, sc), Color.Black.copy(alpha = .18f))
                drawPath(path, TgColors[p.idx])
                drawPath(path, lerp(TgColors[p.idx], Color.Black, .25f), style = Stroke(2.2f, join = StrokeJoin.Round))
            }
        }
        if (puzzleIndex == 0) Text(
            stringResource(R.string.tg_hint),
            Modifier.align(Alignment.TopCenter).padding(10.dp).clip(RoundedCornerShape(50)).background(pm.surface.copy(alpha = .9f))
                .padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall, color = pm.inkSoft,
        )
    }
}
