package com.fws.playmind.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/** 8 ngôn ngữ được hỗ trợ: (mã BCP-47, tên gọi bằng chính ngôn ngữ đó). */
object Langs {
    val all = listOf(
        "en" to "English", "zh-CN" to "简体中文", "es" to "Español", "pt" to "Português",
        "ru" to "Русский", "ja" to "日本語", "ko" to "한국어", "vi" to "Tiếng Việt",
    )

    /** Biểu tượng (cờ) đại diện cho từng ngôn ngữ. Đổi ở đây nếu muốn dùng cờ khác (vd. en -> 🇺🇸, pt -> 🇵🇹). */
    fun flag(tag: String): String = when (tag) {
        "en" -> "\uD83C\uDDEC\uD83C\uDDE7"; "zh-CN" -> "\uD83C\uDDE8\uD83C\uDDF3"; "es" -> "\uD83C\uDDEA\uD83C\uDDF8"
        "pt" -> "\uD83C\uDDE7\uD83C\uDDF7"; "ru" -> "\uD83C\uDDF7\uD83C\uDDFA"; "ja" -> "\uD83C\uDDEF\uD83C\uDDF5"
        "ko" -> "\uD83C\uDDF0\uD83C\uDDF7"; else -> "\uD83C\uDDFB\uD83C\uDDF3"
    }

    /** Tên ngôn ngữ viết bằng ngôn ngữ giao diện hiện tại (vd. giao diện tiếng Việt: English -> "Tiếng Anh"). */
    fun localizedName(tag: String, ui: Locale): String =
        Locale.forLanguageTag(tag).getDisplayLanguage(ui).replaceFirstChar { it.titlecase(ui) }

    fun apply(tag: String) = AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))

    /** Ngôn ngữ đang dùng (mã trong [all]); nếu chưa chọn thì suy ra từ ngôn ngữ hệ thống. */
    fun current(): String {
        val l = AppCompatDelegate.getApplicationLocales()
        val loc = if (l.isEmpty()) Locale.getDefault() else l[0]!!
        return all.firstOrNull { it.first.equals(loc.toLanguageTag(), true) }?.first
            ?: all.firstOrNull { it.first.substringBefore('-') == loc.language }?.first ?: "en"
    }
}
