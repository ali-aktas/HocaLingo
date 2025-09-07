package com.hocalingo.app.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "daily_stats",
    primaryKeys = ["date", "user_id"],
    indices = [Index(value = ["date"])]
)
data class DailyStatsEntity(
    @ColumnInfo(name = "date")
    val date: String, // YYYY-MM-DD format

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "words_studied")
    val wordsStudied: Int = 0,

    @ColumnInfo(name = "correct_answers")
    val correctAnswers: Int = 0,

    @ColumnInfo(name = "total_answers")
    val totalAnswers: Int = 0,

    @ColumnInfo(name = "study_time_ms")
    val studyTimeMs: Long = 0,

    @ColumnInfo(name = "streak_count")
    val streakCount: Int = 0,

    @ColumnInfo(name = "goal_achieved")
    val goalAchieved: Boolean = false
)