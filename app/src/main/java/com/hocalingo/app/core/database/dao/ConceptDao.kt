package com.hocalingo.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hocalingo.app.core.database.entities.ConceptEntity
import com.hocalingo.app.core.database.entities.SelectionStatus

@Dao
interface ConceptDao {

    @Query("SELECT * FROM concepts WHERE package_id = :packageId")
    suspend fun getConceptsByPackage(packageId: String): List<ConceptEntity>

    @Query("SELECT * FROM concepts WHERE id = :conceptId")
    suspend fun getConceptById(conceptId: Int): ConceptEntity?

    @Query("SELECT * FROM concepts WHERE level = :level")
    suspend fun getConceptsByLevel(level: String): List<ConceptEntity>

    @Query("SELECT * FROM concepts WHERE category = :category")
    suspend fun getConceptsByCategory(category: String): List<ConceptEntity>

    @Query("SELECT * FROM concepts WHERE user_added = 1")
    suspend fun getUserAddedConcepts(): List<ConceptEntity>

    @Query("SELECT DISTINCT category FROM concepts ORDER BY category")
    suspend fun getAllCategories(): List<String>

    @Query("SELECT DISTINCT level FROM concepts ORDER BY level")
    suspend fun getAllLevels(): List<String>

    @Query("""
        SELECT c.* FROM concepts c
        INNER JOIN user_selections us ON c.id = us.concept_id 
        WHERE us.status = :status
    """)
    suspend fun getConceptsBySelectionStatus(status: SelectionStatus): List<ConceptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConcepts(concepts: List<ConceptEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConcept(concept: ConceptEntity): Long

    @Update
    suspend fun updateConcept(concept: ConceptEntity)

    @Delete
    suspend fun deleteConcept(concept: ConceptEntity)

    @Query("DELETE FROM concepts WHERE package_id = :packageId")
    suspend fun deleteConceptsByPackage(packageId: String)

    @Query("SELECT COUNT(*) FROM concepts")
    suspend fun getConceptCount(): Int
}