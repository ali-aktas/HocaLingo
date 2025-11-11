package com.hocalingo.app.feature.ai.data

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * GeminiApiService - Retrofit Interface for Google Gemini API
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/ai/data/
 *
 * Google Gemini API entegrasyonu için Retrofit interface.
 *
 * API Documentation: https://ai.google.dev/docs
 * Model: gemini-pro (text generation)
 *
 * IMPORTANT: API Key Firebase Remote Config'den alınacak
 * Endpoint: https://generativelanguage.googleapis.com/v1beta/
 *
 * Rate Limits:
 * - Free tier: 15 requests/minute
 * - Paid tier: 60 requests/minute
 *
 * Usage Example:
 * ```kotlin
 * val response = geminiApiService.generateContent(
 *     apiKey = remoteConfig.getString("gemini_api_key"),
 *     request = GeminiRequest(...)
 * )
 * ```
 */
interface GeminiApiService {

    /**
     * Generate content using Gemini Pro model
     *
     * @param apiKey API key from Firebase Remote Config
     * @param request Content generation request
     * @return Generated content response
     *
     * Throws:
     * - HttpException: API errors (400, 429, 500)
     * - SocketTimeoutException: Timeout (30s)
     * - IOException: Network errors
     */
    @POST("models/gemini-pro:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}