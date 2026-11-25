package com.example.meuassistente.domain.usecase.statistics

import com.example.meuassistente.domain.model.FinancialObligation
import com.example.meuassistente.domain.model.ObligationStatus
import com.example.meuassistente.domain.model.OccurrenceStatus
import com.example.meuassistente.domain.model.PaymentRecord
import com.example.meuassistente.domain.model.RoutineExecution
import com.example.meuassistente.domain.model.TaskOccurrence
import com.example.meuassistente.domain.rule.FinanceRules
import com.example.meuassistente.domain.rule.RecurrenceEngine

data class ProductivitySnapshot(
    val pendingOccurrencesCount: Int,
    val completedOccurrencesCount: Int,
    val overdueOccurrencesCount: Int,
    val completionRatePercent: Int,
    val routineExecutionsCount: Int,
    val openObligationsCents: Long,
    val overdueObligationsCents: Long,
    val paidObligationsCents: Long
)

class ComputeProductivitySnapshotUseCase {
    operator fun invoke(
        occurrences: List<TaskOccurrence>,
        routineExecutions: List<RoutineExecution>,
        obligations: List<FinancialObligation>,
        payments: List<PaymentRecord>,
        nowUtc: Long
    ): ProductivitySnapshot {
        val activeOccurrences = occurrences.filter { it.status != OccurrenceStatus.CANCELLED }
        val completedCount = activeOccurrences.count { it.status == OccurrenceStatus.COMPLETED }
        val pendingCount = activeOccurrences.count {
            it.status == OccurrenceStatus.PENDING || it.status == OccurrenceStatus.SNOOZED
        }
        val overdueCount = activeOccurrences.count { RecurrenceEngine.isOverdue(it, nowUtc) }
        val totalEvaluated = completedCount + pendingCount
        val completionRate = if (totalEvaluated == 0) {
            0
        } else {
            ((completedCount * 100) / totalEvaluated).coerceIn(0, 100)
        }

        var openCents = 0L
        var overdueCents = 0L
        for (obligation in obligations) {
            if (obligation.status == ObligationStatus.OPEN) {
                val remaining = FinanceRules.remainingAmountCents(obligation, payments)
                openCents += remaining
                if (FinanceRules.isOverdue(obligation, nowUtc)) {
                    overdueCents += remaining
                }
            }
        }
        val paidCents = payments.sumOf { it.paidAmountCents }

        return ProductivitySnapshot(
            pendingOccurrencesCount = pendingCount,
            completedOccurrencesCount = completedCount,
            overdueOccurrencesCount = overdueCount,
            completionRatePercent = completionRate,
            routineExecutionsCount = routineExecutions.size,
            openObligationsCents = openCents,
            overdueObligationsCents = overdueCents,
            paidObligationsCents = paidCents
        )
    }
}
