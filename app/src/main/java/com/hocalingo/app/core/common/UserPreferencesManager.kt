package com.hocalingo.app.core.common

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hocalingo.app.core.common.base.AppError
import com.hocalingo.app.core.common.base.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UserPreferencesManager - Enhanced with Profile Settings
 * ✅ Modern theme system (Light/Dark/System)
 * ✅ Study direction preferences
 * ✅ Notification settings
 */
@Singleton
class UserPreferencesManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        // User Identity
        private val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        private val IS_ANONYMOUS_USER = booleanPreferencesKey("is_anonymous_user")

        // Onboarding & Setup
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val LANGUAGE_SETUP_COMPLETED = booleanPreferencesKey("language_setup_completed")
        private val WORDS_SELECTED = booleanPreferencesKey("words_selected")

        // Language Settings
        private val NATIVE_LANGUAGE = stringPreferencesKey("native_language")
        private val TARGET_LANGUAGE = stringPreferencesKey("target_language")
        private val CURRENT_LEVEL = stringPreferencesKey("current_level")

        // Study Preferences
        private val DAILY_GOAL = intPreferencesKey("daily_goal")
        private val STUDY_REMINDER_ENABLED = booleanPreferencesKey("study_reminder_enabled")
        private val STUDY_REMINDER_HOUR = intPreferencesKey("study_reminder_hour")
        private val STUDY_DIRECTION = stringPreferencesKey("study_direction") // EN_TO_TR, TR_TO_EN, MIXED

        // App Settings - Enhanced for Profile
        private val THEME_MODE = stringPreferencesKey("theme_mode") // light, dark, system
        private val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled") // For motivational notifications

        // Premium & Monetization
        private val IS_PREMIUM = booleanPreferencesKey("is_premium")
        private val ADS_ENABLED = booleanPreferencesKey("ads_enabled")
        private val LAST_AD_SHOWN = stringPreferencesKey("last_ad_shown")

        // App State
        private val LAST_PACKAGE_DOWNLOADED = stringPreferencesKey("last_package_downloaded")
        private val LAST_STUDY_SESSION = stringPreferencesKey("last_study_session")
        private val APP_VERSION = stringPreferencesKey("app_version")
    }

    /**
     * User Identity Management
     */
    suspend fun setCurrentUserId(userId: String): Result<Unit> = safeOperation {
        dataStore.edit { preferences ->
            preferences[CURRENT_USER_ID] = userId
        }
    }

    fun getCurrentUserId(): Flow<String?> = dataStore.data
        .map { preferences -> preferences[CURRENT_USER_ID] }
        .catch { emit(null) }

    suspend fun getCurrentUserIdOnce(): String? = try {
        dataStore.data.first()[CURRENT_USER_ID]
    } catch (e: Exception) {
        null
    }

    suspend fun setAnonymousUser(isAnonymous: Boolean): Result<Unit> = safeOperation {
        dataStore.edit { preferences ->
            preferences[IS_ANONYMOUS_USER] = isAnonymous
        }
    }

    fun isAnonymousUser(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[IS_ANONYMOUS_USER] ?: false }
        .catch { emit(false) }

    /**
     * Onboarding Flow Management
     */
    suspend fun setOnboardingCompleted(completed: Boolean): Result<Unit> = safeOperation {
        dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    fun isOnboardingCompleted(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[ONBOARDING_COMPLETED] ?: false }
        .catch { emit(false) }

    suspend fun setLanguageSetupCompleted(completed: Boolean): Result<Unit> = safeOperation {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_SETUP_COMPLETED] = completed
        }
    }

    suspend fun setWordsSelected(selected: Boolean): Result<Unit> = safeOperation {
        dataStore.edit { preferences ->
            preferences[WORDS_SELECTED] = selected
        }
    }

    fun areWordsSelected(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[WORDS_SELECTED] ?: false }
        .catch { emit(false) }

    /**
     * Language Settings
     */
    suspend fun setLanguages(nativeLanguage: String, targetLanguage: String): Result<Unit> = safeOperation {
        dataStore.edit { preferences ->
            preferences[NATIVE_LANGUAGE] = nativeLanguage
            preferences[TARGET_LANGUAGE] = targetLanguage
        }
    }

    fun getNativeLanguage(): Flow<String> = dataStore.data
        .map { preferences -> preferences[NATIVE_LANGUAGE] ?: "tr" }
        .catch { emit("tr") }

    fun getTargetLanguage(): Flow<String> = dataStore.data
        .map { preferences -> preferences[TARGET_LANGUAGE] ?: "en" }
        .catch { emit("en") }

    suspend fun setCurrentLevel(level: String): Result<Unit> = safeOperation {
        dataStore.edit { preferences ->
            preferences[CURRENT_LEVEL] = level
        }
    }

    fun getCurrentLevel(): Flow<String> = dataStore.data
        .map { preferences -> preferences[CURRENT_LEVEL] ?: "A1" }
        .catch { emit("A1") }

    /**
     * Study Preferences
     */
    suspend fun setDailyGoal(goal: Int): Result<Unit> = safeOperation {
        dataStore.edit { preferences ->
            preferences[DAILY_GOAL] = goal
        }
    }

    fun getDailyGoal(): Flow<Int> = dataStore.data
        .map { preferences -> preferences[DAILY_GOAL] ?: 20 }
        .catch { emit(20) }

    suspend fun setStudyReminder(enabled: Boolean, hour: Int = 20): Result<Unit> = safeOperation {
        dataStore.edit { preferences ->
            preferences[STUDY_REMINDER_ENABLED] = enabled
            preferences[STUDY_REMINDER_HOUR] = hour
        }
    }

    fun getStudyReminderSettings(): Flow<Pair<Boolean, Int>> = dataStore.data
        .map { preferences ->
            val enabled = preferences[STUDY_REMINDER_ENABLED] ?: true
            val hour = preferences[STUDY_REMINDER_HOUR] ?: 20
            enabled to hour
        }
        .catch { emit(true to 20) }

    // ✅ Enhanced Study Direction with enum support
    suspend fun setStudyDirection(direction: StudyDirection): Result<Unit> = safeOperation {
        dataStore.edit { preferences ->
            preferences[STUDY_DIRECTION] = direction.value
        }
    }

    fun getStudyDirection(): Flow<StudyDirection> = dataStore.data
        .map { preferences ->
            val value = preferences[STUDY_DIRECTION] ?: "en_to_tr"
            StudyDirection.fromString(value)
        }
        .catch { emit(StudyDirection.EN_TO_TR) }

    /**
     * ✅ NEW: Modern Theme System (Light/Dark/System)
     */
    suspend fun setThemeMode(themeMode: ThemeMode): Result<Unit> = safeOperation {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = themeMode.value
        }
    }

    fun getThemeMode(): Flow<ThemeMode> = dataStore.data
        .map { preferences ->
            val value = preferences[THEME_MODE] ?: "system"
            ThemeMode.fromString(value)
        }
        .catch { emit(ThemeMode.SYSTEM) }

    /**
     * ✅ NEW: Notification Settings (for motivational notifications)
     */
    suspend fun setNotificationsEnabled(enabled: Boolean): Result<Unit> = safeOperation {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    fun areNotificationsEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[NOTIFICATIONS_ENABLED] ?: true }
        .catch { emit(true) }

    /**
     * App Settings
     */
    suspend fun setSoundEnabled(enabled: Boolean): Result<Unit> = safeOperation {
        dataStore.edit { preferences ->
            preferences[SOUND_ENABLED] = enabled
        }
    }

    fun isSoundEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[SOUND_ENABLED] ?: true }
        .catch { emit(true) }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean): Result<Unit> = safeOperation {
        dataStore.edit { preferences ->
            preferences[HAPTIC_FEEDBACK_ENABLED] = enabled
        }
    }

    fun isHapticFeedbackEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[HAPTIC_FEEDBACK_ENABLED] ?: true }
        .catch { emit(true) }

    /**
     * Premium & Monetization
     */
    suspend fun setPremiumStatus(isPremium: Boolean): Result<Unit> = safeOperation {
        dataStore.edit { preferences ->
            preferences[IS_PREMIUM] = isPremium
            preferences[ADS_ENABLED] = !isPremium // Disable ads if premium
        }
    }

    fun isPremium(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[IS_PREMIUM] ?: false }
        .catch { emit(false) }

    fun areAdsEnabled(): Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[ADS_ENABLED] ?: true }
        .catch { emit(true) }

    suspend fun setLastAdShown(timestamp: String): Result<Unit> = safeOperation {
        dataStore.edit { preferences ->
            preferences[LAST_AD_SHOWN] = timestamp
        }
    }

    /**
     * App State Management
     */
    suspend fun setLastPackageDownloaded(packageId: String): Result<Unit> = safeOperation {
        dataStore.edit { preferences ->
            preferences[LAST_PACKAGE_DOWNLOADED] = packageId
        }
    }

    fun getLastPackageDownloaded(): Flow<String?> = dataStore.data
        .map { preferences -> preferences[LAST_PACKAGE_DOWNLOADED] }
        .catch { emit(null) }

    suspend fun setLastStudySession(timestamp: String): Result<Unit> = safeOperation {
        dataStore.edit { preferences ->
            preferences[LAST_STUDY_SESSION] = timestamp
        }
    }

    suspend fun setAppVersion(version: String): Result<Unit> = safeOperation {
        dataStore.edit { preferences ->
            preferences[APP_VERSION] = version
        }
    }

    /**
     * Utility Functions
     */
    suspend fun clearAllPreferences(): Result<Unit> = safeOperation {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun clearUserData(): Result<Unit> = safeOperation {
        dataStore.edit { preferences ->
            // Clear user-specific data but keep app settings
            preferences.remove(CURRENT_USER_ID)
            preferences.remove(IS_ANONYMOUS_USER)
            preferences.remove(ONBOARDING_COMPLETED)
            preferences.remove(LANGUAGE_SETUP_COMPLETED)
            preferences.remove(WORDS_SELECTED)
            preferences.remove(IS_PREMIUM)
            preferences.remove(LAST_PACKAGE_DOWNLOADED)
            preferences.remove(LAST_STUDY_SESSION)
        }
    }

    /**
     * Get all app setup status at once
     */
    data class AppSetupStatus(
        val isUserLoggedIn: Boolean,
        val isOnboardingCompleted: Boolean,
        val areLanguagesSet: Boolean,
        val areWordsSelected: Boolean,
        val currentUserId: String?
    )

    suspend fun getAppSetupStatus(): Result<AppSetupStatus> = try {
        val preferences = dataStore.data.first()

        val status = AppSetupStatus(
            isUserLoggedIn = preferences[CURRENT_USER_ID] != null,
            isOnboardingCompleted = preferences[ONBOARDING_COMPLETED] ?: false,
            areLanguagesSet = preferences[LANGUAGE_SETUP_COMPLETED] ?: false,
            areWordsSelected = preferences[WORDS_SELECTED] ?: false,
            currentUserId = preferences[CURRENT_USER_ID]
        )

        Result.Success(status)
    } catch (e: Exception) {
        Result.Error(AppError.Unknown(e))
    }

    /**
     * Private helper for safe DataStore operations
     */
    private suspend fun safeOperation(operation: suspend () -> Unit): Result<Unit> = try {
        operation()
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppError.Unknown(e))
    }
}