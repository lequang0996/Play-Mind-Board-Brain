package com.fws.playmind.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight

/** Màu bốn bên của Wall Go và Quoridor (đỏ, xanh dương, xanh lá, vàng) theo bộ quân trong ảnh tham chiếu. */
internal val PlayerColors = listOf(Color(0xFFE5483D), Color(0xFF3C7DF0), Color(0xFF2FB36A), Color(0xFFF2C230))

internal fun playerColor(seat: Int): Color = PlayerColors[Math.floorMod(seat, 4)]

/** Quân gỗ tiện dạng chốt: thân trụ bo tròn + đầu tròn, có bóng đổ và điểm sáng. [c] là tâm đáy-thân; [r] là bán kính đầu. */
fun DrawScope.drawPawn(c: Offset, r: Float, color: Color) {
    val dark = lerp(color, Color.Black, .38f); val light = lerp(color, Color.White, .45f)
    drawOval(Color.Black.copy(alpha = .30f), Offset(c.x - r * 1.0f, c.y + r * .62f), Size(r * 2f, r * .75f))
    val bw = r * 1.45f; val bh = r * 1.7f
    drawRoundRect(
        Brush.horizontalGradient(listOf(dark, color, light, color, dark), c.x - bw / 2f, c.x + bw / 2f),
        Offset(c.x - bw / 2f, c.y - bh * .35f), Size(bw, bh), CornerRadius(r * .62f),
    )
    drawCircle(Brush.radialGradient(listOf(light, color, dark), center = Offset(c.x - r * .25f, c.y - r * 1.05f), radius = r * 1.1f), r * .66f, Offset(c.x, c.y - r * .78f))
    drawCircle(Color.White.copy(alpha = .6f), r * .13f, Offset(c.x - r * .24f, c.y - r * 1.0f))
}

/** Quân cờ vây: đá đen / đá trắng có bóng và điểm sáng. */
fun DrawScope.drawStone(c: Offset, r: Float, black: Boolean) {
    drawCircle(Color.Black.copy(alpha = .32f), r, Offset(c.x + r * .07f, c.y + r * .14f))
    val a = if (black) Color(0xFF5B6070) else Color(0xFFFFFFFF)
    val b = if (black) Color(0xFF0B0D13) else Color(0xFFBFC3D0)
    drawCircle(Brush.radialGradient(listOf(a, b), center = Offset(c.x - r * .35f, c.y - r * .4f), radius = r * 1.6f), r, c)
    drawCircle(Color.White.copy(alpha = if (black) .22f else .65f), r * .22f, Offset(c.x - r * .36f, c.y - r * .4f))
}

/** Ô gỗ sồi sáng (viên gỗ Sudoku / nền bàn). */
fun DrawScope.drawWoodCell(topLeft: Offset, s: Float, tone: Float = 0f) {
    val base = lerp(Color(0xFFEBCB96), Color(0xFFD9AE6B), tone.coerceIn(0f, 1f))
    drawRoundRect(Color.Black.copy(alpha = .25f), topLeft + Offset(s * .03f, s * .05f), Size(s, s), CornerRadius(s * .12f))
    drawRoundRect(Brush.linearGradient(listOf(lerp(base, Color.White, .35f), base, lerp(base, Color(0xFF8B5E2B), .35f)), topLeft, topLeft + Offset(s, s)), topLeft, Size(s, s), CornerRadius(s * .12f))
    // vân gỗ
    for (i in 1..3) {
        val y = topLeft.y + s * (.2f * i + .06f * (i % 2))
        drawLine(Color(0xFF8B5E2B).copy(alpha = .16f), Offset(topLeft.x + s * .08f, y), Offset(topLeft.x + s * .92f, y + s * .02f), s * .014f, StrokeCap.Round)
    }
    drawRoundRect(Color.White.copy(alpha = .35f), topLeft, Size(s, s), CornerRadius(s * .12f), style = Stroke(s * .02f))
}

private fun DrawScope.centerText(tm: TextMeasurer, text: String, c: Offset, px: Float, color: Color) {
    val layout = tm.measure(text, TextStyle(fontSize = px.toSp(), fontWeight = FontWeight.ExtraBold, color = color, fontFamily = BodyFont))
    drawText(layout, topLeft = Offset(c.x - layout.size.width / 2f, c.y - layout.size.height / 2f))
}

/** Tranh minh hoạ Sudoku: ba viên gỗ ghi số. */
fun DrawScope.drawSudokuArt(tm: TextMeasurer, ox: Float, oy: Float, s: Float) {
    val t = s * .46f
    val dark = Color(0xFF2A2218); val red = Color(0xFFC8372D)
    val spots = listOf(Offset(ox + s * .06f, oy + s * .12f) to ("5" to dark), Offset(ox + s * .50f, oy + s * .20f) to ("8" to red), Offset(ox + s * .26f, oy + s * .52f) to ("3" to dark))
    for ((p, d) in spots) {
        drawWoodCell(p, t, tone = if (d.second == red) .35f else 0f)
        centerText(tm, d.first, p + Offset(t / 2f, t / 2f), t * .62f, d.second)
    }
}

/** Tranh minh hoạ Ma phương: lưới 3x3 viên gỗ (ma phương Lạc Thư, mọi hàng / cột / chéo cùng tổng 15). */
fun DrawScope.drawMagicArt(tm: TextMeasurer, ox: Float, oy: Float, s: Float) {
    val digits = intArrayOf(4, 9, 2, 3, 5, 7, 8, 1, 6)
    val t = s * .29f; val gap = s * .03f; val x0 = ox + (s - 3 * t - 2 * gap) / 2f; val y0 = oy + (s - 3 * t - 2 * gap) / 2f
    for (i in 0 until 9) {
        val p = Offset(x0 + (i % 3) * (t + gap), y0 + (i / 3) * (t + gap))
        val center = i == 4
        drawWoodCell(p, t, tone = if (center) .45f else 0f)
        centerText(tm, digits[i].toString(), p + Offset(t / 2f, t / 2f), t * .58f, if (center) Color(0xFFC8372D) else Color(0xFF2A2218))
    }
}

/** Tranh minh hoạ Khối gỗ ghép chữ: tháp khối gỗ chữ cái (như tháp chữ C-O-D-E). */
fun DrawScope.drawWordBlocksArt(tm: TextMeasurer, ox: Float, oy: Float, s: Float) {
    val t = s * .30f; val gap = s * .03f
    val rows = listOf("A", "BC", "XYZ")
    for ((r, row) in rows.withIndex()) {
        val w = row.length * t + (row.length - 1) * gap
        for ((c, ch) in row.withIndex()) {
            val p = Offset(ox + (s - w) / 2f + c * (t + gap), oy + s * .04f + r * (t + gap))
            val hi = r == 0
            drawWoodCell(p, t, tone = if (hi) .4f else 0f)
            centerText(tm, ch.toString(), p + Offset(t / 2f, t / 2f), t * .58f, if (hi) Color(0xFFC8372D) else Color(0xFF5A2A10))
        }
    }
}

/** Tranh minh hoạ Wall Go: bàn lưới có tường màu và hai quân. */
fun DrawScope.drawWallGoArt(ox: Float, oy: Float, s: Float) {
    val b = s * .86f; val x0 = ox + s * .07f; val y0 = oy + s * .10f
    drawRoundRect(Brush.linearGradient(listOf(Color(0xFFC9AE8A), Color(0xFF9B7F5C)), Offset(x0, y0), Offset(x0 + b, y0 + b)), Offset(x0, y0), Size(b, b), CornerRadius(s * .06f))
    val cell = b / 3.2f; val gx = x0 + (b - cell * 3f) / 2f; val gy = y0 + (b - cell * 3f) / 2f
    for (r in 0 until 3) for (c in 0 until 3) {
        drawRoundRect(Color(0xFF6E5638).copy(alpha = .75f), Offset(gx + c * cell + cell * .06f, gy + r * cell + cell * .06f), Size(cell * .88f, cell * .88f), CornerRadius(cell * .1f))
    }
    val wallW = cell * .1f
    drawLine(PlayerColors[0], Offset(gx + cell, gy + cell * .05f), Offset(gx + cell, gy + cell * 1.95f), wallW, StrokeCap.Round)
    drawLine(PlayerColors[1], Offset(gx + cell * 1.05f, gy + cell * 2f), Offset(gx + cell * 2.95f, gy + cell * 2f), wallW, StrokeCap.Round)
    drawLine(PlayerColors[3], Offset(gx + cell * 2f, gy + cell * .05f), Offset(gx + cell * 2f, gy + cell * 1f), wallW, StrokeCap.Round)
    drawPawn(Offset(gx + cell * .5f, gy + cell * 1.55f), cell * .22f, PlayerColors[0])
    drawPawn(Offset(gx + cell * 1.5f, gy + cell * .6f), cell * .22f, PlayerColors[2])
    drawPawn(Offset(gx + cell * 2.5f, gy + cell * 2.55f), cell * .22f, PlayerColors[1])
}

/** Ô màu đồng của bàn Quoridor (in 3D): mặt vát sáng, vân lớp in mờ, bóng đổ xuống rãnh; [goal] tô nhẹ màu bên có đích ở hàng này. */
fun DrawScope.drawCopperTile(tl: Offset, s: Float, goal: Color?, seed: Int = 0) {
    val cr = CornerRadius(s * .14f)
    drawRoundRect(Color(0xFF4F7F8F).copy(alpha = .35f), tl + Offset(s * .03f, s * .07f), Size(s, s), cr)
    val base = if (goal != null) lerp(Color(0xFFB66C43), goal, .22f) else Color(0xFFB66C43)
    drawRoundRect(Brush.verticalGradient(listOf(lerp(base, Color.White, .22f), base, lerp(base, Color.Black, .18f)), tl.y, tl.y + s), tl, Size(s, s), cr)
    // vân lớp in rất mờ
    val shift = (seed % 3) * s * .05f
    for (i in 1..4) {
        val y = tl.y + s * (.18f * i) + shift
        if (y < tl.y + s * .92f) drawLine(Color.Black.copy(alpha = .06f), Offset(tl.x + s * .1f, y), Offset(tl.x + s * .9f, y), s * .02f)
    }
    drawRoundRect(Color.White.copy(alpha = .28f), tl, Size(s, s), cr, style = Stroke(s * .035f))
    if (goal != null) drawRoundRect(goal.copy(alpha = .55f), tl + Offset(s * .3f, s * .44f), Size(s * .4f, s * .12f), CornerRadius(s * .06f))
}

/** Quân Quoridor kiểu nhựa bóng vân xoáy: đế tròn, cổ thon, đầu cầu có vân màu và điểm sáng. [c] = tâm đế, [r] = bán kính đầu. */
fun DrawScope.drawQuoridorPawn(c: Offset, r: Float, color: Color) {
    val dark = lerp(color, Color.Black, .40f); val light = lerp(color, Color.White, .55f); val alt = lerp(color, Color(0xFF7B5CFF), .35f)
    // bóng đổ
    drawOval(Color.Black.copy(alpha = .32f), Offset(c.x - r * 1.15f, c.y - r * .1f), Size(r * 2.3f, r * .8f))
    // đế
    drawOval(Brush.horizontalGradient(listOf(dark, color, light, color, dark), c.x - r * 1.05f, c.x + r * 1.05f), Offset(c.x - r * 1.05f, c.y - r * .5f), Size(r * 2.1f, r * .78f))
    drawOval(lerp(color, Color.White, .25f), Offset(c.x - r * .8f, c.y - r * .5f), Size(r * 1.6f, r * .42f))
    // cổ thon
    val neck = androidx.compose.ui.graphics.Path().apply {
        moveTo(c.x - r * .62f, c.y - r * .3f)
        cubicTo(c.x - r * .5f, c.y - r * .9f, c.x - r * .22f, c.y - r * 1.2f, c.x - r * .26f, c.y - r * 1.55f)
        lineTo(c.x + r * .26f, c.y - r * 1.55f)
        cubicTo(c.x + r * .22f, c.y - r * 1.2f, c.x + r * .5f, c.y - r * .9f, c.x + r * .62f, c.y - r * .3f)
        close()
    }
    drawPath(neck, Brush.horizontalGradient(listOf(dark, color, light, color, dark), c.x - r * .62f, c.x + r * .62f))
    // đầu cầu
    val hc = Offset(c.x, c.y - r * 2.05f)
    drawCircle(Brush.radialGradient(listOf(light, color, dark), center = Offset(hc.x - r * .3f, hc.y - r * .35f), radius = r * 1.25f), r * .78f, hc)
    drawCircle(Brush.sweepGradient(listOf(color, alt, light, color, dark, alt, color), hc), r * .78f, hc, alpha = .32f)
    drawCircle(Color.White.copy(alpha = .75f), r * .17f, Offset(hc.x - r * .28f, hc.y - r * .32f))
    drawCircle(Color.White.copy(alpha = .25f), r * .78f, hc, style = Stroke(r * .05f))
}

/** Tranh minh hoạ Quoridor: bàn ô đồng rãnh xanh, tường bạc và hai quân. */
fun DrawScope.drawQuoridorArt(ox: Float, oy: Float, s: Float) {
    val b = s * .86f; val x0 = ox + s * .07f; val y0 = oy + s * .10f
    drawRoundRect(Brush.linearGradient(listOf(Color(0xFFC9EAF3), Color(0xFF9CCFDF)), Offset(x0, y0), Offset(x0 + b, y0 + b)), Offset(x0, y0), Size(b, b), CornerRadius(s * .07f))
    drawRoundRect(Color(0xFFA9653D), Offset(x0, y0), Size(b, b), CornerRadius(s * .07f), style = Stroke(s * .025f))
    val n = 4; val cell = b / (n + .5f); val gx = x0 + (b - cell * n) / 2f; val gy = y0 + (b - cell * n) / 2f
    for (r in 0 until n) for (c in 0 until n) drawCopperTile(Offset(gx + c * cell + cell * .07f, gy + r * cell + cell * .07f), cell * .86f, null, r * 4 + c)
    val w = cell * .16f
    drawLine(PlayerColors[0], Offset(gx + cell * .1f, gy + cell * 1f), Offset(gx + cell * 1.9f, gy + cell * 1f), w * .6f, StrokeCap.Round)
    drawLine(Color(0xFFE9ECF2), Offset(gx + cell * .1f, gy + cell * .94f), Offset(gx + cell * 1.9f, gy + cell * .94f), w, StrokeCap.Round)
    drawLine(PlayerColors[1], Offset(gx + cell * 3f, gy + cell * 1.1f), Offset(gx + cell * 3f, gy + cell * 2.9f), w * .6f, StrokeCap.Round)
    drawLine(Color(0xFFE9ECF2), Offset(gx + cell * 2.96f, gy + cell * 1.1f), Offset(gx + cell * 2.96f, gy + cell * 2.9f), w, StrokeCap.Round)
    drawQuoridorPawn(Offset(gx + cell * 1.5f, gy + cell * 3.75f), cell * .3f, PlayerColors[0])
    drawQuoridorPawn(Offset(gx + cell * 2.5f, gy + cell * .8f), cell * .3f, PlayerColors[1])
}

/** Tranh minh hoạ cờ vây: góc bàn gỗ vàng và vài quân đá. */
fun DrawScope.drawGoArt(ox: Float, oy: Float, s: Float) {
    val b = s * .88f; val x0 = ox + s * .06f; val y0 = oy + s * .08f
    drawRoundRect(Brush.linearGradient(listOf(Color(0xFFF0CE7A), Color(0xFFD9A94E)), Offset(x0, y0), Offset(x0 + b, y0 + b)), Offset(x0, y0), Size(b, b), CornerRadius(s * .05f))
    val n = 4; val step = b / (n + .6f); val gx = x0 + (b - step * (n - 1)) / 2f; val gy = y0 + (b - step * (n - 1)) / 2f
    for (i in 0 until n) {
        drawLine(Color(0xFF6B4F2C).copy(alpha = .8f), Offset(gx, gy + i * step), Offset(gx + step * (n - 1), gy + i * step), s * .012f)
        drawLine(Color(0xFF6B4F2C).copy(alpha = .8f), Offset(gx + i * step, gy), Offset(gx + i * step, gy + step * (n - 1)), s * .012f)
    }
    val r = step * .46f
    drawStone(Offset(gx + step, gy + step), r, true)
    drawStone(Offset(gx + step * 2f, gy + step), r, false)
    drawStone(Offset(gx + step * 2f, gy + step * 2f), r, true)
    drawStone(Offset(gx + step, gy + step * 2f), r, false)
}
