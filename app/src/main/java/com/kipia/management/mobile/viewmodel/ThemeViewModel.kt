package com.kipia.management.mobile.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    // Состояние темы как StateFlow
    val themeMode = preferencesRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PreferencesRepository.THEME_FOLLOW_SYSTEM
        )

    // Состояние динамических цветов
    val dynamicColors = preferencesRepository.dynamicColors
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Для вычисления текущей темы (true = dark, false = light)
    var isDarkTheme by mutableStateOf(false)
        private set

    init {
        // Инициализируем тему при создании ViewModel
        viewModelScope.launch {
            preferencesRepository.themeMode.collect { mode ->
                updateDarkTheme(mode)
            }
        }
    }

    // Обновить тему на основе режима
    private fun updateDarkTheme(mode: Int) {
        // TODO: Здесь нужно учитывать системную тему, но пока просто по режиму
        isDarkTheme = when (mode) {
            PreferencesRepository.THEME_LIGHT -> false
            PreferencesRepository.THEME_DARK -> true
            PreferencesRepository.THEME_FOLLOW_SYSTEM -> {
                // Для тестирования можно временно вернуть false
                // В реальном приложении здесь будет isSystemInDarkTheme()
                false
            }
            else -> false
        }
    }

    // Переключить тему (циклично: Системная → Светлая → Темная → Системная)
    fun toggleTheme() {
        viewModelScope.launch {
            val currentMode = themeMode.value
            val newMode = when (currentMode) {
                PreferencesRepository.THEME_FOLLOW_SYSTEM -> PreferencesRepository.THEME_LIGHT
                PreferencesRepository.THEME_LIGHT -> PreferencesRepository.THEME_DARK
                PreferencesRepository.THEME_DARK -> PreferencesRepository.THEME_FOLLOW_SYSTEM
                else -> PreferencesRepository.THEME_FOLLOW_SYSTEM
            }
            preferencesRepository.setThemeMode(newMode)
        }
    }

    // Установить конкретную тему
    fun setTheme(mode: Int) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    // Переключить динамические цвета
    fun toggleDynamicColors() {
        viewModelScope.launch {
            val current = dynamicColors.value
            preferencesRepository.setDynamicColors(!current)
        }
    }
}