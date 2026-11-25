package com.example.meuassistente.domain.rule

import com.example.meuassistente.domain.model.FinancialObligation
import com.example.meuassistente.domain.model.ObligationStatus
import com.example.meuassistente.domain.model.PaymentRecord
import java.util.Locale

object FinanceRules {

    fun isOverdue(obligation: FinancialObligation, nowUtc: Long): Boolean {
        return obligation.status == ObligationStatus.OPEN && obligation.dueAtUtc < nowUtc
    }

    fun remainingAmountCents(
        obligation: FinancialObligation,
        payments: List<PaymentRecord>
    ): Long {
        if (obligation.status != ObligationStatus.OPEN) return 0L
        val totalPaid = payments
            .filter { it.obligationId == obligation.id }
            .sumOf { it.paidAmountCents }
        return (obligation.amountCents - totalPaid).coerceAtLeast(0L)
    }

    fun formatCentsBrl(amountCents: Long): String {
        val negative = amountCents < 0L
        val absCents = kotlin.math.abs(amountCents)
        val reais = absCents / 100L
        val cents = absCents % 100L
        val sign = if (negative) "-" else ""
        return String.format(Locale.forLanguageTag("pt-BR"), "%sR$ %,d,%02d", sign, reais, cents)
    }
}
