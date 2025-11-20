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
        // ✅ Dinamik kelime sayıları
        private const val SHORT_WORD_COUNT = 12
        private const val MEDIUM_WORD_COUNT = 18
        private const val LONG_WORD_COUNT = 25
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


            // 2. Select learned words (dynamic count based on length)
            val words = selectLearnedWords(difficulty, length)

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


            // ✅ Create request with dynamic token limits (NO THINKING - cost optimization)
            val maxTokens = when (length) {
                StoryLength.SHORT -> 200    // ~100 kelime
                StoryLength.MEDIUM -> 400   // ~200 kelime
                StoryLength.LONG -> 600    // ~300 kelime
            }

            val request = GeminiRequest.fromPrompt(prompt, maxTokens)

            val response = geminiApi.generateContent(apiKey, request)

            if (!response.isValid()) {
                DebugHelper.log("❌ Invalid API response")
                return@withContext Result.Error(AppError.ApiError("Empty response from AI"))
            }

            val generatedText = response.getGeneratedText()
            DebugHelper.log("✅ Story generated (${generatedText.length} chars)")

            // ✅ Clean the generated content
            val cleanedContent = cleanStoryContent(generatedText)
            DebugHelper.log("🧹 Content cleaned (${cleanedContent.length} chars)")

            // 5. Create story object
            val story = GeneratedStory(
                title = extractTitle(cleanedContent, type),  // ✅ cleaned version
                content = cleanedContent,                     // ✅ cleaned version
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

    override suspend fun getEnglishWordsForStory(wordIds: List<Int>): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (wordIds.isEmpty()) {
                return@withContext Result.Success(emptyList())
            }

            // Get concepts from database by IDs
            val concepts = database.conceptDao().getConceptsByIds(wordIds)

            // Extract English words
            val englishWords = concepts.map { it.english }

            DebugHelper.log("📚 Loaded ${englishWords.size} words for highlighting")

            Result.Success(englishWords)
        } catch (e: Exception) {
            DebugHelper.logError("Failed to load words for story", e)
            Result.Error(AppError.Unknown(e))
        }
    }

    // ==================== PRIVATE HELPERS ====================

    /**
     * Select learned words based on difficulty AND length
     * ✅ OPTIMIZED: Dynamic word count based on story length
     */
    private suspend fun selectLearnedWords(
        difficulty: StoryDifficulty,
        length: StoryLength
    ): List<WordInfo> {
        val maxIntervalDays = difficulty.maxIntervalDays

        // ✅ Uzunluğa göre kelime sayısı
        val wordCount = when (length) {
            StoryLength.SHORT -> SHORT_WORD_COUNT   // 12
            StoryLength.MEDIUM -> MEDIUM_WORD_COUNT // 18
            StoryLength.LONG -> LONG_WORD_COUNT     // 25
        }

        return database.wordProgressDao().getWordsForStoryGeneration(
            maxIntervalDays = maxIntervalDays,
            limit = wordCount
        )
    }

    /**
     * Build AI prompt for story generation
     * ✅ OPTIMIZED: More explicit rules, better examples
     */
    private fun buildPrompt(
        words: List<WordInfo>,
        topic: String?,
        type: StoryType,
        length: StoryLength
    ): String {
        val wordList = words.joinToString(", ") { it.english }

        val typeInstruction = when (type) {
            StoryType.STORY -> "bir hikaye yaz"
            StoryType.MOTIVATION -> "motivasyon ve ilham verici bir yazı yaz"
            StoryType.DIALOGUE -> "günlük hayattan 2 kişinin karşılıklı konuştuğu bir diyalog yaz"
            StoryType.ARTICLE -> "bilgilendirici bir makale yaz"
        }

        val lengthInstruction = when (length) {
            StoryLength.SHORT -> "Kısa tut (yaklaşık ${length.targetWordCount} kelime)."
            StoryLength.MEDIUM -> "Orta uzunlukta yaz (yaklaşık ${length.targetWordCount} kelime)."
            StoryLength.LONG -> "Detaylı ve uzun yaz (yaklaşık ${length.targetWordCount} kelime)."
        }

        val topicPart = topic?.let { "Konu: $it\n\n" } ?: ""

        return """
        SEN BİR HİKAYE/METİN YAZARISIN. Türkçe olarak $typeInstruction. $lengthInstruction
        
        ${topicPart}Aşağıdaki İngilizce kelimeleri kullan:
        $wordList
        
        ⚠️ ÇOK ÖNEMLİ KURALLAR:
        
        1. KELİMELER MUTLAKA İNGİLİZCE OLACAK
           ❌ YANLIŞ: "bu genç (young) adam"
           ❌ YANLIŞ: "bu **young** adam"
           ✅ DOĞRU: "bu young adam"
        
        2. HİÇBİR BİÇİMLENDİRME YAPMA
           - Markdown kullanma: **bold**, *italic*, _altı çizili_
           - Parantez içinde çeviri yazma: (genç)
           - Sadece düz metin yaz
        
        3. DOĞAL CÜMLELER KUR
           ✅ "Sabah window'dan manzaraya baktım"
           ✅ "Coffee içerken newspaper okudum"
           ✅ "Arkadaşıma gift aldım"
        
        4. BAŞLIK YAZMA, DİREKT HİKAYEYE BAŞLA
        
        5. TÜM KELİMELERİ KULLAN
           - Her kelimeyi en az 1 kez kullan
           - Doğal akış içinde yerleştir
        
        ÖRNEK METİN:
        "Dün evening saatlerinde park'ta yürüyordum. Suddenly bir çocuk bana doğru running geldi. Happy görünüyordu ve elinde küçük bir gift vardı. Beautiful bir andı."
        
        ŞİMDİ SEN YAZ (sadece hikaye, hiç açıklama yapma):
    """.trimIndent()
    }

    /**
     * Clean AI-generated story from formatting issues
     * ✅ Removes markdown, parenthetical translations, asterisks
     * ✅ Ensures clean, readable text
     */
    private fun cleanStoryContent(content: String): String {
        var cleaned = content

        // 1. Remove markdown bold: **word** -> word
        cleaned = cleaned.replace(Regex("""\*\*([^*]+)\*\*"""), "$1")

        // 2. Remove markdown italic: *word* or _word_ -> word
        cleaned = cleaned.replace(Regex("""\*([^*]+)\*"""), "$1")
        cleaned = cleaned.replace(Regex("""_([^_]+)_"""), "$1")

        // 3. Remove parenthetical translations: word (kelime) -> word
        // Matches: word (Turkish translation)
        cleaned = cleaned.replace(Regex("""(\w+)\s*\([^)]+\)"""), "$1")

        // 4. Remove bracketed translations: word [kelime] -> word
        cleaned = cleaned.replace(Regex("""(\w+)\s*\[[^\]]+\]"""), "$1")

        // 5. Remove any remaining asterisks
        cleaned = cleaned.replace("*", "")

        // 6. Clean up multiple spaces
        cleaned = cleaned.replace(Regex("""\s+"""), " ")

        // 7. Fix incomplete last sentence (if ends without punctuation)
        if (cleaned.isNotEmpty() && !cleaned.last().toString().matches(Regex("[.!?]"))) {
            // Find last complete sentence
            val lastPunctuationIndex = cleaned.indexOfLast { it in ".!?" }
            if (lastPunctuationIndex > 0) {
                cleaned = cleaned.substring(0, lastPunctuationIndex + 1)
            } else {
                // If no punctuation found, add period at end
                cleaned += "."
            }
        }

        // 8. Trim whitespace
        cleaned = cleaned.trim()

        return cleaned
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
