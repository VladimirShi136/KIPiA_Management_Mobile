package com.kipia.management.mobile.commands

import androidx.compose.ui.graphics.Color
import com.kipia.management.mobile.managers.Command
import com.kipia.management.mobile.managers.ShapeManager
import com.kipia.management.mobile.viewmodel.EditorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

class UpdateShapeStrokeColorCommand(
    private val shapeManager: ShapeManager,
    private val editorState: MutableStateFlow<EditorState>,
    private val shapeId: String,
    private val newColor: Color,
    private val oldColor: Color
) : Command {
    override fun execute() {
        Timber.d("🎨 Executing stroke color update: $newColor")
        shapeManager.updateStrokeColor(shapeId, newColor)
        editorState.update { it.copy(uiState = it.uiState.copy(isDirty = true)) }
    }

    override fun undo() {
        Timber.d("🎨 Undoing stroke color update: $oldColor")
        shapeManager.updateStrokeColor(shapeId, oldColor)
        editorState.update { it.copy(uiState = it.uiState.copy(isDirty = true)) }
    }
}