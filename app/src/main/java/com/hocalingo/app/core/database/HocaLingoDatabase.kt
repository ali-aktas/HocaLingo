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
        WordPackageEntity::class
    ],
    version = 2, // 🔥 UPDATED: Version 1 → 2 for hybrid learning system
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
         * 🔥 MIGRATION 1 → 2: Add Hybrid Learning System Fields
         *
         * Adds learning_phase and session_position columns to word_progress table
         * for session-based learning + time-based review system
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Add learning_phase column (default: true = learning phase)
                    database.execSQL("""
                        ALTER TABLE word_progress 
                        ADD COLUMN learning_phase INTEGER NOT NULL DEFAULT 1
                    """)

                    // Add session_position column (nullable for review cards)
                    database.execSQL("""
                        ALTER TABLE word_progress 
                        ADD COLUMN session_position INTEGER
                    """)

                    // Create indexes for new columns to improve query performance
                    database.execSQL("""
                        CREATE INDEX IF NOT EXISTS index_word_progress_learning_phase 
                        ON word_progress(learning_phase)
                    """)

                    database.execSQL("""
                        CREATE INDEX IF NOT EXISTS index_word_progress_session_position 
                        ON word_progress(session_position)
                    """)

                    // Set initial session positions for existing cards
                    // All existing cards start in learning phase with incremental positions
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

                    android.util.Log.d("HocaLingoDatabase", "✅ Migration 1→2 completed successfully")

                } catch (e: Exception) {
                    android.util.Log.e("HocaLingoDatabase", "❌ Migration 1→2 failed", e)
                    throw e
                }
            }
        }

        /**
         * Get database instance with singleton pattern - UPDATED WITH MIGRATION
         */
        fun getDatabase(context: Context): HocaLingoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HocaLingoDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2) // 🔥 ADD MIGRATION HERE
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