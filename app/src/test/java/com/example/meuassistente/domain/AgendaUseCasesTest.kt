package com.example.meuassistente.domain

import com.example.meuassistente.domain.model.CalendarEvent
import com.example.meuassistente.domain.repository.AgendaRepository
import com.example.meuassistente.domain.usecase.agenda.CancelCalendarEventUseCase
import com.example.meuassistente.domain.usecase.agenda.RescheduleCalendarEventUseCase
import com.example.meuassistente.domain.usecase.agenda.SaveCalendarEventUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeAgendaRepository : AgendaRepository {
    private val events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    private var nextId = 1L

    override fun observeEventsInRange(startUtc: Long, endUtc: Long): Flow<List<CalendarEvent>> {
        return events.map { list ->
            list.filter { it.startAtUtc <= endUtc && it.endAtUtc >= startUtc }
        }
    }

    override suspend fun saveEvent(event: CalendarEvent): Long {
        val id = if (event.id == 0L) nextId++ else event.id
        val saved = event.copy(id = id)
        events.value = events.value.filterNot { it.id == id } + saved
        return id
    }

    override suspend fun updateEvent(event: CalendarEvent) {
        events.value = events.value.map { if (it.id == event.id) event else it }
    }

    fun currentEvents(): List<CalendarEvent> = events.value
}

class AgendaUseCasesTest {

    @Test(expected = IllegalArgumentException::class)
    fun saveEventRejectsEndBeforeStart() = runTest {
        val repo = FakeAgendaRepository()
        val saveUseCase = SaveCalendarEventUseCase(repo)
        val base = 1_800_000_000_000L

        saveUseCase(
            CalendarEvent(
                title = "Reunião inválida",
                startAtUtc = base + 3_600_000L,
                endAtUtc = base,
                timezoneId = "America/Sao_Paulo",
                createdAtUtc = base
            )
        )
    }

    @Test
    fun saveRescheduleAndCancelEventPreservesIdentity() = runTest {
        val repo = FakeAgendaRepository()
        val saveUseCase = SaveCalendarEventUseCase(repo)
        val rescheduleUseCase = RescheduleCalendarEventUseCase(repo)
        val cancelUseCase = CancelCalendarEventUseCase(repo)

        val base = 1_800_000_000_000L
        val id = saveUseCase(
            CalendarEvent(
                title = "  Alinhamento técnico  ",
                startAtUtc = base,
                endAtUtc = base + 1_800_000L,
                timezoneId = "America/Sao_Paulo",
                createdAtUtc = base
            )
        )

        val created = repo.currentEvents().first { it.id == id }
        assertEquals("Alinhamento técnico", created.title)

        val rescheduled = rescheduleUseCase(
            event = created,
            newStartAtUtc = base + 7_200_000L,
            newEndAtUtc = base + 9_000_000L
        )
        assertEquals(base + 7_200_000L, rescheduled.startAtUtc)

        val cancelled = cancelUseCase(rescheduled)
        assertEquals("CANCELLED", cancelled.status)
    }
}
