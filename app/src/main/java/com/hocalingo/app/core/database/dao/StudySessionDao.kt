package com.hocalingo.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hocalingo.app.core.database.entities.StudySessionEntity

@Dao
interface StudySessionDao {

    @Query("SELECT * FROM study_sessions ORDER BY started_at DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int = 10): List<StudySessionEntity>

    @Query("SELECT * FROM study_sessions WHERE started_at >= :startTime AND started_at <= :endTime")
    suspend fun getSessionsBetweenDates(startTime: Long, endTime: Long): List<StudySessionEntity>

    @Query("SELECT SUM(words_studied) FROM study_sessions WHERE started_at >= :startTime")
    suspend fun getTotalWordsStudiedSince(startTime: Long): Int

    @Query("SELECT SUM(total_duration_ms) FROM study_sessions WHERE started_at >= :startTime")
    suspend fun getTotalStudyTimeSince(startTime: Long): Long

    @Query("SELECT AVG(CAST(correct_answers AS FLOAT) / CAST(words_studied AS FLOAT)) FROM study_sessions WHERE started_at >= :startTime AND words_studied > 0")
    suspend fun getAverageAccuracySince(startTime: Long): Float

    @Insert
    suspend fun insertSession(session: StudySessionEntity): Long

    @Update
    suspend fun updateSession(session: StudySessionEntity)

    @Delete
    suspend fun deleteSession(session: StudySessionEntity)
}