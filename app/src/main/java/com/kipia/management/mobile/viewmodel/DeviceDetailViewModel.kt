package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceDetailViewModel @Inject constructor(
    private val repository: DeviceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val deviceId = savedStateHandle.get<Int>("deviceId") ?: 0

    private val _device = MutableStateFlow<Device?>(null)
    val device: StateFlow<Device?> = _device.asStateFlow()

    private val _photos = MutableStateFlow<List<String>>(emptyList())
    val photos: StateFlow<List<String>> = _photos.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val gson = Gson()

    init {
        loadDevice()
    }

    private fun loadDevice() {
        viewModelScope.launch {
            _isLoading.value = true
            _device.value = repository.getDeviceById(deviceId)
            _device.value?.let { device ->
                // Парсим JSON с фото
                val photoList = parsePhotoJson(device.photos)
                val allPhotos = mutableListOf<String>()
                device.photoPath?.let { allPhotos.add(it) }
                allPhotos.addAll(photoList)
                _photos.value = allPhotos
            }
            _isLoading.value = false
        }
    }

    fun updateDevicePhoto(photoPath: String) {
        viewModelScope.launch {
            val currentDevice = _device.value ?: return@launch

            // Обновляем основное фото
            val updatedDevice = currentDevice.copy(photoPath = photoPath)
            repository.updateDevice(updatedDevice)
            _device.value = updatedDevice

            // Обновляем список фото
            val updatedPhotos = mutableListOf(photoPath)
            updatedPhotos.addAll(parsePhotoJson(updatedDevice.photos))
            _photos.value = updatedPhotos
        }
    }

    fun addAdditionalPhoto(photoPath: String) {
        viewModelScope.launch {
            val currentDevice = _device.value ?: return@launch

            // Парсим существующие фото
            val existingPhotos = parsePhotoJson(currentDevice.photos)
            val updatedPhotos = mutableListOf<String>()
            updatedPhotos.addAll(existingPhotos)
            updatedPhotos.add(photoPath)

            // Конвертируем обратно в JSON
            val photosJson = gson.toJson(updatedPhotos)

            // Обновляем устройство
            val updatedDevice = currentDevice.copy(photos = photosJson)
            repository.updateDevice(updatedDevice)
            _device.value = updatedDevice

            // Обновляем список фото
            val allPhotos = mutableListOf<String>()
            updatedDevice.photoPath?.let { allPhotos.add(it) }
            allPhotos.addAll(updatedPhotos)
            _photos.value = allPhotos
        }
    }

    fun deletePhoto(photoPath: String) {
        viewModelScope.launch {
            val currentDevice = _device.value ?: return@launch

            if (currentDevice.photoPath == photoPath) {
                // Удаляем основное фото
                val updatedDevice = currentDevice.copy(photoPath = null)
                repository.updateDevice(updatedDevice)
                _device.value = updatedDevice
            } else {
                // Удаляем из дополнительных фото
                val existingPhotos = parsePhotoJson(currentDevice.photos)
                val updatedPhotos = existingPhotos.filter { it != photoPath }
                val photosJson = gson.toJson(updatedPhotos)

                val updatedDevice = currentDevice.copy(photos = photosJson)
                repository.updateDevice(updatedDevice)
                _device.value = updatedDevice
            }

            // Обновляем список фото
            val allPhotos = _photos.value.filter { it != photoPath }
            _photos.value = allPhotos
        }
    }

    private fun parsePhotoJson(photosJson: String?): List<String> {
        return if (photosJson.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                val listType = object : TypeToken<List<String>>() {}.type
                gson.fromJson<List<String>>(photosJson, listType) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}