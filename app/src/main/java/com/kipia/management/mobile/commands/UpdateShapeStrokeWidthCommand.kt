package com.kipia.management.mobile.commands

import com.kipia.management.mobile.managers.Command
import com.kipia.management.mobile.managers.ShapeManager
import com.kipia.management.mobile.viewmodel.EditorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class UpdateShapeStrokeWidthCommand(
    private val shapeManager: ShapeManager,
    private val editorState: MutableStateFlow<EditorState>,
    private val shapeId: String,
    private val newWidth: Float,
    private val oldWidth: Float
) : Command {
    override fun execute() {
        shapeManager.updateStrokeWidth(shapeId, newWidth)
        editorState.update { it.copy(uiState = it.uiState.copy(isDirty = true)) }
    }

    override fun undo() {
        shapeManager.updateStrokeWidth(shapeId, oldWidth)
        editorState.update { it.copy(uiState = it.uiState.copy(isDirty = true)) }
    }
}