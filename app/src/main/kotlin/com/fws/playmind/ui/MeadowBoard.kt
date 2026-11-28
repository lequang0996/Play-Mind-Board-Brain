package com.fws.playmind.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fws.playmind.R
import com.fws.playmind.core.MeadowGame
import com.fws.playmind.core.MeadowPhase
import com.fws.playmind.match.SessionViewModel

private val GrassBg = Color(0xFF6E4A35)
private val GridLine = Color(0xFF8C6650)
private val SoilHi = Color(0xFF8A6146)
private val SoilLo = Color(0xFF5E3D29)
private val GrassMid = Color(0xFF4FAE4A)
private val GrassDark = Color(0xFF2E7D32)
private val GrassLight = Color(0xFF9BE07A)
private val CouponBlue = Color(0xFF2F8FE0)
private val StealPurple = Color(0xFF6B3FA0)
private val StoneGray = Color(0xFF8E939C)
private val PieceGold = Color(0xFFE8B23A)

/** Màu lãnh thổ: cỏ xanh nhuộm màu ghế (vẫn ra "đồng cỏ" nhưng phân biệt được 8 người). */
private fun turfColor(seat: Int): Color = lerp(GrassMid, playerColor(seat), .5f)

/** Một ô cỏ: nền chuyển sắc, viền sáng trên trái, vài ngọn cỏ (vị trí theo [seed]). */
private fun DrawScope.drawGrassCell(tl: Offset, cs: Float, base: Color, seed: Int, rounded: Boolean = false) {
    val hi = lerp(base, GrassLight, .35f); val lo = lerp(base, GrassDark, .45f)
    val br = Brush.linearGradient(listOf(hi, base, lo), tl, tl + Offset(cs, cs))
    if (rounded) drawRoundRect(br, tl + Offset(cs * .04f, cs * .04f), Size(cs * .92f, cs * .92f), CornerRadius(cs * .2f))
    else drawRect(br, tl, Size(cs, cs))
    if (cs < 7f) return
    var h = seed * 1103515245 + 12345
    for (k in 0 until 3) {
        h = h * 1103515245 + 12345
        val fx = .2f + ((h ushr 8) and 255) / 255f * .6f
        val fy = .45f + ((h ushr 16) and 255) / 255f * .4f
        val x = tl.x + fx * cs; val y = tl.y + fy * cs
        val lean = if (k % 2 == 0) cs * .08f else -cs * .08f
        drawLine(lerp(base, GrassDark, .55f).copy(alpha = .75f), Offset(x, y), Offset(x + lean, y - cs * .28f), cs * .07f, StrokeCap.Round)
        drawLine(lerp(base, GrassLight, .6f).copy(alpha = .55f), Offset(x + cs * .06f, y), Offset(x + cs * .06f + lean * .6f, y - cs * .2f), cs * .05f, StrokeCap.Round)
    }
}

/** Huy hiệu ô đặc biệt: thẻ trắng bo góc có bóng, biểu tượng ở giữa. */
private fun DrawScope.specialBadge(tl: Offset, cs: Float, draw: DrawScope.(Offset, Float) -> Unit) {
    val m = cs * .06f
    drawRoundRect(Color.Black.copy(alpha = .28f), tl + Offset(m + cs * .04f, m + cs * .07f), Size(cs - 2 * m, cs - 2 * m), CornerRadius(cs * .22f))
    drawRoundRect(Brush.verticalGradient(listOf(Color.White, Color(0xFFE9EEF2)), tl.y, tl.y + cs), tl + Offset(m, m), Size(cs - 2 * m, cs - 2 * m), CornerRadius(cs * .22f))
    drawRoundRect(Color(0xFF9AA4AE), tl + Offset(m, m), Size(cs - 2 * m, cs - 2 * m), CornerRadius(cs * .22f), style = Stroke(cs * .04f))
    draw(tl + Offset(cs / 2, cs / 2), cs * .36f)
}

/** Vẽ cả đồng cỏ: đất cày, lưới, lãnh thổ (cỏ nhuộm màu ghế, viền màu ghế ở biên lãnh thổ), ô đặc biệt, đá, bóng mảnh. */
private fun DrawScope.drawField(g: MeadowGame, tm: TextMeasurer, stealPick: Int, ghost: IntArray?, ghostOk: Boolean, pickCell: Int) {
    val N = MeadowGame.N
    val cs = size.width / N
    // đất cày: chuyển sắc + ô chẵn lẻ đậm nhạt rất nhẹ
    drawRect(Brush.linearGradient(listOf(SoilHi, SoilLo), Offset.Zero, Offset(size.width, size.height)))
    for (y in 0 until N) for (x in 0 until N) if ((x + y) % 2 == 0) drawRect(Color.White.copy(alpha = .035f), Offset(x * cs, y * cs), Size(cs, cs))
    for (i in 0..N) {
        val strong = i % 10 == 0
        val c = if (strong) Color(0xFFE8C9A8).copy(alpha = .55f) else Color.Black.copy(alpha = .14f)
        val w = if (strong) cs * .09f else 1f
        drawLine(c, Offset(i * cs, 0f), Offset(i * cs, size.height), w)
        drawLine(c, Offset(0f, i * cs), Offset(size.width, i * cs), w)
    }
    val bases = HashSet<Int>()
    for (m in g.history) if (m >= MeadowGame.BASE) bases.add(m - MeadowGame.BASE)
    // lãnh thổ
    for (c in 0 until MeadowGame.CELLS) {
        val o = g.owner[c]; if (o < 0) continue
        val tl = Offset((c % N) * cs, (c / N) * cs)
        drawGrassCell(tl, cs, turfColor(o), c)
        if (MeadowGame.SPECIAL_AT[c] != MeadowGame.SP_NONE) {
            // ô đặc biệt đã chiếm: biểu tượng mờ trên cỏ
            val ctr = tl + Offset(cs / 2, cs / 2)
            when (MeadowGame.SPECIAL_AT[c]) {
                MeadowGame.SP_COUPON -> drawCouponIcon(ctr, cs * .3f)
                MeadowGame.SP_PIECE -> drawPieceIcon(ctr, cs * .34f, null)
                MeadowGame.SP_STONE -> drawStoneIcon(ctr, cs * .3f, true)
                MeadowGame.SP_STEAL -> drawStealIcon(ctr, cs * .3f)
            }
        }
        val t = g.tileOf[c]
        if (t >= 0 && t == stealPick) drawRect(Color.White.copy(alpha = .5f), tl, Size(cs, cs))
    }
    // đường nối giữa các mảnh cùng chủ (mảnh) và viền biên lãnh thổ (màu ghế, đậm)
    for (c in 0 until MeadowGame.CELLS) {
        val o = g.owner[c]; if (o < 0) continue
        val x = c % N; val y = c / N; val tl = Offset(x * cs, y * cs)
        val t = g.tileOf[c]
        val rim = lerp(playerColor(o), Color.Black, .3f)
        fun edge(nx: Int, ny: Int, a: Offset, b: Offset) {
            val inside = nx in 0 until N && ny in 0 until N
            val nc = if (inside) ny * N + nx else -1
            if (!inside || g.owner[nc] != o) drawLine(rim, a, b, cs * .16f, StrokeCap.Square)
            else if (g.tileOf[nc] != t) drawLine(Color.Black.copy(alpha = .22f), a, b, 1.2f)
        }
        edge(x + 1, y, tl + Offset(cs, 0f), tl + Offset(cs, cs))
        edge(x, y + 1, tl + Offset(0f, cs), tl + Offset(cs, cs))
        edge(x - 1, y, tl, tl + Offset(0f, cs))
        edge(x, y - 1, tl, tl + Offset(cs, 0f))
    }
    // ô nền: cờ nhỏ màu ghế
    for (b in bases) {
        val o = g.owner[b]; if (o < 0) continue
        val tl = Offset((b % N) * cs, (b / N) * cs)
        drawLine(Color(0xFF3B2A1A), tl + Offset(cs * .35f, cs * .85f), tl + Offset(cs * .35f, cs * .15f), cs * .09f, StrokeCap.Round)
        val flag = Path().apply { moveTo(tl.x + cs * .38f, tl.y + cs * .15f); lineTo(tl.x + cs * .82f, tl.y + cs * .3f); lineTo(tl.x + cs * .38f, tl.y + cs * .48f); close() }
        drawPath(flag, playerColor(o)); drawPath(flag, Color.White, style = Stroke(cs * .05f))
    }
    // ô đặc biệt chưa chiếm, đá
    for (c in 0 until MeadowGame.CELLS) {
        val tl = Offset((c % N) * cs, (c / N) * cs)
        when (g.special[c]) {
            MeadowGame.SP_COUPON -> specialBadge(tl, cs) { ctr, r -> drawCouponIcon(ctr, r) }
            MeadowGame.SP_PIECE -> specialBadge(tl, cs) { ctr, r -> drawPieceIcon(ctr, r * 1.1f, tm) }
            MeadowGame.SP_STONE -> specialBadge(tl, cs) { ctr, r -> drawStoneIcon(ctr, r, true) }
            MeadowGame.SP_STEAL -> specialBadge(tl, cs) { ctr, r -> drawStealIcon(ctr, r) }
        }
        if (g.stone[c]) {
            val ctr = tl + Offset(cs / 2, cs / 2)
            drawOval(Color.Black.copy(alpha = .3f), Offset(ctr.x - cs * .42f, ctr.y + cs * .12f), Size(cs * .84f, cs * .3f))
            drawStoneIcon(ctr, cs * .5f, true)
        }
    }
    // mảnh mới đặt gần nhất: viền sáng
    if (MeadowGame.isPlace(g.lastMove)) {
        val last = g.insts.lastOrNull()
        if (last != null && last.alive) {
            val set = last.cells.toHashSet()
            for (c in last.cells) {
                val x = c % N; val y = c / N; val tl = Offset(x * cs, y * cs)
                val w = cs * .1f; val col = Color.White.copy(alpha = .9f)
                if (!set.contains(c + 1) || x == N - 1) drawLine(col, tl + Offset(cs, 0f), tl + Offset(cs, cs), w)
                if (!set.contains(c - 1) || x == 0) drawLine(col, tl, tl + Offset(0f, cs), w)
                if (!set.contains(c + N)) drawLine(col, tl + Offset(0f, cs), tl + Offset(cs, cs), w)
                if (!set.contains(c - N)) drawLine(col, tl, tl + Offset(cs, 0f), w)
            }
        }
    }
    // bóng mảnh
    if (ghost != null) {
        val set = ghost.toHashSet()
        val fill = if (ghostOk) Color(0xFFB9F5A8) else Color(0xFFFF8A80)
        val line = if (ghostOk) Color(0xFF1B7F2A) else Color(0xFFB3261E)
        for (c in ghost) {
            val x = c % N; val y = c / N; val tl = Offset(x * cs, y * cs)
            drawRoundRect(fill.copy(alpha = .78f), tl + Offset(cs * .06f, cs * .06f), Size(cs * .88f, cs * .88f), CornerRadius(cs * .18f))
            val w = cs * .12f
            if (!set.contains(c + 1) || x == N - 1) drawLine(line, tl + Offset(cs, 0f), tl + Offset(cs, cs), w, StrokeCap.Round)
            if (!set.contains(c - 1) || x == 0) drawLine(line, tl, tl + Offset(0f, cs), w, StrokeCap.Round)
            if (!set.contains(c + N)) drawLine(line, tl + Offset(0f, cs), tl + Offset(cs, cs), w, StrokeCap.Round)
            if (!set.contains(c - N)) drawLine(line, tl, tl + Offset(cs, 0f), w, StrokeCap.Round)
        }
    } else if (pickCell >= 0) {
        val tl = Offset((pickCell % N) * cs, (pickCell / N) * cs)
        drawRoundRect(Color.White.copy(alpha = .75f), tl + Offset(cs * .06f, cs * .06f), Size(cs * .88f, cs * .88f), CornerRadius(cs * .2f))
        drawRoundRect(Color(0xFF1B7F2A), tl + Offset(cs * .06f, cs * .06f), Size(cs * .88f, cs * .88f), CornerRadius(cs * .2f), style = Stroke(cs * .12f))
    }
}

/** Biểu tượng Piece (đồng FWs): hình thoi vàng có chữ FW. */
fun DrawScope.drawPieceIcon(c: Offset, r: Float, tm: TextMeasurer?) {
    val p = Path().apply { moveTo(c.x, c.y - r); lineTo(c.x + r, c.y); lineTo(c.x, c.y + r); lineTo(c.x - r, c.y); close() }
    drawPath(p, Brush.linearGradient(listOf(Color(0xFFFFE7A0), PieceGold, Color(0xFFB07A10)), Offset(c.x - r, c.y - r), Offset(c.x + r, c.y + r)))
    drawPath(p, Color(0xFF8A5A00), style = Stroke(r * .12f))
    if (tm != null && r > 7f) {
        val l = tm.measure("FW", TextStyle(fontSize = (r * .62f).toSp(), fontWeight = FontWeight.ExtraBold, color = Color(0xFF5A3A00)))
        drawText(l, topLeft = Offset(c.x - l.size.width / 2f, c.y - l.size.height / 2f))
    }
}

/** Biểu tượng phiếu đổi gạch: hai mũi tên vòng xanh nối đuôi nhau. */
fun DrawScope.drawCouponIcon(c: Offset, r: Float) {
    val rr = r * .62f; val w = r * .26f
    for (start in floatArrayOf(200f, 20f)) {
        drawArc(CouponBlue, start, 125f, false, Offset(c.x - rr, c.y - rr), Size(rr * 2, rr * 2), style = Stroke(w, cap = StrokeCap.Round))
        val a = Math.toRadians((start + 125f).toDouble())
        val px = c.x + rr * kotlin.math.cos(a).toFloat(); val py = c.y + rr * kotlin.math.sin(a).toFloat()
        val tx = -kotlin.math.sin(a).toFloat(); val ty = kotlin.math.cos(a).toFloat()
        val nx = kotlin.math.cos(a).toFloat(); val ny = kotlin.math.sin(a).toFloat()
        val h = r * .42f
        val head = Path().apply {
            moveTo(px + tx * h * .9f, py + ty * h * .9f)
            lineTo(px + nx * h * .6f - tx * h * .2f, py + ny * h * .6f - ty * h * .2f)
            lineTo(px - nx * h * .6f - tx * h * .2f, py - ny * h * .6f - ty * h * .2f); close()
        }
        drawPath(head, CouponBlue)
    }
}

/** Biểu tượng cướp mảnh: dao găm tím, lưỡi răng cưa, chuôi sẫm (nghiêng 45°). */
fun DrawScope.drawStealIcon(c: Offset, r: Float) {
    rotate(45f, c) {
        val blade = Path().apply {
            moveTo(c.x - r * .2f, c.y + r * .15f); lineTo(c.x - r * .2f, c.y - r * .55f)
            lineTo(c.x + r * .02f, c.y - r * .95f); lineTo(c.x + r * .24f, c.y - r * .5f)
            lineTo(c.x + r * .14f, c.y - r * .4f); lineTo(c.x + r * .24f, c.y - r * .3f)
            lineTo(c.x + r * .14f, c.y - r * .2f); lineTo(c.x + r * .24f, c.y - r * .1f); lineTo(c.x + r * .24f, c.y + r * .15f); close()
        }
        drawPath(blade, Brush.horizontalGradient(listOf(Color(0xFF9A6FD0), StealPurple), c.x - r * .2f, c.x + r * .24f))
        drawLine(Color.White.copy(alpha = .6f), Offset(c.x - r * .1f, c.y - r * .55f), Offset(c.x - r * .1f, c.y + r * .05f), r * .06f, StrokeCap.Round)
        drawRoundRect(Color(0xFF3B2160), Offset(c.x - r * .36f, c.y + r * .14f), Size(r * .76f, r * .14f), CornerRadius(r * .07f))
        drawRoundRect(Color(0xFF2A1745), Offset(c.x - r * .13f, c.y + r * .28f), Size(r * .3f, r * .62f), CornerRadius(r * .1f))
    }
}

/** Biểu tượng đá: khối đa giác xám có mặt sáng tối. */
fun DrawScope.drawStoneIcon(c: Offset, r: Float, solid: Boolean) {
    if (!solid) drawCircle(Color.White, r, c)
    val k = if (solid) 1f else .78f
    val pts = listOf(-.9f to -.1f, -.45f to -.7f, .4f to -.75f, .9f to -.15f, .55f to .65f, -.5f to .7f)
    val p = Path().apply { pts.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(c.x + x * r * k, c.y + y * r * k) else lineTo(c.x + x * r * k, c.y + y * r * k) }; close() }
    drawPath(p, Brush.linearGradient(listOf(Color(0xFFC9CDD4), StoneGray, Color(0xFF5B6068)), Offset(c.x - r, c.y - r), Offset(c.x + r, c.y + r)))
    drawPath(p, Color(0xFF4A4E55), style = Stroke(r * .08f))
}

/** Tranh minh hoạ: góc đồng cỏ với ba lãnh thổ và một ô Phiếu. */
fun DrawScope.drawMeadowArt(ox: Float, oy: Float, s: Float) {
    val n = 6; val cs = s * .84f / n; val x0 = ox + s * .08f; val y0 = oy + s * .08f
    drawRoundRect(Color(0xFF8C5D37), Offset(x0 - cs * .22f, y0 - cs * .22f), Size(cs * n + cs * .44f, cs * n + cs * .44f), CornerRadius(cs * .5f))
    drawRoundRect(Brush.linearGradient(listOf(SoilHi, SoilLo), Offset(x0, y0), Offset(x0 + cs * n, y0 + cs * n)), Offset(x0, y0), Size(cs * n, cs * n), CornerRadius(cs * .3f))
    val map = arrayOf("000.11", "000.11", "00..1.", "..2...", ".222.3", ".22.33")
    for (r in 0 until n) for (c in 0 until n) {
        val ch = map[r][c]
        if (ch != '.') drawGrassCell(Offset(x0 + c * cs, y0 + r * cs), cs, turfColor(ch - '0'), r * 7 + c, rounded = true)
    }
    specialBadge(Offset(x0 + 3 * cs, y0 + 1 * cs), cs) { ctr, rad -> drawCouponIcon(ctr, rad) }
}

/**
 * Bàn Lãnh địa đồng cỏ: lưới 30×30, hàng chờ mảnh công khai, bảng điều khiển của lượt mình (xoay, lật, bỏ mảnh, đặt,
 * dùng phiếu chọn 1 trong 5 mảnh tới, mua phiếu, dọn đá) và các bước phụ (đặt ô nền, đặt đá, cướp mảnh).
 * Đặt mảnh: chạm hoặc kéo trên bàn để dời bóng mảnh (xanh = hợp lệ, đỏ = sai luật), rồi bấm Đặt.
 */
@Composable
fun MeadowBoardView(vm: SessionViewModel, modifier: Modifier = Modifier) {
    val g = vm.game as? MeadowGame ?: return
    val tick = vm.tick
    val act = vm.canAct
    val me = vm.actingSide
    val tm = rememberTextMeasurer()
    var orient by remember { mutableIntStateOf(0) }
    var anchor by remember { mutableIntStateOf(-1) }     // ô neo của bóng mảnh / ô được chọn
    var clearMode by remember { mutableStateOf(false) }
    var askCoupon by remember { mutableIntStateOf(0) }
    var stealPick by remember { mutableIntStateOf(-1) }
    LaunchedEffect(tick) { orient = 0; anchor = -1; clearMode = false; stealPick = -1 }
    val shape = if (act) g.currentShape else -1
    val phase = g.phase
    val ghost: IntArray? = if (shape >= 0 && anchor >= 0) g.cellsOf(shape, orient.coerceIn(0, MeadowGame.ORIENTS[shape].size - 1), anchor) else null
    val ghostOk = ghost != null && g.canPlace(me, ghost)

    fun cellAt(off: Offset, w: Float): Int {
        val cs = w / MeadowGame.N
        val x = (off.x / cs).toInt(); val y = (off.y / cs).toInt()
        return if (x in 0 until MeadowGame.N && y in 0 until MeadowGame.N) y * MeadowGame.N + x else -1
    }
    /** Đặt bóng mảnh sao cho tâm khung nằm ở ô [c]. */
    fun anchorFor(c: Int): Int {
        if (shape < 0) return c
        val (w, h) = MeadowGame.extent(shape, orient)
        val x = (c % MeadowGame.N - (w - 1) / 2).coerceIn(0, MeadowGame.N - w)
        val y = (c / MeadowGame.N - (h - 1) / 2).coerceIn(0, MeadowGame.N - h)
        return y * MeadowGame.N + x
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val controlsH = 214.dp
        val side = minOf(maxWidth - 8.dp, maxHeight - controlsH).coerceAtLeast(200.dp)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val frame = RoundedCornerShape(16.dp)
            Box(
                Modifier.size(side).lift(frame, 10.dp, Color(0xFF3B2414)).clip(frame)
                    .background(Brush.linearGradient(listOf(Color(0xFFA9774C), Color(0xFF6E4527), Color(0xFF8C5D37))))
                    .padding(5.dp),
            ) {
            Canvas(
                Modifier.fillMaxSize().clip(RoundedCornerShape(11.dp))
                    .pointerInput(tick, act, orient, shape, phase, clearMode) {
                        detectTapGestures { off ->
                            if (!act) return@detectTapGestures
                            val c = cellAt(off, size.width.toFloat()); if (c < 0) return@detectTapGestures
                            when {
                                clearMode -> if (g.stone[c]) vm.playMove(MeadowGame.REMOVE_STONE + c)
                                phase == MeadowPhase.STEAL_PICK -> { val t = g.tileOf[c]; stealPick = if (t >= 0 && g.insts[t].alive && g.insts[t].owner != me) t else -1 }
                                shape >= 0 -> anchor = anchorFor(c)
                                else -> anchor = c
                            }
                        }
                    }
                    .pointerInput(tick, act, orient, shape) {
                        if (!act || shape < 0) return@pointerInput
                        detectDragGestures { ch, _ ->
                            val c = cellAt(ch.position, size.width.toFloat())
                            if (c >= 0) { ch.consume(); anchor = anchorFor(c) }
                        }
                    },
            ) {
                @Suppress("UNUSED_VARIABLE") val dep = tick
                drawField(g, tm, stealPick, ghost, ghostOk, if (shape < 0) anchor else -1)
            }
            }
            // hàng chờ công khai
            QueueStrip(g, act && phase == MeadowPhase.TURN && !g.couponUsed && g.coupons[me] > 0, vm.tick) { askCoupon = it }
            // bảng điều khiển
            val panel = RoundedCornerShape(20.dp)
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp).lift(panel, 8.dp).clip(panel).background(cardBrush()).border(1.dp, hairline(), panel)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MdChip { Text(stringResource(R.string.md_round, g.round.coerceIn(1, MeadowGame.ROUNDS), MeadowGame.ROUNDS), style = MaterialTheme.typography.labelLarge, color = pm.ink) }
                    MdChip { Canvas(Modifier.size(16.dp)) { drawCouponIcon(Offset(size.width / 2, size.height / 2), size.width / 2) }; Text("${g.coupons[me]}", style = MaterialTheme.typography.labelLarge, color = pm.ink) }
                    MdChip { Canvas(Modifier.size(16.dp)) { drawPieceIcon(Offset(size.width / 2, size.height / 2), size.width / 2, null) }; Text("${g.pieces[me]}", style = MaterialTheme.typography.labelLarge, color = pm.ink) }
                    if (act && phase == MeadowPhase.TURN && g.pieces[me] > 0) {
                        MdAction(stringResource(R.string.md_buy_coupon)) { vm.playMove(MeadowGame.BUY_COUPON) }
                        if (g.stone.any { it }) MdAction(stringResource(if (clearMode) R.string.cancel else R.string.md_clear_stone), active = clearMode) { clearMode = !clearMode }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(if (act) Color(0xFF3FA34D) else pm.outline))
                    Text(promptText(g, act, clearMode), style = MaterialTheme.typography.labelMedium, color = if (act) pm.ink else pm.inkSoft)
                }
                if (act) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (phase) {
                        MeadowPhase.BASE -> GoldButton(stringResource(R.string.md_place_base), { if (anchor >= 0) vm.playMove(MeadowGame.BASE + anchor) }, Modifier.weight(1f), enabled = anchor >= 0 && g.canPlaceBase(anchor))
                        MeadowPhase.STONE -> GoldButton(stringResource(R.string.md_place_stone), { if (anchor >= 0) vm.playMove(MeadowGame.STONE + anchor) }, Modifier.weight(1f), enabled = anchor >= 0 && g.isFree(anchor))
                        MeadowPhase.STEAL_PICK -> {
                            OutlineButton(stringResource(R.string.md_skip), { vm.playMove(MeadowGame.STEAL_SKIP) }, Modifier.weight(1f))
                            GoldButton(stringResource(R.string.md_steal_this), { if (stealPick >= 0) vm.playMove(MeadowGame.STEAL + stealPick) }, Modifier.weight(1f), enabled = stealPick >= 0)
                        }
                        MeadowPhase.TURN, MeadowPhase.STEAL_PLACE -> {
                            ToolButton(Ic.Refresh, stringResource(R.string.md_rotate), { if (shape >= 0) { orient = MeadowGame.rotateOrient(shape, orient); if (anchor >= 0) anchor = anchorFor(anchor) } }, Modifier.weight(1f), enabled = shape >= 0)
                            ToolButton(Ic.Sliders, stringResource(R.string.md_flip), { if (shape >= 0) orient = MeadowGame.flipOrient(shape, orient) }, Modifier.weight(1f), enabled = shape >= 0)
                            ToolButton(Ic.Close, stringResource(R.string.md_discard), { vm.playMove(MeadowGame.DISCARD) }, Modifier.weight(1f))
                            GoldButton(stringResource(R.string.md_place), { if (ghostOk && ghost != null) vm.playMove(MeadowGame.place(orient, anchor)) }, Modifier.weight(1.1f), enabled = ghostOk)
                        }
                        else -> {}
                    }
                }
            }
        }
    }
    if (askCoupon > 0) SimpleDialog(
        stringResource(R.string.md_use_coupon), stringResource(R.string.md_use_coupon_text, askCoupon + 1),
        stringResource(R.string.ok), { vm.playMove(MeadowGame.USE_COUPON + askCoupon); askCoupon = 0 },
        stringResource(R.string.cancel), { askCoupon = 0 },
    )
}

@Composable
private fun promptText(g: MeadowGame, act: Boolean, clearMode: Boolean): String = when {
    !act -> stringResource(R.string.md_watch)
    clearMode -> stringResource(R.string.md_prompt_clear)
    g.phase == MeadowPhase.BASE -> stringResource(R.string.md_prompt_base)
    g.phase == MeadowPhase.STONE -> stringResource(R.string.md_prompt_stone)
    g.phase == MeadowPhase.STEAL_PICK -> stringResource(R.string.md_prompt_steal)
    g.phase == MeadowPhase.STEAL_PLACE -> stringResource(R.string.md_prompt_steal_place)
    g.round == 1 -> stringResource(R.string.md_prompt_first)
    else -> stringResource(R.string.md_prompt_place)
}

/** Hàng chờ: 5 mảnh tới (mảnh đang lấy viền vàng; chạm mảnh 2-5 để dùng phiếu) và vài mảnh sau đó nhỏ, mờ hơn. */
@Composable
private fun QueueStrip(g: MeadowGame, canSwap: Boolean, tick: Int, onSwap: (Int) -> Unit) {
    @Suppress("UNUSED_EXPRESSION") tick   // đối tượng game không đổi: phải đọc tick để hàng chờ vẽ lại sau mỗi nước
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        val upto = minOf(g.queue.size, 12)
        for (k in 0 until upto) {
            val sel = k == g.pickOff && g.phase == MeadowPhase.TURN
            val inChoice = k < MeadowGame.CHOICE
            val swappable = canSwap && k in 1 until MeadowGame.CHOICE
            val shape = RoundedCornerShape(12.dp)
            Box(
                Modifier.size(if (inChoice) 50.dp else 36.dp)
                    .lift(shape, if (sel) 8.dp else 3.dp, if (sel) pm.goldBright else Color(0xFF1B2350)).clip(shape)
                    .background(if (sel) Brush.verticalGradient(listOf(Color(0xFFFFF4D6), Color(0xFFF7E2A8))) else Brush.verticalGradient(listOf(Color(0xFFF6EFE3), Color(0xFFE9DCC6))))
                    .border(if (sel) 2.dp else 1.dp, if (sel) pm.goldBright else if (swappable) Color(0xFF2F8FE0) else Color(0xFFD8C9AE), shape)
                    .then(if (swappable) Modifier.clickable { onSwap(k) } else Modifier)
                    .then(if (inChoice) Modifier else Modifier.alpha(.7f)),
                contentAlignment = Alignment.Center,
            ) {
                val s = g.queue[k]
                Canvas(Modifier.size(if (inChoice) 40.dp else 28.dp)) {
                    val o = MeadowGame.ORIENTS[s][0]
                    val (w, h) = MeadowGame.extent(s, 0)
                    val cs = minOf(size.width / maxOf(w, 3), size.height / maxOf(h, 3))
                    val x0 = (size.width - w * cs) / 2; val y0 = (size.height - h * cs) / 2
                    for (i in 0 until o.size / 2) drawGrassCell(Offset(x0 + o[2 * i] * cs, y0 + o[2 * i + 1] * cs), cs, GrassMid, i * 7 + s, rounded = true)
                }
                if (k == 0 || sel) Box(Modifier.align(Alignment.TopStart).padding(3.dp).size(6.dp).clip(RoundedCornerShape(50)).background(if (sel) pm.goldBright else Color(0xFF3FA34D)))
            }
        }
    }
}

@Composable
private fun MdChip(content: @Composable () -> Unit) {
    val chip = RoundedCornerShape(50)
    Row(
        Modifier.clip(chip).background(pm.surface).border(1.dp, pm.outline, chip).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) { content() }
}

@Composable
private fun MdAction(label: String, active: Boolean = false, onClick: () -> Unit) {
    val chip = RoundedCornerShape(50)
    Text(
        label,
        Modifier.clip(chip).background(if (active) pm.goldSoft else pm.surfaceHigh).border(1.dp, if (active) pm.goldBright else pm.outline, chip)
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp), color = pm.ink, maxLines = 1,
    )
}
