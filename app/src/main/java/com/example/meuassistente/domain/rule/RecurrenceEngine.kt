package com.example.meuassistente.domain.rule

import com.example.meuassistente.domain.model.OccurrenceStatus
import com.example.meuassistente.domain.model.RecurrenceSpec
import com.example.meuassistente.domain.model.RecurrenceType
import com.example.meuassistente.domain.model.TaskOccurrence
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object RecurrenceEngine {

    /**
     * Calcula o próximo instante em UTC respeitando fuso horário local, duração dos meses
     * (ex.: dia 31 -> 28/29/30) e anos bissextos (29/02 -> 28/02 em ano não bissexto).
     */
    fun calculateNextOccurrenceUtc(
        baseScheduledUtc: Long,
        timezoneId: String,
        spec: RecurrenceSpec,
        referenceNowUtc: Long? = null
    ): Long? {
        if (spec.type == RecurrenceType.NONE) return null

        val zone = runCatching { ZoneId.of(timezoneId) }.getOrElse { ZoneId.systemDefault() }
        var candidate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(baseScheduledUtc), zone)

        do {
            candidate = stepOnce(candidate, spec)
        } while (referenceNowUtc != null && candidate.toInstant().toEpochMilli() <= referenceNowUtc)

        return candidate.toInstant().toEpochMilli()
    }

    /**
     * Gera até [count] instantes futuros em UTC de forma determinística a partir de [baseScheduledUtc].
     */
    fun previewOccurrences(
        baseScheduledUtc: Long,
        timezoneId: String,
        spec: RecurrenceSpec,
        count: Int
    ): List<Long> {
        require(count >= 0) { "A quantidade de ocorrências na prévia deve ser >= 0." }
        if (count == 0) return emptyList()
        if (spec.type == RecurrenceType.NONE) return listOf(baseScheduledUtc)

        val zone = runCatching { ZoneId.of(timezoneId) }.getOrElse { ZoneId.systemDefault() }
        val result = mutableListOf<Long>()
        var current = ZonedDateTime.ofInstant(Instant.ofEpochMilli(baseScheduledUtc), zone)

        repeat(count) {
            result += current.toInstant().toEpochMilli()
            current = stepOnce(current, spec)
        }
        return result
    }

    private fun stepOnce(current: ZonedDateTime, spec: RecurrenceSpec): ZonedDateTime {
        val step = spec.interval.toLong().coerceAtLeast(1L)
        return when (spec.type) {
            RecurrenceType.NONE -> current
            RecurrenceType.DAILY -> current.plusDays(step)
            RecurrenceType.WEEKLY -> current.plusWeeks(step)
            RecurrenceType.MONTHLY -> current.plusMonths(step)
            RecurrenceType.YEARLY -> current.plusYears(step)
            RecurrenceType.CUSTOM_DAYS -> current.plusDays(step)
        }
    }

    /**
     * Aplica adiamento (snooze) pontual a uma ocorrência sem alterar scheduledAtUtc original.
     */
    fun snoozeOccurrence(
        occurrence: TaskOccurrence,
        snoozeMinutes: Int,
        nowUtc: Long
    ): TaskOccurrence {
        require(snoozeMinutes > 0) { "Os minutos de adiamento devem ser positivos." }
        val targetUtc = nowUtc + snoozeMinutes * 60_000L
        return occurrence.copy(
            snoozedUntilUtc = targetUtc,
            status = OccurrenceStatus.SNOOZED
        )
    }

    fun isOverdue(occurrence: TaskOccurrence, nowUtc: Long): Boolean {
        if (occurrence.status == OccurrenceStatus.COMPLETED || occurrence.status == OccurrenceStatus.CANCELLED) {
            return false
        }
        return occurrence.effectiveTriggerUtc < nowUtc
    }
}
