package com.kipia.management.mobile.ui.screens.devices

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kipia.management.mobile.ui.theme.Dimens
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.managers.CameraManager
import com.kipia.management.mobile.managers.PhotoManager
import com.kipia.management.mobile.ui.components.topappbar.TopAppBarController
import com.kipia.management.mobile.ui.shared.NotificationManager
import com.kipia.management.mobile.viewmodel.DeviceDeleteViewModel
import com.kipia.management.mobile.viewmodel.DeviceEditViewModel
import com.kipia.management.mobile.ui.components.dialogs.SavingOverlay
import com.kipia.management.mobile.ui.components.dialogs.UnsavedChangesDialog
import com.kipia.management.mobile.ui.components.dialogs.PhotoSourceDialog
import com.kipia.management.mobile.ui.components.dialogs.DeviceDeleteDialog
import kotlinx.coroutines.launch

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
    val keyboardController = LocalSoftwareKeyboardController.current

    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var shouldNavigateAfterSave by remember { mutableStateOf(false) }

    // Менеджер камеры
    val cameraManager = remember { CameraManager(context) }

    // Перехват системной кнопки "Назад"
    BackHandler {
        if (viewModel.hasUnsavedChanges()) {
            showUnsavedChangesDialog = true
        } else {
            onNavigateBack()
        }
    }

    // ------------------------------------------------------------------
    // Лончеры
    // ------------------------------------------------------------------

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = cameraManager.consumePendingUri()
            uri?.let { capturedUri ->
                viewModel.addPhoto(capturedUri)
                cameraManager.cleanupTempFile(capturedUri)
            }
        } else {
            cameraManager.consumePendingUri()
        }
    }

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

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedUri ->
            viewModel.addPhoto(selectedUri)
        }
    }

    // ------------------------------------------------------------------
    // Настройка TopAppBar
    // ------------------------------------------------------------------

    LaunchedEffect(deviceId) {
        topAppBarController.setForScreen(
            screenRoute = "device_edit",
            additionalParams = mapOf(
                "isNew" to (deviceId == null || deviceId <= 0),
                "onSaveClick" to {
                    keyboardController?.hide()
                    shouldNavigateAfterSave = true
                    viewModel.saveDevice()
                },
                "onDeleteClick" to {
                    scope.launch {
                        viewModel.device.value?.let { nonNullDevice ->
                            deleteViewModel.checkAndShowDialog(nonNullDevice)
                        } ?: run {
                            snackbarHostState.showSnackbar("Устройство не загружено")
                        }
                    }
                },
                "onBackClick" to {
                    if (viewModel.hasUnsavedChanges()) {
                        showUnsavedChangesDialog = true
                    } else {
                        onNavigateBack()
                    }
                }
            )
        )
    }

    // ------------------------------------------------------------------
    // Реакция на состояние ViewModel
    // ------------------------------------------------------------------

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.clearSaveState()
            if (shouldNavigateAfterSave) {
                onNavigateBack()
            }
        }
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
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
                Device.createEmpty().copy(type = "", status = "")
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
                        viewModel.deletePhoto(photos[photoIndex])
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
                .imePadding()
                .verticalScroll(rememberScrollState()),
            viewModel = viewModel
        )

        SavingOverlay(
            isSaving = uiState.isSaving,
            text = "Сохранение..."
        )

        if (showUnsavedChangesDialog) {
            UnsavedChangesDialog(
                onDismiss = { showUnsavedChangesDialog = false },
                onConfirmExit = {
                    showUnsavedChangesDialog = false
                    onNavigateBack()
                },
                onSaveAndExit = {
                    showUnsavedChangesDialog = false
                    shouldNavigateAfterSave = true
                    viewModel.saveDevice()
                }
            )
        }

        deleteDialogData?.let { dialogData ->
            DeviceDeleteDialog(
                device = dialogData.device,
                scheme = dialogData.scheme,
                deviceCountInLocation = dialogData.deviceCountInLocation,
                isLastInLocation = dialogData.isLastInLocation,
                onDismiss = { deleteViewModel.dismissDialog() },
                onConfirm = { deletePhotos, deleteScheme ->
                    scope.launch {
                        runCatching { viewModel.deleteDevice(deletePhotos, deleteScheme) }
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
                .padding(bottom = Dimens.spacingMedium)
        )
    }
}

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
    val isNew = safeDevice.id <= 0
    val allLocations by viewModel.allLocations.collectAsStateWithLifecycle()

    var showPhotoOptions by remember { mutableStateOf(false) }
    var isStatusExpanded by remember { mutableStateOf(false) }
    var isLocationExpanded by remember { mutableStateOf(false) }

    val photoPaths = remember(safeDevice, photoManager) {
        safeDevice.photos.mapNotNull { fileName ->
            photoManager.getFullPhotoPath(safeDevice, fileName)
                .takeIf { !it.isNullOrBlank() }
        }
    }

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

    // Синхронизация локальных состояний с данными из ViewModel только при необходимости
    LaunchedEffect(safeDevice.id, safeDevice.updatedAt) {
        if (typeText != safeDevice.type) typeText = safeDevice.type
        if (inventoryNumberText != safeDevice.inventoryNumber) inventoryNumberText = safeDevice.inventoryNumber
        if (locationText != safeDevice.location) locationText = safeDevice.location
        if (nameText != (safeDevice.name ?: "")) nameText = safeDevice.name ?: ""
        if (manufacturerText != (safeDevice.manufacturer ?: "")) manufacturerText = safeDevice.manufacturer ?: ""
        if (yearText != (safeDevice.year?.toString() ?: "")) yearText = safeDevice.year?.toString() ?: ""
        if (measurementLimitText != (safeDevice.measurementLimit ?: "")) measurementLimitText = safeDevice.measurementLimit ?: ""
        if (accuracyClassText != (safeDevice.accuracyClass?.toString() ?: "")) accuracyClassText = safeDevice.accuracyClass?.toString() ?: ""
        if (valveNumberText != (safeDevice.valveNumber ?: "")) valveNumberText = safeDevice.valveNumber ?: ""
        if (statusText != safeDevice.status) statusText = safeDevice.status
        if (additionalInfoText != (safeDevice.additionalInfo ?: "")) additionalInfoText = safeDevice.additionalInfo ?: ""
    }

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
        modifier = modifier.padding(Dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)
    ) {
        DeviceEditSectionTitle("Основная информация")
        DeviceTextField(value = typeText, onValueChange = { typeText = it; onTypeChanged(it) }, label = "Тип прибора *",  isError = uiState.typeError != null, errorText = uiState.typeError)
        DeviceTextField(value = nameText, onValueChange = { nameText = it; onNameChanged(it) }, label = "Модель *", isError = uiState.nameError != null, errorText = uiState.nameError)
        DeviceTextField(value = manufacturerText, onValueChange = { manufacturerText = it; onManufacturerChanged(it) }, label = "Производитель")
        DeviceTextField(value = inventoryNumberText, onValueChange = { inventoryNumberText = it; onInventoryNumberChanged(it) }, label = "Инвентарный номер *", isError = uiState.inventoryNumberError != null, errorText = uiState.inventoryNumberError)
        DeviceTextField(value = yearText, onValueChange = { yearText = it; onYearChanged(it) }, label = "Год выпуска", keyboardType = KeyboardType.Number)
        DeviceTextField(value = measurementLimitText, onValueChange = { measurementLimitText = it; onMeasurementLimitChanged(it) }, label = "Предел измерений")
        DeviceTextField(
            value = accuracyClassText,
            onValueChange = { 
                val sanitized = it.replace(',', '.')
                accuracyClassText = sanitized
                onAccuracyClassChanged(sanitized) 
            },
            label = "Класс точности",
            keyboardType = KeyboardType.Decimal
        )
        DeviceTextField(value = valveNumberText, onValueChange = { valveNumberText = it; onValveNumberChanged(it) }, label = "Номер крана")

        DeviceEditSectionTitle("Место и статус")
        ExposedDropdownMenuBox(
            expanded = isLocationExpanded,
            onExpandedChange = { isLocationExpanded = it }
        ) {
            OutlinedTextField(
                value = locationText,
                onValueChange = { 
                    locationText = it
                    onLocationChanged(it)
                },
                label = { Text("Место установки *") },
                modifier = Modifier.fillMaxWidth().menuAnchor().padding(vertical = Dimens.spacingSmall),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isLocationExpanded) },
                isError = uiState.locationError != null
            )
            ExposedDropdownMenu(
                expanded = isLocationExpanded && allLocations.isNotEmpty(),
                onDismissRequest = { isLocationExpanded = false }
            ) {
                allLocations.forEach { location ->
                    DropdownMenuItem(
                        text = { Text(location) },
                        onClick = {
                            locationText = location
                            onLocationChanged(location)
                            isLocationExpanded = false
                        }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(expanded = isStatusExpanded, onExpandedChange = { isStatusExpanded = it }) {
            OutlinedTextField(
                value = statusText,
                onValueChange = { statusText = it; onStatusChanged(it) },
                label = { Text("Статус *") },
                modifier = Modifier.fillMaxWidth().menuAnchor().padding(vertical = Dimens.spacingSmall),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStatusExpanded) },
                isError = uiState.statusError != null
            )
            ExposedDropdownMenu(expanded = isStatusExpanded, onDismissRequest = { isStatusExpanded = false }) {
                Device.STATUSES.forEach { status ->
                    DropdownMenuItem(text = { Text(status) }, onClick = { statusText = status; onStatusChanged(status); isStatusExpanded = false })
                }
            }
        }

        if (!isNew) {
            DeviceEditSectionTitle("Фотографии")
            DeviceEditPhotoGallerySection(photoPaths = photoPaths, onAddPhotoClick = { showPhotoOptions = true }, onDeletePhoto = onPhotoDeleted)
        }

        DeviceEditSectionTitle("Дополнительная информация")
        OutlinedTextField(value = additionalInfoText, onValueChange = { additionalInfoText = it; onAdditionalInfoChanged(it) }, label = { Text("Примечания") }, modifier = Modifier.fillMaxWidth().height(100.dp).padding(vertical = Dimens.spacingSmall), singleLine = false, maxLines = 5)

        if (uiState.showValidationErrorCard && !uiState.isFormValid) {
            ValidationErrorsCard(uiState.validationErrors)
        }
    }
}

@Composable
private fun ValidationErrorsCard(errors: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, "Ошибка", modifier = Modifier.padding(end = Dimens.spacingMedium))
            Text(
                text = "Заполните обязательные поля: " + errors.joinToString(", ") { field ->
                    when (field) {
                        "type" -> "Тип прибора"
                        "name" -> "Модель"
                        "inventoryNumber" -> "Инвентарный номер"
                        "location" -> "Место установки"
                        "status" -> "Статус"
                        else -> field
                    }
                },
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun DeviceTextField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String? = null, isError: Boolean = false, errorText: String? = null, keyboardType: KeyboardType = KeyboardType.Text, singleLine: Boolean = true) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, placeholder = placeholder?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.spacingSmall), singleLine = singleLine, isError = isError, keyboardOptions = KeyboardOptions.Default.copy(keyboardType = keyboardType))
    errorText?.let { Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = Dimens.spacingLarge)) }
}

@Composable
fun DeviceEditSectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = Dimens.spacingSmall, top = Dimens.spacingMedium))
}

@Composable
fun DeviceEditPhotoGallerySection(photoPaths: List<String>, onAddPhotoClick: () -> Unit, onDeletePhoto: (Int) -> Unit) {
    Column {
        if (photoPaths.isEmpty()) {
            Card(onClick = onAddPhotoClick, modifier = Modifier.fillMaxWidth().height(100.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, "Добавить фото", modifier = Modifier.size(Dimens.iconSizeLarge))
                        Text("Добавить фото")
                    }
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                photoPaths.forEachIndexed { index, photoPath ->
                    Box(modifier = Modifier.size(120.dp)) {
                        Card(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(model = photoPath, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                        }

                        // Бейдж "Главное" для первого фото
                        if (index == 0) {
                            Surface(
                                modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Главное",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDeletePhoto(index) },
                            modifier = Modifier.align(Alignment.TopEnd).size(Dimens.iconSizeMedium),
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Icon(Icons.Default.Close, "Удалить", modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Card(
                    onClick = onAddPhotoClick,
                    modifier = Modifier.size(120.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, "Добавить", modifier = Modifier.size(Dimens.iconSizeLarge))
                    }
                }
            }
        }
    }
}