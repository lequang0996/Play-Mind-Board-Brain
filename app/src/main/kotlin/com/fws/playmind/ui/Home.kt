package com.fws.playmind.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
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
    // Lưu ý: tham số đầu tiên của AndroidView là factory, modifier phải truyền theo tên.
    AndroidView<AdView>(
        factory = { c ->
            AdView(c).apply {
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(c, width))
                adUnitId = BuildConfig.ADMOB_BANNER
                loadAd(AdRequest.Builder().build())
            }
        },
        modifier = modifier.fillMaxWidth(),
        onRelease = { adView -> adView.destroy() },
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
            // Thanh trên: huy hiệu (icon app) + tên
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                val logoShape = RoundedCornerShape(16.dp)
                Image(
                    painterResource(R.drawable.logo_badge), null,
                    Modifier.size(54.dp).shadow(6.dp, logoShape, ambientColor = pm.navy.copy(alpha = .3f), spotColor = pm.navy.copy(alpha = .4f))
                        .clip(logoShape).border(1.2.dp, pm.goldBright, logoShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Play Mind", style = MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp, lineHeight = 32.sp), color = pm.ink)
                    Eyebrow("Board & Brain")
                }
                Box(Modifier.size(44.dp).clip(CircleShape).background(pm.surface).border(1.dp, pm.outline, CircleShape), contentAlignment = Alignment.Center) {
                    PmIconButton(Ic.Help, stringResource(R.string.guide), onGuide)
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(44.dp).clip(CircleShape).background(pm.surface).border(1.dp, pm.outline, CircleShape), contentAlignment = Alignment.Center) {
                    PmIconButton(Ic.Sliders, stringResource(R.string.settings), onSettings)
                }
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
                    Box(Modifier.padding(3.dp).height(6.dp).width(if (on) 22.dp else 6.dp).clip(CircleShape).background(if (on) pm.goldBright else pm.outline))
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
    val shape = RoundedCornerShape(26.dp)
    val hi = pm.goldHi; val br = pm.goldBright
    Box(
        Modifier.fillMaxWidth().shadow(12.dp, shape, ambientColor = pm.navy.copy(alpha = .3f), spotColor = pm.navy.copy(alpha = .5f))
            .clip(shape).background(Brush.linearGradient(listOf(pm.navy2, pm.navy)))
            .border(1.dp, pm.goldBright.copy(alpha = .55f), shape)
            .drawBehind {
                // hoa văn: các vòng cung đồng tâm ở góc phải + vài đốm sáng
                val c = Offset(size.width * 1.02f, -size.height * .1f)
                for (k in 1..5) drawCircle(br.copy(alpha = .13f - k * .017f), size.height * (.35f + k * .2f), c, style = Stroke(1.dp.toPx()))
                drawCircle(Brush.radialGradient(0f to hi.copy(alpha = .30f), 1f to Color.Transparent, center = c, radius = size.width * .5f), size.width * .5f, c)
                for (k in 0 until 14) {
                    val x = size.width * hash01(k, 1); val y = size.height * hash01(k, 2)
                    drawCircle(Color.White.copy(alpha = .10f + hash01(k, 3) * .25f), (.6f + hash01(k, 4) * 1.2f).dp.toPx(), Offset(x, y))
                }
            }.padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                DiamondChip(diamonds, onDark = true)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Ic.Flame, null, Modifier.size(16.dp), tint = pm.goldHi)
                    Spacer(Modifier.width(4.dp))
                    Text(pluralStringResource(R.plurals.streak_days, streak, streak), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .85f))
                }
            }
            if (canCheckIn) GoldButton(stringResource(R.string.check_in), onCheckIn, icon = Ic.Calendar)
            else Row(
                Modifier.height(48.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = .10f)).border(1.dp, Color.White.copy(alpha = .2f), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Ic.Check, null, Modifier.size(18.dp), tint = Color(0xFF5BE3C1)); Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.checked_in), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = .9f))
            }
        }
    }
}

@Composable
private fun FeaturedCard(t: Topic, game: GameInfo?, onOpen: (GameInfo) -> Unit) {
    val tint = t.tint()
    val shape = RoundedCornerShape(28.dp)
    val hi = pm.goldHi; val br = pm.goldBright
    Box(
        Modifier.fillMaxWidth().height(200.dp)
            .shadow(12.dp, shape, ambientColor = pm.navy.copy(alpha = .3f), spotColor = pm.navy.copy(alpha = .5f))
            .clip(shape)
            .background(Brush.linearGradient(listOf(pm.navy2, pm.navy, Color(0xFF080B1E))))
            .background(Brush.linearGradient(listOf(tint.copy(alpha = .38f), Color.Transparent), Offset.Zero, Offset(700f, 500f)))
            .drawBehind {
                val c = Offset(size.width * .80f, size.height * .5f)
                drawCircle(Brush.radialGradient(0f to hi.copy(alpha = .38f), 1f to Color.Transparent, center = c, radius = size.height * .85f), size.height * .85f, c)
                // khung vàng kép + 4 góc trang trí
                val m = 7.dp.toPx(); val cr = 22.dp.toPx()
                drawRoundRect(br.copy(alpha = .55f), Offset(m, m), Size(size.width - 2 * m, size.height - 2 * m), CornerRadius(cr), style = Stroke(1.dp.toPx()))
                drawRoundRect(br.copy(alpha = .22f), Offset(m + 4.dp.toPx(), m + 4.dp.toPx()), Size(size.width - 2 * m - 8.dp.toPx(), size.height - 2 * m - 8.dp.toPx()), CornerRadius(cr - 4.dp.toPx()), style = Stroke(.8.dp.toPx()))
                for (k in 0 until 16) drawCircle(Color.White.copy(alpha = .08f + hash01(k, 8) * .22f), (.5f + hash01(k, 9)).dp.toPx(), Offset(size.width * hash01(k, 5), size.height * hash01(k, 6)))
            }
            .then(if (game != null) Modifier.clickable { onOpen(game) } else Modifier),
    ) {
        if (game != null) GameArt(game.id, Modifier.align(Alignment.CenterEnd).padding(end = 14.dp).size(146.dp))
        else Icon(t.icon(), null, Modifier.align(Alignment.CenterEnd).padding(end = 22.dp).size(120.dp), tint = Color.White.copy(alpha = .16f))
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 22.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.fillMaxWidth(.62f)) {
                Eyebrow(stringResource(t.title), color = pm.goldHi)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (game != null) stringResource(game.name) else stringResource(R.string.coming_soon),
                    style = MaterialTheme.typography.headlineSmall, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                if (game != null) Text(stringResource(game.desc), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .78f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                else Text(stringResource(R.string.coming_soon_desc), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .78f))
            }
            if (game != null) {
                val pill = RoundedCornerShape(50)
                Row(
                    Modifier.shadow(6.dp, pill, ambientColor = pm.goldLo.copy(alpha = .4f), spotColor = pm.goldLo.copy(alpha = .6f)).clip(pill)
                        .background(goldBrush()).background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .4f), Color.Transparent)))
                        .border(1.dp, Color.White.copy(alpha = .55f), pill).padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Ic.Play, null, Modifier.size(16.dp), tint = pm.navy); Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.play_now), style = MaterialTheme.typography.labelLarge, color = pm.navy)
                }
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
            Box(Modifier.size(36.dp).clip(CircleShape).background(t.tint().copy(alpha = .12f)).border(1.dp, t.tint().copy(alpha = .35f), CircleShape), contentAlignment = Alignment.Center) {
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
        Gap(6)
        OrnamentDivider()
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
            val artShape = RoundedCornerShape(16.dp)
            Box(
                Modifier.fillMaxWidth().height(84.dp).clip(artShape)
                    .background(Brush.linearGradient(listOf(g.topic.tint().copy(alpha = .14f), pm.goldSoft)))
                    .border(1.dp, pm.outline, artShape),
                contentAlignment = Alignment.Center,
            ) { GameArt(g.id, Modifier.size(76.dp)) }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(g.name), style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp), color = pm.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(g.desc), style = MaterialTheme.typography.bodyMedium, color = pm.inkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
