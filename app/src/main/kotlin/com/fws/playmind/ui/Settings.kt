package com.fws.playmind.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fws.playmind.BuildConfig
import com.fws.playmind.PlayMindApp
import com.fws.playmind.R
import com.fws.playmind.catalog.Catalog

@Composable
fun LanguageList(selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Langs.all.forEach { (tag, name) ->
            val on = tag == selected
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (on) pm.goldSoft else pm.surfaceHigh).clickable { onSelect(tag) }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = pm.ink)
                if (on) Icon(Ic.Check, null, Modifier.size(20.dp), tint = pm.gold)
            }
        }
    }
}

private fun Modifier.background(c: androidx.compose.ui.graphics.Color) = this.then(Modifier.background(c))

@Composable
fun SettingsScreen(app: PlayMindApp, onBack: () -> Unit, onGuide: () -> Unit) {
    val ctx = LocalContext.current
    val activity = ctx as Activity
    var lang by remember { mutableStateOf(Langs.current()) }
    var nick by remember { mutableStateOf(app.prefs.nickname) }

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.settings), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle(stringResource(R.string.language))
            LanguageList(lang) { lang = it; Langs.apply(it) }

            SectionTitle(stringResource(R.string.theme))
            Segmented(
                listOf(stringResource(R.string.theme_system), stringResource(R.string.theme_light), stringResource(R.string.theme_dark)),
                app.prefs.theme, { app.prefs.updateTheme(it) }, Modifier.fillMaxWidth(),
            )

            SectionTitle(stringResource(R.string.nickname))
            OutlinedTextField(
                nick, { nick = it.take(16); app.prefs.updateNickname(nick) }, Modifier.fillMaxWidth(), singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = pm.gold, unfocusedBorderColor = pm.outline, focusedTextColor = pm.ink, unfocusedTextColor = pm.ink, cursorColor = pm.gold),
            )

            SectionTitle(stringResource(R.string.about))
            SurfaceCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.about_text), style = MaterialTheme.typography.bodyMedium, color = pm.inkSoft)
                    Text("v${BuildConfig.VERSION_NAME}  ·  FWs  ·  17eathday@gmail.com", style = MaterialTheme.typography.labelMedium, color = pm.inkSoft)
                }
            }
            OutlineButton(stringResource(R.string.how_to_use), onGuide, Modifier.fillMaxWidth(), Ic.Help)
            OutlineButton(stringResource(R.string.privacy_policy), {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_URL)))
            }, Modifier.fillMaxWidth(), Ic.Shield)
            if (app.ads.privacyOptionsRequired(activity))
                OutlineButton(stringResource(R.string.privacy_options), { app.ads.showPrivacyOptions(activity) }, Modifier.fillMaxWidth(), Ic.Info)
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun GuideScreen(onBack: () -> Unit, focusGame: String?) {
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.how_to_use), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (focusGame == null) {
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.guide_app_title), style = MaterialTheme.typography.titleLarge, color = pm.ink)
                        Text(stringResource(R.string.guide_app), style = MaterialTheme.typography.bodyMedium, color = pm.inkSoft)
                    }
                }
            }
            val list = Catalog.games.let { l -> if (focusGame == null) l else l.sortedByDescending { it.id == focusGame } }
            list.forEach { g ->
                SurfaceCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SideBadge(g.id, 0, Modifier.size(30.dp)); Spacer(Modifier.width(10.dp))
                            Text(stringResource(g.name), style = MaterialTheme.typography.titleLarge, color = pm.ink)
                        }
                        Text(stringResource(g.guide), style = MaterialTheme.typography.bodyMedium, color = pm.inkSoft)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
