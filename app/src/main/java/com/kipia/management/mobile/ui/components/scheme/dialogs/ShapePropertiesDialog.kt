package com.kipia.management.mobile.ui.components.scheme.dialogs

import androidx.compose.runtime.*
import com.kipia.management.mobile.ui.components.scheme.shapes.*

@Composable
fun ShapePropertiesDialog(
    shape: ComposeShape,
    onDismiss: () -> Unit,
    onUpdate: (ComposeShape) -> Unit
) {
    when (shape) {
        is ComposeLine -> CompactLineDialog(shape, onDismiss, onUpdate)
        is ComposeText -> TextPropertiesDialog(shape, onDismiss, onUpdate)
        else -> CompactSizeRotationDialog(shape, onDismiss, onUpdate)
    }
}