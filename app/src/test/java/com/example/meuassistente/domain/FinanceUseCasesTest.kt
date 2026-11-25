package com.example.meuassistente.domain

import com.example.meuassistente.domain.model.FinancialObligation
import com.example.meuassistente.domain.model.ObligationStatus
import com.example.meuassistente.domain.model.PaymentRecord
import com.example.meuassistente.domain.repository.FinanceRepository
import com.example.meuassistente.domain.usecase.finance.ConfirmPaymentUseCase
import com.example.meuassistente.domain.usecase.finance.CreateFinancialObligationUseCase
import com.example.meuassistente.domain.usecase.finance.SummarizeObligationsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeFinanceRepository : FinanceRepository {
    private val obligations = MutableStateFlow<List<FinancialObligation>>(emptyList())
    private val payments = MutableStateFlow<List<PaymentRecord>>(emptyList())
    private var nextId = 1L

    override fun observeObligations(): Flow<List<FinancialObligation>> = obligations

    override fun observePayments(obligationId: Long): Flow<List<PaymentRecord>> =
        MutableStateFlow(payments.value.filter { it.obligationId == obligationId })

    override suspend fun saveObligation(obligation: FinancialObligation): Long {
        val id = if (obligation.id == 0L) nextId++ else obligation.id
        val saved = obligation.copy(id = id)
        obligations.value = obligations.value.filterNot { it.id == id } + saved
        return id
    }

    override suspend fun confirmPayment(obligation: FinancialObligation, payment: PaymentRecord) {
        payments.value = payments.value + payment
        obligations.value = obligations.value.map {
            if (it.id == obligation.id) it.copy(status = ObligationStatus.PAID) else it
        }
    }

    fun currentObligations(): List<FinancialObligation> = obligations.value
    fun currentPayments(): List<PaymentRecord> = payments.value
}

class FinanceUseCasesTest {

    @Test
    fun createsObligationConfirmsPaymentAndSummarizesCents() = runTest {
        val repo = FakeFinanceRepository()
        val createUseCase = CreateFinancialObligationUseCase(repo)
        val confirmPayment = ConfirmPaymentUseCase(repo)
        val summarize = SummarizeObligationsUseCase()

        val base = 1_800_000_000_000L
        val id1 = createUseCase(
            FinancialObligation(
                title = "Conta de luz",
                amountCents = 24550L,
                dueAtUtc = base - 10_000L,
                timezoneId = "America/Sao_Paulo",
                createdAtUtc = base - 100_000L
            )
        )
        val id2 = createUseCase(
            FinancialObligation(
                title = "Hospedagem VPS",
                amountCents = 8990L,
                dueAtUtc = base + 500_000L,
                timezoneId = "America/Sao_Paulo",
                createdAtUtc = base - 100_000L
            )
        )

        val beforePayment = summarize(repo.currentObligations(), repo.currentPayments(), nowUtc = base)
        assertEquals(33540L, beforePayment.openTotalCents)
        assertEquals(24550L, beforePayment.overdueTotalCents)
        assertEquals(0L, beforePayment.paidTotalCents)

        val firstObligation = repo.currentObligations().first { it.id == id1 }
        confirmPayment(firstObligation, paidAmountCents = 24550L, paidAtUtc = base)

        val afterPayment = summarize(repo.currentObligations(), repo.currentPayments(), nowUtc = base)
        assertEquals(8990L, afterPayment.openTotalCents)
        assertEquals(0L, afterPayment.overdueTotalCents)
        assertEquals(24550L, afterPayment.paidTotalCents)
        assertEquals(ObligationStatus.PAID, repo.currentObligations().first { it.id == id1 }.status)
        assertEquals(2, listOf(id1, id2).size)
    }
}
