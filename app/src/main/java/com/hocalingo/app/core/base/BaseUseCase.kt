package com.hocalingo.app.core.base

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class BaseUseCase<in Params, out Type>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend operator fun invoke(params: Params): Result<Type> = try {
        withContext(dispatcher) {
            Result.Success(execute(params))
        }
    } catch (e: Exception) {
        Result.Error(e.toAppError())
    }

    protected abstract suspend fun execute(params: Params): Type
}