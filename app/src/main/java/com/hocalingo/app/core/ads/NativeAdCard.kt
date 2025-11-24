package com.hocalingo.app.core.ads

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.hocalingo.app.R

/**
 * NativeAdCard - Native Reklam UI Component
 *
 * Package: app/src/main/java/com/hocalingo/app/core/ads/
 *
 * Google AdMob native reklamlarını gösterir.
 */
@SuppressLint("InflateParams")
@Composable
fun NativeAdCard(
    nativeAd: NativeAd?,
    modifier: Modifier = Modifier
) {
    if (nativeAd == null) return

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val inflater = LayoutInflater.from(context)
            val adView = inflater.inflate(
                R.layout.native_ad_layout,
                null,
                false
            ) as NativeAdView

            populateNativeAdView(nativeAd, adView)
            adView
        }
    )
}

/**
 * ✅ Native Ad View'ı Doldur (Tip Güvenli)
 */
private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    // ✅ Find views with explicit types
    val iconView: ImageView? = adView.findViewById(R.id.ad_app_icon)
    val headlineView: TextView? = adView.findViewById(R.id.ad_headline)
    val bodyView: TextView? = adView.findViewById(R.id.ad_body)
    val ctaView: Button? = adView.findViewById(R.id.ad_call_to_action)
    val advertiserView: TextView? = adView.findViewById(R.id.ad_advertiser)
    val starRatingView: RatingBar? = adView.findViewById(R.id.ad_stars)
    val mediaView: MediaView? = adView.findViewById(R.id.ad_media)

//    // Opsiyonel view'lar (XML'de olmayabilir)

//    val priceView: TextView? = adView.findViewById(R.id.ad_price)
//    val storeView: TextView? = adView.findViewById(R.id.ad_store)

    // ✅ Assign views to NativeAdView
    adView.headlineView = headlineView
    adView.bodyView = bodyView
    adView.callToActionView = ctaView
    adView.iconView = iconView
    adView.advertiserView = advertiserView
    adView.starRatingView = starRatingView
    adView.mediaView = mediaView

//    adView.priceView = priceView
//    adView.storeView = storeView

    // ✅ Set content from NativeAd
    headlineView?.text = nativeAd.headline ?: ""
    bodyView?.text = nativeAd.body ?: ""
    ctaView?.text = nativeAd.callToAction ?: "Daha Fazla"
    advertiserView?.text = nativeAd.advertiser ?: ""

    // Icon
    nativeAd.icon?.let { icon ->
        iconView?.setImageDrawable(icon.drawable)
    }

    // Star rating
    nativeAd.starRating?.let { rating ->
        starRatingView?.rating = rating.toFloat()
    }

//    // Price (opsiyonel)
//    nativeAd.price?.let { price ->
//        priceView?.text = price
//    }
//
//    // Store (opsiyonel)
//    nativeAd.store?.let { store ->
//        storeView?.text = store
//    }

    // ✅ EN ÖNEMLİ: Native ad'i view'a bind et
    adView.setNativeAd(nativeAd)
}