package com.example.meuassistente.domain.usecase.task

import com.example.meuassistente.domain.model.OccurrenceStatus
import com.example.meuassistente.domain.model.RecurrenceSpec
import com.example.meuassistente.domain.model.TaskOccurrence
import com.example.meuassistente.domain.model.TaskSeries
import com.example.meuassistente.domain.repository.TaskDomainRepository
import com.example.meuassistente.domain.rule.RecurrenceEngine

class CreateTaskSeriesUseCase(
    private val repository: TaskDomainRepository,
    private val onScheduleOccurrence: ((occurrenceId: Long, triggerUtc: Long, hasAlarm: Boolean) -> Unit)? = null
) {
    suspend operator fun invoke(series: TaskSeries, initialTriggerUtc: Long): Long {
        require(series.title.isNotBlank()) { "O título da tarefa não pode estar em branco." }
        val occurrenceId = repository.createSeriesWithInitialOccurrence(
            series = series.copy(title = series.title.trim()),
            initialTriggerUtc = initialTriggerUtc
        )
        onScheduleOccurrence?.invoke(occurrenceId, initialTriggerUtc, series.hasAlarm)
        return occurrenceId
    }
}

class CompleteTaskOccurrenceUseCase(
    private val repository: TaskDomainRepository,
    private val onScheduleOccurrence: ((occurrenceId: Long, triggerUtc: Long, hasAlarm: Boolean) -> Unit)? = null,
    private val onCancelOccurrence: ((occurrenceId: Long) -> Unit)? = null
) {
    suspend operator fun invoke(
        series: TaskSeries,
        occurrence: TaskOccurrence,
        completedAtUtc: Long
    ): Long? {
        val updatedOccurrence = occurrence.copy(
            status = OccurrenceStatus.COMPLETED,
            completedAtUtc = completedAtUtc,
            snoozedUntilUtc = null
        )
        repository.updateOccurrence(updatedOccurrence)
        onCancelOccurrence?.invoke(occurrence.id)

        val nextTriggerUtc = RecurrenceEngine.calculateNextOccurrenceUtc(
            baseScheduledUtc = occurrence.scheduledAtUtc,
            timezoneId = occurrence.timezoneId,
            spec = series.recurrence,
            referenceNowUtc = completedAtUtc
        ) ?: return null

        val nextOccurrenceId = repository.createSeriesWithInitialOccurrence(
            series = series.copy(id = series.id),
            initialTriggerUtc = nextTriggerUtc
        )
        onScheduleOccurrence?.invoke(nextOccurrenceId, nextTriggerUtc, series.hasAlarm)
        return nextOccurrenceId
    }
}

class SnoozeTaskOccurrenceUseCase(
    private val repository: TaskDomainRepository,
    private val onScheduleOccurrence: ((occurrenceId: Long, triggerUtc: Long, hasAlarm: Boolean) -> Unit)? = null
) {
    suspend operator fun invoke(
        occurrence: TaskOccurrence,
        snoozeMinutes: Int,
        nowUtc: Long
    ): TaskOccurrence {
        val snoozed = RecurrenceEngine.snoozeOccurrence(
            occurrence = occurrence,
            snoozeMinutes = snoozeMinutes,
            nowUtc = nowUtc
        )
        repository.updateOccurrence(snoozed)
        val targetTrigger = snoozed.snoozedUntilUtc ?: snoozed.scheduledAtUtc
        onScheduleOccurrence?.invoke(snoozed.id, targetTrigger, true)
        return snoozed
    }
}

class ReopenTaskOccurrenceUseCase(
    private val repository: TaskDomainRepository,
    private val onScheduleOccurrence: ((occurrenceId: Long, triggerUtc: Long, hasAlarm: Boolean) -> Unit)? = null
) {
    suspend operator fun invoke(occurrence: TaskOccurrence): TaskOccurrence {
        val reopened = occurrence.copy(
            status = OccurrenceStatus.PENDING,
            completedAtUtc = null,
            snoozedUntilUtc = null
        )
        repository.updateOccurrence(reopened)
        onScheduleOccurrence?.invoke(reopened.id, reopened.scheduledAtUtc, true)
        return reopened
    }
}

class CancelOccurrenceUseCase(
    private val repository: TaskDomainRepository,
    private val onCancelOccurrence: ((occurrenceId: Long) -> Unit)? = null
) {
    suspend operator fun invoke(occurrence: TaskOccurrence): TaskOccurrence {
        val cancelled = occurrence.copy(
            status = OccurrenceStatus.CANCELLED,
            snoozedUntilUtc = null
        )
        repository.updateOccurrence(cancelled)
        onCancelOccurrence?.invoke(cancelled.id)
        return cancelled
    }
}

class RescheduleOccurrenceUseCase(
    private val repository: TaskDomainRepository,
    private val onScheduleOccurrence: ((occurrenceId: Long, triggerUtc: Long, hasAlarm: Boolean) -> Unit)? = null
) {
    suspend operator fun invoke(
        occurrence: TaskOccurrence,
        newScheduledAtUtc: Long,
        newTimezoneId: String = occurrence.timezoneId
    ): TaskOccurrence {
        val rescheduled = occurrence.copy(
            scheduledAtUtc = newScheduledAtUtc,
            timezoneId = newTimezoneId,
            snoozedUntilUtc = null,
            completedAtUtc = null,
            status = OccurrenceStatus.PENDING
        )
        repository.updateOccurrence(rescheduled)
        onScheduleOccurrence?.invoke(rescheduled.id, newScheduledAtUtc, true)
        return rescheduled
    }
}

class UpdateTaskSeriesRuleUseCase(
    private val repository: TaskDomainRepository,
    private val onScheduleOccurrence: ((occurrenceId: Long, triggerUtc: Long, hasAlarm: Boolean) -> Unit)? = null
) {
    suspend operator fun invoke(
        series: TaskSeries,
        newRecurrence: RecurrenceSpec,
        newTitle: String = series.title,
        newDescription: String = series.description,
        newCategory: String = series.category,
        newPriority: Int = series.priority,
        newHasAlarm: Boolean = series.hasAlarm
    ): TaskSeries {
        require(newTitle.isNotBlank()) { "O título da série não pode estar em branco." }
        val updated = series.copy(
            title = newTitle.trim(),
            description = newDescription,
            category = newCategory,
            recurrence = newRecurrence,
            priority = newPriority,
            hasAlarm = newHasAlarm
        )
        val occurrenceId = repository.createSeriesWithInitialOccurrence(
            series = updated,
            initialTriggerUtc = updated.createdAtUtc
        )
        onScheduleOccurrence?.invoke(occurrenceId, updated.createdAtUtc, updated.hasAlarm)
        return updated
    }
}
