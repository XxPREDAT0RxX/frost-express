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

/**
 * Google AdMob rewarded ads. Uses Google's test ad unit (Balance.AD_UNIT_REWARDED) until you put in your own.
 * The reward is given after the player closes the ad, and only if they watched it to the end.
 */
class AdMobRewarded(
    private val activity: android.app.Activity,
    private val onReady: (Boolean) -> Unit,
) : AdProvider {
    private var ad: com.google.android.gms.ads.rewarded.RewardedAd? = null
    private var loading = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    init {
        com.google.android.gms.ads.MobileAds.initialize(activity) { load() }
    }

    private fun load() {
        if (loading || ad != null) return
        loading = true
        com.google.android.gms.ads.rewarded.RewardedAd.load(
            activity,
            Balance.AD_UNIT_REWARDED,
            com.google.android.gms.ads.AdRequest.Builder().build(),
            object : com.google.android.gms.ads.rewarded.RewardedAdLoadCallback() {
                override fun onAdLoaded(loaded: com.google.android.gms.ads.rewarded.RewardedAd) {
                    loading = false
                    ad = loaded
                    onReady(true)
                }

                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    loading = false
                    ad = null
                    onReady(false)
                    handler.postDelayed({ load() }, 30_000L) // try again later (offline, no fill, ...)
                }
            },
        )
    }

    override fun showRewarded(onReward: () -> Unit) {
        val current = ad ?: run { load(); return }
        ad = null
        onReady(false)
        var earned = false
        current.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                if (earned) onReward()
                load()
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                load()
            }
        }
        current.show(activity) { earned = true }
    }
}
