package com.hocalingo.app.feature.ai

import com.hocalingo.app.core.base.Result
import com.hocalingo.app.feature.ai.models.GeneratedStory
import com.hocalingo.app.feature.ai.models.StoryDifficulty
import com.hocalingo.app.feature.ai.models.StoryLength
import com.hocalingo.app.feature.ai.models.StoryType
import javax.inject.Inject

class GenerateStoryUseCase @Inject constructor(
    private val repository: StoryRepository
) {
    suspend operator fun invoke(
        topic: String?,
        type: StoryType,
        difficulty: StoryDifficulty,
        length: StoryLength
    ): Result<GeneratedStory> {
        return repository.generateStory(topic, type, difficulty, length)
    }
}