package com.fws.playmind.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Bảng màu "Sơn mài & lá vàng": nền tím-chàm sâu / ngọc trai, điểm nhấn vàng cổ. */
data class Pm(
    val dark: Boolean,
    val bg: Color, val surface: Color, val surfaceHigh: Color,
    val ink: Color, val inkSoft: Color,
    val gold: Color, val goldSoft: Color, val outline: Color,
    val boardLight: Color, val boardDark: Color, val danger: Color, val good: Color,
)

val PmLight = Pm(false, Color(0xFFF5F3FA), Color(0xFFFFFFFF), Color(0xFFECE8F5), Color(0xFF16162F), Color(0xFF5C5C7A),
    Color(0xFF9C6D12), Color(0xFFF6E8C3), Color(0xFFDAD5E8), Color(0xFFEFE4CC), Color(0xFFB08F5B), Color(0xFFC0392B), Color(0xFF14937A))
val PmDark = Pm(true, Color(0xFF0A0D1D), Color(0xFF141832), Color(0xFF1E2347), Color(0xFFF2EFFA), Color(0xFFA4A7CB),
    Color(0xFFE8C56E), Color(0xFF3A3354), Color(0xFF2C3158), Color(0xFF8E97BC), Color(0xFF4C5584), Color(0xFFFF6B5E), Color(0xFF3FD0B0))

val LocalPm = staticCompositionLocalOf { PmLight }

private fun scheme(p: Pm): ColorScheme = if (p.dark) darkColorScheme(
    primary = p.gold, onPrimary = Color(0xFF241A04), secondary = p.inkSoft, background = p.bg, onBackground = p.ink,
    surface = p.surface, onSurface = p.ink, surfaceVariant = p.surfaceHigh, onSurfaceVariant = p.inkSoft, outline = p.outline, error = p.danger,
) else lightColorScheme(
    primary = p.gold, onPrimary = Color.White, secondary = p.inkSoft, background = p.bg, onBackground = p.ink,
    surface = p.surface, onSurface = p.ink, surfaceVariant = p.surfaceHigh, onSurfaceVariant = p.inkSoft, outline = p.outline, error = p.danger,
)

private val Display = FontFamily.Serif
private val pmTypography = Typography(
    displaySmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
)

/** [mode]: 0 = theo hệ thống, 1 = sáng, 2 = tối */
@Composable
fun PlayMindTheme(mode: Int, content: @Composable () -> Unit) {
    val dark = when (mode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val pm = if (dark) PmDark else PmLight
    CompositionLocalProvider(LocalPm provides pm) {
        MaterialTheme(colorScheme = scheme(pm), typography = pmTypography, content = content)
    }
}

val pm: Pm @Composable get() = LocalPm.current
