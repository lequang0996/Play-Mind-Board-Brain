package com.fws.playmind.data

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/** Sự kiện thanh toán để UI hiện thông báo một lần. */
sealed interface BillingEvent {
    data class GemsGranted(val n: Int) : BillingEvent
    data class PlanActive(val plan: Plan) : BillingEvent
    data object Pending : BillingEvent
    data object Failed : BillingEvent
}

/**
 * Google Play Billing (PBL 8).
 *  - Đăng ký: [SUB_MONTHLY] 1$/tháng (không còn quảng cáo nào, 3 lượt miễn phí/game/ngày),
 *             [SUB_YEARLY] 10$/năm (không còn quảng cáo nào, không giới hạn lượt chơi).
 *  - Mua một lần (tiêu thụ được): [GEMS_5] 1$ = 5 kim cương, [GEMS_30] 5$ = 30 kim cương.
 * Các mã sản phẩm phải được tạo đúng tên này trong Play Console (xem docs/MONETIZATION.md).
 * Không có máy chủ nên việc xác minh giao dịch chỉ dựa vào Google Play trên máy; muốn chống gian lận triệt để cần xác minh
 * purchaseToken ở máy chủ (Google Play Developer API).
 */
class BillingManager(private val app: Application, private val wallet: WalletRepo) : PurchasesUpdatedListener {
    companion object {
        const val SUB_MONTHLY = "pm_noads_monthly"
        const val SUB_YEARLY = "pm_pro_yearly"
        const val GEMS_5 = "pm_diamonds_5"
        const val GEMS_30 = "pm_diamonds_30"
        val GEMS = mapOf(GEMS_5 to 5, GEMS_30 to 30)
    }

    class Offer(val id: String, val details: ProductDetails, val price: String)

    /** Sản phẩm đã lấy được từ Google Play (rỗng nếu chưa kết nối / chưa tạo sản phẩm). */
    var offers by mutableStateOf<Map<String, Offer>>(emptyMap()); private set
    var connected by mutableStateOf(false); private set
    var event by mutableStateOf<BillingEvent?>(null); private set
    fun consumeEvent() { event = null }

    private val client: BillingClient = BillingClient.newBuilder(app)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    fun start() {
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(r: BillingResult) {
                if (r.responseCode == BillingClient.BillingResponseCode.OK) {
                    connected = true
                    loadProducts(); refresh()
                }
            }
            override fun onBillingServiceDisconnected() { connected = false }
        })
    }

    private fun loadProducts() {
        fun query(type: String, ids: List<String>) {
            val params = QueryProductDetailsParams.newBuilder().setProductList(
                ids.map { QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(type).build() }
            ).build()
            client.queryProductDetailsAsync(params) { res, result ->
                if (res.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
                val m = offers.toMutableMap()
                for (d in result.productDetailsList) {
                    val price = if (type == BillingClient.ProductType.SUBS)
                        d.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
                    else d.oneTimePurchaseOfferDetails?.formattedPrice
                    if (price != null) m[d.productId] = Offer(d.productId, d, price)
                }
                offers = m
            }
        }
        query(BillingClient.ProductType.SUBS, listOf(SUB_MONTHLY, SUB_YEARLY))
        query(BillingClient.ProductType.INAPP, listOf(GEMS_5, GEMS_30))
    }

    /** Đối chiếu lại với Google Play: gói đang có + giao dịch kim cương chưa được tiêu thụ. Gọi mỗi lần mở app. */
    fun refresh() {
        if (!client.isReady) { start(); return }
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()) { res, list ->
            if (res.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            val active = list.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            active.forEach { acknowledgeIfNeeded(it) }
            val ids = active.flatMap { it.products }
            wallet.setPlan(when { SUB_YEARLY in ids -> Plan.YEARLY; SUB_MONTHLY in ids -> Plan.MONTHLY; else -> Plan.NONE })
        }
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()) { res, list ->
            if (res.responseCode == BillingClient.BillingResponseCode.OK) list.forEach { handle(it, silent = true) }
        }
    }

    fun buy(activity: Activity, productId: String) {
        val o = offers[productId] ?: run { event = BillingEvent.Failed; return }
        val pb = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(o.details)
        o.details.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let { pb.setOfferToken(it) }
        val r = client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(pb.build())).build())
        if (r.responseCode != BillingClient.BillingResponseCode.OK && r.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) event = BillingEvent.Failed
    }

    override fun onPurchasesUpdated(r: BillingResult, purchases: MutableList<Purchase>?) {
        when (r.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases?.forEach { handle(it, silent = false) }
            BillingClient.BillingResponseCode.USER_CANCELED -> {}
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            else -> event = BillingEvent.Failed
        }
    }

    private fun handle(p: Purchase, silent: Boolean) {
        if (p.purchaseState == Purchase.PurchaseState.PENDING) { if (!silent) event = BillingEvent.Pending; return }
        if (p.purchaseState != Purchase.PurchaseState.PURCHASED) return
        for (id in p.products) {
            val gems = GEMS[id]
            if (gems != null) {
                // Gói năm: kim cương không bao giờ tăng. Giao dịch kim cương (nếu có) để nguyên, không tiêu thụ;
                // khi gói năm bị hủy, refresh() sẽ xử lý lại (Google Play tự hoàn tiền nếu quá 3 ngày chưa tiêu thụ).
                if (wallet.noDiamonds()) continue
                // Chỉ cộng kim cương SAU KHI tiêu thụ thành công: mỗi giao dịch chỉ tiêu thụ được một lần nên không bao giờ cộng đôi.
                val total = gems * p.quantity.coerceAtLeast(1)
                client.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { r, _ ->
                    if (r.responseCode == BillingClient.BillingResponseCode.OK) { wallet.addDiamonds(total); event = BillingEvent.GemsGranted(total) }
                }
            } else if (id == SUB_MONTHLY || id == SUB_YEARLY) {
                acknowledgeIfNeeded(p)
                val plan = if (id == SUB_YEARLY) Plan.YEARLY else Plan.MONTHLY
                wallet.setPlan(if (wallet.currentPlan().ordinal > plan.ordinal) wallet.currentPlan() else plan)
                if (!silent) event = BillingEvent.PlanActive(plan)
            }
        }
    }

    private fun acknowledgeIfNeeded(p: Purchase) {
        if (p.isAcknowledged) return
        client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { }
    }
}
