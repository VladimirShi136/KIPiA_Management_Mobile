package com.kipia.management.mobile

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.ui.components.table.DeviceFilterMenu
import com.kipia.management.mobile.ui.components.theme.ThemeToggleButton
import com.kipia.management.mobile.ui.components.topappbar.rememberTopAppBarController
import com.kipia.management.mobile.ui.navigation.BottomNavigationBar
import com.kipia.management.mobile.ui.navigation.KIPiANavHost
import com.kipia.management.mobile.ui.theme.KIPiATheme
import com.kipia.management.mobile.viewmodel.DevicesViewModel
import com.kipia.management.mobile.viewmodel.PhotosViewModel
import com.kipia.management.mobile.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.kipia.management.mobile.ui.components.photos.PhotosFilterMenu
import com.kipia.management.mobile.ui.shared.NotificationManager
import com.kipia.management.mobile.ui.theme.getTopAppBarColors
import com.kipia.management.mobile.managers.PhotoManager
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.kipia.management.mobile.repository.PreferencesRepository
import com.kipia.management.mobile.ui.components.scheme.SchemesFilterMenu
import com.kipia.management.mobile.ui.screens.schemes.SchemesSortBy
import com.kipia.management.mobile.ui.theme.AppColors
import com.kipia.management.mobile.ui.theme.SystemColors
import com.kipia.management.mobile.ui.theme.getBottomNavColors
import com.kipia.management.mobile.ui.theme.toHex
import com.kipia.management.mobile.viewmodel.SchemesViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var deviceRepository: DeviceRepository

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var photoManager: PhotoManager

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Тема Material 3
        setTheme(R.style.Theme_KipiaManagement)

        // ★★★★ УПРОЩЕННАЯ НАСТРОЙКА ДЛЯ ВСЕХ ВЕРСИЙ ★★★★
        setupEdgeToEdge()

        Timber.d("MainActivity создан")

        setContent {
            Timber.d("Compose начал рендеринг")
            KIPiAApp(
                notificationManager = notificationManager,
                photoManager = photoManager
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setupEdgeToEdge() {
        // Базовый edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Прозрачные панели
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // Для Android 8.0+ (Oreo)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Для Android 11+ (R) - используем новые API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            // Для старых версий используем старый API с suppress warning
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }
}

@Composable
fun DebugSystemColors() {
    LaunchedEffect(Unit) {
        Timber.d("=== DEBUG SYSTEM COLORS ===")
        Timber.d("AppColors.DarkBlue: ${AppColors.DarkBlue.toHex()}")
        Timber.d("AppColors.MediumDarkGray: ${AppColors.MediumDarkGray.toHex()}")
        Timber.d("SystemColors.TopAppBar.LightBackground: ${SystemColors.TopAppBar.LightBackground.toHex()}")
        Timber.d("SystemColors.TopAppBar.LightContent: ${SystemColors.TopAppBar.LightContent.toHex()}")
        Timber.d("SystemColors.TopAppBar.DarkBackground: ${SystemColors.TopAppBar.DarkBackground.toHex()}")
        Timber.d("SystemColors.TopAppBar.DarkContent: ${SystemColors.TopAppBar.DarkContent.toHex()}")
        Timber.d("SystemColors.BottomNav.LightBackground: ${SystemColors.BottomNav.LightBackground.toHex()}")
        Timber.d("SystemColors.BottomNav.DarkBackground: ${SystemColors.BottomNav.DarkBackground.toHex()}")
        Timber.d("=== END DEBUG ===")
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KIPiAApp(
    notificationManager: NotificationManager,
    photoManager: PhotoManager
) {
    val photosViewModel: PhotosViewModel = hiltViewModel()
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val schemesViewModel: SchemesViewModel = hiltViewModel()

    // ★★★★ ПОЛУЧАЕМ ТЕКУЩУЮ ТЕМУ ★★★★
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val systemUiController = rememberSystemUiController()

    // Определяем, темная ли тема
    val isDarkTheme = when (themeMode) {
        PreferencesRepository.THEME_LIGHT -> false
        PreferencesRepository.THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }

    DebugSystemColors()

    // ★★★★ КОНФИГУРАЦИЯ СИСТЕМНЫХ ПАНЕЛЕЙ ★★★★
    val statusBarColor = if (isDarkTheme) {
        SystemColors.TopAppBar.DarkBackground
    } else {
        SystemColors.TopAppBar.LightBackground
    }

    val navBarColor = if (isDarkTheme) {
        SystemColors.BottomNav.DarkBackground
    } else {
        SystemColors.BottomNav.LightBackground
    }

    val darkIcons = !isDarkTheme
    val context = LocalContext.current

    // ★★★★ ОСНОВНАЯ НАСТРОЙКА СИСТЕМНЫХ ПАНЕЛЕЙ ★★★★
    DisposableEffect(isDarkTheme, systemUiController) {
        Timber.d("Настройка системных панелей:")
        Timber.d("  isDarkTheme: $isDarkTheme")
        Timber.d("  statusBarColor: ${statusBarColor.toHex()}")
        Timber.d("  navBarColor: ${navBarColor.toHex()}")
        Timber.d("  darkIcons: $darkIcons")

        // Настраиваем системные панели
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = darkIcons
        )

        systemUiController.setNavigationBarColor(
            color = navBarColor,
            darkIcons = darkIcons,
            navigationBarContrastEnforced = false
        )

        // ★★★★ ОСОБЫЙ СЛУЧАЙ ДЛЯ REALME/OPPO ★★★★
        // Используем Post для гарантированного выполнения после рендера
        val activity = context as Activity
        activity.window.decorView.post {
            activity.window.statusBarColor = statusBarColor.toArgb()
            activity.window.navigationBarColor = navBarColor.toArgb()

            // Для Android 11+ используем новый API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val insetsController = activity.window.insetsController
                insetsController?.setSystemBarsAppearance(
                    if (darkIcons) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            } else {
                // Для старых версий Android
                @Suppress("DEPRECATION")
                activity.window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            (if (darkIcons) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0) or
                            (if (darkIcons) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0)
            }

            // Повторная настройка через systemUiController
            systemUiController.setStatusBarColor(
                color = statusBarColor,
                darkIcons = darkIcons
            )

            systemUiController.setNavigationBarColor(
                color = navBarColor,
                darkIcons = darkIcons,
                navigationBarContrastEnforced = false
            )
        }

        onDispose {
            Timber.d("Очистка настроек системных панелей")
        }
    }

    // ★★★★ ДОБАВЛЕНО: Получаем состояние photos ★★★★
    val photosState by photosViewModel.uiState.collectAsStateWithLifecycle()
    val photosDevices by photosViewModel.devices.collectAsStateWithLifecycle()
    val photosLocations by photosViewModel.allLocations.collectAsStateWithLifecycle()

    // ★★★★ ДОБАВЛЕНО: Получаем состояние schemes ★★★★
    val schemesState by schemesViewModel.uiState.collectAsStateWithLifecycle()

    KIPiATheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            var showBottomNav by rememberSaveable { mutableStateOf(true) }
            val topAppBarController = rememberTopAppBarController()
            val topAppBarState = topAppBarController.state.value
            // ★★★★ ПОЛУЧАЕМ ТЕКУЩУЮ ЦВЕТОВУЮ СХЕМУ ★★★★
            val colorScheme = MaterialTheme.colorScheme
            val (topAppBarBg, topAppBarContent) = getTopAppBarColors(isDarkTheme)
            val bottomNavColors = getBottomNavColors(isDarkTheme)
            // ★★★★ ФУНКЦИЯ ДЛЯ ОБНОВЛЕНИЯ BOTTOM NAV ★★★★
            val updateBottomNavVisibility: (Boolean) -> Unit = { isVisible ->
                Timber.d("updateBottomNavVisibility вызван: $isVisible")
                showBottomNav = isVisible
            }

            // ★★★★ ИСПОЛЬЗУЕМ OnDestinationChangedListener ДЛЯ НАВИГАЦИИ ★★★★
            LaunchedEffect(navController) {
                navController.addOnDestinationChangedListener { _, destination, arguments ->
                    val route = destination.route
                    Timber.d("═══════════════════════════════════════════")
                    Timber.d("Навигация: Маршрут = $route")
                    Timber.d("Аргументы: $arguments")

                    // ★★★★ ОБРАБОТКА BOTTOM NAV - ТОЛЬКО ПРИ СМЕНЕ МАРШРУТА ★★★★
                    // При смене маршрута устанавливаем дефолтное состояние,
                    // а затем DevicesScreen сам будет управлять скрытием при скролле
                    when {
                        route?.startsWith("device_edit") == true -> {
                            Timber.d("BottomNav: Скрыт для device_edit")
                            showBottomNav = false
                        }

                        route?.startsWith("device_detail") == true -> {
                            Timber.d("BottomNav: Скрыт для device_detail")
                            showBottomNav = false
                        }

                        route == "settings" -> {
                            Timber.d("BottomNav: Скрыт для settings")
                            showBottomNav = false
                        }

                        route?.startsWith("fullscreen_photo") == true -> {
                            Timber.d("BottomNav: Скрыт для fullscreen_photo")
                            showBottomNav = false
                        }

                        route?.startsWith("scheme_editor") == true -> {
                            Timber.d("BottomNav: Скрыт для scheme_editor")
                            showBottomNav = false
                        }

                        // Для главного экрана приборов показываем по умолчанию
                        // (DevicesScreen будет управлять скрытием при скролле)
                        route == "devices" -> {
                            Timber.d("BottomNav: Показан для devices (по умолчанию)")
                            showBottomNav = true
                        }

                        // Другие главные табы - всегда показываем
                        route in listOf("schemes", "reports", "photos") -> {
                            Timber.d("BottomNav: Показан для $route")
                            showBottomNav = true
                        }

                        else -> {
                            Timber.d("BottomNav: По умолчанию показан")
                            showBottomNav = true
                        }
                    }

                    // ОБРАБОТКА TOP APP BAR
                    when {
                        route?.startsWith("device_edit") == true -> {
                            val deviceId = arguments?.getString("deviceId")?.toIntOrNull()
                            val isNew = deviceId == null
                            topAppBarController.setForScreen("device_edit", mapOf("isNew" to isNew))
                            Timber.d("TopAppBar: Установлен для device_edit, isNew=$isNew")
                        }

                        route?.startsWith("device_detail") == true -> {
                            val deviceId = arguments?.getString("deviceId")?.toIntOrNull()
                            Timber.d("TopAppBar: Установка для device_detail, deviceId=$deviceId")

                            val params = mutableMapOf<String, Any>()
                            params["deviceName"] = "Прибор #${deviceId ?: "?"}"
                            params["onEdit"] = {
                                Timber.d("Колбэк onEdit: Переход к редактированию устройства $deviceId")
                                navController.navigate("device_edit/$deviceId")
                            }

                            topAppBarController.setForScreen("device_detail", params)
                            Timber.d("TopAppBar: Установлен для device_detail")
                        }

                        route == "settings" -> {
                            topAppBarController.setForScreen("settings")
                            Timber.d("TopAppBar: Установлен для settings")
                        }

                        route == "photos" -> {
                            topAppBarController.setForScreen("photos")
                            Timber.d("TopAppBar: Установлен для photos")
                        }

                        route == "schemes" -> {
                            Timber.d("TopAppBar: Установка для schemes")

                            // ★ ПЕРЕДАЕМ ПАРАМЕТРЫ ДЛЯ СХЕМ
                            topAppBarController.setForScreen("schemes", buildMap<String, Any> {
                                // ★ УСТАНАВЛИВАЕМ ЗАГОЛОВОК
                                put("title", "Учет приборов КИПиА")

                                // ★ ПЕРЕДАЕМ СОСТОЯНИЕ ФИЛЬТРОВ (ЯВНО ПРЕОБРАЗУЕМ В Any)
                                put("searchQuery", schemesState.searchQuery)
                                put("currentSort", schemesState.sortBy)

                                // ★ КОЛБЭКИ ДЛЯ УПРАВЛЕНИЯ ФИЛЬТРАМИ
                                put("onSearchQueryChange", { query: String ->
                                    schemesViewModel.setSearchQuery(query)
                                })
                                put("onSortSelected", { sortBy: SchemesSortBy -> // ← ИЗМЕНИЛИ
                                    schemesViewModel.setSortBy(sortBy)
                                })
                                put("onResetAllFilters", {
                                    schemesViewModel.resetAllFilters()
                                })

                                // ★ ДОБАВЛЯЕМ КНОПКИ ТЕМЫ И НАСТРОЕК
                                put("showThemeToggle", true)
                                put("showSettingsIcon", true)
                            })

                            Timber.d("TopAppBar: Установлен для schemes")
                        }

                        // Главные табы
                        route in listOf("devices", "reports") -> {
                            topAppBarController.resetToDefault()
                            Timber.d("TopAppBar: Сброшен к дефолту (главный экран)")
                        }

                        else -> {
                            topAppBarController.resetToDefault()
                            Timber.d("TopAppBar: Сброшен к дефолту (неизвестный маршрут)")
                        }
                    }
                    Timber.d("═══════════════════════════════════════════")
                }
            }

            Scaffold(
                topBar = {
                    Timber.d("═══════════════════════════════════════════")
                    Timber.d("Рендеринг TopAppBar:")
                    Timber.d("  isDarkTheme: $isDarkTheme")
                    Timber.d("  Заголовок: '${topAppBarState.title}'")
                    Timber.d("  TopAppBarBg: ${topAppBarBg.toHex()}")
                    Timber.d("  TopAppBarContent: ${topAppBarContent.toHex()}")
                    Timber.d("  Кнопка Назад: ${topAppBarState.showBackButton}")
                    Timber.d("  Показ фильтров схем: ${topAppBarState.showSchemesFilterMenu}")
                    Timber.d("  Редактор схем: ${topAppBarState.showSchemeEditorActions}")
                    Timber.d("═══════════════════════════════════════════")

                    TopAppBar(
                        title = {
                            Text(
                                text = topAppBarState.title,
                                color = topAppBarContent,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = colorScheme.onPrimary.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = topAppBarBg,
                            titleContentColor = topAppBarContent,
                            navigationIconContentColor = topAppBarContent,
                            actionIconContentColor = topAppBarContent
                        ),
                        navigationIcon = {
                            if (topAppBarState.showBackButton) {
                                IconButton(onClick = {
                                    Timber.d("Нажата кнопка 'Назад'")
                                    // ★ ПЕРВЫЙ ВЫЗОВ КОЛБЭКА ИЗ TOPAPPBAR, ЕСЛИ ОН ЕСТЬ
                                    topAppBarState.onBackClick?.invoke() ?: run {
                                        // ★ ЕСЛИ КОЛБЭКА НЕТ, ИСПОЛЬЗУЕМ СТАНДАРТНЫЙ ПОВЕДЕНИЕ
                                        navController.navigateUp()
                                    }
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Назад",
                                        tint = topAppBarContent
                                    )
                                }
                            }
                        },
                        actions = {
                            // ★★★★ ОТЛАДОЧНАЯ ИНФОРМАЦИЯ О ТЕКУЩЕМ СОСТОЯНИИ ★★★★
                            Timber.d("Actions: showBackButton=${topAppBarState.showBackButton}")

                            if (!topAppBarState.showBackButton) {
                                // ★★★★ ОПРЕДЕЛЯЕМ ТЕКУЩИЙ ЭКРАН И ПОКАЗЫВАЕМ СООТВЕТСТВУЮЩИЕ КНОПКИ ★★★★
                                when (navController.currentDestination?.route) {
                                    "photos" -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            PhotosFilterMenu(
                                                selectedLocation = photosState.selectedLocation,
                                                selectedDeviceId = photosState.selectedDeviceId,
                                                locations = photosLocations,
                                                devices = photosDevices,
                                                onLocationFilterChange = { location ->
                                                    photosViewModel.selectLocation(location)
                                                },
                                                onDeviceFilterChange = { deviceId ->
                                                    photosViewModel.selectDevice(deviceId)
                                                },
                                                onResetAllFilters = {
                                                    photosViewModel.resetAllFilters()
                                                },
                                                modifier = Modifier.padding(end = 4.dp)
                                            )

                                            ThemeToggleButton(contentColor = topAppBarContent)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(onClick = {
                                                navController.navigate("settings")
                                            }) {
                                                Icon(
                                                    Icons.Filled.Settings,
                                                    contentDescription = "Настройки",
                                                    tint = topAppBarContent
                                                )
                                            }
                                        }
                                    }

                                    "schemes" -> {
                                        // ★★★★ ФИЛЬТРЫ ДЛЯ СХЕМ ★★★★
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (topAppBarState.showSchemesFilterMenu) {
                                                SchemesFilterMenu(
                                                    searchQuery = topAppBarState.schemesSearchQuery
                                                        ?: "",
                                                    onSearchQueryChange = { query ->
                                                        topAppBarState.onSchemesSearchQueryChange?.invoke(
                                                            query
                                                        )
                                                    },
                                                    currentSort = topAppBarState.schemesCurrentSort
                                                        ?: SchemesSortBy.NAME_ASC,
                                                    onSortSelected = { sortBy ->
                                                        topAppBarState.onSchemesSortSelected?.invoke(
                                                            sortBy
                                                        )
                                                    },
                                                    onResetAllFilters = {
                                                        topAppBarState.onSchemesResetAllFilters?.invoke()
                                                    },
                                                    modifier = Modifier.padding(end = 4.dp)
                                                )
                                            }

                                            ThemeToggleButton(contentColor = topAppBarContent)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(onClick = {
                                                navController.navigate("settings")
                                            }) {
                                                Icon(
                                                    Icons.Filled.Settings,
                                                    contentDescription = "Настройки",
                                                    tint = topAppBarContent
                                                )
                                            }
                                        }
                                    }

                                    else -> {
                                        // ★★★★ ДЕЙСТВИЯ ДЛЯ ГЛАВНОГО ЭКРАНА ПРИБОРОВ И ДРУГИХ ЭКРАНОВ ★★★★
                                        val themeViewModel: ThemeViewModel = hiltViewModel()
                                        val devicesViewModel: DevicesViewModel = hiltViewModel()

                                        val searchQuery by devicesViewModel.searchQuery.collectAsStateWithLifecycle()
                                        val allLocations by devicesViewModel.allLocations.collectAsStateWithLifecycle()
                                        val uiState by devicesViewModel.uiState.collectAsStateWithLifecycle()
                                        val locationFilter = uiState.locationFilter
                                        val statusFilter = uiState.statusFilter

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            DeviceFilterMenu(
                                                searchQuery = searchQuery,
                                                onSearchQueryChange = {
                                                    devicesViewModel.setSearchQuery(it)
                                                },
                                                locationFilter = locationFilter,
                                                locations = allLocations,
                                                onLocationFilterChange = {
                                                    devicesViewModel.setLocationFilter(it)
                                                },
                                                statusFilter = statusFilter,
                                                onStatusFilterChange = {
                                                    devicesViewModel.setStatusFilter(it)
                                                },
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                            ThemeToggleButton(contentColor = topAppBarContent)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(onClick = {
                                                navController.navigate("settings")
                                            }) {
                                                Icon(
                                                    Icons.Filled.Settings,
                                                    contentDescription = "Настройки",
                                                    tint = topAppBarContent
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // ★★★★ ДЛЯ ЭКРАНОВ С КНОПКОЙ НАЗАД ★★★★
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    // ★ ОПРЕДЕЛЯЕМ ТЕКУЩИЙ ЭКРАН ПО МАРШРУТУ
                                    when {
                                        // ★ РЕДАКТОР СХЕМ
                                        navController.currentDestination?.route?.startsWith("scheme_editor") == true -> {
                                            // 1. Кнопка свойств схемы
                                            if (topAppBarState.showSchemeEditorActions) {
                                                IconButton(
                                                    onClick = {
                                                        Timber.d("Кнопка свойств нажата")
                                                        topAppBarState.onPropertiesClick?.invoke()
                                                    },
                                                    enabled = topAppBarState.onPropertiesClick != null
                                                ) {
                                                    Icon(
                                                        Icons.Default.Tune,
                                                        contentDescription = "Свойства схемы",
                                                        tint = topAppBarContent
                                                    )
                                                }
                                            }

                                            // 2. Кнопка сохранения
                                            if (topAppBarState.canSave) {
                                                IconButton(
                                                    onClick = {
                                                        Timber.d("Кнопка сохранения нажата")
                                                        topAppBarState.onSaveClick?.invoke()
                                                    },
                                                    enabled = topAppBarState.onSaveClick != null
                                                ) {
                                                    Icon(
                                                        Icons.Default.Save,
                                                        contentDescription = "Сохранить",
                                                        tint = topAppBarContent
                                                    )
                                                }
                                            }

                                            // 3. Кнопка настроек редактора
                                            if (topAppBarState.showSchemeEditorActions) {
                                                IconButton(
                                                    onClick = {
                                                        Timber.d("Кнопка настроек редактора нажата")
                                                        topAppBarState.onEditorSettingsClick?.invoke()
                                                    },
                                                    enabled = topAppBarState.onEditorSettingsClick != null
                                                ) {
                                                    Icon(
                                                        Icons.Default.Settings,
                                                        contentDescription = "Настройки редактора",
                                                        tint = topAppBarContent
                                                    )
                                                }
                                            }
                                        }

                                        // ★ РЕДАКТОР ПРИБОРА (device_edit)
                                        navController.currentDestination?.route?.startsWith("device_edit") == true -> {
                                            // ★★★★ КНОПКА РЕДАКТИРОВАНИЯ ★★★★
                                            if (topAppBarState.showEditButton) {
                                                IconButton(
                                                    onClick = {
                                                        Timber.d("Кнопка редактирования нажата")
                                                        topAppBarState.onEditClick?.invoke()
                                                    },
                                                    enabled = topAppBarState.onEditClick != null
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Edit,
                                                        contentDescription = "Редактировать",
                                                        tint = topAppBarContent
                                                    )
                                                }
                                            }
                                            // ★★★★ КНОПКА СОХРАНЕНИЯ ★★★★
                                            if (topAppBarState.showSaveButton) {
                                                IconButton(
                                                    onClick = {
                                                        Timber.d("Кнопка сохранения нажата")
                                                        topAppBarState.onSaveClick?.invoke()
                                                    },
                                                    enabled = topAppBarState.onSaveClick != null
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Save,
                                                        contentDescription = "Сохранить",
                                                        tint = topAppBarContent
                                                    )
                                                }
                                            }
                                            // ★★★★ КНОПКА УДАЛЕНИЯ ★★★★
                                            if (topAppBarState.showDeleteButton) {
                                                IconButton(
                                                    onClick = {
                                                        Timber.d("Кнопка удаления нажата")
                                                        topAppBarState.onDeleteClick?.invoke()
                                                    },
                                                    enabled = topAppBarState.onDeleteClick != null
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Delete,
                                                        contentDescription = "Удалить",
                                                        tint = topAppBarContent
                                                    )
                                                }
                                            }
                                            // ★★★★ КНОПКА ДОБАВЛЕНИЯ ★★★★
                                            if (topAppBarState.showAddButton) {
                                                IconButton(
                                                    onClick = {
                                                        Timber.d("Кнопка добавления нажата")
                                                        topAppBarState.onAddClick?.invoke()
                                                    },
                                                    enabled = topAppBarState.onAddClick != null
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Add,
                                                        contentDescription = "Добавить",
                                                        tint = topAppBarContent
                                                    )
                                                }
                                            }
                                        }

                                        // ★ ДЕТАЛИ ПРИБОРА (device_detail)
                                        navController.currentDestination?.route?.startsWith("device_detail") == true -> {
                                            // ★★★★ КНОПКА РЕДАКТИРОВАНИЯ ★★★★
                                            if (topAppBarState.showEditButton) {
                                                IconButton(
                                                    onClick = {
                                                        Timber.d("Кнопка редактирования нажата")
                                                        topAppBarState.onEditClick?.invoke()
                                                    },
                                                    enabled = topAppBarState.onEditClick != null
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Edit,
                                                        contentDescription = "Редактировать",
                                                        tint = topAppBarContent
                                                    )
                                                }
                                            }
                                        }

                                        // ★ НАСТРОЙКИ (settings)
                                        navController.currentDestination?.route == "settings" -> {
                                            // Можно добавить специальные кнопки для настроек если нужно
                                        }

                                        // ★ ПОЛНОЭКРАННОЕ ФОТО (fullscreen_photo)
                                        navController.currentDestination?.route?.startsWith("fullscreen_photo") == true -> {
                                            // Можно добавить кнопки для управления фото
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .windowInsetsPadding(
                                WindowInsets.navigationBars
                                    .only(WindowInsetsSides.Horizontal)
                            )
                    )
                },
                bottomBar = {
                    AnimatedVisibility(
                        visible = showBottomNav,
                        enter = slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(durationMillis = 300)
                        ) + fadeIn(animationSpec = tween(durationMillis = 200)),
                        exit = slideOutVertically(
                            targetOffsetY = { fullHeight -> fullHeight },
                            animationSpec = tween(durationMillis = 300)
                        ) + fadeOut(animationSpec = tween(durationMillis = 200)),
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars),
                            color = bottomNavColors.background,
                            shape = RectangleShape,
                            tonalElevation = 4.dp
                        ) {
                            BottomNavigationBar(
                                navController = navController,
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }
                },
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                KIPiANavHost(
                    navController = navController,
                    devicesViewModel = hiltViewModel(),
                    photosViewModel = photosViewModel,
                    schemesViewModel = schemesViewModel,
                    topAppBarController = topAppBarController,
                    notificationManager = notificationManager,
                    photoManager = photoManager,
                    updateBottomNavVisibility = updateBottomNavVisibility,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                )
            }
        }
    }
}