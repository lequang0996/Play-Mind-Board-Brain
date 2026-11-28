package com.fws.playmind.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import com.fws.playmind.BuildConfig
import java.io.File
import java.security.MessageDigest

/**
 * Kiểm tra môi trường. Nếu thiết bị có dấu hiệu bị can thiệp, app VẪN chơi được nhưng tạm dừng việc nhận kim cương
 * thưởng (thắng ván, chuỗi thắng, điểm danh). Không crash, không xóa dữ liệu người chơi, không chặn mua hàng.
 *
 * Phát hiện: app gỡ lỗi được / đang bị gỡ lỗi; tiến trình bị theo dõi (TracerPid); thư viện hook trong bộ nhớ (Frida,
 * Xposed, LSPosed, Riru, Zygisk, Substrate); Xposed nạp trong tiến trình; root (su, Magisk, KernelSU); bản build test-keys;
 * app chạy trong app nhân bản / không gian ảo (Parallel Space, VirtualXposed...); app bị sửa rồi ký lại bằng khóa khác.
 */
object Integrity {
    private val suPaths = listOf(
        "/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/sbin/su", "/vendor/bin/su", "/su/bin/su",
        "/system/app/Superuser.apk", "/data/local/xbin/su", "/data/local/bin/su", "/data/local/su",
        "/data/adb/magisk", "/data/adb/ksu", "/data/adb/ksud", "/cache/.disable_magisk", "/dev/.magisk.unblock",
        "/system/bin/.ext/su", "/system/usr/we-need-root/su",
    )
    private val hookMarks = listOf("frida", "gum-js", "xposed", "lsposed", "edxp", "riru", "zygisk", "substrate", "sandhook")

    @Volatile private var cached: Boolean? = null

    /** Kết quả được nhớ lại trong một lần chạy (gọi nhiều lần không tốn). */
    fun rewardsAllowed(ctx: Context): Boolean {
        cached?.let { return it }
        val ok = check(ctx)
        cached = ok
        return ok
    }

    private fun check(ctx: Context): Boolean {
        if (BuildConfig.DEBUG) return true
        return try {
            val debuggable = (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            !debuggable && !Debug.isDebuggerConnected() && !Debug.waitingForDebugger() &&
                !tracerAttached() && !hookLibraryLoaded() && !xposedLoaded() && !hasSu() &&
                !Build.TAGS.orEmpty().contains("test-keys") && !clonedApp(ctx) && signatureOk(ctx)
        } catch (e: Throwable) { true }
    }

    private fun hasSu(): Boolean {
        if (suPaths.any { File(it).exists() }) return true
        // su trong PATH
        val path = System.getenv("PATH").orEmpty().split(':')
        return path.any { it.isNotEmpty() && File(it, "su").exists() }
    }

    private fun tracerAttached(): Boolean = try {
        File("/proc/self/status").readLines().firstOrNull { it.startsWith("TracerPid:") }
            ?.substringAfter(":")?.trim()?.toIntOrNull()?.let { it != 0 } ?: false
    } catch (e: Exception) { false }

    private fun hookLibraryLoaded(): Boolean = try {
        File("/proc/self/maps").useLines { lines -> lines.any { l -> hookMarks.any { m -> l.contains(m, true) } } }
    } catch (e: Exception) { false }

    private fun xposedLoaded(): Boolean {
        for (n in listOf("de.robv.android.xposed.XposedBridge", "de.robv.android.xposed.XC_MethodHook")) {
            try { Class.forName(n, false, ClassLoader.getSystemClassLoader()); return true } catch (_: Throwable) {}
        }
        return Throwable().stackTrace.any { it.className.contains("xposed", true) || it.className.contains("lsposed", true) }
    }

    /**
     * App nhân bản / không gian ảo chạy app trong thư mục dữ liệu của app chủ: đường dẫn dữ liệu không còn là
     * /data/user/<n>/<gói> hoặc /data/data/<gói>.
     */
    private fun clonedApp(ctx: Context): Boolean {
        val dir = ctx.filesDir?.absolutePath ?: return false
        val pkg = ctx.packageName
        val normal = Regex("^/data/(user(_de)?/\\d+|data)/" + Regex.escape(pkg) + "/")
        return !normal.containsMatchIn("$dir/")
    }

    /**
     * Chữ ký app phải là một trong các chứng chỉ hợp lệ ([BuildConfig.CERT_SHA256], cách nhau bằng dấu phẩy): khóa tải lên
     * và khóa ký của Google Play (Play App Signing). Danh sách trống thì bỏ qua bước này.
     */
    private fun signatureOk(ctx: Context): Boolean {
        val allowed = BuildConfig.CERT_SHA256.split(',').map { it.trim().replace(":", "").lowercase() }.filter { it.length == 64 }
        if (allowed.isEmpty()) return true
        return certDigests(ctx).any { it in allowed }
    }

    private fun certDigests(ctx: Context): List<String> {
        val pm = ctx.packageManager
        val sigs = if (Build.VERSION.SDK_INT >= 28) {
            val info = pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val si = info.signingInfo ?: return emptyList()
            if (si.hasMultipleSigners()) si.apkContentsSigners else si.signingCertificateHistory
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNATURES).signatures
        } ?: return emptyList()
        val md = MessageDigest.getInstance("SHA-256")
        return sigs.map { s -> md.digest(s.toByteArray()).joinToString("") { "%02x".format(it) } }
    }
}
