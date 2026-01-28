package com.kipia.management.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.ui.components.table.DeviceFilterMenu
import com.kipia.management.mobile.ui.components.theme.ThemeToggleButton
import com.kipia.management.mobile.ui.components.topappbar.rememberTopAppBarController
import com.kipia.management.mobile.ui.navigation.BottomNavigationBar
import com.kipia.management.mobile.ui.navigation.KIPiANavHost
import com.kipia.management.mobile.ui.theme.KIPiATheme
import com.kipia.management.mobile.viewmodel.DevicesViewModel
import com.kipia.management.mobile.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.saveable.rememberSaveable
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.ui.shared.NotificationManager

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var deviceRepository: DeviceRepository

    @Inject
    lateinit var notificationManager: NotificationManager // ★★★★ ДОБАВЛЕНО ★★★★

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Устанавливаем тему Material 3
        setTheme(R.style.Theme_KipiaManagement)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        Timber.d("MainActivity создан")

        // Тест базы данных
        lifecycleScope.launch {
            try {
                val count = deviceRepository.getDeviceCount()
                Timber.d("ТЕСТ: В базе $count устройств")

                // Тест вставки
                val testDevice = Device.createEmpty().copy(
                    type = "Тест",
                    inventoryNumber = "TEST-001",
                    location = "Тестовая"
                )
                val newId = deviceRepository.insertDevice(testDevice)
                Timber.d("ТЕСТ: Вставлено устройство с ID: $newId")

                // Обновляем счет
                val newCount = deviceRepository.getDeviceCount()
                Timber.d("ТЕСТ: Теперь в базе $newCount устройств")
            } catch (e: Exception) {
                Timber.e("ТЕСТ: Ошибка базы: ${e.message}")
            }
        }

        setContent {
            Timber.d("Compose начал рендеринг")
            KIPiAApp(notificationManager) // ★★★★ ПЕРЕДАЕМ notificationManager ★★★★
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KIPiAApp(
    notificationManager: NotificationManager // ★★★★ ПАРАМЕТР ★★★★
) {
    KIPiATheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            var showBottomNav by rememberSaveable { mutableStateOf(true) }
            val topAppBarController = rememberTopAppBarController()

            // ★★★★ ПОЛУЧАЕМ ЗНАЧЕНИЕ STATE ПРЯМО ★★★★
            val topAppBarState = topAppBarController.state.value

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

                    // ОБРАБОТКА TOP APP BAR (оставляем без изменений)
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

                        // Главные табы
                        route in listOf("devices", "schemes", "reports") -> {
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
                    Timber.d("  Заголовок: '${topAppBarState.title}'")
                    Timber.d("  Кнопка Назад: ${topAppBarState.showBackButton}")
                    Timber.d("  Кнопка Редактировать: ${topAppBarState.showEditButton}")
                    Timber.d("  onEditClick доступен: ${topAppBarState.onEditClick != null}")
                    Timber.d("═══════════════════════════════════════════")

                    TopAppBar(
                        title = {
                            Text(
                                text = topAppBarState.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = Color.White,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White,
                            actionIconContentColor = Color.White
                        ),
                        navigationIcon = {
                            if (topAppBarState.showBackButton) {
                                IconButton(onClick = {
                                    Timber.d("Нажата кнопка 'Назад'")
                                    navController.navigateUp()
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Назад",
                                        tint = Color.White
                                    )
                                }
                            }
                        },
                        actions = {
                            // ★★★★ ОТЛАДОЧНАЯ ИНФОРМАЦИЯ О ТЕКУЩЕМ СОСТОЯНИИ ★★★★
                            Timber.d("Actions: showBackButton=${topAppBarState.showBackButton}")

                            if (!topAppBarState.showBackButton) {
                                // ★★★★ ДЕЙСТВИЯ ДЛЯ ГЛАВНОГО ЭКРАНА ★★★★
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
                                        onSearchQueryChange = { devicesViewModel.setSearchQuery(it) },
                                        locationFilter = locationFilter,
                                        locations = allLocations,
                                        onLocationFilterChange = {
                                            devicesViewModel.setLocationFilter(it)
                                        },
                                        statusFilter = statusFilter,
                                        onStatusFilterChange = { devicesViewModel.setStatusFilter(it) },
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    ThemeToggleButton()
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = {
                                        navController.navigate("settings")
                                    }) {
                                        Icon(
                                            Icons.Filled.Settings,
                                            contentDescription = "Настройки",
                                            tint = Color.White
                                        )
                                    }
                                }
                            } else {
                                // ★★★★ ПОЛЬЗОВАТЕЛЬСКИЕ ДЕЙСТВИЯ ДЛЯ ДРУГИХ ЭКРАНОВ ★★★★
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
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
                                                tint = Color.White
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
                                                tint = Color.White
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
                                                tint = Color.White
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
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
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
                        modifier = Modifier.background(MaterialTheme.colorScheme.secondary)
                    ) {
                        BottomNavigationBar(
                            navController = navController,
                            modifier = Modifier
                                .windowInsetsPadding(
                                    WindowInsets.navigationBars
                                        .only(WindowInsetsSides.Bottom)
                                        .add(WindowInsets(bottom = 0.dp))
                                )
                        )
                    }
                },
                contentWindowInsets = WindowInsets(0.dp)
            ) { innerPadding ->
                KIPiANavHost(
                    navController = navController,
                    devicesViewModel = hiltViewModel(),
                    topAppBarController = topAppBarController,
                    notificationManager = notificationManager, // ★★★★ ПЕРЕДАЕМ notificationManager ★★★★
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