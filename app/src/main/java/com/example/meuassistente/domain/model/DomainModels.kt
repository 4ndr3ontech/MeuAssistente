package com.example.meuassistente.domain.model

enum class RecurrenceType {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM_DAYS
}

data class RecurrenceSpec(
    val type: RecurrenceType = RecurrenceType.NONE,
    val interval: Int = 1
) {
    init {
        require(interval >= 1) { "O intervalo de recorrência deve ser >= 1." }
    }

    fun serialize(): String = if (type == RecurrenceType.NONE) "NONE" else "${type.name}:$interval"

    companion object {
        fun parse(raw: String): RecurrenceSpec {
            val trimmed = raw.trim().uppercase()
            if (trimmed.isEmpty() || trimmed == "NONE") return RecurrenceSpec(RecurrenceType.NONE, 1)
            val parts = trimmed.split(":")
            val type = runCatching { RecurrenceType.valueOf(parts[0]) }.getOrDefault(RecurrenceType.NONE)
            val interval = parts.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            return RecurrenceSpec(type = type, interval = interval)
        }
    }
}

enum class OccurrenceStatus {
    PENDING,
    COMPLETED,
    SNOOZED,
    CANCELLED;

    companion object {
        fun fromName(raw: String): OccurrenceStatus =
            entries.find { it.name == raw } ?: PENDING
    }
}

data class TaskSeries(
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val category: String = "GERAL",
    val recurrence: RecurrenceSpec = RecurrenceSpec(),
    val priority: Int = 0,
    val hasAlarm: Boolean = true,
    val timezoneId: String,
    val createdAtUtc: Long,
    val isArchived: Boolean = false
)

data class TaskOccurrence(
    val id: Long = 0L,
    val taskSeriesId: Long,
    val scheduledAtUtc: Long,
    val timezoneId: String,
    val snoozedUntilUtc: Long? = null,
    val status: OccurrenceStatus = OccurrenceStatus.PENDING,
    val completedAtUtc: Long? = null
) {
    val effectiveTriggerUtc: Long
        get() = snoozedUntilUtc ?: scheduledAtUtc
}

data class CalendarEvent(
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val startAtUtc: Long,
    val endAtUtc: Long,
    val timezoneId: String,
    val status: String = "SCHEDULED",
    val createdAtUtc: Long
)

data class RoutineDef(
    val id: Long = 0L,
    val title: String,
    val scheduleSpec: String,
    val timezoneId: String,
    val isActive: Boolean = true,
    val createdAtUtc: Long
)

data class RoutineExecution(
    val id: Long = 0L,
    val routineId: Long,
    val executionDateLocal: String,
    val executedAtUtc: Long,
    val outcome: String = "COMPLETED"
)

enum class ObligationStatus {
    OPEN,
    PAID,
    CANCELLED;

    companion object {
        fun fromName(raw: String): ObligationStatus =
            entries.find { it.name == raw } ?: OPEN
    }
}

data class FinancialObligation(
    val id: Long = 0L,
    val title: String,
    val amountCents: Long,
    val dueAtUtc: Long,
    val timezoneId: String,
    val status: ObligationStatus = ObligationStatus.OPEN,
    val createdAtUtc: Long
)

data class PaymentRecord(
    val id: Long = 0L,
    val obligationId: Long,
    val paidAmountCents: Long,
    val paidAtUtc: Long
)

data class Checklist(
    val id: Long = 0L,
    val title: String,
    val isArchived: Boolean = false,
    val createdAtUtc: Long
)

data class ChecklistItem(
    val id: Long = 0L,
    val checklistId: Long,
    val label: String,
    val isChecked: Boolean = false,
    val sortOrder: Int = 0
)

data class NoteItem(
    val id: Long = 0L,
    val title: String,
    val body: String,
    val updatedAtUtc: Long,
    val isArchived: Boolean = false
)
