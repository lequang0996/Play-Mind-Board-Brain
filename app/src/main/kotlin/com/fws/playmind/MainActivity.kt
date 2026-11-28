package com.fws.playmind

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import com.fws.playmind.ads.UpdateManager
import com.fws.playmind.match.SessionViewModel
import com.fws.playmind.ui.AppRoot
import com.fws.playmind.ui.PlayMindTheme

class MainActivity : AppCompatActivity() {
    private val vm: SessionViewModel by viewModels()
    private lateinit var update: UpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Chỉ có giao diện sáng: thanh trạng thái/điều hướng trong suốt, biểu tượng hệ thống màu tối
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        // Màn hình luôn sáng khi đang mở app (tự tắt khi app vào nền)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val app = application as PlayMindApp
        update = UpdateManager(this)
        app.ads.gatherConsent(this)   // UMP trước, chỉ yêu cầu quảng cáo khi được phép
        update.start()                // In-App Update (Flexible)
        setContent {
            PlayMindTheme { AppRoot(app, vm, update) }
        }
    }

    override fun onStart() { super.onStart(); (application as PlayMindApp).audio.onForeground(true); (application as PlayMindApp).ads.showAppOpenIfReady(this) }
    override fun onStop() { (application as PlayMindApp).audio.onForeground(false); super.onStop() }
    override fun onResume() { super.onResume(); (application as PlayMindApp).audio.onForeground(true); com.fws.playmind.data.NetTime.sync(); update.onResume(); (application as PlayMindApp).billing.refresh() }
    override fun onDestroy() { update.stop(); super.onDestroy() }
}
