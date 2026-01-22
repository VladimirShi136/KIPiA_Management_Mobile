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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var deviceRepository: DeviceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Устанавливаем тему Material 3
        setTheme(R.style.Theme_KipiaManagement)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        Timber.d("MainActivity создан")

        // Тест базы данных
        lifecycleScope.launch {
            try {
                Timber.d("ТЕСТ: Пытаемся получить устройства...")
                val devices = deviceRepository.getAllDevicesSync()
                Timber.d("ТЕСТ: Успех! В базе ${devices.size} устройств")
            } catch (e: Exception) {
                Timber.e("ТЕСТ: Ошибка базы данных: ${e.message}")
                e.printStackTrace()
            }
        }

        setContent {
            Timber.d("Compose начал рендеринг")
            KIPiAApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KIPiAApp() {
    KIPiATheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            var showBottomNav by remember { mutableStateOf(true) }

            // ★★★★ ИСПОЛЬЗУЕМ TopAppBarController ★★★★
            val topAppBarController = rememberTopAppBarController()
            val topAppBarState = topAppBarController.state.value

            // ★★★★ ОБНОВЛЯЕМ СОСТОЯНИЕ ПРИ ИЗМЕНЕНИИ МАРШРУТА ★★★★
            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            LaunchedEffect(currentBackStackEntry) {
                val currentRoute = currentBackStackEntry?.destination?.route
                val arguments = currentBackStackEntry?.arguments

                // Извлекаем параметры из аргументов
                val params = mutableMapOf<String, Any>()

                when {
                    currentRoute?.startsWith("device_detail/") == true -> {
                        val deviceId = arguments?.getString("deviceId")?.toIntOrNull()
                        params["deviceName"] = "Прибор #$deviceId"
                        params["onEdit"] = {
                            navController.navigate("device_edit/$deviceId")
                        }
                        topAppBarController.setForScreen("device_detail", params)
                        showBottomNav = false
                    }

                    currentRoute?.startsWith("device_edit") == true -> {
                        val deviceId = arguments?.getString("deviceId")?.toIntOrNull()
                        params["isNew"] = deviceId == null
                        params["onSave"] = {
                            // TODO: Вызвать сохранение устройства
                            navController.popBackStack()
                        }
                        topAppBarController.setForScreen("device_edit", params)
                        showBottomNav = false
                    }

                    currentRoute == "settings" -> {
                        topAppBarController.setForScreen("settings")
                        showBottomNav = false
                    }

                    currentRoute == "photos" -> {
                        params["onAddPhoto"] = {
                            // TODO: Открыть выбор фото
                        }
                        topAppBarController.setForScreen("photos", params)
                        showBottomNav = false
                    }

                    // Главные табы
                    currentRoute in listOf("devices", "schemes", "reports") -> {
                        topAppBarController.resetToDefault()
                        showBottomNav = true
                    }

                    else -> {
                        topAppBarController.resetToDefault()
                        showBottomNav = true
                    }
                }
            }

            Scaffold(
                topBar = {
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
                            // ВАЖНО: navigationIcon должен быть non-nullable
                            if (topAppBarState.showBackButton) {
                                IconButton(onClick = { navController.navigateUp() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Назад",
                                        tint = Color.White
                                    )
                                }
                            }
                            // Если showBackButton = false, ничего не рисуем
                        },
                        actions = {
                            // Действия по умолчанию для главного экрана
                            if (!topAppBarState.showBackButton) {
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
                                        onLocationFilterChange = { devicesViewModel.setLocationFilter(it) },
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
                                // Пользовательские действия для других экранов
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (topAppBarState.showEditButton) {
                                        IconButton(
                                            onClick = { topAppBarState.onEditClick?.invoke() },
                                            enabled = topAppBarState.onEditClick != null
                                        ) {
                                            Icon(
                                                Icons.Filled.Edit,
                                                contentDescription = "Редактировать",
                                                tint = Color.White
                                            )
                                        }
                                    }

                                    if (topAppBarState.showSaveButton) {
                                        IconButton(
                                            onClick = { topAppBarState.onSaveClick?.invoke() },
                                            enabled = topAppBarState.onSaveClick != null
                                        ) {
                                            Icon(
                                                Icons.Filled.Save,
                                                contentDescription = "Сохранить",
                                                tint = Color.White
                                            )
                                        }
                                    }

                                    if (topAppBarState.showAddButton) {
                                        IconButton(
                                            onClick = { topAppBarState.onAddClick?.invoke() },
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
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
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
                    updateBottomNavVisibility = { showBottomNav = it },
                    devicesViewModel = hiltViewModel(),
                    topAppBarController = topAppBarController,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                )
            }
        }
    }
}