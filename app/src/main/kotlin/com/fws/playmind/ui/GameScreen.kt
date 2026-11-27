package com.fws.playmind.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fws.playmind.PlayMindApp
import com.fws.playmind.R
import com.fws.playmind.catalog.GameInfo
import com.fws.playmind.core.EndReason
import com.fws.playmind.core.GoGame
import com.fws.playmind.core.Outcome
import com.fws.playmind.core.QuoridorGame
import com.fws.playmind.core.WallGoGame
import com.fws.playmind.match.Mode
import com.fws.playmind.match.Opp
import com.fws.playmind.match.Phase
import com.fws.playmind.match.SessionViewModel

/** Tên màu/phe của ghế [seat] (0 và 1 lấy từ danh mục game, 2 và 3 là xanh lá / vàng). */
@Composable
internal fun sideLabel(info: GameInfo, seat: Int): String = stringResource(
    when (seat) { 0 -> info.firstSide; 1 -> info.secondSide; 2 -> R.string.side_green; else -> R.string.side_yellow },
)

/** Tên hiển thị của ghế [seat] trong ván hiện tại. */
@Composable
internal fun seatName(vm: SessionViewModel, info: GameInfo, seat: Int): String {
    if (vm.mode == Mode.LOCAL) return stringResource(R.string.player_n, seat + 1)
    if (vm.playerCount > 2 && seat < vm.seatNames.size) return vm.seatNames[seat]
    return if (seat == vm.mySide) vm.prefs.nickname
    else if (vm.opp == Opp.COMPUTER) stringResource(R.string.computer) else vm.oppName
}

/** Dòng thông tin phụ của một ghế: lãnh thổ (Wall Go), số tường còn lại (Quoridor), quân đã bắt (cờ vây). */
@Composable
private fun seatInfo(vm: SessionViewModel, seat: Int): String? {
    @Suppress("UNUSED_VARIABLE") val t = vm.tick
    return when (val g = vm.game) {
        is WallGoGame -> stringResource(R.string.st_cells, g.territory()[seat])
        is QuoridorGame -> stringResource(R.string.st_walls, g.wallsOf(seat))
        is GoGame -> stringResource(R.string.st_captured, g.capturedBy(seat))
        else -> null
    }
}

@Composable
private fun PlayerBar(info: GameInfo, seat: Int, name: String, label: String, active: Boolean, status: String?, extra: String?, showCheck: Boolean) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp).lift(shape, 6.dp).clip(shape)
            .background(if (active) Brush.linearGradient(listOf(pm.goldSoft, pm.surface)) else Brush.linearGradient(listOf(pm.surface, pm.surface)))
            .border(if (active) 1.5.dp else 1.dp, if (active) pm.goldBright else pm.outline, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(pm.bg).border(1.dp, pm.outline, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            SideBadge(info.id, seat)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = pm.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelMedium, color = pm.inkSoft)
        }
        if (extra != null) Text(extra, Modifier.padding(end = 10.dp), style = MaterialTheme.typography.labelLarge, color = pm.inkSoft)
        if (showCheck) Text(stringResource(R.string.check), Modifier.clip(RoundedCornerShape(50)).background(pm.danger).padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium, color = Color.White)
        else if (status != null) Text(status, style = MaterialTheme.typography.labelMedium, color = if (active) pm.gold else pm.inkSoft)
        if (active && status == null) Box(Modifier.size(10.dp).clip(CircleShape).background(goldBrush()))
    }
}

/** Dải người chơi gọn cho ván 3-4 người: mỗi ghế một ô, ô của người đang đi sáng viền vàng. */
@Composable
private fun SeatStrip(vm: SessionViewModel, info: GameInfo, turn: Int, over: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (seat in 0 until vm.playerCount) {
            val active = !over && turn == seat
            val shape = RoundedCornerShape(16.dp)
            Column(
                Modifier.weight(1f).lift(shape, 6.dp).clip(shape).background(if (active) pm.goldSoft else pm.surface)
                    .border(if (active) 1.5.dp else 1.dp, if (active) pm.goldBright else pm.outline, shape).padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SideBadge(info.id, seat, Modifier.size(30.dp))
                Text(seatName(vm, info, seat), style = MaterialTheme.typography.labelMedium, color = pm.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(seatInfo(vm, seat) ?: sideLabel(info, seat), style = MaterialTheme.typography.labelSmall, color = if (active) pm.gold else pm.inkSoft, maxLines = 1)
            }
        }
    }
}

@Composable
fun GameScreen(app: PlayMindApp, vm: SessionViewModel, info: GameInfo, onExit: () -> Unit) {
    val activity = LocalContext.current as Activity
    val g = vm.game
    val tick = vm.tick
    var confirmResign by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    val over = vm.phase == Phase.OVER
    BackHandler { if (over) onExit() else confirmLeave = true }
    if (g == null) return

    val local = vm.mode == Mode.LOCAL
    val multi = vm.playerCount > 2
    val topSeat = if (local) 1 else vm.oppSide
    val bottomSeat = if (local) 0 else vm.mySide
    val myTurn = !over && g.turn == bottomSeat
    val oppTurn = !over && g.turn == topSeat
    val inCheck = !over && g.inCheck()

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(info.name), { if (over) onExit() else confirmLeave = true }) {
            if (!over) PmIconButton(Ic.Flag, stringResource(R.string.resign), { confirmResign = true })
        }
        if (multi) SeatStrip(vm, info, g.turn, over)
        else PlayerBar(
            info, topSeat, seatName(vm, info, topSeat), sideLabel(info, topSeat), oppTurn,
            if (oppTurn && vm.botThinking) stringResource(R.string.thinking) else if (oppTurn) stringResource(R.string.their_turn) else null,
            seatInfo(vm, topSeat), inCheck && oppTurn,
        )
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
            when (info.id) {
                "chess" -> ChessBoardView(vm)
                "xiangqi" -> XiangqiBoardView(vm)
                "go" -> GoBoardView(vm)
                "quoridor" -> QuoridorBoardView(vm)
                "wallgo" -> WallGoBoardView(vm)
                else -> GomokuBoardView(vm, Modifier.fillMaxSize())   // bàn vô hạn: lấp đầy khoảng trống, kéo/thu phóng được
            }
        }
        if (multi) {
            val who = seatName(vm, info, g.turn)
            Text(
                if (over) "" else if (vm.botThinking) stringResource(R.string.thinking) else if (local || g.turn != vm.mySide) stringResource(R.string.turn_of, who) else stringResource(R.string.your_turn),
                Modifier.fillMaxWidth().padding(vertical = 4.dp), style = MaterialTheme.typography.labelLarge, color = pm.gold, textAlign = TextAlign.Center,
            )
        } else PlayerBar(
            info, bottomSeat, seatName(vm, info, bottomSeat), sideLabel(info, bottomSeat), myTurn,
            if (myTurn) stringResource(R.string.your_turn) else null, seatInfo(vm, bottomSeat), inCheck && myTurn,
        )
        Spacer(Modifier.height(10.dp))
    }

    if (over) ResultDialog(app, vm, info, activity, onExit)

    if (confirmResign) SimpleDialog(stringResource(R.string.resign), stringResource(R.string.resign_confirm), stringResource(R.string.resign), { confirmResign = false; vm.resign() }, stringResource(R.string.cancel), { confirmResign = false }, tone = DialogTone.DANGER)
    if (confirmLeave) SimpleDialog(stringResource(R.string.leave_game), stringResource(if (local) R.string.leave_confirm_local else R.string.leave_confirm), stringResource(R.string.leave), { confirmLeave = false; onExit() }, stringResource(R.string.cancel), { confirmLeave = false }, tone = DialogTone.WARN)
    @Suppress("UNUSED_EXPRESSION") tick
}

@Composable
private fun reasonText(info: GameInfo, r: EndReason): String = stringResource(when (r) {
    EndReason.CHECKMATE -> R.string.reason_checkmate
    EndReason.STALEMATE -> R.string.reason_stalemate
    EndReason.NOMOVES -> R.string.reason_nomoves
    EndReason.REPETITION -> R.string.reason_repetition
    EndReason.FIFTY -> R.string.reason_fifty
    EndReason.MATERIAL -> R.string.reason_material
    EndReason.FIVE -> R.string.reason_five
    EndReason.FULL -> R.string.reason_full
    EndReason.RESIGN -> R.string.reason_resign
    EndReason.LEAVE -> R.string.reason_leave
    EndReason.TERRITORY -> R.string.reason_territory
    EndReason.NONE -> if (info.id == "quoridor") R.string.reason_goal else R.string.empty
})

@Composable
private fun ResultDialog(app: PlayMindApp, vm: SessionViewModel, info: GameInfo, activity: Activity, onExit: () -> Unit) {
    val g = vm.game ?: return
    val local = vm.mode == Mode.LOCAL
    val draw = g.result == Outcome.DRAW
    val win = !draw && (g.result == Outcome.WIN_FIRST) == (vm.mySide == 0)
    val winnerSeat = when (g) { is WallGoGame -> g.winner; is QuoridorGame -> g.winner; else -> if (g.result == Outcome.WIN_FIRST) 0 else if (g.result == Outcome.WIN_SECOND) 1 else -1 }
    val title = when {
        local && winnerSeat >= 0 -> stringResource(R.string.winner_is, seatName(vm, info, winnerSeat))
        draw -> stringResource(R.string.result_draw)
        win -> stringResource(R.string.result_win)
        else -> stringResource(R.string.result_lose)
    }
    val reward = vm.reward
    val exit = { app.ads.onMatchFinished(activity); onExit() }
    val tone = if (win || (local && winnerSeat >= 0)) pm.goldBright else if (draw) pm.royal else pm.inkSoft
    Dialog(onDismissRequest = {}) {
        val frame = RoundedCornerShape(28.dp)
        Column(
            Modifier.fillMaxWidth().clip(frame).background(pm.surface).border(1.dp, pm.outline, frame)
                .background(Brush.verticalGradient(listOf(tone.copy(alpha = .16f), Color.Transparent), endY = 420f)).padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(68.dp).clip(CircleShape).background(Brush.radialGradient(listOf(tone.copy(alpha = .35f), tone.copy(alpha = .08f)))).border(1.dp, tone.copy(alpha = .7f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(if (win || (local && winnerSeat >= 0)) Ic.Trophy else if (draw) Ic.Check else Ic.Flag, null, Modifier.size(32.dp), tint = tone) }
            Text(title, style = MaterialTheme.typography.headlineMedium, color = pm.ink, textAlign = TextAlign.Center)
            val rt = reasonText(info, g.reason)
            if (rt.isNotEmpty()) Text(rt, style = MaterialTheme.typography.bodyMedium, color = pm.inkSoft, textAlign = TextAlign.Center)
            // Bảng điểm cuối ván
            if (g is WallGoGame && g.reason == EndReason.TERRITORY) FinalScores(vm, info, g.finalScores.toList())
            if (g is GoGame && g.reason == EndReason.TERRITORY) {
                Text(stringResource(R.string.go_score, g.finalBlack, g.finalWhite, "%.1f".format(GoGame.KOMI)), style = MaterialTheme.typography.bodyMedium, color = pm.ink, textAlign = TextAlign.Center)
            }
            if (vm.verifying) Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), color = pm.gold, strokeWidth = 2.dp); Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.verifying), style = MaterialTheme.typography.labelMedium, color = pm.inkSoft)
            }
            if (reward != null && reward.diamonds > 0) {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(pm.surfaceHigh).border(1.dp, pm.goldBright.copy(alpha = .7f), RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GemMark(20.dp)
                    Text(pluralStringResource(R.plurals.reward_diamonds, reward.diamonds, reward.diamonds), style = MaterialTheme.typography.titleMedium, color = pm.ink)
                }
                if (reward.bonus > 0) Text(pluralStringResource(R.plurals.streak_bonus, reward.bonus, reward.bonus), style = MaterialTheme.typography.labelMedium, color = pm.gold)
            }
            if (reward != null && reward.blocked) Text(stringResource(R.string.rewards_paused), style = MaterialTheme.typography.labelMedium, color = pm.danger)
            if (local) Text(stringResource(R.string.local_no_reward), style = MaterialTheme.typography.labelMedium, color = pm.inkSoft, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            val rematchLabel = when {
                vm.opp == Opp.HUMAN && vm.rematchAsked -> stringResource(R.string.waiting_rematch)
                vm.opp == Opp.HUMAN && vm.oppAskedRematch -> stringResource(R.string.accept_rematch)
                else -> stringResource(R.string.play_again)
            }
            val canRematch = (vm.opp != Opp.NONE || local) && !vm.verifying && !(vm.opp == Opp.HUMAN && vm.rematchAsked)
            GoldButton(rematchLabel, { vm.rematch() }, Modifier.fillMaxWidth(), Ic.Refresh, enabled = canRematch)
            OutlineButton(stringResource(R.string.back_to_lobby), exit, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun FinalScores(vm: SessionViewModel, info: GameInfo, scores: List<Int>) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        scores.forEachIndexed { seat, pts ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SideBadge(info.id, seat, Modifier.size(26.dp))
                Text(seatName(vm, info, seat), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = pm.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.st_cells, pts), style = MaterialTheme.typography.labelLarge, color = pm.gold)
            }
        }
    }
}
