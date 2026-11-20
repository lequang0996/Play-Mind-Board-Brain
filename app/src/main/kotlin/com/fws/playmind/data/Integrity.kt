package com.fws.playmind.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Debug
import com.fws.playmind.BuildConfig
import java.io.File

/**
 * Kiểm tra môi trường nhẹ nhàng. Nếu thiết bị có dấu hiệu bị can thiệp, app VẪN chơi được
 * nhưng tạm dừng việc nhận kim cương thưởng. Không crash, không xóa dữ liệu người chơi.
 */
object Integrity {
    private val suPaths = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/app/Superuser.apk", "/data/local/xbin/su", "/data/local/bin/su")

    fun rewardsAllowed(ctx: Context): Boolean {
        if (BuildConfig.DEBUG) return true
        return try {
            val debuggable = (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            !debuggable && !Debug.isDebuggerConnected() && !Debug.waitingForDebugger() &&
                !tracerAttached() && !hookLibraryLoaded() && !hasSu() && !Build.TAGS.orEmpty().contains("test-keys")
        } catch (e: Throwable) { true }
    }

    private fun hasSu() = suPaths.any { File(it).exists() }

    private fun tracerAttached(): Boolean = try {
        File("/proc/self/status").readLines().firstOrNull { it.startsWith("TracerPid:") }
            ?.substringAfter(":")?.trim()?.toIntOrNull()?.let { it != 0 } ?: false
    } catch (e: Exception) { false }

    private fun hookLibraryLoaded(): Boolean = try {
        File("/proc/self/maps").useLines { lines -> lines.any { l -> l.contains("frida", true) || l.contains("xposed", true) } }
    } catch (e: Exception) { false }
}
