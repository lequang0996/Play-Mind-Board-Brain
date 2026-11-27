package com.fws.playmind.ads

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fws.playmind.BuildConfig
import com.fws.playmind.data.Plan
import com.fws.playmind.data.Prefs
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * AdMob + UMP. Quảng cáo chỉ được yêu cầu sau khi có sự đồng ý hợp lệ (người dùng EEA/UK/…).
 * App CHỈ dùng 2 loại quảng cáo: banner (màn hình chính, [BuildConfig.ADMOB_BANNER]) và quảng cáo mở app
 * ([BuildConfig.ADMOB_APP_OPEN]). Không có quảng cáo xen kẽ hay quảng cáo có thưởng.
 */
class AdsManager(private val app: Application, private val prefs: Prefs, private val plan: () -> Plan = { Plan.NONE }) {
    var canRequestAds by mutableStateOf(false); private set
    private var started = false
    private var appOpen: AppOpenAd? = null
    private var appOpenLoadedAt = 0L
    private var appOpenLoading = false
    private var lastAppOpenAt = 0L
    private var fullScreenShowing = false
    /** Activity đang chờ hiện quảng cáo mở app lần đầu (khi quảng cáo tải xong sau lúc app đã mở). */
    private var pendingActivity: java.lang.ref.WeakReference<Activity>? = null
    private var pendingSince = 0L

    /** Banner chỉ hiện khi đã được phép yêu cầu quảng cáo VÀ gói hiện tại không bỏ banner. */
    fun bannerAllowed(): Boolean = canRequestAds && !plan().noBanner

    fun gatherConsent(activity: Activity) {
        pendingActivity = java.lang.ref.WeakReference(activity); pendingSince = System.currentTimeMillis()
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
        loadAppOpen()
    }

    private fun loadAppOpen() {
        if (plan().noBanner || appOpenLoading) return       // gói tháng/năm đều bỏ quảng cáo mở app
        appOpenLoading = true
        AppOpenAd.load(app, BuildConfig.ADMOB_APP_OPEN, AdRequest.Builder().build(), object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                appOpenLoading = false
                appOpen = ad; appOpenLoadedAt = System.currentTimeMillis()
                // lần mở app đầu tiên: quảng cáo thường tải xong sau onStart -> hiện nếu vẫn đang trong lúc khởi động (<= 5 giây)
                val a = pendingActivity?.get(); pendingActivity = null
                if (a != null && !a.isFinishing && System.currentTimeMillis() - pendingSince <= 5_000L) showAppOpenIfReady(a)
            }
            override fun onAdFailedToLoad(e: LoadAdError) { appOpenLoading = false; appOpen = null; pendingActivity = null }
        })
    }

    /** Gọi khi app quay lại màn hình. Chỉ hiện nếu: chưa trả phí, quảng cáo còn mới (< 4 giờ), cách lần trước >= 3 phút. */
    fun showAppOpenIfReady(activity: Activity) {
        if (!canRequestAds || plan().noBanner || fullScreenShowing) return
        val ad = appOpen ?: run { loadAppOpen(); return }
        val now = System.currentTimeMillis()
        if (now - appOpenLoadedAt > 4 * 3_600_000L) { appOpen = null; loadAppOpen(); return }
        if (now - lastAppOpenAt < 3 * 60_000L) return
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { fullScreenShowing = false; appOpen = null; loadAppOpen() }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { fullScreenShowing = false; appOpen = null; loadAppOpen() }
        }
        fullScreenShowing = true; lastAppOpenAt = now; prefs.lastAdAt = now
        ad.show(activity)
    }

    /** Quảng cáo đang chiếm toàn màn hình (để không bật thêm gì đè lên). */
    val isShowingFullScreen: Boolean get() = fullScreenShowing
}
