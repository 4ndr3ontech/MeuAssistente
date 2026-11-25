package com.example.meuassistente.domain.usecase.finance

import com.example.meuassistente.domain.model.FinancialObligation
import com.example.meuassistente.domain.model.ObligationStatus
import com.example.meuassistente.domain.model.PaymentRecord
import com.example.meuassistente.domain.repository.FinanceRepository
import com.example.meuassistente.domain.rule.FinanceRules

data class FinanceSummary(
    val openTotalCents: Long,
    val overdueTotalCents: Long,
    val paidTotalCents: Long
)

class CreateFinancialObligationUseCase(
    private val repository: FinanceRepository
) {
    suspend operator fun invoke(obligation: FinancialObligation): Long {
        require(obligation.title.isNotBlank()) { "O título da obrigação financeira não pode estar em branco." }
        require(obligation.amountCents > 0L) { "O valor em centavos deve ser maior que zero." }
        return repository.saveObligation(obligation.copy(title = obligation.title.trim()))
    }
}

class ConfirmPaymentUseCase(
    private val repository: FinanceRepository
) {
    suspend operator fun invoke(
        obligation: FinancialObligation,
        paidAmountCents: Long,
        paidAtUtc: Long
    ): PaymentRecord {
        require(obligation.id > 0L) { "A obrigação financeira precisa ter um ID válido." }
        require(paidAmountCents > 0L) { "O valor do pagamento em centavos deve ser maior que zero." }

        val payment = PaymentRecord(
            obligationId = obligation.id,
            paidAmountCents = paidAmountCents,
            paidAtUtc = paidAtUtc
        )
        repository.confirmPayment(obligation = obligation, payment = payment)
        return payment
    }
}

class SummarizeObligationsUseCase {
    operator fun invoke(
        obligations: List<FinancialObligation>,
        payments: List<PaymentRecord>,
        nowUtc: Long
    ): FinanceSummary {
        var openTotal = 0L
        var overdueTotal = 0L

        for (obligation in obligations) {
            if (obligation.status == ObligationStatus.OPEN) {
                val remaining = FinanceRules.remainingAmountCents(obligation, payments)
                openTotal += remaining
                if (FinanceRules.isOverdue(obligation, nowUtc)) {
                    overdueTotal += remaining
                }
            }
        }

        val paidTotal = payments.sumOf { it.paidAmountCents }
        return FinanceSummary(
            openTotalCents = openTotal,
            overdueTotalCents = overdueTotal,
            paidTotalCents = paidTotal
        )
    }
}
