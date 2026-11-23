package com.fws.playmind.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

/**
 * Bộ vẽ "ngọc – pha lê – thủy tinh" dùng chung cho cả 3 game (cờ vua, cờ tướng, caro).
 * Mọi thứ vẽ bằng Canvas (không dùng ảnh) nên không tốn dung lượng và sắc nét ở mọi độ phân giải.
 * Ánh sáng giả định chiếu từ góc trên-bên-trái: mặt sáng ở trên-trái, bóng đổ xuống dưới-phải.
 */
internal object GlassPalette {
    // Hồng ngọc (đỏ)
    val rubyHi = Color(0xFFFFB3B8); val ruby = Color(0xFFE0293F); val rubyLo = Color(0xFF7A0E22)
    // Hắc ngọc / thủy tinh khói (đen)
    val obsHi = Color(0xFF8E96D8); val obs = Color(0xFF2E3270); val obsLo = Color(0xFF0A0C24)
    // Chữ khắc trên quân cờ tướng
    val cream = Color(0xFFFFF4E0)
}

/** Viên ngọc tròn 3D (dùng cho quân cờ tướng): thân gradient, viền, vòng khắc và vệt phản chiếu. */
internal fun DrawScope.drawGlassDisc(c: Offset, rad: Float, hi: Color, mid: Color, lo: Color) {
    // bóng đổ trên mặt bàn
    drawOval(Color.Black.copy(alpha = .22f), Offset(c.x - rad * .92f, c.y + rad * .72f), Size(rad * 1.84f, rad * .5f))
    // thân ngọc
    drawCircle(Brush.radialGradient(listOf(hi, mid, lo), c - Offset(rad * .35f, rad * .45f), rad * 1.7f), rad, c)
    // viền ngoài tối + vòng khắc sáng bên trong
    drawCircle(lo.copy(alpha = .9f), rad, c, style = Stroke(rad * .07f))
    drawCircle(Color.White.copy(alpha = .35f), rad * .78f, c, style = Stroke(rad * .03f))
    // vệt phản chiếu chính (trên-trái)
    rotate(-28f, pivot = c) {
        drawOval(
            Brush.verticalGradient(listOf(Color.White.copy(alpha = .8f), Color.White.copy(alpha = 0f)), c.y - rad * .9f, c.y - rad * .35f),
            Offset(c.x - rad * .55f, c.y - rad * .9f), Size(rad * 1.1f, rad * .55f),
        )
    }
    // ánh sáng khúc xạ phụ (dưới-phải)
    drawArc(
        Color.White.copy(alpha = .3f), 20f, 60f, false,
        topLeft = c - Offset(rad * .86f, rad * .86f), size = Size(rad * 1.72f, rad * 1.72f),
        style = Stroke(rad * .05f, cap = StrokeCap.Round),
    )
}

/** Ngọc đỏ cho quân X: hai thanh chéo tròn đầu có chiều dày, viền tối, vệt sáng. [r] = nửa kích thước quân. */
internal fun DrawScope.drawGlassX(c: Offset, r: Float) {
    val w = r * .62f
    val bars = listOf(
        (c + Offset(-r, -r)) to (c + Offset(r, r)),
        (c + Offset(r, -r)) to (c + Offset(-r, r)),
    )
    val sh = Offset(r * .12f, r * .2f)
    for ((p, q) in bars) drawLine(Color.Black.copy(alpha = .22f), p + sh, q + sh, w, cap = StrokeCap.Round)
    for ((p, q) in bars) drawLine(GlassPalette.rubyLo, p, q, w * 1.22f, cap = StrokeCap.Round)
    val body = Brush.linearGradient(listOf(GlassPalette.rubyHi, GlassPalette.ruby, GlassPalette.rubyLo), c - Offset(r, r), c + Offset(r, r))
    for ((p, q) in bars) drawLine(body, p, q, w, cap = StrokeCap.Round)
    val lift = Offset(-w * .15f, -w * .18f)
    for ((p, q) in bars) drawLine(Color.White.copy(alpha = .55f), p + (q - p) * .08f + lift, p + (q - p) * .46f + lift, w * .2f, cap = StrokeCap.Round)
}

/** Vòng thủy tinh đen khói cho quân O: ống tròn có chiều dày, viền tối, vệt phản chiếu. [r] = bán kính ngoài. */
internal fun DrawScope.drawGlassO(c: Offset, r: Float) {
    val w = r * .52f
    val rr = r - w * .5f
    drawCircle(Color.Black.copy(alpha = .22f), rr, c + Offset(r * .1f, r * .18f), style = Stroke(w))
    drawCircle(GlassPalette.obsLo, rr, c, style = Stroke(w * 1.2f))
    drawCircle(
        Brush.linearGradient(listOf(GlassPalette.obsHi, GlassPalette.obs, GlassPalette.obsLo), c - Offset(r, r), c + Offset(r, r)),
        rr, c, style = Stroke(w),
    )
    val hr = rr - w * .1f
    drawArc(
        Color.White.copy(alpha = .6f), 195f, 80f, false,
        topLeft = c - Offset(hr, hr), size = Size(hr * 2f, hr * 2f),
        style = Stroke(w * .22f, cap = StrokeCap.Round),
    )
    drawArc(
        Color.White.copy(alpha = .22f), 15f, 50f, false,
        topLeft = c - Offset(hr, hr), size = Size(hr * 2f, hr * 2f),
        style = Stroke(w * .14f, cap = StrokeCap.Round),
    )
}

/** Lớp phản chiếu chéo phủ lên cả tấm bàn để tạo cảm giác một tấm kính. */
internal fun DrawScope.drawGlassSheen(dark: Boolean) {
    drawRect(
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = if (dark) .16f else .34f), Color.Transparent, Color.White.copy(alpha = if (dark) .06f else .12f)),
            Offset.Zero, Offset(size.width, size.height),
        ),
    )
}
