package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.utils.PhotoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotoDetailViewModel @Inject constructor(
    private val photoManager: PhotoManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoDetailUiState())
    val uiState: StateFlow<PhotoDetailUiState> = _uiState

    fun rotatePhoto(photoPath: String, degrees: Float) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val rotatedPath = photoManager.rotatePhoto(
                    android.content.ContextWrapper(), // TODO: Получить контекст
                    photoPath,
                    degrees
                )

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

    fun deletePhoto(photoPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val success = photoManager.deletePhoto(
                    android.content.ContextWrapper(), // TODO: Получить контекст
                    photoPath
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isDeleted = success,
                    error = if (!success) "Не удалось удалить фото" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetDeletion() {
        _uiState.value = _uiState.value.copy(isDeleted = false)
    }
}

data class PhotoDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPhotoPath: String? = null,
    val rotationDegrees: Float = 0f,
    val isDeleted: Boolean = false
)