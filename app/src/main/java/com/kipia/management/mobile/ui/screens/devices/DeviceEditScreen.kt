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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.managers.CameraManager
import com.kipia.management.mobile.managers.PhotoManager
import com.kipia.management.mobile.ui.components.topappbar.TopAppBarController
import com.kipia.management.mobile.ui.shared.NotificationManager
import com.kipia.management.mobile.viewmodel.DeviceDeleteViewModel
import com.kipia.management.mobile.viewmodel.DeviceEditViewModel
import kotlinx.coroutines.launch

// =============================================================================
// DeviceEditScreen
// =============================================================================

@Composable
fun DeviceEditScreen(
    deviceId: Int?,
    onNavigateBack: () -> Unit,
    topAppBarController: TopAppBarController,
    viewModel: DeviceEditViewModel = hiltViewModel(),
    notificationManager: NotificationManager,
    deleteViewModel: DeviceDeleteViewModel = hiltViewModel(),
    photoManager: PhotoManager
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val device by viewModel.device.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Менеджер камеры — живёт столько же, сколько экран
    val cameraManager = remember { CameraManager(context) }

    // ------------------------------------------------------------------
    // Лончеры
    // ------------------------------------------------------------------

    // Съёмка фото камерой
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = cameraManager.consumePendingUri()
            uri?.let { capturedUri ->
                scope.launch {
                    handlePhotoSelected(
                        uri = capturedUri,
                        device = device,
                        photoManager = photoManager,
                        onUpdate = { photos -> viewModel.updateDevice { it.copy(photos = photos) } },
                        onError = { msg -> snackbarHostState.showSnackbar(msg) }
                    )
                    // Удаляем временный файл после сохранения
                    cameraManager.cleanupTempFile(capturedUri)
                }
            }
        } else {
            // Пользователь отменил съёмку — очищаем pending URI
            cameraManager.consumePendingUri()
        }
    }

    // Запрос разрешения камеры
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val launched = cameraManager.onPermissionResult(isGranted, cameraLauncher)
        if (!launched) {
            scope.launch {
                snackbarHostState.showSnackbar("Нет разрешения на использование камеры")
            }
        }
    }

    // Выбор фото из галереи
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedUri ->
            scope.launch {
                handlePhotoSelected(
                    uri = selectedUri,
                    device = device,
                    photoManager = photoManager,
                    onUpdate = { photos -> viewModel.updateDevice { it.copy(photos = photos) } },
                    onError = { msg -> snackbarHostState.showSnackbar(msg) }
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // Колбэки TopAppBar
    // ------------------------------------------------------------------

    LaunchedEffect(viewModel) {
        topAppBarController.updateState(
            topAppBarController.state.value.copy(
                onSaveClick = { viewModel.saveDevice() },
                onDeleteClick = {
                    scope.launch { deleteViewModel.checkAndShowDialog(device) }
                }
            )
        )
    }

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

    // ------------------------------------------------------------------
    // Реакция на состояние ViewModel
    // ------------------------------------------------------------------

    LaunchedEffect(uiState) {
        when {
            uiState.isSaved -> onNavigateBack()
            uiState.isDeleted -> onNavigateBack()
            uiState.error != null -> {
                scope.launch { snackbarHostState.showSnackbar("Ошибка: ${uiState.error}") }
                viewModel.clearError()
            }
        }
    }

    // ------------------------------------------------------------------
    // Инициализация
    // ------------------------------------------------------------------

    LaunchedEffect(deviceId) {
        if (deviceId != null && deviceId > 0) {
            viewModel.loadDevice(deviceId)
        } else {
            viewModel.updateDevice {
                Device.createEmpty().copy(type = "Манометр", status = "В работе")
            }
        }
    }

    // ------------------------------------------------------------------
    // UI
    // ------------------------------------------------------------------

    val deleteDialogData by deleteViewModel.showDeleteDialog.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        DeviceEditForm(
            device = device,
            uiState = uiState,
            photoManager = photoManager,
            onTypeChanged = { viewModel.updateDevice { d -> d.copy(type = it) } },
            onNameChanged = { viewModel.updateDevice { d -> d.copy(name = it) } },
            onManufacturerChanged = { viewModel.updateDevice { d -> d.copy(manufacturer = it) } },
            onInventoryNumberChanged = { viewModel.updateDevice { d -> d.copy(inventoryNumber = it) } },
            onYearChanged = { viewModel.updateDevice { d -> d.copy(year = it.toIntOrNull()) } },
            onMeasurementLimitChanged = { viewModel.updateDevice { d -> d.copy(measurementLimit = it) } },
            onAccuracyClassChanged = { viewModel.updateDevice { d -> d.copy(accuracyClass = it.toDoubleOrNull()) } },
            onLocationChanged = { viewModel.updateDevice { d -> d.copy(location = it) } },
            onValveNumberChanged = { viewModel.updateDevice { d -> d.copy(valveNumber = it) } },
            onStatusChanged = { viewModel.updateDevice { d -> d.copy(status = it) } },
            onAdditionalInfoChanged = { viewModel.updateDevice { d -> d.copy(additionalInfo = it) } },
            onPhotoDeleted = { photoIndex ->
                device?.let { currentDevice ->
                    val photos = currentDevice.photos
                    if (photoIndex in photos.indices) {
                        scope.launch {
                            val deleted = photoManager.deleteDevicePhoto(currentDevice, photos[photoIndex])
                            if (deleted) {
                                val updated = photos.toMutableList().apply { removeAt(photoIndex) }
                                viewModel.updateDevice { it.copy(photos = updated) }
                            } else {
                                snackbarHostState.showSnackbar("Не удалось удалить фото")
                            }
                        }
                    }
                }
            },
            onTakePhotoClick = {
                val launched = cameraManager.launch(permissionLauncher, cameraLauncher)
                if (!launched) {
                    scope.launch { snackbarHostState.showSnackbar("Не удалось создать файл для камеры") }
                }
            },
            onChooseFromGalleryClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        )

        deleteDialogData?.let { dialogData ->
            DeviceDeleteDialog(
                device = dialogData.device,
                scheme = dialogData.scheme,
                deviceCountInLocation = dialogData.deviceCountInLocation,
                isLastInLocation = dialogData.isLastInLocation,
                onDismiss = { deleteViewModel.dismissDialog() },
                onConfirm = { deleteScheme ->
                    scope.launch {
                        runCatching { viewModel.deleteDevice(deleteScheme) }
                            .onFailure { snackbarHostState.showSnackbar("Ошибка удаления: ${it.message}") }
                        deleteViewModel.dismissDialog()
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }
}

// =============================================================================
// Общая suspend-функция сохранения фото (галерея + камера)
// =============================================================================

private suspend fun handlePhotoSelected(
    uri: Uri,
    device: Device?,
    photoManager: PhotoManager,
    onUpdate: (List<String>) -> Unit,
    onError: suspend (String) -> Unit
) {
    val currentDevice = device ?: run {
        onError("Устройство не загружено")
        return
    }

    photoManager.savePhotoForDevice(currentDevice, uri)
        .onSuccess { result -> onUpdate(result.device.photos) }
        .onFailure { onError("Ошибка сохранения фото: ${it.message}") }
}

// =============================================================================
// DeviceEditForm
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceEditForm(
    device: Device?,
    uiState: com.kipia.management.mobile.viewmodel.DeviceEditUiState,
    photoManager: PhotoManager,
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
    onPhotoDeleted: (Int) -> Unit,
    onTakePhotoClick: () -> Unit,
    onChooseFromGalleryClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeviceEditViewModel = hiltViewModel()
) {
    val safeDevice = device ?: Device.createEmpty()
    val isLocationDropdownExpanded by viewModel.isLocationDropdownExpanded.collectAsStateWithLifecycle()
    val allLocations by viewModel.allLocations.collectAsStateWithLifecycle()

    var showPhotoOptions by remember { mutableStateOf(false) }
    var isTypeExpanded by remember { mutableStateOf(false) }
    var isStatusExpanded by remember { mutableStateOf(false) }

    // Полные пути к фото
    val photoPaths = remember(safeDevice, photoManager) {
        safeDevice.photos.mapNotNull { fileName ->
            photoManager.getFullPhotoPath(safeDevice, fileName)
                .takeIf { !it.isNullOrBlank() }
        }
    }

    // Локальные состояния полей ввода
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

    // Диалог выбора источника фото
    if (showPhotoOptions) {
        PhotoSourceDialog(
            onDismiss = { showPhotoOptions = false },
            onTakePhoto = {
                showPhotoOptions = false
                onTakePhotoClick()
            },
            onChooseFromGallery = {
                showPhotoOptions = false
                onChooseFromGalleryClick()
            }
        )
    }

    Column(
        modifier = modifier.padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {

        // ---- Фото --------------------------------------------------------

        DeviceEditSectionTitle("Основное фото")
        DeviceEditMainPhotoSection(
            photoPaths = photoPaths,
            onPhotoClick = { showPhotoOptions = true }
        )

        // ---- Основная информация -----------------------------------------

        DeviceEditSectionTitle("Основная информация")

        DeviceTextField(
            value = typeText,
            onValueChange = { typeText = it; onTypeChanged(it) },
            label = "Тип прибора *",
            placeholder = "Например: Манометр, Термометр и т.д.",
            isError = uiState.typeError != null,
            errorText = uiState.typeError
        )

        DeviceTextField(
            value = nameText,
            onValueChange = { nameText = it; onNameChanged(it) },
            label = "Наименование"
        )

        DeviceTextField(
            value = manufacturerText,
            onValueChange = { manufacturerText = it; onManufacturerChanged(it) },
            label = "Производитель"
        )

        DeviceTextField(
            value = inventoryNumberText,
            onValueChange = { inventoryNumberText = it; onInventoryNumberChanged(it) },
            label = "Инвентарный номер *",
            isError = uiState.inventoryNumberError != null,
            errorText = uiState.inventoryNumberError
        )

        DeviceTextField(
            value = yearText,
            onValueChange = { yearText = it; onYearChanged(it) },
            label = "Год выпуска",
            keyboardType = KeyboardType.Number
        )

        DeviceTextField(
            value = measurementLimitText,
            onValueChange = { measurementLimitText = it; onMeasurementLimitChanged(it) },
            label = "Предел измерений"
        )

        DeviceTextField(
            value = accuracyClassText,
            onValueChange = { accuracyClassText = it; onAccuracyClassChanged(it) },
            label = "Класс точности",
            keyboardType = KeyboardType.Number
        )

        DeviceTextField(
            value = valveNumberText,
            onValueChange = { valveNumberText = it; onValveNumberChanged(it) },
            label = "Номер крана"
        )

        // ---- Место и статус ----------------------------------------------

        DeviceEditSectionTitle("Место и статус")

        // Место установки с автодополнением
        ExposedDropdownMenuBox(
            expanded = isLocationDropdownExpanded,
            onExpandedChange = { expanded ->
                if (expanded) viewModel.expandLocationDropdown()
                else viewModel.collapseLocationDropdown()
            }
        ) {
            OutlinedTextField(
                value = locationText,
                onValueChange = { locationText = it; onLocationChanged(it) },
                label = { Text("Место установки *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .padding(vertical = 4.dp),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isLocationDropdownExpanded)
                },
                isError = uiState.locationError != null,
                placeholder = {
                    if (allLocations.isNotEmpty()) Text("Введите или выберите из списка...")
                }
            )

            ExposedDropdownMenu(
                expanded = isLocationDropdownExpanded && allLocations.isNotEmpty(),
                onDismissRequest = { viewModel.collapseLocationDropdown() }
            ) {
                allLocations.forEach { location ->
                    DropdownMenuItem(
                        text = { Text(location, color = MaterialTheme.colorScheme.onSurfaceVariant) },
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

        // Статус
        ExposedDropdownMenuBox(
            expanded = isStatusExpanded,
            onExpandedChange = { isStatusExpanded = it }
        ) {
            OutlinedTextField(
                value = statusText,
                onValueChange = { statusText = it; onStatusChanged(it) },
                label = { Text("Статус") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .padding(vertical = 4.dp),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStatusExpanded)
                }
            )

            ExposedDropdownMenu(
                expanded = isStatusExpanded,
                onDismissRequest = { isStatusExpanded = false }
            ) {
                Device.Companion.STATUSES.forEach { status ->
                    DropdownMenuItem(
                        text = { Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = {
                            statusText = status
                            onStatusChanged(status)
                            isStatusExpanded = false
                        }
                    )
                }
            }
        }

        // ---- Галерея фото ------------------------------------------------

        DeviceEditSectionTitle("Дополнительные фото")
        DeviceEditPhotoGallerySection(
            photoPaths = photoPaths,
            onAddPhotoClick = { showPhotoOptions = true },
            onDeletePhoto = onPhotoDeleted
        )

        // ---- Доп. информация ---------------------------------------------

        DeviceEditSectionTitle("Дополнительная информация")
        OutlinedTextField(
            value = additionalInfoText,
            onValueChange = { additionalInfoText = it; onAdditionalInfoChanged(it) },
            label = { Text("Примечания") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(vertical = 4.dp),
            singleLine = false,
            maxLines = 5
        )

        // ---- Ошибки валидации --------------------------------------------

        if (!uiState.isFormValid && uiState.validationErrors.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
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
                        text = "Заполните обязательные поля (*): " +
                                uiState.validationErrors.joinToString(", ") { field ->
                                    when (field) {
                                        "type" -> "Тип прибора"
                                        "inventoryNumber" -> "Инвентарный номер"
                                        "location" -> "Место установки"
                                        else -> field
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

// =============================================================================
// Мелкие переиспользуемые компоненты
// =============================================================================

/**
 * Обёртка над OutlinedTextField с поддержкой ошибок — убирает дублирование
 * одинаковых блоков по всей форме.
 */
@Composable
private fun DeviceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String? = null,
    isError: Boolean = false,
    errorText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        placeholder = placeholder?.let { { Text(it) } },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = singleLine,
        isError = isError,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = keyboardType)
    )
    errorText?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )
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

// =============================================================================
// Фото-секции
// =============================================================================

@Composable
fun DeviceEditMainPhotoSection(
    photoPaths: List<String>,
    onPhotoClick: () -> Unit
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

            IconButton(
                onClick = onPhotoClick,
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
    photoPaths: List<String>,
    onAddPhotoClick: () -> Unit,
    onDeletePhoto: (Int) -> Unit
) {
    Column {
        if (photoPaths.isEmpty()) {
            Card(
                onClick = onAddPhotoClick,
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "Добавить фото",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Добавить фото", color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                if (photoPaths.size < 10) {
                    Card(
                        onClick = onAddPhotoClick,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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

// =============================================================================
// Диалоги
// =============================================================================

@Composable
fun PhotoSourceDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить фото") },
        text = { Text("Выберите источник фото:") },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onTakePhoto) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Камера")
                }
                TextButton(onClick = onChooseFromGallery) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Галерея")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
