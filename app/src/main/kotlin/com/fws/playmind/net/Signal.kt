package com.fws.playmind.net

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Mã phòng cho WebRTC: SDP được nén (deflate) rồi mã hóa base64-url, có tiền tố "PM1.o" (offer)
 * hoặc "PM1.a" (answer). Người dùng sao chép/dán hoặc quét QR để trao đổi, không cần máy chủ.
 */
object Signal {
    private const val PREFIX = "PM1."

    fun encode(kind: Char, sdp: String): String {
        val raw = sdp.toByteArray()
        val d = Deflater(Deflater.BEST_COMPRESSION, true)
        d.setInput(raw); d.finish()
        val out = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        while (!d.finished()) out.write(buf, 0, d.deflate(buf))
        d.end()
        return PREFIX + kind + Base64.encodeToString(out.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    /** Trả về SDP nếu mã hợp lệ và đúng loại, ngược lại null. */
    fun decode(kind: Char, code: String): String? = try {
        val c = code.trim().replace("\\s".toRegex(), "")
        if (!c.startsWith(PREFIX + kind)) null else {
            val bytes = Base64.decode(c.substring(PREFIX.length + 1), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val inf = Inflater(true)
            inf.setInput(bytes + byteArrayOf(0))
            val out = ByteArrayOutputStream()
            val buf = ByteArray(2048)
            while (!inf.finished()) {
                val n = inf.inflate(buf)
                if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break
                out.write(buf, 0, n)
                if (out.size() > 64 * 1024) break
            }
            inf.end()
            val s = out.toString("UTF-8")
            if (s.startsWith("v=0")) s else null
        }
    } catch (e: Exception) { null }

    fun qr(text: String, size: Int = 720): Bitmap? = try {
        val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.L, EncodeHintType.MARGIN to 1)
        val m = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val px = IntArray(size * size) { if (m[it % size, it / size]) Color.BLACK else Color.WHITE }
        Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
    } catch (e: Exception) { null }
}
