package com.hocalingo.app.core.ads

/**
 * AdState - Reklam Durumları
 *
 * Package: app/src/main/java/com/hocalingo/app/core/ads/
 *
 * Reklam yükleme ve gösterme süreçlerindeki durumları temsil eder.
 * Her ad type için state management.
 */
sealed class AdState {
    /**
     * Reklam henüz yüklenmedi
     */
    data object NotLoaded : AdState()

    /**
     * Reklam yükleniyor
     */
    data object Loading : AdState()

    /**
     * Reklam başarıyla yüklendi ve gösterilmeye hazır
     */
    data class Loaded(val adId: String) : AdState()

    /**
     * Reklam gösteriliyor
     */
    data object Showing : AdState()

    /**
     * Reklam başarıyla gösterildi ve tamamlandı
     */
    data object Completed : AdState()

    /**
     * Reklam yüklenirken hata oluştu
     */
    data class Error(val message: String) : AdState()

    /**
     * Reklam kapatıldı (kullanıcı dismiss etti)
     */
    data object Dismissed : AdState()
}

/**
 * AdType - Reklam Türleri
 *
 * Uygulamada kullanılan farklı reklam tiplerini tanımlar.
 */
enum class AdType {
    /**
     * App açılışında gösterilen rewarded ad
     */
    APP_LAUNCH_REWARDED,

    /**
     * Study ekranında 40 kelime sonrası gösterilen rewarded ad
     */
    STUDY_REWARDED,

    /**
     * Kelime seçme ekranındaki native ad
     */
    NATIVE_SELECTION,

    /**
     * Study ekranındaki native ad
     */
    NATIVE_STUDY
}

/**
 * AdLoadResult - Ad yükleme sonucu
 *
 * Success/Error handling için
 */
sealed class AdLoadResult {
    data class Success(val adType: AdType) : AdLoadResult()
    data class Error(val adType: AdType, val message: String) : AdLoadResult()
}

/**
 * AdShowResult - Ad gösterme sonucu
 *
 * Rewarded ad için özellikle önemli (reward alındı mı?)
 */
sealed class AdShowResult {
    /**
     * Reklam tamamlandı, reward verildi
     */
    data class Rewarded(val adType: AdType, val rewardAmount: Int = 1) : AdShowResult()

    /**
     * Reklam gösterildi ama reward alınmadı (dismissed)
     */
    data class Dismissed(val adType: AdType) : AdShowResult()

    /**
     * Reklam gösterilirken hata oluştu
     */
    data class Error(val adType: AdType, val message: String) : AdShowResult()
}