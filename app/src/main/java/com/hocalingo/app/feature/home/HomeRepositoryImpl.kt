package com.hocalingo.app.feature.home

import com.hocalingo.app.core.base.Result
import com.hocalingo.app.core.base.toAppError
import com.hocalingo.app.database.dao.CombinedDataDao
import com.hocalingo.app.database.dao.DailyStatsDao
import com.hocalingo.app.database.dao.StudySessionDao
import com.hocalingo.app.database.entities.DailyStatsEntity
import com.hocalingo.app.database.entities.StudyDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Home Repository Implementation - v2.1
 * ✅ App launch tracking eklendi (streak için)
 * ✅ Real daily progress calculation eklendi
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

    /**
     * ✅ NEW: App launch tracking için DailyStatsEntity oluştur/güncelle
     */
    override suspend fun trackAppLaunch(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val today = Calendar.getInstance()
            val todayString = dateFormat.format(today.time)
            val userId = "user_1" // TODO: Real user ID

            // Bugünün kaydını kontrol et
            val existingStats = dailyStatsDao.getStatsByDate(userId, todayString)

            if (existingStats == null) {
                // Bugün ilk kez açılıyor - yeni kayıt oluştur
                val newStats = DailyStatsEntity(
                    date = todayString,
                    userId = userId,
                    wordsStudied = 0,
                    correctAnswers = 0,
                    totalAnswers = 0,
                    studyTimeMs = 0,
                    streakCount = calculateStreakForNewDay(todayString),
                    goalAchieved = false
                )

                dailyStatsDao.insertOrUpdateStats(newStats)
            }
            // Eğer kayıt varsa, zaten bugün giriş yapılmış - hiçbir şey yapma

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    /**
     * Yeni gün için streak hesapla
     */
    private suspend fun calculateStreakForNewDay(todayString: String): Int {
        return try {
            val userId = "user_1"
            val yesterday = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }
            val yesterdayString = dateFormat.format(yesterday.time)

            // Dünün kaydını kontrol et
            val yesterdayStats = dailyStatsDao.getStatsByDate(userId, yesterdayString)

            if (yesterdayStats != null) {
                // Dün giriş yapılmış - streak devam ediyor
                yesterdayStats.streakCount + 1
            } else {
                // Dün giriş yapılmamış - streak sıfırlanıyor
                1 // Bugün yeni streak başlıyor
            }
        } catch (e: Exception) {
            1 // Hata durumunda 1 döndür
        }
    }

    override suspend fun getStreakDays(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val today = Calendar.getInstance()
            val todayString = dateFormat.format(today.time)
            val userId = "user_1"

            // Bugünün kaydından streak al
            val todayStats = dailyStatsDao.getStatsByDate(userId, todayString)
            val streakCount = todayStats?.streakCount ?: 0

            Result.Success(streakCount)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    override suspend fun getDailyGoalProgress(): Result<DailyGoalProgress> =
        withContext(Dispatchers.IO) {
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

                // ✅ FIXED: Real today completed cards calculation
                val todayCompletedCards = try {
                    val today = System.currentTimeMillis()
                    val startOfDay = getStartOfDay(today)
                    val endOfDay = getEndOfDay(today)

                    // Bugün graduated olan kartları say (learning -> review phase)
                    combinedDataDao.getGraduatedWordsToday(
                        StudyDirection.EN_TO_TR,
                        startOfDay,
                        endOfDay
                    ) +
                            combinedDataDao.getGraduatedWordsToday(
                                StudyDirection.TR_TO_EN,
                                startOfDay,
                                endOfDay
                            )
                } catch (e: Exception) {
                    0
                }

                // Today available cards - destede kalan öğrenilmemiş kartlar
                val todayAvailableCards = maxOf(0, totalDeckCards - masteredDeckCards)

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

            // Bu ay aktif günleri hesapla (DailyStatsEntity'den)
            val activeDaysThisMonth = getActiveDaysInMonth(startOfMonth, currentTime)

            // Disiplin puanını hesapla (0-100)
            val disciplineScore = if (daysInMonth > 0) {
                ((activeDaysThisMonth.toFloat() / daysInMonth) * 100).roundToInt()
            } else 0

            // Chart data (son 7 gün için aktivite trend'i)
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
            val userId = "user_1"
            val endOfMonth = Calendar.getInstance().apply {
                timeInMillis = currentTime
                set(Calendar.DAY_OF_MONTH, getDaysInCurrentMonth())
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis

            // Bu ay için tarih aralığı oluştur ve active days say
            val startDateString = dateFormat.format(Date(startOfMonth))
            val endDateString = dateFormat.format(Date(minOf(currentTime, endOfMonth)))

            // DailyStatsEntity'den bu ay active günleri say
            val dailyStatsList = dailyStatsDao.getRecentStats(userId, 31)

            dailyStatsList.count { stats ->
                val statsDate = dateFormat.parse(stats.date)?.time ?: 0L
                statsDate >= startOfMonth && statsDate <= currentTime
            }
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun getChartData(): List<ChartDataPoint> {
        return try {
            val chartData = mutableListOf<ChartDataPoint>()
            val userId = "user_1"

            // Son 7 gün için günlük aktivite verisi
            for (i in 6 downTo 0) {
                val targetDate = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -i)
                }
                val dateString = dateFormat.format(targetDate.time)
                val dayOfMonth = targetDate.get(Calendar.DAY_OF_MONTH)

                val dailyStats = dailyStatsDao.getStatsByDate(userId, dateString)
                val hasActivity = dailyStats != null

                chartData.add(
                    ChartDataPoint(
                        day = dayOfMonth.toString(),
                        value = if (hasActivity) 1.0f else 0.0f
                    )
                )
            }

            chartData
        } catch (e: Exception) {
            emptyList()
        }
    }
}