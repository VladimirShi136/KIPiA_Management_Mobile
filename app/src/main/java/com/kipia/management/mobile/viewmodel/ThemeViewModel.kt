package com.kipia.management.mobile.viewmodel

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    // Состояние темы
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

    // Проверка поддержки Dynamic Colors (Android 12+)
    val supportsDynamicColors: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // Добавьте этот метод для ThemeToggleButton
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
        if (!supportsDynamicColors) return

        viewModelScope.launch {
            val current = dynamicColors.value
            preferencesRepository.setDynamicColors(!current)
        }
    }

    // Получить тему для не-Compose кода (например, для Activity)
    suspend fun getCurrentTheme(): Int {
        return themeMode.value
    }
}