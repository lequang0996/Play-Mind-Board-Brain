package com.fws.playmind.ui

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.fws.playmind.PlayMindApp
import com.fws.playmind.R
import com.fws.playmind.catalog.GameInfo
import com.fws.playmind.catalog.GameKind
import com.fws.playmind.catalog.OptionKind
import com.fws.playmind.match.Notice
import com.fws.playmind.match.SessionViewModel
import com.fws.playmind.net.NearbyTransport

@Composable
fun TopBar(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        PmIconButton(Ic.Back, stringResource(R.string.back), onBack)
        Text(title, Modifier.weight(1f).padding(horizontal = 12.dp), style = MaterialTheme.typography.titleLarge, color = pm.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailing()
    }
}

/** Nhãn của các tùy chọn trước ván (số người / cỡ bàn / mức độ Sudoku). */
@Composable
private fun optionLabels(info: GameInfo): List<String> = when {
    info.kind == GameKind.SOLO -> listOf(R.string.level_easy, R.string.level_medium, R.string.level_hard, R.string.level_expert).map { stringResource(it) }
    info.optionKind == OptionKind.PLAYERS -> info.options.map { stringResource(R.string.n_players, it) }
    info.optionKind == OptionKind.BOARD -> info.options.map { "$it×$it" }
    else -> emptyList()
}

@Composable
private fun optionTitle(info: GameInfo): String = when {
    info.kind == GameKind.SOLO -> stringResource(R.string.level)
    info.optionKind == OptionKind.PLAYERS -> stringResource(R.string.players)
    else -> stringResource(R.string.board_size)
}

@Composable
fun LobbyScreen(app: PlayMindApp, vm: SessionViewModel, info: GameInfo, onBack: () -> Unit, onRoom: () -> Unit, onGuide: () -> Unit) {
    val ctx = LocalContext.current
    val wallet by app.wallet.state.collectAsState()
    val free = app.wallet.freeLeft(info.id)
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    val perms = NearbyTransport.requiredPermissions()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res.values.all { it }) pending?.invoke() else vm.notice = Notice.PERMISSION
        pending = null
    }
    fun withNearby(action: () -> Unit) {
        if (perms.all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }) action()
        else { pending = action; launcher.launch(perms) }
    }
    val solo = info.kind == GameKind.SOLO
    val hasOptions = info.options.isNotEmpty()
    val optIndex = info.options.indexOf(vm.option).coerceAtLeast(0)
    val onlineOk = !solo && (info.optionKind != OptionKind.PLAYERS || vm.option == 2)
    @Suppress("UNUSED_VARIABLE") val keepRecomposing = wallet.diamonds

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(info.name), onBack) { PmIconButton(Ic.Book, stringResource(R.string.view_guide), onGuide) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // ── Thẻ giới thiệu game ──
            val heroShape = RoundedCornerShape(26.dp)
            Row(
                Modifier.fillMaxWidth().clip(heroShape)
                    .background(Brush.linearGradient(listOf(info.topic.color.copy(alpha = .38f), pm.surface, pm.bgDeep), Offset(0f, 0f), Offset(1000f, 700f)))
                    .border(1.dp, info.topic.color.copy(alpha = .5f), heroShape).padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(stringResource(info.topic.title), color = info.topic.color, filled = true)
                    Text(stringResource(info.name), style = MaterialTheme.typography.headlineMedium, color = pm.ink)
                    Text(stringResource(info.desc), style = MaterialTheme.typography.bodyMedium, color = pm.inkSoft)
                    Text(
                        stringResource(R.string.view_guide),
                        Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onGuide).padding(vertical = 2.dp),
                        style = MaterialTheme.typography.labelLarge, color = pm.goldHi, textDecoration = TextDecoration.Underline,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (free > 0) {
                            Icon(Ic.Check, null, Modifier.size(16.dp), tint = pm.good)
                            Text(if (free == Int.MAX_VALUE) stringResource(R.string.free_unlimited) else stringResource(R.string.free_left_n, free), style = MaterialTheme.typography.labelMedium, color = pm.good)
                        } else {
                            GemMark(14.dp)
                            Text(stringResource(R.string.costs_one), style = MaterialTheme.typography.labelMedium, color = pm.inkSoft)
                        }
                    }
                }
                GameArt(info.id, Modifier.size(118.dp))
            }

            // ── Tùy chọn (số người / cỡ bàn / mức độ) ──
            if (hasOptions) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle(optionTitle(info))
                    OptionChips(optionLabels(info), optIndex, { vm.selectOption(info.options[it]) })
                }
            }

            // ── Chơi với máy / bắt đầu ──
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(stringResource(if (solo) R.string.solo_play else R.string.vs_computer))
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!solo && info.optionKind == OptionKind.PLAYERS && vm.option > 2)
                            Text(stringResource(R.string.vs_computers_note, vm.option - 1), style = MaterialTheme.typography.bodySmall, color = pm.inkSoft)
                        GoldButton(
                            stringResource(R.string.start),
                            { if (solo) vm.startSudoku(vm.option) else vm.startVsComputer() },
                            Modifier.fillMaxWidth(), if (solo) Ic.Play else Ic.Computer,
                        )
                    }
                }
            }

            // ── Chơi chung một máy ──
            if (info.localPlay) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle(stringResource(R.string.play_local))
                    SurfaceCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(stringResource(R.string.play_local_desc), style = MaterialTheme.typography.bodySmall, color = pm.inkSoft)
                            OutlineButton(stringResource(R.string.start), { vm.startLocal() }, Modifier.fillMaxWidth(), Ic.Users)
                        }
                    }
                }
            }

            // ── Chơi với bạn bè (online / gần nhau) ──
            if (!solo) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle(stringResource(R.string.play_friends))
                    SurfaceCard(Modifier.fillMaxWidth()) {
                        if (!onlineOk) {
                            Text(stringResource(R.string.online_two_only), style = MaterialTheme.typography.bodyMedium, color = pm.inkSoft)
                        } else Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Ic.Radar, null, Modifier.size(20.dp), tint = pm.gold)
                                Text(stringResource(R.string.nearby), style = MaterialTheme.typography.titleMedium, color = pm.ink)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlineButton(stringResource(R.string.create_room), { withNearby { if (vm.hostRoom(true)) onRoom() } }, Modifier.weight(1f))
                                OutlineButton(stringResource(R.string.join_room), { withNearby { if (vm.joinNearby()) onRoom() } }, Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Ic.Wifi, null, Modifier.size(20.dp), tint = pm.gold)
                                Text(stringResource(R.string.internet), style = MaterialTheme.typography.titleMedium, color = pm.ink)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlineButton(stringResource(R.string.create_room), { if (vm.hostRoom(false)) onRoom() }, Modifier.weight(1f))
                                OutlineButton(stringResource(R.string.join_room), { if (vm.joinNet()) onRoom() }, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(1.dp).height(24.dp))
        }
    }
}
