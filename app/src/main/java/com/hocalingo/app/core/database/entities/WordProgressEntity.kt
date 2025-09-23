package com.hocalingo.app.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "word_progress",
    primaryKeys = ["concept_id", "direction"],
    foreignKeys = [
        ForeignKey(
            entity = ConceptEntity::class,
            parentColumns = ["id"],
            childColumns = ["concept_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["concept_id"]),
        Index(value = ["next_review_at"]),
        Index(value = ["is_selected"]),
        Index(value = ["learning_phase"]),
        Index(value = ["session_position"])
    ]
)
data class WordProgressEntity(
    @ColumnInfo(name = "concept_id")
    val conceptId: Int,

    @ColumnInfo(name = "direction")
    val direction: StudyDirection, // EN_TO_TR, TR_TO_EN

    @ColumnInfo(name = "repetitions")
    val repetitions: Int = 0,

    @ColumnInfo(name = "interval_days")
    val intervalDays: Float = 1f,

    @ColumnInfo(name = "ease_factor")
    val easeFactor: Float = 2.5f,

    @ColumnInfo(name = "next_review_at")
    val nextReviewAt: Long,

    @ColumnInfo(name = "last_review_at")
    val lastReviewAt: Long? = null,

    @ColumnInfo(name = "is_selected")
    val isSelected: Boolean = false,

    @ColumnInfo(name = "is_mastered")
    val isMastered: Boolean = false,

    /**
     * NEW: Learning Phase Management
     *
     * true = Card is in learning phase (stays in current session)
     * false = Card is in review phase (time-based scheduling)
     */
    @ColumnInfo(name = "learning_phase")
    val learningPhase: Boolean = true,

    /**
     * NEW: Session Position for Learning Cards
     *
     * Used to order cards within current session
     * Lower numbers = shown first
     * null = not in current session (review cards)
     */
    @ColumnInfo(name = "session_position")
    val sessionPosition: Int? = null,

    /**
     * NEW: Failure tracking for algorithm improvements
     * Counts consecutive failures for leech detection
     */
    @ColumnInfo(name = "failures")
    val failures: Int? = 0,

    /**
     * NEW: Success streak tracking for recovery system
     * Counts consecutive successful reviews for failure recovery
     */
    @ColumnInfo(name = "success_streak")
    val successStreak: Int? = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Helper method to check if card should stay in current session
     */
    val staysInSession: Boolean
        get() = learningPhase && sessionPosition != null

    /**
     * Helper method to check if card is graduated (moved to review phase)
     */
    val isGraduated: Boolean
        get() = !learningPhase && repetitions >= 2
}
