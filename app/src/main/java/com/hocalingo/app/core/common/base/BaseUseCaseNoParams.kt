package com.hocalingo.app.core.common.base

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class BaseUseCaseNoParams<out Type>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend operator fun invoke(): Result<Type> = try {
        withContext(dispatcher) {
            Result.Success(execute())
        }
    } catch (e: Exception) {
        Result.Error(e.toAppError())
    }

    protected abstract suspend fun execute(): Type
}