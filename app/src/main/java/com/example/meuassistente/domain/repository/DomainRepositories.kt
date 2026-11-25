package com.example.meuassistente.domain.repository

import com.example.meuassistente.domain.model.CalendarEvent
import com.example.meuassistente.domain.model.Checklist
import com.example.meuassistente.domain.model.ChecklistItem
import com.example.meuassistente.domain.model.FinancialObligation
import com.example.meuassistente.domain.model.NoteItem
import com.example.meuassistente.domain.model.PaymentRecord
import com.example.meuassistente.domain.model.RoutineDef
import com.example.meuassistente.domain.model.RoutineExecution
import com.example.meuassistente.domain.model.TaskOccurrence
import com.example.meuassistente.domain.model.TaskSeries
import kotlinx.coroutines.flow.Flow

interface TaskDomainRepository {
    fun observeActiveSeries(): Flow<List<TaskSeries>>
    fun observeOccurrencesBetween(startUtc: Long, endUtc: Long): Flow<List<TaskOccurrence>>
    suspend fun createSeriesWithInitialOccurrence(series: TaskSeries, initialTriggerUtc: Long): Long
    suspend fun updateOccurrence(occurrence: TaskOccurrence)
}

interface AgendaRepository {
    fun observeEventsInRange(startUtc: Long, endUtc: Long): Flow<List<CalendarEvent>>
    suspend fun saveEvent(event: CalendarEvent): Long
    suspend fun updateEvent(event: CalendarEvent)
}

interface RoutineRepository {
    fun observeActiveRoutines(): Flow<List<RoutineDef>>
    fun observeExecutions(routineId: Long): Flow<List<RoutineExecution>>
    suspend fun saveRoutine(routine: RoutineDef): Long
    suspend fun recordExecution(execution: RoutineExecution): Long
}

interface FinanceRepository {
    fun observeObligations(): Flow<List<FinancialObligation>>
    fun observePayments(obligationId: Long): Flow<List<PaymentRecord>>
    suspend fun saveObligation(obligation: FinancialObligation): Long
    suspend fun confirmPayment(obligation: FinancialObligation, payment: PaymentRecord)
}

interface ChecklistRepository {
    fun observeActiveChecklists(): Flow<List<Checklist>>
    fun observeItems(checklistId: Long): Flow<List<ChecklistItem>>
    suspend fun createChecklist(checklist: Checklist): Long
    suspend fun addItem(item: ChecklistItem): Long
    suspend fun updateItem(item: ChecklistItem)
}

interface NoteRepository {
    fun observeActiveNotes(): Flow<List<NoteItem>>
    suspend fun saveNote(note: NoteItem): Long
    suspend fun updateNote(note: NoteItem)
}
