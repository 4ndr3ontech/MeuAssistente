package com.example.meuassistente.domain.usecase.task

import com.example.meuassistente.domain.model.OccurrenceStatus
import com.example.meuassistente.domain.model.RecurrenceSpec
import com.example.meuassistente.domain.model.TaskOccurrence
import com.example.meuassistente.domain.model.TaskSeries
import com.example.meuassistente.domain.repository.TaskDomainRepository
import com.example.meuassistente.domain.rule.RecurrenceEngine

class CreateTaskSeriesUseCase(
    private val repository: TaskDomainRepository
) {
    suspend operator fun invoke(series: TaskSeries, initialTriggerUtc: Long): Long {
        require(series.title.isNotBlank()) { "O título da tarefa não pode estar em branco." }
        return repository.createSeriesWithInitialOccurrence(
            series = series.copy(title = series.title.trim()),
            initialTriggerUtc = initialTriggerUtc
        )
    }
}

class CompleteTaskOccurrenceUseCase(
    private val repository: TaskDomainRepository
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

        val nextTriggerUtc = RecurrenceEngine.calculateNextOccurrenceUtc(
            baseScheduledUtc = occurrence.scheduledAtUtc,
            timezoneId = occurrence.timezoneId,
            spec = series.recurrence,
            referenceNowUtc = completedAtUtc
        ) ?: return null

        return repository.createSeriesWithInitialOccurrence(
            series = series.copy(id = series.id),
            initialTriggerUtc = nextTriggerUtc
        )
    }
}

class SnoozeTaskOccurrenceUseCase(
    private val repository: TaskDomainRepository
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
        return snoozed
    }
}

class ReopenTaskOccurrenceUseCase(
    private val repository: TaskDomainRepository
) {
    suspend operator fun invoke(occurrence: TaskOccurrence): TaskOccurrence {
        val reopened = occurrence.copy(
            status = OccurrenceStatus.PENDING,
            completedAtUtc = null,
            snoozedUntilUtc = null
        )
        repository.updateOccurrence(reopened)
        return reopened
    }
}

class CancelOccurrenceUseCase(
    private val repository: TaskDomainRepository
) {
    suspend operator fun invoke(occurrence: TaskOccurrence): TaskOccurrence {
        val cancelled = occurrence.copy(
            status = OccurrenceStatus.CANCELLED,
            snoozedUntilUtc = null
        )
        repository.updateOccurrence(cancelled)
        return cancelled
    }
}

class RescheduleOccurrenceUseCase(
    private val repository: TaskDomainRepository
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
        return rescheduled
    }
}

class UpdateTaskSeriesRuleUseCase(
    private val repository: TaskDomainRepository
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
        repository.createSeriesWithInitialOccurrence(
            series = updated,
            initialTriggerUtc = updated.createdAtUtc
        )
        return updated
    }
}
