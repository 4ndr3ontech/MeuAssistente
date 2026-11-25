package com.example.meuassistente.domain.usecase.agenda

import com.example.meuassistente.domain.model.CalendarEvent
import com.example.meuassistente.domain.repository.AgendaRepository
import kotlinx.coroutines.flow.Flow

class SaveCalendarEventUseCase(
    private val repository: AgendaRepository
) {
    suspend operator fun invoke(event: CalendarEvent): Long {
        require(event.title.isNotBlank()) { "O título do compromisso não pode estar em branco." }
        require(event.endAtUtc >= event.startAtUtc) {
            "O horário de término do compromisso não pode ser anterior ao início."
        }
        return repository.saveEvent(event.copy(title = event.title.trim()))
    }
}

class RescheduleCalendarEventUseCase(
    private val repository: AgendaRepository
) {
    suspend operator fun invoke(
        event: CalendarEvent,
        newStartAtUtc: Long,
        newEndAtUtc: Long,
        newTimezoneId: String = event.timezoneId
    ): CalendarEvent {
        require(newEndAtUtc >= newStartAtUtc) {
            "O novo horário de término não pode ser anterior ao início."
        }
        val updated = event.copy(
            startAtUtc = newStartAtUtc,
            endAtUtc = newEndAtUtc,
            timezoneId = newTimezoneId,
            status = if (event.status == "CANCELLED") "SCHEDULED" else event.status
        )
        repository.updateEvent(updated)
        return updated
    }
}

class CancelCalendarEventUseCase(
    private val repository: AgendaRepository
) {
    suspend operator fun invoke(event: CalendarEvent): CalendarEvent {
        val cancelled = event.copy(status = "CANCELLED")
        repository.updateEvent(cancelled)
        return cancelled
    }
}

class ObserveAgendaRangeUseCase(
    private val repository: AgendaRepository
) {
    operator fun invoke(startUtc: Long, endUtc: Long): Flow<List<CalendarEvent>> {
        require(endUtc >= startUtc) { "O fim do intervalo deve ser maior ou igual ao início." }
        return repository.observeEventsInRange(startUtc, endUtc)
    }
}
