package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.domain.usecase.SchemeSyncUseCase
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.ui.screens.devices.SortColumn
import com.kipia.management.mobile.managers.PhotoManager
import com.kipia.management.mobile.ui.shared.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val repository: DeviceRepository,
    private val notificationManager: NotificationManager,
    private val schemeSyncUseCase: SchemeSyncUseCase,
    private val photoManager: PhotoManager,
) : ViewModel() {

    // ── Фильтры и сортировка ──────────────────────────────────────────────────

    private val _searchQuery    = MutableStateFlow("")
    private val _locationFilter = MutableStateFlow<String?>(null)
    private val _statusFilter   = MutableStateFlow<String?>(null)
    private val _sortColumn     = MutableStateFlow(SortColumn.INVENTORY_NUMBER)
    private val _sortAscending  = MutableStateFlow(true)

    val searchQuery: StateFlow<String> = _searchQuery

    // ── Источник данных ───────────────────────────────────────────────────────

    private val _rawDevices = repository.getAllDevices()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ── Фильтрация + сортировка ───────────────────────────────────────────────
    // combine поддерживает максимум 5 аргументов — вкладываем два combine

    // Шаг 1: фильтрация
    private val _filteredDevices = combine(
        _rawDevices,
        _searchQuery,
        _locationFilter,
        _statusFilter
    ) { rawDevices, query, locationFilter, statusFilter ->
        rawDevices.filter { device ->
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
            val matchesStatus   = statusFilter == null   || device.status   == statusFilter

            matchesQuery && matchesLocation && matchesStatus
        }
    }

    // Шаг 2: сортировка
    val devices: StateFlow<List<Device>> = combine(
        _filteredDevices,
        _sortColumn,
        _sortAscending
    ) { filtered, sortColumn, sortAscending ->
        val comparator = compareBy<Device> { device ->
            when (sortColumn) {
                SortColumn.TYPE              -> device.type
                SortColumn.NAME              -> device.name ?: ""
                SortColumn.INVENTORY_NUMBER  -> device.inventoryNumber
                SortColumn.MEASUREMENT_LIMIT -> device.measurementLimit ?: ""
                SortColumn.LOCATION          -> device.location
                SortColumn.VALVE_NUMBER      -> device.valveNumber ?: ""
                SortColumn.STATUS            -> device.status
            }
        }.let { if (!sortAscending) it.reversed() else it }

        Timber.d("devices: sorted ${filtered.size} items by $sortColumn asc=$sortAscending")
        filtered.sortedWith(comparator)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ── Статистика — считается из raw (не из отфильтрованного) ───────────────
    // Показывает общую картину по всем приборам, независимо от фильтра

    val stats: StateFlow<DeviceStats> = _rawDevices
        .map { list ->
            DeviceStats(
                total     = list.size,
                inWork    = list.count { it.status == "В работе" },
                inStorage = list.count { it.status == "Хранение" },
                lost      = list.count { it.status == "Утерян" },
                broken    = list.count { it.status == "Испорчен" }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DeviceStats()
        )

    // ── UI-состояние ─────────────────────────────────────────────────────────

    private val _isLoading = MutableStateFlow(false)
    private val _error     = MutableStateFlow<String?>(null)

    // uiState: два combine (max 5 потоков каждый)
    private val _baseUiState = combine(
        _isLoading,
        _error,
        _searchQuery,
        _locationFilter,
        _statusFilter
    ) { isLoading, error, searchQuery, locationFilter, statusFilter ->
        DevicesUiState(
            isLoading      = isLoading,
            error          = error,
            searchQuery    = searchQuery,
            locationFilter = locationFilter,
            statusFilter   = statusFilter
        )
    }

    val uiState: StateFlow<DevicesUiState> = combine(
        _baseUiState,
        _sortColumn,
        _sortAscending
    ) { base, sortColumn, sortAscending ->
        base.copy(sortColumn = sortColumn, sortAscending = sortAscending)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DevicesUiState()
    )

    // ── Локации ───────────────────────────────────────────────────────────────

    val allLocations: StateFlow<List<String>> = repository.getAllLocations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // ── Действия — фильтры ────────────────────────────────────────────────────

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setLocationFilter(filter: String?) {
        _locationFilter.value = filter
    }

    fun setStatusFilter(filter: String?) {
        _statusFilter.value = filter
    }

    // ── Действия — сортировка ─────────────────────────────────────────────────

    fun setSortColumn(column: SortColumn) {
        if (_sortColumn.value == column) {
            // Повторный тап по той же колонке — меняем направление
            _sortAscending.value = !_sortAscending.value
        } else {
            _sortColumn.value    = column
            _sortAscending.value = true
        }
    }

    // ── Удаление ──────────────────────────────────────────────────────────────

    fun deleteDevice(device: Device, deleteScheme: Boolean = false) {
        viewModelScope.launch {
            try {
                // Удаляем физические файлы фото до удаления записи из БД
                val deletedPhotos = photoManager.deleteAllDevicePhotos(device)
                Timber.d("deleteDevice: удалено $deletedPhotos фото для ${device.getDisplayName()}")

                repository.deleteDevice(device)

                val schemeWasDeleted = if (deleteScheme) {
                    schemeSyncUseCase.deleteSchemeIfEmpty(device.location)
                } else {
                    false
                }

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

// ── Data-классы состояния ─────────────────────────────────────────────────────

data class DevicesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val locationFilter: String? = null,
    val statusFilter: String? = null,
    val sortColumn: SortColumn = SortColumn.INVENTORY_NUMBER,
    val sortAscending: Boolean = true
)

data class DeviceStats(
    val total: Int = 0,
    val inWork: Int = 0,
    val inStorage: Int = 0,
    val lost: Int = 0,
    val broken: Int = 0
)