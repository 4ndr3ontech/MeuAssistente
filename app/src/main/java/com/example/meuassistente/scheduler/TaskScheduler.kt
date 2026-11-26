package com.example.meuassistente.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.meuassistente.data.local.entity.TaskEntity

object TaskScheduler {
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_OCCURRENCE_ID = "extra_occurrence_id"
    private const val OCCURRENCE_REQUEST_CODE_OFFSET = 100_000

    fun scheduleTask(context: Context, task: TaskEntity) {
        if (!task.hasAlarm || task.isCompleted || task.triggerAtMillis <= System.currentTimeMillis()) {
            cancelTask(context, task.id)
            return
        }
        scheduleExactOrFallback(
            context = context,
            triggerAtMillis = task.triggerAtMillis,
            pendingIntent = buildTaskPendingIntent(context, task.id)
        )
    }

    fun cancelTask(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildTaskPendingIntent(context, taskId)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun scheduleOccurrence(context: Context, occurrenceId: Long, triggerAtMillis: Long, hasAlarm: Boolean) {
        if (!hasAlarm || triggerAtMillis <= System.currentTimeMillis()) {
            cancelOccurrence(context, occurrenceId)
            return
        }
        scheduleExactOrFallback(
            context = context,
            triggerAtMillis = triggerAtMillis,
            pendingIntent = buildOccurrencePendingIntent(context, occurrenceId)
        )
    }

    fun cancelOccurrence(context: Context, occurrenceId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildOccurrencePendingIntent(context, occurrenceId)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun scheduleExactOrFallback(
        context: Context,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun buildTaskPendingIntent(context: Context, taskId: Long): PendingIntent {
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

    private fun buildOccurrencePendingIntent(context: Context, occurrenceId: Long): PendingIntent {
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            action = TaskAlarmReceiver.ACTION_TRIGGER_TASK
            putExtra(EXTRA_OCCURRENCE_ID, occurrenceId)
        }
        return PendingIntent.getBroadcast(
            context,
            OCCURRENCE_REQUEST_CODE_OFFSET + occurrenceId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
