package com.fws.playmind.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fws.playmind.PlayMindApp
import com.fws.playmind.R
import com.fws.playmind.ads.UpdateManager
import com.fws.playmind.catalog.Catalog
import com.fws.playmind.catalog.GameKind
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.CircularProgressIndicator
import com.fws.playmind.match.Notice
import com.fws.playmind.match.Phase
import com.fws.playmind.match.SessionViewModel

sealed interface Screen {
    data object Home : Screen
    data class Lobby(val gameId: String) : Screen
    data object Room : Screen
    data object Play : Screen
    data object Settings : Screen
    data object Shop : Screen
    /** Danh sách đầy đủ game của một chủ đề, xếp từ dễ chơi / ván ngắn đến khó chơi / ván dài. */
    data class TopicList(val topic: com.fws.playmind.catalog.Topic) : Screen
    data class Guide(val gameId: String?) : Screen
}

@Composable
fun AppRoot(app: PlayMindApp, vm: SessionViewModel, update: UpdateManager) {
    var stack by remember { mutableStateOf<List<Screen>>(listOf(Screen.Home)) }
    val top = stack.last()
    var langChosen by remember { mutableStateOf(app.prefs.langChosen) }

    fun push(s: Screen) { stack = stack + s }
    fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }
    fun exitSession() { vm.leave(); stack = stack.dropLastWhile { it is Screen.Play || it is Screen.Room } }

    // Tự chuyển màn hình theo trạng thái phiên chơi
    LaunchedEffect(vm.phase) {
        when (vm.phase) {
            Phase.PLAYING -> if (stack.last() !is Screen.Play) push(Screen.Play)
            Phase.IDLE -> if (stack.last() is Screen.Play || stack.last() is Screen.Room) stack = stack.dropLastWhile { it is Screen.Play || it is Screen.Room }
            else -> {}
        }
    }

    BackHandler(enabled = top !is Screen.Home && top !is Screen.Play) {
        if (top is Screen.Room) exitSession() else pop()
    }

    // Cho chụp màn hình ở mọi màn, trừ khi đang ở trong một ván (FLAG_SECURE chỉ bật ở màn chơi)
    val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
    val inGame = top is Screen.Play
    androidx.compose.runtime.DisposableEffect(inGame) {
        val w = activity?.window
        if (inGame) w?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) else w?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { }
    }

    val walletReady by app.wallet.ready.collectAsState()
    AppFrame {
        if (!walletReady) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = pm.gold) }
        } else AnimatedContent(
            targetState = top,
            transitionSpec = {
                // tiến vào màn mới: trượt nhẹ từ phải + mờ dần; quay lại: trượt từ trái
                val forward = stack.size >= depthOf(initialState, stack)
                val dir = if (forward) 1 else -1
                (fadeIn(tween(240, delayMillis = 40)) + slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 10 * dir } +
                    scaleIn(tween(320), initialScale = .985f)) togetherWith
                    (fadeOut(tween(160)) + slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it / 14 * dir })
            },
            label = "screen",
        ) { s -> when (s) {
            Screen.Home -> HomeScreen(
                app, update.downloaded, { update.complete() },
                onOpenGame = { vm.open(it); push(Screen.Lobby(it.id)) },
                onSettings = { push(Screen.Settings) }, onGuide = { push(Screen.Guide(null)) }, onShop = { push(Screen.Shop) },
                onTopic = { push(Screen.TopicList(it)) },
            )
            is Screen.TopicList -> TopicScreen(app, s.topic, { pop() }) { vm.open(it); push(Screen.Lobby(it.id)) }
            is Screen.Lobby -> LobbyScreen(app, vm, Catalog.byId(s.gameId), { pop() }, { push(Screen.Room) }, { push(Screen.Guide(s.gameId)) })
            Screen.Room -> RoomScreen(vm) { exitSession() }
            Screen.Play -> vm.info?.let { if (it.id == Catalog.MAGIC_ID) MagicScreen(app, vm, it) { exitSession() } else if (it.id == Catalog.TANGRAM_ID) TangramScreen(app, vm, it) { exitSession() } else if (it.id == Catalog.WORDS_ID) WordBlocksScreen(app, vm, it) { exitSession() } else if (it.kind == GameKind.SOLO) SudokuScreen(app, vm, it) { exitSession() } else GameScreen(app, vm, it) { exitSession() } }
            Screen.Settings -> SettingsScreen(app, { pop() }, { push(Screen.Guide(null)) }, { push(Screen.Shop) })
            Screen.Shop -> ShopScreen(app) { pop() }
            is Screen.Guide -> GuideScreen({ pop() }, s.gameId)
        } }
    }

    // Hộp thoại chọn ngôn ngữ ở lần mở đầu tiên (bắt buộc chọn)
    if (!langChosen) FirstRunLanguage { tag -> Langs.apply(tag); app.prefs.langChosen = true; langChosen = true }

    // Thông báo từ phiên chơi
    val n = vm.notice
    if (n != Notice.NONE) {
        val msg = when (n) {
            Notice.NOT_ENOUGH -> R.string.notice_not_enough
            Notice.PERMISSION -> R.string.notice_permission
            Notice.CONN_LOST -> R.string.notice_conn_lost
            Notice.OPP_LEFT -> R.string.notice_opp_left
            Notice.KICKED -> R.string.notice_kicked
            Notice.BAD_CODE -> R.string.notice_bad_code
            Notice.CONN_FAILED -> R.string.notice_conn_failed
            Notice.UNVERIFIED -> R.string.notice_unverified
            Notice.HOST_LEFT -> R.string.notice_host_left
            Notice.ROOM_TAKEN -> R.string.notice_room_taken
            Notice.MAGIC_DONE -> R.string.notice_magic_done
            Notice.NO_HINTS -> R.string.notice_no_hints
            Notice.FAV_LOCKED -> R.string.notice_fav_locked
            Notice.NONE -> R.string.empty
        }
        if (n == Notice.NOT_ENOUGH || n == Notice.FAV_LOCKED) {
            SimpleDialog(
                stringResource(R.string.notice_title), stringResource(msg),
                stringResource(R.string.shop), { vm.notice = Notice.NONE; if (stack.last() !is Screen.Shop) push(Screen.Shop) },
                stringResource(R.string.close), { vm.notice = Notice.NONE }, tone = DialogTone.GEM,
            )
        } else SimpleDialog(stringResource(R.string.notice_title), stringResource(msg), stringResource(R.string.ok), { vm.notice = Notice.NONE }, tone = DialogTone.WARN)
    }
}

/**
 * Khung nền chung: nền ngọc trai chuyển sắc rất nhẹ kèm hai vầng sáng mờ (vàng champagne ở góc trên, tím nhạt ở góc dưới),
 * giới hạn bề rộng 640dp cho máy tính bảng, tránh thanh hệ thống.
 */
/** Vị trí (độ sâu) của một màn hình trong ngăn xếp; không có thì coi như sâu hơn hiện tại (đang quay lại). */
private fun depthOf(s: Screen, stack: List<Screen>): Int { val i = stack.indexOf(s); return if (i < 0) Int.MAX_VALUE else i + 1 }

@Composable
fun AppFrame(content: @Composable () -> Unit) {
    val p = pm
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(p.bg, p.bgDeep)))
            .drawBehind {
                // "Aurora" rất nhẹ: vàng champagne góc trên trái, xanh chàm góc trên phải, hồng ngọc góc dưới
                val r = size.maxDimension * .75f
                drawCircle(Brush.radialGradient(0f to p.goldBright.copy(alpha = .22f), 1f to Color.Transparent, center = Offset(0f, 0f), radius = r), r, Offset(0f, 0f))
                val r2 = size.maxDimension * .55f
                drawCircle(Brush.radialGradient(0f to Color(0xFF6C7CFF).copy(alpha = .13f), 1f to Color.Transparent, center = Offset(size.width, size.height * .18f), radius = r2), r2, Offset(size.width, size.height * .18f))
                drawCircle(Brush.radialGradient(0f to Color(0xFFB36CFF).copy(alpha = .09f), 1f to Color.Transparent, center = Offset(size.width * .2f, size.height), radius = r), r, Offset(size.width * .2f, size.height))
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(Modifier.widthIn(max = 640.dp).fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) { content() }
    }
}

@Composable
private fun FirstRunLanguage(onPick: (String) -> Unit) {
    Dialog(onDismissRequest = {}) {
        Column(
            Modifier.clip(RoundedCornerShape(28.dp)).background(pm.surface).border(1.dp, pm.outline, RoundedCornerShape(28.dp)).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Ic.Globe, null, Modifier.size(30.dp), tint = pm.gold)
            Text("Choose your language", style = MaterialTheme.typography.headlineSmall, color = pm.ink)
            Box(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) { LanguageList(Langs.current(), onPick) }
        }
    }
}
