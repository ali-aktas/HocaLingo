package com.hocalingo.app.core.common

import com.hocalingo.app.database.entities.WordProgressEntity
import kotlin.math.max
import kotlin.math.min
import com.hocalingo.app.database.entities.StudyDirection

/**
 * SM-2 Spaced Repetition Algorithm - IMPROVED HYBRID VERSION
 *
 * CHATGPT İYİLEŞTİRMELERİ UYGULANMIŞ:
 * ✅ Dynamic GOOD progression (reps arttıkça artış büyür)
 * ✅ Conservative EASY graduation (2 gün başlangıç)
 * ✅ Lateness correction (gecikme penaltısı)
 * ✅ Failures recovery system (3 başarı sonrası failures azalır)
 * ✅ Success streak tracking
 * ✅ Improved SM-2 quality mapping
 */
object SpacedRepetitionAlgorithm {

    // Quality scores for user responses
    const val QUALITY_HARD = 1      // "Zor" button - poor recall
    const val QUALITY_MEDIUM = 2    // "Orta" button - good recall
    const val QUALITY_EASY = 3      // "Kolay" button - perfect recall

    // Learning phase intervals (for button display only - cards stay in session)
    private const val LEARNING_HARD_INTERVAL_MINUTES = 10
    private const val LEARNING_MEDIUM_INTERVAL_MINUTES = 1440  // 1 gün
    private const val LEARNING_EASY_INTERVAL_MINUTES = 4320   // 3 gün

    // Graduation thresholds - IMPROVED
    private const val MIN_REPETITIONS_TO_GRADUATE = 2
    private const val GRADUATION_INTERVAL_DAYS = 2f     // CHATGPT: Conservative 2 days (was 1)

    // Standard intervals in days for Review Phase
    private const val SECOND_REVIEW_INTERVAL_DAYS = 3f
    private const val THIRD_REVIEW_INTERVAL_DAYS = 7f

    // Ease factor bounds
    private const val MIN_EASE_FACTOR = 1.3f
    private const val MAX_EASE_FACTOR = 2.5f
    private const val DEFAULT_EASE_FACTOR = 2.5f

    // NEW: Recovery system constants
    private const val SUCCESS_STREAK_TO_RECOVER = 3  // 3 başarı sonrası failures azalır
    private const val MAX_INTERVAL_DAYS = 730f       // 2 yıl cap

    /**
     * Calculate next review based on user response quality - IMPROVED HYBRID VERSION
     */
    fun calculateNextReview(
        currentProgress: WordProgressEntity,
        quality: Int,
        currentSessionMaxPosition: Int = 100
    ): WordProgressEntity {
        val currentTime = System.currentTimeMillis()

        DebugHelper.log(
            "🔥 IMPROVED HYBRID SM-2: quality=$quality, currentReps=${currentProgress.repetitions}, " +
                    "learningPhase=${currentProgress.learningPhase}, failures=${currentProgress.failures ?: 0}, " +
                    "successStreak=${currentProgress.successStreak ?: 0}"
        )

        // IMPROVED: Calculate lateness if applicable
        val latenessDays = if (currentProgress.nextReviewAt > 0) {
            calculateLateness(currentProgress.nextReviewAt, currentTime)
        } else 0f

        val result = when {
            // LEARNING PHASE RESPONSES
            currentProgress.learningPhase -> handleImprovedLearningPhase(
                currentProgress, quality, currentTime, currentSessionMaxPosition
            )

            // REVIEW PHASE RESPONSES
            else -> handleImprovedReviewPhase(
                currentProgress, quality, currentTime, latenessDays
            )
        }

        // Debug log result
        val phaseText = if (result.learningPhase) "LEARNING" else "REVIEW"
        val timeText = getTimeUntilReview(result.nextReviewAt)
        DebugHelper.log(
            "✅ IMPROVED RESULT: $phaseText phase, reps=${result.repetitions}, " +
                    "failures=${result.failures}, successStreak=${result.successStreak}, " +
                    "interval=${result.intervalDays}d, timeText='$timeText'"
        )

        return result
    }

    /**
     * IMPROVED: Handle responses during Learning Phase with better tracking
     */
    private fun handleImprovedLearningPhase(
        currentProgress: WordProgressEntity,
        quality: Int,
        currentTime: Long,
        currentSessionMaxPosition: Int
    ): WordProgressEntity {

        return when (quality) {
            QUALITY_HARD -> {
                DebugHelper.log("🔴 LEARNING HARD: Reset progress, increase failures")

                currentProgress.copy(
                    repetitions = 0, // Reset learning progress
                    intervalDays = LEARNING_HARD_INTERVAL_MINUTES / (24f * 60f),
                    easeFactor = max(MIN_EASE_FACTOR, currentProgress.easeFactor - 0.2f),
                    nextReviewAt = currentTime + (LEARNING_HARD_INTERVAL_MINUTES * 60 * 1000),
                    lastReviewAt = currentTime,
                    learningPhase = true,
                    sessionPosition = currentSessionMaxPosition + 1,
                    failures = (currentProgress.failures ?: 0) + 1, // IMPROVED: Track failures
                    successStreak = 0, // IMPROVED: Reset success streak
                    isMastered = false,
                    updatedAt = currentTime
                )
            }

            QUALITY_MEDIUM -> {
                DebugHelper.log("🟡 LEARNING MEDIUM: Small progress")

                currentProgress.copy(
                    repetitions = currentProgress.repetitions + 1,
                    intervalDays = LEARNING_MEDIUM_INTERVAL_MINUTES / (24f * 60f),
                    easeFactor = currentProgress.easeFactor, // No change for medium
                    nextReviewAt = currentTime + (LEARNING_MEDIUM_INTERVAL_MINUTES * 60 * 1000),
                    lastReviewAt = currentTime,
                    learningPhase = true,
                    sessionPosition = currentSessionMaxPosition + 1,
                    successStreak = (currentProgress.successStreak ?: 0) + 1, // IMPROVED: Track success
                    updatedAt = currentTime
                )
            }

            QUALITY_EASY -> {
                val newRepetitions = currentProgress.repetitions + 1
                val newSuccessStreak = (currentProgress.successStreak ?: 0) + 1

                if (newRepetitions >= MIN_REPETITIONS_TO_GRADUATE) {
                    // GRADUATION - IMPROVED: 2 days conservative start
                    DebugHelper.log("🎓 GRADUATING: Moving to review phase with 2 day interval")

                    currentProgress.copy(
                        repetitions = newRepetitions,
                        intervalDays = GRADUATION_INTERVAL_DAYS, // IMPROVED: 2 days
                        easeFactor = min(MAX_EASE_FACTOR, currentProgress.easeFactor + 0.12f),
                        nextReviewAt = currentTime + (GRADUATION_INTERVAL_DAYS * 24 * 60 * 60 * 1000).toLong(),
                        lastReviewAt = currentTime,
                        learningPhase = false, // GRADUATE
                        sessionPosition = null,
                        successStreak = newSuccessStreak,
                        isMastered = false,
                        updatedAt = currentTime
                    )
                } else {
                    // Still learning
                    DebugHelper.log("🟢 LEARNING EASY: Progress made, back of session")

                    currentProgress.copy(
                        repetitions = newRepetitions,
                        intervalDays = LEARNING_EASY_INTERVAL_MINUTES / (24f * 60f),
                        easeFactor = min(MAX_EASE_FACTOR, currentProgress.easeFactor + 0.1f),
                        nextReviewAt = currentTime + (LEARNING_EASY_INTERVAL_MINUTES * 60 * 1000),
                        lastReviewAt = currentTime,
                        learningPhase = true,
                        sessionPosition = currentSessionMaxPosition + 1,
                        successStreak = newSuccessStreak,
                        updatedAt = currentTime
                    )
                }
            }

            else -> {
                DebugHelper.logError("Invalid quality score: $quality", Exception())
                currentProgress
            }
        }
    }

    /**
     * IMPROVED: Handle responses during Review Phase with dynamic progression & recovery
     */
    private fun handleImprovedReviewPhase(
        currentProgress: WordProgressEntity,
        quality: Int,
        currentTime: Long,
        latenessDays: Float
    ): WordProgressEntity {

        return when (quality) {
            QUALITY_HARD -> {
                DebugHelper.log("🔴 REVIEW HARD: Back to learning phase")

                // Failed review → Back to learning phase
                currentProgress.copy(
                    repetitions = 0,
                    intervalDays = LEARNING_HARD_INTERVAL_MINUTES / (24f * 60f),
                    easeFactor = max(MIN_EASE_FACTOR, currentProgress.easeFactor - 0.2f),
                    nextReviewAt = currentTime + (LEARNING_HARD_INTERVAL_MINUTES * 60 * 1000),
                    lastReviewAt = currentTime,
                    learningPhase = true, // Back to learning
                    sessionPosition = 1, // Front of session
                    failures = (currentProgress.failures ?: 0) + 1, // IMPROVED: Track failures
                    successStreak = 0, // IMPROVED: Reset success streak
                    isMastered = false,
                    updatedAt = currentTime
                )
            }

            QUALITY_MEDIUM -> {
                DebugHelper.log("🟡 REVIEW MEDIUM: Dynamic progression")

                val newRepetitions = currentProgress.repetitions + 1
                val newSuccessStreak = (currentProgress.successStreak ?: 0) + 1

                // IMPROVED: Dynamic multiplier for MEDIUM - starts at 1.20, increases with reps
                val additional = min(0.4f, 0.05f * currentProgress.repetitions)
                val multiplier = 1.20f + additional

                val baseInterval = max(1f, currentProgress.intervalDays)
                val calculatedInterval = baseInterval * multiplier

                // IMPROVED: Apply lateness correction
                val adjustedInterval = applyLatenessCorrection(calculatedInterval, latenessDays)
                val finalInterval = min(adjustedInterval, MAX_INTERVAL_DAYS)

                // IMPROVED: Failures recovery system
                val (newFailures, finalSuccessStreak) = applyFailuresRecovery(
                    currentProgress.failures ?: 0,
                    newSuccessStreak
                )

                currentProgress.copy(
                    repetitions = newRepetitions,
                    intervalDays = finalInterval,
                    easeFactor = updateEaseFactor(currentProgress.easeFactor, 4), // SM-2 quality 4
                    nextReviewAt = currentTime + (finalInterval * 24 * 60 * 60 * 1000).toLong(),
                    lastReviewAt = currentTime,
                    learningPhase = false,
                    sessionPosition = null,
                    failures = newFailures,
                    successStreak = finalSuccessStreak,
                    isMastered = finalInterval >= 30f && newRepetitions >= 5,
                    updatedAt = currentTime
                )
            }

            QUALITY_EASY -> {
                DebugHelper.log("🟢 REVIEW EASY: Aggressive but controlled progression")

                val newRepetitions = currentProgress.repetitions + 1
                val newSuccessStreak = (currentProgress.successStreak ?: 0) + 1
                val newEaseFactor = updateEaseFactor(currentProgress.easeFactor, 5) // SM-2 quality 5

                val baseInterval = max(1f, currentProgress.intervalDays)
                val calculatedInterval = when (newRepetitions) {
                    3 -> SECOND_REVIEW_INTERVAL_DAYS
                    4 -> THIRD_REVIEW_INTERVAL_DAYS
                    else -> {
                        // IMPROVED: Controlled EF multiplier (cap at 2.0 to prevent explosion)
                        val efMultiplier = min(newEaseFactor, 2.0f)
                        baseInterval * efMultiplier
                    }
                }

                // IMPROVED: Apply lateness correction
                val adjustedInterval = applyLatenessCorrection(calculatedInterval, latenessDays)
                val finalInterval = min(adjustedInterval, MAX_INTERVAL_DAYS)

                // IMPROVED: Failures recovery system
                val (newFailures, finalSuccessStreak) = applyFailuresRecovery(
                    currentProgress.failures ?: 0,
                    newSuccessStreak
                )

                currentProgress.copy(
                    repetitions = newRepetitions,
                    intervalDays = finalInterval,
                    easeFactor = newEaseFactor,
                    nextReviewAt = currentTime + (finalInterval * 24 * 60 * 60 * 1000).toLong(),
                    lastReviewAt = currentTime,
                    learningPhase = false,
                    sessionPosition = null,
                    failures = newFailures,
                    successStreak = finalSuccessStreak,
                    isMastered = finalInterval >= 30f && newRepetitions >= 5,
                    updatedAt = currentTime
                )
            }

            else -> {
                DebugHelper.logError("Invalid quality score: $quality", Exception())
                currentProgress
            }
        }
    }

    // ==================== NEW HELPER FUNCTIONS ====================

    /**
     * IMPROVED: Calculate lateness in days (gentle approach)
     */
    private fun calculateLateness(scheduledTime: Long, currentTime: Long): Float {
        if (currentTime <= scheduledTime) return 0f
        val delayMs = currentTime - scheduledTime
        return delayMs / (24 * 60 * 60 * 1000f)
    }

    /**
     * IMPROVED: Apply gentle lateness correction with gradual penalties
     */
    private fun applyLatenessCorrection(intervalDays: Float, latenessDays: Float): Float {
        if (latenessDays <= 0f) return intervalDays

        val safeInterval = max(0.0001f, intervalDays)
        val delayRatio = latenessDays / safeInterval

        val factor = when {
            delayRatio <= 1.0f -> 1.0f   // On time or slight delay - no change
            delayRatio <= 2.0f -> 0.85f  // 2x late → 15% reduction
            delayRatio <= 3.0f -> 0.7f   // 3x late → 30% reduction
            else -> 0.5f                 // Very late → 50% reduction
        }

        return intervalDays * factor
    }

    /**
     * IMPROVED: Proper SM-2 ease factor update formula
     */
    private fun updateEaseFactor(currentEF: Float, quality: Int): Float {
        // Original SM-2 formula
        val newEF = currentEF + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f))
        return max(MIN_EASE_FACTOR, min(newEF, MAX_EASE_FACTOR))
    }

    /**
     * IMPROVED: Failures recovery system - 3 successful reviews reduce failures by 1
     */
    private fun applyFailuresRecovery(currentFailures: Int, newSuccessStreak: Int): Pair<Int, Int> {
        return if (newSuccessStreak >= SUCCESS_STREAK_TO_RECOVER && currentFailures > 0) {
            val reducedFailures = max(0, currentFailures - 1)
            val resetStreak = 0 // Reset counter after recovery
            Pair(reducedFailures, resetStreak)
        } else {
            Pair(currentFailures, newSuccessStreak)
        }
    }

    // ==================== EXISTING FUNCTIONS (unchanged) ====================

    /**
     * Get human-readable time description for next review
     */
    fun getTimeUntilReview(nextReviewAt: Long): String {
        val currentTime = System.currentTimeMillis()
        val timeDifferenceMs = nextReviewAt - currentTime

        if (timeDifferenceMs <= 0) {
            return "Şimdi"
        }

        val seconds = timeDifferenceMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = days / 30

        return when {
            seconds < 60 -> "${seconds.toInt()} saniye sonra"
            minutes < 60 -> "${minutes.toInt()} dakika sonra"
            hours < 24 -> "${hours.toInt()} saat sonra"
            days < 7 -> "${days.toInt()} gün sonra"
            weeks < 4 -> "${weeks.toInt()} hafta sonra"
            months >= 1 -> "${months.toInt()} ay sonra"
            else -> "${days.toInt()} gün sonra"
        }
    }

    /**
     * Create initial WordProgressEntity for new concepts
     */
    fun createInitialProgress(
        conceptId: Int,
        direction: StudyDirection,
        sessionPosition: Int
    ): WordProgressEntity {
        val currentTime = System.currentTimeMillis()

        return WordProgressEntity(
            conceptId = conceptId,
            direction = direction,
            repetitions = 0,
            intervalDays = 0f,
            easeFactor = DEFAULT_EASE_FACTOR,
            nextReviewAt = currentTime,
            lastReviewAt = null,
            isSelected = true,
            isMastered = false,
            learningPhase = true, // Start in learning
            sessionPosition = sessionPosition,
            failures = 0, // IMPROVED: Initialize failures
            successStreak = 0, // IMPROVED: Initialize success streak
            createdAt = currentTime,
            updatedAt = currentTime
        )
    }

    /**
     * Calculate study priority for session ordering
     */
    fun getStudyPriority(progress: WordProgressEntity, currentTime: Long): Int {
        // Learning cards have highest priority
        if (progress.learningPhase) {
            return Int.MAX_VALUE - (progress.sessionPosition ?: 0)
        }

        // Review cards prioritized by how overdue they are
        val timeDifference = currentTime - progress.nextReviewAt
        return if (timeDifference > 0) {
            (timeDifference / (1000 * 60 * 60)).toInt() // Hours overdue
        } else {
            0
        }
    }

    /**
     * Get button preview texts (for UI)
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