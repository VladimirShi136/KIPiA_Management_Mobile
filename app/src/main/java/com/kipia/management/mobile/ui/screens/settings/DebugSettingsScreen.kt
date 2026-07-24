package com.kipia.management.mobile.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.navigation.NavController
import com.kipia.management.mobile.ui.components.topappbar.TopAppBarController
import com.kipia.management.mobile.ui.theme.Dimens

/**
 * Экран для разработчиков.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugSettingsScreen(
    navController: NavController,
    topAppBarController: TopAppBarController,
    updateBottomNavVisibility: (Boolean) -> Unit = {}
) {
    LaunchedEffect(Unit) {
        updateBottomNavVisibility(false)
        topAppBarController.setForScreen(
            "debug_settings",
            mapOf(
                "title" to "Инженерное меню",
                "showBackButton" to true,
                "onBackClick" to { navController.popBackStack() }
            )
        )
    }
    
    DisposableEffect(Unit) {
        onDispose { }
    }

    val scrollState = rememberScrollState()
    val cardModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = Dimens.screenPadding)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = Dimens.spacingMedium),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)
    ) {
        Card(modifier = cardModifier) {
            Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                Text(
                    text = "Функции обслуживания",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = Dimens.spacingMedium)
                )
                
                ListItem(
                    headlineContent = { Text("Очистка мягкоудаленных файлов") },
                    supportingContent = { Text("Будет добавлено позже") },
                    leadingContent = { Icon(Icons.Default.DeleteSweep, null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.cardRadius))
                        .clickable { /* Будет добавлено позже */ }
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.spacingSmall))
                
                ListItem(
                    headlineContent = { Text("Просмотр всех фото в папке") },
                    supportingContent = { Text("Будет добавлено позже") },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.cardRadius))
                        .clickable { /* Будет добавлено позже */ }
                )
            }
        }
        
        Card(modifier = cardModifier) {
            Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                Text(
                    text = "База данных",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = Dimens.spacingMedium)
                )
                
                ListItem(
                    headlineContent = { Text("Проверка целостности") },
                    leadingContent = { Icon(Icons.Default.Storage, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.cardRadius))
                        .clickable { /* Будет добавлено позже */ }
                )
            }
        }
    }
}
