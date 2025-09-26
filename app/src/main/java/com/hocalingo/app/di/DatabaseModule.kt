package com.hocalingo.app.di

import android.content.Context
import com.hocalingo.app.database.HocaLingoDatabase
import com.hocalingo.app.database.dao.CombinedDataDao
import com.hocalingo.app.database.dao.ConceptDao
import com.hocalingo.app.database.dao.DailyStatsDao
import com.hocalingo.app.database.dao.StudySessionDao
import com.hocalingo.app.database.dao.UserPreferencesDao
import com.hocalingo.app.database.dao.UserSelectionDao
import com.hocalingo.app.database.dao.WordPackageDao
import com.hocalingo.app.database.dao.WordProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Database Module for Hilt DI
 * Provides Room database and DAO instances
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideHocaLingoDatabase(@ApplicationContext context: Context): HocaLingoDatabase {
        return HocaLingoDatabase.getDatabase(context)
    }

    @Provides
    fun provideConceptDao(database: HocaLingoDatabase): ConceptDao {
        return database.conceptDao()
    }

    @Provides
    fun provideWordProgressDao(database: HocaLingoDatabase): WordProgressDao {
        return database.wordProgressDao()
    }

    @Provides
    fun provideUserSelectionDao(database: HocaLingoDatabase): UserSelectionDao {
        return database.userSelectionDao()
    }

    @Provides
    fun provideStudySessionDao(database: HocaLingoDatabase): StudySessionDao {
        return database.studySessionDao()
    }

    @Provides
    fun provideDailyStatsDao(database: HocaLingoDatabase): DailyStatsDao {
        return database.dailyStatsDao()
    }

    @Provides
    fun provideUserPreferencesDao(database: HocaLingoDatabase): UserPreferencesDao {
        return database.userPreferencesDao()
    }

    @Provides
    fun provideWordPackageDao(database: HocaLingoDatabase): WordPackageDao {
        return database.wordPackageDao()
    }

    @Provides
    fun provideCombinedDataDao(database: HocaLingoDatabase): CombinedDataDao {
        return database.combinedDataDao()
    }
}