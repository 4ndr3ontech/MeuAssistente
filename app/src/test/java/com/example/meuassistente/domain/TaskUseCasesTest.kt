package com.example.meuassistente.domain

import com.example.meuassistente.domain.model.OccurrenceStatus
import com.example.meuassistente.domain.model.RecurrenceSpec
import com.example.meuassistente.domain.model.RecurrenceType
import com.example.meuassistente.domain.model.TaskOccurrence
import com.example.meuassistente.domain.model.TaskSeries
import com.example.meuassistente.domain.repository.TaskDomainRepository
import com.example.meuassistente.domain.usecase.task.CompleteTaskOccurrenceUseCase
import com.example.meuassistente.domain.usecase.task.CreateTaskSeriesUseCase
import com.example.meuassistente.domain.usecase.task.ReopenTaskOccurrenceUseCase
import com.example.meuassistente.domain.usecase.task.SnoozeTaskOccurrenceUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

private class FakeTaskDomainRepository : TaskDomainRepository {
    private val seriesList = MutableStateFlow<List<TaskSeries>>(emptyList())
    private val occurrences = MutableStateFlow<List<TaskOccurrence>>(emptyList())
    private var nextOccurrenceId = 1L

    override fun observeActiveSeries(): Flow<List<TaskSeries>> = seriesList

    override fun observeOccurrencesBetween(startUtc: Long, endUtc: Long): Flow<List<TaskOccurrence>> {
        return occurrences.map { list -> list.filter { it.scheduledAtUtc in startUtc..endUtc } }
    }

    override suspend fun createSeriesWithInitialOccurrence(
        series: TaskSeries,
        initialTriggerUtc: Long
    ): Long {
        val id = if (series.id == 0L) (seriesList.value.size + 1L) else series.id
        if (seriesList.value.none { it.id == id }) {
            seriesList.value = seriesList.value + series.copy(id = id)
        }
        occurrences.value = occurrences.value + TaskOccurrence(
            id = nextOccurrenceId++,
            taskSeriesId = id,
            scheduledAtUtc = initialTriggerUtc,
            timezoneId = series.timezoneId,
            status = OccurrenceStatus.PENDING
        )
        return id
    }

    override suspend fun updateOccurrence(occurrence: TaskOccurrence) {
        occurrences.value = occurrences.value.map {
            if (it.id == occurrence.id) occurrence else it
        }
    }

    fun currentOccurrences(): List<TaskOccurrence> = occurrences.value
}

class TaskUseCasesTest {

    @Test
    fun createCompleteSnoozeAndReopenOccurrenceFlow() = runTest {
        val repo = FakeTaskDomainRepository()
        val createUseCase = CreateTaskSeriesUseCase(repo)
        val completeUseCase = CompleteTaskOccurrenceUseCase(repo)
        val snoozeUseCase = SnoozeTaskOccurrenceUseCase(repo)
        val reopenUseCase = ReopenTaskOccurrenceUseCase(repo)

        val baseTime = 1_800_000_000_000L
        val series = TaskSeries(
            title = "Revisar backups",
            recurrence = RecurrenceSpec(RecurrenceType.DAILY, 1),
            timezoneId = "UTC",
            createdAtUtc = baseTime
        )

        val seriesId = createUseCase(series, initialTriggerUtc = baseTime + 3_600_000L)
        val initial = repo.currentOccurrences().first()
        assertEquals(seriesId, initial.taskSeriesId)

        val snoozed = snoozeUseCase(initial, snoozeMinutes = 15, nowUtc = baseTime + 1_800_000L)
        assertEquals(OccurrenceStatus.SNOOZED, snoozed.status)
        assertNotNull(snoozed.snoozedUntilUtc)

        val reopened = reopenUseCase(snoozed)
        assertEquals(OccurrenceStatus.PENDING, reopened.status)
        assertNull(reopened.snoozedUntilUtc)

        completeUseCase(
            series = series.copy(id = seriesId),
            occurrence = reopened,
            completedAtUtc = baseTime + 3_700_000L
        )
        assertEquals(2, repo.currentOccurrences().size)
        assertEquals(OccurrenceStatus.COMPLETED, repo.currentOccurrences().first().status)
        assertEquals(OccurrenceStatus.PENDING, repo.currentOccurrences().last().status)
    }
}
