package com.example.meuassistente.data.local.entity

import java.util.Calendar

enum class TaskCategory(val label: String) {
    GERAL("Geral"),
    TRABALHO("Trabalho"),
    PESSOAL("Pessoal"),
    URGENTE("Urgente");

    companion object {
        fun fromName(name: String): TaskCategory =
            entries.find { it.name == name } ?: GERAL
    }
}

enum class RepeatInterval(val label: String) {
    NONE("Uma vez"),
    DAILY("Diário"),
    WEEKLY("Semanal");

    fun nextTriggerMillis(currentMillis: Long): Long? {
        if (this == NONE) return null
        val cal = Calendar.getInstance().apply { timeInMillis = currentMillis }
        val now = System.currentTimeMillis()
        do {
            when (this) {
                DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                NONE -> return null
            }
        } while (cal.timeInMillis <= now)
        return cal.timeInMillis
    }

    companion object {
        fun fromName(name: String): RepeatInterval =
            entries.find { it.name == name } ?: NONE
    }
}
