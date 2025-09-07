package com.hocalingo.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hocalingo.app.core.database.entities.WordPackageEntity

@Dao
interface WordPackageDao {

    @Query("SELECT * FROM word_packages WHERE is_active = 1 ORDER BY downloaded_at DESC")
    suspend fun getActivePackages(): List<WordPackageEntity>

    @Query("SELECT * FROM word_packages WHERE package_id = :packageId")
    suspend fun getPackageById(packageId: String): WordPackageEntity?

    @Query("SELECT * FROM word_packages WHERE level = :level AND is_active = 1")
    suspend fun getPackagesByLevel(level: String): List<WordPackageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackage(wordPackage: WordPackageEntity)

    @Update
    suspend fun updatePackage(wordPackage: WordPackageEntity)

    @Query("UPDATE word_packages SET is_active = 0 WHERE package_id = :packageId")
    suspend fun deactivatePackage(packageId: String)

    @Delete
    suspend fun deletePackage(wordPackage: WordPackageEntity)
}