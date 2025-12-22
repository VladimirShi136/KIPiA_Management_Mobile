package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.asLiveData
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val repository: DeviceRepository
) : ViewModel() {

    val allDevices = repository.getAllDevices().asLiveData()
    val allLocations = repository.getAllLocations().asLiveData()

    fun addDevice(device: Device) {
        viewModelScope.launch {
            repository.insertDevice(device)
        }
    }

    fun updateDevice(device: Device) {
        viewModelScope.launch {
            repository.updateDevice(device)
        }
    }

    fun deleteDevice(device: Device) {
        viewModelScope.launch {
            repository.deleteDevice(device)
        }
    }

    suspend fun validateInventoryNumber(inventoryNumber: String, excludeId: Int = 0): Boolean {
        val existing = repository.getDeviceByInventoryNumber(inventoryNumber)
        return existing == null || existing.id == excludeId
    }
}