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
import com.fws.playmind.core.BotLevel
import com.fws.playmind.core.GoGame
import com.fws.playmind.core.LeveledEngine
import com.fws.playmind.catalog.Catalog
import com.fws.playmind.core.SudokuGame
import com.fws.playmind.core.SudokuLevel
import com.fws.playmind.core.SudokuStatus
import com.fws.playmind.core.QuoridorGame
import com.fws.playmind.core.WallGoGame
import com.fws.playmind.net.NearbyHub
import kotlinx.coroutines.flow.first
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
import com.fws.playmind.net.Relay
import com.fws.playmind.net.Transport
import com.fws.playmind.net.WebRtcTransport
import kotlinx.coroutines.CoroutineScope
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
enum class Notice { NONE, NOT_ENOUGH, PERMISSION, CONN_LOST, OPP_LEFT, KICKED, BAD_CODE, CONN_FAILED, UNVERIFIED, HOST_LEFT, ROOM_TAKEN }

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
    /** Cấp bot đang chọn (0 = Dễ ... 6 = Siêu việt), luôn trong phạm vi đã mở khóa của game. */
    var botLevel by mutableIntStateOf(0); private set
    /** Thời gian mỗi nước khi chơi với người (chỉ số trong Catalog.LEVEL_MOVE_SECONDS). Chủ phòng chọn, khách nhận theo chủ phòng. */
    var pvpTime by mutableIntStateOf(prefs.pvpTime); private set
    /** Cấp vừa được mở khóa nhờ thắng ván này (-1 nếu không có), để hộp kết quả báo cho người chơi. */
    var unlockedNow by mutableIntStateOf(-1); private set
    /** Bot cờ vây KataGo (khởi động khi cần; null nếu chưa dùng). */
    private var kata: KataGo? = null
    var seatNames by mutableStateOf<List<String>>(emptyList()); private set   // tên từng ghế khi chơi với máy 3-4 người
    // Sudoku (game một người)
    var sudoku by mutableStateOf<SudokuGame?>(null); private set
    var sudokuLoading by mutableStateOf(false); private set
    var sudokuSeconds by mutableIntStateOf(0); private set
    var sudokuTick by mutableIntStateOf(0); private set
    private var sudokuJob: Job? = null
    private var sudokuClock: Job? = null
    /** Đồng hồ nước đi: thời điểm (ms, đồng hồ hệ thống) hết giờ của lượt hiện tại và số giây còn lại để hiển thị. */
    var turnDeadline by mutableStateOf(0L); private set
    var turnSecondsLeft by mutableIntStateOf(0); private set
    private var turnClock: Job? = null
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

    // ── Phòng 3-4 người: chủ phòng là trạm trung tâm ──
    /** Một khách trong phòng nhiều người (chỉ có ở máy chủ phòng). */
    inner class Peer(val t: Transport) {
        var name by mutableStateOf("")
        var seat = -1
        var gone = false
    }
    /** Ván online có hơn 2 người. */
    var multi by mutableStateOf(false); private set
    val peers = mutableStateListOf<Peer>()
    /** Danh sách tên trong phòng (khách nhận từ chủ phòng). */
    var roster by mutableStateOf<List<String>>(emptyList()); private set
    /** Bạn đã bị xử thua giữa ván nhiều người (đầu hàng / hết giờ); ván vẫn tiếp tục với máy thay ghế của bạn. */
    var eliminated by mutableStateOf(false); private set
    var eliminatedReason by mutableStateOf(EndReason.NONE); private set
    private val botSeats = HashSet<Int>()
    private var hub: NearbyHub? = null
    private var pendingNet: WebRtcTransport? = null
    /** Phòng đang mở trên trạm chuyển mã (Relay) của máy chủ phòng, để xóa khi không dùng nữa. */
    private val relayRooms = ArrayList<String>()
    private var relayJob: Job? = null
    /** Khách (internet) đã giành được chỗ qua trạm chuyển mã, đang chờ kết nối. */
    var relayJoined by mutableStateOf(false); private set
    private val peerJobs = ArrayList<Job>()
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
    val canAct get() = !eliminated && game?.let { it.result == Outcome.ONGOING && phase == Phase.PLAYING && (mode == Mode.LOCAL || it.turn == mySide) } ?: false
    val myTurn get() = game?.let { it.result == Outcome.ONGOING && (mode == Mode.LOCAL || it.turn == mySide) } ?: false
    /** Bên đang được người dùng điều khiển: khi chơi chung một máy thì là bên đang đến lượt. */
    val actingSide get() = if (mode == Mode.LOCAL) (game?.turn ?: 0) else mySide
    val oppIsMachine get() = opp == Opp.COMPUTER || opp == Opp.HIDDEN_BOT

    // ───────────────────────── vào / thoát ─────────────────────────
    fun open(g: GameInfo) {
        teardown()
        info = g; mode = Mode.NONE; phase = Phase.IDLE; notice = Notice.NONE; reward = null; option = g.defaultOption
        botLevel = prefs.selectedLevel(g.id)
    }

    fun selectOption(v: Int) { option = v }

    fun selectPvpTime(i: Int) { pvpTime = i.coerceIn(0, Catalog.LEVELS - 1); prefs.pvpTime = pvpTime }

    /** Chọn cấp bot. Cấp chưa mở khóa thì không chọn được (trả về false). */
    fun selectLevel(level: Int): Boolean {
        val inf = info ?: return false
        if (level > prefs.unlockedLevel(inf.id)) return false
        prefs.selectLevel(inf.id, level); botLevel = level
        return true
    }

    /**
     * Thời gian tối đa cho mỗi nước của ván hiện tại (giây). Chơi với máy: theo cấp bot (Catalog.LEVEL_MOVE_SECONDS);
     * chơi với người: cùng 7 mức đó, theo lựa chọn [pvpTime]; game một người: 0 (không giới hạn).
     */
    val moveSeconds: Int get() {
        val inf = info ?: return 0
        if (inf.kind == GameKind.SOLO) return 0
        val i = if (mode == Mode.BOT) botLevel else pvpTime
        return Catalog.LEVEL_MOVE_SECONDS[i.coerceIn(0, Catalog.LEVELS - 1)]
    }

    /** Cấp của bot ở ván hiện tại: chơi với máy theo cấp đã chọn; bot thay ghế trống / bot ẩn trong phòng dùng cấp Khá. */
    private fun matchLevel(): BotLevel = if (mode == Mode.BOT) BotLevel.of(botLevel) else BotLevel.GOOD

    /** Tính nước của bot ở cấp [lv] (chạy trên luồng nền với bản sao [g]). Cờ vây dùng KataGo nếu chạy được. */
    private fun computeBotMove(g: BoardGame, inf: GameInfo, lv: BotLevel): Int {
        val ms = inf.botMillis(option)
        if (g is GoGame) {
            val k = kata ?: KataGo(getApplication<Application>()).also { kata = it }
            k.move(g, lv, ms, Random)?.let { return it }
        }
        return if (g is LeveledEngine) g.levelMove(lv, ms, Random) else g.bestMove(ms)
    }

    /** Thoát hoàn toàn phiên hiện tại (về lobby / trang chủ). Rời giữa ván = thua. */
    fun leave() {
        val g = game
        val inf = info
        val sd = sudoku
        if (phase == Phase.PLAYING && sd != null && inf != null && sd.status == SudokuStatus.PLAYING) {
            sd.giveUp()
            wallet.finishMatch(inf.id, MatchOutcome.LOSS, true)
        } else if (phase == Phase.PLAYING && g != null && inf != null && g.result == Outcome.ONGOING) {
            if (mode == Mode.LOCAL) { /* chơi chung một máy: không tính thắng/thua */ } else if (multi) {
                if (isHost) broadcast(Proto.QUIT) else transport?.send(Proto.QUIT)
                if (!eliminated) wallet.finishMatch(inf.id, MatchOutcome.LOSS, true)
            } else {
                if (isOnline) transport?.send(Proto.QUIT)
                g.forfeit(mySide, EndReason.LEAVE)
                wallet.finishMatch(inf.id, MatchOutcome.LOSS, true)
            }
        } else if (phase == Phase.ROOM && isOnline) { transport?.send(Proto.QUIT); broadcast(Proto.QUIT) }
        else if (phase == Phase.OVER && multi && isHost) broadcast(Proto.QUIT)
        teardown()
        phase = Phase.IDLE; mode = Mode.NONE
    }

    private fun teardown() {
        epoch++
        botJob?.cancel(); timerJob?.cancel(); linkJob?.cancel(); verifyJob?.cancel(); turnClock?.cancel()
        turnDeadline = 0L; turnSecondsLeft = 0
        transport?.onMessage = null
        transport?.close(); transport = null
        for (j in peerJobs) j.cancel()
        peerJobs.clear()
        for (pr in peers) { pr.t.onMessage = null; pr.t.close() }
        peers.clear(); hub?.close(); hub = null; pendingNet?.close(); pendingNet = null
        botSeats.clear(); multi = false; roster = emptyList(); eliminated = false; eliminatedReason = EndReason.NONE
        game = null; opp = Opp.NONE; oppName = ""; botThinking = false; verifying = false
        sudokuJob?.cancel(); sudokuClock?.cancel(); sudoku = null; sudokuLoading = false; sudokuSeconds = 0; seatNames = emptyList()
        inviteCode = ""; replyCode = ""; inviteBusy = false; inviteOpen = false; guestConnected = false
        relayJob?.cancel(); relayJob = null; dropRelayRooms(); relayJoined = false
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
            // game một người: không giới hạn thời gian
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
        if (playerCount > 2) { startMultiRoom(nearby); return true }
        newHostTransport()
        armBotTimer(10)
        return true
    }

    // ───────────────────────── phòng 3-4 người (chủ phòng) ─────────────────────────
    private fun startMultiRoom(nearby: Boolean) {
        val inf = info ?: return
        multi = true; opp = Opp.HUMAN
        if (nearby) {
            val h = NearbyHub(getApplication(), inf.id, playerCount - 1); hub = h
            h.onPeer = { link -> viewModelScope.launch { attachPeer(link) } }
            peerJobs += viewModelScope.launch { h.error.collect { nearbyError.value = it } }
            peerJobs += viewModelScope.launch { h.errorCode.collect { nearbyErrorCode.value = it } }
            h.start(prefs.nickname)
        } else newNetInvite()
    }

    /** Số ghế còn trống cho khách. */
    val freeSeats: Int get() = (playerCount - 1 - peers.count { !it.gone }).coerceAtLeast(0)

    /** Chủ phòng (internet): tạo mã mời mới cho ghế trống tiếp theo. */
    fun newNetInvite() {
        if (!multi || !isHost || mode != Mode.HOST_NET || phase != Phase.ROOM || freeSeats == 0) return
        pendingNet?.let { if (it.state.value != LinkState.CONNECTED) it.close() }
        val t = WebRtcTransport(getApplication()); pendingNet = t
        inviteCode = ""; replyCode = ""; inviteBusy = true
        peerJobs += viewModelScope.launch {
            try { inviteCode = hostCode(t, t.createInvite()) { peerJobs += it } } catch (e: Exception) { notice = Notice.CONN_FAILED }
            inviteBusy = false
        }
        peerJobs += viewModelScope.launch {
            val st = t.state.first { it == LinkState.CONNECTED || it == LinkState.FAILED }
            if (pendingNet === t) pendingNet = null
            if (st == LinkState.CONNECTED) attachPeer(t) else notice = Notice.CONN_FAILED
            inviteCode = ""
        }
    }

    private fun attachPeer(t: Transport) {
        val inf = info ?: return
        if (phase != Phase.ROOM || freeSeats == 0) { t.close(); return }
        val pr = Peer(t); peers.add(pr)
        t.onMessage = { m -> viewModelScope.launch { onPeerMessage(pr, m) } }
        peerJobs += viewModelScope.launch {
            t.state.collect { st ->
                when (st) {
                    LinkState.CONNECTED -> t.send(Proto.hello(inf.id, Proto.clean(prefs.nickname), 0, playerCount))
                    LinkState.CLOSED, LinkState.FAILED -> onPeerDown(pr)
                    else -> {}
                }
            }
        }
    }

    private fun broadcast(msg: String, except: Peer? = null) { for (pr in peers) if (pr !== except && !pr.gone) pr.t.send(msg) }

    private fun broadcastRoster() {
        val names = listOf(Proto.clean(prefs.nickname)) + peers.filter { !it.gone && it.name.isNotEmpty() }.map { it.name }
        roster = names
        broadcast(Proto.roster(names))
    }

    private fun onPeerDown(pr: Peer) {
        if (pr.gone) return
        pr.gone = true
        when (phase) {
            Phase.ROOM -> { peers.remove(pr); broadcastRoster(); if (mode == Mode.HOST_NET && pendingNet == null && inviteOpen) newNetInvite() }
            Phase.PLAYING -> if (pr.seat >= 0) seatToBot(pr.seat)
            else -> {}
        }
    }

    private fun onPeerMessage(pr: Peer, msg: String) {
        if (pr.gone) return
        val p = msg.split("|")
        when (p[0]) {
            "J" -> if (phase == Phase.ROOM && p.size >= 2) { pr.name = Proto.clean(p[1]); broadcastRoster() }
            "M" -> {
                val g = game ?: return
                if (phase != Phase.PLAYING || p.size < 3 || g.turn != pr.seat || pr.seat in botSeats) return
                val idx = p[1].toIntOrNull() ?: return
                val mv = p[2].toIntOrNull() ?: return
                if (idx != g.plies || !g.play(mv)) return
                broadcast(Proto.move(idx, g.lastMove), except = pr)
                afterMove()
            }
            "R", "T" -> if (phase == Phase.PLAYING && pr.seat >= 0) seatToBot(pr.seat)
            "Q" -> { pr.t.close(); onPeerDown(pr) }
        }
    }

    /** Ghế [seat] từ giờ do máy chơi (người ngồi ghế đó đã rời, đầu hàng hoặc hết giờ). */
    private fun seatToBot(seat: Int) {
        val g = game ?: return
        if (seat in botSeats || g.result != Outcome.ONGOING) return
        botSeats.add(seat)
        broadcast(Proto.botSeat(seat))
        if (g.turn == seat) { resetTurnClock(); scheduleBotIfNeeded() }
    }

    /** Chủ phòng bắt đầu ván nhiều người: xếp ghế ngẫu nhiên, ghế trống do máy chơi. */
    private fun hostStartMulti() {
        val n = playerCount
        val live = peers.filter { !it.gone && it.name.isNotEmpty() }.take(n - 1)
        for (pr in peers) if (pr !in live) { pr.t.send(Proto.KICK); pr.gone = true; pr.t.close() }
        peers.retainAll(live.toSet())
        hub?.lock(); pendingNet?.close(); pendingNet = null; inviteCode = ""
        val order = (0 until n).shuffled()
        val names = Array(n) { "" }
        mySide = order[0]; names[mySide] = Proto.clean(prefs.nickname)
        live.forEachIndexed { i, pr -> pr.seat = order[i + 1]; names[pr.seat] = pr.name }
        botSeats.clear()
        val used = names.filter { it.isNotEmpty() }.toMutableSet()
        for (k in live.size + 1 until n) {
            val seat = order[k]; botSeats.add(seat)
            var nm = BotNames.pick(lang()); var tries = 0
            while (nm in used && tries++ < 20) nm = BotNames.pick(lang())
            used.add(nm); names[seat] = Proto.clean(nm)
        }
        seatNames = names.toList()
        for (pr in live) pr.t.send(Proto.startMulti(pr.seat, n, seatNames, pvpTime))
        beginMatch()
    }

    /** Bạn đầu hàng / hết giờ trong ván nhiều người: bạn thua, máy thay ghế của bạn và ván vẫn tiếp tục cho người khác. */
    private fun eliminateSelf(reason: EndReason, notify: String?) {
        if (eliminated || phase != Phase.PLAYING) return
        if (isHost) seatToBot(mySide) else if (notify != null) transport?.send(notify)
        eliminated = true; eliminatedReason = reason
        settle(MatchOutcome.LOSS, true)
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
            relayJob?.cancel(); dropRelayRooms()
            viewModelScope.launch {
                try { inviteCode = hostCode(t, t.createInvite()) { relayJob = it } } catch (e: Exception) { notice = Notice.CONN_FAILED }
                inviteBusy = false
            }
        }
        attachTransport()
    }

    /**
     * Chủ phòng: đưa offer lên trạm chuyển mã và trả về mã ngắn "PM2.<id>" để hiện QR; một vòng chờ sẽ tự nhận answer
     * của khách đầu tiên rồi kết nối. Trạm chưa cấu hình hoặc lỗi mạng: trả về chính offer (trao đổi mã hai chiều như cũ).
     */
    private suspend fun hostCode(t: WebRtcTransport, offer: String, keep: (Job) -> Unit): String {
        if (!Relay.enabled) return offer
        val id = Relay.newId()
        if (!Relay.publish(id, offer)) return offer
        relayRooms += id
        keep(viewModelScope.launch {
            while (t.state.value != LinkState.CLOSED && t.state.value != LinkState.FAILED) {
                delay(1500)
                val ans = Relay.pollAnswer(id) ?: continue
                if (!t.acceptReply(ans)) notice = Notice.BAD_CODE
                break
            }
            Relay.remove(id); relayRooms -= id
        })
        return Relay.code(id)
    }

    private fun dropRelayRooms() {
        if (relayRooms.isEmpty()) return
        val ids = relayRooms.toList(); relayRooms.clear()
        CoroutineScope(Dispatchers.IO).launch { for (id in ids) Relay.remove(id) }
    }

    /** Chủ phòng dán/quét mã trả lời của khách. */
    fun acceptReply(code: String) {
        val t = (if (multi) pendingNet else transport) as? WebRtcTransport ?: return
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
        if (multi) {
            if (phase != Phase.ROOM) return
            if (!wallet.canStart(inf.id)) { notice = Notice.NOT_ENOUGH; return }
            hostStartMulti(); return
        }
        if (phase != Phase.ROOM || opp == Opp.NONE) return
        if (!wallet.canStart(inf.id)) { notice = Notice.NOT_ENOUGH; return }
        hostSide = Random.nextInt(2); mySide = hostSide
        if (opp == Opp.HUMAN) transport?.send(Proto.start(hostSide, option, pvpTime))
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
        val id = Relay.parse(code)
        if (id != null) { joinViaRelay(t, id); return }
        inviteBusy = true
        viewModelScope.launch {
            val r = try { t.createReply(code) } catch (e: Exception) { null }
            if (r == null) notice = Notice.BAD_CODE else replyCode = r
            inviteBusy = false
        }
    }

    /** Khách quét mã PM2: lấy offer từ trạm, tạo answer và giành chỗ. Ai ghi answer trước thì vào phòng. */
    private fun joinViaRelay(t: WebRtcTransport, id: String) {
        if (inviteBusy || relayJoined) return
        inviteBusy = true
        viewModelScope.launch {
            val offer = Relay.fetchOffer(id)
            val reply = if (offer == null) null else try { t.createReply(offer) } catch (e: Exception) { null }
            if (transport !== t) { inviteBusy = false; return@launch }
            when {
                offer == null -> { notice = Notice.ROOM_TAKEN; resetJoinNet() }
                reply == null -> { notice = Notice.BAD_CODE; resetJoinNet() }
                else -> when (Relay.claim(id, reply)) {
                    Relay.Claim.OK -> relayJoined = true
                    Relay.Claim.TAKEN -> { notice = Notice.ROOM_TAKEN; resetJoinNet() }
                    Relay.Claim.ERROR -> notice = Notice.CONN_FAILED.also { resetJoinNet() }
                }
            }
            inviteBusy = false
        }
    }

    /** Khách (internet): bỏ kết nối dở, sẵn sàng quét lại mã khác. */
    private fun resetJoinNet() {
        if (mode != Mode.JOIN_NET) return
        linkJob?.cancel(); transport?.onMessage = null; transport?.close()
        transport = WebRtcTransport(getApplication()); relayJoined = false; replyCode = ""
        _link.value = LinkState.IDLE
        attachTransport()
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
            multi && !isHost && phase == Phase.PLAYING && game?.result == Outcome.ONGOING -> hostLeftMulti()
            phase == Phase.PLAYING && game?.result == Outcome.ONGOING -> {
                game?.forfeit(mySide, EndReason.LEAVE)
                wallet.finishMatch(inf.id, MatchOutcome.LOSS, true)
                notice = Notice.CONN_LOST; phase = Phase.OVER; verifying = false; tick++
            }
            phase == Phase.ROOM && isHost && opp == Opp.HUMAN -> {
                opp = Opp.NONE; oppName = ""; notice = Notice.OPP_LEFT; newHostTransport(); armBotTimer(5)
            }
            phase == Phase.ROOM && !isHost -> { notice = if (failed) Notice.CONN_FAILED else Notice.HOST_LEFT; guestConnected = false; resetJoinNet() }
            phase == Phase.ROOM && isHost && failed -> { notice = Notice.CONN_FAILED; if (mode == Mode.HOST_NET && !multi) newHostTransport() }
        }
    }

    private fun onRemote(msg: String) {
        val p = msg.split("|")
        when (p[0]) {
            "H" -> if (!isHost && phase == Phase.ROOM && p.size >= 4 && p[1] == info?.id) {
                oppName = p[2]; opp = Opp.HUMAN; guestConnected = true
                val n = p.getOrNull(4)?.toIntOrNull() ?: 2
                if (info?.optionKind == OptionKind.PLAYERS && info?.options?.contains(n) == true) option = n
                multi = n > 2
                if (multi) roster = listOf(p[2])
                transport?.send(Proto.join(prefs.nickname))
            }
            "J" -> if (isHost && phase == Phase.ROOM && p.size >= 2) {
                timerJob?.cancel(); opp = Opp.HUMAN; oppName = p[1].take(16)
            }
            "S" -> if (!isHost && phase == Phase.ROOM || phase == Phase.OVER && !isHost) {
                hostSide = p.getOrNull(1)?.toIntOrNull() ?: return
                p.getOrNull(2)?.toIntOrNull()?.let { o -> if (info?.options?.contains(o) == true) option = o }
                p.getOrNull(3)?.toIntOrNull()?.let { t -> pvpTime = t.coerceIn(0, Catalog.LEVELS - 1) }   // thời gian do chủ phòng chọn
                mySide = 1 - hostSide
                beginMatch()
            }
            "P" -> if (!isHost && p.size >= 2) roster = p[1].split(";")
            "S3" -> if (!isHost && (phase == Phase.ROOM || phase == Phase.OVER) && p.size >= 4) {
                val seat = p[1].toIntOrNull() ?: return
                val n = p[2].toIntOrNull() ?: return
                if (info?.options?.contains(n) != true || seat !in 0 until n) return
                option = n; multi = true; mySide = seat; seatNames = p[3].split(";")
                p.getOrNull(4)?.toIntOrNull()?.let { t -> pvpTime = t.coerceIn(0, Catalog.LEVELS - 1) }
                beginMatch()
            }
            "B" -> {}                                   // ghế khác chuyển sang máy: chỉ để thông tin
            "X" -> if (multi && !isHost) eliminateSelf(EndReason.TIMEOUT, null)
            "M" -> onRemoteMove(p)
            "R" -> if (!multi) remoteForfeit(EndReason.RESIGN)
            "T" -> if (!multi) remoteForfeit(EndReason.TIMEOUT)
            "Q" -> if (multi && !isHost && phase == Phase.PLAYING) hostLeftMulti()
                   else if (multi && !isHost && phase == Phase.ROOM) { notice = Notice.HOST_LEFT; guestConnected = false }
                   else if (phase == Phase.PLAYING) remoteForfeit(EndReason.LEAVE)
                   else if (phase == Phase.ROOM && isHost) { /* khách rời: onLinkDown xử lý */ }
                   else if (phase == Phase.OVER) { oppAskedRematch = false; opp = Opp.NONE }
            "K" -> if (!isHost) { notice = Notice.KICKED; leave() }
            "RM" -> { oppAskedRematch = true; maybeRematch() }
            "E" -> { remoteEnd = msg; tryFinalizeOnline() }
        }
    }

    /** Chủ phòng nhiều người rời đi giữa ván: ván dừng lại, không tính thắng/thua cho khách. */
    private fun hostLeftMulti() {
        if (finalized) { phase = Phase.OVER; return }
        botJob?.cancel(); turnClock?.cancel(); botThinking = false
        finalized = true; wallet.cancelMatch()
        notice = Notice.HOST_LEFT; phase = Phase.OVER; tick++
    }

    private fun onRemoteMove(p: List<String>) {
        val g = game ?: return
        if (phase != Phase.PLAYING || (g.turn == mySide && !eliminated) || p.size < 3) return
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
        unlockedNow = -1
        phase = Phase.PLAYING; tick++
        resetTurnClock()
        startTurnClock()
        scheduleBotIfNeeded()
        return true
    }

    // ───────────────────────── đồng hồ nước đi ─────────────────────────
    private fun moveMillis(): Long = moveSeconds * 1000L

    /** Bắt đầu tính giờ lại từ đầu cho lượt hiện tại (gọi sau mỗi nước đi / thao tác). */
    private fun resetTurnClock() {
        turnDeadline = System.currentTimeMillis() + moveMillis()
        turnSecondsLeft = ((turnDeadline - System.currentTimeMillis() + 999) / 1000).toInt()
    }

    /**
     * Vòng kiểm tra đồng hồ theo giờ hệ thống (vẫn đúng khi app vào nền rồi quay lại).
     * Hết giờ: người đang đến lượt bị xử thua. Máy không bao giờ bị tính giờ. Khi đấu online, máy của người hết giờ tự báo thua;
     * máy đối thủ chờ thêm [GRACE_MS] rồi tự xử thắng phòng khi tin nhắn bị mất.
     */
    private fun startTurnClock() {
        turnClock?.cancel()
        val my = epoch
        turnClock = viewModelScope.launch {
            while (my == epoch && phase == Phase.PLAYING) {
                delay(250)
                if (my != epoch || phase != Phase.PLAYING) break
                val left = turnDeadline - System.currentTimeMillis()
                turnSecondsLeft = ((left.coerceAtLeast(0) + 999) / 1000).toInt()
                if (left > 0) continue
                if (onTurnTimeout(left)) break
            }
        }
    }

    /** Xử lý hết giờ. Trả về true nếu ván đã kết thúc. */
    private fun onTurnTimeout(left: Long): Boolean {
        val sd = sudoku
        if (mode == Mode.SOLO) {
            if (sd == null || sd.status != SudokuStatus.PLAYING) return false
            sd.giveUp(); sudokuTick++
            phase = Phase.OVER
            settle(MatchOutcome.LOSS, true)
            return true
        }
        val g = game ?: return true
        if (g.result != Outcome.ONGOING) return true
        val loser = g.turn
        if (multi) {
            when {
                loser in botSeats -> resetTurnClock()
                loser == mySide && !eliminated -> eliminateSelf(EndReason.TIMEOUT, Proto.TIMEOUT)
                isHost && loser != mySide && left <= -GRACE_MS -> {
                    peers.firstOrNull { it.seat == loser && !it.gone }?.t?.send(Proto.ELIMINATED)
                    seatToBot(loser)
                }
            }
            return false                                        // ván nhiều người vẫn tiếp tục
        }
        when {
            mode == Mode.LOCAL -> g.forfeit(loser, EndReason.TIMEOUT)
            oppIsMachine && loser != mySide -> { resetTurnClock(); return false }   // lượt của máy: không tính giờ
            loser == mySide -> {
                if (isOnline && opp == Opp.HUMAN) transport?.send(Proto.TIMEOUT)
                g.forfeit(mySide, EndReason.TIMEOUT)
            }
            else -> {                                                             // đối thủ (người) hết giờ
                if (left > -GRACE_MS) return false
                g.forfeit(loser, EndReason.TIMEOUT)
            }
        }
        afterMove()
        return true
    }

    fun playMove(move: Int): Boolean {
        val g = game ?: return false
        if (!canAct) return false
        val index = g.plies
        if (!g.play(move)) return false
        if (multi && isHost) broadcast(Proto.move(index, g.lastMove))
        else if (isOnline && opp == Opp.HUMAN) transport?.send(Proto.move(index, g.lastMove))
        afterMove()
        return true
    }

    private fun afterMove() {
        val g = game ?: return
        tick++
        resetTurnClock()
        if (g.result != Outcome.ONGOING) onGameOver() else scheduleBotIfNeeded()
    }

    private fun scheduleBotIfNeeded() {
        val g = game ?: return; val inf = info ?: return
        val botTurn = if (multi) isHost && g.turn in botSeats else oppIsMachine && g.turn != mySide
        if (!botTurn || phase != Phase.PLAYING || g.result != Outcome.ONGOING) return
        val my = epoch
        botThinking = true
        botJob?.cancel()
        botJob = viewModelScope.launch {
            val snap = g.copyGame()
            val t0 = System.currentTimeMillis()
            val lv = matchLevel()
            val mv = withContext(Dispatchers.Default) { computeBotMove(snap, inf, lv) }
            val wait = BotNames.humanDelay() - (System.currentTimeMillis() - t0)
            if (wait > 0) delay(wait)
            if (my != epoch || game !== g || phase != Phase.PLAYING) return@launch
            botThinking = false
            val idx = g.plies
            if (mv >= 0 && g.play(mv)) { if (multi && isHost) broadcast(Proto.move(idx, g.lastMove)); afterMove() }
        }
    }

    fun resign() {
        val g = game ?: return
        if (phase != Phase.PLAYING || g.result != Outcome.ONGOING) return
        if (multi) { eliminateSelf(EndReason.RESIGN, Proto.RESIGN); return }
        if (isOnline && opp == Opp.HUMAN) transport?.send(Proto.RESIGN)
        g.forfeit(if (mode == Mode.LOCAL) g.turn else mySide, EndReason.RESIGN)
        afterMove()
    }

    /** Kết quả của ván hiện tại với người dùng (null nếu ván chưa xong). */
    fun myOutcome(): MatchOutcome? {
        if (eliminated) return MatchOutcome.LOSS
        val g = game ?: return null
        return if (g.result == Outcome.ONGOING) null else outcomeForMe(g)
    }

    private fun outcomeForMe(g: BoardGame): MatchOutcome {
        if (playerCount > 2) {
            val w = when (g) { is WallGoGame -> g.winner; is QuoridorGame -> g.winner; else -> -1 }
            if (w >= 0) return if (w == mySide) MatchOutcome.WIN else MatchOutcome.LOSS
            if (g.result == Outcome.DRAW) return MatchOutcome.DRAW
            if (g is WallGoGame && mySide < g.finalScores.size && g.finalScores[mySide] == g.finalScores.max()) return MatchOutcome.DRAW
            return MatchOutcome.LOSS
        }
        return outcome2(g)
    }

    private fun outcome2(g: BoardGame): MatchOutcome = when {
        g.result == Outcome.DRAW -> MatchOutcome.DRAW
        (g.result == Outcome.WIN_FIRST) == (mySide == 0) -> MatchOutcome.WIN
        else -> MatchOutcome.LOSS
    }

    private fun rewardValid(g: BoardGame, inf: GameInfo) =
        !((g.reason == EndReason.RESIGN || g.reason == EndReason.LEAVE || g.reason == EndReason.TIMEOUT) && g.plies < inf.minPlies)

    private fun onGameOver() {
        val g = game ?: return; val inf = info ?: return
        botJob?.cancel(); botThinking = false; turnClock?.cancel()
        phase = Phase.OVER
        if (mode == Mode.LOCAL) { finalized = true; return }       // chơi chung một máy: không thưởng, không thống kê
        if (multi) { settle(if (eliminated) MatchOutcome.LOSS else outcomeForMe(g), true); return }
        val out = outcomeForMe(g)
        // thắng máy ở cấp L thì mở khóa cấp L + 1 của game này
        if (mode == Mode.BOT && out == MatchOutcome.WIN && botLevel < Catalog.LEVELS - 1 && prefs.unlockLevel(inf.id, botLevel + 1)) unlockedNow = botLevel + 1
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
        if (multi) return                                       // phòng nhiều người: về phòng chờ để tạo ván mới
        if (!wallet.canStart(inf.id)) { notice = Notice.NOT_ENOUGH; return }
        if (isOnline && opp == Opp.HUMAN) { rematchAsked = true; transport?.send(Proto.REMATCH); maybeRematch() }
        else if (oppIsMachine) { mySide = 0; beginMatch() }
    }

    private fun maybeRematch() {
        if (phase != Phase.OVER || !rematchAsked || !oppAskedRematch) return
        if (isHost) { hostSide = 1 - hostSide; mySide = hostSide; transport?.send(Proto.start(hostSide, option, pvpTime)); beginMatch() }
    }

    companion object { private const val GRACE_MS = 10_000L }

    override fun onCleared() { teardown(); kata?.close(); kata = null; super.onCleared() }
}
