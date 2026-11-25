package com.example.meuassistente.domain.usecase.notification

import com.example.meuassistente.data.local.dao.NotificationLogDao
import com.example.meuassistente.data.local.entity.NotificationLogEntity

data class NotificationPermissionState(
    val canPostNotifications: Boolean,
    val canScheduleExactAlarms: Boolean
) {
    val hasAnyRestriction: Boolean
        get() = !canPostNotifications || !canScheduleExactAlarms
}

class RecordNotificationActionUseCase(
    private val notificationLogDao: NotificationLogDao
) {
    suspend operator fun invoke(
        sourceType: String,
        sourceId: Long,
        triggeredAtUtc: Long,
        actionTaken: String
    ): Long {
        require(sourceType.isNotBlank()) { "O tipo de origem da notificação não pode estar em branco." }
        require(actionTaken.isNotBlank()) { "A ação registrada não pode estar em branco." }
        return notificationLogDao.insertLog(
            NotificationLogEntity(
                sourceType = sourceType.trim(),
                sourceId = sourceId,
                triggeredAtUtc = triggeredAtUtc,
                actionTaken = actionTaken.trim()
            )
        )
    }
}

class BuildSnoozeTriggerUseCase {
    operator fun invoke(nowUtc: Long, snoozeMinutes: Int): Long {
        require(snoozeMinutes > 0) { "O tempo de soneca deve ser maior que zero." }
        return nowUtc + snoozeMinutes * 60_000L
    }
}
