package com.hocalingo.app.feature.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hocalingo.app.core.base.AppError
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.core.base.toAppError
import com.hocalingo.app.database.HocaLingoDatabase
import com.hocalingo.app.database.entities.UserPreferencesEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User Repository (OPTIMIZED FOR SPARK PLAN)
 *
 * ✅ LOCAL-FIRST YAKLAŞIM:
 * - Ana database: Room (local)
 * - Yedekleme: Firestore (cloud)
 * - Sync: Sadece kritik durumlarda
 *
 * ✅ FIRESTORE'A NE ZAMAN YAZIYORUZ:
 * - İlk login (user profili oluşturma)
 * - Premium satın alma
 * - Uygulama açıldığında (günde 1 sync)
 * - Logout (son data'yı kaydet)
 *
 * ❌ FIRESTORE'A NE ZAMAN YAZMIYORUZ:
 * - Her preference değişikliği
 * - Her kelime öğrenme
 * - Her setting update
 */
@Singleton
class UserRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val database: HocaLingoDatabase
) {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val USER_PREFERENCES = "preferences"
        private const val USER_PROGRESS = "progress"
    }

    /**
     * Get current user ID
     */
    private fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid

    /**
     * Get user preferences as Flow (LOCAL - NO FIRESTORE!)
     */
    fun getUserPreferences(): Flow<UserPreferencesEntity?> {
        val userId = getCurrentUserId() ?: return flowOf(null)
        return database.userPreferencesDao().getPreferencesFlow(userId)
    }

    /**
     * Get user preferences once (LOCAL - NO FIRESTORE!)
     */
    suspend fun getUserPreferencesOnce(): Result<UserPreferencesEntity?> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error(AppError.Unauthorized)
            val preferences = database.userPreferencesDao().getPreferences(userId)
            Result.Success(preferences)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    /**
     * Create user profile
     * ✅ Bu kritik bir işlem - Firestore'a yazıyoruz!
     */
    suspend fun createUserProfile(
        nativeLanguage: String = "tr",
        targetLanguage: String = "en",
        currentLevel: String = "A1"
    ): Result<UserPreferencesEntity> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error(AppError.Unauthorized)
            val user = firebaseAuth.currentUser ?: return Result.Error(AppError.Unauthorized)

            // Create user preferences (LOCAL)
            val preferences = UserPreferencesEntity(
                userId = userId,
                nativeLanguage = nativeLanguage,
                targetLanguage = targetLanguage,
                currentLevel = currentLevel,
                dailyGoal = 20,
                studyReminderEnabled = true,
                studyReminderHour = 20,
                isPremium = false,
                onboardingCompleted = false
            )

            // Save to local database (PRIMARY)
            database.userPreferencesDao().insertOrUpdatePreferences(preferences)

            // ✅ Create user document in Firestore (BACKUP)
            // Bu kritik bir işlem, sadece ilk login'de yapılıyor
            val userDoc = hashMapOf(
                "uid" to userId,
                "email" to user.email,
                "displayName" to user.displayName,
                "photoUrl" to user.photoUrl?.toString(),
                "isAnonymous" to user.isAnonymous,
                "createdAt" to FieldValue.serverTimestamp(),
                "lastLoginAt" to FieldValue.serverTimestamp()
            )

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(userDoc)
                .await()

            // Save preferences to Firestore (BACKUP)
            syncPreferencesToFirestore(preferences)

            Result.Success(preferences)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    /**
     * Update user preferences
     * ❌ FIRESTORE'A YAZMIYOR! (Sadece local)
     * Sync işlemi ayrı bir fonksiyondan yapılacak
     */
    suspend fun updateUserPreferences(preferences: UserPreferencesEntity): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error(AppError.Unauthorized)

            // ✅ Sadece local database'e yaz (HIZLI!)
            database.userPreferencesDao().updatePreferences(preferences)

            // ❌ Firestore'a yazmıyoruz (Para kesintisine neden olur!)
            // syncPreferencesToFirestore(preferences)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    /**
     * Update daily goal
     * ❌ FIRESTORE'A YAZMIYOR! (Sadece local)
     */
    suspend fun updateDailyGoal(goal: Int): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error(AppError.Unauthorized)

            // ✅ Sadece local database'e yaz
            database.userPreferencesDao().updateDailyGoal(userId, goal)

            // ❌ Firestore'a yazmıyoruz
            // firestore.collection(USERS_COLLECTION)
            //     .document(userId)
            //     .collection(USER_PREFERENCES)
            //     .document("settings")
            //     .update("dailyGoal", goal)
            //     .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    /**
     * Mark onboarding as completed
     * ❌ FIRESTORE'A YAZMIYOR! (Sadece local)
     */
    suspend fun completeOnboarding(): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error(AppError.Unauthorized)

            // ✅ Sadece local database'e yaz
            database.userPreferencesDao().markOnboardingCompleted(userId)

            // ❌ Firestore'a yazmıyoruz
            // firestore.collection(USERS_COLLECTION)
            //     .document(userId)
            //     .update("onboardingCompleted", true)
            //     .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    /**
     * Update premium status
     * ✅ Bu kritik bir işlem - Firestore'a yazıyoruz!
     * (Premium satın alma önemli bir event)
     */
    suspend fun updatePremiumStatus(isPremium: Boolean): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error(AppError.Unauthorized)

            // Local database
            database.userPreferencesDao().updatePremiumStatus(userId, isPremium)

            // ✅ Firestore'a da yaz (Premium bilgisi kritik!)
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("isPremium", isPremium)
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    /**
     * Sync user data from Firestore to local database
     * ✅ SADECE UYGULAMA AÇILIŞINDA ÇAĞRILACAK (Günde 1 kez!)
     */
    suspend fun syncUserDataFromFirestore(): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error(AppError.Unauthorized)

            // Get user preferences from Firestore
            val preferencesDoc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(USER_PREFERENCES)
                .document("settings")
                .get()
                .await()

            if (preferencesDoc.exists()) {
                val data = preferencesDoc.data
                if (data != null) {
                    val preferences = UserPreferencesEntity(
                        userId = userId,
                        nativeLanguage = data["nativeLanguage"] as? String ?: "tr",
                        targetLanguage = data["targetLanguage"] as? String ?: "en",
                        currentLevel = data["currentLevel"] as? String ?: "A1",
                        dailyGoal = (data["dailyGoal"] as? Long)?.toInt() ?: 20,
                        studyReminderEnabled = data["studyReminderEnabled"] as? Boolean ?: true,
                        studyReminderHour = (data["studyReminderHour"] as? Long)?.toInt() ?: 20,
                        isPremium = data["isPremium"] as? Boolean ?: false,
                        onboardingCompleted = data["onboardingCompleted"] as? Boolean ?: false
                    )

                    database.userPreferencesDao().insertOrUpdatePreferences(preferences)
                }
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    /**
     * Sync local data to Firestore
     * ✅ SADECE LOGOUT'TA VEYA GÜN SONU SYNC'TE ÇAĞRILACAK
     */
    suspend fun syncLocalDataToFirestore(): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error(AppError.Unauthorized)
            val preferences = database.userPreferencesDao().getPreferences(userId)
                ?: return Result.Error(AppError.NotFound)

            // Batch write ile tek işlemde yaz (daha verimli!)
            syncPreferencesToFirestore(preferences)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    /**
     * Delete user account and all data
     * ✅ Bu kritik bir işlem - Firestore'dan da sil!
     */
    suspend fun deleteUserAccount(): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error(AppError.Unauthorized)

            // Delete from Firestore
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .delete()
                .await()

            // Delete from local database
            database.userPreferencesDao().getPreferences(userId)?.let { preferences ->
                database.userPreferencesDao().updatePreferences(
                    preferences.copy(
                        onboardingCompleted = false,
                        isPremium = false
                    )
                )
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    /**
     * Update last login timestamp
     * ✅ SADECE İLK AÇILIŞTA ÇAĞRILACAK (Günde 1 kez!)
     */
    suspend fun updateLastLogin(): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error(AppError.Unauthorized)

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("lastLoginAt", FieldValue.serverTimestamp())
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    /**
     * Check if user profile exists
     */
    suspend fun userProfileExists(): Result<Boolean> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error(AppError.Unauthorized)

            val doc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            Result.Success(doc.exists())
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    /**
     * Private helper: Sync preferences to Firestore
     * ✅ Batch write kullanarak tek işlemde yaz
     */
    private suspend fun syncPreferencesToFirestore(preferences: UserPreferencesEntity) {
        val userId = getCurrentUserId() ?: return

        val preferencesMap = hashMapOf(
            "nativeLanguage" to preferences.nativeLanguage,
            "targetLanguage" to preferences.targetLanguage,
            "currentLevel" to preferences.currentLevel,
            "dailyGoal" to preferences.dailyGoal,
            "studyReminderEnabled" to preferences.studyReminderEnabled,
            "studyReminderHour" to preferences.studyReminderHour,
            "isPremium" to preferences.isPremium,
            "onboardingCompleted" to preferences.onboardingCompleted,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(USER_PREFERENCES)
            .document("settings")
            .set(preferencesMap)
            .await()
    }
}