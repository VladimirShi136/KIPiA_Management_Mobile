package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.domain.usecase.SchemeSyncUseCase
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.ui.shared.NotificationManager
import com.kipia.management.mobile.ui.theme.DeviceStatus
import com.kipia.management.mobile.managers.PhotoManager
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
    private val schemeSyncUseCase: SchemeSyncUseCase,
    private val notificationManager: NotificationManager
) : ViewModel() {

    private val _device = MutableStateFlow(Device.createEmpty())
    val device: StateFlow<Device> = _device

    private val _uiState = MutableStateFlow(DeviceEditUiState())
    val uiState: StateFlow<DeviceEditUiState> = _uiState
    private val _isLocationDropdownExpanded = MutableStateFlow(false)
    val isLocationDropdownExpanded: StateFlow<Boolean> = _isLocationDropdownExpanded.asStateFlow()

    init {
        // Валидируем форму при каждом изменении устройства
        viewModelScope.launch {
            _device.collect { device ->
                println("DEBUG init: Устройство изменено, валидация...")
                validateForm(device)
            }
        }

        // ★★★★ ДОБАВЛЕНО: Логирование изменений UIState ★★★★
        viewModelScope.launch {
            _uiState.collect { uiState ->
                println("DEBUG init: UIState изменен: isFormValid=${uiState.isFormValid}, errors=${uiState.validationErrors}")
            }
        }
    }

    fun loadDevice(deviceId: Int) {
        viewModelScope.launch {
            println("DEBUG: Загрузка устройства ID: $deviceId")
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getDeviceById(deviceId).collect { loadedDevice ->
                    println("DEBUG: Загружено устройство: $loadedDevice")
                    loadedDevice?.let {
                        _device.value = it
                    }
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                println("DEBUG: Ошибка загрузки: ${e.message}")
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
        val updatedDevice = transform(_device.value)
        _device.value = updatedDevice
    }

    fun saveDevice() {
        viewModelScope.launch {
            println("DEBUG: Начало сохранения")

            // Получаем актуальные значения напрямую из device
            val currentDevice = _device.value
            println("DEBUG: Текущее устройство: тип='${currentDevice.type}', инв='${currentDevice.inventoryNumber}', локация='${currentDevice.location}'")

            // Проверяем обязательные поля напрямую
            val validationErrors = mutableListOf<String>()
            if (currentDevice.type.isBlank()) validationErrors.add("type")
            if (currentDevice.inventoryNumber.isBlank()) validationErrors.add("inventoryNumber")
            if (currentDevice.location.isBlank()) validationErrors.add("location")

            println("DEBUG: Проверка полей: ошибки=$validationErrors")

            if (validationErrors.isNotEmpty()) {
                println("DEBUG: Форма не валидна: $validationErrors")

                // Обновляем UIState с ошибками
                _uiState.update {
                    it.copy(
                        error = "Заполните обязательные поля",
                        typeError = if (currentDevice.type.isBlank()) "Укажите тип прибора" else null,
                        inventoryNumberError = if (currentDevice.inventoryNumber.isBlank()) "Укажите инвентарный номер" else null,
                        locationError = if (currentDevice.location.isBlank()) "Укажите место установки" else null,
                        validationErrors = validationErrors,
                        isFormValid = false
                    )
                }
                return@launch
            }

            // Форма валидна - продолжаем сохранение
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                println("DEBUG: Сохранение устройства: ${currentDevice.type} - ${currentDevice.inventoryNumber}")

                if (!isValidStatus(currentDevice.status)) {
                    throw IllegalArgumentException("Некорректный статус: ${currentDevice.status}")
                }

                val result = if (currentDevice.id > 0) {
                    val rowsUpdated = repository.updateDevice(currentDevice)
                    if (rowsUpdated > 0) {
                        println("DEBUG: Устройство обновлено, затронуто строк: $rowsUpdated")
                        currentDevice
                    } else {
                        throw IllegalStateException("Устройство не найдено для обновления")
                    }
                } else {
                    val newId = repository.insertDevice(currentDevice)
                    println("DEBUG: Устройство вставлено, новый ID: $newId")
                    currentDevice.copy(id = newId.toInt())
                }

                // Обновляем состояние с новым ID
                if (result.id != currentDevice.id) {
                    _device.value = result
                }

                schemeSyncUseCase.syncSchemeOnDeviceSave(result)

                println("DEBUG: Устройство успешно сохранено, ID: ${result.id}")

                // ★★★★ Отправляем уведомление через notificationManager ★★★★
                notificationManager.notifyDeviceSaved(currentDevice.getDisplayName())

                // Сбрасываем состояние загрузки
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSaved = true, // ← ДОБАВИТЬ ЭТУ СТРОКУ!
                        error = null
                    )
                }

            } catch (e: Exception) {
                println("DEBUG: Ошибка сохранения: ${e.message}")
                _uiState.update {
                    it.copy(
                        error = "Ошибка сохранения: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun isValidStatus(status: String): Boolean {
        return DeviceStatus.ALL_STATUSES.contains(status)
    }

    // ИСПРАВЛЕНИЕ: добавить параметр deleteScheme
    fun deleteDevice(deleteScheme: Boolean = false) {
        viewModelScope.launch {
            val deviceToDelete = _device.value
            if (deviceToDelete.id <= 0) {
                _uiState.update { it.copy(error = "Нельзя удалить несохраненное устройство") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                println("DEBUG: Удаление устройства ID: ${deviceToDelete.id}, deleteScheme=$deleteScheme")

                // Удаляем устройство
                val rowsDeleted = repository.deleteDevice(deviceToDelete)

                if (rowsDeleted > 0) {
                    println("DEBUG: Устройство успешно удалено, затронуто строк: $rowsDeleted")

                    // ★★★★ ИСПРАВЛЕНИЕ: Удаляем схему если нужно ★★★★
                    if (deleteScheme && deviceToDelete.location.isNotBlank()) {
                        schemeSyncUseCase.deleteSchemeIfEmpty(deviceToDelete.location)
                    }

                    // ★★★★ Отправляем уведомление с правильным флагом ★★★★
                    notificationManager.notifyDeviceDeleted(
                        deviceName = deviceToDelete.getDisplayName(),
                        withScheme = deleteScheme
                    )

                    // Устанавливаем флаг удаления для навигации
                    _uiState.update {
                        it.copy(
                            isDeleted = true,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                println("DEBUG: Ошибка удаления: ${e.message}")
                _uiState.update {
                    it.copy(
                        error = "Ошибка удаления: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }


    private fun validateForm(device: Device) {
        println("DEBUG validateForm: тип='${device.type}', инв='${device.inventoryNumber}', локация='${device.location}'")

        val errors = mutableListOf<String>()

        val typeError = if (device.type.isBlank()) {
            errors.add("type")
            "Укажите тип прибора"
        } else {
            null
        }

        val inventoryNumberError = if (device.inventoryNumber.isBlank()) {
            errors.add("inventoryNumber")
            "Укажите инвентарный номер"
        } else {
            null
        }

        val locationError = if (device.location.isBlank()) {
            errors.add("location")
            "Укажите место установки"
        } else {
            null
        }

        val statusError = if (!isValidStatus(device.status)) {
            errors.add("status")
            "Некорректный статус"
        } else {
            null
        }

        println("DEBUG validateForm: ошибки = $errors")

        _uiState.update {
            it.copy(
                isFormValid = errors.isEmpty(),
                validationErrors = errors,
                typeError = typeError,
                inventoryNumberError = inventoryNumberError,
                locationError = locationError,
                statusError = statusError
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

    fun expandLocationDropdown() {
        _isLocationDropdownExpanded.value = true
    }

    fun collapseLocationDropdown() {
        _isLocationDropdownExpanded.value = false
    }

    fun clearSaveState() {
        _uiState.update { it.copy(isSaved = false, isDeleted = false) }
    }
}

data class DeviceEditUiState(
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null,
    val isFormValid: Boolean = false,
    val validationErrors: List<String> = emptyList(),
    val typeError: String? = null,
    val inventoryNumberError: String? = null,
    val locationError: String? = null,
    val statusError: String? = null
)