package com.fws.playmind

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.fws.playmind.catalog.Catalog
import com.fws.playmind.core.ChessGame
import com.fws.playmind.core.GomokuGame
import com.fws.playmind.core.XiangqiGame
import com.fws.playmind.match.SessionViewModel
import com.fws.playmind.ui.AppFrame
import com.fws.playmind.ui.GameScreen
import com.fws.playmind.ui.GuideScreen
import com.fws.playmind.ui.HomeScreen
import com.fws.playmind.ui.Langs
import com.fws.playmind.ui.LobbyScreen
import com.fws.playmind.ui.PlayMindTheme
import com.fws.playmind.ui.SettingsScreen

/**
 * Chỉ dành cho bản debug. Hiển thị một "cảnh" cố định bằng chính giao diện thật của app để CI chụp ảnh:
 *   adb shell am start -n com.fws.playmind.debug/com.fws.playmind.ScreenshotActivity --es scene chess --ez dark true --es lang vi
 * scene: home | lobby | chess | xiangqi | caro | settings | guide
 */
class ScreenshotActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as PlayMindApp
        val vm = ViewModelProvider(this)[SessionViewModel::class.java]
        val scene = intent.getStringExtra("scene") ?: "home"
        val dark = intent.getBooleanExtra("dark", false)
        intent.getStringExtra("lang")?.let { Langs.apply(it) }
        app.prefs.langChosen = true
        app.prefs.setTheme(if (dark) 2 else 1)
        app.prefs.setNickname("Alex")
        if (app.wallet.state.value.diamonds == 0) { app.wallet.addDiamonds(12); app.wallet.checkIn() }

        when (scene) {
            "lobby" -> vm.open(Catalog.byId("chess"))
            "chess" -> vm.loadDemo(Catalog.byId("chess"), demoChess(), 0, "Mira")
            "xiangqi" -> vm.loadDemo(Catalog.byId("xiangqi"), demoXiangqi(), 0, "Lan Anh")
            "caro" -> vm.loadDemo(Catalog.byId("caro"), demoCaro(), 0, "Kenji88")
        }
        setContent {
            PlayMindTheme(app.prefs.theme) {
                AppFrame {
                    when (scene) {
                        "lobby" -> LobbyScreen(app, vm, Catalog.byId("chess"), {}, {}, {})
                        "chess", "xiangqi", "caro" -> GameScreen(app, vm, vm.info!!) {}
                        "settings" -> SettingsScreen(app, {}, {})
                        "guide" -> GuideScreen({}, null)
                        else -> HomeScreen(app, false, {}, {}, {}, {})
                    }
                }
            }
        }
    }

    private fun sq(s: String) = (s[1] - '1') * 8 + (s[0] - 'a')

    private fun demoChess(): ChessGame {
        val g = ChessGame()
        listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "a7a6", "b5a4", "g8f6", "e1g1", "f8e7", "f1e1", "b7b5", "a4b3", "d7d6", "c2c3", "e8g8")
            .forEach { m -> val mv = g.findMove(sq(m.substring(0, 2)), sq(m.substring(2, 4))); if (mv >= 0) g.play(mv) }
        return g
    }

    private fun demoXiangqi(): XiangqiGame {
        val g = XiangqiGame()
        // (cột, hàng) -> ô = hàng*9 + cột
        fun c(f: Int, r: Int) = r * 9 + f
        listOf(c(7, 2) to c(4, 2), c(7, 9) to c(6, 7), c(7, 0) to c(6, 2), c(1, 9) to c(2, 7), c(0, 0) to c(0, 1), c(0, 9) to c(0, 8),
            c(1, 2) to c(1, 5), c(8, 6) to c(8, 5)).forEach { (a, b) -> val mv = g.findMove(a, b); if (mv >= 0) g.play(mv) }
        return g
    }

    private fun demoCaro(): GomokuGame {
        val g = GomokuGame()
        listOf(7 to 7, 8 to 8, 8 to 7, 6 to 7, 7 to 6, 7 to 8, 9 to 6, 6 to 5, 10 to 5, 5 to 8, 6 to 6, 9 to 7).forEach { (x, y) -> g.play(y * 15 + x) }
        return g
    }
}
