package com.fws.playmind.data

import android.util.Base64
import com.android.billingclient.api.Purchase
import com.fws.playmind.BuildConfig
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * Kiểm tra giao dịch mua trước khi cộng kim cương / bật gói, chống công cụ giả lập thanh toán (Lucky Patcher, "freedom"...).
 *
 * Google Play ký nội dung giao dịch (originalJson) bằng khóa riêng của app; app kiểm tra bằng khóa công khai RSA
 * ([BuildConfig.PLAY_LICENSE_KEY], lấy ở Play Console: Kiếm tiền bằng Play > Thiết lập kiếm tiền > Cấp phép). Giao dịch giả
 * không có chữ ký hợp lệ. Thêm điều kiện: đúng tên gói của app. Chưa cấu hình khóa thì chỉ kiểm tra tên gói.
 */
object PurchaseCheck {
    private val key: PublicKey? by lazy {
        val b64 = BuildConfig.PLAY_LICENSE_KEY.trim()
        if (b64.isEmpty()) null else try {
            KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.decode(b64, Base64.DEFAULT)))
        } catch (_: Exception) { null }
    }

    fun valid(p: Purchase, packageName: String): Boolean {
        if (p.packageName != packageName) return false
        if (BuildConfig.PLAY_LICENSE_KEY.isBlank()) return true
        val k = key ?: return false
        if (p.signature.isEmpty()) return false
        return try {
            Signature.getInstance("SHA1withRSA").run {
                initVerify(k)
                update(p.originalJson.toByteArray(Charsets.UTF_8))
                verify(Base64.decode(p.signature, Base64.DEFAULT))
            }
        } catch (_: Exception) { false }
    }
}
