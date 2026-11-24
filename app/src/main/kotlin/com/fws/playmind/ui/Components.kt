package com.fws.playmind.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fws.playmind.catalog.Topic
import kotlin.math.min

fun Topic.icon(): ImageVector = when (this) {
    Topic.PUZZLE -> Ic.Puzzle
    Topic.MIND -> Ic.Crown
    Topic.MATH -> Ic.Calculator
    Topic.MEMORY -> Ic.Cards
}

@Composable
fun Topic.tint(): Color = color

/** Gradient vàng kim: sáng - vàng - đậm, chạy chéo theo kích thước của chính thành phần dùng nó. */
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

@Composable
fun DiamondChip(count: Int, modifier: Modifier = Modifier, onDark: Boolean = false) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier.clip(shape)
            .background(if (onDark) Color.White.copy(alpha = .10f) else pm.goldSoft)
            .border(1.dp, if (onDark) pm.goldBright.copy(alpha = .55f) else pm.goldBright.copy(alpha = .55f), shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GemMark(20.dp)
        Text(count.toString(), style = MaterialTheme.typography.titleLarge, color = if (onDark) Color.White else pm.ink)
    }
}

@Composable
fun PmIconButton(icon: ImageVector, desc: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = pm.ink) {
    Box(
        modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = desc, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, Modifier.size(22.dp), tint = tint) }
}

/** Nút tròn nổi (dùng trên bàn cờ caro): nền ngà, viền vàng mảnh, đổ bóng nhẹ. */
@Composable
fun FloatingRoundButton(icon: ImageVector, desc: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(44.dp)
            .shadow(6.dp, CircleShape, ambientColor = pm.goldLo.copy(alpha = .25f), spotColor = pm.goldLo.copy(alpha = .4f))
            .clip(CircleShape).background(pm.surface).border(1.dp, pm.goldBright.copy(alpha = .6f), CircleShape)
            .clickable(role = Role.Button, onClickLabel = desc, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, Modifier.size(22.dp), tint = pm.ink) }
}

/** Nút chính: vàng kim có vệt sáng phía trên, viền sáng và bóng vàng. */
@Composable
fun GoldButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    val shape = RoundedCornerShape(18.dp)
    val ink = if (enabled) pm.navy else pm.inkSoft
    Row(
        modifier.height(54.dp)
            .shadow(if (enabled) 8.dp else 0.dp, shape, ambientColor = pm.goldLo.copy(alpha = .35f), spotColor = pm.goldLo.copy(alpha = .55f))
            .clip(shape)
            .background(if (enabled) goldBrush() else Brush.linearGradient(listOf(pm.outline, pm.outline)))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = if (enabled) .40f else 0f), Color.Transparent, Color.Black.copy(alpha = if (enabled) .06f else 0f))))
            .border(1.dp, Color.White.copy(alpha = if (enabled) .6f else 0f), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(20.dp), tint = ink); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, letterSpacing = 0.4.sp), color = ink)
    }
}

/** Nút phụ: nền trắng, viền vàng mảnh. */
@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.height(50.dp).clip(shape).background(pm.surface)
            .border(1.2.dp, if (enabled) pm.goldBright.copy(alpha = .75f) else pm.outline, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp), tint = if (enabled) pm.gold else pm.inkSoft); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (enabled) pm.ink else pm.inkSoft)
    }
}

@Composable
fun SurfaceCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier
            .shadow(5.dp, shape, ambientColor = pm.goldLo.copy(alpha = .10f), spotColor = pm.goldLo.copy(alpha = .22f))
            .clip(shape).background(pm.surface).border(BorderStroke(1.dp, pm.outline), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(16.dp),
    ) { content() }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.titleLarge, color = pm.ink)
}

/** Dòng chữ nhỏ viết hoa, giãn chữ, màu vàng - dùng làm tiêu đề phụ sang trọng. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = pm.gold) {
    Text(text.uppercase(), modifier, style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.2.sp, fontWeight = FontWeight.Bold, fontSize = 11.sp), color = color)
}

/** Đường phân cách hoa văn: vạch vàng mờ dần hai phía và viên kim cương nhỏ ở giữa. */
@Composable
fun OrnamentDivider(modifier: Modifier = Modifier) {
    val gold = pm.goldBright
    Canvas(modifier.fillMaxWidth().height(14.dp)) {
        val cy = size.height / 2f; val cx = size.width / 2f; val gap = 16.dp.toPx()
        val lw = 1.dp.toPx()
        drawLine(Brush.horizontalGradient(listOf(Color.Transparent, gold.copy(alpha = .8f)), 0f, cx - gap), Offset(0f, cy), Offset(cx - gap, cy), lw)
        drawLine(Brush.horizontalGradient(listOf(gold.copy(alpha = .8f), Color.Transparent), cx + gap, size.width), Offset(cx + gap, cy), Offset(size.width, cy), lw)
        val d = 4.5.dp.toPx()
        val p = Path().apply { moveTo(cx, cy - d); lineTo(cx + d, cy); lineTo(cx, cy + d); lineTo(cx - d, cy); close() }
        drawPath(p, Brush.linearGradient(listOf(Color(0xFFF6DE8E), Color(0xFFB98A24)), Offset(cx - d, cy - d), Offset(cx + d, cy + d)))
        drawCircle(gold, 1.6.dp.toPx(), Offset(cx - gap + 3.dp.toPx(), cy))
        drawCircle(gold, 1.6.dp.toPx(), Offset(cx + gap - 3.dp.toPx(), cy))
    }
}

@Composable
fun SimpleDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, dismiss: String? = null, onDismiss: () -> Unit = onConfirm) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp), tonalElevation = 0.dp,
        containerColor = pm.surface, titleContentColor = pm.ink, textContentColor = pm.inkSoft,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton(onConfirm) { Text(confirm, color = pm.gold, fontWeight = FontWeight.Bold) } },
        dismissButton = if (dismiss != null) { { TextButton(onDismiss) { Text(dismiss, color = pm.inkSoft) } } } else null,
    )
}

@Composable
fun Gap(h: Int) = Spacer(Modifier.fillMaxWidth().height(h.dp))

/** Quân đại diện một bên (thanh người chơi, hộp thoại...). */
@Composable
fun SideBadge(gameId: String, side: Int, modifier: Modifier = Modifier.size(34.dp)) {
    val tm = rememberTextMeasurer()
    Canvas(modifier) { drawSideBadge(gameId, side, tm, size.width) }
}

/** Tranh minh hoạ một game: vài quân cờ đặc trưng xếp cạnh nhau (thẻ nổi bật, ô game, phòng chờ). */
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
            else -> {
                drawCaroPiece(Offset(ox + s * .66f, oy + s * .64f), s * .62f, false)
                drawCaroPiece(Offset(ox + s * .36f, oy + s * .36f), s * .66f, true)
            }
        }
    }
}
