package com.fws.playmind.ui

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.fws.playmind.PlayMindApp
import com.fws.playmind.R
import com.fws.playmind.catalog.GameInfo
import com.fws.playmind.match.Notice
import com.fws.playmind.match.SessionViewModel
import com.fws.playmind.net.NearbyTransport

@Composable
fun TopBar(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(pm.surface).border(1.dp, pm.outline, CircleShape), contentAlignment = Alignment.Center) {
            PmIconButton(Ic.Back, stringResource(R.string.back), onBack)
        }
        Text(title, Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.titleLarge, color = pm.ink, maxLines = 1)
        trailing()
    }
}

@Composable
fun LobbyScreen(app: PlayMindApp, vm: SessionViewModel, info: GameInfo, onBack: () -> Unit, onRoom: () -> Unit, onGuide: () -> Unit) {
    val ctx = LocalContext.current
    val wallet by app.wallet.state.collectAsState()
    val freeAvail = wallet.freeDay[info.id] != app.wallet.today()
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

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(info.name), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            val heroShape = RoundedCornerShape(26.dp)
            Box(
                Modifier.fillMaxWidth().shadow(10.dp, heroShape, ambientColor = pm.navy.copy(alpha = .25f), spotColor = pm.navy.copy(alpha = .45f))
                    .clip(heroShape).background(Brush.linearGradient(listOf(pm.navy2, pm.navy)))
                    .border(1.dp, pm.goldBright.copy(alpha = .5f), heroShape),
            ) {
                Box(Modifier.matchParentSize().drawBehind {
                    drawCircle(Brush.radialGradient(0f to Color(0x55F6DE8E), 1f to Color.Transparent, center = Offset(size.width * .82f, size.height * .45f), radius = size.width * .5f), size.width * .5f, Offset(size.width * .82f, size.height * .45f))
                })
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Eyebrow(stringResource(info.topic.title), color = pm.goldHi)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(info.name), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(info.desc), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = .82f))
                        Spacer(Modifier.height(10.dp))
                        Text(
                            stringResource(R.string.view_guide),
                            Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onGuide).padding(vertical = 2.dp),
                            style = MaterialTheme.typography.labelLarge, color = pm.goldHi, textDecoration = TextDecoration.Underline,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (freeAvail) { Icon(Ic.Check, null, Modifier.size(16.dp), tint = Color(0xFF5BE3C1)); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.free_today), style = MaterialTheme.typography.labelMedium, color = Color(0xFF5BE3C1)) }
                            else { GemMark(14.dp); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.costs_one), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .8f)) }
                        }
                    }
                    GameArt(info.id, Modifier.size(116.dp))
                }
            }
            Gap(16)
            SectionTitle(stringResource(R.string.vs_computer))
            Gap(10)
            SurfaceCard(Modifier.fillMaxWidth()) {
                // Chơi với máy: bạn luôn đi trước nên không cần chọn bên.
                GoldButton(stringResource(R.string.start), { vm.startVsComputer() }, Modifier.fillMaxWidth(), Ic.Computer)
            }
            Gap(20)
            SectionTitle(stringResource(R.string.play_friends))
            Gap(10)
            SurfaceCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Ic.Radar, null, Modifier.size(20.dp), tint = pm.gold); Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.nearby), style = MaterialTheme.typography.titleMedium, color = pm.ink)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlineButton(stringResource(R.string.create_room), { withNearby { if (vm.hostRoom(true)) onRoom() } }, Modifier.weight(1f))
                        OutlineButton(stringResource(R.string.join_room), { withNearby { if (vm.joinNearby()) onRoom() } }, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Ic.Wifi, null, Modifier.size(20.dp), tint = pm.gold); Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.internet), style = MaterialTheme.typography.titleMedium, color = pm.ink)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlineButton(stringResource(R.string.create_room), { if (vm.hostRoom(false)) onRoom() }, Modifier.weight(1f))
                        OutlineButton(stringResource(R.string.join_room), { if (vm.joinNet()) onRoom() }, Modifier.weight(1f))
                    }
                    Text(stringResource(R.string.internet_note), style = MaterialTheme.typography.labelMedium, color = pm.inkSoft)
                }
            }
            Gap(24)
        }
    }
}
