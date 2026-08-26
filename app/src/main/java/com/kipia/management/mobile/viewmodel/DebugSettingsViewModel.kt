package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.managers.DatabaseIntegrityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugSettingsViewModel @Inject constructor(
    private val integrityManager: DatabaseIntegrityManager
) : ViewModel() {

    private val _isCheckingIntegrity = MutableStateFlow(false)
    val isCheckingIntegrity: StateFlow<Boolean> = _isCheckingIntegrity.asStateFlow()

    private val _integrityResult = MutableStateFlow<DatabaseIntegrityManager.IntegrityCheckResult?>(null)
    val integrityResult: StateFlow<DatabaseIntegrityManager.IntegrityCheckResult?> = _integrityResult.asStateFlow()

    private val _isCleaningUp = MutableStateFlow(false)
    val isCleaningUp: StateFlow<Boolean> = _isCleaningUp.asStateFlow()

    private val _cleanupMessage = MutableStateFlow<String?>(null)
    val cleanupMessage: StateFlow<String?> = _cleanupMessage.asStateFlow()

    fun checkIntegrity() {
        viewModelScope.launch {
            _isCheckingIntegrity.value = true
            try {
                val result = integrityManager.checkIntegrity()
                _integrityResult.value = result
            } finally {
                _isCheckingIntegrity.value = false
            }
        }
    }

    fun performCleanup(result: DatabaseIntegrityManager.IntegrityCheckResult) {
        viewModelScope.launch {
            _isCleaningUp.value = true
            try {
                val results = mutableListOf<String>()

                if (result.orphanedFiles.isNotEmpty()) {
                    val orphanResult = integrityManager.cleanupOrphanedFiles(result.orphanedFiles)
                    orphanResult.onSuccess { message ->
                        results.add(message)
                    }.onFailure { error ->
                        results.add("Ошибка при очистке сирот-файлов: ${error.message}")
                    }
                }

                if (result.emptyFolders.isNotEmpty()) {
                    val foldersResult = integrityManager.cleanupEmptyFolders(result.emptyFolders)
                    foldersResult.onSuccess { message ->
                        results.add(message)
                    }.onFailure { error ->
                        results.add("Ошибка при удалении пустых папок: ${error.message}")
                    }
                }

                _cleanupMessage.value = results.joinToString("\n")
            } finally {
                _isCleaningUp.value = false
            }
        }
    }
}

