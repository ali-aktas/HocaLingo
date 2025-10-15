package com.hocalingo.app.di

import android.content.Context
import com.hocalingo.app.core.ads.AdCounterDataStore
import com.hocalingo.app.core.ads.AdMobManager
import com.hocalingo.app.core.ads.NativeAdLoader
import com.hocalingo.app.feature.subscription.SubscriptionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AdModule - Hilt Dependency Injection for Ads
 *
 * Package: app/src/main/java/com/hocalingo/app/di/
 *
 * AdMob sisteminin DI modülü.
 * - AdCounterDataStore
 * - AdMobManager
 * - NativeAdLoader
 */
@Module
@InstallIn(SingletonComponent::class)
object AdModule {

    @Provides
    @Singleton
    fun provideAdCounterDataStore(
        @ApplicationContext context: Context
    ): AdCounterDataStore {
        return AdCounterDataStore(context)
    }

    @Provides
    @Singleton
    fun provideAdMobManager(
        @ApplicationContext context: Context,
        subscriptionRepository: SubscriptionRepository,
        adCounterDataStore: AdCounterDataStore
    ): AdMobManager {
        return AdMobManager(
            context = context,
            subscriptionRepository = subscriptionRepository,
            adCounterDataStore = adCounterDataStore
        )
    }

    @Provides
    @Singleton
    fun provideNativeAdLoader(
        @ApplicationContext context: Context,
        subscriptionRepository: SubscriptionRepository
    ): NativeAdLoader {
        return NativeAdLoader(
            context = context,
            subscriptionRepository = subscriptionRepository
        )
    }
}