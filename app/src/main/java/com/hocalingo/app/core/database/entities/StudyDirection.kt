package com.hocalingo.app.core.database.entities

import kotlinx.serialization.Serializable

@Serializable
enum class StudyDirection {
    EN_TO_TR, // English to Turkish
    TR_TO_EN  // Turkish to English
}