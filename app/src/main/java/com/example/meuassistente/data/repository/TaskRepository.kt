package com.example.meuassistente.data.repository

import com.example.meuassistente.data.local.dao.TaskDao
import com.example.meuassistente.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<TaskEntity>> = taskDao.observeAllTasks()

    suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getTaskById(id)

    suspend fun getUpcomingAlarmTasks(nowMillis: Long): List<TaskEntity> =
        taskDao.getUpcomingAlarmTasks(nowMillis)

    suspend fun saveTask(task: TaskEntity): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)

    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)

    suspend fun toggleTaskCompleted(id: Long, completed: Boolean) =
        taskDao.setTaskCompleted(id, completed)
}
