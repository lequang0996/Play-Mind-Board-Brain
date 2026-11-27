package com.fws.playmind.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fws.playmind.catalog.Topic
import kotlin.math.min

fun Topic.icon(): ImageVector = when (this) {
    Topic.PUZZLE -> Ic.Puzzle
    Topic.MIND -> Ic.Crown
    Topic.MATH -> Ic.Calculator
    Topic.MEMORY -> Ic.Cards
}

@Composable
fun Topic.tint(): Color = color.onLight()

/** Làm đậm một màu nhận diện để dùng làm chữ/biểu tượng trên nền sáng mà vẫn đủ tương phản. */
@Composable
fun Color.onLight(amount: Float = .38f): Color = lerp(this, pm.ink, amount)

/**
 * Bóng đổ mềm để thẻ "nổi" khỏi nền sáng: hai lớp bóng tông xanh mực (không dùng đen thuần).
 * Gọi TRƯỚC clip/background để bóng không bị cắt.
 */
fun Modifier.lift(shape: Shape, elevation: Dp = 10.dp, tint: Color = Color(0xFF1B2350)): Modifier =
    this.shadow(elevation, shape, clip = false, ambientColor = tint.copy(alpha = .10f), spotColor = tint.copy(alpha = .24f))

/** Gradient vàng champagne: sáng - vàng - đậm, chạy chéo theo kích thước của chính thành phần dùng nó. */
@Composable
fun goldBrush(): Brush = Brush.linearGradient(listOf(pm.goldHi, pm.goldBright, pm.goldLo))

/** Viên kim cương nhỏ vẽ bằng Canvas (có chiều sâu), dùng cho chip số dư. */
@Composable
fun GemMark(size: Dp = 18.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        val p = Path().apply {
            moveTo(w * .28f, h * .1f); lineTo(w * .72f, h * .1f); lineTo(w * .96f, h * .4f); lineTo(w * .5f, h * .94f); lineTo(w * .04f, h * .4f); close()
        }
        drawPath(p, Brush.linearGradient(listOf(Color(0xFF8FE9FF), Color(0xFF5B7CFF), Color(0xFFB07BFF)), Offset(0f, 0f), Offset(w, h)))
        val l = Path().apply {
            moveTo(w * .04f, h * .4f); lineTo(w * .96f, h * .4f)
            moveTo(w * .36f, h * .1f); lineTo(w * .26f, h * .4f); lineTo(w * .5f, h * .94f)
            moveTo(w * .64f, h * .1f); lineTo(w * .74f, h * .4f); lineTo(w * .5f, h * .94f)
        }
        drawPath(l, Color.White.copy(alpha = .75f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * .05f))
    }
}

/** Chip số dư kim cương: viên ngọc + con số trên nền trắng, viền vàng mảnh. */
@Composable
fun DiamondChip(count: Int, modifier: Modifier = Modifier, onDark: Boolean = false) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier.lift(shape, 6.dp).clip(shape).background(if (onDark) pm.deepHigh else pm.surface)
            .border(1.dp, pm.goldBright.copy(alpha = .6f), shape)
            .padding(start = 10.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GemMark(20.dp)
        Text(count.toString(), style = MaterialTheme.typography.titleLarge, color = if (onDark) pm.onDeep else pm.ink)
    }
}

/** Viên thuốc nhỏ: nhãn ngắn (miễn phí, 1 kim cương, mức độ...). Chữ được làm đậm hơn viền để dễ đọc trên nền sáng. */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier, color: Color = pm.goldBright, filled: Boolean = false) {
    val shape = RoundedCornerShape(50)
    Text(
        text, modifier.clip(shape).background(if (filled) color.copy(alpha = .14f) else Color.Transparent)
            .border(1.dp, color.copy(alpha = .50f), shape).padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium, color = color.onLight(),
    )
}

@Composable
fun PmIconButton(icon: ImageVector, desc: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = pm.ink) {
    Box(
        modifier.size(44.dp).lift(CircleShape, 6.dp).clip(CircleShape).background(pm.surface).border(1.dp, pm.outline, CircleShape)
            .clickable(role = Role.Button, onClickLabel = desc, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, Modifier.size(21.dp), tint = tint) }
}

/** Nút tròn nổi (dùng trên bàn cờ): nền trắng mờ, viền vàng mảnh. */
@Composable
fun FloatingRoundButton(icon: ImageVector, desc: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(46.dp).lift(CircleShape, 6.dp).clip(CircleShape).background(pm.surface.copy(alpha = .94f)).border(1.dp, pm.goldBright.copy(alpha = .7f), CircleShape)
            .clickable(role = Role.Button, onClickLabel = desc, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, Modifier.size(22.dp), tint = pm.ink) }
}

/** Nút chính: vàng champagne, chữ navy đậm, vệt sáng mảnh phía trên và bóng vàng nhạt bên dưới. */
@Composable
fun GoldButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    val shape = RoundedCornerShape(16.dp)
    val ink = if (enabled) pm.navy else pm.inkSoft
    Row(
        modifier.height(54.dp).then(if (enabled) Modifier.lift(shape, 8.dp, pm.goldLo) else Modifier).clip(shape)
            .background(if (enabled) goldBrush() else Brush.linearGradient(listOf(pm.surfaceHigh, pm.surfaceHigh)))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = if (enabled) .38f else 0f), Color.Transparent)))
            .border(1.dp, if (enabled) pm.goldHi.copy(alpha = .9f) else pm.outline, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(20.dp), tint = ink); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp), color = ink, textAlign = TextAlign.Center)
    }
}

/** Nút phụ: nền trắng, viền vàng mảnh. */
@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.height(50.dp).clip(shape).background(pm.surface)
            .border(1.dp, if (enabled) pm.goldBright.copy(alpha = .75f) else pm.outline, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp), tint = if (enabled) pm.gold else pm.inkSoft); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (enabled) pm.ink else pm.inkSoft, textAlign = TextAlign.Center)
    }
}

/** Mặt thẻ chung: nền trắng nổi nhẹ khỏi nền app bằng bóng mềm, viền mảnh, bo 22dp. */
@Composable
fun SurfaceCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier.lift(shape, 8.dp).clip(shape).background(pm.surface).border(BorderStroke(1.dp, pm.outline), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(16.dp),
    ) { content() }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.titleLarge, color = pm.ink)
}

/** Tiêu đề phụ nhỏ (chữ thường, màu vàng đậm). */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = pm.gold) {
    Text(text, modifier, style = MaterialTheme.typography.labelMedium, color = color)
}

/** Đường phân cách mảnh. */
@Composable
fun OrnamentDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, pm.outline, Color.Transparent))))
}

/** Hàng chọn tùy chọn dạng viên thuốc (số người chơi...). */
@Composable
fun OptionChips(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i, text ->
            val on = i == selected
            val shape = RoundedCornerShape(14.dp)
            Box(
                Modifier.weight(1f).height(46.dp).then(if (on) Modifier.lift(shape, 6.dp, pm.goldLo) else Modifier).clip(shape)
                    .background(if (on) pm.goldSoft else pm.surface)
                    .border(1.2.dp, if (on) pm.goldBright else pm.outline, shape)
                    .clickable(role = Role.RadioButton) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) { Text(text, style = MaterialTheme.typography.labelLarge, color = if (on) pm.gold else pm.inkSoft, textAlign = TextAlign.Center) }
        }
    }
}

/** Sắc thái của hộp thoại: quyết định biểu tượng và màu của huy hiệu phía trên. */
enum class DialogTone { INFO, SUCCESS, WARN, DANGER, GEM }

/**
 * Hộp thoại thương hiệu thay cho AlertDialog: thẻ trắng bo tròn, huy hiệu tròn nhẹ ở trên,
 * tiêu đề + nội dung căn giữa, nút bấm xếp ngang (có nút phụ) hoặc một nút rộng. [body] cho phép chèn nội dung riêng.
 */
@Composable
fun PmDialog(
    title: String, text: String, confirm: String, onConfirm: () -> Unit,
    dismiss: String? = null, onDismiss: () -> Unit = onConfirm,
    tone: DialogTone = DialogTone.INFO, icon: ImageVector? = null,
    body: (@Composable () -> Unit)? = null,
) {
    val accent = when (tone) { DialogTone.INFO -> pm.royal; DialogTone.SUCCESS -> pm.good; DialogTone.WARN -> pm.goldBright; DialogTone.DANGER -> pm.danger; DialogTone.GEM -> pm.goldBright }
    val glyph = icon ?: when (tone) { DialogTone.INFO -> Ic.Info; DialogTone.SUCCESS -> Ic.Check; DialogTone.WARN -> Ic.Warn; DialogTone.DANGER -> Ic.Flag; DialogTone.GEM -> Ic.Gem }
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(28.dp)
        Column(
            Modifier.fillMaxWidth().clip(shape).background(pm.surface).border(1.dp, pm.outline, shape)
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = .14f), Color.Transparent), endY = 380f))
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(60.dp).clip(CircleShape).background(Brush.radialGradient(listOf(accent.copy(alpha = .30f), accent.copy(alpha = .08f))))
                    .border(1.dp, accent.copy(alpha = .6f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(glyph, null, Modifier.size(28.dp), tint = accent.onLight()) }
            Text(title, style = MaterialTheme.typography.headlineSmall, color = pm.ink, textAlign = TextAlign.Center)
            if (text.isNotEmpty()) Text(text, style = MaterialTheme.typography.bodyMedium, color = pm.inkSoft, textAlign = TextAlign.Center)
            if (body != null) body()
            Spacer(Modifier.height(4.dp))
            if (dismiss != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlineButton(dismiss, onDismiss, Modifier.weight(1f))
                    GoldButton(confirm, onConfirm, Modifier.weight(1f))
                }
            } else GoldButton(confirm, onConfirm, Modifier.fillMaxWidth())
        }
    }
}

/** Giữ chữ ký cũ cho mọi nơi đang gọi SimpleDialog: nay hiển thị bằng [PmDialog]. */
@Composable
fun SimpleDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, dismiss: String? = null, onDismiss: () -> Unit = onConfirm, tone: DialogTone = DialogTone.INFO) {
    PmDialog(title, text, confirm, onConfirm, dismiss, onDismiss, tone)
}

@Composable
fun Gap(h: Int) = Spacer(Modifier.fillMaxWidth().height(h.dp))

@Composable
fun RowScope.Grow() = Spacer(Modifier.weight(1f))

/** Quân đại diện một bên (thanh người chơi, hộp thoại...). */
@Composable
fun SideBadge(gameId: String, side: Int, modifier: Modifier = Modifier.size(34.dp)) {
    val tm = rememberTextMeasurer()
    Canvas(modifier) { drawSideBadge(gameId, side, tm, size.width) }
}

/** Tranh minh hoạ một game: vài quân cờ đặc trưng (thẻ nổi bật, ô game, phòng chờ). */
@Composable
fun GameArt(gameId: String, modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()
    Canvas(modifier) {
        val s = min(size.width, size.height)
        val ox = (size.width - s) / 2f; val oy = (size.height - s) / 2f
        when (gameId) {
            "chess" -> {
                drawChessPiece(2, false, Offset(ox + s * .38f, oy + s * .16f), s * .66f)
                drawChessPiece(6, true, Offset(ox + s * -.02f, oy + s * .02f), s * .78f)
            }
            "xiangqi" -> {
                drawXiangqiPiece(Offset(ox + s * .66f, oy + s * .62f), s * .30f, false, 1, tm)
                drawXiangqiPiece(Offset(ox + s * .38f, oy + s * .38f), s * .32f, true, 1, tm)
            }
            "go" -> drawGoArt(ox, oy, s)
            "wallgo" -> drawWallGoArt(ox, oy, s)
            "quoridor" -> drawQuoridorArt(ox, oy, s)
            "sudoku" -> drawSudokuArt(tm, ox, oy, s)
            else -> {
                drawCaroPiece(Offset(ox + s * .66f, oy + s * .64f), s * .62f, false)
                drawCaroPiece(Offset(ox + s * .36f, oy + s * .36f), s * .66f, true)
            }
        }
    }
}
