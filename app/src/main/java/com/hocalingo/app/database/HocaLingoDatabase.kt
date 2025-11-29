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
 * HocaLingo Room Database - UPDATED TO VERSION 5
 *
 * VERSION 5 CHANGES:
 * ✅ Changed successful_reviews from INTEGER to REAL for partial success tracking
 * ✅ MEDIUM button now gives 0.5 points instead of 1 point
 * ✅ Migration from version 4 to 5 included
 *
 * Previous versions:
 * VERSION 4: Added AI Story feature tables
 * VERSION 3: Added hard_presses and successful_reviews tracking
 * VERSION 2: Added learning_phase and session_position for hybrid system
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
    version = 5,  // ✅ 4 → 5
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

        /**
         * 🔥 MIGRATION 3 → 4: Add AI Story Feature Tables
         */
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
         * 🔥 MIGRATION 4 → 5: Change successful_reviews to REAL for partial success
         *
         * This allows MEDIUM button to give 0.5 points instead of 1 point
         * Required for improved graduation criteria:
         * - EASY = 1.0 point
         * - MEDIUM = 0.5 points
         * - Graduation at 3.0 points
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    Log.d("HocaLingoDatabase", "🔄 Starting Migration 4→5...")

                    // SQLite doesn't support ALTER COLUMN TYPE directly
                    // We need to: create new table → copy data → drop old → rename new

                    // 1. Create new table with REAL type for successful_reviews
                    database.execSQL("""
                        CREATE TABLE word_progress_new (
                            concept_id INTEGER NOT NULL,
                            direction TEXT NOT NULL,
                            repetitions INTEGER NOT NULL DEFAULT 0,
                            interval_days REAL NOT NULL DEFAULT 1.0,
                            ease_factor REAL NOT NULL DEFAULT 2.5,
                            next_review_at INTEGER NOT NULL,
                            last_review_at INTEGER,
                            is_selected INTEGER NOT NULL DEFAULT 0,
                            is_mastered INTEGER NOT NULL DEFAULT 0,
                            hard_presses INTEGER DEFAULT 0,
                            successful_reviews REAL DEFAULT 0.0,
                            learning_phase INTEGER NOT NULL DEFAULT 1,
                            session_position INTEGER,
                            failures INTEGER DEFAULT 0,
                            success_streak INTEGER DEFAULT 0,
                            created_at INTEGER NOT NULL,
                            updated_at INTEGER NOT NULL,
                            PRIMARY KEY(concept_id, direction),
                            FOREIGN KEY(concept_id) REFERENCES concepts(id) ON DELETE CASCADE
                        )
                    """)

                    // 2. Copy data (INTEGER automatically converts to REAL)
                    database.execSQL("""
                        INSERT INTO word_progress_new 
                        SELECT 
                            concept_id,
                            direction,
                            repetitions,
                            interval_days,
                            ease_factor,
                            next_review_at,
                            last_review_at,
                            is_selected,
                            is_mastered,
                            hard_presses,
                            CAST(successful_reviews AS REAL),
                            learning_phase,
                            session_position,
                            failures,
                            success_streak,
                            created_at,
                            updated_at
                        FROM word_progress
                    """)

                    // 3. Drop old table
                    database.execSQL("DROP TABLE word_progress")

                    // 4. Rename new table
                    database.execSQL("ALTER TABLE word_progress_new RENAME TO word_progress")

                    // 5. Recreate indexes
                    database.execSQL("""
                        CREATE INDEX IF NOT EXISTS index_word_progress_concept_id 
                        ON word_progress(concept_id)
                    """)

                    database.execSQL("""
                        CREATE INDEX IF NOT EXISTS index_word_progress_next_review_at 
                        ON word_progress(next_review_at)
                    """)

                    database.execSQL("""
                        CREATE INDEX IF NOT EXISTS index_word_progress_is_selected 
                        ON word_progress(is_selected)
                    """)

                    database.execSQL("""
                        CREATE INDEX IF NOT EXISTS index_word_progress_learning_phase 
                        ON word_progress(learning_phase)
                    """)

                    database.execSQL("""
                        CREATE INDEX IF NOT EXISTS index_word_progress_session_position 
                        ON word_progress(session_position)
                    """)

                    Log.d("HocaLingoDatabase", "✅ Migration 4→5 completed successfully")
                } catch (e: Exception) {
                    Log.e("HocaLingoDatabase", "❌ Migration 4→5 failed", e)
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)  // ✅ MIGRATION_4_5 eklendi
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