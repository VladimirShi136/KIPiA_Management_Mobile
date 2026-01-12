package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val repository: DeviceRepository
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    private val _typeFilter = MutableStateFlow<String?>(null)
    private val _statusFilter = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState = combine(
        _isLoading,
        _error,
        _typeFilter,
        _statusFilter
    ) { isLoading, error, typeFilter, statusFilter ->
        DevicesUiState(
            isLoading = isLoading,
            error = error,
            typeFilter = typeFilter,
            statusFilter = statusFilter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DevicesUiState()
    )

    // Фильтрация устройств
    val devices = combine(
        repository.getAllDevices(),
        _searchQuery,
        _typeFilter,
        _statusFilter
    ) { devices, query, typeFilter, statusFilter ->
        devices.filter { device ->
            val matchesQuery = query.isBlank() ||
                    device.inventoryNumber.contains(query, ignoreCase = true) ||
                    device.name?.contains(query, ignoreCase = true) == true ||
                    device.manufacturer?.contains(query, ignoreCase = true) == true ||
                    device.location.contains(query, ignoreCase = true) ||
                    device.valveNumber?.contains(query, ignoreCase = true) == true ||
                    device.type.contains(query, ignoreCase = true) ||
                    device.measurementLimit?.contains(query, ignoreCase = true) == true ||
                    device.status.contains(query, ignoreCase = true) // Добавляем поиск по статусу

            val matchesType = typeFilter == null || device.type == typeFilter
            val matchesStatus = statusFilter == null || device.status == statusFilter

            matchesQuery && matchesType && matchesStatus
        }.sortedBy { it.inventoryNumber }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(filter: String?) {
        _typeFilter.value = filter
    }

    fun setStatusFilter(filter: String?) {
        _statusFilter.value = filter
    }

    fun deleteDevice(device: Device) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.deleteDevice(device)
            } catch (e: Exception) {
                _error.value = "Ошибка удаления: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

data class DevicesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val typeFilter: String? = null,
    val statusFilter: String? = null
)