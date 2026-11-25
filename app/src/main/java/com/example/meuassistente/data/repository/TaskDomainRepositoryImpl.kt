package com.example.meuassistente.data.repository

import com.example.meuassistente.data.local.dao.TaskSeriesDao
import com.example.meuassistente.data.local.entity.TaskOccurrenceEntity
import com.example.meuassistente.data.local.entity.TaskSeriesEntity
import com.example.meuassistente.domain.model.OccurrenceStatus
import com.example.meuassistente.domain.model.RecurrenceSpec
import com.example.meuassistente.domain.model.TaskOccurrence
import com.example.meuassistente.domain.model.TaskSeries
import com.example.meuassistente.domain.repository.TaskDomainRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskDomainRepositoryImpl(
    private val taskSeriesDao: TaskSeriesDao
) : TaskDomainRepository {

    override fun observeActiveSeries(): Flow<List<TaskSeries>> {
        return taskSeriesDao.observeActiveSeries().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeOccurrencesBetween(startUtc: Long, endUtc: Long): Flow<List<TaskOccurrence>> {
        return taskSeriesDao.observeOccurrencesBetween(startUtc, endUtc).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun createSeriesWithInitialOccurrence(
        series: TaskSeries,
        initialTriggerUtc: Long
    ): Long {
        val seriesId = if (series.id == 0L) {
            taskSeriesDao.insertSeries(series.toEntity())
        } else {
            series.id
        }
        taskSeriesDao.insertOccurrence(
            TaskOccurrenceEntity(
                taskSeriesId = seriesId,
                scheduledAtUtc = initialTriggerUtc,
                timezoneId = series.timezoneId,
                status = OccurrenceStatus.PENDING.name
            )
        )
        return seriesId
    }

    override suspend fun updateOccurrence(occurrence: TaskOccurrence) {
        taskSeriesDao.updateOccurrence(occurrence.toEntity())
    }

    private fun TaskSeriesEntity.toDomain(): TaskSeries = TaskSeries(
        id = id,
        title = title,
        description = description,
        category = category,
        recurrence = RecurrenceSpec.parse(recurrenceRule),
        priority = priority,
        hasAlarm = hasAlarm,
        timezoneId = timezoneId,
        createdAtUtc = createdAtUtc,
        isArchived = isArchived
    )

    private fun TaskSeries.toEntity(): TaskSeriesEntity = TaskSeriesEntity(
        id = id,
        title = title,
        description = description,
        category = category,
        recurrenceRule = recurrence.serialize(),
        priority = priority,
        hasAlarm = hasAlarm,
        timezoneId = timezoneId,
        createdAtUtc = createdAtUtc,
        isArchived = isArchived
    )

    private fun TaskOccurrenceEntity.toDomain(): TaskOccurrence = TaskOccurrence(
        id = id,
        taskSeriesId = taskSeriesId,
        scheduledAtUtc = scheduledAtUtc,
        timezoneId = timezoneId,
        snoozedUntilUtc = snoozedUntilUtc,
        status = OccurrenceStatus.fromName(status),
        completedAtUtc = completedAtUtc
    )

    private fun TaskOccurrence.toEntity(): TaskOccurrenceEntity = TaskOccurrenceEntity(
        id = id,
        taskSeriesId = taskSeriesId,
        scheduledAtUtc = scheduledAtUtc,
        timezoneId = timezoneId,
        snoozedUntilUtc = snoozedUntilUtc,
        status = status.name,
        completedAtUtc = completedAtUtc
    )
}
