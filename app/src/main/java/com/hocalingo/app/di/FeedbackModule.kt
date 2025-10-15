package com.hocalingo.app.di

import com.hocalingo.app.core.feedback.FeedbackRepository
import com.hocalingo.app.core.feedback.FeedbackRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * FeedbackModule
 * ==============
 * Hilt Dependency Injection module for Feedback feature
 *
 * Provides:
 * - FeedbackRepository binding to FeedbackRepositoryImpl
 *
 * Package: app/src/main/java/com/hocalingo/app/core/feedback/
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FeedbackModule {

    /**
     * Bind FeedbackRepository interface to its Firebase implementation
     *
     * @param impl FeedbackRepositoryImpl instance
     * @return FeedbackRepository interface
     */
    @Binds
    @Singleton
    abstract fun bindFeedbackRepository(
        impl: FeedbackRepositoryImpl
    ): FeedbackRepository
}