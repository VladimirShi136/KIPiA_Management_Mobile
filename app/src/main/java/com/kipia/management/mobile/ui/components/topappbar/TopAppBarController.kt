package com.kipia.management.mobile.ui.components.topappbar

import androidx.compose.runtime.*

/**
 * Контроллер для управления состоянием TopAppBar во всем приложении
 * Упрощенная версия без хранения composable функций
 */
class TopAppBarController {

    // Состояние TopAppBar как MutableState
    private val _state = mutableStateOf(TopAppBarData.getDefault())
    val state: State<TopAppBarData> get() = _state

    // Обновить состояние
    fun updateState(newState: TopAppBarData) {
        _state.value = newState
    }

    // Сбросить к состоянию по умолчанию (главный экран)
    fun resetToDefault() {
        _state.value = TopAppBarData.getDefault()
    }

    // Установить состояние для конкретного экрана
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
                _state.value = TopAppBarData(
                    title = "Галерея",
                    showBackButton = true,
                    showSettingsIcon = false,
                    showThemeToggle = false,
                    showFilterMenu = false,
                    showAddButton = true,
                    onAddClick = additionalParams["onAddPhoto"] as? () -> Unit
                )
            }

            else -> {
                // Главный экран или неизвестный экран - используем состояние по умолчанию
                _state.value = TopAppBarData.getDefault()
            }
        }
    }
}

/**
 * Данные для TopAppBar (без composable функций)
 */
data class TopAppBarData(
    val title: String = "Учет приборов КИПиА",
    val showBackButton: Boolean = false,
    val showSettingsIcon: Boolean = true,
    val showThemeToggle: Boolean = true,
    val showFilterMenu: Boolean = true,
    val showEditButton: Boolean = false,
    val showSaveButton: Boolean = false,
    val showDeleteButton: Boolean = false,
    val showAddButton: Boolean = false,
    val onEditClick: (() -> Unit)? = null,
    val onSaveClick: (() -> Unit)? = null,
    val onDeleteClick: (() -> Unit)? = null,
    val onAddClick: (() -> Unit)? = null
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
                showAddButton = false
            )
        }
    }
}

/**
 * Remember для TopAppBarController
 */
@Composable
fun rememberTopAppBarController(): TopAppBarController {
    return remember { TopAppBarController() }
}