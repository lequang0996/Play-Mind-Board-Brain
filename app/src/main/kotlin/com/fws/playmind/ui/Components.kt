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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fws.playmind.catalog.Topic

fun Topic.icon(): ImageVector = when (this) {
    Topic.PUZZLE -> Ic.Puzzle
    Topic.MIND -> Ic.Crown
    Topic.MATH -> Ic.Calculator
    Topic.MEMORY -> Ic.Cards
}

@Composable
fun Topic.tint(): Color = if (pm.dark) colorDark else color

@Composable
fun goldBrush(): Brush = Brush.linearGradient(
    listOf(if (pm.dark) Color(0xFFF3D88A) else Color(0xFFD9A93A), if (pm.dark) Color(0xFFC79A3A) else Color(0xFF9C6D12)),
    start = Offset(0f, 0f), end = Offset(300f, 300f),
)

/** Viên kim cương nhỏ vẽ bằng Canvas (có chiều sâu), dùng cho chip số dư. */
@Composable
fun GemMark(size: Dp = 18.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width; val h = this.size.height
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * .28f, h * .1f); lineTo(w * .72f, h * .1f); lineTo(w * .96f, h * .4f); lineTo(w * .5f, h * .94f); lineTo(w * .04f, h * .4f); close()
        }
        drawPath(p, Brush.linearGradient(listOf(Color(0xFF8FE9FF), Color(0xFF5B7CFF), Color(0xFFB07BFF)), Offset(0f, 0f), Offset(w, h)))
        val l = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * .04f, h * .4f); lineTo(w * .96f, h * .4f)
            moveTo(w * .36f, h * .1f); lineTo(w * .26f, h * .4f); lineTo(w * .5f, h * .94f)
            moveTo(w * .64f, h * .1f); lineTo(w * .74f, h * .4f); lineTo(w * .5f, h * .94f)
        }
        drawPath(l, Color.White.copy(alpha = .75f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * .05f))
    }
}

@Composable
fun DiamondChip(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(pm.surfaceHigh).border(1.dp, pm.outline, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GemMark(18.dp)
        Text(count.toString(), style = MaterialTheme.typography.titleMedium, color = pm.ink)
    }
}

@Composable
fun PmIconButton(icon: ImageVector, desc: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = pm.ink) {
    Box(
        modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = desc, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, Modifier.size(22.dp), tint = tint) }
}

/** Nút chính: nền vàng, chữ đậm. */
@Composable
fun GoldButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    Row(
        modifier.height(52.dp).clip(RoundedCornerShape(16.dp))
            .background(if (enabled) goldBrush() else Brush.linearGradient(listOf(pm.outline, pm.outline)))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(20.dp), tint = if (pm.dark) Color(0xFF241A04) else Color.White); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp), color = if (pm.dark) Color(0xFF241A04) else Color.White)
    }
}

/** Nút phụ: viền mảnh. */
@Composable
fun OutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    Row(
        modifier.height(48.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, if (enabled) pm.gold.copy(alpha = .6f) else pm.outline, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp), tint = if (enabled) pm.gold else pm.inkSoft); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (enabled) pm.ink else pm.inkSoft)
    }
}

@Composable
fun SurfaceCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier.clip(shape).background(pm.surface).border(BorderStroke(1.dp, pm.outline), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(16.dp),
    ) { content() }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.titleLarge, color = pm.ink)
}

/** Nhóm nút chọn 1 trong n (dạng viên thuốc). */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.clip(RoundedCornerShape(14.dp)).background(pm.surfaceHigh).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(11.dp))
                    .background(if (on) pm.surface else Color.Transparent)
                    .then(if (on) Modifier.border(1.dp, pm.gold.copy(alpha = .55f), RoundedCornerShape(11.dp)) else Modifier)
                    .clickable(role = Role.RadioButton) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) { Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) pm.ink else pm.inkSoft, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium) }
        }
    }
}

@Composable
fun SimpleDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, dismiss: String? = null, onDismiss: () -> Unit = onConfirm) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = pm.surface, titleContentColor = pm.ink, textContentColor = pm.inkSoft,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton(onConfirm) { Text(confirm, color = pm.gold, fontWeight = FontWeight.Bold) } },
        dismissButton = if (dismiss != null) { { TextButton(onDismiss) { Text(dismiss, color = pm.inkSoft) } } } else null,
    )
}

@Composable
fun Gap(h: Int) = Spacer(Modifier.fillMaxWidth().height(h.dp))
