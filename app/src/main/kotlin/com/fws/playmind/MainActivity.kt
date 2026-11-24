package com.fws.playmind

import android.os.Bundle
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
        val app = application as PlayMindApp
        update = UpdateManager(this)
        app.ads.gatherConsent(this)   // UMP trước, chỉ yêu cầu quảng cáo khi được phép
        update.start()                // In-App Update (Flexible)
        setContent {
            PlayMindTheme { AppRoot(app, vm, update) }
        }
    }

    override fun onResume() { super.onResume(); update.onResume() }
    override fun onDestroy() { update.stop(); super.onDestroy() }
}
