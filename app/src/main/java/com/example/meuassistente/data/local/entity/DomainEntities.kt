package com.example.meuassistente.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.TimeZone

@Entity(tableName = "task_series")
data class TaskSeriesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val category: String = TaskCategory.GERAL.name,
    @ColumnInfo(name = "recurrence_rule")
    val recurrenceRule: String = RepeatInterval.NONE.name,
    val priority: Int = 0,
    @ColumnInfo(name = "has_alarm")
    val hasAlarm: Boolean = true,
    @ColumnInfo(name = "timezone_id")
    val timezoneId: String = TimeZone.getDefault().id,
    @ColumnInfo(name = "created_at_utc")
    val createdAtUtc: Long,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false
)

@Entity(
    tableName = "task_occurrence",
    foreignKeys = [
        ForeignKey(
            entity = TaskSeriesEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_series_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["task_series_id"]),
        Index(value = ["scheduled_at_utc"]),
        Index(value = ["status"])
    ]
)
data class TaskOccurrenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "task_series_id")
    val taskSeriesId: Long,
    @ColumnInfo(name = "scheduled_at_utc")
    val scheduledAtUtc: Long,
    @ColumnInfo(name = "timezone_id")
    val timezoneId: String = TimeZone.getDefault().id,
    @ColumnInfo(name = "snoozed_until_utc")
    val snoozedUntilUtc: Long? = null,
    val status: String = "PENDING",
    @ColumnInfo(name = "completed_at_utc")
    val completedAtUtc: Long? = null
)

@Entity(
    tableName = "calendar_event",
    indices = [
        Index(value = ["start_at_utc"]),
        Index(value = ["end_at_utc"])
    ]
)
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    @ColumnInfo(name = "start_at_utc")
    val startAtUtc: Long,
    @ColumnInfo(name = "end_at_utc")
    val endAtUtc: Long,
    @ColumnInfo(name = "timezone_id")
    val timezoneId: String = TimeZone.getDefault().id,
    val status: String = "SCHEDULED",
    @ColumnInfo(name = "created_at_utc")
    val createdAtUtc: Long
)

@Entity(tableName = "routine_def")
data class RoutineDefEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    @ColumnInfo(name = "schedule_spec")
    val scheduleSpec: String,
    @ColumnInfo(name = "timezone_id")
    val timezoneId: String = TimeZone.getDefault().id,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "created_at_utc")
    val createdAtUtc: Long
)

@Entity(
    tableName = "routine_execution",
    foreignKeys = [
        ForeignKey(
            entity = RoutineDefEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["routine_id"]),
        Index(value = ["execution_date_local"])
    ]
)
data class RoutineExecutionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "routine_id")
    val routineId: Long,
    @ColumnInfo(name = "execution_date_local")
    val executionDateLocal: String,
    @ColumnInfo(name = "executed_at_utc")
    val executedAtUtc: Long,
    val outcome: String = "COMPLETED"
)

@Entity(
    tableName = "financial_obligation",
    indices = [
        Index(value = ["due_at_utc"]),
        Index(value = ["status"])
    ]
)
data class FinancialObligationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    @ColumnInfo(name = "amount_cents")
    val amountCents: Long,
    @ColumnInfo(name = "due_at_utc")
    val dueAtUtc: Long,
    @ColumnInfo(name = "timezone_id")
    val timezoneId: String = TimeZone.getDefault().id,
    val status: String = "OPEN",
    @ColumnInfo(name = "created_at_utc")
    val createdAtUtc: Long
)

@Entity(
    tableName = "payment_record",
    foreignKeys = [
        ForeignKey(
            entity = FinancialObligationEntity::class,
            parentColumns = ["id"],
            childColumns = ["obligation_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["obligation_id"]),
        Index(value = ["paid_at_utc"])
    ]
)
data class PaymentRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "obligation_id")
    val obligationId: Long,
    @ColumnInfo(name = "paid_amount_cents")
    val paidAmountCents: Long,
    @ColumnInfo(name = "paid_at_utc")
    val paidAtUtc: Long
)

@Entity(tableName = "checklist")
data class ChecklistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at_utc")
    val createdAtUtc: Long
)

@Entity(
    tableName = "checklist_item",
    foreignKeys = [
        ForeignKey(
            entity = ChecklistEntity::class,
            parentColumns = ["id"],
            childColumns = ["checklist_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["checklist_id"])]
)
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "checklist_id")
    val checklistId: Long,
    val label: String,
    @ColumnInfo(name = "is_checked")
    val isChecked: Boolean = false,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0
)

@Entity(
    tableName = "note_item",
    indices = [Index(value = ["updated_at_utc"])]
)
data class NoteItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val body: String,
    @ColumnInfo(name = "updated_at_utc")
    val updatedAtUtc: Long,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false
)

@Entity(
    tableName = "notification_log",
    indices = [Index(value = ["triggered_at_utc"])]
)
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "source_type")
    val sourceType: String,
    @ColumnInfo(name = "source_id")
    val sourceId: Long,
    @ColumnInfo(name = "triggered_at_utc")
    val triggeredAtUtc: Long,
    @ColumnInfo(name = "action_taken")
    val actionTaken: String
)
