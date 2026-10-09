package com.chetna.frostexpress

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
import com.android.billingclient.api.QueryPurchasesParams

/**
 * In-app purchases (gem packs). OFF by default: Balance.IAP_ENABLED = false.
 * To switch on: create consumable products with the ids in GemPack (gems_pouch, gems_chest, gems_vault)
 * in Play Console, upload a build to a testing track, then set IAP_ENABLED = true.
 */
interface PurchaseProvider {
    fun purchase(productId: String)
    /** Localised price from Google Play, or null while unknown. */
    fun price(productId: String): String?
    fun release() {}
}

object NoPurchases : PurchaseProvider {
    override fun purchase(productId: String) {}
    override fun price(productId: String): String? = null
}

/** Google Play Billing. [grant] is called once per confirmed, consumed purchase. */
class PlayBilling(
    private val activity: Activity,
    private val onPrices: () -> Unit,
    private val grant: (productId: String, quantity: Int) -> Unit,
) : PurchaseProvider, PurchasesUpdatedListener {

    private val details = HashMap<String, ProductDetails>()
    private val client: BillingClient = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    init {
        connect()
    }

    private fun connect() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) return
                loadProducts()
                // purchases finished while the app was closed
                client.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
                ) { r, list -> if (r.responseCode == BillingClient.BillingResponseCode.OK) list.forEach(::handle) }
            }

            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun loadProducts() {
        val products = GemPack.values().map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it.productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(products).build()) { r, list ->
            if (r.responseCode == BillingClient.BillingResponseCode.OK) {
                activity.runOnUiThread {
                    list.forEach { details[it.productId] = it }
                    onPrices()
                }
            }
        }
    }

    override fun price(productId: String): String? = details[productId]?.oneTimePurchaseOfferDetails?.formattedPrice

    override fun purchase(productId: String) {
        val d = details[productId]
        if (d == null) {
            if (!client.isReady) connect() else loadProducts()
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build()))
            .build()
        client.launchBillingFlow(activity, params)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases?.forEach(::handle)
    }

    private fun handle(p: Purchase) {
        if (p.purchaseState != Purchase.PurchaseState.PURCHASED) return
        client.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { r, _ ->
            if (r.responseCode == BillingClient.BillingResponseCode.OK) {
                activity.runOnUiThread { p.products.forEach { grant(it, p.quantity) } }
            }
        }
    }

    override fun release() {
        client.endConnection()
    }
}
