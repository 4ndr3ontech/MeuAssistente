package com.example.meuassistente.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `task_series` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `recurrence_rule` TEXT NOT NULL,
                `priority` INTEGER NOT NULL,
                `has_alarm` INTEGER NOT NULL,
                `timezone_id` TEXT NOT NULL,
                `created_at_utc` INTEGER NOT NULL,
                `is_archived` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `task_occurrence` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `task_series_id` INTEGER NOT NULL,
                `scheduled_at_utc` INTEGER NOT NULL,
                `timezone_id` TEXT NOT NULL,
                `snoozed_until_utc` INTEGER,
                `status` TEXT NOT NULL,
                `completed_at_utc` INTEGER,
                FOREIGN KEY(`task_series_id`) REFERENCES `task_series`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_occurrence_task_series_id` ON `task_occurrence` (`task_series_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_occurrence_scheduled_at_utc` ON `task_occurrence` (`scheduled_at_utc`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_occurrence_status` ON `task_occurrence` (`status`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `calendar_event` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `start_at_utc` INTEGER NOT NULL,
                `end_at_utc` INTEGER NOT NULL,
                `timezone_id` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `created_at_utc` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_event_start_at_utc` ON `calendar_event` (`start_at_utc`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_calendar_event_end_at_utc` ON `calendar_event` (`end_at_utc`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `routine_def` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `schedule_spec` TEXT NOT NULL,
                `timezone_id` TEXT NOT NULL,
                `is_active` INTEGER NOT NULL,
                `created_at_utc` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `routine_execution` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `routine_id` INTEGER NOT NULL,
                `execution_date_local` TEXT NOT NULL,
                `executed_at_utc` INTEGER NOT NULL,
                `outcome` TEXT NOT NULL,
                FOREIGN KEY(`routine_id`) REFERENCES `routine_def`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_routine_execution_routine_id` ON `routine_execution` (`routine_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_routine_execution_execution_date_local` ON `routine_execution` (`execution_date_local`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `financial_obligation` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `amount_cents` INTEGER NOT NULL,
                `due_at_utc` INTEGER NOT NULL,
                `timezone_id` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `created_at_utc` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_financial_obligation_due_at_utc` ON `financial_obligation` (`due_at_utc`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_financial_obligation_status` ON `financial_obligation` (`status`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `payment_record` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `obligation_id` INTEGER NOT NULL,
                `paid_amount_cents` INTEGER NOT NULL,
                `paid_at_utc` INTEGER NOT NULL,
                FOREIGN KEY(`obligation_id`) REFERENCES `financial_obligation`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_record_obligation_id` ON `payment_record` (`obligation_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_record_paid_at_utc` ON `payment_record` (`paid_at_utc`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `checklist` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `is_archived` INTEGER NOT NULL,
                `created_at_utc` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `checklist_item` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `checklist_id` INTEGER NOT NULL,
                `label` TEXT NOT NULL,
                `is_checked` INTEGER NOT NULL,
                `sort_order` INTEGER NOT NULL,
                FOREIGN KEY(`checklist_id`) REFERENCES `checklist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_checklist_item_checklist_id` ON `checklist_item` (`checklist_id`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `note_item` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `body` TEXT NOT NULL,
                `updated_at_utc` INTEGER NOT NULL,
                `is_archived` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_item_updated_at_utc` ON `note_item` (`updated_at_utc`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `notification_log` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `source_type` TEXT NOT NULL,
                `source_id` INTEGER NOT NULL,
                `triggered_at_utc` INTEGER NOT NULL,
                `action_taken` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_log_triggered_at_utc` ON `notification_log` (`triggered_at_utc`)")
    }
}
