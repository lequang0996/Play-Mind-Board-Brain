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
import com.fws.playmind.catalog.Topic

/**
 * Màn "Xem thêm" của một chủ đề: chỉ hiển thị game của chủ đề đó, xếp từ game dễ chơi và ván ngắn nhất
 * đến game khó chơi và ván dài nhất (thứ tự khai báo ở [GameInfo.order] / [GameInfo.minutes]).
 */
@Composable
fun TopicScreen(app: PlayMindApp, topic: Topic, onBack: () -> Unit, onOpenGame: (GameInfo) -> Unit) {
    val wallet by app.wallet.state.collectAsState()
    @Suppress("UNUSED_VARIABLE") val keepRecomposing = wallet.diamonds
    val games = Catalog.byTopicEasiestFirst(topic)

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(topic.title), onBack) { DiamondChip(wallet.diamonds) }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.padding(bottom = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Ic.Hourglass, null, Modifier.size(16.dp), tint = pm.gold)
                Text(stringResource(R.string.sorted_hint), style = MaterialTheme.typography.bodySmall, color = pm.inkSoft)
            }
            games.forEachIndexed { i, g -> TopicRow(app, g, topic, i + 1, onOpenGame) }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TopicRow(app: PlayMindApp, g: GameInfo, t: Topic, rank: Int, onOpen: (GameInfo) -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val src = rememberPress()
    val free = app.wallet.freeLeft(g.id)
    Row(
        Modifier.bounce(src, .98f).fillMaxWidth().lift(shape, 10.dp).clip(shape).background(cardBrush()).border(1.dp, hairline(), shape).tap(src) { onOpen(g) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(width = 104.dp, height = 124.dp)
                .background(Brush.linearGradient(listOf(t.color.copy(alpha = .28f), pm.surfaceHigh), Offset(0f, 0f), Offset(400f, 400f))),
            contentAlignment = Alignment.Center,
        ) {
            GameArt(g.id, Modifier.size(84.dp))
            // Số thứ tự thể hiện đúng thứ tự dễ → khó nên có ý nghĩa thông tin
            Box(
                Modifier.align(Alignment.TopStart).padding(8.dp).size(24.dp).clip(CircleShape).background(pm.surface).border(1.dp, t.color.copy(alpha = .6f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(rank.toString(), style = MaterialTheme.typography.labelMedium, color = t.tint()) }
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(g.name), style = MaterialTheme.typography.titleLarge, color = pm.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(g.desc), style = MaterialTheme.typography.bodySmall, color = pm.inkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill(stringResource(R.string.minutes_n, g.minutes), color = pm.inkSoft)
                if (free > 0) Pill(if (free == Int.MAX_VALUE) "∞" else stringResource(R.string.free_n, free), color = pm.good, filled = true)
                else Pill(stringResource(R.string.cost_one_gem), color = pm.goldBright)
            }
        }
        Icon(Ic.Chevron, null, Modifier.padding(end = 12.dp).size(18.dp), tint = pm.inkSoft)
    }
}
