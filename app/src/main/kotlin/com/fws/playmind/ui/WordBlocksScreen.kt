package com.fws.playmind.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
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

/** Màu các thanh đáp án (xanh lá, hồng, xanh ngọc, tím, xanh dương, cam, đào). */
private val WbBars = listOf(
    Color(0xFF7BD86A), Color(0xFFF08AD8), Color(0xFF6FD3E0), Color(0xFFC79BF0), Color(0xFF7FB2F0), Color(0xFFFF9A52), Color(0xFFF7B98A),
)
private val WbInk = Color(0xFF4A1A0C)
private val WbFont = FontFamily.Serif
/** Mặt bàn gỗ óc chó (khay khối gỗ, thanh ghép chữ). */
private val TableHi = Color(0xFF8A5A33)
private val TableLo = Color(0xFF5E3A1E)
private val Paper = Color(0xFFFBF5E8)

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

/** Mặt bàn gỗ: nền nâu đậm có vân ngang mờ. */
private fun Modifier.woodTable(shape: RoundedCornerShape): Modifier = this
    .clip(shape)
    .background(Brush.verticalGradient(listOf(TableHi, TableLo)))
    .drawBehind {
        var y = size.height * .08f; var k = 0
        while (y < size.height) {
            drawLine(Color.Black.copy(alpha = if (k % 2 == 0) .07f else .04f), Offset(0f, y), Offset(size.width, y + size.height * .015f), 2f)
            y += size.height / 9f; k++
        }
    }
    .border(1.5.dp, Brush.verticalGradient(listOf(Color(0xFFB9875A), Color(0xFF3E2512))), shape)

/** Một khối gỗ chữ cái; [socket] = khối đã dùng, vẽ thành hốc trống trên mặt bàn. */
@Composable
private fun WoodBlock(ch: Char, size: Dp, socket: Boolean = false, onClick: (() -> Unit)? = null) {
    Box(
        Modifier.size(size)
            .drawBehind {
                if (socket) drawRoundRect(Color.Black.copy(alpha = .22f), Offset(this.size.width * .03f, this.size.width * .03f), Size(this.size.width * .9f, this.size.width * .9f), CornerRadius(this.size.width * .12f))
                else drawWoodCell(Offset.Zero, this.size.width * .94f)
            }
            .then(if (onClick != null && !socket) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (!socket) Text(
            ch.toString(), Modifier.padding(end = size * .06f, bottom = size * .04f),
            style = TextStyle(fontFamily = WbFont, fontWeight = FontWeight.Bold, fontSize = (size.value * .56f).sp, color = WbInk),
        )
    }
}

/**
 * Màn hình Khối gỗ ghép chữ: bảng chủ đề (tên chủ đề, đồng hồ vòng, 7 chấm tiến độ), bảng 7 đáp án, thanh ghép chữ bằng gỗ,
 * hàng nút và khay khối gỗ. Chạm khối để thêm chữ (chạm lại để gỡ), "Ghép" để nộp từ. Đề chỉ có một lời giải nên chỉ
 * 7 từ đáp án được nhận. Gói tháng/năm: ô đáp án hiện số chữ cái; gói năm: thêm 2 gợi ý mỗi ván, mỗi gợi ý tự xếp một từ.
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
    var showHelp by remember { mutableStateOf(!app.prefs.wordsIntroSeen) }
    LaunchedEffect(feedbackId) { if (feedbackId > 0) { delay(1600); feedback = null } }
    val showLen = vm.wordsShowLengths
    val hintMax = vm.wordsHintMax
    val foundSet = g.found.map { it.first }.toSet()

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(info.name), { if (over) onExit() else confirmLeave = true }) {
            HelpPill(info.topic.color) { showHelp = true }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ThemeHeader(info, stringResource(themeName(g.theme.id)), vm.wbSeconds, g.found.size)
            // bảng đáp án: tờ giấy kem, 7 ô theo thứ tự đáp án (từ ghép đúng vào đúng ô của nó)
            Column(
                Modifier.fillMaxWidth().lift(RoundedCornerShape(18.dp), 8.dp).clip(RoundedCornerShape(18.dp)).background(Paper)
                    .border(1.dp, Color(0xFFE6D9BE), RoundedCornerShape(18.dp)).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                for (r in 0 until 4) Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    for (c in 0 until 2) {
                        val k = r * 2 + c
                        if (k < WordBlocks.WORDS) {
                            val a = g.answers[k]
                            AnswerSlot(k, if (a in foundSet) a else null, if (showLen) a.length else 0, Modifier.weight(1f), revealed = over && a !in foundSet, answer = a)
                        } else Spacer(Modifier.weight(1f))
                    }
                }
            }
            // thanh ghép chữ (gỗ) + kết quả lần Ghép gần nhất
            Box(Modifier.fillMaxWidth()) {
                BoxWithConstraints(Modifier.fillMaxWidth().height(66.dp).woodTable(RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    if (g.current.isEmpty()) {
                        Text(stringResource(R.string.wb_tap_hint), Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFF3DFC0), maxLines = 2)
                    } else {
                        val t = minOf(48.dp, (maxWidth - 20.dp) / WordBlocks.MAX_LEN)
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            for (i in g.current) WoodBlock(g.blocks[i], t, onClick = if (over) null else ({ vm.wordsEdit { it.tap(i) } }))
                        }
                    }
                }
                val fb = feedbackText(feedback)
                if (fb != null) {
                    val ok = feedback == WordBlocksGame.Submit.OK
                    Text(
                        stringResource(fb),
                        Modifier.align(Alignment.TopCenter).padding(top = 0.dp).clip(RoundedCornerShape(50))
                            .background(if (ok) pm.good else pm.danger).padding(horizontal = 14.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            // hàng nút
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RoundTool(Ic.Undo, stringResource(R.string.wb_back), !over && g.current.isNotEmpty()) { vm.wordsEdit { it.back() } }
                RoundTool(Ic.Eraser, stringResource(R.string.wb_clear), !over && g.current.isNotEmpty()) { vm.wordsEdit { it.clear() } }
                if (hintMax > 0) {
                    val left = hintMax - g.hintsUsed
                    Box {
                        RoundTool(Ic.Bulb, stringResource(R.string.wb_hint), !over && left > 0) { if (vm.wordsHint() != null) { feedback = WordBlocksGame.Submit.OK; feedbackId++ } }
                        Text(
                            left.toString(),
                            Modifier.align(Alignment.TopEnd).size(18.dp).clip(CircleShape).background(if (left > 0) pm.goldBright else pm.outline).padding(top = 1.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
                GoldButton(stringResource(R.string.wb_submit), { feedback = vm.wordsSubmit(); feedbackId++ }, Modifier.weight(1f), icon = Ic.Check, enabled = !over && g.current.isNotEmpty())
            }
            // khay khối gỗ
            BoxWithConstraints(Modifier.fillMaxWidth().woodTable(RoundedCornerShape(20.dp)).padding(10.dp)) {
                val cols = if (maxWidth > 560.dp) 12 else 8
                val gap = 4.dp
                val t = (maxWidth - gap * (cols - 1)) / cols
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    for (row in g.blocks.indices.chunked(cols)) Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        for (i in row) WoodBlock(g.blocks[i], t, socket = g.used[i] && i !in g.current, onClick = if (over) null else ({ vm.wordsEdit { it.tap(i) } }))
                    }
                }
            }
            if (!showLen) Text(stringResource(R.string.wb_plan_perks), style = MaterialTheme.typography.labelSmall, color = pm.inkSoft)
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
    if (showHelp && !over) PmDialog(
        title = stringResource(R.string.wb_howto_title), text = stringResource(R.string.wb_howto, stringResource(themeName(g.theme.id))),
        confirm = stringResource(R.string.ok), onConfirm = { showHelp = false; app.prefs.wordsIntroSeen = true },
        tone = DialogTone.INFO, icon = Ic.Help,
    )
    if (confirmLeave) SimpleDialog(stringResource(R.string.leave_game), stringResource(R.string.leave_confirm), stringResource(R.string.leave), { confirmLeave = false; onExit() }, stringResource(R.string.cancel), { confirmLeave = false }, tone = DialogTone.WARN)
}

private fun feedbackText(r: WordBlocksGame.Submit?): Int? = when (r) {
    WordBlocksGame.Submit.OK -> R.string.wb_ok
    WordBlocksGame.Submit.NOT_IN_THEME -> R.string.wb_not_theme
    WordBlocksGame.Submit.NOT_ANSWER -> R.string.wb_not_answer
    WordBlocksGame.Submit.DUPLICATE -> R.string.wb_dup
    WordBlocksGame.Submit.TOO_SHORT -> R.string.wb_short
    else -> null
}

/** Nút "Cách chơi" gọn ở góc trên: viên thuốc màu chủ đề có biểu tượng ?. */
@Composable
private fun HelpPill(color: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.clip(shape).background(color.copy(alpha = .16f)).border(1.dp, color.copy(alpha = .5f), shape)
            .clickable(onClick = onClick).padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(Ic.Help, null, Modifier.size(18.dp), tint = color.onLight(.3f))
        Text(stringResource(R.string.wb_howto_title), style = MaterialTheme.typography.labelLarge, color = color.onLight(.45f))
    }
}

/** Bảng chủ đề: nhãn "Chủ đề", tên chủ đề lớn, 7 chấm tiến độ và đồng hồ vòng đếm ngược. */
@Composable
private fun ThemeHeader(info: GameInfo, theme: String, seconds: Int, found: Int) {
    val shape = RoundedCornerShape(22.dp)
    val c = info.topic.color
    val danger = seconds <= 60
    Row(
        Modifier.fillMaxWidth().lift(shape, 10.dp, c).clip(shape)
            .background(Brush.linearGradient(listOf(c, c.onLight(.35f))))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = .22f), Color.Transparent)))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.wb_theme_label).uppercase(), style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.5.sp), color = Color.White.copy(alpha = .85f))
            Text(theme, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for (k in 0 until WordBlocks.WORDS) Box(
                    Modifier.size(10.dp).clip(CircleShape).background(if (k < found) Color.White else Color.White.copy(alpha = .28f)),
                )
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.wb_found, found, WordBlocks.WORDS), style = MaterialTheme.typography.labelMedium, color = Color.White)
            }
        }
        // đồng hồ vòng
        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            val frac = seconds / WordBlocks.SECONDS.toFloat()
            Canvas(Modifier.fillMaxSize()) {
                val sw = size.width * .1f
                drawArc(Color.White.copy(alpha = .25f), 0f, 360f, false, topLeft = Offset(sw / 2, sw / 2), size = Size(size.width - sw, size.height - sw), style = Stroke(sw))
                drawArc(if (danger) Color(0xFFFFD2CC) else Color.White, -90f, 360f * frac, false, topLeft = Offset(sw / 2, sw / 2), size = Size(size.width - sw, size.height - sw), style = Stroke(sw, cap = StrokeCap.Round))
            }
            Text(fmt(seconds), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = if (danger) Color(0xFFFFE3DE) else Color.White)
        }
    }
}

/**
 * Ô đáp án thứ [k]: số thứ tự trong vòng màu; đã ghép thì hiện từ trên nền màu; chưa thì "?" — hoặc [letters] ô chữ trống
 * khi có gói tháng/năm. Hết giờ thì hiện đáp án còn thiếu bằng chữ mờ ([revealed]).
 */
@Composable
private fun AnswerSlot(k: Int, word: String?, letters: Int, modifier: Modifier, revealed: Boolean, answer: String) {
    val shape = RoundedCornerShape(12.dp)
    val color = WbBars[k % WbBars.size]
    Row(
        modifier.height(40.dp).clip(shape)
            .background(if (word != null) color else Color.White)
            .border(1.dp, if (word != null) color.onLight(.15f) else Color(0xFFE8DCC3), shape)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(if (word != null) Color.White.copy(alpha = .6f) else color.copy(alpha = .35f)),
            contentAlignment = Alignment.Center,
        ) { Text((k + 1).toString(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = WbInk) }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when {
                word != null -> Text(word, style = TextStyle(fontFamily = WbFont, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 1.5.sp, color = WbInk), maxLines = 1)
                revealed -> Text(answer, style = TextStyle(fontFamily = WbFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.5.sp, color = pm.danger.copy(alpha = .7f)), maxLines = 1)
                letters > 0 -> Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(letters) { Box(Modifier.width(9.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFB59A70))) }
                }
                else -> Text("?", style = TextStyle(fontFamily = WbFont, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFCDBB98)))
            }
        }
    }
}

/** Nút tròn có biểu tượng (xóa chữ, xóa hết, gợi ý). */
@Composable
private fun RoundTool(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val src = rememberPress()
    Box(
        Modifier.bounce(src, .9f).size(50.dp).lift(CircleShape, 6.dp).clip(CircleShape).background(cardBrush()).border(1.dp, hairline(), CircleShape)
            .tap(src, enabled = enabled, label = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, Modifier.size(22.dp), tint = if (enabled) pm.ink else pm.outline) }
}
