package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.kipia.management.mobile.viewmodel.ReportsViewModel
import com.kipia.management.mobile.viewmodel.ReportStatistics

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    // Состояния фильтрации
    private val _selectedLocation = MutableStateFlow<String?>(null)
    private val _selectedStatus = MutableStateFlow<String?>(null)
    private val _dateRange = MutableStateFlow<Pair<Long, Long>?>(null)

    // Данные
    private val _allDevices = MutableStateFlow<List<Device>>(emptyList())
    private val _filteredDevices = MutableStateFlow<List<Device>>(emptyList())
    val filteredDevices: StateFlow<List<Device>> = _filteredDevices.asStateFlow()

    // Статистика
    private val _statistics = MutableStateFlow<ReportStatistics>(ReportStatistics())
    val statistics: StateFlow<ReportStatistics> = _statistics.asStateFlow()

    // Доступные фильтры
    val availableLocations = deviceRepository.getAllLocations()
    val availableStatuses = listOf("В работе", "В ремонте", "В резерве", "Списан")

    init {
        loadDevices()
        setupFiltering()
    }

    private fun loadDevices() {
        viewModelScope.launch {
            deviceRepository.getAllDevices().collect { devices ->
                _allDevices.value = devices
            }
        }
    }

    private fun setupFiltering() {
        viewModelScope.launch {
            combine(
                _allDevices,
                _selectedLocation,
                _selectedStatus,
                _dateRange
            ) { devices, location, status, dateRange ->
                devices.filter { device ->
                    (location == null || device.location == location) &&
                            (status == null || device.status == status) &&
                            // TODO: Добавить фильтрацию по дате если будет поле createdDate
                            true
                }
            }.collect { filtered ->
                _filteredDevices.value = filtered
                calculateStatistics(filtered)
            }
        }
    }

    fun setLocationFilter(location: String?) {
        _selectedLocation.value = location
    }

    fun setStatusFilter(status: String?) {
        _selectedStatus.value = status
    }

    fun clearFilters() {
        _selectedLocation.value = null
        _selectedStatus.value = null
        _dateRange.value = null
    }

    private fun calculateStatistics(devices: List<Device>) {
        val total = devices.size
        val byStatus = devices.groupBy { it.status }.mapValues { it.value.size }
        val byLocation = devices.groupBy { it.location }.mapValues { it.value.size }

        _statistics.value = ReportStatistics(
            totalDevices = total,
            devicesByStatus = byStatus,
            devicesByLocation = byLocation,
            averageYear = devices.mapNotNull { it.year }.average().takeIf { !it.isNaN() }
        )
    }
}

data class ReportStatistics(
    val totalDevices: Int = 0,
    val devicesByStatus: Map<String, Int> = emptyMap(),
    val devicesByLocation: Map<String, Int> = emptyMap(),
    val averageYear: Double? = null
)