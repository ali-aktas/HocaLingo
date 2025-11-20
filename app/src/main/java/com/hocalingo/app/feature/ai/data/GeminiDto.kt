package com.hocalingo.app.feature.ai.data

import kotlinx.serialization.Serializable

/**
 * Gemini API Data Transfer Objects
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/ai/data/
 *
 * Google Gemini API için request/response modelleri.
 * Kotlinx.serialization kullanılıyor (Retrofit converter ile).
 *
 * API Format:
 * Request: { "contents": [{ "parts": [{ "text": "..." }] }], "generationConfig": {...} }
 * Response: { "candidates": [{ "content": { "parts": [{ "text": "..." }] } }] }
 */

// ==================== REQUEST MODELS ====================

/**
 * Main request wrapper for Gemini API
 *
 * Example:
 * ```kotlin
 * GeminiRequest.fromPrompt("Write a story...", maxTokens = 800)
 * ```
 */
@Serializable
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
) {
    companion object {
        /**
         * Create request from prompt with dynamic token limit
         *
         * @param prompt The text prompt for AI
         * @param maxTokens Maximum output tokens (default: 512)
         * @return Configured GeminiRequest
         */
        fun fromPrompt(prompt: String, maxTokens: Int = 512): GeminiRequest {
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
                    maxOutputTokens = maxTokens,
                    thinkingConfig = ThinkingConfig(
                        thinkingBudget = 0  // 0 = thinking disabled (cost optimization)
                    )
                )
            )
        }
    }
}

/**
 * Thinking configuration for Gemini 2.5 models
 * Set thinkingBudget = 0 to disable thinking and reduce cost
 */
@Serializable
data class ThinkingConfig(
    val thinkingBudget: Int = 0  // 0 = disabled, -1 = dynamic, >0 = fixed budget
)

/**
 * Generation configuration for controlling AI output
 */
@Serializable
data class GenerationConfig(
    val temperature: Double = 0.9,      // Creativity (0.0-1.0)
    val topK: Int = 40,                 // Top K sampling
    val topP: Double = 0.95,            // Top P sampling
    val maxOutputTokens: Int = 512,    // Max tokens to generate
    val thinkingConfig: ThinkingConfig? = null
)

/**
 * Content wrapper for message parts
 */
@Serializable
data class Content(
    val parts: List<Part>? = null  // Nullable to handle empty responses
)

/**
 * Individual message part (text)
 */
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
     * @return Generated text or empty string if no candidates/parts
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

/**
 * Individual candidate response
 */
@Serializable
data class Candidate(
    val content: Content
)