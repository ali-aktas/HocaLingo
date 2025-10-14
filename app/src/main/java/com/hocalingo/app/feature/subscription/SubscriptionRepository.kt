package com.hocalingo.app.feature.subscription

import android.app.Activity
import com.hocalingo.app.core.base.Result
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import kotlinx.coroutines.flow.Flow

/**
 * SubscriptionRepository - UPDATED ✅
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/subscription/
 *
 * ✅ purchasePackage artık Activity parametresi alıyor
 */
interface SubscriptionRepository {

    /**
     * RevenueCat'ten güncel abonelik durumunu kontrol eder
     */
    suspend fun syncSubscriptionStatus(): Result<SubscriptionState>

    /**
     * Local cache'den abonelik durumunu okur
     */
    fun getLocalSubscriptionState(): Flow<SubscriptionState>

    /**
     * Mevcut abonelik paketlerini RevenueCat'ten alır
     */
    suspend fun getAvailablePackages(): Result<List<Package>>

    /**
     * ✅ UPDATED: Activity parametresi eklendi
     * Bir paketi satın alır
     */
    suspend fun purchasePackage(
        activity: Activity,
        packageToPurchase: Package
    ): Result<CustomerInfo>

    /**
     * Önceki satın almaları geri yükler
     */
    suspend fun restorePurchases(): Result<CustomerInfo>

    /**
     * Premium durumunu hızlıca kontrol eder (cache'den)
     */
    suspend fun isPremium(): Boolean
}