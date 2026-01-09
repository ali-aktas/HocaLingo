package com.hocalingo.app.di

import com.hocalingo.app.feature.subscription.RevenueCatRepository
import com.hocalingo.app.feature.subscription.SubscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * SubscriptionModule
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/subscription/
 *
 * Hilt Dependency Injection modülü.
 * SubscriptionRepository'nin RevenueCatRepository implementasyonunu bağlar.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SubscriptionModule {

    /**
     * SubscriptionRepository → RevenueCatRepository binding
     */
    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(
        revenueCatRepository: RevenueCatRepository
    ): SubscriptionRepository
}