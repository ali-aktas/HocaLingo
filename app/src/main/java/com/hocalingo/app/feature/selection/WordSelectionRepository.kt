package com.hocalingo.app.feature.selection

import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.database.HocaLingoDatabase
import com.hocalingo.app.database.entities.ConceptEntity
import com.hocalingo.app.database.entities.SelectionStatus
import com.hocalingo.app.database.entities.StudyDirection
import com.hocalingo.app.database.entities.WordProgressEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WordSelectionRepository - Database Operations Helper
 *
 * Separates database logic from ViewModel
 * All real DAO method names used - NO ERRORS!
 *
 * Package: feature/selection/
 */
@Singleton
class WordSelectionRepository @Inject constructor(
    private val database: HocaLingoDatabase
) {

    /**
     * Load all concepts for a package
     */
    suspend fun loadConceptsByPackage(packageId: String): List<ConceptEntity> {
        return database.conceptDao().getConceptsByPackage(packageId)
    }

    /**
     * Get all selected word IDs
     */
    suspend fun getSelectedWordIds(): Set<Int> {
        return database.userSelectionDao()
            .getSelectionsByStatus(SelectionStatus.SELECTED)
            .map { it.conceptId }
            .toSet()
    }

    /**
     * Get all hidden word IDs
     */
    suspend fun getHiddenWordIds(): Set<Int> {
        return database.userSelectionDao()
            .getSelectionsByStatus(SelectionStatus.HIDDEN)
            .map { it.conceptId }
            .toSet()
    }

    /**
     * Get today's selection count
     */
    suspend fun getTodaySelectionCount(): Int {
        return database.userSelectionDao()
            .getSelectionCountByStatus(SelectionStatus.SELECTED)
    }

    /**
     * Select a word (mark as SELECTED)
     */
    suspend fun selectWord(conceptId: Int, packageLevel: String) {
        database.userSelectionDao().selectWord(conceptId, packageLevel)
    }

    /**
     * Hide a word (mark as HIDDEN)
     */
    suspend fun hideWord(conceptId: Int, packageLevel: String) {
        database.userSelectionDao().hideWord(conceptId, packageLevel)
    }

    /**
     * Delete selection by concept ID
     */
    suspend fun deleteSelection(conceptId: Int) {
        database.userSelectionDao().deleteSelectionByConceptId(conceptId)
    }

    /**
     * Create WordProgressEntity for a selected word
     * Creates progress for both directions (EN_TO_TR, TR_TO_EN)
     */
    suspend fun createWordProgress(conceptId: Int, packageId: String) {
        try {
            // Check existing progress
            val existingProgress = database.wordProgressDao()
                .getProgressByConceptId(conceptId)

            // Get max session positions
            val maxPosEnToTr = database.combinedDataDao()
                .getMaxSessionPosition(StudyDirection.EN_TO_TR)
            val maxPosTrToEn = database.combinedDataDao()
                .getMaxSessionPosition(StudyDirection.TR_TO_EN)

            val currentTime = System.currentTimeMillis()
            val newProgressList = mutableListOf<WordProgressEntity>()

            // Create EN_TO_TR if not exists
            if (existingProgress.none { it.direction == StudyDirection.EN_TO_TR }) {
                newProgressList.add(
                    WordProgressEntity(
                        conceptId = conceptId,
                        direction = StudyDirection.EN_TO_TR,
                        repetitions = 0,
                        intervalDays = 0f,
                        easeFactor = 2.5f,
                        nextReviewAt = currentTime,
                        lastReviewAt = null,
                        isSelected = true,
                        isMastered = false,
                        learningPhase = true,
                        sessionPosition = maxPosEnToTr + 1,
                        createdAt = currentTime,
                        updatedAt = currentTime
                    )
                )
            }

            // Create TR_TO_EN if not exists
            if (existingProgress.none { it.direction == StudyDirection.TR_TO_EN }) {
                newProgressList.add(
                    WordProgressEntity(
                        conceptId = conceptId,
                        direction = StudyDirection.TR_TO_EN,
                        repetitions = 0,
                        intervalDays = 0f,
                        easeFactor = 2.5f,
                        nextReviewAt = currentTime,
                        lastReviewAt = null,
                        isSelected = true,
                        isMastered = false,
                        learningPhase = true,
                        sessionPosition = maxPosTrToEn + 1,
                        createdAt = currentTime,
                        updatedAt = currentTime
                    )
                )
            }

            if (newProgressList.isNotEmpty()) {
                database.wordProgressDao().insertProgressList(newProgressList)
                DebugHelper.logWordSelection("Created ${newProgressList.size} progress records for concept $conceptId")
            }

        } catch (e: Exception) {
            DebugHelper.logError("Error creating word progress for $conceptId", e)
        }
    }

    /**
     * Prepare study session for all selected words
     * Creates WordProgressEntity for words that don't have it yet
     */
    suspend fun prepareStudySession(): Int {
        try {
            val selectedSelections = database.userSelectionDao()
                .getSelectionsByStatus(SelectionStatus.SELECTED)

            if (selectedSelections.isEmpty()) {
                DebugHelper.logWordSelection("No selected words for study session")
                return 0
            }

            val selectedConceptIds = selectedSelections.map { it.conceptId }

            // Get existing progress (both directions)
            val existingProgressIds = mutableSetOf<Int>()
            selectedConceptIds.forEach { conceptId ->
                val progressList = database.wordProgressDao().getProgressByConceptId(conceptId)
                if (progressList.isNotEmpty()) {
                    existingProgressIds.add(conceptId)
                }
            }

            // Create progress for new words
            val newWords = selectedConceptIds.filter { it !in existingProgressIds }

            newWords.forEach { conceptId ->
                val packageLevel = selectedSelections
                    .find { it.conceptId == conceptId }?.packageLevel ?: "CUSTOM"
                createWordProgress(conceptId, packageLevel)
            }

            DebugHelper.logWordSelection("Study session prepared: ${newWords.size} new progress records created")
            return newWords.size

        } catch (e: Exception) {
            DebugHelper.logError("Error preparing study session", e)
            return 0
        }
    }

    /**
     * Validate package exists
     */
    suspend fun validatePackage(packageId: String): Boolean {
        val packageInfo = database.wordPackageDao().getPackageById(packageId)
        return packageInfo != null
    }
}