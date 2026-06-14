package com.kipia.management.mobile.commands

import androidx.compose.ui.geometry.Offset
import com.kipia.management.mobile.managers.Command
import com.kipia.management.mobile.managers.ShapeManager

/**
 * Команда для перемещения фигуры на схеме.
 */
class MoveShapeCommand(
    private val shapeManager: ShapeManager,
    private val onStateChange: () -> Unit,
    private val shapeId: String,
    private val delta: Offset
) : Command {

    /**
     * Выполнить команду
     */
    override fun execute() {
        shapeManager.moveShape(shapeId, delta)
        onStateChange()
    }

    /**
     * Отменить команду
     */
    override fun undo() {
        shapeManager.moveShape(shapeId, Offset(-delta.x, -delta.y))
        onStateChange()
    }
}