package com.hocalingo.app.core.database.entities

import kotlinx.serialization.Serializable

@Serializable
enum class SessionType {
    REVIEW,      // Regular spaced repetition
    NEW_WORDS,   // Learning new words
    MIXED        // Mix of new and review
}