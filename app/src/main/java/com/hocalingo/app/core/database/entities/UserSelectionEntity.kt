package com.hocalingo.app.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_selections",
    foreignKeys = [
        ForeignKey(
            entity = ConceptEntity::class,
            parentColumns = ["id"],
            childColumns = ["concept_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["concept_id"], unique = true)]
)
data class UserSelectionEntity(
    @PrimaryKey
    @ColumnInfo(name = "concept_id")
    val conceptId: Int,

    @ColumnInfo(name = "status")
    val status: SelectionStatus,

    @ColumnInfo(name = "selected_at")
    val selectedAt: Long,

    @ColumnInfo(name = "package_level")
    val packageLevel: String // A1, B1 etc.
)