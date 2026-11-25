package com.example.meuassistente.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val triggerAtMillis: Long,
    val hasAlarm: Boolean = true,
    val isCompleted: Boolean = false,
    val category: String = TaskCategory.GERAL.name,
    val repeatInterval: String = RepeatInterval.NONE.name,
    val createdAtMillis: Long = System.currentTimeMillis()
)
