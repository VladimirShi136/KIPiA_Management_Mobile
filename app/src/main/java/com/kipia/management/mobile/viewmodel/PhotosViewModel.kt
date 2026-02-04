package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.ui.components.photos.DisplayMode
import com.kipia.management.mobile.ui.components.photos.PhotoItem
import com.kipia.management.mobile.ui.screens.photos.ViewMode
import com.kipia.management.mobile.utils.PhotoManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PhotosViewModel @Inject constructor(
    repository: DeviceRepository,
    private val photoManager: PhotoManager
) : ViewModel() {
    private val _forceRefresh = MutableStateFlow(0)
    private val _selectedDeviceId = MutableStateFlow<Int?>(null)
    private val _selectedLocation = MutableStateFlow<String?>(null)
    private val _viewMode = MutableStateFlow(ViewMode.GRID)
    private val _uiState = MutableStateFlow(PhotosUiState())
    val uiState: StateFlow<PhotosUiState> = _uiState

    // Все устройства
    val devices = combine(
        repository.getAllDevices(),
        _forceRefresh
    ) { devices, _ ->
        devices
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Все уникальные местоположения из устройств
    val allLocations = devices.map { deviceList ->
        deviceList.map { it.location }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // Все фото с фильтрацией по устройству и местоположению
    val photos = combine(
        devices,
        _selectedLocation,
        _selectedDeviceId
    ) { deviceList, locationFilter, deviceFilter ->

        deviceList
            .filter { device ->
                // Фильтр по местоположению
                (locationFilter == null || device.location == locationFilter) &&
                        // Фильтр по устройству
                        (deviceFilter == null || device.id == deviceFilter)
            }
            .flatMap { device ->
                device.photos.mapNotNull { fileName ->
                    val fullPath = photoManager.getFullPhotoPath(device, fileName)
                    if (fullPath != null && File(fullPath).exists()) {
                        PhotoItem(
                            fileName = fileName,
                            fullPath = fullPath,
                            device = device
                        )
                    } else {
                        null
                    }
                }
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    //  Функция для сброса всех фильтров
    fun resetAllFilters() {
        Timber.d("═══════════════════════════════════════")
        Timber.d("СБРОС ВСЕХ ФИЛЬТРОВ")
        Timber.d("  Было - Локация: ${_selectedLocation.value}, Устройство: ${_selectedDeviceId.value}")

        _selectedLocation.value = null
        _selectedDeviceId.value = null

        _uiState.value = _uiState.value.copy(
            selectedLocation = null,
            selectedDeviceId = null
        )

        Timber.d("  Стало - Локация: null, Устройство: null")
        Timber.d("═══════════════════════════════════════")
    }

    fun forceLoadData() {
        _forceRefresh.value++
    }

    // ★ ДОБАВЛЯЕМ: Flow для сгруппированных данных
    private val _groupedByLocation = MutableStateFlow<List<LocationPhotoGroup>>(emptyList())
    val groupedByLocation: StateFlow<List<LocationPhotoGroup>> = _groupedByLocation

    // ★ ДОБАВЛЯЕМ: Состояние раскрытия/сворачивания групп
    private val _expandedGroups = MutableStateFlow<Set<String>>(emptySet())
    val expandedGroups: StateFlow<Set<String>> = _expandedGroups


    init {
        Timber.d("══════════════════════════════════════════")
        Timber.d("${this::class.simpleName} СОЗДАН")
        Timber.d("  HashCode: ${System.identityHashCode(this)}")
        Timber.d("  Thread: ${Thread.currentThread().name}")

        // Логирование загрузки данных
        viewModelScope.launch {
            devices.collect {
                Timber.d("${this::class.simpleName}: devices loaded - ${it.size}")
            }
        }

        viewModelScope.launch {
            allLocations.collect {
                Timber.d("${this::class.simpleName}: locations loaded - ${it.size}")
            }
        }

        loadPhotos()

        // ★ ОБНОВЛЯЕМ: группировка должна обновляться при изменении фильтров ИЛИ устройств
        viewModelScope.launch {
            // Объединяем потоки: устройства и фильтры
            combine(
                devices,
                _selectedLocation,
                _selectedDeviceId
            ) { deviceList, locationFilter, deviceFilter ->
                Triple(deviceList, locationFilter, deviceFilter)
            }.collect { (deviceList, locationFilter, deviceFilter) ->
                Timber.d("═══════════════════════════════════════")
                Timber.d("ТРИГГЕР ОБНОВЛЕНИЯ ГРУППИРОВКИ:")
                Timber.d("  Изменились фильтры: $locationFilter / $deviceFilter")
                Timber.d("  Устройств: ${deviceList.size}")
                Timber.d("═══════════════════════════════════════")

                updateGroupedPhotos(deviceList)
            }
        }
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
                    selectedLocation = _selectedLocation.value, // ★ ДОБАВЛЕНО
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

    // ★ ИСПРАВЛЕННЫЙ: Метод для обновления сгруппированных данных с фильтрацией
    private fun updateGroupedPhotos(deviceList: List<Device>) {
        viewModelScope.launch {
            // ★ ВАЖНО: берем текущие значения фильтров
            val locationFilter = _selectedLocation.value
            val deviceFilter = _selectedDeviceId.value

            Timber.d("═══════════════════════════════════════")
            Timber.d("ОБНОВЛЕНИЕ ГРУППИРОВКИ:")
            Timber.d("  Всего устройств: ${deviceList.size}")
            Timber.d("  Фильтр локации: $locationFilter")
            Timber.d("  Фильтр устройства: $deviceFilter")

            // ★ ФИЛЬТРУЕМ устройства перед группировкой
            val filteredDevices = deviceList.filter { device ->
                val matchesLocation = locationFilter == null || device.location == locationFilter
                val matchesDevice = deviceFilter == null || device.id == deviceFilter

                matchesLocation && matchesDevice
            }

            Timber.d("  После фильтрации: ${filteredDevices.size} устройств")

            val groups = mutableMapOf<String, MutableList<PhotoItem>>()

            filteredDevices.forEach { device ->
                val location = device.location.ifEmpty { "Без локации" }

                device.photos.forEach { fileName ->
                    val fullPath = photoManager.getFullPhotoPath(device, fileName)
                    if (fullPath != null && File(fullPath).exists()) {
                        val photoItem = PhotoItem(
                            fileName = fileName,
                            fullPath = fullPath,
                            device = device
                        )

                        groups.getOrPut(location) { mutableListOf() }.add(photoItem)
                    }
                }
            }

            // Преобразуем в отсортированный список групп
            val sortedGroups = groups.map { (location, photos) ->
                LocationPhotoGroup(
                    location = location,
                    photos = photos.sortedByDescending {
                        File(it.fullPath).lastModified()
                    },
                    isExpanded = _expandedGroups.value.contains(location)
                )
            }.sortedBy { it.location }

            Timber.d("  Создано групп: ${sortedGroups.size}")
            sortedGroups.forEach { group ->
                Timber.d("    - ${group.location}: ${group.photos.size} фото")
            }
            Timber.d("═══════════════════════════════════════")

            _groupedByLocation.value = sortedGroups
        }
    }

    // ★ ДОБАВЛЯЕМ: Метод для переключения раскрытия группы
    fun toggleLocationGroup(location: String) {
        val newExpanded = _expandedGroups.value.toMutableSet()
        if (newExpanded.contains(location)) {
            newExpanded.remove(location)
        } else {
            newExpanded.add(location)
        }
        _expandedGroups.value = newExpanded

        // Обновляем группы с новым состоянием
        val updatedGroups = _groupedByLocation.value.map { group ->
            if (group.location == location) {
                group.copy(isExpanded = !group.isExpanded)
            } else {
                group
            }
        }
        _groupedByLocation.value = updatedGroups
    }

    // ★ ДОБАВЛЯЕМ: Метод для раскрытия/сворачивания всех групп
    fun toggleAllGroups(expand: Boolean) {
        if (expand) {
            // Раскрыть все
            val allLocations = _groupedByLocation.value.map { it.location }.toSet()
            _expandedGroups.value = allLocations

            val updatedGroups = _groupedByLocation.value.map { group ->
                group.copy(isExpanded = true)
            }
            _groupedByLocation.value = updatedGroups
        } else {
            // Свернуть все
            _expandedGroups.value = emptySet()

            val updatedGroups = _groupedByLocation.value.map { group ->
                group.copy(isExpanded = false)
            }
            _groupedByLocation.value = updatedGroups
        }
    }

    // ★ ДОБАВЛЕНО: фильтр по местоположению с логами
    fun selectLocation(location: String?) {
        Timber.d("═══════════════════════════════════════")
        Timber.d("ВЫБРАНА ЛОКАЦИЯ: $location")
        Timber.d("  Было: ${_selectedLocation.value}")
        Timber.d("  Стало: $location")
        Timber.d("═══════════════════════════════════════")

        _selectedLocation.value = location
        _uiState.value = _uiState.value.copy(
            selectedLocation = location
        )
    }

    // ★ ИЗМЕНЕНО: переименовано для ясности с логами
    fun selectDevice(deviceId: Int?) {
        Timber.d("═══════════════════════════════════════")
        Timber.d("ВЫБРАНО УСТРОЙСТВО: $deviceId")
        Timber.d("  Было: ${_selectedDeviceId.value}")
        Timber.d("  Стало: $deviceId")
        Timber.d("═══════════════════════════════════════")

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

    fun updateDisplayMode(mode: DisplayMode) {
        _uiState.value = _uiState.value.copy(displayMode = mode)
    }

}

// ★ ДОБАВЛЯЕМ: Data классы для группировки
data class LocationPhotoGroup(
    val location: String,
    val photos: List<PhotoItem>,
    val isExpanded: Boolean = false
)

// ★ ОБНОВЛЯЕМ: PhotosUiState - добавляем режим отображения
data class PhotosUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedDeviceId: Int? = null,
    val selectedLocation: String? = null,
    val viewMode: ViewMode = ViewMode.GRID,
    val isGridView: Boolean = true,
    val displayMode: DisplayMode = DisplayMode.GROUPED // ★ режим отображения
)
