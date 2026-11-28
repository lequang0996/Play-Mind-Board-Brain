package com.fws.playmind.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fws.playmind.PlayMindApp
import com.fws.playmind.R
import com.fws.playmind.catalog.GameInfo
import com.fws.playmind.core.WordBlocks
import com.fws.playmind.core.WordBlocksGame
import com.fws.playmind.match.Phase
import com.fws.playmind.match.SessionViewModel
import kotlinx.coroutines.delay

/** Màu nền các thanh đáp án (như bảng đáp án mẫu: xanh lá, hồng, xanh ngọc, tím, xanh dương, cam, đào). */
private val WbBars = listOf(
    Color(0xFF7BD86A), Color(0xFFF08AD8), Color(0xFF6FD3E0), Color(0xFFC79BF0), Color(0xFF7FB2F0), Color(0xFFFF8A3D), Color(0xFFF7B98A),
)
private val WbInk = Color(0xFF4A1A0C)
private val WbFont = FontFamily.Serif

private fun fmt(sec: Int) = "%d:%02d".format(sec / 60, sec % 60)

/** Tên chủ đề theo ngôn ngữ (từ ghép luôn là tiếng Anh). */
private fun themeName(id: String): Int = when (id) {
    "countries" -> R.string.wb_theme_countries
    "animals" -> R.string.wb_theme_animals
    "fruits" -> R.string.wb_theme_fruits
    "colors" -> R.string.wb_theme_colors
    "sports" -> R.string.wb_theme_sports
    "jobs" -> R.string.wb_theme_jobs
    "body" -> R.string.wb_theme_body
    "vegetables" -> R.string.wb_theme_vegetables
    "instruments" -> R.string.wb_theme_instruments
    "kitchen" -> R.string.wb_theme_kitchen
    "weather" -> R.string.wb_theme_weather
    "clothes" -> R.string.wb_theme_clothes
    "transport" -> R.string.wb_theme_transport
    "space" -> R.string.wb_theme_space
    else -> R.string.wb_theme_cities
}

/** Một khối gỗ chữ cái (vẽ bằng [drawWoodCell] dùng chung với Sudoku / Ma phương). */
@Composable
private fun WoodBlock(ch: Char, size: Dp, modifier: Modifier = Modifier, dim: Boolean = false, onClick: (() -> Unit)? = null) {
    Box(
        modifier.size(size).alpha(if (dim) .28f else 1f)
            .drawBehind { drawWoodCell(Offset.Zero, this.size.width * .94f) }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            ch.toString(), Modifier.padding(end = size * .06f, bottom = size * .04f),
            style = TextStyle(fontFamily = WbFont, fontWeight = FontWeight.Bold, fontSize = (size.value * .56f).sp, color = WbInk),
        )
    }
}

/**
 * Màn hình Khối gỗ ghép chữ: chủ đề + đồng hồ 10 phút ở trên, 7 thanh đáp án, từ đang ghép, rồi các khối gỗ.
 * Chạm khối để thêm chữ (chạm lại để gỡ), "Ghép" để nộp từ, chạm một thanh đáp án để trả từ đó về các khối.
 */
@Composable
fun WordBlocksScreen(app: PlayMindApp, vm: SessionViewModel, info: GameInfo, onExit: () -> Unit) {
    DisposableEffect(Unit) { app.audio.calm = true; onDispose { app.audio.calm = false } }
    val over = vm.phase == Phase.OVER
    var confirmLeave by remember { mutableStateOf(false) }
    BackHandler { if (over) onExit() else confirmLeave = true }
    val g = vm.words ?: return
    @Suppress("UNUSED_VARIABLE") val dep = vm.wbTick
    var feedback by remember { mutableStateOf<WordBlocksGame.Submit?>(null) }
    var feedbackId by remember { mutableIntStateOf(0) }
    LaunchedEffect(feedbackId) { if (feedbackId > 0) { delay(1600); feedback = null } }

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(info.name), { if (over) onExit() else confirmLeave = true })
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            WbChip { Icon(Ic.Book, null, Modifier.size(16.dp), tint = pm.gold); Text(stringResource(R.string.wb_theme, stringResource(themeName(g.theme.id))), style = MaterialTheme.typography.titleSmall, color = pm.ink, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            WbChip {
                Icon(Ic.Hourglass, null, Modifier.size(16.dp), tint = if (vm.wbSeconds <= 60) pm.danger else pm.gold)
                Text(fmt(vm.wbSeconds), style = MaterialTheme.typography.titleSmall, color = if (vm.wbSeconds <= 60) pm.danger else pm.ink)
            }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.wb_found, g.found.size, WordBlocks.WORDS), style = MaterialTheme.typography.labelLarge, color = pm.inkSoft)
            // kết quả lần bấm Ghép gần nhất (hiện cả khi từ bị từ chối và các chữ vẫn còn trong ô ghép)
            val fb = feedbackText(feedback)
            if (fb != null) Text(
                stringResource(fb),
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background((if (feedback == WordBlocksGame.Submit.OK) pm.good else pm.danger).copy(alpha = .14f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall, color = if (feedback == WordBlocksGame.Submit.OK) pm.good else pm.danger,
            )
            // 7 thanh đáp án, 2 cột
            for (r in 0 until 4) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (c in 0 until 2) {
                    val k = r * 2 + c
                    if (k < WordBlocks.WORDS) AnswerBar(k, g.found.getOrNull(k)?.first, Modifier.weight(1f), enabled = !over) { vm.wordsEdit { it.unfind(k) } }
                    else Spacer(Modifier.weight(1f))
                }
            }
            // từ đang ghép
            BoxWithConstraints(
                Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF6B6F78).copy(alpha = .18f))
                    .border(1.dp, pm.outline, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (g.current.isEmpty()) {
                    Text(stringResource(R.string.wb_tap_hint), style = MaterialTheme.typography.bodyMedium, color = pm.inkSoft)
                } else {
                    val t = minOf(48.dp, (maxWidth - 16.dp) / WordBlocks.MAX_LEN)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (i in g.current) WoodBlock(g.blocks[i], t, onClick = if (over) null else ({ vm.wordsEdit { it.tap(i) } }))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolButton(Ic.Undo, stringResource(R.string.wb_back), { vm.wordsEdit { it.back() } }, Modifier.weight(1f), enabled = !over && g.current.isNotEmpty())
                ToolButton(Ic.Eraser, stringResource(R.string.wb_clear), { vm.wordsEdit { it.clear() } }, Modifier.weight(1f), enabled = !over && g.current.isNotEmpty())
                GoldButton(stringResource(R.string.wb_submit), {
                    feedback = vm.wordsSubmit(); feedbackId++
                }, Modifier.weight(1.2f), icon = Ic.Check, enabled = !over && g.current.isNotEmpty())
            }
            // các khối gỗ
            BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF8A8F98).copy(alpha = .22f)).padding(8.dp)) {
                val cols = if (maxWidth > 560.dp) 12 else 8
                val gap = 4.dp
                val t = (maxWidth - gap * (cols - 1)) / cols
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    for (row in g.blocks.indices.chunked(cols)) Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        for (i in row) {
                            val busy = g.used[i]
                            WoodBlock(g.blocks[i], t, dim = busy, onClick = if (over) null else ({ vm.wordsEdit { it.tap(i) } }))
                        }
                    }
                }
            }
            Text(stringResource(R.string.wb_unfind_hint), style = MaterialTheme.typography.labelSmall, color = pm.inkSoft)
            Spacer(Modifier.height(8.dp))
        }
    }

    if (over) {
        val won = g.solved
        val reward = vm.reward
        PmDialog(
            title = stringResource(if (won) R.string.wb_won else R.string.wb_lost),
            text = if (won) stringResource(R.string.wb_won_text, fmt(WordBlocks.SECONDS - vm.wbSeconds)) else stringResource(R.string.wb_lost_text, g.answers.joinToString(", ")),
            confirm = stringResource(R.string.play_again), onConfirm = { vm.rematch() },
            dismiss = stringResource(R.string.back_to_lobby), onDismiss = onExit,
            tone = if (won) DialogTone.GEM else DialogTone.WARN, icon = if (won) Ic.Trophy else Ic.Hourglass,
            body = {
                if (reward != null && reward.diamonds > 0) {
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(pm.surfaceHigh).border(1.dp, pm.goldBright.copy(alpha = .7f), RoundedCornerShape(50)).padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GemMark(20.dp)
                        Text(pluralStringResource(R.plurals.reward_diamonds, reward.diamonds, reward.diamonds), style = MaterialTheme.typography.titleMedium, color = pm.ink)
                    }
                }
            },
        )
    }
    if (confirmLeave) SimpleDialog(stringResource(R.string.leave_game), stringResource(R.string.leave_confirm), stringResource(R.string.leave), { confirmLeave = false; onExit() }, stringResource(R.string.cancel), { confirmLeave = false }, tone = DialogTone.WARN)
}

private fun feedbackText(r: WordBlocksGame.Submit?): Int? = when (r) {
    WordBlocksGame.Submit.OK -> R.string.wb_ok
    WordBlocksGame.Submit.NOT_IN_THEME -> R.string.wb_not_theme
    WordBlocksGame.Submit.DUPLICATE -> R.string.wb_dup
    WordBlocksGame.Submit.TOO_SHORT -> R.string.wb_short
    else -> null
}

/** Thanh đáp án thứ [k]: đã ghép thì hiện từ trên nền màu, chưa thì để trống; chạm thanh đã ghép để trả từ về khối. */
@Composable
private fun AnswerBar(k: Int, word: String?, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val color = WbBars[k % WbBars.size]
    Box(
        modifier.height(40.dp).clip(shape)
            .background(if (word != null) color else pm.surface.copy(alpha = .6f))
            .border(1.dp, if (word != null) color.copy(alpha = .9f) else pm.outline, shape)
            .then(if (word != null && enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            word ?: "· · ·",
            style = TextStyle(fontFamily = WbFont, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 2.sp, color = if (word != null) WbInk else pm.outline),
            maxLines = 1,
        )
    }
}

@Composable
private fun WbChip(content: @Composable () -> Unit) {
    val chip = RoundedCornerShape(50)
    Row(
        Modifier.lift(chip, 6.dp).clip(chip).background(cardBrush()).border(1.dp, hairline(), chip).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) { content() }
}
