package com.example.meuassistente.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.meuassistente.MeuAssistenteApp
import com.example.meuassistente.domain.model.OccurrenceStatus
import kotlinx.coroutines.flow.first

class RescheduleWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val container = (applicationContext as MeuAssistenteApp).container

        val upcomingTasks = container.taskRepository.getUpcomingAlarmTasks(now)
        upcomingTasks.forEach { task ->
            TaskScheduler.scheduleTask(applicationContext, task)
        }

        val activeSeriesMap = container.taskDomainRepository
            .observeActiveSeries()
            .first()
            .associateBy { it.id }

        val occurrences = container.taskDomainRepository
            .observeOccurrencesBetween(0L, Long.MAX_VALUE)
            .first()

        occurrences.forEach { occ ->
            val isPendingOrSnoozed = occ.status == OccurrenceStatus.PENDING || occ.status == OccurrenceStatus.SNOOZED
            val effectiveTrigger = occ.snoozedUntilUtc ?: occ.scheduledAtUtc
            val hasAlarm = activeSeriesMap[occ.taskSeriesId]?.hasAlarm ?: true
            if (isPendingOrSnoozed && hasAlarm && effectiveTrigger > now) {
                TaskScheduler.scheduleOccurrence(
                    context = applicationContext,
                    occurrenceId = occ.id,
                    triggerAtMillis = effectiveTrigger,
                    hasAlarm = true
                )
            }
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "reschedule_upcoming_tasks_work"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<RescheduleWorker>().build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
