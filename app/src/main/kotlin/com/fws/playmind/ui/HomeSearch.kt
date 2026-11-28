package com.fws.playmind.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fws.playmind.PlayMindApp
import com.fws.playmind.R
import com.fws.playmind.catalog.Catalog
import com.fws.playmind.catalog.GameInfo
import java.text.Normalizer

/** Bỏ dấu và viết thường để tìm "co vua" ra "Cờ vua". */
internal fun foldText(s: String): String =
    Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").replace('đ', 'd')

/** Ô tìm kiếm game (tên, mô tả, chủ đề, thẻ), không phân biệt dấu. */
@Composable
fun GameSearchBar(query: String, onChange: (String) -> Unit, onClose: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Row(
        Modifier.fillMaxWidth().lift(shape, 6.dp).clip(shape).background(pm.surface).border(1.dp, pm.goldBright.copy(alpha = .6f), shape)
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Ic.Search, null, Modifier.size(20.dp), tint = pm.gold)
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text(stringResource(R.string.search_hint), style = MaterialTheme.typography.bodyMedium, color = pm.inkSoft)
            BasicTextField(
                query, onChange, Modifier.fillMaxWidth().focusRequester(focus), singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = pm.ink), cursorBrush = SolidColor(pm.gold),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
        }
        PmIconButton(Ic.Close, stringResource(R.string.close), onClose, Modifier.size(36.dp))
    }
}

/** Kết quả tìm kiếm: game có tên, mô tả, chủ đề hoặc thẻ khớp mọi từ đã gõ. */
@Composable
fun GameSearchResults(query: String, onOpen: (GameInfo) -> Unit) {
    val ctx = LocalContext.current
    val words = foldText(query).split(Regex("\\s+")).filter { it.isNotBlank() }
    val hits = Catalog.games.filter { g ->
        val hay = foldText(
            listOf(ctx.getString(g.name), ctx.getString(g.desc), ctx.getString(g.topic.title)).joinToString(" ") +
                " " + g.tags.joinToString(" ") { ctx.getString(it.label) } + " " + g.id,
        )
        words.all { it in hay }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.search_results_n, hits.size), style = MaterialTheme.typography.labelLarge, color = pm.inkSoft)
        if (hits.isEmpty()) Text(stringResource(R.string.search_none), style = MaterialTheme.typography.bodyMedium, color = pm.inkSoft)
        for (g in hits) {
            val shape = RoundedCornerShape(18.dp)
            Row(
                Modifier.fillMaxWidth().lift(shape, 6.dp).clip(shape).background(cardBrush()).border(1.dp, hairline(), shape).clickable { onOpen(g) }.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(g.topic.color.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                    GameArt(g.id, Modifier.size(48.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(g.name), style = MaterialTheme.typography.titleMedium, color = pm.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(g.desc), style = MaterialTheme.typography.bodySmall, color = pm.inkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(g.topic.title), style = MaterialTheme.typography.labelSmall, color = g.topic.tint())
                }
                Icon(Ic.Chevron, null, Modifier.size(16.dp), tint = pm.inkSoft)
            }
        }
    }
}

/** Hàng game yêu thích (gói tháng / năm) ở đầu trang chủ. */
@Composable
fun FavoritesRow(app: PlayMindApp, onOpen: (GameInfo) -> Unit) {
    val favs = app.prefs.favorites
    val games = Catalog.games.filter { it.id in favs }
    if (games.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Ic.Heart, null, Modifier.size(18.dp), tint = androidx.compose.ui.graphics.Color(0xFFE5483D))
            SectionTitle(stringResource(R.string.favorites))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (g in games) {
                val shape = RoundedCornerShape(18.dp)
                Column(
                    Modifier.width(96.dp).lift(shape, 6.dp).clip(shape).background(cardBrush()).border(1.dp, hairline(), shape).clickable { onOpen(g) }.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(g.topic.color.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                        GameArt(g.id, Modifier.size(56.dp))
                    }
                    Text(stringResource(g.name), style = MaterialTheme.typography.labelMedium, color = pm.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
