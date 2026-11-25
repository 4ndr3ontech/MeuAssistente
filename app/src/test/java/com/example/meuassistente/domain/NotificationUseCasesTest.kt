package com.example.meuassistente.domain

import com.example.meuassistente.data.local.dao.NotificationLogDao
import com.example.meuassistente.data.local.entity.NotificationLogEntity
import com.example.meuassistente.domain.usecase.notification.BuildSnoozeTriggerUseCase
import com.example.meuassistente.domain.usecase.notification.NotificationPermissionState
import com.example.meuassistente.domain.usecase.notification.RecordNotificationActionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeNotificationLogDao : NotificationLogDao {
    private val logs = MutableStateFlow<List<NotificationLogEntity>>(emptyList())
    private var nextId = 1L

    override fun observeRecentLogs(): Flow<List<NotificationLogEntity>> = logs

    override suspend fun insertLog(log: NotificationLogEntity): Long {
        val id = nextId++
        logs.value = logs.value + log.copy(id = id)
        return id
    }

    fun entries(): List<NotificationLogEntity> = logs.value
}

class NotificationUseCasesTest {

    @Test
    fun recordsActionAndCalculatesSnoozeIntervals() = runTest {
        val dao = FakeNotificationLogDao()
        val recordUseCase = RecordNotificationActionUseCase(dao)
        val snoozeUseCase = BuildSnoozeTriggerUseCase()

        val now = 1_800_000_000_000L
        val logId = recordUseCase(
            sourceType = "TASK_OCCURRENCE",
            sourceId = 42L,
            triggeredAtUtc = now,
            actionTaken = "SNOOZED_10M"
        )

        assertEquals(1L, logId)
        assertEquals("SNOOZED_10M", dao.entries().first().actionTaken)
        assertEquals(now + 5 * 60_000L, snoozeUseCase(now, 5))
        assertEquals(now + 10 * 60_000L, snoozeUseCase(now, 10))
        assertEquals(now + 30 * 60_000L, snoozeUseCase(now, 30))
    }

    @Test
    fun detectsPermissionRestrictionsAccurately() {
        val unrestricted = NotificationPermissionState(
            canPostNotifications = true,
            canScheduleExactAlarms = true
        )
        val restricted = NotificationPermissionState(
            canPostNotifications = false,
            canScheduleExactAlarms = true
        )

        assertFalse(unrestricted.hasAnyRestriction)
        assertTrue(restricted.hasAnyRestriction)
    }
}
