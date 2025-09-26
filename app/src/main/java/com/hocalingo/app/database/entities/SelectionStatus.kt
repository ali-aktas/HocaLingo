package com.hocalingo.app.database.entities

import kotlinx.serialization.Serializable

@Serializable
enum class SelectionStatus {
    SELECTED,   // User wants to learn
    HIDDEN,     // User doesn't want to learn
    MASTERED    // User already knows well
}