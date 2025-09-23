package com.hocalingo.app.di

import com.hocalingo.app.feature.notification.data.NotificationRepositoryImpl
import com.hocalingo.app.feature.notification.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Notification Module for Hilt Dependency Injection
 * ✅ Provides notification repository
 * ✅ Singleton scope for efficiency
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: NotificationRepositoryImpl
    ): NotificationRepository
}