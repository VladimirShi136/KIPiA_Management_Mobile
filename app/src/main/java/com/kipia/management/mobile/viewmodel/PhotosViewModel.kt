package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.ui.screens.photos.ViewMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PhotosViewModel @Inject constructor(
    private val repository: DeviceRepository
) : ViewModel() {

    private val _selectedDeviceId = MutableStateFlow<Int?>(null)
    private val _viewMode = MutableStateFlow(ViewMode.GRID)

    private val _uiState = MutableStateFlow(PhotosUiState())
    val uiState: StateFlow<PhotosUiState> = _uiState

    // Все устройства для фильтра
    val devices = repository.getAllDevices()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Все фото со всех устройств
    val photos = devices.map { deviceList ->
        deviceList.flatMap { device ->
            device.getPhotoList().map { photoPath ->
                Pair(photoPath, device)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadPhotos()
    }

    fun loadPhotos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            try {
                // Загрузка происходит автоматически через Flow
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedDeviceId = _selectedDeviceId.value,
                    viewMode = _viewMode.value,
                    isGridView = _viewMode.value == ViewMode.GRID
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки фото: ${e.message}"
                )
            }
        }
    }

    fun selectDevice(deviceId: Int?) {
        _selectedDeviceId.value = deviceId
        _uiState.value = _uiState.value.copy(
            selectedDeviceId = deviceId
        )
    }

    fun toggleViewMode() {
        val newMode = if (_viewMode.value == ViewMode.GRID) {
            ViewMode.LIST
        } else {
            ViewMode.GRID
        }

        _viewMode.value = newMode
        _uiState.value = _uiState.value.copy(
            viewMode = newMode,
            isGridView = newMode == ViewMode.GRID
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun deletePhoto(photoPath: String) {
        viewModelScope.launch {
            try {
                // TODO: Реализовать удаление фото из устройства
                // Пока просто обновляем список
                loadPhotos()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Ошибка удаления: ${e.message}"
                )
            }
        }
    }
}

data class PhotosUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedDeviceId: Int? = null,
    val viewMode: ViewMode = ViewMode.GRID,
    val isGridView: Boolean = true
)