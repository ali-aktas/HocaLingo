package com.hocalingo.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.core.common.ThemeMode
import com.hocalingo.app.core.common.UserPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Theme ViewModel - Centralized Theme Management
 * ✅ Manages app-wide theme state
 * ✅ Persists user theme preferences
 * ✅ Handles system theme changes
 * ✅ Real-time theme switching without restart
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    init {
        loadThemePreference()
    }

    /**
     * Load saved theme preference on app start
     */
    private fun loadThemePreference() {
        viewModelScope.launch {
            userPreferencesManager.getThemeMode().collectLatest { savedThemeMode ->
                _themeMode.value = savedThemeMode
            }
        }
    }

    /**
     * Update theme mode (called from Profile screen or settings)
     */
    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            _themeMode.value = themeMode
            userPreferencesManager.setThemeMode(themeMode)
        }
    }

    /**
     * Get effective dark theme boolean based on theme mode
     * This is used by HocaLingoTheme composable
     */
    @Composable
    fun shouldUseDarkTheme(): Boolean {
        val currentThemeMode by themeMode.collectAsState()
        return when (currentThemeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
    }

    /**
     * Convenience method to toggle between light and dark
     * Useful for quick theme switching
     */
    fun toggleTheme() {
        val currentMode = _themeMode.value
        val newMode = when (currentMode) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.SYSTEM -> ThemeMode.LIGHT // Always go to light when toggling from system
        }
        updateThemeMode(newMode)
    }

    /**
     * Check if we're currently using dark theme (for UI logic)
     */
    @Composable
    fun isDarkTheme(): Boolean = shouldUseDarkTheme()
}