package com.hocalingo.app.feature.ai

import com.hocalingo.app.core.base.AppError
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.core.config.RemoteConfigManager
import com.hocalingo.app.database.HocaLingoDatabase
import com.hocalingo.app.database.entities.StoryQuotaEntity
import com.hocalingo.app.feature.ai.data.GeminiApiService
import com.hocalingo.app.feature.ai.data.GeminiRequest
import com.hocalingo.app.feature.ai.models.GeneratedStory
import com.hocalingo.app.feature.ai.models.StoryDifficulty
import com.hocalingo.app.feature.ai.models.StoryLength
import com.hocalingo.app.feature.ai.models.StoryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import com.hocalingo.app.database.dao.WordInfo
import com.hocalingo.app.feature.subscription.SubscriptionRepository

/**
 * StoryRepositoryImpl - Full Business Logic Implementation
 *
 * Package: feature/ai/
 *
 * Handles:
 * - Gemini API calls with retry logic
 * - Word selection from learned vocabulary
 * - Database storage
 * - Daily quota management
 * - Error handling & mapping
 */
@Singleton
class StoryRepositoryImpl @Inject constructor(
    private val geminiApi: GeminiApiService,
    private val database: HocaLingoDatabase,
    private val remoteConfig: RemoteConfigManager,
    private val subscriptionRepository: SubscriptionRepository
) : StoryRepository {

    companion object {
        private const val FREE_DAILY_LIMIT = 1    // Free user
        private const val PREMIUM_DAILY_LIMIT = 2 // Premium user
        private const val WORD_COUNT = 25
    }

    // getDailyLimit() FONKSİYONU EKLE (class içine, companion object'in hemen altına):

    // ✅ YENİ FONKSİYON - EKLE:
    /**
     * Get daily story limit based on premium status
     * Free: 1 story/day
     * Premium: 2 stories/day
     */
    private suspend fun getDailyLimit(): Int {
        val isPremium = subscriptionRepository.isPremium()
        return if (isPremium) PREMIUM_DAILY_LIMIT else FREE_DAILY_LIMIT
    }

    override suspend fun generateStory(
        topic: String?,
        type: StoryType,
        difficulty: StoryDifficulty,
        length: StoryLength
    ): Result<GeneratedStory> = withContext(Dispatchers.IO) {
        try {
            DebugHelper.log("🤖 Starting story generation...")
            DebugHelper.log("   Topic: ${topic ?: "None"}")
            DebugHelper.log("   Type: ${type.displayName}")
            DebugHelper.log("   Difficulty: ${difficulty.displayName}")
            DebugHelper.log("   Length: ${length.displayName}")

            // 1. Check quota
            when (val quotaResult = checkDailyQuota()) {
                is Result.Success -> {
                    if (quotaResult.data <= 0) {
                        DebugHelper.log("❌ Daily quota exceeded")
                        return@withContext Result.Error(AppError.QuotaExceeded)
                    }
                }
                is Result.Error -> return@withContext quotaResult
            }

            // 2. Select learned words
            val words = selectLearnedWords(difficulty)
            if (words.isEmpty()) {
                DebugHelper.log("❌ No learned words found")
                return@withContext Result.Error(AppError.NoWordsAvailable)
            }

            DebugHelper.log("✅ Selected ${words.size} words: ${words.take(5).joinToString()}...")

            // 3. Build prompt
            val prompt = buildPrompt(words, topic, type, length)
            DebugHelper.log("📝 Prompt created (${prompt.length} chars)")

            // 4. Call Gemini API
            val apiKey = try {
                remoteConfig.getGeminiApiKey()
            } catch (e: Exception) {
                DebugHelper.logError("API key error", e)
                return@withContext Result.Error(AppError.ConfigurationError)
            }

            val request = GeminiRequest.fromPrompt(prompt)
            val response = geminiApi.generateContent(apiKey, request)

            if (!response.isValid()) {
                DebugHelper.log("❌ Invalid API response")
                return@withContext Result.Error(AppError.ApiError("Empty response from AI"))
            }

            val generatedText = response.getGeneratedText()
            DebugHelper.log("✅ Story generated (${generatedText.length} chars)")

            // 5. Create story object
            val story = GeneratedStory(
                title = extractTitle(generatedText, type),
                content = generatedText,
                usedWords = words.map { it.id },
                topic = topic,
                type = type,
                difficulty = difficulty,
                length = length
            )

            // 6. Save to database
            database.storyDao().insertStory(story.toEntity())
            DebugHelper.logSuccess("💾 Story saved to database")

            // 7. Update quota
            incrementQuota()
            DebugHelper.log("📊 Quota updated")

            Result.Success(story)

        } catch (e: retrofit2.HttpException) {
            DebugHelper.logError("HTTP error: ${e.code()}", e)
            Result.Error(AppError.ApiError("API request failed: ${e.message()}"))
        } catch (e: java.net.SocketTimeoutException) {
            DebugHelper.logError("Timeout", e)
            Result.Error(AppError.Timeout)
        } catch (e: Exception) {
            DebugHelper.logError("Unexpected error", e)
            Result.Error(AppError.Unknown(e))
        }
    }

    override fun getAllStories(): Flow<List<GeneratedStory>> {
        return database.storyDao().getAllStories()
            .map { entities -> entities.map { GeneratedStory.fromEntity(it) } }
    }

    override suspend fun getStoryById(storyId: String): Result<GeneratedStory> = withContext(Dispatchers.IO) {
        try {
            val entity = database.storyDao().getStoryById(storyId)
            if (entity != null) {
                Result.Success(GeneratedStory.fromEntity(entity))
            } else {
                Result.Error(AppError.NotFound)
            }
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    override suspend fun toggleFavorite(storyId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = database.storyDao().getStoryById(storyId)
            if (entity != null) {
                database.storyDao().updateStory(entity.copy(isFavorite = !entity.isFavorite))
                Result.Success(Unit)
            } else {
                Result.Error(AppError.NotFound)
            }
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    override suspend fun deleteStory(storyId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = database.storyDao().getStoryById(storyId)
            if (entity != null) {
                database.storyDao().deleteStory(entity)
                Result.Success(Unit)
            } else {
                Result.Error(AppError.NotFound)
            }
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    override suspend fun checkDailyQuota(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val today = getTodayString()
            val quota = database.storyDao().getQuotaForDate(today)
            val limit = getDailyLimit()  // ✅ Dynamic limit

            val remaining = if (quota == null) {
                limit
            } else {
                limit - quota.count
            }

            Result.Success(remaining.coerceAtLeast(0))
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    override suspend fun getQuotaInfo(): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            val limit = getDailyLimit()  // ✅ Dynamic limit
            when (val result = checkDailyQuota()) {
                is Result.Success -> {
                    val remaining = result.data
                    val used = limit - remaining
                    Result.Success(Pair(used, limit))
                }
                is Result.Error -> result
            }
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    // ==================== PRIVATE HELPERS ====================

    /**
     * Select learned words based on difficulty
     * Uses interval_days from word_progress table
     */
    private suspend fun selectLearnedWords(difficulty: StoryDifficulty): List<WordInfo> {
        val maxIntervalDays = difficulty.maxIntervalDays

        return database.wordProgressDao().getWordsForStoryGeneration(
            maxIntervalDays = maxIntervalDays,
            limit = WORD_COUNT
        )
    }

    /**
     * Build AI prompt for story generation
     */
    private fun buildPrompt(
        words: List<WordInfo>,
        topic: String?,
        type: StoryType,
        length: StoryLength
    ): String {
        val wordList = words.joinToString(", ") { it.english }

        val typeInstruction = when (type) {
            StoryType.STORY -> "bir kısa hikaye yaz"
            StoryType.MOTIVATION -> "motivasyon verici bir yazı yaz"
            StoryType.DIALOGUE -> "günlük hayattan bir diyalog yaz"
            StoryType.ARTICLE -> "bilgilendirici bir makale yaz"
        }

        val lengthInstruction = when (length) {
            StoryLength.SHORT -> "Kısa tut (yaklaşık ${length.targetWordCount} kelime)."
            StoryLength.MEDIUM -> "Orta uzunlukta yaz (yaklaşık ${length.targetWordCount} kelime)."
            StoryLength.LONG -> "Detaylı ve uzun yaz (yaklaşık ${length.targetWordCount} kelime)."
        }

        val topicPart = topic?.let { "Konu: $it\n" } ?: ""

        return """
            Türkçe olarak $typeInstruction. $lengthInstruction
            
            ${topicPart}Aşağıdaki İngilizce kelimeleri kullan (kelimeleri aynen İngilizce olarak kullan):
            $wordList
            
            KURALLAR:
            1. Verilen kelimeleri hikaye içinde İngilizce olarak kullan (Türkçe çevirme)
            2. Her kelimeyi doğal bir şekilde cümlelere yerleştir
            3. Akıcı ve okunabilir Türkçe yaz
            4. Kelimeler bold veya italic olmasın, düz metin kullan
            5. Başlık ekleme, direkt hikayeye başla
            
            Örnek: "Sabah uyandığımda window'dan güneşi gördüm. Coffee içerken newspaper okudum."
        """.trimIndent()
    }

    /**
     * Extract title from generated content
     * First sentence or first 50 chars
     */
    private fun extractTitle(content: String, type: StoryType): String {
        val firstLine = content.lines().firstOrNull { it.isNotBlank() } ?: content
        val title = if (firstLine.length > 50) {
            firstLine.take(50) + "..."
        } else {
            firstLine
        }
        return title.ifBlank { type.displayName }
    }

    /**
     * Increment today's quota count
     */
    private suspend fun incrementQuota() {
        val today = getTodayString()
        val existing = database.storyDao().getQuotaForDate(today)

        if (existing == null) {
            // Create new quota entry
            database.storyDao().insertQuota(
                StoryQuotaEntity(
                    date = today,
                    count = 1,
                    resetTime = getMidnightTimestamp()
                )
            )
        } else {
            // Increment existing
            database.storyDao().insertQuota(
                existing.copy(count = existing.count + 1)
            )
        }
    }

    /**
     * Get today's date string (YYYY-MM-DD)
     */
    private fun getTodayString(): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return format.format(Date())
    }

    /**
     * Get midnight timestamp for quota reset
     */
    private fun getMidnightTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
