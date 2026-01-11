package com.kipia.management.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.ui.navigation.BottomNavigationBar
import com.kipia.management.mobile.ui.navigation.KIPiANavHost
import com.kipia.management.mobile.ui.theme.KIPiATheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var deviceRepository: DeviceRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Устанавливаем тему Material 3
        setTheme(R.style.Theme_KipiaManagement)
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

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { androidx.compose.material3.Text(stringResource(R.string.app_name)) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                },
                bottomBar = {
                    BottomNavigationBar(navController = navController)
                }
            ) { innerPadding ->
                KIPiANavHost(
                    navController = navController,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}