package com.hocalingo.app.feature.subscription

import com.hocalingo.app.core.base.AppError
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.core.common.DebugHelper
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitRestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RevenueCatRepository
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/subscription/
 *
 * RevenueCat SDK ile iletişimi yöneten repository implementation.
 * SubscriptionDataStore ile birlikte çalışarak offline cache sağlar.
 */
@Singleton
class RevenueCatRepository @Inject constructor(
    private val subscriptionDataStore: SubscriptionDataStore
) : SubscriptionRepository {

    companion object {
        private const val PREMIUM_ENTITLEMENT_ID = "premium"
    }

    /**
     * RevenueCat'ten güncel abonelik durumunu senkronize eder
     */
    override suspend fun syncSubscriptionStatus(): Result<SubscriptionState> = withContext(Dispatchers.IO) {
        try {
            DebugHelper.log("🔄 Syncing subscription status from RevenueCat...")

            val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
            val subscriptionState = parseCustomerInfo(customerInfo)

            // Cache'e kaydet
            subscriptionDataStore.saveSubscriptionState(subscriptionState)

            DebugHelper.logSuccess("✅ Subscription synced: isPremium=${subscriptionState.isPremium}")
            Result.Success(subscriptionState)

        } catch (e: Exception) {
            DebugHelper.logError("❌ Sync error", e)
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Local cache'den abonelik durumunu okur
     */
    override fun getLocalSubscriptionState(): Flow<SubscriptionState> {
        return subscriptionDataStore.getSubscriptionState()
    }

    /**
     * Mevcut paketleri RevenueCat'ten alır
     */
    override suspend fun getAvailablePackages(): Result<List<Package>> = withContext(Dispatchers.IO) {
        try {
            DebugHelper.log("📦 Fetching available packages...")

            val offerings = Purchases.sharedInstance.awaitOfferings()
            val currentOffering = offerings.current

            if (currentOffering == null) {
                DebugHelper.logError("❌ No current offering found")
                return@withContext Result.Error(AppError.NotFound)
            }

            val packages = currentOffering.availablePackages
            DebugHelper.logSuccess("✅ Found ${packages.size} packages")

            Result.Success(packages)

        } catch (e: Exception) {
            DebugHelper.logError("❌ Failed to fetch packages", e)
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Paketi satın alır
     * NOT: Activity reference gerekli (Google Billing Library)
     */
    override suspend fun purchasePackage(packageToPurchase: Package): Result<CustomerInfo> = withContext(Dispatchers.IO) {
        try {
            DebugHelper.log("💳 Purchasing package: ${packageToPurchase.identifier}")

            // Activity gerekiyor ama şimdilik hata döneceğiz
            // ViewModel'den Activity pass edilmesi gerekecek
            Result.Error(AppError.Unknown(Exception("Activity required for purchase")))

        } catch (e: Exception) {
            DebugHelper.logError("❌ Purchase error", e)
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Önceki satın almaları geri yükler
     */
    override suspend fun restorePurchases(): Result<CustomerInfo> = withContext(Dispatchers.IO) {
        try {
            DebugHelper.log("🔄 Restoring purchases...")

            val customerInfo = Purchases.sharedInstance.awaitRestore()
            val subscriptionState = parseCustomerInfo(customerInfo)

            // Cache'e kaydet
            subscriptionDataStore.saveSubscriptionState(subscriptionState)

            DebugHelper.logSuccess("✅ Purchases restored: isPremium=${subscriptionState.isPremium}")
            Result.Success(customerInfo)

        } catch (e: Exception) {
            DebugHelper.logError("❌ Restore error", e)
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Premium durumunu hızlıca kontrol eder
     */
    override suspend fun isPremium(): Boolean {
        return try {
            val state = subscriptionDataStore.getSubscriptionState().first()
            state.isPremium && state.isActive()
        } catch (e: Exception) {
            DebugHelper.logError("Failed to check premium status", e)
            false
        }
    }

    /**
     * CustomerInfo'dan SubscriptionState'e dönüşüm
     */
    private fun parseCustomerInfo(customerInfo: CustomerInfo): SubscriptionState {
        val entitlement = customerInfo.entitlements[PREMIUM_ENTITLEMENT_ID]
        val isActive = entitlement?.isActive == true

        if (!isActive) {
            return SubscriptionState.FREE
        }

        // Product identifier'dan plan tipini çıkar
        val productId = entitlement.productIdentifier // "hocalingo_premium:monthly"
        val productType = when {
            productId.contains("monthly") -> ProductType.MONTHLY
            productId.contains("quarterly") -> ProductType.QUARTERLY
            productId.contains("yearly") -> ProductType.YEARLY
            else -> null
        }

        return SubscriptionState(
            isPremium = true,
            productType = productType,
            expiryDate = entitlement.expirationDate?.time,
            lastSyncTime = System.currentTimeMillis(),
            originalPurchaseDate = entitlement.originalPurchaseDate?.time,
            isInTrialPeriod = entitlement.periodType == com.revenuecat.purchases.PeriodType.TRIAL,
            willRenew = entitlement.willRenew
        )
    }
}