package com.hocalingo.app.feature.ai.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * StoryTextHelper - Text Highlighting Utilities
 *
 * Package: feature/ai/utils/
 *
 * Provides professional text highlighting for AI-generated stories.
 * Highlights English words used in Turkish stories with purple color.
 *
 * Features:
 * - Case-insensitive matching
 * - Handles Turkish suffixes (casual'ı, appearance'ına)
 * - Material Purple 500 color
 * - Bold + colored style
 * - High performance with Regex
 *
 * Usage:
 * ```kotlin
 * val highlighted = StoryTextHelper.highlightWords(
 *     content = story.content,
 *     englishWords = listOf("casual", "appearance", "amount")
 * )
 * Text(text = highlighted)
 * ```
 */
object StoryTextHelper {

    /**
     * Material Purple 500 - Professional highlight color
     */
    private val HighlightColor = Color(0xFF9C27B0)

    /**
     * Highlight English words in story content
     *
     * @param content Turkish story text with English words
     * @param englishWords List of English words to highlight
     * @return AnnotatedString with highlighted words
     */
    fun highlightWords(
        content: String,
        englishWords: List<String>
    ): AnnotatedString {
        if (englishWords.isEmpty()) {
            return AnnotatedString(content)
        }

        val builder = AnnotatedString.Builder(content)

        // Create regex pattern for all words
        // Matches: word, word', word'ı, word'ın, etc.
        val pattern = englishWords.joinToString("|") { word ->
            // Escape special regex characters
            val escaped = Regex.escape(word)
            // Match word with optional Turkish suffixes
            "$escaped('(ı|i|u|ü|a|e|in|ın|un|ün|da|de|den|dan|le|la|yla|yle|nin|nın|nun|nün|ına|ine|a|e)*)?"
        }

        val regex = Regex(pattern, RegexOption.IGNORE_CASE)

        // Find all matches and apply highlighting
        regex.findAll(content).forEach { matchResult ->
            val start = matchResult.range.first
            val end = matchResult.range.last + 1

            builder.addStyle(
                style = SpanStyle(
                    color = HighlightColor,
                    fontWeight = FontWeight.Bold
                ),
                start = start,
                end = end
            )
        }

        return builder.toAnnotatedString()
    }
}