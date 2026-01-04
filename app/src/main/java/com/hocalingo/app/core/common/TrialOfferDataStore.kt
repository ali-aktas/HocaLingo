package com.hocalingo.app.core.common

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TrialOfferDataStore - Akıllı Trial Offer Sistemi
 *
 * Package: app/src/main/java/com/hocalingo/app/core/common/
 *
 * Mantık:
 * 1. İlk onboarding → Trial offer göster
 * 2. User dismiss etti → Kaydet
 * 3. 3 gün sonra → 1 kez daha göster
 * 4. User yine dismiss etti → Artık gösterme
 */
@Singleton
class TrialOfferDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val Context.trialOfferDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "trial_offer_prefs"
    )

    private val dataStore = context.trialOfferDataStore

    companion object {
        private val FIRST_SHOWN = booleanPreferencesKey("trial_offer_first_shown")
        private val FIRST_DISMISS_TIME = longPreferencesKey("trial_offer_first_dismiss_time")
        private val SECOND_SHOWN = booleanPreferencesKey("trial_offer_second_shown")
        private val PERMANENTLY_DISMISSED = booleanPreferencesKey("trial_offer_permanently_dismissed")

        private const val RETRY_DELAY_MS = 3 * 24 * 60 * 60 * 1000L // 3 gün
    }

    /**
     * Trial offer gösterilmeli mi? (Akıllı mantık)
     */
    suspend fun shouldShowTrialOffer(): Boolean {
        val prefs = dataStore.data.first()

        val firstShown = prefs[FIRST_SHOWN] ?: false
        val secondShown = prefs[SECOND_SHOWN] ?: false
        val permanentlyDismissed = prefs[PERMANENTLY_DISMISSED] ?: false

        // Permanently dismiss edilmişse gösterme
        if (permanentlyDismissed) {
            DebugHelper.log("Trial offer: Permanently dismissed")
            return false
        }

        // İlk kez gösterilecek
        if (!firstShown) {
            DebugHelper.log("Trial offer: First time - SHOW")
            return true
        }

        // İkinci kez gösterilecek (3 gün sonra)
        if (!secondShown) {
            val firstDismissTime = prefs[FIRST_DISMISS_TIME] ?: 0L
            val currentTime = System.currentTimeMillis()
            val timePassed = currentTime - firstDismissTime

            if (timePassed >= RETRY_DELAY_MS) {
                DebugHelper.log("Trial offer: 3 days passed - SHOW AGAIN")
                return true
            } else {
                val daysRemaining = (RETRY_DELAY_MS - timePassed) / (24 * 60 * 60 * 1000)
                DebugHelper.log("Trial offer: Wait ${daysRemaining} more days")
                return false
            }
        }

        // Her iki kez de gösterildi, artık gösterme
        DebugHelper.log("Trial offer: Already shown twice - DON'T SHOW")
        return false
    }

    /**
     * İlk gösterim kaydı
     */
    suspend fun markFirstShown() {
        dataStore.edit { prefs ->
            prefs[FIRST_SHOWN] = true
            DebugHelper.log("Trial offer: First shown marked")
        }
    }

    /**
     * İlk dismiss kaydı
     */
    suspend fun markFirstDismissed() {
        dataStore.edit { prefs ->
            prefs[FIRST_DISMISS_TIME] = System.currentTimeMillis()
            DebugHelper.log("Trial offer: First dismissed at ${System.currentTimeMillis()}")
        }
    }

    /**
     * İkinci gösterim kaydı
     */
    suspend fun markSecondShown() {
        dataStore.edit { prefs ->
            prefs[SECOND_SHOWN] = true
            DebugHelper.log("Trial offer: Second shown marked")
        }
    }

    /**
     * İkinci dismiss kaydı (permanently dismiss)
     */
    suspend fun markPermanentlyDismissed() {
        dataStore.edit { prefs ->
            prefs[PERMANENTLY_DISMISSED] = true
            DebugHelper.log("Trial offer: Permanently dismissed")
        }
    }

    /**
     * Premium satın alındığında sıfırla
     */
    suspend fun resetAfterPurchase() {
        dataStore.edit { prefs ->
            prefs[PERMANENTLY_DISMISSED] = true
            DebugHelper.log("Trial offer: Reset after purchase")
        }
    }

    /**
     * Debug: State'i kontrol et
     */
    suspend fun getDebugInfo(): String {
        val prefs = dataStore.data.first()
        return """
            Trial Offer State:
            - First shown: ${prefs[FIRST_SHOWN] ?: false}
            - First dismiss time: ${prefs[FIRST_DISMISS_TIME] ?: 0L}
            - Second shown: ${prefs[SECOND_SHOWN] ?: false}
            - Permanently dismissed: ${prefs[PERMANENTLY_DISMISSED] ?: false}
        """.trimIndent()
    }
}