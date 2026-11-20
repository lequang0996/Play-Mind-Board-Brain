package com.fws.playmind.match

import kotlin.random.Random

/** Tên hiển thị cho đối thủ do máy điều khiển trong phòng chờ (theo ngôn ngữ của app). */
object BotNames {
    private val pools = mapOf(
        "vi" to listOf("Minh Khoa", "Thu Hà", "Quang Huy", "Lan Anh", "Đức Thịnh", "Bảo Ngọc", "Hoàng Nam", "Tuấn Kiệt", "Mai Phương", "Gia Bảo"),
        "zh" to listOf("小明", "子轩", "雨桐", "浩然", "思琪", "志远", "梓涵", "俊杰", "晓雯", "天宇"),
        "ja" to listOf("Haruto", "Yuki", "Sora", "Aoi", "Ren", "Hina", "Kenji", "Mio", "Takumi", "Sakura"),
        "ko" to listOf("민준", "서연", "지호", "하윤", "도윤", "수빈", "예준", "지우", "현우", "유나"),
        "ru" to listOf("Дмитрий", "Анна", "Иван", "Мария", "Алексей", "Ольга", "Никита", "Елена", "Сергей", "Ксения"),
        "es" to listOf("Carlos", "Lucía", "Javier", "Sofía", "Diego", "Marta", "Pablo", "Elena", "Andrés", "Valeria"),
        "pt" to listOf("João", "Beatriz", "Pedro", "Larissa", "Rafael", "Camila", "Lucas", "Júlia", "Thiago", "Marina"),
        "en" to listOf("Alex", "Jordan", "Taylor", "Sam", "Chris", "Morgan", "Riley", "Jamie", "Casey", "Drew"),
    )

    fun pick(lang: String): String {
        val pool = pools[lang] ?: pools.getValue("en")
        val base = pool[Random.nextInt(pool.size)]
        return when (Random.nextInt(4)) {
            0 -> base + Random.nextInt(10, 99)
            1 -> base + "_" + Random.nextInt(80, 99)
            else -> base
        }
    }

    /** Thời gian "suy nghĩ" tối thiểu để nhịp đi của máy giống người (ms). */
    fun humanDelay(): Long = Random.nextLong(1100, 3200)
}
