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

/** Tranh minh hoạ Quoridor: bàn rãnh, chốt và tường dài. */
fun DrawScope.drawQuoridorArt(ox: Float, oy: Float, s: Float) {
    val b = s * .86f; val x0 = ox + s * .07f; val y0 = oy + s * .10f
    drawRoundRect(Brush.linearGradient(listOf(Color(0xFFB9703F), Color(0xFF8A4A25)), Offset(x0, y0), Offset(x0 + b, y0 + b)), Offset(x0, y0), Size(b, b), CornerRadius(s * .06f))
    val n = 4; val cell = b / (n + .5f); val gx = x0 + (b - cell * n) / 2f; val gy = y0 + (b - cell * n) / 2f
    for (r in 0 until n) for (c in 0 until n) {
        drawRoundRect(Color(0xFF6E3A1B).copy(alpha = .55f), Offset(gx + c * cell + cell * .07f, gy + r * cell + cell * .07f), Size(cell * .86f, cell * .86f), CornerRadius(cell * .1f))
    }
    val w = cell * .12f
    drawLine(Color(0xFFD7DAE6), Offset(gx + cell * .05f, gy + cell * 1f), Offset(gx + cell * 1.95f, gy + cell * 1f), w, StrokeCap.Round)
    drawLine(Color(0xFFD7DAE6), Offset(gx + cell * 3f, gy + cell * 1.05f), Offset(gx + cell * 3f, gy + cell * 2.95f), w, StrokeCap.Round)
    drawPawn(Offset(gx + cell * 1.5f, gy + cell * 3.45f), cell * .26f, PlayerColors[0])
    drawPawn(Offset(gx + cell * 2.5f, gy + cell * .5f), cell * .26f, PlayerColors[1])
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
