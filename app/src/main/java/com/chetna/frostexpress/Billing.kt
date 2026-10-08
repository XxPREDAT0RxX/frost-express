package com.chetna.frostexpress

/**
 * In-app purchase hook for after launch (OFF by default, Balance.IAP_ENABLED = false).
 * To enable: add Google Play Billing, implement PurchaseProvider, create the products in Play Console
 * (for example "remove_ads" and "starter_pack"), then switch the flag on.
 */
interface PurchaseProvider {
    fun purchase(productId: String, onSuccess: () -> Unit)
}

object NoPurchases : PurchaseProvider {
    override fun purchase(productId: String, onSuccess: () -> Unit) {}
}
