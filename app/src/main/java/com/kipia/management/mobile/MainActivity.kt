package com.kipia.management.mobile

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.kipia.management.mobile.managers.PhotoManager
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.repository.PreferencesRepository
import com.kipia.management.mobile.ui.components.dialogs.ErrorDialog
import com.kipia.management.mobile.ui.components.topappbar.KIPiATopAppBar
import com.kipia.management.mobile.ui.components.topappbar.rememberTopAppBarController
import com.kipia.management.mobile.ui.navigation.BottomNavigationBar
import com.kipia.management.mobile.ui.navigation.KIPiANavHost
import com.kipia.management.mobile.ui.shared.NotificationManager
import com.kipia.management.mobile.ui.theme.BottomNavColors
import com.kipia.management.mobile.ui.theme.KIPiATheme
import com.kipia.management.mobile.ui.theme.SystemColors
import com.kipia.management.mobile.viewmodel.PhotosViewModel
import com.kipia.management.mobile.viewmodel.SchemesViewModel
import com.kipia.management.mobile.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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
        setTheme(R.style.Theme_KipiaManagement)
        setupEdgeToEdge()
        setContent {
            KIPiAApp(
                notificationManager = notificationManager,
                photoManager = photoManager
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
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
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val systemUiController = rememberSystemUiController()
    val context = LocalContext.current

    // --- ЗАПРОС РАЗРЕШЕНИЯ НА УВЕДОМЛЕНИЯ ПРИ ЗАПУСКЕ ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Можно обработать результат, если нужно
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val isDarkTheme = when (themeMode) {
        PreferencesRepository.THEME_LIGHT -> false
        PreferencesRepository.THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }

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

    val (topAppBarBg, topAppBarContent) = topAppBarColors
    val statusBarColor = if (isDarkTheme) SystemColors.TopAppBar.DarkBackground else SystemColors.TopAppBar.LightBackground
    val navBarColor = if (isDarkTheme) SystemColors.BottomNav.DarkBackground else SystemColors.BottomNav.LightBackground
    val darkIcons = !isDarkTheme

    DisposableEffect(isDarkTheme, systemUiController) {
        systemUiController.setStatusBarColor(color = statusBarColor, darkIcons = darkIcons)
        systemUiController.setNavigationBarColor(color = navBarColor, darkIcons = darkIcons, navigationBarContrastEnforced = false)
        val activity = context as Activity
        activity.window.decorView.post {
            activity.window.statusBarColor = statusBarColor.toArgb()
            activity.window.navigationBarColor = navBarColor.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.window.insetsController?.setSystemBarsAppearance(
                    if (darkIcons) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                )
            }
        }
        onDispose {}
    }

    val photosState by photosViewModel.uiState.collectAsStateWithLifecycle()
    val photosDevices by photosViewModel.devices.collectAsStateWithLifecycle()
    val photosLocations by photosViewModel.allLocations.collectAsStateWithLifecycle()
    val schemesState by schemesViewModel.uiState.collectAsStateWithLifecycle()

    // --- ГЛОБАЛЬНЫЕ УВЕДОМЛЕНИЯ ---
    var globalNotification by remember { mutableStateOf<NotificationManager.Notification>(NotificationManager.Notification.None) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        notificationManager.notification.collect { notification ->
            when (notification) {
                is NotificationManager.Notification.Error -> {
                    errorMessage = notification.message
                    notificationManager.clearLastNotification()
                }
                is NotificationManager.Notification.SyncError -> {
                    errorMessage = notification.message
                    notificationManager.clearLastNotification()
                }
                NotificationManager.Notification.None -> { }
                else -> {
                    globalNotification = notification
                    delay(3000)
                    globalNotification = NotificationManager.Notification.None
                    notificationManager.clearLastNotification()
                }
            }
        }
    }

    KIPiATheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            var showBottomNav by rememberSaveable { mutableStateOf(true) }
            val topAppBarController = rememberTopAppBarController()
            
            val topAppBarState by topAppBarController.state

            LaunchedEffect(Unit) { topAppBarController.resetToDefault() }

            val onBackClick: () -> Unit = {
                topAppBarState.onBackClick?.invoke() ?: navController.navigateUp()
            }

            LaunchedEffect(navController) {
                navController.addOnDestinationChangedListener { _, destination, _ ->
                    val route = destination.route
                    showBottomNav = when {
                        route?.startsWith("device_edit") == true -> false
                        route?.startsWith("device_detail") == true -> false
                        route == "settings" -> false
                        route == "debug_settings" -> false
                        route?.startsWith("fullscreen_photo") == true -> false
                        route?.startsWith("scheme_editor") == true -> false
                        else -> true
                    }
                    
                    when (route) {
                        "devices" -> topAppBarController.resetToDefault()
                        "settings" -> topAppBarController.setForScreen("settings")
                        "photos" -> topAppBarController.setForScreen("photos", mapOf(
                            "locations" to photosLocations, 
                            "devices" to photosDevices,
                            "selectedLocation" to (photosState.selectedLocation ?: ""),
                            "selectedDeviceId" to (photosState.selectedDeviceId ?: 0)
                        ))
                        "schemes" -> topAppBarController.setForScreen("schemes", mapOf(
                            "title" to "Учет приборов КИПиА",
                            "searchQuery" to schemesState.searchQuery,
                            "currentSort" to schemesState.sortBy
                        ))
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        KIPiATopAppBar(
                            topAppBarState = topAppBarState,
                            topAppBarBg = topAppBarBg,
                            topAppBarContent = topAppBarContent,
                            onBackClick = onBackClick,
                            navController = navController,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                        )
                    },
                    bottomBar = {
                        AnimatedVisibility(
                            visible = showBottomNav, 
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), 
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .windowInsetsPadding(WindowInsets.navigationBars), 
                                color = bottomNavColors.background, 
                                shape = RectangleShape, 
                                tonalElevation = 4.dp
                            ) {
                                BottomNavigationBar(navController = navController, isDarkTheme = isDarkTheme)
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        KIPiANavHost(
                            navController = navController,
                            devicesViewModel = hiltViewModel(),
                            photosViewModel = photosViewModel,
                            schemesViewModel = schemesViewModel,
                            topAppBarController = topAppBarController,
                            notificationManager = notificationManager,
                            photoManager = photoManager,
                            updateBottomNavVisibility = { showBottomNav = it },
                            modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background).consumeWindowInsets(innerPadding)
                        )

                        errorMessage?.let { msg ->
                            ErrorDialog(
                                title = "Ошибка",
                                message = msg,
                                onDismiss = { errorMessage = null }
                            )
                        }

                        Box(
                            modifier = Modifier.fillMaxSize().padding(bottom = 200.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            AnimatedVisibility(
                                visible = globalNotification != NotificationManager.Notification.None,
                                enter = fadeIn() + slideInVertically(initialOffsetY = { 100 }),
                                exit = fadeOut() + slideOutVertically(targetOffsetY = { 100 })
                            ) {
                                val (color, icon, text) = when (val n = globalNotification) {
                                    is NotificationManager.Notification.DeviceSaved -> Triple(MaterialTheme.colorScheme.primary, Icons.Default.CheckCircle, "Прибор '${n.deviceName}' сохранен")
                                    is NotificationManager.Notification.SchemeSaved -> Triple(MaterialTheme.colorScheme.primary, Icons.Default.CheckCircle, "Схема '${n.schemeName}' сохранена")
                                    is NotificationManager.Notification.DeviceDeleted -> Triple(MaterialTheme.colorScheme.error, Icons.Default.Error, "Прибор '${n.deviceName}' удален")
                                    is NotificationManager.Notification.SyncSuccess -> Triple(MaterialTheme.colorScheme.primary, Icons.Default.CheckCircle, n.message)
                                    else -> Triple(MaterialTheme.colorScheme.primary, Icons.Default.CheckCircle, "")
                                }

                                if (text.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = color,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        shadowElevation = 8.dp,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(icon, null, modifier = Modifier.size(20.dp))
                                            Text(text = text, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
