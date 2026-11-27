package com.fws.playmind.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.fws.playmind.BuildConfig
import com.fws.playmind.PlayMindApp
import com.fws.playmind.R
import com.fws.playmind.catalog.Catalog
import com.fws.playmind.catalog.GameInfo
import com.fws.playmind.catalog.GameKind
import com.fws.playmind.catalog.OptionKind
import com.fws.playmind.catalog.Topic
import com.fws.playmind.data.CheckIn
import com.fws.playmind.data.Plan
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.delay

/** Trang nhà phát triển trên Google Play (mục \"Ứng dụng khác\" trong menu trang chủ). */
const val DEVELOPER_URL = "https://play.google.com/store/apps/developer?id=FWs+Studio"

/** Game nổi bật đổi mỗi 2 phút. */
const val FEATURED_SECONDS = 120L

/** Game nổi bật của một chủ đề: lần lượt xoay vòng qua các game, mỗi game một khung [FEATURED_SECONDS] giây. */
fun featuredGame(t: Topic, slot: Long): GameInfo? {
    val l = Catalog.byTopic(t)
    if (l.isEmpty()) return null
    return l[((slot + t.ordinal) % l.size).toInt()]
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
    onOpenGame: (GameInfo) -> Unit, onSettings: () -> Unit, onGuide: () -> Unit, onShop: () -> Unit,
    onTopic: (Topic) -> Unit = {},
) {
    val ctx = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    val wallet by app.wallet.state.collectAsState()
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); nowMs = System.currentTimeMillis() } }
    val slot = nowMs / 1000L / FEATURED_SECONDS
    val remaining = (FEATURED_SECONDS - (nowMs / 1000L) % FEATURED_SECONDS).toInt()
    var checkIn by remember { mutableStateOf<CheckIn.Done?>(null) }
    val canCheckIn = wallet.lastCheckInDay != app.wallet.today()
    val plan = app.wallet.currentPlan()

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            // Thanh trên: logo + tên, một nút menu thả xuống (icon + chữ)
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                val logoShape = RoundedCornerShape(14.dp)
                Image(
                    painterResource(R.drawable.logo_badge), null,
                    Modifier.size(46.dp).lift(logoShape, 6.dp).clip(logoShape).border(1.dp, pm.goldBright.copy(alpha = .8f), logoShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Play Mind", style = MaterialTheme.typography.headlineMedium, color = pm.ink)
                    Text("Board & Brain", style = MaterialTheme.typography.labelMedium, color = pm.gold)
                }
                Box {
                    PmIconButton(Ic.Menu, stringResource(R.string.menu), { menuOpen = true })
                    DropdownMenu(
                        expanded = menuOpen, onDismissRequest = { menuOpen = false },
                        modifier = Modifier.widthIn(min = 248.dp).padding(horizontal = 6.dp),
                        offset = DpOffset(0.dp, 8.dp),
                        shape = RoundedCornerShape(24.dp), containerColor = pm.surface,
                        tonalElevation = 0.dp, shadowElevation = 18.dp,
                        border = BorderStroke(1.dp, pm.outline),
                    ) {
                        HomeMenuItem(Ic.Bag, stringResource(R.string.shop), pm.goldBright) { menuOpen = false; onShop() }
                        HomeMenuItem(Ic.Book, stringResource(R.string.guide), pm.royal) { menuOpen = false; onGuide() }
                        HomeMenuItem(Ic.Sliders, stringResource(R.string.settings), Color(0xFF14937A)) { menuOpen = false; onSettings() }
                        OrnamentDivider(Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
                        HomeMenuItem(Ic.Apps, stringResource(R.string.other_apps), Color(0xFF8A4FD0)) {
                            menuOpen = false
                            try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DEVELOPER_URL))) } catch (e: ActivityNotFoundException) { }
                        }
                    }
                }
            }
            Gap(16)
            if (!app.wallet.noDiamonds()) Vault(wallet.diamonds, wallet.streakDays, canCheckIn) {
                val r = app.wallet.checkIn(); if (r is CheckIn.Done) checkIn = r
            }
            if (plan == Plan.NONE) {
                Gap(10)
                ShopTeaser(onShop)
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
            val pager = rememberPagerState(pageCount = { Topic.entries.size })
            HorizontalPager(pager, Modifier.fillMaxWidth(), pageSpacing = 12.dp, contentPadding = PaddingValues(end = 28.dp)) { i ->
                val t = Topic.entries[i]
                FeaturedCard(t, featuredGame(t, slot), remaining, onOpenGame)
            }
            Gap(10)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(Topic.entries.size) { i ->
                    val on = pager.currentPage == i
                    Box(Modifier.padding(3.dp).height(6.dp).width(if (on) 22.dp else 6.dp).clip(CircleShape).background(if (on) pm.goldBright else pm.outline))
                }
            }
            Gap(22)
            for (t in Topic.entries) {
                TopicSection(app, t, onOpenGame, onTopic)
                Gap(20)
            }
            Gap(8)
        }
        if (app.ads.bannerAllowed()) BannerAd()
    }

    checkIn?.let { d ->
        PmDialog(
            stringResource(R.string.check_in_done), "",
            stringResource(R.string.ok), { checkIn = null }, tone = DialogTone.GEM,
            body = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("+${d.reward}", style = MaterialTheme.typography.displaySmall, color = pm.gold)
                    GemMark(34.dp)
                }
                Text(pluralStringResource(R.plurals.streak_days, d.streak, d.streak), style = MaterialTheme.typography.bodyMedium, color = pm.inkSoft)
            },
        )
    }
}

@Composable
private fun HomeMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, accent: Color, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, style = MaterialTheme.typography.titleSmall, color = pm.ink, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        leadingIcon = {
            val tile = RoundedCornerShape(12.dp)
            Box(
                Modifier.size(38.dp).clip(tile).background(Brush.linearGradient(listOf(accent.copy(alpha = .20f), accent.copy(alpha = .08f))))
                    .border(1.dp, accent.copy(alpha = .35f), tile),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, Modifier.size(20.dp), tint = accent.onLight(.25f)) }
        },
        trailingIcon = { Icon(Ic.Chevron, null, Modifier.size(16.dp), tint = pm.inkSoft.copy(alpha = .6f)) },
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 6.dp).clip(RoundedCornerShape(16.dp)),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
    )
}

/** Két kim cương: thẻ navy đậm với viền vàng mảnh; số dư lớn, chuỗi điểm danh 7 ngày và nút điểm danh. */
@Composable
private fun Vault(diamonds: Int, streak: Int, canCheckIn: Boolean, onCheckIn: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        Modifier.fillMaxWidth().lift(shape, 14.dp, pm.deep).clip(shape)
            .background(Brush.linearGradient(listOf(pm.deepHigh, pm.deep), Offset(0f, 0f), Offset(900f, 500f)))
            .background(Brush.radialGradient(listOf(pm.goldBright.copy(alpha = .22f), Color.Transparent), center = Offset(60f, 0f), radius = 520f))
            .border(1.dp, pm.goldBright.copy(alpha = .55f), shape).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GemMark(30.dp)
                Text(diamonds.toString(), style = MaterialTheme.typography.displaySmall, color = pm.onDeep)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Ic.Flame, null, Modifier.size(15.dp), tint = if (streak > 0) pm.goldBright else pm.onDeepSoft)
                val filled = if (streak == 0) 0 else ((streak - 1) % 7) + 1
                repeat(7) { i -> Box(Modifier.size(8.dp).clip(CircleShape).background(if (i < filled) pm.goldBright else pm.onDeepSoft.copy(alpha = .3f))) }
            }
            Text(pluralStringResource(R.plurals.streak_days, streak, streak), style = MaterialTheme.typography.labelMedium, color = pm.onDeepSoft)
        }
        if (canCheckIn) GoldButton(stringResource(R.string.check_in), onCheckIn, icon = Ic.Calendar)
        else Row(
            Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF3DDC97).copy(alpha = .14f)).border(1.dp, Color(0xFF3DDC97).copy(alpha = .5f), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Ic.Check, null, Modifier.size(18.dp), tint = Color(0xFF3DDC97))
            Text(stringResource(R.string.checked_in), style = MaterialTheme.typography.labelLarge, color = Color(0xFF3DDC97))
        }
    }
}

@Composable
private fun ShopTeaser(onShop: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val src = rememberPress()
    Row(
        Modifier.bounce(src, .98f).fillMaxWidth().lift(shape, 6.dp).clip(shape).background(cardBrush()).border(1.dp, hairline(), shape).tap(src, onClick = onShop).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(pm.goldSoft), contentAlignment = Alignment.Center) {
            Icon(Ic.Bag, null, Modifier.size(18.dp), tint = pm.gold)
        }
        Text(stringResource(R.string.shop_teaser), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = pm.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Icon(Ic.Chevron, null, Modifier.size(18.dp), tint = pm.inkSoft)
    }
}

@Composable
private fun CountdownChip(remaining: Int) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.clip(shape).background(pm.surface.copy(alpha = .85f)).border(1.dp, pm.outline, shape).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Ic.Hourglass, null, Modifier.size(13.dp), tint = pm.inkSoft)
        Text("%d:%02d".format(remaining / 60, remaining % 60), style = MaterialTheme.typography.labelMedium, color = pm.ink)
    }
}

@Composable
private fun FeaturedCard(t: Topic, game: GameInfo?, remaining: Int, onOpen: (GameInfo) -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    val tc = t.color
    val src = rememberPress()
    Column(
        Modifier.bounce(src, .985f).fillMaxWidth().height(214.dp).lift(shape, 12.dp, tc.onLight(.5f)).clip(shape)
            .background(Brush.linearGradient(listOf(tc.copy(alpha = .34f), pm.surface, pm.surface), Offset(0f, 0f), Offset(900f, 900f)))
            .background(Brush.radialGradient(listOf(Color.White.copy(alpha = .55f), Color.Transparent), center = Offset(820f, 120f), radius = 420f))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White, tc.copy(alpha = .5f))), shape)
            .then(if (game != null) Modifier.tap(src) { onOpen(game) } else Modifier)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(stringResource(t.title), color = tc, filled = true)
            Spacer(Modifier.weight(1f))
            if (game != null) CountdownChip(remaining)
        }
        Row(Modifier.fillMaxWidth().weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (game != null) stringResource(game.name) else stringResource(R.string.coming_soon),
                    style = MaterialTheme.typography.headlineMedium, color = pm.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (game != null) stringResource(game.desc) else stringResource(R.string.coming_soon_desc),
                    style = MaterialTheme.typography.bodySmall, color = pm.inkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                if (game != null) {
                    Spacer(Modifier.height(8.dp))
                    GoldButton(stringResource(R.string.play_now), { onOpen(game) }, Modifier.height(44.dp), icon = Ic.Play)
                }
            }
            if (game != null) {
                Spacer(Modifier.width(8.dp))
                GameArt(game.id, Modifier.size(124.dp))
            }
        }
    }
}

@Composable
private fun TopicSection(app: PlayMindApp, t: Topic, onOpen: (GameInfo) -> Unit, onTopic: (Topic) -> Unit) {
    val games = Catalog.byTopic(t)
    TopicFrame(t) {
        Row(Modifier.padding(start = 16.dp, end = 10.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val tile = RoundedCornerShape(13.dp)
            Box(
                Modifier.size(40.dp).lift(tile, 6.dp, t.color).clip(tile)
                    .background(Brush.linearGradient(listOf(t.color, t.color.onLight(.35f))))
                    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .35f), Color.Transparent))),
                contentAlignment = Alignment.Center,
            ) { Icon(t.icon(), null, Modifier.size(21.dp), tint = Color.White) }
            Column(Modifier.weight(1f)) {
                SectionTitle(stringResource(t.title))
                Text(pluralStringResource(R.plurals.topic_games, games.size, games.size), style = MaterialTheme.typography.labelMedium, color = t.color.onLight(.45f))
            }
            // "Xem thêm (n)": mở danh sách đầy đủ của chủ đề, xếp từ dễ chơi / ván ngắn đến khó / ván dài
            if (games.size > 1) Row(
                Modifier.clip(RoundedCornerShape(50)).background(pm.surface.copy(alpha = .75f)).border(1.dp, t.color.copy(alpha = .35f), RoundedCornerShape(50))
                    .clickable { onTopic(t) }.padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(stringResource(R.string.see_more), style = MaterialTheme.typography.labelLarge, color = t.color.onLight(.45f))
                Icon(Ic.Chevron, null, Modifier.size(16.dp), tint = t.color.onLight(.45f))
            }
        }
        if (games.isEmpty()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Ic.Hourglass, null, Modifier.size(18.dp), tint = t.color.onLight(.4f))
                Text(stringResource(R.string.coming_soon_desc), style = MaterialTheme.typography.bodyMedium, color = pm.inkSoft)
            }
        } else {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                for (g in games) GameTile(app, g, t, onOpen)
            }
        }
    }
}

/**
 * Khung bọc mỗi chủ đề, mỗi chủ đề một họa tiết riêng vẽ mờ ở góc phải:
 * Trí tuệ = bàn cờ ô vuông, Tính toán = các phép tính, Giải đố = mạng nút nối nhau, Ghi nhớ = các lá bài xếp chồng.
 */
@Composable
private fun TopicFrame(t: Topic, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(30.dp)
    val c = t.color
    Column(
        Modifier.fillMaxWidth().lift(shape, 12.dp, c.onLight(.5f)).clip(shape)
            .background(Brush.verticalGradient(listOf(c.copy(alpha = .16f), pm.surface.copy(alpha = .92f), pm.surfaceHigh)))
            .drawBehind { drawTopicMotif(t, c) }
            .border(1.dp, Brush.verticalGradient(listOf(Color.White, c.copy(alpha = .40f))), shape),
    ) { content() }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTopicMotif(t: Topic, c: Color) {
    val u = 15.dp.toPx()
    val right = size.width
    val ink = c.copy(alpha = .16f)
    val line = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
    when (t) {
        Topic.MIND -> {                                   // bàn cờ mờ dần về phía trái
            for (r in 0 until 4) for (k in 0 until 7) if ((r + k) % 2 == 0) {
                val a = (.16f - k * .02f - r * .025f).coerceAtLeast(0f)
                drawRect(c.copy(alpha = a), Offset(right - (k + 1) * u, r * u), androidx.compose.ui.geometry.Size(u, u))
            }
        }
        Topic.MATH -> {                                   // + − × ÷
            val y = u * 1.6f
            for (i in 0 until 4) {
                val cx = right - u * (1.4f + i * 2.2f); val h = u * .55f
                val col = c.copy(alpha = (.20f - i * .035f))
                when (i) {
                    0 -> { drawLine(col, Offset(cx - h, y), Offset(cx + h, y), line.width, line.cap); drawLine(col, Offset(cx, y - h), Offset(cx, y + h), line.width, line.cap) }
                    1 -> drawLine(col, Offset(cx - h, y), Offset(cx + h, y), line.width, line.cap)
                    2 -> { drawLine(col, Offset(cx - h * .75f, y - h * .75f), Offset(cx + h * .75f, y + h * .75f), line.width, line.cap); drawLine(col, Offset(cx - h * .75f, y + h * .75f), Offset(cx + h * .75f, y - h * .75f), line.width, line.cap) }
                    else -> { drawLine(col, Offset(cx - h, y), Offset(cx + h, y), line.width, line.cap); drawCircle(col, h * .2f, Offset(cx, y - h * .6f)); drawCircle(col, h * .2f, Offset(cx, y + h * .6f)) }
                }
            }
        }
        Topic.PUZZLE -> {                                 // các nút nối nhau như một câu đố
            val pts = listOf(Offset(right - u * 1.2f, u * .9f), Offset(right - u * 3f, u * 2.2f), Offset(right - u * 4.8f, u * .8f), Offset(right - u * 6.4f, u * 2.4f), Offset(right - u * 2.4f, u * 3.6f))
            val links = listOf(0 to 1, 1 to 2, 2 to 3, 1 to 4, 0 to 4)
            for ((a, b) in links) drawLine(ink, pts[a], pts[b], line.width, line.cap)
            pts.forEachIndexed { i, o -> drawCircle(c.copy(alpha = .22f - i * .03f), u * .32f, o) }
        }
        Topic.MEMORY -> {                                 // ba lá bài xòe ra
            for (i in 0 until 3) {
                val cx = right - u * (1.8f + i * 1.3f); val cy = u * 1.9f
                rotate(degrees = 14f - i * 14f, pivot = Offset(cx, cy + u * 2f)) {
                    drawRoundRect(c.copy(alpha = .10f + i * .03f), Offset(cx - u * .8f, cy - u * 1.1f), androidx.compose.ui.geometry.Size(u * 1.6f, u * 2.2f), androidx.compose.ui.geometry.CornerRadius(u * .25f))
                    drawRoundRect(c.copy(alpha = .22f), Offset(cx - u * .8f, cy - u * 1.1f), androidx.compose.ui.geometry.Size(u * 1.6f, u * 2.2f), androidx.compose.ui.geometry.CornerRadius(u * .25f), style = line)
                }
            }
        }
    }
}

/** Nhãn thông tin gọn một dòng (giá, số người) dùng trên thẻ game. */
@Composable
private fun MetaChip(text: String, color: Color, leading: @Composable () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.clip(shape).background(color.copy(alpha = .10f)).border(1.dp, color.copy(alpha = .30f), shape).padding(start = 6.dp, end = 9.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        leading()
        Text(text, style = MaterialTheme.typography.labelMedium, color = color.onLight(), maxLines = 1, softWrap = false)
    }
}

@Composable
internal fun gameTag(g: GameInfo): String? {
    val o = g.options
    return when {
        g.kind == GameKind.SOLO -> null                       // game một người: không cần nhãn
        g.optionKind == OptionKind.PLAYERS -> stringResource(R.string.tag_players, o.min(), o.max())
        else -> null
    }
}

@Composable
private fun GameTile(app: PlayMindApp, g: GameInfo, t: Topic, onOpen: (GameInfo) -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val free = app.wallet.freeLeft(g.id)
    val src = rememberPress()
    Column(
        Modifier.bounce(src).width(166.dp).lift(shape, 10.dp).clip(shape).background(cardBrush()).border(1.dp, hairline(), shape).tap(src) { onOpen(g) },
    ) {
        Box(
            Modifier.fillMaxWidth().height(116.dp).background(Brush.linearGradient(listOf(t.color.copy(alpha = .26f), pm.surfaceHigh), Offset(0f, 0f), Offset(500f, 400f))),
            contentAlignment = Alignment.Center,
        ) { GameArt(g.id, Modifier.size(104.dp)) }
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(g.name), style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp), color = pm.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(g.desc), style = MaterialTheme.typography.bodySmall, color = pm.inkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (app.wallet.noDiamonds()) { /* gói năm: không còn kim cương / lượt miễn phí */ }
                else if (free > 0) MetaChip(if (free == Int.MAX_VALUE) "∞" else stringResource(R.string.free_n, free), pm.good) { Icon(Ic.Check, null, Modifier.size(12.dp), tint = pm.good) }
                else MetaChip("1", pm.goldBright) { GemMark(13.dp) }
                if (g.optionKind == OptionKind.PLAYERS) MetaChip("${g.options.min()}–${g.options.max()}", pm.royal) { Icon(Ic.Users, null, Modifier.size(13.dp), tint = pm.royal.onLight(.2f)) }
            }
        }
    }
}
