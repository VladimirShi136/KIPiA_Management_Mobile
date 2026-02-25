package com.kipia.management.mobile.managers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CommandManager(
    private val maxHistorySize: Int = 50
) {
    private val undoStack = ArrayDeque<Command>(maxHistorySize)
    private val redoStack = ArrayDeque<Command>(maxHistorySize)

    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo = _canRedo.asStateFlow()

    fun execute(command: Command) {
        command.execute()
        undoStack.addLast(command)

        // Ограничиваем размер стека
        while (undoStack.size > maxHistorySize) {
            undoStack.removeFirst()
        }

        redoStack.clear()
        updateState()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val command = undoStack.removeLast()
            command.undo()
            redoStack.addLast(command)
            updateState()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val command = redoStack.removeLast()
            command.execute()
            undoStack.addLast(command)
            updateState()
        }
    }

    private fun updateState() {
        _canUndo.update { undoStack.isNotEmpty() }
        _canRedo.update { redoStack.isNotEmpty() }
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        updateState()
    }
}

// Базовый интерфейс команды
interface Command {
    fun execute()
    fun undo()
}