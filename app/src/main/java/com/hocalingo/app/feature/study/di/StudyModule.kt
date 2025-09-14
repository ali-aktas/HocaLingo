package com.hocalingo.app.feature.study.di

import com.hocalingo.app.feature.study.data.StudyRepositoryImpl
import com.hocalingo.app.feature.study.domain.StudyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module for Study Feature
 *
 * Provides:
 * - StudyRepository binding
 * - Study-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class StudyModule {

    /**
     * Bind StudyRepository interface to its implementation
     */
    @Binds
    @Singleton
    abstract fun bindStudyRepository(
        studyRepositoryImpl: StudyRepositoryImpl
    ): StudyRepository
}