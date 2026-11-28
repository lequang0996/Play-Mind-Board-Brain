package com.fws.playmind.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fws.playmind.PlayMindApp
import com.fws.playmind.R
import com.fws.playmind.catalog.Catalog
import com.fws.playmind.catalog.GameInfo
import com.fws.playmind.catalog.Tag
import com.fws.playmind.catalog.Topic

/**
 * Màn "Xem thêm" của một chủ đề: chỉ hiển thị game của chủ đề đó, xếp theo độ phổ biến tăng dần (game độc lạ lên trước,
 * [GameInfo.popularity]). Hàng thẻ ở trên (nội dung + thời lượng): chọn một thẻ thì game có thẻ đó lên đầu,
 * game còn lại mờ đi ở phía dưới; chạm lại thẻ đang chọn để bỏ chọn.
 */
@Composable
fun TopicScreen(app: PlayMindApp, topic: Topic, onBack: () -> Unit, onOpenGame: (GameInfo) -> Unit) {
    val wallet by app.wallet.state.collectAsState()
    @Suppress("UNUSED_VARIABLE") val keepRecomposing = wallet.diamonds
    var tagName by rememberSaveable(topic) { mutableStateOf<String?>(null) }
    val tag = tagName?.let { n -> Tag.entries.firstOrNull { it.name == n } }
    val games = Catalog.byTopicTagFirst(topic, tag)

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(topic.title), onBack) { if (!app.wallet.noDiamonds()) DiamondChip(wallet.diamonds) }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.padding(bottom = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Ic.Flame, null, Modifier.size(16.dp), tint = pm.gold)
                Text(stringResource(R.string.sorted_hint), style = MaterialTheme.typography.bodySmall, color = pm.inkSoft)
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TagChip(stringResource(R.string.tag_all), tag == null, topic) { tagName = null }
                for (t in Catalog.tagsOf(topic)) TagChip(stringResource(t.label), tag == t, topic) { tagName = if (tag == t) null else t.name }
            }
            games.forEachIndexed { i, g -> TopicRow(app, g, topic, i + 1, tag, onOpenGame) }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TagChip(label: String, selected: Boolean, t: Topic, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Text(
        label,
        Modifier.clip(shape).background(if (selected) t.color.copy(alpha = .9f) else pm.surface)
            .border(1.dp, if (selected) t.color else t.color.copy(alpha = .35f), shape)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 7.dp),
        style = MaterialTheme.typography.labelLarge, color = if (selected) androidx.compose.ui.graphics.Color.White else pm.ink,
    )
}

@Composable
private fun TopicRow(app: PlayMindApp, g: GameInfo, t: Topic, rank: Int, tag: Tag?, onOpen: (GameInfo) -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val src = rememberPress()
    val free = app.wallet.freeLeft(g.id)
    Row(
        Modifier.alpha(if (tag == null || tag in g.tags) 1f else .5f).bounce(src, .98f).fillMaxWidth().lift(shape, 10.dp).clip(shape).background(cardBrush()).border(1.dp, hairline(), shape).tap(src) { onOpen(g) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(width = 104.dp, height = 124.dp)
                .background(Brush.linearGradient(listOf(t.color.copy(alpha = .28f), pm.surfaceHigh), Offset(0f, 0f), Offset(400f, 400f))),
            contentAlignment = Alignment.Center,
        ) {
            GameArt(g.id, Modifier.size(84.dp))
            // Số thứ tự: thứ hạng từ độc lạ đến phổ biến
            Box(
                Modifier.align(Alignment.TopStart).padding(8.dp).size(24.dp).clip(CircleShape).background(pm.surface).border(1.dp, t.color.copy(alpha = .6f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(rank.toString(), style = MaterialTheme.typography.labelMedium, color = t.tint()) }
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(g.name), style = MaterialTheme.typography.titleLarge, color = pm.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(g.desc), style = MaterialTheme.typography.bodySmall, color = pm.inkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
            // thẻ nội dung (thẻ đang chọn tô màu chủ đề)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (x in g.contentTags) Pill(stringResource(x.label), color = if (x == tag) t.tint() else pm.inkSoft, filled = x == tag)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val timeTag = g.tags.last()
                Pill(stringResource(R.string.minutes_n, g.minutes), color = if (timeTag == tag) t.tint() else pm.inkSoft, filled = timeTag == tag)
                if (app.wallet.noDiamonds()) { }
                else if (free > 0) Pill(if (free == Int.MAX_VALUE) "∞" else stringResource(R.string.free_n, free), color = pm.good, filled = true)
                else Pill(stringResource(R.string.cost_one_gem), color = pm.goldBright)
            }
        }
        Icon(Ic.Chevron, null, Modifier.padding(end = 12.dp).size(18.dp), tint = pm.inkSoft)
    }
}
