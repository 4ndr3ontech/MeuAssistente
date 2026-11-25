package com.example.meuassistente.data

import com.example.meuassistente.data.local.entity.FinancialObligationEntity
import com.example.meuassistente.data.local.entity.PaymentRecordEntity
import com.example.meuassistente.data.local.entity.TaskOccurrenceEntity
import com.example.meuassistente.data.local.entity.TaskSeriesEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseSchemaIntegrityTest {

    @Test
    fun financialAmountsAreStoredInExactCents() {
        val obligation = FinancialObligationEntity(
            id = 1L,
            title = "Energia elétrica",
            amountCents = 18590L,
            dueAtUtc = 1795564800000L,
            timezoneId = "America/Sao_Paulo",
            createdAtUtc = 1795478400000L
        )
        val payment = PaymentRecordEntity(
            id = 1L,
            obligationId = obligation.id,
            paidAmountCents = 18590L,
            paidAtUtc = 1795564800000L
        )

        assertEquals(18590L, obligation.amountCents)
        assertEquals(obligation.amountCents, payment.paidAmountCents)
    }

    @Test
    fun seriesAndOccurrenceKeepIndependentTimestamps() {
        val series = TaskSeriesEntity(
            id = 10L,
            title = "Revisão semanal",
            createdAtUtc = 1795478400000L
        )
        val occurrence = TaskOccurrenceEntity(
            id = 20L,
            taskSeriesId = series.id,
            scheduledAtUtc = 1795564800000L,
            timezoneId = "America/Sao_Paulo"
        )

        assertEquals(series.id, occurrence.taskSeriesId)
        assertTrue(occurrence.scheduledAtUtc > series.createdAtUtc)
    }
}
