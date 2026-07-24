package com.kipia.management.mobile.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.domain.usecase.SchemeSyncUseCase
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.ui.shared.NotificationManager
import com.kipia.management.mobile.ui.theme.DeviceStatus
import com.kipia.management.mobile.managers.PhotoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DeviceEditViewModel @Inject constructor(
    private val repository: DeviceRepository,
    private val photoManager: PhotoManager,
    private val schemeSyncUseCase: SchemeSyncUseCase,
    private val notificationManager: NotificationManager
) : ViewModel() {

    private val _device = MutableStateFlow<Device?>(null)
    val device: StateFlow<Device?> = _device

    private var originalDevice: Device? = null

    private val _uiState = MutableStateFlow(DeviceEditUiState())
    val uiState: StateFlow<DeviceEditUiState> = _uiState

    init {
        viewModelScope.launch {
            _device.collect { device ->
                if (device != null) {
                    validateForm(device)
                }
            }
        }
    }

    fun loadDevice(deviceId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.getDeviceById(deviceId).collect { loadedDevice ->
                    if (loadedDevice == null) {
                        _uiState.update { it.copy(error = "Прибор не найден", isLoading = false) }
                        _device.value = null
                    } else {
                        val validatedDevice = validateAndFixDevicePhotos(loadedDevice)
                        _device.value = validatedDevice
                        if (originalDevice == null) originalDevice = validatedDevice
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Ошибка загрузки: ${e.message}") }
            }
        }
    }

    private suspend fun validateAndFixDevicePhotos(device: Device): Device {
        val currentLocationDir = photoManager.getLocationDir(device.location)
        val unknownDir = photoManager.getLocationDir("unknown")
        val validPhotos = mutableListOf<String>()
        var needFix = false

        device.photos.forEach { fileName ->
            val fileInCurrent = File(currentLocationDir, fileName)
            val fileInUnknown = File(unknownDir, fileName)
            when {
                fileInCurrent.exists() -> validPhotos.add(fileName)
                fileInUnknown.exists() -> {
                    try {
                        fileInUnknown.copyTo(fileInCurrent, overwrite = true)
                        if (fileInCurrent.exists()) {
                            fileInUnknown.delete()
                            validPhotos.add(fileName)
                            needFix = true
                        } else validPhotos.add(fileName)
                    } catch (e: Exception) { validPhotos.add(fileName) }
                }
                else -> needFix = true
            }
        }

        if (needFix && validPhotos.size != device.photos.size) {
            val updatedDevice = device.copy(photos = validPhotos)
            repository.updateDeviceWithTimestamp(updatedDevice)
            return updatedDevice.withUpdatedNow()
        }
        return device
    }

    fun updateDevice(transform: (Device) -> Device) {
        _device.update { current ->
            transform(current ?: Device.createEmpty().copy(type = "", status = ""))
        }
    }

    fun addPhoto(uri: Uri) {
        viewModelScope.launch {
            val currentDevice = _device.value ?: return@launch
            _uiState.update { it.copy(isSaving = true) }

            try {
                var deviceToUse = if (originalDevice != null && originalDevice!!.location != currentDevice.location) {
                    photoManager.migrateIfLocationChanged(originalDevice!!, currentDevice)
                } else currentDevice

                if (deviceToUse.photos.size >= 10) {
                    val oldestPhoto = deviceToUse.photos.first()
                    photoManager.deleteDevicePhoto(deviceToUse, oldestPhoto)
                    deviceToUse = deviceToUse.removePhoto(oldestPhoto)
                }

                photoManager.savePhotoForDevice(deviceToUse, uri)
                    .onSuccess { result ->
                        onDevicePersisted(result.device)
                        _uiState.update { it.copy(isSaving = false) }
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isSaving = false) }
                        notificationManager.notifyError("Ошибка сохранения фото: ${e.message}")
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                notificationManager.notifyError("Ошибка: ${e.message}")
            }
        }
    }

    fun deletePhoto(fileName: String) {
        viewModelScope.launch {
            val currentDevice = _device.value ?: return@launch
            try {
                val deleted = photoManager.deleteDevicePhoto(currentDevice, fileName)
                if (deleted) {
                    onDevicePersisted(currentDevice.removePhoto(fileName))
                } else {
                    notificationManager.notifyError("Не удалось удалить файл")
                }
            } catch (e: Exception) {
                notificationManager.notifyError("Ошибка удаления: ${e.message}")
            }
        }
    }

    private fun onDevicePersisted(updatedDevice: Device) {
        _device.value = updatedDevice
        originalDevice = updatedDevice
    }

    fun saveDevice() {
        viewModelScope.launch {
            val currentDevice = _device.value ?: return@launch
            
            validateForm(currentDevice)
            
            if (!_uiState.value.isFormValid) {
                _uiState.update { it.copy(showValidationErrorCard = true) }
                notificationManager.notifyError("Заполните обязательные поля")
                return@launch
            }

            _uiState.update { it.copy(isSaving = true) }
            delay(500)

            try {
                // ПРОВЕРКА НА КОНФЛИКТ ИНВЕНТАРНОГО НОМЕРА
                val conflictDevice = repository.getDeviceByInventory(currentDevice.inventoryNumber)
                if (conflictDevice != null && conflictDevice.id != currentDevice.id) {
                    _uiState.update { it.copy(isSaving = false) }
                    if (conflictDevice.isDeleted()) {
                        notificationManager.notifyError(
                            "Номер ${currentDevice.inventoryNumber} занят удаленным прибором. " +
                            "Окончательно удалите его или используйте другой номер."
                        )
                    } else {
                        notificationManager.notifyError(
                            "Прибор с номером ${currentDevice.inventoryNumber} уже существует (Локация: ${conflictDevice.location})"
                        )
                    }
                    return@launch
                }

                val deviceToSave = if (originalDevice != null) {
                    photoManager.migrateIfLocationChanged(originalDevice!!, currentDevice)
                } else currentDevice

                val result = if (deviceToSave.id > 0) {
                    repository.updateDeviceWithTimestamp(deviceToSave)
                    deviceToSave.withUpdatedNow()
                } else {
                    val newId = repository.insertDeviceWithTimestamp(deviceToSave)
                    deviceToSave.copy(id = newId.toInt()).withUpdatedNow()
                }

                onDevicePersisted(result)
                schemeSyncUseCase.syncSchemeOnDeviceSave(result)
                _uiState.update { it.copy(isSaving = false, isSaved = true, showValidationErrorCard = false) }
                notificationManager.notifyDeviceSaved(currentDevice.getDisplayName())
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false) }
                notificationManager.notifyError("Ошибка сохранения: ${e.message}")
            }
        }
    }

    fun hasUnsavedChanges(): Boolean {
        val current = _device.value ?: return false
        val original = originalDevice ?: return current.type.isNotBlank() || current.inventoryNumber.isNotBlank()
        return current.copy(updatedAt = 0) != original.copy(updatedAt = 0)
    }

    fun deleteDevice(deletePhotos: Boolean, deleteScheme: Boolean = false) {
        viewModelScope.launch {
            val deviceToDelete = _device.value ?: return@launch
            _uiState.update { it.copy(isLoading = true) }
            try {
                if (deletePhotos) {
                    photoManager.deleteAllDevicePhotos(deviceToDelete)
                }

                if (repository.deleteDevice(deviceToDelete) > 0) {
                    if (deleteScheme) schemeSyncUseCase.deleteSchemeIfEmpty(deviceToDelete.location)
                    _uiState.update { it.copy(isDeleted = true, isLoading = false) }
                    notificationManager.notifyDeviceDeleted(deviceToDelete.getDisplayName(), deleteScheme)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                notificationManager.notifyError("Ошибка удаления: ${e.message}")
            }
        }
    }

    private fun validateForm(device: Device) {
        val errors = mutableListOf<String>()
        val typeErr = if (device.type.isBlank()) { errors.add("type"); "Укажите тип" } else null
        val nameErr = if (device.name.isNullOrBlank()) { errors.add("name"); "Укажите модель" } else null
        val invErr = if (device.inventoryNumber.isBlank()) { errors.add("inventoryNumber"); "Укажите номер" } else null
        val locErr = if (device.location.isBlank()) { errors.add("location"); "Укажите место" } else null
        val statusErr = if (device.status.isBlank()) { errors.add("status"); "Укажите статус" } else null
        
        val isValid = errors.isEmpty()
        
        _uiState.update { state ->
            state.copy(
                isFormValid = isValid, 
                validationErrors = errors, 
                typeError = typeErr, 
                nameError = nameErr,
                inventoryNumberError = invErr, 
                locationError = locErr,
                statusError = statusErr,
                // Если все поля заполнены, карточка ошибки исчезает автоматически
                showValidationErrorCard = if (isValid) false else state.showValidationErrorCard
            )
        }
    }

    val allLocations = repository.getAllLocations().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    fun clearSaveState() { _uiState.update { it.copy(isSaved = false, isDeleted = false) } }
}

data class DeviceEditUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null,
    val isFormValid: Boolean = false,
    val validationErrors: List<String> = emptyList(),
    val typeError: String? = null,
    val nameError: String? = null,
    val inventoryNumberError: String? = null,
    val locationError: String? = null,
    val statusError: String? = null,
    val showValidationErrorCard: Boolean = false
)