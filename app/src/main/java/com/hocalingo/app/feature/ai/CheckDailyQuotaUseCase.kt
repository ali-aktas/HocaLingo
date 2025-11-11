package com.hocalingo.app.feature.ai

import com.hocalingo.app.core.base.Result
import javax.inject.Inject

class CheckDailyQuotaUseCase @Inject constructor(
    private val repository: StoryRepository
) {
    suspend operator fun invoke(): Result<Pair<Int, Int>> {
        return repository.getQuotaInfo()
    }
}