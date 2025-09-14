package com.hocalingo.app.core.common

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TextToSpeech Manager for HocaLingo
 *
 * Manages Android native TextToSpeech functionality:
 * - English pronunciation for vocabulary words
 * - Multiple language support for future expansion
 * - Speech state tracking
 * - Error handling and fallbacks
 */
@Singleton
class TextToSpeechManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false

    // Speech state tracking
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    // Supported languages for TTS
    private val supportedLanguages = mapOf(
        "en" to Locale.ENGLISH,
        "tr" to Locale("tr", "TR"),
        "es" to Locale("es", "ES"),
        "fr" to Locale.FRENCH,
        "de" to Locale.GERMAN,
        "it" to Locale.ITALIAN
    )

    init {
        DebugHelper.log("=== TextToSpeechManager BAŞLATILIYOR ===")
        initializeTts()
    }

    /**
     * Initialize TextToSpeech engine
     */
    private fun initializeTts() {
        try {
            textToSpeech = TextToSpeech(context) { status ->
                _isInitializing.value = false

                when (status) {
                    TextToSpeech.SUCCESS -> {
                        isInitialized = true
                        setupTtsDefaults()
                        DebugHelper.log("TextToSpeech initialized successfully")
                    }
                    TextToSpeech.ERROR -> {
                        isInitialized = false
                        DebugHelper.logError("TextToSpeech initialization failed")
                    }
                    else -> {
                        isInitialized = false
                        DebugHelper.logError("TextToSpeech initialization unknown status: $status")
                    }
                }
            }
        } catch (e: Exception) {
            DebugHelper.logError("TextToSpeech initialization exception", e)
            _isInitializing.value = false
            isInitialized = false
        }
    }

    /**
     * Setup default TTS settings
     */
    private fun setupTtsDefaults() {
        textToSpeech?.let { tts ->
            // Set default language to English
            val result = tts.setLanguage(Locale.ENGLISH)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                DebugHelper.logError("English TTS language not supported")
                return
            }

            // Set speech rate (slightly slower for learning)
            tts.setSpeechRate(0.8f)

            // Set pitch (normal)
            tts.setPitch(1.0f)

            // Set up utterance progress listener
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    DebugHelper.log("TTS started speaking: $utteranceId")
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    DebugHelper.log("TTS finished speaking: $utteranceId")
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    DebugHelper.logError("TTS error for utterance: $utteranceId")
                }
            })

            DebugHelper.log("TTS defaults configured successfully")
        }
    }

    /**
     * Speak text with specified language
     *
     * @param text Text to speak
     * @param languageCode Language code (e.g., "en", "tr")
     * @param utteranceId Unique ID for this speech request
     */
    fun speak(text: String, languageCode: String = "en", utteranceId: String = generateUtteranceId()) {
        if (!isInitialized) {
            DebugHelper.logError("TTS not initialized, cannot speak: $text")
            return
        }

        if (text.isBlank()) {
            DebugHelper.logError("Empty text provided to TTS")
            return
        }

        textToSpeech?.let { tts ->
            try {
                // Set language for this utterance
                val locale = supportedLanguages[languageCode] ?: Locale.ENGLISH
                val langResult = tts.setLanguage(locale)

                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    DebugHelper.logError("Language not supported: $languageCode, falling back to English")
                    tts.setLanguage(Locale.ENGLISH)
                }

                // Speak the text
                val speakResult = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

                if (speakResult == TextToSpeech.ERROR) {
                    DebugHelper.logError("TTS speak error for text: $text")
                } else {
                    DebugHelper.log("TTS speaking: '$text' in language: $languageCode")
                }

            } catch (e: Exception) {
                DebugHelper.logError("TTS speak exception", e)
            }
        }
    }

    /**
     * Stop current speech
     */
    fun stop() {
        try {
            textToSpeech?.stop()
            _isSpeaking.value = false
            DebugHelper.log("TTS stopped")
        } catch (e: Exception) {
            DebugHelper.logError("TTS stop exception", e)
        }
    }

    /**
     * Check if a language is supported by TTS
     *
     * @param languageCode Language code to check
     * @return True if language is supported
     */
    fun isLanguageSupported(languageCode: String): Boolean {
        if (!isInitialized) return false

        return textToSpeech?.let { tts ->
            val locale = supportedLanguages[languageCode] ?: return false
            val result = tts.isLanguageAvailable(locale)
            result >= TextToSpeech.LANG_AVAILABLE
        } ?: false
    }

    /**
     * Get list of supported languages
     */
    fun getSupportedLanguages(): List<String> {
        if (!isInitialized) return emptyList()

        return supportedLanguages.keys.filter { isLanguageSupported(it) }
    }

    /**
     * Set speech rate
     *
     * @param rate Speech rate (0.5f = half speed, 2.0f = double speed)
     */
    fun setSpeechRate(rate: Float) {
        textToSpeech?.setSpeechRate(rate.coerceIn(0.1f, 3.0f))
        DebugHelper.log("TTS speech rate set to: $rate")
    }

    /**
     * Set speech pitch
     *
     * @param pitch Speech pitch (0.5f = lower pitch, 2.0f = higher pitch)
     */
    fun setPitch(pitch: Float) {
        textToSpeech?.setPitch(pitch.coerceIn(0.1f, 2.0f))
        DebugHelper.log("TTS pitch set to: $pitch")
    }

    /**
     * Cleanup TTS resources
     */
    fun cleanup() {
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            isInitialized = false
            _isSpeaking.value = false
            DebugHelper.log("TTS cleanup completed")
        } catch (e: Exception) {
            DebugHelper.logError("TTS cleanup exception", e)
        }
    }

    /**
     * Generate unique utterance ID
     */
    private fun generateUtteranceId(): String {
        return "hocalingo_${System.currentTimeMillis()}"
    }

    /**
     * Convenience methods for common use cases
     */
    fun speakEnglishWord(word: String) {
        speak(word, "en", "word_${word.hashCode()}")
    }

    fun speakTurkishWord(word: String) {
        speak(word, "tr", "turkish_${word.hashCode()}")
    }

    fun speakPronunciation(word: String, pronunciation: String?) {
        // If pronunciation guide is available, speak the actual word
        // TODO: In future, we could parse IPA notation and adjust TTS accordingly
        val textToSpeak = pronunciation?.takeIf { it.isNotBlank() } ?: word
        speak(textToSpeak, "en", "pronunciation_${word.hashCode()}")
    }
}

/**
 * Extension functions for easy TTS usage
 */

/**
 * Speak English text
 */
fun TextToSpeechManager.speakEnglish(text: String) {
    speak(text, "en")
}

/**
 * Speak Turkish text
 */
fun TextToSpeechManager.speakTurkish(text: String) {
    speak(text, "tr")
}