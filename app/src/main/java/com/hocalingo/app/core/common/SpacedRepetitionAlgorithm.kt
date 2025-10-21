package com.hocalingo.app.core.common

import com.hocalingo.app.database.entities.WordProgressEntity
import kotlin.math.max
import kotlin.math.min
import com.hocalingo.app.database.entities.StudyDirection

/**
 * ✅ OPTIMIZED SM-2 Spaced Repetition Algorithm - PRODUCTION READY
 *
 * Package: app/src/main/java/com/hocalingo/app/core/common/
 *
 * 🎯 核心修正：
 * 1. Learning phase = Bugün içinde tekrar göster (nextReviewAt = today)
 * 2. Graduation = Gerçek öğrenme (minimum 3-4 başarılı deneme)
 * 3. Session position = Aynı gün içinde sıralama (başa/ortaya/sona)
 * 4. Review phase = Gerçekten öğrenilmiş kelimeler için
 *
 * 🔥 V2 IMPROVEMENTS:
 * - ✅ Review MEDIUM: Adaptive multiplier (interval-based behavior)
 *   - 0-3 gün: 1.5x progression (gentle growth)
 *   - 4-7 gün: 1.2x progression (moderate growth)
 *   - 8-21 gün: 0.85x reduction (slight decrease)
 *   - 21+ gün: 0.5x reduction (significant decrease)
 * - ✅ Learning MEDIUM: +0.05 EaseFactor bonus
 * - ✅ Semantically correct: "Zorlandım" = daha sık görmek istiyorum
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
    private const val MIN_SUCCESSFUL_REVIEWS = 3     // En az 3 başarılı
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
     * ✅ OPTIMIZED: Calculate next review with adaptive MEDIUM behavior
     */
    fun calculateNextReview(
        currentProgress: WordProgressEntity,
        quality: Int,
        currentSessionMaxPosition: Int = 100
    ): WordProgressEntity {
        val currentTime = System.currentTimeMillis()
        val todayEnd = getTodayEndTime(currentTime) // ✅ Bugünün sonu

        DebugHelper.log(
            "🔥 SM-2 V2: quality=$quality, reps=${currentProgress.repetitions}, " +
                    "learningPhase=${currentProgress.learningPhase}, " +
                    "hardPresses=${currentProgress.hardPresses ?: 0}, " +
                    "successfulReviews=${currentProgress.successfulReviews ?: 0}"
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
                    "interval=${result.intervalDays}d, EF=${result.easeFactor}, next='$timeText'"
        )

        return result
    }

    // ==================== LEARNING PHASE ====================

    /**
     * ✅ FIXED Learning Phase: Kelimeleri aynı gün içinde tekrar göster
     *
     * Mantık:
     * - HARD → En başa (position = current + 1), hardPresses++
     * - MEDIUM → Ortaya (position = current + 5), successfulReviews++, hafif EF bonusu
     * - EASY → Sona (position = current + 10), successfulReviews++
     * - Graduation check: 3+ başarılı VE max 1 HARD
     * - nextReviewAt = Bugünün sonu (aynı gün içinde review)
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
                DebugHelper.log("🔴 LEARNING HARD: Position = başa")

                currentProgress.copy(
                    repetitions = currentProgress.repetitions + 1,
                    intervalDays = 0f, // Same day
                    easeFactor = max(MIN_EASE_FACTOR, currentProgress.easeFactor - 0.2f),
                    nextReviewAt = currentTime + (10 * 60 * 1000),
                    lastReviewAt = currentTime,
                    learningPhase = true,
                    sessionPosition = currentSessionMaxPosition + HARD_POSITION_INCREMENT,
                    hardPresses = (currentProgress.hardPresses ?: 0) + 1,
                    successfulReviews = currentProgress.successfulReviews ?: 0, // No change
                    isMastered = false,
                    updatedAt = currentTime
                )
            }

            QUALITY_MEDIUM -> {
                val newSuccessful = (currentProgress.successfulReviews ?: 0) + 1
                val newReps = currentProgress.repetitions + 1
                val hardPresses = currentProgress.hardPresses ?: 0

                // ✅ Check graduation
                if (shouldGraduate(newSuccessful, hardPresses)) {
                    DebugHelper.log("🎓 GRADUATING (MEDIUM): $newSuccessful successful, $hardPresses hard")
                    graduateToReview(currentProgress, newReps, currentTime)
                } else {
                    DebugHelper.log("🟡 LEARNING MEDIUM: Position = ortaya, successful = $newSuccessful")

                    currentProgress.copy(
                        repetitions = newReps,
                        intervalDays = 0f, // Same day
                        easeFactor = min(MAX_EASE_FACTOR, currentProgress.easeFactor + 0.05f), // ✅ FIX: Hafif bonus
                        nextReviewAt = currentTime + (10 * 60 * 1000), // 10 dk sonra
                        lastReviewAt = currentTime,
                        learningPhase = true,
                        sessionPosition = currentSessionMaxPosition + MEDIUM_POSITION_INCREMENT,
                        successfulReviews = newSuccessful,
                        isMastered = false,
                        updatedAt = currentTime
                    )
                }
            }

            QUALITY_EASY -> {
                val newSuccessful = (currentProgress.successfulReviews ?: 0) + 1
                val newReps = currentProgress.repetitions + 1
                val hardPresses = currentProgress.hardPresses ?: 0

                // ✅ Check graduation
                if (shouldGraduate(newSuccessful, hardPresses)) {
                    DebugHelper.log("🎓 GRADUATING (EASY): $newSuccessful successful, $hardPresses hard")
                    graduateToReview(currentProgress, newReps, currentTime)
                } else {
                    DebugHelper.log("🟢 LEARNING EASY: Position = sona, successful = $newSuccessful")

                    currentProgress.copy(
                        repetitions = newReps,
                        intervalDays = 0f, // Same day
                        easeFactor = min(MAX_EASE_FACTOR, currentProgress.easeFactor + 0.1f),
                        nextReviewAt = currentTime + (60 * 60 * 1000), // 1 saat sonra
                        lastReviewAt = currentTime,
                        learningPhase = true,
                        sessionPosition = currentSessionMaxPosition + EASY_POSITION_INCREMENT,
                        successfulReviews = newSuccessful,
                        isMastered = false,
                        updatedAt = currentTime
                    )
                }
            }

            else -> currentProgress
        }
    }

    /**
     * ✅ Graduation criteria check
     */
    private fun shouldGraduate(successfulReviews: Int, hardPresses: Int): Boolean {
        return successfulReviews >= MIN_SUCCESSFUL_REVIEWS &&
                hardPresses <= MAX_HARD_PRESSES_TO_GRADUATE
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
            learningPhase = false, // ✅ GRADUATE
            sessionPosition = null, // No longer in session
            isMastered = false,
            updatedAt = currentTime
        )
    }

    // ==================== REVIEW PHASE ====================

    /**
     * ✅ OPTIMIZED Review Phase: Gerçek spaced repetition (SM-2 + Adaptive MEDIUM)
     *
     * 🔧 V2: MEDIUM artık adaptive multiplier kullanıyor
     */
    private fun handleReviewPhase(
        currentProgress: WordProgressEntity,
        quality: Int,
        currentTime: Long
    ): WordProgressEntity {

        return when (quality) {
            QUALITY_HARD -> {
                DebugHelper.log("🔴 REVIEW HARD: Back to learning")

                // ✅ Başarısız review → Learning phase'e geri dön
                val todayEnd = getTodayEndTime(currentTime)

                currentProgress.copy(
                    repetitions = 0, // Reset
                    intervalDays = 0f,
                    easeFactor = max(MIN_EASE_FACTOR, currentProgress.easeFactor - 0.2f),
                    nextReviewAt = todayEnd, // ✅ Bugün tekrar göster
                    lastReviewAt = currentTime,
                    learningPhase = true, // Back to learning
                    sessionPosition = 1, // Front of session
                    hardPresses = 1, // Reset counter
                    successfulReviews = 0, // Reset
                    isMastered = false,
                    updatedAt = currentTime
                )
            }

            QUALITY_MEDIUM -> {
                DebugHelper.log("🟡 REVIEW MEDIUM: Adaptive progression/reduction")

                val newReps = currentProgress.repetitions + 1
                val baseInterval = max(1f, currentProgress.intervalDays)
                val newEaseFactor = updateEaseFactor(currentProgress.easeFactor, 4) // SM-2 quality 4

                // ✅ ADAPTIVE MULTIPLIER: Interval'a göre dinamik davranış
                // "Zorlandım" semantiği: Küçük interval'da ilerlet, büyük interval'da geri çek
                val mediumMultiplier = when {
                    baseInterval <= 3f -> 1.5f      // 0-3 gün: Gentle progression (1→1.5, 3→4.5)
                    baseInterval <= 7f -> 1.2f      // 4-7 gün: Hafif progression (5→6, 7→8.4)
                    baseInterval <= 21f -> 0.85f    // 8-21 gün: Hafif reduction (14→11.9, 21→17.85)
                    else -> 0.5f                     // 21+ gün: Ciddi reduction (30→15, 60→30)
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
                    isMastered = finalInterval >= 21f && newReps >= 4, // 3 hafta+
                    updatedAt = currentTime
                )
            }

            QUALITY_EASY -> {
                DebugHelper.log("🟢 REVIEW EASY: Strong progression")

                val newReps = currentProgress.repetitions + 1
                val newEaseFactor = updateEaseFactor(currentProgress.easeFactor, 5) // SM-2 quality 5

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
        // SM-2 formula: EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
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
            minutes < 60 -> "${minutes.toInt()} dk"
            hours < 24 -> "${hours.toInt()} saat"
            days < 7 -> "${days.toInt()} gün"
            days < 30 -> "${(days / 7).toInt()} hafta"
            else -> "${(days / 30).toInt()} ay"
        }
    }

    /**
     * ✅ Create initial progress for new words
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
            nextReviewAt = todayEnd, // ✅ Bugün çalışılacak
            lastReviewAt = null,
            isSelected = true,
            isMastered = false,
            learningPhase = true, // Start in learning
            sessionPosition = sessionPosition,
            hardPresses = 0,
            successfulReviews = 0,
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
            (timeDifference / (1000 * 60 * 60)).toInt() // Hours overdue
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