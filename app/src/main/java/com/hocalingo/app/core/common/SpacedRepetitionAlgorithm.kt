package com.hocalingo.app.core.common

import com.hocalingo.app.core.database.entities.WordProgressEntity
import kotlin.math.max
import kotlin.math.min

/**
 * SM-2 Spaced Repetition Algorithm Implementation for HocaLingo
 *
 * Based on the SuperMemo SM-2 algorithm with HocaLingo-specific adaptations:
 * - Easy (3): Perfect recall, longer intervals
 * - Medium (2): Good recall, standard intervals
 * - Hard (1): Poor recall, reset to short intervals
 *
 * Interval Progression:
 * First time Easy → 15 minutes
 * Second time Easy → 1 day
 * Third time Easy → 3 days
 * Fourth+ time Easy → previous_interval * ease_factor
 *
 * Hard → Always 5 minutes + resets progress
 * Medium → 30 minutes + reduces ease factor slightly
 */
object SpacedRepetitionAlgorithm {

    // Quality scores for user responses
    const val QUALITY_HARD = 1      // "Zor" button - poor recall
    const val QUALITY_MEDIUM = 2    // "Orta" button - good recall
    const val QUALITY_EASY = 3      // "Kolay" button - perfect recall

    // Initial intervals in minutes
    private const val INITIAL_HARD_INTERVAL_MINUTES = 5
    private const val INITIAL_MEDIUM_INTERVAL_MINUTES = 30
    private const val INITIAL_EASY_INTERVAL_MINUTES = 15

    // Standard intervals in days for Easy progression
    private const val SECOND_EASY_INTERVAL_DAYS = 1f
    private const val THIRD_EASY_INTERVAL_DAYS = 3f

    // Ease factor bounds
    private const val MIN_EASE_FACTOR = 1.3f
    private const val MAX_EASE_FACTOR = 2.5f
    private const val DEFAULT_EASE_FACTOR = 2.5f

    /**
     * Calculate next review based on user response quality
     *
     * @param currentProgress Current WordProgressEntity
     * @param quality User response quality (1=Hard, 2=Medium, 3=Easy)
     * @return Updated WordProgressEntity with new interval and timing
     */
    fun calculateNextReview(
        currentProgress: WordProgressEntity,
        quality: Int
    ): WordProgressEntity {
        val currentTime = System.currentTimeMillis()

        return when (quality) {
            QUALITY_HARD -> handleHardResponse(currentProgress, currentTime)
            QUALITY_MEDIUM -> handleMediumResponse(currentProgress, currentTime)
            QUALITY_EASY -> handleEasyResponse(currentProgress, currentTime)
            else -> currentProgress // Invalid quality, return unchanged
        }
    }

    /**
     * Handle "Hard" response - Reset progress and short interval
     */
    private fun handleHardResponse(
        currentProgress: WordProgressEntity,
        currentTime: Long
    ): WordProgressEntity {
        val nextReviewTime = currentTime + (INITIAL_HARD_INTERVAL_MINUTES * 60 * 1000)

        return currentProgress.copy(
            repetitions = 0, // Reset progress
            intervalDays = INITIAL_HARD_INTERVAL_MINUTES / (24f * 60f), // Convert to days
            easeFactor = max(MIN_EASE_FACTOR, currentProgress.easeFactor - 0.2f), // Reduce ease
            nextReviewAt = nextReviewTime,
            lastReviewAt = currentTime,
            isMastered = false,
            updatedAt = currentTime
        )
    }

    /**
     * Handle "Medium" response - Moderate interval, slight ease reduction
     */
    private fun handleMediumResponse(
        currentProgress: WordProgressEntity,
        currentTime: Long
    ): WordProgressEntity {
        val nextReviewTime = currentTime + (INITIAL_MEDIUM_INTERVAL_MINUTES * 60 * 1000)

        return currentProgress.copy(
            repetitions = currentProgress.repetitions + 1,
            intervalDays = INITIAL_MEDIUM_INTERVAL_MINUTES / (24f * 60f), // Convert to days
            easeFactor = max(MIN_EASE_FACTOR, currentProgress.easeFactor - 0.1f), // Small reduction
            nextReviewAt = nextReviewTime,
            lastReviewAt = currentTime,
            isMastered = false,
            updatedAt = currentTime
        )
    }

    /**
     * Handle "Easy" response - Progressive intervals based on repetition count
     */
    private fun handleEasyResponse(
        currentProgress: WordProgressEntity,
        currentTime: Long
    ): WordProgressEntity {
        val newRepetitions = currentProgress.repetitions + 1
        val newEaseFactor = min(MAX_EASE_FACTOR, currentProgress.easeFactor + 0.1f)

        val intervalDays = when (newRepetitions) {
            1 -> INITIAL_EASY_INTERVAL_MINUTES / (24f * 60f) // 15 minutes in days
            2 -> SECOND_EASY_INTERVAL_DAYS // 1 day
            3 -> THIRD_EASY_INTERVAL_DAYS  // 3 days
            else -> {
                // Use SM-2 formula: previous_interval * ease_factor
                currentProgress.intervalDays * newEaseFactor
            }
        }

        val nextReviewTime = currentTime + (intervalDays * 24 * 60 * 60 * 1000).toLong()

        // Consider mastered after 5+ successful easy reviews
        val isMastered = newRepetitions >= 5 && intervalDays >= 30f // 1 month+

        return currentProgress.copy(
            repetitions = newRepetitions,
            intervalDays = intervalDays,
            easeFactor = newEaseFactor,
            nextReviewAt = nextReviewTime,
            lastReviewAt = currentTime,
            isMastered = isMastered,
            updatedAt = currentTime
        )
    }

    /**
     * Get human-readable time description for next review
     * Used in UI buttons: "5 dakika sonra", "1 gün sonra", etc.
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
            days >= 30 -> "${(days / 30).toInt()} ay sonra"
            days >= 1 -> "${days.toInt()} gün sonra"
            hours >= 1 -> "${hours.toInt()} saat sonra"
            minutes >= 1 -> "${minutes.toInt()} dakika sonra"
            else -> "1 dakika sonra"
        }
    }

    /**
     * Create initial WordProgressEntity for a newly selected concept
     * Sets up default values for SM-2 algorithm
     */
    fun createInitialProgress(
        conceptId: Int,
        direction: com.hocalingo.app.core.database.entities.StudyDirection
    ): WordProgressEntity {
        val currentTime = System.currentTimeMillis()

        return WordProgressEntity(
            conceptId = conceptId,
            direction = direction,
            repetitions = 0,
            intervalDays = 0f, // Will be set on first review
            easeFactor = DEFAULT_EASE_FACTOR,
            nextReviewAt = currentTime, // Available immediately for first study
            lastReviewAt = null,
            isSelected = true,
            isMastered = false,
            createdAt = currentTime,
            updatedAt = currentTime
        )
    }

    /**
     * Get study priority score for sorting
     * Higher score = higher priority
     * Used to order study queue
     */
    fun getStudyPriority(progress: WordProgressEntity, currentTime: Long): Int {
        val overdueDays = ((currentTime - progress.nextReviewAt) / (24 * 60 * 60 * 1000f)).toInt()

        return when {
            progress.repetitions == 0 -> 1000 + overdueDays // New words highest priority
            overdueDays > 0 -> 500 + overdueDays // Overdue words
            else -> 100 - overdueDays // Future reviews (negative overdue)
        }
    }
}