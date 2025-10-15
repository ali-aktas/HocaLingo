package com.hocalingo.app.core.ads

import android.view.LayoutInflater
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.hocalingo.app.R

/**
 * NativeAdCard - Native Reklam UI Component
 *
 * Package: app/src/main/java/com/hocalingo/app/core/ads/
 *
 * Google AdMob native reklamlarını gösterir.
 * - Material 3 Card wrapper
 * - "Sponsorlu" badge
 * - Dark mode support
 * - Swipeable in lists
 */
@Composable
fun NativeAdCard(
    nativeAd: NativeAd?,
    modifier: Modifier = Modifier,
    cardHeight: Int = 300
) {
    if (nativeAd == null) {
        // Ad yüklenene kadar placeholder göster
        NativeAdPlaceholder(modifier = modifier)
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // "Sponsorlu" badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Sponsorlu",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Native Ad View
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { context ->
                    val inflater = LayoutInflater.from(context)
                    val adView = inflater.inflate(
                        R.layout.native_ad_layout,
                        null
                    ) as NativeAdView

                    // Populate the native ad
                    populateNativeAdView(nativeAd, adView)

                    adView
                },
                update = { adView ->
                    // Update ad view if needed
                    populateNativeAdView(nativeAd, adView)
                }
            )
        }
    }
}

/**
 * Populate Native Ad View with data
 */
private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    // Set the native ad to the ad view
    adView.setNativeAd(nativeAd)

    // Set asset views
    adView.headlineView = adView.findViewById(R.id.ad_headline)
    adView.bodyView = adView.findViewById(R.id.ad_body)
    adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
    adView.iconView = adView.findViewById(R.id.ad_app_icon)
    adView.priceView = adView.findViewById(R.id.ad_price)
    adView.starRatingView = adView.findViewById(R.id.ad_stars)
    adView.storeView = adView.findViewById(R.id.ad_store)
    adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
    adView.mediaView = adView.findViewById(R.id.ad_media)

    // Populate headline
    nativeAd.headline?.let {
        (adView.headlineView as? android.widget.TextView)?.text = it
    }

    // Populate body
    nativeAd.body?.let {
        adView.bodyView?.visibility = android.view.View.VISIBLE
        (adView.bodyView as? android.widget.TextView)?.text = it
    } ?: run {
        adView.bodyView?.visibility = android.view.View.GONE
    }

    // Populate call to action
    nativeAd.callToAction?.let {
        adView.callToActionView?.visibility = android.view.View.VISIBLE
        (adView.callToActionView as? android.widget.Button)?.text = it
    } ?: run {
        adView.callToActionView?.visibility = android.view.View.GONE
    }

    // Populate icon
    nativeAd.icon?.let {
        adView.iconView?.visibility = android.view.View.VISIBLE
        (adView.iconView as? android.widget.ImageView)?.setImageDrawable(it.drawable)
    } ?: run {
        adView.iconView?.visibility = android.view.View.GONE
    }

    // Populate price
    nativeAd.price?.let {
        adView.priceView?.visibility = android.view.View.VISIBLE
        (adView.priceView as? android.widget.TextView)?.text = it
    } ?: run {
        adView.priceView?.visibility = android.view.View.GONE
    }

    // Populate star rating
    nativeAd.starRating?.let {
        adView.starRatingView?.visibility = android.view.View.VISIBLE
        (adView.starRatingView as? android.widget.RatingBar)?.rating = it.toFloat()
    } ?: run {
        adView.starRatingView?.visibility = android.view.View.GONE
    }

    // Populate store
    nativeAd.store?.let {
        adView.storeView?.visibility = android.view.View.VISIBLE
        (adView.storeView as? android.widget.TextView)?.text = it
    } ?: run {
        adView.storeView?.visibility = android.view.View.GONE
    }

    // Populate advertiser
    nativeAd.advertiser?.let {
        adView.advertiserView?.visibility = android.view.View.VISIBLE
        (adView.advertiserView as? android.widget.TextView)?.text = it
    } ?: run {
        adView.advertiserView?.visibility = android.view.View.GONE
    }

    // Populate media view
    nativeAd.mediaContent?.let {
        adView.mediaView?.setMediaContent(it)
    }
}

/**
 * Placeholder while ad is loading
 */
@Composable
private fun NativeAdPlaceholder(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Reklam yükleniyor...",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}