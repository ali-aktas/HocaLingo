package com.hocalingo.app.feature.ai

import com.hocalingo.app.feature.ai.models.GeneratedStory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStoryHistoryUseCase @Inject constructor(
    private val repository: StoryRepository
) {
    operator fun invoke(): Flow<List<GeneratedStory>> {
        return repository.getAllStories()
    }
}