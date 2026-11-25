package com.example.meuassistente.domain.usecase.routine

import com.example.meuassistente.domain.model.RoutineDef
import com.example.meuassistente.domain.model.RoutineExecution
import com.example.meuassistente.domain.repository.RoutineRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SaveRoutineDefUseCase(
    private val repository: RoutineRepository
) {
    suspend operator fun invoke(routine: RoutineDef): Long {
        require(routine.title.isNotBlank()) { "O título da rotina não pode estar em branco." }
        require(routine.scheduleSpec.isNotBlank()) { "A programação da rotina não pode estar em branco." }
        return repository.saveRoutine(
            routine.copy(
                title = routine.title.trim(),
                scheduleSpec = routine.scheduleSpec.trim()
            )
        )
    }
}

class RecordRoutineExecutionUseCase(
    private val repository: RoutineRepository
) {
    suspend operator fun invoke(
        routine: RoutineDef,
        executedAtUtc: Long,
        outcome: String = "COMPLETED"
    ): Long {
        require(routine.id > 0L) { "A rotina precisa ter um ID válido antes de registrar execução." }
        require(outcome.isNotBlank()) { "O resultado da execução não pode estar em branco." }

        val zone = runCatching { ZoneId.of(routine.timezoneId) }.getOrElse { ZoneId.systemDefault() }
        val localDate: LocalDate = Instant.ofEpochMilli(executedAtUtc).atZone(zone).toLocalDate()

        return repository.recordExecution(
            RoutineExecution(
                routineId = routine.id,
                executionDateLocal = localDate.toString(),
                executedAtUtc = executedAtUtc,
                outcome = outcome.trim()
            )
        )
    }
}

class ToggleRoutineActiveUseCase(
    private val repository: RoutineRepository
) {
    suspend operator fun invoke(routine: RoutineDef, isActive: Boolean): RoutineDef {
        val updated = routine.copy(isActive = isActive)
        repository.saveRoutine(updated)
        return updated
    }
}
