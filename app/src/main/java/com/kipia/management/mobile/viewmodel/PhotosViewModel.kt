package com.kipia.management.mobile.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.ui.components.photos.DisplayMode
import com.kipia.management.mobile.ui.components.photos.PhotoItem
import com.kipia.management.mobile.ui.screens.photos.ViewMode
import com.kipia.management.mobile.managers.PhotoManager
import com.kipia.management.mobile.ui.components.photos.PhotosSortBy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
    private val _searchQuery = MutableStateFlow("")
    private val _sortBy = MutableStateFlow(PhotosSortBy.NAME_ASC)
    private val _displayMode = MutableStateFlow(DisplayMode.FLAT)
    private val _expandedGroups = MutableStateFlow<Set<String>>(emptySet())
    
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

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

    // Поток для определения наличия фото вообще
    val hasPhotos: StateFlow<Boolean> = devices.map { list ->
        list.any { it.photos.isNotEmpty() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val uiState: StateFlow<PhotosUiState> = combine(
        _selectedDeviceId,
        _selectedLocation,
        _viewMode,
        _displayMode,
        _searchQuery,
        _sortBy,
        _isLoading,
        _error
    ) { args ->
        PhotosUiState(
            selectedDeviceId = args[0] as? Int,
            selectedLocation = args[1] as? String,
            viewMode = args[2] as ViewMode,
            isGridView = (args[2] as ViewMode) == ViewMode.GRID,
            displayMode = args[3] as DisplayMode,
            searchQuery = args[4] as String,
            sortBy = args[5] as PhotosSortBy,
            isLoading = args[6] as Boolean,
            error = args[7] as? String
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PhotosUiState(isLoading = true)
    )

    val photos = combine(
        devices,
        _selectedLocation,
        _selectedDeviceId,
        _searchQuery,
        _sortBy
    ) { args ->
        val deviceList = args[0] as List<Device>
        val locationFilter = args[1] as? String
        val deviceFilter = args[2] as? Int
        val searchQuery = args[3] as String
        val sortBy = args[4] as PhotosSortBy

        deviceList
            .filter { device ->
                val matchesLocation = locationFilter == null || device.location == locationFilter
                val matchesDevice = deviceFilter == null || device.id == deviceFilter
                val matchesSearch = searchQuery.isBlank() ||
                        device.location.contains(searchQuery, ignoreCase = true) ||
                        device.name?.contains(searchQuery, ignoreCase = true) == true ||
                        device.inventoryNumber.contains(searchQuery, ignoreCase = true)
                matchesLocation && matchesDevice && matchesSearch
            }
            .flatMap { device ->
                device.photos.mapNotNull { fileName ->
                    val fullPath = photoManager.getFullPhotoPath(device, fileName)
                    if (fullPath != null && File(fullPath).exists()) {
                        PhotoItem(fileName = fileName, fullPath = fullPath, device = device)
                    } else null
                }
            }
            .let { list ->
                when (sortBy) {
                    PhotosSortBy.NAME_ASC -> list.sortedBy { it.device.getDisplayName() }
                    PhotosSortBy.NAME_DESC -> list.sortedByDescending { it.device.getDisplayName() }
                }
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val groupedByLocation = combine(
        photos,
        _expandedGroups
    ) { photoList, expandedSet ->
        photoList.groupBy { it.device.location.ifEmpty { "Без локации" } }
            .map { (location, items) ->
                LocationPhotoGroup(
                    location = location,
                    photos = items.sortedByDescending { File(it.fullPath).lastModified() },
                    isExpanded = expandedSet.contains(location)
                )
            }
            .sortedBy { it.location }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalStats: StateFlow<PhotoStats> = devices.map { list ->
        val locations = list.filter { it.photos.isNotEmpty() }.map { it.location }.distinct().size
        val photoCount = list.sumOf { d -> 
            d.photos.count { f -> 
                photoManager.getFullPhotoPath(d, f)?.let { File(it).exists() } == true 
            } 
        }
        PhotoStats(locations = locations, devices = list.size, photos = photoCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PhotoStats())

    val filteredStats: StateFlow<PhotoStats> = photos.map { list ->
        val devicesWithPhotos = list.map { it.device.id }.distinct().size
        PhotoStats(
            locations = list.map { it.device.location }.distinct().size,
            devices = list.map { it.device.id }.distinct().size,
            photos = list.size,
            devicesWithPhotos = devicesWithPhotos
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PhotoStats())

    val allLocations = devices.map { list ->
        list.map { it.location }.filter { it.isNotBlank() }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        refreshData()
        viewModelScope.launch {
            allLocations.collect { locations ->
                if (locations.isNotEmpty() && _expandedGroups.value.isEmpty()) {
                    _expandedGroups.value = locations.toSet()
                }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            delay(600)
            _isLoading.value = false
        }
    }

    fun resetLoadingState() {
        _isLoading.value = true
    }

    fun resetAllFilters() {
        _searchQuery.value = ""
        _sortBy.value = PhotosSortBy.NAME_ASC
        _selectedLocation.value = null
        _selectedDeviceId.value = null
    }

    fun forceLoadData() {
        _forceRefresh.update { it + 1 }
    }

    fun loadPhotos() {
        refreshData()
        forceLoadData()
    }

    fun toggleLocationGroup(location: String) {
        _expandedGroups.update { current ->
            if (current.contains(location)) current - location else current + location
        }
    }

    fun toggleAllGroups(expand: Boolean) {
        _expandedGroups.value = if (expand) allLocations.value.toSet() else emptySet()
    }

    fun selectLocation(location: String?) {
        _selectedLocation.value = location
    }

    fun selectDevice(deviceId: Int?) {
        _selectedDeviceId.value = deviceId
    }

    fun toggleViewMode() {
        _viewMode.update { if (it == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID }
    }

    fun updateDisplayMode(mode: DisplayMode) {
        _displayMode.value = mode
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortBy(sortBy: PhotosSortBy) {
        _sortBy.value = sortBy
    }
}

data class LocationPhotoGroup(
    val location: String,
    val photos: List<PhotoItem>,
    val isExpanded: Boolean = false
)

data class PhotosUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedDeviceId: Int? = null,
    val selectedLocation: String? = null,
    val viewMode: ViewMode = ViewMode.GRID,
    val isGridView: Boolean = true,
    val displayMode: DisplayMode = DisplayMode.GROUPED,
    val searchQuery: String = "",
    val sortBy: PhotosSortBy = PhotosSortBy.NAME_ASC
)

@Immutable
data class PhotoStats(
    val locations: Int = 0,
    val devices: Int = 0,
    val photos: Int = 0,
    val devicesWithPhotos: Int = 0
)
