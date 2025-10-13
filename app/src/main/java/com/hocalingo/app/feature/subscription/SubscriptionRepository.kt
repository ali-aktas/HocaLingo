package com.hocalingo.app.feature.subscription

import com.hocalingo.app.core.base.Result
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import kotlinx.coroutines.flow.Flow

/**
 * SubscriptionRepository
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/subscription/
 *
 * Abonelik işlemlerinin soyutlanmış interface'i.
 * RevenueCat ve DataStore'dan gelen bilgileri birleştirir.
 */
interface SubscriptionRepository {

    /**
     * RevenueCat'ten güncel abonelik durumunu kontrol eder
     * Network isteği yapar, sonucu cache'e yazar
     */
    suspend fun syncSubscriptionStatus(): Result<SubscriptionState>

    /**
     * Local cache'den abonelik durumunu okur (offline-friendly)
     */
    fun getLocalSubscriptionState(): Flow<SubscriptionState>

    /**
     * Mevcut abonelik paketlerini RevenueCat'ten alır
     */
    suspend fun getAvailablePackages(): Result<List<Package>>

    /**
     * Bir paketi satın alır
     */
    suspend fun purchasePackage(packageToPurchase: Package): Result<CustomerInfo>

    /**
     * Önceki satın almaları geri yükler
     */
    suspend fun restorePurchases(): Result<CustomerInfo>

    /**
     * Premium durumunu hızlıca kontrol eder (cache'den)
     */
    suspend fun isPremium(): Boolean
}