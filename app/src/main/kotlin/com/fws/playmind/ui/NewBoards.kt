package com.fws.playmind.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fws.playmind.R
import com.fws.playmind.core.GoGame
import com.fws.playmind.core.QuoridorGame
import com.fws.playmind.core.WallGoGame
import com.fws.playmind.match.Mode
import com.fws.playmind.match.SessionViewModel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

// ═════════════════════════════════ WALL GO ═════════════════════════════════

/**
 * Bàn Wall Go 7x7. Đặt quân: chạm ô trống. Chơi: chạm quân của mình -> chạm ô đích sáng (hoặc chạm lại quân để đứng yên)
 * -> chạm về phía cạnh muốn đặt tường (các cạnh khả dụng sáng vàng). Ô thuộc lãnh thổ đã khép kín được tô màu chủ nhân.
 */
@Composable
fun WallGoBoardView(vm: SessionViewModel, modifier: Modifier = Modifier) {
    val g = vm.game as? WallGoGame ?: return
    val tick = vm.tick
    var selFrom by remember { mutableIntStateOf(-1) }
    var selTo by remember { mutableIntStateOf(-1) }
    LaunchedEffect(tick) { selFrom = -1; selTo = -1 }
    val act = vm.canAct
    val over = !act && vm.phase == com.fws.playmind.match.Phase.OVER
    val reach = if (!g.inSetup && selFrom >= 0) g.reach(selFrom) else IntArray(0)
    val sides = if (selTo >= 0) g.wallSides(selTo) else 0
    val owners = remember(tick) { g.regionOwners() }
    val last = g.lastMove

    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val side = if (maxWidth < maxHeight - 56.dp) maxWidth else maxHeight - 56.dp
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Canvas(
                Modifier.size(side).clip(RoundedCornerShape(18.dp))
                    .pointerInput(tick, selFrom, selTo, act) {
                        detectTapGestures { off ->
                            if (!act) return@detectTapGestures
                            val w = size.width.toFloat(); val pad = w * .035f; val cs = (w - 2 * pad) / 7f
                            val col = ((off.x - pad) / cs).toInt(); val row = ((off.y - pad) / cs).toInt()
                            val inside = col in 0..6 && row in 0..6
                            val cell = if (inside) row * 7 + col else -1
                            if (g.inSetup) {
                                if (cell >= 0 && g.ownerAt(cell) == -1) vm.playMove(WallGoGame.mvPlace(cell))
                                return@detectTapGestures
                            }
                            if (selTo >= 0) {
                                // chọn cạnh đặt tường theo hướng chạm so với tâm ô đích
                                val cx = pad + (selTo % 7 + .5f) * cs; val cy = pad + (selTo / 7 + .5f) * cs
                                val dx = (off.x - cx) / cs; val dy = (off.y - cy) / cs
                                if (abs(dx) > 1.7f || abs(dy) > 1.7f) { selFrom = -1; selTo = -1; return@detectTapGestures }
                                if (max(abs(dx), abs(dy)) < .12f) return@detectTapGestures
                                val s = if (abs(dx) > abs(dy)) (if (dx > 0) WallGoGame.RIGHT else WallGoGame.LEFT) else (if (dy > 0) WallGoGame.DOWN else WallGoGame.UP)
                                if ((sides shr s) and 1 == 1) vm.playMove(WallGoGame.mvMove(selFrom, selTo, s))
                                return@detectTapGestures
                            }
                            if (selFrom >= 0) {
                                if (cell >= 0 && cell in reach) selTo = cell
                                else if (cell >= 0 && g.ownerAt(cell) == g.turn) selFrom = cell
                                else selFrom = -1
                            } else if (cell >= 0 && g.ownerAt(cell) == g.turn) selFrom = cell
                        }
                    },
            ) {
                @Suppress("UNUSED_VARIABLE") val dep = tick
                val w = size.width; val pad = w * .035f; val cs = (w - 2 * pad) / 7f
                fun cx(c: Int) = pad + (c % 7 + .5f) * cs
                fun cy(c: Int) = pad + (c / 7 + .5f) * cs
                drawRect(Brush.linearGradient(listOf(Color(0xFF7A5A38), Color(0xFF4A3420)), Offset.Zero, Offset(w, w)))
                for (c in 0 until 49) {
                    val tl = Offset(pad + (c % 7) * cs + cs * .045f, pad + (c / 7) * cs + cs * .045f)
                    val base = Color(0xFF9A7650)
                    val col = if (owners[c] >= 0) lerp(base, playerColor(owners[c]), .42f) else base
                    drawRoundRect(col, tl, Size(cs * .91f, cs * .91f), CornerRadius(cs * .1f))
                    drawRoundRect(Color.White.copy(alpha = .10f), tl, Size(cs * .91f, cs * .91f), CornerRadius(cs * .1f), style = Stroke(cs * .02f))
                }
                // tường đã đặt (vẽ cạnh PHẢI và DƯỚI của mỗi ô để không vẽ trùng)
                val ww = cs * .12f
                for (c in 0 until 49) {
                    val x0 = pad + (c % 7) * cs; val y0 = pad + (c / 7) * cs
                    if ((g.wallsAt(c) shr WallGoGame.RIGHT) and 1 == 1) {
                        val o = g.wallOwnerAt(c, WallGoGame.RIGHT); val colr = if (o >= 0) playerColor(o) else Color(0xFFDDE0EA)
                        drawLine(Color.Black.copy(alpha = .35f), Offset(x0 + cs + ww * .25f, y0 + ww * .35f), Offset(x0 + cs + ww * .25f, y0 + cs + ww * .1f), ww, StrokeCap.Round)
                        drawLine(colr, Offset(x0 + cs, y0 + ww * .2f), Offset(x0 + cs, y0 + cs - ww * .2f), ww, StrokeCap.Round)
                    }
                    if ((g.wallsAt(c) shr WallGoGame.DOWN) and 1 == 1) {
                        val o = g.wallOwnerAt(c, WallGoGame.DOWN); val colr = if (o >= 0) playerColor(o) else Color(0xFFDDE0EA)
                        drawLine(Color.Black.copy(alpha = .35f), Offset(x0 + ww * .35f, y0 + cs + ww * .25f), Offset(x0 + cs + ww * .1f, y0 + cs + ww * .25f), ww, StrokeCap.Round)
                        drawLine(colr, Offset(x0 + ww * .2f, y0 + cs), Offset(x0 + cs - ww * .2f, y0 + cs), ww, StrokeCap.Round)
                    }
                }
                // nước đi gần nhất
                if (last >= 0) {
                    val c = if (WallGoGame.isPlay(last)) WallGoGame.mvTo(last) else WallGoGame.mvCell(last)
                    drawCircle(Lux.goldHi.copy(alpha = .9f), cs * .42f, Offset(cx(c), cy(c)), style = Stroke(cs * .045f))
                }
                // ô đến được
                for (c in reach) {
                    if (c == selFrom) drawCircle(Lux.goldHi.copy(alpha = .9f), cs * .40f, Offset(cx(c), cy(c)), style = Stroke(cs * .05f))
                    else drawCircle(Lux.goldHi.copy(alpha = .85f), cs * .12f, Offset(cx(c), cy(c)))
                }
                // quân
                for (c in 0 until 49) {
                    val o = g.ownerAt(c); if (o < 0) continue
                    drawPawn(Offset(cx(c), cy(c) + cs * .06f), cs * .21f, playerColor(o))
                    if (c == selFrom) drawCircle(Color.White.copy(alpha = .85f), cs * .40f, Offset(cx(c), cy(c)), style = Stroke(cs * .04f))
                }
                // ô đích đã chọn + các cạnh có thể đặt tường
                if (selTo >= 0) {
                    val x0 = pad + (selTo % 7) * cs; val y0 = pad + (selTo / 7) * cs
                    drawRoundRect(Lux.goldHi.copy(alpha = .30f), Offset(x0 + cs * .045f, y0 + cs * .045f), Size(cs * .91f, cs * .91f), CornerRadius(cs * .1f))
                    if (selTo != selFrom) drawPawn(Offset(cx(selTo), cy(selTo) + cs * .06f), cs * .21f, playerColor(g.turn))
                    val gold = Lux.goldHi
                    if ((sides shr WallGoGame.UP) and 1 == 1) drawLine(gold, Offset(x0 + ww, y0), Offset(x0 + cs - ww, y0), ww * 1.4f, StrokeCap.Round)
                    if ((sides shr WallGoGame.DOWN) and 1 == 1) drawLine(gold, Offset(x0 + ww, y0 + cs), Offset(x0 + cs - ww, y0 + cs), ww * 1.4f, StrokeCap.Round)
                    if ((sides shr WallGoGame.LEFT) and 1 == 1) drawLine(gold, Offset(x0, y0 + ww), Offset(x0, y0 + cs - ww), ww * 1.4f, StrokeCap.Round)
                    if ((sides shr WallGoGame.RIGHT) and 1 == 1) drawLine(gold, Offset(x0 + cs, y0 + ww), Offset(x0 + cs, y0 + cs - ww), ww * 1.4f, StrokeCap.Round)
                }
            }
            Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val hint = when {
                    over -> ""
                    vm.botThinking -> stringResource(R.string.thinking)
                    !act -> stringResource(R.string.their_turn)
                    g.inSetup -> stringResource(R.string.wg_hint_place, g.stonesPlaced + 1, g.stonesTotal)
                    selTo >= 0 -> stringResource(R.string.wg_hint_wall)
                    selFrom >= 0 -> stringResource(R.string.wg_hint_dest)
                    else -> stringResource(R.string.wg_hint_pick)
                }
                Text(hint, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = pm.gold, textAlign = TextAlign.Center)
                if (selFrom >= 0 && act) OutlineButton(stringResource(R.string.cancel), { selFrom = -1; selTo = -1 }, Modifier.height(40.dp))
            }
        }
    }
}

// ═════════════════════════════════ QUORIDOR ═════════════════════════════════

private enum class QMode { MOVE, WALL_H, WALL_V }

/**
 * Bàn Quoridor 9x9. Chế độ "Di chuyển": chạm ô sáng để đi. Chế độ "Tường": chạm gần điểm giao giữa bốn ô để xem trước tường
 * (xanh = hợp lệ, đỏ = không được vì cắt tường khác hoặc chặn kín đường), rồi bấm "Đặt tường".
 * Ở ván 2 người online, người chơi bên 1 được xoay bàn để quân mình luôn ở phía dưới.
 */
@Composable
fun QuoridorBoardView(vm: SessionViewModel, modifier: Modifier = Modifier) {
    val g = vm.game as? QuoridorGame ?: return
    val tick = vm.tick
    var mode by remember { mutableStateOf(QMode.MOVE) }
    var pr by remember { mutableIntStateOf(-1) }      // neo xem trước (logic): r * 8 + c
    LaunchedEffect(tick) { pr = -1 }
    val act = vm.canAct
    val flip = g.players == 2 && vm.mode != Mode.LOCAL && vm.mySide == 1
    val moves = if (act && mode == QMode.MOVE) g.pawnMoves(g.turn) else IntArray(0)
    val previewH = mode == QMode.WALL_H
    val previewOk = pr >= 0 && mode != QMode.MOVE && act && g.canPlaceWall(previewH, pr / 8, pr % 8)
    val last = g.lastMove
    val wallsLeft = if (g.turn in 0 until g.players) g.wallsOf(g.turn) else 0
    /** Ai đã đặt từng bức tường (phát lại lịch sử ván) để tô dải màu dưới tường. */
    val wallOwners = remember(tick) {
        val out = HashMap<Int, Int>(); val replay = QuoridorGame(g.players)
        for (mv in g.history) { val who = replay.turn; if (!replay.play(mv)) break; if (QuoridorGame.isWall(mv)) out[mv] = who }
        out
    }

    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val side = if (maxWidth < maxHeight - 112.dp) maxWidth else maxHeight - 112.dp
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Canvas(
                Modifier.size(side).clip(RoundedCornerShape(18.dp))
                    .pointerInput(tick, mode, pr, act, flip) {
                        detectTapGestures { off ->
                            if (!act) return@detectTapGestures
                            val w = size.width.toFloat(); val m = w * .045f; val cell = (w - 2 * m) / 11.08f; val step = cell * 1.26f
                            if (mode == QMode.MOVE) {
                                val col = ((off.x - m) / step).toInt(); val row = ((off.y - m) / step).toInt()
                                if (col !in 0..8 || row !in 0..8) return@detectTapGestures
                                val lr = if (flip) 8 - row else row; val lc = if (flip) 8 - col else col
                                val dest = lr * 9 + lc
                                if (dest in moves) vm.playMove(dest)
                            } else {
                                // điểm neo gần nhất (hiển thị) -> neo logic
                                var best = -1; var bd = Float.MAX_VALUE
                                for (r in 0..7) for (c in 0..7) {
                                    val ax = m + c * step + cell + (step - cell) / 2f; val ay = m + r * step + cell + (step - cell) / 2f
                                    val d = (off.x - ax) * (off.x - ax) + (off.y - ay) * (off.y - ay)
                                    if (d < bd) { bd = d; best = r * 8 + c }
                                }
                                if (best >= 0 && bd < step * step * 1.2f) {
                                    val r = best / 8; val c = best % 8
                                    pr = if (flip) (7 - r) * 8 + (7 - c) else best
                                }
                            }
                        }
                    },
            ) {
                @Suppress("UNUSED_VARIABLE") val dep = tick
                val w = size.width; val m = w * .045f; val cell = (w - 2 * m) / 11.08f; val gw = cell * .26f; val step = cell + gw
                fun dr(r: Int) = if (flip) 8 - r else r
                fun dc(c: Int) = if (flip) 8 - c else c
                fun cellTL(r: Int, c: Int) = Offset(m + dc(c) * step, m + dr(r) * step)
                fun ctr(sq: Int): Offset { val t = cellTL(sq / 9, sq % 9); return Offset(t.x + cell / 2f, t.y + cell / 2f) }
                // Bàn theo mẫu bộ cờ in 3D: đế xanh nhạt làm rãnh, viền và ô màu đồng, tường bạc dựng đứng
                drawRoundRect(Brush.linearGradient(listOf(Color(0xFFC9EAF3), Color(0xFF9CCFDF)), Offset.Zero, Offset(w, w)), cornerRadius = CornerRadius(w * .05f))
                drawRoundRect(Brush.linearGradient(listOf(Color(0xFFC27B50), Color(0xFF94522F)), Offset.Zero, Offset(w, w)), Offset(m * .12f, m * .12f), Size(w - m * .24f, w - m * .24f), CornerRadius(w * .05f), style = Stroke(m * .5f))
                // ô đích của từng bên
                val goalTint = Array(81) { -1 }
                for (p in 0 until g.players) {
                    val gs = QuoridorGame.goalSide(g.players, p)
                    for (i in 0..8) {
                        val sq = when (gs) { 0 -> i; 1 -> i * 9 + 8; 2 -> 72 + i; else -> i * 9 }
                        goalTint[sq] = p
                    }
                }
                for (sq in 0 until 81) drawCopperTile(cellTL(sq / 9, sq % 9), cell, if (goalTint[sq] >= 0) playerColor(goalTint[sq]) else null, sq)
                // nước đi gần nhất
                if (last >= 0 && !QuoridorGame.isWall(last)) drawCircle(Color.White.copy(alpha = .85f), cell * .46f, ctr(last), style = Stroke(cell * .055f))
                // gợi ý ô đến
                for (d in moves) {
                    drawCircle(Color.Black.copy(alpha = .18f), cell * .15f, ctr(d) + Offset(0f, cell * .02f))
                    drawCircle(Color(0xFFF6FBFF), cell * .13f, ctr(d))
                }
                // tường: dải màu của người đặt nằm trong rãnh + tấm bạc dựng đứng
                fun wallRect(h: Boolean, r: Int, c: Int): Pair<Offset, Size> {
                    val rr = if (flip) 7 - r else r; val cc = if (flip) 7 - c else c
                    val x = m + cc * step; val y = m + rr * step
                    return if (h) Offset(x, y + cell) to Size(cell * 2f + gw, gw) else Offset(x + cell, y) to Size(gw, cell * 2f + gw)
                }
                fun drawWall(h: Boolean, r: Int, c: Int, owner: Color?, preview: Color? = null) {
                    val (o, sz) = wallRect(h, r, c)
                    if (preview != null) { drawRoundRect(preview, o, sz, CornerRadius(gw / 2f)); return }
                    if (owner != null) drawRoundRect(owner, o, sz, CornerRadius(gw / 2f))
                    // tấm tường bạc nhô lên (dịch nhẹ lên trên để có cảm giác đứng)
                    val lift = gw * .55f
                    val so = Offset(o.x - (if (h) 0f else gw * .12f), o.y - lift - (if (h) gw * .1f else 0f))
                    val ss = if (h) Size(sz.width, gw * 1.15f) else Size(gw * 1.24f, sz.height)
                    drawRoundRect(Color.Black.copy(alpha = .30f), so + Offset(gw * .25f, lift + gw * .35f), ss, CornerRadius(gw * .3f))
                    drawRoundRect(
                        if (h) Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD6DAE2), Color(0xFFA3A9B5)), so.y, so.y + ss.height)
                        else Brush.horizontalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFD6DAE2), Color(0xFFA3A9B5)), so.x, so.x + ss.width),
                        so, ss, CornerRadius(gw * .3f),
                    )
                    drawRoundRect(Color.White.copy(alpha = .7f), so, ss, CornerRadius(gw * .3f), style = Stroke(gw * .08f))
                }
                for (r in 0..7) for (c in 0..7) {
                    if (g.hasWallH(r, c)) drawWall(true, r, c, wallOwners[QuoridorGame.mvWall(true, r, c)]?.let { playerColor(it) })
                    if (g.hasWallV(r, c)) drawWall(false, r, c, wallOwners[QuoridorGame.mvWall(false, r, c)]?.let { playerColor(it) })
                }
                // xem trước tường
                if (pr >= 0 && mode != QMode.MOVE) drawWall(previewH, pr / 8, pr % 8, null, (if (previewOk) Color(0xFF16B37A) else Color(0xFFE5483D)).copy(alpha = .9f))
                // quân: xếp theo hàng để quân phía dưới vẽ đè quân phía trên
                val order = (0 until g.players).sortedBy { ctr(g.pawnAt(it)).y }
                for (p in order) { val o = ctr(g.pawnAt(p)); drawQuoridorPawn(Offset(o.x, o.y + cell * .30f), cell * .30f, playerColor(p)) }
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val labels = listOf(stringResource(R.string.qd_mode_move), stringResource(R.string.qd_mode_wall_h), stringResource(R.string.qd_mode_wall_v))
                OptionChips(labels, mode.ordinal, { mode = QMode.values()[it]; pr = -1 })
                if (mode != QMode.MOVE) GoldButton(
                    stringResource(R.string.qd_place_wall),
                    { if (previewOk) vm.playMove(QuoridorGame.mvWall(previewH, pr / 8, pr % 8)) },
                    Modifier.fillMaxWidth().height(48.dp), enabled = previewOk,
                )
            }
        }
    }
}

// ═════════════════════════════════ CỜ VÂY ═════════════════════════════════

private fun hoshi(n: Int): List<Int> = when (n) {
    9 -> listOf(2 * 9 + 2, 2 * 9 + 6, 6 * 9 + 2, 6 * 9 + 6, 4 * 9 + 4)
    13 -> listOf(3, 6, 9).let { l -> l.flatMap { y -> l.map { x -> y * 13 + x } } }
    else -> listOf(3, 9, 15).let { l -> l.flatMap { y -> l.map { x -> y * 19 + x } } }
}

/** Bàn cờ vây 19x19: chạm một giao điểm để đặt thử (quân mờ), bấm "Đặt quân" để xác nhận. */
@Composable
fun GoBoardView(vm: SessionViewModel, modifier: Modifier = Modifier) {
    val g = vm.game as? GoGame ?: return
    val n = g.size
    val tick = vm.tick
    var preview by remember { mutableIntStateOf(-1) }
    var confirmPass by remember { mutableStateOf(false) }
    LaunchedEffect(tick) { preview = -1 }
    val act = vm.canAct
    val ok = preview >= 0 && act && g.isLegal(preview)
    val last = g.lastMove
    val seatNow = g.turn

    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val side = if (maxWidth < maxHeight - 112.dp) maxWidth else maxHeight - 112.dp
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Canvas(
                Modifier.size(side).clip(RoundedCornerShape(14.dp))
                    .pointerInput(tick, act, preview) {
                        detectTapGestures { off ->
                            if (!act) return@detectTapGestures
                            val w = size.width.toFloat(); val st = w / (n - 1 + 1.4f); val m = st * .7f
                            val x = ((off.x - m) / st).roundToInt().coerceIn(0, n - 1); val y = ((off.y - m) / st).roundToInt().coerceIn(0, n - 1)
                            val cell = y * n + x
                            preview = if (g.stoneAt(cell) == 0) cell else -1
                        }
                    },
            ) {
                @Suppress("UNUSED_VARIABLE") val dep = tick
                val w = size.width; val st = w / (n - 1 + 1.4f); val m = st * .7f
                fun px(cell: Int) = Offset(m + (cell % n) * st, m + (cell / n) * st)
                drawRect(Brush.linearGradient(listOf(Color(0xFFF0CE7A), Color(0xFFD9A94E), Color(0xFFC9923A)), Offset.Zero, Offset(w, w)))
                val line = Color(0xFF5A4020).copy(alpha = .85f)
                for (i in 0 until n) {
                    drawLine(line, Offset(m, m + i * st), Offset(m + (n - 1) * st, m + i * st), max(1f, st * .035f))
                    drawLine(line, Offset(m + i * st, m), Offset(m + i * st, m + (n - 1) * st), max(1f, st * .035f))
                }
                for (h in hoshi(n)) drawCircle(line, st * .11f, px(h))
                for (c in 0 until n * n) {
                    val s = g.stoneAt(c); if (s == 0) continue
                    drawStone(px(c), st * .47f, s == 1)
                }
                if (last >= 0 && last != GoGame.PASS) {
                    val st0 = g.stoneAt(last)
                    drawCircle(if (st0 == 1) Color.White.copy(alpha = .9f) else Color(0xFF222222), st * .2f, px(last), style = Stroke(st * .07f))
                }
                if (preview >= 0) {
                    val c = px(preview)
                    val col = if (ok) (if (seatNow == 0) Color.Black else Color.White) else Color(0xFFFF6B6B)
                    drawCircle(col.copy(alpha = .55f), st * .47f, c)
                    drawCircle(if (ok) Lux.goldHi else Color(0xFFFF6B6B), st * .5f, c, style = Stroke(st * .06f))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Không còn nút "Bỏ lượt" thường trực. Chỉ khi đối thủ vừa bỏ lượt mới hiện "Kết thúc ván" để hai bên tính điểm.
                if (g.lastWasPass && act) OutlineButton(stringResource(R.string.go_end_game), { confirmPass = true }, Modifier.weight(1f), Ic.Flag)
                GoldButton(stringResource(R.string.go_place), { if (ok) vm.playMove(preview) }, Modifier.weight(1f), enabled = ok)
            }
            if (g.lastWasPass && act) Text(stringResource(R.string.go_opp_passed), style = MaterialTheme.typography.labelMedium, color = pm.gold, textAlign = TextAlign.Center)
        }
    }
    if (confirmPass) PmDialog(
        stringResource(R.string.go_end_title), stringResource(R.string.go_end_text),
        stringResource(R.string.go_end_game), { confirmPass = false; vm.playMove(GoGame.PASS) }, stringResource(R.string.cancel), { confirmPass = false },
        tone = DialogTone.WARN,
    )
}
