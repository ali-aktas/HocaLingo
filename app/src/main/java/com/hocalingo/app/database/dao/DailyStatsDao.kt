package com.hocalingo.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hocalingo.app.database.entities.DailyStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStatsDao {

    @Query("SELECT * FROM daily_stats WHERE user_id = :userId ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentStats(userId: String, limit: Int = 30): List<DailyStatsEntity>

    @Query("SELECT * FROM daily_stats WHERE user_id = :userId AND date = :date")
    suspend fun getStatsByDate(userId: String, date: String): DailyStatsEntity?

    @Query("SELECT COUNT(*) FROM daily_stats WHERE user_id = :userId AND goal_achieved = 1 AND date >= :sinceDate")
    suspend fun getStreakCount(userId: String, sinceDate: String): Int

    @Query("SELECT SUM(words_studied) FROM daily_stats WHERE user_id = :userId")
    suspend fun getTotalWordsStudied(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStats(stats: DailyStatsEntity)

    @Update
    suspend fun updateStats(stats: DailyStatsEntity)

    @Query("DELETE FROM daily_stats WHERE date < :cutoffDate")
    suspend fun deleteOldStats(cutoffDate: String)

    @Query("SELECT * FROM daily_stats WHERE user_id = :userId ORDER BY date DESC LIMIT 1")
    fun getTodayStatsFlow(userId: String): Flow<DailyStatsEntity?>
}