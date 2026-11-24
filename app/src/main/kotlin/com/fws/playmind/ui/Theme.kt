package com.fws.playmind.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Bảng màu "Ngà & Vàng Hoàng gia": nền ngà ấm, mực xanh hải quân (cùng tông với icon), điểm nhấn vàng kim.
 * App chỉ có giao diện sáng, không có chế độ tối.
 */
data class Pm(
    val bg: Color, val bgDeep: Color, val surface: Color, val surfaceHigh: Color,
    val ink: Color, val inkSoft: Color,
    /** Vàng đậm cho chữ/biểu tượng (đủ tương phản trên nền ngà). */
    val gold: Color,
    /** Vàng sáng cho mảng màu, đường viền, hiệu ứng. */
    val goldBright: Color, val goldHi: Color, val goldLo: Color, val goldSoft: Color, val outline: Color,
    val navy: Color, val navy2: Color, val royal: Color,
    val danger: Color, val good: Color,
)

val PmLight = Pm(
    bg = Color(0xFFFBF8F2), bgDeep = Color(0xFFF1E9D8), surface = Color(0xFFFFFFFF), surfaceHigh = Color(0xFFF7F1E5),
    ink = Color(0xFF121734), inkSoft = Color(0xFF5A5F7A),
    gold = Color(0xFF8F6610), goldBright = Color(0xFFD9A93A), goldHi = Color(0xFFF6DE8E), goldLo = Color(0xFF8C6414),
    goldSoft = Color(0xFFF9EFD2), outline = Color(0xFFE6DABF),
    navy = Color(0xFF0E1330), navy2 = Color(0xFF1D2858), royal = Color(0xFF4A86FF),
    danger = Color(0xFFC0392B), good = Color(0xFF14937A),
)

val LocalPm = staticCompositionLocalOf { PmLight }

private fun scheme(p: Pm): ColorScheme = lightColorScheme(
    primary = p.gold, onPrimary = Color.White, secondary = p.inkSoft, background = p.bg, onBackground = p.ink,
    surface = p.surface, onSurface = p.ink, surfaceVariant = p.surfaceHigh, onSurfaceVariant = p.inkSoft, outline = p.outline, error = p.danger,
)

private val Display = FontFamily.Serif
private val pmTypography = Typography(
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.3).sp),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.2.sp),
)

@Composable
fun PlayMindTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPm provides PmLight) {
        MaterialTheme(colorScheme = scheme(PmLight), typography = pmTypography, content = content)
    }
}

val pm: Pm @Composable get() = LocalPm.current
