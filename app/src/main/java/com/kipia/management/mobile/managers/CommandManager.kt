package com.kipia.management.mobile.managers

import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape

class CommandManager {
    private val undoStack = mutableListOf<Command>()
    private val redoStack = mutableListOf<Command>()
    private var maxHistory = 50

    fun execute(command: Command) {
        command.execute()
        undoStack.add(command)

        // Ограничиваем размер истории
        if (undoStack.size > maxHistory) {
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
}

interface Command {
    fun execute()
    fun undo()
}

// Команды для редактора схем
class AddShapeCommand(
    private val shapeManager: ShapeManager,
    private val shape: ComposeShape
) : Command {
    override fun execute() {
        shapeManager.addShape(shape)
    }

    override fun undo() {
        shapeManager.removeShape(shape)
    }
}

class DeleteShapeCommand(
    private val shapeManager: ShapeManager,
    private val shape: ComposeShape
) : Command {
    private var shapeIndex: Int = -1

    override fun execute() {
        // Получаем индекс перед удалением
        shapeIndex = shapeManager.getAllShapes().indexOf(shape)
        shapeManager.removeShape(shape)
    }

    override fun undo() {
        if (shapeIndex != -1) {
            // Восстанавливаем фигуру
            // Нужно получить доступ к mutable списку или добавить метод в ShapeManager
            shapeManager.restoreShape(shape, shapeIndex)
        } else {
            shapeManager.addShape(shape)
        }
    }
}

class MoveShapeCommand(
    private val shape: ComposeShape,
    private val oldX: Float,
    private val oldY: Float,
    private val newX: Float,
    private val newY: Float
) : Command {
    override fun execute() {
        shape.x = newX
        shape.y = newY
    }

    override fun undo() {
        shape.x = oldX
        shape.y = oldY
    }
}

class ResizeShapeCommand(
    private val shape: ComposeShape,
    private val oldWidth: Float,
    private val oldHeight: Float,
    private val newWidth: Float,
    private val newHeight: Float
) : Command {
    override fun execute() {
        shape.width = newWidth
        shape.height = newHeight
    }

    override fun undo() {
        shape.width = oldWidth
        shape.height = oldHeight
    }
}