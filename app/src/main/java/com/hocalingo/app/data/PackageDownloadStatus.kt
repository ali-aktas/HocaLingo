package com.hocalingo.app.data

/**
 * Package Download Status
 *
 * Represents the download state of a word package for the current user.
 *
 * Used to display appropriate badges on package cards:
 * - NOT_DOWNLOADED: "İndir" badge
 * - FULLY_DOWNLOADED: "İndirildi ✓" badge
 * - HAS_NEW_WORDS: "X Yeni Kelime" badge
 *
 * Package: app/src/main/java/com/hocalingo/app/data/
 */
sealed class PackageDownloadStatus {

    /**
     * Package has never been downloaded
     */
    data object NotDownloaded : PackageDownloadStatus()

    /**
     * All words from the package are downloaded
     */
    data object FullyDownloaded : PackageDownloadStatus()

    /**
     * Some words are not downloaded yet (can be old or new words)
     *
     * @param newWordsCount Number of words not yet downloaded
     */
    data class HasNewWords(val newWordsCount: Int) : PackageDownloadStatus()
}