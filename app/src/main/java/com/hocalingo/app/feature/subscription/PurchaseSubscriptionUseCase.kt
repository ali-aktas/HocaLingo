package com.hocalingo.app.feature.subscription

import com.hocalingo.app.core.base.Result
import com.revenuecat.purchases.Package
import javax.inject.Inject

/**
 * PurchaseSubscriptionUseCase
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/subscription/
 *
 * Abonelik satın alma işlemini yönetir.
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
     * Seçilen paketi satın alır
     */
    suspend fun purchasePackage(packageToPurchase: Package): Result<Boolean> {
        return when (val result = repository.purchasePackage(packageToPurchase)) {
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