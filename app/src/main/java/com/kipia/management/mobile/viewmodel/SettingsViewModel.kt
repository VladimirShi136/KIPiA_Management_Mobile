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
                    preferencesRepository.saveLastImportTimestamp(System.currentTimeMillis())
                    _syncState.value = SyncState.ImportSuccess(stats)
                },
                onFailure = { e ->
                    _syncState.value = SyncState.Error(e.message ?: "Ошибка импорта")
                }
            )
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
}
