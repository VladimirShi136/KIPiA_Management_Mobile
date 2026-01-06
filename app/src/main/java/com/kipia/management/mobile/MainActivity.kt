package com.kipia.management.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.rememberNavController
import com.kipia.management.mobile.ui.navigation.BottomNavigationBar
import com.kipia.management.mobile.ui.navigation.KIPiANavHost
import com.kipia.management.mobile.ui.theme.KIPiATheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Устанавливаем тему Material 3
        setTheme(R.style.Theme_KipiaManagement)

        setContent {
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
            ) { paddingValues ->
                KIPiANavHost(
                    navController = navController,
                    modifier = Modifier.fillMaxSize(),
                    paddingValues = paddingValues
                )
            }
        }
    }
}