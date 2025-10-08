package com.hocalingo.app.database

import com.hocalingo.app.core.common.UserPreferencesManager
import com.hocalingo.app.core.base.AppError
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.database.entities.SelectionStatus
import com.hocalingo.app.feature.auth.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DatabaseSeeder - FIXED FOR FIREBASE
 *
 * Package: app/src/main/java/com/hocalingo/app/database/
 *
 * ✅ Firebase uyumlu hale getirildi
 * ✅ Assets bağımlılıkları kaldırıldı
 * ✅ Eski metodlar yeni sisteme adapte edildi
 *
 * Handles initial data setup for new users and app installation
 * Coordinates JsonLoader, UserRepository, and UserPreferencesManager
 */
@Singleton
class MainDatabaseSeeder @Inject constructor(
    private val database: HocaLingoDatabase,
    private val jsonLoader: JsonLoader,
    private val userRepository: UserRepository,
    private val preferencesManager: UserPreferencesManager
) {

    /**
     * Seed data for a new user
     * This is called after successful authentication
     */
    suspend fun seedNewUser(
        userId: String,
        isAnonymous: Boolean,
        nativeLanguage: String = "tr",
        targetLanguage: String = "en",
        selectedLevel: String = "A1"
    ): Result<SeedingResult> {
        try {
            val result = SeedingResult()

            // 1. Set up user preferences in DataStore
            preferencesManager.setCurrentUserId(userId).fold(
                onSuccess = { result.userPreferencesSet = true },
                onError = { return Result.Error(it) }
            )

            preferencesManager.setAnonymousUser(isAnonymous).fold(
                onSuccess = { result.anonymousStatusSet = true },
                onError = { return Result.Error(it) }
            )

            preferencesManager.setLanguages(nativeLanguage, targetLanguage).fold(
                onSuccess = { result.languagesSet = true },
                onError = { return Result.Error(it) }
            )

            preferencesManager.setCurrentLevel(selectedLevel).fold(
                onSuccess = { result.levelSet = true },
                onError = { return Result.Error(it) }
            )

            // 2. Create user profile in Repository (Room + Firestore)
            userRepository.createUserProfile(nativeLanguage, targetLanguage, selectedLevel).fold(
                onSuccess = { result.userProfileCreated = true },
                onError = { return Result.Error(it) }
            )

            // 3. Load test word data if not already loaded
            // ✅ FIXED: Firebase'den yükleniyor
            jsonLoader.isTestDataLoaded().fold(
                onSuccess = { isLoaded ->
                    if (!isLoaded) {
                        jsonLoader.loadTestWords().fold(
                            onSuccess = { wordsCount ->
                                result.wordsLoaded = true
                                result.wordsCount = wordsCount
                            },
                            onError = { return Result.Error(it) }
                        )
                    } else {
                        result.wordsLoaded = true
                        result.wordsCount = database.conceptDao().getConceptCount()
                    }
                },
                onError = { return Result.Error(it) }
            )

            // 4. Update last login
            userRepository.updateLastLogin().fold(
                onSuccess = { result.lastLoginUpdated = true },
                onError = { /* Non-critical, continue */ }
            )

            return Result.Success(result)

        } catch (e: Exception) {
            return Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Initialize app on first launch (before user authentication)
     *
     * ✅ FIXED: Assets bağımlılığı kaldırıldı
     */
    suspend fun initializeApp(): Result<AppInitResult> {
        try {
            val result = AppInitResult()

            // Check if this is first app launch
            val setupStatus = preferencesManager.getAppSetupStatus()
            setupStatus.fold(
                onSuccess = { status ->
                    result.isFirstLaunch = !status.isUserLoggedIn
                    result.needsOnboarding = !status.isOnboardingCompleted
                    result.needsWordSelection = !status.areWordsSelected
                },
                onError = {
                    result.isFirstLaunch = true
                    result.needsOnboarding = true
                    result.needsWordSelection = true
                }
            )

            // Set app version
            preferencesManager.setAppVersion("1.0.0")

            // ✅ FIXED: Available packages artık Firestore'dan çekilecek
            // Şimdilik hardcoded list kullanıyoruz (UI'da zaten var)
            if (result.isFirstLaunch) {
                // Firebase'den package listesi çekilebilir ama şimdilik gerekli değil
                // Çünkü PackageSelectionViewModel zaten kendi listesini oluşturuyor
                result.packagesScanned = true
                result.availablePackages = listOf(
                    "a1_en_tr_test_v1",
                    "a2_en_tr_v1",
                    "b1_en_tr_v1",
                    "b2_en_tr_v1",
                    "c1_en_tr_v1",
                    "c2_en_tr_v1"
                )
            }

            return Result.Success(result)

        } catch (e: Exception) {
            return Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Complete onboarding setup
     */
    suspend fun completeOnboarding(
        level: String,
        dailyGoal: Int = 20,
        reminderEnabled: Boolean = true,
        reminderHour: Int = 20
    ): Result<Unit> {
        try {
            // Update preferences
            preferencesManager.setCurrentLevel(level)
            preferencesManager.setDailyGoal(dailyGoal)
            preferencesManager.setStudyReminder(reminderEnabled, reminderHour)
            preferencesManager.setOnboardingCompleted(true)

            // Update in repository
            userRepository.completeOnboarding()

            return Result.Success(Unit)

        } catch (e: Exception) {
            return Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Set up word selection after onboarding
     *
     * ✅ FIXED: Firebase sistemine adapte edildi
     */
    suspend fun setupWordSelection(selectedLevel: String): Result<WordSelectionSetup> {
        try {
            val result = WordSelectionSetup()

            // Update selected level
            preferencesManager.setCurrentLevel(selectedLevel)

            // Get words for the selected level
            val concepts = database.conceptDao().getConceptsByLevel(selectedLevel)
            result.availableWords = concepts.size

            // ✅ FIXED: Artık Firebase'den yükleniyor
            if (concepts.isEmpty()) {
                // Kullanıcı henüz paket indirmemiş
                // PackageSelectionScreen'de manuel indirme yapacak
                // Burada otomatik indirme yapmıyoruz
                result.wordsLoaded = 0
                result.availableWords = 0
            } else {
                result.wordsLoaded = concepts.size
                result.availableWords = concepts.size
            }

            return Result.Success(result)

        } catch (e: Exception) {
            return Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Check what data exists and what needs to be set up
     */
    suspend fun getDatabaseStatus(): Result<DatabaseStatus> {
        try {
            val status = DatabaseStatus()

            // Check user setup
            val currentUserId = preferencesManager.getCurrentUserIdOnce()
            status.hasUser = currentUserId != null

            if (currentUserId != null) {
                // Check user preferences
                userRepository.getUserPreferencesOnce().fold(
                    onSuccess = { prefs ->
                        status.hasUserPreferences = prefs != null
                        status.onboardingCompleted = prefs?.onboardingCompleted ?: false
                    },
                    onError = { /* Continue checking other things */ }
                )

                // Check if anonymous
                status.isAnonymousUser = preferencesManager.isAnonymousUser().first()
            }

            // Check word data
            val wordCount = database.conceptDao().getConceptCount()
            status.hasWordData = wordCount > 0
            status.wordCount = wordCount

            // Check packages
            val packages = database.wordPackageDao().getActivePackages()
            status.hasPackages = packages.isNotEmpty()
            status.packageCount = packages.size

            // Check selections
            val selectedCount = database.userSelectionDao().getSelectionCountByStatus(
                SelectionStatus.SELECTED
            )
            status.hasSelectedWords = selectedCount > 0
            status.selectedWordCount = selectedCount

            return Result.Success(status)

        } catch (e: Exception) {
            return Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Clean up all user data (for logout or account deletion)
     */
    suspend fun cleanupUserData(): Result<Unit> {
        try {
            // Clear DataStore preferences
            preferencesManager.clearUserData()

            // Note: We don't clear word data as it can be reused
            // Only clear user-specific selections and progress

            return Result.Success(Unit)

        } catch (e: Exception) {
            return Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Reset everything (for development/testing)
     */
    suspend fun resetEverything(): Result<Unit> {
        try {
            // Clear all preferences
            preferencesManager.clearAllPreferences()

            // Clear all words and packages
            jsonLoader.clearAllWords()

            return Result.Success(Unit)

        } catch (e: Exception) {
            return Result.Error(AppError.Unknown(e))
        }
    }
}

/**
 * Result data classes
 */
data class SeedingResult(
    var userPreferencesSet: Boolean = false,
    var anonymousStatusSet: Boolean = false,
    var languagesSet: Boolean = false,
    var levelSet: Boolean = false,
    var userProfileCreated: Boolean = false,
    var wordsLoaded: Boolean = false,
    var wordsCount: Int = 0,
    var lastLoginUpdated: Boolean = false
) {
    val isSuccessful: Boolean
        get() = userPreferencesSet && userProfileCreated && wordsLoaded

    val summary: String
        get() = "User setup: $isSuccessful, Words loaded: $wordsCount"
}

data class AppInitResult(
    var isFirstLaunch: Boolean = false,
    var needsOnboarding: Boolean = false,
    var needsWordSelection: Boolean = false,
    var packagesScanned: Boolean = false,
    var availablePackages: List<String> = emptyList()
) {
    val summary: String
        get() = "First launch: $isFirstLaunch, Needs onboarding: $needsOnboarding"
}

data class WordSelectionSetup(
    var availableWords: Int = 0,
    var wordsLoaded: Int = 0
) {
    val summary: String
        get() = "Available: $availableWords, Loaded: $wordsLoaded"
}

data class DatabaseStatus(
    var hasUser: Boolean = false,
    var isAnonymousUser: Boolean = false,
    var hasUserPreferences: Boolean = false,
    var onboardingCompleted: Boolean = false,
    var hasWordData: Boolean = false,
    var wordCount: Int = 0,
    var hasPackages: Boolean = false,
    var packageCount: Int = 0,
    var hasSelectedWords: Boolean = false,
    var selectedWordCount: Int = 0
) {
    val readyForStudy: Boolean
        get() = hasUser && hasUserPreferences && hasWordData && hasSelectedWords

    val summary: String
        get() = """
            User: $hasUser (anonymous: $isAnonymousUser)
            Onboarding: $onboardingCompleted
            Words: $wordCount, Packages: $packageCount
            Selected: $selectedWordCount
            Ready: $readyForStudy
        """.trimIndent()
}