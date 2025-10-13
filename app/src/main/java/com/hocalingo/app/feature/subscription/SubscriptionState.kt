package com.hocalingo.app.feature.subscription

/**
 * SubscriptionState
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/subscription/
 *
 * Kullanıcının abonelik durumunu temsil eden immutable data class.
 * DataStore'da cache'lenir, RevenueCat source of truth'tur.
 *
 * @property isPremium Kullanıcı premium üye mi?
 * @property productType Ürün tipi: "monthly", "quarterly", "yearly", null
 * @property expiryDate Abonelik bitiş tarihi (epoch millis), null ise lifetime
 * @property lastSyncTime Son RevenueCat sync zamanı (epoch millis)
 * @property originalPurchaseDate İlk satın alma tarihi (epoch millis)
 * @property isInTrialPeriod Şu an trial sürecinde mi?
 * @property willRenew Abonelik otomatik yenilenecek mi?
 */
data class SubscriptionState(
    val isPremium: Boolean = false,
    val productType: ProductType? = null,
    val expiryDate: Long? = null,
    val lastSyncTime: Long = 0L,
    val originalPurchaseDate: Long? = null,
    val isInTrialPeriod: Boolean = false,
    val willRenew: Boolean = false
) {
    /**
     * Aboneliğin hala geçerli olup olmadığını kontrol eder
     */
    fun isActive(): Boolean {
        if (!isPremium) return false

        // Subscription'lar için expiry date kontrolü
        val currentTime = System.currentTimeMillis()
        return expiryDate?.let { it > currentTime } ?: false
    }

    /**
     * Aboneliğin süresinin dolmasına kalan gün sayısı
     */
    fun daysUntilExpiry(): Int? {
        if (expiryDate == null) return null

        val currentTime = System.currentTimeMillis()
        val diff = expiryDate - currentTime
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }

    companion object {
        /**
         * Free kullanıcı için default state
         */
        val FREE = SubscriptionState(
            isPremium = false,
            productType = null,
            expiryDate = null,
            lastSyncTime = System.currentTimeMillis()
        )
    }
}

/**
 * Ürün Tipleri
 */
enum class ProductType {
    MONTHLY,
    QUARTERLY,
    YEARLY;

    companion object {
        fun fromString(value: String?): ProductType? {
            return when (value?.lowercase()) {
                "monthly" -> MONTHLY
                "quarterly" -> QUARTERLY
                "yearly" -> YEARLY
                else -> null
            }
        }
    }

    fun toReadableString(): String {
        return when (this) {
            MONTHLY -> "Aylık Abonelik"
            QUARTERLY -> "3 Aylık Abonelik"
            YEARLY -> "Yıllık Abonelik"
        }
    }
}