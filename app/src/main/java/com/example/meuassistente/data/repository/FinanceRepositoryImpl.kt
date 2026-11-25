package com.example.meuassistente.data.repository

import com.example.meuassistente.data.local.dao.FinanceDao
import com.example.meuassistente.data.local.entity.FinancialObligationEntity
import com.example.meuassistente.data.local.entity.PaymentRecordEntity
import com.example.meuassistente.domain.model.FinancialObligation
import com.example.meuassistente.domain.model.ObligationStatus
import com.example.meuassistente.domain.model.PaymentRecord
import com.example.meuassistente.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FinanceRepositoryImpl(
    private val financeDao: FinanceDao
) : FinanceRepository {

    override fun observeObligations(): Flow<List<FinancialObligation>> {
        return financeDao.observeObligations().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observePayments(obligationId: Long): Flow<List<PaymentRecord>> {
        return financeDao.observePaymentsForObligation(obligationId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveObligation(obligation: FinancialObligation): Long {
        return financeDao.insertObligation(obligation.toEntity())
    }

    override suspend fun confirmPayment(obligation: FinancialObligation, payment: PaymentRecord) {
        financeDao.registerPaymentAndMarkPaid(
            obligation = obligation.toEntity(),
            payment = payment.toEntity()
        )
    }

    private fun FinancialObligationEntity.toDomain(): FinancialObligation = FinancialObligation(
        id = id,
        title = title,
        amountCents = amountCents,
        dueAtUtc = dueAtUtc,
        timezoneId = timezoneId,
        status = ObligationStatus.fromName(status),
        createdAtUtc = createdAtUtc
    )

    private fun FinancialObligation.toEntity(): FinancialObligationEntity = FinancialObligationEntity(
        id = id,
        title = title,
        amountCents = amountCents,
        dueAtUtc = dueAtUtc,
        timezoneId = timezoneId,
        status = status.name,
        createdAtUtc = createdAtUtc
    )

    private fun PaymentRecordEntity.toDomain(): PaymentRecord = PaymentRecord(
        id = id,
        obligationId = obligationId,
        paidAmountCents = paidAmountCents,
        paidAtUtc = paidAtUtc
    )

    private fun PaymentRecord.toEntity(): PaymentRecordEntity = PaymentRecordEntity(
        id = id,
        obligationId = obligationId,
        paidAmountCents = paidAmountCents,
        paidAtUtc = paidAtUtc
    )
}
