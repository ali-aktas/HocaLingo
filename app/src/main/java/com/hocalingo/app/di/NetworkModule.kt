package com.hocalingo.app.di

import com.hocalingo.app.feature.ai.data.GeminiApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * NetworkModule - Hilt DI Module for Network Layer
 *
 * Package: app/src/main/java/com/hocalingo/app/di/
 *
 * Provides:
 * - OkHttpClient (with logging, timeouts)
 * - Retrofit (with kotlinx.serialization)
 * - GeminiApiService
 *
 * Configuration:
 * - Base URL: Gemini API endpoint
 * - Timeout: 30 seconds (AI generation can take time)
 * - Logging: Enabled in debug builds
 * - Serialization: kotlinx.serialization (not Gson)
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/"
    private const val TIMEOUT_SECONDS = 30L

    /**
     * Provide JSON serializer
     * Configured for API compatibility
     */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
        encodeDefaults = true
    }

    /**
     * Provide OkHttpClient for network calls
     *
     * Features:
     * - 30s timeout (AI generation)
     * - Logging interceptor (debug only)
     * - Retry on connection failure
     */
    @Provides
    @Singleton
    @GeminiHttpClient
    fun provideGeminiOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().apply {
            // Timeouts (AI can take time)
            connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)

            // Logging (debug builds only)
            if (com.hocalingo.app.BuildConfig.DEBUG) {
                val logging = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
                addInterceptor(logging)
            }

            // Retry on failure (network issues)
            retryOnConnectionFailure(true)
        }.build()
    }

    /**
     * Provide Retrofit instance for Gemini API
     *
     * Base URL: https://generativelanguage.googleapis.com/v1beta/
     * Converter: kotlinx.serialization (JSON)
     */
    @Provides
    @Singleton
    @GeminiRetrofit
    fun provideGeminiRetrofit(
        @GeminiHttpClient okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    /**
     * Provide GeminiApiService
     *
     * Retrofit will implement this interface
     */
    @Provides
    @Singleton
    fun provideGeminiApiService(
        @GeminiRetrofit retrofit: Retrofit
    ): GeminiApiService {
        return retrofit.create(GeminiApiService::class.java)
    }
}

/**
 * Qualifier annotations for multiple network clients
 * (Future: May need separate clients for different APIs)
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GeminiHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GeminiRetrofit