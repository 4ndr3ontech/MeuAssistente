package com.example.meuassistente.ui.viewmodel

import com.example.meuassistente.data.local.entity.TaskCategory
import com.example.meuassistente.data.local.entity.TaskEntity

data class TaskUiState(
    val allTasks: List<TaskEntity> = emptyList(),
    val selectedCategory: TaskCategory? = null,
    val isDialogOpen: Boolean = false,
    val editingTask: TaskEntity? = null,
    val statusMessage: String? = null
) {
    val tasks: List<TaskEntity>
        get() = if (selectedCategory == null) allTasks
        else allTasks.filter { it.category == selectedCategory.name }

    val pendingCount: Int get() = allTasks.count { !it.isCompleted }
    val completedCount: Int get() = allTasks.count { it.isCompleted }
}
