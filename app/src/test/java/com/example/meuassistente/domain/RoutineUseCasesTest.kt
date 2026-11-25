package com.example.meuassistente.domain

import com.example.meuassistente.domain.model.RoutineDef
import com.example.meuassistente.domain.model.RoutineExecution
import com.example.meuassistente.domain.repository.RoutineRepository
import com.example.meuassistente.domain.usecase.routine.RecordRoutineExecutionUseCase
import com.example.meuassistente.domain.usecase.routine.SaveRoutineDefUseCase
import com.example.meuassistente.domain.usecase.routine.ToggleRoutineActiveUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

private class FakeRoutineRepository : RoutineRepository {
    private val routines = MutableStateFlow<List<RoutineDef>>(emptyList())
    private val executions = MutableStateFlow<List<RoutineExecution>>(emptyList())
    private var nextRoutineId = 1L
    private var nextExecId = 1L

    override fun observeActiveRoutines(): Flow<List<RoutineDef>> =
        routines.map { list -> list.filter { it.isActive } }

    override fun observeExecutions(routineId: Long): Flow<List<RoutineExecution>> =
        executions.map { list -> list.filter { it.routineId == routineId } }

    override suspend fun saveRoutine(routine: RoutineDef): Long {
        val id = if (routine.id == 0L) nextRoutineId++ else routine.id
        val saved = routine.copy(id = id)
        routines.value = routines.value.filterNot { it.id == id } + saved
        return id
    }

    override suspend fun recordExecution(execution: RoutineExecution): Long {
        val id = nextExecId++
        executions.value = executions.value + execution.copy(id = id)
        return id
    }

    fun currentExecutions(): List<RoutineExecution> = executions.value
}

class RoutineUseCasesTest {

    @Test
    fun recordsLocalDateExecutionAndTogglesActiveWithoutLosingHistory() = runTest {
        val repo = FakeRoutineRepository()
        val saveRoutine = SaveRoutineDefUseCase(repo)
        val recordExecution = RecordRoutineExecutionUseCase(repo)
        val toggleActive = ToggleRoutineActiveUseCase(repo)

        val zone = "America/Sao_Paulo"
        val createdAt = ZonedDateTime.of(2026, 11, 20, 7, 0, 0, 0, ZoneId.of(zone))
            .toInstant().toEpochMilli()
        val routineId = saveRoutine(
            RoutineDef(
                title = "Alongamento matinal",
                scheduleSpec = "DAILY:07:00",
                timezoneId = zone,
                createdAtUtc = createdAt
            )
        )

        val execUtc = ZonedDateTime.of(2026, 11, 25, 7, 15, 0, 0, ZoneId.of(zone))
            .toInstant().toEpochMilli()
        val routine = RoutineDef(
            id = routineId,
            title = "Alongamento matinal",
            scheduleSpec = "DAILY:07:00",
            timezoneId = zone,
            createdAtUtc = createdAt
        )
        recordExecution(routine, executedAtUtc = execUtc)

        assertEquals(1, repo.currentExecutions().size)
        assertEquals("2026-11-25", repo.currentExecutions().first().executionDateLocal)

        val paused = toggleActive(routine, isActive = false)
        assertFalse(paused.isActive)
        assertEquals(1, repo.currentExecutions().size)
    }
}
