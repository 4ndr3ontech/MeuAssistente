package com.example.meuassistente.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.meuassistente.data.local.AppDatabase
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
                        val task = AppDatabase.getInstance(context).taskDao().getTaskById(taskId)
                        if (task != null && !task.isCompleted) {
                            NotificationHelper.showTaskNotification(
                                context = context,
                                taskId = task.id,
                                title = task.title,
                                description = task.description
                            )
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_TRIGGER_TASK = "com.example.meuassistente.ACTION_TRIGGER_TASK"
    }
}
