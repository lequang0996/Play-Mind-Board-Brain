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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fws.playmind.R

/**
 * Bảng màu "Pearl & Champagne" (giao diện sáng): nền ngọc trai xanh nhạt, thẻ trắng nổi bằng bóng đổ mềm,
 * chữ xanh navy đậm, vàng champagne dùng cho hành động chính + kim cương (cùng tông với icon app).
 * Một vài thẻ nhấn (két kim cương, gói năm) dùng bộ màu "deep" xanh navy đậm để tạo điểm nhấn sang trọng.
 * Tên trường giữ nguyên từ bản cũ để các màn hình khác dùng chung:
 * ink = màu chữ chính (đậm), inkSoft = chữ phụ, surface = mặt thẻ, navy = màu chữ đặt TRÊN nền vàng.
 */
data class Pm(
    val bg: Color, val bgDeep: Color, val surface: Color, val surfaceHigh: Color,
    val ink: Color, val inkSoft: Color,
    /** Vàng dùng cho chữ/biểu tượng nhấn trên nền sáng (đủ tương phản). */
    val gold: Color,
    /** Vàng sáng cho mảng màu, đường viền, hiệu ứng. */
    val goldBright: Color, val goldHi: Color, val goldLo: Color, val goldSoft: Color, val outline: Color,
    val navy: Color, val navy2: Color, val royal: Color,
    val danger: Color, val good: Color,
    /** Bộ màu tối dành cho thẻ nhấn: nền, nền nổi, chữ chính, chữ phụ trên nền tối. */
    val deep: Color, val deepHigh: Color, val onDeep: Color, val onDeepSoft: Color,
)

val PmLight = Pm(
    bg = Color(0xFFF4F5FA), bgDeep = Color(0xFFE7EAF4), surface = Color(0xFFFFFFFF), surfaceHigh = Color(0xFFF6F7FC),
    ink = Color(0xFF141A33), inkSoft = Color(0xFF5E6684),
    gold = Color(0xFF8F6210), goldBright = Color(0xFFD4A237), goldHi = Color(0xFFF3DC95), goldLo = Color(0xFFA97C1F),
    goldSoft = Color(0xFFFBF2D9), outline = Color(0xFFE2E5F0),
    navy = Color(0xFF1A1630), navy2 = Color(0xFF1B2350), royal = Color(0xFF3F5BEF),
    danger = Color(0xFFD93C3C), good = Color(0xFF139A66),
    deep = Color(0xFF121733), deepHigh = Color(0xFF222A5A), onDeep = Color(0xFFF6F1E4), onDeepSoft = Color(0xFFB7BCD8),
)

val LocalPm = staticCompositionLocalOf { PmLight }

private fun scheme(p: Pm): ColorScheme = lightColorScheme(
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
    CompositionLocalProvider(LocalPm provides PmLight) {
        MaterialTheme(colorScheme = scheme(PmLight), typography = pmTypography, content = content)
    }
}

val pm: Pm @Composable get() = LocalPm.current
