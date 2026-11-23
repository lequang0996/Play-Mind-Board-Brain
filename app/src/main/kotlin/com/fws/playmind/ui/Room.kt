package com.fws.playmind.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fws.playmind.R
import com.fws.playmind.match.Mode
import com.fws.playmind.match.Opp
import com.fws.playmind.match.SessionViewModel
import com.fws.playmind.net.FoundRoom
import com.fws.playmind.net.LinkState
import com.fws.playmind.net.Signal
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

fun scanQr(ctx: Context, onResult: (String) -> Unit) {
    val opts = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    GmsBarcodeScanning.getClient(ctx, opts).startScan().addOnSuccessListener { b -> b.rawValue?.let(onResult) }
}

private fun shareText(ctx: Context, text: String) {
    val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    ctx.startActivity(Intent.createChooser(i, null))
}

private val avatarColors = listOf(Color(0xFF14937A), Color(0xFF5348C9), Color(0xFFC27A0A), Color(0xFFC93E6B), Color(0xFF2B7BD6), Color(0xFF8A4FD0))

@Composable
fun Avatar(name: String, size: Int = 46) {
    val c = avatarColors[(name.hashCode() and 0x7fffffff) % avatarColors.size]
    Box(Modifier.size(size.dp).clip(CircleShape).background(c), contentAlignment = Alignment.Center) {
        Text(name.firstOrNull()?.uppercase() ?: "?", color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Seat(name: String?, you: Boolean, waitingLabel: String) {
    SurfaceCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (name != null) {
                Avatar(name)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, color = pm.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (you) Text(stringResource(R.string.you), style = MaterialTheme.typography.labelMedium, color = pm.gold)
                }
            } else {
                Box(Modifier.size(46.dp).clip(CircleShape).background(pm.surfaceHigh), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = pm.gold, strokeWidth = 2.dp)
                }
                Spacer(Modifier.width(12.dp))
                Text(waitingLabel, style = MaterialTheme.typography.bodyMedium, color = pm.inkSoft)
            }
        }
    }
}

@Composable
private fun CodeField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value, onChange, modifier.fillMaxWidth(), label = { Text(label) }, maxLines = 4,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = pm.gold, unfocusedBorderColor = pm.outline, focusedLabelColor = pm.gold,
            focusedTextColor = pm.ink, unfocusedTextColor = pm.ink, cursorColor = pm.gold,
        ),
    )
}

/** Hộp thoại hiển thị mã + QR để trao đổi, kèm ô dán/quét mã của đối phương. */
@Composable
private fun CodeExchangeDialog(
    title: String, myCodeLabel: String, myCode: String, busy: Boolean,
    otherLabel: String?, actionLabel: String, onAction: (String) -> Unit, onClose: () -> Unit,
) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    var other by remember { mutableStateOf("") }
    val qr = remember(myCode) { if (myCode.isNotEmpty()) Signal.qr(myCode) else null }
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.clip(RoundedCornerShape(26.dp)).background(pm.surface).padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = pm.ink)
                PmIconButton(Ic.Close, stringResource(R.string.close), onClose)
            }
            if (myCodeLabel.isNotEmpty()) {
                Text(myCodeLabel, style = MaterialTheme.typography.labelMedium, color = pm.inkSoft)
                if (busy || myCode.isEmpty()) Row(Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(28.dp), color = pm.gold, strokeWidth = 3.dp)
                } else {
                    qr?.let {
                        Image(it.asImageBitmap(), null, Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(10.dp))
                    }
                    Text(myCode, style = MaterialTheme.typography.labelMedium, color = pm.inkSoft, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlineButton(stringResource(R.string.copy), { clip.setText(AnnotatedString(myCode)) }, Modifier.weight(1f), Ic.Copy)
                        OutlineButton(stringResource(R.string.share), { shareText(ctx, myCode) }, Modifier.weight(1f), Ic.Qr)
                    }
                }
            }
            if (otherLabel != null) {
                CodeField(other, { other = it }, otherLabel)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlineButton(stringResource(R.string.paste), { clip.getText()?.text?.let { other = it } }, Modifier.weight(1f))
                    OutlineButton(stringResource(R.string.scan_qr), { scanQr(ctx) { other = it } }, Modifier.weight(1f), Ic.Scan)
                }
                GoldButton(actionLabel, { onAction(other) }, Modifier.fillMaxWidth(), enabled = other.isNotBlank())
            }
        }
    }
}

@Composable
fun RoomScreen(vm: SessionViewModel, onLeave: () -> Unit) {
    val link by vm.linkState.collectAsState()
    val rooms by vm.nearbyRooms.collectAsState()
    val nearbyErr by vm.nearbyError.collectAsState()
    val nearbyCode by vm.nearbyErrorCode.collectAsState()
    val ctx = LocalContext.current
    var showInvite by remember { mutableStateOf(false) }
    DisposableEffect(showInvite) { vm.inviteOpen = showInvite; onDispose { vm.inviteOpen = false } }

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.room), onLeave)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (vm.mode) {
                Mode.HOST_NEARBY, Mode.HOST_NET -> {
                    Seat(vm.prefs.nickname, true, "")
                    Seat(if (vm.opp == Opp.NONE) null else vm.oppName, false, stringResource(R.string.waiting_opponent))
                    if (nearbyErr) Text(stringResource(R.string.nearby_unavailable) + " [$nearbyCode]", color = pm.danger, style = MaterialTheme.typography.bodyMedium)
                    if (vm.mode == Mode.HOST_NET && vm.opp != Opp.HUMAN) {
                        OutlineButton(stringResource(R.string.invite), { showInvite = true }, Modifier.fillMaxWidth(), Ic.Qr)
                        if (link == LinkState.CONNECTING) Text(stringResource(R.string.connecting), color = pm.inkSoft, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (vm.mode == Mode.HOST_NEARBY && vm.opp == Opp.NONE)
                        Text(stringResource(R.string.nearby_host_hint), color = pm.inkSoft, style = MaterialTheme.typography.bodyMedium)
                    Gap(4)
                    GoldButton(stringResource(R.string.start_game), { vm.hostStart() }, Modifier.fillMaxWidth(), Ic.Play, enabled = vm.opp != Opp.NONE)
                    if (vm.opp != Opp.NONE) OutlineButton(stringResource(R.string.kick), { vm.kick() }, Modifier.fillMaxWidth())
                }
                Mode.JOIN_NEARBY -> {
                    if (vm.guestConnected) {
                        Seat(vm.oppName, false, "")
                        Text(stringResource(R.string.waiting_host_start), color = pm.inkSoft, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = pm.gold, strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.searching_rooms), color = pm.inkSoft, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (nearbyErr) Text(stringResource(R.string.nearby_unavailable) + " [$nearbyCode]", color = pm.danger, style = MaterialTheme.typography.bodyMedium)
                        rooms.forEach { r -> RoomRow(r, link == LinkState.CONNECTING) { vm.connectTo(r) } }
                    }
                }
                Mode.JOIN_NET -> {
                    if (vm.guestConnected) {
                        Seat(vm.oppName, false, "")
                        Text(stringResource(R.string.waiting_host_start), color = pm.inkSoft, style = MaterialTheme.typography.bodyMedium)
                    } else if (vm.replyCode.isEmpty()) {
                        Text(stringResource(R.string.join_net_hint), color = pm.inkSoft, style = MaterialTheme.typography.bodyMedium)
                        GoldButton(stringResource(R.string.enter_invite), { showInvite = true }, Modifier.fillMaxWidth(), Ic.Qr)
                    } else {
                        Text(stringResource(R.string.reply_hint), color = pm.inkSoft, style = MaterialTheme.typography.bodyMedium)
                        OutlineButton(stringResource(R.string.show_reply), { showInvite = true }, Modifier.fillMaxWidth(), Ic.Qr)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = pm.gold, strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.connecting), color = pm.inkSoft, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                else -> {}
            }
            Gap(20)
        }
    }

    if (showInvite) when (vm.mode) {
        Mode.HOST_NET -> CodeExchangeDialog(
            stringResource(R.string.invite), stringResource(R.string.your_invite_code), vm.inviteCode, vm.inviteBusy,
            stringResource(R.string.paste_reply), stringResource(R.string.connect), { vm.acceptReply(it); showInvite = false }, { showInvite = false },
        )
        Mode.JOIN_NET -> CodeExchangeDialog(
            stringResource(if (vm.replyCode.isEmpty()) R.string.enter_invite else R.string.show_reply),
            if (vm.replyCode.isEmpty()) "" else stringResource(R.string.your_reply_code), vm.replyCode, vm.inviteBusy,
            if (vm.replyCode.isEmpty()) stringResource(R.string.paste_invite) else null,
            stringResource(R.string.create_reply), { vm.submitInvite(it) }, { showInvite = false },
        )
        else -> showInvite = false
    }
}

@Composable
private fun RoomRow(r: FoundRoom, busy: Boolean, onClick: () -> Unit) {
    SurfaceCard(Modifier.fillMaxWidth(), onClick = { if (!busy) onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(r.host, 40); Spacer(Modifier.width(12.dp))
            Text(r.host, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = pm.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Ic.Chevron, null, tint = pm.inkSoft)
        }
    }
}
