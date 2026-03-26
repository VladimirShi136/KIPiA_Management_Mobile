package com.kipia.management.mobile.ui.components.topappbar

import androidx.compose.runtime.*
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.ui.screens.reports.models.ReportFilter
import com.kipia.management.mobile.ui.screens.schemes.SchemesSortBy

/**
 * Контроллер для управления состоянием TopAppBar во всем приложении
 */
class TopAppBarController {

    private val _state = mutableStateOf(TopAppBarData.getDefault())
    val state: State<TopAppBarData> get() = _state

    fun updateState(newState: TopAppBarData) {
        _state.value = newState
    }

    fun resetToDefault() {
        _state.value = TopAppBarData.getDefault()
    }

    fun setForScreen(screenRoute: String, additionalParams: Map<String, Any> = emptyMap()) {
        when (screenRoute) {
            "settings" -> {
                _state.value = TopAppBarData(
                    title = "Настройки",
                    showBackButton = true,
                    showSettingsIcon = false,
                    showThemeToggle = false,
                    showFilterMenu = false
                )
            }

            "device_detail" -> {
                _state.value = TopAppBarData(
                    title = "Детали прибора",
                    showBackButton = true,
                    showSettingsIcon = false,
                    showThemeToggle = false,
                    showFilterMenu = false,
                    showEditButton = true,
                    onEditClick = additionalParams["onEdit"] as? () -> Unit
                )
            }

            "device_edit" -> {
                val isNew = additionalParams["isNew"] as? Boolean ?: true
                _state.value = TopAppBarData(
                    title = if (isNew) "Новый прибор" else "Редактирование",
                    showBackButton = true,
                    showSettingsIcon = false,
                    showThemeToggle = false,
                    showFilterMenu = false,
                    showSaveButton = true,
                    showDeleteButton = !isNew,
                    onSaveClick = additionalParams["onSave"] as? () -> Unit,
                    onDeleteClick = additionalParams["onDelete"] as? () -> Unit
                )
            }

            "photos" -> {
                val selectedLocation = additionalParams["selectedLocation"] as? String
                val selectedDeviceId = additionalParams["selectedDeviceId"] as? Int
                val actualLocation = if (selectedLocation.isNullOrEmpty()) null else selectedLocation
                val actualDeviceId = if (selectedDeviceId == 0) null else selectedDeviceId

                _state.value = TopAppBarData(
                    title = "Учет приборов КИПиА",
                    showBackButton = false,
                    showSettingsIcon = true,
                    showThemeToggle = true,
                    showFilterMenu = true,
                    showAddButton = false,
                    isGridView = additionalParams["isGridView"] as? Boolean ?: true,
                    selectedLocation = actualLocation,
                    selectedDeviceId = actualDeviceId,
                    locations = additionalParams["locations"] as? List<String> ?: emptyList(),
                    devices = additionalParams["devices"] as? List<Device> ?: emptyList(),
                    onLocationFilterChange = additionalParams["onLocationFilterChange"] as? ((String?) -> Unit),
                    onDeviceFilterChange = additionalParams["onDeviceFilterChange"] as? ((Int?) -> Unit),
                )
            }

            "schemes" -> {
                val searchQuery = additionalParams["searchQuery"] as? String ?: ""
                additionalParams["selectedFilter"] as? String?
                val currentSort = additionalParams["currentSort"] as? SchemesSortBy
                    ?: SchemesSortBy.NAME_ASC

                _state.value = TopAppBarData(
                    title = additionalParams["title"] as? String ?: "Учет приборов КИПиА",
                    showBackButton = false,
                    showSettingsIcon = additionalParams["showSettingsIcon"] as? Boolean ?: true,
                    showThemeToggle = additionalParams["showThemeToggle"] as? Boolean ?: true,
                    showFilterMenu = true,
                    showSchemesFilterMenu = true,
                    schemesSearchQuery = searchQuery,
                    schemesCurrentSort = currentSort,
                    onSchemesSearchQueryChange = additionalParams["onSearchQueryChange"] as? ((String) -> Unit),
                    onSchemesSortSelected = additionalParams["onSortSelected"] as? ((SchemesSortBy) -> Unit),
                    onSchemesResetAllFilters = additionalParams["onResetAllFilters"] as? (() -> Unit)
                )
            }

            "scheme_editor" -> {
                _state.value = TopAppBarData(
                    title = "Редактор",
                    showBackButton = true,
                    showSettingsIcon = false,
                    showThemeToggle = false,
                    showFilterMenu = false,
                    showSchemeEditorActions = true,
                    canSave = additionalParams["canSave"] as? Boolean ?: true,
                    canUndo = additionalParams["canUndo"] as? Boolean ?: false,
                    canRedo = additionalParams["canRedo"] as? Boolean ?: false,
                    isDirty = additionalParams["isDirty"] as? Boolean ?: false,
                    showClearButton = true,
                    canClear = additionalParams["canClear"] as? Boolean ?: true,
                    onClearClick = additionalParams["onClearClick"] as? (() -> Unit),
                    onBackClick = additionalParams["onBackClick"] as? (() -> Unit),
                    onSaveClick = additionalParams["onSaveClick"] as? (() -> Unit),
                    onUndoClick = additionalParams["onUndoClick"] as? (() -> Unit),
                    onRedoClick = additionalParams["onRedoClick"] as? (() -> Unit),
                    onPropertiesClick = additionalParams["onPropertiesClick"] as? (() -> Unit),
                    onEditorSettingsClick = additionalParams["onEditorSettingsClick"] as? (() -> Unit)
                )
            }

            "fullscreen_photo" -> {
                val inventoryNumber = additionalParams["inventoryNumber"] as? String ?: ""
                val valveNumber = additionalParams["valveNumber"] as? String

                _state.value = TopAppBarData(
                    title = inventoryNumber,
                    showBackButton = true,
                    showSettingsIcon = false,
                    showThemeToggle = false,
                    showFilterMenu = false,
                    showPhotoActions = true,
                    photoInventoryNumber = inventoryNumber,
                    photoValveNumber = valveNumber,
                    photoFileName = additionalParams["photoFileName"] as? String,
                    photoFilePath = additionalParams["photoFilePath"] as? String,
                    onBackClick = additionalParams["onBackClick"] as? (() -> Unit),
                    onRotateLeftClick = additionalParams["onRotateLeftClick"] as? (() -> Unit),
                    onRotateRightClick = additionalParams["onRotateRightClick"] as? (() -> Unit),
                    onDeletePhotoClick = additionalParams["onDeletePhotoClick"] as? (() -> Unit)
                )
            }

            "reports_with_filter" -> {
                _state.value = TopAppBarData(
                    title = additionalParams["title"] as? String ?: "Учет приборов КИПиА",
                    showBackButton = additionalParams["showBackButton"] as? Boolean ?: false,
                    showSettingsIcon = additionalParams["showSettingsIcon"] as? Boolean ?: true,
                    showThemeToggle = additionalParams["showThemeToggle"] as? Boolean ?: true,
                    showFilterMenu = false,
                    showReportFilterMenu = true,
                    reportFilter = additionalParams["reportFilter"] as? ReportFilter ?: ReportFilter.Empty,
                    reportFilterAvailableStatuses = additionalParams["availableStatuses"] as? List<String> ?: emptyList(),
                    reportFilterAvailableTypes = additionalParams["availableTypes"] as? List<String> ?: emptyList(),
                    reportFilterAvailableManufacturers = additionalParams["availableManufacturers"] as? List<String> ?: emptyList(),
                    reportFilterAvailableLocations = additionalParams["availableLocations"] as? List<String> ?: emptyList(),
                    reportFilterAvailableYears = additionalParams["availableYears"] as? List<Int> ?: emptyList(),
                    onReportFilterChange = additionalParams["onFilterChange"] as? ((ReportFilter) -> Unit)
                )
            }

            else -> {
                _state.value = TopAppBarData.getDefault()
            }
        }
    }
}

@Stable
data class TopAppBarData(
    // ★ ОСНОВНЫЕ ПОЛЯ
    val title: String = "Учет приборов КИПиА",
    val showBackButton: Boolean = false,
    val showSettingsIcon: Boolean = true,
    val showThemeToggle: Boolean = true,
    val showFilterMenu: Boolean = true,
    val showEditButton: Boolean = false,
    val showSaveButton: Boolean = false,
    val showDeleteButton: Boolean = false,
    val showAddButton: Boolean = false,
    val isGridView: Boolean = true,
    val selectedLocation: String? = null,
    val selectedDeviceId: Int? = null,
    // ★ ПОЛЯ ДЛЯ КНОПКИ ОЧИСТКИ В РЕДАКТОРЕ СХЕМ
    val showClearButton: Boolean = false,
    val canClear: Boolean = true,
    // ★ Общий колбэк
    val onSaveClick: (() -> Unit)? = null,
    // ★ КОЛБЭКИ ДЛЯ ПРИБОРОВ
    val onEditClick: (() -> Unit)? = null,
    val onDeleteClick: (() -> Unit)? = null,
    val onAddClick: (() -> Unit)? = null,
    // ★ ПОЛЯ ДЛЯ МЕНЮ ФИЛЬТРАЦИИ ФОТО
    val locations: List<String> = emptyList(),
    val devices: List<Device> = emptyList(),
    // ★ КОЛБЭКИ ДЛЯ МЕНЮ ФИЛЬТРАЦИИ ФОТО
    val onLocationFilterChange: ((String?) -> Unit)? = null,
    val onDeviceFilterChange: ((Int?) -> Unit)? = null,
    // ★ ПОЛЯ ДЛЯ МЕНЮ ФИЛЬТРАЦИИ СХЕМ
    val showSchemesFilterMenu: Boolean = false,
    val schemesSearchQuery: String? = null,
    val schemesCurrentSort: SchemesSortBy? = null,
    // ★ КОЛБЭКИ ДЛЯ МЕНЮ ФИЛЬТРАЦИИ СХЕМ
    val onSchemesSearchQueryChange: ((String) -> Unit)? = null,
    val onSchemesSortSelected: ((SchemesSortBy) -> Unit)? = null,
    val onSchemesResetAllFilters: (() -> Unit)? = null,
    // ★ ПОЛЯ ДЛЯ РЕДАКТОРА СХЕМ
    val showSchemeEditorActions: Boolean = false,
    val isNewScheme: Boolean = true,
    val canSave: Boolean = true,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isDirty: Boolean = false,
    // ★ КОЛБЭКИ ДЛЯ РЕДАКТОРА СХЕМ
    val onBackClick: (() -> Unit)? = null,
    val onUndoClick: (() -> Unit)? = null,
    val onRedoClick: (() -> Unit)? = null,
    val onPropertiesClick: (() -> Unit)? = null,
    val onEditorSettingsClick: (() -> Unit)? = null,
    // ★ КОЛБЭК ДЛЯ ОЧИСТКИ СХЕМЫ
    val onClearClick: (() -> Unit)? = null,
    // ★ ПОЛЯ ДЛЯ ПОЛНОЭКРАННОГО ПРОСМОТРА ФОТО
    val showPhotoActions: Boolean = false,
    val photoInventoryNumber: String? = null,
    val photoValveNumber: String? = null,
    val photoFileName: String? = null,
    val photoFilePath: String? = null,
    val onRotateLeftClick: (() -> Unit)? = null,
    val onRotateRightClick: (() -> Unit)? = null,
    val onDeletePhotoClick: (() -> Unit)? = null,
    // ★ ПОЛЯ ДЛЯ ФИЛЬТРАЦИИ ОТЧЁТОВ
    val showReportFilterMenu: Boolean = false,
    val reportFilter: ReportFilter = ReportFilter.Empty,
    val reportFilterAvailableStatuses: List<String> = emptyList(),
    val reportFilterAvailableTypes: List<String> = emptyList(),
    val reportFilterAvailableManufacturers: List<String> = emptyList(),
    val reportFilterAvailableLocations: List<String> = emptyList(),
    val reportFilterAvailableYears: List<Int> = emptyList(),
    val onReportFilterChange: ((ReportFilter) -> Unit)? = null
) {
    companion object {
        fun getDefault(): TopAppBarData {
            return TopAppBarData(
                title = "Учет приборов КИПиА",
                showBackButton = false,
                showSettingsIcon = true,
                showThemeToggle = true,
                showFilterMenu = true,
                showEditButton = false,
                showSaveButton = false,
                showDeleteButton = false,
                showAddButton = false,
                isGridView = true
            )
        }
    }
}

@Composable
fun rememberTopAppBarController(): TopAppBarController {
    return remember { TopAppBarController() }
}