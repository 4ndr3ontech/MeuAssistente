package com.example.meuassistente.ui.viewmodel

import com.example.meuassistente.data.local.entity.TaskCategory
import com.example.meuassistente.data.local.entity.TaskEntity

data class TaskUiState(
    val allTasks: List<TaskEntity> = emptyList(),
    val selectedCategory: TaskCategory? = null,
    val searchQuery: String = "",
    val isDialogOpen: Boolean = false,
    val editingTask: TaskEntity? = null,
    val statusMessage: String? = null
) {
    val tasks: List<TaskEntity>
        get() {
            val q = searchQuery.trim()
            return allTasks.filter { task ->
                val matchesCategory = selectedCategory == null || task.category == selectedCategory.name
                val matchesQuery = q.isEmpty() ||
                    task.title.contains(q, ignoreCase = true) ||
                    task.description.contains(q, ignoreCase = true)
                matchesCategory && matchesQuery
            }
        }

    val pendingCount: Int get() = allTasks.count { !it.isCompleted }
    val completedCount: Int get() = allTasks.count { it.isCompleted }
}
