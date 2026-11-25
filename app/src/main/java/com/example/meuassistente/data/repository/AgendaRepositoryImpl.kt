package com.example.meuassistente.data.repository

import com.example.meuassistente.data.local.dao.CalendarEventDao
import com.example.meuassistente.data.local.entity.CalendarEventEntity
import com.example.meuassistente.domain.model.CalendarEvent
import com.example.meuassistente.domain.repository.AgendaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AgendaRepositoryImpl(
    private val calendarEventDao: CalendarEventDao
) : AgendaRepository {

    override fun observeEventsInRange(startUtc: Long, endUtc: Long): Flow<List<CalendarEvent>> {
        return calendarEventDao.observeEventsInRange(startUtc, endUtc).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveEvent(event: CalendarEvent): Long {
        return calendarEventDao.insertEvent(event.toEntity())
    }

    override suspend fun updateEvent(event: CalendarEvent) {
        calendarEventDao.updateEvent(event.toEntity())
    }

    private fun CalendarEventEntity.toDomain(): CalendarEvent = CalendarEvent(
        id = id,
        title = title,
        description = description,
        startAtUtc = startAtUtc,
        endAtUtc = endAtUtc,
        timezoneId = timezoneId,
        status = status,
        createdAtUtc = createdAtUtc
    )

    private fun CalendarEvent.toEntity(): CalendarEventEntity = CalendarEventEntity(
        id = id,
        title = title,
        description = description,
        startAtUtc = startAtUtc,
        endAtUtc = endAtUtc,
        timezoneId = timezoneId,
        status = status,
        createdAtUtc = createdAtUtc
    )
}
