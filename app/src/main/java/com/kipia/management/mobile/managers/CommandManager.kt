package com.kipia.management.mobile.managers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeLine

class CommandManager {
    private val undoStack = mutableListOf<Command>()
    private val redoStack = mutableListOf<Command>()
    private var maxHistory = 50

    fun execute(command: Command) {
        command.execute()
        undoStack.add(command)

        // Ограничиваем размер истории
        while (undoStack.size > maxHistory) {
            undoStack.removeAt(0)
        }

        // Очищаем redo stack при новом действии
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val command = undoStack.removeAt(undoStack.size - 1)
            command.undo()
            redoStack.add(command)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val command = redoStack.removeAt(redoStack.size - 1)
            command.execute()
            undoStack.add(command)
        }
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    fun getUndoStackSize(): Int = undoStack.size
    fun getRedoStackSize(): Int = redoStack.size
}

interface Command {
    fun execute()
    fun undo()
}

// Команды для редактора схем
class AddShapeCommand(
    private val shapeManager: ShapeManager,
    private val shape: ComposeShape,
    private val onShapeAdded: (() -> Unit)? = null
) : Command {
    override fun execute() {
        shapeManager.addShape(shape)
        onShapeAdded?.invoke()
    }

    override fun undo() {
        shapeManager.removeShape(shape)
        onShapeAdded?.invoke()
    }
}

class DeleteShapeCommand(
    private val shapeManager: ShapeManager,
    private val shape: ComposeShape,
    private val onShapeDeleted: (() -> Unit)? = null
) : Command {
    private var shapeIndex: Int = -1
    private var wasSelected: Boolean = false

    override fun execute() {
        shapeIndex = shapeManager.getShapeIndex(shape)
        wasSelected = shapeManager.getSelectedShape() == shape
        shapeManager.removeShape(shape)
        onShapeDeleted?.invoke()
    }

    override fun undo() {
        if (shapeIndex != -1) {
            shapeManager.restoreShape(shape, shapeIndex)
            if (wasSelected) {
                shapeManager.selectShape(shape)
            }
            onShapeDeleted?.invoke()
        }
    }
}

class MoveShapeCommand(
    private val shapeManager: ShapeManager,
    private val shape: ComposeShape,
    private val oldX: Float,
    private val oldY: Float,
    private val newX: Float,
    private val newY: Float,
    private val onShapeMoved: (() -> Unit)? = null
) : Command {
    override fun execute() {
        shapeManager.moveShapeTo(shape, newX, newY)
        onShapeMoved?.invoke()
    }

    override fun undo() {
        shapeManager.moveShapeTo(shape, oldX, oldY)
        onShapeMoved?.invoke()
    }
}

class ResizeShapeCommand(
    private val shapeManager: ShapeManager,
    private val shape: ComposeShape,
    private val oldWidth: Float,
    private val oldHeight: Float,
    private val newWidth: Float,
    private val newHeight: Float,
    private val onShapeResized: (() -> Unit)? = null
) : Command {

    private val oldEndX = if (shape is ComposeLine) shape.endX else 0f
    private val oldEndY = if (shape is ComposeLine) shape.endY else 0f
    private val newEndX = if (shape is ComposeLine) newWidth else 0f
    private val newEndY = if (shape is ComposeLine) newHeight else 0f

    override fun execute() {
        if (shape is ComposeLine) {
            shape.endX = newEndX
            shape.endY = newEndY
            shape.width = maxOf(shape.startX, newEndX) + 10f
            shape.height = maxOf(shape.startY, newEndY) + 10f
        } else {
            shapeManager.resizeShape(shape, newWidth, newHeight)
        }
        onShapeResized?.invoke()
    }

    override fun undo() {
        if (shape is ComposeLine) {
            shape.endX = oldEndX
            shape.endY = oldEndY
            shape.width = maxOf(shape.startX, oldEndX) + 10f
            shape.height = maxOf(shape.startY, oldEndY) + 10f
        } else {
            shapeManager.resizeShape(shape, oldWidth, oldHeight)
        }
        onShapeResized?.invoke()
    }
}

class UpdateShapePropertiesCommand(
    private val shapeManager: ShapeManager,
    private val oldShape: ComposeShape,
    private val newShape: ComposeShape,
    private val onShapeUpdated: (() -> Unit)? = null
) : Command {
    override fun execute() {
        shapeManager.updateShape(oldShape, newShape)
        onShapeUpdated?.invoke()
    }

    override fun undo() {
        shapeManager.updateShape(newShape, oldShape)
        onShapeUpdated?.invoke()
    }
}

class RotateShapeCommand(
    private val shapeManager: ShapeManager,
    private val shape: ComposeShape,
    private val oldRotation: Float,
    private val newRotation: Float,
    private val onShapeRotated: (() -> Unit)? = null
) : Command {
    override fun execute() {
        shapeManager.rotateShapeTo(shape, newRotation)
        onShapeRotated?.invoke()
    }

    override fun undo() {
        shapeManager.rotateShapeTo(shape, oldRotation)
        onShapeRotated?.invoke()
    }
}

class ChangeColorCommand(
    private val shapeManager: ShapeManager,
    private val shape: ComposeShape,
    private val oldColor: Color,
    private val newColor: Color,
    private val isFillColor: Boolean,
    private val onColorChanged: (() -> Unit)? = null
) : Command {
    override fun execute() {
        if (isFillColor) {
            shapeManager.changeFillColor(shape, newColor)
        } else {
            shapeManager.changeStrokeColor(shape, newColor)
        }
        onColorChanged?.invoke()
    }

    override fun undo() {
        if (isFillColor) {
            shapeManager.changeFillColor(shape, oldColor)
        } else {
            shapeManager.changeStrokeColor(shape, oldColor)
        }
        onColorChanged?.invoke()
    }
}

class ChangeStrokeWidthCommand(
    private val shapeManager: ShapeManager,
    private val shape: ComposeShape,
    private val oldWidth: Float,
    private val newWidth: Float,
    private val onWidthChanged: (() -> Unit)? = null
) : Command {
    override fun execute() {
        shapeManager.changeStrokeWidth(shape, newWidth)
        onWidthChanged?.invoke()
    }

    override fun undo() {
        shapeManager.changeStrokeWidth(shape, oldWidth)
        onWidthChanged?.invoke()
    }
}

class BringToFrontCommand(
    private val shapeManager: ShapeManager,
    private val shape: ComposeShape,
    private val onOrderChanged: (() -> Unit)? = null
) : Command {
    private var oldIndex: Int = -1

    override fun execute() {
        oldIndex = shapeManager.getShapeIndex(shape)
        shapeManager.bringToFront(shape)
        onOrderChanged?.invoke()
    }

    override fun undo() {
        if (oldIndex != -1) {
            shapeManager.removeShape(shape)
            shapeManager.restoreShape(shape, oldIndex)
            onOrderChanged?.invoke()
        }
    }
}

class SendToBackCommand(
    private val shapeManager: ShapeManager,
    private val shape: ComposeShape,
    private val onOrderChanged: (() -> Unit)? = null
) : Command {
    private var oldIndex: Int = -1

    override fun execute() {
        oldIndex = shapeManager.getShapeIndex(shape)
        shapeManager.sendToBack(shape)
        onOrderChanged?.invoke()
    }

    override fun undo() {
        if (oldIndex != -1) {
            shapeManager.removeShape(shape)
            shapeManager.restoreShape(shape, oldIndex)
            onOrderChanged?.invoke()
        }
    }
}