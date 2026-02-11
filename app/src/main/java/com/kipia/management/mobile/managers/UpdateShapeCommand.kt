package com.kipia.management.mobile.managers

import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape

class UpdateShapeCommand(
    private val shapeManager: ShapeManager,
    private val oldShape: ComposeShape,
    private val newShape: ComposeShape
) : Command {

    override fun execute() {
        // Удаляем старую фигуру
        shapeManager.removeShape(oldShape)
        // Добавляем новую
        shapeManager.addShape(newShape)
    }

    override fun undo() {
        // Удаляем новую фигуру
        shapeManager.removeShape(newShape)
        // Восстанавливаем старую
        shapeManager.addShape(oldShape)
    }
}