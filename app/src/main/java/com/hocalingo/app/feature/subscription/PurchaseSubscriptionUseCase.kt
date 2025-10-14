package com.hocalingo.app.feature.subscription

import android.app.Activity
import com.hocalingo.app.core.base.Result
import com.revenuecat.purchases.Package
import javax.inject.Inject

/**
 * PurchaseSubscriptionUseCase - UPDATED ✅
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/subscription/
 *
 * ✅ Activity parametresi eklendi
 */
class PurchaseSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {

    /**
     * Mevcut paketleri getirir
     */
    suspend fun getAvailablePackages(): Result<List<Package>> {
        return repository.getAvailablePackages()
    }

    /**
     * ✅ UPDATED: Activity parametresi eklendi
     * Seçilen paketi satın alır
     */
    suspend fun purchasePackage(
        activity: Activity,
        packageToPurchase: Package
    ): Result<Boolean> {
        return when (val result = repository.purchasePackage(activity, packageToPurchase)) {
            is Result.Success -> {
                // Purchase başarılı, sync yap
                repository.syncSubscriptionStatus()
                Result.Success(true)
            }
            is Result.Error -> {
                Result.Error(result.error)
            }
        }
    }
}