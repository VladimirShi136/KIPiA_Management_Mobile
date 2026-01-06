package com.kipia.management.mobile.ui.screens.devices

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kipia.management.mobile.R
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.utils.PhotoManager
import com.kipia.management.mobile.viewmodel.DeviceEditUiState
import com.kipia.management.mobile.viewmodel.DeviceEditViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceEditScreen(
    deviceId: Int?,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: DeviceEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val device by viewModel.device.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Инициализация при загрузке
    LaunchedEffect(deviceId) {
        deviceId?.let { viewModel.loadDevice(it) }
    }

    // Обработка результатов
    LaunchedEffect(uiState) {
        when {
            uiState.isSuccess -> {
                onSaveSuccess()
            }
            uiState.error != null -> {
                snackbarHostState.showSnackbar(uiState.error)
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (deviceId == null) "Новый прибор" else "Редактирование"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (deviceId != null) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    viewModel.deleteDevice()
                                    onSaveSuccess()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.saveDevice() },
                enabled = uiState.isFormValid && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = "Сохранить")
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        DeviceEditForm(
            device = device,
            uiState = uiState,
            onTypeChanged = { viewModel.updateDevice { it.copy(type = it) } },
            onNameChanged = { viewModel.updateDevice { it.copy(name = it) } },
            onManufacturerChanged = { viewModel.updateDevice { it.copy(manufacturer = it) } },
            onInventoryNumberChanged = { viewModel.updateDevice { it.copy(inventoryNumber = it) } },
            onYearChanged = { viewModel.updateDevice { it.copy(year = it.toIntOrNull()) } },
            onMeasurementLimitChanged = { viewModel.updateDevice { it.copy(measurementLimit = it) } },
            onAccuracyClassChanged = { viewModel.updateDevice { it.copy(accuracyClass = it.toDoubleOrNull()) } },
            onLocationChanged = { viewModel.updateDevice { it.copy(location = it) } },
            onValveNumberChanged = { viewModel.updateDevice { it.copy(valveNumber = it) } },
            onStatusChanged = { viewModel.updateDevice { it.copy(status = it) } },
            onAdditionalInfoChanged = { viewModel.updateDevice { it.copy(additionalInfo = it) } },
            onPhotoSelected = { uri ->
                scope.launch {
                    val photoPath = PhotoManager.savePhotoFromUri(context, uri)
                    photoPath?.let { path ->
                        val updatedPhotos = device.getPhotoList() + path
                        viewModel.updateDevice { it.setPhotoList(updatedPhotos) }
                    }
                }
            },
            onMainPhotoSelected = { uri ->
                scope.launch {
                    val photoPath = PhotoManager.savePhotoFromUri(context, uri)
                    photoPath?.let { path ->
                        viewModel.updateDevice { it.copy(photoPath = path) }
                    }
                }
            },
            onPhotoDeleted = { photoIndex ->
                val updatedPhotos = device.getPhotoList().toMutableList().apply {
                    removeAt(photoIndex)
                }
                viewModel.updateDevice { it.setPhotoList(updatedPhotos) }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceEditForm(
    device: Device,
    uiState: DeviceEditUiState,
    onTypeChanged: (String) -> Unit,
    onNameChanged: (String) -> Unit,
    onManufacturerChanged: (String) -> Unit,
    onInventoryNumberChanged: (String) -> Unit,
    onYearChanged: (String) -> Unit,
    onMeasurementLimitChanged: (String) -> Unit,
    onAccuracyClassChanged: (String) -> Unit,
    onLocationChanged: (String) -> Unit,
    onValveNumberChanged: (String) -> Unit,
    onStatusChanged: (String) -> Unit,
    onAdditionalInfoChanged: (String) -> Unit,
    onPhotoSelected: (Uri) -> Unit,
    onMainPhotoSelected: (Uri) -> Unit,
    onPhotoDeleted: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(onPhotoSelected)
    }

    val mainPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(onMainPhotoSelected)
    }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Основное фото
        SectionTitle("Основное фото")
        MainPhotoSection(
            photoPath = device.photoPath,
            onSelectPhoto = {
                mainPhotoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )

        // Основная информация
        SectionTitle("Основная информация")

        // Тип прибора (выпадающий список)
        ExposedDropdownMenuBox(
            expanded = uiState.isTypeExpanded,
            onExpandedChange = { uiState.isTypeExpanded = it }
        ) {
            OutlinedTextField(
                value = device.type,
                onValueChange = onTypeChanged,
                label = { Text("Тип прибора *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isTypeExpanded)
                },
                isError = uiState.typeError != null
            )

            ExposedDropdownMenu(
                expanded = uiState.isTypeExpanded,
                onDismissRequest = { uiState.isTypeExpanded = false }
            ) {
                Device.TYPES.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            onTypeChanged(type)
                            uiState.isTypeExpanded = false
                        }
                    )
                }
            }
        }

        if (uiState.typeError != null) {
            Text(
                text = uiState.typeError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
        }

        OutlinedTextField(
            value = device.name ?: "",
            onValueChange = onNameChanged,
            label = { Text("Наименование") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = device.manufacturer ?: "",
            onValueChange = onManufacturerChanged,
            label = { Text("Производитель") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = device.inventoryNumber,
            onValueChange = onInventoryNumberChanged,
            label = { Text("Инвентарный номер *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = uiState.inventoryNumberError != null
        )

        if (uiState.inventoryNumberError != null) {
            Text(
                text = uiState.inventoryNumberError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = device.year?.toString() ?: "",
                onValueChange = onYearChanged,
                label = { Text("Год выпуска") },
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number,
                singleLine = true
            )

            OutlinedTextField(
                value = device.measurementLimit ?: "",
                onValueChange = onMeasurementLimitChanged,
                label = { Text("Предел измерений") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = device.accuracyClass?.toString() ?: "",
                onValueChange = onAccuracyClassChanged,
                label = { Text("Класс точности") },
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number,
                singleLine = true
            )

            OutlinedTextField(
                value = device.valveNumber ?: "",
                onValueChange = onValveNumberChanged,
                label = { Text("Номер вентиля") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        // Место установки и статус
        SectionTitle("Место и статус")

        OutlinedTextField(
            value = device.location,
            onValueChange = onLocationChanged,
            label = { Text("Место установки *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = uiState.locationError != null
        )

        if (uiState.locationError != null) {
            Text(
                text = uiState.locationError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
        }

        // Статус (выпадающий список)
        ExposedDropdownMenuBox(
            expanded = uiState.isStatusExpanded,
            onExpandedChange = { uiState.isStatusExpanded = it }
        ) {
            OutlinedTextField(
                value = device.status,
                onValueChange = onStatusChanged,
                label = { Text("Статус") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = uiState.isStatusExpanded)
                }
            )

            ExposedDropdownMenu(
                expanded = uiState.isStatusExpanded,
                onDismissRequest = { uiState.isStatusExpanded = false }
            ) {
                Device.STATUSES.forEach { status ->
                    DropdownMenuItem(
                        text = { Text(status) },
                        onClick = {
                            onStatusChanged(status)
                            uiState.isStatusExpanded = false
                        }
                    )
                }
            }
        }

        // Дополнительные фото
        SectionTitle("Дополнительные фото")
        PhotoGallerySection(
            photos = device.getPhotoList(),
            onAddPhoto = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDeletePhoto = onPhotoDeleted
        )

        // Дополнительная информация
        SectionTitle("Дополнительная информация")
        OutlinedTextField(
            value = device.additionalInfo ?: "",
            onValueChange = onAdditionalInfoChanged,
            label = { Text("Примечания") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5
        )

        // Валидация формы
        if (!uiState.isFormValid) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Ошибка",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Заполните обязательные поля (отмечены *)",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun MainPhotoSection(
    photoPath: String?,
    onSelectPhoto: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        if (photoPath != null) {
            AsyncImage(
                model = photoPath,
                contentDescription = "Основное фото",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "Добавить фото",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Добавить основное фото",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Кнопка добавления/изменения фото
        IconButton(
            onClick = onSelectPhoto,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            Icon(
                if (photoPath != null) Icons.Default.Edit else Icons.Default.Add,
                contentDescription = if (photoPath != null) "Изменить фото" else "Добавить фото",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun PhotoGallerySection(
    photos: List<String>,
    onAddPhoto: () -> Unit,
    onDeletePhoto: (Int) -> Unit
) {
    Column {
        if (photos.isEmpty()) {
            Card(
                onClick = onAddPhoto,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "Добавить фото",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Добавить фото",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                photos.forEachIndexed { index, photoPath ->
                    Box(modifier = Modifier.weight(1f)) {
                        AsyncImage(
                            model = photoPath,
                            contentDescription = "Фото $index",
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(MaterialTheme.shapes.medium)
                        )

                        // Кнопка удаления фото
                        IconButton(
                            onClick = { onDeletePhoto(index) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Удалить фото",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Кнопка добавления нового фото
                if (photos.size < 10) { // Ограничение на количество фото
                    Card(
                        onClick = onAddPhoto,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Добавить фото",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Text(
                text = "Фото: ${photos.size}/10",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}