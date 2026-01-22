package com.kipia.management.mobile.ui.screens.devices

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
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.ui.theme.DeviceStatus
import com.kipia.management.mobile.viewmodel.DeviceDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    deviceId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Int) -> Unit,
    updateBottomNavVisibility: (Boolean) -> Unit = {},
    viewModel: DeviceDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val device by viewModel.device.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle(initialValue = emptyList()) // ← ВОТ ОНО!

    // ★★★★ ОТКЛЮЧАЕМ BOTTOM NAVIGATION ★★★★
    LaunchedEffect(Unit) {
        updateBottomNavVisibility(false)
    }

    // ★★★★ ВОССТАНАВЛИВАЕМ ПРИ ВЫХОДЕ ★★★★
    DisposableEffect(Unit) {
        onDispose {
            updateBottomNavVisibility(true)
        }
    }

    // Загружаем устройство при входе на экран
    LaunchedEffect(deviceId) {
        if (deviceId > 0) {
            viewModel.loadDevice(deviceId)
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
                    // TODO: Открыть полноэкранный просмотр
                    println("Нажато фото с индексом: $index")
                },
                onShare = { viewModel.shareDeviceInfo() },
                onToggleFavorite = { viewModel.toggleFavorite() },
                onNavigateToEdit = { onNavigateToEdit(deviceId) },
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp) // ← Добавляем отступ сверху
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
    Column(
        modifier = modifier
    ) {
        // Информационная карточка
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Заголовок с инвентарным номером
                Text(
                    text = device.getDisplayName(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Инвентарный номер: ${device.inventoryNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                DeviceDetailSectionTitle("Основная информация")

                Spacer(modifier = Modifier.height(8.dp))

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

                // ★★★★ ДОБАВИМ КНОПКИ ДЕЙСТВИЙ ВНУТРИ КАРТОЧКИ ★★★★
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Кнопка "Редактировать"
                    OutlinedButton(
                        onClick = onNavigateToEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Редактировать")
                    }

                    // Кнопка "В избранное"
                    OutlinedButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp),
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        Text(if (isFavorite) "В избранном" else "В избранное")
                    }
                }
            }
        }

        // Дополнительная информация
        device.additionalInfo?.takeIf { it.isNotBlank() }?.let { info ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    DeviceDetailSectionTitle("Дополнительная информация")

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = info,
                        style = MaterialTheme.typography.bodyMedium,
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    DeviceDetailSectionTitle("Фотографии (${photos.size})")

                    Spacer(modifier = Modifier.height(8.dp))

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
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Кнопка "Поделиться"
            OutlinedButton(
                onClick = onShare,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Поделиться")
            }

            // Кнопка "QR код"
            OutlinedButton(
                onClick = { /* TODO: Генерация QR кода */ },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Filled.QrCode,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("QR код")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StatusBadge(status: String) {
    val deviceStatus = DeviceStatus.fromString(status)

    Surface(
        color = deviceStatus.backgroundColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = status,
            color = deviceStatus.textColor,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
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
            .padding(vertical = 6.dp)
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Показываем до 3 фото в ряд
        photos.chunked(3).forEach { rowPhotos ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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

                // Заполняем оставшиеся места пустыми
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
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Загрузка...")
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
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

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
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Прибор не найден",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Прибор был удален или произошла ошибка",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

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