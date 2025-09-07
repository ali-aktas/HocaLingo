package com.hocalingo.app.core.database.entities

import androidx.room.TypeConverter

class DatabaseTypeConverters {

    @TypeConverter
    fun fromStudyDirection(direction: StudyDirection): String {
        return direction.name
    }

    @TypeConverter
    fun toStudyDirection(direction: String): StudyDirection {
        return StudyDirection.valueOf(direction)
    }

    @TypeConverter
    fun fromSessionType(sessionType: SessionType): String {
        return sessionType.name
    }

    @TypeConverter
    fun toSessionType(sessionType: String): SessionType {
        return SessionType.valueOf(sessionType)
    }

    @TypeConverter
    fun fromSelectionStatus(status: SelectionStatus): String {
        return status.name
    }

    @TypeConverter
    fun toSelectionStatus(status: String): SelectionStatus {
        return SelectionStatus.valueOf(status)
    }
}