package com.fws.playmind.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Toàn bộ phần VẼ bàn cờ và quân cờ của 3 game (hàm thuần trên DrawScope, không phụ thuộc Compose runtime).
 * Phong cách theo ảnh tham chiếu:
 *  - Cờ vua: quân tiện bạc/đen viền vàng, bàn đen - trắng ngọc trai có khung đen khảm vàng.
 *  - Cờ tướng: bàn lá vàng khắc bản đồ, khung gỗ đỏ thẫm, quân là đĩa kính trong suốt.
 *  - Caro: bàn gỗ sồi sáng vô hạn, X gỗ sẫm, O vòng gỗ sáng.
 */
internal object Lux {
    val goldHi = Color(0xFFFFE9A6); val gold = Color(0xFFD9A93A); val goldLo = Color(0xFF8C6414)
    fun goldBrush(a: Offset, b: Offset) = Brush.linearGradient(0f to goldHi, .45f to gold, 1f to goldLo, start = a, end = b)
}

/** Số ngẫu nhiên xác định (để vân gỗ, hạt vàng... luôn giống nhau giữa các lần vẽ). */
internal fun hash01(a: Int, b: Int = 0): Float {
    var h = a * 374761393 + b * 668265263 + 0x2545F491
    h = (h xor (h ushr 13)) * 1274126177
    h = h xor (h ushr 16)
    return (h and 0xFFFFFF) / 16777216f
}

// ═════════════════════════════ CỜ VUA ═════════════════════════════

fun DrawScope.drawChessPiece(type: Int, white: Boolean, topLeft: Offset, cell: Float) {
    val sc = cell * 0.98f / 100f
    val tx = topLeft.x + (cell - 100f * sc) / 2f
    val ty = topLeft.y + cell * 0.03f
    // bóng đổ mềm dưới chân
    drawOval(Color.Black.copy(alpha = .16f), Offset(topLeft.x + cell * .17f, topLeft.y + cell * .83f), Size(cell * .66f, cell * .13f))
    drawOval(Color.Black.copy(alpha = .16f), Offset(topLeft.x + cell * .23f, topLeft.y + cell * .855f), Size(cell * .54f, cell * .09f))
    withTransform({ translate(tx, ty); scale(sc, sc, pivot = Offset.Zero) }) {
        drawLayers(ChessArt.layers(type, white))
    }
}

/** Khung + 64 ô. Trả về cạnh một ô. Mọi vị trí ô tính theo [chessSquareOrigin]. */
fun chessFrame(side: Float) = side * 0.052f

fun DrawScope.drawChessBoard(tm: TextMeasurer, flip: Boolean) {
    val side = size.width
    val fr = chessFrame(side)
    val s = (side - 2 * fr) / 8f
    val rr = CornerRadius(side * .035f)
    // khung sơn mài đen
    drawRoundRect(Brush.linearGradient(0f to Color(0xFF34343E), .5f to Color(0xFF0C0C11), 1f to Color(0xFF24242C), start = Offset.Zero, end = Offset(side, side)), Offset.Zero, Size(side, side), rr)
    drawRoundRect(Color.White.copy(alpha = .12f), Offset(side * .004f, side * .004f), Size(side * .992f, side * .992f), rr, style = Stroke(side * .004f))
    // dải vàng khảm
    val inlay = fr * .2f
    drawRoundRect(Lux.goldBrush(Offset.Zero, Offset(side, side)), Offset(inlay, inlay), Size(side - 2 * inlay, side - 2 * inlay), CornerRadius(side * .02f), style = Stroke(fr * .08f))
    drawRoundRect(Lux.gold.copy(alpha = .55f), Offset(fr * .06f, fr * .06f), Size(side - fr * .12f, side - fr * .12f), rr, style = Stroke(fr * .03f))
    // ô cờ
    for (r in 0 until 8) for (c in 0 until 8) {
        val x = fr + c * s; val y = fr + r * s
        val light = (r + c) % 2 == 0
        if (light) {
            drawRect(Brush.linearGradient(0f to Color(0xFFFFFFFF), .6f to Color(0xFFF3EFE3), 1f to Color(0xFFE2DCCB), start = Offset(x, y), end = Offset(x + s, y + s)), Offset(x, y), Size(s, s))
            drawRect(Color.White.copy(alpha = .7f), Offset(x, y), Size(s, s * .04f))
        } else {
            drawRect(Brush.linearGradient(0f to Color(0xFF3A3A46), .55f to Color(0xFF1B1B23), 1f to Color(0xFF0E0E13), start = Offset(x, y), end = Offset(x + s, y + s)), Offset(x, y), Size(s, s))
            drawRect(Color.White.copy(alpha = .16f), Offset(x, y), Size(s, s * .04f))
        }
    }
    // gờ vàng quanh bàn
    drawRect(Lux.gold.copy(alpha = .9f), Offset(fr, fr), Size(8 * s, 8 * s), style = Stroke(s * .028f))
    // toạ độ trên khung
    val st = TextStyle(color = Lux.gold.copy(alpha = .95f), fontSize = (fr * .55f).toSp(), fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Serif)
    for (i in 0 until 8) {
        val file = ('a' + (if (flip) 7 - i else i)).toString()
        val rank = ('1' + (if (flip) i else 7 - i)).toString()
        val tf = tm.measure(file, st); val tr = tm.measure(rank, st)
        drawText(tf, topLeft = Offset(fr + i * s + (s - tf.size.width) / 2f, side - fr + (fr - tf.size.height) / 2f))
        drawText(tr, topLeft = Offset((fr - tr.size.width) / 2f, fr + i * s + (s - tr.size.height) / 2f))
    }
}

// ═════════════════════════════ CỜ TƯỚNG ═════════════════════════════

/** Tỉ lệ bàn cờ tướng (rộng/cao) và lề từ mép đến đường kẻ ngoài cùng, tính theo bước lưới. */
const val XQ_MARGIN = 0.86f
const val XQ_ASPECT = (8f + 2 * XQ_MARGIN) / (9f + 2 * XQ_MARGIN)

private fun xqSpiral(cx: Float, cy: Float, r0: Float, turns: Float, sx: Float, sy: Float, rot: Float): Path {
    val p = Path()
    val n = 46
    for (i in 0..n) {
        val t = i / n.toFloat()
        val a = rot + t * turns * 2f * PI.toFloat()
        val r = r0 * (1f - t * .92f)
        val x = cx + sx * r * cos(a); val y = cy + sy * r * sin(a)
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    return p
}

fun DrawScope.drawXiangqiBoard(tm: TextMeasurer, flip: Boolean) {
    val w = size.width; val h = size.height
    val s = w / (8f + 2 * XQ_MARGIN)
    val m = XQ_MARGIN * s
    val fr = s * .30f                       // độ dày khung gỗ
    val rr = CornerRadius(s * .28f)
    // khung gỗ đỏ thẫm
    drawRoundRect(Brush.linearGradient(0f to Color(0xFF8A2C39), .5f to Color(0xFF5A1620), 1f to Color(0xFF3E0E16), start = Offset.Zero, end = Offset(w, h)), Offset.Zero, size, rr)
    drawRoundRect(Color.White.copy(alpha = .16f), Offset(s * .015f, s * .015f), Size(w - s * .03f, h - s * .03f), rr, style = Stroke(s * .03f))
    // mặt lá vàng
    val px = fr; val py = fr; val pw = w - 2 * fr; val ph = h - 2 * fr
    val prr = CornerRadius(s * .12f)
    drawRoundRect(Brush.linearGradient(0f to Color(0xFFF8E4A0), .35f to Color(0xFFE2B95C), .65f to Color(0xFFF1D27E), 1f to Color(0xFFCFA040), start = Offset(px, py), end = Offset(px + pw, py + ph)), Offset(px, py), Size(pw, ph), prr)
    // bản đồ khắc chìm: vài đường đồng mức và dòng sông
    val ink = Color(0xFF6B4A12)
    for (k in 0 until 9) {
        val p = Path()
        val y0 = py + ph * (.08f + .105f * k) + (hash01(k, 3) - .5f) * s * .5f
        p.moveTo(px, y0)
        var x = px
        while (x < px + pw) {
            val nx = x + s * (1.1f + hash01(k, (x / s).toInt()) * .9f)
            val ny = y0 + (hash01(k, (nx / s).toInt() + 40) - .5f) * s * 1.6f
            p.quadraticTo((x + nx) / 2f, (y0 + ny) / 2f + (hash01(k, (x / s).toInt() + 7) - .5f) * s * .8f, min(nx, px + pw), ny)
            x = nx
        }
        drawPath(p, ink.copy(alpha = .22f), style = Stroke(s * .022f, cap = StrokeCap.Round))
    }
    // hạt lá vàng
    for (i in 0 until 520) {
        val x = px + hash01(i, 11) * pw; val y = py + hash01(i, 12) * ph
        val rad = s * (.008f + hash01(i, 13) * .02f)
        val c = if (hash01(i, 14) > .5f) Color.White.copy(alpha = .10f + hash01(i, 15) * .18f) else Color(0xFF8C6414).copy(alpha = .06f + hash01(i, 15) * .12f)
        drawCircle(c, rad, Offset(x, y))
    }
    // vầng sáng kim loại
    drawRoundRect(Brush.radialGradient(0f to Color.White.copy(alpha = .30f), 1f to Color.Transparent, center = Offset(px + pw * .28f, py + ph * .22f), radius = pw * .8f), Offset(px, py), Size(pw, ph), prr)
    // hoa văn mây ở 4 góc
    val o = s * .40f
    for (cx in 0..1) for (cy in 0..1) {
        val x = if (cx == 0) px + o else px + pw - o; val y = if (cy == 0) py + o else py + ph - o
        val sx = if (cx == 0) 1f else -1f; val sy = if (cy == 0) 1f else -1f
        drawPath(xqSpiral(x + sx * s * .06f, y + sy * s * .06f, s * .27f, 2.1f, sx, sy, 0f), ink.copy(alpha = .55f), style = Stroke(s * .028f, cap = StrokeCap.Round))
        drawPath(xqSpiral(x + sx * s * .46f, y + sy * s * .02f, s * .14f, 1.8f, sx, sy, 2.2f), ink.copy(alpha = .40f), style = Stroke(s * .022f, cap = StrokeCap.Round))
        drawPath(xqSpiral(x + sx * s * .02f, y + sy * s * .46f, s * .14f, 1.8f, sx, sy, -1.0f), ink.copy(alpha = .40f), style = Stroke(s * .022f, cap = StrokeCap.Round))
    }
    drawRoundRect(Lux.goldLo.copy(alpha = .75f), Offset(px + s * .07f, py + s * .07f), Size(pw - s * .14f, ph - s * .14f), CornerRadius(s * .08f), style = Stroke(s * .018f))
    // lưới
    val line = Color(0xFF241705)
    val lw = max(s * .03f, 1.2f)
    fun P(f: Int, r: Int) = Offset(m + f * s, m + r * s)
    // viền ngoài đậm hơn
    drawRect(line, P(0, 0), Size(8 * s, 9 * s), style = Stroke(lw * 1.9f))
    for (r in 1..8) drawLine(line, P(0, r), P(8, r), lw)
    for (f in 1..7) { drawLine(line, P(f, 0), P(f, 4), lw); drawLine(line, P(f, 5), P(f, 9), lw) }
    for (r0 in intArrayOf(0, 7)) { drawLine(line, P(3, r0), P(5, r0 + 2), lw); drawLine(line, P(5, r0), P(3, r0 + 2), lw) }
    // dấu + ở vị trí pháo và tốt
    val tk = s * .09f; val gp = s * .07f
    fun tick(f: Int, r: Int) {
        val c = P(f, r)
        for (dx in intArrayOf(-1, 1)) for (dy in intArrayOf(-1, 1)) {
            if (f == 0 && dx < 0) continue
            if (f == 8 && dx > 0) continue
            val x = c.x + dx * gp; val y = c.y + dy * gp
            drawLine(line, Offset(x, y), Offset(x + dx * tk, y), lw * .9f)
            drawLine(line, Offset(x, y), Offset(x, y + dy * tk), lw * .9f)
        }
    }
    for (f in intArrayOf(1, 7)) { tick(f, 2); tick(f, 7) }
    for (f in intArrayOf(0, 2, 4, 6, 8)) { tick(f, 3); tick(f, 6) }
    // sông
    val rs = TextStyle(color = Color(0xFF6B4A12).copy(alpha = .75f), fontSize = (s * .52f).toSp(), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, letterSpacing = (s * .06f).toSp())
    val l = if (flip) "漢界" else "楚河"; val r = if (flip) "楚河" else "漢界"
    val ml = tm.measure(l, rs); val mr = tm.measure(r, rs)
    val ry = m + 4.5f * s - ml.size.height / 2f
    drawText(ml, topLeft = Offset(m + 1.2f * s, ry)); drawText(mr, topLeft = Offset(m + 8 * s - 1.2f * s - mr.size.width, ry))
}

private val RED_CHARS = arrayOf("", "帥", "仕", "相", "傌", "俥", "炮", "兵")
private val BLK_CHARS = arrayOf("", "將", "士", "象", "馬", "車", "砲", "卒")

/** Quân cờ tướng: đĩa kính trong suốt, chữ khắc đỏ (Đỏ) hoặc đen (Đen). [rad] = bán kính ngoài. */
fun DrawScope.drawXiangqiPiece(c: Offset, rad: Float, red: Boolean, type: Int, tm: TextMeasurer) {
    val sc = rad / 46f
    drawOval(Color.Black.copy(alpha = .20f), Offset(c.x - rad * .9f, c.y + rad * .62f), Size(rad * 1.84f, rad * .5f))
    withTransform({ translate(c.x - 50f * sc, c.y - 50f * sc); scale(sc, sc, pivot = Offset.Zero) }) {
        drawLayers(if (red) XiangqiArt.red else XiangqiArt.black)
    }
    val ch = (if (red) RED_CHARS else BLK_CHARS)[type]
    val ink = if (red) Color(0xFFB3122B) else Color(0xFF0C0C12)
    val r = tm.measure(ch, TextStyle(color = ink, fontSize = (rad * 1.12f).toSp(), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif,
        shadow = Shadow(Color.White.copy(alpha = .55f), Offset(rad * .02f, rad * .05f), rad * .03f)))
    drawText(r, topLeft = c - Offset(r.size.width / 2f, r.size.height / 2f + rad * .02f))
}

// ═════════════════════════════ CARO (bàn gỗ vô hạn) ═════════════════════════════

/**
 * Nền bàn caro vô hạn. Ô (x, y) chiếm [x, x+1) x [y, y+1) trong toạ độ thế giới; màn hình = offset + toạ độ * cell.
 * Vân gỗ và đường kẻ gắn với toạ độ thế giới nên khi kéo bàn cờ chúng trượt theo, không bị "bơi".
 */
fun DrawScope.drawCaroBoard(cell: Float, offX: Float, offY: Float) {
    val w = size.width; val h = size.height
    drawRect(Brush.linearGradient(0f to Color(0xFFF6ECDA), .5f to Color(0xFFEFE1C7), 1f to Color(0xFFE6D5B4), start = Offset.Zero, end = Offset(w, h)), Offset.Zero, size)
    // vân gỗ chạy ngang, neo theo toạ độ thế giới
    val gStep = .16f
    val k0 = floor(-offY / cell / gStep).toInt() - 1
    val k1 = floor((h - offY) / cell / gStep).toInt() + 1
    for (k in k0..k1) {
        val seed = hash01(k, 91)
        val a = .035f + hash01(k, 92) * .075f
        val col = if (hash01(k, 93) > .35f) Color(0xFF9A7A45) else Color(0xFFFFFFFF)
        val amp = (.05f + hash01(k, 94) * .14f) * cell
        val fr = .35f + hash01(k, 95) * .6f
        val ph = seed * 6.28f
        val wy = k * gStep
        val path = Path()
        var wx = floor(-offX / cell).toInt() - 1f
        val endX = (w - offX) / cell + 1f
        var first = true
        while (wx <= endX) {
            val x = offX + wx * cell
            val y = offY + wy * cell + sin(wx * fr + ph) * amp
            if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
            wx += .8f
        }
        drawPath(path, col.copy(alpha = if (col == Color(0xFFFFFFFF)) a * 2.2f else a), style = Stroke(max(1f, cell * (.012f + hash01(k, 96) * .02f))))
    }
    // đường kẻ
    val x0 = floor(-offX / cell).toInt() - 1; val x1 = floor((w - offX) / cell).toInt() + 1
    val y0 = floor(-offY / cell).toInt() - 1; val y1 = floor((h - offY) / cell).toInt() + 1
    val base = Color(0xFF6B4F2C)
    val thin = max(1f, cell * .018f)
    for (x in x0..x1) {
        val sx = offX + x * cell
        val major = Math.floorMod(x, 5) == 0
        drawLine(base.copy(alpha = if (major) .62f else .36f), Offset(sx, 0f), Offset(sx, h), if (major) thin * 1.5f else thin)
    }
    for (y in y0..y1) {
        val sy = offY + y * cell
        val major = Math.floorMod(y, 5) == 0
        drawLine(base.copy(alpha = if (major) .62f else .36f), Offset(0f, sy), Offset(w, sy), if (major) thin * 1.5f else thin)
    }
    // dấu gốc toạ độ (ô đầu tiên)
    drawCircle(Lux.goldLo.copy(alpha = .35f), cell * .06f, Offset(offX + .5f * cell, offY + .5f * cell))
    // tối nhẹ ở mép để bàn như một vật thể
    drawRect(Brush.radialGradient(0f to Color.Transparent, .72f to Color.Transparent, 1f to Color(0xFF5A4020).copy(alpha = .20f), center = Offset(w / 2f, h / 2f), radius = max(w, h) * .72f), Offset.Zero, size)
}

fun DrawScope.drawCaroPiece(c: Offset, cell: Float, isX: Boolean) {
    val sc = cell * (if (isX) 1.0f else .94f) / 100f
    // bóng đổ mềm
    if (isX) drawOval(Color.Black.copy(alpha = .13f), Offset(c.x - cell * .34f, c.y - cell * .22f), Size(cell * .7f, cell * .62f))
    else drawCircle(Color.Black.copy(alpha = .12f), cell * .30f, Offset(c.x + cell * .035f, c.y + cell * .07f), style = Stroke(cell * .12f))
    withTransform({ translate(c.x - 50f * sc, c.y - 50f * sc); scale(sc, sc, pivot = Offset.Zero) }) {
        drawLayers(if (isX) CaroArt.x else CaroArt.o)
    }
}

/** Ô đánh dấu nước đi gần nhất: viền vàng bốn góc. */
fun DrawScope.drawCaroLast(c: Offset, cell: Float) {
    val h = cell * .46f; val k = cell * .2f; val lw = max(2f, cell * .05f)
    drawRoundRect(Lux.gold.copy(alpha = .20f), c - Offset(h, h), Size(h * 2, h * 2), CornerRadius(cell * .1f))
    for (sx in intArrayOf(-1, 1)) for (sy in intArrayOf(-1, 1)) {
        val cx = c.x + sx * h; val cy = c.y + sy * h
        drawLine(Lux.gold, Offset(cx, cy), Offset(cx - sx * k, cy), lw, StrokeCap.Round)
        drawLine(Lux.gold, Offset(cx, cy), Offset(cx, cy - sy * k), lw, StrokeCap.Round)
    }
}

/** Vạch chiến thắng nối 5 quân. */
fun DrawScope.drawWinLine(a: Offset, b: Offset, cell: Float) {
    drawLine(Lux.gold.copy(alpha = .28f), a, b, cell * .5f, StrokeCap.Round)
    drawLine(Lux.goldLo, a, b, cell * .17f, StrokeCap.Round)
    drawLine(Lux.goldHi, a, b, cell * .10f, StrokeCap.Round)
}

// ═════════════════════════════ HUY HIỆU NHỎ ═════════════════════════════

/** Quân đại diện một bên (thanh người chơi, ô game, thẻ nổi bật). [s] = cạnh vùng vẽ. */
fun DrawScope.drawSideBadge(gameId: String, side: Int, tm: TextMeasurer, s: Float) {
    when (gameId) {
        "chess" -> drawChessPiece(6, side == 0, Offset.Zero, s)
        "xiangqi" -> drawXiangqiPiece(Offset(s / 2f, s / 2f), s * .46f, side == 0, 1, tm)
        else -> drawCaroPiece(Offset(s / 2f, s / 2f), s * 1.05f, side == 0)
    }
}
