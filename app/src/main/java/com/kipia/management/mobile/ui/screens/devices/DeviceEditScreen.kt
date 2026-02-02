package com.kipia.management.mobile.ui.screens.devices

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.ui.components.topappbar.TopAppBarController
import com.kipia.management.mobile.ui.shared.NotificationManager
import com.kipia.management.mobile.utils.PhotoManager
import com.kipia.management.mobile.viewmodel.DeviceEditViewModel
import com.kipia.management.mobile.viewmodel.DeviceDeleteViewModel
import kotlinx.coroutines.launch

@Composable
fun DeviceEditScreen(
    deviceId: Int?,
    onNavigateBack: () -> Unit,
    topAppBarController: TopAppBarController,
    viewModel: DeviceEditViewModel = hiltViewModel(),
    notificationManager: NotificationManager,
    deleteViewModel: DeviceDeleteViewModel = hiltViewModel(),
    photoManager: PhotoManager // ✅ ИСПРАВЛЕНО: убираем hiltViewModel() так как PhotoManager не ViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val device by viewModel.device.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Устанавливаем колбэки для TopAppBar
    LaunchedEffect(viewModel) {
        topAppBarController.updateState(
            topAppBarController.state.value.copy(
                onSaveClick = {
                    println("DEBUG: Сохранение вызвано из TopAppBar")
                    viewModel.saveDevice()
                },
                onDeleteClick = {
                    println("DEBUG: Удаление вызвано из TopAppBar")
                    scope.launch {
                        deleteViewModel.checkAndShowDialog(device)
                    }
                }
            )
        )
    }

    // Обработчик состояния
    LaunchedEffect(uiState) {
        when {
            uiState.isSaved -> {
                println("DEBUG: Устройство сохранено - возвращаемся назад")
                onNavigateBack()
            }

            uiState.isDeleted -> {
                println("DEBUG: Устройство удалено")
                onNavigateBack()
            }

            uiState.error != null -> {
                scope.launch {
                    snackbarHostState.showSnackbar("Ошибка: ${uiState.error}")
                }
                viewModel.clearError()
            }
        }
    }

    // Сбрасываем колбэки при выходе
    DisposableEffect(Unit) {
        onDispose {
            topAppBarController.updateState(
                topAppBarController.state.value.copy(
                    onSaveClick = null,
                    onDeleteClick = null
                )
            )
        }
    }

    // Инициализация
    LaunchedEffect(key1 = deviceId) {
        println("DEBUG DeviceEditScreen: инициализация, deviceId=$deviceId")
        if (deviceId != null && deviceId > 0) {
            viewModel.loadDevice(deviceId)
        } else {
            // Для нового устройства
            viewModel.updateDevice {
                Device.createEmpty().copy(
                    type = "Манометр",
                    status = "В работе"
                )
            }
        }
    }

    // Диалог удаления из deleteViewModel
    val deleteDialogData by deleteViewModel.showDeleteDialog.collectAsStateWithLifecycle()

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Форма редактирования
        DeviceEditForm(
            device = device,
            uiState = uiState,
            onTypeChanged = { type ->
                println("DEBUG: onTypeChanged called with: '$type'")
                viewModel.updateDevice { it.copy(type = type) }
            },
            onNameChanged = { name ->
                println("DEBUG: onNameChanged called with: '$name'")
                viewModel.updateDevice { it.copy(name = name) }
            },
            onManufacturerChanged = { manufacturer ->
                println("DEBUG: onManufacturerChanged called with: '$manufacturer'")
                viewModel.updateDevice { it.copy(manufacturer = manufacturer) }
            },
            onInventoryNumberChanged = { inventoryNumber ->
                println("DEBUG: onInventoryNumberChanged called with: '$inventoryNumber'")
                viewModel.updateDevice { it.copy(inventoryNumber = inventoryNumber) }
            },
            onYearChanged = { yearStr ->
                println("DEBUG: onYearChanged called with: '$yearStr'")
                val year = yearStr.takeIf { it.isNotBlank() }?.toIntOrNull()
                viewModel.updateDevice { it.copy(year = year) }
            },
            onMeasurementLimitChanged = { measurementLimit ->
                println("DEBUG: onMeasurementLimitChanged called with: '$measurementLimit'")
                viewModel.updateDevice { it.copy(measurementLimit = measurementLimit) }
            },
            onAccuracyClassChanged = { accuracyStr ->
                println("DEBUG: onAccuracyClassChanged called with: '$accuracyStr'")
                val accuracy = accuracyStr.takeIf { it.isNotBlank() }?.toDoubleOrNull()
                viewModel.updateDevice { it.copy(accuracyClass = accuracy) }
            },
            onLocationChanged = { location ->
                println("DEBUG: onLocationChanged called with: '$location'")
                viewModel.updateDevice { it.copy(location = location) }
            },
            onValveNumberChanged = { valveNumber ->
                println("DEBUG: onValveNumberChanged called with: '$valveNumber'")
                viewModel.updateDevice { it.copy(valveNumber = valveNumber) }
            },
            onStatusChanged = { status ->
                println("DEBUG: onStatusChanged called with: '$status'")
                viewModel.updateDevice { it.copy(status = status) }
            },
            onAdditionalInfoChanged = { additionalInfo ->
                println("DEBUG: onAdditionalInfoChanged called with: '$additionalInfo'")
                viewModel.updateDevice { it.copy(additionalInfo = additionalInfo) }
            },
            onPhotoSelected = { uri ->
                scope.launch {
                    // ✅ ИСПРАВЛЕНО: используем Device напрямую
                    val currentDevice = device ?: return@launch
                    val result = photoManager.savePhotoForDevice(currentDevice, uri)

                    result.onSuccess { photoResult ->
                        // Обновляем устройство с новым фото
                        viewModel.updateDevice {
                            it.copy(photos = photoResult.device.photos)
                        }
                    }.onFailure { error ->
                        scope.launch {
                            snackbarHostState.showSnackbar("Ошибка сохранения фото: ${error.message}")
                        }
                    }
                }
            },
            onPhotoDeleted = { photoIndex ->
                device?.let { currentDevice ->
                    val currentPhotos = currentDevice.photos

                    if (currentPhotos.isNotEmpty() && photoIndex < currentPhotos.size) {
                        scope.launch {
                            val fileName = currentPhotos[photoIndex]
                            val success = photoManager.deleteDevicePhoto(currentDevice, fileName)

                            if (success) {
                                val updatedPhotos = currentPhotos.toMutableList().apply {
                                    removeAt(photoIndex)
                                }
                                viewModel.updateDevice {
                                    it.copy(photos = updatedPhotos)
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Не удалось удалить фото")
                                }
                            }
                        }
                    }
                }
            },
            photoManager = photoManager, // ✅ ИСПРАВЛЕНО: передаем как параметр
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        )

        // Диалог удаления
        deleteDialogData?.let { dialogData ->
            DeviceDeleteDialog(
                device = dialogData.device,
                scheme = dialogData.scheme,
                deviceCountInLocation = dialogData.deviceCountInLocation,
                isLastInLocation = dialogData.isLastInLocation,
                onDismiss = {
                    deleteViewModel.dismissDialog()
                },
                onConfirm = { deleteScheme ->
                    scope.launch {
                        try {
                            viewModel.deleteDevice(deleteScheme)
                            deleteViewModel.dismissDialog()
                        } catch (e: Exception) {
                            deleteViewModel.dismissDialog()
                            snackbarHostState.showSnackbar("Ошибка удаления: ${e.message}")
                        }
                    }
                }
            )
        }

        // Snackbar для уведомлений
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceEditForm(
    device: Device?,
    uiState: com.kipia.management.mobile.viewmodel.DeviceEditUiState,
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
    onPhotoDeleted: (Int) -> Unit,
    photoManager: PhotoManager, // ✅ ИСПРАВЛЕНО: получаем как параметр
    modifier: Modifier = Modifier,
    viewModel: DeviceEditViewModel = hiltViewModel()
) {
    val safeDevice = device ?: Device.createEmpty()

    var isTypeExpanded by remember { mutableStateOf(false) }
    var isStatusExpanded by remember { mutableStateOf(false) }

    // Используем StateFlow из ViewModel
    val isLocationDropdownExpanded by viewModel.isLocationDropdownExpanded.collectAsStateWithLifecycle()
    val allLocations by viewModel.allLocations.collectAsStateWithLifecycle()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(onPhotoSelected)
    }

    // ✅ ИСПРАВЛЕНО: получаем полные пути к фото
    val photoPaths = remember(safeDevice, photoManager) {
        safeDevice.photos.mapNotNull { fileName ->
            photoManager.getFullPhotoPath(safeDevice, fileName)
        }.filter { path -> path != null && path.isNotBlank() }
    }

    // Локальные состояния для полей ввода
    var typeText by remember { mutableStateOf(safeDevice.type) }
    var inventoryNumberText by remember { mutableStateOf(safeDevice.inventoryNumber) }
    var locationText by remember { mutableStateOf(safeDevice.location) }
    var nameText by remember { mutableStateOf(safeDevice.name ?: "") }
    var manufacturerText by remember { mutableStateOf(safeDevice.manufacturer ?: "") }
    var yearText by remember { mutableStateOf(safeDevice.year?.toString() ?: "") }
    var measurementLimitText by remember { mutableStateOf(safeDevice.measurementLimit ?: "") }
    var accuracyClassText by remember { mutableStateOf(safeDevice.accuracyClass?.toString() ?: "") }
    var valveNumberText by remember { mutableStateOf(safeDevice.valveNumber ?: "") }
    var statusText by remember { mutableStateOf(safeDevice.status) }
    var additionalInfoText by remember { mutableStateOf(safeDevice.additionalInfo ?: "") }

    // Обновляем локальные состояния при изменении device
    LaunchedEffect(safeDevice) {
        typeText = safeDevice.type
        inventoryNumberText = safeDevice.inventoryNumber
        locationText = safeDevice.location
        nameText = safeDevice.name ?: ""
        manufacturerText = safeDevice.manufacturer ?: ""
        yearText = safeDevice.year?.toString() ?: ""
        measurementLimitText = safeDevice.measurementLimit ?: ""
        accuracyClassText = safeDevice.accuracyClass?.toString() ?: ""
        valveNumberText = safeDevice.valveNumber ?: ""
        statusText = safeDevice.status
        additionalInfoText = safeDevice.additionalInfo ?: ""
    }

    Column(
        modifier = modifier.padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Основное фото
        DeviceEditSectionTitle("Основное фото")
        DeviceEditMainPhotoSection(
            photoPaths = photoPaths, // ✅ ИСПРАВЛЕНО
            onSelectPhoto = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )

        // Основная информация
        DeviceEditSectionTitle("Основная информация")

        // Тип прибора (простое текстовое поле)
        OutlinedTextField(
            value = typeText,
            onValueChange = { newValue ->
                typeText = newValue
                onTypeChanged(newValue)
            },
            label = {
                Text(
                    "Тип прибора *",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            singleLine = true,
            isError = uiState.typeError != null,
            placeholder = {
                Text("Например: Манометр, Термометр и т.д.")
            }
        )

        uiState.typeError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        OutlinedTextField(
            value = nameText,
            onValueChange = { newValue ->
                nameText = newValue
                onNameChanged(newValue)
            },
            label = { Text("Наименование", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = manufacturerText,
            onValueChange = { newValue ->
                manufacturerText = newValue
                onManufacturerChanged(newValue)
            },
            label = { Text("Производитель") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = inventoryNumberText,
            onValueChange = { newValue ->
                inventoryNumberText = newValue
                onInventoryNumberChanged(newValue)
            },
            label = { Text("Инвентарный номер *") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            singleLine = true,
            isError = uiState.inventoryNumberError != null
        )

        uiState.inventoryNumberError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        OutlinedTextField(
            value = yearText,
            onValueChange = { newValue ->
                yearText = newValue
                onYearChanged(newValue)
            },
            label = {
                Text(
                    "Год выпуска",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number
            ),
            singleLine = true
        )

        OutlinedTextField(
            value = measurementLimitText,
            onValueChange = { newValue ->
                measurementLimitText = newValue
                onMeasurementLimitChanged(newValue)
            },
            label = {
                Text(
                    "Предел измерений",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = accuracyClassText,
            onValueChange = { newValue ->
                accuracyClassText = newValue
                onAccuracyClassChanged(newValue)
            },
            label = {
                Text(
                    "Класс точности",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number
            ),
            singleLine = true
        )

        OutlinedTextField(
            value = valveNumberText,
            onValueChange = { newValue ->
                valveNumberText = newValue
                onValveNumberChanged(newValue)
            },
            label = {
                Text(
                    "Номер крана",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Место установки и статус
        DeviceEditSectionTitle("Место и статус")

        // Поле местоположения с выпадающим списком
        ExposedDropdownMenuBox(
            expanded = isLocationDropdownExpanded,
            onExpandedChange = { expanded ->
                if (expanded) {
                    viewModel.expandLocationDropdown()
                } else {
                    viewModel.collapseLocationDropdown()
                }
            }
        ) {
            OutlinedTextField(
                value = locationText,
                onValueChange = { newValue ->
                    locationText = newValue
                    onLocationChanged(newValue)
                },
                label = { Text("Место установки *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .padding(vertical = 4.dp),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = isLocationDropdownExpanded
                    )
                },
                isError = uiState.locationError != null,
                placeholder = {
                    if (allLocations.isNotEmpty()) {
                        Text("Введите или выберите из списка...")
                    }
                }
            )

            ExposedDropdownMenu(
                expanded = isLocationDropdownExpanded && allLocations.isNotEmpty(),
                onDismissRequest = { viewModel.collapseLocationDropdown() }
            ) {
                allLocations.forEach { location ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = location, // ✅ ИСПРАВЛЕНО: добавляем параметр text
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            locationText = location
                            onLocationChanged(location)
                            viewModel.collapseLocationDropdown()
                        }
                    )
                }
            }
        }

        uiState.locationError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        // Статус (выпадающий список)
        ExposedDropdownMenuBox(
            expanded = isStatusExpanded,
            onExpandedChange = { isStatusExpanded = it }
        ) {
            OutlinedTextField(
                value = statusText,
                onValueChange = { newValue ->
                    statusText = newValue
                    onStatusChanged(newValue)
                },
                label = { Text("Статус") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .padding(vertical = 4.dp),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = isStatusExpanded
                    )
                }
            )

            ExposedDropdownMenu(
                expanded = isStatusExpanded,
                onDismissRequest = { isStatusExpanded = false }
            ) {
                // ✅ ИСПРАВЛЕНО: используем STATUSES из компаньона
                Device.Companion.STATUSES.forEach { status ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = status, // ✅ ИСПРАВЛЕНО: добавляем параметр text
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            statusText = status
                            onStatusChanged(status)
                            isStatusExpanded = false
                        }
                    )
                }
            }
        }

        // Дополнительные фото
        DeviceEditSectionTitle("Дополнительные фото")
        DeviceEditPhotoGallerySection(
            photoPaths = photoPaths, // ✅ ИСПРАВЛЕНО
            onAddPhoto = {
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDeletePhoto = onPhotoDeleted
        )

        // Дополнительная информация
        DeviceEditSectionTitle("Дополнительная информация")
        OutlinedTextField(
            value = additionalInfoText,
            onValueChange = { newValue ->
                additionalInfoText = newValue
                onAdditionalInfoChanged(newValue)
            },
            label = { Text("Примечания") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(vertical = 4.dp),
            singleLine = false,
            maxLines = 5
        )

        // Валидация формы
        if (!uiState.isFormValid && uiState.validationErrors.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Ошибка",
                        modifier = Modifier.padding(end = 8.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Заполните обязательные поля (отмечены *): " +
                                uiState.validationErrors.joinToString(", ") {
                                    when (it) {
                                        "type" -> "Тип прибора"
                                        "inventoryNumber" -> "Инвентарный номер"
                                        "location" -> "Место установки"
                                        else -> it
                                    }
                                },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceEditSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun DeviceEditMainPhotoSection(
    photoPaths: List<String>, // ✅ ИСПРАВЛЕНО
    onSelectPhoto: () -> Unit
) {
    val mainPhoto = photoPaths.firstOrNull()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (mainPhoto != null) {
                AsyncImage(
                    model = mainPhoto,
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
                    .padding(8.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    if (mainPhoto != null) Icons.Default.Edit else Icons.Default.Add,
                    contentDescription = if (mainPhoto != null) "Изменить фото" else "Добавить фото",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun DeviceEditPhotoGallerySection(
    photoPaths: List<String>, // ✅ ИСПРАВЛЕНО
    onAddPhoto: () -> Unit,
    onDeletePhoto: (Int) -> Unit
) {
    Column {
        if (photoPaths.isEmpty()) {
            Card(
                onClick = onAddPhoto,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
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
                photoPaths.forEachIndexed { index, photoPath ->
                    Box(modifier = Modifier.weight(1f)) {
                        Card(
                            modifier = Modifier.aspectRatio(1f),
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            AsyncImage(
                                model = photoPath,
                                contentDescription = "Фото $index",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Кнопка удаления фото
                        IconButton(
                            onClick = { onDeletePhoto(index) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Удалить фото",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Кнопка добавления нового фото
                if (photoPaths.size < 10) {
                    Card(
                        onClick = onAddPhoto,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Добавить фото",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Text(
                text = "Фото: ${photoPaths.size}/10",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}