package com.example.meuassistente.ui.screen.modules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.meuassistente.domain.model.RoutineDef
import com.example.meuassistente.domain.repository.RoutineRepository
import com.example.meuassistente.domain.usecase.routine.RecordRoutineExecutionUseCase
import com.example.meuassistente.domain.usecase.routine.SaveRoutineDefUseCase
import kotlinx.coroutines.launch
import java.util.TimeZone

@Composable
fun RoutinesScreen(
    routineRepository: RoutineRepository,
    saveRoutineUseCase: SaveRoutineDefUseCase,
    recordExecutionUseCase: RecordRoutineExecutionUseCase,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val routines by routineRepository.observeActiveRoutines().collectAsState(initial = emptyList())
    var titleInput by remember { mutableStateOf("") }
    var lastExecutedRoutineId by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Rotinas e Hábitos",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            OutlinedButton(onClick = onBack) {
                Text("Voltar")
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Nova rotina diária") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (titleInput.isNotBlank()) {
                            scope.launch {
                                saveRoutineUseCase(
                                    RoutineDef(
                                        title = titleInput,
                                        scheduleSpec = "DAILY:08:00",
                                        timezoneId = TimeZone.getDefault().id,
                                        isActive = true,
                                        createdAtUtc = System.currentTimeMillis()
                                    )
                                )
                                titleInput = ""
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Salvar rotina")
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(routines, key = { it.id }) { routine ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = routine.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (lastExecutedRoutineId == routine.id) {
                                    "Executada hoje (${routine.scheduleSpec})"
                                } else {
                                    routine.scheduleSpec
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    recordExecutionUseCase(
                                        routine = routine,
                                        executedAtUtc = System.currentTimeMillis()
                                    )
                                    lastExecutedRoutineId = routine.id
                                }
                            }
                        ) {
                            Text("Concluir")
                        }
                    }
                }
            }
        }
    }
}
