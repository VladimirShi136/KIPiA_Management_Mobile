// viewmodel/SchemesViewModel.kt
package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.repository.SchemeRepository
import com.kipia.management.mobile.ui.screens.schemes.SortBy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SchemesViewModel @Inject constructor(
    private val repository: SchemeRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortBy = MutableStateFlow(SortBy.DATE_DESC)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState = combine(_searchQuery, _sortBy, _isLoading, _error) {
            searchQuery, sortBy, isLoading, error ->
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
        .combine(_searchQuery, _sortBy) { schemes, query, sortBy ->
            var filtered = if (query.isBlank()) {
                schemes
            } else {
                schemes.filter { scheme ->
                    scheme.name.contains(query, ignoreCase = true) ||
                            scheme.description?.contains(query, ignoreCase = true) == true
                }
            }

            // Сортировка
            filtered = when (sortBy) {
                SortBy.NAME_ASC -> filtered.sortedBy { it.name }
                SortBy.NAME_DESC -> filtered.sortedByDescending { it.name }
                SortBy.DATE_ASC -> filtered.sortedBy { it.createdAt }
                SortBy.DATE_DESC -> filtered.sortedByDescending { it.createdAt }
            }

            filtered
        }.stateIn(
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

    fun deleteScheme(scheme: Scheme) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.deleteScheme(scheme)
            } catch (e: Exception) {
                _error.value = "Ошибка удаления: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadSchemes() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // Загрузка происходит через Flow автоматически
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

data class SchemesUiState(
    val searchQuery: String = "",
    val sortBy: SortBy = SortBy.DATE_DESC,
    val isLoading: Boolean = false,
    val error: String? = null
)