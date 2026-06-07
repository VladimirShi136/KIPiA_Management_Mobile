package com.kipia.management.mobile.ui.screens.photos

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kipia.management.mobile.ui.theme.Dimens
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.ui.components.topappbar.TopAppBarController
import com.kipia.management.mobile.viewmodel.PhotoDetailViewModel

@Composable
fun FullScreenPhotoContent(
    device: Device?,
    photos: List<String>,
    photoIndex: Int,
    isLoading: Boolean,
    error: String?,
    photoDetailViewModel: PhotoDetailViewModel,
    topAppBarController: TopAppBarController,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        isLoading -> LoadingPhotoState()

        error != null -> {
            ErrorPhotoState(
                error = error,
                onRetry = onRetry,
                onNavigateBack = onNavigateBack
            )
        }

        device != null && photos.isNotEmpty() && photoIndex < photos.size -> {
            LaunchedEffect(device) {
                photoDetailViewModel.setCurrentDevice(device)
            }

            FullScreenPhotoScreen(
                initialPhotoPath = photos[photoIndex],
                photos = photos,
                initialIndex = photoIndex,
                device = device,
                onNavigateBack = onNavigateBack,
                viewModel = photoDetailViewModel,
                topAppBarController = topAppBarController
            )
        }

        else -> {
            ErrorPhotoState(
                error = "Фото не найдено",
                onRetry = onRetry,
                onNavigateBack = onNavigateBack
            )
        }
    }
}

@Composable
fun LoadingPhotoState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(Dimens.spacingLarge))
            Text("Загрузка фото...")
        }
    }
}

@Composable
fun ErrorPhotoState(
    error: String,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Dimens.spacingLarge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = "Ошибка",
            modifier = Modifier.size(Dimens.iconSizeXXLarge),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(Dimens.spacingLarge))
        Text("Ошибка", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(Dimens.spacingMedium))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Dimens.spacingXLarge))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingLarge)) {
            Button(onClick = onNavigateBack) { Text("Назад") }
            Button(onClick = onRetry) { Text("Повторить") }
        }
    }
}