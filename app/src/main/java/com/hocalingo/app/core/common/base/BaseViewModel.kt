package com.hocalingo.app.core.common.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel<UiState, Event, Effect>(
    initialState: UiState
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<Effect>()
    val effect: SharedFlow<Effect> = _effect.asSharedFlow()

    protected val currentState: UiState
        get() = _state.value

    protected fun updateState(update: (UiState) -> UiState) {
        _state.value = update(currentState)
    }

    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }

    abstract fun onEvent(event: Event)

    protected fun launch(
        onError: (Exception) -> Unit = { handleError(it) },
        block: suspend CoroutineScope.() -> Unit
    ) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    protected open fun handleError(exception: Exception) {
        // Override in child ViewModels
    }
}