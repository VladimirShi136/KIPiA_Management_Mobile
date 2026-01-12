package com.kipia.management.mobile.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Создаем DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context // ← Добавлено @ApplicationContext
) {
    companion object {
        // Ключи для хранения настроек
        private val THEME_MODE_KEY = intPreferencesKey("theme_mode")
        private val DYNAMIC_COLORS_KEY = booleanPreferencesKey("dynamic_colors")

        // Режимы темы
        const val THEME_FOLLOW_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2
    }

    // Получить текущий режим темы
    val themeMode: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_MODE_KEY] ?: THEME_FOLLOW_SYSTEM
        }

    // Получить настройку динамических цветов
    val dynamicColors: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DYNAMIC_COLORS_KEY] ?: false
        }

    // Установить режим темы
    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode
        }
    }

    // Установить динамические цвета
    suspend fun setDynamicColors(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLORS_KEY] = enabled
        }
    }

    // Получить текущий режим синхронно (для не-Compose кода)
    suspend fun getThemeModeSync(): Int {
        return context.dataStore.data
            .map { it[THEME_MODE_KEY] ?: THEME_FOLLOW_SYSTEM }
            .first()
    }
}