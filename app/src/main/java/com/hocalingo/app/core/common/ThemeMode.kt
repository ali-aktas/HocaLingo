package com.hocalingo.app.core.common

/**
 * Theme Mode Options for the app
 * Modern Android approach: Light, Dark, System
 */
enum class ThemeMode(val value: String) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system"); // Follows system theme

    companion object {
        fun fromString(value: String): ThemeMode {
            return values().find { it.value == value } ?: SYSTEM
        }
    }
}

/**
 * Study Direction Options
 * Controls which language appears on the front of study cards
 */
enum class StudyDirection(val value: String) {
    EN_TO_TR("en_to_tr"),  // English front, Turkish back
    TR_TO_EN("tr_to_en"),  // Turkish front, English back
    MIXED("mixed");        // Random front/back

    companion object {
        fun fromString(value: String): StudyDirection {
            return values().find { it.value == value } ?: EN_TO_TR
        }
    }
}