package com.kipia.management.mobile.commands

import com.kipia.management.mobile.managers.Command
import com.kipia.management.mobile.managers.ShapeManager
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape
import com.kipia.management.mobile.viewmodel.EditorState
import com.kipia.management.mobile.viewmodel.SelectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Команда для удаления фигуры со схемы.
 */
class DeleteShapeCommand(
    private val shapeManager: ShapeManager,
    private val editorState: MutableStateFlow<EditorState>,
    private val shape: ComposeShape
) : Command {

    /**
     * Выполнить команду
     */
    override fun execute() {
        shapeManager.removeShape(shape.id)
        editorState.update { state ->
            state.copy(
                selection = SelectionState(),
                uiState = state.uiState.copy(
                    isDirty = true,
                    showShapeProperties = false
                )
            )
        }
    }

    /**
     * Отменить команду
     */
    override fun undo() {
        shapeManager.addShape(shape)
        editorState.update { state ->
            state.copy(
                uiState = state.uiState.copy(isDirty = true)
            )
        }
    }
}