package com.hocalingo.app.core.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.hocalingo.app.core.database.dao.CombinedDataDao
import com.hocalingo.app.core.database.dao.ConceptDao
import com.hocalingo.app.core.database.dao.DailyStatsDao
import com.hocalingo.app.core.database.dao.StudySessionDao
import com.hocalingo.app.core.database.dao.UserPreferencesDao
import com.hocalingo.app.core.database.dao.UserSelectionDao
import com.hocalingo.app.core.database.dao.WordPackageDao
import com.hocalingo.app.core.database.dao.WordProgressDao
import com.hocalingo.app.core.database.entities.ConceptEntity
import com.hocalingo.app.core.database.entities.DailyStatsEntity
import com.hocalingo.app.core.database.entities.DatabaseTypeConverters
import com.hocalingo.app.core.database.entities.StudySessionEntity
import com.hocalingo.app.core.database.entities.UserPreferencesEntity
import com.hocalingo.app.core.database.entities.UserSelectionEntity
import com.hocalingo.app.core.database.entities.WordPackageEntity
import com.hocalingo.app.core.database.entities.WordProgressEntity

/**
 * HocaLingo Room Database
 *
 * Central database for all app data including:
 * - Word concepts and vocabulary
 * - User progress and spaced repetition data
 * - Study sessions and statistics
 * - User preferences and settings
 */
@Database(
    entities = [
        ConceptEntity::class,
        WordProgressEntity::class,
        UserSelectionEntity::class,
        StudySessionEntity::class,
        DailyStatsEntity::class,
        UserPreferencesEntity::class,
        WordPackageEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(DatabaseTypeConverters::class)
abstract class HocaLingoDatabase : RoomDatabase() {

    // DAO Abstract Methods
    abstract fun conceptDao(): ConceptDao
    abstract fun wordProgressDao(): WordProgressDao
    abstract fun userSelectionDao(): UserSelectionDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun dailyStatsDao(): DailyStatsDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun wordPackageDao(): WordPackageDao
    abstract fun combinedDataDao(): CombinedDataDao

    companion object {
        const val DATABASE_NAME = "hocalingo_database"

        @Volatile
        private var INSTANCE: HocaLingoDatabase? = null

        /**
         * Get database instance with singleton pattern
         */
        fun getDatabase(context: Context): HocaLingoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HocaLingoDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations() // Add migrations when needed
                    .addCallback(DatabaseCallback()) // Initial setup
                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * Database callback for initial setup
         */
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Database created for the first time
                // Any initial setup can be done here
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Database opened
                // Can be used for maintenance tasks
            }
        }

        // Future migrations will be added here
        // Example migration from version 1 to 2:
        /*
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE concepts ADD COLUMN new_field TEXT")
            }
        }
        */
    }
}

/**
 * Database utility functions
 */
object DatabaseUtils {

    /**
     * Get current timestamp in milliseconds
     */
    fun getCurrentTimestamp(): Long = System.currentTimeMillis()

    /**
     * Get date string in YYYY-MM-DD format for daily stats
     */
    fun getTodayDateString(): String {
        val calendar = java.util.Calendar.getInstance()
        return String.format(
            "%04d-%02d-%02d",
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Calculate next review time based on SM-2 algorithm
     */
    fun calculateNextReviewTime(
        currentTime: Long,
        intervalDays: Float
    ): Long {
        val intervalMs = (intervalDays * 24 * 60 * 60 * 1000).toLong()
        return currentTime + intervalMs
    }

    /**
     * Get start of day timestamp
     */
    fun getStartOfDayTimestamp(timestamp: Long = getCurrentTimestamp()): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Get end of day timestamp
     */
    fun getEndOfDayTimestamp(timestamp: Long = getCurrentTimestamp()): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
}

/**
 * Database seeding functions for development
 */
object DatabaseSeeder {

    /**
     * Insert test data from JSON file (for development)
     */
    suspend fun seedTestData(database: HocaLingoDatabase) {
        // This will be implemented when we load JSON data
        // For now, we can insert a few test concepts manually

        val testConcepts = listOf(
            ConceptEntity(
                id = 1001,
                english = "book",
                turkish = "kitap",
                exampleEn = "I'm reading a good book",
                exampleTr = "İyi bir kitap okuyorum",
                pronunciation = "bʊk",
                level = "A1",
                category = "education",
                packageId = "test_a1_en_tr_v1"
            ),
            ConceptEntity(
                id = 1002,
                english = "water",
                turkish = "su",
                exampleEn = "I drink water every day",
                exampleTr = "Her gün su içerim",
                pronunciation = "ˈwɔːtər",
                level = "A1",
                category = "basic",
                packageId = "test_a1_en_tr_v1"
            ),
            ConceptEntity(
                id = 1003,
                english = "house",
                turkish = "ev",
                exampleEn = "My house is big",
                exampleTr = "Benim evim büyük",
                pronunciation = "haʊs",
                level = "A1",
                category = "home",
                packageId = "test_a1_en_tr_v1"
            )
        )

        // Insert test concepts
        database.conceptDao().insertConcepts(testConcepts)

        // Insert test package info
        database.wordPackageDao().insertPackage(
            WordPackageEntity(
                packageId = "test_a1_en_tr_v1",
                version = "1.0.0",
                level = "A1",
                languagePair = "en_tr",
                totalWords = testConcepts.size,
                downloadedAt = DatabaseUtils.getCurrentTimestamp(),
                description = "Test A1 package for development"
            )
        )
    }

    /**
     * Create default user preferences
     */
    suspend fun createDefaultUserPreferences(
        database: HocaLingoDatabase,
        userId: String
    ) {
        val defaultPreferences = UserPreferencesEntity(
            userId = userId,
            nativeLanguage = "tr",
            targetLanguage = "en",
            currentLevel = "A1",
            dailyGoal = 20,
            studyReminderEnabled = true,
            studyReminderHour = 20,
            isPremium = false,
            onboardingCompleted = false
        )

        database.userPreferencesDao().insertOrUpdatePreferences(defaultPreferences)
    }
}