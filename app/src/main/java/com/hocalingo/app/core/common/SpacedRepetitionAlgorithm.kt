package com.hocalingo.app.core.common

import com.hocalingo.app.database.entities.WordProgressEntity
import kotlin.math.max
import kotlin.math.min
import com.hocalingo.app.database.entities.StudyDirection

/**
 * ✅ OPTIMIZED SM-2 Spaced Repetition Algorithm - VERSION 3
 *
 * Package: app/src/main/java/com/hocalingo/app/core/common/
 *
 * 🎯 V3 MAJOR CHANGES:
 * 1. ✅ successfulReviews now Float (partial success tracking)
 * 2. ✅ MEDIUM = 0.5 points (graduation requires 3 points = 3 EASY or 6 MEDIUM)
 * 3. ✅ EASY = 1.0 point (full success)
 * 4. ✅ Review HARD: Reset to learning phase (sıfırdan başla)
 *
 * Previous improvements:
 * - Learning phase = Bugün içinde tekrar göster
 * - Graduation = Gerçek öğrenme (minimum 3 başarılı)
 * - Session position = Aynı gün içinde sıralama
 * - Review MEDIUM: Adaptive multiplier (interval-based)
 */
object SpacedRepetitionAlgorithm {

    // ==================== QUALITY SCORES ====================
    const val QUALITY_HARD = 1      // "Zor" - Hatırlamadım, tekrar göster
    const val QUALITY_MEDIUM = 2    // "Orta" - Hatırladım ama zorlandım
    const val QUALITY_EASY = 3      // "Kolay" - Rahatça hatırladım

    // ==================== LEARNING PHASE CONSTANTS ====================
    // ✅ Session position increments (aynı gün içinde sıralama)
    private const val HARD_POSITION_INCREMENT = 1    // En başa
    private const val MEDIUM_POSITION_INCREMENT = 5  // Ortaya
    private const val EASY_POSITION_INCREMENT = 10   // Sona

    // ✅ Graduation thresholds (gerçek öğrenme kriterleri)
    private const val MIN_SUCCESSFUL_REVIEWS = 3f     // En az 3.0 puan
    private const val MAX_HARD_PRESSES_TO_GRADUATE = 1  // Max 1 HARD basışı

    // ==================== REVIEW PHASE CONSTANTS ====================
    // Graduation sonrası başlangıç aralığı
    private const val GRADUATION_INTERVAL_DAYS = 1f  // 1 gün (ilk review)

    // Standard review intervals
    private const val SECOND_REVIEW_INTERVAL_DAYS = 3f   // 2. review: 3 gün
    private const val THIRD_REVIEW_INTERVAL_DAYS = 7f    // 3. review: 7 gün

    // Ease factor bounds
    private const val MIN_EASE_FACTOR = 1.3f
    private const val MAX_EASE_FACTOR = 2.5f
    private const val DEFAULT_EASE_FACTOR = 2.5f

    // Max interval cap
    private const val MAX_INTERVAL_DAYS = 365f  // 1 yıl

    // ==================== MAIN ALGORITHM ====================

    /**
     * ✅ V3 OPTIMIZED: Calculate next review with partial success tracking
     */
    fun calculateNextReview(
        currentProgress: WordProgressEntity,
        quality: Int,
        currentSessionMaxPosition: Int = 100
    ): WordProgressEntity {
        val currentTime = System.currentTimeMillis()
        val todayEnd = getTodayEndTime(currentTime)

        DebugHelper.log(
            "🔥 SM-2 V3: quality=$quality, reps=${currentProgress.repetitions}, " +
                    "learningPhase=${currentProgress.learningPhase}, " +
                    "hardPresses=${currentProgress.hardPresses ?: 0}, " +
                    "successfulReviews=${currentProgress.successfulReviews ?: 0f}"
        )

        val result = when {
            // ✅ LEARNING PHASE (bugün içinde tekrar göster)
            currentProgress.learningPhase -> handleLearningPhase(
                currentProgress, quality, currentTime, todayEnd, currentSessionMaxPosition
            )

            // ✅ REVIEW PHASE (gerçek spaced repetition)
            else -> handleReviewPhase(
                currentProgress, quality, currentTime
            )
        }

        // Debug log
        val phaseText = if (result.learningPhase) "LEARNING" else "REVIEW"
        val timeText = getTimeUntilReview(result.nextReviewAt)
        DebugHelper.log(
            "✅ RESULT: $phaseText, reps=${result.repetitions}, " +
                    "successful=${result.successfulReviews ?: 0f}, " +
                    "interval=${result.intervalDays}d, EF=${result.easeFactor}, next='$timeText'"
        )

        return result
    }

    // ==================== LEARNING PHASE ====================

    /**
     * ✅ V3 Learning Phase: MEDIUM gives 0.5 points, EASY gives 1.0 point
     *
     * Graduation requires 3.0 points:
     * - 3 EASY = 3.0 points ✅
     * - 6 MEDIUM = 3.0 points ✅
     * - 2 EASY + 2 MEDIUM = 3.0 points ✅
     * - 1 EASY + 4 MEDIUM = 3.0 points ✅
     */
    private fun handleLearningPhase(
        currentProgress: WordProgressEntity,
        quality: Int,
        currentTime: Long,
        todayEnd: Long,
        currentSessionMaxPosition: Int
    ): WordProgressEntity {

        return when (quality) {
            QUALITY_HARD -> {
                DebugHelper.log("🔴 LEARNING HARD: Position = başa, no points")

                currentProgress.copy(
                    repetitions = currentProgress.repetitions + 1,
                    intervalDays = 0f,
                    easeFactor = max(MIN_EASE_FACTOR, currentProgress.easeFactor - 0.2f),
                    nextReviewAt = currentTime + (1 * 60 * 1000),
                    lastReviewAt = currentTime,
                    learningPhase = true,
                    sessionPosition = currentSessionMaxPosition + HARD_POSITION_INCREMENT,
                    hardPresses = (currentProgress.hardPresses ?: 0) + 1,
                    successfulReviews = 0f,  // SIFIRLA
                    isMastered = false,
                    updatedAt = currentTime
                )
            }

            QUALITY_MEDIUM -> {
                val newSuccessful = (currentProgress.successfulReviews ?: 0f) + 0.5f  // ✅ Yarım puan
                val newReps = currentProgress.repetitions + 1
                val hardPresses = currentProgress.hardPresses ?: 0

                // ✅ Check graduation
                if (shouldGraduate(newSuccessful, hardPresses)) {
                    DebugHelper.log("🎓 GRADUATING (MEDIUM): $newSuccessful points, $hardPresses hard")
                    graduateToReview(currentProgress, newReps, currentTime)
                } else {
                    DebugHelper.log("🟡 LEARNING MEDIUM: Position = ortaya, successful = $newSuccessful points")

                    currentProgress.copy(
                        repetitions = newReps,
                        intervalDays = 0f,
                        easeFactor = min(MAX_EASE_FACTOR, currentProgress.easeFactor + 0.05f),
                        nextReviewAt = currentTime + (10 * 60 * 1000),  // 10 dakika
                        lastReviewAt = currentTime,
                        learningPhase = true,
                        sessionPosition = currentSessionMaxPosition + MEDIUM_POSITION_INCREMENT,
                        successfulReviews = newSuccessful,  // ✅ Yarım puan eklendi
                        isMastered = false,
                        updatedAt = currentTime
                    )
                }
            }

            QUALITY_EASY -> {
                val newSuccessful = (currentProgress.successfulReviews ?: 0f) + 1f  // ✅ Tam puan
                val newReps = currentProgress.repetitions + 1
                val hardPresses = currentProgress.hardPresses ?: 0

                // ✅ Check graduation
                if (shouldGraduate(newSuccessful, hardPresses)) {
                    DebugHelper.log("🎓 GRADUATING (EASY): $newSuccessful points, $hardPresses hard")
                    graduateToReview(currentProgress, newReps, currentTime)
                } else {
                    DebugHelper.log("🟢 LEARNING EASY: Position = sona, successful = $newSuccessful points")

                    currentProgress.copy(
                        repetitions = newReps,
                        intervalDays = 0f,
                        easeFactor = min(MAX_EASE_FACTOR, currentProgress.easeFactor + 0.1f),
                        nextReviewAt = currentTime + (60 * 60 * 1000),  // 1 saat
                        lastReviewAt = currentTime,
                        learningPhase = true,
                        sessionPosition = currentSessionMaxPosition + EASY_POSITION_INCREMENT,
                        successfulReviews = newSuccessful,  // ✅ Tam puan eklendi
                        isMastered = false,
                        updatedAt = currentTime
                    )
                }
            }

            else -> currentProgress
        }
    }

    /**
     * ✅ V3 Graduation criteria: 3.0 points required
     */
    private fun shouldGraduate(successfulReviews: Float, hardPresses: Int): Boolean {
        return successfulReviews >= MIN_SUCCESSFUL_REVIEWS  // ✅ Float comparison: >= 3.0
    }

    /**
     * ✅ Graduate to Review Phase
     */
    private fun graduateToReview(
        currentProgress: WordProgressEntity,
        newReps: Int,
        currentTime: Long
    ): WordProgressEntity {
        return currentProgress.copy(
            repetitions = newReps,
            intervalDays = GRADUATION_INTERVAL_DAYS,
            easeFactor = min(MAX_EASE_FACTOR, currentProgress.easeFactor + 0.15f),
            nextReviewAt = currentTime + (GRADUATION_INTERVAL_DAYS * 24 * 60 * 60 * 1000).toLong(),
            lastReviewAt = currentTime,
            learningPhase = false,  // ✅ GRADUATE
            sessionPosition = null,
            isMastered = false,
            updatedAt = currentTime
        )
    }

    // ==================== REVIEW PHASE ====================

    /**
     * ✅ V3 Review Phase: HARD resets to learning phase
     *
     * HARD behavior:
     * - Resets to learning phase (başa dön)
     * - User didn't remember the word, start over
     */
    private fun handleReviewPhase(
        currentProgress: WordProgressEntity,
        quality: Int,
        currentTime: Long
    ): WordProgressEntity {

        return when (quality) {
            QUALITY_HARD -> {
                DebugHelper.log("🔴 REVIEW HARD: Back to learning phase")

                // ✅ Başarısız review → Learning phase'e geri dön
                val todayEnd = getTodayEndTime(currentTime)

                currentProgress.copy(
                    repetitions = 1,
                    intervalDays = GRADUATION_INTERVAL_DAYS,
                    easeFactor = max(MIN_EASE_FACTOR, currentProgress.easeFactor - 0.2f),
                    nextReviewAt = currentTime + (GRADUATION_INTERVAL_DAYS * 24 * 60 * 60 * 1000).toLong(),
                    lastReviewAt = currentTime,
                    learningPhase = false,
                    sessionPosition = null,
                    hardPresses = (currentProgress.hardPresses ?: 0) + 1,
                    successfulReviews = 0f,
                    isMastered = false,
                    updatedAt = currentTime
                )
            }

            QUALITY_MEDIUM -> {
                DebugHelper.log("🟡 REVIEW MEDIUM: Adaptive progression/reduction")

                val newReps = currentProgress.repetitions + 1
                val baseInterval = max(1f, currentProgress.intervalDays)
                val newEaseFactor = updateEaseFactor(currentProgress.easeFactor, 4)

                // ✅ ADAPTIVE MULTIPLIER: Interval'a göre dinamik davranış
                val mediumMultiplier = when {
                    baseInterval <= 3f -> 1.5f      // 0-3 gün: Gentle progression
                    baseInterval <= 7f -> 1.2f      // 4-7 gün: Hafif progression
                    baseInterval <= 21f -> 0.85f    // 8-21 gün: Hafif reduction
                    else -> 0.5f                     // 21+ gün: Ciddi reduction
                }

                val calculatedInterval = baseInterval * mediumMultiplier
                val finalInterval = min(calculatedInterval, MAX_INTERVAL_DAYS)

                DebugHelper.log(
                    "🔧 MEDIUM: baseInterval=${baseInterval}d, multiplier=${mediumMultiplier}, " +
                            "EF=${newEaseFactor}, finalInterval=${finalInterval}d"
                )

                currentProgress.copy(
                    repetitions = newReps,
                    intervalDays = finalInterval,
                    easeFactor = newEaseFactor,
                    nextReviewAt = currentTime + (finalInterval * 24 * 60 * 60 * 1000).toLong(),
                    lastReviewAt = currentTime,
                    learningPhase = false,
                    sessionPosition = null,
                    isMastered = finalInterval >= 21f && newReps >= 4,
                    updatedAt = currentTime
                )
            }

            QUALITY_EASY -> {
                DebugHelper.log("🟢 REVIEW EASY: Strong progression")

                val newReps = currentProgress.repetitions + 1
                val newEaseFactor = updateEaseFactor(currentProgress.easeFactor, 5)

                // ✅ Progressive interval calculation
                val calculatedInterval = when (newReps) {
                    1 -> GRADUATION_INTERVAL_DAYS     // 1 gün
                    2 -> SECOND_REVIEW_INTERVAL_DAYS  // 3 gün
                    3 -> THIRD_REVIEW_INTERVAL_DAYS   // 7 gün
                    else -> {
                        val baseInterval = max(1f, currentProgress.intervalDays)
                        baseInterval * newEaseFactor
                    }
                }

                val finalInterval = min(calculatedInterval, MAX_INTERVAL_DAYS)

                currentProgress.copy(
                    repetitions = newReps,
                    intervalDays = finalInterval,
                    easeFactor = newEaseFactor,
                    nextReviewAt = currentTime + (finalInterval * 24 * 60 * 60 * 1000).toLong(),
                    lastReviewAt = currentTime,
                    learningPhase = false,
                    sessionPosition = null,
                    isMastered = finalInterval >= 21f && newReps >= 4,
                    updatedAt = currentTime
                )
            }

            else -> currentProgress
        }
    }

    // ==================== HELPER FUNCTIONS ====================

    /**
     * ✅ Get today's end time (23:59:59.999)
     */
    private fun getTodayEndTime(currentTime: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = currentTime
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    /**
     * ✅ SM-2 Ease Factor update formula
     */
    private fun updateEaseFactor(currentEF: Float, quality: Int): Float {
        val newEF = currentEF + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f))
        return max(MIN_EASE_FACTOR, min(newEF, MAX_EASE_FACTOR))
    }

    /**
     * ✅ Get human-readable time until review
     */
    fun getTimeUntilReview(nextReviewAt: Long): String {
        val currentTime = System.currentTimeMillis()
        val timeDifferenceMs = nextReviewAt - currentTime

        if (timeDifferenceMs <= 0) {
            return "Şimdi"
        }

        val minutes = timeDifferenceMs / (60 * 1000)
        val hours = minutes / 60
        val days = hours / 24

        return when {
            // ✅ Learning phase için insan dostu metinler
            minutes < 5 -> "Hemen tekrar"          // 0-5 dk
            minutes < 30 -> "Birazdan"             // 5-30 dk
            hours < 2 -> "Sonra"                   // 30 dk - 2 saat
            hours < 12 -> "Bugün tekrar"           // 2-12 saat
            days < 1 -> "Yarın"                    // 12-24 saat

            // ✅ Review phase: gün → ay → yıl (ondalıklı)
            days < 30 -> "${days.toInt()} gün"
            days < 365 -> "%.1f ay".format(days / 30f)
            else -> "%.1f yıl".format(days / 365f)
        }
    }

    /**
     * ✅ V3: Create initial progress with Float successfulReviews
     */
    fun createInitialProgress(
        conceptId: Int,
        direction: StudyDirection,
        sessionPosition: Int
    ): WordProgressEntity {
        val currentTime = System.currentTimeMillis()
        val todayEnd = getTodayEndTime(currentTime)

        return WordProgressEntity(
            conceptId = conceptId,
            direction = direction,
            repetitions = 0,
            intervalDays = 0f,
            easeFactor = DEFAULT_EASE_FACTOR,
            nextReviewAt = todayEnd,
            lastReviewAt = null,
            isSelected = true,
            isMastered = false,
            learningPhase = true,
            sessionPosition = sessionPosition,
            hardPresses = 0,
            successfulReviews = 0f,  // ✅ Float initialization
            createdAt = currentTime,
            updatedAt = currentTime
        )
    }

    /**
     * ✅ Get study priority for session ordering
     */
    fun getStudyPriority(progress: WordProgressEntity, currentTime: Long): Int {
        // Learning cards sorted by sessionPosition
        if (progress.learningPhase) {
            return Int.MAX_VALUE - (progress.sessionPosition ?: 0)
        }

        // Review cards sorted by how overdue
        val timeDifference = currentTime - progress.nextReviewAt
        return if (timeDifference > 0) {
            (timeDifference / (1000 * 60 * 60)).toInt()
        } else {
            0
        }
    }

    /**
     * ✅ Get button preview texts for UI
     */
    fun getButtonPreviews(progress: WordProgressEntity): Triple<String, String, String> {
        val mockResults = listOf(
            calculateNextReview(progress, QUALITY_HARD),
            calculateNextReview(progress, QUALITY_MEDIUM),
            calculateNextReview(progress, QUALITY_EASY)
        )

        return Triple(
            getTimeUntilReview(mockResults[0].nextReviewAt),
            getTimeUntilReview(mockResults[1].nextReviewAt),
            getTimeUntilReview(mockResults[2].nextReviewAt)
        )
    }
}