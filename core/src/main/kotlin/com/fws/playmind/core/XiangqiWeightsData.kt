package com.fws.playmind.core

/**
 * Trọng số đánh giá cờ tướng đã tinh chỉnh (Texel tuning). Chuỗi rỗng = dùng giá trị khởi điểm trong [XW.defaults].
 * Định dạng: các cặp "mg,eg" cách nhau bởi dấu ";" theo thứ tự chỉ số của [XW]. Do công cụ tinh chỉnh sinh ra, không sửa tay.
 */
internal object XiangqiWeightsData {
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
