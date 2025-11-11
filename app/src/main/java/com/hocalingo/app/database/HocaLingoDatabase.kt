package com.hocalingo.app.database

import com.hocalingo.app.database.dao.StoryDao
import com.hocalingo.app.database.entities.StoryEntity
import com.hocalingo.app.database.entities.StoryQuotaEntity
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import android.util.Log
import com.hocalingo.app.database.dao.CombinedDataDao
import com.hocalingo.app.database.dao.ConceptDao
import com.hocalingo.app.database.dao.DailyStatsDao
import com.hocalingo.app.database.dao.StudySessionDao
import com.hocalingo.app.database.dao.UserPreferencesDao
import com.hocalingo.app.database.dao.UserSelectionDao
import com.hocalingo.app.database.dao.WordPackageDao
import com.hocalingo.app.database.dao.WordProgressDao
import com.hocalingo.app.database.entities.ConceptEntity
import com.hocalingo.app.database.entities.DailyStatsEntity
import com.hocalingo.app.database.entities.DatabaseTypeConverters
import com.hocalingo.app.database.entities.StudySessionEntity
import com.hocalingo.app.database.entities.UserPreferencesEntity
import com.hocalingo.app.database.entities.UserSelectionEntity
import com.hocalingo.app.database.entities.WordPackageEntity
import com.hocalingo.app.database.entities.WordProgressEntity

/**
 * HocaLingo Room Database - UPDATED TO VERSION 2
 *
 * VERSION 2 CHANGES:
 * ✅ Added learning_phase column to word_progress table
 * ✅ Added session_position column to word_progress table
 * ✅ Added indexes for new columns
 * ✅ Migration from version 1 to 2 included
 *
 * Central database for all app data including:
 * - Word concepts and vocabulary
 * - User progress and spaced repetition data (HYBRID LEARNING + REVIEW)
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
        WordPackageEntity::class,
        StoryEntity::class,
        StoryQuotaEntity::class
    ],
    version = 4,
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
    abstract fun storyDao(): StoryDao

    companion object {
        const val DATABASE_NAME = "hocalingo_database"

        @Volatile
        private var INSTANCE: HocaLingoDatabase? = null

        /**
         * 🔥 MIGRATION 1 → 2: Add Hybrid Learning System Fields
         *
         * ✅ Public yapıldı - artık DatabaseModule'den erişilebilir
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Add learning_phase column
                    database.execSQL("""
                    ALTER TABLE word_progress 
                    ADD COLUMN learning_phase INTEGER NOT NULL DEFAULT 1
                """)

                    // Add session_position column
                    database.execSQL("""
                    ALTER TABLE word_progress 
                    ADD COLUMN session_position INTEGER
                """)

                    // Create indexes
                    database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_word_progress_learning_phase 
                    ON word_progress(learning_phase)
                """)

                    database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_word_progress_session_position 
                    ON word_progress(session_position)
                """)

                    // Initialize session positions
                    database.execSQL("""
                    UPDATE word_progress 
                    SET session_position = (
                        SELECT COUNT(*) + 1 
                        FROM word_progress p2 
                        WHERE p2.direction = word_progress.direction 
                        AND p2.rowid < word_progress.rowid
                    )
                    WHERE learning_phase = 1
                """)

                    Log.d("HocaLingoDatabase", "✅ Migration 1→2 completed")
                } catch (e: Exception) {
                    Log.e("HocaLingoDatabase", "❌ Migration 1→2 failed", e)
                    throw e
                }
            }
        }

        /**
         * 🔥 MIGRATION 2 → 3: Add Enhanced Spaced Repetition Tracking
         *
         * ✅ Public yapıldı - artık DatabaseModule'den erişilebilir
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Add hard_presses column
                    database.execSQL("""
                    ALTER TABLE word_progress 
                    ADD COLUMN hard_presses INTEGER DEFAULT 0
                """)

                    // Add successful_reviews column
                    database.execSQL("""
                    ALTER TABLE word_progress 
                    ADD COLUMN successful_reviews INTEGER DEFAULT 0
                """)

                    // Initialize values
                    database.execSQL("""
                    UPDATE word_progress 
                    SET hard_presses = 0, 
                        successful_reviews = CASE 
                            WHEN learning_phase = 0 THEN 3 
                            ELSE repetitions 
                        END
                """)

                    Log.d("HocaLingoDatabase", "✅ Migration 2→3 completed")
                } catch (e: Exception) {
                    Log.e("HocaLingoDatabase", "❌ Migration 2→3 failed", e)
                    throw e
                }
            }
        }


        // ← BURAYI YENİ EKLE:
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Stories table
                    database.execSQL("""
                CREATE TABLE IF NOT EXISTS `generated_stories` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `title` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `used_words` TEXT NOT NULL,
                    `topic` TEXT,
                    `type` TEXT NOT NULL,
                    `difficulty` TEXT NOT NULL,
                    `length` TEXT NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    `is_favorite` INTEGER NOT NULL DEFAULT 0,
                    `is_premium` INTEGER NOT NULL DEFAULT 1
                )
            """)

                    // Indexes
                    database.execSQL("""
                CREATE INDEX IF NOT EXISTS `index_generated_stories_created_at` 
                ON `generated_stories` (`created_at`)
            """)

                    database.execSQL("""
                CREATE INDEX IF NOT EXISTS `index_generated_stories_type` 
                ON `generated_stories` (`type`)
            """)

                    database.execSQL("""
                CREATE INDEX IF NOT EXISTS `index_generated_stories_is_favorite` 
                ON `generated_stories` (`is_favorite`)
            """)

                    // Quota table
                    database.execSQL("""
                CREATE TABLE IF NOT EXISTS `story_quota` (
                    `date` TEXT NOT NULL PRIMARY KEY,
                    `count` INTEGER NOT NULL DEFAULT 0,
                    `reset_time` INTEGER NOT NULL
                )
            """)

                    Log.d("HocaLingoDatabase", "✅ Migration 3→4 completed")
                } catch (e: Exception) {
                    Log.e("HocaLingoDatabase", "❌ Migration 3→4 failed", e)
                    throw e
                }
            }
        }

        /**
         * Get database instance with singleton pattern
         *
         * ⚠️ BU METOD ŞİMDİLİK KULLANILMIYOR!
         * DatabaseModule Hilt ile database oluşturuyor.
         * Ama ileride manuel initialization için kullanılabilir.
         */
        fun getDatabase(context: Context): HocaLingoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HocaLingoDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4) // ← MIGRATION_3_4 ekle
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Clear database instance (for testing)
         */
        fun clearInstance() {
            INSTANCE = null
        }
    }
}