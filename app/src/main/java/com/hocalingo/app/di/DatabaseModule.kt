package com.hocalingo.app.di

import android.content.Context
import androidx.room.Room
import com.hocalingo.app.database.HocaLingoDatabase
import com.hocalingo.app.database.LocalPackageLoader
import com.hocalingo.app.database.dao.CombinedDataDao
import com.hocalingo.app.database.dao.ConceptDao
import com.hocalingo.app.database.dao.DailyStatsDao
import com.hocalingo.app.database.dao.StoryDao
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
 * DatabaseModule - Room Database ve DAO'lar için Hilt modülü
 *
 * ✅ LocalPackageLoader eklendi (assets yükleme için)
 * ✅ Tüm DAO'lar provide ediliyor
 * ✅ Migration hatası düzeltildi
 * ✅ Singleton pattern
 *
 * Package: di/
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideHocaLingoDatabase(
        @ApplicationContext context: Context
    ): HocaLingoDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            HocaLingoDatabase::class.java,
            HocaLingoDatabase.DATABASE_NAME
        )
            // ✅ Migration'lar HocaLingoDatabase.getDatabase() içinde tanımlı
            // Direkt getDatabase() kullan
            .build()
    }

    // ═══════════════════════════════════════════════════════════
    // DAO PROVIDERS
    // ═══════════════════════════════════════════════════════════

    @Provides
    @Singleton
    fun provideConceptDao(database: HocaLingoDatabase): ConceptDao {
        return database.conceptDao()
    }

    @Provides
    @Singleton
    fun provideWordProgressDao(database: HocaLingoDatabase): WordProgressDao {
        return database.wordProgressDao()
    }

    @Provides
    @Singleton
    fun provideUserSelectionDao(database: HocaLingoDatabase): UserSelectionDao {
        return database.userSelectionDao()
    }

    @Provides
    @Singleton
    fun provideStudySessionDao(database: HocaLingoDatabase): StudySessionDao {
        return database.studySessionDao()
    }

    @Provides
    @Singleton
    fun provideDailyStatsDao(database: HocaLingoDatabase): DailyStatsDao {
        return database.dailyStatsDao()
    }

    @Provides
    @Singleton
    fun provideUserPreferencesDao(database: HocaLingoDatabase): UserPreferencesDao {
        return database.userPreferencesDao()
    }

    @Provides
    @Singleton
    fun provideWordPackageDao(database: HocaLingoDatabase): WordPackageDao {
        return database.wordPackageDao()
    }

    @Provides
    @Singleton
    fun provideCombinedDataDao(database: HocaLingoDatabase): CombinedDataDao {
        return database.combinedDataDao()
    }

    // ═══════════════════════════════════════════════════════════
    // ✨ YENİ: LOCAL PACKAGE LOADER
    // ═══════════════════════════════════════════════════════════

    /**
     * LocalPackageLoader - Assets'ten 1600 kelimeyi yükler
     *
     * @Singleton olarak provide ediliyor
     * SplashViewModel'de inject edilerek kullanılıyor
     */
    @Provides
    @Singleton
    fun provideLocalPackageLoader(
        @ApplicationContext context: Context,
        database: HocaLingoDatabase
    ): LocalPackageLoader {
        return LocalPackageLoader(context, database)
    }

    @Provides
    @Singleton
    fun provideStoryDao(database: HocaLingoDatabase): StoryDao {
        return database.storyDao()
    }

}