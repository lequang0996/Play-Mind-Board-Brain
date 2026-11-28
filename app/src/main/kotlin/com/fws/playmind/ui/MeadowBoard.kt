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
private val CouponBlue = Color(0xFF2F8FE0)
private val StealPurple = Color(0xFF6B3FA0)
private val StoneGray = Color(0xFF8E939C)
private val PieceGold = Color(0xFFE8B23A)

/** Màu lãnh thổ: màu ghế pha xanh cỏ để bàn vẫn ra "đồng cỏ". */
private fun turfColor(seat: Int): Color = lerp(playerColor(seat), Color(0xFF3FA34D), .18f)

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

/** Biểu tượng phiếu đổi gạch: hai mũi tên vòng xanh. */
fun DrawScope.drawCouponIcon(c: Offset, r: Float) {
    drawCircle(Color.White, r, c)
    drawArc(CouponBlue, 200f, 140f, false, Offset(c.x - r * .62f, c.y - r * .62f), Size(r * 1.24f, r * 1.24f), style = Stroke(r * .24f, cap = StrokeCap.Round))
    drawArc(CouponBlue, 20f, 140f, false, Offset(c.x - r * .62f, c.y - r * .62f), Size(r * 1.24f, r * 1.24f), style = Stroke(r * .24f, cap = StrokeCap.Round))
    drawCircle(CouponBlue, r * .16f, Offset(c.x + r * .58f, c.y - r * .22f)); drawCircle(CouponBlue, r * .16f, Offset(c.x - r * .58f, c.y + r * .22f))
}

/** Biểu tượng cướp mảnh: lưỡi dao tím. */
fun DrawScope.drawStealIcon(c: Offset, r: Float) {
    drawCircle(Color.White, r, c)
    val blade = Path().apply {
        moveTo(c.x - r * .55f, c.y + r * .55f); lineTo(c.x + r * .25f, c.y - r * .35f); lineTo(c.x + r * .55f, c.y - r * .62f)
        lineTo(c.x + r * .38f, c.y - r * .1f); lineTo(c.x - r * .35f, c.y + r * .62f); close()
    }
    drawPath(blade, StealPurple)
    drawLine(Color(0xFF3B2160), Offset(c.x - r * .7f, c.y + r * .7f), Offset(c.x - r * .45f, c.y + r * .45f), r * .22f, StrokeCap.Round)
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

/** Tranh minh hoạ: góc đồng cỏ với ba lãnh thổ và một ô đặc biệt. */
fun DrawScope.drawMeadowArt(ox: Float, oy: Float, s: Float) {
    val n = 6; val cs = s * .86f / n; val x0 = ox + s * .07f; val y0 = oy + s * .07f
    drawRoundRect(GrassBg, Offset(x0 - cs * .1f, y0 - cs * .1f), Size(cs * n + cs * .2f, cs * n + cs * .2f), CornerRadius(cs * .4f))
    val map = arrayOf("000.11", "000.11", "00..1.", "..2...", ".222.3", ".22.33")
    for (r in 0 until n) for (c in 0 until n) {
        val ch = map[r][c]
        val tl = Offset(x0 + c * cs, y0 + r * cs)
        if (ch == '.') drawRect(GridLine.copy(alpha = .45f), tl, Size(cs, cs), style = Stroke(1f))
        else drawRoundRect(turfColor(ch - '0'), tl + Offset(cs * .04f, cs * .04f), Size(cs * .92f, cs * .92f), CornerRadius(cs * .15f))
    }
    drawCouponIcon(Offset(x0 + 3.5f * cs, y0 + 1.5f * cs), cs * .42f)
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
        val controlsH = 196.dp
        val side = minOf(maxWidth, maxHeight - controlsH).coerceAtLeast(200.dp)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Canvas(
                Modifier.size(side).clip(RoundedCornerShape(10.dp))
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
                val cs = size.width / MeadowGame.N
                drawRect(GrassBg)
                // vạch 9 khu 10×10
                for (i in 0..MeadowGame.N) {
                    val strong = i % 10 == 0
                    drawLine(if (strong) Color(0xFFC9A48C) else GridLine, Offset(i * cs, 0f), Offset(i * cs, size.height), if (strong) 1.6f else .6f)
                    drawLine(if (strong) Color(0xFFC9A48C) else GridLine, Offset(0f, i * cs), Offset(size.width, i * cs), if (strong) 1.6f else .6f)
                }
                for (c in 0 until MeadowGame.CELLS) {
                    val tl = Offset((c % MeadowGame.N) * cs, (c / MeadowGame.N) * cs)
                    val o = g.owner[c]
                    if (o >= 0) {
                        val col = turfColor(o)
                        drawRect(col, tl, Size(cs, cs))
                        // viền mảnh: kẻ cạnh khi ô bên cạnh thuộc mảnh khác
                        val t = g.tileOf[c]
                        val x = c % MeadowGame.N; val y = c / MeadowGame.N
                        val edge = lerp(col, Color.Black, .35f)
                        if (x == MeadowGame.N - 1 || g.tileOf[c + 1] != t || g.owner[c + 1] != o) drawLine(edge, tl + Offset(cs, 0f), tl + Offset(cs, cs), 1.2f)
                        if (y == MeadowGame.N - 1 || g.tileOf[c + MeadowGame.N] != t || g.owner[c + MeadowGame.N] != o) drawLine(edge, tl + Offset(0f, cs), tl + Offset(cs, cs), 1.2f)
                        if (x == 0 || g.tileOf[c - 1] != t || g.owner[c - 1] != o) drawLine(edge, tl, tl + Offset(0f, cs), 1.2f)
                        if (y == 0 || g.tileOf[c - MeadowGame.N] != t || g.owner[c - MeadowGame.N] != o) drawLine(edge, tl, tl + Offset(cs, 0f), 1.2f)
                        if (t >= 0 && t == stealPick) drawRect(Color.White.copy(alpha = .45f), tl, Size(cs, cs))
                    }
                    val ctr = tl + Offset(cs / 2, cs / 2)
                    when (g.special[c]) {
                        MeadowGame.SP_COUPON -> drawCouponIcon(ctr, cs * .45f)
                        MeadowGame.SP_PIECE -> drawPieceIcon(ctr, cs * .5f, tm)
                        MeadowGame.SP_STONE -> drawStoneIcon(ctr, cs * .45f, false)
                        MeadowGame.SP_STEAL -> drawStealIcon(ctr, cs * .45f)
                    }
                    if (g.stone[c]) drawStoneIcon(ctr, cs * .48f, true)
                }
                // ô vừa đặt gần nhất
                val lm = g.lastMove
                if (MeadowGame.isPlace(lm)) {
                    // đánh dấu nhẹ mọi ô của mảnh mới nhất
                    val last = g.insts.lastOrNull()
                    if (last != null && last.alive) for (c in last.cells) drawRect(Color.White.copy(alpha = .22f), Offset((c % MeadowGame.N) * cs, (c / MeadowGame.N) * cs), Size(cs, cs))
                }
                // bóng mảnh / ô chọn
                if (ghost != null) for (c in ghost) {
                    val tl = Offset((c % MeadowGame.N) * cs, (c / MeadowGame.N) * cs)
                    drawRect((if (ghostOk) Color.White else Color(0xFFFF5A4E)).copy(alpha = .55f), tl, Size(cs, cs))
                    drawRect(if (ghostOk) Color(0xFF1E8E3E) else Color(0xFFB3261E), tl, Size(cs, cs), style = Stroke(1.6f))
                } else if (anchor >= 0 && shape < 0) {
                    val tl = Offset((anchor % MeadowGame.N) * cs, (anchor / MeadowGame.N) * cs)
                    drawRect(Color.White.copy(alpha = .6f), tl, Size(cs, cs)); drawRect(Color(0xFF1E8E3E), tl, Size(cs, cs), style = Stroke(2f))
                }
            }
            // hàng chờ công khai
            QueueStrip(g, act && phase == MeadowPhase.TURN && !g.couponUsed && g.coupons[me] > 0) { askCoupon = it }
            // bảng điều khiển
            Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MdChip { Text(stringResource(R.string.md_round, g.round.coerceIn(1, MeadowGame.ROUNDS), MeadowGame.ROUNDS), style = MaterialTheme.typography.labelLarge, color = pm.ink) }
                    MdChip { Canvas(Modifier.size(16.dp)) { drawCouponIcon(Offset(size.width / 2, size.height / 2), size.width / 2) }; Text("${g.coupons[me]}", style = MaterialTheme.typography.labelLarge, color = pm.ink) }
                    MdChip { Canvas(Modifier.size(16.dp)) { drawPieceIcon(Offset(size.width / 2, size.height / 2), size.width / 2, null) }; Text("${g.pieces[me]}", style = MaterialTheme.typography.labelLarge, color = pm.ink) }
                    if (act && phase == MeadowPhase.TURN && g.pieces[me] > 0) {
                        MdAction(stringResource(R.string.md_buy_coupon)) { vm.playMove(MeadowGame.BUY_COUPON) }
                        if (g.stone.any { it }) MdAction(stringResource(if (clearMode) R.string.cancel else R.string.md_clear_stone), active = clearMode) { clearMode = !clearMode }
                    }
                }
                Text(promptText(g, act, clearMode), style = MaterialTheme.typography.labelMedium, color = pm.gold)
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

/** Hàng chờ: 5 mảnh tới (mảnh đang lấy viền vàng; chạm mảnh 2-5 để dùng phiếu) và vài mảnh sau đó mờ hơn. */
@Composable
private fun QueueStrip(g: MeadowGame, canSwap: Boolean, onSwap: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        val upto = minOf(g.queue.size, 12)
        for (k in 0 until upto) {
            val sel = k == g.pickOff && g.phase == MeadowPhase.TURN
            val inChoice = k < MeadowGame.CHOICE
            val shape = RoundedCornerShape(10.dp)
            Box(
                Modifier.size(if (inChoice) 46.dp else 34.dp).clip(shape)
                    .background(if (sel) pm.goldSoft else pm.surface.copy(alpha = if (inChoice) 1f else .6f))
                    .border(if (sel) 2.dp else 1.dp, if (sel) pm.goldBright else pm.outline, shape)
                    .then(if (canSwap && k in 1 until MeadowGame.CHOICE) Modifier.clickable { onSwap(k) } else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                val s = g.queue[k]
                Canvas(Modifier.size(if (inChoice) 38.dp else 28.dp)) {
                    val o = MeadowGame.ORIENTS[s][0]
                    val (w, h) = MeadowGame.extent(s, 0)
                    val cs = minOf(size.width / maxOf(w, 3), size.height / maxOf(h, 3))
                    val x0 = (size.width - w * cs) / 2; val y0 = (size.height - h * cs) / 2
                    for (i in 0 until o.size / 2) {
                        drawRoundRect(Color(0xFF3FA34D), Offset(x0 + o[2 * i] * cs + .5f, y0 + o[2 * i + 1] * cs + .5f), Size(cs - 1f, cs - 1f), CornerRadius(cs * .18f))
                    }
                }
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
