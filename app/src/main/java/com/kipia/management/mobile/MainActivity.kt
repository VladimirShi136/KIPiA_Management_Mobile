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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.kipia.management.mobile.managers.PhotoManager
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.repository.PreferencesRepository
import com.kipia.management.mobile.ui.components.topappbar.KIPiATopAppBar
import com.kipia.management.mobile.ui.components.topappbar.rememberTopAppBarController
import com.kipia.management.mobile.ui.navigation.BottomNavigationBar
import com.kipia.management.mobile.ui.navigation.KIPiANavHost
import com.kipia.management.mobile.ui.screens.schemes.SchemesSortBy
import com.kipia.management.mobile.ui.shared.NotificationManager
import com.kipia.management.mobile.ui.theme.BottomNavColors
import com.kipia.management.mobile.ui.theme.KIPiATheme
import com.kipia.management.mobile.ui.theme.SystemColors
import com.kipia.management.mobile.ui.theme.toHex
import com.kipia.management.mobile.viewmodel.PhotosViewModel
import com.kipia.management.mobile.viewmodel.SchemesViewModel
import com.kipia.management.mobile.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

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

@RequiresApi(Build.VERSION_CODES.O)
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

    // Определяем, темная ли тема (простое вычисление без derivedStateOf)
    val isDarkTheme = when (themeMode) {
        PreferencesRepository.THEME_LIGHT -> false
        PreferencesRepository.THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }

    // ★★★★ Мемоизация цветов (вычисляются ТОЛЬКО при смене темы) ★★★★
    val topAppBarColors = remember(isDarkTheme) {
        if (isDarkTheme) {
            Pair(SystemColors.TopAppBar.DarkBackground, SystemColors.TopAppBar.DarkContent)
        } else {
            Pair(SystemColors.TopAppBar.LightBackground, SystemColors.TopAppBar.LightContent)
        }
    }

    val bottomNavColors = remember(isDarkTheme) {
        if (isDarkTheme) {
            BottomNavColors(
                background = SystemColors.BottomNav.DarkBackground,
                selectedText = SystemColors.BottomNav.DarkSelectedText,
                unselectedText = SystemColors.BottomNav.DarkUnselectedText,
                border = SystemColors.BottomNav.DarkBorder
            )
        } else {
            BottomNavColors(
                background = SystemColors.BottomNav.LightBackground,
                selectedText = SystemColors.BottomNav.LightSelectedText,
                unselectedText = SystemColors.BottomNav.LightUnselectedText,
                border = SystemColors.BottomNav.LightBorder
            )
        }
    }

    // ★★★★ Извлекаем цвета для удобства использования ★★★★
    val (topAppBarBg, topAppBarContent) = topAppBarColors

    // ★★★★ КОНФИГУРАЦИЯ СИСТЕМНЫХ ПАНЕЛЕЙ (с rememberUpdatedState) ★★★★
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

    // Используем rememberUpdatedState для актуальных значений в эффекте
    val currentIsDarkTheme by rememberUpdatedState(isDarkTheme)
    val currentStatusBarColor by rememberUpdatedState(statusBarColor)
    val currentNavBarColor by rememberUpdatedState(navBarColor)
    val currentDarkIcons by rememberUpdatedState(darkIcons)
    val currentSystemUiController by rememberUpdatedState(systemUiController)

    // ★★★★ ЭФФЕКТ ДЛЯ СИСТЕМНЫХ ПАНЕЛЕЙ ★★★★
    DisposableEffect(currentIsDarkTheme, currentSystemUiController) {
        Timber.d("Настройка системных панелей:")
        Timber.d("  isDarkTheme: $currentIsDarkTheme")
        Timber.d("  statusBarColor: ${currentStatusBarColor.toHex()}")
        Timber.d("  navBarColor: ${currentNavBarColor.toHex()}")
        Timber.d("  darkIcons: $currentDarkIcons")

        // Настраиваем системные панели
        currentSystemUiController.setStatusBarColor(
            color = currentStatusBarColor,
            darkIcons = currentDarkIcons
        )

        currentSystemUiController.setNavigationBarColor(
            color = currentNavBarColor,
            darkIcons = currentDarkIcons,
            navigationBarContrastEnforced = false
        )

        // ★★★★ ОСОБЫЙ СЛУЧАЙ ДЛЯ REALME/OPPO ★★★★
        val activity = context as Activity
        activity.window.decorView.post {
            activity.window.statusBarColor = currentStatusBarColor.toArgb()
            activity.window.navigationBarColor = currentNavBarColor.toArgb()

            // Для Android 11+ используем новый API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val insetsController = activity.window.insetsController
                insetsController?.setSystemBarsAppearance(
                    if (currentDarkIcons) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
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
                            (if (currentDarkIcons) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0) or
                            (if (currentDarkIcons) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0)
            }

            // Повторная настройка через systemUiController
            currentSystemUiController.setStatusBarColor(
                color = currentStatusBarColor,
                darkIcons = currentDarkIcons
            )

            currentSystemUiController.setNavigationBarColor(
                color = currentNavBarColor,
                darkIcons = currentDarkIcons,
                navigationBarContrastEnforced = false
            )
        }

        onDispose {
            Timber.d("Очистка настроек системных панелей")
        }
    }

    // ★★★★ ПОЛУЧАЕМ СОСТОЯНИЯ (с lifecycle-aware коллекшенами) ★★★★
    val photosState by photosViewModel.uiState.collectAsStateWithLifecycle()
    val photosDevices by photosViewModel.devices.collectAsStateWithLifecycle()
    val photosLocations by photosViewModel.allLocations.collectAsStateWithLifecycle()
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

            // ★★★★ Колбэк для кнопки назад (мемоизирован) ★★★★
            val onBackClick: () -> Unit = remember {
                {
                    Timber.d("Нажата кнопка 'Назад'")
                    topAppBarState.onBackClick?.invoke() ?: navController.navigateUp()
                }
            }

            // ★★★★ Функция обновления BottomNav (мемоизирована) ★★★★
            val updateBottomNavVisibility = remember {
                { isVisible: Boolean ->
                    Timber.d("updateBottomNavVisibility вызван: $isVisible")
                    showBottomNav = isVisible
                }
            }

            // ★★★★ НАВИГАЦИОННЫЙ ЛИСТЕНЕР (оптимизирован) ★★★★
            LaunchedEffect(navController) {
                navController.addOnDestinationChangedListener { _, destination, arguments ->
                    val route = destination.route

                    // Обновляем BottomNav visibility
                    showBottomNav = when {
                        route?.startsWith("device_edit") == true -> false
                        route?.startsWith("device_detail") == true -> false
                        route == "settings" -> false
                        route?.startsWith("fullscreen_photo") == true -> false
                        route?.startsWith("scheme_editor") == true -> false
                        route == "devices" -> true
                        route in listOf("schemes", "reports", "photos") -> true
                        else -> true
                    }

                    // Обновляем TopAppBar состояние
                    when {
                        route?.startsWith("device_edit") == true -> {
                            val deviceId = arguments?.getString("deviceId")?.toIntOrNull()
                            val isNew = deviceId == null
                            topAppBarController.setForScreen("device_edit", mapOf("isNew" to isNew))
                        }

                        route?.startsWith("device_detail") == true -> {
                            val deviceId = arguments?.getString("deviceId")?.toIntOrNull()
                            topAppBarController.setForScreen(
                                "device_detail", mapOf(
                                "deviceName" to "Прибор #${deviceId ?: "?"}",
                                "onEdit" to { navController.navigate("device_edit/$deviceId") }
                            ))
                        }

                        route == "settings" -> {
                            topAppBarController.setForScreen("settings")
                        }

                        route == "photos" -> {
                            topAppBarController.setForScreen(
                                "photos", mapOf(
                                "selectedLocation" to (photosState.selectedLocation ?: ""),
                                "selectedDeviceId" to (photosState.selectedDeviceId ?: 0),
                                "locations" to photosLocations,
                                "devices" to photosDevices,
                                "onLocationFilterChange" to { location: String? ->
                                    photosViewModel.selectLocation(location)
                                },
                                "onDeviceFilterChange" to { deviceId: Int? ->
                                    photosViewModel.selectDevice(deviceId)
                                }
                            ))
                        }

                        route == "schemes" -> {
                            topAppBarController.setForScreen(
                                "schemes", mapOf(
                                "title" to "Учет приборов КИПиА",
                                "searchQuery" to schemesState.searchQuery,
                                "currentSort" to (schemesState.sortBy ?: SchemesSortBy.NAME_ASC),
                                "showThemeToggle" to true,
                                "showSettingsIcon" to true,
                                "onSearchQueryChange" to { query: String ->
                                    schemesViewModel.setSearchQuery(query)
                                },
                                "onSortSelected" to { sortBy: SchemesSortBy ->
                                    schemesViewModel.setSortBy(sortBy)
                                },
                                "onResetAllFilters" to {
                                    schemesViewModel.resetAllFilters()
                                }
                            ))
                        }

                        route == "scheme_editor" -> {
                            // Получаем параметры из аргументов или устанавливаем по умолчанию
                            val schemeId = arguments?.getString("schemeId")?.toIntOrNull()
                            topAppBarController.setForScreen(
                                "scheme_editor", mapOf(
                                "canSave" to true,
                                "canUndo" to false,
                                "canRedo" to false,
                                "isDirty" to false,
                                "onBackClick" to { navController.navigateUp() },
                                "onSaveClick" to { /* будет установлено из SchemeEditorScreen */ },
                                "onPropertiesClick" to { /* будет установлено из SchemeEditorScreen */ }
                            ))
                        }

                        else -> {
                            topAppBarController.resetToDefault()
                        }
                    }
                }
            }

            Scaffold(
                topBar = {
                    KIPiATopAppBar(
                        topAppBarState = topAppBarState,
                        topAppBarBg = topAppBarBg,
                        topAppBarContent = topAppBarContent,
                        onBackClick = onBackClick,
                        navController = navController,
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