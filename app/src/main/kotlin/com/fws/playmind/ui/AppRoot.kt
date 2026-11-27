package com.fws.playmind.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
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
    data class Guide(val gameId: String?) : Screen
}

@Composable
fun AppRoot(app: PlayMindApp, vm: SessionViewModel, update: UpdateManager) {
    val activity = LocalContext.current as Activity
    var stack by remember { mutableStateOf<List<Screen>>(listOf(Screen.Home)) }
    val top = stack.last()
    var langChosen by remember { mutableStateOf(app.prefs.langChosen) }
    var noEarn by remember { mutableStateOf(false) }

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

    val walletReady by app.wallet.ready.collectAsState()
    AppFrame {
        if (!walletReady) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = pm.gold) }
        } else when (val s = top) {
            Screen.Home -> HomeScreen(
                app, update.downloaded, { update.complete() },
                onOpenGame = { vm.open(it); push(Screen.Lobby(it.id)) },
                onSettings = { push(Screen.Settings) }, onGuide = { push(Screen.Guide(null)) }, onShop = { push(Screen.Shop) },
            )
            is Screen.Lobby -> LobbyScreen(app, vm, Catalog.byId(s.gameId), { pop() }, { push(Screen.Room) }, { push(Screen.Guide(s.gameId)) })
            Screen.Room -> RoomScreen(vm) { exitSession() }
            Screen.Play -> vm.info?.let { if (it.kind == GameKind.SOLO) SudokuScreen(app, vm, it) { exitSession() } else GameScreen(app, vm, it) { exitSession() } }
            Screen.Settings -> SettingsScreen(app, { pop() }, { push(Screen.Guide(null)) }, { push(Screen.Shop) })
            Screen.Shop -> ShopScreen(app) { pop() }
            is Screen.Guide -> GuideScreen({ pop() }, s.gameId)
        }
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
            Notice.NONE -> R.string.empty
        }
        if (n == Notice.NOT_ENOUGH) {
            SimpleDialog(
                stringResource(R.string.notice_title), stringResource(msg),
                stringResource(R.string.watch_ad), {
                    vm.notice = Notice.NONE
                    app.ads.showRewarded(activity, { app.wallet.addDiamonds(1) }, { noEarn = true })
                },
                stringResource(R.string.close), { vm.notice = Notice.NONE }, tone = DialogTone.GEM,
            )
        } else SimpleDialog(stringResource(R.string.notice_title), stringResource(msg), stringResource(R.string.ok), { vm.notice = Notice.NONE }, tone = DialogTone.WARN)
    }
    if (noEarn) SimpleDialog(stringResource(R.string.notice_title), stringResource(R.string.ad_unavailable), stringResource(R.string.ok), { noEarn = false })
}

/**
 * Khung nền chung: nền mực xanh đen chuyển sắc rất nhẹ kèm hai vầng sáng mờ (vàng đồng ở góc trên, tím ở góc dưới),
 * giới hạn bề rộng 640dp cho máy tính bảng, tránh thanh hệ thống.
 */
@Composable
fun AppFrame(content: @Composable () -> Unit) {
    val p = pm
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(p.bg, p.bgDeep)))
            .drawBehind {
                val r = size.maxDimension * .75f
                drawCircle(Brush.radialGradient(0f to p.goldBright.copy(alpha = .10f), 1f to Color.Transparent, center = Offset(0f, 0f), radius = r), r, Offset(0f, 0f))
                drawCircle(Brush.radialGradient(0f to Color(0xFF6C5CFF).copy(alpha = .12f), 1f to Color.Transparent, center = Offset(size.width, size.height), radius = r), r, Offset(size.width, size.height))
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
