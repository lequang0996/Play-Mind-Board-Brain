package com.fws.playmind.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
    val goldBright = Color(0xFFE3B64E)
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
    val rr = CornerRadius(s * .22f)
    // khung gỗ nâu ấm, có vệt sáng ở mép trên
    drawRoundRect(Brush.linearGradient(0f to Color(0xFFC08448), .5f to Color(0xFF9A6234), 1f to Color(0xFF734321), start = Offset.Zero, end = Offset(w, h)), Offset.Zero, size, rr)
    drawRoundRect(Color.White.copy(alpha = .22f), Offset(s * .015f, s * .015f), Size(w - s * .03f, h - s * .03f), rr, style = Stroke(s * .03f))
    // mặt bàn gỗ sáng
    val px = fr; val py = fr; val pw = w - 2 * fr; val ph = h - 2 * fr
    val prr = CornerRadius(s * .08f)
    drawRoundRect(Brush.linearGradient(0f to Color(0xFFF3D49E), .45f to Color(0xFFE6BC7E), 1f to Color(0xFFDDAC6A), start = Offset(px, py), end = Offset(px + pw, py + ph)), Offset(px, py), Size(pw, ph), prr)
    // vân gỗ: nhiều đường ngang mảnh hơi lượn sóng
    val grain = Color(0xFF8A5A24)
    for (k in 0 until 46) {
        val y0 = py + ph * (k + hash01(k, 21) * .8f) / 46f
        val p = Path(); p.moveTo(px, y0)
        var x = px
        while (x < px + pw) {
            val nx = min(x + s * (1.6f + hash01(k, (x / s).toInt() + 5) * 1.4f), px + pw)
            p.quadraticTo((x + nx) / 2f, y0 + (hash01(k, (x / s).toInt() + 9) - .5f) * s * .22f, nx, y0 + (hash01(k, (nx / s).toInt() + 13) - .5f) * s * .1f)
            x = nx
        }
        drawPath(p, grain.copy(alpha = .05f + hash01(k, 30) * .10f), style = Stroke(s * (.012f + hash01(k, 31) * .02f), cap = StrokeCap.Round))
    }
    // vài mắt gỗ và hạt nhỏ
    for (i in 0 until 5) {
        val kx = px + pw * (.12f + hash01(i, 41) * .76f); val ky = py + ph * (.10f + hash01(i, 42) * .80f)
        drawOval(grain.copy(alpha = .10f), Offset(kx - s * .22f, ky - s * .07f), Size(s * .44f, s * .14f), style = Stroke(s * .014f))
    }
    for (i in 0 until 260) {
        val x = px + hash01(i, 11) * pw; val y = py + hash01(i, 12) * ph
        drawCircle(grain.copy(alpha = .04f + hash01(i, 15) * .07f), s * (.006f + hash01(i, 13) * .012f), Offset(x, y))
    }
    // ánh sáng mềm từ góc trên trái
    drawRoundRect(Brush.radialGradient(0f to Color.White.copy(alpha = .20f), 1f to Color.Transparent, center = Offset(px + pw * .25f, py + ph * .2f), radius = pw * .85f), Offset(px, py), Size(pw, ph), prr)
    // lưới: nét mảnh màu nâu đậm như bàn cờ gỗ thật
    val line = Color(0xFF5B3A1B)
    val lw = max(s * .022f, 1.1f)
    fun P(f: Int, r: Int) = Offset(m + f * s, m + r * s)
    drawRect(line, P(0, 0), Size(8 * s, 9 * s), style = Stroke(lw * 1.8f))
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
    // sông: chữ khắc nâu sẫm (kèm bóng sáng nhẹ để có cảm giác khắc chìm)
    val rs = TextStyle(color = Color(0xFF4A2D12).copy(alpha = .88f), fontSize = (s * .56f).toSp(), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, letterSpacing = (s * .06f).toSp(),
        shadow = Shadow(Color.White.copy(alpha = .35f), Offset(s * .012f, s * .02f), s * .02f))
    val l = if (flip) "漢界" else "楚河"; val r = if (flip) "楚河" else "漢界"
    val ml = tm.measure(l, rs); val mr = tm.measure(r, rs)
    val ry = m + 4.5f * s - ml.size.height / 2f
    drawText(ml, topLeft = Offset(m + 1.2f * s, ry)); drawText(mr, topLeft = Offset(m + 8 * s - 1.2f * s - mr.size.width, ry))
}

private val RED_CHARS = arrayOf("", "帥", "仕", "相", "傌", "俥", "炮", "兵")
private val BLK_CHARS = arrayOf("", "將", "士", "象", "馬", "車", "砲", "卒")

/** Quân cờ tướng: đĩa gỗ tròn có cạnh dày, vòng khắc và chữ đỏ (Đỏ) hoặc đen (Đen). [rad] = bán kính ngoài. */
fun DrawScope.drawXiangqiPiece(c: Offset, rad: Float, red: Boolean, type: Int, tm: TextMeasurer) {
    val edge = rad * .10f                    // độ dày của cạnh đĩa
    drawOval(Color.Black.copy(alpha = .26f), Offset(c.x - rad * .95f, c.y + rad * .62f), Size(rad * 1.9f, rad * .5f))
    // cạnh đĩa (phần gỗ sẫm bên dưới), rồi mặt đĩa
    drawCircle(Color(0xFF9C6B34), rad, c + Offset(0f, edge))
    drawCircle(Brush.radialGradient(0f to Color(0xFFF9E6BF), .65f to Color(0xFFE8C48A), 1f to Color(0xFFD2A062), center = c - Offset(rad * .28f, rad * .30f), radius = rad * 1.5f), rad, c)
    drawCircle(Color(0xFFA77438).copy(alpha = .75f), rad, c, style = Stroke(rad * .05f))
    // vòng khắc bên trong theo màu của bên đi
    val inkRing = if (red) Color(0xFFC0182B) else Color(0xFF1B140D)
    drawCircle(inkRing.copy(alpha = .55f), rad * .80f, c, style = Stroke(rad * .035f))
    // vân gỗ mờ trên mặt đĩa
    for (k in 0 until 4) {
        val yy = c.y - rad * .6f + rad * .4f * k
        drawLine(Color(0xFF8A5A24).copy(alpha = .06f), Offset(c.x - rad * .7f, yy), Offset(c.x + rad * .7f, yy + rad * .06f), rad * .03f)
    }
    val ch = (if (red) RED_CHARS else BLK_CHARS)[type]
    val ink = if (red) Color(0xFFC0182B) else Color(0xFF15110D)
    val r = tm.measure(ch, TextStyle(color = ink, fontSize = (rad * 1.08f).toSp(), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif,
        shadow = Shadow(Color.White.copy(alpha = .45f), Offset(rad * .02f, rad * .04f), rad * .03f)))
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
    if (isX) {
        // bóng của chữ X cũng phải là hình chữ X (trước đây là hình bầu dục nên trông như bóng của quân O)
        val k = cell * .27f; val off = Offset(cell * .035f, cell * .07f); val sw = cell * .17f
        drawLine(Color.Black.copy(alpha = .14f), Offset(c.x - k, c.y - k) + off, Offset(c.x + k, c.y + k) + off, sw, StrokeCap.Round)
        drawLine(Color.Black.copy(alpha = .14f), Offset(c.x + k, c.y - k) + off, Offset(c.x - k, c.y + k) + off, sw, StrokeCap.Round)
    } else drawCircle(Color.Black.copy(alpha = .12f), cell * .30f, Offset(c.x + cell * .035f, c.y + cell * .07f), style = Stroke(cell * .12f))
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
        "go" -> drawStone(Offset(s / 2f, s / 2f), s * .44f, side == 0)
        "wallgo" -> drawPawn(Offset(s / 2f, s * .56f), s * .27f, playerColor(side))
        "quoridor" -> drawQuoridorPawn(Offset(s / 2f, s * .86f), s * .25f, playerColor(side))
        "hilo" -> {
            drawCircle(lerp(playerColor(side), Color(0xFF6A3FD0), .3f), s * .38f, Offset(s / 2f, s / 2f))
            drawCircle(Color.White.copy(alpha = .8f), s * .38f, Offset(s / 2f, s / 2f), style = Stroke(s * .06f))
            drawCircle(Color.White.copy(alpha = .55f), s * .22f, Offset(s / 2f, s / 2f), style = Stroke(s * .03f))
        }
        "meadow" -> {
            drawRoundRect(lerp(playerColor(side), Color(0xFF3FA34D), .18f), Offset(s * .14f, s * .14f), Size(s * .72f, s * .72f), CornerRadius(s * .16f))
            drawRoundRect(Color.Black.copy(alpha = .25f), Offset(s * .14f, s * .14f), Size(s * .72f, s * .72f), CornerRadius(s * .16f), style = Stroke(s * .05f))
        }
        else -> drawCaroPiece(Offset(s / 2f, s / 2f), s * 1.05f, side == 0)
    }
}
