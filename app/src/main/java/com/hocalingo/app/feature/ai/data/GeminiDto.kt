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
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
) {
    companion object {
        fun fromPrompt(prompt: String): GeminiRequest {
            return GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt)
                        )
                    )
                ),
                generationConfig = GenerationConfig(
                    temperature = 0.9,
                    topK = 40,
                    topP = 0.95,
                    maxOutputTokens = 512,
                    thinkingConfig = ThinkingConfig(
                        thinkingBudget = 0 // 🔴 thinking tamamen kapalı
                    )
                )
            )
        }
    }
}


@Serializable
data class ThinkingConfig(
    val thinkingBudget: Int = 0 // 0 = thinking kapalı, -1 = dynamic, >0 = sabit bütçe
)

@Serializable
data class GenerationConfig(
    val temperature: Double = 0.9,
    val topK: Int = 40,
    val topP: Double = 0.95,
    val maxOutputTokens: Int = 512, // 150-200 kelime hikaye için fazlasıyla yeter
    val thinkingConfig: ThinkingConfig? = null
)


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