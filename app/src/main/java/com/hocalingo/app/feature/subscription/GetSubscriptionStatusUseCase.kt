package com.hocalingo.app.feature.subscription

import com.hocalingo.app.core.base.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * GetSubscriptionStatusUseCase
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/subscription/
 *
 * Kullanıcının abonelik durumunu kontrol eder.
 * Önce cache'e bakar, sonra RevenueCat'ten sync eder.
 */
class GetSubscriptionStatusUseCase @Inject constructor(
    private val repository: SubscriptionRepository
) {

    /**
     * Local cache'den subscription state Flow'u döner (reactive)
     */
    fun observeSubscriptionState(): Flow<SubscriptionState> {
        return repository.getLocalSubscriptionState()
    }

    /**
     * RevenueCat'ten sync eder (network isteği)
     */
    suspend fun syncFromRemote(): Result<SubscriptionState> {
        return repository.syncSubscriptionStatus()
    }

    /**
     * Premium durumunu hızlıca kontrol eder
     */
    suspend fun isPremium(): Boolean {
        return repository.isPremium()
    }
}