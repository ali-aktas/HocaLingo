package com.hocalingo.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hocalingo.app.database.entities.UserPreferencesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {

    @Query("SELECT * FROM user_preferences WHERE user_id = :userId")
    suspend fun getPreferences(userId: String): UserPreferencesEntity?

    @Query("SELECT * FROM user_preferences WHERE user_id = :userId")
    fun getPreferencesFlow(userId: String): Flow<UserPreferencesEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePreferences(preferences: UserPreferencesEntity)

    @Update
    suspend fun updatePreferences(preferences: UserPreferencesEntity)

    @Query("UPDATE user_preferences SET is_premium = :isPremium WHERE user_id = :userId")
    suspend fun updatePremiumStatus(userId: String, isPremium: Boolean)

    @Query("UPDATE user_preferences SET onboarding_completed = 1 WHERE user_id = :userId")
    suspend fun markOnboardingCompleted(userId: String)

    @Query("UPDATE user_preferences SET daily_goal = :goal WHERE user_id = :userId")
    suspend fun updateDailyGoal(userId: String, goal: Int)
}