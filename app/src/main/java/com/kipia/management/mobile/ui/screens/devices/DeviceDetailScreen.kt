package com.kipia.management.mobile.ui.screens.devices

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kipia.management.mobile.ui.theme.Dimens
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.ui.theme.DeviceStatus
import com.kipia.management.mobile.ui.components.topappbar.TopAppBarController
import com.kipia.management.mobile.viewmodel.DeviceDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    deviceId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    onNavigateToPhotos: (Int, Device) -> Unit,
    viewModel: DeviceDetailViewModel = hiltViewModel(),
    topAppBarController: TopAppBarController? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val device by viewModel.device.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle(initialValue = emptyList())

    // Загружаем устройство при входе на экран
    LaunchedEffect(deviceId) {
        if (deviceId > 0) {
            viewModel.loadDevice(deviceId)
        }
    }

    // Настройка TopAppBar (исправлены ключи колбэков)
    LaunchedEffect(device) {
        device?.let {
            topAppBarController?.setForScreen(
                screenRoute = "device_detail",
                additionalParams = mapOf(
                    "onEditClick" to { onNavigateToEdit(deviceId) },
                    "onBackClick" to onNavigateBack
                )
            )
        }
    }

    when {
        uiState.isLoading -> {
            DeviceDetailLoadingState()
        }
        uiState.error != null -> {
            DeviceDetailErrorState(
                error = uiState.error ?: "Неизвестная ошибка",
                onRetry = { viewModel.loadDevice(deviceId) },
                modifier = Modifier.fillMaxSize()
            )
        }
        device != null -> {
            DeviceDetailContent(
                device = device!!,
                photos = photos,
                isFavorite = uiState.isFavorite,
                onPhotoClick = { index ->
                    onNavigateToPhotos(index, device!!)
                },
                onShare = { viewModel.shareDeviceInfo() },
                onToggleFavorite = { viewModel.toggleFavorite() },
                onNavigateToEdit = {
                    onNavigateToEdit(deviceId)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.screenPadding)
            )
        }
        else -> {
            DeviceDetailEmptyState(
                onNavigateBack = onNavigateBack,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun DeviceDetailContent(
    device: Device,
    photos: List<String>,
    isFavorite: Boolean,
    onPhotoClick: (Int) -> Unit,
    onShare: () -> Unit,
    onToggleFavorite: () -> Unit,
    onNavigateToEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = modifier
    ) {
        // Информационная карточка
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Dimens.spacingMedium),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(Dimens.cardPadding)
            ) {
                // Заголовок с инвентарным номером
                Text(

                    text = device.getDisplayName(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Dimens.spacingSmall))

                Text(
                    text = "Инвентарный номер: ${device.inventoryNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Dimens.spacingSmall))

                Text(
                    text = "Обновлено: ${if (device.updatedAt > 0) dateFormat.format(Date(device.updatedAt)) else "неизвестно"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Синхронизировано: ${if (device.lastSyncedAt > 0) dateFormat.format(Date(device.lastSyncedAt)) else "никогда"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Dimens.spacingLarge))

                // Статус с цветным индикатором
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(status = device.status)
                }
            }
        }

        // Основная информация
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Dimens.spacingMedium),
        ) {
            Column(
                modifier = Modifier.padding(Dimens.cardPadding)
            ) {
                DeviceDetailSectionTitle("Основная информация")

                Spacer(modifier = Modifier.height(Dimens.spacingMedium))

                DeviceDetailRow(
                    label = "Тип прибора:",
                    value = device.type
                )

                device.manufacturer?.let { manufacturer ->
                    DeviceDetailRow(
                        label = "Производитель:",
                        value = manufacturer
                    )
                }

                device.year?.let { year ->
                    DeviceDetailRow(
                        label = "Год выпуска:",
                        value = year.toString()
                    )
                }

                device.measurementLimit?.let { limit ->
                    DeviceDetailRow(
                        label = "Предел измерений:",
                        value = limit
                    )
                }

                device.accuracyClass?.let { accuracy ->
                    DeviceDetailRow(
                        label = "Класс точности:",
                        value = accuracy.toString()
                    )
                }

                DeviceDetailRow(
                    label = "Место установки:",
                    value = device.location
                )

                device.valveNumber?.let { valve ->
                    DeviceDetailRow(
                        label = "Номер крана:",
                        value = valve
                    )
                }
            }
        }

        // Дополнительная информация
        device.additionalInfo?.takeIf { it.isNotBlank() }?.let { info ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.spacingMedium)
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.cardPadding)
                ) {
                    DeviceDetailSectionTitle("Дополнительная информация")

                    Spacer(modifier = Modifier.height(Dimens.spacingMedium))

                    Text(
                        text = info,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Галерея фото
        if (photos.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.spacingMedium),
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.cardPadding)
                ) {
                    DeviceDetailSectionTitle("Фотографии (${photos.size})")

                    Spacer(modifier = Modifier.height(Dimens.spacingMedium))

                    DevicePhotoGallery(
                        photos = photos,
                        onPhotoClick = onPhotoClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Кнопки действий внизу
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingLarge)
        ) {
            // Кнопка "Поделиться"
            OutlinedButton(
                onClick = onShare,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = null,
                    modifier = Modifier.padding(end = Dimens.spacingMedium)
                )
                Text("Поделиться",
                    color = MaterialTheme.colorScheme.primary)
            }

            // Кнопка "QR код"
            OutlinedButton(
                onClick = { /* TODO: Генерация QR кода */ },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Filled.QrCode,
                    contentDescription = null,
                    modifier = Modifier.padding(end = Dimens.spacingMedium)
                )
                Text("QR код",
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val deviceStatus = DeviceStatus.fromString(status)

    Surface(
        color = deviceStatus.containerColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(horizontal = Dimens.spacingMedium),
        border = BorderStroke(
            width = 1.dp,
            color = deviceStatus.color.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = status,
            color = deviceStatus.textColor,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(horizontal = Dimens.spacingFab, vertical = Dimens.spacingSmall)
        )
    }
}

@Composable
fun DeviceDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.spacingMedium)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(140.dp)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun DevicePhotoGallery(
    photos: List<String>,
    onPhotoClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)
    ) {
        photos.chunked(3).forEach { rowPhotos ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowPhotos.forEachIndexed { _, photo ->
                    val photoIndex = photos.indexOf(photo)
                    DevicePhotoThumbnail(
                        photoPath = photo,
                        onClick = { onPhotoClick(photoIndex) },
                        modifier = Modifier.weight(1f)
                    )
                }

                repeat(3 - rowPhotos.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun DevicePhotoThumbnail(
    photoPath: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = MaterialTheme.shapes.medium
    ) {
        AsyncImage(
            model = photoPath,
            contentDescription = "Фото прибора",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun DeviceDetailLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(Dimens.iconSizeXLarge)
            )
            Spacer(modifier = Modifier.height(Dimens.spacingLarge))
            Text(
                "Загрузка...",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DeviceDetailErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Error,
            contentDescription = "Ошибка",
            modifier = Modifier.size(Dimens.iconSizeXXLarge),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(Dimens.spacingLarge))

        Text(
            text = "Ошибка",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(Dimens.spacingMedium))

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Dimens.spacingXXLarge)
        )

        Spacer(modifier = Modifier.height(Dimens.spacingXLarge))

        Button(onClick = onRetry) {
            Text("Повторить")
        }
    }
}

@Composable
fun DeviceDetailEmptyState(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.SearchOff,
            contentDescription = "Не найдено",
            modifier = Modifier.size(Dimens.iconSizeXXLarge),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Dimens.spacingLarge))

        Text(
            text = "Прибор не найден",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Dimens.spacingMedium))

        Text(
            text = "Прибор был удален или произошла ошибка",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Dimens.spacingXXLarge)
        )

        Spacer(modifier = Modifier.height(Dimens.spacingXLarge))

        Button(onClick = onNavigateBack) {
            Text("Вернуться")
        }
    }
}

@Composable
fun DeviceDetailSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
    )
}