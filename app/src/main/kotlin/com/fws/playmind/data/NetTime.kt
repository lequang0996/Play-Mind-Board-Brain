package com.fws.playmind.data

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

/**
 * Giờ chuẩn từ mạng, chống chỉnh đồng hồ máy để điểm danh hoặc nhận lượt miễn phí sớm.
 *
 * Đọc header "Date" của một yêu cầu rỗng tới máy chủ Google (không gửi dữ liệu người dùng), rồi neo vào
 * SystemClock.elapsedRealtime(): người dùng đổi giờ hệ thống sau đó cũng không ảnh hưởng. Chưa lấy được (offline) thì
 * [now] trả về null và ví dùng đồng hồ máy có kiểm tra nhảy giờ như cũ.
 */
object NetTime {
    private val urls = listOf("https://www.google.com/generate_204", "https://www.gstatic.com/generate_204")
    @Volatile private var anchorWall = 0L
    @Volatile private var anchorElapsed = 0L
    @Volatile private var busy = false

    /** Giờ chuẩn hiện tại (ms) hoặc null nếu chưa đồng bộ được trong lần chạy này. */
    fun now(): Long? = if (anchorWall > 0) anchorWall + (SystemClock.elapsedRealtime() - anchorElapsed) else null

    /** Đồng bộ ở nền (gọi khi mở app / quay lại app). Đã có neo trong 6 giờ qua thì bỏ qua. */
    fun sync() {
        if (busy) return
        if (anchorWall > 0 && SystemClock.elapsedRealtime() - anchorElapsed < 6 * 3_600_000L) return
        busy = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (u in urls) {
                    val t = fetch(u) ?: continue
                    anchorWall = t.first; anchorElapsed = t.second
                    break
                }
            } finally { busy = false }
        }
    }

    private fun fetch(u: String): Pair<Long, Long>? {
        var c: HttpURLConnection? = null
        return try {
            val t0 = SystemClock.elapsedRealtime()
            c = (URL(u).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"; connectTimeout = 5000; readTimeout = 5000; useCaches = false
            }
            c.responseCode
            val date = c.getHeaderFieldDate("Date", 0L)
            val t1 = SystemClock.elapsedRealtime()
            // header Date chỉ chính xác tới giây: lấy mốc giữa lúc gửi và lúc nhận
            if (date > 1_600_000_000_000L) date to (t0 + t1) / 2 else null
        } catch (_: Exception) {
            null
        } finally {
            c?.disconnect()
        }
    }
}
