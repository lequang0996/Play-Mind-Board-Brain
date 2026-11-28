package com.fws.playmind.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

/** Cài đặt không nhạy cảm (ngôn ngữ đã chọn, biệt danh). Dữ liệu kim cương nằm ở [WalletRepo]. */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    var langChosen: Boolean
        get() = sp.getBoolean("lang_chosen", false)
        set(v) = sp.edit().putBoolean("lang_chosen", v).apply()

    var nickname by mutableStateOf(sp.getString("nick", null) ?: ("Player" + Random.nextInt(1000, 9999)))
        private set

    fun updateNickname(v: String) {
        val clean = v.filter { it != '|' && it != '\n' }.trim().take(16).ifEmpty { "Player" }
        nickname = clean; sp.edit().putString("nick", clean).apply()
    }

    // ── Cấp bot (Dễ = 0 ... Siêu việt = 6): mặc định chỉ mở Dễ; thắng cấp L thì mở cấp L + 1 của game đó ──
    private val unlocked = mutableStateMapOf<String, Int>()
    private val selected = mutableStateMapOf<String, Int>()

    /** Gói năm mở khóa mọi cấp (PlayMindApp nối hàm này với WalletRepo.noDiamonds). */
    var allLevelsUnlocked: () -> Boolean = { false }

    /** Cấp cao nhất đã mở khóa của game [gameId]. */
    fun unlockedLevel(gameId: String): Int =
        if (allLevelsUnlocked()) 6 else (unlocked[gameId] ?: sp.getInt("lvl_$gameId", 0)).coerceIn(0, 6)

    /** Thời gian mỗi nước khi chơi với người: chỉ số trong Catalog.LEVEL_MOVE_SECONDS (mặc định 5 phút). */
    var pvpTime: Int
        get() = sp.getInt("pvp_time", 3).coerceIn(0, 6)
        set(v) = sp.edit().putInt("pvp_time", v.coerceIn(0, 6)).apply()

    /** Mở khóa tới cấp [level] (không bao giờ hạ). Trả về true nếu vừa mở thêm. */
    fun unlockLevel(gameId: String, level: Int): Boolean {
        val l = level.coerceIn(0, 6)
        if (l <= unlockedLevel(gameId)) return false
        unlocked[gameId] = l; sp.edit().putInt("lvl_$gameId", l).apply()
        return true
    }

    /** Cấp đang chọn của game (luôn trong phạm vi đã mở khóa). */
    fun selectedLevel(gameId: String): Int =
        (selected[gameId] ?: sp.getInt("sel_$gameId", 0)).coerceIn(0, unlockedLevel(gameId))

    fun selectLevel(gameId: String, level: Int) {
        val l = level.coerceIn(0, unlockedLevel(gameId))
        selected[gameId] = l; sp.edit().putInt("sel_$gameId", l).apply()
    }

    // ── Ma phương 7x7: mỗi ngày một đề; lưu ván đang giải dở để thoát ra vào lại vẫn giữ nguyên ──
    /** Ngày của đề đã lưu (WalletRepo.today()), -1 nếu chưa có. */
    val magicDay: Long get() = sp.getLong("mg_day", -1L)
    /** Ván đã lưu (MagicSquareGame.encode()). */
    val magicData: String? get() = sp.getString("mg_data", null)
    val magicSeconds: Int get() = sp.getInt("mg_sec", 0)
    val magicSolved: Boolean get() = sp.getBoolean("mg_done", false)
    /** Đã trả lượt / kim cương cho đề của ngày [magicDay]. */
    val magicPaid: Boolean get() = sp.getBoolean("mg_paid", false)
    /** Game yêu thích (mã game), dùng được khi có gói tháng hoặc gói năm. */
    var favorites: Set<String>
        get() = sp.getString("favs", "")!!.split(',').filter { it.isNotBlank() }.toSet()
        set(v) = sp.edit().putString("favs", v.joinToString(",")).apply()
    fun toggleFavorite(id: String): Boolean { val f = favorites; favorites = if (id in f) f - id else f + id; return id !in f }

    /** Đã xem hướng dẫn nhanh của Khối gỗ ghép chữ (tự mở ở lượt đầu tiên). */
    var wordsIntroSeen: Boolean
        get() = sp.getBoolean("wb_intro", false)
        set(v) = sp.edit().putBoolean("wb_intro", v).apply()

    fun magicSave(day: Long, data: String, seconds: Int, solved: Boolean, paid: Boolean) {
        sp.edit().putLong("mg_day", day).putString("mg_data", data).putInt("mg_sec", seconds)
            .putBoolean("mg_done", solved).putBoolean("mg_paid", paid).apply()
    }

    var gamesSinceAd: Int
        get() = sp.getInt("games_since_ad", 0)
        set(v) = sp.edit().putInt("games_since_ad", v).apply()

    var lastAdAt: Long
        get() = sp.getLong("last_ad_at", 0L)
        set(v) = sp.edit().putLong("last_ad_at", v).apply()
}
