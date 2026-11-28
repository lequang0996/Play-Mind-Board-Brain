package com.fws.playmind.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import java.util.TimeZone

/** Trạng thái ví + thống kê. Bất biến; mọi thay đổi đi qua [WalletRepo]. */
data class WalletState(
    val diamonds: Int = 0,
    val streakDays: Int = 0,
    val lastCheckInDay: Long = -1,
    val freeUsed: Map<String, Pair<Long, Int>> = emptyMap(),   // gameId -> (ngày, số lượt miễn phí đã dùng trong ngày đó)
    val winStreak: Map<String, Int> = emptyMap(),    // gameId -> chuỗi thắng hiện tại
    val stats: Map<String, List<Int>> = emptyMap(),  // gameId -> [thắng, thua, hòa]
    val pending: String = "",                        // ván đang chơi dở (thoát app = thua)
    val lastWall: Long = 0, val lastElapsed: Long = 0,
    val counter: Long = 0,
    val plan: Plan = Plan.NONE, val planAt: Long = 0,  // gói đăng ký lần cuối xác nhận được với Google Play
)

/** Gói người dùng: NONE = miễn phí, MONTHLY = 1$/tháng, YEARLY = 10$/năm. */
enum class Plan(val freePerDay: Int, val noBanner: Boolean, val noInterstitial: Boolean) {
    NONE(1, false, false), MONTHLY(3, true, true), YEARLY(Int.MAX_VALUE, true, true)
}

enum class StartCost { FREE, PAID, INSUFFICIENT }
enum class MatchOutcome { WIN, LOSS, DRAW }

/** Phần thưởng của một ván: [diamonds] đã cộng (gồm thưởng chuỗi), [bonus] là phần thưởng chuỗi thắng. */
data class Reward(val diamonds: Int = 0, val bonus: Int = 0, val streak: Int = 0, val blocked: Boolean = false)

sealed interface CheckIn {
    data object Already : CheckIn
    data class Done(val reward: Int, val streak: Int) : CheckIn
}

/**
 * Luật kim cương:
 *  - Điểm danh mỗi ngày +1. Điểm danh liên tục 30 ngày thì mỗi lần tiếp theo +2 (gói tháng: +4). Gói năm không dùng kim cương.
 *  - Mỗi game 1 lượt chơi miễn phí/ngày (gói tháng: 3, gói năm: không giới hạn). Hết lượt: mỗi ván tốn 1 kim cương.
 *  - Thắng +1. Thắng liên tiếp 3 ván trong một game +2, 5 ván +3 (rồi tính lại từ 0).
 *  - Thua / hòa / thoát giữa ván / mất kết nối: chuỗi thắng về 0.
 * Không có mức trần kim cương.
 */
/** Chuỗi thắng: thắng liên tiếp [STREAK_1] ván +2 kim cương, [STREAK_2] ván +3 (rồi tính lại từ 0). */
const val STREAK_1 = 3
const val STREAK_2 = 5

class WalletRepo(private val ctx: Context) {
    private val store = SecureStore(ctx)
    private val backup = BlockStoreBackup(ctx)
    private val marks = ctx.getSharedPreferences("pm_marks", Context.MODE_PRIVATE)   // chỉ lưu số lượt miễn phí đã dùng; được Auto Backup sao lưu
    @Volatile var wasReset = false; private set   // phải khai báo trước _state (load() có thể đặt cờ này)
    @Volatile private var freshInstall = false      // phải khai báo trước _state (load() đặt cờ này)
    // ───── chống sửa bộ nhớ (GameGuardian...): bản sao số kim cương đã che bằng khóa ngẫu nhiên, kèm mã kiểm tra ─────
    private val mask = java.security.SecureRandom().nextInt()
    @Volatile private var shadow = 0
    @Volatile private var shadowCheck = 0
    @Volatile private var memTamper = false

    private val _state = MutableStateFlow(load())
    val state: StateFlow<WalletState> = _state
    private val envOk: Boolean = Integrity.rewardsAllowed(ctx)
    /** false nếu thiết bị bị can thiệp (Integrity) hoặc số kim cương trong bộ nhớ bị sửa: vẫn chơi được nhưng không nhận thưởng. */
    val rewardsAllowed: Boolean get() = envOk && !memTamper

    init { guard(_state.value.diamonds) }

    private fun guard(d: Int) { shadow = d xor mask; shadowCheck = Integer.rotateLeft(d, 11) xor mask.inv() }

    private fun guarded(): Int? { val d = shadow xor mask; return if ((Integer.rotateLeft(d, 11) xor mask.inv()) == shadowCheck) d else null }

    /** Trạng thái hiện tại, với số kim cương đối chiếu bản sao: nếu bị sửa trong bộ nhớ thì trả lại giá trị đúng. */
    private fun st(): WalletState {
        val cur = _state.value
        val g = guarded() ?: run { memTamper = true; return cur }
        if (cur.diamonds == g) return cur
        memTamper = true
        val fixed = cur.copy(diamonds = g)
        _state.value = fixed
        return fixed
    }

    private val _ready = MutableStateFlow(true)
    /** false trong lúc đang khôi phục ví từ Block Store sau khi cài lại (tối đa ~3 giây). UI nên chờ cờ này. */
    val ready: StateFlow<Boolean> = _ready

    init { if (freshInstall) startRestore() }

    /** Gói đang có hiệu lực. Nếu quá 8 ngày chưa xác nhận được với Google Play (offline lâu) thì coi như miễn phí. */
    fun currentPlan(): Plan {
        val st = st()
        return if (st.plan != Plan.NONE && trustedNow() - st.planAt < 8 * 86_400_000L) st.plan else Plan.NONE
    }

    /**
     * Gói năm (10$/năm): bỏ hẳn kim cương (không tốn, không bao giờ tăng, không điểm danh), ẩn mọi gói khác và mở khóa mọi cấp bot.
     * Số kim cương cũ được giữ nguyên và hoạt động lại khi gói năm bị hủy / hết hạn.
     */
    fun noDiamonds(): Boolean = currentPlan() == Plan.YEARLY

    /** BillingManager gọi khi biết gói hiện tại. */
    fun setPlan(plan: Plan) {
        val st = st()
        if (st.plan == plan && trustedNow() - st.planAt < 86_400_000L) return
        commit(st.copy(plan = plan, planAt = trustedNow()))
    }

    private fun startRestore() {
        _ready.value = false
        val h = Handler(Looper.getMainLooper())
        val done = AtomicBoolean(false)
        fun finish(body: String?) {
            if (!done.compareAndSet(false, true)) return
            h.removeCallbacksAndMessages(null)
            if (body != null) applyRestored(body)
            _ready.value = true
        }
        h.postDelayed({ finish(null) }, 3000)
        backup.restore { body -> h.post { finish(body) } }
    }

    /** Dán ví lấy từ Block Store vào máy vừa cài lại (chỉ khi chữ ký hợp lệ). */
    private fun applyRestored(raw: String) {
        val cut = raw.lastIndexOf("\n#sig=")
        if (cut < 0) return
        val body = raw.substring(0, cut)
        if (raw.substring(cut + 6) != sha(body)) return
        val r = decode(body) ?: return
        val cur = _state.value
        val fixed = r.copy(
            pending = "",
            winStreak = if (r.pending.isNotEmpty()) r.winStreak + (r.pending to 0) else r.winStreak,
            stats = if (r.pending.isNotEmpty()) bump(r.stats, r.pending, 1) else r.stats,
            freeUsed = mergeFree(r.freeUsed, cur.freeUsed),
        )
        commit(fixed)
    }

    private fun mergeFree(a: Map<String, Pair<Long, Int>>, b: Map<String, Pair<Long, Int>>): Map<String, Pair<Long, Int>> {
        val out = a.toMutableMap()
        for ((k, v) in b) {
            val o = out[k]
            if (o == null || v.first > o.first || (v.first == o.first && v.second > o.second)) out[k] = v
        }
        return out
    }

    private fun readMarks(): Map<String, Pair<Long, Int>> {
        val out = HashMap<String, Pair<Long, Int>>()
        for ((k, v) in marks.all) {
            val t = (v as? String)?.split(":") ?: continue
            val d = t.getOrNull(0)?.toLongOrNull() ?: continue
            out[k] = d to (t.getOrNull(1)?.toIntOrNull() ?: 1)
        }
        return out
    }

    private fun writeMarks(m: Map<String, Pair<Long, Int>>) {
        val e = marks.edit()
        for ((k, v) in m) e.putString(k, "${v.first}:${v.second}")
        e.apply()
    }

    // ───── đồng hồ đáng tin: giờ mạng (NetTime) nếu có; nếu không thì phát hiện chỉnh giờ trong cùng một lần khởi động ─────
    private fun trustedNow(): Long {
        NetTime.now()?.let { return it }
        val st = st()
        val wall = System.currentTimeMillis(); val el = SystemClock.elapsedRealtime()
        return if (st.lastElapsed in 1..el && st.lastWall > 0) {
            val expected = st.lastWall + (el - st.lastElapsed)
            if (kotlin.math.abs(wall - expected) > 10 * 60_000L) expected else wall
        } else maxOf(wall, st.lastWall)
    }

    fun today(): Long {
        val now = trustedNow()
        return Math.floorDiv(now + TimeZone.getDefault().getOffset(now), 86_400_000L)
    }

    private fun commit(s: WalletState) {
        val ns = s.copy(lastWall = trustedNow(), lastElapsed = SystemClock.elapsedRealtime(), counter = s.counter + 1)
        val body = encode(ns)
        store.write(FILE, body)
        store.write(SIDE, ns.counter.toString() + "|" + sha(body))
        writeMarks(ns.freeUsed)
        backup.save(body + "\n#sig=" + sha(body))
        guard(ns.diamonds)
        _state.value = ns
    }

    private fun load(): WalletState {
        val raw = store.read(FILE)
        val side = store.read(SIDE)
        val saved = readMarks()
        if (raw == null && side == null) {
            freshInstall = true
            return WalletState(freeUsed = saved)       // cài lại: giữ số lượt miễn phí đã dùng nếu Auto Backup khôi phục được
        }
        if (raw == null || raw == SecureStore.CORRUPT || side == SecureStore.CORRUPT) { wasReset = true; return WalletState(freeUsed = saved) }
        val s0 = decode(raw) ?: run { wasReset = true; return WalletState(freeUsed = saved) }
        // chống khôi phục file cũ: bộ đếm phụ không được lớn hơn bộ đếm của ví
        val parts = side?.split("|")
        val sc = parts?.getOrNull(0)?.toLongOrNull() ?: -1
        if (sc > s0.counter || (sc == s0.counter && parts?.getOrNull(1) != sha(raw))) { wasReset = true; return WalletState(freeUsed = saved) }
        val s = s0.copy(freeUsed = mergeFree(s0.freeUsed, saved))
        // ván dở dang từ lần trước = thua
        return if (s.pending.isNotEmpty()) {
            val g = s.pending
            s.copy(pending = "", winStreak = s.winStreak + (g to 0), stats = bump(s.stats, g, 1))
        } else s
    }

    // ───── điểm danh ─────
    fun canCheckIn(): Boolean = today() > _state.value.lastCheckInDay

    fun checkIn(): CheckIn {
        val st = st(); val day = today()
        // ngày điểm danh lần trước ở "tương lai" (đã chỉnh giờ tới trước) cũng không được điểm danh lại
        if (day <= st.lastCheckInDay || noDiamonds() || !rewardsAllowed) return CheckIn.Already
        val streak = if (st.lastCheckInDay == day - 1) st.streakDays + 1 else 1
        // Điểm danh +1; sau 30 ngày liên tục, mỗi lần tiếp theo +2 (gói tháng: +4; gói năm không điểm danh)
        val reward = if (streak > 30) (if (currentPlan() != Plan.NONE) 4 else 2) else 1
        commit(st.copy(diamonds = st.diamonds + reward, streakDays = streak, lastCheckInDay = day))
        return CheckIn.Done(reward, streak)
    }

    // ───── bắt đầu / kết thúc ván ─────
    /** Số lượt chơi miễn phí còn lại hôm nay của game (Int.MAX_VALUE = không giới hạn). */
    fun freeLeft(gameId: String): Int {
        val limit = currentPlan().freePerDay
        if (limit == Int.MAX_VALUE) return Int.MAX_VALUE
        val u = _state.value.freeUsed[gameId]
        val day = today()
        // lượt ghi ở một ngày "tương lai" (đã chỉnh giờ tới trước rồi chỉnh lại) coi như đã dùng hết hôm nay
        val used = when { u == null || u.first < day -> 0; u.first == day -> u.second; else -> limit }
        return (limit - used).coerceAtLeast(0)
    }

    fun freeAvailable(gameId: String) = freeLeft(gameId) > 0

    fun canStart(gameId: String): Boolean = freeAvailable(gameId) || st().diamonds >= 1

    fun startMatch(gameId: String): StartCost {
        val st = st()
        if (freeLeft(gameId) > 0) {
            val u = st.freeUsed[gameId]
            val used = if (u != null && u.first == today()) u.second else 0
            commit(st.copy(freeUsed = st.freeUsed + (gameId to (today() to used + 1)), pending = gameId)); return StartCost.FREE
        }
        return if (st.diamonds >= 1) {
            commit(st.copy(diamonds = st.diamonds - 1, pending = gameId)); StartCost.PAID
        } else StartCost.INSUFFICIENT
    }

    /**
     * [valid] = ván đủ điều kiện nhận thưởng (kết thúc tự nhiên hoặc đủ số nước tối thiểu,
     * và với ván online thì hai máy đã đối chiếu cùng kết quả).
     */
    fun finishMatch(gameId: String, outcome: MatchOutcome, valid: Boolean): Reward {
        val st = st()
        var d = st.diamonds
        var ws = st.winStreak[gameId] ?: 0
        var bonus = 0; var gained = 0; var blocked = false
        when (outcome) {
            MatchOutcome.WIN -> if (noDiamonds()) {
                if (valid) ws += 1                                   // gói năm: chỉ giữ thống kê, không có kim cương
            } else if (valid && rewardsAllowed) {
                ws += 1; gained = 1
                if (ws == STREAK_1) bonus = 2 else if (ws == STREAK_2) { bonus = 3; ws = 0 }
                gained += bonus
            } else if (!rewardsAllowed) blocked = true
            else -> ws = 0
        }
        val idx = when (outcome) { MatchOutcome.WIN -> 0; MatchOutcome.LOSS -> 1; MatchOutcome.DRAW -> 2 }
        commit(st.copy(diamonds = d + gained, winStreak = st.winStreak + (gameId to ws), stats = bump(st.stats, gameId, idx), pending = ""))
        return Reward(gained, bonus, if (ws == 0 && bonus == 3) STREAK_2 else ws, blocked)
    }

    /** Ván bị hủy không do lỗi người chơi (vd. đối thủ thoát sớm): trả lại trạng thái, không tính thắng/thua. */
    fun cancelMatch() { val st = _state.value; if (st.pending.isNotEmpty()) commit(st.copy(pending = "")) }

    fun addDiamonds(n: Int) { if (n > 0 && !noDiamonds()) { val st = st(); commit(st.copy(diamonds = st.diamonds + n)) } }

    private fun bump(m: Map<String, List<Int>>, g: String, i: Int): Map<String, List<Int>> {
        val cur = (m[g] ?: listOf(0, 0, 0)).toMutableList(); cur[i] = cur[i] + 1
        return m + (g to cur)
    }

    // ───── (de)serialize ─────
    private fun encode(s: WalletState): String = buildString {
        append("v=2\nd=${s.diamonds}\ns=${s.streakDays}\nc=${s.lastCheckInDay}\nw=${s.lastWall}\ne=${s.lastElapsed}\nn=${s.counter}\np=${s.pending}\nl=${s.plan.ordinal}\nla=${s.planAt}\n")
        s.freeUsed.forEach { (k, v) -> append("f.$k=${v.first}:${v.second}\n") }
        s.winStreak.forEach { (k, v) -> append("t.$k=$v\n") }
        s.stats.forEach { (k, v) -> append("st.$k=${v.joinToString(",")}\n") }
    }

    private fun decode(text: String): WalletState? = try {
        var s = WalletState()
        var free = mapOf<String, Pair<Long, Int>>(); var ws = mapOf<String, Int>(); var stats = mapOf<String, List<Int>>()
        for (line in text.lines()) {
            val i = line.indexOf('='); if (i < 0) continue
            val k = line.substring(0, i); val v = line.substring(i + 1)
            when {
                k == "d" -> s = s.copy(diamonds = v.toInt())
                k == "s" -> s = s.copy(streakDays = v.toInt())
                k == "c" -> s = s.copy(lastCheckInDay = v.toLong())
                k == "w" -> s = s.copy(lastWall = v.toLong())
                k == "e" -> s = s.copy(lastElapsed = v.toLong())
                k == "n" -> s = s.copy(counter = v.toLong())
                k == "p" -> s = s.copy(pending = v)
                k == "l" -> s = s.copy(plan = Plan.values().getOrElse(v.toInt()) { Plan.NONE })
                k == "la" -> s = s.copy(planAt = v.toLong())
                k.startsWith("f.") -> { val t = v.split(":"); free = free + (k.substring(2) to (t[0].toLong() to (t.getOrNull(1)?.toInt() ?: 1))) }
                k.startsWith("t.") -> ws = ws + (k.substring(2) to v.toInt())
                k.startsWith("st.") -> stats = stats + (k.substring(3) to v.split(",").map { it.toInt() })
            }
        }
        s.copy(freeUsed = free, winStreak = ws, stats = stats)
    } catch (e: Exception) { null }

    private fun sha(s: String): String = MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    companion object { private const val FILE = "wallet.dat"; private const val SIDE = "wallet.ctr" }
}
