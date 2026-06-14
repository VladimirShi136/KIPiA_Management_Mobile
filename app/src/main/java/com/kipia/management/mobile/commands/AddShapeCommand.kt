package com.kipia.management.mobile.commands

import com.kipia.management.mobile.managers.Command
import com.kipia.management.mobile.managers.ShapeManager
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape

/**
 * Команда для добавления фигуры на схему.
 */
class AddShapeCommand(
    private val shapeManager: ShapeManager,
    private val onStateChange: () -> Unit,
    private val shape: ComposeShape
) : Command {

    /**
     * Выполняет команду.
     */
    override fun execute() {
        shapeManager.addShape(shape)
        onStateChange()
    }

    /**
     * Отменяет команду.
     */
    override fun undo() {
        shapeManager.removeShape(shape.id)
        onStateChange()
    }
}