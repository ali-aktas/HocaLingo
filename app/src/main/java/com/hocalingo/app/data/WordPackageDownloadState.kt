package com.hocalingo.app.data

/**
 * WordPackageDownloadState - Download durumları
 *
 * Package: app/src/main/java/com/hocalingo/app/data/
 *
 * Download işleminin tüm state'lerini temsil eder
 */
sealed class WordPackageDownloadState {

    /**
     * İndirme başlamadan önceki durum
     */
    data object Idle : WordPackageDownloadState()

    /**
     * İndirme devam ediyor
     * @param bytesDownloaded İndirilen byte sayısı
     * @param totalBytes Toplam byte sayısı
     * @param progress İndirme yüzdesi (0-100)
     */
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val progress: Int
    ) : WordPackageDownloadState()

    /**
     * JSON parse ediliyor
     */
    data object Parsing : WordPackageDownloadState()

    /**
     * Database'e kaydediliyor
     */
    data class Saving(
        val currentWord: Int,
        val totalWords: Int,
        val progress: Int
    ) : WordPackageDownloadState()

    /**
     * İndirme başarıyla tamamlandı
     * @param wordCount Kaydedilen kelime sayısı
     */
    data class Success(val wordCount: Int) : WordPackageDownloadState()

    /**
     * İndirme başarısız oldu
     * @param error Hata mesajı
     * @param throwable İsteğe bağlı exception
     */
    data class Error(
        val error: String,
        val throwable: Throwable? = null
    ) : WordPackageDownloadState()
}

/**
 * Download State'i progress yüzdesine çevir
 */
fun WordPackageDownloadState.toProgressPercentage(): Int {
    return when (this) {
        is WordPackageDownloadState.Idle -> 0
        is WordPackageDownloadState.Downloading -> progress
        is WordPackageDownloadState.Parsing -> 75
        is WordPackageDownloadState.Saving -> 75 + (progress / 4) // 75-100 arası
        is WordPackageDownloadState.Success -> 100
        is WordPackageDownloadState.Error -> 0
    }
}

/**
 * Download State'in kullanıcıya gösterilecek mesajı
 */
fun WordPackageDownloadState.toDisplayMessage(): String {
    return when (this) {
        is WordPackageDownloadState.Idle -> "Hazır"
        is WordPackageDownloadState.Downloading -> "İndiriliyor... %$progress"
        is WordPackageDownloadState.Parsing -> "İşleniyor..."
        is WordPackageDownloadState.Saving -> "Kaydediliyor... %$progress"
        is WordPackageDownloadState.Success -> "✓ $wordCount kelime indirildi"
        is WordPackageDownloadState.Error -> "Hata: $error"
    }
}