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

    fun apply(tag: String) = AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))

    /** Ngôn ngữ đang dùng (mã trong [all]); nếu chưa chọn thì suy ra từ ngôn ngữ hệ thống. */
    fun current(): String {
        val l = AppCompatDelegate.getApplicationLocales()
        val loc = if (l.isEmpty()) Locale.getDefault() else l[0]!!
        return all.firstOrNull { it.first.equals(loc.toLanguageTag(), true) }?.first
            ?: all.firstOrNull { it.first.substringBefore('-') == loc.language }?.first ?: "en"
    }
}
