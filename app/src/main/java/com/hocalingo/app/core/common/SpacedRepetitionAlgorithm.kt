package com.hocalingo.app.core.common

import com.hocalingo.app.core.database.entities.WordProgressEntity
import kotlin.math.max
import kotlin.math.min

/**
 * SM-2 Spaced Repetition Algorithm - HYBRID LEARNING + REVIEW APPROACH
 *
 * MAJOR UPDATE: Implements session-based learning + time-based review
 *
 * LEARNING PHASE (stays in session):
 * - Hard → Back of current session queue
 * - Medium → Back of current session queue
 * - Easy (1st time) → Back of current session queue
 *
 * REVIEW PHASE (time-based):
 * - Easy (2nd+ time) → 1+ days → Real time scheduling
 *
 * USER EXPERIENCE:
 * ✅ Session never becomes empty (learning cards cycle)
 * ✅ Cards graduate only when user demonstrates mastery
 * ✅ Button texts remain same (time is invisible to user)
 * ✅ Progress bar only increases when cards graduate
 */
object SpacedRepetitionAlgorithm {

    // Quality scores for user responses
    const val QUALITY_HARD = 1      // "Zor" button - poor recall
    const val QUALITY_MEDIUM = 2    // "Orta" button - good recall
    const val QUALITY_EASY = 3      // "Kolay" button - perfect recall

    // Learning phase intervals (for button display only - cards stay in session)
    private const val LEARNING_HARD_INTERVAL_MINUTES = 5
    private const val LEARNING_MEDIUM_INTERVAL_MINUTES = 10
    private const val LEARNING_EASY_INTERVAL_MINUTES = 15

    // Graduation thresholds
    private const val MIN_REPETITIONS_TO_GRADUATE = 2    // Need 2+ easy responses to graduate
    private const val GRADUATION_INTERVAL_DAYS = 1f     // First review after graduation: 1 day

    // Standard intervals in days for Review Phase
    private const val SECOND_REVIEW_INTERVAL_DAYS = 3f
    private const val THIRD_REVIEW_INTERVAL_DAYS = 7f

    // Ease factor bounds
    private const val MIN_EASE_FACTOR = 1.3f
    private const val MAX_EASE_FACTOR = 2.5f
    private const val DEFAULT_EASE_FACTOR = 2.5f

    /**
     * Calculate next review based on user response quality - HYBRID VERSION
     *
     * @param currentProgress Current WordProgressEntity
     * @param quality User response quality (1=Hard, 2=Medium, 3=Easy)
     * @param currentSessionMaxPosition Current max position in session (for ordering)
     * @return Updated WordProgressEntity with learning/review phase management
     */
    fun calculateNextReview(
        currentProgress: WordProgressEntity,
        quality: Int,
        currentSessionMaxPosition: Int = 100
    ): WordProgressEntity {
        val currentTime = System.currentTimeMillis()

        com.hocalingo.app.core.common.DebugHelper.log(
            "🔥 HYBRID SM-2: quality=$quality, currentReps=${currentProgress.repetitions}, " +
                    "learningPhase=${currentProgress.learningPhase}, sessionPos=${currentProgress.sessionPosition}"
        )

        val result = when {
            // LEARNING PHASE RESPONSES
            currentProgress.learningPhase -> handleLearningPhaseResponse(
                currentProgress, quality, currentTime, currentSessionMaxPosition
            )

            // REVIEW PHASE RESPONSES
            else -> handleReviewPhaseResponse(
                currentProgress, quality, currentTime
            )
        }

        // Debug log result
        val phaseText = if (result.learningPhase) "LEARNING" else "REVIEW"
        val timeText = getTimeUntilReview(result.nextReviewAt)
        com.hocalingo.app.core.common.DebugHelper.log(
            "✅ HYBRID RESULT: $phaseText phase, reps=${result.repetitions}, " +
                    "sessionPos=${result.sessionPosition}, timeText='$timeText'"
        )

        return result
    }

    /**
     * Handle responses during Learning Phase
     * Cards stay in session until graduation
     */
    private fun handleLearningPhaseResponse(
        currentProgress: WordProgressEntity,
        quality: Int,
        currentTime: Long,
        currentSessionMaxPosition: Int
    ): WordProgressEntity {

        return when (quality) {
            QUALITY_HARD -> {
                com.hocalingo.app.core.common.DebugHelper.log("🔴 LEARNING HARD: Reset progress, back of session")

                currentProgress.copy(
                    repetitions = 0, // Reset learning progress
                    intervalDays = LEARNING_HARD_INTERVAL_MINUTES / (24f * 60f),
                    easeFactor = max(MIN_EASE_FACTOR, currentProgress.easeFactor - 0.2f),
                    nextReviewAt = currentTime + (LEARNING_HARD_INTERVAL_MINUTES * 60 * 1000),
                    lastReviewAt = currentTime,
                    learningPhase = true, // Stay in learning
                    sessionPosition = currentSessionMaxPosition + 1, // Back of queue
                    isMastered = false,
                    updatedAt = currentTime
                )
            }

            QUALITY_MEDIUM -> {
                com.hocalingo.app.core.common.DebugHelper.log("🟡 LEARNING MEDIUM: Small progress, back of session")

                currentProgress.copy(
                    repetitions = currentProgress.repetitions + 1,
                    intervalDays = LEARNING_MEDIUM_INTERVAL_MINUTES / (24f * 60f),
                    easeFactor = max(MIN_EASE_FACTOR, currentProgress.easeFactor - 0.1f),
                    nextReviewAt = currentTime + (LEARNING_MEDIUM_INTERVAL_MINUTES * 60 * 1000),
                    lastReviewAt = currentTime,
                    learningPhase = true, // Stay in learning
                    sessionPosition = currentSessionMaxPosition + 1, // Back of queue
                    isMastered = false,
                    updatedAt = currentTime
                )
            }

            QUALITY_EASY -> {
                val newRepetitions = currentProgress.repetitions + 1

                if (newRepetitions >= MIN_REPETITIONS_TO_GRADUATE) {
                    // GRADUATION! Move to review phase
                    com.hocalingo.app.core.common.DebugHelper.log("🎓 GRADUATING: Moving to review phase with 1 day interval")

                    currentProgress.copy(
                        repetitions = newRepetitions,
                        intervalDays = GRADUATION_INTERVAL_DAYS,
                        easeFactor = min(MAX_EASE_FACTOR, currentProgress.easeFactor + 0.1f),
                        nextReviewAt = currentTime + (GRADUATION_INTERVAL_DAYS * 24 * 60 * 60 * 1000).toLong(),
                        lastReviewAt = currentTime,
                        learningPhase = false, // GRADUATE to review phase
                        sessionPosition = null, // No longer in session queue
                        isMastered = false,
                        updatedAt = currentTime
                    )
                } else {
                    // Still learning, back of session
                    com.hocalingo.app.core.common.DebugHelper.log("🟢 LEARNING EASY: Progress made, back of session")

                    currentProgress.copy(
                        repetitions = newRepetitions,
                        intervalDays = LEARNING_EASY_INTERVAL_MINUTES / (24f * 60f),
                        easeFactor = min(MAX_EASE_FACTOR, currentProgress.easeFactor + 0.1f),
                        nextReviewAt = currentTime + (LEARNING_EASY_INTERVAL_MINUTES * 60 * 1000),
                        lastReviewAt = currentTime,
                        learningPhase = true, // Stay in learning
                        sessionPosition = currentSessionMaxPosition + 1, // Back of queue
                        isMastered = false,
                        updatedAt = currentTime
                    )
                }
            }

            else -> {
                com.hocalingo.app.core.common.DebugHelper.logError("Invalid quality score: $quality", Exception())
                currentProgress
            }
        }
    }

    /**
     * Handle responses during Review Phase
     * Standard SM-2 algorithm with time-based scheduling
     */
    private fun handleReviewPhaseResponse(
        currentProgress: WordProgressEntity,
        quality: Int,
        currentTime: Long
    ): WordProgressEntity {

        return when (quality) {
            QUALITY_HARD -> {
                com.hocalingo.app.core.common.DebugHelper.log("🔴 REVIEW HARD: Back to learning phase")

                // Failed review → Back to learning phase
                currentProgress.copy(
                    repetitions = 0,
                    intervalDays = LEARNING_HARD_INTERVAL_MINUTES / (24f * 60f),
                    easeFactor = max(MIN_EASE_FACTOR, currentProgress.easeFactor - 0.2f),
                    nextReviewAt = currentTime + (LEARNING_HARD_INTERVAL_MINUTES * 60 * 1000),
                    lastReviewAt = currentTime,
                    learningPhase = true, // Back to learning
                    sessionPosition = 1, // Front of session (immediate retry)
                    isMastered = false,
                    updatedAt = currentTime
                )
            }

            QUALITY_MEDIUM -> {
                com.hocalingo.app.core.common.DebugHelper.log("🟡 REVIEW MEDIUM: Short review interval")

                currentProgress.copy(
                    repetitions = currentProgress.repetitions + 1,
                    intervalDays = 1f, // 1 day
                    easeFactor = max(MIN_EASE_FACTOR, currentProgress.easeFactor - 0.1f),
                    nextReviewAt = currentTime + (24 * 60 * 60 * 1000),
                    lastReviewAt = currentTime,
                    learningPhase = false, // Stay in review
                    sessionPosition = null,
                    isMastered = false,
                    updatedAt = currentTime
                )
            }

            QUALITY_EASY -> {
                com.hocalingo.app.core.common.DebugHelper.log("🟢 REVIEW EASY: Standard SM-2 progression")

                val newRepetitions = currentProgress.repetitions + 1
                val newEaseFactor = min(MAX_EASE_FACTOR, currentProgress.easeFactor + 0.1f)

                val intervalDays = when (newRepetitions) {
                    3 -> SECOND_REVIEW_INTERVAL_DAYS  // 3 days
                    4 -> THIRD_REVIEW_INTERVAL_DAYS   // 7 days
                    else -> currentProgress.intervalDays * newEaseFactor // Exponential growth
                }

                val nextReviewTime = currentTime + (intervalDays * 24 * 60 * 60 * 1000).toLong()
                val isMastered = newRepetitions >= 5 && intervalDays >= 30f

                currentProgress.copy(
                    repetitions = newRepetitions,
                    intervalDays = intervalDays,
                    easeFactor = newEaseFactor,
                    nextReviewAt = nextReviewTime,
                    lastReviewAt = currentTime,
                    learningPhase = false, // Stay in review
                    sessionPosition = null,
                    isMastered = isMastered,
                    updatedAt = currentTime
                )
            }

            else -> {
                com.hocalingo.app.core.common.DebugHelper.logError("Invalid quality score: $quality", Exception())
                currentProgress
            }
        }
    }

    /**
     * Get human-readable time description for next review
     * (Same as before - button texts unchanged)
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
     * Create initial WordProgressEntity for a newly selected concept
     * Starts in learning phase
     */
    fun createInitialProgress(
        conceptId: Int,
        direction: com.hocalingo.app.core.database.entities.StudyDirection,
        sessionPosition: Int = 1
    ): WordProgressEntity {
        val currentTime = System.currentTimeMillis()

        return WordProgressEntity(
            conceptId = conceptId,
            direction = direction,
            repetitions = 0,
            intervalDays = 0f,
            easeFactor = DEFAULT_EASE_FACTOR,
            nextReviewAt = currentTime, // Available immediately
            lastReviewAt = null,
            isSelected = true,
            isMastered = false,
            learningPhase = true, // Start in learning phase
            sessionPosition = sessionPosition, // Position in session queue
            createdAt = currentTime,
            updatedAt = currentTime
        )
    }

    /**
     * Get study priority score for sorting - UPDATED FOR HYBRID
     */
    fun getStudyPriority(progress: WordProgressEntity, currentTime: Long): Int {
        return when {
            // Learning cards: prioritize by session position
            progress.learningPhase && progress.sessionPosition != null -> {
                1000 - progress.sessionPosition // Lower position = higher priority
            }

            // Review cards: prioritize by overdue time
            !progress.learningPhase -> {
                val overdueDays = ((currentTime - progress.nextReviewAt) / (24 * 60 * 60 * 1000f)).toInt()
                when {
                    overdueDays > 0 -> 500 + overdueDays // Overdue reviews
                    else -> 100 - overdueDays // Future reviews
                }
            }

            // Fallback
            else -> 50
        }
    }

    /**
     * Utility: Check if card should graduate (move to review phase)
     */
    fun shouldGraduate(progress: WordProgressEntity, quality: Int): Boolean {
        return progress.learningPhase &&
                quality == QUALITY_EASY &&
                progress.repetitions + 1 >= MIN_REPETITIONS_TO_GRADUATE
    }
}