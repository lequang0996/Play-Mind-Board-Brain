package com.fws.playmind.match

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fws.playmind.PlayMindApp
import com.fws.playmind.catalog.GameInfo
import com.fws.playmind.core.BoardGame
import com.fws.playmind.core.SudokuGame
import com.fws.playmind.core.SudokuLevel
import com.fws.playmind.core.SudokuStatus
import com.fws.playmind.catalog.GameKind
import com.fws.playmind.catalog.OptionKind
import androidx.compose.runtime.mutableStateListOf
import com.fws.playmind.core.EndReason
import com.fws.playmind.core.Outcome
import com.fws.playmind.core.movesHash
import com.fws.playmind.data.MatchOutcome
import com.fws.playmind.data.Reward
import com.fws.playmind.data.StartCost
import com.fws.playmind.net.FoundRoom
import com.fws.playmind.net.LinkState
import com.fws.playmind.net.NearbyTransport
import com.fws.playmind.net.Proto
import com.fws.playmind.net.Transport
import com.fws.playmind.net.WebRtcTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

enum class Mode { NONE, BOT, LOCAL, SOLO, HOST_NEARBY, HOST_NET, JOIN_NEARBY, JOIN_NET }
enum class Phase { IDLE, ROOM, PLAYING, OVER }
enum class Opp { NONE, HUMAN, COMPUTER, HIDDEN_BOT }
enum class Notice { NONE, NOT_ENOUGH, PERMISSION, CONN_LOST, OPP_LEFT, KICKED, BAD_CODE, CONN_FAILED, UNVERIFIED, HOST_LEFT }

/**
 * Điều phối toàn bộ một phiên chơi: phòng chờ → ván đấu → kết quả. Dùng chung cho 3 chế độ:
 * chơi với máy, chơi gần nhau (Nearby), chơi qua internet (WebRTC).
 *
 * Quy ước: mySide/oppSide = 0 (đi trước) hoặc 1 (đi sau).
 * Đối thủ "bot trong phòng" (HIDDEN_BOT) được hiển thị như một người chơi bình thường; việc này
 * được nêu rõ trong mô tả, privacy policy và phần hướng dẫn của app.
 */
class SessionViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = app as PlayMindApp
    val wallet = graph.wallet
    val prefs = graph.prefs

    var info by mutableStateOf<GameInfo?>(null); private set
    var mode by mutableStateOf(Mode.NONE); private set
    var phase by mutableStateOf(Phase.IDLE); private set
    var game: BoardGame? = null; private set
    var tick by mutableIntStateOf(0); private set          // tăng mỗi khi bàn cờ thay đổi
    /** Tùy chọn trước ván: số người chơi (Wall Go, Quoridor) hoặc cỡ bàn (cờ vây) hoặc mức độ (Sudoku). */
    var option by mutableIntStateOf(0); private set
    var seatNames by mutableStateOf<List<String>>(emptyList()); private set   // tên từng ghế khi chơi với máy 3-4 người
    // Sudoku (game một người)
    var sudoku by mutableStateOf<SudokuGame?>(null); private set
    var sudokuLoading by mutableStateOf(false); private set
    var sudokuSeconds by mutableIntStateOf(0); private set
    var sudokuTick by mutableIntStateOf(0); private set
    private var sudokuJob: Job? = null
    private var sudokuClock: Job? = null
    var mySide by mutableIntStateOf(0); private set
    var opp by mutableStateOf(Opp.NONE); private set
    var oppName by mutableStateOf(""); private set
    var botThinking by mutableStateOf(false); private set
    var notice by mutableStateOf(Notice.NONE)
    var reward by mutableStateOf<Reward?>(null); private set
    var verifying by mutableStateOf(false); private set
    var inviteCode by mutableStateOf(""); private set
    var replyCode by mutableStateOf(""); private set
    var inviteBusy by mutableStateOf(false); private set
    var inviteOpen by mutableStateOf(false)
    var guestConnected by mutableStateOf(false); private set  // khách đã kết nối, chờ chủ phòng bắt đầu
    var rematchAsked by mutableStateOf(false); private set
    var oppAskedRematch by mutableStateOf(false); private set
    val nearbyRooms = MutableStateFlow<List<FoundRoom>>(emptyList())
    val nearbyError = MutableStateFlow(false)
    val nearbyErrorCode = MutableStateFlow(0)
    val linkState: StateFlow<LinkState> get() = _link
    private val _link = MutableStateFlow(LinkState.IDLE)

    private var transport: Transport? = null
    private var hostSide = 0
    private var botJob: Job? = null
    private var timerJob: Job? = null
    private var linkJob: Job? = null
    private var verifyJob: Job? = null
    private var localEnd: String? = null
    private var remoteEnd: String? = null
    private var epoch = 0                                  // vô hiệu hóa các tác vụ bot cũ
    private var finalized = false

    private fun lang(): String = AppCompatDelegate.getApplicationLocales().let { if (it.isEmpty()) "en" else it[0]?.language ?: "en" }
    val isOnline get() = mode == Mode.HOST_NEARBY || mode == Mode.HOST_NET || mode == Mode.JOIN_NEARBY || mode == Mode.JOIN_NET
    val isHost get() = mode == Mode.HOST_NEARBY || mode == Mode.HOST_NET
    val oppSide get() = 1 - mySide
    /** Số người chơi của ván hiện tại (2 với mọi game không có tùy chọn số người). */
    val playerCount get() = if (info?.optionKind == OptionKind.PLAYERS) option else 2
    /** Người dùng có được đi ngay bây giờ không (chế độ chơi chung một máy: bên nào đang đến lượt cũng được). */
    val canAct get() = game?.let { it.result == Outcome.ONGOING && phase == Phase.PLAYING && (mode == Mode.LOCAL || it.turn == mySide) } ?: false
    val myTurn get() = game?.let { it.result == Outcome.ONGOING && (mode == Mode.LOCAL || it.turn == mySide) } ?: false
    /** Bên đang được người dùng điều khiển: khi chơi chung một máy thì là bên đang đến lượt. */
    val actingSide get() = if (mode == Mode.LOCAL) (game?.turn ?: 0) else mySide
    val oppIsMachine get() = opp == Opp.COMPUTER || opp == Opp.HIDDEN_BOT

    // ───────────────────────── vào / thoát ─────────────────────────
    fun open(g: GameInfo) {
        teardown()
        info = g; mode = Mode.NONE; phase = Phase.IDLE; notice = Notice.NONE; reward = null; option = g.defaultOption
    }

    fun selectOption(v: Int) { option = v }

    /** Thoát hoàn toàn phiên hiện tại (về lobby / trang chủ). Rời giữa ván = thua. */
    fun leave() {
        val g = game
        val inf = info
        val sd = sudoku
        if (phase == Phase.PLAYING && sd != null && inf != null && sd.status == SudokuStatus.PLAYING) {
            sd.giveUp()
            wallet.finishMatch(inf.id, MatchOutcome.LOSS, true)
        } else if (phase == Phase.PLAYING && g != null && inf != null && g.result == Outcome.ONGOING) {
            if (mode == Mode.LOCAL) { /* chơi chung một máy: không tính thắng/thua */ } else {
                if (isOnline) transport?.send(Proto.QUIT)
                g.forfeit(mySide, EndReason.LEAVE)
                wallet.finishMatch(inf.id, MatchOutcome.LOSS, true)
            }
        } else if (phase == Phase.ROOM && isOnline) transport?.send(Proto.QUIT)
        teardown()
        phase = Phase.IDLE; mode = Mode.NONE
    }

    private fun teardown() {
        epoch++
        botJob?.cancel(); timerJob?.cancel(); linkJob?.cancel(); verifyJob?.cancel()
        transport?.onMessage = null
        transport?.close(); transport = null
        game = null; opp = Opp.NONE; oppName = ""; botThinking = false; verifying = false
        sudokuJob?.cancel(); sudokuClock?.cancel(); sudoku = null; sudokuLoading = false; sudokuSeconds = 0; seatNames = emptyList()
        inviteCode = ""; replyCode = ""; inviteBusy = false; inviteOpen = false; guestConnected = false
        rematchAsked = false; oppAskedRematch = false; localEnd = null; remoteEnd = null; finalized = false
        nearbyRooms.value = emptyList(); nearbyError.value = false; nearbyErrorCode.value = 0; _link.value = LinkState.IDLE
    }

    // ───────────────────────── chơi với máy ─────────────────────────
    /** Chơi với máy: người chơi LUÔN đi trước. Trả về false nếu không đủ kim cương. */
    fun startVsComputer(): Boolean {
        val inf = info ?: return false
        if (!wallet.canStart(inf.id)) { notice = Notice.NOT_ENOUGH; return false }
        teardown()
        mode = Mode.BOT; opp = Opp.COMPUTER
        mySide = 0
        if (playerCount > 2) {
            val used = HashSet<String>(); val names = ArrayList<String>()
            for (seat in 0 until playerCount) {
                if (seat == 0) { names.add(prefs.nickname); continue }
                var n = BotNames.pick(lang()); var tries = 0
                while (n in used && tries++ < 20) n = BotNames.pick(lang())
                used.add(n); names.add(n)
            }
            seatNames = names
        }
        return beginMatch()
    }

    // ───────────────────────── Sudoku (một người) ─────────────────────────
    fun startSudoku(levelIndex: Int): Boolean {
        val inf = info ?: return false
        if (!wallet.canStart(inf.id)) { notice = Notice.NOT_ENOUGH; return false }
        teardown()
        if (wallet.startMatch(inf.id) == StartCost.INSUFFICIENT) { notice = Notice.NOT_ENOUGH; return false }
        val level = SudokuLevel.values().getOrElse(levelIndex) { SudokuLevel.EASY }
        option = levelIndex
        mode = Mode.SOLO; reward = null; finalized = false
        sudokuLoading = true; sudokuSeconds = 0
        phase = Phase.PLAYING; tick++
        val my = ++epoch
        sudokuJob = viewModelScope.launch {
            val g = withContext(Dispatchers.Default) { SudokuGame.create(level) }
            if (my != epoch) return@launch
            sudoku = g; sudokuLoading = false; sudokuTick++
            sudokuClock = launch { while (true) { delay(1000); if (sudoku?.status == SudokuStatus.PLAYING) sudokuSeconds++ } }
        }
        return true
    }

    /** Mọi thao tác trên bàn Sudoku đi qua đây để bàn được vẽ lại và kết quả được chốt. */
    fun sudokuEdit(block: (SudokuGame) -> Unit) {
        val g = sudoku ?: return
        if (phase != Phase.PLAYING) return
        block(g)
        sudokuTick++
        if (g.status != SudokuStatus.PLAYING) {
            phase = Phase.OVER
            settle(if (g.status == SudokuStatus.WON) MatchOutcome.WIN else MatchOutcome.LOSS, true)
        }
    }

    // ───────────────────────── tạo phòng (chủ phòng) ─────────────────────────
    fun hostRoom(nearby: Boolean): Boolean {
        val inf = info ?: return false
        if (!wallet.canStart(inf.id)) { notice = Notice.NOT_ENOUGH; return false }
        teardown()
        mode = if (nearby) Mode.HOST_NEARBY else Mode.HOST_NET
        phase = Phase.ROOM
        newHostTransport()
        armBotTimer(10)
        return true
    }

    private fun newHostTransport() {
        val inf = info ?: return
        transport?.onMessage = null; linkJob?.cancel(); transport?.close()
        val ctx = getApplication<Application>()
        if (mode == Mode.HOST_NEARBY) {
            val t = NearbyTransport(ctx, inf.id); transport = t
            t.host(prefs.nickname)
            viewModelScope.launch { t.error.collect { nearbyError.value = it } }
            viewModelScope.launch { t.errorCode.collect { nearbyErrorCode.value = it } }
        } else {
            val t = WebRtcTransport(ctx); transport = t
            inviteCode = ""; replyCode = ""; inviteBusy = true
            viewModelScope.launch {
                try { inviteCode = t.createInvite() } catch (e: Exception) { notice = Notice.CONN_FAILED }
                inviteBusy = false
            }
        }
        attachTransport()
    }

    /** Chủ phòng dán/quét mã trả lời của khách. */
    fun acceptReply(code: String) {
        val t = transport as? WebRtcTransport ?: return
        viewModelScope.launch { if (!t.acceptReply(code)) notice = Notice.BAD_CODE }
    }

    private fun armBotTimer(seconds: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var left = seconds
            while (left > 0) {
                delay(1000)
                if (phase != Phase.ROOM || opp != Opp.NONE) return@launch
                // chờ khi chủ phòng đang mời bạn bè hoặc đang bắt tay kết nối
                if (!inviteOpen && _link.value != LinkState.CONNECTING) left--
            }
            if (phase == Phase.ROOM && opp == Opp.NONE) { opp = Opp.HIDDEN_BOT; oppName = BotNames.pick(lang()) }
        }
    }

    /** Mời đối thủ ra khỏi ghế (bot hoặc người). 5 giây sau nếu chưa ai vào thì thêm bot mới. */
    fun kick() {
        if (phase != Phase.ROOM || !isHost) return
        val wasHuman = opp == Opp.HUMAN
        if (wasHuman) { transport?.send(Proto.KICK); opp = Opp.NONE; oppName = ""; newHostTransport() }
        else { opp = Opp.NONE; oppName = "" }
        armBotTimer(5)
    }

    /** Chủ phòng bấm bắt đầu. */
    fun hostStart() {
        val inf = info ?: return
        if (phase != Phase.ROOM || opp == Opp.NONE) return
        if (!wallet.canStart(inf.id)) { notice = Notice.NOT_ENOUGH; return }
        hostSide = Random.nextInt(2); mySide = hostSide
        if (opp == Opp.HUMAN) transport?.send(Proto.start(hostSide, option))
        timerJob?.cancel()
        beginMatch()
    }

    // ───────────────────────── vào phòng (khách) ─────────────────────────
    fun joinNearby(): Boolean {
        val inf = info ?: return false
        if (!wallet.canStart(inf.id)) { notice = Notice.NOT_ENOUGH; return false }
        teardown()
        mode = Mode.JOIN_NEARBY; phase = Phase.ROOM
        val t = NearbyTransport(getApplication(), inf.id); transport = t
        t.discover()
        viewModelScope.launch { t.rooms.collect { nearbyRooms.value = it } }
        viewModelScope.launch { t.error.collect { nearbyError.value = it } }
        viewModelScope.launch { t.errorCode.collect { nearbyErrorCode.value = it } }
        attachTransport()
        return true
    }

    fun connectTo(room: FoundRoom) { (transport as? NearbyTransport)?.connect(room, prefs.nickname) }

    fun joinNet(): Boolean {
        val inf = info ?: return false
        if (!wallet.canStart(inf.id)) { notice = Notice.NOT_ENOUGH; return false }
        teardown()
        mode = Mode.JOIN_NET; phase = Phase.ROOM
        transport = WebRtcTransport(getApplication())
        attachTransport()
        return true
    }

    /** Khách dán/quét mã mời → tạo mã trả lời. */
    fun submitInvite(code: String) {
        val t = transport as? WebRtcTransport ?: return
        inviteBusy = true
        viewModelScope.launch {
            val r = try { t.createReply(code) } catch (e: Exception) { null }
            if (r == null) notice = Notice.BAD_CODE else replyCode = r
            inviteBusy = false
        }
    }

    // ───────────────────────── kết nối ─────────────────────────
    private fun attachTransport() {
        val t = transport ?: return
        t.onMessage = { msg -> viewModelScope.launch { onRemote(msg) } }
        linkJob = viewModelScope.launch {
            t.state.collect { s ->
                _link.value = s
                when (s) {
                    LinkState.CONNECTED -> onLinkUp()
                    LinkState.CLOSED, LinkState.FAILED -> onLinkDown(s == LinkState.FAILED)
                    else -> {}
                }
            }
        }
    }

    private fun onLinkUp() {
        val inf = info ?: return
        if (isHost && phase == Phase.ROOM) transport?.send(Proto.hello(inf.id, prefs.nickname, 0))
    }

    private fun onLinkDown(failed: Boolean) {
        val inf = info ?: return
        when {
            phase == Phase.PLAYING && game?.result == Outcome.ONGOING -> {
                game?.forfeit(mySide, EndReason.LEAVE)
                wallet.finishMatch(inf.id, MatchOutcome.LOSS, true)
                notice = Notice.CONN_LOST; phase = Phase.OVER; verifying = false; tick++
            }
            phase == Phase.ROOM && isHost && opp == Opp.HUMAN -> {
                opp = Opp.NONE; oppName = ""; notice = Notice.OPP_LEFT; newHostTransport(); armBotTimer(5)
            }
            phase == Phase.ROOM && !isHost -> { notice = if (failed) Notice.CONN_FAILED else Notice.HOST_LEFT; guestConnected = false }
            phase == Phase.ROOM && isHost && failed -> notice = Notice.CONN_FAILED
        }
    }

    private fun onRemote(msg: String) {
        val p = msg.split("|")
        when (p[0]) {
            "H" -> if (!isHost && phase == Phase.ROOM && p.size >= 4 && p[1] == info?.id) {
                oppName = p[2]; opp = Opp.HUMAN; guestConnected = true
                transport?.send(Proto.join(prefs.nickname))
            }
            "J" -> if (isHost && phase == Phase.ROOM && p.size >= 2) {
                timerJob?.cancel(); opp = Opp.HUMAN; oppName = p[1].take(16)
            }
            "S" -> if (!isHost && phase == Phase.ROOM || phase == Phase.OVER && !isHost) {
                hostSide = p.getOrNull(1)?.toIntOrNull() ?: return
                p.getOrNull(2)?.toIntOrNull()?.let { o -> if (info?.options?.contains(o) == true) option = o }
                mySide = 1 - hostSide
                beginMatch()
            }
            "M" -> onRemoteMove(p)
            "R" -> remoteForfeit(EndReason.RESIGN)
            "Q" -> if (phase == Phase.PLAYING) remoteForfeit(EndReason.LEAVE)
                   else if (phase == Phase.ROOM && isHost) { /* khách rời: onLinkDown xử lý */ }
                   else if (phase == Phase.OVER) { oppAskedRematch = false; opp = Opp.NONE }
            "K" -> if (!isHost) { notice = Notice.KICKED; leave() }
            "RM" -> { oppAskedRematch = true; maybeRematch() }
            "E" -> { remoteEnd = msg; tryFinalizeOnline() }
        }
    }

    private fun onRemoteMove(p: List<String>) {
        val g = game ?: return
        if (phase != Phase.PLAYING || g.turn == mySide || p.size < 3) return
        val idx = p[1].toIntOrNull() ?: return
        val mv = p[2].toIntOrNull() ?: return
        if (idx != g.plies || !g.play(mv)) { transport?.close(); return }
        afterMove()
    }

    private fun remoteForfeit(reason: EndReason) {
        val g = game ?: return
        if (phase != Phase.PLAYING) return
        g.forfeit(oppSide, reason)
        afterMove()
    }

    // ───────────────────────── ván đấu ─────────────────────────
    private fun beginMatch(): Boolean {
        val inf = info ?: return false
        if (mode != Mode.LOCAL && wallet.startMatch(inf.id) == StartCost.INSUFFICIENT) {
            notice = Notice.NOT_ENOUGH
            if (isOnline) { transport?.send(Proto.QUIT) }
            teardown(); phase = Phase.IDLE; mode = Mode.NONE
            return false
        }
        epoch++
        game = inf.make(option); reward = null; verifying = false; finalized = false
        localEnd = null; remoteEnd = null; rematchAsked = false; oppAskedRematch = false; botThinking = false
        phase = Phase.PLAYING; tick++
        scheduleBotIfNeeded()
        return true
    }

    fun playMove(move: Int): Boolean {
        val g = game ?: return false
        if (!canAct) return false
        val index = g.plies
        if (!g.play(move)) return false
        if (isOnline && opp == Opp.HUMAN) transport?.send(Proto.move(index, g.lastMove))
        afterMove()
        return true
    }

    private fun afterMove() {
        val g = game ?: return
        tick++
        if (g.result != Outcome.ONGOING) onGameOver() else scheduleBotIfNeeded()
    }

    private fun scheduleBotIfNeeded() {
        val g = game ?: return; val inf = info ?: return
        if (!oppIsMachine || phase != Phase.PLAYING || g.result != Outcome.ONGOING || g.turn == mySide) return
        val my = epoch
        botThinking = true
        botJob?.cancel()
        botJob = viewModelScope.launch {
            val snap = g.copyGame()
            val t0 = System.currentTimeMillis()
            val mv = withContext(Dispatchers.Default) { snap.bestMove(inf.botMillis(option)) }
            val wait = BotNames.humanDelay() - (System.currentTimeMillis() - t0)
            if (wait > 0) delay(wait)
            if (my != epoch || game !== g || phase != Phase.PLAYING) return@launch
            botThinking = false
            if (mv >= 0 && g.play(mv)) afterMove()
        }
    }

    fun resign() {
        val g = game ?: return
        if (phase != Phase.PLAYING || g.result != Outcome.ONGOING) return
        if (isOnline && opp == Opp.HUMAN) transport?.send(Proto.RESIGN)
        g.forfeit(if (mode == Mode.LOCAL) g.turn else mySide, EndReason.RESIGN)
        afterMove()
    }

    private fun outcomeForMe(g: BoardGame): MatchOutcome = when {
        g.result == Outcome.DRAW -> MatchOutcome.DRAW
        (g.result == Outcome.WIN_FIRST) == (mySide == 0) -> MatchOutcome.WIN
        else -> MatchOutcome.LOSS
    }

    private fun rewardValid(g: BoardGame, inf: GameInfo) =
        !((g.reason == EndReason.RESIGN || g.reason == EndReason.LEAVE) && g.plies < inf.minPlies)

    private fun onGameOver() {
        val g = game ?: return; val inf = info ?: return
        botJob?.cancel(); botThinking = false
        phase = Phase.OVER
        if (mode == Mode.LOCAL) { finalized = true; return }       // chơi chung một máy: không thưởng, không thống kê
        val out = outcomeForMe(g)
        if (isOnline && opp == Opp.HUMAN) {
            localEnd = Proto.end(g.result, movesHash(g.history))
            transport?.send(localEnd!!)
            if (out == MatchOutcome.WIN) {
                verifying = true
                verifyJob?.cancel()
                verifyJob = viewModelScope.launch { delay(7000); if (!finalized) { finalized = true; verifying = false; wallet.cancelMatch(); notice = Notice.UNVERIFIED } }
                tryFinalizeOnline()
            } else settle(out, true)
        } else settle(out, rewardValid(g, inf))
    }

    private fun tryFinalizeOnline() {
        val g = game ?: return; val inf = info ?: return
        if (phase != Phase.OVER || finalized || localEnd == null || remoteEnd == null) return
        if (localEnd == remoteEnd) { verifyJob?.cancel(); verifying = false; settle(outcomeForMe(g), rewardValid(g, inf)) }
        else { finalized = true; verifyJob?.cancel(); verifying = false; wallet.cancelMatch(); notice = Notice.UNVERIFIED }
    }

    private fun settle(out: MatchOutcome, valid: Boolean) {
        if (finalized) return
        finalized = true
        val inf = info ?: return
        reward = wallet.finishMatch(inf.id, out, valid)
        tick++
    }

    /** Chỉ dùng cho ScreenshotActivity (bản debug): nạp sẵn một thế cờ mẫu để chụp ảnh cửa hàng. */
    fun loadDemo(g: GameInfo, board: BoardGame, side: Int, opponent: String) {
        teardown()
        info = g; game = board; mySide = side; mode = Mode.BOT; opp = Opp.HUMAN; oppName = opponent
        phase = Phase.PLAYING; tick++
    }

    // ───────────────────────── chơi lại ─────────────────────────
    fun rematch() {
        val inf = info ?: return
        if (phase != Phase.OVER) return
        if (mode == Mode.LOCAL) { beginMatch(); return }
        if (mode == Mode.SOLO) { startSudoku(option); return }
        if (!wallet.canStart(inf.id)) { notice = Notice.NOT_ENOUGH; return }
        if (isOnline && opp == Opp.HUMAN) { rematchAsked = true; transport?.send(Proto.REMATCH); maybeRematch() }
        else if (oppIsMachine) { mySide = 0; beginMatch() }
    }

    private fun maybeRematch() {
        if (phase != Phase.OVER || !rematchAsked || !oppAskedRematch) return
        if (isHost) { hostSide = 1 - hostSide; mySide = hostSide; transport?.send(Proto.start(hostSide, option)); beginMatch() }
    }

    override fun onCleared() { teardown(); super.onCleared() }
}
