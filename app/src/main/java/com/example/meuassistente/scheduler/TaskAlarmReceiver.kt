package com.example.meuassistente.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.meuassistente.MeuAssistenteApp
import com.example.meuassistente.data.local.AppDatabase
import com.example.meuassistente.data.local.entity.RepeatInterval
import com.example.meuassistente.data.local.entity.TaskCategory
import com.example.meuassistente.domain.model.OccurrenceStatus
import com.example.meuassistente.widget.TaskWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
                val occurrenceId = intent.getLongExtra(TaskScheduler.EXTRA_OCCURRENCE_ID, -1L)
                if (taskId == -1L && occurrenceId == -1L) return

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getInstance(context)
                        if (occurrenceId != -1L) {
                            val occurrences = db.taskSeriesDao()
                                .observeOccurrencesBetween(0L, Long.MAX_VALUE)
                                .first()
                            val occ = occurrences.find { it.id == occurrenceId }
                            if (occ != null && occ.status != OccurrenceStatus.COMPLETED.name && occ.status != OccurrenceStatus.CANCELLED.name) {
                                val series = db.taskSeriesDao()
                                    .observeActiveSeries()
                                    .first()
                                    .find { it.id == occ.taskSeriesId }
                                val title = series?.title ?: "Tarefa Agendada"
                                val desc = series?.description ?: ""
                                val catLabel = TaskCategory.fromName(series?.category ?: "GERAL").label

                                UrgentAlarmService.start(
                                    context = context,
                                    occurrenceId = occ.id,
                                    title = title,
                                    description = desc
                                )
                                NotificationHelper.showPersistentReminder(
                                    context = context,
                                    notificationId = (occ.id + 200_000L).toInt(),
                                    occurrenceId = occ.id,
                                    categoryLabel = catLabel,
                                    title = title,
                                    description = desc
                                )
                            }
                        } else if (taskId != -1L) {
                            val dao = db.taskDao()
                            val task = dao.getTaskById(taskId)
                            if (task != null && !task.isCompleted) {
                                UrgentAlarmService.start(
                                    context = context,
                                    taskId = task.id,
                                    title = task.title,
                                    description = task.description
                                )
                                NotificationHelper.showTaskNotification(context, task)

                                val repeat = RepeatInterval.fromName(task.repeatInterval)
                                val nextMillis = repeat.nextTriggerMillis(task.triggerAtMillis)
                                if (nextMillis != null && task.hasAlarm) {
                                    val nextTask = task.copy(triggerAtMillis = nextMillis)
                                    dao.updateTask(nextTask)
                                    TaskScheduler.scheduleTask(context, nextTask)
                                }
                            }
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_SNOOZE_TASK -> {
                val taskId = intent.getLongExtra(TaskScheduler.EXTRA_TASK_ID, -1L)
                val occurrenceId = intent.getLongExtra(TaskScheduler.EXTRA_OCCURRENCE_ID, -1L)
                if (taskId == -1L && occurrenceId == -1L) return

                UrgentAlarmService.stop(context)
                if (taskId != -1L) NotificationHelper.cancelNotification(context, taskId.toInt())
                if (occurrenceId != -1L) NotificationHelper.cancelNotification(context, (occurrenceId + 200_000L).toInt())

                val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 5)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getInstance(context)
                        if (occurrenceId != -1L) {
                            val app = context.applicationContext as? MeuAssistenteApp
                            val container = app?.container
                            if (container != null) {
                                val occurrences = container.taskDomainRepository
                                    .observeOccurrencesBetween(0L, Long.MAX_VALUE)
                                    .first()
                                val occ = occurrences.find { it.id == occurrenceId }
                                if (occ != null) {
                                    container.snoozeTaskOccurrenceUseCase(
                                        occurrence = occ,
                                        snoozeMinutes = snoozeMinutes,
                                        nowUtc = System.currentTimeMillis()
                                    )
                                }
                            }
                        } else if (taskId != -1L) {
                            val dao = db.taskDao()
                            val task = dao.getTaskById(taskId)
                            if (task != null && !task.isCompleted) {
                                val snoozedTask = task.copy(
                                    triggerAtMillis = System.currentTimeMillis() + snoozeMinutes * 60_000L,
                                    hasAlarm = true
                                )
                                dao.updateTask(snoozedTask)
                                TaskScheduler.scheduleTask(context, snoozedTask)
                            }
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_COMPLETE_TASK -> {
                val taskId = intent.getLongExtra(TaskScheduler.EXTRA_TASK_ID, -1L)
                val occurrenceId = intent.getLongExtra(TaskScheduler.EXTRA_OCCURRENCE_ID, -1L)
                if (taskId == -1L && occurrenceId == -1L) return

                UrgentAlarmService.stop(context)
                if (taskId != -1L) NotificationHelper.cancelNotification(context, taskId.toInt())
                if (occurrenceId != -1L) NotificationHelper.cancelNotification(context, (occurrenceId + 200_000L).toInt())

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getInstance(context)
                        if (occurrenceId != -1L) {
                            val app = context.applicationContext as? MeuAssistenteApp
                            val container = app?.container
                            if (container != null) {
                                val occurrences = container.taskDomainRepository
                                    .observeOccurrencesBetween(0L, Long.MAX_VALUE)
                                    .first()
                                val occ = occurrences.find { it.id == occurrenceId }
                                if (occ != null) {
                                    val series = container.taskDomainRepository
                                        .observeActiveSeries()
                                        .first()
                                        .find { it.id == occ.taskSeriesId }
                                    if (series != null) {
                                        container.completeTaskOccurrenceUseCase(
                                            series = series,
                                            occurrence = occ,
                                            completedAtUtc = System.currentTimeMillis()
                                        )
                                    } else {
                                        container.taskDomainRepository.updateOccurrence(
                                            occ.copy(
                                                status = OccurrenceStatus.COMPLETED,
                                                completedAtUtc = System.currentTimeMillis(),
                                                snoozedUntilUtc = null
                                            )
                                        )
                                    }
                                    TaskScheduler.cancelOccurrence(context, occurrenceId)
                                }
                            }
                        } else if (taskId != -1L) {
                            val dao = db.taskDao()
                            dao.setTaskCompleted(taskId, true)
                            TaskScheduler.cancelTask(context, taskId)
                        }
                        TaskWidgetProvider.refreshAll(context)
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
        const val EXTRA_SNOOZE_MINUTES = "com.example.meuassistente.EXTRA_SNOOZE_MINUTES"
    }
}
