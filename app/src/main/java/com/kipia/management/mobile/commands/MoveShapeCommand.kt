package com.kipia.management.mobile.commands

import androidx.compose.ui.geometry.Offset
import com.kipia.management.mobile.managers.Command
import com.kipia.management.mobile.managers.ShapeManager

class MoveShapeCommand(
    private val shapeManager: ShapeManager,
    private val onStateChange: () -> Unit,
    private val shapeId: String,
    private val delta: Offset
) : Command {
    override fun execute() {
        shapeManager.moveShape(shapeId, delta)
        onStateChange()
    }

    override fun undo() {
        shapeManager.moveShape(shapeId, Offset(-delta.x, -delta.y))
        onStateChange()
    }
}