package com.fws.playmind.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fws.playmind.R
import com.fws.playmind.catalog.GameInfo
import com.fws.playmind.core.HiLoGame
import com.fws.playmind.core.HiLoPhase
import com.fws.playmind.core.HiLoShowdown
import com.fws.playmind.match.Mode
import com.fws.playmind.match.SessionViewModel
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

// ─────────────────────────── màu thẻ ───────────────────────────

/** Màu chữ số theo chất: vàng > bạc > đồng > đen. */
private val SuitColors = listOf(Color(0xFFC8971E), Color(0xFF8E99A6), Color(0xFFB4683A), Color(0xFF2C2A33))
private val OpRed = Color(0xFFC0303A)
private val FeltTop = Color(0xFF2E2550)
private val FeltBottom = Color(0xFF1C1736)
private val CardBack = Color(0xFF5B3FA8)

private fun opSymbol(op: Int) = when (op) { HiLoGame.OP_ADD -> "+"; HiLoGame.OP_SUB -> "−"; HiLoGame.OP_DIV -> "÷"; else -> "×" }

/** Số gọn: tối đa 3 chữ số thập phân, bỏ số 0 thừa. */
internal fun fmtVal(x: Double): String {
    if (x.isNaN()) return "—"
    val s = "%.3f".format(java.util.Locale.US, x).trimEnd('0').trimEnd('.')
    return if (s == "-0") "0" else s
}

/** Chuỗi biểu thức của mã [code] với thẻ số [cards] và phép [ops]. */
internal fun exprText(code: Int, cards: IntArray, ops: IntArray): String {
    if (code !in 0 until HiLoGame.CODES || cards.size != 4 || ops.size != 3) return "—"
    val mask = code % 16; val q = HiLoGame.PERM3[(code / 16) % 6]; val p = HiLoGame.PERM4[code / 96]
    val sb = StringBuilder()
    for (i in 0 until 4) {
        if (i > 0) sb.append(' ').append(opSymbol(ops[q[i - 1]])).append(' ')
        if (mask and (1 shl i) != 0) sb.append('√')
        sb.append(HiLoGame.value(cards[p[i]]))
    }
    return sb.toString()
}

// ─────────────────────────── thẻ bài ───────────────────────────

/** Một thẻ bài trắng: số (màu theo chất), phép toán (đỏ) hoặc mặt sau (tím). */
@Composable
private fun HlCard(
    card: Int, w: Dp, modifier: Modifier = Modifier, faceDown: Boolean = false, op: Int = -1, sqrt: Boolean = false,
    selected: Boolean = false, dim: Boolean = false, onClick: (() -> Unit)? = null,
) {
    val h = w * 1.4f
    val shape = RoundedCornerShape(w * .14f)
    var m = modifier.size(w, h).alpha(if (dim) .32f else 1f).lift(shape, 3.dp).clip(shape)
    m = if (faceDown) m.background(Brush.linearGradient(listOf(CardBack, Color(0xFF3A2878))))
    else m.background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF1EEF6))))
    m = m.border(if (selected) 2.dp else 1.dp, if (selected) pm.goldBright else Color(0x22000000), shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Box(m, contentAlignment = Alignment.Center) {
        val fs = (w.value * .5f).sp
        when {
            faceDown -> {
                Box(Modifier.size(w * .62f, h * .7f).border(1.dp, Color.White.copy(alpha = .35f), RoundedCornerShape(w * .1f)), contentAlignment = Alignment.Center) {
                    Text("?", color = Color.White.copy(alpha = .8f), fontSize = fs, fontWeight = FontWeight.Bold)
                }
            }
            sqrt -> Text("√", color = OpRed, fontSize = fs * 1.1f, fontWeight = FontWeight.Bold)
            op >= 0 -> Text(opSymbol(op), color = OpRed, fontSize = fs * 1.15f, fontWeight = FontWeight.Bold)
            else -> Text(
                HiLoGame.value(card).toString(), color = SuitColors[HiLoGame.suit(card)],
                fontSize = if (HiLoGame.value(card) == 10) fs * .85f else fs, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif,
            )
        }
    }
}

/** Marker chọn cửa: High = ô thoi đen số 20, Low = tròn trắng số 1, Swing = cả hai. */
@Composable
private fun SideMarker(side: Int, size: Dp, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        if (side != HiLoGame.SIDE_LOW) Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Box(Modifier.size(size * .72f).rotate(45f).clip(RoundedCornerShape(size * .12f)).background(Color(0xFF15131C)).border(1.dp, Color.White.copy(alpha = .5f), RoundedCornerShape(size * .12f)))
            Text("20", color = Color.White, fontSize = (size.value * .34f).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
        }
        if (side != HiLoGame.SIDE_HIGH) Box(Modifier.size(size * .86f).clip(CircleShape).background(Color.White).border(1.dp, Color(0x33000000), CircleShape), contentAlignment = Alignment.Center) {
            Text("1", color = Color(0xFF15131C), fontSize = (size.value * .4f).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
        }
    }
}

/** Đồng chip tím. */
@Composable
private fun ChipDot(size: Dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFFB59BFF), Color(0xFF6A3FD0)))).border(1.5.dp, Color.White.copy(alpha = .7f), CircleShape))
}

// ─────────────────────────── bàn chơi (kiểu bàn poker) ───────────────────────────

private val FeltA = Color(0xFF1E8C86)
private val FeltB = Color(0xFF0F5E5A)
private val Rail = Color(0xFF6B4A2E)

/**
 * Bàn bài hình bầu dục như bàn poker: đối thủ ngồi quanh mép trên và hai bên, mình ngồi dưới cùng.
 * Mỗi ghế thấy được thẻ phép toán, thẻ úp (mặt sau) và mọi thẻ ngửa; chip đã cược ở vòng này đặt trước ghế; pot ở giữa.
 * Mọi composable con đều nhận và đọc [SessionViewModel.tick] để vẽ lại sau mỗi nước (đối tượng game không đổi tham chiếu,
 * nếu không Compose bỏ qua vẽ lại và bàn đứng yên).
 */
@Composable
fun HiLoBoardView(vm: SessionViewModel, info: GameInfo) {
    val tick = vm.tick
    val g = vm.game as? HiLoGame ?: return
    val me = if (vm.mode == Mode.LOCAL) g.turn else vm.mySide
    val act = vm.canAct
    val over = g.phase == HiLoPhase.OVER

    // chia bài: lượt của mình thì tự chia sau một nhịp ngắn
    LaunchedEffect(g.plies, act, g.phase) {
        if (act && g.phase == HiLoPhase.DEAL) { delay(900); if (vm.canAct && g.phase == HiLoPhase.DEAL) vm.playMove(HiLoGame.DEAL + Random.nextInt(1_000_000)) }
    }
    var seenHand by remember { mutableIntStateOf(0) }
    val sd = g.lastShowdown

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Table(vm, info, g, me, tick, Modifier.weight(1f).fillMaxWidth())
        MyHand(g, me, tick)
        if (!over) ActionPanel(vm, info, g, me, act, tick)
    }
    if (sd != null && sd.hand != seenHand && sd.hand > 0) ShowdownDialog(vm, info, g, sd) { seenHand = sd.hand }
}

@Composable
private fun Table(vm: SessionViewModel, info: GameInfo, g: HiLoGame, me: Int, tick: Int, modifier: Modifier) {
    @Suppress("UNUSED_EXPRESSION") tick
    BoxWithConstraints(modifier) {
        val w = maxWidth; val h = maxHeight
        // mặt bàn: viền gỗ, nỉ xanh
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension * .5f
            drawRoundRect(Rail, Offset.Zero, size, CornerRadius(r, r))
            val inset = size.minDimension * .035f
            drawRoundRect(
                Brush.radialGradient(listOf(FeltA, FeltB), center = Offset(size.width / 2, size.height / 2), radius = size.maxDimension * .6f),
                Offset(inset, inset), Size(size.width - 2 * inset, size.height - 2 * inset), CornerRadius(r - inset, r - inset),
            )
            drawRoundRect(
                Color.White.copy(alpha = .12f), Offset(inset * 2.2f, inset * 2.2f), Size(size.width - 4.4f * inset, size.height - 4.4f * inset),
                CornerRadius(r - inset * 2.2f, r - inset * 2.2f), style = Stroke(size.minDimension * .006f),
            )
        }
        // giữa bàn: pot, ván bài, vòng cược, đồng hồ
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = .35f)).padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ChipDot(18.dp)
                Text(stringResource(R.string.hl_pot, g.pot), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(stringResource(R.string.hl_hand, g.hand.coerceAtLeast(1), HiLoGame.ante(g.hand.coerceAtLeast(1))), color = Color.White.copy(alpha = .85f), style = MaterialTheme.typography.labelSmall)
            if (g.phase == HiLoPhase.BET) Text(stringResource(R.string.hl_bet_round, g.betRound), color = Color(0xFFFFE08A), style = MaterialTheme.typography.labelSmall)
            if (g.phase != HiLoPhase.OVER && vm.moveSeconds > 0) TurnClock(vm.turnSecondsLeft)
        }
        // ghế: mình ở dưới cùng, đối thủ trải từ trái lên trên rồi xuống phải
        val n = g.players
        val seatW = 104.dp; val seatH = 88.dp
        val rx = (w - seatW) / 2; val ry = (h - seatH) / 2
        for (k in 0 until n) {
            val p = (me + k) % n
            val deg = if (k == 0) 90.0 else if (n == 2) 270.0 else 150.0 + (k - 1) * (240.0 / (n - 2))
            val rad = Math.toRadians(deg)
            val cs = kotlin.math.cos(rad).toFloat(); val sn = kotlin.math.sin(rad).toFloat()
            val cx = w / 2 + rx * cs; val cy = h / 2 + ry * sn
            Seat(vm, info, g, p, p == me, tick, Modifier.offset(cx - seatW / 2, cy - seatH / 2).width(seatW))
            // chip đã cược ở vòng này, đặt trước ghế (về phía giữa bàn)
            val bet = if (g.phase == HiLoPhase.BET) g.roundBet[p] else 0
            if (bet > 0) {
                val bx = w / 2 + rx * .5f * cs; val by = h / 2 + ry * .45f * sn
                Row(
                    Modifier.offset(bx - 20.dp, by - 10.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = .3f)).padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    ChipDot(12.dp)
                    Text("$bet", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun statusText(g: HiLoGame, p: Int): String? = when {
    g.out[p] -> stringResource(R.string.hl_out)
    g.inHand[p] && g.folded[p] -> stringResource(R.string.hl_folded)
    g.inHand[p] && g.chips[p] == 0 && g.phase != HiLoPhase.DEAL -> stringResource(R.string.hl_allin)
    else -> null
}

/** Một ghế: tên, chip, trạng thái, thẻ phép toán, thẻ úp (của mình thì thấy mặt số) và thẻ ngửa. */
@Composable
private fun Seat(vm: SessionViewModel, info: GameInfo, g: HiLoGame, p: Int, mine: Boolean, tick: Int, modifier: Modifier) {
    @Suppress("UNUSED_EXPRESSION") tick
    val turn = g.turn == p && g.phase != HiLoPhase.OVER
    val shape = RoundedCornerShape(12.dp)
    val faded = g.out[p] || (g.inHand[p] && g.folded[p])
    Column(
        modifier.alpha(if (faded) .45f else 1f).clip(shape).background(Color.Black.copy(alpha = if (turn) .5f else .3f))
            .border(if (turn) 2.dp else 1.dp, if (turn) Color(0xFFFFD54F) else Color.White.copy(alpha = .18f), shape).padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.size(16.dp).clip(CircleShape).background(playerColor(p)), contentAlignment = Alignment.Center) {
                if (g.dealer == p) Text("D", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Text(seatName(vm, info, p), Modifier.weight(1f), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            ChipDot(10.dp)
            Text("${g.chips[p]}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            val st = statusText(g, p)
            if (st != null) Text(st, color = Color(0xFFFFB4A8), fontSize = 9.sp, maxLines = 1)
            else if (g.side[p] >= 0 && g.phase == HiLoPhase.BUILD) Text("✓", color = Color(0xFF8CF0B0), fontSize = 10.sp)
        }
        if (g.inHand[p] && g.nums[p].isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (o in g.ops[p]) HlCard(0, 13.dp, op = o)
                for (c in g.openCards[p]) if (c >= HiLoGame.SQRT_CARD) HlCard(c, 13.dp, sqrt = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (mine) HlCard(g.nums[p][0], 19.dp) else HlCard(0, 19.dp, faceDown = true)
                for (c in g.openCards[p]) if (HiLoGame.isNumber(c)) HlCard(c, 19.dp)
            }
        }
    }
}

/** Bài của mình, thẻ to: phép toán, thẻ úp (viền tím) và thẻ ngửa. Ẩn khi đang ghép biểu thức (bảng ghép đã có đủ thẻ). */
@Composable
private fun MyHand(g: HiLoGame, me: Int, tick: Int) {
    @Suppress("UNUSED_EXPRESSION") tick
    if (!g.inHand[me] || g.nums[me].isEmpty() || g.phase == HiLoPhase.BUILD) return
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(Brush.verticalGradient(listOf(FeltTop, FeltBottom))).border(1.dp, Color.White.copy(alpha = .16f), shape)
            .padding(8.dp).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom,
    ) {
        for (o in g.ops[me]) HlCard(0, 30.dp, op = o)
        Spacer(Modifier.width(4.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HlCard(g.nums[me][0], 30.dp, Modifier.border(1.5.dp, Color(0xFFCDB8FF), RoundedCornerShape(5.dp)))
            Text(stringResource(R.string.hl_hidden), color = Color(0xFFCDB8FF), fontSize = 9.sp)
        }
        for (c in g.openCards[me]) when {
            HiLoGame.isNumber(c) -> HlCard(c, 30.dp)
            c >= HiLoGame.SQRT_CARD -> HlCard(c, 30.dp, sqrt = true)
            else -> {}
        }
    }
}

// ─────────────────────────── thao tác theo pha ───────────────────────────

@Composable
private fun ActionPanel(vm: SessionViewModel, info: GameInfo, g: HiLoGame, me: Int, act: Boolean, tick: Int) {
    @Suppress("UNUSED_EXPRESSION") tick
    if (!act) {
        val who = seatName(vm, info, g.turn)
        Text(
            if (vm.botThinking) stringResource(R.string.thinking) else stringResource(R.string.hl_waiting, who),
            Modifier.fillMaxWidth().padding(vertical = 6.dp), color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center,
        )
        return
    }
    when (g.phase) {
        HiLoPhase.DEAL -> GoldButton(stringResource(R.string.hl_deal), { vm.playMove(HiLoGame.DEAL + Random.nextInt(1_000_000)) }, Modifier.fillMaxWidth())
        HiLoPhase.DISCARD -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Prompt(stringResource(R.string.hl_discard_title))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                for (o in g.ops[me].filter { it != HiLoGame.OP_MUL }.distinct()) HlCard(0, 40.dp, op = o, onClick = { vm.playMove(HiLoGame.DISCARD + o) })
                Spacer(Modifier.weight(1f))
            }
        }
        HiLoPhase.BET -> BetPanel(vm, g, me, tick)
        HiLoPhase.DECLARE -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Prompt(stringResource(R.string.hl_declare_title))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((s, label) in listOf(HiLoGame.SIDE_HIGH to R.string.hl_high, HiLoGame.SIDE_LOW to R.string.hl_low, HiLoGame.SIDE_SWING to R.string.hl_swing)) {
                    val sh = RoundedCornerShape(14.dp)
                    Column(
                        Modifier.weight(1f).clip(sh).background(Color.White.copy(alpha = .1f)).border(1.dp, Color.White.copy(alpha = .25f), sh)
                            .clickable { vm.playMove(HiLoGame.DECLARE + s) }.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        SideMarker(s, 34.dp)
                        Text(stringResource(label), color = Color.White, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        HiLoPhase.BUILD -> Builder(vm, g, me, tick)
        HiLoPhase.OVER -> {}
    }
}

@Composable
private fun Prompt(text: String) {
    Text(text, Modifier.fillMaxWidth(), color = Color(0xFFFFE08A), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
}

@Composable
private fun BetPanel(vm: SessionViewModel, g: HiLoGame, me: Int, tick: Int) {
    @Suppress("UNUSED_EXPRESSION") tick
    val call = g.toCall(me)
    val room = g.chips[me] - call
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlineButton(stringResource(R.string.hl_fold), { vm.playMove(HiLoGame.FOLD) }, Modifier.weight(1f))
            GoldButton(
                if (call == 0) stringResource(R.string.hl_check) else stringResource(R.string.hl_call, minOf(call, g.chips[me])),
                { vm.playMove(HiLoGame.CALL) }, Modifier.weight(1.3f),
            )
        }
        if (g.canRaise(me)) Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            val opts = listOf(1, 2, 5, 10).filter { it < room } + room
            for (r in opts.distinct()) {
                val sh = RoundedCornerShape(50)
                Text(
                    if (r == room) stringResource(R.string.hl_allin) else stringResource(R.string.hl_raise, r),
                    Modifier.weight(1f).clip(sh).background(Color(0xFF6A3FD0)).border(1.dp, Color.White.copy(alpha = .35f), sh)
                        .clickable { vm.playMove(HiLoGame.RAISE + r) }.padding(vertical = 8.dp),
                    color = Color.White, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1,
                )
            }
        }
    }
}

/** Ghép biểu thức: chạm thẻ theo thứ tự số - phép - số - phép - số - phép - số; chạm √ rồi chạm số để đặt căn. */
@Composable
private fun Builder(vm: SessionViewModel, g: HiLoGame, me: Int, tick: Int) {
    @Suppress("UNUSED_EXPRESSION") tick
    val cards = g.cardsOf(me); val ops = g.opsOf(me); val k = g.sqrtN[me]
    val swing = g.side[me] == HiLoGame.SIDE_SWING
    var tab by remember(g.hand) { mutableIntStateOf(if (g.side[me] == HiLoGame.SIDE_LOW) 1 else 0) }
    // mỗi biểu thức: thứ tự số, thứ tự phép, căn theo vị trí
    val numOrder = remember(g.hand) { List(2) { mutableStateListOf<Int>() } }
    val opOrder = remember(g.hand) { List(2) { mutableStateListOf<Int>() } }
    val sq = remember(g.hand) { List(2) { mutableStateListOf<Boolean>() } }
    var sqrtArmed by remember(g.hand) { mutableStateOf(false) }

    fun codeOf(i: Int): Int {
        if (numOrder[i].size != 4 || opOrder[i].size != 3) return -1
        val pm = HiLoGame.PERM4.indexOfFirst { it.contentEquals(numOrder[i].toIntArray()) }
        val op = HiLoGame.PERM3.indexOfFirst { it.contentEquals(opOrder[i].toIntArray()) }
        var mask = 0; for (j in 0 until 4) if (sq[i].getOrElse(j) { false }) mask = mask or (1 shl j)
        return HiLoGame.code(pm, op, mask)
    }
    fun valueOf(i: Int): Double { val c = codeOf(i); return if (c < 0) Double.NaN else HiLoGame.valueOf(c, cards, ops, k) }
    val cur = tab
    val usedSqrt = sq[cur].count { it }
    val expectNum = numOrder[cur].size == opOrder[cur].size

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (swing) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((i, label) in listOf(0 to R.string.hl_build_high, 1 to R.string.hl_build_low)) {
                val sh = RoundedCornerShape(50); val on = tab == i
                Row(
                    Modifier.weight(1f).clip(sh).background(if (on) Color.White.copy(alpha = .22f) else Color.Transparent).border(1.dp, Color.White.copy(alpha = .3f), sh)
                        .clickable { tab = i; sqrtArmed = false }.padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    SideMarker(if (i == 0) HiLoGame.SIDE_HIGH else HiLoGame.SIDE_LOW, 20.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(label), color = Color.White, style = MaterialTheme.typography.labelMedium)
                    if (codeOf(i) >= 0 && !valueOf(i).isNaN()) Text("  ✓", color = Color(0xFF8CF0B0), style = MaterialTheme.typography.labelMedium)
                }
            }
        } else Prompt(stringResource(if (g.side[me] == HiLoGame.SIDE_LOW) R.string.hl_build_low else R.string.hl_build_high))

        // dòng biểu thức đang ghép
        val sh = RoundedCornerShape(12.dp)
        Row(
            Modifier.fillMaxWidth().height(52.dp).clip(sh).background(Color.Black.copy(alpha = .25f)).padding(horizontal = 6.dp).horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            for (j in 0 until 7) {
                if (j % 2 == 0) {
                    val n = j / 2
                    if (n < numOrder[cur].size) {
                        if (sq[cur].getOrElse(n) { false }) HlCard(0, 22.dp, sqrt = true)
                        HlCard(cards[numOrder[cur][n]], 26.dp)
                    } else Slot()
                } else {
                    val o = j / 2
                    if (o < opOrder[cur].size) HlCard(0, 26.dp, op = ops[opOrder[cur][o]]) else Slot()
                }
            }
            val v = valueOf(cur)
            if (!v.isNaN()) {
                val target = if (cur == 0) HiLoGame.HIGH else HiLoGame.LOW
                Column(Modifier.padding(start = 6.dp)) {
                    Text("= ${fmtVal(v)}", color = Color(0xFFFFE08A), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.hl_off, fmtVal(abs(v - target))), color = Color(0xFFFFB4A8), fontSize = 10.sp)
                }
            } else if (codeOf(cur) >= 0) Text(stringResource(R.string.hl_invalid), Modifier.padding(start = 6.dp), color = Color(0xFFFFB4A8), style = MaterialTheme.typography.labelSmall)
        }

        // thẻ để chạm
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            for (i in 0 until 4) {
                val used = i in numOrder[cur]
                HlCard(cards[i], 32.dp, dim = used || !expectNum, onClick = if (used || !expectNum) null else ({
                    numOrder[cur].add(i); sq[cur].add(sqrtArmed && usedSqrt < k); sqrtArmed = false
                }))
            }
            Spacer(Modifier.width(4.dp))
            for (i in 0 until 3) {
                val used = i in opOrder[cur]
                HlCard(0, 32.dp, op = ops[i], dim = used || expectNum || numOrder[cur].size >= 4, onClick = if (used || expectNum || numOrder[cur].size >= 4) null else ({ opOrder[cur].add(i); Unit }))
            }
            if (k > 0) {
                Spacer(Modifier.width(4.dp))
                val left = k - usedSqrt
                Box {
                    HlCard(0, 32.dp, sqrt = true, selected = sqrtArmed, dim = left <= 0 || !expectNum, onClick = if (left <= 0 || !expectNum) null else ({ sqrtArmed = !sqrtArmed }))
                    Text("$left", Modifier.align(Alignment.TopEnd).size(14.dp).clip(CircleShape).background(pm.goldBright), color = Color.White, fontSize = 9.sp, textAlign = TextAlign.Center)
                }
            }
        }
        if (k > 0) Text(stringResource(R.string.hl_sqrt_hint, k), color = Color.White.copy(alpha = .6f), style = MaterialTheme.typography.labelSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlineButton(stringResource(R.string.hl_undo), {
                if (opOrder[cur].size >= numOrder[cur].size && opOrder[cur].isNotEmpty() && opOrder[cur].size == numOrder[cur].size) opOrder[cur].removeAt(opOrder[cur].lastIndex)
                else if (numOrder[cur].isNotEmpty()) { numOrder[cur].removeAt(numOrder[cur].lastIndex); sq[cur].removeAt(sq[cur].lastIndex) }
                sqrtArmed = false
            }, Modifier.weight(1f))
            val ready = if (swing) (0..1).all { codeOf(it) >= 0 && !valueOf(it).isNaN() && sq[it].count { s -> s } == k }
                else codeOf(cur) >= 0 && !valueOf(cur).isNaN() && usedSqrt == k
            GoldButton(stringResource(R.string.hl_submit), {
                val mv = if (swing) codeOf(0) * HiLoGame.CODES + codeOf(1) else codeOf(cur)
                vm.playMove(mv)
            }, Modifier.weight(1.4f), enabled = ready)
        }
    }
}

@Composable
private fun Slot() {
    Box(Modifier.size(26.dp, 36.dp).clip(RoundedCornerShape(4.dp)).border(1.dp, Color.White.copy(alpha = .3f), RoundedCornerShape(4.dp)))
}

// ─────────────────────────── kết quả ván bài ───────────────────────────

@Composable
private fun ShowdownDialog(vm: SessionViewModel, info: GameInfo, g: HiLoGame, sd: HiLoShowdown, onClose: () -> Unit) {
    val lines = buildList {
        for (p in 0 until g.players) {
            if (sd.payout[p] == 0 && (sd.folded[p] || g.out[p] && sd.sides[p] < 0)) continue
            add(p)
        }
    }
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        val shape = RoundedCornerShape(24.dp)
        Column(
            Modifier.fillMaxWidth().clip(shape).background(Brush.verticalGradient(listOf(FeltTop, FeltBottom))).border(1.dp, Color.White.copy(alpha = .2f), shape).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.hl_result_title, sd.hand), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (sd.uncontested) Text(stringResource(R.string.hl_uncontested), color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.bodyMedium)
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (p in lines) {
                    val won = sd.payout[p]
                    val sh = RoundedCornerShape(14.dp)
                    Column(
                        Modifier.fillMaxWidth().clip(sh).background(if (won > 0) Color(0x3328C76F) else Color.White.copy(alpha = .06f)).padding(9.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(playerColor(p)))
                            Text(seatName(vm, info, p), Modifier.weight(1f), color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (sd.sides[p] >= 0 && !sd.uncontested) SideMarker(sd.sides[p], 22.dp)
                            if (won > 0) Text(stringResource(R.string.hl_won, won), color = Color(0xFF8CF0B0), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                        when {
                            sd.uncontested -> {}
                            sd.folded[p] -> Text(stringResource(R.string.hl_folded), color = Color.White.copy(alpha = .6f), style = MaterialTheme.typography.labelSmall)
                            sd.failed[p] || sd.exprs[p] < 0 -> Text(stringResource(R.string.hl_failed), color = Color(0xFFFFB4A8), style = MaterialTheme.typography.labelSmall)
                            else -> {
                                val c = sd.nums[p]; val o = sd.ops[p]
                                val hi = if (sd.sides[p] == HiLoGame.SIDE_SWING) sd.exprs[p] / HiLoGame.CODES else sd.exprs[p]
                                val lo = if (sd.sides[p] == HiLoGame.SIDE_SWING) sd.exprs[p] % HiLoGame.CODES else sd.exprs[p]
                                if (sd.sides[p] != HiLoGame.SIDE_LOW) ExprLine(exprText(hi, c, o), sd.high[p], HiLoGame.HIGH, p == sd.highWinner)
                                if (sd.sides[p] != HiLoGame.SIDE_HIGH) ExprLine(exprText(lo, c, o), sd.low[p], HiLoGame.LOW, p == sd.lowWinner)
                            }
                        }
                    }
                }
            }
            GoldButton(stringResource(R.string.hl_continue), onClose, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ExprLine(text: String, v: Double, target: Double, best: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f), color = Color.White, fontFamily = FontFamily.Serif, style = MaterialTheme.typography.bodyMedium)
        Text("= ${fmtVal(v)}", color = if (best) Color(0xFFFFE08A) else Color(0xFFCDB8FF), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        if (!v.isNaN()) Text(" (${fmtVal(abs(v - target))})", color = Color(0xFFFFB4A8), style = MaterialTheme.typography.labelSmall)
    }
}

// ─────────────────────────── hình đại diện game ───────────────────────────

/** Ảnh game: thẻ "20" ô thoi đen và thẻ "1" tròn trắng trên hai lá bài. */
fun DrawScope.drawHiLoArt(ox: Float, oy: Float, s: Float) {
    fun card(x: Float, y: Float, rot: Float) {
        val w = s * .34f; val h = s * .48f
        val c = Offset(ox + x, oy + y)
        rotate(rot, c) {
            drawRoundRect(Color.Black.copy(alpha = .18f), Offset(c.x - w / 2 + s * .02f, c.y - h / 2 + s * .03f), Size(w, h), CornerRadius(s * .05f))
            drawRoundRect(Color.White, Offset(c.x - w / 2, c.y - h / 2), Size(w, h), CornerRadius(s * .05f))
            drawRoundRect(Color(0x22000000), Offset(c.x - w / 2, c.y - h / 2), Size(w, h), CornerRadius(s * .05f), style = Stroke(s * .012f))
        }
    }
    card(s * .34f, s * .5f, -12f); card(s * .66f, s * .5f, 12f)
    // ô thoi đen (20)
    val d = Offset(ox + s * .34f, oy + s * .5f); val r = s * .12f
    val path = androidx.compose.ui.graphics.Path().apply { moveTo(d.x, d.y - r); lineTo(d.x + r, d.y); lineTo(d.x, d.y + r); lineTo(d.x - r, d.y); close() }
    drawPath(path, Color(0xFF15131C))
    // tròn trắng viền (1)
    drawCircle(Color(0xFFF1EEF6), s * .105f, Offset(ox + s * .66f, oy + s * .5f))
    drawCircle(Color(0xFF15131C), s * .105f, Offset(ox + s * .66f, oy + s * .5f), style = Stroke(s * .02f))
    drawCircle(Color(0xFF6A3FD0), s * .03f, Offset(ox + s * .66f, oy + s * .5f))
    drawCircle(Color.White, s * .03f, d)
}
