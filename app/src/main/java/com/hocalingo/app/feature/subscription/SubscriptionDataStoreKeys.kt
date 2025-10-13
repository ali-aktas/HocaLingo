package com.hocalingo.app.feature.subscription

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * SubscriptionDataStoreKeys
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/subscription/data/
 *
 * DataStore keys for caching subscription state locally.
 * Bu sayede offline durumda bile premium durumu kontrol edilebilir.
 */
object SubscriptionDataStoreKeys {

    /**
     * Premium Status
     */
    val IS_PREMIUM = booleanPreferencesKey("subscription_is_premium")

    /**
     * Product Type: "monthly", "quarterly", "lifetime"
     */
    val PRODUCT_TYPE = stringPreferencesKey("subscription_product_type")

    /**
     * Expiry Date (epoch milliseconds)
     * null/0 ise lifetime veya geçerli değil
     */
    val EXPIRY_DATE = longPreferencesKey("subscription_expiry_date")

    /**
     * Last Sync Time with RevenueCat (epoch milliseconds)
     */
    val LAST_SYNC_TIME = longPreferencesKey("subscription_last_sync_time")

    /**
     * Original Purchase Date (epoch milliseconds)
     */
    val ORIGINAL_PURCHASE_DATE = longPreferencesKey("subscription_original_purchase_date")

    /**
     * Is in Trial Period
     */
    val IS_IN_TRIAL = booleanPreferencesKey("subscription_is_in_trial")

    /**
     * Will Renew (auto-renewal status)
     */
    val WILL_RENEW = booleanPreferencesKey("subscription_will_renew")
}