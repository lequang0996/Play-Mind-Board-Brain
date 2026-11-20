package com.fws.playmind.ads

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fws.playmind.BuildConfig
import com.fws.playmind.data.Prefs
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * AdMob + UMP. Quảng cáo chỉ được yêu cầu sau khi có sự đồng ý hợp lệ (người dùng EEA/UK/…).
 * Banner chỉ ở màn hình chính; quảng cáo xen kẽ chỉ hiện giữa các ván (tối đa 1 lần / 3 ván, cách nhau ≥ 4 phút);
 * quảng cáo có thưởng do người dùng chủ động bấm.
 */
class AdsManager(private val app: Application, private val prefs: Prefs) {
    var canRequestAds by mutableStateOf(false); private set
    private var started = false
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null

    fun gatherConsent(activity: Activity) {
        val ci = UserMessagingPlatform.getConsentInformation(activity)
        ci.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ -> if (ci.canRequestAds()) start() }
        }, { _ -> if (ci.canRequestAds()) start() })
        if (ci.canRequestAds()) start()
    }

    fun privacyOptionsRequired(activity: Activity) =
        UserMessagingPlatform.getConsentInformation(activity).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) { UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> } }

    private fun start() {
        if (started) return
        started = true
        MobileAds.initialize(app) {}
        canRequestAds = true
        loadInterstitial(); loadRewarded()
    }

    private fun loadInterstitial() {
        InterstitialAd.load(app, BuildConfig.ADMOB_INTERSTITIAL, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
            override fun onAdFailedToLoad(e: LoadAdError) { interstitial = null }
        })
    }

    private fun loadRewarded() {
        RewardedAd.load(app, BuildConfig.ADMOB_REWARDED, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewarded = ad }
            override fun onAdFailedToLoad(e: LoadAdError) { rewarded = null }
        })
    }

    /** Gọi khi người chơi rời màn kết quả. Chỉ hiện khi đủ điều kiện tần suất. */
    fun onMatchFinished(activity: Activity) {
        prefs.gamesSinceAd = prefs.gamesSinceAd + 1
        val ad = interstitial ?: return
        if (prefs.gamesSinceAd < 3 || System.currentTimeMillis() - prefs.lastAdAt < 4 * 60_000L) return
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { interstitial = null; loadInterstitial() }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { interstitial = null; loadInterstitial() }
        }
        prefs.gamesSinceAd = 0; prefs.lastAdAt = System.currentTimeMillis()
        ad.show(activity)
    }

    val rewardedReady: Boolean get() = rewarded != null

    fun showRewarded(activity: Activity, onReward: () -> Unit, onUnavailable: () -> Unit) {
        val ad = rewarded ?: run { loadRewarded(); onUnavailable(); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { rewarded = null; loadRewarded() }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { rewarded = null; loadRewarded(); onUnavailable() }
        }
        ad.show(activity) { onReward() }
    }
}
