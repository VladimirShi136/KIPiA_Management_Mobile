package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.ViewModel
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.domain.usecase.SchemeSyncUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DeviceDeleteViewModel @Inject constructor(
    private val schemeSyncUseCase: SchemeSyncUseCase
) : ViewModel() {

    private val _showDeleteDialog = MutableStateFlow<DeleteDialogData?>(null)
    val showDeleteDialog: StateFlow<DeleteDialogData?> = _showDeleteDialog

    /**
     * Проверяет нужно ли показывать диалог при удалении устройства
     */
    suspend fun checkAndShowDialog(device: Device): Boolean {
        val scheme = schemeSyncUseCase.checkSchemeOnDeviceDelete(device)

        return if (scheme != null) {
            _showDeleteDialog.value = DeleteDialogData(
                device = device,
                scheme = scheme
            )
            true  // Нужно показать диалог
        } else {
            false // Можно удалять сразу
        }
    }

    fun dismissDialog() {
        _showDeleteDialog.value = null
    }
}

data class DeleteDialogData(
    val device: Device,
    val scheme: Scheme
)