package com.fws.playmind.data

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest
import java.util.TimeZone

/** Trạng thái ví + thống kê. Bất biến; mọi thay đổi đi qua [WalletRepo]. */
data class WalletState(
    val diamonds: Int = 0,
    val streakDays: Int = 0,
    val lastCheckInDay: Long = -1,
    val freeDay: Map<String, Long> = emptyMap(),     // gameId -> ngày đã dùng lượt miễn phí
    val winStreak: Map<String, Int> = emptyMap(),    // gameId -> chuỗi thắng hiện tại
    val stats: Map<String, List<Int>> = emptyMap(),  // gameId -> [thắng, thua, hòa]
    val pending: String = "",                        // ván đang chơi dở (thoát app = thua)
    val lastWall: Long = 0, val lastElapsed: Long = 0,
    val counter: Long = 0,
)

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
 *  - Điểm danh mỗi ngày +1. Có > 10 kim cương và chuỗi điểm danh > 7 ngày (tính cả hôm nay) thì +2.
 *  - Mỗi game 1 lượt chơi miễn phí/ngày. Hết lượt: mỗi ván tốn 1 kim cương.
 *  - Thắng +1. Thắng liên tiếp 5 ván trong một game +2, 10 ván +3 (rồi tính lại từ 0).
 *  - Thua / hòa / thoát giữa ván / mất kết nối: chuỗi thắng về 0.
 * Không có mức trần kim cương.
 */
class WalletRepo(private val ctx: Context) {
    private val store = SecureStore(ctx)
    @Volatile var wasReset = false; private set   // phải khai báo trước _state (load() có thể đặt cờ này)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<WalletState> = _state
    val rewardsAllowed: Boolean = Integrity.rewardsAllowed(ctx)

    // ───── đồng hồ đáng tin: phát hiện chỉnh giờ hệ thống trong cùng một lần khởi động ─────
    private fun trustedNow(): Long {
        val st = _state.value
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
        _state.value = ns
    }

    private fun load(): WalletState {
        val raw = store.read(FILE)
        val side = store.read(SIDE)
        if (raw == null && side == null) return WalletState()
        if (raw == null || raw == SecureStore.CORRUPT || side == SecureStore.CORRUPT) { wasReset = true; return WalletState() }
        val s = decode(raw) ?: run { wasReset = true; return WalletState() }
        // chống khôi phục file cũ: bộ đếm phụ không được lớn hơn bộ đếm của ví
        val parts = side?.split("|")
        val sc = parts?.getOrNull(0)?.toLongOrNull() ?: -1
        if (sc > s.counter || (sc == s.counter && parts?.getOrNull(1) != sha(raw))) { wasReset = true; return WalletState() }
        // ván dở dang từ lần trước = thua
        return if (s.pending.isNotEmpty()) {
            val g = s.pending
            s.copy(pending = "", winStreak = s.winStreak + (g to 0), stats = bump(s.stats, g, 1))
        } else s
    }

    // ───── điểm danh ─────
    fun canCheckIn(): Boolean = _state.value.lastCheckInDay != today()

    fun checkIn(): CheckIn {
        val st = _state.value; val day = today()
        if (st.lastCheckInDay == day) return CheckIn.Already
        val streak = if (st.lastCheckInDay == day - 1) st.streakDays + 1 else 1
        val reward = if (st.diamonds > 10 && streak > 7) 2 else 1
        commit(st.copy(diamonds = st.diamonds + reward, streakDays = streak, lastCheckInDay = day))
        return CheckIn.Done(reward, streak)
    }

    // ───── bắt đầu / kết thúc ván ─────
    fun freeAvailable(gameId: String) = _state.value.freeDay[gameId] != today()

    fun canStart(gameId: String): Boolean = freeAvailable(gameId) || _state.value.diamonds >= 1

    fun startMatch(gameId: String): StartCost {
        val st = _state.value
        return if (st.freeDay[gameId] != today()) {
            commit(st.copy(freeDay = st.freeDay + (gameId to today()), pending = gameId)); StartCost.FREE
        } else if (st.diamonds >= 1) {
            commit(st.copy(diamonds = st.diamonds - 1, pending = gameId)); StartCost.PAID
        } else StartCost.INSUFFICIENT
    }

    /**
     * [valid] = ván đủ điều kiện nhận thưởng (kết thúc tự nhiên hoặc đủ số nước tối thiểu,
     * và với ván online thì hai máy đã đối chiếu cùng kết quả).
     */
    fun finishMatch(gameId: String, outcome: MatchOutcome, valid: Boolean): Reward {
        val st = _state.value
        var d = st.diamonds
        var ws = st.winStreak[gameId] ?: 0
        var bonus = 0; var gained = 0; var blocked = false
        when (outcome) {
            MatchOutcome.WIN -> if (valid && rewardsAllowed) {
                ws += 1; gained = 1
                if (ws == 5) bonus = 2 else if (ws == 10) { bonus = 3; ws = 0 }
                gained += bonus
            } else if (!rewardsAllowed) blocked = true
            else -> ws = 0
        }
        val idx = when (outcome) { MatchOutcome.WIN -> 0; MatchOutcome.LOSS -> 1; MatchOutcome.DRAW -> 2 }
        commit(st.copy(diamonds = d + gained, winStreak = st.winStreak + (gameId to ws), stats = bump(st.stats, gameId, idx), pending = ""))
        return Reward(gained, bonus, if (ws == 0 && bonus == 3) 10 else ws, blocked)
    }

    /** Ván bị hủy không do lỗi người chơi (vd. đối thủ thoát sớm): trả lại trạng thái, không tính thắng/thua. */
    fun cancelMatch() { val st = _state.value; if (st.pending.isNotEmpty()) commit(st.copy(pending = "")) }

    fun addDiamonds(n: Int) { if (n > 0) commit(_state.value.copy(diamonds = _state.value.diamonds + n)) }

    private fun bump(m: Map<String, List<Int>>, g: String, i: Int): Map<String, List<Int>> {
        val cur = (m[g] ?: listOf(0, 0, 0)).toMutableList(); cur[i] = cur[i] + 1
        return m + (g to cur)
    }

    // ───── (de)serialize ─────
    private fun encode(s: WalletState): String = buildString {
        append("v=1\nd=${s.diamonds}\ns=${s.streakDays}\nc=${s.lastCheckInDay}\nw=${s.lastWall}\ne=${s.lastElapsed}\nn=${s.counter}\np=${s.pending}\n")
        s.freeDay.forEach { (k, v) -> append("f.$k=$v\n") }
        s.winStreak.forEach { (k, v) -> append("t.$k=$v\n") }
        s.stats.forEach { (k, v) -> append("st.$k=${v.joinToString(",")}\n") }
    }

    private fun decode(text: String): WalletState? = try {
        var s = WalletState()
        var free = mapOf<String, Long>(); var ws = mapOf<String, Int>(); var stats = mapOf<String, List<Int>>()
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
                k.startsWith("f.") -> free = free + (k.substring(2) to v.toLong())
                k.startsWith("t.") -> ws = ws + (k.substring(2) to v.toInt())
                k.startsWith("st.") -> stats = stats + (k.substring(3) to v.split(",").map { it.toInt() })
            }
        }
        s.copy(freeDay = free, winStreak = ws, stats = stats)
    } catch (e: Exception) { null }

    private fun sha(s: String): String = MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    companion object { private const val FILE = "wallet.dat"; private const val SIDE = "wallet.ctr" }
}
