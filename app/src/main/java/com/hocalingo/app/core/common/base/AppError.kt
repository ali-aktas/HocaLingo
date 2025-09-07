package com.hocalingo.app.core.common.base

sealed class AppError(
    message: String = "",
    cause: Throwable? = null
) : Exception(message, cause) {

    object Network : AppError("Network connection error")
    object Timeout : AppError("Request timeout")
    data class Http(val code: Int, override val message: String = "") : AppError("HTTP $code: $message")
    data class Database(override val message: String = "") : AppError("Database error: $message")
    data class Unknown(val throwable: Throwable) : AppError("Unknown error", throwable)
    object ValidationError : AppError("Validation failed")
    object NotFound : AppError("Resource not found")
    object Unauthorized : AppError("Unauthorized access")

    // HocaLingo specific errors
    object WordPackageNotFound : AppError("Word package not found")
    object StudySessionEmpty : AppError("No words available for study")
    object DuplicateWord : AppError("Word already exists")
    object InvalidLevel : AppError("Invalid language level")
}

fun Exception.toAppError(): AppError = when (this) {
    is java.net.UnknownHostException -> AppError.Network
    is java.net.SocketTimeoutException -> AppError.Timeout
    is java.io.IOException -> AppError.Network
    is IllegalArgumentException -> AppError.ValidationError
    is AppError -> this
    else -> AppError.Unknown(this)
}

fun AppError.toUserMessage(): String = when (this) {
    AppError.Network -> "İnternet bağlantınızı kontrol edin"
    AppError.Timeout -> "İstek zaman aşımına uğradı"
    is AppError.Http -> "Sunucu hatası ($code)"
    is AppError.Database -> "Veritabanı hatası"
    AppError.ValidationError -> "Geçersiz veri girişi"
    AppError.NotFound -> "İçerik bulunamadı"
    AppError.Unauthorized -> "Erişim izni yok"
    AppError.WordPackageNotFound -> "Kelime paketi bulunamadı"
    AppError.StudySessionEmpty -> "Çalışılacak kelime yok"
    AppError.DuplicateWord -> "Bu kelime zaten mevcut"
    AppError.InvalidLevel -> "Geçersiz dil seviyesi"
    is AppError.Unknown -> "Beklenmeyen bir hata oluştu"
}