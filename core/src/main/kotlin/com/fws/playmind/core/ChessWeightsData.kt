package com.fws.playmind.core

/**
 * Trọng số đánh giá cờ vua đã tinh chỉnh (Texel tuning, tools/bot-bench). Chuỗi rỗng = dùng giá trị khởi điểm trong [CW.defaults].
 * Định dạng: các cặp "mg,eg" cách nhau bởi dấu ";" theo thứ tự chỉ số của [CW]. File này do ChessTune sinh ra, không sửa tay.
 */
internal object ChessWeightsData {
    private const val DATA = ""

    fun load(mg: IntArray, eg: IntArray): Boolean {
        if (DATA.isEmpty()) return false
        val parts = DATA.split(';')
        if (parts.size != mg.size) return false
        for (i in parts.indices) {
            val (a, b) = parts[i].split(',')
            mg[i] = a.toInt(); eg[i] = b.toInt()
        }
        return true
    }
}
