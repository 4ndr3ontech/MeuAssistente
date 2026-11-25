package com.example.meuassistente.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.meuassistente.data.local.entity.CalendarEventEntity
import com.example.meuassistente.data.local.entity.ChecklistEntity
import com.example.meuassistente.data.local.entity.ChecklistItemEntity
import com.example.meuassistente.data.local.entity.FinancialObligationEntity
import com.example.meuassistente.data.local.entity.NoteItemEntity
import com.example.meuassistente.data.local.entity.NotificationLogEntity
import com.example.meuassistente.data.local.entity.PaymentRecordEntity
import com.example.meuassistente.data.local.entity.RoutineDefEntity
import com.example.meuassistente.data.local.entity.RoutineExecutionEntity
import com.example.meuassistente.data.local.entity.TaskOccurrenceEntity
import com.example.meuassistente.data.local.entity.TaskSeriesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskSeriesDao {
    @Query("SELECT * FROM task_series WHERE is_archived = 0 ORDER BY created_at_utc DESC")
    fun observeActiveSeries(): Flow<List<TaskSeriesEntity>>

    @Query("SELECT * FROM task_occurrence WHERE scheduled_at_utc BETWEEN :startUtc AND :endUtc ORDER BY scheduled_at_utc ASC")
    fun observeOccurrencesBetween(startUtc: Long, endUtc: Long): Flow<List<TaskOccurrenceEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSeries(series: TaskSeriesEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOccurrence(occurrence: TaskOccurrenceEntity): Long

    @Update
    suspend fun updateOccurrence(occurrence: TaskOccurrenceEntity)
}

@Dao
interface CalendarEventDao {
    @Query("SELECT * FROM calendar_event WHERE start_at_utc <= :endUtc AND end_at_utc >= :startUtc ORDER BY start_at_utc ASC")
    fun observeEventsInRange(startUtc: Long, endUtc: Long): Flow<List<CalendarEventEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvent(event: CalendarEventEntity): Long

    @Update
    suspend fun updateEvent(event: CalendarEventEntity)
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routine_def WHERE is_active = 1 ORDER BY created_at_utc ASC")
    fun observeActiveRoutines(): Flow<List<RoutineDefEntity>>

    @Query("SELECT * FROM routine_execution WHERE routine_id = :routineId ORDER BY executed_at_utc DESC")
    fun observeExecutionsForRoutine(routineId: Long): Flow<List<RoutineExecutionEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRoutine(routine: RoutineDefEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExecution(execution: RoutineExecutionEntity): Long
}

@Dao
interface FinanceDao {
    @Query("SELECT * FROM financial_obligation ORDER BY due_at_utc ASC")
    fun observeObligations(): Flow<List<FinancialObligationEntity>>

    @Query("SELECT * FROM payment_record ORDER BY paid_at_utc DESC")
    fun observeAllPayments(): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_record WHERE obligation_id = :obligationId ORDER BY paid_at_utc DESC")
    fun observePaymentsForObligation(obligationId: Long): Flow<List<PaymentRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertObligation(obligation: FinancialObligationEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPayment(payment: PaymentRecordEntity): Long

    @Update
    suspend fun updateObligation(obligation: FinancialObligationEntity)

    @Transaction
    suspend fun registerPaymentAndMarkPaid(
        obligation: FinancialObligationEntity,
        payment: PaymentRecordEntity
    ) {
        insertPayment(payment)
        updateObligation(obligation.copy(status = "PAID"))
    }
}

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklist WHERE is_archived = 0 ORDER BY created_at_utc DESC")
    fun observeActiveChecklists(): Flow<List<ChecklistEntity>>

    @Query("SELECT * FROM checklist_item WHERE checklist_id = :checklistId ORDER BY sort_order ASC, id ASC")
    fun observeItems(checklistId: Long): Flow<List<ChecklistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertChecklist(checklist: ChecklistEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItem(item: ChecklistItemEntity): Long

    @Update
    suspend fun updateItem(item: ChecklistItemEntity)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM note_item WHERE is_archived = 0 ORDER BY updated_at_utc DESC")
    fun observeActiveNotes(): Flow<List<NoteItemEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertNote(note: NoteItemEntity): Long

    @Update
    suspend fun updateNote(note: NoteItemEntity)
}

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM notification_log ORDER BY triggered_at_utc DESC LIMIT 200")
    fun observeRecentLogs(): Flow<List<NotificationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLog(log: NotificationLogEntity): Long
}
