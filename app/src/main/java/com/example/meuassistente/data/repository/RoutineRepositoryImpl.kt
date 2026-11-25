package com.example.meuassistente.data.repository

import com.example.meuassistente.data.local.dao.RoutineDao
import com.example.meuassistente.data.local.entity.RoutineDefEntity
import com.example.meuassistente.data.local.entity.RoutineExecutionEntity
import com.example.meuassistente.domain.model.RoutineDef
import com.example.meuassistente.domain.model.RoutineExecution
import com.example.meuassistente.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoutineRepositoryImpl(
    private val routineDao: RoutineDao
) : RoutineRepository {

    override fun observeActiveRoutines(): Flow<List<RoutineDef>> {
        return routineDao.observeActiveRoutines().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeExecutions(routineId: Long): Flow<List<RoutineExecution>> {
        return routineDao.observeExecutionsForRoutine(routineId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun saveRoutine(routine: RoutineDef): Long {
        return routineDao.insertRoutine(routine.toEntity())
    }

    override suspend fun recordExecution(execution: RoutineExecution): Long {
        return routineDao.insertExecution(execution.toEntity())
    }

    private fun RoutineDefEntity.toDomain(): RoutineDef = RoutineDef(
        id = id,
        title = title,
        scheduleSpec = scheduleSpec,
        timezoneId = timezoneId,
        isActive = isActive,
        createdAtUtc = createdAtUtc
    )

    private fun RoutineDef.toEntity(): RoutineDefEntity = RoutineDefEntity(
        id = id,
        title = title,
        scheduleSpec = scheduleSpec,
        timezoneId = timezoneId,
        isActive = isActive,
        createdAtUtc = createdAtUtc
    )

    private fun RoutineExecutionEntity.toDomain(): RoutineExecution = RoutineExecution(
        id = id,
        routineId = routineId,
        executionDateLocal = executionDateLocal,
        executedAtUtc = executedAtUtc,
        outcome = outcome
    )

    private fun RoutineExecution.toEntity(): RoutineExecutionEntity = RoutineExecutionEntity(
        id = id,
        routineId = routineId,
        executionDateLocal = executionDateLocal,
        executedAtUtc = executedAtUtc,
        outcome = outcome
    )
}
