package com.example.meuassistente.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meuassistente.data.local.AppDatabase
import com.example.meuassistente.data.local.entity.RepeatInterval
import com.example.meuassistente.data.local.entity.TaskCategory
import com.example.meuassistente.data.local.entity.TaskEntity
import com.example.meuassistente.data.repository.TaskRepository
import com.example.meuassistente.scheduler.NotificationHelper
import com.example.meuassistente.scheduler.TaskScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TaskRepository(AppDatabase.getInstance(application).taskDao())
    private val selectedCategory = MutableStateFlow<TaskCategory?>(null)
    private val isDialogOpen = MutableStateFlow(false)
    private val editingTask = MutableStateFlow<TaskEntity?>(null)

    val uiState: StateFlow<TaskUiState> = combine(
        repository.allTasks, selectedCategory, isDialogOpen, editingTask
    ) { tasks, category, dialogOpen, taskToEdit ->
        TaskUiState(
            allTasks = tasks,
            selectedCategory = category,
            isDialogOpen = dialogOpen,
            editingTask = taskToEdit
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskUiState())

    fun selectCategory(category: TaskCategory?) { selectedCategory.value = category }
    fun openNewTaskDialog() { editingTask.value = null; isDialogOpen.value = true }
    fun openEditTaskDialog(task: TaskEntity) { editingTask.value = task; isDialogOpen.value = true }
    fun closeDialog() { isDialogOpen.value = false; editingTask.value = null }

    fun saveTask(
        title: String,
        description: String,
        triggerAtMillis: Long,
        hasAlarm: Boolean,
        category: TaskCategory,
        repeatInterval: RepeatInterval
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        viewModelScope.launch {
            val current = editingTask.value
            val entity = current?.copy(
                title = cleanTitle,
                description = description.trim(),
                triggerAtMillis = triggerAtMillis,
                hasAlarm = hasAlarm,
                category = category.name,
                repeatInterval = repeatInterval.name
            ) ?: TaskEntity(
                title = cleanTitle,
                description = description.trim(),
                triggerAtMillis = triggerAtMillis,
                hasAlarm = hasAlarm,
                category = category.name,
                repeatInterval = repeatInterval.name
            )
            val savedId = repository.saveTask(entity)
            val finalTask = if (entity.id == 0L) entity.copy(id = savedId) else entity
            val ctx = getApplication<Application>()
            if (finalTask.hasAlarm && !finalTask.isCompleted) TaskScheduler.scheduleTask(ctx, finalTask)
            else TaskScheduler.cancelTask(ctx, finalTask.id)
            closeDialog()
        }
    }

    fun toggleCompleted(task: TaskEntity) {
        viewModelScope.launch {
            val newCompleted = !task.isCompleted
            repository.toggleTaskCompleted(task.id, newCompleted)
            val ctx = getApplication<Application>()
            if (newCompleted) {
                TaskScheduler.cancelTask(ctx, task.id)
                NotificationHelper.cancelNotification(ctx, task.id)
            } else if (task.hasAlarm) {
                TaskScheduler.scheduleTask(ctx, task.copy(isCompleted = false))
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            TaskScheduler.cancelTask(ctx, task.id)
            NotificationHelper.cancelNotification(ctx, task.id)
            repository.deleteTask(task)
        }
    }
}
