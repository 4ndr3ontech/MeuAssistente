package com.example.meuassistente.ui.viewmodel

import com.example.meuassistente.data.local.entity.TaskEntity

data class TaskUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val isDialogOpen: Boolean = false,
    val editingTask: TaskEntity? = null
) {
    val pendingCount: Int get() = tasks.count { !it.isCompleted }
    val completedCount: Int get() = tasks.count { it.isCompleted }
}
