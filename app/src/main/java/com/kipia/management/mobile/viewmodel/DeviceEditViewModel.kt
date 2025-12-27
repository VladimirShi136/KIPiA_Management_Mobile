package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceEditViewModel @Inject constructor(
    private val repository: DeviceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val deviceId = savedStateHandle.get<Int>("deviceId") ?: 0

    private val _device = MutableStateFlow<Device?>(null)
    val device: StateFlow<Device?> = _device.asStateFlow()

    private val _validationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val validationErrors: StateFlow<Map<String, String>> = _validationErrors.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    init {
        loadDevice()
    }

    private fun loadDevice() {
        if (deviceId > 0) {
            viewModelScope.launch {
                _isLoading.value = true
                _device.value = repository.getDeviceById(deviceId)
                _isLoading.value = false
            }
        } else {
            // Новый прибор
            _device.value = Device(
                id = 0,
                type = "", // обязательно
                name = "", // было не передано
                manufacturer = null, // было не передано
                inventoryNumber = "", // обязательно
                year = null, // было не передано
                measurementLimit = null, // было не передано
                accuracyClass = null, // было не передано
                location = "", // обязательно
                valveNumber = null, // было не передано
                status = "В работе",
                additionalInfo = null, // было не передано
                photoPath = null, // было не передано
                photos = null // было не передано
            )
        }
    }

    suspend fun validateAndSave(
        inventoryNumber: String,
        type: String,
        name: String?,
        manufacturer: String?,
        year: Int?,
        location: String,
        status: String,
        accuracyClass: Double?,
        measurementLimit: String?,
        valveNumber: String?,
        additionalInfo: String?
    ): Boolean {

        val errors = mutableMapOf<String, String>()

        // Валидация инвентарного номера
        if (inventoryNumber.isBlank()) {
            errors["inventoryNumber"] = "required"
        } else {
            val isValid = repository.validateInventoryNumber(inventoryNumber, deviceId)
            if (!isValid) {
                errors["inventoryNumber"] = "unique"
            }
        }

        // Валидация типа
        if (type.isBlank()) {
            errors["type"] = "required"
        }

        // Валидация места установки
        if (location.isBlank()) {
            errors["location"] = "required"
        }

        _validationErrors.value = errors

        if (errors.isNotEmpty()) {
            return false
        }

        // Создание/обновление прибора
        val currentDevice = _device.value ?: Device(
            id = 0,
            type = "", // обязательно
            name = "", // было не передано
            manufacturer = null, // было не передано
            inventoryNumber = "", // обязательно
            year = null, // было не передано
            measurementLimit = null, // было не передано
            accuracyClass = null, // было не передано
            location = "", // обязательно
            valveNumber = null, // было не передано
            status = "В работе",
            additionalInfo = null, // было не передано
            photoPath = null, // было не передано
            photos = null // было не передано
        )

        val updatedDevice = currentDevice.copy(
            inventoryNumber = inventoryNumber.trim(),
            type = type.trim(),
            name = name?.trim(),
            manufacturer = manufacturer?.trim(),
            year = year,
            location = location.trim(),
            status = status,
            accuracyClass = accuracyClass,
            measurementLimit = measurementLimit?.trim(),
            valveNumber = valveNumber?.trim(),
            additionalInfo = additionalInfo?.trim()
        )

        viewModelScope.launch {
            if (deviceId > 0) {
                repository.updateDevice(updatedDevice)
            } else {
                repository.insertDevice(updatedDevice)
            }
            _saveSuccess.value = true
        }

        return true
    }

    fun deleteDevice() {
        viewModelScope.launch {
            if (deviceId > 0) {
                repository.deleteDeviceById(deviceId)
                _saveSuccess.value = true
            }
        }
    }

    fun clearSuccess() {
        _saveSuccess.value = false
    }
}