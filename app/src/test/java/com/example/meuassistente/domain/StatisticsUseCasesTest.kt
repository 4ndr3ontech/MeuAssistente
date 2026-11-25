package com.example.meuassistente.domain

import com.example.meuassistente.domain.model.FinancialObligation
import com.example.meuassistente.domain.model.ObligationStatus
import com.example.meuassistente.domain.model.OccurrenceStatus
import com.example.meuassistente.domain.model.PaymentRecord
import com.example.meuassistente.domain.model.RoutineExecution
import com.example.meuassistente.domain.model.TaskOccurrence
import com.example.meuassistente.domain.usecase.statistics.ComputeProductivitySnapshotUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsUseCasesTest {

    @Test
    fun computesAccurateSnapshotMetrics() {
        val useCase = ComputeProductivitySnapshotUseCase()
        val now = 1_800_000_000_000L

        val occurrences = listOf(
            TaskOccurrence(
                id = 1L,
                taskSeriesId = 1L,
                scheduledAtUtc = now - 60_000L,
                timezoneId = "UTC",
                status = OccurrenceStatus.PENDING
            ),
            TaskOccurrence(
                id = 2L,
                taskSeriesId = 1L,
                scheduledAtUtc = now + 60_000L,
                timezoneId = "UTC",
                status = OccurrenceStatus.PENDING
            ),
            TaskOccurrence(
                id = 3L,
                taskSeriesId = 1L,
                scheduledAtUtc = now - 120_000L,
                timezoneId = "UTC",
                status = OccurrenceStatus.COMPLETED,
                completedAtUtc = now - 90_000L
            )
        )
        val executions = listOf(
            RoutineExecution(
                id = 1L,
                routineId = 10L,
                executionDateLocal = "2026-11-25",
                executedAtUtc = now - 30_000L
            )
        )
        val obligations = listOf(
            FinancialObligation(
                id = 1L,
                title = "Energia",
                amountCents = 15000L,
                dueAtUtc = now - 5_000L,
                timezoneId = "UTC",
                status = ObligationStatus.OPEN,
                createdAtUtc = now - 100_000L
            )
        )
        val payments = listOf(
            PaymentRecord(
                id = 1L,
                obligationId = 2L,
                paidAmountCents = 7500L,
                paidAtUtc = now - 20_000L
            )
        )

        val snapshot = useCase(occurrences, executions, obligations, payments, now)
        assertEquals(2, snapshot.pendingOccurrencesCount)
        assertEquals(1, snapshot.completedOccurrencesCount)
        assertEquals(1, snapshot.overdueOccurrencesCount)
        assertEquals(33, snapshot.completionRatePercent)
        assertEquals(1, snapshot.routineExecutionsCount)
        assertEquals(15000L, snapshot.openObligationsCents)
        assertEquals(15000L, snapshot.overdueObligationsCents)
        assertEquals(7500L, snapshot.paidObligationsCents)
    }
}
