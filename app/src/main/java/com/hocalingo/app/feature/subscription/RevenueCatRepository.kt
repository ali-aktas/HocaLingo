package com.hocalingo.app.feature.subscription

import android.app.Activity
import com.hocalingo.app.core.base.AppError
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.core.common.DebugHelper
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.interfaces.PurchaseCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * RevenueCatRepository - Profesyonel & Doğru Implementasyon ✅
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/subscription/
 *
 * RevenueCat SDK ile tam entegrasyon
 * - Callback-based API'yi coroutine'e çevirme
 * - Error handling ve user cancellation
 * - Local cache management
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
     * Paketi satın alır - RevenueCat PurchaseCallback ile
     */
    override suspend fun purchasePackage(
        activity: Activity,
        packageToPurchase: Package
    ): Result<CustomerInfo> = withContext(Dispatchers.Main) {
        try {
            DebugHelper.log("💳 Purchasing package: ${packageToPurchase.identifier}")

            val customerInfo = suspendCoroutine<CustomerInfo> { continuation ->
                val purchaseParams = PurchaseParams.Builder(activity, packageToPurchase).build()

                Purchases.sharedInstance.purchase(
                    purchaseParams,
                    object : PurchaseCallback {
                        override fun onCompleted(
                            storeTransaction: StoreTransaction,
                            customerInfo: CustomerInfo
                        ) {
                            DebugHelper.logSuccess("✅ Purchase successful")
                            continuation.resume(customerInfo)
                        }

                        override fun onError(error: PurchasesError, userCancelled: Boolean) {
                            if (userCancelled) {
                                DebugHelper.log("ℹ️ User cancelled purchase")
                                continuation.resumeWithException(Exception("Satın alma iptal edildi"))
                            } else {
                                DebugHelper.logError("❌ Purchase error", Exception(error.message))
                                continuation.resumeWithException(Exception(error.message))
                            }
                        }
                    }
                )
            }

            val subscriptionState = parseCustomerInfo(customerInfo)
            subscriptionDataStore.saveSubscriptionState(subscriptionState)

            DebugHelper.logSuccess("✅ Purchase completed: isPremium=${subscriptionState.isPremium}")
            Result.Success(customerInfo)

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

            subscriptionDataStore.saveSubscriptionState(subscriptionState)

            DebugHelper.logSuccess("✅ Purchases restored: isPremium=${subscriptionState.isPremium}")
            Result.Success(customerInfo)

        } catch (e: Exception) {
            DebugHelper.logError("❌ Restore error", e)
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Premium durumunu kontrol eder (cache'den)
     */
    override suspend fun isPremium(): Boolean {
        return subscriptionDataStore.getSubscriptionState().first().isPremium
    }

    /**
     * CustomerInfo'yu SubscriptionState'e parse eder
     */
    private fun parseCustomerInfo(customerInfo: CustomerInfo): SubscriptionState {
        val entitlement = customerInfo.entitlements[PREMIUM_ENTITLEMENT_ID]

        if (entitlement == null || !entitlement.isActive) {
            return SubscriptionState.FREE
        }

        val expirationDate = entitlement.expirationDate
        val productIdentifier = entitlement.productIdentifier
        val originalPurchaseDate = entitlement.originalPurchaseDate
        val willRenew = entitlement.willRenew

        return SubscriptionState(
            isPremium = true,
            productType = parseProductType(productIdentifier),
            expiryDate = expirationDate?.time,
            lastSyncTime = System.currentTimeMillis(),
            originalPurchaseDate = originalPurchaseDate?.time,
            isInTrialPeriod = entitlement.periodType == PeriodType.TRIAL,
            willRenew = willRenew
        )
    }

    /**
     * Product identifier'dan ProductType çıkarır
     */
    private fun parseProductType(productId: String?): ProductType? {
        return when {
            productId == null -> null
            productId.contains("monthly", ignoreCase = true) -> ProductType.MONTHLY
            productId.contains("quarterly", ignoreCase = true) -> ProductType.QUARTERLY
            productId.contains("yearly", ignoreCase = true) ||
                    productId.contains("annual", ignoreCase = true) -> ProductType.YEARLY
            else -> null
        }
    }
}