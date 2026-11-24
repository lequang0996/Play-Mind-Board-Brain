package com.fws.playmind

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.fws.playmind.ads.AdsManager
import com.fws.playmind.data.Prefs
import com.fws.playmind.data.WalletRepo

class PlayMindApp : Application() {
    lateinit var prefs: Prefs; private set
    lateinit var wallet: WalletRepo; private set
    lateinit var ads: AdsManager; private set

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)   // app chỉ có giao diện sáng
        prefs = Prefs(this)
        wallet = WalletRepo(this)
        ads = AdsManager(this, prefs)
    }
}
