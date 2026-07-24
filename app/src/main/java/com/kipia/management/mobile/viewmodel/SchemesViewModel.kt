package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.repository.SchemeRepository
import com.kipia.management.mobile.ui.screens.schemes.SchemesSortBy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SchemesViewModel @Inject constructor(
    private val repository: SchemeRepository,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortBy = MutableStateFlow(SchemesSortBy.NAME_ASC)
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)

    val uiState = combine(
        _searchQuery,
        _sortBy,
        _error
    ) { searchQuery, sortBy, error ->
        SchemesUiState(
            searchQuery = searchQuery,
            sortBy = sortBy,
            isLoading = false,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SchemesUiState()
    )

    // Поток для определения наличия данных вообще
    val hasSchemes = repository.getAllSchemes()
        .map { it.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val schemes = repository.getAllSchemes()
        .combine(_searchQuery) { schemes, query ->
            if (query.isBlank()) {
                schemes
            } else {
                schemes.filter { scheme ->
                    scheme.name.contains(query, ignoreCase = true) ||
                            scheme.description?.contains(query, ignoreCase = true) == true
                }
            }
        }
        .combine(_sortBy) { filteredSchemes, sortBy ->
            when (sortBy) {
                SchemesSortBy.NAME_ASC -> filteredSchemes.sortedBy { it.name }
                SchemesSortBy.NAME_DESC -> filteredSchemes.sortedByDescending { it.name }
                else -> filteredSchemes
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            val startTime = System.currentTimeMillis()
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 600) delay(600 - elapsed)
            _isLoading.value = false
        }
    }

    fun resetLoadingState() {
        _isLoading.value = true
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortBy(sortBy: SchemesSortBy) {
        _sortBy.value = sortBy
    }

    fun resetAllFilters() {
        _searchQuery.value = ""
        _sortBy.value = SchemesSortBy.NAME_ASC
    }

    suspend fun deleteScheme(scheme: Scheme): DeleteResult {
        return try {
            _isLoading.value = true
            val startTime = System.currentTimeMillis()

            val devices = deviceRepository.getAllDevicesSync()
            val deviceCount = devices.count { it.location == scheme.name }

            if (deviceCount > 0) {
                return DeleteResult.Error("Нельзя удалить схему '${scheme.name}'. " +
                        "К ней привязано $deviceCount приборов. " +
                        "Сначала удалите или переместите приборы.")
            }

            repository.deleteScheme(scheme)
            
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < 600) delay(600 - elapsed)
            
            DeleteResult.Success
        } catch (e: Exception) {
            DeleteResult.Error(e.message ?: "Ошибка удаления")
        } finally {
            _isLoading.value = false
        }
    }


    fun getSchemesWithStatus(): Flow<List<SchemeWithStatus>> {
        return repository.getAllSchemes()
            .combine(deviceRepository.getAllDevices()) { schemes, devices ->
                schemes.map { scheme ->
                    val deviceCount = devices.count { it.location == scheme.name }
                    SchemeWithStatus(
                        scheme = scheme,
                        deviceCount = deviceCount,
                        canDelete = deviceCount == 0
                    )
                }
            }
            .combine(_searchQuery) { schemesWithStatus, query ->
                if (query.isBlank()) {
                    schemesWithStatus
                } else {
                    schemesWithStatus.filter { item ->
                        item.scheme.name.contains(query, ignoreCase = true) ||
                                item.scheme.description?.contains(query, ignoreCase = true) == true
                    }
                }
            }
            .combine(_sortBy) { filteredSchemes, sortBy ->
                when (sortBy) {
                    SchemesSortBy.NAME_ASC -> filteredSchemes.sortedBy { it.scheme.name }
                    SchemesSortBy.NAME_DESC -> filteredSchemes.sortedByDescending { it.scheme.name }
                    else -> filteredSchemes
                }
            }
    }
}

data class SchemesUiState(
    val searchQuery: String = "",
    val sortBy: SchemesSortBy = SchemesSortBy.NAME_ASC,
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class SchemeWithStatus(
    val scheme: Scheme,
    val deviceCount: Int,
    val canDelete: Boolean
)

sealed class DeleteResult {
    object Success : DeleteResult()
    data class Error(val message: String) : DeleteResult()
}
