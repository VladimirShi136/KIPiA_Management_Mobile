package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.managers.PhotoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotoDetailViewModel @Inject constructor(
    private val photoManager: PhotoManager,
    private val repository: DeviceRepository,

) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoDetailUiState())
    val uiState: StateFlow<PhotoDetailUiState> = _uiState

    private var currentDevice: Device? = null

    var onPhotoDeleted: (() -> Unit)? = null // Callback для обновления

    fun setCurrentDevice(device: Device) {
        currentDevice = device
    }

    fun rotatePhoto(photoPath: String, degrees: Float) {
        val device = currentDevice ?: run {
            _uiState.value = _uiState.value.copy(error = "Устройство не задано")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val rotatedPath = photoManager.rotatePhoto(photoPath, degrees)
                if (rotatedPath != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentPhotoPath = rotatedPath,
                        rotationDegrees = (_uiState.value.rotationDegrees + degrees) % 360
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Не удалось повернуть фото"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка: ${e.message}"
                )
            }
        }
    }

    suspend fun deletePhoto(fileName: String): Boolean {
        val device = currentDevice ?: return false // Защита от null

        return try {
            val success = photoManager.deleteDevicePhoto(device, fileName)
            if (success) {
                onPhotoDeleted?.invoke() // Сообщаем, что фото удалено
            }
            success
        } catch (_: Exception) {
            false
        }
    }
}

data class PhotoDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPhotoPath: String? = null,
    val rotationDegrees: Float = 0f,
    val isDeleted: Boolean = false
)