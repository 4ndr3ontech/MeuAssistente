package com.example.meuassistente.data.repository

import com.example.meuassistente.data.local.dao.TaskDao
import com.example.meuassistente.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val dao: TaskDao) {
    val allTasks: Flow<List<TaskEntity>> = dao.observeAllTasks()

    suspend fun getUpcomingAlarmTasks(nowMillis: Long): List<TaskEntity> =
        dao.getUpcomingAlarmTasks(nowMillis)

    suspend fun getTaskById(taskId: Long): TaskEntity? = dao.getTaskById(taskId)

    suspend fun saveTask(task: TaskEntity): Long = dao.insertTask(task)

    suspend fun updateTask(task: TaskEntity) = dao.updateTask(task)

    suspend fun deleteTask(task: TaskEntity) = dao.deleteTask(task)

    suspend fun toggleTaskCompleted(taskId: Long, completed: Boolean) =
        dao.setTaskCompleted(taskId, completed)

    suspend fun deleteCompletedTasks(): Int = dao.deleteCompletedTasks()
}
