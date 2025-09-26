package com.hocalingo.app.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_preferences",
    indices = [Index(value = ["user_id"], unique = true)]
)
data class UserPreferencesEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "native_language")
    val nativeLanguage: String = "tr", // Turkish

    @ColumnInfo(name = "target_language")
    val targetLanguage: String = "en", // English

    @ColumnInfo(name = "current_level")
    val currentLevel: String = "A1",

    @ColumnInfo(name = "daily_goal")
    val dailyGoal: Int = 20, // words per day

    @ColumnInfo(name = "study_reminder_enabled")
    val studyReminderEnabled: Boolean = true,

    @ColumnInfo(name = "study_reminder_hour")
    val studyReminderHour: Int = 20, // 8 PM

    @ColumnInfo(name = "is_premium")
    val isPremium: Boolean = false,

    @ColumnInfo(name = "onboarding_completed")
    val onboardingCompleted: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)