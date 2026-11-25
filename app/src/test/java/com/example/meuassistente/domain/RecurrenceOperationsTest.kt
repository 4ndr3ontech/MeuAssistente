package com.example.meuassistente.domain

import com.example.meuassistente.domain.model.OccurrenceStatus
import com.example.meuassistente.domain.model.RecurrenceSpec
import com.example.meuassistente.domain.model.RecurrenceType
import com.example.meuassistente.domain.model.TaskOccurrence
import com.example.meuassistente.domain.model.TaskSeries
import com.example.meuassistente.domain.repository.TaskDomainRepository
import com.example.meuassistente.domain.rule.RecurrenceEngine
import com.example.meuassistente.domain.usecase.task.CancelOccurrenceUseCase
import com.example.meuassistente.domain.usecase.task.RescheduleOccurrenceUseCase
import com.example.meuassistente.domain.usecase.task.UpdateTaskSeriesRuleUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

private class InMemoryRecurrenceRepo : TaskDomainRepository {
    private val seriesFlow = MutableStateFlow<List<TaskSeries>>(emptyList())
    private val occurrencesFlow = MutableStateFlow<List<TaskOccurrence>>(emptyList())
    private var nextOccId = 1L

    override fun observeActiveSeries(): Flow<List<TaskSeries>> = seriesFlow

    override fun observeOccurrencesBetween(startUtc: Long, endUtc: Long): Flow<List<TaskOccurrence>> =
        occurrencesFlow.map { list -> list.filter { it.scheduledAtUtc in startUtc..endUtc } }

    override suspend fun createSeriesWithInitialOccurrence(
        series: TaskSeries,
        initialTriggerUtc: Long
    ): Long {
        val id = if (series.id == 0L) (seriesFlow.value.size + 1L) else series.id
        seriesFlow.value = seriesFlow.value.filterNot { it.id == id } + series.copy(id = id)
        occurrencesFlow.value = occurrencesFlow.value + TaskOccurrence(
            id = nextOccId++,
            taskSeriesId = id,
            scheduledAtUtc = initialTriggerUtc,
            timezoneId = series.timezoneId,
            status = OccurrenceStatus.PENDING
        )
        return id
    }

    override suspend fun updateOccurrence(occurrence: TaskOccurrence) {
        occurrencesFlow.value = occurrencesFlow.value.map {
            if (it.id == occurrence.id) occurrence else it
        }
    }

    fun currentSeries(): List<TaskSeries> = seriesFlow.value
}

class RecurrenceOperationsTest {

    @Test
    fun previewGeneratesDeterministicWeeklyOccurrences() {
        val zone = "America/Sao_Paulo"
        val startUtc = ZonedDateTime.of(2026, 11, 25, 15, 0, 0, 0, ZoneId.of(zone))
            .toInstant().toEpochMilli()
        val preview = RecurrenceEngine.previewOccurrences(
            baseScheduledUtc = startUtc,
            timezoneId = zone,
            spec = RecurrenceSpec(RecurrenceType.WEEKLY, 1),
            count = 3
        )
        assertEquals(3, preview.size)
        assertEquals(startUtc, preview[0])
        assertEquals(startUtc + 7L * 24 * 3600_000L, preview[1])
        assertEquals(startUtc + 14L * 24 * 3600_000L, preview[2])
    }

    @Test
    fun cancelRescheduleAndUpdateSeriesRuleWorkIndependently() = runTest {
        val repo = InMemoryRecurrenceRepo()
        val cancelUseCase = CancelOccurrenceUseCase(repo)
        val rescheduleUseCase = RescheduleOccurrenceUseCase(repo)
        val updateRuleUseCase = UpdateTaskSeriesRuleUseCase(repo)

        val base = 1_800_000_000_000L
        val series = TaskSeries(
            id = 1L,
            title = "Checklist semanal",
            recurrence = RecurrenceSpec(RecurrenceType.WEEKLY, 1),
            timezoneId = "UTC",
            createdAtUtc = base
        )
        repo.createSeriesWithInitialOccurrence(series, base)

        val occ = TaskOccurrence(id = 1L, taskSeriesId = 1L, scheduledAtUtc = base, timezoneId = "UTC")
        val cancelled = cancelUseCase(occ)
        assertEquals(OccurrenceStatus.CANCELLED, cancelled.status)

        val rescheduled = rescheduleUseCase(cancelled, newScheduledAtUtc = base + 3_600_000L)
        assertEquals(OccurrenceStatus.PENDING, rescheduled.status)
        assertEquals(base + 3_600_000L, rescheduled.scheduledAtUtc)

        val updatedSeries = updateRuleUseCase(
            series = series,
            newRecurrence = RecurrenceSpec(RecurrenceType.MONTHLY, 1),
            newTitle = "Checklist mensal"
        )
        assertEquals("Checklist mensal", updatedSeries.title)
        assertEquals(RecurrenceType.MONTHLY, repo.currentSeries().first().recurrence.type)
    }
}
