package com.fws.playmind.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import com.fws.playmind.match.Notice
import com.fws.playmind.match.Phase
import com.fws.playmind.match.SessionViewModel

sealed interface Screen {
    data object Home : Screen
    data class Lobby(val gameId: String) : Screen
    data object Room : Screen
    data object Play : Screen
    data object Settings : Screen
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

    AppFrame {
        when (val s = top) {
            Screen.Home -> HomeScreen(
                app, update.downloaded, { update.complete() },
                onOpenGame = { vm.open(it); push(Screen.Lobby(it.id)) },
                onSettings = { push(Screen.Settings) }, onGuide = { push(Screen.Guide(null)) },
            )
            is Screen.Lobby -> LobbyScreen(app, vm, Catalog.byId(s.gameId), { pop() }, { push(Screen.Room) }, { push(Screen.Guide(s.gameId)) })
            Screen.Room -> RoomScreen(vm) { exitSession() }
            Screen.Play -> vm.info?.let { GameScreen(app, vm, it) { exitSession() } }
            Screen.Settings -> SettingsScreen(app, { pop() }, { push(Screen.Guide(null)) })
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
                stringResource(R.string.close), { vm.notice = Notice.NONE },
            )
        } else SimpleDialog(stringResource(R.string.notice_title), stringResource(msg), stringResource(R.string.ok), { vm.notice = Notice.NONE })
    }
    if (noEarn) SimpleDialog(stringResource(R.string.notice_title), stringResource(R.string.ad_unavailable), stringResource(R.string.ok), { noEarn = false })
}

/** Khung nền chung: nền đặc (sáng = trắng tinh), giới hạn bề rộng 640dp cho máy tính bảng, tránh thanh hệ thống. */
@Composable
fun AppFrame(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(pm.bg),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(Modifier.widthIn(max = 640.dp).fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) { content() }
    }
}

@Composable
private fun FirstRunLanguage(onPick: (String) -> Unit) {
    Dialog(onDismissRequest = {}) {
        Column(
            Modifier.clip(RoundedCornerShape(28.dp)).background(pm.surface).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Ic.Globe, null, Modifier.size(32.dp), tint = pm.gold)
            Text("Choose your language", style = MaterialTheme.typography.titleLarge, color = pm.ink)
            Text(
                "选择语言 · Elige idioma · Escolha o idioma · Выберите язык · 言語を選択 · 언어 선택 · Chọn ngôn ngữ",
                style = MaterialTheme.typography.labelMedium, color = pm.inkSoft,
            )
            Box(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) { LanguageList(Langs.current(), onPick) }
        }
    }
}
