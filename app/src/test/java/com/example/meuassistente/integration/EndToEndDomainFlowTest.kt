package com.example.meuassistente.integration

import com.example.meuassistente.data.security.LogRedactor
import com.example.meuassistente.domain.model.FinancialObligation
import com.example.meuassistente.domain.model.ObligationStatus
import com.example.meuassistente.domain.model.OccurrenceStatus
import com.example.meuassistente.domain.model.PaymentRecord
import com.example.meuassistente.domain.model.RecurrenceSpec
import com.example.meuassistente.domain.model.RecurrenceType
import com.example.meuassistente.domain.model.TaskOccurrence
import com.example.meuassistente.domain.model.TaskSeries
import com.example.meuassistente.domain.repository.FinanceRepository
import com.example.meuassistente.domain.repository.TaskDomainRepository
import com.example.meuassistente.domain.usecase.ai.ExtractLocalDraftProposalUseCase
import com.example.meuassistente.domain.usecase.ai.ProposedActionType
import com.example.meuassistente.domain.usecase.backup.BuildBackupPayloadUseCase
import com.example.meuassistente.domain.usecase.backup.VerifyAndParseBackupUseCase
import com.example.meuassistente.domain.usecase.finance.ConfirmPaymentUseCase
import com.example.meuassistente.domain.usecase.finance.CreateFinancialObligationUseCase
import com.example.meuassistente.domain.usecase.statistics.ComputeProductivitySnapshotUseCase
import com.example.meuassistente.domain.usecase.task.CompleteTaskOccurrenceUseCase
import com.example.meuassistente.domain.usecase.task.CreateTaskSeriesUseCase
import com.example.meuassistente.domain.usecase.task.SnoozeTaskOccurrenceUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class IntegrationTaskRepo : TaskDomainRepository {
    val series = MutableStateFlow<List<TaskSeries>>(emptyList())
    val occurrences = MutableStateFlow<List<TaskOccurrence>>(emptyList())
    private var nextOccId = 1L

    override fun observeActiveSeries(): Flow<List<TaskSeries>> = series

    override fun observeOccurrencesBetween(startUtc: Long, endUtc: Long): Flow<List<TaskOccurrence>> =
        occurrences.map { list -> list.filter { it.scheduledAtUtc in startUtc..endUtc } }

    override suspend fun createSeriesWithInitialOccurrence(
        series: TaskSeries,
        initialTriggerUtc: Long
    ): Long {
        val id = if (series.id == 0L) (this.series.value.size + 1L) else series.id
        this.series.value = this.series.value.filterNot { it.id == id } + series.copy(id = id)
        occurrences.value = occurrences.value + TaskOccurrence(
            id = nextOccId++,
            taskSeriesId = id,
            scheduledAtUtc = initialTriggerUtc,
            timezoneId = series.timezoneId,
            status = OccurrenceStatus.PENDING
        )
        return id
    }

    override suspend fun updateOccurrence(occurrence: TaskOccurrence) {
        occurrences.value = occurrences.value.map { if (it.id == occurrence.id) occurrence else it }
    }
}

private class IntegrationFinanceRepo : FinanceRepository {
    val obligations = MutableStateFlow<List<FinancialObligation>>(emptyList())
    val payments = MutableStateFlow<List<PaymentRecord>>(emptyList())
    private var nextId = 1L

    override fun observeObligations(): Flow<List<FinancialObligation>> = obligations

    override fun observePayments(obligationId: Long): Flow<List<PaymentRecord>> =
        payments.map { list -> list.filter { it.obligationId == obligationId } }

    override suspend fun saveObligation(obligation: FinancialObligation): Long {
        val id = if (obligation.id == 0L) nextId++ else obligation.id
        obligations.value = obligations.value.filterNot { it.id == id } + obligation.copy(id = id)
        return id
    }

    override suspend fun confirmPayment(obligation: FinancialObligation, payment: PaymentRecord) {
        payments.value = payments.value + payment
        obligations.value = obligations.value.map {
            if (it.id == obligation.id) it.copy(status = ObligationStatus.PAID) else it
        }
    }
}

class EndToEndDomainFlowTest {

    @Test
    fun fullOfflineLifecycleWorksDeterministically() = runTest {
        val taskRepo = IntegrationTaskRepo()
        val financeRepo = IntegrationFinanceRepo()

        val aiExtractor = ExtractLocalDraftProposalUseCase()
        val createTask = CreateTaskSeriesUseCase(taskRepo)
        val snoozeTask = SnoozeTaskOccurrenceUseCase(taskRepo)
        val completeTask = CompleteTaskOccurrenceUseCase(taskRepo)
        val createObligation = CreateFinancialObligationUseCase(financeRepo)
        val confirmPayment = ConfirmPaymentUseCase(financeRepo)
        val computeSnapshot = ComputeProductivitySnapshotUseCase()
        val buildBackup = BuildBackupPayloadUseCase()
        val verifyBackup = VerifyAndParseBackupUseCase()

        val now = 1_800_000_000_000L

        val proposal = aiExtractor("Pagar boleto internet R$ 119,90")
        assertEquals(ProposedActionType.CREATE_OBLIGATION, proposal.actionType)
        assertEquals(11990L, proposal.suggestedAmountCents)

        val obligationId = createObligation(
            FinancialObligation(
                title = proposal.title,
                amountCents = proposal.suggestedAmountCents!!,
                dueAtUtc = now + 3_600_000L,
                timezoneId = "America/Sao_Paulo",
                createdAtUtc = now
            )
        )

        val series = TaskSeries(
            title = "Conferir extrato mensal",
            recurrence = RecurrenceSpec(RecurrenceType.MONTHLY, 1),
            timezoneId = "America/Sao_Paulo",
            createdAtUtc = now
        )
        val seriesId = createTask(series, initialTriggerUtc = now + 1_800_000L)
        val firstOcc = taskRepo.occurrences.value.first()

        val snoozed = snoozeTask(firstOcc, snoozeMinutes = 10, nowUtc = now + 1_900_000L)
        assertEquals(OccurrenceStatus.SNOOZED, snoozed.status)

        completeTask(series.copy(id = seriesId), snoozed, completedAtUtc = now + 2_500_000L)
        assertEquals(2, taskRepo.occurrences.value.size)

        val savedObligation = financeRepo.obligations.value.first { it.id == obligationId }
        confirmPayment(savedObligation, paidAmountCents = 11990L, paidAtUtc = now + 2_600_000L)

        val snapshot = computeSnapshot(
            occurrences = taskRepo.occurrences.value,
            routineExecutions = emptyList(),
            obligations = financeRepo.obligations.value,
            payments = financeRepo.payments.value,
            nowUtc = now + 2_700_000L
        )
        assertEquals(1, snapshot.completedOccurrencesCount)
        assertEquals(1, snapshot.pendingOccurrencesCount)
        assertEquals(50, snapshot.completionRatePercent)
        assertEquals(0L, snapshot.openObligationsCents)
        assertEquals(11990L, snapshot.paidObligationsCents)

        val envelope = buildBackup(
            exportedAtUtc = now + 2_800_000L,
            tasksCount = taskRepo.series.value.size,
            obligationsCount = financeRepo.obligations.value.size,
            notesCount = 0,
            serializedItemsJson = """{"series":${taskRepo.series.value.size},"paid":11990}"""
        )
        val parsed = verifyBackup(buildBackup.toJsonString(envelope))
        assertTrue(parsed.isSuccess)

        val logLine = LogRedactor.sanitize("Sync token sk-testkey1234567890abcd concluído")
        assertFalse(logLine.contains("sk-testkey1234567890abcd"))
    }
}
