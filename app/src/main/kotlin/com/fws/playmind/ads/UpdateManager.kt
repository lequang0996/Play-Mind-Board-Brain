package com.fws.playmind.ads

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/** In-App Update kiểu Flexible: tải ngầm khi đang dùng app, xong thì hỏi người dùng khởi động lại để cài. */
class UpdateManager(private val activity: ComponentActivity) {
    var downloaded by mutableStateOf(false); private set
    private val mgr = AppUpdateManagerFactory.create(activity)
    private val launcher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }
    private val listener = InstallStateUpdatedListener { st -> if (st.installStatus() == InstallStatus.DOWNLOADED) downloaded = true }

    fun start() {
        mgr.registerListener(listener)
        mgr.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) downloaded = true
            else if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                try { mgr.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()) } catch (_: Exception) {}
            }
        }
    }

    /** Khi quay lại app: nếu bản cập nhật đã tải xong lúc ở nền thì nhắc cài. */
    fun onResume() {
        mgr.appUpdateInfo.addOnSuccessListener { info -> if (info.installStatus() == InstallStatus.DOWNLOADED) downloaded = true }
    }

    fun complete() { mgr.completeUpdate() }
    fun stop() { mgr.unregisterListener(listener) }
}
