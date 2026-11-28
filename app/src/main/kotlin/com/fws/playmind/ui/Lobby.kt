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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.fws.playmind.catalog.Catalog
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
        Text(title, Modifier.weight(1f).padding(horizontal = 14.dp), style = MaterialTheme.typography.headlineSmall, color = pm.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailing()
    }
}

/** Nhãn của tùy chọn trước ván: chỉ còn số người chơi (Wall Go, Quoridor). Cờ vây luôn 19x19, Sudoku chỉ có một chế độ chuẩn. */
@Composable
private fun optionLabels(info: GameInfo): List<String> = when {
    info.optionKind == OptionKind.PLAYERS -> info.options.map { stringResource(R.string.n_players, it) }
    else -> emptyList()
}

/**
 * Một khối chế độ chơi: biểu tượng tròn, tiêu đề + mô tả ngắn, rồi các nút hành động bên dưới.
 * Dùng chung cho "Chơi với máy" và hai kiểu "Chơi với bạn bè" để mọi game có cùng một ngôn ngữ giao diện.
 */
@Composable
private fun ModeCard(icon: ImageVector, title: String, subtitle: String?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier.fillMaxWidth().lift(shape, 10.dp).clip(shape).background(cardBrush()).border(1.dp, hairline(), shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(Brush.linearGradient(listOf(pm.goldSoft, pm.surface))).border(1.dp, pm.goldBright.copy(alpha = .6f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, Modifier.size(21.dp), tint = pm.gold) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = pm.ink)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = pm.inkSoft)
            }
        }
        content()
    }
}

private val LEVEL_NAMES = intArrayOf(R.string.level_0, R.string.level_1, R.string.level_2, R.string.level_3, R.string.level_4, R.string.level_5, R.string.level_6)
private val LEVEL_DESCS = intArrayOf(R.string.level_desc_0, R.string.level_desc_1, R.string.level_desc_2, R.string.level_desc_3, R.string.level_desc_4, R.string.level_desc_5, R.string.level_desc_6)

/** Tên cấp bot (Dễ ... Siêu việt). */
@Composable
fun levelName(i: Int): String = stringResource(LEVEL_NAMES[i.coerceIn(0, LEVEL_NAMES.size - 1)])

/**
 * Chọn 1 trong 7 cấp bot. Chỉ cấp Dễ mở sẵn; thắng cấp L thì mở cấp L + 1. Cấp chưa mở hiện ổ khóa, bấm vào chỉ hiện gợi ý.
 * Dưới lưới là mô tả cấp và thời gian tối đa cho mỗi nước của người chơi ở cấp đó.
 */
@Composable
private fun LevelPicker(selected: Int, unlocked: Int, onSelect: (Int) -> Unit) {
    var hint by remember { mutableStateOf(-1) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in listOf(0..3, 4..6)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in row) {
                    val on = i == selected; val locked = i > unlocked
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        Modifier.weight(1f).height(44.dp).clip(shape)
                            .background(if (on) Brush.verticalGradient(listOf(pm.goldSoft, pm.goldHi.copy(alpha = .55f))) else cardBrush())
                            .border(1.2.dp, if (on) Brush.verticalGradient(listOf(pm.goldHi, pm.goldBright)) else hairline(), shape)
                            .clickable { if (locked) hint = i else { hint = -1; onSelect(i) } }
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                    ) {
                        if (locked) { Icon(Ic.Lock, null, Modifier.size(13.dp), tint = pm.inkSoft.copy(alpha = .7f)); Spacer(Modifier.width(3.dp)) }
                        Text(
                            levelName(i), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = if (on) pm.gold else if (locked) pm.inkSoft.copy(alpha = .6f) else pm.ink,
                        )
                    }
                }
                if (row.last - row.first < 3) Spacer(Modifier.weight(1f))
            }
        }
        val shown = if (hint >= 0) hint else selected
        Text(
            if (hint >= 0) stringResource(R.string.level_locked, levelName(hint - 1)) else stringResource(LEVEL_DESCS[shown]),
            style = MaterialTheme.typography.bodySmall, color = if (hint >= 0) pm.danger else pm.inkSoft,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Ic.Hourglass, null, Modifier.size(14.dp), tint = pm.gold)
            Text(stringResource(R.string.level_move_time, Catalog.LEVEL_MOVE_SECONDS[selected] / 60), style = MaterialTheme.typography.labelMedium, color = pm.gold)
        }
    }
}

/** Nhãn ngắn của một mức thời gian mỗi nước (1 phút ... 1 giờ). */
@Composable
fun timeLabel(seconds: Int): String = if (seconds >= 3600) stringResource(R.string.time_1h) else stringResource(R.string.time_n_min, seconds / 60)

/** Chọn thời gian mỗi nước khi chơi với người: cùng 7 mức với các cấp máy (Catalog.LEVEL_MOVE_SECONDS). */
@Composable
private fun TimePicker(selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in listOf(0..3, 4..6)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in row) {
                    val on = i == selected
                    val shape = RoundedCornerShape(14.dp)
                    Box(
                        Modifier.weight(1f).height(40.dp).clip(shape)
                            .background(if (on) Brush.verticalGradient(listOf(pm.goldSoft, pm.goldHi.copy(alpha = .55f))) else cardBrush())
                            .border(1.2.dp, if (on) Brush.verticalGradient(listOf(pm.goldHi, pm.goldBright)) else hairline(), shape)
                            .clickable { onSelect(i) },
                        contentAlignment = Alignment.Center,
                    ) { Text(timeLabel(Catalog.LEVEL_MOVE_SECONDS[i]), style = MaterialTheme.typography.labelMedium, color = if (on) pm.gold else pm.ink, maxLines = 1) }
                }
                if (row.last - row.first < 3) Spacer(Modifier.weight(1f))
            }
        }
    }
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
    var askNearby by remember { mutableStateOf(false) }
    fun withNearby(action: () -> Unit) {
        if (perms.all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }) action()
        else { pending = action; askNearby = true }
    }
    val solo = info.kind == GameKind.SOLO
    val hasOptions = info.options.isNotEmpty()
    val optIndex = info.options.indexOf(vm.option).coerceAtLeast(0)
    val onlineOk = !solo
    @Suppress("UNUSED_VARIABLE") val keepRecomposing = wallet.diamonds

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(info.name), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            // ── Thẻ giới thiệu game ──
            val heroShape = RoundedCornerShape(28.dp)
            Row(
                Modifier.fillMaxWidth().lift(heroShape, 12.dp, info.topic.color.onLight(.5f)).clip(heroShape)
                    .background(Brush.linearGradient(listOf(info.topic.color.copy(alpha = .34f), pm.surface, pm.surface), Offset(0f, 0f), Offset(900f, 800f)))
                    .border(1.dp, info.topic.color.copy(alpha = .45f), heroShape).padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(stringResource(info.topic.title), color = info.topic.color, filled = true)
                    Text(stringResource(info.name), style = MaterialTheme.typography.headlineMedium, color = pm.ink)
                    Text(stringResource(info.desc), style = MaterialTheme.typography.bodyMedium, color = pm.inkSoft)
                    Text(
                        stringResource(R.string.view_guide),
                        Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onGuide).padding(vertical = 2.dp),
                        style = MaterialTheme.typography.labelLarge, color = pm.gold, textDecoration = TextDecoration.Underline,
                    )
                    if (!app.wallet.noDiamonds()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

            // ── Số người chơi (chỉ Wall Go và Quoridor) ──
            if (hasOptions) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle(stringResource(R.string.players))
                    OptionChips(optionLabels(info), optIndex, { vm.selectOption(info.options[it]) })
                }
            }

            // ── Chơi với máy / bắt đầu ──
            ModeCard(
                if (solo) Ic.Play else Ic.Computer,
                stringResource(if (solo) R.string.solo_play else R.string.vs_computer),
                if (!solo && info.optionKind == OptionKind.PLAYERS && vm.option > 2) stringResource(R.string.vs_computers_note, vm.option - 1) else null,
            ) {
                if (!solo) {
                    SectionTitle(stringResource(R.string.bot_level))
                    LevelPicker(vm.botLevel, app.prefs.unlockedLevel(info.id)) { vm.selectLevel(it) }
                }
                GoldButton(
                    stringResource(R.string.start),
                    { if (solo) vm.startSudoku(Catalog.SUDOKU_LEVEL) else vm.startVsComputer() },
                    Modifier.fillMaxWidth(), if (solo) Ic.Play else Ic.Computer,
                )
            }

            // ── Chơi với bạn bè: gần nhau (Bluetooth / Wi-Fi) và qua internet ──
            if (!solo) {
                if (!onlineOk) {
                    ModeCard(Ic.Users, stringResource(R.string.play_friends), stringResource(R.string.online_two_only)) {}
                } else {
                    SectionTitle(stringResource(R.string.play_friends))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.pvp_time), style = MaterialTheme.typography.titleSmall, color = pm.ink)
                        TimePicker(vm.pvpTime) { vm.selectPvpTime(it) }
                        Text(stringResource(R.string.pvp_time_note), style = MaterialTheme.typography.bodySmall, color = pm.inkSoft)
                        // ván với bạn bè: mỗi người -1 kim cương, thắng +1; gói tháng / năm không mất kim cương
                        Text(stringResource(if (vm.wallet.currentPlan() == com.fws.playmind.data.Plan.NONE) R.string.room_cost else R.string.room_cost_plan), style = MaterialTheme.typography.bodySmall, color = pm.gold)
                    }
                    ModeCard(Ic.Nearby, stringResource(R.string.nearby), stringResource(R.string.nearby_card_desc)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlineButton(stringResource(R.string.create_room), { withNearby { if (vm.hostRoom(true)) onRoom() } }, Modifier.weight(1f))
                            OutlineButton(stringResource(R.string.join_room), { withNearby { if (vm.joinNearby()) onRoom() } }, Modifier.weight(1f))
                        }
                    }
                    ModeCard(Ic.Internet, stringResource(R.string.internet), stringResource(R.string.internet_card_desc)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlineButton(stringResource(R.string.create_room), { if (vm.hostRoom(false)) onRoom() }, Modifier.weight(1f))
                            OutlineButton(stringResource(R.string.join_room), { if (vm.joinNet()) onRoom() }, Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(Modifier.width(1.dp).height(24.dp))
        }
    }

    // Giải thích vì sao cần quyền "thiết bị ở gần" trước khi hệ thống hỏi (chỉ để tìm bạn qua Bluetooth/Wi-Fi, không thu thập vị trí)
    if (askNearby) PmDialog(
        stringResource(R.string.nearby_why_title), stringResource(R.string.nearby_why_text),
        stringResource(R.string.continue_label), { askNearby = false; launcher.launch(perms) },
        stringResource(R.string.cancel), { askNearby = false; pending = null },
        tone = DialogTone.INFO, icon = Ic.Radar,
    )
}
