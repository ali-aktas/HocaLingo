package com.hocalingo.app.feature.profile.di

import com.hocalingo.app.feature.profile.data.ProfileRepositoryImpl
import com.hocalingo.app.feature.profile.domain.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module for Profile Feature
 * Provides ProfileRepository binding
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {

    /**
     * Bind ProfileRepository interface to its implementation
     */
    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        profileRepositoryImpl: ProfileRepositoryImpl
    ): ProfileRepository
}