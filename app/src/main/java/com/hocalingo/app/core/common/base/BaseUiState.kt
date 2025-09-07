package com.hocalingo.app.core.common.base

interface BaseUiState {
    val isLoading: Boolean
    val error: String?
}

data class LoadingState<T>(
    val data: T? = null,
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val isEmpty: Boolean = false
) : BaseUiState

fun <T> LoadingState<T>.loading(): LoadingState<T> = copy(isLoading = true, error = null)
fun <T> LoadingState<T>.success(data: T): LoadingState<T> = copy(
    data = data,
    isLoading = false,
    error = null,
    isEmpty = false
)
fun <T> LoadingState<T>.error(message: String): LoadingState<T> = copy(
    isLoading = false,
    error = message
)
fun <T> LoadingState<T>.empty(): LoadingState<T> = copy(
    isLoading = false,
    error = null,
    isEmpty = true
)