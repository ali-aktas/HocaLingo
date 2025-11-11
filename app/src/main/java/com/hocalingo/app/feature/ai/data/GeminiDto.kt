package com.hocalingo.app.feature.ai.data

import kotlinx.serialization.Serializable

/**
 * Gemini API Data Transfer Objects
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/ai/data/models/
 *
 * Google Gemini API için request/response modelleri.
 * Kotlinx.serialization kullanılıyor (Retrofit converter ile).
 *
 * API Format:
 * Request: { "contents": [{ "parts": [{ "text": "..." }] }] }
 * Response: { "candidates": [{ "content": { "parts": [{ "text": "..." }] } }] }
 */

// ==================== REQUEST MODELS ====================

/**
 * Main request wrapper for Gemini API
 *
 * Example:
 * ```kotlin
 * GeminiRequest(
 *     contents = listOf(
 *         Content(
 *             parts = listOf(
 *                 Part(text = "Write a story using these words: book, coffee, rain")
 *             )
 *         )
 *     )
 * )
 * ```
 */
@Serializable
data class GeminiRequest(
    val contents: List<Content>
) {
    companion object {
        /**
         * Helper function to create request from simple text prompt
         */
        fun fromPrompt(prompt: String): GeminiRequest {
            return GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt)
                        )
                    )
                )
            )
        }
    }
}

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String
)

// ==================== RESPONSE MODELS ====================

/**
 * Main response wrapper from Gemini API
 *
 * Contains:
 * - candidates: List of generated responses (usually 1)
 * - Each candidate has content with text parts
 */
@Serializable
data class GeminiResponse(
    val candidates: List<Candidate>
) {
    /**
     * Extract generated text from first candidate
     *
     * @return Generated text or empty string if no candidates
     */
    fun getGeneratedText(): String {
        return candidates.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
            ?: ""
    }

    /**
     * Check if response is valid (has content)
     */
    fun isValid(): Boolean {
        return candidates.isNotEmpty() &&
                getGeneratedText().isNotBlank()
    }
}

@Serializable
data class Candidate(
    val content: Content
)