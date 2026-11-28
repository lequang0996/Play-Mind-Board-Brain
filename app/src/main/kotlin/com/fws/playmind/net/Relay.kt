package com.fws.playmind.net

import com.fws.playmind.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONTokener
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom

/**
 * Trạm chuyển mã kết nối cho chế độ chơi qua internet: Firebase Realtime Database, gọi qua REST (không cần SDK).
 * Chỉ chuyển mã offer / answer của WebRTC; ván đấu vẫn đi thẳng giữa hai máy.
 *
 * Chủ phòng ghi offer vào `rooms/<id>` rồi hiện QR ngắn "PM2.<id>". Khách quét QR, đọc offer, ghi answer vào
 * `rooms/<id>/a`. Luật database (docs/firebase-rules.json) chỉ cho ghi `a` khi nó chưa có, nên khách nào ghi trước thì
 * vào phòng, các khách sau nhận lỗi quyền và được báo phòng đã có người. Chủ phòng đọc answer, kết nối, rồi xóa phòng.
 *
 * Chưa cấu hình [BuildConfig.SIGNAL_DB_URL] thì [enabled] = false và app dùng cách trao đổi mã hai chiều (Signal, PM1).
 */
object Relay {
    private const val PREFIX = "PM2."
    private const val ID_LEN = 12
    private const val ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789"
    private val base = BuildConfig.SIGNAL_DB_URL.trim().trimEnd('/')
    private val rnd = SecureRandom()

    val enabled: Boolean get() = base.startsWith("https://")

    fun newId(): String = String(CharArray(ID_LEN) { ALPHABET[rnd.nextInt(ALPHABET.length)] })
    fun code(id: String) = PREFIX + id

    /** Mã phòng trong một mã PM2 (hoặc null nếu không phải). */
    fun parse(code: String): String? {
        val c = code.trim()
        if (!c.startsWith(PREFIX)) return null
        val id = c.substring(PREFIX.length)
        return if (id.length == ID_LEN && id.all { it in ALPHABET }) id else null
    }

    /** Kết quả khi khách giành chỗ. */
    enum class Claim { OK, TAKEN, ERROR }

    suspend fun publish(id: String, offer: String): Boolean {
        val body = JSONObject().put("o", offer).put("t", JSONObject().put(".sv", "timestamp")).toString()
        return http("PUT", "rooms/$id", body).first == 200
    }

    suspend fun fetchOffer(id: String): String? = readString("rooms/$id/o")

    suspend fun pollAnswer(id: String): String? = readString("rooms/$id/a")

    suspend fun claim(id: String, answer: String): Claim {
        val (status, _) = http("PUT", "rooms/$id/a", JSONObject.quote(answer))
        return when (status) {
            200 -> Claim.OK
            401, 403 -> Claim.TAKEN       // luật database từ chối: đã có người ghi trước
            else -> Claim.ERROR
        }
    }

    suspend fun remove(id: String) { http("DELETE", "rooms/$id", null) }

    private suspend fun readString(path: String): String? {
        val (status, body) = http("GET", path, null)
        if (status != 200 || body == null) return null
        return try { JSONTokener(body).nextValue() as? String } catch (_: Exception) { null }
    }

    /** Trả về (mã HTTP, nội dung); mã -1 nếu lỗi mạng. */
    private suspend fun http(method: String, path: String, body: String?): Pair<Int, String?> = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext -1 to null
        var conn: HttpURLConnection? = null
        try {
            conn = (URL("$base/$path.json").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 8000; readTimeout = 8000
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
            }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }
            code to text
        } catch (_: Exception) {
            -1 to null
        } finally {
            conn?.disconnect()
        }
    }
}
