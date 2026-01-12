package com.kipia.management.mobile.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceDetailViewModel @Inject constructor(
    private val repository: DeviceRepository
) : ViewModel() {

    private val _device = MutableStateFlow<Device?>(null)
    val device: StateFlow<Device?> = _device

    // Flow для получения списка фото
    val photos = _device.map { device ->
        device?.getPhotoList() ?: emptyList()
    }

    private val _uiState = MutableStateFlow(DeviceDetailUiState())
    val uiState: StateFlow<DeviceDetailUiState> = _uiState

    fun loadDevice(deviceId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                repository.getDeviceById(deviceId).collect { loadedDevice ->
                    _device.value = loadedDevice
                    if (loadedDevice == null) {
                        _uiState.value = _uiState.value.copy(
                            error = "Прибор не найден",
                            isLoading = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isFavorite = loadFavoriteStatus(deviceId)
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

    fun toggleFavorite() {
        val currentDevice = _device.value ?: return
        val newFavoriteStatus = !_uiState.value.isFavorite

        viewModelScope.launch {
            saveFavoriteStatus(currentDevice.id, newFavoriteStatus)
            _uiState.value = _uiState.value.copy(isFavorite = newFavoriteStatus)
        }
    }

    fun shareDeviceInfo() {
        // TODO: Реализовать обмен информацией о приборе
        // Можно использовать Intent для отправки через другие приложения
        val device = _device.value ?: return

        val shareText = buildString {
            appendLine("📊 Информация о приборе")
            appendLine("📌 Тип: ${device.type}")
            appendLine("🏷️ Название: ${device.name ?: "Не указано"}")
            appendLine("🔢 Инв. номер: ${device.inventoryNumber}")
            appendLine("📍 Место: ${device.location}")
            appendLine("📊 Статус: ${device.status}")
            device.manufacturer?.let { appendLine("🏭 Производитель: $it") }
            device.year?.let { appendLine("📅 Год выпуска: $it") }
            device.measurementLimit?.let { appendLine("📏 Предел измерений: $it") }
            device.accuracyClass?.let { appendLine("🎯 Класс точности: $it") }
        }

        // Здесь будет логика отправки через Intent
        println("Текст для отправки: $shareText")
    }

    private fun loadFavoriteStatus(deviceId: Int): Boolean {
        // TODO: Загрузить статус из SharedPreferences или БД
        // Временная реализация:
        return false
    }

    private fun saveFavoriteStatus(deviceId: Int, isFavorite: Boolean) {
        // TODO: Сохранить статус в SharedPreferences или БД
        // Временная реализация:
        println("Сохранение статуса избранного: deviceId=$deviceId, isFavorite=$isFavorite")
    }
}

data class DeviceDetailUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFavorite: Boolean = false
)