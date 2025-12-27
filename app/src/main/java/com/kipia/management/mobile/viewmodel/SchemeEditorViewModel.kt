package com.kipia.management.mobile.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.DeviceLocation
import com.kipia.management.mobile.repository.DeviceLocationRepository
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.repository.SchemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SchemeEditorViewModel @Inject constructor(
    private val schemeRepository: SchemeRepository,
    private val deviceRepository: DeviceRepository,
    private val deviceLocationRepository: DeviceLocationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val schemeId = savedStateHandle.get<Int>("schemeId") ?: 0

    private val _scheme = MutableStateFlow<com.kipia.management.mobile.data.entities.Scheme?>(null)
    val scheme: StateFlow<com.kipia.management.mobile.data.entities.Scheme?> = _scheme.asStateFlow()

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val _deviceLocations = MutableStateFlow<List<DeviceLocation>>(emptyList())
    val deviceLocations: StateFlow<List<DeviceLocation>> = _deviceLocations.asStateFlow()

    private val _backgroundImage = MutableStateFlow<Bitmap?>(null)
    val backgroundImage: StateFlow<Bitmap?> = _backgroundImage.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true

            // Загружаем схему
            _scheme.value = schemeRepository.getSchemeById(schemeId)

            // Загружаем все приборы для выбора
            deviceRepository.getAllDevices().collect { deviceList ->
                _devices.value = deviceList
            }

            // Загружаем расположения приборов на этой схеме
            val locations = deviceLocationRepository.getLocationsForScheme(schemeId)
            _deviceLocations.value = locations

            // TODO: Загрузить фоновое изображение схемы (пока заглушка)
            // _backgroundImage.value = loadSchemeImage(scheme.value?.imagePath)

            _isLoading.value = false
        }
    }

    fun saveDeviceLocation(deviceId: Int, x: Float, y: Float, rotation: Float = 0f) {
        viewModelScope.launch {
            val location = DeviceLocation(
                deviceId = deviceId,
                schemeId = schemeId,
                x = x,
                y = y,
                rotation = rotation
            )
            deviceLocationRepository.saveLocation(location)

            // Обновляем локальный список
            val updatedLocations = _deviceLocations.value.toMutableList()
            val existingIndex = updatedLocations.indexOfFirst {
                it.deviceId == deviceId && it.schemeId == schemeId
            }

            if (existingIndex >= 0) {
                updatedLocations[existingIndex] = location
            } else {
                updatedLocations.add(location)
            }

            _deviceLocations.value = updatedLocations
        }
    }

    fun deleteDeviceLocation(deviceId: Int) {
        viewModelScope.launch {
            val location = _deviceLocations.value.firstOrNull {
                it.deviceId == deviceId && it.schemeId == schemeId
            }

            location?.let { it ->
                deviceLocationRepository.deleteLocation(it)

                // Обновляем локальный список
                val updatedLocations = _deviceLocations.value.filterNot {
                    it.deviceId == deviceId && it.schemeId == schemeId
                }
                _deviceLocations.value = updatedLocations
            }
        }
    }

    fun getDeviceById(deviceId: Int): Device? {
        return _devices.value.firstOrNull { it.id == deviceId }
    }

    fun getLocationForDevice(deviceId: Int): DeviceLocation? {
        return _deviceLocations.value.firstOrNull {
            it.deviceId == deviceId && it.schemeId == schemeId
        }
    }

    // Вспомогательный метод для загрузки изображения схемы
    private fun loadSchemeImage(imagePath: String?): Bitmap? {
        return if (!imagePath.isNullOrEmpty() && File(imagePath).exists()) {
            BitmapFactory.decodeFile(imagePath)
        } else {
            null
        }
    }
}