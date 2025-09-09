package com.hocalingo.app.feature.auth.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hocalingo.app.core.common.base.AppError
import com.hocalingo.app.core.common.base.Result
import com.hocalingo.app.core.common.base.toAppError
import com.hocalingo.app.core.database.HocaLingoDatabase
import com.hocalingo.app.core.database.entities.UserPreferencesEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User Repository
 * Manages user data in both Firestore and local Room database
 * Handles user preferences, progress, and profile information
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
     * Get user preferences as Flow
     */
    fun getUserPreferences(): Flow<UserPreferencesEntity?> {
        val userId = getCurrentUserId() ?: return kotlinx.coroutines.flow.flowOf(null)
        return database.userPreferencesDao().getPreferencesFlow(userId)
    }

    /**
     * Get user preferences once
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
     * Create user profile in Firestore and local DB
     */
    suspend fun createUserProfile(
        nativeLanguage: String = "tr",
        targetLanguage: String = "en",
        currentLevel: String = "A1"
    ): Result<UserPreferencesEntity> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error(AppError.Unauthorized)
            val user = firebaseAuth.currentUser ?: return Result.Error(AppError.Unauthorized)

            // Create user preferences
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

            // Save to local database
            database.userPreferencesDao().insertOrUpdatePreferences(preferences)

            // Create user document in Firestore
            val userDoc = hashMapOf(
                "uid" to userId,
                "email" to user.email,
                "displayName" to user.displayName,
                "photoUrl" to user.photoUrl?.toString(),
                "isAnonymous" to user.isAnonymous,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "lastLoginAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(userDoc)
                .await()

            // Save preferences to Firestore
            syncPreferencesToFirestore(preferences)

            Result.Success(preferences)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    /**
     * Update user preferences
     */
    suspend fun updateUserPreferences(preferences: UserPreferencesEntity): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error(AppError.Unauthorized)

            // Update local database
            database.userPreferencesDao().updatePreferences(preferences)

            // Sync to Firestore
            syncPreferencesToFirestore(preferences)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    /**
     * Update daily goal
     */
    suspend fun updateDailyGoal(goal: Int): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error(AppError.Unauthorized)
            database.userPreferencesDao().updateDailyGoal(userId, goal)

            // Update in Firestore
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(USER_PREFERENCES)
                .document("settings")
                .update("dailyGoal", goal)
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    /**
     * Mark onboarding as completed
     */
    suspend fun completeOnboarding(): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error(AppError.Unauthorized)
            database.userPreferencesDao().markOnboardingCompleted(userId)

            // Update in Firestore
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("onboardingCompleted", true)
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    /**
     * Update premium status
     */
    suspend fun updatePremiumStatus(isPremium: Boolean): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error(AppError.Unauthorized)
            database.userPreferencesDao().updatePremiumStatus(userId, isPremium)

            // Update in Firestore
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
     * Delete user account and all data
     */
    suspend fun deleteUserAccount(): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error(AppError.Unauthorized)

            // Delete from Firestore
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .delete()
                .await()

            // Delete from local database (user-specific data)
            // Note: This might need more specific cleanup based on your data model
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
     */
    suspend fun updateLastLogin(): Result<Unit> {
        return try {
            val userId = getCurrentUserId() ?: return Result.Error(AppError.Unauthorized)

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("lastLoginAt", com.google.firebase.firestore.FieldValue.serverTimestamp())
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
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(USER_PREFERENCES)
            .document("settings")
            .set(preferencesMap)
            .await()
    }
}