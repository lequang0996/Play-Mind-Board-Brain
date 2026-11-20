package com.fws.playmind.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Bộ icon tự vẽ (24x24, nét 1.8). Chỉ gồm những icon app thực sự dùng, không nhúng thư viện icon ngoài.
 */
object Ic {
    private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

    private fun build(name: String, filled: Boolean, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            for (d in paths) addPath(
                PathParser().parsePathString(d).toNodes(),
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = if (filled) null else SolidColor(Color.Black),
                strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            )
        }.build()

    val Gem = build("gem", true, "M7 3h10l5 6.5L12 21 2 9.5z")
    val GemFacets = build("gem_facets", false, "M2 9.5h20M9 3l-2 6.5L12 21M15 3l2 6.5L12 21")
    val Sun = build("sun", false, circle(12f, 12f, 4f), "M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4")
    val Moon = build("moon", false, "M20 14.5A8 8 0 019.5 4a8 8 0 1010.5 10.5z")
    val Sliders = build("sliders", false, "M4 6h16M4 12h16M4 18h16", circle(9f, 6f, 2f), circle(15f, 12f, 2f), circle(7f, 18f, 2f))
    val Help = build("help", false, circle(12f, 12f, 9f), "M9.6 9.6a2.5 2.5 0 114.2 1.8c-.9.8-1.8 1.3-1.8 2.6M12 17.2v.01")
    val Back = build("back", false, "M15 5l-7 7 7 7")
    val Close = build("close", false, "M6 6l12 12M18 6L6 18")
    val Chevron = build("chevron", false, "M9 5l7 7-7 7")
    val Globe = build("globe", false, circle(12f, 12f, 9f), "M3 12h18M12 3c3.2 3 3.2 15 0 18M12 3c-3.2 3-3.2 15 0 18")
    val Check = build("check", false, "M5 12.5l4.5 4.5L19 7")
    val Calendar = build("calendar", false, "M5 5.5h14v14.5H5zM5 10.5h14M9 3.5v4M15 3.5v4")
    val Flag = build("flag", false, "M6 21V4M6 5h11l-2.5 4L17 13H6")
    val Refresh = build("refresh", false, "M20 12a8 8 0 11-2.4-5.7M20 4v5h-5")
    val Copy = build("copy", false, "M9 9h11v11H9zM5 15V4h11")
    val Qr = build("qr", false, "M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h2v2h-2zM18 18h2v2h-2zM18 14h2M14 18h2")
    val Scan = build("scan", false, "M4 8V5.5A1.5 1.5 0 015.5 4H8M16 4h2.5A1.5 1.5 0 0120 5.5V8M20 16v2.5a1.5 1.5 0 01-1.5 1.5H16M8 20H5.5A1.5 1.5 0 014 18.5V16M4 12h16")
    val Computer = build("computer", false, "M5 8.5h14v10H5zM12 4.5v4M9 12.5v.01M15 12.5v.01M9.5 15.5h5")
    val Wifi = build("wifi", false, "M2.5 9a14 14 0 0119 0M5.5 12.5a9.5 9.5 0 0113 0M8.7 16a5 5 0 016.6 0M12 19.5v.01")
    val Radar = build("radar", false, "M12 12v.01M8.5 8.5a5 5 0 000 7M15.5 8.5a5 5 0 010 7M5.5 5.5a9 9 0 000 13M18.5 5.5a9 9 0 010 13")
    val Play = build("play", true, "M7 4.5l12 7.5-12 7.5z")
    val Flame = build("flame", false, "M12 3c.8 3.8 5 5.2 5 10a5 5 0 01-10 0c0-2 .9-3.4 2.2-4.4.1 1.6.8 2.6 1.8 2.9C10.5 8.4 10.5 5.6 12 3z")
    val Person = build("person", false, circle(12f, 8f, 3.5f), "M5 20c.8-4 3.5-6 7-6s6.2 2 7 6")
    val Hourglass = build("hourglass", false, "M7 3.5h10M7 20.5h10M8 3.5c0 5 8 5 8 8.5s-8 3.5-8 8.5M16 3.5c0 5-8 5-8 8.5")
    val Info = build("info", false, circle(12f, 12f, 9f), "M12 11v5.5M12 7.8v.01")
    val Shield = build("shield", false, "M12 3l7.5 3v5.5c0 4.5-3 7.8-7.5 9.5-4.5-1.7-7.5-5-7.5-9.5V6z")
    val Video = build("video", false, "M4 6.5h11v11H4zM15 10.5l5-2.5v8l-5-2.5z")

    // Biểu tượng 4 chủ đề
    val Puzzle = build("puzzle", false, "M4.5 9.5h3.2a2.3 2.3 0 114.6 0h3.2v3.2a2.3 2.3 0 110 4.6v3.2H4.5z")
    val Crown = build("crown", false, "M4 18.5L3 8.5l5 4 4-7 4 7 5-4-1 10zM4.5 21.5h15")
    val Calculator = build("calculator", false, "M6.5 3.5h11v17h-11zM9 7.5h6M9 12v.01M12 12v.01M15 12v.01M9 16v.01M12 16v.01M15 16v.01")
    val Cards = build("cards", false, "M3.5 8h10v12.5h-10zM9.5 4.5h10.5v12.5")
}
