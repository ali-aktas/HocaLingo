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
        Index(value = ["is_selected"])
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

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)