package com.hocalingo.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.hocalingo.app.core.database.entities.SelectionStatus
import com.hocalingo.app.core.database.entities.UserSelectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSelectionDao {

    @Query("SELECT * FROM user_selections WHERE concept_id = :conceptId")
    suspend fun getSelectionByConceptId(conceptId: Int): UserSelectionEntity?

    @Query("SELECT * FROM user_selections WHERE status = :status")
    suspend fun getSelectionsByStatus(status: SelectionStatus): List<UserSelectionEntity>

    @Query("SELECT COUNT(*) FROM user_selections WHERE status = :status")
    suspend fun getSelectionCountByStatus(status: SelectionStatus): Int

    @Query("SELECT COUNT(*) FROM user_selections WHERE status = 'SELECTED'")
    fun getSelectedWordsCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSelection(selection: UserSelectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSelections(selections: List<UserSelectionEntity>)

    @Update
    suspend fun updateSelection(selection: UserSelectionEntity)

    @Delete
    suspend fun deleteSelection(selection: UserSelectionEntity)

    @Query("DELETE FROM user_selections WHERE concept_id = :conceptId")
    suspend fun deleteSelectionByConceptId(conceptId: Int)

    @Transaction
    suspend fun selectWord(conceptId: Int, packageLevel: String) {
        insertSelection(
            UserSelectionEntity(
                conceptId = conceptId,
                status = SelectionStatus.SELECTED,
                selectedAt = System.currentTimeMillis(),
                packageLevel = packageLevel
            )
        )
    }

    @Transaction
    suspend fun hideWord(conceptId: Int, packageLevel: String) {
        insertSelection(
            UserSelectionEntity(
                conceptId = conceptId,
                status = SelectionStatus.HIDDEN,
                selectedAt = System.currentTimeMillis(),
                packageLevel = packageLevel
            )
        )
    }
}