package com.example.meuassistente.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.meuassistente.data.local.dao.CalendarEventDao
import com.example.meuassistente.data.local.dao.ChecklistDao
import com.example.meuassistente.data.local.dao.FinanceDao
import com.example.meuassistente.data.local.dao.NoteDao
import com.example.meuassistente.data.local.dao.NotificationLogDao
import com.example.meuassistente.data.local.dao.RoutineDao
import com.example.meuassistente.data.local.dao.TaskDao
import com.example.meuassistente.data.local.dao.TaskSeriesDao
import com.example.meuassistente.data.local.entity.CalendarEventEntity
import com.example.meuassistente.data.local.entity.ChecklistEntity
import com.example.meuassistente.data.local.entity.ChecklistItemEntity
import com.example.meuassistente.data.local.entity.FinancialObligationEntity
import com.example.meuassistente.data.local.entity.NoteItemEntity
import com.example.meuassistente.data.local.entity.NotificationLogEntity
import com.example.meuassistente.data.local.entity.PaymentRecordEntity
import com.example.meuassistente.data.local.entity.RoutineDefEntity
import com.example.meuassistente.data.local.entity.RoutineExecutionEntity
import com.example.meuassistente.data.local.entity.TaskEntity
import com.example.meuassistente.data.local.entity.TaskOccurrenceEntity
import com.example.meuassistente.data.local.entity.TaskSeriesEntity
import com.example.meuassistente.data.local.migration.MIGRATION_2_3

@Database(
    entities = [
        TaskEntity::class,
        TaskSeriesEntity::class,
        TaskOccurrenceEntity::class,
        CalendarEventEntity::class,
        RoutineDefEntity::class,
        RoutineExecutionEntity::class,
        FinancialObligationEntity::class,
        PaymentRecordEntity::class,
        ChecklistEntity::class,
        ChecklistItemEntity::class,
        NoteItemEntity::class,
        NotificationLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskSeriesDao(): TaskSeriesDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun routineDao(): RoutineDao
    abstract fun financeDao(): FinanceDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun noteDao(): NoteDao
    abstract fun notificationLogDao(): NotificationLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN category TEXT NOT NULL DEFAULT 'GERAL'")
                db.execSQL("ALTER TABLE tasks ADD COLUMN repeatInterval TEXT NOT NULL DEFAULT 'NONE'")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "meu_assistente_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
