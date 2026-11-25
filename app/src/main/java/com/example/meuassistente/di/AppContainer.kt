package com.example.meuassistente.di

import android.content.Context
import com.example.meuassistente.data.local.AppDatabase
import com.example.meuassistente.data.local.datastore.AppPreferencesStore
import com.example.meuassistente.data.repository.AgendaRepositoryImpl
import com.example.meuassistente.data.repository.ChecklistRepositoryImpl
import com.example.meuassistente.data.repository.FinanceRepositoryImpl
import com.example.meuassistente.data.repository.NoteRepositoryImpl
import com.example.meuassistente.data.repository.RoutineRepositoryImpl
import com.example.meuassistente.data.repository.TaskDomainRepositoryImpl
import com.example.meuassistente.data.repository.TaskRepository
import com.example.meuassistente.data.security.ApiKeyVault
import com.example.meuassistente.domain.model.FinancialObligation
import com.example.meuassistente.domain.model.ObligationStatus
import com.example.meuassistente.domain.model.OccurrenceStatus
import com.example.meuassistente.domain.model.PaymentRecord
import com.example.meuassistente.domain.model.TaskOccurrence
import com.example.meuassistente.domain.repository.AgendaRepository
import com.example.meuassistente.domain.repository.ChecklistRepository
import com.example.meuassistente.domain.repository.FinanceRepository
import com.example.meuassistente.domain.repository.NoteRepository
import com.example.meuassistente.domain.repository.RoutineRepository
import com.example.meuassistente.domain.repository.TaskDomainRepository
import com.example.meuassistente.domain.usecase.agenda.CancelCalendarEventUseCase
import com.example.meuassistente.domain.usecase.agenda.ObserveAgendaRangeUseCase
import com.example.meuassistente.domain.usecase.agenda.RescheduleCalendarEventUseCase
import com.example.meuassistente.domain.usecase.agenda.SaveCalendarEventUseCase
import com.example.meuassistente.domain.usecase.ai.ExtractLocalDraftProposalUseCase
import com.example.meuassistente.domain.usecase.backup.BuildBackupPayloadUseCase
import com.example.meuassistente.domain.usecase.backup.VerifyAndParseBackupUseCase
import com.example.meuassistente.domain.usecase.finance.ConfirmPaymentUseCase
import com.example.meuassistente.domain.usecase.finance.CreateFinancialObligationUseCase
import com.example.meuassistente.domain.usecase.finance.SummarizeObligationsUseCase
import com.example.meuassistente.domain.usecase.list.AddChecklistItemUseCase
import com.example.meuassistente.domain.usecase.list.CreateChecklistUseCase
import com.example.meuassistente.domain.usecase.list.SaveNoteUseCase
import com.example.meuassistente.domain.usecase.list.ToggleChecklistItemUseCase
import com.example.meuassistente.domain.usecase.notification.BuildSnoozeTriggerUseCase
import com.example.meuassistente.domain.usecase.notification.RecordNotificationActionUseCase
import com.example.meuassistente.domain.usecase.routine.RecordRoutineExecutionUseCase
import com.example.meuassistente.domain.usecase.routine.SaveRoutineDefUseCase
import com.example.meuassistente.domain.usecase.routine.ToggleRoutineActiveUseCase
import com.example.meuassistente.domain.usecase.statistics.ComputeProductivitySnapshotUseCase
import com.example.meuassistente.domain.usecase.statistics.ProductivitySnapshot
import com.example.meuassistente.domain.usecase.task.CancelOccurrenceUseCase
import com.example.meuassistente.domain.usecase.task.CompleteTaskOccurrenceUseCase
import com.example.meuassistente.domain.usecase.task.CreateTaskSeriesUseCase
import com.example.meuassistente.domain.usecase.task.ReopenTaskOccurrenceUseCase
import com.example.meuassistente.domain.usecase.task.RescheduleOccurrenceUseCase
import com.example.meuassistente.domain.usecase.task.SnoozeTaskOccurrenceUseCase
import com.example.meuassistente.domain.usecase.task.UpdateTaskSeriesRuleUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.TimeZone

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val preferencesStore: AppPreferencesStore by lazy {
        AppPreferencesStore(appContext)
    }

    val apiKeyVault: ApiKeyVault by lazy {
        ApiKeyVault(appContext)
    }

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(appContext)
    }

    val taskRepository: TaskRepository by lazy {
        TaskRepository(database.taskDao())
    }

    val taskDomainRepository: TaskDomainRepository by lazy {
        TaskDomainRepositoryImpl(database.taskSeriesDao())
    }

    val agendaRepository: AgendaRepository by lazy {
        AgendaRepositoryImpl(database.calendarEventDao())
    }

    val routineRepository: RoutineRepository by lazy {
        RoutineRepositoryImpl(database.routineDao())
    }

    val financeRepository: FinanceRepository by lazy {
        FinanceRepositoryImpl(database.financeDao())
    }

    val checklistRepository: ChecklistRepository by lazy {
        ChecklistRepositoryImpl(database.checklistDao())
    }

    val noteRepository: NoteRepository by lazy {
        NoteRepositoryImpl(database.noteDao())
    }

    val createTaskSeriesUseCase: CreateTaskSeriesUseCase by lazy {
        CreateTaskSeriesUseCase(taskDomainRepository)
    }

    val completeTaskOccurrenceUseCase: CompleteTaskOccurrenceUseCase by lazy {
        CompleteTaskOccurrenceUseCase(taskDomainRepository)
    }

    val snoozeTaskOccurrenceUseCase: SnoozeTaskOccurrenceUseCase by lazy {
        SnoozeTaskOccurrenceUseCase(taskDomainRepository)
    }

    val reopenTaskOccurrenceUseCase: ReopenTaskOccurrenceUseCase by lazy {
        ReopenTaskOccurrenceUseCase(taskDomainRepository)
    }

    val cancelOccurrenceUseCase: CancelOccurrenceUseCase by lazy {
        CancelOccurrenceUseCase(taskDomainRepository)
    }

    val rescheduleOccurrenceUseCase: RescheduleOccurrenceUseCase by lazy {
        RescheduleOccurrenceUseCase(taskDomainRepository)
    }

    val updateTaskSeriesRuleUseCase: UpdateTaskSeriesRuleUseCase by lazy {
        UpdateTaskSeriesRuleUseCase(taskDomainRepository)
    }

    val saveCalendarEventUseCase: SaveCalendarEventUseCase by lazy {
        SaveCalendarEventUseCase(agendaRepository)
    }

    val rescheduleCalendarEventUseCase: RescheduleCalendarEventUseCase by lazy {
        RescheduleCalendarEventUseCase(agendaRepository)
    }

    val cancelCalendarEventUseCase: CancelCalendarEventUseCase by lazy {
        CancelCalendarEventUseCase(agendaRepository)
    }

    val observeAgendaRangeUseCase: ObserveAgendaRangeUseCase by lazy {
        ObserveAgendaRangeUseCase(agendaRepository)
    }

    val recordNotificationActionUseCase: RecordNotificationActionUseCase by lazy {
        RecordNotificationActionUseCase(database.notificationLogDao())
    }

    val buildSnoozeTriggerUseCase: BuildSnoozeTriggerUseCase by lazy {
        BuildSnoozeTriggerUseCase()
    }

    val saveRoutineDefUseCase: SaveRoutineDefUseCase by lazy {
        SaveRoutineDefUseCase(routineRepository)
    }

    val recordRoutineExecutionUseCase: RecordRoutineExecutionUseCase by lazy {
        RecordRoutineExecutionUseCase(routineRepository)
    }

    val toggleRoutineActiveUseCase: ToggleRoutineActiveUseCase by lazy {
        ToggleRoutineActiveUseCase(routineRepository)
    }

    val createFinancialObligationUseCase: CreateFinancialObligationUseCase by lazy {
        CreateFinancialObligationUseCase(financeRepository)
    }

    val confirmPaymentUseCase: ConfirmPaymentUseCase by lazy {
        ConfirmPaymentUseCase(financeRepository)
    }

    val summarizeObligationsUseCase: SummarizeObligationsUseCase by lazy {
        SummarizeObligationsUseCase()
    }

    val createChecklistUseCase: CreateChecklistUseCase by lazy {
        CreateChecklistUseCase(checklistRepository)
    }

    val addChecklistItemUseCase: AddChecklistItemUseCase by lazy {
        AddChecklistItemUseCase(checklistRepository)
    }

    val toggleChecklistItemUseCase: ToggleChecklistItemUseCase by lazy {
        ToggleChecklistItemUseCase(checklistRepository)
    }

    val saveNoteUseCase: SaveNoteUseCase by lazy {
        SaveNoteUseCase(noteRepository)
    }

    val computeProductivitySnapshotUseCase: ComputeProductivitySnapshotUseCase by lazy {
        ComputeProductivitySnapshotUseCase()
    }

    val extractLocalDraftProposalUseCase: ExtractLocalDraftProposalUseCase by lazy {
        ExtractLocalDraftProposalUseCase()
    }

    val buildBackupPayloadUseCase: BuildBackupPayloadUseCase by lazy {
        BuildBackupPayloadUseCase()
    }

    val verifyAndParseBackupUseCase: VerifyAndParseBackupUseCase by lazy {
        VerifyAndParseBackupUseCase()
    }

    fun observeHomeProductivitySnapshot(): Flow<ProductivitySnapshot> {
        val defaultTz = TimeZone.getDefault().id
        val legacyTasksFlow = database.taskDao().observeAllTasks()
        val seriesOccurrencesFlow = database.taskSeriesDao().observeOccurrencesBetween(0L, Long.MAX_VALUE)
        val obligationsFlow = database.financeDao().observeObligations()
        val paymentsFlow = database.financeDao().observeAllPayments()

        return combine(
            legacyTasksFlow,
            seriesOccurrencesFlow,
            obligationsFlow,
            paymentsFlow
        ) { legacyTasks, seriesOccurrences, obligationEntities, paymentEntities ->
            val mappedLegacy = legacyTasks.map { task ->
                TaskOccurrence(
                    id = task.id,
                    taskSeriesId = 0L,
                    scheduledAtUtc = task.triggerAtMillis,
                    timezoneId = defaultTz,
                    status = if (task.isCompleted) OccurrenceStatus.COMPLETED else OccurrenceStatus.PENDING,
                    completedAtUtc = if (task.isCompleted) task.triggerAtMillis else null
                )
            }
            val mappedSeries = seriesOccurrences.map { occ ->
                TaskOccurrence(
                    id = occ.id,
                    taskSeriesId = occ.taskSeriesId,
                    scheduledAtUtc = occ.scheduledAtUtc,
                    timezoneId = occ.timezoneId,
                    snoozedUntilUtc = occ.snoozedUntilUtc,
                    status = OccurrenceStatus.fromName(occ.status),
                    completedAtUtc = occ.completedAtUtc
                )
            }
            val obligations = obligationEntities.map { ob ->
                FinancialObligation(
                    id = ob.id,
                    title = ob.title,
                    amountCents = ob.amountCents,
                    dueAtUtc = ob.dueAtUtc,
                    timezoneId = ob.timezoneId,
                    status = ObligationStatus.fromName(ob.status),
                    createdAtUtc = ob.createdAtUtc
                )
            }
            val payments = paymentEntities.map { pay ->
                PaymentRecord(
                    id = pay.id,
                    obligationId = pay.obligationId,
                    paidAmountCents = pay.paidAmountCents,
                    paidAtUtc = pay.paidAtUtc
                )
            }

            computeProductivitySnapshotUseCase(
                occurrences = mappedLegacy + mappedSeries,
                routineExecutions = emptyList(),
                obligations = obligations,
                payments = payments,
                nowUtc = System.currentTimeMillis()
            )
        }
    }
}
