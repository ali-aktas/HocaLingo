package com.hocalingo.app.feature.home.di

import com.hocalingo.app.feature.home.data.HomeRepositoryImpl
import com.hocalingo.app.feature.home.domain.HomeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for Home feature
 * Provides repository implementation
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class HomeModule {

    @Binds
    @Singleton
    abstract fun bindHomeRepository(
        homeRepositoryImpl: HomeRepositoryImpl
    ): HomeRepository
}