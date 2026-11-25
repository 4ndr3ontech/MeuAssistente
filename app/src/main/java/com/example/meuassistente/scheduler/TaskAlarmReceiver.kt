package com.example.meuassistente.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.meuassistente.data.local.AppDatabase
import com.example.meuassistente.data.local.entity.RepeatInterval
import com.example.meuassistente.data.local.entity.TaskCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                RescheduleWorker.enqueue(context)
            }
            ACTION_TRIGGER_TASK -> {
                val taskId = intent.getLongExtra(TaskScheduler.EXTRA_TASK_ID, -1L)
                if (taskId == -1L) return
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val dao = AppDatabase.getInstance(context).taskDao()
                        val task = dao.getTaskById(taskId)
                        if (task != null && !task.isCompleted) {
                            if (task.category == TaskCategory.URGENTE.name) {
                                UrgentAlarmService.start(context, task.id, task.title, task.description)
                            } else {
                                NotificationHelper.showTaskNotification(context, task)
                            }

                            val repeat = RepeatInterval.fromName(task.repeatInterval)
                            val nextMillis = repeat.nextTriggerMillis(task.triggerAtMillis)
                            if (nextMillis != null && task.hasAlarm) {
                                val nextTask = task.copy(triggerAtMillis = nextMillis)
                                dao.updateTask(nextTask)
                                TaskScheduler.scheduleTask(context, nextTask)
                            }
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_SNOOZE_TASK -> {
                val taskId = intent.getLongExtra(TaskScheduler.EXTRA_TASK_ID, -1L)
                if (taskId == -1L) return
                UrgentAlarmService.stop(context)
                NotificationHelper.cancelNotification(context, taskId)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val dao = AppDatabase.getInstance(context).taskDao()
                        val task = dao.getTaskById(taskId)
                        if (task != null && !task.isCompleted) {
                            val snoozedTask = task.copy(
                                triggerAtMillis = System.currentTimeMillis() + 5 * 60_000L,
                                hasAlarm = true
                            )
                            dao.updateTask(snoozedTask)
                            TaskScheduler.scheduleTask(context, snoozedTask)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_COMPLETE_TASK -> {
                val taskId = intent.getLongExtra(TaskScheduler.EXTRA_TASK_ID, -1L)
                if (taskId == -1L) return
                UrgentAlarmService.stop(context)
                NotificationHelper.cancelNotification(context, taskId)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val dao = AppDatabase.getInstance(context).taskDao()
                        dao.setTaskCompleted(taskId, true)
                        TaskScheduler.cancelTask(context, taskId)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_TRIGGER_TASK = "com.example.meuassistente.ACTION_TRIGGER_TASK"
        const val ACTION_SNOOZE_TASK = "com.example.meuassistente.ACTION_SNOOZE_TASK"
        const val ACTION_COMPLETE_TASK = "com.example.meuassistente.ACTION_COMPLETE_TASK"
    }
}
