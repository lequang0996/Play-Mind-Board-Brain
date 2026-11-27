package com.fws.playmind.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fws.playmind.R

/**
 * Bảng màu "Midnight Lacquer": nền xanh mực gần đen như mặt sơn mài, các mặt phẳng nổi lên bằng sắc xanh tím nhạt,
 * đường viền mảnh, và vàng đồng chỉ dùng cho hành động chính + kim cương (cùng tông với icon app).
 * App chỉ có giao diện tối. Tên trường giữ nguyên từ bản cũ để các màn hình khác dùng chung:
 * ink = màu chữ chính (sáng), inkSoft = chữ phụ, surface = mặt thẻ, navy = màu chữ đặt TRÊN nền vàng.
 */
data class Pm(
    val bg: Color, val bgDeep: Color, val surface: Color, val surfaceHigh: Color,
    val ink: Color, val inkSoft: Color,
    /** Vàng dùng cho chữ/biểu tượng nhấn trên nền tối. */
    val gold: Color,
    /** Vàng sáng cho mảng màu, đường viền, hiệu ứng. */
    val goldBright: Color, val goldHi: Color, val goldLo: Color, val goldSoft: Color, val outline: Color,
    val navy: Color, val navy2: Color, val royal: Color,
    val danger: Color, val good: Color,
)

val PmDark = Pm(
    bg = Color(0xFF0B0E1A), bgDeep = Color(0xFF070914), surface = Color(0xFF141935), surfaceHigh = Color(0xFF1C2347),
    ink = Color(0xFFF3EFE6), inkSoft = Color(0xFFA3A8C4),
    gold = Color(0xFFE2B857), goldBright = Color(0xFFE2B857), goldHi = Color(0xFFF7E3A1), goldLo = Color(0xFFA9812A),
    goldSoft = Color(0xFF2A2742), outline = Color(0xFF2A3157),
    navy = Color(0xFF0B0E1A), navy2 = Color(0xFF1B2350), royal = Color(0xFF6C8CFF),
    danger = Color(0xFFFF6B6B), good = Color(0xFF3DDC97),
)

val LocalPm = staticCompositionLocalOf { PmDark }

private fun scheme(p: Pm): ColorScheme = darkColorScheme(
    primary = p.gold, onPrimary = p.navy, secondary = p.inkSoft, background = p.bg, onBackground = p.ink,
    surface = p.surface, onSurface = p.ink, surfaceVariant = p.surfaceHigh, onSurfaceVariant = p.inkSoft, outline = p.outline, error = p.danger,
)

/** Phông thân bài: Manrope (hỗ trợ Latin, tiếng Việt, Cyrillic). Chữ Hán/Nhật/Hàn tự rơi về phông hệ thống. */
val BodyFont = FontFamily(
    Font(R.font.manrope_medium, FontWeight.Normal), Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold), Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

/** Phông tiêu đề / số lớn: Unbounded (rộng, hình học, cảm giác "game"). Chỉ một độ đậm nên mọi weight đều dùng bản Bold. */
val DisplayFont = FontFamily(
    Font(R.font.unbounded_bold, FontWeight.Normal), Font(R.font.unbounded_bold, FontWeight.Medium),
    Font(R.font.unbounded_bold, FontWeight.SemiBold), Font(R.font.unbounded_bold, FontWeight.Bold),
)

private val pmTypography = Typography(
    displayLarge = TextStyle(fontFamily = DisplayFont, fontSize = 38.sp, lineHeight = 44.sp),
    displayMedium = TextStyle(fontFamily = DisplayFont, fontSize = 32.sp, lineHeight = 38.sp),
    displaySmall = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.4).sp),
    headlineLarge = TextStyle(fontFamily = DisplayFont, fontSize = 26.sp, lineHeight = 32.sp),
    headlineMedium = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 25.sp),
    titleLarge = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleMedium = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = BodyFont, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = BodyFont, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = BodyFont, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.1.sp),
    labelSmall = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.1.sp),
)

@Composable
fun PlayMindTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPm provides PmDark) {
        MaterialTheme(colorScheme = scheme(PmDark), typography = pmTypography, content = content)
    }
}

val pm: Pm @Composable get() = LocalPm.current
