package com.example.meuassistente.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.meuassistente.data.local.AppDatabase
import com.example.meuassistente.data.repository.TaskRepository

class RescheduleWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dao = AppDatabase.getInstance(applicationContext).taskDao()
        val repository = TaskRepository(dao)
        val upcomingTasks = repository.getUpcomingAlarmTasks(System.currentTimeMillis())

        upcomingTasks.forEach { task ->
            TaskScheduler.scheduleTask(applicationContext, task)
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "reschedule_upcoming_tasks_work"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<RescheduleWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
