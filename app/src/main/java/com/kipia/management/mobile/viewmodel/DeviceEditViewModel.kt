package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceEditViewModel @Inject constructor(
    private val repository: DeviceRepository
) : ViewModel() {

    private val _device = MutableStateFlow<Device?>(null)
    val device: StateFlow<Device?> = _device.asStateFlow()

    private val _validationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val validationErrors: StateFlow<Map<String, String>> = _validationErrors.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ИСПРАВЛЕНО: Убрали 'private' - теперь метод публичный
    fun loadDevice(deviceId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val device = if (deviceId > 0) {
                    repository.getDeviceById(deviceId)
                } else {
                    // Создаем новое устройство
                    Device(
                        id = 0,
                        inventoryNumber = "",
                        type = "",
                        name = null,
                        manufacturer = null,
                        year = null,
                        location = "",
                        status = "В работе",
                        accuracyClass = null,
                        measurementLimit = null,
                        valveNumber = null,
                        additionalInfo = null,
                        photoPath = null,
                        photos = null
                    )
                }
                _device.value = device
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ИСПРАВЛЕНО: Убрали 'private' - теперь метод публичный
    fun saveDevice(device: Device) {
        if (!validateDevice(device)) {
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (device.id == 0) {
                    // Новое устройство
                    repository.insertDevice(device)
                } else {
                    // Обновление существующего
                    repository.updateDevice(device)
                }
                _saveSuccess.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                // Можно добавить обработку ошибок сохранения
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun validateDevice(device: Device): Boolean {
        val errors = mutableMapOf<String, String>()

        if (device.inventoryNumber.isBlank()) {
            errors["inventoryNumber"] = "Инвентарный номер обязателен"
        }

        if (device.type.isBlank()) {
            errors["type"] = "Тип прибора обязателен"
        }

        if (device.location.isBlank()) {
            errors["location"] = "Место установки обязательно"
        }

        // Проверка уникальности инвентарного номера
        // (можно добавить асинхронную проверку в будущем)

        _validationErrors.value = errors
        return errors.isEmpty()
    }

    fun clearErrors() {
        _validationErrors.value = emptyMap()
    }

    fun deleteDevice() {
        viewModelScope.launch {
            _device.value?.let { device ->
                _isLoading.value = true
                try {
                    repository.deleteDevice(device)
                    _saveSuccess.value = true
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
}