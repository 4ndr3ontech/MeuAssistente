package com.example.meuassistente.rule

import com.example.meuassistente.domain.model.FinancialObligation
import com.example.meuassistente.domain.model.ObligationStatus
import com.example.meuassistente.domain.model.OccurrenceStatus
import com.example.meuassistente.domain.model.PaymentRecord
import com.example.meuassistente.domain.model.RecurrenceSpec
import com.example.meuassistente.domain.model.RecurrenceType
import com.example.meuassistente.domain.model.TaskOccurrence
import com.example.meuassistente.domain.rule.FinanceRules
import com.example.meuassistente.domain.rule.RecurrenceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class RecurrenceAndFinanceRulesTest {

    private val zoneSp = "America/Sao_Paulo"

    @Test
    fun nonRecurringReturnsNull() {
        val baseUtc = ZonedDateTime.of(2026, 11, 25, 9, 0, 0, 0, ZoneId.of(zoneSp))
            .toInstant().toEpochMilli()
        val next = RecurrenceEngine.calculateNextOccurrenceUtc(
            baseScheduledUtc = baseUtc,
            timezoneId = zoneSp,
            spec = RecurrenceSpec(RecurrenceType.NONE)
        )
        assertNull(next)
    }

    @Test
    fun monthlyRecurrenceOnDay31AdjustsToFebruaryLastDay() {
        val jan31Utc = ZonedDateTime.of(2026, 1, 31, 10, 30, 0, 0, ZoneId.of(zoneSp))
            .toInstant().toEpochMilli()
        val nextUtc = RecurrenceEngine.calculateNextOccurrenceUtc(
            baseScheduledUtc = jan31Utc,
            timezoneId = zoneSp,
            spec = RecurrenceSpec(RecurrenceType.MONTHLY, 1)
        )!!
        val nextZoned = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nextUtc), ZoneId.of(zoneSp))
        assertEquals(2026, nextZoned.year)
        assertEquals(2, nextZoned.monthValue)
        assertEquals(28, nextZoned.dayOfMonth)
        assertEquals(10, nextZoned.hour)
        assertEquals(30, nextZoned.minute)
    }

    @Test
    fun yearlyRecurrenceOnLeapDayAdjustsCleanlyInNonLeapYear() {
        val feb29Utc = ZonedDateTime.of(2024, 2, 29, 8, 0, 0, 0, ZoneId.of(zoneSp))
            .toInstant().toEpochMilli()
        val nextUtc = RecurrenceEngine.calculateNextOccurrenceUtc(
            baseScheduledUtc = feb29Utc,
            timezoneId = zoneSp,
            spec = RecurrenceSpec(RecurrenceType.YEARLY, 1)
        )!!
        val nextZoned = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nextUtc), ZoneId.of(zoneSp))
        assertEquals(2025, nextZoned.year)
        assertEquals(2, nextZoned.monthValue)
        assertEquals(28, nextZoned.dayOfMonth)
    }

    @Test
    fun snoozePreservesOriginalScheduledTimestamp() {
        val scheduledUtc = 1_800_000_000_000L
        val nowUtc = 1_800_000_060_000L
        val original = TaskOccurrence(
            id = 5L,
            taskSeriesId = 1L,
            scheduledAtUtc = scheduledUtc,
            timezoneId = zoneSp
        )

        val snoozed = RecurrenceEngine.snoozeOccurrence(original, snoozeMinutes = 10, nowUtc = nowUtc)

        assertEquals(scheduledUtc, snoozed.scheduledAtUtc)
        assertEquals(nowUtc + 600_000L, snoozed.snoozedUntilUtc)
        assertEquals(OccurrenceStatus.SNOOZED, snoozed.status)
    }

    @Test
    fun financeRulesCalculateExactRemainingAndFormatting() {
        val obligation = FinancialObligation(
            id = 7L,
            title = "Internet Fibra",
            amountCents = 12990L,
            dueAtUtc = 1_795_000_000_000L,
            timezoneId = zoneSp,
            status = ObligationStatus.OPEN,
            createdAtUtc = 1_794_000_000_000L
        )

        assertTrue(FinanceRules.isOverdue(obligation, nowUtc = 1_795_100_000_000L))
        assertEquals(12990L, FinanceRules.remainingAmountCents(obligation, emptyList()))

        val paidObligation = obligation.copy(status = ObligationStatus.PAID)
        val payment = PaymentRecord(
            id = 1L,
            obligationId = 7L,
            paidAmountCents = 12990L,
            paidAtUtc = 1_795_050_000_000L
        )
        assertFalse(FinanceRules.isOverdue(paidObligation, nowUtc = 1_795_100_000_000L))
        assertEquals(0L, FinanceRules.remainingAmountCents(paidObligation, listOf(payment)))
        assertEquals("R$ 129,90", FinanceRules.formatCentsBrl(12990L))
    }
}
