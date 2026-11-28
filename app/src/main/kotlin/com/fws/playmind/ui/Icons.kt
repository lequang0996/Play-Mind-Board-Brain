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

    /**
     * Trình đọc path của Compose không hiểu cờ cung viết liền ("a2 2 0 014 0" = cờ 0, 1 rồi x=4): khi đó cả icon bị vỡ thành
     * một chấm nhỏ. Hàm này tách cờ của lệnh cung (a/A) ra bằng khoảng trắng trước khi đọc.
     */
    internal fun expandArcs(d: String): String {
        val out = StringBuilder(); var i = 0; var cmd = ' '
        fun skip() { while (i < d.length && (d[i] == ' ' || d[i] == ',')) i++ }
        fun num(): String {
            skip(); val st = i
            if (i < d.length && (d[i] == '-' || d[i] == '+')) i++
            var dot = false
            while (i < d.length) {
                val c = d[i]
                if (c.isDigit()) i++
                else if (c == '.' && !dot) { dot = true; i++ }
                else if ((c == 'e' || c == 'E') && i + 1 < d.length && (d[i + 1].isDigit() || d[i + 1] == '-')) { i += 2 }
                else break
            }
            return d.substring(st, i)
        }
        fun flag(): String { skip(); return if (i < d.length) d[i++].toString() else "0" }
        while (true) {
            skip(); if (i >= d.length) break
            val c = d[i]
            if (c.isLetter()) { cmd = c; out.append(c).append(' '); i++; continue }
            val before = i
            if (cmd == 'a' || cmd == 'A') out.append("${num()} ${num()} ${num()} ${flag()} ${flag()} ${num()} ${num()} ")
            else out.append(num()).append(' ')
            if (i == before) i++                     // ký tự lạ: bỏ qua để không lặp vô hạn
        }
        return out.toString().trim()
    }

    private fun build(name: String, filled: Boolean, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            for (d in paths) addPath(
                PathParser().parsePathString(expandArcs(d)).toNodes(),
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = if (filled) null else SolidColor(Color.Black),
                strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            )
        }.build()

    val Gem = build("gem", true, "M7 3h10l5 6.5L12 21 2 9.5z")
    val GemFacets = build("gem_facets", false, "M2 9.5h20M9 3l-2 6.5L12 21M15 3l2 6.5L12 21")
    val Sun = build("sun", false, circle(12f, 12f, 4f), "M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4")
    val Moon = build("moon", false, "M20 14.5A8 8 0 019.5 4a8 8 0 1010.5 10.5z")
    val Sliders = build("sliders", false, "M4 7h9M17 7h3M4 17h3M11 17h9", circle(15f, 7f, 2f), circle(9f, 17f, 2f))
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
    val Plus = build("plus", false, "M12 5v14M5 12h14")
    val Minus = build("minus", false, "M5 12h14")
    val Target = build("target", false, circle(12f, 12f, 6.5f), "M12 2.5v4M12 17.5v4M2.5 12h4M17.5 12h4M12 12v.01")
    val Video = build("video", false, "M4 6.5h11v11H4zM15 10.5l5-2.5v8l-5-2.5z")

    // Biểu tượng 4 chủ đề
    val Puzzle = build("puzzle", false, "M10 4.5a2 2 0 014 0V6h3a1 1 0 011 1v3h-1.5a2 2 0 000 4H18v3a1 1 0 01-1 1h-3v-1.5a2 2 0 00-4 0V18H7a1 1 0 01-1-1v-3h1.5a2 2 0 000-4H6V7a1 1 0 011-1h3z")
    val Crown = build("crown", false, "M4 18.5L3 8.5l5 4 4-7 4 7 5-4-1 10zM4.5 21.5h15")
    val Calculator = build("calculator", false, "M7.5 3.5h9a2 2 0 012 2v13a2 2 0 01-2 2h-9a2 2 0 01-2-2v-13a2 2 0 012-2z", "M8.5 6.5h7v3h-7z", "M9 13h.01M12 13h.01M15 13h.01M9 16.5h.01M12 16.5h.01M15 16.5h.01")
    val Cards = build("cards", false, "M5.5 7h7.5a1.5 1.5 0 011.5 1.5v10a1.5 1.5 0 01-1.5 1.5H5.5A1.5 1.5 0 014 18.5v-10A1.5 1.5 0 015.5 7z", "M9 5.2l.4-1a1.5 1.5 0 011.9-.9l7.1 2.6a1.5 1.5 0 01.9 1.9l-3.3 9", "M9.3 11.6c-.5-.8-2-.6-2 .6 0 1 2 2.2 2 2.2s2-1.2 2-2.2c0-1.2-1.5-1.4-2-.6z")

    // Biểu tượng bổ sung cho giao diện mới
    val Book = build("book", false, "M12 6.8C10.6 5.5 8.3 4.8 4.5 4.8v12.6c3.8 0 6.1.7 7.5 2 1.4-1.3 3.7-2 7.5-2V4.8c-3.8 0-6.1.7-7.5 2z", "M12 6.8v12.6")
    val Bag = build("bag", false, "M6.2 8.5h11.6a1 1 0 011 1.1l-.8 9.1a2 2 0 01-2 1.8H8a2 2 0 01-2-1.8l-.8-9.1a1 1 0 011-1.1z", "M9 10.5V7a3 3 0 016 0v3.5")
    val Undo = build("undo", false, "M9 14L4 9l5-5M4 9h10a6 6 0 010 12h-3")
    val Eraser = build("eraser", false, "M7 20h12M5.5 15.5l9-9a2 2 0 013 0l1.5 1.5a2 2 0 010 3l-7 7H8.5z")
    val Pencil = build("pencil", false, "M4 20l1-4L16.5 4.5a2 2 0 013 3L8 19zM14 7l3 3")
    val Bulb = build("bulb", false, "M9 18h6M10 21h4M12 3a6 6 0 00-3.5 10.9c.7.6 1 1.3 1 2.1h5c0-.8.3-1.5 1-2.1A6 6 0 0012 3z")
    val Users = build("users", false, circle(9f, 8f, 3f), "M3 20c.6-3.4 3-5 6-5s5.4 1.6 6 5M16 5.5a3 3 0 010 5.5M18 15.2c1.8.7 2.8 2.2 3 4.8")
    val Trophy = build("trophy", false, "M8 4h8v5a4 4 0 01-8 0zM8 6H5v1.5A3 3 0 008 10.5M16 6h3v1.5a3 3 0 01-3 3M12 13v4M8.5 20h7M10 17h4")
    val Lock = build("lock", false, "M6.5 11h11a1 1 0 0 1 1 1v7a1 1 0 0 1-1 1h-11a1 1 0 0 1-1-1v-7a1 1 0 0 1 1-1zM8.5 11V8a3.5 3.5 0 0 1 7 0v3M12 14.5v2")
    val Unlock = build("unlock", false, "M6.5 11h11a1 1 0 0 1 1 1v7a1 1 0 0 1-1 1h-11a1 1 0 0 1-1-1v-7a1 1 0 0 1 1-1zM8.5 11V8a3.5 3.5 0 0 1 6.9-.8M12 14.5v2")
    val Warn = build("warn", false, "M12 3.5l9.5 16.5h-19zM12 10v4.5M12 17.2v.01")
    val Skip = build("skip", false, "M6 5v14l9-7zM18 5v14")
    val Brick = build("brick", false, "M3.5 7h17v10h-17zM3.5 12h17M9 7v5M15 12v5")
    val Search = build("search", false, circle(10.5f, 10.5f, 6f), "M15 15l5 5")
    val Heart = build("heart", true, "M12 20s-7.5-4.6-7.5-10A4.2 4.2 0 0112 7.2 4.2 4.2 0 0119.5 10c0 5.4-7.5 10-7.5 10z")
    val Crosshair = build("crosshair", false, "M12 4v16M4 12h16")
    val Music = build("music", false, "M9 18V5.5l10-2V16", circle(6.5f, 18f, 2.5f), circle(16.5f, 16f, 2.5f))
    val Home = build("home", false, "M4 11l8-6.5 8 6.5", "M6 9.5V19a1 1 0 001 1h3.5v-5h3v5H17a1 1 0 001-1V9.5")
    val Menu = build("menu", false, "M4.5 7h15M4.5 12h15M4.5 17h9")
    /** Hai điện thoại kết nối trực tiếp (Bluetooth / Wi-Fi) - chơi gần nhau. */
    val Nearby = build("nearby", false, "M3.5 6.5h4a1 1 0 011 1v9a1 1 0 01-1 1h-4a1 1 0 01-1-1v-9a1 1 0 011-1zM16.5 6.5h4a1 1 0 011 1v9a1 1 0 01-1 1h-4a1 1 0 01-1-1v-9a1 1 0 011-1z", "M10.6 10.2a2.6 2.6 0 000 3.6M13.4 10.2a2.6 2.6 0 010 3.6", "M5.5 15h.01M18.5 15h.01")
    /** Quả địa cầu có mạng lưới - chơi qua internet. */
    val Internet = build("internet", false, circle(12f, 12f, 8.5f), "M3.5 12h17M12 3.5c2.6 2.4 2.6 14.6 0 17M12 3.5c-2.6 2.4-2.6 14.6 0 17M5 7.5h14M5 16.5h14")
    val Apps = build("apps", false, "M5.5 4h3A1.5 1.5 0 0110 5.5v3A1.5 1.5 0 018.5 10h-3A1.5 1.5 0 014 8.5v-3A1.5 1.5 0 015.5 4zM15.5 4h3A1.5 1.5 0 0120 5.5v3a1.5 1.5 0 01-1.5 1.5h-3A1.5 1.5 0 0114 8.5v-3A1.5 1.5 0 0115.5 4zM5.5 14h3a1.5 1.5 0 011.5 1.5v3A1.5 1.5 0 018.5 20h-3A1.5 1.5 0 014 18.5v-3A1.5 1.5 0 015.5 14z", circle(17f, 17f, 3f))
}
