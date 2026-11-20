package com.fws.playmind.data

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Lưu dữ liệu ví đã mã hóa AES-256-GCM bằng khóa trong Android Keystore (khóa không xuất ra được).
 * GCM vừa mã hóa vừa xác thực: sửa 1 bit trong file là giải mã thất bại.
 */
class SecureStore(private val ctx: Context) {
    private val alias = "pm_wallet_key_v1"
    private val aad = "com.fws.playmind.wallet.v1".toByteArray()

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val b = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
        kg.init(b.build())
        return kg.generateKey()
    }

    private fun seal(plain: ByteArray): ByteArray {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key())
        c.updateAAD(aad)
        return c.iv + c.doFinal(plain)
    }

    private fun open(blob: ByteArray): ByteArray? = try {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, blob.copyOfRange(0, 12)))
        c.updateAAD(aad)
        c.doFinal(blob, 12, blob.size - 12)
    } catch (e: Exception) { null }

    private fun file(name: String) = AtomicFile(File(ctx.noBackupFilesDir, name))

    fun write(name: String, text: String) {
        val af = file(name)
        val out = af.startWrite()
        try { out.write(seal(text.toByteArray())); af.finishWrite(out) } catch (e: Exception) { af.failWrite(out) }
    }

    /** null nếu chưa có file; [CORRUPT] nếu file có nhưng giải mã/xác thực thất bại. */
    fun read(name: String): String? {
        val f = File(ctx.noBackupFilesDir, name)
        if (!f.exists()) return null
        val bytes = try { file(name).readFully() } catch (e: Exception) { return CORRUPT }
        return open(bytes)?.toString(Charsets.UTF_8) ?: CORRUPT
    }

    companion object { const val CORRUPT = "\u0000CORRUPT" }
}
