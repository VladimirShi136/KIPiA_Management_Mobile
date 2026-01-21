package com.kipia.management.mobile.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.domain.usecase.SchemeSyncUseCase
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.ui.theme.DeviceStatus
import com.kipia.management.mobile.utils.PhotoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceEditViewModel @Inject constructor(
    private val repository: DeviceRepository,
    private val photoManager: PhotoManager,
    private val schemeSyncUseCase: SchemeSyncUseCase
) : ViewModel() {

    private val _device = MutableStateFlow(Device.createEmpty())
    val device: StateFlow<Device> = _device

    private val _uiState = MutableStateFlow(DeviceEditUiState())
    val uiState: StateFlow<DeviceEditUiState> = _uiState

    // ★★★★ ИСПРАВЛЯЕМ: Используем MutableStateFlow вместо mutableStateOf ★★★★
    private val _isLocationDropdownExpanded = MutableStateFlow(false)
    val isLocationDropdownExpanded: StateFlow<Boolean> = _isLocationDropdownExpanded.asStateFlow()

    fun loadDevice(deviceId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getDeviceById(deviceId).collect { loadedDevice ->
                    loadedDevice?.let {
                        _device.value = it
                        validateForm()
                    }
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка загрузки: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateDevice(transform: (Device) -> Device) {
        _device.update { transform(it) }
        validateForm()
    }

    fun saveDevice() {
        viewModelScope.launch {
            if (!_uiState.value.isFormValid) return@launch

            _uiState.update { it.copy(isLoading = true) }
            try {
                val currentDevice = _device.value

                if (!isValidStatus(currentDevice.status)) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Некорректный статус: ${currentDevice.status}"
                        )
                    }
                    return@launch
                }

                if (currentDevice.id > 0) {
                    repository.updateDevice(currentDevice)
                } else {
                    repository.insertDevice(currentDevice)
                }

                schemeSyncUseCase.syncSchemeOnDeviceSave(currentDevice)

                _uiState.update { it.copy(isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Ошибка сохранения: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun isValidStatus(status: String): Boolean {
        return DeviceStatus.ALL_STATUSES.contains(status)
    }

    fun deleteDevice() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.deleteDevice(_device.value)
                _uiState.update { it.copy(isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Ошибка удаления: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun validateForm() {
        val currentDevice = _device.value
        val errors = mutableListOf<String>()

        if (currentDevice.type.isBlank()) {
            errors.add("type")
            _uiState.update { it.copy(typeError = "Укажите тип прибора") }
        } else {
            _uiState.update { it.copy(typeError = null) }
        }

        if (currentDevice.inventoryNumber.isBlank()) {
            errors.add("inventoryNumber")
            _uiState.update { it.copy(inventoryNumberError = "Укажите инвентарный номер") }
        } else {
            _uiState.update { it.copy(inventoryNumberError = null) }
        }

        if (currentDevice.location.isBlank()) {
            errors.add("location")
            _uiState.update { it.copy(locationError = "Укажите место установки") }
        } else {
            _uiState.update { it.copy(locationError = null) }
        }

        if (!isValidStatus(currentDevice.status)) {
            errors.add("status")
            _uiState.update { it.copy(statusError = "Некорректный статус") }
        } else {
            _uiState.update { it.copy(statusError = null) }
        }

        _uiState.update {
            it.copy(
                isFormValid = errors.isEmpty(),
                validationErrors = errors
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, statusError = null) }
    }

    suspend fun savePhotoFromUri(uri: android.net.Uri): String? {
        return photoManager.savePhotoFromUri(uri)
    }

    // ★★★★ ДОБАВЛЯЕМ: Список всех местоположений ★★★★
    val allLocations = repository.getAllLocations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ★★★★ ДОБАВЛЯЕМ: Методы для управления выпадающим списком ★★★★
    fun expandLocationDropdown() {
        _isLocationDropdownExpanded.value = true
    }

    fun collapseLocationDropdown() {
        _isLocationDropdownExpanded.value = false
    }
}

data class DeviceEditUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val isFormValid: Boolean = false,
    val validationErrors: List<String> = emptyList(),
    val typeError: String? = null,
    val inventoryNumberError: String? = null,
    val locationError: String? = null,
    val statusError: String? = null
)