package com.fws.playmind.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

/** Cài đặt không nhạy cảm (ngôn ngữ đã chọn, giao diện, biệt danh). Dữ liệu kim cương nằm ở [WalletRepo]. */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    var langChosen: Boolean
        get() = sp.getBoolean("lang_chosen", false)
        set(v) = sp.edit().putBoolean("lang_chosen", v).apply()

    /** 0 = theo hệ thống, 1 = sáng, 2 = tối */
    var theme by mutableIntStateOf(sp.getInt("theme", 0))
        private set

    fun setTheme(v: Int) { theme = v; sp.edit().putInt("theme", v).apply() }

    var nickname by mutableStateOf(sp.getString("nick", null) ?: ("Player" + Random.nextInt(1000, 9999)))
        private set

    fun setNickname(v: String) {
        val clean = v.filter { it != '|' && it != '\n' }.trim().take(16).ifEmpty { "Player" }
        nickname = clean; sp.edit().putString("nick", clean).apply()
    }

    var gamesSinceAd: Int
        get() = sp.getInt("games_since_ad", 0)
        set(v) = sp.edit().putInt("games_since_ad", v).apply()

    var lastAdAt: Long
        get() = sp.getLong("last_ad_at", 0L)
        set(v) = sp.edit().putLong("last_ad_at", v).apply()
}
