package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.domain.usecase.SchemeSyncUseCase
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.ui.shared.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val repository: DeviceRepository,
    private val notificationManager: NotificationManager,
    private val schemeSyncUseCase: SchemeSyncUseCase,
) : ViewModel() {

    // === Фильтры ===
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _locationFilter = MutableStateFlow<String?>(null)
    private val _statusFilter = MutableStateFlow<String?>(null)

    // === Состояние ===
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    // === ИСТОЧНИК ДАННЫХ: Используем Flow напрямую из репозитория ===
    private val _rawDevices = repository.getAllDevices()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // ← КРИТИЧНО ВАЖНО!
            initialValue = emptyList()
        )

    val devices = combine(
        _rawDevices, // ← теперь это Flow, который реагирует на изменения БД
        _searchQuery,
        _locationFilter,
        _statusFilter
    ) { devices, query, locationFilter, statusFilter ->
        Timber.d("╔═══════════════════════════════════════╗")
        Timber.d("║ FILTERING START                       ║")
        Timber.d("╠═══════════════════════════════════════╣")
        Timber.d("║ Всего устройств: ${devices.size}")
        Timber.d("║ Поиск: '$query'")
        Timber.d("║ Место: $locationFilter")
        Timber.d("║ Статус: $statusFilter")
        Timber.d("╠═══════════════════════════════════════╣")

        val filtered = devices.filter { device ->
            val matchesQuery = query.isBlank() ||
                    device.inventoryNumber.contains(query, ignoreCase = true) ||
                    device.name?.contains(query, ignoreCase = true) == true ||
                    device.manufacturer?.contains(query, ignoreCase = true) == true ||
                    device.location.contains(query, ignoreCase = true) ||
                    device.valveNumber?.contains(query, ignoreCase = true) == true ||
                    device.type.contains(query, ignoreCase = true) ||
                    device.measurementLimit?.contains(query, ignoreCase = true) == true ||
                    device.status.contains(query, ignoreCase = true)

            val matchesLocation = locationFilter == null || device.location == locationFilter
            val matchesStatus = statusFilter == null || device.status == statusFilter

            val result = matchesQuery && matchesLocation && matchesStatus
            if (!result) {
                Timber.d("║ ❌ Отфильтровано: ${device.inventoryNumber}")
                Timber.d("║   matchesQuery=$matchesQuery")
                Timber.d("║   matchesLocation=$matchesLocation")
                Timber.d("║   matchesStatus=$matchesStatus")
            }
            result
        }.sortedBy { it.inventoryNumber }

        Timber.d("╠═══════════════════════════════════════╣")
        Timber.d("║ Результат фильтрации: ${filtered.size} устройств")
        filtered.forEachIndexed { index, device ->
            Timber.d("║ ${index + 1}. ${device.inventoryNumber} - ${device.type}")
        }
        Timber.d("╚═══════════════════════════════════════╝")

        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000), // ← Тоже важно
        initialValue = emptyList()
    )

    // === UI-состояние ===
    val uiState = combine(
        _isLoading,
        _error,
        _searchQuery,
        _locationFilter,
        _statusFilter
    ) { isLoading, error, searchQuery, locationFilter, statusFilter ->
        DevicesUiState(
            isLoading = isLoading,
            error = error,
            searchQuery = searchQuery,
            locationFilter = locationFilter,
            statusFilter = statusFilter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DevicesUiState()
    )

    // === Локация ===
    val allLocations = repository.getAllLocations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // === Действия ===
    fun setSearchQuery(query: String) {
        Timber.d("setSearchQuery: '$query'")
        _searchQuery.value = query
    }

    fun setLocationFilter(filter: String?) {
        Timber.d("setLocationFilter: $filter (было: ${_locationFilter.value}, станет: $filter)")
        _locationFilter.value = filter
        Timber.d("setLocationFilter: установлено, теперь = ${_locationFilter.value}")
    }

    fun setStatusFilter(filter: String?) {
        Timber.d("setStatusFilter: $filter (было: ${_statusFilter.value}, станет: $filter)")
        _statusFilter.value = filter
        Timber.d("setStatusFilter: установлено, теперь = ${_statusFilter.value}")
    }

    fun deleteDevice(device: Device, deleteScheme: Boolean = false) {
        viewModelScope.launch {
            try {
                // ★★★★ ИСПРАВЛЕНИЕ: Сначала удаляем устройство ★★★★
                repository.deleteDevice(device)

                // ★★★★ Затем проверяем схему (если нужно) ★★★★
                val schemeWasDeleted = if (deleteScheme) {
                    schemeSyncUseCase.deleteSchemeIfEmpty(device.location)
                } else {
                    false
                }

                // ★★★★ Отправляем уведомление с правильным флагом ★★★★
                notificationManager.notifyDeviceDeleted(
                    deviceName = device.getDisplayName(),
                    withScheme = schemeWasDeleted
                )

            } catch (e: Exception) {
                notificationManager.notifyError(e.message ?: "Ошибка удаления")
            }
        }
    }
}

data class DevicesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val locationFilter: String? = null,
    val statusFilter: String? = null
)