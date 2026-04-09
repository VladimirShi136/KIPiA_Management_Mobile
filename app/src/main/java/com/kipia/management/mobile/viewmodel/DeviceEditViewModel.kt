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
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DeviceEditViewModel @Inject constructor(
    private val repository: DeviceRepository,
    private val photoManager: PhotoManager,
    private val schemeSyncUseCase: SchemeSyncUseCase,
    private val notificationManager: NotificationManager
) : ViewModel() {

    private val _device = MutableStateFlow<Device?>(null) // ★ ИЗМЕНЯЕМ на nullable
    val device: StateFlow<Device?> = _device

    private var originalDevice: Device? = null

    private val _uiState = MutableStateFlow(DeviceEditUiState())
    val uiState: StateFlow<DeviceEditUiState> = _uiState
    private val _isLocationDropdownExpanded = MutableStateFlow(false)
    val isLocationDropdownExpanded: StateFlow<Boolean> = _isLocationDropdownExpanded.asStateFlow()

    init {
        // Валидируем форму при каждом изменении устройства
        viewModelScope.launch {
            _device.collect { device ->
                if (device != null) {
                    println("DEBUG init: Устройство изменено, валидация...")
                    validateForm(device)
                }
            }
        }

        viewModelScope.launch {
            _uiState.collect { uiState ->
                println("DEBUG init: UIState изменен: isFormValid=${uiState.isFormValid}, errors=${uiState.validationErrors}")
            }
        }
    }

    fun loadDevice(deviceId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repository.getDeviceById(deviceId).collect { loadedDevice ->
                    if (loadedDevice == null) {
                        _uiState.value = _uiState.value.copy(
                            error = "Прибор не найден",
                            isLoading = false
                        )
                        _device.value = null
                    } else {
                        // ★★★ ПРОВЕРЯЕМ ЦЕЛОСТНОСТЬ ФОТО ★★★
                        val validatedDevice = validateAndFixDevicePhotos(loadedDevice)
                        _device.value = validatedDevice
                        originalDevice = validatedDevice // ★ СОХРАНЯЕМ оригинал

                        _uiState.value = _uiState.value.copy(
                            isLoading = false
                            // Убираем isFavorite, так как его нет в DeviceEditUiState
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки: ${e.message}"
                )
            }
        }
    }

    /**
     * Проверяет и исправляет целостность фото устройства
     */
    private suspend fun validateAndFixDevicePhotos(device: Device): Device {
        // Получаем все фото из папки текущей локации
        val currentLocationDir = photoManager.getLocationDir(device.location)
        val unknownDir = photoManager.getLocationDir("unknown")

        val validPhotos = mutableListOf<String>()
        var needFix = false

        device.photos.forEach { fileName ->
            // Проверяем наличие файла в текущей локации
            val fileInCurrent = File(currentLocationDir, fileName)
            // Проверяем наличие файла в папке unknown
            val fileInUnknown = File(unknownDir, fileName)

            when {
                fileInCurrent.exists() -> {
                    // Фото в правильной папке
                    validPhotos.add(fileName)
                }
                fileInUnknown.exists() -> {
                    // Фото в папке unknown - перемещаем в текущую локацию
                    Timber.d("Найдено фото в unknown для устройства ${device.id}: $fileName")
                    try {
                        fileInUnknown.copyTo(fileInCurrent, overwrite = true)
                        if (fileInCurrent.exists()) {
                            fileInUnknown.delete()
                            validPhotos.add(fileName)
                            needFix = true
                            Timber.d("  ✓ Фото перемещено в ${device.location}")
                        } else {
                            Timber.e("  ✗ Не удалось скопировать фото")
                            validPhotos.add(fileName)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Ошибка при перемещении фото: $fileName")
                        validPhotos.add(fileName)
                    }
                }
                else -> {
                    // Фото потеряно - пропускаем
                    Timber.w("Фото потеряно для устройства ${device.id}: $fileName")
                    needFix = true
                    // Не добавляем в validPhotos - удаляем из списка
                }
            }
        }

        // Если список фото изменился, обновляем устройство в БД
        if (needFix && validPhotos.size != device.photos.size) {
            Timber.d("Обновляем список фото для устройства ${device.id}: ${device.photos.size} -> ${validPhotos.size}")
            val updatedDevice = device.copy(photos = validPhotos)
            repository.updateDevice(updatedDevice)
            return updatedDevice
        }

        return device
    }

    fun updateDevice(transform: (Device) -> Device) {
        _device.value?.let { currentDevice ->
            val updatedDevice = transform(currentDevice)
            _device.value = updatedDevice
        }
    }

    fun saveDevice() {
        viewModelScope.launch {
            val currentDevice = _device.value
            if (currentDevice == null) {
                _uiState.update {
                    it.copy(
                        error = "Устройство не загружено",
                        isLoading = false
                    )
                }
                return@launch
            }

            println("DEBUG: Начало сохранения")
            println("DEBUG: Текущее устройство: тип='${currentDevice.type}', инв='${currentDevice.inventoryNumber}', локация='${currentDevice.location}'")

            // Проверяем обязательные поля напрямую
            val validationErrors = mutableListOf<String>()
            if (currentDevice.type.isBlank()) validationErrors.add("type")
            if (currentDevice.inventoryNumber.isBlank()) validationErrors.add("inventoryNumber")
            if (currentDevice.location.isBlank()) validationErrors.add("location")

            println("DEBUG: Проверка полей: ошибки=$validationErrors")

            if (validationErrors.isNotEmpty()) {
                println("DEBUG: Форма не валидна: $validationErrors")
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

            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                println("DEBUG: Сохранение устройства: ${currentDevice.type} - ${currentDevice.inventoryNumber}")

                if (!isValidStatus(currentDevice.status)) {
                    throw IllegalArgumentException("Некорректный статус: ${currentDevice.status}")
                }

                // ★★★ НОВОЕ: миграция фото при изменении локации ★★★
                val deviceToSave = if (originalDevice != null) {
                    photoManager.migrateIfLocationChanged(originalDevice!!, currentDevice)
                } else {
                    currentDevice
                }

                val result = if (deviceToSave.id > 0) {
                    // Обновление существующего
                    val rowsUpdated = repository.updateDeviceWithTimestamp(deviceToSave)
                    if (rowsUpdated > 0) {
                        println("DEBUG: Устройство обновлено, затронуто строк: $rowsUpdated")
                        deviceToSave
                    } else {
                        throw IllegalStateException("Устройство не найдено для обновления")
                    }
                } else {
                    // Создание нового
                    val newId = repository.insertDeviceWithTimestamp(deviceToSave)
                    println("DEBUG: Устройство вставлено, новый ID: $newId")
                    deviceToSave.copy(id = newId.toInt())
                }

                // Обновляем состояние с новым ID
                if (result.id != currentDevice.id) {
                    _device.value = result
                }

                // ★ ОБНОВЛЯЕМ originalDevice после успешного сохранения
                originalDevice = result

                schemeSyncUseCase.syncSchemeOnDeviceSave(result)

                println("DEBUG: Устройство успешно сохранено, ID: ${result.id}")

                notificationManager.notifyDeviceSaved(currentDevice.getDisplayName())

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSaved = true,
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

    fun deleteDevice(deleteScheme: Boolean = false) {
        viewModelScope.launch {
            val deviceToDelete = _device.value
            if (deviceToDelete == null || deviceToDelete.id <= 0) {
                _uiState.update { it.copy(error = "Нельзя удалить несохраненное устройство") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                println("DEBUG: Удаление устройства ID: ${deviceToDelete.id}, deleteScheme=$deleteScheme")

                // Удаляем физические файлы фото до удаления записи из БД
                val deletedPhotos = photoManager.deleteAllDevicePhotos(deviceToDelete)
                println("DEBUG: Удалено $deletedPhotos фото для ${deviceToDelete.getDisplayName()}")

                // Удаляем устройство
                val rowsDeleted = repository.deleteDevice(deviceToDelete)

                if (rowsDeleted > 0) {
                    println("DEBUG: Устройство успешно удалено, затронуто строк: $rowsDeleted")

                    if (deleteScheme && deviceToDelete.location.isNotBlank()) {
                        schemeSyncUseCase.deleteSchemeIfEmpty(deviceToDelete.location)
                    }

                    notificationManager.notifyDeviceDeleted(
                        deviceName = deviceToDelete.getDisplayName(),
                        withScheme = deleteScheme
                    )

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

    fun savePhotoFromUri(uri: android.net.Uri): String? {
        return photoManager.savePhotoFromUri(uri)
    }

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