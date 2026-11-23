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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fws.playmind.PlayMindApp
import com.fws.playmind.R
import com.fws.playmind.catalog.GameInfo
import com.fws.playmind.core.EndReason
import com.fws.playmind.core.Outcome
import com.fws.playmind.match.Opp
import com.fws.playmind.match.Phase
import com.fws.playmind.match.SessionViewModel

@Composable
private fun PlayerBar(info: GameInfo, side: Int, name: String, active: Boolean, status: String?, showCheck: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp).clip(RoundedCornerShape(16.dp))
            .background(if (active) pm.goldSoft else pm.surface).border(1.dp, if (active) pm.gold.copy(alpha = .5f) else pm.outline, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SideBadge(info.id, side)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = pm.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(if (side == 0) info.firstSide else info.secondSide), style = MaterialTheme.typography.labelMedium, color = pm.inkSoft)
        }
        if (showCheck) Text(stringResource(R.string.check), Modifier.clip(RoundedCornerShape(50)).background(pm.danger).padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color.White)
        else if (status != null) Text(status, style = MaterialTheme.typography.labelMedium, color = if (active) pm.gold else pm.inkSoft)
        if (active && status == null) Box(Modifier.size(10.dp).clip(CircleShape).background(pm.gold))
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

    val oppName = if (vm.opp == Opp.COMPUTER) stringResource(R.string.computer) else vm.oppName
    val myTurn = !over && g.turn == vm.mySide
    val oppTurn = !over && g.turn != vm.mySide
    val myName = vm.prefs.nickname
    val inCheck = !over && g.inCheck()

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(info.name), { if (over) onExit() else confirmLeave = true }) {
            if (!over) PmIconButton(Ic.Flag, stringResource(R.string.resign), { confirmResign = true })
        }
        PlayerBar(info, vm.oppSide, oppName, oppTurn, if (oppTurn && vm.botThinking) stringResource(R.string.thinking) else if (oppTurn) stringResource(R.string.their_turn) else null, inCheck && oppTurn)
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            when (info.id) {
                "chess" -> ChessBoardView(vm)
                "xiangqi" -> XiangqiBoardView(vm)
                else -> GomokuBoardView(vm)
            }
        }
        PlayerBar(info, vm.mySide, myName, myTurn, if (myTurn) stringResource(R.string.your_turn) else null, inCheck && myTurn)
        Spacer(Modifier.height(10.dp))
    }

    if (over) ResultDialog(app, vm, info, activity, onExit)

    if (confirmResign) SimpleDialog(stringResource(R.string.resign), stringResource(R.string.resign_confirm), stringResource(R.string.resign), { confirmResign = false; vm.resign() }, stringResource(R.string.cancel), { confirmResign = false })
    if (confirmLeave) SimpleDialog(stringResource(R.string.leave_game), stringResource(R.string.leave_confirm), stringResource(R.string.leave), { confirmLeave = false; onExit() }, stringResource(R.string.cancel), { confirmLeave = false })
    @Suppress("UNUSED_EXPRESSION") tick
}

@Composable
private fun reasonText(r: EndReason): String = stringResource(when (r) {
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
    EndReason.NONE -> R.string.empty
})

@Composable
private fun ResultDialog(app: PlayMindApp, vm: SessionViewModel, info: GameInfo, activity: Activity, onExit: () -> Unit) {
    val g = vm.game ?: return
    val draw = g.result == Outcome.DRAW
    val win = !draw && (g.result == Outcome.WIN_FIRST) == (vm.mySide == 0)
    val title = stringResource(if (draw) R.string.result_draw else if (win) R.string.result_win else R.string.result_lose)
    val reward = vm.reward
    val exit = { app.ads.onMatchFinished(activity); onExit() }
    Dialog(onDismissRequest = {}) {
        Column(
            Modifier.clip(RoundedCornerShape(28.dp)).background(pm.surface).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(goldBrush()), contentAlignment = Alignment.Center) {
                Icon(if (win) Ic.Crown else if (draw) Ic.Check else Ic.Flag, null, Modifier.size(32.dp), tint = if (pm.dark) androidx.compose.ui.graphics.Color(0xFF241A04) else androidx.compose.ui.graphics.Color.White)
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, color = pm.ink)
            Text(reasonText(g.reason), style = MaterialTheme.typography.bodyMedium, color = pm.inkSoft)
            if (vm.verifying) Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), color = pm.gold, strokeWidth = 2.dp); Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.verifying), style = MaterialTheme.typography.labelMedium, color = pm.inkSoft)
            }
            if (reward != null && reward.diamonds > 0) {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(pm.goldSoft).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GemMark(20.dp)
                    Text(pluralStringResource(R.plurals.reward_diamonds, reward.diamonds, reward.diamonds), style = MaterialTheme.typography.titleMedium, color = pm.ink)
                }
                if (reward.bonus > 0) Text(pluralStringResource(R.plurals.streak_bonus, reward.bonus, reward.bonus), style = MaterialTheme.typography.labelMedium, color = pm.gold)
            }
            if (reward != null && reward.blocked) Text(stringResource(R.string.rewards_paused), style = MaterialTheme.typography.labelMedium, color = pm.danger)
            Spacer(Modifier.height(4.dp))
            val rematchLabel = when {
                vm.opp == Opp.HUMAN && vm.rematchAsked -> stringResource(R.string.waiting_rematch)
                vm.opp == Opp.HUMAN && vm.oppAskedRematch -> stringResource(R.string.accept_rematch)
                else -> stringResource(R.string.play_again)
            }
            val canRematch = vm.opp != Opp.NONE && !vm.verifying && !(vm.opp == Opp.HUMAN && vm.rematchAsked)
            GoldButton(rematchLabel, { vm.rematch() }, Modifier.fillMaxWidth(), Ic.Refresh, enabled = canRematch)
            OutlineButton(stringResource(R.string.back_to_lobby), exit, Modifier.fillMaxWidth())
        }
    }
}
