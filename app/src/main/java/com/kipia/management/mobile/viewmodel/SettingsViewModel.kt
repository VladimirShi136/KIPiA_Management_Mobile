package com.kipia.management.mobile.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.managers.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncManager: SyncManager
) : ViewModel() {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    fun exportDatabase(outputUri: Uri) {
        viewModelScope.launch {
            _syncState.value = SyncState.Loading("Экспорт...")
            syncManager.exportToZip(outputUri).fold(
                onSuccess = {
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
