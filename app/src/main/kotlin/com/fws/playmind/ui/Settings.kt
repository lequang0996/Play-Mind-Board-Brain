package com.fws.playmind.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fws.playmind.BuildConfig
import com.fws.playmind.PlayMindApp
import com.fws.playmind.R
import com.fws.playmind.catalog.Catalog

/** Danh sách chọn ngôn ngữ: cờ + tên gốc, kèm tên ngôn ngữ đó theo ngôn ngữ giao diện hiện tại, vd. "English (Tiếng Anh)". */
@Composable
fun LanguageList(selected: String, onSelect: (String) -> Unit) {
    val ui = LocalConfiguration.current.locales[0]
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Langs.all.forEach { (tag, native) ->
            val on = tag == selected
            val shape = RoundedCornerShape(16.dp)
            val translated = Langs.localizedName(tag, ui)
            Row(
                Modifier.fillMaxWidth().clip(shape)
                    .background(if (on) pm.goldSoft else pm.surfaceHigh).border(if (on) 1.5.dp else 1.dp, if (on) pm.goldBright else pm.outline, shape)
                    .clickable { onSelect(tag) }.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(pm.bg).border(1.dp, pm.outline, CircleShape), contentAlignment = Alignment.Center) {
                    Text(Langs.flag(tag), fontSize = 20.sp)
                }
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = pm.ink, fontWeight = FontWeight.Bold)) { append(native) }
                        if (!translated.equals(native, ignoreCase = true)) withStyle(SpanStyle(color = pm.inkSoft, fontWeight = FontWeight.Medium, fontSize = 13.sp)) { append("  ($translated)") }
                    },
                    Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                )
                if (on) Icon(Ic.Check, null, Modifier.size(20.dp), tint = pm.gold)
            }
        }
    }
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val src = rememberPress()
    Row(
        Modifier.bounce(src, .98f).fillMaxWidth().lift(shape, 4.dp).clip(shape).background(cardBrush()).border(1.dp, hairline(), shape).tap(src, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = pm.gold)
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = pm.ink)
        Icon(Ic.Chevron, null, Modifier.size(18.dp), tint = pm.inkSoft)
    }
}

@Composable
private fun ToggleRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().lift(shape, 4.dp).clip(shape).background(cardBrush()).border(1.dp, hairline(), shape)
            .clickable { onChange(!on) }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = pm.gold)
        Text(text, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = pm.ink)
        Switch(
            on, onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = pm.surface, checkedTrackColor = pm.gold, checkedBorderColor = pm.gold,
                uncheckedThumbColor = pm.inkSoft, uncheckedTrackColor = pm.surfaceHigh, uncheckedBorderColor = pm.outline,
            ),
        )
    }
}

@Composable
fun SettingsScreen(app: PlayMindApp, onBack: () -> Unit, onGuide: () -> Unit, onShop: () -> Unit) {
    val ctx = LocalContext.current
    val activity = ctx as Activity
    var lang by remember { mutableStateOf(Langs.current()) }
    var nick by remember { mutableStateOf(app.prefs.nickname) }
    var showLicenses by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.settings), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(stringResource(R.string.language))
            LanguageList(lang) { lang = it; Langs.apply(it) }

            Spacer(Modifier.height(6.dp))
            SectionTitle(stringResource(R.string.nickname))
            OutlinedTextField(
                nick, { nick = it.take(16); app.prefs.updateNickname(nick) }, Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = pm.surface, unfocusedContainerColor = pm.surface, focusedBorderColor = pm.goldBright, unfocusedBorderColor = pm.outline,
                    focusedTextColor = pm.ink, unfocusedTextColor = pm.ink, cursorColor = pm.gold,
                ),
            )

            Spacer(Modifier.height(6.dp))
            SectionTitle(stringResource(R.string.sound))
            ToggleRow(Ic.Music, stringResource(R.string.music), app.audio.musicOn) { app.audio.updateMusic(it) }

            Spacer(Modifier.height(6.dp))
            SettingsRow(Ic.Bag, stringResource(R.string.shop), onShop)
            SettingsRow(Ic.Book, stringResource(R.string.how_to_use), onGuide)
            SettingsRow(Ic.Shield, stringResource(R.string.privacy_policy)) { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_URL))) }
            if (app.ads.privacyOptionsRequired(activity)) SettingsRow(Ic.Info, stringResource(R.string.privacy_options)) { app.ads.showPrivacyOptions(activity) }
            SettingsRow(Ic.Info, stringResource(R.string.licenses)) { showLicenses = true }
            Spacer(Modifier.height(20.dp))
        }
    }
    if (showLicenses) LicensesDialog { showLicenses = false }
}

/** Thông báo giấy phép bên thứ ba (KataGo, mạng CC0, Eigen, phông chữ...), đọc từ assets/third_party_notices.txt. */
@Composable
private fun LicensesDialog(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val text = remember { try { ctx.assets.open("third_party_notices.txt").bufferedReader().use { it.readText() } } catch (_: Exception) { "" } }
    Dialog(onDismissRequest = onClose) {
        val shape = RoundedCornerShape(24.dp)
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(.85f).clip(shape).background(pm.surface).border(1.dp, pm.outline, shape).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.licenses), style = MaterialTheme.typography.titleMedium, color = pm.ink)
            Text(
                text, Modifier.weight(1f).verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = pm.inkSoft,
            )
            OutlineButton(stringResource(R.string.close), onClose, Modifier.fillMaxWidth())
        }
    }
}

/**
 * Hiển thị văn bản hướng dẫn dạng "dòng mở đầu + các dòng bắt đầu bằng •": mỗi gạch đầu dòng thành một hàng có chấm vàng,
 * phần trước dấu ":" (nếu ngắn) được in đậm làm nhãn.
 */
@Composable
fun GuideBody(text: String) {
    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (line in lines) {
            if (line.startsWith("•")) {
                val body = line.removePrefix("•").trim()
                val colon = body.indexOf(':')
                val hasLabel = colon in 1..30
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.padding(top = 8.dp).size(6.dp).clip(CircleShape).background(pm.goldBright))
                    Text(
                        buildAnnotatedString {
                            if (hasLabel) {
                                withStyle(SpanStyle(color = pm.ink, fontWeight = FontWeight.ExtraBold)) { append(body.substring(0, colon + 1)) }
                                withStyle(SpanStyle(color = pm.inkSoft)) { append(body.substring(colon + 1)) }
                            } else withStyle(SpanStyle(color = pm.inkSoft)) { append(body) }
                        },
                        Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else Text(line, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = pm.ink)
        }
    }
}

@Composable
fun GuideScreen(onBack: () -> Unit, focusGame: String?) {
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.how_to_use), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (focusGame == null) {
                // Từ trang chủ / cài đặt: hướng dẫn dùng app
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(pm.goldSoft).border(1.dp, pm.goldBright.copy(alpha = .6f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Ic.Book, null, Modifier.size(20.dp), tint = pm.gold)
                            }
                            Text(stringResource(R.string.guide_app_title), style = MaterialTheme.typography.headlineSmall, color = pm.ink)
                        }
                        GuideBody(stringResource(R.string.guide_app))
                    }
                }
            } else {
                // Từ màn hình game: luật chơi của riêng game đó
                val g = Catalog.byId(focusGame)
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(g.topic.color.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                                GameArt(g.id, Modifier.size(52.dp))
                            }
                            Text(stringResource(g.name), style = MaterialTheme.typography.headlineSmall, color = pm.ink)
                        }
                        GuideBody(stringResource(g.guide))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
