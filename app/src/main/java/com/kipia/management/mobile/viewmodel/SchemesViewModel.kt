package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.repository.SchemeRepository
import com.kipia.management.mobile.ui.screens.schemes.SortBy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SchemesViewModel @Inject constructor(
    private val repository: SchemeRepository,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortBy = MutableStateFlow(SortBy.NAME_ASC)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    // Используем combine для 4 потоков
    val uiState = combine(
        _searchQuery,
        _sortBy,
        _isLoading,
        _error
    ) { searchQuery, sortBy, isLoading, error ->
        SchemesUiState(
            searchQuery = searchQuery,
            sortBy = sortBy,
            isLoading = isLoading,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SchemesUiState()
    )

    // Сортировка и фильтрация схем
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
            // Сортировка только по имени
            when (sortBy) {
                SortBy.NAME_ASC -> filteredSchemes.sortedBy { it.name }
                SortBy.NAME_DESC -> filteredSchemes.sortedByDescending { it.name }
                else -> filteredSchemes // Для других значений оставляем как есть
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortBy(sortBy: SortBy) {
        _sortBy.value = sortBy
    }

    suspend fun deleteScheme(scheme: Scheme): DeleteResult {
        return try {
            _isLoading.value = true

            // ★★★★ ПРОВЕРКА: ЕСТЬ ЛИ ПРИБОРЫ С ЭТОЙ ЛОКАЦИЕЙ? ★★★★
            val devices = deviceRepository.getAllDevicesSync()
            val deviceCount = devices.count { it.location == scheme.name }

            if (deviceCount > 0) {
                return DeleteResult.Error("Нельзя удалить схему '$scheme.name'. " +
                        "К ней привязано $deviceCount приборов. " +
                        "Сначала удалите или переместите приборы.")
            }

            // Если приборов нет - удаляем схему
            repository.deleteScheme(scheme)
            DeleteResult.Success
        } catch (e: Exception) {
            DeleteResult.Error(e.message ?: "Ошибка удаления")
        } finally {
            _isLoading.value = false
        }
    }

    // Дополнительный метод для отображения статуса
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
    }
}

data class SchemesUiState(
    val searchQuery: String = "",
    val sortBy: SortBy = SortBy.NAME_ASC,
    val isLoading: Boolean = false,
    val error: String? = null
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