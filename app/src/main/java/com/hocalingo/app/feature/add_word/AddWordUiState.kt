package com.hocalingo.app.feature.add_word

/**
 * UI State for Add Word Screen
 * Manages form data, validation, and user feedback
 */
data class AddWordUiState(
    // Form Data
    val englishWord: String = "",
    val turkishWord: String = "",
    val englishExample: String = "",
    val turkishExample: String = "",

    // Validation States
    val englishWordError: String? = null,
    val turkishWordError: String? = null,
    val englishExampleError: String? = null,
    val turkishExampleError: String? = null,

    // UI States
    val isLoading: Boolean = false,
    val isFormValid: Boolean = false,
    val showSuccessAnimation: Boolean = false,
    val error: String? = null,

    // Progress tracking
    val userWordsCount: Int = 0
) {
    /**
     * Check if required fields are filled and valid
     */
    val canSubmit: Boolean
        get() = englishWord.isNotBlank() &&
                turkishWord.isNotBlank() &&
                englishWordError == null &&
                turkishWordError == null &&
                !isLoading
}

/**
 * Form data container for word creation
 */
data class WordFormData(
    val english: String,
    val turkish: String,
    val exampleEn: String? = null,
    val exampleTr: String? = null
) {
    fun validate(): ValidationResult {
        val errors = mutableListOf<FieldError>()

        val ENGLISH_WORD_REGEX = Regex("^[a-zA-Z\\s'-]+$")


        // English word validation
        when {
            english.isBlank() -> errors.add(FieldError.EnglishRequired)
            english.length < 2 -> errors.add(FieldError.EnglishTooShort)
            english.length > 50 -> errors.add(FieldError.EnglishTooLong)
            !ENGLISH_WORD_REGEX.matches(english) -> errors.add(FieldError.EnglishInvalidFormat)
        }

        // Turkish word validation
        when {
            turkish.isBlank() -> errors.add(FieldError.TurkishRequired)
            turkish.length < 2 -> errors.add(FieldError.TurkishTooShort)
            turkish.length > 50 -> errors.add(FieldError.TurkishTooLong)
        }

        // Example validation (if provided)
        exampleEn?.let { example ->
            if (example.isNotBlank() && example.length > 200) {
                errors.add(FieldError.EnglishExampleTooLong)
            }
        }

        exampleTr?.let { example ->
            if (example.isNotBlank() && example.length > 200) {
                errors.add(FieldError.TurkishExampleTooLong)
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
}

/**
 * Validation result with specific field errors
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<FieldError>
) {
    fun getErrorForField(field: FormField): String? {
        return errors.firstOrNull { it.field == field }?.message
    }
}

/**
 * Form field types for error mapping
 */
enum class FormField {
    ENGLISH_WORD,
    TURKISH_WORD,
    ENGLISH_EXAMPLE,
    TURKISH_EXAMPLE
}

/**
 * Specific validation errors with user-friendly messages
 */
sealed class FieldError(val field: FormField, val message: String) {
    object EnglishRequired : FieldError(FormField.ENGLISH_WORD, "İngilizce kelime gerekli")
    object EnglishTooShort : FieldError(FormField.ENGLISH_WORD, "En az 2 karakter olmalı")
    object EnglishTooLong : FieldError(FormField.ENGLISH_WORD, "En fazla 50 karakter olabilir")
    object EnglishInvalidFormat : FieldError(FormField.ENGLISH_WORD, "Sadece İngilizce harfler kullanın")

    object TurkishRequired : FieldError(FormField.TURKISH_WORD, "Türkçe kelime gerekli")
    object TurkishTooShort : FieldError(FormField.TURKISH_WORD, "En az 2 karakter olmalı")
    object TurkishTooLong : FieldError(FormField.TURKISH_WORD, "En fazla 50 karakter olabilir")

    object EnglishExampleTooLong : FieldError(FormField.ENGLISH_EXAMPLE, "Örnek cümle çok uzun (max 200 karakter)")
    object TurkishExampleTooLong : FieldError(FormField.TURKISH_EXAMPLE, "Örnek cümle çok uzun (max 200 karakter)")
}

/**
 * User Events for Add Word Screen
 */
sealed interface AddWordEvent {
    // Form Input Events
    data class EnglishWordChanged(val value: String) : AddWordEvent
    data class TurkishWordChanged(val value: String) : AddWordEvent
    data class EnglishExampleChanged(val value: String) : AddWordEvent
    data class TurkishExampleChanged(val value: String) : AddWordEvent

    // Action Events
    data object SubmitWord : AddWordEvent
    data object ClearForm : AddWordEvent
    data object DismissError : AddWordEvent
    data object DismissSuccess : AddWordEvent

    // Navigation Events
    data object NavigateBack : AddWordEvent
    data object NavigateToStudy : AddWordEvent
}

/**
 * One-time effects for Add Word Screen
 */
sealed interface AddWordEffect {
    data object NavigateBack : AddWordEffect
    data object NavigateToStudy : AddWordEffect
    data class ShowMessage(val message: String) : AddWordEffect
    data class ShowError(val error: String) : AddWordEffect
    data object ShowSuccessAndNavigate : AddWordEffect
    data object ClearFormFields : AddWordEffect
}