package com.chetna.frostexpress

/**
 * Ad hook for after launch. Ads are OFF by default (Balance.ADS_ENABLED = false).
 * To enable rewarded ads later:
 *  1. Add the AdMob SDK and your app id, then implement AdProvider with a rewarded ad.
 *  2. Replace NoAds in GameViewModel.ads with your implementation.
 *  3. Set Balance.ADS_ENABLED = true and update the Play Console ads declaration + privacy policy.
 */
interface AdProvider {
    /** Shows a rewarded ad. Calls onReward only if the user finished watching it. */
    fun showRewarded(onReward: () -> Unit)
}

/** Placeholder used while ads are disabled. */
object NoAds : AdProvider {
    override fun showRewarded(onReward: () -> Unit) {}
}
