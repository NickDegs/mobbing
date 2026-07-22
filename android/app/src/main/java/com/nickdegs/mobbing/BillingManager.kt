package com.nickdegs.mobbing

import android.app.Activity
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

/** Rüşvet IAP — consumable, product id: "bribe" */
class BillingManager(private val activity: Activity) : PurchasesUpdatedListener {

    var onBribeSuccess: (() -> Unit)? = null
    var product: ProductDetails? = null; private set
    var priceLabel: String = "$0.99"; private set

    private val client: BillingClient = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun connect() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(r: BillingResult) {
                if (r.responseCode == BillingClient.BillingResponseCode.OK) query()
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun query() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId("bribe")
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()))
            .build()
        client.queryProductDetailsAsync(params) { r, list ->
            if (r.responseCode == BillingClient.BillingResponseCode.OK) {
                product = list.firstOrNull()
                product?.oneTimePurchaseOfferDetails?.formattedPrice?.let { priceLabel = it }
            }
        }
    }

    fun buy() {
        val p = product ?: return
        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(p).build()))
            .build()
        client.launchBillingFlow(activity, flow)
    }

    override fun onPurchasesUpdated(r: BillingResult, purchases: MutableList<Purchase>?) {
        if (r.responseCode != BillingClient.BillingResponseCode.OK || purchases == null) return
        for (pur in purchases) {
            if (pur.purchaseState == Purchase.PurchaseState.PURCHASED) {
                client.consumeAsync(
                    ConsumeParams.newBuilder().setPurchaseToken(pur.purchaseToken).build()
                ) { _, _ -> }
                activity.runOnUiThread { onBribeSuccess?.invoke() }
            }
        }
    }
}
