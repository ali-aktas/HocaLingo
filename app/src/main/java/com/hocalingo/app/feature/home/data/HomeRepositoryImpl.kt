package com.hocalingo.app.feature.home.data

import com.hocalingo.app.core.common.base.Result
import com.hocalingo.app.core.common.base.toAppError
import com.hocalingo.app.core.database.dao.DailyStatsDao
import com.hocalingo.app.core.database.dao.StudySessionDao
import com.hocalingo.app.core.database.dao.CombinedDataDao
import com.hocalingo.app.core.database.entities.StudyDirection
import com.hocalingo.app.feature.home.domain.HomeRepository
import com.hocalingo.app.feature.home.presentation.ChartDataPoint
import com.hocalingo.app.feature.home.presentation.DailyGoalProgress
import com.hocalingo.app.feature.home.presentation.MonthlyStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Home Repository Implementation - v2.0
 * Real data integration ile güncellendi
 */
@Singleton
class HomeRepositoryImpl @Inject constructor(
    private val dailyStatsDao: DailyStatsDao,
    private val studySessionDao: StudySessionDao,
    private val combinedDataDao: CombinedDataDao
) : HomeRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override suspend fun getUserName(): Result<String> = withContext(Dispatchers.IO) {
        try {
            // TODO: User preferences'tan alınacak
            Result.Success("Ali")
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    override suspend fun getStreakDays(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val today = Calendar.getInstance()
            var streakCount = 0

            // Bugünden geriye doğru kontrol et
            for (i in 0 until 365) { // Max 365 gün kontrol
                val checkDate = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -i)
                }
                val dateString = dateFormat.format(checkDate.time)

                val dailyStats = dailyStatsDao.getStatsByDate("user_1", dateString)

                if (dailyStats != null && dailyStats.wordsStudied > 0) {
                    streakCount++
                } else {
                    break // Streak kırıldı
                }
            }

            Result.Success(streakCount)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    override suspend fun getDailyGoalProgress(): Result<DailyGoalProgress> = withContext(Dispatchers.IO) {
        try {
            val direction = StudyDirection.EN_TO_TR // Default direction

            // Safe data fetch with fallbacks
            val totalDeckCards = try {
                combinedDataDao.getTotalSelectedWordsCount()
            } catch (e: Exception) {
                0
            }

            val masteredDeckCards = try {
                combinedDataDao.getMasteredWordsCount(direction)
            } catch (e: Exception) {
                0
            }

            // Today available cards - basit hesaplama
            val todayAvailableCards = maxOf(0, totalDeckCards - masteredDeckCards)

            // Today completed cards - şimdilik mock, gerçek implementasyon sonra
            val todayCompletedCards = 0

            val progress = DailyGoalProgress(
                todayAvailableCards = todayAvailableCards,
                todayCompletedCards = todayCompletedCards,
                totalDeckCards = totalDeckCards,
                masteredDeckCards = masteredDeckCards
            )

            Result.Success(progress)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    override suspend fun getMonthlyStats(): Result<MonthlyStats> = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            val startOfMonth = getStartOfMonth(currentTime)
            val daysInMonth = getDaysInCurrentMonth()

            // Bu ay çalışma süresini hesapla (ms → dakika)
            val totalStudyTimeMs = studySessionDao.getTotalStudyTimeSince(startOfMonth)
            val studyTimeMinutes = (totalStudyTimeMs / (1000 * 60)).toInt()

            // Bu ay aktif günleri hesapla
            val activeDaysThisMonth = getActiveDaysInMonth(startOfMonth, currentTime)

            // Disiplin puanını hesapla (0-100)
            val disciplineScore = if (daysInMonth > 0) {
                ((activeDaysThisMonth.toFloat() / daysInMonth) * 100).roundToInt()
            } else 0

            // Chart data (son 7 gün için disiplin trend'i)
            val chartData = getChartData()

            val monthlyStats = MonthlyStats(
                studyTimeMinutes = studyTimeMinutes,
                activeDaysThisMonth = activeDaysThisMonth,
                disciplineScore = disciplineScore,
                chartData = chartData
            )

            Result.Success(monthlyStats)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    // Private helper methods

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun getEndOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    private fun getStartOfMonth(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun getDaysInCurrentMonth(): Int {
        val calendar = Calendar.getInstance()
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private suspend fun getActiveDaysInMonth(startOfMonth: Long, currentTime: Long): Int {
        return try {
            val dailyStatsList = dailyStatsDao.getRecentStats("user_1", 31)

            dailyStatsList.count { stats ->
                val statsDate = dateFormat.parse(stats.date)?.time ?: 0L
                statsDate >= startOfMonth && statsDate <= currentTime && stats.wordsStudied > 0
            }
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun getChartData(): List<ChartDataPoint> {
        return try {
            val chartData = mutableListOf<ChartDataPoint>()
            val today = Calendar.getInstance()

            // Son 7 gün için günlük aktivite verisi
            for (i in 6 downTo 0) {
                val targetDate = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -i)
                }
                val dateString = dateFormat.format(targetDate.time)
                val dayOfMonth = targetDate.get(Calendar.DAY_OF_MONTH)

                val dailyStats = dailyStatsDao.getStatsByDate("user_1", dateString)
                val hasActivity = dailyStats?.wordsStudied ?: 0 > 0

                chartData.add(
                    ChartDataPoint(
                        day = dayOfMonth.toString(),
                        value = if (hasActivity) 1f else 0f
                    )
                )
            }

            chartData
        } catch (e: Exception) {
            // Fallback mock data
            listOf(
                ChartDataPoint("15", 0.8f),
                ChartDataPoint("16", 0.9f),
                ChartDataPoint("17", 0.6f),
                ChartDataPoint("18", 1.0f),
                ChartDataPoint("19", 0.7f),
                ChartDataPoint("20", 0.9f),
                ChartDataPoint("21", 0.8f)
            )
        }
    }
}