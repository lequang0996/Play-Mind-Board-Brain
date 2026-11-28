package com.fws.playmind

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.fws.playmind.ads.AdsManager
import com.fws.playmind.audio.AudioEngine
import com.fws.playmind.data.BillingManager
import com.fws.playmind.data.Prefs
import com.fws.playmind.data.WalletRepo

class PlayMindApp : Application() {
    lateinit var prefs: Prefs; private set
    lateinit var wallet: WalletRepo; private set
    lateinit var ads: AdsManager; private set
    lateinit var billing: BillingManager; private set
    lateinit var audio: AudioEngine; private set

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)  // app chỉ có giao diện tối
        prefs = Prefs(this)
        audio = AudioEngine(this)
        wallet = WalletRepo(this)
        prefs.allLevelsUnlocked = { wallet.noDiamonds() }
        ads = AdsManager(this, prefs) { wallet.currentPlan() }
        billing = BillingManager(this, wallet)
        billing.start()
    }
}
