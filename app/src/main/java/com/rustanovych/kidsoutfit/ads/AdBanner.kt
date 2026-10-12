package com.rustanovych.kidsoutfit.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.rustanovych.kidsoutfit.BuildConfig

/**
 * Adaptive anchored banner. Collapses to nothing when no ad can be loaded, so a failed request
 * never leaves an empty strip. Place it only once consent allows ads (see [AdConsent]).
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    var failed by remember { mutableStateOf(false) }

    val adView = remember(widthDp) {
        AdView(context).apply {
            adUnitId = BuildConfig.ADMOB_BANNER_ID
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp))
            adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    failed = true
                }

                override fun onAdLoaded() {
                    failed = false
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(adView) { onDispose { adView.destroy() } }

    AndroidView(
        factory = { adView },
        // The bottom bar slot is not inset-aware, so the banner keeps itself above the nav bar.
        // A failed load drops the inset too: the whole container must be gone, not just the ad.
        modifier = modifier
            .fillMaxWidth()
            .then(if (failed) Modifier.height(0.dp) else Modifier.navigationBarsPadding()),
    )
}
