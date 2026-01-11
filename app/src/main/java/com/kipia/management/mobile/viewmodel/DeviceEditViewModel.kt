package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.utils.PhotoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceEditViewModel @Inject constructor(
    private val repository: DeviceRepository,
    private val photoManager: PhotoManager
) : ViewModel() {

    private val _device = MutableStateFlow(Device.createEmpty())
    val device: StateFlow<Device> = _device

    private val _uiState = MutableStateFlow(DeviceEditUiState())
    val uiState: StateFlow<DeviceEditUiState> = _uiState

    fun loadDevice(deviceId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getDeviceById(deviceId).collect { loadedDevice ->
                    loadedDevice?.let {
                        _device.value = it
                        validateForm()
                    }
                    // Сброс isLoading после получения первого значения
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

                // Если у устройства есть ID - обновляем, иначе создаем новое
                if (currentDevice.id > 0) {
                    repository.updateDevice(currentDevice) // ← ДОБАВЬТЕ ЭТОТ МЕТОД
                } else {
                    repository.insertDevice(currentDevice)
                }

                _uiState.update { it.copy(isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Ошибка сохранения: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
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

        // Проверка обязательных полей
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

        _uiState.update {
            it.copy(
                isFormValid = errors.isEmpty(),
                validationErrors = errors
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // Добавьте методы для работы с фото
    suspend fun savePhotoFromUri(uri: android.net.Uri): String? {
        return photoManager.savePhotoFromUri(uri)
    }
}

data class DeviceEditUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val isFormValid: Boolean = false,
    val validationErrors: List<String> = emptyList(),
    val isTypeExpanded: Boolean = false,
    val isStatusExpanded: Boolean = false,
    val typeError: String? = null,
    val inventoryNumberError: String? = null,
    val locationError: String? = null
)