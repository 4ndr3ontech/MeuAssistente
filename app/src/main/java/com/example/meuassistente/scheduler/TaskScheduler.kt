package com.example.meuassistente.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.meuassistente.data.local.entity.TaskEntity

object TaskScheduler {
    const val EXTRA_TASK_ID = "extra_task_id"

    fun scheduleTask(context: Context, task: TaskEntity) {
        if (!task.hasAlarm || task.isCompleted || task.triggerAtMillis <= System.currentTimeMillis()) {
            cancelTask(context, task.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildAlarmPendingIntent(context, task.id)

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                task.triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                task.triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelTask(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildAlarmPendingIntent(context, taskId)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun buildAlarmPendingIntent(context: Context, taskId: Long): PendingIntent {
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_TRIGGER_TASK
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
