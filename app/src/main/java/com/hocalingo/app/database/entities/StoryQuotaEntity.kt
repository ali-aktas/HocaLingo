package com.hocalingo.app.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * StoryQuotaEntity - Daily Story Generation Quota Tracking
 *
 * Package: app/src/main/java/com/hocalingo/app/database/entities/
 *
 * Tracks daily story generation limits for premium users.
 * Resets automatically at midnight (00:00).
 *
 * Business Logic:
 * - Premium users: 2 stories/day
 * - Free users: 1 stories (feature disabled)
 * - Auto-reset at midnight (resetTime)
 *
 * Example:
 * date = "2025-11-10"
 * count = 1
 * resetTime = 1731196800000 (midnight timestamp)
 */
@Entity(tableName = "story_quota")
data class StoryQuotaEntity(
    /**
     * Date in YYYY-MM-DD format
     * Example: "2025-11-10"
     */
    @PrimaryKey
    val date: String,

    /**
     * Number of stories generated today
     * Max: 2 for premium users
     */
    @ColumnInfo(name = "count")
    val count: Int = 0,

    /**
     * Midnight timestamp for automatic reset
     * Used to check if new day has started
     */
    @ColumnInfo(name = "reset_time")
    val resetTime: Long
)