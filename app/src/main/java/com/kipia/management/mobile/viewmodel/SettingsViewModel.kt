package com.kipia.management.mobile.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.managers.SyncManager
import com.kipia.management.mobile.managers.SyncState
import com.kipia.management.mobile.repository.PreferencesRepository
import com.kipia.management.mobile.services.SyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncManager: SyncManager,
    private val preferencesRepository: PreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Подписываемся на глобальное состояние из SyncManager
    val syncState: StateFlow<SyncState> = syncManager.syncState

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
        val intent = Intent(context, SyncService::class.java).apply {
            action = SyncService.ACTION_START_EXPORT
            putExtra(SyncService.EXTRA_URI, outputUri)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun importDatabase(inputUri: Uri, importDeleted: Boolean = false) {
        val intent = Intent(context, SyncService::class.java).apply {
            action = SyncService.ACTION_START_IMPORT
            putExtra(SyncService.EXTRA_URI, inputUri)
            putExtra(SyncService.EXTRA_IMPORT_DELETED, importDeleted)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun resolveGhostDevices(importThem: Boolean) {
        syncManager.resolveGhostDevices(importThem)
    }

    fun resolveConflicts(resolutions: List<SyncManager.ConflictResolution>) {
        syncManager.resolveConflicts(resolutions)
    }

    fun resetState() {
        syncManager.resetState()
    }
}
