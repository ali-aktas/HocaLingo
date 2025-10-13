package com.hocalingo.app.feature.subscription

import com.hocalingo.app.core.base.Result
import javax.inject.Inject

/**
 * RestorePurchasesUseCase
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/subscription/
 *
 * Kullanıcının önceki satın almalarını geri yükler.
 * Cihaz değişimi, uygulama silme/kurma durumlarında kullanılır.
 */
class RestorePurchasesUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {

    /**
     * Satın almaları geri yükler
     */
    suspend operator fun invoke(): Result<Boolean> {
        return when (val result = repository.restorePurchases()) {
            is Result.Success -> {
                // Restore başarılı
                Result.Success(true)
            }
            is Result.Error -> {
                Result.Error(result.error)
            }
        }
    }
}