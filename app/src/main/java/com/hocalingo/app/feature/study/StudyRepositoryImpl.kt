package com.hocalingo.app.feature.study

import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.core.common.SpacedRepetitionAlgorithm
import com.hocalingo.app.core.common.UserPreferencesManager
import com.hocalingo.app.core.base.AppError
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.database.HocaLingoDatabase
import com.hocalingo.app.database.dao.ConceptWithTimingData
import com.hocalingo.app.database.entities.ConceptEntity
import com.hocalingo.app.database.entities.DailyStatsEntity
import com.hocalingo.app.database.entities.SessionType
import com.hocalingo.app.database.entities.StudyDirection
import com.hocalingo.app.database.entities.StudySessionEntity
import com.hocalingo.app.database.entities.WordProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of StudyRepository - FIXED VERSION v2.1
 *
 * FIXES APPLIED:
 * ✅ Enhanced updateWordProgress() with detailed debug logging
 * ✅ Fixed daily progress logic - only 1+ day intervals count
 * ✅ Added comprehensive error handling
 * ✅ Improved SM-2 algorithm integration
 * ✅ Real DailyStatsEntity update in incrementDailyProgress()
 *
 * Handles all study-related data operations using DAO layer
 * with real daily progress tracking
 */
@Singleton
class StudyRepositoryImpl @Inject constructor(
    private val database: HocaLingoDatabase,
    private val preferencesManager: UserPreferencesManager
) : StudyRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun getStudyQueue(
        direction: StudyDirection,
        limit: Int
    ): Flow<List<ConceptWithTimingData>> = flow {
        try {
            val currentTime = System.currentTimeMillis()

            // Call DAO suspend function
            val studyQueue = database.combinedDataDao().getStudyQueue(direction, currentTime, limit)

            DebugHelper.log("📚 Study queue loaded: ${studyQueue.size} words for direction $direction")

            // Sort by priority using SM-2 algorithm
            val sortedQueue = studyQueue.sortedByDescending { conceptData ->
                val mockProgress = WordProgressEntity(
                    conceptId = conceptData.id,
                    direction = direction,
                    repetitions = conceptData.repetitions,
                    intervalDays = 0f,
                    easeFactor = 2.5f,
                    nextReviewAt = conceptData.nextReviewAt,
                    lastReviewAt = null,
                    isSelected = true,
                    isMastered = false
                )
                SpacedRepetitionAlgorithm.getStudyPriority(mockProgress, currentTime)
            }

            emit(sortedQueue)

        } catch (e: Exception) {
            DebugHelper.logError("Study queue flow error", e)
            emit(emptyList())
        }
    }.catch { e ->
        DebugHelper.logError("Study queue flow catch", e)
        emit(emptyList())
    }

    override suspend fun getCurrentProgress(
        conceptId: Int,
        direction: StudyDirection
    ): Result<WordProgressEntity?> = try {
        val progress = database.wordProgressDao().getProgressByConceptAndDirection(conceptId, direction)
        DebugHelper.log("📊 Current progress for concept $conceptId: ${progress?.let { "reps=${it.repetitions}, interval=${it.intervalDays}" } ?: "null"}")
        Result.Success(progress)
    } catch (e: Exception) {
        DebugHelper.logError("Get current progress error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun getConceptById(conceptId: Int): Result<ConceptEntity?> = try {
        val concept = database.conceptDao().getConceptById(conceptId)
        Result.Success(concept)
    } catch (e: Exception) {
        DebugHelper.logError("Get concept by id error", e)
        Result.Error(AppError.Unknown(e))
    }

    /**
     * Update word progress after user response - HYBRID VERSION
     *
     * MAJOR CHANGES:
     * ✅ Uses hybrid algorithm (learning + review phases)
     * ✅ Manages session positions for learning cards
     * ✅ Daily progress only counts graduated cards
     * ✅ Session never becomes empty (learning cards cycle)
     */
    override suspend fun updateWordProgress(
        conceptId: Int,
        direction: StudyDirection,
        quality: Int
    ): Result<WordProgressEntity> = try {

        DebugHelper.log("🔄 HYBRID UPDATE: conceptId=$conceptId, direction=$direction, quality=$quality")

        // Get current progress or create new
        val currentProgress = database.wordProgressDao().getProgressByConceptAndDirection(conceptId, direction)
            ?: createDefaultHybridProgress(conceptId, direction)

        DebugHelper.log("📊 Current: phase=${if(currentProgress.learningPhase) "LEARNING" else "REVIEW"}, reps=${currentProgress.repetitions}, sessionPos=${currentProgress.sessionPosition}")

        // Get current max session position for learning cards
        val maxSessionPosition = database.combinedDataDao().getMaxSessionPosition(direction)

        // Calculate new progress using HYBRID SM-2
        val newProgress = SpacedRepetitionAlgorithm.calculateNextReview(
            currentProgress,
            quality,
            maxSessionPosition
        )

        val timeText = SpacedRepetitionAlgorithm.getTimeUntilReview(newProgress.nextReviewAt)
        val newPhaseText = if (newProgress.learningPhase) "LEARNING" else "REVIEW"

        DebugHelper.log("📊 New: phase=$newPhaseText, reps=${newProgress.repetitions}, sessionPos=${newProgress.sessionPosition}")
        DebugHelper.log("⏰ Next review: $timeText")

        // Update in database
        database.wordProgressDao().insertProgress(newProgress)
        DebugHelper.log("💾 Progress saved to database")

        // HYBRID DAILY PROGRESS LOGIC: Only graduated cards count
        val wasInLearning = currentProgress.learningPhase
        val isNowInReview = !newProgress.learningPhase
        val hasGraduated = wasInLearning && isNowInReview

        DebugHelper.log("📈 Graduation check:")
        DebugHelper.log("   Was in learning: $wasInLearning")
        DebugHelper.log("   Is now in review: $isNowInReview")
        DebugHelper.log("   Has graduated: $hasGraduated")

        if (hasGraduated) {
            // Card graduated from learning to review phase = daily progress!
            incrementDailyProgress()
            DebugHelper.log("🎓 Card GRADUATED! Daily progress incremented")
        } else if (newProgress.learningPhase) {
            DebugHelper.log("📝 Card stays in learning phase, no daily progress")
        } else {
            DebugHelper.log("📝 Review card updated, no daily progress change")
        }

        // Log quality interpretation
        val qualityText = when (quality) {
            SpacedRepetitionAlgorithm.QUALITY_HARD -> "HARD (Don't Know)"
            SpacedRepetitionAlgorithm.QUALITY_MEDIUM -> "MEDIUM (Orta)"
            SpacedRepetitionAlgorithm.QUALITY_EASY -> "EASY (Kolay)"
            else -> "UNKNOWN ($quality)"
        }
        DebugHelper.log("✅ HYBRID update completed for $qualityText response")

        Result.Success(newProgress)

    } catch (e: Exception) {
        DebugHelper.logError("❌ Update word progress error for concept $conceptId", e)
        Result.Error(AppError.Unknown(e))
    }

    /**
     * Create default progress for new word - HYBRID VERSION
     * New words start in learning phase
     */
    private suspend fun createDefaultHybridProgress(
        conceptId: Int,
        direction: StudyDirection
    ): WordProgressEntity {
        val currentTime = System.currentTimeMillis()

        // Get next session position
        val maxPosition = database.combinedDataDao().getMaxSessionPosition(direction)
        val sessionPosition = maxPosition + 1

        DebugHelper.log("🆕 Creating new learning card: conceptId=$conceptId, sessionPosition=$sessionPosition")

        return WordProgressEntity(
            conceptId = conceptId,
            direction = direction,
            repetitions = 0,
            intervalDays = 0f,
            easeFactor = 2.5f,
            nextReviewAt = currentTime,
            lastReviewAt = null,
            isSelected = true,
            isMastered = false,
            learningPhase = true,
            sessionPosition = sessionPosition,
            createdAt = currentTime,
            updatedAt = currentTime
        )
    }

    override suspend fun hasWordsToStudy(direction: StudyDirection): Result<Boolean> = try {
        val currentTime = System.currentTimeMillis()
        val learningCount = database.combinedDataDao().getLearningCardsCount(direction)
        val overdueCount = database.combinedDataDao().getOverdueWordsCount(direction, currentTime)
        val hasWords = (learningCount + overdueCount) > 0
        DebugHelper.log("📚 Has words to study for $direction: $hasWords")
        Result.Success(hasWords)
    } catch (e: Exception) {
        DebugHelper.logError("Check words to study error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun getLearningCardsCount(direction: StudyDirection): Result<Int> = try {
        val count = database.combinedDataDao().getLearningCardsCount(direction)
        DebugHelper.log("📊 Learning cards count for $direction: $count")
        Result.Success(count)
    } catch (e: Exception) {
        DebugHelper.logError("Get learning cards count error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun startStudySession(sessionType: SessionType): Result<Long> = try {
        val sessionEntity = StudySessionEntity(
            id = 0,
            sessionType = sessionType,
            startedAt = System.currentTimeMillis(),
            endedAt = null,
            wordsStudied = 0,
            correctAnswers = 0,
            totalDurationMs = 0
        )

        val sessionId = database.studySessionDao().insertSession(sessionEntity)
        DebugHelper.log("📚 Study session started: $sessionId")
        Result.Success(sessionId)

    } catch (e: Exception) {
        DebugHelper.logError("Start study session error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun endStudySession(
        sessionId: Long,
        wordsStudied: Int,
        correctAnswers: Int
    ): Result<Unit> = try {
        val currentTime = System.currentTimeMillis()

        // Get session to calculate duration
        val sessions = database.studySessionDao().getRecentSessions(1)
        val session = sessions.firstOrNull { it.id == sessionId }

        if (session != null) {
            val updatedSession = session.copy(
                endedAt = currentTime,
                wordsStudied = wordsStudied,
                correctAnswers = correctAnswers,
                totalDurationMs = currentTime - session.startedAt
            )
            database.studySessionDao().updateSession(updatedSession)
            DebugHelper.log("📚 Study session ended: $sessionId, words: $wordsStudied")
        }

        Result.Success(Unit)

    } catch (e: Exception) {
        DebugHelper.logError("End study session error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun getTodayWordsStudied(): Result<Int> = try {
        val today = System.currentTimeMillis()
        val startOfDay = today - (today % (24 * 60 * 60 * 1000))
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000)

        // Count words that were reviewed today
        val wordsStudied = database.wordProgressDao()
            .getWordsCompletedToday(startOfDay, endOfDay)

        DebugHelper.log("📊 Words studied today: $wordsStudied")
        Result.Success(wordsStudied)

    } catch (e: Exception) {
        DebugHelper.logError("Get today words studied error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun getDailyCompletedWords(): Result<Int> = try {
        val today = System.currentTimeMillis()
        val startOfDay = today - (today % (24 * 60 * 60 * 1000))
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000)

        // HYBRID: Count words that graduated from learning to review phase today
        val graduatedCount = database.combinedDataDao()
            .getGraduatedWordsToday(StudyDirection.EN_TO_TR, startOfDay, endOfDay) +
                database.combinedDataDao()
                    .getGraduatedWordsToday(StudyDirection.TR_TO_EN, startOfDay, endOfDay)

        DebugHelper.log("📊 HYBRID daily completed (graduated): $graduatedCount")
        Result.Success(graduatedCount)

    } catch (e: Exception) {
        DebugHelper.logError("Get daily completed words error", e)
        Result.Error(AppError.Unknown(e))
    }

    /**
     * ✅ FIXED: Real DailyStatsEntity update when card graduates
     */
    override suspend fun incrementDailyProgress(): Result<Unit> = try {
        val today = Calendar.getInstance()
        val todayString = dateFormat.format(today.time)
        val userId = "user_1" // TODO: Real user ID

        // Bugünün kaydını al veya oluştur
        val existingStats = database.dailyStatsDao().getStatsByDate(userId, todayString)

        if (existingStats != null) {
            // Mevcut kaydı güncelle - wordsStudied +1
            val updatedStats = existingStats.copy(
                wordsStudied = existingStats.wordsStudied + 1,
                // Hedef 20 kart, tamamlanma durumunu kontrol et
                goalAchieved = (existingStats.wordsStudied + 1) >= 20
            )
            database.dailyStatsDao().insertOrUpdateStats(updatedStats)
            DebugHelper.log("📈 Daily progress updated: ${updatedStats.wordsStudied} words studied today")
        } else {
            // Bugün için kayıt yok, yeni oluştur
            val newStats = DailyStatsEntity(
                date = todayString,
                userId = userId,
                wordsStudied = 1,
                correctAnswers = 0,
                totalAnswers = 0,
                studyTimeMs = 0,
                streakCount = 1, // Bu değer app launch'ta güncellenir
                goalAchieved = false // 1 >= 20 değil
            )
            database.dailyStatsDao().insertOrUpdateStats(newStats)
            DebugHelper.log("📈 Daily progress created: 1 word studied today")
        }

        Result.Success(Unit)

    } catch (e: Exception) {
        DebugHelper.logError("Increment daily progress error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun getDailyGoal(): Result<Int> = try {
        val dailyGoal = preferencesManager.getDailyGoal().first()
        DebugHelper.log("🎯 Daily goal: $dailyGoal")
        Result.Success(dailyGoal)
    } catch (e: Exception) {
        DebugHelper.logError("Get daily goal error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun isDailyGoalReached(): Result<Boolean> = try {
        val completedWordsResult = getDailyCompletedWords()
        val dailyGoalResult = getDailyGoal()

        if (completedWordsResult is Result.Success && dailyGoalResult is Result.Success) {
            val isReached = completedWordsResult.data >= dailyGoalResult.data
            DebugHelper.log("🏆 Daily goal check: ${completedWordsResult.data}/${dailyGoalResult.data} = reached: $isReached")
            Result.Success(isReached)
        } else {
            DebugHelper.log("❌ Could not check daily goal - using default false")
            Result.Success(false)
        }

    } catch (e: Exception) {
        DebugHelper.logError("Check daily goal reached error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun getTodaySessionStats(): Result<TodayStats> = try {
        val wordsStudiedResult = getTodayWordsStudied()
        val completedWordsResult = getDailyCompletedWords()
        val dailyGoalResult = getDailyGoal()

        val wordsStudied = (wordsStudiedResult as? Result.Success)?.data ?: 0
        val cardsCompleted = (completedWordsResult as? Result.Success)?.data ?: 0
        val dailyGoal = (dailyGoalResult as? Result.Success)?.data ?: 20

        val progressPercentage = if (dailyGoal > 0) {
            (cardsCompleted.toFloat() / dailyGoal.toFloat() * 100f).coerceAtMost(100f)
        } else 0f

        val todayStats = TodayStats(
            wordsStudied = wordsStudied,
            cardsCompleted = cardsCompleted,
            dailyGoal = dailyGoal,
            progressPercentage = progressPercentage,
            sessionCount = 1 // Simplified
        )

        DebugHelper.log("📊 Today stats: $todayStats")
        Result.Success(todayStats)

    } catch (e: Exception) {
        DebugHelper.logError("Get today session stats error", e)
        Result.Error(AppError.Unknown(e))
    }
}