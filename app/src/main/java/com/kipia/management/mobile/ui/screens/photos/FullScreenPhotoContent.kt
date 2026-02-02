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
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.viewmodel.PhotoDetailViewModel

@Composable
fun FullScreenPhotoContent(
    device: Device?,
    photos: List<String>,
    photoIndex: Int,
    isLoading: Boolean,
    error: String?,
    photoDetailViewModel: PhotoDetailViewModel,
    onNavigateBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        isLoading -> {
            LoadingPhotoState()
        }
        error != null -> {
            ErrorPhotoState(
                error = error,
                onRetry = onRetry,
                onNavigateBack = onNavigateBack
            )
        }
        device != null && photos.isNotEmpty() && photoIndex < photos.size -> {
            val photoPath = photos[photoIndex]

            LaunchedEffect(device) {
                photoDetailViewModel.setCurrentDevice(device)
            }

            FullScreenPhotoScreen(
                photoPath = photoPath,
                device = device,
                onNavigateBack = onNavigateBack,
                viewModel = photoDetailViewModel
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
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = "Ошибка",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ошибка",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = onNavigateBack) {
                Text("Назад")
            }

            Button(onClick = onRetry) {
                Text("Повторить")
            }
        }
    }
}