package com.kipia.management.mobile.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey  // ← добавь импорт
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private val THEME_MODE_KEY = intPreferencesKey("theme_mode")
        private val DYNAMIC_COLORS_KEY = booleanPreferencesKey("dynamic_colors")
        private val LAST_EXPORT_KEY = longPreferencesKey("last_export_timestamp")  // ← добавь
        private val LAST_IMPORT_KEY = longPreferencesKey("last_import_timestamp")  // ← добавь

        const val THEME_FOLLOW_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2
    }

    val themeMode: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[THEME_MODE_KEY] ?: THEME_FOLLOW_SYSTEM }

    val dynamicColors: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[DYNAMIC_COLORS_KEY] ?: false }

    val lastExportTimestamp: Flow<Long?> = context.dataStore.data
        .map { it[LAST_EXPORT_KEY] }

    val lastImportTimestamp: Flow<Long?> = context.dataStore.data
        .map { it[LAST_IMPORT_KEY] }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { preferences -> preferences[THEME_MODE_KEY] = mode }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[DYNAMIC_COLORS_KEY] = enabled }
    }

    suspend fun saveLastExportTimestamp(timestamp: Long) {
        context.dataStore.edit { it[LAST_EXPORT_KEY] = timestamp }
    }

    suspend fun saveLastImportTimestamp(timestamp: Long) {
        context.dataStore.edit { it[LAST_IMPORT_KEY] = timestamp }
    }
}