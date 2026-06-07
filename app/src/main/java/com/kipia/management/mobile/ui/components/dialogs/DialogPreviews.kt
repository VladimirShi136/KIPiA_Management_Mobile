package com.kipia.management.mobile.ui.components.dialogs

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kipia.management.mobile.ui.theme.KIPiATheme

@Preview(showBackground = true, name = "Info Dialog")
@Composable
fun InfoDialogPreview() {
    KIPiATheme {
        InfoDialog(
            title = "Внимание",
            message = "Это информационное сообщение для пользователя.",
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "Confirm Dialog")
@Composable
fun ConfirmDialogPreview() {
    KIPiATheme {
        ConfirmDialog(
            title = "Подтверждение",
            message = "Вы уверены, что хотите выполнить это действие?",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "Delete Confirm Dialog")
@Composable
fun DeleteConfirmDialogPreview() {
    KIPiATheme {
        DeleteConfirmDialog(
            title = "Удаление",
            itemName = "Объект: №12345",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "Device Delete Confirm Dialog")
@Composable
fun DeviceDeleteConfirmDialogPreview() {
    KIPiATheme {
        DeviceDeleteConfirmDialog(
            deviceName = "Манометр МП-4У (Инв. №12345)",
            photoCount = 3,
            message = "Это последнее устройство в данной локации.",
            onConfirm = { _ -> }, // Fixed: Added boolean parameter to match DeviceDeleteConfirmDialog signature
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "Unsaved Changes Dialog")
@Composable
fun UnsavedChangesDialogPreview() {
    KIPiATheme {
        UnsavedChangesDialog(
            onDismiss = {},
            onConfirmExit = {},
            onSaveAndExit = {}
        )
    }
}

@Preview(showBackground = true, name = "Photo Source Dialog")
@Composable
fun PhotoSourceDialogPreview() {
    KIPiATheme {
        PhotoSourceDialog(
            onDismiss = {},
            onTakePhoto = {},
            onChooseFromGallery = {}
        )
    }
}

@Preview(showBackground = true, name = "Device Delete With Scheme Dialog")
@Composable
fun DeviceDeleteWithSchemeDialogPreview() {
    KIPiATheme {
        DeviceDeleteWithSchemeDialog(
            deviceName = "Датчик температуры Т-1",
            schemeName = "Схема цеха №1",
            photoCount = 5,
            onConfirm = { _, _ -> },
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "Error Dialog")
@Composable
fun ErrorDialogPreview() {
    KIPiATheme {
        ErrorDialog(
            title = "Ошибка",
            message = "Произошла непредвиденная ошибка при синхронизации данных.",
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true, name = "Saving Overlay")
@Composable
fun SavingOverlayPreview() {
    KIPiATheme {
        SavingOverlay(isSaving = true)
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, name = "Dark Theme Preview")
@Composable
fun DarkThemePreview() {
    KIPiATheme {
        InfoDialog(
            title = "Темная тема",
            message = "Проверка того, как диалоги выглядят в ночном режиме.",
            onDismiss = {}
        )
    }
}
