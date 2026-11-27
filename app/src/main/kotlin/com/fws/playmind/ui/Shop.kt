package com.fws.playmind.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fws.playmind.PlayMindApp
import com.fws.playmind.R
import com.fws.playmind.data.BillingEvent
import com.fws.playmind.data.BillingManager
import com.fws.playmind.data.Plan

@Composable
private fun Perk(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Ic.Check, null, Modifier.size(16.dp), tint = pm.good)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = pm.ink)
    }
}

@Composable
private fun PlanCard(title: String, price: String?, per: String, perks: List<String>, active: Boolean, highlight: Boolean, onBuy: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.linearGradient(listOf(if (highlight) pm.goldSoft else pm.surfaceHigh, pm.surface), Offset(0f, 0f), Offset(900f, 500f)))
            .border(if (highlight) 1.5.dp else 1.dp, if (highlight) pm.goldBright else pm.outline, shape).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, color = pm.ink)
            if (active) Pill(stringResource(R.string.plan_active), color = pm.good, filled = true)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(price ?: "—", style = MaterialTheme.typography.headlineMedium, color = pm.goldHi)
            Text(per, style = MaterialTheme.typography.bodyMedium, color = pm.inkSoft, modifier = Modifier.padding(bottom = 3.dp))
        }
        perks.forEach { Perk(it) }
        Spacer(Modifier.height(4.dp))
        GoldButton(
            if (active) stringResource(R.string.plan_active) else if (price == null) stringResource(R.string.store_unavailable) else stringResource(R.string.subscribe),
            onBuy, Modifier.fillMaxWidth(), enabled = !active && price != null,
        )
    }
}

@Composable
private fun GemPack(count: Int, price: String?, best: Boolean, onBuy: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.clip(shape).background(pm.surface).border(if (best) 1.5.dp else 1.dp, if (best) pm.goldBright else pm.outline, shape).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (best) Pill(stringResource(R.string.best_value), color = pm.goldBright, filled = true) else Spacer(Modifier.height(26.dp))
        GemMark(44.dp)
        Text("$count", style = MaterialTheme.typography.headlineMedium, color = pm.ink)
        Text(stringResource(R.string.gems_word), style = MaterialTheme.typography.bodySmall, color = pm.inkSoft)
        OutlineButton(price ?: "—", onBuy, Modifier.fillMaxWidth(), enabled = price != null)
    }
}

@Composable
fun ShopScreen(app: PlayMindApp, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val activity = ctx as Activity
    val billing = app.billing
    val wallet by app.wallet.state.collectAsState()
    val plan = app.wallet.currentPlan()
    val offers = billing.offers
    fun price(id: String) = offers[id]?.price

    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.shop), onBack) { DiamondChip(wallet.diamonds) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle(stringResource(R.string.shop_plans))
            PlanCard(
                stringResource(R.string.plan_monthly), price(BillingManager.SUB_MONTHLY), stringResource(R.string.per_month),
                listOf(stringResource(R.string.perk_no_banner), stringResource(R.string.perk_no_appopen), stringResource(R.string.perk_free3)),
                plan == Plan.MONTHLY, false,
            ) { billing.buy(activity, BillingManager.SUB_MONTHLY) }
            PlanCard(
                stringResource(R.string.plan_yearly), price(BillingManager.SUB_YEARLY), stringResource(R.string.per_year),
                listOf(stringResource(R.string.perk_no_ads), stringResource(R.string.perk_unlimited)),
                plan == Plan.YEARLY, true,
            ) { billing.buy(activity, BillingManager.SUB_YEARLY) }
            if (plan != Plan.NONE) OutlineButton(stringResource(R.string.manage_sub), {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions?package=${ctx.packageName}")))
            }, Modifier.fillMaxWidth())

            Spacer(Modifier.height(6.dp))
            SectionTitle(stringResource(R.string.shop_gems))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GemPack(5, price(BillingManager.GEMS_5), false, { billing.buy(activity, BillingManager.GEMS_5) }, Modifier.weight(1f))
                GemPack(30, price(BillingManager.GEMS_30), true, { billing.buy(activity, BillingManager.GEMS_30) }, Modifier.weight(1f))
            }
            OutlineButton(stringResource(R.string.restore_purchases), { billing.refresh() }, Modifier.fillMaxWidth(), Ic.Refresh)
            Spacer(Modifier.height(20.dp))
        }
    }

    when (val e = billing.event) {
        is BillingEvent.GemsGranted -> PmDialog(stringResource(R.string.bill_ok_title), stringResource(R.string.bill_ok_gems, e.n), stringResource(R.string.ok), { billing.consumeEvent() }, tone = DialogTone.GEM)
        is BillingEvent.PlanActive -> PmDialog(stringResource(R.string.bill_ok_title), stringResource(R.string.bill_ok_plan), stringResource(R.string.ok), { billing.consumeEvent() }, tone = DialogTone.SUCCESS)
        BillingEvent.Pending -> PmDialog(stringResource(R.string.notice_title), stringResource(R.string.bill_pending), stringResource(R.string.ok), { billing.consumeEvent() })
        BillingEvent.Failed -> PmDialog(stringResource(R.string.notice_title), stringResource(R.string.bill_failed), stringResource(R.string.ok), { billing.consumeEvent() }, tone = DialogTone.WARN)
        null -> {}
    }
}
