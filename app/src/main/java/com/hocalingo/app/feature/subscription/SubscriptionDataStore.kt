package com.hocalingo.app.feature.subscription

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.core.common.DebugHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SubscriptionDataStore
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/subscription/
 *
 * Abonelik bilgilerini local olarak cache'ler.
 * Offline durumda bile premium kontrolü yapılabilsin diye.
 */
@Singleton
class SubscriptionDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    /**
     * Subscription state'i Flow olarak döner
     */
    fun getSubscriptionState(): Flow<SubscriptionState> = dataStore.data
        .map { preferences ->
            SubscriptionState(
                isPremium = preferences[SubscriptionDataStoreKeys.IS_PREMIUM] ?: false,
                productType = ProductType.fromString(
                    preferences[SubscriptionDataStoreKeys.PRODUCT_TYPE]
                ),
                expiryDate = preferences[SubscriptionDataStoreKeys.EXPIRY_DATE],
                lastSyncTime = preferences[SubscriptionDataStoreKeys.LAST_SYNC_TIME] ?: 0L,
                originalPurchaseDate = preferences[SubscriptionDataStoreKeys.ORIGINAL_PURCHASE_DATE],
                isInTrialPeriod = preferences[SubscriptionDataStoreKeys.IS_IN_TRIAL] ?: false,
                willRenew = preferences[SubscriptionDataStoreKeys.WILL_RENEW] ?: false
            )
        }
        .catch { exception ->
            DebugHelper.logError("DataStore read error", exception)
            emit(SubscriptionState.FREE)
        }

    /**
     * Subscription state'i kaydeder
     */
    suspend fun saveSubscriptionState(state: SubscriptionState): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[SubscriptionDataStoreKeys.IS_PREMIUM] = state.isPremium
                preferences[SubscriptionDataStoreKeys.PRODUCT_TYPE] = state.productType?.name?.lowercase() ?: ""

                state.expiryDate?.let {
                    preferences[SubscriptionDataStoreKeys.EXPIRY_DATE] = it
                }

                preferences[SubscriptionDataStoreKeys.LAST_SYNC_TIME] = System.currentTimeMillis()

                state.originalPurchaseDate?.let {
                    preferences[SubscriptionDataStoreKeys.ORIGINAL_PURCHASE_DATE] = it
                }

                preferences[SubscriptionDataStoreKeys.IS_IN_TRIAL] = state.isInTrialPeriod
                preferences[SubscriptionDataStoreKeys.WILL_RENEW] = state.willRenew
            }

            DebugHelper.logSuccess("Subscription state saved: isPremium=${state.isPremium}")
            Result.Success(Unit)

        } catch (e: Exception) {
            DebugHelper.logError("Failed to save subscription state", e)
            Result.Error(com.hocalingo.app.core.base.AppError.Unknown(e))
        }
    }

    /**
     * Premium durumunu temizler (logout vs.)
     */
    suspend fun clearSubscriptionState(): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences.remove(SubscriptionDataStoreKeys.IS_PREMIUM)
                preferences.remove(SubscriptionDataStoreKeys.PRODUCT_TYPE)
                preferences.remove(SubscriptionDataStoreKeys.EXPIRY_DATE)
                preferences.remove(SubscriptionDataStoreKeys.ORIGINAL_PURCHASE_DATE)
                preferences.remove(SubscriptionDataStoreKeys.IS_IN_TRIAL)
                preferences.remove(SubscriptionDataStoreKeys.WILL_RENEW)
            }

            DebugHelper.log("Subscription state cleared")
            Result.Success(Unit)

        } catch (e: Exception) {
            DebugHelper.logError("Failed to clear subscription state", e)
            Result.Error(com.hocalingo.app.core.base.AppError.Unknown(e))
        }
    }
}