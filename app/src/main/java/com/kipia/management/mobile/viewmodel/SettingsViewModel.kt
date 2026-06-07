package com.kipia.management.mobile.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.managers.SyncManager
import com.kipia.management.mobile.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncManager: SyncManager,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    val lastExportTimestamp: StateFlow<Long?> = preferencesRepository.lastExportTimestamp
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val lastImportTimestamp: StateFlow<Long?> = preferencesRepository.lastImportTimestamp
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun exportDatabase(outputUri: Uri) {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading("Экспорт...")
            syncManager.exportToZip(outputUri).fold(
                onSuccess = {
                    preferencesRepository.saveLastExportTimestamp(System.currentTimeMillis())
                    _syncState.value = SyncState.ExportSuccess
                },
                onFailure = { e ->
                    _syncState.value = SyncState.Error(e.message ?: "Ошибка экспорта")
                }
            )
        }
    }

    fun importDatabase(inputUri: Uri) {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading("Импорт...")
            syncManager.importFromZip(inputUri).fold(
                onSuccess = { stats ->
                    if (stats.conflicts.isNotEmpty()) {
                        // Если есть конфликты, переходим в состояние ожидания решения
                        _syncState.value = SyncState.ConflictsDetected(stats.conflicts, stats)
                    } else {
                        preferencesRepository.saveLastImportTimestamp(System.currentTimeMillis())
                        _syncState.value = SyncState.ImportSuccess(stats)
                    }
                },
                onFailure = { e ->
                    _syncState.value = SyncState.Error(e.message ?: "Ошибка импорта")
                }
            )
        }
    }

    /**
     * Вызывается из UI после того, как пользователь выбрал решения для всех конфликтов
     */
    fun resolveConflicts(resolutions: List<SyncManager.ConflictResolution>) {
        val currentState = _syncState.value
        if (currentState is SyncState.ConflictsDetected) {
            viewModelScope.launch {
                _syncState.value = SyncState.Loading("Применение решений...")
                syncManager.applyConflictResolutions(currentState.conflicts, resolutions).fold(
                    onSuccess = {
                        preferencesRepository.saveLastImportTimestamp(System.currentTimeMillis())
                        // После разрешения конфликтов считаем импорт успешным
                        // Можно передать накопленную статистику
                        _syncState.value = SyncState.ImportSuccess(currentState.initialStats)
                    },
                    onFailure = { e ->
                        _syncState.value = SyncState.Error(e.message ?: "Ошибка разрешения конфликтов")
                    }
                )
            }
        }
    }

    fun resetState() {
        _syncState.value = SyncState.Idle
    }
}

sealed class SyncState {
    data object Idle : SyncState()
    data class Loading(val message: String) : SyncState()
    data object ExportSuccess : SyncState()
    data class ImportSuccess(val stats: SyncManager.SyncStats) : SyncState()
    data class Error(val message: String) : SyncState()
    
    // Новое состояние для ручного разрешения конфликтов
    data class ConflictsDetected(
        val conflicts: List<SyncManager.ConflictInfo>,
        val initialStats: SyncManager.SyncStats
    ) : SyncState()
}
