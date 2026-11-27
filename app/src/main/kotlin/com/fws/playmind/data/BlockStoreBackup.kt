package com.fws.playmind.data

import android.content.Context
import com.google.android.gms.auth.blockstore.Blockstore
import com.google.android.gms.auth.blockstore.RetrieveBytesRequest
import com.google.android.gms.auth.blockstore.StoreBytesData

/**
 * Bản sao ví lưu trong Google Block Store (Play services). Khác với file riêng của app, dữ liệu này
 * KHÔNG bị xóa khi gỡ app, nên cài lại vẫn khôi phục được ví và số lượt miễn phí đã dùng trong ngày.
 * Mọi lỗi (máy không có Play services, bị từ chối...) đều bị bỏ qua: đây chỉ là lớp bảo vệ bổ sung.
 */
class BlockStoreBackup(ctx: Context) {
    private val client = try { Blockstore.getClient(ctx.applicationContext) } catch (e: Throwable) { null }

    fun save(body: String) {
        val c = client ?: return
        try {
            val bytes = body.toByteArray(Charsets.UTF_8)
            if (bytes.size > 4000) return     // Block Store giới hạn 4 KB mỗi mục
            val data = StoreBytesData.Builder().setKey(KEY).setBytes(bytes).setShouldBackupToCloud(true).build()
            c.storeBytes(data)
        } catch (e: Throwable) { }
    }

    /** Gọi [cb] đúng một lần với nội dung đã lưu, hoặc null nếu không có / lỗi. */
    fun restore(cb: (String?) -> Unit) {
        val c = client ?: run { cb(null); return }
        try {
            val req = RetrieveBytesRequest.Builder().setKeys(listOf(KEY)).build()
            c.retrieveBytes(req)
                .addOnSuccessListener { r -> cb(r.blockstoreDataMap[KEY]?.bytes?.toString(Charsets.UTF_8)) }
                .addOnFailureListener { cb(null) }
        } catch (e: Throwable) { cb(null) }
    }

    companion object { private const val KEY = "pm_wallet_v2" }
}
