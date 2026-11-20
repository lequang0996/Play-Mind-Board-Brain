package com.fws.playmind.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fws.playmind.BuildConfig
import com.fws.playmind.PlayMindApp
import com.fws.playmind.R
import com.fws.playmind.catalog.Catalog
import com.fws.playmind.catalog.GameInfo
import com.fws.playmind.catalog.Topic
import com.fws.playmind.data.CheckIn
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.delay
import kotlin.random.Random

/** Game nổi bật của một chủ đề: ngẫu nhiên nhưng cố định trong mỗi khung 5 phút. */
fun featuredGame(t: Topic, slot: Long): GameInfo? {
    val l = Catalog.byTopic(t)
    if (l.isEmpty()) return null
    return l[Random(slot * 7919L + t.ordinal * 104729L).nextInt(l.size)]
}

@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val width = LocalConfiguration.current.screenWidthDp.coerceAtMost(640)
    AndroidView(
        modifier.fillMaxWidth(),
        factory = { c ->
            AdView(c).apply {
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(c, width))
                adUnitId = BuildConfig.ADMOB_BANNER
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { it.destroy() },
    )
}

@Composable
fun HomeScreen(
    app: PlayMindApp, updateReady: Boolean, onRestart: () -> Unit,
    onOpenGame: (GameInfo) -> Unit, onSettings: () -> Unit, onGuide: () -> Unit,
) {
    val wallet by app.wallet.state.collectAsState()
    var slot by remember { mutableLongStateOf(System.currentTimeMillis() / 300_000L) }
    LaunchedEffect(Unit) { while (true) { delay(20_000); slot = System.currentTimeMillis() / 300_000L } }
    var checkIn by remember { mutableStateOf<CheckIn.Done?>(null) }
    val expanded = remember { mutableStateMapOf<Topic, Boolean>() }
    val canCheckIn = wallet.lastCheckInDay != app.wallet.today()

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            // Thanh trên
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Play Mind", style = MaterialTheme.typography.displaySmall, color = pm.ink)
                    Text("Board & Brain", style = MaterialTheme.typography.labelMedium, color = pm.gold)
                }
                PmIconButton(Ic.Help, stringResource(R.string.guide), onGuide)
                PmIconButton(Ic.Sliders, stringResource(R.string.settings), onSettings)
            }
            Gap(14)
            WalletCard(wallet.diamonds, wallet.streakDays, canCheckIn) {
                val r = app.wallet.checkIn(); if (r is CheckIn.Done) checkIn = r
            }
            if (updateReady) {
                Gap(10)
                SurfaceCard(Modifier.fillMaxWidth(), onClick = onRestart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Ic.Refresh, null, tint = pm.gold)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.update_ready), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = pm.ink)
                        Text(stringResource(R.string.restart), style = MaterialTheme.typography.labelLarge, color = pm.gold)
                    }
                }
            }
            Gap(18)
            // Carousel chủ đề
            val pager = rememberPagerState(pageCount = { Topic.entries.size })
            HorizontalPager(pager, Modifier.fillMaxWidth(), pageSpacing = 12.dp, contentPadding = PaddingValues(end = 28.dp)) { i ->
                val t = Topic.entries[i]
                FeaturedCard(t, featuredGame(t, slot), onOpenGame)
            }
            Gap(10)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(Topic.entries.size) { i ->
                    val on = pager.currentPage == i
                    Box(Modifier.padding(3.dp).height(6.dp).width(if (on) 22.dp else 6.dp).clip(CircleShape).background(if (on) pm.gold else pm.outline))
                }
            }
            Gap(22)
            // Bốn phần game
            for (t in Topic.entries) {
                TopicSection(t, expanded[t] == true, { expanded[t] = !(expanded[t] ?: false) }, onOpenGame)
                Gap(18)
            }
            Gap(8)
        }
        if (app.ads.canRequestAds) BannerAd()
    }

    checkIn?.let { d ->
        SimpleDialog(
            stringResource(R.string.check_in_done),
            pluralStringResource(R.plurals.check_in_reward, d.reward, d.reward) + "\n" + pluralStringResource(R.plurals.streak_days, d.streak, d.streak),
            stringResource(R.string.ok), { checkIn = null },
        )
    }
}

@Composable
private fun WalletCard(diamonds: Int, streak: Int, canCheckIn: Boolean, onCheckIn: () -> Unit) {
    SurfaceCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                DiamondChip(diamonds)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Ic.Flame, null, Modifier.size(16.dp), tint = pm.gold)
                    Spacer(Modifier.width(4.dp))
                    Text(pluralStringResource(R.plurals.streak_days, streak, streak), style = MaterialTheme.typography.labelMedium, color = pm.inkSoft)
                }
            }
            if (canCheckIn) GoldButton(stringResource(R.string.check_in), onCheckIn, icon = Ic.Calendar)
            else Row(
                Modifier.height(48.dp).clip(RoundedCornerShape(14.dp)).background(pm.surfaceHigh).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Ic.Check, null, Modifier.size(18.dp), tint = pm.good); Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.checked_in), style = MaterialTheme.typography.labelLarge, color = pm.inkSoft)
            }
        }
    }
}

@Composable
private fun FeaturedCard(t: Topic, game: GameInfo?, onOpen: (GameInfo) -> Unit) {
    val tint = t.tint()
    val shape = RoundedCornerShape(26.dp)
    val dark = pm.dark
    Box(
        Modifier.fillMaxWidth().height(184.dp).clip(shape)
            .background(Brush.linearGradient(
                if (dark) listOf(tint.copy(alpha = .38f), Color(0xFF141832)) else listOf(t.color, t.color.copy(red = t.color.red * .6f, green = t.color.green * .6f, blue = t.color.blue * .75f)),
                Offset.Zero, Offset(900f, 700f)))
            .border(BorderStroke(1.dp, if (dark) tint.copy(alpha = .45f) else Color.White.copy(alpha = .2f)), shape)
            .then(if (game != null) Modifier.clickable { onOpen(game) } else Modifier),
    ) {
        Icon(t.icon(), null, Modifier.align(Alignment.CenterEnd).size(150.dp).padding(end = 6.dp), tint = Color.White.copy(alpha = .13f))
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(stringResource(t.title), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = .85f))
                Spacer(Modifier.height(4.dp))
                Text(
                    if (game != null) stringResource(game.name) else stringResource(R.string.coming_soon),
                    style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (game != null) Text(stringResource(game.desc), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                else Text(stringResource(R.string.coming_soon_desc), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .8f))
            }
            if (game != null) Row(
                Modifier.clip(RoundedCornerShape(50)).background(Color.White).padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Ic.Play, null, Modifier.size(16.dp), tint = Color(0xFF16162F)); Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.play_now), style = MaterialTheme.typography.labelLarge, color = Color(0xFF16162F))
            }
        }
    }
}

@Composable
private fun TopicSection(t: Topic, expanded: Boolean, onToggle: () -> Unit, onOpen: (GameInfo) -> Unit) {
    val games = Catalog.byTopic(t)
    val shown = if (expanded) games else games.take(2)
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(t.tint().copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                Icon(t.icon(), null, Modifier.size(20.dp), tint = t.tint())
            }
            Spacer(Modifier.width(10.dp))
            SectionTitle(stringResource(t.title), Modifier.weight(1f))
            if (games.size > 2) Text(
                stringResource(if (expanded) R.string.see_less else R.string.see_more),
                Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onToggle).padding(8.dp),
                style = MaterialTheme.typography.labelLarge, color = pm.gold,
            )
        }
        Gap(10)
        if (games.isEmpty()) {
            SurfaceCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Ic.Hourglass, null, tint = pm.inkSoft); Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.coming_soon_desc), style = MaterialTheme.typography.bodyMedium, color = pm.inkSoft)
                }
            }
        } else shown.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { g -> GameTile(g, Modifier.weight(1f), onOpen) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GameTile(g: GameInfo, modifier: Modifier, onOpen: (GameInfo) -> Unit) {
    SurfaceCard(modifier, onClick = { onOpen(g) }) {
        Column {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(g.topic.tint().copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                SideBadge(g.id, 0, Modifier.size(30.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(g.name), style = MaterialTheme.typography.titleMedium, color = pm.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stringResource(g.desc), style = MaterialTheme.typography.bodyMedium, color = pm.inkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
