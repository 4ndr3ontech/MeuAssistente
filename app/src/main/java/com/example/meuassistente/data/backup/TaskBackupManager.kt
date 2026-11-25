package com.example.meuassistente.data.backup

import com.example.meuassistente.data.local.entity.RepeatInterval
import com.example.meuassistente.data.local.entity.TaskCategory
import com.example.meuassistente.data.local.entity.TaskEntity
import org.json.JSONArray
import org.json.JSONObject

object TaskBackupManager {

    fun exportToJson(tasks: List<TaskEntity>): String {
        val root = JSONObject().apply {
            put("app", "MeuAssistente")
            put("schemaVersion", 2)
            put("exportedAtMillis", System.currentTimeMillis())
        }
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(JSONObject().apply {
                put("title", task.title)
                put("description", task.description)
                put("triggerAtMillis", task.triggerAtMillis)
                put("hasAlarm", task.hasAlarm)
                put("isCompleted", task.isCompleted)
                put("category", task.category)
                put("repeatInterval", task.repeatInterval)
                put("createdAtMillis", task.createdAtMillis)
            })
        }
        root.put("tasks", array)
        return root.toString(2)
    }

    fun importFromJson(rawJson: String): List<TaskEntity> {
        val root = JSONObject(rawJson)
        val array = root.getJSONArray("tasks")
        val imported = mutableListOf<TaskEntity>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val title = obj.optString("title", "").trim()
            if (title.isEmpty()) continue
            imported.add(
                TaskEntity(
                    id = 0L,
                    title = title,
                    description = obj.optString("description", ""),
                    triggerAtMillis = obj.optLong("triggerAtMillis", System.currentTimeMillis()),
                    hasAlarm = obj.optBoolean("hasAlarm", true),
                    isCompleted = obj.optBoolean("isCompleted", false),
                    category = TaskCategory.fromName(obj.optString("category", TaskCategory.GERAL.name)).name,
                    repeatInterval = RepeatInterval.fromName(obj.optString("repeatInterval", RepeatInterval.NONE.name)).name,
                    createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                )
            )
        }
        return imported
    }
}
